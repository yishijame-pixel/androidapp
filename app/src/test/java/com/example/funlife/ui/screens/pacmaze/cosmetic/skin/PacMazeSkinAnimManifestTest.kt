package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PacMazeSkinAnimManifestTest {

    private val legacyJson = """
        {
          "skinId": "long_walk",
          "clips": { "walk": 10 }
        }
    """.trimIndent()

    private val v2Json = """
        {
          "schemaVersion": 2,
          "skinId": "food_chick_walker_pro_max",
          "normalized": true,
          "canvas": { "w": 480, "h": 864 },
          "anchorFrac": { "x": 0.5, "y": 0.962 },
          "clips": {
            "idle": { "count": 61, "folder": "idle", "prefix": "idle", "fps": 12 },
            "walk": { "count": 61, "folder": "walk", "prefix": "walk", "fps": 24 },
            "jump": { "count": 61, "folder": "jump", "prefix": "jump", "fps": 20 }
          },
          "render": {
            "syncWalkCycleToSprite": true,
            "sampleSize": 1
          }
        }
    """.trimIndent()

    @Test
    fun parseManifest_legacyClipCounts() {
        val manifest = PacMazeSkinAnimManifest.parseManifest(legacyJson)
        assertEquals("long_walk", manifest.skinId)
        assertEquals(1, manifest.schemaVersion)
        assertFalse(manifest.normalized)
        assertEquals(10, manifest.frameCount(PacMazeSkinAnimClip.WALK))
        assertTrue(PacMazeSkinAnimClip.WALK in manifest.clipSet())
    }

    @Test
    fun parseManifest_v2Normalized() {
        val manifest = PacMazeSkinAnimManifest.parseManifest(v2Json)
        assertEquals(2, manifest.schemaVersion)
        assertTrue(manifest.normalized)
        assertEquals(480, manifest.canvas?.w)
        assertEquals(864, manifest.canvas?.h)
        assertEquals(0.5f, manifest.anchorFrac?.x ?: 0f, 0.001f)
        assertEquals(61, manifest.frameCount(PacMazeSkinAnimClip.IDLE))
        assertEquals("idle", manifest.clipFolder(PacMazeSkinAnimClip.IDLE))
        assertEquals(true, manifest.render?.syncWalkCycleToSprite)
    }

    @Test
    fun remoteCatalog_mergesManifestRender() {
        val base = PacMazeRemoteSkinAnimCatalog.baseConfig(
            com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId.FOOD_CHICK_WALKER_PRO_MAX,
        )
        assertTrue(base!!.syncWalkCycleToSprite)
    }
}
