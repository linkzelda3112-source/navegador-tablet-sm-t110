package com.craterz;

/**
 * Modelos de armas en primera persona (diseños originales inspirados en armas reales de época),
 * con manos de dedos y mangas, y piezas móviles: corredera/cerrojo/bombeo y cargador.
 * Grupos: 0 metal, 1 madera, 2 brazos, 3 corredera (móvil), 4 cargador (móvil), 5 mano de bombeo (móvil).
 */
public final class WeaponModels {
    private WeaponModels() {}
    public static final int GROUPS = 14;
    // grupos extra de la Tormenta: 6 brillo fijo, 7 brillo del cargador, 8-11 arcos eléctricos, 12 núcleo de plasma, 13 letras
    public static final int G_GLOW = 6, G_MAGGLOW = 7, G_ARC0 = 8, N_ARC = 4, G_CORE = 12, G_TEXT = 13;
    private static final float SK0 = 0.02f, SK1 = 0.48f, CL1 = 0.52f, CL2 = 0.98f;

    public static float[][] view(int w) {
        MeshBuilder[] b = new MeshBuilder[GROUPS];
        for (int i = 0; i < GROUPS; i++) b[i] = new MeshBuilder();
        switch (w) {
            case Weapons.PISTOL: pistol(b); break;
            case Weapons.SHOTGUN: shotgun(b); break;
            case Weapons.RIFLE: rifle(b); break;
            case Weapons.SMG: smg(b); break;
            default: storm(b); break;
        }
        float[][] r = new float[GROUPS][];
        for (int i = 0; i < GROUPS; i++) r[i] = b[i].toArray();
        return r;
    }

    /** Desplazamiento hacia atrás (+Z) de la corredera/cerrojo/bombeo según el tiempo desde el disparo. */
    public static float slideBack(int w, float t, float reloadP) {
        float v = 0;
        switch (w) {
            case Weapons.PISTOL: v = 0.045f * (float) Math.exp(-t * 26f); break;
            case Weapons.SMG: v = 0.04f * (float) Math.exp(-t * 30f); break;
            case Weapons.RIFLE: v = t < 0.5f ? 0.07f * (t < 0.08f ? t / 0.08f : (float) Math.exp(-(t - 0.08f) * 7f)) : 0f; break;
            case Weapons.SHOTGUN: {
                float q = t - 0.28f;
                v = q > 0 && q < 0.5f ? 0.095f * (float) Math.sin(Math.PI * q / 0.5f) : 0f;
                break;
            }
            default: v = 0;
        }
        if (reloadP > 0.8f && reloadP < 0.97f && w != Weapons.SHOTGUN && w != Weapons.STORM)
            v = Math.max(v, 0.05f * (float) Math.sin(Math.PI * (reloadP - 0.8f) / 0.17f));
        return v;
    }

    /** Desplazamiento vertical del cargador durante la recarga (p de 0 a 1). */
    public static float magDrop(float p) {
        if (p <= 0) return 0;
        float k;
        if (p < 0.25f) k = p / 0.25f;
        else if (p < 0.55f) k = 1f;
        else if (p < 0.78f) k = 1f - (p - 0.55f) / 0.23f;
        else k = 0f;
        return -0.2f * k * k * (3 - 2 * k);
    }

    public static float[] muzzle(int w) {
        switch (w) {
            case Weapons.PISTOL: return new float[]{0.0f, 0.04f, -0.32f};
            case Weapons.SHOTGUN: return new float[]{0.0f, 0.04f, -0.84f};
            case Weapons.RIFLE: return new float[]{0.0f, 0.035f, -0.84f};
            case Weapons.SMG: return new float[]{0.0f, 0.025f, -0.7f};
            default: return new float[]{0.0f, 0.045f, -0.72f};
        }
    }

    public static float[] hip(int w) {
        switch (w) {
            case Weapons.PISTOL: return new float[]{0.15f, -0.16f, -0.28f};
            case Weapons.SHOTGUN: return new float[]{0.17f, -0.2f, -0.32f};
            case Weapons.RIFLE: return new float[]{0.17f, -0.2f, -0.3f};
            case Weapons.SMG: return new float[]{0.16f, -0.19f, -0.3f};
            default: return new float[]{0.15f, -0.17f, -0.3f};
        }
    }

    // ------------------------------------------------------------ utilidades
    private static void limb(MeshBuilder b, float fx, float fy, float fz, float tx, float ty, float tz, float w0, float w1) {
        b.push();
        float dx = tx - fx, dy = ty - fy, dz = tz - fz;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        b.aim(fx, fy, fz, dx, dy, dz);
        b.taper(len, w0, w0 * 0.9f, w1, w1 * 0.9f, 1f);
        b.pop();
    }

    /** Cilindro a lo largo de -Z que empieza en (x,y,z). */
    private static void bar(MeshBuilder b, float x, float y, float z, float len, float r0, float r1, int segs) {
        b.push(); b.translate(x, y, z); b.rotX(-90); b.cylinder(r0, r1, len, segs, true, true, 0.6f); b.pop();
    }

    private static void sleeve(MeshBuilder a, float wx, float wy, float wz, float fx, float fy, float fz) {
        a.uvRect(SK0, CL1, SK1, CL2);
        a.color(0.62f, 0.66f, 0.5f);
        limb(a, wx, wy, wz, fx, fy, fz, 0.044f, 0.066f);
        // puño con dobladillo
        a.color(0.5f, 0.54f, 0.4f);
        float dx = fx - wx, dy = fy - wy, dz = fz - wz, l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        limb(a, wx + dx / l * 0.002f, wy + dy / l * 0.002f, wz + dz / l * 0.002f, wx + dx / l * 0.03f, wy + dy / l * 0.03f, wz + dz / l * 0.03f, 0.05f, 0.052f);
    }

