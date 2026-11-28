package com.craterz;

/**
 * Constructor de mallas procedurales. Vértice de 15 floats:
 * pos(3) normal(3) tangente(3) uv(2) color(3) humedad(1). Triángulos sin índices, sentido antihorario.
 */
public final class MeshBuilder {
    public static final int STRIDE = 15;

    private float[] data = new float[STRIDE * 3 * 256];
    private int count;
    private final float[] m = new float[16];
    private final float[][] stack = new float[16][16];
    private int sp;
    private final float[] tp = new float[3], tn = new float[3], tt = new float[3];

    public float cr = 1, cg = 1, cb = 1, wet = 0;
    /** Si no es null, tri() usa estos colores por vértice (rgb*3) y humedad (3). */
    public float[] pvc, pvw;
    public float uOff, vOff;
    private boolean useRect;
    private float ru0, rv0, ru1, rv1;

    public MeshBuilder() { Mat4.identity(m); }

    public int vertexCount() { return count; }

    public float[] toArray() {
        float[] r = new float[count * STRIDE];
        System.arraycopy(data, 0, r, 0, r.length);
        return r;
    }

    public void color(float r, float g, float b) { cr = r; cg = g; cb = b; }
    public void color(float r, float g, float b, float w) { cr = r; cg = g; cb = b; wet = w; }

    /** Mapea cada cara completa a este rectángulo del atlas (sin repetir). */
    public void uvRect(float u0, float v0, float u1, float v1) { useRect = true; ru0 = u0; rv0 = v0; ru1 = u1; rv1 = v1; }
    public void uvTiled() { useRect = false; }

    // ---- transformaciones
    public void identity() { Mat4.identity(m); }
    public void push() { System.arraycopy(m, 0, stack[sp++], 0, 16); }
    public void pop() { System.arraycopy(stack[--sp], 0, m, 0, 16); }
    public void translate(float x, float y, float z) { Mat4.translate(m, x, y, z); }
    public void rotX(float d) { Mat4.rotateX(m, d); }
    public void rotY(float d) { Mat4.rotateY(m, d); }
    public void rotZ(float d) { Mat4.rotateZ(m, d); }
    public void scale(float x, float y, float z) { Mat4.scale(m, x, y, z); }
    public void mult(float[] a) { Mat4.multiply(m, m, a); }

    /** Orienta el eje -Y local hacia (dx,dy,dz) y coloca el origen en (px,py,pz). */
    public void aim(float px, float py, float pz, float dx, float dy, float dz) {
        float l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (l < 1e-6f) return;
        float yx = -dx / l, yy = -dy / l, yz = -dz / l;
        float hx = 0, hy = 0, hz = 1;
        if (Math.abs(yz) > 0.9f) { hx = 1; hy = 0; hz = 0; }
        float xx = hy * yz - hz * yy, xy = hz * yx - hx * yz, xz = hx * yy - hy * yx;
        float xl = (float) Math.sqrt(xx * xx + xy * xy + xz * xz);
        xx /= xl; xy /= xl; xz /= xl;
        float zx = xy * yz - xz * yy, zy = xz * yx - xx * yz, zz = xx * yy - xy * yx;
        float[] a = {xx, xy, xz, 0, yx, yy, yz, 0, zx, zy, zz, 0, px, py, pz, 1};
        mult(a);
    }

    // ---- vértices
    private void ensure() {
        if ((count + 1) * STRIDE > data.length) {
            float[] n = new float[data.length * 2];
            System.arraycopy(data, 0, n, 0, count * STRIDE);
            data = n;
        }
    }

