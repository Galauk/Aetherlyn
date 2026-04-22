package com.angelo.mmorpg.entity;

/**
 * Atributos base do player.
 * Inclui regeneração de HP fora de combate e métodos de progressão.
 */
public class PlayerStats {

    private static final float COMBAT_TIMEOUT = 5.0f;
    private static final float REGEN_PER_SEC  = 1.5f;

    private int   strength;
    private int   defense;
    private int   maxHp;
    private float hp;
    private float maxWeight;

    private float   combatTimer = 0f;
    private boolean inCombat    = false;

    public PlayerStats() { this(10, 2, 50); }

    public PlayerStats(int strength, int defense, int maxHp) {
        this.strength  = strength;
        this.defense   = defense;
        this.maxHp     = maxHp;
        this.hp        = maxHp;
        this.maxWeight = calcMaxWeight(strength);
    }

    public void update(float delta) {
        if (isDead()) return;
        if (inCombat) {
            combatTimer -= delta;
            if (combatTimer <= 0) { inCombat = false; combatTimer = 0; }
        }
        if (!inCombat && hp < maxHp) {
            hp = Math.min(maxHp, hp + REGEN_PER_SEC * delta);
        }
    }

    public void takeDamage(int amount) {
        hp          = Math.max(0, hp - amount);
        inCombat    = true;
        combatTimer = COMBAT_TIMEOUT;
    }

    public void heal(int amount)           { hp = Math.min(maxHp, hp + amount); }

    public void respawn() {
        hp = maxHp; inCombat = false; combatTimer = 0;
    }

    public void increaseStrength(int n)    { strength += n; maxWeight = calcMaxWeight(strength); }
    public void increaseDefense(int n)     { defense  += n; }
    public void increaseMaxHp(int n)       { maxHp    += n; hp = Math.min(hp + n, maxHp); }

    private float calcMaxWeight(int str)   { return str * 3.5f; }

    public boolean isDead()      { return hp <= 0; }
    public boolean isInCombat()  { return inCombat; }
    public int   getStrength()   { return strength; }
    public int   getDefense()    { return defense; }
    public int   getHp()         { return (int) Math.ceil(hp); }
    public int   getMaxHp()      { return maxHp; }
    public float getHpExact()    { return hp; }
    public float getMaxWeight()  { return maxWeight; }
}