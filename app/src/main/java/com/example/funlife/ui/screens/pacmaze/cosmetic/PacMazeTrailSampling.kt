package com.example.funlife.ui.screens.pacmaze.cosmetic

import androidx.compose.ui.geometry.Offset
import com.example.funlife.social.game.engine.pacmaze.PacMazeEntity
import com.example.funlife.social.game.engine.pacmaze.PacMazeEntityVisuals
import com.example.funlife.ui.screens.pacmaze.cosmetic.trail.PacMazeTrailBuffer
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapRenderContext
import kotlin.math.hypot

internal object PacMazeTrailSampling {

    fun pushPlayerTrail(
        buffer: PacMazeTrailBuffer,
        ctx: PacMazeMapRenderContext,
        pac: PacMazeEntity,
        powerTicksLeft: Int,
    ) {
        if (!PacMazePlayerCosmeticDraw.isTrailMotionActive(pac)) {
            buffer.reset()
            return
        }
        val skinId = ctx.avatarLoadout.skinId
        val visualCenter = ctx.playerDrawCenter(pac, skinId)
        val visualCell = kotlin.math.min(ctx.cellX, ctx.cellY)
        val radius = PacMazeCosmeticCatalog.visualRadius(
            cell = visualCell,
            loadout = ctx.avatarLoadout,
            userDrawScale = ctx.playerDrawScale,
            entityDrawBoost = ctx.entityDrawBoost,
            minRadiusPx = ctx.minPlayerRadiusPx,
        )
        val trailDepth = PacMazeEntityVisuals.trailRearAttachDepthPx(radius, visualCell)
        val travelFacing = PacMazeEntityVisuals.travelFacing(pac)
        val (ox, oy) = PacMazeEntityVisuals.trailAnchorOffset(
            velX = pac.velX,
            velY = pac.velY,
            fallbackFacing = travelFacing,
            cellX = ctx.cellX,
            cellY = ctx.cellY,
            trailDepthPx = trailDepth,
        )
        val trailPos = Offset(visualCenter.x + ox, visualCenter.y + oy)
        val screenVel = Offset(pac.velX * ctx.cellX, pac.velY * ctx.cellY)
        val minStep = visualCell * 0.06f
        buffer.push(
            position = trailPos,
            velocity = screenVel,
            powerBoost = powerTicksLeft > 0,
            minStepPx = minStep,
        )
    }
}
