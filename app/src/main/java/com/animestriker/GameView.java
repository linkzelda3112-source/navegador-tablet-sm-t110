package com.animestriker;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

/** Vista principal: bucle de juego, flujo de pantallas, copa y entrada táctil. */
public final class GameView extends View implements Choreographer.FrameCallback {

    static final int TITLE = 0, MENU = 1, TEAM = 2, RIVAL = 3, SETTINGS = 4, MATCH = 5, RESULT = 6, CUP = 7;

    final Renderer rend = new Renderer();
    final Ui ui = new Ui();
    final Pad pad = new Pad();
    final Sfx sfx = new Sfx();
    final Random rnd = new Random();
    final SharedPreferences prefs;
    final Runnable finish;

    int screen = TITLE, mode;            // mode 0 amistoso, 1 copa
    int selTeam = 0, selRival = 1;
    int diff = 1, dur = 1;
    boolean snd = true, mus = true;
    Match match;
    boolean paused, running, ready;
    float time, acc, endTimer, lastDt;
    long lastNs;
    int downId = -1;

    // copa
    final int[] cupTeams = new int[4];
    final boolean[] semiWin = new boolean[2];
    int round;
    String resHead = "", resSub = "", resBtn = "CONTINUAR";
    boolean cupDone;

    public GameView(Context ctx, Runnable finish) {
        super(ctx);
        this.finish = finish;
        prefs = ctx.getSharedPreferences("animestriker", Context.MODE_PRIVATE);
        diff = prefs.getInt("diff", 1); dur = prefs.getInt("dur", 1);
        snd = prefs.getBoolean("snd", true); mus = prefs.getBoolean("mus", true);
        selTeam = prefs.getInt("team", 0);
        setKeepScreenOn(true);
        new Thread(() -> {
            sfx.sound = snd; sfx.music = mus;
            sfx.init();
            sfx.sound = sfx.sound && snd; sfx.music = sfx.music && mus;
            ready = true;
            post(this::musicForScreen);
        }, "sfx-init").start();
    }

    void save() {
        prefs.edit().putInt("diff", diff).putInt("dur", dur).putBoolean("snd", snd).putBoolean("mus", mus).putInt("team", selTeam).apply();
    }

    // ------------------------------------------------------------------ ciclo de vida
    public void onResumeGame() {
        running = true; lastNs = 0; Choreographer.getInstance().postFrameCallback(this); musicForScreen();
    }

    public void onPauseGame() {
        running = false;
        if (screen == MATCH && match != null && match.state != Match.ST_END) paused = true;
        sfx.startMusic(false); sfx.startAmbient(false);
    }

    public void onDestroyGame() { sfx.release(); }

    void musicForScreen() {
        if (!ready) return;
        sfx.startMusic(screen != MATCH);
        sfx.startAmbient(screen == MATCH && !paused);
    }

    void go(int s) { screen = s; musicForScreen(); }

    /** Devuelve true si la vista manejó el botón atrás. */
    public boolean onBack() {
        switch (screen) {
            case MATCH: paused = !paused; musicForScreen(); return true;
            case TEAM: case RIVAL: case SETTINGS: case CUP: go(MENU); return true;
            case MENU: go(TITLE); return true;
            case RESULT: go(MENU); return true;
            default: return false;
        }
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        rend.resize(w, h); ui.resize(w, h); pad.layout(w, h);
    }

