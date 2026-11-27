package com.craterz;

/**
 * Zombi detallado: cabeza esculpida con mandíbula móvil, torso con uniforme rasgado o costillas,
 * miembros con volumen, manos con dedos en garra y botas. Todo se genera por código (original).
 */
public final class ZombieModel {
    public static final int TORSO_COAT = 0, TORSO_RAG = 1, HEAD = 2, JAW = 3, HELMET = 4, UARM_COAT = 5, UARM_BARE = 6,
            FARM_NEG = 7, FARM_POS = 8, THIGH = 9, SHIN = 10, NPARTS = 11;
    public final float[][] part = new float[NPARTS][];

    // regiones del atlas (con un pequeño margen para que no se mezclen al reducir)
    private static final float A0 = 0.006f, A1 = 0.494f, B0 = 0.506f, B1 = 0.994f;
    // piel: u A0..A1, v A0..A1 ; cara: u B0..B1, v A0..A1 ; tela: u A0..A1, v B0..B1 ; cuero: u B0..0.745 ; casco: u 0.755..B1

    private static float gauss(float x, float c, float s) { float d = (x - c) / s; return (float) Math.exp(-d * d); }
    private static float ss(float a, float b, float x) { return Noise.smoothstep(a, b, x); }

    public ZombieModel() {
        buildTorsoCoat();
        buildTorsoRag();
        buildHead();
        buildHelmet();
        buildArms();
        buildLegs();
    }

    // ------------------------------------------------------------------ torsos
    private void skinUV(MeshBuilder b) { b.uvRect(A0, A0, A1, A1); }

    private void buildTorsoCoat() {
        MeshBuilder b = new MeshBuilder();
        // abrigo: de la falda (y=-0.38) al cuello
        float[] ys = {-0.38f, -0.32f, -0.2f, -0.08f, 0.04f, 0.16f, 0.28f, 0.38f, 0.46f, 0.52f, 0.565f, 0.595f};
        float[] rx = {0.215f, 0.205f, 0.19f, 0.178f, 0.172f, 0.19f, 0.222f, 0.24f, 0.205f, 0.14f, 0.098f, 0.085f};
        float[] rz = {0.13f, 0.127f, 0.118f, 0.108f, 0.104f, 0.116f, 0.128f, 0.12f, 0.1f, 0.08f, 0.07f, 0.066f};
        b.color(1, 1, 1);
        b.latheEx(ys, rx, rz, 28, A0, A1, B1, B0, 0.11f, 0f, 11, false, false);
        // cuello de piel
        float[] ny = {0.50f, 0.56f, 0.62f, 0.66f};
        float[] nrx = {0.07f, 0.058f, 0.052f, 0.05f}, nrz = {0.07f, 0.058f, 0.054f, 0.052f};
        b.uvRect(A0, A0, A1, A1);
        b.color(0.95f, 0.9f, 0.9f);
        b.lathe(ny, nrx, nrz, 12, A0, A1, A1, A0, 0f, 1, false, false);
        // cinturón de cuero y bolsas
        b.uvRect(B0, B0, 0.745f, B1);
        b.color(0.95f, 0.9f, 0.85f);
        float[] by = {-0.045f, -0.03f, 0.03f, 0.045f};
        float[] brx = {0.18f, 0.186f, 0.186f, 0.18f}, brz = {0.112f, 0.118f, 0.118f, 0.112f};
        b.lathe(by, brx, brz, 24, B0, 0.745f, B1, B0, 0f, 2, false, false);
        b.color(0.8f, 0.78f, 0.74f);
        for (int i = -1; i <= 1; i += 2) {
            b.push(); b.translate(i * 0.075f, -0.0f, 0.125f); b.rotY(i * -6);
            b.box(0, 0, 0, 0.032f, 0.045f, 0.022f, 1f);
            b.box(0, 0.045f, 0.002f, 0.034f, 0.01f, 0.024f, 1f);
            b.pop();
        }
        // cantimplora (metal)
        b.uvRect(0.755f, B0, B1, B1);
        b.color(0.9f, 0.9f, 0.85f);
        b.push(); b.translate(-0.2f, -0.12f, -0.02f); b.rotZ(-8); b.cylinder(0.055f, 0.055f, 0.14f, 10, true, true, 1f); b.pop();
        // correa cruzada
        b.uvRect(B0, B0, 0.745f, B1);
        b.color(0.9f, 0.85f, 0.8f);
        b.push(); b.translate(0.0f, 0.26f, 0.118f); b.rotZ(-52); b.box(0, 0, 0, 0.26f, 0.025f, 0.012f, 1f); b.pop();
        b.push(); b.translate(0.0f, 0.26f, -0.115f); b.rotZ(52); b.box(0, 0, 0, 0.26f, 0.025f, 0.012f, 1f); b.pop();
        // hombreras y solapa de cuello
        b.uvRect(A0, B0, A1, B1);
        b.color(0.9f, 0.9f, 0.88f);
        for (int i = -1; i <= 1; i += 2) { b.push(); b.translate(i * 0.2f, 0.47f, 0); b.rotZ(-i * 28); b.box(0, 0, 0, 0.06f, 0.012f, 0.075f, 1f); b.pop(); }
        // herida: carne expuesta en el pecho
        b.uvRect(A0, A0, A1, A1);
        b.color(1.15f, 0.55f, 0.5f);
        b.push(); b.translate(0.07f, 0.3f, 0.124f); b.rotZ(-12);
        float[] wy = {-0.05f, -0.03f, 0f, 0.03f, 0.05f}, wrx = {0.0f, 0.05f, 0.065f, 0.05f, 0.0f}, wrz = {0, 0.012f, 0.016f, 0.012f, 0};
        b.push(); b.rotX(90); b.lathe(wy, wrx, wrz, 12, A0, A1, A1, A0, 0f, 3, false, false); b.pop();
        b.pop();
        part[TORSO_COAT] = b.toArray();
    }

