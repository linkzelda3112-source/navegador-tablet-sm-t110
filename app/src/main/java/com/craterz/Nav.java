package com.craterz;

/**
 * Navegación colectiva: una rejilla del mapa con los obstáculos inflados y un campo de flujo (distancia al jugador)
 * calculado una vez para TODOS los zombis. Cada zombi baja por el campo y rodea muros, trincheras e iglesia.
 */
public final class Nav {
    public static final float CELL = 0.8f;
    private static final float ORG = World.ORG;
    private static final int N = (int) (World.CH * World.NC / CELL);
    private static final int INF = 1 << 28;

    private final World world;
    private final boolean[] blocked = new boolean[N * N];
    private final int[] dist = new int[N * N];
    private final int[] queue = new int[N * N * 2];
    private final boolean[] inQ = new boolean[N * N];
    private int doorsSeen = -1;
    private float timer;
    private int lastCell = -1;
    public boolean valid;

    public Nav(World w) { world = w; rebuild(); }

    private static int cx(float x) { return (int) Math.floor((x - ORG) / CELL); }
    private static float wc(int c) { return ORG + (c + 0.5f) * CELL; }
    private static boolean in(int a) { return a >= 0 && a < N; }

    private void rebuild() {
        java.util.Arrays.fill(blocked, false);
        final float inf = 0.46f;
        for (int i = 0; i < world.cols.size(); i++) {
            World.Collider c = world.cols.get(i);
            if (!c.active) continue;
            float x1, z1, x2, z2;
            if (c.circle) { x1 = c.x1 - c.r; x2 = c.x1 + c.r; z1 = c.z1 - c.r; z2 = c.z1 + c.r; }
            else { x1 = c.x1; x2 = c.x2; z1 = c.z1; z2 = c.z2; }
            int a0 = Math.max(0, cx(x1 - inf)), a1 = Math.min(N - 1, cx(x2 + inf));
            int b0 = Math.max(0, cx(z1 - inf)), b1 = Math.min(N - 1, cx(z2 + inf));
            for (int b = b0; b <= b1; b++)
                for (int a = a0; a <= a1; a++) {
                    float px = wc(a), pz = wc(b);
                    float dx, dz, lim;
                    if (c.circle) { dx = px - c.x1; dz = pz - c.z1; lim = c.r + inf; }
                    else { dx = px - Noise.clamp(px, c.x1, c.x2); dz = pz - Noise.clamp(pz, c.z1, c.z2); lim = inf; }
                    if (dx * dx + dz * dz < lim * lim) blocked[b * N + a] = true;
                }
        }
        valid = true;
    }

    private int openDoors() {
        int n = 0;
        for (int i = 0; i < world.doors.size(); i++) if (world.doors.get(i).open) n++;
        return n;
    }

    /** Llamar cada fotograma: recalcula el campo cuando el jugador cambia de celda (máx. ~4 veces/s). */
    public void update(float dt, float px, float pz) {
        timer -= dt;
        int od = openDoors();
        if (od != doorsSeen) { doorsSeen = od; rebuild(); lastCell = -1; }
        int ax = cx(px), az = cx(pz);
        if (!in(ax) || !in(az)) return;
        int cell = az * N + ax;
        if (cell == lastCell && timer > 0) return;
        if (timer > 0 && lastCell != -1) return;
        timer = 0.25f;
        lastCell = cell;
        compute(ax, az);
    }

    private void compute(int sx, int sz) {
        java.util.Arrays.fill(dist, INF);
        // si la celda del jugador está bloqueada, parte de la libre más cercana
        int start = sz * N + sx;
        if (blocked[start]) {
            int best = -1, bd = INF;
            for (int r = 1; r <= 4 && best < 0; r++)
                for (int dz = -r; dz <= r; dz++)
                    for (int dx = -r; dx <= r; dx++) {
                        int a = sx + dx, b = sz + dz;
                        if (!in(a) || !in(b) || blocked[b * N + a]) continue;
                        int d2 = dx * dx + dz * dz;
                        if (d2 < bd) { bd = d2; best = b * N + a; }
                    }
            if (best < 0) return;
            start = best;
        }
        int head = 0, tail = 0, cap = queue.length;
        dist[start] = 0;
        queue[tail++] = start; inQ[start] = true;
        while (head != tail) {
            int cur = queue[head++]; if (head == cap) head = 0;
            inQ[cur] = false;
            int ca = cur % N, cb = cur / N, cd = dist[cur];
            for (int k = 0; k < 8; k++) {
                int da = DX[k], db = DZ[k];
                int a = ca + da, b = cb + db;
                if (!in(a) || !in(b)) continue;
                int ni = b * N + a;
                if (blocked[ni]) continue;
                int cost = 10;
                if (da != 0 && db != 0) {
                    if (blocked[cb * N + a] || blocked[b * N + ca]) continue; // sin cortar esquinas
                    cost = 14;
                }
                int nd = cd + cost;
                if (nd < dist[ni]) {
                    dist[ni] = nd;
                    if (!inQ[ni]) { queue[tail++] = ni; if (tail == cap) tail = 0; inQ[ni] = true; }
                }
            }
        }
    }

