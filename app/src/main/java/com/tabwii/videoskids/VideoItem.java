package com.tabwii.videoskids;

import java.io.File;

/** Representa un video encontrado en la carpeta de videos del tablet. */
public class VideoItem {
    public final File file;
    public final String title;
    /** Nombre de la subcarpeta donde estaba el video (sirve como "categoría"), o "Todos" si
     *  estaba directo en la carpeta principal de videos. */
    public final String category;

    public VideoItem(File file, String category) {
        this.file = file;
        this.category = category;
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        this.title = dot > 0 ? name.substring(0, dot) : name;
    }
}
