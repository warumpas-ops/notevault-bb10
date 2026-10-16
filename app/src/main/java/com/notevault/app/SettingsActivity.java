package com.notevault.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;

public class SettingsActivity extends Activity {

    private StorageManager storage;
    private NoteRepository repo;

    private ScrollView rootScrollView;
    private LinearLayout rootView;
    private LinearLayout headerBar;
    private TextView tvHeaderTitle;
    private Button btnBack;
    private Button btnSave;
    private View headerDivider;

    private TextView tvThemeHeader;
    private LinearLayout themeRow;
    private TextView tvThemeLabel;
    private CheckBox cbDarkMode;
    private View themeDivider;

    private TextView tvStorageHeader;
    private LinearLayout sdRow;
    private TextView tvSdLabel;
    private CheckBox cbSdCard;
    private TextView tvCustomPathLabel;
    private EditText etCustomPath;
    private TextView tvCurrentPath;
    private View storageDivider;

    private TextView tvExportHeader;
    private Button btnExport;
    private TextView tvExportResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        storage = new StorageManager(this);
        repo = new NoteRepository(storage);

        rootScrollView = (ScrollView) findViewById(R.id.rootScrollView);
        rootView = (LinearLayout) findViewById(R.id.rootView);
        headerBar = (LinearLayout) findViewById(R.id.headerBar);
        tvHeaderTitle = (TextView) findViewById(R.id.tvHeaderTitle);
        btnBack = (Button) findViewById(R.id.btnBack);
        btnSave = (Button) findViewById(R.id.btnSave);
        headerDivider = findViewById(R.id.headerDivider);

        tvThemeHeader = (TextView) findViewById(R.id.tvThemeHeader);
        themeRow = (LinearLayout) findViewById(R.id.themeRow);
        tvThemeLabel = (TextView) findViewById(R.id.tvThemeLabel);
        cbDarkMode = (CheckBox) findViewById(R.id.cbDarkMode);
        themeDivider = findViewById(R.id.themeDivider);

        tvStorageHeader = (TextView) findViewById(R.id.tvStorageHeader);
        sdRow = (LinearLayout) findViewById(R.id.sdRow);
        tvSdLabel = (TextView) findViewById(R.id.tvSdLabel);
        cbSdCard = (CheckBox) findViewById(R.id.cbSdCard);
        tvCustomPathLabel = (TextView) findViewById(R.id.tvCustomPathLabel);
        etCustomPath = (EditText) findViewById(R.id.etCustomPath);
        tvCurrentPath = (TextView) findViewById(R.id.tvCurrentPath);
        storageDivider = findViewById(R.id.storageDivider);

        tvExportHeader = (TextView) findViewById(R.id.tvExportHeader);
        btnExport = (Button) findViewById(R.id.btnExport);
        tvExportResult = (TextView) findViewById(R.id.tvExportResult);

        cbDarkMode.setChecked(storage.isDarkTheme());
        cbSdCard.setChecked(storage.isUseSd());
        etCustomPath.setText(storage.getCustomPath());
        updateCurrentPathDisplay();

        cbDarkMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                storage.setDarkTheme(isChecked);
                applyTheme();
            }
        });

        cbSdCard.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                storage.setUseSd(isChecked);
                storage.migrateLegacyNotesIfNeeded();
                updateCurrentPathDisplay();
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
                Toast.makeText(SettingsActivity.this, "Settings saved", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportNotes();
            }
        });

        applyTheme();
    }

    private void applyTheme() {
        ThemeManager.Palette p = ThemeManager.get(storage);

        rootScrollView.setBackgroundColor(p.bg);
        rootView.setBackgroundColor(p.bg);
        headerBar.setBackgroundColor(p.headerBg);
        tvHeaderTitle.setTextColor(p.textPrimary);
        btnBack.setTextColor(p.textSecondary);
        btnSave.setTextColor(p.textPrimary);
        headerDivider.setBackgroundColor(p.divider);

        tvThemeHeader.setTextColor(p.textSecondary);
        themeRow.setBackgroundColor(p.bg);
        tvThemeLabel.setTextColor(p.textPrimary);
        themeDivider.setBackgroundColor(p.divider);

        tvStorageHeader.setTextColor(p.textSecondary);
        sdRow.setBackgroundColor(p.bg);
        tvSdLabel.setTextColor(p.textPrimary);
        tvCustomPathLabel.setTextColor(p.textSecondary);

        etCustomPath.setBackgroundDrawable(ThemeManager.createBox(p.inputBg, p.divider, 6));
        etCustomPath.setTextColor(p.textPrimary);
        etCustomPath.setHintTextColor(p.textSecondary);

        tvCurrentPath.setTextColor(p.textSecondary);
        storageDivider.setBackgroundColor(p.divider);

        tvExportHeader.setTextColor(p.textSecondary);
        btnExport.setBackgroundDrawable(ThemeManager.createBox(p.btnBg, p.btnBorder, 6));
        btnExport.setTextColor(p.textPrimary);

        tvExportResult.setTextColor(p.textSecondary);

        FontHelper.apply(rootView, this);
    }

    private void updateCurrentPathDisplay() {
        tvCurrentPath.setText("active storage path:\n" + storage.storageDescription());
    }

    private void save() {
        storage.setDarkTheme(cbDarkMode.isChecked());
        storage.setUseSd(cbSdCard.isChecked());
        storage.setCustomPath(etCustomPath.getText().toString().trim());
        updateCurrentPathDisplay();
    }

    private void exportNotes() {
        save();
        try {
            File exportFile = new File(storage.notesDir(), "NoteVault_Export_" + System.currentTimeMillis() + ".txt");
            repo.exportAll(exportFile);
            tvExportResult.setText("[ exported successfully to ]\n" + exportFile.getAbsolutePath());
            Toast.makeText(this, "Exported successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            tvExportResult.setText("[ export failed ] " + e.getMessage());
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        save();
    }
}
