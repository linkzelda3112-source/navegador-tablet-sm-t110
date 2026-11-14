package com.lumina;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.util.Size;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Carga portadas en segundo plano (la incrustada en el archivo o la del álbum) y las guarda en memoria. */
final class ArtCache {
    private final Context ctx;
    private final LruCache<String, Bitmap> cache = new LruCache<>(80);
    private final Set<String> pending = new HashSet<>();
    private final Set<String> failed = new HashSet<>();
    private final ExecutorService exec = Executors.newFixedThreadPool(2);
    private final Handler main = new Handler(Looper.getMainLooper());

    ArtCache(Context ctx) { this.ctx = ctx.getApplicationContext(); }

    private static String k(Track t, int px) { return t.key + "@" + px; }

    /** Devuelve la portada si ya está en memoria; si no, la pide y avisa con 'done' cuando llegue. */
    Bitmap get(Track t, int px, Runnable done) {
        if (t == null) return null;
        String key = k(t, px);
        Bitmap b = cache.get(key);
        if (b != null) return b;
        synchronized (this) {
            if (failed.contains(key) || pending.contains(key)) return null;
            pending.add(key);
        }
        exec.execute(() -> {
            Bitmap bm = load(t, px);
            synchronized (this) {
                pending.remove(key);
                if (bm == null) failed.add(key);
            }
            if (bm != null) cache.put(key, bm);
            if (bm != null && done != null) main.post(done);
        });
        return null;
    }

    private Bitmap load(Track t, int px) {
        Bitmap bm = null;
        if (t.store && Build.VERSION.SDK_INT >= 29) {
            try {
                bm = ctx.getContentResolver().loadThumbnail(t.uri, new Size(px, px), null);
            } catch (Throwable ignored) {
            }
        }
        if (bm == null) bm = embedded(t.uri, px);
        if (bm == null && t.store && t.albumId > 0) {
            try {
                Uri art = Uri.parse("content://media/external/audio/albumart/" + t.albumId);
                InputStream in = ctx.getContentResolver().openInputStream(art);
                if (in != null) {
                    try {
                        byte[] data = readAll(in);
                        bm = decode(data, px);
                    } finally {
                        in.close();
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return bm;
    }

    private Bitmap embedded(Uri uri, int px) {
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            r.setDataSource(ctx, uri);
            byte[] data = r.getEmbeddedPicture();
            if (data == null) return null;
            return decode(data, px);
        } catch (Throwable t) {
            return null;
        } finally {
            try { r.release(); } catch (Throwable ignored) { }
        }
    }

    private static Bitmap decode(byte[] data, int px) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, o);
        int side = Math.min(o.outWidth, o.outHeight);
        int sample = 1;
        while (side / (sample * 2) >= px) sample *= 2;
        o = new BitmapFactory.Options();
        o.inSampleSize = sample;
        return BitmapFactory.decodeByteArray(data, 0, data.length, o);
    }

    private static byte[] readAll(InputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        return bo.toByteArray();
    }
}
