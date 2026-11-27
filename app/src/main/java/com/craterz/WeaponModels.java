package com.craterz;

/** Modelos de armas en primera persona (con manos y mangas) y siluetas de tiza para las paredes. */
public final class WeaponModels {
    private WeaponModels() {}

    /** Devuelve {metal, madera, brazos} ya como arreglos de vértices. */
    public static float[][] view(int w) {
        MeshBuilder metal = new MeshBuilder(), wood = new MeshBuilder(), arms = new MeshBuilder();
        switch (w) {
            case Weapons.PISTOL: pistol(metal, wood, arms); break;
            case Weapons.SHOTGUN: shotgun(metal, wood, arms); break;
            case Weapons.RIFLE: rifle(metal, wood, arms); break;
            case Weapons.SMG: smg(metal, wood, arms); break;
            default: storm(metal, wood, arms); break;
        }
        return new float[][]{metal.toArray(), wood.toArray(), arms.toArray()};
    }

    /** Posición de la boca del arma en el espacio de la cámara. */
    public static float[] muzzle(int w) {
        switch (w) {
            case Weapons.PISTOL: return new float[]{0.0f, 0.035f, -0.31f};
            case Weapons.SHOTGUN: return new float[]{0.0f, 0.03f, -0.82f};
            case Weapons.RIFLE: return new float[]{0.0f, 0.03f, -0.78f};
            case Weapons.SMG: return new float[]{0.0f, 0.02f, -0.62f};
            default: return new float[]{0.0f, 0.03f, -0.4f};
        }
    }

    /** Offset de la cadera por arma: dónde se sostiene respecto a la cámara. */
    public static float[] hip(int w) {
        switch (w) {
            case Weapons.PISTOL: return new float[]{0.15f, -0.16f, -0.28f};
            case Weapons.SHOTGUN: return new float[]{0.17f, -0.2f, -0.32f};
            case Weapons.RIFLE: return new float[]{0.17f, -0.2f, -0.3f};
            case Weapons.SMG: return new float[]{0.16f, -0.19f, -0.3f};
            default: return new float[]{0.15f, -0.17f, -0.3f};
        }
    }

    private static void limb(MeshBuilder b, float fx, float fy, float fz, float tx, float ty, float tz, float w0, float w1) {
        b.push();
        float dx = tx - fx, dy = ty - fy, dz = tz - fz;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        b.aim(fx, fy, fz, dx, dy, dz);
        b.taper(len, w0, w0, w1, w1, 1f);
        b.pop();
    }

    private static void hand(MeshBuilder a, float x, float y, float z, float fx, float fy, float fz) {
        a.uvRect(0.02f, 0.02f, 0.5f, 0.48f);
        a.color(0.55f, 0.45f, 0.38f);
        a.push(); a.translate(x, y, z); a.box(0, 0, 0, 0.026f, 0.03f, 0.04f, 1f); a.pop();
        a.uvRect(0.02f, 0.52f, 0.5f, 0.98f);
        a.color(0.8f, 0.85f, 0.7f);
        limb(a, x, y + 0.01f, z + 0.03f, fx, fy, fz, 0.052f, 0.075f);
    }

