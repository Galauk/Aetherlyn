# Aetherlyn: MMORPG

Protótipo de um MMORPG em Java com LWJGL, inspirado em *Haven & Hearth* e *Overlord*.

## Recursos
- **Sobrevivência**: Mova o jogador com WASD, colete madeira com espaço.
- **Monstros**: Transforme-se em lich após coletar 50 madeira, invoque um esqueleto com E.
- Mapa 2D procedural com grama, árvores e pedras.

## Como Rodar
1. Instale Java JDK 21 (oracle.com).
2. Instale IntelliJ IDEA Community (jetbrains.com).
3. Clone este repositório: `git clone <URL>`
4. Abra no IntelliJ, espere o Maven baixar dependências.
5. Coloque sprites (32x32) em `src/main/resources/assets`: `grass.png`, `tree.png`, `stone.png`, `player.png`, `lich.png`, `skeleton.png`.
6. Rode `Game.java` (clique direito > Run).

## Próximos Passos
- Adicionar construção de cabanas.
- Implementar salvamento com SQLite.
- Suporte inicial a multiplayer.