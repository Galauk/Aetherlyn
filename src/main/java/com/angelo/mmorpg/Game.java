package com.angelo.mmorpg;

import com.angelo.mmorpg.camera.Camera;
import com.angelo.mmorpg.core.Window;
import com.angelo.mmorpg.debug.DebugInfo;
import com.angelo.mmorpg.debug.DebugRenderer;
import com.angelo.mmorpg.debug.DebugState;
import com.angelo.mmorpg.entity.*;
import com.angelo.mmorpg.input.InputHandler;
import com.angelo.mmorpg.rendering.*;
import com.angelo.mmorpg.world.StaticObject;
import com.angelo.mmorpg.world.WorldMap;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.opengl.GL11.*;

public class Game {

    private static final int    WIDTH    = 800;
    private static final int    HEIGHT   = 600;
    private static final double TPS      = 60.0;
    private static final double TICK_TIME= 1.0/TPS;
    private static final int    MAX_TICKS= 5;
    private static final float  PLAYER_SPEED    = 3.0f;
    private static final float  ATTACK_COOLDOWN = 0.8f;
    private static final float  DEATH_RESPAWN   = 5.0f;

    private Window            window;
    private Camera            camera;
    private InputHandler      input;
    private Renderer          renderer;
    private TerrainRenderer   terrainRenderer;
    private ObjectRenderer    objectRenderer;
    private CreatureRenderer  creatureRenderer;
    private VisionRenderer    visionRenderer;
    private GridRenderer      gridRenderer;
    private InventoryRenderer inventoryRenderer;
    private HudRenderer       hudRenderer;
    private DebugState        debugState;
    private DebugInfo         debugInfo;
    private DebugRenderer     debugRenderer;
    private WorldMap          worldMap;
    private Player            player;
    private CreatureManager   creatureManager;
    private CombatSystem      combatSystem;

    private Vector3f lastClick         = null;
    private float    playerAttackTimer = 0;
    private float    deathTimer        = 0;
    private boolean  isDead            = false;

    private float fps=0,fpsTimer=0,frameCount=0;
    private float tps=0,tpsTimer=0,tickCount=0;

    public void run() { init(); loop(); cleanup(); }

    private void init() {
        window = new Window(WIDTH, HEIGHT, "Aetherlyn: MMORPG 3D");
        window.init();

        worldMap          = new WorldMap();
        player            = new Player(WorldMap.WIDTH/2f, WorldMap.HEIGHT/2f);
        combatSystem      = new CombatSystem();
        creatureManager   = new CreatureManager();
        debugState        = new DebugState();
        debugInfo         = new DebugInfo();
        camera            = new Camera(WIDTH, HEIGHT);
        inventoryRenderer = new InventoryRenderer(WIDTH, HEIGHT);
        hudRenderer       = new HudRenderer(WIDTH, HEIGHT);

        input = new InputHandler(window.getHandle(), camera, debugState,
                inventoryRenderer, player.getInventory(), WIDTH, HEIGHT);

        renderer         = new Renderer();
        terrainRenderer  = new TerrainRenderer();
        objectRenderer   = new ObjectRenderer();
        creatureRenderer = new CreatureRenderer();
        visionRenderer   = new VisionRenderer();
        gridRenderer     = new GridRenderer();
        debugRenderer    = new DebugRenderer(WIDTH, HEIGHT);

        input.init();
        renderer.init();
        terrainRenderer.init(worldMap);
        objectRenderer.init();
        creatureRenderer.init();
        visionRenderer.init();
        gridRenderer.init();
        debugRenderer.init();
        inventoryRenderer.init();
        hudRenderer.init();

        player.getExperience().setOnLevelUp(() ->
                combatSystem.getLog().add(0, "*** LEVEL UP! Level "
                        + player.getExperience().getLevel() + " ***")
        );

        creatureManager.spawnAll(worldMap);
        camera.setTarget(player.getPosition());
    }

    private void loop() {
        double previous=glfwGetTime(), accumulator=0.0;
        while (!window.shouldClose()) {
            double current=glfwGetTime();
            double elapsed=Math.min(current-previous, MAX_TICKS*TICK_TIME);
            previous=current; accumulator+=elapsed;

            while (accumulator>=TICK_TIME) { tick((float)TICK_TIME); accumulator-=TICK_TIME; tickCount++; }

            tpsTimer+=(float)elapsed;
            if (tpsTimer>=0.5f){tps=tickCount/tpsTimer;tpsTimer=0;tickCount=0;}
            frameCount++;fpsTimer+=(float)elapsed;
            if (fpsTimer>=0.5f){fps=frameCount/fpsTimer;fpsTimer=0;frameCount=0;}

            render();
            window.swapBuffers();
            window.pollEvents();

            // Atualiza câmera se janela foi redimensionada
            if (window.wasResized()) {
                camera.resize(window.getWidth(), window.getHeight());
            }
        }
    }

