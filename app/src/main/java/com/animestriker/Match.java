package com.animestriker;

import java.util.Random;

/**
 * Lógica pura del partido (sin Android): física del balón, jugadores, IA, reglas.
 * Coordenadas en metros: campo 100 x 60. El equipo 0 ataca hacia x = 100.
 */
public final class Match {

    public static final float L = 100f, W = 60f, CY = 30f;
    public static final float GOAL_HALF = 4.5f, GOAL_H = 2.6f, NET = 3.2f;
    static final float G = 26f;

    public static final int GK = 0, DF = 1, MF = 2, FW = 3;

    public static final int ST_KICKOFF = 0, ST_PLAY = 1, ST_GOAL = 2, ST_RESTART = 3,
            ST_CUTIN = 4, ST_HALF = 5, ST_END = 6;

    public static final int EV_KICK = 1, EV_PASS = 2, EV_WHISTLE = 3, EV_GOAL = 4, EV_SAVE = 5,
            EV_POST = 6, EV_SPECIAL = 7, EV_TACKLE = 8, EV_BOUNCE = 9, EV_OUT = 10,
            EV_HALF = 11, EV_END = 12, EV_SHOT = 13, EV_SPECIAL_GO = 14;

    public interface Listener { void onEvent(int type, Player p); }

    public static final class Player {
        public int team, idx, role, number;
        public float x, y, vx, vy, fx = 1, fy;
        public float hu, hy;
        public float stamina = 1f, stun, slide, kickCd, tackleCd, anim, flip = 1f, think, charge;
        public float spd, sht, pas, def, gkSkill;
        public float tx, ty;
        public boolean sprinting;
        public float actionT, slideDx, slideDy, diveT, diveDir, cheer, holdT;
        public int mode;
    }

    public final Player[][] pl = new Player[2][6];
    public final Teams.Def[] def = new Teams.Def[2];
    public final int[] score = new int[2];
    public final int[] shots = new int[2];
    public final float[] special = new float[2];
    public final float[] possTime = new float[2];

    public float bx = 50, by = 30, bz, bvx, bvy, bvz, spin;
    public boolean ballSpecial, gkRolled;
    public Player owner, lastTouch, lastScorer, specialShooter, taker;
    public int lastTouchTeam = -1, lastScoringTeam = -1, goalSide;
    public int state = ST_KICKOFF;
    public float stateT, cutinT;

    public int humanTeam;
    public Player ctrl;
    public float time, halfLen, goldenEnd;
    public int half = 1;
    public boolean golden, cup, shootout;
    public int penA, penB;
    public String banner = "";
    public float bannerT;
    public int bannerKind;
    public Listener listener;
    public final Random rnd = new Random();
    public float cpuLevel;
    private float cpuMul = 1f;

    // entrada del jugador humano
    public float inX, inY;
    public boolean inSprint, inShoot, prevShoot;
    public boolean pressA, pressB, pressC, pressD;

    private final Player[][] chasers = new Player[2][2];

    public Match(Teams.Def a, Teams.Def b, int human, float halfLen, float cpuLevel, boolean cup) {
        def[0] = a; def[1] = b;
        humanTeam = human; this.halfLen = halfLen; this.cpuLevel = cpuLevel; this.cup = cup;
        cpuMul = 0.93f + 0.10f * cpuLevel;
        int[] roles = {GK, DF, DF, MF, FW, FW};
        float[] hu = {0.03f, 0.22f, 0.22f, 0.42f, 0.62f, 0.62f};
        float[] hy = {30f, 16f, 44f, 30f, 19f, 41f};
        int[] nums = {1, 4, 5, 8, 9, 10};
        for (int t = 0; t < 2; t++) {
            Teams.Def d = def[t];
            for (int i = 0; i < 6; i++) {
                Player p = new Player();
                p.team = t; p.idx = i; p.role = roles[i]; p.number = nums[i];
                p.hu = hu[i]; p.hy = hy[i];
                float m = (t == human) ? 1f : cpuMul;
                p.spd = d.spd * m * (0.97f + rnd.nextFloat() * 0.06f);
                p.sht = d.sht * (0.97f + rnd.nextFloat() * 0.06f);
                p.pas = d.pas * (0.97f + rnd.nextFloat() * 0.06f);
                p.def = d.def * (t == human ? 1f : (0.95f + 0.08f * cpuLevel));
                p.gkSkill = d.gk;
                pl[t][i] = p;
            }
        }
        kickoff(0);
    }

    // ------------------------------------------------------------------ utilidades
    float dir(int t) { return t == 0 ? 1f : -1f; }
    float ownGoalX(int t) { return t == 0 ? 0f : L; }
    float oppGoalX(int t) { return t == 0 ? L : 0f; }
    static float dist(float ax, float ay, float bx, float by) {
        float dx = ax - bx, dy = ay - by; return (float) Math.sqrt(dx * dx + dy * dy);
    }
    static float clamp(float v, float a, float b) { return v < a ? a : (v > b ? b : v); }
    void ev(int t, Player p) { if (listener != null) listener.onEvent(t, p); }
    void say(String s, float t, int kind) { banner = s; bannerT = t; bannerKind = kind; }

    public float ballSpeed() { return (float) Math.sqrt(bvx * bvx + bvy * bvy); }

    public int minute() {
        float total = 2f * halfLen;
        int m = (int) (Math.min(time, total) / total * 90f);
        return m;
    }

