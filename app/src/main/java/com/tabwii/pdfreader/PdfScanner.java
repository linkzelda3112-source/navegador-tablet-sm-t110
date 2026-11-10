package com.tabwii.pdfreader;

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Busca archivos PDF en el almacenamiento del tablet, sin tocar la red: todo esto es local.
 * Revisa dos lugares: la carpeta propia de la app ({@link #MIS_PDF_FOLDER}, con subcarpetas) y la
 * carpeta de Descargas (solo el primer nivel, para no tardar escaneando todo el almacenamiento),
 * ya que ahí suelen terminar los PDF que se reciben por correo o se bajan con el navegador.
 */
public class PdfScanner {

    /** Carpeta propia de la app, donde el usuario puede copiar u organizar sus PDF a mano. */
    public static final File MIS_PDF_FOLDER = new File(Environment.getExternalStorageDirectory(), "MisPDF");

    public static List<PdfFileItem> scan() {
        // Se usa un mapa por ruta para no mostrar el mismo archivo dos veces si, por ejemplo, el
        // usuario copió un PDF tanto a MisPDF como a Descargas.
        Map<String, PdfFileItem> found = new LinkedHashMap<>();

        scanInto(MIS_PDF_FOLDER, found, true);

        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        scanInto(downloads, found, false);

        List<PdfFileItem> result = new ArrayList<>(found.values());
        Collections.sort(result, new Comparator<PdfFileItem>() {
            @Override
            public int compare(PdfFileItem a, PdfFileItem b) {
                return a.title.compareToIgnoreCase(b.title);
            }
        });
        return result;
    }

    private static void scanInto(File dir, Map<String, PdfFileItem> out, boolean recursive) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                if (recursive) scanInto(f, out, true);
            } else if (isPdf(f.getName())) {
                try {
                    String key = f.getCanonicalPath();
                    if (!out.containsKey(key)) {
                        out.put(key, new PdfFileItem(f));
                    }
                } catch (Exception e) {
                    // Ruta rara/enlace roto: se ignora este archivo en vez de tronar el escaneo.
                }
            }
        }
    }

    private static boolean isPdf(String name) {
        return name.toLowerCase().endsWith(".pdf");
    }
}
