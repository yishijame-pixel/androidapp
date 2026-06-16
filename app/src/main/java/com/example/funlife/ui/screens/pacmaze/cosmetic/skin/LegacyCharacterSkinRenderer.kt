package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterDraw
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterPose
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemePalette

internal class LegacyCharacterSkinRenderer(
    override val skinId: PacMazeSkinId,
) : PacMazeSkinRenderer {

    init {
        require(skinId.legacyCharacterId() != null) { "Legacy renderer requires mapped character: $skinId" }
    }

    override fun draw(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        pose: PacMazeCharacterPose,
        themeId: PacMazeMapThemeId,
        palette: PacMazeThemePalette,
    ) {
        val legacyId = skinId.legacyCharacterId() ?: return
        PacMazeCharacterDraw.draw(
            scope = scope,
            characterId = legacyId,
            center = center,
            radius = radius,
            pose = pose,
            themeId = themeId,
        )
    }
}