    /** Mano derecha agarrando una empuñadura inclinada: (x,y,z) = centro de la empuñadura, tilt en grados. */
    private static void gripHand(MeshBuilder a, float x, float y, float z, float tilt, float armX, float armY, float armZ) {
        a.uvRect(SK0, SK0, SK1, SK1);
        a.color(0.66f, 0.52f, 0.44f);
        a.push(); a.translate(x, y, z); a.rotX(tilt);
        a.box(0.016f, 0.0f, 0.026f, 0.014f, 0.036f, 0.017f, 1f);          // palma
        for (int i = 0; i < 4; i++) {                                      // dedos enrollados al frente
            float fy = 0.026f - i * 0.0185f;
            a.box(0.002f, fy, -0.016f, 0.0105f, 0.0085f, 0.012f, 1f);
            a.box(-0.012f, fy, -0.012f, 0.0085f, 0.0078f, 0.0145f, 1f);
        }
        a.push(); a.translate(-0.028f, 0.028f, 0.0f); a.rotY(-14);        // pulgar
        a.box(0, 0, -0.02f, 0.008f, 0.009f, 0.028f, 1f);
        a.box(0, -0.004f, -0.05f, 0.0065f, 0.007f, 0.012f, 1f);
        a.pop();
        a.pop();
        sleeve(a, x + 0.02f, y - 0.025f, z + 0.05f, armX, armY, armZ);
    }

    /** Mano izquierda de apoyo: palma sobre el costado y dedos curvados por debajo del guardamanos. */
    private static void supportHand(MeshBuilder a, float x, float y, float z, float armX, float armY, float armZ) {
        a.uvRect(SK0, SK0, SK1, SK1);
        a.color(0.66f, 0.52f, 0.44f);
        a.push(); a.translate(x, y, z);
        a.box(0.016f, 0.004f, 0.0f, 0.007f, 0.025f, 0.032f, 1f);          // palma lateral
        for (int i = 0; i < 4; i++) {
            float fz = 0.026f - i * 0.0175f;
            a.box(0.006f, -0.032f, fz, 0.017f, 0.0075f, 0.0078f, 1f);      // dedos bajo la pieza
            a.box(-0.009f, -0.026f, fz, 0.0075f, 0.012f, 0.0072f, 1f);
        }
        a.push(); a.translate(-0.012f, 0.024f, 0.03f); a.rotX(12);        // pulgar encima
        a.box(0, 0, -0.016f, 0.008f, 0.008f, 0.026f, 1f);
        a.pop();
        a.pop();
        sleeve(a, x + 0.055f, y - 0.03f, z + 0.04f, armX, armY, armZ);
    }

    private static void sights(MeshBuilder m, float rz, float fz, float y, boolean dot) {
        m.color(0.12f, 0.12f, 0.13f);
        m.box(-0.011f, y, rz, 0.004f, 0.009f, 0.007f, 0.5f);
        m.box(0.011f, y, rz, 0.004f, 0.009f, 0.007f, 0.5f);
        m.box(0, y - 0.0035f, rz, 0.008f, 0.005f, 0.007f, 0.5f);
        m.box(0, y, fz, 0.0028f, 0.011f, 0.004f, 0.5f);
        if (dot) {
            m.color(1.25f, 1.4f, 1.1f);
            m.box(0, y + 0.004f, fz - 0.0042f, 0.0014f, 0.0016f, 0.0005f, 0.5f);
            m.box(-0.011f, y + 0.002f, rz - 0.0072f, 0.0013f, 0.0016f, 0.0005f, 0.5f);
            m.box(0.011f, y + 0.002f, rz - 0.0072f, 0.0013f, 0.0016f, 0.0005f, 0.5f);
        }
    }

    private static void screw(MeshBuilder m, float x, float y, float z, float dir) {
        m.color(0.1f, 0.1f, 0.1f);
        m.box(x, y, z, 0.0025f * Math.abs(dir) + 0.001f, 0.004f, 0.004f, 0.4f);
    }

