package com.angelo.mmorpg.input;

import com.angelo.mmorpg.camera.Camera;
import com.angelo.mmorpg.debug.DebugState;
import com.angelo.mmorpg.entity.Inventory;
import com.angelo.mmorpg.rendering.HudRenderer;
import com.angelo.mmorpg.rendering.InventoryRenderer;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

public class InputHandler {

    private final long              windowHandle;
    private final Camera            camera;
    private final DebugState        debugState;
    private final InventoryRenderer inventoryRenderer;
    private final HudRenderer       hudRenderer;
    private final Inventory         inventory;
    private final int               screenW;
    private final int               screenH;

    private Vector3f moveTarget       = null;
    private boolean  hasMoveTarget    = false;
    private Vector3f interactTarget   = null;
    private boolean  hasInteractTarget= false;

    private float mouseX, mouseY;

    public InputHandler(long windowHandle, Camera camera, DebugState debugState,
                        InventoryRenderer inventoryRenderer, HudRenderer hudRenderer,
                        Inventory inventory, int screenWidth, int screenHeight) {
        this.windowHandle      = windowHandle;
        this.camera            = camera;
        this.debugState        = debugState;
        this.inventoryRenderer = inventoryRenderer;
        this.hudRenderer       = hudRenderer;
        this.inventory         = inventory;
        this.screenW           = screenWidth;
        this.screenH           = screenHeight;
    }

    public void init() {
        glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_NORMAL);

        glfwSetCursorPosCallback(windowHandle, (window, xpos, ypos) -> {
            mouseX = (float) xpos;
            mouseY = (float) ypos;
            inventoryRenderer.onMouseMove(mouseX, mouseY, inventory);
            hudRenderer.setMousePos(mouseX, mouseY);
        });

        glfwSetMouseButtonCallback(windowHandle, (window, button, action, mods) -> {
            inventoryRenderer.onMouseButton(mouseX, mouseY, button, action, inventory);

            if (action == GLFW_PRESS) {
                Vector3f worldPos = camera.screenToWorld(mouseX, mouseY, screenW, screenH);

                if (button == GLFW_MOUSE_BUTTON_LEFT && worldPos != null) {
                    moveTarget    = worldPos;
                    hasMoveTarget = true;
                }
                if (button == GLFW_MOUSE_BUTTON_RIGHT && worldPos != null) {
                    interactTarget    = worldPos;
                    hasInteractTarget = true;
                }
            }
        });

        glfwSetScrollCallback(windowHandle, (window, xoffset, yoffset) ->
                camera.setZoom(camera.getZoom() - (float) yoffset * 0.5f)
        );

        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                if (key == GLFW_KEY_ESCAPE) glfwSetWindowShouldClose(window, true);
                if (key == GLFW_KEY_F3)     debugState.toggleDebugPanel();
                if (key == GLFW_KEY_G && (mods & GLFW_MOD_CONTROL) != 0) debugState.toggleGrid();
                if (key == GLFW_KEY_Q)      camera.rotateLeft();
                if (key == GLFW_KEY_E)      camera.rotateRight();
                if (key == GLFW_KEY_I)      inventoryRenderer.toggle();
            }
        });
    }

    public Vector3f consumeMoveTarget() {
        if (!hasMoveTarget) return null;
        hasMoveTarget = false;
        return moveTarget;
    }

    public Vector3f consumeInteractTarget() {
        if (!hasInteractTarget) return null;
        hasInteractTarget = false;
        return interactTarget;
    }
}