package com.example.funlife.domain.skin

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * 不上 Robolectric —— 纯数据校验，跑得快。
 */
class BuiltInSkinsTest {

    @Test
    fun `all built-in skins have unique ids`() {
        val ids = BuiltInSkins.all.map { it.id.raw }
        assertThat(ids).containsNoDuplicates()
    }

    @Test
    fun `default skin is in the list`() {
        assertThat(BuiltInSkins.all).contains(BuiltInSkins.default)
    }

    @Test
    fun `default skin is HengWu free`() {
        assertThat(BuiltInSkins.default.id.raw).isEqualTo("builtin::hengwu")
        assertThat(BuiltInSkins.default.meta.unlock).isEqualTo(Unlock.Free)
    }

    @Test
    fun `findById returns matching skin`() {
        val id = SkinId.of("builtin", "hengwu")
        assertThat(BuiltInSkins.findById(id)).isEqualTo(BuiltInSkins.HengWu)
    }

    @Test
    fun `findById returns null for unknown id`() {
        val id = SkinId.of("builtin", "ghost")
        assertThat(BuiltInSkins.findById(id)).isNull()
    }

    @Test
    fun `geometry numbers are positive`() {
        BuiltInSkins.all.forEach { skin ->
            val g = skin.geometry
            assertThat(g.pageStackCountHigh).isGreaterThan(0)
            assertThat(g.pageStackCountMid).isGreaterThan(0)
            assertThat(g.pageStackCountLow).isGreaterThan(0)
            assertThat(g.pageStackLineAlphaMin).isAtLeast(0f)
            assertThat(g.pageStackLineAlphaMax).isAtMost(1f)
            assertThat(g.pageStackLineAlphaMin).isLessThan(g.pageStackLineAlphaMax)
        }
    }

    @Test
    fun `palette colors are not transparent`() {
        BuiltInSkins.all.forEach { skin ->
            val p = skin.palette
            assertThat(p.cover.base.alpha).isGreaterThan(0f)
            assertThat(p.paper.alpha).isGreaterThan(0f)
            assertThat(p.ink.alpha).isGreaterThan(0f)
        }
    }

    @Test
    fun `SkinId requires namespace separator`() {
        runCatching { SkinId("nope") }.also {
            assertThat(it.isFailure).isTrue()
        }
        val ok = SkinId("ns::name")
        assertThat(ok.namespace).isEqualTo("ns")
        assertThat(ok.name).isEqualTo("name")
    }
}
