package com.angelo.mmorpg.core;

import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {

    private long handle;
    private int  width;
    private int  height;
    private final String title;

    // Flag para que o Game saiba quando recalcular projeções
    private boolean resized = false;

    public Window(int width, int height, String title) {
        this.width  = width;
        this.height = height;
        this.title  = title;
    }

    public void init() {
        if (!glfwInit()) throw new IllegalStateException("Falha ao iniciar GLFW");

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        handle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (handle == NULL) throw new RuntimeException("Falha ao criar janela");

        glfwMakeContextCurrent(handle);
        glfwSwapInterval(1);
        glfwShowWindow(handle);

        GL.createCapabilities();

        // Callback de redimensionamento — atualiza viewport OpenGL automaticamente
        glfwSetFramebufferSizeCallback(handle, (window, w, h) -> {
            width   = w;
            height  = h;
            resized = true;
            glViewport(0, 0, w, h);
        });
    }

    /** Consome o flag de resize — retorna true se a janela foi redimensionada. */
    public boolean wasResized() {
        if (resized) { resized = false; return true; }
        return false;
    }

    public boolean shouldClose()  { return glfwWindowShouldClose(handle); }
    public void    swapBuffers()  { glfwSwapBuffers(handle); }
    public void    pollEvents()   { glfwPollEvents(); }
    public void    destroy()      { glfwDestroyWindow(handle); glfwTerminate(); }

    public long getHandle()  { return handle; }
    public int  getWidth()   { return width; }
    public int  getHeight()  { return height; }
}