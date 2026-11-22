package com.animestriker;

import android.graphics.Canvas;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Renderiza capturas del juego con el shim de AWT (solo para revisar el arte en el servidor). */
public class Preview {
    static final int W = 2400, H = 1080;

    static void shot(Renderer r, Match m, Pad pad, String file, float dt) throws Exception {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(java.awt.Color.BLACK); g.fillRect(0, 0, W, H);
        Canvas c = new Canvas(g);
        r.drawMatch(c, m, dt, pad);
        g.dispose();
        ImageIO.write(img, "png", new File(file));
    }

    public static void main(String[] a) throws Exception {
        String out = a[0];
        System.setProperty("java.awt.headless", "true");
        Renderer r = new Renderer(); r.resize(W, H);
        Pad pad = new Pad(); pad.layout(W, H);
        Match m = new Match(Teams.ALL[0], Teams.ALL[1], 0, 90f, 0.5f, false);
        m.listener = (t, p) -> r.event(t, m, p);
        for (int i = 0; i < 60 * 6; i++) {
            m.inX = 0.9f; m.inY = 0.2f; m.inSprint = true;
            m.update(1f / 60f);
            r.tickParticles(1f / 60f); r.time += 1f / 60f;
        }
        for (int i = 0; i < 30; i++) r.camX += (m.bx - r.camX) * 0.1f;
        m.inShoot = true; m.prevShoot = true;
        if (m.ctrl != null) m.ctrl.charge = 0.8f;
        shot(r, m, pad, out + "/play.png", 1f / 60f);
        // corte especial
        Match.Player sh = m.pl[0][5];
        m.state = Match.ST_CUTIN; m.specialShooter = sh; m.cutinT = 0.7f; m.stateT = 0.6f;
        shot(r, m, pad, out + "/cutin.png", 0.001f);
        // gol
        m.state = Match.ST_GOAL; m.lastScoringTeam = 0; m.lastScorer = sh; m.stateT = 3.2f; m.goalSide = 1;
        r.confetti(m, 0);
        for (int i = 0; i < 40; i++) r.tickParticles(0.03f);
        shot(r, m, pad, out + "/goal.png", 0.001f);
        // primer plano de los 12 personajes
        BufferedImage img = new BufferedImage(W, 700, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics(); g.setColor(new java.awt.Color(0xFFB5E3FF)); g.fillRect(0, 0, W, 700);
        Canvas c = new Canvas(g);
        for (int t = 0; t < 2; t++) for (int i = 0; i < 6; i++) {
            Match.Player p = m.pl[t][i];
            Teams.Def d = Teams.ALL[t == 0 ? 0 : 3];
            p.flip = 1f; p.vx = p.vy = 0; p.anim = i * 0.6f; p.stun = 0; p.slide = 0; p.cheer = 0; p.charge = 0; p.actionT = 0; p.diveT = 0;
            Art.body(c, m, p, 200 + i * 380, 300 + t * 320, 70f, 1.0f + i * 0.3f, false);
        }
        g.dispose();
        ImageIO.write(img, "png", new File(out + "/chars.png"));
        System.out.println("ok");
    }
}
