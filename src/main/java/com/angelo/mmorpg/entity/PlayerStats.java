package com.angelo.mmorpg.entity;

/**
 * Atributos base do player.
 * Inclui regeneração de HP fora de combate.
 */
public class PlayerStats {

    // Tempo sem receber dano para sair do modo combate
    private static final float COMBAT_TIMEOUT = 5.0f;
    // HP regenerado por segundo fora de combate
    private static final float REGEN_PER_SEC  = 1.5f;

    private int   strength;
    private int   defense;
    private int   maxHp;
    private float hp;        // float para regeneração suave
    private float maxWeight;

    private float combatTimer = 0f; // tempo desde o último dano recebido
    private boolean inCombat  = false;

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

    /**
     * Atualiza regeneração e timer de combate.
     * Chamado a cada tick.
     */
    public void update(float delta) {
        if (isDead()) return;

        if (inCombat) {
            combatTimer -= delta;
            if (combatTimer <= 0) {
                inCombat    = false;
                combatTimer = 0;
            }
        }

        // Regenera fora de combate
        if (!inCombat && hp < maxHp) {
            hp = Math.min(maxHp, hp + REGEN_PER_SEC * delta);
        }
    }

    public void takeDamage(int amount) {
        hp          = Math.max(0, hp - amount);
        inCombat    = true;
        combatTimer = COMBAT_TIMEOUT;
    }

    public void heal(int amount) {
        hp = Math.min(maxHp, hp + amount);
    }

    /** Respawn: restaura HP completo e sai de combate. */
    public void respawn() {
        hp          = maxHp;
        inCombat    = false;
        combatTimer = 0;
    }

    private float calcMaxWeight(int str) { return str * 3.5f; }

    public void increaseStrength(int amount) {
        strength  += amount;
        maxWeight  = calcMaxWeight(strength);
    }

    public boolean isDead()     { return hp <= 0; }
    public boolean isInCombat() { return inCombat; }
    public int   getStrength()  { return strength; }
    public int   getDefense()   { return defense; }
    public int   getHp()        { return (int) Math.ceil(hp); }
    public int   getMaxHp()     { return maxHp; }
    public float getHpExact()   { return hp; }
    public float getMaxWeight() { return maxWeight; }
}