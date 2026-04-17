package com.angelo.mmorpg;

import com.angelo.mmorpg.camera.Camera;
import com.angelo.mmorpg.core.Window;
import com.angelo.mmorpg.debug.DebugInfo;
import com.angelo.mmorpg.debug.DebugRenderer;
import com.angelo.mmorpg.debug.DebugState;
import com.angelo.mmorpg.entity.Player;
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

    private static final float  PLAYER_SPEED = 3.0f;

    private Window            window;
    private Camera            camera;
    private InputHandler      input;
    private Renderer          renderer;
    private TerrainRenderer   terrainRenderer;
    private ObjectRenderer    objectRenderer;
    private GridRenderer      gridRenderer;
    private InventoryRenderer inventoryRenderer;
    private DebugState        debugState;
    private DebugInfo         debugInfo;
    private DebugRenderer     debugRenderer;
    private WorldMap          worldMap;
    private Player            player;

    private Vector3f lastClick = null;

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
        debugState        = new DebugState();
        debugInfo         = new DebugInfo();
        camera            = new Camera(WIDTH, HEIGHT);
        inventoryRenderer = new InventoryRenderer(WIDTH, HEIGHT);
        input             = new InputHandler(window.getHandle(), camera, debugState,
                inventoryRenderer, player.getInventory(),
                WIDTH, HEIGHT);
        renderer        = new Renderer();
        terrainRenderer = new TerrainRenderer();
        objectRenderer  = new ObjectRenderer();
        gridRenderer    = new GridRenderer();
        debugRenderer   = new DebugRenderer(WIDTH, HEIGHT);

        input.init();
        renderer.init();
        terrainRenderer.init(worldMap);
        objectRenderer.init();
        gridRenderer.init();
        debugRenderer.init();
        inventoryRenderer.init();

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

        // Interação (clique direito) — coleta objeto próximo
        Vector3f interactTarget = input.consumeInteractTarget();
        if (interactTarget != null) {
            tryCollect(interactTarget.x, interactTarget.z);
        }

        movePlayer(delta);
        camera.update(delta);
        camera.setTarget(pos);
        debugInfo.update(fps, tps, pos, lastClick, camera.getZoom(), camera.getYaw(), worldMap.getSeed());
    }

    /**
     * Tenta coletar um objeto próximo ao ponto clicado e ao player.
     */
    private void tryCollect(float wx, float wz) {
        Vector3f pos = player.getPosition();
        List<StaticObject> objects = worldMap.getObjects();

        StaticObject nearest = null;
        float minDist = Float.MAX_VALUE;

        for (StaticObject obj : objects) {
            if (obj.isCollected()) continue;

            // Distância do clique ao objeto
            float dx = wx - obj.worldX;
            float dz = wz - obj.worldZ;
            float distClick = (float) Math.sqrt(dx * dx + dz * dz);

            if (distClick < 1.0f && distClick < minDist) {
                // Verifica se o player está perto o suficiente
                if (player.canInteractWith(obj.worldX, obj.worldZ)) {
                    nearest = obj;
                    minDist = distClick;
                }
            }
        }

        if (nearest != null) {
            boolean collected = player.collect(nearest.type.drop);
            if (collected) {
                nearest.collect();
                // Remove da grid de colisão no worldMap
                worldMap.clearObject(nearest.tileX, nearest.tileZ);
            }
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

        if (canMoveTo(next))       { player.setPosition(next); return; }

        Vector3f nextX = new Vector3f(next.x, pos.y, pos.z);
        if (canMoveTo(nextX))      { player.setPosition(nextX); return; }

        Vector3f nextZ = new Vector3f(pos.x, pos.y, next.z);
        if (canMoveTo(nextZ))        player.setPosition(nextZ);
    }

    private boolean canMoveTo(Vector3f pos) {
        float r = Player.COLLISION_RADIUS;
        float[][] corners = {{ pos.x-r, pos.z-r }, { pos.x+r, pos.z-r },
                { pos.x-r, pos.z+r }, { pos.x+r, pos.z+r }};
        for (float[] c : corners) if (!worldMap.isWalkable(c[0], c[1])) return false;
        return !worldMap.collidesWithObject(pos.x, pos.z, r);
    }

    private void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        terrainRenderer.render(camera);
        objectRenderer.render(camera, worldMap);
        renderer.render(camera, player.getPosition());

        if (debugState.isGridVisible())
            gridRenderer.render(camera);

        // UI — renderizado por cima
        inventoryRenderer.render(player.getInventory());

        if (debugState.isDebugPanelVisible())
            debugRenderer.render(debugInfo);
    }

    private void cleanup() {
        renderer.cleanup();
        terrainRenderer.cleanup();
        objectRenderer.cleanup();
        gridRenderer.cleanup();
        inventoryRenderer.cleanup();
        debugRenderer.cleanup();
        window.destroy();
    }

    public static void main(String[] args) {
        new Game().run();
    }
}