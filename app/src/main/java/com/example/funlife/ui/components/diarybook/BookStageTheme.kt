// ═══════════════════════════════════════════════════════════════════════════
// BookStageTheme.kt — 魔法书"剧场"配色单一事实源
//
// 深邃剧场风：书本悬浮在暗色魔法剧场中，特效（火焰/雷电/星河）在深背景上
// 才能真正发光发亮。每个皮肤一套：
//   · 深色背景三段渐变（bgTop→bgMid→bgBot）
//   · 书后发光气场色 halo（贴合皮肤的暖/冷光，绝不用白色 → 杜绝白色光晕）
//   · 地面反照色 ground（火光不该投黑影）
//   · 顶部舞台聚光 spotlight
//   · 浅色发光文字 / 图标 / 统计 配色（深底必须用浅字）
//   · 主操作按钮渐变
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.diarybook

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class BookStageTheme(
    val bgTop: Color,
    val bgMid: Color,
    val bgBot: Color,
    val halo: Color,          // 书后发光气场（皮肤暖/冷光，非白）
    val haloCore: Color,      // 气场更亮的内核
    val ground: Color,        // 地面反照色
    val groundAlpha: Float,
    val spotlight: Color,     // 顶部舞台聚光
    val title: Color,         // 标题（浅色发光）
    val subtitle: Color,      // 副标题
    val icon: Color,          // 顶栏图标
    val statValue: Color,
    val statLabel: Color,
    val primaryStart: Color,  // 主按钮渐变起
    val primaryEnd: Color,    // 主按钮渐变止
    val primaryText: Color,
)

/** 皮肤 raw id → 深邃剧场配色。未知皮肤回退到 [hengWuStage]（暗夜墨金）。 */
fun bookStageThemeFor(rawId: String): BookStageTheme = when (rawId) {
    "builtin::chiyan"    -> chiYanStage
    "builtin::jiyue"     -> jiYueStage
    "builtin::qingchuan" -> qingChuanStage
    "builtin::qingluan"  -> qingLuanStage
    "builtin::xinghe"    -> xingHeStage
    else                 -> hengWuStage
}

// ── 赤焰天书 · 焚天熔炉 ──────────────────────────────────────────────────
private val chiYanStage = BookStageTheme(
    bgTop      = Color(0xFF170805),
    bgMid      = Color(0xFF2A0C07),
    bgBot      = Color(0xFF3F1206),   // 底部熔岩暖起
    halo       = Color(0xFFFF7A2A),   // 炽橙气场（替代原白色光晕）
    haloCore   = Color(0xFFFFC062),   // 内核金橙
    ground     = Color(0xFFFF4D1A),   // 火光橙红反照
    groundAlpha = 0.60f,
    spotlight  = Color(0xFFFFA64D),
    title      = Color(0xFFFFD98A),
    subtitle   = Color(0xFFE0A45A),
    icon       = Color(0xFFEBB877),
    statValue  = Color(0xFFFFE3A6),
    statLabel  = Color(0xFFC9905A),
    primaryStart = Color(0xFFFF8A2A),
    primaryEnd   = Color(0xFFC02418),
    primaryText  = Color(0xFFFFF6EA),
)

// ── 霁月长明 · 紫电夜幕 ──────────────────────────────────────────────────
private val jiYueStage = BookStageTheme(
    bgTop      = Color(0xFF0A0618),
    bgMid      = Color(0xFF130A2A),
    bgBot      = Color(0xFF1B0E38),
    halo       = Color(0xFF8E78E0),   // 银紫气场
    haloCore   = Color(0xFFC9BCFF),
    ground     = Color(0xFF7B5BFF),
    groundAlpha = 0.45f,
    spotlight  = Color(0xFFB6A8FF),
    title      = Color(0xFFEAE4FF),
    subtitle   = Color(0xFFA99DD6),
    icon       = Color(0xFFCFC8F0),
    statValue  = Color(0xFFEAE4FF),
    statLabel  = Color(0xFF9A8DCC),
    primaryStart = Color(0xFF8E6BFF),
    primaryEnd   = Color(0xFF4A2C8C),
    primaryText  = Color(0xFFF3EFFF),
)

