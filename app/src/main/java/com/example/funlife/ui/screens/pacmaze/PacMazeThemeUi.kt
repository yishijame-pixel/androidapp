package com.example.funlife.ui.screens.pacmaze

import androidx.compose.ui.graphics.Color
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId

fun pacMazeThemeAccent(themeId: PacMazeMapThemeId): Color = when (themeId) {
    PacMazeMapThemeId.CYBERPUNK -> Color(0xFF00D4FF)
    PacMazeMapThemeId.GARDEN -> Color(0xFF66BB6A)
    PacMazeMapThemeId.FOOD -> Color(0xFFFF7043)
    PacMazeMapThemeId.CHINESE -> Color(0xFFFFCA28)
    PacMazeMapThemeId.CLASSIC -> Color(0xFF9575FF)
}

fun pacMazeThemeEmoji(themeId: PacMazeMapThemeId): String = when (themeId) {
    PacMazeMapThemeId.CYBERPUNK -> "🌃"
    PacMazeMapThemeId.GARDEN -> "🌿"
    PacMazeMapThemeId.FOOD -> "🍬"
    PacMazeMapThemeId.CHINESE -> "🏯"
    PacMazeMapThemeId.CLASSIC -> "👾"
}

fun pacMazeCharacterAccent(id: PacMazeCharacterId): Color = when (id) {
    PacMazeCharacterId.CLASSIC_PAC -> Color(0xFFFFCA28)
    PacMazeCharacterId.SCHOLAR -> Color(0xFF66BB6A)
    PacMazeCharacterId.LANTERN_FOX -> Color(0xFFFF7043)
    PacMazeCharacterId.CANDY_SPIRIT -> Color(0xFFF472B6)
    PacMazeCharacterId.DATA_CORE -> Color(0xFF22D3EE)
    PacMazeCharacterId.BUBBLE_SLIME -> Color(0xFF4ADE80)
    PacMazeCharacterId.NOODLE_PHANTOM -> Color(0xFFE2E8F0)
    PacMazeCharacterId.GEAR_MOLE -> Color(0xFFFB923C)
}

fun pacMazeCharacterTag(id: PacMazeCharacterId): String = when (id) {
    PacMazeCharacterId.CLASSIC_PAC -> "ARCADE"
    PacMazeCharacterId.SCHOLAR -> "COURTYARD"
    PacMazeCharacterId.LANTERN_FOX -> "GARDEN"
    PacMazeCharacterId.CANDY_SPIRIT -> "CANDY"
    PacMazeCharacterId.DATA_CORE -> "CYBER"
    PacMazeCharacterId.BUBBLE_SLIME -> "SLIME"
    PacMazeCharacterId.NOODLE_PHANTOM -> "FOOD"
    PacMazeCharacterId.GEAR_MOLE -> "STEAM"
}
