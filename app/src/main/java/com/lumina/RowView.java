package com.lumina;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;

/** Una fila de lista dibujada a mano: portada, título, subtítulo y duración (o ecualizador si suena). */
final class RowView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();

    Track artTrack;
    String title = "", sub = "", right = "";
    boolean round, current, playing;
    int rating;
    ArtCache art;
    Runnable artCb;

    RowView(Context c) { super(c); }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        animate().scaleX(pressed ? 0.97f : 1f).scaleY(pressed ? 0.97f : 1f).setDuration(pressed ? 90 : 220).start();
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        setMeasuredDimension(MeasureSpec.getSize(wSpec), Ui.dpi(72));
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float pad = Ui.dp(20), s = Ui.dp(52);
        r.set(pad, (h - s) / 2f, pad + s, (h + s) / 2f);
        Bitmap bm = (art != null && artTrack != null) ? art.get(artTrack, 160, artCb) : null;
        Ui.drawArt(c, p, bm, artTrack == null ? title : artTrack.album + artTrack.artist, r, round ? s / 2f : Ui.dp(12));

        float tx = pad + s + Ui.dp(16);
        float reserve = current && playing ? Ui.dp(52) : (right.isEmpty() ? Ui.dp(20) : Ui.dp(64)) + (rating > 0 ? Ui.dp(24) : 0);
        float maxW = w - tx - reserve;

        tp.setTypeface(Ui.MEDIUM);
        tp.setTextSize(Ui.dp(16.5f));
        tp.setColor(current ? Ui.ACCENT : Ui.TEXT);
        String t = TextUtils.ellipsize(title, tp, maxW, TextUtils.TruncateAt.END).toString();
        c.drawText(t, tx, h / 2f - Ui.dp(3), tp);

        tp.setTypeface(Ui.REGULAR);
        tp.setTextSize(Ui.dp(13.5f));
        tp.setColor(Ui.TEXT2);
        String st = TextUtils.ellipsize(sub, tp, maxW, TextUtils.TruncateAt.END).toString();
        c.drawText(st, tx, h / 2f + Ui.dp(17), tp);

        if (current) {
            float t0 = System.nanoTime() / 1e9f;
            p.setStyle(Paint.Style.FILL);
            p.setColor(Ui.ACCENT);
            float bw = Ui.dp(3.5f), gap = Ui.dp(3f);
            float x0 = w - pad - 3 * bw - 2 * gap;
            for (int i = 0; i < 3; i++) {
                float f = playing ? 0.5f + 0.5f * (float) Math.sin(t0 * (5f + i * 1.9f) + i * 2f) : 0.35f;
                float bh = Ui.dp(5) + Ui.dp(15) * f;
                float x = x0 + i * (bw + gap);
                r.set(x, h / 2f + bh / 2f - bh, x + bw, h / 2f + bh / 2f);
                r.offset(0, 0);
                c.drawRoundRect(r, bw / 2f, bw / 2f, p);
            }
            if (playing) postInvalidateOnAnimation();
        }
        if (rating > 0 && !(current && playing)) {
            float ix = right.isEmpty() ? w - pad - Ui.dp(8) : w - pad - Ui.dp(68);
            Icons.draw(c, p, rating >= 2 ? Icons.HEART_FILLED : Icons.THUMB_FILLED, ix, h / 2f, Ui.dp(24),
                    rating >= 2 ? 0xFFE8557E : Ui.ACCENT);
        }
        if (!current && !right.isEmpty()) {
            tp.setTypeface(Ui.REGULAR);
            tp.setTextSize(Ui.dp(12.5f));
            tp.setColor(Ui.TEXT2);
            tp.setTextAlign(Paint.Align.RIGHT);
            c.drawText(right, w - pad, h / 2f + Ui.dp(4.5f), tp);
            tp.setTextAlign(Paint.Align.LEFT);
        }
    }
}
