package com.animestriker;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

/** Menús estilo anime. Cada pantalla registra sus botones para detectar toques. */
final class Ui {
    static final int B_PLAY = 1, B_FRIENDLY = 2, B_CUP = 3, B_SETTINGS = 4, B_BACK = 5, B_CONFIRM = 6,
            B_DIFF = 7, B_DUR = 8, B_SND = 9, B_MUS = 10, B_CONT = 11, B_RESUME = 12, B_QUIT = 13,
            B_RESTART = 14, B_TEAM0 = 20;

    final Paint P = Art.P;
    final Path PA = Art.PA;
    final RectF R = Art.R;
    final RectF[] rect = new RectF[40];
    int w, h;
    LinearGradient bg;

    Ui() { for (int i = 0; i < rect.length; i++) rect[i] = new RectF(); }

    void resize(int w, int h) {
        this.w = w; this.h = h;
        bg = new LinearGradient(0, 0, 0, h, new int[]{0xFF241B6B, 0xFFB5489B, 0xFFFF9A8B, 0xFFFFD39A},
                new float[]{0f, 0.5f, 0.82f, 1f}, Shader.TileMode.CLAMP);
    }

    void clear() { for (RectF r : rect) r.set(0, 0, 0, 0); }

    int hit(float x, float y) {
        for (int i = 1; i < rect.length; i++) if (rect[i].width() > 0 && rect[i].contains(x, y)) return i;
        return -1;
    }

    // ---------------------------------------------------------------- piezas
    void backdrop(Canvas c, float t) {
        P.setShader(bg); P.setStyle(Paint.Style.FILL); c.drawRect(0, 0, w, h, P); P.setShader(null);
        // rayos giratorios
        for (int i = 0; i < 18; i++) {
            float a = i * 6.2832f / 18f + t * 0.05f, wd = 0.1f;
            float cx = w * 0.5f, cy = h * 0.62f, far = w * 1.2f;
            PA.reset(); PA.moveTo(cx, cy);
            PA.lineTo(cx + (float) Math.cos(a - wd) * far, cy + (float) Math.sin(a - wd) * far);
            PA.lineTo(cx + (float) Math.cos(a + wd) * far, cy + (float) Math.sin(a + wd) * far);
            PA.close(); Art.fill(0x1FFFFFFF); c.drawPath(PA, P);
        }
        Art.fill(0x55FFE9B0); c.drawCircle(w * 0.5f, h * 0.62f, h * 0.35f, P);
        // pétalos de sakura
        for (int i = 0; i < 26; i++) {
            float sp = 40f + (i * 37 % 60), x = ((i * 211 % 1000) / 1000f * w + t * sp * 0.6f + (float) Math.sin(t + i) * 40f) % (w + 80f) - 40f;
            float y = (t * sp + i * 97f) % (h + 80f) - 40f, s = h * (0.012f + (i % 4) * 0.004f);
            c.save(); c.translate(x, y); c.rotate(t * 80f + i * 30f);
            Art.fill(0xCCFFC1DC); R.set(-s, -s * 0.55f, s, s * 0.55f); c.drawOval(R, P); c.restore();
        }
        // césped abajo
        Art.fill(0xFF2E9A4A); PA.reset(); PA.moveTo(0, h * 0.9f); PA.lineTo(w, h * 0.85f); PA.lineTo(w, h); PA.lineTo(0, h); PA.close(); c.drawPath(PA, P);
        Art.stroke(Art.INK, 6f); c.drawLine(0, h * 0.9f, w, h * 0.85f, P);
    }

