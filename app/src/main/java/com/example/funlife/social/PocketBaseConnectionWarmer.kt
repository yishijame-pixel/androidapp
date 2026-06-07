package com.example.funlife.social

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Request

/**
 * 预热 PocketBase TCP+TLS，避免首包多等一整次 RTT。
 * 不替代「服务器就近部署」，但可让第 2 次起的 HTTP 复用连接（~0ms 握手）。
 */
object PocketBaseConnectionWarmer {

    private const val TAG = "PbConnWarm"
    private const val WARM_TTL_MS = 45_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var warmedAtMs = 0L

    fun warmAsync(context: Context) {
        if (!PocketBaseConfig.isEnabled()) return
        val now = System.currentTimeMillis()
        if (now - warmedAtMs < WARM_TTL_MS) return
        scope.launch {
            runCatching {
                val url = "${PocketBaseConfig.apiBase()}/health"
                val request = Request.Builder().url(url).get().build()
                PocketBaseHttp.client().newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "warm health ${response.code}")
                    }
                }
                warmedAtMs = System.currentTimeMillis()
                Log.d(TAG, "connection warmed")
            }.onFailure { Log.w(TAG, "warm failed: ${it.message}") }
        }
    }
}
