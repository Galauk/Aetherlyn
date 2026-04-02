package com.angelo.mmorpg.rendering;

import com.angelo.mmorpg.camera.Camera;
import com.angelo.mmorpg.core.ResourceLoader;
import com.angelo.mmorpg.world.WorldMap;
import com.angelo.mmorpg.world.WorldMap.TileType;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Renderiza o terreno como uma grade de tiles planos no Y=0.
 * Cada tile é um quad texturizado com a região correspondente do spritesheet terrain.png.
 *
 * Spritesheet: 672x736px, tiles de 64x64px, grade 10x11.
 */
public class TerrainRenderer {

    // Dimensões do spritesheet
    private static final float SHEET_W    = 672.0f;
    private static final float SHEET_H    = 736.0f;
    private static final float TILE_PX    = 64.0f;  // pixels por tile
    private static final float TILE_U     = TILE_PX / SHEET_W;  // largura UV de um tile
    private static final float TILE_V     = TILE_PX / SHEET_H;  // altura UV de um tile

    private int shaderProgram;
    private int terrainTexture;

    // Um VAO/VBO por tipo de tile (evita re-bind de textura por tile)
    // Simples: usamos um único mesh dinâmico por tipo
    private int vao, vbo, ebo;
    private int indexCount;

    public void init(WorldMap worldMap) {
        shaderProgram  = ResourceLoader.createShaderProgram("/shaders/vertex.glsl", "/shaders/fragment.glsl");
        terrainTexture = ResourceLoader.loadTexture("/assets/terrain.png");

        buildMesh(worldMap);

        glEnable(GL_DEPTH_TEST);
        glClearColor(0.15f, 0.2f, 0.25f, 1.0f);
    }

    /**
     * Constrói um único mesh com todos os tiles do mapa.
     * Cada tile é um quad (2 triângulos) com UVs apontando para a região correta do spritesheet.
     */
    private void buildMesh(WorldMap worldMap) {
        int w = worldMap.getWidth();
        int h = worldMap.getHeight();

        // 4 vértices por tile × (3 pos + 2 uv) floats
        FloatBuffer vertices = BufferUtils.createFloatBuffer(w * h * 4 * 5);
        // 6 índices por tile (2 triângulos)
        IntBuffer   indices  = BufferUtils.createIntBuffer(w * h * 6);

        int vertexOffset = 0;

        for (int x = 0; x < w; x++) {
            for (int z = 0; z < h; z++) {
                TileType tile = worldMap.getTile(x, z);

                // UV da região do tile no spritesheet
                float u0 = tile.spriteCol * TILE_U;
                float v0 = tile.spriteRow * TILE_V;
                float u1 = u0 + TILE_U;
                float v1 = v0 + TILE_V;

                // Posições dos 4 cantos do quad no Y=0
                float x0 = x;
                float x1 = x + 1.0f;
                float z0 = z;
                float z1 = z + 1.0f;

                // v0 — canto traseiro esquerdo
                vertices.put(x0).put(0.0f).put(z0).put(u0).put(v0);
                // v1 — canto traseiro direito
                vertices.put(x1).put(0.0f).put(z0).put(u1).put(v0);
                // v2 — canto frontal direito
                vertices.put(x1).put(0.0f).put(z1).put(u1).put(v1);
                // v3 — canto frontal esquerdo
                vertices.put(x0).put(0.0f).put(z1).put(u0).put(v1);

                // 2 triângulos: (0,1,2) e (2,3,0)
                int i = vertexOffset;
                indices.put(i).put(i+1).put(i+2);
                indices.put(i+2).put(i+3).put(i);

                vertexOffset += 4;
            }
        }

        vertices.flip();
        indices.flip();
        indexCount = indices.limit();

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        // Atributo 0: posição (xyz)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // Atributo 1: texcoord (uv)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
    }

    public void render(Camera camera) {
        glUseProgram(shaderProgram);

        // Model = identidade (terreno não se move)
        setUniformMatrix("model",      new Matrix4f().identity());
        setUniformMatrix("view",       camera.getViewMatrix());
        setUniformMatrix("projection", camera.getProjectionMatrix());

        glBindTexture(GL_TEXTURE_2D, terrainTexture);
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
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
        glDeleteTextures(terrainTexture);
        glDeleteProgram(shaderProgram);
    }
}