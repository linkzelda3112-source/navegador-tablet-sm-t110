package com.craterz;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.util.ArrayList;

/**
 * Sonido 100% sintetizado por código (sin archivos): disparos en capas (chasquido + estampido + golpe grave + eco
 * del terreno), sonidos mecánicos con parciales metálicos, voces de zombi con formantes, e impactos carnosos.
 * Todo pasa por un mezclador estéreo con reverberación.
 */
public final class Sfx {
    public static final int PISTOL = 0, SHOTGUN = 1, RIFLE = 2, SMG = 3, STORM = 4, RELOAD = 5, HIT = 6,
            GROAN1 = 7, GROAN2 = 8, GROAN3 = 9, ZDIE = 10, SWIPE = 11, HURT = 12, CASH = 13, DOOR = 14,
            ROUND = 15, THUNDER = 16, CRACK = 17, EMPTY = 18, POWER = 19, STEP = 20, BOX = 21, END = 22,
            MAG_OUT = 23, MAG_IN = 24, BOLT = 25, PUMP = 26, SHELL = 27, SWITCH = 28, FLESH = 29, HEADSHOT = 30,
            SNARL = 31, SCREAM = 32, NSND = 33;

    static final int RATE = 32000;
    private static final float TAU = 6.2831853f;
    private static final short[][] bank = new short[NSND][];
    private static final float[] send = new float[NSND];
    private static short[] rainLoop, windLoop, droneLoop;
    private static boolean built;

    private static final class Voice {
        short[] data; int pos; float gl, gr, sd; boolean loop; int tag;
    }

    private final ArrayList<Voice> voices = new ArrayList<Voice>();
    private AudioTrack track;
    private Thread thread;
    private volatile boolean running;
    private volatile boolean muted;
    private Voice rain, wind, drone;
    private volatile float rainVol = 0.5f;

    // ------------------------------------------------------------------ utilidades de síntesis
    private static int seed = 1;
    private static float nz() {
        seed = seed * 1664525 + 1013904223;
        return ((seed >>> 8) & 0xFFFF) / 32768f - 1f;
    }
    private static float ex(float x) { return (float) Math.exp(x); }
    private static float sin(float x) { return (float) Math.sin(x); }

    private static float[] fa(float sec) { return new float[(int) (sec * RATE)]; }
    private static short s16(float v) { v = v < -1 ? -1 : (v > 1 ? 1 : v); return (short) (v * 32000); }
    private static float lp(float[] st, int i, float x, float a) { st[i] += a * (x - st[i]); return st[i]; }

    /** Normaliza a un pico dado con saturación suave y lo convierte a 16 bits. */
    private static short[] fin(float[] f, float peak) {
        float mx = 1e-6f;
        for (int i = 0; i < f.length; i++) mx = Math.max(mx, Math.abs(f[i]));
        float g = peak / mx;
        short[] d = new short[f.length];
        for (int i = 0; i < f.length; i++) {
            float x = f[i] * g;
            x = x / (1f + 0.25f * Math.abs(x));
            d[i] = s16(x * 1.2f);
        }
        // fundido final para evitar clics
        int fade = Math.min(d.length, 220);
        for (int i = 0; i < fade; i++) d[d.length - 1 - i] = (short) (d[d.length - 1 - i] * (i / (float) fade));
        return d;
    }

    /** Añade ecos del terreno (reflexiones suavizadas) a un buffer. */
    private static void echoes(float[] f, float[] times, float[] gains, float damp) {
        float[] src = f.clone();
        for (int e = 0; e < times.length; e++) {
            int off = (int) (times[e] * RATE);
            float st = 0;
            for (int i = off; i < f.length; i++) {
                st += damp * (src[i - off] - st);
                f[i] += st * gains[e];
            }
        }
    }

