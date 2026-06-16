package com.example.funlife.ui.screens.pacmaze

import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeContract
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeDifficulty
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeMutator
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeVariant

data class PacMazeMazeCodexEntry(
    val id: String,
    val pillar: String,
    val title: String,
    val emoji: String,
    val summary: String,
    val detail: String,
)

object PacMazeMazeCatalog {

    val codexEntries: List<PacMazeMazeCodexEntry> = listOf(
        PacMazeMazeCodexEntry("fog", "探索", "战争迷雾", "🌫", "仅照亮周围格子", "移动时揭开相邻区域；浓雾契约会缩小视野半径。"),
        PacMazeMazeCodexEntry("radar", "探索", "回声雷达", "📡", "主动脉冲探路", "右下角按钮释放全向探测，有冷却时间；猎手契约可减半 CD。"),
        PacMazeMazeCodexEntry("echo", "探索", "回声豆", "💡", "线索豆指路", "死胡同与岔路的蓝色豆被吃掉后，短暂指向最近未收集钥印。"),
        PacMazeMazeCodexEntry("keys", "探索", "钥印门", "🗝", "集齐后开出口", "自由模式任意顺序；封印模式必须按 1→2→3 收集。"),
        PacMazeMazeCodexEntry("intel", "探索", "情报拍卖", "🎯", "3 点情报换信息", "可揭开象限或显示下一钥印方向；不花费有利于三星用时。"),
        PacMazeMazeCodexEntry("walls", "逃生", "动态墙", "🧱", "条纹墙定时开合", "标准/深渊赛道生效；镜像契约反转开合相位。"),
        PacMazeMazeCodexEntry("ghosts", "逃生", "幽灵压迫", "👻", "逼近红晕 + 签名", "幽灵靠近时屏幕边缘泛红；每日挑战有固定 AI 签名。"),
        PacMazeMazeCodexEntry("items", "逃生", "道具房", "🎁", "单次拾取增益", "冰霜定鬼、迅捷加速、护盾抵挡，随机出现在死胡同。"),
        PacMazeMazeCodexEntry("hunt", "逃生", "追猎倒计时", "⏳", "鬼随时间进化", "出口区域先亮；每隔 30 秒幽灵加速，抢在时间耗尽前逃出。"),
        PacMazeMazeCodexEntry("daily", "竞技", "每日挑战", "📅", "全天同 seed", "所有人同一张图，可对比用时与 Ghost 轨迹。"),
        PacMazeMazeCodexEntry("random", "竞技", "自由随机", "🎲", "每局新迷宫", "开局生成新 seed，可无限重开刷图。"),
        PacMazeMazeCodexEntry("mutator", "竞技", "深渊周赛", "🏆", "每周全局规则", "每日挑战附带本周 Mutator：墙速/多鬼/时限等。"),
        PacMazeMazeCodexEntry("ghost_replay", "竞技", "异步 Ghost", "👻", "挑战最快残影", "记录今日最佳路线，分段对比超越。"),
        PacMazeMazeCodexEntry("dual", "探索", "双迷宫", "🌀", "传送门切换", "双生契约或双迷宫变体生成传送门对，连接远距区域。"),
        PacMazeMazeCodexEntry("contracts", "竞技", "开局契约", "📜", "修饰规则", "9 种契约可叠加赛道，影响视野、时限、地图尺寸等。"),
    )

    fun contractDetail(contract: PacMazeMazeContract): String = when (contract) {
        PacMazeMazeContract.NONE -> "不附加额外规则，适合熟悉地图与基础节奏。"
        PacMazeMazeContract.DEEP_FOG -> "视野半径 -1，三星时限额外 +15 秒。适合喜欢纯探索的玩家。"
        PacMazeMazeContract.SILENT -> "移除全部幽灵，必须集齐钥印才能通关。解谜向。"
        PacMazeMazeContract.RUSH -> "时限缩短至 65%，得分 ×1.5。速通向。"
        PacMazeMazeContract.LABYRINTH -> "地图 +4 格，多 1 把钥印。大型迷宫体验。"
        PacMazeMazeContract.MIRROR -> "动态墙开合相位反转，需要反向记节奏。"
        PacMazeMazeContract.HUNTER -> "额外 1 只幽灵，雷达冷却减半。高风险高信息。"
        PacMazeMazeContract.BLIND_PATH -> "无线索豆；收集最后一把钥印时短暂照亮出口。"
        PacMazeMazeContract.TWIN -> "生成一对传送门，踩双门后可远距跃迁。"
    }

    fun variantDetail(variant: PacMazeMazeVariant): String = when (variant) {
        PacMazeMazeVariant.STANDARD -> "经典迷雾迷宫：探索、钥印、出口、可选幽灵与动态墙。"
        PacMazeMazeVariant.HUNT -> "鬼会随时间变强；在深渊化之前找到出口。"
        PacMazeMazeVariant.DUAL -> "地图略大并含传送门对，适合绕路解谜。"
        PacMazeMazeVariant.INTEL -> "开局 3 情报点，局内购买地图信息。"
        PacMazeMazeVariant.GHOST_REPLAY -> "每日专属：与本日最快 Ghost 轨迹分段对比。"
    }

    fun trackDetail(diff: PacMazeMazeDifficulty): String = when (diff) {
        PacMazeMazeDifficulty.SCOUT -> "11×11 · 无幽灵 · 宽视野 · 适合入门与解谜。"
        PacMazeMazeDifficulty.STANDARD -> "15×15 · 1 幽灵 · 动态墙 · 平衡体验。"
        PacMazeMazeDifficulty.ABYSS -> "19×19 · 2 幽灵 · 窄视野 · 周赛主赛道。"
    }

    fun mutatorDetail(mutator: PacMazeMazeMutator): String = when (mutator) {
        PacMazeMazeMutator.NONE -> "本周无额外修饰。"
        PacMazeMazeMutator.FAST_WALLS -> "动态墙切换速度 ×1.5，节奏更紧张。"
        PacMazeMazeMutator.EXTRA_GHOST -> "额外 1 只幽灵，压迫感上升。"
        PacMazeMazeMutator.TIME_CRUNCH -> "时限 -20%，得分 ×1.5 补偿。"
    }

    fun recommendedContracts(diff: PacMazeMazeDifficulty): List<PacMazeMazeContract> = when (diff) {
        PacMazeMazeDifficulty.SCOUT -> listOf(PacMazeMazeContract.NONE, PacMazeMazeContract.SILENT, PacMazeMazeContract.DEEP_FOG)
        PacMazeMazeDifficulty.STANDARD -> listOf(PacMazeMazeContract.NONE, PacMazeMazeContract.RUSH, PacMazeMazeContract.TWIN)
        PacMazeMazeDifficulty.ABYSS -> listOf(PacMazeMazeContract.HUNTER, PacMazeMazeContract.MIRROR, PacMazeMazeContract.RUSH)
    }
}

fun formatMazeSeedCode(seed: Long): String = "#${seed.toString().takeLast(8)}"
