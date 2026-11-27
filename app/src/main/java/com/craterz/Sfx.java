package com.craterz;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.util.ArrayList;

/** Sonido 100% sintetizado por código y mezclado en un solo AudioTrack estéreo. */
public final class Sfx {
    public static final int PISTOL = 0, SHOTGUN = 1, RIFLE = 2, SMG = 3, STORM = 4, RELOAD = 5, HIT = 6,
            GROAN1 = 7, GROAN2 = 8, GROAN3 = 9, ZDIE = 10, SWIPE = 11, HURT = 12, CASH = 13, DOOR = 14,
            ROUND = 15, THUNDER = 16, CRACK = 17, EMPTY = 18, POWER = 19, STEP = 20, BOX = 21, END = 22, NSND = 23;

    static final int RATE = 22050;
    private static final short[][] bank = new short[NSND][];
    private static short[] rainLoop, windLoop, droneLoop;
    private static boolean built;

    private static final class Voice {
        short[] data; int pos; float gl, gr; boolean loop; int tag;
    }

    private final ArrayList<Voice> voices = new ArrayList<Voice>();
    private AudioTrack track;
    private Thread thread;
    private volatile boolean running;
    private volatile boolean muted;
    private Voice rain, wind, drone;
    private volatile float rainVol = 0.5f;

    // ------------------------------------------------------------------ síntesis
    private static int seed = 1;
    private static float nz() {
        seed = seed * 1664525 + 1013904223;
        return ((seed >>> 8) & 0xFFFF) / 32768f - 1f;
    }

    private static short[] mk(float sec) { return new short[(int) (sec * RATE)]; }
    private static short s16(float v) { v = v < -1 ? -1 : (v > 1 ? 1 : v); return (short) (v * 32000); }

    private static float lp(float[] st, int i, float x, float a) { st[i] += a * (x - st[i]); return st[i]; }

    private static short[] gun(float sec, float thump, float crack, float body, float tail, float f0) {
        short[] d = mk(sec);
        float[] st = new float[3];
        float ph = 0;
        for (int i = 0; i < d.length; i++) {
            float t = i / (float) RATE;
            float n = nz();
            float env = (float) Math.exp(-t * 28 * body);
            float env2 = (float) Math.exp(-t * 6 / tail);
            float f = f0 * (float) Math.exp(-t * 14) + 40;
            ph += 6.2831853f * f / RATE;
            float th = (float) Math.sin(ph) * (float) Math.exp(-t * 22) * thump;
            float cr = n * (float) Math.exp(-t * 90) * crack;
            float bd = lp(st, 0, n, 0.35f) * env * 1.1f + lp(st, 1, n, 0.04f) * env2 * 0.8f * tail;
            d[i] = s16((th + cr + bd) * 0.8f);
        }
        return d;
    }

    private static short[] groan(float sec, float f0, float f1, float vib, float f1a, float f2a) {
        short[] d = mk(sec);
        float ph = 0;
        float y1a = 0, y1b = 0, y2a = 0, y2b = 0;
        float r1 = 0.985f, r2 = 0.97f;
        float c1 = 2 * r1 * (float) Math.cos(2 * Math.PI * f1a / RATE), c2 = 2 * r2 * (float) Math.cos(2 * Math.PI * f2a / RATE);
        for (int i = 0; i < d.length; i++) {
            float t = i / (float) RATE, u = t / sec;
            float f = f0 + (f1 - f0) * u + (float) Math.sin(t * 32) * vib;
            ph += f / RATE; ph -= (float) Math.floor(ph);
            float saw = ph * 2 - 1;
            float src = saw * 0.7f + nz() * 0.35f;
            float env = (float) (Math.sin(Math.PI * Math.pow(u, 0.6)));
            float o1 = src + c1 * y1a - r1 * r1 * y1b; y1b = y1a; y1a = o1;
            float o2 = src + c2 * y2a - r2 * r2 * y2b; y2b = y2a; y2a = o2;
            d[i] = s16((o1 * 0.035f + o2 * 0.028f) * env);
        }
        return d;
    }

    private static short[] noiseBurst(float sec, float decay, float cut, float vol) {
        short[] d = mk(sec);
        float[] st = new float[2];
        for (int i = 0; i < d.length; i++) {
            float t = i / (float) RATE;
            d[i] = s16(lp(st, 0, nz(), cut) * (float) Math.exp(-t * decay) * vol);
        }
        return d;
    }