    private static void addMetalHit(float[] d, float at, float f0, float amp, float decay, float noiseAmt) {
        int s0 = (int) (at * RATE);
        float[] pr = {1f, 2.76f, 5.4f, 8.93f};
        float[] pa = {1f, 0.55f, 0.3f, 0.15f};
        for (int i = 0; s0 + i < d.length && i < RATE / 3; i++) {
            float t = i / (float) RATE;
            float v = 0;
            for (int k = 0; k < 4; k++) v += sin(TAU * f0 * pr[k] * t) * pa[k] * ex(-t * decay * (1f + k * 0.8f));
            v += nz() * noiseAmt * ex(-t * decay * 4f);
            d[s0 + i] += v * amp;
        }
    }

    private static void addThud(float[] d, float at, float f0, float amp, float decay) {
        int s0 = (int) (at * RATE);
        for (int i = 0; s0 + i < d.length && i < RATE / 4; i++) {
            float t = i / (float) RATE;
            d[s0 + i] += sin(TAU * (f0 * ex(-t * 12f) + f0 * 0.5f) * t) * ex(-t * decay) * amp;
        }
    }

    private static void addScrape(float[] d, float at, float len, float amp, float cut0, float cut1) {
        int s0 = (int) (at * RATE), n = (int) (len * RATE);
        float st = 0;
        for (int i = 0; i < n && s0 + i < d.length; i++) {
            float u = i / (float) n;
            float a = cut0 + (cut1 - cut0) * u;
            st += a * (nz() - st);
            float env = (float) Math.sin(Math.PI * u);
            float rough = 0.6f + 0.4f * sin(TAU * 95f * u * len);
            d[s0 + i] += (st - 0.0f) * env * rough * amp;
        }
    }

    // ------------------------------------------------------------------ armas
    /**
     * Disparo en capas. crack: chasquido supersónico; blast: estampido de gases; thump: golpe grave del cañón;
     * rumble: retumbo largo; ech: intensidad de los ecos del terreno.
     */
    private static short[] gun(float sec, float crack, float crackDecay, float blast, float blastDecay, float blastCut,
                               float thump, float thumpF, float rumble, float rumbleDecay, float ech) {
        float[] f = fa(sec);
        float[] st = new float[4];
        float ph = 0;
        for (int i = 0; i < f.length; i++) {
            float t = i / (float) RATE;
            float n = nz();
            float hp = n - lp(st, 0, n, 0.3f);
            float cr = hp * ex(-t * crackDecay) * crack + hp * ex(-t * crackDecay * 0.25f) * crack * 0.25f;
            float bl = lp(st, 1, n, blastCut) * ex(-t * blastDecay) * blast;
            ph += TAU * (thumpF * ex(-t * 20f) + 32f) / RATE;
            float th = sin(ph) * ex(-t * 24f) * thump;
            float rb = lp(st, 2, n, 0.012f) * ex(-t * rumbleDecay) * rumble * 4f;
            f[i] = cr + bl + th + rb;
        }
        echoes(f, new float[]{0.038f, 0.083f, 0.15f, 0.26f}, new float[]{0.32f * ech, 0.22f * ech, 0.14f * ech, 0.08f * ech}, 0.22f);
        return fin(f, 0.92f);
    }

    private static short[] storm() {
        float[] f = fa(0.8f);
        float ph = 0, ph2 = 0;
        float[] st = new float[2];
        for (int i = 0; i < f.length; i++) {
            float t = i / (float) RATE;
            float fr = 2600f * ex(-t * 8f) + 150f;
            ph += fr / RATE; ph -= (float) Math.floor(ph);
            ph2 += fr * 1.51f / RATE; ph2 -= (float) Math.floor(ph2);
            float env = ex(-t * 5f);
            float wob = sin(t * 110f) * 0.5f + 0.5f;
            float sq = (ph < 0.5f ? 1f : -1f) * 0.25f + (ph2 < 0.5f ? 1f : -1f) * 0.12f;
            float n = nz();
            float crackle = (n - lp(st, 0, n, 0.4f)) * ex(-t * 20f) * 0.6f;
            float sub = sin(TAU * 55f * t) * ex(-t * 9f) * 0.7f;
            f[i] = (sq * (0.5f + wob) + crackle + sub) * env;
        }
        echoes(f, new float[]{0.05f, 0.12f}, new float[]{0.25f, 0.15f}, 0.3f);
        return fin(f, 0.85f);
    }