    // ------------------------------------------------------------------ saques
    public void kickoff(int team) {
        state = ST_KICKOFF; stateT = 1.8f; owner = null; taker = null;
        ballSpecial = false; spin = 0;
        for (int t = 0; t < 2; t++) {
            float d = dir(t);
            for (Player p : pl[t]) {
                float u = p.role == GK ? 0.03f : Math.min(p.hu * 0.85f, 0.42f);
                p.x = t == 0 ? u * L : L - u * L;
                p.y = p.hy;
                p.vx = p.vy = 0; p.fx = d; p.fy = 0; p.flip = d;
                p.stun = p.slide = p.kickCd = p.tackleCd = p.diveT = p.cheer = p.charge = 0;
                p.holdT = 0;
            }
        }
        Player k = pl[team][4], k2 = pl[team][5];
        float d = dir(team);
        k.x = 50f - d * 0.9f; k.y = 30f;
        k2.x = 50f - d * 2.5f; k2.y = 34f;
        bx = 50; by = 30; bz = 0; bvx = bvy = bvz = 0;
        lastTouchTeam = team; lastTouch = k;
        taker = k;
        if (humanTeam >= 0) ctrl = (humanTeam == team) ? k : pl[humanTeam][4];
        say("KICK OFF", 1.6f, 0);
    }

    private void startRestart(int kind, int team, float x, float y) {
        // kind: 0 banda, 1 esquina, 2 saque de puerta
        state = ST_RESTART; stateT = 1.2f; owner = null; ballSpecial = false; spin = 0;
        bx = x; by = y; bz = 0; bvx = bvy = bvz = 0;
        Player best = null; float bd = 1e9f;
        if (kind == 2) best = pl[team][0];
        else for (Player p : pl[team]) {
            if (p.role == GK) continue;
            float d = dist(p.x, p.y, x, y);
            if (d < bd) { bd = d; best = p; }
        }
        taker = best;
        float inx = kind == 2 ? (team == 0 ? 5f : L - 5f) : x;
        float iny = kind == 2 ? CY : y;
        if (kind == 0) iny = y < 1 ? 0.8f : W - 0.8f;
        best.x = clamp(inx, 0.8f, L - 0.8f); best.y = clamp(iny, 0.8f, W - 0.8f);
        best.vx = best.vy = 0; best.stun = 0; best.slide = 0;
        float ex = 50f - best.x, ey = 30f - best.y; float l = (float) Math.sqrt(ex * ex + ey * ey);
        best.fx = ex / l; best.fy = ey / l;
        if (kind != 2) { bx = best.x + best.fx * 0.2f; by = best.y + best.fy * 0.2f; }
        else { bx = best.x + best.fx * 0.8f; by = best.y; }
        if (team == humanTeam && best.role != GK) ctrl = best;
        say(kind == 0 ? "SAQUE DE BANDA" : kind == 1 ? "TIRO DE ESQUINA" : "SAQUE DE PORTERIA", 1.2f, 0);
        ev(EV_OUT, best);
    }

    // ------------------------------------------------------------------ bucle
    public void update(float dt) {
        if (bannerT > 0) bannerT -= dt;
        for (int t = 0; t < 2; t++) for (Player p : pl[t]) {
            if (p.actionT > 0) p.actionT -= dt;
            if (p.cheer > 0) p.cheer -= dt;
        }
        switch (state) {
            case ST_KICKOFF:
                idle(dt);
                stateT -= dt;
                if (stateT <= 0) {
                    state = ST_PLAY; ev(EV_WHISTLE, null);
                    owner = taker; taker.kickCd = 0; take(taker);
                }
                break;
            case ST_PLAY:
                playStep(dt);
                clock(dt);
                break;
            case ST_GOAL:
                celebrate(dt);
                ballStep(dt, true);
                stateT -= dt;
                if (stateT <= 0) afterGoal();
                break;
            case ST_RESTART:
                playersStep(dt);
                stateT -= dt;
                if (stateT <= 0) {
                    state = ST_PLAY; ev(EV_WHISTLE, null);
                    owner = taker; take(taker);
                    if (taker.role == GK) { taker.holdT = 0.2f; }
                }
                break;
            case ST_CUTIN:
                cutinT += dt; stateT -= dt;
                if (stateT <= 0) fireSpecial();
                break;
            case ST_HALF:
                idle(dt);
                stateT -= dt;
                if (stateT <= 0) { half = 2; kickoff(1); }
                break;
            default: idle(dt);
        }
    }

    private void idle(float dt) {
        for (int t = 0; t < 2; t++) for (Player p : pl[t]) { p.vx = p.vy = 0; p.sprinting = false; }
    }

    private void clock(float dt) {
        time += dt;
        possTime[owner != null ? owner.team : (lastTouchTeam < 0 ? 0 : lastTouchTeam)] += dt;
        if (state != ST_PLAY) return;
        if (golden) {
            if (time >= goldenEnd) {
                shootout = true;
                float p0 = clamp(0.5f + (def[0].stars - def[1].stars) * 0.04f + (humanTeam == 0 ? 0.04f : 0f), 0.2f, 0.8f);
                boolean w0 = rnd.nextFloat() < p0;
                int hi = 4 + rnd.nextInt(2), lo = hi - 1 - rnd.nextInt(2);
                penA = w0 ? hi : lo; penB = w0 ? lo : hi;
                endMatch();
            }
        } else if (half == 1 && time >= halfLen) startHalftime();
        else if (half == 2 && time >= 2f * halfLen) regulationEnd();
    }

    private void startHalftime() {
        state = ST_HALF; stateT = 3.2f; ev(EV_WHISTLE, null); ev(EV_HALF, null);
        say("DESCANSO", 3.0f, 1);
    }

    private void regulationEnd() {
        if (cup && score[0] == score[1]) {
            golden = true; goldenEnd = time + 50f;
            say("GOL DE ORO", 2.2f, 1);
            kickoff(rnd.nextInt(2));
            bannerT = 2.2f; banner = "GOL DE ORO";
        } else endMatch();
    }

    private void endMatch() {
        state = ST_END; ev(EV_WHISTLE, null); ev(EV_END, null);
        say("FIN DEL PARTIDO", 99f, 1);
    }

