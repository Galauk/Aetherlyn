package com.angelo.mmorpg.rendering;

import com.angelo.mmorpg.core.ResourceLoader;
import com.angelo.mmorpg.entity.Creature;
import com.angelo.mmorpg.entity.CreatureManager;
import com.angelo.mmorpg.entity.Player;
import com.angelo.mmorpg.world.WorldMap;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * HUD completo:
 *  - Barra de HP + indicador de combate     (canto inferior esquerdo)
 *  - Barra de peso do inventário
 *  - Barra de XP + nível
 *  - Tela de morte com contador de respawn
 *  - Minimap quadrado                        (canto superior direito)
 *  - Tooltip de criatura ao passar o mouse
 */
public class HudRenderer {

    // --- Barras (canto inferior esquerdo) ---
    private static final float BAR_X   = 16f;
    private static final float BAR_W   = 180f;
    private static final float BAR_H   = 16f;
    private static final float BAR_GAP = 5f;

    // --- Minimap (canto superior direito) ---
    private static final int   MM_SIZE    = 150; // pixels
    private static final int   MM_MARGIN  = 12;
    private static final int   MM_BORDER  = 2;

    private final int screenW;
    private final int screenH;

    // Mouse position (atualizado externamente)
    private float mouseX, mouseY;

    private int shader;
    private int vao, vbo, ebo;
    private int locProj, locPos, locSize, locColor;

    // Minimap texture
    private int mmTexture;
    private int mmShader;
    private int mmVao, mmVbo, mmEbo;
    private int mmLocProj, mmLocPos, mmLocSize;

    private float pulseTimer = 0f;

    // Cache do minimap (só recria quando mapa muda)
    private int[] minimapPixels;
    private boolean minimapDirty = true;

    public HudRenderer(int screenW, int screenH) {
        this.screenW = screenW;
        this.screenH = screenH;
    }

    public void setMousePos(float x, float y) { mouseX = x; mouseY = y; }

    public void init() {
        // Shader flat (retângulos coloridos)
        shader = ResourceLoader.createShaderProgramFromSource(
                "#version 330 core\n" +
                        "layout(location=0) in vec2 aPos;\n" +
                        "uniform mat4 projection;\n" +
                        "uniform vec2 pos, size;\n" +
                        "void main() { gl_Position = projection * vec4(pos + aPos*size,0,1); }\n",
                "#version 330 core\n" +
                        "out vec4 FragColor;\n" +
                        "uniform vec4 color;\n" +
                        "void main() { FragColor = color; }\n"
        );
        locProj  = glGetUniformLocation(shader, "projection");
        locPos   = glGetUniformLocation(shader, "pos");
        locSize  = glGetUniformLocation(shader, "size");
        locColor = glGetUniformLocation(shader, "color");

        // Shader sprite (minimap texture)
        mmShader = ResourceLoader.createShaderProgramFromSource(
                "#version 330 core\n" +
                        "layout(location=0) in vec2 aPos;\n" +
                        "out vec2 UV;\n" +
                        "uniform mat4 projection;\n" +
                        "uniform vec2 pos, size;\n" +
                        "void main() { gl_Position=projection*vec4(pos+aPos*size,0,1); UV=aPos; }\n",
                "#version 330 core\n" +
                        "in vec2 UV;\n" +
                        "out vec4 FragColor;\n" +
                        "uniform sampler2D tex;\n" +
                        "void main() { FragColor = texture(tex, UV); }\n"
        );
        mmLocProj = glGetUniformLocation(mmShader, "projection");
        mmLocPos  = glGetUniformLocation(mmShader, "pos");
        mmLocSize = glGetUniformLocation(mmShader, "size");

        // Quad unit
        float[] v = {0,0, 1,0, 1,1, 0,1};
        int[]   i = {0,1,2, 2,3,0};

        vao = buildVao(v, i);
        mmVao = buildVao(v, i);

        // Minimap texture (vazia inicialmente)
        mmTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, mmTexture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    }

    // -------------------------------------------------------------------------
    // Render principal
    // -------------------------------------------------------------------------

