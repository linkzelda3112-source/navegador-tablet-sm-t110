package com.craterz;

import java.util.ArrayList;

/** Toda la lógica del juego: jugador, armas, zombis, rondas, compras y efectos. */
public final class Game {
    public static final int MODE_NORMAL = 0, MODE_FAST = 1, MODE_NIGHT = 2, MODE_TEST = 3;
    public static final String[] MODE_NAME = {"Supervivencia", "Horda veloz", "Noche cerrada", "Zona de pruebas"};

    public static final class Zombie {
        public float x, y, z, yaw, hp, maxHp, speed, scale = 1f, phase, hitFlash, deathT, riseT, atkT, atkCd, grT, stuckT, side, sideT;
        public float tr = 1, tg = 1, tb = 1, losT, slot, stunT, stunFx, hunger, enrageT, lungeT, lungeCd;
        public boolean spotted;
        public boolean los = true;
        public int state; // 0 surgiendo, 1 persiguiendo, 2 muerto
        public int tier;  // 0 camina, 1 trota, 2 corre
        public boolean brute;
        public int variant, gait; // aspecto (0 casco+abrigo,1 abrigo,2 andrajos,3 andrajos+casco) y forma de andar
        public float seed;
        public float lx, lz;
    }

    public static final class PowerUp {
        public int type; // 0 munición máx, 1 muerte instantánea, 2 puntos x2, 3 bomba
        public float x, y, z, life, spin;
    }

    public final World world;
    public final Input in;
    public final Sfx sfx;
    public final Particles ps = new Particles();
    public final int mode;
    public final ArrayList<Zombie> zs = new ArrayList<Zombie>();
    public final ArrayList<PowerUp> ups = new ArrayList<PowerUp>();

    // jugador
    public float px, pz, py, yaw, pitch, bobT, speedNow;
    public float health = 100f, maxHealth = 100f, sinceHit = 99f, dmgFlash, hitMarker, headMarker, killFlash;
    public int points, kills, round, perks;
    public boolean over, paused;
    public float time;

    // armas
    public final int[] slotW = {Weapons.PISTOL, -1};
    public final int[][] ammo = new int[2][2]; // [slot][mag, reserve]
    public int cur;
    public float sinceShot = 9f, cd, reloadT, reloadDur, swapT, recoil, flashT, adsT, kick;
    public boolean adsOn, prevFire;
    public String prompt = "";
    public int promptCost;

    // rondas
    public boolean roundActive;
    public float roundWait = 4f, spawnTimer, roundBanner;
    public int toSpawn, alive;
    public boolean[] zoneOpen = new boolean[5];

    // efectos
    public float instaT, doubleT, lightning, thunderDelay, nextBolt = 8f, moonFlash;
    public String toast = "";
    public float toastT;
    public boolean vibrateReq;
    private float stepT;
    private int rs = 424242;
    private final float[] tmp = new float[2], look = new float[2];

    public Game(int mode, Sfx sfx, Input in) {
        this.mode = mode;
        this.sfx = sfx;
        this.in = in;
        world = new World();
        nav = new Nav(world);
        px = world.startX; pz = world.startZ;
        py = world.ground(px, pz) + 1.65f;
        zoneOpen[0] = true;
        ammo[0][0] = Weapons.MAG[Weapons.PISTOL]; ammo[0][1] = Weapons.RESERVE[Weapons.PISTOL];
        points = mode == MODE_FAST ? 1500 : 500;
        if (mode == MODE_FAST) round = 0;
        if (mode == MODE_TEST) {
            // zona de pruebas: todo abierto y gratis, vida y munición infinitas, zombis de todos los tipos
            round = 10; points = 999999; roundActive = true;
            for (int i = 0; i < world.doors.size(); i++) if (!world.doors.get(i).open) openDoor(i);
            say("ZONA DE PRUEBAS: todo gratis. CAMBIAR recorre todas las armas");
        }
        sfx.play(Sfx.ROUND, 0.7f);
    }

    private float rnd() { rs = rs * 1103515245 + 12345; return ((rs >>> 8) & 0xFFFF) / 65536f; }

    public int weapon() { return slotW[cur]; }
    public boolean hasPerk(int p) { return (perks & (1 << p)) != 0; }

