package com.animestriker;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;

/** Dibuja el partido: estadio, campo, personajes, efectos, HUD, cortes de cámara y controles. */
public final class Renderer {

    final Paint P = Art.P;
    final Path PA = Art.PA, PB = Art.PB;
    final RectF R = Art.R;
    static final int INK = Art.INK;

    int w, h;
    float ppm, topY, botY, camX = 50f, shake, shakeT, time;
    LinearGradient skyG, vigG, grassG;
    Bitmap bg, grass;
    Canvas bgC;
    int bgw, bgh;
    final Rect srcR = new Rect();
    final RectF dstR = new RectF();
    final Paint bmpP = new Paint(Paint.ANTI_ALIAS_FLAG);

    // multitud
    static final int ROWS = 6, COLS = 90;
    final int[] crowd = new int[ROWS * COLS];

    // partículas
    static final int NP = 380;
    final float[] qx = new float[NP], qy = new float[NP], qz = new float[NP], qvx = new float[NP],
            qvy = new float[NP], qvz = new float[NP], ql = new float[NP], qm = new float[NP],
            qs = new float[NP], qr = new float[NP], qvr = new float[NP];
    final int[] qt = new int[NP], qc = new int[NP];
    final boolean[] qw = new boolean[NP];
    int qhead;
    float ballRot;

    final Match.Player[] ents = new Match.Player[12];
    final float[] keys = new float[13];
    final int[] order = new int[13];

    static final String[] ADS = {"SAKURA COLA", "RAMEN-GO", "KAWAII BANK", "TURBO SHOES", "NEKO MART", "HYPER PHONE", "MANGA PLUS"};
    static final int[] ADC = {0xFFD81B60, 0xFFFF8F00, 0xFF00897B, 0xFF3949AB, 0xFF6D4C41, 0xFF00ACC1, 0xFF7B1FA2};

    public Renderer() {
        Art.init();
        java.util.Random r = new java.util.Random(11);
        int[] pal = {0xFFE53935, 0xFF1E88E5, 0xFFFFC107, 0xFFEC407A, 0xFF66BB6A, 0xFFAB47BC, 0xFFFFFFFF, 0xFFFF7043, 0xFF26C6DA};
        for (int i = 0; i < crowd.length; i++) crowd[i] = Art.shade(pal[r.nextInt(pal.length)], 0.6f + r.nextFloat() * 0.5f);
    }

    public void resize(int w, int h) {
        this.w = w; this.h = h;
        ppm = w / 44f; topY = h * 0.30f; botY = h * 0.935f;
        skyG = new LinearGradient(0, 0, 0, topY * 0.75f, new int[]{0xFF3F8FDD, 0xFF7DBDEE, 0xFFD3ECFA},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP);
        vigG = new LinearGradient(0, topY, 0, botY, new int[]{0x30102040, 0x00000000, 0x28001800},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);
        grassG = new LinearGradient(0, topY, 0, botY, new int[]{0xFF3C8F35, 0xFF4BA43D, 0xFF5CB847},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);
        bgw = Math.max(64, w / 7); bgh = Math.max(32, (int) (topY / 7f));
        bg = Bitmap.createBitmap(bgw, bgh, Bitmap.Config.ARGB_8888);
        bgC = new Canvas(bg);
        bmpP.setFilterBitmap(true);
        buildGrass();
    }

    void buildGrass() {
        grass = Bitmap.createBitmap(512, 128, Bitmap.Config.ARGB_8888);
        Canvas gc = new Canvas(grass);
        java.util.Random r = new java.util.Random(21);
        for (int i = 0; i < 3600; i++) {
            float x = r.nextFloat() * 512f, y = r.nextFloat() * 128f, len = 3f + r.nextFloat() * 9f, lean = (r.nextFloat() - 0.5f) * 4f;
            boolean light = r.nextFloat() < 0.5f;
            int a = 40 + r.nextInt(60);
            Art.stroke(light ? Art.alpha(0xFFB6E86A, a) : Art.alpha(0xFF14501A, a), 1.0f + r.nextFloat() * 0.8f);
            gc.drawLine(x, y, x + lean, y - len, P);
        }
    }

    // ------------------------------------------------------------------ proyección
    float sc(float y) { return 0.68f + 0.32f * (y / Match.W); }
    float sy(float y) { return topY + (y / Match.W) * (botY - topY); }
    float sx(float x, float y) { return w * 0.5f + (x - camX) * ppm * sc(y); }
    float syz(float y, float z) { return sy(y) - z * ppm * sc(y); }
    float lw(float y) { return Math.max(2f, ppm * sc(y) * 0.24f); }

    // ------------------------------------------------------------------ partículas
    void spawn(int type, boolean world, float x, float y, float z, float vx, float vy, float vz, float life, int col, float size) {
        int i = qhead; qhead = (qhead + 1) % NP;
        qt[i] = type; qw[i] = world; qx[i] = x; qy[i] = y; qz[i] = z; qvx[i] = vx; qvy[i] = vy; qvz[i] = vz;
        ql[i] = life; qm[i] = life; qc[i] = col; qs[i] = size; qr[i] = (float) Math.random() * 6.28f; qvr[i] = ((float) Math.random() - 0.5f) * 14f;
    }

    void confetti(Match m, int team) {
        Teams.Def d = m.def[team];
        int[] cols = {d.main, d.second, 0xFFFFE066, 0xFFFFFFFF, d.accent == 0xFF4A0E0E ? 0xFFFF8A65 : d.accent};
        for (int i = 0; i < 90; i++) {
            spawn(2, false, (float) Math.random() * w, -(float) Math.random() * h * 0.4f, 0,
                    ((float) Math.random() - 0.5f) * 120f, 220f + (float) Math.random() * 260f, 0, 3.8f,
                    cols[i % cols.length], h * (0.012f + (float) Math.random() * 0.012f));
        }
        for (int i = 0; i < 40; i++) {
            spawn(3, false, (float) Math.random() * w, -(float) Math.random() * h * 0.5f, 0,
                    -60f - (float) Math.random() * 120f, 120f + (float) Math.random() * 160f, 0, 4.2f,
                    0xFFFFB7D5, h * (0.014f + (float) Math.random() * 0.012f));
        }
    }

