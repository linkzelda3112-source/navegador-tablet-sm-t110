package com.animestriker;

import android.graphics.Canvas;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class Preview2 {
    interface Draw { void go(Canvas c); }
    static void shot(String f, Draw d) throws Exception {
        BufferedImage img = new BufferedImage(2400, 1080, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics(); Canvas c = new Canvas(g); d.go(c); g.dispose();
        ImageIO.write(img, "png", new File(f));
    }
    public static void main(String[] a) throws Exception {
        String o = a[0];
        Art.init(); Ui ui = new Ui(); ui.resize(2400, 1080);
        shot(o + "/title.png", c -> ui.drawTitle(c, 3.3f));
        shot(o + "/teams.png", c -> ui.drawTeams(c, 2f, "ELIGE TU EQUIPO", 1, -1));
        Match m = new Match(Teams.ALL[0], Teams.ALL[1], 0, 90f, 0.5f, true);
        m.score[0] = 2; m.score[1] = 1;
        shot(o + "/result.png", c -> ui.drawResult(c, 2f, m, "¡VICTORIA!", "¡Qué partidazo!", "CONTINUAR"));
        shot(o + "/cup.png", c -> ui.drawCup(c, 2f, new int[]{0, 2, 3, 5}, 0, new boolean[]{true, true}, "Semifinal contra Azure Wolves"));

        // prueba con jugador humano y entradas aleatorias
        java.util.Random r = new java.util.Random(3);
        int goals = 0, shots = 0, nan = 0;
        for (int g = 0; g < 6; g++) {
            Match mm = new Match(Teams.ALL[g], Teams.ALL[(g + 2) % 6], 0, 60f, 1f, false);
            mm.rnd.setSeed(g);
            int steps = 0; float ix = 0, iy = 0;
            while (mm.state != Match.ST_END && steps < 60 * 200) {
                if (steps % 20 == 0) { ix = r.nextFloat() * 2 - 1; iy = r.nextFloat() * 2 - 1; mm.inSprint = r.nextBoolean(); }
                mm.inX = ix; mm.inY = iy;
                int k = r.nextInt(60);
                if (k == 0) mm.pressA = true; if (k == 1) mm.pressB = true; if (k == 2) mm.pressC = true; if (k == 3) mm.pressD = true;
                mm.inShoot = (steps / 25) % 4 == 0;
                mm.update(1f / 60f); steps++;
                if (Float.isNaN(mm.bx)) nan++;
            }
            goals += mm.score[0] + mm.score[1]; shots += mm.shots[0];
            System.out.println("humano " + mm.score[0] + "-" + mm.score[1] + " fin=" + (mm.state == Match.ST_END));
        }
        System.out.println("nan=" + nan + " tirosHumano=" + shots);
    }
}
