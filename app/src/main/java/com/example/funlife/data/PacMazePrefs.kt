package com.example.funlife.data

import android.content.Context
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterId

/**
 * 豆人迷宫本地偏好（按 userId 隔离）。
 */
class PacMazePrefs(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun selectedCharacterId(userId: Long): PacMazeCharacterId {
        val raw = prefs.getString(characterKey(userId), null) ?: return PacMazeCharacterId.CLASSIC_PAC
        return PacMazeCharacterId.fromStorage(raw)
    }

    fun setSelectedCharacterId(userId: Long, characterId: PacMazeCharacterId) {
        prefs.edit().putString(characterKey(userId), characterId.storageKey).apply()
    }

    fun playerDrawScale(userId: Long): Float =
        prefs.getFloat(scaleKey(userId), 1f).coerceIn(0.5f, 1.5f)

    fun setPlayerDrawScale(userId: Long, scale: Float) {
        prefs.edit()
            .putFloat(scaleKey(userId), scale.coerceIn(0.5f, 1.5f))
            .apply()
    }

    private fun characterKey(userId: Long) = "u${userId}_selected_character"

    private fun scaleKey(userId: Long) = "u${userId}_player_draw_scale"

    companion object {
        private const val PREFS_NAME = "pac_maze_prefs"
    }
}
