package com.valleradial;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * Manos, kunai y pistola dibujados "a tinta": relleno plano + contorno negro + sombreado con rayitas.
 * Se dibuja en un espacio de referencia de 640x360 escalado a la altura de la pantalla.
 */
final class Hands {
    private static final int YELLOW = 0xFFD9C94A;
    private static final int YELLOW_LIGHT = 0xFFF0E58E;
    private static final int YELLOW_DARK = 0xFFB3A12C;
    private static final int SLEEVE = 0xFFD6B93A;
    private static final int SLEEVE_DARK = 0xFFB89A28;
    private static final int SKIN = 0xFFF7F2E8;
    private static final int SKIN_SHADE = 0xFFDDD4C0;
    private static final int INK = 0xFF141212;
    private static final int INK_SOFT = 0xFF3A3434;
    private static final int ROPE = 0xFFC2639A;
    private static final int ROPE_LIGHT = 0xFFE59CC4;
    private static final int STEEL = 0xFFF6F3EA;
    private static final int STEEL_SHADE = 0xFFC9C3B2;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hatch = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Path clip = new Path();
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

    // ------------------------------------------------------------------------------------ utilidades de dibujo
    private void ink(Canvas c, Path p, int color) {
        fill.setColor(color);
        c.drawPath(p, fill);
        c.drawPath(p, stroke);
    }

