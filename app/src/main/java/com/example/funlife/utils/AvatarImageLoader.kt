package com.example.funlife.utils

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.imageLoader
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * 头像专用 Coil 配置：无 crossfade、稳定 cache key、prefetch 写入内存+磁盘。
 * 远程头像优先同步读内存/磁盘缓存，首帧直出，避免 loading 占位闪一下。
 */
object AvatarImageLoader {

    fun buildRequest(
        context: Context,
        url: String,
        pbAuthToken: String? = null,
    ): ImageRequest {
        val needsAuth = url.contains("/api/files/") && !pbAuthToken.isNullOrBlank()
        return ImageRequest.Builder(context)
            .data(url)
            .memoryCacheKey(url)
            .diskCacheKey(url)
            .crossfade(false)
            .apply {
                if (needsAuth) {
                    addHeader("Authorization", "Bearer $pbAuthToken")
                }
                listener(
                    onSuccess = { _, result ->
                        val loadable = url.trim()
                        (result.drawable as? BitmapDrawable)?.bitmap?.asImageBitmap()?.let {
                            UserAvatarBitmapCache.putBitmap(loadable, it)
                        }
                    },
                )
            }
            .build()
    }

    /** 仅读缓存，不触发网络；用于 compose 首帧同步解码。 */
    private fun buildCachedOnlyRequest(
        context: Context,
        url: String,
        pbAuthToken: String? = null,
    ): ImageRequest {
        val needsAuth = url.contains("/api/files/") && !pbAuthToken.isNullOrBlank()
        return ImageRequest.Builder(context)
            .data(url)
            .memoryCacheKey(url)
            .diskCacheKey(url)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.READ_ONLY)
            .diskCachePolicy(CachePolicy.READ_ONLY)
            .networkCachePolicy(CachePolicy.DISABLED)
            .apply {
                if (needsAuth) {
                    addHeader("Authorization", "Bearer $pbAuthToken")
                }
            }
            .build()
    }

    fun peekMemoryBitmap(context: Context, url: String): ImageBitmap? {
        val key = MemoryCache.Key(url)
        val bitmap = context.applicationContext.imageLoader.memoryCache?.get(key)?.bitmap ?: return null
        return bitmap.asImageBitmap()
    }

    /** 内存 → 磁盘同步读取；未命中返回 null，由调用方走异步加载。禁止在主线程调用。 */
    fun loadCachedBitmapBlocking(
        context: Context,
        url: String,
        pbAuthToken: String? = null,
    ): ImageBitmap? {
        UserAvatarBitmapCache.peekBitmap(url)?.let { return it }
        peekMemoryBitmap(context, url)?.let { bitmap ->
            UserAvatarBitmapCache.putBitmap(url, bitmap)
            return bitmap
        }
        if (Looper.myLooper() == Looper.getMainLooper()) return null
        val decoded = runBlocking(Dispatchers.IO) {
            val loader = context.applicationContext.imageLoader
            val request = buildCachedOnlyRequest(context.applicationContext, url, pbAuthToken)
            when (val result = loader.execute(request)) {
                is SuccessResult -> (result.drawable as? BitmapDrawable)?.bitmap?.asImageBitmap()
                else -> null
            }
        }
        if (decoded != null) {
            UserAvatarBitmapCache.putBitmap(url, decoded)
        }
        return decoded
    }

    suspend fun warm(
        context: Context,
        url: String?,
        pbAuthToken: String? = null,
        loader: ImageLoader = context.applicationContext.imageLoader,
    ) {
        val trimmed = url?.trim()?.takeIf { it.isNotEmpty() } ?: return
        if (AvatarStorageHelper.isLocalAvatarUri(trimmed)) return
        withContext(Dispatchers.IO) {
            runCatching {
                loader.execute(buildRequest(context.applicationContext, trimmed, pbAuthToken))
            }
        }
    }

    suspend fun warmAll(
        context: Context,
        urls: Iterable<String?>,
        pbAuthToken: String? = null,
    ) {
        withContext(Dispatchers.IO) {
            val loader = context.applicationContext.imageLoader
            urls.filterNotNull()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .forEach { url ->
                    if (AvatarStorageHelper.isLocalAvatarUri(url)) return@forEach
                    runCatching {
                        loader.execute(buildRequest(context.applicationContext, url, pbAuthToken))
                    }
                }
        }
    }

    fun prefetch(
        context: Context,
        url: String?,
        pbAuthToken: String? = null,
        loader: ImageLoader = context.applicationContext.imageLoader,
    ) {
        val trimmed = url?.trim()?.takeIf { it.isNotEmpty() } ?: return
        if (AvatarStorageHelper.isLocalAvatarUri(trimmed)) return
        loader.enqueue(buildRequest(context.applicationContext, trimmed, pbAuthToken))
    }

    fun prefetchAll(
        context: Context,
        urls: Iterable<String?>,
        pbAuthToken: String? = null,
    ) {
        urls.filterNotNull()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .forEach { prefetch(context, it, pbAuthToken) }
    }

    /** 仅读内存缓存（Compose 首帧安全，不做磁盘/网络 IO）。 */
    @Composable
    fun rememberCachedRemoteAvatarBitmap(
        avatarUrl: String?,
        pbAuthToken: String? = null,
    ): ImageBitmap? {
        val context = LocalContext.current
        val trimmed = avatarUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (AvatarStorageHelper.isLocalAvatarUri(trimmed)) return null
        return remember(trimmed, pbAuthToken) {
            UserAvatarBitmapCache.peekBitmap(trimmed)
                ?: peekMemoryBitmap(context, trimmed)?.also { UserAvatarBitmapCache.putBitmap(trimmed, it) }
        }
    }

    /** 仅读内存缓存中的本地头像（Compose 首帧安全）。 */
    @Composable
    fun rememberLocalAvatarBitmap(avatarUri: String?): ImageBitmap? {
        val context = LocalContext.current
        val loadable = remember(avatarUri) {
            AvatarStorageHelper.resolveLoadableAvatarUri(context, avatarUri)
        }
        return remember(loadable) {
            if (loadable == null || !AvatarStorageHelper.isLocalAvatarUri(loadable)) return@remember null
            UserAvatarBitmapCache.peekBitmap(loadable)
        }
    }

    /**
     * 异步解析头像：首帧读内存缓存，后台读磁盘/Coil 并写入 UserAvatarBitmapCache 后刷新 UI。
     */
    @Composable
    fun rememberAvatarBitmap(
        avatarUrl: String?,
        localAvatarUri: String? = null,
        pbAuthToken: String? = null,
    ): ImageBitmap? {
        val context = LocalContext.current
        val remote = avatarUrl?.trim()?.takeIf { it.isNotEmpty() }
            ?.takeUnless { AvatarStorageHelper.isLocalAvatarUri(it) }
        val local = remember(localAvatarUri, avatarUrl) {
            localAvatarUri?.takeIf { it.isNotBlank() }
                ?: avatarUrl?.takeIf { AvatarStorageHelper.isLocalAvatarUri(it) }
                    ?.let { AvatarStorageHelper.resolveLoadableAvatarUri(context, it) }
        }
        var bitmap by remember(remote, local, pbAuthToken) {
            mutableStateOf(peekInstantBitmap(context, remote, local))
        }
        LaunchedEffect(remote, local, pbAuthToken) {
            peekInstantBitmap(context, remote, local)?.let {
                bitmap = it
                return@LaunchedEffect
            }
            val loaded = withContext(Dispatchers.IO) {
                when {
                    local != null -> UserAvatarBitmapCache.rememberBitmap(context, local)
                    !remote.isNullOrBlank() -> {
                        loadCachedBitmapBlocking(context, remote, pbAuthToken)
                            ?: run {
                                warm(context, remote, pbAuthToken)
                                UserAvatarBitmapCache.peekBitmap(remote)
                                    ?: peekMemoryBitmap(context, remote)?.also {
                                        UserAvatarBitmapCache.putBitmap(remote, it)
                                    }
                            }
                    }
                    else -> null
                }
            }
            if (loaded != null) bitmap = loaded
        }
        return bitmap
    }

    private fun peekInstantBitmap(
        context: Context,
        remote: String?,
        local: String?,
    ): ImageBitmap? {
        local?.let { UserAvatarBitmapCache.peekBitmap(it) }?.let { return it }
        if (!remote.isNullOrBlank()) {
            UserAvatarBitmapCache.peekBitmap(remote)?.let { return it }
            peekMemoryBitmap(context, remote)?.let {
                UserAvatarBitmapCache.putBitmap(remote, it)
                return it
            }
        }
        return null
    }
}
