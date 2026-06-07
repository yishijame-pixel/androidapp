package com.example.funlife.social.game

import android.content.Context
import android.util.Log
import com.example.funlife.FunLifeApplication
import com.example.funlife.repository.SocialLinkRepository
import com.example.funlife.social.PocketBaseApiClient
import com.example.funlife.social.SocialCredentials
import com.example.funlife.social.SocialFailure
import com.example.funlife.social.SocialFailureException
import com.example.funlife.social.SocialOperationGate
import com.example.funlife.social.SocialSecureStore
import com.example.funlife.social.SocialSessionManager
import com.example.funlife.social.SocialTokenCache
import com.example.funlife.utils.UserSessionManager

/**
 * 对局页凭证门禁：自动绑定 / 刷新 Token，不让用户手动去好友页刷新。
 */
object GamePlayCredentialGate {

    private const val TAG = "GamePlayCred"

    suspend fun ensure(ctx: Context, userId: Long): Result<SocialCredentials> {
        if (userId <= 0L) {
            return Result.failure(SocialFailureException(SocialFailure.NotLoggedIn))
        }
        val appCtx = ctx.applicationContext
        SocialSessionManager.ensureSession(appCtx, scheduleRetryOnFailure = false)
        rebindIfNeeded(appCtx, userId)
        val acquired = SocialOperationGate.acquire(appCtx, userId, forceSession = true)
            .getOrElse { return Result.failure(it) }
        val refreshed = refreshToken(appCtx, userId, acquired)
        return validate(refreshed)
    }

    fun authIdFrom(cred: SocialCredentials): String =
        runCatching { PocketBaseApiClient.recordIdFromToken(cred.token) }
            .getOrDefault("")
            .ifBlank { cred.pbRecordId }

    fun isRecoverableError(t: Throwable): Boolean {
        val root = generateSequence(t) { it.cause }.last()
        val msg = root.message.orEmpty()
        return msg.contains("Cannot be blank", ignoreCase = true) ||
            msg.contains("社交", ignoreCase = true) ||
            msg.contains("Token", ignoreCase = true) ||
            msg.contains("401", ignoreCase = true) ||
            msg.contains("Unauthorized", ignoreCase = true) ||
            (root is com.example.funlife.social.PocketBaseApiException &&
                root.code in setOf(401, 403, 0))
    }

    suspend fun invalidateAndRebind(ctx: Context, userId: Long) {
        Log.w(TAG, "rebind credentials userId=$userId")
        SocialOperationGate.invalidateUserVerification(userId)
        SocialTokenCache.clear(userId)
        val appCtx = ctx.applicationContext
        rebindIfNeeded(appCtx, userId)
        SocialSessionManager.ensureSession(appCtx, scheduleRetryOnFailure = false)
    }

    fun validate(cred: SocialCredentials): Result<SocialCredentials> {
        if (cred.token.isBlank()) {
            return Result.failure(
                SocialFailureException(SocialFailure.NotReady("正在连接社交服务…")),
            )
        }
        val authId = authIdFrom(cred)
        if (authId.isBlank()) {
            return Result.failure(
                SocialFailureException(SocialFailure.NotReady("正在连接社交服务…")),
            )
        }
        return Result.success(
            if (authId == cred.pbRecordId) cred else SocialCredentials(cred.userId, authId, cred.token),
        )
    }

    private suspend fun rebindIfNeeded(appCtx: Context, userId: Long) {
        val session = UserSessionManager(appCtx).getSession() ?: return
        if (session.userId != userId) return
        val db = (appCtx as? FunLifeApplication)?.database
            ?: com.example.funlife.data.database.AppDatabase.getDatabase(appCtx)
        val linkRepo = SocialLinkRepository(appCtx, db.socialDao())
        val displayName = session.nickname.ifBlank { session.username }
        if (displayName.isBlank()) return
        runCatching {
            linkRepo.ensureLinked(userId, session.username, displayName)
        }.onFailure { Log.w(TAG, "ensureLinked failed: ${it.message}") }
    }

    private suspend fun refreshToken(
        appCtx: Context,
        userId: Long,
        cred: SocialCredentials,
    ): SocialCredentials {
        val token = runCatching {
            val api = PocketBaseApiClient(appCtx)
            val refreshed = api.authRefresh(cred.token)
            SocialSecureStore.saveToken(appCtx, userId, refreshed.token)
            SocialTokenCache.put(userId, refreshed.token)
            refreshed.token
        }.getOrDefault(cred.token)
        val authId = runCatching { PocketBaseApiClient.recordIdFromToken(token) }
            .getOrDefault(cred.pbRecordId)
        return SocialCredentials(userId, authId, token)
    }
}
