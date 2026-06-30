package com.example.funlife.game.platformer

/** 在网格上绘制关卡。y=0 为顶行，y 增大向下。 */
class PlatformerMapCanvas(val width: Int, val height: Int) {
    private val grid = Array(height) { CharArray(width) { '.' } }

    fun set(x: Int, y: Int, ch: Char) {
        if (x in 0 until width && y in 0 until height) grid[y][x] = ch
    }

    fun fillRect(x: Int, y: Int, w: Int, h: Int, ch: Char) {
        for (dy in 0 until h) for (dx in 0 until w) set(x + dx, y + dy, ch)
    }

    /** 实心岛：顶行可选平台 `-`，其余 `#`。 */
    fun island(x: Int, y: Int, w: Int, bodyH: Int, platformTop: Boolean = false) {
        if (platformTop && bodyH > 0) {
            fillRect(x, y, w, 1, '-')
            if (bodyH > 1) fillRect(x, y + 1, w, bodyH - 1, '#')
        } else {
            fillRect(x, y, w, bodyH.coerceAtLeast(1), '#')
        }
    }

    /** 从 fromY 到 toY（含）画竖柱。 */
    fun pillar(x: Int, fromY: Int, toY: Int) {
        val y0 = minOf(fromY, toY)
        val y1 = maxOf(fromY, toY)
        for (y in y0..y1) set(x, y, '#')
    }

    fun gem(x: Int, y: Int) = set(x, y, 'G')
    fun deco(x: Int, y: Int) = set(x, y, '+')
    fun goal(x: Int, y: Int) = set(x, y, 'O')
    fun spawn(x: Int, y: Int) = set(x, y, '@')
    fun crate(x: Int, y: Int) = set(x, y, 'C')
    fun backdrop(x: Int, y: Int) = set(x, y, 'T')
    fun spike(x: Int, y: Int) = set(x, y, '^')
    fun spikes(x: Int, y: Int, w: Int) = fillRect(x, y, w, 1, '^')
    fun spring(x: Int, y: Int) = set(x, y, 'S')

    private val enemyMarks = mutableListOf<PlatformerEnemySpawn>()
    private val trapMarks = mutableListOf<PlatformerTrapSpawn>()

    fun enemy(x: Int, y: Int, type: PlatformerEnemyType, patrol: Int = 4) {
        enemyMarks += PlatformerEnemySpawn(x, y, type, patrol)
    }

    fun trap(
        x: Int,
        y: Int,
        type: PlatformerTrapType,
        span: Int = 4,
        axis: PlatformerTrapAxis = PlatformerTrapAxis.HORIZONTAL,
        cycle: Float = 2.4f,
        facingRight: Boolean = true,
    ) {
        trapMarks += PlatformerTrapSpawn(x, y, type, span, axis, cycle, facingRight = facingRight)
    }

    fun turret(x: Int, y: Int, facingRight: Boolean = true) =
        trap(x, y, PlatformerTrapType.TURRET, span = 1, facingRight = facingRight)

    fun laserH(x: Int, y: Int, span: Int, cycle: Float = 2.2f) =
        trap(x, y, PlatformerTrapType.LASER, span, PlatformerTrapAxis.HORIZONTAL, cycle)

    fun laserV(x: Int, y: Int, span: Int, cycle: Float = 2.6f) =
        trap(x, y, PlatformerTrapType.LASER, span, PlatformerTrapAxis.VERTICAL, cycle)

    fun movingSpike(x: Int, y: Int, span: Int = 3) =
        trap(x, y, PlatformerTrapType.MOVING_SPIKE, span)

    fun crusher(x: Int, y: Int) =
        trap(x, y, PlatformerTrapType.CRUSHER, span = 1, cycle = 3f)

    fun enemySpawns(): List<PlatformerEnemySpawn> = enemyMarks.toList()
    fun trapSpawns(): List<PlatformerTrapSpawn> = trapMarks.toList()

    /** 沟壑上方铺连通踏脚石（低路保底）。 */
    fun bridgeGap(gapX: Int, groundY: Int, gapWidth: Int) {
        val w = gapWidth.coerceIn(1, PlatformerLevelDesign.MAX_GROUND_GAP + 1)
        fillRect(gapX, groundY, w, 1, '-')
        if (w >= 2) fillRect(gapX + 1, groundY - 1, 1, 1, '-')
    }

    /** 高低双路线：低路踏脚石 + 高路平台 + 弹簧捷径。 */
    fun dualRoute(baseX: Int, groundY: Int, gapWidth: Int = 2) {
        bridgeGap(baseX, groundY, gapWidth)
        islandSupported(baseX + gapWidth + 2, groundY - 2, 4, 1, groundY)
        spring(baseX + gapWidth + 1, groundY - 1)
    }

    /** 地面缺口底部铺地刺（gap 中间 1~2 格）。 */
    fun spikePit(groundY: Int, gapStart: Int, gapWidth: Int) {
        val w = gapWidth.coerceIn(1, 3)
        val x = gapStart + (gapWidth - w) / 2
        spikes(x, groundY, w)
    }

