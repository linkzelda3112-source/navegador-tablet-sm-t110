package com.kiarena;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.util.Random;

/** Efectos de sonido sintetizados en código (sin archivos de audio). */
final class Sfx {
    static final int SELECT = 0, HIT = 1, BLAST = 2, ULTI = 3, HEAL = 4, GUARD = 5, WIN = 6, LOSE = 7, KO = 8, BUFF = 9;
    private static final int RATE = 22050;

    private final AudioTrack[] tracks = new AudioTrack[10];
    boolean enabled = true;

    Sfx() {
        try {
            set(SELECT, tone(600, 950, 0.09f, 0.5f, 0f, false));
            set(HIT, tone(220, 60, 0.2f, 0.9f, 0.6f, true));
            set(BLAST, tone(280, 950, 0.32f, 0.6f, 0.2f, false));
            set(ULTI, tone(90, 1300, 0.9f, 0.7f, 0.25f, false));
            set(HEAL, join(tone(520, 520, 0.1f, 0.5f, 0f, false), tone(660, 660, 0.1f, 0.5f, 0f, false),
                    tone(880, 880, 0.18f, 0.5f, 0f, false)));
            set(GUARD, tone(260, 190, 0.14f, 0.7f, 0.1f, true));
            set(WIN, join(tone(523, 523, 0.14f, 0.6f, 0f, false), tone(659, 659, 0.14f, 0.6f, 0f, false),
                    tone(784, 784, 0.14f, 0.6f, 0f, false), tone(1047, 1047, 0.4f, 0.6f, 0f, false)));
            set(LOSE, tone(400, 90, 0.8f, 0.6f, 0.05f, true));
            set(KO, tone(320, 40, 0.55f, 0.8f, 0.4f, true));
            set(BUFF, tone(300, 700, 0.35f, 0.5f, 0f, false));
        } catch (Throwable ignored) {
            // si el audio falla, el juego sigue sin sonido
        }
    }

    private void set(int id, short[] pcm) {
        try {
            AudioTrack t = new AudioTrack(AudioManager.STREAM_MUSIC, RATE, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, pcm.length * 2, AudioTrack.MODE_STATIC);
            t.write(pcm, 0, pcm.length);
            tracks[id] = t;
        } catch (Throwable ignored) {
            tracks[id] = null;
        }
    }

    void play(int id) {
        if (!enabled) return;
        try {
            AudioTrack t = tracks[id];
            if (t == null) return;
            t.stop();
            t.reloadStaticData();
            t.play();
        } catch (Throwable ignored) {
        }
    }

    void release() {
        for (int i = 0; i < tracks.length; i++) {
            try {
                if (tracks[i] != null) tracks[i].release();
            } catch (Throwable ignored) {
            }
            tracks[i] = null;
        }
    }

    private static short[] join(short[]... parts) {
        int n = 0;
        for (short[] a : parts) n += a.length;
        short[] out = new short[n];
        int o = 0;
        for (short[] a : parts) {
            System.arraycopy(a, 0, out, o, a.length);
            o += a.length;
        }
        return out;
    }

    private static short[] tone(float f0, float f1, float dur, float vol, float noise, boolean square) {
        int n = Math.max(1, (int) (RATE * dur));
        short[] out = new short[n];
        Random r = new Random(5);
        double phase = 0;
        for (int i = 0; i < n; i++) {
            float k = i / (float) n;
            double f = f0 + (f1 - f0) * k;
            phase += 2 * Math.PI * f / RATE;
            double v = square ? (Math.sin(phase) >= 0 ? 0.6 : -0.6) : Math.sin(phase);
            v = v * (1 - noise) + (r.nextFloat() * 2 - 1) * noise;
            double env = Math.min(1.0, i / 200.0) * Math.pow(1 - k, 1.4);
            out[i] = (short) (v * env * vol * 22000);
        }
        return out;
    }
}
