package com.example.funlife.game.platformer

/**
 * 关卡「片段」库：复杂长关由可复用区段拼接，保证低路连通 + 高路奖励。
 *
 * 设计原则（与 [PlatformerLevelDesign] 对齐）：
 * - 每段宽度固定，便于拼接与难度曲线
 * - 低路：踏脚石 / 踏板 / 弹簧，始终可达
 * - 高路：浮岛 + 宝石，可选挑战
 * - 机关抬高，不封死地面主路
 */
object PlatformerSegmentLibrary {

    const val SEGMENT_W = 28

    enum class SegmentKind {
        /** 出生 + 教学踏板 */
        ENTRY,
        /** 地面裂隙 + 踏脚石 */
        GAP,
        /** 低层阶梯 */
        STEPS,
        /** 双路线（低桥 + 高岛） */
        FORK,
        /** 竖井浮岛攀登 */
        TOWER,
        /** 机关走廊（激光/炮台） */
        TRAP_LANE,
        /** 敌人平台战 */
        ENEMY_ROOM,
        /** 弹簧捷径 */
        SPRING_VAULT,
        /** 终点高台 */
        FINALE,
        /** 低层平台走廊 */
        LOW_PLATEAU,
        /** 中层廊桥 */
        MID_BRIDGE,
        /** 高层天路 */
        HIGH_SKYWAY,
        /** 三界串联（低→中→高） */
        TIER_ASCENT,
        /** 剧情 TMX 房间（嵌入长关，32 逻辑格宽） */
        STORY_ROOM,
        /** 高空出生：站在浮岛 g-n 处 */
        SKY_ENTRY,
        /** 连续天路浮岛链 */
        SKY_LANE,
        /** 狭道浮岛 + 下方地刺 */
        SKY_CHASE,
        /** 自高处逐级下降 */
        SKY_DESCENT,
        /** 高空终点台 */
        SKY_FINALE,
    }

    data class SegmentSpec(
        val kind: SegmentKind,
        val hazardLevel: Int = 1,
        val trapLevel: Int = 1,
        val tmxAssetPath: String? = null,
        /** 仅 SKY_ENTRY：出生高度（距地面向上格数） */
        val skySpawnLift: Int = 0,
        /** 仅 SKY_FINALE：终点台高度 */
        val skyFinaleLift: Int = 0,
        /** 出生段布局变体（按关卡 id 轮换，避免每关开头一模一样）。 */
        val layoutVariant: Int = 0,
    )

    /** 在 canvas 上绘制片段，返回片段占用宽度。 */
    fun paint(
        m: PlatformerMapCanvas,
        g: Int,
        startX: Int,
        spec: SegmentSpec,
        sectionIndex: Int,
        endlessFloor: Boolean = false,
    ): Int {
        val x = startX
        val w = segmentWidth(spec.kind)
        if (endlessFloor) {
            m.fillRect(x, g, w, 1, '#')
            if (spec.kind != SegmentKind.ENTRY && spec.kind != SegmentKind.SKY_ENTRY) {
                m.backdrop(x + 6, g - 1)
                if (sectionIndex % 2 == 0) m.backdrop(x + 18, g - 2)
            }
        }
        when (spec.kind) {
            SegmentKind.ENTRY -> paintEntry(m, g, x, w, spec.layoutVariant)
            SegmentKind.GAP -> paintGap(m, g, x, w, spec.hazardLevel)
            SegmentKind.STEPS -> paintSteps(m, g, x, w, spec.hazardLevel)
            SegmentKind.FORK -> paintFork(m, g, x, w, spec.hazardLevel)
            SegmentKind.TOWER -> paintTower(m, g, x, w, spec.trapLevel)
            SegmentKind.TRAP_LANE -> paintTrapLane(m, g, x, w, spec.trapLevel)
            SegmentKind.ENEMY_ROOM -> paintEnemyRoom(m, g, x, w, sectionIndex)
            SegmentKind.SPRING_VAULT -> paintSpringVault(m, g, x, w, spec.trapLevel)
            SegmentKind.FINALE -> paintFinale(m, g, x, w)
            SegmentKind.LOW_PLATEAU -> paintLowPlateau(m, g, x, w, spec.hazardLevel)
            SegmentKind.MID_BRIDGE -> paintMidBridge(m, g, x, w, spec.hazardLevel)
            SegmentKind.HIGH_SKYWAY -> paintHighSkyway(m, g, x, w, spec.trapLevel)
            SegmentKind.TIER_ASCENT -> paintTierAscent(m, g, x, w, spec.hazardLevel, spec.trapLevel)
            SegmentKind.STORY_ROOM -> paintStoryRoomPlaceholder(m, g, x, w)
            SegmentKind.SKY_ENTRY -> paintSkyEntry(
                m, g, x, w,
                spec.skySpawnLift.coerceIn(3, 6),
                spec.layoutVariant,
            )
            SegmentKind.SKY_LANE -> paintSkyLane(m, g, x, w, spec.trapLevel)
            SegmentKind.SKY_CHASE -> paintSkyChase(m, g, x, w, spec.hazardLevel, spec.trapLevel)
            SegmentKind.SKY_DESCENT -> paintSkyDescent(m, g, x, w, spec.trapLevel)
            SegmentKind.SKY_FINALE -> paintSkyFinale(m, g, x, w, spec.skyFinaleLift.coerceIn(4, 6))
        }
        decorateSegmentHazards(m, g, x, w, spec, sectionIndex)
        return w
    }