    // ------------------------------------------------------------ pistola (tipo 1911)
    private static void pistol(MeshBuilder[] g) {
        MeshBuilder m = g[0], wd = g[1], a = g[2], sl = g[3], mg = g[4];
        // armazón
        m.color(0.5f, 0.5f, 0.52f);
        m.box(0, 0.006f, -0.13f, 0.0145f, 0.012f, 0.095f, 0.5f);
        m.push(); m.translate(0, -0.014f, -0.035f); m.rotX(-14); m.box(0, -0.02f, 0, 0.0145f, 0.05f, 0.023f, 0.5f); m.pop(); // cuerpo de empuñadura
        // guardamonte
        m.box(0, -0.016f, -0.115f, 0.004f, 0.004f, 0.045f, 0.5f);
        m.box(0, -0.0105f, -0.16f, 0.004f, 0.01f, 0.005f, 0.5f);
        m.color(0.15f, 0.15f, 0.16f);
        m.box(0, -0.0035f, -0.088f, 0.0025f, 0.013f, 0.004f, 0.5f);       // gatillo
        m.push(); m.translate(0, -0.003f, -0.03f); m.rotX(-14); m.box(0, 0.002f, 0.021f, 0.0125f, 0.05f, 0.004f, 0.5f); m.pop(); // tapa trasera
        m.color(0.18f, 0.18f, 0.2f);
        m.box(0.0155f, 0.015f, -0.06f, 0.002f, 0.004f, 0.018f, 0.5f);     // seguro
        m.box(0.0155f, 0.012f, -0.1f, 0.0018f, 0.005f, 0.01f, 0.5f);      // retén de corredera
        m.box(0, 0.052f, -0.025f, 0.0045f, 0.012f, 0.012f, 0.5f);         // martillo
        m.color(0.35f, 0.35f, 0.37f);
        bar(m, 0, 0.034f, -0.285f, 0.025f, 0.0075f, 0.0075f, 10);         // buje del cañón
        // corredera (móvil)
        sl.color(0.42f, 0.42f, 0.45f);
        sl.box(0, 0.038f, -0.165f, 0.0155f, 0.0175f, 0.125f, 0.5f);
        sl.color(0.1f, 0.1f, 0.11f);
        sl.box(0.0158f, 0.04f, -0.14f, 0.0008f, 0.0065f, 0.026f, 0.4f);   // ventana de expulsión
        sl.box(-0.0158f, 0.04f, -0.14f, 0.0008f, 0.0065f, 0.026f, 0.4f);
        for (int i = 0; i < 6; i++) {                                      // estrías de agarre
            sl.box(0.0158f, 0.038f, -0.063f - i * 0.0085f, 0.0007f, 0.0145f, 0.0017f, 0.4f);
            sl.box(-0.0158f, 0.038f, -0.063f - i * 0.0085f, 0.0007f, 0.0145f, 0.0017f, 0.4f);
        }
        sights(sl, -0.052f, -0.285f, 0.062f, true);
        // empuñadura de nogal con 2 tornillos
        wd.color(0.9f, 0.72f, 0.58f);
        wd.push(); wd.translate(0, -0.014f, -0.035f); wd.rotX(-14);
        wd.box(0.0155f, -0.02f, 0, 0.0025f, 0.047f, 0.02f, 0.5f);
        wd.box(-0.0155f, -0.02f, 0, 0.0025f, 0.047f, 0.02f, 0.5f);
        wd.pop();
        screw(m, 0.0185f, -0.04f, -0.045f, 1); screw(m, -0.0185f, -0.04f, -0.045f, 1);
        // cargador (móvil)
        mg.color(0.22f, 0.22f, 0.24f);
        mg.push(); mg.translate(0, -0.014f, -0.035f); mg.rotX(-14);
        mg.box(0, -0.058f, 0, 0.0135f, 0.045f, 0.0195f, 0.5f);
        mg.color(0.3f, 0.3f, 0.32f);
        mg.box(0, -0.106f, 0.002f, 0.0165f, 0.005f, 0.0225f, 0.5f);       // base
        mg.pop();
        // brazos
        gripHand(a, 0.0f, -0.058f, -0.04f, -14f, 0.14f, -0.28f, 0.25f);
        supportHand(a, -0.026f, -0.09f, -0.07f, -0.16f, -0.3f, 0.22f);
    }

    // ------------------------------------------------------------ escopeta de bombeo
    private static void shotgun(MeshBuilder[] g) {
        MeshBuilder m = g[0], wd = g[1], a = g[2], sl = g[3], mg = g[4], pa = g[5];
        m.color(0.24f, 0.24f, 0.26f);
        bar(m, 0, 0.04f, -0.12f, 0.72f, 0.0125f, 0.0125f, 12);            // cañón
        m.color(0.2f, 0.2f, 0.22f);
        bar(m, 0, 0.004f, -0.12f, 0.52f, 0.0115f, 0.0115f, 12);           // tubo del cargador
        m.color(0.32f, 0.32f, 0.34f);
        bar(m, 0, 0.004f, -0.64f, 0.012f, 0.0145f, 0.0145f, 12);          // tapa del tubo
        m.color(0.18f, 0.18f, 0.19f);
        m.box(0, 0.058f, -0.45f, 0.004f, 0.0018f, 0.3f, 0.4f);            // cinta ventilada
        m.color(0.4f, 0.4f, 0.42f);
        m.box(0, 0.024f, -0.02f, 0.0215f, 0.036f, 0.12f, 0.5f);           // receptor
        m.color(0.1f, 0.1f, 0.11f);
        m.box(0.0218f, 0.032f, -0.04f, 0.0008f, 0.012f, 0.04f, 0.4f);     // ventana de expulsión
        m.color(0.3f, 0.3f, 0.32f);
        m.box(0, -0.008f, 0.0f, 0.014f, 0.005f, 0.07f, 0.5f);
        m.box(0, -0.028f, -0.08f, 0.004f, 0.004f, 0.04f, 0.5f);           // guardamonte
        m.box(0, -0.0165f, -0.12f, 0.004f, 0.012f, 0.005f, 0.5f);
        m.box(0, -0.016f, -0.05f, 0.004f, 0.012f, 0.005f, 0.5f);
        m.color(0.12f, 0.12f, 0.13f);
        m.box(0, -0.014f, -0.075f, 0.0025f, 0.012f, 0.004f, 0.5f);        // gatillo
        m.color(1.3f, 1.2f, 0.9f);
        m.box(0, 0.0565f, -0.8f, 0.003f, 0.004f, 0.003f, 0.4f);           // mira de cuenta
        // bombeo (móvil)
        sl.color(0.27f, 0.25f, 0.24f);
        sl.box(0, 0.002f, -0.4f, 0.0245f, 0.0245f, 0.085f, 0.5f);
        sl.color(0.14f, 0.14f, 0.14f);
        for (int i = 0; i < 9; i++) sl.box(0, -0.0225f, -0.325f - i * 0.0185f, 0.019f, 0.0025f, 0.0045f, 0.4f);
        // madera
        wd.color(0.85f, 0.68f, 0.52f);
        wd.push(); wd.translate(0, -0.012f, 0.27f); wd.rotX(7);
        wd.box(0, 0, 0, 0.0215f, 0.05f, 0.19f, 0.5f);
        wd.pop();
        wd.push(); wd.translate(0, -0.052f, 0.07f); wd.rotX(-20); wd.box(0, 0, 0, 0.0185f, 0.042f, 0.021f, 0.5f); wd.pop();
        wd.color(0.15f, 0.15f, 0.16f);
        wd.push(); wd.translate(0, -0.026f, 0.46f); wd.rotX(7); wd.box(0, 0, 0, 0.0225f, 0.06f, 0.006f, 0.4f); wd.pop();
        // brazos
        gripHand(a, 0.0f, -0.052f, 0.07f, -20f, 0.14f, -0.28f, 0.3f);
        supportHand(pa, -0.02f, -0.0f, -0.4f, -0.2f, -0.22f, -0.02f);
    }

