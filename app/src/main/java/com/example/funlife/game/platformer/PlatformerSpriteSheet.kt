package com.example.funlife.game.platformer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** 从 Pixel Adventure 式命名条带图（如 Run (36x30).png）切帧。 */
data class SpriteStrip(
    val frames: List<ImageBitmap>,
    val frameWidth: Int,
    val frameHeight: Int,
)

object PlatformerSpriteSheet {

    private val sizeInName = Regex("""(\d+)x(\d+)""")

    fun loadStrip(context: Context, assetPath: String): SpriteStrip? = runCatching {
        val bmp = context.assets.open(assetPath).use { BitmapFactory.decodeStream(it) } ?: return null
        val match = sizeInName.find(assetPath)
        val fw = match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: bmp.width
        val fh = match?.groupValues?.getOrNull(2)?.toIntOrNull() ?: bmp.height
        val count = (bmp.width / fw).coerceAtLeast(1)
        val frames = (0 until count).mapNotNull { i ->
            val left = i * fw
            if (left + fw > bmp.width) return@mapNotNull null
            Bitmap.createBitmap(bmp, left, 0, fw, fh.coerceAtMost(bmp.height)).asImageBitmap()
        }
        if (frames.isEmpty()) return null
        SpriteStrip(frames, fw, fh)
    }.getOrNull()

    fun fromBitmaps(frames: List<ImageBitmap>): SpriteStrip? {
        if (frames.isEmpty()) return null
        return SpriteStrip(frames, frames.first().width, frames.first().height)
    }

    fun loadSequence(context: Context, paths: List<String>): SpriteStrip? {
        val frames = paths.mapNotNull { path ->
            runCatching {
                context.assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            }.getOrNull()
        }
        if (frames.isEmpty()) return null
        val fw = frames.first().width
        val fh = frames.first().height
        return SpriteStrip(frames, fw, fh)
    }
}
