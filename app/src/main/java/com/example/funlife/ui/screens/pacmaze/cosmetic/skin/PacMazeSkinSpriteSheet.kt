package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.ui.graphics.ImageBitmap

internal data class PacMazeSkinSpriteSheet(
    val frames: List<ImageBitmap>,
    val columns: Int,
    val rows: Int,
)

internal data class SpriteSheetAssetConfig(
    val assetPath: String,
    val columns: Int,
    val rows: Int,
    val frameCount: Int,
    val prep: PacMazeSkinAssetCache.AssetPrep,
    /** 裁掉每格顶部比例（去掉「Frame 1」等标注） */
    val topCropRatio: Float = 0.18f,
)
