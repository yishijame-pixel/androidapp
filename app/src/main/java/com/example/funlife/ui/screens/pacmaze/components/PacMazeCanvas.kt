package com.example.funlife.ui.screens.pacmaze.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.funlife.social.game.engine.pacmaze.PacMazeEntity
import com.example.funlife.social.game.engine.pacmaze.PacMazeMapMarker
import com.example.funlife.social.game.engine.pacmaze.PacMazeMotion
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapRenderContext
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeParticleField
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazePlayerTrail
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemeRegistry
import com.example.funlife.viewmodel.PacMazeRenderFrame
import kotlin.math.min

private const val MAP_HEIGHT_USAGE = 0.98f
private const val MAP_WIDTH_USAGE = 0.96f
private const val THEME_MAP_SCALE_BOOST = 1.12f

/**
 * 地图渲染入口：逻辑格 → 主题配置 → 主题渲染器。
 */
@Composable
fun PacMazeCanvas(
    modifier: Modifier,
    renderFrame: PacMazeRenderFrame,
    themeId: PacMazeMapThemeId = PacMazeMapThemeId.CYBERPUNK,
    playerCharacterId: PacMazeCharacterId = PacMazeCharacterId.CLASSIC_PAC,
    playerDrawScale: Float = 1f,
    markers: List<PacMazeMapMarker> = emptyList(),
) {
    val config = remember(themeId) { PacMazeThemeRegistry.configFor(themeId) }
    val particles = remember(themeId) { PacMazeParticleField(config.particles, seed = themeId.ordinal.toLong()) }
    val playerTrail = remember { PacMazePlayerTrail(capacity = 14) }

    val world = renderFrame.current
    val blend = PacMazeMotion.smoothBlend(renderFrame.blend)
    val previous = renderFrame.previous

    fun renderAnchor(entity: PacMazeEntity): Pair<Float, Float> {
        val prev = previous?.entities?.firstOrNull { it.id == entity.id }
        if (prev == null || blend >= 1f) return entity.x to entity.y
        return PacMazeMotion.lerpAnchor(prev.x, entity.x, blend) to
            PacMazeMotion.lerpAnchor(prev.y, entity.y, blend)
    }

    Canvas(modifier = modifier) {
        val heightCell = size.height * MAP_HEIGHT_USAGE / world.height
        val widthCell = size.width * MAP_WIDTH_USAGE / world.width
        var cell = min(heightCell, widthCell).coerceAtLeast(8f)
        if (themeId != PacMazeMapThemeId.CLASSIC) {
            val boost = if (themeId == PacMazeMapThemeId.CYBERPUNK) 1.16f else THEME_MAP_SCALE_BOOST
            cell = min(heightCell * boost, widthCell * 1.06f)
        }
        val mapW = cell * world.width
        val mapH = cell * world.height
        val offsetX = (size.width - mapW) / 2f
        val offsetY = (size.height - mapH) / 2f
        val animPhase = world.tick * 0.18f

        particles.advance(1f / 60f)

        world.entities.firstOrNull { it.role == "pac" }?.let { pac ->
            val (ax, ay) = renderAnchor(pac)
            playerTrail.push(Offset(offsetX + (ax + 0.5f) * cell, offsetY + (ay + 0.5f) * cell))
        }

        val ctx = PacMazeMapRenderContext(
            world = world,
            previous = previous,
            blend = blend,
            cell = cell,
            offsetX = offsetX,
            offsetY = offsetY,
            mapW = mapW,
            mapH = mapH,
            animPhase = animPhase,
            canvasSize = Size(size.width, size.height),
            config = config,
            renderAnchor = ::renderAnchor,
            playerTrail = if (
                themeId == PacMazeMapThemeId.CYBERPUNK ||
                playerCharacterId == PacMazeCharacterId.DATA_CORE
            ) {
                playerTrail.snapshot()
            } else {
                emptyList()
            },
            markers = markers,
            playerCharacterId = playerCharacterId,
            playerDrawScale = playerDrawScale.coerceIn(0.5f, 1.5f),
        )
        PacMazeThemeRegistry.renderSafe(this, ctx, particles, themeId)
    }
}