    // ------------------------------------------------------------------ bucle
    @Override public void doFrame(long ns) {
        if (!running) return;
        float dt = lastNs == 0 ? 0.016f : Math.min(0.05f, (ns - lastNs) / 1e9f);
        lastNs = ns; lastDt = dt; time += dt;
        if (screen == MATCH && match != null && !paused) {
            acc += dt;
            int n = 0;
            while (acc >= 1f / 60f && n < 4) {
                if (match.state != Match.ST_PLAY && match.state != Match.ST_RESTART) match.pressA = match.pressB = match.pressC = match.pressD = false;
                match.update(1f / 60f); acc -= 1f / 60f; n++;
            }
            if (n == 4) acc = 0;
            if (match.state == Match.ST_END) {
                endTimer += dt;
                if (endTimer > 2.6f) finishMatch();
            }
        }
        invalidate();
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override protected void onDraw(Canvas c) {
        switch (screen) {
            case TITLE: ui.drawTitle(c, time); break;
            case MENU: ui.drawMenu(c, time); break;
            case TEAM: ui.drawTeams(c, time, "ELIGE TU EQUIPO", selTeam, -1); break;
            case RIVAL: ui.drawTeams(c, time, "ELIGE A TU RIVAL", selRival, selTeam); break;
            case SETTINGS: ui.drawSettings(c, time, diff, dur, snd, mus); break;
            case CUP: ui.drawCup(c, time, cupTeams, round, semiWin,
                    round == 0 ? "Semifinal contra " + Teams.ALL[cupTeams[1]].name
                            : "Final contra " + Teams.ALL[finalRival()].name); break;
            case RESULT: ui.drawResult(c, time, match, resHead, resSub, resBtn); break;
            case MATCH:
                if (match != null) {
                    rend.drawMatch(c, match, paused ? 0f : lastDt, pad);
                    if (paused) ui.drawPause(c);
                }
                break;
            default:
        }
    }

    // ------------------------------------------------------------------ partidos
    int finalRival() { return semiWin[1] ? cupTeams[2] : cupTeams[3]; }

    void startMatch(Teams.Def a, Teams.Def b, boolean cup) {
        float[] halves = {90f, 120f, 180f};
        match = new Match(a, b, 0, halves[dur], diff / 2f, cup);
        final Match mm = match;
        match.listener = (t, p) -> { onEvent(t, p, mm); };
        endTimer = 0; paused = false; acc = 0;
        pad.joyOn = false; pad.joyId = -1; pad.layout(getWidth(), getHeight());
        for (int i = 0; i < 5; i++) { pad.down[i] = false; pad.ptr[i] = -1; }
        go(MATCH);
        sfx.play(Sfx.WHISTLE);
    }

    void onEvent(int t, Match.Player p, Match m) {
        rend.event(t, m, p);
        switch (t) {
            case Match.EV_KICK: case Match.EV_SHOT: case Match.EV_SPECIAL_GO: sfx.play(Sfx.KICK); break;
            case Match.EV_PASS: sfx.play(Sfx.PASS); break;
            case Match.EV_WHISTLE: sfx.play(Sfx.WHISTLE); break;
            case Match.EV_GOAL: sfx.play(Sfx.GOAL); break;
            case Match.EV_SAVE: sfx.play(Sfx.SAVE); break;
            case Match.EV_POST: sfx.play(Sfx.POST); break;
            case Match.EV_SPECIAL: sfx.play(Sfx.SPECIAL); break;
            case Match.EV_TACKLE: sfx.play(Sfx.TACKLE); break;
            case Match.EV_BOUNCE: sfx.play(Sfx.BOUNCE); break;
            default:
        }
    }

    void finishMatch() {
        Match m = match;
        boolean won = m.score[0] > m.score[1] || (m.score[0] == m.score[1] && m.shootout && m.penA > m.penB);
        boolean draw = m.score[0] == m.score[1] && !m.shootout;
        resBtn = "CONTINUAR"; cupDone = true;
        if (mode == 0) {
            resHead = won ? "¡VICTORIA!" : draw ? "EMPATE" : "DERROTA";
            resSub = won ? "¡Qué partidazo, " + m.def[0].captain() + " estuvo imparable!" : draw ? "Todo quedó igualado." : "¡La revancha te espera!";
        } else if (won) {
            if (round == 0) {
                semiWin[0] = true;
                semiWin[1] = rnd.nextFloat() < 0.5f + (Teams.ALL[cupTeams[2]].stars - Teams.ALL[cupTeams[3]].stars) * 0.08f;
                round = 1; cupDone = false;
                resHead = "¡A LA FINAL!"; resSub = "Tu rival será " + Teams.ALL[finalRival()].name;
            } else { resHead = "¡CAMPEÓN!"; resSub = "¡Levantaste la Copa Anime!"; }
        } else {
            resHead = round == 0 ? "ELIMINADO" : "SUBCAMPEÓN";
            resSub = round == 0 ? "Caíste en semifinales." : "Estuviste a un paso de la copa.";
        }
        go(RESULT);
    }

    void startCup() {
        round = 0; cupDone = false;
        java.util.ArrayList<Integer> others = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) if (i != selTeam) others.add(i);
        java.util.Collections.shuffle(others, rnd);
        cupTeams[0] = selTeam; cupTeams[1] = others.get(0); cupTeams[2] = others.get(1); cupTeams[3] = others.get(2);
        go(CUP);
    }

