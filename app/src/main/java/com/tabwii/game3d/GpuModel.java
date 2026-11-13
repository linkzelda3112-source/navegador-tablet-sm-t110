package com.tabwii.game3d;

/**
 * Un modelo ya subido a la tarjeta gráfica (VBO + textura, si tiene). A diferencia de
 * {@link RawMesh}, esto solo existe mientras dure el contexto de OpenGL, y varios
 * {@link SceneObject} pueden compartir el mismo GpuModel (por ejemplo, si agregas el mismo modelo
 * dos veces a la escena, no hace falta subirlo dos veces).
 */
public class GpuModel {
    public final int vertexBufferId;
    public final int vertexCount;
    public final int textureId; // 0 si no tiene textura
    public final boolean hasTexture;
    public final float colorR, colorG, colorB;
    public final String name;

    public GpuModel(int vertexBufferId, int vertexCount, int textureId, boolean hasTexture,
                     float colorR, float colorG, float colorB, String name) {
        this.vertexBufferId = vertexBufferId;
        this.vertexCount = vertexCount;
        this.textureId = textureId;
        this.hasTexture = hasTexture;
        this.colorR = colorR;
        this.colorG = colorG;
        this.colorB = colorB;
        this.name = name;
    }
}
