package com.example.funlife.ui.screens.pacmaze.character

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.ui.screens.pacmaze.PacMazePalette
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeAvatarLoadout
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeCosmeticCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeBitmapWalkCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinAnimCache
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinAnimCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinLoadOverlay
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.RemoteSkinLoadMode
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinAssetCache
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinPreviewBitmapDraw
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinRegistry
import com.example.funlife.ui.screens.pacmaze.cosmetic.trail.PacMazeTrailBuffer
import com.example.funlife.ui.screens.pacmaze.cosmetic.trail.PacMazeTrailRegistry
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemeRegistry
import com.example.funlife.ui.screens.pacmaze.pacMazeSkinAccent
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
private fun rememberRemotePreviewLoadMode(
    skinId: PacMazeSkinId,
    selected: Boolean,
    forceCoverOnly: Boolean = false,
): RemoteSkinLoadMode {
    if (!PacMazeRemoteSkinAnimCatalog.usesRemoteAnim(skinId)) {
        return RemoteSkinLoadMode.FullAnimation
    }
    if (forceCoverOnly) return RemoteSkinLoadMode.CoverOnly
    val animReady = PacMazeRemoteSkinAnimCache.isReady(skinId)
    LaunchedEffect(skinId, selected, animReady) {
        if (selected && !animReady) {
            PacMazeRemoteSkinAnimCache.requestPreloadAsync(skinId)
        }
    }
    return when {
        !selected -> RemoteSkinLoadMode.CoverOnly
        animReady -> RemoteSkinLoadMode.FullAnimation
        else -> RemoteSkinLoadMode.CoverOnly
    }
}

@Composable
private fun remotePreviewPreferCover(
    skinId: PacMazeSkinId,
    selected: Boolean,
    forceCoverOnly: Boolean = false,
): Boolean {
    if (forceCoverOnly) return true
    if (!PacMazeRemoteSkinAnimCatalog.usesRemoteAnim(skinId)) return false
    return !selected || !PacMazeRemoteSkinAnimCache.isReady(skinId)
}

@Composable
fun PacMazeCharacterPreview(
    characterId: PacMazeCharacterId,
    modifier: Modifier = Modifier,
    animateWalk: Boolean = true,
    facing: Direction = Direction.RIGHT,
    powerActive: Boolean = false,
    selected: Boolean = false,
) = PacMazeCharacterPreview(
    skinId = PacMazeSkinId.fromLegacy(characterId),
    modifier = modifier,
    animateWalk = animateWalk,
    facing = facing,
    powerActive = powerActive,
    selected = selected,
)

@Composable
fun PacMazeCharacterPreview(
    skinId: PacMazeSkinId,
    modifier: Modifier = Modifier,
    animateWalk: Boolean = true,
    facing: Direction = Direction.RIGHT,
    powerActive: Boolean = false,
    selected: Boolean = false,
    loadout: PacMazeAvatarLoadout = PacMazeAvatarLoadout(skinId = skinId),
) {
    var animPhase by remember { mutableFloatStateOf(0f) }
    val spriteFrame = rememberSpriteWalkFrame(skinId, animateWalk)
    val effectiveWalk = PacMazeCharacterPreviewAnim.effectiveAnimateWalk(skinId, animateWalk)
    LaunchedEffect(effectiveWalk, skinId) {
        while (true) {
            delay(16L)
            animPhase += PacMazeCharacterPreviewAnim.walkPhaseStep(skinId, effectiveWalk)
        }
    }

    val drawFacing = if (PacMazeCharacterPreviewAnim.usesLeftPreviewFacing(skinId)) {
        PacMazeCharacterPreviewAnim.previewFacing(skinId)
    } else {
        facing
    }

    val remoteLoadMode = rememberRemotePreviewLoadMode(skinId, selected)
    val preferCoverOnly = remotePreviewPreferCover(skinId, selected)

    PacMazeRemoteSkinLoadOverlay(
        skinId = skinId,
        modifier = modifier,
        loadMode = remoteLoadMode,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (selected) {
                        Modifier.border(2.dp, PacMazePalette.accentGold, CircleShape)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize(if (PacMazeBitmapWalkCatalog.contains(skinId)) 0.96f else 0.85f)) {
                val config = PacMazeThemeRegistry.configFor(PacMazeMapThemeId.CLASSIC)
                val pose = PacMazeCharacterPose(
                    facing = drawFacing,
                    animPhase = animPhase,
                    isMoving = effectiveWalk,
                    powerActive = powerActive,
                    walkPreview = true,
                    spriteFrameOverride = spriteFrame,
                    preferCoverOnly = preferCoverOnly,
                )
                if (PacMazeSkinPreviewBitmapDraw.shouldUse(skinId)) {
                    val c = Offset(
                        size.width / 2f,
                        size.height * PacMazeSkinPreviewBitmapDraw.centerYFrac(PacMazeSkinPreviewBitmapDraw.Slot.Chip),
                    )
                    PacMazeSkinPreviewBitmapDraw.drawFit(
                        scope = this,
                        skinId = skinId,
                        boxWidth = size.width,
                        boxHeight = size.height,
                        center = c,
                        facing = drawFacing,
                        pose = pose,
                        slot = PacMazeSkinPreviewBitmapDraw.Slot.Chip,
                    )
                } else {
                    val tier = PacMazeCosmeticCatalog.bodyTier(skinId).scaleMul
                    val baseMul = 0.34f
                    val r = size.minDimension * baseMul * tier
                    val c = Offset(size.width / 2f, size.height * 0.5f)
                    PacMazeSkinRegistry.draw(
                        scope = this,
                        skinId = skinId,
                        center = c,
                        radius = r,
                        pose = pose,
                        themeId = PacMazeMapThemeId.CLASSIC,
                        palette = config.palette,
                        corridorCellPx = size.minDimension,
                    )
                }
            }
        }
    }
}

