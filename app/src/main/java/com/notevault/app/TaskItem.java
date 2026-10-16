package com.notevault.app;

public class TaskItem {
    public String text;
    public boolean done;

    public TaskItem(String text, boolean done) {
        this.text = text;
        this.done = done;
    }

    public TaskItem(String text) {
        this(text, false);
    }
}
