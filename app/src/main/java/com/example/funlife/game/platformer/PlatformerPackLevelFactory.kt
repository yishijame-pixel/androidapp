package com.example.funlife.game.platformer

/**
 * 可复用外部地砖包关卡模板：分段地面 + 双路线 + 机关/敌人。
 */
object PlatformerPackLevelFactory {

    data class Config(
        val id: Int,
        val title: String,
        val subtitle: String,
        val pack: PlatformerTilesetPack,
        val theme: PlatformerTheme,
        val skyTop: Long,
        val skyBottom: Long,
        val seriesId: String,
        val seriesOrder: Int,
        val width: Int = 160,
        val sections: Int = 5,
        val hazardLevel: Int = 1,
        val trapLevel: Int = 1,
        val enemies: List<PlatformerEnemyType> = listOf(
            PlatformerEnemyType.SLIME,
            PlatformerEnemyType.MUSHROOM,
        ),
    )

    fun build(cfg: Config): PlatformerLevelDef {
        val w = cfg.width
        val h = 14
        val m = PlatformerMapCanvas(w, h)
        val g = h - 1
        val seg = w / cfg.sections
        val spans = mutableListOf<IntArray>()
        var x = 0
        repeat(cfg.sections) { i ->
            val spanW = if (i == cfg.sections - 1) w - x - 1 else seg - 1
            if (spanW > 2) spans += intArrayOf(x, spanW)
            x += spanW + 1
            if (i < cfg.sections - 1 && cfg.hazardLevel > 0) {
                m.spikePit(g, (x - 1).coerceIn(0, w - 1), 1)
                // 低路连通：每段裂隙上方铺踏脚石
                m.bridgeGap((x - 1).coerceIn(0, w - 1), g, 1)
            }
        }
        m.groundSpans(g, *spans.toTypedArray())
        m.spawn(2, g)
        m.backdrop(8, g - 1)
        m.crate(12, g)

        repeat(cfg.sections) { i ->
            val base = i * seg + 14
            if (base + 10 >= w - 4) return@repeat

            m.gentleSteps(base, g, 2 + (i % 3))
            m.gem((base + 2).coerceAtMost(w - 2), g - 2)

            if (cfg.hazardLevel >= 1) {
                val hx = (base + 5).coerceAtMost(w - 4)
                m.fillRect(hx, g - 1, 2.coerceAtMost(w - hx), 1, '-')
                if (hx + 2 < w) m.spike((hx + 2).coerceAtMost(w - 1), g - 1)
            }
            if (cfg.hazardLevel >= 2 && i % 2 == 0) {
                m.spring((base + 8).coerceAtMost(w - 2), g - 1)
            }
            if (cfg.hazardLevel >= 3) {
                val px = (base + 11).coerceAtMost(w - 3)
                m.spikes(px, g, (w - px).coerceAtMost(2))
            }

            // 高路浮岛 + 双路线
            val islandX = (base + 15).coerceAtMost(w - 6)
            if (islandX + 5 < w) {
                m.dualRoute(islandX - 2, g, 2)
                m.islandSupported(islandX, g - 2, 5, 1, g)
                m.gem((islandX + 2).coerceAtMost(w - 2), g - 3)
            }

            val enemyType = cfg.enemies[i % cfg.enemies.size]
            m.enemy((base + 4).coerceAtMost(w - 3), g - 1, enemyType, patrol = 3 + i % 3)
            if (i % 2 == 0) {
                m.enemy((base + 10).coerceAtMost(w - 3), g - 3, PlatformerEnemyType.BAT, patrol = 5)
            }
            if (i % 3 == 1) {
                m.enemy((base + 7).coerceAtMost(w - 3), g - 2, PlatformerEnemyType.SNAIL, patrol = 2)
            }

            // 机关密度随 trapLevel 递增
            if (cfg.trapLevel >= 1 && i % 2 == 0) {
                m.turret((base + 6).coerceAtMost(w - 2), g - 2, facingRight = i % 4 != 0)
            }
            if (cfg.trapLevel >= 2 && i % 3 != 2) {
                m.laserH((base + 3).coerceAtMost(w - 4), g - 3, span = 4 + i % 2, cycle = 2f + i * 0.2f)
            }
            if (cfg.trapLevel >= 3) {
                m.movingSpike((base + 12).coerceAtMost(w - 4), g - 2, span = 3)
                if (cfg.theme == PlatformerTheme.PACK_SCIFI || cfg.theme == PlatformerTheme.PACK_GRAVEYARD) {
                    m.laserV((base + 9).coerceAtMost(w - 2), g - 5, span = 3, cycle = 1.8f)
                }
            }
            if (cfg.trapLevel >= 2 && i == cfg.sections - 1) {
                m.crusher((base + 8).coerceAtMost(w - 2), g - 3)
            }
        }

        m.goal(w - 4, g - 2)
        m.gem(w - 3, g - 2)
        m.backdrop((w - 8).coerceAtLeast(1), g - 1)

        return PlatformerLevelEnhancer.finalize(
            PlatformerLevelDef(
                id = cfg.id,
                title = cfg.title,
                subtitle = cfg.subtitle,
                theme = cfg.theme,
                tilesetPack = cfg.pack,
                skyTop = cfg.skyTop,
                skyBottom = cfg.skyBottom,
                rows = m.toRows(),
                seriesId = cfg.seriesId,
                seriesOrder = cfg.seriesOrder,
            ),
            m, g, w,
        )
    }
}
