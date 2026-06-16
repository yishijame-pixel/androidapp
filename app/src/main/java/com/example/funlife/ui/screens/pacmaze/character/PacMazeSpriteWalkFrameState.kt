package com.example.funlife.ui.screens.pacmaze.character

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSpriteWalkAnim
import kotlinx.coroutines.delay

/** 序列帧皮肤预览：独立计时逐步播放帧序列。 */
@Composable
fun rememberSpriteWalkFrame(skinId: PacMazeSkinId, animateWalk: Boolean): Int? {
    if (PacMazeCharacterPreviewAnim.usesRemoteAnim(skinId)) {
        var frame by remember(skinId) { mutableIntStateOf(0) }
        val playing = PacMazeCharacterPreviewAnim.effectiveAnimateWalk(skinId, animateWalk)
        LaunchedEffect(skinId, playing) {
            frame = 0
            if (!playing) return@LaunchedEffect
            while (true) {
                delay(80L)
                frame = (frame + 1) % 61
            }
        }
        return if (playing) frame else 0
    }
    if (!PacMazeCharacterPreviewAnim.usesSpriteWalk(skinId)) return null
    var frame by remember(skinId) { mutableIntStateOf(0) }
    val playing = PacMazeCharacterPreviewAnim.effectiveAnimateWalk(skinId, animateWalk)
    LaunchedEffect(skinId, playing) {
        frame = 0
        if (!playing) return@LaunchedEffect
        while (true) {
            delay(PacMazeCharacterPreviewAnim.SPRITE_FRAME_HOLD_MS)
            frame = (frame + 1) % PacMazeSpriteWalkAnim.FRAME_COUNT
        }
    }
    return if (playing) frame else 0
}
