package com.example.funlife.ui.screens.pacmaze.cosmetic.trail

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeTrailId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemePalette

object PacMazeTrailRegistry {

    private val renderers: Map<PacMazeTrailId, PacMazeTrailRenderer> = mapOf(
        PacMazeTrailId.RIBBON_FLOW to RibbonFlowTrailRenderer,
        PacMazeTrailId.RIBBON_SAKURA to RibbonSakuraTrailRenderer,
        PacMazeTrailId.RIBBON_AURORA to RibbonAuroraTrailRenderer,
        PacMazeTrailId.RIBBON_PHOENIX to RibbonPhoenixTrailRenderer,
        PacMazeTrailId.RIBBON_SOUL to RibbonSoulTrailRenderer,
        PacMazeTrailId.RIBBON_JADE to RibbonJadeTrailRenderer,
        PacMazeTrailId.RIBBON_CINNABAR to RibbonCinnabarTrailRenderer,
        PacMazeTrailId.RIBBON_CELADON to RibbonCeladonTrailRenderer,
        PacMazeTrailId.RIBBON_VIOLET to RibbonVioletTrailRenderer,
        PacMazeTrailId.RIBBON_GINKGO to RibbonGinkgoTrailRenderer,
        PacMazeTrailId.RIBBON_MINT_BUBBLE to RibbonMintBubbleTrailRenderer,
        PacMazeTrailId.RIBBON_NIGHT_INK to RibbonNightInkTrailRenderer,
        PacMazeTrailId.PETAL_SHOWER to PetalShowerTrailRenderer,
        PacMazeTrailId.NOTE_HOP to NoteHopTrailRenderer,
        PacMazeTrailId.CANDY_CRUMB to CandyCrumbTrailRenderer,
        PacMazeTrailId.SNOW_SWIRL to SnowSwirlTrailRenderer,
        PacMazeTrailId.HEX_HONEY to HexHoneyTrailRenderer,
        PacMazeTrailId.DATA_CASCADE to DataCascadeTrailRenderer,
        PacMazeTrailId.RADAR_SWEEP to RadarSweepTrailRenderer,
        PacMazeTrailId.CUBE_SHATTER to CubeShatterTrailRenderer,
        PacMazeTrailId.PAW_PRINT to PawPrintTrailRenderer,
        PacMazeTrailId.RIPPLE_STEP to RippleStepTrailRenderer,
        PacMazeTrailId.NONE to NoneTrailRenderer,
        PacMazeTrailId.NEON_PIXEL to NeonPixelTrailRenderer,
        PacMazeTrailId.ION_WAKE to IonWakeTrailRenderer,
        PacMazeTrailId.GHOST_ECHO to GhostEchoTrailRenderer,
        PacMazeTrailId.STAR_COMET to StarCometTrailRenderer,
    )

    fun draw(
        scope: DrawScope,
        trailId: PacMazeTrailId,
        samples: List<PacMazeTrailSample>,
        palette: PacMazeThemePalette,
        cell: Float,
        powerActive: Boolean,
    ) {
        if (trailId == PacMazeTrailId.NONE || samples.isEmpty()) return
        renderers[trailId]?.draw(scope, samples, palette, cell, powerActive)
    }
}
