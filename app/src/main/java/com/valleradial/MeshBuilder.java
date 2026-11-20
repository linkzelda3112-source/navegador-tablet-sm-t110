package com.valleradial;

import java.util.Random;

/**
 * Construye mallas de triangulos (pos3, normal3, color3). Sin Android: solo arreglos.
 * Los triangulos se orientan solos hacia afuera respecto a un centro de referencia.
 */
public final class MeshBuilder {
    private float[] data = new float[9 * 3 * 256];
    private int n; // floats usados

    public int vertexCount() { return n / 9; }

    public float[] toArray() {
        float[] out = new float[n];
        System.arraycopy(data, 0, out, 0, n);
        return out;
    }

    private void v(float x, float y, float z, float nx, float ny, float nz, float[] c) {
        if (n + 9 > data.length) {
            float[] nd = new float[data.length * 2];
            System.arraycopy(data, 0, nd, 0, n);
            data = nd;
        }
        data[n++] = x; data[n++] = y; data[n++] = z;
        data[n++] = nx; data[n++] = ny; data[n++] = nz;
        data[n++] = c[0]; data[n++] = c[1]; data[n++] = c[2];
    }

    /** Triangulo con normal plana, orientado hacia afuera de (cx,cy,cz). */
    public void tri(float[] a, float[] b, float[] c, float[] col, float cx, float cy, float cz) {
        float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
        float wx = c[0] - a[0], wy = c[1] - a[1], wz = c[2] - a[2];
        float nx = uy * wz - uz * wy, ny = uz * wx - ux * wz, nz = ux * wy - uy * wx;
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (l < 1e-9f) return;
        nx /= l; ny /= l; nz /= l;
        float mx = (a[0] + b[0] + c[0]) / 3f - cx, my = (a[1] + b[1] + c[1]) / 3f - cy, mz = (a[2] + b[2] + c[2]) / 3f - cz;
        if (nx * mx + ny * my + nz * mz < 0f) {
            float[] t = b; b = c; c = t;
            nx = -nx; ny = -ny; nz = -nz;
        }
        v(a[0], a[1], a[2], nx, ny, nz, col);
        v(b[0], b[1], b[2], nx, ny, nz, col);
        v(c[0], c[1], c[2], nx, ny, nz, col);
    }

    private static float[] p(float x, float y, float z) { return new float[]{x, y, z}; }

    public void quad(float[] a, float[] b, float[] c, float[] d, float[] col, float cx, float cy, float cz) {
        tri(a, b, c, col, cx, cy, cz);
        tri(a, c, d, col, cx, cy, cz);
    }

    public void box(float cx, float cy, float cz, float hx, float hy, float hz, float[] col) {
        float[] a = p(cx - hx, cy - hy, cz - hz), b = p(cx + hx, cy - hy, cz - hz),
                c = p(cx + hx, cy + hy, cz - hz), d = p(cx - hx, cy + hy, cz - hz),
                e = p(cx - hx, cy - hy, cz + hz), f = p(cx + hx, cy - hy, cz + hz),
                g = p(cx + hx, cy + hy, cz + hz), h = p(cx - hx, cy + hy, cz + hz);
        quad(a, b, c, d, col, cx, cy, cz);
        quad(e, f, g, h, col, cx, cy, cz);
        quad(a, b, f, e, col, cx, cy, cz);
        quad(d, c, g, h, col, cx, cy, cz);
        quad(a, d, h, e, col, cx, cy, cz);
        quad(b, c, g, f, col, cx, cy, cz);
    }

    /** Cilindro/tronco de cono vertical con la base en y0. */
    public void cylinder(float cx, float y0, float cz, float r0, float r1, float h, int seg, float[] col) {
        for (int i = 0; i < seg; i++) {
            double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
            float[] b0 = p(cx + r0 * (float) Math.cos(a0), y0, cz + r0 * (float) Math.sin(a0));
            float[] b1 = p(cx + r0 * (float) Math.cos(a1), y0, cz + r0 * (float) Math.sin(a1));
            float[] t0 = p(cx + r1 * (float) Math.cos(a0), y0 + h, cz + r1 * (float) Math.sin(a0));
            float[] t1 = p(cx + r1 * (float) Math.cos(a1), y0 + h, cz + r1 * (float) Math.sin(a1));
            quad(b0, b1, t1, t0, col, cx, y0 + h * 0.5f, cz);
            tri(t0, t1, p(cx, y0 + h, cz), col, cx, y0 + h * 0.5f, cz);
        }
    }

