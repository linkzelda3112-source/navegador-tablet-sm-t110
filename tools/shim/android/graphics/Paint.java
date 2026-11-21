package android.graphics;
public class Paint {
    public static final int ANTI_ALIAS_FLAG = 1;
    public enum Style { FILL, STROKE, FILL_AND_STROKE }
    public enum Join { ROUND, MITER, BEVEL }
    public enum Cap { ROUND, BUTT, SQUARE }
    public Style style = Style.FILL; public int color = 0xFF000000; public float width = 1f;
    public Join join = Join.MITER; public Cap cap = Cap.BUTT;
    public Paint() {}
    public Paint(int flags) {}
    public void setStyle(Style s) { style = s; }
    public void setColor(int c) { color = c; }
    public void setStrokeWidth(float w) { width = w; }
    public void setStrokeJoin(Join j) { join = j; }
    public void setStrokeCap(Cap c) { cap = c; }
    public void setAlpha(int a) { color = (color & 0xFFFFFF) | (a << 24); }
}
