package com.example.funlife.social

import android.content.Context
import android.net.Uri
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.funlife.BuildConfig
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * PocketBase 专用 HTTP 客户端。
 * - Token 不落日志（Authorization redact）
 * - Release 禁止 BODY 日志
 * - 可选证书 Pin（POCKETBASE_PIN）
 */
object PocketBaseHttp {

    fun client(): OkHttpClient = newBuilder().build()

    fun newBuilder(): OkHttpClient.Builder {
        val readSec = if (PocketBaseConfig.isRemote()) 45L else 20L
        val connectSec = if (PocketBaseConfig.isRemote()) 20L else 12L
        val builder = OkHttpClient.Builder()
            .connectTimeout(connectSec, TimeUnit.SECONDS)
            .readTimeout(readSec, TimeUnit.SECONDS)
            .writeTimeout(readSec, TimeUnit.SECONDS)

        applyPinning(builder)

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
                redactHeader("Authorization")
                redactHeader("Cookie")
            }
            builder.addInterceptor(logging)
        }

        return builder
    }

    private fun applyPinning(builder: OkHttpClient.Builder) {
        val pinSpec = BuildConfig.POCKETBASE_PIN
        val baseUrl = BuildConfig.POCKETBASE_URL
        if (pinSpec.isBlank() || baseUrl.isBlank()) return
        val host = runCatching { Uri.parse(baseUrl).host }.getOrNull() ?: return
        val pinner = CertificatePinner.Builder().apply {
            pinSpec.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { add(host, it) }
        }.build()
        builder.certificatePinner(pinner)
    }
}

/**
 * 按本地 userId 隔离的 PocketBase Token / 派生密码（EncryptedSharedPreferences）。
 * 不存储 FunLife 明文密码。
 */
object SocialSecureStore {

    private const val PREFS = "social_pb_secure"

    private fun prefs(context: Context) = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (_: Exception) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    private fun tokenKey(userId: Long) = "pb_token_$userId"
    private fun pwdKey(userId: Long) = "pb_pwd_$userId"
    private fun avatarSyncKey(userId: Long) = "pb_avatar_uri_$userId"

    fun getToken(context: Context, userId: Long): String? =
        prefs(context).getString(tokenKey(userId), null)?.takeIf { it.isNotBlank() }

    fun saveToken(context: Context, userId: Long, token: String) {
        prefs(context).edit().putString(tokenKey(userId), token).apply()
    }

    fun getOrCreatePassword(context: Context, userId: Long): String {
        val p = prefs(context)
        p.getString(pwdKey(userId), null)?.let { if (it.isNotBlank()) return it }
        val generated = generatePassword()
        p.edit().putString(pwdKey(userId), generated).apply()
        return generated
    }

    fun getLastSyncedAvatarUri(context: Context, userId: Long): String? =
        prefs(context).getString(avatarSyncKey(userId), null)?.takeIf { it.isNotBlank() }

    fun saveLastSyncedAvatarUri(context: Context, userId: Long, avatarUri: String) {
        prefs(context).edit().putString(avatarSyncKey(userId), avatarUri).apply()
    }

    fun clearUser(context: Context, userId: Long) {
        prefs(context).edit()
            .remove(tokenKey(userId))
            .remove(pwdKey(userId))
            .remove(avatarSyncKey(userId))
            .apply()
    }

    private fun generatePassword(): String {
        val bytes = ByteArray(32)
        java.security.SecureRandom().nextBytes(bytes)
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }

    /** 合成 PocketBase 登录 identity（非真实邮箱，仅作唯一账号标识） */
    fun syntheticIdentity(localUserId: Long, funlifeUsername: String): String {
        val safe = funlifeUsername.lowercase().replace(Regex("[^a-z0-9_]"), "")
        return "u${localUserId}_${safe}@funlife.social.invalid"
    }
}
