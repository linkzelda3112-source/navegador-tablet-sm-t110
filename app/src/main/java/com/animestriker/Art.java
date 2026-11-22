package com.animestriker;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

/** Dibujo estilo anime (tinta + cel-shading) de personajes, balón y texto. Todo por código. */
final class Art {
    static final int INK = 0xFF15152B;
    static final Paint P = new Paint(Paint.ANTI_ALIAS_FLAG);
    static final Path PA = new Path(), PB = new Path();
    static final RectF R = new RectF();
    static Typeface BOLD, ITALIC;

    // estilos de pelo por jugador (0..5)
    static final int[] NB = {5, 6, 3, 7, 8, 9};
    static final float[] LB = {0.45f, 0.65f, 0.35f, 1.0f, 1.2f, 1.55f};
    static final int[] NF = {3, 3, 2, 4, 4, 5};
    static final float[] LF = {0.35f, 0.5f, 0.6f, 0.55f, 0.6f, 0.75f};

    static void init() {
        BOLD = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
        ITALIC = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC);
    }

    // ------------------------------------------------------------ colores
    static int shade(int c, float f) {
        int a = c >>> 24, r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        if (f <= 1f) { r = (int) (r * f); g = (int) (g * f); b = (int) (b * f); }
        else { float k = f - 1f; r += (int) ((255 - r) * k); g += (int) ((255 - g) * k); b += (int) ((255 - b) * k); }
        return (a << 24) | (clamp255(r) << 16) | (clamp255(g) << 8) | clamp255(b);
    }
    static int clamp255(int v) { return v < 0 ? 0 : (v > 255 ? 255 : v); }
    static int alpha(int c, int a) { return (a << 24) | (c & 0xFFFFFF); }
    static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return 0xFF000000 | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    static void fill(int c) { P.setShader(null); P.setStyle(Paint.Style.FILL); P.setColor(c); }
    static void stroke(int c, float w) {
        P.setShader(null); P.setStyle(Paint.Style.STROKE); P.setColor(c); P.setStrokeWidth(w);
        P.setStrokeCap(Paint.Cap.ROUND); P.setStrokeJoin(Paint.Join.ROUND);
    }

    static void oval(Canvas cv, float cx, float cy, float rx, float ry, int col, float ink) {
        R.set(cx - rx, cy - ry, cx + rx, cy + ry);
        fill(col); cv.drawOval(R, P);
        if (ink > 0) { stroke(INK, ink); cv.drawOval(R, P); }
    }

    static void line(Canvas cv, float x0, float y0, float x1, float y1, float w, int col) {
        stroke(col, w); cv.drawLine(x0, y0, x1, y1, P);
    }

    /** Segmento con contorno de tinta. */
    static void limb(Canvas cv, float x0, float y0, float x1, float y1, float w, int col, float ink) {
        line(cv, x0, y0, x1, y1, w + ink * 2f, INK);
        line(cv, x0, y0, x1, y1, w, col);
    }

    static void text(Canvas cv, String s, float x, float y, float size, int fill, int ink, float inkW,
                     Paint.Align al, boolean italic) {
        P.setShader(null);
        P.setTypeface(italic ? ITALIC : BOLD);
        P.setTextSize(size); P.setTextAlign(al);
        if (inkW > 0) {
            P.setStyle(Paint.Style.STROKE); P.setStrokeWidth(inkW); P.setColor(ink);
            P.setStrokeJoin(Paint.Join.ROUND);
            cv.drawText(s, x, y, P);
        }
        P.setStyle(Paint.Style.FILL); P.setColor(fill);
        cv.drawText(s, x, y, P);
    }

    // ------------------------------------------------------------ cara
    /** emo: 0 normal, 1 decidido, 2 feliz, 3 grito. fl = orientación (-1 izq .. 1 der). */
    static void face(Canvas cv, Teams.Def d, int idx, float cx, float cy, float S, float fl,
                     float t, int emo, float speed) {
        int skin = d.skin[idx], hair = d.hair[idx], eye = d.eye[idx];
        float R0 = S;
        float sway = 0.05f + Math.min(speed, 12f) / 30f;

        // pelo de atrás (picos)
        int nB = NB[idx]; float lenB = LB[idx];
        PA.reset();
        float th0 = 30f, th1 = 235f;
        for (int i = 0; i <= nB; i++) {
            float a = th0 + (th1 - th0) * i / nB, rad = (float) Math.toRadians(a);
            float bxp = cx + fl * (float) Math.cos(rad) * R0, byp = cy - (float) Math.sin(rad) * R0;
            if (i == 0) PA.moveTo(bxp, byp); else PA.lineTo(bxp, byp);
            if (i < nB) {
                float a2 = th0 + (th1 - th0) * (i + 0.5f) / nB;
                float tipLen = R0 + lenB * S * (0.75f + 0.5f * (float) Math.sin(i * 2.3f + idx));
                float tipA = a2 + 10f + (float) Math.sin(t * 9f + i) * sway * 40f;
                float tr = (float) Math.toRadians(tipA);
                PA.lineTo(cx + fl * (float) Math.cos(tr) * tipLen, cy - (float) Math.sin(tr) * tipLen);
            }
        }
        PA.lineTo(cx, cy); PA.close();
        fill(shade(hair, 0.8f)); cv.drawPath(PA, P);
        stroke(shade(hair, 0.35f), 0.05f * S); cv.drawPath(PA, P);

        // cabeza
        oval(cv, cx, cy, R0, R0 * 0.96f, skin, 0.05f * S);
        // sombra cel bajo la barbilla
        fill(alpha(shade(skin, 0.8f), 120));
        R.set(cx - R0 * 0.8f, cy + R0 * 0.55f, cx + R0 * 0.8f, cy + R0 * 0.9f);
        cv.drawOval(R, P);
        // mejilla
        float o = fl * 0.36f * S;
        fill(0x55FF5C8A);
        R.set(cx + o + fl * 0.35f * S - 0.17f * S, cy + 0.32f * S, cx + o + fl * 0.35f * S + 0.17f * S, cy + 0.5f * S);
        cv.drawOval(R, P);

        // ojos
        float ey = cy + 0.06f * S;
        for (int k = 0; k < 2; k++) {
            float ex = cx + o + (k == 0 ? -0.3f : 0.3f) * S;
            boolean back = (k == 0) == (fl >= 0);
            float sz = back ? 1f - 0.2f * Math.abs(fl) : 1f;
            float rx = 0.24f * S * sz, ry = 0.34f * S;
            if (emo == 2) {
                PB.reset();
                PB.moveTo(ex - rx, ey + 0.08f * S); PB.quadTo(ex, ey - 0.28f * S, ex + rx, ey + 0.08f * S);
                stroke(INK, 0.08f * S); cv.drawPath(PB, P);
            } else {
                oval(cv, ex, ey, rx, ry, 0xFFFFFFFF, 0.05f * S);
                oval(cv, ex + fl * 0.04f * S, ey + 0.03f * S, rx * 0.74f, ry * 0.82f, eye, 0);
                fill(shade(eye, 0.55f));
                R.set(ex + fl * 0.04f * S - rx * 0.74f, ey + 0.03f * S - ry * 0.82f, ex + fl * 0.04f * S + rx * 0.74f, ey - 0.06f * S);
                cv.drawOval(R, P);
                oval(cv, ex + fl * 0.05f * S, ey + 0.05f * S, rx * 0.36f, ry * 0.5f, INK, 0);
                fill(0xFFFFFFFF);
                cv.drawCircle(ex - 0.07f * S * sz, ey - 0.1f * S, 0.065f * S, P);
                cv.drawCircle(ex + 0.07f * S * sz, ey + 0.12f * S, 0.035f * S, P);
                // pestaña superior
                PB.reset();
                PB.moveTo(ex - rx - 0.02f * S, ey - ry * 0.3f); PB.quadTo(ex, ey - ry * 1.25f, ex + rx + 0.02f * S, ey - ry * 0.45f);
                stroke(INK, 0.09f * S); cv.drawPath(PB, P);
            }
            // ceja
            float bl = 0.22f * S, by = ey - 0.5f * S;
            float tilt = (emo == 1 || emo == 3) ? 0.12f * S : (emo == 2 ? -0.05f * S : 0f);
            // extremo interior (hacia el otro ojo) más bajo cuando está decidido
            if (k == 0) line(cv, ex - bl, by - tilt * 0.5f, ex + bl, by + tilt, 0.07f * S, INK);
            else line(cv, ex - bl, by + tilt, ex + bl, by - tilt * 0.5f, 0.07f * S, INK);
        }

        // nariz y boca
        float mx = cx + o * 1.2f, my = cy + 0.6f * S;
        line(cv, cx + o * 1.8f, cy + 0.32f * S, cx + o * 1.8f + fl * 0.05f * S, cy + 0.38f * S, 0.05f * S, shade(skin, 0.55f));
        if (emo == 3) {
            oval(cv, mx, my, 0.2f * S, 0.17f * S, INK, 0);
            oval(cv, mx, my + 0.07f * S, 0.12f * S, 0.07f * S, 0xFFFF6B81, 0);
        } else if (emo == 2) {
            PB.reset();
            PB.moveTo(mx - 0.2f * S, my - 0.04f * S); PB.quadTo(mx, my + 0.3f * S, mx + 0.2f * S, my - 0.04f * S); PB.close();
            fill(INK); cv.drawPath(PB, P);
            oval(cv, mx, my + 0.1f * S, 0.1f * S, 0.05f * S, 0xFFFF6B81, 0);
        } else {
            PB.reset();
            PB.moveTo(mx - 0.13f * S, my); PB.quadTo(mx, my + (emo == 1 ? 0.0f : 0.09f) * S, mx + 0.13f * S, my);
            stroke(INK, 0.06f * S); cv.drawPath(PB, P);
        }

        // flequillo
        int nF = NF[idx]; float lenF = LF[idx];
        PA.reset();
        float f0 = 12f, f1 = 168f;
        for (int i = 0; i <= nF; i++) {
            float a = f0 + (f1 - f0) * i / nF, rad = (float) Math.toRadians(a);
            float bxp = cx + fl * (float) Math.cos(rad) * R0 * 1.02f, byp = cy - (float) Math.sin(rad) * R0 * 1.02f;
            if (i == 0) PA.moveTo(bxp, byp); else PA.lineTo(bxp, byp);
            if (i < nF) {
                float a2 = f0 + (f1 - f0) * (i + 0.5f) / nF, r2 = (float) Math.toRadians(a2);
                float sn = (float) Math.sin(r2);
                float tx = cx + fl * (float) Math.cos(r2) * R0 * 0.66f;
                float ty = cy - sn * R0 * 0.55f + lenF * S * 0.5f * sn + (float) Math.sin(t * 8f + i) * sway * 0.15f * S;
                PA.lineTo(tx, ty);
            }
        }
        PA.lineTo(cx, cy - R0 * 0.25f); PA.close();
        fill(hair); cv.drawPath(PA, P);
        stroke(shade(hair, 0.35f), 0.04f * S); cv.drawPath(PA, P);
        // brillo del pelo
        PB.reset();
        for (int i = 0; i < 6; i++) {
            float a = 38f + i * 9f, rad = (float) Math.toRadians(a);
            float px = cx + fl * (float) Math.cos(rad) * R0 * 0.84f, py = cy - (float) Math.sin(rad) * R0 * 0.84f;
            if (i == 0) PB.moveTo(px, py); else PB.lineTo(px, py);
        }
        stroke(shade(hair, 1.6f), 0.08f * S); cv.drawPath(PB, P);

        // cinta del capitán
        if (idx == 5) {
            int band = d.accent == 0xFF4A0E0E ? 0xFFFFC107 : d.second == 0xFFFFFFFF ? 0xFFFFFFFF : d.second;
            PB.reset();
            PB.moveTo(cx - fl * 0.98f * S, cy - 0.38f * S); PB.quadTo(cx, cy - 0.62f * S, cx + fl * 0.98f * S, cy - 0.38f * S);
            stroke(INK, 0.26f * S); cv.drawPath(PB, P);
            stroke(band, 0.17f * S); cv.drawPath(PB, P);
            float fx = (float) Math.sin(t * 10f) * 0.25f * S;
            line(cv, cx - fl * 0.98f * S, cy - 0.38f * S, cx - fl * 1.9f * S, cy - 0.2f * S + fx, 0.14f * S, band);
            line(cv, cx - fl * 0.98f * S, cy - 0.38f * S, cx - fl * 1.75f * S, cy - 0.55f * S - fx, 0.14f * S, band);
        }
    }

    // ------------------------------------------------------------ jugador
    static int gkColor(Teams.Def d) { return d.main == 0xFF00C853 ? 0xFFFF9100 : 0xFF39FF14; }

    /** Tubo cónico con contorno fino del mismo tono (estilo anime semi-realista). */
    static void tube(Canvas cv, float x0, float y0, float x1, float y1, float w0, float w1, int col, float ow) {
        float dx = x1 - x0, dy = y1 - y0, l = (float) Math.sqrt(dx * dx + dy * dy);
        if (l < 0.001f) return;
        float nx = -dy / l, ny = dx / l;
        PB.reset();
        PB.moveTo(x0 + nx * w0 / 2, y0 + ny * w0 / 2); PB.lineTo(x1 + nx * w1 / 2, y1 + ny * w1 / 2);
        PB.lineTo(x1 - nx * w1 / 2, y1 - ny * w1 / 2); PB.lineTo(x0 - nx * w0 / 2, y0 - ny * w0 / 2); PB.close();
        fill(col); cv.drawPath(PB, P);
        cv.drawCircle(x0, y0, w0 / 2, P); cv.drawCircle(x1, y1, w1 / 2, P);
        if (ow > 0) { stroke(shade(col, 0.5f), ow); cv.drawPath(PB, P); }
    }

    /** Franja plana (sin tapas redondas ni contorno). */
    static void band(Canvas cv, float x0, float y0, float x1, float y1, float w0, float w1, int col) {
        float dx = x1 - x0, dy = y1 - y0, l = (float) Math.sqrt(dx * dx + dy * dy);
        if (l < 0.001f) return;
        float nx = -dy / l, ny = dx / l;
        PB.reset();
        PB.moveTo(x0 + nx * w0 / 2, y0 + ny * w0 / 2); PB.lineTo(x1 + nx * w1 / 2, y1 + ny * w1 / 2);
        PB.lineTo(x1 - nx * w1 / 2, y1 - ny * w1 / 2); PB.lineTo(x0 - nx * w0 / 2, y0 - ny * w0 / 2); PB.close();
        fill(col); cv.drawPath(PB, P);
    }

    static void body(Canvas cv, Match m, Match.Player pl, float gx, float gy, float S, float t, boolean human) {
        float U = S * 0.62f;                 // 1 cabeza ≈ 1U; el jugador mide ~7U
        float ow = Math.max(1.2f, U * 0.06f);
        Teams.Def d = m.def[pl.team];
        boolean gk = pl.role == Match.GK;
        int main = gk ? gkColor(d) : d.main;
        int trim = gk ? 0xFF263238 : d.second;
        int shorts = gk ? 0xFF263238 : shade(d.main, 0.82f);
        int sock = gk ? shade(main, 0.9f) : shade(d.main, 0.95f);
        int skin = d.skin[pl.idx];
        float fl = pl.flip, afl = Math.abs(fl);
        float sp = (float) Math.sqrt(pl.vx * pl.vx + pl.vy * pl.vy);
        float run = Math.min(1f, sp / 8f);
        float ph = pl.anim;
        float bob = sp > 0.8f ? Math.abs((float) Math.sin(ph)) * 0.16f : (float) Math.sin(t * 2f + pl.idx) * 0.03f;
        float lean = Math.min(1f, sp / 9f) * 0.3f;
        boolean slide = pl.slide > 0;
        boolean fall = pl.stun > 0.18f && !slide;
        float jump = 0;
        if (pl.diveT > 0) { float k = 1f - pl.diveT / 0.55f; jump = (float) Math.sin(Math.PI * Math.max(0, Math.min(1, k))) * 1.6f; }
        if (pl.cheer > 0) jump = Math.max(jump, Math.abs((float) Math.sin(t * 9f + pl.idx)) * 1.1f);

        // sombra larga (luz desde la izquierda)
        cv.save();
        cv.rotate(-5f, gx, gy);
        fill(0x22000000); R.set(gx - 1.0f * U, gy - 0.5f * U, gx + 4.6f * U, gy + 0.5f * U); cv.drawOval(R, P);
        fill(0x44000000); R.set(gx - 0.7f * U, gy - 0.32f * U, gx + 3.8f * U * (1f - jump * 0.15f), gy + 0.34f * U); cv.drawOval(R, P);
        cv.restore();

        cv.save();
        float gyJ = gy - jump * U;
        if (slide) { cv.rotate(-80f * fl, gx, gyJ - 2.2f * U); cv.translate(0, 0.9f * U); }
        else if (fall) { cv.rotate(-28f * fl, gx, gyJ); }

        float hipY = gyJ - (3.55f + bob) * U;
        float shY = gyJ - (5.5f + bob) * U;
        float waistX = gx + fl * lean * U * 0.6f, shX = gx + fl * lean * U * 1.5f;
        float headX = gx + fl * lean * U * 2.1f, headY = gyJ - (6.5f + bob) * U;
        float halfW = (0.78f - 0.2f * afl) * U, waistHalf = halfW * 0.72f;

        // ---- brazo lejano
        arm(cv, 0, fl, ph, sp, run, shX, shY, halfW, U, main, trim, skin, gk, pl, ow, jump);

        // ---- piernas
        for (int k = 0; k < 2; k++) {
            float phase = ph + k * (float) Math.PI;
            float amp = sp > 0.8f ? 0.5f + 0.9f * run : 0.05f;
            float fxo = (float) Math.sin(phase) * amp * 1.1f;
            float lift = sp > 0.8f ? Math.max(0f, (float) Math.cos(phase)) * 0.95f * run : 0f;
            float hx = waistX + fl * (k == 0 ? -0.26f : 0.26f) * U;
            float ftx = hx + fl * fxo * U, fty = gyJ - (0.28f + lift) * U;
            if (slide) { ftx = hx + fl * (k == 0 ? 2.2f : 2.9f) * U; fty = gyJ - (k == 0 ? 0.3f : 1.0f) * U; }
            if (k == 1 && pl.actionT > 0.02f && !gk) {
                float r = Math.min(1f, pl.actionT / 0.3f);
                ftx = hx + fl * (2.6f * r - 0.3f) * U; fty = gyJ - (0.3f + 1.1f * r) * U;
            }
            if (jump > 0.2f && gk) { ftx = hx + fl * (k == 0 ? -0.9f : 0.9f) * U; fty = gyJ - 0.5f * U; }
            float kx = (hx + ftx) * 0.5f + fl * (0.3f + 0.45f * run) * U, ky = (hipY + fty) * 0.5f - 0.05f * U;
            // muslo (piel) y pantaloneta
            tube(cv, hx, hipY, kx, ky, 0.7f * U, 0.5f * U, skin, ow);
            float sx1 = hx + (kx - hx) * 0.55f, sy1 = hipY + (ky - hipY) * 0.55f;
            tube(cv, hx, hipY, sx1, sy1, 0.9f * U, 0.78f * U, shorts, ow);
            // media larga con franja
            tube(cv, kx, ky, ftx, fty, 0.52f * U, 0.36f * U, sock, ow);
            float px = kx + (ftx - kx) * 0.16f, py = ky + (fty - ky) * 0.16f, qx = kx + (ftx - kx) * 0.3f, qy = ky + (fty - ky) * 0.3f;
            band(cv, px, py, qx, qy, 0.5f * U, 0.47f * U, trim);
            // botín
            PA.reset();
            PA.moveTo(ftx - fl * 0.3f * U, fty - 0.15f * U); PA.lineTo(ftx + fl * 0.25f * U, fty - 0.3f * U);
            PA.lineTo(ftx + fl * 1.0f * U, fty + 0.02f * U); PA.lineTo(ftx + fl * 1.05f * U, fty + 0.3f * U);
            PA.lineTo(ftx - fl * 0.32f * U, fty + 0.3f * U); PA.close();
            fill(0xFFF4F6FA); cv.drawPath(PA, P);
            fill(shade(d.accent, 1.15f)); R.set(Math.min(ftx, ftx + fl * 0.7f * U), fty - 0.12f * U, Math.max(ftx, ftx + fl * 0.7f * U), fty + 0.04f * U); cv.drawOval(R, P);
            fill(0xFF2B2F3A); R.set(Math.min(ftx - fl * 0.32f * U, ftx + fl * 1.05f * U), fty + 0.22f * U, Math.max(ftx - fl * 0.32f * U, ftx + fl * 1.05f * U), fty + 0.32f * U); cv.drawRect(R, P);
            stroke(shade(0xFF8FA0B8, 0.7f), ow); cv.drawPath(PA, P);
        }

        // ---- pantaloneta (cintura)
        PA.reset();
        PA.moveTo(waistX - waistHalf * 1.05f, hipY - 0.2f * U); PA.lineTo(waistX + waistHalf * 1.05f, hipY - 0.2f * U);
        PA.lineTo(waistX + waistHalf * 1.2f, hipY + 0.75f * U); PA.lineTo(waistX - waistHalf * 1.2f, hipY + 0.75f * U); PA.close();
        fill(shorts); cv.drawPath(PA, P);
        float sideX = waistX - fl * waistHalf * 1.1f;
        line(cv, sideX, hipY - 0.1f * U, sideX - fl * 0.05f * U, hipY + 0.7f * U, 0.14f * U, trim);
        stroke(shade(shorts, 0.5f), ow); cv.drawPath(PA, P);
        fill(0x22FFFFFF); R.set(waistX - waistHalf, hipY - 0.2f * U, waistX + waistHalf, hipY + 0.05f * U); cv.drawRect(R, P);

        // ---- torso (camiseta)
        PA.reset();
        PA.moveTo(shX - halfW, shY); PA.lineTo(shX + halfW, shY);
        PA.lineTo(waistX + waistHalf, hipY - 0.15f * U); PA.lineTo(waistX - waistHalf, hipY - 0.15f * U); PA.close();
        fill(main); cv.drawPath(PA, P);
        cv.drawCircle(shX - halfW, shY + 0.28f * U, 0.3f * U, P); cv.drawCircle(shX + halfW, shY + 0.28f * U, 0.3f * U, P);
        // sombra cel del lado trasero y panel lateral
        float bx0 = fl >= 0 ? shX - halfW : shX + halfW * 0.25f;
        fill(0x33000000);
        PB.reset(); PB.moveTo(bx0, shY); PB.lineTo(bx0 + halfW * 0.75f, shY);
        PB.lineTo(bx0 + halfW * 0.75f * 0.72f + (waistX - shX), hipY - 0.15f * U); PB.lineTo(bx0 + (waistX - shX) * 1f + (fl >= 0 ? waistHalf * 0.0f : 0f) - (fl >= 0 ? 0 : 0), hipY - 0.15f * U); PB.close();
        cv.drawPath(PB, P);
        float sx = shX - fl * halfW * 0.88f, ex = waistX - fl * waistHalf * 0.88f;
        line(cv, sx, shY + 0.45f * U, ex, hipY - 0.2f * U, 0.22f * U, trim);
        fill(0x30FFFFFF); PB.reset(); PB.moveTo(shX - halfW * 0.5f, shY + 0.1f * U); PB.lineTo(shX + halfW * 0.1f, shY + 0.1f * U); PB.lineTo(shX - halfW * 0.1f, shY + 1.4f * U); PB.lineTo(shX - halfW * 0.55f, shY + 1.2f * U); PB.close(); cv.drawPath(PB, P);
        stroke(shade(main, 0.5f), ow); cv.drawPath(PA, P);
        // cuello en V y número
        PB.reset(); PB.moveTo(shX - 0.3f * U, shY - 0.02f * U); PB.lineTo(shX + 0.3f * U, shY - 0.02f * U); PB.lineTo(shX + fl * 0.05f * U, shY + 0.62f * U); PB.close();
        fill(skin); cv.drawPath(PB, P); stroke(trim, ow * 1.4f); cv.drawPath(PB, P);
        if (afl < 0.9f) text(cv, String.valueOf(pl.number), shX + fl * 0.05f * U, shY + 1.95f * U, 0.82f * U, trim, 0, 0, Paint.Align.CENTER, false);
        else text(cv, String.valueOf(pl.number), shX - fl * 0.1f * U, shY + 1.7f * U, 0.5f * U, trim, 0, 0, Paint.Align.CENTER, false);

        // ---- brazo cercano
        arm(cv, 1, fl, ph, sp, run, shX, shY, halfW, U, main, trim, skin, gk, pl, ow, jump);

        // ---- cuello y cabeza
        tube(cv, shX + fl * 0.1f * U, shY - 0.05f * U, headX, headY + 0.4f * U, 0.34f * U, 0.3f * U, shade(skin, 0.92f), ow);
        int emo = pl.cheer > 0 ? 2 : (pl.charge > 0.3f || pl.sprinting || pl.actionT > 0 ? 1 : 0);
        if (pl.stun > 0.18f) emo = 3;
        face(cv, d, pl.idx, headX, headY, 0.7f * U, fl, t, emo, sp);
        cv.restore();
    }

    static void arm(Canvas cv, int k, float fl, float ph, float sp, float run, float shX, float shY, float halfW,
                    float U, int main, int trim, int skin, boolean gk, Match.Player pl, float ow, float jump) {
        float phase = ph + (k == 0 ? (float) Math.PI : 0f);
        float amp = sp > 0.8f ? 0.35f + 0.65f * run : 0.05f;
        float sx0 = shX + fl * (k == 0 ? -1f : 1f) * halfW * 0.85f, sy0 = shY + 0.3f * U;
        float hx = sx0 + fl * (float) Math.sin(phase) * amp * 1.6f * U;
        float hy = sy0 + (2.5f - 0.9f * run * Math.max(0f, (float) Math.sin(phase + 1.2f))) * U;
        if (jump > 0.2f && gk) { hx = sx0 + fl * (k == 0 ? -0.5f : 0.7f) * U; hy = sy0 - 1.9f * U; }
        if (pl.cheer > 0) { hx = sx0 + fl * (k == 0 ? -0.6f : 0.7f) * U; hy = sy0 - 1.8f * U; }
        if (pl.charge > 0.3f) { hx = sx0 + fl * (k == 0 ? -1.0f : 0.4f) * U; hy = sy0 + 1.6f * U; }
        float ex = (sx0 + hx) * 0.5f + fl * (0.15f + 0.4f * run) * U, ey = (sy0 + hy) * 0.5f + 0.05f * U;
        tube(cv, sx0, sy0, ex, ey, 0.44f * U, 0.36f * U, skin, ow);
        float sx1 = sx0 + (ex - sx0) * 0.65f, sy1 = sy0 + (ey - sy0) * 0.65f;
        tube(cv, sx0, sy0, sx1, sy1, 0.6f * U, 0.52f * U, main, ow);
        band(cv, sx0 + (ex - sx0) * 0.55f, sy0 + (ey - sy0) * 0.55f, sx1, sy1, 0.54f * U, 0.52f * U, trim);
        tube(cv, ex, ey, hx, hy, 0.34f * U, 0.26f * U, skin, ow);
        oval(cv, hx, hy, gk ? 0.26f * U : 0.18f * U, gk ? 0.26f * U : 0.2f * U, gk ? 0xFFFFFFFF : skin, 0);
    }

    // ------------------------------------------------------------ balón
    static void ball(Canvas cv, float cx, float cy, float r, float rot) {
        oval(cv, cx, cy, r, r, 0xFFFFFFFF, Math.max(1.5f, r * 0.14f));
        // sombra cel (creciente inferior)
        PB.reset();
        for (int i = 0; i <= 8; i++) {
            float a = (float) Math.toRadians(10 + 160f * i / 8f);
            float px = cx + (float) Math.cos(a) * r * 0.96f, py = cy + (float) Math.sin(a) * r * 0.96f;
            if (i == 0) PB.moveTo(px, py); else PB.lineTo(px, py);
        }
        for (int i = 8; i >= 0; i--) {
            float a = (float) Math.toRadians(10 + 160f * i / 8f);
            float px = cx + (float) Math.cos(a) * r * 0.92f, py = cy - r * 0.32f + (float) Math.sin(a) * r * 0.78f;
            PB.lineTo(px, py);
        }
        PB.close();
        fill(0x44223355); cv.drawPath(PB, P);
        // parches
        fill(INK);
        for (int k = 0; k < 3; k++) {
            float a = rot + k * 2.0944f;
            float px = cx + (float) Math.cos(a) * r * 0.55f, py = cy + (float) Math.sin(a) * r * 0.55f;
            cv.drawCircle(px, py, r * 0.27f, P);
        }
        cv.drawCircle(cx + (float) Math.cos(rot + 1f) * r * 0.08f, cy + (float) Math.sin(rot + 1f) * r * 0.08f, r * 0.24f, P);
        fill(0xFFFFFFFF);
        cv.drawCircle(cx - r * 0.35f, cy - r * 0.4f, r * 0.16f, P);
    }

    private Art() {}
}
