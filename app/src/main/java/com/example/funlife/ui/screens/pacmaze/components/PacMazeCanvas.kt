package com.example.funlife.ui.screens.pacmaze.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.funlife.social.game.engine.pacmaze.PacMazeConstants
import com.example.funlife.social.game.engine.pacmaze.PacMazeEntity
import com.example.funlife.social.game.engine.pacmaze.PacMazeWorldState
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.social.game.engine.pacmaze.PacMazeEntityVisuals
import com.example.funlife.social.game.engine.pacmaze.isPlayerPac
import com.example.funlife.social.game.engine.pacmaze.PacMazeMapMarker
import com.example.funlife.social.game.engine.pacmaze.PacMazeMotion
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeAvatarLoadout
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeCosmeticCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeTrailSampling
import com.example.funlife.ui.screens.pacmaze.cosmetic.trail.PacMazeTrailBuffer
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapRenderContext
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeParticleField
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemeRegistry
import com.example.funlife.viewmodel.PacMazeRenderFrame
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** 地图缩放：对局用 [FIT_MAX]——[PacMazeMapViewport] 统一策略。 */
enum class PacMazeMapScalePolicy {
    FIT_CENTER,
    FIT_MAX,
}

data class PacMazeMapLayoutInsets(
    val top: Dp = 0.dp,
    val bottom: Dp = 0.dp,
    val horizontal: Dp = 0.dp,
)

/** 横/竖走时锁定垂直/水平轨道；插值中（blend<1）保持 fractional 坐标以便转弯平滑。 */
private fun pacMazeRailTravelAnchor(
    world: PacMazeWorldState,
    entity: PacMazeEntity,
    renderX: Float,
    renderY: Float,
    blend: Float,
): Pair<Float, Float> {
    var rx = renderX
    var ry = renderY
    if (entity.isPlayerPac()) {
        when (entity.direction ?: PacMazeEntityVisuals.travelFacing(entity)) {
            Direction.LEFT, Direction.RIGHT -> ry = entity.y
            Direction.UP, Direction.DOWN -> rx = entity.x
            else -> Unit
        }
    }
    return PacMazeMotion.clampRenderAnchor(
        world,
        entity,
        rx,
        ry,
        forGhost = entity.role == "ghost",
    )
}

