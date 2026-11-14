package com.lumina;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * La biblioteca de música: de dónde se lee (todo el teléfono y/o carpetas elegidas), el escaneo en
 * segundo plano y una caché en disco para que la app abra al instante.
 */
final class Library {
    interface Listener { void onLibraryChanged(); }

    static final String UNKNOWN_ARTIST = "Artista desconocido";
    static final String UNKNOWN_ALBUM = "Álbum desconocido";

    static final class Group {
        final String name;
        String sub = "";
        final List<Track> tracks = new ArrayList<>();
        Group(String name) { this.name = name; }
        Track cover() { return tracks.get(0); }
    }

    private final Context ctx;
    private final SharedPreferences sp;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final List<Listener> listeners = new ArrayList<>();
    private volatile List<Track> tracks = new ArrayList<>();
    private volatile boolean scanning;
    private volatile int found;
    private int gen;

    Library(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.sp = this.ctx.getSharedPreferences("lumina", Context.MODE_PRIVATE);
    }

    // ------------------------------------------------------------------ estado

    void addListener(Listener l) { if (!listeners.contains(l)) listeners.add(l); }

    void removeListener(Listener l) { listeners.remove(l); }

    List<Track> tracks() { return tracks; }

    boolean isScanning() { return scanning; }

    int found() { return found; }

    private void notifyUi() {
        main.post(() -> {
            for (Listener l : new ArrayList<>(listeners)) l.onLibraryChanged();
        });
    }

    // ----------------------------------------------------------------- fuentes

    boolean allPhone() { return sp.getBoolean("allPhone", false); }

    void setAllPhone(boolean on) { sp.edit().putBoolean("allPhone", on).apply(); }

    List<String> folders() {
        return new ArrayList<>(sp.getStringSet("folders", new HashSet<String>()));
    }

    void addFolder(String treeUri) {
        Set<String> s = new HashSet<>(sp.getStringSet("folders", new HashSet<String>()));
        s.add(treeUri);
        sp.edit().putStringSet("folders", s).apply();
    }

    void removeFolder(String treeUri) {
        Set<String> s = new HashSet<>(sp.getStringSet("folders", new HashSet<String>()));
        s.remove(treeUri);
        sp.edit().putStringSet("folders", s).apply();
    }

    boolean hasSources() { return allPhone() || !folders().isEmpty(); }

    // ------------------------------------------------------------------ agrupar

    static List<Group> albums(List<Track> src) {
        Map<String, Group> map = new LinkedHashMap<>();
        for (Track t : src) {
            String k = t.album.toLowerCase(Locale.ROOT);
            Group g = map.get(k);
            if (g == null) {
                g = new Group(t.album);
                g.sub = t.artist;
                map.put(k, g);
            } else if (!g.sub.equals(t.artist) && !g.sub.equals("Varios artistas")) {
                g.sub = "Varios artistas";
            }
            g.tracks.add(t);
        }
        List<Group> out = new ArrayList<>(map.values());
        for (Group g : out) g.sub = g.sub + " · " + g.tracks.size() + (g.tracks.size() == 1 ? " canción" : " canciones");
        Collections.sort(out, (a, b) -> a.name.compareToIgnoreCase(b.name));
        return out;
    }

    static List<Group> artists(List<Track> src) {
        Map<String, Group> map = new LinkedHashMap<>();
        for (Track t : src) {
            String k = t.artist.toLowerCase(Locale.ROOT);
            Group g = map.get(k);
            if (g == null) {
                g = new Group(t.artist);
                map.put(k, g);
            }
            g.tracks.add(t);
        }
        List<Group> out = new ArrayList<>(map.values());
        for (Group g : out) g.sub = g.tracks.size() + (g.tracks.size() == 1 ? " canción" : " canciones");
        Collections.sort(out, (a, b) -> a.name.compareToIgnoreCase(b.name));
        return out;
    }

    // ------------------------------------------------------------------ escaneo

    /** Carga la caché del disco (rápido) y luego vuelve a escanear las fuentes en segundo plano. */
    void start() {
        exec.execute(() -> {
            List<Track> cached = readCache();
            if (!cached.isEmpty() && tracks.isEmpty()) {
                tracks = cached;
                notifyUi();
            }
            if (hasSources()) {
                final int g;
                synchronized (Library.this) { g = ++gen; }
                doScan(g);
            }
        });
    }

    void rescan() {
        final int g;
        synchronized (this) { g = ++gen; }
        scanning = hasSources();
        found = 0;
        notifyUi();
        exec.execute(() -> doScan(g));
    }

    private boolean stale(int g) {
        synchronized (this) { return g != gen; }
    }

    private void doScan(int g) {
        if (!hasSources()) {
            tracks = new ArrayList<>();
            scanning = false;
            writeCache(tracks);
            notifyUi();
            return;
        }
        scanning = true;
        found = 0;
        notifyUi();
        Map<String, Track> known = new HashMap<>();
        for (Track t : tracks) known.put(t.key, t);
        List<Track> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        try {
            if (allPhone()) scanStore(out, seen, g);
            for (String f : folders()) {
                if (stale(g)) return;
                try {
                    scanTree(Uri.parse(f), out, seen, known, g);
                } catch (Throwable ignored) {
                    // una carpeta ilegible no debe detener las demás
                }
            }
        } catch (Throwable ignored) {
        }
        if (stale(g)) return;
        Collections.sort(out, new Comparator<Track>() {
            @Override public int compare(Track a, Track b) { return a.title.compareToIgnoreCase(b.title); }
        });
        tracks = out;
        scanning = false;
        writeCache(out);
        notifyUi();
    }

