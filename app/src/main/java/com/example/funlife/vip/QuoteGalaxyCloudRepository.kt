// QuoteGalaxyCloudRepository.kt — v53 阅光书房 · 匿名摘抄星河（云端代理）
//
// 与 ChatAiCloudRepository 同构，路径 /quote_galaxy/<action>。
// 所有请求带凭证 + HMAC 签名，由云函数侧统一鉴权与频控。
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

class QuoteGalaxyCloudRepository(
    private val context: Context,
    private val baseUrl: String = BuildConfig.VIP_BACKEND_URL,
) {
    private val http: OkHttpClient = SecureHttp.client()
    private val gson = Gson()
    private val store = VipCertificateStore(context)

    /* ---------------- 数据 ---------------- */

    data class StarItem(
        @SerializedName("id") val id: String,
        @SerializedName("text") val text: String,
        @SerializedName("bookTitle") val bookTitle: String = "",
        @SerializedName("publishedAt") val publishedAt: Long = 0L,
        @SerializedName("lightCount") val lightCount: Int = 0,
    )

    private data class FeedResp(
        val ok: Boolean = false,
        val code: String? = null,
        val msg: String? = null,
        val items: List<StarItem> = emptyList(),
        val nextCursor: String? = null,
    )

    private data class GenericResp(
        val ok: Boolean = false,
        val code: String? = null,
        val msg: String? = null,
        val id: String? = null,
        val lightCount: Int? = null,
    )

    sealed class Result<out T> {
        data class Ok<T>(val value: T) : Result<T>()
        data class Err(val code: String, val msg: String) : Result<Nothing>()
    }

    /* ---------------- 拉星河 ---------------- */

    suspend fun feed(userId: Long, cursor: String? = null, limit: Int = 30): Result<Pair<List<StarItem>, String?>> =
        callWithAuth(userId, "feed", mapOf("cursor" to cursor, "limit" to limit)) { body ->
            val r = gson.fromJson(body, FeedResp::class.java)
            if (r.ok) Result.Ok(r.items to r.nextCursor)
            else Result.Err(r.code ?: "UNKNOWN", r.msg ?: "拉取失败")
        }

    /* ---------------- 发布（VIP1+） ---------------- */

    suspend fun publish(userId: Long, text: String, bookTitle: String): Result<String> =
        callWithAuth(userId, "publish", mapOf("text" to text, "bookTitle" to bookTitle)) { body ->
            val r = gson.fromJson(body, GenericResp::class.java)
            if (r.ok && !r.id.isNullOrBlank()) Result.Ok(r.id)
            else Result.Err(r.code ?: "UNKNOWN", r.msg ?: "发布失败")
        }

    /* ---------------- 接住⭐（点亮） ---------------- */

    suspend fun light(userId: Long, starId: String): Result<Int> =
        callWithAuth(userId, "light", mapOf("id" to starId)) { body ->
            val r = gson.fromJson(body, GenericResp::class.java)
            if (r.ok) Result.Ok(r.lightCount ?: -1)
            else Result.Err(r.code ?: "UNKNOWN", r.msg ?: "点亮失败")
        }

    /* ---------------- 举报 ---------------- */

    suspend fun report(userId: Long, starId: String, reason: String = ""): Result<Unit> =
        callWithAuth(userId, "report", mapOf("id" to starId, "reason" to reason)) { body ->
            val r = gson.fromJson(body, GenericResp::class.java)
            if (r.ok) Result.Ok(Unit) else Result.Err(r.code ?: "UNKNOWN", r.msg ?: "举报失败")
        }

    /* ---------------- 通用调用 ---------------- */

    private suspend fun <T> callWithAuth(
        userId: Long,
        action: String,
        body: Map<String, Any?>,
        parser: (String) -> Result<T>,
    ): Result<T> = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank() || !baseUrl.startsWith("http"))
            return@withContext Result.Err("NO_BACKEND", "云端未配置")
        val pair = store.load(userId)
            ?: return@withContext Result.Err("NO_CERT", "未登录或无 VIP 凭证")
        val (cert, sig) = pair
        val payload = mapOf("certificate" to cert, "signature" to sig, "action" to action, "body" to body)
        try {
            val req = Request.Builder()
                .url(baseUrl.trimEnd('/') + "/quote_galaxy")
                .post(gson.toJson(payload).toRequestBody(JSON))
                .build()
            http.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@use Result.Err("HTTP_${resp.code}", "网络异常(${resp.code})")
                parser(text)
            }
        } catch (e: Exception) {
            Result.Err("NETWORK_ERROR", e.message ?: "网络错误")
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
        fun isEnabled(): Boolean = BuildConfig.VIP_BACKEND_URL.isNotBlank()
    }
}
