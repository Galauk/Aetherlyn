package com.angelo.mmorpg.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registro central de definições de itens.
 *
 * Para adicionar um novo item:
 *   1. Adicione um new ItemDef(...) no bloco estático abaixo.
 *   2. Se o item for dropado por objeto estático, registre-o em StaticObjectDef.
 *   3. Nenhum outro arquivo precisa ser alterado.
 *
 * Acesso: ItemRegistry.get("stone")
 */
public final class ItemRegistry {

    private static final Map<String, ItemDef> REGISTRY = new LinkedHashMap<>();

    static {
        register(new ItemDef("stone", "Stone", 2.0f, 20, 19, 3));
        register(new ItemDef("wood",  "Wood",  1.0f, 17, 11, 4));
    }

    private static void register(ItemDef def) {
        REGISTRY.put(def.id, def);
    }

    public static ItemDef get(String id) {
        ItemDef def = REGISTRY.get(id);
        if (def == null) throw new IllegalArgumentException("Unknown item: " + id);
        return def;
    }

    public static Collection<ItemDef> all() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    private ItemRegistry() {}
}