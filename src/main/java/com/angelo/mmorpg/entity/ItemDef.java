package com.angelo.mmorpg.entity;

/**
 * Definição imutável de um tipo de item.
 * Criada uma vez no ItemRegistry e reutilizada por todas as instâncias.
 */
public final class ItemDef {

    public final String id;
    public final String name;
    public final float  weight;
    public final int    spriteCol; // coluna no spritesheet terrain.png (32px)
    public final int    spriteRow; // linha no spritesheet terrain.png (32px)
    public final int    xpOnCollect;

    public ItemDef(String id, String name, float weight,
                   int spriteCol, int spriteRow, int xpOnCollect) {
        this.id          = id;
        this.name        = name;
        this.weight      = weight;
        this.spriteCol   = spriteCol;
        this.spriteRow   = spriteRow;
        this.xpOnCollect = xpOnCollect;
    }
}