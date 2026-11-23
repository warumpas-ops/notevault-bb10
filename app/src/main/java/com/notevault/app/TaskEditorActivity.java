package com.notevault.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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

    private View rootView;
    private LinearLayout headerBar;
    private Button btnBack;
    private TextView tvHeaderTitle;
    private TextView tvCategoryBadge;
    private Button btnEditMeta;
    private View headerDivider;

    private ListView listTasks;
    private TextView tvEmptyTasks;
    private TaskAdapter adapter;

    private View bottomDivider;
    private LinearLayout inlineAddRow;
    private EditText etNewTask;
    private Button btnSubmitTask;
    private Button btnCloseAddRow;

    private LinearLayout bottomBar;
    private Button btnOpenAdd;
    private TextView tvTaskStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_editor);

        storage = new StorageManager(this);
        repo = new NoteRepository(storage);

        rootView = findViewById(R.id.rootView);
        headerBar = (LinearLayout) findViewById(R.id.headerBar);
        btnBack = (Button) findViewById(R.id.btnBack);
        tvHeaderTitle = (TextView) findViewById(R.id.tvHeaderTitle);
        tvCategoryBadge = (TextView) findViewById(R.id.tvCategoryBadge);
        btnEditMeta = (Button) findViewById(R.id.btnEditMeta);
        headerDivider = findViewById(R.id.headerDivider);

        listTasks = (ListView) findViewById(R.id.listTasks);
        tvEmptyTasks = (TextView) findViewById(R.id.tvEmptyTasks);

        bottomDivider = findViewById(R.id.bottomDivider);
        inlineAddRow = (LinearLayout) findViewById(R.id.inlineAddRow);
        etNewTask = (EditText) findViewById(R.id.etNewTask);
        btnSubmitTask = (Button) findViewById(R.id.btnSubmitTask);
        btnCloseAddRow = (Button) findViewById(R.id.btnCloseAddRow);

        bottomBar = (LinearLayout) findViewById(R.id.bottomBar);
        btnOpenAdd = (Button) findViewById(R.id.btnOpenAdd);
        tvTaskStats = (TextView) findViewById(R.id.tvTaskStats);

        applyTheme();

        String noteId = getIntent().getStringExtra("note_id");
        boolean isNew = false;
        if (noteId != null && !noteId.isEmpty()) {
            File f = storage.noteFile(noteId);
            if (f.exists()) {
                try {
                    note = repo.load(f);
                } catch (Exception e) {
                    note = new Note(NoteType.TASK);
                    isNew = true;
                }
            } else {
                note = new Note(NoteType.TASK);
                isNew = true;
            }
        } else {
            note = new Note(NoteType.TASK);
            isNew = true;
        }

        updateMetaDisplay();

        adapter = new TaskAdapter();
        listTasks.setAdapter(adapter);

        updateStats();

        // Listeners
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
                finish();
            }
        });

        btnEditMeta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditMetaDialog();
            }
        });

        findViewById(R.id.titleHeaderGroup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditMetaDialog();
            }
        });

        btnOpenAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddRow(true);
            }
        });

        btnCloseAddRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddRow(false);
            }
        });

        btnSubmitTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitNewTask();
            }
        });

        etNewTask.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    submitNewTask();
                    return true;
                }
                return false;
            }
        });

        // If brand new, open add row immediately
        if (isNew && note.tasks.isEmpty()) {
            showAddRow(true);
        }

        FontHelper.apply(rootView, this);
    }

    private void applyTheme() {
        ThemeManager.Palette p = ThemeManager.get(storage);
        rootView.setBackgroundColor(p.bg);
        headerBar.setBackgroundColor(p.headerBg);
        tvHeaderTitle.setTextColor(p.textPrimary);
        tvCategoryBadge.setTextColor(p.textSecondary);
        tvCategoryBadge.setBackgroundDrawable(ThemeManager.createBox(p.inputBg, p.divider, 4));
        btnBack.setTextColor(p.textSecondary);
        btnEditMeta.setTextColor(p.textPrimary);
        headerDivider.setBackgroundColor(p.divider);

        listTasks.setBackgroundColor(p.bg);
        listTasks.setDivider(new ColorDrawable(p.divider));
        listTasks.setDividerHeight(1);
        tvEmptyTasks.setTextColor(p.textSecondary);

        bottomDivider.setBackgroundColor(p.divider);
        inlineAddRow.setBackgroundColor(p.headerBg);
        etNewTask.setBackgroundDrawable(ThemeManager.createBox(p.inputBg, p.divider, 6));
        etNewTask.setTextColor(p.textPrimary);
        etNewTask.setHintTextColor(p.textSecondary);

        btnSubmitTask.setBackgroundDrawable(ThemeManager.createBox(p.btnBg, p.btnBorder, 6));
        btnSubmitTask.setTextColor(p.textPrimary);
        btnCloseAddRow.setTextColor(p.textSecondary);

        bottomBar.setBackgroundColor(p.headerBg);
        btnOpenAdd.setTextColor(p.textPrimary);
        tvTaskStats.setTextColor(p.textSecondary);
    }

    private void updateMetaDisplay() {
        if (note.title != null && !note.title.trim().isEmpty()) {
            tvHeaderTitle.setText(note.title.trim());
        } else {
            tvHeaderTitle.setText("Untitled List");
        }

        if (note.category != null && !note.category.trim().isEmpty()) {
            String cat = note.category.trim();
            if (!cat.startsWith("#")) cat = "#" + cat;
            tvCategoryBadge.setText(cat);
            tvCategoryBadge.setVisibility(View.VISIBLE);
        } else {
            tvCategoryBadge.setVisibility(View.GONE);
        }
    }

    private void updateStats() {
        int total = note.tasks != null ? note.tasks.size() : 0;
        int done = 0;
        if (note.tasks != null) {
            for (TaskItem t : note.tasks) {
                if (t.done) done++;
            }
        }
        tvTaskStats.setText(done + " / " + total + " done");
        tvEmptyTasks.setVisibility(total == 0 ? View.VISIBLE : View.GONE);
    }

    private void showAddRow(boolean show) {
        if (show) {
            inlineAddRow.setVisibility(View.VISIBLE);
            etNewTask.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etNewTask, InputMethodManager.SHOW_IMPLICIT);
            }
        } else {
            inlineAddRow.setVisibility(View.GONE);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(etNewTask.getWindowToken(), 0);
            }
        }
    }

    private void submitNewTask() {
        String itemText = etNewTask.getText().toString().trim();
        if (!itemText.isEmpty()) {
            note.tasks.add(new TaskItem(itemText, false));
            etNewTask.setText("");
            adapter.notifyDataSetChanged();
            updateStats();
            listTasks.setSelection(adapter.getCount() - 1);
            save();
        }
    }

    private void showEditMetaDialog() {
        ThemeManager.Palette p = ThemeManager.get(storage);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("[edit list details]");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 16, 24, 8);

        final EditText inputTitle = new EditText(this);
        inputTitle.setHint("title (e.g. Groceries)");
        inputTitle.setText(note.title == null ? "" : note.title);
        inputTitle.setTextColor(p.textPrimary);
        inputTitle.setHintTextColor(p.textSecondary);
        inputTitle.setSingleLine(true);
        layout.addView(inputTitle);

        final EditText inputCategory = new EditText(this);
        inputCategory.setHint("category (optional, e.g. home)");
        inputCategory.setText(note.category == null ? "" : note.category);
        inputCategory.setTextColor(p.textPrimary);
        inputCategory.setHintTextColor(p.textSecondary);
        inputCategory.setSingleLine(true);
        layout.addView(inputCategory);

        builder.setView(layout);

        builder.setPositiveButton("[save]", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                note.title = inputTitle.getText().toString().trim();
                note.category = inputCategory.getText().toString().trim();
                updateMetaDisplay();
                save();
            }
        });
        builder.setNegativeButton("[cancel]", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Physical keyboard shortcut 'A' opens quick add row
        if (keyCode == KeyEvent.KEYCODE_A && inlineAddRow.getVisibility() != View.VISIBLE) {
            showAddRow(true);
            return true;
        }
        // Physical keyboard shortcut 'E' edits details
        if (keyCode == KeyEvent.KEYCODE_E && inlineAddRow.getVisibility() != View.VISIBLE) {
            showEditMetaDialog();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK && inlineAddRow.getVisibility() == View.VISIBLE) {
            showAddRow(false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void save() {
        if (note == null) return;
        boolean hasTitle = note.title != null && !note.title.trim().isEmpty();
        boolean hasTasks = note.tasks != null && !note.tasks.isEmpty();
        boolean hasCat = note.category != null && !note.category.trim().isEmpty();

        if (!hasTitle && !hasTasks && !hasCat) {
            return;
        }
        if (!hasTitle) {
            note.title = "Untitled List";
        }

        try {
            repo.save(note);
        } catch (Exception e) {
            // silent save
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

            // Row click toggles done directly
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    item.done = !item.done;
                    applyStrikethrough(tv, item.done, p);
                    notifyDataSetChanged();
                    updateStats();
                    save();
                }
            });

            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    item.done = isChecked;
                    applyStrikethrough(tv, isChecked, p);
                    updateStats();
                    save();
                }
            });

            btnDel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    note.tasks.remove(position);
                    notifyDataSetChanged();
                    updateStats();
                    save();
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
