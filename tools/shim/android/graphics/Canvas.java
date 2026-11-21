package android.graphics;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayDeque;
public class Canvas {
    public final Graphics2D g;
    private final ArrayDeque<Object[]> st = new ArrayDeque<>();
    public Canvas(Graphics2D g) {
        this.g = g;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }
    public int save() { st.push(new Object[]{g.getTransform(), g.getClip()}); return st.size(); }
    public void restore() { Object[] o = st.pop(); g.setTransform((AffineTransform) o[0]); g.setClip((Shape) o[1]); }
    public void scale(float a, float b) { g.scale(a, b); }
    public void translate(float a, float b) { g.translate(a, b); }
    public void rotate(float d) { g.rotate(Math.toRadians(d)); }
    public void rotate(float d, float x, float y) { g.rotate(Math.toRadians(d), x, y); }
    public boolean clipPath(Path p) { g.clip(p.p); return true; }
    private void paint(Shape s, Paint pt) {
        g.setColor(new Color(pt.color, true));
        if (pt.style == Paint.Style.FILL) { g.fill(s); return; }
        int cap = pt.cap == Paint.Cap.ROUND ? BasicStroke.CAP_ROUND : pt.cap == Paint.Cap.SQUARE ? BasicStroke.CAP_SQUARE : BasicStroke.CAP_BUTT;
        int jn = pt.join == Paint.Join.ROUND ? BasicStroke.JOIN_ROUND : pt.join == Paint.Join.BEVEL ? BasicStroke.JOIN_BEVEL : BasicStroke.JOIN_MITER;
        g.setStroke(new BasicStroke(pt.width, cap, jn));
        g.draw(s);
    }
    public void drawPath(Path p, Paint pt) { paint(p.p, pt); }
    public void drawLine(float a, float b, float x, float y, Paint pt) { paint(new Line2D.Float(a, b, x, y), pt); }
    public void drawCircle(float x, float y, float r, Paint pt) { paint(new Ellipse2D.Float(x - r, y - r, 2 * r, 2 * r), pt); }
    public void drawOval(RectF r, Paint pt) { paint(new Ellipse2D.Float(r.left, r.top, r.right - r.left, r.bottom - r.top), pt); }
    public void drawRoundRect(RectF r, float rx, float ry, Paint pt) {
        paint(new RoundRectangle2D.Float(r.left, r.top, r.right - r.left, r.bottom - r.top, rx * 2, ry * 2), pt);
    }
}
