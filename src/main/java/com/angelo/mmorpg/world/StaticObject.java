package com.angelo.mmorpg.world;

/**
 * Instância de um objeto estático no mundo.
 * Usa StaticObjectDef (do StaticObjectRegistry) para seus atributos.
 */
public class StaticObject {

    public final StaticObjectDef def;
    public final int             tileX;
    public final int             tileZ;
    public final float           worldX;
    public final float           worldZ;

    private boolean collected = false;

    public StaticObject(StaticObjectDef def, int tileX, int tileZ) {
        this.def    = def;
        this.tileX  = tileX;
        this.tileZ  = tileZ;
        this.worldX = tileX + 0.5f;
        this.worldZ = tileZ + 0.5f;
    }

    public boolean collidesWith(float x, float z, float playerRadius) {
        if (collected) return false;
        float dx = x - worldX, dz = z - worldZ;
        return Math.sqrt(dx*dx + dz*dz) < (def.collisionRadius + playerRadius);
    }

    public void    collect()         { collected = true; }
    public boolean isCollected()     { return collected; }
}