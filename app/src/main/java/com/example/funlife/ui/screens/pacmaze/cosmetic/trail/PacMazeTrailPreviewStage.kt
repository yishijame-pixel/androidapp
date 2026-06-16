package com.example.funlife.ui.screens.pacmaze.cosmetic.trail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterPose
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterPreviewAnim
import com.example.funlife.ui.screens.pacmaze.character.rememberSpriteWalkFrame
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterStageDecor
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeAvatarLoadout
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeCosmeticCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeIkunCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeTrailId
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinRegistry
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemeRegistry
import com.example.funlife.ui.screens.pacmaze.pacMazeSkinAccent
import com.example.funlife.ui.screens.pacmaze.pacMazeTrailAccent
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * 拖尾展示舞台：角色沿横向 S 形轨迹移动，拖尾采样更密、线宽更大，便于工坊/收藏册预览。
 */
@Composable
fun PacMazeTrailPreviewStage(
    loadout: PacMazeAvatarLoadout,
    modifier: Modifier = Modifier,
    trailId: PacMazeTrailId = loadout.trailId,
    skinId: PacMazeSkinId = loadout.skinId,
    animateWalk: Boolean = true,
    showTrackHint: Boolean = true,
    showBorder: Boolean = true,
    selected: Boolean = true,
) {
    val accent = pacMazeSkinAccent(skinId)
    val trailAccent = pacMazeTrailAccent(trailId)
    var animPhase by remember { mutableFloatStateOf(0f) }
    var t by remember { mutableFloatStateOf(0f) }
    val trailBuffer = remember { PacMazeTrailBuffer(capacity = 32) }
    val powerActive = skinId.hasPowerAura()
    val spriteFrame = rememberSpriteWalkFrame(skinId, animateWalk)
    val effectiveWalk = PacMazeCharacterPreviewAnim.effectiveAnimateWalk(skinId, animateWalk)

    val trailTimeStep = PacMazeCharacterPreviewAnim.trailTimeStep(skinId)
    val orbitScale = when {
        PacMazeIkunCatalog.contains(skinId) -> 0.82f
        PacMazeCharacterPreviewAnim.usesSpriteWalk(skinId) -> 0.72f
        else -> 1f
    }

    LaunchedEffect(animateWalk, trailId, skinId) {
        trailBuffer.reset()
        PacMazeTrailPreviewSamples.stage(
            width = 260f,
            height = 260f,
            phase = t,
            count = 20,
            powerBoost = powerActive,
        ).forEach { sample ->
            trailBuffer.push(sample.position, sample.velocity, sample.powerBoost)
        }
        while (true) {
            delay(16L)
            animPhase += PacMazeCharacterPreviewAnim.walkPhaseStep(skinId, effectiveWalk)
            t += trailTimeStep
            if (effectiveWalk) {
                val nx = 0.5f + sin(t) * 0.36f * orbitScale
                val ny = 0.54f + sin(t * 1.55f + 0.6f) * 0.14f * orbitScale
                trailBuffer.push(
                    position = Offset(nx * 260f, ny * 260f),
                    velocity = Offset(cos(t) * 55f * orbitScale, cos(t * 1.55f) * 18f * orbitScale),
                    powerBoost = powerActive,
                )
            } else {
                trailBuffer.reset()
            }
        }
    }

    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier.then(
            if (showBorder) {
                Modifier
                    .clip(shape)
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = trailAccent.copy(alpha = if (selected) 0.55f else 0.28f),
                        shape = shape,
                    )
            } else {
                Modifier
            },
        ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            PacMazeCharacterStageDecor.drawGroundShadow(this)

            if (showTrackHint) {
                val trackPoints = (0..24).map { i ->
                    val tt = t - (24 - i) * 0.042f
                    Offset(
                        x = (0.5f + sin(tt) * 0.36f) * size.width,
                        y = (0.54f + sin(tt * 1.55f + 0.6f) * 0.14f) * size.height,
                    )
                }
                for (i in 0 until trackPoints.lastIndex) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.06f),
                        start = trackPoints[i],
                        end = trackPoints[i + 1],
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)),
                    )
                }
            }

            val headNx = 0.5f + sin(t) * 0.36f
            val headNy = 0.54f + sin(t * 1.55f + 0.6f) * 0.14f
            val head = Offset(headNx * size.width, headNy * size.height)

            val animatedSamples = trailBuffer.snapshot().map { sample ->
                sample.copy(
                    position = Offset(
                        sample.position.x / 260f * size.width,
                        sample.position.y / 260f * size.height,
                    ),
                )
            }
            val samples = if (trailId == PacMazeTrailId.NONE) {
                emptyList()
            } else if (animatedSamples.size >= 3) {
                animatedSamples
            } else {
                PacMazeTrailPreviewSamples.stage(
                    width = size.width,
                    height = size.height,
                    phase = t,
                    count = 20,
                    powerBoost = powerActive,
                )
            }

            val cell = size.minDimension * 0.2f
            val config = PacMazeThemeRegistry.configFor(PacMazeMapThemeId.CLASSIC)
            PacMazeTrailRegistry.draw(
                scope = this,
                trailId = trailId,
                samples = samples,
                palette = config.palette,
                cell = cell,
                powerActive = powerActive,
            )

            val tier = PacMazeCosmeticCatalog.bodyTier(skinId).scaleMul
            val baseRadiusFrac = if (PacMazeIkunCatalog.contains(skinId)) 0.19f else 0.11f
            val r = size.minDimension * baseRadiusFrac * tier * PacMazeCharacterPreviewAnim.previewDrawRadiusMul(skinId)
            val corridorPx = size.minDimension * if (PacMazeIkunCatalog.contains(skinId)) 1.35f else 1f
            PacMazeSkinRegistry.draw(
                scope = this,
                skinId = skinId,
                center = head,
                radius = r,
                pose = PacMazeCharacterPose(
                    facing = if (cos(t) >= 0) Direction.RIGHT else Direction.LEFT,
                    animPhase = animPhase,
                    isMoving = effectiveWalk,
                    powerActive = powerActive,
                    walkPreview = true,
                    spriteFrameOverride = spriteFrame,
                ),
                themeId = PacMazeMapThemeId.CLASSIC,
                palette = config.palette,
                corridorCellPx = corridorPx,
            )
        }
    }
}

