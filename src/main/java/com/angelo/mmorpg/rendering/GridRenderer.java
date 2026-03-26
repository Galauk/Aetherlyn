package com.angelo.mmorpg.rendering;

import com.angelo.mmorpg.camera.Camera;
import com.angelo.mmorpg.core.ResourceLoader;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class GridRenderer {

    private static final int   GRID_HALF = 20;
    private static final float LINE_STEP = 1.0f;

    private int shaderProgram;
    private int vao, vbo;
    private int lineCount;

    private static final String VERT_SRC =
            "#version 330 core\n" +
                    "layout(location=0) in vec3 aPos;\n" +
                    "uniform mat4 view;\n" +
                    "uniform mat4 projection;\n" +
                    "void main() {\n" +
                    "    gl_Position = projection * view * vec4(aPos, 1.0);\n" +
                    "}\n";

    private static final String FRAG_SRC =
            "#version 330 core\n" +
                    "out vec4 FragColor;\n" +
                    "uniform vec4 color;\n" +
                    "void main() {\n" +
                    "    FragColor = color;\n" +
                    "}\n";

    public void init() {
        shaderProgram = ResourceLoader.createShaderProgramFromSource(VERT_SRC, FRAG_SRC);
        buildGrid();
    }

    private void buildGrid() {
        List<Float> verts = new ArrayList<>();
        float lim = GRID_HALF * LINE_STEP;

        for (int i = -GRID_HALF; i <= GRID_HALF; i++) {
            float fi = i * LINE_STEP;
            verts.add(fi);   verts.add(0.01f); verts.add(-lim);
            verts.add(fi);   verts.add(0.01f); verts.add( lim);
            verts.add(-lim); verts.add(0.01f); verts.add(fi);
            verts.add( lim); verts.add(0.01f); verts.add(fi);
        }

        lineCount = verts.size() / 3;

        FloatBuffer buf = BufferUtils.createFloatBuffer(verts.size());
        for (float v : verts) buf.put(v);
        buf.flip();

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buf, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glBindVertexArray(0);
    }

    public void render(Camera camera) {
        glUseProgram(shaderProgram);

        FloatBuffer fb = BufferUtils.createFloatBuffer(16);
        glUniformMatrix4fv(glGetUniformLocation(shaderProgram, "view"),       false, camera.getViewMatrix().get(fb));
        glUniformMatrix4fv(glGetUniformLocation(shaderProgram, "projection"), false, camera.getProjectionMatrix().get(fb));

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glUniform4f(glGetUniformLocation(shaderProgram, "color"), 1f, 1f, 1f, 0.25f);

        glBindVertexArray(vao);
        glDrawArrays(GL_LINES, 0, lineCount);
        glBindVertexArray(0);

        glDisable(GL_BLEND);
    }

    public void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteProgram(shaderProgram);
    }
}