    /** 所有片段统一 28 格宽（含 STORY_ROOM，TMX 裁切嵌入）。 */
    fun segmentWidth(kind: SegmentKind): Int = SEGMENT_W

    private fun paintStoryRoomPlaceholder(m: PlatformerMapCanvas, g: Int, x: Int, w: Int) {
        m.groundSpans(g, intArrayOf(x, w - 2))
        m.backdrop(x + 4, g - 1)
        m.backdrop(x + w - 6, g - 1)
        m.islandSupported(x + 6, g - 2, 6, 1, g)
        m.islandSupported(x + 16, g - 3, 8, 1, g)
        m.enemy(x + 10, g - 1, PlatformerEnemyType.SKULL, 4)
        m.gem(x + 20, g - 4)
        m.deco(x + 24, g - 2)
    }

    private fun paintEntry(m: PlatformerMapCanvas, g: Int, x: Int, w: Int, variant: Int) {
        when (variant % 4) {
            0 -> {
                m.groundSpans(g, intArrayOf(x, w - 2))
                m.spawn(x + 2, g)
                m.backdrop(x + 4, g - 1)
                m.gentleSteps(x + 6, g, 3, 3)
                m.gem(x + 9, g - 2)
                m.deco(x + 12, g - 1)
            }
            1 -> {
                m.groundSpans(g, intArrayOf(x, 8), intArrayOf(x + 12, w - 14))
                m.spawn(x + 2, g)
                m.bridgeGap(x + 8, g, 2)
                m.spring(x + 9, g - 1)
                m.islandSupported(x + 12, g - 2, 5, 1, g)
                m.gem(x + 14, g - 3)
                m.deco(x + 5, g - 1)
            }
            2 -> {
                m.groundSpans(g, intArrayOf(x, w - 2))
                m.spawn(x + 3, g)
                m.lowPlateauLane(x + 4, 14, g)
                m.gem(x + 10, g - 2)
                m.backdrop(x + 18, g - 1)
            }
            else -> {
                m.groundSpans(g, intArrayOf(x, 6), intArrayOf(x + 10, w - 12))
                m.spawn(x + 2, g)
                m.bridgeGap(x + 6, g, 2)
                m.islandSupported(x + 4, g - 2, 4, 1, g)
                m.gem(x + 5, g - 3)
                m.gentleSteps(x + 12, g, 2, 3)
                m.deco(x + 8, g - 1)
            }
        }
    }