    void button(Canvas c, int id, String label, float cx, float cy, float bw, float bh, int col, boolean sel) {
        rect[id].set(cx - bw / 2, cy - bh / 2, cx + bw / 2, cy + bh / 2);
        float sk = bh * 0.25f;
        PA.reset(); PA.moveTo(cx - bw / 2 + sk, cy - bh / 2); PA.lineTo(cx + bw / 2, cy - bh / 2); PA.lineTo(cx + bw / 2 - sk, cy + bh / 2); PA.lineTo(cx - bw / 2, cy + bh / 2); PA.close();
        Art.fill(Art.shade(col, 0.55f)); c.save(); c.translate(0, bh * 0.07f); c.drawPath(PA, P); c.restore();
        Art.fill(col); c.drawPath(PA, P);
        Art.fill(0x40FFFFFF); PA.reset(); PA.moveTo(cx - bw / 2 + sk, cy - bh / 2); PA.lineTo(cx + bw / 2, cy - bh / 2); PA.lineTo(cx + bw / 2 - sk * 0.5f, cy); PA.lineTo(cx - bw / 2 + sk * 0.5f, cy); PA.close(); c.drawPath(PA, P);
        PA.reset(); PA.moveTo(cx - bw / 2 + sk, cy - bh / 2); PA.lineTo(cx + bw / 2, cy - bh / 2); PA.lineTo(cx + bw / 2 - sk, cy + bh / 2); PA.lineTo(cx - bw / 2, cy + bh / 2); PA.close();
        Art.stroke(sel ? 0xFFFFFFFF : Art.INK, bh * 0.07f); c.drawPath(PA, P);
        Art.text(c, label, cx, cy + bh * 0.17f, bh * 0.46f, 0xFFFFFFFF, Art.INK, bh * 0.1f, Paint.Align.CENTER, true);
    }

    void title(Canvas c, String s, float cx, float cy, float size, float t) {
        float k = 1f + 0.02f * (float) Math.sin(t * 3f);
        c.save(); c.translate(cx, cy); c.rotate(-4f); c.scale(k, k);
        Art.text(c, s, size * 0.04f, size * 0.07f, size, 0xFF6A1B9A, Art.INK, size * 0.2f, Paint.Align.CENTER, true);
        Art.text(c, s, 0, 0, size, 0xFFFFFFFF, Art.INK, size * 0.12f, Paint.Align.CENTER, true);
        c.restore();
    }

    // ---------------------------------------------------------------- pantallas
    void drawTitle(Canvas c, float t) {
        clear(); backdrop(c, t);
        for (int i = 0; i < 6; i++) {
            Teams.Def d = Teams.ALL[i];
            float S = h * 0.075f, x = w * (0.1f + i * 0.16f), y = h * 0.8f + (float) Math.sin(t * 3f + i) * h * 0.012f;
            Art.fill(Art.alpha(d.main, 230)); c.drawCircle(x, y, S * 1.5f, P);
            Art.stroke(Art.INK, S * 0.1f); c.drawCircle(x, y, S * 1.5f, P);
            Art.face(c, d, 5, x, y, S, i % 2 == 0 ? 1f : -1f, t, i % 3 == 0 ? 2 : 1, 0f);
        }
        Art.text(c, "ANIME", w * 0.5f, h * 0.2f, h * 0.1f, 0xFFFFE066, Art.INK, h * 0.022f, Paint.Align.CENTER, true);
        title(c, "STRIKER", w * 0.5f, h * 0.43f, h * 0.24f, t);
        Art.text(c, "¡FÚTBOL A TODA VELOCIDAD!", w * 0.5f, h * 0.55f, h * 0.045f, 0xFFFFFFFF, Art.INK, h * 0.01f, Paint.Align.CENTER, true);
        if (t % 1.2f < 0.8f) Art.text(c, "TOCA PARA JUGAR", w * 0.5f, h * 0.68f, h * 0.06f, 0xFFFFFFFF, Art.INK, h * 0.013f, Paint.Align.CENTER, true);
        rect[B_PLAY].set(0, 0, w, h);
    }

    void drawMenu(Canvas c, float t) {
        clear(); backdrop(c, t);
        title(c, "MODO DE JUEGO", w * 0.5f, h * 0.17f, h * 0.1f, t);
        float bw = w * 0.5f, bh = h * 0.15f;
        button(c, B_FRIENDLY, "AMISTOSO", w * 0.5f, h * 0.36f, bw, bh, 0xFFFF4D8D, false);
        button(c, B_CUP, "COPA ANIME", w * 0.5f, h * 0.56f, bw, bh, 0xFFFFA000, false);
        button(c, B_SETTINGS, "AJUSTES", w * 0.5f, h * 0.76f, bw, bh, 0xFF3D8BFF, false);
        button(c, B_BACK, "‹", w * 0.07f, h * 0.1f, h * 0.14f, h * 0.12f, 0xFF7C4DFF, false);
    }

