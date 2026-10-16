package com.notevault.app;

import java.util.ArrayList;
import java.util.List;

public class Note {
    public String id;
    public String title;
    public String content;
    public NoteType type;
    public long createdAt;
    public long updatedAt;
    public List<TaskItem> tasks;
    public String audioFileName;
    public String category;

    public Note() {
        this.tasks = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public Note(NoteType type) {
        this();
        this.id = "note_" + System.currentTimeMillis();
        this.type = type;
        this.title = "";
        this.content = "";
        this.category = "";
    }

    public String displayTitle() {
        if (title != null && !title.trim().isEmpty()) return title;
        if (content != null && !content.trim().isEmpty()) {
            String s = content.trim();
            return s.length() > 40 ? s.substring(0, 40) + "…" : s;
        }
        switch (type) {
            case AUDIO: return "Voice Memo";
            case TASK:  return "Task List";
            default:    return "Untitled";
        }
    }

    public String typeLabel() {
        switch (type) {
            case AUDIO: return "Audio";
            case TASK:  return "Tasks";
            default:    return "Text";
        }
    }
}
