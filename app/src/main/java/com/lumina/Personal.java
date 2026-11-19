package com.lumina;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Lo personal: "Me gusta" (1), "Me encanta" (2) y lo escuchado recientemente. Se guarda por título + artista. */
final class Personal {
    private static final int MAX_HISTORY = 40;
    private static final Pattern PAREN = Pattern.compile("\\([^)]*\\)|\\[[^\\]]*\\]");
    private static final Pattern FEAT = Pattern.compile("\\b(feat|ft|featuring)\\b.*$");
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON = Pattern.compile("[^\\p{L}\\p{N}]+");

    private final SharedPreferences sp;
    private final LinkedHashMap<String, Integer> ratings = new LinkedHashMap<>();
    private final ArrayList<String> history = new ArrayList<>();

    Personal(Context c) {
        sp = c.getApplicationContext().getSharedPreferences("lumina_personal", Context.MODE_PRIVATE);
        for (String line : sp.getString("ratings", "").split("\n")) {
            int i = line.lastIndexOf('=');
            if (i <= 0) continue;
            try {
                ratings.put(line.substring(0, i), Integer.parseInt(line.substring(i + 1)));
            } catch (NumberFormatException ignored) { }
        }
        for (String line : sp.getString("history", "").split("\n")) {
            if (!line.isEmpty()) history.add(line);
        }
    }

    /** Texto sin acentos, minúsculas, sin (paréntesis), sin "- remasterizado" ni "feat." y solo letras/números. */
    static String norm(String s) {
        if (s == null) return "";
        String x = MARKS.matcher(Normalizer.normalize(s, Normalizer.Form.NFD)).replaceAll("").toLowerCase(Locale.ROOT);
        String base = NON.matcher(x).replaceAll("");
        x = PAREN.matcher(x).replaceAll(" ");
        int d = x.indexOf(" - ");
        if (d > 0) x = x.substring(0, d);
        x = FEAT.matcher(x).replaceAll("");
        x = NON.matcher(x).replaceAll("");
        return x.isEmpty() ? base : x;
    }

    /** Palabras (3+ letras) de un nombre, sin acentos, para comparar artistas. */
    static List<String> tokens(String s) {
        List<String> out = new ArrayList<>();
        if (s == null) return out;
        String x = MARKS.matcher(Normalizer.normalize(s, Normalizer.Form.NFD)).replaceAll("").toLowerCase(Locale.ROOT);
        for (String w : NON.split(x)) {
            if (w.length() >= 3 && !w.equals("feat") && !w.equals("the") && !w.equals("and") && !w.equals("con")) out.add(w);
        }
        return out;
    }

    static String key(Track t) {
        if (t.pkey == null) t.pkey = norm(t.title) + "|" + norm(t.artist);
        return t.pkey;
    }

    int rating(Track t) {
        Integer r = ratings.get(key(t));
        return r == null ? 0 : r;
    }

    void setRating(Track t, int r) {
        String k = key(t);
        ratings.remove(k);
        if (r > 0) ratings.put(k, r);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : ratings.entrySet()) sb.append(e.getKey()).append('=').append(e.getValue()).append('\n');
        sp.edit().putString("ratings", sb.toString()).apply();
    }

    void played(Track t) {
        String k = key(t);
        history.remove(k);
        history.add(0, k);
        while (history.size() > MAX_HISTORY) history.remove(history.size() - 1);
        StringBuilder sb = new StringBuilder();
        for (String h : history) sb.append(h).append('\n');
        sp.edit().putString("history", sb.toString()).apply();
    }

    private static Map<String, Track> index(List<Track> all) {
        Map<String, Track> m = new HashMap<>();
        for (Track t : all) {
            String k = key(t);
            if (!m.containsKey(k)) m.put(k, t);
        }
        return m;
    }

    /** Canciones de la biblioteca con valoración >= min, las más recientes primero. */
    List<Track> rated(List<Track> all, int min) {
        Map<String, Track> idx = index(all);
        List<String> keys = new ArrayList<>(ratings.keySet());
        Collections.reverse(keys);
        List<Track> out = new ArrayList<>();
        for (String k : keys) {
            Integer r = ratings.get(k);
            Track t = idx.get(k);
            if (r != null && r >= min && t != null) out.add(t);
        }
        return out;
    }

    List<Track> recent(List<Track> all) {
        Map<String, Track> idx = index(all);
        List<Track> out = new ArrayList<>();
        for (String k : history) {
            Track t = idx.get(k);
            if (t != null) out.add(t);
        }
        return out;
    }
}
