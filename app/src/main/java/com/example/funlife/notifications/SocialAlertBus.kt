package com.example.funlife.notifications

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 好友申请等社交事件的 App 内 heads-up 横幅（前台时保证可见，不依赖 OEM 系统横幅）。 */
data class SocialHeadsUpAlert(
    val id: String,
    val title: String,
    val body: String,
    val deepLinkRoute: String? = "friends",
    val timestamp: Long = System.currentTimeMillis(),
)

object SocialAlertBus {

    private val _alert = MutableStateFlow<SocialHeadsUpAlert?>(null)
    val alert: StateFlow<SocialHeadsUpAlert?> = _alert.asStateFlow()

    @Volatile
    var isAppForeground: Boolean = false
        private set

    fun installProcessObserver() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                isAppForeground = true
            }

            override fun onStop(owner: LifecycleOwner) {
                isAppForeground = false
            }
        })
    }

    fun publish(alert: SocialHeadsUpAlert) {
        _alert.value = alert
    }

    fun dismiss() {
        _alert.value = null
    }
}
