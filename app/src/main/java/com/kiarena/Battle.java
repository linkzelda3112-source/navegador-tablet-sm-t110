package com.kiarena;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Lógica pura del combate por turnos 3 vs 3 (sin nada de Android, para poder probarla aparte).
 * Cada turno ambos bandos eligen una acción a la vez: una carta de la mano, o cambiar de guerrero.
 * turn() resuelve todo y devuelve la lista de eventos para que la pantalla los anime.
 */
public final class Battle {
    public static final int PLAYER = 0, ENEMY = 1, MAX_KI = 8;
    public static final int E_STRIKE = 0, E_BLAST = 1, E_ULTI = 2, E_GUARD = 3, E_HEAL = 4,
            E_BUFF = 5, E_CHARGE = 6, E_SWAP = 7, E_KO = 8;

    public static int swapTo(int k) { return -10 - k; }
    public static boolean isSwap(int act) { return act <= -10; }

    public static final class Unit {
        public final Fighter f;
        public int hp;
        public final int maxHp;
        public final float atk;
        public int buff; // turnos restantes de ATQ +40%

        Unit(Fighter f, float mult) {
            this.f = f;
            this.maxHp = Math.round(f.hp * mult);
            this.hp = maxHp;
            this.atk = f.atk * mult;
        }

        public boolean alive() { return hp > 0; }
    }

    public static final class Ev {
        public int type, actor, aIdx, tIdx, value, hpAfter, mult;
        public boolean crit, guarded, auto;
        public String text = "";
    }

    public final Unit[][] team = new Unit[2][3];
    public final int[] active = new int[2];
    public final int[] ki = {3, 3};
    public final int[][] hand = new int[2][4];
    public final boolean[] guard = new boolean[2];
    public int winner = -1;
    public int turn = 1;
    private final Random rnd;

    public Battle(Fighter[] player, Fighter[] enemy, float enemyMult, long seed) {
        rnd = new Random(seed);
        for (int i = 0; i < 3; i++) {
            team[PLAYER][i] = new Unit(player[i], 1f);
            team[ENEMY][i] = new Unit(enemy[i], enemyMult);
        }
        deal(PLAYER);
        deal(ENEMY);
    }

    public Unit unit(int s) { return team[s][active[s]]; }

    public Card card(int s, int idx) { return unit(s).f.deck[idx]; }

    private void deal(int s) {
        List<Integer> others = new ArrayList<>();
        for (int i = 1; i < 6; i++) others.add(i);
        Collections.shuffle(others, rnd);
        hand[s][0] = 0; // el golpe básico (costo 0) siempre está disponible
        for (int i = 0; i < 3; i++) hand[s][i + 1] = others.get(i);
    }

    public boolean inHand(int s, int cardIdx) {
        for (int c : hand[s]) if (c == cardIdx) return true;
        return false;
    }

    // ---------------------------------------------------------------- IA

    public int aiChoose(int s, int level) {
        int o = 1 - s;
        Unit u = unit(s);
        Unit d = unit(o);
        List<Integer> legal = new ArrayList<>();
        for (int c : hand[s]) if (u.f.deck[c].cost <= ki[s]) legal.add(c);

        // 1) curarse si está débil
        if (level >= 2 && u.hp < u.maxHp * 0.35f) {
            for (int c : legal) if (u.f.deck[c].kind == Card.HEAL && rnd.nextFloat() < 0.75f) return c;
        }
        // 2) definitivo
        for (int c : legal) {
            if (u.f.deck[c].kind == Card.ULTI && rnd.nextFloat() < 0.5f + 0.07f * level) return c;
        }
        // 3) cambiar si tiene desventaja de tipo y un compañero tiene ventaja
        if (level >= 3 && rnd.nextFloat() < 0.25f && advantage(d.f.type, u.f.type) > 0) {
            for (int k = 0; k < 3; k++) {
                if (k != active[s] && team[s][k].alive() && advantage(team[s][k].f.type, d.f.type) > 0) {
                    return swapTo(k);
                }
            }
        }
        // 4) guardia si el rival puede soltar un definitivo
        if (ki[o] >= 6 && rnd.nextFloat() < 0.25f + 0.05f * level) {
            for (int c : legal) if (u.f.deck[c].kind == Card.GUARD) return c;
        }
        // 5) al azar ponderado por poder
        int total = 0;
        int[] w = new int[legal.size()];
        for (int i = 0; i < legal.size(); i++) {
            Card c = u.f.deck[legal.get(i)];
            int wt;
            switch (c.kind) {
                case Card.GUARD: wt = 15; break;
                case Card.HEAL: wt = u.hp < u.maxHp * 0.7f ? 60 : 5; break;
                case Card.BUFF: wt = u.buff > 0 ? 3 : 70; break;
                case Card.CHARGE: wt = ki[s] < 4 ? 50 : 8; break;
                case Card.ULTI: wt = 40; break;
                default: wt = 20 + c.power / 2; break;
            }
            w[i] = wt;
            total += wt;
        }
        int r = rnd.nextInt(Math.max(1, total));
        for (int i = 0; i < legal.size(); i++) {
            r -= w[i];
            if (r < 0) return legal.get(i);
        }
        return 0;
    }

    /** 1 si 'a' tiene ventaja sobre 'd', -1 si desventaja, 0 si neutro. Fuerza>Técnica>Ki>Fuerza. */
    public static int advantage(int a, int d) {
        if (d == (a + 1) % 3) return 1;
        if (a == (d + 1) % 3) return -1;
        return 0;
    }

