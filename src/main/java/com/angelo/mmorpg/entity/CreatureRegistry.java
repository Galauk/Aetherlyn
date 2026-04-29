package com.angelo.mmorpg.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registro central de definições de criaturas.
 *
 * Para adicionar uma nova criatura:
 *   1. Adicione um new CreatureDef(...) no bloco estático abaixo.
 *   2. Nenhum outro arquivo precisa ser alterado.
 *
 * Acesso: CreatureRegistry.get("skeleton")
 */
public final class CreatureRegistry {

    private static final Map<String, CreatureDef> REGISTRY = new LinkedHashMap<>();

    static {
        register(new CreatureDef(
                "skeleton", "Skeleton",
                30, 2, 4, 8, 2.0f, 6.0f, 1.2f,
                CreatureBehavior.HOSTILE,
                "/assets/skeleton.png", 25
        ));

        register(new CreatureDef(
                "lich", "Lich",
                80, 8, 10, 20, 1.5f, 8.0f, 2.0f,
                CreatureBehavior.HOSTILE,
                "/assets/lich.png", 120
        ));

        register(new CreatureDef(
                "villager", "Villager",
                20, 0, 2, 4, 2.5f, 4.0f, 1.5f,
                CreatureBehavior.NEUTRAL,
                "/assets/player.png", 15   // placeholder texture
        ));

        register(new CreatureDef(
                "deer", "Deer",
                15, 0, 0, 0, 3.5f, 5.0f, 0.0f,
                CreatureBehavior.PASSIVE,
                "/assets/player.png", 8    // placeholder texture
        ));
    }

    private static void register(CreatureDef def) {
        REGISTRY.put(def.id, def);
    }

    /** Retorna a definição pelo id. Lança exceção se não encontrada. */
    public static CreatureDef get(String id) {
        CreatureDef def = REGISTRY.get(id);
        if (def == null) throw new IllegalArgumentException("Unknown creature: " + id);
        return def;
    }

    /** Todos os ids registrados. */
    public static Collection<String> ids() {
        return Collections.unmodifiableCollection(REGISTRY.keySet());
    }

    /** Todas as definições registradas. */
    public static Collection<CreatureDef> all() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    private CreatureRegistry() {}
}