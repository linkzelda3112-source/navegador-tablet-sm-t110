package com.tabwii.game3d;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee un modelo 3D en formato .obj (el formato de texto más simple y común para modelos 3D;
 * Blender y casi cualquier programa de modelado lo pueden exportar) y lo convierte en un
 * {@link RawMesh} listo para mandar a la tarjeta gráfica.
 *
 * Solo entiende lo esencial de .obj: vértices (v), normales (vn), coordenadas de textura (vt),
 * caras (f, con 3 o más vértices — las de más de 3 se parten en triángulos) y, del .mtl, el color
 * (Kd) o la imagen de textura (map_Kd). Es a propósito simple: cualquiera que sepa Java puede leer
 * este archivo de arriba a abajo y entender exactamente qué hace.
 */
public class ObjLoader {

    public static RawMesh load(File objFile, File mtlFile, File modelDir, String displayName) throws Exception {
        List<float[]> positions = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        List<float[]> texCoords = new ArrayList<>();
        // Cada entrada es un triángulo ya triangulado: 3 vértices, cada uno [posIdx, texIdx, normIdx] (base 0, -1 = no existe)
        List<int[][]> triangles = new ArrayList<>();

        String mtlRef = null;
        for (String rawLine : readLines(objFile)) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split("\\s+");
            switch (parts[0]) {
                case "v":
                    positions.add(new float[]{f(parts[1]), f(parts[2]), f(parts[3])});
                    break;
                case "vn":
                    normals.add(new float[]{f(parts[1]), f(parts[2]), f(parts[3])});
                    break;
                case "vt":
                    texCoords.add(new float[]{f(parts[1]), f(parts[2])});
                    break;
                case "f": {
                    int[][] faceVerts = new int[parts.length - 1][3];
                    for (int i = 1; i < parts.length; i++) {
                        faceVerts[i - 1] = parseFaceVertex(parts[i], positions.size(), texCoords.size(), normals.size());
                    }
                    // Si la cara tiene más de 3 vértices (ej. un cuadrado), se parte en triángulos
                    // tipo "abanico": (0,1,2), (0,2,3), (0,3,4)...
                    for (int i = 1; i + 1 < faceVerts.length; i++) {
                        triangles.add(new int[][]{faceVerts[0], faceVerts[i], faceVerts[i + 1]});
                    }
                    break;
                }
                case "mtllib":
                    mtlRef = parts[1];
                    break;
                default:
                    // Lo demás (grupos, suavizado, etc.) no hace falta para este editor sencillo.
                    break;
            }
        }

        // Color/textura: primero se intenta con el .mtl que nos pasaron explícitamente; si no,
        // con el que referencia el propio .obj (mtllib).
        float[] color = {0.75f, 0.75f, 0.8f};
        Bitmap texture = null;
        File actualMtl = mtlFile;
        if (actualMtl == null && mtlRef != null) {
            File candidate = new File(modelDir, mtlRef);
            if (candidate.exists()) actualMtl = candidate;
        }
        if (actualMtl != null && actualMtl.exists()) {
            MtlResult mtl = readMtl(actualMtl, modelDir);
            if (mtl.color != null) color = mtl.color;
            if (mtl.textureFile != null && mtl.textureFile.exists()) {
                texture = BitmapFactory.decodeFile(mtl.textureFile.getAbsolutePath());
            }
        }

        boolean hasNormals = !normals.isEmpty();
        boolean hasTexCoords = !texCoords.isEmpty();

        float[] data = new float[triangles.size() * 3 * RawMesh.FLOATS_PER_VERTEX];
        int cursor = 0;
        for (int[][] tri : triangles) {
            // Normal "plana" del triángulo, usada si el .obj no trae normales por vértice.
            float[] flat = hasNormals ? null : faceNormal(
                    positions.get(tri[0][0]), positions.get(tri[1][0]), positions.get(tri[2][0]));
            for (int[] v : tri) {
                float[] pos = positions.get(v[0]);
                float[] n = hasNormals && v[2] >= 0 ? normals.get(v[2]) : flat;
                float[] uv = hasTexCoords && v[1] >= 0 ? texCoords.get(v[1]) : new float[]{0f, 0f};

                data[cursor++] = pos[0];
                data[cursor++] = pos[1];
                data[cursor++] = pos[2];
                data[cursor++] = n[0];
                data[cursor++] = n[1];
                data[cursor++] = n[2];
                data[cursor++] = uv[0];
                data[cursor++] = uv[1];
            }
        }

        return new RawMesh(data, triangles.size() * 3, displayName, texture, color[0], color[1], color[2]);
    }

    private static int[] parseFaceVertex(String token, int posCount, int texCount, int normCount) {
        // Formatos posibles: "1", "1/2", "1/2/3", "1//3"
        String[] bits = token.split("/", -1);
        int pos = resolveIndex(bits[0], posCount);
        int tex = (bits.length > 1 && bits[1].length() > 0) ? resolveIndex(bits[1], texCount) : -1;
        int norm = (bits.length > 2 && bits[2].length() > 0) ? resolveIndex(bits[2], normCount) : -1;
        return new int[]{pos, tex, norm};
    }

    /** Los índices de .obj empiezan en 1, y pueden ser negativos (cuentan desde el final). */
    private static int resolveIndex(String raw, int countSoFar) {
        int i = Integer.parseInt(raw);
        return i < 0 ? countSoFar + i : i - 1;
    }

    private static float[] faceNormal(float[] a, float[] b, float[] c) {
        float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
        float vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 0.00001f) return new float[]{0, 1, 0};
        return new float[]{nx / len, ny / len, nz / len};
    }

    private static class MtlResult {
        float[] color;
        File textureFile;
    }

    private static MtlResult readMtl(File mtlFile, File modelDir) throws Exception {
        MtlResult result = new MtlResult();
        for (String rawLine : readLines(mtlFile)) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split("\\s+");
            if (parts[0].equals("Kd") && parts.length >= 4) {
                result.color = new float[]{f(parts[1]), f(parts[2]), f(parts[3])};
            } else if (parts[0].equals("map_Kd") && parts.length >= 2) {
                result.textureFile = new File(modelDir, parts[parts.length - 1]);
            }
        }
        return result;
    }

    private static float f(String s) {
        return Float.parseFloat(s);
    }

    private static List<String> readLines(File file) throws Exception {
        List<String> lines = new ArrayList<>();
        InputStream in = new FileInputStream(file);
        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } finally {
            in.close();
        }
        return lines;
    }
}
