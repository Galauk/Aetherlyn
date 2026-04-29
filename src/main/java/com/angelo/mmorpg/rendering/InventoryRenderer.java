package com.angelo.mmorpg.rendering;

import com.angelo.mmorpg.core.ResourceLoader;
import com.angelo.mmorpg.entity.Inventory;
import com.angelo.mmorpg.entity.Item;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Renderiza a janela de inventário flutuante estilo Ultima Online.
 * - Janela arrastável pelo título
 * - Itens posicionados livremente dentro da área
 * - Barra de peso na parte inferior
 */
public class InventoryRenderer {

    // Dimensões da janela
    private static final int WIN_W      = Inventory.AREA_W + 16;  // margem lateral
    private static final int WIN_H      = Inventory.AREA_H + 52;  // título + área + barra peso
    private static final int TITLE_H    = 20;
    private static final int ICON_SIZE  = Inventory.ICON_SIZE;

    // Spritesheet terrain.png para ícones dos itens
    private static final float SHEET_W   = 672f;
    private static final float SHEET_H   = 736f;
    private static final float TILE_PX   = 32f;
    private static final float TILE_U    = TILE_PX / SHEET_W;
    private static final float TILE_V    = TILE_PX / SHEET_H;
    private static final float UV_MARGIN = 0.5f / SHEET_W;

    private final int screenW;
    private final int screenH;

    // Posição da janela na tela
    private float winX;
    private float winY;

    // Estado de drag da janela
    private boolean dragging    = false;
    private float   dragOffsetX = 0;
    private float   dragOffsetY = 0;

    // Estado de drag de item
    private int   draggingItemIdx = -1;
    private float dragItemX, dragItemY;

    private boolean visible = false;

    // Shaders e buffers
    private int shaderFlat;    // para retângulos coloridos
    private int shaderSprite;  // para ícones do spritesheet
    private int terrainTexture;

    private int quadVao, quadVbo, quadEbo;

    // Uniform locations — flat
    private int locFlatProj, locFlatColor, locFlatPos, locFlatSize;
    // Uniform locations — sprite
    private int locSprProj, locSprPos, locSprSize, locSprUVOffset, locSprUVScale;

    public InventoryRenderer(int screenW, int screenH) {
        this.screenW = screenW;
        this.screenH = screenH;
        this.winX    = screenW - WIN_W - 20;
        this.winY    = 60;
    }

    public void init() {
        shaderFlat   = buildFlatShader();
        shaderSprite = buildSpriteShader();
        terrainTexture = ResourceLoader.loadTexture("/assets/terrain.png");
        buildQuad();
        cacheUniforms();
    }

    private void buildQuad() {
        float[] v = { 0,0, 1,0, 1,1, 0,1 };
        int[]   i = { 0,1,2, 2,3,0 };

        quadVao = glGenVertexArrays();
        quadVbo = glGenBuffers();
        quadEbo = glGenBuffers();

        glBindVertexArray(quadVao);

        FloatBuffer fb = BufferUtils.createFloatBuffer(v.length);
        fb.put(v).flip();
        glBindBuffer(GL_ARRAY_BUFFER, quadVbo);
        glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);

