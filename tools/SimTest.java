import com.valleradial.Game;
import com.valleradial.World;

/** Prueba de fisica en una JVM normal: javac -d out app/src/main/java/com/valleradial/{World,Game}.java tools/SimTest.java */
public class SimTest {
    static int fails = 0;

    static void check(boolean ok, String what) {
        System.out.println((ok ? "OK   " : "FAIL ") + what);
        if (!ok) fails++;
    }

    static float heightAbove(Game g) {
        float d = (float) Math.sqrt(g.px * g.px + g.py * g.py + g.pz * g.pz);
        return World.groundRadius(g.px / d, g.py / d, g.pz / d) - d;
    }

    public static void main(String[] a) {
        Game g = new Game();
        int[] counts = new int[8];
        for (World.Prop p : g.world.props) counts[p.kind]++;
        System.out.println("props=" + g.world.props.size() + " casas=" + counts[0] + " arboles=" + counts[1]
                + " rocas=" + counts[2] + " arbustos=" + counts[3] + " torres=" + counts[4]
                + " arcos=" + counts[5] + " molinos=" + counts[6] + " ruinas=" + counts[7]);
        check(counts[World.HOUSE] > 60 && counts[World.TOWER] > 12 && counts[World.ARCH] > 12
                && counts[World.WINDMILL] > 8 && counts[World.RUIN] > 15, "hay muchas estructuras de cada tipo");

        // apariciones sobre el camino
        float[] sp = World.spawnDir();
        check(World.pathDist(sp[0], sp[1], sp[2]) < 1f, "el jugador aparece sobre el camino");

        // props pegados al suelo
        boolean grounded = true;
        for (World.Prop p : g.world.props) {
            float d = (float) Math.sqrt(p.x * p.x + p.y * p.y + p.z * p.z);
            float rg = World.groundRadius(p.x / d, p.y / d, p.z / d);
            if (Math.abs(d - rg) > 0.05f) grounded = false;
        }
        check(grounded, "todas las estructuras estan sobre el suelo");

        // 1) cae y se queda en el suelo
        for (int i = 0; i < 120; i++) g.update(1 / 60f);
        check(g.onGround && Math.abs(heightAbove(g)) < 0.2f, "aterriza y se queda en el suelo (h=" + heightAbove(g) + ")");

        // 2) caminar: el "arriba" rota -> las paredes se vuelven piso; nunca flota ni se hunde
        float up0x = g.ux, up0y = g.uy, up0z = g.uz;
        g.moveX = 0f; g.moveY = 1f;
        float maxH = 0, minDot = 2;
        boolean nan = false;
        for (int i = 0; i < 60 * 60; i++) {
            g.update(1 / 60f);
            maxH = Math.max(maxH, Math.abs(heightAbove(g)));
            minDot = Math.min(minDot, up0x * g.ux + up0y * g.uy + up0z * g.uz);
            if (Float.isNaN(g.px) || Float.isNaN(g.ex) || Float.isNaN(g.dx)) nan = true;
        }
        System.out.println("tras 60 s caminando: min(up0 . up)=" + minDot + " altura max=" + maxH + " hp=" + g.hp);
        check(!nan, "sin NaN");
        check(maxH < 4f, "pegado al suelo mientras camina por la pared");
        check(minDot < 0.2f, "la gravedad local giro mas de 78 grados (paredes -> piso)");

        // 3) camara unitaria y perpendicular a "arriba" cuando pitch=0
        float dl = (float) Math.sqrt(g.dx * g.dx + g.dy * g.dy + g.dz * g.dz);
        check(Math.abs(dl - 1f) < 1e-3f, "direccion de camara unitaria");

        // 4) yaw
        g.moveX = 0; g.moveY = 0;
        for (int i = 0; i < 60; i++) g.update(1 / 60f);
        float fx0 = g.fx, fy0 = g.fy, fz0 = g.fz;
        g.addLook(0.5f, 0f);
        g.update(1 / 60f);
        float dot = fx0 * g.fx + fy0 * g.fy + fz0 * g.fz;
        check(Math.abs(dot - Math.cos(0.5)) < 0.02, "giro de yaw correcto (dot=" + dot + ")");

        // 5) gancho al suelo adelante: vuelo -> enganchado -> avanza
        g.reset(false);
        for (int i = 0; i < 60; i++) g.update(1 / 60f);
        g.addLook(0f, -0.05f);
        g.update(1 / 60f);
        float x0 = g.px, y0 = g.py, z0 = g.pz;
        g.hookHeld = true;
        int fly = 0, att = 0;
        float maxRel = 0;
        for (int i = 0; i < 60 * 3; i++) {
            g.update(1 / 60f);
            maxRel = Math.max(maxRel, g.releaseT);
            if (g.hookState == Game.HOOK_FLY) fly++;
            if (g.hookState == Game.HOOK_ATTACHED) att++;
        }
        g.hookHeld = false;
        g.update(1 / 60f);
        float moved = (float) Math.sqrt((g.px - x0) * (g.px - x0) + (g.py - y0) * (g.py - y0) + (g.pz - z0) * (g.pz - z0));
        System.out.println("gancho: vuelo=" + fly + " frames, enganchado=" + att + " frames, avance=" + moved);
        check(fly >= 2, "el kunai vuela antes de engancharse");
        check(att > 10 || moved > 10f, "el gancho jala al jugador");
        check(maxRel > 0f, "al soltar se activa la mano abierta");
        check(!Float.isNaN(g.px), "gancho sin NaN");

        // 6) gancho al aire: falla, el kunai regresa
        g.reset(false);
        for (int i = 0; i < 30; i++) g.update(1 / 60f);
        g.addLook(0f, 1.3f);
        g.update(1 / 60f);
        g.hookHeld = true;
        boolean sawMiss = false;
        for (int i = 0; i < 40; i++) {
            g.update(1 / 60f);
            if (g.hookState == Game.HOOK_FLY && g.hookMiss) sawMiss = true;
        }
        g.hookHeld = false;
        check(sawMiss && g.hookState != Game.HOOK_ATTACHED, "apuntar al vacio: el kunai sale y no engancha");

        // 7) rayo contra una casa
        World.Prop house = null;
        for (World.Prop p : g.world.props) if (p.kind == World.HOUSE) { house = p; break; }
        int hit = g.raycast(house.x + house.ux * 20f, house.y + house.uy * 20f, house.z + house.uz * 20f,
                -house.ux, -house.uy, -house.uz, 40f, false);
        check(hit == 2, "un rayo desde arriba golpea el techo de una casa (tipo=" + hit + ")");
        World.Prop arch = null;
        for (World.Prop p : g.world.props) if (p.kind == World.ARCH) { arch = p; break; }
        float ox = arch.x + arch.ax * arch.off + arch.ux * 20f, oy = arch.y + arch.ay * arch.off + arch.uy * 20f,
                oz = arch.z + arch.az * arch.off + arch.uz * 20f;
        hit = g.raycast(ox, oy, oz, -arch.ux, -arch.uy, -arch.uz, 40f, false);
        check(hit == 2, "un rayo golpea un pilar de arco (tipo=" + hit + ")");

        // 8) disparar a un enemigo lo elimina
        g.reset(false);
        for (int i = 0; i < 30; i++) g.update(1 / 60f);
        Game.Enemy e = g.enemies.get(0);
        float dd = 15f;
        float tx = g.px + g.fx * dd, ty = g.py + g.fy * dd, tz = g.pz + g.fz * dd;
        float tr = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
        float rr = World.groundRadius(tx / tr, ty / tr, tz / tr);
        e.alive = true; e.hp = 3;
        e.x = tx / tr * rr; e.y = ty / tr * rr; e.z = tz / tr * rr;
        {
            // apunta al centro del cuerpo (1.2 sobre el suelo), considerando la curvatura del suelo
            float cx = e.x - e.x / rr * 1.2f - g.ex, cy = e.y - e.y / rr * 1.2f - g.ey, cz = e.z - e.z / rr * 1.2f - g.ez;
            float cl = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
            g.pitch = (float) Math.asin((cx * g.ux + cy * g.uy + cz * g.uz) / cl);
        }
        g.update(1 / 600f);
        int kills0 = g.score;
        g.fireHeld = true;
        for (int i = 0; i < 60 * 2; i++) {
            e.x = tx / tr * rr; e.y = ty / tr * rr; e.z = tz / tr * rr;
            g.update(1 / 60f);
        }
        g.fireHeld = false;
        check(g.score > kills0, "disparar a un enemigo lo elimina (score=" + g.score + ")");

        System.out.println(fails == 0 ? "TODO BIEN" : ("FALLOS: " + fails));
        System.exit(fails == 0 ? 0 : 1);
    }
}
