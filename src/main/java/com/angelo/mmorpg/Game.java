package com.angelo.mmorpg;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.stb.*;
import org.lwjgl.system.*;
import java.nio.*;
import java.util.Random;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Game {
    private long window;
    private int[][] map;
    private int tileSize = 32;
    private float playerX = 100, playerY = 100;
    private int wood = 0;
    private boolean isLich = false;
    private float skeletonX = -1, skeletonY = -1;
    private int grassTexture, treeTexture, stoneTexture, playerTexture, lichTexture, skeletonTexture;
    private double lastTime; // Para calcular delta time

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
// Inicializa GLFW
        if (!glfwInit()) throw new IllegalStateException("Falha ao iniciar GLFW");
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        window = glfwCreateWindow(800, 600, "Aetherlyn: MMORPG", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Falha ao criar janela");
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (vidmode.width() - 800) / 2, (vidmode.height() - 600) / 2);
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        GL.createCapabilities();

// Configura OpenGL
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

// Carrega texturas
        grassTexture = loadTexture("assets/grass.png");
        treeTexture = loadTexture("assets/tree.png");
        stoneTexture = loadTexture("assets/stone.png");
        playerTexture = loadTexture("assets/player.png");
        lichTexture = loadTexture("assets/lich.png");
        skeletonTexture = loadTexture("assets/skeleton.png");

// Gera mapa
        map = new int[50][50];
        Random rand = new Random();
        for (int x = 0; x < 50; x++) {
            for (int y = 0; y < 50; y++) {
                map[x][y] = rand.nextInt(3); // 0 = grama, 1 = árvore, 2 = pedra
            }
        }

// Inicializa tempo
        lastTime = glfwGetTime();
    }

    private int loadTexture(String path) {
        // Caminho absoluto para teste
        String absolutePath = "src/main/resources/" + path;
        java.io.File file = new java.io.File(absolutePath);
        if (!file.exists()) {
            throw new RuntimeException("Arquivo de textura não encontrado: " + absolutePath);
        }

        IntBuffer width = BufferUtils.createIntBuffer(1);
        IntBuffer height = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);
        ByteBuffer image = stbi_load(absolutePath, width, height, channels, 4);
        if (image == null) {
            throw new RuntimeException("Falha ao carregar textura: " + absolutePath + ". Erro STB: " + stbi_failure_reason());
        }

        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        stbi_image_free(image);
        return texture;
    }

    private void loop() {
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glOrtho(0, 800, 0, 600, -1, 1);

        while (!glfwWindowShouldClose(window)) {
// Calcula delta time
            double currentTime = glfwGetTime();
            float deltaTime = (float) (currentTime - lastTime);
            lastTime = currentTime;

// Movimento
            float speed = 200 * deltaTime;
            if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) playerY += speed;
            if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) playerY -= speed;
            if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) playerX -= speed;
            if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) playerX += speed;

// Coleta madeira
            if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS && !isLich) {
                for (int x = 0; x < 50; x++) {
                    for (int y = 0; y < 50; y++) {
                        if (map[x][y] == 1) {
                            float treeX = x * tileSize, treeY = y * tileSize;
                            if (Math.abs(playerX - treeX) < tileSize && Math.abs(playerY - treeY) < tileSize) {
                                map[x][y] = 0;
                                wood += 10;
                                System.out.println("Coletou 10 madeira! Total: " + wood);
                                if (wood >= 50) {
                                    isLich = true;
                                    System.out.println("Transformado em Lich!");
                                }
                            }
                        }
                    }
                }
            }

// Habilidade do lich
            if (isLich && glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS && skeletonX == -1) {
                skeletonX = playerX + tileSize;
                skeletonY = playerY;
                System.out.println("Esqueleto invocado!");
            }

// Desenha
            glClear(GL_COLOR_BUFFER_BIT);
            for (int x = 0; x < 50; x++) {
                for (int y = 0; y < 50; y++) {
                    float posX = x * tileSize, posY = y * tileSize;
                    glBindTexture(GL_TEXTURE_2D, map[x][y] == 0 ? grassTexture : map[x][y] == 1 ? treeTexture : stoneTexture);
                    glBegin(GL_QUADS);
                    glTexCoord2f(0, 0); glVertex2f(posX, posY);
                    glTexCoord2f(1, 0); glVertex2f(posX + tileSize, posY);
                    glTexCoord2f(1, 1); glVertex2f(posX + tileSize, posY + tileSize);
                    glTexCoord2f(0, 1); glVertex2f(posX, posY + tileSize);
                    glEnd();
                }
            }
// Desenha jogador
            glBindTexture(GL_TEXTURE_2D, isLich ? lichTexture : playerTexture);
            glBegin(GL_QUADS);
            glTexCoord2f(0, 0); glVertex2f(playerX, playerY);
            glTexCoord2f(1, 0); glVertex2f(playerX + tileSize, playerY);
            glTexCoord2f(1, 1); glVertex2f(playerX + tileSize, playerY + tileSize);
            glTexCoord2f(0, 1); glVertex2f(playerX, playerY + tileSize);
            glEnd();
// Desenha esqueleto
            if (skeletonX != -1) {
                glBindTexture(GL_TEXTURE_2D, skeletonTexture);
                glBegin(GL_QUADS);
                glTexCoord2f(0, 0); glVertex2f(skeletonX, skeletonY);
                glTexCoord2f(1, 0); glVertex2f(skeletonX + tileSize, skeletonY);
                glTexCoord2f(1, 1); glVertex2f(skeletonX + tileSize, skeletonY + tileSize);
                glTexCoord2f(0, 1); glVertex2f(skeletonX, skeletonY + tileSize);
                glEnd();
            }

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void cleanup() {
        glDeleteTextures(grassTexture);
        glDeleteTextures(treeTexture);
        glDeleteTextures(stoneTexture);
        glDeleteTextures(playerTexture);
        glDeleteTextures(lichTexture);
        glDeleteTextures(skeletonTexture);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    public static void main(String[] args) {
        new Game().run();
    }
}