    /** 浮岛 + 中心支柱落地，避免视觉悬空。 */
    fun islandSupported(x: Int, y: Int, w: Int, bodyH: Int, groundY: Int, platformTop: Boolean = false) {
        island(x, y, w, bodyH, platformTop)
        if (w <= 0 || bodyH <= 0) return
        val bodyTop = if (platformTop && bodyH > 0) y + 1 else y
        val supportX = x + w / 2
        pillar(supportX, bodyTop, groundY)
    }

    /** 地面多段，每段之间留 gap 格空隙（≤ [PlatformerLevelDesign.MAX_GROUND_GAP]）。 */
    fun groundSpans(groundY: Int, vararg spans: IntArray) {
        spans.forEach { span ->
            require(span.size == 2) { "span 为 intArrayOf(start, width)" }
            fillRect(span[0], groundY, span[1], 1, '#')
        }
    }

    /** 低高度阶梯平台（每级 1 格抬升）。 */
    fun gentleSteps(startX: Int, groundY: Int, count: Int, width: Int = 3) {
        for (i in 0 until count) {
            val lift = (i + 1).coerceAtMost(PlatformerLevelDesign.LOW_ROUTE_MAX_LIFT)
            val y = groundY - lift
            val x = startX + i * (width + 1)
            if (lift == 1) {
                islandSupported(x, y, width, 1, groundY)
            } else {
                fillRect(x, y, width, 1, '#')
                pillar(x + width / 2, y, groundY)
            }
        }
    }

    fun toRows(): List<String> = grid.map { String(it) }

    fun importFromCells(cells: Array<PlatformerCell>, width: Int, height: Int) {
        for (y in 0 until height.coerceAtMost(this.height)) {
            for (x in 0 until width.coerceAtMost(this.width)) {
                val cell = cells[y * width + x]
                set(x, y, when (cell) {
                    PlatformerCell.AIR -> '.'
                    PlatformerCell.SOLID -> '#'
                    PlatformerCell.PLATFORM -> '-'
                    PlatformerCell.SPAWN -> '@'
                    PlatformerCell.GEM -> 'G'
                    PlatformerCell.GOAL -> 'O'
                    PlatformerCell.DECO -> '+'
                    PlatformerCell.CRATE -> 'C'
                    PlatformerCell.BACKDROP -> 'T'
                    PlatformerCell.SPIKE -> '^'
                    PlatformerCell.SPRING -> 'S'
                })
            }
        }
    }

    /** 低层平台走廊：地面 + 1 格抬升踏板。 */
    fun lowPlateauLane(startX: Int, laneW: Int, groundY: Int) {
        groundSpans(groundY, intArrayOf(startX, laneW - 4))
        for (i in 0 until 3) {
            val x = startX + 4 + i * 6
            islandSupported(x, groundY - 1, 4, 1, groundY)
            gem(x + 2, groundY - 2)
        }
        deco(startX + 2, groundY - 1)
    }

    /** 中层廊桥：地面断续 + g-2/g-3 平台链。 */
    fun midBridgeLane(startX: Int, laneW: Int, groundY: Int) {
        groundSpans(groundY, intArrayOf(startX, 5), intArrayOf(startX + 10, laneW - 12))
        bridgeGap(startX + 5, groundY, 2)
        islandSupported(startX + 8, groundY - 2, 5, 1, groundY)
        islandSupported(startX + 14, groundY - 3, 4, 1, groundY)
        islandSupported(startX + 20, groundY - 2, 5, 1, groundY)
        gem(startX + 16, groundY - 4)
    }

    /** 高层天路：少地面，g-4~g-6 浮岛链 + 弹簧衔接。 */
    fun highSkywayLane(startX: Int, laneW: Int, groundY: Int) {
        groundSpans(groundY, intArrayOf(startX, 4), intArrayOf(startX + laneW - 8, 6))
        bridgeGap(startX + 4, groundY, 2)
        spring(startX + 5, groundY)
        islandSupported(startX + 8, groundY - 4, 4, 1, groundY)
        islandSupported(startX + 13, groundY - 5, 5, 1, groundY)
        islandSupported(startX + 19, groundY - 6, 4, 1, groundY)
        gem(startX + 15, groundY - 6)
        gem(startX + 21, groundY - 7)
    }
}

object PlatformerLevelBuilder {

