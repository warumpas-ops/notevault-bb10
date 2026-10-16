package com.notevault.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ThemeManager {

    public static class Palette {
        public final int bg;
        public final int headerBg;
        public final int surface;
        public final int inputBg;
        public final int textPrimary;
        public final int textSecondary;
        public final int divider;
        public final int btnBg;
        public final int btnBorder;
        public final int itemPressed;

        public Palette(boolean isDark) {
            if (isDark) {
                bg = Color.parseColor("#0F0F10");
                headerBg = Color.parseColor("#141416");
                surface = Color.parseColor("#171719");
                inputBg = Color.parseColor("#1C1C1F");
                textPrimary = Color.parseColor("#F2F2F2");
                textSecondary = Color.parseColor("#808086");
                divider = Color.parseColor("#252528");
                btnBg = Color.parseColor("#1F1F23");
                btnBorder = Color.parseColor("#323237");
                itemPressed = Color.parseColor("#2A2A30");
            } else {
                bg = Color.parseColor("#F7F7F8");
                headerBg = Color.parseColor("#FFFFFF");
                surface = Color.parseColor("#FFFFFF");
                inputBg = Color.parseColor("#EDEDF0");
                textPrimary = Color.parseColor("#141416");
                textSecondary = Color.parseColor("#787880");
                divider = Color.parseColor("#E4E4E8");
                btnBg = Color.parseColor("#EBEBF0");
                btnBorder = Color.parseColor("#D4D4D8");
                itemPressed = Color.parseColor("#E0E0E5");
            }
        }
    }

    public static Palette get(StorageManager storage) {
        return new Palette(storage.isDarkTheme());
    }

    public static GradientDrawable createBox(int bgColor, int borderColor, int radiusPx) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bgColor);
        if (borderColor != 0) {
            gd.setStroke(1, borderColor);
        }
        if (radiusPx > 0) {
            gd.setCornerRadius(radiusPx);
        }
        return gd;
    }
}
