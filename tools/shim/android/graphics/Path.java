package android.graphics;
public class Path {
    public final java.awt.geom.Path2D.Float p = new java.awt.geom.Path2D.Float();
    public void reset() { p.reset(); }
    public void moveTo(float x, float y) { p.moveTo(x, y); }
    public void lineTo(float x, float y) { p.lineTo(x, y); }
    public void quadTo(float a, float b, float x, float y) { p.quadTo(a, b, x, y); }
    public void cubicTo(float a, float b, float c, float d, float x, float y) { p.curveTo(a, b, c, d, x, y); }
    public void close() { p.closePath(); }
}
