// ChatAiCloudRepository.kt — 聊天记账 AI 云函数代理客户端
//
// 与 LetterCloudRepository 设计同构，差异：
//   - 路径 /chat_ai
//   - 配额按"日"扣减，不要 letterId 幂等
//   - body 字段：mode / personaSystem / userText
package com.example.funlife.vip

import android.content.Context
import com.example.funlife.BuildConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ChatAiCloudRepository(
    private val context: Context,
    private val baseUrl: String = BuildConfig.VIP_BACKEND_URL
) {
    private val http: OkHttpClient = SecureHttp.client()
    private val gson = Gson()
    private val store = VipCertificateStore(context)

    data class Body(
        @SerializedName("mode") val mode: String,                // "bill" | "chat"
        @SerializedName("personaSystem") val personaSystem: String,
        @SerializedName("userText") val userText: String
    )

    data class Response(
        val ok: Boolean,
        val code: String? = null,
        val msg: String? = null,
        val reply: String? = null,
        val used: Int? = null,
        val limit: Int? = null,
        val usedMonth: Int? = null,
        val limitMonth: Int? = null,
        val tier: Int? = null,
        val source: String? = null,
        val poolType: String? = null,
        val vipLevel: Int? = null
    )

    sealed class CallResult {
        data class Success(val reply: String, val used: Int, val limit: Int) : CallResult()
        data class QuotaExceeded(val used: Int, val limit: Int, val vipLevel: Int) : CallResult()
        data class Recoverable(val code: String, val msg: String) : CallResult()
        data class Rejected(val code: String, val msg: String) : CallResult()
    }

    suspend fun reply(userId: Long, body: Body): CallResult = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank() || !baseUrl.startsWith("http")) {
            return@withContext CallResult.Recoverable("NO_BACKEND", "云端地址未配置")
        }
        val pair = store.loadForChatAi(userId)
        if (pair == null) return@withContext CallResult.Recoverable("NO_CERT", "未激活 AI 额度卡")
        val (cert, sig) = pair
        val req = mapOf(
            "certificate" to cert,
            "signature" to sig,
            "body" to body
        )
        try {
            val httpReq = Request.Builder()
                .url(baseUrl.trimEnd('/') + "/chat_ai")
                .post(gson.toJson(req).toRequestBody(JSON))
                .build()
            http.newCall(httpReq).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@use CallResult.Recoverable("HTTP_${resp.code}", "网络异常(${resp.code})")
                }
                val r = runCatching { gson.fromJson(text, Response::class.java) }.getOrNull()
                    ?: return@use CallResult.Recoverable("PARSE_ERROR", "响应解析失败")
                if (r.ok && !r.reply.isNullOrBlank()) {
                    return@use CallResult.Success(
                        reply = r.reply, used = r.used ?: -1, limit = r.limit ?: -1
                    )
                }
                when (r.code) {
                    "QUOTA_EXCEEDED", "MONTHLY_CAP", "TRIAL_EXHAUSTED" -> CallResult.QuotaExceeded(
                        used = r.used ?: -1, limit = r.limit ?: -1, vipLevel = r.tier ?: r.vipLevel ?: 0
                    )
                    "NO_ENTITLEMENT", "BAD_SIGNATURE", "CERT_EXPIRED", "DISABLED", "REVOKED",
                    "RATE_LIMITED", "INVALID", "BAD_REQUEST" ->
                        CallResult.Rejected(r.code, r.msg ?: "请求被拒绝")
                    else -> CallResult.Recoverable(r.code ?: "UNKNOWN", r.msg ?: "云端异常")
                }
            }
        } catch (e: Exception) {
            CallResult.Recoverable("NETWORK_ERROR", e.message ?: "网络错误")
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        fun isEnabled(): Boolean =
            BuildConfig.CHAT_AI_USE_PROXY &&
            BuildConfig.VIP_BACKEND_URL.isNotBlank()
    }
}