    // ------------------------------------------------------------ fusil de cerrojo (tipo Mk3)
    private static void rifle(MeshBuilder[] g) {
        MeshBuilder m = g[0], wd = g[1], a = g[2], sl = g[3], mg = g[4];
        m.color(0.27f, 0.27f, 0.3f);
        m.box(0, 0.026f, -0.08f, 0.0215f, 0.034f, 0.17f, 0.5f);           // receptor
        bar(m, 0, 0.032f, -0.25f, 0.6f, 0.0105f, 0.0095f, 12);            // cañón
        m.color(0.2f, 0.2f, 0.22f);
        bar(m, 0, 0.032f, -0.83f, 0.014f, 0.0125f, 0.0125f, 12);          // freno de boca
        m.color(0.35f, 0.35f, 0.37f);
        m.box(0, -0.012f, -0.1f, 0.012f, 0.004f, 0.09f, 0.5f);
        m.box(0, -0.032f, -0.07f, 0.004f, 0.004f, 0.045f, 0.5f);          // guardamonte
        m.box(0, -0.02f, -0.115f, 0.004f, 0.012f, 0.005f, 0.5f);
        m.color(0.12f, 0.12f, 0.13f);
        m.box(0, -0.017f, -0.07f, 0.0025f, 0.012f, 0.004f, 0.5f);
        sights(m, 0.06f, -0.79f, 0.072f, false);
        m.color(0.4f, 0.4f, 0.43f);
        m.box(0, 0.077f, 0.0f, 0.007f, 0.003f, 0.02f, 0.5f);              // alza de tangente
        m.color(0.28f, 0.28f, 0.3f);
        m.box(0.0225f, 0.022f, 0.0f, 0.0015f, 0.012f, 0.1f, 0.4f);        // riel del cerrojo
        // cerrojo (móvil): cuerpo y palanca con bola
        sl.color(0.45f, 0.45f, 0.48f);
        sl.box(0, 0.04f, -0.01f, 0.012f, 0.012f, 0.08f, 0.5f);
        sl.push(); sl.translate(0.022f, 0.036f, 0.025f); sl.rotZ(-50);
        sl.box(0.02f, 0, 0, 0.022f, 0.004f, 0.004f, 0.5f);
        sl.translate(0.046f, 0, 0); sl.sphere(0.0085f, 8, 6, 0.5f);
        sl.pop();
        // cargador (móvil), curvo
        mg.color(0.2f, 0.2f, 0.22f);
        mg.push(); mg.translate(0, -0.012f, -0.1f); mg.rotX(-6);
        mg.box(0, -0.05f, 0, 0.0135f, 0.055f, 0.026f, 0.5f);
        mg.color(0.3f, 0.3f, 0.32f);
        mg.box(0, -0.108f, 0.003f, 0.0155f, 0.004f, 0.029f, 0.5f);
        mg.pop();
        // madera: culata, mango, guardamanos largo
        wd.color(0.84f, 0.68f, 0.52f);
        wd.push(); wd.translate(0, -0.012f, 0.3f); wd.rotX(6); wd.box(0, 0, 0, 0.0215f, 0.055f, 0.18f, 0.5f); wd.pop();
        wd.push(); wd.translate(0, 0.0f, 0.15f); wd.rotX(2); wd.box(0, 0.03f, 0, 0.0185f, 0.022f, 0.06f, 0.5f); wd.pop();
        wd.push(); wd.translate(0, -0.052f, 0.06f); wd.rotX(-18); wd.box(0, 0, 0, 0.0185f, 0.045f, 0.02f, 0.5f); wd.pop();
        wd.box(0, 0.014f, -0.5f, 0.0245f, 0.028f, 0.2f, 0.5f);            // guardamanos
        wd.box(0, 0.05f, -0.5f, 0.015f, 0.004f, 0.2f, 0.5f);
        wd.color(0.15f, 0.15f, 0.16f);
        wd.push(); wd.translate(0, -0.027f, 0.485f); wd.rotX(6); wd.box(0, 0, 0, 0.0225f, 0.06f, 0.006f, 0.4f); wd.pop();
        gripHand(a, 0.0f, -0.052f, 0.06f, -18f, 0.14f, -0.28f, 0.3f);
        supportHand(a, -0.026f, -0.005f, -0.45f, -0.22f, -0.22f, -0.05f);
    }

