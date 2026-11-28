package com.craterz;

/** Texturas procedurales (difusa + mapa de normales con brillo en el canal alfa). Todo generado por código. */
public final class TexGen {
    public static final int N = 256;
    public static final int MUD = 0, BRICK = 1, WOOD = 2, METAL = 3, CONCRETE = 4, SAND = 5, BARK = 6, ZOMBIE = 7, GUNMETAL = 8, WALNUT = 9, TEMPEST = 10, SCIFI = 11;
    public static final int COUNT = 12;
    public static final int ZN = 512;

    public static int size(int id) { return id == ZOMBIE ? ZN : N; }

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
        Img m = new Img(size(id), size(id));
        float strength;
        switch (id) {
            case MUD: mud(m); strength = 5f; break;
            case BRICK: brick(m); strength = 6f; break;
            case WOOD: wood(m); strength = 5f; break;
            case METAL: metal(m); strength = 4f; break;
            case CONCRETE: concrete(m); strength = 5f; break;
            case SAND: sand(m); strength = 5f; break;
            case BARK: bark(m); strength = 6f; break;
            case GUNMETAL: gunmetal(m); strength = 3f; break;
            case WALNUT: walnut(m); strength = 3f; break;
            case TEMPEST: tempest(m); strength = 2f; break;
            case SCIFI: scifi(m); strength = 4f; break;
            default: zombie(m); strength = 5f; break;
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
    /** Atlas 512: A=piel, B=cara, C=tela de uniforme, D=cuero (izq.) y acero de casco (der.). */
    private static void zombie(Img m) {
        int n = m.w, h = n / 2;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                int qx = x / h, qy = y / h;
                float lu = (x % h) / (float) h, lv = (y % h) / (float) h;
                if (qy == 0) skin(m, x, y, lu, lv, qx == 1);
                else if (qx == 0) cloth(m, x, y, lu, lv);
                else leatherMetal(m, x, y, lu, lv);
            }
        }
    }

    private static float g3(float x, float y, float cx, float cy, float sx, float sy) {
        float dx = (x - cx) / sx, dy = (y - cy) / sy;
        return (float) Math.exp(-(dx * dx + dy * dy));
    }

    private static void skin(Img m, int x, int y, float lu, float lv, boolean face) {
        float mott = Noise.fbmWrap(lu, lv, 5, 5, 5, 71);
        float liv = ss(0.50f, 0.72f, Noise.fbmWrap(lu, lv, 4, 4, 4, 74));
        float necro = ss(0.55f, 0.74f, Noise.fbmWrap(lu, lv, 6, 6, 4, 77));
        float vr = Math.abs(Noise.fbmWrap(lu, lv, 9, 9, 4, 72) - 0.5f);
        float vein = ss(0.030f, 0.003f, vr);
        float wn = Noise.fbmWrap(lu, lv, 5, 5, 5, 73);
        float wound = ss(0.715f, 0.75f, wn);
        float rim = ss(0.68f, 0.715f, wn) * (1f - wound);
        float scab = ss(0.63f, 0.66f, Noise.fbmWrap(lu, lv, 12, 12, 3, 78)) * (1f - wound);
        float pore = Noise.rnd(x, y, 76);
        float cr = mix(0.50f, 0.64f, mott), cg = mix(0.52f, 0.63f, mott), cb = mix(0.45f, 0.54f, mott);
        cr = mix(cr, 0.34f, liv * 0.7f); cg = mix(cg, 0.31f, liv * 0.7f); cb = mix(cb, 0.44f, liv * 0.7f);
        cr = mix(cr, 0.46f, necro * 0.55f); cg = mix(cg, 0.52f, necro * 0.55f); cb = mix(cb, 0.26f, necro * 0.55f);
        cr = mix(cr, 0.28f, vein * 0.75f); cg = mix(cg, 0.22f, vein * 0.75f); cb = mix(cb, 0.38f, vein * 0.75f);
        cr = mix(cr, 0.22f, scab * 0.8f); cg = mix(cg, 0.10f, scab * 0.8f); cb = mix(cb, 0.07f, scab * 0.8f);
        cr = mix(cr, 0.24f, rim * 0.8f); cg = mix(cg, 0.07f, rim * 0.8f); cb = mix(cb, 0.06f, rim * 0.8f);
        float wm = Noise.fbmWrap(lu, lv, 14, 14, 3, 79);
        cr = mix(cr, 0.42f + 0.12f * wm, wound); cg = mix(cg, 0.05f + 0.05f * wm, wound); cb = mix(cb, 0.05f, wound);
        float ht = mott * 0.22f + pore * 0.05f + vein * 0.22f + scab * 0.35f - wound * 0.5f - rim * 0.1f;
        float sp = 0.26f + wound * 0.55f + vein * 0.06f - scab * 0.2f;
        if (face) {
            // ojos hundidos, boca, goteo de sangre, cabello
            float eyeL = g3(lu, lv, 0.448f, 0.40f, 0.045f, 0.055f), eyeR = g3(lu, lv, 0.552f, 0.40f, 0.045f, 0.055f);
            float eye = Math.min(1f, eyeL + eyeR);
            cr = mix(cr, 0.16f, eye * 0.85f); cg = mix(cg, 0.10f, eye * 0.85f); cb = mix(cb, 0.14f, eye * 0.85f);
            float cheek = Math.max(g3(lu, lv, 0.40f, 0.56f, 0.06f, 0.08f), g3(lu, lv, 0.60f, 0.56f, 0.06f, 0.08f));
            cr *= 1f - cheek * 0.22f; cg *= 1f - cheek * 0.22f; cb *= 1f - cheek * 0.2f;
            float mouth = g3(lu, lv, 0.5f, 0.72f, 0.06f, 0.012f);
            cr = mix(cr, 0.18f, mouth * 0.9f); cg = mix(cg, 0.03f, mouth * 0.9f); cb = mix(cb, 0.04f, mouth * 0.9f);
            float drip = g3(lu, lv, 0.5f + 0.04f * (Noise.value(lv * 9f, 1f, 5) - 0.5f), 0.88f, 0.03f, 0.14f) * ss(0.7f, 0.76f, lv);
            cr = mix(cr, 0.28f, drip * 0.85f); cg = mix(cg, 0.02f, drip * 0.85f); cb = mix(cb, 0.02f, drip * 0.85f);
            sp = mix(sp, 0.6f, drip);
            float hair = ss(0.16f, 0.06f, lv + 0.12f * Noise.fbmWrap(lu, lv, 8, 8, 3, 80)) + ss(0.28f, 0.12f, lv) * ss(0.78f, 0.9f, Math.abs(lu - 0.5f) * 2f);
            hair = Math.min(1f, hair) * (Noise.fbmWrap(lu, lv, 24, 24, 2, 81) > 0.38f ? 1f : 0.35f);
            cr = mix(cr, 0.07f, hair * 0.9f); cg = mix(cg, 0.055f, hair * 0.9f); cb = mix(cb, 0.045f, hair * 0.9f);
            ht += hair * 0.25f - eye * 0.35f - mouth * 0.15f;
            sp = mix(sp, 0.08f, hair);
        }
        float dirt = 0.7f + 0.3f * Noise.fbmWrap(lu, lv, 4, 4, 4, 75);
        float grime = 1f - 0.35f * ss(0.55f, 0.95f, lv) * Noise.fbmWrap(lu, lv, 6, 3, 3, 83);
        float k = dirt * grime;
        m.set(x, y, cr * k, cg * k, cb * k, ht, Noise.clamp(sp, 0f, 1f));
    }

    private static void cloth(Img m, int x, int y, float lu, float lv) {
        float wv = (float) (Math.sin(x * 2.3f) * Math.sin(y * 2.3f)) * 0.5f + 0.5f;
        float wv2 = (float) (Math.sin(x * 0.55f + y * 0.35f)) * 0.5f + 0.5f;
        float t = Noise.fbmWrap(lu, lv, 5, 5, 5, 81);
        float cr = mix(0.20f, 0.34f, t), cg = mix(0.22f, 0.33f, t), cb = mix(0.12f, 0.2f, t);
        float k = 0.72f + 0.2f * wv + 0.08f * wv2;
        cr *= k; cg *= k; cb *= k;
        float stain = ss(0.60f, 0.68f, Noise.fbmWrap(lu, lv, 7, 7, 3, 82));
        cr = mix(cr, 0.26f, stain * 0.85f); cg = mix(cg, 0.03f, stain * 0.85f); cb = mix(cb, 0.03f, stain * 0.85f);
        float mud = ss(0.35f, 1.0f, lv) * (0.5f + 0.5f * Noise.fbmWrap(lu, lv, 8, 3, 4, 83));
        cr = mix(cr, 0.15f, mud * 0.65f); cg = mix(cg, 0.11f, mud * 0.65f); cb = mix(cb, 0.07f, mud * 0.65f);
        float burn = ss(0.68f, 0.76f, Noise.fbmWrap(lu, lv, 6, 6, 4, 85));
        cr *= 1f - burn * 0.85f; cg *= 1f - burn * 0.85f; cb *= 1f - burn * 0.85f;
        float rn = Noise.fbmWrap(lu, lv, 7, 7, 4, 84);
        float hole = ss(0.715f, 0.745f, rn);
        float fray = ss(0.69f, 0.715f, rn) * (1f - hole);
        cr = mix(cr, 0.40f, hole); cg = mix(cg, 0.34f, hole); cb = mix(cb, 0.27f, hole);
        cr = mix(cr, cr * 1.2f, fray * 0.6f);
        float seam = (float) Math.exp(-Math.pow((lu - 0.5f) * 90f, 2)) + (float) Math.exp(-Math.pow((lu - 0.02f) * 90f, 2));
        cr *= 1f - seam * 0.35f; cg *= 1f - seam * 0.35f; cb *= 1f - seam * 0.35f;
        m.set(x, y, cr, cg, cb, wv * 0.4f + wv2 * 0.1f - hole * 0.6f + fray * 0.2f + seam * -0.3f, 0.05f + stain * 0.3f);
    }

    private static void leatherMetal(Img m, int x, int y, float lu, float lv) {
        if (lu < 0.5f) {
            float u = lu * 2f;
            float cr = Noise.fbmWrap(u, lv, 6, 6, 5, 91);
            float crack = ss(0.035f, 0.004f, Math.abs(Noise.fbmWrap(u, lv, 10, 10, 4, 92) - 0.5f));
            float base = 0.7f + 0.5f * cr;
            float r = 0.25f * base, g = 0.145f * base, b = 0.085f * base;
            float wear = ss(0.6f, 0.78f, Noise.fbmWrap(u, lv, 5, 5, 3, 93));
            r = mix(r, 0.36f, wear * 0.5f); g = mix(g, 0.26f, wear * 0.5f); b = mix(b, 0.17f, wear * 0.5f);
            r *= 1f - crack * 0.7f; g *= 1f - crack * 0.7f; b *= 1f - crack * 0.7f;
            float stitch = ((x % 9) < 4 ? 1f : 0f) * (g3(u, lv, 0.5f, 0.08f, 2f, 0.012f) + g3(u, lv, 0.5f, 0.92f, 2f, 0.012f));
            r = mix(r, 0.5f, Math.min(1f, stitch) * 0.7f); g = mix(g, 0.42f, Math.min(1f, stitch) * 0.7f); b = mix(b, 0.3f, Math.min(1f, stitch) * 0.7f);
            float mud = ss(0.5f, 1.0f, lv) * Noise.fbmWrap(u, lv, 8, 4, 3, 94);
            r = mix(r, 0.14f, mud * 0.6f); g = mix(g, 0.1f, mud * 0.6f); b = mix(b, 0.06f, mud * 0.6f);
            m.set(x, y, r, g, b, cr * 0.5f - crack * 0.5f + stitch * 0.2f, 0.22f + wear * 0.2f);
        } else {
            float u = (lu - 0.5f) * 2f;
            float dent = Noise.fbmWrap(u, lv, 3, 3, 3, 95);
            float rust = ss(0.55f, 0.75f, Noise.fbmWrap(u, lv, 7, 7, 5, 96));
            float scr = Noise.fbmWrap(u, lv, 2, 48, 2, 97);
            float r = 0.30f + 0.05f * dent, g = 0.33f + 0.05f * dent, b = 0.27f + 0.04f * dent;
            r = mix(r, 0.46f, rust * 0.8f); g = mix(g, 0.21f, rust * 0.8f); b = mix(b, 0.08f, rust * 0.8f);
            if (scr > 0.66f) { r += 0.1f; g += 0.1f; b += 0.09f; }
            float spk = Noise.rnd(x, y, 98) > 0.985f ? 0.2f : 0f;
            r += spk; g += spk * 0.6f; b += spk * 0.3f;
            m.set(x, y, r, g, b, dent * 0.5f + rust * 0.12f, (1f - rust) * 0.5f + 0.1f);
        }
    }

    // ----------------------------------------------------- armas: acero pavonado y nogal
    private static final String[][] FONT = {
        {"T","11111","00100","00100","00100","00100","00100","00100"},
        {"R","11110","10001","10001","11110","10100","10010","10001"},
        {"-","00000","00000","00000","11111","00000","00000","00000"},
        {"7","11111","00001","00010","00100","01000","01000","01000"},
        {"4","00010","00110","01010","10010","11111","00010","00010"},
        {"E","11111","10000","10000","11110","10000","10000","11111"},
        {"M","10001","11011","10101","10101","10001","10001","10001"},
        {"P","11110","10001","10001","11110","10000","10000","10000"},
        {"S","01111","10000","10000","01110","00001","00001","11110"},
        {"O","01110","10001","10001","10001","10001","10001","01110"},
        {"N","10001","11001","10101","10011","10001","10001","10001"},
        {"A","01110","10001","10001","11111","10001","10001","10001"},
    };

    private static void drawText(Img m, String s, int x0, int yTop, int sc) {
        int x = x0;
        for (int ci = 0; ci < s.length(); ci++, x += 6 * sc) {
            String[] gl = null;
            for (int k = 0; k < FONT.length; k++) if (FONT[k][0].charAt(0) == s.charAt(ci)) gl = FONT[k];
            if (gl == null) continue;
            for (int ry = 0; ry < 7; ry++)
                for (int rx = 0; rx < 5; rx++) {
                    if (gl[1 + ry].charAt(rx) != '1') continue;
                    for (int py = 0; py < sc; py++)
                        for (int px = 0; px < sc; px++) {
                            int xx = x + rx * sc + px, yy = m.h - 1 - (yTop + ry * sc + py); // v crece hacia arriba
                            if (xx < 0 || xx >= m.w || yy < 0 || yy >= m.h) continue;
                            m.set(xx, yy, 0.45f, 0.95f, 1.0f, 0.04f, 0.2f);
                        }
                }
        }
    }

    /** Pantalla oscura con letras cian (TR-74 TEMPEST / TORMENTA) y un bloque blanco para zonas emisivas. */
    private static void tempest(Img m) {
        for (int y = 0; y < m.h; y++)
            for (int x = 0; x < m.w; x++) m.set(x, y, 0.035f, 0.06f + 0.01f * Noise.rnd(x, y, 7), 0.08f, 0f, 0.3f);
        drawText(m, "TR-74 TEMPEST", 12, 8, 3);
        drawText(m, "TORMENTA", 32, 42, 4);
        for (int y = 0; y < 24; y++) for (int x = 232; x < 256; x++) m.set(x, y, 1f, 1f, 1f, 0f, 0.1f);
    }

    /** Blindaje de ciencia ficción: paneles azul acero con juntas, remaches, rayones y detalles de bronce. */
    private static void scifi(Img m) {
        int n = m.w;
        int gx = 4, gy = 3;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                float u = x / (float) n, v = y / (float) n;
                // paneles irregulares: rejilla con desplazamiento por fila
                float rowH = n / (float) gy;
                int row = (int) (y / rowH);
                float off = Noise.rnd(row, 3, 311) * n / gx;
                float px = (x + off) % (n / (float) gx), py = y % rowH;
                float ex = Math.min(px, n / (float) gx - px), ey = Math.min(py, rowH - py);
                float edge = Math.min(ex, ey);
                float seam = 1f - ss(0f, 2.6f, edge);              // junta oscura
                float bevel = ss(2f, 7f, edge);                    // altura del panel
                int pid = row * 17 + (int) ((x + off) / (n / (float) gx));
                float tone = 0.9f + 0.2f * Noise.rnd(pid, 5, 312);
                float brushed = Noise.fbmWrap(u, v, 128, 3, 3, 313);
                float cloud = Noise.fbmWrap(u, v, 4, 4, 4, 314);
                float k = (0.30f + 0.07f * brushed + 0.05f * cloud) * tone;
                float r = k * 0.88f, g = k * 1.02f, b = k * 1.22f;
                // desgaste en los bordes y rayones
                float wear = ss(0.35f, 0.9f, Noise.fbmWrap(u, v, 10, 10, 4, 315)) * (1f - ss(2f, 9f, edge)) * 0.8f;
                float scr = Noise.fbmWrap(u, v, 60, 2, 2, 316) > 0.74f ? 0.18f : 0f;
                r = mix(r, 0.62f, wear) + scr; g = mix(g, 0.64f, wear) + scr; b = mix(b, 0.68f, wear) + scr;
                // parches de bronce
                float bz = ss(0.72f, 0.82f, Noise.fbmWrap(u, v, 5, 5, 3, 317));
                r = mix(r, 0.66f, bz * 0.85f); g = mix(g, 0.42f, bz * 0.85f); b = mix(b, 0.22f, bz * 0.85f);
                // remaches en las esquinas de cada panel
                float rx = Math.abs(px - 7f), ry = Math.abs(py - 7f);
                float rivet = ss(3.2f, 1.2f, (float) Math.sqrt(rx * rx + ry * ry));
                r += rivet * 0.12f; g += rivet * 0.12f; b += rivet * 0.14f;
                float sm = 1f - seam * 0.8f;
                float height = bevel * 0.7f + rivet * 0.5f + brushed * 0.08f;
                m.set(x, y, r * sm, g * sm, b * sm, height, 0.55f + wear * 0.2f + bz * 0.15f);
            }
        }
    }

    private static void gunmetal(Img m) {
        int n = m.w;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                float u = x / (float) n, v = y / (float) n;
                float brushed = Noise.fbmWrap(u, v, 3, 96, 3, 101);
                float grain = Noise.rnd(x, y, 102);
                float wear = ss(0.60f, 0.80f, Noise.fbmWrap(u, v, 6, 6, 5, 103));
                float k = 0.10f + 0.06f * brushed + 0.02f * grain;
                float r = k * 0.95f, g = k * 1.0f, b = k * 1.12f;
                r = mix(r, 0.30f, wear * 0.7f); g = mix(g, 0.30f, wear * 0.7f); b = mix(b, 0.31f, wear * 0.7f);
                float oil = ss(0.55f, 0.8f, Noise.fbmWrap(u, v, 4, 4, 3, 104));
                r *= 1f - oil * 0.25f; g *= 1f - oil * 0.25f; b *= 1f - oil * 0.2f;
                float scratch = Noise.fbmWrap(u, v, 40, 2, 2, 105) > 0.7f ? 0.1f : 0f;
                r += scratch; g += scratch; b += scratch;
                m.set(x, y, r, g, b, brushed * 0.3f + grain * 0.06f + wear * 0.05f, 0.55f + wear * 0.15f + oil * 0.2f);
            }
        }
    }

    private static void walnut(Img m) {
        int n = m.w;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                float u = x / (float) n, v = y / (float) n;
                float gr = Noise.fbmWrap(u, v, 3, 40, 4, 111);
                float ring = (float) Math.sin(v * 6.2831853f * 14f + gr * 9f) * 0.5f + 0.5f;
                float fig = Noise.fbmWrap(u, v, 6, 6, 3, 112);
                float k = 0.6f + 0.4f * ring * gr + 0.15f * fig;
                float r = 0.32f * k, g = 0.19f * k, b = 0.10f * k;
                float dark = ss(0.55f, 0.75f, Noise.fbmWrap(u, v, 5, 5, 3, 113));
                r = mix(r, 0.12f, dark * 0.5f); g = mix(g, 0.07f, dark * 0.5f); b = mix(b, 0.04f, dark * 0.5f);
                float scuff = Noise.fbmWrap(u, v, 20, 20, 2, 114) > 0.72f ? 0.06f : 0f;
                m.set(x, y, r + scuff, g + scuff, b + scuff * 0.6f, ring * 0.25f + gr * 0.2f - scuff * 2f, 0.38f);
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
