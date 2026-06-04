// AvatarStorageHelper.kt - 头像存储助手
package com.example.funlife.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * 头像存储助手
 * 负责将用户上传的头像保存到应用内部存储
 */
object AvatarStorageHelper {
    
    private const val TAG = "AvatarStorageHelper"
    private const val AVATAR_DIR = "avatars"
    private const val MAX_IMAGE_SIZE = 1024 // 最大宽高
    private const val JPEG_QUALITY = 85 // JPEG压缩质量
    
    /**
     * 保存头像到内部存储
     * @param context 上下文
     * @param sourceUri 源图片URI（来自相册或相机）
     * @param userId 用户ID
     * @return 保存后的内部存储URI，失败返回null
     */
    fun saveAvatarToInternalStorage(
        context: Context,
        sourceUri: Uri,
        userId: Long
    ): String? {
        return try {
            // 1. 读取源图片
            val inputStream = context.contentResolver.openInputStream(sourceUri)
            if (inputStream == null) {
                Log.e(TAG, "无法打开源图片: $sourceUri")
                return null
            }
            
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (originalBitmap == null) {
                Log.e(TAG, "无法解码图片: $sourceUri")
                return null
            }
            
            // 2. 压缩和缩放图片
            val scaledBitmap = scaleBitmap(originalBitmap, MAX_IMAGE_SIZE)
            
            // 3. 生成唯一文件名
            val fileName = "avatar_${userId}_${UUID.randomUUID()}.jpg"
            
            // 4. 保存到内部存储
            val savedFile = saveToInternalStorage(context, scaledBitmap, fileName)
            
            // 5. 回收Bitmap
            if (scaledBitmap != originalBitmap) {
                scaledBitmap.recycle()
            }
            originalBitmap.recycle()
            
            // 6. 返回内部存储URI
            savedFile?.let { "file://${it.absolutePath}" }
            
        } catch (e: Exception) {
            Log.e(TAG, "保存头像失败", e)
            null
        }
    }
    
    /**
     * 缩放Bitmap到指定最大尺寸
     */
    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // 如果图片已经足够小，直接返回
        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }
        
        // 计算缩放比例
        val scale = if (width > height) {
            maxSize.toFloat() / width
        } else {
            maxSize.toFloat() / height
        }
        
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * 保存Bitmap到内部存储
     */
    private fun saveToInternalStorage(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): File? {
        return try {
            // 创建头像目录
            val avatarDir = File(context.filesDir, AVATAR_DIR)
            if (!avatarDir.exists()) {
                avatarDir.mkdirs()
            }
            
            // 创建文件
            val file = File(avatarDir, fileName)
            
            // 写入文件
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
                outputStream.flush()
            }
            
            Log.d(TAG, "头像保存成功: ${file.absolutePath}")
            file
            
        } catch (e: IOException) {
            Log.e(TAG, "保存文件失败", e)
            null
        }
    }
    
    fun isLocalAvatarUri(avatarUri: String?): Boolean {
        if (avatarUri.isNullOrBlank()) return false
        return avatarUri.startsWith("file://") || avatarUri.startsWith("/")
    }

    /**
     * 返回可用于 Coil 加载的 URI；本地文件不存在或 content URI 不可读时返回 null。
     */
    fun resolveLoadableAvatarUri(context: Context, avatarUri: String?): String? {
        val trimmed = avatarUri?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return when {
            trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            isLocalAvatarUri(trimmed) -> trimmed.takeIf { isAvatarExists(context, trimmed) }
            else -> {
                val readable = runCatching {
                    context.contentResolver.openInputStream(Uri.parse(trimmed))?.use { true } == true
                }.getOrDefault(false)
                trimmed.takeIf { readable }
            }
        }
    }

    fun deleteAvatarFile(context: Context, avatarUri: String?): Boolean {
        if (!isLocalAvatarUri(avatarUri)) return false
        return try {
            val file = File(Uri.parse(avatarUri).path ?: return false)
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            Log.e(TAG, "删除头像文件失败", e)
            false
        }
    }

    /**
     * 删除用户的所有头像
     */
    fun deleteUserAvatars(context: Context, userId: Long) {
        try {
            val avatarDir = File(context.filesDir, AVATAR_DIR)
            if (!avatarDir.exists()) return
            
            val userAvatars = avatarDir.listFiles { file ->
                file.name.startsWith("avatar_${userId}_")
            }
            
            userAvatars?.forEach { file ->
                if (file.delete()) {
                    Log.d(TAG, "删除头像: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "删除用户头像失败", e)
        }
    }
    
    /**
     * 获取头像文件大小（字节）
     */
    fun getAvatarFileSize(context: Context, avatarUri: String?): Long {
        if (avatarUri == null) return 0
        
        return try {
            val file = File(Uri.parse(avatarUri).path ?: return 0)
            if (file.exists()) file.length() else 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * 检查头像文件是否存在
     */
    fun isAvatarExists(context: Context, avatarUri: String?): Boolean {
        if (avatarUri == null) return false
        
        return try {
            val file = File(Uri.parse(avatarUri).path ?: return false)
            file.exists()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取头像目录的总大小
     */
    fun getAvatarDirectorySize(context: Context): Long {
        val avatarDir = File(context.filesDir, AVATAR_DIR)
        if (!avatarDir.exists()) return 0
        
        return avatarDir.listFiles()?.sumOf { it.length() } ?: 0
    }
    
    /**
     * 清理所有头像（慎用）
     */
    fun cleanAllAvatars(context: Context): Boolean {
        return try {
            val avatarDir = File(context.filesDir, AVATAR_DIR)
            if (avatarDir.exists()) {
                avatarDir.deleteRecursively()
                Log.d(TAG, "清理所有头像成功")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "清理所有头像失败", e)
            false
        }
    }
}