    fun grassland(): PlatformerLevelDef {
        val w = 130
        val h = 14
        val m = PlatformerMapCanvas(w, h)
        val ground = h - 1

        // 起伏地面 + 沟壑（22~25、44~47 等为真实空隙）
        m.fillRect(0, ground, 22, 1, '#')
        m.fillRect(26, ground, 18, 1, '#')
        m.fillRect(48, ground, 14, 1, '#')
        m.fillRect(66, ground, 20, 1, '#')
        m.fillRect(90, ground, 16, 1, '#')
        m.fillRect(110, ground, 20, 1, '#')
        m.spawn(2, ground)
        m.backdrop(6, ground - 1)
        m.backdrop(18, ground - 1)
        m.backdrop(40, ground - 2)

        // 左侧教学浮岛
        m.island(8, ground - 3, 4, 2)
        m.gem(10, ground - 4)
        m.deco(9, ground - 3)

        // 低层平台群
        m.island(20, ground - 2, 5, 1)
        m.island(30, ground - 3, 4, 2)
        m.pillar(31, ground - 2, ground - 1)
        m.pillar(32, ground - 2, ground - 1)
        m.gem(32, ground - 4)

        m.island(38, ground - 4, 3, 1)
        m.island(44, ground - 2, 2, 1)
        m.island(48, ground - 2, 2, 1)
        m.gem(47, ground - 3)
        m.deco(45, ground - 2)

        // 中层：柱廊结构（桥面 3 格，中间留 1 格缝）
        m.island(54, ground - 5, 2, 2)
        m.island(57, ground - 5, 2, 2)
        m.pillar(54, ground - 4, ground - 1)
        m.pillar(57, ground - 4, ground - 1)
        m.gem(56, ground - 6)

        m.island(62, ground - 3, 4, 1)
        m.island(68, ground - 6, 3, 2)
        m.pillar(69, ground - 5, ground - 1)
        m.gem(69, ground - 7)

        // 高空捷径 vs 地面绕路
        m.island(74, ground - 4, 8, 1)
        m.gem(78, ground - 5)
        m.gem(80, ground - 5)

        m.island(84, ground - 7, 4, 2)
        m.pillar(85, ground - 6, ground - 1)
        m.pillar(86, ground - 6, ground - 1)
        m.gem(86, ground - 8)

        // 终段高台
        m.island(94, ground - 3, 3, 2)
        m.island(98, ground - 3, 2, 2)
        m.island(102, ground - 5, 2, 2)
        m.island(105, ground - 5, 2, 2)
        m.gem(104, ground - 6)

        m.island(110, ground - 8, 6, 3)
        m.pillar(111, ground - 7, ground - 1)
        m.pillar(112, ground - 7, ground - 1)
        m.pillar(113, ground - 7, ground - 1)
        m.pillar(114, ground - 7, ground - 1)
        m.gem(112, ground - 9)
        m.deco(113, ground - 8)

        // 沟壑连通踏脚石（低路保底，高路仍可选）
        m.bridgeGap(22, ground, 4)
        m.bridgeGap(44, ground, 3)
        m.bridgeGap(62, ground, 4)
        m.dualRoute(88, ground, 2)

        // 教学机关带
        m.turret(35, ground - 3, facingRight = true)
        m.laserH(50, ground - 4, 4)
        m.movingSpike(72, ground - 5, 3)
        m.enemy(40, ground - 1, PlatformerEnemyType.SLIME, 3)

        m.island(118, ground - 6, 4, 2)
        m.goal(120, ground - 7)
        m.gem(122, ground - 7)

        return PlatformerLevelEnhancer.finalize(
            PlatformerLevelDef(
                id = 1,
                title = "欢迎公园",
                subtitle = "Grassland · 浮岛探索",
                theme = PlatformerTheme.GRASS,
                skyTop = 0xFF87CEEB,
                skyBottom = 0xFFB0E0FF,
                parallaxHill = 0xFF5DAD5D,
                rows = m.toRows(),
            ),
            m, ground, w,
        )
    }

    fun dungeon(): PlatformerLevelDef {
        val w = 130
        val h = 14
        val m = PlatformerMapCanvas(w, h)
        val g = h - 1

        // 分段地面 + 裂隙踏脚石（低路始终可达）
        m.groundSpans(
            g,
            intArrayOf(0, 26),
            intArrayOf(28, 22),
            intArrayOf(52, 24),
            intArrayOf(78, 24),
            intArrayOf(104, 24),
        )
        m.bridgeGap(26, g, 2)
        m.bridgeGap(50, g, 2)
        m.bridgeGap(76, g, 2)
        m.bridgeGap(102, g, 2)
        m.spawn(2, g)
        m.backdrop(8, g - 1)

        // 区段 A：入门低路（贴地踏板 + 弹簧上二层）
        m.gentleSteps(6, g, 3, 3)
        m.gem(10, g - 2)
        m.fillRect(14, g - 1, 4, 1, '-')
        m.spring(18, g - 1)
        m.islandSupported(20, g - 3, 5, 1, g)
        m.gem(22, g - 4)

        // 区段 B：柱廊（矮柱不挡地面，中层岛链）
        m.islandSupported(30, g - 2, 6, 1, g)
        m.islandSupported(38, g - 3, 5, 1, g)
        m.fillRect(44, g - 1, 4, 1, '-')
        m.spring(48, g - 1)
        for (y in g - 5 until g) m.set(54, y, '#')
        m.islandSupported(56, g - 2, 8, 1, g)
        m.gem(60, g - 3)
        m.islandSupported(66, g - 3, 4, 1, g)

        // 区段 C：竖井攀登（高路奖励 + 低路踏板）
        m.islandSupported(74, g - 4, 6, 1, g)
        m.spring(80, g - 2)
        m.fillRect(82, g - 1, 5, 1, '-')
        m.islandSupported(88, g - 3, 6, 1, g)
        m.gem(90, g - 4)
        m.islandSupported(96, g - 2, 5, 1, g)

        // 区段 D：终塔
        m.gentleSteps(102, g, 3, 3)
        m.islandSupported(110, g - 5, 8, 2, g)
        m.goal(112, g - 6)
        m.gem(114, g - 6)
        m.deco(111, g - 5)

        // 机关（抬高，不封死低路）
        m.laserH(36, g - 5, 4, cycle = 2.4f)
        m.turret(62, g - 3, facingRight = true)
        m.movingSpike(42, g - 6, 3)
        m.crusher(78, g - 5)
        m.enemy(24, g - 1, PlatformerEnemyType.SLIME, 3)
        m.enemy(58, g - 2, PlatformerEnemyType.MUSHROOM, 3)

        return PlatformerLevelEnhancer.finalize(
            PlatformerLevelDef(
                id = 2, title = "石牢迷城", subtitle = "Dungeon · 竖井攀登",
                theme = PlatformerTheme.METAL, skyTop = 0xFF2A2A35, skyBottom = 0xFF3D3D4A,
                rows = m.toRows(),
            ),
            m, g, w,
        )
    }

