package com.notevault.app;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NoteEditorActivity extends Activity {

    private interface TimestampSpanMarker {}

    private static class TimestampColorSpan extends ForegroundColorSpan implements TimestampSpanMarker {
        public TimestampColorSpan(int color) { super(color); }
    }

    private static class TimestampSizeSpan extends RelativeSizeSpan implements TimestampSpanMarker {
        public TimestampSizeSpan(float proportion) { super(proportion); }
    }

    private static class TimestampStyleSpan extends StyleSpan implements TimestampSpanMarker {
        public TimestampStyleSpan(int style) { super(style); }
    }

    private StorageManager storage;
    private NoteRepository repo;
    private Note note;
    private EditText etTitle;
    private EditText etCategory;
    private EditText etContent;
    private String originalContent = "";
    private boolean isSaving = false;
    private boolean isStyling = false;
    private boolean savedAndClosed = false;

    private final Pattern patternDate = Pattern.compile("(?m)^\\s*\\[\\s*\\d{1,2}\\s+[A-Za-z]{3,4}\\s*\\]\\s*$");
    private final Pattern patternTime = Pattern.compile("(?m)^\\s*\\d{1,2}:\\d{2}\\s*$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        storage = new StorageManager(this);
        repo = new NoteRepository(storage);

        View rootView = findViewById(R.id.rootView);
        LinearLayout headerBar = (LinearLayout) findViewById(R.id.headerBar);
        TextView tvHeaderTitle = (TextView) findViewById(R.id.tvHeaderTitle);
        Button btnBack = (Button) findViewById(R.id.btnBack);
        Button btnSave = (Button) findViewById(R.id.btnSave);
        View headerDivider = findViewById(R.id.headerDivider);
        View titleDivider = findViewById(R.id.titleDivider);
        View categoryDivider = findViewById(R.id.categoryDivider);

        etTitle = (EditText) findViewById(R.id.etTitle);
        etCategory = (EditText) findViewById(R.id.etCategory);
        etContent = (EditText) findViewById(R.id.etContent);

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
        etContent.setTextColor(p.textPrimary);
        etContent.setHintTextColor(p.textSecondary);

        String noteId = getIntent().getStringExtra("note_id");
        boolean isNew = (noteId == null || noteId.isEmpty());
        if (!isNew) {
            File f = storage.noteFile(noteId);
            if (f.exists()) {
                try {
                    note = repo.load(f);
                } catch (Exception e) {
                    note = new Note(NoteType.TEXT);
                    isNew = true;
                }
            } else {
                note = new Note(NoteType.TEXT);
                isNew = true;
            }
        } else {
            note = new Note(NoteType.TEXT);
        }

        if (note.title != null) etTitle.setText(note.title);
        if (note.category != null) etCategory.setText(note.category);

        if (note.content != null && !note.content.isEmpty()) {
            originalContent = note.content;
            etContent.setText(note.content);
        }

        // Apply visual styling to timestamps (muted subtle gray, smaller size)
        applyTimestampStyling(etContent.getText());

        // Focus & Cursor placement:
        // New note -> Focus on Title
        // Existing note -> Focus at the end of Content where user left off
        final boolean finalIsNew = isNew;
        rootView.post(new Runnable() {
            @Override
            public void run() {
                if (finalIsNew) {
                    etTitle.requestFocus();
                    if (etTitle.getText() != null) {
                        etTitle.setSelection(etTitle.getText().length());
                    }
                } else {
                    etContent.requestFocus();
                    if (etContent.getText() != null) {
                        etContent.setSelection(etContent.getText().length());
                    }
                }
            }
        });

        etContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                applyTimestampStyling(s);
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
                savedAndClosed = true;
                finish();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
                savedAndClosed = true;
                Toast.makeText(NoteEditorActivity.this, "Saved", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        FontHelper.apply(rootView, this);
    }

    private void applyTimestampStyling(Editable s) {
        if (isStyling || s == null || s.length() == 0) return;
        isStyling = true;
        try {
            TimestampSpanMarker[] oldSpans = s.getSpans(0, s.length(), TimestampSpanMarker.class);
            for (TimestampSpanMarker sp : oldSpans) {
                s.removeSpan(sp);
            }

            ThemeManager.Palette p = ThemeManager.get(storage);
            String text = s.toString();

            Matcher mDate = patternDate.matcher(text);
            while (mDate.find()) {
                int start = mDate.start();
                int end = mDate.end();
                s.setSpan(new TimestampColorSpan(p.textSecondary), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                s.setSpan(new TimestampSizeSpan(0.88f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                s.setSpan(new TimestampStyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            Matcher mTime = patternTime.matcher(text);
            while (mTime.find()) {
                int start = mTime.start();
                int end = mTime.end();
                s.setSpan(new TimestampColorSpan(p.textSecondary), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                s.setSpan(new TimestampSizeSpan(0.82f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } finally {
            isStyling = false;
        }
    }

    private void save() {
        if (note == null || isSaving) return;
        isSaving = true;
        try {
            String title = etTitle.getText().toString().trim();
            String cat = etCategory.getText().toString().trim();
            String currentContent = etContent.getText().toString();

            if (title.isEmpty() && currentContent.trim().isEmpty() && cat.isEmpty()) {
                return;
            }

            currentContent = applyTimestamps(originalContent, currentContent);
            note.title = title;
            note.category = cat;
            note.content = currentContent;

            repo.save(note);
            originalContent = currentContent;
        } catch (Exception e) {
            Toast.makeText(this, "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            isSaving = false;
        }
    }

    private String applyTimestamps(String original, String current) {
        String origTrim = original != null ? original.trim() : "";
        String currTrim = current != null ? current.trim() : "";

        if (currTrim.isEmpty() || currTrim.equals(origTrim)) {
            return current;
        }

        SimpleDateFormat dayFormat = new SimpleDateFormat("d MMM", Locale.US);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
        Date now = new Date();
        String todayHeader = "[ " + dayFormat.format(now).toUpperCase() + " ]";
        String currentTime = timeFormat.format(now);

        // Case 1: Brand new note -> One timestamp at the top when saved
        if (origTrim.isEmpty()) {
            if (!currTrim.startsWith("[")) {
                return todayHeader + "\n" + currentTime + "\n" + currTrim;
            }
            return current;
        }

        // Case 2: User appended text to existing note
        if (current.startsWith(original)) {
            String added = current.substring(original.length());
            String cleanAdded = added.trim();
            if (cleanAdded.isEmpty()) {
                return current;
            }

            if (original.contains(todayHeader)) {
                return origTrim + "\n\n" + currentTime + "\n" + cleanAdded;
            } else {
                return origTrim + "\n\n" + todayHeader + "\n" + currentTime + "\n" + cleanAdded;
            }
        }

        // Case 3: Appended with slight whitespace differences
        if (currTrim.length() > origTrim.length() && currTrim.startsWith(origTrim)) {
            String added = currTrim.substring(origTrim.length()).trim();
            if (!added.isEmpty()) {
                if (origTrim.contains(todayHeader)) {
                    return origTrim + "\n\n" + currentTime + "\n" + added;
                } else {
                    return origTrim + "\n\n" + todayHeader + "\n" + currentTime + "\n" + added;
                }
            }
        }

        return current;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!savedAndClosed) {
            save();
        }
    }
}
