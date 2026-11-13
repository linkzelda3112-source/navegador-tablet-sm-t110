package com.kiarena;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Random;

/**
 * Toda la interfaz y animación del juego en una sola vista, dibujada con Canvas.
 * Se trabaja en coordenadas virtuales de 1280x720 que se escalan a la pantalla.
 */
public class GameView extends View {
    private static final int W = 720, H = 1280;
    private static final int S_MENU = 0, S_SELECT = 1, S_BATTLE = 2;
    private static final int PH_INTRO = 0, PH_INPUT = 1, PH_ANIM = 2, PH_END = 3, PH_RESULT = 4;
    private static final float GROUND = 600f, FSCALE = 2.1f;
    private static final float[] PX = {190f, 530f};

    private final Save save;
    private final Sfx sfx;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rf = new RectF();
    private final Random rnd = new Random();
    private final Bitmap[] bgs = new Bitmap[5];

    private float sc = 1, ox = 0, oy = 0;
    private long lastNs = 0;
    private float time = 0;

    private int screen = S_MENU;
    private boolean campaign = true;
    private int stage = 1;
    private final ArrayList<Integer> team = new ArrayList<>();
    private int focus = 0;

    // botones fijos
    private final RectF bCampaign = new RectF(110, 830, 610, 920);
    private final RectF bFree = new RectF(110, 936, 610, 1026);
    private final RectF bSound = new RectF(110, 1042, 610, 1112);
    private final RectF bBack = new RectF(20, 20, 160, 74);
    private final RectF bFight = new RectF(460, 985, 700, 1115);
    private final RectF bRes1 = new RectF(80, 740, 355, 824);
    private final RectF bRes2 = new RectF(365, 740, 640, 824);
    private final RectF bQuit = new RectF(650, 14, 706, 52);

    // combate
    private Battle b;
    private int bgIdx = 0;
    private int aiLevel = 3;
    private int phase = PH_INTRO;
    private float introT, endT;
    private final float[][] hpShow = new float[2][3];
    private final int[][] hpTgt = new int[2][3];
    private final int[] dispActive = new int[2];
    private final int[] uiKi = new int[2];
    private final int[] uiHand = new int[4];
    private final ArrayDeque<Battle.Ev> queue = new ArrayDeque<>();
    private Battle.Ev cur;
    private float evT, evDur, evHit;
    private boolean hitDone, subDone;
    private final float[] offX = new float[2];
    private final float[] hitUntil = new float[2];
    private final boolean[] ko = new boolean[2];
    private final boolean[] holdGuard = new boolean[2];
    private final int[] alpha = {255, 255};
    private String banner = "";
    private float bannerT, bannerTotal;
    private int bannerColor = 0xFFFFFFFF;
    private float shake, darken, quitArm;
    private boolean won;
    private String resultMsg = "";

    private static final class Particle { float x, y, vx, vy, life, max, size; int color; }
    private static final class Floater { String s; float x, y, life, max, size; int color; }
    private final ArrayList<Particle> particles = new ArrayList<>();
    private final ArrayList<Floater> floaters = new ArrayList<>();

    public GameView(Context ctx) {
        super(ctx);
        save = new Save(ctx);
        sfx = new Sfx();
        sfx.enabled = save.sound();
        team.add(0);
        team.add(1);
        team.add(2);
        stage = Math.min(Roster.STAGES, save.cleared() + 1);
    }

    void release() { sfx.release(); }

    // ======================================================================= bucle

    @Override
    protected void onDraw(Canvas c) {
        long now = System.nanoTime();
        float dt = lastNs == 0 ? 0.016f : (now - lastNs) / 1e9f;
        lastNs = now;
        if (dt > 0.05f) dt = 0.05f;
        time += dt;

        c.drawColor(0xFF000000);
        sc = Math.min(getWidth() / (float) W, getHeight() / (float) H);
        ox = (getWidth() - W * sc) / 2f;
        oy = (getHeight() - H * sc) / 2f;
        c.save();
        c.translate(ox, oy);
        c.scale(sc, sc);
        c.clipRect(0, 0, W, H);

        updateParticles(dt);
        switch (screen) {
            case S_MENU: drawMenu(c); break;
            case S_SELECT: drawSelect(c); break;
            default:
                updateBattle(dt);
                drawBattle(c);
                break;
        }
        c.restore();
        postInvalidateOnAnimation();
    }

