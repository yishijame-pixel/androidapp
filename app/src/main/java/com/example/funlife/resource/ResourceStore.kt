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
import kotlinx.coroutines.launch
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

    /** 与 COS 上已发布的 pac_maze_skins.zip 保持一致；preview.png 为可选增强项 */
    const val PAC_MAZE_SKINS_BUNDLE_VERSION = 7

    private val PAC_MAZE_SKINS_MARKERS = listOf(
        "pac_maze_skins/bundle_version.txt",
        "pac_maze_skins/food_chick_walker_pro_max/walk/walk_1.png",
        "pac_maze_skins/xia_walk/walk/walk_1.png",
        "pac_maze_skins/laoshu_walk/walk/walk_1.png",
        "pac_maze_skins/qinting_walk/walk/walk_1.png",
        "pac_maze_skins/wenzi_walk/walk/walk_1.png",
        "pac_maze_skins/toushi_walk/walk/walk_1.png",
    )

    private val CLOUD_ROOTS = setOf("xiangkuang", "pet", "login", "renge", "dibu", "wheel", "pac_maze_sfx", "pac_maze_skins")

    private lateinit var appContext: Context
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bundleMutex = ConcurrentHashMap<String, Mutex>()
    @Volatile private var lastManifestVersion = 0
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

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
    }

    private fun prefs() =
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 供 [PacMazeResourceUpdateNotifier] 读写「已读 manifest 版本」等元数据。 */
    fun prefsAccessor() = prefs()

    fun lastFetchedManifestVersion(): Int = lastManifestVersion

    fun isPacMazeBundleReady(bundleId: String): Boolean = isBundleReady(bundleId)

    private fun cacheRoot(): File =
        File(appContext.filesDir, "resource_cache").also { it.mkdirs() }

    /** 解析后的序列帧 PNG 缓存（避免每次启动重新 decode）。 */
    fun decodedSkinCacheRoot(): File =
        File(appContext.filesDir, "resource_cache/decoded_pac_maze_skins").also { it.mkdirs() }

    fun invalidatePacMazeSkinsBundle() {
        clearBundleCache("pac_maze_skins")
        prefs().edit()
            .remove("bundle_pac_maze_skins")
            .remove("bundle_content_pac_maze_skins")
            .apply()
        Log.i(TAG, "invalidated pac_maze_skins bundle cache")
    }

    fun isCloudResource(path: String): Boolean {
        val root = path.substringBefore('/').trim()
        return root in CLOUD_ROOTS
    }

    fun resolveFile(relativePath: String): File? {
        val normalized = normalizePath(relativePath) ?: return null
        val cached = File(cacheRoot(), normalized)
        if (cached.isFile) return cached
        // 兼容 zip 多包一层目录：resource_cache/pac_maze_sfx/pac_maze_sfx/...
        val root = normalized.substringBefore('/')
        val rest = normalized.substringAfter('/', "")
        if (root.isNotBlank() && rest.isNotBlank()) {
            val nested = File(cacheRoot(), "$root/$root/$rest")
            if (nested.isFile) return nested
        }
        return null
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

    /** 启动时：拉 manifest，WiFi 下预下载 login + dibu */
    suspend fun syncAndPrefetchOnLaunch() = withContext(Dispatchers.IO) {
        if (BuildConfig.VIP_BACKEND_URL.isBlank()) return@withContext
        runCatching { refreshManifest() }
            .onFailure { Log.w(TAG, "manifest sync failed: ${it.message}") }
        if (!isOnWifi()) return@withContext
        listOf("login", "dibu", PacMazeResourceBundles.SKINS, PacMazeResourceBundles.SFX).forEach { id ->
            runCatching { ensureBundle(id) }
                .onFailure { Log.w(TAG, "prefetch $id failed: ${it.message}") }
        }
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
            dispatchProgress(onProgress, BundleLoadProgress("manifest", 5))
            val targetDir = bundleTargetDir(bundleId) ?: bundleId
            val manifest = fetchManifest()
            if (manifest == null) {
                Log.w(TAG, "ensureBundle($bundleId): manifest unavailable")
                if (isBundleReady(targetDir)) {
                    Log.i(TAG, "ensureBundle($bundleId): offline fallback — validated local cache")
                    dispatchProgress(onProgress, BundleLoadProgress("ready", 100))
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
                    "云端清单未包含 $bundleId",
                )
            }
            lastManifestVersion = manifest.version
            val cacheValid = isBundleReady(bundle.targetDir)
            if (cacheValid) {
                markBundleSynced(bundleId, manifest.version)
                dispatchProgress(onProgress, BundleLoadProgress("ready", 100))
                return@withLock BundleEnsureResult.Success
            }
            clearBundleCache(bundle.targetDir)
            val url = bundle.url?.takeIf { it.isNotBlank() }
                ?: signBundleUrl(bundleId)?.url
            if (url.isNullOrBlank()) {
                Log.w(TAG, "ensureBundle($bundleId): no signed download url (COS zip missing?)")
                return@withLock BundleEnsureResult.Failed(
                    BundleEnsureFailure.DOWNLOAD_URL_UNAVAILABLE,
                    "无法获取 $bundleId 下载链接",
                )
            }
            runCatching {
                downloadAndUnzip(url, bundle.targetDir, onProgress)
            }.onFailure {
                Log.e(TAG, "ensureBundle($bundleId) download failed", it)
                clearBundleCache(bundle.targetDir)
                return@withLock BundleEnsureResult.Failed(
                    BundleEnsureFailure.DOWNLOAD_FAILED,
                    it.message,
                )
            }
            if (!isBundleReady(bundle.targetDir)) {
                Log.e(TAG, "ensureBundle($bundleId): downloaded but validation failed")
                clearBundleCache(bundle.targetDir)
                return@withLock BundleEnsureResult.Failed(
                    BundleEnsureFailure.VALIDATION_FAILED,
                    "${PacMazeResourceBundles.displayName(bundleId)}不完整，请重试",
                )
            }
            markBundleSynced(bundleId, manifest.version)
            dispatchProgress(onProgress, BundleLoadProgress("ready", 100))
            BundleEnsureResult.Success
        }
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

    private fun markBundleSynced(bundleId: String, manifestVersion: Int) {
        prefs().edit()
            .putInt(KEY_MANIFEST_VERSION, manifestVersion)
            .putInt("bundle_content_$bundleId", manifestVersion)
            .putBoolean("bundle_$bundleId", true)
            .apply()
    }

    /** 检测豆人迷宫进局所需 bundle 是否需重新下载。 */
    suspend fun checkPacMazeResourceUpdates(): PacMazeResourceUpdateStatus = withContext(Dispatchers.IO) {
        val manifest = runCatching { fetchManifest() }.getOrNull()
        val manifestOk = manifest != null
        if (manifestOk) {
            lastManifestVersion = manifest!!.version
        }
        val manifestVer = if (manifestOk) manifest!!.version else lastManifestVersion
        val pref = prefs()
        val pending = PacMazeResourceBundles.bootOrder.filter { bundleId ->
            !isBundleReady(bundleId) ||
                (manifestOk && pref.getInt("bundle_content_$bundleId", 0) < manifestVer)
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
        bundleIds: List<String> = PacMazeResourceBundles.bootOrder,
        onProgress: (bundleId: String, progress: BundleLoadProgress, overallPercent: Int) -> Unit,
    ): BundleEnsureResult = withContext(Dispatchers.IO) {
        val ids = bundleIds.ifEmpty { PacMazeResourceBundles.bootOrder }
        val weights = ids.map { id ->
            when (id) {
                PacMazeResourceBundles.SKINS -> 62
                PacMazeResourceBundles.SFX -> 38
                else -> 30
            }
        }
        val totalWeight = weights.sum().coerceAtLeast(1)
        var completedWeight = 0
        for ((index, bundleId) in ids.withIndex()) {
            val weight = weights[index]
            val result = ensureBundleResult(bundleId) { bp ->
                val inner = bundlePhaseToPercent(bp)
                val overall = ((completedWeight * 100 + weight * inner) / totalWeight).coerceIn(0, 99)
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

    private fun bundlePhaseToPercent(progress: BundleLoadProgress): Int = when (progress.phase) {
        "manifest" -> 8
        "download" -> 12 + (progress.percent * 72 / 100)
        "unzip" -> 86 + (progress.percent * 13 / 100)
        "ready" -> 100
        else -> progress.percent.coerceIn(0, 100)
    }

    private suspend fun refreshManifest(): AssetManifestResponse? = fetchManifest()

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
        if (bundleCacheDir(targetDir) == null) return false
        if (targetDir == "pac_maze_sfx") {
            return resolveFile("pac_maze_sfx/curated/ui/back.ogg") != null
        }
        if (targetDir == "pac_maze_skins") {
            if (bundleCacheDir(targetDir) == null) return false
            if (!PAC_MAZE_SKINS_MARKERS.all { resolveFile(it) != null }) return false
            val versionText = resolveFile("pac_maze_skins/bundle_version.txt")?.readText()?.trim()
            return versionText?.toIntOrNull() == PAC_MAZE_SKINS_BUNDLE_VERSION
        }
        return true
    }

    private fun clearBundleCache(targetDir: String) {
        File(cacheRoot(), targetDir).deleteRecursively()
        File(cacheRoot(), "$targetDir/$targetDir").deleteRecursively()
        if (targetDir == "pac_maze_skins") {
            decodedSkinCacheRoot().deleteRecursively()
        }
    }

    private fun bundleTargetDir(bundleId: String): String? = when (bundleId) {
        "xiangkuang", "pet", "login", "renge", "dibu", "wheel", "pac_maze_sfx", "pac_maze_skins" -> bundleId
        else -> null
    }

    private fun normalizePath(raw: String): String? {
        val p = raw.trim().trimStart('/').replace('\\', '/')
        if (p.isEmpty() || p.contains("..")) return null
        return p
    }

    private fun isOnWifi(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
    }

    private suspend fun fetchManifest(): AssetManifestResponse? {
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
            if (text.isBlank()) return null
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val root = gson.fromJson<Map<String, Any>>(text, mapType) ?: return null
            if (root["ok"] != true) return null
            val manifest = gson.fromJson(text, AssetManifestResponse::class.java)
            if (manifest != null) lastManifestVersion = manifest.version
            return manifest
        }
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
        onProgress: ((BundleLoadProgress) -> Unit)? = null,
    ) {
        val tmpZip = File(appContext.cacheDir, "bundle_${targetDir}_${System.currentTimeMillis()}.zip")
        val stagingRoot = File(appContext.cacheDir, "bundle_staging_${targetDir}_${System.currentTimeMillis()}")
        stagingRoot.mkdirs()
        try {
            val req = Request.Builder().url(url).get().build()
            downloadClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("download failed HTTP ${resp.code}")
                val body = resp.body ?: error("empty body")
                val totalBytes = body.contentLength()
                dispatchProgress(onProgress, BundleLoadProgress("download", 0))
                if (totalBytes > 0L && onProgress != null) {
                    body.byteStream().buffered().use { input ->
                        tmpZip.outputStream().buffered().use { out ->
                            val buffer = ByteArray(8192)
                            var downloaded = 0L
                            while (true) {
                                val read = input.read(buffer)
                                if (read <= 0) break
                                out.write(buffer, 0, read)
                                downloaded += read
                                val pct = ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 100)
                                dispatchProgress(onProgress, BundleLoadProgress("download", pct))
                            }
                        }
                    }
                } else {
                    tmpZip.outputStream().use { out -> body.byteStream().copyTo(out) }
                    dispatchProgress(onProgress, BundleLoadProgress("download", 100))
                }
            }
            dispatchProgress(onProgress, BundleLoadProgress("unzip", 0))
            unzipToCache(tmpZip, stagingRoot)
            dispatchProgress(onProgress, BundleLoadProgress("unzip", 100))
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
            if (!PAC_MAZE_SKINS_MARKERS.all { resolveFileInRoot(root, it) != null }) return false
            val versionText = resolveFileInRoot(root, "pac_maze_skins/bundle_version.txt")?.readText()?.trim()
            return versionText?.toIntOrNull() == PAC_MAZE_SKINS_BUNDLE_VERSION
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

    private fun unzipToCache(zipFile: File, destRoot: File) {
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            val destCanonical = destRoot.canonicalPath
            while (entry != null) {
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
                        outFile.outputStream().use { zis.copyTo(it) }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

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
