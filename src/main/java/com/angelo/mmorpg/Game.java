package com.angelo.mmorpg;

import static org.lwjgl.glfw.GLFW.glfwGetTime;

public class Game {

    private static final int   WIDTH  = 800;
    private static final int   HEIGHT = 600;
    private static final float FOV    = 45.0f;

    private Window       window;
    private Camera       camera;
    private InputHandler input;
    private Renderer     renderer;

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        window   = new Window(WIDTH, HEIGHT, "Aetherlyn: MMORPG 3D");
        window.init();

        camera   = new Camera(FOV, WIDTH, HEIGHT);
        input    = new InputHandler(window.getHandle(), camera, WIDTH, HEIGHT);
        renderer = new Renderer();

        input.init();
        renderer.init();
    }

    private void loop() {
        double lastTime = glfwGetTime();

        while (!window.shouldClose()) {
            double currentTime = glfwGetTime();
            float  deltaTime   = (float) (currentTime - lastTime);
            lastTime = currentTime;

            input.processKeyboard(deltaTime);
            renderer.render(camera);

            window.swapBuffers();
            window.pollEvents();
        }
    }

    private void cleanup() {
        renderer.cleanup();
        window.destroy();
    }

    public static void main(String[] args) {
        new Game().run();
    }
}