    // ======================================================================= toques

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            float x = (e.getX() - ox) / sc, y = (e.getY() - oy) / sc;
            switch (screen) {
                case S_MENU: tapMenu(x, y); break;
                case S_SELECT: tapSelect(x, y); break;
                default: tapBattle(x, y); break;
            }
        }
        return true;
    }

    boolean onBack() {
        if (screen == S_BATTLE || screen == S_SELECT) {
            screen = S_MENU;
            sfx.play(Sfx.SELECT);
            return true;
        }
        return false;
    }

    private void tapMenu(float x, float y) {
        if (bCampaign.contains(x, y)) {
            campaign = true;
            stage = Math.min(Roster.STAGES, save.cleared() + 1);
            goSelect();
        } else if (bFree.contains(x, y)) {
            campaign = false;
            goSelect();
        } else if (bSound.contains(x, y)) {
            sfx.enabled = !sfx.enabled;
            save.setSound(sfx.enabled);
            sfx.play(Sfx.SELECT);
        }
    }

    private void goSelect() {
        sfx.play(Sfx.SELECT);
        screen = S_SELECT;
        int unlocked = Roster.unlockedCount(save.cleared());
        for (int i = team.size() - 1; i >= 0; i--) if (team.get(i) >= unlocked) team.remove(i);
        focus = team.isEmpty() ? 0 : team.get(0);
    }

    private RectF selRect(int i) {
        float x = 20 + (i % 4) * 175, y = 110 + (i / 4) * 244;
        return new RectF(x, y, x + 160, y + 230);
    }

    private void tapSelect(float x, float y) {
        if (bBack.contains(x, y)) { onBack(); return; }
        int unlocked = Roster.unlockedCount(save.cleared());
        for (int i = 0; i < Roster.ALL.length; i++) {
            if (selRect(i).contains(x, y)) {
                if (i >= unlocked) { sfx.play(Sfx.GUARD); focus = i; return; }
                sfx.play(Sfx.SELECT);
                focus = i;
                int at = team.indexOf(i);
                if (at >= 0) team.remove(at);
                else if (team.size() < 3) team.add(i);
                return;
            }
        }
        if (team.size() == 3 && bFight.contains(x, y)) startBattle();
    }

    private void tapBattle(float x, float y) {
        if (phase == PH_RESULT) {
            if (bRes1.contains(x, y)) {
                sfx.play(Sfx.SELECT);
                if (won && campaign && stage < Roster.STAGES) stage++;
                startBattle();
            } else if (bRes2.contains(x, y)) {
                sfx.play(Sfx.SELECT);
                screen = S_MENU;
            }
            return;
        }
        if (bQuit.contains(x, y)) {
            if (time < quitArm) { screen = S_MENU; sfx.play(Sfx.SELECT); }
            else quitArm = time + 2f;
            return;
        }
        if (phase != PH_INPUT) return;
        for (int i = 0; i < 4; i++) {
            if (cardRect(i).contains(x, y)) {
                int ci = uiHand[i];
                if (b.card(0, ci).cost <= uiKi[0]) playerAct(ci);
                else { sfx.play(Sfx.GUARD); addFloater("Ki insuficiente", 360, 840, 0xFFFFCA28, 34); }
                return;
            }
        }
        int[] sw = swapMembers();
        for (int j = 0; j < sw.length; j++) {
            if (swapRect(j).contains(x, y) && b.team[0][sw[j]].alive()) {
                playerAct(Battle.swapTo(sw[j]));
                return;
            }
        }
    }

    // ======================================================================= combate: lógica de pantalla

    private void startBattle() {
        Fighter[] mine = new Fighter[3];
        for (int i = 0; i < 3; i++) mine[i] = Roster.ALL[team.get(i)];
        Fighter[] enemy;
        float mult;
        if (campaign) {
            enemy = Roster.stageTeam(stage);
            aiLevel = stage;
            mult = Roster.levelMult(stage);
            bgIdx = (stage - 1) % 5;
        } else {
            ArrayList<Integer> ids = new ArrayList<>();
            for (int i = 0; i < Roster.ALL.length; i++) ids.add(i);
            java.util.Collections.shuffle(ids, rnd);
            enemy = new Fighter[]{Roster.ALL[ids.get(0)], Roster.ALL[ids.get(1)], Roster.ALL[ids.get(2)]};
            aiLevel = 3;
            mult = Roster.levelMult(3);
            bgIdx = rnd.nextInt(5);
        }
        b = new Battle(mine, enemy, mult, System.nanoTime());
        for (int s = 0; s < 2; s++) {
            for (int i = 0; i < 3; i++) {
                hpTgt[s][i] = b.team[s][i].hp;
                hpShow[s][i] = b.team[s][i].hp;
            }
            dispActive[s] = b.active[s];
            ko[s] = false;
            holdGuard[s] = false;
            alpha[s] = 255;
            offX[s] = 0;
            hitUntil[s] = 0;
        }
        syncUi();
        queue.clear();
        cur = null;
        particles.clear();
        floaters.clear();
        banner = "";
        bannerT = 0;
        shake = 0;
        darken = 0;
        phase = PH_INTRO;
        introT = 0;
        screen = S_BATTLE;
        sfx.play(Sfx.SELECT);
    }

    private void syncUi() {
        uiKi[0] = b.ki[0];
        uiKi[1] = b.ki[1];
        System.arraycopy(b.hand[0], 0, uiHand, 0, 4);
    }

    private void playerAct(int act) {
        int ea = b.aiChoose(1, aiLevel);
        queue.clear();
        queue.addAll(b.turn(act, ea));
        cur = null;
        phase = PH_ANIM;
        sfx.play(Sfx.SELECT);
    }

    private void updateBattle(float dt) {
        // barras de vida suaves
        for (int s = 0; s < 2; s++) {
            for (int i = 0; i < 3; i++) {
                float d = hpTgt[s][i] - hpShow[s][i];
                float step = Math.max(Math.abs(d) * 6f * dt, 60f * dt);
                if (Math.abs(d) <= step) hpShow[s][i] = hpTgt[s][i];
                else hpShow[s][i] += Math.signum(d) * step;
            }
        }
        shake = Math.max(0, shake - 40f * dt);
        if (bannerT > 0) bannerT -= dt;

        switch (phase) {
            case PH_INTRO:
                introT += dt;
                if (introT > 1.8f) phase = PH_INPUT;
                break;
            case PH_ANIM:
                if (cur == null) {
                    cur = queue.poll();
                    if (cur == null) finishAnim();
                    else startEv(cur);
                } else {
                    evT += dt;
                    updateEv();
                    if (evT >= evDur) {
                        offX[0] = offX[1] = 0;
                        cur = null;
                    }
                }
                break;
            case PH_END:
                endT += dt;
                if (endT > 1.8f) enterResult();
                break;
            default:
                break;
        }
        if (cur == null) darken = Math.max(0, darken - 2f * dt);
    }

    private void finishAnim() {
        syncUi();
        holdGuard[0] = holdGuard[1] = false;
        darken = 0;
        if (b.winner >= 0) {
            phase = PH_END;
            endT = 0;
        } else {
            phase = PH_INPUT;
        }
    }

    private void enterResult() {
        phase = PH_RESULT;
        won = b.winner == 0;
        resultMsg = "";
        sfx.play(won ? Sfx.WIN : Sfx.LOSE);
        if (won && campaign) {
            int before = Roster.unlockedCount(save.cleared());
            if (stage > save.cleared()) save.setCleared(stage);
            int after = Roster.unlockedCount(save.cleared());
            if (after > before) {
                resultMsg = "¡Nuevo guerrero desbloqueado: " + Roster.ALL[after - 1].name + "!";
            } else if (stage >= Roster.STAGES) {
                resultMsg = "¡Campaña completada!";
            }
        }
    }

    private void startEv(Battle.Ev e) {
        evT = 0;
        hitDone = false;
        subDone = false;
        offX[0] = offX[1] = 0;
        boolean mine = e.actor == 0;
        bannerColor = mine ? 0xFF4FC3F7 : 0xFFFF8A65;
        switch (e.type) {
            case Battle.E_STRIKE: evDur = 0.95f; evHit = 0.28f; setBanner(e.text, evDur); break;
            case Battle.E_BLAST: evDur = 1.05f; evHit = 0.7f; setBanner(e.text, evDur); sfx.play(Sfx.BLAST); break;
            case Battle.E_ULTI:
                evDur = 2.1f; evHit = 1.05f;
                setBanner(e.text, evDur);
                sfx.play(Sfx.ULTI);
                break;
            case Battle.E_GUARD:
                evDur = 0.5f; setBanner(e.text, evDur); holdGuard[e.actor] = true; sfx.play(Sfx.GUARD);
                break;
            case Battle.E_HEAL: evDur = 0.95f; setBanner(e.text, evDur); sfx.play(Sfx.HEAL); break;
            case Battle.E_BUFF: evDur = 0.8f; setBanner(e.text, evDur); sfx.play(Sfx.BUFF); break;
            case Battle.E_CHARGE: evDur = 0.8f; setBanner(e.text, evDur); sfx.play(Sfx.BUFF); break;
            case Battle.E_SWAP:
                evDur = 0.65f;
                setBanner(e.auto ? "¡" + e.text + " entra!" : "¡Cambio: " + e.text + "!", evDur);
                sfx.play(Sfx.SELECT);
                break;
            default: // KO
                evDur = 1.0f;
                bannerColor = 0xFFFF5252;
                setBanner("¡K.O.!", evDur);
                ko[e.actor] = true;
                sfx.play(Sfx.KO);
                break;
        }
    }

    private void setBanner(String s, float d) {
        banner = s;
        bannerT = d;
        bannerTotal = d;
    }

    private static float ease(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    private void updateEv() {
        int a = cur.actor, t = 1 - a;
        float dir = a == 0 ? 1 : -1;
        switch (cur.type) {
            case Battle.E_STRIKE: {
                float dist = (PX[t] - PX[a]) - dir * 115f;
                float o;
                if (evT < 0.25f) o = ease(evT / 0.25f) * dist;
                else if (evT < 0.5f) o = dist;
                else if (evT < 0.85f) o = dist * (1 - ease((evT - 0.5f) / 0.35f));
                else o = 0;
                offX[a] = o;
                if (!hitDone && evT >= evHit) applyHit();
                break;
            }
            case Battle.E_BLAST:
                if (!hitDone && evT >= evHit) applyHit();
                break;
            case Battle.E_ULTI:
                darken = Math.min(0.55f, evT * 1.2f);
                if (evT > 0.1f && evT < 0.8f && rnd.nextInt(2) == 0) {
                    Fighter f = b.team[a][dispActive[a]].f;
                    spawn(PX[a] + dir * 60 + rnd.nextInt(120) - 60, GROUND - 40 - rnd.nextInt(60),
                            (rnd.nextFloat() - 0.5f) * 40, -120 - rnd.nextInt(120), 0.7f, 8 + rnd.nextInt(8),
                            f.aura);
                }
                if (!hitDone && evT >= evHit) applyHit();
                break;
            case Battle.E_HEAL:
                if (!hitDone && evT >= 0.4f) {
                    hitDone = true;
                    hpTgt[a][cur.tIdx] = cur.hpAfter;
                    addFloater("+" + cur.value, PX[a], GROUND - 230, 0xFF69F0AE, 52);
                    for (int i = 0; i < 20; i++) {
                        spawn(PX[a] + rnd.nextInt(100) - 50, GROUND - rnd.nextInt(180), 0,
                                -80 - rnd.nextInt(80), 0.9f, 9, 0xFF69F0AE);
                    }
                }
                break;
            case Battle.E_BUFF:
                if (!subDone && evT >= 0.3f) {
                    subDone = true;
                    addFloater("¡ATQ +40%!", PX[a], GROUND - 240, 0xFFFFD54F, 42);
                }
                break;
            case Battle.E_CHARGE:
                if (!subDone && evT >= 0.3f) {
                    subDone = true;
                    addFloater("+" + cur.value + " KI", PX[a], GROUND - 240, 0xFF4FC3F7, 46);
                }
                break;
            case Battle.E_SWAP: {
                if (evT < 0.3f) {
                    alpha[a] = Math.round(255 * (1 - evT / 0.3f));
                } else {
                    if (!subDone) {
                        subDone = true;
                        dispActive[a] = cur.aIdx;
                        ko[a] = false;
                        hitUntil[a] = 0;
                    }
                    alpha[a] = Math.min(255, Math.round(255 * ((evT - 0.3f) / 0.3f)));
                }
                break;
            }
            default:
                break;
        }
        if (cur.type != Battle.E_SWAP && evT >= evDur - 0.001f) alpha[a] = 255;
        if (cur.type == Battle.E_SWAP && evT >= evDur - 0.001f) alpha[a] = 255;
    }

    private void applyHit() {
        hitDone = true;
        int a = cur.actor, t = 1 - a;
        hpTgt[t][cur.tIdx] = cur.hpAfter;
        hitUntil[t] = time + 0.4f;
        float tx = PX[t], ty = GROUND - 215;
        int col = cur.crit ? 0xFFFFD600 : (cur.guarded ? 0xFFB0BEC5 : 0xFFFFFFFF);
        addFloater(String.valueOf(cur.value), tx, ty, col, cur.crit ? 66 : 50);
        float yy = ty - 52;
        if (cur.crit) { addFloater("¡CRÍTICO!", tx, yy, 0xFFFFD600, 34); yy -= 38; }
        if (cur.mult > 0) { addFloater("¡VENTAJA!", tx, yy, 0xFF69F0AE, 30); yy -= 34; }
        else if (cur.mult < 0) { addFloater("RESISTE", tx, yy, 0xFFB0BEC5, 28); yy -= 34; }
        if (cur.guarded) addFloater("GUARDIA", tx, yy, 0xFF4FC3F7, 28);
        Fighter af = b.team[a][dispActive[a]].f;
        int n = cur.type == Battle.E_ULTI ? 46 : 20;
        for (int i = 0; i < n; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            float sp = 120 + rnd.nextInt(380);
            spawn(tx, GROUND - 120, (float) Math.cos(ang) * sp, (float) Math.sin(ang) * sp - 80,
                    0.5f + rnd.nextFloat() * 0.4f, 6 + rnd.nextInt(8), rnd.nextBoolean() ? af.aura : 0xFFFFFFFF);
        }
        shake = Math.max(shake, cur.type == Battle.E_ULTI ? 24 : (cur.type == Battle.E_BLAST ? 13 : 9));
        sfx.play(Sfx.HIT);
    }

    // ======================================================================= partículas

    private void spawn(float x, float y, float vx, float vy, float life, float size, int color) {
        if (particles.size() > 400) return;
        Particle q = new Particle();
        q.x = x; q.y = y; q.vx = vx; q.vy = vy; q.life = life; q.max = life; q.size = size; q.color = color;
        particles.add(q);
    }

    private void addFloater(String s, float x, float y, int color, float size) {
        Floater f = new Floater();
        f.s = s; f.x = x; f.y = y; f.life = 1.3f; f.max = 1.3f; f.size = size; f.color = color;
        floaters.add(f);
    }

    private void updateParticles(float dt) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle q = particles.get(i);
            q.life -= dt;
            if (q.life <= 0) { particles.remove(i); continue; }
            q.x += q.vx * dt;
            q.y += q.vy * dt;
            q.vy += 260 * dt;
        }
        for (int i = floaters.size() - 1; i >= 0; i--) {
            Floater f = floaters.get(i);
            f.life -= dt;
            if (f.life <= 0) { floaters.remove(i); continue; }
            f.y -= 38 * dt;
        }
    }

    // ======================================================================= helpers de dibujo

    private void text(Canvas c, String s, float x, float y, float size, int color, Paint.Align al) {
        p.setStyle(Paint.Style.FILL);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(size);
        p.setTextAlign(al);
        p.setColor(0xB0000000);
        c.drawText(s, x + 2, y + 2, p);
        p.setColor(color);
        c.drawText(s, x, y, p);
    }

    private void fitText(Canvas c, String s, float x, float y, float size, float maxW, int color) {
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(size);
        float w = p.measureText(s);
        if (w > maxW) size = size * maxW / w;
        text(c, s, x, y, size, color, Paint.Align.CENTER);
    }

    private void panel(Canvas c, RectF r, int fill, int stroke, float rad) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(fill);
        c.drawRoundRect(r, rad, rad, p);
        if (stroke != 0) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3);
            p.setColor(stroke);
            c.drawRoundRect(r, rad, rad, p);
        }
    }

    private void button(Canvas c, RectF r, String label, int color, boolean enabled) {
        panel(c, r, enabled ? color : 0xFF444A52, enabled ? 0xFFFFFFFF : 0xFF777E87, 18);
        fitText(c, label, r.centerX(), r.centerY() + 11, 32, r.width() - 24, enabled ? 0xFFFFFFFF : 0xFFAAAAAA);
    }

    private void bar(Canvas c, float x, float y, float w, float h, float ratio, int color, boolean fromRight) {
        ratio = Math.max(0, Math.min(1, ratio));
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF1A1F26);
        rf.set(x, y, x + w, y + h);
        c.drawRoundRect(rf, h / 2, h / 2, p);
        if (ratio > 0) {
            p.setColor(color);
            if (fromRight) rf.set(x + w * (1 - ratio), y, x + w, y + h);
            else rf.set(x, y, x + w * ratio, y + h);
            c.drawRoundRect(rf, h / 2, h / 2, p);
        }
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2);
        p.setColor(0xFFFFFFFF);
        rf.set(x, y, x + w, y + h);
        c.drawRoundRect(rf, h / 2, h / 2, p);
    }

    private static int hpColor(float r) {
        return r > 0.5f ? 0xFF66BB6A : (r > 0.25f ? 0xFFFFCA28 : 0xFFEF5350);
    }

    private void kiPips(Canvas c, float x, float y, int n, boolean rightToLeft, float r) {
        for (int i = 0; i < Battle.MAX_KI; i++) {
            float cx = rightToLeft ? x - i * (r * 2.7f) : x + i * (r * 2.7f);
            p.setStyle(Paint.Style.FILL);
            p.setColor(i < n ? 0xFF4FC3F7 : 0xFF263238);
            c.drawCircle(cx, y, r, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2);
            p.setColor(i < n ? 0xFFE1F5FE : 0xFF607D8B);
            c.drawCircle(cx, y, r, p);
        }
    }

    private Bitmap bg(int i) {
        if (bgs[i] == null) bgs[i] = makeBg(i);
        return bgs[i];
    }

    private void drawBg(Canvas c, int i) {
        c.drawBitmap(bg(i), 0, 0, null);
        // nubes
        p.setStyle(Paint.Style.FILL);
        p.setColor(i == 2 ? 0x22FFFFFF : 0x55FFFFFF);
        for (int k = 0; k < 4; k++) {
            float x = ((time * (8 + k * 3) + k * 380) % (W + 400)) - 200;
            float y = 70 + k * 46;
            rf.set(x - 90, y - 18, x + 90, y + 18);
            c.drawOval(rf, p);
            rf.set(x - 50, y - 34, x + 60, y + 6);
            c.drawOval(rf, p);
        }
    }

    private Bitmap makeBg(int pal) {
        final int[][] pl = {
            {0xFF3A8DDE, 0xFFBFE6FF, 0xFFFFF1A8, 0xFF5B7FA6, 0xFF3E5D80, 0xFF8A6B4B, 0xFF4E3B2A},
            {0xFF3B2A6B, 0xFFFF8F5A, 0xFFFFD27A, 0xFF7A3D6B, 0xFF4F2756, 0xFF6B4A44, 0xFF35232A},
            {0xFF050B2A, 0xFF2B3A7A, 0xFFE8EEFF, 0xFF1D2B5B, 0xFF121B3F, 0xFF3C4366, 0xFF1C2038},
            {0xFF5A1F1F, 0xFFE0793A, 0xFFFFC36B, 0xFF6E2E22, 0xFF45201A, 0xFF7A4B2E, 0xFF3A2216},
            {0xFF1F7A8C, 0xFFCFF3E6, 0xFFFFF7B0, 0xFF2F8F83, 0xFF1F6B63, 0xFFCFC7B0, 0xFF8E8670},
        };
        int[] k = pl[pal];
        Bitmap bm = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        Paint q = new Paint(Paint.ANTI_ALIAS_FLAG);
        q.setShader(new LinearGradient(0, 0, 0, GROUND, k[0], k[1], Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W, H, q);
        q.setShader(null);
        Random r = new Random(100 + pal);
        if (pal == 2) {
            q.setColor(0xFFFFFFFF);
            for (int i = 0; i < 90; i++) c.drawCircle(r.nextInt(W), r.nextInt(300), 0.8f + r.nextFloat() * 1.6f, q);
        }
        q.setColor(k[2]);
        c.drawCircle(560, 150, pal == 2 ? 52 : 70, q);
        q.setColor((k[2] & 0xFFFFFF) | 0x33000000);
        c.drawCircle(560, 150, 110, q);
        for (int layer = 0; layer < 2; layer++) {
            q.setColor(k[3 + layer]);
            Path mt = new Path();
            mt.moveTo(0, GROUND);
            float x = 0;
            float base = GROUND - 150 + layer * 40;
            while (x < W + 120) {
                float peak = base - 40 - r.nextInt(130);
                mt.lineTo(x + 60 + r.nextInt(50), peak);
                x += 130 + r.nextInt(90);
                mt.lineTo(x, base + 25 + r.nextInt(25));
            }
            mt.lineTo(W, GROUND);
            mt.close();
            c.drawPath(mt, q);
        }
        q.setShader(new LinearGradient(0, GROUND - 14, 0, H, k[5], k[6], Shader.TileMode.CLAMP));
        c.drawRect(0, GROUND - 14, W, H, q);
        q.setShader(null);
        q.setStyle(Paint.Style.STROKE);
        q.setStrokeWidth(2);
        q.setColor(0x33000000);
        if (pal == 4) {
            for (int i = -2; i < 14; i++) c.drawLine(i * 120, H, 140 + i * 90, GROUND, q);
            for (int j = 1; j < 5; j++) c.drawLine(0, GROUND + j * j * 12, W, GROUND + j * j * 12, q);
        } else {
            for (int i = 0; i < 14; i++) {
                float sx = r.nextInt(W), sy = GROUND + 6 + r.nextInt(H - (int) GROUND - 10);
                c.drawLine(sx, sy, sx + 40 + r.nextInt(60), sy + 8 + r.nextInt(14), q);
            }
        }
        return bm;
    }

    // ======================================================================= MENÚ

    private void drawMenu(Canvas c) {
        drawBg(c, 0);
        FighterArt.draw(c, p, Roster.ALL[0], 190, 760, 3.0f, true, FighterArt.P_IDLE, time, false, 255);
        FighterArt.draw(c, p, Roster.ALL[6], 530, 760, 3.0f, false, FighterArt.P_IDLE, time + 1f, false, 255);

        float pulse = 1f + 0.03f * (float) Math.sin(time * 3f);
        c.save();
        c.scale(pulse, pulse, 360, 200);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(110);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(12);
        p.setColor(0xFF1B1B3A);
        c.drawText("KI ARENA", 360, 215, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFFFA000);
        c.drawText("KI ARENA", 360, 215, p);
        p.setColor(0xFFFFE082);
        c.drawText("KI ARENA", 360, 207, p);
        c.restore();
        text(c, "Combates por cartas 3 contra 3", 360, 275, 30, 0xFFFFFFFF, Paint.Align.CENTER);

        int cl = save.cleared();
        String sub = cl >= Roster.STAGES ? "CAMPAÑA  (¡completada!)" : "CAMPAÑA  (combate " + (cl + 1) + "/" + Roster.STAGES + ")";
        button(c, bCampaign, sub, 0xFFE65100, true);
        button(c, bFree, "COMBATE LIBRE", 0xFF1565C0, true);
        button(c, bSound, sfx.enabled ? "Sonido: SÍ" : "Sonido: NO", 0xFF455A64, true);
        text(c, "Guerreros desbloqueados: " + Roster.unlockedCount(cl) + "/" + Roster.ALL.length,
                360, 1170, 26, 0xFFFFFFFF, Paint.Align.CENTER);
    }

    // ======================================================================= SELECCIÓN

    private void drawSelect(Canvas c) {
        drawBg(c, 2);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x88000000);
        c.drawRect(0, 0, W, H, p);
        button(c, bBack, "‹ Atrás", 0xFF455A64, true);
        String title = campaign ? "CAMPAÑA · Combate " + stage + "/" + Roster.STAGES : "COMBATE LIBRE";
        text(c, title, 440, 56, 32, 0xFFFFD54F, Paint.Align.CENTER);
        text(c, "Elige 3 guerreros (el primero sale a pelear)", 360, 98, 24, 0xFFFFFFFF, Paint.Align.CENTER);

        int unlocked = Roster.unlockedCount(save.cleared());
        for (int i = 0; i < Roster.ALL.length; i++) {
            Fighter f = Roster.ALL[i];
            RectF r = selRect(i);
            float x = r.left, y = r.top;
            boolean locked = i >= unlocked;
            int at = team.indexOf(i);
            panel(c, r, locked ? 0xFF20242B : 0xFF2B3340, at >= 0 ? 0xFFFFD54F : (i == focus ? 0xFFFFFFFF : 0xFF566170), 16);
            if (locked) {
                text(c, "?", x + 80, y + 140, 90, 0xFF59616D, Paint.Align.CENTER);
                text(c, "BLOQUEADO", x + 80, y + 212, 18, 0xFF8892A0, Paint.Align.CENTER);
            } else {
                p.setStyle(Paint.Style.FILL);
                p.setColor(shadeA(Fighter.TYPE_COLORS[f.type], 0.35f));
                rf.set(x + 8, y + 8, x + 152, y + 150);
                c.drawRoundRect(rf, 12, 12, p);
                c.save();
                c.clipRect(x + 8, y + 8, x + 152, y + 150);
                FighterArt.portrait(c, p, f, x + 80, y + 80, 100, false);
                c.restore();
                fitText(c, f.name, x + 80, y + 184, 26, 148, 0xFFFFFFFF);
                p.setStyle(Paint.Style.FILL);
                p.setColor(Fighter.TYPE_COLORS[f.type]);
                rf.set(x + 25, y + 196, x + 135, y + 218);
                c.drawRoundRect(rf, 11, 11, p);
                text(c, Fighter.TYPE_NAMES[f.type], x + 80, y + 213, 15, 0xFFFFFFFF, Paint.Align.CENTER);
                if (at >= 0) {
                    p.setColor(0xFFFFD54F);
                    c.drawCircle(x + 24, y + 24, 17, p);
                    text(c, String.valueOf(at + 1), x + 24, y + 34, 24, 0xFF000000, Paint.Align.CENTER);
                }
            }
        }

        // panel de detalle
        RectF d = new RectF(20, 600, 700, 930);
        panel(c, d, 0xDD1B2129, 0xFF566170, 16);
        if (focus < unlocked) {
            Fighter f = Roster.ALL[focus];
            text(c, f.name, 40, 648, 38, 0xFFFFFFFF, Paint.Align.LEFT);
            text(c, f.title, 40, 680, 22, 0xFFBDBDBD, Paint.Align.LEFT);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Fighter.TYPE_COLORS[f.type]);
            rf.set(556, 624, 684, 652);
            c.drawRoundRect(rf, 14, 14, p);
            text(c, Fighter.TYPE_NAMES[f.type], 620, 645, 19, 0xFFFFFFFF, Paint.Align.CENTER);
            statRow(c, "VIDA", f.hp / 1300f, 744, 0xFF66BB6A);
            statRow(c, "ATQ", f.atk / 135f, 780, 0xFFEF5350);
            statRow(c, "DEF", f.def / 110f, 816, 0xFF42A5F5);
            statRow(c, "VEL", f.spd / 130f, 852, 0xFFFFCA28);
            text(c, "MOVIMIENTOS", 370, 722, 20, 0xFFFFD54F, Paint.Align.LEFT);
            for (int i = 0; i < 6; i++) {
                Card cd = f.deck[i];
                float yy = 756 + i * 30;
                p.setStyle(Paint.Style.FILL);
                p.setColor(0xFF263238);
                c.drawCircle(384, yy - 7, 12, p);
                text(c, String.valueOf(cd.cost), 384, yy - 1, 17, 0xFF4FC3F7, Paint.Align.CENTER);
                fitLeft(c, cd.name + " — " + cd.desc(), 406, yy, 18, 285, 0xFFFFFFFF);
            }
        } else {
            text(c, "???", 360, 760, 60, 0xFF59616D, Paint.Align.CENTER);
            text(c, "Gana combates de la", 360, 820, 24, 0xFFBDBDBD, Paint.Align.CENTER);
            text(c, "campaña para desbloquearlo", 360, 852, 24, 0xFFBDBDBD, Paint.Align.CENTER);
        }

        // equipo elegido
        text(c, "TU EQUIPO", 20, 968, 24, 0xFFFFD54F, Paint.Align.LEFT);
        for (int i = 0; i < 3; i++) {
            float x = 20 + i * 146;
            rf.set(x, 985, x + 130, 1115);
            panel(c, rf, 0xFF2B3340, i < team.size() ? 0xFFFFD54F : 0xFF566170, 12);
            if (i < team.size()) {
                c.save();
                c.clipRect(rf);
                FighterArt.portrait(c, p, Roster.ALL[team.get(i)], x + 65, 1048, 86, false);
                c.restore();
            } else {
                text(c, String.valueOf(i + 1), x + 65, 1066, 48, 0xFF59616D, Paint.Align.CENTER);
            }
        }
        button(c, bFight, "¡A PELEAR!", 0xFFE65100, team.size() == 3);
    }

    private static int shadeA(int color, float k) { return FighterArt.shade(color, k); }

    private void fitLeft(Canvas c, String s, float x, float y, float size, float maxW, int color) {
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(size);
        float w = p.measureText(s);
        if (w > maxW) size = size * maxW / w;
        text(c, s, x, y, size, color, Paint.Align.LEFT);
    }

    private void statRow(Canvas c, String label, float ratio, float y, int color) {
        text(c, label, 40, y, 20, 0xFFFFFFFF, Paint.Align.LEFT);
        bar(c, 105, y - 17, 230, 18, ratio, color, false);
    }

    // ======================================================================= COMBATE: dibujo

    private RectF cardRect(int i) {
        float x = 20 + (i % 2) * 350, y = 896 + (i / 2) * 152;
        return new RectF(x, y, x + 340, y + 142);
    }

    private RectF swapRect(int j) {
        float x = 20 + j * 350;
        return new RectF(x, 1204, x + 340, 1270);
    }

    private int[] swapMembers() {
        int[] r = new int[2];
        int n = 0;
        for (int i = 0; i < 3; i++) if (i != dispActive[0] && n < 2) r[n++] = i;
        return r;
    }

    private int poseOf(int s) {
        if (ko[s]) return FighterArt.P_KO;
        if (hitUntil[s] > time) return FighterArt.P_HIT;
        if (cur != null && cur.actor == s) {
            switch (cur.type) {
                case Battle.E_STRIKE: return (evT > 0.12f && evT < 0.6f) ? FighterArt.P_PUNCH : FighterArt.P_IDLE;
                case Battle.E_BLAST: return evT < 0.95f ? FighterArt.P_BLAST : FighterArt.P_IDLE;
                case Battle.E_ULTI: return evT < 0.8f ? FighterArt.P_ULTI : (evT < 1.6f ? FighterArt.P_BLAST : FighterArt.P_IDLE);
                case Battle.E_GUARD: return FighterArt.P_GUARD;
                case Battle.E_HEAL: return FighterArt.P_HEAL;
                case Battle.E_BUFF:
                case Battle.E_CHARGE: return FighterArt.P_CHARGE;
                default: break;
            }
        }
        if (holdGuard[s]) return FighterArt.P_GUARD;
        return FighterArt.P_IDLE;
    }

    private void drawBattle(Canvas c) {
        c.save();
        if (shake > 0) c.translate((rnd.nextFloat() - 0.5f) * shake, (rnd.nextFloat() - 0.5f) * shake);
        drawBg(c, bgIdx);
        if (darken > 0) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(((int) (darken * 255) << 24));
            c.drawRect(-30, -30, W + 30, H + 30, p);
        }
        for (int s = 1; s >= 0; s--) {
            Battle.Unit u = b.team[s][dispActive[s]];
            float x = PX[s] + offX[s];
            boolean buffed = u.buff > 0 && !ko[s];
            FighterArt.draw(c, p, u.f, x, GROUND, FSCALE, s == 0, poseOf(s), time + s * 0.7f, buffed, alpha[s]);
        }
        drawEffects(c);
        c.restore();

        drawHud(c);
        drawHand(c);

        // anuncio del movimiento
        if (bannerT > 0 && phase != PH_INTRO) {
            float k = Math.min(1f, (bannerTotal - bannerT) / 0.12f);
            float sz = 40 + 14 * (1f - k);
            fitText(c, banner, 360, 300, sz, 680, bannerColor);
        }
        // números flotantes
        for (Floater f : floaters) {
            int a = Math.min(255, (int) (255 * f.life / 0.5f));
            int col = (f.color & 0xFFFFFF) | (a << 24);
            text(c, f.s, f.x, f.y, f.size, col, Paint.Align.CENTER);
        }
        // partículas
        p.setStyle(Paint.Style.FILL);
        for (Particle q : particles) {
            int a = Math.min(255, (int) (255 * q.life / q.max));
            p.setColor((q.color & 0xFFFFFF) | (a << 24));
            c.drawCircle(q.x, q.y, q.size * (0.4f + 0.6f * q.life / q.max), p);
        }

        if (phase == PH_INTRO) drawIntro(c);
        if (phase == PH_END) {
            float k = Math.min(1f, endT / 0.4f);
            text(c, b.winner == 0 ? "¡VICTORIA!" : "DERROTA", 360, 420, 50 + 34 * k,
                    b.winner == 0 ? 0xFFFFD54F : 0xFFEF5350, Paint.Align.CENTER);
        }
        if (phase == PH_RESULT) drawResult(c);
        if (time < quitArm) text(c, "Toca la X otra vez para salir", 360, 224, 24, 0xFFFFCA28, Paint.Align.CENTER);
    }

    private void drawEffects(Canvas c) {
        if (cur == null) return;
        int a = cur.actor, t = 1 - a;
        float dir = a == 0 ? 1 : -1;
        Fighter f = b.team[a][dispActive[a]].f;
        int rgb = f.aura & 0xFFFFFF;
        float hx = PX[a] + offX[a] + dir * 60, hy = GROUND - 125;
        float tx = PX[t] - dir * 25, ty = GROUND - 112;
        p.setStyle(Paint.Style.FILL);
        if (cur.type == Battle.E_BLAST) {
            float r;
            float bx, by;
            if (evT < 0.25f) {
                r = 7 + 20 * (evT / 0.25f);
                bx = hx; by = hy;
            } else if (evT < 0.7f) {
                float k = (evT - 0.25f) / 0.45f;
                r = 27;
                bx = hx + (tx - hx) * k;
                by = hy + (ty - hy) * k;
            } else {
                return;
            }
            orb(c, bx, by, r, rgb);
        } else if (cur.type == Battle.E_ULTI) {
            if (evT < 0.8f) {
                float k = ease(evT / 0.8f);
                orb(c, hx, hy, 10 + 58 * k, rgb);
            } else if (evT < 1.6f) {
                float k = (evT - 0.8f) / 0.8f;
                float w = 70 * (1f - 0.6f * k) * (0.9f + 0.1f * (float) Math.sin(time * 40f));
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeCap(Paint.Cap.ROUND);
                p.setStrokeWidth(w * 1.6f);
                p.setColor((70 << 24) | rgb);
                c.drawLine(hx, hy, tx, ty, p);
                p.setStrokeWidth(w);
                p.setColor((200 << 24) | rgb);
                c.drawLine(hx, hy, tx, ty, p);
                p.setStrokeWidth(w * 0.45f);
                p.setColor(0xFFFFFFFF);
                c.drawLine(hx, hy, tx, ty, p);
                p.setStyle(Paint.Style.FILL);
                orb(c, hx, hy, 50, rgb);
                orb(c, tx, ty, 58, rgb);
            }
        }
    }

    private void orb(Canvas c, float x, float y, float r, int rgb) {
        p.setStyle(Paint.Style.FILL);
        p.setColor((60 << 24) | rgb);
        c.drawCircle(x, y, r * 1.7f, p);
        p.setColor((130 << 24) | rgb);
        c.drawCircle(x, y, r * 1.25f, p);
        p.setColor((235 << 24) | rgb);
        c.drawCircle(x, y, r, p);
        p.setColor(0xFFFFFFFF);
        c.drawCircle(x, y, r * 0.55f, p);
    }

    private void drawHud(Canvas c) {
        for (int s = 0; s < 2; s++) {
            int ai = dispActive[s];
            Battle.Unit u = b.team[s][ai];
            float py = s == 1 ? 8 : 690;
            rf.set(10, py, 710, py + 148);
            panel(c, rf, 0x99000000, 0, 14);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Fighter.TYPE_COLORS[u.f.type]);
            c.drawCircle(34, py + 28, 11, p);
            text(c, u.f.name + (s == 1 ? "  (rival)" : ""), 54, py + 38, 28, 0xFFFFFFFF, Paint.Align.LEFT);
            if (u.buff > 0) text(c, "ATQ↑", 490, py + 38, 22, 0xFFFFD54F, Paint.Align.RIGHT);
            float ratio = hpShow[s][ai] / u.maxHp;
            bar(c, 20, py + 48, 470, 26, ratio, hpColor(ratio), false);
            text(c, Math.round(hpShow[s][ai]) + " / " + u.maxHp, 255, py + 69, 20, 0xFFFFFFFF, Paint.Align.CENTER);
            kiPips(c, 30, py + 108, uiKi[s], false, 8);
            for (int i = 0; i < 3; i++) {
                float ix = 515 + i * 64;
                float iy = py + 56;
                rf.set(ix, iy, ix + 56, iy + 50);
                panel(c, rf, 0xFF2B3340, i == ai ? 0xFFFFD54F : 0xFF566170, 8);
                c.save();
                c.clipRect(rf);
                FighterArt.portrait(c, p, b.team[s][i].f, ix + 28, iy + 26, 38, !b.team[s][i].alive() || hpTgt[s][i] <= 0);
                c.restore();
                float hr = hpShow[s][i] / b.team[s][i].maxHp;
                bar(c, ix + 2, iy + 42, 52, 6, hr, hpColor(hr), false);
            }
        }
        button(c, bQuit, "X", 0xFF8E2424, true);
        text(c, "Turno " + b.turn, 360, 192, 22, 0xFFFFFFFF, Paint.Align.CENTER);
    }

    private static int cardColor(int kind) {
        switch (kind) {
            case Card.STRIKE: return 0xFFD84315;
            case Card.BLAST: return 0xFF1565C0;
            case Card.GUARD: return 0xFF546E7A;
            case Card.HEAL: return 0xFF2E7D32;
            case Card.BUFF: return 0xFFF9A825;
            case Card.CHARGE: return 0xFF00838F;
            default: return 0xFF6A1B9A;
        }
    }

    private void drawHand(Canvas c) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xCC000000);
        c.drawRect(0, 850, W, H, p);
        kiPips(c, 34, 876, uiKi[0], false, 7);
        text(c, "KI " + uiKi[0] + "/" + Battle.MAX_KI, 200, 884, 22, 0xFF4FC3F7, Paint.Align.LEFT);

        boolean can = phase == PH_INPUT;
        // cartas
        Battle.Unit me = b.team[0][dispActive[0]];
        for (int i = 0; i < 4; i++) {
            Card cd = me.f.deck[uiHand[i]];
            RectF r = cardRect(i);
            float x = r.left, y = r.top;
            int col = cardColor(cd.kind);
            panel(c, r, col, cd.kind == Card.ULTI ? 0xFFFFD54F : 0xFFFFFFFF, 16);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0x33000000);
            rf.set(x + 6, y + 70, x + 334, y + 136);
            c.drawRoundRect(rf, 10, 10, p);
            p.setColor(0xFF10151B);
            c.drawCircle(x + 32, y + 32, 24, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3);
            p.setColor(0xFF4FC3F7);
            c.drawCircle(x + 32, y + 32, 24, p);
            text(c, String.valueOf(cd.cost), x + 32, y + 44, 34, 0xFFFFFFFF, Paint.Align.CENTER);
            String tag = cd.kind == Card.STRIKE ? "GOLPE" : cd.kind == Card.BLAST ? "KI" : cd.kind == Card.GUARD ? "DEFENSA"
                    : cd.kind == Card.HEAL ? "CURA" : cd.kind == Card.BUFF ? "MEJORA" : cd.kind == Card.CHARGE ? "CARGA" : "DEFINITIVO";
            text(c, tag, x + 200, y + 42, 22, 0xFFFFFFFF, Paint.Align.CENTER);
            fitText(c, cd.name, x + 170, y + 100, 28, 310, 0xFFFFFFFF);
            fitText(c, cd.desc(), x + 170, y + 126, 20, 310, 0xFFFFF3E0);
            if (!can || cd.cost > uiKi[0]) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(0x99000000);
                c.drawRoundRect(r, 16, 16, p);
            }
        }
        // cambios de guerrero
        int[] sw = swapMembers();
        for (int j = 0; j < 2; j++) {
            RectF r = swapRect(j);
            float x = r.left, y = r.top;
            Battle.Unit u = b.team[0][sw[j]];
            boolean dead = !u.alive();
            panel(c, r, 0xFF2B3340, 0xFF8895A7, 14);
            c.save();
            c.clipRect(x + 4, y + 4, x + 66, y + 62);
            FighterArt.portrait(c, p, u.f, x + 35, y + 34, 54, dead);
            c.restore();
            text(c, u.f.name, x + 76, y + 28, 24, 0xFFFFFFFF, Paint.Align.LEFT);
            float hr = hpShow[0][sw[j]] / u.maxHp;
            bar(c, x + 76, y + 40, 150, 12, hr, hpColor(hr), false);
            text(c, dead ? "K.O." : "CAMBIAR", x + 330, y + 42, 20, dead ? 0xFFEF5350 : 0xFFFFD54F, Paint.Align.RIGHT);
            if (!can || dead) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(0x77000000);
                c.drawRoundRect(r, 14, 14, p);
            }
        }
    }

    private void drawIntro(Canvas c) {
        float k = Math.min(1f, introT / 0.35f);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x99000000);
        c.drawRect(0, 500, W, 760, p);
        String head = campaign ? "COMBATE " + stage : "COMBATE LIBRE";
        text(c, head, 360 - (1f - k) * 500, 585, 60, 0xFFFFD54F, Paint.Align.CENTER);
        text(c, "VS", 360, 650, 48, 0xFFFFFFFF, Paint.Align.CENTER);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            if (i > 0) sb.append(" · ");
            sb.append(b.team[1][i].f.name);
        }
        fitText(c, sb.toString(), 360 + (1f - k) * 500, 714, 32, 680, 0xFFFF8A65);
    }

    private void drawResult(Canvas c) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xAA000000);
        c.drawRect(0, 0, W, H, p);
        rf.set(40, 380, 680, 880);
        panel(c, rf, 0xFF1B2129, won ? 0xFFFFD54F : 0xFFEF5350, 24);
        text(c, won ? "¡VICTORIA!" : "DERROTA", 360, 490, 64, won ? 0xFFFFD54F : 0xFFEF5350, Paint.Align.CENTER);
        text(c, "Turnos: " + b.turn, 360, 550, 30, 0xFFFFFFFF, Paint.Align.CENTER);
        int alive = 0;
        for (Battle.Unit u : b.team[0]) if (u.alive()) alive++;
        if (won) text(c, "Guerreros en pie: " + alive + "/3", 360, 595, 30, 0xFFFFFFFF, Paint.Align.CENTER);
        else fitText(c, "Cambia de equipo o aprovecha las ventajas de tipo", 360, 595, 24, 600, 0xFFBDBDBD);
        if (!resultMsg.isEmpty()) fitText(c, resultMsg, 360, 665, 30, 580, 0xFF69F0AE);
        boolean next = won && campaign && stage < Roster.STAGES;
        button(c, bRes1, next ? "Siguiente" : "Reintentar", next ? 0xFF2E7D32 : 0xFFE65100, true);
        button(c, bRes2, "Menú", 0xFF455A64, true);
    }
}
