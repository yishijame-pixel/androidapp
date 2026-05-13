package com.example.funlife.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.DrawableRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * 图片保存工具类
 * 用于将图片保存到手机相册
 */
object ImageSaveHelper {
    
    /**
     * 保存Drawable资源图片到相册
     * @param context 上下文
     * @param drawableId Drawable资源ID
     * @param fileName 保存的文件名
     * @return 是否保存成功
     */
    suspend fun saveDrawableToGallery(
        context: Context,
        @DrawableRes drawableId: Int,
        fileName: String = "wechat_pay_qr_${System.currentTimeMillis()}.png"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 从资源加载Bitmap
            val bitmap = BitmapFactory.decodeResource(context.resources, drawableId)
            
            // 保存到相册
            saveBitmapToGalleryInternal(context, bitmap, fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 保存Bitmap到相册
     * @param context 上下文
     * @param bitmap 要保存的Bitmap
     * @param fileName 保存的文件名
     * @return 是否保存成功
     */
    suspend fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            saveBitmapToGalleryInternal(context, bitmap, fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 保存Bitmap到相册（内部实现）
     */
    private fun saveBitmapToGalleryInternal(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): Boolean {
        return try {
            val outputStream: OutputStream?
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10及以上使用MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FunLife")
                }
                
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                
                outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
            } else {
                // Android 9及以下使用传统方式
                val imagesDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                ).toString() + "/FunLife"
                
                val dir = File(imagesDir)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                
                val file = File(dir, fileName)
                outputStream = FileOutputStream(file)
                
                // 通知系统扫描新文件
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, file.absolutePath)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                }
                context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                )
            }
            
            outputStream?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                it.flush()
            }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