    private void buildTorsoRag() {
        MeshBuilder b = new MeshBuilder();
        // torso de piel con costillas marcadas
        int n = 22;
        float[] ys = new float[n], rx = new float[n], rz = new float[n];
        for (int i = 0; i < n; i++) {
            float y = -0.2f + 0.82f * i / (n - 1);
            ys[i] = y;
            float w = baseWidth(y), d = baseDepth(y) ;
            float rib = 0f;
            if (y > 0.1f && y < 0.4f) rib = 0.007f * (float) Math.sin((y - 0.1f) / 0.045f * Math.PI * 2);
            float belly = y < 0.08f ? -0.012f * ss(0.08f, -0.1f, y) : 0f;
            rx[i] = w - 0.012f + rib * 0.8f; rz[i] = d - 0.008f + rib + belly;
        }
        b.uvRect(A0, A0, A1, A1);
        b.color(1, 1, 1);
        b.lathe(ys, rx, rz, 24, A0, A1, A1, A0, 0f, 21, false, false);
        // camisa hecha jirones: solo la parte baja y un hombro
        float[] sy = {-0.3f, -0.22f, -0.1f, 0.02f, 0.12f, 0.2f, 0.27f};
        float[] srx = {0.2f, 0.195f, 0.182f, 0.176f, 0.186f, 0.2f, 0.205f}, srz = {0.122f, 0.12f, 0.112f, 0.108f, 0.114f, 0.122f, 0.12f};
        b.uvRect(A0, B0, A1, B1);
        b.color(0.95f, 0.92f, 0.9f);
        b.latheEx(sy, srx, srz, 24, A0, A1, B1, B0, 0.14f, 0.16f, 22, false, false);
        // cinturón
        b.uvRect(B0, B0, 0.745f, B1);
        b.color(0.9f, 0.86f, 0.8f);
        float[] by = {-0.065f, -0.05f, 0.0f, 0.015f};
        float[] brx = {0.172f, 0.178f, 0.178f, 0.172f}, brz = {0.108f, 0.114f, 0.114f, 0.108f};
        b.lathe(by, brx, brz, 22, B0, 0.745f, B1, B0, 0f, 23, false, false);
        part[TORSO_RAG] = b.toArray();
    }

    private static float baseWidth(float y) {
        float[] ky = {-0.2f, -0.05f, 0.1f, 0.25f, 0.38f, 0.46f, 0.52f, 0.58f, 0.62f};
        float[] kv = {0.178f, 0.17f, 0.165f, 0.195f, 0.225f, 0.2f, 0.13f, 0.07f, 0.055f};
        return interp(ky, kv, y);
    }

    private static float baseDepth(float y) {
        float[] ky = {-0.2f, -0.05f, 0.1f, 0.25f, 0.38f, 0.46f, 0.52f, 0.58f, 0.62f};
        float[] kv = {0.112f, 0.102f, 0.098f, 0.112f, 0.118f, 0.1f, 0.08f, 0.062f, 0.055f};
        return interp(ky, kv, y);
    }

    private static float interp(float[] k, float[] v, float y) {
        if (y <= k[0]) return v[0];
        for (int i = 1; i < k.length; i++) if (y <= k[i]) { float t = (y - k[i - 1]) / (k[i] - k[i - 1]); return v[i - 1] + (v[i] - v[i - 1]) * t; }
        return v[v.length - 1];
    }

