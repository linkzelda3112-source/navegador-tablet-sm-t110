package com.craterz;

/** Ruido de valor determinista (con variantes que se repiten sin costuras para texturas). */
public final class Noise {
    private Noise() {}

    public static int hash(int x, int y, int s) {
        int h = x * 374761393 + y * 668265263 + s * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return h;
    }

    public static float rnd(int x, int y, int s) {
        return (hash(x, y, s) & 0xFFFFFF) / 16777216f;
    }

    static float sm(float t) { return t * t * (3 - 2 * t); }

    public static float value(float x, float y, int s) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        float fx = sm(x - xi), fy = sm(y - yi);
        float a = rnd(xi, yi, s), b = rnd(xi + 1, yi, s), c = rnd(xi, yi + 1, s), d = rnd(xi + 1, yi + 1, s);
        return (a + (b - a) * fx) + ((c + (d - c) * fx) - (a + (b - a) * fx)) * fy;
    }

    public static float fbm(float x, float y, int oct, int s) {
        float a = 0.5f, sum = 0, norm = 0;
        for (int i = 0; i < oct; i++) {
            sum += a * value(x, y, s + i * 17);
            norm += a;
            x *= 2; y *= 2; a *= 0.5f;
        }
        return sum / norm;
    }

    /** Ruido que se repite con periodo px,py (en celdas de la malla). */
    public static float valueWrap(float x, float y, int px, int py, int s) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        float fx = sm(x - xi), fy = sm(y - yi);
        int x0 = mod(xi, px), x1 = mod(xi + 1, px), y0 = mod(yi, py), y1 = mod(yi + 1, py);
        float a = rnd(x0, y0, s), b = rnd(x1, y0, s), c = rnd(x0, y1, s), d = rnd(x1, y1, s);
        float top = a + (b - a) * fx, bot = c + (d - c) * fx;
        return top + (bot - top) * fy;
    }

    /** fbm repetible: u,v en [0,1); px,py celdas base por eje. */
    public static float fbmWrap(float u, float v, int px, int py, int oct, int s) {
        float a = 0.5f, sum = 0, norm = 0;
        for (int i = 0; i < oct; i++) {
            sum += a * valueWrap(u * px, v * py, px, py, s + i * 31);
            norm += a;
            px *= 2; py *= 2; a *= 0.5f;
        }
        return sum / norm;
    }

    static int mod(int a, int n) {
        int r = a % n;
        return r < 0 ? r + n : r;
    }

    public static float smoothstep(float a, float b, float x) {
        float t = (x - a) / (b - a);
        if (t < 0) t = 0; else if (t > 1) t = 1;
        return t * t * (3 - 2 * t);
    }

    public static float clamp(float v, float a, float b) { return v < a ? a : (v > b ? b : v); }

    public static float mix(float a, float b, float t) { return a + (b - a) * t; }
}