        IntBuffer ib = BufferUtils.createIntBuffer(i.length);
        ib.put(i).flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, quadEbo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glBindVertexArray(0);
    }

    private void cacheUniforms() {
        locFlatProj  = glGetUniformLocation(shaderFlat, "projection");
        locFlatColor = glGetUniformLocation(shaderFlat, "color");
        locFlatPos   = glGetUniformLocation(shaderFlat, "pos");
        locFlatSize  = glGetUniformLocation(shaderFlat, "size");

        locSprProj     = glGetUniformLocation(shaderSprite, "projection");
        locSprPos      = glGetUniformLocation(shaderSprite, "pos");
        locSprSize     = glGetUniformLocation(shaderSprite, "size");
        locSprUVOffset = glGetUniformLocation(shaderSprite, "uvOffset");
        locSprUVScale  = glGetUniformLocation(shaderSprite, "uvScale");
    }

    public void render(Inventory inventory) {
        if (!visible) return;

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        Matrix4f ortho = new Matrix4f().ortho(0, screenW, screenH, 0, -1, 1);
        FloatBuffer fb = BufferUtils.createFloatBuffer(16);

        // --- Fundo da janela ---
        drawRect(ortho, fb, winX, winY, WIN_W, WIN_H, 0.12f, 0.10f, 0.15f, 0.92f);

        // --- Barra de título ---
        drawRect(ortho, fb, winX, winY, WIN_W, TITLE_H, 0.25f, 0.10f, 0.40f, 1.0f);

        // --- Borda ---
        drawRect(ortho, fb, winX,           winY,           WIN_W, 1,     0.5f, 0.3f, 0.8f, 1f);
        drawRect(ortho, fb, winX,           winY+WIN_H-1,   WIN_W, 1,     0.5f, 0.3f, 0.8f, 1f);
        drawRect(ortho, fb, winX,           winY,           1,     WIN_H, 0.5f, 0.3f, 0.8f, 1f);
        drawRect(ortho, fb, winX+WIN_W-1,   winY,           1,     WIN_H, 0.5f, 0.3f, 0.8f, 1f);

        // --- Área dos itens (fundo levemente diferente) ---
        float areaX = winX + 8;
        float areaY = winY + TITLE_H + 4;
        drawRect(ortho, fb, areaX, areaY, Inventory.AREA_W, Inventory.AREA_H, 0.08f, 0.06f, 0.10f, 0.95f);

        // --- Ícones dos itens ---
        glUseProgram(shaderSprite);
        glUniformMatrix4fv(locSprProj, false, ortho.get(fb));
        glUniform2f(locSprUVScale, TILE_U - UV_MARGIN * 2, TILE_V - UV_MARGIN * 2);
        glBindTexture(GL_TEXTURE_2D, terrainTexture);
        glBindVertexArray(quadVao);

        for (Item item : inventory.getItems()) {
            float ix = areaX + item.invX;
            float iy = areaY + item.invY;
            float u0 = item.def.spriteCol * TILE_U + UV_MARGIN;
            float v0 = item.def.spriteRow * TILE_V + UV_MARGIN;

            glUniform2f(locSprPos, ix, iy);
            glUniform2f(locSprSize, ICON_SIZE, ICON_SIZE);
            glUniform2f(locSprUVOffset, u0, v0);
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
        }

        glBindVertexArray(0);

        // --- Barra de peso ---
        float barY     = winY + TITLE_H + 4 + Inventory.AREA_H + 4;
        float barX     = winX + 8;
        float barW     = Inventory.AREA_W;
        float barH     = 14;
        float ratio    = Math.min(inventory.getCurrentWeight() / inventory.getMaxWeight(), 1f);

        drawRect(ortho, fb, barX, barY, barW, barH, 0.08f, 0.06f, 0.10f, 1f);
        // Fill — cor muda de verde para vermelho conforme peso aumenta
        drawRect(ortho, fb, barX, barY, barW * ratio, barH,
                ratio, 1f - ratio, 0f, 1f);
        // Borda da barra
        drawRect(ortho, fb, barX, barY, barW, 1,    0.5f,0.3f,0.8f,1f);
        drawRect(ortho, fb, barX, barY+barH-1, barW, 1, 0.5f,0.3f,0.8f,1f);

        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }

    private void drawRect(Matrix4f ortho, FloatBuffer fb,
                          float x, float y, float w, float h,
                          float r, float g, float b, float a) {
        glUseProgram(shaderFlat);
        glUniformMatrix4fv(locFlatProj, false, ortho.get(fb));
        glUniform4f(locFlatColor, r, g, b, a);
        glUniform2f(locFlatPos, x, y);
        glUniform2f(locFlatSize, w, h);
        glBindVertexArray(quadVao);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    // -------------------------------------------------------------------------
    // Input — drag da janela e dos itens
    // -------------------------------------------------------------------------

    public void onMouseButton(float mx, float my, int button, int action, Inventory inventory) {
        boolean press   = (action == 1);
        boolean release = (action == 0);

        if (!visible) return;

        if (button == 0) { // botão esquerdo
            if (press) {
                // Inicia drag de item se clicou em um item
                draggingItemIdx = getItemAt(mx, my, inventory);
                if (draggingItemIdx >= 0) {
                    Item item = inventory.getItems().get(draggingItemIdx);
                    dragItemX = mx - (winX + 8 + item.invX);
                    dragItemY = my - (winY + TITLE_H + 4 + item.invY);
                }
                // Inicia drag da janela se clicou no título OU na área vazia (sem item)
                else if (isOnWindow(mx, my)) {
                    dragging    = true;
                    dragOffsetX = mx - winX;
                    dragOffsetY = my - winY;
                }
            }
            if (release) {
                dragging = false;
                if (draggingItemIdx >= 0) {
                    Item item = inventory.getItems().get(draggingItemIdx);
                    float newX = mx - dragItemX - (winX + 8);
                    float newY = my - dragItemY - (winY + TITLE_H + 4);
                    newX = Math.max(0, Math.min(newX, Inventory.AREA_W - ICON_SIZE));
                    newY = Math.max(0, Math.min(newY, Inventory.AREA_H - ICON_SIZE));
                    item.invX = newX;
                    item.invY = newY;
                    draggingItemIdx = -1;
                }
            }
        }
    }

    public void onMouseMove(float mx, float my, Inventory inventory) {
        if (!visible) return;
        if (dragging) {
            winX = mx - dragOffsetX;
            winY = my - dragOffsetY;
        }
    }

    private boolean isOnTitle(float mx, float my) {
        return mx >= winX && mx <= winX + WIN_W
                && my >= winY && my <= winY + TITLE_H;
    }

    private boolean isOnWindow(float mx, float my) {
        return mx >= winX && mx <= winX + WIN_W
                && my >= winY && my <= winY + WIN_H;
    }

    private int getItemAt(float mx, float my, Inventory inventory) {
        float areaX = winX + 8;
        float areaY = winY + TITLE_H + 4;
        for (int i = 0; i < inventory.getItems().size(); i++) {
            Item item = inventory.getItems().get(i);
            float ix = areaX + item.invX;
            float iy = areaY + item.invY;
            if (mx >= ix && mx <= ix + ICON_SIZE && my >= iy && my <= iy + ICON_SIZE) {
                return i;
            }
        }
        return -1;
    }

    public void toggle()          { visible = !visible; }
    public boolean isVisible()    { return visible; }

    public void cleanup() {
        glDeleteVertexArrays(quadVao);
        glDeleteBuffers(quadVbo);
        glDeleteBuffers(quadEbo);
        glDeleteTextures(terrainTexture);
        glDeleteProgram(shaderFlat);
        glDeleteProgram(shaderSprite);
    }

    // -------------------------------------------------------------------------
    // Shaders inline
    // -------------------------------------------------------------------------

    private static int buildFlatShader() {
        return ResourceLoader.createShaderProgramFromSource(
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
    }

    private static int buildSpriteShader() {
        return ResourceLoader.createShaderProgramFromSource(
                "#version 330 core\n" +
                        "layout(location=0) in vec2 aPos;\n" +
                        "out vec2 TexCoord;\n" +
                        "uniform mat4 projection;\n" +
                        "uniform vec2 pos;\n" +
                        "uniform vec2 size;\n" +
                        "uniform vec2 uvOffset;\n" +
                        "uniform vec2 uvScale;\n" +
                        "void main() {\n" +
                        "    gl_Position = projection * vec4(pos + aPos * size, 0.0, 1.0);\n" +
                        "    TexCoord = uvOffset + aPos * uvScale;\n" +
                        "}\n",

                "#version 330 core\n" +
                        "in vec2 TexCoord;\n" +
                        "out vec4 FragColor;\n" +
                        "uniform sampler2D tex;\n" +
                        "void main() {\n" +
                        "    vec4 c = texture(tex, TexCoord);\n" +
                        "    if (c.a < 0.1) discard;\n" +
                        "    FragColor = c;\n" +
                        "}\n"
        );
    }
}