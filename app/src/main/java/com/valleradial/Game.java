package com.valleradial;

import java.util.ArrayList;
import java.util.Random;

/**
 * Logica del juego (sin Android). Gravedad radial hacia el centro de la esfera: "arriba" siempre
 * apunta al centro, asi que al caminar por una pared la camara rota y la pared pasa a ser piso.
 */
public final class Game {
    public interface Events {
        void onShoot();
        void onHook();
        void onHit();
        void onHurt();
        void onKill();
    }

    public static final int MAX_HP = 6;
    public static final float EYE = 1.7f, SPEED = 13f, JUMP = 11f, GRAV = 30f;
    public static final int HOOK_NONE = 0, HOOK_FLY = 1, HOOK_ATTACHED = 2;
    private static final float HOOK_RANGE = 170f, HOOK_PULL = 52f;

    public static final class Enemy {
        public float x, y, z, fx, fy, fz;
        public int hp;
        public boolean alive;
        public float respawn, cd, bob, hurt;
    }

    public Events events;
    public final World world = new World();
    public final ArrayList<Enemy> enemies = new ArrayList<>();
    private final Random rnd = new Random(11);

    // jugador
    public float px, py, pz, vx, vy, vz, fx, fy, fz, pitch;
    public boolean onGround;
    public int hp = MAX_HP, score;
    public float time, invuln, hurtFlash, hitMarker, recoil, swayX, swayY, bob;

    // camara (la calcula update)
    public float ex, ey, ez, dx, dy, dz, ux, uy, uz;
    public final float[] vp = new float[16]; // lo escribe el renderer, lo lee el HUD

    // entrada (la escribe la UI)
    public volatile float moveX, moveY;
    public volatile boolean fireHeld, hookHeld, jumpHeld;
    private float lookYaw, lookPitch;

    // gancho
    public int hookState = HOOK_NONE;
    public float hax, hay, haz;          // punto de anclaje (o punto lejano si falla)
    public float hookProg;               // 0..1 vuelo del kunai
    public boolean hookMiss;
    public float releaseT;               // >0 un momento tras soltar (mano abierta)
    public float hookTime;               // tiempo desde que se engancho (animacion)
    private float hookFlyDur;
    private boolean hookPrev;

    // pistola
    private float fireCd;

    public String message = "";
    public float messageT;

    public float hitX, hitY, hitZ;
    private int hitEnemy;
    private final int[] cand = new int[2048];

    public Game() {
        for (int i = 0; i < 10; i++) enemies.add(new Enemy());
        reset(true);
    }

    public synchronized void addLook(float dyaw, float dpitch) {
        lookYaw += dyaw;
        lookPitch += dpitch;
    }

    public void say(String m, float t) {
        message = m;
        messageT = t;
    }

    public void reset(boolean first) {
        float[] n = World.spawnDir();
        float rg = World.groundRadius(n[0], n[1], n[2]);
        px = n[0] * rg; py = n[1] * rg; pz = n[2] * rg;
        vx = vy = vz = 0;
        float[] f = World.spawnForward();
        fx = f[0]; fy = f[1]; fz = f[2];
        pitch = 0;
        hp = MAX_HP;
        hookState = HOOK_NONE;
        invuln = 1.5f;
        for (Enemy e : enemies) spawnEnemy(e);
        if (first) say("Valle Radial", 8f);
        updateCamera();
    }

    private void spawnEnemy(Enemy e) {
        for (int tries = 0; tries < 40; tries++) {
            float gx = (float) rnd.nextGaussian(), gy = (float) rnd.nextGaussian(), gz = (float) rnd.nextGaussian();
            float gl = (float) Math.sqrt(gx * gx + gy * gy + gz * gz);
            if (gl < 1e-3f) continue;
            gx /= gl; gy /= gl; gz /= gl;
            float rg = World.groundRadius(gx, gy, gz);
            e.x = gx * rg; e.y = gy * rg; e.z = gz * rg;
            float ddx = e.x - px, ddy = e.y - py, ddz = e.z - pz;
            if (ddx * ddx + ddy * ddy + ddz * ddz > 70f * 70f) break;
        }
        e.hp = 3;
        e.alive = true;
        e.respawn = 0;
        e.cd = 0;
        e.fx = 1; e.fy = 0; e.fz = 0;
    }

