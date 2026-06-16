package com.example.funlife.ui.screens.pacmaze.maptheme



import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.drawscope.DrawScope

import com.example.funlife.social.game.engine.pacmaze.PacMazeMarkerKind
import com.example.funlife.social.game.engine.pacmaze.primaryPac

import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeExploration

import kotlin.math.sin



internal object PacMazeModeOverlayDraw {



    fun drawEndlessVignette(scope: DrawScope, ctx: PacMazeMapRenderContext) {

        val pulse = 0.55f + 0.12f * sin(ctx.animPhase * 1.4f)

        scope.drawRect(

            brush = Brush.radialGradient(

                colors = listOf(

                    Color(0xFF7C4DFF).copy(alpha = 0.08f * pulse),

                    Color.Transparent,

                ),

                center = Offset(ctx.canvasSize.width * 0.5f, ctx.canvasSize.height * 0.45f),

                radius = ctx.canvasSize.minDimension * 0.62f,

            ),

        )

        scope.drawRect(

            brush = Brush.radialGradient(

                colors = listOf(Color.Transparent, Color(0xFF060010).copy(alpha = 0.55f)),

                center = Offset(ctx.canvasSize.width * 0.5f, ctx.canvasSize.height * 0.5f),

                radius = ctx.canvasSize.minDimension * 0.78f,

            ),

        )

    }



    fun drawMazeFog(scope: DrawScope, ctx: PacMazeMapRenderContext) {

        val level = ctx.levelConfig

        val fogEnabled = level?.modeRules?.fogEnabled == true

        if (fogEnabled && level != null) {

            for (y in 0 until ctx.world.height) {

                for (x in 0 until ctx.world.width) {

                    if (PacMazeMazeExploration.isTileVisible(ctx.world, level, x, y)) continue

                    val rect = ctx.tileRect(x, y)

                    scope.drawRect(

                        color = Color(0xFF030508).copy(alpha = 0.96f),

                        topLeft = rect.topLeft,

                        size = rect.size,

                    )

                }

            }

        }



        val exitVisible = level == null || !fogEnabled ||

            ctx.markers.firstOrNull { it.kind == PacMazeMarkerKind.EXIT }?.let { exit ->

                PacMazeMazeExploration.isTileVisible(ctx.world, level, exit.x, exit.y)

            } == true

        if (exitVisible) {

            ctx.markers.firstOrNull { it.kind == PacMazeMarkerKind.EXIT }?.let { exit ->

                val rect = ctx.tileRect(exit.x, exit.y)

                val pulse = 0.65f + 0.25f * sin(ctx.animPhase * 2f)

                scope.drawCircle(

                    brush = Brush.radialGradient(

                        colors = listOf(

                            Color(0xFFFFB74D).copy(alpha = 0.35f * pulse),

                            Color.Transparent,

                        ),

                    ),

                    radius = ctx.cell * 1.8f,

                    center = rect.center,

                )

            }

        }



        if (ctx.world.radarRevealTicksLeft > 0) {

            ctx.world.primaryPac()?.let { pac ->

                val center = ctx.entityCenter(pac)

                val pulse = 0.5f + 0.35f * sin(ctx.animPhase * 3.2f)

                val radius = ctx.cell * PacMazeMazeExploration.RADAR_RADIUS * 1.15f

                scope.drawCircle(

                    brush = Brush.radialGradient(

                        colors = listOf(

                            Color(0xFF4FC3F7).copy(alpha = 0.22f * pulse),

                            Color(0xFF0277BD).copy(alpha = 0.08f * pulse),

                            Color.Transparent,

                        ),

                    ),

                    radius = radius,

                    center = center,

                )

                scope.drawCircle(

                    color = Color(0xFF81D4FA).copy(alpha = 0.45f * pulse),

                    radius = radius,

                    center = center,

                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = ctx.cell * 0.08f),

                )

            }

        }



        scope.drawRect(

            brush = Brush.radialGradient(

                colors = listOf(Color.Transparent, Color(0xFF050508).copy(alpha = if (fogEnabled) 0.35f else 0.48f)),

                center = Offset(ctx.canvasSize.width * 0.5f, ctx.canvasSize.height * 0.5f),

                radius = ctx.canvasSize.minDimension * 0.72f,

            ),

        )

    }

}


