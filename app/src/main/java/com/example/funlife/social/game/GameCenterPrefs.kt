package com.example.funlife.social.game

import android.content.Context

/**
 * 趣玩中心本地偏好，所有 key 按 [userId] 隔离（DEVELOPMENT_PRINCIPLES §1.5）。
 */
class GameCenterPrefs(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isTutorialSeen(userId: Long): Boolean =
        prefs.getBoolean(tutorialKey(userId), false)

    fun setTutorialSeen(userId: Long) {
        prefs.edit().putBoolean(tutorialKey(userId), true).apply()
    }

    fun recentGameIds(userId: Long): List<String> {
        val raw = prefs.getString(recentKey(userId), null) ?: return emptyList()
        return raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun touchRecentGame(userId: Long, gameId: String) {
        val merged = (listOf(gameId) + recentGameIds(userId).filter { it != gameId }).take(MAX_RECENT)
        prefs.edit().putString(recentKey(userId), merged.joinToString(",")).apply()
    }

    private fun tutorialKey(userId: Long) = "u${userId}_game_center_tutorial_seen"
    private fun recentKey(userId: Long) = "u${userId}_recent_game_ids"

    companion object {
        private const val PREFS_NAME = "social_game_center"
        private const val MAX_RECENT = 6
    }
}