    /** 高空出生：脚下是深渊，仅远端有落地点。 */
    private fun paintSkyEntry(m: PlatformerMapCanvas, g: Int, x: Int, w: Int, lift: Int, variant: Int) {
        val sy = g - lift
        when (variant % 4) {
            0 -> {
                m.groundSpans(g, intArrayOf(x + w - 10, 8))
                m.islandSupported(x + 2, sy, 6, 1, g)
                m.spawn(x + 4, sy)
                m.islandSupported(x + 10, (sy - 1).coerceAtLeast(1), 5, 1, g)
                m.spring(x + 8, sy)
                m.gem(x + 12, (sy - 1).coerceAtLeast(1))
                m.backdrop(x + 6, (sy - 1).coerceAtLeast(1))
            }
            1 -> {
                m.groundSpans(g, intArrayOf(x + w - 8, 6))
                m.islandSupported(x + 3, sy, 5, 1, g)
                m.spawn(x + 5, sy)
                m.islandSupported(x + 10, sy, 4, 1, g)
                m.islandSupported(x + 16, (sy - 1).coerceAtLeast(1), 5, 1, g)
                m.spring(x + 8, sy)
                m.gem(x + 14, (sy - 1).coerceAtLeast(1))
                m.backdrop(x + 12, sy - 1.coerceAtLeast(1))
            }
            2 -> {
                m.groundSpans(g, intArrayOf(x + w - 12, 10))
                m.islandSupported(x + 2, sy, 4, 2, g)
                m.spawn(x + 3, sy)
                m.islandSupported(x + 8, (sy - 1).coerceAtLeast(1), 3, 1, g)
                m.spring(x + 6, sy)
                m.islandSupported(x + 14, sy, 5, 1, g)
                m.gem(x + 10, (sy - 2).coerceAtLeast(1))
                m.deco(x + 16, sy)
            }
            else -> {
                m.groundSpans(g, intArrayOf(x, 4), intArrayOf(x + w - 8, 6))
                m.islandSupported(x + 4, sy, 6, 1, g)
                m.spawn(x + 6, sy)
                m.islandSupported(x + 12, (sy - 2).coerceAtLeast(1), 4, 1, g)
                m.islandSupported(x + 18, (sy - 1).coerceAtLeast(1), 4, 1, g)
                m.spring(x + 10, sy)
                m.gem(x + 14, (sy - 2).coerceAtLeast(1))
                m.backdrop(x + 8, (sy - 1).coerceAtLeast(1))
            }
        }
    }

    /** 连续天路浮岛链（主体在高处）。 */
    private fun paintSkyLane(m: PlatformerMapCanvas, g: Int, x: Int, w: Int, trapLevel: Int) {
        m.groundSpans(g, intArrayOf(x, 4), intArrayOf(x + w - 6, 4))
        m.highSkywayLane(x, w, g)
        if (trapLevel >= 1) m.laserH(x + 10, g - 6, 4)
        if (trapLevel >= 2) m.turret(x + 16, g - 5, facingRight = true)
        if (trapLevel >= 3) m.movingSpike(x + 20, g - 5, 2)
        m.gem(x + 22, g - 6)
    }

    /** 狭道浮岛 + 下方地刺陷阱。 */
    private fun paintSkyChase(m: PlatformerMapCanvas, g: Int, x: Int, w: Int, hazard: Int, trapLevel: Int) {
        m.groundSpans(g, intArrayOf(x, 3))
        for (i in 0 until 4) {
            val px = x + 4 + i * 5
            m.islandSupported(px, g - 4 - (i % 2), 3, 1, g)
            if (i % 2 == 0) m.gem(px + 1, g - 5 - (i % 2))
        }
        if (hazard >= 1) m.spikes(x + 8, g, 4.coerceAtMost(w - 10))
        if (hazard >= 2) m.spikePit(g, x + 12, 2)
        if (trapLevel >= 2) m.movingSpike(x + 14, g - 5, 2)
        if (trapLevel >= 3) m.laserV(x + 18, g - 6, 3)
    }

    /** 自高处逐级下降至中层。 */
    private fun paintSkyDescent(m: PlatformerMapCanvas, g: Int, x: Int, w: Int, trapLevel: Int) {
        m.groundSpans(g, intArrayOf(x + w - 12, 10))
        m.islandSupported(x + 4, g - 5, 4, 1, g)
        m.islandSupported(x + 9, g - 4, 4, 1, g)
        m.islandSupported(x + 14, g - 3, 4, 1, g)
        m.islandSupported(x + 19, g - 2, 5, 1, g)
        m.spring(x + 12, g - 3)
        m.gem(x + 16, g - 4)
        if (trapLevel >= 2) m.laserH(x + 10, g - 5, 3)
        if (trapLevel >= 3) m.crusher(x + 20, g - 2)
    }