    private static void build() {
        if (built) return;
        synchronized (Sfx.class) {
            if (built) return;
            bank[PISTOL] = gun(0.28f, 0.9f, 0.9f, 1.0f, 0.7f, 220);
            bank[SHOTGUN] = gun(0.55f, 1.3f, 1.0f, 0.55f, 1.4f, 160);
            bank[RIFLE] = gun(0.4f, 1.0f, 1.2f, 0.8f, 1.0f, 200);
            bank[SMG] = gun(0.2f, 0.7f, 1.0f, 1.3f, 0.5f, 260);
            // Tormenta: zap eléctrico descendente
            {
                short[] d = mk(0.7f);
                float ph = 0;
                for (int i = 0; i < d.length; i++) {
                    float t = i / (float) RATE;
                    float f = 2200 * (float) Math.exp(-t * 7) + 140;
                    ph += f / RATE; ph -= (float) Math.floor(ph);
                    float sq = ph < 0.5f ? 1 : -1;
                    float env = (float) Math.exp(-t * 5);
                    float wob = (float) Math.sin(t * 90) * 0.5f + 0.5f;
                    d[i] = s16((sq * 0.28f * wob + nz() * 0.22f * (float) Math.exp(-t * 12) + (float) Math.sin(ph * 6.2831853f * 0.5f) * 0.4f * (float) Math.exp(-t * 18)) * env * 1.6f);
                }
                bank[STORM] = d;
            }
            // recarga: clics metálicos
            {
                short[] d = mk(0.9f);
                float[] marks = {0.02f, 0.42f, 0.7f};
                for (int m = 0; m < marks.length; m++) {
                    int s0 = (int) (marks[m] * RATE);
                    for (int i = 0; i < 1400 && s0 + i < d.length; i++) {
                        float t = i / (float) RATE;
                        float v = (nz() * 0.6f + (float) Math.sin(t * 6.2831853f * (2300 + m * 700)) * 0.5f) * (float) Math.exp(-t * 130) * (m == 1 ? 0.7f : 0.5f);
                        d[s0 + i] = s16(d[s0 + i] / 32000f + v);
                    }
                }
                bank[RELOAD] = d;
            }
            // impacto carnoso
            {
                short[] d = mk(0.22f);
                float[] st = new float[2];
                float ph = 0;
                for (int i = 0; i < d.length; i++) {
                    float t = i / (float) RATE;
                    ph += 6.2831853f * (140 * (float) Math.exp(-t * 18) + 45) / RATE;
                    d[i] = s16(((float) Math.sin(ph) * 0.7f + lp(st, 0, nz(), 0.2f) * 0.8f) * (float) Math.exp(-t * 22));
                }
                bank[HIT] = d;
            }
            bank[GROAN1] = groan(1.3f, 78, 58, 6, 520, 1150);
            bank[GROAN2] = groan(1.0f, 96, 70, 9, 430, 980);
            bank[GROAN3] = groan(1.7f, 64, 48, 4, 600, 900);
            {
                short[] g = groan(1.1f, 120, 40, 14, 700, 1300);
                bank[ZDIE] = g;
            }
            bank[SWIPE] = noiseBurst(0.3f, 9, 0.15f, 0.9f);
            // golpe al jugador
            {
                short[] d = mk(0.5f);
                float ph = 0;
                for (int i = 0; i < d.length; i++) {
                    float t = i / (float) RATE;
                    ph += 6.2831853f * (90 * (float) Math.exp(-t * 6) + 40) / RATE;
                    d[i] = s16(((float) Math.sin(ph) * 0.9f + nz() * 0.25f * (float) Math.exp(-t * 30) + (float) Math.sin(t * 6.2831853f * 1800) * 0.08f * (float) Math.exp(-t * 4)) * (float) Math.exp(-t * 7));
                }
                bank[HURT] = d;
            }
            // caja registradora
            {
                short[] d = mk(0.5f);
                for (int i = 0; i < d.length; i++) {
                    float t = i / (float) RATE;
                    float a = (float) Math.sin(t * 6.2831853f * 1568) * (float) Math.exp(-t * 14);
                    float b = t > 0.07f ? (float) Math.sin((t - 0.07f) * 6.2831853f * 2349) * (float) Math.exp(-(t - 0.07f) * 10) : 0;
                    d[i] = s16((a + b) * 0.4f);
                }
                bank[CASH] = d;
            }
            // puerta / escombros
            {
                short[] d = mk(1.6f);
                float[] st = new float[2];
                for (int i = 0; i < d.length; i++) {
                    float t = i / (float) RATE;
                    float rum = lp(st, 0, nz(), 0.03f) * 3.2f * (float) Math.exp(-t * 1.6f);
                    float deb = lp(st, 1, nz(), 0.5f) * (float) Math.exp(-t * 4) * (nz() > 0.6f ? 1.2f : 0.2f);
                    d[i] = s16((rum + deb) * 0.9f);
                }
                bank[DOOR] = d;
            }
            // inicio de ronda: acorde grave menor
            {
                short[] d = mk(3.2f);
                float[] fr = {55f, 65.4f, 82.4f, 110f};
                for (int i = 0; i < d.length; i++) {
                    float t = i / (float) RATE;
                    float v = 0;
                    for (int k = 0; k < fr.length; k++) v += (float) Math.sin(t * 6.2831853f * fr[k] * (1f - 0.02f * t)) * (0.35f - k * 0.05f);
                    v += (float) Math.sin(t * 6.2831853f * 880 * (1f - 0.04f * t)) * 0.05f * (float) Math.exp(-t * 2);
                    float env = Math.min(1f, t * 4f) * (float) Math.exp(-t * 0.9f);
                    d[i] = s16(v * env * 0.8f);
                }
                bank[ROUND] = d;
            }
            // trueno
            {
                short[] d = mk(4.2f);
                float[] st = new float[3];
                for (int i = 0; i < d.length; i++) {
                    float t = i / (float) RATE;
                    float env = (float) (Math.exp(-t * 0.9) * (0.6 + 0.4 * Math.sin(t * 9 + Math.sin(t * 3) * 2)));
                    float v = lp(st, 0, nz(), 0.03f) * 5f + lp(st, 1, nz(), 0.012f) * 7f;
                    d[i] = s16(v * env * 0.9f);
                }
                bank[THUNDER] = d;
            }
            bank[CRACK] = noiseBurst(0.35f, 12, 0.7f, 1.2f);
            // sin balas: clic seco
            {
                short[] d = mk(0.12f);
                for (int i = 0; i < d.length; i++) {
                    float t = i / (float) RATE;
                    d[i] = s16((nz() * 0.5f + (float) Math.sin(t * 6.2831853f * 1200) * 0.4f) * (float) Math.exp(-t * 90));
                }
                bank[EMPTY] = d;
            }
            // bonificación
            {
                short[] d = mk(0.9f);
                for (int i = 0; i < d.length; i++) {
                    float t = i / (float) RATE;
                    float f = 440 * (float) Math.pow(2, Math.floor(t * 8) / 12f * 3.2f);
                    d[i] = s16((float) Math.sin(t * 6.2831853f * f) * 0.35f * (float) Math.exp(-t * 2.4f) + (float) Math.sin(t * 6.2831853f * f * 2) * 0.1f * (float) Math.exp(-t * 3));
                }
                bank[POWER] = d;
            }
            // paso en el barro
            {
                short[] d = mk(0.16f);
                float[] st = new float[2];
                for (int i = 0; i < d.length; i++) {
                    float t = i / (float) RATE;
                    d[i] = s16(lp(st, 0, nz(), 0.12f) * (float) Math.exp(-t * 28) * 0.8f);
                }
                bank[STEP] = d;
            }
            // caja misteriosa: melodía extraña
            {
                short[] d = mk(2.4f);
                float[] notes = {392, 466, 523, 587, 523, 466, 392, 349, 392, 466, 523, 622};
                for (int i = 0; i < d.length; i++) {
                    float t = i / (float) RATE;
                    int n = Math.min(notes.length - 1, (int) (t * 5));
                    float lt = t * 5 - n;
                    float env = (float) Math.exp(-lt * 3.5f) * 0.3f;
                    d[i] = s16(((float) Math.sin(t * 6.2831853f * notes[n]) + 0.4f * (float) Math.sin(t * 6.2831853f * notes[n] * 2)) * env);
                }
                bank[BOX] = d;
            }
            // fin de partida: caída lenta
            {
                short[] d = mk(3.5f);
                for (int i = 0; i < d.length; i++) {
                    float t = i / (float) RATE;
                    float f = 110 * (float) Math.pow(0.5, t * 0.5);
                    float v = (float) Math.sin(t * 6.2831853f * f) + 0.6f * (float) Math.sin(t * 6.2831853f * f * 1.5f) + 0.4f * (float) Math.sin(t * 6.2831853f * f * 1.19f);
                    d[i] = s16(v * 0.28f * (float) Math.exp(-t * 0.7f) * Math.min(1f, t * 6));
                }
                bank[END] = d;
            }
            // loops
            {
                rainLoop = mk(2f);
                float[] st = new float[2];
                for (int i = 0; i < rainLoop.length; i++) {
                    float n = nz();
                    float v = lp(st, 0, n, 0.5f) * 0.55f + (n - st[1]) * 0.1f;
                    st[1] += (n - st[1]) * 0.3f;
                    rainLoop[i] = s16(v * 0.5f);
                }
                int fade = 2000;
                for (int i = 0; i < fade; i++) {
                    float a = i / (float) fade;
                    int j = rainLoop.length - fade + i;
                    rainLoop[j] = (short) (rainLoop[j] * (1 - a) + rainLoop[i] * a);
                }
                windLoop = mk(8f);
                float[] ws = new float[2];
                for (int i = 0; i < windLoop.length; i++) {
                    float t = i / (float) RATE;
                    float mod = 0.5f + 0.5f * (float) Math.sin(t * 6.2831853f / 8f * 1 + 0.3f) * (0.6f + 0.4f * (float) Math.sin(t * 6.2831853f / 8f * 3));
                    windLoop[i] = s16(lp(ws, 0, nz(), 0.02f + 0.03f * mod) * 5.5f * mod);
                }
                droneLoop = mk(4f);
                for (int i = 0; i < droneLoop.length; i++) {
                    float t = i / (float) RATE;
                    float v = (float) Math.sin(t * 6.2831853f * 55f) * 0.5f + (float) Math.sin(t * 6.2831853f * 55.5f) * 0.5f
                            + 0.3f * (float) Math.sin(t * 6.2831853f * 82.5f) + 0.15f * (float) Math.sin(t * 6.2831853f * 110.25f);
                    droneLoop[i] = s16(v * 0.18f);
                }
            }
            built = true;
        }
    }