    // ------------------------------------------------------------ subfusil (tipo MP18 con camisa perforada)
    private static void smg(MeshBuilder[] g) {
        MeshBuilder m = g[0], wd = g[1], a = g[2], sl = g[3], mg = g[4];
        m.color(0.26f, 0.27f, 0.29f);
        m.box(0, 0.02f, -0.06f, 0.0245f, 0.03f, 0.12f, 0.5f);             // receptor
        bar(m, 0, 0.026f, -0.18f, 0.5f, 0.0235f, 0.0235f, 14);            // camisa del cañón
        m.color(0.07f, 0.07f, 0.08f);
        for (int r = 0; r < 3; r++)                                        // perforaciones
            for (int i = 0; i < 9; i++) {
                float ang = (r - 1) * 55f;
                m.push(); m.translate(0, 0.026f, 0); m.rotZ(ang);
                m.box(0, 0.0238f, -0.2f - i * 0.05f, 0.005f, 0.0012f, 0.011f, 0.4f);
                m.pop();
            }
        m.color(0.34f, 0.34f, 0.36f);
        bar(m, 0, 0.026f, -0.68f, 0.03f, 0.011f, 0.011f, 12);             // boca
        m.box(0, -0.014f, -0.06f, 0.014f, 0.004f, 0.08f, 0.5f);
        m.box(0, -0.03f, -0.055f, 0.004f, 0.004f, 0.05f, 0.5f);           // guardamonte
        m.color(0.12f, 0.12f, 0.13f);
        m.box(0, -0.017f, -0.06f, 0.0025f, 0.012f, 0.004f, 0.5f);
        sights(m, 0.015f, -0.66f, 0.06f, false);
        m.color(0.3f, 0.3f, 0.32f);
        m.box(0, 0.052f, -0.06f, 0.016f, 0.003f, 0.08f, 0.5f);            // tapa superior
        // palanca de armado (móvil) en el lado derecho
        sl.color(0.42f, 0.42f, 0.45f);
        sl.box(0.028f, 0.026f, -0.02f, 0.004f, 0.005f, 0.02f, 0.5f);
        sl.box(0.034f, 0.026f, -0.0f, 0.007f, 0.007f, 0.007f, 0.5f);
        // cargador tipo caracol a la izquierda, bajo el cañón (móvil)
        mg.color(0.3f, 0.31f, 0.33f);
        mg.push(); mg.translate(-0.05f, -0.032f, -0.09f); mg.rotZ(90);
        mg.cylinder(0.064f, 0.064f, 0.034f, 18, true, true, 0.5f);
        mg.color(0.18f, 0.18f, 0.2f);
        mg.translate(0, 0.034f, 0); mg.cylinder(0.04f, 0.04f, 0.004f, 14, true, true, 0.5f);
        mg.pop();
        mg.color(0.24f, 0.25f, 0.27f);
        mg.box(-0.032f, 0.004f, -0.09f, 0.014f, 0.022f, 0.02f, 0.5f);     // cuello al receptor
        // madera
        wd.color(0.84f, 0.68f, 0.52f);
        wd.push(); wd.translate(0, -0.01f, 0.24f); wd.rotX(5); wd.box(0, 0, 0, 0.0185f, 0.043f, 0.14f, 0.5f); wd.pop();
        wd.push(); wd.translate(0, -0.012f, 0.09f); wd.box(0, 0.015f, 0, 0.0185f, 0.03f, 0.04f, 0.5f); wd.pop();
        wd.push(); wd.translate(0, -0.05f, 0.04f); wd.rotX(-16); wd.box(0, 0, 0, 0.0175f, 0.04f, 0.02f, 0.5f); wd.pop();
        wd.color(0.15f, 0.15f, 0.16f);
        wd.push(); wd.translate(0, -0.012f, 0.385f); wd.rotX(5); wd.box(0, 0, 0, 0.0195f, 0.045f, 0.005f, 0.4f); wd.pop();
        gripHand(a, 0.0f, -0.05f, 0.04f, -16f, 0.14f, -0.28f, 0.3f);
        supportHand(a, -0.018f, -0.01f, -0.3f, -0.2f, -0.22f, 0.05f);
    }

    // ------------------------------------------------------------ TR-74 "Tempest" (Tormenta, diseño original)
    private static final float[] BRONZE = {0.95f, 0.62f, 0.34f};
    private static final float[] CY_HI = {2.0f, 2.8f, 3.0f}, CY_HALO = {1.0f, 1.7f, 2.3f}, CY_LAMP = {1.0f, 2.1f, 2.6f};

    private static void glowRect(MeshBuilder b) { b.uvRect(0.92f, 0.02f, 0.99f, 0.08f); }

    private static void arc(MeshBuilder b, java.util.Random r, float[] p0, float[] p1, int segs, float jit, float wid) {
        float[] pr = p0.clone();
        for (int i = 1; i <= segs; i++) {
            float u = i / (float) segs;
            float[] pn = new float[3];
            for (int k = 0; k < 3; k++) pn[k] = p0[k] + (p1[k] - p0[k]) * u + (i < segs ? (r.nextFloat() - 0.5f) * jit * (k == 0 ? 0.4f : 1f) : 0f);
            b.color(CY_HALO[0], CY_HALO[1], CY_HALO[2]);
            limb(b, pr[0], pr[1], pr[2], pn[0], pn[1], pn[2], wid * 3f, wid * 3f);
            b.color(CY_HI[0], CY_HI[1], CY_HI[2]);
            limb(b, pr[0], pr[1], pr[2], pn[0], pn[1], pn[2], wid, wid);
            pr = pn;
        }
    }

