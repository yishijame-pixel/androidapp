package com.example.funlife.resource

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.example.funlife.BuildConfig
import com.example.funlife.vip.SecureHttp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * 云端资源按需下载 + 本地缓存。
 * 解析顺序：resource_cache → assets（兼容未剥离 debug 包）→ 后台拉包。
 */
object ResourceStore {

    private const val TAG = "ResourceStore"
    private const val PREFS = "resource_store"
    private const val KEY_MANIFEST_VERSION = "manifest_version"
    private const val KEY_MANIFEST_CACHE_JSON = "manifest_cache_json"
    private const val KEY_MANIFEST_CACHE_AT = "manifest_cache_at_ms"
    private const val KEY_BUNDLE_SHA256_PREFIX = "bundle_sha256_"
    private const val KEY_BUNDLE_LOCAL_VERSION_PREFIX = "bundle_local_version_"
    /** 本地 manifest 缓存 TTL：同版本下跳过网络拉取 */
    private const val MANIFEST_CACHE_TTL_MS = 15 * 60 * 1000L
    /** bundle 就绪检查结果 TTL */
    private const val BUNDLE_READY_TTL_MS = 5 * 60 * 1000L

    /** 与 COS 上已发布的 pac_maze_skins.zip 保持一致；v22 起默认 sprite sheet */
    const val PAC_MAZE_SKINS_BUNDLE_VERSION = 25

    /** 无 manifest 时的兜底最低版本 */
    private const val PAC_MAZE_SKINS_MIN_BUNDLE_VERSION = 14

    /** 每组任一 path 存在即视为 bundle 完整（兼容 sheet / 逐帧 PNG 迁移期） */
    private val PAC_MAZE_SKINS_MARKERS: List<List<String>> = listOf(
        listOf("pac_maze_skins/bundle_version.txt"),
        listOf(
            "pac_maze_skins/food_chick_walker_pro_max/walk/walk_sheet.webp",
            "pac_maze_skins/food_chick_walker_pro_max/walk/walk_1.png",
        ),
        listOf(
            "pac_maze_skins/food_chick_walker_pro_max/idle/idle_sheet.webp",
            "pac_maze_skins/food_chick_walker_pro_max/idle/idle_1.png",
        ),
        listOf(
            "pac_maze_skins/food_chick_walker_pro_max/die/die_sheet.webp",
            "pac_maze_skins/food_chick_walker_pro_max/die/die_1.png",
        ),
        listOf(
            "pac_maze_skins/food_chick_walker_pro_max/jump/jump_sheet.webp",
            "pac_maze_skins/food_chick_walker_pro_max/jump/jump_1.png",
        ),
        listOf("pac_maze_skins/xia_walk/walk/walk_sheet.webp", "pac_maze_skins/xia_walk/walk/walk_1.png"),
        listOf("pac_maze_skins/laoshu_walk/walk/walk_sheet.webp", "pac_maze_skins/laoshu_walk/walk/walk_1.png"),
        listOf("pac_maze_skins/qinting_walk/walk/walk_sheet.webp", "pac_maze_skins/qinting_walk/walk/walk_1.png"),
        listOf("pac_maze_skins/wenzi_walk/walk/walk_sheet.webp", "pac_maze_skins/wenzi_walk/walk/walk_1.png"),
        listOf("pac_maze_skins/toushi_walk/walk/walk_sheet.webp", "pac_maze_skins/toushi_walk/walk/walk_1.png"),
        listOf("pac_maze_skins/fire_long_walk/walk/walk_sheet.webp", "pac_maze_skins/fire_long_walk/walk/walk_1.png"),
        listOf("pac_maze_skins/green_long_walk/walk/walk_sheet.webp", "pac_maze_skins/green_long_walk/walk/walk_1.png"),
        listOf("pac_maze_skins/haimian_walk/walk/walk_sheet.webp", "pac_maze_skins/haimian_walk/walk/walk_1.png"),
        listOf("pac_maze_skins/ice_long_walk/walk/walk_sheet.webp", "pac_maze_skins/ice_long_walk/walk/walk_1.png"),
        listOf("pac_maze_skins/long_walk/walk/walk_sheet.webp", "pac_maze_skins/long_walk/walk/walk_1.png"),
        listOf("pac_maze_skins/magic_dog_walk/walk/walk_sheet.webp", "pac_maze_skins/magic_dog_walk/walk/walk_1.png"),
        listOf("pac_maze_skins/paidaxin_walk/walk/walk_sheet.webp", "pac_maze_skins/paidaxin_walk/walk/walk_1.png"),
        listOf("pac_maze_skins/qishi_dog_walk/walk/walk_sheet.webp", "pac_maze_skins/qishi_dog_walk/walk/walk_1.png"),
        listOf("pac_maze_skins/bl_long_walk/walk/walk_sheet.webp", "pac_maze_skins/bl_long_walk/walk/walk_1.png"),
    )

    private const val PLATFORMER_CHARACTERS_BUNDLE_VERSION = 2
    private const val PLATFORMER_SFX_BUNDLE_VERSION = 1
    private const val PLATFORMER_SUPERTUX_BUNDLE_VERSION = 5

    private val PLATFORMER_SFX_MARKERS: List<List<String>> = listOf(
        listOf("platformer_sfx/bundle_version.txt"),
        listOf("platformer_sfx/sfx_manifest.json"),
        listOf("platformer_sfx/curated/platformer/jump.wav"),
    )

    private val PLATFORMER_SUPERTUX_MARKERS: List<List<String>> = listOf(
        listOf("platformer_supertux/bundle_version.txt"),
        listOf("platformer_supertux/content_catalog.json"),
        listOf("platformer_supertux/tilesets/antarctic/tileset_manifest.json"),
        listOf("platformer_supertux/levels/level_901/level.json"),
        listOf("platformer_supertux/levels/level_931/level.json"),
        listOf("platformer_supertux/levels/level_1018/level.json"),
    )

    private val PLATFORMER_CHARACTERS_MARKERS: List<List<String>> = listOf(
        listOf("platformer_characters/bundle_version.txt"),
        listOf("platformer_characters/content_catalog.json"),
        listOf(
            "platformer_characters/characters/temple_runner/run/run_sheet.webp",
            "platformer_characters/characters/temple_runner/run/run_1.png",
        ),
        listOf(
            "platformer_characters/characters/adventure_girl/run/run_sheet.webp",
            "platformer_characters/characters/adventure_girl/run/run_1.png",
        ),
        listOf(
            "platformer_characters/enemies/zombie_male/walk/walk_sheet.webp",
            "platformer_characters/enemies/zombie_male/walk/walk_1.png",
        ),
    )

    private val CLOUD_ROOTS = setOf(
        "xiangkuang", "pet", "login", "renge", "dibu", "wheel",
        "pac_maze_sfx", "pac_maze_skins", "platformer_characters",
        "platformer_sfx", "platformer_supertux",
    )

    private lateinit var appContext: Context
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bundleMutex = ConcurrentHashMap<String, Mutex>()
    @Volatile private var lastManifestVersion = 0
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private data class BundleReadySnapshot(
        val ready: Boolean,
        val checkedAtMs: Long,
        val versionKey: String?,
    )

    private val bundleReadyMemo = ConcurrentHashMap<String, BundleReadySnapshot>()
    private val resourceExistsMemo = ConcurrentHashMap<String, Boolean>()
    @Volatile private var cachedManifest: AssetManifestResponse? = null
    @Volatile private var cachedManifestAtMs: Long = 0L

    private val _activeDownload = MutableStateFlow<ActiveBundleDownloadProgress?>(null)
    val activeDownload: StateFlow<ActiveBundleDownloadProgress?> = _activeDownload.asStateFlow()