    public void render(Player player, boolean isDead, float deathTimer,
                       WorldMap worldMap, CreatureManager creatures) {
        pulseTimer += 0.016f;

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glUseProgram(shader);
        Matrix4f ortho = new Matrix4f().ortho(0, screenW, screenH, 0, -1, 1);
        FloatBuffer fb = BufferUtils.createFloatBuffer(16);
        glUniformMatrix4fv(locProj, false, ortho.get(fb));
        glBindVertexArray(vao);

        if (isDead) {
            renderDeathScreen(deathTimer);
        } else {
            renderBars(player);
            renderTooltip(player, creatures);
        }

        glBindVertexArray(0);

        renderMinimap(ortho, fb, player, worldMap, creatures);

        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }

    // -------------------------------------------------------------------------
    // Barras de status
    // -------------------------------------------------------------------------

    private void renderBars(Player player) {
        float bottomY    = screenH - 16f;
        float hpY        = bottomY - BAR_H;
        float weightY    = hpY    - BAR_H - BAR_GAP;
        float xpY        = weightY - BAR_H - BAR_GAP;

        boolean inCombat = player.getStats().isInCombat();
        float   pulse    = inCombat ? (float)(0.5f+0.5f*Math.sin(pulseTimer*6f)) : 0f;

        // HP
        float hpRatio = (float) player.getStats().getHp() / player.getStats().getMaxHp();
        drawBar(BAR_X, hpY, BAR_W, BAR_H,
                hpRatio,
                1f - hpRatio + pulse*0.3f, hpRatio*0.8f*(1f-pulse*0.5f), 0f);

        // Indicador de combate
        if (inCombat) rect(BAR_X + BAR_W + 6f, hpY + 3f, 10f, 10f, 0.9f,0.1f,0.1f,0.9f);

        // Peso
        float wRatio = player.getInventory().getCurrentWeight()
                / player.getInventory().getMaxWeight();
        drawBar(BAR_X, weightY, BAR_W, BAR_H, wRatio,
                wRatio*0.7f, 0.5f-wRatio*0.3f, 0.1f);

        // XP
        float xpRatio = player.getExperience().getXpRatio();
        drawBar(BAR_X, xpY, BAR_W, BAR_H, xpRatio,
                0.2f, 0.5f, 0.9f);

        // Nível — caixinha ao lado da barra de XP
        int lvl = player.getExperience().getLevel();
        float lvlX = BAR_X + BAR_W + 6f;
        float lvlY = xpY;
        float lvlS = BAR_H * 2f + BAR_GAP; // altura de 2 barras
        rect(lvlX, lvlY - lvlS + BAR_H, 28f, lvlS, 0.15f,0.15f,0.25f,0.9f);
        rect(lvlX, lvlY - lvlS + BAR_H, 28f, 1,    0.5f,0.3f,0.9f,1f);
        rect(lvlX, lvlY + BAR_H,        28f, 1,    0.5f,0.3f,0.9f,1f);
        rect(lvlX, lvlY - lvlS + BAR_H, 1,   lvlS, 0.5f,0.3f,0.9f,1f);
        rect(lvlX+27f, lvlY-lvlS+BAR_H, 1,   lvlS, 0.5f,0.3f,0.9f,1f);
        // Número do nível como barra preenchida (representação visual)
        float lvlFill = Math.min(1f, lvl / 20f);
        rect(lvlX+2f, lvlY+2f, 24f, BAR_H-4f, 0.1f,0.1f,0.15f,1f);
        rect(lvlX+2f, lvlY+2f, 24f*lvlFill, BAR_H-4f, 0.5f,0.3f,0.9f,1f);
    }

    // -------------------------------------------------------------------------
    // Tooltip de criatura
    // -------------------------------------------------------------------------

