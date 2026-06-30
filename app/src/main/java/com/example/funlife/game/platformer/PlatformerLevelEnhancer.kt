package com.example.funlife.game.platformer



import com.example.funlife.game.platformer.catalog.PlatformerEnemyCatalog

/** 按关卡 ID 注入敌人 / 机关，保证 22 张地图均有挑战内容。 */

object PlatformerLevelEnhancer {



    fun finalize(

        def: PlatformerLevelDef,

        canvas: PlatformerMapCanvas,

        groundY: Int,

        width: Int,

    ): PlatformerLevelDef {

        if (def.seriesId == "supertux_antarctic" || def.theme == PlatformerTheme.PACK_SUPERTUX) {
            return def.copy(
                enemySpawns = canvas.enemySpawns(),
                trapSpawns = canvas.trapSpawns(),
            )
        }

        if (!def.campaignSegmentScript.isNullOrEmpty()) {

            return def.copy(

                enemySpawns = canvas.enemySpawns() + scriptedEnemiesScaled(def.id, groundY, width),

                trapSpawns = canvas.trapSpawns() + scriptedTrapsScaled(def.id, groundY, width),

            )

        }

        val extraTraps = scriptedTraps(def.id, groundY, width)

        val extraEnemies = scriptedEnemies(def.id, groundY, width)

        return def.copy(

            enemySpawns = canvas.enemySpawns() + extraEnemies,

            trapSpawns = canvas.trapSpawns() + extraTraps,

        )

    }



    private fun scriptedTrapsScaled(id: Int, g: Int, w: Int): List<PlatformerTrapSpawn> {

        val legacy = if (w < 250) scriptedTraps(id, g, w) else emptyList()

        return legacy + campaignTrapsDistributed(id, g, w)

    }



    private fun scriptedEnemiesScaled(id: Int, g: Int, w: Int): List<PlatformerEnemySpawn> {

        val heroSpawns = PlatformerEnemyCatalog.heroLevelEnemySpawns(id, g, w)
        if (heroSpawns.isNotEmpty()) return heroSpawns

        val legacy = if (w < 250) scriptedEnemies(id, g, w) else emptyList()

        return legacy + campaignEnemiesDistributed(id, g, w)

    }



    /** 长关沿宽度均匀撒布激光 / 炮台 / 地刺等机关。 */

    private fun campaignTrapsDistributed(id: Int, g: Int, w: Int): List<PlatformerTrapSpawn> {

        if (w < 80) return emptyList()

        val tier = when {

            id <= 6 -> 1

            id <= 16 -> 2

            id <= 34 -> 3

            else -> 3

        }

        val step = when {

            w >= 500 -> 26

            w >= 300 -> 28

            else -> 32

        }

        val traps = mutableListOf<PlatformerTrapSpawn>()

        var x = step

        var i = 0

        while (x < w - step - 4) {

            val y = (g - 2 - (i % 4)).coerceAtLeast(1)

            val slot = (i + id * 3) % 7

            traps += when {

                slot == 0 -> trapSpawn(x, y, PlatformerTrapType.TURRET, facing = i % 2 == 0, w = w)

                slot == 1 -> trapSpawn(x, y, PlatformerTrapType.LASER, span = 4 + tier, w = w)

                slot == 2 -> trapSpawn(x, y, PlatformerTrapType.MOVING_SPIKE, span = 2 + tier, w = w)

                slot == 3 && tier >= 2 -> trapSpawn(

                    x, y - 1, PlatformerTrapType.LASER,

                    span = 3, axis = PlatformerTrapAxis.VERTICAL, cycle = 2.4f, w = w,

                )

                slot == 4 && tier >= 2 -> trapSpawn(x, y, PlatformerTrapType.CRUSHER, cycle = 2.8f, w = w)

                slot == 5 && tier >= 2 -> trapSpawn(

                    x, y, PlatformerTrapType.LASER, span = 5, cycle = 1.7f, w = w,

                )

                slot == 6 && tier >= 3 -> trapSpawn(

                    x, y, PlatformerTrapType.TURRET, facing = false, w = w,

                )

                else -> trapSpawn(x, y, PlatformerTrapType.TURRET, facing = true, w = w)

            }

            x += step

            i++

        }

        return traps

    }



