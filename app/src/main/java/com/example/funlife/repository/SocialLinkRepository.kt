package com.example.funlife.repository

import android.content.Context
import android.net.Uri
import com.example.funlife.data.dao.SocialDao
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.SocialPocketBaseLink
import com.example.funlife.BuildConfig
import com.example.funlife.social.PocketBaseApiClient
import com.example.funlife.social.PocketBaseApiException
import com.example.funlife.social.PocketBaseConfig
import com.example.funlife.social.SocialSecureStore
import com.example.funlife.social.SocialTokenCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 本地 FunLife 账号 ↔ PocketBase 绑定（不替换 Room 登录）。
 */
class SocialLinkRepository(
    private val context: Context,
    private val socialDao: SocialDao,
) {
    private val api = PocketBaseApiClient(context.applicationContext)

    suspend fun getLink(userId: Long): SocialPocketBaseLink? =
        socialDao.getLink(userId)

    suspend fun ensureLinked(
        userId: Long,
        funlifeUsername: String,
        displayName: String,
    ): Result<SocialPocketBaseLink> = withContext(Dispatchers.IO) {
        if (!PocketBaseConfig.isEnabled()) {
            return@withContext Result.failure(IllegalStateException("PocketBase 未配置"))
        }
        try {
            socialDao.getLink(userId)?.let { existing ->
                refreshTokenIfNeeded(userId, existing.pbIdentity)?.let { token ->
                    SocialTokenCache.put(userId, token)
                    runCatching { api.updateOnline(token, true) }
                    // 头像同步放后台，避免阻塞好友页首屏
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        syncAvatarToPocketBase(userId, token, existing.pbRecordId)
                    }
                    return@withContext Result.success(existing)
                }
            }

            val identity = SocialSecureStore.syntheticIdentity(userId, funlifeUsername)
            val password = SocialSecureStore.getOrCreatePassword(context, userId)
            val token = SocialSecureStore.getToken(context, userId)

            val auth = if (!token.isNullOrBlank()) {
                runCatching { api.authRefresh(token) }
                    .getOrElse { api.authWithPassword(identity, password) }
            } else {
                runCatching { api.authWithPassword(identity, password) }
                    .getOrElse {
                        api.registerUser(identity, password, displayName, userId, funlifeUsername)
                    }
            }

            SocialSecureStore.saveToken(context, userId, auth.token)
            SocialTokenCache.put(userId, auth.token)
            val link = SocialPocketBaseLink(
                userId = userId,
                pbRecordId = auth.recordId,
                pbIdentity = identity,
                linkedAt = System.currentTimeMillis(),
            )
            socialDao.upsertLink(link)
            runCatching { api.updateOnline(auth.token, true) }
            syncAvatarToPocketBase(userId, auth.token, auth.recordId)
            Result.success(link)
        } catch (e: PocketBaseApiException) {
            Result.failure(Exception(e.message ?: "社交服务连接失败"))
        } catch (e: java.io.IOException) {
            Result.failure(
                Exception(
                    "无法连接社交服务器\n请确认 PocketBase 已启动，且手机与电脑在同一 WiFi\n(${PocketBaseConfig.baseUrl()})",
                ),
            )
        } catch (e: Exception) {
            val detail = if (BuildConfig.DEBUG) e.message else null
            Result.failure(
                Exception(
                    if (detail.isNullOrBlank()) "社交账号绑定失败，请稍后再试"
                    else "社交账号绑定失败：$detail",
                ),
            )
        }
    }

    suspend fun clearLocal(userId: Long) = withContext(Dispatchers.IO) {
        runCatching {
            val link = socialDao.getLink(userId)
            val token = SocialSecureStore.getToken(context, userId)
            if (link != null && token != null) {
                runCatching { api.updateOnline(token, false) }
            }
        }
        SocialSecureStore.clearUser(context, userId)
        SocialTokenCache.clear(userId)
        socialDao.deleteLink(userId)
        socialDao.clearFriends(userId)
    }

    private fun refreshTokenIfNeeded(userId: Long, identity: String): String? {
        SocialTokenCache.get(userId)?.let { return it }
        val token = SocialSecureStore.getToken(context, userId) ?: return null
        val refreshed = runCatching {
            val result = api.authRefresh(token)
            SocialSecureStore.saveToken(context, userId, result.token)
            result.token
        }.getOrElse {
            runCatching {
                val pwd = SocialSecureStore.getOrCreatePassword(context, userId)
                val auth = api.authWithPassword(identity, pwd)
                SocialSecureStore.saveToken(context, userId, auth.token)
                auth.token
            }.getOrNull()
        }
        if (refreshed != null) {
            SocialTokenCache.put(userId, refreshed)
        }
        return refreshed
    }

    suspend fun getValidToken(userId: Long): String? = withContext(Dispatchers.IO) {
        SocialTokenCache.get(userId)?.let { return@withContext it }
        val link = socialDao.getLink(userId) ?: return@withContext null
        runCatching { refreshTokenIfNeeded(userId, link.pbIdentity) }.getOrNull()
    }

    /** 进入好友页时预热 Token，避免首次搜索卡顿 */
    suspend fun warmToken(userId: Long) {
        getValidToken(userId)
    }

    /** 将 Room 本地头像同步到 PocketBase，供他人搜索时展示 */
    suspend fun syncAvatarToPocketBase(userId: Long) = withContext(Dispatchers.IO) {
        if (!PocketBaseConfig.isEnabled()) return@withContext
        val link = socialDao.getLink(userId) ?: return@withContext
        val token = refreshTokenIfNeeded(userId, link.pbIdentity) ?: return@withContext
        syncAvatarToPocketBase(userId, token, link.pbRecordId)
    }

    private suspend fun syncAvatarToPocketBase(userId: Long, token: String, pbRecordId: String) {
        runCatching {
            val db = AppDatabase.getDatabase(context.applicationContext)
            val avatarUri = db.userAvatarDao().getUserAvatar(userId).first()?.avatarUri
            if (avatarUri.isNullOrBlank()) return@runCatching
            if (avatarUri == SocialSecureStore.getLastSyncedAvatarUri(context, userId)) return@runCatching
            val file = resolveAvatarFile(context, avatarUri) ?: return@runCatching
            api.uploadUserAvatar(token, pbRecordId, file)
            SocialSecureStore.saveLastSyncedAvatarUri(context, userId, avatarUri)
        }
    }

    private fun resolveAvatarFile(context: Context, avatarUri: String): File? {
        return when {
            avatarUri.startsWith("file://") -> {
                val path = Uri.parse(avatarUri).path ?: return null
                File(path).takeIf { it.exists() && it.canRead() }
            }
            avatarUri.startsWith("/") -> File(avatarUri).takeIf { it.exists() && it.canRead() }
            else -> {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(avatarUri))?.use { input ->
                        val temp = File(context.cacheDir, "pb_avatar_upload_${System.currentTimeMillis()}.jpg")
                        temp.outputStream().use { output -> input.copyTo(output) }
                        temp
                    }
                }.getOrNull()
            }
        }
    }
}
