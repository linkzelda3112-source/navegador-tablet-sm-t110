package com.craterz;

/** Sistema de partículas por CPU (sangre, humo, chispas, brasas, destellos, trazadoras). */
public final class Particles {
    public static final int CAP = 900;
    public static final int SMOKE = 0, SPARK = 1, RAIN = 2, RING = 3;

    public final float[] x = new float[CAP], y = new float[CAP], z = new float[CAP];
    public final float[] vx = new float[CAP], vy = new float[CAP], vz = new float[CAP];
    public final float[] life = new float[CAP], maxLife = new float[CAP];
    public final float[] s0 = new float[CAP], s1 = new float[CAP];
    public final float[] r = new float[CAP], g = new float[CAP], b = new float[CAP], a = new float[CAP];
    public final float[] grav = new float[CAP], drag = new float[CAP], rot = new float[CAP], spin = new float[CAP];
    public final float[] ex = new float[CAP], ey = new float[CAP], ez = new float[CAP];
    public final int[] cell = new int[CAP];
    public final boolean[] add = new boolean[CAP], tracer = new boolean[CAP], ground = new boolean[CAP];
    public int count;
    public int limit = CAP;
    private int rs = 987654321;

    private float rnd() { rs = rs * 1103515245 + 12345; return ((rs >>> 8) & 0xFFFF) / 65536f; }
    private float rr(float a, float b) { return a + (b - a) * rnd(); }

    private int alloc() {
        if (count >= limit) return -1;
        int i = count++;
        tracer[i] = false; ground[i] = false; rot[i] = 0; spin[i] = 0; drag[i] = 0; grav[i] = 0;
        return i;
    }

    public void clear() { count = 0; }

    public void blood(float px, float py, float pz, float dx, float dy, float dz, int n) {
        for (int k = 0; k < n; k++) {
            int i = alloc(); if (i < 0) return;
            x[i] = px; y[i] = py; z[i] = pz;
            vx[i] = -dx * rr(0.5f, 3f) + rr(-1.5f, 1.5f); vy[i] = rr(0.3f, 2.6f); vz[i] = -dz * rr(0.5f, 3f) + rr(-1.5f, 1.5f);
            life[i] = maxLife[i] = rr(0.3f, 0.8f);
            s0[i] = rr(0.04f, 0.09f); s1[i] = s0[i] * 0.5f;
            r[i] = rr(0.35f, 0.55f); g[i] = 0.02f; b[i] = 0.02f; a[i] = 0.95f;
            grav[i] = 9f; drag[i] = 0.5f; cell[i] = SMOKE; add[i] = false; ground[i] = true;
        }
        int i = alloc(); if (i < 0) return;
        x[i] = px; y[i] = py; z[i] = pz; vx[i] = vy[i] = vz[i] = 0;
        life[i] = maxLife[i] = 0.35f; s0[i] = 0.2f; s1[i] = 0.55f;
        r[i] = 0.35f; g[i] = 0.03f; b[i] = 0.03f; a[i] = 0.55f; cell[i] = SMOKE; add[i] = false;
    }

    public void dust(float px, float py, float pz, float nx, float ny, float nz, int n, float cr, float cg, float cb) {
        for (int k = 0; k < n; k++) {
            int i = alloc(); if (i < 0) return;
            x[i] = px; y[i] = py; z[i] = pz;
            vx[i] = nx * rr(0.3f, 1.4f) + rr(-0.5f, 0.5f); vy[i] = ny * rr(0.3f, 1.4f) + rr(0.1f, 0.8f); vz[i] = nz * rr(0.3f, 1.4f) + rr(-0.5f, 0.5f);
            life[i] = maxLife[i] = rr(0.5f, 1.2f);
            s0[i] = rr(0.1f, 0.2f); s1[i] = rr(0.4f, 0.8f);
            r[i] = cr; g[i] = cg; b[i] = cb; a[i] = 0.5f;
            grav[i] = -0.2f; drag[i] = 2.2f; cell[i] = SMOKE; add[i] = false; rot[i] = rr(0, 6.28f); spin[i] = rr(-1.5f, 1.5f);
        }
    }

    public void sparks(float px, float py, float pz, float nx, float ny, float nz, int n, float cr, float cg, float cb) {
        for (int k = 0; k < n; k++) {
            int i = alloc(); if (i < 0) return;
            x[i] = px; y[i] = py; z[i] = pz;
            vx[i] = nx * rr(1f, 4f) + rr(-2f, 2f); vy[i] = ny * rr(1f, 4f) + rr(0.5f, 2.5f); vz[i] = nz * rr(1f, 4f) + rr(-2f, 2f);
            life[i] = maxLife[i] = rr(0.2f, 0.55f);
            s0[i] = rr(0.025f, 0.05f); s1[i] = 0.005f;
            r[i] = cr; g[i] = cg; b[i] = cb; a[i] = 1f;
            grav[i] = 8f; drag[i] = 0.3f; cell[i] = SPARK; add[i] = true;
        }
    }

