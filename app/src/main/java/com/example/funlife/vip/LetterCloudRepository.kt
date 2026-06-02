// LetterCloudRepository.kt — 时光信箱 AI 云函数代理客户端
//
// 🎯 职责
//   把"为信件生成 AI 回信"的请求发到云函数 /letter_ai。
//   云函数会做：HMAC 校验 → 查 DB 验真实 vipLevel → 服务端权威配额 → 调 LLM → 返回 reply
//
// 🔒 安全
//   - 复用 SecureHttp.client()：自带 CertPin（BuildConfig.VIP_BACKEND_PIN）
//   - 凭证从 VipCertificateStore 取 → 保证服务端能识别真实 VIP 身份
//   - 普通用户（无 cert）也允许调用：服务端按 vipLevel=0 配额（每月 1 次）
//   - 服务端 KEY 不下发；客户端 BuildConfig.AI_API_KEY 仅作为兜底直连降级用
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

class LetterCloudRepository(
    private val context: Context,
    private val baseUrl: String = BuildConfig.VIP_BACKEND_URL
) {
    private val http: OkHttpClient = SecureHttp.client()
    private val gson = Gson()
    private val store = VipCertificateStore(context)

    /** 调用方传入的业务参数（与云函数 letter_ai/index.js 字段一一对应） */
    data class Body(
        @SerializedName("letterId") val letterId: String,
        @SerializedName("recipientName") val recipientName: String,
        @SerializedName("relation") val relation: String,
        @SerializedName("persona") val persona: String,
        @SerializedName("timeAnchor") val timeAnchor: Long? = null,
        @SerializedName("userLetter") val userLetter: String,
        @SerializedName("mood") val mood: String? = null
    )

    /** 云函数响应 */
    data class Response(
        val ok: Boolean,
        val code: String? = null,
        val msg: String? = null,
        val reply: String? = null,
        val used: Int? = null,
        val quota: Int? = null,
        val vipLevel: Int? = null
    )

    sealed class CallResult {
        data class Success(val reply: String, val used: Int, val quota: Int) : CallResult()
        /** 云端权威配额已达上限，客户端不应再降级直连（防止绕过） */
        data class QuotaExceeded(val used: Int, val quota: Int, val vipLevel: Int) : CallResult()
        /** 云端不可用 / 网络异常 / 配置缺失 → 调用方可决定是否降级到本地直连 */
        data class Recoverable(val code: String, val msg: String) : CallResult()
        /** 云端明确拒绝（凭证无效、限流、签名错） → 不应降级 */
        data class Rejected(val code: String, val msg: String) : CallResult()
    }

    /**
     * @param userId 当前登录用户（用于取出该用户的 cert）
     */
    suspend fun generateReply(userId: Long, body: Body): CallResult = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank() || !baseUrl.startsWith("http")) {
            return@withContext CallResult.Recoverable("NO_BACKEND", "云端地址未配置")
        }
        // 取本地 cert（无 cert = 普通用户，依然可用免费额度）
        val pair = store.load(userId)
        val (cert, sig) = if (pair != null) pair else (null to null)

        val req = mutableMapOf<String, Any?>(
            "body" to body
        )
        if (cert != null && !sig.isNullOrBlank()) {
            req["certificate"] = cert
            req["signature"] = sig
        } else {
            // 服务端要求 certificate 字段。无 VIP 用户：发"伪凭证"占位（服务端验签会失败，但
            // 普通用户不应该走云函数代理 → 让上层降级直连本地 AI 即可）
            // 这里直接返回 Recoverable，避免无意义的网络请求
            return@withContext CallResult.Recoverable("NO_CERT", "未登录或无 VIP 凭证")
        }
        try {
            val httpReq = Request.Builder()
                .url(baseUrl.trimEnd('/') + "/letter_ai")
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
                        reply = r.reply,
                        used = r.used ?: -1,
                        quota = r.quota ?: -1
                    )
                }
                when (r.code) {
                    "QUOTA_EXCEEDED" -> CallResult.QuotaExceeded(
                        used = r.used ?: -1, quota = r.quota ?: -1, vipLevel = r.vipLevel ?: 0
                    )
                    // 这些错误明确"不该降级"——降级也救不了，反而暴露行踪
                    "BAD_SIGNATURE", "CERT_EXPIRED", "DISABLED", "REVOKED",
                    "RATE_LIMITED", "INVALID", "BAD_REQUEST" ->
                        CallResult.Rejected(r.code, r.msg ?: "请求被拒绝")
                    // SERVER_MISCONFIG / DB_ERROR / LLM_FAILED / NO_BACKEND → 可降级直连
                    else -> CallResult.Recoverable(r.code ?: "UNKNOWN", r.msg ?: "云端异常")
                }
            }
        } catch (e: Exception) {
            CallResult.Recoverable("NETWORK_ERROR", e.message ?: "网络错误")
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        /**
         * 全局开关：BuildConfig.LETTER_AI_USE_PROXY = false 关代理（开发态全直连）
         * 默认 true（生产：代理优先，失败再降级直连）
         */
        fun isEnabled(): Boolean =
            BuildConfig.LETTER_AI_USE_PROXY &&
            BuildConfig.VIP_BACKEND_URL.isNotBlank()
    }
}
