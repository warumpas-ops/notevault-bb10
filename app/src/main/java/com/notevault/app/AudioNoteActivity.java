package com.notevault.app;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.Locale;

public class AudioNoteActivity extends Activity {

    private StorageManager storage;
    private NoteRepository repo;
    private Note note;

    private View rootView;
    private EditText etTitle;
    private EditText etCategory;
    private TextView tvStatus;
    private TextView tvTimer;
    private Button btnRecord;
    private Button btnStop;
    private Button btnPlay;

    private MediaRecorder recorder;
    private MediaPlayer player;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private long recordStartTime = 0;
    private long recordDurationSeconds = 0;

    private final Handler timerHandler = new Handler();
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRecording) {
                long elapsedSeconds = (System.currentTimeMillis() - recordStartTime) / 1000;
                recordDurationSeconds = elapsedSeconds;
                long minutes = elapsedSeconds / 60;
                long seconds = elapsedSeconds % 60;
                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
                timerHandler.postDelayed(this, 500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_note);

        storage = new StorageManager(this);
        repo = new NoteRepository(storage);

        rootView = findViewById(R.id.rootView);
        LinearLayout headerBar = (LinearLayout) findViewById(R.id.headerBar);
        TextView tvHeaderTitle = (TextView) findViewById(R.id.tvHeaderTitle);
        Button btnBack = (Button) findViewById(R.id.btnBack);
        Button btnSave = (Button) findViewById(R.id.btnSave);
        View headerDivider = findViewById(R.id.headerDivider);
        View titleDivider = findViewById(R.id.titleDivider);
        View categoryDivider = findViewById(R.id.categoryDivider);

        etTitle = (EditText) findViewById(R.id.etTitle);
        etCategory = (EditText) findViewById(R.id.etCategory);
        tvStatus = (TextView) findViewById(R.id.tvStatus);
        tvTimer = (TextView) findViewById(R.id.tvTimer);
        btnRecord = (Button) findViewById(R.id.btnRecord);
        btnStop = (Button) findViewById(R.id.btnStop);
        btnPlay = (Button) findViewById(R.id.btnPlay);

        // Apply theme
        ThemeManager.Palette p = ThemeManager.get(storage);
        rootView.setBackgroundColor(p.bg);
        headerBar.setBackgroundColor(p.headerBg);
        tvHeaderTitle.setTextColor(p.textPrimary);
        btnBack.setTextColor(p.textSecondary);
        btnSave.setTextColor(p.textPrimary);
        headerDivider.setBackgroundColor(p.divider);
        titleDivider.setBackgroundColor(p.divider);
        categoryDivider.setBackgroundColor(p.divider);

        etTitle.setTextColor(p.textPrimary);
        etTitle.setHintTextColor(p.textSecondary);
        etCategory.setTextColor(p.textSecondary);
        etCategory.setHintTextColor(p.textSecondary);

        tvStatus.setTextColor(p.textSecondary);
        tvTimer.setTextColor(p.textPrimary);

        btnRecord.setBackgroundDrawable(ThemeManager.createBox(p.btnBg, p.btnBorder, 6));
        btnRecord.setTextColor(p.textPrimary);

        btnStop.setBackgroundDrawable(ThemeManager.createBox(p.inputBg, p.divider, 6));
        btnStop.setTextColor(p.textSecondary);

        btnPlay.setBackgroundDrawable(ThemeManager.createBox(p.inputBg, p.divider, 6));
        btnPlay.setTextColor(p.textSecondary);

        String noteId = getIntent().getStringExtra("note_id");
        if (noteId != null && !noteId.isEmpty()) {
            File f = storage.noteFile(noteId);
            if (f.exists()) {
                try {
                    note = repo.load(f);
                } catch (Exception e) {
                    note = new Note(NoteType.AUDIO);
                }
            } else {
                note = new Note(NoteType.AUDIO);
            }
        } else {
            note = new Note(NoteType.AUDIO);
        }

        if (note.title != null) etTitle.setText(note.title);
        if (note.category != null) etCategory.setText(note.category);

        updateAudioUiState();

        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording) {
                    stopRecording();
                } else if (isPlaying) {
                    stopPlaying();
                }
            }
        });

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPlaying();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
                finish();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
                Toast.makeText(AudioNoteActivity.this, "Saved", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        FontHelper.apply(rootView, this);
    }

    private void updateAudioUiState() {
        ThemeManager.Palette p = ThemeManager.get(storage);
        File audioFile = storage.audioFile(note.id);
        boolean fileExists = audioFile.exists() && audioFile.length() > 0;

        if (fileExists) {
            tvStatus.setText("[ recording saved: " + (audioFile.length() / 1024) + " kb ]");
            btnPlay.setEnabled(true);
            btnPlay.setText("[play recording]");
            btnPlay.setBackgroundDrawable(ThemeManager.createBox(p.btnBg, p.btnBorder, 6));
            btnPlay.setTextColor(p.textPrimary);
        } else {
            tvStatus.setText("[ tap record to begin ]");
            btnPlay.setEnabled(false);
            btnPlay.setText("[play recording]");
            btnPlay.setBackgroundDrawable(ThemeManager.createBox(p.inputBg, p.divider, 6));
            btnPlay.setTextColor(p.textSecondary);
        }
    }

    private void startRecording() {
        ThemeManager.Palette p = ThemeManager.get(storage);
        if (isPlaying) {
            stopPlaying();
        }

        File outFile = storage.audioFile(note.id);
        File parent = outFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try {
            if (recorder != null) {
                safeReleaseRecorder();
            }

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(outFile.getAbsolutePath());

            recorder.prepare();
            recorder.start();

            isRecording = true;
            recordStartTime = System.currentTimeMillis();
            recordDurationSeconds = 0;

            tvStatus.setText("[ recording in progress... ]");
            btnRecord.setEnabled(false);
            btnRecord.setBackgroundDrawable(ThemeManager.createBox(p.inputBg, p.divider, 6));
            btnRecord.setTextColor(p.textSecondary);

            btnStop.setEnabled(true);
            btnStop.setBackgroundDrawable(ThemeManager.createBox(p.btnBg, p.btnBorder, 6));
            btnStop.setTextColor(p.textPrimary);

            btnPlay.setEnabled(false);

            timerHandler.post(timerRunnable);
        } catch (Exception e) {
            safeReleaseRecorder();
            isRecording = false;
            Toast.makeText(this, "Could not start recording: " + e.getMessage(), Toast.LENGTH_LONG).show();
            tvStatus.setText("[ recording failed ]");
            btnRecord.setEnabled(true);
            btnRecord.setBackgroundDrawable(ThemeManager.createBox(p.btnBg, p.btnBorder, 6));
            btnRecord.setTextColor(p.textPrimary);

            btnStop.setEnabled(false);
        }
    }

    private void stopRecording() {
        if (!isRecording) return;
        ThemeManager.Palette p = ThemeManager.get(storage);

        timerHandler.removeCallbacks(timerRunnable);
        long elapsed = System.currentTimeMillis() - recordStartTime;

        if (elapsed < 1000) {
            try {
                Thread.sleep(1000 - elapsed);
            } catch (InterruptedException ignored) {}
        }

        try {
            if (recorder != null) {
                recorder.stop();
            }
        } catch (Exception e) {
        } finally {
            safeReleaseRecorder();
            isRecording = false;
        }

        note.audioFileName = note.id + ".3gp";

        btnRecord.setEnabled(true);
        btnRecord.setBackgroundDrawable(ThemeManager.createBox(p.btnBg, p.btnBorder, 6));
        btnRecord.setTextColor(p.textPrimary);

        btnStop.setEnabled(false);
        btnStop.setBackgroundDrawable(ThemeManager.createBox(p.inputBg, p.divider, 6));
        btnStop.setTextColor(p.textSecondary);

        updateAudioUiState();
        save();
    }

    private void safeReleaseRecorder() {
        if (recorder != null) {
            try {
                recorder.reset();
            } catch (Exception ignored) {}
            try {
                recorder.release();
            } catch (Exception ignored) {}
            recorder = null;
        }
    }

    private void startPlaying() {
        ThemeManager.Palette p = ThemeManager.get(storage);
        File audioFile = storage.audioFile(note.id);
        if (!audioFile.exists() || audioFile.length() == 0) {
            Toast.makeText(this, "No recording found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            stopPlaying();

            player = new MediaPlayer();
            player.setDataSource(audioFile.getAbsolutePath());
            player.prepare();
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopPlaying();
                }
            });
            player.start();

            isPlaying = true;
            tvStatus.setText("[ playing audio... ]");
            btnPlay.setText("[playing...]");
            btnPlay.setEnabled(false);

            btnStop.setEnabled(true);
            btnStop.setBackgroundDrawable(ThemeManager.createBox(p.btnBg, p.btnBorder, 6));
            btnStop.setTextColor(p.textPrimary);

            btnRecord.setEnabled(false);
        } catch (Exception e) {
            stopPlaying();
            Toast.makeText(this, "Playback error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopPlaying() {
        ThemeManager.Palette p = ThemeManager.get(storage);
        if (player != null) {
            try {
                if (player.isPlaying()) {
                    player.stop();
                }
            } catch (Exception ignored) {}
            try {
                player.release();
            } catch (Exception ignored) {}
            player = null;
        }
        isPlaying = false;
        btnRecord.setEnabled(true);
        btnRecord.setBackgroundDrawable(ThemeManager.createBox(p.btnBg, p.btnBorder, 6));
        btnRecord.setTextColor(p.textPrimary);

        btnStop.setEnabled(false);
        btnStop.setBackgroundDrawable(ThemeManager.createBox(p.inputBg, p.divider, 6));
        btnStop.setTextColor(p.textSecondary);

        updateAudioUiState();
    }

    private void save() {
        if (note == null) return;
        String title = etTitle.getText().toString().trim();
        String cat = etCategory.getText().toString().trim();

        File audioFile = storage.audioFile(note.id);
        boolean hasAudio = audioFile.exists() && audioFile.length() > 0;

        if (title.isEmpty() && !hasAudio && cat.isEmpty()) {
            return;
        }

        note.title = title;
        note.category = cat;
        if (hasAudio) {
            note.audioFileName = note.id + ".3gp";
        }

        try {
            repo.save(note);
        } catch (Exception e) {
            Toast.makeText(this, "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isRecording) {
            stopRecording();
        }
        if (isPlaying) {
            stopPlaying();
        }
        save();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        safeReleaseRecorder();
        stopPlaying();
    }
}