    public void event(int type, Match m, Match.Player p) {
        switch (type) {
            case Match.EV_KICK: case Match.EV_PASS: case Match.EV_SHOT:
                for (int i = 0; i < 5; i++)
                    spawn(0, true, m.bx, m.by, 0.1f, ((float) Math.random() - 0.5f) * 3f, ((float) Math.random() - 0.5f) * 2f, 0.8f, 0.5f, 0xFFE8F5D0, 0.35f);
                if (type == Match.EV_SHOT) spawn(5, true, m.bx, m.by, 0.4f, 0, 0, 0, 0.28f, 0xFFFFFFFF, 1.6f);
                break;
            case Match.EV_TACKLE:
                for (int i = 0; i < 8; i++)
                    spawn(1, true, m.bx, m.by, 0.6f, ((float) Math.random() - 0.5f) * 14f, ((float) Math.random() - 0.5f) * 6f, 3f + (float) Math.random() * 5f, 0.4f, 0xFFFFF59D, 0.16f);
                spawn(4, true, m.bx, m.by, 1.2f, 0, 0, 0, 0.4f, 0xFFFFEB3B, 2.2f);
                shake = Math.max(shake, 4f); shakeT = 0.2f;
                break;
            case Match.EV_SAVE:
                spawn(4, true, m.bx, m.by, 1.5f, 0, 0, 0, 0.5f, 0xFFFFFFFF, 2.6f);
                for (int i = 0; i < 6; i++)
                    spawn(1, true, m.bx, m.by, 1.5f, ((float) Math.random() - 0.5f) * 10f, ((float) Math.random() - 0.5f) * 8f, 2f + (float) Math.random() * 4f, 0.5f, 0xFF80D8FF, 0.16f);
                shake = Math.max(shake, 6f); shakeT = 0.25f;
                break;
            case Match.EV_POST:
                for (int i = 0; i < 10; i++)
                    spawn(1, true, m.bx, m.by, 1.5f, ((float) Math.random() - 0.5f) * 12f, ((float) Math.random() - 0.5f) * 12f, 1f + (float) Math.random() * 5f, 0.45f, 0xFFFFFFFF, 0.14f);
                shake = Math.max(shake, 8f); shakeT = 0.3f;
                break;
            case Match.EV_SPECIAL_GO:
                spawn(5, true, m.bx, m.by, 0.8f, 0, 0, 0, 0.5f, 0xFFFFD54F, 3.5f);
                for (int i = 0; i < 14; i++)
                    spawn(1, true, m.bx, m.by, 0.8f, ((float) Math.random() - 0.5f) * 18f, ((float) Math.random() - 0.5f) * 8f, (float) Math.random() * 6f, 0.5f, 0xFFFF9800, 0.2f);
                shake = Math.max(shake, 12f); shakeT = 0.4f;
                break;
            case Match.EV_GOAL:
                confetti(m, m.lastScoringTeam);
                shake = 14f; shakeT = 0.5f;
                break;
            default:
        }
    }

    void tickParticles(float dt) {
        for (int i = 0; i < NP; i++) {
            if (ql[i] <= 0) continue;
            ql[i] -= dt;
            qx[i] += qvx[i] * dt; qy[i] += qvy[i] * dt; qr[i] += qvr[i] * dt;
            if (qw[i]) { qz[i] += qvz[i] * dt; if (qt[i] == 1) qvz[i] -= 18f * dt; if (qz[i] < 0) { qz[i] = 0; qvz[i] *= -0.4f; } }
            else if (qt[i] == 3) qvx[i] = -60f + (float) Math.sin(time * 3f + i) * 90f;
        }
    }

    void drawParticles(Canvas c) {
        for (int i = 0; i < NP; i++) {
            if (ql[i] <= 0) continue;
            float k = ql[i] / qm[i];
            int a = (int) (255 * Math.min(1f, k * 2f));
            float x, y, s;
            if (qw[i]) {
                x = sx(qx[i], qy[i]); y = syz(qy[i], qz[i]); s = ppm * sc(qy[i]) * qs[i];
            } else { x = qx[i]; y = qy[i]; s = qs[i]; }
            switch (qt[i]) {
                case 0: Art.fill(Art.alpha(qc[i], a / 3)); c.drawCircle(x, y, s * (1.6f - k), P); break;
                case 1: Art.fill(Art.alpha(qc[i], a)); c.drawCircle(x, y, Math.max(2f, s * 0.5f), P); break;
                case 2: case 3:
                    c.save(); c.translate(x, y); c.rotate(qr[i] * 57.3f);
                    Art.fill(Art.alpha(qc[i], a));
                    if (qt[i] == 2) { R.set(-s, -s * 0.5f, s, s * 0.5f); c.drawRect(R, P); }
                    else { R.set(-s, -s * 0.55f, s, s * 0.55f); c.drawOval(R, P); }
                    c.restore(); break;
                case 4: {
                    float r1 = s * (0.5f + (1f - k) * 1.2f), r2 = r1 * 0.28f;
                    PA.reset();
                    for (int j = 0; j < 8; j++) {
                        float ang = j * 0.7854f + qr[i] * 0.1f, rr = (j % 2 == 0) ? r1 : r2;
                        float px = x + (float) Math.cos(ang) * rr, py = y + (float) Math.sin(ang) * rr;
                        if (j == 0) PA.moveTo(px, py); else PA.lineTo(px, py);
                    }
                    PA.close();
                    Art.fill(Art.alpha(qc[i], a)); c.drawPath(PA, P);
                    Art.stroke(Art.alpha(INK, a), 3f); c.drawPath(PA, P);
                    break;
                }
                case 5: {
                    float rr = s * (1f - k) * 1.2f + s * 0.2f;
                    Art.stroke(Art.alpha(qc[i], a), Math.max(2f, s * 0.12f * k));
                    R.set(x - rr, y - rr * 0.45f, x + rr, y + rr * 0.45f); c.drawOval(R, P); break;
                }
                default:
            }
        }
    }

    // ------------------------------------------------------------------ escena
    public void drawMatch(Canvas c, Match m, float dt, Pad pad) {
        time += dt;
        tickParticles(dt);
        // cámara
        float target = m.bx + (m.owner != null ? m.owner.fx * 4f : Math.max(-8f, Math.min(8f, m.bvx * 0.25f)));
        if (m.state == Match.ST_KICKOFF || m.state == Match.ST_RESTART) target = m.bx;
        camX += (target - camX) * Math.min(1f, dt * 3.6f);
        camX = Math.max(15f, Math.min(85f, camX));
        float sx0 = 0, sy0 = 0;
        if (shakeT > 0) { shakeT -= dt; shake *= 0.9f; sx0 = ((float) Math.random() - 0.5f) * shake; sy0 = ((float) Math.random() - 0.5f) * shake; }
        else shake = 0;
        c.save();
        c.translate(sx0, sy0);
        background(c, m);
        pitch(c, m);
        goal(c, 0); goal(c, 1);
        entities(c, m);
        drawParticles(c);
        c.restore();
        // viñeta de especial
        if (m.ballSpecial) {
            Art.fill(Art.alpha(0xFFFF8A00, 18 + (int) (14 * Math.sin(time * 30f)))); c.drawRect(0, 0, w, h, P);
        }
        hud(c, m);
        if (m.state == Match.ST_CUTIN) cutin(c, m);
        else if (m.state == Match.ST_GOAL) goalOverlay(c, m);
        else banner(c, m);
        if (pad != null) pad(c, pad, m);
    }

