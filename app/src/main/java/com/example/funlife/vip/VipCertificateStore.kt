package com.example.funlife.vip

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson

/**
 * 本地凭证存储（EncryptedSharedPreferences，AES-256-GCM）
 *
 * 按 userId 隔离：多账号场景下每个用户的 VIP 凭证独立。
 * 破解者拿到 APK 也无法直接读 / 改凭证（除非 root + frida）。
 */
class VipCertificateStore(context: Context) {
    private val masterKey = MasterKey.Builder(context.applicationContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context.applicationContext,
        "vip_cert_store",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val gson = Gson()

    fun save(userId: Long, cert: VipCertificate, signature: String, redeemCode: String? = null) {
        val editor = prefs.edit()
            .putString(keyCert(userId), gson.toJson(cert))
            .putString(keySig(userId), signature)
        // 仅在 redeemCode 非空时写入（迁移/复验场景不传 code，要保留原 code）
        if (!redeemCode.isNullOrBlank()) {
            editor.putString(keyCode(userId), redeemCode)
        }
        editor.apply()
    }

    fun load(userId: Long): Pair<VipCertificate, String>? {
        val certJson = prefs.getString(keyCert(userId), null) ?: return null
        val sig = prefs.getString(keySig(userId), null) ?: return null
        return try {
            gson.fromJson(certJson, VipCertificate::class.java) to sig
        } catch (e: Exception) {
            null
        }
    }

    /** 取当前用户的兑换码（用于导出迁移凭证） */
    fun loadRedeemCode(userId: Long): String? = prefs.getString(keyCode(userId), null)

    fun clear(userId: Long) {
        prefs.edit()
            .remove(keyCert(userId))
            .remove(keySig(userId))
            .remove(keyCode(userId))
            .apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun keyCert(userId: Long) = "cert_$userId"
    private fun keySig(userId: Long) = "sig_$userId"
    private fun keyCode(userId: Long) = "code_$userId"
}