    // ------------------------------------------------------------------ mecánica
    private static short[] magOut() {
        float[] f = fa(0.34f);
        addScrape(f, 0.0f, 0.12f, 0.22f, 0.1f, 0.25f);
        addMetalHit(f, 0.14f, 1700f, 0.5f, 55f, 0.6f);
        addThud(f, 0.14f, 180f, 0.25f, 40f);
        return fin(f, 0.55f);
    }
    private static short[] magIn() {
        float[] f = fa(0.4f);
        addScrape(f, 0.0f, 0.13f, 0.25f, 0.12f, 0.3f);
        addThud(f, 0.15f, 260f, 0.7f, 38f);
        addMetalHit(f, 0.15f, 1300f, 0.65f, 42f, 0.8f);
        addMetalHit(f, 0.22f, 2400f, 0.25f, 70f, 0.3f);
        return fin(f, 0.7f);
    }
    private static short[] bolt() {
        float[] f = fa(0.5f);
        addScrape(f, 0.0f, 0.1f, 0.3f, 0.08f, 0.35f);
        addMetalHit(f, 0.11f, 2100f, 0.45f, 60f, 0.5f);
        addScrape(f, 0.2f, 0.07f, 0.25f, 0.3f, 0.1f);
        addMetalHit(f, 0.28f, 1500f, 0.8f, 40f, 0.9f);
        addThud(f, 0.28f, 150f, 0.45f, 30f);
        return fin(f, 0.75f);
    }
    private static short[] pump() {
        float[] f = fa(0.6f);
        addScrape(f, 0.0f, 0.14f, 0.35f, 0.06f, 0.25f);
        addMetalHit(f, 0.15f, 900f, 0.55f, 34f, 0.7f);
        addMetalHit(f, 0.2f, 3100f, 0.18f, 90f, 0.2f); // cartucho cayendo
        addScrape(f, 0.3f, 0.1f, 0.3f, 0.25f, 0.06f);
        addMetalHit(f, 0.41f, 700f, 1.0f, 28f, 1.0f);
        addThud(f, 0.41f, 120f, 0.7f, 26f);
        return fin(f, 0.85f);
    }
    private static short[] shell() {
        float[] f = fa(0.45f);
        float[] at = {0.0f, 0.075f, 0.13f, 0.17f};
        float[] fq = {3900f, 4300f, 4700f, 5200f};
        for (int k = 0; k < 4; k++) addMetalHit(f, at[k], fq[k], 0.5f / (1 + k * 0.6f), 85f + k * 20f, 0.05f);
        return fin(f, 0.3f);
    }
    private static short[] cloth() {
        float[] f = fa(0.4f);
        addScrape(f, 0.0f, 0.28f, 0.5f, 0.05f, 0.05f);
        addMetalHit(f, 0.3f, 2000f, 0.2f, 90f, 0.2f);
        return fin(f, 0.4f);
    }
    private static short[] click(float sec, float f0) {
        float[] f = fa(sec);
        addMetalHit(f, 0f, f0, 0.8f, 70f, 0.8f);
        return fin(f, 0.5f);
    }

