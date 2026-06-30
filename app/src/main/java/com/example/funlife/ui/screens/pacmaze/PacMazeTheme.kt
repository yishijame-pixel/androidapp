package com.example.funlife.ui.screens.pacmaze

import com.example.funlife.social.game.engine.pacmaze.PacMazeLevelProgression
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object PacMazePalette {
    val bgTop = Color(0xFF0F1628)
    val bgMid = Color(0xFF1A2440)
    val bgBottom = Color(0xFF121A2E)

    val accentOrange = Color(0xFFFF7043)
    val accentGold = Color(0xFFFFCA28)
    val accentPurple = Color(0xFF9575FF)
    val accentBlue = Color(0xFF5C6BC0)
    val accentMint = Color(0xFF4ADE80)
    val accentCyan = Color(0xFF22D3EE)

    val inkPrimary = Color(0xFFFFFFFF)
    val inkSecondary = Color(0xFFE2E8F0)
    val inkMuted = Color(0xFFB8C5D9)
    val inkHint = Color(0xFF8FA3BF)

    val cardFill = Color(0xFF243047)
    val cardFillElevated = Color(0xFF2D3A52)
    val cardBorder = Color.White.copy(alpha = 0.18f)
    val cardBorderStrong = Color.White.copy(alpha = 0.28f)
    val cardGlow = Color(0xFFFF7043).copy(alpha = 0.22f)

    val locked = Color(0xFF475569)
    val starFilled = Color(0xFFFFD54F)
    val starEmpty = Color(0xFF64748B)

    val difficultyEasy = Color(0xFF4ADE80)
    val difficultyNormal = Color(0xFF38BDF8)
    val difficultyHard = Color(0xFFFB923C)
    val difficultyExtreme = Color(0xFFF87171)

    val hubGradient = Brush.verticalGradient(listOf(bgTop, bgMid, bgBottom))
    val heroGlow = Brush.radialGradient(
        colors = listOf(accentOrange.copy(alpha = 0.42f), Color.Transparent),
    )
    val ctaGradient = Brush.horizontalGradient(listOf(Color(0xFFFF5722), Color(0xFFFFCA28)))
    val heroPanelGradient = Brush.verticalGradient(
        listOf(Color(0xFF2A3550), Color(0xFF1E2838)),
    )
    val contentPanelGradient = Brush.verticalGradient(
        listOf(Color(0xFF2F3B55), Color(0xFF243047)),
    )
    val overlayCardGradient = Brush.verticalGradient(
        listOf(Color(0xFF2E3A54), Color(0xFF1F283C)),
    )
    val hudGradient = Brush.horizontalGradient(
        listOf(Color(0xE6121828), Color(0xCC121828)),
    )
    val hudScrim = Brush.verticalGradient(
        listOf(Color(0xCC060A14), Color(0x00060A14)),
    )

    val modeEndless = Color(0xFFB388FF)
    val modeMaze = Color(0xFFFFB74D)
    val modePractice = Color(0xFF4ADE80)
}

/** 横屏对局安全区：避开系统栏与左右触控区。 */
object PacMazeLandscapeInsets {
    val hudTop = 6.dp
    val hudHorizontal = 10.dp
    val joystickStart = 14.dp
    val joystickBottom = 10.dp
    val actionEnd = 14.dp
    val actionBottom = 10.dp
    val joystickSize = 136.dp
    val joystickZoneWidth = 168.dp
    val joystickZoneHeight = 168.dp
}

enum class PacMazePlayMode(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val playable: Boolean = false,
) {
    SOLO("solo", "单人闯关", "23关 · 14主题递进", "🏃", playable = true),
    ENDLESS("endless", "无尽模式", "波次递进 · 冲高分", "♾️", playable = true),
    MAZE("maze", "迷雾迷宫", "战争迷雾 · 钥印出口 · 每日挑战", "🌫️", playable = true),
    ONLINE_VERSUS("online_versus", "豆人对决", "邀请好友实时竞技", "⚔️", playable = true),
    ONLINE_COOP("online_coop", "并肩闯关", "在线合作 L1–L8", "👥", playable = true),
    ONLINE("online", "在线对战", "大厅内联机开房", "🌐", playable = true),
}

/** @deprecated 使用 [PacMazeMenuRoute] 栈式导航 */
@Deprecated("Replaced by PacMazeMenuRoute")
enum class PacMazeMenuStep {
    MODE_SELECT,
    CHARACTER_SELECT,
    LEVEL_SELECT,
}

data class PacMazeLevelMeta(
    val id: Int,
    val name: String,
    val subtitle: String,
    val difficulty: String,
    val mechanisms: List<PacMazeMechanismKind> = PacMazeLevelMechanisms.forLevel(id),
    val tutorialHint: String = "",
)

object PacMazeLevelCatalog {
    const val TOTAL_LEVELS = 23

    val levels: List<PacMazeLevelMeta> = (1..TOTAL_LEVELS).map { id ->
        val meta = when (id) {
            1 -> "赛博-1" to "数据管道 · 道具入门"
            2 -> "赛博-2" to "内核环路 · 双工厂"
            3 -> "花园-1" to "树篱迷宫 · 冰霜解锁"
            4 -> "糖果-1" to "方糖群岛 · 三厂补给"
            5 -> "古风-1" to "四合院 · 激光庭院"
            6 -> "赛博-3" to "分叉数据港 · 传送门"
            7 -> "花园-2" to "四瓣花坛 · 十字花圃"
            8 -> "糖果-2" to "糖浆螺旋 · 充能登场"
            9 -> "古风-2" to "九曲回廊 · 蛇形廊道"
            10 -> "古风-3" to "龙门关 · 五厂终局"
            11 -> "古风-4" to "四合院落 · 四面厢房"
            12 -> "花园-3" to "江南园林 · 曲径水池"
            13 -> "古风-5" to "回字院落 · 三重围合"
            14 -> "蒸汽-1" to "活塞矩阵 · 移动墙入门"
            15 -> "故障-1" to "RGB 错位 · 十字激光"
            16 -> "星港-1" to "气闸矩阵 · 能量门阵"
            17 -> "熔岩-1" to "地核脉冲 · 炮塔走廊"
            18 -> "深潜-1" to "声呐迷宫 · 五重围合"
            19 -> "冰库-1" to "霜花机关 · 动态条纹"
            20 -> "古籍-1" to "符卷索引 · 全幽灵池"
            21 -> "地铁-1" to "区间闸机 · 八卦要道"
            22 -> "戏台-1" to "幕布包抄 · 六厂熔炉"
            else -> "温室-1" to "穹顶终局 · 地狱熔炉"
        }
        PacMazeLevelMeta(
            id = id,
            name = meta.first,
            subtitle = meta.second,
            difficulty = PacMazeLevelProgression.difficultyLabel(id),
            tutorialHint = tutorialHintFor(id),
        )
    }

    fun find(levelId: Int): PacMazeLevelMeta? = levels.firstOrNull { it.id == levelId }

    fun difficultyColor(label: String) = when (label) {
        "简单" -> PacMazePalette.difficultyEasy
        "普通" -> PacMazePalette.difficultyNormal
        "困难" -> PacMazePalette.difficultyHard
        "挑战" -> PacMazePalette.difficultyExtreme
        "极限" -> PacMazePalette.difficultyExtreme
        else -> PacMazePalette.difficultyExtreme
    }
}

