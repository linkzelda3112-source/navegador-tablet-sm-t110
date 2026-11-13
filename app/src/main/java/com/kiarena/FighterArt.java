package com.kiarena;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * Dibuja a los guerreros por código (sin imágenes). Todo está en "unidades" donde el cuerpo mide
 * ~100 de alto, con los pies en y=0 y el frente hacia +x; 's' son píxeles por unidad.
 */
final class FighterArt {
    private FighterArt() {}

    static final int P_IDLE = 0, P_PUNCH = 1, P_BLAST = 2, P_GUARD = 3, P_HIT = 4, P_KO = 5,
            P_CHARGE = 6, P_ULTI = 7, P_HEAL = 8;

    private static final Path path = new Path();
    private static final RectF rf = new RectF();

    static int shade(int color, float k) {
        int a = color >>> 24;
        int r = Math.min(255, Math.round(((color >> 16) & 255) * k));
        int g = Math.min(255, Math.round(((color >> 8) & 255) * k));
        int b = Math.min(255, Math.round((color & 255) * k));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // ------------------------------------------------------------------ cuerpo completo

    static void draw(Canvas c, Paint p, Fighter f, float x, float y, float s, boolean right,
                     int pose, float t, boolean buffed, int alpha) {
        if (alpha <= 0) return;
        c.save();
        if (alpha < 255) c.saveLayerAlpha(x - 200, y - 380, x + 200, y + 30, alpha);
        c.translate(x, y);
        c.scale(right ? s : -s, s);

        float b = f.build;
        float bob = (pose == P_IDLE || pose == P_GUARD) ? (float) Math.sin(t * 3.2f) * 1.4f : 0f;

        // sombra
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x55000000);
        rf.set(-24 * b, -4, 24 * b, 4);
        c.drawOval(rf, p);

        if (pose == P_KO) {
            c.rotate(-82, -6, 0);
            c.translate(-4, -2);
        }

        boolean strong = pose == P_CHARGE || pose == P_ULTI || buffed;
        if (strong) aura(c, p, f.aura, pose == P_ULTI ? 1.6f : 1.0f, t);
        if (pose == P_HEAL) aura(c, p, 0xFF69F0AE, 1.0f, t);

        // pose: posiciones de manos y inclinación
        float lean = 0, hipY = -42 + bob * 0.5f;
        float bx, by, fx, fy; // mano trasera y delantera
        switch (pose) {
            case P_PUNCH: lean = 7; bx = 8; by = -62; fx = 48; fy = -78; break;
            case P_BLAST: lean = 4; bx = 38; by = -70; fx = 42; fy = -78; break;
            case P_GUARD: bx = 18; by = -82; fx = 20; fy = -70; break;
            case P_HIT: lean = -12; bx = -8; by = -58; fx = -4; fy = -70; break;
            case P_KO: bx = -6; by = -50; fx = 10; fy = -44; break;
            case P_CHARGE: lean = 3; hipY = -36; bx = 8; by = -46; fx = 16; fy = -50; break;
            case P_ULTI: bx = 20; by = -112; fx = 28; fy = -108; break;
            case P_HEAL: bx = 30; by = -62; fx = 32; fy = -68; break;
            default: bx = 14; by = -68 + bob; fx = 26; fy = -76 + bob; break;
        }

        int pants = shade(f.gi, 0.7f);
        int boots = shade(f.gi, 0.4f);

        // piernas
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStyle(Paint.Style.STROKE);
        float lw = 10.5f * b;
        float kneeBend = (pose == P_CHARGE) ? 6 : 0;
        p.setColor(pants);
        p.setStrokeWidth(lw);
        c.drawLine(-4, hipY, -14, -3, p);               // pierna trasera
        c.drawLine(4, hipY, 14 + kneeBend, -3, p);      // pierna delantera
        p.setColor(boots);
        p.setStrokeWidth(lw * 0.95f);
        c.drawLine(-14, -3, -22 + (lean < 0 ? -3 : 0), -1, p);
        c.drawLine(14 + kneeBend, -3, 24 + kneeBend, -1, p);

        // tronco + cabeza (inclinados)
        c.save();
        c.translate(0, hipY);
        c.rotate(lean);
        c.translate(0, -hipY);

        // brazo trasero (detrás del torso)
        arm(c, p, f, -9 * b, -72 + bob, bx, by, b);

        // torso
        p.setStyle(Paint.Style.FILL);
        p.setColor(f.gi);
        rf.set(-12 * b, -78 + bob, 12 * b, hipY + 2);
        c.drawRoundRect(rf, 7, 7, p);
        p.setColor(f.skin); // pecho en V
        path.reset();
        path.moveTo(-5 * b, -78 + bob);
        path.lineTo(5 * b, -78 + bob);
        path.lineTo(0, -62 + bob);
        path.close();
        c.drawPath(path, p);
        p.setColor(shade(f.gi, 0.55f)); // cinturón
        rf.set(-12.5f * b, hipY - 5, 12.5f * b, hipY + 1);
        c.drawRect(rf, p);

        // cabeza
        c.save();
        c.translate(3, -90 + bob);
        drawHead(c, p, f);
        c.restore();

        // brazo delantero (encima)
        arm(c, p, f, 8 * b, -72 + bob, fx, fy, b);
        c.restore();

        // escudo de guardia
        if (pose == P_GUARD) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2.5f);
            p.setColor(0xAAFFFFFF);
            rf.set(8, -112, 52, -40);
            c.drawOval(rf, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0x334FC3F7);
            c.drawOval(rf, p);
        }
        c.restore();
        if (alpha < 255) c.restore();
    }

    private static void arm(Canvas c, Paint p, Fighter f, float sx, float sy, float hx, float hy, float b) {
        float dx = hx - sx, dy = hy - sy;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f) len = 0.01f;
        float sign = (hy < sy - 15) ? -1f : 1f;
        float ex = (sx + hx) / 2f + (-dy / len) * 7f * sign;
        float ey = (sy + hy) / 2f + (dx / len) * 7f * sign;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(8.5f * b);
        p.setColor(f.skin);
        c.drawLine(sx, sy, ex, ey, p);
        c.drawLine(ex, ey, hx, hy, p);
        p.setColor(shade(f.gi, 0.55f)); // muñequera
        p.setStrokeWidth(9.5f * b);
        float k = 0.82f;
        c.drawLine(ex + (hx - ex) * k, ey + (hy - ey) * k, hx, hy, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(f.skin);
        c.drawCircle(hx, hy, 5.2f * b, p);
    }

    private static void aura(Canvas c, Paint p, int color, float k, float t) {
        int rgb = color & 0xFFFFFF;
        p.setStyle(Paint.Style.FILL);
        float fl = 1f + 0.06f * (float) Math.sin(t * 14f);
        for (int i = 0; i < 3; i++) {
            float r = (30 + i * 9) * k * fl;
            p.setColor(((38 + i * 18) << 24) | rgb);
            rf.set(-r * 0.85f, -55 - r * 1.35f, r * 0.85f, -55 + r * 1.1f);
            c.drawOval(rf, p);
        }
        // llamas que suben
        p.setColor((150 << 24) | rgb);
        for (int i = 0; i < 7; i++) {
            float ph = ((t * 1.4f + i * 0.31f) % 1f);
            float bx = -26 + i * 8.7f;
            float h = 38 * k * (1f - ph);
            float by = -10 - ph * 80 * k;
            path.reset();
            path.moveTo(bx - 4, by);
            path.lineTo(bx, by - h * 0.5f);
            path.lineTo(bx + 4, by);
            path.close();
            c.drawPath(path, p);
        }
    }

    // ---------------------------------------------------------------------- cabeza

    /** Dibuja la cabeza con su centro en el origen (radio 12), mirando hacia +x. */
    static void drawHead(Canvas c, Paint p, Fighter f) {
        int hair = f.hair;
        int outline = shade(hair, 0.55f);

        // melena larga por detrás
        if (f.hairStyle == 1) {
            hairPath(c, p, hair, outline, new float[]{-12, -6, -20, 10, -36, 34, -18, 28, -9, 12});
        }
        if (f.hairStyle == 4) {
            hairPath(c, p, hair, outline, new float[]{-12, -2, -26, 12, -22, 30, -12, 22, -8, 8});
        }
        // cabeza
        p.setStyle(Paint.Style.FILL);
        p.setColor(f.skin);
        c.drawCircle(0, 0, 12, p);
        // orejas / cuernos del alienígena
        if (f.hairStyle == 3) {
            hairPath(c, p, shade(f.skin, 0.75f), shade(f.skin, 0.5f), new float[]{-6, -9, -10, -27, -1, -11});
            hairPath(c, p, shade(f.skin, 0.75f), shade(f.skin, 0.5f), new float[]{3, -11, 10, -26, 10, -7});
        }
        // pelo
        switch (f.hairStyle) {
            case 0: // puntas hacia arriba
                hairPath(c, p, hair, outline, new float[]{-13, 3, -18, -9, -10, -9, -12, -24, -4, -14,
                        1, -29, 6, -14, 13, -25, 12, -8, 19, -10, 13, 3});
                break;
            case 1: // largo con flequillo
                hairPath(c, p, hair, outline, new float[]{-13, 3, -14, -10, -4, -17, 8, -15, 14, -6, 13, 3,
                        8, -5, 1, -3, -4, -6});
                break;
            case 2: // cresta
                hairPath(c, p, hair, outline, new float[]{-13, 3, -13, -8, -8, -11, -3, -31, 6, -29,
                        10, -11, 13, -8, 13, 3});
                break;
            case 4: // peinado hacia atrás
                hairPath(c, p, hair, outline, new float[]{-13, 3, -32, -4, -18, -8, -34, -16, -14, -16,
                        -22, -28, -2, -20, 4, -28, 8, -16, 16, -12, 13, 3});
                break;
            default:
                break;
        }
        // ojos y boca
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFFFFFFF);
        rf.set(0, 1, 6, 7);
        c.drawOval(rf, p);
        rf.set(7, 1, 12, 7);
        c.drawOval(rf, p);
        p.setColor(f.hairStyle == 3 ? 0xFFFF1744 : 0xFF111111);
        c.drawCircle(4.3f, 4, 1.9f, p);
        c.drawCircle(9.6f, 4, 1.7f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.1f);
        p.setColor(0xFF111111);
        c.drawLine(0, 0, 6, 1.5f, p);       // ceja
        c.drawLine(7, 1.5f, 12, 0, p);
        c.drawLine(5, 10, 9, 10, p);        // boca
    }

    private static void hairPath(Canvas c, Paint p, int fill, int stroke, float[] pts) {
        path.reset();
        path.moveTo(pts[0], pts[1]);
        for (int i = 2; i < pts.length; i += 2) path.lineTo(pts[i], pts[i + 1]);
        path.close();
        p.setStyle(Paint.Style.FILL);
        p.setColor(fill);
        c.drawPath(path, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.3f);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setColor(stroke);
        c.drawPath(path, p);
    }

    // -------------------------------------------------------------------- retrato

    /** Retrato de cabeza y hombros con el centro de la cabeza en (cx, cy); 'size' = alto aprox. */
    static void portrait(Canvas c, Paint p, Fighter f, float cx, float cy, float size, boolean dim) {
        c.save();
        c.translate(cx, cy);
        float k = size / 52f;
        c.scale(k, k);
        p.setStyle(Paint.Style.FILL);
        p.setColor(f.gi);
        rf.set(-22, 12, 22, 40);
        c.drawRoundRect(rf, 10, 10, p);
        p.setColor(f.skin);
        path.reset();
        path.moveTo(-6, 12);
        path.lineTo(6, 12);
        path.lineTo(0, 26);
        path.close();
        c.drawPath(path, p);
        c.scale(1.15f, 1.15f);
        drawHead(c, p, f);
        c.restore();
        if (dim) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xAA000000);
            c.drawRect(cx - size * 0.6f, cy - size * 0.7f, cx + size * 0.6f, cy + size * 0.55f, p);
        }
    }
}
