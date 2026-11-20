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
    private float anchorX = -1, anchorY = -1; // ancla del gancho en espacio de referencia (lo pone HudView)

    void setAnchor(float rx, float ry) { anchorX = rx; anchorY = ry; }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private void drawLeft(Canvas c, float Wr, float Hr, Game g, float time, float u) {
        stroke.setStrokeWidth(2.4f);
        if (g.hookState == Game.HOOK_ATTACHED) {
            sleeveIdle(c);
            drawRopeFist(c, g, time);
        } else if (g.hookState == Game.HOOK_FLY) {
            drawThrow(c, g);
        } else if (g.releaseT > 0f) {
            drawRelease(c, g.releaseT / 0.4f);
        } else {
            sleeveIdle(c);
            drawIdleKunai(c, time);
        }
    }

    private void sleeveIdle(Canvas c) {
        poly(c, SLEEVE, -30f, 340f, 36f, 262f, 104f, 240f, 150f, 300f, 196f, 380f, -30f, 380f);
        for (int i = 0; i < 7; i++) line(c, 20f + i * 16f, 330f - i * 6f, 8f + i * 16f, 360f);
    }

    private void cuff(Canvas c, float dy) {
        poly(c, INK, 66f, 226f + dy, 114f, 246f + dy, 104f, 282f + dy, 56f, 262f + dy);
    }

    /** Antebrazo largo desde la esquina inferior izquierda hasta la muneca. Devuelve el angulo (grados) respecto a "arriba". */
    private float armTo(Canvas c, float wx, float wy) {
        float bx = 10f, by = 380f;
        float dx = wx - bx, dy = wy - by;
        float l = (float) Math.sqrt(dx * dx + dy * dy);
        float ux = dx / l, uy = dy / l, nx = -uy, ny = ux;
        float w0 = 44f, w1 = 25f;
        poly(c, SLEEVE, bx + nx * w0, by + ny * w0, wx + nx * w1, wy + ny * w1,
                wx - nx * w1, wy - ny * w1, bx - nx * w0, by - ny * w0);
        for (int i = 1; i < 7; i++) {
            float t = i / 7f;
            float cx = lerp(bx, wx, t), cy = lerp(by, wy, t), w = lerp(w0, w1, t) * 0.8f;
            line(c, cx + nx * w, cy + ny * w, cx + nx * (w - 10f) - ux * 8f, cy + ny * (w - 10f) - uy * 8f);
            line(c, cx - nx * w * 0.2f, cy - ny * w * 0.2f, cx - nx * w * 0.2f - ux * 10f, cy - ny * w * 0.2f - uy * 10f);
        }
        // punio negro
        poly(c, INK, wx - ux * 22f + nx * (w1 + 1f), wy - uy * 22f + ny * (w1 + 1f),
                wx - ux * 22f - nx * (w1 + 1f), wy - uy * 22f - ny * (w1 + 1f),
                wx - ux * 4f - nx * (w1 - 1f), wy - uy * 4f - ny * (w1 - 1f),
                wx - ux * 4f + nx * (w1 - 1f), wy - uy * 4f + ny * (w1 - 1f));
        return (float) Math.toDegrees(Math.atan2(dx, -dy));
    }

    /** Palma abierta (spread=1) o apuntando con el indice (spread=0). Origen en la muneca, apunta hacia -y. */
    private void handLocal(Canvas c, float spread) {
        rrect(c, SKIN, -18f, -40f, 18f, 2f, 10f);
        float[] angs = {-36f, -18f, 0f, 18f, 34f};
        float[] lens = {36f, 48f, 54f, 46f, 34f};
        for (int i = 0; i < 5; i++) {
            c.save();
            c.translate(0f, -34f);
            float ang = angs[i] * spread;
            c.rotate(ang);
            float len = lens[i];
            if (spread < 0.5f && i != 1) len *= 0.35f;   // dedos doblados al apuntar
            rrect(c, SKIN, -6.5f, -len, 6.5f, 6f, 6.5f);
            c.restore();
        }
        // pulgar
        c.save();
        c.translate(-14f, -14f);
        c.rotate(-58f * (0.4f + 0.6f * spread));
        rrect(c, SKIN, -6f, -28f, 6f, 4f, 6f);
        c.restore();
        line(c, -8f, -4f, 8f, -6f);
    }

    private void kunai(Canvas c, float x, float y, float angDeg, float s) {
        c.save();
        c.translate(x, y);
        c.rotate(angDeg);
        c.scale(s, s);
        poly(c, SKIN, 0f, -26f, 5.5f, -6f, 0f, 5f, -5.5f, -6f);
        line(c, 0f, -22f, 0f, 0f);
        c.drawCircle(0f, 10f, 4.5f, stroke);
        c.restore();
    }

    private void rope(Canvas c, float x0, float y0, float x1, float y1, float sag) {
        stroke.setColor(ROPE);
        stroke.setStrokeWidth(2.2f);
        path.reset();
        path.moveTo(x0, y0);
        path.quadTo((x0 + x1) * 0.5f, (y0 + y1) * 0.5f + sag, x1, y1);
        c.drawPath(path, stroke);
        stroke.setColor(INK);
        stroke.setStrokeWidth(2.4f);
    }

    /** Lanzamiento: el brazo se extiende, la mano abierta apunta y el kunai vuela con la cuerda. */
    private void drawThrow(Canvas c, Game g) {
        float p = g.hookProg;
        float ext = Math.min(1f, 0.35f + p * 2.2f);
        float wx = lerp(100f, 182f, ext), wy = lerp(262f, 180f, ext);
        float ang = armTo(c, wx, wy);
        c.save();
        c.translate(wx, wy);
        c.rotate(ang);
        handLocal(c, p < 0.35f ? 1f : 0f);
        c.restore();
        // punta de los dedos (indice) en espacio de referencia
        double a = Math.toRadians(ang);
        float tx = wx + (float) Math.sin(a) * 60f, ty = wy - (float) Math.cos(a) * 60f;
        float ax = anchorX >= 0f ? anchorX : 320f, ay = anchorX >= 0f ? anchorY : 170f;
        if (anchorX < 0f) { ax = tx + (float) Math.sin(a) * 300f; ay = ty - (float) Math.cos(a) * 300f; }
        float ex = lerp(tx, ax, p), ey = lerp(ty, ay, p);
        rope(c, tx, ty, ex, ey, 0f);
        float dir = (float) Math.toDegrees(Math.atan2(ex - tx, -(ey - ty)));
        kunai(c, ex, ey, dir, 0.55f);
    }

    /** Soltar: la mano se abre y vuelve a su sitio. t: 1 -> 0. */
    private void drawRelease(Canvas c, float t) {
        float wx = lerp(100f, 168f, t), wy = lerp(262f, 192f, t);
        float ang = armTo(c, wx, wy);
        c.save();
        c.translate(wx, wy);
        c.rotate(ang);
        handLocal(c, 1f);
        c.restore();
    }

    /** Dedo arriba con el kunai colgando de la cuerda y balanceandose (como en el video). */
    private void drawIdleKunai(Canvas c, float time) {
        cuff(c, 0f);
        rrect(c, SKIN, 66f, 188f, 122f, 244f, 18f);
        line(c, 74f, 222f, 112f, 226f);
        line(c, 76f, 232f, 110f, 236f);
        rrect(c, SKIN, 88f, 110f, 108f, 198f, 10f);       // indice
        float tx = 98f, ty = 108f;
        float swing = 1.05f + 0.55f * (float) Math.sin(time * 3.1f);   // radianes desde la vertical
        float rx = tx + (float) Math.sin(swing) * 50f, ry = ty - (float) Math.cos(swing) * 50f;
        // cuerda en V desde la punta del dedo hasta el anillo
        stroke.setColor(ROPE);
        stroke.setStrokeWidth(2f);
        path.reset();
        path.moveTo(tx, ty);
        path.quadTo((tx + rx) * 0.5f + 6f, (ty + ry) * 0.5f + 12f, rx, ry);
        c.drawPath(path, stroke);
        stroke.setColor(INK);
        stroke.setStrokeWidth(2.4f);
        // el kunai apunta alejandose del anillo
        float deg = (float) Math.toDegrees(swing) + 62f;
        kunai(c, rx, ry, deg, 0.95f);
        // rayitas de movimiento
        line(c, rx + 12f, ry - 20f, rx + 26f, ry - 30f);
        line(c, rx + 20f, ry - 10f, rx + 36f, ry - 16f);
    }

    /** Puno con la cuerda tensa hacia el punto donde se clavo el kunai. */
    private void drawRopeFist(Canvas c, Game g, float time) {
        float shake = (float) Math.sin(time * 40f) * 1.2f;
        c.save();
        c.translate(shake, -shake * 0.6f);
        cuff(c, 0f);
        float fx = 108f, fy = 200f;
        float tx = anchorX, ty = anchorY;
        if (tx < 0f) { tx = 320f; ty = 170f; }
        rope(c, fx, fy, tx, ty, 6f);
        // kunai clavado en el ancla
        float dir = (float) Math.toDegrees(Math.atan2(tx - fx, -(ty - fy)));
        kunai(c, tx, ty, dir, 0.45f);
        // cola de cuerda colgando del puno
        stroke.setColor(ROPE);
        stroke.setStrokeWidth(3.2f);
        path.reset();
        path.moveTo(84f, 248f);
        path.quadTo(66f + (float) Math.sin(time * 7f) * 5f, 284f, 94f, 322f);
        c.drawPath(path, stroke);
        stroke.setColor(INK);
        stroke.setStrokeWidth(2.4f);
        // puno cerrado
        rrect(c, SKIN, 62f, 196f, 128f, 250f, 20f);
        for (int i = 0; i < 4; i++) {
            float y = 202f + i * 11f;
            rrect(c, SKIN, 100f, y, 134f, y + 11f, 5f);
        }
        line(c, 70f, 238f, 110f, 244f);
        // marquitas de tension
        line(c, 54f, 196f, 44f, 190f);
        line(c, 56f, 186f, 48f, 174f);
        line(c, 66f, 182f, 62f, 168f);
        c.restore();
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
