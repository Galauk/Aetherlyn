package com.angelo.mmorpg.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Resolve combate e concede XP usando CreatureRegistry e ItemRegistry.
 */
public class CombatSystem {

    private static final int LOG_MAX = 8;

    private final Random       rng = new Random();
    private final List<String> log = new ArrayList<>();

    public int playerAttack(Player player, Creature target) {
        if (target.isDead()) return 0;

        int str = player.getStats().getStrength();
        int dmg = Math.max(1, str/2 + rng.nextInt(5) - 2 - target.getDef().defense);

        target.takeDamage(dmg);
        addLog("You hit " + target.getDef().name + " for " + dmg + " dmg.");

        if (target.isDead()) {
            int xp = target.getDef().xpReward;
            player.getExperience().addXp(xp);
            addLog(target.getDef().name + " slain! +" + xp + " XP");
        }

        return dmg;
    }

    public int creatureAttack(Creature attacker, Player target) {
        CreatureDef d   = attacker.getDef();
        int         dmg = d.damageMin + rng.nextInt(Math.max(1, d.damageMax - d.damageMin + 1));
        dmg = Math.max(1, dmg - target.getStats().getDefense());
        target.getStats().takeDamage(dmg);
        addLog(d.name + " hits you for " + dmg + " dmg.");
        if (target.getStats().isDead()) addLog("You have been slain!");
        return dmg;
    }

    private void addLog(String msg) {
        log.add(0, msg);
        if (log.size() > LOG_MAX) log.remove(log.size() - 1);
    }

    public List<String> getLog() { return log; }
}