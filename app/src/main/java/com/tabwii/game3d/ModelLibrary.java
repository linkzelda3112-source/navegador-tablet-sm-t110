package com.tabwii.game3d;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Busca los archivos .zip con modelos 3D que el usuario copió a la carpeta de la app, y sabe
 * descomprimirlos y convertirlos en un {@link RawMesh} usable. Cada .zip debe traer, sueltos o en
 * una subcarpeta, un archivo .obj y, si el modelo tiene textura, su .mtl y la imagen (.png/.jpg).
 */
public class ModelLibrary {

    private static final String FOLDER_NAME = "Modelos3D";

    /** Carpeta donde el usuario copia sus .zip de modelos. Visible en un administrador de
     *  archivos dentro de Android/data/com.tabwii.game3d/files/Modelos3D — no requiere ningún
     *  permiso de almacenamiento. */
    public static File folder(Context context) {
        File dir = new File(context.getExternalFilesDir(null), FOLDER_NAME);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static List<File> listZips(Context context) {
        List<File> result = new ArrayList<>();
        File[] files = folder(context).listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().toLowerCase().endsWith(".zip")) result.add(f);
            }
        }
        return result;
    }

    /** Extrae (si hace falta) y carga el modelo de un .zip. Tarda, así que hay que llamarlo fuera
     *  del hilo principal. */
    public static RawMesh load(Context context, File zipFile) throws Exception {
        String baseName = zipFile.getName().replaceAll("(?i)\\.zip$", "");
        File extractDir = new File(context.getCacheDir(), "modelos_extraidos/" + baseName);
        if (!extractDir.exists() || isEmpty(extractDir)) {
            extractDir.mkdirs();
            extractZip(zipFile, extractDir);
        }

        File objFile = findFirstObj(extractDir);
        if (objFile == null) {
            throw new Exception("El zip \"" + zipFile.getName() + "\" no trae ningún archivo .obj adentro.");
        }
        return ObjLoader.load(objFile, null, extractDir, baseName);
    }

    private static boolean isEmpty(File dir) {
        File[] files = dir.listFiles();
        return files == null || files.length == 0;
    }

    private static File findFirstObj(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.isFile() && f.getName().toLowerCase().endsWith(".obj")) return f;
        }
        return null;
    }

    /** Descomprime TODO el .zip dentro de una sola carpeta plana (sin subcarpetas), para que el
     *  .obj y su .mtl/textura queden siempre juntos sin importar cómo venían empaquetados. */
    private static void extractZip(File zipFile, File targetDir) throws Exception {
        InputStream fis = new java.io.FileInputStream(zipFile);
        ZipInputStream zis = new ZipInputStream(fis);
        try {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String flatName = new File(entry.getName()).getName();
                if (flatName.isEmpty()) continue;
                File outFile = new File(targetDir, flatName);
                OutputStream out = new FileOutputStream(outFile);
                try {
                    int read;
                    while ((read = zis.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                } finally {
                    out.close();
                }
            }
        } finally {
            zis.close();
        }
    }
}
