package com.example.funlife.ui.screens.pacmaze

import androidx.compose.ui.graphics.Color
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterId
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeCosmeticCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeTrailId
import com.example.funlife.social.game.engine.pacmaze.GhostKind
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId

fun pacMazeThemeAccent(themeId: PacMazeMapThemeId): Color = when (themeId) {
    PacMazeMapThemeId.CYBERPUNK -> Color(0xFF00D4FF)
    PacMazeMapThemeId.GARDEN -> Color(0xFF66BB6A)
    PacMazeMapThemeId.FOOD -> Color(0xFFFF7043)
    PacMazeMapThemeId.CHINESE -> Color(0xFFFFCA28)
    PacMazeMapThemeId.CLASSIC -> Color(0xFF9575FF)
    PacMazeMapThemeId.ENDLESS -> Color(0xFFB388FF)
    PacMazeMapThemeId.MAZE -> Color(0xFFFFB74D)
    PacMazeMapThemeId.STEAMPUNK -> Color(0xFFFF8F00)
    PacMazeMapThemeId.VHS -> Color(0xFF18FFFF)
    PacMazeMapThemeId.ORBITAL -> Color(0xFF90CAF9)
    PacMazeMapThemeId.MAGMA -> Color(0xFFFF5722)
    PacMazeMapThemeId.SUBMARINE -> Color(0xFF4FC3F7)
    PacMazeMapThemeId.FROST -> Color(0xFFB3E5FC)
    PacMazeMapThemeId.ARCHIVE -> Color(0xFFD7CCC8)
    PacMazeMapThemeId.METRO -> Color(0xFFFFEB3B)
    PacMazeMapThemeId.OPERA -> Color(0xFFFFD54F)
    PacMazeMapThemeId.GREENHOUSE -> Color(0xFF8BC34A)
    PacMazeMapThemeId.CHRONO -> Color(0xFFFFD54F)
    PacMazeMapThemeId.MIRROR -> Color(0xFF7C4DFF)
}

fun pacMazeGhostAccent(kind: GhostKind): Color = Color(kind.accentArgb)

fun pacMazeThemeEmoji(themeId: PacMazeMapThemeId): String = when (themeId) {
    PacMazeMapThemeId.CYBERPUNK -> "🌃"
    PacMazeMapThemeId.GARDEN -> "🌿"
    PacMazeMapThemeId.FOOD -> "🍬"
    PacMazeMapThemeId.CHINESE -> "🏯"
    PacMazeMapThemeId.CLASSIC -> "👾"
    PacMazeMapThemeId.ENDLESS -> "♾️"
    PacMazeMapThemeId.MAZE -> "🧭"
    PacMazeMapThemeId.STEAMPUNK -> "⚙️"
    PacMazeMapThemeId.VHS -> "📺"
    PacMazeMapThemeId.ORBITAL -> "🛸"
    PacMazeMapThemeId.MAGMA -> "🌋"
    PacMazeMapThemeId.SUBMARINE -> "🫧"
    PacMazeMapThemeId.FROST -> "❄️"
    PacMazeMapThemeId.ARCHIVE -> "📜"
    PacMazeMapThemeId.METRO -> "🚇"
    PacMazeMapThemeId.OPERA -> "🎭"
    PacMazeMapThemeId.GREENHOUSE -> "🌱"
    PacMazeMapThemeId.CHRONO -> "⏳"
    PacMazeMapThemeId.MIRROR -> "🪞"
}