    /**
     * Esfera deformada. smooth=true: normales suaves (copas, bichos). false: facetada (rocas).
     * topLight aclara la parte de arriba (pinta como el video: copas con luz arriba).
     */
    public void blob(float cx, float cy, float cz, float rx, float ry, float rz, int lat, int lon,
                     float noise, long seed, float[] col, boolean smooth, float topLight) {
        Random rnd = new Random(seed);
        float[][][] pts = new float[lat + 1][lon][];
        float[][] rad = new float[lat + 1][lon];
        for (int i = 0; i <= lat; i++) {
            for (int j = 0; j < lon; j++) {
                rad[i][j] = 1f + (rnd.nextFloat() - 0.5f) * noise;
            }
        }
        for (int j = 1; j < lon; j++) { rad[0][j] = rad[0][0]; rad[lat][j] = rad[lat][0]; }
        for (int i = 0; i <= lat; i++) {
            double th = Math.PI * i / lat;
            for (int j = 0; j < lon; j++) {
                double ph = 2 * Math.PI * j / lon;
                float sx = (float) (Math.sin(th) * Math.cos(ph)), sy = (float) Math.cos(th), sz = (float) (Math.sin(th) * Math.sin(ph));
                float r = rad[i][j];
                pts[i][j] = new float[]{cx + sx * rx * r, cy + sy * ry * r, cz + sz * rz * r, sx, sy, sz};
            }
        }
        for (int i = 0; i < lat; i++) {
            for (int j = 0; j < lon; j++) {
                int j2 = (j + 1) % lon;
                float[] a = pts[i][j], b = pts[i][j2], c = pts[i + 1][j2], d = pts[i + 1][j];
                emit(a, b, c, col, cx, cy, cz, smooth, topLight);
                emit(a, c, d, col, cx, cy, cz, smooth, topLight);
            }
        }
    }

    private void emit(float[] a, float[] b, float[] c, float[] col, float cx, float cy, float cz,
                      boolean smooth, float topLight) {
        float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
        float wx = c[0] - a[0], wy = c[1] - a[1], wz = c[2] - a[2];
        float nx = uy * wz - uz * wy, ny = uz * wx - ux * wz, nz = ux * wy - uy * wx;
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (l < 1e-9f) return; // triangulo degenerado en los polos
        nx /= l; ny /= l; nz /= l;
        float mx = (a[0] + b[0] + c[0]) / 3f - cx, my = (a[1] + b[1] + c[1]) / 3f - cy, mz = (a[2] + b[2] + c[2]) / 3f - cz;
        if (nx * mx + ny * my + nz * mz < 0f) { float[] t = b; b = c; c = t; nx = -nx; ny = -ny; nz = -nz; }
        float[] va = a, vb = b, vc = c;
        float[][] vs = {va, vb, vc};
        for (float[] q : vs) {
            float sh = 1f + topLight * (q[4] * 0.5f);
            float[] cc = {Math.min(1f, col[0] * sh), Math.min(1f, col[1] * sh), Math.min(1f, col[2] * sh)};
            if (smooth) v(q[0], q[1], q[2], q[3], q[4], q[5], cc);
            else v(q[0], q[1], q[2], nx, ny, nz, cc);
        }
    }

    // ---------------------------------------------------------------- objetos del juego
    private static final float[] STONE = {0.78f, 0.70f, 0.55f};
    private static final float[] ROOF_A = {0.80f, 0.31f, 0.26f};
    private static final float[] ROOF_B = {0.68f, 0.22f, 0.20f};
    private static final float[] DARK = {0.25f, 0.17f, 0.13f};

