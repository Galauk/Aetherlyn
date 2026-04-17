package com.angelo.mmorpg.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Inventário estilo Ultima Online.
 * Itens são posicionados livremente dentro de uma área.
 * O peso total é limitado pela força do player.
 */
public class Inventory {

    // Área interna da janela do inventário (pixels disponíveis para posicionar itens)
    public static final int AREA_W = 200;
    public static final int AREA_H = 200;

    // Tamanho do ícone de cada item na janela
    public static final int ICON_SIZE = 32;

    private final List<Item> items = new ArrayList<>();
    private final PlayerStats stats;

    public Inventory(PlayerStats stats) {
        this.stats = stats;
    }

    /**
     * Tenta adicionar um item ao inventário.
     * @return true se adicionado, false se exceder o peso máximo
     */
    public boolean addItem(ItemType type) {
        if (getCurrentWeight() + type.weight > stats.getMaxWeight()) return false;

        // Encontra posição livre automaticamente
        float[] pos = findFreePosition();
        items.add(new Item(type, pos[0], pos[1]));
        return true;
    }

    /**
     * Remove um item pelo índice.
     */
    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) items.remove(index);
    }

    /**
     * Encontra uma posição livre na grade do inventário (evita sobreposição).
     */
    private float[] findFreePosition() {
        int cols = AREA_W / ICON_SIZE;
        int rows = AREA_H / ICON_SIZE;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                float cx = col * ICON_SIZE;
                float cy = row * ICON_SIZE;
                if (!isOccupied(cx, cy)) return new float[]{ cx, cy };
            }
        }

        // Fallback: empilha no centro se tudo ocupado
        return new float[]{ AREA_W / 2f, AREA_H / 2f };
    }

    private boolean isOccupied(float x, float y) {
        for (Item item : items) {
            if (Math.abs(item.invX - x) < ICON_SIZE && Math.abs(item.invY - y) < ICON_SIZE) {
                return true;
            }
        }
        return false;
    }

    public float getCurrentWeight() {
        return items.stream().map(Item::getWeight).reduce(0f, Float::sum);
    }

    public float getMaxWeight()         { return stats.getMaxWeight(); }
    public List<Item> getItems()        { return Collections.unmodifiableList(items); }
    public int getItemCount()           { return items.size(); }
    public boolean isFull()             { return getCurrentWeight() >= stats.getMaxWeight(); }
}