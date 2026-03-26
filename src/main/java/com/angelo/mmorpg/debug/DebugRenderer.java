package com.angelo.mmorpg.debug;

import com.angelo.mmorpg.core.ResourceLoader;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBTruetype.*;

public class DebugRenderer {

    private static final int   BITMAP_W   = 512;
    private static final int   BITMAP_H   = 512;
    private static final float FONT_SIZE  = 16.0f;
    private static final int   FIRST_CHAR = 32;
    private static final int   CHAR_COUNT = 96;

    private final int screenW;
    private final int screenH;

    private int fontTexture;
    private STBTTBakedChar.Buffer charData;

    private int shaderProgram;
    private int vao, vbo;

    private static final String VERT_SRC =
            "#version 330 core\n" +
                    "layout(location=0) in vec4 vertex;\n" +
                    "out vec2 TexCoord;\n" +
                    "uniform mat4 projection;\n" +
                    "void main() {\n" +
                    "    gl_Position = projection * vec4(vertex.xy, 0.0, 1.0);\n" +
                    "    TexCoord = vertex.zw;\n" +
                    "}\n";

    private static final String FRAG_SRC =
            "#version 330 core\n" +
                    "in vec2 TexCoord;\n" +
                    "out vec4 FragColor;\n" +
                    "uniform sampler2D fontTex;\n" +
                    "uniform vec3 textColor;\n" +
                    "void main() {\n" +
                    "    float alpha = texture(fontTex, TexCoord).r;\n" +
                    "    FragColor = vec4(textColor, alpha);\n" +
                    "}\n";

    public DebugRenderer(int screenW, int screenH) {
        this.screenW = screenW;
        this.screenH = screenH;
    }

    public void init() {
        shaderProgram = ResourceLoader.createShaderProgramFromSource(VERT_SRC, FRAG_SRC);
        loadFont();
        setupMesh();
    }

    private void loadFont() {
        byte[] fontBytes;
        try (InputStream is = DebugRenderer.class.getResourceAsStream("assets/font.ttf")) {
            if (is == null)
                throw new RuntimeException(
                        "Fonte não encontrada: /assets/font.ttf\n" +
                                "Baixe uma fonte .ttf e coloque em src/main/resources/assets/font.ttf"
                );
            fontBytes = is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler fonte", e);
        }

        ByteBuffer fontBuf = BufferUtils.createByteBuffer(fontBytes.length);
        fontBuf.put(fontBytes).flip();

        ByteBuffer bitmap = BufferUtils.createByteBuffer(BITMAP_W * BITMAP_H);
        charData = STBTTBakedChar.malloc(CHAR_COUNT);
        stbtt_BakeFontBitmap(fontBuf, FONT_SIZE, bitmap, BITMAP_W, BITMAP_H, FIRST_CHAR, charData);

        fontTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, fontTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, BITMAP_W, BITMAP_H, 0, GL_RED, GL_UNSIGNED_BYTE, bitmap);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    }

    private void setupMesh() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, (long) 6 * 4 * Float.BYTES * 256, GL_DYNAMIC_DRAW);
        glVertexAttribPointer(0, 4, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glBindVertexArray(0);
    }

    public void render(DebugInfo info) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_DEPTH_TEST);

        glUseProgram(shaderProgram);

        Matrix4f ortho = new Matrix4f().ortho(0, screenW, screenH, 0, -1, 1);
        FloatBuffer fb = BufferUtils.createFloatBuffer(16);
        glUniformMatrix4fv(glGetUniformLocation(shaderProgram, "projection"), false, ortho.get(fb));
        glBindTexture(GL_TEXTURE_2D, fontTexture);

        // Título em verde
        glUniform3f(glGetUniformLocation(shaderProgram, "textColor"), 0.0f, 1.0f, 0.2f);
        renderLine("[DEBUG]", 16, 26);

        // Linhas de info
        String[] lines = info.lines();
        for (int i = 0; i < lines.length; i++) {
            renderLine(lines[i], 16, 46 + i * 20);
        }

        // Dica de atalho em cinza
        glUniform3f(glGetUniformLocation(shaderProgram, "textColor"), 0.5f, 0.5f, 0.5f);
        renderLine("Ctrl+G: grid", 16, 46 + lines.length * 20 + 8);

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    private void renderLine(String text, float startX, float startY) {
        FloatBuffer xb = BufferUtils.createFloatBuffer(1);
        FloatBuffer yb = BufferUtils.createFloatBuffer(1);
        xb.put(0, startX);
        yb.put(0, startY);

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        try (STBTTAlignedQuad q = STBTTAlignedQuad.malloc()) {
            for (char c : text.toCharArray()) {
                if (c < FIRST_CHAR || c >= FIRST_CHAR + CHAR_COUNT) {
                    xb.put(0, xb.get(0) + 8);
                    continue;
                }

                stbtt_GetBakedQuad(charData, BITMAP_W, BITMAP_H, c - FIRST_CHAR, xb, yb, q, true);

                float x0 = q.x0(), y0 = q.y0(), x1 = q.x1(), y1 = q.y1();
                float u0 = q.s0(), v0 = q.t0(), u1 = q.s1(), v1 = q.t1();

                float[] verts = {
                        x0, y0, u0, v0,
                        x1, y0, u1, v0,
                        x1, y1, u1, v1,
                        x0, y0, u0, v0,
                        x1, y1, u1, v1,
                        x0, y1, u0, v1,
                };

                FloatBuffer buf = BufferUtils.createFloatBuffer(verts.length);
                buf.put(verts).flip();
                glBufferSubData(GL_ARRAY_BUFFER, 0, buf);
                glDrawArrays(GL_TRIANGLES, 0, 6);
            }
        }

        glBindVertexArray(0);
    }

    public void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteTextures(fontTexture);
        glDeleteProgram(shaderProgram);
        charData.free();
    }
}