    private void renderTooltip(Player player, CreatureManager creatures) {
        // Tooltip simples: mostra nome + HP da criatura mais próxima do mouse
        // A conversão mouse→mundo é feita na Camera; aqui apenas mostramos
        // a criatura mais próxima do player dentro do raio de visão
        Creature nearest = null;
        float minDist = 6f;
        for (Creature c : creatures.getCreatures()) {
            if (c.isDead()) continue;
            float d = c.distanceTo(player.getPosition());
            if (d < minDist) { nearest = c; minDist = d; }
        }

        if (nearest == null) return;

        float tx = mouseX + 12f;
        float ty = mouseY - 40f;

        // Clamp na tela
        if (tx + 140f > screenW) tx = mouseX - 150f;
        if (ty < 0) ty = mouseY + 10f;

        // Fundo
        rect(tx, ty, 140f, 36f, 0.08f,0.06f,0.12f,0.92f);
        rect(tx, ty, 140f, 1,   0.5f,0.3f,0.8f,1f);
        rect(tx, ty+35f, 140f, 1, 0.5f,0.3f,0.8f,1f);
        rect(tx, ty, 1, 36f, 0.5f,0.3f,0.8f,1f);
        rect(tx+139f, ty, 1, 36f, 0.5f,0.3f,0.8f,1f);

        // HP bar da criatura no tooltip
        float hpR = (float) nearest.getHp() / nearest.getMaxHp();
        rect(tx+4f, ty+20f, 132f, 10f, 0.1f,0.1f,0.1f,1f);
        rect(tx+4f, ty+20f, 132f*hpR, 10f, 1f-hpR, hpR*0.8f, 0f, 1f);
    }

    // -------------------------------------------------------------------------
    // Tela de morte
    // -------------------------------------------------------------------------

    private void renderDeathScreen(float deathTimer) {
        rect(0, 0, screenW, screenH, 0f,0f,0f,0.6f);

        float bw=300f, bh=80f;
        float bx=(screenW-bw)/2f, by=(screenH-bh)/2f;
        rect(bx, by, bw, bh,   0.5f,0f,0f,0.9f);
        rect(bx, by,     bw,2, 0.9f,0.2f,0.2f,1f);
        rect(bx, by+bh-2,bw,2, 0.9f,0.2f,0.2f,1f);
        rect(bx, by,     2,bh, 0.9f,0.2f,0.2f,1f);
        rect(bx+bw-2,by, 2,bh, 0.9f,0.2f,0.2f,1f);

        float progress = Math.max(0, 1f - (deathTimer / 5.0f));
        float barY = by + bh + 10f;
        rect(bx, barY, bw,           12f, 0.1f,0.1f,0.1f,0.9f);
        rect(bx, barY, bw * progress,12f, 0.8f,0.3f,0.1f,1.0f);
    }

    // -------------------------------------------------------------------------
    // Minimap
    // -------------------------------------------------------------------------

    private void renderMinimap(Matrix4f ortho, FloatBuffer fb,
                               Player player, WorldMap worldMap,
                               CreatureManager creatures) {
        float mmX = screenW - MM_SIZE - MM_MARGIN;
        float mmY = MM_MARGIN;

        // Regenera textura do mapa se necessário
        if (minimapDirty) {
            buildMinimapTexture(worldMap);
            minimapDirty = false;
        }

        // Desenha textura do mapa
        glUseProgram(mmShader);
        glUniformMatrix4fv(mmLocProj, false, ortho.get(fb));
        glUniform2f(mmLocPos,  mmX, mmY);
        glUniform2f(mmLocSize, MM_SIZE, MM_SIZE);
        glBindTexture(GL_TEXTURE_2D, mmTexture);
        glBindVertexArray(mmVao);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);

        // Borda do minimap
        glUseProgram(shader);
        glUniformMatrix4fv(locProj, false, ortho.get(fb));
        glBindVertexArray(vao);
        rect(mmX-MM_BORDER, mmY-MM_BORDER, MM_SIZE+MM_BORDER*2, MM_BORDER,         0.5f,0.3f,0.8f,1f);
        rect(mmX-MM_BORDER, mmY+MM_SIZE,   MM_SIZE+MM_BORDER*2, MM_BORDER,         0.5f,0.3f,0.8f,1f);
        rect(mmX-MM_BORDER, mmY-MM_BORDER, MM_BORDER, MM_SIZE+MM_BORDER*2,         0.5f,0.3f,0.8f,1f);
        rect(mmX+MM_SIZE,   mmY-MM_BORDER, MM_BORDER, MM_SIZE+MM_BORDER*2,         0.5f,0.3f,0.8f,1f);

