package com.angelo.mmorpg.input;

import com.angelo.mmorpg.camera.Camera;
import com.angelo.mmorpg.debug.DebugState;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

public class InputHandler {

    private final long       windowHandle;
    private final Camera     camera;
    private final DebugState debugState;
    private final int        screenW;
    private final int        screenH;

    private Vector3f moveTarget    = null;
    private boolean  hasMoveTarget = false;

    public InputHandler(long windowHandle, Camera camera, DebugState debugState, int screenWidth, int screenHeight) {
        this.windowHandle = windowHandle;
        this.camera       = camera;
        this.debugState   = debugState;
        this.screenW      = screenWidth;
        this.screenH      = screenHeight;
    }

    public void init() {
        glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_NORMAL);

        // Clique esquerdo → mover player
        glfwSetMouseButtonCallback(windowHandle, (window, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                double[] xpos = new double[1];
                double[] ypos = new double[1];
                glfwGetCursorPos(window, xpos, ypos);

                Vector3f worldPos = camera.screenToWorld(
                        (float) xpos[0], (float) ypos[0], screenW, screenH
                );

                if (worldPos != null) {
                    moveTarget    = worldPos;
                    hasMoveTarget = true;
                }
            }
        });

        // Scroll → zoom
        glfwSetScrollCallback(windowHandle, (window, xoffset, yoffset) ->
                camera.setZoom(camera.getZoom() - (float) yoffset * 0.5f)
        );

        // Teclado
        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                if (key == GLFW_KEY_ESCAPE)
                    glfwSetWindowShouldClose(window, true);

                if (key == GLFW_KEY_F3)
                    debugState.toggleDebugPanel();

                if (key == GLFW_KEY_G && (mods & GLFW_MOD_CONTROL) != 0)
                    debugState.toggleGrid();

                // Q/E → rotacionar câmera 45°
                if (key == GLFW_KEY_PAGE_DOWN)
                    camera.rotateLeft();

                if (key == GLFW_KEY_PAGE_UP)
                    camera.rotateRight();
            }
        });
    }

    public Vector3f consumeMoveTarget() {
        if (!hasMoveTarget) return null;
        hasMoveTarget = false;
        return moveTarget;
    }
}