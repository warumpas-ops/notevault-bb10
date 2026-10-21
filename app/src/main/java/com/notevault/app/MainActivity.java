package com.notevault.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private StorageManager storage;
    private NoteRepository repo;
    private List<Note> allNotes = new ArrayList<Note>();
    private List<Note> displayedNotes = new ArrayList<Note>();
    private NotesAdapter adapter;

    private View rootView;
    private LinearLayout headerBar;
    private TextView tvAppTitle;
    private Button btnTheme;
    private Button btnSettings;
    private View headerDivider;
    private TextView tvStoragePath;
    private LinearLayout searchContainer;
    private EditText etSearch;
    private ListView listNotes;
    private TextView tvEmpty;
    private View bottomDivider;
    private LinearLayout bottomBar;
    private Button btnNewText;
    private Button btnNewTask;
    private Button btnNewAudio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        storage = new StorageManager(this);
        repo = new NoteRepository(storage);

        rootView = findViewById(R.id.rootView);
        headerBar = (LinearLayout) findViewById(R.id.headerBar);
        tvAppTitle = (TextView) findViewById(R.id.tvAppTitle);
        btnTheme = (Button) findViewById(R.id.btnTheme);
        btnSettings = (Button) findViewById(R.id.btnSettings);
        headerDivider = findViewById(R.id.headerDivider);
        tvStoragePath = (TextView) findViewById(R.id.tvStoragePath);
        searchContainer = (LinearLayout) findViewById(R.id.searchContainer);
        etSearch = (EditText) findViewById(R.id.etSearch);
        listNotes = (ListView) findViewById(R.id.listNotes);
        tvEmpty = (TextView) findViewById(R.id.tvEmpty);
        bottomDivider = findViewById(R.id.bottomDivider);
        bottomBar = (LinearLayout) findViewById(R.id.bottomBar);
        btnNewText = (Button) findViewById(R.id.btnNewText);
        btnNewTask = (Button) findViewById(R.id.btnNewTask);
        btnNewAudio = (Button) findViewById(R.id.btnNewAudio);

        adapter = new NotesAdapter();
        listNotes.setAdapter(adapter);

        btnTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storage.setDarkTheme(!storage.isDarkTheme());
                applyCurrentTheme();
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        btnNewText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, NoteEditorActivity.class));
            }
        });

        btnNewTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, TaskEditorActivity.class));
            }
        });

        btnNewAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AudioNoteActivity.class));
            }
        });

        listNotes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note note = displayedNotes.get(position);
                Intent intent;
                if (note.type == NoteType.TASK) {
                    intent = new Intent(MainActivity.this, TaskEditorActivity.class);
                } else if (note.type == NoteType.AUDIO) {
                    intent = new Intent(MainActivity.this, AudioNoteActivity.class);
                } else {
                    intent = new Intent(MainActivity.this, NoteEditorActivity.class);
                }
                intent.putExtra("note_id", note.id);
                startActivity(intent);
            }
        });

        listNotes.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Note note = displayedNotes.get(position);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete")
                        .setMessage("Delete \"" + note.displayTitle() + "\"?")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                repo.delete(note);
                                loadNotes();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterNotes(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        applyCurrentTheme();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyCurrentTheme();
        tvStoragePath.setText("path: " + storage.storageDescription());
        loadNotes();
    }

    private void applyCurrentTheme() {
        ThemeManager.Palette p = ThemeManager.get(storage);

        rootView.setBackgroundColor(p.bg);
        headerBar.setBackgroundColor(p.headerBg);
        tvAppTitle.setTextColor(p.textPrimary);

        btnTheme.setTextColor(p.textSecondary);
        btnTheme.setText(storage.isDarkTheme() ? "[light]" : "[dark]");

        btnSettings.setTextColor(p.textSecondary);
        headerDivider.setBackgroundColor(p.divider);

        tvStoragePath.setTextColor(p.textSecondary);
        tvStoragePath.setBackgroundColor(p.bg);

        searchContainer.setBackgroundColor(p.bg);
        etSearch.setBackgroundDrawable(ThemeManager.createBox(p.inputBg, p.divider, 8));
        etSearch.setTextColor(p.textPrimary);
        etSearch.setHintTextColor(p.textSecondary);

        listNotes.setBackgroundColor(p.bg);
        listNotes.setDivider(new ColorDrawable(p.divider));
        listNotes.setDividerHeight(1);

        tvEmpty.setTextColor(p.textSecondary);

        bottomDivider.setBackgroundColor(p.divider);
        bottomBar.setBackgroundColor(p.headerBg);

        btnNewText.setBackgroundDrawable(ThemeManager.createBox(p.btnBg, p.btnBorder, 6));
        btnNewText.setTextColor(p.textPrimary);

        btnNewTask.setBackgroundDrawable(ThemeManager.createBox(p.btnBg, p.btnBorder, 6));
        btnNewTask.setTextColor(p.textPrimary);

        btnNewAudio.setBackgroundDrawable(ThemeManager.createBox(p.btnBg, p.btnBorder, 6));
        btnNewAudio.setTextColor(p.textPrimary);

        adapter.notifyDataSetChanged();
        FontHelper.apply(rootView, this);
    }

    private void loadNotes() {
        allNotes = repo.loadAll();
        tvAppTitle.setText("NoteVault [" + allNotes.size() + "]");
        filterNotes(etSearch.getText().toString());
    }

    private void filterNotes(String query) {
        displayedNotes.clear();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());
        for (Note n : allNotes) {
            if (q.isEmpty()) {
                displayedNotes.add(n);
            } else {
                boolean match = false;
                if (n.title != null && n.title.toLowerCase(Locale.getDefault()).contains(q)) match = true;
                if (n.content != null && n.content.toLowerCase(Locale.getDefault()).contains(q)) match = true;
                if (n.category != null && n.category.toLowerCase(Locale.getDefault()).contains(q)) match = true;
                if (!match && n.tasks != null) {
                    for (TaskItem ti : n.tasks) {
                        if (ti.text != null && ti.text.toLowerCase(Locale.getDefault()).contains(q)) {
                            match = true;
                            break;
                        }
                    }
                }
                if (match) displayedNotes.add(n);
            }
        }
        adapter.notifyDataSetChanged();
        if (displayedNotes.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            listNotes.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            listNotes.setVisibility(View.VISIBLE);
        }
    }

    private class NotesAdapter extends BaseAdapter {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault());

        @Override
        public int getCount() {
            return displayedNotes.size();
        }

        @Override
        public Object getItem(int position) {
            return displayedNotes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_note, parent, false);
            }
            Note note = displayedNotes.get(position);
            ThemeManager.Palette p = ThemeManager.get(storage);

            convertView.setBackgroundColor(p.bg);

            TextView tvType = (TextView) convertView.findViewById(R.id.tvType);
            TextView tvTitle = (TextView) convertView.findViewById(R.id.tvTitle);
            TextView tvDate = (TextView) convertView.findViewById(R.id.tvDate);
            TextView tvPreview = (TextView) convertView.findViewById(R.id.tvPreview);
            TextView tvCategory = (TextView) convertView.findViewById(R.id.tvCategory);

            tvTitle.setText(note.displayTitle());
            tvTitle.setTextColor(p.textPrimary);

            tvType.setTextColor(p.textSecondary);
            if (note.type == NoteType.TASK) {
                tvType.setText("[tasks]");
            } else if (note.type == NoteType.AUDIO) {
                tvType.setText("[audio]");
            } else {
                tvType.setText("[text]");
            }

            String preview = "";
            if (note.type == NoteType.TEXT) {
                preview = note.content == null ? "" : note.content.replace('\n', ' ');
            } else if (note.type == NoteType.TASK) {
                int total = note.tasks != null ? note.tasks.size() : 0;
                int done = 0;
                if (note.tasks != null) {
                    for (TaskItem t : note.tasks) if (t.done) done++;
                }
                preview = done + "/" + total + " completed";
            } else if (note.type == NoteType.AUDIO) {
                preview = "voice recording";
            }
            tvPreview.setText(preview);
            tvPreview.setTextColor(p.textSecondary);

            if (note.category != null && !note.category.trim().isEmpty()) {
                tvCategory.setText("[#" + note.category.trim() + "]");
                tvCategory.setTextColor(p.textSecondary);
                tvCategory.setVisibility(View.VISIBLE);
            } else {
                tvCategory.setVisibility(View.GONE);
            }

            long dateToFormat = note.createdAt > 0 ? note.createdAt : note.updatedAt;
            tvDate.setText(dateFormat.format(new Date(dateToFormat)));
            tvDate.setTextColor(p.textSecondary);

            FontHelper.apply(convertView, MainActivity.this);

            return convertView;
        }
    }
}
