package com.angelo.mmorpg.world;

/**
 * Representa o mapa do mundo como uma grade de tiles.
 * TODO Fase 2: substituir generate() por Perlin/Simplex Noise.
 */
public class WorldMap {

    public static final int WIDTH  = 32;
    public static final int HEIGHT = 32;

    // Coordenadas confirmadas no spritesheet terrain.png (64x64px, grade 10x11)
    public enum TileType {
        GRASS (1, 3, true),   // grama — verde claro
        DIRT  (0, 0, true),   // terra — marrom
        STONE (0, 9, true),   // pedra — cinza
        WATER (9, 0, false);  // água  — azul, intransponível

        public final int     spriteCol;
        public final int     spriteRow;
        public final boolean walkable;

        TileType(int col, int row, boolean walkable) {
            this.spriteCol = col;
            this.spriteRow = row;
            this.walkable  = walkable;
        }
    }

    private TileType[][] tiles;

    public WorldMap() {
        tiles = new TileType[WIDTH][HEIGHT];
        generate();
    }

    private void generate() {
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < HEIGHT; z++) {
                if (x == 0 || x == WIDTH - 1 || z == 0 || z == HEIGHT - 1) {
                    tiles[x][z] = TileType.WATER;
                } else if ((x * 7 + z * 3) % 17 == 0) {
                    tiles[x][z] = TileType.STONE;
                } else if ((x * 3 + z * 11) % 13 == 0) {
                    tiles[x][z] = TileType.DIRT;
                } else {
                    tiles[x][z] = TileType.GRASS;
                }
            }
        }
    }

    public TileType getTile(int x, int z) {
        if (x < 0 || x >= WIDTH || z < 0 || z >= HEIGHT) return TileType.WATER;
        return tiles[x][z];
    }

    /** Verifica se a posição em coordenadas de mundo é caminhável. */
    public boolean isWalkable(float worldX, float worldZ) {
        int tileX = (int) Math.floor(worldX);
        int tileZ = (int) Math.floor(worldZ);
        return getTile(tileX, tileZ).walkable;
    }

    public int getWidth()  { return WIDTH; }
    public int getHeight() { return HEIGHT; }
}