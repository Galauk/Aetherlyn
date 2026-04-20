package com.angelo.mmorpg;

import com.angelo.mmorpg.camera.Camera;
import com.angelo.mmorpg.core.Window;
import com.angelo.mmorpg.debug.DebugInfo;
import com.angelo.mmorpg.debug.DebugRenderer;
import com.angelo.mmorpg.debug.DebugState;
import com.angelo.mmorpg.entity.*;
import com.angelo.mmorpg.input.InputHandler;
import com.angelo.mmorpg.rendering.*;
import com.angelo.mmorpg.world.StaticObject;
import com.angelo.mmorpg.world.WorldMap;
import org.joml.Vector3f;

import java.util.List;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.opengl.GL11.*;

public class Game {

    private static final int    WIDTH    = 800;
    private static final int    HEIGHT   = 600;

    private static final double TPS       = 60.0;
    private static final double TICK_TIME = 1.0 / TPS;
    private static final int    MAX_TICKS = 5;

    private static final float  PLAYER_SPEED     = 3.0f;
    private static final float  ATTACK_COOLDOWN  = 0.8f;  // segundos entre ataques do player

    private Window            window;
    private Camera            camera;
    private InputHandler      input;
    private Renderer          renderer;
    private TerrainRenderer   terrainRenderer;
    private ObjectRenderer    objectRenderer;
    private CreatureRenderer  creatureRenderer;
    private GridRenderer      gridRenderer;
    private InventoryRenderer inventoryRenderer;
    private DebugState        debugState;
    private DebugInfo         debugInfo;
    private DebugRenderer     debugRenderer;
    private WorldMap          worldMap;
    private Player            player;
    private CreatureManager   creatureManager;
    private CombatSystem      combatSystem;

    private Vector3f lastClick        = null;
    private float    playerAttackTimer = 0;

    private float fps = 0, fpsTimer = 0;
    private int   frameCount = 0;
    private float tps = 0, tpsTimer = 0;
    private int   tickCount = 0;

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        window = new Window(WIDTH, HEIGHT, "Aetherlyn: MMORPG 3D");
        window.init();

        worldMap          = new WorldMap();
        player            = new Player(WorldMap.WIDTH / 2f, WorldMap.HEIGHT / 2f);
        combatSystem      = new CombatSystem();
        creatureManager   = new CreatureManager();
        debugState        = new DebugState();
        debugInfo         = new DebugInfo();
        camera            = new Camera(WIDTH, HEIGHT);
        inventoryRenderer = new InventoryRenderer(WIDTH, HEIGHT);
        input             = new InputHandler(window.getHandle(), camera, debugState,
                inventoryRenderer, player.getInventory(),
                WIDTH, HEIGHT);
        renderer         = new Renderer();
        terrainRenderer  = new TerrainRenderer();
        objectRenderer   = new ObjectRenderer();
        creatureRenderer = new CreatureRenderer();
        gridRenderer     = new GridRenderer();
        debugRenderer    = new DebugRenderer(WIDTH, HEIGHT);

        input.init();
        renderer.init();
        terrainRenderer.init(worldMap);
        objectRenderer.init();
        creatureRenderer.init();
        gridRenderer.init();
        debugRenderer.init();
        inventoryRenderer.init();

