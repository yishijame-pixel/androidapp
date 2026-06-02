// LetterCrypto.kt — 时光信箱：信件正文 AES-GCM 加密
//
// 🎯 设计目标（企业级）
//   - 信件内容（用户对已故亲人的倾诉 / 写给未来自己的隐私）属于"高敏感"数据
//   - 即使 DB 被 root 用户导出（如 adb pull /data/.../databases）也不可读
//   - 不同用户的信件用同一 master key 但走不同 AAD（Additional Authenticated Data）
//     → 越权读取（伪造 userId / recipientId）会触发 GCM tag 校验失败
//
// 🔑 密钥生命周期
//   - Master key 由 AndroidKeyStore 托管（硬件支持时进 TEE / SE），不可导出
//   - 钥匙生成一次后永驻 KeyStore，应用卸载 / KeyStore 重置时被销毁
//   - 销毁后历史密文不可恢复（与"应用数据被删"语义一致）
//
// 🧱 算法
//   - AES/GCM/NoPadding, key=256bit
//   - 每次加密随机生成 12 字节 IV
//   - 输出格式 = base64(IV(12B) || ciphertext+tag)
//   - AAD = "letter:v1:{userId}:{recipientId}"，密文与 userId/recipientId 绑定
//
// ⚠ 兼容性
//   - API 23+（AndroidKeyStore AES 支持自 API 23 起）
//   - 旧设备走 fallback：仍 AES/GCM，但 key 用 EncryptedSharedPreferences 持久化
package com.example.funlife.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object LetterCrypto {

    private const val TAG = "LetterCrypto"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "funlife_letter_master_v1"
    private const val FALLBACK_PREFS = "letter_crypto_fb"
    private const val FALLBACK_KEY = "k"
    private const val GCM_IV_LEN = 12      // 96-bit IV，AES-GCM 推荐值
    private const val GCM_TAG_BITS = 128
    private val rng = SecureRandom()

    // ─────────── 公共 API ───────────

    /**
     * 加密信件内容。
     * @param plain      明文
     * @param userId     拥有者 userId（≥1）
     * @param recipientId 收信人 id（≥1）
     * @return base64(IV || ciphertext||tag)
     */
    @JvmStatic
    fun encrypt(context: Context, plain: String, userId: Long, recipientId: Long): String {
        require(userId > 0L) { "userId must be > 0" }
        require(recipientId > 0L) { "recipientId must be > 0" }
        val key = obtainKey(context)
        val iv = ByteArray(GCM_IV_LEN).also { rng.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            updateAAD(aad(userId, recipientId))
        }
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val out = ByteArray(iv.size + ct.size).apply {
            System.arraycopy(iv, 0, this, 0, iv.size)
            System.arraycopy(ct, 0, this, iv.size, ct.size)
        }
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    /**
     * 解密。AAD 不匹配（即试图把 A 用户的信件挪到 B 用户名下）→ AEADBadTagException。
     * 失败返回 null，由调用方决定 fallback（如显示"无法读取"占位文案）。
     */
    @JvmStatic
    fun decrypt(context: Context, cipherText: String, userId: Long, recipientId: Long): String? {
        if (cipherText.isBlank()) return null
        // 兼容：旧数据若是明文（不含 IV 头），直接返回
        if (!isLikelyCipher(cipherText)) return cipherText
        return try {
            val all = Base64.decode(cipherText, Base64.NO_WRAP)
            if (all.size <= GCM_IV_LEN) return null
            val iv = all.copyOfRange(0, GCM_IV_LEN)
            val ct = all.copyOfRange(GCM_IV_LEN, all.size)
            val key = obtainKey(context)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
                updateAAD(aad(userId, recipientId))
            }
            String(cipher.doFinal(ct), Charsets.UTF_8)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "decrypt failed (userId=$userId, recipientId=$recipientId): ${e.javaClass.simpleName}")
            null
        }
    }

    // ─────────── 内部 ───────────

    private fun aad(userId: Long, recipientId: Long): ByteArray =
        "letter:v1:$userId:$recipientId".toByteArray(Charsets.UTF_8)

    /** 快速判别：是否像我们的密文格式（base64 + 长度合理） */
    private fun isLikelyCipher(s: String): Boolean {
        if (s.length < 24) return false
        // 朴素 base64 字符集校验
        return s.all { it.isLetterOrDigit() || it == '+' || it == '/' || it == '=' }
    }

    @Synchronized
    private fun obtainKey(context: Context): SecretKey {
        // 优先 AndroidKeyStore
        try {
            val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
            return generateKeystoreKey()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "AndroidKeyStore unavailable, falling back: ${e.message}")
            return fallbackKey(context)
        }
    }

    private fun generateKeystoreKey(): SecretKey {
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
        // API 28+ 强制 StrongBox（如硬件支持，否则忽略）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try { builder.setIsStrongBoxBacked(true) } catch (_: Throwable) {}
        }
        gen.init(builder.build())
        return gen.generateKey()
    }

    /**
     * Keystore 不可用时的兜底：用 EncryptedSharedPreferences 持久化一份 32 字节随机 key。
     * 仍然比明文落盘强。
     */
    private fun fallbackKey(context: Context): SecretKey {
        val prefs = try {
            val mk = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context.applicationContext, FALLBACK_PREFS, mk,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            context.applicationContext.getSharedPreferences(FALLBACK_PREFS + "_plain", Context.MODE_PRIVATE)
        }
        val existing = prefs.getString(FALLBACK_KEY, null)
        val bytes = if (existing.isNullOrEmpty()) {
            val fresh = ByteArray(32).also { rng.nextBytes(it) }
            prefs.edit().putString(FALLBACK_KEY, Base64.encodeToString(fresh, Base64.NO_WRAP)).apply()
            fresh
        } else {
            Base64.decode(existing, Base64.NO_WRAP)
        }
        return SecretKeySpec(bytes, "AES")
    }
}
