package com.lumina;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;

/** Carátulas elegidas por el usuario (de una imagen del teléfono o buscadas en línea). */
final class CoverStore {
    private final Context ctx;
    private final SharedPreferences sp;
    private final File dir;

    CoverStore(Context c) {
        ctx = c.getApplicationContext();
        sp = ctx.getSharedPreferences("covers", Context.MODE_PRIVATE);
        dir = new File(ctx.getFilesDir(), "covers");
        dir.mkdirs();
    }

    /** Las canciones de un mismo álbum comparten carátula; sin álbum, es por canción. */
    static String keyOf(Track t) {
        if (!t.album.equals(Library.UNKNOWN_ALBUM)) {
            return "alb|" + t.album.toLowerCase(Locale.ROOT) + "|" + t.artist.toLowerCase(Locale.ROOT);
        }
        return "trk|" + t.key;
    }

    File get(Track t) {
        String n = sp.getString(keyOf(t), null);
        if (n == null) return null;
        File f = new File(dir, n);
        return f.exists() ? f : null;
    }

    boolean has(Track t) { return get(t) != null; }

    boolean setFromUri(Track t, Uri u) {
        try (InputStream in = ctx.getContentResolver().openInputStream(u)) {
            if (in == null) return false;
            return setFromBytes(t, readAll(in));
        } catch (Throwable e) {
            return false;
        }
    }

    boolean setFromBytes(Track t, byte[] data) {
        Bitmap bm = decodeSampled(data, 1024);
        if (bm == null) return false;
        String key = keyOf(t);
        String name = Integer.toHexString(key.hashCode()) + "_" + System.currentTimeMillis() + ".jpg";
        File f = new File(dir, name);
        try (FileOutputStream o = new FileOutputStream(f)) {
            bm.compress(Bitmap.CompressFormat.JPEG, 90, o);
        } catch (Throwable e) {
            return false;
        }
        String old = sp.getString(key, null);
        if (old != null) new File(dir, old).delete();
        sp.edit().putString(key, name).apply();
        return true;
    }

    void remove(Track t) {
        String key = keyOf(t);
        String old = sp.getString(key, null);
        if (old != null) new File(dir, old).delete();
        sp.edit().remove(key).apply();
    }

    static Bitmap decodeSampled(byte[] data, int max) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, o);
            int side = Math.min(o.outWidth, o.outHeight);
            int sample = 1;
            while (side / (sample * 2) >= max) sample *= 2;
            o = new BitmapFactory.Options();
            o.inSampleSize = sample;
            return BitmapFactory.decodeByteArray(data, 0, data.length, o);
        } catch (Throwable t) {
            return null;
        }
    }

    static Bitmap decodeFile(File f, int px) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getPath(), o);
            int side = Math.min(o.outWidth, o.outHeight);
            int sample = 1;
            while (side / (sample * 2) >= px) sample *= 2;
            o = new BitmapFactory.Options();
            o.inSampleSize = sample;
            return BitmapFactory.decodeFile(f.getPath(), o);
        } catch (Throwable t) {
            return null;
        }
    }

    static byte[] readAll(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        return bo.toByteArray();
    }
}