    // ------------------------------------------------------------------ fondo
    void backdropScene(Canvas c, Match m) {
        // cielo
        P.setShader(skyG); P.setStyle(Paint.Style.FILL); c.drawRect(0, 0, w, topY, P); P.setShader(null);
        // nubes grandes y suaves
        for (int i = 0; i < 5; i++) {
            float cx = ((i * 0.27f * w + time * 5f * (1 + i * 0.2f) - camX * ppm * 0.06f) % (w + 700f)) - 350f;
            float cy = h * (0.03f + 0.025f * (i % 3)), s = h * (0.05f + 0.01f * i);
            Art.fill(0x66A9C9EA); R.set(cx - s * 0.5f, cy + s * 0.2f, cx + s * 4.2f, cy + s * 1.15f); c.drawOval(R, P);
            Art.fill(0xFFFFFFFF);
            c.drawCircle(cx, cy + s * 0.3f, s * 0.8f, P); c.drawCircle(cx + s * 1.1f, cy, s * 1.1f, P);
            c.drawCircle(cx + s * 2.2f, cy + s * 0.15f, s, P); c.drawCircle(cx + s * 3.2f, cy + s * 0.35f, s * 0.75f, P);
            R.set(cx - s * 0.3f, cy + s * 0.3f, cx + s * 3.7f, cy + s * 1.0f); c.drawOval(R, P);
        }
        float standTop = h * 0.07f, boardH = 1.15f * ppm * sc(0), boardTop = topY - boardH;
        // estructura de la grada
        Art.fill(0xFF6C7587); c.drawRect(0, standTop, w, boardTop, P);
        Art.fill(0xFF858EA0); c.drawRect(0, standTop, w, standTop + h * 0.012f, P);
        // torre de luces
        float tx = w * 0.56f - camX * ppm * 0.05f;
        Art.fill(0xFF8D95A5); R.set(tx - h * 0.006f, 0, tx + h * 0.006f, standTop + h * 0.02f); c.drawRect(R, P);
        Art.fill(0xFFB8BFCC); R.set(tx - h * 0.06f, 0, tx + h * 0.06f, h * 0.075f); c.drawRect(R, P);
        Art.fill(0x66FFFFFF); c.drawCircle(tx, h * 0.04f, h * 0.11f, P);
        Art.fill(0xFFFFFFFF);
        for (int i = 0; i < 4; i++) for (int j = 0; j < 3; j++) c.drawCircle(tx - h * 0.045f + i * h * 0.03f, h * 0.014f + j * h * 0.02f, h * 0.0085f, P);
        // pantalla gigante
        float vx = w * 0.04f - camX * ppm * 0.03f;
        Art.fill(0xFF22262F); R.set(vx, h * 0.02f, vx + h * 0.4f, h * 0.17f); c.drawRect(R, P);
        Art.fill(0xFF3D9F45); R.set(vx + h * 0.015f, h * 0.035f, vx + h * 0.19f, h * 0.155f); c.drawRect(R, P);
        Art.fill(0xFF5FB0E8); R.set(vx + h * 0.21f, h * 0.035f, vx + h * 0.385f, h * 0.155f); c.drawRect(R, P);
        Art.fill(0xFFFFFFFF); c.drawCircle(vx + h * 0.1f, h * 0.1f, h * 0.012f, P);
        // techo de la tribuna (derecha)
        PA.reset(); PA.moveTo(w * 0.8f, 0); PA.lineTo(w, 0); PA.lineTo(w, h * 0.16f); PA.lineTo(w * 0.84f, h * 0.11f); PA.close();
        Art.fill(0xFF50586A); c.drawPath(PA, P);
        Art.fill(0x55000000); PA.reset(); PA.moveTo(w * 0.84f, h * 0.11f); PA.lineTo(w, h * 0.16f); PA.lineTo(w, h * 0.2f); PA.lineTo(w * 0.86f, h * 0.15f); PA.close(); c.drawPath(PA, P);
        // público
        int rows = 9;
        float rowH = (boardTop - standTop - h * 0.012f) / rows, rad = rowH * 0.5f, spacing = w / 62f;
        boolean party = m.state == Match.ST_GOAL;
        float off = (-camX * ppm * 0.2f) % spacing;
        for (int r = 0; r < rows; r++) {
            float yy = standTop + h * 0.012f + rowH * (r + 0.7f);
            Art.fill(Art.alpha(0xFF000000, 30 + r * 4)); c.drawRect(0, yy + rowH * 0.2f, w, yy + rowH * 0.5f, P);
            for (int i = -1; i < 64; i++) {
                float x = i * spacing + off + (r % 2) * spacing * 0.5f;
                int col = crowd[(r * COLS + (i + 7)) % crowd.length];
                float j = party ? (float) Math.abs(Math.sin(time * 8f + i * 0.9f + r)) * rowH * 0.6f : (float) Math.sin(time * 1.5f + i * 1.3f + r) * rowH * 0.05f;
                Art.fill(Art.shade(col, 0.9f)); R.set(x - rad * 1.1f, yy - j, x + rad * 1.1f, yy + rowH * 0.45f); c.drawOval(R, P);
                Art.fill(0xFFE2B596); c.drawCircle(x, yy - rad * 0.55f - j, rad * 0.55f, P);
                if ((i + r) % 3 == 0) { Art.fill(0xFF2A2430); R.set(x - rad * 0.55f, yy - rad * 1.1f - j, x + rad * 0.55f, yy - rad * 0.55f - j); c.drawOval(R, P); }
            }
        }
    }

    void background(Canvas c, Match m) {
        // la escena lejana se dibuja a 1/4 de resolución y se reescala: efecto de profundidad de campo
        float k = bgw / (float) w;
        bgC.save(); bgC.scale(k, k);
        Canvas keep = null;
        Canvas real = c;
        backdropScene(bgC, m);
        bgC.restore();
        srcR.set(0, 0, bgw, bgh); dstR.set(0, 0, w, topY);
        c.drawBitmap(bg, srcR, dstR, bmpP);
        Art.fill(0x14FFFFFF); c.drawRect(0, 0, w, topY, P);
        // vallas publicitarias nítidas
        float boardH = 1.15f * ppm * sc(0), boardTop = topY - boardH;
        float bw = 9f * ppm * sc(0);
        int first = (int) Math.floor((camX - 45f) / 9f);
        for (int i = first; i < first + 12; i++) {
            float x0 = sx(i * 9f, 0), x1 = x0 + bw;
            int kk = ((i % 7) + 7) % 7;
            Art.fill(ADC[kk]); R.set(x0, boardTop, x1, topY); c.drawRect(R, P);
            Art.fill(0x22FFFFFF); R.set(x0, boardTop, x1, boardTop + boardH * 0.3f); c.drawRect(R, P);
            Art.fill(0x33000000); R.set(x0, topY - boardH * 0.12f, x1, topY); c.drawRect(R, P);
            Art.text(c, ADS[kk], (x0 + x1) * 0.5f, boardTop + boardH * 0.7f, boardH * 0.4f, 0xFFFFFFFF, 0, 0, Paint.Align.CENTER, true);
            Art.line(c, x1, boardTop, x1, topY, 2f, 0x55FFFFFF);
        }
    }

    // ------------------------------------------------------------------ campo
    void seg(Canvas c, float x0, float y0, float x1, float y1, int col) {
        float ww = (lw(y0) + lw(y1)) * 0.5f;
        Art.line(c, sx(x0, y0), sy(y0), sx(x1, y1), sy(y1), ww * 1.9f, 0x22FFFFFF);
        Art.line(c, sx(x0, y0), sy(y0), sx(x1, y1), sy(y1), ww, col);
    }

