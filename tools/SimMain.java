import com.animestriker.*;

/** Simulación sin gráficos: partidos CPU vs CPU para validar la lógica. */
public class SimMain {
    public static void main(String[] a) {
        int games = a.length > 0 ? Integer.parseInt(a[0]) : 12;
        int[] ev = new int[20];
        int totalGoals = 0, nan = 0, stuck = 0;
        long seed = 7;
        for (int g = 0; g < games; g++) {
            Teams.Def A = Teams.ALL[g % 6], B = Teams.ALL[(g * 5 + 2) % 6];
            if (A == B) B = Teams.ALL[(g + 1) % 6];
            Match m = new Match(A, B, -1, 60f, 0.5f, g % 3 == 0);
            m.rnd.setSeed(seed + g);
            m.listener = (t, p) -> ev[t]++;
            float dt = 1f / 60f; int steps = 0; float lastOwnerT = 0, noOwner = 0;
            while (m.state != Match.ST_END && steps < 60 * 60 * 6) {
                m.update(dt); steps++;
                if (Float.isNaN(m.bx) || Float.isNaN(m.by) || Float.isNaN(m.bz)) nan++;
                for (Match.Player[] ps : m.pl) for (Match.Player p : ps)
                    if (Float.isNaN(p.x) || Float.isNaN(p.y) || p.x < -1 || p.x > 101 || p.y < -1 || p.y > 61) nan++;
                if (m.state == Match.ST_PLAY) { if (m.owner == null) noOwner += dt; else noOwner = 0; if (noOwner > 15) { stuck++; noOwner = 0; } }
            }
            totalGoals += m.score[0] + m.score[1];
            System.out.printf("%-16s %d-%d %-16s  tiros %d-%d  pos %2.0f%%  fin=%s t=%.0fs pen=%s%n",
                    A.name, m.score[0], m.score[1], B.name, m.shots[0], m.shots[1],
                    100f * m.possTime[0] / (m.possTime[0] + m.possTime[1] + 1e-3f),
                    m.state == Match.ST_END, m.time, m.shootout ? m.penA + "-" + m.penB : "-");
        }
        System.out.println("goles/partido (2 min de juego): " + (totalGoals / (float) games));
        System.out.println("eventos: pases=" + ev[Match.EV_PASS] + " tiros=" + ev[Match.EV_SHOT] + " paradas=" + ev[Match.EV_SAVE]
                + " postes=" + ev[Match.EV_POST] + " entradas=" + ev[Match.EV_TACKLE] + " fuera=" + ev[Match.EV_OUT]
                + " especiales=" + ev[Match.EV_SPECIAL] + " goles=" + ev[Match.EV_GOAL]);
        System.out.println("NaN/fuera de campo=" + nan + "  balon_sin_dueño>15s=" + stuck);
    }
}