    void drawTeams(Canvas c, float t, String head, int sel, int disabled) {
        clear(); backdrop(c, t);
        title(c, head, w * 0.5f, h * 0.13f, h * 0.085f, t);
        float cw = w * 0.145f, gap = w * 0.012f, x0 = (w - (cw * 6 + gap * 5)) / 2f, y0 = h * 0.22f, ch = h * 0.56f;
        for (int i = 0; i < 6; i++) {
            Teams.Def d = Teams.ALL[i];
            float x = x0 + i * (cw + gap);
            boolean off = i == disabled, s = i == sel;
            float lift = s ? -h * 0.02f : 0f;
            rect[B_TEAM0 + i].set(x, y0 + lift, x + cw, y0 + ch + lift);
            R.set(x, y0 + lift, x + cw, y0 + ch + lift);
            Art.fill(off ? 0xFF4A4560 : Art.shade(d.main, 0.5f)); c.drawRoundRect(R, 26, 26, P);
            R.set(x + 6, y0 + lift + 6, x + cw - 6, y0 + lift + ch * 0.46f);
            Art.fill(off ? 0xFF6B6480 : d.main); c.drawRoundRect(R, 22, 22, P);
            Art.fill(0x33FFFFFF); R.set(x + 6, y0 + lift + 6, x + cw - 6, y0 + lift + ch * 0.2f); c.drawRoundRect(R, 22, 22, P);
            R.set(x, y0 + lift, x + cw, y0 + ch + lift);
            Art.stroke(s ? 0xFFFFEB3B : Art.INK, s ? 10f : 6f); c.drawRoundRect(R, 26, 26, P);
            float S = cw * 0.25f, fy = y0 + lift + ch * 0.27f;
            Art.face(c, d, 5, x + cw * 0.5f, fy, S, 1f, t + i, s ? 2 : 1, 0f);
            Art.text(c, d.name, x + cw * 0.5f, y0 + lift + ch * 0.55f, cw * 0.115f, 0xFFFFFFFF, Art.INK, cw * 0.025f, Paint.Align.CENTER, true);
            String stars = "";
            for (int k = 0; k < 5; k++) stars += k < d.stars ? "★" : "☆";
            Art.text(c, stars, x + cw * 0.5f, y0 + lift + ch * 0.63f, cw * 0.13f, 0xFFFFE066, Art.INK, cw * 0.02f, Paint.Align.CENTER, true);
            String[] lab = {"VEL", "TIRO", "PASE", "DEF", "POR"};
            float[] v = {d.spd, d.sht, d.pas, d.def, d.gk};
            for (int k = 0; k < 5; k++) {
                float by = y0 + lift + ch * (0.68f + k * 0.056f), bw = cw * 0.55f, bh = ch * 0.032f;
                Art.text(c, lab[k], x + cw * 0.1f, by + bh, bh * 1.35f, 0xFFFFFFFF, Art.INK, bh * 0.25f, Paint.Align.LEFT, true);
                R.set(x + cw * 0.36f, by, x + cw * 0.36f + bw, by + bh); Art.fill(0xAA120F25); c.drawRoundRect(R, bh / 2, bh / 2, P);
                float f = Math.max(0.05f, Math.min(1f, (v[k] - 0.88f) / 0.27f));
                R.set(x + cw * 0.36f, by, x + cw * 0.36f + bw * f, by + bh); Art.fill(0xFF76FF03); c.drawRoundRect(R, bh / 2, bh / 2, P);
            }
        }
        if (sel >= 0) {
            Teams.Def d = Teams.ALL[sel];
            Art.text(c, "Especial: " + d.special + "  ·  Capitán: " + d.captain(), w * 0.5f, h * 0.84f, h * 0.04f, 0xFFFFFFFF, Art.INK, h * 0.009f, Paint.Align.CENTER, true);
            button(c, B_CONFIRM, "CONFIRMAR", w * 0.5f, h * 0.93f, w * 0.3f, h * 0.1f, 0xFF43D17A, false);
        }
        button(c, B_BACK, "‹", w * 0.07f, h * 0.1f, h * 0.14f, h * 0.12f, 0xFF7C4DFF, false);
    }