fun pacMazeSkinAccent(id: PacMazeSkinId): Color = when (id) {
    PacMazeSkinId.LINE_PUPPY -> Color(0xFFFFB74D)
    PacMazeSkinId.LINE_KITTY -> Color(0xFFF472B6)
    PacMazeSkinId.LINE_BUNNY -> Color(0xFFF9A8D4)
    PacMazeSkinId.LINE_PANDA -> Color(0xFF94A3B8)
    PacMazeSkinId.LINE_FOX -> Color(0xFFFF7043)
    PacMazeSkinId.LINE_BEAR -> Color(0xFFD97706)
    PacMazeSkinId.LINE_PENGUIN -> Color(0xFF7DD3FC)
    PacMazeSkinId.LINE_OWL -> Color(0xFFD97706)
    PacMazeSkinId.LINE_HEDGEHOG -> Color(0xFF92400E)
    PacMazeSkinId.LINE_SHIBA -> Color(0xFFF97316)
    PacMazeSkinId.LINE_OTTER -> Color(0xFF2DD4BF)
    PacMazeSkinId.LINE_KOALA -> Color(0xFF9CA3AF)
    PacMazeSkinId.SEA_SHARK -> Color(0xFF78909C)
    PacMazeSkinId.SEA_CLOWNFISH -> Color(0xFFFF7043)
    PacMazeSkinId.SEA_JELLYFISH -> Color(0xFFCE93D8)
    PacMazeSkinId.SEA_OCTOPUS -> Color(0xFFEF5350)
    PacMazeSkinId.SEA_TURTLE -> Color(0xFF4DB6AC)
    PacMazeSkinId.SEA_MANTA -> Color(0xFF607D8B)
    PacMazeSkinId.SEA_SEAHORSE -> Color(0xFFFFA726)
    PacMazeSkinId.SEA_DOLPHIN -> Color(0xFF29B6F6)
    PacMazeSkinId.SEA_SQUID -> Color(0xFFA78BFA)
    PacMazeSkinId.SEA_ANGLER -> Color(0xFFFBBF24)
    PacMazeSkinId.SEA_HERMIT -> Color(0xFFF87171)
    PacMazeSkinId.SEA_STARFISH -> Color(0xFFFB7185)
    PacMazeSkinId.SEA_EEL -> Color(0xFF38BDF8)
    PacMazeSkinId.SEA_SUNFISH -> Color(0xFF60A5FA)
    PacMazeSkinId.INK_DROP_SPIRIT -> Color(0xFF475569)
    PacMazeSkinId.INK_PAPER_BIRD -> Color(0xFFF472B6)
    PacMazeSkinId.INK_LION_DANCE -> Color(0xFFEF4444)
    PacMazeSkinId.INK_PORCELAIN -> Color(0xFF38BDF8)
    PacMazeSkinId.INK_KYLIN -> Color(0xFFFFB300)
    PacMazeSkinId.INK_FAN_FAIRY -> Color(0xFFE91E63)
    PacMazeSkinId.INK_LOTUS_BUD -> Color(0xFF66BB6A)
    PacMazeSkinId.INK_SHADOW_PUPPET -> Color(0xFFEF4444)
    PacMazeSkinId.SCHOLAR -> Color(0xFF66BB6A)
    PacMazeSkinId.LANTERN_FOX -> Color(0xFFFF7043)
    PacMazeSkinId.CYBER_HOLO_CAT -> Color(0xFF22D3EE)
    PacMazeSkinId.CYBER_GLITCH_CUBE -> Color(0xFFF472B6)
    PacMazeSkinId.CYBER_MAGLEV_ORB -> Color(0xFF818CF8)
    PacMazeSkinId.CYBER_WIRE_WORM -> Color(0xFF34D399)
    PacMazeSkinId.CYBER_DRONE_BEE -> Color(0xFFFFEB3B)
    PacMazeSkinId.CYBER_NEON_SNAKE -> Color(0xFF00E5FF)
    PacMazeSkinId.CYBER_CHIP_MONKEY -> Color(0xFF4ADE80)
    PacMazeSkinId.CYBER_LASER_BEETLE -> Color(0xFFFF1744)
    PacMazeSkinId.DATA_CORE -> Color(0xFF22D3EE)
    PacMazeSkinId.FOOD_MOCHI -> Color(0xFFF9A8D4)
    PacMazeSkinId.FOOD_CHILI -> Color(0xFFEF4444)
    PacMazeSkinId.FOOD_SUSHI -> Color(0xFFF97316)
    PacMazeSkinId.FOOD_POPCORN -> Color(0xFFFBBF24)
    PacMazeSkinId.FOOD_TANGYUAN -> Color(0xFFE2E8F0)
    PacMazeSkinId.FOOD_DUMPLING -> Color(0xFFFF7043)
    PacMazeSkinId.FOOD_MANGO_PUDDING -> Color(0xFFFF9800)
    PacMazeSkinId.FOOD_DONUT -> Color(0xFFE91E63)
    PacMazeSkinId.FOOD_CHICK_DAZE -> Color(0xFFFFEB3B)
    PacMazeSkinId.FOOD_CHICK_BALLER -> Color(0xFFFF9800)
    PacMazeSkinId.FOOD_CHICK_WALKER -> Color(0xFFFFC107)
    PacMazeSkinId.FOOD_CHICK_WALKER_PRO_MAX -> Color(0xFFFF5722)
    PacMazeSkinId.FOOD_XIA_WALK -> Color(0xFF42A5F5)
    PacMazeSkinId.FOOD_MOUSE_WALK -> Color(0xFF8D6E63)
    PacMazeSkinId.FOOD_QINGTING_WALK -> Color(0xFF66BB6A)
    PacMazeSkinId.FOOD_MOSQUITO_WALK -> Color(0xFFAB47BC)
    PacMazeSkinId.FOOD_TOUSHI_WALK -> Color(0xFFFF8A65)
    PacMazeSkinId.FOOD_ZOMBIE_WALK -> Color(0xFF66BB6A)
    PacMazeSkinId.YISHI_FIRE_LONG -> Color(0xFFFF5722)
    PacMazeSkinId.YISHI_GREEN_LONG -> Color(0xFF66BB6A)
    PacMazeSkinId.YISHI_HAIMIAN -> Color(0xFFFFEB3B)
    PacMazeSkinId.YISHI_ICE_LONG -> Color(0xFF4FC3F7)
    PacMazeSkinId.YISHI_LONG -> Color(0xFFFF7043)
    PacMazeSkinId.YISHI_MAGIC_DOG -> Color(0xFFAB47BC)
    PacMazeSkinId.YISHI_PAIDAXIN -> Color(0xFF81C784)
    PacMazeSkinId.YISHI_QISHI_DOG -> Color(0xFF5C6BC0)
    PacMazeSkinId.YISHI_BL_LONG -> Color(0xFF42A5F5)
    PacMazeSkinId.CANDY_SPIRIT -> Color(0xFFF472B6)
    PacMazeSkinId.BUBBLE_SLIME -> Color(0xFF4ADE80)
    PacMazeSkinId.NOODLE_PHANTOM -> Color(0xFFE2E8F0)
    PacMazeSkinId.CLASSIC_PAC -> Color(0xFFFFCA28)
    PacMazeSkinId.GEAR_MOLE -> Color(0xFFFB923C)
}

