package com.example.funlife.social

import android.content.Context
import android.util.Log
import com.example.funlife.FunLifeApplication
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.repository.SocialLinkRepository
import com.example.funlife.utils.UserSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

/**
 * 企业级社交操作门禁：所有需要 PocketBase 网络的路径统一经此层。
 *
 * - 按 userId 串行化凭证获取，避免并发 authRefresh 互相覆盖
 * - 本地 Token → ensureSession → 再取 Token
 * - 统一超时 / 异常 → [SocialFailure]
 */
object SocialOperationGate {

    private const val TAG = "SocialOpGate"
    const val TIMEOUT_BIND_MS = 20_000L
    const val TIMEOUT_SEARCH_MS = 25_000L
    const val TIMEOUT_MUTATION_MS = 30_000L
    const val TIMEOUT_SYNC_MS = 30_000L

    private fun bindTimeoutMs(): Long =
        if (PocketBaseConfig.isRemote()) 35_000L else TIMEOUT_BIND_MS

    private fun mutationTimeoutMs(): Long =
        if (PocketBaseConfig.isRemote()) 45_000L else TIMEOUT_MUTATION_MS

    private fun syncTimeoutMs(): Long =
        if (PocketBaseConfig.isRemote()) 45_000L else TIMEOUT_SYNC_MS

    private val userLocks = ConcurrentHashMap<Long, Mutex>()

    /** 规范化 FunLife 用户名（去 @、trim、小写用于比较） */
    fun normalizeUsername(raw: String): String =
        raw.trim().removePrefix("@").trim()

    fun validateSearchQuery(raw: String): Result<String> {
        val q = normalizeUsername(raw)
        return if (q.length < 2) {
            Result.failure(SocialFailureException(SocialFailure.Validation("用户名至少 2 个字符")))
        } else {
            Result.success(q)
        }
    }

    /**
     * 获取可用凭证；[forceSession] 为 true 时在本地 Token 失效时阻塞绑定。
     */
    suspend fun acquire(
        ctx: Context,
        userId: Long,
        forceSession: Boolean = true,
        bindTimeoutMs: Long = bindTimeoutMs(),
    ): Result<SocialCredentials> = withContext(Dispatchers.IO) {
        if (!PocketBaseConfig.isEnabled()) {
            return@withContext Result.failure(SocialFailureException(SocialFailure.NotConfigured))
        }
        if (userId <= 0L) {
            return@withContext Result.failure(SocialFailureException(SocialFailure.NotLoggedIn))
        }

        val sessionUserId = runCatching { UserSessionManager(ctx.applicationContext).getCurrentUserId() }
            .getOrDefault(0L)
        if (sessionUserId > 0L && sessionUserId != userId) {
            return@withContext Result.failure(
                SocialFailureException(SocialFailure.NotLoggedIn),
            )
        }

        lockFor(userId).withLock {
            val appCtx = ctx.applicationContext
            fromLocalSuspend(appCtx, userId)?.let { return@withLock Result.success(it) }

            if (!forceSession) {
                return@withLock Result.failure(
                    SocialFailureException(
                        SocialFailure.NotReady("社交账号未就绪，请稍后或下拉刷新"),
                    ),
                )
            }

            val linked = withTimeoutOrNull(bindTimeoutMs) {
                SocialSessionManager.ensureSession(appCtx, timeoutMs = bindTimeoutMs)
            } ?: false

            fromLocalSuspend(appCtx, userId)?.let { return@withLock Result.success(it) }

            val msg = if (linked) {
                "社交 Token 获取失败，请下拉刷新"
            } else {
                "社交账号绑定失败\n请确认 PocketBase 已启动且手机能访问服务器"
            }
            Log.w(TAG, "acquire failed userId=$userId linked=$linked")
            Result.failure(SocialFailureException(SocialFailure.NotReady(msg)))
        }
    }

    /**
     * 门禁 + 超时 + 统一异常映射。业务层只需写 `block(credentials)`。
     */
    suspend fun <T> run(
        ctx: Context,
        userId: Long,
        operation: String,
        timeoutMs: Long = mutationTimeoutMs(),
        forceSession: Boolean = true,
        block: suspend (SocialCredentials) -> Result<T>,
    ): Result<T> = withContext(Dispatchers.IO) {
        suspend fun executeWith(cred: SocialCredentials): Result<T> {
            return try {
                withTimeoutOrNull(timeoutMs) {
                    block(cred)
                } ?: Result.failure(
                    SocialFailureException(SocialFailure.Timeout(operation)),
                )
            } catch (e: SocialFailureException) {
                Result.failure(e)
            } catch (e: Throwable) {
                Result.failure(mapToSocialFailure(e))
            }
        }

        fun isUnauthorized(result: Result<T>): Boolean {
            val t = result.exceptionOrNull() ?: return false
            if (t is PocketBaseApiException && t.code == 401) return true
            if (t is SocialFailureException && t.failure is SocialFailure.Api) {
                val msg = t.failure.userMessage
                if (msg.contains("401") || msg.contains("Unauthorized", ignoreCase = true)) return true
            }
            return false
        }

        val credResult = acquire(ctx, userId, forceSession)
        var cred = credResult.getOrElse { return@withContext Result.failure(it) }

        var result = executeWith(cred)
        if (isUnauthorized(result)) {
            Log.w(TAG, "401 on $operation — refresh credentials userId=$userId")
            SocialTokenCache.clear(userId)
            cred = acquire(ctx, userId, forceSession = true).getOrElse { return@withContext Result.failure(it) }
            result = executeWith(cred)
        }
        result
    }

    suspend fun warmCredentials(ctx: Context, userId: Long) {
        runCatching { acquire(ctx, userId, forceSession = false) }
    }

    private suspend fun fromLocalSuspend(appCtx: Context, userId: Long): SocialCredentials? {
        val db = (appCtx as? FunLifeApplication)?.database ?: AppDatabase.getDatabase(appCtx)
        val link = db.socialDao().getLink(userId) ?: return null
        if (link.pbRecordId.isBlank()) return null
        SocialTokenCache.get(userId)?.let {
            return SocialCredentials(userId, link.pbRecordId, it)
        }
        val stored = SocialSecureStore.getToken(appCtx, userId)
        if (!stored.isNullOrBlank()) {
            SocialTokenCache.put(userId, stored)
            return SocialCredentials(userId, link.pbRecordId, stored)
        }
        return null
    }

    private fun lockFor(userId: Long): Mutex = userLocks.getOrPut(userId) { Mutex() }

    private fun mapToSocialFailure(t: Throwable): SocialFailureException =
        SocialFailureException(SocialFailure.fromThrowable(t))

    /** 供 SocialSessionManager / UI 判断凭证是否可用（不触发绑定）。 */
    suspend fun peek(ctx: Context, userId: Long): SocialCredentials? = withContext(Dispatchers.IO) {
        if (userId <= 0L || !PocketBaseConfig.isEnabled()) return@withContext null
        fromLocalSuspend(ctx.applicationContext, userId)
    }

    suspend fun currentUserId(ctx: Context): Long =
        runCatching { UserSessionManager(ctx).getCurrentUserId() }.getOrDefault(0L)
}