    public void vert(float px, float py, float pz, float nx, float ny, float nz,
                     float tx, float ty, float tz, float u, float v) {
        ensure();
        Mat4.point(m, px, py, pz, tp);
        Mat4.dir(m, nx, ny, nz, tn);
        Mat4.dir(m, tx, ty, tz, tt);
        float nl = (float) Math.sqrt(tn[0] * tn[0] + tn[1] * tn[1] + tn[2] * tn[2]);
        if (nl > 1e-6f) { tn[0] /= nl; tn[1] /= nl; tn[2] /= nl; }
        float tl = (float) Math.sqrt(tt[0] * tt[0] + tt[1] * tt[1] + tt[2] * tt[2]);
        if (tl > 1e-6f) { tt[0] /= tl; tt[1] /= tl; tt[2] /= tl; }
        int i = count * STRIDE;
        data[i] = tp[0]; data[i + 1] = tp[1]; data[i + 2] = tp[2];
        data[i + 3] = tn[0]; data[i + 4] = tn[1]; data[i + 5] = tn[2];
        data[i + 6] = tt[0]; data[i + 7] = tt[1]; data[i + 8] = tt[2];
        data[i + 9] = u + uOff; data[i + 10] = v + vOff;
        data[i + 11] = cr; data[i + 12] = cg; data[i + 13] = cb; data[i + 14] = wet;
        count++;
    }

    /** Triángulo con normales dadas; corrige el sentido de giro para que mire hacia donde apuntan las normales. */
    public void tri(float[] p, float[] n, float[] t, float[] uv, int a, int b, int c) {
        float ex = p[b * 3] - p[a * 3], ey = p[b * 3 + 1] - p[a * 3 + 1], ez = p[b * 3 + 2] - p[a * 3 + 2];
        float fx = p[c * 3] - p[a * 3], fy = p[c * 3 + 1] - p[a * 3 + 1], fz = p[c * 3 + 2] - p[a * 3 + 2];
        float gx = ey * fz - ez * fy, gy = ez * fx - ex * fz, gz = ex * fy - ey * fx;
        float sx = n[a * 3] + n[b * 3] + n[c * 3], sy = n[a * 3 + 1] + n[b * 3 + 1] + n[c * 3 + 1], sz = n[a * 3 + 2] + n[b * 3 + 2] + n[c * 3 + 2];
        if (gx * sx + gy * sy + gz * sz < 0) { int tmp = b; b = c; c = tmp; }
        int[] idx = {a, b, c};
        for (int k = 0; k < 3; k++) {
            int j = idx[k];
            if (pvc != null) { cr = pvc[j * 3]; cg = pvc[j * 3 + 1]; cb = pvc[j * 3 + 2]; wet = pvw[j]; }
            vert(p[j * 3], p[j * 3 + 1], p[j * 3 + 2], n[j * 3], n[j * 3 + 1], n[j * 3 + 2],
                    t[j * 3], t[j * 3 + 1], t[j * 3 + 2], uv[j * 2], uv[j * 2 + 1]);
        }
    }

    private final float[] qp = new float[12], qn = new float[12], qt = new float[12], qu = new float[8];

    /** Cuadrilátero plano p0..p3 (antihorario visto desde el lado visible). */
    public void quad(float x0, float y0, float z0, float x1, float y1, float z1,
                     float x2, float y2, float z2, float x3, float y3, float z3, float uvScale) {
        float ux = x1 - x0, uy = y1 - y0, uz = z1 - z0;
        float vx = x3 - x0, vy = y3 - y0, vz = z3 - z0;
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        float nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (nl < 1e-9f) return;
        nx /= nl; ny /= nl; nz /= nl;
        float ul = (float) Math.sqrt(ux * ux + uy * uy + uz * uz), vl = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
        float tx = ux / (ul + 1e-9f), ty = uy / (ul + 1e-9f), tz = uz / (ul + 1e-9f);
        float u0, v0, u1, v1;
        if (useRect) { u0 = ru0; v0 = rv0; u1 = ru1; v1 = rv1; }
        else { u0 = 0; v0 = 0; u1 = ul * uvScale; v1 = vl * uvScale; }
        float[] P = {x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3};
        float[] U = {u0, v0, u1, v0, u1, v1, u0, v1};
        for (int k = 0; k < 4; k++) {
            qp[k * 3] = P[k * 3]; qp[k * 3 + 1] = P[k * 3 + 1]; qp[k * 3 + 2] = P[k * 3 + 2];
            qn[k * 3] = nx; qn[k * 3 + 1] = ny; qn[k * 3 + 2] = nz;
            qt[k * 3] = tx; qt[k * 3 + 1] = ty; qt[k * 3 + 2] = tz;
        }
        System.arraycopy(U, 0, qu, 0, 8);
        tri(qp, qn, qt, qu, 0, 1, 2);
        tri(qp, qn, qt, qu, 0, 2, 3);
    }