    private void afterGoal() {
        if (golden) { endMatch(); return; }
        if (half == 1 && time >= halfLen) { startHalftime(); return; }
        if (half == 2 && time >= 2f * halfLen) { regulationEnd(); return; }
        kickoff(1 - lastScoringTeam);
    }

    private void playStep(float dt) {
        playersStep(dt);
        ballStep(dt, false);
        tryPickup();
        // carga del medidor especial
        for (int t = 0; t < 2; t++) {
            float k = (t == humanTeam) ? 1f : 0.8f;
            special[t] = Math.min(1f, special[t] + dt * 0.006f * k);
        }
        if (owner != null && owner.role != GK) {
            int t = owner.team;
            boolean forward = (owner.vx * dir(t)) > 3f && ((t == 0 && owner.x > 45) || (t == 1 && owner.x < 55));
            if (forward) special[t] = Math.min(1f, special[t] + dt * 0.028f * (t == humanTeam ? 1f : 0.8f));
        }
    }

    // ------------------------------------------------------------------ jugadores
    private void computeChasers() {
        for (int t = 0; t < 2; t++) {
            Player a = null, b = null; float da = 1e9f, db = 1e9f;
            float rx = owner != null ? owner.x : bx, ry = owner != null ? owner.y : by;
            for (Player p : pl[t]) {
                if (p.role == GK || p.stun > 0) continue;
                float d = dist(p.x, p.y, rx, ry);
                if (d < da) { b = a; db = da; a = p; da = d; }
                else if (d < db) { b = p; db = d; }
            }
            chasers[t][0] = a; chasers[t][1] = b;
        }
    }

    private void humanSelect(float dt) {
        if (humanTeam < 0) return;
        int h = humanTeam;
        if (owner != null && owner.team == h && owner.role != GK) { ctrl = owner; return; }
        Player best = null; float bd = 1e9f, cd = 1e9f;
        float rx = owner != null ? owner.x : bx, ry = owner != null ? owner.y : by;
        for (Player p : pl[h]) {
            if (p.role == GK) continue;
            float d = dist(p.x, p.y, rx, ry);
            if (d < bd) { bd = d; best = p; }
        }
        if (ctrl != null && ctrl.role != GK) cd = dist(ctrl.x, ctrl.y, rx, ry);
        if (ctrl == null || ctrl.role == GK || ctrl.stun > 0 || (best != ctrl && bd < cd - 3f)) ctrl = best;
        if (pressA && state == ST_PLAY) {
            Player alt = null; float ad = 1e9f;
            for (Player p : pl[h]) {
                if (p.role == GK || p == ctrl) continue;
                float d = dist(p.x, p.y, rx, ry);
                if (d < ad) { ad = d; alt = p; }
            }
            if (alt != null) ctrl = alt;
            pressA = false;
        }
    }

    private void playersStep(float dt) {
        computeChasers();
        humanSelect(dt);
        for (int t = 0; t < 2; t++) {
            for (Player p : pl[t]) {
                p.tackleCd -= dt; p.kickCd -= dt;
                if (p.diveT > 0) p.diveT -= dt;
                if (state == ST_RESTART && p == taker) {
                    p.vx = p.vy = 0; continue;
                }
                if (p.stun > 0) {
                    p.stun -= dt; p.vx *= 0.88f; p.vy *= 0.88f; integrate(p, dt); continue;
                }
                if (p.slide > 0) { slideStep(p, dt); continue; }
                if (p == ctrl && humanTeam >= 0) humanControl(p, dt);
                else if (p.role == GK) gkAI(p, dt);
                else outfieldAI(p, dt);
            }
        }
        for (int a = 0; a < 2; a++) for (Player p : pl[a]) {
            for (int b = a; b < 2; b++) for (Player q : pl[b]) {
                if (p == q || (a == b && q.idx < p.idx)) continue;
                if (p.slide > 0 || q.slide > 0) continue;
                float dx = q.x - p.x, dy = q.y - p.y, d2 = dx * dx + dy * dy;
                if (d2 < 1.21f && d2 > 1e-6f) {
                    float d = (float) Math.sqrt(d2), push = (1.1f - d) * 0.5f;
                    dx /= d; dy /= d;
                    p.x -= dx * push; p.y -= dy * push; q.x += dx * push; q.y += dy * push;
                }
            }
        }
        pressA = pressB = pressC = pressD = false;
        prevShoot = inShoot;
    }

    private void integrate(Player p, float dt) {
        p.x += p.vx * dt; p.y += p.vy * dt;
        float xmin = 0.4f, xmax = L - 0.4f;
        if (p.role == GK) { if (p.team == 0) xmax = 16f; else xmin = L - 16f; }
        p.x = clamp(p.x, xmin, xmax); p.y = clamp(p.y, 0.4f, W - 0.4f);
        float sp = (float) Math.sqrt(p.vx * p.vx + p.vy * p.vy);
        if (sp > 0.8f) {
            float nx = p.vx / sp, ny = p.vy / sp, k = Math.min(1f, dt * 14f);
            p.fx += (nx - p.fx) * k; p.fy += (ny - p.fy) * k;
            float l = (float) Math.sqrt(p.fx * p.fx + p.fy * p.fy);
            p.fx /= l; p.fy /= l;
            p.anim += dt * (4f + sp * 1.5f);
        } else p.anim += dt * 1.5f;
        float target = p.fx >= 0 ? 1f : -1f;
        p.flip += (target - p.flip) * Math.min(1f, dt * 16f);
    }

