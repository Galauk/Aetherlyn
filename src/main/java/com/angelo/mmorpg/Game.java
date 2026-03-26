package com.angelo.mmorpg;

import com.angelo.mmorpg.camera.Camera;
import com.angelo.mmorpg.core.Window;
import com.angelo.mmorpg.debug.DebugInfo;
import com.angelo.mmorpg.debug.DebugRenderer;
import com.angelo.mmorpg.debug.DebugState;
import com.angelo.mmorpg.input.InputHandler;
import com.angelo.mmorpg.rendering.GridRenderer;
import com.angelo.mmorpg.rendering.Renderer;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.glfwGetTime;

public class Game {

    private static final int   WIDTH        = 800;
    private static final int   HEIGHT       = 600;
    private static final float PLAYER_SPEED = 3.0f;

    private Window        window;
    private Camera        camera;
    private InputHandler  input;
    private Renderer      renderer;
    private GridRenderer  gridRenderer;
    private DebugState    debugState;
    private DebugInfo     debugInfo;
    private DebugRenderer debugRenderer;

    private Vector3f playerPos    = new Vector3f(0.0f, 0.0f, 0.0f);
    private Vector3f playerTarget = new Vector3f(0.0f, 0.0f, 0.0f);
    private Vector3f lastClick    = null;

    private float fps        = 0;
    private float fpsTimer   = 0;
    private int   frameCount = 0;

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        window        = new Window(WIDTH, HEIGHT, "Aetherlyn: MMORPG 3D");
        window.init();

        debugState    = new DebugState();
        debugInfo     = new DebugInfo();
        camera        = new Camera(WIDTH, HEIGHT);
        input         = new InputHandler(window.getHandle(), camera, debugState, WIDTH, HEIGHT);
        renderer      = new Renderer();
        gridRenderer  = new GridRenderer();
        debugRenderer = new DebugRenderer(WIDTH, HEIGHT);

        input.init();
        renderer.init();
        gridRenderer.init();
        debugRenderer.init();

        camera.setTarget(playerPos);
    }

    private void loop() {
        double lastTime = glfwGetTime();

        while (!window.shouldClose()) {
            double currentTime = glfwGetTime();
            float  deltaTime   = (float) (currentTime - lastTime);
            lastTime = currentTime;

            // FPS
            fpsTimer += deltaTime;
            frameCount++;
            if (fpsTimer >= 0.5f) {
                fps        = frameCount / fpsTimer;
                fpsTimer   = 0;
                frameCount = 0;
            }

            // Input
            Vector3f clickTarget = input.consumeMoveTarget();
            if (clickTarget != null) {
                playerTarget.set(clickTarget);
                lastClick = new Vector3f(clickTarget);
            }

            // Lógica
            movePlayer(deltaTime);
            camera.setTarget(playerPos);
            debugInfo.update(fps, playerPos, lastClick, camera.getZoom());

            // Render
            renderer.render(camera, playerPos);

            if (debugState.isGridVisible())
                gridRenderer.render(camera);

            if (debugState.isDebugPanelVisible())
                debugRenderer.render(debugInfo);

            window.swapBuffers();
            window.pollEvents();
        }
    }

    private void movePlayer(float deltaTime) {
        Vector3f diff     = new Vector3f(playerTarget).sub(playerPos);
        float    distance = diff.length();

        if (distance > 0.05f) {
            float step = PLAYER_SPEED * deltaTime;
            if (step >= distance) {
                playerPos.set(playerTarget);
            } else {
                playerPos.add(new Vector3f(diff).normalize().mul(step));
            }
        }
    }

    private void cleanup() {
        renderer.cleanup();
        gridRenderer.cleanup();
        debugRenderer.cleanup();
        window.destroy();
    }

    public static void main(String[] args) {
        new Game().run();
    }
}