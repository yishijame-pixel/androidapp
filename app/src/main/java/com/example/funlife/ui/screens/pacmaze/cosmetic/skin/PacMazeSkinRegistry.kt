package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterPose
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemePalette

object PacMazeSkinRegistry {

    /** 局内绘制时由 [draw] 注入的真实格宽（像素），供位图皮肤裁剪走廊。 */
    internal var drawCorridorCellPx: Float? = null
    internal var drawCellXPx: Float? = null
    internal var drawCellYPx: Float? = null
    internal var drawVerticalCellPx: Float? = null
    internal var drawTileCellPx: Float? = null
    internal var drawTileBottomYPx: Float? = null
    internal var drawFeetAnchorPx: Offset? = null
    internal var drawCorridorCenterPx: Offset? = null
    internal var drawWallBoxRect: Rect? = null
    internal var drawMapClipRect: Rect? = null
    internal var drawVisualFacing: com.example.funlife.social.game.engine.pacmaze.Direction? = null
    /** 移动轴（逻辑方向），用于脚点/枢轴布局；与 [drawVisualFacing] 解耦。 */
    internal var drawTravelFacing: com.example.funlife.social.game.engine.pacmaze.Direction? = null
    internal var drawUserScale: Float = 1f
    internal var drawEntityBoost: Float = 1f
    /** DEBUG：布局脚点诊断 */
    internal var drawDiagEntityId: String? = null
    internal var drawDiagLogicY: Float = Float.NaN
    internal var drawDiagFrameIndex: Int = -1

    private val lineArtRenderers: Map<PacMazeSkinId, PacMazeSkinRenderer> = mapOf(
        PacMazeSkinId.LINE_PUPPY to LinePuppySkinRenderer,
        PacMazeSkinId.LINE_KITTY to LineKittySkinRenderer,
        PacMazeSkinId.LINE_BUNNY to LineBunnySkinRenderer,
        PacMazeSkinId.LINE_PANDA to LinePandaSkinRenderer,
        PacMazeSkinId.LINE_FOX to LineFoxSkinRenderer,
        PacMazeSkinId.LINE_BEAR to LineBearSkinRenderer,
        PacMazeSkinId.LINE_PENGUIN to LinePenguinSkinRenderer,
        PacMazeSkinId.LINE_OWL to LineOwlSkinRenderer,
        PacMazeSkinId.LINE_HEDGEHOG to LineHedgehogSkinRenderer,
        PacMazeSkinId.LINE_SHIBA to LineShibaSkinRenderer,
        PacMazeSkinId.LINE_OTTER to LineOtterSkinRenderer,
        PacMazeSkinId.LINE_KOALA to LineKoalaSkinRenderer,
    )

    private val seaCreatureRenderers: Map<PacMazeSkinId, PacMazeSkinRenderer> = mapOf(
        PacMazeSkinId.SEA_SHARK to SeaSharkSkinRenderer,
        PacMazeSkinId.SEA_CLOWNFISH to SeaClownfishSkinRenderer,
        PacMazeSkinId.SEA_JELLYFISH to SeaJellyfishSkinRenderer,
        PacMazeSkinId.SEA_OCTOPUS to SeaOctopusSkinRenderer,
        PacMazeSkinId.SEA_TURTLE to SeaTurtleSkinRenderer,
        PacMazeSkinId.SEA_MANTA to SeaMantaSkinRenderer,
        PacMazeSkinId.SEA_SEAHORSE to SeaSeahorseSkinRenderer,
        PacMazeSkinId.SEA_DOLPHIN to SeaDolphinSkinRenderer,
        PacMazeSkinId.SEA_SQUID to SeaSquidSkinRenderer,
        PacMazeSkinId.SEA_ANGLER to SeaAnglerSkinRenderer,
        PacMazeSkinId.SEA_HERMIT to SeaHermitSkinRenderer,
        PacMazeSkinId.SEA_STARFISH to SeaStarfishSkinRenderer,
        PacMazeSkinId.SEA_EEL to SeaEelSkinRenderer,
        PacMazeSkinId.SEA_SUNFISH to SeaSunfishSkinRenderer,
    )

