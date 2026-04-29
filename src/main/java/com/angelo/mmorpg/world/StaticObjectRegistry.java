package com.angelo.mmorpg.world;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registro central de definições de objetos estáticos.
 *
 * Para adicionar um novo objeto:
 *   1. Adicione um new StaticObjectDef(...) no bloco estático abaixo.
 *   2. Configure sua geração no WorldMap.generate() se necessário.
 */
public final class StaticObjectRegistry {

    private static final Map<String, StaticObjectDef> REGISTRY = new LinkedHashMap<>();

    static {
        register(new StaticObjectDef("stone", 20, 19, 0.4f, "stone"));
        register(new StaticObjectDef("bush",  17, 11, 0.4f, "wood"));
    }

    private static void register(StaticObjectDef def) {
        REGISTRY.put(def.id, def);
    }

    public static StaticObjectDef get(String id) {
        StaticObjectDef def = REGISTRY.get(id);
        if (def == null) throw new IllegalArgumentException("Unknown static object: " + id);
        return def;
    }

    public static Collection<StaticObjectDef> all() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    private StaticObjectRegistry() {}
}