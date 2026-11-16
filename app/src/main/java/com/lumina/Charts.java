package com.lumina;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Las canciones más escuchadas del momento (lista pública de Apple), y cuáles de ellas tienes en tu biblioteca. */
final class Charts {
    static final class Entry {
        final String title, artist;
        Entry(String title, String artist) { this.title = title; this.artist = artist; }
    }

    static final class Hit {
        final int rank;
        final Track track;
        Hit(int rank, Track track) { this.rank = rank; this.track = track; }
    }

    private static final long FRESH_MS = 3L * 60 * 60 * 1000;
    private final SharedPreferences sp;
    private final Handler main = new Handler(Looper.getMainLooper());
    private List<Entry> entries = new ArrayList<>();
    private String cc = "";
    private long ts, lastTry;
    private boolean loading;

    Charts(Context c) {
        sp = c.getApplicationContext().getSharedPreferences("lumina_charts", Context.MODE_PRIVATE);
        cc = sp.getString("cc", "");
        ts = sp.getLong("ts", 0);
        for (String line : sp.getString("data", "").split("\n")) {
            int i = line.indexOf('\t');
            if (i > 0) entries.add(new Entry(line.substring(0, i), line.substring(i + 1)));
        }
    }

    boolean has() { return !entries.isEmpty(); }

    boolean isLoading() { return loading; }

    int size() { return entries.size(); }

    String countryName() {
        if (cc.length() != 2) return "";
        String n = new Locale("", cc).getDisplayCountry(new Locale("es"));
        return n == null || n.isEmpty() ? cc.toUpperCase(Locale.ROOT) : n;
    }

    /** Descarga la lista si hace falta (cada ~3 horas) o si 'force'. 'done' se llama en el hilo principal. */
    void refresh(boolean force, Runnable done) {
        long now = System.currentTimeMillis();
        if (loading) return;
        if (!force) {
            if (!entries.isEmpty() && now - ts < FRESH_MS) return;
            if (now - lastTry < 60_000) return;
        }
        lastTry = now;
        loading = true;
        new Thread(() -> {
            String country = Locale.getDefault().getCountry().toLowerCase(Locale.ROOT);
            if (country.length() != 2) country = "us";
            String[] list = country.equals("us") ? new String[]{"us"} : new String[]{country, "us"};
            List<Entry> res = null;
            String used = "";
            for (String c : list) {
                res = fetch("https://rss.applemarketingtools.com/api/v2/" + c + "/music/most-played/100/songs.json");
                if (res == null || res.isEmpty()) res = fetch("https://itunes.apple.com/" + c + "/rss/topsongs/limit=100/json");
                if (res != null && !res.isEmpty()) {
                    used = c;
                    break;
                }
            }
            final List<Entry> fin = res;
            final String fcc = used;
            main.post(() -> {
                loading = false;
                if (fin != null && !fin.isEmpty()) {
                    entries = fin;
                    cc = fcc;
                    ts = System.currentTimeMillis();
                    StringBuilder sb = new StringBuilder();
                    for (Entry e : fin) sb.append(e.title).append('\t').append(e.artist).append('\n');
                    sp.edit().putString("data", sb.toString()).putString("cc", cc).putLong("ts", ts).apply();
                }
                if (done != null) done.run();
            });
        }).start();
    }

    private static String clean(String s) {
        return s == null ? "" : s.replace('\t', ' ').replace('\n', ' ').trim();
    }

    private static List<Entry> fetch(String url) {
        HttpURLConnection h = null;
        try {
            h = (HttpURLConnection) new URL(url).openConnection();
            h.setConnectTimeout(8000);
            h.setReadTimeout(10000);
            h.setRequestProperty("User-Agent", "Lumina/1.2");
            if (h.getResponseCode() != 200) return null;
            InputStream in = h.getInputStream();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            in.close();
            JSONObject feed = new JSONObject(bo.toString("UTF-8")).getJSONObject("feed");
            List<Entry> out = new ArrayList<>();
            if (feed.has("results")) {
                JSONArray a = feed.getJSONArray("results");
                for (int i = 0; i < a.length(); i++) {
                    JSONObject o = a.getJSONObject(i);
                    String t = clean(o.optString("name")), ar = clean(o.optString("artistName"));
                    if (!t.isEmpty()) out.add(new Entry(t, ar));
                }
            } else if (feed.has("entry")) {
                JSONArray a = feed.getJSONArray("entry");
                for (int i = 0; i < a.length(); i++) {
                    JSONObject o = a.getJSONObject(i);
                    String t = clean(o.getJSONObject("im:name").optString("label"));
                    String ar = clean(o.getJSONObject("im:artist").optString("label"));
                    if (!t.isEmpty()) out.add(new Entry(t, ar));
                }
            }
            return out;
        } catch (Throwable e) {
            return null;
        } finally {
            if (h != null) h.disconnect();
        }
    }

    private static boolean artistMatch(String chartArtist, String libArtist, int titleLen) {
        if (libArtist == null || libArtist.isEmpty() || libArtist.equals(Library.UNKNOWN_ARTIST)) return titleLen >= 4;
        String ca = Personal.norm(chartArtist), la = Personal.norm(libArtist);
        if (ca.isEmpty()) return true;
        if (la.length() >= 3 && ca.contains(la)) return true;
        if (ca.length() >= 3 && la.contains(ca)) return true;
        List<String> lt = Personal.tokens(libArtist);
        for (String w : Personal.tokens(chartArtist)) {
            if (lt.contains(w)) return true;
        }
        return false;
    }

    /** Las canciones de la lista que existen en la biblioteca, en orden de popularidad. */
    List<Hit> match(List<Track> all) {
        List<Hit> out = new ArrayList<>();
        if (entries.isEmpty() || all.isEmpty()) return out;
        Map<String, List<Track>> byTitle = new HashMap<>();
        for (Track t : all) {
            String k = Personal.norm(t.title);
            if (k.length() < 2) continue;
            List<Track> l = byTitle.get(k);
            if (l == null) {
                l = new ArrayList<>(2);
                byTitle.put(k, l);
            }
            l.add(t);
        }
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            String nt = Personal.norm(e.title);
            List<Track> l = byTitle.get(nt);
            if (l == null) continue;
            for (Track t : l) {
                if (artistMatch(e.artist, t.artist, nt.length())) {
                    out.add(new Hit(i + 1, t));
                    break;
                }
            }
        }
        return out;
    }
}
