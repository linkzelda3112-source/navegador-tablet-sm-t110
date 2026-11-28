package com.craterz;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

/** Interfaz sobre el 3D: controles táctiles, marcador, radar, pausa y fin de partida. */
public final class Hud extends View {
    public interface Listener {
        void onHudResume();
        void onHudRestart();
        void onHudMenu();
        void onHudPause();
    }

    private static final int ROLE_NONE = 0, ROLE_MOVE = 1, ROLE_LOOK = 2, ROLE_FIRE = 3, ROLE_BTN = 4;

    private final Game game;
    private final Input in;
    private final Listener listener;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();
    private final Typeface bold = Typeface.create("sans-serif-condensed", Typeface.BOLD);
    private final Typeface serif = Typeface.create(Typeface.SERIF, Typeface.BOLD);
    private float d; // densidad de píxeles

    // controles
    private final int[] role = new int[12], btn = new int[12];
    private final float[] lastX = new float[12], lastY = new float[12];
    private float joyCx, joyCy, joyX, joyY;
    private boolean joyOn;
    private int joyPid = -1;
    private float fireX, fireY, fireR, fire2X, fire2Y, adsX, adsY, rldX, rldY, swpX, swpY, useX, useY, pauseX, pauseY, smallR;
    private boolean fireDown;
    private int firePtr = -1, firePtr2 = -1;
    public float fps;

    // pausa / fin
    private final RectF bResume = new RectF(), bRestart = new RectF(), bMenu = new RectF();

    public Hud(Context c, Game game, Input in, Listener l) {
        super(c);
        this.game = game; this.in = in; this.listener = l;
        d = c.getResources().getDisplayMetrics().density;
        for (int i = 0; i < role.length; i++) role[i] = ROLE_NONE;
        setFocusable(false);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        float u = Math.min(w, h);
        fireR = u * 0.12f;
        smallR = u * 0.075f;
        fireX = w - u * 0.20f; fireY = h - u * 0.24f;
        fire2X = u * 0.50f; fire2Y = h - u * 0.30f;
        adsX = w - u * 0.46f; adsY = h - u * 0.15f;
        rldX = w - u * 0.12f; rldY = h - u * 0.55f;
        swpX = w - u * 0.33f; swpY = h - u * 0.50f;
        useX = w - u * 0.12f; useY = h - u * 0.74f;
        pauseX = w - u * 0.07f; pauseY = u * 0.07f;
        joyCx = u * 0.26f; joyCy = h - u * 0.27f;
        float bw = u * 0.62f, bh = u * 0.13f;
        float cx = w / 2f;
        bResume.set(cx - bw / 2, h * 0.40f, cx + bw / 2, h * 0.40f + bh);
        bRestart.set(cx - bw / 2, h * 0.40f + bh * 1.25f, cx + bw / 2, h * 0.40f + bh * 2.25f);
        bMenu.set(cx - bw / 2, h * 0.40f + bh * 2.5f, cx + bw / 2, h * 0.40f + bh * 3.5f);
    }

