package com.lumina;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/** Barra de progreso fina y elegante con un punto que crece al arrastrar. */
final class SeekView extends View {
    interface Listener {
        void onScrub(float fraction);
        void onSeek(float fraction);
    }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private Listener listener;
    private float progress;
    private boolean dragging;
    private int accent = Ui.ACCENT;
    private float thumb = 0f; // 0..1 animado

    SeekView(Context c) { super(c); }

    void setListener(Listener l) { listener = l; }

    void setAccent(int c) { accent = c; invalidate(); }

    void setProgress(float f) {
        if (dragging) return;
        progress = Math.max(0f, Math.min(1f, f));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float pad = Ui.dp(10), th = Ui.dp(4);
        float cy = h / 2f;
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x33FFFFFF);
        r.set(pad, cy - th / 2, w - pad, cy + th / 2);
        c.drawRoundRect(r, th, th, p);
        float x = pad + (w - 2 * pad) * progress;
        p.setColor(accent);
        r.set(pad, cy - th / 2, Math.max(pad + th, x), cy + th / 2);
        c.drawRoundRect(r, th, th, p);
        float rad = Ui.dp(5) + Ui.dp(4) * thumb;
        p.setColor(Ui.withAlpha(accent, 70));
        c.drawCircle(x, cy, rad + Ui.dp(4) * thumb, p);
        p.setColor(0xFFFFFFFF);
        c.drawCircle(x, cy, rad, p);
    }

    private float fraction(MotionEvent e) {
        float pad = Ui.dp(10);
        return Math.max(0f, Math.min(1f, (e.getX() - pad) / (getWidth() - 2 * pad)));
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                dragging = true;
                progress = fraction(e);
                animate(1f);
                if (listener != null) listener.onScrub(progress);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                progress = fraction(e);
                if (listener != null) listener.onScrub(progress);
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                progress = fraction(e);
                dragging = false;
                animate(0f);
                if (listener != null) listener.onSeek(progress);
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                animate(0f);
                invalidate();
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    private void animate(float to) {
        android.animation.ValueAnimator a = android.animation.ValueAnimator.ofFloat(thumb, to);
        a.setDuration(140);
        a.addUpdateListener(v -> { thumb = (float) v.getAnimatedValue(); invalidate(); });
        a.start();
    }
}