    private final float[] hc = new float[24];

    private void hq(int a, int b, int c, int d, float s) {
        quad(hc[a * 3], hc[a * 3 + 1], hc[a * 3 + 2], hc[b * 3], hc[b * 3 + 1], hc[b * 3 + 2],
                hc[c * 3], hc[c * 3 + 1], hc[c * 3 + 2], hc[d * 3], hc[d * 3 + 1], hc[d * 3 + 2], s);
    }

    private static int ci(int sx, int sy, int sz) { return sx + 2 * sy + 4 * sz; }

    /** Caja general: esquina k con bits (sx + 2*sy + 4*sz), 1 = lado positivo. */
    public void boxCorners(float[] c, float s) {
        System.arraycopy(c, 0, hc, 0, 24);
        hq(ci(1, 0, 1), ci(1, 0, 0), ci(1, 1, 0), ci(1, 1, 1), s); // +X
        hq(ci(0, 0, 0), ci(0, 0, 1), ci(0, 1, 1), ci(0, 1, 0), s); // -X
        hq(ci(0, 1, 1), ci(1, 1, 1), ci(1, 1, 0), ci(0, 1, 0), s); // +Y
        hq(ci(0, 0, 0), ci(1, 0, 0), ci(1, 0, 1), ci(0, 0, 1), s); // -Y
        hq(ci(0, 0, 1), ci(1, 0, 1), ci(1, 1, 1), ci(0, 1, 1), s); // +Z
        hq(ci(1, 0, 0), ci(0, 0, 0), ci(0, 1, 0), ci(1, 1, 0), s); // -Z
    }

    private final float[] bc = new float[24];

    public void box(float cx, float cy, float cz, float hx, float hy, float hz, float s) {
        for (int k = 0; k < 8; k++) {
            bc[k * 3] = cx + ((k & 1) != 0 ? hx : -hx);
            bc[k * 3 + 1] = cy + ((k & 2) != 0 ? hy : -hy);
            bc[k * 3 + 2] = cz + ((k & 4) != 0 ? hz : -hz);
        }
        boxCorners(bc, s);
    }


