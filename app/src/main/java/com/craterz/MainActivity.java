package com.craterz;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/** Hub principal: modos de juego, ajustes, récords y ayuda, sobre una portada animada. */
public class MainActivity extends Activity {
    private MenuView view;
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Prefs.init(this);
        TexGen.warmUp();
        Sfx.warmUp();
        view = new MenuView(this);
        setContentView(view);
        hideBars();
    }

    private void hideBars() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean f) { super.onWindowFocusChanged(f); if (f) hideBars(); }

    @Override
    protected void onResume() { super.onResume(); view.running = true; view.tick(); }

    @Override
    protected void onPause() { super.onPause(); view.running = false; }

    @Override
    public void onBackPressed() {
        if (view.screen != MenuView.HOME) { view.screen = MenuView.HOME; view.invalidate(); }
        else super.onBackPressed();
    }

    void play(int mode) {
        Intent i = new Intent(this, GameActivity.class);
        i.putExtra("mode", mode);
        startActivity(i);
    }

    private final class MenuView extends View {
        static final int HOME = 0, MODES = 1, SETTINGS = 2, RECORDS = 3, HELP = 4;
        int screen = HOME;
        boolean running = true;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF rf = new RectF();
        private final Typeface bold = Typeface.create("sans-serif-condensed", Typeface.BOLD);
        private final Typeface serif = Typeface.create(Typeface.SERIF, Typeface.BOLD);
        private final long t0 = System.currentTimeMillis();
        private float bolt, nextBolt = 3f, lastT;
        private final RectF[] btn = new RectF[8];
        private final String[] btnLabel = new String[8];
        private int nBtn;
        private float d;

        MenuView(Context c) {
            super(c);
            d = c.getResources().getDisplayMetrics().density;
            for (int i = 0; i < btn.length; i++) btn[i] = new RectF();
        }

        void tick() {
            if (!running) return;
            invalidate();
            handler.postDelayed(new Runnable() { public void run() { tick(); } }, 33);
        }

        private float hash(int i, int s) {
            int h = i * 374761393 + s * 668265263;
            h = (h ^ (h >>> 13)) * 1274126177; h ^= h >>> 16;
            return (h & 0xFFFFFF) / 16777216f;
        }

        private void text(Canvas c, String s, float x, float y, float size, int col, Paint.Align a, Typeface tf) {
            p.setTypeface(tf); p.setTextSize(size); p.setTextAlign(a);
            p.setShadowLayer(size * 0.1f, 0, size * 0.04f, 0xFF000000);
            p.setColor(col);
            c.drawText(s, x, y, p);
            p.clearShadowLayer();
        }

        private void drawBackground(Canvas c, int w, int h, float t) {
            float dt = Math.max(0f, t - lastT); lastT = t;
            nextBolt -= dt;
            if (nextBolt <= 0) { bolt = 1f; nextBolt = 5f + hash((int) (t * 10), 3) * 9f; }
            if (bolt > 0) bolt -= dt * (bolt > 0.5f ? 3f : 1.3f);
            if (bolt < 0) bolt = 0;
            float b = bolt;
            // cielo
            p.setStyle(Paint.Style.FILL);
            p.setShader(new LinearGradient(0, 0, 0, h, new int[]{rgb(0.02f + 0.4f * b, 0.04f + 0.45f * b, 0.08f + 0.55f * b), rgb(0.07f + 0.4f * b, 0.11f + 0.45f * b, 0.15f + 0.5f * b), rgb(0.05f, 0.07f, 0.08f)}, new float[]{0, 0.62f, 1f}, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, p);
            p.setShader(null);
            // luna
            float mx = w * 0.78f, my = h * 0.22f, mr = h * 0.075f;
            p.setShader(new RadialGradient(mx, my, mr * 4.5f, 0x44A8C8F0, 0x00A8C8F0, Shader.TileMode.CLAMP));
            c.drawCircle(mx, my, mr * 4.5f, p);
            p.setShader(null);
            p.setColor(0xFFDCE8F4);
            c.drawCircle(mx, my, mr, p);
            p.setColor(0x33789098);
            c.drawCircle(mx - mr * 0.3f, my - mr * 0.2f, mr * 0.25f, p);
            c.drawCircle(mx + mr * 0.35f, my + mr * 0.3f, mr * 0.18f, p);
            // nubes
            for (int i = 0; i < 7; i++) {
                float cx = ((hash(i, 1) * w * 1.6f + t * (6f + i * 2f)) % (w * 1.6f)) - w * 0.3f;
                float cy = h * (0.08f + hash(i, 2) * 0.35f);
                float rw = w * (0.18f + hash(i, 3) * 0.2f), rh = h * 0.07f;
                p.setShader(new RadialGradient(cx, cy, rw, rgba(0.05f + 0.5f * b, 0.07f + 0.5f * b, 0.1f + 0.6f * b, 0.7f), 0x00000000, Shader.TileMode.CLAMP));
                rf.set(cx - rw, cy - rh * 1.8f, cx + rw, cy + rh * 1.8f);
                c.drawOval(rf, p);
            }
            p.setShader(null);
            // siluetas: colinas, iglesia en ruinas, árboles muertos
            float gy = h * 0.74f;
            p.setColor(0xFF0A0F12);
            path.reset();
            path.moveTo(0, h);
            path.lineTo(0, gy);
            for (int i = 0; i <= 24; i++) path.lineTo(w * i / 24f, gy - hash(i, 7) * h * 0.04f - (float) Math.sin(i * 0.5f) * h * 0.015f);
            path.lineTo(w, h);
            path.close();
            c.drawPath(path, p);
            // iglesia
            float cx = w * 0.30f, base = gy - h * 0.005f, cw = w * 0.16f, ch = h * 0.20f;
            p.setColor(0xFF070B0D);
            path.reset();
            path.moveTo(cx - cw / 2, base);
            path.lineTo(cx - cw / 2, base - ch * 0.62f);
            path.lineTo(cx - cw * 0.28f, base - ch * 0.55f);
            path.lineTo(cx - cw * 0.1f, base - ch * 0.85f);
            path.lineTo(cx + cw * 0.1f, base - ch * 0.78f);
            path.lineTo(cx + cw * 0.2f, base - ch * 0.5f);
            path.lineTo(cx + cw / 2, base - ch * 0.58f);
            path.lineTo(cx + cw / 2, base);
            path.close();
            c.drawPath(path, p);
            // torre
            c.drawRect(cx - cw * 0.9f, base - ch * 1.25f, cx - cw * 0.6f, base, p);
            path.reset();
            path.moveTo(cx - cw * 0.9f, base - ch * 1.25f);
            path.lineTo(cx - cw * 0.75f - cw * 0.04f, base - ch * 1.5f);
            path.lineTo(cx - cw * 0.6f, base - ch * 1.2f);
            path.close();
            c.drawPath(path, p);
            p.setColor(0xFFE8B060);
            c.drawRect(cx - cw * 0.1f, base - ch * 0.3f, cx + cw * 0.02f, base - ch * 0.12f, p); // ventana con luz de vela
            // árboles muertos
            p.setStyle(Paint.Style.STROKE);
            p.setColor(0xFF05080A);
            for (int i = 0; i < 6; i++) {
                float tx = w * (0.05f + hash(i, 11) * 0.9f);
                if (Math.abs(tx - cx) < cw) continue;
                float th = h * (0.12f + hash(i, 12) * 0.12f);
                p.setStrokeWidth(d * 4);
                c.drawLine(tx, gy + h * 0.02f, tx, gy - th, p);
                p.setStrokeWidth(d * 2);
                for (int k = 0; k < 4; k++) {
                    float by = gy - th * (0.35f + k * 0.17f), dir = k % 2 == 0 ? 1 : -1;
                    c.drawLine(tx, by, tx + dir * th * 0.28f, by - th * 0.2f, p);
                    c.drawLine(tx + dir * th * 0.28f, by - th * 0.2f, tx + dir * th * 0.4f, by - th * 0.38f, p);
                }
            }
            // alambre de púas
            p.setStrokeWidth(d * 1.5f);
            path.reset();
            path.moveTo(0, h * 0.88f);
            for (int i = 1; i <= 16; i++) path.lineTo(w * i / 16f, h * 0.88f + (float) Math.sin(i * 1.7f) * h * 0.01f + (i % 2) * h * 0.012f);
            c.drawPath(path, p);
            p.setStyle(Paint.Style.FILL);
            c.drawRect(0, h * 0.9f, w, h, p);
            // niebla
            for (int i = 0; i < 5; i++) {
                float fx = ((hash(i, 21) * w * 1.5f + t * (10f + i * 3f)) % (w * 1.5f)) - w * 0.25f;
                float fy = h * (0.70f + hash(i, 22) * 0.2f);
                p.setShader(new RadialGradient(fx, fy, w * 0.3f, rgba(0.3f + 0.3f * b, 0.38f + 0.3f * b, 0.42f + 0.3f * b, 0.22f), 0x00000000, Shader.TileMode.CLAMP));
                rf.set(fx - w * 0.3f, fy - h * 0.07f, fx + w * 0.3f, fy + h * 0.07f);
                c.drawOval(rf, p);
            }
            p.setShader(null);
            // lluvia
            p.setStrokeWidth(d * 1.2f);
            p.setColor(0x44B8D0E0);
            for (int i = 0; i < 140; i++) {
                float x = hash(i, 31) * w, sp = 700f + hash(i, 32) * 500f;
                float y = (hash(i, 33) * h + t * sp) % h;
                c.drawLine(x, y, x - h * 0.012f, y + h * 0.045f, p);
            }
            if (b > 0.01f) { p.setColor(((int) (b * 70) << 24) | 0xC8DCFF); p.setStyle(Paint.Style.FILL); c.drawRect(0, 0, w, h, p); }
        }

        private int rgb(float r, float g, float b) {
            return 0xFF000000 | ((int) (Math.min(1f, r) * 255) << 16) | ((int) (Math.min(1f, g) * 255) << 8) | (int) (Math.min(1f, b) * 255);
        }

        private int rgba(float r, float g, float b, float a) {
            return ((int) (Math.min(1f, a) * 255) << 24) | ((int) (Math.min(1f, r) * 255) << 16) | ((int) (Math.min(1f, g) * 255) << 8) | (int) (Math.min(1f, b) * 255);
        }

        private void button(Canvas c, int i, String label, float x, float y, float w, float h, int col) {
            btn[i].set(x, y, x + w, y + h);
            btnLabel[i] = label;
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xCC10161A);
            c.drawRoundRect(btn[i], h * 0.18f, h * 0.18f, p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(d * 2.5f); p.setColor(col);
            c.drawRoundRect(btn[i], h * 0.18f, h * 0.18f, p);
            text(c, label, btn[i].centerX(), btn[i].centerY() + h * 0.17f, h * 0.46f, 0xFFFFFFFF, Paint.Align.CENTER, bold);
            nBtn = Math.max(nBtn, i + 1);
        }

        @Override
        protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight();
            float u = Math.min(w, h);
            float t = (System.currentTimeMillis() - t0) / 1000f;
            drawBackground(c, w, h, t);
            nBtn = 0;
            float bw = u * 0.78f, bh = u * 0.115f, x = w / 2f - bw / 2f;
            switch (screen) {
                case HOME: {
                    float tl = u * 0.2f;
                    p.setStyle(Paint.Style.FILL);
                    text(c, "CRÁTER Z", w / 2f, h * 0.27f, tl, 0xFFB01818, Paint.Align.CENTER, serif);
                    text(c, "SUPERVIVENCIA 3D", w / 2f, h * 0.27f + u * 0.07f, u * 0.045f, 0xCCD8E4E8, Paint.Align.CENTER, bold);
                    float y = h * 0.42f;
                    button(c, 0, "Jugar", x, y, bw, bh, 0xFFB01818); y += bh * 1.2f;
                    button(c, 1, "Ajustes", x, y, bw * 0.48f, bh, 0xFF7090A0);
                    button(c, 2, "Récords", x + bw * 0.52f, y, bw * 0.48f, bh, 0xFF7090A0); y += bh * 1.2f;
                    button(c, 3, "Cómo jugar", x, y, bw, bh, 0xFF7090A0);
                    break;
                }
                case MODES: {
                    text(c, "ELIGE UN MODO", w / 2f, h * 0.17f, u * 0.08f, 0xFFD8E4E8, Paint.Align.CENTER, serif);
                    float y = h * 0.26f;
                    String[] desc = {"Rondas infinitas en El Cráter. Compra puertas, armas y ventajas.", "Los zombis corren desde el inicio. Empiezas con más puntos.", "Casi sin luz y con niebla densa. Escucha y usa las luces."};
                    for (int i = 0; i < 3; i++) {
                        button(c, i, Game.MODE_NAME[i], x, y, bw, bh, i == 0 ? 0xFFB01818 : (i == 1 ? 0xFFD08030 : 0xFF506890));
                        text(c, desc[i], w / 2f, y + bh + u * 0.04f, u * 0.032f, 0xBBFFFFFF, Paint.Align.CENTER, bold);
                        y += bh * 1.55f;
                    }
                    button(c, 3, "Volver", x + bw * 0.25f, y, bw * 0.5f, bh * 0.8f, 0xFF7090A0);
                    break;
                }
                case SETTINGS: {
                    text(c, "AJUSTES", w / 2f, h * 0.15f, u * 0.08f, 0xFFD8E4E8, Paint.Align.CENTER, serif);
                    float y = h * 0.22f;
                    String[] q = {"Baja (más fluido)", "Media (recomendada)", "Alta (más bonito)"};
                    button(c, 0, "Gráficos: " + q[Prefs.quality()], x, y, bw, bh, 0xFF7090A0); y += bh * 1.2f;
                    button(c, 1, "Sensibilidad: " + (int) (Prefs.sens() * 100) + "%", x, y, bw, bh, 0xFF7090A0); y += bh * 1.2f;
                    button(c, 2, "Vibración: " + (Prefs.vibrate() ? "Sí" : "No"), x, y, bw, bh, 0xFF7090A0); y += bh * 1.2f;
                    button(c, 3, "Sonido: " + (Prefs.sound() ? "Sí" : "No"), x, y, bw, bh, 0xFF7090A0); y += bh * 1.3f;
                    button(c, 4, "Volver", x + bw * 0.25f, y, bw * 0.5f, bh * 0.8f, 0xFF7090A0);
                    break;
                }
                case RECORDS: {
                    text(c, "RÉCORDS", w / 2f, h * 0.17f, u * 0.08f, 0xFFD8E4E8, Paint.Align.CENTER, serif);
                    float y = h * 0.30f;
                    for (int i = 0; i < 3; i++) {
                        text(c, Game.MODE_NAME[i], w / 2f, y, u * 0.05f, 0xFFE8C070, Paint.Align.CENTER, bold);
                        text(c, "Ronda " + Prefs.bestRound(i) + "   ·   " + Prefs.bestKills(i) + " bajas   ·   " + Prefs.bestPoints(i) + " pts", w / 2f, y + u * 0.055f, u * 0.04f, 0xCCFFFFFF, Paint.Align.CENTER, bold);
                        y += u * 0.16f;
                    }
                    text(c, "Zombis abatidos en total: " + Prefs.totalKills(), w / 2f, y, u * 0.04f, 0xAAFFFFFF, Paint.Align.CENTER, bold);
                    button(c, 0, "Volver", x + bw * 0.25f, y + u * 0.05f, bw * 0.5f, bh * 0.8f, 0xFF7090A0);
                    break;
                }
                default: {
                    text(c, "CÓMO JUGAR", w / 2f, h * 0.13f, u * 0.075f, 0xFFD8E4E8, Paint.Align.CENTER, serif);
                    String[] ln = {
                            "Izquierda: arrastra para moverte. Derecha: arrastra para mirar.",
                            "FUEGO dispara (hay un botón a cada lado). MIRA apunta con precisión.",
                            "Los zombis surgen de la tierra: dispara a la cabeza para más daño y puntos.",
                            "Cada golpe y cada baja te da puntos. Úsalos en las compras:",
                            "· USAR cerca de escombros: abre nuevas zonas del mapa.",
                            "· Siluetas de tiza en las paredes: comprar armas y munición.",
                            "· Máquinas de colores: ventajas permanentes (Vigor, Ráfaga, Reflejos, Pies ligeros).",
                            "· Caja Misteriosa: arma al azar, quizá la rara Tormenta.",
                            "Los objetos brillantes soltados por zombis dan bonificaciones temporales.",
                            "Tu vida se regenera si te ocultas unos segundos. ¡Aguanta cuantas rondas puedas!"};
                    float y = h * 0.22f;
                    for (int i = 0; i < ln.length; i++) { text(c, ln[i], w / 2f, y, u * 0.033f, 0xDDFFFFFF, Paint.Align.CENTER, bold); y += u * 0.058f; }
                    button(c, 0, "Volver", x + bw * 0.25f, y + u * 0.02f, bw * 0.5f, bh * 0.8f, 0xFF7090A0);
                    break;
                }
            }
            text(c, "v1.0  ·  mapa y personajes originales", w / 2f, h - u * 0.025f, u * 0.026f, 0x88FFFFFF, Paint.Align.CENTER, bold);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() != MotionEvent.ACTION_UP) return true;
            float x = e.getX(), y = e.getY();
            int hit = -1;
            for (int i = 0; i < nBtn; i++) if (btn[i].contains(x, y)) hit = i;
            if (hit < 0) return true;
            switch (screen) {
                case HOME:
                    if (hit == 0) screen = MODES; else if (hit == 1) screen = SETTINGS; else if (hit == 2) screen = RECORDS; else screen = HELP;
                    break;
                case MODES:
                    if (hit < 3) play(hit); else screen = HOME;
                    break;
                case SETTINGS:
                    if (hit == 0) Prefs.setQuality((Prefs.quality() + 1) % 3);
                    else if (hit == 1) { float s = Prefs.sens() + 0.25f; if (s > 2.01f) s = 0.5f; Prefs.setSens(s); }
                    else if (hit == 2) Prefs.setVibrate(!Prefs.vibrate());
                    else if (hit == 3) Prefs.setSound(!Prefs.sound());
                    else screen = HOME;
                    break;
                default:
                    screen = HOME;
            }
            invalidate();
            return true;
        }
    }
}
