package android.graphics;
import java.awt.geom.Point2D;
public class LinearGradient extends Shader {
    public LinearGradient(float x0, float y0, float x1, float y1, int[] colors, float[] pos, TileMode m) {
        java.awt.Color[] cs = new java.awt.Color[colors.length];
        for (int i = 0; i < cs.length; i++) cs[i] = new java.awt.Color(colors[i], true);
        awt = new java.awt.LinearGradientPaint(new Point2D.Float(x0, y0), new Point2D.Float(x1, y1 == y0 ? y1 + 0.001f : y1), pos, cs);
    }
}
