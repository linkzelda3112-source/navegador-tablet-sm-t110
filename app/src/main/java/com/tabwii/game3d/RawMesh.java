package com.tabwii.game3d;

import android.graphics.Bitmap;

/**
 * Datos de un modelo 3D ya leídos del archivo .obj, pero todavía sin subir a la tarjeta gráfica
 * (eso solo se puede hacer desde el hilo de OpenGL, así que este objeto viaja del hilo que lee
 * archivos al hilo de render a través de {@link GameRenderer}).
 *
 * Los vértices van "intercalados": por cada vértice, 3 números de posición (x,y,z), 3 de normal
 * (para la luz) y 2 de coordenada de textura (u,v) — 8 números (floats) por vértice, uno detrás
 * de otro. No se usan índices: cada triángulo repite sus 3 vértices completos, que es más simple
 * de programar aunque use un poco más de memoria (para modelos pequeños no importa).
 */
public class RawMesh {
    public static final int FLOATS_PER_VERTEX = 8; // x,y,z, nx,ny,nz, u,v

    public final float[] interleavedVertexData;
    public final int vertexCount;
    public final String sourceName;

    /** Puede ser null si el modelo no trae textura (se dibuja con un color liso). */
    public final Bitmap texture;
    public final float colorR, colorG, colorB;

    public RawMesh(float[] interleavedVertexData, int vertexCount, String sourceName,
                    Bitmap texture, float colorR, float colorG, float colorB) {
        this.interleavedVertexData = interleavedVertexData;
        this.vertexCount = vertexCount;
        this.sourceName = sourceName;
        this.texture = texture;
        this.colorR = colorR;
        this.colorG = colorG;
        this.colorB = colorB;
    }
}
