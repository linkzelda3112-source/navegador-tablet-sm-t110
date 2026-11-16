package com.lumina;

import android.net.Uri;

/** Una canción de la biblioteca. */
final class Track {
    final String key;
    final Uri uri;
    final String title, artist, album;
    final long durationMs;
    /** true si viene de MediaStore (búsqueda en todo el teléfono); false si viene de una carpeta elegida. */
    final boolean store;
    final long albumId;
    /** Clave normalizada (título|artista) para valoraciones; se calcula la primera vez que se pide. */
    String pkey;

    Track(Uri uri, String title, String artist, String album, long durationMs, boolean store, long albumId) {
        this.key = uri.toString();
        this.uri = uri;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.durationMs = durationMs;
        this.store = store;
        this.albumId = albumId;
    }
}
