// ═══════════════════════════════════════════════════════════════════════════
// SkinUnlockGate — 解锁逻辑抽象
//
// P0：默认实现只放行 Unlock.Free，VIP/Purchase/Event/Achievement 一律拒绝。
// P2：业务方实现 DefaultSkinUnlockGate，对接 VIPManager / 内购 / 成就系统。
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.data.skin

import com.example.funlife.domain.skin.BookSkin
import com.example.funlife.domain.skin.Unlock

interface SkinUnlockGate {
    suspend fun isUnlocked(skin: BookSkin): Boolean
}

/** P0 实现：仅 Free 可用。 */
class FreeOnlyUnlockGate : SkinUnlockGate {
    override suspend fun isUnlocked(skin: BookSkin): Boolean =
        skin.meta.unlock is Unlock.Free
}
