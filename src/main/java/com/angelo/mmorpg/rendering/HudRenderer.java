package com.angelo.mmorpg.rendering;

import com.angelo.mmorpg.core.ResourceLoader;
import com.angelo.mmorpg.entity.Creature;
import com.angelo.mmorpg.entity.CreatureManager;
import com.angelo.mmorpg.entity.Player;
import com.angelo.mmorpg.world.WorldMap;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBTruetype.*;

/**
 * HUD completo:
 *  - Barras de HP / Peso / XP com valores numéricos  (canto inferior esquerdo)
 *  - Nível do player
 *  - Painel de criaturas próximas (canto superior direito, abaixo do minimap)
 *  - Minimap quadrado                                 (canto superior direito)
 *  - Tela de morte com contador de respawn
 */
public class HudRenderer {

    // --- Layout barras ---
    private static final float BAR_X   = 16f;
    private static final float BAR_W   = 180f;
    private static final float BAR_H   = 16f;
    private static final float BAR_GAP = 5f;

    // --- Minimap ---
    private static final int MM_SIZE   = 150;
    private static final int MM_MARGIN = 12;
    private static final int MM_BORDER = 2;

    // --- Painel de criaturas ---
    private static final float CP_W        = 180f; // largura do painel
    private static final float CP_BAR_H    = 14f;  // altura da barra principal
    private static final float CP_BAR_H_SM = 10f;  // altura das barras menores
    private static final float CP_GAP      = 4f;
    private static final int   CP_MAX      = 3;    // máximo de criaturas exibidas
    private static final float CP_RADIUS   = 8f;   // raio de detecção

    // --- Fonte ---
    private static final int   BITMAP_W   = 512;
    private static final int   BITMAP_H   = 512;
    private static final float FONT_SIZE  = 13f;
    private static final int   FIRST_CHAR = 32;
    private static final int   CHAR_COUNT = 96;

    private final int screenW;
    private final int screenH;

    private int flatShader, textShader;
    private int flatVao,    textVao, textVbo;
    private int locFlatProj, locFlatPos, locFlatSize, locFlatColor;
    private int locTextProj, locTextColor;

    private int fontTexture;
    private STBTTBakedChar.Buffer charData;

    private int mmShader, mmVao;
    private int locMmProj, locMmPos, locMmSize;
    private int mmTexture;
    private boolean mmDirty = true;

    private float pulseTimer = 0f;

    public HudRenderer(int screenW, int screenH) {
        this.screenW = screenW;
        this.screenH = screenH;
    }

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------

