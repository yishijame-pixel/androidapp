package com.example.funlife.game.platformer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 不依赖 ResourceStore / Robolectric 的纯逻辑测试。 */
class PlatformerSuperTuxLevelCatalogTest {

    @Test
    fun levelIdRange_coversAllChapters() {
        assertEquals(901, PlatformerSuperTuxLengthSpec.SUPERTUX_LEVEL_START)
        assertEquals(1018, PlatformerSuperTuxLengthSpec.SUPERTUX_LEVEL_END)
        assertEquals(107, PlatformerSuperTuxLengthSpec.SUPERTUX_LEVEL_COUNT)
    }

    @Test
    fun meta_usesSupertuxThemeAndTileset() {
        val meta = PlatformerSuperTuxLevelCatalog.meta(901, "欢迎来到南极")
        assertEquals(901, meta.id)
        assertEquals("欢迎来到南极", meta.title)
        assertEquals(PlatformerTheme.PACK_SUPERTUX, meta.theme)
        assertEquals(PlatformerTilesetPack.SUPERTUX, meta.tilesetPack)
        assertEquals("supertux_antarctic", meta.seriesId)
        assertEquals(1, meta.seriesOrder)
    }

    @Test
    fun mainCampaignCount_unchangedAt74() {
        assertEquals(74, PLATFORMER_TOTAL_LEVEL_COUNT)
        assertEquals(52, PLATFORMER_CAMPAIGN_LEVEL_COUNT)
    }

    @Test
    fun isSuperTuxLevel_respectsChapterRangesAndGaps() {
        assertTrue(PlatformerSuperTuxLengthSpec.isSuperTuxLevel(901))
        assertTrue(PlatformerSuperTuxLengthSpec.isSuperTuxLevel(931))
        assertTrue(PlatformerSuperTuxLengthSpec.isSuperTuxLevel(941))
        assertTrue(PlatformerSuperTuxLengthSpec.isSuperTuxLevel(1018))
        assertFalse(PlatformerSuperTuxLengthSpec.isSuperTuxLevel(935))
        assertFalse(PlatformerSuperTuxLengthSpec.isSuperTuxLevel(74))
        assertFalse(PlatformerSuperTuxLengthSpec.isSuperTuxLevel(900))
    }

    @Test
    fun fallbackOrderedIds_has107Entries() {
        assertEquals(107, PlatformerSuperTuxLevelCatalog.orderedLevelIds.size)
    }
}
