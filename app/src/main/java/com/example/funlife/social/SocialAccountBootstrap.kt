package com.example.funlife.social

import android.content.Context

/**
 * @deprecated 请使用 [SocialSessionManager]；保留此类仅为兼容旧调用点。
 */
object SocialAccountBootstrap {

    fun ensureReadyAsync(ctx: Context) = SocialSessionManager.warmStartAsync(ctx)

    suspend fun ensureReady(ctx: Context): Boolean = SocialSessionManager.ensureSession(ctx)

    suspend fun isAlreadyLinked(ctx: Context, userId: Long): Boolean =
        SocialSessionManager.isLinked(ctx, userId)
}
