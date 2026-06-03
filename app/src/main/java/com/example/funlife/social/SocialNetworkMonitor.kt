package com.example.funlife.social

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * 网络恢复时重启 Realtime 并强制同步（企业级断网重连）。
 */
object SocialNetworkMonitor {

    private const val TAG = "SocialNetwork"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var callback: ConnectivityManager.NetworkCallback? = null
    private var registered = false

    fun register(ctx: Context) {
        if (!PocketBaseConfig.isEnabled() || registered) return
        val appCtx = ctx.applicationContext
        mainHandler.post {
            if (registered) return@post
            val cm = appCtx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm == null) {
                Log.w(TAG, "ConnectivityManager unavailable")
                return@post
            }
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            val cb = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "network available")
                    SocialSessionManager.onNetworkRestored(appCtx)
                }
            }
            runCatching { cm.registerNetworkCallback(request, cb) }
                .onSuccess {
                    callback = cb
                    registered = true
                }
                .onFailure { Log.w(TAG, "register failed: ${it.message}") }
        }
    }

    fun unregister(ctx: Context) {
        if (!registered) return
        val appCtx = ctx.applicationContext
        mainHandler.post {
            if (!registered) return@post
            val cm = appCtx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            callback?.let { cb ->
                runCatching { cm?.unregisterNetworkCallback(cb) }
            }
            callback = null
            registered = false
        }
    }
}
