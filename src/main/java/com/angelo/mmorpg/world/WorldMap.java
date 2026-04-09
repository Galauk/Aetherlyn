package com.angelo.mmorpg.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa o mapa do mundo como uma grade de tiles.
 * Também gerencia os objetos estáticos (pedras, arbustos).
 *
 * TODO Fase 2: substituir generate() por Perlin/Simplex Noise.
 */
public class WorldMap {

    public static final int WIDTH  = 32;
    public static final int HEIGHT = 32;

    // Coordenadas confirmadas no spritesheet (32x32px, grade 21x23)
    public enum TileType {
        GRASS (0, 11, true),
        DIRT  (4,  0, true),
        STONE (2, 20, true),
        WATER (18, 0, false);

        public final int     spriteCol;
        public final int     spriteRow;
        public final boolean walkable;

        TileType(int col, int row, boolean walkable) {
            this.spriteCol = col;
            this.spriteRow = row;
            this.walkable  = walkable;
        }
    }

    private TileType[][]       tiles;
    private List<StaticObject> objects;

    // Grid auxiliar para colisão rápida com objetos (true = tile ocupado por objeto)
    private boolean[][] objectGrid;

    public WorldMap() {
        tiles      = new TileType[WIDTH][HEIGHT];
        objectGrid = new boolean[WIDTH][HEIGHT];
        objects    = new ArrayList<>();
        generate();
    }

    private void generate() {
        // --- Tiles ---
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

        // --- Objetos estáticos ---
        // Pedras: distribuídas em tiles de pedra e grama
        for (int x = 1; x < WIDTH - 1; x++) {
            for (int z = 1; z < HEIGHT - 1; z++) {
                if ((x * 5 + z * 9) % 23 == 0 && tiles[x][z].walkable) {
                    placeObject(StaticObject.ObjectType.STONE, x, z);
                } else if ((x * 11 + z * 7) % 29 == 0 && tiles[x][z] == TileType.GRASS) {
                    placeObject(StaticObject.ObjectType.BUSH, x, z);
                }
            }
        }
    }

    private void placeObject(StaticObject.ObjectType type, int x, int z) {
        objects.add(new StaticObject(type, x, z));
        objectGrid[x][z] = true;
    }

    public TileType getTile(int x, int z) {
        if (x < 0 || x >= WIDTH || z < 0 || z >= HEIGHT) return TileType.WATER;
        return tiles[x][z];
    }

    public boolean hasObject(int x, int z) {
        if (x < 0 || x >= WIDTH || z < 0 || z >= HEIGHT) return false;
        return objectGrid[x][z];
    }

    /**
     * Verifica se a posição em coordenadas de mundo é caminhável
     * (tile caminhável E sem objeto estático no tile).
     */
    public boolean isWalkable(float worldX, float worldZ) {
        int tileX = (int) Math.floor(worldX);
        int tileZ = (int) Math.floor(worldZ);
        if (!getTile(tileX, tileZ).walkable) return false;
        if (hasObject(tileX, tileZ)) return false;
        return true;
    }

    /**
     * Verifica colisão circular com objetos próximos.
     * Mais preciso que isWalkable para o movimento do player.
     */
    public boolean collidesWithObject(float worldX, float worldZ, float playerRadius) {
        int tileX = (int) Math.floor(worldX);
        int tileZ = (int) Math.floor(worldZ);

        // Verifica objetos nos tiles vizinhos (raio de busca = 2)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int nx = tileX + dx;
                int nz = tileZ + dz;
                if (nx < 0 || nx >= WIDTH || nz < 0 || nz >= HEIGHT) continue;
                if (!objectGrid[nx][nz]) continue;

                // Encontra o objeto nesse tile
                for (StaticObject obj : objects) {
                    if (obj.tileX == nx && obj.tileZ == nz) {
                        if (obj.collidesWith(worldX, worldZ, playerRadius)) return true;
                        break;
                    }
                }
            }
        }
        return false;
    }

    public List<StaticObject> getObjects() { return Collections.unmodifiableList(objects); }
    public int getWidth()                  { return WIDTH; }
    public int getHeight()                 { return HEIGHT; }
}