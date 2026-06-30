package com.example.funlife.ui.screens.platformer.minigame

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.funlife.game.platformer.PlatformerCharacterId
import com.example.funlife.game.platformer.catalog.PlatformerAnimClip
import com.example.funlife.game.platformer.catalog.PlatformerRemoteAnimCache
import com.example.funlife.game.platformer.catalog.catalogId
import com.example.funlife.resource.ResourceStore

/** 副玩法素材加载：plane / hillclimb / temple_runner，缺失时返回 null 由 UI 降级绘制。 */
object PlatformerMiniGameAssets {

    data class HillClimbParts(
        val body: ImageBitmap?,
        val frontWheel: ImageBitmap?,
        val backWheel: ImageBitmap?,
    )

    fun loadBitmap(path: String): ImageBitmap? = runCatching {
        ResourceStore.openInputStream(path)?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
    }.getOrNull()

    fun loadPlaneFlyFrames(): List<ImageBitmap> {
        val frames = loadNumberedFrames("platformer_characters/modes/plane/Plane", "Fly", 1, 8)
        if (frames.isNotEmpty()) return frames
        return listOfNotNull(loadBitmap("platformer_characters/modes/plane/Plane/Fly (1).png"))
    }

    fun loadPlaneBulletFrames(): List<ImageBitmap> {
        val frames = loadNumberedFrames("platformer_characters/modes/plane/Bullet", "Bullet", 1, 4)
        if (frames.isNotEmpty()) return frames
        return listOfNotNull(loadBitmap("platformer_characters/modes/plane/Bullet/Bullet (1).png"))
    }

    fun loadPlaneEnemyFrames(): List<ImageBitmap> {
        val frames = loadNumberedFrames("platformer_characters/modes/plane/Enemy", "Enemy", 1, 4)
        if (frames.isNotEmpty()) return frames
        return emptyList()
    }

    fun loadHillClimbParts(): HillClimbParts = HillClimbParts(
        body = loadBitmap("platformer_characters/modes/hillclimb/girl_Body.png")
            ?: loadBitmap("platformer_characters/modes/hillclimb/boy_Body.png"),
        frontWheel = loadBitmap("platformer_characters/modes/hillclimb/girl_FrontWheel.png")
            ?: loadBitmap("platformer_characters/modes/hillclimb/boy_FrontWheel.png"),
        backWheel = loadBitmap("platformer_characters/modes/hillclimb/girl_BackWheel.png")
            ?: loadBitmap("platformer_characters/modes/hillclimb/boy_BackWheel.png"),
    )

    fun templeRunnerFrames(clip: PlatformerAnimClip): List<ImageBitmap> {
        val cfg = PlatformerRemoteAnimCache.config(PlatformerCharacterId.TEMPLE_RUNNER) ?: return emptyList()
        val cached = PlatformerRemoteAnimCache.playbackFrames(PlatformerCharacterId.TEMPLE_RUNNER.catalogId, clip)
        if (!cached.isNullOrEmpty()) return cached
        val folder = when (clip) {
            PlatformerAnimClip.RUN, PlatformerAnimClip.WALK -> "run"
            PlatformerAnimClip.JUMP -> "jump"
            PlatformerAnimClip.SLIDE -> "slide"
            PlatformerAnimClip.IDLE -> "idle"
            PlatformerAnimClip.DIE -> "die"
            else -> "run"
        }
        val prefix = folder
        return loadNumberedFrames("${cfg.assetRoot}/$folder", prefix, 1, 10)
    }

    private fun loadNumberedFrames(basePath: String, prefix: String, from: Int, to: Int): List<ImageBitmap> =
        (from..to).mapNotNull { i ->
            loadBitmap("$basePath/$prefix ($i).png")
                ?: loadBitmap("$basePath/${prefix}_$i.png")
                ?: loadBitmap("$basePath/$prefix$i.png")
        }
}