    void pitch(Canvas c, Match m) {
        P.setShader(grassG); P.setStyle(Paint.Style.FILL); c.drawRect(0, topY, w, h, P); P.setShader(null);
        for (int i = 0; i < 20; i++) {
            float x0 = i * 5f, x1 = x0 + 5f;
            float a = sx(x0, 0), b = sx(x1, 0), a2 = sx(x0, Match.W), b2 = sx(x1, Match.W);
            if (Math.max(b, b2) < -20 || Math.min(a, a2) > w + 20) continue;
            PA.reset(); PA.moveTo(a, sy(0)); PA.lineTo(b, sy(0)); PA.lineTo(b2, sy(Match.W)); PA.lineTo(a2, sy(Match.W)); PA.close();
            Art.fill(i % 2 == 0 ? 0x26D8FF9A : 0x1A0A3A10); c.drawPath(PA, P);
        }
        // textura de hierba en 4 bandas (perspectiva aproximada)
        float[] yb = {0f, 11f, 25f, 41f, 60f};
        bmpP.setAlpha(150);
        for (int bnd = 0; bnd < 4; bnd++) {
            float ym = (yb[bnd] + yb[bnd + 1]) * 0.5f, tw = 360f * sc(ym) * (w / 2400f) * 1.6f;
            float top = sy(yb[bnd]), bot = sy(yb[bnd + 1]);
            float scroll = (-camX * ppm * sc(ym)) % tw;
            if (scroll > 0) scroll -= tw;
            for (float x = scroll; x < w; x += tw) { dstR.set(x, top, x + tw, bot); srcR.set(0, 0, 512, 128); c.drawBitmap(grass, srcR, dstR, bmpP); }
        }
        bmpP.setAlpha(255);
        // luz del sol desde la izquierda
        Art.fill(0x12FFFFC0); PA.reset(); PA.moveTo(0, topY); PA.lineTo(w * 0.55f, topY); PA.lineTo(w * 0.2f, botY); PA.lineTo(0, botY); PA.close(); c.drawPath(PA, P);
        P.setStyle(Paint.Style.FILL); P.setShader(vigG); c.drawRect(0, topY, w, botY, P); P.setShader(null);
        int wh = 0xF2FFFFFF;
        float L = Match.L, W = Match.W;
        seg(c, 0, 0, L, 0, wh); seg(c, 0, W, L, W, wh); seg(c, 0, 0, 0, W, wh); seg(c, L, 0, L, W, wh);
        seg(c, 50, 0, 50, W, wh);
        // área grande y chica
        for (int s = 0; s < 2; s++) {
            float x0 = s == 0 ? 0 : L, dd = s == 0 ? 1 : -1;
            seg(c, x0 + dd * 15f, 13f, x0 + dd * 15f, 47f, wh);
            seg(c, x0, 13f, x0 + dd * 15f, 13f, wh); seg(c, x0, 47f, x0 + dd * 15f, 47f, wh);
            seg(c, x0 + dd * 5f, 23f, x0 + dd * 5f, 37f, wh);
            seg(c, x0, 23f, x0 + dd * 5f, 23f, wh); seg(c, x0, 37f, x0 + dd * 5f, 37f, wh);
            Art.fill(wh); c.drawCircle(sx(x0 + dd * 10f, 30f), sy(30f), lw(30f) * 0.8f, P);
            PA.reset(); boolean st = false;
            for (int i = -30; i <= 30; i++) {
                float a = (float) Math.toRadians(i * 2.2f);
                float px = x0 + dd * (10f + 8f * (float) Math.cos(a)), py = 30f + 8f * (float) Math.sin(a);
                if (dd * (px - x0) < 15f) continue;
                if (!st) { PA.moveTo(sx(px, py), sy(py)); st = true; } else PA.lineTo(sx(px, py), sy(py));
            }
            Art.stroke(wh, lw(30f)); c.drawPath(PA, P);
        }
        // círculo central
        PA.reset();
        for (int i = 0; i <= 48; i++) {
            float a = i * 6.2832f / 48f, px = 50f + 8f * (float) Math.cos(a), py = 30f + 8f * (float) Math.sin(a);
            if (i == 0) PA.moveTo(sx(px, py), sy(py)); else PA.lineTo(sx(px, py), sy(py));
        }
        Art.stroke(wh, lw(30f)); c.drawPath(PA, P);
        Art.fill(wh); c.drawCircle(sx(50, 30), sy(30), lw(30f) * 0.9f, P);
    }

    float gpx(float x, float y) { return sx(x, y); }

    void goal(Canvas c, int side) {
        float x0 = side == 0 ? 0f : Match.L, d = side == 0 ? -1f : 1f, xb = x0 + d * Match.NET;
        float y1 = Match.CY - Match.GOAL_HALF, y2 = Match.CY + Match.GOAL_HALF, gh = Match.GOAL_H, hb = 2.1f;
        // red trasera
        PA.reset();
        PA.moveTo(sx(xb, y1), syz(y1, 0)); PA.lineTo(sx(xb, y2), syz(y2, 0)); PA.lineTo(sx(xb, y2), syz(y2, hb)); PA.lineTo(sx(xb, y1), syz(y1, hb)); PA.close();
        Art.fill(0x40FFFFFF); c.drawPath(PA, P);
        // red superior
        PA.reset();
        PA.moveTo(sx(x0, y1), syz(y1, gh)); PA.lineTo(sx(x0, y2), syz(y2, gh)); PA.lineTo(sx(xb, y2), syz(y2, hb)); PA.lineTo(sx(xb, y1), syz(y1, hb)); PA.close();
        Art.fill(0x33FFFFFF); c.drawPath(PA, P);
        // red lateral (lado lejano)
        PA.reset();
        PA.moveTo(sx(x0, y1), syz(y1, 0)); PA.lineTo(sx(xb, y1), syz(y1, 0)); PA.lineTo(sx(xb, y1), syz(y1, hb)); PA.lineTo(sx(x0, y1), syz(y1, gh)); PA.close();
        Art.fill(0x2CFFFFFF); c.drawPath(PA, P);
        // hilos
        for (int i = 0; i <= 6; i++) {
            float y = y1 + (y2 - y1) * i / 6f;
            Art.line(c, sx(xb, y), syz(y, 0), sx(xb, y), syz(y, hb), 1.5f, 0x66FFFFFF);
            Art.line(c, sx(x0, y), syz(y, gh), sx(xb, y), syz(y, hb), 1.5f, 0x55FFFFFF);
        }
        for (int i = 0; i <= 4; i++) {
            float z = hb * i / 4f;
            Art.line(c, sx(xb, y1), syz(y1, z), sx(xb, y2), syz(y2, z), 1.5f, 0x55FFFFFF);
        }
        // postes
        float pw = Math.max(5f, ppm * 0.3f * sc(y1));
        for (int k = 0; k < 2; k++) {
            float y = k == 0 ? y1 : y2; float pwk = Math.max(5f, ppm * 0.3f * sc(y));
            Art.line(c, sx(x0, y), syz(y, 0), sx(x0, y), syz(y, gh), pwk + 4f, INK);
            Art.line(c, sx(x0, y), syz(y, 0), sx(x0, y), syz(y, gh), pwk, 0xFFFFFFFF);
        }
        Art.line(c, sx(x0, y1), syz(y1, gh), sx(x0, y2), syz(y2, gh), pw + 4f, INK);
        Art.line(c, sx(x0, y1), syz(y1, gh), sx(x0, y2), syz(y2, gh), pw, 0xFFFFFFFF);
    }

