// UserSessionManager.kt - 用户会话管理器
package com.example.funlife.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.funlife.data.model.UserSession

class UserSessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "user_session",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_AVATAR = "avatar"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
    
    fun saveSession(session: UserSession) {
        prefs.edit().apply {
            putLong(KEY_USER_ID, session.userId)
            putString(KEY_USERNAME, session.username)
            putString(KEY_NICKNAME, session.nickname)
            putString(KEY_AVATAR, session.avatar)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }
    
    fun getSession(): UserSession? {
        if (!isLoggedIn()) return null
        
        return UserSession(
            userId = prefs.getLong(KEY_USER_ID, -1),
            username = prefs.getString(KEY_USERNAME, "") ?: "",
            nickname = prefs.getString(KEY_NICKNAME, "") ?: "",
            avatar = prefs.getString(KEY_AVATAR, "") ?: ""
        )
    }
    
    fun getCurrentUserId(): Long {
        return prefs.getLong(KEY_USER_ID, -1)
    }
    
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && 
               prefs.getLong(KEY_USER_ID, -1) != -1L
    }
    
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
