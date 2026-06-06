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

    // ── 聊天 AI 额度卡凭证（与 VIP 凭证分 key 存储，互不覆盖） ──

    fun saveChatAi(userId: Long, cert: VipCertificate, signature: String, redeemCode: String? = null) {
        val editor = prefs.edit()
            .putString(keyChatAiCert(userId), gson.toJson(cert))
            .putString(keyChatAiSig(userId), signature)
        if (!redeemCode.isNullOrBlank()) {
            editor.putString(keyChatAiCode(userId), redeemCode)
        }
        editor.apply()
    }

    fun loadChatAi(userId: Long): Pair<VipCertificate, String>? {
        val certJson = prefs.getString(keyChatAiCert(userId), null) ?: return null
        val sig = prefs.getString(keyChatAiSig(userId), null) ?: return null
        return try {
            gson.fromJson(certJson, VipCertificate::class.java) to sig
        } catch (e: Exception) {
            null
        }
    }

    fun loadChatAiRedeemCode(userId: Long): String? =
        prefs.getString(keyChatAiCode(userId), null)

    fun clearChatAi(userId: Long) {
        prefs.edit()
            .remove(keyChatAiCert(userId))
            .remove(keyChatAiSig(userId))
            .remove(keyChatAiCode(userId))
            .apply()
    }

    /** 云端 /chat_ai 调用：优先 VIP 凭证，其次 AI 卡凭证 */
    fun loadForChatAi(userId: Long): Pair<VipCertificate, String>? =
        load(userId) ?: loadChatAi(userId)

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun keyCert(userId: Long) = "cert_$userId"
    private fun keySig(userId: Long) = "sig_$userId"
    private fun keyCode(userId: Long) = "code_$userId"
    private fun keyChatAiCert(userId: Long) = "chat_ai_cert_$userId"
    private fun keyChatAiSig(userId: Long) = "chat_ai_sig_$userId"
    private fun keyChatAiCode(userId: Long) = "chat_ai_code_$userId"
}
