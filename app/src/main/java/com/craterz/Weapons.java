package com.craterz;

/** Tabla de armas. Todas son originales (nombres y diseños propios). */
public final class Weapons {
    public static final int PISTOL = 0, SHOTGUN = 1, RIFLE = 2, SMG = 3, STORM = 4;
    public static final int COUNT = 5;

    public static final String[] NAME = {"Pistola Cráter", "Escopeta Trinchera", "Fusil Mk3", "Metralleta MP18", "Tormenta"};
    /** daño por perdigón/bala */
    public static final float[] DMG = {30, 26, 46, 26, 320};
    public static final int[] PELLETS = {1, 8, 1, 1, 1};
    public static final float[] INTERVAL = {0.20f, 0.85f, 0.115f, 0.07f, 0.45f};
    public static final boolean[] AUTO = {false, false, true, true, false};
    public static final int[] MAG = {8, 6, 30, 32, 12};
    public static final int[] RESERVE = {96, 42, 240, 256, 72};
    public static final float[] RELOAD = {1.5f, 2.7f, 2.3f, 2.0f, 2.6f};
    /** dispersión en grados (cadera) */
    public static final float[] SPREAD = {1.1f, 4.2f, 1.7f, 2.6f, 0.8f};
    public static final float[] HEAD_MULT = {2.4f, 1.8f, 2.2f, 2.0f, 1.5f};
    public static final float[] KICK = {2.4f, 6.5f, 1.5f, 1.0f, 3.0f};
    public static final int[] PRICE = {0, 500, 1200, 1000, 0};

    private Weapons() {}
}