    fun desert(): PlatformerLevelDef {
        val w = 130
        val h = 14
        val m = PlatformerMapCanvas(w, h)
        val g = h - 1
        m.groundSpans(
            g,
            intArrayOf(0, 18),
            intArrayOf(22, 16),
            intArrayOf(42, 20),
            intArrayOf(66, 18),
            intArrayOf(88, 22),
            intArrayOf(114, 16),
        )
        m.spawn(2, g)
        m.backdrop(6, g - 1)

        // 浮岛阶梯（高路）
        for (i in 0 until 5) {
            val bx = 16 + i * 20
            val by = g - 2 - (i % 3)
            m.islandSupported(bx, by, 5 + (i % 2), 2, g)
            m.gem(bx + 2, by - 1)
        }
        m.islandSupported(55, g - 6, 8, 3, g)
        m.gem(58, g - 7)
        m.islandSupported(72, g - 4, 6, 2, g)
        m.islandSupported(82, g - 7, 5, 2, g)
        m.gem(84, g - 8)
        m.islandSupported(92, g - 5, 7, 2, g)
        m.islandSupported(104, g - 8, 6, 3, g)
        m.goal(107, g - 9)
        m.gem(109, g - 9)

        // 低路连通：宽踏脚石 + 中层踏板链
        m.bridgeGap(18, g, 4)
        m.fillRect(20, g - 1, 4, 1, '-')
        m.spring(24, g - 1)
        m.dualRoute(38, g, 3)
        m.fillRect(44, g - 1, 8, 1, '-')
        m.islandSupported(48, g - 2, 4, 1, g)
        m.bridgeGap(62, g, 3)
        m.fillRect(64, g - 1, 4, 1, '-')
        m.dualRoute(84, g, 2)
        m.gentleSteps(108, g, 3, 3)

        // 机关（抬高，留跳跃窗口）
        m.laserH(30, g - 5, 4)
        m.turret(50, g - 4, facingRight = false)
        m.movingSpike(58, g - 7, 3)
        m.crusher(80, g - 6)
        m.spring(70, g - 1)

        // 敌人：贴地 / 贴台生成
        m.enemy(30, g - 1, PlatformerEnemyType.SNAIL, 2)
        m.enemy(58, g - 6, PlatformerEnemyType.MUSHROOM, 3)
        m.enemy(76, g - 1, PlatformerEnemyType.SLIME, 3)
        m.enemy(98, g - 1, PlatformerEnemyType.CHICKEN, 4)

        return PlatformerLevelEnhancer.finalize(
            PlatformerLevelDef(
                id = 3, title = "赤岩峡谷", subtitle = "Canyon · 台地跳跃",
                theme = PlatformerTheme.DESERT, skyTop = 0xFFF4A460, skyBottom = 0xFFFFE4B5,
                parallaxHill = 0xFF8B4513, rows = m.toRows(),
            ),
            m, g, w,
        )
    }

    fun spooky(): PlatformerLevelDef {
        val w = 130
        val h = 14
        val m = PlatformerMapCanvas(w, h)
        val g = h - 1
        m.fillRect(0, g, w, 1, '#')
        m.spawn(2, g)

        // 迷宫通道
        for (y in g - 8 until g) m.set(10, y, '#')
        for (y in g - 6 until g) m.set(20, y, '#')
        m.island(12, g - 4, 6, 1)
        m.island(22, g - 6, 5, 2)
        m.gem(24, g - 7)
        for (y in g - 9 until g) m.set(30, y, '#')
        m.island(32, g - 5, 4, 1)
        m.island(38, g - 8, 6, 2)
        m.gem(41, g - 9)

        for (y in g - 7 until g) m.set(48, y, '#')
        m.island(50, g - 3, 8, 1)
        m.island(62, g - 6, 5, 2)
        m.gem(64, g - 7)
        for (y in g - 10 until g) m.set(72, y, '#')
        m.island(74, g - 7, 6, 2)
        m.island(84, g - 4, 5, 1)
        m.gem(86, g - 5)

        m.island(92, g - 9, 8, 3)
        m.goal(96, g - 10)
        m.gem(98, g - 10)
        for (y in g - 8 until g) m.set(104, y, '#')
        m.island(106, g - 5, 6, 2)

        // 迷宫高低双路 + 弹簧捷径
        m.spring(18, g - 4)
        m.dualRoute(28, g, 2)
        m.islandSupported(66, g - 2, 4, 1, g)
        m.spring(80, g - 3)
        m.bridgeGap(100, g, 2)

        m.laserH(35, g - 7, 4)
        m.turret(52, g - 4, facingRight = true)
        m.movingSpike(78, g - 6, 3)
        m.crusher(88, g - 8)
        m.laserV(72, g - 9, 4)
        m.enemy(44, g - 1, PlatformerEnemyType.GHOST, 4)
        m.enemy(58, g - 3, PlatformerEnemyType.SKULL, 3)
        m.spikes(50, g, 2)

        return PlatformerLevelEnhancer.finalize(
            PlatformerLevelDef(
                id = 4, title = "幽夜密林", subtitle = "Spooky · 狭道迷宫",
                theme = PlatformerTheme.SPOOKY, skyTop = 0xFF0D1B2A, skyBottom = 0xFF1B263B,
                rows = m.toRows(),
            ),
            m, g, w,
        )
    }

