package com.angelo.mmorpg.entity;

/**
 * Estados possíveis da máquina de estados da IA de uma criatura.
 */
public enum CreatureState {

    /** Parado, aguardando. */
    IDLE,

    /** Caminhando aleatoriamente pelo mapa. */
    PATROL,

    /** Perseguindo o player. */
    CHASE,

    /** Atacando o player (dentro do raio de ataque). */
    ATTACK,

    /** Fugindo do player (comportamento PASSIVE ao ser ameaçado). */
    FLEE,

    /** Morta — aguardando remoção. */
    DEAD
}