    // ------------------------------------------------------------------ entidades
    void entities(Canvas c, Match m) {
        int n = 0;
        for (int t = 0; t < 2; t++) for (int i = 0; i < 6; i++) {
            Match.Player p = m.pl[t][i]; ents[n] = p; keys[n] = p.y; order[n] = n; n++;
        }
        keys[12] = m.by + (m.bz > 1f ? -3f : 0.3f); order[12] = 12;
        for (int i = 1; i < 13; i++) {
            int o = order[i], j = i - 1;
            while (j >= 0 && keys[order[j]] > keys[o]) { order[j + 1] = order[j]; j--; }
            order[j + 1] = o;
        }
        // sombra y estela del balón
        ballShadowAndTrail(c, m);
        for (int i = 0; i < 13; i++) {
            int o = order[i];
            if (o == 12) drawBall(c, m); else drawPlayer(c, m, ents[o]);
        }
        // marcador sobre el jugador controlado
        if (m.humanTeam >= 0 && m.ctrl != null && (m.state == Match.ST_PLAY || m.state == Match.ST_RESTART || m.state == Match.ST_KICKOFF)) {
            Match.Player p = m.ctrl;
            float gx = sx(p.x, p.y), u = ppm * sc(p.y) * 0.72f, top = sy(p.y) - 5.0f * u;
            float bob = (float) Math.sin(time * 8f) * u * 0.18f;
            int col = m.def[p.team].main;
            PA.reset(); PA.moveTo(gx - u * 0.5f, top + bob); PA.lineTo(gx + u * 0.5f, top + bob); PA.lineTo(gx, top + u * 0.55f + bob); PA.close();
            Art.fill(0xFFFFEB3B); c.drawPath(PA, P);
            Art.stroke(INK, u * 0.12f); c.drawPath(PA, P);
        }
    }

    void ballShadowAndTrail(Canvas c, Match m) {
        float u = ppm * sc(m.by), gx = sx(m.bx, m.by), gy = sy(m.by);
        float k = 1f / (1f + m.bz * 0.35f);
        Art.fill(Art.alpha(0xFF000000, (int) (70 * k)));
        R.set(gx - u * 0.55f * k, gy - u * 0.13f * k, gx + u * 0.55f * k, gy + u * 0.17f * k); c.drawOval(R, P);
        float sp = m.ballSpeed();
        if (m.owner == null && sp > 13f) {
            float bxs = gx, bys = syz(m.by, m.bz + 0.5f);
            float dx = -m.bvx * ppm * sc(m.by), dy = -m.bvy * (botY - topY) / Match.W + m.bvz * ppm * sc(m.by);
            float l = (float) Math.sqrt(dx * dx + dy * dy);
            if (l > 1f) {
                float len = Math.min(sp * 0.035f, 1f) * ppm * 3.2f * sc(m.by) * (m.ballSpecial ? 1.6f : 1f);
                dx = dx / l * len; dy = dy / l * len;
                float nx = -dy / len, ny = dx / len, wd = u * 0.34f;
                for (int i = 0; i < (m.ballSpecial ? 3 : 1); i++) {
                    float f = 1f - i * 0.28f, ww = wd * (1f - i * 0.25f);
                    PA.reset(); PA.moveTo(bxs + nx * ww, bys + ny * ww); PA.lineTo(bxs + dx * f * (1f + 0.15f * (float) Math.sin(time * 40f + i)), bys + dy * f); PA.lineTo(bxs - nx * ww, bys - ny * ww); PA.close();
                    Art.fill(m.ballSpecial ? (i == 0 ? 0x88FF5A00 : i == 1 ? 0xAAFF9800 : 0xCCFFEB3B) : 0x66FFFFFF);
                    c.drawPath(PA, P);
                }
            }
        }
    }

    void drawBall(Canvas c, Match m) {
        float u = ppm * sc(m.by), r = u * 0.58f;
        float gx = sx(m.bx, m.by), gy = syz(m.by, m.bz) - r;
        ballRot += (m.ballSpeed() * 0.4f + 0.5f) * 0.03f * (m.bvx >= 0 ? 1 : -1);
        if (m.ballSpecial) {
            Art.fill(0x55FFB300); c.drawCircle(gx, gy, r * (1.9f + 0.2f * (float) Math.sin(time * 30f)), P);
            Art.fill(0x88FF6D00); c.drawCircle(gx, gy, r * 1.35f, P);
        }
        Art.ball(c, gx, gy, r, ballRot);
    }

    void drawPlayer(Canvas c, Match m, Match.Player p) {
        float gx = sx(p.x, p.y), gy = sy(p.y), S = ppm * sc(p.y) * 0.72f;
        boolean human = m.humanTeam == p.team;
        // anillo de control
        if (p == m.ctrl) {
            Art.stroke(0xCCFFFFFF, S * 0.12f);
            R.set(gx - S * 1.25f, gy - S * 0.38f, gx + S * 1.25f, gy + S * 0.4f); c.drawOval(R, P);
        }
        // aura de carga / especial
        float chg = p.charge;
        if (m.state == Match.ST_CUTIN && p == m.specialShooter) chg = 1.2f;
        if (chg > 0.35f) aura(c, gx, gy, S, chg, p, m);
        // líneas de velocidad
        float sp = (float) Math.sqrt(p.vx * p.vx + p.vy * p.vy);
        if (p.sprinting && sp > 6f) {
            for (int i = 0; i < 3; i++) {
                float yy = gy - S * (0.9f + i * 0.9f), xo = -p.flip * S * (1.0f + (float) ((time * 7f + i * 0.37f) % 1f) * 0.8f);
                Art.line(c, gx + xo, yy, gx + xo - p.flip * S * 1.6f, yy, S * 0.07f, 0x88FFFFFF);
            }
            if (Math.random() < 0.25) spawn(0, true, p.x - p.fx * 0.5f, p.y - p.fy * 0.5f, 0.1f, 0, 0, 0.5f, 0.45f, 0xFFD7F5C0, 0.4f);
        }
        Art.body(c, m, p, gx, gy, S, time, human);
    }

