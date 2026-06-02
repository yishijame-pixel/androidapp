// ═════════════════════════════════════════════════════════════════════════
// DeepLinkBus.kt
// 通知点击 → MainActivity 接收 intent → 投递到 NavHost 的桥梁。
// 进程级单例 Channel，UI 用 collectAsState 消费即可，消费后即清空。
// ═════════════════════════════════════════════════════════════════════════
package com.example.funlife.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object DeepLinkBus {
    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending

    fun publish(route: String?) {
        if (route.isNullOrBlank()) return
        _pending.value = route
    }

    fun consume() { _pending.value = null }
}