    private static void pistol(MeshBuilder m, MeshBuilder wd, MeshBuilder a) {
        m.color(0.55f, 0.55f, 0.58f);
        m.box(0, 0.035f, -0.17f, 0.017f, 0.021f, 0.125f, 0.5f);
        m.color(0.35f, 0.35f, 0.38f);
        m.box(0, 0.005f, -0.14f, 0.016f, 0.016f, 0.1f, 0.5f);
        m.box(0, 0.065f, -0.285f, 0.004f, 0.01f, 0.004f, 0.5f);
        m.box(0, 0.065f, -0.06f, 0.012f, 0.008f, 0.008f, 0.5f);
        m.push(); m.translate(0, -0.075f, -0.045f); m.rotX(-14);
        m.color(0.3f, 0.3f, 0.32f);
        m.box(0, 0, 0, 0.017f, 0.052f, 0.026f, 0.5f);
        m.pop();
        m.box(0, -0.02f, -0.095f, 0.003f, 0.012f, 0.03f, 0.5f);
        wd.color(0.8f, 0.7f, 0.6f);
        wd.push(); wd.translate(0, -0.075f, -0.045f); wd.rotX(-14);
        wd.box(0.0175f, 0, 0, 0.003f, 0.048f, 0.022f, 0.5f);
        wd.box(-0.0175f, 0, 0, 0.003f, 0.048f, 0.022f, 0.5f);
        wd.pop();
        hand(a, 0.0f, -0.07f, -0.03f, 0.14f, -0.28f, 0.25f);
        hand(a, -0.012f, -0.1f, -0.045f, -0.12f, -0.3f, 0.25f);
    }

    private static void shotgun(MeshBuilder m, MeshBuilder wd, MeshBuilder a) {
        m.color(0.28f, 0.28f, 0.3f);
        m.push(); m.translate(0, 0.035f, -0.1f); m.rotX(-90); m.cylinder(0.014f, 0.014f, 0.7f, 8, true, true, 0.6f); m.pop();
        m.color(0.22f, 0.22f, 0.24f);
        m.push(); m.translate(0, 0.0f, -0.1f); m.rotX(-90); m.cylinder(0.013f, 0.013f, 0.5f, 8, true, true, 0.6f); m.pop();
        m.color(0.4f, 0.4f, 0.42f);
        m.box(0, 0.015f, -0.02f, 0.021f, 0.035f, 0.13f, 0.5f);
        m.box(0, 0.052f, -0.74f, 0.004f, 0.008f, 0.004f, 0.5f);
        wd.color(0.9f, 0.75f, 0.6f);
        wd.box(0, -0.006f, -0.36f, 0.028f, 0.024f, 0.1f, 0.5f);
        wd.push(); wd.translate(0, -0.04f, 0.3f); wd.rotX(6);
        wd.box(0, 0, 0, 0.021f, 0.05f, 0.17f, 0.5f);
        wd.pop();
        wd.push(); wd.translate(0, -0.06f, 0.06f); wd.rotX(-16); wd.box(0, 0, 0, 0.02f, 0.05f, 0.025f, 0.5f); wd.pop();
        hand(a, 0.0f, -0.07f, 0.06f, 0.14f, -0.28f, 0.3f);
        hand(a, 0.0f, -0.03f, -0.36f, -0.2f, -0.2f, -0.02f);
    }

    private static void rifle(MeshBuilder m, MeshBuilder wd, MeshBuilder a) {
        m.color(0.3f, 0.3f, 0.33f);
        m.box(0, 0.02f, -0.1f, 0.02f, 0.034f, 0.2f, 0.5f);
        m.push(); m.translate(0, 0.03f, -0.3f); m.rotX(-90); m.cylinder(0.011f, 0.011f, 0.52f, 8, true, true, 0.6f); m.pop();
        m.color(0.22f, 0.22f, 0.25f);
        m.push(); m.translate(0, -0.1f, -0.13f); m.rotX(-12); m.box(0, 0, 0, 0.014f, 0.065f, 0.026f, 0.5f); m.pop();
        m.color(0.45f, 0.45f, 0.48f);
        m.box(0, 0.062f, -0.02f, 0.006f, 0.014f, 0.006f, 0.5f);
        m.box(0, 0.06f, -0.72f, 0.004f, 0.012f, 0.004f, 0.5f);
        m.box(0, 0.06f, 0.1f, 0.012f, 0.008f, 0.012f, 0.5f);
        wd.color(0.85f, 0.7f, 0.55f);
        wd.box(0, 0.012f, -0.46f, 0.026f, 0.028f, 0.15f, 0.5f);
        wd.push(); wd.translate(0, -0.02f, 0.3f); wd.rotX(5); wd.box(0, 0, 0, 0.022f, 0.052f, 0.17f, 0.5f); wd.pop();
        wd.push(); wd.translate(0, -0.06f, 0.06f); wd.rotX(-16); wd.box(0, 0, 0, 0.02f, 0.05f, 0.024f, 0.5f); wd.pop();
        hand(a, 0.0f, -0.07f, 0.06f, 0.14f, -0.28f, 0.3f);
        hand(a, 0.0f, -0.0f, -0.42f, -0.22f, -0.22f, -0.05f);
    }

