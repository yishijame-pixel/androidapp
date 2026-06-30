package com.example.funlife.game.platformer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.funlife.resource.ResourceStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PlatformerSuperTuxClassicPathsTest {

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ResourceStore.init(context)
    }

    @Test
    fun level901_mapsToWelcomeAntarcticaStl() {
        val path = PlatformerSuperTuxClassicPaths.levelStlPath(901)
        assertEquals("levels/world1/welcome_antarctica.stl", path)
    }

    @Test
    fun nonSuperTuxLevel_returnsNull() {
        assertTrue(PlatformerSuperTuxClassicPaths.levelStlPath(1) == null)
    }
}
