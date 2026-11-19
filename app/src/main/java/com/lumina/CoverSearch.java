package com.lumina;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Busca carátulas en línea con la API pública de búsqueda de iTunes (sin clave ni cuenta). */
final class CoverSearch {
    private CoverSearch() {}

    static final class Result {
        final String title, artist, thumb, full;
        Result(String title, String artist, String thumb, String full) {
            this.title = title;
            this.artist = artist;
            this.thumb = thumb;
            this.full = full;
        }
    }

    /** Lanza una excepción solo si no hubo conexión; una lista vacía significa "sin resultados". */
    static List<Result> search(String term) throws Exception {
        List<Result> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        boolean anyOk = false;
        Exception last = null;
        for (String entity : new String[]{"album", "song"}) {
            try {
                String url = "https://itunes.apple.com/search?media=music&entity=" + entity + "&limit=12&term="
                        + URLEncoder.encode(term, "UTF-8");
                JSONObject o = new JSONObject(new String(download(url), "UTF-8"));
                JSONArray a = o.optJSONArray("results");
                anyOk = true;
                if (a == null) continue;
                for (int i = 0; i < a.length(); i++) {
                    JSONObject r = a.getJSONObject(i);
                    String art = r.optString("artworkUrl100", "");
                    if (art.isEmpty()) continue;
                    String full = art.replace("100x100bb", "700x700bb");
                    if (!seen.add(full)) continue;
                    String title = r.optString("collectionName", r.optString("trackName", ""));
                    out.add(new Result(title, r.optString("artistName", ""), art, full));
                }
            } catch (Exception e) {
                last = e;
            }
        }
        if (!anyOk && last != null) throw last;
        return out;
    }

    static byte[] download(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(12000);
        c.setRequestProperty("User-Agent", "Lumina/1.1");
        try {
            if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
            try (InputStream in = c.getInputStream()) {
                java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[16384];
                int n, total = 0;
                while ((n = in.read(buf)) > 0) {
                    total += n;
                    if (total > 12_000_000) throw new IOException("demasiado grande");
                    bo.write(buf, 0, n);
                }
                return bo.toByteArray();
            }
        } finally {
            c.disconnect();
        }
    }
}
