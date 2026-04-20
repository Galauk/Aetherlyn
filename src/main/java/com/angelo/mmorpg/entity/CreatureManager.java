package com.angelo.mmorpg.entity;

import com.angelo.mmorpg.world.WorldMap;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Gerencia o ciclo de vida de todas as criaturas do mundo.
 * Spawna criaturas em posições caminhável e remove as mortas.
 */
public class CreatureManager {

    private final List<Creature> creatures = new ArrayList<>();
    private final Random         rng       = new Random();

    public void spawnAll(WorldMap worldMap) {
        // Spawna criaturas distribuídas pelo mapa caminhável
        spawnType(worldMap, CreatureType.SKELETON, 8);
        spawnType(worldMap, CreatureType.LICH,     2);
        spawnType(worldMap, CreatureType.VILLAGER, 5);
        spawnType(worldMap, CreatureType.DEER,     6);
    }

    private void spawnType(WorldMap worldMap, CreatureType type, int count) {
        int attempts = 0;
        int spawned  = 0;

        while (spawned < count && attempts < count * 20) {
            attempts++;
            int x = 2 + rng.nextInt(WorldMap.WIDTH  - 4);
            int z = 2 + rng.nextInt(WorldMap.HEIGHT - 4);

            if (worldMap.isWalkable(x + 0.5f, z + 0.5f)) {
                creatures.add(new Creature(type, x + 0.5f, z + 0.5f));
                spawned++;
            }
        }
    }

    /**
     * Atualiza todas as criaturas vivas.
     * Remove criaturas mortas após um delay (para animação futura).
     */
    public void update(float delta, Player player, WorldMap worldMap, CombatSystem combat) {
        for (Creature c : creatures) {
            c.update(delta, player, worldMap, combat);
        }

        // Remove mortas
        creatures.removeIf(Creature::isDead);
    }

    /**
     * Retorna a criatura mais próxima do ponto (wx, wz) dentro do raio dado.
     * Usado para clique direito do player.
     */
    public Creature getCreatureAt(float wx, float wz, float radius) {
        Creature nearest  = null;
        float    minDist  = radius;

        for (Creature c : creatures) {
            if (c.isDead()) continue;
            float dx   = c.getPosition().x - wx;
            float dz   = c.getPosition().z - wz;
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            if (dist < minDist) {
                minDist = dist;
                nearest = c;
            }
        }

        return nearest;
    }

    public List<Creature> getCreatures() { return creatures; }
}