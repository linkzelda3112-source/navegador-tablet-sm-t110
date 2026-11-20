package com.valleradial;

import java.util.ArrayList;
import java.util.Random;

/**
 * El mundo es el INTERIOR de un cilindro gigante (eje = eje Z). La gravedad apunta
 * hacia afuera del eje, asi que los muros se convierten en piso.
 * Coordenadas del terreno: (theta, z). Radio del suelo = R - altura(theta, z).
 * Codigo puro (sin Android) para poder probarlo en una JVM normal.
 */
public final class World {
    public static final float R = 150f;
    public static final float L = 800f;
    public static final int NT = 312;   // celdas alrededor del tubo
    public static final int NZ = 264;   // celdas a lo largo
    public static final float TWO_PI = (float) (Math.PI * 2.0);

    public static final int HOUSE = 0, TREE = 1, ROCK = 2, BUSH = 3;

    public static final class Prop {
        public int kind, variant;
        public float x, y, z;       // base sobre el suelo
        public float ux, uy, uz;    // "arriba" local (hacia el eje)
        public float r, h;          // cilindro de colision
        public boolean solid, standable;
        public float sr, sh;        // esfera (copa de arbol): radio y altura
        public float scale, yaw;
        public final float[] model = new float[16];
    }

    public final ArrayList<Prop> props = new ArrayList<>();

    // ---------------------------------------------------------------- funciones del terreno
    public static float wrapPi(float a) {
        a = (float) (a % TWO_PI);
        if (a > Math.PI) a -= TWO_PI;
        if (a < -Math.PI) a += TWO_PI;
        return a;
    }

    public static float smooth(float a, float b, float x) {
        float t = (x - a) / (b - a);
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        return t * t * (3f - 2f * t);
    }

    /** Angulo del camino en cada z: serpentea subiendo por las paredes. */
    public static float pathTheta(float z) {
        return (float) (-Math.PI / 2.0 + 0.6 * Math.sin(z * 0.009) + 0.25 * Math.sin(z * 0.023 + 1.0));
    }

    /** Distancia (en unidades de arco) al centro del camino. */
    public static float pathDist(float th, float z) {
        return Math.abs(wrapPi(th - pathTheta(z))) * R;
    }

    /** 0..1, 0.5 justo en el borde del camino (el shader corta en 0.5). */
    public static float pathAttr(float th, float z) {
        float v = 1.5f - pathDist(th, z) / 3f;
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    public static float height(float th, float z) {
        float a = 0.5f + 0.5f * (float) Math.sin(3 * th + z * 0.017f);
        float b = 0.5f + 0.5f * (float) Math.sin(5 * th - z * 0.011f + 2f);
        float h = 1.5f + 4.5f * a * b
                + 1.2f * (float) (Math.sin(11 * th + z * 0.05f) * Math.sin(7 * th - z * 0.04f));
        float w = 1f - smooth(4f, 12f, pathDist(th, z));
        return h * (1f - 0.85f * w);
    }

    public static float groundRadius(float th, float z) {
        return R - height(th, z);
    }

    // ---------------------------------------------------------------- props
    public World() {
        Random rnd = new Random(7);
        scatter(rnd, HOUSE, 34);
        scatter(rnd, TREE, 330);
        scatter(rnd, ROCK, 170);
        scatter(rnd, BUSH, 130);
    }

    private void scatter(Random rnd, int kind, int count) {
        float spawnTh = pathTheta(30f);
        int placed = 0, tries = 0;
        while (placed < count && tries < count * 60) {
            tries++;
            float th = rnd.nextFloat() * TWO_PI;
            float z = 12f + rnd.nextFloat() * (L - 24f);
            float dp = pathDist(th, z);
            float margin = kind == HOUSE ? 9f : 5f;
            if (dp < margin) continue;
            if (pathDist(th, z) < 1000f && Math.abs(z - 30f) < 18f
                    && Math.abs(wrapPi(th - spawnTh)) * R < 18f) continue;

            Prop p = new Prop();
            p.kind = kind;
            switch (kind) {
                case HOUSE:
                    p.scale = 1.0f + rnd.nextFloat() * 0.35f;
                    p.r = 4.6f * p.scale; p.h = 8f * p.scale; p.solid = true; p.standable = true;
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
            float rg = groundRadius(th, z);
            float c = (float) Math.cos(th), s = (float) Math.sin(th);
            p.x = rg * c; p.y = rg * s; p.z = z;
            p.ux = -c; p.uy = -s; p.uz = 0f;

            boolean ok = true;
            for (Prop q : props) {
                float dx = p.x - q.x, dy = p.y - q.y, dz = p.z - q.z;
                float lim = p.r + q.r + 2.5f;
                if (dx * dx + dy * dy + dz * dz < lim * lim) { ok = false; break; }
            }
            if (!ok) continue;
            fillModel(p, th);
            props.add(p);
            placed++;
        }
    }

    /** Matriz de modelo (columnas): X=(-s,c,0), Y=arriba, Z=(0,0,1), con giro yaw. */
    static void fillModel(Prop p, float th) {
        float c = (float) Math.cos(th), s = (float) Math.sin(th);
        float xx = -s, xy = c;
        float cy = (float) Math.cos(p.yaw), sy = (float) Math.sin(p.yaw);
        float[] m = p.model;
        float k = p.scale;
        m[0] = (xx * cy) * k;  m[1] = (xy * cy) * k;  m[2] = sy * k;   m[3] = 0;
        m[4] = -c * k;         m[5] = -s * k;         m[6] = 0;        m[7] = 0;
        m[8] = (-xx * sy) * k; m[9] = (-xy * sy) * k; m[10] = cy * k;  m[11] = 0;
        m[12] = p.x; m[13] = p.y; m[14] = p.z; m[15] = 1;
    }
}