    private static void storm(MeshBuilder[] g) {
        MeshBuilder m = g[0], a = g[2], sl = g[3], mg = g[4], gl = g[G_GLOW], mgl = g[G_MAGGLOW], core = g[G_CORE], tx = g[G_TEXT];
        glowRect(gl); glowRect(mgl); glowRect(core);
        final float Y = 0.045f; // eje del cañón
        // ---- freno de boca con ranuras y aro de bronce
        m.color(0.84f, 0.89f, 0.94f);
        bar(m, 0, Y, -0.71f, 0.125f, 0.034f, 0.034f, 24);
        m.color(BRONZE[0], BRONZE[1], BRONZE[2]);
        bar(m, 0, Y, -0.715f, 0.012f, 0.0365f, 0.0365f, 24);
        m.color(0.03f, 0.03f, 0.03f);
        bar(m, 0, Y, -0.7165f, 0.003f, 0.021f, 0.021f, 14);               // ánima
        for (int i = 0; i < 3; i++)                                         // ranuras laterales del freno
            for (int s = -1; s <= 1; s += 2) m.bbox(s * 0.0335f, Y, -0.665f + i * 0.026f, 0.003f, 0.019f, 0.0075f, 0.4f);
        for (int s = -1; s <= 1; s += 2) m.bbox(0, Y + s * 0.0335f, -0.652f, 0.012f, 0.003f, 0.036f, 0.4f);
        // ---- cuello acanalado
        m.color(0.46f, 0.5f, 0.57f);
        bar(m, 0, Y, -0.585f, 0.09f, 0.0225f, 0.0225f, 12);
        m.color(0.74f, 0.78f, 0.84f);
        for (int i = 0; i < 5; i++) bar(m, 0, Y, -0.575f + i * 0.015f, 0.007f, 0.0265f, 0.0265f, 12);
        // ---- carcasa hexagonal frontal con vivos de bronce
        m.color(0.74f, 0.81f, 0.92f);
        bar(m, 0, Y, -0.5f, 0.135f, 0.06f, 0.064f, 6);
        m.color(BRONZE[0], BRONZE[1], BRONZE[2]);
        bar(m, 0, Y, -0.505f, 0.009f, 0.0625f, 0.0625f, 6);
        bar(m, 0, Y, -0.375f, 0.007f, 0.0665f, 0.0665f, 6);
        // ---- cuerpo principal
        m.color(0.78f, 0.86f, 0.97f);
        m.bbox(0, 0.052f, -0.115f, 0.0385f, 0.052f, 0.255f, 0.5f);
        m.color(0.62f, 0.68f, 0.77f);
        m.bbox(0, 0.108f, -0.115f, 0.03f, 0.006f, 0.255f, 0.5f);           // tapa superior
        m.color(0.41f, 0.45f, 0.51f);
        for (int s = -1; s <= 1; s += 2) {
            m.bbox(s * 0.0392f, 0.052f, -0.3f, 0.0012f, 0.048f, 0.075f, 0.5f);   // placas en relieve
            m.bbox(s * 0.0392f, 0.095f, -0.04f, 0.0012f, 0.01f, 0.1f, 0.5f);
            m.bbox(s * 0.0392f, 0.021f, 0.03f, 0.0012f, 0.012f, 0.1f, 0.5f);
        }
        // marco del núcleo de plasma
        m.color(0.27f, 0.3f, 0.35f);
        for (int s = -1; s <= 1; s += 2) {
            m.bbox(s * 0.0388f, 0.092f, -0.1f, 0.0016f, 0.004f, 0.105f, 0.4f);
            m.bbox(s * 0.0388f, 0.024f, -0.1f, 0.0016f, 0.004f, 0.105f, 0.4f);
            m.bbox(s * 0.0388f, 0.058f, -0.2f, 0.0016f, 0.034f, 0.004f, 0.4f);
            m.bbox(s * 0.0388f, 0.058f, 0.0f, 0.0016f, 0.034f, 0.004f, 0.4f);
        }
        // riel superior con dientes
        m.color(0.43f, 0.47f, 0.54f);
        m.bbox(0, 0.118f, -0.1f, 0.019f, 0.004f, 0.255f, 0.5f);
        m.color(0.16f, 0.18f, 0.2f);
        for (int i = 0; i < 20; i++) m.bbox(0, 0.1215f, -0.33f + i * 0.0245f, 0.0175f, 0.0025f, 0.0075f, 0.4f);
        // ---- mira holográfica
        m.color(0.7f, 0.78f, 0.89f);
        m.bbox(0, 0.131f, -0.075f, 0.03f, 0.01f, 0.068f, 0.5f);            // base
        m.color(0.84f, 0.92f, 1.0f);
        m.bbox(0, 0.188f, -0.075f, 0.034f, 0.008f, 0.07f, 0.5f);           // visera
        m.bbox(0, 0.168f, -0.138f, 0.034f, 0.026f, 0.006f, 0.5f);          // marco frontal
        m.bbox(-0.0285f, 0.158f, -0.075f, 0.0055f, 0.03f, 0.07f, 0.5f);    // costados
        m.bbox(0.0285f, 0.158f, -0.075f, 0.0055f, 0.03f, 0.07f, 0.5f);
        m.color(0.41f, 0.45f, 0.51f);
        m.bbox(0, 0.138f, -0.01f, 0.034f, 0.007f, 0.005f, 0.5f);           // marco inferior trasero
        m.bbox(-0.0295f, 0.162f, -0.01f, 0.005f, 0.03f, 0.005f, 0.5f);
        m.bbox(0.0295f, 0.162f, -0.01f, 0.005f, 0.03f, 0.005f, 0.5f);
        m.bbox(0, 0.19f, -0.01f, 0.034f, 0.005f, 0.005f, 0.5f);
        m.color(0.2f, 0.22f, 0.24f);
        m.bbox(0, 0.128f, 0.075f, 0.007f, 0.012f, 0.006f, 0.4f);           // alza de emergencia
        m.bbox(0, 0.126f, -0.34f, 0.0035f, 0.014f, 0.006f, 0.4f);          // punto de mira
        // luces de la mira + retícula
        gl.color(CY_LAMP[0], CY_LAMP[1], CY_LAMP[2]);
        for (int s = -1; s <= 1; s += 2) gl.box(s * 0.0343f, 0.158f, -0.075f, 0.0008f, 0.0035f, 0.028f, 0.4f);
        for (int i = 0; i < 12; i++) {                                      // anillo de la retícula (mira hacia atrás)
            float an = i * 6.2832f / 12f;
            gl.box((float) Math.cos(an) * 0.0105f, 0.158f + (float) Math.sin(an) * 0.0105f, -0.0135f, 0.0014f, 0.0014f, 0.0006f, 0.4f);
        }
        gl.box(0, 0.158f, -0.0135f, 0.0017f, 0.0017f, 0.0006f, 0.4f);
        // ---- ranuras de ventilación y luces laterales
        for (int s = -1; s <= 1; s += 2) {
            gl.color(CY_LAMP[0], CY_LAMP[1], CY_LAMP[2]);
            for (int i = 0; i < 4; i++) gl.box(s * 0.0394f, 0.093f, -0.355f + i * 0.04f, 0.0008f, 0.0045f, 0.015f, 0.4f);
            for (int i = 0; i < 2; i++) gl.box(s * 0.0394f, 0.044f, -0.34f + i * 0.055f, 0.0008f, 0.0065f, 0.02f, 0.4f);
            gl.box(s * 0.0394f, 0.022f, 0.03f, 0.0008f, 0.0035f, 0.05f, 0.4f);
            gl.box(s * 0.0394f, 0.1f, 0.075f, 0.0008f, 0.003f, 0.024f, 0.4f);
        }
        // ---- núcleo de plasma (pulsa) con arcos internos
        core.color(0.55f, 1.5f, 2.1f);
        for (int s = -1; s <= 1; s += 2) {
            core.box(s * 0.0394f, 0.058f, -0.1f, 0.0007f, 0.03f, 0.095f, 0.4f);
            core.color(1.4f, 2.4f, 2.8f);
            core.box(s * 0.0398f, 0.058f, -0.1f, 0.0006f, 0.016f, 0.05f, 0.4f);
            core.color(0.55f, 1.5f, 2.1f);
        }
        java.util.Random cr = new java.util.Random(77);
        for (int s = -1; s <= 1; s += 2)
            for (int i = 0; i < 4; i++) {
                float[] p0 = {s * 0.0402f, 0.058f + (cr.nextFloat() - 0.5f) * 0.03f, -0.1f + (cr.nextFloat() - 0.5f) * 0.09f};
                float[] p1 = {s * 0.0402f, 0.058f + (cr.nextFloat() - 0.5f) * 0.05f, -0.1f + (cr.nextFloat() - 0.5f) * 0.18f};
                arc(core, cr, p0, p1, 4, 0.02f, 0.0012f);
            }
        // ---- letras: TR-74 TEMPEST en el receptor y TORMENTA sobre el núcleo
        for (int s = -1; s <= 1; s += 2) {
            tx.color(1.35f, 1.35f, 1.35f);
            tx.uvRect(0f, 0.855f, 1f, 0.985f);
            tx.box(s * 0.0394f, 0.108f * 0.0f + 0.103f, 0.075f, 0.0007f, 0.0082f, 0.062f, 0.4f);
            tx.uvRect(0f, 0.7f, 1f, 0.85f);
            tx.box(s * 0.0401f, 0.058f, -0.1f, 0.0004f, 0.0085f, 0.044f, 0.4f);
        }
        // ---- guardamanos inferior de goma con estrías y vivos de bronce
        m.color(0.38f, 0.39f, 0.43f);
        m.bbox(0, -0.032f, -0.34f, 0.032f, 0.03f, 0.145f, 0.5f);
        m.bbox(0, -0.028f, -0.5f, 0.028f, 0.024f, 0.02f, 0.5f);
        m.color(0.16f, 0.16f, 0.19f);
        for (int i = 0; i < 7; i++) m.bbox(0, -0.0625f, -0.22f - i * 0.036f, 0.027f, 0.0025f, 0.009f, 0.4f);
        m.color(BRONZE[0], BRONZE[1], BRONZE[2]);
        m.bbox(0, -0.032f, -0.488f, 0.033f, 0.031f, 0.004f, 0.4f);
        m.bbox(0, -0.0035f, -0.34f, 0.033f, 0.0015f, 0.143f, 0.4f);
        // cable de energía hacia el guardamanos
        m.color(0.09f, 0.09f, 0.11f);
        limb(m, -0.03f, -0.012f, -0.11f, -0.037f, -0.03f, -0.2f, 0.011f, 0.011f);
        limb(m, -0.037f, -0.03f, -0.2f, -0.034f, -0.035f, -0.25f, 0.011f, 0.011f);
        // ---- receptor inferior, guardamonte y empuñadura
        m.color(0.46f, 0.5f, 0.57f);
        m.bbox(0, -0.035f, 0.02f, 0.034f, 0.035f, 0.12f, 0.5f);
        m.color(0.27f, 0.3f, 0.34f);
        m.bbox(0, -0.062f, 0.075f, 0.01f, 0.004f, 0.045f, 0.5f);
        m.bbox(0, -0.082f, 0.108f, 0.006f, 0.02f, 0.004f, 0.5f);
        m.color(0.09f, 0.09f, 0.11f);
        m.bbox(0, -0.062f, 0.034f, 0.0025f, 0.012f, 0.004f, 0.5f);          // gatillo
        m.color(0.16f, 0.16f, 0.19f);
        m.push(); m.translate(0, -0.06f, 0.1f); m.rotX(-20);
        m.bbox(0, -0.052f, 0, 0.0225f, 0.058f, 0.0235f, 0.5f);              // empuñadura
        for (int i = 0; i < 5; i++) m.bbox(0, -0.035f - i * 0.019f, -0.0245f, 0.021f, 0.0045f, 0.0012f, 0.4f);
        m.pop();
        m.color(BRONZE[0], BRONZE[1], BRONZE[2]);
        m.bbox(0, -0.065f, 0.08f, 0.0062f, 0.0045f, 0.0045f, 0.4f);
        // ---- culata esquelética angular
        m.color(0.46f, 0.5f, 0.57f);
        m.bbox(0, 0.062f, 0.235f, 0.0285f, 0.04f, 0.11f, 0.5f);             // carrillera superior
        m.color(0.62f, 0.68f, 0.76f);
        m.bbox(0, 0.104f, 0.23f, 0.0235f, 0.004f, 0.1f, 0.5f);
        m.color(0.38f, 0.41f, 0.46f);
        m.push(); m.translate(0, 0.045f, 0.35f); m.rotX(-6); m.bbox(0, -0.04f, 0, 0.029f, 0.085f, 0.0125f, 0.5f); m.pop(); // placa trasera
        limb(m, 0, -0.015f, 0.145f, 0, -0.068f, 0.345f, 0.03f, 0.036f);    // puntal inferior
        m.color(BRONZE[0], BRONZE[1], BRONZE[2]);
        m.bbox(0, -0.036f, 0.358f, 0.03f, 0.0065f, 0.0048f, 0.4f);
        m.bbox(0, 0.046f, 0.355f, 0.03f, 0.004f, 0.005f, 0.4f);
        // ---- manija de carga (móvil) a la derecha del receptor
        sl.color(0.7f, 0.74f, 0.8f);
        sl.bbox(0.0455f, 0.082f, 0.03f, 0.0075f, 0.007f, 0.022f, 0.5f);
        sl.color(0.2f, 0.22f, 0.25f);
        for (int i = 0; i < 4; i++) sl.bbox(0.0535f, 0.082f, 0.012f + i * 0.012f, 0.0012f, 0.006f, 0.004f, 0.4f);
        // ---- cargador (móvil), vertical, con franjas luminosas
        mg.color(0.36f, 0.4f, 0.45f);
        mg.bbox(0, -0.133f, -0.05f, 0.0235f, 0.065f, 0.04f, 0.5f);
        mg.color(0.2f, 0.22f, 0.25f);
        mg.bbox(0, -0.2f, -0.05f, 0.0275f, 0.008f, 0.0445f, 0.5f);
        mg.color(BRONZE[0], BRONZE[1], BRONZE[2]);
        mg.bbox(0, -0.2085f, -0.05f, 0.028f, 0.0015f, 0.045f, 0.4f);
        mgl.color(CY_LAMP[0], CY_LAMP[1], CY_LAMP[2]);
        for (int s = -1; s <= 1; s += 2) mgl.box(s * 0.0239f, -0.135f, -0.045f, 0.0008f, 0.055f, 0.0065f, 0.4f);
        // ---- arcos eléctricos (cuatro variantes que se alternan)
        for (int v = 0; v < N_ARC; v++) {
            MeshBuilder ar = g[G_ARC0 + v];
            glowRect(ar);
            java.util.Random r = new java.util.Random(1000 + v * 31);
            for (int s = -1; s <= 1; s += 2) {
                float x = s * 0.043f;
                arc(ar, r, new float[]{x, 0.075f, -0.69f}, new float[]{x, 0.03f, 0.40f}, 11, 0.045f, 0.0016f);       // de la boca a la culata y más allá
                arc(ar, r, new float[]{x, 0.095f, -0.33f}, new float[]{x, 0.05f, -0.12f}, 5, 0.04f, 0.0014f);
                arc(ar, r, new float[]{x, 0.02f, -0.45f}, new float[]{x, 0.075f, -0.55f}, 4, 0.035f, 0.0013f);
            }
            arc(ar, r, new float[]{0.0f, 0.2f, -0.12f}, new float[]{0.0f, 0.125f, -0.3f}, 4, 0.04f, 0.0013f);        // sobre la mira
            arc(ar, r, new float[]{-0.03f, -0.1f, -0.07f}, new float[]{-0.04f, -0.02f, 0.0f}, 3, 0.03f, 0.0013f);   // cargador
            arc(ar, r, new float[]{0.0f, 0.05f, -0.72f}, new float[]{(r.nextFloat() - 0.5f) * 0.1f, 0.05f + (r.nextFloat() - 0.5f) * 0.1f, -0.95f}, 4, 0.05f, 0.0016f); // salto al frente
            arc(ar, r, new float[]{0.0f, 0.03f, 0.36f}, new float[]{0.0f, -0.03f + r.nextFloat() * 0.05f, 0.55f}, 4, 0.06f, 0.0016f);  // cola tras la culata
        }
        gripHand(a, 0.0f, -0.11f, 0.095f, -20f, 0.14f, -0.28f, 0.33f);
        supportHand(a, -0.04f, -0.035f, -0.34f, -0.22f, -0.24f, -0.02f);
    }