    private static void smg(MeshBuilder m, MeshBuilder wd, MeshBuilder a) {
        m.color(0.28f, 0.29f, 0.3f);
        m.box(0, 0.015f, -0.1f, 0.026f, 0.035f, 0.17f, 0.5f);
        m.push(); m.translate(0, 0.02f, -0.27f); m.rotX(-90); m.cylinder(0.02f, 0.02f, 0.3f, 8, true, true, 0.6f); m.pop();
        m.color(0.12f, 0.12f, 0.13f);
        for (int i = 0; i < 4; i++) m.box(0, 0.04f, -0.32f - i * 0.065f, 0.021f, 0.002f, 0.012f, 0.5f);
        m.color(0.35f, 0.35f, 0.38f);
        m.push(); m.translate(0, -0.08f, -0.1f); m.rotZ(90); m.cylinder(0.062f, 0.062f, 0.06f, 12, true, true, 0.5f); m.translate(-0.0f, -0.03f, 0); m.pop();
        m.box(0, 0.058f, -0.02f, 0.006f, 0.012f, 0.006f, 0.5f);
        wd.color(0.85f, 0.7f, 0.55f);
        wd.push(); wd.translate(0, -0.03f, 0.2f); wd.box(0, 0, 0, 0.017f, 0.035f, 0.12f, 0.5f); wd.pop();
        wd.push(); wd.translate(0, -0.07f, 0.04f); wd.rotX(-12); wd.box(0, 0, 0, 0.018f, 0.045f, 0.022f, 0.5f); wd.pop();
        hand(a, 0.0f, -0.075f, 0.045f, 0.14f, -0.28f, 0.3f);
        hand(a, 0.0f, -0.075f, -0.27f, -0.2f, -0.22f, 0.05f);
    }

    private static void storm(MeshBuilder m, MeshBuilder wd, MeshBuilder a) {
        m.color(0.28f, 0.3f, 0.34f);
        m.box(0, 0.02f, -0.12f, 0.022f, 0.032f, 0.13f, 0.5f);
        m.push(); m.translate(0, -0.075f, -0.05f); m.rotX(-14); m.box(0, 0, 0, 0.018f, 0.052f, 0.026f, 0.5f); m.pop();
        m.color(0.4f, 1.2f, 1.7f);
        m.push(); m.translate(0, 0.03f, -0.2f); m.rotX(-90); m.cylinder(0.012f, 0.012f, 0.2f, 8, true, true, 0.5f); m.pop();
        for (int i = 0; i < 4; i++) {
            m.color(1.2f, 1.9f, 2.2f);
            m.push(); m.translate(0, 0.03f, -0.22f - i * 0.045f); m.rotX(-90); m.cylinder(0.03f, 0.03f, 0.012f, 10, true, true, 0.5f); m.pop();
        }
        m.color(1.3f, 2.0f, 2.3f);
        m.box(0, 0.056f, -0.07f, 0.01f, 0.01f, 0.03f, 0.5f);
        wd.color(0.5f, 0.45f, 0.4f);
        wd.push(); wd.translate(0, -0.075f, -0.05f); wd.rotX(-14); wd.box(0.0185f, 0, 0, 0.003f, 0.046f, 0.022f, 0.5f); wd.pop();
        hand(a, 0.0f, -0.07f, -0.03f, 0.14f, -0.28f, 0.25f);
        hand(a, -0.012f, -0.1f, -0.045f, -0.12f, -0.3f, 0.25f);
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
