package com.angelo.mmorpg.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Inventory {

    public static final int AREA_W    = 200;
    public static final int AREA_H    = 200;
    public static final int ICON_SIZE = 32;

    private final List<Item>  items = new ArrayList<>();
    private final PlayerStats stats;

    public Inventory(PlayerStats stats) { this.stats = stats; }

    public boolean addItem(ItemDef def) {
        if (getCurrentWeight() + def.weight > stats.getMaxWeight()) return false;
        float[] pos = findFreePosition();
        items.add(new Item(def, pos[0], pos[1]));
        return true;
    }

    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) items.remove(index);
    }

    private float[] findFreePosition() {
        int cols = AREA_W / ICON_SIZE, rows = AREA_H / ICON_SIZE;
        for (int r=0;r<rows;r++) for (int c=0;c<cols;c++) {
            float cx=c*ICON_SIZE, cy=r*ICON_SIZE;
            if (!isOccupied(cx, cy)) return new float[]{cx, cy};
        }
        return new float[]{AREA_W/2f, AREA_H/2f};
    }

    private boolean isOccupied(float x, float y) {
        for (Item item : items)
            if (Math.abs(item.invX-x)<ICON_SIZE && Math.abs(item.invY-y)<ICON_SIZE) return true;
        return false;
    }

    public float        getCurrentWeight() { return items.stream().map(Item::getWeight).reduce(0f,Float::sum); }
    public float        getMaxWeight()     { return stats.getMaxWeight(); }
    public List<Item>   getItems()         { return Collections.unmodifiableList(items); }
    public int          getItemCount()     { return items.size(); }
    public boolean      isFull()           { return getCurrentWeight() >= stats.getMaxWeight(); }
}