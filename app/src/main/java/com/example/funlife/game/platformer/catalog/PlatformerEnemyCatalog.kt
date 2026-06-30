package com.example.funlife.game.platformer.catalog

import com.example.funlife.game.platformer.PLATFORMER_TILE_PX
import com.example.funlife.game.platformer.PlatformerEnemyBehavior
import com.example.funlife.game.platformer.PlatformerEnemySpawn
import com.example.funlife.game.platformer.PlatformerEnemyType
import com.example.funlife.game.platformer.PlatformerHeroLevelCatalog

/**
 * Catalog 驱动敌人元数据：行为、碰撞盒、关卡 featured 映射。
 */
object PlatformerEnemyCatalog {

    fun entry(catalogId: String): PlatformerContentCatalog.EnemyEntry? =
        PlatformerContentCatalog.requireLoaded().enemies.find { it.id == catalogId }

    fun featuredEnemyForLevel(levelId: Int): PlatformerContentCatalog.EnemyEntry? =
        PlatformerContentCatalog.heroLevels()
            .find { it.id == levelId }
            ?.featuredEnemyId
            ?.let { entry(it) }

    fun behaviorFor(catalogId: String): PlatformerEnemyBehavior {
        val raw = entry(catalogId)?.behavior?.uppercase() ?: return PlatformerEnemyBehavior.PATROL
        return when (raw) {
            "FLY" -> PlatformerEnemyBehavior.FLY
            "FLOAT" -> PlatformerEnemyBehavior.FLOAT
            else -> PlatformerEnemyBehavior.PATROL
        }
    }

    fun fallbackType(catalogId: String): PlatformerEnemyType = when (entry(catalogId)?.group) {
        "undead" -> PlatformerEnemyType.SKULL
        "beast" -> PlatformerEnemyType.CHICKEN
        "mech" -> PlatformerEnemyType.MUSHROOM
        else -> PlatformerEnemyType.SLIME
    }

    fun width(catalogId: String?, type: PlatformerEnemyType, tilePx: Int = PLATFORMER_TILE_PX): Float {
        if (catalogId != null) {
            return when (catalogId) {
                "dino_enemy" -> tilePx * 0.95f
                "robot_sentry" -> tilePx * 0.82f
                "wild_dog" -> tilePx * 0.78f
                "zombie_male", "zombie_female" -> tilePx * 0.72f
                else -> tilePx * 0.75f
            }
        }
        return when (type) {
            PlatformerEnemyType.SKULL -> tilePx * 0.9f
            PlatformerEnemyType.SNAIL -> tilePx * 0.65f
            PlatformerEnemyType.BAT -> tilePx * 0.7f
            else -> tilePx * 0.72f
        }
    }

    fun height(catalogId: String?, type: PlatformerEnemyType, tilePx: Int = PLATFORMER_TILE_PX): Float {
        if (catalogId != null) {
            return when (catalogId) {
                "dino_enemy" -> tilePx * 0.88f
                "robot_sentry" -> tilePx * 0.9f
                "wild_dog" -> tilePx * 0.65f
                "zombie_male", "zombie_female" -> tilePx * 0.85f
                else -> tilePx * 0.78f
            }
        }
        return when (type) {
            PlatformerEnemyType.SKULL -> tilePx * 0.95f
            PlatformerEnemyType.SNAIL -> tilePx * 0.55f
            PlatformerEnemyType.CHICKEN -> tilePx * 0.82f
            PlatformerEnemyType.BAT -> tilePx * 0.62f
            else -> tilePx * 0.72f
        }
    }

    fun heightCellFrac(catalogId: String): Float =
        entry(catalogId)?.let { 0.85f } ?: 0.85f

    /** 英雄章节：沿宽度均匀撒 catalog 敌人；无 featured 时保留 legacy 分布。 */
    fun heroLevelEnemySpawns(levelId: Int, groundY: Int, width: Int): List<PlatformerEnemySpawn> {
        if (levelId !in PlatformerHeroLevelCatalog.HERO_LEVEL_START until
            PlatformerHeroLevelCatalog.HERO_LEVEL_START + PlatformerHeroLevelCatalog.HERO_LEVEL_COUNT
        ) {
            return emptyList()
        }
        val featured = featuredEnemyForLevel(levelId) ?: return emptyList()
        if (width < 80) return emptyList()
        val step = when {
            width >= 500 -> 36
            width >= 300 -> 40
            else -> 44
        }
        val behavior = behaviorFor(featured.id)
        val fallback = fallbackType(featured.id)
        val spawns = mutableListOf<PlatformerEnemySpawn>()
        var x = step
        var i = 0
        while (x < width - step - 4) {
            spawns += PlatformerEnemySpawn(
                tileX = x.coerceIn(0, width - 2),
                tileY = when (behavior) {
                    PlatformerEnemyBehavior.FLY, PlatformerEnemyBehavior.FLOAT -> groundY - 3
                    else -> groundY - 1
                }.coerceIn(1, com.example.funlife.game.platformer.PLATFORMER_LEVEL_ROWS - 2),
                type = fallback,
                patrolTiles = 3 + (i % 3),
                catalogId = featured.id,
            )
            x += step
            i++
        }
        return spawns
    }

    /** 无尽模式 catalog 敌人轮换（墓园/机械/英雄跑道群系）。 */
    fun endlessCatalogEnemyForBiome(biomeIndex: Int): String? {
        val catalogEnemies = PlatformerContentCatalog.enemies()
        if (catalogEnemies.isEmpty()) return null
        return when (biomeIndex % 5) {
            0 -> catalogEnemies.find { it.id == "zombie_male" }?.id
            1 -> catalogEnemies.find { it.id == "robot_sentry" }?.id
            2 -> catalogEnemies.find { it.id == "wild_dog" }?.id
            3 -> catalogEnemies.find { it.id == "dino_enemy" }?.id
            else -> catalogEnemies.find { it.id == "zombie_female" }?.id
        }
    }
}
