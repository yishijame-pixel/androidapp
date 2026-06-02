// ═══════════════════════════════════════════════════════════════════════════
// SkinRepository — 日记本皮肤状态层
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.data.skin

import com.example.funlife.domain.skin.BookSkin
import com.example.funlife.domain.skin.SkinId
import kotlinx.coroutines.flow.StateFlow

/** 业务侧使用的接口，只暴露 StateFlow + 切换/解锁方法。 */
interface SkinRepository {
    val currentSkin: StateFlow<BookSkin>
    val availableSkins: StateFlow<List<BookSkin>>

    /** 切换皮肤；未解锁返回 [Result.failure] 并带 [SkinException.Locked]。 */
    suspend fun select(id: SkinId): Result<Unit>

    /** 查询某皮肤是否已解锁（解锁逻辑由 [SkinUnlockGate] 决定）。 */
    suspend fun isUnlocked(id: SkinId): Boolean
}

sealed class SkinException(message: String) : RuntimeException(message) {
    class NotFound(val id: SkinId) : SkinException("Skin not found: $id")
    class Locked(val id: SkinId)   : SkinException("Skin locked: $id")
}
