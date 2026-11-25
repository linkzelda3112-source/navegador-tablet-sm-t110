package com.lumina;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;

/** Botones en píldora (Inicio, Canciones…): la seleccionada se rellena con degradado y se desliza al cambiar. */
final class TabsView extends View {
    interface Listener { void onTab(int index); }

    private final String[] labels = {"Inicio", "Canciones", "Álbumes", "Artistas"};
    private final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final float[] left = new float[4], right = new float[4];
    private int selected, pressed = -1;
    private float indX = -1, indW;
    private Listener listener;

    TabsView(Context c) { super(c); }

    void setListener(Listener l) { listener = l; }

    void select(int i) {
        if (i == selected) return;
        selected = i;
        animateTo();
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(MeasureSpec.getSize(w), Ui.dpi(60));
    }

    private void layoutChips() {
        float w = getWidth();
        if (w <= 0) w = Ui.dp(360);
        float gap = Ui.dp(8), margin = Ui.dp(18);
        float avail = w - 2 * margin - gap * (labels.length - 1);
        tp.setTypeface(Ui.MEDIUM);
        tp.setTextSize(Ui.dp(14));
        float sum = 0;
        float[] tw = new float[labels.length];
        for (int i = 0; i < labels.length; i++) { tw[i] = tp.measureText(labels[i]); sum += tw[i]; }
        float extra = Math.max(Ui.dp(20), (avail - sum) / labels.length);
        float x = margin;
        for (int i = 0; i < labels.length; i++) {
            left[i] = x;
            right[i] = x + tw[i] + extra;
            x = right[i] + gap;
        }
    }

    private void animateTo() {
        layoutChips();
        final float fromX = indX < 0 ? left[selected] : indX, fromW = indW;
        final float toX = left[selected], toW = right[selected] - left[selected];
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(320);
        a.setInterpolator(new android.view.animation.OvershootInterpolator(0.9f));
        a.addUpdateListener(v -> {
            float f = (float) v.getAnimatedValue();
            indX = fromX + (toX - fromX) * f;
            indW = fromW + (toW - fromW) * f;
            invalidate();
        });
        a.start();
    }

    private static int onAccent() {
        int a = Ui.ACCENT;
        double l = 0.299 * Color.red(a) + 0.587 * Color.green(a) + 0.114 * Color.blue(a);
        return l > 150 ? 0xFF14121C : 0xFFFFFFFF;
    }

    @Override
    protected void onDraw(Canvas c) {
        layoutChips();
        if (indX < 0) {
            indX = left[selected];
            indW = right[selected] - left[selected];
        }
        float h = getHeight(), ch = Ui.dp(42), top = (h - ch) / 2f;
        float rad = ch / 2f;
        for (int i = 0; i < labels.length; i++) {
            r.set(left[i], top, right[i], top + ch);
            p.setStyle(Paint.Style.FILL);
            p.setColor(pressed == i ? Ui.SURFACE2 : Ui.SURFACE);
            c.drawRoundRect(r, rad, rad, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Ui.dp(1));
            p.setColor(Ui.LINE);
            c.drawRoundRect(r, rad, rad, p);
        }
        // píldora seleccionada
        r.set(indX, top, indX + indW, top + ch);
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(r.left, r.top, r.right, r.bottom, Ui.ACCENT,
                Ui.blend(Ui.ACCENT, Ui.light ? 0xFF000000 : 0xFFFFFFFF, 0.22f), Shader.TileMode.CLAMP));
        c.drawRoundRect(r, rad, rad, p);
        p.setShader(null);
        float base = top + ch / 2f + Ui.dp(5);
        tp.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < labels.length; i++) {
            float mid = (left[i] + right[i]) / 2f;
            float cover = Math.max(0f, 1f - Math.abs(mid - (indX + indW / 2f)) / (indW * 0.75f));
            tp.setTypeface(i == selected ? Ui.MEDIUM : Ui.REGULAR);
            tp.setTextSize(Ui.dp(14));
            tp.setColor(Ui.blend(Ui.TEXT2, onAccent(), Math.min(1f, cover)));
            c.drawText(labels[i], mid, base, tp);
        }
        tp.setTextAlign(Paint.Align.LEFT);
    }

    private int hit(float x) {
        for (int i = 0; i < labels.length; i++) if (x >= left[i] - Ui.dp(4) && x <= right[i] + Ui.dp(4)) return i;
        return -1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressed = hit(e.getX());
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
                pressed = -1;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP: {
                int i = hit(e.getX());
                pressed = -1;
                invalidate();
                if (i >= 0 && i != selected) {
                    selected = i;
                    animateTo();
                    if (listener != null) listener.onTab(i);
                }
                return true;
            }
            default:
                return true;
        }
    }
}