    void aura(Canvas c, float gx, float gy, float S, float chg, Match.Player p, Match m) {
        boolean sp = chg > 1.1f;
        float pow = Math.min(1f, chg);
        Art.fill(sp ? 0x4400E5FF : 0x44FF8A00);
        R.set(gx - S * 1.6f, gy - S * 0.5f, gx + S * 1.6f, gy + S * 0.55f); c.drawOval(R, P);
        for (int layer = 0; layer < 2; layer++) {
            PA.reset();
            int n = 9;
            for (int i = 0; i <= n; i++) {
                float f = i / (float) n, bx = gx + (f - 0.5f) * S * (2.4f - layer * 0.7f);
                float hgt = S * (1.8f + pow * 2.2f) * (0.5f + 0.5f * (float) Math.sin(f * 3.14f)) * (0.8f + 0.35f * (float) Math.sin(time * 18f + i * 1.7f)) * (1f - layer * 0.3f);
                if (i == 0) PA.moveTo(bx, gy - S * 0.1f);
                float tipx = bx + (float) Math.sin(time * 12f + i) * S * 0.12f;
                PA.lineTo(tipx, gy - hgt - S * 0.5f);
                PA.lineTo(bx + S * 0.12f, gy - S * 0.1f);
            }
            PA.close();
            if (sp) Art.fill(layer == 0 ? 0x8800B0FF : 0xAAE1F5FE); else Art.fill(layer == 0 ? 0x88FF5A00 : 0xAAFFEB3B);
            c.drawPath(PA, P);
        }
    }

    // ------------------------------------------------------------------ HUD
    String clock(Match m) {
        if (m.golden) return "90+";
        return m.minute() + "'";
    }

    void hud(Canvas c, Match m) {
        float pw = w * 0.36f, ph = h * 0.085f, x0 = (w - pw) * 0.5f, y0 = h * 0.018f, sk = ph * 0.35f;
        // placa
        PA.reset(); PA.moveTo(x0 + sk, y0); PA.lineTo(x0 + pw, y0); PA.lineTo(x0 + pw - sk, y0 + ph); PA.lineTo(x0, y0 + ph); PA.close();
        Art.fill(0xE61A1633); c.drawPath(PA, P); Art.stroke(INK, 5f); c.drawPath(PA, P);
        float cw = pw * 0.32f;
        // equipos
        for (int t = 0; t < 2; t++) {
            Teams.Def d = m.def[t];
            float bx0 = t == 0 ? x0 + sk * 0.5f : x0 + pw - cw - sk * 0.5f;
            PA.reset(); PA.moveTo(bx0 + (t == 0 ? sk * 0.6f : 0), y0 + 4); PA.lineTo(bx0 + cw, y0 + 4); PA.lineTo(bx0 + cw - (t == 0 ? 0 : sk * 0.6f), y0 + ph - 4); PA.lineTo(bx0, y0 + ph - 4); PA.close();
            Art.fill(d.main); c.drawPath(PA, P);
            Art.fill(0x33FFFFFF); R.set(bx0, y0 + 4, bx0 + cw, y0 + ph * 0.45f); c.drawRect(R, P);
            Art.text(c, d.tag, bx0 + cw * 0.5f, y0 + ph * 0.66f, ph * 0.42f, d.second == 0xFFFFFFFF || d.second == 0xFFCFD8DC || d.second == 0xFF212121 && false ? 0xFFFFFFFF : 0xFFFFFFFF, INK, ph * 0.09f, Paint.Align.CENTER, true);
        }
        // marcador
        Art.text(c, m.score[0] + "  -  " + m.score[1], w * 0.5f, y0 + ph * 0.5f, ph * 0.5f, 0xFFFFFFFF, INK, ph * 0.1f, Paint.Align.CENTER, true);
        Art.text(c, (m.half == 1 ? "1T " : "2T ") + clock(m), w * 0.5f, y0 + ph * 0.92f, ph * 0.27f, 0xFFFFE082, INK, ph * 0.06f, Paint.Align.CENTER, true);

        // jugador controlado, resistencia y especial
        if (m.humanTeam >= 0) {
            int t = m.humanTeam; Teams.Def d = m.def[t];
            float bx = w * 0.025f, by = h * 0.03f, bw = w * 0.22f, bh = h * 0.026f;
            if (m.ctrl != null) Art.text(c, m.ctrl.number + "  " + d.names[m.ctrl.idx], bx, by + bh * 0.9f, bh * 1.1f, 0xFFFFFFFF, INK, bh * 0.22f, Paint.Align.LEFT, true);
            by += bh * 1.5f;
            bar(c, bx, by, bw, bh * 0.7f, m.ctrl != null ? m.ctrl.stamina : 1f, 0xFF76FF03, "RES");
            by += bh * 1.15f;
            float sp = m.special[t];
            boolean full = sp >= 1f;
            bar(c, bx, by, bw, bh * 0.9f, sp, full ? (time % 0.4f < 0.2f ? 0xFFFFF176 : 0xFFFFB300) : 0xFFFF9800, full ? "¡ESPECIAL!" : "ESP");
        }
        // pausa
        float pr = h * 0.055f, px = w - pr * 1.8f, py = pr * 1.8f;
        Art.fill(0xAA1A1633); c.drawCircle(px, py, pr, P); Art.stroke(INK, 4f); c.drawCircle(px, py, pr, P);
        Art.fill(0xFFFFFFFF); R.set(px - pr * 0.38f, py - pr * 0.4f, px - pr * 0.12f, py + pr * 0.4f); c.drawRect(R, P);
        R.set(px + pr * 0.12f, py - pr * 0.4f, px + pr * 0.38f, py + pr * 0.4f); c.drawRect(R, P);
        radar(c, m);
    }

    void bar(Canvas c, float x, float y, float bw, float bh, float v, int col, String label) {
        R.set(x, y, x + bw, y + bh); Art.fill(0xAA120F25); c.drawRoundRect(R, bh * 0.5f, bh * 0.5f, P);
        R.set(x + 2, y + 2, x + 2 + (bw - 4) * Math.max(0f, Math.min(1f, v)), y + bh - 2); Art.fill(col); c.drawRoundRect(R, bh * 0.5f, bh * 0.5f, P);
        R.set(x, y, x + bw, y + bh); Art.stroke(INK, 3f); c.drawRoundRect(R, bh * 0.5f, bh * 0.5f, P);
        Art.text(c, label, x + bw * 0.5f, y + bh * 0.78f, bh * 0.78f, 0xFFFFFFFF, INK, bh * 0.16f, Paint.Align.CENTER, true);
    }

    void radar(Canvas c, Match m) {
        float rw = w * 0.17f, rh = rw * 0.6f, x0 = (w - rw) * 0.5f, y0 = h - rh - h * 0.02f;
        R.set(x0, y0, x0 + rw, y0 + rh); Art.fill(0x663BA04C); c.drawRoundRect(R, 8, 8, P);
        Art.stroke(0x99FFFFFF, 2f); c.drawRoundRect(R, 8, 8, P);
        Art.line(c, x0 + rw * 0.5f, y0, x0 + rw * 0.5f, y0 + rh, 2f, 0x77FFFFFF);
        for (int t = 0; t < 2; t++) for (Match.Player p : m.pl[t]) {
            Art.fill(m.def[t].main); c.drawCircle(x0 + p.x / Match.L * rw, y0 + p.y / Match.W * rh, rh * 0.05f, P);
            if (p == m.ctrl) { Art.stroke(0xFFFFFFFF, 2f); c.drawCircle(x0 + p.x / Match.L * rw, y0 + p.y / Match.W * rh, rh * 0.09f, P); }
        }
        Art.fill(0xFFFFFFFF); c.drawCircle(x0 + m.bx / Match.L * rw, y0 + m.by / Match.W * rh, rh * 0.045f, P);
    }

