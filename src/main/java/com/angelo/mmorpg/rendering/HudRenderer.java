package com.angelo.mmorpg.rendering;

import com.angelo.mmorpg.core.ResourceLoader;
import com.angelo.mmorpg.entity.Player;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * HUD fixo na tela:
 *  - Barra de HP (canto inferior esquerdo)
 *  - Barra de peso do inventário
 *  - Indicador de modo combate (barra HP fica pulsando em vermelho)
 *  - Tela de morte com contador de respawn
 */
public class HudRenderer {

    private static final float BAR_X   = 16f;
    private static final float BAR_W   = 180f;
    private static final float BAR_H   = 18f;
    private static final float BAR_GAP = 6f;

    private final int screenW;
    private final int screenH;

    private int shader;
    private int vao, vbo, ebo;
    private int locProj, locPos, locSize, locColor;

    // Para animação de pulso da barra de HP em combate
    private float pulseTimer = 0f;

    public HudRenderer(int screenW, int screenH) {
        this.screenW = screenW;
        this.screenH = screenH;
    }

    public void init() {
        shader = ResourceLoader.createShaderProgramFromSource(
                "#version 330 core\n" +
                        "layout(location=0) in vec2 aPos;\n" +
                        "uniform mat4 projection;\n" +
                        "uniform vec2 pos;\n" +
                        "uniform vec2 size;\n" +
                        "void main() { gl_Position = projection * vec4(pos + aPos * size, 0.0, 1.0); }\n",

                "#version 330 core\n" +
                        "out vec4 FragColor;\n" +
                        "uniform vec4 color;\n" +
                        "void main() { FragColor = color; }\n"
        );

        locProj  = glGetUniformLocation(shader, "projection");
        locPos   = glGetUniformLocation(shader, "pos");
        locSize  = glGetUniformLocation(shader, "size");
        locColor = glGetUniformLocation(shader, "color");

        float[] v = { 0,0, 1,0, 1,1, 0,1 };
        int[]   i = { 0,1,2, 2,3,0 };

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);
        FloatBuffer fb = BufferUtils.createFloatBuffer(v.length);
        fb.put(v).flip();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);
        IntBuffer ib = BufferUtils.createIntBuffer(i.length);
        ib.put(i).flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glBindVertexArray(0);
    }

    public void render(Player player, boolean isDead, float deathTimer) {
        pulseTimer += 0.016f; // ~60fps

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glUseProgram(shader);
        Matrix4f ortho = new Matrix4f().ortho(0, screenW, screenH, 0, -1, 1);
        FloatBuffer fb = BufferUtils.createFloatBuffer(16);
        glUniformMatrix4fv(locProj, false, ortho.get(fb));

        if (isDead) {
            // Overlay escuro
            rect(0, 0, screenW, screenH, 0f, 0f, 0f, 0.6f);

            // Caixa central "YOU DIED"
            float bw = 300f, bh = 80f;
            float bx = (screenW - bw) / 2f;
            float by = (screenH - bh) / 2f;
            rect(bx, by, bw, bh, 0.5f, 0f, 0f, 0.9f);
            // Borda
            rect(bx,      by,      bw, 2,  0.9f,0.2f,0.2f,1f);
            rect(bx,      by+bh-2, bw, 2,  0.9f,0.2f,0.2f,1f);
            rect(bx,      by,      2,  bh, 0.9f,0.2f,0.2f,1f);
            rect(bx+bw-2, by,      2,  bh, 0.9f,0.2f,0.2f,1f);

            // Barra de progresso do respawn
            float progress = 1f - (deathTimer / 5.0f);
            float barY = by + bh + 10f;
            rect(bx, barY, bw,          12f, 0.1f,0.1f,0.1f,0.9f);
            rect(bx, barY, bw*progress, 12f, 0.8f,0.3f,0.1f,1.0f);

        } else {
            // --- Barra de HP ---
            float hpRatio    = (float) player.getStats().getHp() / player.getStats().getMaxHp();
            float hpY        = screenH - 16f - BAR_H;
            boolean inCombat = player.getStats().isInCombat();

            // Pulso vermelho em combate
            float pulse = inCombat ? (float)(0.5f + 0.5f * Math.sin(pulseTimer * 6f)) : 0f;

            drawBar(hpY, hpRatio,
                    1f - hpRatio + pulse * 0.3f,
                    hpRatio * 0.8f * (1f - pulse * 0.5f),
                    0f);

            // Ponto vermelho indicando combate ativo
            if (inCombat) {
                rect(BAR_X + BAR_W + 6f, hpY + 4f, 10f, 10f, 0.9f, 0.1f, 0.1f, 0.9f);
            }

            // --- Barra de peso ---
            float weightY     = hpY - BAR_H - BAR_GAP;
            float weightRatio = player.getInventory().getCurrentWeight()
                    / player.getInventory().getMaxWeight();
            drawBar(weightY, weightRatio, weightRatio * 0.7f, 0.5f - weightRatio * 0.3f, 0.1f);
        }

        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }

    private void drawBar(float y, float ratio, float fr, float fg, float fb2) {
        glBindVertexArray(vao);
        rect(BAR_X, y, BAR_W,                                   BAR_H, 0.08f,0.08f,0.08f,0.85f);
        rect(BAR_X, y, BAR_W * Math.max(0, Math.min(ratio,1f)), BAR_H, fr,   fg,   fb2,  1.0f);
        rect(BAR_X,           y,          BAR_W, 1,     0.6f,0.6f,0.6f,1f);
        rect(BAR_X,           y+BAR_H-1,  BAR_W, 1,     0.6f,0.6f,0.6f,1f);
        rect(BAR_X,           y,          1,     BAR_H, 0.6f,0.6f,0.6f,1f);
        rect(BAR_X+BAR_W-1,   y,          1,     BAR_H, 0.6f,0.6f,0.6f,1f);
        glBindVertexArray(0);
    }

    private void rect(float x, float y, float w, float h,
                      float r, float g, float b, float a) {
        glUniform2f(locPos,   x, y);
        glUniform2f(locSize,  w, h);
        glUniform4f(locColor, r, g, b, a);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
    }

    public void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        glDeleteProgram(shader);
    }
}