package com.angelo.mmorpg.entity;

/**
 * Instância de um item no inventário.
 * Usa ItemDef (do ItemRegistry) para seus atributos.
 */
public class Item {

    public final ItemDef def;
    public float invX;
    public float invY;

    public Item(ItemDef def, float invX, float invY) {
        this.def  = def;
        this.invX = invX;
        this.invY = invY;
    }

    public float  getWeight() { return def.weight; }
    public String getName()   { return def.name; }
}