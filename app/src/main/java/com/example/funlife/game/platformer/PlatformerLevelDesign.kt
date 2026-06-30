package com.example.funlife.game.platformer

/**
 * 关卡几何约束（与 [PlatformerPhysics] 二段跳能力对齐）。
 * 首跳约 3.2 格，二段跳叠加后安全爬升建议 ≤2 格/步。
 *
 * 复杂关卡设计参考：
 * 1. 片段化 — [PlatformerSegmentLibrary] 每段 28 格，拼接成长关
 * 2. 双路线 — 低路（踏脚石/踏板）保底 + 高路（浮岛/宝石）奖励
 * 3. 节奏 — 教学 → 挑战 → 喘息 → 高潮（每 3~4 段一个喘息段）
 * 4. 机关抬高 — 激光/炮台放在空中走廊，不封死地面
 */
/** 平台高度层（相对地面行 g 向上抬升的格数）。 */
enum class PlatformerPlatformTier(val liftMin: Int, val liftMax: Int, val label: String) {
    LOW(0, 1, "低层"),
    MID(2, 3, "中层"),
    HIGH(4, 6, "高层"),
}

object PlatformerLevelDesign {
    /** 相邻落脚点最大垂直差（格） */
    const val MAX_STEP_UP = 2
    /** 地面最大缺口（格，需平台或二段跳兜底） */
    const val MAX_GROUND_GAP = 2
    /** 低路推荐最高高度（相对地面行） */
    const val LOW_ROUTE_MAX_LIFT = 2
    /** 高路奖励区最高高度（相对地面行） */
    const val HIGH_ROUTE_MAX_LIFT = 4

    fun tierY(groundY: Int, tier: PlatformerPlatformTier, slot: Int = 0): Int {
        val span = tier.liftMax - tier.liftMin
        val lift = tier.liftMin + (slot % (span + 1))
        return groundY - lift
    }
}