        creatureManager.spawnAll(worldMap);
        camera.setTarget(player.getPosition());
    }

    private void loop() {
        double previous    = glfwGetTime();
        double accumulator = 0.0;

        while (!window.shouldClose()) {
            double current = glfwGetTime();
            double elapsed = Math.min(current - previous, MAX_TICKS * TICK_TIME);
            previous       = current;
            accumulator   += elapsed;

            while (accumulator >= TICK_TIME) {
                tick((float) TICK_TIME);
                accumulator -= TICK_TIME;
                tickCount++;
            }

            tpsTimer += (float) elapsed;
            if (tpsTimer >= 0.5f) { tps = tickCount / tpsTimer; tpsTimer = 0; tickCount = 0; }
            frameCount++;
            fpsTimer += (float) elapsed;
            if (fpsTimer >= 0.5f) { fps = frameCount / fpsTimer; fpsTimer = 0; frameCount = 0; }

            render();
            window.swapBuffers();
            window.pollEvents();
        }
    }

    private void tick(float delta) {
        if (player.getStats().isDead()) return;

        playerAttackTimer = Math.max(0, playerAttackTimer - delta);

        Vector3f pos = player.getPosition();

        // Movimento
        Vector3f clickTarget = input.consumeMoveTarget();
        if (clickTarget != null) {
            if (worldMap.isWalkable(clickTarget.x, clickTarget.z)
                    && !worldMap.collidesWithObject(clickTarget.x, clickTarget.z, Player.COLLISION_RADIUS)) {
                player.setTarget(clickTarget);
                lastClick = new Vector3f(clickTarget);
            }
        }

        // Interação: coleta objeto OU ataca criatura
        Vector3f interactTarget = input.consumeInteractTarget();
        if (interactTarget != null) {
            // Tenta atacar criatura primeiro
            Creature target = creatureManager.getCreatureAt(
                    interactTarget.x, interactTarget.z, 1.5f);

            if (target != null && player.canInteractWith(target.getPosition().x, target.getPosition().z)
                    && playerAttackTimer <= 0) {
                combatSystem.playerAttack(player, target);
                playerAttackTimer = ATTACK_COOLDOWN;
            } else {
                // Nenhuma criatura — tenta coletar objeto
                tryCollect(interactTarget.x, interactTarget.z);
            }
        }

        // Atualiza criaturas
        creatureManager.update(delta, player, worldMap, combatSystem);

        movePlayer(delta);
        camera.update(delta);
        camera.setTarget(pos);
        debugInfo.update(fps, tps, pos, lastClick, camera.getZoom(), camera.getYaw(), worldMap.getSeed());
    }

    private void tryCollect(float wx, float wz) {
        List<StaticObject> objects = worldMap.getObjects();

        StaticObject nearest = null;
        float minDist = Float.MAX_VALUE;

        for (StaticObject obj : objects) {
            if (obj.isCollected()) continue;
            float dx = wx - obj.worldX;
            float dz = wz - obj.worldZ;
            float d  = (float) Math.sqrt(dx * dx + dz * dz);
            if (d < 1.0f && d < minDist && player.canInteractWith(obj.worldX, obj.worldZ)) {
                nearest = obj;
                minDist = d;
            }
        }

        if (nearest != null && player.collect(nearest.type.drop)) {
            nearest.collect();
            worldMap.clearObject(nearest.tileX, nearest.tileZ);
        }
    }

    private void movePlayer(float delta) {
        Vector3f pos    = player.getPosition();
        Vector3f target = player.getTarget();
        Vector3f diff   = new Vector3f(target).sub(pos);
        float    dist   = diff.length();

        if (dist < 0.05f) return;

        float    step = PLAYER_SPEED * delta;
        Vector3f dir  = new Vector3f(diff).normalize();
        Vector3f next = new Vector3f(pos).add(new Vector3f(dir).mul(Math.min(step, dist)));

        if (canMoveTo(next))            { player.setPosition(next); return; }
        Vector3f nx = new Vector3f(next.x, pos.y, pos.z);
        if (canMoveTo(nx))              { player.setPosition(nx);   return; }
        Vector3f nz = new Vector3f(pos.x, pos.y, next.z);
        if (canMoveTo(nz))                player.setPosition(nz);
    }

    private boolean canMoveTo(Vector3f pos) {
        float r = Player.COLLISION_RADIUS;
        if (!worldMap.isWalkable(pos.x - r, pos.z - r)) return false;
        if (!worldMap.isWalkable(pos.x + r, pos.z - r)) return false;
        if (!worldMap.isWalkable(pos.x - r, pos.z + r)) return false;
        if (!worldMap.isWalkable(pos.x + r, pos.z + r)) return false;
        return !worldMap.collidesWithObject(pos.x, pos.z, r);
    }

    private void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        terrainRenderer.render(camera);
        objectRenderer.render(camera, worldMap);
        creatureRenderer.render(camera, creatureManager);
        renderer.render(camera, player.getPosition());

        if (debugState.isGridVisible())
            gridRenderer.render(camera);

        inventoryRenderer.render(player.getInventory());

        if (debugState.isDebugPanelVisible())
            debugRenderer.render(debugInfo);
    }

    private void cleanup() {
        renderer.cleanup();
        terrainRenderer.cleanup();
        objectRenderer.cleanup();
        creatureRenderer.cleanup();
        gridRenderer.cleanup();
        inventoryRenderer.cleanup();
        debugRenderer.cleanup();
        window.destroy();
    }

    public static void main(String[] args) {
        new Game().run();
    }
}