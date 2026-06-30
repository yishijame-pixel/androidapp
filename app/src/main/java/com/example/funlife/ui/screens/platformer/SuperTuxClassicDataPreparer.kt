package com.example.funlife.ui.screens.platformer

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.util.zip.ZipInputStream

/**
 * 将 APK assets 内的 `data.zip` 流式复制到 `getExternalFilesDir()/data.zip`，
 * 并解压到 `getExternalFilesDir()/supertux_data/`，供 SuperTux 原生引擎挂载。
 *
 * 目录挂载比每次 mount 274MB zip 快得多（见 patched libsupertux2.so）。
 */
object SuperTuxClassicDataPreparer {

    private const val TAG = "SuperTuxClassic"
    private const val ASSET_NAME = "data.zip"
    private const val DATA_DIR_NAME = "supertux_data"
    private const val MARKER_NAME = ".extract_complete"
    private const val MIN_BYTES = 32L * 1024 * 1024
    private const val BUFFER_SIZE = 256 * 1024

    data class Progress(val percent: Int, val stage: String)

    @JvmStatic
    fun targetFile(context: Context): File =
        File(context.applicationContext.getExternalFilesDir(null), ASSET_NAME)

    @JvmStatic
    fun dataDir(context: Context): File =
        File(context.applicationContext.getExternalFilesDir(null), DATA_DIR_NAME)

    @JvmStatic
    fun isStaged(context: Context): Boolean {
        val file = targetFile(context)
        return file.isFile && file.length() >= MIN_BYTES
    }

    @JvmStatic
    fun isExtracted(context: Context): Boolean {
        val appContext = context.applicationContext
        val zip = targetFile(appContext)
        val marker = File(dataDir(appContext), MARKER_NAME)
        val images = File(dataDir(appContext), "images")
        if (!marker.isFile || !images.isDirectory) return false
        return marker.readText().trim() == zip.length().toString()
    }

    /** Full prepare: copy zip (0–35%) then extract (35–100%). */
    suspend fun ensureReady(
        context: Context,
        onProgress: (Progress) -> Unit = {},
    ): Boolean = withContext(Dispatchers.IO) {
        if (!ensureStaged(context) { pct ->
                onProgress(Progress(mapStage(pct, 0, 35), stageForStaging(pct)))
            }
        ) {
            return@withContext false
        }
        ensureExtracted(context) { pct ->
            onProgress(Progress(mapStage(pct, 35, 100), stageForExtraction(pct)))
        }
    }

    suspend fun ensureStaged(
        context: Context,
        onProgress: (Int) -> Unit = {},
    ): Boolean = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        if (isStaged(appContext)) {
            onProgress(100)
            return@withContext true
        }
        val dest = targetFile(appContext)
        dest.parentFile?.mkdirs()
        val tmp = File(dest.parentFile, "$ASSET_NAME.tmp")
        tmp.delete()
        try {
            val copied = copyAssetToFile(appContext, tmp, onProgress)
            if (copied < MIN_BYTES) {
                Log.e(TAG, "Staged data.zip too small: $copied bytes")
                tmp.delete()
                return@withContext false
            }
            if (!tmp.renameTo(dest)) {
                tmp.copyTo(dest, overwrite = true)
                tmp.delete()
            }
            onProgress(100)
            Log.i(TAG, "Staged data.zip -> ${dest.absolutePath} (${dest.length()} bytes)")
            dest.isFile && dest.length() >= MIN_BYTES
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stage data.zip", e)
            tmp.delete()
            false
        }
    }

    suspend fun ensureExtracted(
        context: Context,
        onProgress: (Int) -> Unit = {},
    ): Boolean = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        if (isExtracted(appContext)) {
            onProgress(100)
            return@withContext true
        }
        if (!isStaged(appContext)) {
            Log.e(TAG, "Cannot extract — data.zip not staged")
            return@withContext false
        }
        val zip = targetFile(appContext)
        val dest = dataDir(appContext)
        val tmp = File(dest.parentFile, "${DATA_DIR_NAME}.tmp")
        if (tmp.exists()) tmp.deleteRecursively()
        tmp.mkdirs()
        try {
            onProgress(5)
            extractZip(zip, tmp) { inner ->
                onProgress((5 + inner * 0.93).toInt().coerceIn(5, 98))
            }
            if (dest.exists()) dest.deleteRecursively()
            if (!tmp.renameTo(dest)) {
                tmp.copyRecursively(dest, overwrite = true)
                tmp.deleteRecursively()
            }
            File(dest, MARKER_NAME).writeText(zip.length().toString())
            onProgress(100)
            Log.i(TAG, "Extracted data -> ${dest.absolutePath}")
            isExtracted(appContext)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract data.zip", e)
            tmp.deleteRecursively()
            false
        }
    }

    private fun extractZip(
        zipFile: File,
        destDir: File,
        onProgress: (Int) -> Unit,
    ) {
        val destCanonical = destDir.canonicalPath + File.separator
        val totalEntries = runCatching {
            ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zis ->
                var count = 0
                while (zis.nextEntry != null) {
                    count++
                    zis.closeEntry()
                }
                count.coerceAtLeast(1)
            }
        }.getOrDefault(1000)
        var processed = 0
        ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zis ->
            val buffer = ByteArray(BUFFER_SIZE)
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(destDir, entry.name)
                if (!outFile.canonicalPath.startsWith(destCanonical)) {
                    throw SecurityException("Zip slip blocked: ${entry.name}")
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().buffered().use { out ->
                        var read = zis.read(buffer)
                        while (read > 0) {
                            out.write(buffer, 0, read)
                            read = zis.read(buffer)
                        }
                    }
                }
                zis.closeEntry()
                processed++
                onProgress((processed * 100 / totalEntries).coerceIn(0, 100))
                entry = zis.nextEntry
            }
        }
    }

    private fun copyAssetToFile(
        context: Context,
        dest: File,
        onProgress: (Int) -> Unit,
    ): Long {
        try {
            context.assets.openFd(ASSET_NAME).use { afd ->
                val total = afd.length.coerceAtLeast(1L)
                afd.createInputStream().use { input ->
                    return streamCopy(input, dest, total, onProgress)
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "openFd failed for $ASSET_NAME, falling back to stream copy", e)
            context.assets.open(ASSET_NAME).use { input ->
                return streamCopy(input, dest, totalBytes = -1L, onProgress)
            }
        }
    }

    private fun streamCopy(
        input: java.io.InputStream,
        dest: File,
        totalBytes: Long,
        onProgress: (Int) -> Unit,
    ): Long {
        dest.outputStream().buffered().use { output ->
            val buffer = ByteArray(BUFFER_SIZE)
            var copied = 0L
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                output.write(buffer, 0, read)
                copied += read
                if (totalBytes > 0) {
                    onProgress(((copied * 100) / totalBytes).toInt().coerceIn(0, 99))
                }
            }
            output.flush()
            return copied
        }
    }

    private fun mapStage(pct: Int, start: Int, end: Int): Int =
        (start + (pct * (end - start) / 100)).coerceIn(start, end)

    private fun stageForStaging(pct: Int): String = when {
        pct >= 99 -> "资源包复制完成"
        pct >= 50 -> "正在复制游戏资源包… ($pct%)"
        else -> "正在准备 SuperTux 资源包… ($pct%)"
    }

    private fun stageForExtraction(pct: Int): String = when {
        pct >= 99 -> "资源解压完成"
        pct >= 60 -> "正在解压图块与音效… ($pct%)"
        else -> "正在解压游戏资源… ($pct%)"
    }
}
