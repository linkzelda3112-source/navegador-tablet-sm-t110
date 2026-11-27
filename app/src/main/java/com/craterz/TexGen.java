package com.craterz;

/** Texturas procedurales (difusa + mapa de normales con brillo en el canal alfa). Todo generado por código. */
public final class TexGen {
    public static final int N = 256;
    public static final int MUD = 0, BRICK = 1, WOOD = 2, METAL = 3, CONCRETE = 4, SAND = 5, BARK = 6, ZOMBIE = 7;
    public static final int COUNT = 8;

    private static final byte[][][] cache = new byte[COUNT][][];
    private static byte[] particleAtlas, cloudTex;
    private static boolean warming;

    private TexGen() {}

    /** Genera todo en un hilo aparte (se llama al abrir el menú, así el juego carga rápido). */
    public static synchronized void warmUp() {
        if (warming) return;
        warming = true;
        Thread t = new Thread(new Runnable() {
            public void run() {
                for (int i = 0; i < COUNT; i++) get(i);
                particles();
                clouds();
            }
        }, "texgen");
        t.setPriority(Thread.MIN_PRIORITY + 1);
        t.start();
    }

    public static synchronized byte[][] get(int id) {
        if (cache[id] == null) cache[id] = build(id);
        return cache[id];
    }

    public static synchronized byte[] particles() {
        if (particleAtlas == null) particleAtlas = buildParticles();
        return particleAtlas;
    }

    public static synchronized byte[] clouds() {
        if (cloudTex == null) cloudTex = buildClouds();
        return cloudTex;
    }

    // ------------------------------------------------------------------

    static final class Img {
        final int w, h;
        final float[] r, g, b, ht, sp;
        Img(int w, int h) {
            this.w = w; this.h = h;
            r = new float[w * h]; g = new float[w * h]; b = new float[w * h];
            ht = new float[w * h]; sp = new float[w * h];
        }
        void set(int x, int y, float cr, float cg, float cb, float height, float spec) {
            int i = y * w + x;
            r[i] = cr; g[i] = cg; b[i] = cb; ht[i] = height; sp[i] = spec;
        }
    }

    private static byte[][] build(int id) {
        Img m = new Img(N, N);
        float strength;
        switch (id) {
            case MUD: mud(m); strength = 5f; break;
            case BRICK: brick(m); strength = 6f; break;
            case WOOD: wood(m); strength = 5f; break;
            case METAL: metal(m); strength = 4f; break;
            case CONCRETE: concrete(m); strength = 5f; break;
            case SAND: sand(m); strength = 5f; break;
            case BARK: bark(m); strength = 6f; break;
            default: zombie(m); strength = 4f; break;
        }
        return finish(m, strength);
    }

