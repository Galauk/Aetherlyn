package com.angelo.mmorpg.entity;

import com.angelo.mmorpg.world.WorldMap;
import org.joml.Vector3f;

import java.util.Random;

/**
 * Instância de uma criatura no mundo.
 * Usa CreatureDef (do CreatureRegistry) para seus atributos base.
 */
public class Creature {

    private static final float MIN_DISTANCE      = 0.9f;
    private static final float PATROL_CHANGE_TIME = 3.0f;
    private static final float IDLE_TIME          = 1.5f;
    private static final float ATTACK_COOLDOWN    = 1.5f;

    private final CreatureDef  def;
    private final Vector3f     position;
    private final Vector3f     patrolTarget;

    private CreatureState state      = CreatureState.IDLE;
    private int           hp;
    private boolean       aggressive = false;

    private float idleTimer   = 0;
    private float patrolTimer = 0;
    private float attackTimer = 0;

    private final Random rng = new Random();
    private final long   id;
    private static long  idCounter = 0;

    public Creature(CreatureDef def, float x, float z) {
        this.def          = def;
        this.position     = new Vector3f(x, 0f, z);
        this.patrolTarget = new Vector3f(x, 0f, z);
        this.hp           = def.maxHp;
        this.id           = idCounter++;
    }

    public void update(float delta, Player player, WorldMap worldMap, CombatSystem combat) {
        if (state == CreatureState.DEAD) return;

        float dist = distanceTo(player.getPosition());
        attackTimer = Math.max(0, attackTimer - delta);

        switch (state) {
            case IDLE   -> updateIdle(delta, dist);
            case PATROL -> updatePatrol(delta, dist, player, worldMap);
            case CHASE  -> updateChase(delta, dist, player, worldMap);
            case ATTACK -> updateAttack(delta, dist, player, combat);
            case FLEE   -> updateFlee(delta, dist, player, worldMap);
            default     -> {}
        }
    }

    private void updateIdle(float delta, float dist) {
        idleTimer += delta;
        if (idleTimer >= IDLE_TIME) { idleTimer = 0; state = CreatureState.PATROL; pickPatrolTarget(); }
        checkAggro(dist);
    }

    private void updatePatrol(float delta, float dist, Player player, WorldMap worldMap) {
        patrolTimer += delta;
        if (patrolTimer >= PATROL_CHANGE_TIME) { patrolTimer = 0; pickPatrolTarget(); }
        moveToward(patrolTarget, def.speed * 0.5f, delta, worldMap);
        if (distanceTo(patrolTarget) < 0.3f) state = CreatureState.IDLE;
        checkAggro(dist);
    }

    private void updateChase(float delta, float dist, Player player, WorldMap worldMap) {
        if (dist <= def.attackRadius) { state = CreatureState.ATTACK; return; }
        if (dist > MIN_DISTANCE) moveToward(player.getPosition(), def.speed, delta, worldMap);
        if (dist > def.visionRadius * 1.5f) { state = CreatureState.PATROL; aggressive = false; }
    }

    private void updateAttack(float delta, float dist, Player player, CombatSystem combat) {
        if (dist > def.attackRadius + 0.5f) { state = CreatureState.CHASE; return; }
        if (dist < MIN_DISTANCE) {
            Vector3f away = new Vector3f(position).sub(player.getPosition());
            if (away.length() > 0.001f) position.add(away.normalize().mul(MIN_DISTANCE - dist));
        }
        if (attackTimer <= 0 && def.damageMax > 0) {
            combat.creatureAttack(this, player);
            attackTimer = ATTACK_COOLDOWN;
        }
    }

    private void updateFlee(float delta, float dist, Player player, WorldMap worldMap) {
        if (dist > def.visionRadius) { state = CreatureState.PATROL; return; }
        Vector3f away = new Vector3f(position).sub(player.getPosition());
        if (away.length() > 0.001f) away.normalize();
        moveToward(new Vector3f(position).add(new Vector3f(away).mul(3f)), def.speed * 1.2f, delta, worldMap);
    }

    private void checkAggro(float dist) {
        if (state == CreatureState.DEAD) return;
        boolean aggro = switch (def.behavior) {
            case HOSTILE -> dist <= def.visionRadius;
            case NEUTRAL -> aggressive && dist <= def.visionRadius * 1.5f;
            case PASSIVE -> false;
        };
        boolean flee = def.behavior == CreatureBehavior.PASSIVE && dist <= def.visionRadius * 0.5f;
        if (aggro) state = CreatureState.CHASE;
        if (flee)  state = CreatureState.FLEE;
    }

    private void moveToward(Vector3f target, float speed, float delta, WorldMap worldMap) {
        Vector3f diff = new Vector3f(target).sub(position);
        if (diff.length() < 0.05f) return;
        Vector3f dir  = new Vector3f(diff).normalize();
        Vector3f next = new Vector3f(position).add(new Vector3f(dir).mul(speed * delta));
        if      (worldMap.isWalkable(next.x, next.z))                         position.set(next);
        else if (worldMap.isWalkable(next.x, position.z))                     position.x = next.x;
        else if (worldMap.isWalkable(position.x, next.z))                     position.z = next.z;
        else    pickPatrolTarget();
    }

    private void pickPatrolTarget() {
        float r = 4.0f;
        patrolTarget.set(
                position.x + (rng.nextFloat() * r * 2 - r), 0f,
                position.z + (rng.nextFloat() * r * 2 - r)
        );
    }

    public void takeDamage(int amount) {
        hp -= amount;
        if (hp <= 0) { hp = 0; state = CreatureState.DEAD; }
        else { if (def.behavior == CreatureBehavior.NEUTRAL) aggressive = true; state = CreatureState.CHASE; }
    }

    public CreatureDef   getDef()      { return def; }
    public Vector3f      getPosition() { return position; }
    public CreatureState getState()    { return state; }
    public int           getHp()       { return hp; }
    public int           getMaxHp()    { return def.maxHp; }
    public boolean       isDead()      { return state == CreatureState.DEAD; }
    public long          getId()       { return id; }

    public float distanceTo(Vector3f other) {
        float dx = position.x - other.x, dz = position.z - other.z;
        return (float) Math.sqrt(dx*dx + dz*dz);
    }
}