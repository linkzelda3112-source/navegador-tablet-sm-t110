package android.graphics;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayDeque;
public class Canvas {
    public final Graphics2D g;
    private final ArrayDeque<AffineTransform> st = new ArrayDeque<>();
    public Canvas(Bitmap b) { this(b.img.createGraphics()); }
    public Canvas(Graphics2D g) {
        this.g = g;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }
    void paint(Paint p, Shape s) {
        if (p.shader != null && p.shader.awt != null) g.setPaint(p.shader.awt); else g.setPaint(new Color(p.color, true));
        if (p.style == Paint.Style.STROKE) {
            int cap = p.cap == Paint.Cap.ROUND ? BasicStroke.CAP_ROUND : p.cap == Paint.Cap.SQUARE ? BasicStroke.CAP_SQUARE : BasicStroke.CAP_BUTT;
            int jn = p.join == Paint.Join.ROUND ? BasicStroke.JOIN_ROUND : p.join == Paint.Join.BEVEL ? BasicStroke.JOIN_BEVEL : BasicStroke.JOIN_MITER;
            g.setStroke(new BasicStroke(Math.max(0.01f, p.sw), cap, jn, 4f));
            g.draw(s);
        } else g.fill(s);
    }
    public void drawRect(float l, float t, float r, float b, Paint p) { paint(p, new Rectangle2D.Float(l, t, r - l, b - t)); }
    public void drawRect(RectF r, Paint p) { drawRect(r.left, r.top, r.right, r.bottom, p); }
    public void drawRoundRect(RectF r, float rx, float ry, Paint p) { paint(p, new RoundRectangle2D.Float(r.left, r.top, r.right - r.left, r.bottom - r.top, rx * 2, ry * 2)); }
    public void drawCircle(float x, float y, float r, Paint p) { paint(p, new Ellipse2D.Float(x - r, y - r, r * 2, r * 2)); }
    public void drawOval(RectF r, Paint p) { paint(p, new Ellipse2D.Float(r.left, r.top, r.right - r.left, r.bottom - r.top)); }
    public void drawLine(float a, float b, float c, float d, Paint p) {
        Paint q = new Paint(); q.color = p.color; q.style = Paint.Style.STROKE; q.sw = p.sw; q.cap = p.cap; q.join = p.join; q.shader = p.shader;
        paint(q, new Line2D.Float(a, b, c, d));
    }
    public void drawPath(Path pa, Paint p) { paint(p, pa.p); }
    public void drawText(String s, float x, float y, Paint p) {
        Font f = new Font("Dialog", ((p.tf.style & 1) != 0 ? Font.BOLD : 0) | ((p.tf.style & 2) != 0 ? Font.ITALIC : 0), 100).deriveFont(p.ts);
        java.awt.font.GlyphVector gv = f.createGlyphVector(g.getFontRenderContext(), s);
        float w = (float) gv.getLogicalBounds().getWidth();
        float dx = p.align == Paint.Align.CENTER ? -w / 2 : p.align == Paint.Align.RIGHT ? -w : 0;
        paint(p, gv.getOutline(x + dx, y));
    }
    public void drawBitmap(Bitmap b, Rect src, RectF dst, Paint p) {
        int sw = b.getWidth(), sh = b.getHeight();
        int sx0 = src == null ? 0 : src.left, sy0 = src == null ? 0 : src.top, sx1 = src == null ? sw : src.right, sy1 = src == null ? sh : src.bottom;
        Object old = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        Composite oc = g.getComposite();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, p != null && p.filter ? RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (p != null) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (p.color >>> 24) / 255f));
        g.drawImage(b.img, Math.round(dst.left), Math.round(dst.top), Math.round(dst.right), Math.round(dst.bottom), sx0, sy0, sx1, sy1, null);
        g.setComposite(oc);
        if (old != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, old);
    }
    public void drawColor(int c) { g.setPaint(new Color(c, true)); g.fillRect(-10000, -10000, 20000, 20000); }
    public int save() { st.push(g.getTransform()); return st.size(); }
    public void restore() { g.setTransform(st.pop()); }
    public void translate(float x, float y) { g.translate(x, y); }
    public void scale(float x, float y) { g.scale(x, y); }
    public void rotate(float deg) { g.rotate(Math.toRadians(deg)); }
    public void rotate(float deg, float px, float py) { g.rotate(Math.toRadians(deg), px, py); }
}
