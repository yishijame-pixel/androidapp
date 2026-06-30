package com.example.funlife.game.platformer.tmx

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.funlife.game.platformer.PlatformerCell
import com.example.funlife.game.platformer.PlatformerEnemySpawn
import com.example.funlife.game.platformer.PlatformerEnemyType
import com.example.funlife.game.platformer.PlatformerTrapSpawn
import com.example.funlife.game.platformer.PlatformerTrapType
import com.example.funlife.game.platformer.PlatformerGem
import com.example.funlife.game.platformer.PlatformerLevelDef
import com.example.funlife.game.platformer.PlatformerPlayer
import com.example.funlife.game.platformer.PlatformerTheme
import com.example.funlife.game.platformer.PlatformerTilesetPack
import com.example.funlife.game.platformer.PlatformerSkyChickSystem
import com.example.funlife.game.platformer.PlatformerWorld

object PlatformerTmxWorldBuilder {

    private const val COLLISION_LAYER = "bg1"
    private const val SPRITE_LAYER = "sprites"
    private const val DECO_LAYER = "bg0"

    fun build(
        context: Context,
        level: PlatformerLevelDef,
        tmx: PlatformerTmxMap,
        characterId: com.example.funlife.game.platformer.PlatformerCharacterId,
    ): PlatformerWorld {
        val tile = tmx.tilePx
        val cells = Array(tmx.width * tmx.height) { PlatformerCell.AIR }
        val collision = tmx.layers[COLLISION_LAYER] ?: tmx.layers[DECO_LAYER] ?: IntArray(tmx.width * tmx.height)
        val sprites = tmx.layers[SPRITE_LAYER]

        for (ty in 0 until tmx.height) {
            for (tx in 0 until tmx.width) {
                val idx = ty * tmx.width + tx
                val raw = collision.getOrElse(idx) { 0 }
                val gid = tmx.strippedGid(raw)
                if (gid <= 0) continue
                val local = gid - tmx.tilesetFirstGid + 1
                val groundBand = ty >= tmx.height - 10
                val platform = local in 1..64 && ty >= tmx.height / 3
                if (groundBand || platform) {
                    cells[idx] = PlatformerCell.SOLID
                }
            }
        }

        var spawnX = tile * 2f
        var spawnY = tile * 2f
        var goalX = (tmx.width - 3) * tile.toFloat()
        var goalY = (tmx.height - 4) * tile.toFloat()
        val gems = mutableListOf<PlatformerGem>()
        val enemies = mutableListOf<PlatformerEnemySpawn>()

        if (sprites != null) {
            var foundSpawn = false
            for (ty in 0 until tmx.height) {
                for (tx in 0 until tmx.width) {
                    val raw = sprites[ty * tmx.width + tx]
                    val gid = tmx.strippedGid(raw)
                    if (gid <= 0) continue
                    val local = gid - tmx.tilesetFirstGid + 1
                    when {
                        !foundSpawn && local in 519..525 -> {
                            spawnX = tx * tile.toFloat()
                            spawnY = ty * tile.toFloat() - tile * 0.5f
                            foundSpawn = true
                        }
                        local in 615..620 -> {
                            goalX = tx * tile + tile / 2f
                            goalY = ty * tile + tile / 2f
                            cells[ty * tmx.width + tx] = PlatformerCell.GOAL
                        }
                        local in 551..560 -> {
                            enemies += PlatformerEnemySpawn(tx, ty - 1, PlatformerEnemyType.SKULL, 3)
                        }
                        local in 583..590 -> {
                            gems += PlatformerGem(
                                x = tx * tile + tile / 2f,
                                y = ty * tile + tile / 2f,
                            )
                        }
                    }
                }
            }
        }

        if (!gems.any()) {
            for (tx in 0 until tmx.width step 5) {
                gems += PlatformerGem(tx * tile + tile / 2f, (tmx.height - 6) * tile.toFloat())
            }
        }

        val playerH = tile * 1.75f
        val levelWithEnemies = level.copy(
            enemySpawns = enemies + tmxEnemiesForLevel(level.id, tmx.width, tmx.height),
            trapSpawns = tmxTrapsForLevel(level.id, tmx.width, tmx.height),
        )
        val base = PlatformerWorld(
            level = levelWithEnemies,
            width = tmx.width,
            height = tmx.height,
            cells = cells,
            gems = gems,
            player = PlatformerPlayer(
                x = spawnX,
                y = spawnY - playerH + tile,
                grounded = true,
            ),
            characterId = characterId,
            tmx = tmx,
            tilePx = tile,
            goalX = goalX,
            goalY = goalY,
        )
        return base.copy(
            enemies = com.example.funlife.game.platformer.PlatformerEnemySystem.spawnFrom(
                levelWithEnemies,
                tilePx = tile,
                world = base,
            ),
            traps = com.example.funlife.game.platformer.PlatformerTrapSystem.spawnFrom(
                levelWithEnemies,
                tilePx = tile,
            ),
        ).let { PlatformerSkyChickSystem.withSkyHazard(it) }
    }

