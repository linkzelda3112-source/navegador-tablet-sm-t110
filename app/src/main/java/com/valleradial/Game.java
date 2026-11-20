package com.valleradial;

import java.util.ArrayList;
import java.util.Random;

/**
 * Logica del juego (sin Android). Gravedad radial: "arriba" siempre apunta hacia el eje
 * del cilindro, asi que al caminar por la pared la camara rota y la pared pasa a ser piso.
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
    public static final int HOOK_NONE = 0, HOOK_ATTACHED = 1;

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
    public float time, invuln, hurtFlash, hitMarker, recoil, throwT, swayX, swayY, bob;

    // camara (la calcula update)
    public float ex, ey, ez, dx, dy, dz, ux, uy, uz;
    public final float[] vp = new float[16]; // lo escribe el renderer, lo lee el HUD

    // entrada (la escribe la UI)
    public volatile float moveX, moveY;
    public volatile boolean fireHeld, hookHeld, jumpHeld;
    private float lookYaw, lookPitch;

    // gancho
    public int hookState = HOOK_NONE;
    public float hax, hay, haz;
    private boolean hookPrev;

    // pistola
    private float fireCd;

    public String message = "";
    public float messageT;

    public float hitX, hitY, hitZ;
    private int hitEnemy;
    private final int[] cand = new int[1024];

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
        float z = 30f, th = World.pathTheta(z);
        float rg = World.groundRadius(th, z);
        px = rg * (float) Math.cos(th);
        py = rg * (float) Math.sin(th);
        pz = z;
        vx = vy = vz = 0;
        fx = 0; fy = 0; fz = 1;
        pitch = 0;
        hp = MAX_HP;
        hookState = HOOK_NONE;
        invuln = 1.5f;
        for (Enemy e : enemies) spawnEnemy(e);
        if (first) {
            say("Valle Radial", 8f);
        }
        updateCamera();
    }

    private void spawnEnemy(Enemy e) {
        for (int tries = 0; tries < 30; tries++) {
            float th = rnd.nextFloat() * World.TWO_PI;
            float z = 60f + rnd.nextFloat() * (World.L - 90f);
            float rg = World.groundRadius(th, z);
            e.x = rg * (float) Math.cos(th);
            e.y = rg * (float) Math.sin(th);
            e.z = z;
            float ddx = e.x - px, ddy = e.y - py, ddz = e.z - pz;
            if (ddx * ddx + ddy * ddy + ddz * ddz > 70f * 70f) break;
        }
        e.hp = 3;
        e.alive = true;
        e.respawn = 0;
        e.cd = 0;
        e.fx = 0; e.fy = 0; e.fz = 1;
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
        if (throwT > 0) throwT -= dt;
        if (fireCd > 0) fireCd -= dt;
        recoil = Math.max(0f, recoil - dt * 7f);

        float d = (float) Math.sqrt(px * px + py * py);
        if (d < 1f) d = 1f;
        float upx = -px / d, upy = -py / d; // upz = 0

        // forward ortonormal a "arriba"
        float fu = fx * upx + fy * upy;
        fx -= upx * fu; fy -= upy * fu;
        float fl = (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
        if (fl < 1e-4f) { fx = -upy; fy = upx; fz = 0; fl = 1f; }
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
            float rx = fy * 0f - fz * upy;
            float ry = fz * upx - fx * 0f;
            float rz = fx * upy - fy * upx;
            float c = (float) Math.cos(dyaw), s = (float) Math.sin(dyaw);
            fx = fx * c + rx * s; fy = fy * c + ry * s; fz = fz * c + rz * s;
            float l = (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
            fx /= l; fy /= l; fz /= l;
        }
        pitch += dpit;
        if (pitch > 1.45f) pitch = 1.45f;
        if (pitch < -1.45f) pitch = -1.45f;

        // vectores base
        float rx = fy * 0f - fz * upy;
        float ry = fz * upx - fx * 0f;
        float rz = fx * upy - fy * upx;

        // velocidad = parte vertical + horizontal
        float vup = vx * upx + vy * upy;
        float hvx = vx - upx * vup, hvy = vy - upy * vup, hvz = vz;

        float mx = moveX, my = moveY;
        float ml = (float) Math.sqrt(mx * mx + my * my);
        if (ml > 1f) { mx /= ml; my /= ml; }
        float wx = fx * my + rx * mx, wy = fy * my + ry * mx, wz = fz * my + rz * mx;
        float acc = onGround ? 14f : 2.2f;
        float k = Math.min(1f, acc * dt);
        float tx = wx * SPEED, ty = wy * SPEED, tz = wz * SPEED;
        if (hookState == HOOK_ATTACHED) k = Math.min(1f, 0.6f * dt);
        hvx += (tx - hvx) * k; hvy += (ty - hvy) * k; hvz += (tz - hvz) * k;

        if (jumpHeld && onGround) { vup = JUMP; onGround = false; }

        float g = GRAV;
        if (hookState == HOOK_ATTACHED) g *= 0.55f;
        vup -= g * dt;
        vx = hvx + upx * vup; vy = hvy + upy * vup; vz = hvz;

        updateHook(dt);

        // integrar
        px += vx * dt; py += vy * dt; pz += vz * dt;
        if (pz < 4f) { pz = 4f; if (vz < 0) vz = 0; }
        if (pz > World.L - 4f) { pz = World.L - 4f; if (vz > 0) vz = 0; }

        collideProps();

        // suelo
        d = (float) Math.sqrt(px * px + py * py);
        if (d < 1f) d = 1f;
        float th = (float) Math.atan2(py, px);
        float rg = World.groundRadius(th, pz);
        float hgt = rg - d;
        upx = -px / d; upy = -py / d;
        float vupN = vx * upx + vy * upy;
        boolean wasGround = onGround;
        if (hgt <= 0f || (wasGround && hgt < 0.6f && vupN <= 0.1f)) {
            float s = rg / d;
            px *= s; py *= s;
            if (vupN < 0f) { vx -= upx * vupN; vy -= upy * vupN; }
            onGround = true;
        } else {
            onGround = hgt < 0.08f;
        }

        // pasos / vaiven
        float hs = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (onGround) bob += dt * (3f + hs * 0.55f);
        else bob += dt * 2f;

        updateGun();
        updateEnemies(dt);
        updateCamera();
    }

    private void updateCamera() {
        float d = (float) Math.sqrt(px * px + py * py);
        if (d < 1f) d = 1f;
        ux = -px / d; uy = -py / d; uz = 0f;
        float fu = fx * ux + fy * uy;
        float gx = fx - ux * fu, gy = fy - uy * fu, gz = fz;
        float gl = (float) Math.sqrt(gx * gx + gy * gy + gz * gz);
        if (gl < 1e-4f) { gx = fx; gy = fy; gz = fz; gl = 1f; }
        gx /= gl; gy /= gl; gz /= gl;
        float cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch);
        dx = gx * cp + ux * sp; dy = gy * cp + uy * sp; dz = gz * cp + uz * sp;
        ex = px + ux * EYE; ey = py + uy * EYE; ez = pz + uz * EYE;
    }

    // ---------------------------------------------------------------- gancho (kunai con cuerda)
    private void updateHook(float dt) {
        boolean press = hookHeld && !hookPrev;
        hookPrev = hookHeld;
        if (press) {
            throwT = 0.45f;
            if (events != null) events.onHook();
            int t = raycast(ex, ey, ez, dx, dy, dz, 140f, false);
            if (t == 1 || t == 2) {
                hax = hitX; hay = hitY; haz = hitZ;
                hookState = HOOK_ATTACHED;
            } else {
                hookState = HOOK_NONE;
            }
        }
        if (hookState == HOOK_ATTACHED) {
            if (!hookHeld) { hookState = HOOK_NONE; return; }
            float tx = hax - px, ty = hay - py, tz = haz - (pz + 1f);
            float dist = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
            if (dist < 3f || dist > 170f) { hookState = HOOK_NONE; return; }
            tx /= dist; ty /= dist; tz /= dist;
            float along = vx * tx + vy * ty + vz * tz;
            float want = 34f;
            if (along < want) {
                float add = Math.min(want - along, 90f * dt);
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
            int t = raycast(ex, ey, ez, dx, dy, dz, 160f, true);
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
            float d = (float) Math.sqrt(e.x * e.x + e.y * e.y);
            float eux = -e.x / d, euy = -e.y / d;
            float tx = px - e.x, ty = py - e.y, tz = pz - e.z;
            float dist = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
            float tu = tx * eux + ty * euy;
            float hx2 = tx - eux * tu, hy2 = ty - euy * tu, hz2 = tz;
            float hl = (float) Math.sqrt(hx2 * hx2 + hy2 * hy2 + hz2 * hz2);
            if (hl > 0.01f) {
                hx2 /= hl; hy2 /= hl; hz2 /= hl;
                e.fx = hx2; e.fy = hy2; e.fz = hz2;
            }
            if (dist < 95f && dist > 2.2f) {
                float sp = 7.5f * dt;
                e.x += hx2 * sp; e.y += hy2 * sp; e.z += hz2 * sp;
                if (e.z < 4f) e.z = 4f;
                if (e.z > World.L - 4f) e.z = World.L - 4f;
                float th = (float) Math.atan2(e.y, e.x);
                float rg = World.groundRadius(th, e.z);
                float dd = (float) Math.sqrt(e.x * e.x + e.y * e.y);
                e.x *= rg / dd; e.y *= rg / dd;
            }
            // contacto con el jugador
            float cx = e.x + eux * 1.2f - px - 0f, cy = e.y + euy * 1.2f - py, cz = e.z - pz;
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

    // ---------------------------------------------------------------- colisiones con casas, arboles y rocas
    private void collideProps() {
        for (World.Prop p : world.props) {
            if (!p.solid) continue;
            float rx = px - p.x, ry = py - p.y, rz = pz - p.z;
            if (rx * rx + ry * ry + rz * rz > 400f) continue;
            float along = rx * p.ux + ry * p.uy; // uz = 0
            float lx = rx - p.ux * along, ly = ry - p.uy * along, lz = rz;
            float lat = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
            float rad = p.r + 0.5f;
            if (along < -2f || along > p.h + 3f) continue;
            if (lat >= rad) continue;
            float vup = vx * p.ux + vy * p.uy;
            if (p.standable && along > p.h - 0.7f && vup <= 0.5f) {
                // aterrizar encima
                px += p.ux * (p.h - along); py += p.uy * (p.h - along);
                if (vup < 0f) { vx -= p.ux * vup; vy -= p.uy * vup; }
                onGround = true;
                continue;
            }
            if (along > p.h) continue;
            if (lat < 1e-3f) { lx = 1f; ly = 0f; lz = 0f; lat = 1f; }
            float push = rad - lat;
            px += lx / lat * push; py += ly / lat * push; pz += lz / lat * push;
            float vn = (vx * lx + vy * ly + vz * lz) / lat;
            if (vn < 0f) { vx -= lx / lat * vn; vy -= ly / lat * vn; vz -= lz / lat * vn; }
        }
    }

    // ---------------------------------------------------------------- rayos
    /** @return 0 nada, 1 terreno, 2 objeto, 3 enemigo. El punto queda en hitX/Y/Z. */
    public int raycast(float ox, float oy, float oz, float rdx, float rdy, float rdz,
                       float max, boolean withEnemies) {
        int nc = 0;
        float lim = (max + 14f) * (max + 14f);
        ArrayList<World.Prop> ps = world.props;
        for (int i = 0; i < ps.size() && nc < cand.length; i++) {
            World.Prop p = ps.get(i);
            if (!p.solid) continue;
            float a = p.x - ox, b = p.y - oy, c = p.z - oz;
            if (a * a + b * b + c * c < lim) cand[nc++] = i;
        }
        for (float t = 0.8f; t <= max; t += 0.7f) {
            float x = ox + rdx * t, y = oy + rdy * t, z = oz + rdz * t;
            if (z < 0f || z > World.L) return 0;
            if (withEnemies) {
                for (int i = 0; i < enemies.size(); i++) {
                    Enemy e = enemies.get(i);
                    if (!e.alive) continue;
                    float d0 = (float) Math.sqrt(e.x * e.x + e.y * e.y);
                    float cx = e.x - e.x / d0 * 1.2f - x, cy = e.y - e.y / d0 * 1.2f - y, cz = e.z - z;
                    if (cx * cx + cy * cy + cz * cz < 1.9f * 1.9f) {
                        hitX = x; hitY = y; hitZ = z; hitEnemy = i;
                        return 3;
                    }
                }
            }
            float d = (float) Math.sqrt(x * x + y * y);
            float th = (float) Math.atan2(y, x);
            if (d >= World.groundRadius(th, z)) {
                hitX = ox + rdx * (t - 0.35f); hitY = oy + rdy * (t - 0.35f); hitZ = oz + rdz * (t - 0.35f);
                return 1;
            }
            for (int k = 0; k < nc; k++) {
                World.Prop p = ps.get(cand[k]);
                float rx = x - p.x, ry = y - p.y, rz = z - p.z;
                float along = rx * p.ux + ry * p.uy;
                float lx = rx - p.ux * along, ly = ry - p.uy * along;
                boolean hit = false;
                if (along > -0.5f && along < p.h && lx * lx + ly * ly + rz * rz < p.r * p.r) hit = true;
                if (!hit && p.sr > 0f) {
                    float sa = along - p.sh;
                    if (sa * sa + lx * lx + ly * ly + rz * rz < p.sr * p.sr) hit = true;
                }
                if (hit) {
                    hitX = x; hitY = y; hitZ = z;
                    return 2;
                }
            }
        }
        return 0;
    }
}
