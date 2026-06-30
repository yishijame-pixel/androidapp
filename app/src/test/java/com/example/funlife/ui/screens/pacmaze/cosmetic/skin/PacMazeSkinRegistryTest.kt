package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeYishiCatalog
import org.junit.Assert.assertTrue
import org.junit.Test

class PacMazeSkinRegistryTest {

    @Test
    fun yishiSkins_registeredInRemoteCatalog() {
        PacMazeYishiCatalog.skinIds.forEach { skinId ->
            assertTrue(
                "yishi skin not in remote catalog: $skinId",
                PacMazeRemoteSkinAnimCatalog.usesRemoteAnim(skinId),
            )
        }
    }

    @Test
    fun remoteSkins_includeAllYishiCharacters() {
        val remote = PacMazeRemoteSkinAnimCatalog.remoteSkinIds
        PacMazeYishiCatalog.skinIds.forEach { skinId ->
            assertTrue("remote catalog missing $skinId", skinId in remote)
        }
    }
}