    public static MeshBuilder house() {
        MeshBuilder m = new MeshBuilder();
        m.box(0, 2.2f, 0, 3.4f, 2.2f, 2.8f, STONE);
        // puerta y ventana
        m.box(0, 1.2f, 2.82f, 0.8f, 1.2f, 0.06f, DARK);
        m.box(2.0f, 2.8f, 2.82f, 0.55f, 0.55f, 0.06f, DARK);
        // techo a cuatro aguas, con franjas de tejas
        float yb = 4.4f, yt = 7.2f;
        float[] fl = {-4.0f, yb, 3.4f}, fr = {4.0f, yb, 3.4f}, bl = {-4.0f, yb, -3.4f}, br = {4.0f, yb, -3.4f};
        float[] tl = {-1.6f, yt, 0f}, tr = {1.6f, yt, 0f};
        float cx = 0, cy = 5.2f, cz = 0;
        for (int k = 0; k < 4; k++) {
            float t0 = k / 4f, t1 = (k + 1) / 4f;
            float[] col = (k % 2 == 0) ? ROOF_A : ROOF_B;
            // pendiente frontal
            m.quad(lerp(fl, tl, t0), lerp(fr, tr, t0), lerp(fr, tr, t1), lerp(fl, tl, t1), col, cx, cy, cz);
            // pendiente trasera
            m.quad(lerp(bl, tl, t0), lerp(br, tr, t0), lerp(br, tr, t1), lerp(bl, tl, t1), col, cx, cy, cz);
        }
        m.tri(fl, bl, tl, ROOF_B, cx, cy, cz);
        m.tri(fr, br, tr, ROOF_B, cx, cy, cz);
        return m;
    }

    private static float[] lerp(float[] a, float[] b, float t) {
        return new float[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    private static final float[][] CANOPY = {
            {0.55f, 0.65f, 0.22f}, {0.95f, 0.76f, 0.30f}, {0.82f, 0.30f, 0.38f}, {0.42f, 0.55f, 0.20f}};

    public static MeshBuilder tree(int variant) {
        MeshBuilder m = new MeshBuilder();
        m.cylinder(0, 0, 0, 0.6f, 0.35f, 5.4f, 6, new float[]{0.42f, 0.29f, 0.20f});
        float[] c = CANOPY[variant & 3];
        m.blob(0, 7.2f, 0, 3.4f, 3.0f, 3.4f, 6, 8, 0.25f, 100 + variant, c, true, 0.5f);
        m.blob(1.6f, 8.6f, 0.6f, 2.3f, 2.0f, 2.3f, 5, 7, 0.25f, 200 + variant, c, true, 0.5f);
        return m;
    }

    public static MeshBuilder rock() {
        MeshBuilder m = new MeshBuilder();
        m.blob(0, 0.7f, 0, 1.8f, 1.1f, 1.5f, 4, 7, 0.35f, 5, new float[]{0.44f, 0.49f, 0.58f}, false, 0.3f);
        return m;
    }

    public static MeshBuilder bush(int variant) {
        MeshBuilder m = new MeshBuilder();
        float[] c = variant == 0 ? new float[]{0.56f, 0.66f, 0.22f} : new float[]{0.92f, 0.42f, 0.48f};
        m.blob(0, 0.7f, 0, 1.4f, 0.9f, 1.4f, 4, 7, 0.3f, 9 + variant, c, true, 0.4f);
        return m;
    }

    /** Mancha de tinta enemiga: bola rosada con un ojo grande hacia +Z. */
    public static MeshBuilder enemy() {
        MeshBuilder m = new MeshBuilder();
        m.blob(0, 1.2f, 0, 1.3f, 1.2f, 1.3f, 8, 10, 0.12f, 3, new float[]{0.93f, 0.42f, 0.55f}, true, 0.5f);
        m.blob(0, 1.5f, 1.05f, 0.62f, 0.62f, 0.4f, 6, 8, 0f, 4, new float[]{0.98f, 0.97f, 0.92f}, true, 0f);
        m.blob(0, 1.5f, 1.38f, 0.28f, 0.28f, 0.16f, 5, 7, 0f, 5, new float[]{0.08f, 0.06f, 0.07f}, true, 0f);
        // dos "orejas" de tinta
        m.blob(-0.8f, 2.3f, 0, 0.3f, 0.55f, 0.3f, 4, 6, 0f, 6, new float[]{0.93f, 0.42f, 0.55f}, true, 0.3f);
        m.blob(0.8f, 2.3f, 0, 0.3f, 0.55f, 0.3f, 4, 6, 0f, 7, new float[]{0.93f, 0.42f, 0.55f}, true, 0.3f);
        return m;
    }
}
