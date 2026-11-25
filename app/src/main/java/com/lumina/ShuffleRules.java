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

    private static final Set<String> wArtists = new HashSet<>();
    private static final Set<String> wSongs = new HashSet<>();
    private static boolean workOn;
    private static int version;
    private static List<Track> viewSrc, viewOut;
    private static int viewVer = -1;

    private ShuffleRules() { }

    static boolean workOn() { return workOn; }

    static void setWork(boolean on) {
        workOn = on;
        version++;
        if (sp != null) sp.edit().putBoolean("work", on).apply();
    }

    static boolean workArtist(String name) { return wArtists.contains(Personal.norm(name)); }

    static boolean workSong(Track t) { return wSongs.contains(Personal.key(t)); }

    static boolean workAllowed(Track t) {
        return wArtists.contains(artistKey(t)) || wSongs.contains(Personal.key(t));
    }

    static void toggleWorkArtist(String name) {
        String k = Personal.norm(name);
        if (!wArtists.remove(k)) wArtists.add(k);
        version++;
        saveWork();
    }

    static void toggleWorkSong(Track t) {
        String k = Personal.key(t);
        if (!wSongs.remove(k)) wSongs.add(k);
        version++;
        saveWork();
    }

    static void clearWork() {
        wArtists.clear();
        wSongs.clear();
        version++;
        saveWork();
    }

    private static void saveWork() {
        if (sp == null) return;
        sp.edit().putStringSet("wartists", new HashSet<>(wArtists)).putStringSet("wsongs", new HashSet<>(wSongs)).apply();
    }

    static int workTotal() { return wArtists.size() + wSongs.size(); }

    /** Biblioteca visible: completa, o solo lo permitido en modo trabajo (la lista se reutiliza si no cambió). */
    static List<Track> view(List<Track> src) {
        if (!workOn) return src;
        if (src != viewSrc || viewVer != version || viewOut == null) {
            List<Track> out = new java.util.ArrayList<>();
            for (Track t : src) if (workAllowed(t)) out.add(t);
            viewSrc = src;
            viewOut = out;
            viewVer = version;
        }
        return viewOut;
    }

    static void init(Context c) {
        if (sp != null) return;
        sp = c.getApplicationContext().getSharedPreferences("lumina_shuffle", Context.MODE_PRIVATE);
        artists.addAll(sp.getStringSet("artists", new HashSet<>()));
        songs.addAll(sp.getStringSet("songs", new HashSet<>()));
        wArtists.addAll(sp.getStringSet("wartists", new HashSet<>()));
        wSongs.addAll(sp.getStringSet("wsongs", new HashSet<>()));
        workOn = sp.getBoolean("work", false);
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