        // Player no minimap (ponto branco)
        float px = mmX + (player.getPosition().x / worldMap.getWidth())  * MM_SIZE;
        float py = mmY + (player.getPosition().z / worldMap.getHeight()) * MM_SIZE;
        rect(px - 3f, py - 3f, 6f, 6f, 1f,1f,1f,1f);

        // Criaturas no minimap (pontos coloridos por comportamento)
        for (Creature c : creatures.getCreatures()) {
            if (c.isDead()) continue;
            float cx = mmX + (c.getPosition().x / worldMap.getWidth())  * MM_SIZE;
            float cy = mmY + (c.getPosition().z / worldMap.getHeight()) * MM_SIZE;
            float r, g, b;
            switch (c.getType().behavior) {
                case HOSTILE -> { r=0.9f; g=0.1f; b=0.1f; }
                case NEUTRAL -> { r=0.9f; g=0.8f; b=0.1f; }
                default      -> { r=0.1f; g=0.9f; b=0.3f; }
            }
            rect(cx - 2f, cy - 2f, 4f, 4f, r, g, b, 0.9f);
        }

        glBindVertexArray(0);
    }

    private void buildMinimapTexture(WorldMap worldMap) {
        int w = worldMap.getWidth();
        int h = worldMap.getHeight();
        int[] pixels = new int[w * h];

        for (int x = 0; x < w; x++) {
            for (int z = 0; z < h; z++) {
                int color = switch (worldMap.getTile(x, z)) {
                    case GRASS -> 0xFF3A7A3A;
                    case DIRT  -> 0xFF7A5A30;
                    case STONE -> 0xFF6A6A6A;
                    case WATER -> 0xFF2A5A9A;
                };
                // OpenGL espera RGBA, mas IntBuffer é ABGR em little-endian
                // Convertemos ARGB → ABGR
                int a = (color >> 24) & 0xFF;
                int r = (color >> 16) & 0xFF;
                int g = (color >>  8) & 0xFF;
                int b =  color        & 0xFF;
                pixels[z * w + x] = (a << 24) | (b << 16) | (g << 8) | r;
            }
        }

        java.nio.IntBuffer buf = BufferUtils.createIntBuffer(w * h);
        buf.put(pixels).flip();

        glBindTexture(GL_TEXTURE_2D, mmTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA,
                GL_UNSIGNED_BYTE, buf);
    }

    public void markMinimapDirty() { minimapDirty = true; }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void drawBar(float x, float y, float w, float h, float ratio,
                         float fr, float fg, float fb2) {
        ratio = Math.max(0, Math.min(ratio, 1f));
        rect(x, y, w,       h, 0.08f,0.08f,0.08f,0.85f);
        rect(x, y, w*ratio, h, fr, fg, fb2, 1.0f);
        rect(x,     y,     w, 1, 0.6f,0.6f,0.6f,1f);
        rect(x,     y+h-1, w, 1, 0.6f,0.6f,0.6f,1f);
        rect(x,     y,     1, h, 0.6f,0.6f,0.6f,1f);
        rect(x+w-1, y,     1, h, 0.6f,0.6f,0.6f,1f);
    }

    private void rect(float x, float y, float w, float h,
                      float r, float g, float b, float a) {
        glUniform2f(locPos,   x, y);
        glUniform2f(locSize,  w, h);
        glUniform4f(locColor, r, g, b, a);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
    }

    private int buildVao(float[] verts, int[] idx) {
        int v = glGenVertexArrays();
        int vb = glGenBuffers();
        int eb = glGenBuffers();
        glBindVertexArray(v);
        FloatBuffer fb = BufferUtils.createFloatBuffer(verts.length);
        fb.put(verts).flip();
        glBindBuffer(GL_ARRAY_BUFFER, vb);
        glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);
        IntBuffer ib = BufferUtils.createIntBuffer(idx.length);
        ib.put(idx).flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eb);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2*Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glBindVertexArray(0);
        return v;
    }

    public void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteVertexArrays(mmVao);
        glDeleteTextures(mmTexture);
        glDeleteProgram(shader);
        glDeleteProgram(mmShader);
    }
}