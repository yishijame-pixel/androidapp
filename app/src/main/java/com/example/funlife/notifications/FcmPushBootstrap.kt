package com.example.funlife.notifications

import android.content.Context
import android.util.Log
import com.example.funlife.BuildConfig
import com.example.funlife.social.SocialPushTokenRegistry
import com.example.funlife.utils.UserSessionManager
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 登录 / 冷启动时拉取 FCM Token 并上传 PocketBase。
 * 国内网络可能暂时连不上 Google（SERVICE_NOT_AVAILABLE），会自动重试。
 */
object FcmPushBootstrap {

    private const val TAG = "FcmPushBootstrap"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun initAsync(ctx: Context) {
        if (!BuildConfig.FCM_ENABLED) {
            Log.d(TAG, "FCM disabled (missing google-services.json)")
            return
        }
        scope.launch { fetchTokenWithRetry(ctx.applicationContext, attempt = 0) }
    }

    private suspend fun fetchTokenWithRetry(appCtx: Context, attempt: Int) {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    val err = task.exception?.message.orEmpty()
                    Log.w(TAG, "token fetch failed (attempt ${attempt + 1}): $err")
                    if (attempt < 4 && err.contains("SERVICE_NOT_AVAILABLE", ignoreCase = true)) {
                        scope.launch {
                            delay(3_000L * (attempt + 1))
                            fetchTokenWithRetry(appCtx, attempt + 1)
                        }
                    }
                    return@addOnCompleteListener
                }
                val token = task.result ?: return@addOnCompleteListener
                val userId = runCatching { UserSessionManager(appCtx).getCurrentUserId() }
                    .getOrDefault(0L)
                SocialPushTokenRegistry.saveToken(appCtx, userId, token)
                Log.i(TAG, "FCM token ready userId=$userId len=${token.length}")
            }
    }
}
