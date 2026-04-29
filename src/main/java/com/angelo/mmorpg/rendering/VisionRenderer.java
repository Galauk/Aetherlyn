package com.angelo.mmorpg.rendering;

import com.angelo.mmorpg.camera.Camera;
import com.angelo.mmorpg.core.ResourceLoader;
import com.angelo.mmorpg.entity.Creature;
import com.angelo.mmorpg.entity.CreatureBehavior;
import com.angelo.mmorpg.entity.CreatureManager;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Renderiza círculos de raio de visão das criaturas (apenas em debug mode).
 * Cor por comportamento: vermelho=HOSTILE, amarelo=NEUTRAL, verde=PASSIVE.
 */
public class VisionRenderer {

    private static final int CIRCLE_SEGMENTS = 32;

    private int shader;
    private int vao, vbo;

    private int locModel, locView, locProjection, locColor;

    public void init() {
        shader = ResourceLoader.createShaderProgramFromSource(
                "#version 330 core\n" +
                        "layout(location=0) in vec3 aPos;\n" +
                        "uniform mat4 model, view, projection;\n" +
                        "void main(){gl_Position=projection*view*model*vec4(aPos,1.0);}\n",
                "#version 330 core\n" +
                        "out vec4 F; uniform vec4 color;\n" +
                        "void main(){F=color;}\n"
        );
        locModel      = glGetUniformLocation(shader, "model");
        locView       = glGetUniformLocation(shader, "view");
        locProjection = glGetUniformLocation(shader, "projection");
        locColor      = glGetUniformLocation(shader, "color");

        // Círculo unitário no plano XZ (y=0)
        List<Float> verts = new ArrayList<>();
        for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
            float angle = (float)(2 * Math.PI * i / CIRCLE_SEGMENTS);
            verts.add((float) Math.cos(angle));
            verts.add(0.01f); // ligeiramente acima do chão
            verts.add((float) Math.sin(angle));
        }

        FloatBuffer buf = BufferUtils.createFloatBuffer(verts.size());
        for (float f : verts) buf.put(f);
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

    public void render(Camera camera, CreatureManager manager) {
        glUseProgram(shader);

        Matrix4f view = camera.getViewMatrix();
        Matrix4f proj = camera.getProjectionMatrix();
        FloatBuffer fb = BufferUtils.createFloatBuffer(16);
        glUniformMatrix4fv(locView,       false, view.get(fb));
        glUniformMatrix4fv(locProjection, false, proj.get(fb));

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glBindVertexArray(vao);

        for (Creature c : manager.getCreatures()) {
            if (c.isDead()) continue;

            // Cor por comportamento
            float r, g, b;
            switch (c.getDef().behavior) {
                case HOSTILE -> { r=0.9f; g=0.1f; b=0.1f; }
                case NEUTRAL -> { r=0.9f; g=0.8f; b=0.1f; }
                default      -> { r=0.1f; g=0.9f; b=0.3f; }
            }

            // Raio de visão
            Matrix4f model = new Matrix4f()
                    .translate(c.getPosition().x, 0f, c.getPosition().z)
                    .scale(c.getDef().visionRadius);
            glUniformMatrix4fv(locModel, false, model.get(fb));
            glUniform4f(locColor, r, g, b, 0.3f);
            glDrawArrays(GL_LINE_STRIP, 0, CIRCLE_SEGMENTS + 1);

            // Raio de ataque (só se for combativo)
            if (c.getDef().damageMax > 0) {
                Matrix4f attackModel = new Matrix4f()
                        .translate(c.getPosition().x, 0f, c.getPosition().z)
                        .scale(c.getDef().attackRadius);
                glUniformMatrix4fv(locModel, false, attackModel.get(fb));
                glUniform4f(locColor, r, g, b, 0.6f);
                glDrawArrays(GL_LINE_STRIP, 0, CIRCLE_SEGMENTS + 1);
            }
        }

        glBindVertexArray(0);
        glDisable(GL_BLEND);
    }

    public void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteProgram(shader);
    }
}