    // ------------------------------------------------------------------ cabeza
    private static float[] headRad(float y, float alpha) {
        float t = (y - 0.13f) / 0.13f;
        if (t < -1) t = -1; if (t > 1) t = 1;
        float c = (float) Math.pow(Math.sqrt(Math.max(0f, 1f - t * t)), 0.82f);
        float taper = 0.72f + 0.28f * ss(0.02f, 0.13f, y);
        float aa = Math.abs(alpha);
        float f = 1f;
        f -= 0.17f * gauss(aa, 0.33f, 0.17f) * gauss(y, 0.153f, 0.022f);
        f += 0.055f * gauss(y, 0.185f, 0.013f) * ss(1.2f, 0.5f, aa);
        f += 0.34f * gauss(aa, 0f, 0.11f) * gauss(y, 0.118f, 0.030f);
        f -= 0.10f * gauss(aa, 0.13f, 0.05f) * gauss(y, 0.093f, 0.008f);
        f += 0.07f * gauss(aa, 0.72f, 0.2f) * gauss(y, 0.12f, 0.025f);
        f -= 0.09f * gauss(aa, 0.7f, 0.25f) * gauss(y, 0.08f, 0.025f);
        f -= 0.06f * gauss(aa, 0f, 0.3f) * gauss(y, 0.066f, 0.005f);
        f += 0.04f * gauss(aa, 0f, 0.25f) * (gauss(y, 0.077f, 0.008f) + gauss(y, 0.055f, 0.008f));
        f += 0.10f * gauss(aa, 0f, 0.2f) * gauss(y, 0.015f, 0.02f);
        f += 0.14f * gauss(aa, 1.5f, 0.14f) * gauss(y, 0.14f, 0.025f);
        return new float[]{0.088f * c * taper * f, 0.105f * c * f};
    }

    private float[][][] headGrid(float ph0, float ph1, int rows, int cols) {
        float[][][] g = new float[rows][cols + 1][3];
        for (int i = 0; i < rows; i++) {
            float ph = ph0 + (ph1 - ph0) * i / (rows - 1);
            float y = 0.13f + 0.13f * (float) Math.sin(ph);
            for (int j = 0; j <= cols; j++) {
                float aj = (float) (2 * Math.PI * (j % cols) / cols + Math.PI);
                float alpha = (float) (2 * Math.PI * (j % cols) / cols - Math.PI);
                float[] r = headRad(y, alpha);
                g[i][j][0] = (float) Math.sin(aj) * r[0];
                g[i][j][1] = y;
                g[i][j][2] = (float) Math.cos(aj) * r[1];
            }
        }
        return g;
    }

    private void teeth(MeshBuilder b, float y, float dirY, int seed) {
        b.uvRect(B0, A0, B1, A1);
        for (int i = 0; i < 9; i++) {
            float al = (i - 4) * 0.17f;
            if (Noise.rnd(i, seed, 31) < 0.18f) continue;
            float h = 0.012f + 0.006f * Noise.rnd(i, seed, 32);
            b.color(1.45f, 1.36f, 1.05f);
            b.push();
            b.translate((float) Math.sin(al) * 0.071f, y + dirY * h * 0.5f, (float) Math.cos(al) * 0.086f - 0.004f);
            b.rotY((float) Math.toDegrees(al));
            b.rotZ(Noise.rnd(i, seed, 33) * 8 - 4);
            b.box(0, 0, 0, 0.0055f, h * 0.5f, 0.004f, 1f);
            b.pop();
        }
    }

