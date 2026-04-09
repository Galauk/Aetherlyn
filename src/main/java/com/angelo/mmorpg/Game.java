package com.angelo.mmorpg;

import com.angelo.mmorpg.camera.Camera;
import com.angelo.mmorpg.core.Window;
import com.angelo.mmorpg.debug.DebugInfo;
import com.angelo.mmorpg.debug.DebugRenderer;
import com.angelo.mmorpg.debug.DebugState;
import com.angelo.mmorpg.input.InputHandler;
import com.angelo.mmorpg.rendering.GridRenderer;
import com.angelo.mmorpg.rendering.ObjectRenderer;
import com.angelo.mmorpg.rendering.Renderer;
import com.angelo.mmorpg.rendering.TerrainRenderer;
import com.angelo.mmorpg.world.WorldMap;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.opengl.GL11.*;

public class Game {

    private static final int    WIDTH        = 800;
    private static final int    HEIGHT       = 600;
    private static final float  PLAYER_SPEED = 3.0f;
    private static final float  PLAYER_SIZE  = 0.3f;

    private static final double TPS       = 60.0;
    private static final double TICK_TIME = 1.0 / TPS;
    private static final int    MAX_TICKS = 5;

    private Window          window;
    private Camera          camera;
    private InputHandler    input;
    private Renderer        renderer;
    private TerrainRenderer terrainRenderer;
    private ObjectRenderer  objectRenderer;
    private GridRenderer    gridRenderer;
    private DebugState      debugState;
    private DebugInfo       debugInfo;
    private DebugRenderer   debugRenderer;
    private WorldMap        worldMap;

    private Vector3f playerPos    = new Vector3f(16.0f, 0.0f, 16.0f);
    private Vector3f playerTarget = new Vector3f(16.0f, 0.0f, 16.0f);
    private Vector3f lastClick    = null;

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

        worldMap        = new WorldMap();
        debugState      = new DebugState();
        debugInfo       = new DebugInfo();
        camera          = new Camera(WIDTH, HEIGHT);
        input           = new InputHandler(window.getHandle(), camera, debugState, WIDTH, HEIGHT);
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

        camera.setTarget(playerPos);
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
        Vector3f clickTarget = input.consumeMoveTarget();
        if (clickTarget != null) {
            if (worldMap.isWalkable(clickTarget.x, clickTarget.z)
                    && !worldMap.collidesWithObject(clickTarget.x, clickTarget.z, PLAYER_SIZE)) {
                playerTarget.set(clickTarget);
                lastClick = new Vector3f(clickTarget);
            }
        }

        movePlayer(delta);
        camera.setTarget(playerPos);
        debugInfo.update(fps, tps, playerPos, lastClick, camera.getZoom());
    }

    private void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        terrainRenderer.render(camera);
        objectRenderer.render(camera, worldMap);
        renderer.render(camera, playerPos);

        if (debugState.isGridVisible())
            gridRenderer.render(camera);
        if (debugState.isDebugPanelVisible())
            debugRenderer.render(debugInfo);
    }

    private void movePlayer(float delta) {
        Vector3f diff     = new Vector3f(playerTarget).sub(playerPos);
        float    distance = diff.length();
        if (distance < 0.05f) return;

        float    step = PLAYER_SPEED * delta;
        Vector3f dir  = new Vector3f(diff).normalize();
        Vector3f next = new Vector3f(playerPos).add(new Vector3f(dir).mul(Math.min(step, distance)));

        if (canMoveTo(next)) {
            playerPos.set(next);
            return;
        }

        // Tenta deslizar no eixo X
        Vector3f nextX = new Vector3f(next.x, playerPos.y, playerPos.z);
        if (canMoveTo(nextX)) {
            playerPos.set(nextX);
            return;
        }

        // Tenta deslizar no eixo Z
        Vector3f nextZ = new Vector3f(playerPos.x, playerPos.y, next.z);
        if (canMoveTo(nextZ)) {
            playerPos.set(nextZ);
        }
    }

    /**
     * Verifica se o player pode mover para a posição dada.
     * Considera borda do mapa, água e objetos estáticos.
     */
    private boolean canMoveTo(Vector3f pos) {
        // Verifica os 4 cantos do bounding box
        float[] xs = { pos.x - PLAYER_SIZE, pos.x + PLAYER_SIZE };
        float[] zs = { pos.z - PLAYER_SIZE, pos.z + PLAYER_SIZE };

        for (float x : xs) {
            for (float z : zs) {
                if (!worldMap.isWalkable(x, z)) return false;
            }
        }

        // Colisão circular com objetos
        if (worldMap.collidesWithObject(pos.x, pos.z, PLAYER_SIZE)) return false;

        return true;
    }

    private void cleanup() {
        renderer.cleanup();
        terrainRenderer.cleanup();
        objectRenderer.cleanup();
        gridRenderer.cleanup();
        debugRenderer.cleanup();
        window.destroy();
    }

    public static void main(String[] args) {
        new Game().run();
    }
}