    // ------------------------------------------------------------------ menús
    void menuClick(int id) {
        if (id < 0) return;
        sfx.play(Sfx.CLICK);
        switch (screen) {
            case TITLE: go(MENU); break;
            case MENU:
                if (id == Ui.B_FRIENDLY) { mode = 0; go(TEAM); }
                else if (id == Ui.B_CUP) { mode = 1; go(TEAM); }
                else if (id == Ui.B_SETTINGS) go(SETTINGS);
                else if (id == Ui.B_BACK) go(TITLE);
                break;
            case TEAM:
                if (id >= Ui.B_TEAM0 && id < Ui.B_TEAM0 + 6) { selTeam = id - Ui.B_TEAM0; if (selRival == selTeam) selRival = (selTeam + 1) % 6; }
                else if (id == Ui.B_CONFIRM) { save(); if (mode == 0) go(RIVAL); else startCup(); }
                else if (id == Ui.B_BACK) go(MENU);
                break;
            case RIVAL:
                if (id >= Ui.B_TEAM0 && id < Ui.B_TEAM0 + 6 && id - Ui.B_TEAM0 != selTeam) selRival = id - Ui.B_TEAM0;
                else if (id == Ui.B_CONFIRM) startMatch(Teams.ALL[selTeam], Teams.ALL[selRival], false);
                else if (id == Ui.B_BACK) go(TEAM);
                break;
            case SETTINGS:
                if (id == Ui.B_DIFF) diff = (diff + 1) % 3;
                else if (id == Ui.B_DUR) dur = (dur + 1) % 3;
                else if (id == Ui.B_SND) { snd = !snd; sfx.sound = snd; sfx.startAmbient(false); }
                else if (id == Ui.B_MUS) { mus = !mus; sfx.music = mus; musicForScreen(); }
                else if (id == Ui.B_BACK) { save(); go(MENU); }
                break;
            case CUP:
                if (id == Ui.B_CONT) startMatch(Teams.ALL[selTeam], Teams.ALL[round == 0 ? cupTeams[1] : finalRival()], true);
                else if (id == Ui.B_BACK) go(MENU);
                break;
            case RESULT:
                if (id == Ui.B_CONT) { if (mode == 1 && !cupDone) go(CUP); else go(MENU); }
                break;
            case MATCH:
                if (id == Ui.B_RESUME) { paused = false; musicForScreen(); }
                else if (id == Ui.B_RESTART) { Match m = match; startMatch(m.def[0], m.def[1], m.cup); }
                else if (id == Ui.B_QUIT) { paused = false; go(MENU); }
                break;
            default:
        }
    }

    // ------------------------------------------------------------------ toques
    @Override public boolean onTouchEvent(MotionEvent e) {
        int act = e.getActionMasked();
        boolean menu = screen != MATCH || paused;
        if (menu) {
            if (act == MotionEvent.ACTION_DOWN) downId = ui.hit(e.getX(), e.getY());
            else if (act == MotionEvent.ACTION_UP) {
                int id = ui.hit(e.getX(), e.getY());
                if (id == downId) menuClick(id);
                downId = -1;
            }
            return true;
        }
        Match m = match;
        if (m == null) return true;
        if (act == MotionEvent.ACTION_DOWN || act == MotionEvent.ACTION_POINTER_DOWN) {
            int i = e.getActionIndex(), id = e.getPointerId(i);
            float x = e.getX(i), y = e.getY(i);
            if (pad.hitPause(x, y)) { paused = true; musicForScreen(); return true; }
            int b = pad.hit(x, y);
            if (b >= 0) {
                pad.down[b] = true; pad.ptr[b] = id;
                switch (b) {
                    case Pad.A: m.pressA = true; break;
                    case Pad.B: m.pressB = true; m.inShoot = true; break;
                    case Pad.C: m.pressC = true; break;
                    case Pad.D: m.pressD = true; break;
                    default: m.inSprint = true;
                }
            } else if (x < getWidth() * 0.55f && pad.joyId == -1) {
                pad.joyId = id; pad.joyOn = true;
                pad.baseX = Math.max(pad.jr, Math.min(getWidth() * 0.5f, x));
                pad.baseY = Math.max(getHeight() * 0.3f, Math.min(getHeight() - pad.jr * 0.6f, y));
                pad.knobX = pad.baseX; pad.knobY = pad.baseY;
            }
        } else if (act == MotionEvent.ACTION_MOVE) {
            for (int i = 0; i < e.getPointerCount(); i++) {
                if (e.getPointerId(i) != pad.joyId) continue;
                float dx = e.getX(i) - pad.baseX, dy = e.getY(i) - pad.baseY;
                float d = (float) Math.sqrt(dx * dx + dy * dy), k = d > pad.jr ? pad.jr / d : 1f;
                pad.knobX = pad.baseX + dx * k; pad.knobY = pad.baseY + dy * k;
                m.inX = dx * k / pad.jr; m.inY = dy * k / pad.jr;
            }
        } else if (act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_POINTER_UP || act == MotionEvent.ACTION_CANCEL) {
            int i = e.getActionIndex(), id = e.getPointerId(i);
            if (act == MotionEvent.ACTION_CANCEL) releaseAll(m);
            else {
                if (id == pad.joyId) releaseJoy(m);
                for (int b = 0; b < 5; b++) if (pad.ptr[b] == id) {
                    pad.ptr[b] = -1; pad.down[b] = false;
                    if (b == Pad.B) m.inShoot = false;
                    if (b == Pad.S) m.inSprint = false;
                }
            }
        }
        return true;
    }

    void releaseJoy(Match m) {
        pad.joyId = -1; pad.joyOn = false; pad.layout(getWidth(), getHeight());
        pad.baseX = pad.jx; pad.baseY = pad.jy; pad.knobX = pad.jx; pad.knobY = pad.jy;
        m.inX = 0; m.inY = 0;
    }

    void releaseAll(Match m) {
        releaseJoy(m);
        for (int b = 0; b < 5; b++) { pad.ptr[b] = -1; pad.down[b] = false; }
        m.inShoot = false; m.inSprint = false;
    }
}
