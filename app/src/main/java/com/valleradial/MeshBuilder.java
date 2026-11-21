package com.valleradial;

import java.util.Random;

/**
 * Construye mallas de triangulos (pos3, normal3, color3, balanceo1). Sin Android: solo arreglos.
 * Los triangulos se orientan solos hacia afuera respecto a un centro de referencia.
 * "balanceo" es el peso con el que el viento mueve cada vertice (0 = fijo).
 */
public final class MeshBuilder {
    public static final int STRIDE = 10;

    private float[] data = new float[STRIDE * 3 * 256];
    private int n; // floats usados
    private float swY0, swSpan; // balanceo: 0 abajo de y0, 1 a y0+span (cuadratico). span=0 -> apagado

    public int vertexCount() { return n / STRIDE; }

    public float[] toArray() {
        float[] out = new float[n];
        System.arraycopy(data, 0, out, 0, n);
        return out;
    }

    public void sway(float y0, float span) { swY0 = y0; swSpan = span; }

    private float wAt(float y) {
        if (swSpan <= 0f) return 0f;
        float t = (y - swY0) / swSpan;
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        return t * t;
    }

    private void vw(float x, float y, float z, float nx, float ny, float nz, float[] c, float w) {
        if (n + STRIDE > data.length) {
            float[] nd = new float[data.length * 2];
            System.arraycopy(data, 0, nd, 0, n);
            data = nd;
        }
        data[n++] = x; data[n++] = y; data[n++] = z;
        data[n++] = nx; data[n++] = ny; data[n++] = nz;
        data[n++] = c[0]; data[n++] = c[1]; data[n++] = c[2];
        data[n++] = w;
    }

    private void v(float x, float y, float z, float nx, float ny, float nz, float[] c) {
        vw(x, y, z, nx, ny, nz, c, wAt(y));
    }

    private static float[] p(float x, float y, float z) { return new float[]{x, y, z}; }

    private static float[] c3(float r, float g, float b) { return new float[]{r, g, b}; }

    private static float[] sc(float[] c, float k) {
        return new float[]{Math.min(1f, c[0] * k), Math.min(1f, c[1] * k), Math.min(1f, c[2] * k)};
    }

    private static float[] mix(float[] a, float[] b, float t) {
        return new float[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    // ---------------------------------------------------------------- primitivas
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

    /** Caja girada alrededor del eje Z (por el origen del plano XY del centro). */
    public void boxRotZ(float cx, float cy, float cz, float hx, float hy, float hz, float ang, float[] col) {
        float c = (float) Math.cos(ang), s = (float) Math.sin(ang);
        float[][] q = new float[8][];
        int k = 0;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    float x = sx * hx, y = sy * hy;
                    q[k++] = p(cx + x * c - y * s, cy + x * s + y * c, cz + sz * hz);
                }
            }
        }
        quad(q[0], q[1], q[3], q[2], col, cx, cy, cz);
        quad(q[4], q[5], q[7], q[6], col, cx, cy, cz);
        quad(q[0], q[1], q[5], q[4], col, cx, cy, cz);
        quad(q[2], q[3], q[7], q[6], col, cx, cy, cz);
        quad(q[0], q[2], q[6], q[4], col, cx, cy, cz);
        quad(q[1], q[3], q[7], q[5], col, cx, cy, cz);
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

    /** Cilindro con anillos de color alternado (franjas de piedra, madera...). */
    public void cylinderBands(float cx, float y0, float cz, float r0, float r1, float h, int seg, int bands,
                              float[] colA, float[] colB) {
        for (int k = 0; k < bands; k++) {
            float t0 = k / (float) bands, t1 = (k + 1) / (float) bands;
            float ra = r0 + (r1 - r0) * t0, rb = r0 + (r1 - r0) * t1;
            cylinder(cx, y0 + h * t0, cz, ra, rb, h / bands, seg, (k % 2 == 0) ? colA : colB);
        }
    }

    /** Extremidad (rama, raiz, columna caida) entre dos puntos cualesquiera. */
    public void limb(float x0, float y0, float z0, float x1, float y1, float z1, float r0, float r1, int seg, float[] col) {
        float ax = x1 - x0, ay = y1 - y0, az = z1 - z0;
        float l = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        if (l < 1e-5f) return;
        ax /= l; ay /= l; az /= l;
        float rx = Math.abs(ay) < 0.9f ? 0f : 1f, ry = Math.abs(ay) < 0.9f ? 1f : 0f;
        float e1x = ay * 0f - az * ry, e1y = az * rx - ax * 0f, e1z = ax * ry - ay * rx;
        float el = (float) Math.sqrt(e1x * e1x + e1y * e1y + e1z * e1z);
        e1x /= el; e1y /= el; e1z /= el;
        float e2x = ay * e1z - az * e1y, e2y = az * e1x - ax * e1z, e2z = ax * e1y - ay * e1x;
        for (int i = 0; i < seg; i++) {
            double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float[] b0 = p(x0 + (e1x * c0 + e2x * s0) * r0, y0 + (e1y * c0 + e2y * s0) * r0, z0 + (e1z * c0 + e2z * s0) * r0);
            float[] b1 = p(x0 + (e1x * c1 + e2x * s1) * r0, y0 + (e1y * c1 + e2y * s1) * r0, z0 + (e1z * c1 + e2z * s1) * r0);
            float[] t0 = p(x1 + (e1x * c0 + e2x * s0) * r1, y1 + (e1y * c0 + e2y * s0) * r1, z1 + (e1z * c0 + e2z * s0) * r1);
            float[] t1 = p(x1 + (e1x * c1 + e2x * s1) * r1, y1 + (e1y * c1 + e2y * s1) * r1, z1 + (e1z * c1 + e2z * s1) * r1);
            float mx = (x0 + x1) * 0.5f, my = (y0 + y1) * 0.5f, mz = (z0 + z1) * 0.5f;
            quad(b0, b1, t1, t0, col, mx, my, mz);
            tri(t0, t1, p(x1, y1, z1), col, mx, my, mz);
        }
    }

    /**
     * Esfera deformada. smooth=true: normales suaves (copas, bichos). false: facetada (rocas).
     * topLight aclara la parte de arriba. jitter varia el color por vertice (hojas, musgo).
     */
    public void blob(float cx, float cy, float cz, float rx, float ry, float rz, int lat, int lon,
                     float noise, long seed, float[] col, boolean smooth, float topLight) {
        blobJ(cx, cy, cz, rx, ry, rz, lat, lon, noise, seed, col, smooth, topLight, 0f);
    }