    public float fwdX() { return (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))); }
    public float fwdY() { return (float) Math.sin(Math.toRadians(pitch)); }
    public float fwdZ() { return (float) (-Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))); }

    private void say(String s) { toast = s; toastT = 2.6f; }

    // ------------------------------------------------------------------ bucle
    public void update(float dt) {
        if (paused) return;
        time += dt;
        if (over) {
            ps.update(dt, world);
            updateZombies(dt);
            if (dmgFlash > 0) dmgFlash -= dt * 0.5f;
            return;
        }
        if (toastT > 0) toastT -= dt;
        if (hitMarker > 0) hitMarker -= dt;
        if (headMarker > 0) headMarker -= dt;
        if (killFlash > 0) killFlash -= dt;
        if (dmgFlash > 0) dmgFlash -= dt * 1.2f;
        if (instaT > 0) instaT -= dt;
        if (doubleT > 0) doubleT -= dt;
        if (roundBanner > 0) roundBanner -= dt;
        sinceHit += dt;

        updatePlayer(dt);
        updateWeapon(dt);
        updateRounds(dt);
        updateZombies(dt);
        updatePowerUps(dt);
        updateBuying();
        updateAmbience(dt);
        ps.update(dt, world);

        if (health <= 0) die();
    }

    private void updatePlayer(float dt) {
        in.takeLook(look);
        float sens = Prefs.sens() * (adsOn ? 0.55f : 1f);
        yaw += look[0] * 0.115f * sens;
        pitch -= look[1] * 0.115f * sens;
        if (pitch > 80) pitch = 80;
        if (pitch < -80) pitch = -80;

        float mx = in.moveX, my = in.moveY;
        float len = (float) Math.sqrt(mx * mx + my * my);
        if (len > 1f) { mx /= len; my /= len; len = 1f; }
        float spd = 5.1f * (hasPerk(World.PERK_PIES) ? 1.18f : 1f) * (adsOn ? 0.62f : 1f) * (reloadT > 0 ? 0.92f : 1f);
        float sy = (float) Math.sin(Math.toRadians(yaw)), cy = (float) Math.cos(Math.toRadians(yaw));
        float dx = (sy * my + cy * mx) * spd * dt, dz = (-cy * my + sy * mx) * spd * dt;
        speedNow += (len * spd - speedNow) * Math.min(1f, dt * 10f);
        tmp[0] = px + dx; tmp[1] = pz + dz;
        world.resolve(tmp, 0.4f);
        tmp[0] = Noise.clamp(tmp[0], -65.5f, 65.5f); tmp[1] = Noise.clamp(tmp[1], -65.5f, 65.5f);
        px = tmp[0]; pz = tmp[1];

        if (speedNow > 0.8f) {
            bobT += dt * speedNow * 1.9f;
            stepT -= dt * speedNow;
            if (stepT <= 0) { stepT = 2.0f; sfx.play(Sfx.STEP, 0.2f, (rnd() - 0.5f) * 0.3f); }
        }
        float bob = (float) Math.sin(bobT) * 0.035f * Math.min(1f, speedNow / 4f);
        float target = world.ground(px, pz) + 1.65f + bob;
        py += (target - py) * Math.min(1f, dt * 14f);

        if (health < maxHealth && sinceHit > 4f) {
            health = Math.min(maxHealth, health + (hasPerk(World.PERK_VIGOR) ? 70f : 28f) * dt);
        }
        if (in.takeAds()) adsOn = !adsOn;
        adsT += ((adsOn && reloadT <= 0 && swapT <= 0 ? 1f : 0f) - adsT) * Math.min(1f, dt * 12f);
        if (in.takeSwap()) swapWeapon();
        if (in.takeReload()) startReload();
    }

    private void swapWeapon() {
        if (mode == MODE_TEST) {
            if (swapT > 0) return;
            int w = (slotW[cur] + 1) % Weapons.COUNT;
            slotW[cur] = w;
            ammo[cur][0] = Weapons.MAG[w]; ammo[cur][1] = Weapons.RESERVE[w];
            swapT = 0.5f; sfx.play(Sfx.SWITCH, 0.4f); reloadT = 0; adsOn = false;
            say(Weapons.NAME[w]);
            return;
        }
        if (slotW[1] < 0 || swapT > 0) return;
        cur = 1 - cur;
        swapT = 0.5f; sfx.play(Sfx.SWITCH, 0.4f); reloadT = 0; adsOn = false;
    }

    private void startReload() {
        int w = weapon();
        if (reloadT > 0 || swapT > 0) return;
        if (ammo[cur][0] >= Weapons.MAG[w] || ammo[cur][1] <= 0) return;
        reloadDur = Weapons.RELOAD[w] * (hasPerk(World.PERK_REFLEJOS) ? 0.55f : 1f);
        reloadT = reloadDur;
        adsOn = false;
        sfx.play(Sfx.RELOAD, 0.5f);
        reloadEv = 0;
        shellPlayed = true;
    }

    private int reloadEv;
    private boolean shellPlayed = true, pumpPlayed = true;

    private void weaponSfx(int w) {
        if (reloadT > 0) {
            float p = 1f - reloadT / Math.max(0.1f, reloadDur);
            if (w == Weapons.SHOTGUN) {
                if (reloadEv == 0 && p > 0.25f) { reloadEv = 1; sfx.play(Sfx.MAG_IN, 0.45f); }
                if (reloadEv == 1 && p > 0.5f) { reloadEv = 2; sfx.play(Sfx.MAG_IN, 0.45f); }
                if (reloadEv == 2 && p > 0.88f) { reloadEv = 3; sfx.play(Sfx.PUMP, 0.6f); }
            } else {
                if (reloadEv == 0 && p > 0.18f) { reloadEv = 1; sfx.play(Sfx.MAG_OUT, 0.6f); }
                if (reloadEv == 1 && p > 0.58f) { reloadEv = 2; sfx.play(Sfx.MAG_IN, 0.7f); }
                if (reloadEv == 2 && p > 0.82f) { reloadEv = 3; sfx.play(w == Weapons.STORM ? Sfx.SWITCH : Sfx.BOLT, 0.6f); }
            }
        }
        if (!shellPlayed && sinceShot > 0.3f) {
            shellPlayed = true;
            if (w != Weapons.SHOTGUN && w != Weapons.STORM) sfx.play(Sfx.SHELL, 0.35f, 0.3f);
        }
        if (!pumpPlayed && sinceShot > 0.3f) {
            pumpPlayed = true;
            if (w == Weapons.SHOTGUN && ammo[cur][0] > 0) { sfx.play(Sfx.PUMP, 0.55f); sfx.play(Sfx.SHELL, 0.3f, 0.3f); }
        }
    }

    private void updateWeapon(float dt) {
        int w = weapon();
        if (cd > 0) cd -= dt;
        if (swapT > 0) swapT -= dt;
        if (flashT > 0) flashT -= dt;
        sinceShot += dt;
        updateStorm(dt);
        weaponSfx(w);
        recoil += (0f - recoil) * Math.min(1f, dt * 9f);
        if (reloadT > 0) {
            reloadT -= dt;
            if (reloadT <= 0) {
                int need = Weapons.MAG[w] - ammo[cur][0];
                int take = Math.min(need, ammo[cur][1]);
                ammo[cur][0] += take; ammo[cur][1] -= take;
                reloadT = 0;
            }
        }
        boolean fire = in.fire;
        boolean trigger = Weapons.AUTO[w] ? fire : (fire && !prevFire);
        prevFire = fire;
        if (trigger && reloadT <= 0 && swapT <= 0 && cd <= 0) {
            if (ammo[cur][0] > 0) shoot(w);
            else if (ammo[cur][1] > 0) startReload();
            else if (!prevEmptyClick) { sfx.play(Sfx.EMPTY, 0.5f); prevEmptyClick = true; cd = 0.3f; }
        }
        if (!fire) prevEmptyClick = false;
    }

    private boolean prevEmptyClick;

    /** Tormenta: primer disparo = rayo que paraliza y hace daño brutal; después dispara normal mientras el rayo se recarga. */
    public boolean stormBolt = true;
    public float stormCharge = 1f;
    public static final float STORM_RECHARGE = 8f;
    public float boltFlash;

    private void updateStorm(float dt) {
        if (boltFlash > 0) boltFlash -= dt;
        if (!stormBolt) {
            stormCharge += dt / (mode == MODE_TEST ? 2f : STORM_RECHARGE);
            if (stormCharge >= 1f) {
                stormCharge = 1f; stormBolt = true;
                sfx.play(Sfx.POWER, 0.5f);
                say("¡Rayo de la Tormenta listo!");
            }
        }
    }

    private void zigzag(float x0, float y0, float z0, float x1, float y1, float z1, float amp) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        int n = Math.max(3, Math.min(10, (int) (len / 1.2f)));
        float px0 = x0, py0 = y0, pz0 = z0;
        for (int i = 1; i <= n; i++) {
            float u = i / (float) n;
            float nx = x0 + dx * u, ny = y0 + dy * u, nz = z0 + dz * u;
            if (i < n) { nx += (rnd() - 0.5f) * amp; ny += (rnd() - 0.5f) * amp; nz += (rnd() - 0.5f) * amp; }
            ps.tracer(px0, py0, pz0, nx, ny, nz, 0.35f, 0.8f, 1.8f, 0.075f);
            ps.tracer(px0, py0, pz0, nx, ny, nz, 1f, 1f, 1f, 0.026f);
            px0 = nx; py0 = ny; pz0 = nz;
        }
    }

    private void lightningShot(float mx, float my, float mz) {
        float ox = px, oy = py, oz = pz, dx = fwdX(), dy = fwdY(), dz = fwdZ();
        // objetivo principal: el zombi que tengas en la mira (o el más cercano a la línea de tiro, con ayuda de puntería)
        Zombie tgt = null; float bestT = 60f, bestPerp = 2.6f;
        for (int i = 0; i < zs.size(); i++) {
            Zombie z = zs.get(i);
            if (z.state == 2) continue;
            float vx = z.x - ox, vy = z.y + 1.1f * z.scale - oy, vz = z.z - oz;
            float t = vx * dx + vy * dy + vz * dz;
            if (t < 0.5f || t > 60f) continue;
            float cx = vx - dx * t, cy = vy - dy * t, cz = vz - dz * t;
            float perp = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
            if (perp < bestPerp && t < bestT + 8f && world.rayBlock(ox, oy, oz, vx, vy, vz, 1f) > 1.5f) { bestPerp = perp; tgt = z; bestT = t; }
        }
        float ex = ox + dx * 40f, ey = oy + dy * 40f, ez = oz + dz * 40f;
        float wl = world.rayBlock(ox, oy, oz, dx, dy, dz, 40f);
        if (wl <= 1f) { ex = ox + dx * 40f * wl; ey = oy + dy * 40f * wl; ez = oz + dz * 40f * wl; }
        sfx.play(Sfx.CRACK, 1f); sfx.play(Sfx.STORM, 1f); sfx.play(Sfx.THUNDER, 0.5f);
        lightning = Math.max(lightning, 0.7f); boltFlash = 0.35f;
        recoil = Math.min(1.6f, recoil + 0.6f);
        if (tgt == null) {
            zigzag(mx, my, mz, ex, ey, ez, 0.7f);
            ps.sparks(ex, ey, ez, 0, 1, 0, 14, 0.5f, 1f, 1.8f);
            ps.flash(ex, ey, ez, 1.4f, 0.4f, 0.9f, 1.8f, 0.2f);
            return;
        }
        ArrayList<Zombie> chain = new ArrayList<Zombie>();
        chain.add(tgt);
        Zombie cur = tgt;
        for (int hop = 0; hop < 6; hop++) {
            Zombie next = null; float bd = 8.5f;
            for (int i = 0; i < zs.size(); i++) {
                Zombie z = zs.get(i);
                if (z.state == 2 || chain.contains(z)) continue;
                float d = (float) Math.hypot(z.x - cur.x, z.z - cur.z);
                if (d < bd) { bd = d; next = z; }
            }
            if (next == null) break;
            chain.add(next); cur = next;
        }
        float lx = mx, ly = my, lz = mz;
        for (int i = 0; i < chain.size(); i++) {
            Zombie z = chain.get(i);
            float hx = z.x, hy = z.y + 1.2f * z.scale, hz = z.z;
            zigzag(lx, ly, lz, hx, hy, hz, 0.6f);
            ps.flash(hx, hy, hz, 1.6f, 0.4f, 0.9f, 1.9f, 0.22f);
            ps.ring(hx, z.y + 0.1f, hz, 2.2f, 0.4f, 0.9f, 1.8f, 0.35f);
            ps.sparks(hx, hy, hz, 0, 1, 0, 16, 0.5f, 1f, 1.9f);
            float ddx = hx - lx, ddz = hz - lz, dl = (float) Math.sqrt(ddx * ddx + ddz * ddz) + 1e-4f;
            damageZombie(z, i == 0 ? 1400f : 900f, false, hx, hy, hz, ddx / dl, 0f, ddz / dl);
            z.stunT = 5f;   // paralizado
            lx = hx; ly = hy; lz = hz;
        }
    }

    private void shoot(int w) {
        ammo[cur][0]--;
        float rate = hasPerk(World.PERK_RAFAGA) ? 1.0f / 1.38f : 1f;
        cd = Weapons.INTERVAL[w] * rate;
        sinceShot = 0f; shellPlayed = false; pumpPlayed = false;
        recoil = Math.min(1.6f, recoil + Weapons.KICK[w] * 0.18f);
        pitch += Weapons.KICK[w] * (adsOn ? 0.14f : 0.22f) * (0.6f + rnd() * 0.8f);
        yaw += (rnd() - 0.5f) * Weapons.KICK[w] * 0.18f;
        flashT = 0.07f;
        int snd = w == Weapons.PISTOL ? Sfx.PISTOL : w == Weapons.SHOTGUN ? Sfx.SHOTGUN : w == Weapons.RIFLE ? Sfx.RIFLE : w == Weapons.SMG ? Sfx.SMG : Sfx.STORM;
        if (w == Weapons.STORM && stormBolt) {
            // primer disparo: rayo paralizante; deja la Tormenta en modo normal mientras se recarga
            stormBolt = false; stormCharge = 0f;
            if (Prefs.vibrate()) vibrateReq = true;
            float[] mm = WeaponModels.muzzle(w);
            float rx0 = (float) Math.cos(Math.toRadians(yaw)), rz0 = (float) Math.sin(Math.toRadians(yaw));
            lightningShot(px + rx0 * (mm[0] + 0.12f * (1 - adsT)) + fwdX() * -mm[2], py + (mm[1] - 0.1f * (1 - adsT)) + fwdY() * -mm[2], pz + rz0 * (mm[0] + 0.12f * (1 - adsT)) + fwdZ() * -mm[2]);
            return;
        }
        sfx.play(snd, w == Weapons.SMG ? 0.5f : 0.7f);
        if (Prefs.vibrate() && w != Weapons.SMG) vibrateReq = true;

        float fx = fwdX(), fy = fwdY(), fz = fwdZ();
        float rx = (float) Math.cos(Math.toRadians(yaw)), rz = (float) Math.sin(Math.toRadians(yaw));
        float ux = -fy * (float) Math.sin(Math.toRadians(yaw)), uy = (float) Math.cos(Math.toRadians(pitch)), uz = fy * (float) Math.cos(Math.toRadians(yaw));
        float spread = Weapons.SPREAD[w] * (adsOn ? 0.3f : 1f) * (1f + speedNow * 0.12f) * (float) Math.PI / 180f;
        float[] m = WeaponModels.muzzle(w);
        // la boca del arma en el mundo (aprox.)
        float mxw = px + rx * (m[0] + 0.12f * (1 - adsT)) + fx * -m[2], myw = py + (m[1] - 0.17f * (1 - adsT) * 0.6f) + fy * -m[2], mzw = pz + rz * (m[0] + 0.12f * (1 - adsT)) + fz * -m[2];
        ps.flash(mxw, myw, mzw, w == Weapons.SHOTGUN ? 0.55f : 0.38f, 1f, 0.8f, 0.4f, 0.06f);
        if (w == Weapons.STORM) ps.flash(mxw, myw, mzw, 0.7f, 0.4f, 0.9f, 1.4f, 0.09f);
        ps.smoke(mxw + fx * 0.2f, myw, mzw + fz * 0.2f, 0.1f, 0.5f, 0.5f, 0.5f, 0.25f, 0.8f, 0.4f);

        int pellets = Weapons.PELLETS[w];
        for (int p = 0; p < pellets; p++) {
            float sa = (rnd() * 2 - 1) * spread, sb = (rnd() * 2 - 1) * spread;
            if (pellets > 1) { sa *= 1.0f + rnd(); sb *= 1.0f + rnd(); }
            float dx = fx + rx * sa + ux * sb, dy = fy + uy * sb, dz = fz + rz * sa + uz * sb;
            float dl = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= dl; dy /= dl; dz /= dl;
            castRay(w, px, py, pz, dx, dy, dz, p == 0 || w != Weapons.SHOTGUN, mxw, myw, mzw);
        }
    }

    private void castRay(int w, float ox, float oy, float oz, float dx, float dy, float dz, boolean fx, float mx, float my, float mz) {
        float maxLen = 90f;
        float bestT = maxLen;
        Zombie bestZ = null;
        boolean head = false;
        for (int i = 0; i < zs.size(); i++) {
            Zombie z = zs.get(i);
            if (z.state == 2) continue;
            float r = 0.42f * z.scale, base = z.y + (z.state == 0 ? -2f * (1 - z.riseT) : 0), top = base + 1.88f * z.scale;
            float fx0 = ox - z.x, fz0 = oz - z.z;
            float a = dx * dx + dz * dz;
            float t;
            if (a < 1e-8f) continue;
            float b2 = 2 * (fx0 * dx + fz0 * dz), c = fx0 * fx0 + fz0 * fz0 - r * r;
            float disc = b2 * b2 - 4 * a * c;
            if (disc < 0) continue;
            t = (-b2 - (float) Math.sqrt(disc)) / (2 * a);
            if (t < 0.1f || t > bestT) continue;
            float yy = oy + dy * t;
            if (yy < base || yy > top) continue;
            bestT = t; bestZ = z;
            head = yy > base + 1.5f * z.scale;
        }
        float wl = world.rayBlock(ox, oy, oz, dx, dy, dz, maxLen) * maxLen;
        float hitT = bestT;
        boolean zombieHit = bestZ != null && bestT < wl;
        if (!zombieHit) hitT = Math.min(wl, maxLen);
        if (zombieHit) {
            float hx = ox + dx * hitT, hy = oy + dy * hitT, hz = oz + dz * hitT;
            float dmg = Weapons.DMG[w];
            if (w == Weapons.SHOTGUN && hitT > 12f) dmg *= Math.max(0.25f, 1f - (hitT - 12f) / 20f);
            if (head) dmg *= Weapons.HEAD_MULT[w];
            if (instaT > 0) dmg = 99999f;
            damageZombie(bestZ, dmg, head, hx, hy, hz, dx, dy, dz);
        } else if (hitT < maxLen) {
            float hx = ox + dx * hitT, hy = oy + dy * hitT, hz = oz + dz * hitT;
            if (fx) {
                ps.sparks(hx - dx * 0.05f, hy, hz - dz * 0.05f, -dx, 0.5f, -dz, 5, 1f, 0.8f, 0.4f);
                ps.dust(hx - dx * 0.05f, hy, hz - dz * 0.05f, -dx, 0.2f, -dz, 2, 0.45f, 0.42f, 0.38f);
            }
        } else if (fx && dy < -0.02f) {
            // impacto en el suelo
            for (float tt = 2; tt < 60; tt += 1.5f) {
                float gx = ox + dx * tt, gy = oy + dy * tt, gz = oz + dz * tt;
                if (gy <= world.ground(gx, gz)) {
                    ps.dust(gx, gy + 0.05f, gz, 0, 1, 0, 3, 0.3f, 0.24f, 0.17f);
                    ps.sparks(gx, gy + 0.05f, gz, 0, 1, 0, 3, 1f, 0.7f, 0.35f);
                    hitT = tt; break;
                }
            }
        }
        if (fx || zombieHit) {
            float ex = ox + dx * hitT, ey = oy + dy * hitT, ez = oz + dz * hitT;
            if (w == Weapons.STORM) ps.tracer(mx, my, mz, ex, ey, ez, 0.4f, 0.9f, 1.6f, 0.09f);
            else ps.tracer(mx, my, mz, ex, ey, ez, 1f, 0.85f, 0.5f, 0.018f);
        }
    }

    private void damageZombie(Zombie z, float dmg, boolean head, float hx, float hy, float hz, float dx, float dy, float dz) {
        if (z.state == 0) { z.state = 1; z.riseT = 1f; }
        z.hp -= dmg;
        z.hitFlash = 0.12f;
        hitMarker = 0.18f;
        if (head) headMarker = 0.22f;
        ps.blood(hx, hy, hz, dx, dy, dz, head ? 9 : 6);
        sfx.play(head ? Sfx.HEADSHOT : Sfx.FLESH, head ? 0.65f : 0.5f, 0f);
        // empujón
        z.x += dx * 0.06f; z.z += dz * 0.06f;
        if (z.hp <= 0) {
            killZombie(z, head);
        } else {
            addPoints(10);
        }
    }

    public void addPoints(int n) { points += doubleT > 0 ? n * 2 : n; }

    private void killZombie(Zombie z, boolean head) {
        z.state = 2; z.deathT = 0; z.hp = 0;
        kills++;
        killFlash = 0.25f;
        addPoints(head ? 100 : 60);
        alive--;
        float dd = (float) Math.hypot(z.x - px, z.z - pz);
        sfx.play(Sfx.ZDIE, Math.max(0.15f, 0.7f - dd / 40f), pan(z.x, z.z));
        ps.blood(z.x, z.y + 1.2f * z.scale, z.z, 0, 0, 0, 10);
        if (round >= 2 && ups.size() < 1 && rnd() < (mode == MODE_TEST ? 0.25f : 0.045f)) {
            PowerUp u = new PowerUp();
            u.type = (int) (rnd() * 4); if (u.type > 3) u.type = 3;
            u.x = z.x; u.z = z.z; u.y = world.ground(z.x, z.z) + 0.9f; u.life = 28f;
            ups.add(u);
        }
    }

    private float pan(float x, float z) {
        float dx = x - px, dz = z - pz;
        float rx = (float) Math.cos(Math.toRadians(yaw)), rz = (float) Math.sin(Math.toRadians(yaw));
        float l = (float) Math.sqrt(dx * dx + dz * dz) + 1e-4f;
        return Noise.clamp((dx * rx + dz * rz) / l, -1f, 1f);
    }

    // ------------------------------------------------------------------ rondas y zombis
    private int roundTotal(int r) { return (int) (6 + 3.8f * (r - 1) + 0.18f * (r - 1) * (r - 1)); }

    private float roundHp(int r) {
        if (r <= 9) return 70f + r * 80f;
        return (70f + 9 * 80f) * (float) Math.pow(1.11, r - 9);
    }

    private void updateRounds(float dt) {
        if (mode == MODE_TEST) {
            health = maxHealth; points = 999999;
            for (int s = 0; s < 2; s++) if (slotW[s] >= 0) { ammo[s][0] = Weapons.MAG[slotW[s]]; ammo[s][1] = Weapons.RESERVE[slotW[s]]; }
            spawnTimer -= dt;
            if (alive < 6 && spawnTimer <= 0) { spawnZombie(); alive++; spawnTimer = 1.5f; }
            return;
        }
        if (!roundActive) {
            roundWait -= dt;
            if (roundWait <= 0) {
                round++;
                toSpawn = roundTotal(round);
                spawnTimer = 1f;
                roundActive = true;
                roundBanner = 4f;
                sfx.play(Sfx.ROUND, 0.8f);
            }
            return;
        }
        spawnTimer -= dt;
        int maxAlive = Prefs.quality() == 0 ? 10 : 16;
        if (toSpawn > 0 && alive < maxAlive && spawnTimer <= 0) {
            spawnZombie();
            toSpawn--; alive++;
            spawnTimer = Math.max(0.35f, 2.1f - round * 0.11f);
        }
        if (toSpawn == 0 && alive <= 0) {
            roundActive = false;
            roundWait = 7f;
            say("Ronda " + round + " superada");
        }
    }

    private void spawnZombie() {
        int tries = 0;
        float sx = 0, sz = 0;
        boolean ok = false;
        while (tries++ < 12 && !ok) {
            int zn;
            int open = 0;
            for (int i = 0; i < 5; i++) if (zoneOpen[i]) open++;
            int pick = (int) (rnd() * open);
            zn = 0;
            for (int i = 0; i < 5; i++) if (zoneOpen[i]) { if (pick == 0) { zn = i; break; } pick--; }
            float[][] pts = world.zoneSpawns[zn];
            float[] p = pts[(int) (rnd() * pts.length)];
            float d = (float) Math.hypot(p[0] - px, p[1] - pz);
            if (d < 15f) continue;
            sx = p[0] + (rnd() - 0.5f) * 2f; sz = p[1] + (rnd() - 0.5f) * 2f; ok = true;
        }
        if (!ok) {
            float[][] pts = world.zoneSpawns[0];
            float[] p = pts[(int) (rnd() * pts.length)];
            sx = p[0]; sz = p[1];
        }
        Zombie z = new Zombie();
        z.x = sx; z.z = sz; z.y = world.ground(sx, sz);
        z.state = 0; z.riseT = 0;
        z.yaw = (float) Math.toDegrees(Math.atan2(px - sx, -(pz - sz)));
        int bonus = mode == MODE_FAST ? 3 : 0;
        float pr = Noise.clamp((round + bonus - 2) * 0.13f, 0f, 0.9f);
        float ps2 = Noise.clamp((round + bonus - 6) * 0.1f, 0f, 0.65f);
        float r = rnd();
        z.tier = r < ps2 ? 2 : (rnd() < pr ? 1 : 0);
        z.speed = z.tier == 0 ? 1.2f + rnd() * 0.6f + Math.min(0.5f, round * 0.03f) : z.tier == 1 ? 2.6f + rnd() * 0.9f : 3.9f + rnd() * 0.6f;
        z.maxHp = z.hp = roundHp(round);
        if (round >= 5 && rnd() < 0.09f) { z.brute = true; z.scale = 1.28f; z.maxHp = z.hp = z.maxHp * 2.4f; z.speed *= 0.78f; }
        else z.scale = 0.94f + rnd() * 0.14f;
        float t = 0.75f + rnd() * 0.5f;
        z.tr = t * (0.9f + rnd() * 0.2f); z.tg = t; z.tb = t * (0.85f + rnd() * 0.3f);
        z.phase = rnd() * 6f;
        z.seed = rnd();
        z.hunger = rnd() * 0.3f;
        z.slot = (rnd() - 0.5f) * 0.9f;
        { float vr = rnd(); z.variant = vr < 0.3f ? 0 : vr < 0.55f ? 1 : vr < 0.85f ? 2 : 3; }
        { float gr = rnd(); z.gait = gr < 0.35f ? 0 : gr < 0.55f ? 1 : gr < 0.8f ? 2 : 3; }
        z.grT = 1f + rnd() * 3f;
        z.side = rnd() < 0.5f ? 1 : -1;
        zs.add(z);
        ps.dust(sx, z.y + 0.1f, sz, 0, 1, 0, 8, 0.25f, 0.2f, 0.14f);
    }

    private Nav nav;
    private final float[] ndir = new float[2];

    private void updateZombies(float dt) {
        nav.update(dt, px, pz);
        for (int i = zs.size() - 1; i >= 0; i--) {
            Zombie z = zs.get(i);
            if (z.hitFlash > 0) z.hitFlash -= dt;
            if (z.state == 2) {
                z.deathT += dt;
                if (z.deathT > 7f) zs.remove(i);
                continue;
            }
            if (over) { z.phase += dt * 2f; continue; }
            z.y = world.ground(z.x, z.z);
            if (z.state == 0) {
                z.riseT += dt / 1.5f;
                if (z.riseT >= 1f) { z.riseT = 1f; z.state = 1; }
                if (((int) (z.riseT * 20)) % 5 == 0 && rnd() < 0.2f) ps.dust(z.x, z.y + 0.1f, z.z, 0, 1, 0, 1, 0.25f, 0.2f, 0.14f);
                continue;
            }
            if (z.stunT > 0) {
                z.stunT -= dt; z.stunFx -= dt;
                if (z.stunFx <= 0) { z.stunFx = 0.1f; ps.sparks(z.x + (rnd() - 0.5f) * 0.5f, z.y + rnd() * 1.8f * z.scale, z.z + (rnd() - 0.5f) * 0.5f, 0, 1, 0, 2, 0.5f, 1f, 1.9f); }
                continue;
            }
            float dx = px - z.x, dz = pz - z.z;
            float d = (float) Math.sqrt(dx * dx + dz * dz);
            float want = (float) Math.toDegrees(Math.atan2(dx, -dz));
            float diff = want - z.yaw;
            while (diff > 180) diff -= 360;
            while (diff < -180) diff += 360;
            if (z.los) z.yaw += Noise.clamp(diff, -360f * dt, 360f * dt);
            // hambre: crece mientras no comen; con sangre cerca y al verte se enfurecen
            z.hunger = Math.min(1f, z.hunger + dt / 45f);
            if (z.enrageT > 0) z.enrageT -= dt;
            if (z.lungeT > 0) z.lungeT -= dt;
            if (z.lungeCd > 0) z.lungeCd -= dt;
            float frenzy = 1f + 0.32f * z.hunger + (health < 0.4f * maxHealth ? 0.12f : 0f) + (z.enrageT > 0 ? 0.4f : 0f);
            if (z.hunger >= 1f && z.tier == 0) { z.tier = 1; z.speed *= 1.35f; }
            if (!z.spotted && z.los && d < 32f) {
                z.spotted = true; z.enrageT = 2.2f;
                sfx.play(rnd() < 0.5f ? Sfx.SCREAM : Sfx.SNARL, Math.max(0.15f, 0.6f - d / 60f), pan(z.x, z.z));
            }
            z.phase += dt * z.speed * frenzy * 1.9f;
            if (z.atkT > 0) z.atkT -= dt;
            if (z.atkCd > 0) z.atkCd -= dt;
            if (d > 1.05f * z.scale) {
                float mvx = dx / d, mvz = dz / d;
                // inteligencia colectiva: ¿ve al jugador? si no, sigue el campo de flujo compartido
                z.losT -= dt;
                if (z.losT <= 0) {
                    z.losT = 0.2f + rnd() * 0.15f;
                    z.los = d < 2.5f || world.rayBlock(z.x, 0.3f, z.z, dx / d, 0f, dz / d, d) > 1.5f;
                }
                if (z.los && d > 2.8f && d < 11f && z.tier < 2) {
                    // rodean: cada zombi apunta a una posición distinta alrededor del jugador
                    float sa = z.seed * 6.2832f + z.slot;
                    float tx = px + (float) Math.sin(sa) * 1.6f - z.x, tz = pz + (float) Math.cos(sa) * 1.6f - z.z;
                    float tl = (float) Math.sqrt(tx * tx + tz * tz);
                    if (tl > 0.01f) { mvx = tx / tl; mvz = tz / tl; }
                } else if (!z.los && nav.dir(z.x, z.z, z.seed, ndir)) {
                    mvx = ndir[0]; mvz = ndir[1];
                }
                // gira hacia donde camina, no hacia el jugador, cuando rodea obstáculos
                if (!z.los) {
                    float wy = (float) Math.toDegrees(Math.atan2(mvx, -mvz));
                    float df = wy - z.yaw;
                    while (df > 180) df -= 360;
                    while (df < -180) df += 360;
                    z.yaw += Noise.clamp(df, -300f * dt, 300f * dt);
                }
                if (z.sideT > 0) {
                    z.sideT -= dt;
                    float c = (float) Math.cos(Math.toRadians(70 * z.side)), s = (float) Math.sin(Math.toRadians(70 * z.side));
                    float nx = mvx * c - mvz * s, nz = mvx * s + mvz * c;
                    mvx = nx; mvz = nz;
                }
                if (z.los && d < 2.8f * z.scale && z.lungeT <= 0 && z.lungeCd <= 0 && z.atkT <= 0 && z.stunT <= 0) { z.lungeT = 0.25f; z.lungeCd = 2.4f - 1.2f * z.hunger; z.atkT = 0.45f; }
                float spd = z.speed * frenzy * (z.atkT > 0 ? 0.3f : 1f);
                if (z.lungeT > 0) spd = 7.5f;
                tmp[0] = z.x + mvx * spd * dt; tmp[1] = z.z + mvz * spd * dt;
                        world.resolve(tmp, 0.38f * z.scale);
                float moved = (float) Math.hypot(tmp[0] - z.x, tmp[1] - z.z);
                if (moved < spd * dt * 0.35f) {
                    z.stuckT += dt;
                    if (z.stuckT > 0.35f) { z.sideT = 0.9f + rnd() * 0.8f; z.side = -z.side; z.stuckT = 0; }
                } else z.stuckT = Math.max(0, z.stuckT - dt);
                z.x = tmp[0]; z.z = tmp[1];
            } else if (z.atkCd <= 0) {
                z.atkCd = 1.05f - 0.3f * z.hunger; z.atkT = 0.45f;
                z.hunger *= 0.5f;   // muerde: calma un poco el hambre
                sfx.play(Sfx.SWIPE, 0.4f, pan(z.x, z.z));
                hurtPlayer(z.brute ? 55f : 34f);
            }
            // separación entre zombis
            for (int j = i - 1; j >= 0; j--) {
                Zombie o = zs.get(j);
                if (o.state == 2) continue;
                float ex = z.x - o.x, ez = z.z - o.z, dd = ex * ex + ez * ez;
                float min = 0.7f * (z.scale + o.scale) * 0.5f;
                if (dd < min * min && dd > 1e-5f) {
                    float l = (float) Math.sqrt(dd), push = (min - l) * 0.5f;
                    z.x += ex / l * push; z.z += ez / l * push; o.x -= ex / l * push; o.z -= ez / l * push;
                }
            }
            z.grT -= dt;
            if (z.grT <= 0) {
                z.grT = 2.5f + rnd() * 5f;
                if (d < 38f) {
                    float vv = Math.max(0.08f, 0.55f - d / 70f) * (z.brute ? 1.2f : 1f);
                    int snd = z.tier == 2 ? (rnd() < 0.5f ? Sfx.SCREAM : Sfx.SNARL) : (z.tier == 1 && rnd() < 0.35f ? Sfx.SNARL : Sfx.GROAN1 + (int) (rnd() * 3));
                    sfx.play(snd, vv, pan(z.x, z.z));
                }
            }
        }
    }

    private void hurtPlayer(float amount) {
        health -= amount;
        sinceHit = 0f;
        dmgFlash = 1f;
        sfx.play(Sfx.HURT, 0.75f);
        if (Prefs.vibrate()) vibrateReq = true;
        pitch += (rnd() - 0.3f) * 5f;
        yaw += (rnd() - 0.5f) * 8f;
    }

    private void die() {
        over = true;
        health = 0;
        sfx.play(Sfx.END, 0.9f);
        if (mode != MODE_TEST) { Prefs.saveRecord(mode, round, points, kills); Prefs.addKills(kills); }
        in.clear();
    }

    private void updatePowerUps(float dt) {
        for (int i = ups.size() - 1; i >= 0; i--) {
            PowerUp u = ups.get(i);
            u.life -= dt; u.spin += dt * 120f;
            if (u.life <= 0) { ups.remove(i); continue; }
            if (Math.hypot(u.x - px, u.z - pz) < 1.6f) {
                applyPower(u.type);
                ups.remove(i);
            }
        }
    }

    private void applyPower(int t) {
        sfx.play(Sfx.POWER, 0.8f);
        ps.ring(px, py - 1f, pz, 3f, 0.9f, 0.9f, 0.4f, 0.6f);
        if (t == 0) {
            for (int s = 0; s < 2; s++) if (slotW[s] >= 0) { ammo[s][0] = Weapons.MAG[slotW[s]]; ammo[s][1] = Weapons.RESERVE[slotW[s]]; }
            say("Munición máxima");
        } else if (t == 1) { instaT = 30f; say("Muerte instantánea"); }
        else if (t == 2) { doubleT = 30f; say("Puntos dobles"); }
        else {
            say("Bomba");
            lightning = 1f;
            sfx.play(Sfx.CRACK, 0.9f);
            for (int i = 0; i < zs.size(); i++) {
                Zombie z = zs.get(i);
                if (z.state == 2) continue;
                z.hp = 0;
                z.state = 2; z.deathT = 0; kills++; alive--;
                ps.blood(z.x, z.y + 1.2f, z.z, 0, 0, 0, 5);
                ps.flash(z.x, z.y + 1.2f, z.z, 1.6f, 1f, 0.6f, 0.3f, 0.35f);
            }
            addPoints(400);
        }
    }

    // ------------------------------------------------------------------ compras
    private void updateBuying() {
        prompt = "";
        boolean use = in.takeUse();
        World.Buyable best = null;
        float bd = 1e9f;
        for (int i = 0; i < world.buyables.size(); i++) {
            World.Buyable b = world.buyables.get(i);
            if (b.type == World.B_DOOR && world.doors.get(b.id).open) continue;
            float d = (float) Math.hypot(b.x - px, b.z - pz);
            if (d < b.r && d < bd) { bd = d; best = b; }
        }
        if (best == null) return;
        int cost = best.cost;
        String line;
        boolean owned = false;
        if (best.type == World.B_WALL) {
            for (int s = 0; s < 2; s++) if (slotW[s] == best.id) owned = true;
            if (owned) cost = best.cost / 2;
            line = (owned ? "Munición " : "Comprar ") + best.name + " [" + cost + "]";
        } else if (best.type == World.B_PERK) {
            if (hasPerk(best.id)) { prompt = best.name + " (ya lo tienes)"; return; }
            line = "Beber " + best.name + " [" + cost + "]";
        } else if (best.type == World.B_BOX) {
            line = "Caja Misteriosa [" + cost + "]";
        } else {
            line = best.name + " [" + cost + "]";
        }
        prompt = line;
        promptCost = cost;
        if (!use) return;
        if (points < cost) { sfx.play(Sfx.EMPTY, 0.6f); say("Puntos insuficientes"); return; }
        if (best.type == World.B_DOOR) {
            points -= cost;
            openDoor(best.id);
        } else if (best.type == World.B_WALL) {
            points -= cost;
            sfx.play(Sfx.CASH, 0.7f);
            if (owned) { for (int s = 0; s < 2; s++) if (slotW[s] == best.id) { ammo[s][0] = Weapons.MAG[best.id]; ammo[s][1] = Weapons.RESERVE[best.id]; } }
            else giveWeapon(best.id);
        } else if (best.type == World.B_PERK) {
            points -= cost;
            perks |= 1 << best.id;
            sfx.play(Sfx.CASH, 0.7f);
            sfx.play(Sfx.POWER, 0.5f);
            if (best.id == World.PERK_VIGOR) { maxHealth = 250f; health = 250f; }
            say(best.name + " activado");
        } else if (best.type == World.B_BOX) {
            points -= cost;
            sfx.play(Sfx.CASH, 0.7f);
            sfx.play(Sfx.BOX, 0.7f);
            int r = (int) (rnd() * 4);
            if (rnd() < 0.12f) r = 4;
            int[] pool = {Weapons.SHOTGUN, Weapons.RIFLE, Weapons.SMG, Weapons.PISTOL, Weapons.STORM};
            giveWeapon(pool[r]);
            say("Caja: " + Weapons.NAME[pool[r]]);
        }
    }

    private void giveWeapon(int w) {
        for (int s = 0; s < 2; s++) if (slotW[s] == w) { ammo[s][0] = Weapons.MAG[w]; ammo[s][1] = Weapons.RESERVE[w]; cur = s; swapT = 0.4f; return; }
        int slot = slotW[1] < 0 ? 1 : cur;
        slotW[slot] = w;
        ammo[slot][0] = Weapons.MAG[w]; ammo[slot][1] = Weapons.RESERVE[w];
        cur = slot; swapT = 0.5f; sfx.play(Sfx.SWITCH, 0.4f); reloadT = 0; adsOn = false;
    }

    private void openDoor(int id) {
        World.Door d = world.doors.get(id);
        d.open = true;
        d.col.active = false;
        zoneOpen[d.zone] = true;
        sfx.play(Sfx.DOOR, 0.9f);
        sfx.play(Sfx.CASH, 0.6f);
        for (int i = 0; i < 18; i++) {
            float ox = d.nx == 1 ? (rnd() - 0.5f) * 2f : (rnd() - 0.5f) * 8f, oz = d.nx == 1 ? (rnd() - 0.5f) * 8f : (rnd() - 0.5f) * 2f;
            ps.dust(d.x + ox, 1.0f + rnd() * 2.5f, d.z + oz, 0, 0.5f, 0, 1, 0.35f, 0.3f, 0.25f);
        }
        say("Zona abierta");
    }

    // ------------------------------------------------------------------ ambiente
    private void updateAmbience(float dt) {
        // relámpagos
        nextBolt -= dt;
        if (nextBolt <= 0) {
            nextBolt = 7f + rnd() * 14f;
            lightning = 1f;
            thunderDelay = 0.4f + rnd() * 2.2f;
            sfx.play(Sfx.CRACK, 0.35f);
        }
        if (lightning > 0) lightning -= dt * (lightning > 0.55f ? 3f : 1.4f);
        if (lightning < 0) lightning = 0;
        if (thunderDelay > 0) {
            thunderDelay -= dt;
            if (thunderDelay <= 0) sfx.play(Sfx.THUNDER, 0.8f);
        }
        // fuego: brasas y humo
        for (int i = 0; i < world.fires.size(); i++) {
            World.Fire f = world.fires.get(i);
            float d2 = (f.x - px) * (f.x - px) + (f.z - pz) * (f.z - pz);
            if (d2 > 45f * 45f) continue;
            if (rnd() < dt * 5f * f.size) ps.ember(f.x, f.y, f.z);
            if (rnd() < dt * 2.2f * f.size) ps.smoke(f.x, f.y + 0.4f, f.z, 0.3f * f.size, 0.18f, 0.17f, 0.16f, 0.35f, 2.8f, 1.1f);
        }
        // niebla baja
        if (rnd() < dt * 4f) {
            float a = rnd() * 6.283f, d = 6f + rnd() * 24f;
            float gx = px + (float) Math.cos(a) * d, gz = pz + (float) Math.sin(a) * d;
            ps.smoke(gx, world.ground(gx, gz) + 0.4f, gz, 2.4f, 0.32f, 0.38f, 0.42f, 0.10f, 6f, 0.02f);
        }
        sfx.setRain(mode == MODE_NIGHT ? 0.9f : 0.6f);
    }
}
