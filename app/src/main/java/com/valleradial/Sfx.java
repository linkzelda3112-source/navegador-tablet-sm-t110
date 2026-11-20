package com.valleradial;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.util.Random;

/** Efectos de sonido sintetizados (sin archivos). */
final class Sfx implements Game.Events {
    private static final int RATE = 22050;
    private final AudioTrack shoot, hook, hit, hurt, kill;

    Sfx() {
        Random r = new Random(1);
        // disparo: chasquido con caida de tono
        short[] s = new short[(int) (RATE * 0.14f)];
        for (int i = 0; i < s.length; i++) {
            float t = i / (float) RATE, e = (float) Math.exp(-t * 28f);
            float tone = (float) Math.sin(2 * Math.PI * (700f - 4200f * t) * t);
            s[i] = (short) (((r.nextFloat() * 2 - 1) * 0.6f + tone * 0.5f) * e * 14000);
        }
        shoot = make(s);
        // gancho: "fiuuu" ascendente
        s = new short[(int) (RATE * 0.22f)];
        for (int i = 0; i < s.length; i++) {
            float t = i / (float) RATE, e = (float) Math.sin(Math.PI * i / s.length);
            float f = 500f + 2600f * t;
            s[i] = (short) (((float) Math.sin(2 * Math.PI * f * t) * 0.4f + (r.nextFloat() * 2 - 1) * 0.25f) * e * 9000);
        }
        hook = make(s);
        // golpe a enemigo
        s = new short[(int) (RATE * 0.1f)];
        for (int i = 0; i < s.length; i++) {
            float t = i / (float) RATE, e = (float) Math.exp(-t * 35f);
            s[i] = (short) ((float) Math.sin(2 * Math.PI * 180f * t) * e * 16000);
        }
        hit = make(s);
        // dano al jugador
        s = new short[(int) (RATE * 0.28f)];
        for (int i = 0; i < s.length; i++) {
            float t = i / (float) RATE, e = (float) Math.exp(-t * 9f);
            float f = 320f - 700f * t;
            s[i] = (short) ((float) Math.sin(2 * Math.PI * f * t) * e * 15000);
        }
        hurt = make(s);
        // enemigo eliminado: dos notas
        s = new short[(int) (RATE * 0.3f)];
        for (int i = 0; i < s.length; i++) {
            float t = i / (float) RATE, e = (float) Math.exp(-t * 8f);
            float f = t < 0.12f ? 520f : 780f;
            s[i] = (short) ((float) Math.sin(2 * Math.PI * f * t) * e * 12000);
        }
        kill = make(s);
    }

    private static AudioTrack make(short[] data) {
        try {
            AudioTrack t = new AudioTrack(AudioManager.STREAM_MUSIC, RATE, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, data.length * 2, AudioTrack.MODE_STATIC);
            t.write(data, 0, data.length);
            return t;
        } catch (Throwable e) {
            return null;
        }
    }

    private static void play(AudioTrack t) {
        if (t == null) return;
        try {
            t.stop();
            t.reloadStaticData();
            t.play();
        } catch (Throwable ignored) {
        }
    }

    void release() {
        for (AudioTrack t : new AudioTrack[]{shoot, hook, hit, hurt, kill}) {
            if (t != null) {
                try { t.release(); } catch (Throwable ignored) { }
            }
        }
    }

    @Override public void onShoot() { play(shoot); }
    @Override public void onHook() { play(hook); }
    @Override public void onHit() { play(hit); }
    @Override public void onHurt() { play(hurt); }
    @Override public void onKill() { play(kill); }
}
