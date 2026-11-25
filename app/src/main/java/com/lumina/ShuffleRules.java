package com.lumina;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Artistas y canciones que nunca deben salir en el modo aleatorio. */
final class ShuffleRules {
    private static SharedPreferences sp;
    private static final Set<String> artists = new HashSet<>();
    private static final Set<String> songs = new HashSet<>();

    private ShuffleRules() { }

    static void init(Context c) {
        if (sp != null) return;
        sp = c.getApplicationContext().getSharedPreferences("lumina_shuffle", Context.MODE_PRIVATE);
        artists.addAll(sp.getStringSet("artists", new HashSet<>()));
        songs.addAll(sp.getStringSet("songs", new HashSet<>()));
    }

    private static void save() {
        if (sp == null) return;
        sp.edit().putStringSet("artists", new HashSet<>(artists)).putStringSet("songs", new HashSet<>(songs)).apply();
    }

    static String artistKey(Track t) { return Personal.norm(t.artist); }

    static boolean artistExcluded(String name) { return artists.contains(Personal.norm(name)); }

    static boolean songExcluded(Track t) { return songs.contains(Personal.key(t)); }

    static boolean isExcluded(Track t) {
        return artists.contains(artistKey(t)) || songs.contains(Personal.key(t));
    }

    static void toggleArtist(String name) {
        String k = Personal.norm(name);
        if (!artists.remove(k)) artists.add(k);
        save();
    }

    static void toggleSong(Track t) {
        String k = Personal.key(t);
        if (!songs.remove(k)) songs.add(k);
        save();
    }

    static void clear() {
        artists.clear();
        songs.clear();
        save();
    }

    static int total() { return artists.size() + songs.size(); }

    static int countAllowed(List<Track> l) {
        int n = 0;
        for (Track t : l) if (!isExcluded(t)) n++;
        return n;
    }
}