    private void buildHead() {
        // cráneo y cara
        MeshBuilder b = new MeshBuilder();
        float ph0 = -0.515f, ph1 = (float) (Math.PI / 2);
        float[][][] g = headGrid(ph0, ph1, 28, 34);
        b.color(1, 1, 1);
        float vBot = A0 + (A1 - A0) * (1f - 0.066f / 0.26f);
        b.surface(g, true, B0, B1, vBot, A0, 1f);
        // interior de la boca
        b.uvRect(A0, A0, A1, A1);
        b.color(0.35f, 0.1f, 0.1f);
        b.cap(0.066f, 0.062f, 0.078f, 16, -1f, A0, A1, A0, A1);
        teeth(b, 0.064f, -1f, 3);
        // ojos brillantes dentro de las cuencas
        b.color(2.7f, 2.9f, 0.5f);
        for (int s = -1; s <= 1; s += 2) {
            b.push(); b.translate(s * 0.034f, 0.153f, 0.074f); b.sphere(0.0125f, 8, 6, 0.1f); b.pop();
        }
        // pelo ralo y piel colgando
        b.uvRect(A0, A0, A1, A1);
        b.color(0.5f, 0.45f, 0.45f);
        b.push(); b.translate(-0.07f, 0.24f, -0.02f); b.rotZ(30); b.box(0, -0.02f, 0, 0.004f, 0.03f, 0.03f, 1f); b.pop();
        part[HEAD] = b.toArray();

        // mandíbula inferior móvil
        b = new MeshBuilder();
        float[][][] jg = headGrid(-(float) (Math.PI / 2), -0.462f, 9, 34);
        b.color(1, 1, 1);
        b.surface(jg, true, B0, B1, A1, A0 + (A1 - A0) * (1f - 0.072f / 0.26f), 1f);
        b.uvRect(A0, A0, A1, A1);
        b.color(0.3f, 0.08f, 0.08f);
        float[] r = headRad(0.072f, 1.5f);
        b.cap(0.072f, r[0] * 0.9f, 0.075f, 16, 1f, A0, A1, A0, A1);
        // lengua
        b.color(0.75f, 0.25f, 0.25f);
        b.push(); b.translate(0, 0.066f, 0.02f); b.box(0, 0, 0, 0.026f, 0.006f, 0.04f, 1f); b.pop();
        teeth(b, 0.07f, 1f, 9);
        part[JAW] = b.toArray();
    }

    private void buildHelmet() {
        MeshBuilder b = new MeshBuilder();
        float[] ys = {-0.012f, 0.0f, 0.03f, 0.07f, 0.105f, 0.128f, 0.138f};
        float[] rx = {0.158f, 0.145f, 0.122f, 0.106f, 0.082f, 0.04f, 0.0f};
        float[] rz = {0.176f, 0.162f, 0.136f, 0.118f, 0.094f, 0.05f, 0.0f};
        b.uvRect(0.755f, B0, B1, B1);
        b.color(1, 1, 1);
        b.lathe(ys, rx, rz, 24, 0.755f, B1, B1, B0, 0f, 41, false, false);
        // interior del casco y barboquejo
        b.color(0.4f, 0.4f, 0.38f);
        b.cap(-0.012f, 0.14f, 0.157f, 18, -1f, 0.755f, B1, B1, B0);
        b.uvRect(B0, B0, 0.745f, B1);
        b.color(0.8f, 0.75f, 0.7f);
        for (int s = -1; s <= 1; s += 2) { b.push(); b.translate(s * 0.095f, -0.07f, 0.01f); b.rotZ(s * 6); b.box(0, 0, 0, 0.006f, 0.06f, 0.012f, 1f); b.pop(); }
        part[HELMET] = b.toArray();
    }

    // ------------------------------------------------------------------ brazos
    private void buildArms() {
        MeshBuilder b = new MeshBuilder();
        // manga del uniforme (rasgada en el codo)
        b.uvRect(A0, B0, A1, B1);
        b.color(1, 1, 1);
        float[] sleeve = {0.095f, 0.108f, 0.125f, 0.128f, 0.118f};
        b.limb(0.27f, sleeve, 1.0f, 14, A0, A1, B1, B0, 0.07f, 51);
        part[UARM_COAT] = b.toArray();

        // brazo descubierto (piel con músculo)
        b = new MeshBuilder();
        float[] bare = {0.075f, 0.092f, 0.104f, 0.108f, 0.098f};
        b.color(1, 1, 1);
        b.limb(0.30f, bare, 0.95f, 14, A0, A1, A1, A0, 0f, 52);
        part[UARM_BARE] = b.toArray();

        part[FARM_NEG] = buildForearm(+1);
        part[FARM_POS] = buildForearm(-1);
    }

