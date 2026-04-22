package com.angelo.mmorpg.entity;

/**
 * Gerencia XP e níveis do player.
 * XP necessário por nível: base * (nível ^ 1.5) — curva suave.
 * Ao subir de nível: +5 HP máx, +1 força, +1 defesa.
 */
public class ExperienceSystem {

    private static final int   BASE_XP    = 100; // XP para o nível 2
    private static final float XP_SCALING = 1.5f;

    // XP concedido por ação
    public static final int XP_KILL_SKELETON = 25;
    public static final int XP_KILL_LICH     = 120;
    public static final int XP_KILL_VILLAGER = 15;
    public static final int XP_KILL_DEER     = 8;
    public static final int XP_COLLECT_STONE = 3;
    public static final int XP_COLLECT_WOOD  = 4;

    private int   level  = 1;
    private int   xp     = 0;
    private int   xpNext = BASE_XP; // XP para o próximo nível

    private final PlayerStats stats;

    // Callback para notificação de level up (pode ser null)
    private Runnable onLevelUp;

    public ExperienceSystem(PlayerStats stats) {
        this.stats = stats;
    }

    public void setOnLevelUp(Runnable callback) {
        this.onLevelUp = callback;
    }

    /**
     * Adiciona XP e verifica level up.
     */
    public void addXp(int amount) {
        xp += amount;
        while (xp >= xpNext) {
            xp     -= xpNext;
            level++;
            xpNext  = calcXpForLevel(level + 1);
            applyLevelUpBonus();
            if (onLevelUp != null) onLevelUp.run();
        }
    }

    private int calcXpForLevel(int lvl) {
        return (int) (BASE_XP * Math.pow(lvl - 1, XP_SCALING));
    }

    private void applyLevelUpBonus() {
        stats.increaseStrength(1);
        stats.increaseDefense(1);
        stats.increaseMaxHp(5);
    }

    public int   getLevel()       { return level; }
    public int   getXp()          { return xp; }
    public int   getXpNext()      { return xpNext; }
    public float getXpRatio()     { return (float) xp / xpNext; }
}