    /** 高空终点：终点在浮岛最高处。 */
    private fun paintSkyFinale(m: PlatformerMapCanvas, g: Int, x: Int, w: Int, lift: Int) {
        val fy = g - lift
        m.groundSpans(g, intArrayOf(x, w - 8))
        m.islandSupported(x + 6, fy, 10, 2, g)
        m.goal(x + 10, fy)
        m.gem(x + 12, (fy - 1).coerceAtLeast(1))
        m.gem(x + 14, fy)
        m.backdrop(x + 8, (fy - 1).coerceAtLeast(1))
        m.deco(x + 16, fy)
        m.gentleSteps(x + 2, g, 2, 3)
    }

    private fun paintGap(m: PlatformerMapCanvas, g: Int, x: Int, w: Int, hazard: Int) {
        val gapX = x + 10
        val gapW = 2.coerceAtMost(PlatformerLevelDesign.MAX_GROUND_GAP)
        m.groundSpans(g, intArrayOf(x, 10), intArrayOf(gapX + gapW, w - 12))
        m.bridgeGap(gapX, g, gapW)
        if (hazard >= 1) m.spikePit(g, gapX, gapW)
        if (hazard >= 2) m.spikes(gapX - 1, g, 1)
        m.fillRect(gapX - 2, g - 2, 1, 2, '#')
        m.spring(gapX + gapW + 1, g - 1)
        m.gem(x + 4, g - 2)
        if (hazard >= 2) m.laserH(x + 6, g - 4, 3)
    }

    private fun paintSteps(m: PlatformerMapCanvas, g: Int, x: Int, w: Int, hazard: Int) {
        m.groundSpans(g, intArrayOf(x, w - 2))
        m.gentleSteps(x + 4, g, 4, 3)
        m.gem(x + 8, g - 3)
        m.islandSupported(x + 16, g - 1, 4, 1, g)
        m.gem(x + 20, g - 2)
        if (hazard >= 2) m.spikes(x + 2, g, 2)
    }

    private fun paintFork(m: PlatformerMapCanvas, g: Int, x: Int, w: Int, hazard: Int) {
        val gapX = x + 8
        m.groundSpans(g, intArrayOf(x, 8), intArrayOf(gapX + 2, w - 11))
        m.dualRoute(gapX, g, 2)
        m.islandSupported(gapX + 5, g - 2, 5, 1, g)
        m.gem(gapX + 7, g - 3)
        m.islandSupported(x + 18, g - 3, 4, 1, g)
        m.gem(x + 20, g - 4)
        if (hazard >= 2) m.spike((gapX + 1).coerceAtMost(x + w - 2), g - 1)
    }

    private fun paintTower(m: PlatformerMapCanvas, g: Int, x: Int, w: Int, trapLevel: Int) {
        m.groundSpans(g, intArrayOf(x, 6), intArrayOf(x + 10, w - 12))
        m.bridgeGap(x + 6, g, 2)
        m.islandSupported(x + 4, g - 2, 3, 1, g)
        m.islandSupported(x + 8, g - 4, 4, 1, g)
        m.spring(x + 12, g - 2)
        m.islandSupported(x + 14, g - 3, 4, 1, g)
        m.gem(x + 16, g - 4)
        m.islandSupported(x + 20, g - 5, 3, 1, g)
        m.gem(x + 21, g - 6)
        if (trapLevel >= 2) m.laserH(x + 10, g - 5, 4)
    }

    private fun paintTrapLane(m: PlatformerMapCanvas, g: Int, x: Int, w: Int, trapLevel: Int) {
        m.groundSpans(g, intArrayOf(x, w - 2))
        m.islandSupported(x + 4, g - 2, 6, 1, g)
        m.islandSupported(x + 12, g - 1, 4, 1, g)
        m.islandSupported(x + 18, g - 2, 5, 1, g)
        when {
            trapLevel >= 3 -> {
                m.laserH(x + 6, g - 4, 5, cycle = 1.9f)
                m.turret(x + 14, g - 3, facingRight = true)
                m.movingSpike(x + 20, g - 3, 3)
                m.laserV(x + 10, g - 5, 3, cycle = 2.2f)
                m.crusher(x + 22, g - 2)
            }
            trapLevel >= 2 -> {
                m.laserH(x + 8, g - 4, 4)
                m.turret(x + 16, g - 3, facingRight = false)
                m.movingSpike(x + 20, g - 3, 2)
            }
            else -> {
                m.turret(x + 10, g - 3, facingRight = true)
                m.laserH(x + 14, g - 4, 3)
            }
        }
        m.spikes(x + 6, g, 2)
        m.gem(x + 22, g - 3)
    }

