package com.notevault.app;

import android.app.Activity;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;

public class TaskEditorActivity extends Activity {

    private StorageManager storage;
    private NoteRepository repo;
    private Note note;
    private EditText etTitle;
    private EditText etCategory;
    private EditText etNewTask;
    private ListView listTasks;
    private TaskAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_editor);

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
        LinearLayout addTaskRow = (LinearLayout) findViewById(R.id.addTaskRow);
        View taskDivider = findViewById(R.id.taskDivider);

        etTitle = (EditText) findViewById(R.id.etTitle);
        etCategory = (EditText) findViewById(R.id.etCategory);
        etNewTask = (EditText) findViewById(R.id.etNewTask);
        Button btnAddTask = (Button) findViewById(R.id.btnAddTask);
        listTasks = (ListView) findViewById(R.id.listTasks);

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

        addTaskRow.setBackgroundColor(p.bg);
        etNewTask.setBackgroundDrawable(ThemeManager.createBox(p.inputBg, p.divider, 6));
        etNewTask.setTextColor(p.textPrimary);
        etNewTask.setHintTextColor(p.textSecondary);

        btnAddTask.setBackgroundDrawable(ThemeManager.createBox(p.btnBg, p.btnBorder, 6));
        btnAddTask.setTextColor(p.textPrimary);

        taskDivider.setBackgroundColor(p.divider);
        listTasks.setBackgroundColor(p.bg);
        listTasks.setDivider(new ColorDrawable(p.divider));
        listTasks.setDividerHeight(1);

        String noteId = getIntent().getStringExtra("note_id");
        if (noteId != null && !noteId.isEmpty()) {
            File f = storage.noteFile(noteId);
            if (f.exists()) {
                try {
                    note = repo.load(f);
                } catch (Exception e) {
                    note = new Note(NoteType.TASK);
                }
            } else {
                note = new Note(NoteType.TASK);
            }
        } else {
            note = new Note(NoteType.TASK);
        }

        if (note.title != null) etTitle.setText(note.title);
        if (note.category != null) etCategory.setText(note.category);

        adapter = new TaskAdapter();
        listTasks.setAdapter(adapter);

        btnAddTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String itemText = etNewTask.getText().toString().trim();
                if (!itemText.isEmpty()) {
                    note.tasks.add(new TaskItem(itemText, false));
                    etNewTask.setText("");
                    adapter.notifyDataSetChanged();
                }
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
                Toast.makeText(TaskEditorActivity.this, "Saved", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        FontHelper.apply(rootView, this);
    }

    private void save() {
        if (note == null) return;
        String title = etTitle.getText().toString().trim();
        String cat = etCategory.getText().toString().trim();

        if (title.isEmpty() && note.tasks.isEmpty() && cat.isEmpty()) {
            return;
        }

        note.title = title;
        note.category = cat;

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

    private class TaskAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return note.tasks != null ? note.tasks.size() : 0;
        }

        @Override
        public Object getItem(int position) {
            return note.tasks.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(TaskEditorActivity.this).inflate(R.layout.item_task, parent, false);
            }
            final TaskItem item = note.tasks.get(position);
            final ThemeManager.Palette p = ThemeManager.get(storage);

            convertView.setBackgroundColor(p.bg);

            CheckBox cb = (CheckBox) convertView.findViewById(R.id.cbTask);
            final TextView tv = (TextView) convertView.findViewById(R.id.tvTaskText);
            Button btnDel = (Button) convertView.findViewById(R.id.btnDeleteTask);

            tv.setText(item.text);
            btnDel.setTextColor(p.textSecondary);

            cb.setOnCheckedChangeListener(null);
            cb.setChecked(item.done);
            applyStrikethrough(tv, item.done, p);

            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    item.done = isChecked;
                    applyStrikethrough(tv, isChecked, p);
                }
            });

            btnDel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    note.tasks.remove(position);
                    notifyDataSetChanged();
                }
            });

            FontHelper.apply(convertView, TaskEditorActivity.this);
            return convertView;
        }

        private void applyStrikethrough(TextView tv, boolean isDone, ThemeManager.Palette p) {
            if (isDone) {
                tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tv.setTextColor(p.textSecondary);
            } else {
                tv.setPaintFlags(tv.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tv.setTextColor(p.textPrimary);
            }
        }
    }
}
