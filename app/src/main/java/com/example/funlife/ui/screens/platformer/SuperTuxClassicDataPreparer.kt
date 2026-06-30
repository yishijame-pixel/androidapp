package com.example.funlife.ui.screens.platformer

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 将 APK assets 内的 `data.zip` 流式解压到 `getExternalFilesDir()/data.zip`，
 * 供 SuperTux 原生引擎挂载（需 patched libsupertux2.so 跳过整包读入内存）。
 */
object SuperTuxClassicDataPreparer {

    private const val TAG = "SuperTuxClassic"
    private const val ASSET_NAME = "data.zip"
    private const val MIN_BYTES = 32L * 1024 * 1024

    fun targetFile(context: Context): File =
        File(context.getExternalFilesDir(null), ASSET_NAME)

    fun isStaged(context: Context): Boolean {
        val file = targetFile(context)
        return file.isFile && file.length() >= MIN_BYTES
    }

    suspend fun ensureStaged(
        context: Context,
        onProgress: (Int) -> Unit = {},
    ): Boolean = withContext(Dispatchers.IO) {
        if (isStaged(context)) {
            onProgress(100)
            return@withContext true
        }
        val dest = targetFile(context)
        dest.parentFile?.mkdirs()
        val tmp = File(dest.parentFile, "$ASSET_NAME.tmp")
        tmp.delete()
        try {
            context.assets.openFd(ASSET_NAME).use { afd ->
                val total = afd.length.coerceAtLeast(1L)
                afd.createInputStream().use { input ->
                    tmp.outputStream().buffered().use { output ->
                        val buffer = ByteArray(256 * 1024)
                        var copied = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            copied += read
                            onProgress(((copied * 100) / total).toInt().coerceIn(0, 99))
                        }
                    }
                }
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
}
