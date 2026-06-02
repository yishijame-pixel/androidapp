package com.example.funlife.vip

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.funlife.BuildConfig
import com.example.funlife.data.model.VipLevel
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * VIP 运行时配置（从云端 vip_config 云函数拉取，可被后台动态调整）
 *
 * 设计目标：
 *   - 让客户端的 dailyCoins / bonusCoins / durationDays 等"运营可调"参数
 *     不必随版本发布，运营在后台改一次全部生效
 *   - 失败安全：网络异常 / 集合未初始化 → 自动回退到 VipLevel 枚举里的默认值
 *
 * 用法：
 *   - 启动时：VipRuntimeConfig.refreshAsync(context)
 *   - 业务里：VipRuntimeConfig.dailyCoinsOf(vipLevel) / bonusCoinsOf(vipLevel)
 */
object VipRuntimeConfig {

    private const val PREFS = "vip_runtime_config"
    private const val KEY_JSON = "config_json"
    private const val KEY_FETCHED_AT = "fetched_at_ms"

    // 内存缓存：vipLevel(Int) → 字段
    private val dailyCoinsMap = mutableMapOf<Int, Int>()
    private val bonusCoinsMap = mutableMapOf<Int, Int>()
    private val durationDaysMap = mutableMapOf<Int, Int>()
    private val nameMap = mutableMapOf<Int, String>()
    private val priceMap = mutableMapOf<Int, String>()        // 售价（元，字符串保留小数显示）

    @Volatile private var loaded = false
    @Volatile private var lastFetchedAtMs = 0L
    private const val THROTTLE_MS = 30_000L     // 30 秒内重复 refresh 走缓存

    /**
     * 版本号（Compose State）—— 每次 applyJson 成功就 +1
     * Composable 读取 priceOf / dailyCoinsOf 等会通过 version 间接订阅，云端拉到新值时自动重组。
     */
    var version by mutableStateOf(0)
        private set

    // ─── Compose 订阅辅助：用 also{} 而非 unused val，避免被 R8 优化掉 ───
    private inline fun <T> withSubscribe(value: T): T = value.also { version }

    /** 读取每日金币（运行时配置 > 默认枚举值） */
    fun dailyCoinsOf(level: VipLevel): Int = withSubscribe(
        if (level == VipLevel.NORMAL) level.dailyCoins
        else dailyCoinsMap[level.level] ?: level.dailyCoins
    )

    /** 读取激活赠币（云端缺失时回退到 sku.js 内置默认值，避免显示 0） */
    private val DEFAULT_BONUS_COINS = mapOf(1 to 50, 2 to 300, 3 to 1000, 99 to 1000)
    fun bonusCoinsOf(level: VipLevel): Int = withSubscribe(
        bonusCoinsMap[level.level] ?: DEFAULT_BONUS_COINS[level.level] ?: 0
    )

    /** 读取有效期天数（-1=永久） */
    private val DEFAULT_DURATION = mapOf(1 to 30, 2 to 365, 3 to -1, 99 to -1)
    fun durationDaysOf(level: VipLevel): Int? = withSubscribe(
        durationDaysMap[level.level] ?: DEFAULT_DURATION[level.level]
    )

    fun displayNameOf(level: VipLevel): String = withSubscribe(
        nameMap[level.level] ?: level.displayName
    )

    /** 售价（元，字符串）；后台未配置时返回 null，由调用方提供本地默认 */
    fun priceOf(level: VipLevel): String? = withSubscribe(priceMap[level.level])

