// ═══════════════════════════════════════════════════════════════════════════
// TopDrawerPrefs.kt
// 顶部下拉抽屉用户偏好：当前选中的模式 id（多用户隔离）
// 严格遵循 DEVELOPMENT_PRINCIPLES.md：SharedPreferences 简单持久化
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.data

import android.content.Context

object TopDrawerPrefs {
    private const val PREF = "top_drawer_prefs"
    private fun keyMode(userId: Long) = "mode_user_$userId"

    /** 读取当前用户选中的 mode id；默认为 WINDOW。 */
    fun getMode(context: Context, userId: Long): String {
        if (userId <= 0L) return DEFAULT_MODE_ID
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return sp.getString(keyMode(userId), DEFAULT_MODE_ID) ?: DEFAULT_MODE_ID
    }

    fun setMode(context: Context, userId: Long, modeId: String) {
        if (userId <= 0L) return
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        sp.edit().putString(keyMode(userId), modeId).apply()
    }

    const val DEFAULT_MODE_ID = "window"
}
