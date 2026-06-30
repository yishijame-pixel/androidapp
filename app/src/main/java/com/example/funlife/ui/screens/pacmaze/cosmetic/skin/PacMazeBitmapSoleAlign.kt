package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.ui.graphics.ImageBitmap
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId

/** @see PacMazeBitmapFrameAlign */
internal object PacMazeBitmapSoleAlign {

    fun offsetPxForSheet(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        sheet: PacMazeSkinSheetPlayback,
        frameIndex: Int,
        layoutHeight: Float,
    ): Float = PacMazeBitmapFrameAlign.forSheet(skinId, clip, sheet, frameIndex, layoutHeight).soleOffsetY

    fun offsetPxForImage(
        skinId: PacMazeSkinId,
        image: ImageBitmap,
        layoutHeight: Float,
    ): Float = PacMazeBitmapFrameAlign.forImage(skinId, image, layoutHeight).soleOffsetY
}
