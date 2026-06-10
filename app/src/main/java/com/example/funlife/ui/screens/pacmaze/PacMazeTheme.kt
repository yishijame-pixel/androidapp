package com.example.funlife.ui.screens.pacmaze

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

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
}

enum class PacMazePlayMode(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val playable: Boolean = false,
) {
    SOLO("solo", "单人闯关", "四主题 13 关递进", "🏃", playable = true),
    ENDLESS("endless", "无尽模式", "波次递进 · 冲高分", "♾️", playable = true),
    MAZE("maze", "迷宫模式", "找到出口 · 计时赛", "🧭", playable = true),
    LOCAL_COOP("local_coop", "本地双人", "同屏协作闯关", "👥"),
    LOCAL_VERSUS("local_versus", "本地 1v1", "豆人 VS 幽灵", "⚔️"),
    ONLINE("online", "在线对战", "好友联机（开发中）", "🌐"),
}

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
)

object PacMazeLevelCatalog {
    const val TOTAL_LEVELS = 13

    val levels: List<PacMazeLevelMeta> = listOf(
        PacMazeLevelMeta(1, "赛博-1", "数据管道 · 交错竖廊", "简单"),
        PacMazeLevelMeta(2, "赛博-2", "内核环路 · 中空堡垒", "简单"),
        PacMazeLevelMeta(3, "花园-1", "树篱迷宫 · 多死路", "普通"),
        PacMazeLevelMeta(4, "糖果-1", "方糖群岛 · 散点岛", "困难"),
        PacMazeLevelMeta(5, "古风-1", "四合院 · 天井中空", "挑战"),
        PacMazeLevelMeta(6, "赛博-3", "分叉数据港 · 传送门", "普通"),
        PacMazeLevelMeta(7, "花园-2", "四瓣花坛 · 十字花圃", "普通"),
        PacMazeLevelMeta(8, "糖果-2", "糖浆螺旋 · 回字形", "困难"),
        PacMazeLevelMeta(9, "古风-2", "九曲回廊 · 蛇形廊道", "挑战"),
        PacMazeLevelMeta(10, "古风-3", "龙门关 · 终局双激光", "挑战"),
        PacMazeLevelMeta(11, "古风-4", "四合院落 · 四面厢房", "挑战"),
        PacMazeLevelMeta(12, "花园-3", "江南园林 · 曲径水池", "普通"),
        PacMazeLevelMeta(13, "古风-5", "回字院落 · 三重围合", "挑战"),
    )

    fun find(levelId: Int): PacMazeLevelMeta? = levels.firstOrNull { it.id == levelId }

    fun difficultyColor(label: String) = when (label) {
        "简单" -> PacMazePalette.difficultyEasy
        "普通" -> PacMazePalette.difficultyNormal
        "困难" -> PacMazePalette.difficultyHard
        else -> PacMazePalette.difficultyExtreme
    }
}

