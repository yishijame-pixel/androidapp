// ═══════════════════════════════════════════════════════════════════════════
// DefaultSkinRepository — SkinRepository 的本地实现
//
// 存储：SharedPreferences（与项目其他偏好一致，不引入 DataStore）
// 启动行为：从 prefs 读 lastSelected → 校验仍存在且已解锁 → emit；否则回退默认
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.data.skin

import android.content.Context
import android.content.SharedPreferences
import com.example.funlife.domain.skin.BookSkin
import com.example.funlife.domain.skin.BuiltInSkins
import com.example.funlife.domain.skin.SkinId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DefaultSkinRepository(
    private val prefs: SharedPreferences,
    private val unlockGate: SkinUnlockGate,
    builtIn: List<BookSkin> = BuiltInSkins.all,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    /** 仅测试可注入；生产代码用默认 SupervisorJob scope。 */
    initScope: CoroutineScope? = null
) : SkinRepository {

    private val _current = MutableStateFlow(BuiltInSkins.default)
    override val currentSkin: StateFlow<BookSkin> = _current.asStateFlow()

    private val _available = MutableStateFlow(builtIn)
    override val availableSkins: StateFlow<List<BookSkin>> = _available.asStateFlow()

    private val scope = initScope ?: CoroutineScope(SupervisorJob() + ioDispatcher)

    init {
        scope.launch { restoreSelection() }
    }

    private suspend fun restoreSelection() {
        val raw = withContext(ioDispatcher) { prefs.getString(KEY_SELECTED, null) } ?: return
        val candidate = runCatching { SkinId(raw) }.getOrNull() ?: return
        val skin = _available.value.firstOrNull { it.id == candidate } ?: return
        if (!unlockGate.isUnlocked(skin)) return
        _current.value = skin
    }

    override suspend fun select(id: SkinId): Result<Unit> = withContext(ioDispatcher) {
        val skin = _available.value.firstOrNull { it.id == id }
            ?: return@withContext Result.failure(SkinException.NotFound(id))
        if (!unlockGate.isUnlocked(skin)) {
            return@withContext Result.failure(SkinException.Locked(id))
        }
        prefs.edit().putString(KEY_SELECTED, id.raw).apply()
        _current.value = skin
        Result.success(Unit)
    }

    override suspend fun isUnlocked(id: SkinId): Boolean {
        val skin = _available.value.firstOrNull { it.id == id } ?: return false
        return unlockGate.isUnlocked(skin)
    }

    companion object {
        const val PREFS_NAME = "diary_book_skin"
        const val KEY_SELECTED = "selected_skin_id"

        /** 便捷工厂：在 Application 里调用一次即可。 */
        fun create(context: Context, unlockGate: SkinUnlockGate = FreeOnlyUnlockGate()): DefaultSkinRepository {
            val prefs = context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return DefaultSkinRepository(prefs, unlockGate)
        }
    }
}
