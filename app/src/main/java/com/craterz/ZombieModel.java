package com.craterz;

/** Partes del zombi (mallas estáticas); se animan con matrices en el renderizador. */
public final class ZombieModel {
    public static final int TORSO = 0, HEAD = 1, UARM = 2, FARM = 3, THIGH = 4, SHIN = 5, EYES = 6, NPARTS = 7;
    public final float[][] part = new float[NPARTS][];

    private static final float SK0 = 0.02f, SK1 = 0.02f, SK2 = 0.5f, SK3 = 0.48f;   // piel (mitad superior)
    private static final float CL0 = 0.02f, CL1 = 0.52f, CL2 = 0.98f, CL3 = 0.98f;  // ropa (mitad inferior)

    public ZombieModel() {
        MeshBuilder b;

        // torso: caja con hombros, hombros torcidos, costillas y harapos
        b = new MeshBuilder();
        b.uvRect(CL0, CL1, CL2, CL3);
        b.color(1, 1, 1);
        b.box(0, 0.3f, 0, 0.23f, 0.3f, 0.13f, 1f);
        b.color(0.8f, 0.78f, 0.75f);
        b.box(0, -0.05f, 0, 0.2f, 0.12f, 0.115f, 1f);
        b.color(0.9f, 0.85f, 0.8f);
        b.push(); b.translate(0, 0.0f, 0.12f); b.rotX(8); b.box(0, -0.12f, 0, 0.2f, 0.18f, 0.012f, 1f); b.pop();
        b.push(); b.translate(0.1f, -0.06f, -0.12f); b.rotZ(10); b.box(0, -0.12f, 0, 0.07f, 0.2f, 0.012f, 1f); b.pop();
        b.uvRect(SK0, SK1, SK2, SK3);
        b.color(1.05f, 0.9f, 0.9f);
        b.box(0, 0.5f, 0.0f, 0.075f, 0.07f, 0.075f, 1f);  // cuello
        b.color(0.8f, 0.5f, 0.5f);
        b.box(0.05f, 0.35f, 0.128f, 0.08f, 0.07f, 0.008f, 1f); // herida pecho expuesta
        part[TORSO] = b.toArray();

        // cabeza: cráneo, mandíbula caída, pelo ralo
        b = new MeshBuilder();
        b.uvRect(SK0, SK1, SK2, SK3);
        b.color(1, 1, 1);
        b.box(0, 0.12f, 0, 0.1f, 0.115f, 0.11f, 1f);
        b.color(0.85f, 0.7f, 0.7f);
        b.push(); b.translate(0, -0.02f, 0.025f); b.rotX(-14); b.box(0, -0.04f, 0, 0.075f, 0.04f, 0.085f, 1f); b.pop();
        b.color(0.1f, 0.03f, 0.03f);
        b.box(0, 0.0f, 0.1f, 0.05f, 0.012f, 0.02f, 1f); // boca
        b.uvRect(CL0, CL1, CL2, CL3);
        b.color(0.55f, 0.5f, 0.45f);
        b.box(-0.02f, 0.235f, -0.01f, 0.08f, 0.02f, 0.09f, 1f);
        part[HEAD] = b.toArray();

        // ojos brillantes (emisivos: color > 1)
        b = new MeshBuilder();
        b.uvRect(SK0, SK1, SK2, SK3);
        b.color(3.2f, 2.6f, 0.4f);
        b.box(-0.04f, 0.15f, 0.106f, 0.018f, 0.014f, 0.01f, 1f);
        b.box(0.04f, 0.15f, 0.106f, 0.018f, 0.014f, 0.01f, 1f);
        part[EYES] = b.toArray();

        // brazo superior (cuelga desde el hombro, -Y)
        b = new MeshBuilder();
        b.uvRect(CL0, CL1, CL2, CL3);
        b.color(1, 1, 1);
        b.taper(0.3f, 0.085f, 0.085f, 0.1f, 0.1f, 1f);
        part[UARM] = b.toArray();

        // antebrazo + mano con garras
        b = new MeshBuilder();
        b.uvRect(SK0, SK1, SK2, SK3);
        b.color(1.05f, 0.95f, 0.95f);
        b.taper(0.26f, 0.06f, 0.07f, 0.08f, 0.085f, 1f);
        b.color(0.85f, 0.8f, 0.75f);
        b.push(); b.translate(0, -0.26f, 0); b.box(0, -0.04f, 0, 0.035f, 0.045f, 0.012f, 1f); b.pop();
        b.color(0.25f, 0.22f, 0.18f);
        for (int i = -1; i <= 1; i++) { b.push(); b.translate(i * 0.022f, -0.3f, 0); b.box(0, -0.035f, 0, 0.007f, 0.04f, 0.007f, 1f); b.pop(); }
        part[FARM] = b.toArray();

        // muslo
        b = new MeshBuilder();
        b.uvRect(CL0, CL1, CL2, CL3);
        b.color(0.85f, 0.82f, 0.8f);
        b.taper(0.46f, 0.12f, 0.12f, 0.15f, 0.15f, 1f);
        part[THIGH] = b.toArray();

        // pantorrilla + bota
        b = new MeshBuilder();
        b.uvRect(CL0, CL1, CL2, CL3);
        b.color(0.75f, 0.72f, 0.7f);
        b.taper(0.44f, 0.09f, 0.09f, 0.115f, 0.115f, 1f);
        b.color(0.35f, 0.3f, 0.28f);
        b.box(0, -0.46f, 0.045f, 0.065f, 0.04f, 0.11f, 1f);
        part[SHIN] = b.toArray();
    }
}
