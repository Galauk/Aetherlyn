package com.angelo.mmorpg.world;

/**
 * Definição imutável de um tipo de objeto estático (pedra, arbusto, etc).
 * Registrada no StaticObjectRegistry.
 */
public final class StaticObjectDef {

    public final String id;
    public final int    spriteCol;
    public final int    spriteRow;
    public final float  collisionRadius;
    public final String dropItemId;   // id do item dropado ao coletar (null = não coletável)

    public StaticObjectDef(String id, int spriteCol, int spriteRow,
                           float collisionRadius, String dropItemId) {
        this.id              = id;
        this.spriteCol       = spriteCol;
        this.spriteRow       = spriteRow;
        this.collisionRadius = collisionRadius;
        this.dropItemId      = dropItemId;
    }

    public boolean isCollectable() { return dropItemId != null; }
}