package com.example.funlife.ui.screens.pacmaze

import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeCosmeticCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.cosmetic.SkinStyleFamily

/** 角色选择系列（L1 入口）。 */
enum class PacMazeSkinSeries(
    val title: String,
    val subtitle: String,
    val emoji: String,
    val families: Set<SkinStyleFamily>,
    val accentArgb: Long,
) {
    LINE_ART("线条系列", "手绘简笔 · 治愈闯关", "✏️", setOf(SkinStyleFamily.LINE_ART), 0xFFFFB74D),
    OCEAN("海底系列", "深海游侠 · 摆尾穿梭", "🌊", setOf(SkinStyleFamily.OCEAN), 0xFF29B6F6),
    INK("国风系列", "水墨庭院 · 剪纸瓷韵", "🏯", setOf(SkinStyleFamily.INK), 0xFFFFCA28),
    CYBER("赛博系列", "霓虹全息 · 数据流光", "💠", setOf(SkinStyleFamily.CYBER), 0xFF22D3EE),
    FOOD("怪趣零食", "软糯火辣 · 零食成精", "🍡", setOf(SkinStyleFamily.FOOD, SkinStyleFamily.CHIBI), 0xFFFF7043),
    IKUN("ikun类", "梗图行走 · 梗图角色", "🐔", setOf(SkinStyleFamily.IKUN), 0xFFFFD54F),
    YISHI("一十类", "品牌行走 · 品牌角色", "✨", setOf(SkinStyleFamily.YISHI), 0xFF81C784),
    COLLECT("主题典藏", "街机蒸汽 · 经典回味", "🎭", setOf(SkinStyleFamily.RETRO, SkinStyleFamily.STEAM), 0xFF9575FF),
    ;

    fun isBitmapWalkSeries(): Boolean = this == IKUN || this == YISHI

    fun skins(): List<PacMazeSkinId> = when (this) {
        LINE_ART, OCEAN, INK, CYBER, FOOD -> PacMazeSkinId.selectable.filter {
            PacMazeCosmeticCatalog.definition(it).styleFamily in families
        }
        IKUN -> PacMazeSkinId.selectable.filter {
            PacMazeCosmeticCatalog.definition(it).styleFamily == SkinStyleFamily.IKUN
        }
        YISHI -> PacMazeSkinId.selectable.filter {
            PacMazeCosmeticCatalog.definition(it).styleFamily == SkinStyleFamily.YISHI
        }
        COLLECT -> PacMazeSkinId.selectable.filter {
            val f = PacMazeCosmeticCatalog.definition(it).styleFamily
            f == SkinStyleFamily.RETRO || f == SkinStyleFamily.STEAM
        }
    }
}

object PacMazeCosmeticSeriesNav {
    fun seriesForSkin(skinId: PacMazeSkinId): PacMazeSkinSeries {
        val family = PacMazeCosmeticCatalog.definition(skinId).styleFamily
        return when (family) {
            SkinStyleFamily.LINE_ART -> PacMazeSkinSeries.LINE_ART
            SkinStyleFamily.OCEAN -> PacMazeSkinSeries.OCEAN
            SkinStyleFamily.INK -> PacMazeSkinSeries.INK
            SkinStyleFamily.CYBER -> PacMazeSkinSeries.CYBER
            SkinStyleFamily.FOOD, SkinStyleFamily.CHIBI -> PacMazeSkinSeries.FOOD
            SkinStyleFamily.IKUN -> PacMazeSkinSeries.IKUN
            SkinStyleFamily.YISHI -> PacMazeSkinSeries.YISHI
            SkinStyleFamily.RETRO, SkinStyleFamily.STEAM -> PacMazeSkinSeries.COLLECT
            else -> PacMazeSkinSeries.COLLECT
        }
    }

    fun familyFilterForSeries(series: PacMazeSkinSeries): SkinStyleFamily? =
        when (series) {
            PacMazeSkinSeries.LINE_ART -> SkinStyleFamily.LINE_ART
            PacMazeSkinSeries.OCEAN -> SkinStyleFamily.OCEAN
            PacMazeSkinSeries.INK -> SkinStyleFamily.INK
            PacMazeSkinSeries.CYBER -> SkinStyleFamily.CYBER
            PacMazeSkinSeries.FOOD -> null
            PacMazeSkinSeries.IKUN -> SkinStyleFamily.IKUN
            PacMazeSkinSeries.YISHI -> SkinStyleFamily.YISHI
            PacMazeSkinSeries.COLLECT -> null
        }
}
