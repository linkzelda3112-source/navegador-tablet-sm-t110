package com.animestriker;

/** Equipos y jugadores originales (nada de marcas ni personajes reales). */
public final class Teams {

    public static final class Def {
        public final String name, tag, special;
        public final int main, second, accent;
        public final float spd, sht, pas, def, gk;
        public final String[] names;
        public final int[] hair, eye, skin;
        public final int stars;

        Def(String name, String tag, String special, int main, int second, int accent,
            float spd, float sht, float pas, float def, float gk,
            String[] names, int[] hair, int[] eye, int[] skin) {
            this.name = name; this.tag = tag; this.special = special;
            this.main = main; this.second = second; this.accent = accent;
            this.spd = spd; this.sht = sht; this.pas = pas; this.def = def; this.gk = gk;
            this.names = names; this.hair = hair; this.eye = eye; this.skin = skin;
            this.stars = Math.round(((spd + sht + pas + def + gk) / 5f - 0.9f) * 20f) + 2;
        }

        /** Capitán (delantero estrella, índice 5). */
        public String captain() { return names[5]; }
    }

    static final int SK1 = 0xFFFFE0C4, SK2 = 0xFFF6C9A4, SK3 = 0xFFDCA77C, SK4 = 0xFFB87A55;

    public static final Def[] ALL = {
        new Def("Sakura Storm", "SKR", "Tormenta de Pétalos",
            0xFFFF6FA5, 0xFFFFFFFF, 0xFF8E1F52,
            1.00f, 1.00f, 1.10f, 0.96f, 0.96f,
            new String[]{"Ren Aoki", "Haru Mori", "Daichi Sato", "Yuki Hanabi", "Sora Kimura", "Hinata Sakura"},
            new int[]{0xFF2B2B3C, 0xFF5B3A29, 0xFF3A3A55, 0xFFFFB6D5, 0xFFFF8FC0, 0xFFFF4F9A},
            new int[]{0xFF3A7BD5, 0xFF2E8B57, 0xFF8E44AD, 0xFFE84393, 0xFFFF7675, 0xFFE0307F},
            new int[]{SK2, SK1, SK3, SK1, SK2, SK1}),
        new Def("Dragon Kai", "DRK", "Colmillo del Dragón",
            0xFFE53935, 0xFFFFC107, 0xFF4A0E0E,
            1.00f, 1.13f, 0.95f, 1.00f, 0.95f,
            new String[]{"Goro Tatsu", "Ryu Kenzo", "Jin Akagi", "Kaito Honoo", "Shin Ryuzaki", "Raiga Hinode"},
            new int[]{0xFF1E1E1E, 0xFF7A1E1E, 0xFFFF6A00, 0xFFFFB300, 0xFFD50000, 0xFFFF3D00},
            new int[]{0xFFFFA000, 0xFFD32F2F, 0xFFF57C00, 0xFFFFC400, 0xFFFFD600, 0xFFFFAB00},
            new int[]{SK3, SK2, SK4, SK2, SK1, SK3}),
        new Def("Azure Wolves", "AZW", "Aullido Lunar",
            0xFF1E88E5, 0xFFFFFFFF, 0xFF0D2A57,
            1.00f, 0.97f, 1.00f, 1.13f, 1.06f,
            new String[]{"Takeru Ookami", "Ao Minato", "Ryo Shirogane", "Kazuma Tsuki", "Ranmaru Kaze", "Yamato Fubuki"},
            new int[]{0xFF263238, 0xFF90A4AE, 0xFF1565C0, 0xFF80D8FF, 0xFFB3E5FC, 0xFF29B6F6},
            new int[]{0xFF00B8D4, 0xFF5C6BC0, 0xFF26C6DA, 0xFF42A5F5, 0xFF7E57C2, 0xFF00E5FF},
            new int[]{SK1, SK2, SK1, SK3, SK2, SK1}),
        new Def("Thunder Kitsune", "TKN", "Rayo Zorro",
            0xFFFFD600, 0xFF212121, 0xFFFF6D00,
            1.13f, 1.00f, 1.00f, 0.92f, 0.95f,
            new String[]{"Kenta Inari", "Mugen Raiden", "Tora Denkou", "Akira Suzume", "Hayate Ranka", "Kitsu Raimei"},
            new int[]{0xFF4E342E, 0xFF212121, 0xFFFF8F00, 0xFFFFEE58, 0xFFFFCA28, 0xFFFFD600},
            new int[]{0xFFE65100, 0xFF6D4C41, 0xFF00897B, 0xFFEF6C00, 0xFF8E24AA, 0xFFFF6F00},
            new int[]{SK2, SK4, SK3, SK1, SK2, SK2}),
        new Def("Emerald Ronin", "EMR", "Corte Esmeralda",
            0xFF00C853, 0xFF102A1A, 0xFFB9F6CA,
            1.00f, 1.00f, 1.00f, 1.03f, 1.13f,
            new String[]{"Musashi Daigo", "Kenshin Aoyama", "Hanzo Midori", "Sen Takigawa", "Itto Kusanagi", "Jubei Hagane"},
            new int[]{0xFF1B1B1B, 0xFF2E7D32, 0xFF455A64, 0xFF69F0AE, 0xFF00E676, 0xFF1DE9B6},
            new int[]{0xFF2E7D32, 0xFF00ACC1, 0xFF558B2F, 0xFF00BFA5, 0xFFC62828, 0xFF00C853},
            new int[]{SK3, SK2, SK1, SK4, SK2, SK2}),
        new Def("Violet Phantoms", "VPH", "Eclipse Fantasma",
            0xFF8E24AA, 0xFFCFD8DC, 0xFF311B92,
            1.03f, 1.06f, 1.06f, 1.00f, 1.00f,
            new String[]{"Rei Kageyama", "Yami Shiranui", "Nagi Yoruno", "Mikoto Tsukikage", "Zen Kurogane", "Raven Kuroha"},
            new int[]{0xFF311B92, 0xFFB39DDB, 0xFF212121, 0xFFE040FB, 0xFFCE93D8, 0xFF7C4DFF},
            new int[]{0xFFD500F9, 0xFF651FFF, 0xFFAA00FF, 0xFFFF4081, 0xFF00E5FF, 0xFFE040FB},
            new int[]{SK1, SK1, SK2, SK1, SK3, SK1}),
    };

    private Teams() {}
}