    /** 从本地 SharedPreferences 加载（同步、应在主线程外调用） */
    fun loadFromCache(context: Context) {
        if (loaded) return
        try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_JSON, null) ?: return
            applyJson(json)
            loaded = true
        } catch (e: Exception) {
            android.util.Log.w("VipRuntimeConfig", "loadFromCache failed", e)
        }
    }

    /**
     * 异步刷新
     *  @param force true=忽略节流强制拉取（如用户主动下拉刷新）
     *               false=30 秒内已拉过则跳过（默认，避免每次进 VIP 页都打云函数）
     */
    fun refreshAsync(context: Context, force: Boolean = false) {
        scope.launch { runCatching { refresh(context, force) } }
    }

    /**
     * 应用启动调用：先 IO 线程读缓存（避免主线程 StrictMode 警告），再异步拉云端。
     * 缓存命中时即可让首屏价格立刻有值，云端拉取成功后再 version++ 自动重组。
     */
    fun bootstrapAsync(context: Context) {
        val app = context.applicationContext
        scope.launch {
            runCatching { loadFromCache(app) }
            runCatching { refresh(app, force = true) }
        }
    }

    /** 同步刷新（必须在 IO 协程中调用） */
    suspend fun refresh(context: Context, force: Boolean = false) = withContext(Dispatchers.IO) {
        if (!force && System.currentTimeMillis() - lastFetchedAtMs < THROTTLE_MS) return@withContext
        val baseUrl = BuildConfig.VIP_BACKEND_URL
        if (baseUrl.isBlank() || !baseUrl.startsWith("http")) return@withContext
        try {
            val req = Request.Builder()
                .url(baseUrl.trimEnd('/') + "/vip_config")
                .post("{}".toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            SecureHttp.client().newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use
                val text = resp.body?.string().orEmpty()
                applyJson(text)
                lastFetchedAtMs = System.currentTimeMillis()
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(KEY_JSON, text)
                    .putLong(KEY_FETCHED_AT, lastFetchedAtMs)
                    .apply()
                loaded = true
            }
        } catch (e: Exception) {
            android.util.Log.w("VipRuntimeConfig", "refresh failed: ${e.message}")
        }
    }

    /**
     * 解析云端 JSON：先写入本地 buffer，全部解析成功后一次性 commit。
     * 避免中途报错导致全局 map 被清空后仅部分填充的“脱靠”状态。
     */
    private fun applyJson(json: String) {
        val newDaily = mutableMapOf<Int, Int>()
        val newBonus = mutableMapOf<Int, Int>()
        val newDuration = mutableMapOf<Int, Int>()
        val newName = mutableMapOf<Int, String>()
        val newPrice = mutableMapOf<Int, String>()
        try {
            val gson = Gson()
            val root = gson.fromJson(json, JsonObject::class.java) ?: return
            val data = root.getAsJsonObject("data") ?: return
            val vipLevels = data.getAsJsonObject("vipLevels") ?: return
            vipLevels.entrySet().forEach { (key, value) ->
                val lvl = key.toIntOrNull() ?: return@forEach
                val obj = value.asJsonObject
                obj.get("dailyCoins")?.takeIf { !it.isJsonNull }?.asInt?.let { newDaily[lvl] = it }
                obj.get("bonusCoins")?.takeIf { !it.isJsonNull }?.asInt?.let { newBonus[lvl] = it }
                obj.get("durationDays")?.takeIf { !it.isJsonNull }?.asInt?.let { newDuration[lvl] = it }
                obj.get("name")?.takeIf { !it.isJsonNull }?.asString?.let { newName[lvl] = it }
                obj.get("price")?.takeIf { !it.isJsonNull }?.let { p ->
                    newPrice[lvl] = if (p.isJsonPrimitive && p.asJsonPrimitive.isNumber) {
                        val d = p.asDouble
                        if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()
                    } else p.asString
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("VipRuntimeConfig", "applyJson parse failed, keep old config", e)
            return  // 解析失败 → 保留原有 map，不触发重组
        }
        // 原子 commit
        synchronized(this) {
            dailyCoinsMap.clear(); dailyCoinsMap.putAll(newDaily)
            bonusCoinsMap.clear(); bonusCoinsMap.putAll(newBonus)
            durationDaysMap.clear(); durationDaysMap.putAll(newDuration)
            nameMap.clear(); nameMap.putAll(newName)
            priceMap.clear(); priceMap.putAll(newPrice)
        }
        version++  // 触发 Compose 重组
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