    fun ice(): PlatformerLevelDef {
        val w = 130
        val h = 14
        val m = PlatformerMapCanvas(w, h)
        val g = h - 1
        m.fillRect(0, g, 25, 1, '#')
        m.fillRect(35, g, 20, 1, '#')
        m.fillRect(60, g, 18, 1, '#')
        m.fillRect(85, g, 22, 1, '#')
        m.fillRect(112, g, 18, 1, '#')
        m.spikePit(g, 25, 1)
        m.spikePit(g, 55, 1)
        m.spikePit(g, 78, 1)
        m.spikePit(g, 107, 1)
        m.spawn(2, g)

        var x = 8
        repeat(8) { i ->
            m.fillRect(x, g - 2 - (i % 3), 3, 1, '-')
            m.gem(x + 1, g - 3 - (i % 3))
            x += 14
        }
        m.island(70, g - 5, 4, 2)
        m.spring(68, g - 1)
        m.gem(71, g - 6)
        m.island(95, g - 7, 5, 2)
        m.spikes(96, g - 7, 2)
        m.goal(98, g - 8)
        m.gem(100, g - 8)

        // 宽裂谷踏脚石链（保证低路可达）
        m.fillRect(26, g, 2, 1, '-')
        m.fillRect(29, g - 1, 2, 1, '-')
        m.fillRect(32, g, 2, 1, '-')
        m.bridgeGap(55, g, 1)
        m.bridgeGap(78, g, 1)
        m.dualRoute(85, g, 2)
        m.bridgeGap(107, g, 1)
        m.spring(28, g - 1)
        m.spring(56, g - 1)

        m.laserH(45, g - 4, 5)
        m.turret(62, g - 4, facingRight = false)
        m.movingSpike(82, g - 6, 3)
        m.crusher(88, g - 7)
        m.enemy(38, g - 1, PlatformerEnemyType.SNAIL, 2)
        m.enemy(72, g - 2, PlatformerEnemyType.BAT, 5)

        return PlatformerLevelEnhancer.finalize(
            PlatformerLevelDef(
                id = 5, title = "冰封绝壁", subtitle = "Ice · 极限连跳",
                theme = PlatformerTheme.ICE, skyTop = 0xFFB0D4E8, skyBottom = 0xFFE8F4FC,
                parallaxHill = 0xFF6B9DB8, rows = m.toRows(),
            ),
            m, g, w,
        )
    }

    fun fortress(): PlatformerLevelDef {
        val w = 130
        val h = 14
        val m = PlatformerMapCanvas(w, h)
        val g = h - 1
        m.fillRect(0, g, w, 1, '#')
        m.spawn(2, g)

        m.island(6, g - 3, 5, 2)
        m.island(16, g - 5, 6, 2)
        m.gem(18, g - 6)
        m.island(28, g - 4, 4, 2)
        m.island(36, g - 7, 8, 3)
        for (x in 37..42) m.pillar(x, g - 6, g - 1)
        m.gem(39, g - 8)

        m.island(48, g - 3, 10, 1)
        m.spikes(52, g - 3, 2)
        m.island(62, g - 6, 6, 2)
        m.spring(60, g - 3)
        m.island(72, g - 4, 5, 2)
        m.gem(74, g - 5)
        m.island(82, g - 8, 7, 3)
        m.spike(85, g - 8)
        m.gem(85, g - 9)

        m.island(94, g - 5, 8, 2)
        m.island(106, g - 7, 6, 3)
        m.goal(109, g - 8)
        m.gem(111, g - 8)
        m.deco(108, g - 7)

        // 要塞机关走廊
        m.laserH(22, g - 5, 4)
        m.turret(38, g - 6, facingRight = true)
        m.movingSpike(55, g - 4, 4)
        m.crusher(68, g - 5)
        m.laserV(78, g - 7, 4)
        m.turret(92, g - 6, facingRight = false)
        m.dualRoute(48, g, 2)
        m.spring(70, g - 3)
        m.enemy(32, g - 1, PlatformerEnemyType.MUSHROOM, 4)
        m.enemy(78, g - 2, PlatformerEnemyType.CHICKEN, 5)
        m.spikes(58, g, 2)

        return PlatformerLevelEnhancer.finalize(
            PlatformerLevelDef(
                id = 6, title = "边境要塞", subtitle = "Fortress · 综合挑战",
                theme = PlatformerTheme.FORTRESS, skyTop = 0xFF87CEEB, skyBottom = 0xFFB0D4E8,
                parallaxHill = 0xFF6B8E6B, rows = m.toRows(),
            ),
            m, g, w,
        )
    }