    // ------------------------------------------------------------ turno

    public List<Ev> turn(int pAct, int eAct) {
        List<Ev> evs = new ArrayList<>();
        int[] act = {pAct, eAct};
        int[] startIdx = {active[0], active[1]};
        boolean[] done = new boolean[2];
        guard[0] = guard[1] = false;

        // 1) cambios de guerrero
        for (int s = 0; s < 2; s++) {
            if (isSwap(act[s])) {
                done[s] = true;
                int k = -10 - act[s];
                if (k >= 0 && k < 3 && k != active[s] && team[s][k].alive()) {
                    active[s] = k;
                    Ev e = new Ev();
                    e.type = E_SWAP; e.actor = s; e.aIdx = k;
                    e.text = team[s][k].f.name;
                    evs.add(e);
                }
            }
        }
        // 2) guardias
        for (int s = 0; s < 2; s++) {
            if (done[s]) continue;
            Card c = card(s, act[s]);
            if (c.kind == Card.GUARD && ki[s] >= c.cost) {
                ki[s] -= c.cost;
                guard[s] = true;
                done[s] = true;
                Ev e = new Ev();
                e.type = E_GUARD; e.actor = s; e.aIdx = active[s]; e.text = c.name;
                evs.add(e);
            }
        }
        // 3) el resto, ordenado por velocidad
        List<Integer> order = new ArrayList<>();
        float[] sp = new float[2];
        for (int s = 0; s < 2; s++) {
            if (done[s]) continue;
            order.add(s);
            Card c = card(s, act[s]);
            sp[s] = unit(s).f.spd + rnd.nextInt(13)
                    + (c.kind == Card.STRIKE ? 15 : 0) + (c.kind == Card.ULTI ? -20 : 0);
        }
        if (order.size() == 2 && sp[1] > sp[0]) Collections.reverse(order);
        for (int s : order) perform(s, act[s], startIdx[s], evs);

        // 4) fin de turno
        if (winner < 0) {
            for (int s = 0; s < 2; s++) {
                ki[s] = Math.min(MAX_KI, ki[s] + 2 + (guard[s] ? 1 : 0));
                for (Unit u : team[s]) if (u.buff > 0) u.buff--;
            }
            turn++;
            deal(PLAYER);
            deal(ENEMY);
        }
        return evs;
    }

    private void perform(int s, int cardIdx, int startIdx, List<Ev> evs) {
        if (winner >= 0) return;
        Unit a = unit(s);
        if (active[s] != startIdx || !a.alive()) return; // murió o fue reemplazado antes de actuar
        Card c = a.f.deck[cardIdx];
        if (ki[s] < c.cost) c = a.f.deck[0];
        ki[s] -= c.cost;
        int o = 1 - s;
        Unit d = unit(o);
        Ev e = new Ev();
        e.actor = s;
        e.aIdx = active[s];
        e.text = c.name;

        switch (c.kind) {
            case Card.STRIKE:
            case Card.BLAST:
            case Card.ULTI: {
                e.type = c.kind == Card.STRIKE ? E_STRIKE : (c.kind == Card.BLAST ? E_BLAST : E_ULTI);
                e.tIdx = active[o];
                e.mult = advantage(a.f.type, d.f.type);
                e.guarded = guard[o];
                e.crit = rnd.nextFloat() < 0.12f;
                float dmg = a.atk * c.power / 100f * 4f;
                float df = d.f.def * (c.kind == Card.STRIKE ? 1f : 0.6f);
                dmg = dmg * 100f / (100f + df);
                if (e.mult > 0) dmg *= 1.25f;
                if (e.mult < 0) dmg *= 0.8f;
                if (a.buff > 0) dmg *= 1.4f;
                if (e.crit) dmg *= 1.5f;
                if (guard[o]) dmg *= (c.kind == Card.STRIKE ? 0.3f : 0.55f);
                dmg *= 0.93f + rnd.nextFloat() * 0.14f;
                int dm = Math.max(1, Math.round(dmg));
                d.hp = Math.max(0, d.hp - dm);
                e.value = dm;
                e.hpAfter = d.hp;
                evs.add(e);
                if (d.hp <= 0) {
                    Ev k = new Ev();
                    k.type = E_KO; k.actor = o; k.aIdx = active[o]; k.tIdx = active[o];
                    k.text = d.f.name;
                    evs.add(k);
                    int next = -1;
                    for (int i = 0; i < 3; i++) if (team[o][i].alive()) { next = i; break; }
                    if (next < 0) {
                        winner = s;
                    } else {
                        active[o] = next;
                        Ev w = new Ev();
                        w.type = E_SWAP; w.actor = o; w.aIdx = next; w.auto = true;
                        w.text = team[o][next].f.name;
                        evs.add(w);
                    }
                }
                break;
            }
            case Card.HEAL: {
                int heal = Math.round(a.maxHp * c.power / 100f);
                a.hp = Math.min(a.maxHp, a.hp + heal);
                e.type = E_HEAL; e.tIdx = active[s]; e.value = heal; e.hpAfter = a.hp;
                evs.add(e);
                break;
            }
            case Card.BUFF:
                a.buff = 3;
                e.type = E_BUFF; e.value = c.power;
                evs.add(e);
                break;
            case Card.CHARGE:
                ki[s] = Math.min(MAX_KI, ki[s] + c.power);
                e.type = E_CHARGE; e.value = c.power;
                evs.add(e);
                break;
            default:
                break;
        }
    }
}