/** 选角卡片专用：主题化舞台 + 拖尾预览 */
@Composable
fun PacMazeCharacterStagePreview(
    skinId: PacMazeSkinId,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    animateWalk: Boolean = true,
    powerActive: Boolean = false,
    loadout: PacMazeAvatarLoadout = PacMazeAvatarLoadout(skinId = skinId),
    backdrop: PacMazeStageBackdrop = PacMazeStageBackdrop.Clean,
    liteMode: Boolean = false,
) {
    val accent = pacMazeSkinAccent(skinId)
    val context = LocalContext.current
    var animPhase by remember { mutableFloatStateOf(0f) }
    val trailBuffer = remember { PacMazeTrailBuffer(capacity = 16) }
    val spriteFrame = rememberSpriteWalkFrame(skinId, animateWalk)
    val effectiveWalk = PacMazeCharacterPreviewAnim.effectiveAnimateWalk(skinId, animateWalk)
    val orbitScale = PacMazeCharacterPreviewAnim.trailOrbitScale(skinId)
    LaunchedEffect(Unit) {
        PacMazeSkinAssetCache.ensureLoaded(context)
    }
    LaunchedEffect(animateWalk, skinId, loadout.trailId, liteMode) {
        if (liteMode) return@LaunchedEffect
        trailBuffer.reset()
        var t = 0f
        while (true) {
            delay(16L)
            animPhase += PacMazeCharacterPreviewAnim.walkPhaseStep(skinId, effectiveWalk)
            t += PacMazeCharacterPreviewAnim.trailTimeStep(skinId)
            if (effectiveWalk) {
                if (skinId.isOcean()) {
                    val glideX = t * 3.5f
                    val glideY = sin(t * 0.55f) * 5f
                    trailBuffer.push(
                        position = Offset(120f + glideX, 120f + glideY),
                        velocity = Offset(18f, cos(t * 0.55f) * 8f),
                        powerBoost = powerActive,
                    )
                } else {
                    val orbitX = cos(t) * 18f * orbitScale
                    val orbitY = sin(t * 0.8f) * 8f * orbitScale
                    trailBuffer.push(
                        position = Offset(120f + orbitX, 120f + orbitY),
                        velocity = Offset(-sin(t) * 40f * orbitScale, cos(t * 0.8f) * 20f * orbitScale),
                        powerBoost = powerActive,
                    )
                }
            } else {
                trailBuffer.reset()
            }
        }
    }
    LaunchedEffect(effectiveWalk, skinId, liteMode) {
        if (liteMode) {
            while (true) {
                delay(if (effectiveWalk) 33L else 120L)
                if (effectiveWalk) {
                    animPhase += PacMazeCharacterPreviewAnim.walkPhaseStep(skinId, true)
                }
            }
        }
    }

    val stageShape = RoundedCornerShape(16.dp)
    val forceCoverOnly = liteMode && PacMazeRemoteSkinAnimCatalog.usesRemoteAnim(skinId)
    val remoteLoadMode = rememberRemotePreviewLoadMode(skinId, selected, forceCoverOnly)
    val preferCoverOnly = remotePreviewPreferCover(skinId, selected, forceCoverOnly)

    PacMazeRemoteSkinLoadOverlay(
        skinId = skinId,
        modifier = modifier,
        loadMode = remoteLoadMode,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (backdrop == PacMazeStageBackdrop.Decorated) {
                        Modifier
                            .clip(stageShape)
                            .background(
                                PacMazeCharacterStageDecor.stageBackground(skinId, selected, accent, backdrop),
                            )
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) accent.copy(alpha = 0.65f) else PacMazePalette.cardBorder,
                                shape = stageShape,
                            )
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (!liteMode && backdrop != PacMazeStageBackdrop.Clean) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    PacMazeCharacterStageDecor.drawBackdrop(this, skinId, accent, selected, animPhase, backdrop)
                }
            }
            Canvas(
                modifier = Modifier.fillMaxSize(
                    if (PacMazeSkinPreviewBitmapDraw.shouldUse(skinId)) {
                        if (liteMode) 0.88f else 0.92f
                    } else {
                        PacMazeCharacterPreviewAnim.previewCanvasFill(skinId, liteMode)
                    },
                ),
            ) {
                val tier = PacMazeCosmeticCatalog.bodyTier(skinId).scaleMul
                val config = PacMazeThemeRegistry.configFor(PacMazeMapThemeId.CLASSIC)
                val slot = if (liteMode) {
                    PacMazeSkinPreviewBitmapDraw.Slot.GridCell
                } else {
                    PacMazeSkinPreviewBitmapDraw.Slot.Stage
                }
                val drawFacing = PacMazeCharacterPreviewAnim.previewFacing(skinId)
                val pose = PacMazeCharacterPose(
                    facing = drawFacing,
                    animPhase = animPhase,
                    isMoving = effectiveWalk,
                    powerActive = powerActive,
                    walkPreview = true,
                    spriteFrameOverride = spriteFrame,
                    preferCoverOnly = preferCoverOnly,
                )
                val c = Offset(
                    size.width / 2f,
                    size.height * if (PacMazeSkinPreviewBitmapDraw.shouldUse(skinId)) {
                        PacMazeSkinPreviewBitmapDraw.centerYFrac(slot)
                    } else {
                        PacMazeCharacterPreviewAnim.previewCenterYFrac(skinId, liteMode)
                    },
                )
                if (PacMazeSkinPreviewBitmapDraw.shouldUse(skinId)) {
                    PacMazeSkinPreviewBitmapDraw.drawFit(
                        scope = this,
                        skinId = skinId,
                        boxWidth = size.width,
                        boxHeight = size.height,
                        center = c,
                        facing = drawFacing,
                        pose = pose,
                        slot = slot,
                    )
                } else {
                    val baseMul = PacMazeCharacterPreviewAnim.previewBaseRadiusMul(skinId, liteMode)
                    val r = size.minDimension * baseMul * tier *
                        PacMazeCharacterPreviewAnim.previewDrawRadiusMul(skinId, liteMode)
                    if (!liteMode) {
                        val samples = trailBuffer.snapshot().map { sample ->
                            sample.copy(
                                position = Offset(
                                    c.x + (sample.position.x - 120f) * (size.width / 240f),
                                    c.y + (sample.position.y - 120f) * (size.height / 240f),
                                ),
                            )
                        }
                        PacMazeTrailRegistry.draw(
                            scope = this,
                            trailId = loadout.trailId,
                            samples = samples,
                            palette = config.palette,
                            cell = size.minDimension * 0.12f,
                            powerActive = powerActive,
                        )
                    }
                    PacMazeSkinRegistry.draw(
                        scope = this,
                        skinId = skinId,
                        center = c,
                        radius = r,
                        pose = pose,
                        themeId = PacMazeMapThemeId.CLASSIC,
                        palette = config.palette,
                        corridorCellPx = size.minDimension,
                    )
                }
            }
        }
    }
}
