package com.notevault.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import java.io.File;

public class StorageManager {

    private static final String PREFS = "notevault_prefs";
    private static final String KEY_USE_SD = "use_sd";
    private static final String KEY_CUSTOM_PATH = "custom_path";
    private static final String KEY_DARK_THEME = "dark_theme";

    private final Context ctx;

    public StorageManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    private SharedPreferences prefs() {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isDarkTheme() {
        return prefs().getBoolean(KEY_DARK_THEME, true);
    }

    public void setDarkTheme(boolean v) {
        prefs().edit().putBoolean(KEY_DARK_THEME, v).commit();
    }

    public boolean isUseSd() {
        return prefs().getBoolean(KEY_USE_SD, false);
    }

    public void setUseSd(boolean v) {
        prefs().edit().putBoolean(KEY_USE_SD, v).commit();
    }

    public String getCustomPath() {
        return prefs().getString(KEY_CUSTOM_PATH, "");
    }

    public void setCustomPath(String path) {
        prefs().edit().putString(KEY_CUSTOM_PATH, path).commit();
    }

    /** Returns the directory where notes are stored. Never returns null — falls back to internal. */
    public File notesDir() {
        // 1. Custom path
        String custom = getCustomPath();
        if (custom != null && !custom.trim().isEmpty()) {
            File f = new File(custom.trim(), "NoteVault");
            if (tryMkdir(f)) return f;
        }

        // 2. SD card
        if (isUseSd()) {
            File sd = findSdCard();
            if (sd != null) {
                File f = new File(sd, "NoteVault");
                if (tryMkdir(f)) return f;
            }
        }

        // 3. External files dir (no permission needed on API 19+)
        File ext = ctx.getExternalFilesDir(null);
        if (ext != null) {
            File f = new File(ext, "NoteVault");
            if (tryMkdir(f)) return f;
        }

        // 4. Internal
        File internal = new File(ctx.getFilesDir(), "NoteVault");
        tryMkdir(internal);
        return internal;
    }

    public File noteFile(String id) {
        return new File(notesDir(), id + ".json");
    }

    public File audioFile(String id) {
        return new File(notesDir(), id + ".3gp");
    }

    public String storageDescription() {
        String custom = getCustomPath();
        if (custom != null && !custom.trim().isEmpty()) {
            File f = new File(custom.trim(), "NoteVault");
            if (f.exists()) return f.getAbsolutePath();
        }
        if (isUseSd()) {
            File sd = findSdCard();
            if (sd != null) {
                File f = new File(sd, "NoteVault");
                if (f.exists() || tryMkdir(f)) return f.getAbsolutePath();
            }
        }
        File ext = ctx.getExternalFilesDir(null);
        if (ext != null) {
            return new File(ext, "NoteVault").getAbsolutePath();
        }
        return new File(ctx.getFilesDir(), "NoteVault").getAbsolutePath();
    }

    private File findSdCard() {
        // BB10 typical paths
        String[] candidates = {
            System.getenv("SECONDARY_STORAGE"),
            "/storage/sdcard1",
            "/removable_sdcard",
            "/mnt/extsd",
            "/mnt/sdcard1"
        };
        for (String path : candidates) {
            if (path == null || path.isEmpty()) continue;
            File f = new File(path);
            if (f.exists() && f.canWrite()) return f;
        }
        // Fallback: check external dirs
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            File[] dirs = ctx.getExternalFilesDirs(null);
            if (dirs != null && dirs.length >= 2 && dirs[1] != null && dirs[1].exists()) {
                return dirs[1].getParentFile().getParentFile().getParentFile().getParentFile();
            }
        }
        return null;
    }

    private boolean tryMkdir(File f) {
        return f.exists() || f.mkdirs();
    }
}