    private val familySkinRenderers: Map<PacMazeSkinId, PacMazeSkinRenderer> = mapOf(
        PacMazeSkinId.INK_DROP_SPIRIT to InkDropSpiritSkinRenderer,
        PacMazeSkinId.INK_PAPER_BIRD to InkPaperBirdSkinRenderer,
        PacMazeSkinId.INK_LION_DANCE to InkLionDanceSkinRenderer,
        PacMazeSkinId.INK_PORCELAIN to InkPorcelainSkinRenderer,
        PacMazeSkinId.INK_KYLIN to InkKylinSkinRenderer,
        PacMazeSkinId.INK_FAN_FAIRY to InkFanFairySkinRenderer,
        PacMazeSkinId.INK_LOTUS_BUD to InkLotusBudSkinRenderer,
        PacMazeSkinId.INK_SHADOW_PUPPET to InkShadowPuppetSkinRenderer,
        PacMazeSkinId.CYBER_HOLO_CAT to CyberHoloCatSkinRenderer,
        PacMazeSkinId.CYBER_GLITCH_CUBE to CyberGlitchCubeSkinRenderer,
        PacMazeSkinId.CYBER_MAGLEV_ORB to CyberMaglevOrbSkinRenderer,
        PacMazeSkinId.CYBER_WIRE_WORM to CyberWireWormSkinRenderer,
        PacMazeSkinId.CYBER_DRONE_BEE to CyberDroneBeeSkinRenderer,
        PacMazeSkinId.CYBER_NEON_SNAKE to CyberNeonSnakeSkinRenderer,
        PacMazeSkinId.CYBER_CHIP_MONKEY to CyberChipMonkeySkinRenderer,
        PacMazeSkinId.CYBER_LASER_BEETLE to CyberLaserBeetleSkinRenderer,
        PacMazeSkinId.FOOD_MOCHI to FoodMochiSkinRenderer,
        PacMazeSkinId.FOOD_CHILI to FoodChiliSkinRenderer,
        PacMazeSkinId.FOOD_SUSHI to FoodSushiSkinRenderer,
        PacMazeSkinId.FOOD_POPCORN to FoodPopcornSkinRenderer,
        PacMazeSkinId.FOOD_TANGYUAN to FoodTangyuanSkinRenderer,
        PacMazeSkinId.FOOD_DUMPLING to FoodDumplingSkinRenderer,
        PacMazeSkinId.FOOD_MANGO_PUDDING to FoodMangoPuddingSkinRenderer,
        PacMazeSkinId.FOOD_DONUT to FoodDonutSkinRenderer,
        PacMazeSkinId.FOOD_CHICK_DAZE to AssetBitmapSkinRenderer(PacMazeSkinId.FOOD_CHICK_DAZE),
        PacMazeSkinId.FOOD_CHICK_BALLER to AssetBitmapSkinRenderer(PacMazeSkinId.FOOD_CHICK_BALLER),
        PacMazeSkinId.FOOD_CHICK_WALKER to AssetBitmapSkinRenderer(PacMazeSkinId.FOOD_CHICK_WALKER),
    )

    private val remoteAnimatedRenderers: Map<PacMazeSkinId, PacMazeSkinRenderer> =
        PacMazeRemoteSkinAnimCatalog.remoteSkinIds.associateWith { skinId ->
            RemoteAnimatedSkinRenderer(skinId)
        }

    private val renderers: Map<PacMazeSkinId, PacMazeSkinRenderer> = buildMap {
        putAll(lineArtRenderers)
        putAll(seaCreatureRenderers)
        putAll(familySkinRenderers)
        putAll(remoteAnimatedRenderers)
        PacMazeSkinId.entries.forEach { skinId ->
            if (
                !lineArtRenderers.containsKey(skinId) &&
                !seaCreatureRenderers.containsKey(skinId) &&
                !familySkinRenderers.containsKey(skinId) &&
                skinId.legacyCharacterId() != null
            ) {
                put(skinId, LegacyCharacterSkinRenderer(skinId))
            }
        }
    }

