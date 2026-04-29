# Aetherlyn

> An open-world MMORPG prototype built in Java, inspired by *Haven & Hearth* and *Overlord*.

---

## Current State

### ✅ Phase 1 — Foundation (complete)
- Isometric camera with PgUp/PgDown rotation (smooth ease-out)
- Point-and-click movement with raycasting
- Scroll zoom
- Fixed timestep game loop (60 TPS / uncapped render)
- Package structure: `core`, `camera`, `input`, `rendering`, `debug`, `world`, `entity`
- F3 debug mode — enables Ctrl+G (grid) and Ctrl+F (creature vision radii)

### ✅ Phase 2 — World (complete)
- Terrain from `terrain.png` spritesheet (32×32px tiles: grass, dirt, stone, water)
- Procedural generation via Perlin Noise + FBM — island-shaped 64×64 map
- Static objects (stone, bush) with billboard rendering and collision
- Camera rotation with ease-out (PgUp/PgDown, 45° steps)

### ✅ Phase 3 — Entities (complete)
- Player with HP, STR, DEF, XP, level progression
- UO-style inventory — weight limit by strength, free item placement, drag items (`I`)
- Resource collection — right-click stone/bush
- Creature AI — state machine (IDLE→PATROL→CHASE→ATTACK→FLEE→DEAD)
- 3 behaviors: PASSIVE (Deer), NEUTRAL (Villager), HOSTILE (Skeleton, Lich)
- Combat — right-click creature, variable damage, HP + defense
- Out-of-combat HP regeneration (5s timeout)
- Death screen with 5s respawn timer
- Player pushes creatures instead of being blocked
- **Registry system** — `CreatureRegistry`, `ItemRegistry`, `StaticObjectRegistry`:
  adding new types requires only one new entry per registry, no other file changes
- **Complete HUD (bottom-left):**
  - HP bar (pulses red in combat) with numeric HP
  - Weight bar with numeric values
  - XP bar with level display
  - Creature panel (top-right, below minimap) — up to 3 nearest creatures with HP bars + numeric HP
  - Minimap 150×150px — tiles + player (white) + creatures by behavior (red/yellow/green)
  - Death screen with respawn progress bar
- **Debug mode (F3):**
  - Ctrl+G — debug grid overlay
  - Ctrl+F — creature vision and attack radii (color-coded by behavior)

### 🔲 Phase 4 — Server
- KryoNet multiplayer
- SQLite persistence
- Shared world for 2D and 3D clients

### 🔲 Phase 5 — Dark Path
- Lich transformation
- Undead army management
- Phylactery mechanic

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 21 |
| 3D Rendering | LWJGL 3, OpenGL 3.3, GLSL |
| Math | JOML |
| Font Rendering | STB TrueType |
| Procedural Gen | Perlin Noise (custom) |
| Networking | KryoNet (planned) |
| Database | SQLite (planned) |
| Build | Maven |

---

## Project Structure

```
src/main/java/com/angelo/mmorpg/
├── Game.java
├── camera/Camera.java
├── core/{Window, ResourceLoader}.java
├── debug/{DebugState, DebugInfo, DebugRenderer}.java
├── entity/
│   ├── {Player, PlayerStats, ExperienceSystem}.java
│   ├── {Inventory, Item, ItemDef, ItemRegistry}.java
│   ├── {Creature, CreatureDef, CreatureRegistry}.java
│   ├── {CreatureBehavior, CreatureState, CreatureManager}.java
│   └── CombatSystem.java
├── input/InputHandler.java
├── rendering/
│   ├── {Renderer, TerrainRenderer, ObjectRenderer}.java
│   ├── {CreatureRenderer, VisionRenderer, GridRenderer}.java
│   ├── {HudRenderer, InventoryRenderer, DebugRenderer}.java (wait, DebugRenderer is in debug/)
└── world/
    ├── {WorldMap, PerlinNoise}.java
    ├── {StaticObject, StaticObjectDef, StaticObjectRegistry}.java
```

---

## Controls

| Input | Action |
|-------|--------|
| Left click | Move player |
| Right click | Attack creature / Collect resource |
| PgUp / PgDown | Rotate camera 45° |
| Scroll | Zoom in/out |
| I | Open/close inventory |
| F3 | Toggle debug mode |
| Ctrl+G | Grid overlay (debug mode only) |
| Ctrl+F | Creature vision radii (debug mode only) |
| ESC | Quit |

---

## Adding New Content

### New creature
Add one entry to `CreatureRegistry.java` — nothing else changes.

### New item
Add one entry to `ItemRegistry.java`. If it drops from a world object, update `StaticObjectRegistry.java`.

### New world object
Add one entry to `StaticObjectRegistry.java` and configure its spawn in `WorldMap.generate()`.

---

## How to Run

```bash
git clone https://github.com/Galauk/Aetherlyn
cd Aetherlyn
mvn clean install
java -jar target/Aetherlyn-1.0-SNAPSHOT.jar
```

Linux:
```bash
sudo apt install libgl1-mesa-dev libglfw3-dev libopenal-dev
```

---

## Inspiration
- [Haven & Hearth](https://www.havenandhearth.com/) — persistent world, survival depth
- *Overlord* — dark fantasy, commanding minions, morality through power