    // ------------------------------------------------------------------ banners y cortes de cámara
    void banner(Canvas c, Match m) {
        if (m.bannerT <= 0 || m.bannerKind == 2) return;
        float k = Math.min(1f, (1f - Math.min(m.bannerT, 99f) % 99f / 99f));
        float a = Math.min(1f, m.bannerT * 3f);
        float pop = 1f + 0.12f * (float) Math.max(0, Math.sin(time * 6f));
        boolean big = m.bannerKind == 1;
        float sz = h * (big ? 0.11f : 0.06f);
        c.save();
        c.translate(w * 0.5f, h * (big ? 0.42f : 0.2f));
        c.rotate(-4f);
        float tw = sz * (m.banner.length() * 0.62f + 1f);
        PA.reset(); PA.moveTo(-tw * 0.5f, -sz * 0.75f); PA.lineTo(tw * 0.5f + sz * 0.3f, -sz * 0.75f); PA.lineTo(tw * 0.5f, sz * 0.35f); PA.lineTo(-tw * 0.5f - sz * 0.3f, sz * 0.35f); PA.close();
        Art.fill(Art.alpha(0xFF1A1633, (int) (220 * a))); c.drawPath(PA, P);
        Art.stroke(Art.alpha(0xFFFFE066, (int) (255 * a)), sz * 0.07f); c.drawPath(PA, P);
        c.scale(pop, pop);
        Art.text(c, m.banner, 0, 0, sz, Art.alpha(0xFFFFFFFF, (int) (255 * a)), Art.alpha(INK, (int) (255 * a)), sz * 0.14f, Paint.Align.CENTER, true);
        c.restore();
    }

    void burst(Canvas c, float cx, float cy, float inner, int n, int col, float seed) {
        for (int i = 0; i < n; i++) {
            float a = i * 6.2832f / n + seed;
            float wid = 0.5f * 6.2832f / n * (0.4f + 0.6f * (float) Math.abs(Math.sin(i * 12.9898f + seed * 3f)));
            float far = Math.max(w, h) * 1.3f;
            PA.reset();
            PA.moveTo(cx + (float) Math.cos(a - wid * 0.2f) * inner, cy + (float) Math.sin(a - wid * 0.2f) * inner);
            PA.lineTo(cx + (float) Math.cos(a - wid) * far, cy + (float) Math.sin(a - wid) * far);
            PA.lineTo(cx + (float) Math.cos(a + wid) * far, cy + (float) Math.sin(a + wid) * far);
            PA.lineTo(cx + (float) Math.cos(a + wid * 0.2f) * inner, cy + (float) Math.sin(a + wid * 0.2f) * inner);
            PA.close();
            Art.fill(col); c.drawPath(PA, P);
        }
    }

    void cutin(Canvas c, Match m) {
        float t = m.cutinT, total = 1.35f;
        Match.Player p = m.specialShooter; if (p == null) return;
        Teams.Def d = m.def[p.team];
        float a = Math.min(1f, t / 0.12f);
        Art.fill(Art.alpha(0xFF05050F, (int) (170 * a))); c.drawRect(0, 0, w, h, P);
        long fr = (long) (time * 18f);
        burst(c, w * 0.5f, h * 0.5f, h * 0.22f, 44, 0xCCFFFFFF, (fr % 7) * 0.31f);
        // banda diagonal
        float slide = 1f - (float) Math.pow(1f - Math.min(1f, t / 0.25f), 3f);
        float off = (1f - slide) * -w;
        c.save(); c.translate(off, 0);
        float y0 = h * 0.26f, y1 = h * 0.74f, sk = h * 0.18f;
        PA.reset(); PA.moveTo(-sk, y1); PA.lineTo(sk * 0.4f, y0); PA.lineTo(w + sk, y0); PA.lineTo(w, y1); PA.close();
        Art.fill(d.main); c.drawPath(PA, P);
        PA.reset(); PA.moveTo(-sk * 0.5f, y1); PA.lineTo(sk * 0.75f, y0 + (y1 - y0) * 0.62f); PA.lineTo(w + sk, y0 + (y1 - y0) * 0.62f); PA.lineTo(w, y1); PA.close();
        Art.fill(Art.shade(d.main, 0.7f)); c.drawPath(PA, P);
        Art.stroke(INK, h * 0.012f);
        PA.reset(); PA.moveTo(-sk, y1); PA.lineTo(sk * 0.4f, y0); PA.lineTo(w + sk, y0); PA.lineTo(w, y1); PA.close(); c.drawPath(PA, P);
        Art.line(c, sk * 0.5f, y0 - h * 0.012f, w, y0 - h * 0.012f, h * 0.012f, 0xFFFFFFFF);
        Art.line(c, 0, y1 + h * 0.012f, w - sk * 0.5f, y1 + h * 0.012f, h * 0.012f, 0xFFFFFFFF);
        c.restore();
        // retrato
        float px = w * 0.72f + (1f - slide) * w * 0.3f, py = h * 0.5f;
        float S = h * 0.2f;
        burst(c, px, py, S * 1.0f, 28, Art.alpha(d.second == 0xFFFFFFFF ? 0xFFFFFFFF : d.second, 170), (fr % 5) * 0.4f);
        Art.face(c, d, p.idx, px, py, S, 1f, time, 3, 6f);
        // sudor/brillo en los ojos
        Art.fill(0xFFFFFFFF);
        for (int i = 0; i < 3; i++) c.drawCircle(px + S * (0.75f + i * 0.2f), py - S * (0.5f + 0.1f * i), S * 0.04f, P);
        // texto
        float k = Math.min(1f, Math.max(0f, (t - 0.12f) / 0.18f));
        c.save(); c.translate(w * 0.06f + (1f - k) * -w * 0.4f, h * 0.5f); c.rotate(-5f);
        Art.text(c, "TIRO ESPECIAL", 0, -h * 0.07f, h * 0.05f, 0xFFFFE066, INK, h * 0.012f, Paint.Align.LEFT, true);
        String nm = d.special.toUpperCase(java.util.Locale.ROOT);
        float size = Math.min(h * 0.12f, (w * 0.5f) / (nm.length() * 0.6f));
        Art.text(c, nm, 0, h * 0.04f, size, 0xFFFFFFFF, INK, size * 0.14f, Paint.Align.LEFT, true);
        Art.text(c, p.number + " " + d.names[p.idx], 0, h * 0.04f + size * 0.7f, h * 0.045f, 0xFFFFFFFF, INK, h * 0.01f, Paint.Align.LEFT, true);
        c.restore();
        float fa = t < 0.1f ? 1f - t / 0.1f : (t > total - 0.12f ? (t - (total - 0.12f)) / 0.12f : 0f);
        if (fa > 0) { Art.fill(Art.alpha(0xFFFFFFFF, (int) (255 * fa))); c.drawRect(0, 0, w, h, P); }
    }

