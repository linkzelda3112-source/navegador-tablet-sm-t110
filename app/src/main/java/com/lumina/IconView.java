package com.lumina;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/** Botón de ícono dibujado por código, con pequeño rebote al tocarlo. */
final class IconView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int kind, color, circle;
    private float frac = 0.5f;

    IconView(Context c, int kind, int color) {
        super(c);
        this.kind = kind;
        this.color = color;
        setClickable(true);
    }

    void setKind(int k) { if (kind != k) { kind = k; invalidate(); } }

    void setColor(int c) { if (color != c) { color = c; invalidate(); } }

    void setCircle(int c) { circle = c; invalidate(); }

    void setFrac(float f) { frac = f; invalidate(); }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float m = Math.min(w, h);
        if (circle != 0) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(circle);
            c.drawCircle(w / 2, h / 2, m / 2, p);
        }
        Icons.draw(c, p, kind, w / 2, h / 2, m * frac, color);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                animate().scaleX(0.88f).scaleY(0.88f).setDuration(90).start();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                animate().scaleX(1f).scaleY(1f).setDuration(160).start();
                break;
            default:
                break;
        }
        return super.onTouchEvent(e);
    }
}