    /** 局内裁剪预布局：取当前可走帧或封面，无则 null。 */
    fun peekGameplayBitmap(skinId: PacMazeSkinId): androidx.compose.ui.graphics.ImageBitmap? {
        PacMazeRemoteSkinAnimCache.cover(skinId)?.let { return it }
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: run {
            return PacMazeSkinAssetCache.bitmap(skinId)
        }
        val clip = config.primaryClip()
        PacMazeRemoteSkinAnimCache.frames(skinId, clip)?.firstOrNull()?.let { return it }
        return PacMazeRemoteSkinAnimCache.cover(skinId) ?: PacMazeSkinAssetCache.bitmap(skinId)
    }

    fun draw(
        scope: DrawScope,
        skinId: PacMazeSkinId,
        center: Offset,
        radius: Float,
        pose: PacMazeCharacterPose,
        themeId: PacMazeMapThemeId,
        palette: PacMazeThemePalette,
        corridorCellPx: Float? = null,
        cellXPx: Float? = null,
        cellYPx: Float? = null,
        verticalCellPx: Float? = null,
        tileCellPx: Float? = null,
        tileBottomYPx: Float? = null,
        feetAnchorPx: Offset? = null,
        corridorCenterPx: Offset? = null,
        wallBoxRect: Rect? = null,
        mapClipRect: Rect? = null,
        visualFacing: com.example.funlife.social.game.engine.pacmaze.Direction? = null,
        travelFacing: com.example.funlife.social.game.engine.pacmaze.Direction? = null,
        userDrawScale: Float = 1f,
        entityDrawBoost: Float = 1f,
    ) {
        val prevCorridor = drawCorridorCellPx
        val prevCellX = drawCellXPx
        val prevCellY = drawCellYPx
        val prevVertical = drawVerticalCellPx
        val prevTile = drawTileCellPx
        val prevTileBottom = drawTileBottomYPx
        val prevFeet = drawFeetAnchorPx
        val prevCorridorCenter = drawCorridorCenterPx
        val prevWallBox = drawWallBoxRect
        val prevMapClip = drawMapClipRect
        val prevVisualFacing = drawVisualFacing
        val prevTravelFacing = drawTravelFacing
        val prevUser = drawUserScale
        val prevBoost = drawEntityBoost
        if (corridorCellPx != null) drawCorridorCellPx = corridorCellPx
        if (cellXPx != null) drawCellXPx = cellXPx
        if (cellYPx != null) drawCellYPx = cellYPx
        if (verticalCellPx != null) drawVerticalCellPx = verticalCellPx
        if (tileCellPx != null) drawTileCellPx = tileCellPx
        if (tileBottomYPx != null) drawTileBottomYPx = tileBottomYPx
        drawFeetAnchorPx = feetAnchorPx
        drawCorridorCenterPx = corridorCenterPx
        drawWallBoxRect = wallBoxRect
        drawMapClipRect = mapClipRect
        drawVisualFacing = visualFacing
        drawTravelFacing = travelFacing
        drawUserScale = userDrawScale
        drawEntityBoost = entityDrawBoost
        drawDiagEntityId = null
        drawDiagLogicY = Float.NaN
        drawDiagFrameIndex = -1
        try {
            renderers[skinId]?.draw(scope, center, radius, pose, themeId, palette)
        } finally {
            drawCorridorCellPx = prevCorridor
            drawCellXPx = prevCellX
            drawCellYPx = prevCellY
            drawVerticalCellPx = prevVertical
            drawTileCellPx = prevTile
            drawTileBottomYPx = prevTileBottom
            drawFeetAnchorPx = prevFeet
            drawCorridorCenterPx = prevCorridorCenter
            drawWallBoxRect = prevWallBox
            drawMapClipRect = prevMapClip
            drawVisualFacing = prevVisualFacing
            drawTravelFacing = prevTravelFacing
            drawUserScale = prevUser
            drawEntityBoost = prevBoost
        }
    }
}
