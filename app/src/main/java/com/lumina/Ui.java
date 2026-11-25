package com.lumina;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.widget.TextView;

/** Colores, tipografías y utilidades de dibujo compartidas. */
final class Ui {
    private Ui() {}

    static int BG, SURFACE, SURFACE2, TEXT, TEXT2, ACCENT, LINE;
    static boolean light;
    static int theme, npBg;

    /** Esquema oscuro fijo para el mini reproductor y "Reproduciendo", que van sobre portadas. */
    static final int DK_BG = 0xFF0A0914, DK_SURFACE2 = 0xFF1E1B2E, DK_TEXT = 0xFFF5F2FF, DK_TEXT2 = 0xFF9E9BB4;

    static final String[] THEME_NAMES = {"Aurora", "Atardecer", "Neón", "Selva", "Chicle", "Perla"};
    // fondo, tarjeta, tarjeta2, texto, texto2, acento, línea
    private static final int[][] THEMES = {
        {0xFF0D0A1F, 0xFF171233, 0xFF221B48, 0xFFF4F1FF, 0xFFA39FC4, 0xFFB388FF, 0x1AFFFFFF},
        {0xFF160B12, 0xFF22121C, 0xFF331A28, 0xFFFFF1EA, 0xFFC09A9A, 0xFFFF7A59, 0x1AFFFFFF},
        {0xFF05070D, 0xFF0D1220, 0xFF141C33, 0xFFEAF6FF, 0xFF7F93B3, 0xFF00E5FF, 0x1AFFFFFF},
        {0xFF06130F, 0xFF0C2019, 0xFF133229, 0xFFEAFBF3, 0xFF86B3A0, 0xFF3DDC97, 0x1AFFFFFF},
        {0xFF1A0A18, 0xFF2A1226, 0xFF3D1A37, 0xFFFFEDF8, 0xFFC596B6, 0xFFFF5CAA, 0x1AFFFFFF},
        {0xFFF6F3FB, 0xFFFFFFFF, 0xFFECE6F7, 0xFF1C1830, 0xFF77708F, 0xFF7C4DFF, 0x14000000},
    };

    static int[] themeColors(int i) { return THEMES[i]; }

    static void apply(int i) {
        int[] t = THEMES[Math.max(0, Math.min(THEMES.length - 1, i))];
        theme = Math.max(0, Math.min(THEMES.length - 1, i));
        BG = t[0]; SURFACE = t[1]; SURFACE2 = t[2]; TEXT = t[3]; TEXT2 = t[4]; ACCENT = t[5]; LINE = t[6];
        light = Color.red(BG) + Color.green(BG) + Color.blue(BG) > 3 * 128;
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("lumina_ui", Context.MODE_PRIVATE);
    }

    static void saveTheme(Context c, int i) {
        apply(i);
        prefs(c).edit().putInt("theme", i).apply();
    }

    static void saveNpBg(Context c, int i) {
        npBg = i;
        prefs(c).edit().putInt("npBg", i).apply();
    }

    static Typeface SERIF, MEDIUM, LIGHT, REGULAR;
    private static float density = 3f;

    static void init(Context c) {
        density = c.getResources().getDisplayMetrics().density;
        SharedPreferences sp = prefs(c);
        apply(sp.getInt("theme", 0));
        npBg = sp.getInt("npBg", 0);
        SERIF = Typeface.create("serif", Typeface.NORMAL);
        MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        LIGHT = Typeface.create("sans-serif-light", Typeface.NORMAL);
        REGULAR = Typeface.create("sans-serif", Typeface.NORMAL);
    }

    static float dp(float v) { return v * density; }

    static int dpi(float v) { return Math.round(v * density); }

