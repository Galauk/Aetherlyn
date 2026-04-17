package com.angelo.mmorpg.entity;

import org.joml.Vector3f;

/**
 * Entidade do jogador.
 * Centraliza posição, stats e inventário.
 */
public class Player {

    // Raio de colisão e alcance de interação
    public static final float COLLISION_RADIUS  = 0.3f;
    public static final float INTERACT_RADIUS   = 1.8f; // distância máxima para coletar

    private Vector3f    position;
    private Vector3f    target;
    private PlayerStats stats;
    private Inventory   inventory;

    public Player(float startX, float startZ) {
        this.position  = new Vector3f(startX, 0f, startZ);
        this.target    = new Vector3f(startX, 0f, startZ);
        this.stats     = new PlayerStats();
        this.inventory = new Inventory(stats);
    }

    /**
     * Verifica se o player está próximo o suficiente para interagir com um ponto.
     */
    public boolean canInteractWith(float worldX, float worldZ) {
        float dx = worldX - position.x;
        float dz = worldZ - position.z;
        return Math.sqrt(dx * dx + dz * dz) <= INTERACT_RADIUS;
    }

    /**
     * Tenta coletar um item. Retorna true se coletado com sucesso.
     */
    public boolean collect(ItemType type) {
        return inventory.addItem(type);
    }

    public Vector3f    getPosition()  { return position; }
    public Vector3f    getTarget()    { return target; }
    public PlayerStats getStats()     { return stats; }
    public Inventory   getInventory() { return inventory; }

    public void setPosition(Vector3f pos) { position.set(pos); }
    public void setTarget(Vector3f t)     { target.set(t); }
}