    /** 第 7 关：DesertTileset — 双路线（低路保底 + 高路奖励），180 格长线关卡 */
    fun desertPackLevel(): PlatformerLevelDef {
        val w = 180
        val h = 14
        val m = PlatformerMapCanvas(w, h)
        val g = h - 1

        m.groundSpans(
            g,
            intArrayOf(0, 28),
            intArrayOf(29, 22),
            intArrayOf(51, 20),
            intArrayOf(72, 22),
            intArrayOf(95, 20),
            intArrayOf(116, 22),
            intArrayOf(139, 20),
            intArrayOf(160, 20),
        )
        // 地面裂隙底部地刺
        m.spikePit(g, 28, 1)
        m.spikePit(g, 71, 1)
        m.spikePit(g, 94, 1)
        m.spikePit(g, 115, 1)
        m.spikePit(g, 138, 1)
        m.spikePit(g, 159, 1)
        m.spawn(2, g)
        m.backdrop(6, g - 1)
        m.backdrop(34, g - 1)
        m.crate(9, g)
        m.deco(10, g - 1)

        // —— 区段 A：教学 (0~45) ——
        m.fillRect(14, g - 1, 3, 1, '-')
        m.crate(18, g - 1)
        m.gem(19, g - 2)
        m.gentleSteps(24, g, 3)
        m.spike(27, g) // 起跳点前地刺，需踩箱越过
        m.gem(30, g - 2)
        m.fillRect(38, g - 1, 2, 1, '-')
        m.spike(40, g - 1) // 窄台地刺
        m.fillRect(41, g - 1, 1, 1, '-')
        m.pillar(40, g - 1, g - 1)
        m.spring(43, g - 2) // 弹簧送至高路
        m.islandSupported(44, g - 2, 4, 1, g)
        m.gem(46, g - 3)

        // —— 区段 B：流沙裂谷 (45~85) ——
        m.fillRect(52, g - 1, 3, 1, '-')
        m.crate(56, g - 1)
        m.islandSupported(60, g - 2, 5, 2, g)
        m.deco(62, g - 3)
        m.gem(63, g - 3)
        m.fillRect(69, g - 1, 1, 1, '-')
        m.spike(70, g - 1)
        m.fillRect(71, g - 1, 1, 1, '-')
        m.islandSupported(75, g - 2, 3, 1, g)
        m.fillRect(77, g - 1, 1, 1, '-')
        m.spring(78, g - 1) // 弹簧越过宽裂谷
        m.fillRect(79, g - 1, 4, 1, '-')
        m.gem(81, g - 2)
        m.backdrop(88, g - 1)
        m.islandSupported(90, g - 2, 4, 2, g)
        m.crate(92, g - 2)
        m.spikes(93, g - 2, 2) // 浮岛地刺带
        m.islandSupported(96, g - 1, 4, 1, g)

        // —— 区段 C：遗迹柱廊 (85~125) ——
        m.islandSupported(104, g - 1, 4, 1, g)
        m.fillRect(109, g - 2, 2, 1, '-')
        m.spike(111, g - 2)
        m.fillRect(112, g - 2, 1, 1, '-')
        m.gem(112, g - 3)
        m.fillRect(116, g - 1, 2, 1, '-')
        m.spike(118, g - 1)
        m.fillRect(119, g - 1, 1, 1, '-')
        m.crate(120, g - 1)
        m.spring(125, g - 2)
        m.islandSupported(126, g - 2, 5, 2, g)
        m.deco(128, g - 3)
        m.gem(129, g - 3)
        m.islandSupported(134, g - 1, 3, 1, g)
        m.spikes(137, g - 1, 2)
        m.islandSupported(139, g - 2, 4, 1, g)
        m.gem(141, g - 3)

        // —— 区段 D：高路奖励 (125~155) ——
        m.islandSupported(146, g - 3, 3, 1, g)
        m.gem(147, g - 4)
        m.spring(150, g - 2)
        m.fillRect(151, g - 2, 3, 1, '-')
        m.spike(153, g - 2)
        m.islandSupported(155, g - 3, 4, 1, g)
        m.gem(157, g - 4)
        m.islandSupported(162, g - 2, 2, 1, g)
        m.crate(163, g - 2)
        m.spikes(164, g - 2, 2)

        // —— 区段 E：终点遗迹 (155~180) ——
        m.islandSupported(168, g - 1, 4, 1, g)
        m.islandSupported(173, g - 2, 5, 2, g)
        m.backdrop(175, g - 1)
        m.deco(176, g - 3)
        m.goal(177, g - 2)
        m.gem(178, g - 2)

        return PlatformerLevelEnhancer.finalize(
            PlatformerLevelDef(
                id = 7,
                title = "烈日遗迹",
                subtitle = "Desert Pack · 机关挑战",
                theme = PlatformerTheme.PACK_DESERT,
                tilesetPack = PlatformerTilesetPack.DESERT_PACK,
                skyTop = 0xFFE8B86D,
                skyBottom = 0xFFFFF0C8,
                rows = m.toRows(),
            ),
            m, g, w,
        )
    }

