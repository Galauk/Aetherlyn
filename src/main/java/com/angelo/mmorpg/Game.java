package com.angelo.mmorpg;

import com.angelo.mmorpg.camera.Camera;
import com.angelo.mmorpg.core.Window;
import com.angelo.mmorpg.debug.DebugInfo;
import com.angelo.mmorpg.debug.DebugRenderer;
import com.angelo.mmorpg.debug.DebugState;
import com.angelo.mmorpg.input.InputHandler;
import com.angelo.mmorpg.rendering.GridRenderer;
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
    private static final float  PLAYER_SIZE  = 0.4f; // raio do player para colisão

    // Fixed timestep: 60 TPS
    private static final double TPS       = 60.0;
    private static final double TICK_TIME = 1.0 / TPS;
    private static final int    MAX_TICKS = 5;

    private Window          window;
    private Camera          camera;
    private InputHandler    input;
    private Renderer        renderer;
    private TerrainRenderer terrainRenderer;
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
        gridRenderer    = new GridRenderer();
        debugRenderer   = new DebugRenderer(WIDTH, HEIGHT);

        input.init();
        renderer.init();
        terrainRenderer.init(worldMap);
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

            // Lógica fixa a 60 TPS
            while (accumulator >= TICK_TIME) {
                tick((float) TICK_TIME);
                accumulator -= TICK_TIME;
                tickCount++;
            }

            // Métricas
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
            // Só aceita destino se for caminhável
            if (worldMap.isWalkable(clickTarget.x, clickTarget.z)) {
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

        float    step      = PLAYER_SPEED * delta;
        Vector3f direction = new Vector3f(diff).normalize();

        // Posição candidata
        Vector3f next = new Vector3f(playerPos).add(new Vector3f(direction).mul(Math.min(step, distance)));

        // Verifica colisão considerando o raio do player
        // Testa os 4 cantos do bounding box para cobrir bordas de tiles
        boolean canMove = worldMap.isWalkable(next.x - PLAYER_SIZE, next.z - PLAYER_SIZE)
                && worldMap.isWalkable(next.x + PLAYER_SIZE, next.z - PLAYER_SIZE)
                && worldMap.isWalkable(next.x - PLAYER_SIZE, next.z + PLAYER_SIZE)
                && worldMap.isWalkable(next.x + PLAYER_SIZE, next.z + PLAYER_SIZE);

        if (canMove) {
            playerPos.set(next);
        } else {
            // Tenta mover só no eixo X
            Vector3f nextX = new Vector3f(playerPos.x + direction.x * Math.min(step, distance),
                    playerPos.y,
                    playerPos.z);
            boolean canX = worldMap.isWalkable(nextX.x - PLAYER_SIZE, nextX.z - PLAYER_SIZE)
                    && worldMap.isWalkable(nextX.x + PLAYER_SIZE, nextX.z - PLAYER_SIZE)
                    && worldMap.isWalkable(nextX.x - PLAYER_SIZE, nextX.z + PLAYER_SIZE)
                    && worldMap.isWalkable(nextX.x + PLAYER_SIZE, nextX.z + PLAYER_SIZE);
            if (canX) { playerPos.set(nextX); return; }

            // Tenta mover só no eixo Z
            Vector3f nextZ = new Vector3f(playerPos.x,
                    playerPos.y,
                    playerPos.z + direction.z * Math.min(step, distance));
            boolean canZ = worldMap.isWalkable(nextZ.x - PLAYER_SIZE, nextZ.z - PLAYER_SIZE)
                    && worldMap.isWalkable(nextZ.x + PLAYER_SIZE, nextZ.z - PLAYER_SIZE)
                    && worldMap.isWalkable(nextZ.x - PLAYER_SIZE, nextZ.z + PLAYER_SIZE)
                    && worldMap.isWalkable(nextZ.x + PLAYER_SIZE, nextZ.z + PLAYER_SIZE);
            if (canZ) playerPos.set(nextZ);
            // Se nenhum eixo funciona, para
        }
    }

    private void cleanup() {
        renderer.cleanup();
        terrainRenderer.cleanup();
        gridRenderer.cleanup();
        debugRenderer.cleanup();
        window.destroy();
    }

    public static void main(String[] args) {
        new Game().run();
    }
}