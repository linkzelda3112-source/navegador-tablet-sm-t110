package com.craterz;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

/** Pantalla de juego: carga el mapa en segundo plano y luego muestra el 3D con la interfaz encima. */
public class GameActivity extends Activity implements Hud.Listener {
    private FrameLayout root;
    private GLSurfaceView gl;
    private Hud hud;
    private LoadingView loading;
    private Game game;
    private Sfx sfx;
    private Input in;
    private GlRenderer renderer;
    private final Handler handler = new Handler();
    private int mode;
    private volatile boolean built;
    private boolean glStarted, destroyed;
    private Vibrator vib;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Prefs.init(this);
        mode = getIntent().getIntExtra("mode", 0);
        vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        loading = new LoadingView(this);
        root.addView(loading);
        setContentView(root);
        hideBars();

        sfx = new Sfx();
        in = new Input();
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    sfx.start();
                    sfx.setMuted(!Prefs.sound());
                    Game g = new Game(mode, sfx, in);
                    game = g;
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                built = true;
            }
        }, "build-world");
        t.setPriority(Thread.NORM_PRIORITY - 1);
        t.start();
        handler.postDelayed(tick, 100);
    }

    private final Runnable tick = new Runnable() {
        public void run() {
            if (destroyed) return;
            if (built && !glStarted) {
                if (game == null) { finish(); return; }
                startGl();
            }
            if (loading != null) loading.invalidate();
            if (hud != null) {
                hud.fps = renderer != null ? renderer.fps : 0f;
                hud.invalidate();
                if (game != null && game.vibrateReq) {
                    game.vibrateReq = false;
                    try { if (vib != null) vib.vibrate(18); } catch (Throwable ignored) { }
                }
            }
            handler.postDelayed(this, 33);
        }
    };

    private void startGl() {
        glStarted = true;
        gl = new GLSurfaceView(this);
        gl.setEGLContextClientVersion(2);
        gl.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        renderer = new GlRenderer(game, Prefs.quality());
        gl.setPreserveEGLContextOnPause(true);
        gl.setRenderer(renderer);
        gl.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float scale = Prefs.quality() == 0 ? 0.55f : (Prefs.quality() == 1 ? 0.75f : 1f);
        int pw = Math.max(dm.widthPixels, dm.heightPixels), ph = Math.min(dm.widthPixels, dm.heightPixels);
        if (scale < 0.99f) gl.getHolder().setFixedSize((int) (pw * scale), (int) (ph * scale));
        root.addView(gl, 0, new FrameLayout.LayoutParams(-1, -1));
        hud = new Hud(this, game, in, this);
        root.addView(hud, new FrameLayout.LayoutParams(-1, -1));
        if (loading != null) { root.removeView(loading); loading = null; }
    }

    private void hideBars() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean f) {
        super.onWindowFocusChanged(f);
        if (f) hideBars();
        else if (game != null && !game.over) pauseGame();
    }

    private void pauseGame() {
        if (game != null && !game.over) { game.paused = true; in.clear(); }
    }

    @Override protected void onPause() { super.onPause(); pauseGame(); if (gl != null) gl.onPause(); if (sfx != null) sfx.setMuted(true); }

    @Override
    protected void onResume() {
        super.onResume();
        if (gl != null) gl.onResume();
        if (sfx != null) sfx.setMuted(!Prefs.sound() || (game != null && game.paused));
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        handler.removeCallbacksAndMessages(null);
        if (sfx != null) sfx.stop();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (game == null) { super.onBackPressed(); return; }
        if (game.over) { onHudMenu(); return; }
        if (game.paused) onHudResume(); else onHudPause();
    }

    // ---- Hud.Listener
    @Override
    public void onHudResume() {
        if (game != null) { game.paused = false; in.clear(); }
        if (sfx != null) sfx.setMuted(!Prefs.sound());
    }

    @Override
    public void onHudPause() {
        pauseGame();
        if (sfx != null) sfx.setMuted(true);
    }

    @Override
    public void onHudRestart() {
        Intent i = new Intent(this, GameActivity.class);
        i.putExtra("mode", mode);
        finish();
        startActivity(i);
    }

    @Override
    public void onHudMenu() { finish(); }

    /** Pequeña pantalla de carga con lluvia mientras se genera el mapa. */
    private static final class LoadingView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final long t0 = System.currentTimeMillis();
        LoadingView(Context c) { super(c); }
        @Override
        protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight();
            float t = (System.currentTimeMillis() - t0) / 1000f;
            c.drawColor(0xFF05080B);
            p.setColor(0x33A8C8D8);
            p.setStrokeWidth(2f);
            for (int i = 0; i < 90; i++) {
                float x = (i * 137.5f) % w, y = ((i * 71.3f) + t * 900f * (0.7f + (i % 5) * 0.12f)) % h;
                c.drawLine(x, y, x - 6, y + 28, p);
            }
            p.setColor(0xFFB01818);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(Math.min(w, h) * 0.12f);
            p.setFakeBoldText(true);
            c.drawText("CRÁTER Z", w / 2f, h * 0.45f, p);
            p.setColor(0xCCFFFFFF);
            p.setFakeBoldText(false);
            p.setTextSize(Math.min(w, h) * 0.045f);
            int dots = (int) (t * 2) % 4;
            c.drawText("Levantando el campo de batalla" + "...".substring(0, dots), w / 2f, h * 0.58f, p);
        }
    }
}