    /** Silueta de tiza (plano, en el plano XY, mirando a +Z) para comprar en la pared. */
    public static void chalk(MeshBuilder b, int w) {
        float t = 0.012f;
        switch (w) {
            case Weapons.SHOTGUN:
                b.box(0.25f, 0.1f, 0, 1.0f, 0.045f, t, 0.5f); b.box(0.35f, -0.02f, 0, 0.75f, 0.03f, t, 0.5f);
                b.push(); b.translate(-1.2f, -0.12f, 0); b.rotZ(-12); b.box(0, 0, 0, 0.35f, 0.1f, t, 0.5f); b.pop();
                b.box(-0.55f, -0.04f, 0, 0.18f, 0.07f, t, 0.5f); break;
            case Weapons.RIFLE:
                b.box(0.3f, 0.1f, 0, 1.05f, 0.045f, t, 0.5f); b.box(-0.45f, 0.0f, 0, 0.5f, 0.1f, t, 0.5f);
                b.push(); b.translate(-1.25f, -0.06f, 0); b.rotZ(-8); b.box(0, 0, 0, 0.4f, 0.11f, t, 0.5f); b.pop();
                b.push(); b.translate(-0.35f, -0.28f, 0); b.rotZ(-12); b.box(0, 0, 0, 0.06f, 0.2f, t, 0.5f); b.pop(); break;
            case Weapons.SMG:
                b.box(0.1f, 0.1f, 0, 0.75f, 0.09f, t, 0.5f); b.box(0.8f, 0.12f, 0, 0.35f, 0.04f, t, 0.5f);
                b.push(); b.translate(-0.2f, -0.2f, 0); b.rotZ(-8); b.box(0, 0, 0, 0.08f, 0.22f, t, 0.5f); b.pop();
                b.box(-0.85f, 0.05f, 0, 0.25f, 0.06f, t, 0.5f); break;
            default:
                b.box(0.1f, 0.1f, 0, 0.55f, 0.09f, t, 0.5f);
                b.push(); b.translate(-0.3f, -0.2f, 0); b.rotZ(-14); b.box(0, 0, 0, 0.1f, 0.25f, t, 0.5f); b.pop(); break;
        }
    }
}