    private fun campaignEnemiesDistributed(id: Int, g: Int, w: Int): List<PlatformerEnemySpawn> {

        if (w < 80) return emptyList()

        val step = when {

            w >= 500 -> 36

            w >= 300 -> 40

            else -> 44

        }

        val types = listOf(

            PlatformerEnemyType.SLIME,

            PlatformerEnemyType.MUSHROOM,

            PlatformerEnemyType.CHICKEN,

            PlatformerEnemyType.SNAIL,

            PlatformerEnemyType.BAT,

            PlatformerEnemyType.SKULL,

        )

        val enemies = mutableListOf<PlatformerEnemySpawn>()

        var x = step

        var i = 0

        while (x < w - step - 4) {

            val groundType = types[(i + id) % types.size]

            enemies += enemySpawn(x, g - 1, groundType, 3 + (i % 3), w)

            if (id >= 10 && i % 3 == 0) {

                enemies += enemySpawn(x + 2, g - 3, PlatformerEnemyType.BAT, 4, w)

            }

            if (id >= 20 && i % 5 == 0) {

                enemies += enemySpawn(x + 1, g - 2, PlatformerEnemyType.GHOST, 3, w)

            }

            x += step

            i++

        }

        return enemies

    }



    private fun trapSpawn(

        x: Int,

        y: Int,

        type: PlatformerTrapType,

        span: Int = 4,

        axis: PlatformerTrapAxis = PlatformerTrapAxis.HORIZONTAL,

        cycle: Float = 2.4f,

        facing: Boolean = true,

        w: Int,

    ) = PlatformerTrapSpawn(

        x.coerceIn(0, w - 2),

        y.coerceIn(1, PLATFORMER_LEVEL_ROWS - 2),

        type,

        span,

        axis,

        cycle,

        facingRight = facing,

    )



    private fun enemySpawn(x: Int, y: Int, type: PlatformerEnemyType, patrol: Int, w: Int) =

        PlatformerEnemySpawn(x.coerceIn(0, w - 2), y.coerceIn(1, PLATFORMER_LEVEL_ROWS - 2), type, patrol)



    private fun scriptedTraps(id: Int, g: Int, w: Int): List<PlatformerTrapSpawn> {

        fun t(

            x: Int, y: Int, type: PlatformerTrapType,

            span: Int = 4, axis: PlatformerTrapAxis = PlatformerTrapAxis.HORIZONTAL,

            cycle: Float = 2.4f, facing: Boolean = true,

        ) = PlatformerTrapSpawn(x.coerceIn(0, w - 2), y.coerceIn(1, g), type, span, axis, cycle, facingRight = facing)



        return when (id) {

            1 -> listOf(

                t(28, g - 2, PlatformerTrapType.TURRET, facing = true),

                t(52, g - 4, PlatformerTrapType.LASER, span = 5),

                t(88, g - 3, PlatformerTrapType.MOVING_SPIKE, span = 3),

            )

            2 -> listOf(

                t(34, g - 5, PlatformerTrapType.LASER, span = 4, cycle = 2.4f),

                t(64, g - 4, PlatformerTrapType.TURRET, facing = false),

                t(88, g - 5, PlatformerTrapType.MOVING_SPIKE, span = 3),

            )

            3 -> listOf(

                t(40, g - 5, PlatformerTrapType.LASER, span = 4),

                t(68, g - 4, PlatformerTrapType.TURRET),

                t(94, g - 6, PlatformerTrapType.MOVING_SPIKE, span = 3),

            )

            4 -> listOf(

                t(30, g - 3, PlatformerTrapType.LASER, span = 5, cycle = 1.8f),

                t(55, g - 4, PlatformerTrapType.TURRET, facing = false),

                t(72, g - 2, PlatformerTrapType.CRUSHER),

                t(95, g - 5, PlatformerTrapType.LASER, span = 4),

            )

            5 -> listOf(

                t(40, g - 2, PlatformerTrapType.TURRET),

                t(62, g - 3, PlatformerTrapType.LASER, span = 5),

                t(88, g - 4, PlatformerTrapType.MOVING_SPIKE, span = 4),

            )

            6 -> listOf(

                t(35, g - 3, PlatformerTrapType.LASER, span = 6),

                t(60, g - 4, PlatformerTrapType.TURRET, facing = false),

                t(85, g - 5, PlatformerTrapType.CRUSHER),

                t(105, g - 3, PlatformerTrapType.MOVING_SPIKE, span = 5),

            )

            7 -> listOf(

                t(35, g - 3, PlatformerTrapType.LASER, span = 5),

                t(80, g - 3, PlatformerTrapType.TURRET, facing = false),

                t(120, g - 4, PlatformerTrapType.MOVING_SPIKE, span = 4),

                t(155, g - 3, PlatformerTrapType.CRUSHER),

            )

            8 -> listOf(

                t(40, g - 3, PlatformerTrapType.LASER, span = 4),

                t(90, g - 3, PlatformerTrapType.TURRET),

                t(130, g - 4, PlatformerTrapType.MOVING_SPIKE, span = 4),

            )

            in 9..16 -> emptyList() // pack factory 自带机关

            in 17..22 -> emptyList() // TMX builder 注入

            in 23..52 -> emptyList() // 片段关自带机关

            else -> emptyList()

        }

    }



