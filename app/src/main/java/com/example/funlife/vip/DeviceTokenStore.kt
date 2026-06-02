package com.example.funlife.vip

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 服务端签发的 device_token 持久化（加密存储）
 *
 * 由 register_log 在注册成功时返回，后续所有写入云端的请求都要带上。
 * 用 username 隔离：换账号注册不会污染。
 */
class DeviceTokenStore(context: Context) {

    private val prefs = try {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            "device_token_store",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // 降级：明文 SP（device_token 本身已经是签名过的，不算超敏感数据）
        context.applicationContext.getSharedPreferences("device_token_store_fallback", Context.MODE_PRIVATE)
    }

    fun save(username: String, token: String) {
        prefs.edit().putString(keyOf(username), token).apply()
    }

    fun load(username: String): String? = prefs.getString(keyOf(username), null)

    fun clear(username: String) {
        prefs.edit().remove(keyOf(username)).apply()
    }

    private fun keyOf(u: String) = "tok_" + u.trim().lowercase()
}