    private void move(Player p, float ix, float iy, boolean sprint, float dt, float mul) {
        float base = 7.3f * p.spd * mul;
        if (owner == p) base *= 0.93f;
        boolean moving = ix * ix + iy * iy > 0.01f;
        boolean sp = sprint && moving && p.stamina > 0.04f;
        p.sprinting = sp;
        if (sp) { base *= 1.38f; p.stamina -= dt * 0.15f; }
        else p.stamina = Math.min(1f, p.stamina + dt * (moving ? 0.05f : 0.16f));
        if (p.charge > 0.3f) base *= 0.8f;
        float k = Math.min(1f, dt * 8f);
        p.vx += (ix * base - p.vx) * k; p.vy += (iy * base - p.vy) * k;
        integrate(p, dt);
    }

    private void moveTo(Player p, float tx, float ty, boolean sprint, float dt, float mul) {
        float dx = tx - p.x, dy = ty - p.y, d = (float) Math.sqrt(dx * dx + dy * dy);
        if (d < 0.35f) { move(p, 0, 0, false, dt, mul); return; }
        float f = Math.min(1f, d / 3f);
        move(p, dx / d * f, dy / d * f, sprint, dt, mul);
    }

    private void slideStep(Player p, float dt) {
        p.slide -= dt;
        float sp = 12.5f * (p.slide / 0.5f + 0.15f);
        p.vx = p.slideDx * sp; p.vy = p.slideDy * sp;
        integrate(p, dt);
        if (owner != null && owner.team != p.team && dist(p.x, p.y, owner.x, owner.y) < 1.5f) {
            Player o = owner;
            float ch = clamp(0.55f + (p.def - o.spd) * 0.6f, 0.3f, 0.85f);
            if (rnd.nextFloat() < ch) { steal(p, o); }
            else { o.stun = 0.25f; }
            p.slide = 0; p.stun = 0.5f;
        }
        if (p.slide <= 0 && p.stun <= 0) p.stun = 0.45f;
    }

    private void humanControl(Player p, float dt) {
        float ix = inX, iy = inY, m = (float) Math.sqrt(ix * ix + iy * iy);
        if (m > 1f) { ix /= m; iy /= m; }
        if (m < 0.12f) { ix = 0; iy = 0; m = 0; }
        if (state == ST_KICKOFF) return;
        move(p, ix, iy, inSprint, dt, 1f);
        if (state != ST_PLAY && state != ST_RESTART) return;
        if (owner == p) {
            if (inShoot) p.charge = Math.min(1f, p.charge + dt / 0.85f);
            else if (prevShoot && p.charge > 0f) {
                doShoot(p, iy, Math.max(0.28f, p.charge), true);
                p.charge = 0;
            } else p.charge = 0;
            if (pressA) doPass(p, ix, iy, m, false);
            else if (pressC) doPass(p, ix, iy, m, true);
            else if (pressD && special[p.team] >= 1f && state == ST_PLAY) startSpecial(p);
        } else {
            p.charge = 0;
            if (state == ST_PLAY) {
                if (pressB) {
                    float d = owner != null ? dist(p.x, p.y, owner.x, owner.y) : 99f;
                    tackle(p, d > 2.4f);
                } else if (pressC) tackle(p, true);
            }
        }
    }

    // ------------------------------------------------------------------ pase / tiro
    private Player passTarget(Player p, float dx, float dy) {
        Player best = null; float bs = -1e9f;
        for (Player m : pl[p.team]) {
            if (m == p) continue;
            float vx = m.x - p.x, vy = m.y - p.y, d = (float) Math.sqrt(vx * vx + vy * vy);
            if (d < 2.5f) continue;
            float cos = (vx * dx + vy * dy) / d;
            if (cos < 0.5f) continue;
            float s = cos * 3f - d * 0.03f + (m.role == GK ? -2f : 0f);
            if (s > bs) { bs = s; best = m; }
        }
        return best;
    }

    private void doPass(Player p, float ix, float iy, float mag, boolean lob) {
        float dx = mag > 0.2f ? ix / mag : p.fx, dy = mag > 0.2f ? iy / mag : p.fy;
        Player m = passTarget(p, dx, dy);
        if (m != null) {
            passTo(p, m, lob);
            if (p.team == humanTeam && m.role != GK) ctrl = m;
        } else {
            kickBall(p, dx, dy, lob ? 17f : 15f, lob ? 7f : 0f, EV_PASS);
        }
        pressA = pressC = false;
    }

    private void passTo(Player p, Player m, boolean lob) {
        float d = dist(p.x, p.y, m.x, m.y);
        float T = lob ? 0.75f + d / 38f : Math.max(0.3f, d / clamp(d * 1.2f + 7f, 10f, 26f));
        float tx = m.x + m.vx * T * 0.85f, ty = m.y + m.vy * T * 0.85f;
        tx = clamp(tx, 1f, L - 1f); ty = clamp(ty, 1f, W - 1f);
        float dx = tx - p.x, dy = ty - p.y, dd = (float) Math.sqrt(dx * dx + dy * dy);
        if (dd < 0.01f) { dx = p.fx; dy = p.fy; dd = 1f; }
        dx /= dd; dy /= dd;
        float err = (float) rnd.nextGaussian() * (1.15f - p.pas) * 0.07f;
        float c = (float) Math.cos(err), s = (float) Math.sin(err);
        float rx = dx * c - dy * s, ry = dx * s + dy * c;
        float speed, vz = 0;
        if (lob) { speed = dd / T; vz = 0.5f * G * T; }
        else speed = clamp(dd * 1.2f + 7f, 10f, 26f);
        kickBall(p, rx, ry, speed, vz, EV_PASS);
    }