    void goalOverlay(Canvas c, Match m) {
        float t = 4.4f - m.stateT;           // tiempo desde el gol
        Teams.Def d = m.def[m.lastScoringTeam];
        if (t < 3.2f) {
            float k = Math.min(1f, t / 0.3f);
            Art.fill(Art.alpha(0xFF000000, (int) (70 * k))); c.drawRect(0, 0, w, h, P);
            if (t < 1.4f) burst(c, w * 0.5f, h * 0.4f, h * 0.1f, 36, Art.alpha(d.main, 60), time * 0.3f);
            float pop = k < 1f ? 0.4f + 1.3f * k : 1f + 0.06f * (float) Math.sin(time * 12f);
            float sz = h * 0.2f;
            c.save(); c.translate(w * 0.5f, h * 0.42f); c.rotate(-6f); c.scale(pop, pop);
            String txt = "¡GOOOL!";
            Art.text(c, txt, sz * 0.05f, sz * 0.06f, sz, Art.alpha(d.accent, 255), INK, sz * 0.22f, Paint.Align.CENTER, true);
            Art.text(c, txt, 0, 0, sz, 0xFFFFE259, INK, sz * 0.12f, Paint.Align.CENTER, true);
            Art.text(c, txt, 0, -sz * 0.04f, sz * 0.9f, 0xFFFFF7B0, 0, 0, Paint.Align.CENTER, true);
            c.restore();
            // retrato del goleador
            Match.Player sc = m.lastScorer;
            if (sc != null && t > 0.2f) {
                float S = h * 0.11f, px = w * 0.15f, py = h * 0.62f;
                float slide = Math.min(1f, (t - 0.2f) / 0.3f);
                px = -S * 2f + (w * 0.14f + S * 2f) * slide;
                Art.fill(Art.alpha(d.main, 230)); c.drawCircle(px, py, S * 1.45f, P);
                Art.stroke(INK, S * 0.1f); c.drawCircle(px, py, S * 1.45f, P);
                Art.face(c, d, sc.idx, px, py, S, 1f, time, 2, 0f);
                Art.text(c, sc.number + " " + d.names[sc.idx], px + S * 1.7f, py - S * 0.1f, h * 0.05f, 0xFFFFFFFF, INK, h * 0.011f, Paint.Align.LEFT, true);
                Art.text(c, m.minute() + "'", px + S * 1.7f, py + S * 0.55f, h * 0.045f, 0xFFFFE082, INK, h * 0.01f, Paint.Align.LEFT, true);
            } else if (sc == null && t > 0.2f) {
                Art.text(c, "GOL EN CONTRA", w * 0.5f, h * 0.62f, h * 0.05f, 0xFFFFFFFF, INK, h * 0.011f, Paint.Align.CENTER, true);
            }
        }
        drawParticles(c);
    }

    // ------------------------------------------------------------------ controles táctiles
    static final int[] BCOL = {0xFF43D17A, 0xFFFF4D5E, 0xFF4DA3FF, 0xFFFFC21A, 0xFFFFE04D};

    void pad(Canvas c, Pad pad, Match m) {
        pad.layout(w, h);
        boolean have = m.owner != null && m.owner.team == m.humanTeam;
        // joystick
        Art.fill(0x33FFFFFF); c.drawCircle(pad.baseX, pad.baseY, pad.jr, P);
        Art.stroke(0x99FFFFFF, 5f); c.drawCircle(pad.baseX, pad.baseY, pad.jr, P);
        Art.fill(0x88FFFFFF); c.drawCircle(pad.knobX, pad.knobY, pad.jr * 0.45f, P);
        Art.stroke(INK, 4f); c.drawCircle(pad.knobX, pad.knobY, pad.jr * 0.45f, P);
        String[] lab = have ? new String[]{"PASE", "TIRO", "LARGO"} : new String[]{"CAMBIO", "ENTRADA", "BARRIDA"};
        for (int i = 0; i < 5; i++) {
            float x = pad.bx[i], y = pad.by[i], r = pad.br[i] * (pad.down[i] ? 0.92f : 1f);
            int col = BCOL[i];
            boolean ready = i == Pad.D && m.humanTeam >= 0 && m.special[m.humanTeam] >= 1f;
            if (i == Pad.D && !ready) col = 0xFF6B6480;
            if (ready) { Art.fill(Art.alpha(0xFFFFD54F, 90 + (int) (70 * Math.sin(time * 10f)))); c.drawCircle(x, y, r * 1.35f, P); }
            Art.fill(Art.alpha(col, pad.down[i] ? 255 : 200)); c.drawCircle(x, y, r, P);
            Art.fill(0x44FFFFFF); R.set(x - r * 0.8f, y - r * 0.9f, x + r * 0.8f, y - r * 0.1f); c.drawOval(R, P);
            Art.stroke(INK, 6f); c.drawCircle(x, y, r, P);
            if (i == Pad.D) {
                float k = m.humanTeam >= 0 ? m.special[m.humanTeam] : 0f;
                if (!ready) {
                    PA.reset();
                    for (int j = 0; j <= 20; j++) { float a = -1.5708f + 6.2832f * k * j / 20f; float px = x + (float) Math.cos(a) * r * 0.86f, py = y + (float) Math.sin(a) * r * 0.86f; if (j == 0) PA.moveTo(px, py); else PA.lineTo(px, py); }
                    Art.stroke(0xFFFFC21A, 7f); c.drawPath(PA, P);
                }
                PA.reset();
                for (int j = 0; j < 10; j++) { float a = -1.5708f + j * 0.6283f, rr = (j % 2 == 0) ? r * 0.55f : r * 0.24f; float px = x + (float) Math.cos(a) * rr, py = y + (float) Math.sin(a) * rr; if (j == 0) PA.moveTo(px, py); else PA.lineTo(px, py); }
                PA.close(); Art.fill(0xFFFFFFFF); c.drawPath(PA, P); Art.stroke(INK, 3f); c.drawPath(PA, P);
            } else if (i == Pad.S) {
                Art.text(c, "CORRER", x, y + r * 0.2f, r * 0.42f, 0xFFFFFFFF, INK, r * 0.1f, Paint.Align.CENTER, true);
            } else {
                Art.text(c, lab[i], x, y + r * 0.2f, r * (lab[i].length() > 5 ? 0.36f : 0.46f), 0xFFFFFFFF, INK, r * 0.1f, Paint.Align.CENTER, true);
            }
        }
        // medidor de carga de tiro
        if (m.ctrl != null && m.ctrl.charge > 0.05f && m.owner == m.ctrl) {
            float ch = m.ctrl.charge, bw = w * 0.16f, bh = h * 0.022f, bx = pad.bx[Pad.B] - bw * 0.5f, by = pad.by[Pad.B] - pad.br[Pad.B] * 1.45f;
            bar(c, bx, by, bw, bh, ch, ch > 0.85f ? 0xFFFF5252 : 0xFFFFC107, "POTENCIA");
        }
    }
}
