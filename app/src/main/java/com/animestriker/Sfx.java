package com.animestriker;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import java.util.Random;

/** Efectos de sonido y música sintetizados por código (sin archivos de audio). */
final class Sfx {
    static final int RATE = 22050;
    static final int KICK = 0, PASS = 1, WHISTLE = 2, GOAL = 3, SAVE = 4, POST = 5, SPECIAL = 6,
            TACKLE = 7, CLICK = 8, BOUNCE = 9, N = 10;

    boolean sound = true, music = true;
    private final AudioTrack[] clips = new AudioTrack[N];
    private AudioTrack musicTrack, crowdTrack;
    private final Random rnd = new Random(5);

    void init() {
        try {
            clips[KICK] = make(kick(0.16f, 130f, 1f), 1f);
            clips[PASS] = make(kick(0.11f, 170f, 0.55f), 1f);
            clips[WHISTLE] = make(whistle(), 0.8f);
            clips[GOAL] = make(goal(), 1f);
            clips[SAVE] = make(kick(0.2f, 90f, 0.9f), 1f);
            clips[POST] = make(post(), 1f);
            clips[SPECIAL] = make(special(), 1f);
            clips[TACKLE] = make(kick(0.14f, 70f, 0.8f), 1f);
            clips[CLICK] = make(click(), 0.8f);
            clips[BOUNCE] = make(kick(0.07f, 220f, 0.35f), 0.7f);
            crowdTrack = make(crowd(), 0.22f); crowdTrack.setLoopPoints(0, crowdLen, -1);
            musicTrack = make(song(), 0.35f); musicTrack.setLoopPoints(0, songLen, -1);
        } catch (Throwable t) { sound = false; music = false; }
    }

    private int crowdLen, songLen;

    private AudioTrack make(short[] d, float vol) {
        AudioTrack t = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(d.length * 2).setTransferMode(AudioTrack.MODE_STATIC).build();
        t.write(d, 0, d.length);
        t.setVolume(vol);
        return t;
    }

    void play(int id) {
        if (!sound || id < 0 || id >= N || clips[id] == null) return;
        try { AudioTrack t = clips[id]; t.stop(); t.reloadStaticData(); t.play(); } catch (Throwable ignored) { }
    }

    void startAmbient(boolean on) {
        try {
            if (crowdTrack != null) { if (on && sound) crowdTrack.play(); else { crowdTrack.pause(); } }
        } catch (Throwable ignored) { }
    }

    void startMusic(boolean on) {
        try {
            if (musicTrack != null) { if (on && music) musicTrack.play(); else musicTrack.pause(); }
        } catch (Throwable ignored) { }
    }

    void release() {
        for (AudioTrack t : clips) if (t != null) try { t.release(); } catch (Throwable ignored) { }
        try { if (musicTrack != null) musicTrack.release(); if (crowdTrack != null) crowdTrack.release(); } catch (Throwable ignored) { }
    }

    // ---------------------------------------------------------------- síntesis
    private static short s(float v) { return (short) (Math.max(-1f, Math.min(1f, v)) * 30000f); }

