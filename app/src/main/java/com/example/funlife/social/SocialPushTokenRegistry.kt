package com.example.funlife.social

import android.content.Context
import android.util.Log
import com.example.funlife.FunLifeApplication
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.repository.SocialLinkRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * FCM / 厂商推送 Token 注册（企业级杀进程推送的前置条件）。
 *
 * 接入 Firebase 后，在 MessagingService.onNewToken 中调用 [saveToken]，
 * 本类会在社交绑定成功后自动上传到 PocketBase users.fcm_token。
 */
object SocialPushTokenRegistry {

    private const val TAG = "SocialPushToken"
    private const val PREFS = "social_push_token"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun saveToken(ctx: Context, userId: Long, token: String) {
        if (userId <= 0L || token.isBlank()) return
        ctx.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(key(userId), token.trim())
            .apply()
        syncToServerAsync(ctx, userId)
    }

    fun getToken(ctx: Context, userId: Long): String? =
        ctx.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key(userId), null)
            ?.takeIf { it.isNotBlank() }

    fun clearToken(ctx: Context, userId: Long) {
        ctx.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(key(userId))
            .apply()
    }

    fun syncToServerAsync(ctx: Context, userId: Long) {
        if (!PocketBaseConfig.isEnabled() || userId <= 0L) return
        scope.launch {
            runCatching { syncToServer(ctx, userId) }
                .onFailure { Log.w(TAG, "upload failed: ${it.message}") }
        }
    }

    suspend fun syncToServer(ctx: Context, userId: Long) {
        val pushToken = getToken(ctx, userId) ?: return
        val appCtx = ctx.applicationContext
        if (!SocialSessionManager.isLinked(appCtx, userId)) return
        val db = (appCtx as? FunLifeApplication)?.database ?: AppDatabase.getDatabase(appCtx)
        val linkRepo = SocialLinkRepository(appCtx, db.socialDao())
        val link = db.socialDao().getLink(userId) ?: return
        val authToken = linkRepo.getValidToken(userId) ?: return
        PocketBaseApiClient(appCtx).updatePushToken(authToken, link.pbRecordId, pushToken)
        Log.d(TAG, "push token synced userId=$userId")
    }

    private fun key(userId: Long) = "fcm_$userId"
}
