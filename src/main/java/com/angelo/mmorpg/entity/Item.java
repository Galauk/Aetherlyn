package com.angelo.mmorpg.entity;

/**
 * Instância de um item no inventário.
 * Tem posição livre (px, py) dentro da janela do inventário, estilo Ultima Online.
 */
public class Item {

    public final ItemType type;

    // Posição do item dentro da janela do inventário (pixels)
    public float invX;
    public float invY;

    public Item(ItemType type, float invX, float invY) {
        this.type = type;
        this.invX = invX;
        this.invY = invY;
    }

    public float getWeight() { return type.weight; }
    public String getName()  { return type.name; }
}