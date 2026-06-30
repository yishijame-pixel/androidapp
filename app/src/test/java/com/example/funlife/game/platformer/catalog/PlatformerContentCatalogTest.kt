import android.content.Context
import com.example.funlife.game.platformer.catalog.PlatformerContentCatalog
import com.example.funlife.resource.ResourceStore
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlatformerContentCatalogTest {

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ResourceStore.init(context)
        PlatformerContentCatalog.invalidateCache()
    }

    @Test
    fun catalog_loads_characters_and_hero_levels() {
        val catalog = PlatformerContentCatalog.load(force = true)
        assertTrue(catalog != null)
        assertTrue(catalog!!.characters.isNotEmpty())
        assertTrue(PlatformerContentCatalog.heroLevels().size >= 12)
        assertTrue(catalog.characters.any { it.id == "ninja_girl" })
    }
}
