package com.tabwii.videoskids;

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Guarda en memoria la lista de videos ya encontrada, para que la pantalla principal y la de
 * reproducción compartan la misma lista sin tener que volver a leer el almacenamiento ni pasar
 * objetos complicados entre actividades.
 */
public class VideoLibrary {

    /** Carpeta donde el usuario copia sus videos desde la PC/USB. Se crea sola si no existe. */
    public static final File VIDEOS_FOLDER = new File(Environment.getExternalStorageDirectory(), "VideosKids");

    /** Categoría de los videos que están sueltos, directo en VIDEOS_FOLDER (sin subcarpeta). */
    public static final String DEFAULT_CATEGORY = "Todos";

    private static List<VideoItem> items = new ArrayList<>();

    public static List<VideoItem> getItems() {
        return items;
    }

    public static void setItems(List<VideoItem> newItems) {
        items = newItems;
    }

    /** Lista de categorías encontradas, en el orden en que aparecieron, con "Todos" primero. */
    public static List<String> categories() {
        Set<String> found = new LinkedHashSet<>();
        for (VideoItem item : items) {
            found.add(item.category);
        }
        List<String> result = new ArrayList<>();
        result.add(DEFAULT_CATEGORY);
        for (String c : found) {
            if (!c.equals(DEFAULT_CATEGORY)) result.add(c);
        }
        return result;
    }

    /** Lista de índices de otros videos, en orden aleatorio, para las recomendaciones. */
    public static List<Integer> shuffledOtherIndexes(int excludeIndex) {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (i != excludeIndex) indexes.add(i);
        }
        Collections.shuffle(indexes);
        return indexes;
    }
}
