package com.lumina;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.view.View;

/** Botón de cabecera de la lista: reproducir todo en aleatorio. */
final class ShuffleHeaderView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private int count;

    ShuffleHeaderView(Context c) {
        super(c);
        setClickable(true);
    }

    interface OnSettings { void open(); }
    private OnSettings settings;
    private float downX;

    void setOnSettings(OnSettings s) { settings = s; }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent e) {
        if (e.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) downX = e.getX();
        boolean inTune = downX > getWidth() - Ui.dp(76);
        if (inTune && settings != null) {
            if (e.getActionMasked() == android.view.MotionEvent.ACTION_UP) {
                performClick0();
            }
            return true;
        }
        return super.onTouchEvent(e);
    }

    private void performClick0() { settings.open(); }

    void setCount(int n) { count = n; invalidate(); }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(MeasureSpec.getSize(w), Ui.dpi(68));
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        r.set(Ui.dp(20), Ui.dp(8), w - Ui.dp(20), h - Ui.dp(8));
        p.setStyle(Paint.Style.FILL);
        p.setColor(isPressed() ? Ui.SURFACE2 : Ui.SURFACE);
        c.drawRoundRect(r, Ui.dp(18), Ui.dp(18), p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Ui.dp(1));
        p.setColor(Ui.withAlpha(Ui.ACCENT, 90));
        c.drawRoundRect(r, Ui.dp(18), Ui.dp(18), p);
        // destello que cruza la tarjeta cada pocos segundos
        float cyc = (System.nanoTime() / 1e9f) % 5f;
        if (cyc < 1.1f) {
            float f = cyc / 1.1f;
            float gx = r.left + (r.width() + Ui.dp(120)) * f - Ui.dp(60);
            c.save();
            c.clipRect(r);
            p.setShader(new android.graphics.LinearGradient(gx - Ui.dp(60), 0, gx + Ui.dp(60), 0,
                    new int[]{0x00FFFFFF, Ui.withAlpha(Ui.ACCENT, 70), 0x00FFFFFF}, null, android.graphics.Shader.TileMode.CLAMP));
            p.setStyle(Paint.Style.FILL);
            c.drawRect(r, p);
            p.setShader(null);
            c.restore();
        }
        postInvalidateDelayed(cyc < 1.1f ? 16 : 120);
        float pulse = 1f + 0.06f * (float) Math.sin(System.nanoTime() / 1e9f * 2.2f);
        Icons.draw(c, p, Icons.SHUFFLE, r.left + Ui.dp(34), h / 2f, Ui.dp(30) * pulse, Ui.ACCENT);
        tp.setTypeface(Ui.MEDIUM);
        tp.setTextSize(Ui.dp(16));
        tp.setColor(Ui.TEXT);
        c.drawText("Reproducir en aleatorio", r.left + Ui.dp(64), h / 2f + Ui.dp(5.5f), tp);
        tp.setTypeface(Ui.REGULAR);
        tp.setTextSize(Ui.dp(13));
        tp.setColor(Ui.TEXT2);
        tp.setTextAlign(Paint.Align.RIGHT);
        c.drawText(String.valueOf(count), r.right - Ui.dp(58), h / 2f + Ui.dp(4.5f), tp);
        tp.setTextAlign(Paint.Align.LEFT);
        // botón de ajustes (controles deslizantes)
        float cx = r.right - Ui.dp(28);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Ui.dp(1.6f));
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(Ui.withAlpha(Ui.TEXT2, 60));
        c.drawLine(cx - Ui.dp(24), r.top + Ui.dp(10), cx - Ui.dp(24), r.bottom - Ui.dp(10), p);
        p.setColor(Ui.ACCENT);
        float[] ys = {-7, 0, 7}, ks = {-3, 4, -1};
        for (int i = 0; i < 3; i++) {
            float y = h / 2f + Ui.dp(ys[i]);
            c.drawLine(cx - Ui.dp(9), y, cx + Ui.dp(9), y, p);
            p.setStyle(Paint.Style.FILL);
            c.drawCircle(cx + Ui.dp(ks[i]), y, Ui.dp(2.6f), p);
            p.setStyle(Paint.Style.STROKE);
        }
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        invalidate();
    }
}