    private boolean addUnique(List<Track> out, Set<String> seen, Track t, int g) {
        String dup = t.title.toLowerCase(Locale.ROOT) + "|" + t.artist.toLowerCase(Locale.ROOT) + "|" + (t.durationMs / 2000);
        if (!seen.add(t.key) || !seen.add(dup)) return false;
        out.add(t);
        found = out.size();
        if (found % 25 == 0 && !stale(g)) notifyUi();
        return true;
    }

    private void scanStore(List<Track> out, Set<String> seen, int g) {
        Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] proj = {
                MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM_ID};
        String sel = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " + MediaStore.Audio.Media.DURATION + " >= 30000";
        Cursor c = null;
        try {
            c = ctx.getContentResolver().query(base, proj, sel, null, null);
            if (c == null) return;
            while (c.moveToNext()) {
                if (stale(g)) return;
                long id = c.getLong(0);
                String title = clean(c.getString(1), "Sin título");
                String artist = clean(c.getString(2), UNKNOWN_ARTIST);
                String album = clean(c.getString(3), UNKNOWN_ALBUM);
                long dur = c.getLong(4);
                long albumId = c.getLong(5);
                Uri uri = ContentUris.withAppendedId(base, id);
                addUnique(out, seen, new Track(uri, title, artist, album, dur, true, albumId), g);
            }
        } finally {
            if (c != null) c.close();
        }
    }

    private static String clean(String s, String def) {
        if (s == null) return def;
        s = s.trim();
        if (s.isEmpty() || s.equals("<unknown>")) return def;
        return s;
    }

    private static boolean isAudio(String mime, String name) {
        if (mime != null && mime.startsWith("audio/")) return true;
        if (mime != null && mime.equals("application/ogg")) return true;
        if (name == null) return false;
        String n = name.toLowerCase(Locale.ROOT);
        return n.endsWith(".mp3") || n.endsWith(".m4a") || n.endsWith(".flac") || n.endsWith(".ogg")
                || n.endsWith(".opus") || n.endsWith(".wav") || n.endsWith(".aac") || n.endsWith(".wma");
    }

    private void scanTree(Uri tree, List<Track> out, Set<String> seen, Map<String, Track> known, int g) {
        String root = DocumentsContract.getTreeDocumentId(tree);
        walk(tree, root, 0, out, seen, known, g);
    }

    private void walk(Uri tree, String docId, int depth, List<Track> out, Set<String> seen,
                      Map<String, Track> known, int g) {
        if (depth > 12 || stale(g)) return;
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId);
        ContentResolver cr = ctx.getContentResolver();
        List<String> dirs = new ArrayList<>();
        List<String[]> files = new ArrayList<>();
        Cursor c = null;
        try {
            c = cr.query(children, new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE}, null, null, null);
            if (c == null) return;
            while (c.moveToNext()) {
                String id = c.getString(0), name = c.getString(1), mime = c.getString(2);
                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                    if (name != null && name.startsWith(".")) continue;
                    dirs.add(id);
                } else if (isAudio(mime, name)) {
                    files.add(new String[]{id, name});
                }
            }
        } catch (Throwable t) {
            return;
        } finally {
            if (c != null) c.close();
        }
        for (String[] f : files) {
            if (stale(g)) return;
            Uri doc = DocumentsContract.buildDocumentUriUsingTree(tree, f[0]);
            Track t = known.get(doc.toString());
            if (t == null) t = readMetadata(doc, f[1]);
            if (t != null) addUnique(out, seen, t, g);
        }
        for (String d : dirs) walk(tree, d, depth + 1, out, seen, known, g);
    }

    private Track readMetadata(Uri uri, String fileName) {
        String title = null, artist = null, album = null;
        long dur = 0;
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            r.setDataSource(ctx, uri);
            title = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            artist = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            if (artist == null) artist = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST);
            album = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            String d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (d != null) dur = Long.parseLong(d);
        } catch (Throwable ignored) {
        } finally {
            try { r.release(); } catch (Throwable ignored) { }
        }
        if (title == null || title.trim().isEmpty()) {
            String n = fileName == null ? "Sin título" : fileName;
            int dot = n.lastIndexOf('.');
            if (dot > 0) n = n.substring(0, dot);
            title = n.replace('_', ' ');
        }
        return new Track(uri, clean(title, "Sin título"), clean(artist, UNKNOWN_ARTIST),
                clean(album, UNKNOWN_ALBUM), dur, false, 0);
    }

    // -------------------------------------------------------------------- caché

    private File cacheFile() { return new File(ctx.getFilesDir(), "library.tsv"); }

    private static String esc(String s) { return s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' '); }

    private void writeCache(List<Track> list) {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(cacheFile()))) {
            for (Track t : list) {
                w.write(esc(t.key) + "\t" + esc(t.title) + "\t" + esc(t.artist) + "\t" + esc(t.album) + "\t"
                        + t.durationMs + "\t" + (t.store ? 1 : 0) + "\t" + t.albumId + "\n");
            }
        } catch (Throwable ignored) {
        }
    }

    private List<Track> readCache() {
        List<Track> out = new ArrayList<>();
        File f = cacheFile();
        if (!f.exists()) return out;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.split("\t", -1);
                if (p.length < 7) continue;
                out.add(new Track(Uri.parse(p[0]), p[1], p[2], p[3], Long.parseLong(p[4]), "1".equals(p[5]),
                        Long.parseLong(p[6])));
            }
        } catch (Throwable ignored) {
        }
        return out;
    }
}