fun pacMazeSkinTag(id: PacMazeSkinId): String =
    PacMazeCosmeticCatalog.definition(id).styleFamily.label

fun pacMazeTrailAccent(id: PacMazeTrailId): Color = when (id) {
    PacMazeTrailId.NONE -> Color(0xFF94A3B8)
    PacMazeTrailId.RIBBON_FLOW -> Color(0xFFFF8A80)
    PacMazeTrailId.RIBBON_SAKURA -> Color(0xFFF472B6)
    PacMazeTrailId.RIBBON_AURORA -> Color(0xFF2DD4BF)
    PacMazeTrailId.RIBBON_PHOENIX -> Color(0xFFFF7043)
    PacMazeTrailId.RIBBON_SOUL -> Color(0xFF818CF8)
    PacMazeTrailId.RIBBON_JADE -> Color(0xFF4ADE80)
    PacMazeTrailId.RIBBON_CINNABAR -> Color(0xFFEF4444)
    PacMazeTrailId.RIBBON_CELADON -> Color(0xFF2DD4BF)
    PacMazeTrailId.RIBBON_VIOLET -> Color(0xFF8B5CF6)
    PacMazeTrailId.RIBBON_GINKGO -> Color(0xFFEAB308)
    PacMazeTrailId.RIBBON_MINT_BUBBLE -> Color(0xFF34D399)
    PacMazeTrailId.RIBBON_NIGHT_INK -> Color(0xFF6366F1)
    PacMazeTrailId.PETAL_SHOWER -> Color(0xFFF472B6)
    PacMazeTrailId.NOTE_HOP -> Color(0xFF818CF8)
    PacMazeTrailId.CANDY_CRUMB -> Color(0xFFF472B6)
    PacMazeTrailId.SNOW_SWIRL -> Color(0xFF7DD3FC)
    PacMazeTrailId.HEX_HONEY -> Color(0xFFFBBF24)
    PacMazeTrailId.DATA_CASCADE -> Color(0xFF22D3EE)
    PacMazeTrailId.RADAR_SWEEP -> Color(0xFF34D399)
    PacMazeTrailId.CUBE_SHATTER -> Color(0xFF818CF8)
    PacMazeTrailId.PAW_PRINT -> Color(0xFFF97316)
    PacMazeTrailId.RIPPLE_STEP -> Color(0xFF38BDF8)
    PacMazeTrailId.NEON_PIXEL -> Color(0xFFFF1744)
    PacMazeTrailId.ION_WAKE -> Color(0xFF00E5FF)
    PacMazeTrailId.GHOST_ECHO -> Color(0xFFB388FF)
    PacMazeTrailId.STAR_COMET -> Color(0xFFFFD54F)
}

fun pacMazeCharacterAccent(id: PacMazeCharacterId): Color =
    pacMazeSkinAccent(PacMazeSkinId.fromLegacy(id))

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

fun pacMazeFormatBestTime(ms: Long): String {
    if (ms <= 0L) return "尚未挑战"
    val totalSec = (ms / 1000).toInt()
    val min = totalSec / 60
    val sec = totalSec % 60
    return if (min > 0) "${min}分${sec}秒" else "${sec}秒"
}
