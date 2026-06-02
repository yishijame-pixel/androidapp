// PostcardDriftCloudRepository.kt — v53 阅光书房 · 明信片漂流（云端代理）
//
// 端点：/postcard_drift  action=send|inbox|react
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

class PostcardDriftCloudRepository(
    private val context: Context,
    private val baseUrl: String = BuildConfig.VIP_BACKEND_URL,
) {
    private val http: OkHttpClient = SecureHttp.client()
    private val gson = Gson()
    private val store = VipCertificateStore(context)

    data class Postcard(
        @SerializedName("id") val id: String,
        @SerializedName("text") val text: String,
        @SerializedName("bookTitle") val bookTitle: String = "",
        @SerializedName("sentAt") val sentAt: Long = 0L,
        @SerializedName("reactedHeart") val reactedHeart: Boolean = false,
    )

    private data class GenericResp(
        val ok: Boolean = false,
        val code: String? = null,
        val msg: String? = null,
        val id: String? = null,
        val items: List<Postcard> = emptyList(),
        val totalReceived: Int? = null,
        val used: Int? = null,
        val limit: Int? = null,
        val vipLevel: Int? = null,
    )

    sealed class Result<out T> {
        data class Ok<T>(val value: T) : Result<T>()
        data class QuotaExceeded(val used: Int, val limit: Int, val vipLevel: Int) : Result<Nothing>()
        data class Err(val code: String, val msg: String) : Result<Nothing>()
    }

    /** 寄出明信片（VIP2+，按月配额） */
    suspend fun send(userId: Long, quoteText: String, bookTitle: String): Result<String> =
        call(userId, "send", mapOf("text" to quoteText, "bookTitle" to bookTitle)) { r ->
            when {
                r.ok && !r.id.isNullOrBlank() -> Result.Ok(r.id)
                r.code == "QUOTA_EXCEEDED" -> Result.QuotaExceeded(
                    r.used ?: -1, r.limit ?: -1, r.vipLevel ?: 0
                )
                else -> Result.Err(r.code ?: "UNKNOWN", r.msg ?: "寄送失败")
            }
        }

    /** 收件箱（已收到的明信片，最多 50 条） */
    suspend fun inbox(userId: Long): Result<List<Postcard>> =
        call(userId, "inbox", emptyMap()) { r ->
            if (r.ok) Result.Ok(r.items) else Result.Err(r.code ?: "UNKNOWN", r.msg ?: "拉取失败")
        }

    /** 给一张收到的明信片点❤ */
    suspend fun react(userId: Long, postcardId: String): Result<Unit> =
        call(userId, "react", mapOf("id" to postcardId)) { r ->
            if (r.ok) Result.Ok(Unit) else Result.Err(r.code ?: "UNKNOWN", r.msg ?: "操作失败")
        }

    private suspend fun <T> call(
        userId: Long,
        action: String,
        body: Map<String, Any?>,
        parse: (GenericResp) -> Result<T>,
    ): Result<T> = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank() || !baseUrl.startsWith("http"))
            return@withContext Result.Err("NO_BACKEND", "云端未配置")
        val pair = store.load(userId)
            ?: return@withContext Result.Err("NO_CERT", "未登录或无 VIP 凭证")
        val (cert, sig) = pair
        val payload = mapOf("certificate" to cert, "signature" to sig, "action" to action, "body" to body)
        try {
            val req = Request.Builder()
                .url(baseUrl.trimEnd('/') + "/postcard_drift")
                .post(gson.toJson(payload).toRequestBody(JSON))
                .build()
            http.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@use Result.Err("HTTP_${resp.code}", "网络异常(${resp.code})")
                val r = runCatching { gson.fromJson(text, GenericResp::class.java) }.getOrNull()
                    ?: return@use Result.Err("PARSE_ERROR", "响应解析失败")
                parse(r)
            }
        } catch (e: Exception) {
            Result.Err("NETWORK_ERROR", e.message ?: "网络错误")
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
