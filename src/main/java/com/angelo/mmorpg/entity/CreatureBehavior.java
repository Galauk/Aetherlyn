package com.angelo.mmorpg.entity;

/**
 * Define o comportamento base de uma criatura.
 * Determina como a IA reage ao player e ao ambiente.
 */
public enum CreatureBehavior {

    /** Nunca ataca. Foge se o player se aproximar muito. */
    PASSIVE,

    /** Ignora o player até ser atacado. Então persegue e ataca. */
    NEUTRAL,

    /** Ataca o player assim que entra no raio de visão. */
    HOSTILE
}