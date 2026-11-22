package android.graphics;
public class Bitmap {
    public enum Config { ARGB_8888 }
    public final java.awt.image.BufferedImage img;
    Bitmap(int w, int h) { img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB); }
    public static Bitmap createBitmap(int w, int h, Config c) { return new Bitmap(w, h); }
    public int getWidth() { return img.getWidth(); }
    public int getHeight() { return img.getHeight(); }
}
