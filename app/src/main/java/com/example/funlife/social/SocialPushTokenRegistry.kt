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
 * FCM Token 注册：本地缓存 + 上传 PocketBase users.fcm_token。
 */
object SocialPushTokenRegistry {

    private const val TAG = "SocialPushToken"
    private const val PREFS = "social_push_token"
    private const val PENDING_KEY = "fcm_pending"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun saveToken(ctx: Context, userId: Long, token: String) {
        if (token.isBlank()) return
        val trimmed = token.trim()
        val appCtx = ctx.applicationContext
        val prefs = appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(PENDING_KEY, trimmed).apply()
        if (userId > 0L) {
            prefs.edit().putString(key(userId), trimmed).apply()
            syncToServerAsync(appCtx, userId)
        } else {
            Log.d(TAG, "token cached pending upload (no userId yet)")
        }
    }

    fun getToken(ctx: Context, userId: Long): String? {
        val appCtx = ctx.applicationContext
        val prefs = appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(key(userId), null)
            ?.takeIf { it.isNotBlank() }
            ?: prefs.getString(PENDING_KEY, null)?.takeIf { it.isNotBlank() }
    }

    fun clearToken(ctx: Context, userId: Long) {
        appCtx(ctx).getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(key(userId))
            .apply()
    }

    fun syncToServerAsync(ctx: Context, userId: Long) {
        if (!PocketBaseConfig.isEnabled() || userId <= 0L) return
        scope.launch {
            runCatching { syncToServer(ctx, userId) }
                .onFailure { Log.w(TAG, "upload failed userId=$userId: ${it.message}") }
        }
    }

    suspend fun syncToServer(ctx: Context, userId: Long) {
        val appCtx = ctx.applicationContext
        val pushToken = getToken(appCtx, userId) ?: run {
            Log.d(TAG, "no local token to upload userId=$userId")
            return
        }
        if (!SocialSessionManager.isLinked(appCtx, userId)) {
            Log.d(TAG, "skip upload: not linked userId=$userId")
            return
        }
        val db = (appCtx as? FunLifeApplication)?.database ?: AppDatabase.getDatabase(appCtx)
        val linkRepo = SocialLinkRepository(appCtx, db.socialDao())
        val link = db.socialDao().getLink(userId) ?: return
        val authToken = linkRepo.getValidToken(userId) ?: return
        PocketBaseApiClient(appCtx).updatePushToken(authToken, link.pbRecordId, pushToken)
        appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(key(userId), pushToken)
            .remove(PENDING_KEY)
            .apply()
        Log.i(TAG, "push token synced userId=$userId pb=${link.pbRecordId}")
    }

    private fun key(userId: Long) = "fcm_$userId"

    private fun appCtx(ctx: Context) = ctx.applicationContext
}
