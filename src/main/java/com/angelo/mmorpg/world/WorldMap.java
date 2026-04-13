package com.angelo.mmorpg.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa o mapa do mundo como uma grade de tiles.
 * Geração procedural via Perlin Noise com FBM (Fractional Brownian Motion).
 *
 * Dois mapas de ruído independentes definem o terreno:
 *   - heightMap: determina o tipo de tile (água, grama, terra, pedra)
 *   - moistureMap: influencia a distribuição de objetos
 */
public class WorldMap {

    public static final int WIDTH  = 64;
    public static final int HEIGHT = 64;

    // Seed padrão — pode ser alterada para gerar mapas diferentes
    public static final long DEFAULT_SEED = 42L;

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

    private final long seed;
    private TileType[][]       tiles;
    private boolean[][]        objectGrid;
    private List<StaticObject> objects;

    public WorldMap() {
        this(DEFAULT_SEED);
    }

    public WorldMap(long seed) {
        this.seed       = seed;
        this.tiles      = new TileType[WIDTH][HEIGHT];
        this.objectGrid = new boolean[WIDTH][HEIGHT];
        this.objects    = new ArrayList<>();
        generate();
    }

    private void generate() {
        PerlinNoise heightNoise   = new PerlinNoise(seed);
        PerlinNoise moistureNoise = new PerlinNoise(seed + 1337);

        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < HEIGHT; z++) {
                // FBM para altura: 4 oitavas, persistência 0.5, escala 0.08
                float height   = heightNoise.fbm(x, z, 4, 0.5f, 0.08f);
                float moisture = moistureNoise.fbm(x, z, 2, 0.5f, 0.05f);

                // Borda forçada para água (ilha natural)
                float edgeDist = edgeFactor(x, z);
                height -= edgeDist;

                // Define tile pela altura
                if (height < -0.1f) {
                    tiles[x][z] = TileType.WATER;
                } else if (height < 0.05f) {
                    tiles[x][z] = TileType.DIRT;
                } else if (height > 0.35f) {
                    tiles[x][z] = TileType.STONE;
                } else {
                    tiles[x][z] = TileType.GRASS;
                }
            }
        }

        // Objetos: distribuídos após definir tiles
        PerlinNoise objectNoise = new PerlinNoise(seed + 9999);
        for (int x = 1; x < WIDTH - 1; x++) {
            for (int z = 1; z < HEIGHT - 1; z++) {
                if (!tiles[x][z].walkable) continue;

                float density = objectNoise.noise(x * 0.3f, z * 0.3f);

                if (tiles[x][z] == TileType.GRASS && density > 0.4f) {
                    placeObject(StaticObject.ObjectType.BUSH, x, z);
                } else if (tiles[x][z] == TileType.STONE && density > 0.3f) {
                    placeObject(StaticObject.ObjectType.STONE, x, z);
                } else if (tiles[x][z] == TileType.DIRT && density > 0.5f) {
                    placeObject(StaticObject.ObjectType.STONE, x, z);
                }
            }
        }
    }

    /**
     * Fator de borda: retorna valor positivo nas bordas do mapa
     * para forçar água ao redor, criando uma ilha natural.
     */
    private float edgeFactor(int x, int z) {
        float nx = (float) x / WIDTH  * 2f - 1f; // -1 a 1
        float nz = (float) z / HEIGHT * 2f - 1f; // -1 a 1
        float d  = Math.max(Math.abs(nx), Math.abs(nz)); // distância à borda
        return Math.max(0f, d - 0.6f) * 2.5f; // começa a puxar para água a partir de 60% do raio
    }

    private void placeObject(StaticObject.ObjectType type, int x, int z) {
        if (objectGrid[x][z]) return; // tile já ocupado
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

    public boolean isWalkable(float worldX, float worldZ) {
        int tileX = (int) Math.floor(worldX);
        int tileZ = (int) Math.floor(worldZ);
        if (!getTile(tileX, tileZ).walkable) return false;
        if (hasObject(tileX, tileZ)) return false;
        return true;
    }

    public boolean collidesWithObject(float worldX, float worldZ, float playerRadius) {
        int tileX = (int) Math.floor(worldX);
        int tileZ = (int) Math.floor(worldZ);

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int nx = tileX + dx;
                int nz = tileZ + dz;
                if (nx < 0 || nx >= WIDTH || nz < 0 || nz >= HEIGHT) continue;
                if (!objectGrid[nx][nz]) continue;

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

    public long              getSeed()    { return seed; }
    public List<StaticObject> getObjects() { return Collections.unmodifiableList(objects); }
    public int               getWidth()   { return WIDTH; }
    public int               getHeight()  { return HEIGHT; }
}