    private fun scriptedEnemies(id: Int, g: Int, w: Int): List<PlatformerEnemySpawn> {

        fun e(x: Int, y: Int, type: PlatformerEnemyType, patrol: Int = 3) =

            PlatformerEnemySpawn(x.coerceIn(0, w - 2), y.coerceIn(1, g), type, patrol)



        if (id in 23..52) {

            return listOf(

                e(w / 5, g - 1, PlatformerEnemyType.SLIME),

                e(w / 2, g - 1, PlatformerEnemyType.MUSHROOM, 4),

                e(3 * w / 4, g - 2, PlatformerEnemyType.BAT, 5),

            )

        }



        return when (id) {

            1 -> listOf(e(24, g - 1, PlatformerEnemyType.SLIME), e(70, g - 1, PlatformerEnemyType.CHICKEN, 4))

            2 -> listOf(e(40, g - 1, PlatformerEnemyType.SNAIL), e(52, g - 5, PlatformerEnemyType.BAT, 4))

            3 -> listOf(e(44, g - 1, PlatformerEnemyType.SLIME), e(88, g - 1, PlatformerEnemyType.SNAIL, 3))

            4 -> listOf(e(25, g - 1, PlatformerEnemyType.GHOST), e(60, g - 3, PlatformerEnemyType.SKULL), e(90, g - 1, PlatformerEnemyType.MUSHROOM))

            5 -> listOf(e(35, g - 1, PlatformerEnemyType.SNAIL), e(75, g - 2, PlatformerEnemyType.BAT, 4))

            6 -> listOf(e(28, g - 1, PlatformerEnemyType.MUSHROOM), e(68, g - 1, PlatformerEnemyType.CHICKEN, 5), e(100, g - 3, PlatformerEnemyType.BAT))

            7 -> listOf(

                e(20, g - 1, PlatformerEnemyType.SLIME),

                e(55, g - 2, PlatformerEnemyType.MUSHROOM, 4),

                e(100, g - 1, PlatformerEnemyType.SNAIL),

                e(140, g - 3, PlatformerEnemyType.BAT, 5),

            )

            8 -> listOf(

                e(18, g - 1, PlatformerEnemyType.SLIME),

                e(60, g - 2, PlatformerEnemyType.CHICKEN, 4),

                e(110, g - 1, PlatformerEnemyType.MUSHROOM),

                e(150, g - 3, PlatformerEnemyType.BAT, 5),

            )

            else -> emptyList()

        }

    }

}


