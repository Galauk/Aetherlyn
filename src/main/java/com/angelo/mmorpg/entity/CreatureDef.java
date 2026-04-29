package com.angelo.mmorpg.entity;

/**
 * Definição imutável de um tipo de criatura.
 * Criada uma vez no CreatureRegistry e reutilizada por todas as instâncias.
 * Para adicionar uma nova criatura: crie uma instância no CreatureRegistry.
 */
public final class CreatureDef {

    public final String          id;
    public final String          name;
    public final int             maxHp;
    public final int             defense;
    public final int             damageMin;
    public final int             damageMax;
    public final float           speed;
    public final float           visionRadius;
    public final float           attackRadius;
    public final CreatureBehavior behavior;
    public final String          texturePath;
    public final int             xpReward;

    public CreatureDef(String id, String name,
                       int maxHp, int defense, int damageMin, int damageMax,
                       float speed, float visionRadius, float attackRadius,
                       CreatureBehavior behavior, String texturePath, int xpReward) {
        this.id           = id;
        this.name         = name;
        this.maxHp        = maxHp;
        this.defense      = defense;
        this.damageMin    = damageMin;
        this.damageMax    = damageMax;
        this.speed        = speed;
        this.visionRadius = visionRadius;
        this.attackRadius = attackRadius;
        this.behavior     = behavior;
        this.texturePath  = texturePath;
        this.xpReward     = xpReward;
    }
}