    // ---------------------------------------------------------------- actualizacion
    public void update(float dt) {
        if (dt > 0.05f) dt = 0.05f;
        if (dt <= 0f) return;
        time += dt;
        if (messageT > 0) messageT -= dt;
        if (invuln > 0) invuln -= dt;
        if (hurtFlash > 0) hurtFlash -= dt;
        if (hitMarker > 0) hitMarker -= dt;
        if (releaseT > 0) releaseT -= dt;
        if (fireCd > 0) fireCd -= dt;
        recoil = Math.max(0f, recoil - dt * 7f);

        float d = (float) Math.sqrt(px * px + py * py + pz * pz);
        if (d < 1f) d = 1f;
        float upx = -px / d, upy = -py / d, upz = -pz / d;

        // forward ortonormal a "arriba"
        float fu = fx * upx + fy * upy + fz * upz;
        fx -= upx * fu; fy -= upy * fu; fz -= upz * fu;
        float fl = (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
        if (fl < 1e-4f) {
            // cualquier tangente
            float tx = upy, ty = -upx, tz = 0f;
            float tl = (float) Math.sqrt(tx * tx + ty * ty);
            if (tl < 1e-4f) { tx = 1f; ty = 0f; tz = 0f; tl = 1f; }
            fx = tx / tl; fy = ty / tl; fz = tz / tl; fl = 1f;
            fu = fx * upx + fy * upy + fz * upz;
            fx -= upx * fu; fy -= upy * fu; fz -= upz * fu;
            fl = (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
        }
        fx /= fl; fy /= fl; fz /= fl;

        // mirar
        float dyaw, dpit;
        synchronized (this) {
            dyaw = lookYaw; dpit = lookPitch;
            lookYaw = 0; lookPitch = 0;
        }
        swayX += (dyaw * 6f - swayX) * Math.min(1f, dt * 10f);
        swayY += (dpit * 6f - swayY) * Math.min(1f, dt * 10f);
        if (dyaw != 0f) {
            // derecha = f x up
            float rx0 = fy * upz - fz * upy, ry0 = fz * upx - fx * upz, rz0 = fx * upy - fy * upx;
            float c = (float) Math.cos(dyaw), s = (float) Math.sin(dyaw);
            fx = fx * c + rx0 * s; fy = fy * c + ry0 * s; fz = fz * c + rz0 * s;
            float l = (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
            fx /= l; fy /= l; fz /= l;
        }
        pitch += dpit;
        if (pitch > 1.45f) pitch = 1.45f;
        if (pitch < -1.45f) pitch = -1.45f;

        float rx = fy * upz - fz * upy, ry = fz * upx - fx * upz, rz = fx * upy - fy * upx;

        // velocidad = parte vertical + horizontal
        float vup = vx * upx + vy * upy + vz * upz;
        float hvx = vx - upx * vup, hvy = vy - upy * vup, hvz = vz - upz * vup;

        float mx = moveX, my = moveY;
        float ml = (float) Math.sqrt(mx * mx + my * my);
        if (ml > 1f) { mx /= ml; my /= ml; }
        float wx = fx * my + rx * mx, wy = fy * my + ry * mx, wz = fz * my + rz * mx;
        float acc = onGround ? 14f : 2.2f;
        float k = Math.min(1f, acc * dt);
        if (hookState == HOOK_ATTACHED) k = Math.min(1f, 0.6f * dt);
        hvx += (wx * SPEED - hvx) * k; hvy += (wy * SPEED - hvy) * k; hvz += (wz * SPEED - hvz) * k;

        if (jumpHeld && onGround) { vup = JUMP; onGround = false; }

        float g = GRAV;
        if (hookState == HOOK_ATTACHED) g *= 0.5f;
        vup -= g * dt;
        vx = hvx + upx * vup; vy = hvy + upy * vup; vz = hvz + upz * vup;

        updateHook(dt, upx, upy, upz);

        // integrar
        px += vx * dt; py += vy * dt; pz += vz * dt;

        collideProps();

        // suelo
        d = (float) Math.sqrt(px * px + py * py + pz * pz);
        if (d < 1f) d = 1f;
        float nx = px / d, ny = py / d, nz = pz / d;
        float rg = World.groundRadius(nx, ny, nz);
        float hgt = rg - d;
        upx = -nx; upy = -ny; upz = -nz;
        float vupN = vx * upx + vy * upy + vz * upz;
        boolean wasGround = onGround;
        if (hgt <= 0f || (wasGround && hgt < 0.6f && vupN <= 0.1f)) {
            float s = rg / d;
            px *= s; py *= s; pz *= s;
            if (vupN < 0f) { vx -= upx * vupN; vy -= upy * vupN; vz -= upz * vupN; }
            onGround = true;
        } else {
            onGround = hgt < 0.08f;
        }

        // vaiven al caminar
        float hs = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (onGround) bob += dt * (3f + hs * 0.55f);
        else bob += dt * 2f;

        updateGun();
        updateEnemies(dt);
        updateCamera();
    }

    public float speed() {
        return (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
    }

    private void updateCamera() {
        float d = (float) Math.sqrt(px * px + py * py + pz * pz);
        if (d < 1f) d = 1f;
        ux = -px / d; uy = -py / d; uz = -pz / d;
        float fu = fx * ux + fy * uy + fz * uz;
        float gx = fx - ux * fu, gy = fy - uy * fu, gz = fz - uz * fu;
        float gl = (float) Math.sqrt(gx * gx + gy * gy + gz * gz);
        if (gl < 1e-4f) { gx = fx; gy = fy; gz = fz; gl = 1f; }
        gx /= gl; gy /= gl; gz /= gl;
        float cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch);
        dx = gx * cp + ux * sp; dy = gy * cp + uy * sp; dz = gz * cp + uz * sp;
        ex = px + ux * EYE; ey = py + uy * EYE; ez = pz + uz * EYE;
    }

    // ---------------------------------------------------------------- gancho (kunai con cuerda)
    private void releaseHook() {
        if (hookState != HOOK_NONE) releaseT = 0.4f;
        hookState = HOOK_NONE;
    }

    private void updateHook(float dt, float upx, float upy, float upz) {
        boolean press = hookHeld && !hookPrev;
        hookPrev = hookHeld;
        if (press && hookState == HOOK_NONE) {
            if (events != null) events.onHook();
            int t = raycast(ex, ey, ez, dx, dy, dz, HOOK_RANGE, false);
            float dist;
            if (t == 1 || t == 2) {
                hax = hitX; hay = hitY; haz = hitZ;
                hookMiss = false;
                dist = (float) Math.sqrt((hax - ex) * (hax - ex) + (hay - ey) * (hay - ey) + (haz - ez) * (haz - ez));
            } else {
                dist = 70f;
                hax = ex + dx * dist; hay = ey + dy * dist; haz = ez + dz * dist;
                hookMiss = true;
            }
            hookState = HOOK_FLY;
            hookProg = 0f;
            hookFlyDur = Math.max(0.14f, dist / 190f);
            releaseT = 0f;
        }
        if (hookState == HOOK_FLY) {
            if (!hookHeld && !hookMiss) { releaseHook(); return; }
            hookProg += dt / hookFlyDur;
            if (hookProg >= 1f) {
                hookProg = 1f;
                if (hookMiss) {
                    releaseHook();
                } else {
                    hookState = HOOK_ATTACHED;
                    hookTime = 0f;
                }
            }
            return;
        }
        if (hookState == HOOK_ATTACHED) {
            hookTime += dt;
            if (!hookHeld) { releaseHook(); return; }
            float tx = hax - px - upx, ty = hay - py - upy, tz = haz - pz - upz;
            float dist = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
            if (dist < 3.5f || dist > HOOK_RANGE + 40f) { releaseHook(); return; }
            tx /= dist; ty /= dist; tz /= dist;
            float along = vx * tx + vy * ty + vz * tz;
            if (along < HOOK_PULL) {
                float add = Math.min(HOOK_PULL - along, 140f * dt);
                vx += tx * add; vy += ty * add; vz += tz * add;
            }
            onGround = false;
        }
    }

    // ---------------------------------------------------------------- pistola
    private void updateGun() {
        if (fireHeld && fireCd <= 0f) {
            fireCd = 0.2f;
            recoil = 1f;
            if (events != null) events.onShoot();
            int t = raycast(ex, ey, ez, dx, dy, dz, 170f, true);
            if (t == 3) {
                Enemy e = enemies.get(hitEnemy);
                e.hp--;
                e.hurt = 0.15f;
                hitMarker = 0.18f;
                if (events != null) events.onHit();
                if (e.hp <= 0) {
                    e.alive = false;
                    e.respawn = 6f;
                    score++;
                    if (events != null) events.onKill();
                }
            }
        }
    }

    // ---------------------------------------------------------------- enemigos ("manchas de tinta")
    private void updateEnemies(float dt) {
        for (Enemy e : enemies) {
            if (!e.alive) {
                e.respawn -= dt;
                if (e.respawn <= 0f) spawnEnemy(e);
                continue;
            }
            if (e.hurt > 0) e.hurt -= dt;
            if (e.cd > 0) e.cd -= dt;
            e.bob += dt * 5f;
            float d = (float) Math.sqrt(e.x * e.x + e.y * e.y + e.z * e.z);
            float eux = -e.x / d, euy = -e.y / d, euz = -e.z / d;
            float tx = px - e.x, ty = py - e.y, tz = pz - e.z;
            float dist = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
            float tu = tx * eux + ty * euy + tz * euz;
            float hx2 = tx - eux * tu, hy2 = ty - euy * tu, hz2 = tz - euz * tu;
            float hl = (float) Math.sqrt(hx2 * hx2 + hy2 * hy2 + hz2 * hz2);
            if (hl > 0.01f) {
                hx2 /= hl; hy2 /= hl; hz2 /= hl;
                e.fx = hx2; e.fy = hy2; e.fz = hz2;
            }
            if (dist < 95f && dist > 2.2f && hl > 0.01f) {
                float sp = 7.5f * dt;
                e.x += hx2 * sp; e.y += hy2 * sp; e.z += hz2 * sp;
                float dd = (float) Math.sqrt(e.x * e.x + e.y * e.y + e.z * e.z);
                float rg = World.groundRadius(e.x / dd, e.y / dd, e.z / dd);
                e.x *= rg / dd; e.y *= rg / dd; e.z *= rg / dd;
            }
            // contacto con el jugador (centro del cuerpo a 1.2 sobre el suelo)
            d = (float) Math.sqrt(e.x * e.x + e.y * e.y + e.z * e.z);
            float cx = e.x - e.x / d * 1.2f - px, cy = e.y - e.y / d * 1.2f - py, cz = e.z - e.z / d * 1.2f - pz;
            float cd = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
            if (cd < 2.7f && e.cd <= 0f) {
                e.cd = 1.1f;
                hurt();
            }
        }
    }

    private void hurt() {
        if (invuln > 0f) return;
        hp--;
        invuln = 1.2f;
        hurtFlash = 0.35f;
        if (events != null) events.onHurt();
        if (hp <= 0) {
            say("Has caido... otra vez", 3f);
            reset(false);
        }
    }

    // ---------------------------------------------------------------- colisiones
    private void collideProps() {
        for (World.Prop p : world.props) {
            if (!p.solid) continue;
            float rx = px - p.x, ry = py - p.y, rz = pz - p.z;
            float ext = p.r + p.off + 3f;
            if (rx * rx + ry * ry + rz * rz > (ext + p.h + 3f) * (ext + p.h + 3f)) continue;
            int n = p.off > 0f ? 2 : 1;
            for (int ci = 0; ci < n; ci++) {
                float o = n == 1 ? 0f : (ci == 0 ? -p.off : p.off);
                float qx = px - (p.x + p.ax * o), qy = py - (p.y + p.ay * o), qz = pz - (p.z + p.az * o);
                float along = qx * p.ux + qy * p.uy + qz * p.uz;
                float lx = qx - p.ux * along, ly = qy - p.uy * along, lz = qz - p.uz * along;
                float lat = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
                float rad = p.r + 0.5f;
                if (along < -2f || along > p.h + 3f) continue;
                if (lat >= rad) continue;
                float vup = vx * p.ux + vy * p.uy + vz * p.uz;
                if (p.standable && along > p.h - 0.7f && vup <= 0.5f) {
                    float lift = p.h - along;
                    px += p.ux * lift; py += p.uy * lift; pz += p.uz * lift;
                    if (vup < 0f) { vx -= p.ux * vup; vy -= p.uy * vup; vz -= p.uz * vup; }
                    onGround = true;
                    continue;
                }
                if (along > p.h) continue;
                if (lat < 1e-3f) { lx = p.ax; ly = p.ay; lz = p.az; lat = 1f; }
                float push = rad - lat;
                px += lx / lat * push; py += ly / lat * push; pz += lz / lat * push;
                float vn = (vx * lx + vy * ly + vz * lz) / lat;
                if (vn < 0f) { vx -= lx / lat * vn; vy -= ly / lat * vn; vz -= lz / lat * vn; }
            }
        }
    }

    // ---------------------------------------------------------------- rayos
    /** @return 0 nada, 1 terreno, 2 objeto, 3 enemigo. El punto queda en hitX/Y/Z. */
    public int raycast(float ox, float oy, float oz, float rdx, float rdy, float rdz,
                       float max, boolean withEnemies) {
        int nc = 0;
        ArrayList<World.Prop> ps = world.props;
        for (int i = 0; i < ps.size() && nc < cand.length; i++) {
            World.Prop p = ps.get(i);
            if (!p.solid) continue;
            float lim = max + 14f + p.off + p.h;
            float a = p.x - ox, b = p.y - oy, c = p.z - oz;
            if (a * a + b * b + c * c < lim * lim) cand[nc++] = i;
        }
        for (float t = 0.8f; t <= max; t += 0.7f) {
            float x = ox + rdx * t, y = oy + rdy * t, z = oz + rdz * t;
            if (withEnemies) {
                for (int i = 0; i < enemies.size(); i++) {
                    Enemy e = enemies.get(i);
                    if (!e.alive) continue;
                    float d0 = (float) Math.sqrt(e.x * e.x + e.y * e.y + e.z * e.z);
                    float cx = e.x - e.x / d0 * 1.2f - x, cy = e.y - e.y / d0 * 1.2f - y, cz = e.z - e.z / d0 * 1.2f - z;
                    if (cx * cx + cy * cy + cz * cz < 1.9f * 1.9f) {
                        hitX = x; hitY = y; hitZ = z; hitEnemy = i;
                        return 3;
                    }
                }
            }
            float d = (float) Math.sqrt(x * x + y * y + z * z);
            if (d >= World.groundRadius(x / d, y / d, z / d)) {
                hitX = ox + rdx * (t - 0.35f); hitY = oy + rdy * (t - 0.35f); hitZ = oz + rdz * (t - 0.35f);
                return 1;
            }
            for (int k = 0; k < nc; k++) {
                World.Prop p = ps.get(cand[k]);
                int n = p.off > 0f ? 2 : 1;
                for (int ci = 0; ci < n; ci++) {
                    float o = n == 1 ? 0f : (ci == 0 ? -p.off : p.off);
                    float qx = x - (p.x + p.ax * o), qy = y - (p.y + p.ay * o), qz = z - (p.z + p.az * o);
                    float along = qx * p.ux + qy * p.uy + qz * p.uz;
                    float lx = qx - p.ux * along, ly = qy - p.uy * along, lz = qz - p.uz * along;
                    float l2 = lx * lx + ly * ly + lz * lz;
                    boolean hit = along > -0.5f && along < p.h && l2 < p.r * p.r;
                    if (!hit && p.sr > 0f) {
                        float sa = along - p.sh;
                        if (sa * sa + l2 < p.sr * p.sr) hit = true;
                    }
                    if (hit) {
                        hitX = x; hitY = y; hitZ = z;
                        return 2;
                    }
                }
            }
        }
        return 0;
    }
}
