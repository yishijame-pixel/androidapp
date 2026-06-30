package com.example.funlife.ui.screens.pacmaze

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.funlife.BuildConfig
import com.example.funlife.vip.SecureHttp
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** ikun 类进入前必读须知（后台 [pac_maze_config] 云函数可配置）。 */
object PacMazeIkunDisclosureConfig {

    private const val PREFS = "pac_maze_ikun_disclosure"
    private const val KEY_JSON = "config_json"
    private const val KEY_FETCHED_AT = "fetched_at_ms"
    private const val THROTTLE_MS = 30_000L

    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var loaded = false
    @Volatile private var lastFetchedAtMs = 0L

    var revision by mutableStateOf(0)
        private set

    @Volatile
    var current: PacMazeIkunDisclosureContent = PacMazeIkunDisclosureContent.defaults()
        private set

    fun loadFromCache(context: Context) {
        if (loaded) return
        runCatching {
            val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_JSON, null) ?: return
            applyJson(json)
            loaded = true
        }
    }

    fun refreshAsync(context: Context, force: Boolean = false) {
        scope.launch { runCatching { refresh(context.applicationContext, force) } }
    }

    suspend fun refresh(context: Context, force: Boolean = false) = withContext(Dispatchers.IO) {
        if (!force && System.currentTimeMillis() - lastFetchedAtMs < THROTTLE_MS) return@withContext
        val baseUrl = BuildConfig.VIP_BACKEND_URL.trimEnd('/')
        if (baseUrl.isBlank() || !baseUrl.startsWith("http")) return@withContext
        try {
            val req = Request.Builder()
                .url("$baseUrl/pac_maze_config")
                .post("{}".toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            SecureHttp.client().newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use
                val text = resp.body?.string().orEmpty()
                if (text.isBlank()) return@use
                applyJson(text)
                lastFetchedAtMs = System.currentTimeMillis()
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(KEY_JSON, text)
                    .putLong(KEY_FETCHED_AT, lastFetchedAtMs)
                    .apply()
                loaded = true
            }
        } catch (_: Exception) {
            // 保留缓存 / 默认文案
        }
    }

    private fun applyJson(json: String) {
        val root = runCatching { gson.fromJson(json, ApiEnvelope::class.java) }.getOrNull() ?: return
        val data = root.data ?: return
        current = PacMazeIkunDisclosureContent(
            version = data.version,
            enabled = data.enabled,
            title = data.title?.takeIf { it.isNotBlank() } ?: PacMazeIkunDisclosureContent.defaults().title,
            body = data.body?.takeIf { it.isNotBlank() } ?: PacMazeIkunDisclosureContent.defaults().body,
            agreeButtonText = data.agreeButtonText?.takeIf { it.isNotBlank() }
                ?: PacMazeIkunDisclosureContent.defaults().agreeButtonText,
            footerHint = data.footerHint?.takeIf { it.isNotBlank() }
                ?: PacMazeIkunDisclosureContent.defaults().footerHint,
        )
        revision++
    }

    private data class ApiEnvelope(
        val ok: Boolean = false,
        val data: ApiPayload? = null,
    )

    private data class ApiPayload(
        val version: Long = 1L,
        val enabled: Boolean = true,
        @SerializedName("title") val title: String? = null,
        @SerializedName("body") val body: String? = null,
        @SerializedName("agreeButtonText") val agreeButtonText: String? = null,
        @SerializedName("footerHint") val footerHint: String? = null,
    )
}

data class PacMazeIkunDisclosureContent(
    val version: Long,
    val enabled: Boolean,
    val title: String,
    val body: String,
    val agreeButtonText: String,
    val footerHint: String,
) {
    companion object {
        fun defaults() = PacMazeIkunDisclosureContent(
            version = 1L,
            enabled = true,
            title = "ikun类角色使用须知",
            body = """
                欢迎使用「ikun类」梗图行走角色。进入本分类前，请先阅读以下说明：

                1. 本分类角色为娱乐向二次创作形象，部分素材来自网络梗图或用户上传的角色资源包，仅供游戏内娱乐体验。

                2. 请理性使用角色形象，勿用于侮辱、诽谤、骚扰他人，或从事任何违法违规活动。

                3. 若您认为某角色形象涉及侵权或不当内容，可通过应用内反馈渠道联系我们，我们将及时核实处理。

                4. 继续使用即表示您已理解上述说明，并同意在合法、合规、尊重他人的前提下使用本分类角色。

                感谢您的配合，祝您游戏愉快。
            """.trimIndent(),
            agreeButtonText = "我已阅读并同意",
            footerHint = "请滑动阅读全文后再点击同意",
        )
    }
}
