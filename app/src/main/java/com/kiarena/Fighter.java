package com.kiarena;

/** Un guerrero del plantel: estadísticas, colores y mazo de cartas. */
public final class Fighter {
    public static final int FUERZA = 0, TECNICA = 1, KI = 2;
    public static final String[] TYPE_NAMES = {"FUERZA", "TÉCNICA", "KI"};
    public static final int[] TYPE_COLORS = {0xFFE53935, 0xFF43A047, 0xFF1E88E5};

    public final int id;
    public final String name, title;
    public final int type;
    public final int hp;
    public final float atk, def, spd;
    public final int skin, hair, gi, aura, hairStyle;
    public final float build;
    public final Card[] deck;

    public Fighter(int id, String name, String title, int type, int hp, float atk, float def, float spd,
                   int skin, int hair, int gi, int aura, int hairStyle, float build, Card[] deck) {
        this.id = id;
        this.name = name;
        this.title = title;
        this.type = type;
        this.hp = hp;
        this.atk = atk;
        this.def = def;
        this.spd = spd;
        this.skin = skin;
        this.hair = hair;
        this.gi = gi;
        this.aura = aura;
        this.hairStyle = hairStyle;
        this.build = build;
        this.deck = deck;
    }
}
