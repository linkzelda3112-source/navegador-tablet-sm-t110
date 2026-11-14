package com.lumina;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;

/** Tarjeta flotante con la canción actual: tocarla abre la pantalla de reproducción. */
final class MiniPlayerView extends View {
    interface Callbacks { void onOpen(); void onToggle(); void onNext(); }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Callbacks cb;
    private Track track;
    private Bitmap art;
    private boolean playing;
    private float progress;
    private int tint = Ui.SURFACE2;
    private boolean down;

    MiniPlayerView(Context c, Callbacks cb) {
        super(c);
        this.cb = cb;
        setElevation(Ui.dp(12));
        setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), Ui.dp(22));
            }
        });
    }

    void set(Track t, Bitmap art, boolean playing, int tint) {
        this.track = t;
        this.art = art;
        this.playing = playing;
        this.tint = Ui.blend(Ui.SURFACE2, tint, 0.38f);
        invalidate();
    }

    void setProgress(float f) {
        progress = f;
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(MeasureSpec.getSize(w), Ui.dpi(68));
    }

    @Override
    protected void onDraw(Canvas c) {
        if (track == null) return;
        float w = getWidth(), h = getHeight();
        r.set(0, 0, w, h);
        p.setStyle(Paint.Style.FILL);
        p.setColor(down ? Ui.blend(tint, 0xFFFFFFFF, 0.06f) : tint);
        c.drawRoundRect(r, Ui.dp(22), Ui.dp(22), p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Ui.dp(1));
        p.setColor(0x22FFFFFF);
        c.drawRoundRect(r, Ui.dp(22), Ui.dp(22), p);

        float s = Ui.dp(48);
        r.set(Ui.dp(10), (h - s) / 2f, Ui.dp(10) + s, (h + s) / 2f);
        Ui.drawArt(c, p, art, track.album + track.artist, r, Ui.dp(14));

        float tx = Ui.dp(10) + s + Ui.dp(14);
        float maxW = w - tx - Ui.dp(108);
        tp.setTypeface(Ui.MEDIUM);
        tp.setTextSize(Ui.dp(15.5f));
        tp.setColor(Ui.TEXT);
        c.drawText(TextUtils.ellipsize(track.title, tp, maxW, TextUtils.TruncateAt.END).toString(), tx, h / 2f - Ui.dp(2), tp);
        tp.setTypeface(Ui.REGULAR);
        tp.setTextSize(Ui.dp(13));
        tp.setColor(Ui.withAlpha(Ui.TEXT, 170));
        c.drawText(TextUtils.ellipsize(track.artist, tp, maxW, TextUtils.TruncateAt.END).toString(), tx, h / 2f + Ui.dp(16), tp);

        Icons.draw(c, p, playing ? Icons.PAUSE : Icons.PLAY, w - Ui.dp(76), h / 2f, Ui.dp(34), Ui.TEXT);
        Icons.draw(c, p, Icons.NEXT, w - Ui.dp(32), h / 2f, Ui.dp(30), Ui.TEXT);

        // progreso
        float px0 = Ui.dp(22), px1 = w - Ui.dp(22), py = h - Ui.dp(5);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x26FFFFFF);
        r.set(px0, py - Ui.dp(1.5f), px1, py + Ui.dp(1.5f));
        c.drawRoundRect(r, Ui.dp(2), Ui.dp(2), p);
        p.setColor(Ui.ACCENT);
        r.set(px0, py - Ui.dp(1.5f), px0 + (px1 - px0) * Math.max(0f, Math.min(1f, progress)), py + Ui.dp(1.5f));
        c.drawRoundRect(r, Ui.dp(2), Ui.dp(2), p);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                down = true;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                down = false;
                invalidate();
                float w = getWidth();
                if (e.getX() > w - Ui.dp(54)) cb.onNext();
                else if (e.getX() > w - Ui.dp(100)) cb.onToggle();
                else cb.onOpen();
                return true;
            case MotionEvent.ACTION_CANCEL:
                down = false;
                invalidate();
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }
}