    // ------------------------------------------------------------ caja biselada
    private void outQuad(float[] a, float[] b, float[] c, float[] d, float hx, float hy, float hz, float s) {
        float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2], vx = d[0] - a[0], vy = d[1] - a[1], vz = d[2] - a[2];
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        if (nx * hx + ny * hy + nz * hz >= 0) quad(a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2], d[0], d[1], d[2], s);
        else quad(a[0], a[1], a[2], d[0], d[1], d[2], c[0], c[1], c[2], b[0], b[1], b[2], s);
    }

    private void outTri(float[] a, float[] b, float[] c, float hx, float hy, float hz, float s) {
        float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2], vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        float nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (nl < 1e-12f) return;
        nx /= nl; ny /= nl; nz /= nl;
        if (nx * hx + ny * hy + nz * hz < 0) { nx = -nx; ny = -ny; nz = -nz; }
        float ul = (float) Math.sqrt(ux * ux + uy * uy + uz * uz) + 1e-9f;
        float[] P = {a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2]};
        for (int k = 0; k < 3; k++) {
            qp[k * 3] = P[k * 3]; qp[k * 3 + 1] = P[k * 3 + 1]; qp[k * 3 + 2] = P[k * 3 + 2];
            qn[k * 3] = nx; qn[k * 3 + 1] = ny; qn[k * 3 + 2] = nz;
            qt[k * 3] = ux / ul; qt[k * 3 + 1] = uy / ul; qt[k * 3 + 2] = uz / ul;
        }
        qu[0] = 0; qu[1] = 0; qu[2] = ul * s; qu[3] = 0; qu[4] = 0; qu[5] = ul * s;
        tri(qp, qn, qt, qu, 0, 1, 2);
    }

    /** Caja con aristas achaflanadas (brillos en los bordes, mucho menos "de bloques"). */
    public void bbox(float cx, float cy, float cz, float hx, float hy, float hz, float s) {
        float mn = Math.min(hx, Math.min(hy, hz));
        if (mn < 0.0065f) { box(cx, cy, cz, hx, hy, hz, s); return; }
        float b = Math.min(0.0042f, mn * 0.45f);
        float[] h = {hx, hy, hz}, c0 = {cx, cy, cz};
        // caras: eje a con signo sa, ejes tangentes t1,t2
        for (int ax = 0; ax < 3; ax++)
            for (int sg = -1; sg <= 1; sg += 2) {
                int t1 = (ax + 1) % 3, t2 = (ax + 2) % 3;
                float[][] q = new float[4][3];
                int[][] sc = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
                for (int k = 0; k < 4; k++) {
                    q[k][ax] = c0[ax] + sg * h[ax];
                    q[k][t1] = c0[t1] + sc[k][0] * (h[t1] - b);
                    q[k][t2] = c0[t2] + sc[k][1] * (h[t2] - b);
                }
                float[] hint = new float[3]; hint[ax] = sg;
                outQuad(q[0], q[1], q[2], q[3], hint[0], hint[1], hint[2], s);
            }
        // aristas (12): dos ejes con signo, el tercero recorre
        for (int ex = 0; ex < 3; ex++) {
            int a1 = (ex + 1) % 3, a2 = (ex + 2) % 3;
            for (int s1 = -1; s1 <= 1; s1 += 2)
                for (int s2 = -1; s2 <= 1; s2 += 2) {
                    float[][] q = new float[4][3];
                    // extremo en cara a1
                    for (int k = 0; k < 4; k++) {
                        float run = (k == 0 || k == 3) ? -1 : 1;
                        q[k][ex] = c0[ex] + run * (h[ex] - b);
                        if (k < 2) { q[k][a1] = c0[a1] + s1 * h[a1]; q[k][a2] = c0[a2] + s2 * (h[a2] - b); }
                        else { q[k][a2] = c0[a2] + s2 * h[a2]; q[k][a1] = c0[a1] + s1 * (h[a1] - b); }
                    }
                    float[] hint = new float[3]; hint[a1] = s1; hint[a2] = s2;
                    outQuad(q[0], q[1], q[2], q[3], hint[0], hint[1], hint[2], s);
                }
        }
        // esquinas (8)
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sy = -1; sy <= 1; sy += 2)
                for (int sz = -1; sz <= 1; sz += 2) {
                    float[] sgn = {sx, sy, sz};
                    float[][] q = new float[3][3];
                    for (int k = 0; k < 3; k++)
                        for (int a = 0; a < 3; a++) q[k][a] = c0[a] + sgn[a] * (a == k ? h[a] : h[a] - b);
                    outTri(q[0], q[1], q[2], sx, sy, sz, s);
                }
    }

    /** Caja que cuelga desde el origen hacia -Y (miembros del zombi): arriba w1×d1, abajo w0×d0. */
    public void taper(float len, float w0, float d0, float w1, float d1, float s) {
        for (int k = 0; k < 8; k++) {
            boolean top = (k & 2) != 0;
            float w = top ? w1 : w0, d = top ? d1 : d0;
            bc[k * 3] = ((k & 1) != 0 ? 0.5f : -0.5f) * w;
            bc[k * 3 + 1] = top ? 0 : -len;
            bc[k * 3 + 2] = ((k & 4) != 0 ? 0.5f : -0.5f) * d;
        }
        boxCorners(bc, s);
    }

    private final float[] cp = new float[9], cn = new float[9], ct = new float[9], cu = new float[6];

    private void sv(int i, float x, float y, float z, float nx, float ny, float nz, float tx, float ty, float tz, float u, float v) {
        cp[i * 3] = x; cp[i * 3 + 1] = y; cp[i * 3 + 2] = z;
        cn[i * 3] = nx; cn[i * 3 + 1] = ny; cn[i * 3 + 2] = nz;
        ct[i * 3] = tx; ct[i * 3 + 1] = ty; ct[i * 3 + 2] = tz;
        cu[i * 2] = u; cu[i * 2 + 1] = v;
    }

    /** Cilindro / cono truncado a lo largo de +Y desde y=0 hasta y=h. */
    public void cylinder(float r0, float r1, float h, int segs, boolean capTop, boolean capBottom, float s) {
        float slope = (r0 - r1) / h;
        float nl = (float) Math.sqrt(1 + slope * slope);
        for (int i = 0; i < segs; i++) {
            float a0 = (float) (i * 2 * Math.PI / segs), a1 = (float) ((i + 1) * 2 * Math.PI / segs);
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float circ = (float) (2 * Math.PI * Math.max(r0, 0.01f)) * s;
            float u0 = (float) (segs - i) / segs * circ;
            float u1 = (float) (segs - i - 1) / segs * circ;
            float vt = h * s;
            // dos triángulos de la pared
            sv(0, r0 * c0, 0, r0 * s0, c0 / nl, slope / nl, s0 / nl, s0, 0, -c0, u0, 0);
            sv(1, r0 * c1, 0, r0 * s1, c1 / nl, slope / nl, s1 / nl, s1, 0, -c1, u1, 0);
            sv(2, r1 * c1, h, r1 * s1, c1 / nl, slope / nl, s1 / nl, s1, 0, -c1, u1, vt);
            tri(cp, cn, ct, cu, 0, 1, 2);
            sv(1, r1 * c1, h, r1 * s1, c1 / nl, slope / nl, s1 / nl, s1, 0, -c1, u1, vt);
            sv(2, r1 * c0, h, r1 * s0, c0 / nl, slope / nl, s0 / nl, s0, 0, -c0, u0, vt);
            tri(cp, cn, ct, cu, 0, 1, 2);
            if (capTop && r1 > 0) {
                sv(0, 0, h, 0, 0, 1, 0, 1, 0, 0, 0.5f * s, 0.5f * s);
                sv(1, r1 * c0, h, r1 * s0, 0, 1, 0, 1, 0, 0, (0.5f + 0.5f * c0) * s, (0.5f - 0.5f * s0) * s);
                sv(2, r1 * c1, h, r1 * s1, 0, 1, 0, 1, 0, 0, (0.5f + 0.5f * c1) * s, (0.5f - 0.5f * s1) * s);
                tri(cp, cn, ct, cu, 0, 1, 2);
            }
            if (capBottom && r0 > 0) {
                sv(0, 0, 0, 0, 0, -1, 0, 1, 0, 0, 0.5f * s, 0.5f * s);
                sv(1, r0 * c0, 0, r0 * s0, 0, -1, 0, 1, 0, 0, (0.5f + 0.5f * c0) * s, (0.5f + 0.5f * s0) * s);
                sv(2, r0 * c1, 0, r0 * s1, 0, -1, 0, 1, 0, 0, (0.5f + 0.5f * c1) * s, (0.5f + 0.5f * s1) * s);
                tri(cp, cn, ct, cu, 0, 1, 2);
            }
        }
    }

    /** Esfera suave centrada en el origen. */
    public void sphere(float r, int lon, int lat, float s) {
        for (int j = 0; j < lat; j++) {
            float p0 = (float) (Math.PI * j / lat - Math.PI / 2), p1 = (float) (Math.PI * (j + 1) / lat - Math.PI / 2);
            for (int i = 0; i < lon; i++) {
                float a0 = (float) (2 * Math.PI * i / lon), a1 = (float) (2 * Math.PI * (i + 1) / lon);
                float[][] q = new float[4][];
                float[] pa = {p0, p0, p1, p1}, aa = {a0, a1, a1, a0};
                float[] uu = {(float) i / lon, (float) (i + 1) / lon, (float) (i + 1) / lon, (float) i / lon};
                float[] vv = {(float) j / lat, (float) j / lat, (float) (j + 1) / lat, (float) (j + 1) / lat};
                for (int k = 0; k < 4; k++) {
                    float nx = (float) (Math.cos(pa[k]) * Math.cos(aa[k])), ny = (float) Math.sin(pa[k]), nz = (float) (Math.cos(pa[k]) * Math.sin(aa[k]));
                    q[k] = new float[]{nx, ny, nz};
                }
                for (int tri = 0; tri < 2; tri++) {
                    int[] id = tri == 0 ? new int[]{0, 1, 2} : new int[]{0, 2, 3};
                    for (int k = 0; k < 3; k++) {
                        int w = id[k];
                        float[] nn = q[w];
                        sv(k, nn[0] * r, nn[1] * r, nn[2] * r, nn[0], nn[1], nn[2], -nn[2], 0, nn[0], uu[w] * 2 * r * s * 3, vv[w] * r * s * 3);
                    }
                    tri(cp, cn, ct, cu, 0, 1, 2);
                }
            }
        }
    }

    // ------------------------------------------------------------------ superficies orgánicas
    private final float[] sp0 = new float[9], sn0 = new float[9], st0 = new float[9], su0 = new float[6];

    /**
     * Superficie suave a partir de una rejilla de posiciones [filas i][columnas j][xyz].
     * Si closed, la última columna se une con la primera. Normales por diferencias finitas.
     * u de u0 a u1 a lo largo de j, v de v0 a v1 a lo largo de i. outward: sentido de la normal (+1 / -1).
     */
    public void surface(float[][][] g, boolean closed, float u0, float u1, float v0, float v1, float outward) {
        int rows = g.length, cols = g[0].length;
        int cj = closed ? cols : cols - 1;
        float[][][] n = new float[rows][cols][3], tg = new float[rows][cols][3];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int j0 = closed ? (j - 1 + cols) % cols : Math.max(0, j - 1), j1 = closed ? (j + 1) % cols : Math.min(cols - 1, j + 1);
                int i0 = Math.max(0, i - 1), i1 = Math.min(rows - 1, i + 1);
                float ax = g[i][j1][0] - g[i][j0][0], ay = g[i][j1][1] - g[i][j0][1], az = g[i][j1][2] - g[i][j0][2];
                float bx = g[i1][j][0] - g[i0][j][0], by = g[i1][j][1] - g[i0][j][1], bz = g[i1][j][2] - g[i0][j][2];
                float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
                float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (l < 1e-9f) { nx = 0; ny = 1; nz = 0; l = 1; }
                n[i][j][0] = nx / l * outward; n[i][j][1] = ny / l * outward; n[i][j][2] = nz / l * outward;
                float al = (float) Math.sqrt(ax * ax + ay * ay + az * az) + 1e-9f;
                tg[i][j][0] = ax / al; tg[i][j][1] = ay / al; tg[i][j][2] = az / al;
            }
        }
        for (int i = 0; i < rows - 1; i++) {
            for (int j = 0; j < cj; j++) {
                int jn = (j + 1) % cols;
                int[][] c = {{i, j}, {i, jn}, {i + 1, jn}, {i + 1, j}};
                float[] uu = {u0 + (u1 - u0) * j / cj, u0 + (u1 - u0) * (j + 1) / cj};
                float va = v0 + (v1 - v0) * i / (rows - 1), vb = v0 + (v1 - v0) * (i + 1) / (rows - 1);
                float[] uvs = {uu[0], va, uu[1], va, uu[1], vb, uu[0], vb};
                for (int t = 0; t < 2; t++) {
                    int[] id = t == 0 ? new int[]{0, 1, 2} : new int[]{0, 2, 3};
                    for (int k = 0; k < 3; k++) {
                        int[] q = c[id[k]];
                        sp0[k * 3] = g[q[0]][q[1]][0]; sp0[k * 3 + 1] = g[q[0]][q[1]][1]; sp0[k * 3 + 2] = g[q[0]][q[1]][2];
                        sn0[k * 3] = n[q[0]][q[1]][0]; sn0[k * 3 + 1] = n[q[0]][q[1]][1]; sn0[k * 3 + 2] = n[q[0]][q[1]][2];
                        st0[k * 3] = tg[q[0]][q[1]][0]; st0[k * 3 + 1] = tg[q[0]][q[1]][1]; st0[k * 3 + 2] = tg[q[0]][q[1]][2];
                        su0[k * 2] = uvs[id[k] * 2]; su0[k * 2 + 1] = uvs[id[k] * 2 + 1];
                    }
                    tri(sp0, sn0, st0, su0, 0, 1, 2);
                }
            }
        }
    }

    /**
     * Sólido de revolución a lo largo de +Y con sección elíptica por anillo.
     * ys, rx, rz: un valor por anillo (de abajo a arriba). jag: irregularidad del borde inferior (ropa rasgada).
     * Ángulo 0 = +Z (frente), crece hacia +X.
     */
    public void lathe(float[] ys, float[] rx, float[] rz, int segs, float u0, float u1, float v0, float v1, float jag, int seed, boolean capTop, boolean capBottom) {
        latheEx(ys, rx, rz, segs, u0, u1, v0, v1, jag, 0f, seed, capTop, capBottom);
    }

    public void latheEx(float[] ys, float[] rx, float[] rz, int segs, float u0, float u1, float v0, float v1, float jag, float jagTop, int seed, boolean capTop, boolean capBottom) {
        int rows = ys.length;
        float[][][] g = new float[rows][segs + 1][3];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j <= segs; j++) {
                float a = (float) (2 * Math.PI * (j % segs) / segs + Math.PI);
                float y = ys[i];
                if (i == 0 && jag > 0) y += (Noise.rnd(j % segs, seed, 5) - 0.35f) * jag;
                if (i == 1 && jag > 0) y += (Noise.rnd(j % segs, seed, 6) - 0.5f) * jag * 0.3f;
                if (i == rows - 1 && jagTop > 0) y -= (Noise.rnd(j % segs, seed, 7) - 0.2f) * jagTop;
                if (i == rows - 2 && jagTop > 0) y -= (Noise.rnd(j % segs, seed, 8) - 0.5f) * jagTop * 0.3f;
                g[i][j][0] = (float) Math.sin(a) * rx[i];
                g[i][j][1] = y;
                g[i][j][2] = (float) Math.cos(a) * rz[i];
            }
        }
        surface(g, true, u0, u1, v0, v1, 1f);
        if (capTop) cap(ys[rows - 1], rx[rows - 1], rz[rows - 1], segs, 1f, u0, u1, v0, v1);
        if (capBottom) cap(ys[0], rx[0], rz[0], segs, -1f, u0, u1, v0, v1);
    }

    public void cap(float y, float rx, float rz, int segs, float dir, float u0, float u1, float v0, float v1) {
        for (int j = 0; j < segs; j++) {
            float a0 = (float) (2 * Math.PI * j / segs), a1 = (float) (2 * Math.PI * (j + 1) / segs);
            sv(0, 0, y, 0, 0, dir, 0, 1, 0, 0, (u0 + u1) * 0.5f, (v0 + v1) * 0.5f);
            sv(1, (float) Math.sin(a0) * rx, y, (float) Math.cos(a0) * rz, 0, dir, 0, 1, 0, 0, u0, v0);
            sv(2, (float) Math.sin(a1) * rx, y, (float) Math.cos(a1) * rz, 0, dir, 0, 1, 0, 0, u1, v0);
            tri(cp, cn, ct, cu, 0, 1, 2);
        }
    }

    /** Miembro que cuelga hacia -Y con perfil suave (hinchazón muscular) y sección elíptica. */
    public void limb(float len, float[] prof, float depthRatio, int segs, float u0, float u1, float v0, float v1, float jag, int seed) {
        int n = prof.length;
        // de abajo (y=-len) hacia arriba (y=0)
        float[] ys2 = new float[n], rx2 = new float[n], rz2 = new float[n];
        for (int i = 0; i < n; i++) { ys2[i] = -len + len * i / (n - 1); rx2[i] = prof[i] * 0.5f; rz2[i] = prof[i] * 0.5f * depthRatio; }
        lathe(ys2, rx2, rz2, segs, u0, u1, v0, v1, jag, seed, true, true);
    }

    private final float[] fp = new float[9], fn = new float[9], ft = new float[9], fu = new float[6];

    /** Triángulo con sombreado plano; uv por proyección planar. */
    public void flatTri(float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float s, float outx, float outy, float outz) {
        float ex = x1 - x0, ey = y1 - y0, ez = z1 - z0, fx = x2 - x0, fy = y2 - y0, fz = z2 - z0;
        float nx = ey * fz - ez * fy, ny = ez * fx - ex * fz, nz = ex * fy - ey * fx;
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (l < 1e-9f) return;
        nx /= l; ny /= l; nz /= l;
        if (nx * outx + ny * outy + nz * outz < 0) { nx = -nx; ny = -ny; nz = -nz; }
        float tl = (float) Math.sqrt(ex * ex + ey * ey + ez * ez) + 1e-9f;
        float[] P = {x0, y0, z0, x1, y1, z1, x2, y2, z2};
        for (int k = 0; k < 3; k++) {
            fp[k * 3] = P[k * 3]; fp[k * 3 + 1] = P[k * 3 + 1]; fp[k * 3 + 2] = P[k * 3 + 2];
            fn[k * 3] = nx; fn[k * 3 + 1] = ny; fn[k * 3 + 2] = nz;
            ft[k * 3] = ex / tl; ft[k * 3 + 1] = ey / tl; ft[k * 3 + 2] = ez / tl;
            float ax = Math.abs(nx), ay = Math.abs(ny), az = Math.abs(nz);
            float u, v;
            if (ay >= ax && ay >= az) { u = P[k * 3] * s; v = P[k * 3 + 2] * s; }
            else if (ax >= az) { u = P[k * 3 + 2] * s; v = P[k * 3 + 1] * s; }
            else { u = P[k * 3] * s; v = P[k * 3 + 1] * s; }
            fu[k * 2] = u; fu[k * 2 + 1] = v;
        }
        tri(fp, fn, ft, fu, 0, 1, 2);
    }

    /** Roca irregular con sombreado plano (para escombros y piedras). */
    public void rock(float rx, float ry, float rz, int seed, float s) {
        int lon = 7, lat = 4;
        float[][][] g = new float[lat + 1][lon][3];
        for (int j = 0; j <= lat; j++) {
            float p = (float) (Math.PI * j / lat - Math.PI / 2);
            for (int i = 0; i < lon; i++) {
                float a = (float) (2 * Math.PI * i / lon);
                float k = 0.72f + 0.55f * Noise.rnd(i, j, seed);
                if (j == 0 || j == lat) k = 0.8f;
                g[j][i][0] = (float) (Math.cos(p) * Math.cos(a)) * rx * k;
                g[j][i][1] = (float) Math.sin(p) * ry * k;
                g[j][i][2] = (float) (Math.cos(p) * Math.sin(a)) * rz * k;
            }
        }
        for (int j = 0; j < lat; j++) {
            for (int i = 0; i < lon; i++) {
                float[] a = g[j][i], b = g[j][(i + 1) % lon], c = g[j + 1][(i + 1) % lon], d = g[j + 1][i];
                float mx = (a[0] + b[0] + c[0] + d[0]) / 4, my = (a[1] + b[1] + c[1] + d[1]) / 4, mz = (a[2] + b[2] + c[2] + d[2]) / 4;
                flatTri(a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2], s, mx, my, mz);
                flatTri(a[0], a[1], a[2], c[0], c[1], c[2], d[0], d[1], d[2], s, mx, my, mz);
            }
        }
    }

    /** Copia vértices ya existentes transformados por una matriz (para posar zombis). */
    public static int transformInto(float[] src, int srcCount, float[] mat, float tr, float tg, float tb,
                                    float[] dst, int dstVert) {
        float[] o = new float[3];
        int di = dstVert * STRIDE;
        for (int v = 0; v < srcCount; v++) {
            int si = v * STRIDE;
            Mat4.point(mat, src[si], src[si + 1], src[si + 2], o);
            dst[di] = o[0]; dst[di + 1] = o[1]; dst[di + 2] = o[2];
            Mat4.dir(mat, src[si + 3], src[si + 4], src[si + 5], o);
            dst[di + 3] = o[0]; dst[di + 4] = o[1]; dst[di + 5] = o[2];
            Mat4.dir(mat, src[si + 6], src[si + 7], src[si + 8], o);
            dst[di + 6] = o[0]; dst[di + 7] = o[1]; dst[di + 8] = o[2];
            dst[di + 9] = src[si + 9]; dst[di + 10] = src[si + 10];
            dst[di + 11] = src[si + 11] * tr; dst[di + 12] = src[si + 12] * tg; dst[di + 13] = src[si + 13] * tb;
            dst[di + 14] = src[si + 14];
            di += STRIDE;
        }
        return dstVert + srcCount;
    }
}
