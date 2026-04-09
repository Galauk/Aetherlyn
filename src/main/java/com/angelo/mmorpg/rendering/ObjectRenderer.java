package com.angelo.mmorpg.rendering;

import com.angelo.mmorpg.camera.Camera;
import com.angelo.mmorpg.core.ResourceLoader;
import com.angelo.mmorpg.world.StaticObject;
import com.angelo.mmorpg.world.WorldMap;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Renderiza objetos estáticos como billboard sprites usando o terrain.png.
 * Spritesheet: 672x736px, tiles de 32x32px.
 */
public class ObjectRenderer {

    private static final float SHEET_W  = 672.0f;
    private static final float SHEET_H  = 736.0f;
    private static final float TILE_PX  = 32.0f;
    private static final float TILE_U   = TILE_PX / SHEET_W;
    private static final float TILE_V   = TILE_PX / SHEET_H;
    private static final float UV_MARGIN = 0.5f / SHEET_W;

    // Tamanho do sprite no mundo
    private static final float SPRITE_W = 0.9f;
    private static final float SPRITE_H = 0.9f;

    private int shaderProgram;
    private int terrainTexture;
    private int vao, vbo, ebo;

    // Quad base — UVs serão atualizadas por objeto via uniform
    private static final float[] VERTICES = {
            -SPRITE_W / 2, 0.0f,      0.0f,  0.0f, 1.0f,
            SPRITE_W / 2, 0.0f,      0.0f,  1.0f, 1.0f,
            SPRITE_W / 2, SPRITE_H,  0.0f,  1.0f, 0.0f,
            -SPRITE_W / 2, SPRITE_H,  0.0f,  0.0f, 0.0f,
    };

    private static final int[] INDICES = { 0, 1, 2, 2, 3, 0 };

    // Uniform locations
    private int locModel, locView, locProjection, locUVOffset, locUVScale;

    public void init() {
        shaderProgram  = createShaderProgram();
        terrainTexture = ResourceLoader.loadTexture("/assets/terrain.png");

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

        locModel      = glGetUniformLocation(shaderProgram, "model");
        locView       = glGetUniformLocation(shaderProgram, "view");
        locProjection = glGetUniformLocation(shaderProgram, "projection");
        locUVOffset   = glGetUniformLocation(shaderProgram, "uvOffset");
        locUVScale    = glGetUniformLocation(shaderProgram, "uvScale");
    }

    public void render(Camera camera, WorldMap worldMap) {
        List<StaticObject> objects = worldMap.getObjects();
        if (objects.isEmpty()) return;

        glUseProgram(shaderProgram);

        Matrix4f view       = camera.getViewMatrix();
        Matrix4f projection = camera.getProjectionMatrix();

        FloatBuffer fb = BufferUtils.createFloatBuffer(16);
        glUniformMatrix4fv(locView,       false, view.get(fb));
        glUniformMatrix4fv(locProjection, false, projection.get(fb));

        // Eixo right da câmera para billboard
        Vector3f right = new Vector3f(view.m00(), view.m10(), view.m20());
        Vector3f up    = new Vector3f(0f, 1f, 0f);

        // UV scale igual para todos (um tile 32px)
        glUniform2f(locUVScale, TILE_U - UV_MARGIN * 2, TILE_V - UV_MARGIN * 2);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glBindTexture(GL_TEXTURE_2D, terrainTexture);
        glBindVertexArray(vao);

        for (StaticObject obj : objects) {
            // UV offset para o tile correto do spritesheet
            float u0 = obj.type.spriteCol * TILE_U + UV_MARGIN;
            float v0 = obj.type.spriteRow * TILE_V + UV_MARGIN;
            glUniform2f(locUVOffset, u0, v0);

            // Billboard matrix
            Matrix4f model = new Matrix4f(
                    right.x,   right.y,   right.z,   0,
                    up.x,      up.y,      up.z,      0,
                    0,         0,         1,         0,
                    obj.worldX, 0f,       obj.worldZ, 1
            );
            glUniformMatrix4fv(locModel, false, model.get(fb));

            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
        }

        glBindVertexArray(0);
        glDisable(GL_BLEND);
    }

    public void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        glDeleteTextures(terrainTexture);
        glDeleteProgram(shaderProgram);
    }

    /**
     * Shader customizado com uvOffset + uvScale para selecionar
     * o tile correto do spritesheet por objeto.
     */
    private static int createShaderProgram() {
        String vert =
                "#version 330 core\n" +
                        "layout(location=0) in vec3 aPos;\n" +
                        "layout(location=1) in vec2 aTexCoord;\n" +
                        "out vec2 TexCoord;\n" +
                        "uniform mat4 model;\n" +
                        "uniform mat4 view;\n" +
                        "uniform mat4 projection;\n" +
                        "uniform vec2 uvOffset;\n" +
                        "uniform vec2 uvScale;\n" +
                        "void main() {\n" +
                        "    gl_Position = projection * view * model * vec4(aPos, 1.0);\n" +
                        "    TexCoord = uvOffset + aTexCoord * uvScale;\n" +
                        "}\n";

        String frag =
                "#version 330 core\n" +
                        "in vec2 TexCoord;\n" +
                        "out vec4 FragColor;\n" +
                        "uniform sampler2D texture0;\n" +
                        "void main() {\n" +
                        "    vec4 color = texture(texture0, TexCoord);\n" +
                        "    if (color.a < 0.1) discard;\n" +
                        "    FragColor = color;\n" +
                        "}\n";

        return ResourceLoader.createShaderProgramFromSource(vert, frag);
    }
}