    public void blobJ(float cx, float cy, float cz, float rx, float ry, float rz, int lat, int lon,
                      float noise, long seed, float[] col, boolean smooth, float topLight, float jitter) {
        Random rnd = new Random(seed);
        float[][][] pts = new float[lat + 1][lon][];
        float[][] rad = new float[lat + 1][lon];
        for (int i = 0; i <= lat; i++) {
            for (int j = 0; j < lon; j++) rad[i][j] = 1f + (rnd.nextFloat() - 0.5f) * noise;
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
                emit(a, b, c, col, cx, cy, cz, smooth, topLight, jitter, rnd);
                emit(a, c, d, col, cx, cy, cz, smooth, topLight, jitter, rnd);
            }
        }
    }

    private void emit(float[] a, float[] b, float[] c, float[] col, float cx, float cy, float cz,
                      boolean smooth, float topLight, float jitter, Random rnd) {
        float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
        float wx = c[0] - a[0], wy = c[1] - a[1], wz = c[2] - a[2];
        float nx = uy * wz - uz * wy, ny = uz * wx - ux * wz, nz = ux * wy - uy * wx;
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (l < 1e-9f) return; // triangulo degenerado en los polos
        nx /= l; ny /= l; nz /= l;
        float mx = (a[0] + b[0] + c[0]) / 3f - cx, my = (a[1] + b[1] + c[1]) / 3f - cy, mz = (a[2] + b[2] + c[2]) / 3f - cz;
        if (nx * mx + ny * my + nz * mz < 0f) { float[] t = b; b = c; c = t; nx = -nx; ny = -ny; nz = -nz; }
        float[][] vs = {a, b, c};
        float jt = 1f + (rnd.nextFloat() - 0.5f) * jitter;
        for (float[] q : vs) {
            float sh = (1f + topLight * (q[4] * 0.5f)) * jt;
            float[] cc = {Math.min(1f, col[0] * sh), Math.min(1f, col[1] * sh), Math.min(1f, col[2] * sh)};
            if (smooth) v(q[0], q[1], q[2], q[3], q[4], q[5], cc);
            else v(q[0], q[1], q[2], nx, ny, nz, cc);
        }
    }

    // ---------------------------------------------------------------- pasto (mundo, sin orientar)
    /** Triangulo ya orientado, con normal y balanceo explicitos por vertice. */
    private void rawTri(float[] a, float[] b, float[] c, float[] ca, float[] cb, float[] cc,
                        float wa, float wb, float wc, float nx, float ny, float nz) {
        vw(a[0], a[1], a[2], nx, ny, nz, ca, wa);
        vw(b[0], b[1], b[2], nx, ny, nz, cb, wb);
        vw(c[0], c[1], c[2], nx, ny, nz, cc, wc);
    }

    /**
     * Mata de pasto en coordenadas de mundo: 3 hojas cruzadas y, a veces, una florecita.
     * (ux,uy,uz) es "arriba" local (hacia el centro del mundo).
     */
    public void tuft(float x, float y, float z, float ux, float uy, float uz, Random r) {
        float rx = Math.abs(uy) < 0.9f ? 0f : 1f, ry = Math.abs(uy) < 0.9f ? 1f : 0f;
        float t1x = uy * 0f - uz * ry, t1y = uz * rx - ux * 0f, t1z = ux * ry - uy * rx;
        float tl = (float) Math.sqrt(t1x * t1x + t1y * t1y + t1z * t1z);
        t1x /= tl; t1y /= tl; t1z /= tl;
        float t2x = uy * t1z - uz * t1y, t2y = uz * t1x - ux * t1z, t2z = ux * t1y - uy * t1x;
        boolean green = r.nextFloat() < 0.35f;
        float[] base = green ? c3(0.30f, 0.44f, 0.13f) : c3(0.50f, 0.46f, 0.13f);
        float[] tip = green ? c3(0.66f, 0.80f, 0.30f) : c3(0.97f, 0.84f, 0.36f);
        float h0 = 0.55f + r.nextFloat() * 0.6f;
        float baseAng = r.nextFloat() * 3.1416f;
        float topX = 0, topY = 0, topZ = 0;
        for (int i = 0; i < 3; i++) {
            float a = baseAng + i * 1.0472f;
            float sx = t1x * (float) Math.cos(a) + t2x * (float) Math.sin(a);
            float sy = t1y * (float) Math.cos(a) + t2y * (float) Math.sin(a);
            float sz = t1z * (float) Math.cos(a) + t2z * (float) Math.sin(a);
            // inclinacion hacia el lado
            float lean = (0.15f + r.nextFloat() * 0.3f) * (r.nextBoolean() ? 1 : -1);
            float lx = uy * sz - uz * sy, ly = uz * sx - ux * sz, lz = ux * sy - uy * sx;
            float h = h0 * (0.75f + r.nextFloat() * 0.5f);
            float w = 0.09f + r.nextFloat() * 0.05f;
            float[] b0 = p(x - sx * w, y - sy * w, z - sz * w), b1 = p(x + sx * w, y + sy * w, z + sz * w);
            float[] tp = p(x + ux * h + lx * lean * h, y + uy * h + ly * lean * h, z + uz * h + lz * lean * h);
            float[] tipc = mix(tip, base, r.nextFloat() * 0.25f);
            rawTri(b0, b1, tp, base, base, tipc, 0f, 0f, 1f, ux, uy, uz);
            topX = tp[0]; topY = tp[1]; topZ = tp[2];
        }
        if (r.nextFloat() < 0.16f) {
            float[][] fl = {c3(0.95f, 0.45f, 0.55f), c3(0.98f, 0.96f, 0.88f), c3(0.98f, 0.82f, 0.30f), c3(0.55f, 0.62f, 0.95f)};
            float[] fc = fl[r.nextInt(4)];
            float fh = h0 * 1.05f, fw = 0.2f;
            float cx = x + ux * fh, cy = y + uy * fh, cz = z + uz * fh;
            float[] f0 = p(cx + t1x * fw, cy + t1y * fw, cz + t1z * fw), f1 = p(cx + t2x * fw, cy + t2y * fw, cz + t2z * fw),
                    f2 = p(cx - t1x * fw, cy - t1y * fw, cz - t1z * fw), f3 = p(cx - t2x * fw, cy - t2y * fw, cz - t2z * fw);
            float[] top = p(cx + ux * 0.1f, cy + uy * 0.1f, cz + uz * 0.1f);
            float[] ctr = c3(0.95f, 0.78f, 0.2f);
            rawTri(f0, f1, top, fc, fc, ctr, 1f, 1f, 1f, ux, uy, uz);
            rawTri(f1, f2, top, fc, fc, ctr, 1f, 1f, 1f, ux, uy, uz);
            rawTri(f2, f3, top, fc, fc, ctr, 1f, 1f, 1f, ux, uy, uz);
            rawTri(f3, f0, top, fc, fc, ctr, 1f, 1f, 1f, ux, uy, uz);
        }
    }

    // ================================================================ PALETAS
    private static final float[] STONE = {0.78f, 0.70f, 0.55f};
    private static final float[] STONE_D = {0.62f, 0.58f, 0.50f};
    private static final float[] GREY_STONE = {0.62f, 0.60f, 0.58f};
    private static final float[] GREY_STONE_D = {0.50f, 0.49f, 0.50f};
    private static final float[] WHITE_STONE = {0.90f, 0.86f, 0.76f};
    private static final float[] DARK = {0.25f, 0.17f, 0.13f};
    private static final float[] WOOD = {0.50f, 0.34f, 0.22f};
    private static final float[] WOOD_D = {0.36f, 0.24f, 0.16f};
    private static final float[] GLASS = {0.55f, 0.76f, 0.88f};
    private static final float[] MOSS = {0.42f, 0.58f, 0.22f};
    private static final float[] IVY = {0.30f, 0.50f, 0.20f};
    private static final float[] GOLD = {0.98f, 0.84f, 0.35f};

    private static final float[][] WALLS = {
            {0.93f, 0.86f, 0.70f}, {0.92f, 0.74f, 0.66f}, {0.74f, 0.82f, 0.88f}, {0.90f, 0.74f, 0.42f}};
    private static final float[][] ROOF_A = {
            {0.80f, 0.31f, 0.26f}, {0.38f, 0.48f, 0.62f}, {0.34f, 0.56f, 0.38f}, {0.60f, 0.38f, 0.26f}};
    private static final float[][] ROOF_B = {
            {0.68f, 0.22f, 0.20f}, {0.30f, 0.40f, 0.54f}, {0.27f, 0.46f, 0.31f}, {0.48f, 0.30f, 0.20f}};
    private static final float[][] CANOPY = {
            {0.55f, 0.65f, 0.22f}, {0.95f, 0.76f, 0.30f}, {0.82f, 0.30f, 0.38f}, {0.20f, 0.42f, 0.24f}};

    // ================================================================ CASAS
    /** Version simple (para lejos). */
    public static MeshBuilder houseSimple(int variant) {
        int v = variant & 3;
        MeshBuilder m = new MeshBuilder();
        m.box(0, 2.2f, 0, 3.4f, 2.2f, 2.8f, WALLS[v]);
        m.box(0, 1.2f, 2.82f, 0.8f, 1.2f, 0.06f, DARK);
        m.box(2.0f, 2.8f, 2.82f, 0.55f, 0.55f, 0.06f, DARK);
        float yb = 4.4f, yt = 7.2f;
        float[] fl = {-4.0f, yb, 3.4f}, fr = {4.0f, yb, 3.4f}, bl = {-4.0f, yb, -3.4f}, br = {4.0f, yb, -3.4f};
        float[] tl = {-1.6f, yt, 0f}, tr = {1.6f, yt, 0f};
        float cx = 0, cy = 5.2f, cz = 0;
        for (int k = 0; k < 4; k++) {
            float t0 = k / 4f, t1 = (k + 1) / 4f;
            float[] col = (k % 2 == 0) ? ROOF_A[v] : ROOF_B[v];
            m.quad(lerp(fl, tl, t0), lerp(fr, tr, t0), lerp(fr, tr, t1), lerp(fl, tl, t1), col, cx, cy, cz);
            m.quad(lerp(bl, tl, t0), lerp(br, tr, t0), lerp(br, tr, t1), lerp(bl, tl, t1), col, cx, cy, cz);
        }
        m.tri(fl, bl, tl, ROOF_B[v], cx, cy, cz);
        m.tri(fr, br, tr, ROOF_B[v], cx, cy, cz);
        return m;
    }

    private static float[] lerp(float[] a, float[] b, float t) {
        return new float[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    /** Casa detallada: base de piedra, entramado de madera, ventanas con postigos y flores, techo con tejas, chimenea con humo. */
    public static MeshBuilder house(int variant) {
        int v = variant & 3;
        float[] wall = WALLS[v], roofA = ROOF_A[v], roofB = ROOF_B[v];
        MeshBuilder m = new MeshBuilder();
        // cimiento y muros
        m.box(0, 0.35f, 0, 3.65f, 0.35f, 3.05f, STONE_D);
        for (int i = 0; i < 6; i++) {   // piedras del cimiento
            float x = -3.0f + i * 1.2f;
            m.box(x, 0.45f, 3.06f, 0.5f, 0.22f, 0.05f, (i % 2 == 0) ? STONE : GREY_STONE);
            m.box(x, 0.45f, -3.06f, 0.5f, 0.22f, 0.05f, (i % 2 == 0) ? GREY_STONE : STONE);
        }
        m.box(0, 2.55f, 0, 3.4f, 1.85f, 2.8f, wall);
        // entramado de madera
        float[] bm = WOOD_D;
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2)
            m.box(sx * 3.4f, 2.55f, sz * 2.8f, 0.15f, 1.9f, 0.15f, bm);
        m.box(0, 2.55f, 2.84f, 3.4f, 0.1f, 0.06f, bm);
        m.box(0, 2.55f, -2.84f, 3.4f, 0.1f, 0.06f, bm);
        m.box(3.44f, 2.55f, 0, 0.06f, 0.1f, 2.8f, bm);
        m.box(-3.44f, 2.55f, 0, 0.06f, 0.1f, 2.8f, bm);
        m.box(0, 4.45f, 0, 3.55f, 0.12f, 2.95f, bm);
        m.box(0, 0.75f, 0, 3.5f, 0.08f, 2.9f, bm);
        m.box(-0.1f, 2.55f, 2.84f, 0.08f, 1.85f, 0.06f, bm);
        m.box(2.55f, 2.55f, 2.84f, 0.08f, 1.85f, 0.06f, bm);
        m.boxRotZ(-2.5f, 3.4f, 2.86f, 0.05f, 0.75f, 0.04f, 0.9f, bm);   // tornapuntas
        m.boxRotZ(-2.5f, 3.4f, 2.86f, 0.05f, 0.75f, 0.04f, -0.9f, bm);
        // puerta con marco, escalon y farol
        m.box(-1.0f, 1.75f, 2.86f, 0.82f, 1.05f, 0.05f, bm);
        m.box(-1.0f, 1.7f, 2.9f, 0.62f, 0.95f, 0.04f, WOOD);
        m.box(-1.0f, 1.7f, 2.95f, 0.05f, 0.9f, 0.03f, WOOD_D);
        m.blob(-0.55f, 1.65f, 2.98f, 0.07f, 0.07f, 0.07f, 3, 5, 0f, 1, GOLD, true, 0f);
        m.box(-1.0f, 0.55f, 3.3f, 1.0f, 0.13f, 0.42f, GREY_STONE);
        m.box(-1.0f, 0.3f, 3.45f, 1.1f, 0.13f, 0.4f, GREY_STONE_D);
        m.box(-1.95f, 2.75f, 2.93f, 0.03f, 0.22f, 0.03f, bm);
        m.blob(-1.95f, 2.45f, 2.97f, 0.16f, 0.22f, 0.16f, 4, 6, 0f, 2, GOLD, true, 0.2f);
        // ventanas del frente
        window(m, 1.7f, 2.9f, 2.84f, roofA, bm);
        // ventanas laterales
        for (int s = -1; s <= 1; s += 2) {
            float x = s * 3.46f;
            m.box(x, 2.9f, 0.9f, 0.05f, 0.62f, 0.66f, bm);
            m.box(x + s * 0.02f, 2.9f, 0.9f, 0.05f, 0.5f, 0.52f, GLASS);
            m.box(x + s * 0.04f, 2.9f, 0.9f, 0.03f, 0.04f, 0.52f, bm);
            m.box(x + s * 0.04f, 2.9f, 0.9f, 0.03f, 0.5f, 0.04f, bm);
            m.box(x + s * 0.1f, 2.9f, 0.1f, 0.06f, 0.55f, 0.26f, roofB);
            m.box(x + s * 0.1f, 2.9f, 1.7f, 0.06f, 0.55f, 0.26f, roofB);
        }
        // techo con vuelo, tejas por franjas y cumbrera
        float yb = 4.5f, yt = 7.3f;
        float[] fl = {-4.3f, yb, 3.7f}, fr = {4.3f, yb, 3.7f}, bl = {-4.3f, yb, -3.7f}, br = {4.3f, yb, -3.7f};
        float[] tl = {-2.0f, yt, 0f}, tr = {2.0f, yt, 0f};
        float cx = 0, cy = 5.6f, cz = 0;
        int strips = 7;
        for (int k = 0; k < strips; k++) {
            float t0 = k / (float) strips, t1 = (k + 1) / (float) strips;
            float[] col = sc((k % 2 == 0) ? roofA : roofB, 0.92f + 0.12f * t1);
            m.quad(lerp(fl, tl, t0), lerp(fr, tr, t0), lerp(fr, tr, t1), lerp(fl, tl, t1), col, cx, cy, cz);
            m.quad(lerp(bl, tl, t0), lerp(br, tr, t0), lerp(br, tr, t1), lerp(bl, tl, t1), col, cx, cy, cz);
            // hilera de tejas: pequenas cajitas al borde de cada franja
            float yy = yb + (yt - yb) * t0, zz = 3.7f * (1f - t0);
            if (k > 0) {
                m.box(0, yy + 0.03f, zz + 0.04f, 4.1f - 2.1f * t0, 0.05f, 0.06f, sc(roofB, 0.8f));
                m.box(0, yy + 0.03f, -zz - 0.04f, 4.1f - 2.1f * t0, 0.05f, 0.06f, sc(roofB, 0.8f));
            }
        }
        m.tri(fl, bl, tl, roofB, cx, cy, cz);
        m.tri(fr, br, tr, roofB, cx, cy, cz);
        m.box(0, 7.36f, 0, 2.15f, 0.12f, 0.16f, sc(roofB, 0.75f));
        // buhardilla
        m.box(-1.6f, 5.7f, 2.05f, 0.55f, 0.5f, 0.42f, wall);
        m.box(-1.6f, 5.7f, 2.49f, 0.32f, 0.3f, 0.04f, GLASS);
        m.box(-1.6f, 5.7f, 2.51f, 0.36f, 0.34f, 0.02f, bm);
        m.cylinder(-1.6f, 6.15f, 2.05f, 0.95f, 0f, 0.7f, 4, roofA);
        // chimenea con humo
        m.box(2.6f, 6.5f, -0.7f, 0.42f, 1.4f, 0.42f, STONE_D);
        m.box(2.6f, 6.5f, -0.25f, 0.44f, 0.15f, 0.05f, STONE);
        m.box(2.6f, 7.95f, -0.7f, 0.52f, 0.1f, 0.52f, GREY_STONE);
        m.sway(8.0f, 2.0f);
        float[] smoke = c3(0.96f, 0.96f, 0.95f);
        m.blob(2.7f, 8.4f, -0.7f, 0.34f, 0.3f, 0.34f, 4, 6, 0.2f, 31, smoke, true, 0.3f);
        m.blob(3.0f, 9.0f, -0.7f, 0.45f, 0.38f, 0.45f, 4, 6, 0.2f, 32, sc(smoke, 0.97f), true, 0.3f);
        m.blob(3.4f, 9.8f, -0.7f, 0.58f, 0.46f, 0.58f, 4, 6, 0.2f, 33, sc(smoke, 0.94f), true, 0.3f);
        m.sway(0f, 0f);
        // barril, cajas, heno, calabazas
        m.cylinder(3.0f, 0.0f, 2.6f, 0.46f, 0.46f, 0.95f, 8, WOOD);
        m.cylinder(3.0f, 0.15f, 2.6f, 0.49f, 0.49f, 0.08f, 8, DARK);
        m.cylinder(3.0f, 0.72f, 2.6f, 0.49f, 0.49f, 0.08f, 8, DARK);
        m.box(-3.1f, 0.4f, 2.6f, 0.42f, 0.4f, 0.42f, WOOD);
        m.box(-2.95f, 1.2f, 2.55f, 0.3f, 0.3f, 0.3f, sc(WOOD, 1.1f));
        m.blobJ(-3.3f, 0.5f, -2.0f, 1.0f, 0.8f, 1.0f, 4, 7, 0.3f, 41, c3(0.93f, 0.80f, 0.38f), true, 0.5f, 0.2f);
        m.blob(-2.2f, 0.28f, 3.6f, 0.3f, 0.25f, 0.3f, 4, 6, 0.1f, 42, c3(0.95f, 0.55f, 0.15f), true, 0.4f);
        m.blob(-1.7f, 0.22f, 3.9f, 0.22f, 0.2f, 0.22f, 4, 6, 0.1f, 43, c3(0.93f, 0.50f, 0.12f), true, 0.4f);
        // matas de flores al pie de los muros
        for (int i = 0; i < 6; i++) {
            float x = -3.0f + i * 1.15f;
            float[][] fc = {c3(0.95f, 0.45f, 0.55f), c3(0.98f, 0.9f, 0.45f), c3(0.7f, 0.55f, 0.95f)};
            m.blobJ(x, 0.3f, -3.3f, 0.35f, 0.3f, 0.35f, 3, 5, 0.2f, 50 + i, IVY, true, 0.4f, 0.3f);
            m.blob(x, 0.6f, -3.3f, 0.12f, 0.12f, 0.12f, 2, 4, 0f, 60 + i, fc[i % 3], true, 0f);
        }
        return m;
    }

    private static void window(MeshBuilder m, float x, float y, float z, float[] shutter, float[] frame) {
        m.box(x, y, z, 0.68f, 0.62f, 0.05f, frame);
        m.box(x, y, z + 0.03f, 0.52f, 0.46f, 0.05f, GLASS);
        m.box(x, y, z + 0.07f, 0.52f, 0.04f, 0.03f, frame);
        m.box(x, y, z + 0.07f, 0.04f, 0.46f, 0.03f, frame);
        m.box(x - 0.92f, y, z + 0.06f, 0.26f, 0.58f, 0.05f, shutter);
        m.box(x + 0.92f, y, z + 0.06f, 0.26f, 0.58f, 0.05f, shutter);
        for (int i = -1; i <= 1; i += 2) {
            m.box(x - 0.92f, y + i * 0.2f, z + 0.1f, 0.2f, 0.03f, 0.02f, sc(shutter, 0.7f));
            m.box(x + 0.92f, y + i * 0.2f, z + 0.1f, 0.2f, 0.03f, 0.02f, sc(shutter, 0.7f));
        }
        // jardinera con flores
        m.box(x, y - 0.74f, z + 0.2f, 0.72f, 0.14f, 0.2f, WOOD);
        float[][] fc = {c3(0.95f, 0.40f, 0.50f), c3(0.98f, 0.85f, 0.30f), c3(0.95f, 0.95f, 0.9f), c3(0.9f, 0.35f, 0.6f)};
        for (int i = 0; i < 5; i++) {
            m.blobJ(x - 0.56f + i * 0.28f, y - 0.5f, z + 0.2f, 0.17f, 0.17f, 0.17f, 3, 5, 0.2f, 70 + i, i % 2 == 0 ? IVY : MOSS, true, 0.4f, 0.3f);
            m.blob(x - 0.56f + i * 0.28f, y - 0.36f, z + 0.2f, 0.1f, 0.1f, 0.1f, 2, 4, 0f, 80 + i, fc[i % 4], true, 0f);
        }
    }

    // ================================================================ ARBOLES
    /** Version simple (para lejos). */
    public static MeshBuilder treeSimple(int variant) {
        MeshBuilder m = new MeshBuilder();
        m.sway(4f, 8f);
        m.cylinder(0, 0, 0, 0.6f, 0.35f, 5.4f, 6, c3(0.42f, 0.29f, 0.20f));
        float[] c = CANOPY[variant & 3];
        if ((variant & 3) == 3) {
            m.cylinder(0, 1.5f, 0, 3.0f, 0f, 5.0f, 7, c);
            m.cylinder(0, 4.5f, 0, 2.2f, 0f, 4.5f, 7, sc(c, 1.1f));
            m.cylinder(0, 7.0f, 0, 1.4f, 0f, 3.8f, 7, sc(c, 1.2f));
        } else {
            m.blob(0, 7.2f, 0, 3.4f, 3.0f, 3.4f, 6, 8, 0.25f, 100 + variant, c, true, 0.5f);
            m.blob(1.6f, 8.6f, 0.6f, 2.3f, 2.0f, 2.3f, 5, 7, 0.25f, 200 + variant, c, true, 0.5f);
        }
        return m;
    }

    /**
     * Arbol detallado: raices, tronco con ramas, copa hecha de muchos mechones de hojas con color variado,
     * frutos o flores. La variante 3 es una conifera de capas.
     */
    public static MeshBuilder tree(int variant) {
        int v = variant & 3;
        MeshBuilder m = new MeshBuilder();
        Random r = new Random(1000 + v);
        float[] bark = c3(0.42f, 0.29f, 0.20f), barkD = c3(0.30f, 0.20f, 0.14f), barkL = c3(0.52f, 0.38f, 0.27f);
        // raices y tronco
        m.cylinder(0, 0, 0, 1.2f, 0.7f, 1.0f, 8, barkD);
        for (int i = 0; i < 5; i++) {
            float a = i * 1.2566f + 0.3f;
            m.limb((float) Math.cos(a) * 0.5f, 0.6f, (float) Math.sin(a) * 0.5f,
                    (float) Math.cos(a) * 1.9f, -0.05f, (float) Math.sin(a) * 1.9f, 0.34f, 0.1f, 5, barkD);
        }
        m.cylinder(0, 0.9f, 0, 0.7f, 0.52f, 2.2f, 8, bark);
        m.cylinder(0, 3.0f, 0, 0.52f, 0.38f, 2.6f, 7, barkL);
        for (int k = 0; k < 4; k++) {    // nudos
            float a = k * 1.7f;
            m.blob((float) Math.cos(a) * 0.55f, 1.4f + k * 0.8f, (float) Math.sin(a) * 0.55f, 0.18f, 0.2f, 0.18f, 3, 5, 0.1f, 5 + k, barkD, true, 0f);
        }
        if (v == 3) return conifer(m, r, bark);
        m.sway(4.5f, 9f);
        // ramas
        for (int i = 0; i < 5; i++) {
            float a = i * 1.2566f + 0.5f;
            float ex = (float) Math.cos(a) * 2.3f, ez = (float) Math.sin(a) * 2.3f;
            m.limb(0, 4.0f + i * 0.35f, 0, ex, 6.3f + i * 0.35f, ez, 0.2f, 0.09f, 5, bark);
        }
        float[] base = CANOPY[v];
        // nucleo oscuro (sombra interior) + mechones
        m.blobJ(0, 7.4f, 0, 2.9f, 2.5f, 2.9f, 5, 8, 0.2f, 300 + v, sc(base, 0.68f), true, 0.3f, 0.15f);
        for (int i = 0; i < 26; i++) {
            double th = r.nextDouble() * Math.PI * 2, ph = Math.acos(1.0 - r.nextDouble() * 1.7);
            float dx = (float) (Math.sin(ph) * Math.cos(th)), dy = (float) Math.cos(ph), dz = (float) (Math.sin(ph) * Math.sin(th));
            float cx = dx * 3.0f, cy = 7.4f + dy * 2.6f, cz = dz * 3.0f;
            float rad = 0.95f + r.nextFloat() * 0.65f;
            float light = 0.82f + 0.28f * (dy * 0.5f + 0.5f) + (r.nextFloat() - 0.5f) * 0.12f;
            float[] col = sc(base, light);
            if (v == 1 && r.nextFloat() < 0.25f) col = mix(col, c3(0.9f, 0.5f, 0.2f), 0.5f);   // otono
            if (v == 0 && r.nextFloat() < 0.15f) col = mix(col, c3(0.85f, 0.8f, 0.3f), 0.5f);
            m.blobJ(cx, cy, cz, rad, rad * 0.82f, rad, 3, 5, 0.3f, 400 + i + v * 50, col, true, 0.7f, 0.18f);
        }
        // copa alta
        m.blobJ(0.4f, 9.6f, 0.2f, 1.7f, 1.3f, 1.7f, 4, 6, 0.25f, 460 + v, sc(base, 1.12f), true, 0.7f, 0.15f);
        // frutos o flores
        if (v == 0 || v == 2) {
            float[] fc = v == 0 ? c3(0.88f, 0.22f, 0.20f) : c3(0.98f, 0.82f, 0.45f);
            for (int i = 0; i < 12; i++) {
                double th = r.nextDouble() * Math.PI * 2, ph = Math.acos(1.0 - r.nextDouble() * 1.4);
                float dx = (float) (Math.sin(ph) * Math.cos(th)), dy = (float) Math.cos(ph), dz = (float) (Math.sin(ph) * Math.sin(th));
                m.blob(dx * 3.7f, 7.4f + dy * 3.0f, dz * 3.7f, 0.2f, 0.2f, 0.2f, 2, 4, 0f, 500 + i, fc, true, 0f);
            }
        }
        // hongos al pie
        for (int i = 0; i < 3; i++) {
            float a = 0.8f + i * 2.1f;
            float px = (float) Math.cos(a) * 1.5f, pz = (float) Math.sin(a) * 1.5f;
            m.sway(0f, 0f);
            m.cylinder(px, 0, pz, 0.07f, 0.07f, 0.25f, 5, c3(0.95f, 0.93f, 0.85f));
            m.blob(px, 0.3f, pz, 0.22f, 0.12f, 0.22f, 3, 6, 0f, 520 + i, c3(0.85f, 0.25f, 0.25f), true, 0.4f);
        }
        return m;
    }

    private static MeshBuilder conifer(MeshBuilder m, Random r, float[] bark) {
        m.sway(3f, 9f);
        float[] g = CANOPY[3];
        float[] radii = {3.4f, 2.9f, 2.4f, 1.9f, 1.4f, 0.9f};
        float y = 1.6f;
        for (int i = 0; i < 6; i++) {
            float col = 0.85f + 0.07f * i + (r.nextFloat() - 0.5f) * 0.08f;
            m.cylinder(0, y, 0, radii[i], 0.12f, 2.6f, 9, sc(g, col));
            // faldon mas oscuro bajo cada piso
            m.cylinder(0, y - 0.15f, 0, radii[i] * 1.02f, radii[i] * 0.95f, 0.3f, 9, sc(g, col * 0.7f));
            // puntas de ramas
            for (int k = 0; k < 6; k++) {
                float a = k * 1.047f + i * 0.5f;
                m.limb((float) Math.cos(a) * radii[i] * 0.6f, y + 0.5f, (float) Math.sin(a) * radii[i] * 0.6f,
                        (float) Math.cos(a) * radii[i] * 1.12f, y - 0.1f, (float) Math.sin(a) * radii[i] * 1.12f,
                        0.12f, 0.03f, 4, sc(g, col * 0.8f));
            }
            y += 1.8f - i * 0.1f;
        }
        m.blob(0, y + 1.5f, 0, 0.3f, 0.5f, 0.3f, 3, 5, 0f, 7, GOLD, true, 0.3f);
        // pinas
        for (int i = 0; i < 6; i++) {
            float a = i * 1.05f;
            m.blob((float) Math.cos(a) * 1.9f, 3.6f + (i % 3) * 1.6f, (float) Math.sin(a) * 1.9f, 0.18f, 0.28f, 0.18f, 3, 5, 0.1f, 8 + i, c3(0.52f, 0.36f, 0.22f), true, 0.2f);
        }
        return m;
    }

    // ================================================================ ROCAS Y ARBUSTOS
    public static MeshBuilder rockSimple() {
        MeshBuilder m = new MeshBuilder();
        m.blob(0, 0.7f, 0, 1.8f, 1.1f, 1.5f, 4, 7, 0.35f, 5, c3(0.44f, 0.49f, 0.58f), false, 0.3f);
        return m;
    }

    public static MeshBuilder rock() {
        MeshBuilder m = new MeshBuilder();
        float[] g = c3(0.44f, 0.49f, 0.58f);
        m.blobJ(0, 0.7f, 0, 1.8f, 1.15f, 1.5f, 5, 8, 0.4f, 5, g, false, 0.4f, 0.18f);
        m.blobJ(1.5f, 0.35f, 0.6f, 0.9f, 0.6f, 0.8f, 4, 6, 0.4f, 6, sc(g, 1.08f), false, 0.4f, 0.18f);
        m.blobJ(-1.2f, 0.3f, -0.9f, 0.7f, 0.5f, 0.7f, 4, 6, 0.4f, 7, sc(g, 0.92f), false, 0.4f, 0.18f);
        // musgo arriba
        m.blobJ(0, 1.45f, 0, 1.15f, 0.35f, 0.95f, 3, 7, 0.4f, 8, MOSS, true, 0.5f, 0.3f);
        m.blobJ(1.5f, 0.85f, 0.6f, 0.55f, 0.2f, 0.5f, 3, 6, 0.3f, 9, sc(MOSS, 1.1f), true, 0.5f, 0.3f);
        // florecitas
        float[][] fc = {c3(0.95f, 0.45f, 0.55f), c3(0.98f, 0.9f, 0.45f)};
        for (int i = 0; i < 4; i++) {
            m.blob(-0.5f + i * 0.45f, 1.7f, 0.3f - i * 0.1f, 0.1f, 0.1f, 0.1f, 2, 4, 0f, 10 + i, fc[i % 2], true, 0f);
        }
        return m;
    }

    public static MeshBuilder bushSimple(int variant) {
        MeshBuilder m = new MeshBuilder();
        m.sway(0.3f, 1.5f);
        float[] c = variant == 0 ? c3(0.56f, 0.66f, 0.22f) : c3(0.92f, 0.42f, 0.48f);
        m.blob(0, 0.7f, 0, 1.4f, 0.9f, 1.4f, 4, 7, 0.3f, 9 + variant, c, true, 0.4f);
        return m;
    }

    public static MeshBuilder bush(int variant) {
        MeshBuilder m = new MeshBuilder();
        m.sway(0.2f, 1.8f);
        Random r = new Random(77 + variant);
        float[] c = variant == 0 ? c3(0.50f, 0.62f, 0.20f) : c3(0.62f, 0.60f, 0.22f);
        m.blobJ(0, 0.7f, 0, 1.3f, 0.85f, 1.3f, 4, 7, 0.25f, 90 + variant, sc(c, 0.7f), true, 0.3f, 0.2f);
        for (int i = 0; i < 7; i++) {
            float a = i * 0.9f + r.nextFloat();
            float d = 0.5f + r.nextFloat() * 0.6f;
            float rad = 0.55f + r.nextFloat() * 0.35f;
            m.blobJ((float) Math.cos(a) * d, 0.7f + r.nextFloat() * 0.5f, (float) Math.sin(a) * d, rad, rad * 0.85f, rad, 3, 5, 0.3f,
                    100 + i + variant * 10, sc(c, 0.9f + r.nextFloat() * 0.3f), true, 0.6f, 0.2f);
        }
        float[][] fc = variant == 0
                ? new float[][]{c3(0.95f, 0.45f, 0.55f), c3(0.98f, 0.95f, 0.85f)}
                : new float[][]{c3(0.92f, 0.28f, 0.28f), c3(0.95f, 0.55f, 0.15f)};
        for (int i = 0; i < 9; i++) {
            float a = r.nextFloat() * 6.28f;
            float d = 0.7f + r.nextFloat() * 0.5f;
            m.blob((float) Math.cos(a) * d, 0.7f + r.nextFloat() * 0.8f, (float) Math.sin(a) * d, 0.13f, 0.13f, 0.13f, 2, 4, 0f, 130 + i, fc[i % 2], true, 0f);
        }
        return m;
    }

    // ================================================================ TORRE
    public static MeshBuilder towerSimple() {
        MeshBuilder m = new MeshBuilder();
        m.cylinder(0, 0, 0, 2.7f, 2.3f, 15f, 8, GREY_STONE);
        m.cylinder(0, 15f, 0, 3.4f, 3.4f, 1.0f, 8, STONE);
        m.cylinder(0, 16f, 0, 3.5f, 0.0f, 5.5f, 8, ROOF_A[0]);
        m.box(0, 1.1f, 2.65f, 0.7f, 1.1f, 0.08f, DARK);
        return m;
    }

    /** Torre detallada: franjas de piedra, ventanas, almenas, techo conico con bandera ondeando, hiedra y puerta. */
    public static MeshBuilder tower() {
        MeshBuilder m = new MeshBuilder();
        m.cylinder(0, 0, 0, 3.5f, 3.0f, 1.3f, 12, STONE_D);
        m.cylinderBands(0, 1.2f, 0, 2.9f, 2.4f, 13.6f, 12, 9, GREY_STONE, sc(GREY_STONE, 0.85f));
        // contrafuertes
        for (int i = 0; i < 4; i++) {
            float a = i * 1.5708f + 0.785f;
            m.limb((float) Math.cos(a) * 3.2f, 0.0f, (float) Math.sin(a) * 3.2f, (float) Math.cos(a) * 2.7f, 4.2f, (float) Math.sin(a) * 2.7f, 0.55f, 0.25f, 5, STONE_D);
        }
        // ventanas arqueadas en 4 lados y 3 alturas
        for (int h = 0; h < 3; h++) {
            float y = 4.6f + h * 3.4f;
            for (int s = 0; s < 4; s++) {
                float a = s * 1.5708f + (h % 2) * 0.785f;
                float cx = (float) Math.cos(a), cz = (float) Math.sin(a);
                float rr = 2.9f - (y / 15f) * 0.5f + 0.05f;
                windowAt(m, cx * rr, y, cz * rr, a);
            }
        }
        // puerta
        m.box(0, 1.1f + 1.2f, 3.0f, 0.95f, 1.2f, 0.07f, WOOD_D);
        m.box(0, 2.0f, 3.05f, 0.75f, 1.0f, 0.04f, WOOD);
        m.blob(0, 2.45f, 3.1f, 0.62f, 0.4f, 0.1f, 3, 6, 0f, 3, WOOD, true, 0.3f);
        m.box(0, 0.7f, 3.5f, 1.2f, 0.12f, 0.5f, GREY_STONE_D);
        // balcon y almenas
        m.cylinder(0, 14.5f, 0, 3.7f, 3.4f, 0.9f, 12, STONE);
        m.cylinder(0, 15.4f, 0, 3.7f, 3.7f, 0.25f, 12, STONE_D);
        for (int i = 0; i < 12; i++) {
            float a = i * 0.5236f;
            m.box((float) Math.cos(a) * 3.5f, 16.2f, (float) Math.sin(a) * 3.5f, 0.38f, 0.6f, 0.38f, (i % 2 == 0) ? STONE : sc(STONE, 0.9f));
        }
        // torreta central y techo conico en tres tramos de tejas
        m.cylinder(0, 15.6f, 0, 2.5f, 2.3f, 3.2f, 12, sc(GREY_STONE, 1.05f));
        m.cylinder(0, 18.6f, 0, 3.1f, 2.1f, 1.7f, 12, ROOF_A[0]);
        m.cylinder(0, 20.3f, 0, 2.1f, 1.1f, 1.7f, 12, ROOF_B[0]);
        m.cylinder(0, 22.0f, 0, 1.1f, 0.0f, 1.9f, 12, ROOF_A[0]);
        m.blob(0, 24.0f, 0, 0.35f, 0.35f, 0.35f, 4, 6, 0f, 4, GOLD, true, 0.4f);
        // mastil y bandera ondeando
        m.box(0, 25.2f, 0, 0.06f, 1.3f, 0.06f, WOOD_D);
        m.sway(24.4f, 1.6f);
        float[] flag = c3(0.85f, 0.28f, 0.42f);
        m.box(0.95f, 26.2f, 0, 0.9f, 0.5f, 0.03f, flag);
        m.box(1.8f, 26.2f, 0, 0.06f, 0.5f, 0.04f, sc(flag, 1.2f));
        m.sway(0f, 0f);
        // hiedra
        for (int i = 0; i < 10; i++) {
            float y = 1.8f + i * 1.05f;
            float a = 0.5f + i * 0.35f;
            float rr = 2.75f - (y / 15f) * 0.5f;
            m.blobJ((float) Math.cos(a) * rr, y, (float) Math.sin(a) * rr, 0.55f, 0.5f, 0.55f, 3, 5, 0.3f, 600 + i, IVY, true, 0.5f, 0.3f);
            if (i % 3 == 0) m.blob((float) Math.cos(a) * (rr + 0.4f), y, (float) Math.sin(a) * (rr + 0.4f), 0.12f, 0.12f, 0.12f, 2, 4, 0f, 620 + i, c3(0.95f, 0.5f, 0.6f), true, 0f);
        }
        return m;
    }

    /** Ventana arqueada pegada al muro de la torre en (x,y,z) mirando hacia afuera con angulo a. */
    private static void windowAt(MeshBuilder m, float x, float y, float z, float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        // caja pequena orientada hacia afuera (cajas alineadas a ejes: usamos el eje dominante)
        boolean alongX = Math.abs(c) > Math.abs(s);
        if (alongX) {
            float sx = c > 0 ? 1 : -1;
            m.box(x + sx * 0.05f, y, z, 0.1f, 0.75f, 0.5f, WOOD_D);
            m.box(x + sx * 0.1f, y, z, 0.05f, 0.62f, 0.36f, DARK);
            m.box(x + sx * 0.12f, y + 0.55f, z, 0.05f, 0.18f, 0.3f, DARK);
        } else {
            float sz = s > 0 ? 1 : -1;
            m.box(x, y, z + sz * 0.05f, 0.5f, 0.75f, 0.1f, WOOD_D);
            m.box(x, y, z + sz * 0.1f, 0.36f, 0.62f, 0.05f, DARK);
            m.box(x, y + 0.55f, z + sz * 0.12f, 0.3f, 0.18f, 0.05f, DARK);
        }
    }

    // ================================================================ MOLINO
    public static MeshBuilder windmillSimple() {
        MeshBuilder m = new MeshBuilder();
        m.cylinder(0, 0, 0, 2.8f, 1.7f, 9f, 8, WHITE_STONE);
        m.cylinder(0, 9f, 0, 2.2f, 0.0f, 3.6f, 8, ROOF_A[0]);
        m.box(0, 1.0f, 2.7f, 0.7f, 1.0f, 0.08f, DARK);
        m.box(0, 8.0f, 2.2f, 3.1f, 0.38f, 0.1f, WOOD);
        m.box(0, 8.0f, 2.25f, 0.38f, 3.1f, 0.1f, WOOD);
        return m;
    }

    /** Cuerpo del molino detallado (sin aspas). Las aspas son otra malla que gira. */
    public static MeshBuilder windmillBody() {
        MeshBuilder m = new MeshBuilder();
        m.cylinder(0, 0, 0, 3.1f, 2.9f, 0.9f, 10, STONE_D);
        m.cylinderBands(0, 0.8f, 0, 2.85f, 1.75f, 8.4f, 10, 7, WHITE_STONE, sc(WHITE_STONE, 0.9f));
        // aros de madera
        m.cylinder(0, 3.2f, 0, 2.5f, 2.5f, 0.2f, 10, WOOD_D);
        m.cylinder(0, 6.0f, 0, 2.05f, 2.05f, 0.2f, 10, WOOD_D);
        // puerta y ventanas
        m.box(0, 1.0f, 2.85f, 0.8f, 1.0f, 0.08f, WOOD_D);
        m.box(0, 0.95f, 2.9f, 0.62f, 0.85f, 0.04f, WOOD);
        m.box(0, 0.35f, 3.3f, 1.1f, 0.12f, 0.45f, GREY_STONE_D);
        m.box(0, 4.6f, 2.45f, 0.45f, 0.5f, 0.07f, WOOD_D);
        m.box(0, 4.6f, 2.5f, 0.33f, 0.4f, 0.05f, GLASS);
        m.box(0, 4.6f, 2.53f, 0.33f, 0.03f, 0.03f, WOOD_D);
        m.box(0, 4.6f, 2.53f, 0.03f, 0.4f, 0.03f, WOOD_D);
        m.box(2.0f, 6.8f, 0.5f, 0.07f, 0.4f, 0.32f, WOOD_D);
        // sombrero del molino
        m.cylinder(0, 9.0f, 0, 2.3f, 2.3f, 0.5f, 10, WOOD_D);
        m.cylinder(0, 9.4f, 0, 2.6f, 1.3f, 1.8f, 10, ROOF_A[0]);
        m.cylinder(0, 11.2f, 0, 1.3f, 0f, 1.8f, 10, ROOF_B[0]);
        m.blob(0, 13.0f, 0, 0.25f, 0.25f, 0.25f, 3, 5, 0f, 6, GOLD, true, 0.3f);
        // cabeza del eje
        m.box(0, 8.0f, 2.2f, 0.55f, 0.55f, 0.9f, WOOD_D);
        m.limb(0, 8.0f, 2.5f, 0, 8.0f, 3.5f, 0.28f, 0.2f, 6, WOOD);
        // sacos y barriles
        m.blobJ(-2.5f, 0.5f, 2.2f, 0.6f, 0.55f, 0.5f, 4, 6, 0.2f, 12, c3(0.92f, 0.85f, 0.7f), true, 0.5f, 0.15f);
        m.blobJ(-3.1f, 0.45f, 1.5f, 0.5f, 0.5f, 0.45f, 4, 6, 0.2f, 13, c3(0.88f, 0.8f, 0.64f), true, 0.5f, 0.15f);
        m.cylinder(2.8f, 0, 2.0f, 0.5f, 0.5f, 1.0f, 8, WOOD);
        m.cylinder(2.8f, 0.2f, 2.0f, 0.53f, 0.53f, 0.08f, 8, DARK);
        // hiedra
        for (int i = 0; i < 6; i++) {
            float y = 1.0f + i * 1.0f;
            float a = 2.4f + i * 0.4f;
            float rr = 2.8f - y * 0.11f;
            m.blobJ((float) Math.cos(a) * rr, y, (float) Math.sin(a) * rr, 0.5f, 0.45f, 0.5f, 3, 5, 0.3f, 700 + i, IVY, true, 0.5f, 0.3f);
        }
        return m;
    }

    /** Aspas del molino con el centro en el origen (se giran sobre el eje Z). */
    public static MeshBuilder windmillBlades() {
        MeshBuilder m = new MeshBuilder();
        float[] sail = c3(0.95f, 0.92f, 0.82f);
        float[] sailAlt = c3(0.85f, 0.35f, 0.32f);
        for (int i = 0; i < 4; i++) {
            float a = i * 1.5708f;
            float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
            // larguero (a lo largo del aspa, que sale en direccion (-sa, ca) rotada)
            m.boxRotZ(-sa * 3.4f, ca * 3.4f, 0f, 0.14f, 3.5f, 0.11f, a, WOOD_D);
            // vela a un costado
            float ox = ca * 0.62f, oy = sa * 0.62f;
            m.boxRotZ(-sa * 3.6f + ox, ca * 3.6f + oy, 0.05f, 0.5f, 2.6f, 0.04f, a, (i % 2 == 0) ? sail : sailAlt);
            // travesanos
            for (int k = 1; k <= 4; k++) {
                float d = 0.9f + k * 0.6f;
                m.boxRotZ(-sa * d + ox, ca * d + oy, 0.1f, 0.5f, 0.04f, 0.04f, a, WOOD);
            }
        }
        m.blob(0, 0, 0.15f, 0.45f, 0.45f, 0.4f, 4, 6, 0f, 14, WOOD, true, 0.3f);
        m.blob(0, 0, 0.5f, 0.18f, 0.18f, 0.2f, 3, 5, 0f, 15, GOLD, true, 0.3f);
        return m;
    }

    // ================================================================ ARCO
    public static MeshBuilder archSimple() {
        MeshBuilder m = new MeshBuilder();
        m.box(-3.3f, 3.6f, 0, 1.1f, 3.6f, 1.1f, GREY_STONE);
        m.box(3.3f, 3.6f, 0, 1.1f, 3.6f, 1.1f, GREY_STONE);
        m.box(0, 8.0f, 0, 5.0f, 0.8f, 1.2f, STONE);
        return m;
    }

    /** Arco de dovelas con clave, pilares de bloques, hiedra colgando, farol y musgo. */
    public static MeshBuilder arch() {
        MeshBuilder m = new MeshBuilder();
        for (int s = -1; s <= 1; s += 2) {
            float x = s * 3.3f;
            m.box(x, 0.3f, 0, 1.55f, 0.3f, 1.55f, STONE_D);
            for (int k = 0; k < 5; k++) {   // bloques del pilar
                float[] c = (k % 2 == 0) ? GREY_STONE : GREY_STONE_D;
                m.box(x + (k % 2 == 0 ? 0.04f : -0.04f), 1.0f + k * 1.0f, 0, 1.1f, 0.5f, 1.1f, c);
            }
            m.box(x, 6.2f, 0, 1.4f, 0.25f, 1.4f, STONE);   // capitel
        }
        // dovelas del arco (semicirculo de radio 3.3 centrado en (0,6.3))
        int n = 9;
        for (int k = 0; k < n; k++) {
            float a = (float) (Math.PI * (k + 0.5f) / n);
            float cx = (float) Math.cos(a) * 3.3f, cy = 6.45f + (float) Math.sin(a) * 3.3f;
            boolean key = k == n / 2;
            m.boxRotZ(cx, cy, 0, 0.55f, key ? 0.75f : 0.62f, 1.05f, a, key ? sc(STONE, 1.1f) : ((k % 2 == 0) ? STONE : GREY_STONE));
        }
        // farol colgando de la clave
        m.limb(0, 9.5f, 0, 0, 8.6f, 0, 0.03f, 0.03f, 4, DARK);
        m.box(0, 8.3f, 0, 0.22f, 0.3f, 0.22f, DARK);
        m.blob(0, 8.3f, 0, 0.15f, 0.2f, 0.15f, 3, 5, 0f, 4, GOLD, true, 0.2f);
        // hiedra colgando
        m.sway(2.0f, 6.0f);
        for (int i = 0; i < 9; i++) {
            float a = 0.2f + i * 0.33f;
            float cx = (float) Math.cos(a) * 3.3f, cy = 6.4f + (float) Math.sin(a) * 3.1f;
            m.blobJ(cx, cy - 0.2f, 1.1f, 0.35f, 0.4f, 0.3f, 3, 5, 0.3f, 800 + i, IVY, true, 0.5f, 0.3f);
        }
        for (int i = 0; i < 6; i++) {
            m.blobJ(3.9f, 6.0f - i * 0.9f, 1.15f, 0.34f, 0.45f, 0.3f, 3, 5, 0.3f, 820 + i, IVY, true, 0.5f, 0.3f);
        }
        m.sway(0f, 0f);
        // musgo y flores al pie
        for (int s = -1; s <= 1; s += 2) {
            m.blobJ(s * 3.3f, 0.8f, 1.4f, 0.8f, 0.3f, 0.5f, 3, 6, 0.3f, 840 + s, MOSS, true, 0.5f, 0.3f);
            for (int i = 0; i < 4; i++) {
                float[] fc = (i % 2 == 0) ? c3(0.95f, 0.45f, 0.55f) : c3(0.98f, 0.9f, 0.45f);
                m.blob(s * 3.3f - 0.6f + i * 0.4f, 0.9f, 1.55f, 0.1f, 0.1f, 0.1f, 2, 4, 0f, 850 + i, fc, true, 0f);
            }
        }
        return m;
    }

    // ================================================================ RUINAS
    public static MeshBuilder ruinSimple() {
        MeshBuilder m = new MeshBuilder();
        float[] hs = {5.0f, 3.2f, 4.4f};
        for (int i = 0; i < 3; i++) m.cylinder((i - 1) * 2.5f, 0, 0, 0.9f, 0.75f, hs[i], 7, STONE);
        return m;
    }

    /** Columnas acanaladas con base y capitel, rotas, con tambores caidos, escombros, musgo e hiedra. */
    public static MeshBuilder ruin() {
        MeshBuilder m = new MeshBuilder();
        float[] hs = {5.2f, 3.4f, 4.6f};
        for (int i = 0; i < 3; i++) {
            float x = (i - 1) * 2.5f;
            m.box(x, 0.2f, 0, 1.15f, 0.2f, 1.15f, STONE_D);
            m.cylinderBands(x, 0.4f, 0, 0.95f, 0.72f, hs[i] - 0.4f, 12, 4, STONE, sc(STONE, 0.88f));
            if (i != 1) {
                m.box(x, hs[i] + 0.15f, 0, 1.05f, 0.18f, 1.05f, STONE);   // capitel
                m.box(x, hs[i] + 0.45f, 0, 0.9f, 0.1f, 0.9f, STONE_D);
            } else {
                // rota: borde irregular
                m.blobJ(x, hs[i] - 0.1f, 0, 0.75f, 0.3f, 0.75f, 3, 7, 0.5f, 900, STONE, false, 0.3f, 0.2f);
            }
        }
        m.boxRotZ(-1.25f, 5.9f, 0, 1.9f, 0.28f, 0.7f, 0.04f, GREY_STONE);   // dintel apoyado
        // tambor caido
        m.limb(-0.2f, 0.65f, 2.2f, 1.9f, 0.65f, 2.7f, 0.7f, 0.7f, 10, STONE);
        m.limb(-3.4f, 0.55f, -2.2f, -4.8f, 0.55f, -1.4f, 0.6f, 0.6f, 9, sc(STONE, 0.9f));
        // escombros
        for (int i = 0; i < 6; i++) {
            float a = i * 1.1f;
            m.blobJ((float) Math.cos(a) * 3.2f, 0.25f, (float) Math.sin(a) * 2.6f, 0.45f + 0.1f * (i % 3), 0.3f, 0.4f, 3, 5, 0.4f, 910 + i, GREY_STONE, false, 0.4f, 0.2f);
        }
        // musgo, hiedra, flores
        m.blobJ(-2.5f, 0.55f, 1.1f, 0.9f, 0.3f, 0.6f, 3, 6, 0.3f, 930, MOSS, true, 0.5f, 0.3f);
        m.blobJ(2.5f, 0.55f, 1.1f, 0.8f, 0.3f, 0.55f, 3, 6, 0.3f, 931, MOSS, true, 0.5f, 0.3f);
        m.sway(1.0f, 4.0f);
        for (int i = 0; i < 6; i++) {
            m.blobJ(2.5f + 0.5f, 0.8f + i * 0.7f, 0.5f, 0.35f, 0.4f, 0.3f, 3, 5, 0.3f, 940 + i, IVY, true, 0.5f, 0.3f);
        }
        m.sway(0f, 0f);
        for (int i = 0; i < 6; i++) {
            float[] fc = (i % 2 == 0) ? c3(0.95f, 0.45f, 0.55f) : c3(0.98f, 0.9f, 0.45f);
            m.blob(-2.9f + i * 1.1f, 0.5f, 1.8f, 0.12f, 0.12f, 0.12f, 2, 4, 0f, 960 + i, fc, true, 0f);
        }
        return m;
    }

    // ================================================================ ENEMIGO
    /** Mancha de tinta enemiga: bola rosada con un ojo grande hacia +Z. */
    public static MeshBuilder enemy() {
        MeshBuilder m = new MeshBuilder();
        float[] pink = c3(0.93f, 0.42f, 0.55f);
        m.blobJ(0, 1.2f, 0, 1.3f, 1.2f, 1.3f, 8, 10, 0.12f, 3, pink, true, 0.5f, 0.1f);
        m.blob(0, 1.5f, 1.05f, 0.62f, 0.62f, 0.4f, 6, 8, 0f, 4, c3(0.98f, 0.97f, 0.92f), true, 0f);
        m.blob(0, 1.5f, 1.38f, 0.28f, 0.28f, 0.16f, 5, 7, 0f, 5, c3(0.08f, 0.06f, 0.07f), true, 0f);
        m.blob(-0.8f, 2.3f, 0, 0.3f, 0.55f, 0.3f, 4, 6, 0f, 6, pink, true, 0.3f);
        m.blob(0.8f, 2.3f, 0, 0.3f, 0.55f, 0.3f, 4, 6, 0f, 7, pink, true, 0.3f);
        // goticas de tinta al pie
        m.blob(-0.9f, 0.15f, 0.6f, 0.35f, 0.15f, 0.35f, 3, 6, 0.2f, 8, sc(pink, 0.8f), true, 0.2f);
        m.blob(0.8f, 0.12f, -0.7f, 0.3f, 0.12f, 0.3f, 3, 6, 0.2f, 9, sc(pink, 0.8f), true, 0.2f);
        return m;
    }
}
