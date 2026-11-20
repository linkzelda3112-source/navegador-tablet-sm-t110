import com.valleradial.Game;
import com.valleradial.World;

/** Prueba de fisica en una JVM normal: javac -d out app/src/main/java/com/valleradial/{World,Game}.java tools/SimTest.java */
public class SimTest {
    static int fails = 0;
    static void check(boolean ok, String what) {
        System.out.println((ok ? "OK   " : "FAIL ") + what);
        if (!ok) fails++;
    }

    public static void main(String[] a) {
        Game g = new Game();
        System.out.println("props=" + g.world.props.size());
        // 1) cae y se queda en el suelo
        for (int i = 0; i < 120; i++) g.update(1 / 60f);
        float d = (float) Math.hypot(g.px, g.py);
        float rg = World.groundRadius((float) Math.atan2(g.py, g.px), g.pz);
        check(g.onGround && Math.abs(rg - d) < 0.2f, "aterriza y se queda en el suelo (h=" + (rg - d) + ")");

        // 2) caminar de lado: la camara "arriba" rota -> las paredes se vuelven piso
        float up0y = g.uy;
        g.moveX = 1f; g.moveY = 0f;
        float maxH = 0, minUpY = 9;
        boolean nan = false;
        for (int i = 0; i < 60 * 40; i++) {
            g.update(1 / 60f);
            float dd = (float) Math.hypot(g.px, g.py);
            float r2 = World.groundRadius((float) Math.atan2(g.py, g.px), g.pz);
            maxH = Math.max(maxH, Math.abs(r2 - dd));
            minUpY = Math.min(minUpY, g.uy);
            if (Float.isNaN(g.px) || Float.isNaN(g.ex)) nan = true;
        }
        double ang = Math.toDegrees(Math.atan2(g.py, g.px));
        System.out.println("angulo final=" + ang + " up=(" + g.ux + "," + g.uy + ") altura max sobre suelo=" + maxH);
        check(!nan, "sin NaN");
        check(maxH < 3.5f, "pegado al suelo mientras camina por la pared");
        check(Math.abs(g.uy - up0y) > 0.3f || Math.abs(g.ux) > 0.3f, "el arriba local rota al caminar por la pared");

        // 3) camara ortonormal
        float dl = (float) Math.sqrt(g.dx * g.dx + g.dy * g.dy + g.dz * g.dz);
        check(Math.abs(dl - 1f) < 1e-3f, "direccion de camara unitaria");

        // 4) mirar a la derecha gira forward
        g.moveX = 0; g.moveY = 0;
        for (int i = 0; i < 60; i++) g.update(1 / 60f);
        float fx0 = g.fx, fy0 = g.fy, fz0 = g.fz;
        g.addLook(0.5f, 0f);
        g.update(1 / 60f);
        float dot = fx0 * g.fx + fy0 * g.fy + fz0 * g.fz;
        check(Math.abs(dot - Math.cos(0.5)) < 0.02, "giro de yaw correcto (dot=" + dot + ")");

        // 5) gancho: dispara a un punto lejano y avanza
        g.reset(false);
        for (int i = 0; i < 60; i++) g.update(1 / 60f);
        g.addLook(0f, 0.12f);
        g.update(1 / 60f);
        float z0 = g.pz;
        g.hookHeld = true;
        int attached = 0;
        for (int i = 0; i < 60 * 3; i++) {
            g.update(1 / 60f);
            if (g.hookState == Game.HOOK_ATTACHED) attached++;
        }
        g.hookHeld = false;
        System.out.println("gancho enganchado frames=" + attached + " z " + z0 + " -> " + g.pz);
        check(attached > 10, "el gancho se engancha");
        check(!Float.isNaN(g.px), "gancho sin NaN");

        // 6) raycast al suelo y disparo a enemigo
        g.reset(false);
        for (int i = 0; i < 30; i++) g.update(1 / 60f);
        Game.Enemy e = g.enemies.get(0);
        float eu = (float) Math.hypot(e.x, e.y);
        // pon al enemigo delante del jugador
        float dd = 15f;
        float tx = g.px + g.fx * dd, ty = g.py + g.fy * dd, tz = g.pz + g.fz * dd;
        float tr = (float) Math.hypot(tx, ty);
        float th = (float) Math.atan2(ty, tx);
        float rr = World.groundRadius(th, tz);
        e.x = tx / tr * rr; e.y = ty / tr * rr; e.z = tz; e.alive = true; e.hp = 3;
        // apunta con pitch para que el centro (1.2 arriba) quede en la mira
        g.pitch = (float) Math.atan2(1.2 - 1.7, dd);
        g.update(1 / 600f);
        int kills0 = g.score;
        g.fireHeld = true;
        for (int i = 0; i < 60 * 2; i++) {
            // mantener al enemigo quieto delante
            e.x = tx / tr * rr; e.y = ty / tr * rr; e.z = tz;
            g.update(1 / 60f);
        }
        g.fireHeld = false;
        check(g.score > kills0, "disparar a un enemigo lo elimina (score=" + g.score + ")");

        System.out.println(fails == 0 ? "TODO BIEN" : ("FALLOS: " + fails));
        System.exit(fails == 0 ? 0 : 1);
    }
}
