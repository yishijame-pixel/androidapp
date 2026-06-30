package com.example.funlife.game.platformer.catalog

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntRect
import com.example.funlife.game.platformer.PlatformerCharacterId
import com.example.funlife.game.platformer.catalog.catalogId
import com.example.funlife.game.platformer.PlatformerPlayerSprites
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinAnimCache
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinAnimClip
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * GPU Atlas 纹理缓存：将 clip 全帧合入单张纹理，draw 时用 srcRect 采样，减少逐帧 bitmap 切换。
 */
object PlatformerSpriteAtlasCache {

    private const val TAG = "PlatformerAtlas"
    private const val MAX_ATLAS_EDGE = 4096
    private const val ATLAS_PADDING = 2

    data class AtlasFrame(
        val atlas: ImageBitmap,
        val srcRect: IntRect,
    )

    data class ClipAtlas(
        val bitmap: ImageBitmap,
        val frameRects: List<IntRect>,
    )

    private val clipAtlases = ConcurrentHashMap<String, ClipAtlas>()

    private fun atlasKey(characterId: PlatformerCharacterId, clipName: String): String =
        "${characterId.catalogId}:$clipName"

    fun buildForCharacter(characterId: PlatformerCharacterId) {
        when {
            characterId == PlatformerCharacterId.CHICK_PRO_MAX -> {
                buildChickClip(PacMazeSkinAnimClip.WALK)
                buildChickClip(PacMazeSkinAnimClip.JUMP)
                buildChickClip(PacMazeSkinAnimClip.DIE)
            }
            characterId.isCatalogRemote -> {
                val cfg = PlatformerRemoteAnimCache.config(characterId) ?: return
                cfg.clips.forEach { clip ->
                    buildCatalogClip(characterId, clip)
                }
            }
        }
    }

    fun resolveAtlasFrame(
        characterId: PlatformerCharacterId,
        clipName: String,
        frameIndex: Int,
    ): AtlasFrame? {
        val key = atlasKey(characterId, clipName)
        val atlas = clipAtlases[key] ?: return null
        val rect = atlas.frameRects.getOrNull(frameIndex.coerceIn(0, atlas.frameRects.lastIndex))
            ?: return null
        return AtlasFrame(atlas = atlas.bitmap, srcRect = rect)
    }

    fun invalidateCharacter(characterId: PlatformerCharacterId) {
        val prefix = "${characterId.catalogId}:"
        clipAtlases.keys.filter { it.startsWith(prefix) }.forEach { clipAtlases.remove(it) }
    }

    fun invalidateAll() {
        clipAtlases.clear()
    }

    private fun buildChickClip(clip: PacMazeSkinAnimClip) {
        val skinId = PlatformerPlayerSprites.skinId
        val frames = PacMazeRemoteSkinAnimCache.playbackFrames(skinId, clip) ?: return
        if (frames.size < 2) return
        packFrames(
            key = atlasKey(PlatformerCharacterId.CHICK_PRO_MAX, clip.name.lowercase()),
            frames = frames,
        )
    }

    private fun buildCatalogClip(characterId: PlatformerCharacterId, clip: PlatformerAnimClip) {
        val frames = PlatformerRemoteAnimCache.playbackFrames(characterId.catalogId, clip) ?: return
        if (frames.size < 2) return
        packFrames(
            key = atlasKey(characterId, clip.folder),
            frames = frames,
        )
    }

    private fun packFrames(key: String, frames: List<ImageBitmap>) {
        if (clipAtlases.containsKey(key)) return
        runCatching {
            val maxW = frames.maxOf { it.width }.coerceAtLeast(1)
            val maxH = frames.maxOf { it.height }.coerceAtLeast(1)
            val cols = ceil(sqrt(frames.size.toDouble())).toInt().coerceAtLeast(1)
            val rows = ceil(frames.size.toDouble() / cols).toInt().coerceAtLeast(1)
            val cellW = maxW + ATLAS_PADDING
            val cellH = maxH + ATLAS_PADDING
            val atlasW = (cols * cellW).coerceAtMost(MAX_ATLAS_EDGE)
            val atlasH = (rows * cellH).coerceAtMost(MAX_ATLAS_EDGE)
            if (cols * cellW > MAX_ATLAS_EDGE || rows * cellH > MAX_ATLAS_EDGE) {
                Log.w(TAG, "Atlas too large for $key (${cols}x$rows cells), skipping")
                return
            }
            val bmp = Bitmap.createBitmap(atlasW, atlasH, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            val rects = mutableListOf<IntRect>()
            frames.forEachIndexed { index, frame ->
                val col = index % cols
                val row = index / cols
                val left = col * cellW
                val top = row * cellH
                canvas.drawBitmap(frame.asAndroidBitmap(), left.toFloat(), top.toFloat(), null)
                rects.add(IntRect(left, top, left + frame.width, top + frame.height))
            }
            clipAtlases[key] = ClipAtlas(bitmap = bmp.asImageBitmap(), frameRects = rects)
        }.onFailure { Log.w(TAG, "packFrames failed $key", it) }
    }

}