    private void doShoot(Player p, float aimInput, float charge, boolean human) {
        int t = p.team;
        float gx = oppGoalX(t);
        float aimY = human ? CY + clamp(aimInput * 5f, -4.1f, 4.1f)
                : CY + (rnd.nextBoolean() ? -1f : 1f) * (1.6f + rnd.nextFloat() * 2.4f);
        float dx = gx - p.x, dy = aimY - p.y, d = (float) Math.sqrt(dx * dx + dy * dy);
        dx /= d; dy /= d;
        float sigma = 0.035f + (1.15f - p.sht) * 0.25f + Math.max(0f, charge - 0.85f) * 0.12f + d * 0.0012f;
        if (!human) sigma *= (1.25f - 0.25f * cpuLevel);
        float err = (float) rnd.nextGaussian() * sigma;
        float c = (float) Math.cos(err), s = (float) Math.sin(err);
        float rx = dx * c - dy * s, ry = dx * s + dy * c;
        float speed = 15f + 20f * charge * (0.9f + 0.1f * p.sht);
        float vz = 0.8f + 6.2f * charge * (0.4f + rnd.nextFloat() * 0.6f);
        kickBall(p, rx, ry, speed, vz, EV_SHOT);
        shots[t]++;
    }

    private void kickBall(Player p, float dx, float dy, float speed, float vz, int evType) {
        owner = null;
        bx = p.x + dx * 0.9f; by = p.y + dy * 0.9f; bz = Math.max(bz, 0.25f);
        bvx = dx * speed; bvy = dy * speed; bvz = vz;
        lastTouch = p; lastTouchTeam = p.team; p.kickCd = 0.4f; p.actionT = 0.3f;
        spin = 0; ballSpecial = false; gkRolled = false;
        ev(evType, p);
    }

    private void startSpecial(Player p) {
        special[p.team] = 0f;
        state = ST_CUTIN; stateT = 1.35f; cutinT = 0f; specialShooter = p; p.charge = 0;
        say(def[p.team].special, 1.3f, 2);
        ev(EV_SPECIAL, p);
    }

    private void fireSpecial() {
        Player p = specialShooter;
        state = ST_PLAY;
        if (owner != p) return;
        float gx = oppGoalX(p.team);
        float aimInputY = (p.team == humanTeam) ? inY : (rnd.nextBoolean() ? 0.8f : -0.8f);
        float aimY = CY + clamp(aimInputY * 5f, -4f, 4f);
        if (Math.abs(aimY - CY) < 1.2f) aimY = CY + (rnd.nextBoolean() ? 2.6f : -2.6f);
        float dx = gx - p.x, dy = aimY - p.y, d = (float) Math.sqrt(dx * dx + dy * dy);
        float speed = 40f;
        float T = d / speed;
        float sp = (rnd.nextBoolean() ? 1f : -1f) * 0.9f;
        float a0 = -sp * T * 0.5f;
        float ang = (float) Math.atan2(dy, dx) + a0;
        kickBall(p, (float) Math.cos(ang), (float) Math.sin(ang), speed, 2.2f, EV_SPECIAL_GO);
        spin = sp; ballSpecial = true; shots[p.team]++;
    }

    // ------------------------------------------------------------------ entradas
    private void tackle(Player p, boolean slide) {
        if (p.tackleCd > 0 || p.stun > 0 || p.slide > 0) return;
        p.tackleCd = 0.85f;
        if (slide) {
            p.slide = 0.5f; p.actionT = 0.5f;
            float dx = p.fx, dy = p.fy;
            if (owner != null) {
                dx = owner.x - p.x; dy = owner.y - p.y;
            } else if (dist(p.x, p.y, bx, by) < 8f) { dx = bx - p.x; dy = by - p.y; }
            float l = (float) Math.sqrt(dx * dx + dy * dy);
            if (l < 0.01f) { dx = p.fx; dy = p.fy; l = 1f; }
            p.slideDx = dx / l; p.slideDy = dy / l;
            return;
        }
        p.actionT = 0.3f;
        Player o = owner;
        if (o != null && o.team != p.team) {
            float d = dist(p.x, p.y, o.x, o.y);
            float dot = (o.x - p.x) * p.fx + (o.y - p.y) * p.fy;
            if (d < 2.1f && dot > -0.4f) {
                float ch = clamp(0.5f + (p.def - o.spd) * 0.8f + (o.sprinting ? -0.1f : 0.05f), 0.25f, 0.85f);
                if (rnd.nextFloat() < ch) steal(p, o);
                else p.stun = 0.4f;
            } else p.tackleCd = 0.5f;
        }
    }

    private void steal(Player p, Player o) {
        o.stun = 0.55f;
        o.vx = p.fx * 3f; o.vy = p.fy * 3f;
        owner = null;
        special[p.team] = Math.min(1f, special[p.team] + 0.12f);
        ev(EV_TACKLE, p);
        take(p);
    }

    private void take(Player p) {
        if (lastTouch != null && lastTouch != p && lastTouch.team == p.team && state == ST_PLAY)
            special[p.team] = Math.min(1f, special[p.team] + 0.05f);
        owner = p; lastTouch = p; lastTouchTeam = p.team;
        ballSpecial = false; spin = 0; p.kickCd = 0;
        bvx = bvy = bvz = 0; bz = 0;
        p.think = 0.25f + rnd.nextFloat() * 0.25f;
        if (p.role == GK) p.holdT = 0.9f + rnd.nextFloat() * 0.8f;
        if (p.team == humanTeam && p.role != GK) ctrl = p;
    }