    private short[] kick(float dur, float f0, float gain) {
        int n = (int) (RATE * dur); short[] d = new short[n]; float lp = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) RATE, env = (float) Math.exp(-t * (28f / dur * 0.25f));
            lp += (rnd.nextFloat() * 2f - 1f - lp) * 0.35f;
            float v = (float) Math.sin(6.2832 * (f0 * (1f - t * 2f)) * t) * 0.8f + lp * 0.6f;
            d[i] = s(v * env * gain);
        }
        return d;
    }

    private short[] click() {
        int n = (int) (RATE * 0.07f); short[] d = new short[n];
        for (int i = 0; i < n; i++) { float t = i / (float) RATE; d[i] = s((float) Math.sin(6.2832 * 1100 * t) * (float) Math.exp(-t * 60f) * 0.6f); }
        return d;
    }

    private short[] whistle() {
        int n = (int) (RATE * 0.55f); short[] d = new short[n];
        for (int i = 0; i < n; i++) {
            float t = i / (float) RATE, f = 2900f + 120f * (float) Math.sin(6.2832 * 28f * t);
            float env = Math.min(1f, t * 40f) * Math.min(1f, (0.55f - t) * 20f);
            float v = (float) Math.sin(6.2832 * f * t) * 0.6f + (rnd.nextFloat() - 0.5f) * 0.12f;
            d[i] = s(v * env * 0.7f);
        }
        return d;
    }

    private short[] post() {
        int n = (int) (RATE * 0.5f); short[] d = new short[n];
        for (int i = 0; i < n; i++) {
            float t = i / (float) RATE, env = (float) Math.exp(-t * 9f);
            float v = (float) (Math.sin(6.2832 * 820 * t) + 0.6 * Math.sin(6.2832 * 1490 * t) + 0.4 * Math.sin(6.2832 * 2310 * t));
            d[i] = s(v * env * 0.4f);
        }
        return d;
    }

    private short[] special() {
        int n = (int) (RATE * 0.9f); short[] d = new short[n]; float lp = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) RATE, k = t / 0.9f;
            lp += (rnd.nextFloat() * 2f - 1f - lp) * (0.05f + 0.5f * k);
            float f = 300f + 1800f * k * k;
            float v = lp * 0.9f * k + (float) Math.sin(6.2832 * f * t) * 0.35f * (1f - k * 0.5f);
            if (k > 0.8f) v += (float) Math.sin(6.2832 * 1760 * t) * 0.3f * (1f - k) * 5f;
            d[i] = s(v * Math.min(1f, (0.9f - t) * 8f) * 0.8f);
        }
        return d;
    }

    private short[] goal() {
        int n = (int) (RATE * 2.6f); short[] d = new short[n]; float lp = 0, lp2 = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) RATE;
            float env = Math.min(1f, t * 3f) * Math.min(1f, (2.6f - t) * 1.2f);
            float nz = rnd.nextFloat() * 2f - 1f;
            lp += (nz - lp) * 0.22f; lp2 += (lp - lp2) * 0.3f;
            float horn = (t < 1.1f) ? (float) (Math.sin(6.2832 * 440 * t) + 0.5 * Math.sin(6.2832 * 880 * t)) * 0.18f * Math.min(1f, (1.1f - t) * 6f) : 0f;
            float bell = (float) Math.sin(6.2832 * (t < 0.2f ? 784 : t < 0.4f ? 988 : 1319) * t) * 0.12f * (float) Math.exp(-((t % 0.2f)) * 9f) * (t < 1.0f ? 1f : 0f);
            d[i] = s((lp2 * 1.6f * env) + horn + bell);
        }
        return d;
    }

    private short[] crowd() {
        int n = RATE * 3; short[] d = new short[n]; float lp = 0, lp2 = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) RATE;
            lp += ((rnd.nextFloat() * 2f - 1f) - lp) * 0.08f; lp2 += (lp - lp2) * 0.1f;
            float swell = 0.65f + 0.35f * (float) Math.sin(6.2832 * t / 3f * 2f);
            d[i] = s(lp2 * 2.2f * swell);
        }
        crowdLen = n;
        return d;
    }

    private short[] song() {
        // 8 compases, 4 acordes (Am F C G), arpegios pentatónicos, bajo y percusión
        float bpm = 148f, step = 60f / bpm / 2f;
        int steps = 64, n = (int) (steps * step * RATE);
        float[] mix = new float[n];
        int[][] chords = {{57, 60, 64}, {53, 57, 60}, {48, 52, 55}, {55, 59, 62}};
        int[] melody = {0, 2, 4, 2, 5, 4, 2, 0, 4, 5, 7, 5, 4, 2, 4, 0};
        int[] scale = {69, 72, 74, 76, 79, 81, 84, 86};
        for (int st = 0; st < steps; st++) {
            int bar = st / 8, ch = bar % 4, pos = st % 8;
            int off = (int) (st * step * RATE);
            // melodía
            int note = scale[melody[(st + (bar / 4) * 3) % 16] % scale.length];
            if (st % 2 == 0 || pos % 3 == 0) tone(mix, off, step * 0.95f, midi(note), 0.13f, 0.5f);
            // arpegio
            int[] cc = chords[ch];
            tone(mix, off, step * 0.8f, midi(cc[pos % 3] + 12), 0.07f, 0.25f);
            // bajo
            if (pos % 2 == 0) tone(mix, off, step * 1.6f, midi(cc[0] - 12), 0.2f, 0.0f);
            // bombo y caja
            if (pos % 4 == 0) drum(mix, off, 0.14f, 70f, 0.4f, false);
            if (pos % 4 == 2) drum(mix, off, 0.1f, 200f, 0.18f, true);
            if (pos % 2 == 1) drum(mix, off, 0.03f, 0f, 0.05f, true);
        }
        short[] d = new short[n];
        for (int i = 0; i < n; i++) d[i] = s(mix[i]);
        songLen = n;
        return d;
    }

    private static float midi(int m) { return 440f * (float) Math.pow(2, (m - 69) / 12f); }

    private void tone(float[] mix, int off, float dur, float f, float amp, float duty) {
        int n = (int) (dur * RATE);
        for (int i = 0; i < n && off + i < mix.length; i++) {
            float t = i / (float) RATE, ph = (t * f) % 1f;
            float w = duty > 0 ? (ph < duty ? 1f : -1f) : (ph < 0.5f ? 4f * ph - 1f : 3f - 4f * ph);
            float env = Math.min(1f, t * 200f) * (float) Math.exp(-t * (3f / dur) * 0.6f) * Math.min(1f, (dur - t) * 60f);
            mix[off + i] += w * amp * env;
        }
    }

    private void drum(float[] mix, int off, float dur, float f0, float amp, boolean noise) {
        int n = (int) (dur * RATE);
        for (int i = 0; i < n && off + i < mix.length; i++) {
            float t = i / (float) RATE, env = (float) Math.exp(-t * 8f / dur * 0.5f);
            float v = noise ? (rnd.nextFloat() * 2f - 1f) : (float) Math.sin(6.2832 * (f0 * (1f - t * 4f)) * t);
            mix[off + i] += v * amp * env;
        }
    }
}
