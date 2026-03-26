package com.angelo.mmorpg.rendering;

import com.angelo.mmorpg.camera.Camera;
import com.angelo.mmorpg.core.ResourceLoader;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Renderer {

    private int shaderProgram;

    private int vao, vbo, ebo;
    private int texture;

    private int floorVao, floorVbo, floorEbo;
    private int floorTexture;

    private static final float FLOOR_SIZE = 20.0f;

    private static final float[] CUBE_VERTICES = {
            -0.5f, -0.5f, -0.5f,  0.0f, 0.0f,
            0.5f, -0.5f, -0.5f,  1.0f, 0.0f,
            0.5f,  0.5f, -0.5f,  1.0f, 1.0f,
            -0.5f,  0.5f, -0.5f,  0.0f, 1.0f,
            -0.5f, -0.5f,  0.5f,  0.0f, 0.0f,
            0.5f, -0.5f,  0.5f,  1.0f, 0.0f,
            0.5f,  0.5f,  0.5f,  1.0f, 1.0f,
            -0.5f,  0.5f,  0.5f,  0.0f, 1.0f,
    };

    private static final int[] CUBE_INDICES = {
            0, 1, 2, 2, 3, 0,
            1, 5, 6, 6, 2, 1,
            5, 4, 7, 7, 6, 5,
            4, 0, 3, 3, 7, 4,
            3, 2, 6, 6, 7, 3,
            4, 5, 1, 1, 0, 4,
    };

    public void init() {
        shaderProgram = ResourceLoader.createShaderProgram("/shaders/vertex.glsl", "/shaders/fragment.glsl");
        texture       = ResourceLoader.loadTexture("/assets/grass.png");

        setupCube();
        setupFloor();

        glEnable(GL_DEPTH_TEST);
        glClearColor(0.15f, 0.2f, 0.25f, 1.0f);
    }

    private void setupCube() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);

        FloatBuffer fb = BufferUtils.createFloatBuffer(CUBE_VERTICES.length);
        fb.put(CUBE_VERTICES).flip();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);

        IntBuffer ib = BufferUtils.createIntBuffer(CUBE_INDICES.length);
        ib.put(CUBE_INDICES).flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
    }

    private void setupFloor() {
        float s = FLOOR_SIZE;
        float[] floorVertices = {
                -s, 0.0f, -s,  0.0f, s,
                s, 0.0f, -s,  s,    s,
                s, 0.0f,  s,  s,    0.0f,
                -s, 0.0f,  s,  0.0f, 0.0f,
        };
        int[] floorIndices = { 0, 1, 2, 2, 3, 0 };

        floorVao = glGenVertexArrays();
        floorVbo = glGenBuffers();
        floorEbo = glGenBuffers();

        glBindVertexArray(floorVao);

        FloatBuffer fb = BufferUtils.createFloatBuffer(floorVertices.length);
        fb.put(floorVertices).flip();
        glBindBuffer(GL_ARRAY_BUFFER, floorVbo);
        glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);

        IntBuffer ib = BufferUtils.createIntBuffer(floorIndices.length);
        ib.put(floorIndices).flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, floorEbo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);

        floorTexture = texture;
    }

    public void render(Camera camera, Vector3f playerPos) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glUseProgram(shaderProgram);

        setUniformMatrix("view",       camera.getViewMatrix());
        setUniformMatrix("projection", camera.getProjectionMatrix());

        // Chão
        setUniformMatrix("model", new Matrix4f().identity());
        glBindTexture(GL_TEXTURE_2D, floorTexture);
        glBindVertexArray(floorVao);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);

        // Cubo do player (Y=0.5 para ficar sobre o chão)
        setUniformMatrix("model", new Matrix4f().translate(new Vector3f(playerPos.x, 0.5f, playerPos.z)));
        glBindTexture(GL_TEXTURE_2D, texture);
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0);

        glBindVertexArray(0);
    }

    private void setUniformMatrix(String name, Matrix4f matrix) {
        int loc = glGetUniformLocation(shaderProgram, name);
        FloatBuffer buf = BufferUtils.createFloatBuffer(16);
        glUniformMatrix4fv(loc, false, matrix.get(buf));
    }

    public void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        glDeleteVertexArrays(floorVao);
        glDeleteBuffers(floorVbo);
        glDeleteBuffers(floorEbo);
        glDeleteTextures(texture);
        glDeleteProgram(shaderProgram);
    }
}