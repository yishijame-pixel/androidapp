package com.example.funlife.game.treasurehunter

import com.example.funlife.game.platformer.PlatformerEnemyType
import com.example.funlife.game.platformer.PlatformerLevelDef
import com.example.funlife.game.platformer.PlatformerMapCanvas
import com.example.funlife.game.platformer.PlatformerTheme
import com.example.funlife.game.platformer.PlatformerTilesetPack

/** 宝藏猎人独立模式关卡（地牢探索 + 敌人 + 宝藏）。 */
object TreasureHunterLevels {

    val all: List<PlatformerLevelDef> by lazy {
        listOf(stage1(), stage2(), stage3(), stage4(), stage5())
    }

    private fun stage1(): PlatformerLevelDef {
        val w = 120
        val h = 14
        val m = PlatformerMapCanvas(w, h)
        val g = h - 1
        m.fillRect(0, g, w, 1, '#')
        m.spawn(2, g)
        m.gentleSteps(10, g, 3)
        m.enemy(18, g - 1, PlatformerEnemyType.BAT, 4)
        m.gem(22, g - 2)
        m.spikePit(g, 28, 1)
        m.spring(32, g - 1)
        m.enemy(40, g - 1, PlatformerEnemyType.SLIME, 3)
        m.gem(45, g - 2)
        m.goal(55, g - 2)
        return def(1, "地下入口", "Entry · 蝙蝠洞", m, 0xFF1A237E, 0xFF3949AB)
    }

    private fun stage2(): PlatformerLevelDef {
        val w = 140
        val h = 14
        val m = PlatformerMapCanvas(w, h)
        val g = h - 1
        m.groundSpans(g, intArrayOf(0, 30), intArrayOf(31, 25), intArrayOf(57, 30), intArrayOf(88, 25), intArrayOf(114, 26))
        m.spikePit(g, 30, 1)
        m.spikePit(g, 56, 1)
        m.spikePit(g, 87, 1)
        m.spawn(2, g)
        m.enemy(15, g - 1, PlatformerEnemyType.GHOST, 4)
        m.enemy(35, g - 2, PlatformerEnemyType.SKULL, 3)
        m.spring(42, g - 1)
        m.gem(50, g - 3)
        m.enemy(70, g - 1, PlatformerEnemyType.MUSHROOM, 4)
        m.spikes(78, g - 1, 2)
        m.gem(95, g - 2)
        m.goal(120, g - 2)
        return def(2, "幽魂走廊", "Corridor · 骷髅巡逻", m, 0xFF311B92, 0xFF512DA8)
    }

    private fun stage3(): PlatformerLevelDef {
        val w = 160
        val h = 14
        val m = PlatformerMapCanvas(w, h)
        val g = h - 1
        m.groundSpans(g, intArrayOf(0, 35), intArrayOf(36, 30), intArrayOf(67, 35), intArrayOf(103, 30), intArrayOf(134, 26))
        m.spikePit(g, 35, 1)
        m.spikePit(g, 66, 1)
        m.spikePit(g, 102, 1)
        m.spawn(2, g)
        repeat(4) { i ->
            val base = 20 + i * 28
            m.islandSupported(base, g - 2, 4, 1, g)
            m.enemy(base + 1, g - 2, PlatformerEnemyType.BAT, 3)
            m.gem(base + 2, g - 3)
        }
        m.spring(90, g - 1)
        m.enemy(110, g - 1, PlatformerEnemyType.CHICKEN, 5)
        m.goal(145, g - 2)
        return def(3, "宝藏前厅", "Vault · 多层平台", m, 0xFF4A148C, 0xFF6A1B9A)
    }

    private fun stage4(): PlatformerLevelDef {
        val w = 170
        val h = 14
        val m = PlatformerMapCanvas(w, h)
        val g = h - 1
        m.groundSpans(g, intArrayOf(0, 28), intArrayOf(29, 24), intArrayOf(54, 28), intArrayOf(83, 24), intArrayOf(108, 28), intArrayOf(137, 33))
        listOf(28, 53, 82, 107).forEach { m.spikePit(g, it, 1) }
        m.spawn(2, g)
        m.enemy(12, g - 1, PlatformerEnemyType.SNAIL, 4)
        m.spikes(25, g - 1, 2)
        m.spring(35, g - 1)
        m.enemy(60, g - 2, PlatformerEnemyType.GHOST, 4)
        m.enemy(75, g - 1, PlatformerEnemyType.SKULL, 3)
        m.gem(88, g - 3)
        m.spring(100, g - 2)
        m.enemy(125, g - 1, PlatformerEnemyType.MUSHROOM, 4)
        m.spikes(140, g - 1, 2)
        m.goal(155, g - 2)
        return def(4, "诅咒宝库", "Cursed · 高密度机关", m, 0xFF880E4F, 0xFFC2185B)
    }

    private fun stage5(): PlatformerLevelDef {
        val w = 180
        val h = 14
        val m = PlatformerMapCanvas(w, h)
        val g = h - 1
        m.groundSpans(g, intArrayOf(0, 32), intArrayOf(33, 28), intArrayOf(62, 30), intArrayOf(93, 28), intArrayOf(122, 30), intArrayOf(153, 27))
        listOf(32, 61, 92, 121, 152).forEach { m.spikePit(g, it, 1) }
        m.spawn(2, g)
        repeat(5) { i ->
            val x = 18 + i * 32
            m.enemy(x, g - 1, PlatformerEnemyType.entries[i % PlatformerEnemyType.entries.size], 4)
            m.gem(x + 4, g - 2)
        }
        m.spring(80, g - 1)
        m.spring(130, g - 2)
        m.islandSupported(160, g - 3, 6, 2, g)
        m.goal(170, g - 3)
        return def(5, "猎人传说", "Legend · 终极挑战", m, 0xFFBF360C, 0xFFE65100)
    }

    private fun def(
        id: Int,
        title: String,
        subtitle: String,
        m: PlatformerMapCanvas,
        skyTop: Long,
        skyBottom: Long,
    ) = PlatformerLevelDef(
        id = id,
        title = title,
        subtitle = subtitle,
        theme = PlatformerTheme.SPOOKY,
        tilesetPack = PlatformerTilesetPack.GRAVEYARD_PACK,
        skyTop = skyTop,
        skyBottom = skyBottom,
        rows = m.toRows(),
        enemySpawns = m.enemySpawns(),
        seriesId = "treasure_hunter",
        seriesOrder = id,
    )
}
