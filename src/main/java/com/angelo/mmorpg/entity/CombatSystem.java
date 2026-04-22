package com.angelo.mmorpg.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Resolve combate e concede XP ao matar criaturas.
 */
public class CombatSystem {

    private static final int LOG_MAX = 8;

    private final Random       rng = new Random();
    private final List<String> log = new ArrayList<>();

    public int playerAttack(Player player, Creature target) {
        if (target.isDead()) return 0;

        int str  = player.getStats().getStrength();
        int base = str / 2;
        int dmg  = Math.max(1, base + rng.nextInt(5) - 2 - target.getType().defense);

        target.takeDamage(dmg);
        addLog("You hit " + target.getType().name + " for " + dmg + " dmg.");

        if (target.isDead()) {
            addLog(target.getType().name + " has been slain!");
            int xp = switch (target.getType()) {
                case SKELETON -> ExperienceSystem.XP_KILL_SKELETON;
                case LICH     -> ExperienceSystem.XP_KILL_LICH;
                case VILLAGER -> ExperienceSystem.XP_KILL_VILLAGER;
                case DEER     -> ExperienceSystem.XP_KILL_DEER;
            };
            player.getExperience().addXp(xp);
            addLog("+" + xp + " XP");
        }

        return dmg;
    }

    public int creatureAttack(Creature attacker, Player target) {
        CreatureType t   = attacker.getType();
        int          dmg = t.damageMin + rng.nextInt(Math.max(1, t.damageMax - t.damageMin + 1));
        dmg = Math.max(1, dmg - target.getStats().getDefense());

        target.getStats().takeDamage(dmg);
        addLog(t.name + " hits you for " + dmg + " dmg.");

        if (target.getStats().isDead()) addLog("You have been slain!");
        return dmg;
    }

    private void addLog(String msg) {
        log.add(0, msg);
        if (log.size() > LOG_MAX) log.remove(log.size() - 1);
    }

    public List<String> getLog() { return log; }
}