    private fun paintEnemyRoom(m: PlatformerMapCanvas, g: Int, x: Int, w: Int, sectionIndex: Int) {
        m.groundSpans(g, intArrayOf(x, w - 2))
        m.islandSupported(x + 6, g - 2, 8, 1, g)
        m.crate(x + 8, g - 2)
        val types = listOf(
            PlatformerEnemyType.SLIME,
            PlatformerEnemyType.MUSHROOM,
            PlatformerEnemyType.CHICKEN,
            PlatformerEnemyType.SNAIL,
        )
        val t1 = types[sectionIndex % types.size]
        val t2 = types[(sectionIndex + 2) % types.size]
        m.enemy(x + 5, g - 1, t1, 3)
        m.enemy(x + 12, g - 2, t2, 4)
        if (sectionIndex % 2 == 0) {
            m.enemy(x + 18, g - 4, PlatformerEnemyType.BAT, 5)
        }
        m.gem(x + 20, g - 3)
    }

    private fun paintSpringVault(m: PlatformerMapCanvas, g: Int, x: Int, w: Int, trapLevel: Int) {
        m.groundSpans(g, intArrayOf(x, 8), intArrayOf(x + 12, w - 14))
        m.bridgeGap(x + 8, g, 2)
        m.spring(x + 9, g)
        m.islandSupported(x + 14, g - 3, 5, 1, g)
        m.gem(x + 16, g - 4)
        m.islandSupported(x + 20, g - 1, 4, 1, g)
        m.gem(x + 23, g - 2)
        if (trapLevel >= 2) m.laserH(x + 11, g - 5, 3)
        if (trapLevel >= 3) m.turret(x + 18, g - 3, facingRight = true)
    }

    private fun paintFinale(m: PlatformerMapCanvas, g: Int, x: Int, w: Int) {
        m.groundSpans(g, intArrayOf(x, w - 6))
        m.gentleSteps(x + 2, g, 2, 3)
        m.islandSupported(x + 10, g - 4, 8, 2, g)
        m.goal(x + 14, g - 5)
        m.gem(x + 16, g - 5)
        m.deco(x + 12, g - 4)
        m.backdrop(x + 20, g - 1)
    }

    private fun paintLowPlateau(m: PlatformerMapCanvas, g: Int, x: Int, w: Int, hazard: Int) {
        m.lowPlateauLane(x, w, g)
        if (hazard >= 1) m.spike(x + 18, g - 1)
        if (hazard >= 2) m.spikes(x + 10, g, 2)
        m.spring(x + 22, g - 1)
        if (hazard >= 2) m.turret(x + 14, g - 3, facingRight = true)
    }

    private fun paintMidBridge(m: PlatformerMapCanvas, g: Int, x: Int, w: Int, hazard: Int) {
        m.midBridgeLane(x, w, g)
        if (hazard >= 2) {
            m.enemy(x + 10, g - 2, PlatformerEnemyType.SNAIL, 3)
        }
        m.gem(x + 24, g - 3)
    }

    private fun paintHighSkyway(m: PlatformerMapCanvas, g: Int, x: Int, w: Int, trapLevel: Int) {
        m.highSkywayLane(x, w, g)
        if (trapLevel >= 2) m.laserH(x + 12, g - 5, 4)
        if (trapLevel >= 3) m.turret(x + 18, g - 4, facingRight = true)
    }

    private fun paintTierAscent(m: PlatformerMapCanvas, g: Int, x: Int, w: Int, hazard: Int, trapLevel: Int) {
        m.groundSpans(g, intArrayOf(x, 6), intArrayOf(x + 20, w - 8))
        m.lowPlateauLane(x, 12, g)
        m.midBridgeLane(x + 8, 12, g)
        m.islandSupported(x + 14, g - 4, 4, 1, g)
        m.islandSupported(x + 18, g - 5, 4, 1, g)
        m.spring(x + 16, g - 2)
        m.highSkywayLane(x + 12, 14, g)
        if (hazard >= 2) m.enemy(x + 6, g - 1, PlatformerEnemyType.MUSHROOM, 3)
        if (trapLevel >= 2) m.laserH(x + 10, g - 4, 3)
    }