    void drawSettings(Canvas c, float t, int diff, int dur, boolean snd, boolean mus) {
        clear(); backdrop(c, t);
        title(c, "AJUSTES", w * 0.5f, h * 0.15f, h * 0.1f, t);
        String[] dn = {"FÁCIL", "NORMAL", "DIFÍCIL"}, du = {"1:30 por tiempo", "2:00 por tiempo", "3:00 por tiempo"};
        float bw = w * 0.55f, bh = h * 0.12f;
        button(c, B_DIFF, "Dificultad: " + dn[diff], w * 0.5f, h * 0.35f, bw, bh, 0xFFFF4D8D, false);
        button(c, B_DUR, "Duración: " + du[dur], w * 0.5f, h * 0.51f, bw, bh, 0xFFFFA000, false);
        button(c, B_SND, "Efectos: " + (snd ? "SÍ" : "NO"), w * 0.5f, h * 0.67f, bw, bh, 0xFF3D8BFF, false);
        button(c, B_MUS, "Música: " + (mus ? "SÍ" : "NO"), w * 0.5f, h * 0.83f, bw, bh, 0xFF8E24AA, false);
        button(c, B_BACK, "‹", w * 0.07f, h * 0.1f, h * 0.14f, h * 0.12f, 0xFF7C4DFF, false);
    }

    void drawPause(Canvas c) {
        clear();
        Art.fill(0xC0100C24); c.drawRect(0, 0, w, h, P);
        Art.text(c, "PAUSA", w * 0.5f, h * 0.22f, h * 0.12f, 0xFFFFFFFF, Art.INK, h * 0.02f, Paint.Align.CENTER, true);
        float bw = w * 0.4f, bh = h * 0.13f;
        button(c, B_RESUME, "SEGUIR", w * 0.5f, h * 0.42f, bw, bh, 0xFF43D17A, false);
        button(c, B_RESTART, "REINICIAR", w * 0.5f, h * 0.60f, bw, bh, 0xFFFFA000, false);
        button(c, B_QUIT, "SALIR", w * 0.5f, h * 0.78f, bw, bh, 0xFFFF4D5E, false);
    }

    void drawResult(Canvas c, float t, Match m, String head, String sub, String btn) {
        clear(); backdrop(c, t);
        int me = m.humanTeam < 0 ? 0 : m.humanTeam;
        title(c, head, w * 0.5f, h * 0.2f, h * 0.13f, t);
        for (int i = 0; i < 2; i++) {
            Teams.Def d = m.def[i];
            float x = w * (i == 0 ? 0.25f : 0.75f), S = h * 0.1f;
            Art.fill(Art.alpha(d.main, 240)); c.drawCircle(x, h * 0.47f, S * 1.5f, P);
            Art.stroke(Art.INK, S * 0.1f); c.drawCircle(x, h * 0.47f, S * 1.5f, P);
            Art.face(c, d, 5, x, h * 0.47f, S, i == 0 ? 1f : -1f, t, m.score[i] >= m.score[1 - i] ? 2 : 0, 0f);
            Art.text(c, d.name, x, h * 0.71f, h * 0.055f, 0xFFFFFFFF, Art.INK, h * 0.012f, Paint.Align.CENTER, true);
        }
        Art.text(c, m.score[0] + " - " + m.score[1], w * 0.5f, h * 0.5f, h * 0.17f, 0xFFFFFFFF, Art.INK, h * 0.03f, Paint.Align.CENTER, true);
        if (m.shootout) Art.text(c, "Penales " + m.penA + " - " + m.penB, w * 0.5f, h * 0.6f, h * 0.05f, 0xFFFFE066, Art.INK, h * 0.011f, Paint.Align.CENTER, true);
        Art.text(c, sub, w * 0.5f, h * 0.77f, h * 0.045f, 0xFFFFFFFF, Art.INK, h * 0.01f, Paint.Align.CENTER, true);
        Art.text(c, "Tiros " + m.shots[0] + " - " + m.shots[1], w * 0.5f, h * 0.68f, h * 0.04f, 0xFFFFFFFF, Art.INK, h * 0.009f, Paint.Align.CENTER, true);
        button(c, B_CONT, btn, w * 0.5f, h * 0.9f, w * 0.34f, h * 0.11f, 0xFF43D17A, false);
    }

