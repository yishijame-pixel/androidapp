package com.example.funlife.game.platformer



import android.content.Context

import com.example.funlife.game.platformer.tmx.PlatformerTmxLevelCatalog

import com.example.funlife.game.platformer.tmx.PlatformerTmxParser

import com.example.funlife.game.platformer.tmx.PlatformerTmxWorldBuilder



/** 横屏可见行数（整关高度 = 14 格，铺满屏幕）。 */

const val PLATFORMER_LEVEL_ROWS = 14

const val PLATFORMER_CAMPAIGN_LEVEL_COUNT = 52

const val PLATFORMER_HERO_LEVEL_START = PlatformerHeroLevelCatalog.HERO_LEVEL_START

const val PLATFORMER_HERO_LEVEL_COUNT = PlatformerHeroLevelCatalog.HERO_LEVEL_COUNT

const val PLATFORMER_SKY_LEVEL_START = PlatformerSkyLengthSpec.SKY_LEVEL_START

const val PLATFORMER_SKY_LEVEL_COUNT = PlatformerSkyLengthSpec.SKY_LEVEL_COUNT

/** 主线 52 + 英雄 12 + 高空 10 */
const val PLATFORMER_TOTAL_LEVEL_COUNT =
    PLATFORMER_CAMPAIGN_LEVEL_COUNT + PLATFORMER_HERO_LEVEL_COUNT + PLATFORMER_SKY_LEVEL_COUNT



object PlatformerLevels {



    val all: List<PlatformerLevelDef> by lazy {
        PlatformerCampaignLevelCatalog.all +
            PlatformerHeroLevelCatalog.all +
            PlatformerSkyLevelCatalog.all +
            PlatformerSuperTuxLevelCatalog.all
    }



    /** 安全构建：TMX/行地图失败时回退到应急关卡，避免闪退。 */

    fun buildWorldOrFallback(

        context: Context,

        level: PlatformerLevelDef,

        characterId: PlatformerCharacterId = PlatformerCharacterId.CHICK_PRO_MAX,

    ): PlatformerWorld = runCatching {

        buildWorld(context, level, characterId)

    }.getOrElse {

        buildEmergencyWorld(level, characterId)

    }



    fun buildWorld(

        context: Context,

        level: PlatformerLevelDef,

        characterId: PlatformerCharacterId = PlatformerCharacterId.CHICK_PRO_MAX,

    ): PlatformerWorld {

        if (PlatformerSuperTuxLevelCatalog.isSuperTuxLevel(level.id)) {

            return PlatformerSuperTuxLevelLoader.buildWorld(context, level, characterId)

        }

        if (!level.campaignSegmentScript.isNullOrEmpty()) {

            return PlatformerSegmentLevelFactory.buildWorld(context, level, characterId)

        }

        level.tmxAsset?.let { path ->

            val tmx = PlatformerTmxParser.load(context, path)

            return PlatformerTmxWorldBuilder.build(context, level, tmx, characterId)

        }

        return buildWorldFromRows(level, characterId)

    }



    fun buildWorld(

        level: PlatformerLevelDef,

        characterId: PlatformerCharacterId = PlatformerCharacterId.CHICK_PRO_MAX,

    ): PlatformerWorld {

        if (level.rows.isEmpty()) {

            return buildEmergencyWorld(level, characterId)

        }

        return buildWorldFromRows(level, characterId)

    }



    /** TMX 或资源缺失时的最小可玩关卡。 */

    fun buildEmergencyWorld(

        level: PlatformerLevelDef,

        characterId: PlatformerCharacterId,

    ): PlatformerWorld {

        val w = 48

        val h = 14

        val g = h - 1

        val cells = Array(w * h) { PlatformerCell.AIR }

        for (x in 0 until w) {

            cells[g * w + x] = PlatformerCell.SOLID

        }

        cells[g * w + w - 3] = PlatformerCell.GOAL

        val tile = PLATFORMER_TILE_PX.toFloat()

        val ph = PlatformerPhysics.playerH(PLATFORMER_TILE_PX)

        val base = PlatformerLevelDef(

            id = level.id,

            title = level.title,

            subtitle = "${level.subtitle} · 应急地图",

            theme = level.theme,

            rows = List(h) { row ->

                if (row == g) "#".repeat(w - 3) + "O.." else ".".repeat(w)

            },

            skyTop = level.skyTop,

            skyBottom = level.skyBottom,

            tilesetPack = level.tilesetPack,

        )

        return PlatformerWorld(

            level = base,

            width = w,

            height = h,

            cells = cells,

            gems = listOf(

                PlatformerGem(tile * 8f, (g - 2) * tile + tile / 2f),

                PlatformerGem(tile * 16f, (g - 2) * tile + tile / 2f),

            ),

            player = PlatformerPlayer(x = tile * 2f, y = g * tile - ph, grounded = true),

            characterId = characterId,

            goalX = (w - 3) * tile + tile / 2f,

            goalY = g * tile + tile / 2f,

        ).let { PlatformerSkyChickSystem.withSkyHazard(it) }

    }



    private fun buildWorldFromRows(

        level: PlatformerLevelDef,

        characterId: PlatformerCharacterId,

    ): PlatformerWorld = buildWorldFromRowsInternal(level, characterId)

    fun buildWorldFromRowsInternal(
        level: PlatformerLevelDef,
        characterId: PlatformerCharacterId,
    ): PlatformerWorld {

        if (level.rows.isEmpty()) {
            if (!level.campaignSegmentScript.isNullOrEmpty()) {
                error("Campaign level ${level.id} missing baked rows; use PlatformerSegmentLevelFactory.buildWorld")
            }
            return buildEmergencyWorld(level, characterId)
        }

        val height = level.rows.size

        val width = level.rows.maxOf { it.length }

        val cells = Array(height * width) { PlatformerCell.AIR }

        val gems = mutableListOf<PlatformerGem>()

        var spawnX = 48f

        var spawnY = 48f



        level.rows.forEachIndexed { y, row ->

            row.padEnd(width, '.').forEachIndexed { x, ch ->

                val cell = PlatformerCell.fromChar(ch) ?: PlatformerCell.AIR

                cells[y * width + x] = cell

                when (cell) {

                    PlatformerCell.SPAWN -> {

                        spawnX = x * PLATFORMER_TILE_PX + 5f

                        spawnY = y * PLATFORMER_TILE_PX - PlatformerPhysics.playerH(PLATFORMER_TILE_PX) + 2f

                        cells[y * width + x] = PlatformerCell.SOLID

                    }

                    PlatformerCell.GEM -> {

                        gems += PlatformerGem(

                            x = x * PLATFORMER_TILE_PX + PLATFORMER_TILE_PX / 2f,

                            y = y * PLATFORMER_TILE_PX + PLATFORMER_TILE_PX / 2f,

                        )

                        cells[y * width + x] = PlatformerCell.AIR

                    }

                    else -> Unit

                }

            }

        }



        val base = PlatformerWorld(
            level = level,
            width = width,
            height = height,
            cells = cells,
            gems = gems,
            player = PlatformerPlayer(x = spawnX, y = spawnY, grounded = true),
            characterId = characterId,
            levelSpawnX = spawnX,
            levelSpawnY = spawnY,
        )

        return base.copy(
            enemies = PlatformerEnemySystem.spawnFrom(level, world = base),
            traps = PlatformerTrapSystem.spawnFrom(level),
        ).let { PlatformerSkyChickSystem.withSkyHazard(it) }

    }

}