    static String fmt(long ms) {
        long s = Math.max(0, ms) / 1000;
        long m = s / 60;
        s = s % 60;
        if (m >= 60) {
            long h = m / 60;
            m = m % 60;
            return h + ":" + (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
        }
        return m + ":" + (s < 10 ? "0" : "") + s;
    }

    static int blend(int a, int b, float t) {
        int ar = Color.alpha(a), rr = Color.red(a), gg = Color.green(a), bb = Color.blue(a);
        int br = Color.alpha(b), rr2 = Color.red(b), gg2 = Color.green(b), bb2 = Color.blue(b);
        return Color.argb(
                Math.round(ar + (br - ar) * t), Math.round(rr + (rr2 - rr) * t),
                Math.round(gg + (gg2 - gg) * t), Math.round(bb + (bb2 - bb) * t));
    }

    static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    /** Color oscuro y apagado derivado del texto (para portadas sin imagen). */
    static int seedColor(String seed) {
        int h = (seed == null ? 0 : seed.hashCode()) & 0x7FFFFFFF;
        float[] hsv = {h % 360, 0.42f, 0.42f};
        return Color.HSVToColor(hsv);
    }

    /** Color dominante de una portada, ajustado para usarse de fondo elegante. */
    static int dominant(Bitmap bm, String seed) {
        if (bm == null) return seedColor(seed);
        try {
            Bitmap s = Bitmap.createScaledBitmap(bm, 16, 16, true);
            float[] hsv = new float[3];
            double r = 0, g = 0, b = 0, wsum = 0;
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int px = s.getPixel(x, y);
                    Color.colorToHSV(px, hsv);
                    double w = hsv[1] * (0.3 + hsv[2]) + 0.05;
                    r += Color.red(px) * w;
                    g += Color.green(px) * w;
                    b += Color.blue(px) * w;
                    wsum += w;
                }
            }
            if (s != bm) s.recycle();
            int avg = Color.rgb((int) (r / wsum), (int) (g / wsum), (int) (b / wsum));
            Color.colorToHSV(avg, hsv);
            hsv[1] = Math.max(0.30f, Math.min(0.80f, hsv[1]));
            hsv[2] = Math.max(0.26f, Math.min(0.52f, hsv[2]));
            return Color.HSVToColor(hsv);
        } catch (Throwable t) {
            return seedColor(seed);
        }
    }

    private static final Path clip = new Path();
    private static final Rect src = new Rect();

    /** Dibuja la portada (recortada al centro y redondeada) o un marcador elegante si no hay imagen. */
    static void drawArt(Canvas c, Paint p, Bitmap bm, String seed, RectF dst, float radius) {
        c.save();
        clip.reset();
        clip.addRoundRect(dst, radius, radius, Path.Direction.CW);
        c.clipPath(clip);
        p.setStyle(Paint.Style.FILL);
        if (bm != null) {
            int bw = bm.getWidth(), bh = bm.getHeight();
            int side = Math.min(bw, bh);
            src.set((bw - side) / 2, (bh - side) / 2, (bw + side) / 2, (bh + side) / 2);
            p.setColor(0xFFFFFFFF);
            p.setFilterBitmap(true);
            c.drawBitmap(bm, src, dst, p);
        } else {
            int base = seedColor(seed);
            p.setColor(base);
            c.drawRect(dst, p);
            p.setColor(withAlpha(blend(base, 0xFFFFFFFF, 0.35f), 70));
            c.drawCircle(dst.left + dst.width() * 0.8f, dst.top + dst.height() * 0.2f, dst.width() * 0.6f, p);
            p.setColor(withAlpha(0xFF000000, 60));
            c.drawCircle(dst.left + dst.width() * 0.1f, dst.bottom, dst.width() * 0.55f, p);
            Icons.draw(c, p, Icons.NOTE, dst.centerX(), dst.centerY(), dst.width() * 0.42f, 0x99FFFFFF);
        }
        c.restore();
    }

    static Drawable shape(int fill, int stroke, float radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radiusPx);
        if (stroke != 0) g.setStroke(dpi(1), stroke);
        return g;
    }

    static Drawable pressable(int fill, int pressed, int stroke, float radiusPx) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, shape(pressed, stroke, radiusPx));
        s.addState(new int[]{}, shape(fill, stroke, radiusPx));
        return s;
    }

    static TextView label(Context c, String text, float sizeDp, int color, Typeface tf) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, dp(sizeDp));
        t.setTextColor(color);
        t.setTypeface(tf);
        t.setIncludeFontPadding(false);
        return t;
    }
}
