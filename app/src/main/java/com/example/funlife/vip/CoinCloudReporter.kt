package com.example.funlife.vip

import android.content.Context
import com.example.funlife.BuildConfig
import com.example.funlife.security.SecurityManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 金币变动云端上报（fire-and-forget，全异步、非阻塞）
 *
 * 用于：
 *  - 后台能看到全量用户金币余额和趋势
 *  - 服务端做异常检测（短时暴涨/单次入账过大）
 *  - 必要时后台一键扣金币 / 封号
 *
 * 网络异常一律静默，不影响用户使用。
 */
object CoinCloudReporter {

    private val gson = Gson()
    private val client = SecureHttp.newBuilder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .build()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * 上报一次金币变动。
     *
     * @param op   "earn" 或 "spend"
     * @param amount 本次变动数量（正整数）
     * @param reason 业务原因，如 "spin_reward"/"shop_buy"/"daily_claim"/"redeem_code"
     * @param balance 当前余额（变动后）
     */
    fun report(
        context: Context,
        username: String,
        op: String,
        amount: Int,
        reason: String,
        balance: Int,
        totalEarned: Int = 0,
        totalSpent: Int = 0,
        pointsBalance: Int? = null,
    ) {
        val baseUrl = BuildConfig.VIP_BACKEND_URL
        if (baseUrl.isNullOrBlank()) return
        if (username.isBlank() || amount <= 0) return
        scope.launch {
            try {
                val deviceToken = DeviceTokenStore(context).load(username)
                if (deviceToken.isNullOrBlank()) {
                    // 还没拿到 token（注册时 register_log 网络失败等），跳过本次上报
                    // 后续注册成功后会重新拿到，避免无 token 上报被服务端 401
                    return@launch
                }
                // 服务端签的 token 里 deviceId 只取了前 32 位，这里也只传 32 位避免身份不一致
                val deviceId = SecurityManager.getDeviceFingerprint(context).take(32)
                // 🔒 nonce + ts：服务端按 nonce 去重，防止网络层重放
                val nonce = java.util.UUID.randomUUID().toString().replace("-", "")
                val ts = System.currentTimeMillis()
                val body = buildMap<String, Any> {
                    put("username", username)
                    put("deviceId", deviceId)
                    put("deviceToken", deviceToken)
                    put("op", op)
                    put("amount", amount)
                    put("reason", reason)
                    put("balance", balance)
                    put("totalEarned", totalEarned)
                    put("totalSpent", totalSpent)
                    put("nonce", nonce)
                    put("ts", ts)
                    if (pointsBalance != null) put("pointsBalance", pointsBalance)
                }
                val req = Request.Builder()
                    .url(baseUrl.trimEnd('/') + "/coin_log")
                    .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(req).execute().use { resp ->
                    // 拿到 AUTH 错误 → token 已失效，清掉本地。下次登录会自动补领
                    if (resp.code in 200..299) {
                        try {
                            val text = resp.body?.string().orEmpty()
                            if (text.contains("\"AUTH_") || text.contains("AUTH_INVALID")) {
                                DeviceTokenStore(context).clear(username)
                                android.util.Log.w(
                                    "CoinCloudReporter",
                                    "device_token rejected by server, cleared. user=$username; please re-login to refresh"
                                )
                            }
                        } catch (e: Exception) { /* 静默 */ }
                    }
                }
            } catch (e: Exception) {
                // 静默
            }
        }
    }
}
