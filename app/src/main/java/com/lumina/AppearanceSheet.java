package com.lumina;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Hoja inferior de apariencia: tema de colores de la app y fondo de la pantalla "Reproduciendo". */
final class AppearanceSheet extends Dialog {
    interface Callbacks {
        void onTheme(int index);
        void onNowPlayingBg(int style);
        void onBackground();
    }

    /** Muestra de un tema: fondo, tarjeta y punto de acento, con anillo si está elegido. */
    private static final class Swatch extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private final int[] colors;
        private final String name;
        private final boolean selected;

        Swatch(Context c, int[] colors, String name, boolean selected) {
            super(c);
            this.colors = colors;
            this.name = name;
            this.selected = selected;
            setClickable(true);
        }

        @Override protected void onMeasure(int w, int h) {
            setMeasuredDimension(MeasureSpec.getSize(w), Ui.dpi(104));
        }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth();
            float cw = Ui.dp(76), ch = Ui.dp(66);
            r.set((w - cw) / 2f, Ui.dp(4), (w + cw) / 2f, Ui.dp(4) + ch);
            p.setStyle(Paint.Style.FILL);
            p.setColor(colors[0]);
            c.drawRoundRect(r, Ui.dp(18), Ui.dp(18), p);
            RectF card = new RectF(r.left + Ui.dp(10), r.bottom - Ui.dp(26), r.right - Ui.dp(10), r.bottom - Ui.dp(10));
            p.setColor(colors[1]);
            c.drawRoundRect(card, Ui.dp(8), Ui.dp(8), p);
            p.setColor(colors[5]);
            c.drawCircle(r.left + Ui.dp(22), r.top + Ui.dp(22), Ui.dp(8), p);
            p.setColor(colors[3]);
            RectF line = new RectF(r.left + Ui.dp(38), r.top + Ui.dp(17), r.right - Ui.dp(12), r.top + Ui.dp(21));
            c.drawRoundRect(line, Ui.dp(2), Ui.dp(2), p);
            p.setColor(colors[4]);
            line.set(r.left + Ui.dp(38), r.top + Ui.dp(25), r.right - Ui.dp(24), r.top + Ui.dp(28));
            c.drawRoundRect(line, Ui.dp(2), Ui.dp(2), p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(selected ? Ui.dp(2.5f) : Ui.dp(1));
            p.setColor(selected ? Ui.ACCENT : Ui.withAlpha(Ui.TEXT, 50));
            c.drawRoundRect(r, Ui.dp(18), Ui.dp(18), p);
            p.setStyle(Paint.Style.FILL);
            p.setTypeface(selected ? Ui.MEDIUM : Ui.REGULAR);
            p.setTextSize(Ui.dp(13));
            p.setTextAlign(Paint.Align.CENTER);
            p.setColor(selected ? Ui.TEXT : Ui.TEXT2);
            c.drawText(name, w / 2f, r.bottom + Ui.dp(20), p);
        }
    }

    private final Callbacks cb;
    private LinearLayout root;

    AppearanceSheet(Context c, Callbacks cb) {
        super(c);
        this.cb = cb;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(Ui.SURFACE);
        float r = Ui.dp(28);
        g.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        root.setBackground(g);
        root.setPadding(Ui.dpi(22), Ui.dpi(12), Ui.dpi(22), Ui.dpi(28));
        setContentView(root);
        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setGravity(Gravity.BOTTOM);
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        build();
    }

    private void build() {
        Context c = getContext();
        root.removeAllViews();
        View h = new View(c);
        h.setBackground(Ui.shape(Ui.withAlpha(Ui.TEXT, 50), 0, Ui.dp(3)));
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(Ui.dpi(40), Ui.dpi(5));
        hl.gravity = Gravity.CENTER_HORIZONTAL;
        hl.bottomMargin = Ui.dpi(16);
        root.addView(h, hl);
        root.addView(Ui.label(c, "Ajustes", 24, Ui.TEXT, Ui.SERIF));

        TextView s1 = Ui.label(c, "TEMA", 12, Ui.TEXT2, Ui.MEDIUM);
        s1.setLetterSpacing(0.2f);
        s1.setPadding(0, Ui.dpi(18), 0, Ui.dpi(6));
        root.addView(s1);
        int n = Ui.THEME_NAMES.length;
        LinearLayout row = null;
        for (int i = 0; i < n; i++) {
            if (i % 3 == 0) {
                row = new LinearLayout(c);
                row.setOrientation(LinearLayout.HORIZONTAL);
                root.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            final int idx = i;
            Swatch sw = new Swatch(c, Ui.themeColors(i), Ui.THEME_NAMES[i], i == Ui.theme);
            sw.setOnClickListener(v -> {
                if (idx != Ui.theme) {
                    dismiss();
                    cb.onTheme(idx);
                }
            });
            row.addView(sw, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }

        TextView s2 = Ui.label(c, "FONDO DE “REPRODUCIENDO”", 12, Ui.TEXT2, Ui.MEDIUM);
        s2.setLetterSpacing(0.2f);
        s2.setPadding(0, Ui.dpi(18), 0, Ui.dpi(10));
        root.addView(s2);
        LinearLayout seg = new LinearLayout(c);
        seg.setOrientation(LinearLayout.HORIZONTAL);
        String[] names = {"Color de portada", "Portada difuminada", "Liso"};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            boolean sel = i == Ui.npBg;
            TextView t = Ui.label(c, names[i], 13.5f, sel ? Ui.BG : Ui.TEXT, Ui.MEDIUM);
            t.setGravity(Gravity.CENTER);
            t.setClickable(true);
            t.setBackground(sel ? Ui.shape(Ui.ACCENT, 0, Ui.dp(22))
                    : Ui.pressable(Ui.SURFACE2, Ui.blend(Ui.SURFACE2, Ui.TEXT, 0.1f), 0, Ui.dp(22)));
            t.setOnClickListener(v -> {
                Ui.saveNpBg(getContext(), idx);
                cb.onNowPlayingBg(idx);
                build();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dpi(44), 1f);
            lp.rightMargin = i < 2 ? Ui.dpi(8) : 0;
            seg.addView(t, lp);
        }
        root.addView(seg, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView s3 = Ui.label(c, "SEGUNDO PLANO", 12, Ui.TEXT2, Ui.MEDIUM);
        s3.setLetterSpacing(0.2f);
        s3.setPadding(0, Ui.dpi(20), 0, Ui.dpi(6));
        root.addView(s3);
        TextView note = Ui.label(c, "Para que la música no se corte al salir de la app, permite que Lumina funcione "
                + "sin restricciones de batería.", 13, Ui.TEXT2, Ui.REGULAR);
        note.setLineSpacing(Ui.dp(2), 1f);
        root.addView(note);
        TextView btn = Ui.label(c, "Mantener la música en segundo plano", 15, Ui.ACCENT, Ui.MEDIUM);
        btn.setGravity(Gravity.CENTER);
        btn.setClickable(true);
        btn.setBackground(Ui.pressable(0x00000000, Ui.SURFACE2, Ui.withAlpha(Ui.ACCENT, 140), Ui.dp(24)));
        btn.setOnClickListener(v -> cb.onBackground());
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dpi(48));
        bl.topMargin = Ui.dpi(12);
        root.addView(btn, bl);
    }
}
