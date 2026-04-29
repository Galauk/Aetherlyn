package com.angelo.mmorpg.world;

import com.angelo.mmorpg.entity.ItemRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorldMap {

    public static final int WIDTH  = 64;
    public static final int HEIGHT = 64;
    public static final long DEFAULT_SEED = 42L;

    public enum TileType {
        GRASS (0, 11, true),
        DIRT  (4,  0, true),
        STONE (2, 20, true),
        WATER (18, 0, false);

        public final int     spriteCol;
        public final int     spriteRow;
        public final boolean walkable;

        TileType(int col, int row, boolean walkable) {
            this.spriteCol = col; this.spriteRow = row; this.walkable = walkable;
        }
    }

    private final long seed;
    private TileType[][]       tiles;
    private boolean[][]        objectGrid;
    private List<StaticObject> objects;

    public WorldMap()           { this(DEFAULT_SEED); }
    public WorldMap(long seed)  {
        this.seed       = seed;
        this.tiles      = new TileType[WIDTH][HEIGHT];
        this.objectGrid = new boolean[WIDTH][HEIGHT];
        this.objects    = new ArrayList<>();
        generate();
    }

    private void generate() {
        PerlinNoise heightNoise   = new PerlinNoise(seed);
        PerlinNoise moistureNoise = new PerlinNoise(seed + 1337);

        for (int x=0;x<WIDTH;x++) for (int z=0;z<HEIGHT;z++) {
            float h = heightNoise.fbm(x, z, 4, 0.5f, 0.08f) - edgeFactor(x, z);
            if      (h < -0.1f) tiles[x][z] = TileType.WATER;
            else if (h <  0.05f) tiles[x][z] = TileType.DIRT;
            else if (h >  0.35f) tiles[x][z] = TileType.STONE;
            else                  tiles[x][z] = TileType.GRASS;
        }

        PerlinNoise objNoise = new PerlinNoise(seed + 9999);
        StaticObjectDef stoneDef = StaticObjectRegistry.get("stone");
        StaticObjectDef bushDef  = StaticObjectRegistry.get("bush");

        for (int x=1;x<WIDTH-1;x++) for (int z=1;z<HEIGHT-1;z++) {
            if (!tiles[x][z].walkable) continue;
            float d = objNoise.noise(x*0.3f, z*0.3f);
            if      (tiles[x][z] == TileType.GRASS && d > 0.4f)  placeObject(bushDef,  x, z);
            else if (tiles[x][z] == TileType.STONE && d > 0.3f)  placeObject(stoneDef, x, z);
            else if (tiles[x][z] == TileType.DIRT  && d > 0.5f)  placeObject(stoneDef, x, z);
        }
    }

    private float edgeFactor(int x, int z) {
        float nx=(float)x/WIDTH*2f-1f, nz=(float)z/HEIGHT*2f-1f;
        float d = Math.max(Math.abs(nx), Math.abs(nz));
        return Math.max(0f, d-0.6f)*2.5f;
    }

    private void placeObject(StaticObjectDef def, int x, int z) {
        if (objectGrid[x][z]) return;
        objects.add(new StaticObject(def, x, z));
        objectGrid[x][z] = true;
    }

    public TileType getTile(int x, int z) {
        if (x<0||x>=WIDTH||z<0||z>=HEIGHT) return TileType.WATER;
        return tiles[x][z];
    }

    public boolean hasObject(int x, int z) {
        if (x<0||x>=WIDTH||z<0||z>=HEIGHT) return false;
        return objectGrid[x][z];
    }

    public boolean isWalkable(float wx, float wz) {
        int x=(int)Math.floor(wx), z=(int)Math.floor(wz);
        return getTile(x,z).walkable && !hasObject(x,z);
    }

    public boolean collidesWithObject(float wx, float wz, float r) {
        int tx=(int)Math.floor(wx), tz=(int)Math.floor(wz);
        for (int dx=-2;dx<=2;dx++) for (int dz=-2;dz<=2;dz++) {
            int nx=tx+dx, nz=tz+dz;
            if (nx<0||nx>=WIDTH||nz<0||nz>=HEIGHT||!objectGrid[nx][nz]) continue;
            for (StaticObject obj : objects)
                if (obj.tileX==nx && obj.tileZ==nz && obj.collidesWith(wx,wz,r)) return true;
        }
        return false;
    }

    public void clearObject(int x, int z) {
        if (x>=0&&x<WIDTH&&z>=0&&z<HEIGHT) objectGrid[x][z] = false;
    }

    public long              getSeed()    { return seed; }
    public List<StaticObject> getObjects() { return Collections.unmodifiableList(objects); }
    public int               getWidth()   { return WIDTH; }
    public int               getHeight()  { return HEIGHT; }
}