    // ------------------------------------------------------------------ voces
    /** Voz gutural: fuente glotal con temblor y chirrido de vocal, tres formantes que se deslizan, respiración y gorgoteo. */
    private static short[] vocal(float sec, float f0a, float f0b, float jit, float fry, float[] f1, float[] f2, float[] f3,
                                 float breath, float gurgle, float dist, float envPow) {
        float[] f = fa(sec);
        float ph = 0;
        float[][] y = new float[3][2];
        float[] bw = {0.987f, 0.982f, 0.975f};
        float[] st = new float[2];
        float j = 0; int per = 0; float lastPh = 0;
        for (int i = 0; i < f.length; i++) {
            float t = i / (float) RATE, u = t / sec;
            j += (nz() - j) * 0.002f;
            float fr = f0a + (f0b - f0a) * u + j * jit * f0a + sin(t * 34f) * jit * 4f;
            ph += fr / RATE;
            if (ph >= 1f) { ph -= 1f; per++; }
            // pulso glotal: subida suave y cierre brusco
            float g = ph < 0.55f ? sin(Math.min(1f, ph / 0.55f) * 1.5708f) : (1f - (ph - 0.55f) / 0.45f);
            g = g * 2f - 1f;
            float fryK = (per & 1) == 1 ? 1f - fry : 1f;
            float src = g * fryK + nz() * breath;
            float outs = 0;
            for (int k = 0; k < 3; k++) {
                float fc = (k == 0 ? f1 : k == 1 ? f2 : f3)[0] + ((k == 0 ? f1 : k == 1 ? f2 : f3)[1] - (k == 0 ? f1 : k == 1 ? f2 : f3)[0]) * u;
                float c = 2f * bw[k] * (float) Math.cos(TAU * fc / RATE);
                float o = src + c * y[k][0] - bw[k] * bw[k] * y[k][1];
                y[k][1] = y[k][0]; y[k][0] = o;
                outs += o * (k == 0 ? 0.0075f : k == 1 ? 0.0055f : 0.0025f);
            }
            float gur = 1f - gurgle + gurgle * (0.5f + 0.5f * sin(t * TAU * 24f + sin(t * 7f) * 2f));
            float env = (float) Math.pow(Math.sin(Math.PI * Math.pow(u, 0.55)), envPow);
            float v = outs * gur * env;
            if (dist > 0) v = (float) Math.tanh(v * (1f + dist * 5f)) / (1f + dist);
            f[i] = v;
        }
        echoes(f, new float[]{0.07f, 0.16f}, new float[]{0.12f, 0.07f}, 0.2f);
        return fin(f, 0.42f);
    }

    private static short[] flesh(boolean head) {
        float[] f = fa(head ? 0.3f : 0.26f);
        float[] st = new float[3];
        float ph = 0;
        for (int i = 0; i < f.length; i++) {
            float t = i / (float) RATE;
            float n = nz();
            // chapoteo: ruido medio modulado por burbujas
            float bub = 0.5f + 0.5f * sin(t * TAU * (38f + 60f * t) + sin(t * 90f));
            float wet = lp(st, 0, n, 0.16f) * ex(-t * 20f) * (0.5f + bub) * 1.2f;
            ph += TAU * (150f * ex(-t * 16f) + 55f) / RATE;
            float body = sin(ph) * ex(-t * 20f) * 0.9f;
            float slap = (n - lp(st, 1, n, 0.35f)) * ex(-t * 160f) * 0.6f;
            float v = wet + body + slap;
            if (head) {
                // golpe seco del hueso + resonancia aguda
                v += sin(TAU * 1250f * t) * ex(-t * 70f) * 0.35f + (n - lp(st, 2, n, 0.5f)) * ex(-t * 300f) * 0.9f;
            }
            f[i] = v;
        }
        return fin(f, head ? 0.9f : 0.75f);
    }

    private static short[] noiseBurst(float sec, float decay, float cut, float vol) {
        float[] f = fa(sec);
        float[] st = new float[2];
        for (int i = 0; i < f.length; i++) {
            float t = i / (float) RATE;
            f[i] = lp(st, 0, nz(), cut) * ex(-t * decay) * vol;
        }
        return fin(f, Math.min(0.9f, vol));
    }

