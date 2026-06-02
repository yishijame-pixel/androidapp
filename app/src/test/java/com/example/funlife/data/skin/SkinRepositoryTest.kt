package com.example.funlife.data.skin

import androidx.test.core.app.ApplicationProvider
import com.example.funlife.domain.skin.BookSkin
import com.example.funlife.domain.skin.BuiltInSkins
import com.example.funlife.domain.skin.SkinId
import com.example.funlife.domain.skin.SkinMeta
import com.example.funlife.domain.skin.Unlock
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SkinRepositoryTest {

    private fun newPrefs(name: String = "skin_test") =
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .getSharedPreferences(name, android.content.Context.MODE_PRIVATE)
            .also { it.edit().clear().apply() }

    /** 临时 VIP 皮肤：赋予 BuiltInSkins.HengWu 不同的颜色，避免与内置实例衰送。 */
    private val vipSkin: BookSkin = BuiltInSkins.HengWu.copy(
        id = SkinId.of("test", "vip-only"),
        meta = BuiltInSkins.HengWu.meta.copy(unlock = Unlock.Vip(level = 1))
    )

    private fun newRepo(
        gate: SkinUnlockGate = FreeOnlyUnlockGate(),
        prefs: android.content.SharedPreferences = newPrefs(),
        builtIn: List<BookSkin> = BuiltInSkins.all + vipSkin,
        scope: TestScope
    ) = DefaultSkinRepository(
        prefs = prefs,
        unlockGate = gate,
        builtIn = builtIn,
        ioDispatcher = UnconfinedTestDispatcher(scope.testScheduler),
        initScope = scope
    )

    @Test
    fun `default skin emitted on first launch`() = runTest {
        val repo = newRepo(scope = this)
        assertThat(repo.currentSkin.value).isEqualTo(BuiltInSkins.default)
    }

    @Test
    fun `select free skin succeeds and persists`() = runTest {
        val prefs = newPrefs()
        val repo = newRepo(prefs = prefs, scope = this)

        val r = repo.select(BuiltInSkins.HengWu.id)
        assertThat(r.isSuccess).isTrue()
        assertThat(repo.currentSkin.value).isEqualTo(BuiltInSkins.HengWu)
        assertThat(prefs.getString(DefaultSkinRepository.KEY_SELECTED, null))
            .isEqualTo(BuiltInSkins.HengWu.id.raw)
    }

    @Test
    fun `select vip skin is rejected by FreeOnlyUnlockGate`() = runTest {
        val repo = newRepo(scope = this)
        val r = repo.select(vipSkin.id)
        assertThat(r.isFailure).isTrue()
        assertThat(r.exceptionOrNull()).isInstanceOf(SkinException.Locked::class.java)
        // current 没变
        assertThat(repo.currentSkin.value).isEqualTo(BuiltInSkins.default)
    }

    @Test
    fun `select unknown skin returns NotFound`() = runTest {
        val repo = newRepo(scope = this)
        val r = repo.select(SkinId.of("builtin", "ghost"))
        assertThat(r.isFailure).isTrue()
        assertThat(r.exceptionOrNull()).isInstanceOf(SkinException.NotFound::class.java)
    }

    @Test
    fun `repo restores last selected skin from prefs`() = runTest {
        val prefs = newPrefs()
        // 模拟上次选了一个 free 皮（HengWu）
        prefs.edit().putString(
            DefaultSkinRepository.KEY_SELECTED,
            BuiltInSkins.HengWu.id.raw
        ).apply()

        val repo = newRepo(prefs = prefs, scope = this)
        // 因 init 走 testScheduler，需要让协程跑完
        testScheduler.advanceUntilIdle()
        assertThat(repo.currentSkin.value).isEqualTo(BuiltInSkins.HengWu)
    }

    @Test
    fun `repo ignores corrupted skin id in prefs`() = runTest {
        val prefs = newPrefs()
        prefs.edit().putString(DefaultSkinRepository.KEY_SELECTED, "corrupted-no-namespace").apply()
        val repo = newRepo(prefs = prefs, scope = this)
        testScheduler.advanceUntilIdle()
        // 回退默认
        assertThat(repo.currentSkin.value).isEqualTo(BuiltInSkins.default)
    }

    @Test
    fun `repo ignores locked skin in prefs and falls back to default`() = runTest {
        val prefs = newPrefs()
        prefs.edit().putString(
            DefaultSkinRepository.KEY_SELECTED,
            vipSkin.id.raw   // VIP，被 FreeOnlyUnlockGate 拒绝
        ).apply()
        val repo = newRepo(prefs = prefs, scope = this)
        testScheduler.advanceUntilIdle()
        assertThat(repo.currentSkin.value).isEqualTo(BuiltInSkins.default)
    }

    @Test
    fun `isUnlocked reports gate state`() = runTest {
        val repo = newRepo(scope = this)
        assertThat(repo.isUnlocked(BuiltInSkins.HengWu.id)).isTrue()
        assertThat(repo.isUnlocked(vipSkin.id)).isFalse()
    }

    @Test
    fun `custom unlock gate enables vip skin`() = runTest {
        val gate = object : SkinUnlockGate {
            override suspend fun isUnlocked(skin: BookSkin): Boolean =
                skin.meta.unlock is Unlock.Free || skin.meta.unlock is Unlock.Vip
        }
        val repo = newRepo(gate = gate, scope = this)
        val r = repo.select(vipSkin.id)
        assertThat(r.isSuccess).isTrue()
        assertThat(repo.currentSkin.value).isEqualTo(vipSkin)
    }
}
