package com.angelo.mmorpg.entity;

import com.angelo.mmorpg.world.WorldMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Gerencia o ciclo de vida de todas as criaturas.
 * Spawna usando o CreatureRegistry — adicionar novos tipos não requer alterar este arquivo.
 */
public class CreatureManager {

    private final List<Creature> creatures = new ArrayList<>();
    private final Random         rng       = new Random();

    // Define quantas de cada tipo spawnam
    private static final Map<String, Integer> SPAWN_COUNT = Map.of(
            "skeleton", 8,
            "lich",     2,
            "villager", 5,
            "deer",     6
    );

    public void spawnAll(WorldMap worldMap) {
        SPAWN_COUNT.forEach((id, count) -> spawnType(worldMap, id, count));
    }

    private void spawnType(WorldMap worldMap, String defId, int count) {
        CreatureDef def      = CreatureRegistry.get(defId);
        int         spawned  = 0;
        int         attempts = 0;

        while (spawned < count && attempts < count * 20) {
            attempts++;
            int x = 2 + rng.nextInt(WorldMap.WIDTH  - 4);
            int z = 2 + rng.nextInt(WorldMap.HEIGHT - 4);
            if (worldMap.isWalkable(x + 0.5f, z + 0.5f)) {
                creatures.add(new Creature(def, x + 0.5f, z + 0.5f));
                spawned++;
            }
        }
    }

    public void update(float delta, Player player, WorldMap worldMap, CombatSystem combat) {
        for (Creature c : creatures) c.update(delta, player, worldMap, combat);
        creatures.removeIf(Creature::isDead);
    }

    public Creature getCreatureAt(float wx, float wz, float radius) {
        Creature nearest = null;
        float    minDist = radius;
        for (Creature c : creatures) {
            if (c.isDead()) continue;
            float dx=c.getPosition().x-wx, dz=c.getPosition().z-wz;
            float d = (float) Math.sqrt(dx*dx+dz*dz);
            if (d < minDist) { minDist = d; nearest = c; }
        }
        return nearest;
    }

    public List<Creature> getCreatures() { return creatures; }
}