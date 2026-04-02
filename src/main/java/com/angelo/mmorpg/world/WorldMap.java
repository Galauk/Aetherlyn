package com.angelo.mmorpg.world;

/**
 * Representa o mapa do mundo como uma grade de tiles.
 * Cada célula contém um TileType que define textura e comportamento.
 *
 * TODO Fase 2: substituir generate() por Perlin/Simplex Noise.
 */
public class WorldMap {

    public static final int WIDTH  = 32;
    public static final int HEIGHT = 32;

    // Coordenadas no spritesheet terrain.png (64x64px por tile, grade 10x11)
    public enum TileType {
        GRASS (2, 3),  // grama verde
        DIRT  (0, 0),  // terra marrom
        STONE (0, 9),  // pedra cinza
        WATER (9, 0);  // água azul

        public final int spriteCol;
        public final int spriteRow;

        TileType(int col, int row) {
            this.spriteCol = col;
            this.spriteRow = row;
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
                } else if ((x + z) % 11 == 0) {
                    tiles[x][z] = TileType.STONE;
                } else if ((x * 3 + z * 7) % 13 == 0) {
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

    public int getWidth()  { return WIDTH; }
    public int getHeight() { return HEIGHT; }
}