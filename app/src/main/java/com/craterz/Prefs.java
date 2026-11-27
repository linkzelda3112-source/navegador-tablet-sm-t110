package com.craterz;

import android.content.Context;
import android.content.SharedPreferences;

/** Ajustes y récords guardados. */
public final class Prefs {
    private static SharedPreferences sp;

    private Prefs() {}

    public static void init(Context c) { if (sp == null) sp = c.getApplicationContext().getSharedPreferences("craterz", Context.MODE_PRIVATE); }

    /** 0 baja, 1 media, 2 alta */
    public static int quality() { return sp.getInt("quality", 1); }
    public static void setQuality(int q) { sp.edit().putInt("quality", q).apply(); }

    public static float sens() { return sp.getFloat("sens", 1f); }
    public static void setSens(float v) { sp.edit().putFloat("sens", v).apply(); }

    public static boolean vibrate() { return sp.getBoolean("vib", true); }
    public static void setVibrate(boolean v) { sp.edit().putBoolean("vib", v).apply(); }

    public static boolean sound() { return sp.getBoolean("snd", true); }
    public static void setSound(boolean v) { sp.edit().putBoolean("snd", v).apply(); }

    public static int bestRound(int mode) { return sp.getInt("best_r" + mode, 0); }
    public static int bestPoints(int mode) { return sp.getInt("best_p" + mode, 0); }
    public static int bestKills(int mode) { return sp.getInt("best_k" + mode, 0); }

    public static void saveRecord(int mode, int round, int points, int kills) {
        SharedPreferences.Editor e = sp.edit();
        if (round > bestRound(mode)) e.putInt("best_r" + mode, round);
        if (points > bestPoints(mode)) e.putInt("best_p" + mode, points);
        if (kills > bestKills(mode)) e.putInt("best_k" + mode, kills);
        e.apply();
    }

    public static int totalKills() { return sp.getInt("tot_k", 0); }
    public static void addKills(int n) { sp.edit().putInt("tot_k", totalKills() + n).apply(); }
}
