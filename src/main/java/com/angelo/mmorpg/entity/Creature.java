package com.angelo.mmorpg.entity;

import com.angelo.mmorpg.world.WorldMap;
import org.joml.Vector3f;

import java.util.Random;

/**
 * Entidade de criatura com máquina de estados de IA.
 * Mantém distância mínima do player para não sobrepor visualmente.
 */
public class Creature {

    // Distância mínima que a criatura mantém do player ao atacar
    private static final float MIN_DISTANCE     = 0.9f;

    private static final float PATROL_CHANGE_TIME = 3.0f;
    private static final float IDLE_TIME          = 1.5f;
    private static final float ATTACK_COOLDOWN    = 1.5f;

    private final CreatureType  type;
    private final Vector3f      position;
    private final Vector3f      patrolTarget;

    private CreatureState state      = CreatureState.IDLE;
    private int           hp;
    private boolean       aggressive = false;

    private float idleTimer   = 0;
    private float patrolTimer = 0;
    private float attackTimer = 0;

    private final Random rng = new Random();
    private final long   id;
    private static long  idCounter = 0;

    public Creature(CreatureType type, float x, float z) {
        this.type         = type;
        this.position     = new Vector3f(x, 0f, z);
        this.patrolTarget = new Vector3f(x, 0f, z);
        this.hp           = type.maxHp;
        this.id           = idCounter++;
    }

    public void update(float delta, Player player, WorldMap worldMap, CombatSystem combat) {
        if (state == CreatureState.DEAD) return;

        float distToPlayer = distanceTo(player.getPosition());
        attackTimer = Math.max(0, attackTimer - delta);

        switch (state) {
            case IDLE   -> updateIdle(delta, distToPlayer);
            case PATROL -> updatePatrol(delta, distToPlayer, player, worldMap);
            case CHASE  -> updateChase(delta, distToPlayer, player, worldMap);
            case ATTACK -> updateAttack(delta, distToPlayer, player, combat);
            case FLEE   -> updateFlee(delta, distToPlayer, player, worldMap);
            default     -> {}
        }
    }

    // -------------------------------------------------------------------------
    // Estados
    // -------------------------------------------------------------------------

    private void updateIdle(float delta, float distToPlayer) {
        idleTimer += delta;
        if (idleTimer >= IDLE_TIME) {
            idleTimer = 0;
            state     = CreatureState.PATROL;
            pickPatrolTarget();
        }
        checkAggro(distToPlayer);
    }

    private void updatePatrol(float delta, float distToPlayer, Player player, WorldMap worldMap) {
        patrolTimer += delta;
        if (patrolTimer >= PATROL_CHANGE_TIME) {
            patrolTimer = 0;
            pickPatrolTarget();
        }
        moveToward(patrolTarget, type.speed * 0.5f, delta, worldMap);
        if (distanceTo(patrolTarget) < 0.3f) state = CreatureState.IDLE;
        checkAggro(distToPlayer);
    }

    private void updateChase(float delta, float distToPlayer, Player player, WorldMap worldMap) {
        if (distToPlayer <= type.attackRadius) {
            state = CreatureState.ATTACK;
            return;
        }

        // Só move se estiver além da distância mínima
        if (distToPlayer > MIN_DISTANCE) {
            moveToward(player.getPosition(), type.speed, delta, worldMap);
        }

        if (distToPlayer > type.visionRadius * 1.5f) {
            state      = CreatureState.PATROL;
            aggressive = false;
        }
    }

    private void updateAttack(float delta, float distToPlayer, Player player, CombatSystem combat) {
        // Se player saiu do raio de ataque, volta a perseguir
        if (distToPlayer > type.attackRadius + 0.5f) {
            state = CreatureState.CHASE;
            return;
        }

        // Mantém distância mínima — empurra criatura para trás se muito perto
        if (distToPlayer < MIN_DISTANCE) {
            Vector3f away = new Vector3f(position).sub(player.getPosition());
            if (away.length() > 0.001f) {
                away.normalize().mul(MIN_DISTANCE - distToPlayer);
                position.add(away);
            }
        }

        // Ataca com cooldown
        if (attackTimer <= 0 && type.damageMax > 0) {
            combat.creatureAttack(this, player);
            attackTimer = ATTACK_COOLDOWN;
        }
    }

    private void updateFlee(float delta, float distToPlayer, Player player, WorldMap worldMap) {
        if (distToPlayer > type.visionRadius) {
            state = CreatureState.PATROL;
            return;
        }
        Vector3f away      = new Vector3f(position).sub(player.getPosition());
        if (away.length() > 0.001f) away.normalize();
        Vector3f fleeTarget = new Vector3f(position).add(new Vector3f(away).mul(3f));
        moveToward(fleeTarget, type.speed * 1.2f, delta, worldMap);
    }

    private void checkAggro(float distToPlayer) {
        if (state == CreatureState.DEAD) return;

        boolean shouldAggro = switch (type.behavior) {
            case HOSTILE -> distToPlayer <= type.visionRadius;
            case NEUTRAL -> aggressive && distToPlayer <= type.visionRadius * 1.5f;
            case PASSIVE -> false;
        };

        boolean shouldFlee = type.behavior == CreatureBehavior.PASSIVE
                && distToPlayer <= type.visionRadius * 0.5f;

        if (shouldAggro) state = CreatureState.CHASE;
        if (shouldFlee)  state = CreatureState.FLEE;
    }

    // -------------------------------------------------------------------------
    // Movimento
    // -------------------------------------------------------------------------

    private void moveToward(Vector3f target, float speed, float delta, WorldMap worldMap) {
        Vector3f diff = new Vector3f(target).sub(position);
        if (diff.length() < 0.05f) return;

        Vector3f dir  = new Vector3f(diff).normalize();
        Vector3f next = new Vector3f(position).add(new Vector3f(dir).mul(speed * delta));

        if (worldMap.isWalkable(next.x, next.z))           { position.set(next); return; }
        Vector3f nx = new Vector3f(next.x, 0, position.z);
        if (worldMap.isWalkable(nx.x, nx.z))               { position.set(nx);   return; }
        Vector3f nz = new Vector3f(position.x, 0, next.z);
        if (worldMap.isWalkable(nz.x, nz.z))               { position.set(nz);   return; }
        pickPatrolTarget();
    }

    private void pickPatrolTarget() {
        float range = 4.0f;
        patrolTarget.set(
                position.x + (rng.nextFloat() * range * 2 - range),
                0f,
                position.z + (rng.nextFloat() * range * 2 - range)
        );
    }

    // -------------------------------------------------------------------------
    // Combate
    // -------------------------------------------------------------------------

    public void takeDamage(int amount) {
        hp -= amount;
        if (hp <= 0) {
            hp    = 0;
            state = CreatureState.DEAD;
        } else {
            if (type.behavior == CreatureBehavior.NEUTRAL) aggressive = true;
            state = CreatureState.CHASE;
        }
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public CreatureType  getType()     { return type; }
    public Vector3f      getPosition() { return position; }
    public CreatureState getState()    { return state; }
    public int           getHp()       { return hp; }
    public int           getMaxHp()    { return type.maxHp; }
    public boolean       isDead()      { return state == CreatureState.DEAD; }
    public long          getId()       { return id; }

    public float distanceTo(Vector3f other) {
        float dx = position.x - other.x;
        float dz = position.z - other.z;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }
}