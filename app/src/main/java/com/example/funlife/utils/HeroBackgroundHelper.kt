// HeroBackgroundHelper.kt - 个人主页 Hero 区域自定义背景图存储助手
package com.example.funlife.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 个人主页橙色波浪 Hero 区域 - 自定义背景图存储工具
 * - 图片存到 internal storage 的 hero_backgrounds/ 目录
 * - URI 路径用 SharedPreferences 持久化（按 userId）
 */
object HeroBackgroundHelper {

    private const val TAG = "HeroBackgroundHelper"
    private const val DIR = "hero_backgrounds"
    private const val PREF_NAME = "hero_bg_pref"
    private const val MAX_SIZE = 1600
    private const val JPEG_QUALITY = 88

    private fun keyOf(userId: Long) = "hero_bg_uri_$userId"

    /** 读取已保存的背景 URI（不存在则为 null） */
    fun getHeroBackgroundUri(context: Context, userId: Long): String? {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val uri = sp.getString(keyOf(userId), null) ?: return null
        // 校验文件是否还存在
        return try {
            val path = Uri.parse(uri).path
            if (path != null && File(path).exists()) uri else null
        } catch (e: Exception) {
            null
        }
    }

    /** 保存源 URI 的图片到内部存储，并把结果路径写入 SharedPreferences */
    fun saveHeroBackground(context: Context, sourceUri: Uri, userId: Long): String? {
        return try {
            val ins = context.contentResolver.openInputStream(sourceUri) ?: return null
            val original = BitmapFactory.decodeStream(ins)
            ins.close()
            if (original == null) return null

            val scaled = scale(original, MAX_SIZE)

            val dir = File(context.filesDir, DIR).apply { if (!exists()) mkdirs() }
            val fileName = "hero_${userId}_${UUID.randomUUID()}.jpg"
            val file = File(dir, fileName)
            FileOutputStream(file).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                out.flush()
            }
            if (scaled != original) scaled.recycle()
            original.recycle()

            // 清理同用户旧文件
            cleanOld(context, userId, fileName)

            val uri = "file://${file.absolutePath}"
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(keyOf(userId), uri).apply()
            Log.d(TAG, "Hero 背景已保存: $uri")
            uri
        } catch (e: Exception) {
            Log.e(TAG, "保存 Hero 背景失败", e)
            null
        }
    }

    /** 清除当前用户的自定义背景 */
    fun clearHeroBackground(context: Context, userId: Long) {
        try {
            val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            sp.edit().remove(keyOf(userId)).apply()
            val dir = File(context.filesDir, DIR)
            if (dir.exists()) {
                dir.listFiles { f -> f.name.startsWith("hero_${userId}_") }
                    ?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "清除 Hero 背景失败", e)
        }
    }

    private fun scale(bitmap: Bitmap, maxSize: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxSize && h <= maxSize) return bitmap
        val scale = if (w > h) maxSize.toFloat() / w else maxSize.toFloat() / h
        return Bitmap.createScaledBitmap(bitmap, (w * scale).toInt(), (h * scale).toInt(), true)
    }

    private fun cleanOld(context: Context, userId: Long, currentFileName: String) {
        try {
            val dir = File(context.filesDir, DIR)
            if (!dir.exists()) return
            dir.listFiles { f ->
                f.name.startsWith("hero_${userId}_") && f.name != currentFileName
            }?.forEach { it.delete() }
        } catch (_: Exception) {
        }
    }
}
