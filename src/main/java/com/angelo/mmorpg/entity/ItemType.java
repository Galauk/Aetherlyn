package com.angelo.mmorpg.entity;

/**
 * Define os tipos de item existentes no jogo.
 * Cada tipo tem peso e coordenadas do ícone no spritesheet terrain.png (32px).
 */
public enum ItemType {

    STONE ("Stone", 2.0f, 20, 19),  // pedra — sprite [20,19]
    WOOD  ("Wood",  1.0f, 17, 11);  // madeira — reutiliza sprite do arbusto por enquanto

    public final String name;
    public final float  weight;     // peso em unidades
    public final int    spriteCol;  // coluna no spritesheet terrain.png (32px)
    public final int    spriteRow;  // linha no spritesheet terrain.png (32px)

    ItemType(String name, float weight, int spriteCol, int spriteRow) {
        this.name      = name;
        this.weight    = weight;
        this.spriteCol = spriteCol;
        this.spriteRow = spriteRow;
    }
}