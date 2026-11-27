package com.craterz;

import java.util.ArrayList;

/** Mapa "El Cráter": terreno, ruinas, trincheras, iglesia, cementerio, puertas y objetos comprables. Todo original. */
public final class World {
    public static final int CH = 32, NC = 6;
    public static final float ORG = -96f;
    public static final int M_MUD = 0, M_BRICK = 1, M_WOOD = 2, M_METAL = 3, M_CONC = 4, M_SAND = 5, M_BARK = 6, NMAT = 7;
    public static final int B_DOOR = 0, B_WALL = 1, B_BOX = 2, B_PERK = 3;
    public static final int PERK_VIGOR = 0, PERK_RAFAGA = 1, PERK_REFLEJOS = 2, PERK_PIES = 3;
    public static final String[] PERK_NAME = {"Vigor", "Ráfaga", "Reflejos", "Pies ligeros"};
    public static final int[] PERK_COST = {2500, 2000, 1500, 2000};
    public static final float[][] PERK_COLOR = {{1f, 0.22f, 0.2f}, {1f, 0.72f, 0.12f}, {0.25f, 0.62f, 1f}, {0.3f, 1f, 0.42f}};
    public static final float GAP = 4f;

    public static final class Collider {
        public boolean circle, active = true;
        public float x1, z1, x2, z2, r, top;
    }

    public static final class Buyable {
        public int type, id, cost;
        public float x, z, r;
        public String name;
    }

    public static final class Fire { public float x, y, z, size; }

    public static final class Light {
        public float x, y, z, r, g, b, intensity, range, phase;
        public int flicker; // 0 fijo, 1 fuego, 2 pulso
    }

    public static final class Door {
        public int id, cost, zone;
        public float x, z, nx, nz;
        public float[] mesh;
        public int verts;
        public boolean open;
        public Collider col;
    }

    public final MeshBuilder[][] chunks = new MeshBuilder[NC * NC][NMAT];
    public final ArrayList<Collider> cols = new ArrayList<Collider>();
    public final ArrayList<Buyable> buyables = new ArrayList<Buyable>();
    public final ArrayList<Fire> fires = new ArrayList<Fire>();
    public final ArrayList<Light> lights = new ArrayList<Light>();
    public final ArrayList<Door> doors = new ArrayList<Door>();
    public final float[][][] zoneSpawns = new float[5][][];
    public float startX = 0, startZ = 52;
    public float boxX = -19, boxZ = -6;

    private final ArrayList<float[]> trenches = new ArrayList<float[]>();
    private float[][] holes;
    private int rs = 12345;

    public World() {
        initTerrainData();
        buildTerrain();
        buildBoundary();
        buildWalls();
        buildCamp();
        buildCrater();
        buildChurch();
        buildWest();
        buildEast();
        scatterTrees();
        buildSpawns();
    }

    // ------------------------------------------------------------ utilidades
    private float rnd() {
        rs = rs * 1103515245 + 12345;
        return ((rs >>> 8) & 0xFFFF) / 65536f;
    }

    private float rr(float a, float b) { return a + (b - a) * rnd(); }

    private static float sinD(float d) { return (float) Math.sin(Math.toRadians(d)); }
    private static float cosD(float d) { return (float) Math.cos(Math.toRadians(d)); }

    public static int zoneOf(float x, float z) {
        if (z > 28) return 0;
        if (z < -30) return 2;
        if (x < -30) return 3;
        if (x > 30) return 4;
        return 1;
    }

    private MeshBuilder mb(int mat, float x, float z) {
        int cx = (int) Math.floor((x - ORG) / CH), cz = (int) Math.floor((z - ORG) / CH);
        if (cx < 0) cx = 0; if (cx >= NC) cx = NC - 1;
        if (cz < 0) cz = 0; if (cz >= NC) cz = NC - 1;
        MeshBuilder b = chunks[cz * NC + cx][mat];
        if (b == null) { b = new MeshBuilder(); chunks[cz * NC + cx][mat] = b; }
        b.identity(); b.uvTiled(); b.color(1, 1, 1, 0); b.uOff = 0; b.vOff = 0;
        return b;
    }

    // --------------------------------------------------------------- terreno
    private void initTerrainData() {
        trenches.add(new float[]{-44, 38, -26, 42, -8, 38, 10, 43, 30, 39, 48, 44});
        trenches.add(new float[]{-24, -20, -12, -24, 2, -22});
        trenches.add(new float[]{22, 14, 15, 21, 8, 23});
        trenches.add(new float[]{-40, -18, -48, -8, -44, 8, -50, 20});
        trenches.add(new float[]{40, 18, 50, 8, 44, -8, 52, -20});
        trenches.add(new float[]{-52, -38, -36, -34, -20, -36});
        int n = 24;
        holes = new float[n][4];
        int k = 0;
        int guard = 0;
        while (k < n && guard++ < 500) {
            float x = rr(-62, 62), z = rr(-64, 62);
            if (nearGap(x, z, 12)) continue;
            if (Math.abs(x) < 16 && Math.abs(z) < 16) continue;
            if (Math.abs(x) < 15 && z < -34 && z > -66) continue;
            if (Math.abs(z - 28) < 5 || Math.abs(z + 30) < 5) continue;
            if ((Math.abs(x + 30) < 5 || Math.abs(x - 30) < 5) && z > -30 && z < 28) continue;
            if (Math.hypot(x - startX, z - startZ) < 9) continue;
            holes[k][0] = x; holes[k][1] = z; holes[k][2] = rr(2.0f, 3.6f); holes[k][3] = rr(0.7f, 1.4f);
            k++;
        }
        if (k < n) { float[][] h2 = new float[k][]; System.arraycopy(holes, 0, h2, 0, k); holes = h2; }
    }

    private static boolean nearGap(float x, float z, float d) {
        return Math.hypot(x, z - 28) < d || Math.hypot(x, z + 30) < d || Math.hypot(x + 30, z) < d || Math.hypot(x - 30, z) < d;
    }

    private static float distSeg(float px, float pz, float ax, float az, float bx, float bz) {
        float dx = bx - ax, dz = bz - az;
        float l2 = dx * dx + dz * dz;
        float t = l2 < 1e-6f ? 0 : ((px - ax) * dx + (pz - az) * dz) / l2;
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        float qx = ax + dx * t - px, qz = az + dz * t - pz;
        return (float) Math.sqrt(qx * qx + qz * qz);
    }

    public float ground(float x, float z) {
        float h = (Noise.fbm(x * 0.045f + 50, z * 0.045f + 50, 3, 1) - 0.5f) * 1.3f + (Noise.fbm(x * 0.2f, z * 0.2f, 2, 2) - 0.5f) * 0.28f;
        float m = Math.max(Math.abs(x), Math.abs(z));
        h += Noise.smoothstep(58, 92, m) * (5.5f + 3f * Noise.fbm(x * 0.05f, z * 0.05f, 2, 3));
        float d = (float) Math.sqrt(x * x + z * z);
        h -= 3.3f * (float) Math.exp(-(d / 10.5f) * (d / 10.5f));
        h += 0.85f * (float) Math.exp(-((d - 14.5f) / 3f) * ((d - 14.5f) / 3f));
        float tr = 0;
        for (int t = 0; t < trenches.size(); t++) {
            float[] p = trenches.get(t);
            float best = 99;
            for (int i = 0; i + 3 < p.length; i += 2) best = Math.min(best, distSeg(x, z, p[i], p[i + 1], p[i + 2], p[i + 3]));
            float w = 1.9f;
            tr -= 1.5f * Noise.smoothstep(w, w * 0.45f, best);
            float lip = (best - w * 1.25f) / 0.6f;
            tr += 0.38f * (float) Math.exp(-lip * lip);
        }
        h += tr;
        for (int i = 0; i < holes.length; i++) {
            float dx = x - holes[i][0], dz = z - holes[i][1];
            float q = (dx * dx + dz * dz) / (holes[i][2] * holes[i][2]);
            if (q < 9) h -= holes[i][3] * (float) Math.exp(-q);
        }
        float flat = 0;
        float[][] g = {{0, 28}, {0, -30}, {-30, 0}, {30, 0}};
        for (int i = 0; i < 4; i++) flat = Math.max(flat, Noise.smoothstep(9, 4, (float) Math.hypot(x - g[i][0], z - g[i][1])));
        float cx = Math.max(Math.abs(x) - 14f, Math.abs(z + 51) - 15f);
        flat = Math.max(flat, Noise.smoothstep(4f, 0f, cx));
        float sx = (float) Math.hypot(x - startX, z - startZ);
        flat = Math.max(flat, 0.6f * Noise.smoothstep(7, 2, sx));
        return h + (0.0f - h) * flat;
    }