    public void flash(float px, float py, float pz, float size, float cr, float cg, float cb, float lifeT) {
        int i = alloc(); if (i < 0) return;
        x[i] = px; y[i] = py; z[i] = pz; vx[i] = vy[i] = vz[i] = 0;
        life[i] = maxLife[i] = lifeT; s0[i] = size; s1[i] = size * 0.4f;
        r[i] = cr; g[i] = cg; b[i] = cb; a[i] = 1f; cell[i] = SPARK; add[i] = true; rot[i] = rr(0, 6.28f);
    }

    public void ring(float px, float py, float pz, float size, float cr, float cg, float cb, float lifeT) {
        int i = alloc(); if (i < 0) return;
        x[i] = px; y[i] = py; z[i] = pz; vx[i] = vy[i] = vz[i] = 0;
        life[i] = maxLife[i] = lifeT; s0[i] = size * 0.2f; s1[i] = size;
        r[i] = cr; g[i] = cg; b[i] = cb; a[i] = 0.9f; cell[i] = RING; add[i] = true;
    }

    public void smoke(float px, float py, float pz, float size, float cr, float cg, float cb, float al, float lifeT, float up) {
        int i = alloc(); if (i < 0) return;
        x[i] = px; y[i] = py; z[i] = pz;
        vx[i] = rr(-0.25f, 0.25f) + 0.35f; vy[i] = up; vz[i] = rr(-0.25f, 0.25f);
        life[i] = maxLife[i] = lifeT; s0[i] = size; s1[i] = size * 3f;
        r[i] = cr; g[i] = cg; b[i] = cb; a[i] = al; cell[i] = SMOKE; add[i] = false; rot[i] = rr(0, 6.28f); spin[i] = rr(-0.5f, 0.5f); drag[i] = 0.4f;
    }

    public void ember(float px, float py, float pz) {
        int i = alloc(); if (i < 0) return;
        x[i] = px + rr(-0.25f, 0.25f); y[i] = py; z[i] = pz + rr(-0.25f, 0.25f);
        vx[i] = rr(-0.5f, 0.9f); vy[i] = rr(1.2f, 3f); vz[i] = rr(-0.5f, 0.5f);
        life[i] = maxLife[i] = rr(0.8f, 2.0f); s0[i] = rr(0.03f, 0.06f); s1[i] = 0.005f;
        r[i] = 1f; g[i] = rr(0.4f, 0.65f); b[i] = 0.1f; a[i] = 1f; cell[i] = SPARK; add[i] = true; drag[i] = 0.2f;
    }

    public void tracer(float sx, float sy, float sz, float tx, float ty, float tz, float cr, float cg, float cb, float width) {
        int i = alloc(); if (i < 0) return;
        x[i] = sx; y[i] = sy; z[i] = sz; ex[i] = tx; ey[i] = ty; ez[i] = tz;
        vx[i] = vy[i] = vz[i] = 0;
        life[i] = maxLife[i] = 0.07f; s0[i] = width; s1[i] = width * 0.3f;
        r[i] = cr; g[i] = cg; b[i] = cb; a[i] = 0.85f; cell[i] = RAIN; add[i] = true; tracer[i] = true;
    }

    public void update(float dt, World w) {
        for (int i = count - 1; i >= 0; i--) {
            life[i] -= dt;
            if (life[i] <= 0) { remove(i); continue; }
            if (tracer[i]) continue;
            float d = 1f - drag[i] * dt; if (d < 0) d = 0;
            vx[i] *= d; vy[i] = vy[i] * d - grav[i] * dt; vz[i] *= d;
            x[i] += vx[i] * dt; y[i] += vy[i] * dt; z[i] += vz[i] * dt;
            rot[i] += spin[i] * dt;
            if (ground[i]) {
                float gy = w.ground(x[i], z[i]);
                if (y[i] < gy + 0.02f) { y[i] = gy + 0.02f; vx[i] = vy[i] = vz[i] = 0; life[i] = Math.min(life[i], 0.25f); }
            }
        }
    }

    private void remove(int i) {
        int l = count - 1;
        if (i != l) copy(l, i);
        count--;
    }

    private void copy(int s, int d) {
        x[d] = x[s]; y[d] = y[s]; z[d] = z[s]; vx[d] = vx[s]; vy[d] = vy[s]; vz[d] = vz[s];
        life[d] = life[s]; maxLife[d] = maxLife[s]; s0[d] = s0[s]; s1[d] = s1[s];
        r[d] = r[s]; g[d] = g[s]; b[d] = b[s]; a[d] = a[s]; grav[d] = grav[s]; drag[d] = drag[s];
        rot[d] = rot[s]; spin[d] = spin[s]; ex[d] = ex[s]; ey[d] = ey[s]; ez[d] = ez[s];
        cell[d] = cell[s]; add[d] = add[s]; tracer[d] = tracer[s]; ground[d] = ground[s];
    }
}
