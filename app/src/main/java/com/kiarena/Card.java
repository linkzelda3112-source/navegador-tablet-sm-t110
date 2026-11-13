package com.kiarena;

/** Una carta de movimiento. Cada guerrero tiene 6 en su mazo. */
public final class Card {
    public static final int STRIKE = 0, BLAST = 1, GUARD = 2, HEAL = 3, BUFF = 4, CHARGE = 5, ULTI = 6;

    public final String name;
    public final int kind;
    public final int cost;
    /** Daño: porcentaje de poder. Cura: % de vida. Carga: ki ganado. */
    public final int power;

    public Card(String name, int kind, int cost, int power) {
        this.name = name;
        this.kind = kind;
        this.cost = cost;
        this.power = power;
    }

    public String desc() {
        switch (kind) {
            case STRIKE: return "Golpe · " + power;
            case BLAST: return "Ki · " + power;
            case GUARD: return "Reduce daño";
            case HEAL: return "Cura " + power + "%";
            case BUFF: return "ATQ +40%";
            case CHARGE: return "+" + power + " Ki";
            default: return "¡DEFINITIVO! " + power;
        }
    }
}
