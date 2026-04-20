package com.angelo.mmorpg.entity;

/**
 * Atributos base do player.
 * Força → peso máximo carregável e dano base.
 * Defesa → reduz dano recebido.
 */
public class PlayerStats {

    private int   strength;
    private int   defense;
    private int   maxHp;
    private int   hp;
    private float maxWeight;

    public PlayerStats() {
        this(10, 2, 50);
    }

    public PlayerStats(int strength, int defense, int maxHp) {
        this.strength  = strength;
        this.defense   = defense;
        this.maxHp     = maxHp;
        this.hp        = maxHp;
        this.maxWeight = calcMaxWeight(strength);
    }

    private float calcMaxWeight(int str) { return str * 3.5f; }

    public void takeDamage(int amount) {
        hp = Math.max(0, hp - amount);
    }

    public void heal(int amount) {
        hp = Math.min(maxHp, hp + amount);
    }

    public void increaseStrength(int amount) {
        strength  += amount;
        maxWeight  = calcMaxWeight(strength);
    }

    public boolean isDead()     { return hp <= 0; }
    public int   getStrength()  { return strength; }
    public int   getDefense()   { return defense; }
    public int   getHp()        { return hp; }
    public int   getMaxHp()     { return maxHp; }
    public float getMaxWeight() { return maxWeight; }
}