    /** 第 8 关：WinterTileset — 雪原主路，180 格冰桥长廊 */
    fun winterPackLevel(): PlatformerLevelDef {
        val w = 180
        val h = 14
        val m = PlatformerMapCanvas(w, h)
        val g = h - 1

        m.groundSpans(
            g,
            intArrayOf(0, 24),
            intArrayOf(25, 20),
            intArrayOf(46, 18),
            intArrayOf(65, 20),
            intArrayOf(86, 18),
            intArrayOf(105, 20),
            intArrayOf(126, 18),
            intArrayOf(145, 20),
            intArrayOf(166, 14),
        )
        // 冰裂隙地刺
        m.spikePit(g, 24, 1)
        m.spikePit(g, 45, 1)
        m.spikePit(g, 64, 1)
        m.spikePit(g, 85, 1)
        m.spikePit(g, 104, 1)
        m.spikePit(g, 125, 1)
        m.spikePit(g, 144, 1)
        m.spikePit(g, 165, 1)
        m.spawn(2, g)
        m.backdrop(5, g - 1)
        m.crate(7, g)
        m.deco(8, g - 1)

        // —— 区段 A：入门 (0~40) ——
        m.gentleSteps(12, g, 4)
        m.gem(15, g - 2)
        m.spike(23, g) // 裂隙前地刺
        m.fillRect(22, g - 1, 2, 1, '-')
        m.pillar(24, g - 1, g - 1)
        m.spring(25, g) // 弹簧越过首段裂隙
        m.crate(26, g - 1)
        m.islandSupported(32, g - 1, 4, 1, g)
        m.deco(34, g - 2)
        m.gem(35, g - 2)
        m.fillRect(40, g - 1, 2, 1, '-')
        m.spike(42, g - 1)
        m.islandSupported(44, g - 2, 3, 1, g)
        m.gem(45, g - 3)

        // —— 区段 B：浮冰连跳 (40~80) ——
        m.islandSupported(54, g - 1, 5, 1, g)
        m.crate(56, g - 1)
        m.spikes(58, g - 1, 2)
        m.fillRect(61, g - 1, 3, 1, '-')
        m.spring(64, g - 2)
        m.islandSupported(65, g - 2, 4, 1, g)
        m.gem(67, g - 3)
        m.islandSupported(72, g - 1, 2, 1, g)
        m.spike(74, g - 1)
        m.islandSupported(77, g - 2, 4, 1, g)
        m.backdrop(79, g - 1)
        m.spring(81, g - 1)
        m.fillRect(82, g - 1, 4, 1, '-')
        m.gem(84, g - 2)

        // —— 区段 C：雪林通道 (80~120) ——
        m.islandSupported(92, g - 2, 4, 2, g)
        m.deco(94, g - 3)
        m.gem(95, g - 3)
        m.spikes(97, g - 2, 2)
        m.islandSupported(100, g - 1, 5, 1, g)
        m.crate(102, g - 1)
        m.fillRect(107, g - 2, 2, 1, '-')
        m.spike(109, g - 2)
        m.islandSupported(110, g - 2, 2, 1, g)
        m.gem(110, g - 3)
        m.spring(113, g - 1)
        m.fillRect(114, g - 1, 4, 1, '-')
        m.islandSupported(120, g - 1, 4, 1, g)
        m.deco(122, g - 2)

        // —— 区段 D：冰屋前廊 (120~155) ——
        m.islandSupported(128, g - 2, 4, 1, g)
        m.spike(132, g - 2)
        m.islandSupported(134, g - 1, 3, 1, g)
        m.gem(136, g - 2)
        m.spring(139, g - 2)
        m.fillRect(140, g - 1, 2, 1, '-')
        m.spike(142, g - 1)
        m.islandSupported(146, g - 2, 5, 2, g)
        m.crate(148, g - 2)
        m.gem(150, g - 3)
        m.backdrop(152, g - 1)
        m.spikes(154, g - 1, 2)
        m.islandSupported(156, g - 1, 4, 1, g)

        // —— 区段 E：冰屋终点 (155~180) ——
        m.islandSupported(162, g - 2, 4, 1, g)
        m.fillRect(168, g - 1, 3, 1, '-')
        m.islandSupported(172, g - 2, 5, 2, g)
        m.deco(174, g - 3)
        m.goal(175, g - 2)
        m.gem(177, g - 2)
        m.backdrop(178, g - 1)

        return PlatformerLevelEnhancer.finalize(
            PlatformerLevelDef(
                id = 8,
                title = "雪国长廊",
                subtitle = "Winter Pack · 冰刺机关",
                theme = PlatformerTheme.PACK_WINTER,
                tilesetPack = PlatformerTilesetPack.WINTER_PACK,
                skyTop = 0xFFB3D9F2,
                skyBottom = 0xFFE8F6FF,
                rows = m.toRows(),
            ),
            m, g, w,
        )
    }

    // —— 外部素材包扩展关卡（9~16）——

    fun forestPackLevel(): PlatformerLevelDef = PlatformerPackLevelFactory.build(
        PlatformerPackLevelFactory.Config(
            id = 9, title = "翡翠林海", subtitle = "Forest · 初探",
            pack = PlatformerTilesetPack.FOREST_PACK, theme = PlatformerTheme.PACK_FOREST,
            skyTop = 0xFF87CEEB, skyBottom = 0xFFE0F4FF,
            seriesId = "forest", seriesOrder = 1, hazardLevel = 1, trapLevel = 1,
            enemies = listOf(PlatformerEnemyType.SLIME, PlatformerEnemyType.CHICKEN),
        ),
    )