@Composable
fun PacMazeCanvas(
    modifier: Modifier,
    renderFrame: PacMazeRenderFrame,
    renderBlend: Float = renderFrame.blend,
    displayClockNs: Long = 0L,
    frameDeltaSec: Float = 1f / PacMazeConstants.TICKS_PER_SECOND,
    themeId: PacMazeMapThemeId = PacMazeMapThemeId.CYBERPUNK,
    avatarLoadout: PacMazeAvatarLoadout = PacMazeAvatarLoadout(),
    playerDrawScale: Float = 1f,
    mapWidthScale: Float = PAC_MAZE_MAP_SCALE_DEFAULT,
    mapHeightScale: Float = PAC_MAZE_MAP_SCALE_DEFAULT,
    markers: List<PacMazeMapMarker> = emptyList(),
    levelConfig: com.example.funlife.social.game.engine.pacmaze.PacMazeLevelConfig? = null,
    layoutInsets: PacMazeMapLayoutInsets = PacMazeMapLayoutInsets(),
    scalePolicy: PacMazeMapScalePolicy = PacMazeMapScalePolicy.FIT_CENTER,
    onlineLocalEntityId: String = "",
) {
    val config = remember(themeId) { PacMazeThemeRegistry.configFor(themeId) }
    val particles = remember(themeId) { PacMazeParticleField(config.particles, seed = themeId.ordinal.toLong()) }
    val trailBuffer = remember { PacMazeTrailBuffer(capacity = 32) }
    val displayAnimPhase = remember { DisplayAnimPhase() }
    val displaySmoother = remember { DisplayAnchorSmoother() }

    val world = renderFrame.current
    val previous = renderFrame.previous
    val blend = if (renderFrame.spanDurationNs > 0L) {
        PacMazeRenderTickLoop.displaySpanBlend(
            spanStartNs = renderFrame.spanStartNs,
            spanDurationNs = renderFrame.spanDurationNs,
            displayClockNs = displayClockNs,
            fallbackBlend = renderBlend,
        )
    } else {
        renderBlend
    }.coerceIn(0f, 1f)
    val useAccumInterpolation = previous != null && renderFrame.spanDurationNs <= 0L

    Canvas(modifier = modifier) {
        val clampedFrameDelta = frameDeltaSec.coerceIn(1f / 240f, 1f / 20f)

        fun renderAnchor(entity: PacMazeEntity): Pair<Float, Float> {
            if (onlineLocalEntityId.isNotBlank() && entity.id == onlineLocalEntityId) {
                return pacMazeRailTravelAnchor(world, entity, entity.x, entity.y, blend = 1f)
            }
            val prev = previous?.entities?.firstOrNull { it.id == entity.id }
            val (rx, ry) = PacMazeMotion.renderEntityAnchor(prev, entity, blend)
            val (railX, railY) = pacMazeRailTravelAnchor(world, entity, rx, ry, blend)
            if (entity.isPlayerPac()) {
                val moving = hypot(entity.velX, entity.velY) > 0.01f
                if (useAccumInterpolation || renderFrame.spanDurationNs > 0L) return railX to railY
                return displaySmoother.follow(railX, railY, clampedFrameDelta, moving)
            }
            return railX to railY
        }

        val contentH = (size.height - layoutInsets.top.toPx() - layoutInsets.bottom.toPx())
            .coerceAtLeast(1f)
        val viewport = when (scalePolicy) {
            PacMazeMapScalePolicy.FIT_MAX -> PacMazeMapViewport.computeFitMax(
                canvasWidth = size.width,
                contentHeight = contentH,
                mapGridWidth = world.width,
                mapGridHeight = world.height,
                insetTopPx = layoutInsets.top.toPx(),
                widthScale = mapWidthScale,
                heightScale = mapHeightScale,
            )
            PacMazeMapScalePolicy.FIT_CENTER -> {
                val contentW = (size.width - layoutInsets.horizontal.toPx() * 2f).coerceAtLeast(1f)
                val cell = min(
                    contentW / world.width.toFloat(),
                    contentH / world.height.toFloat(),
                ).coerceAtLeast(8f)
                val baseMapW = cell * world.width
                val baseMapH = cell * world.height
                PacMazeMapViewport.Layout(
                    cell = cell,
                    offsetX = layoutInsets.horizontal.toPx() + (contentW - baseMapW) / 2f,
                    offsetY = layoutInsets.top.toPx() + max(0f, (contentH - baseMapH) / 2f),
                    baseMapW = baseMapW,
                    baseMapH = baseMapH,
                    stretchX = 1f,
                    stretchY = 1f,
                )
            }
        }
        val animPhase = displayAnimPhase.advance(world.tick, clampedFrameDelta)
        particles.advance(clampedFrameDelta)

        val clampedScale = playerDrawScale.coerceIn(PAC_MAZE_PLAYER_SCALE_MIN, PAC_MAZE_PLAYER_SCALE_MAX)
        val tierScale = PacMazeCosmeticCatalog.bodyTier(avatarLoadout.skinId).scaleMul
        // 实体尺寸跟「可见地砖」走：用户拉宽/拉高地图时同步放大角色，避免相对格子越来越小。
        val visualCell = min(viewport.cellX, viewport.cellY)
        val comfort = PacMazeEntityComfortScale.compute(
            baseCellPx = visualCell,
            canvasContentHeightPx = contentH,
            playerUserScale = clampedScale,
            playerTierScale = tierScale,
            densityPxPerDp = density,
        )
        val ctx = PacMazeMapRenderContext(
            world = world,
            previous = previous,
            blend = blend,
            cell = viewport.cell,
            entityCell = comfort.entityCell,
            cellX = viewport.cellX,
            cellY = viewport.cellY,
            offsetX = viewport.offsetX,
            offsetY = viewport.offsetY,
            mapW = viewport.visualMapW,
            mapH = viewport.visualMapH,
            animPhase = animPhase,
            canvasSize = Size(size.width, size.height),
            config = config,
            levelConfig = levelConfig,
            renderAnchor = ::renderAnchor,
            trailSamples = emptyList(),
            markers = markers,
            avatarLoadout = avatarLoadout,
            playerDrawScale = clampedScale,
            entityDrawBoost = comfort.boost,
            minPlayerRadiusPx = comfort.minPlayerRadiusPx,
            minGhostRadiusPx = comfort.minGhostRadiusPx,
            onlineLocalEntityId = onlineLocalEntityId,
        )

        val pacEntities = world.entities.filter { it.isPlayerPac() }
        if (pacEntities.isEmpty()) {
            world.entities.firstOrNull { it.role == "pac" }?.let { pac ->
                pushTrail(world, pac, ctx, trailBuffer)
            }
        } else {
            pacEntities.forEach { pac ->
                pushTrail(world, pac, ctx, trailBuffer)
            }
        }

        val forceCyberTrail = themeId == PacMazeMapThemeId.CYBERPUNK || themeId == PacMazeMapThemeId.ENDLESS
        val renderCtx = if (PacMazeCosmeticCatalog.shouldRenderTrail(avatarLoadout, forceCyberTrail)) {
            ctx.copy(trailSamples = trailBuffer.snapshot())
        } else {
            ctx
        }

        PacMazeThemeRegistry.renderSafe(this, renderCtx, particles, themeId)
    }
}

/** 显示层指数平滑：消化 30fps 下每帧 2 tick 的离散步长。 */
private class DisplayAnchorSmoother {
    private var x = Float.NaN
    private var y = Float.NaN

    fun follow(
        targetX: Float,
        targetY: Float,
        frameDeltaSec: Float,
        moving: Boolean,
    ): Pair<Float, Float> {
        if (!moving) {
            x = targetX
            y = targetY
            return targetX to targetY
        }
        if (x.isNaN()) {
            x = targetX
            y = targetY
            return targetX to targetY
        }
        val t = (1f - exp(-32f * frameDeltaSec)).coerceIn(0.28f, 0.82f)
        x += (targetX - x) * t
        y += (targetY - y) * t
        return x to y
    }
}

/** 行走动画按显示帧推进，避免 30fps 下每帧 2 tick 导致序列帧连跳。 */
private class DisplayAnimPhase {
    private var phase = 0f
    private var lastWorldTick = -1L

    fun advance(worldTick: Long, frameDeltaSec: Float): Float {
        if (worldTick < lastWorldTick) {
            phase = worldTick * PacMazeConstants.ANIM_PHASE_PER_TICK
        }
        lastWorldTick = worldTick
        phase += frameDeltaSec * PacMazeConstants.TICKS_PER_SECOND * PacMazeConstants.ANIM_PHASE_PER_TICK
        return phase
    }
}

private fun pushTrail(
    world: PacMazeWorldState,
    pac: PacMazeEntity,
    ctx: PacMazeMapRenderContext,
    trailBuffer: PacMazeTrailBuffer,
) {
    PacMazeTrailSampling.pushPlayerTrail(
        buffer = trailBuffer,
        ctx = ctx,
        pac = pac,
        powerTicksLeft = world.powerTicksLeft,
    )
}
