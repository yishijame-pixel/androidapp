// UserSessionManager.kt - 用户会话管理器
package com.example.funlife.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.funlife.data.model.UserSession

class UserSessionManager(context: Context) {
    
    // 🔥 修复：使用加密的 SharedPreferences
    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        EncryptedSharedPreferences.create(
            context,
            "user_session_encrypted",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // 降级到普通 SharedPreferences（用于兼容性）
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    }
    
    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_AVATAR = "avatar"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_LOGIN_TIME = "login_time"
        private const val KEY_EXPIRY_TIME = "expiry_time"
        
        // 🔥 新增：会话有效期（7天）
        private const val SESSION_VALIDITY_DAYS = 7L
        private const val SESSION_VALIDITY_MILLIS = SESSION_VALIDITY_DAYS * 24 * 60 * 60 * 1000
    }
    
    fun saveSession(session: UserSession) {
        val currentTime = System.currentTimeMillis()
        val expiryTime = currentTime + SESSION_VALIDITY_MILLIS
        
        prefs.edit().apply {
            putLong(KEY_USER_ID, session.userId)
            putString(KEY_USERNAME, session.username)
            putString(KEY_NICKNAME, session.nickname)
            putString(KEY_AVATAR, session.avatar)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_LOGIN_TIME, currentTime)
            putLong(KEY_EXPIRY_TIME, expiryTime)
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
    
    // 🔥 修复：检查会话是否过期
    fun isLoggedIn(): Boolean {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val userId = prefs.getLong(KEY_USER_ID, -1)
        val expiryTime = prefs.getLong(KEY_EXPIRY_TIME, 0)
        val currentTime = System.currentTimeMillis()
        
        // 检查会话是否过期
        if (isLoggedIn && userId != -1L) {
            if (currentTime > expiryTime) {
                // 会话已过期，清除会话
                clearSession()
                return false
            }
            return true
        }
        
        return false
    }
    
    // 🔥 新增：刷新会话（延长有效期）
    fun refreshSession() {
        if (isLoggedIn()) {
            val currentTime = System.currentTimeMillis()
            val expiryTime = currentTime + SESSION_VALIDITY_MILLIS
            prefs.edit().apply {
                putLong(KEY_EXPIRY_TIME, expiryTime)
                apply()
            }
        }
    }
    
    // 🔥 新增：获取会话剩余时间（毫秒）
    fun getSessionRemainingTime(): Long {
        val expiryTime = prefs.getLong(KEY_EXPIRY_TIME, 0)
        val currentTime = System.currentTimeMillis()
        return maxOf(0, expiryTime - currentTime)
    }
    
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
