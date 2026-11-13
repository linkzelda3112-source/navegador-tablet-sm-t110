package com.kiarena;

import android.content.Context;
import android.content.SharedPreferences;

/** Progreso guardado: combates de campaña superados y si el sonido está activo. */
final class Save {
    private final SharedPreferences sp;

    Save(Context ctx) {
        sp = ctx.getSharedPreferences("kiarena", Context.MODE_PRIVATE);
    }

    int cleared() { return sp.getInt("cleared", 0); }

    void setCleared(int n) { sp.edit().putInt("cleared", n).apply(); }

    boolean sound() { return sp.getBoolean("sound", true); }

    void setSound(boolean on) { sp.edit().putBoolean("sound", on).apply(); }
}