    /** Cuadro de la copa: team[0..3]; semis (0-1 usuario) y final. */
    void drawCup(Canvas c, float t, int[] cupTeams, int round, boolean[] semiWin, String info) {
        clear(); backdrop(c, t);
        title(c, "COPA ANIME", w * 0.5f, h * 0.14f, h * 0.09f, t);
        float cw = w * 0.2f, ch = h * 0.1f;
        float[] ys = {h * 0.33f, h * 0.47f, h * 0.65f, h * 0.79f};
        for (int i = 0; i < 4; i++) {
            Teams.Def d = Teams.ALL[cupTeams[i]];
            float x = w * 0.14f; boolean alive = round == 0 || (i < 2 ? semiWin[0] == (i == 0) : semiWin[1] == (i == 2));
            R.set(x, ys[i] - ch / 2, x + cw, ys[i] + ch / 2);
            Art.fill(alive ? d.main : 0xFF5A5470); c.drawRoundRect(R, 18, 18, P);
            Art.stroke(i == 0 ? 0xFFFFEB3B : Art.INK, i == 0 ? 8f : 5f); c.drawRoundRect(R, 18, 18, P);
            Art.text(c, d.name, x + cw * 0.5f, ys[i] + ch * 0.17f, ch * 0.4f, 0xFFFFFFFF, Art.INK, ch * 0.08f, Paint.Align.CENTER, true);
        }
        // final
        Art.text(c, "SEMIS", w * 0.24f, h * 0.24f, h * 0.04f, 0xFFFFE066, Art.INK, h * 0.009f, Paint.Align.CENTER, true);
        Art.text(c, "FINAL", w * 0.62f, h * 0.24f, h * 0.04f, 0xFFFFE066, Art.INK, h * 0.009f, Paint.Align.CENTER, true);
        for (int i = 0; i < 2; i++) {
            float y = i == 0 ? (ys[0] + ys[1]) / 2 : (ys[2] + ys[3]) / 2;
            Art.line(c, w * 0.14f + cw, ys[i * 2], w * 0.46f, y, 6f, 0xFFFFFFFF);
            Art.line(c, w * 0.14f + cw, ys[i * 2 + 1], w * 0.46f, y, 6f, 0xFFFFFFFF);
            Art.line(c, w * 0.46f, y, w * 0.56f, (ys[1] + ys[2]) / 2, 6f, 0xFFFFFFFF);
        }
        trophy(c, w * 0.78f, h * 0.55f, h * 0.2f, t);
        Art.text(c, info, w * 0.5f, h * 0.9f, h * 0.045f, 0xFFFFFFFF, Art.INK, h * 0.01f, Paint.Align.CENTER, true);
        button(c, B_CONT, "JUGAR", w * 0.78f, h * 0.9f, w * 0.26f, h * 0.1f, 0xFF43D17A, false);
        button(c, B_BACK, "‹", w * 0.07f, h * 0.1f, h * 0.14f, h * 0.12f, 0xFF7C4DFF, false);
    }

    void trophy(Canvas c, float x, float y, float s, float t) {
        float k = 1f + 0.03f * (float) Math.sin(t * 4f);
        c.save(); c.translate(x, y); c.scale(k, k);
        Art.fill(0x66FFF59D); c.drawCircle(0, -s * 0.1f, s * 0.9f, P);
        Art.fill(0xFFFFC107);
        PA.reset(); PA.moveTo(-s * 0.45f, -s * 0.7f); PA.lineTo(s * 0.45f, -s * 0.7f); PA.quadTo(s * 0.4f, s * 0.05f, 0, s * 0.15f); PA.quadTo(-s * 0.4f, s * 0.05f, -s * 0.45f, -s * 0.7f); PA.close();
        c.drawPath(PA, P); Art.stroke(Art.INK, s * 0.04f); c.drawPath(PA, P);
        Art.stroke(0xFFFFC107, s * 0.07f);
        R.set(-s * 0.7f, -s * 0.62f, -s * 0.3f, -s * 0.1f); c.drawOval(R, P); R.set(s * 0.3f, -s * 0.62f, s * 0.7f, -s * 0.1f); c.drawOval(R, P);
        Art.fill(0xFFFFC107); R.set(-s * 0.08f, s * 0.12f, s * 0.08f, s * 0.42f); c.drawRect(R, P);
        Art.fill(0xFF7B1FA2); R.set(-s * 0.32f, s * 0.42f, s * 0.32f, s * 0.58f); c.drawRoundRect(R, 8, 8, P);
        Art.stroke(Art.INK, s * 0.03f); c.drawRoundRect(R, 8, 8, P);
        Art.fill(0x88FFFFFF); R.set(-s * 0.3f, -s * 0.62f, -s * 0.15f, -s * 0.1f); c.drawOval(R, P);
        c.restore();
    }
}
