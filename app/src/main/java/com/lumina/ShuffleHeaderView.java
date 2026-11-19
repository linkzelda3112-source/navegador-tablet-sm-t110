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
        Icons.draw(c, p, Icons.SHUFFLE, r.left + Ui.dp(34), h / 2f, Ui.dp(30), Ui.ACCENT);
        tp.setTypeface(Ui.MEDIUM);
        tp.setTextSize(Ui.dp(16));
        tp.setColor(Ui.TEXT);
        c.drawText("Reproducir en aleatorio", r.left + Ui.dp(64), h / 2f + Ui.dp(5.5f), tp);
        tp.setTypeface(Ui.REGULAR);
        tp.setTextSize(Ui.dp(13));
        tp.setColor(Ui.TEXT2);
        tp.setTextAlign(Paint.Align.RIGHT);
        c.drawText(String.valueOf(count), r.right - Ui.dp(18), h / 2f + Ui.dp(4.5f), tp);
        tp.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        invalidate();
    }
}
