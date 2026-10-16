package com.notevault.app;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NoteRepository {

    private final StorageManager storage;

    public NoteRepository(StorageManager storage) {
        this.storage = storage;
    }

    public void save(Note note) throws Exception {
        note.updatedAt = System.currentTimeMillis();
        JSONObject obj = new JSONObject();
        obj.put("id", note.id);
        obj.put("title", note.title == null ? "" : note.title);
        obj.put("content", note.content == null ? "" : note.content);
        obj.put("type", note.type.name());
        obj.put("createdAt", note.createdAt);
        obj.put("updatedAt", note.updatedAt);
        obj.put("audioFileName", note.audioFileName == null ? "" : note.audioFileName);
        obj.put("category", note.category == null ? "" : note.category);
        JSONArray tasks = new JSONArray();
        if (note.tasks != null) {
            for (TaskItem t : note.tasks) {
                JSONObject to = new JSONObject();
                to.put("text", t.text == null ? "" : t.text);
                to.put("done", t.done);
                tasks.put(to);
            }
        }
        obj.put("tasks", tasks);

        File f = storage.noteFile(note.id);
        FileWriter fw = new FileWriter(f);
        fw.write(obj.toString());
        fw.close();
    }

    public Note load(File f) throws Exception {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        JSONObject obj = new JSONObject(sb.toString());
        Note note = new Note();
        note.id = obj.optString("id", "");
        note.title = obj.optString("title", "");
        note.content = obj.optString("content", "");
        note.type = NoteType.valueOf(obj.optString("type", "TEXT"));
        note.createdAt = obj.optLong("createdAt", 0);
        note.updatedAt = obj.optLong("updatedAt", 0);
        note.audioFileName = obj.optString("audioFileName", "");
        note.category = obj.optString("category", "");
        JSONArray tasks = obj.optJSONArray("tasks");
        if (tasks != null) {
            for (int i = 0; i < tasks.length(); i++) {
                JSONObject to = tasks.getJSONObject(i);
                note.tasks.add(new TaskItem(to.optString("text", ""), to.optBoolean("done", false)));
            }
        }
        return note;
    }

    public List<Note> loadAll() {
        List<Note> notes = new ArrayList<>();
        File dir = storage.notesDir();
        File[] files = dir.listFiles();
        if (files == null) return notes;
        for (File f : files) {
            if (f.getName().endsWith(".json")) {
                try {
                    notes.add(load(f));
                } catch (Exception e) {
                    // skip corrupt files
                }
            }
        }
        Collections.sort(notes, new Comparator<Note>() {
            @Override
            public int compare(Note a, Note b) {
                return Long.compare(b.updatedAt, a.updatedAt);
            }
        });
        return notes;
    }

    public void delete(Note note) {
        File f = storage.noteFile(note.id);
        if (f.exists()) f.delete();
        if (note.audioFileName != null && !note.audioFileName.isEmpty()) {
            File audio = new File(storage.notesDir(), note.audioFileName);
            if (audio.exists()) audio.delete();
        }
    }

    public void exportAll(File exportFile) throws Exception {
        List<Note> notes = loadAll();
        StringBuilder sb = new StringBuilder();
        sb.append("NoteVault Export\n");
        sb.append("================\n\n");
        for (Note n : notes) {
            sb.append("Title: ").append(n.displayTitle()).append("\n");
            sb.append("Type: ").append(n.typeLabel()).append("\n");
            if (n.category != null && !n.category.isEmpty())
                sb.append("Category: ").append(n.category).append("\n");
            sb.append("---\n");
            if (n.type == NoteType.TEXT) {
                sb.append(n.content).append("\n");
            } else if (n.type == NoteType.TASK) {
                for (TaskItem t : n.tasks) {
                    sb.append(t.done ? "[x] " : "[ ] ").append(t.text).append("\n");
                }
            } else if (n.type == NoteType.AUDIO) {
                sb.append("[Voice memo: ").append(n.audioFileName).append("]\n");
            }
            sb.append("\n");
        }
        FileWriter fw = new FileWriter(exportFile);
        fw.write(sb.toString());
        fw.close();
    }
}
