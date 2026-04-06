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

/**
 * Renderiza o player como um billboard sprite —
 * um quad plano que sempre fica de frente para a câmera isométrica.
 */
public class Renderer {

    private int shaderProgram;
    private int vao, vbo, ebo;
    private int playerTexture;

    // Tamanho visual do sprite no mundo (1 tile = 1 unidade)
    private static final float SPRITE_W = 0.8f;
    private static final float SPRITE_H = 1.0f;

    // Quad centrado horizontalmente, base no Y=0
    private static final float[] VERTICES = {
            -SPRITE_W / 2, 0.0f,     0.0f,  0.0f, 1.0f,
            SPRITE_W / 2, 0.0f,     0.0f,  1.0f, 1.0f,
            SPRITE_W / 2, SPRITE_H, 0.0f,  1.0f, 0.0f,
            -SPRITE_W / 2, SPRITE_H, 0.0f,  0.0f, 0.0f,
    };

    private static final int[] INDICES = {
            0, 1, 2,
            2, 3, 0,
    };

    public void init() {
        shaderProgram = ResourceLoader.createShaderProgram("/shaders/vertex.glsl", "/shaders/fragment.glsl");
        playerTexture = ResourceLoader.loadTexture("/assets/player.png");

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);

        FloatBuffer fb = BufferUtils.createFloatBuffer(VERTICES.length);
        fb.put(VERTICES).flip();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);

        IntBuffer ib = BufferUtils.createIntBuffer(INDICES.length);
        ib.put(INDICES).flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    public void render(Camera camera, Vector3f playerPos) {
        glUseProgram(shaderProgram);

        Matrix4f view = camera.getViewMatrix();

        // Eixo right da câmera extraído da view matrix
        Vector3f right = new Vector3f(view.m00(), view.m10(), view.m20());
        // Up fixo no Y do mundo para evitar inclinação com câmera isométrica
        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);

        // Billboard matrix: alinha o quad com o eixo right da câmera
        Matrix4f model = new Matrix4f(
                right.x,     right.y,     right.z,     0,
                up.x,        up.y,        up.z,        0,
                0,           0,           1,           0,
                playerPos.x, playerPos.y, playerPos.z, 1
        );

        setUniformMatrix("model",      model);
        setUniformMatrix("view",       view);
        setUniformMatrix("projection", camera.getProjectionMatrix());

        glBindTexture(GL_TEXTURE_2D, playerTexture);
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
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
        glDeleteTextures(playerTexture);
        glDeleteProgram(shaderProgram);
    }
}