package android.graphics;
public class Paint {
    public static final int ANTI_ALIAS_FLAG = 1;
    public enum Style { FILL, STROKE, FILL_AND_STROKE }
    public enum Cap { BUTT, ROUND, SQUARE }
    public enum Join { MITER, ROUND, BEVEL }
    public enum Align { LEFT, CENTER, RIGHT }
    int color = 0xFF000000; Style style = Style.FILL; float sw = 1; Cap cap = Cap.BUTT; Join join = Join.MITER;
    float ts = 12; Typeface tf = Typeface.DEFAULT; Align align = Align.LEFT; Shader shader;
    public Paint() {}
    public Paint(int f) {}
    public void setColor(int c) { color = c; }
    public void setAlpha(int a) { color = (a << 24) | (color & 0xFFFFFF); }
    public void setStyle(Style s) { style = s; }
    public void setStrokeWidth(float w) { sw = w; }
    public void setStrokeCap(Cap c) { cap = c; }
    public void setStrokeJoin(Join j) { join = j; }
    public void setTextSize(float s) { ts = s; }
    public void setTypeface(Typeface t) { tf = t; }
    public void setTextAlign(Align a) { align = a; }
    public void setShader(Shader s) { shader = s; }
    public void setAntiAlias(boolean b) {}
    public float measureText(String s) {
        java.awt.Font f = new java.awt.Font("Dialog", ((tf.style & 1) != 0 ? java.awt.Font.BOLD : 0) | ((tf.style & 2) != 0 ? java.awt.Font.ITALIC : 0), 100).deriveFont(ts);
        return (float) f.getStringBounds(s, new java.awt.font.FontRenderContext(null, true, true)).getWidth();
    }
}