    /** Antebrazo con mano en garra. thumbSide: lado (x) donde va el pulgar. */
    private float[] buildForearm(int thumbSide) {
        MeshBuilder b = new MeshBuilder();
        b.color(1, 1, 1);
        float[] fore = {0.052f, 0.058f, 0.07f, 0.082f, 0.088f};
        b.limb(0.26f, fore, 1.0f, 12, A0, A1, A1, A0, 0f, 61);
        // manga arremangada: pliegue de tela en el codo
        b.uvRect(A0, B0, A1, B1);
        b.color(0.9f, 0.9f, 0.85f);
        b.push(); b.translate(0, -0.012f, 0);
        float[] cy = {-0.03f, 0.0f, 0.03f, 0.05f}, cr = {0.054f, 0.062f, 0.066f, 0.055f};
        b.latheEx(cy, cr, cr, 12, A0, A1, B1, B0, 0.02f, 0.02f, 62, false, false);
        b.pop();
        b.uvRect(A0, A0, A1, A1);
        // palma
        b.color(0.95f, 0.92f, 0.92f);
        b.push(); b.translate(0, -0.26f, 0);
        float[] palm = {0.058f, 0.084f, 0.088f, 0.08f};
        b.limb(0.085f, palm, 0.42f, 10, A0, A1, A1, A0, 0f, 63);
        // dedos en garra (dos falanges cada uno)
        float[] fx = {-0.032f, -0.011f, 0.011f, 0.032f};
        float[] fl = {0.038f, 0.045f, 0.042f, 0.034f};
        for (int i = 0; i < 4; i++) {
            b.push();
            b.translate(fx[i], -0.085f, 0.0f);
            b.rotX(14 + i % 2 * 5);
            float[] fp = {0.011f, 0.014f, 0.015f};
            b.limb(fl[i], fp, 1f, 6, A0, A1, A1, A0, 0f, 70 + i);
            b.translate(0, -fl[i], 0);
            b.rotX(40);
            float[] fp2 = {0.007f, 0.011f, 0.012f};
            b.limb(fl[i] * 0.85f, fp2, 1f, 6, A0, A1, A1, A0, 0f, 74 + i);
            b.translate(0, -fl[i] * 0.85f, 0);
            b.rotX(32);
            b.color(0.28f, 0.24f, 0.16f);
            float[] fp3 = {0.002f, 0.007f, 0.009f};
            b.limb(0.018f, fp3, 1f, 6, B0, 0.745f, B1, B0, 0f, 78 + i);
            b.color(0.95f, 0.92f, 0.92f);
            b.pop();
        }
        // pulgar hacia el lado interior
        b.push();
        b.translate(thumbSide * 0.04f, -0.03f, 0.0f);
        b.rotZ(-thumbSide * 38);
        b.rotX(10);
        float[] tp = {0.012f, 0.016f, 0.018f};
        b.limb(0.036f, tp, 1f, 6, A0, A1, A1, A0, 0f, 82);
        b.translate(0, -0.036f, 0);
        b.rotX(28);
        float[] tp2 = {0.006f, 0.011f, 0.013f};
        b.limb(0.03f, tp2, 1f, 6, A0, A1, A1, A0, 0f, 83);
        b.pop();
        b.pop();
        return b.toArray();
    }

    // ------------------------------------------------------------------ piernas
    private void buildLegs() {
        MeshBuilder b = new MeshBuilder();
        b.uvRect(A0, B0, A1, B1);
        b.color(0.95f, 0.95f, 0.92f);
        float[] thigh = {0.115f, 0.135f, 0.158f, 0.172f, 0.17f};
        b.limb(0.47f, thigh, 1.05f, 14, A0, A1, B1, B0, 0.05f, 91);
        part[THIGH] = b.toArray();

        b = new MeshBuilder();
        // pantorrilla con tela y botín de cuero
        b.uvRect(A0, B0, A1, B1);
        b.color(0.9f, 0.9f, 0.86f);
        float[] shin = {0.095f, 0.1f, 0.112f, 0.122f, 0.118f};
        b.limb(0.4f, shin, 1.0f, 14, A0, A1, B1, B0, 0.04f, 92);
        // bota
        b.uvRect(B0, B0, 0.745f, B1);
        b.color(0.8f, 0.76f, 0.72f);
        float[] bys = {-0.46f, -0.43f, -0.4f, -0.37f, -0.32f};
        float[] brx = {0.056f, 0.062f, 0.068f, 0.066f, 0.062f};
        b.lathe(bys, brx, brx, 12, B0, 0.745f, B1, B0, 0f, 93, false, false);
        b.push(); b.translate(0, -0.455f, -0.045f); b.rotX(90);
        float[] fy = {0.0f, 0.03f, 0.08f, 0.17f, 0.23f, 0.27f};
        float[] fw = {0.046f, 0.056f, 0.056f, 0.056f, 0.046f, 0.02f};
        float[] fh = {0.04f, 0.052f, 0.048f, 0.04f, 0.034f, 0.016f};
        b.lathe(fy, fw, fh, 12, B0, 0.745f, B1, B0, 0f, 94, true, true);
        b.pop();
        b.color(0.3f, 0.28f, 0.26f);
        b.push(); b.translate(0, -0.489f, 0.1f); b.box(0, 0, 0, 0.056f, 0.008f, 0.145f, 1f); b.pop();
        part[SHIN] = b.toArray();
    }
}
