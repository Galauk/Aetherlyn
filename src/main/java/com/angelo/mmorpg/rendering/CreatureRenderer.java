package com.angelo.mmorpg.rendering;

import com.angelo.mmorpg.camera.Camera;
import com.angelo.mmorpg.core.ResourceLoader;
import com.angelo.mmorpg.entity.Creature;
import com.angelo.mmorpg.entity.CreatureManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.util.HashMap;
import java.util.Map;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Renderiza criaturas como billboard sprites com barra de HP.
 * Carrega texturas por tipo de criatura (uma por texturePath único).
 */
public class CreatureRenderer {

    private static final float SPRITE_W = 0.8f;
    private static final float SPRITE_H = 1.0f;

    // HP bar
    private static final float BAR_W    = 0.8f;
    private static final float BAR_H    = 0.08f;
    private static final float BAR_Y    = 1.15f; // acima do sprite

    private int spriteProg, flatProg;
    private int spriteVao, spriteVbo, spriteEbo;
    private int flatVao,   flatVbo,   flatEbo;

    // Cache de texturas por path
    private final Map<String, Integer> textureCache = new HashMap<>();

    // Uniform locations
    private int locSprModel, locSprView, locSprProj;
    private int locFlatModel, locFlatView, locFlatProj, locFlatColor;

    public void init() {
        spriteProg = buildSpriteShader();
        flatProg   = buildFlatShader();

        spriteVao = buildQuad(SPRITE_W, SPRITE_H, true);
        flatVao   = buildQuad(1f,       1f,        false);

        locSprModel  = glGetUniformLocation(spriteProg, "model");
        locSprView   = glGetUniformLocation(spriteProg, "view");
        locSprProj   = glGetUniformLocation(spriteProg, "projection");

        locFlatModel  = glGetUniformLocation(flatProg, "model");
        locFlatView   = glGetUniformLocation(flatProg, "view");
        locFlatProj   = glGetUniformLocation(flatProg, "projection");
        locFlatColor  = glGetUniformLocation(flatProg, "color");
    }

    public void render(Camera camera, CreatureManager manager) {
        if (manager.getCreatures().isEmpty()) return;

        Matrix4f view = camera.getViewMatrix();
        Matrix4f proj = camera.getProjectionMatrix();

        Vector3f right = new Vector3f(view.m00(), view.m10(), view.m20());
        Vector3f up    = new Vector3f(0f, 1f, 0f);

        FloatBuffer fb = BufferUtils.createFloatBuffer(16);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        for (Creature c : manager.getCreatures()) {
            if (c.isDead()) continue;

            Vector3f pos    = c.getPosition();
            Matrix4f model  = billboardMatrix(right, up, pos);

            // --- Sprite ---
            int tex = getTexture(c.getDef().texturePath);
            glUseProgram(spriteProg);
            glUniformMatrix4fv(locSprModel, false, model.get(fb));
            glUniformMatrix4fv(locSprView,  false, view.get(fb));
            glUniformMatrix4fv(locSprProj,  false, proj.get(fb));
            glBindTexture(GL_TEXTURE_2D, tex);
            glBindVertexArray(spriteVao);
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);

            // --- Barra de HP ---
            float hpRatio = (float) c.getHp() / c.getMaxHp();

            glUseProgram(flatProg);
            glUniformMatrix4fv(locFlatView, false, view.get(fb));
            glUniformMatrix4fv(locFlatProj, false, proj.get(fb));
            glBindVertexArray(flatVao);

            // Fundo (cinza escuro)
            Matrix4f barBg = billboardMatrix(right, up,
                    new Vector3f(pos.x, BAR_Y, pos.z));
            barBg.scale(BAR_W, BAR_H, 1f);
            glUniformMatrix4fv(locFlatModel, false, barBg.get(fb));
            glUniform4f(locFlatColor, 0.2f, 0.2f, 0.2f, 0.85f);
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);

            // Fill (verde → vermelho)
            Matrix4f barFill = billboardMatrix(right, up,
                    new Vector3f(pos.x - BAR_W / 2f * (1f - hpRatio),
                            BAR_Y, pos.z));
            barFill.scale(BAR_W * hpRatio, BAR_H, 1f);
            glUniformMatrix4fv(locFlatModel, false, barFill.get(fb));
            glUniform4f(locFlatColor, 1f - hpRatio, hpRatio, 0f, 1f);
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);

            glBindVertexArray(0);
        }

        glDisable(GL_BLEND);
    }

    private Matrix4f billboardMatrix(Vector3f right, Vector3f up, Vector3f pos) {
        return new Matrix4f(
                right.x, right.y, right.z, 0,
                up.x,    up.y,    up.z,    0,
                0,       0,       1,       0,
                pos.x,   pos.y,   pos.z,   1
        );
    }

    private int getTexture(String path) {
        return textureCache.computeIfAbsent(path, ResourceLoader::loadTexture);
    }

    private int buildQuad(float w, float h, boolean centered) {
        float x0 = centered ? -w / 2f : 0f;
        float[] verts = {
                x0,     0,  0,  0, 1,
                x0 + w, 0,  0,  1, 1,
                x0 + w, h,  0,  1, 0,
                x0,     h,  0,  0, 0,
        };
        int[] idx = { 0,1,2, 2,3,0 };

        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        int ebo = glGenBuffers();

        glBindVertexArray(vao);

        FloatBuffer fb = BufferUtils.createFloatBuffer(verts.length);
        fb.put(verts).flip();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);

        IntBuffer ib = BufferUtils.createIntBuffer(idx.length);
        ib.put(idx).flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
        return vao;
    }

    public void cleanup() {
        glDeleteVertexArrays(spriteVao);
        glDeleteVertexArrays(flatVao);
        glDeleteProgram(spriteProg);
        glDeleteProgram(flatProg);
        textureCache.values().forEach(t -> glDeleteTextures(t));
    }

    // --- Shaders ---

    private static int buildSpriteShader() {
        return ResourceLoader.createShaderProgramFromSource(
                "#version 330 core\n" +
                        "layout(location=0) in vec3 aPos;\n" +
                        "layout(location=1) in vec2 aUV;\n" +
                        "out vec2 UV;\n" +
                        "uniform mat4 model, view, projection;\n" +
                        "void main() {\n" +
                        "    gl_Position = projection * view * model * vec4(aPos,1.0);\n" +
                        "    UV = aUV;\n" +
                        "}\n",
                "#version 330 core\n" +
                        "in vec2 UV;\n" +
                        "out vec4 FragColor;\n" +
                        "uniform sampler2D tex;\n" +
                        "void main() {\n" +
                        "    vec4 c = texture(tex, UV);\n" +
                        "    if (c.a < 0.1) discard;\n" +
                        "    FragColor = c;\n" +
                        "}\n"
        );
    }

    private static int buildFlatShader() {
        return ResourceLoader.createShaderProgramFromSource(
                "#version 330 core\n" +
                        "layout(location=0) in vec3 aPos;\n" +
                        "layout(location=1) in vec2 aUV;\n" +
                        "uniform mat4 model, view, projection;\n" +
                        "void main() {\n" +
                        "    gl_Position = projection * view * model * vec4(aPos,1.0);\n" +
                        "}\n",
                "#version 330 core\n" +
                        "out vec4 FragColor;\n" +
                        "uniform vec4 color;\n" +
                        "void main() { FragColor = color; }\n"
        );
    }
}