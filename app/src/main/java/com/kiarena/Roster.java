package com.kiarena;

/** El plantel de 8 guerreros originales y los enemigos de cada etapa de la campaña. */
public final class Roster {
    private Roster() {}

    public static final int START_UNLOCKED = 4;
    public static final int STAGES = 5;

    public static final Fighter[] ALL = {
        mk(0, "Raiden", "El Rayo Azul", Fighter.KI, 1000, 105, 85, 100,
            0xFFF2C9A0, 0xFF1A1A2E, 0xFF1565C0, 0xFF4FC3F7, 0, 1.0f,
            Card.BLAST, "Rayo Gemelo", 175, 3,
            "Puñetazo", "Combo Trueno", "Onda Eléctrica", "TORMENTA CELESTIAL"),
        mk(1, "Hayate", "Filo del Viento", Fighter.TECNICA, 960, 106, 85, 125,
            0xFFF2C9A0, 0xFF2E7D32, 0xFF00695C, 0xFF69F0AE, 1, 0.9f,
            Card.BUFF, "Concentración", 40, 2,
            "Corte rápido", "Danza de Cuchillas", "Ráfaga Cortante", "TORNADO DE ACERO"),
        mk(2, "Gorak", "Puño de Roca", Fighter.FUERZA, 1250, 110, 105, 70,
            0xFFD9A066, 0xFFB71C1C, 0xFF6D4C41, 0xFFFF8A65, 2, 1.35f,
            Card.STRIKE, "Martillo Sísmico", 170, 3,
            "Golpe pesado", "Embestida", "Roca Ardiente", "TERREMOTO FINAL"),
        mk(3, "Mira", "Estrella Rosa", Fighter.KI, 1020, 102, 85, 110,
            0xFFFFDDC2, 0xFFEC407A, 0xFF8E24AA, 0xFFF48FB1, 4, 0.85f,
            Card.HEAL, "Luz Sanadora", 30, 3,
            "Patada Veloz", "Lluvia de Estrellas", "Orbe Rosa", "SUPERNOVA ROSA"),
        mk(4, "Vorg", "El Invasor", Fighter.FUERZA, 1150, 118, 95, 85,
            0xFF8E6BBF, 0xFF4A148C, 0xFF37474F, 0xFFB388FF, 3, 1.25f,
            Card.CHARGE, "Absorber Ki", 3, 1,
            "Zarpazo", "Garra Abisal", "Rayo Oscuro", "DEVORADOR DE MUNDOS"),
        mk(5, "Sora", "Monje Esmeralda", Fighter.TECNICA, 1080, 98, 100, 105,
            0xFFE0B48A, 0xFF5D4037, 0xFFF9A825, 0xFFFFEE58, 2, 0.95f,
            Card.HEAL, "Meditación", 30, 3,
            "Palma Suave", "Cien Palmas", "Esfera Serena", "ILUMINACIÓN"),
        mk(6, "Drakon", "Emperador Carmesí", Fighter.FUERZA, 1150, 125, 100, 95,
            0xFFC0392B, 0xFF212121, 0xFF4E342E, 0xFFFF5252, 0, 1.2f,
            Card.BLAST, "Llama Imperial", 175, 3,
            "Garra Real", "Tormenta de Golpes", "Bola Infernal", "ERUPCIÓN IMPERIAL"),
        mk(7, "Nova", "Leyenda Dorada", Fighter.KI, 1100, 130, 95, 115,
            0xFFF2C9A0, 0xFFFFD600, 0xFFEF6C00, 0xFFFFE082, 0, 1.05f,
            Card.BUFF, "Modo Dorado", 40, 2,
            "Puño Dorado", "Ráfaga Solar", "Cañón de Luz", "BIG BANG DORADO"),
    };

    /** Equipos enemigos por etapa (índices en ALL). Etapa 1..5. */
    private static final int[][] STAGE_TEAMS = {
        {4, 5, 3},
        {1, 2, 4},
        {5, 6, 0},
        {6, 7, 2},
        {6, 4, 7},
    };

    public static Fighter[] stageTeam(int stage) {
        int[] ids = STAGE_TEAMS[Math.max(1, Math.min(STAGES, stage)) - 1];
        Fighter[] t = new Fighter[3];
        for (int i = 0; i < 3; i++) t[i] = ALL[ids[i]];
        return t;
    }

    /** Multiplicador de vida/ataque del enemigo según el nivel de dificultad (1..5). */
    public static float levelMult(int level) {
        return 0.78f + 0.06f * (level - 1);
    }

    public static int unlockedCount(int cleared) {
        return Math.min(ALL.length, START_UNLOCKED + cleared);
    }

    private static Fighter mk(int id, String name, String title, int type, int hp, float atk, float def, float spd,
                              int skin, int hair, int gi, int aura, int hairStyle, float build,
                              int spKind, String spName, int spPower, int spCost,
                              String s0, String s1, String bl, String ulti) {
        Card[] d = {
            new Card(s0, Card.STRIKE, 0, 70),
            new Card(s1, Card.STRIKE, 2, 125),
            new Card(bl, Card.BLAST, 2, 135),
            new Card("Guardia", Card.GUARD, 1, 0),
            new Card(spName, spKind, spCost, spPower),
            new Card(ulti, Card.ULTI, 6, 300),
        };
        return new Fighter(id, name, title, type, hp, atk, def, spd, skin, hair, gi, aura, hairStyle, build, d);
    }
}