    private void buildTerrain() {
        final float step = 2f;
        final int span = (int) (CH * NC / step);            // celdas por lado
        final int n = span + 3;                              // con margen de 1 a cada lado
        float[][] hg = new float[n][n];
        float[][] wg = new float[n][n];
        float[][] vg = new float[n][n];
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                float x = ORG + (i - 1) * step, z = ORG + (j - 1) * step;
                float h = ground(x, z);
                hg[j][i] = h;
                float v = 0.62f + 0.6f * Noise.fbm(x * 0.07f, z * 0.07f, 3, 8);
                float burnt = Noise.smoothstep(18, 6, (float) Math.hypot(x, z)) * 0.5f;
                float low = Noise.clamp(-h / 1.6f, 0f, 1f);
                float wetn = Noise.smoothstep(0.55f, 0.75f, Noise.fbm(x * 0.09f + 7, z * 0.09f + 3, 3, 9));
                wg[j][i] = Noise.clamp(low * 0.9f + wetn * 0.7f, 0f, 1f);
                vg[j][i] = v * (1f - burnt * 0.5f);
            }
        }
        int cells = (int) (CH / step);
        float[] p = new float[9], nn = new float[9], t = new float[9], uv = new float[6];
        for (int cz = 0; cz < NC; cz++) {
            for (int cx = 0; cx < NC; cx++) {
                MeshBuilder b = chunks[cz * NC + cx][M_MUD] = new MeshBuilder();
                b.pvc = new float[9]; b.pvw = new float[3];
                for (int j = 0; j < cells; j++) {
                    for (int i = 0; i < cells; i++) {
                        int gi = cx * cells + i + 1, gj = cz * cells + j + 1;
                        int[][] a = {{0, 0}, {1, 0}, {1, 1}, {0, 0}, {1, 1}, {0, 1}};
                        for (int tri = 0; tri < 2; tri++) {
                            for (int k = 0; k < 3; k++) {
                                int ii = gi + a[tri * 3 + k][0], jj = gj + a[tri * 3 + k][1];
                                float x = ORG + (ii - 1) * step, z = ORG + (jj - 1) * step;
                                float dhx = (hg[jj][ii + 1] - hg[jj][ii - 1]) / (2 * step);
                                float dhz = (hg[jj + 1][ii] - hg[jj - 1][ii]) / (2 * step);
                                float nl = (float) Math.sqrt(dhx * dhx + 1 + dhz * dhz);
                                p[k * 3] = x; p[k * 3 + 1] = hg[jj][ii]; p[k * 3 + 2] = z;
                                nn[k * 3] = -dhx / nl; nn[k * 3 + 1] = 1f / nl; nn[k * 3 + 2] = -dhz / nl;
                                float tl = (float) Math.sqrt(1 + dhx * dhx);
                                t[k * 3] = 1f / tl; t[k * 3 + 1] = dhx / tl; t[k * 3 + 2] = 0;
                                uv[k * 2] = x * 0.22f; uv[k * 2 + 1] = -z * 0.22f;
                                float w = wg[jj][ii], v = vg[jj][ii];
                                b.pvc[k * 3] = v * (1f - w * 0.25f); b.pvc[k * 3 + 1] = v * (1f - w * 0.22f); b.pvc[k * 3 + 2] = v * (1f - w * 0.15f);
                                b.pvw[k] = w;
                            }
                            b.tri(p, nn, t, uv, 0, 1, 2);
                        }
                    }
                }
                b.pvc = null; b.pvw = null;
            }
        }
    }

    // ------------------------------------------------------------- colisiones
    private Collider addBox(float x1, float z1, float x2, float z2, float top) {
        Collider c = new Collider();
        c.x1 = Math.min(x1, x2); c.x2 = Math.max(x1, x2); c.z1 = Math.min(z1, z2); c.z2 = Math.max(z1, z2); c.top = top;
        cols.add(c);
        return c;
    }

    private Collider addCircle(float x, float z, float r, float top) {
        Collider c = new Collider();
        c.circle = true; c.x1 = x; c.z1 = z; c.r = r; c.top = top;
        cols.add(c);
        return c;
    }

    /** Pared centrada en (x,z), con "len" a lo largo del eje local x girado yaw grados. */
    private void lineCollider(float x, float z, float yaw, float len, float thick, float top) {
        int a = ((Math.round(yaw) % 180) + 180) % 180;
        if (a == 0) addBox(x - len / 2, z - thick / 2, x + len / 2, z + thick / 2, top);
        else if (a == 90) addBox(x - thick / 2, z - len / 2, x + thick / 2, z + len / 2, top);
        else {
            int n = Math.max(1, (int) (len / thick));
            for (int i = 0; i < n; i++) {
                float lx = -len / 2 + (i + 0.5f) * len / n;
                addCircle(x + lx * cosD(yaw), z - lx * sinD(yaw), thick * 0.62f, top);
            }
        }
    }

    /** Empuja (pos[0],pos[1]) fuera de los obstáculos. Devuelve true si hubo contacto. */
    public boolean resolve(float[] pos, float rad) {
        boolean hit = false;
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < cols.size(); i++) {
                Collider c = cols.get(i);
                if (!c.active) continue;
                float px = pos[0], pz = pos[1];
                if (c.circle) {
                    float dx = px - c.x1, dz = pz - c.z1, rr2 = c.r + rad;
                    float d2 = dx * dx + dz * dz;
                    if (d2 < rr2 * rr2) {
                        float d = (float) Math.sqrt(d2);
                        if (d < 1e-4f) { dx = 1; dz = 0; d = 1; }
                        pos[0] = c.x1 + dx / d * rr2; pos[1] = c.z1 + dz / d * rr2;
                        hit = true;
                    }
                } else {
                    if (px < c.x1 - rad || px > c.x2 + rad || pz < c.z1 - rad || pz > c.z2 + rad) continue;
                    float qx = Noise.clamp(px, c.x1, c.x2), qz = Noise.clamp(pz, c.z1, c.z2);
                    float dx = px - qx, dz = pz - qz;
                    float d2 = dx * dx + dz * dz;
                    if (d2 < rad * rad) {
                        if (d2 > 1e-8f) {
                            float d = (float) Math.sqrt(d2);
                            pos[0] = qx + dx / d * rad; pos[1] = qz + dz / d * rad;
                        } else {
                            float l = px - c.x1, r = c.x2 - px, t = pz - c.z1, bt = c.z2 - pz;
                            float m = Math.min(Math.min(l, r), Math.min(t, bt));
                            if (m == l) pos[0] = c.x1 - rad; else if (m == r) pos[0] = c.x2 + rad;
                            else if (m == t) pos[1] = c.z1 - rad; else pos[1] = c.z2 + rad;
                        }
                        hit = true;
                    }
                }
            }
        }
        return hit;
    }

    /** Distancia t (0..1 de la longitud) al primer obstáculo cuya altura supera al rayo; 2 si no choca. */
    public float rayBlock(float ox, float oy, float oz, float dx, float dy, float dz, float maxLen) {
        float best = 2f;
        float ex = dx * maxLen, ez = dz * maxLen;
        for (int i = 0; i < cols.size(); i++) {
            Collider c = cols.get(i);
            if (!c.active) continue;
            float t = -1;
            if (c.circle) {
                float fx = ox - c.x1, fz = oz - c.z1;
                float a = ex * ex + ez * ez, b = 2 * (fx * ex + fz * ez), cc = fx * fx + fz * fz - c.r * c.r;
                if (a < 1e-9f) continue;
                float disc = b * b - 4 * a * cc;
                if (disc < 0) continue;
                float sq = (float) Math.sqrt(disc);
                t = (-b - sq) / (2 * a);
                if (t < 0) t = (-b + sq) / (2 * a) < 0 ? -1 : 0;
            } else {
                float tmin = 0, tmax = 1;
                if (Math.abs(ex) < 1e-9f) { if (ox < c.x1 || ox > c.x2) continue; }
                else {
                    float t1 = (c.x1 - ox) / ex, t2 = (c.x2 - ox) / ex;
                    tmin = Math.max(tmin, Math.min(t1, t2)); tmax = Math.min(tmax, Math.max(t1, t2));
                }
                if (Math.abs(ez) < 1e-9f) { if (oz < c.z1 || oz > c.z2) continue; }
                else {
                    float t1 = (c.z1 - oz) / ez, t2 = (c.z2 - oz) / ez;
                    tmin = Math.max(tmin, Math.min(t1, t2)); tmax = Math.min(tmax, Math.max(t1, t2));
                }
                if (tmin > tmax) continue;
                t = tmin;
            }
            if (t < 0 || t > 1 || t >= best) continue;
            float y = oy + dy * maxLen * t;
            if (y < c.top) best = t;
        }
        return best;
    }

    // ----------------------------------------------------- bloques de dibujo
    private void rubble(float x, float z, float spread, int n, int mat) {
        for (int i = 0; i < n; i++) {
            float rx = x + rr(-spread, spread), rz = z + rr(-spread, spread);
            MeshBuilder b = mb(rnd() < 0.5f ? M_CONC : M_BRICK, rx, rz);
            float s = rr(0.25f, 0.7f);
            b.translate(rx, ground(rx, rz) + s * 0.15f, rz);
            b.rotY(rr(0, 360));
            float sh = rnd();
            b.color(0.8f + 0.3f * sh, 0.8f + 0.3f * sh, 0.8f + 0.3f * sh);
            b.rock(s, s * 0.6f, s * 0.85f, (int) (rnd() * 1000), 0.5f);
        }
    }

    private void brokenWall(float x1, float z1, float x2, float z2, float thick, float hMax, float y0, int mat, float uvs) {
        boolean alongX = Math.abs(z2 - z1) < 0.01f;
        float len = alongX ? Math.abs(x2 - x1) : Math.abs(z2 - z1);
        int n = Math.max(1, (int) Math.ceil(len / 1.6f));
        float seg = len / n;
        for (int i = 0; i < n; i++) {
            float a = (alongX ? Math.min(x1, x2) : Math.min(z1, z2)) + i * seg;
            float cxm = alongX ? a + seg / 2 : x1, czm = alongX ? z1 : a + seg / 2;
            float h = hMax * (0.5f + 0.5f * rnd());
            if (rnd() < 0.08f) h = hMax * 0.25f;
            float top = ground(cxm, czm) + h;
            float yb = y0;
            MeshBuilder b = mb(mat, cxm, czm);
            b.uOff = rnd(); b.vOff = rnd();
            float k = 0.8f + 0.3f * rnd();
            b.color(k, k, k);
            b.box(cxm, (yb + top) / 2, czm, alongX ? seg / 2 : thick / 2, (top - yb) / 2, alongX ? thick / 2 : seg / 2, uvs);
            if (rnd() < 0.3f) rubble(cxm, czm + (alongX ? thick : 0) * 0.9f, 1.2f, 2, mat);
        }
    }

    private void emitFire(float x, float y, float z, float size) {
        Fire f = new Fire(); f.x = x; f.y = y; f.z = z; f.size = size; fires.add(f);
        Light l = new Light(); l.x = x; l.y = y + 0.8f; l.z = z; l.r = 1f; l.g = 0.52f; l.b = 0.18f;
        l.intensity = 2.6f * size; l.range = 13f * size; l.flicker = 1; l.phase = rnd() * 10; lights.add(l);
    }

    private void light(float x, float y, float z, float r, float g, float b, float in, float range, int fl) {
        Light l = new Light(); l.x = x; l.y = y; l.z = z; l.r = r; l.g = g; l.b = b; l.intensity = in; l.range = range; l.flicker = fl; l.phase = rnd() * 10;
        lights.add(l);
    }

    // ----------------------------------------------------------------- props
    private void sandbagLine(float x, float z, float yaw, float len, int courses) {
        int n = Math.max(1, (int) (len / 0.92f));
        for (int c = 0; c < courses; c++) {
            for (int i = 0; i < n; i++) {
                float off = (c % 2) * 0.46f;
                float lx = -len / 2 + (i + 0.5f) * (len / n) + off - (c % 2 == 1 ? 0.23f : 0);
                float wx = x + lx * cosD(yaw), wz = z - lx * sinD(yaw);
                MeshBuilder b = mb(M_SAND, wx, wz);
                b.uOff = rnd(); b.vOff = rnd();
                float k = 0.75f + 0.35f * rnd();
                b.color(k, k * 0.98f, k * 0.92f);
                b.translate(wx, ground(wx, wz) + 0.17f + c * 0.36f, wz);
                b.rotY(yaw + rr(-5, 5));
                b.box(0, 0, 0, 0.47f, 0.18f, 0.26f, 0.28f);
            }
        }
        lineCollider(x, z, yaw, len, 0.6f, courses * 0.36f + 0.2f);
    }

    private void crate(float x, float z, float yaw, float s) {
        MeshBuilder b = mb(M_WOOD, x, z);
        b.uOff = rnd(); b.vOff = rnd();
        float k = 0.7f + 0.4f * rnd();
        b.color(k, k, k);
        float y = ground(x, z);
        b.translate(x, y + s / 2 - 0.05f, z);
        b.rotY(yaw);
        b.box(0, 0, 0, s / 2, s / 2, s / 2, 0.55f);
        b.color(0.35f, 0.3f, 0.28f);
        b.box(0, 0, 0, s / 2 + 0.02f, s * 0.04f, s / 2 + 0.02f, 0.55f);
        b.box(0, s * 0.3f, 0, s / 2 + 0.02f, s * 0.04f, s / 2 + 0.02f, 0.55f);
        b.box(0, -s * 0.3f, 0, s / 2 + 0.02f, s * 0.04f, s / 2 + 0.02f, 0.55f);
        addCircle(x, z, s * 0.72f, y + s);
    }

    private void barrel(float x, float z, boolean fire) {
        MeshBuilder b = mb(M_METAL, x, z);
        b.uOff = rnd(); b.vOff = rnd();
        float k = 0.7f + 0.5f * rnd();
        b.color(k * 1.05f, k * 0.85f, k * 0.7f);
        float y = ground(x, z);
        b.translate(x, y, z);
        b.cylinder(0.34f, 0.34f, 0.95f, 10, !fire, true, 0.7f);
        b.color(0.5f, 0.35f, 0.25f);
        b.translate(0, 0.2f, 0); b.cylinder(0.355f, 0.355f, 0.07f, 10, false, false, 0.7f);
        b.translate(0, 0.5f, 0); b.cylinder(0.355f, 0.355f, 0.07f, 10, false, false, 0.7f);
        if (fire) emitFire(x, y + 1.0f, z, 1f);
        addCircle(x, z, 0.4f, y + 0.95f);
    }

    private void deadTree(float x, float z, float h) {
        MeshBuilder b = mb(M_BARK, x, z);
        b.uOff = rnd(); b.vOff = rnd();
        float y = ground(x, z);
        b.translate(x, y - 0.2f, z);
        b.rotY(rr(0, 360));
        b.rotZ(rr(-4, 4));
        float k = 0.8f + 0.4f * rnd();
        b.color(k, k, k);
        b.cylinder(0.3f, 0.16f, h * 0.6f, 6, false, false, 0.7f);
        branch(b, h * 0.6f, 0.16f, h * 0.45f, 2);
        addCircle(x, z, 0.38f, y + h);
    }

    private void branch(MeshBuilder b, float yOff, float r, float len, int depth) {
        int count = depth == 2 ? 3 : 2;
        for (int i = 0; i < count; i++) {
            b.push();
            b.translate(0, yOff, 0);
            b.rotY(i * (360f / count) + rr(-30, 30));
            b.rotZ(rr(28, 62));
            b.cylinder(r * 0.85f, r * 0.35f, len, 5, false, false, 0.7f);
            if (depth > 1) branch(b, len * 0.7f, r * 0.45f, len * 0.6f, depth - 1);
            b.pop();
        }
    }

    private void gravestone(float x, float z, float yaw, int type) {
        MeshBuilder b = mb(type == 2 ? M_WOOD : M_CONC, x, z);
        b.uOff = rnd(); b.vOff = rnd();
        float k = 0.7f + 0.4f * rnd();
        b.color(k, k, k);
        float y = ground(x, z);
        b.translate(x, y, z);
        b.rotY(yaw);
        b.rotZ(rr(-7, 7)); b.rotX(rr(-6, 6));
        if (type == 0) {
            b.box(0, 0.5f, 0, 0.38f, 0.5f, 0.09f, 0.6f);
            b.box(0, 1.0f, 0, 0.3f, 0.07f, 0.08f, 0.6f);
        } else if (type == 1) {
            b.box(0, 0.45f, 0, 0.14f, 0.45f, 0.1f, 0.6f);
            b.box(0, 0.7f, 0, 0.36f, 0.1f, 0.1f, 0.6f);
        } else {
            b.box(0, 0.6f, 0, 0.06f, 0.6f, 0.05f, 0.6f);
            b.box(0, 0.85f, 0, 0.3f, 0.05f, 0.05f, 0.6f);
        }
        addCircle(x, z, 0.4f, y + 1.0f);
    }

    private void barbedWire(float x1, float z1, float x2, float z2, boolean solid) {
        float dx = x2 - x1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dz, dx));
        int posts = Math.max(2, (int) (len / 3f) + 1);
        float cxm = (x1 + x2) / 2, czm = (z1 + z2) / 2;
        for (int i = 0; i < posts; i++) {
            float t = i / (float) (posts - 1);
            float px = x1 + dx * t, pz = z1 + dz * t;
            MeshBuilder b = mb(M_WOOD, px, pz);
            b.color(0.7f, 0.65f, 0.6f);
            float y = ground(px, pz);
            b.translate(px, y + 0.7f, pz); b.rotY(rr(0, 360)); b.rotZ(rr(-8, 8));
            b.box(0, 0, 0, 0.06f, 0.75f, 0.06f, 0.5f);
        }
        for (int s = 0; s < 3; s++) {
            float hgt = 0.35f + s * 0.4f;
            int segs = Math.max(2, (int) (len / 0.9f));
            for (int i = 0; i < segs; i++) {
                float t0 = i / (float) segs, t1 = (i + 1) / (float) segs;
                float ax = x1 + dx * t0, az = z1 + dz * t0, bx = x1 + dx * t1, bz = z1 + dz * t1;
                float ay = ground(ax, az) + hgt - 0.05f * (float) Math.sin(t0 * 3.14159f * posts), by = ground(bx, bz) + hgt - 0.05f * (float) Math.sin(t1 * 3.14159f * posts);
                MeshBuilder b = mb(M_METAL, (ax + bx) / 2, (az + bz) / 2);
                b.color(0.5f, 0.42f, 0.38f);
                float ex = bx - ax, ey = by - ay, ez = bz - az;
                float el = (float) Math.sqrt(ex * ex + ey * ey + ez * ez);
                b.translate((ax + bx) / 2, (ay + by) / 2, (az + bz) / 2);
                b.rotY((float) Math.toDegrees(Math.atan2(-ez, ex)));
                b.rotZ((float) Math.toDegrees(Math.asin(ey / el)));
                b.box(0, 0, 0, el / 2, 0.012f, 0.012f, 3f);
                b.box(0, 0, 0, 0.012f, 0.05f, 0.012f, 3f);
                b.box(0.2f, 0, 0, 0.012f, 0.012f, 0.05f, 3f);
            }
        }
        if (solid) lineCollider(cxm, czm, yaw, len, 0.35f, 1.4f);
    }

    private void lampPost(float x, float z, float r, float g, float bl) {
        MeshBuilder b = mb(M_WOOD, x, z);
        float y = ground(x, z);
        b.color(0.8f, 0.75f, 0.7f);
        b.translate(x, y, z);
        b.cylinder(0.08f, 0.07f, 3.0f, 6, true, true, 0.7f);
        b.color(1.0f + r * 1.3f, 1.0f + g * 1.0f, 1.0f + bl * 0.6f);
        b.box(0, 3.15f, 0, 0.14f, 0.16f, 0.14f, 0.4f);
        b.color(0.25f, 0.22f, 0.2f);
        b.box(0, 3.35f, 0, 0.2f, 0.04f, 0.2f, 0.4f);
        light(x, y + 3.1f, z, r, g, bl, 2.2f, 11f, 1);
        addCircle(x, z, 0.14f, y + 3);
    }

    private void tent(float x, float z, float yaw) {
        MeshBuilder b = mb(M_SAND, x, z);
        float y = ground(x, z);
        b.color(0.55f, 0.6f, 0.45f);
        b.translate(x, y, z); b.rotY(yaw);
        float w = 2.2f, l = 3.4f, h = 1.9f;
        b.quad(-w, 0, l, 0, h, l, 0, h, -l, -w, 0, -l, 0.35f);
        b.quad(0, h, l, w, 0, l, w, 0, -l, 0, h, -l, 0.35f);
        b.quad(-w, 0, -l, 0, h, -l, w, 0, -l, w, 0, -l, 0.35f);
        b.quad(-w, 0, l, w, 0, l, 0, h, l, 0, h, l, 0.35f);
        b.color(0.35f, 0.3f, 0.25f);
        b.box(0, 0.1f, 0, w * 0.95f, 0.05f, l * 0.95f, 0.5f);
        lineCollider(x, z, yaw + 90, l * 2 - 0.5f, w * 1.2f, 1.6f);
    }

    private void wagon(float x, float z, float yaw) {
        MeshBuilder b = mb(M_WOOD, x, z);
        float y = ground(x, z);
        b.color(0.8f, 0.75f, 0.7f);
        b.translate(x, y + 0.7f, z); b.rotY(yaw); b.rotZ(rr(-6, 6));
        b.box(0, 0.2f, 0, 1.9f, 0.1f, 0.9f, 0.5f);
        b.box(0, 0.65f, 0.85f, 1.9f, 0.4f, 0.06f, 0.5f);
        b.box(0, 0.65f, -0.85f, 1.9f, 0.4f, 0.06f, 0.5f);
        b.box(-1.85f, 0.65f, 0, 0.06f, 0.4f, 0.85f, 0.5f);
        for (int s = -1; s <= 1; s += 2) {
            b.push();
            b.translate(s * 1.1f, -0.1f, 1.0f); b.rotX(90);
            b.color(0.55f, 0.5f, 0.45f);
            b.cylinder(0.62f, 0.62f, 0.12f, 10, true, true, 0.5f);
            b.pop();
            b.push();
            b.translate(s * 1.1f, -0.1f, -1.12f); b.rotX(90);
            b.cylinder(0.62f, 0.62f, 0.12f, 10, true, true, 0.5f);
            b.pop();
        }
        lineCollider(x, z, yaw, 3.8f, 1.9f, y + 1.4f);
    }

    private void cannon(float x, float z, float yaw) {
        MeshBuilder b = mb(M_METAL, x, z);
        float y = ground(x, z);
        b.color(0.55f, 0.5f, 0.48f);
        b.translate(x, y + 0.7f, z); b.rotY(yaw);
        b.push(); b.rotZ(-90); b.rotZ(18); b.translate(0, -1.4f, 0);
        b.cylinder(0.32f, 0.22f, 3.0f, 10, true, true, 0.6f); b.pop();
        MeshBuilder w = mb(M_WOOD, x, z);
        w.color(0.8f, 0.75f, 0.7f);
        w.translate(x, y + 0.7f, z); w.rotY(yaw);
        w.box(0, -0.1f, 0, 1.1f, 0.12f, 0.35f, 0.5f);
        for (int s = -1; s <= 1; s += 2) {
            w.push(); w.translate(0, -0.1f, s * 0.55f); w.rotX(90); w.translate(0, -0.06f, 0);
            w.cylinder(0.7f, 0.7f, 0.12f, 10, true, true, 0.5f); w.pop();
        }
        addCircle(x, z, 1.2f, y + 1.4f);
    }

    private void tank(float x, float z) {
        MeshBuilder b = mb(M_METAL, x, z);
        float y = ground(x, z);
        b.uOff = 0.3f;
        b.color(0.75f, 0.68f, 0.62f);
        b.translate(x, y + 1.3f, z); b.rotZ(4);
        b.box(0, 0, 0, 4.2f, 1.2f, 1.5f, 0.45f);
        for (int s = -1; s <= 1; s += 2) {
            b.color(0.55f, 0.48f, 0.43f);
            b.box(0, 0.1f, s * 1.75f, 4.8f, 1.35f, 0.4f, 0.4f);
            b.push(); b.translate(4.8f, 0.1f, s * 1.75f); b.rotX(90); b.translate(0, -0.4f, 0);
            b.cylinder(1.35f, 1.35f, 0.8f, 12, true, true, 0.4f); b.pop();
            b.push(); b.translate(-4.8f, 0.1f, s * 1.75f); b.rotX(90); b.translate(0, -0.4f, 0);
            b.cylinder(1.35f, 1.35f, 0.8f, 12, true, true, 0.4f); b.pop();
        }
        b.color(0.7f, 0.64f, 0.6f);
        b.box(0.4f, 1.55f, 0, 1.4f, 0.4f, 1.2f, 0.45f);
        b.push(); b.translate(0.4f, 1.9f, 0); b.cylinder(1.0f, 0.85f, 0.7f, 10, true, true, 0.5f); b.pop();
        b.push(); b.translate(2.6f, 0.5f, 1.95f); b.rotZ(-90); b.cylinder(0.2f, 0.16f, 3.0f, 8, true, true, 0.6f); b.pop();
        b.push(); b.translate(2.6f, 0.5f, -1.95f); b.rotZ(-90); b.cylinder(0.2f, 0.16f, 2.4f, 8, true, true, 0.6f); b.pop();
        addBox(x - 5.6f, z - 2.3f, x + 5.6f, z + 2.3f, y + 3.2f);
        emitFire(x - 2.5f, y + 2.8f, z, 1.5f);
    }

    private void perkMachine(float x, float z, float yaw, int perk) {
        float[] c = PERK_COLOR[perk];
        float y = ground(x, z);
        MeshBuilder b = mb(M_BRICK, x, z);
        b.color(0.55f, 0.5f, 0.5f);
        b.translate(x, y, z); b.rotY(yaw);
        b.box(0, 1.05f, 0, 0.62f, 1.05f, 0.45f, 0.6f);
        b.box(0, 2.2f, 0, 0.72f, 0.1f, 0.55f, 0.6f);
        MeshBuilder g = mb(M_METAL, x, z);
        g.color(1f + c[0] * 1.2f, 1f + c[1] * 1.2f, 1f + c[2] * 1.2f);
        g.translate(x, y, z); g.rotY(yaw);
        g.box(0, 1.35f, 0.46f, 0.42f, 0.6f, 0.02f, 0.5f);
        g.color(0.25f, 0.25f, 0.27f);
        g.box(0, 0.45f, 0.47f, 0.4f, 0.25f, 0.03f, 0.5f);
        g.color(1f + c[0] * 1.5f, 1f + c[1] * 1.5f, 1f + c[2] * 1.5f);
        g.push(); g.translate(0, 2.3f, 0); g.cylinder(0.16f, 0.16f, 0.45f, 8, true, true, 0.4f); g.pop();
        light(x + sinD(yaw) * 1.2f, y + 1.5f, z + cosD(yaw) * 1.2f, c[0], c[1], c[2], 2.4f, 8f, 2);
        addCircle(x, z, 0.85f, y + 2.3f);
        Buyable by = new Buyable();
        by.type = B_PERK; by.id = perk; by.x = x + sinD(yaw) * 1.1f; by.z = z + cosD(yaw) * 1.1f; by.r = 1.7f; by.cost = PERK_COST[perk];
        by.name = PERK_NAME[perk];
        buyables.add(by);
    }

    private void wallBuy(float x, float z, float yaw, int weapon) {
        float y = ground(x, z);
        MeshBuilder b = mb(M_WOOD, x, z);
        b.color(0.6f, 0.55f, 0.5f);
        b.translate(x, y + 1.7f, z); b.rotY(yaw);
        b.box(0, 0, 0, 1.2f, 0.45f, 0.04f, 0.5f);
        MeshBuilder c = mb(M_CONC, x, z);
        c.color(1.55f, 1.5f, 1.35f);
        c.translate(x, y + 1.7f, z + 0.0f); c.rotY(yaw); c.translate(0, 0, 0.06f);
        WeaponModels.chalk(c, weapon);
        light(x + sinD(yaw) * 1.5f, y + 1.9f, z + cosD(yaw) * 1.5f, 1f, 0.95f, 0.8f, 0.8f, 5f, 0);
        Buyable by = new Buyable();
        by.type = B_WALL; by.id = weapon; by.x = x + sinD(yaw) * 1.1f; by.z = z + cosD(yaw) * 1.1f; by.r = 1.9f; by.cost = Weapons.PRICE[weapon];
        by.name = Weapons.NAME[weapon];
        buyables.add(by);
    }

    private void mysteryBox(float x, float z, float yaw) {
        float y = ground(x, z);
        MeshBuilder b = mb(M_WOOD, x, z);
        b.color(0.7f, 0.6f, 0.5f);
        b.translate(x, y, z); b.rotY(yaw);
        b.box(0, 0.4f, 0, 0.85f, 0.4f, 0.45f, 0.6f);
        b.box(0, 0.9f, 0, 0.85f, 0.1f, 0.45f, 0.6f);
        MeshBuilder m = mb(M_METAL, x, z);
        m.translate(x, y, z); m.rotY(yaw);
        m.color(0.6f, 0.55f, 0.5f);
        for (int s = -1; s <= 1; s += 2) m.box(s * 0.6f, 0.55f, 0, 0.07f, 0.55f, 0.47f, 0.5f);
        m.color(2.2f, 1.7f, 0.5f);
        m.box(0, 0.8f, 0.0f, 0.82f, 0.015f, 0.46f, 0.4f);
        m.box(0, 0.55f, 0.46f, 0.12f, 0.15f, 0.02f, 0.4f);
        light(x, y + 1.3f, z, 1f, 0.8f, 0.3f, 2.0f, 9f, 2);
        addBox(x - 1f, z - 0.9f, x + 1f, z + 0.9f, y + 1.0f);
        Buyable by = new Buyable();
        by.type = B_BOX; by.id = 0; by.x = x; by.z = z; by.r = 2.1f; by.cost = 950; by.name = "Caja Misteriosa";
        buyables.add(by);
    }

    // --------------------------------------------------------------- límites
    private void buildBoundary() {
        float e = 67.5f;
        brokenWall(-e, -e, e, -e, 2.0f, 5.5f, -4f, M_CONC, 0.35f);
        brokenWall(-e, e, e, e, 2.0f, 5.5f, -4f, M_CONC, 0.35f);
        brokenWall(-e, -e, -e, e, 2.0f, 5.5f, -4f, M_CONC, 0.35f);
        brokenWall(e, -e, e, e, 2.0f, 5.5f, -4f, M_CONC, 0.35f);
        addBox(-70, -70, 70, -66, 8); addBox(-70, 66, 70, 70, 8);
        addBox(-70, -70, -66, 70, 8); addBox(66, -70, 70, 70, 8);
        for (int i = 0; i < 70; i++) {
            float a = rr(0, 6.2832f), d = rr(70, 84);
            float x = (float) Math.cos(a) * d, z = (float) Math.sin(a) * d;
            if (Math.max(Math.abs(x), Math.abs(z)) < 69) continue;
            deadTree(x, z, rr(4.5f, 8f));
        }
        for (int i = 0; i < 40; i++) {
            float x = rr(-66, 66), z = rr(-66, 66);
            if (Math.max(Math.abs(x), Math.abs(z)) < 62) continue;
            deadTree(x, z, rr(4f, 7f));
        }
    }

    private void buildWalls() {
        brokenWall(-67, 28, -GAP, 28, 2.4f, 3.8f, -3.5f, M_BRICK, 0.6f);
        brokenWall(GAP, 28, 67, 28, 2.4f, 3.8f, -3.5f, M_BRICK, 0.6f);
        brokenWall(-67, -30, -GAP, -30, 2.4f, 3.8f, -3.5f, M_CONC, 0.35f);
        brokenWall(GAP, -30, 67, -30, 2.4f, 3.8f, -3.5f, M_CONC, 0.35f);
        brokenWall(-30, -28.8f, -30, -GAP, 2.4f, 3.8f, -3.5f, M_BRICK, 0.6f);
        brokenWall(-30, GAP, -30, 26.8f, 2.4f, 3.8f, -3.5f, M_BRICK, 0.6f);
        brokenWall(30, -28.8f, 30, -GAP, 2.4f, 3.8f, -3.5f, M_CONC, 0.35f);
        brokenWall(30, GAP, 30, 26.8f, 2.4f, 3.8f, -3.5f, M_CONC, 0.35f);
        addBox(-68, 26.8f, -GAP, 29.2f, 4.2f); addBox(GAP, 26.8f, 68, 29.2f, 4.2f);
        addBox(-68, -31.2f, -GAP, -28.8f, 4.2f); addBox(GAP, -31.2f, 68, -28.8f, 4.2f);
        addBox(-31.2f, -28.8f, -28.8f, -GAP, 4.2f); addBox(-31.2f, GAP, -28.8f, 26.8f, 4.2f);
        addBox(28.8f, -28.8f, 31.2f, -GAP, 4.2f); addBox(28.8f, GAP, 31.2f, 26.8f, 4.2f);
        float[][] gp = {{0, 28, 0, -1}, {0, -30, 0, -1}, {-30, 0, 1, 0}, {30, 0, 1, 0}};
        int[] costs = {750, 1000, 1250, 1250};
        for (int i = 0; i < 4; i++) {
            for (int s = -1; s <= 1; s += 2) {
                float px = gp[i][0] + (gp[i][2] == 0 ? s * (GAP + 0.7f) : 0), pz = gp[i][1] + (gp[i][2] == 0 ? 0 : s * (GAP + 0.7f));
                MeshBuilder b = mb(M_CONC, px, pz);
                b.color(0.9f, 0.9f, 0.9f);
                b.translate(px, 1.0f, pz);
                b.box(0, 0, 0, 0.9f, 3.0f, 1.5f, 0.4f);
                rubble(px, pz, 2.2f, 4, M_CONC);
            }
            Door d = new Door();
            d.id = i; d.cost = costs[i]; d.x = gp[i][0]; d.z = gp[i][1]; d.zone = i + 1;
            d.nx = gp[i][2] == 0 ? 0 : 1; d.nz = gp[i][2] == 0 ? 1 : 0;
            buildDoorMesh(d);
            boolean alongX = gp[i][2] == 0;
            d.col = alongX ? addBox(d.x - GAP, d.z - 1.5f, d.x + GAP, d.z + 1.5f, 4.2f) : addBox(d.x - 1.5f, d.z - GAP, d.x + 1.5f, d.z + GAP, 4.2f);
            doors.add(d);
            Buyable by = new Buyable();
            by.type = B_DOOR; by.id = i; by.x = d.x; by.z = d.z; by.r = 4.6f; by.cost = d.cost;
            by.name = i == 0 ? "Despejar escombros" : (i == 1 ? "Despejar a la iglesia" : (i == 2 ? "Abrir al cementerio" : "Abrir al taller"));
            buyables.add(by);
        }
    }

    private void buildDoorMesh(Door d) {
        MeshBuilder b = new MeshBuilder();
        b.color(0.8f, 0.75f, 0.7f);
        float yaw = d.nx == 1 ? 90 : 0;
        float gy = 0.0f;
        b.translate(d.x, gy, d.z);
        b.rotY(yaw);
        for (int i = 0; i < 8; i++) {
            float k = 0.7f + 0.4f * rnd();
            b.color(k, k * 0.95f, k * 0.9f);
            b.uOff = rnd(); b.vOff = rnd();
            b.push();
            b.translate(rr(-0.15f, 0.15f), -0.1f + i * 0.45f, rr(-0.5f, 0.5f));
            b.rotZ(rr(-2.5f, 2.5f)); b.rotY(rr(-3, 3));
            b.box(0, 0, 0, GAP + rr(-0.1f, 0.4f), 0.2f, 0.16f, 0.5f);
            b.pop();
        }
        for (int i = -2; i <= 2; i++) {
            b.color(0.6f, 0.55f, 0.5f);
            b.push(); b.translate(i * 1.6f, 1.8f, 0.45f); b.rotZ(rr(-3, 3));
            b.box(0, 0, 0, 0.12f, 1.9f, 0.1f, 0.5f); b.pop();
        }
        b.color(0.65f, 0.6f, 0.55f);
        b.push(); b.translate(0, 1.8f, 0.58f); b.rotZ(32); b.box(0, 0, 0, 5.2f, 0.14f, 0.07f, 0.5f); b.pop();
        b.push(); b.translate(0, 1.8f, 0.58f); b.rotZ(-32); b.box(0, 0, 0, 5.2f, 0.14f, 0.07f, 0.5f); b.pop();
        b.color(0.35f, 0.3f, 0.28f);
        for (int i = -3; i <= 3; i++) {
            b.push(); b.translate(i * 1.1f, 3.7f + (i % 2) * 0.1f, 0.5f); b.rotZ(rr(-40, 40));
            b.box(0, 0, 0, 0.5f, 0.015f, 0.015f, 0.5f); b.pop();
        }
        d.mesh = b.toArray(); d.verts = b.vertexCount();
    }

    // ------------------------------------------------------------------ zonas
    private void buildCamp() {
        sandbagLine(-8, 44, 0, 7, 3);
        sandbagLine(8, 44, 0, 7, 3);
        sandbagLine(-14, 36, 90, 5, 2);
        sandbagLine(14, 36, 90, 5, 2);
        sandbagLine(-30, 52, 40, 6, 2);
        sandbagLine(34, 50, -35, 6, 2);
        tent(-16, 56, 15); tent(18, 58, -10); tent(-36, 40, 70);
        wagon(24, 46, 20);
        crate(-5, 58, 10, 1.1f); crate(-3.7f, 58.4f, 40, 0.9f); crate(-4.4f, 58.2f, 5, 0.9f);
        crate(12, 40, 25, 1.0f); crate(40, 56, 8, 1.2f);
        barrel(6, 48, true); barrel(-9, 53, true); barrel(30, 38, false); barrel(31.2f, 38.3f, false); barrel(-40, 46, false);
        lampPost(-11, 47, 1f, 0.7f, 0.35f); lampPost(11, 47, 1f, 0.7f, 0.35f);
        lampPost(0, 36, 1f, 0.7f, 0.35f);
        barbedWire(-60, 60, -20, 62, false); barbedWire(20, 62, 60, 60, false);
        barbedWire(-62, 34, -62, 60, true); barbedWire(62, 34, 62, 60, true);
        cannon(-40, 52, 30);
        rubble(0, 38, 5, 10, M_CONC);
        wallBuy(-14, 29.3f, 0, Weapons.SHOTGUN);
        for (int i = 0; i < 14; i++) crateOrRock(rr(-60, 60), rr(32, 62));
    }

    private void crateOrRock(float x, float z) {
        if (nearGap(x, z, 9) || Math.hypot(x - startX, z - startZ) < 6) return;
        rubble(x, z, 0.6f, 1, M_CONC);
    }

    private void buildCrater() {
        for (int k = 0; k < 6; k++) {
            float a = k * 60 + 15;
            float x = 15.5f * cosD(a), z = 15.5f * sinD(a);
            float y = ground(x, z);
            MeshBuilder b = mb(M_CONC, x, z);
            b.color(0.85f, 0.85f, 0.85f);
            b.translate(x, y + 1.6f, z); b.rotY(-a + 90);
            b.rotZ(rr(-4, 4));
            b.box(0, 0, 0, 0.55f, 1.8f, 0.35f, 0.4f);
            b.box(0, 1.8f, 0, 0.42f, 0.18f, 0.3f, 0.4f);
            MeshBuilder r = mb(M_METAL, x, z);
            r.color(1.0f, 1.9f, 2.1f);
            r.translate(x, y + 1.6f, z); r.rotY(-a + 90);
            r.box(0, 0.2f, 0.37f, 0.3f, 0.6f, 0.01f, 0.4f);
            r.box(0, 0.9f, 0.37f, 0.18f, 0.08f, 0.01f, 0.4f);
            addCircle(x, z, 0.7f, y + 3.5f);
        }
        light(0, -1.5f, 0, 0.25f, 0.85f, 1.0f, 3.5f, 20f, 2);
        light(0, 3.0f, 0, 0.3f, 0.6f, 0.9f, 1.5f, 24f, 2);
        cannon(-9, 17, 90);
        wagon(22, 11, 70);
        crate(10, -16, 10, 1.1f); crate(11.2f, -16.3f, 40, 0.9f);
        barrel(-14, 12, true); barrel(16, -4, false); barrel(-22, -14, false);
        sandbagLine(-8, -24, 0, 6, 2); sandbagLine(14, 20, 20, 5, 2); sandbagLine(-16, 24, -20, 5, 2);
        barbedWire(-26, 6, -26, 20, true); barbedWire(12, -26, 26, -26, true); barbedWire(26, 4, 26, 18, true);
        lampPost(-6, 20, 0.9f, 0.7f, 0.4f); lampPost(8, -18, 0.9f, 0.7f, 0.4f);
        mysteryBox(boxX, boxZ, 90);
        perkMachine(19, -10, -90, PERK_VIGOR);
        rubble(-20, 0, 6, 12, M_CONC); rubble(20, 2, 6, 12, M_CONC);
    }

    private void wallOpenings(boolean alongX, float fixed, float a0, float a1, float thick, float hMax, int mat, float uvs, float[][] op) {
        int n = Math.max(1, (int) Math.ceil((a1 - a0) / 1.5f));
        float seg = (a1 - a0) / n;
        for (int i = 0; i < n; i++) {
            float s0 = a0 + i * seg, s1 = s0 + seg;
            float h = hMax * (0.55f + 0.45f * rnd());
            if (rnd() < 0.06f) h = hMax * 0.3f;
            float cur = s0;
            while (cur < s1 - 0.001f) {
                float[] o = null;
                for (int k = 0; k < op.length; k++) {
                    float lo = op[k][0] - op[k][1] / 2, hi = op[k][0] + op[k][1] / 2;
                    if (cur >= lo - 0.001f && cur < hi) { o = op[k]; break; }
                }
                float next = s1;
                if (o != null) next = Math.min(s1, o[0] + o[1] / 2);
                else {
                    for (int k = 0; k < op.length; k++) {
                        float lo = op[k][0] - op[k][1] / 2;
                        if (lo > cur && lo < next) next = lo;
                    }
                }
                float mid = (cur + next) / 2, half = (next - cur) / 2;
                float cxm = alongX ? mid : fixed, czm = alongX ? fixed : mid;
                MeshBuilder b = mb(mat, cxm, czm);
                b.uOff = rnd(); b.vOff = rnd();
                float kk = 0.8f + 0.3f * rnd();
                b.color(kk, kk, kk);
                float y0 = 0.1f;
                float hx = alongX ? half : thick / 2, hz = alongX ? thick / 2 : half;
                if (o == null) {
                    if (h > y0 + 0.05f) b.box(cxm, (y0 + h) / 2, czm, hx, (h - y0) / 2, hz, uvs);
                } else {
                    float sill = Math.min(o[2], h);
                    b.box(cxm, (y0 + sill) / 2, czm, hx, (sill - y0) / 2, hz, uvs);
                    if (o[3] < h) b.box(cxm, (o[3] + h) / 2, czm, hx, (h - o[3]) / 2, hz, uvs);
                }
                cur = next;
            }
        }
    }

    private void buildChurch() {
        float[][] winL = {{-44, 1.6f, 1.7f, 5.6f}, {-50, 1.6f, 1.7f, 5.6f}, {-56, 1.6f, 1.7f, 5.6f}};
        float hW = 8.0f;
        wallOpenings(false, -6.5f, -62, -40, 1.0f, hW, M_BRICK, 0.6f, winL);
        wallOpenings(false, 6.5f, -62, -40, 1.0f, hW, M_BRICK, 0.6f, winL);
        wallOpenings(true, -62, -7, 7, 1.0f, 9f, M_BRICK, 0.6f, new float[][]{{0, 3.4f, 3.0f, 8.0f}});
        wallOpenings(true, -40, -7, -2, 1.0f, 7f, M_BRICK, 0.6f, new float[0][]);
        wallOpenings(true, -40, 2, 7, 1.0f, 7f, M_BRICK, 0.6f, new float[0][]);
        addBox(-7.2f, -62.6f, -5.8f, -39.6f, 7f); addBox(5.8f, -62.6f, 7.2f, -39.6f, 7f);
        addBox(-7.2f, -62.6f, 7.2f, -61.4f, 8f);
        addBox(-7.2f, -40.6f, -2f, -39.4f, 7f); addBox(2f, -40.6f, 7.2f, -39.4f, 7f);
        MeshBuilder fl = mb(M_CONC, 0, -51);
        fl.color(0.9f, 0.85f, 0.85f);
        fl.box(0, 0.1f, -51, 6.2f, 0.18f, 11f, 0.35f);
        MeshBuilder gl = mb(M_METAL, 0, -61);
        float[][] gc = {{1.9f, 0.7f, 0.4f}, {0.5f, 1.1f, 1.9f}, {1.7f, 1.2f, 0.4f}, {1.0f, 0.5f, 1.6f}, {0.6f, 1.7f, 0.9f}};
        for (int cI = -1; cI <= 1; cI++) {
            for (int rI = 0; rI < 5; rI++) {
                float[] cc = gc[(cI + 1 + rI * 2) % 5];
                gl.color(0.9f + cc[0] * 0.7f, 0.9f + cc[1] * 0.7f, 0.9f + cc[2] * 0.7f);
                gl.box(cI * 1.05f, 3.4f + rI * 0.95f, -61.55f, 0.48f, 0.43f, 0.03f, 0.5f);
            }
        }
        light(0, 5.0f, -58, 0.8f, 0.55f, 1.0f, 3.0f, 17f, 2);
        // contrafuertes
        for (int s = -1; s <= 1; s += 2) {
            for (int i = 0; i < 4; i++) {
                float bz = -42 - i * 6.2f;
                MeshBuilder b = mb(M_BRICK, s * 7.3f, bz);
                b.color(0.9f, 0.88f, 0.85f);
                float hh = 3.5f + 2.5f * rnd();
                b.box(s * 7.8f, hh / 2, bz, 0.5f, hh / 2, 0.6f, 0.6f);
            }
        }
        // torre
        for (int lv = 0; lv < 5; lv++) {
            float hh = lv < 4 ? 3.8f : 2.2f + 1.5f * rnd();
            MeshBuilder b = mb(M_BRICK, -9.5f, -43.5f);
            b.uOff = lv * 0.31f;
            b.color(0.85f, 0.83f, 0.8f);
            float sc = 1f - lv * 0.05f;
            b.box(-9.5f, lv * 3.8f + hh / 2, -43.5f, 3.0f * sc, hh / 2, 3.0f * sc, 0.6f);
            MeshBuilder d = mb(M_CONC, -9.5f, -43.5f);
            d.color(0.08f, 0.08f, 0.1f);
            if (lv >= 1) {
                d.box(-9.5f, lv * 3.8f + 2f, -40.45f + 0.01f, 0.35f * sc, 0.8f, 0.02f, 0.5f);
                d.box(-6.45f + 0.0f, lv * 3.8f + 2f, -43.5f, 0.02f, 0.8f, 0.35f * sc, 0.5f);
            }
        }
        MeshBuilder tb = mb(M_BARK, -9.5f, -43.5f);
        tb.color(0.9f, 0.9f, 0.9f);
        tb.translate(-9.5f, 18.6f, -43.5f); tb.rotZ(18);
        tb.box(0, 0, 0, 0.3f, 1.8f, 0.3f, 0.6f);
        addBox(-12.6f, -46.6f, -6.4f, -40.4f, 19f);
        light(-9.5f, 12.5f, -40.2f, 1f, 0.6f, 0.3f, 1.4f, 9f, 1);
        // vigas rotas del techo
        for (int i = 0; i < 6; i++) {
            float bz = -41.5f - i * 3.6f;
            MeshBuilder b = mb(M_WOOD, 0, bz);
            b.color(0.65f, 0.6f, 0.55f);
            for (int s = -1; s <= 1; s += 2) {
                if (rnd() < 0.3f) continue;
                b.push();
                b.translate(s * 3.4f, 9.4f, bz); b.rotZ(-s * 38);
                float l = rr(2.5f, 4.4f);
                b.box(0, 0, 0, l, 0.16f, 0.16f, 0.5f);
                b.pop();
            }
        }
        // bancas
        for (int s = -1; s <= 1; s += 2) {
            for (int i = 0; i < 7; i++) {
                float bz = -44.5f - i * 1.9f, bx = s * 3.3f;
                if (rnd() < 0.2f) continue;
                MeshBuilder b = mb(M_WOOD, bx, bz);
                b.color(0.7f, 0.62f, 0.55f);
                b.translate(bx, 0.55f, bz);
                if (rnd() < 0.25f) { b.rotZ(rr(60, 90)); b.translate(0, 0.3f, 0); }
                b.rotY(rr(-6, 6));
                b.box(0, 0, 0, 1.7f, 0.06f, 0.28f, 0.5f);
                b.box(0, 0.45f, -0.26f, 1.7f, 0.4f, 0.04f, 0.5f);
                b.box(-1.5f, -0.28f, 0, 0.05f, 0.28f, 0.25f, 0.5f);
                b.box(1.5f, -0.28f, 0, 0.05f, 0.28f, 0.25f, 0.5f);
                addCircle(bx, bz, 0.9f, 0.95f);
            }
        }
        MeshBuilder al = mb(M_CONC, 0, -59);
        al.color(0.95f, 0.92f, 0.9f);
        al.box(0, 0.75f, -59.2f, 1.6f, 0.55f, 0.5f, 0.4f);
        al.box(0, 1.35f, -59.2f, 1.8f, 0.07f, 0.6f, 0.4f);
        addBox(-1.8f, -59.9f, 1.8f, -58.5f, 1.4f);
        MeshBuilder ca = mb(M_METAL, 0, -59);
        ca.color(2.2f, 1.5f, 0.6f);
        for (int i = -1; i <= 1; i++) ca.box(i * 1.0f, 1.65f, -59.2f, 0.035f, 0.2f, 0.035f, 0.3f);
        for (int i = -1; i <= 1; i++) light(i * 1.0f, 2.0f, -59, 1f, 0.62f, 0.25f, 1.0f, 6f, 1);
        perkMachine(4.9f, -58.5f, -90, PERK_RAFAGA);
        rubble(0, -47, 4.5f, 14, M_CONC);
        rubble(-4, -58, 2.5f, 6, M_BRICK);
        // cementerio de la iglesia
        for (int i = 0; i < 46; i++) {
            float x = rr(-60, 60), z = rr(-66, -34);
            if (Math.abs(x) < 15 && z < -38 && z > -65) continue;
            if (nearGap(x, z, 8)) continue;
            gravestone(x, z, rr(-25, 25) + 180, (int) (rnd() * 3));
        }
        barbedWire(-60, -36, -34, -36, true); barbedWire(34, -36, 60, -36, true);
        barrel(-16, -34, true); barrel(14, -35, true);
        lampPost(-4, -35, 0.7f, 0.9f, 1f); 
        sandbagLine(-20, -33, 0, 6, 2);
    }

    private void buildWest() {
        MeshBuilder c = mb(M_CONC, -52, -8);
        c.color(0.85f, 0.85f, 0.85f);
        c.box(-52, 1.7f, -8, 3.0f, 1.7f, 4.0f, 0.35f);
        c.box(-52, 3.5f, -8, 3.4f, 0.2f, 4.4f, 0.35f);
        MeshBuilder d = mb(M_CONC, -49, -8);
        d.color(0.04f, 0.04f, 0.05f);
        d.box(-48.97f, 1.0f, -8, 0.02f, 1.0f, 0.9f, 0.5f);
        addBox(-55, -12, -49, -4, 3.7f);
        light(-47.5f, 1.4f, -8, 0.3f, 1.0f, 0.5f, 1.4f, 8f, 2);
        wallBuy(-48.9f, -11.4f, 90, Weapons.SMG);
        perkMachine(-55, 12, 0, PERK_REFLEJOS);
        for (int i = 0; i < 40; i++) {
            float x = rr(-64, -34), z = rr(-26, 24);
            if (Math.hypot(x + 52, z + 8) < 6) continue;
            if (nearGap(x, z, 9)) continue;
            gravestone(x, z, rr(-20, 20) + 90, (int) (rnd() * 3));
        }
        barbedWire(-62, -26, -62, 24, true);
        barbedWire(-50, 22, -34, 22, true);
        barrel(-40, 8, true); barrel(-58, -20, true);
        lampPost(-36, -6, 0.6f, 1f, 0.6f); lampPost(-45, 14, 0.6f, 1f, 0.6f);
        rubble(-45, 0, 8, 10, M_CONC);
    }

    private void buildEast() {
        tank(49, -6);
        MeshBuilder s = mb(M_METAL, 46, 16);
        s.color(0.8f, 0.72f, 0.66f);
        s.box(46, 1.5f, 11.5f, 4.5f, 1.5f, 0.1f, 0.45f);
        s.box(41.5f, 1.5f, 15.5f, 0.1f, 1.5f, 4.0f, 0.45f);
        s.box(50.5f, 1.5f, 15.5f, 0.1f, 1.5f, 4.0f, 0.45f);
        s.push(); s.translate(46, 3.4f, 15.5f); s.rotZ(8); s.box(0, 0, 0, 5f, 0.08f, 2.8f, 0.45f); s.pop();
        addBox(41.3f, 11.4f, 50.7f, 11.6f, 3.2f); addBox(41.3f, 11.4f, 41.7f, 19.6f, 3.2f); addBox(50.3f, 11.4f, 50.7f, 19.6f, 3.2f);
        wallBuy(46, 11.3f, 180, Weapons.RIFLE);
        perkMachine(40, -18, 90, PERK_PIES);
        crate(54, 16, 20, 1.2f); crate(55.3f, 16.4f, 5, 1.0f); crate(48, 22, 40, 1.0f);
        barrel(36, 8, true); barrel(56, -20, true); barrel(38, 22, false);
        // camión viejo
        MeshBuilder t = mb(M_METAL, 56, 4);
        float ty = ground(56, 4);
        t.color(0.6f, 0.5f, 0.45f);
        t.translate(56, ty + 1.0f, 4); t.rotY(90);
        t.box(-1.4f, 0.6f, 0, 1.0f, 0.9f, 1.1f, 0.4f);
        t.box(1.3f, 0.2f, 0, 2.2f, 0.15f, 1.1f, 0.4f);
        addBox(53.8f, 0.8f, 58.2f, 7.2f, ty + 2.2f);
        lampPost(36, 14, 1f, 0.7f, 0.4f); lampPost(52, -14, 1f, 0.7f, 0.4f);
        barbedWire(62, -26, 62, 24, true);
        barbedWire(34, -26, 34, -14, false);
        rubble(40, 6, 8, 14, M_CONC);
        for (int i = 0; i < 20; i++) crateOrRock(rr(34, 64), rr(-26, 24));
    }

    private void scatterTrees() {
        int placed = 0, guard = 0;
        while (placed < 70 && guard++ < 1500) {
            float x = rr(-64, 64), z = rr(-64, 62);
            if (nearGap(x, z, 10)) continue;
            if (Math.hypot(x - startX, z - startZ) < 9) continue;
            if (Math.abs(x) < 17 && z < -37 && z > -66) continue;
            if (Math.hypot(x, z) < 19) continue;
            if (Math.abs(z - 28) < 3 || Math.abs(z + 30) < 3) continue;
            if ((Math.abs(x + 30) < 3 || Math.abs(x - 30) < 3) && z > -30 && z < 28) continue;
            boolean bad = false;
            for (int i = 0; i < cols.size() && !bad; i++) {
                Collider c = cols.get(i);
                if (c.circle) { if (Math.hypot(x - c.x1, z - c.z1) < c.r + 2.4f) bad = true; }
                else if (x > c.x1 - 2.4f && x < c.x2 + 2.4f && z > c.z1 - 2.4f && z < c.z2 + 2.4f) bad = true;
            }
            for (int i = 0; i < buyables.size() && !bad; i++) if (Math.hypot(x - buyables.get(i).x, z - buyables.get(i).z) < 5) bad = true;
            if (bad) continue;
            deadTree(x, z, rr(4.2f, 7.5f));
            placed++;
        }
    }

    private void buildSpawns() {
        zoneSpawns[0] = new float[][]{{-30, 60}, {-10, 63}, {12, 63}, {32, 58}, {-46, 45}, {46, 46}, {-52, 58}, {54, 56}};
        zoneSpawns[1] = new float[][]{{-20, -16}, {20, -20}, {-22, 20}, {22, 22}, {0, -22}, {-2, 20}, {13, 2}, {-13, -2}};
        zoneSpawns[2] = new float[][]{{-32, -46}, {32, -46}, {-16, -66}, {16, -66}, {0, -35}, {-46, -56}, {46, -56}, {-24, -42}, {24, -42}};
        zoneSpawns[3] = new float[][]{{-46, -20}, {-46, 20}, {-60, 0}, {-40, -23}, {-40, 23}, {-58, -18}, {-58, 18}};
        zoneSpawns[4] = new float[][]{{46, -22}, {46, 22}, {60, 0}, {40, -24}, {38, 24}, {58, -18}, {58, 20}};
    }

    public boolean isBlocked(float x, float z, float rad) {
        float[] p = {x, z};
        float ox = x, oz = z;
        resolve(p, rad);
        return Math.abs(p[0] - ox) > 0.001f || Math.abs(p[1] - oz) > 0.001f;
    }
}
