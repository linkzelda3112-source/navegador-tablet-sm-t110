package com.tabwii.pdfreader;

import java.io.File;

/** Representa un PDF encontrado en el almacenamiento del tablet. */
public class PdfFileItem {
    public final File file;
    public final String title;

    public PdfFileItem(File file) {
        this.file = file;
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        this.title = dot > 0 ? name.substring(0, dot) : name;
    }

    /** Tamaño legible del archivo, por ejemplo "2.3 MB" o "480 KB". */
    public String readableSize() {
        long bytes = file.length();
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.0f KB", kb);
        double mb = kb / 1024.0;
        return String.format("%.1f MB", mb);
    }
}