    // ------------------------------------------------------------------ tacto
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int act = e.getActionMasked();
        int idx = e.getActionIndex();
        if (game.over || game.paused) {
            if (act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_POINTER_UP) {
                float x = e.getX(idx), y = e.getY(idx);
                if (game.over) {
                    if (bRestart.contains(x, y)) listener.onHudRestart();
                    else if (bMenu.contains(x, y)) listener.onHudMenu();
                } else {
                    if (bResume.contains(x, y)) listener.onHudResume();
                    else if (bRestart.contains(x, y)) listener.onHudRestart();
                    else if (bMenu.contains(x, y)) listener.onHudMenu();
                }
            }
            return true;
        }
        switch (act) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int id = e.getPointerId(idx);
                if (id >= role.length) return true;
                down(id, e.getX(idx), e.getY(idx));
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                for (int i = 0; i < e.getPointerCount(); i++) {
                    int id = e.getPointerId(i);
                    if (id >= role.length) continue;
                    move(id, e.getX(i), e.getY(i));
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (act == MotionEvent.ACTION_CANCEL) {
                    for (int i = 0; i < role.length; i++) release(i);
                } else {
                    int id = e.getPointerId(idx);
                    if (id < role.length) release(id);
                }
                break;
            }
        }
        return true;
    }

    private boolean hit(float x, float y, float cx, float cy, float r) {
        float dx = x - cx, dy = y - cy;
        return dx * dx + dy * dy <= r * r;
    }

    private void down(int id, float x, float y) {
        lastX[id] = x; lastY[id] = y;
        if (hit(x, y, pauseX, pauseY, smallR * 1.1f)) { role[id] = ROLE_BTN; btn[id] = 9; return; }
        if (hit(x, y, fireX, fireY, fireR * 1.25f)) { role[id] = ROLE_FIRE; firePtr = id; in.fire = true; return; }
        if (hit(x, y, fire2X, fire2Y, fireR * 0.95f) && x < getWidth() * 0.5f) { role[id] = ROLE_FIRE; firePtr2 = id; in.fire = true; return; }
        if (hit(x, y, adsX, adsY, smallR * 1.25f)) { role[id] = ROLE_BTN; btn[id] = 1; in.pressAds(); return; }
        if (hit(x, y, rldX, rldY, smallR * 1.2f)) { role[id] = ROLE_BTN; btn[id] = 2; in.pressReload(); return; }
        if (hit(x, y, swpX, swpY, smallR * 1.2f)) { role[id] = ROLE_BTN; btn[id] = 3; in.pressSwap(); return; }
        if (hit(x, y, useX, useY, smallR * 1.35f)) { role[id] = ROLE_BTN; btn[id] = 4; in.pressUse(); return; }
        if (x < getWidth() * 0.42f && joyPid < 0) {
            role[id] = ROLE_MOVE; joyPid = id; joyOn = true;
            joyCx = x; joyCy = y; joyX = 0; joyY = 0;
            in.moveX = 0; in.moveY = 0;
            return;
        }
        role[id] = ROLE_LOOK;
    }

    private void move(int id, float x, float y) {
        int r = role[id];
        if (r == ROLE_MOVE) {
            float max = Math.min(getWidth(), getHeight()) * 0.11f;
            float dx = x - joyCx, dy = y - joyCy;
            float l = (float) Math.sqrt(dx * dx + dy * dy);
            if (l > max) { dx = dx / l * max; dy = dy / l * max; }
            joyX = dx; joyY = dy;
            float nx = dx / max, ny = -dy / max;
            // zona muerta suave
            float m = (float) Math.sqrt(nx * nx + ny * ny);
            if (m < 0.12f) { nx = 0; ny = 0; }
            in.moveX = nx; in.moveY = ny;
        } else if (r == ROLE_LOOK) {
            in.addLook(x - lastX[id], y - lastY[id]);
        }
        lastX[id] = x; lastY[id] = y;
    }

    private void release(int id) {
        int r = role[id];
        if (r == ROLE_MOVE) { joyPid = -1; joyOn = false; in.moveX = 0; in.moveY = 0; joyX = joyY = 0; }
        else if (r == ROLE_FIRE) {
            if (id == firePtr) firePtr = -1;
            if (id == firePtr2) firePtr2 = -1;
            if (firePtr < 0 && firePtr2 < 0) in.fire = false;
        } else if (r == ROLE_BTN && btn[id] == 9) {
            in.pressPause();
            listener.onHudPause();
        }
        role[id] = ROLE_NONE;
    }

    // ------------------------------------------------------------------ dibujo
    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        float u = Math.min(w, h);
        Game g = game;

        drawRadar(c, u);
        drawTopLeft(c, u);
        drawScore(c, w, u);
        drawCrosshair(c, w, h, u);
        drawBottomInfo(c, w, h, u);
        drawMessages(c, w, h, u);
        drawControls(c, w, h);
        if (g.over) drawOver(c, w, h, u);
        else if (g.paused) drawPause(c, w, h, u);
    }

    private void text(Canvas c, String s, float x, float y, float size, int color, Paint.Align al, Typeface tf) {
        tp.setTypeface(tf);
        tp.setTextSize(size);
        tp.setTextAlign(al);
        tp.setShadowLayer(size * 0.12f, 0, size * 0.04f, 0xFF000000);
        tp.setColor(color);
        c.drawText(s, x, y, tp);
        tp.clearShadowLayer();
    }

    private void drawRadar(Canvas c, float u) {
        Game g = game;
        float r = u * 0.12f, cx = r + u * 0.03f, cy = r + u * 0.03f + u * 0.18f;
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x66000000);
        c.drawCircle(cx, cy, r, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(d * 1.5f);
        p.setColor(0x55A8C8B8);
        c.drawCircle(cx, cy, r, p);
        c.drawCircle(cx, cy, r * 0.5f, p);
        p.setStyle(Paint.Style.FILL);
        float sy = (float) Math.sin(Math.toRadians(g.yaw)), cyw = (float) Math.cos(Math.toRadians(g.yaw));
        float range = 32f;
        for (int i = 0; i < g.zs.size(); i++) {
            Game.Zombie z = g.zs.get(i);
            if (z.state == 2) continue;
            float dx = z.x - g.px, dz = z.z - g.pz;
            // a coordenadas de pantalla: arriba = adelante
            float fx = dx * cyw + dz * sy;
            float fy = dx * sy - dz * cyw;
            float dd = (float) Math.sqrt(fx * fx + fy * fy);
            if (dd > range) { fx = fx / dd * range; fy = fy / dd * range; }
            p.setColor(z.brute ? 0xFFFF8A2A : 0xFFE03030);
            c.drawCircle(cx + fx / range * r * 0.94f, cy - fy / range * r * 0.94f, d * (z.brute ? 3.6f : 2.6f), p);
        }
        p.setColor(0xFFFFFFFF);
        c.drawCircle(cx, cy, d * 2.6f, p);
        p.setColor(0x66FFFFFF);
        c.drawLine(cx, cy, cx, cy - r * 0.35f, p);
    }

    private void drawTopLeft(Canvas c, float u) {
        Game g = game;
        String rn = g.round <= 0 ? "-" : String.valueOf(g.round);
        text(c, rn, u * 0.05f, u * 0.17f, u * 0.17f, 0xFFB01818, Paint.Align.LEFT, serif);
        if (g.roundActive) {
            text(c, "restan " + (g.toSpawn + Math.max(0, g.alive)), u * 0.05f + u * 0.02f + tp.measureText(rn), u * 0.17f, u * 0.042f, 0xBBFFFFFF, Paint.Align.LEFT, bold);
        }
    }

    private void drawScore(Canvas c, int w, float u) {
        Game g = game;
        text(c, String.valueOf(g.points), w - u * 0.17f, u * 0.095f, u * 0.075f, g.doubleT > 0 ? 0xFFFFD84A : 0xFFFFFFFF, Paint.Align.RIGHT, bold);
        text(c, "puntos", w - u * 0.17f, u * 0.14f, u * 0.035f, 0xAAFFFFFF, Paint.Align.RIGHT, bold);
        // bonificaciones activas
        float y = u * 0.22f;
        if (g.instaT > 0) { chip(c, w - u * 0.17f, y, "MUERTE INSTANTÁNEA " + (int) g.instaT, 0xFFD43030, u); y += u * 0.06f; }
        if (g.doubleT > 0) { chip(c, w - u * 0.17f, y, "PUNTOS x2 " + (int) g.doubleT, 0xFFE0B020, u); y += u * 0.06f; }
        if (fps > 0 && Prefs.quality() >= 0 && showFps) text(c, (int) fps + " fps", w - u * 0.17f, y + u * 0.04f, u * 0.03f, 0x99FFFFFF, Paint.Align.RIGHT, bold);
    }

    public boolean showFps = false;

    private void chip(Canvas c, float rx, float y, String s, int col, float u) {
        tp.setTypeface(bold); tp.setTextSize(u * 0.032f);
        float tw = tp.measureText(s);
        rf.set(rx - tw - u * 0.03f, y - u * 0.034f, rx + u * 0.005f, y + u * 0.012f);
        p.setStyle(Paint.Style.FILL);
        p.setColor((col & 0x00FFFFFF) | 0xAA000000);
        c.drawRoundRect(rf, u * 0.015f, u * 0.015f, p);
        text(c, s, rx - u * 0.008f, y + u * 0.004f, u * 0.032f, col, Paint.Align.RIGHT, bold);
    }

    private void drawCrosshair(Canvas c, int w, int h, float u) {
        Game g = game;
        float cx = w / 2f, cy = h / 2f;
        float gap = u * (0.012f + 0.025f * (1f - g.adsT)) + g.speedNow * u * 0.002f + g.recoil * u * 0.012f;
        float len = u * 0.018f;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(d * 2f);
        p.setColor(0xCCFFFFFF);
        c.drawLine(cx - gap - len, cy, cx - gap, cy, p);
        c.drawLine(cx + gap, cy, cx + gap + len, cy, p);
        c.drawLine(cx, cy - gap - len, cx, cy - gap, p);
        c.drawLine(cx, cy + gap, cx, cy + gap + len, p);
        if (g.hitMarker > 0) {
            float a = g.hitMarker / 0.18f;
            p.setColor(g.headMarker > 0 ? ((int) (255 * a) << 24 | 0xFF3030) : ((int) (255 * a) << 24 | 0xFFFFFF));
            float k = u * 0.03f;
            c.drawLine(cx - k, cy - k, cx - k * 0.4f, cy - k * 0.4f, p);
            c.drawLine(cx + k, cy - k, cx + k * 0.4f, cy - k * 0.4f, p);
            c.drawLine(cx - k, cy + k, cx - k * 0.4f, cy + k * 0.4f, p);
            c.drawLine(cx + k, cy + k, cx + k * 0.4f, cy + k * 0.4f, p);
        }
    }

    private void drawBottomInfo(Canvas c, int w, int h, float u) {
        Game g = game;
        int wp = g.weapon();
        float bx = w * 0.5f, by = h - u * 0.05f;
        // vida
        float bw = u * 0.55f, bh = u * 0.022f;
        rf.set(bx - bw / 2, by - bh - u * 0.1f, bx + bw / 2, by - u * 0.1f);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x88000000);
        c.drawRoundRect(rf, bh, bh, p);
        float hp = Math.max(0f, g.health / g.maxHealth);
        rf.set(bx - bw / 2, by - bh - u * 0.1f, bx - bw / 2 + bw * hp, by - u * 0.1f);
        p.setColor(hp > 0.5f ? 0xFF58B04A : (hp > 0.25f ? 0xFFD8A030 : 0xFFD83030));
        c.drawRoundRect(rf, bh, bh, p);
        // munición
        String am = g.reloadT > 0 ? "recargando…" : g.ammo[g.cur][0] + " / " + g.ammo[g.cur][1];
        text(c, am, bx, by - u * 0.02f, u * 0.06f, g.ammo[g.cur][0] == 0 && g.reloadT <= 0 ? 0xFFFF5040 : 0xFFFFFFFF, Paint.Align.CENTER, bold);
        text(c, Weapons.NAME[wp], bx, by - u * 0.075f - bh * 1.4f - u * 0.012f, u * 0.032f, 0xCCFFFFFF, Paint.Align.CENTER, bold);
        if (wp == Weapons.STORM) {
            float cw = u * 0.34f, ch = u * 0.013f, cy = by - u * 0.1f - bh - u * 0.03f;
            rf.set(bx - cw / 2, cy, bx + cw / 2, cy + ch);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0x88000000);
            c.drawRoundRect(rf, ch, ch, p);
            boolean ready = g.stormBolt;
            float k = ready ? 1f : g.stormCharge;
            rf.set(bx - cw / 2, cy, bx - cw / 2 + cw * k, cy + ch);
            p.setColor(ready ? (((int) (g.time * 8) % 2 == 0) ? 0xFF9AF0FF : 0xFF40C8F0) : 0xFF2880A8);
            c.drawRoundRect(rf, ch, ch, p);
            text(c, ready ? "RAYO LISTO" : "recargando rayo", bx, cy - u * 0.008f, u * 0.026f, ready ? 0xFF9AF0FF : 0xAA7ABCD8, Paint.Align.CENTER, bold);
        }
        // ventajas
        float px0 = bx - bw / 2;
        float r = u * 0.022f;
        for (int i = 0; i < 4; i++) {
            if (!g.hasPerk(i)) continue;
            float[] col = World.PERK_COLOR[i];
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.argb(230, (int) (col[0] * 255), (int) (col[1] * 255), (int) (col[2] * 255)));
            c.drawCircle(px0 + r, by - u * 0.17f, r, p);
            text(c, World.PERK_NAME[i].substring(0, 1), px0 + r, by - u * 0.17f + r * 0.4f, r * 1.2f, 0xFF101010, Paint.Align.CENTER, bold);
            px0 += r * 2.4f;
        }
    }

    private void drawMessages(Canvas c, int w, int h, float u) {
        Game g = game;
        if (!g.prompt.isEmpty()) {
            text(c, g.prompt, w / 2f, h * 0.70f, u * 0.045f, g.points >= g.promptCost ? 0xFFFFE070 : 0xFFFF8A7A, Paint.Align.CENTER, bold);
        }
        if (g.toastT > 0) {
            int a = (int) (255 * Math.min(1f, g.toastT * 1.5f));
            text(c, g.toast, w / 2f, h * 0.28f, u * 0.05f, (a << 24) | 0xE8E0C8, Paint.Align.CENTER, serif);
        }
        if (g.roundBanner > 0) {
            float a = Math.min(1f, g.roundBanner) * Math.min(1f, (4f - g.roundBanner) * 2f + 0.2f);
            int ia = (int) (255 * Math.max(0f, Math.min(1f, a)));
            text(c, "RONDA " + g.round, w / 2f, h * 0.44f, u * 0.16f, (ia << 24) | 0xB01818, Paint.Align.CENTER, serif);
        } else if (!g.roundActive && g.round > 0 && !g.over) {
            text(c, "Próxima ronda en " + (int) Math.ceil(g.roundWait), w / 2f, h * 0.2f, u * 0.04f, 0xAAFFFFFF, Paint.Align.CENTER, bold);
        }
        if (g.round == 0 && !g.over) {
            text(c, "Prepárate…", w / 2f, h * 0.35f, u * 0.06f, 0xCCFFFFFF, Paint.Align.CENTER, serif);
        }
    }

    private void drawControls(Canvas c, int w, int h) {
        float u = Math.min(w, h);
        // joystick
        p.setStyle(Paint.Style.FILL);
        p.setColor(joyOn ? 0x33FFFFFF : 0x1AFFFFFF);
        c.drawCircle(joyCx, joyCy, u * 0.11f, p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(d * 2f); p.setColor(0x55FFFFFF);
        c.drawCircle(joyCx, joyCy, u * 0.11f, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(joyOn ? 0x99FFFFFF : 0x44FFFFFF);
        c.drawCircle(joyCx + joyX, joyCy + joyY, u * 0.055f, p);

        button(c, fireX, fireY, fireR, "FUEGO", firePtr >= 0, 0xFFD04040);
        button(c, fire2X, fire2Y, fireR * 0.8f, "FUEGO", firePtr2 >= 0, 0xFFD04040);
        button(c, adsX, adsY, smallR, "MIRA", game.adsOn, 0xFF70A0D0);
        button(c, rldX, rldY, smallR, "RECARGAR", game.reloadT > 0, 0xFFD0B070);
        button(c, swpX, swpY, smallR, "CAMBIAR", game.swapT > 0, 0xFFA0A0A0);
        boolean near = !game.prompt.isEmpty();
        button(c, useX, useY, smallR * 1.1f, "USAR", near, near && game.points >= game.promptCost ? 0xFF58D070 : 0xFFE0A040);
        // pausa
        p.setStyle(Paint.Style.FILL); p.setColor(0x66000000);
        c.drawCircle(pauseX, pauseY, smallR * 0.8f, p);
        p.setColor(0xDDFFFFFF);
        float k = smallR * 0.28f;
        c.drawRect(pauseX - k * 1.05f, pauseY - k, pauseX - k * 0.35f, pauseY + k, p);
        c.drawRect(pauseX + k * 0.35f, pauseY - k, pauseX + k * 1.05f, pauseY + k, p);
    }

    private void button(Canvas c, float x, float y, float r, String label, boolean on, int col) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(on ? ((col & 0x00FFFFFF) | 0x99000000) : 0x44000000);
        c.drawCircle(x, y, r, p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(d * 2f);
        p.setColor((col & 0x00FFFFFF) | (on ? 0xFF000000 : 0x99000000));
        c.drawCircle(x, y, r, p);
        text(c, label, x, y + r * 0.12f, r * (label.length() > 5 ? 0.30f : 0.38f), 0xFFFFFFFF, Paint.Align.CENTER, bold);
    }

    private void panel(Canvas c, int w, int h, float u, String title) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xCC000000);
        c.drawRect(0, 0, w, h, p);
        text(c, title, w / 2f, h * 0.22f, u * 0.12f, 0xFFB01818, Paint.Align.CENTER, serif);
    }

    private void menuButton(Canvas c, RectF r, String label, int col) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xCC15191C);
        c.drawRoundRect(r, r.height() * 0.2f, r.height() * 0.2f, p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(d * 2.5f); p.setColor(col);
        c.drawRoundRect(r, r.height() * 0.2f, r.height() * 0.2f, p);
        text(c, label, r.centerX(), r.centerY() + r.height() * 0.16f, r.height() * 0.46f, 0xFFFFFFFF, Paint.Align.CENTER, bold);
    }

    private void drawPause(Canvas c, int w, int h, float u) {
        panel(c, w, h, u, "PAUSA");
        menuButton(c, bResume, "Continuar", 0xFF58B04A);
        menuButton(c, bRestart, "Reiniciar", 0xFFD0B070);
        menuButton(c, bMenu, "Salir al menú", 0xFFB04A4A);
    }

    private void drawOver(Canvas c, int w, int h, float u) {
        Game g = game;
        panel(c, w, h, u, "HAS CAÍDO");
        text(c, "Sobreviviste " + Math.max(0, g.round) + (g.round == 1 ? " ronda" : " rondas"), w / 2f, h * 0.31f, u * 0.06f, 0xFFFFFFFF, Paint.Align.CENTER, serif);
        text(c, g.kills + " zombis abatidos  ·  " + g.points + " puntos", w / 2f, h * 0.36f, u * 0.04f, 0xCCFFFFFF, Paint.Align.CENTER, bold);
        menuButton(c, bRestart, "Reintentar", 0xFF58B04A);
        menuButton(c, bMenu, "Menú", 0xFFB04A4A);
    }
}
