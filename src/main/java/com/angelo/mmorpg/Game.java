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

    // Fixed timestep: 60 ticks por segundo
    private static final double TPS       = 60.0;
    private static final double TICK_TIME = 1.0 / TPS;
    private static final int    MAX_TICKS = 5; // evita spiral of death

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

    // FPS (medido no loop de render)
    private float fps        = 0;
    private float fpsTimer   = 0;
    private int   frameCount = 0;

    // TPS (medido no loop de lógica)
    private float tps        = 0;
    private float tpsTimer   = 0;
    private int   tickCount  = 0;

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
        double previous   = glfwGetTime();
        double accumulator = 0.0;

        while (!window.shouldClose()) {
            double current  = glfwGetTime();
            double elapsed  = current - previous;
            previous        = current;

            // Limita elapsed para evitar "spiral of death" se o jogo travar
            if (elapsed > MAX_TICKS * TICK_TIME) elapsed = MAX_TICKS * TICK_TIME;

            accumulator += elapsed;

            // --- TICKS DE LÓGICA (fixo a 60 TPS) ---
            while (accumulator >= TICK_TIME) {
                tick((float) TICK_TIME);
                accumulator -= TICK_TIME;
                tickCount++;
            }

            // Mede TPS
            tpsTimer += (float) elapsed;
            if (tpsTimer >= 0.5f) {
                tps       = tickCount / tpsTimer;
                tpsTimer  = 0;
                tickCount = 0;
            }

            // Mede FPS
            frameCount++;
            fpsTimer += (float) elapsed;
            if (fpsTimer >= 0.5f) {
                fps        = frameCount / fpsTimer;
                fpsTimer   = 0;
                frameCount = 0;
            }

            // --- RENDER (livre, o mais rápido possível) ---
            render();

            window.swapBuffers();
            window.pollEvents();
        }
    }

    /**
     * Lógica do jogo — chamada a exatamente 60 TPS independente do FPS.
     * Toda movimentação, física e IA deve ficar aqui.
     */
    private void tick(float delta) {
        // Consome input acumulado
        Vector3f clickTarget = input.consumeMoveTarget();
        if (clickTarget != null) {
            playerTarget.set(clickTarget);
            lastClick = new Vector3f(clickTarget);
        }

        movePlayer(delta);
        camera.setTarget(playerPos);
        debugInfo.update(fps, tps, playerPos, lastClick, camera.getZoom());
    }

    /**
     * Render — chamado o mais rápido possível, sem lógica de jogo.
     */
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

        if (distance > 0.05f) {
            float step = PLAYER_SPEED * delta;
            if (step >= distance) {
                playerPos.set(playerTarget);
            } else {
                playerPos.add(new Vector3f(diff).normalize().mul(step));
            }
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