    /** 仅用户可见的全局下载会话才写入 [activeDownload]，避免静默 ensureBundle 误触横幅。 */
    @Volatile private var globalDownloadUiActive: Boolean = false

    fun beginGlobalDownloadUi() {
        globalDownloadUiActive = true
    }

    fun endGlobalDownloadUi() {
        globalDownloadUiActive = false
        _activeDownload.value = null
    }

    fun clearActiveDownloadProgress() {
        _activeDownload.value = null
    }

    private val apiClient: OkHttpClient by lazy {
        SecureHttp.newBuilder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val downloadClient: OkHttpClient by lazy {
        SecureHttp.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        purgeIncompletePlatformerSuperTuxCache()
    }

    private fun prefs() =
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 供 [PacMazeResourceUpdateNotifier] 读写「已读 manifest 版本」等元数据。 */
    fun prefsAccessor() = prefs()

    fun lastFetchedManifestVersion(): Int = lastManifestVersion

    fun isPacMazeBundleReady(bundleId: String): Boolean = isBundleReady(bundleId)

    fun isPlatformerSfxBundleReady(): Boolean = isBundleReady("platformer_sfx")

    fun isPlatformerSuperTuxBundleReady(): Boolean = isBundleReady("platformer_supertux")

    /** IO 线程调用，避免主线程 stat 风暴。 */
    suspend fun isBundleReadyAsync(bundleId: String): Boolean = withContext(Dispatchers.IO) {
        isBundleReady(bundleId)
    }

    fun invalidateBundleReadyMemo(bundleId: String? = null) {
        if (bundleId == null) {
            bundleReadyMemo.clear()
            resourceExistsMemo.clear()
        } else {
            bundleReadyMemo.remove(bundleId)
            resourceExistsMemo.keys.filter { it.startsWith("$bundleId/") }.forEach { resourceExistsMemo.remove(it) }
        }
    }

    fun invalidateManifestCache() {
        cachedManifest = null
        cachedManifestAtMs = 0L
        prefs().edit()
            .remove(KEY_MANIFEST_CACHE_JSON)
            .remove(KEY_MANIFEST_CACHE_AT)
            .apply()
    }

    private fun bundleVersionKey(targetDir: String): String? = when (targetDir) {
        "pac_maze_skins" -> readPacMazeSkinsBundleVersion()?.toString()
        "platformer_characters" -> readPlatformerCharactersBundleVersion()?.toString()
        "platformer_sfx" -> readPlatformerSfxBundleVersion()?.toString()
        "platformer_supertux" -> readPlatformerSuperTuxBundleVersion()?.toString()
        else -> null
    }

    fun readPacMazeSkinsBundleVersion(): Int? =
        resolveFile("pac_maze_skins/bundle_version.txt")?.readText()?.trim()?.toIntOrNull()

    /** 磁盘解码缓存目录 tag，随实际 bundle 版本变化 */
    fun pacMazeSkinsDecodeTag(): String =
        "norm_bv${readPacMazeSkinsBundleVersion() ?: PAC_MAZE_SKINS_BUNDLE_VERSION}"

    fun readPlatformerCharactersBundleVersion(): Int? =
        resolveFile("platformer_characters/bundle_version.txt")?.readText()?.trim()?.toIntOrNull()

    fun readPlatformerSfxBundleVersion(): Int? =
        resolveFile("platformer_sfx/bundle_version.txt")?.readText()?.trim()?.toIntOrNull()

    fun readPlatformerSuperTuxBundleVersion(): Int? =
        resolveFile("platformer_supertux/bundle_version.txt")?.readText()?.trim()?.toIntOrNull()

    /** manifest 声明的最低 bundle 版本；无 manifest 时回退 APK 内常量 */
    fun requiredBundleVersion(bundleId: String): Int? = requiredBundleVersion(bundleId, cachedManifest)

    private fun requiredBundleVersion(bundleId: String, manifest: AssetManifestResponse?): Int? {
        val fromManifest = manifest?.bundles?.firstOrNull { it.id == bundleId }?.bundleVersion
        if (fromManifest != null && fromManifest > 0) return fromManifest
        return when (bundleId) {
            "pac_maze_skins" -> PAC_MAZE_SKINS_BUNDLE_VERSION
            "platformer_characters" -> PLATFORMER_CHARACTERS_BUNDLE_VERSION
            "platformer_sfx" -> PLATFORMER_SFX_BUNDLE_VERSION
            "platformer_supertux" -> PLATFORMER_SUPERTUX_BUNDLE_VERSION
            else -> null
        }
    }

    private fun readLocalBundleVersion(bundleId: String): Int? = when (bundleId) {
        "pac_maze_skins" -> readPacMazeSkinsBundleVersion()
        "platformer_characters" -> readPlatformerCharactersBundleVersion()
        "platformer_sfx" -> readPlatformerSfxBundleVersion()
        "platformer_supertux" -> readPlatformerSuperTuxBundleVersion()
        else -> null
    }

    /** 磁盘 bundle_version.txt 或上次同步写入 prefs 的版本号。 */
    private fun readEffectiveBundleVersion(bundleId: String): Int? =
        readLocalBundleVersion(bundleId)
            ?: prefs().getInt("$KEY_BUNDLE_LOCAL_VERSION_PREFIX$bundleId", 0).takeIf { it > 0 }

    private fun bundleInfoFor(manifest: AssetManifestResponse?, bundleId: String): AssetBundleInfo? =
        manifest?.bundles?.firstOrNull { it.id == bundleId }

    /** 本地包是否满足 manifest 要求（版本 + sha256） */
    private fun isBundleContentCurrent(bundleId: String, manifest: AssetManifestResponse?): Boolean {
        if (!isBundleReady(bundleId)) return false
        val info = bundleInfoFor(manifest, bundleId) ?: return true
        val required = info.bundleVersion
        if (required != null && required > 0) {
            val local = readEffectiveBundleVersion(bundleId)
            if (local == null) {
                // pac_maze_sfx 等无 bundle_version.txt：内容 marker 已通过即视为当前，并写入同步戳
                markBundleSynced(
                    bundleId = bundleId,
                    manifestVersion = manifest?.version ?: lastManifestVersion,
                    sha256 = info.sha256,
                    bundleVersion = required,
                )
                return true
            }
            if (local < required) return false
        }
        val expectedSha = info.sha256?.trim()?.takeIf { it.isNotEmpty() }
        if (expectedSha != null) {
            val syncedSha = prefs().getString("$KEY_BUNDLE_SHA256_PREFIX$bundleId", null)
            if (syncedSha == null) {
                markBundleSynced(
                    bundleId = bundleId,
                    manifestVersion = manifest?.version ?: lastManifestVersion,
                    sha256 = expectedSha,
                    bundleVersion = info.bundleVersion,
                )
                return true
            }
            if (!syncedSha.equals(expectedSha, ignoreCase = true)) return false
        }
        return true
    }

    /** 横版 catalog 序列帧磁盘缓存根目录 */
    fun decodedPlatformerCacheRoot(): File =
        File(appContext.filesDir, "resource_cache/decoded_platformer_characters").also { it.mkdirs() }

    fun platformerDecodeTag(): String =
        "bv${readPlatformerCharactersBundleVersion() ?: PLATFORMER_CHARACTERS_BUNDLE_VERSION}"

    private const val KEY_PLATFORMER_DECODE_STAMP = "platformer_decode_stamp_v1"

    /** 当前资源版本指纹：skins + platformer bundle 版本变化时需重新解码。 */
    fun platformerDecodeStamp(): String {
        val skins = readPacMazeSkinsBundleVersion() ?: PAC_MAZE_SKINS_BUNDLE_VERSION
        val platformer = readPlatformerCharactersBundleVersion() ?: PLATFORMER_CHARACTERS_BUNDLE_VERSION
        return "s$skins:p$platformer"
    }

    fun isPlatformerDecodeStampCurrent(): Boolean =
        prefs().getString(KEY_PLATFORMER_DECODE_STAMP, null) == platformerDecodeStamp()

    fun markPlatformerDecodeStampComplete() {
        prefs().edit().putString(KEY_PLATFORMER_DECODE_STAMP, platformerDecodeStamp()).apply()
    }

    /** @deprecated 供 [PlatformerDecodeStampStore] 同步旧 stamp 键 */
    internal fun markPlatformerDecodeStampCompleteLegacy() = markPlatformerDecodeStampComplete()

    fun clearPlatformerDecodeStamp() {
        prefs().edit().remove(KEY_PLATFORMER_DECODE_STAMP).apply()
    }

    fun invalidatePlatformerDecodedCache() {
        decodedPlatformerCacheRoot().deleteRecursively()
        clearPlatformerDecodeStamp()
        Log.i(TAG, "invalidated platformer decoded anim cache")
    }

    /** 本地校验各核心游戏包是否已解压就绪（不拉 manifest）。 */
    fun localGameResourceStatus(): GameResourceLocalStatus {
        val ready = GameResourceBundles.gameBootOrder.filter(::isBundleReady)
        val pending = GameResourceBundles.gameBootOrder.filterNot(::isBundleReady)
        return GameResourceLocalStatus(
            readyBundleIds = ready,
            pendingBundleIds = pending,
        )
    }

    private fun cacheRoot(): File =
        File(appContext.filesDir, "resource_cache").also { it.mkdirs() }

    /** 解析后的序列帧 PNG 缓存（避免每次启动重新 decode）。 */
    fun decodedSkinCacheRoot(): File =
        File(appContext.filesDir, "resource_cache/decoded_pac_maze_skins").also { it.mkdirs() }

    fun invalidatePacMazeSkinsBundle() {
        clearBundleCache("pac_maze_skins")
        invalidateBundleReadyMemo("pac_maze_skins")
        invalidateManifestCache()
        prefs().edit()
            .remove("bundle_pac_maze_skins")
            .remove("bundle_content_pac_maze_skins")
            .apply()
        com.example.funlife.ui.screens.platformer.PlatformerBootCache.invalidatePlayable()
        Log.i(TAG, "invalidated pac_maze_skins bundle cache")
    }

    fun isCloudResource(path: String): Boolean {
        val root = path.substringBefore('/').trim()
        return root in CLOUD_ROOTS
    }

    fun resolveFile(relativePath: String): File? {
        val normalized = normalizePath(relativePath) ?: return null
        if (normalized.startsWith("platformer_supertux/") && !isPlatformerSuperTuxCacheAuthoritative()) {
            return null
        }
        return resolveCacheFile(normalized)
    }

    /** Cache-only lookup (never falls back to assets). */
    private fun resolveCacheFile(relativePath: String): File? {
        val normalized = normalizePath(relativePath) ?: return null
        val cached = File(cacheRoot(), normalized)
        if (cached.isFile) return cached
        val root = normalized.substringBefore('/')
        val rest = normalized.substringAfter('/', "")
        if (root.isNotBlank() && rest.isNotBlank()) {
            val nested = File(cacheRoot(), "$root/$root/$rest")
            if (nested.isFile) return nested
        }
        return null
    }

    /**
     * Old cloud bundles (v3) only shipped 10 demo levels (901–910). Ignore that cache so APK / full
     * bundle assets are used until a complete v4+ cache is present.
     */
    private fun isPlatformerSuperTuxCacheAuthoritative(): Boolean {
        if (bundleCacheDir("platformer_supertux") == null) return false
        val version = resolveCacheFile("platformer_supertux/bundle_version.txt")
            ?.readText()?.trim()?.toIntOrNull() ?: return false
        if (version < PLATFORMER_SUPERTUX_BUNDLE_VERSION) return false
        return resolveCacheFile("platformer_supertux/levels/level_931/level.json") != null &&
            resolveCacheFile("platformer_supertux/levels/level_1018/level.json") != null
    }

    private fun purgeIncompletePlatformerSuperTuxCache() {
        val cacheDir = File(cacheRoot(), "platformer_supertux")
        if (!cacheDir.isDirectory) return
        if (isPlatformerSuperTuxCacheAuthoritative()) return
        val version = resolveCacheFile("platformer_supertux/bundle_version.txt")
            ?.readText()?.trim()?.toIntOrNull()
        Log.w(
            TAG,
            "Purging incomplete platformer_supertux cache " +
                "(v=${version ?: "?"}, need v$PLATFORMER_SUPERTUX_BUNDLE_VERSION + 107 levels)",
        )
        clearBundleCache("platformer_supertux")
    }

    /** 本地 cache 或 APK assets 中是否存在该资源（带内存 memo）。 */
    fun resourceExists(relativePath: String): Boolean {
        val normalized = normalizePath(relativePath) ?: return false
        resourceExistsMemo[normalized]?.let { return it }
        val exists = resourceExistsUncached(normalized)
        resourceExistsMemo[normalized] = exists
        return exists
    }

    private fun resourceExistsUncached(normalized: String): Boolean {
        if (resolveFile(normalized) != null) return true
        return try {
            appContext.assets.openFd(normalized).close()
            true
        } catch (_: Exception) {
            false
        }
    }

    /** 批量预检帧路径，减少重复 stat。 */
    fun prefetchResourceExists(paths: List<String>) {
        paths.forEach { path ->
            normalizePath(path)?.let { normalized ->
                if (!resourceExistsMemo.containsKey(normalized)) {
                    resourceExistsMemo[normalized] = resourceExistsUncached(normalized)
                }
            }
        }
    }

    fun bundleCacheDir(bundleId: String): File? {
        val direct = File(cacheRoot(), bundleId)
        if (direct.isDirectory && direct.list()?.isNotEmpty() == true) return direct
        val nested = File(cacheRoot(), "$bundleId/$bundleId")
        return nested.takeIf { it.isDirectory && it.list()?.isNotEmpty() == true }
    }

    /** 同步打开：cache → assets；云资源缺失时触发后台下载 */
    fun openInputStream(relativePath: String): InputStream? {
        val normalized = normalizePath(relativePath) ?: return null
        resolveFile(normalized)?.inputStream()?.let { return it }
        return try {
            appContext.assets.open(normalized)
        } catch (_: Exception) {
            if (isCloudResource(normalized)) {
                requestBundleDownloadAsync(normalized)
            }
            null
        }
    }

    /**
     * 逐格读取 sprite sheet：优先 cache 文件路径 / AssetFileDescriptor。
     * InputStream 版 RegionDecoder 在部分 WebP 上不可靠。
     */
    fun withBitmapRegionDecoder(relativePath: String, block: (android.graphics.BitmapRegionDecoder) -> Boolean): Boolean {
        val normalized = normalizePath(relativePath) ?: return false
        resolveFile(normalized)?.let { file ->
            return runCatching {
                val decoder = android.graphics.BitmapRegionDecoder.newInstance(file.absolutePath, false)
                    ?: return@runCatching false
                try {
                    block(decoder)
                } finally {
                    decoder.recycle()
                }
            }.getOrDefault(false)
        }
        return try {
            appContext.assets.open(normalized).use { input ->
                val bytes = input.readBytes()
                val decoder = android.graphics.BitmapRegionDecoder.newInstance(bytes, 0, bytes.size, false)
                    ?: return false
                try {
                    block(decoder)
                } finally {
                    decoder.recycle()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "withBitmapRegionDecoder asset failed: $normalized", e)
            false
        }
    }

    suspend fun openInputStreamAsync(relativePath: String): InputStream? =
        withContext(Dispatchers.IO) {
            val normalized = normalizePath(relativePath) ?: return@withContext null
            if (isCloudResource(normalized)) {
                bundleIdForPath(normalized)?.let { ensureBundle(it) }
            }
            openInputStream(normalized)
        }

    /**
     * Coil / AsyncImage 可用的数据源：resource_cache 文件 → assets URI。
     * 云资源缺失时触发后台下载并返回 null（调用方显示占位图）。
     */
    fun resolveCoilModel(relativePath: String): Any? {
        val normalized = normalizePath(relativePath) ?: return null
        resolveFile(normalized)?.let { return it }
        return try {
            appContext.assets.openFd(normalized).use { }
            "file:///android_asset/$normalized"
        } catch (_: Exception) {
            if (isCloudResource(normalized)) {
                requestBundleDownloadAsync(normalized)
            }
            null
        }
    }

    /** 启动时：拉 manifest，预下载 UI 装饰；核心游戏包走统一下载通道（带进度）。 */
    suspend fun syncAndPrefetchOnLaunch() = withContext(Dispatchers.IO) {
        if (!isAssetSourceConfigured()) return@withContext
        runCatching { refreshManifest() }
            .onFailure { Log.w(TAG, "manifest sync failed: ${it.message}") }
        listOf("login", "dibu").forEach { id ->
            runCatching { ensureBundle(id) }
                .onFailure { Log.w(TAG, "prefetch $id failed: ${it.message}") }
        }
        PacMazeResourceUpdateNotifier.refresh()
        PacMazeResourceUpdateNotifier.autoApplyPendingUpdates(appContext)
    }

    fun isBundleCached(bundleId: String): Boolean = bundleCacheDir(bundleId) != null

    suspend fun ensureBundle(
        bundleId: String,
        onProgress: ((BundleLoadProgress) -> Unit)? = null,
    ): Boolean = ensureBundleResult(bundleId, onProgress).isSuccess

    suspend fun ensureBundleResult(
        bundleId: String,
        onProgress: ((BundleLoadProgress) -> Unit)? = null,
    ): BundleEnsureResult = withContext(Dispatchers.IO) {
        val mutex = bundleMutex.getOrPut(bundleId) { Mutex() }
        mutex.withLock {
            try {
                reportBundleProgress(bundleId, BundleLoadProgress("manifest", 5), onProgress)
                val targetDir = bundleTargetDir(bundleId) ?: bundleId
                val manifest = fetchManifest()
                if (manifest == null) {
                    Log.w(TAG, "ensureBundle($bundleId): manifest unavailable")
                    if (isBundleReady(targetDir)) {
                        Log.i(TAG, "ensureBundle($bundleId): offline fallback — validated local cache")
                        reportBundleProgress(bundleId, BundleLoadProgress("ready", 100), onProgress)
                        return@withLock BundleEnsureResult.Success
                    }
                    return@withLock BundleEnsureResult.Failed(
                        BundleEnsureFailure.MANIFEST_UNAVAILABLE,
                        "无法连接资源服务器",
                    )
                }
                val bundle = manifest.bundles.firstOrNull { it.id == bundleId }
            if (bundle == null) {
                Log.w(TAG, "ensureBundle($bundleId): not in manifest (deploy asset_bundle?)")
                return@withLock BundleEnsureResult.Failed(
                    BundleEnsureFailure.BUNDLE_NOT_IN_MANIFEST,
                    "${GameResourceBundles.displayName(bundleId)}尚未发布到云端，请更新服务端 asset_bundle",
                )
            }
                lastManifestVersion = manifest.version
                val cacheValid = isBundleContentCurrent(bundleId, manifest)
                if (cacheValid) {
                    markBundleSynced(
                        bundleId = bundleId,
                        manifestVersion = manifest.version,
                        sha256 = bundle.sha256,
                        bundleVersion = bundle.bundleVersion,
                    )
                    reportBundleProgress(bundleId, BundleLoadProgress("ready", 100), onProgress)
                    return@withLock BundleEnsureResult.Success
                }
                // 原子更新：保留可用旧缓存，下载成功后再 promote；失败时不删本地包
                val url = cacheBustedDownloadUrl(
                    bundle.url?.takeIf { it.isNotBlank() }
                        ?: signBundleUrl(bundleId)?.url,
                    bundle.sha256,
                )
                if (url.isNullOrBlank()) {
                    Log.w(TAG, "ensureBundle($bundleId): no signed download url (COS zip missing?)")
                    return@withLock BundleEnsureResult.Failed(
                        BundleEnsureFailure.DOWNLOAD_URL_UNAVAILABLE,
                        "无法获取 $bundleId 下载链接",
                    )
                }
            runCatching {
                downloadAndUnzip(url, bundle.targetDir, bundleId, onProgress, bundle.sha256)
            }.onFailure {
                Log.e(TAG, "ensureBundle($bundleId) download failed", it)
                // 下载失败：若本地仍有可玩缓存则静默成功（后台更新失败不阻断）
                if (isBundlePlayable(bundle.targetDir)) {
                    Log.w(TAG, "ensureBundle($bundleId): download failed, keeping playable local cache")
                    reportBundleProgress(bundleId, BundleLoadProgress("ready", 100), onProgress)
                    return@withLock BundleEnsureResult.Success
                }
                clearBundleCache(bundle.targetDir)
                val detail = it.message.orEmpty()
                val reason = when {
                    detail.contains("validation", ignoreCase = true) ->
                        BundleEnsureFailure.VALIDATION_FAILED
                    else -> BundleEnsureFailure.DOWNLOAD_FAILED
                }
                val msg = when (reason) {
                    BundleEnsureFailure.VALIDATION_FAILED ->
                        "${GameResourceBundles.displayName(bundleId)}校验未通过（版本或文件缺失），请稍后重试"
                    else -> detail.ifBlank { null }
                }
                return@withLock BundleEnsureResult.Failed(reason, msg)
            }
                if (!isBundleContentCurrent(bundleId, manifest)) {
                    Log.e(TAG, "ensureBundle($bundleId): downloaded but validation failed")
                    if (isBundlePlayable(bundle.targetDir)) {
                        Log.w(TAG, "ensureBundle($bundleId): validation mismatch, keeping playable local cache")
                        reportBundleProgress(bundleId, BundleLoadProgress("ready", 100), onProgress)
                        return@withLock BundleEnsureResult.Success
                    }
                    clearBundleCache(bundle.targetDir)
                    val detail = bundleValidationFailureMessage(bundleId, manifest)
                    return@withLock BundleEnsureResult.Failed(
                        BundleEnsureFailure.VALIDATION_FAILED,
                        detail,
                    )
                }
                markBundleSynced(
                    bundleId = bundleId,
                    manifestVersion = manifest.version,
                    sha256 = bundle.sha256,
                    bundleVersion = bundle.bundleVersion,
                )
                reportBundleProgress(bundleId, BundleLoadProgress("ready", 100), onProgress)
                BundleEnsureResult.Success
            } finally {
                if (_activeDownload.value?.bundleId == bundleId) {
                    _activeDownload.value = null
                }
            }
        }
    }

    private fun reportBundleProgress(
        bundleId: String,
        progress: BundleLoadProgress,
        onProgress: ((BundleLoadProgress) -> Unit)?,
    ) {
        if (globalDownloadUiActive) {
            _activeDownload.value = ActiveBundleDownloadProgress(bundleId, progress.phase, progress.percent)
        }
        dispatchProgress(onProgress, progress)
    }

    /** 将单包进度映射为 0–100，供横幅/加载页展示。 */
    fun bundlePhaseToPercent(progress: BundleLoadProgress): Int = when (progress.phase) {
        "manifest" -> 8
        "download" -> 12 + (progress.percent * 72 / 100)
        "unzip" -> 86 + (progress.percent * 13 / 100)
        "ready" -> 100
        else -> progress.percent.coerceIn(0, 100)
    }

    /** 根据本地已就绪包 + 当前下载包，估算总进度。 */
    fun estimateGameResourceOverallPercent(
        active: ActiveBundleDownloadProgress?,
        pendingBundleIds: List<String> = localGameResourceStatus().pendingBundleIds,
    ): Int {
        if (active == null) return 0
        val order = GameResourceBundles.gameBootOrder
        val readyBefore = order.count { it !in pendingBundleIds && isBundleReady(it) }
        val activeIndex = order.indexOf(active.bundleId).coerceAtLeast(0)
        val weights = order.map { GameResourceBundles.bootWeight(it) }
        val totalWeight = weights.sum().coerceAtLeast(1)
        var completed = 0
        for (i in order.indices) {
            when {
                i < activeIndex -> completed += weights[i]
                order[i] == active.bundleId -> {
                    val inner = bundlePhaseToPercent(BundleLoadProgress(active.phase, active.percent))
                    return ((completed * 100 + weights[i] * inner) / totalWeight).coerceIn(1, 100)
                }
            }
        }
        val inner = bundlePhaseToPercent(BundleLoadProgress(active.phase, active.percent))
        return ((readyBefore * 100 + weights.getOrElse(activeIndex) { 30 } * inner) / totalWeight)
            .coerceIn(1, 100)
    }

    private var lastProgressKey = ""
    private var lastProgressMs = 0L

    /** 进度回调可能从下载 IO 线程触发，统一切回主线程；下载阶段节流避免主线程消息风暴。 */
    private fun dispatchProgress(
        callback: ((BundleLoadProgress) -> Unit)?,
        progress: BundleLoadProgress,
    ) {
        if (callback == null) return
        if (progress.phase == "download") {
            val key = "${progress.phase}:${progress.percent}"
            val now = SystemClock.elapsedRealtime()
            synchronized(this) {
                if (key == lastProgressKey && now - lastProgressMs < 120L) return
                lastProgressKey = key
                lastProgressMs = now
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            callback(progress)
        } else {
            mainHandler.post { callback(progress) }
        }
    }

    private fun dispatchBootProgress(
        callback: (bundleId: String, progress: BundleLoadProgress, overallPercent: Int) -> Unit,
        bundleId: String,
        progress: BundleLoadProgress,
        overallPercent: Int,
    ) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            callback(bundleId, progress, overallPercent)
        } else {
            mainHandler.post { callback(bundleId, progress, overallPercent) }
        }
    }

    private fun markBundleSynced(
        bundleId: String,
        manifestVersion: Int,
        sha256: String? = null,
        bundleVersion: Int? = null,
    ) {
        val editor = prefs().edit()
            .putInt(KEY_MANIFEST_VERSION, manifestVersion)
            .putInt("bundle_content_$bundleId", manifestVersion)
            .putBoolean("bundle_$bundleId", true)
        sha256?.trim()?.takeIf { it.isNotEmpty() }?.let {
            editor.putString("$KEY_BUNDLE_SHA256_PREFIX$bundleId", it.lowercase())
        }
        (bundleVersion ?: readLocalBundleVersion(bundleId))?.takeIf { it > 0 }?.let {
            editor.putInt("$KEY_BUNDLE_LOCAL_VERSION_PREFIX$bundleId", it)
        }
        editor.apply()
    }

    /** 检测豆人迷宫进局所需 bundle 是否需重新下载。 */
    suspend fun checkPacMazeResourceUpdates(): PacMazeResourceUpdateStatus = withContext(Dispatchers.IO) {
        val manifest = runCatching { fetchManifest() }.getOrNull()
        val manifestOk = manifest != null
        if (manifestOk) {
            lastManifestVersion = manifest!!.version
        }
        val manifestVer = if (manifestOk) manifest!!.version else lastManifestVersion
        val pending = GameResourceBundles.gameBootOrder.filter { bundleId ->
            !isBundleContentCurrent(bundleId, if (manifestOk) manifest else null)
        }
        PacMazeResourceUpdateStatus(
            manifestVersion = manifestVer,
            pendingBundleIds = pending,
        )
    }

    /**
     * 加载页顺序确保皮肤包 + 音效包；[onProgress] 的 overallPercent 为 0–100 总进度。
     */
    suspend fun ensurePacMazeBootBundles(
        bundleIds: List<String> = GameResourceBundles.gameBootOrder,
        onProgress: (bundleId: String, progress: BundleLoadProgress, overallPercent: Int) -> Unit,
    ): BundleEnsureResult = withContext(Dispatchers.IO) {
        val requested = bundleIds.ifEmpty { GameResourceBundles.gameBootOrder }
        val manifest = fetchManifest()
        val ids = if (manifest != null) {
            val available = manifest.bundles.map { it.id }.toSet()
            requested.filter { it in available }
        } else {
            requested
        }
        if (ids.isEmpty() && requested.isNotEmpty()) {
            Log.w(TAG, "ensurePacMazeBootBundles: none of $requested in manifest")
            return@withContext BundleEnsureResult.Failed(
                BundleEnsureFailure.BUNDLE_NOT_IN_MANIFEST,
                "游戏资源尚未发布到云端，请更新 asset_bundle 云函数",
            )
        }
        val skipped = requested - ids.toSet()
        if (skipped.isNotEmpty()) {
            Log.w(TAG, "ensurePacMazeBootBundles: skip (not in manifest): $skipped")
        }
        val weights = ids.map { id -> GameResourceBundles.bootWeight(id) }
        val totalWeight = weights.sum().coerceAtLeast(1)
        val progressInner = java.util.concurrent.ConcurrentHashMap<String, Int>()
        ids.forEach { progressInner[it] = 0 }
        val progressLock = Any()

        fun dispatchCombinedProgress(activeBundleId: String) {
            var weightedSum = 0
            ids.forEachIndexed { index, id ->
                val inner = progressInner[id] ?: 0
                weightedSum += weights[index] * inner
            }
            val overall = (weightedSum / totalWeight).coerceIn(1, 99)
            val bp = BundleLoadProgress(
                phase = progressInner[activeBundleId]?.let { if (it >= 100) "ready" else "download" } ?: "download",
                percent = progressInner[activeBundleId] ?: 0,
            )
            dispatchBootProgress(onProgress, activeBundleId, bp, overall)
        }

        // 三包容器并行下载：总耗时 ≈ max(skins, sfx, platformer)，而非串行相加
        val results = coroutineScope {
            ids.map { bundleId ->
                async {
                    val result = ensureBundleResult(bundleId) { bp ->
                        progressInner[bundleId] = bundlePhaseToPercent(bp)
                        synchronized(progressLock) {
                            dispatchCombinedProgress(bundleId)
                        }
                    }
                    bundleId to result
                }
            }.awaitAll()
        }
        results.forEach { (_, result) ->
            if (result !is BundleEnsureResult.Success) return@withContext result
        }
        ids.forEach { bundleId ->
            dispatchBootProgress(
                onProgress,
                bundleId,
                BundleLoadProgress("ready", 100),
                100,
            )
        }
        _activeDownload.value = null
        BundleEnsureResult.Success
    }

    /** @deprecated 串行下载，仅测试/回退用 */
    @Suppress("unused")
    private suspend fun ensurePacMazeBootBundlesSequential(
        bundleIds: List<String>,
        onProgress: (bundleId: String, progress: BundleLoadProgress, overallPercent: Int) -> Unit,
    ): BundleEnsureResult = withContext(Dispatchers.IO) {
        val ids = bundleIds
        val weights = ids.map { id -> GameResourceBundles.bootWeight(id) }
        val totalWeight = weights.sum().coerceAtLeast(1)
        var completedWeight = 0
        for ((index, bundleId) in ids.withIndex()) {
            val weight = weights[index]
            val result = ensureBundleResult(bundleId) { bp ->
                val inner = bundlePhaseToPercent(bp)
                val overall = ((completedWeight * 100 + weight * inner) / totalWeight).coerceIn(1, 99)
                dispatchBootProgress(onProgress, bundleId, bp, overall)
            }
            if (result !is BundleEnsureResult.Success) return@withContext result
            completedWeight += weight
            dispatchBootProgress(
                onProgress,
                bundleId,
                BundleLoadProgress("ready", 100),
                (completedWeight * 100 / totalWeight).coerceIn(0, 100),
            )
        }
        BundleEnsureResult.Success
    }

    private suspend fun refreshManifest(): AssetManifestResponse? = fetchManifest()

    private fun loadManifestFromPrefs(): AssetManifestResponse? {
        val at = prefs().getLong(KEY_MANIFEST_CACHE_AT, 0L)
        if (System.currentTimeMillis() - at > MANIFEST_CACHE_TTL_MS) return null
        val json = prefs().getString(KEY_MANIFEST_CACHE_JSON, null) ?: return null
        return runCatching { gson.fromJson(json, AssetManifestResponse::class.java) }.getOrNull()
    }

    private fun persistManifestCache(manifest: AssetManifestResponse) {
        cachedManifest = manifest
        cachedManifestAtMs = System.currentTimeMillis()
        lastManifestVersion = manifest.version
        prefs().edit()
            .putString(KEY_MANIFEST_CACHE_JSON, gson.toJson(manifest))
            .putLong(KEY_MANIFEST_CACHE_AT, cachedManifestAtMs)
            .putInt(KEY_MANIFEST_VERSION, manifest.version)
            .apply()
    }

    private suspend fun fetchManifest(): AssetManifestResponse? {
        val now = System.currentTimeMillis()
        cachedManifest?.let { m ->
            if (now - cachedManifestAtMs < MANIFEST_CACHE_TTL_MS) return m
        }
        loadManifestFromPrefs()?.let { m ->
            cachedManifest = m
            cachedManifestAtMs = prefs().getLong(KEY_MANIFEST_CACHE_AT, now)
            lastManifestVersion = m.version
            return m
        }
        val fetched = fetchStaticManifest() ?: fetchCloudManifest()
        if (fetched != null) {
            persistManifestCache(fetched)
        }
        return fetched
    }

    private fun requestBundleDownloadAsync(path: String) {
        val bundleId = bundleIdForPath(path) ?: return
        scope.launch {
            runCatching { ensureBundle(bundleId) }
                .onFailure { Log.w(TAG, "ensureBundle($bundleId) failed: ${it.message}") }
        }
    }

    private fun bundleIdForPath(path: String): String? {
        val root = path.substringBefore('/')
        return if (root in CLOUD_ROOTS) root else null
    }

    /** pac_maze_sfx 需含 Kenney UI 音效；旧缓存仅有 BGM/SFX 时视为未完成，触发重新下载。 */
    private fun isBundleReady(targetDir: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        val versionKey = bundleVersionKey(targetDir)
        bundleReadyMemo[targetDir]?.let { snap ->
            if (now - snap.checkedAtMs < BUNDLE_READY_TTL_MS && snap.versionKey == versionKey) {
                return snap.ready
            }
        }
        val ready = isBundleReadyUncached(targetDir)
        bundleReadyMemo[targetDir] = BundleReadySnapshot(ready, now, versionKey)
        return ready
    }

    private fun isBundleReadyUncached(targetDir: String): Boolean {
        if (targetDir == "platformer_supertux") {
            return platformerSuperTuxBundleReady()
        }
        if (bundleCacheDir(targetDir) == null) return false
        if (targetDir == "pac_maze_sfx") {
            return resolveFile("pac_maze_sfx/curated/ui/back.ogg") != null
        }
        if (targetDir == "pac_maze_skins") {
            if (bundleCacheDir(targetDir) == null) return false
            if (!pacMazeSkinsMarkersReady { resolveFile(it) }) return false
            val versionText = resolveFile("pac_maze_skins/bundle_version.txt")?.readText()?.trim()
            val required = requiredBundleVersion("pac_maze_skins") ?: PAC_MAZE_SKINS_BUNDLE_VERSION
            return isPacMazeSkinsVersionValid(versionText?.toIntOrNull(), required)
        }
        if (targetDir == "platformer_characters") {
            if (!platformerCharactersMarkersReady { resolveFile(it) }) return false
            val versionText = resolveFile("platformer_characters/bundle_version.txt")?.readText()?.trim()
            val required = requiredBundleVersion("platformer_characters") ?: PLATFORMER_CHARACTERS_BUNDLE_VERSION
            val version = versionText?.toIntOrNull() ?: return false
            return version >= required
        }
        if (targetDir == "platformer_sfx") {
            if (!platformerSfxMarkersReady { resolveFile(it) }) return false
            val versionText = resolveFile("platformer_sfx/bundle_version.txt")?.readText()?.trim()
            val required = requiredBundleVersion("platformer_sfx") ?: PLATFORMER_SFX_BUNDLE_VERSION
            val version = versionText?.toIntOrNull() ?: return false
            return version >= required
        }
        return true
    }

    private fun platformerSuperTuxBundleReady(): Boolean {
        if (isPlatformerSuperTuxCacheAuthoritative() &&
            platformerSuperTuxMarkersReady { resolveCacheFile(it) }
        ) {
            val versionText = resolveCacheFile("platformer_supertux/bundle_version.txt")
                ?.readText()?.trim()?.toIntOrNull() ?: return false
            val required = requiredBundleVersion("platformer_supertux") ?: PLATFORMER_SUPERTUX_BUNDLE_VERSION
            return versionText >= required
        }
        return platformerSuperTuxAssetsReady()
    }

    private fun platformerSuperTuxAssetsReady(): Boolean =
        PLATFORMER_SUPERTUX_MARKERS.all { group -> group.any { assetFileExists(it) } }

    private fun assetFileExists(relativePath: String): Boolean = try {
        appContext.assets.openFd(relativePath).close()
        true
    } catch (_: Exception) {
        false
    }

    private fun clearBundleCache(targetDir: String) {
        File(cacheRoot(), targetDir).deleteRecursively()
        File(cacheRoot(), "$targetDir/$targetDir").deleteRecursively()
        invalidateBundleReadyMemo(targetDir)
        if (targetDir == "pac_maze_skins") {
            decodedSkinCacheRoot().deleteRecursively()
        }
        if (targetDir == "platformer_characters") {
            invalidatePlatformerDecodedCache()
        }
    }

    private fun bundleTargetDir(bundleId: String): String? = when (bundleId) {
        "xiangkuang", "pet", "login", "renge", "dibu", "wheel",
        "pac_maze_sfx", "pac_maze_skins", "platformer_characters",
        "platformer_sfx", "platformer_supertux" -> bundleId
        else -> null
    }

    private fun normalizePath(raw: String): String? {
        val p = raw.trim().trimStart('/').replace('\\', '/')
        if (p.isEmpty() || p.contains("..")) return null
        return p
    }

    private fun bundleValidationFailureMessage(bundleId: String, manifest: AssetManifestResponse? = cachedManifest): String {
        val name = GameResourceBundles.displayName(bundleId)
        val required = requiredBundleVersion(bundleId, manifest)
        if (bundleId == "pac_maze_skins" && required != null) {
            return "云端${name}版本低于 v$required，请稍后重试或联系更新资源"
        }
        if (bundleId == "platformer_characters" && required != null) {
            return "云端${name}版本低于 v$required，请稍后重试"
        }
        return "${name}不完整，请重试"
    }

    /** 本地缓存是否足够进游戏（允许略低于 manifest 最新版，用于更新失败降级） */
    private fun isBundlePlayable(targetDir: String): Boolean = isBundlePlayableUncached(targetDir)

    private fun isBundlePlayableUncached(targetDir: String): Boolean {
        if (bundleCacheDir(targetDir) == null) return false
        if (targetDir == "pac_maze_sfx") {
            return resolveFile("pac_maze_sfx/curated/ui/back.ogg") != null
        }
        if (targetDir == "pac_maze_skins") {
            if (!pacMazeSkinsMarkersReady { resolveFile(it) }) return false
            val versionText = resolveFile("pac_maze_skins/bundle_version.txt")?.readText()?.trim()
            return isPacMazeSkinsVersionValid(
                versionText?.toIntOrNull(),
                PAC_MAZE_SKINS_MIN_BUNDLE_VERSION,
            )
        }
        if (targetDir == "platformer_characters") {
            if (!platformerCharactersMarkersReady { resolveFile(it) }) return false
            val versionText = resolveFile("platformer_characters/bundle_version.txt")?.readText()?.trim()
            return (versionText?.toIntOrNull() ?: 0) >= 1
        }
        if (targetDir == "platformer_sfx") {
            return platformerSfxMarkersReady { resolveFile(it) }
        }
        if (targetDir == "platformer_supertux") {
            return isPlatformerSuperTuxCacheAuthoritative() &&
                platformerSuperTuxMarkersReady { resolveCacheFile(it) }
        }
        return true
    }

    private fun isPacMazeSkinsVersionValid(version: Int?, required: Int = requiredBundleVersion("pac_maze_skins")
        ?: PAC_MAZE_SKINS_BUNDLE_VERSION): Boolean {
        if (version == null) return false
        return version >= required.coerceAtLeast(PAC_MAZE_SKINS_MIN_BUNDLE_VERSION)
    }

    private fun cacheBustedDownloadUrl(url: String?, sha256: String?): String? {
        val base = url?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (base.contains("?v=") || base.contains("&v=")) return base
        val bust = sha256?.trim()?.takeIf { it.length >= 8 }?.take(12) ?: return base
        val sep = if (base.contains('?')) "&" else "?"
        return "$base${sep}v=$bust"
    }

    private fun pacMazeSkinsMarkersReady(resolve: (String) -> File?): Boolean =
        PAC_MAZE_SKINS_MARKERS.all { group -> group.any { resolve(it) != null } }

    private fun platformerCharactersMarkersReady(resolve: (String) -> File?): Boolean =
        PLATFORMER_CHARACTERS_MARKERS.all { group -> group.any { resolve(it) != null } }

    private fun platformerSfxMarkersReady(resolve: (String) -> File?): Boolean =
        PLATFORMER_SFX_MARKERS.all { group -> group.any { resolve(it) != null } }

    private fun platformerSuperTuxMarkersReady(resolve: (String) -> File?): Boolean =
        PLATFORMER_SUPERTUX_MARKERS.all { group -> group.any { resolve(it) != null } }

    private fun isOnWifi(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun isAssetSourceConfigured(): Boolean =
        BuildConfig.ASSET_MANIFEST_URL.isNotBlank() || BuildConfig.VIP_BACKEND_URL.isNotBlank()

    private suspend fun fetchStaticManifest(): AssetManifestResponse? {
        val url = BuildConfig.ASSET_MANIFEST_URL.trim()
        if (url.isBlank()) return null
        val req = Request.Builder()
            .url(url)
            .header("Cache-Control", "no-cache")
            .get()
            .build()
        return runCatching {
            apiClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "static manifest HTTP ${resp.code}")
                    return null
                }
                parseManifestBody(resp.body?.string().orEmpty())
            }
        }.onFailure { Log.w(TAG, "static manifest failed: ${it.message}") }
            .getOrNull()
    }

    private suspend fun fetchCloudManifest(): AssetManifestResponse? {
        val base = BuildConfig.VIP_BACKEND_URL.trimEnd('/')
        if (base.isBlank()) return null
        val body = mapOf("action" to "manifest")
        val req = Request.Builder()
            .url("$base/asset_bundle")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        apiClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                Log.w(TAG, "manifest HTTP ${resp.code}")
                return null
            }
            val text = resp.body?.string().orEmpty()
            return parseManifestBody(text)
        }
    }

    private fun parseManifestBody(text: String): AssetManifestResponse? {
        if (text.isBlank()) return null
        val mapType = object : TypeToken<Map<String, Any>>() {}.type
        val root = gson.fromJson<Map<String, Any>>(text, mapType) ?: return null
        if (root["ok"] != true) return null
        val manifest = gson.fromJson(text, AssetManifestResponse::class.java)
        if (manifest != null) lastManifestVersion = manifest.version
        return manifest
    }

    private data class SignResult(val url: String?)

    private suspend fun signBundleUrl(bundleId: String): SignResult? {
        val base = BuildConfig.VIP_BACKEND_URL.trimEnd('/')
        if (base.isBlank()) return null
        val body = mapOf("action" to "sign", "bundleId" to bundleId)
        val req = Request.Builder()
            .url("$base/asset_bundle")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        apiClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                Log.w(TAG, "sign($bundleId) HTTP ${resp.code}")
                return null
            }
            val text = resp.body?.string().orEmpty()
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val root = gson.fromJson<Map<String, Any>>(text, mapType) ?: return null
            if (root["ok"] != true) return null
            @Suppress("UNCHECKED_CAST")
            return SignResult(root["url"] as? String)
        }
    }

    private fun downloadAndUnzip(
        url: String,
        targetDir: String,
        bundleId: String,
        onProgress: ((BundleLoadProgress) -> Unit)? = null,
        expectedSha256: String? = null,
    ) {
        val tmpZip = File(appContext.cacheDir, "bundle_${targetDir}_${System.currentTimeMillis()}.zip")
        val stagingRoot = File(appContext.cacheDir, "bundle_staging_${targetDir}_${System.currentTimeMillis()}")
        stagingRoot.mkdirs()
        try {
            val req = Request.Builder()
            .url(url)
            .header("Cache-Control", "no-cache")
            .get()
            .build()
            downloadClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("download failed HTTP ${resp.code}")
                val body = resp.body ?: error("empty body")
                val totalBytes = body.contentLength()
                reportBundleProgress(bundleId, BundleLoadProgress("download", 0), onProgress)
                body.byteStream().buffered().use { input ->
                    tmpZip.outputStream().buffered().use { out ->
                        val buffer = ByteArray(16384)
                        var downloaded = 0L
                        var lastReportedPct = -1
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            out.write(buffer, 0, read)
                            downloaded += read
                            val pct = if (totalBytes > 0L) {
                                ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 99)
                            } else {
                                estimateUnknownDownloadPercent(downloaded)
                            }
                            if (pct != lastReportedPct) {
                                lastReportedPct = pct
                                reportBundleProgress(bundleId, BundleLoadProgress("download", pct), onProgress)
                            }
                        }
                    }
                }
                reportBundleProgress(bundleId, BundleLoadProgress("download", 100), onProgress)
            }
            expectedSha256?.trim()?.takeIf { it.isNotEmpty() }?.let { expected ->
                val actual = sha256HexFile(tmpZip)
                if (!actual.equals(expected, ignoreCase = true)) {
                    error("bundle sha256 mismatch (expected=$expected actual=$actual)")
                }
            }
            reportBundleProgress(bundleId, BundleLoadProgress("unzip", 0), onProgress)
            unzipToCache(tmpZip, stagingRoot)
            reportBundleProgress(bundleId, BundleLoadProgress("unzip", 100), onProgress)
            if (!isBundleReadyInRoot(stagingRoot, targetDir)) {
                error("bundle validation failed after unzip")
            }
            promoteStagingToCache(stagingRoot, targetDir)
            Log.i(TAG, "bundle ready: $targetDir")
        } finally {
            stagingRoot.deleteRecursively()
            tmpZip.delete()
        }
    }

    /** CDN 常不返回 Content-Length，按已下载体量估算进度避免长期停在 8%。 */
    private fun estimateUnknownDownloadPercent(downloadedBytes: Long): Int = when {
        downloadedBytes < 64 * 1024 -> 1 + (downloadedBytes / (64 * 1024)).toInt().coerceAtMost(4)
        downloadedBytes < 256 * 1024 -> 5 + ((downloadedBytes - 64 * 1024) * 8 / (192 * 1024)).toInt()
        downloadedBytes < 1024 * 1024 -> 13 + ((downloadedBytes - 256 * 1024) * 10 / (768 * 1024)).toInt()
        downloadedBytes < 4 * 1024 * 1024 -> 23 + ((downloadedBytes - 1024 * 1024) * 12 / (3 * 1024 * 1024)).toInt()
        downloadedBytes < 12 * 1024 * 1024 -> 35 + ((downloadedBytes - 4 * 1024 * 1024) * 18 / (8 * 1024 * 1024)).toInt()
        downloadedBytes < 24 * 1024 * 1024 -> 53 + ((downloadedBytes - 12 * 1024 * 1024) * 12 / (12 * 1024 * 1024)).toInt()
        downloadedBytes < 48 * 1024 * 1024 -> 65 + ((downloadedBytes - 24 * 1024 * 1024) * 14 / (24 * 1024 * 1024)).toInt()
        downloadedBytes < 96 * 1024 * 1024 -> 79 + ((downloadedBytes - 48 * 1024 * 1024) * 10 / (48 * 1024 * 1024)).toInt()
        else -> (89 + (downloadedBytes - 96 * 1024 * 1024) / (20 * 1024 * 1024)).toInt().coerceAtMost(99)
    }

    private fun resolveFileInRoot(root: File, relativePath: String): File? {
        val normalized = normalizePath(relativePath) ?: return null
        val cached = File(root, normalized)
        if (cached.isFile) return cached
        val dir = normalized.substringBefore('/')
        val rest = normalized.substringAfter('/', "")
        if (dir.isNotBlank() && rest.isNotBlank()) {
            val nested = File(root, "$dir/$dir/$rest")
            if (nested.isFile) return nested
        }
        return null
    }

    private fun isBundleReadyInRoot(root: File, targetDir: String): Boolean {
        if (!File(root, targetDir).isDirectory && File(root, "$targetDir/$targetDir").let { !it.isDirectory }) {
            return false
        }
        if (targetDir == "pac_maze_sfx") {
            return resolveFileInRoot(root, "pac_maze_sfx/curated/ui/back.ogg") != null
        }
        if (targetDir == "pac_maze_skins") {
            if (!pacMazeSkinsMarkersReady { resolveFileInRoot(root, it) }) return false
            val versionText = resolveFileInRoot(root, "pac_maze_skins/bundle_version.txt")?.readText()?.trim()
            val required = requiredBundleVersion("pac_maze_skins") ?: PAC_MAZE_SKINS_BUNDLE_VERSION
            return isPacMazeSkinsVersionValid(versionText?.toIntOrNull(), required)
        }
        if (targetDir == "platformer_characters") {
            if (!platformerCharactersMarkersReady { resolveFileInRoot(root, it) }) return false
            val versionText = resolveFileInRoot(root, "platformer_characters/bundle_version.txt")?.readText()?.trim()
            val required = requiredBundleVersion("platformer_characters") ?: PLATFORMER_CHARACTERS_BUNDLE_VERSION
            val version = versionText?.toIntOrNull() ?: return false
            return version >= required
        }
        if (targetDir == "platformer_sfx") {
            if (!platformerSfxMarkersReady { resolveFileInRoot(root, it) }) return false
            val versionText = resolveFileInRoot(root, "platformer_sfx/bundle_version.txt")?.readText()?.trim()
            val required = requiredBundleVersion("platformer_sfx") ?: PLATFORMER_SFX_BUNDLE_VERSION
            val version = versionText?.toIntOrNull() ?: return false
            return version >= required
        }
        if (targetDir == "platformer_supertux") {
            if (!platformerSuperTuxMarkersReady { resolveFileInRoot(root, it) }) return false
            val versionText = resolveFileInRoot(root, "platformer_supertux/bundle_version.txt")?.readText()?.trim()
            val required = requiredBundleVersion("platformer_supertux") ?: PLATFORMER_SUPERTUX_BUNDLE_VERSION
            val version = versionText?.toIntOrNull() ?: return false
            return version >= required
        }
        return true
    }

    private fun promoteStagingToCache(stagingRoot: File, targetDir: String) {
        clearBundleCache(targetDir)
        val direct = File(stagingRoot, targetDir)
        val nested = File(stagingRoot, "$targetDir/$targetDir")
        val src = when {
            direct.isDirectory -> direct
            nested.isDirectory -> nested
            else -> error("staging missing $targetDir")
        }
        val dest = File(cacheRoot(), targetDir)
        src.copyRecursively(dest, overwrite = true)
    }

    private fun sha256HexFile(file: File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buf = ByteArray(8192)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun unzipToCache(zipFile: File, destRoot: File) {
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            val destCanonical = destRoot.canonicalPath
            var entryCount = 0
            var totalBytes = 0L
            while (entry != null) {
                entryCount++
                if (entryCount > MAX_ZIP_ENTRIES) error("zip too many entries")
                val name = entry.name.replace('\\', '/')
                if (!name.contains("..")) {
                    val outFile = File(destRoot, name)
                    if (!outFile.canonicalPath.startsWith(destCanonical)) {
                        error("zip slip blocked: $name")
                    }
                    if (entry.isDirectory) {
                        ensureExtractDir(outFile)
                    } else {
                        outFile.parentFile?.let { ensureExtractDir(it) }
                        outFile.outputStream().use { out ->
                            val copied = zis.copyTo(out)
                            totalBytes += copied
                            if (totalBytes > MAX_ZIP_UNCOMPRESSED_BYTES) {
                                error("zip uncompressed size limit exceeded")
                            }
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private const val MAX_ZIP_UNCOMPRESSED_BYTES = 512L * 1024 * 1024
    private const val MAX_ZIP_ENTRIES = 20_000

    /** 解压前确保目录存在；若路径上误留了同名文件（旧缓存残留），先删掉再建目录。 */
    private fun ensureExtractDir(dir: File) {
        val parent = dir.parentFile
        if (parent != null && parent != dir) {
            ensureExtractDir(parent)
        }
        if (dir.exists()) {
            if (dir.isDirectory) return
            if (!dir.delete()) {
                error("${dir.path}: open failed: ENOTDIR (Not a directory)")
            }
        }
        if (!dir.mkdir() && !dir.isDirectory) {
            error("cannot mkdir: ${dir.path}")
        }
    }
}
