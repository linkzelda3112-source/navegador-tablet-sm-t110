package com.valleradial;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * Manos y pistola dibujadas "a tinta" (relleno plano + contorno negro + rayitas de sombreado).
 * Se dibuja en un espacio de referencia de 640x360 escalado a la altura de la pantalla.
 */
final class Hands {
    private static final int YELLOW = 0xFFD9C94A;
    private static final int YELLOW_LIGHT = 0xFFEADD83;
    private static final int SLEEVE = 0xFFD6B93A;
    private static final int SKIN = 0xFFF5F0E6;
    private static final int INK = 0xFF141212;
    private static final int ROPE = 0xFFB05A8A;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hatch = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();

    Hands() {
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setColor(INK);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeWidth(2.4f);
        hatch.setStyle(Paint.Style.STROKE);
        hatch.setColor(INK);
        hatch.setStrokeCap(Paint.Cap.ROUND);
        hatch.setStrokeWidth(1.1f);
    }

    private void ink(Canvas c, Path p, int color) {
        fill.setColor(color);
        c.drawPath(p, fill);
        c.drawPath(p, stroke);
    }

    private void poly(Canvas c, int color, float... xy) {
        path.reset();
        path.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) path.lineTo(xy[i], xy[i + 1]);
        path.close();
        ink(c, path, color);
    }

    private void rrect(Canvas c, int color, float l, float t, float r, float b, float rad) {
        rect.set(l, t, r, b);
        fill.setColor(color);
        c.drawRoundRect(rect, rad, rad, fill);
        c.drawRoundRect(rect, rad, rad, stroke);
    }

    private void line(Canvas c, float x0, float y0, float x1, float y1) {
        c.drawLine(x0, y0, x1, y1, hatch);
    }

    /** Dibuja ambas manos. W,H en pixeles; u = H/360. */
    void draw(Canvas c, float W, float H, Game g, float time) {
        float u = H / 360f;
        float speed = (float) Math.sqrt(g.vx * g.vx + g.vy * g.vy + g.vz * g.vz);
        float amp = g.onGround ? Math.min(1f, speed / 13f) : 0.25f;
        float bobY = (float) Math.sin(g.bob * 1.0f) * 3.5f * amp;
        float bobX = (float) Math.cos(g.bob * 0.5f) * 3.0f * amp;
        float swayX = clamp(-g.swayX * 18f, -22f, 22f);
        float swayY = clamp(g.swayY * 18f, -14f, 14f);

        // ---------------- mano izquierda (kunai y cuerda)
        c.save();
        c.scale(u, u);
        c.translate(bobX * 0.6f + swayX * 0.7f, bobY + swayY);
        drawLeft(c, W / u, H / u, g, time, u);
        c.restore();

        // ---------------- mano derecha (pistola), pegada al borde derecho
        c.save();
        c.scale(u, u);
        float offX = W / u - 640f;
        float rec = g.recoil;
        c.translate(offX + bobX + swayX * 0.5f + rec * 9f, bobY * 1.2f + swayY + rec * 7f);
        c.rotate(-rec * 4f, 540f, 300f);
        drawGun(c, g);
        c.restore();
    }

    private static float clamp(float v, float a, float b) { return v < a ? a : (v > b ? b : v); }

    // ------------------------------------------------------------------------------------ pistola
    private void drawGun(Canvas c, Game g) {
        stroke.setStrokeWidth(2.4f);
        // manga: manga y puño
        poly(c, SLEEVE, 572f, 338f, 632f, 318f, 720f, 330f, 720f, 380f, 590f, 380f);
        for (int i = 0; i < 6; i++) line(c, 600f + i * 16f, 346f + i * 3f, 590f + i * 16f, 380f);
        poly(c, INK, 560f, 304f, 614f, 294f, 628f, 334f, 574f, 346f); // punio negro
        // agarre
        poly(c, YELLOW, 522f, 236f, 576f, 234f, 590f, 308f, 548f, 318f);
        // mano blanca: palma + dedos
        poly(c, SKIN, 544f, 232f, 604f, 226f, 616f, 300f, 566f, 322f);
        rrect(c, SKIN, 494f, 242f, 548f, 266f, 11f);
        rrect(c, SKIN, 492f, 266f, 546f, 288f, 11f);
        rrect(c, SKIN, 496f, 288f, 548f, 310f, 11f);
        line(c, 560f, 252f, 566f, 296f);
        line(c, 572f, 250f, 578f, 298f);
        // corredera
        path.reset();
        path.moveTo(432f, 176f);
        path.lineTo(444f, 160f);
        path.lineTo(568f, 156f);
        path.lineTo(576f, 172f);
        path.lineTo(572f, 238f);
        path.lineTo(442f, 246f);
        path.lineTo(432f, 230f);
        path.close();
        ink(c, path, YELLOW);
        // ranura / ventana
        rrect(c, YELLOW_LIGHT, 452f, 184f, 524f, 214f, 15f);
        rrect(c, 0xFF5E5420, 460f, 192f, 516f, 206f, 7f);
        // ventilas
        for (int i = 0; i < 3; i++) {
            float x = 538f + i * 9f;
            stroke.setStrokeWidth(2.2f);
            c.drawLine(x, 186f, x, 220f, stroke);
        }
        stroke.setStrokeWidth(2.4f);
        poly(c, YELLOW, 556f, 146f, 570f, 146f, 570f, 158f, 556f, 158f); // mira
        // rayitas de sombra
        for (int i = 0; i < 9; i++) line(c, 446f + i * 14f, 238f, 440f + i * 14f, 226f);
        for (int i = 0; i < 6; i++) line(c, 526f + i * 9f, 300f, 522f + i * 9f, 286f);
        // fogonazo
        if (g.recoil > 0.7f) {
            fill.setColor(0xFFFFF4B8);
            path.reset();
            float mx = 428f, my = 188f;
            int pts = 10;
            for (int i = 0; i < pts * 2; i++) {
                double a = Math.PI * i / pts;
                float r = (i % 2 == 0) ? 34f : 13f;
                float x = mx - (float) Math.cos(a) * r * 1.2f;
                float y = my + (float) Math.sin(a) * r * 1.4f - (float) Math.sin(a) * 0f;
                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            path.close();
            c.drawPath(path, fill);
            c.drawPath(path, stroke);
        }
    }

    // ------------------------------------------------------------------------------------ mano izquierda
    private float anchorX = -1, anchorY = -1; // en espacio de referencia (lo pone HudView)

    void setAnchor(float rx, float ry) { anchorX = rx; anchorY = ry; }

    private void drawLeft(Canvas c, float Wr, float Hr, Game g, float time, float u) {
        stroke.setStrokeWidth(2.4f);
        // manga
        poly(c, SLEEVE, -30f, 340f, 36f, 262f, 104f, 240f, 150f, 300f, 196f, 380f, -30f, 380f);
        for (int i = 0; i < 7; i++) line(c, 20f + i * 16f, 330f - i * 6f, 8f + i * 16f, 360f);
        boolean attached = g.hookState == Game.HOOK_ATTACHED;
        if (attached) {
            drawRopeFist(c, g, time);
        } else if (g.throwT > 0f) {
            float t = g.throwT / 0.45f;          // 1 -> 0
            drawOpenHand(c, t);
        } else {
            drawIdleKunai(c, time);
        }
    }

    private void cuff(Canvas c, float dy) {
        poly(c, INK, 66f, 226f + dy, 114f, 246f + dy, 104f, 282f + dy, 56f, 262f + dy);
    }

    /** Dedo arriba con el kunai oscilando (como en el video). */
    private void drawIdleKunai(Canvas c, float time) {
        cuff(c, 0f);
        // puño
        rrect(c, SKIN, 66f, 188f, 122f, 244f, 18f);
        line(c, 74f, 222f, 112f, 226f);
        line(c, 76f, 232f, 110f, 236f);
        // indice
        rrect(c, SKIN, 88f, 110f, 108f, 198f, 10f);
        // kunai en la punta del dedo
        float ang = (float) Math.sin(time * 3.2f) * 22f - 20f;
        c.save();
        c.rotate(ang, 98f, 106f);
        // cuerda en lazo
        stroke.setColor(ROPE);
        stroke.setStrokeWidth(2f);
        path.reset();
        path.moveTo(98f, 82f);
        path.quadTo(150f, 90f, 124f, 190f);
        c.drawPath(path, stroke);
        stroke.setColor(INK);
        stroke.setStrokeWidth(2.4f);
        poly(c, SKIN, 98f, 40f, 108f, 74f, 98f, 88f, 88f, 74f);   // hoja
        line(c, 98f, 46f, 98f, 82f);
        c.drawCircle(98f, 94f, 6f, stroke);                      // anillo
        c.restore();
    }

    /** Mano abierta al lanzar o soltar. */
    private void drawOpenHand(Canvas c, float t) {
        float lift = (1f - t) * 20f;
        cuff(c, -lift * 0.2f);
        c.save();
        c.translate(0f, -lift);
        rrect(c, SKIN, 62f, 196f, 120f, 244f, 14f);
        float[] angs = {-38f, -18f, 0f, 18f, 34f};
        float[] lens = {44f, 56f, 60f, 54f, 42f};
        for (int i = 0; i < 5; i++) {
            c.save();
            c.rotate(angs[i], 92f, 206f);
            rrect(c, SKIN, 92f - 8f, 206f - lens[i], 92f + 8f, 212f, 8f);
            c.restore();
        }
        c.restore();
    }

    /** Puño con la cuerda hacia el punto donde clavo el kunai. */
    private void drawRopeFist(Canvas c, Game g, float time) {
        cuff(c, 0f);
        float fx = 116f, fy = 204f;
        // cuerda hacia el ancla
        float tx = anchorX, ty = anchorY;
        if (tx < 0f) { tx = 320f; ty = 170f; }
        stroke.setColor(ROPE);
        stroke.setStrokeWidth(2.6f);
        path.reset();
        path.moveTo(fx, fy);
        float mx = (fx + tx) * 0.5f, my = (fy + ty) * 0.5f + 8f;
        path.quadTo(mx, my, tx, ty);
        c.drawPath(path, stroke);
        // cola colgando
        path.reset();
        path.moveTo(84f, 248f);
        path.quadTo(70f + (float) Math.sin(time * 6f) * 4f, 280f, 92f, 312f);
        stroke.setStrokeWidth(3f);
        c.drawPath(path, stroke);
        stroke.setColor(INK);
        stroke.setStrokeWidth(2.4f);
        // puño cerrado
        rrect(c, SKIN, 62f, 196f, 128f, 250f, 20f);
        for (int i = 0; i < 4; i++) {
            float y = 202f + i * 11f;
            rrect(c, SKIN, 100f, y, 134f, y + 11f, 5f);
        }
        line(c, 70f, 238f, 110f, 244f);
        // marquitas de tension
        line(c, 54f, 196f, 46f, 190f);
        line(c, 58f, 188f, 52f, 178f);
    }

    // ------------------------------------------------------------------------------------ barra de vida
    void drawHealth(Canvas c, float H, int hp, int max, float flash) {
        float u = H / 360f;
        c.save();
        c.scale(u, u);
        c.translate(18f, 334f);
        c.rotate(-21f);
        stroke.setStrokeWidth(2.6f);
        for (int i = 0; i < max; i++) {
            float x = i * 27f;
            path.reset();
            path.moveTo(x + 4f, -9f);
            path.lineTo(x + 28f, -9f);
            path.lineTo(x + 24f, 9f);
            path.lineTo(x, 9f);
            path.close();
            if (i < hp) {
                ink(c, path, flash > 0f && (i == hp - 1) ? 0xFFFFE0E8 : 0xFFD6477F);
            } else {
                ink(c, path, 0x66301018);
            }
        }
        c.restore();
    }
}
