package com.angelo.mmorpg.world;

import com.angelo.mmorpg.entity.ItemType;

/**
 * Objeto estático no mundo (pedra, arbusto).
 * Pode ser coletado com clique direito, dropando um item.
 */
public class StaticObject {

    public enum ObjectType {
        STONE (20, 19, 0.4f, ItemType.STONE),
        BUSH  (17, 11, 0.4f, ItemType.WOOD);

        public final int      spriteCol;
        public final int      spriteRow;
        public final float    collisionRadius;
        public final ItemType drop; // item que dropa ao ser coletado

        ObjectType(int col, int row, float radius, ItemType drop) {
            this.spriteCol       = col;
            this.spriteRow       = row;
            this.collisionRadius = radius;
            this.drop            = drop;
        }
    }

    public final ObjectType type;
    public final int        tileX;
    public final int        tileZ;
    public final float      worldX;
    public final float      worldZ;

    private boolean collected = false;

    public StaticObject(ObjectType type, int tileX, int tileZ) {
        this.type   = type;
        this.tileX  = tileX;
        this.tileZ  = tileZ;
        this.worldX = tileX + 0.5f;
        this.worldZ = tileZ + 0.5f;
    }

    public boolean collidesWith(float x, float z, float playerRadius) {
        if (collected) return false;
        float dx = x - worldX;
        float dz = z - worldZ;
        return Math.sqrt(dx * dx + dz * dz) < (type.collisionRadius + playerRadius);
    }

    public void collect()       { collected = true; }
    public boolean isCollected(){ return collected; }
}