    /** Poligono de esquinas vivas con relleno y contorno. */
    private void poly(Canvas c, int color, float... xy) {
        path.reset();
        path.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) path.lineTo(xy[i], xy[i + 1]);
        path.close();
        ink(c, path, color);
    }

    /** Poligono relleno sin contorno (luces y sombras planas). */
    private void flat(Canvas c, int color, float... xy) {
        path.reset();
        path.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) path.lineTo(xy[i], xy[i + 1]);
        path.close();
        fill.setColor(color);
        c.drawPath(path, fill);
    }

    /** Curva cerrada suave: los puntos son puntos de control, las esquinas se redondean. */
    private void smoothInto(Path p, float[] xy) {
        int n = xy.length / 2;
        p.reset();
        p.moveTo((xy[2 * n - 2] + xy[0]) * 0.5f, (xy[2 * n - 1] + xy[1]) * 0.5f);
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            p.quadTo(xy[2 * i], xy[2 * i + 1], (xy[2 * i] + xy[2 * j]) * 0.5f, (xy[2 * i + 1] + xy[2 * j + 1]) * 0.5f);
        }
        p.close();
    }

    private void blob(Canvas c, int color, float... xy) {
        smoothInto(path, xy);
        ink(c, path, color);
    }

    /** Fija el recorte a una curva suave (hay que envolver en save()/restore()). */
    private void clipBlob(Canvas c, float... xy) {
        smoothInto(clip, xy);
        c.clipPath(clip);
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

    private void thick(Canvas c, float w, float x0, float y0, float x1, float y1) {
        stroke.setStrokeWidth(w);
        c.drawLine(x0, y0, x1, y1, stroke);
        stroke.setStrokeWidth(2.4f);
    }

    /** Curva fina de pliegue. */
    private void crease(Canvas c, float x0, float y0, float cx, float cy, float x1, float y1) {
        path.reset();
        path.moveTo(x0, y0);
        path.quadTo(cx, cy, x1, y1);
        c.drawPath(path, hatch);
    }

    /** Rayitas paralelas de sombreado: n lineas desde (x,y) con paso (sx,sy) y vector (dx,dy). */
    private void hatchRun(Canvas c, int n, float x, float y, float sx, float sy, float dx, float dy) {
        for (int i = 0; i < n; i++) line(c, x + sx * i, y + sy * i, x + sx * i + dx, y + sy * i + dy);
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private static float clamp(float v, float a, float b) { return v < a ? a : (v > b ? b : v); }

    // ------------------------------------------------------------------------------------ cuerda trenzada
    /** Cuerda con contorno, hebras cruzadas y brillo, siguiendo una curva cuadratica. */
    private void rope(Canvas c, float x0, float y0, float cx, float cy, float x1, float y1, float w) {
        path.reset();
        path.moveTo(x0, y0);
        path.quadTo(cx, cy, x1, y1);
        stroke.setStrokeWidth(w + 2.6f);
        c.drawPath(path, stroke);
        stroke.setColor(ROPE);
        stroke.setStrokeWidth(w);
        c.drawPath(path, stroke);
        stroke.setColor(INK);
        stroke.setStrokeWidth(2.4f);
        float len = (float) Math.hypot(x1 - x0, y1 - y0);
        int n = Math.max(2, (int) (len / (w * 1.5f)));
        for (int i = 1; i < n; i++) {
            float t = (float) i / n, it = 1f - t;
            float px = it * it * x0 + 2 * it * t * cx + t * t * x1;
            float py = it * it * y0 + 2 * it * t * cy + t * t * y1;
            float tx = 2 * it * (cx - x0) + 2 * t * (x1 - cx), ty = 2 * it * (cy - y0) + 2 * t * (y1 - cy);
            float tl = (float) Math.hypot(tx, ty);
            if (tl < 1e-3f) continue;
            tx /= tl; ty /= tl;
            float nx = -ty * w * 0.5f, ny = tx * w * 0.5f;
            line(c, px - nx - tx * w * 0.35f, py - ny - ty * w * 0.35f, px + nx + tx * w * 0.35f, py + ny + ty * w * 0.35f);
        }
    }

    // ------------------------------------------------------------------------------------ kunai
    /** Kunai: hoja en forma de hoja con arista central, guarda, mango vendado y anillo. Punta en (0,-34). */
    private void kunai(Canvas c, float x, float y, float angDeg, float s) {
        c.save();
        c.translate(x, y);
        c.rotate(angDeg);
        c.scale(s, s);
        // mango vendado
        poly(c, INK, -3.4f, 6f, 3.4f, 6f, 3.8f, 24f, -3.8f, 24f);
        stroke.setStrokeWidth(1.4f);
        stroke.setColor(0xFFEFE9DA);
        for (int i = 0; i < 4; i++) c.drawLine(-3.6f, 8f + i * 4.2f, 3.6f, 11f + i * 4.2f, stroke);
        stroke.setColor(INK);
        stroke.setStrokeWidth(2.4f);
        // guarda
        poly(c, INK_SOFT, -8.5f, 2f, 8.5f, 2f, 7.5f, 7f, -7.5f, 7f);
        // hoja
        poly(c, STEEL, 0f, -34f, 6.8f, -14f, 5.2f, 2.5f, -5.2f, 2.5f, -6.8f, -14f);
        flat(c, STEEL_SHADE, 0f, -33f, 5.2f, -14f, 4f, 1.8f, 0f, 1.8f);
        c.drawLine(0f, -31f, 0f, 1.5f, hatch);
        hatchRun(c, 4, 1.6f, -16f, 0f, 4.2f, 2.8f, 1.4f);
        // anillo del pomo
        c.drawCircle(0f, 30f, 6.4f, stroke);
        stroke.setColor(STEEL);
        stroke.setStrokeWidth(2.4f);
        c.drawCircle(0f, 30f, 6.4f, stroke);
        stroke.setColor(INK);
        stroke.setStrokeWidth(1.2f);
        c.drawCircle(0f, 30f, 7.7f, stroke);
        c.drawCircle(0f, 30f, 5.1f, stroke);
        stroke.setStrokeWidth(2.4f);
        c.restore();
    }

    // ------------------------------------------------------------------------------------ dedos y manos
    /** Dedo de espalda de mano: origen en la base, apunta a -y, w = ancho, con nudillos y una. */
    private void finger(Canvas c, float angDeg, float len, float w) {
        c.save();
        c.rotate(angDeg);
        float h = w * 0.5f;
        blob(c, SKIN, -h * 0.95f, 4f, -h * 1.05f, -len * 0.45f, -h * 0.8f, -len + w * 0.5f, 0f, -len - 1f,
                h * 0.8f, -len + w * 0.5f, h * 1.05f, -len * 0.45f, h * 0.95f, 4f);
        // sombra del costado derecho
        c.save();
        clipBlob(c, -h * 0.95f, 4f, -h * 1.05f, -len * 0.45f, -h * 0.8f, -len + w * 0.5f, 0f, -len - 1f,
                h * 0.8f, -len + w * 0.5f, h * 1.05f, -len * 0.45f, h * 0.95f, 4f);
        flat(c, SKIN_SHADE, h * 0.35f, 6f, h * 1.4f, 6f, h * 1.4f, -len - 3f, h * 0.55f, -len - 3f);
        c.restore();
        path.reset();
        path.moveTo(-h * 0.95f, 4f);
        path.quadTo(-h * 1.05f, -len * 0.45f, -h * 0.8f, -len + w * 0.5f);
        path.quadTo(0f, -len - 4f, h * 0.8f, -len + w * 0.5f);
        path.quadTo(h * 1.05f, -len * 0.45f, h * 0.95f, 4f);
        c.drawPath(path, stroke);
        // articulaciones
        float j1 = -len * 0.36f, j2 = -len * 0.68f;
        line(c, -h * 0.55f, j1, h * 0.55f, j1 - 0.8f);
        line(c, -h * 0.3f, j1 - 2.4f, h * 0.3f, j1 - 3f);
        line(c, -h * 0.5f, j2, h * 0.5f, j2 - 0.8f);
        // una
        blob(c, 0xFFFFFFFF, -h * 0.5f, -len + w * 0.95f, -h * 0.45f, -len + w * 0.35f, 0f, -len + w * 0.12f,
                h * 0.45f, -len + w * 0.35f, h * 0.5f, -len + w * 0.95f, 0f, -len + w * 1.1f);
        c.restore();
    }

    /** Mano abierta vista de espalda. Origen en la muneca, apunta a -y. spread 1 = abierta, 0 = apuntando con el indice. */
    private void handLocal(Canvas c, float spread) {
        float[] angs = {-34f, -17f, 0f, 16f, 32f};
        float[] lens = {38f, 52f, 57f, 49f, 36f};
        float[] ws = {12.5f, 13.5f, 14f, 13f, 11.5f};
        float[] bx = {-13f, -6.5f, 0f, 6.5f, 12.5f};
        float[] by = {-33f, -37f, -38f, -37f, -33f};
        // dedos (detras de la palma)
        for (int i = 0; i < 5; i++) {
            c.save();
            c.translate(bx[i], by[i]);
            float len = lens[i];
            float ang = angs[i] * spread;
            if (spread < 0.5f && i != 1) { len *= 0.4f; ang *= 0.3f; }
            finger(c, ang, len, ws[i]);
            c.restore();
        }
        // pulgar
        c.save();
        c.translate(-15f, -9f);
        c.rotate(-52f * (0.45f + 0.55f * spread));
        finger(c, 0f, 31f, 14.5f);
        c.restore();
        // palma
        blob(c, SKIN, -20f, 3f, -22f, -14f, -21f, -32f, -14f, -42f, 0f, -45f, 14f, -42f, 21f, -32f, 22f, -14f, 20f, 3f, 0f, 8f);
        c.save();
        clipBlob(c, -20f, 3f, -22f, -14f, -21f, -32f, -14f, -42f, 0f, -45f, 14f, -42f, 21f, -32f, 22f, -14f, 20f, 3f, 0f, 8f);
        flat(c, SKIN_SHADE, 12f, -50f, 30f, -50f, 30f, 12f, 8f, 12f);
        c.restore();
        // tendones del dorso
        crease(c, -8f, -6f, -9f, -18f, -7f, -33f);
        crease(c, 0f, -6f, 1f, -20f, 0f, -36f);
        crease(c, 8f, -6f, 9f, -18f, 7f, -33f);
        // nudillos
        for (int i = 0; i < 5; i++) crease(c, bx[i] - 4f, -31f + (i == 2 ? -6f : 0f), bx[i], -35f + (i == 2 ? -6f : 0f), bx[i] + 4f, -31f + (i == 2 ? -6f : 0f));
        hatchRun(c, 4, -17f, -2f, 0f, 3f, 6f, 1f);
    }

    // ------------------------------------------------------------------------------------ mangas
    private void sleeveIdle(Canvas c) {
        // manga amplia con pliegues
        poly(c, SLEEVE, -30f, 340f, 34f, 262f, 106f, 236f, 156f, 296f, 200f, 380f, -30f, 380f);
        c.save();
        path.reset();
        path.moveTo(-30f, 340f); path.lineTo(34f, 262f); path.lineTo(106f, 236f); path.lineTo(156f, 296f);
        path.lineTo(200f, 380f); path.lineTo(-30f, 380f); path.close();
        c.clipPath(path);
        flat(c, SLEEVE_DARK, -30f, 372f, 60f, 332f, 150f, 320f, 210f, 380f, -30f, 380f);
        c.restore();
        crease(c, 30f, 282f, 60f, 300f, 54f, 336f);
        crease(c, 62f, 262f, 92f, 284f, 84f, 320f);
        crease(c, 96f, 250f, 126f, 276f, 120f, 312f);
        crease(c, 10f, 318f, 34f, 328f, 36f, 362f);
        hatchRun(c, 9, 4f, 344f, 15f, -4f, -10f, 28f);
        hatchRun(c, 5, 112f, 300f, 12f, 12f, -9f, 20f);
    }

    private void cuff(Canvas c, float dy) {
        poly(c, INK, 64f, 224f + dy, 116f, 244f + dy, 106f, 284f + dy, 54f, 264f + dy);
        flat(c, INK_SOFT, 70f, 232f + dy, 104f, 245f + dy, 102f, 252f + dy, 66f, 238f + dy);
        line(c, 62f, 258f + dy, 100f, 274f + dy);
    }

    /** Antebrazo largo hasta la muneca. Devuelve el angulo (grados) respecto a "arriba". */
    private float armTo(Canvas c, float wx, float wy) {
        float bx = 10f, by = 380f;
        float dx = wx - bx, dy = wy - by;
        float l = (float) Math.sqrt(dx * dx + dy * dy);
        float ux = dx / l, uy = dy / l, nx = -uy, ny = ux;
        float w0 = 46f, w1 = 26f;
        poly(c, SLEEVE, bx + nx * w0, by + ny * w0, wx + nx * w1, wy + ny * w1,
                wx - nx * w1, wy - ny * w1, bx - nx * w0, by - ny * w0);
        // lado en sombra
        flat(c, SLEEVE_DARK, bx - nx * w0, by - ny * w0, wx - nx * w1, wy - ny * w1,
                wx - nx * w1 * 0.25f, wy - ny * w1 * 0.25f, bx - nx * w0 * 0.2f, by - ny * w0 * 0.2f);
        for (int i = 1; i < 7; i++) {
            float t = i / 7f;
            float cx = lerp(bx, wx, t), cy = lerp(by, wy, t), w = lerp(w0, w1, t);
            crease(c, cx + nx * w * 0.9f, cy + ny * w * 0.9f, cx + nx * w * 0.2f - ux * 6f, cy + ny * w * 0.2f - uy * 6f,
                    cx - nx * w * 0.7f - ux * 2f, cy - ny * w * 0.7f - uy * 2f);
            line(c, cx - nx * w * 0.55f, cy - ny * w * 0.55f, cx - nx * w * 0.55f - ux * 9f, cy - ny * w * 0.55f - uy * 9f);
        }
        // puno negro grueso
        poly(c, INK, wx - ux * 24f + nx * (w1 + 2f), wy - uy * 24f + ny * (w1 + 2f),
                wx - ux * 24f - nx * (w1 + 2f), wy - uy * 24f - ny * (w1 + 2f),
                wx - ux * 3f - nx * (w1 - 1f), wy - uy * 3f - ny * (w1 - 1f),
                wx - ux * 3f + nx * (w1 - 1f), wy - uy * 3f + ny * (w1 - 1f));
        flat(c, INK_SOFT, wx - ux * 20f + nx * w1 * 0.9f, wy - uy * 20f + ny * w1 * 0.9f,
                wx - ux * 17f + nx * w1 * 0.9f, wy - uy * 17f + ny * w1 * 0.9f,
                wx - ux * 17f - nx * w1 * 0.9f, wy - uy * 17f - ny * w1 * 0.9f,
                wx - ux * 20f - nx * w1 * 0.9f, wy - uy * 20f - ny * w1 * 0.9f);
        return (float) Math.toDegrees(Math.atan2(dx, -dy));
    }

    // ------------------------------------------------------------------------------------ mano derecha: pistola
    private void drawGun(Canvas c, Game g) {
        stroke.setStrokeWidth(2.4f);

        // ---- manga con pliegues
        poly(c, SLEEVE, 566f, 346f, 602f, 320f, 662f, 308f, 740f, 322f, 740f, 400f, 580f, 400f);
        c.save();
        path.reset();
        path.moveTo(566f, 346f); path.lineTo(602f, 320f); path.lineTo(662f, 308f); path.lineTo(740f, 322f);
        path.lineTo(740f, 400f); path.lineTo(580f, 400f); path.close();
        c.clipPath(path);
        flat(c, SLEEVE_DARK, 566f, 380f, 640f, 350f, 740f, 350f, 740f, 400f, 566f, 400f);
        c.restore();
        crease(c, 600f, 336f, 626f, 350f, 618f, 384f);
        crease(c, 636f, 322f, 664f, 340f, 656f, 380f);
        crease(c, 676f, 318f, 700f, 338f, 694f, 378f);
        hatchRun(c, 8, 590f, 360f, 15f, -2f, -9f, 30f);

        // ---- punio negro grueso
        poly(c, INK, 552f, 296f, 614f, 282f, 638f, 336f, 576f, 356f);
        flat(c, INK_SOFT, 560f, 300f, 612f, 288f, 616f, 298f, 564f, 311f);
        line(c, 574f, 330f, 626f, 318f);

        // ---- armazon (parte baja de la pistola) y guardamonte
        poly(c, YELLOW_DARK, 438f, 208f, 592f, 204f, 596f, 238f, 566f, 252f, 480f, 250f, 450f, 240f);
        hatchRun(c, 8, 460f, 232f, 14f, 0f, -6f, 14f);
        // guardamonte
        stroke.setStrokeWidth(8f);
        path.reset();
        path.moveTo(466f, 248f);
        path.cubicTo(466f, 296f, 536f, 298f, 542f, 250f);
        c.drawPath(path, stroke);
        stroke.setColor(YELLOW);
        stroke.setStrokeWidth(3.6f);
        c.drawPath(path, stroke);
        stroke.setColor(INK);
        stroke.setStrokeWidth(2.4f);
        // gatillo
        poly(c, INK, 497f, 246f, 509f, 246f, 505f, 268f, 497f, 270f);
        line(c, 500f, 252f, 502f, 264f);

        // ---- agarre (empunadura) con textura
        poly(c, YELLOW, 536f, 236f, 592f, 232f, 612f, 316f, 560f, 332f);
        c.save();
        path.reset();
        path.moveTo(536f, 236f); path.lineTo(592f, 232f); path.lineTo(612f, 316f); path.lineTo(560f, 332f); path.close();
        c.clipPath(path);
        flat(c, YELLOW_DARK, 586f, 232f, 620f, 232f, 620f, 330f, 604f, 330f);
        for (int i = 0; i < 9; i++) line(c, 540f + i * 8f, 330f, 560f + i * 8f, 236f);
        c.restore();
        // base del cargador
        poly(c, INK, 556f, 326f, 614f, 310f, 618f, 326f, 560f, 344f);
        line(c, 566f, 336f, 608f, 322f);

        // ---- palma y dorso de la mano
        blob(c, SKIN, 556f, 240f, 600f, 232f, 618f, 262f, 614f, 300f, 588f, 318f, 560f, 300f, 550f, 270f);
        c.save();
        clipBlob(c, 556f, 240f, 600f, 232f, 618f, 262f, 614f, 300f, 588f, 318f, 560f, 300f, 550f, 270f);
        flat(c, SKIN_SHADE, 598f, 224f, 640f, 224f, 640f, 330f, 596f, 330f, 604f, 280f);
        c.restore();
        crease(c, 576f, 252f, 582f, 272f, 576f, 296f);
        hatchRun(c, 4, 598f, 296f, 5f, 3f, -4f, 12f);

        // ---- cuatro dedos envolviendo el agarre (indice arriba, menique abajo)
        float[] fy = {254f, 274f, 294f, 313f};
        float[] fx = {512f, 504f, 508f, 520f};
        float[] fl = {52f, 62f, 58f, 46f};
        for (int i = 0; i < 4; i++) {
            float y = fy[i], x = fx[i], r = x + fl[i];
            float hh = i == 3 ? 8f : 9.5f;
            blob(c, SKIN, x, y - hh, x + 16f, y - hh - 2f, r - 12f, y - hh - 1f, r, y - 2f, r - 2f, y + hh, r - 14f, y + hh + 1f, x + 16f, y + hh + 2f, x, y + hh, x - 4f, y);
            c.save();
            clipBlob(c, x, y - hh, x + 16f, y - hh - 2f, r - 12f, y - hh - 1f, r, y - 2f, r - 2f, y + hh, r - 14f, y + hh + 1f, x + 16f, y + hh + 2f, x, y + hh, x - 4f, y);
            flat(c, SKIN_SHADE, x - 6f, y + 2f, r + 6f, y + 2f, r + 6f, y + 16f, x - 6f, y + 16f);
            c.restore();
            line(c, x + 20f, y - hh + 2f, x + 22f, y + hh - 2f);
            line(c, x + 38f, y - hh + 2f, x + 40f, y + hh - 2f);
            blob(c, 0xFFFFFFFF, x + 1f, y - 4f, x + 8f, y - 6f, x + 10f, y + 1f, x + 4f, y + 4f);
        }
        // pulgar apoyado sobre el armazon
        blob(c, SKIN, 546f, 232f, 572f, 226f, 602f, 228f, 608f, 238f, 598f, 246f, 570f, 244f, 548f, 241f);
        line(c, 572f, 232f, 574f, 242f);
        line(c, 586f, 231f, 588f, 243f);
        blob(c, 0xFFFFFFFF, 541f, 233f, 549f, 230f, 552f, 237f, 545f, 241f);

        // ---- corredera
        blob(c, YELLOW, 436f, 168f, 452f, 150f, 520f, 146f, 578f, 148f, 588f, 160f, 588f, 190f, 586f, 214f, 520f, 218f, 440f, 214f, 432f, 198f);
        c.save();
        clipBlob(c, 436f, 168f, 452f, 150f, 520f, 146f, 578f, 148f, 588f, 160f, 588f, 190f, 586f, 214f, 520f, 218f, 440f, 214f, 432f, 198f);
        flat(c, YELLOW_LIGHT, 430f, 150f, 600f, 146f, 600f, 160f, 430f, 164f);              // luz del lomo
        flat(c, YELLOW_DARK, 430f, 204f, 600f, 200f, 600f, 224f, 430f, 224f);               // sombra inferior
        c.restore();
        line(c, 438f, 164f, 584f, 160f);
        // ranura de expulsion con el casquillo
        rrect(c, 0xFF4A4218, 468f, 170f, 546f, 198f, 14f);
        rrect(c, 0xFFB7A63A, 508f, 176f, 540f, 192f, 7f);
        line(c, 512f, 181f, 534f, 180f);
        stroke.setStrokeWidth(1.6f);
        stroke.setColor(0xFFFFF3A8);
        c.drawLine(474f, 175f, 500f, 174f, stroke);
        stroke.setColor(INK);
        stroke.setStrokeWidth(2.4f);
        // estrias delanteras
        for (int i = 0; i < 4; i++) line(c, 446f + i * 5f, 172f, 442f + i * 5f, 200f);
        // ventilas traseras
        for (int i = 0; i < 3; i++) {
            float x = 556f + i * 9f;
            thick(c, 3f, x, 168f, x - 2f, 202f);
            stroke.setColor(YELLOW_LIGHT);
            stroke.setStrokeWidth(1.2f);
            c.drawLine(x + 2.5f, 170f, x + 0.5f, 200f, stroke);
            stroke.setColor(INK);
            stroke.setStrokeWidth(2.4f);
        }
        // sombra de la corredera sobre el armazon
        hatchRun(c, 9, 448f, 220f, 12f, -1f, -5f, 12f);
        // mira trasera y delantera
        poly(c, YELLOW, 560f, 134f, 584f, 134f, 588f, 148f, 558f, 148f);
        poly(c, INK, 568f, 135f, 576f, 135f, 576f, 146f, 568f, 146f);
        poly(c, YELLOW, 446f, 138f, 458f, 138f, 460f, 152f, 444f, 152f);
        // boca del canon
        blob(c, YELLOW_DARK, 416f, 176f, 432f, 172f, 436f, 192f, 432f, 208f, 416f, 204f, 410f, 190f);
        c.drawCircle(416f, 190f, 7f, stroke);
        fill.setColor(INK);
        c.drawCircle(416f, 190f, 4.6f, fill);
        fill.setColor(0xFF6A6460);
        c.drawCircle(414.5f, 188.5f, 1.4f, fill);

        // ---- fogonazo
        if (g.recoil > 0.7f) {
            float mx = 404f, my = 190f;
            for (int layer = 0; layer < 2; layer++) {
                path.reset();
                int pts = 9;
                float k = layer == 0 ? 1f : 0.55f;
                for (int i = 0; i < pts * 2; i++) {
                    double a = Math.PI * i / pts;
                    float r = ((i % 2 == 0) ? 40f : 13f) * k;
                    float x = mx - (float) Math.cos(a) * r * 1.3f;
                    float y = my + (float) Math.sin(a) * r * 1.4f;
                    if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
                }
                path.close();
                fill.setColor(layer == 0 ? 0xFFFFE680 : 0xFFFFFCE8);
                c.drawPath(path, fill);
                if (layer == 0) c.drawPath(path, stroke);
            }
            line(c, 360f, 190f, 330f, 190f);
            line(c, 366f, 170f, 342f, 158f);
            line(c, 366f, 210f, 342f, 222f);
        }
    }

    // ------------------------------------------------------------------------------------ mano izquierda
    private float anchorX = -1, anchorY = -1; // ancla del gancho en espacio de referencia (lo pone HudView)

    void setAnchor(float rx, float ry) { anchorX = rx; anchorY = ry; }

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
        double a = Math.toRadians(ang);
        float tx = wx + (float) Math.sin(a) * 66f, ty = wy - (float) Math.cos(a) * 66f;
        float ax = anchorX >= 0f ? anchorX : 320f, ay = anchorX >= 0f ? anchorY : 170f;
        if (anchorX < 0f) { ax = tx + (float) Math.sin(a) * 300f; ay = ty - (float) Math.cos(a) * 300f; }
        float ex = lerp(tx, ax, p), ey = lerp(ty, ay, p);
        rope(c, tx, ty, (tx + ex) * 0.5f, (ty + ey) * 0.5f, ex, ey, 2.6f);
        float dir = (float) Math.toDegrees(Math.atan2(ex - tx, -(ey - ty)));
        kunai(c, ex, ey, dir, 0.6f);
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

    /** Puno cerrado visto de espalda: nudillos, dedos doblados y pulgar. Origen = esquina de la muneca. */
    private void fistBody(Canvas c) {
        // dorso del puno
        blob(c, SKIN, 60f, 218f, 70f, 198f, 96f, 190f, 124f, 194f, 134f, 214f, 132f, 240f, 110f, 254f, 78f, 250f, 62f, 238f);
        c.save();
        clipBlob(c, 60f, 218f, 70f, 198f, 96f, 190f, 124f, 194f, 134f, 214f, 132f, 240f, 110f, 254f, 78f, 250f, 62f, 238f);
        flat(c, SKIN_SHADE, 60f, 238f, 140f, 232f, 140f, 260f, 60f, 260f);
        c.restore();
        // cuatro dedos doblados (se ven los nudillos y las falanges)
        for (int i = 0; i < 4; i++) {
            float y = 199f + i * 12f;
            blob(c, SKIN, 96f, y, 112f, y - 3f, 132f, y - 1f, 140f, y + 6f, 132f, y + 13f, 112f, y + 12f, 96f, y + 11f);
            line(c, 120f, y + 1f, 122f, y + 11f);
            line(c, 104f, y + 1f, 105f, y + 9f);
        }
        // pulgar cruzado al frente
        blob(c, SKIN, 66f, 228f, 82f, 220f, 106f, 224f, 112f, 234f, 100f, 242f, 80f, 244f, 66f, 240f);
        line(c, 90f, 224f, 92f, 240f);
        blob(c, 0xFFFFFFFF, 100f, 227f, 108f, 229f, 108f, 236f, 100f, 237f);
        hatchRun(c, 4, 70f, 242f, 9f, 1f, 3f, 9f);
        // arrugas del dorso
        crease(c, 78f, 198f, 84f, 210f, 80f, 220f);
    }

    /** Dedo arriba con el kunai colgando de la cuerda y balanceandose. */
    private void drawIdleKunai(Canvas c, float time) {
        cuff(c, 0f);
        // dorso de la mano y puno
        blob(c, SKIN, 62f, 214f, 74f, 190f, 106f, 184f, 124f, 196f, 128f, 226f, 116f, 246f, 84f, 248f, 66f, 236f);
        c.save();
        clipBlob(c, 62f, 214f, 74f, 190f, 106f, 184f, 124f, 196f, 128f, 226f, 116f, 246f, 84f, 248f, 66f, 236f);
        flat(c, SKIN_SHADE, 108f, 180f, 140f, 180f, 140f, 260f, 100f, 260f);
        c.restore();
        // dedos doblados (nudillos)
        for (int i = 0; i < 3; i++) {
            float y = 196f + i * 13f;
            blob(c, SKIN, 106f, y, 122f, y - 2f, 134f, y + 2f, 134f, y + 10f, 122f, y + 14f, 106f, y + 12f);
            line(c, 120f, y + 1f, 121f, y + 12f);
        }
        // pulgar doblado
        blob(c, SKIN, 64f, 226f, 78f, 218f, 98f, 222f, 102f, 234f, 84f, 242f, 66f, 238f);
        line(c, 84f, 222f, 86f, 238f);
        crease(c, 76f, 244f, 90f, 248f, 106f, 244f);
        // indice extendido
        c.save();
        c.translate(98f, 196f);
        finger(c, 0f, 88f, 20f);
        c.restore();
        float tx = 98f, ty = 108f;
        float swing = 1.05f + 0.55f * (float) Math.sin(time * 3.1f);   // radianes desde la vertical
        float rx = tx + (float) Math.sin(swing) * 52f, ry = ty - (float) Math.cos(swing) * 52f;
        // cuerda desde la punta del dedo hasta el anillo
        rope(c, tx, ty + 6f, (tx + rx) * 0.5f + 6f, (ty + ry) * 0.5f + 14f, rx, ry, 2.4f);
        // vuelta de cuerda alrededor del dedo
        stroke.setColor(ROPE);
        stroke.setStrokeWidth(2f);
        c.drawLine(88f, 124f, 108f, 128f, stroke);
        c.drawLine(88f, 131f, 108f, 135f, stroke);
        stroke.setColor(INK);
        stroke.setStrokeWidth(2.4f);
        // kunai colgando: la punta se aleja del anillo
        float deg = (float) Math.toDegrees(swing) + 62f;
        kunaiHanging(c, rx, ry, deg, 0.95f);
        // rayitas de movimiento
        line(c, rx + 14f, ry - 22f, rx + 30f, ry - 32f);
        line(c, rx + 22f, ry - 10f, rx + 40f, ry - 16f);
        line(c, rx + 10f, ry - 34f, rx + 20f, ry - 46f);
    }

    /** Kunai colgado: el anillo (pomo) queda en (x,y) y el cuerpo sale hacia la direccion deg. */
    private void kunaiHanging(Canvas c, float x, float y, float deg, float s) {
        // el origen del kunai es el centro; el anillo esta en +30 sobre el eje. Se desplaza para que el anillo caiga en (x,y).
        double a = Math.toRadians(deg);
        float cx = x + (float) Math.sin(a) * 30f * s, cy = y - (float) Math.cos(a) * 30f * s;
        kunai(c, cx, cy, deg, s);
    }

    /** Puno con la cuerda tensa hacia el punto donde se clavo el kunai. */
    private void drawRopeFist(Canvas c, Game g, float time) {
        float shake = (float) Math.sin(time * 40f) * 1.2f;
        c.save();
        c.translate(shake, -shake * 0.6f);
        cuff(c, 0f);
        float fx = 112f, fy = 196f;
        float tx = anchorX, ty = anchorY;
        if (tx < 0f) { tx = 320f; ty = 170f; }
        // cola de cuerda colgando del puno (detras)
        float sw = (float) Math.sin(time * 7f) * 5f;
        rope(c, 88f, 246f, 64f + sw, 286f, 92f, 326f, 3.6f);
        rope(c, 94f, 326f, 100f, 336f, 90f, 346f, 3.0f);
        fistBody(c);
        // cuerda tensa al ancla
        rope(c, fx, fy, (fx + tx) * 0.5f, (fy + ty) * 0.5f + 5f, tx, ty, 3.0f);
        // vueltas de cuerda alrededor del puno
        stroke.setColor(ROPE);
        stroke.setStrokeWidth(2.6f);
        path.reset();
        path.moveTo(96f, 192f); path.quadTo(112f, 200f, 128f, 194f);
        c.drawPath(path, stroke);
        path.reset();
        path.moveTo(94f, 244f); path.quadTo(112f, 252f, 130f, 242f);
        c.drawPath(path, stroke);
        stroke.setColor(INK);
        stroke.setStrokeWidth(2.4f);
        // kunai clavado en el ancla
        float dir = (float) Math.toDegrees(Math.atan2(tx - fx, -(ty - fy)));
        kunaiHanging(c, tx, ty, dir + 180f, 0.5f);
        // marquitas de tension
        line(c, 54f, 196f, 42f, 190f);
        line(c, 56f, 184f, 46f, 172f);
        line(c, 68f, 178f, 62f, 164f);
        c.restore();
    }

    // ------------------------------------------------------------------------------------ dibujo principal
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
