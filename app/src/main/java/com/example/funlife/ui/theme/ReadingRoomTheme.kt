// ReadingRoomTheme.kt — v53 阅光书房 · 晨光胶囊视觉系统
//
// 设计语言：
//   "晨光胶囊" = 晨曦的米白 + 雾蓝 + 暖橙
//   - 主背景：从顶部象牙白 → 雾蓝 → 暖橙的纵向渐变
//   - 卡片：奶白纸纹，软阴影，圆角 24+
//   - 强调：暖橙 → 玫瑰金的水平渐变
//   - 文字：墨蓝主色 + 暖灰副色
//
// 这套配色与现有 BookshelfScreen 的"墨色书页"主题不同——是更"治愈系"，
// 因为 v53 是情感容器（胶囊/星河/DNA），需要更柔和的氛围。
package com.example.funlife.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object ReadingRoomTheme {
    /* ── 背景 ── */
    val BgIvory       = Color(0xFFFFF9F2)   // 顶部象牙
    val BgMist        = Color(0xFFE9EEF5)   // 中部雾蓝
    val BgPeach       = Color(0xFFFCE0CC)   // 底部暖橙

    /* ── 主色 ── */
    val PrimaryInk    = Color(0xFF2C3E50)   // 墨蓝（主文字）
    val SecondaryInk  = Color(0xFF607185)   // 副文字
    val MutedInk      = Color(0xFFA9B5C4)   // 灰提示

    /* ── 强调 ── */
    val AccentRose    = Color(0xFFE57373)   // 玫瑰
    val AccentOrange  = Color(0xFFFFAB66)   // 暖橙
    val AccentGold    = Color(0xFFF6C97A)   // 金黄
    val AccentSky     = Color(0xFF7FB7E0)   // 浅天蓝
    val AccentLeaf    = Color(0xFF9FC089)   // 叶绿（用于打卡完成态）

    /* ── 卡片 ── */
    val CardCream     = Color(0xFFFFFCF7)   // 奶白
    val CardSky       = Color(0xFFF0F6FC)   // 雾蓝卡
    val CardPeach     = Color(0xFFFFF1E5)   // 桃色卡
    val CardSoft      = Color(0xFFFAF7F2)

    /* ── 星河 ── */
    val GalaxyBgTop    = Color(0xFF0F1830)   // 深蓝夜空
    val GalaxyBgBottom = Color(0xFF1B2545)
    val GalaxyStar     = Color(0xFFFFE9B0)
    val GalaxyAccent   = Color(0xFFFFD27A)

    /* ── 渐变 ── */
    fun pageBackground(): Brush = Brush.verticalGradient(
        listOf(BgIvory, BgMist, BgPeach)
    )

    fun heroGradient(): Brush = Brush.horizontalGradient(
        listOf(AccentOrange, AccentRose)
    )

    fun snapshotGradient(): Brush = Brush.verticalGradient(
        listOf(BgIvory, BgPeach, AccentOrange.copy(alpha = 0.4f))
    )

    fun galaxyBackground(): Brush = Brush.verticalGradient(
        listOf(GalaxyBgTop, GalaxyBgBottom)
    )
}