    /** TMX 关卡注入程序化机关（弥补 object layer 未解析）。 */
    private fun tmxTrapsForLevel(levelId: Int, mapW: Int, mapH: Int): List<PlatformerTrapSpawn> {
        val traps = mutableListOf<PlatformerTrapSpawn>()
        val step = (mapW / 6).coerceAtLeast(4)
        var x = step
        val g = mapH - 4
        while (x < mapW - step) {
            val ty = g - (x / step) % 3
            when ((x / step + levelId) % 4) {
                0 -> traps += PlatformerTrapSpawn(x, ty, PlatformerTrapType.TURRET, 1, facingRight = x % 2 == 0)
                1 -> traps += PlatformerTrapSpawn(x, ty - 1, PlatformerTrapType.LASER, spanTiles = 5)
                2 -> traps += PlatformerTrapSpawn(x, ty, PlatformerTrapType.MOVING_SPIKE, spanTiles = 3)
                else -> traps += PlatformerTrapSpawn(x, ty - 2, PlatformerTrapType.CRUSHER, spanTiles = 1, cycleSec = 3f)
            }
            x += step
        }
        return traps
    }

    /** TMX 关卡补充程序化敌人（地图较长时分段巡逻）。 */
    private fun tmxEnemiesForLevel(levelId: Int, mapW: Int, mapH: Int): List<PlatformerEnemySpawn> {
        val out = mutableListOf<PlatformerEnemySpawn>()
        val step = (mapW / 5).coerceAtLeast(6)
        var x = step
        val g = mapH - 3
        val types = listOf(
            PlatformerEnemyType.SLIME,
            PlatformerEnemyType.MUSHROOM,
            PlatformerEnemyType.BAT,
            PlatformerEnemyType.SNAIL,
            PlatformerEnemyType.GHOST,
            PlatformerEnemyType.SKULL,
        )
        while (x < mapW - step) {
            val ty = g - (x / step) % 2
            val type = types[(x / step + levelId) % types.size]
            val patrol = 3 + (x / step) % 3
            out += PlatformerEnemySpawn(x, ty, type, patrol)
            if (x % (step * 2) == 0) {
                out += PlatformerEnemySpawn(x + 2, g - 4 - (x / step) % 2, PlatformerEnemyType.BAT, 5)
            }
            x += step
        }
        return out
    }

    fun loadTilesetBitmap(context: Context, path: String): ImageBitmap? =
        runCatching {
            context.assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
        }.getOrNull()

    fun loadBackgroundBitmap(context: Context, path: String?): ImageBitmap? {
        if (path == null) return null
        return loadTilesetBitmap(context, path)
    }
}

object PlatformerTmxLevelCatalog {

    val levels: List<PlatformerLevelDef> = listOf(
        tmxLevel(17, "绿林侠影", "Marian & Robin · 森林", "forest",
            PlatformerTheme.PACK_FOREST, 0xFF1B5E20, 0xFF81C784),
        tmxLevel(18, "古堡征途 I", "Castle · 第一幕", "castle/castle",
            PlatformerTheme.PACK_GRAVEYARD, 0xFF263238, 0xFF546E7A),
        tmxLevel(19, "古堡征途 II", "Castle · 第二幕", "castle/castle2",
            PlatformerTheme.PACK_GRAVEYARD, 0xFF263238, 0xFF455A64),
        tmxLevel(20, "古堡征途 III", "Castle · 第三幕", "castle/castle3",
            PlatformerTheme.PACK_GRAVEYARD, 0xFF1A1A2E, 0xFF37474F),
        tmxLevel(21, "古堡终章", "Castle · 第四幕", "castle/castle4",
            PlatformerTheme.PACK_GRAVEYARD, 0xFF0D0D18, 0xFF263238),
        tmxLevel(22, "比武大会", "Tournament · 决赛场", "tournament/tournament",
            PlatformerTheme.FORTRESS, 0xFF4E342E, 0xFF8D6E63),
    )

    private fun tmxLevel(
        id: Int,
        title: String,
        subtitle: String,
        path: String,
        theme: PlatformerTheme,
        skyTop: Long,
        skyBottom: Long,
    ) = PlatformerLevelDef(
        id = id,
        title = title,
        subtitle = subtitle,
        theme = theme,
        tilesetPack = PlatformerTilesetPack.GOODLY,
        skyTop = skyTop,
        skyBottom = skyBottom,
        rows = emptyList(),
        tmxAsset = "platformer/marianandrobin/levels/$path.tmx",
        seriesId = path.substringBefore('/'),
        seriesOrder = id - 16,
    )
}