    private void tick(float delta) {
        player.getStats().update(delta);

        if (player.getStats().isDead()) {
            if (!isDead) { isDead=true; deathTimer=DEATH_RESPAWN; }
            deathTimer-=delta;
            if (deathTimer<=0) respawn();
            camera.update(delta);
            camera.setTarget(player.getPosition());
            return;
        }
        isDead=false;
        playerAttackTimer=Math.max(0,playerAttackTimer-delta);

        Vector3f click=input.consumeMoveTarget();
        if (click!=null && canMoveTo(click)) { player.setTarget(click); lastClick=new Vector3f(click); }

        Vector3f interact=input.consumeInteractTarget();
        if (interact!=null) {
            Creature target=creatureManager.getCreatureAt(interact.x,interact.z,1.5f);
            if (target!=null && player.canInteractWith(target.getPosition().x,target.getPosition().z)
                    && playerAttackTimer<=0) {
                combatSystem.playerAttack(player,target);
                playerAttackTimer=ATTACK_COOLDOWN;
            } else {
                tryCollect(interact.x,interact.z);
            }
        }

        creatureManager.update(delta,player,worldMap,combatSystem);
        movePlayer(delta);
        camera.update(delta);
        camera.setTarget(player.getPosition());
        debugInfo.update(fps,tps,player.getPosition(),lastClick,
                camera.getZoom(),camera.getYaw(),worldMap.getSeed());
    }

    private void respawn() {
        Vector3f c=new Vector3f(WorldMap.WIDTH/2f,0,WorldMap.HEIGHT/2f);
        player.setPosition(c); player.setTarget(c);
        player.getStats().respawn(); isDead=false;
    }

    private void tryCollect(float wx,float wz) {
        for (StaticObject obj : worldMap.getObjects()) {
            if (obj.isCollected()) continue;
            float dx=wx-obj.worldX,dz=wz-obj.worldZ;
            if (Math.sqrt(dx*dx+dz*dz)<1.0f
                    && player.canInteractWith(obj.worldX,obj.worldZ)
                    && obj.def.isCollectable()
                    && player.collect(obj.def.dropItemId)) {
                obj.collect(); worldMap.clearObject(obj.tileX,obj.tileZ); break;
            }
        }
    }

    private void movePlayer(float delta) {
        Vector3f pos=player.getPosition();
        Vector3f diff=new Vector3f(player.getTarget()).sub(pos);
        float dist=diff.length();
        if (dist<0.05f) return;

        float step=PLAYER_SPEED*delta;
        Vector3f dir=new Vector3f(diff).normalize();
        Vector3f next=new Vector3f(pos).add(new Vector3f(dir).mul(Math.min(step,dist)));

        if      (canMoveTo(next))                            { player.setPosition(next); }
        else if (canMoveTo(new Vector3f(next.x,0,pos.z)))   { player.setPosition(new Vector3f(next.x,0,pos.z)); }
        else if (canMoveTo(new Vector3f(pos.x,0,next.z)))   { player.setPosition(new Vector3f(pos.x,0,next.z)); }
    }

    private boolean canMoveTo(Vector3f pos) {
        float r=Player.COLLISION_RADIUS;
        return worldMap.isWalkable(pos.x-r,pos.z-r) && worldMap.isWalkable(pos.x+r,pos.z-r)
                && worldMap.isWalkable(pos.x-r,pos.z+r) && worldMap.isWalkable(pos.x+r,pos.z+r)
                && !worldMap.collidesWithObject(pos.x,pos.z,r);
    }

    private void render() {
        glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
        terrainRenderer.render(camera);
        objectRenderer.render(camera,worldMap);
        creatureRenderer.render(camera,creatureManager);
        if (!isDead) renderer.render(camera,player.getPosition());

        // Debug overlays (só em debug mode)
        if (debugState.isGridVisible())   gridRenderer.render(camera);
        if (debugState.isVisionVisible()) visionRenderer.render(camera,creatureManager);

        // UI
        hudRenderer.render(player,isDead,deathTimer,worldMap,creatureManager);
        inventoryRenderer.render(player.getInventory());
        if (debugState.isDebugPanelVisible()) debugRenderer.render(debugInfo);
    }

    private void cleanup() {
        renderer.cleanup(); terrainRenderer.cleanup(); objectRenderer.cleanup();
        creatureRenderer.cleanup(); visionRenderer.cleanup(); gridRenderer.cleanup();
        hudRenderer.cleanup(); inventoryRenderer.cleanup(); debugRenderer.cleanup();
        window.destroy();
    }

    public static void main(String[] args) { new Game().run(); }
}