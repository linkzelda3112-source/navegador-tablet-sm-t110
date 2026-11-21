import com.valleradial.Game;
import android.graphics.Canvas;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;

/** Vista previa de las manos en una JVM normal con un shim de android.graphics sobre java.awt. */
public class HandsPreview {
    public static void main(String[] a) throws Exception {
        Class<?> hc = Class.forName("com.valleradial.Hands");
        Constructor<?> k = hc.getDeclaredConstructor(); k.setAccessible(true);
        Object hands = k.newInstance();
        Method draw = hc.getDeclaredMethod("draw", Canvas.class, float.class, float.class, Game.class, float.class);
        Method anchor = hc.getDeclaredMethod("setAnchor", float.class, float.class);
        draw.setAccessible(true); anchor.setAccessible(true);
        int W = 1280, H = 720;
        String[] names = {"idle", "fly1", "fly2", "attached", "release", "fire"};
        for (String n : names) {
            Game g = new Game();
            g.onGround = true;
            anchor.invoke(hands, 330f, 150f);
            switch (n) {
                case "fly1": g.hookState = Game.HOOK_FLY; g.hookProg = 0.15f; break;
                case "fly2": g.hookState = Game.HOOK_FLY; g.hookProg = 0.6f; break;
                case "attached": g.hookState = Game.HOOK_ATTACHED; break;
                case "release": g.releaseT = 0.3f; break;
                case "fire": g.recoil = 0.95f; break;
            }
            BufferedImage im = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = im.createGraphics();
            g2.setPaint(new GradientPaint(0, 0, new Color(0xB9D8A0), 0, H, new Color(0x6E9A58)));
            g2.fillRect(0, 0, W, H);
            Canvas c = new Canvas(g2);
            draw.invoke(hands, c, (float) W, (float) H, g, 1.0f);
            javax.imageio.ImageIO.write(im, "png", new java.io.File(a[0] + "/" + n + ".png"));
        }
    }
}