    fun forestPackLevelHard(): PlatformerLevelDef = PlatformerPackLevelFactory.build(
        PlatformerPackLevelFactory.Config(
            id = 10, title = "幽林迷踪", subtitle = "Forest · 机关强化",
            pack = PlatformerTilesetPack.FOREST_PACK, theme = PlatformerTheme.PACK_FOREST,
            skyTop = 0xFF6BA3C7, skyBottom = 0xFFB8D9E8,
            seriesId = "forest", seriesOrder = 2, width = 180, hazardLevel = 3, trapLevel = 3,
            enemies = listOf(PlatformerEnemyType.MUSHROOM, PlatformerEnemyType.BAT, PlatformerEnemyType.SNAIL),
        ),
    )

    fun graveyardPackLevel(): PlatformerLevelDef = PlatformerPackLevelFactory.build(
        PlatformerPackLevelFactory.Config(
            id = 11, title = "墓园夜行", subtitle = "Graveyard · 亡灵出没",
            pack = PlatformerTilesetPack.GRAVEYARD_PACK, theme = PlatformerTheme.PACK_GRAVEYARD,
            skyTop = 0xFF1A1A2E, skyBottom = 0xFF3D3D5C,
            seriesId = "graveyard", seriesOrder = 1, hazardLevel = 2, trapLevel = 2,
            enemies = listOf(PlatformerEnemyType.GHOST, PlatformerEnemyType.SKULL),
        ),
    )

    fun graveyardPackLevelDeep(): PlatformerLevelDef = PlatformerPackLevelFactory.build(
        PlatformerPackLevelFactory.Config(
            id = 12, title = "深渊墓穴", subtitle = "Graveyard · 高密度机关",
            pack = PlatformerTilesetPack.GRAVEYARD_PACK, theme = PlatformerTheme.PACK_GRAVEYARD,
            skyTop = 0xFF0D0D18, skyBottom = 0xFF2A2A40,
            seriesId = "graveyard", seriesOrder = 2, width = 180, hazardLevel = 3, trapLevel = 3,
            enemies = listOf(PlatformerEnemyType.GHOST, PlatformerEnemyType.SKULL, PlatformerEnemyType.BAT),
        ),
    )

    fun junglePackLevel(): PlatformerLevelDef = PlatformerPackLevelFactory.build(
        PlatformerPackLevelFactory.Config(
            id = 13, title = "丛林遗迹", subtitle = "Jungle · 藤蔓古道",
            pack = PlatformerTilesetPack.JUNGLE_PACK, theme = PlatformerTheme.PACK_JUNGLE,
            skyTop = 0xFF2E7D32, skyBottom = 0xFFA5D6A7,
            seriesId = "jungle", seriesOrder = 1, hazardLevel = 2, trapLevel = 2,
            enemies = listOf(PlatformerEnemyType.SNAIL, PlatformerEnemyType.CHICKEN, PlatformerEnemyType.BAT),
        ),
    )

    fun scifiPackLevel(): PlatformerLevelDef = PlatformerPackLevelFactory.build(
        PlatformerPackLevelFactory.Config(
            id = 14, title = "科幻站台", subtitle = "Sci-Fi · 激光陷阱",
            pack = PlatformerTilesetPack.SCIFI_PACK, theme = PlatformerTheme.PACK_SCIFI,
            skyTop = 0xFF0A0E27, skyBottom = 0xFF1A237E,
            seriesId = "scifi", seriesOrder = 1, width = 170, hazardLevel = 3, trapLevel = 3,
            enemies = listOf(PlatformerEnemyType.BLUE_BIRD, PlatformerEnemyType.MUSHROOM),
        ),
    )

    fun grottoPackLevel(): PlatformerLevelDef = PlatformerPackLevelFactory.build(
        PlatformerPackLevelFactory.Config(
            id = 15, title = "溶洞逃亡", subtitle = "Grotto · 超级洞穴",
            pack = PlatformerTilesetPack.GROTTO_PACK, theme = PlatformerTheme.PACK_GROTTO,
            skyTop = 0xFF1B2838, skyBottom = 0xFF2C3E50,
            seriesId = "grotto", seriesOrder = 1, hazardLevel = 2, trapLevel = 2,
            enemies = listOf(PlatformerEnemyType.SLIME, PlatformerEnemyType.BAT),
        ),
    )

    fun minimalPackLevel(): PlatformerLevelDef = PlatformerPackLevelFactory.build(
        PlatformerPackLevelFactory.Config(
            id = 16, title = "极简冲刺", subtitle = "Minimal · 节奏跳跃",
            pack = PlatformerTilesetPack.MINIMAL_PACK, theme = PlatformerTheme.PACK_MINIMAL,
            skyTop = 0xFF1565C0, skyBottom = 0xFF42A5F5,
            seriesId = "minimal", seriesOrder = 1, width = 140, sections = 4, hazardLevel = 3, trapLevel = 2,
            enemies = listOf(PlatformerEnemyType.BLUE_BIRD),
        ),
    )
}
