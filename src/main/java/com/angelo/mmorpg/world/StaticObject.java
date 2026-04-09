package com.angelo.mmorpg.world;

/**
 * Representa um objeto estático no mundo (pedra, arbusto, etc).
 * Tem posição no grid e bloqueia passagem do player.
 */
public class StaticObject {

    public enum ObjectType {
        STONE  (20, 19, 0.4f),  // pedra  — [20,19] no spritesheet 32px
        BUSH   (17, 11, 0.4f);  // arbusto — [17,11] no spritesheet 32px

        public final int   spriteCol;
        public final int   spriteRow;
        public final float collisionRadius; // raio de colisão em unidades de mundo

        ObjectType(int col, int row, float radius) {
            this.spriteCol       = col;
            this.spriteRow       = row;
            this.collisionRadius = radius;
        }
    }

    public final ObjectType type;
    public final int        tileX;
    public final int        tileZ;

    // Centro do objeto em coordenadas de mundo (meio do tile)
    public final float worldX;
    public final float worldZ;

    public StaticObject(ObjectType type, int tileX, int tileZ) {
        this.type   = type;
        this.tileX  = tileX;
        this.tileZ  = tileZ;
        this.worldX = tileX + 0.5f;
        this.worldZ = tileZ + 0.5f;
    }

    /** Verifica se o ponto (x, z) colide com este objeto. */
    public boolean collidesWith(float x, float z, float playerRadius) {
        float dx   = x - worldX;
        float dz   = z - worldZ;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        return dist < (type.collisionRadius + playerRadius);
    }
}