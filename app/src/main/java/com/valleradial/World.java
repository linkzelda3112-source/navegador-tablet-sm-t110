package com.valleradial;

import java.util.ArrayList;
import java.util.Random;

/**
 * El mundo es el INTERIOR de una esfera gigante. La gravedad apunta hacia afuera del centro,
 * asi que caminar hacia cualquier lado hace que las paredes se vuelvan piso.
 * El terreno se describe por la direccion unitaria n: radio del suelo = R - altura(n).
 * Codigo puro (sin Android) para poder probarlo en una JVM normal.
 */
public final class World {
    public static final float R = 230f;
    public static final float TWO_PI = (float) (Math.PI * 2.0);

    public static final int HOUSE = 0, TREE = 1, ROCK = 2, BUSH = 3, TOWER = 4, ARCH = 5, WINDMILL = 6, RUIN = 7;

    public static final class Prop {
        public int kind, variant;
        public float x, y, z;       // base sobre el suelo
        public float ux, uy, uz;    // "arriba" local (hacia el centro)
        public float ax, ay, az;    // eje X local unitario (para colisionadores desplazados)
        public float r, h;          // cilindro de colision
        public float off;           // si > 0: dos cilindros a +-off sobre el eje X local
        public boolean solid, standable;
        public float sr, sh;        // esfera (copa de arbol): radio y altura
        public float scale, yaw;
        public final float[] model = new float[16];
    }

    public final ArrayList<Prop> props = new ArrayList<>();

    // ---------------------------------------------------------------- caminos (circulos maximos ondulados)
    private static final float[][] PN = new float[2][3], PE1 = new float[2][3], PE2 = new float[2][3];

    static {
        setCircle(0, 0f, 1f, 0f);
        setCircle(1, 1f, 0.25f, 0.6f);
    }

    private static float[] norm(float x, float y, float z) {
        float l = (float) Math.sqrt(x * x + y * y + z * z);
        return new float[]{x / l, y / l, z / l};
    }

