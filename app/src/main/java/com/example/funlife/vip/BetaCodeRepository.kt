package com.example.funlife.vip

import android.content.Context
import com.example.funlife.BuildConfig
import com.example.funlife.security.SecurityManager
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 内测邀请码校验客户端
 *
 * - 调云端 /beta_validate
 * - 网络失败时返回 NetworkError，由调用方决定是否走本地兜底
 */
class BetaCodeRepository(private val context: Context) {

    private val gson = Gson()
    private val client = SecureHttp.newBuilder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()

    sealed class Result {
        object Ok : Result()
        data class Failed(val code: String, val msg: String) : Result()
        data class NetworkError(val cause: Throwable) : Result()
    }

    /**
     * 调云端校验内测码。注册阶段调用。
     * 已验证过的同 username/deviceId 重新调返回幂等成功。
     */
    fun validate(code: String, username: String): Result {
        val baseUrl = BuildConfig.VIP_BACKEND_URL
        if (baseUrl.isNullOrBlank()) return Result.NetworkError(IllegalStateException("VIP_BACKEND_URL 未配置"))

        return try {
            val deviceId = SecurityManager.getDeviceFingerprint(context)
            val body = mapOf(
                "code" to code.trim(),
                "username" to username.trim(),
                "deviceId" to deviceId,
            )
            val req = Request.Builder()
                .url(baseUrl.trimEnd('/') + "/beta_validate")
                .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (text.isBlank()) return Result.Failed("EMPTY", "服务器无响应")
                val r = gson.fromJson(text, Map::class.java) as Map<*, *>
                val ok = r["ok"] as? Boolean ?: false
                if (ok) Result.Ok
                else Result.Failed(
                    (r["code"] as? String) ?: "UNKNOWN",
                    (r["msg"] as? String) ?: "校验失败"
                )
            }
        } catch (e: Exception) {
            Result.NetworkError(e)
        }
    }
}
