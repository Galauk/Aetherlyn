package com.angelo.mmorpg.world;

/**
 * Representa o mapa do mundo.
 * TODO Fase 2: implementar grid de tiles, geração procedural com Perlin/Simplex Noise.
 */
public class WorldMap {

    public static final int WIDTH  = 32;
    public static final int HEIGHT = 32;

    private int[][] tiles;

    public WorldMap() {
        tiles = new int[WIDTH][HEIGHT];
        generate();
    }

    private void generate() {
        // Por enquanto preenche tudo com tile 0 (grama)
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < HEIGHT; z++) {
                tiles[x][z] = 0;
            }
        }
    }

    public int getTile(int x, int z) {
        if (x < 0 || x >= WIDTH || z < 0 || z >= HEIGHT) return -1;
        return tiles[x][z];
    }

    public int getWidth()  { return WIDTH; }
    public int getHeight() { return HEIGHT; }
}