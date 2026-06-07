package com.example.funlife.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.funlife.FunLifeApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * 用户头像/头像框展示缓存（内存 + SharedPreferences）。
 * Room Flow 首帧常为 null；进程重启后内存也空。磁盘快照 + 同步 hydrate 保证首帧直出。
 */
object UserAvatarBitmapCache {

    private const val PREFS_NAME = "user_display_snapshot"

    @Volatile
    private var appContext: Context? = null

    private val uriByUserId = ConcurrentHashMap<Long, String>()
    private val frameByUserId = ConcurrentHashMap<Long, String>()

    private val maxBytes: Int by lazy {
        val runtimeMax = (Runtime.getRuntime().maxMemory() / 16).toInt()
        runtimeMax.coerceAtMost(24 * 1024 * 1024).coerceAtLeast(8 * 1024 * 1024)
    }

    private val bitmapByLoadableUri = object : android.util.LruCache<String, ImageBitmap>(maxBytes) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    fun install(context: Context) {
        appContext = context.applicationContext
    }

    private fun prefs(): android.content.SharedPreferences {
        val ctx = appContext ?: error("UserAvatarBitmapCache.install() must be called in Application.onCreate")
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun avatarKey(userId: Long) = "avatar_uri_$userId"
    private fun frameKey(userId: Long) = "avatar_frame_$userId"

    fun peekUri(userId: Long): String? {
        if (userId <= 0L) return null
        uriByUserId[userId]?.let { return it }
        val fromDisk = prefs().getString(avatarKey(userId), null)?.trim()?.takeIf { it.isNotEmpty() }
        if (fromDisk != null) uriByUserId[userId] = fromDisk
        return fromDisk
    }

    fun peekFrame(userId: Long): String? {
        if (userId <= 0L) return null
        frameByUserId[userId]?.let { return it }
        val fromDisk = prefs().getString(frameKey(userId), null)?.trim()?.takeIf { it.isNotEmpty() }
        if (fromDisk != null) frameByUserId[userId] = fromDisk
        return fromDisk
    }

    fun peekBitmap(loadableUri: String?): ImageBitmap? {
        val key = loadableUri?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val cached = bitmapByLoadableUri.get(key) ?: return null
        return if (cached.isUsable()) cached else {
            bitmapByLoadableUri.remove(key)
            null
        }
    }

    private fun ImageBitmap.isUsable(): Boolean = runCatching {
        width > 0 && height > 0 && !asAndroidBitmap().isRecycled
    }.getOrDefault(false)

    fun putBitmap(loadableUri: String, bitmap: ImageBitmap) {
        val key = loadableUri.trim()
        if (key.isEmpty()) return
        bitmapByLoadableUri.put(key, bitmap)
    }

    fun publishUri(userId: Long, avatarUri: String?) {
        if (userId <= 0L) return
        val trimmed = avatarUri?.trim()?.takeIf { it.isNotEmpty() }
        if (trimmed == null) {
            uriByUserId.remove(userId)
            prefs().edit().remove(avatarKey(userId)).apply()
            return
        }
        uriByUserId[userId] = trimmed
        prefs().edit().putString(avatarKey(userId), trimmed).apply()
    }

    fun publishFrame(userId: Long, frameAssetPath: String?) {
        if (userId <= 0L) return
        val trimmed = frameAssetPath?.trim()?.takeIf { it.isNotEmpty() }
        if (trimmed == null) {
            frameByUserId.remove(userId)
            prefs().edit().remove(frameKey(userId)).apply()
            return
        }
        frameByUserId[userId] = trimmed
        prefs().edit().putString(frameKey(userId), trimmed).apply()
    }

    fun clearFrame(userId: Long) {
        publishFrame(userId, null)
    }

    fun clearUser(userId: Long) {
        if (userId <= 0L) return
        uriByUserId.remove(userId)
        frameByUserId.remove(userId)
        prefs().edit()
            .remove(avatarKey(userId))
            .remove(frameKey(userId))
            .apply()
    }

    /**
     * 首页 compose 前同步：读磁盘快照 + 解码头像/框 PNG（仅 IO，小图可接受）。
     */
    fun hydrateUserSync(context: Context, userId: Long) {
        if (userId <= 0L) return
        if (appContext == null) install(context)

        peekUri(userId)
        peekFrame(userId)

        runBlocking(Dispatchers.IO) {
            val app = context.applicationContext
            val db = (app as? FunLifeApplication)?.database
            if (db != null) {
                if (peekUri(userId) == null) {
                    db.userAvatarDao().getUserAvatar(userId).first()?.avatarUri?.let { publishUri(userId, it) }
                }
                if (peekFrame(userId) == null) {
                    db.userPreferencesDao().getPreferencesSync(userId)?.equippedAvatarFrame?.let { publishFrame(userId, it) }
                }
            }
            peekUri(userId)?.let { uri ->
                rememberBitmap(app, uri)
            }
            peekFrame(userId)?.let { frame ->
                runCatching {
                    com.example.funlife.ui.components.warmAvatarFrameAsset(app, frame)
                }
            }
        }
    }

    fun rememberBitmap(context: Context, avatarUri: String?): ImageBitmap? {
        val loadable = AvatarStorageHelper.resolveLoadableAvatarUri(context, avatarUri) ?: return null
        peekBitmap(loadable)?.let { return it }
        val decoded = decodeLoadableUri(context, loadable) ?: return null
        putBitmap(loadable, decoded)
        return decoded
    }

    suspend fun warmUser(context: Context, userId: Long) {
        if (userId <= 0L) return
        if (appContext == null) install(context)
        withContext(Dispatchers.IO) {
            val app = context.applicationContext
            val db = (app as? FunLifeApplication)?.database ?: return@withContext

            db.userPreferencesDao().getPreferencesSync(userId)?.equippedAvatarFrame?.let { framePath ->
                publishFrame(userId, framePath)
                com.example.funlife.ui.components.warmAvatarFrameAsset(app, framePath)
            }

            val uri = db.userAvatarDao().getUserAvatar(userId).first()?.avatarUri
            if (uri != null) {
                publishUri(userId, uri)
                rememberBitmap(app, uri)
                AvatarImageLoader.warm(app, AvatarStorageHelper.resolveLoadableAvatarUri(app, uri))
            }
        }
    }

    private fun decodeLoadableUri(context: Context, loadableUri: String): ImageBitmap? {
        if (AvatarStorageHelper.isLocalAvatarUri(loadableUri)) {
            return runCatching {
                BitmapFactory.decodeFile(Uri.parse(loadableUri).path)?.asImageBitmap()
            }.getOrNull()
        }
        return AvatarImageLoader.loadCachedBitmapBlocking(context, loadableUri)
    }

    fun trim() {
        bitmapByLoadableUri.trimToSize(bitmapByLoadableUri.size() / 2)
    }

    fun clear() {
        bitmapByLoadableUri.evictAll()
        uriByUserId.clear()
        frameByUserId.clear()
        runCatching { prefs().edit().clear().apply() }
    }
}