    public void init() {
        float[] v = {0,0, 1,0, 1,1, 0,1};
        int[]   i = {0,1,2, 2,3,0};

        flatShader = ResourceLoader.createShaderProgramFromSource(
                "#version 330 core\n" +
                        "layout(location=0) in vec2 aPos;\n" +
                        "uniform mat4 projection; uniform vec2 pos,size;\n" +
                        "void main(){gl_Position=projection*vec4(pos+aPos*size,0,1);}\n",
                "#version 330 core\n out vec4 F; uniform vec4 color;\n void main(){F=color;}\n"
        );
        locFlatProj  = glGetUniformLocation(flatShader,"projection");
        locFlatPos   = glGetUniformLocation(flatShader,"pos");
        locFlatSize  = glGetUniformLocation(flatShader,"size");
        locFlatColor = glGetUniformLocation(flatShader,"color");
        flatVao = buildVao(v, i);

        textShader = ResourceLoader.createShaderProgramFromSource(
                "#version 330 core\n" +
                        "layout(location=0) in vec4 vertex;\n" +
                        "out vec2 UV;\n" +
                        "uniform mat4 projection;\n" +
                        "void main(){gl_Position=projection*vec4(vertex.xy,0,1);UV=vertex.zw;}\n",
                "#version 330 core\n in vec2 UV; out vec4 F;\n" +
                        "uniform sampler2D fontTex; uniform vec4 color;\n" +
                        "void main(){float a=texture(fontTex,UV).r; F=vec4(color.rgb,a*color.a);}\n"
        );
        locTextProj  = glGetUniformLocation(textShader,"projection");
        locTextColor = glGetUniformLocation(textShader,"color");
        textVao = glGenVertexArrays();
        textVbo = glGenBuffers();
        glBindVertexArray(textVao);
        glBindBuffer(GL_ARRAY_BUFFER, textVbo);
        glBufferData(GL_ARRAY_BUFFER, (long)6*4*Float.BYTES*256, GL_DYNAMIC_DRAW);
        glVertexAttribPointer(0,4,GL_FLOAT,false,4*Float.BYTES,0);
        glEnableVertexAttribArray(0);
        glBindVertexArray(0);
        loadFont();

        mmShader = ResourceLoader.createShaderProgramFromSource(
                "#version 330 core\n" +
                        "layout(location=0) in vec2 aPos; out vec2 UV;\n" +
                        "uniform mat4 projection; uniform vec2 pos,size;\n" +
                        "void main(){gl_Position=projection*vec4(pos+aPos*size,0,1);UV=aPos;}\n",
                "#version 330 core\n in vec2 UV; out vec4 F;\n" +
                        "uniform sampler2D tex;\n void main(){F=texture(tex,UV);}\n"
        );
        locMmProj = glGetUniformLocation(mmShader,"projection");
        locMmPos  = glGetUniformLocation(mmShader,"pos");
        locMmSize = glGetUniformLocation(mmShader,"size");
        mmVao = buildVao(v, i);

        mmTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, mmTexture);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
    }

    private void loadFont() {
        try (InputStream is = HudRenderer.class.getResourceAsStream("/assets/font.ttf")) {
            if (is == null) throw new RuntimeException("font.ttf not found");
            byte[] bytes = is.readAllBytes();
            ByteBuffer buf = BufferUtils.createByteBuffer(bytes.length);
            buf.put(bytes).flip();
            ByteBuffer bitmap = BufferUtils.createByteBuffer(BITMAP_W * BITMAP_H);
            charData = STBTTBakedChar.malloc(CHAR_COUNT);
            stbtt_BakeFontBitmap(buf, FONT_SIZE, bitmap, BITMAP_W, BITMAP_H, FIRST_CHAR, charData);
            fontTexture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, fontTexture);
            glTexImage2D(GL_TEXTURE_2D,0,GL_RED,BITMAP_W,BITMAP_H,0,GL_RED,GL_UNSIGNED_BYTE,bitmap);
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    // -------------------------------------------------------------------------
    // Render
    // -------------------------------------------------------------------------

    public void render(Player player, boolean isDead, float deathTimer,
                       WorldMap worldMap, CreatureManager creatures) {
        pulseTimer += 0.016f;

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        Matrix4f ortho = new Matrix4f().ortho(0, screenW, screenH, 0, -1, 1);
        FloatBuffer fb = BufferUtils.createFloatBuffer(16);

        if (isDead) {
            useFlatShader(ortho, fb);
            renderDeathScreen(deathTimer);
        } else {
            useFlatShader(ortho, fb);
            renderStatusBars(player);
            renderCreaturePanel(player, creatures, ortho, fb);
        }

        renderMinimap(ortho, fb, player, worldMap, creatures);

        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }

    // -------------------------------------------------------------------------
    // Barras de status
    // -------------------------------------------------------------------------

    private void renderStatusBars(Player player) {
        float bottomY = screenH - 16f;
        float hpY     = bottomY - BAR_H;
        float wY      = hpY    - BAR_H - BAR_GAP;
        float xpY     = wY     - BAR_H - BAR_GAP;

        boolean inCombat = player.getStats().isInCombat();
        float   pulse    = inCombat ? (float)(0.5f+0.5f*Math.sin(pulseTimer*6f)) : 0f;

        // HP
        float hpR = (float) player.getStats().getHp() / player.getStats().getMaxHp();
        drawBar(BAR_X, hpY, BAR_W, BAR_H, hpR,
                1f-hpR+pulse*0.3f, hpR*0.8f*(1f-pulse*0.5f), 0f);
        if (inCombat) rect(BAR_X+BAR_W+6f, hpY+3f, 10f,10f, 0.9f,0.1f,0.1f,0.9f);
        drawText(player.getStats().getHp()+"/"+player.getStats().getMaxHp(),
                BAR_X+4f, hpY+BAR_H-2f, 1f,1f,1f,1f);

        // Peso
        float wR = player.getInventory().getCurrentWeight()
                / player.getInventory().getMaxWeight();
        drawBar(BAR_X, wY, BAR_W, BAR_H, wR, wR*0.7f,0.5f-wR*0.3f,0.1f);
        drawText(String.format("%.1f/%.1f",
                        player.getInventory().getCurrentWeight(),
                        player.getInventory().getMaxWeight()),
                BAR_X+4f, wY+BAR_H-2f, 1f,1f,1f,1f);

        // XP
        float xpR = player.getExperience().getXpRatio();
        drawBar(BAR_X, xpY, BAR_W, BAR_H, xpR, 0.2f,0.5f,0.9f);
        drawText("LVL "+player.getExperience().getLevel()
                        +"  "+player.getExperience().getXp()+"/"+player.getExperience().getXpNext()+" XP",
                BAR_X+4f, xpY+BAR_H-2f, 1f,1f,1f,1f);
    }

    // -------------------------------------------------------------------------
    // Painel de criaturas (canto superior direito, abaixo do minimap)
    // -------------------------------------------------------------------------

    private void renderCreaturePanel(Player player, CreatureManager creatures,
                                     Matrix4f ortho, FloatBuffer fb) {
        // Ordena criaturas por distância ao player
        List<Creature> nearby = new ArrayList<>();
        for (Creature c : creatures.getCreatures()) {
            if (!c.isDead() && c.distanceTo(player.getPosition()) <= CP_RADIUS) {
                nearby.add(c);
            }
        }
        nearby.sort(Comparator.comparingDouble(c -> c.distanceTo(player.getPosition())));
        if (nearby.size() > CP_MAX) nearby = nearby.subList(0, CP_MAX);
        if (nearby.isEmpty()) return;

        float panelX = screenW - CP_W - MM_MARGIN;
        float panelY = MM_MARGIN + MM_SIZE + MM_BORDER*2 + 10f;

        useFlatShader(ortho, fb);

        for (int idx = 0; idx < nearby.size(); idx++) {
            Creature c   = nearby.get(idx);
            boolean main = (idx == 0);
            float barH   = main ? CP_BAR_H : CP_BAR_H_SM;
            float barW   = main ? CP_W     : CP_W * 0.75f;
            float barX   = main ? panelX   : panelX + (CP_W - barW);
            float barY   = panelY;

            float hpR = (float) c.getHp() / c.getMaxHp();

            // Fundo
            rect(barX-2f, barY-2f, barW+4f, barH+4f, 0.08f,0.06f,0.12f,0.85f);
            // Barra
            drawBar(barX, barY, barW, barH, hpR, 1f-hpR, hpR*0.8f, 0f);

            // HP numérico
            String hpText = c.getHp() + "/" + c.getMaxHp();
            drawText(hpText, barX + 4f, barY + barH - 2f, 1f,1f,1f,1f);

            // Nome (só na principal)
            if (main) {
                drawText(c.getType().name, barX, barY - 14f, 1f,0.9f,0.6f,1f);
            }

            panelY += barH + CP_GAP + (main ? 18f : 0f);
        }
    }

    // -------------------------------------------------------------------------
    // Minimap
    // -------------------------------------------------------------------------

    private void renderMinimap(Matrix4f ortho, FloatBuffer fb,
                               Player player, WorldMap worldMap,
                               CreatureManager creatures) {
        float mmX = screenW - MM_SIZE - MM_MARGIN;
        float mmY = MM_MARGIN;

        if (mmDirty) { buildMinimapTexture(worldMap); mmDirty = false; }

        glUseProgram(mmShader);
        glUniformMatrix4fv(locMmProj, false, ortho.get(fb));
        glUniform2f(locMmPos,  mmX, mmY);
        glUniform2f(locMmSize, MM_SIZE, MM_SIZE);
        glBindTexture(GL_TEXTURE_2D, mmTexture);
        glBindVertexArray(mmVao);
        glDrawElements(GL_TRIANGLES,6,GL_UNSIGNED_INT,0);
        glBindVertexArray(0);

        useFlatShader(ortho, fb);
        glBindVertexArray(flatVao);
        rect(mmX-MM_BORDER, mmY-MM_BORDER, MM_SIZE+MM_BORDER*2, MM_BORDER,         0.5f,0.3f,0.8f,1f);
        rect(mmX-MM_BORDER, mmY+MM_SIZE,   MM_SIZE+MM_BORDER*2, MM_BORDER,         0.5f,0.3f,0.8f,1f);
        rect(mmX-MM_BORDER, mmY-MM_BORDER, MM_BORDER, MM_SIZE+MM_BORDER*2,         0.5f,0.3f,0.8f,1f);
        rect(mmX+MM_SIZE,   mmY-MM_BORDER, MM_BORDER, MM_SIZE+MM_BORDER*2,         0.5f,0.3f,0.8f,1f);

        // Player
        float px = mmX+(player.getPosition().x/worldMap.getWidth())*MM_SIZE;
        float py = mmY+(player.getPosition().z/worldMap.getHeight())*MM_SIZE;
        rect(px-3f,py-3f,6f,6f, 1f,1f,1f,1f);

        // Criaturas
        for (Creature c : creatures.getCreatures()) {
            if (c.isDead()) continue;
            float cx=mmX+(c.getPosition().x/worldMap.getWidth())*MM_SIZE;
            float cy=mmY+(c.getPosition().z/worldMap.getHeight())*MM_SIZE;
            float r,g,b;
            switch (c.getType().behavior) {
                case HOSTILE -> {r=0.9f;g=0.1f;b=0.1f;}
                case NEUTRAL -> {r=0.9f;g=0.8f;b=0.1f;}
                default      -> {r=0.1f;g=0.9f;b=0.3f;}
            }
            rect(cx-2f,cy-2f,4f,4f, r,g,b,0.9f);
        }
        glBindVertexArray(0);
    }

    private void buildMinimapTexture(WorldMap worldMap) {
        int w=worldMap.getWidth(), h=worldMap.getHeight();
        int[] pixels = new int[w*h];
        for (int x=0;x<w;x++) for (int z=0;z<h;z++) {
            int c = switch (worldMap.getTile(x,z)) {
                case GRASS -> 0xFF3A7A3A;
                case DIRT  -> 0xFF7A5A30;
                case STONE -> 0xFF6A6A6A;
                case WATER -> 0xFF2A5A9A;
            };
            int a=(c>>24)&0xFF,r=(c>>16)&0xFF,g=(c>>8)&0xFF,b=c&0xFF;
            pixels[z*w+x]=(a<<24)|(b<<16)|(g<<8)|r;
        }
        IntBuffer buf=BufferUtils.createIntBuffer(w*h);
        buf.put(pixels).flip();
        glBindTexture(GL_TEXTURE_2D,mmTexture);
        glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,w,h,0,GL_RGBA,GL_UNSIGNED_BYTE,buf);
    }

    // -------------------------------------------------------------------------
    // Tela de morte
    // -------------------------------------------------------------------------

    private void renderDeathScreen(float deathTimer) {
        glBindVertexArray(flatVao);
        rect(0,0,screenW,screenH, 0f,0f,0f,0.6f);
        float bw=300f,bh=80f,bx=(screenW-bw)/2f,by=(screenH-bh)/2f;
        rect(bx,by,bw,bh, 0.5f,0f,0f,0.9f);
        rect(bx,by,bw,2, 0.9f,0.2f,0.2f,1f); rect(bx,by+bh-2,bw,2, 0.9f,0.2f,0.2f,1f);
        rect(bx,by,2,bh, 0.9f,0.2f,0.2f,1f); rect(bx+bw-2,by,2,bh, 0.9f,0.2f,0.2f,1f);
        float prog=Math.max(0,1f-(deathTimer/5f)),barY=by+bh+10f;
        rect(bx,barY,bw,12f, 0.1f,0.1f,0.1f,0.9f);
        rect(bx,barY,bw*prog,12f, 0.8f,0.3f,0.1f,1f);
        glBindVertexArray(0);
        drawText("YOU DIED", bx+bw/2f-40f, by+bh/2f+6f, 1f,0.2f,0.2f,1f);
        drawText("Respawning...", bx+bw/2f-50f, barY+10f, 0.8f,0.8f,0.8f,1f);
    }

    // -------------------------------------------------------------------------
    // Texto (STB TrueType)
    // -------------------------------------------------------------------------

    private void drawText(String text, float x, float y,
                          float r, float g, float b, float a) {
        Matrix4f ortho = new Matrix4f().ortho(0,screenW,screenH,0,-1,1);
        FloatBuffer fb = BufferUtils.createFloatBuffer(16);

        glUseProgram(textShader);
        glUniformMatrix4fv(locTextProj, false, ortho.get(fb));
        glUniform4f(locTextColor, r,g,b,a);
        glBindTexture(GL_TEXTURE_2D, fontTexture);
        glBindVertexArray(textVao);
        glBindBuffer(GL_ARRAY_BUFFER, textVbo);

        FloatBuffer xb = BufferUtils.createFloatBuffer(1);
        FloatBuffer yb = BufferUtils.createFloatBuffer(1);
        xb.put(0,x); yb.put(0,y);

        try (STBTTAlignedQuad q = STBTTAlignedQuad.malloc()) {
            for (char c : text.toCharArray()) {
                if (c < FIRST_CHAR || c >= FIRST_CHAR+CHAR_COUNT) { xb.put(0,xb.get(0)+6); continue; }
                stbtt_GetBakedQuad(charData,BITMAP_W,BITMAP_H,c-FIRST_CHAR,xb,yb,q,true);
                float[] verts = {
                        q.x0(),q.y0(),q.s0(),q.t0(), q.x1(),q.y0(),q.s1(),q.t0(),
                        q.x1(),q.y1(),q.s1(),q.t1(), q.x0(),q.y0(),q.s0(),q.t0(),
                        q.x1(),q.y1(),q.s1(),q.t1(), q.x0(),q.y1(),q.s0(),q.t1(),
                };
                FloatBuffer buf = BufferUtils.createFloatBuffer(verts.length);
                buf.put(verts).flip();
                glBufferSubData(GL_ARRAY_BUFFER,0,buf);
                glDrawArrays(GL_TRIANGLES,0,6);
            }
        }
        glBindVertexArray(0);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void useFlatShader(Matrix4f ortho, FloatBuffer fb) {
        glUseProgram(flatShader);
        glUniformMatrix4fv(locFlatProj, false, ortho.get(fb));
        glBindVertexArray(flatVao);
    }

    private void drawBar(float x, float y, float w, float h, float ratio,
                         float fr, float fg, float fb2) {
        ratio = Math.max(0,Math.min(ratio,1f));
        rect(x,y,w,h, 0.08f,0.08f,0.08f,0.85f);
        rect(x,y,w*ratio,h, fr,fg,fb2,1f);
        rect(x,y,w,1, 0.5f,0.5f,0.5f,1f); rect(x,y+h-1,w,1, 0.5f,0.5f,0.5f,1f);
        rect(x,y,1,h, 0.5f,0.5f,0.5f,1f); rect(x+w-1,y,1,h, 0.5f,0.5f,0.5f,1f);
    }

    private void rect(float x, float y, float w, float h,
                      float r, float g, float b, float a) {
        glUniform2f(locFlatPos,   x, y);
        glUniform2f(locFlatSize,  w, h);
        glUniform4f(locFlatColor, r, g, b, a);
        glDrawElements(GL_TRIANGLES,6,GL_UNSIGNED_INT,0);
    }

    private int buildVao(float[] verts, int[] idx) {
        int v=glGenVertexArrays(),vb=glGenBuffers(),eb=glGenBuffers();
        glBindVertexArray(v);
        FloatBuffer fb=BufferUtils.createFloatBuffer(verts.length); fb.put(verts).flip();
        glBindBuffer(GL_ARRAY_BUFFER,vb); glBufferData(GL_ARRAY_BUFFER,fb,GL_STATIC_DRAW);
        IntBuffer ib=BufferUtils.createIntBuffer(idx.length); ib.put(idx).flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,eb); glBufferData(GL_ELEMENT_ARRAY_BUFFER,ib,GL_STATIC_DRAW);
        glVertexAttribPointer(0,2,GL_FLOAT,false,2*Float.BYTES,0); glEnableVertexAttribArray(0);
        glBindVertexArray(0); return v;
    }

    public void markMinimapDirty() { mmDirty = true; }

    public void cleanup() {
        glDeleteVertexArrays(flatVao); glDeleteVertexArrays(textVao); glDeleteVertexArrays(mmVao);
        glDeleteBuffers(textVbo); glDeleteTextures(fontTexture); glDeleteTextures(mmTexture);
        glDeleteProgram(flatShader); glDeleteProgram(textShader); glDeleteProgram(mmShader);
        charData.free();
    }
}