    // ------------------------------------------------------------------ balón
    private void ballStep(float dt, boolean inNet) {
        if (state == ST_RESTART || state == ST_KICKOFF) return;
        if (owner != null) {
            Player o = owner;
            float tx = o.x + o.fx * 0.85f, ty = o.y + o.fy * 0.85f;
            float k = Math.min(1f, dt * 22f);
            bx += (tx - bx) * k; by += (ty - by) * k;
            bx = clamp(bx, 0.2f, L - 0.2f); by = clamp(by, 0.2f, W - 0.2f);
            bz = 0.12f * Math.abs((float) Math.sin(o.anim * 1.2f));
            bvx = o.vx; bvy = o.vy; bvz = 0;
            return;
        }
        float px = bx, py = by;
        bvz -= G * dt;
        bx += bvx * dt; by += bvy * dt; bz += bvz * dt;
        if (bz < 0) {
            bz = 0;
            if (bvz < -3f) {
                bvz = -bvz * 0.52f; bvx *= 0.9f; bvy *= 0.9f;
                if (bvz > 1.6f) ev(EV_BOUNCE, null);
            } else bvz = 0;
        }
        if (bz <= 0.01f) {
            float f = (float) Math.exp(-(inNet ? 3.2f : 0.85f) * dt);
            bvx *= f; bvy *= f;
        } else {
            float f = (float) Math.exp(-0.05f * dt); bvx *= f; bvy *= f;
        }
        if (spin != 0f) {
            float sp = ballSpeed();
            if (sp > 5f) {
                float a = spin * dt, c = (float) Math.cos(a), s = (float) Math.sin(a);
                float nx = bvx * c - bvy * s, ny = bvx * s + bvy * c; bvx = nx; bvy = ny;
            }
            spin *= (1f - Math.min(1f, dt * 0.5f));
        }
        if (ballSpecial && ballSpeed() < 9f) ballSpecial = false;

        if (inNet) {
            float lo = goalSide == 0 ? -NET : L, hi = goalSide == 0 ? 0f : L + NET;
            if (bx < lo) { bx = lo; bvx = Math.abs(bvx) * 0.1f; }
            if (bx > hi) { bx = hi; bvx = -Math.abs(bvx) * 0.1f; }
            by = clamp(by, CY - GOAL_HALF, CY + GOAL_HALF);
            return;
        }
        if (state != ST_PLAY) return;

        // línea de gol
        boolean crossL = px > 0 && bx <= 0, crossR = px < L && bx >= L;
        if (crossL || crossR) {
            float lineX = crossL ? 0f : L;
            float f = (lineX - px) / (bx - px);
            float yAt = py + (by - py) * f, dy = Math.abs(yAt - CY);
            float zAt = bz;
            int side = crossL ? 0 : 1;
            if (dy < GOAL_HALF - 0.25f && zAt <= GOAL_H) {
                scoreGoal(side == 0 ? 1 : 0, side);
            } else if (dy <= GOAL_HALF + 0.25f && zAt <= GOAL_H + 0.3f
                    && (dy >= GOAL_HALF - 0.25f || zAt > GOAL_H)) {
                bx = lineX + (crossL ? 0.35f : -0.35f);
                bvx = -bvx * 0.55f;
                if (zAt > GOAL_H) bvz = Math.abs(bvz) * 0.3f + 1.5f;
                else bvy += (yAt > CY ? 1f : -1f) * (2f + rnd.nextFloat() * 3f);
                ballSpecial = false;
                ev(EV_POST, null);
            } else {
                int defending = crossL ? 0 : 1;
                if (lastTouchTeam == defending) {
                    float cy = by < CY ? 0.8f : W - 0.8f;
                    startRestart(1, 1 - defending, crossL ? 0.8f : L - 0.8f, cy);
                } else startRestart(2, defending, 0, CY);
            }
            return;
        }
        if (by < 0 || by > W) {
            int team = lastTouchTeam < 0 ? 0 : 1 - lastTouchTeam;
            startRestart(0, team, clamp(bx, 4f, L - 4f), by < 0 ? 0f : W);
        } else if (bx < -1 || bx > L + 1) {
            int defending = bx < 0 ? 0 : 1;
            if (lastTouchTeam == defending) startRestart(1, 1 - defending, bx < 0 ? 0.8f : L - 0.8f, by < CY ? 0.8f : W - 0.8f);
            else startRestart(2, defending, 0, CY);
        }
    }

    private void scoreGoal(int team, int side) {
        score[team]++;
        lastScoringTeam = team; goalSide = side;
        lastScorer = (lastTouch != null && lastTouch.team == team) ? lastTouch : null;
        state = ST_GOAL; stateT = 4.4f; ballSpecial = false;
        special[1 - team] = Math.min(1f, special[1 - team] + 0.15f);
        say("GOOOL", 4.0f, 3);
        ev(EV_GOAL, lastScorer);
    }

    private void tryPickup() {
        if (owner != null) return;
        Player best = null; float bd = 1e9f;
        float sp = ballSpeed();
        for (int t = 0; t < 2; t++) for (Player p : pl[t]) {
            if (p.kickCd > 0 || p.stun > 0) continue;
            float reach;
            if (p.role == GK) {
                reach = 1.5f + 0.5f * p.gkSkill;
                if (ballSpecial) reach *= 0.45f;
            } else reach = p.slide > 0 ? 1.5f : 1.0f;
            float zl = p.role == GK ? 2.8f : 1.5f;
            float d = dist(bx, by, p.x, p.y);
            if (d < reach && bz < zl && d < bd) { bd = d; best = p; }
        }
        if (best == null) return;
        Player p = best;
        if (p.role == GK) {
            boolean incoming = (bvx * dir(p.team)) < -2f && sp > 6f;
            if (!incoming) { take(p); return; }
            if (!gkRolled) {
                gkRolled = true;
                boolean fast = sp > 24f;
                float chanceHold = clamp(0.95f - (sp - 10f) * 0.035f, 0.05f, 0.95f) * (0.85f + 0.15f * p.gkSkill);
                if (ballSpecial) {
                    if (rnd.nextFloat() < 0.12f + 0.2f * (p.gkSkill - 0.9f) * 2f) { parry(p); return; }
                    p.stun = 0.7f; p.diveT = 0.6f; p.diveDir = by > p.y ? 1f : -1f;
                    return;
                }
                if (rnd.nextFloat() < chanceHold && !fast) { ev(EV_SAVE, p); special[p.team] = Math.min(1f, special[p.team] + 0.1f); take(p); }
                else parry(p);
            }
            return;
        }
        if (sp > 21f && rnd.nextFloat() < 0.4f) {
            bvx *= 0.45f; bvy *= 0.45f; bvz = 1.5f; p.kickCd = 0.25f;
            lastTouch = p; lastTouchTeam = p.team;
            return;
        }
        take(p);
    }

