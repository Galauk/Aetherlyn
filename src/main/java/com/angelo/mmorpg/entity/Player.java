package com.angelo.mmorpg.entity;

import org.joml.Vector3f;

public class Player {

    public static final float COLLISION_RADIUS = 0.3f;
    public static final float INTERACT_RADIUS  = 1.8f;

    private Vector3f         position;
    private Vector3f         target;
    private PlayerStats      stats;
    private Inventory        inventory;
    private ExperienceSystem experience;

    public Player(float startX, float startZ) {
        this.position   = new Vector3f(startX, 0f, startZ);
        this.target     = new Vector3f(startX, 0f, startZ);
        this.stats      = new PlayerStats();
        this.inventory  = new Inventory(stats);
        this.experience = new ExperienceSystem(stats);
    }

    public boolean canInteractWith(float worldX, float worldZ) {
        float dx=worldX-position.x, dz=worldZ-position.z;
        return Math.sqrt(dx*dx+dz*dz) <= INTERACT_RADIUS;
    }

    /** Tenta coletar um item pelo id do ItemRegistry. */
    public boolean collect(String itemId) {
        ItemDef def = ItemRegistry.get(itemId);
        boolean ok  = inventory.addItem(def);
        if (ok) experience.addXp(def.xpOnCollect);
        return ok;
    }

    public Vector3f         getPosition()   { return position; }
    public Vector3f         getTarget()     { return target; }
    public PlayerStats      getStats()      { return stats; }
    public Inventory        getInventory()  { return inventory; }
    public ExperienceSystem getExperience() { return experience; }

    public void setPosition(Vector3f pos) { position.set(pos); }
    public void setTarget(Vector3f t)     { target.set(t); }
}