    private static float[] cross(float[] a, float[] b) {
        return new float[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static void setCircle(int i, float nx, float ny, float nz) {
        float[] n = norm(nx, ny, nz);
        float[] ref = Math.abs(n[1]) < 0.9f ? new float[]{0, 1, 0} : new float[]{1, 0, 0};
        float[] e1 = cross(n, ref);
        e1 = norm(e1[0], e1[1], e1[2]);
        float[] e2 = cross(n, e1);
        PN[i] = n; PE1[i] = e1; PE2[i] = e2;
    }

    private static float wobble(int i, float phi) {
        if (i == 0) return 0.16f * (float) Math.sin(3 * phi + 0.5) + 0.07f * (float) Math.sin(7 * phi + 1.0);
        return 0.14f * (float) Math.sin(2 * phi + 2.0) + 0.06f * (float) Math.sin(5 * phi);
    }

    /** Distancia (unidades de arco) al camino mas cercano, para la direccion unitaria (x,y,z). */
    public static float pathDist(float x, float y, float z) {
        float best = 1e9f;
        for (int i = 0; i < 2; i++) {
            float[] n = PN[i], e1 = PE1[i], e2 = PE2[i];
            float sn = x * n[0] + y * n[1] + z * n[2];
            if (sn > 1f) sn = 1f;
            if (sn < -1f) sn = -1f;
            float s = (float) Math.asin(sn);
            float phi = (float) Math.atan2(x * e2[0] + y * e2[1] + z * e2[2], x * e1[0] + y * e1[1] + z * e1[2]);
            float d = Math.abs(s - wobble(i, phi)) * R;
            if (d < best) best = d;
        }
        return best;
    }

    /** 0..1, el shader corta en 0.5 (borde del camino). */
    public static float pathAttr(float x, float y, float z) {
        float v = 1.5f - pathDist(x, y, z) / 3f;
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    public static float smooth(float a, float b, float x) {
        float t = (x - a) / (b - a);
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        return t * t * (3f - 2f * t);
    }

    public static float height(float nx, float ny, float nz) {
        float px = nx * R, py = ny * R, pz = nz * R;
        float a = 0.5f + 0.5f * (float) Math.sin(px * 0.021f + py * 0.017f);
        float b = 0.5f + 0.5f * (float) Math.sin(py * 0.013f - pz * 0.019f + 2f);
        float c = (float) (Math.sin(pz * 0.045f + px * 0.03f) * Math.sin(px * 0.038f - py * 0.041f));
        float h = 1.5f + 5.0f * a * b + 1.2f * c;
        float w = 1f - smooth(4f, 12f, pathDist(nx, ny, nz));
        return h * (1f - 0.85f * w);
    }

    public static float groundRadius(float nx, float ny, float nz) {
        return R - height(nx, ny, nz);
    }

    /** Direccion de aparicion (sobre el camino 0) y hacia donde mira. */
    public static float[] spawnDir() {
        float s = wobble(0, 0f);
        float c = (float) Math.cos(s), sn = (float) Math.sin(s);
        float[] n = PN[0], e1 = PE1[0];
        return new float[]{c * e1[0] + sn * n[0], c * e1[1] + sn * n[1], c * e1[2] + sn * n[2]};
    }

    public static float[] spawnForward() {
        return PE2[0].clone();
    }

    // ---------------------------------------------------------------- estructuras
    public World() {
        Random rnd = new Random(7);
        scatter(rnd, HOUSE, 95);
        scatter(rnd, TOWER, 26);
        scatter(rnd, ARCH, 24);
        scatter(rnd, WINDMILL, 18);
        scatter(rnd, RUIN, 34);
        scatter(rnd, TREE, 700);
        scatter(rnd, ROCK, 330);
        scatter(rnd, BUSH, 240);
    }

    private void scatter(Random rnd, int kind, int count) {
        float[] sp = spawnDir();
        float spx = sp[0] * R, spy = sp[1] * R, spz = sp[2] * R;
        int placed = 0, tries = 0;
        while (placed < count && tries < count * 60) {
            tries++;
            float gx = (float) rnd.nextGaussian(), gy = (float) rnd.nextGaussian(), gz = (float) rnd.nextGaussian();
            float gl = (float) Math.sqrt(gx * gx + gy * gy + gz * gz);
            if (gl < 1e-3f) continue;
            float nx = gx / gl, ny = gy / gl, nz = gz / gl;
            float dp = pathDist(nx, ny, nz);
            float margin = (kind == HOUSE || kind == TOWER || kind == WINDMILL || kind == ARCH) ? 9f : 5f;
            if (kind == ARCH) margin = 0f; // los arcos pueden cruzar el camino
            if (dp < margin) continue;

            Prop p = new Prop();
            p.kind = kind;
            switch (kind) {
                case HOUSE:
                    p.scale = 1.0f + rnd.nextFloat() * 0.35f;
                    p.variant = rnd.nextInt(4);
                    p.r = 4.6f * p.scale; p.h = 8f * p.scale; p.solid = true; p.standable = true;
                    break;
                case TOWER:
                    p.scale = 1.0f + rnd.nextFloat() * 0.5f;
                    p.r = 2.8f * p.scale; p.h = 24f * p.scale; p.solid = true;
                    break;
                case ARCH:
                    p.scale = 1.0f + rnd.nextFloat() * 0.5f;
                    p.off = 3.3f * p.scale; p.r = 1.1f * p.scale; p.h = 9f * p.scale; p.solid = true;
                    break;
                case WINDMILL:
                    p.scale = 1.0f + rnd.nextFloat() * 0.4f;
                    p.r = 2.9f * p.scale; p.h = 12f * p.scale; p.solid = true;
                    break;
                case RUIN:
                    p.scale = 0.9f + rnd.nextFloat() * 0.6f;
                    p.off = 2.5f * p.scale; p.r = 0.9f * p.scale; p.h = 4.2f * p.scale; p.solid = true;
                    break;
                case TREE:
                    p.scale = 0.8f + rnd.nextFloat() * 0.8f; p.variant = rnd.nextInt(4);
                    p.r = 0.7f * p.scale; p.h = 6f * p.scale; p.solid = true;
                    p.sr = 3.6f * p.scale; p.sh = 7.4f * p.scale;
                    break;
                case ROCK:
                    p.scale = 0.8f + rnd.nextFloat() * 1.8f;
                    p.r = 1.7f * p.scale; p.h = 1.5f * p.scale; p.solid = true; p.standable = true;
                    break;
                default:
                    p.scale = 0.8f + rnd.nextFloat() * 0.7f; p.variant = rnd.nextInt(2);
                    p.r = 1.2f * p.scale; p.h = 1.2f * p.scale; p.solid = false;
                    break;
            }
            p.yaw = rnd.nextFloat() * TWO_PI;
            float rg = groundRadius(nx, ny, nz);
            p.x = nx * rg; p.y = ny * rg; p.z = nz * rg;
            p.ux = -nx; p.uy = -ny; p.uz = -nz;

            // lejos del punto de aparicion
            float sx = p.x - spx, sy = p.y - spy, sz = p.z - spz;
            if (sx * sx + sy * sy + sz * sz < 22f * 22f) continue;

            boolean ok = true;
            float ext = p.r + p.off;
            for (Prop q : props) {
                float dx = p.x - q.x, dy = p.y - q.y, dz = p.z - q.z;
                float lim = ext + q.r + q.off + 2.5f;
                if (dx * dx + dy * dy + dz * dz < lim * lim) { ok = false; break; }
            }
            if (!ok) continue;
            fillModel(p, nx, ny, nz);
            props.add(p);
            placed++;
        }
    }

    /** Matriz de modelo (columnas): X, Y=arriba, Z=X x Y, con giro yaw sobre Y y escala uniforme. */
    static void fillModel(Prop p, float nx, float ny, float nz) {
        float ux = -nx, uy = -ny, uz = -nz;
        float rx, ry, rz;
        if (Math.abs(ny) < 0.9f) { rx = 0; ry = 1; rz = 0; } else { rx = 1; ry = 0; rz = 0; }
        // X0 = up x ref
        float xx = uy * rz - uz * ry, xy = uz * rx - ux * rz, xz = ux * ry - uy * rx;
        float xl = (float) Math.sqrt(xx * xx + xy * xy + xz * xz);
        xx /= xl; xy /= xl; xz /= xl;
        // Z0 = X0 x up
        float zx = xy * uz - xz * uy, zy = xz * ux - xx * uz, zz = xx * uy - xy * ux;
        float cy = (float) Math.cos(p.yaw), sy = (float) Math.sin(p.yaw);
        float ax = xx * cy + zx * sy, ay = xy * cy + zy * sy, az = xz * cy + zz * sy;
        float bx = -xx * sy + zx * cy, by = -xy * sy + zy * cy, bz = -xz * sy + zz * cy;
        p.ax = ax; p.ay = ay; p.az = az;
        float k = p.scale;
        float[] m = p.model;
        m[0] = ax * k; m[1] = ay * k; m[2] = az * k; m[3] = 0;
        m[4] = ux * k; m[5] = uy * k; m[6] = uz * k; m[7] = 0;
        m[8] = bx * k; m[9] = by * k; m[10] = bz * k; m[11] = 0;
        m[12] = p.x; m[13] = p.y; m[14] = p.z; m[15] = 1;
    }
}
