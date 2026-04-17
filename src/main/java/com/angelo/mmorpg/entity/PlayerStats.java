package com.angelo.mmorpg.entity;

/**
 * Atributos base do player.
 * A força determina o peso máximo carregável — estilo Ultima Online.
 * Peso máximo = força * 3.5 (cada ponto de força = 3.5 unidades de carga)
 */
public class PlayerStats {

    private int   strength;   // força base
    private float maxWeight;  // peso máximo calculado

    public PlayerStats() {
        this(10); // força inicial
    }

    public PlayerStats(int strength) {
        this.strength  = strength;
        this.maxWeight = calcMaxWeight(strength);
    }

    private float calcMaxWeight(int str) {
        return str * 3.5f;
    }

    public void increaseStrength(int amount) {
        strength  += amount;
        maxWeight  = calcMaxWeight(strength);
    }

    public int   getStrength()  { return strength; }
    public float getMaxWeight() { return maxWeight; }
}