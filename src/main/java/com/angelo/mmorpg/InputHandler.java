package com.angelo.mmorpg;

import static org.lwjgl.glfw.GLFW.*;

public class InputHandler {

    private final long windowHandle;
    private final Camera camera;

    private double lastX;
    private double lastY;
    private boolean firstMouse = true;

    public InputHandler(long windowHandle, Camera camera, int screenWidth, int screenHeight) {
        this.windowHandle = windowHandle;
        this.camera = camera;
        this.lastX = screenWidth / 2.0;
        this.lastY = screenHeight / 2.0;
    }

    public void init() {
        glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        glfwSetCursorPosCallback(windowHandle, (window, xpos, ypos) -> {
            if (firstMouse) {
                lastX = xpos;
                lastY = ypos;
                firstMouse = false;
            }

            float xOffset = (float) (xpos - lastX);
            float yOffset = (float) (lastY - ypos); // invertido: y cresce para baixo na tela
            lastX = xpos;
            lastY = ypos;

            camera.processMouse(xOffset, yOffset);
        });
    }

    public void processKeyboard(float deltaTime) {
        boolean w = glfwGetKey(windowHandle, GLFW_KEY_W) == GLFW_PRESS;
        boolean s = glfwGetKey(windowHandle, GLFW_KEY_S) == GLFW_PRESS;
        boolean a = glfwGetKey(windowHandle, GLFW_KEY_A) == GLFW_PRESS;
        boolean d = glfwGetKey(windowHandle, GLFW_KEY_D) == GLFW_PRESS;

        camera.processKeyboard(w, s, a, d, deltaTime);

        if (glfwGetKey(windowHandle, GLFW_KEY_ESCAPE) == GLFW_PRESS) {
            glfwSetWindowShouldClose(windowHandle, true);
        }
    }
}