    private static void build() {
        if (built) return;
        synchronized (Sfx.class) {
            if (built) return;
            // pistola: chasquido seco con eco corto; escopeta: estampido enorme y retumbo largo;
            // fusil: chasquido supersónico y cola larga; subfusil: ligero y corto
            bank[PISTOL] = gun(0.7f, 1.0f, 260f, 0.9f, 38f, 0.42f, 0.7f, 190f, 0.5f, 7f, 1.0f);
            bank[SHOTGUN] = gun(1.2f, 0.9f, 170f, 1.5f, 15f, 0.3f, 1.4f, 120f, 1.0f, 3.2f, 1.3f);
            bank[RIFLE] = gun(1.0f, 1.3f, 300f, 1.0f, 28f, 0.38f, 1.0f, 150f, 0.8f, 4.6f, 1.25f);
            bank[SMG] = gun(0.45f, 0.95f, 240f, 0.85f, 48f, 0.46f, 0.55f, 210f, 0.3f, 10f, 0.8f);
            bank[STORM] = storm();
            bank[RELOAD] = cloth();
            bank[SWITCH] = cloth();
            bank[MAG_OUT] = magOut();
            bank[MAG_IN] = magIn();
            bank[BOLT] = bolt();
            bank[PUMP] = pump();
            bank[SHELL] = shell();
            bank[HIT] = flesh(false);
            bank[FLESH] = flesh(false);
            bank[HEADSHOT] = flesh(true);
            // voces: vocales distintas (formantes F1/F2/F3 en Hz, inicio → fin)
            bank[GROAN1] = vocal(1.5f, 82f, 56f, 0.5f, 0.35f, new float[]{560, 400}, new float[]{980, 760}, new float[]{2500, 2300}, 0.30f, 0.30f, 0.15f, 0.9f);
            bank[GROAN2] = vocal(1.2f, 104f, 66f, 0.7f, 0.45f, new float[]{720, 520}, new float[]{1150, 900}, new float[]{2600, 2400}, 0.45f, 0.5f, 0.25f, 0.8f);
            bank[GROAN3] = vocal(1.9f, 66f, 46f, 0.4f, 0.5f, new float[]{480, 620}, new float[]{900, 1050}, new float[]{2400, 2500}, 0.55f, 0.35f, 0.10f, 1.0f);
            bank[SNARL] = vocal(0.9f, 118f, 82f, 1.0f, 0.55f, new float[]{760, 640}, new float[]{1250, 1050}, new float[]{2700, 2500}, 0.55f, 0.65f, 0.55f, 0.7f);
            bank[SCREAM] = vocal(1.1f, 310f, 190f, 0.9f, 0.2f, new float[]{880, 700}, new float[]{1500, 1250}, new float[]{3000, 2800}, 0.4f, 0.25f, 0.7f, 0.55f);
            bank[ZDIE] = vocal(1.3f, 135f, 36f, 1.0f, 0.5f, new float[]{700, 380}, new float[]{1200, 700}, new float[]{2600, 2200}, 0.5f, 0.75f, 0.35f, 0.6f);
            bank[SWIPE] = noiseBurst(0.3f, 9, 0.15f, 0.9f);
            // golpe al jugador
            {
                float[] f = fa(0.55f);
                float ph = 0;
                for (int i = 0; i < f.length; i++) {
                    float t = i / (float) RATE;
                    ph += TAU * (90 * ex(-t * 6) + 40) / RATE;
                    f[i] = (sin(ph) * 0.9f + nz() * 0.25f * ex(-t * 30) + sin(t * TAU * 1800) * 0.08f * ex(-t * 4)) * ex(-t * 7);
                }
                bank[HURT] = fin(f, 0.9f);
            }
            // caja registradora
            {
                float[] f = fa(0.6f);
                addMetalHit(f, 0f, 1568f, 0.5f, 9f, 0.05f);
                addMetalHit(f, 0.07f, 2349f, 0.5f, 7f, 0.05f);
                addThud(f, 0f, 200f, 0.4f, 30f);
                bank[CASH] = fin(f, 0.6f);
            }
            // puerta / escombros
            {
                float[] f = fa(1.8f);
                float[] st = new float[2];
                for (int i = 0; i < f.length; i++) {
                    float t = i / (float) RATE;
                    float rum = lp(st, 0, nz(), 0.03f) * 3.2f * ex(-t * 1.6f);
                    float deb = lp(st, 1, nz(), 0.5f) * ex(-t * 4) * (nz() > 0.6f ? 1.2f : 0.2f);
                    f[i] = rum + deb;
                }
                bank[DOOR] = fin(f, 0.85f);
            }
            // inicio de ronda: acorde grave menor
            {
                float[] f = fa(3.4f);
                float[] fr = {55f, 65.4f, 82.4f, 110f};
                for (int i = 0; i < f.length; i++) {
                    float t = i / (float) RATE;
                    float v = 0;
                    for (int k = 0; k < fr.length; k++) v += sin(t * TAU * fr[k] * (1f - 0.02f * t)) * (0.35f - k * 0.05f);
                    v += sin(t * TAU * 880 * (1f - 0.04f * t)) * 0.05f * ex(-t * 2);
                    f[i] = v * Math.min(1f, t * 4f) * ex(-t * 0.9f);
                }
                bank[ROUND] = fin(f, 0.7f);
            }
            // trueno: chasquido inicial, rodamiento largo y retumbos
            {
                float[] f = fa(5.2f);
                float[] st = new float[4];
                for (int i = 0; i < f.length; i++) {
                    float t = i / (float) RATE;
                    float n = nz();
                    float roll = (0.55f + 0.45f * sin(t * 8f + sin(t * 2.6f) * 2.5f)) * ex(-t * 0.75f);
                    float crk = (n - lp(st, 3, n, 0.3f)) * ex(-t * 28f) * 1.4f;
                    float v = lp(st, 0, n, 0.045f) * 4f * roll + lp(st, 1, n, 0.012f) * 7f * roll;
                    float far = t > 0.9f ? lp(st, 2, n, 0.02f) * 4f * ex(-(t - 0.9f) * 1.1f) * (0.5f + 0.5f * sin(t * 5.3f)) : 0f;
                    f[i] = v + crk + far;
                }
                bank[THUNDER] = fin(f, 0.9f);
            }
            bank[CRACK] = noiseBurst(0.4f, 11, 0.7f, 1.2f);
            // sin balas: clic seco
            {
                float[] f = fa(0.2f);
                addMetalHit(f, 0f, 1500f, 0.7f, 60f, 0.7f);
                addThud(f, 0f, 220f, 0.3f, 50f);
                bank[EMPTY] = fin(f, 0.5f);
            }
            // bonificación
            {
                float[] f = fa(1.0f);
                for (int i = 0; i < f.length; i++) {
                    float t = i / (float) RATE;
                    float fq = 440 * (float) Math.pow(2, Math.floor(t * 8) / 12f * 3.2f);
                    f[i] = sin(t * TAU * fq) * 0.35f * ex(-t * 2.4f) + sin(t * TAU * fq * 2) * 0.1f * ex(-t * 3);
                }
                bank[POWER] = fin(f, 0.7f);
            }
            // paso en barro húmedo: chapoteo + succión
            {
                float[] f = fa(0.24f);
                float[] st = new float[3];
                for (int i = 0; i < f.length; i++) {
                    float t = i / (float) RATE;
                    float n = nz();
                    f[i] = lp(st, 0, n, 0.1f) * ex(-t * 22f) * 0.9f + lp(st, 1, n, 0.35f) * ex(-t * 45f) * 0.4f
                            + sin(TAU * (90f - 120f * t) * t) * ex(-t * 30f) * 0.5f;
                }
                bank[STEP] = fin(f, 0.5f);
            }
            // caja misteriosa: melodía extraña
            {
                float[] f = fa(2.4f);
                float[] notes = {392, 466, 523, 587, 523, 466, 392, 349, 392, 466, 523, 622};
                for (int i = 0; i < f.length; i++) {
                    float t = i / (float) RATE;
                    int n = Math.min(notes.length - 1, (int) (t * 5));
                    float lt = t * 5 - n;
                    float env = ex(-lt * 3.5f) * 0.3f;
                    f[i] = (sin(t * TAU * notes[n]) + 0.4f * sin(t * TAU * notes[n] * 2)) * env;
                }
                bank[BOX] = fin(f, 0.6f);
            }
            // fin de partida: caída lenta
            {
                float[] f = fa(3.6f);
                for (int i = 0; i < f.length; i++) {
                    float t = i / (float) RATE;
                    float fq = 110 * (float) Math.pow(0.5, t * 0.5);
                    float v = sin(t * TAU * fq) + 0.6f * sin(t * TAU * fq * 1.5f) + 0.4f * sin(t * TAU * fq * 1.19f);
                    f[i] = v * 0.28f * ex(-t * 0.7f) * Math.min(1f, t * 6);
                }
                bank[END] = fin(f, 0.7f);
            }
            // reverberación (envío) por sonido
            for (int i = 0; i < NSND; i++) send[i] = 0.06f;
            send[PISTOL] = send[SMG] = 0.3f; send[SHOTGUN] = send[RIFLE] = 0.4f; send[STORM] = 0.35f;
            send[THUNDER] = 0.45f; send[ROUND] = 0.3f; send[END] = 0.3f; send[DOOR] = 0.3f;
            send[SCREAM] = send[SNARL] = 0.2f; send[GROAN1] = send[GROAN2] = send[GROAN3] = send[ZDIE] = 0.18f;
            send[STEP] = 0.02f; send[HIT] = send[FLESH] = send[HEADSHOT] = 0.1f;
            // lazos de ambiente
            {
                float[] r = fa(3f);
                float[] st = new float[3];
                for (int i = 0; i < r.length; i++) {
                    float t = i / (float) RATE;
                    float n = nz();
                    float v = lp(st, 0, n, 0.5f) * 0.5f + (n - lp(st, 1, n, 0.3f)) * 0.12f;
                    // gotas sueltas sobre charcos
                    if (((int) (nz() * 1e6f) & 1023) == 0) v += sin(t * TAU * (1800f + nz() * 900f)) * 0.25f;
                    r[i] = v;
                }
                rainLoop = new short[r.length];
                for (int i = 0; i < r.length; i++) rainLoop[i] = s16(r[i] * 0.5f);
                int fade = 3000;
                for (int i = 0; i < fade; i++) {
                    float a = i / (float) fade;
                    int j = rainLoop.length - fade + i;
                    rainLoop[j] = (short) (rainLoop[j] * (1 - a) + rainLoop[i] * a);
                }
                float[] w = fa(10f);
                float[] ws = new float[3];
                for (int i = 0; i < w.length; i++) {
                    float t = i / (float) RATE;
                    float mod = 0.5f + 0.5f * sin(t * TAU / 10f + 0.3f) * (0.6f + 0.4f * sin(t * TAU / 10f * 3));
                    float n = nz();
                    float body = lp(ws, 0, n, 0.02f + 0.03f * mod) * 5.5f * mod;
                    float howl = lp(ws, 1, n, 0.06f) - lp(ws, 2, n, 0.04f);
                    w[i] = body + howl * 6f * mod * mod * (0.6f + 0.4f * sin(t * 1.7f));
                }
                windLoop = new short[w.length];
                for (int i = 0; i < w.length; i++) windLoop[i] = s16(w[i]);
                droneLoop = new short[4 * RATE];
                for (int i = 0; i < droneLoop.length; i++) {
                    float t = i / (float) RATE;
                    float v = sin(t * TAU * 55f) * 0.5f + sin(t * TAU * 55.5f) * 0.5f
                            + 0.3f * sin(t * TAU * 82.5f) + 0.15f * sin(t * TAU * 110.25f);
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
        v.sd = send[id];
        float p = pan < -1 ? -1 : (pan > 1 ? 1 : pan);
        v.gl = vol * (float) Math.sqrt((1 - p) * 0.5f) * 1.41f;
        v.gr = vol * (float) Math.sqrt((1 + p) * 0.5f) * 1.41f;
        synchronized (voices) {
            if (voices.size() > 30) {
                for (int i = 0; i < voices.size(); i++) if (!voices.get(i).loop) { voices.remove(i); break; }
            }
            voices.add(v);
        }
    }

    // reverberación de Schroeder estéreo
    private static final int[] CL = {(int) (0.0297f * RATE), (int) (0.0371f * RATE), (int) (0.0411f * RATE), (int) (0.0437f * RATE)};
    private static final int[] CR = {(int) (0.0311f * RATE), (int) (0.0383f * RATE), (int) (0.0427f * RATE), (int) (0.0457f * RATE)};
    private final float[][] combL = new float[4][], combR = new float[4][];
    private final int[] ciL = new int[4], ciR = new int[4];
    private final float[] dampL = new float[4], dampR = new float[4];
    private final float[] apL = new float[(int) (0.0051f * RATE)], apR = new float[(int) (0.0043f * RATE)];
    private int apiL, apiR;

    private void mixLoop() {
        final int N = 512;
        float[] acc = new float[N * 2];
        float[] snd = new float[N];
        short[] out = new short[N * 2];
        for (int k = 0; k < 4; k++) { combL[k] = new float[CL[k]]; combR[k] = new float[CR[k]]; }
        try { track.play(); } catch (Throwable t) { return; }
        while (running) {
            for (int i = 0; i < acc.length; i++) acc[i] = 0;
            for (int i = 0; i < N; i++) snd[i] = 0;
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
                    float sd = v.sd;
                    for (int i = 0; i < N; i++) {
                        if (p >= d.length) {
                            if (v.loop) p = 0; else break;
                        }
                        float s = d[p++] / 32768f;
                        acc[i * 2] += s * gl;
                        acc[i * 2 + 1] += s * gr;
                        snd[i] += s * (gl + gr) * 0.5f * sd;
                    }
                    v.pos = p;
                    if (!v.loop && p >= d.length) voices.remove(k);
                }
            }
            float mute = muted ? 0f : 1f;
            for (int i = 0; i < N; i++) {
                float x = snd[i];
                float l = 0, r = 0;
                for (int k = 0; k < 4; k++) {
                    float[] cl = combL[k], cr = combR[k];
                    float yl = cl[ciL[k]], yr = cr[ciR[k]];
                    dampL[k] += 0.35f * (yl - dampL[k]); dampR[k] += 0.35f * (yr - dampR[k]);
                    cl[ciL[k]] = x + dampL[k] * 0.84f; cr[ciR[k]] = x + dampR[k] * 0.84f;
                    if (++ciL[k] >= cl.length) ciL[k] = 0;
                    if (++ciR[k] >= cr.length) ciR[k] = 0;
                    l += yl; r += yr;
                }
                float al = apL[apiL], ar = apR[apiR];
                float nl = l + al * 0.5f, nr = r + ar * 0.5f;
                apL[apiL] = nl; apR[apiR] = nr;
                l = al - nl * 0.5f; r = ar - nr * 0.5f;
                if (++apiL >= apL.length) apiL = 0;
                if (++apiR >= apR.length) apiR = 0;
                acc[i * 2] += l * 0.5f;
                acc[i * 2 + 1] += r * 0.5f;
            }
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
