package com.angelo.mmorpg.entity;

/**
 * Define os atributos base de cada tipo de criatura.
 * Genérico — o sprite é configurado separadamente via texturePath.
 */
public enum CreatureType {

    //                       name         hp   def  dmgMin dmgMax  spd   vision  attack  behavior          texturePath
    SKELETON   ("Skeleton",  30,  2,  4,  8,   2.0f, 6.0f,  1.2f, CreatureBehavior.HOSTILE, "/assets/skeleton.png"),
    LICH       ("Lich",      80,  8,  10, 20,  1.5f, 8.0f,  2.0f, CreatureBehavior.HOSTILE, "/assets/lich.png"),
    VILLAGER   ("Villager",  20,  0,  2,  4,   2.5f, 4.0f,  1.5f, CreatureBehavior.NEUTRAL, "/assets/player.png"),  // placeholder
    DEER       ("Deer",      15,  0,  0,  0,   3.5f, 5.0f,  0.0f, CreatureBehavior.PASSIVE, "/assets/player.png");  // placeholder

    public final String          name;
    public final int             maxHp;
    public final int             defense;
    public final int             damageMin;
    public final int             damageMax;
    public final float           speed;
    public final float           visionRadius;  // raio de visão em tiles
    public final float           attackRadius;  // raio de ataque em tiles
    public final CreatureBehavior behavior;
    public final String          texturePath;

    CreatureType(String name, int maxHp, int defense, int damageMin, int damageMax,
                 float speed, float visionRadius, float attackRadius,
                 CreatureBehavior behavior, String texturePath) {
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
    }
}