    /** Línea recta entre dos celdas sin atravesar celdas bloqueadas (ignora la celda de partida). */
    private boolean clearLine(int a0, int b0, int a1, int b1) {
        int n = Math.max(Math.abs(a1 - a0), Math.abs(b1 - b0));
        for (int i = 1; i <= n; i++) {
            int a = a0 + Math.round((a1 - a0) * i / (float) n), b = b0 + Math.round((b1 - b0) * i / (float) n);
            if (blocked[b * N + a]) return false;
        }
        return true;
    }

    private static final int[] DX = {1, -1, 0, 0, 1, 1, -1, -1};
    private static final int[] DZ = {0, 0, 1, -1, 1, -1, 1, -1};

    /** ¿Hay camino conocido desde (x,z)? */
    public boolean reachable(float x, float z) {
        int a = cx(x), b = cx(z);
        if (!in(a) || !in(b)) return false;
        for (int dz = -1; dz <= 1; dz++)
            for (int dx = -1; dx <= 1; dx++)
                if (in(a + dx) && in(b + dz) && dist[(b + dz) * N + a + dx] < INF) return true;
        return false;
    }

    /**
     * Dirección (unitaria, en out[0..1]) para ir de (x,z) hacia el jugador esquivando obstáculos.
     * rnd (0..1) desempata y reparte a los zombis entre rutas casi igual de buenas para que no se amontonen.
     * Devuelve false si no hay ruta (ir directo).
     */
    public boolean dir(float x, float z, float rnd, float[] out) {
        int a = cx(x), b = cx(z);
        if (!in(a) || !in(b)) return false;
        // da pasos por el campo (hasta 3 celdas) y apunta al punto alcanzado: movimiento suave sin zigzag
        int ca = a, cb = b;
        int cur = dist[cb * N + ca];
        boolean found = cur < INF;
        if (!found) {
            // zombi dentro de celda inflada: busca la celda vecina con menor distancia
            int bd = INF;
            for (int dz = -2; dz <= 2; dz++)
                for (int dx = -2; dx <= 2; dx++) {
                    int na = a + dx, nb = b + dz;
                    if (!in(na) || !in(nb)) continue;
                    int dv = dist[nb * N + na];
                    if (dv < bd && clearLine(a, b, na, nb)) { bd = dv; ca = na; cb = nb; }
                }
            if (bd >= INF) return false;
            cur = bd;
        }
        int steps = found ? 4 : 0;
        for (int s = 0; s < steps; s++) {
            int best = cur, ba = ca, bb = cb;
            for (int k = 0; k < 8; k++) {
                int na = ca + DX[k], nb = cb + DZ[k];
                if (!in(na) || !in(nb)) continue;
                int dv = dist[nb * N + na];
                if (DX[k] != 0 && DZ[k] != 0 && (blocked[cb * N + na] || blocked[nb * N + ca])) continue;
                // desempate: ligera preferencia aleatoria por zombi entre rutas equivalentes
                int adj = dv + (int) (((k * 7 + (int) (rnd * 97)) % 5) * 0.4f);
                if (adj < best) { best = adj; ba = na; bb = nb; }
            }
            if (ba == ca && bb == cb) break;
            if (!clearLine(a, b, ba, bb)) break;
            ca = ba; cb = bb; cur = dist[cb * N + ca];
            if (cur <= 0) break;
        }
        float tx = wc(ca) - x, tz = wc(cb) - z;
        float l = (float) Math.sqrt(tx * tx + tz * tz);
        if (l < 0.05f) return false;
        out[0] = tx / l; out[1] = tz / l;
        return true;
    }
}