/** 列表项用迷你拖尾条带预览 */
@Composable
fun PacMazeTrailSwatch(
    trailId: PacMazeTrailId,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    val accent = pacMazeTrailAccent(trailId)
    var t by remember { mutableFloatStateOf(0f) }
    val trailBuffer = remember { PacMazeTrailBuffer(capacity = 18) }

    LaunchedEffect(trailId, animate) {
        trailBuffer.reset()
        if (!animate) return@LaunchedEffect
        PacMazeTrailPreviewSamples.swatch(
            width = 160f,
            height = 80f,
            phase = t,
        ).forEach { sample ->
            trailBuffer.push(sample.position, sample.velocity, sample.powerBoost)
        }
        while (true) {
            delay(16L)
            t += 0.05f
            trailBuffer.push(
                position = Offset(80f + sin(t) * 28f, 40f + cos(t * 1.3f) * 6f),
                velocity = Offset(cos(t) * 40f, 0f),
                powerBoost = false,
            )
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(8.dp)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val animatedSamples = trailBuffer.snapshot().map { sample ->
                sample.copy(
                    position = Offset(
                        sample.position.x / 160f * size.width,
                        sample.position.y / 80f * size.height,
                    ),
                )
            }
            val samples = when {
                trailId == PacMazeTrailId.NONE -> emptyList()
                animate && animatedSamples.size >= 3 -> animatedSamples
                else -> PacMazeTrailPreviewSamples.swatch(size.width, size.height, phase = t)
            }
            val config = PacMazeThemeRegistry.configFor(PacMazeMapThemeId.CLASSIC)
            if (samples.isNotEmpty()) {
                PacMazeTrailRegistry.draw(
                    scope = this,
                    trailId = trailId,
                    samples = samples,
                    palette = config.palette,
                    cell = size.minDimension * 0.28f,
                    powerActive = false,
                )
            } else if (trailId == PacMazeTrailId.NONE) {
                drawLine(
                    color = accent.copy(alpha = 0.35f),
                    start = Offset(size.width * 0.22f, size.height * 0.5f),
                    end = Offset(size.width * 0.78f, size.height * 0.5f),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                )
            }
        }
    }
}