    private void parry(Player g) {
        float d = dir(g.team);
        bvx = d * Math.max(4f, Math.abs(bvx) * 0.3f);
        bvy = (rnd.nextFloat() - 0.5f) * 14f; bvz = 3.5f;
        g.kickCd = 0.35f; g.diveT = 0.5f; g.diveDir = bvy > 0 ? 1f : -1f;
        lastTouch = g; lastTouchTeam = g.team; ballSpecial = false; spin = 0;
        special[g.team] = Math.min(1f, special[g.team] + 0.1f);
        ev(EV_SAVE, g);
    }

    // ------------------------------------------------------------------ IA
    private void gkAI(Player g, float dt) {
        int t = g.team; float d = dir(t), gx = ownGoalX(t);
        if (owner == g) {
            move(g, 0, 0, false, dt, 1f);
            g.holdT -= dt;
            if (g.holdT <= 0 && state == ST_PLAY) distribute(g);
            return;
        }
        float bu = Math.abs(bx - gx);
        float tx = gx + d * (1.6f + clamp(bu / 40f, 0f, 1f) * 4.2f);
        float ty = CY + (by - CY) * clamp(0.32f - bu * 0.002f, 0.1f, 0.32f) * 1.0f;
        float mul = 1f; boolean dive = false;
        if (owner == null && bvx * d < -4f && ballSpeed() > 6f) {
            float lineX = gx + d * 1.3f;
            float tt = (lineX - bx) / bvx;
            if (tt > 0 && tt < 1.7f) {
                float py = by + bvy * tt;
                if (Math.abs(py - CY) < GOAL_HALF + 3f) {
                    ty = clamp(py, CY - GOAL_HALF - 0.4f, CY + GOAL_HALF + 0.4f); tx = lineX;
                    mul = 1.7f * (0.9f + 0.15f * g.gkSkill); dive = true;
                    if (tt < 0.45f && g.diveT <= 0 && Math.abs(ty - g.y) > 1.2f) {
                        g.diveT = 0.55f; g.diveDir = ty > g.y ? 1f : -1f;
                    }
                }
            }
        }
        if (!dive && owner == null && bu < 14f && Math.abs(by - CY) < 16f && ballSpeed() < 8f) {
            Player c = chasers[t][0];
            if (c == null || dist(c.x, c.y, bx, by) > dist(g.x, g.y, bx, by) + 1f) { tx = bx; ty = by; mul = 1.15f; }
        }
        moveTo(g, tx, ty, false, dt, mul);
    }

    private void distribute(Player g) {
        int t = g.team; Player best = null; float bs = -1e9f;
        for (Player m : pl[t]) {
            if (m == g || m.role == GK) continue;
            float open = nearestOppDist(m.x, m.y, t);
            float s = open * 0.4f + Math.abs(m.x - g.x) * 0.02f - Math.abs(m.y - CY) * 0.02f;
            if (s > bs) { bs = s; best = m; }
        }
        if (best != null) {
            passTo(g, best, dist(g.x, g.y, best.x, best.y) > 18f);
            if (t == humanTeam) ctrl = best;
        } else kickBall(g, dir(t), 0, 18f, 6f, EV_PASS);
    }

    private float nearestOppDist(float x, float y, int t) {
        float b = 99f;
        for (Player o : pl[1 - t]) b = Math.min(b, dist(x, y, o.x, o.y));
        return b;
    }

    private float homeX(Player p, float ballU, boolean teamBall) {
        float k = p.role == DF ? 0.20f : p.role == MF ? 0.34f : 0.40f;
        float u = p.hu + (ballU - 0.5f) * k + (teamBall && p.role >= MF ? 0.08f : 0f);
        u = clamp(u, 0.05f, 0.93f);
        return p.team == 0 ? u * L : L - u * L;
    }

    private void outfieldAI(Player p, float dt) {
        int t = p.team, o = 1 - t;
        float d = dir(t);
        float cm = (t == humanTeam) ? 1f : 1f;
        if (state == ST_KICKOFF) return;
        if (owner == p) { aiWithBall(p, dt); return; }
        boolean teamBall = owner != null && owner.team == t;
        boolean oppBall = owner != null && owner.team == o;
        float ballU = t == 0 ? bx / L : 1f - bx / L;
        float hx = homeX(p, ballU, teamBall);
        float hy = clamp(p.hy + (by - CY) * 0.28f, 5f, 55f);
        boolean sprint = false;
        float tx = hx, ty = hy;
        boolean c0 = chasers[t][0] == p, c1 = chasers[t][1] == p;
        if (owner == null) {
            if (c0) {
                tx = bx + bvx * 0.3f; ty = by + bvy * 0.3f; sprint = true;
            } else if (c1 && dist(p.x, p.y, bx, by) < 9f) { tx = bx - d * 2f; ty = by; sprint = true; }
        } else if (oppBall) {
            Player ob = owner;
            if (c0 || (c1 && p.role != FW)) {
                float off = c0 ? 0f : 3.5f * (p.y > ob.y ? 1f : -1f);
                tx = ob.x + ob.vx * 0.25f - d * 0.6f; ty = ob.y + ob.vy * 0.25f + off;
                sprint = dist(p.x, p.y, ob.x, ob.y) > 5f;
                float dd = dist(p.x, p.y, ob.x, ob.y);
                if (dd < 2.2f && p.tackleCd <= 0 && state == ST_PLAY) {
                    float rate = (t == humanTeam ? 2.2f : 1.4f + 1.6f * cpuLevel);
                    if (rnd.nextFloat() < rate * dt) tackle(p, false);
                }
            } else if (p.role == FW) {
                tx = hx; ty = hy;
            } else {
                float gx = ownGoalX(t);
                tx = hx; ty = hy + (ob.y - hy) * 0.35f;
                if (p.role == DF) tx = Math.min(Math.max(hx, 0f), L) ;
            }
        } else if (teamBall) {
            Player ob = owner;
            if (p.role == FW) {
                tx = clamp(ob.x + d * (10f + 5f * (p.idx - 4)), 4f, L - 4f);
                ty = (p.idx == 4 ? 17f : 43f) + (float) Math.sin(time * 0.7f + p.idx) * 5f;
                sprint = (tx - p.x) * d > 4f;
            } else if (p.role == MF) {
                tx = ob.x - d * 6f; ty = CY + (ob.y > CY ? -9f : 9f);
            }
        }
        moveTo(p, tx, ty, sprint, dt, t == humanTeam ? 1f : 1f);
    }

