package com.example.funlife.resource

import android.content.Context
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

    private val CLOUD_ROOTS = setOf("xiangkuang", "pet", "login", "renge", "dibu", "wheel")

    private lateinit var appContext: Context
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bundleMutex = ConcurrentHashMap<String, Mutex>()

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

    private fun cacheRoot(): File =
        File(appContext.filesDir, "resource_cache").also { it.mkdirs() }

    fun isCloudResource(path: String): Boolean {
        val root = path.substringBefore('/').trim()
        return root in CLOUD_ROOTS
    }

    fun resolveFile(relativePath: String): File? {
        val normalized = normalizePath(relativePath) ?: return null
        val cached = File(cacheRoot(), normalized)
        return cached.takeIf { it.isFile }
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

    /** 启动时：拉 manifest，WiFi 下预下载 login + dibu */
    suspend fun syncAndPrefetchOnLaunch() = withContext(Dispatchers.IO) {
        if (BuildConfig.VIP_BACKEND_URL.isBlank()) return@withContext
        runCatching { refreshManifest() }
            .onFailure { Log.w(TAG, "manifest sync failed: ${it.message}") }
        if (!isOnWifi()) return@withContext
        listOf("login", "dibu").forEach { id ->
            runCatching { ensureBundle(id) }
                .onFailure { Log.w(TAG, "prefetch $id failed: ${it.message}") }
        }
    }

    suspend fun ensureBundle(bundleId: String): Boolean = withContext(Dispatchers.IO) {
        val mutex = bundleMutex.getOrPut(bundleId) { Mutex() }
        mutex.withLock {
            val manifest = fetchManifest() ?: return@withLock false
            val bundle = manifest.bundles.firstOrNull { it.id == bundleId } ?: return@withLock false
            if (isBundleReady(bundle.targetDir) && prefs().getInt(KEY_MANIFEST_VERSION, 0) >= manifest.version) {
                return@withLock true
            }
            val url = bundle.url?.takeIf { it.isNotBlank() }
                ?: signBundleUrl(bundleId)?.url
                ?: return@withLock false
            downloadAndUnzip(url, bundle.targetDir)
            prefs().edit()
                .putInt(KEY_MANIFEST_VERSION, manifest.version)
                .putBoolean("bundle_$bundleId", true)
                .apply()
            true
        }
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

    private fun isBundleReady(targetDir: String): Boolean {
        val dir = File(cacheRoot(), targetDir)
        return dir.isDirectory && dir.list()?.isNotEmpty() == true
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
            val text = resp.body?.string().orEmpty()
            if (text.isBlank()) return null
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val root = gson.fromJson<Map<String, Any>>(text, mapType) ?: return null
            if (root["ok"] != true) return null
            return gson.fromJson(text, AssetManifestResponse::class.java)
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
            val text = resp.body?.string().orEmpty()
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val root = gson.fromJson<Map<String, Any>>(text, mapType) ?: return null
            if (root["ok"] != true) return null
            @Suppress("UNCHECKED_CAST")
            return SignResult(root["url"] as? String)
        }
    }

    private fun downloadAndUnzip(url: String, targetDir: String) {
        val tmpZip = File(appContext.cacheDir, "bundle_${targetDir}_${System.currentTimeMillis()}.zip")
        val req = Request.Builder().url(url).get().build()
        downloadClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("download failed HTTP ${resp.code}")
            val body = resp.body ?: error("empty body")
            tmpZip.outputStream().use { out -> body.byteStream().copyTo(out) }
        }
        unzipToCache(tmpZip, cacheRoot())
        tmpZip.delete()
        Log.i(TAG, "bundle ready: $targetDir")
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
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { zis.copyTo(it) }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
