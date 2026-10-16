package com.notevault.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class StorageManager {

    private static final String PREFS = "notevault_prefs";
    private static final String KEY_USE_SD = "use_sd";
    private static final String KEY_CUSTOM_PATH = "custom_path";
    private static final String KEY_DARK_THEME = "dark_theme";

    private final Context ctx;

    public StorageManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        // Ensure active directory and auto-migrate legacy files on startup
        migrateLegacyNotesIfNeeded();
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
        // Automatically ensure folder exists and migrate notes if toggled
        notesDir();
    }

    public String getCustomPath() {
        return prefs().getString(KEY_CUSTOM_PATH, "");
    }

    public void setCustomPath(String path) {
        prefs().edit().putString(KEY_CUSTOM_PATH, path).commit();
        notesDir();
    }

    /**
     * Returns the active NoteVault folder.
     * Automatically creates the folder if it does not exist.
     * 
     * 1. If custom path is set -> creates custom/NoteVault
     * 2. If Use SD Card is ON  -> creates SD_CARD/NoteVault (e.g. /storage/sdcard1/NoteVault)
     * 3. If Use SD Card is OFF -> creates Phone_Storage/NoteVault (e.g. /sdcard/NoteVault)
     */
    public File notesDir() {
        File targetDir = null;

        // 1. Custom path override
        String custom = getCustomPath();
        if (custom != null && !custom.trim().isEmpty()) {
            File base = new File(custom.trim());
            File f = base.getName().equalsIgnoreCase("NoteVault") ? base : new File(base, "NoteVault");
            if (tryMkdir(f)) {
                targetDir = f;
            }
        }

        // 2. MicroSD card (when toggle is ON)
        if (targetDir == null && isUseSd()) {
            File sd = findSdCard();
            if (sd != null) {
                File f = new File(sd, "NoteVault");
                if (tryMkdir(f)) {
                    targetDir = f;
                }
            }
        }

        // 3. Phone internal storage (when toggle is OFF, or if SD card is missing/unwritable)
        if (targetDir == null) {
            File phoneRoot = getPhoneStorageRoot();
            File f = new File(phoneRoot, "NoteVault");
            if (tryMkdir(f)) {
                targetDir = f;
            }
        }

        // 4. Ultimate fallback: private internal app files
        if (targetDir == null) {
            File internal = new File(ctx.getFilesDir(), "NoteVault");
            tryMkdir(internal);
            targetDir = internal;
        }

        return targetDir;
    }

    public File noteFile(String id) {
        return new File(notesDir(), id + ".json");
    }

    public File audioFile(String id) {
        return new File(notesDir(), id + ".3gp");
    }

    public String storageDescription() {
        File dir = notesDir();
        if (isUseSd()) {
            File sd = findSdCard();
            if (sd != null && dir.getAbsolutePath().startsWith(sd.getAbsolutePath())) {
                return "[microSD] " + dir.getAbsolutePath();
            }
            return "[SD missing -> phone] " + dir.getAbsolutePath();
        }
        return "[phone] " + dir.getAbsolutePath();
    }

    /**
     * Root directory for the phone's user-accessible internal storage.
     * Typically /sdcard or /storage/sdcard0 on BlackBerry 10.
     */
    public File getPhoneStorageRoot() {
        // 1. Standard primary shared storage (visible in BlackBerry File Manager under Device)
        try {
            File ext = Environment.getExternalStorageDirectory();
            if (ext != null && ext.exists() && ext.canWrite()) {
                return ext;
            }
        } catch (Exception e) {}

        // 2. Fallback to external files dir (Android/data/...)
        try {
            File extFiles = ctx.getExternalFilesDir(null);
            if (extFiles != null && extFiles.exists() && extFiles.canWrite()) {
                return extFiles;
            }
        } catch (Exception e) {}

        // 3. Fallback to private internal files
        return ctx.getFilesDir();
    }

    /**
     * Detects external removable MicroSD card on BlackBerry 10 and Android devices.
     */
    public File findSdCard() {
        String[] candidates = {
            System.getenv("SECONDARY_STORAGE"),
            "/storage/sdcard1",
            "/removable_sdcard",
            "/mnt/sdcard1",
            "/mnt/extsd",
            "/storage/extSdCard"
        };

        for (String path : candidates) {
            if (path == null || path.trim().isEmpty()) continue;
            try {
                File f = new File(path.trim());
                if (f.exists() && f.isDirectory() && f.canWrite()) {
                    return f;
                }
            } catch (Exception e) {}
        }

        // Check getExternalFilesDirs on Android 4.4+ if available
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            try {
                File[] dirs = ctx.getExternalFilesDirs(null);
                if (dirs != null && dirs.length >= 2 && dirs[1] != null && dirs[1].exists()) {
                    File cur = dirs[1];
                    for (int i = 0; i < 4 && cur != null; i++) {
                        cur = cur.getParentFile();
                    }
                    if (cur != null && cur.exists() && cur.canWrite()) {
                        return cur;
                    }
                }
            } catch (Exception e) {}
        }

        return null;
    }

    private boolean tryMkdir(File f) {
        if (f == null) return false;
        try {
            if (f.exists()) return f.isDirectory() && f.canWrite();
            return f.mkdirs();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Automatically imports notes from older versions (e.g. from Android/data/... or private files)
     * into the new NoteVault folder so no existing notes are lost when updating!
     */
    public void migrateLegacyNotesIfNeeded() {
        try {
            File currentDir = notesDir();
            if (currentDir == null || !currentDir.exists()) return;

            // Check potential older locations
            File[] legacyDirs = new File[] {
                new File(ctx.getExternalFilesDir(null), "NoteVault"),
                new File(ctx.getFilesDir(), "NoteVault")
            };

            for (File oldDir : legacyDirs) {
                if (oldDir != null && oldDir.exists() && !oldDir.getAbsolutePath().equals(currentDir.getAbsolutePath())) {
                    File[] files = oldDir.listFiles();
                    if (files != null) {
                        for (File src : files) {
                            if (src.isFile() && (src.getName().endsWith(".json") || src.getName().endsWith(".3gp"))) {
                                File dest = new File(currentDir, src.getName());
                                if (!dest.exists()) {
                                    copyFile(src, dest);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Fail silently during background migration check
        }
    }

    public static void copyFile(File src, File dest) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dest);
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (Exception e) {
            // Ignore
        } finally {
            try { if (in != null) in.close(); } catch (Exception e) {}
            try { if (out != null) out.close(); } catch (Exception e) {}
        }
    }
}