    private void aiWithBall(Player p, float dt) {
        int t = p.team; float d = dir(t), gx = oppGoalX(t);
        float dGoal = dist(p.x, p.y, gx, CY);
        p.think -= dt;
        if (state == ST_RESTART) { move(p, 0, 0, false, dt, 1f); return; }
        if (p.think <= 0) {
            p.think = 0.15f + rnd.nextFloat() * 0.2f;
            float pres = nearestOppDist(p.x, p.y, t);
            p.mode = 0;
            if (special[t] >= 1f && dGoal < 30f && rnd.nextFloat() < 0.35f) { startSpecial(p); return; }
            boolean shootZone = dGoal < 25f && Math.abs(p.y - CY) < 23f;
            if (shootZone && rnd.nextFloat() < 0.45f + (25f - dGoal) * 0.03f) {
                doShoot(p, 0, 0.55f + rnd.nextFloat() * 0.4f, false); return;
            }
            if (pres < 3.4f && rnd.nextFloat() < 0.6f) { if (passAI(p)) return; }
            else if (dGoal > 28f && rnd.nextFloat() < 0.22f) { if (passAI(p)) return; }
            // regate: hacia la portería esquivando al rival más cercano
            float ty = CY + (p.y - CY) * 0.55f;
            Player near = null; float nd = 6.5f;
            for (Player q : pl[1 - t]) {
                float dd = dist(p.x, p.y, q.x, q.y);
                if (dd < nd && (q.x - p.x) * d > -1f) { nd = dd; near = q; }
            }
            if (near != null) ty = p.y + (p.y >= near.y ? 1f : -1f) * 9f;
            p.tx = gx; p.ty = clamp(ty, 3f, W - 3f);
        }
        float dx = p.tx - p.x, dy = p.ty - p.y, l = (float) Math.sqrt(dx * dx + dy * dy);
        if (l > 0.01f) { dx /= l; dy /= l; }
        // algo de avance hacia la portería mientras esquiva
        dx = dx * 0.7f + d * 0.5f;
        float m = (float) Math.sqrt(dx * dx + dy * dy); dx /= m; dy /= m;
        move(p, dx, dy, dGoal < 50f && p.stamina > 0.25f, dt, 1f);
    }

    private boolean passAI(Player p) {
        int t = p.team; float d = dir(t);
        Player best = null; float bs = -1e9f; boolean lob = false;
        for (Player m : pl[t]) {
            if (m == p || m.role == GK) continue;
            float dd = dist(p.x, p.y, m.x, m.y);
            if (dd < 5f || dd > 38f) continue;
            float open = nearestOppDist(m.x, m.y, t);
            float s = (m.x - p.x) * d * 0.09f + Math.min(open, 10f) * 0.18f - dd * 0.02f;
            boolean blocked = false;
            for (Player q : pl[1 - t]) {
                float t2 = ((q.x - p.x) * (m.x - p.x) + (q.y - p.y) * (m.y - p.y)) / (dd * dd);
                if (t2 < 0f || t2 > 1f) continue;
                float cx = p.x + (m.x - p.x) * t2, cy = p.y + (m.y - p.y) * t2;
                if (dist(q.x, q.y, cx, cy) < 1.8f) { blocked = true; break; }
            }
            if (blocked) s -= 1.2f;
            if (s > bs) { bs = s; best = m; lob = blocked && dd > 12f; }
        }
        if (best == null || bs < -1f) return false;
        passTo(p, best, lob || dist(p.x, p.y, best.x, best.y) > 26f);
        return true;
    }

    // ------------------------------------------------------------------ celebración
    private void celebrate(float dt) {
        computeChasers();
        Player sc = lastScorer;
        float cx = sc != null ? sc.x : (goalSide == 0 ? 6f : L - 6f), cy = sc != null ? sc.y : CY;
        for (int t = 0; t < 2; t++) for (Player p : pl[t]) {
            if (p.stun > 0) p.stun -= dt;
            if (t == lastScoringTeam) {
                p.cheer = 0.5f;
                if (p == sc) {
                    float ex = (goalSide == 0 ? 0f : L) - p.x;
                    moveTo(p, p.x + (ex > 0 ? 1f : -1f) * 3f * (stateT > 2.5f ? 1f : 0f), CY + (p.y > CY ? 10f : -10f), true, dt, 1f);
                } else if (p.role != GK) {
                    float ang = p.idx * 1.1f;
                    moveTo(p, cx + (float) Math.cos(ang) * 2.2f, cy + (float) Math.sin(ang) * 2.2f, true, dt, 1f);
                } else move(p, 0, 0, false, dt, 1f);
            } else {
                move(p, 0, 0, false, dt, 1f);
            }
        }
    }
}
