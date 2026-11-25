package com.lumina;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/** Fondo vivo: dos halos de color que flotan despacio detrás de la pantalla principal. */
final class AmbientView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    AmbientView(Context c) { super(c); }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float t = System.nanoTime() / 1e9f;
        blob(c, w * (0.85f + 0.12f * (float) Math.sin(t * 0.21f)), h * (0.08f + 0.06f * (float) Math.cos(t * 0.17f)),
                w * 0.85f, Ui.withAlpha(Ui.ACCENT, Ui.light ? 46 : 54));
        blob(c, w * (0.05f + 0.12f * (float) Math.cos(t * 0.15f)), h * (0.62f + 0.1f * (float) Math.sin(t * 0.19f)),
                w * 0.9f, Ui.withAlpha(Ui.blend(Ui.ACCENT, Ui.TEXT2, 0.6f), Ui.light ? 30 : 34));
        postInvalidateDelayed(40);
    }

    private void blob(Canvas c, float x, float y, float r, int color) {
        p.setShader(new RadialGradient(x, y, r, color, color & 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, r, p);
    }
}