// ── 晴川早春 · 夜樱玫瑰金（深邃剧场 + 暖樱微光）──────────────────────────
private val qingChuanStage = BookStageTheme(
    bgTop      = Color(0xFF160E14),
    bgMid      = Color(0xFF261520),
    bgBot      = Color(0xFF381E2C),
    halo       = Color(0xFFFFA888),
    haloCore   = Color(0xFFFFD4BC),
    ground     = Color(0xFFE89BAA),
    groundAlpha = 0.52f,
    spotlight  = Color(0xFFFFC4A8),
    title      = Color(0xFFFFEDE4),
    subtitle   = Color(0xFFE8B8A8),
    icon       = Color(0xFFF0C4B0),
    statValue  = Color(0xFFFFEDE4),
    statLabel  = Color(0xFFD4A090),
    primaryStart = Color(0xFFF0A088),
    primaryEnd   = Color(0xFFB85A48),
    primaryText  = Color(0xFFFFF8F4),
)

// ── 青鸾翠竹 · 翠林夜雾 ──────────────────────────────────────────────────
private val qingLuanStage = BookStageTheme(
    bgTop      = Color(0xFF07150E),
    bgMid      = Color(0xFF0D2318),
    bgBot      = Color(0xFF143322),
    halo       = Color(0xFF6FC79B),   // 翠玉气场
    haloCore   = Color(0xFFBCEBD2),
    ground     = Color(0xFF4FA77E),
    groundAlpha = 0.45f,
    spotlight  = Color(0xFF9EDDBC),
    title      = Color(0xFFE6F2EA),
    subtitle   = Color(0xFFA0C7B0),
    icon       = Color(0xFFC0DECF),
    statValue  = Color(0xFFE6F2EA),
    statLabel  = Color(0xFF8FB8A1),
    primaryStart = Color(0xFF49A87C),
    primaryEnd   = Color(0xFF1F4D3A),
    primaryText  = Color(0xFFF0F8F2),
)

// ── 星河长卷 · 深海星渊 ──────────────────────────────────────────────────
private val xingHeStage = BookStageTheme(
    bgTop      = Color(0xFF030814),
    bgMid      = Color(0xFF07122C),
    bgBot      = Color(0xFF0B1F4A),
    halo       = Color(0xFF5C8FE0),   // 星蓝气场
    haloCore   = Color(0xFFAFCDFF),
    ground     = Color(0xFF4F8FE0),
    groundAlpha = 0.42f,
    spotlight  = Color(0xFF8FBFFF),
    title      = Color(0xFFE6EEFF),
    subtitle   = Color(0xFF9DB8E0),
    icon       = Color(0xFFBDD2F0),
    statValue  = Color(0xFFE6EEFF),
    statLabel  = Color(0xFF8AA4CC),
    primaryStart = Color(0xFF3F7FD6),
    primaryEnd   = Color(0xFF132B66),
    primaryText  = Color(0xFFEFF4FF),
)

// ── 蘅芜旧卷 · 暗夜墨金（默认）──────────────────────────────────────────
private val hengWuStage = BookStageTheme(
    bgTop      = Color(0xFF0B0E16),
    bgMid      = Color(0xFF141B2A),
    bgBot      = Color(0xFF1F2A3D),
    halo       = Color(0xFFD8B25A),   // 暖金气场
    haloCore   = Color(0xFFF2D58C),
    ground     = Color(0xFFC9A24A),
    groundAlpha = 0.50f,
    spotlight  = Color(0xFFE8C676),
    title      = Color(0xFFF0DCA8),
    subtitle   = Color(0xFFBFA378),
    icon       = Color(0xFFD8C28B),
    statValue  = Color(0xFFF0DCA8),
    statLabel  = Color(0xFFAD9266),
    primaryStart = Color(0xFFD8B25A),
    primaryEnd   = Color(0xFF8A6A28),
    primaryText  = Color(0xFF2A1E08),
)