    /** 无尽模式可选片段（不含出生/终点）。 */
    val ENDLESS_POOL: List<SegmentKind> = listOf(
        SegmentKind.GAP,
        SegmentKind.STEPS,
        SegmentKind.LOW_PLATEAU,
        SegmentKind.MID_BRIDGE,
        SegmentKind.HIGH_SKYWAY,
        SegmentKind.FORK,
        SegmentKind.TOWER,
        SegmentKind.TRAP_LANE,
        SegmentKind.ENEMY_ROOM,
        SegmentKind.SPRING_VAULT,
        SegmentKind.TIER_ASCENT,
    )

    fun pickEndlessSpec(
        segmentIndex: Int,
        seed: Long,
        tilesRun: Int = 0,
        biome: PlatformerEndlessBiomes.Biome? = null,
    ): SegmentSpec {
        if (segmentIndex == 0) return SegmentSpec(SegmentKind.ENTRY, 1, 1)
        val rng = kotlin.random.Random(seed + segmentIndex * 31L)
        val distBoost = (tilesRun / PlatformerSegmentLibrary.SEGMENT_W).coerceAtMost(8)
        val hazard = (1 + segmentIndex / 3 + distBoost / 2).coerceAtMost(3)
        val trap = (1 + segmentIndex / 4 + distBoost / 2).coerceAtMost(3)
        val pool = buildList {
            addAll(ENDLESS_POOL)
            if (distBoost >= 2) repeat(2) { add(SegmentKind.TRAP_LANE) }
            if (distBoost >= 3) repeat(2) { add(SegmentKind.ENEMY_ROOM) }
            if (distBoost >= 4) add(SegmentKind.HIGH_SKYWAY)
            if (distBoost < 2) {
                repeat(3) { add(SegmentKind.LOW_PLATEAU) }
                repeat(2) { add(SegmentKind.STEPS) }
            }
        }
        val kind = when {
            biome != null && biome.favoredSegments.isNotEmpty() && rng.nextFloat() < 0.55f ->
                biome.favoredSegments[rng.nextInt(biome.favoredSegments.size)]
            else -> pool[rng.nextInt(pool.size)]
        }
        return SegmentSpec(kind, hazard, trap)
    }

    /** 为片段序列铺设地面连通（段间裂隙 + 踏脚石）。 */
    fun stitchGroundSpans(m: PlatformerMapCanvas, g: Int, totalWidth: Int, segmentCount: Int) {
        val seg = PlatformerSegmentLibrary.SEGMENT_W
        var x = 0
        repeat(segmentCount) { i ->
            val spanW = if (i == segmentCount - 1) totalWidth - x else seg - 2
            if (spanW > 2) m.groundSpans(g, intArrayOf(x, spanW))
            x += seg
            if (i < segmentCount - 1 && x < totalWidth - 2) {
                m.bridgeGap((x - 2).coerceAtLeast(0), g, 1)
            }
        }
    }

    /** 每段末尾补基础地刺 / 机关，保证长关密度。 */
    private fun decorateSegmentHazards(
        m: PlatformerMapCanvas,
        g: Int,
        x: Int,
        w: Int,
        spec: SegmentSpec,
        sectionIndex: Int,
    ) {
        when (spec.kind) {
            SegmentKind.ENTRY,
            SegmentKind.FINALE,
            SegmentKind.SKY_ENTRY,
            SegmentKind.SKY_FINALE,
            -> Unit
            else -> {
                if (spec.hazardLevel >= 1 && sectionIndex % 2 == 0) {
                    m.spike((x + w - 4).coerceAtMost(x + w - 2), g)
                }
                if (spec.hazardLevel >= 2 && sectionIndex % 3 != 0) {
                    m.spikes(x + 6, g, 2.coerceAtMost(w - 8))
                }
                if (spec.trapLevel >= 1 && spec.kind != SegmentKind.TRAP_LANE) {
                    m.turret(x + w / 2, g - 3, facingRight = sectionIndex % 2 == 0)
                }
                if (spec.trapLevel >= 2 && spec.kind != SegmentKind.TRAP_LANE) {
                    m.laserH(x + 4, g - 4, 3.coerceAtMost(w - 8), cycle = 2.1f)
                }
                if (spec.trapLevel >= 3 && sectionIndex % 4 == 0) {
                    m.movingSpike(x + 10, g - 3, 2)
                }
            }
        }
    }
}