    private static byte[][] finish(Img m, float strength) {
        int w = m.w, h = m.h;
        byte[] d = new byte[w * h * 4], nm = new byte[w * h * 4];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                d[i * 4] = q(m.r[i]); d[i * 4 + 1] = q(m.g[i]); d[i * 4 + 2] = q(m.b[i]); d[i * 4 + 3] = (byte) 255;
                int xl = (x - 1 + w) % w, xr = (x + 1) % w, yu = (y - 1 + h) % h, yd = (y + 1) % h;
                float dx = m.ht[y * w + xr] - m.ht[y * w + xl];
                float dy = m.ht[yd * w + x] - m.ht[yu * w + x];
                float nx = -dx * strength, ny = -dy * strength, nz = 1f;
                float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                nm[i * 4] = q(nx / l * 0.5f + 0.5f);
                nm[i * 4 + 1] = q(ny / l * 0.5f + 0.5f);
                nm[i * 4 + 2] = q(nz / l * 0.5f + 0.5f);
                nm[i * 4 + 3] = q(m.sp[i]);
            }
        }
        return new byte[][]{d, nm};
    }

    private static byte q(float v) {
        int i = (int) (v * 255f + 0.5f);
        return (byte) (i < 0 ? 0 : (i > 255 ? 255 : i));
    }

    private static float lastId;

    /** Distancia al punto de Voronoi más cercano (en celdas); lastId queda con el id de la celda. */
    private static float voronoi(float x, float y, int cells, int seed) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        float best = 1e9f;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int cx = Noise.mod(xi + dx, cells), cy = Noise.mod(yi + dy, cells);
                float px = xi + dx + Noise.rnd(cx, cy, seed);
                float py = yi + dy + Noise.rnd(cx, cy, seed + 7);
                float ddx = px - x, ddy = py - y;
                float dd = ddx * ddx + ddy * ddy;
                if (dd < best) { best = dd; lastId = Noise.rnd(cx, cy, seed + 13); }
            }
        }
        return (float) Math.sqrt(best);
    }

    private static float mix(float a, float b, float t) { return a + (b - a) * t; }
    private static float ss(float a, float b, float x) { return Noise.smoothstep(a, b, x); }

    // --------------------------------------------------------------- MUD
    private static void mud(Img m) {
        int n = m.w;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                float u = x / (float) n, v = y / (float) n;
                float h = 0.55f * Noise.fbmWrap(u, v, 6, 6, 5, 1) + 0.30f * Noise.fbmWrap(u, v, 16, 16, 3, 2)
                        + 0.15f * Noise.fbmWrap(u, v, 40, 40, 2, 3);
                float pud = ss(0.47f, 0.38f, Noise.fbmWrap(u, v, 3, 3, 3, 9));
                float d = voronoi(u * 22, v * 22, 22, 5);
                float pebbleOn = lastId < 0.22f ? 1f : 0f;
                float rad = 0.26f + lastId * 0.25f;
                float peb = ss(rad, rad * 0.55f, d) * pebbleOn;
                float cr = mix(0.13f, 0.36f, h), cg = mix(0.10f, 0.28f, h), cb = mix(0.07f, 0.19f, h);
                float moss = ss(0.60f, 0.72f, Noise.fbmWrap(u, v, 5, 5, 4, 7));
                cr = mix(cr, 0.17f, moss * 0.55f); cg = mix(cg, 0.24f, moss * 0.55f); cb = mix(cb, 0.09f, moss * 0.55f);
                float pg = 0.20f + lastId * 0.25f + Noise.rnd(x, y, 3) * 0.05f;
                cr = mix(cr, pg, peb); cg = mix(cg, pg * 0.97f, peb); cb = mix(cb, pg * 0.9f, peb);
                cr = mix(cr, cr * 0.7f, pud); cg = mix(cg, cg * 0.72f, pud); cb = mix(cb, cb * 0.78f, pud);
                float grain = 0.92f + 0.16f * Noise.rnd(x, y, 99);
                float ht = h * 0.9f + peb * 0.5f - pud * 0.35f;
                m.set(x, y, cr * grain, cg * grain, cb * grain, ht, 0.10f + pud * 0.85f + peb * 0.1f);
            }
        }
    }

    // ------------------------------------------------------------- BRICK
    private static void brick(Img m) {
        int n = m.w;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                float u = x / (float) n, v = y / (float) n;
                int row = y / 32, ox = (row & 1) * 32;
                int xx = (x + ox) % n;
                int bx = xx % 64, by = y % 32, bi = xx / 64;
                float wob = Noise.fbmWrap(u, v, 8, 8, 2, 4) * 2.6f - 1.3f;
                float e = Math.min(Math.min(bx, 64 - bx), Math.min(by, 32 - by)) + wob;
                float bev = ss(0.3f, 4.2f, e);
                float rv = Noise.rnd(bi, row, 11), rv2 = Noise.rnd(bi, row, 12);
                float cr = 0.40f + rv * 0.14f, cg = 0.17f + rv * 0.07f, cb = 0.12f + rv * 0.05f;
                if (rv2 < 0.22f) { cr = 0.30f + rv * 0.1f; cg = 0.25f + rv * 0.06f; cb = 0.22f + rv * 0.05f; }
                float grime = Noise.fbmWrap(u, v, 4, 4, 4, 5);
                float soot = ss(0.42f, 0.78f, grime);
                cr *= 1f - soot * 0.62f; cg *= 1f - soot * 0.58f; cb *= 1f - soot * 0.5f;
                float chip = Noise.fbmWrap(u, v, 24, 24, 2, 6);
                float mort = 0.26f + 0.08f * Noise.rnd(x, y, 21) + 0.08f * chip;
                float mr = mort * 1.05f, mg = mort, mb = mort * 0.9f;
                float fr = mix(mr, cr, bev), fg = mix(mg, cg, bev), fb = mix(mb, cb, bev);
                float moss = ss(0.66f, 0.78f, Noise.fbmWrap(u, v, 6, 6, 3, 8));
                fr = mix(fr, 0.18f, moss * 0.5f); fg = mix(fg, 0.26f, moss * 0.5f); fb = mix(fb, 0.12f, moss * 0.5f);
                float gr = 0.9f + 0.2f * Noise.rnd(x, y, 5);
                m.set(x, y, fr * gr, fg * gr, fb * gr, bev * (0.55f + 0.45f * chip) , 0.14f);
            }
        }
    }

    // -------------------------------------------------------------- WOOD
    private static void wood(Img m) {
        int n = m.w;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                float u = x / (float) n, v = y / (float) n;
                int plank = x / 64, px = x % 64;
                float pr = Noise.rnd(plank, 0, 3);
                float gr = Noise.fbmWrap(u, v, 48, 3, 3, 20 + plank);
                float rings = (float) Math.sin(px * 0.33f + gr * 10f + pr * 6f) * 0.5f + 0.5f;
                float weather = Noise.fbmWrap(u, v, 5, 5, 3, 24);
                float k = 0.55f + 0.45f * gr;
                float cr = 0.37f * k * (0.85f + 0.15f * rings) * (0.8f + 0.4f * pr);
                float cg = 0.28f * k * (0.85f + 0.15f * rings) * (0.8f + 0.4f * pr);
                float cb = 0.20f * k * (0.85f + 0.15f * rings) * (0.8f + 0.4f * pr);
                cr = mix(cr, 0.43f * k, weather * 0.6f); cg = mix(cg, 0.41f * k, weather * 0.6f); cb = mix(cb, 0.38f * k, weather * 0.6f);
                float ht = gr * 0.5f + rings * 0.2f;
                float gap = (px < 2 || px > 61) ? 1f : 0f;
                float kd = voronoi(u * 4, v * 4, 4, 31);
                if (kd < 0.2f) {
                    float ring = (float) Math.sin(kd * 70f) * 0.5f + 0.5f;
                    float kk = 1f - kd / 0.2f;
                    cr = mix(cr, 0.12f * (0.6f + ring * 0.4f), kk); cg = mix(cg, 0.08f * (0.6f + ring * 0.4f), kk); cb = mix(cb, 0.05f, kk);
                    ht -= kk * 0.2f;
                }
                int ny = y % 128;
                float dn = (float) Math.sqrt((px - 32) * (px - 32) + (ny - 20) * (ny - 20));
                if (dn < 3.2f) { cr = 0.1f; cg = 0.1f; cb = 0.11f; ht += 0.6f; }
                else if (dn < 5f) { cr *= 0.55f; cg *= 0.55f; cb *= 0.55f; }
                if (gap > 0) { cr = 0.03f; cg = 0.025f; cb = 0.02f; ht = -0.6f; }
                m.set(x, y, cr, cg, cb, ht, 0.16f);
            }
        }
    }

    // ------------------------------------------------------------- METAL
    private static void metal(Img m) {
        int n = m.w;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                float u = x / (float) n, v = y / (float) n;
                float k = 0.8f + 0.2f * Noise.fbmWrap(u, v, 8, 8, 3, 40);
                float cr = 0.30f * k, cg = 0.31f * k, cb = 0.33f * k;
                float rust = ss(0.48f, 0.68f, Noise.fbmWrap(u, v, 5, 5, 5, 41) + 0.15f * Noise.fbmWrap(u, v, 30, 30, 2, 42));
                float rk = Noise.fbmWrap(u, v, 12, 12, 3, 43);
                float rr = mix(0.38f, 0.56f, rk), rg = mix(0.16f, 0.28f, rk), rb = mix(0.06f, 0.10f, rk);
                cr = mix(cr, rr, rust); cg = mix(cg, rg, rust); cb = mix(cb, rb, rust);
                float corr = (float) Math.sin(x * 6.2831853f * 8f / n);
                float ht = 0.5f + 0.5f * corr * 0.5f + rust * 0.15f * Noise.rnd(x, y, 44);
                int rx = x % 32, ry = y % 64;
                float rd = (float) Math.sqrt((rx - 16) * (rx - 16) + (ry - 8) * (ry - 8));
                if (rd < 3f) { cr *= 1.25f; cg *= 1.25f; cb *= 1.25f; ht += 0.5f; }
                else if (rd < 4.2f) { cr *= 0.5f; cg *= 0.5f; cb *= 0.5f; }
                float sc = Noise.fbmWrap(u, v, 2, 64, 2, 45);
                if (sc > 0.66f) { cr += 0.08f; cg += 0.08f; cb += 0.08f; }
                cr *= 1f - corr * 0.06f; cg *= 1f - corr * 0.06f; cb *= 1f - corr * 0.06f;
                m.set(x, y, cr, cg, cb, ht, (1f - rust) * 0.55f + 0.08f);
            }
        }
    }

    // ---------------------------------------------------------- CONCRETE
    private static void concrete(Img m) {
        int n = m.w;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                float u = x / (float) n, v = y / (float) n;
                float h = Noise.fbmWrap(u, v, 8, 8, 5, 51);
                float k = 0.30f + h * 0.2f;
                float cr = k, cg = k, cb = k * 0.97f;
                float r = Math.abs(Noise.fbmWrap(u, v, 4, 4, 4, 52) - 0.5f);
                float crack = ss(0.008f, 0.0f, r);
                float stain = Noise.fbmWrap(u, v, 10, 1, 3, 53);
                float st = ss(0.5f, 0.85f, stain);
                cr *= 1f - st * 0.5f; cg *= 1f - st * 0.5f; cb *= 1f - st * 0.45f;
                float moss = ss(0.62f, 0.74f, Noise.fbmWrap(u, v, 6, 6, 3, 54));
                cr = mix(cr, 0.17f, moss * 0.6f); cg = mix(cg, 0.25f, moss * 0.6f); cb = mix(cb, 0.11f, moss * 0.6f);
                cr *= 1f - crack * 0.85f; cg *= 1f - crack * 0.85f; cb *= 1f - crack * 0.85f;
                float pit = Noise.value(x * 0.7f, y * 0.7f, 55);
                if (pit > 0.88f) { cr *= 0.6f; cg *= 0.6f; cb *= 0.6f; }
                float seam = (y % 128 == 0 || x % 128 == 0) ? 0.5f : 1f;
                float gr = (0.92f + 0.16f * Noise.rnd(x, y, 56)) * seam;
                m.set(x, y, cr * gr, cg * gr, cb * gr, h * 0.7f - crack * 0.9f - (seam < 1 ? 0.3f : 0f) - (pit > 0.88f ? 0.2f : 0f), 0.10f);
            }
        }
    }

    // -------------------------------------------------------------- SAND
    private static void sand(Img m) {
        int n = m.w;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                float u = x / (float) n, v = y / (float) n;
                int bx = x % 64, by = y % 32;
                int row = y / 32;
                int shift = (row & 1) * 32;
                int xx = (x + shift) % 64;
                float puff = (float) (Math.sin(Math.PI * (xx + 0.5f) / 64f) * Math.sin(Math.PI * (by + 0.5f) / 32f));
                float weave = (float) (Math.sin(x * 1.9f) * Math.sin(y * 1.9f)) * 0.5f + 0.5f;
                float dirt = Noise.fbmWrap(u, v, 6, 6, 4, 61);
                float k = (0.62f + 0.38f * weave) * (0.65f + 0.55f * dirt) * (0.6f + 0.4f * puff);
                float cr = 0.50f * k, cg = 0.43f * k, cb = 0.28f * k;
                float mud = ss(0.55f, 0.8f, Noise.fbmWrap(u, v, 3, 3, 3, 62));
                cr = mix(cr, 0.2f, mud * 0.6f); cg = mix(cg, 0.15f, mud * 0.6f); cb = mix(cb, 0.1f, mud * 0.6f);
                float seam = puff < 0.14f ? 0.45f : 1f;
                m.set(x, y, cr * seam, cg * seam, cb * seam, puff * 0.8f + weave * 0.12f, 0.06f);
            }
        }
    }

    // -------------------------------------------------------------- BARK
    private static void bark(Img m) {
        int n = m.w;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                float u = x / (float) n, v = y / (float) n;
                float rg = Noise.fbmWrap(u, v, 14, 2, 4, 71);
                float rg2 = Noise.fbmWrap(u, v, 30, 4, 2, 72);
                float ridge = Math.abs(rg - 0.5f) * 2f;
                float k = 0.55f + 0.7f * ridge;
                float cr = 0.17f * k, cg = 0.125f * k, cb = 0.09f * k;
                float lichen = ss(0.62f, 0.75f, Noise.fbmWrap(u, v, 5, 5, 3, 73));
                cr = mix(cr, 0.20f, lichen * 0.5f); cg = mix(cg, 0.24f, lichen * 0.5f); cb = mix(cb, 0.17f, lichen * 0.5f);
                float crack = ss(0.025f, 0.0f, Math.abs(rg2 - 0.5f));
                cr *= 1f - crack * 0.8f; cg *= 1f - crack * 0.8f; cb *= 1f - crack * 0.8f;
                m.set(x, y, cr, cg, cb, ridge * 0.9f - crack * 0.5f, 0.07f);
            }
        }
    }

    // ------------------------------------------------------------ ZOMBIE
    /** Atlas: mitad superior = piel putrefacta, mitad inferior = ropa hecha jirones. */
    private static void zombie(Img m) {
        int n = m.w, half = n / 2;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                float u = x / (float) n;
                if (y < half) {
                    float v = y / (float) half;
                    float mott = Noise.fbmWrap(u, v, 6, 3, 4, 71);
                    float cr = mix(0.55f, 0.43f, mott), cg = mix(0.58f, 0.42f, mott), cb = mix(0.50f, 0.34f, mott);
                    float bruise = ss(0.60f, 0.72f, Noise.fbmWrap(u, v, 5, 3, 4, 74));
                    cr = mix(cr, 0.34f, bruise * 0.7f); cg = mix(cg, 0.33f, bruise * 0.7f); cb = mix(cb, 0.48f, bruise * 0.7f);
                    float vr = Math.abs(Noise.fbmWrap(u, v, 8, 4, 3, 72) - 0.5f);
                    float vein = ss(0.04f, 0.005f, vr);
                    cr = mix(cr, 0.28f, vein * 0.8f); cg = mix(cg, 0.22f, vein * 0.8f); cb = mix(cb, 0.34f, vein * 0.8f);
                    float blood = ss(0.64f, 0.70f, Noise.fbmWrap(u, v, 10, 5, 3, 73));
                    cr = mix(cr, 0.30f, blood * 0.85f); cg = mix(cg, 0.04f, blood * 0.85f); cb = mix(cb, 0.03f, blood * 0.85f);
                    float dirt = 0.75f + 0.3f * Noise.fbmWrap(u, v, 4, 2, 4, 75);
                    float pore = Noise.rnd(x, y, 76);
                    m.set(x, y, cr * dirt, cg * dirt, cb * dirt, mott * 0.3f + pore * 0.1f + vein * 0.25f - blood * 0.1f, 0.35f);
                } else {
                    int ly = y - half;
                    float v = ly / (float) half;
                    float weave = (float) (Math.sin(x * 1.7f) * Math.sin(ly * 1.7f)) * 0.5f + 0.5f;
                    float t = Noise.fbmWrap(u, v, 5, 3, 4, 81);
                    float cr = mix(0.18f, 0.30f, t), cg = mix(0.20f, 0.29f, t), cb = mix(0.14f, 0.2f, t);
                    float k = 0.7f + 0.3f * weave;
                    cr *= k; cg *= k; cb *= k;
                    float stain = ss(0.60f, 0.68f, Noise.fbmWrap(u, v, 7, 4, 3, 82));
                    cr = mix(cr, 0.28f, stain * 0.8f); cg = mix(cg, 0.05f, stain * 0.8f); cb = mix(cb, 0.04f, stain * 0.8f);
                    float mud = ss(0.5f, 0.75f, Noise.fbmWrap(u, v, 3, 2, 3, 83));
                    cr = mix(cr, 0.16f, mud * 0.5f); cg = mix(cg, 0.12f, mud * 0.5f); cb = mix(cb, 0.08f, mud * 0.5f);
                    float hole = ss(0.70f, 0.78f, Noise.fbmWrap(u, v, 7, 3, 3, 84));
                    cr *= 1f - hole * 0.8f; cg *= 1f - hole * 0.8f; cb *= 1f - hole * 0.8f;
                    float seam = (ly == 63 || ly == 64) ? 0.55f : 1f;
                    m.set(x, y, cr * seam, cg * seam, cb * seam, weave * 0.5f - hole * 0.6f, 0.05f);
                }
            }
        }
    }

    // ------------------------------------------------- partículas / nubes
    private static byte[] buildParticles() {
        int n = 128, c = 64;
        byte[] d = new byte[n * n * 4];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                int cx = x / c, cy = y / c;
                float lx = (x % c + 0.5f) / c * 2f - 1f, ly = (y % c + 0.5f) / c * 2f - 1f;
                float r = (float) Math.sqrt(lx * lx + ly * ly);
                float a = 0;
                int cell = cy * 2 + cx;
                if (cell == 0) { // humo / bocanada
                    float nz = Noise.fbm(x * 0.12f, y * 0.12f, 4, 90);
                    a = ss(1f, 0.15f, r) * (0.55f + 0.7f * nz);
                } else if (cell == 1) { // chispa
                    a = (float) (Math.exp(-r * r * 9.0) * 0.9 + Math.exp(-r * r * 2.2) * 0.25);
                } else if (cell == 2) { // lluvia (franja vertical)
                    float w = (float) Math.exp(-lx * lx * 40.0);
                    float f = ss(1f, 0.6f, Math.abs(ly));
                    a = w * f;
                } else { // anillo
                    float rr = Math.abs(r - 0.78f);
                    a = ss(0.16f, 0.0f, rr) * ss(1f, 0.85f, r);
                }
                a = Noise.clamp(a, 0f, 1f);
                int i = (y * n + x) * 4;
                d[i] = (byte) 255; d[i + 1] = (byte) 255; d[i + 2] = (byte) 255; d[i + 3] = q(a);
            }
        }
        return d;
    }

    private static byte[] buildClouds() {
        int n = 256;
        byte[] d = new byte[n * n * 4];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                float u = x / (float) n, v = y / (float) n;
                float c = Noise.fbmWrap(u, v, 4, 4, 6, 95);
                int i = (y * n + x) * 4;
                byte b = q(c);
                d[i] = b; d[i + 1] = b; d[i + 2] = b; d[i + 3] = (byte) 255;
            }
        }
        return d;
    }
}
