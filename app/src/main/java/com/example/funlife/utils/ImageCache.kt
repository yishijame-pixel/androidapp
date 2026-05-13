package com.example.funlife.utils

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

object ImageCache {
    private val cache = mutableMapOf<String, ImageBitmap>()
    
    fun loadImage(context: Context, assetPath: String): ImageBitmap? {
        // 如果已缓存，直接返回
        cache[assetPath]?.let { return it }
        
        // 否则加载并缓存
        return try {
            context.assets.open(assetPath).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)?.asImageBitmap()?.also {
                    cache[assetPath] = it
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    fun clear() {
        cache.clear()
    }
}
