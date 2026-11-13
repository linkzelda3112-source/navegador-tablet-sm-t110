package com.tabwii.game3d;

/** Una instancia de un modelo colocado en la escena: su posición, rotación y tamaño. Varios
 *  SceneObject pueden apuntar al mismo {@link GpuModel} (el mismo modelo repetido varias veces). */
public class SceneObject {
    public final GpuModel model;
    public final String label;
    public volatile float x, y, z;
    public volatile float rotationY; // grados, girando sobre el eje vertical
    public volatile float scale = 1f;

    public SceneObject(GpuModel model, String label) {
        this.model = model;
        this.label = label;
    }
}
