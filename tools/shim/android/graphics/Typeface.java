package android.graphics;
public class Typeface {
    public static final int NORMAL = 0, BOLD = 1, ITALIC = 2, BOLD_ITALIC = 3;
    public static final Typeface DEFAULT = new Typeface(0), DEFAULT_BOLD = new Typeface(1);
    public final int style;
    Typeface(int s) { style = s; }
    public static Typeface create(Typeface f, int style) { return new Typeface(style); }
}
