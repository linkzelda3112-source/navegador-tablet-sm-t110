package com.lumina;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;

/** Pestañas de texto con un pequeño trazo dorado que se desliza bajo la seleccionada. */
final class TabsView extends View {
    interface Listener { void onTab(int index); }

    private final String[] labels = {"Inicio", "Canciones", "Álbumes", "Artistas"};
    private final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final float[] left = new float[4], right = new float[4];
    private int selected;
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
        setMeasuredDimension(MeasureSpec.getSize(w), Ui.dpi(46));
    }

    private void layoutLabels() {
        float x = Ui.dp(24);
        for (int i = 0; i < labels.length; i++) {
            tp.setTypeface(Ui.MEDIUM);
            tp.setTextSize(Ui.dp(15));
            float w = tp.measureText(labels[i]);
            left[i] = x;
            right[i] = x + w;
            x += w + Ui.dp(20);
        }
    }

    private void animateTo() {
        layoutLabels();
        final float fromX = indX < 0 ? left[selected] : indX, fromW = indW;
        final float toX = (left[selected] + right[selected]) / 2f - Ui.dp(10), toW = Ui.dp(20);
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(240);
        a.addUpdateListener(v -> {
            float f = (float) v.getAnimatedValue();
            indX = fromX + (toX - fromX) * f;
            indW = fromW + (toW - fromW) * f;
            invalidate();
        });
        a.start();
    }

    @Override
    protected void onDraw(Canvas c) {
        layoutLabels();
        if (indX < 0) {
            indX = (left[selected] + right[selected]) / 2f - Ui.dp(10);
            indW = Ui.dp(20);
        }
        float base = getHeight() / 2f + Ui.dp(2);
        for (int i = 0; i < labels.length; i++) {
            tp.setTypeface(i == selected ? Ui.MEDIUM : Ui.REGULAR);
            tp.setTextSize(Ui.dp(15));
            tp.setColor(i == selected ? Ui.TEXT : Ui.TEXT2);
            c.drawText(labels[i], left[i], base, tp);
        }
        p.setStyle(Paint.Style.FILL);
        p.setColor(Ui.ACCENT);
        r.set(indX, getHeight() - Ui.dp(9), indX + indW, getHeight() - Ui.dp(6.5f));
        c.drawRoundRect(r, Ui.dp(2), Ui.dp(2), p);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) return true;
        if (e.getActionMasked() == MotionEvent.ACTION_UP) {
            layoutLabels();
            for (int i = 0; i < labels.length; i++) {
                if (e.getX() >= left[i] - Ui.dp(14) && e.getX() <= right[i] + Ui.dp(14)) {
                    if (i != selected) {
                        selected = i;
                        animateTo();
                        if (listener != null) listener.onTab(i);
                    }
                    break;
                }
            }
            return true;
        }
        return super.onTouchEvent(e);
    }
}
