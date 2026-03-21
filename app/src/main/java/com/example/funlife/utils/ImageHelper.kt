// ImageHelper.kt - 图片处理工具类
package com.example.funlife.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageHelper {
    private const val TAG = "ImageHelper"
    private const val IMAGES_DIR = "anniversary_images"
    
    /**
     * 将用户选择的图片复制到应用私有目录
     * @param context 上下文
     * @param sourceUri 用户选择的图片URI
     * @return 复制后的文件URI字符串，失败返回null
     */
    fun copyImageToAppStorage(context: Context, sourceUri: Uri): String? {
        try {
            Log.d(TAG, "开始复制图片: $sourceUri")
            
            // 创建应用私有目录
            val imagesDir = File(context.filesDir, IMAGES_DIR)
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
                Log.d(TAG, "创建图片目录: ${imagesDir.absolutePath}")
            }
            
            // 生成唯一文件名
            val fileName = "img_${UUID.randomUUID()}.jpg"
            val destFile = File(imagesDir, fileName)
            
            // 复制文件
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            val resultUri = Uri.fromFile(destFile).toString()
            Log.d(TAG, "图片复制成功: $resultUri")
            return resultUri
            
        } catch (e: Exception) {
            Log.e(TAG, "复制图片失败", e)
            return null
        }
    }
    
    /**
     * 删除图片文件
     * @param imageUri 图片URI字符串
     */
    fun deleteImage(imageUri: String?) {
        if (imageUri.isNullOrEmpty()) return
        
        try {
            val uri = Uri.parse(imageUri)
            val file = File(uri.path ?: return)
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "删除图片: $imageUri")
            }
        } catch (e: Exception) {
            Log.e(TAG, "删除图片失败", e)
        }
    }
    
    /**
     * 清理未使用的图片
     * @param context 上下文
     * @param usedImageUris 正在使用的图片URI列表
     */
    fun cleanupUnusedImages(context: Context, usedImageUris: List<String>) {
        try {
            val imagesDir = File(context.filesDir, IMAGES_DIR)
            if (!imagesDir.exists()) return
            
            val usedFiles = usedImageUris.mapNotNull { uri ->
                try {
                    File(Uri.parse(uri).path ?: "").name
                } catch (e: Exception) {
                    null
                }
            }.toSet()
            
            imagesDir.listFiles()?.forEach { file ->
                if (file.name !in usedFiles) {
                    file.delete()
                    Log.d(TAG, "清理未使用的图片: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "清理图片失败", e)
        }
    }
}
