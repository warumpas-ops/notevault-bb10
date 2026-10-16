package com.notevault.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;

public class NoteEditorActivity extends Activity {

    private StorageManager storage;
    private NoteRepository repo;
    private Note note;
    private EditText etTitle;
    private EditText etCategory;
    private EditText etContent;

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
        if (noteId != null && !noteId.isEmpty()) {
            File f = storage.noteFile(noteId);
            if (f.exists()) {
                try {
                    note = repo.load(f);
                } catch (Exception e) {
                    note = new Note(NoteType.TEXT);
                }
            } else {
                note = new Note(NoteType.TEXT);
            }
        } else {
            note = new Note(NoteType.TEXT);
        }

        if (note.title != null) etTitle.setText(note.title);
        if (note.category != null) etCategory.setText(note.category);
        if (note.content != null) etContent.setText(note.content);

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
                Toast.makeText(NoteEditorActivity.this, "Saved", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        FontHelper.apply(rootView, this);
    }

    private void save() {
        if (note == null) return;
        String title = etTitle.getText().toString().trim();
        String cat = etCategory.getText().toString().trim();
        String content = etContent.getText().toString();

        if (title.isEmpty() && content.trim().isEmpty() && cat.isEmpty()) {
            return;
        }

        note.title = title;
        note.category = cat;
        note.content = content;

        try {
            repo.save(note);
        } catch (Exception e) {
            Toast.makeText(this, "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        save();
    }
}
