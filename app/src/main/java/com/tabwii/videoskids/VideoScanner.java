package com.tabwii.videoskids;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Busca videos dentro de la carpeta de videos del tablet (ver {@link VideoLibrary#VIDEOS_FOLDER}),
 * incluyendo subcarpetas, sin tocar la red: todo esto es local. Cada subcarpeta de primer nivel se
 * usa como "categoría" (por ejemplo, "VideosKids/Caricaturas" o "VideosKids/Música"), para poder
 * mostrar pestañas como las de YouTube Kids. Los videos sueltos, directo en la carpeta principal,
 * quedan en la categoría {@link VideoLibrary#DEFAULT_CATEGORY}.
 */
public class VideoScanner {

    private static final String[] EXTENSIONS = {
            ".mp4", ".mkv", ".webm", ".3gp", ".avi", ".m4v"
    };

    public static List<VideoItem> scan(File root) {
        List<VideoItem> result = new ArrayList<>();
        scanInto(root, result, null);
        return result;
    }

    private static void scanInto(File dir, List<VideoItem> out, String category) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                // La categoría se fija en el primer nivel de subcarpeta y se hereda hacia abajo,
                // para no complicar la pantalla con demasiadas pestañas distintas.
                String childCategory = category == null ? f.getName() : category;
                scanInto(f, out, childCategory);
            } else if (isVideo(f.getName())) {
                out.add(new VideoItem(f, category == null ? VideoLibrary.DEFAULT_CATEGORY : category));
            }
        }
    }

    private static boolean isVideo(String name) {
        String lower = name.toLowerCase();
        for (String ext : EXTENSIONS) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }
}