    public static void warmUp() {
        Thread t = new Thread(new Runnable() { public void run() { build(); } }, "sfxgen");
        t.setPriority(Thread.MIN_PRIORITY + 1);
        t.start();
    }

    // ------------------------------------------------------------------ mezclador
    public void start() {
        build();
        int minBuf = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        track = new AudioTrack(AudioManager.STREAM_MUSIC, RATE, AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, Math.max(minBuf, 4096 * 4), AudioTrack.MODE_STREAM);
        running = true;
        rain = addLoop(rainLoop, 0.3f, 1);
        wind = addLoop(windLoop, 0.5f, 2);
        drone = addLoop(droneLoop, 0.5f, 3);
        thread = new Thread(new Runnable() { public void run() { mixLoop(); } }, "sfxmix");
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    private Voice addLoop(short[] d, float vol, int tag) {
        Voice v = new Voice();
        v.data = d; v.gl = vol; v.gr = vol; v.loop = true; v.tag = tag;
        synchronized (voices) { voices.add(v); }
        return v;
    }

    public void setMuted(boolean m) { muted = m; }
    public void setRain(float v) { rainVol = v; }

    public void play(int id, float vol) { play(id, vol, 0f); }

    /** pan en -1..1 (izquierda..derecha). */
    public void play(int id, float vol, float pan) {
        if (!running || muted || vol < 0.01f) return;
        short[] d = bank[id];
        if (d == null) return;
        Voice v = new Voice();
        v.data = d;
        float p = pan < -1 ? -1 : (pan > 1 ? 1 : pan);
        v.gl = vol * (float) Math.sqrt((1 - p) * 0.5f) * 1.41f;
        v.gr = vol * (float) Math.sqrt((1 + p) * 0.5f) * 1.41f;
        synchronized (voices) {
            if (voices.size() > 28) {
                for (int i = 0; i < voices.size(); i++) if (!voices.get(i).loop) { voices.remove(i); break; }
            }
            voices.add(v);
        }
    }

    private void mixLoop() {
        final int N = 512;
        float[] acc = new float[N * 2];
        short[] out = new short[N * 2];
        try { track.play(); } catch (Throwable t) { return; }
        while (running) {
            for (int i = 0; i < acc.length; i++) acc[i] = 0;
            float rv = rainVol;
            synchronized (voices) {
                for (int k = voices.size() - 1; k >= 0; k--) {
                    Voice v = voices.get(k);
                    float gl = v.gl, gr = v.gr;
                    if (v.tag == 1) { gl = gr = 0.32f * rv; }
                    else if (v.tag == 2) { gl = gr = 0.22f + 0.12f * rv; }
                    else if (v.tag == 3) { gl = gr = 0.22f; }
                    short[] d = v.data;
                    int p = v.pos;
                    for (int i = 0; i < N; i++) {
                        if (p >= d.length) {
                            if (v.loop) p = 0; else break;
                        }
                        float s = d[p++] / 32768f;
                        acc[i * 2] += s * gl;
                        acc[i * 2 + 1] += s * gr;
                    }
                    v.pos = p;
                    if (!v.loop && p >= d.length) voices.remove(k);
                }
            }
            float mute = muted ? 0f : 1f;
            for (int i = 0; i < acc.length; i++) {
                float x = acc[i] * mute;
                x = x / (1f + Math.abs(x) * 0.6f);
                out[i] = (short) (x * 30000);
            }
            try { track.write(out, 0, out.length); } catch (Throwable t) { break; }
        }
        try { track.stop(); track.release(); } catch (Throwable ignored) { }
    }

    public void stop() {
        running = false;
        if (thread != null) { try { thread.join(500); } catch (InterruptedException ignored) { } }
        synchronized (voices) { voices.clear(); }
    }
}
