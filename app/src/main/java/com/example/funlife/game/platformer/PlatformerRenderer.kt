package com.example.funlife.game.platformer

import android.graphics.BitmapFactory
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.funlife.game.platformer.tmx.PlatformerTmxRenderer
import com.example.funlife.game.platformer.tmx.PlatformerTmxWorldBuilder
import com.example.funlife.game.platformer.catalog.PlatformerEnemySkinRenderer
import com.example.funlife.game.platformer.catalog.PlatformerRemoteAnimCache
import com.example.funlife.game.platformer.catalog.PlatformerSkinRenderer
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeIkunGameplayScale
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeBitmapFeetAnchor
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinAnimManifest
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.PI
import kotlin.math.sin

class PlatformerAssets(
    val atlas: ImageBitmap,
    val packTilesets: Map<PlatformerTilesetPack, PlatformerPackTileset>,
    val enemyAssets: PlatformerEnemyAssets,
    val characterAssets: PlatformerCharacterAssets,
    val tmxBitmaps: Map<String, ImageBitmap>,
    val chickBasketball: ImageBitmap? = null,
    val skyChickSheet: ImageBitmap? = null,
    val supertuxTileAtlas: PlatformerSuperTuxTileAtlas? = null,
) {
    fun packFor(level: PlatformerLevelDef): PlatformerPackTileset? =
        packTilesets[level.tilesetPack]

    fun withTmxBitmaps(extra: Map<String, ImageBitmap>): PlatformerAssets {
        if (extra.isEmpty()) return this
        return PlatformerAssets(
            atlas = atlas,
            packTilesets = packTilesets,
            enemyAssets = enemyAssets,
            characterAssets = characterAssets,
            tmxBitmaps = tmxBitmaps + extra,
            chickBasketball = chickBasketball,
            skyChickSheet = skyChickSheet,
            supertuxTileAtlas = supertuxTileAtlas,
        )
    }

    companion object {
        fun load(context: android.content.Context): PlatformerAssets {
            val atlasBmp = decodeAsset(context, "platformer/goodly_2x.png")
                ?: placeholderAtlas()
            val packs = PlatformerPackTilesetLoader.loadAll(context)
            val enemies = PlatformerEnemyAssetsLoader.load(context)
            val characters = PlatformerCharacterAssetsLoader.load(context)
            val tmxBitmaps = loadTmxBitmaps(context)
            val basketball = decodeAsset(context, "platformer/chick_basketball.png")
            val skyChick = decodeAsset(context, "platformer/sky_chick_enemy.png", alpha = true)
            val supertuxAtlas = PlatformerSuperTuxTileAtlasLoader.load(context)
            return PlatformerAssets(
                atlas = atlasBmp,
                packTilesets = packs,
                enemyAssets = enemies,
                characterAssets = characters,
                tmxBitmaps = tmxBitmaps,
                chickBasketball = basketball,
                skyChickSheet = skyChick,
                supertuxTileAtlas = supertuxAtlas,
            )
        }

        /** 资源缺失时仍返回最小可用图集，避免加载页卡死。 */
        fun loadFallback(context: android.content.Context): PlatformerAssets = runCatching {
            load(context)
        }.getOrElse {
            PlatformerAssets(
                atlas = placeholderAtlas(),
                packTilesets = emptyMap(),
                enemyAssets = PlatformerEnemyAssets(emptyMap()),
                characterAssets = PlatformerCharacterAssets(emptyMap()),
                tmxBitmaps = loadTmxBitmaps(context),
            )
        }

        private fun placeholderAtlas(): ImageBitmap {
            val bmp = android.graphics.Bitmap.createBitmap(32, 32, android.graphics.Bitmap.Config.ARGB_8888)
            bmp.eraseColor(android.graphics.Color.rgb(100, 180, 100))
            return bmp.asImageBitmap()
        }

        private fun decodeAsset(
            context: android.content.Context,
            path: String,
            alpha: Boolean = false,
        ): ImageBitmap? =
            runCatching {
                val opts = if (alpha) {
                    BitmapFactory.Options().apply {
                        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                        inScaled = false
                    }
                } else {
                    null
                }
                context.assets.open(path).use { stream ->
                    if (opts != null) BitmapFactory.decodeStream(stream, null, opts)?.asImageBitmap()
                    else BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()

        private fun loadTmxBitmaps(context: android.content.Context): Map<String, ImageBitmap> {
            val paths = listOf(
                "platformer/marianandrobin/levels/forest/forest_tileset.png",
                "platformer/marianandrobin/levels/forest/background.png",
                "platformer/marianandrobin/levels/castle/castle_tileset.png",
                "platformer/marianandrobin/levels/tournament/tournament_tileset.png",
            )
            return paths.mapNotNull { path ->
                decodeAsset(context, path)?.let { path to it }
            }.toMap()
        }
    }
}

object PlatformerRenderer {

    fun draw(
        scope: DrawScope,
        world: PlatformerWorld,
        assets: PlatformerAssets,
        viewportW: Float,
        viewportH: Float,
        animTime: Float,
    ) {
        val vp = PlatformerViewport.compute(world, viewportW, viewportH)
        val camX = world.cameraX
        val level = world.level

        drawSky(scope, level, assets, viewportW, viewportH, camX, vp.scale, animTime)
        if (world.tmx != null) {
            val tmx = world.tmx
            val tileset = assets.tmxBitmaps[tmx.tilesetPath]
            val bg = tmx.backgroundPath?.let { assets.tmxBitmaps[it] }
            if (tileset != null) {
                PlatformerTmxRenderer.draw(scope, world, tileset, bg, camX, vp, viewportW)
            }
        } else {
            drawPackBackdrops(scope, world, assets, camX, vp, viewportW)
            drawGoodlyBackdrops(scope, world, camX, vp, viewportW)
            drawPackDecor(scope, world, assets, camX, vp, viewportW)
            drawTiles(scope, world, assets, camX, vp, viewportW)
            drawHazards(scope, world, camX, vp, viewportW, animTime)
        }
        drawEnemies(scope, world, assets, camX, vp, animTime)
        drawTraps(scope, world, assets, camX, vp, animTime)
        drawGems(scope, world, camX, vp, animTime)
        drawGoal(scope, world, camX, vp, animTime)
        drawHitSparks(scope, world, camX, vp)
        drawSkyChickAndEggs(scope, world, assets, camX, vp)
        drawPlayer(scope, world, assets, camX, vp, animTime)
    }

    private fun drawSkyChickAndEggs(
        scope: DrawScope,
        world: PlatformerWorld,
        assets: PlatformerAssets,
        camX: Float,
        vp: PlatformerViewport,
    ) {
        val tile = world.tileF
        world.skyEggs.forEach { egg ->
            if (!egg.alive) return@forEach
            drawSkyEgg(scope, egg, tile, camX, vp)
        }
        val chick = world.skyChick ?: return
        val sheet = assets.skyChickSheet ?: return
        val (cellW, cellH) = PlatformerSkyChickAssets.cellSize(sheet)
        val frameIdx = PlatformerSkyChickSystem.frameIndex(chick)
        val rect = PlatformerSkyChickAssets.frameRect(frameIdx, cellW, cellH)
        val w = PlatformerSkyChickSystem.chickWidth(world.tilePx) * (vp.cell / tile)
        val h = PlatformerSkyChickSystem.chickHeight(world.tilePx) * (vp.cell / tile)
        val left = vp.worldToScreenX(chick.x, camX)
        val top = vp.worldToScreenY(chick.y)
        val pivot = Offset(left + w * 0.5f, top + h)
        scope.withTransform({
            if (!chick.facingRight) scale(scaleX = -1f, scaleY = 1f, pivot = pivot)
        }) {
            scope.drawImage(
                image = sheet,
                srcOffset = IntOffset(rect.srcX, rect.srcY),
                srcSize = IntSize(rect.w, rect.h),
                dstOffset = IntOffset(left.toInt(), top.toInt()),
                dstSize = IntSize(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1)),
                filterQuality = FilterQuality.High,
            )
        }
    }

    private fun drawSkyEgg(
        scope: DrawScope,
        egg: PlatformerSkyEgg,
        tile: Float,
        camX: Float,
        vp: PlatformerViewport,
    ) {
        val r = PlatformerSkyChickSystem.eggRadius(tile) * (vp.cell / tile)
        val cx = vp.worldToScreenX(egg.x + r, camX)
        val cy = vp.worldToScreenY(egg.y + r)
        val w = r * 1.35f
        val h = r * 1.75f
        val topLeft = Offset(cx - w * 0.5f, cy - h * 0.5f)
        val size = Size(w, h)
        scope.drawOval(
            color = Color(0x33000000),
            topLeft = Offset(topLeft.x + r * 0.08f, topLeft.y + r * 0.12f),
            size = size,
        )
        scope.drawOval(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFF8E1), Color(0xFFFFECB3), Color(0xFFFFE082)),
                startY = topLeft.y,
                endY = topLeft.y + h,
            ),
            topLeft = topLeft,
            size = size,
        )
        scope.drawOval(
            color = Color(0xFF8D6E63).copy(alpha = 0.55f),
            topLeft = topLeft,
            size = size,
            style = Stroke(width = max(1f, r * 0.12f)),
        )
        scope.drawOval(
            color = Color.White.copy(alpha = 0.75f),
            topLeft = Offset(topLeft.x + w * 0.22f, topLeft.y + h * 0.18f),
            size = Size(w * 0.28f, h * 0.22f),
        )
    }

    private fun drawHitSparks(
        scope: DrawScope,
        world: PlatformerWorld,
        camX: Float,
        vp: PlatformerViewport,
    ) {
        world.hitSparks.forEach { spark ->
            val t = (spark.ageSec / PlatformerHitSpark.LIFETIME_SEC).coerceIn(0f, 1f)
            val alpha = (1f - t) * 0.9f
            val cx = vp.worldToScreenX(spark.x, camX)
            val cy = vp.worldToScreenY(spark.y)
            val r = vp.cell * (0.18f + t * 0.22f)
            scope.drawCircle(Color(0xFFFFF176).copy(alpha = alpha), radius = r, center = Offset(cx, cy))
            scope.drawCircle(Color(0xFFFF7043).copy(alpha = alpha * 0.65f), radius = r * 0.55f, center = Offset(cx, cy))
        }
    }

    private fun drawSky(
        scope: DrawScope,
        level: PlatformerLevelDef,
        assets: PlatformerAssets,
        viewportW: Float,
        viewportH: Float,
        camX: Float,
        scale: Float,
        animTime: Float,
    ) {
        val packBg = assets.packFor(level)?.background
        if (packBg != null) {
            val parallax = camX * scale * 0.22f
            val bgW = packBg.width.toFloat()
            val bgH = packBg.height.toFloat()
            val drawH = viewportH
            val drawW = drawH * (bgW / bgH)
            var x = -parallax % drawW
            if (x > 0f) x -= drawW
            while (x < viewportW) {
                scope.drawImage(
                    image = packBg,
                    dstOffset = IntOffset(x.toInt(), 0),
                    dstSize = IntSize(drawW.toInt().coerceAtLeast(1), drawH.toInt().coerceAtLeast(1)),
                )
                x += drawW
            }
            scope.drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.08f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.18f),
                    ),
                    startY = 0f,
                    endY = viewportH,
                ),
                size = Size(viewportW, viewportH),
            )
        } else {
            scope.drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(level.skyTop), Color(level.skyBottom)),
                    startY = 0f,
                    endY = viewportH,
                ),
                size = Size(viewportW, viewportH),
            )
            PlatformerScenicBackdrop.draw(
                scope = scope,
                level = level,
                viewportW = viewportW,
                viewportH = viewportH,
                camX = camX,
                scale = scale,
                animTime = animTime,
            )
        }
        if (packBg == null && level.id !in 1..6 && !PlatformerCampaignVisuals.isEndless(level)) {
            level.parallaxHill?.let { hillColor ->
                val baseY = viewportH * 0.72f
                val hillH = viewportH * 0.22f
                repeat(4) { i ->
                    val parallax = camX * scale * (0.12f + i * 0.06f)
                    val w = viewportW * 0.55f
                    val x = ((i * 280f - parallax) % (viewportW + w)) - w * 0.3f
                    scope.drawOval(
                        color = Color(hillColor).copy(alpha = 0.28f - i * 0.05f),
                        topLeft = Offset(x, baseY - hillH * (0.5f + i * 0.12f)),
                        size = Size(w, hillH),
                    )
                }
            }
            if (level.theme == PlatformerTheme.SPOOKY) {
                repeat(6) { i ->
                    val tx = ((i * 140f - camX * scale * 0.15f) % (viewportW + 120f)) - 60f
                    scope.drawRect(
                        color = Color(0xFF0A0A12),
                        topLeft = Offset(tx, viewportH * 0.08f),
                        size = Size(10f * scale.coerceIn(1f, 3f), viewportH * 0.5f),
                    )
                }
            }
            if (level.theme == PlatformerTheme.ICE) {
                repeat(16) { i ->
                    val sx = (i * 53f + animTime * 28f) % viewportW
                    val sy = (i * 37f + animTime * 18f) % viewportH
                    scope.drawCircle(Color.White.copy(alpha = 0.45f), radius = 2f, center = Offset(sx, sy))
                }
            }
        }
        if (level.theme == PlatformerTheme.PACK_WINTER) {
            repeat(24) { i ->
                val sx = (i * 47f + animTime * (20f + i)) % viewportW
                val sy = (i * 31f + animTime * 12f) % viewportH
                scope.drawCircle(Color.White.copy(alpha = 0.55f), radius = 2.2f, center = Offset(sx, sy))
            }
        }
    }

    private fun drawTiles(
        scope: DrawScope,
        world: PlatformerWorld,
        assets: PlatformerAssets,
        camX: Float,
        vp: PlatformerViewport,
        viewportW: Float,
    ) {
        val pack = assets.packFor(world.level)
        val stVisual = world.supertuxVisualTiles
        val stAtlas = assets.supertuxTileAtlas
        val tile = PLATFORMER_TILE_PX.toFloat()
        val startTx = (camX / tile).toInt().coerceAtLeast(0)
        val endTx = ((camX + vp.viewWorldW) / tile).toInt() + 2

        for (ty in 0 until world.height) {
            for (tx in startTx..minOf(endTx, world.width - 1)) {
                val idx = world.index(tx, ty)
                val visualId = stVisual?.getOrNull(idx) ?: 0
                if (visualId > 0 && stAtlas != null) {
                    val dst = tileRect(tx, ty, camX, vp)
                    if (dst.right < 0f || dst.left > viewportW) continue
                    stAtlas.tile(visualId)?.let { blitPackTile(scope, it, dst) }
                    continue
                }

                val cell = world.cellAt(tx, ty)
                if (cell == PlatformerCell.AIR || cell == PlatformerCell.SPAWN ||
                    cell == PlatformerCell.GOAL || cell == PlatformerCell.DECO ||
                    cell == PlatformerCell.BACKDROP || cell == PlatformerCell.SPIKE ||
                    cell == PlatformerCell.SPRING
                ) continue

                val dst = tileRect(tx, ty, camX, vp)
                if (dst.right < 0f || dst.left > viewportW) continue

                if (pack != null) {
                    if (cell == PlatformerCell.CRATE) {
                        val sprite = pack.crateSprite
                            ?: pack.pickSolidTile(true, true, true, true, false)
                        sprite?.let { blitPackTile(scope, it, dst) }
                        continue
                    }
                    val image = when (cell) {
                        PlatformerCell.SOLID -> {
                            val above = isSolid(world, tx, ty - 1)
                            val below = isSolid(world, tx, ty + 1)
                            val left = isSolid(world, tx - 1, ty)
                            val right = isSolid(world, tx + 1, ty)
                            pack.pickSolidTile(above, below, left, right, isPlatform = false)
                        }
                        PlatformerCell.PLATFORM -> pack.pickSolidTile(
                            solidAbove = false,
                            solidBelow = true,
                            solidLeft = isSolid(world, tx - 1, ty),
                            solidRight = isSolid(world, tx + 1, ty),
                            isPlatform = true,
                        )
                        else -> pack.pickSolidTile(true, true, true, true, false)
                    }
                    image?.let { blitPackTile(scope, it, dst) }
                        ?: drawFallbackTile(scope, world.level.theme, cell, dst)
                } else {
                    val theme = resolveTheme(world.level.theme, tx, ty)
                    val atlasTile = when (cell) {
                        PlatformerCell.SOLID -> {
                            val above = isSolid(world, tx, ty - 1)
                            val below = isSolid(world, tx, ty + 1)
                            val left = isSolid(world, tx - 1, ty)
                            val right = isSolid(world, tx + 1, ty)
                            PlatformerAtlas.pickSolidTile(theme, above, below, left, right, isPlatform = false)
                        }
                        PlatformerCell.PLATFORM -> PlatformerAtlas.tilesFor(theme).platform
                        else -> PlatformerAtlas.tilesFor(theme).fill
                    }
                    blitTile(scope, assets.atlas, atlasTile, dst)
                }
            }
        }
    }

    private fun drawPackBackdrops(
        scope: DrawScope,
        world: PlatformerWorld,
        assets: PlatformerAssets,
        camX: Float,
        vp: PlatformerViewport,
        viewportW: Float,
    ) {
        val pack = assets.packFor(world.level) ?: return
        val tree = pack.treeSprite ?: return
        val tile = PLATFORMER_TILE_PX.toFloat()
        val startTx = (camX / tile).toInt().coerceAtLeast(0)
        val endTx = ((camX + vp.viewWorldW) / tile).toInt() + 2
        val parallax = camX * vp.scale * 0.08f

        for (ty in 0 until world.height) {
            for (tx in startTx..minOf(endTx, world.width - 1)) {
                if (world.cellAt(tx, ty) != PlatformerCell.BACKDROP) continue
                val supportTy = (ty + 1..world.height - 1).firstOrNull { sy ->
                    isSolid(world, tx, sy)
                } ?: continue
                val feetY = vp.worldToScreenY((supportTy) * tile)
                val cx = vp.worldToScreenX(tx * tile + tile * 0.5f, camX) - parallax * 0.15f
                val targetW = vp.cell * 2.1f
                val aspect = tree.height.toFloat() / tree.width.coerceAtLeast(1)
                val targetH = targetW * aspect
                scope.drawImage(
                    image = tree,
                    dstOffset = IntOffset(
                        (cx - targetW * 0.5f).toInt(),
                        (feetY - targetH).toInt(),
                    ),
                    dstSize = IntSize(targetW.toInt().coerceAtLeast(1), targetH.toInt().coerceAtLeast(1)),
                )
            }
        }
    }

    private fun drawGoodlyBackdrops(
        scope: DrawScope,
        world: PlatformerWorld,
        camX: Float,
        vp: PlatformerViewport,
        viewportW: Float,
    ) {
        if (!PlatformerCampaignVisuals.showsGoodlyMapBackdrops(world.level)) return
        val tile = PLATFORMER_TILE_PX.toFloat()
        val startTx = (camX / tile).toInt().coerceAtLeast(0)
        val endTx = ((camX + vp.viewWorldW) / tile).toInt() + 2
        val treeColor = when (world.level.theme) {
            PlatformerTheme.DESERT -> Color(0xFF6D8B3C)
            PlatformerTheme.ICE -> Color(0xFF7EB6D4)
            PlatformerTheme.SPOOKY -> Color(0xFF1B263B)
            PlatformerTheme.METAL -> Color(0xFF3D4A5C)
            else -> Color(0xFF4A8F4A)
        }

        for (ty in 0 until world.height) {
            for (tx in startTx..minOf(endTx, world.width - 1)) {
                if (world.cellAt(tx, ty) != PlatformerCell.BACKDROP) continue
                val supportTy = (ty + 1..world.height - 1).firstOrNull { sy ->
                    isSolid(world, tx, sy)
                } ?: continue
                val feetY = vp.worldToScreenY(supportTy * tile)
                val cx = vp.worldToScreenX(tx * tile + tile * 0.5f, camX)
                val targetH = vp.cell * 1.35f
                val targetW = targetH * 0.9f
                scope.drawOval(
                    color = treeColor.copy(alpha = 0.62f),
                    topLeft = Offset(cx - targetW * 0.5f, feetY - targetH),
                    size = Size(targetW, targetH * 0.85f),
                )
                scope.drawRect(
                    color = treeColor.copy(alpha = 0.75f),
                    topLeft = Offset(cx - targetW * 0.08f, feetY - targetH * 0.28f),
                    size = Size(targetW * 0.16f, targetH * 0.32f),
                )
            }
        }
    }

    private fun drawPackDecor(
        scope: DrawScope,
        world: PlatformerWorld,
        assets: PlatformerAssets,
        camX: Float,
        vp: PlatformerViewport,
        viewportW: Float,
    ) {
        val pack = assets.packFor(world.level) ?: return
        val decos = pack.groundDecoSprites
        if (decos.isEmpty()) return
        val tile = PLATFORMER_TILE_PX.toFloat()
        val startTx = (camX / tile).toInt().coerceAtLeast(0)
        val endTx = ((camX + vp.viewWorldW) / tile).toInt() + 2

        for (ty in 0 until world.height) {
            for (tx in startTx..minOf(endTx, world.width - 1)) {
                if (world.cellAt(tx, ty) != PlatformerCell.DECO) continue
                val supportTy = ty + 1
                if (supportTy >= world.height || !isSolid(world, tx, supportTy)) continue
                val obj = decos[(tx * 13 + ty * 7) % decos.size]
                val feetY = vp.worldToScreenY(supportTy * tile)
                val cx = vp.worldToScreenX(tx * tile + tile * 0.5f, camX)
                val targetW = vp.cell * 0.95f
                val aspect = obj.height.toFloat() / obj.width.coerceAtLeast(1)
                val targetH = targetW * aspect
                scope.drawImage(
                    image = obj,
                    dstOffset = IntOffset(
                        (cx - targetW * 0.5f).toInt(),
                        (feetY - targetH).toInt(),
                    ),
                    dstSize = IntSize(targetW.toInt().coerceAtLeast(1), targetH.toInt().coerceAtLeast(1)),
                )
            }
        }
    }

    private fun tileRect(tx: Int, ty: Int, camX: Float, vp: PlatformerViewport): Rect {
        val tile = PLATFORMER_TILE_PX.toFloat()
        val left = vp.worldToScreenX(tx * tile, camX)
        val top = vp.worldToScreenY(ty * tile)
        return Rect(left, top, left + vp.cell, top + vp.cell)
    }

    private fun resolveTheme(base: PlatformerTheme, tx: Int, ty: Int): PlatformerTheme {
        if (base != PlatformerTheme.FORTRESS) return base
        return if ((tx / 4 + ty) % 2 == 0) PlatformerTheme.DESERT else PlatformerTheme.METAL
    }

    private fun isSolid(world: PlatformerWorld, tx: Int, ty: Int): Boolean {
        val c = world.cellAt(tx, ty)
        return c == PlatformerCell.SOLID || c == PlatformerCell.PLATFORM ||
            c == PlatformerCell.CRATE || c == PlatformerCell.SPRING
    }

    private fun drawFallbackTile(
        scope: DrawScope,
        theme: PlatformerTheme,
        cell: PlatformerCell,
        dst: Rect,
    ) {
        val color = when (cell) {
            PlatformerCell.PLATFORM -> Color(0xFF8D6E63)
            else -> when (theme) {
                PlatformerTheme.PACK_MINIMAL -> Color(0xFF37474F)
                PlatformerTheme.PACK_DESERT -> Color(0xFFD84315)
                PlatformerTheme.PACK_WINTER -> Color(0xFF90CAF9)
                PlatformerTheme.PACK_FOREST, PlatformerTheme.GRASS -> Color(0xFF43A047)
                PlatformerTheme.PACK_GRAVEYARD, PlatformerTheme.SPOOKY -> Color(0xFF5E35B1)
                PlatformerTheme.PACK_SCIFI -> Color(0xFF00838F)
                PlatformerTheme.PACK_GROTTO -> Color(0xFF546E7A)
                PlatformerTheme.PACK_JUNGLE -> Color(0xFF2E7D32)
                else -> Color(0xFF6D4C41)
            }
        }
        scope.drawRect(color, topLeft = Offset(dst.left, dst.top), size = Size(dst.width, dst.height))
        if (cell == PlatformerCell.PLATFORM) {
            scope.drawRect(
                color = Color.White.copy(alpha = 0.18f),
                topLeft = Offset(dst.left, dst.top),
                size = Size(dst.width, dst.height * 0.22f),
            )
        }
    }

    private fun blitPackTile(
        scope: DrawScope,
        image: ImageBitmap,
        dst: Rect,
    ) {
        val aspect = image.height.toFloat() / image.width.coerceAtLeast(1)
        val dstW = dst.width
        val dstH = dstW * aspect
        // 碰撞顶面在格子上沿，贴图顶对齐，避免平台/地块视觉悬空
        val top = dst.top
        scope.drawImage(
            image = image,
            dstOffset = IntOffset(dst.left.toInt(), top.toInt()),
            dstSize = IntSize(dstW.toInt().coerceAtLeast(1), dstH.toInt().coerceAtLeast(1)),
        )
    }

    private fun blitTile(scope: DrawScope, atlas: ImageBitmap, tile: AtlasTile, dst: Rect) {
        val cell = PLATFORMER_TILE_PX
        val src = tile.uvRect()
        scope.drawImage(
            image = atlas,
            srcOffset = IntOffset(src.left.toInt(), src.top.toInt()),
            srcSize = IntSize(cell, cell),
            dstOffset = IntOffset(dst.left.toInt(), dst.top.toInt()),
            dstSize = IntSize(dst.width.toInt().coerceAtLeast(1), dst.height.toInt().coerceAtLeast(1)),
        )
    }

    private fun drawGems(
        scope: DrawScope,
        world: PlatformerWorld,
        camX: Float,
        vp: PlatformerViewport,
        animTime: Float,
    ) {
        val pulse = 1f + 0.1f * sin(animTime * 5f)
        val r = vp.cell * 0.22f * pulse
        world.gems.filter { !it.collected }.forEach { g ->
            val cx = vp.worldToScreenX(g.x, camX)
            val cy = vp.worldToScreenY(g.y)
            val path = Path().apply {
                moveTo(cx, cy - r)
                lineTo(cx + r, cy)
                lineTo(cx, cy + r)
                lineTo(cx - r, cy)
                close()
            }
            scope.drawPath(path, Color(0xFF29B6F6))
            scope.drawPath(path, Color(0xFF01579B), style = Stroke(width = 2f))
        }
    }

    private fun drawGoal(
        scope: DrawScope,
        world: PlatformerWorld,
        camX: Float,
        vp: PlatformerViewport,
        animTime: Float,
    ) {
        val tile = world.tileF
        if (world.goalX != null && world.goalY != null) {
            drawGoalMarker(scope, vp, camX, world.goalX!!, world.goalY!!, animTime)
            return
        }
        for (y in 0 until world.height) {
            for (x in 0 until world.width) {
                if (world.cellAt(x, y) != PlatformerCell.GOAL) continue
                drawGoalMarker(scope, vp, camX, x * tile + tile / 2f, y * tile + tile / 2f, animTime)
            }
        }
    }

    private fun drawGoalMarker(
        scope: DrawScope,
        vp: PlatformerViewport,
        camX: Float,
        gx: Float,
        gy: Float,
        animTime: Float,
    ) {
        val cx = vp.worldToScreenX(gx, camX)
        val cy = vp.worldToScreenY(gy)
        val pulse = 1f + 0.12f * sin(animTime * 4f)
        val outer = vp.cell * 0.42f * pulse
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFE082), Color(0xFFFF8F00)),
                center = Offset(cx, cy),
                radius = outer,
            ),
            radius = outer,
            center = Offset(cx, cy),
        )
        scope.drawCircle(
            color = Color(0xFFFF6F00).copy(alpha = 0.7f),
            radius = outer,
            center = Offset(cx, cy),
            style = Stroke(width = 3f),
        )
    }

    private fun drawEnemies(
        scope: DrawScope,
        world: PlatformerWorld,
        assets: PlatformerAssets,
        camX: Float,
        vp: PlatformerViewport,
        animTime: Float,
    ) {
        world.enemies.filter { it.alive }.forEach { enemy ->
            val catalogId = enemy.catalogId
            if (catalogId != null) {
                val sheetDraw = PlatformerEnemySkinRenderer.resolveSheetDraw(
                    catalogId,
                    enemy.behavior,
                    enemy.animPhase,
                )
                if (sheetDraw != null) {
                    val ew = PlatformerEnemySystem.width(enemy.type, world.tilePx, catalogId)
                    val eh = PlatformerEnemySystem.height(enemy.type, world.tilePx, catalogId)
                    val layout = PlatformerEnemySkinRenderer.layoutForDrawSheet(
                        catalogId,
                        sheetDraw.sheet,
                        vp.cell,
                        world.tilePx,
                    )
                    val feetScreen = Offset(
                        vp.worldToScreenX(enemy.x + ew * 0.5f, camX),
                        vp.worldToScreenY(enemy.y + eh),
                    )
                    val mirror = PlatformerEnemySkinRenderer.mirrorHorizontally(
                        catalogId,
                        enemy.facingRight,
                    )
                    PlatformerSpriteDraw.drawSheetLayout(
                        scope = scope,
                        sheet = sheetDraw.sheet,
                        frameIndex = sheetDraw.frameIndex,
                        layout = layout,
                        feetScreen = feetScreen,
                        mirrorHorizontally = mirror,
                    )
                    return@forEach
                }
            }
            val set = assets.enemyAssets.byType[enemy.type] ?: return@forEach
            val strip = when (enemy.behavior) {
                PlatformerEnemyBehavior.FLY, PlatformerEnemyBehavior.FLOAT -> set.fly ?: set.move
                else -> set.move ?: set.idle
            } ?: return@forEach
            val frameCount = strip.frames.size.coerceAtLeast(1)
            val idx = (kotlin.math.floor(enemy.animPhase) % frameCount).toInt().coerceIn(0, frameCount - 1)
            val frame = strip.frames[idx]
            val dstH = vp.cell * 0.85f
            val aspect = frame.width.toFloat() / frame.height.coerceAtLeast(1)
            val dstW = dstH * aspect
            val left = vp.worldToScreenX(enemy.x, camX)
            val top = vp.worldToScreenY(enemy.y)
            val pivot = Offset(left + dstW / 2f, top + dstH)
            scope.withTransform({
                if (!enemy.facingRight) scale(scaleX = -1f, scaleY = 1f, pivot = pivot)
            }) {
                scope.drawImage(
                    image = frame,
                    dstOffset = IntOffset(left.toInt(), top.toInt()),
                    dstSize = IntSize(dstW.toInt().coerceAtLeast(1), dstH.toInt().coerceAtLeast(1)),
                )
            }
        }
    }

    private fun drawPlayer(
        scope: DrawScope,
        world: PlatformerWorld,
        assets: PlatformerAssets,
        camX: Float,
        vp: PlatformerViewport,
        animTime: Float,
    ) {
        val p = world.player
        val characterId = world.characterId
        if (characterId == PlatformerCharacterId.CHICK_PRO_MAX) {
            val sheetDraw = PlatformerPlayerSprites.resolveChickSheetDraw(p, animTime)
            if (sheetDraw != null) {
                val pw = PlatformerPhysics.playerW(world.tilePx)
                val layout = PlatformerPlayerSprites.layoutForDrawManifest(
                    sheetDraw.sheet,
                    p,
                    vp.cell,
                    world.tilePx,
                    sheetDraw.clip,
                    sheetDraw.frameIndex,
                )
                val footBottomWorld = p.y + PlatformerPhysics.playerH(world.tilePx)
                val footBottomScreen = vp.worldToScreenY(footBottomWorld)
                val centerWorldX = p.x + pw * 0.5f
                val centerScreenX = vp.worldToScreenX(centerWorldX, camX)
                val feetBefore = Offset(centerScreenX, footBottomScreen)
                val minHeadTopPx = PlatformerPlayerSprites.minHeadTopPxForPlayer(vp.cell, p)
                var feetScreen = PlatformerPlayerSprites.alignFeetScreenForSheet(
                    feetScreen = feetBefore,
                    player = p,
                    cellPx = vp.cell,
                )
                val drawLeft = feetScreen.x - layout.frameW * layout.feetXFrac + layout.frameOx
                val drawTop = feetScreen.y - layout.frameH * layout.feetYFrac + layout.frameOy
                val drawW = layout.frameW.roundToInt().coerceAtLeast(1)
                val drawH = layout.frameH.roundToInt().coerceAtLeast(1)
                PlatformerChickWalkDbg.onWalkDraw(
                    PlatformerChickWalkDbg.DrawSample(
                        clip = sheetDraw.clip,
                        frameIndex = sheetDraw.frameIndex,
                        frameCount = sheetDraw.sheet.frameCount,
                        path = "sheet",
                        layout = layout,
                        drawLeft = drawLeft,
                        drawTop = drawTop,
                        drawW = drawW,
                        drawH = drawH,
                        feetScreen = feetScreen,
                        animTime = animTime,
                        player = p,
                    ),
                )
                val mirror = PlatformerCharacterRenderer.mirrorHorizontally(characterId, p.facingRight)
                PlatformerChickLoadLog.logSheetDraw(
                    sheet = sheetDraw.sheet,
                    clip = sheetDraw.clip,
                    layout = layout,
                    path = "sheet",
                )
                PlatformerSpriteDraw.drawSheetLayout(
                    scope = scope,
                    sheet = sheetDraw.sheet,
                    frameIndex = sheetDraw.frameIndex,
                    layout = layout,
                    feetScreen = feetScreen,
                    mirrorHorizontally = mirror,
                    debugPlayer = p,
                    debugFeetBefore = feetBefore,
                    debugMinHeadTopPx = minHeadTopPx,
                )
                return
            }
            PlatformerChickWalkDbg.logPathMiss("sheet_unavailable")
        }
        if (characterId.isCatalogRemote) {
            val sheetDraw = PlatformerRemoteAnimCache.resolveSheetDraw(characterId, p, animTime)
            if (sheetDraw != null) {
                val pw = PlatformerPhysics.playerW(world.tilePx)
                val layout = PlatformerSkinRenderer.layoutForDrawSheet(
                    characterId, sheetDraw.sheet, p, vp.cell, world.tilePx,
                )
                val footBottomWorld = p.y + PlatformerPhysics.playerH(world.tilePx)
                val footBottomScreen = vp.worldToScreenY(footBottomWorld)
                val centerWorldX = p.x + pw * 0.5f
                val centerScreenX = vp.worldToScreenX(centerWorldX, camX)
                val feetScreen = Offset(centerScreenX, footBottomScreen)
                val mirror = PlatformerCharacterRenderer.mirrorHorizontally(characterId, p.facingRight)
                PlatformerSpriteDraw.drawSheetLayout(
                    scope = scope,
                    sheet = sheetDraw.sheet,
                    frameIndex = sheetDraw.frameIndex,
                    layout = layout,
                    feetScreen = feetScreen,
                    mirrorHorizontally = mirror,
                )
                drawAttackSlash(scope, p, feetScreen, vp, mirror)
                drawRangedMuzzleFlash(scope, p, feetScreen, vp, mirror)
                return
            }
        }
        val frame = PlatformerCharacterRenderer.resolveFrame(
            characterId, p, animTime, assets.characterAssets,
        )
        if (frame != null) {
            val pw = PlatformerPhysics.playerW(world.tilePx)
            val layout = when {
                characterId == PlatformerCharacterId.CHICK_PRO_MAX ->
                    PlatformerPlayerSprites.layoutForDraw(frame, p, vp.cell, world.tilePx)
                characterId.isCatalogRemote ->
                    PlatformerSkinRenderer.layoutForDraw(characterId, frame, p, vp.cell, world.tilePx)
                else -> {
                    val dstH = vp.cell * PlatformerPlayerSprites.drawHeightCells *
                        (world.tilePx / PLATFORMER_TILE_PX.toFloat()).coerceIn(0.5f, 1f)
                    val aspect = frame.width.toFloat() / frame.height.coerceAtLeast(1)
                    val anchor = PacMazeBitmapFeetAnchor.gameplayFeetAnchor(frame, PlatformerPlayerSprites.skinId)
                    val dstW = dstH * aspect
                    PlatformerPlayerSprites.ChickSpriteLayout(
                        dstW = dstW,
                        dstH = dstH,
                        feetYFrac = anchor.first,
                        feetXFrac = anchor.second,
                        frameW = dstW,
                        frameH = dstH,
                    )
                }
            }
            val footBottomWorld = p.y + PlatformerPhysics.playerH(world.tilePx)
            val footBottomScreen = vp.worldToScreenY(footBottomWorld)
            val centerWorldX = p.x + pw * 0.5f
            val centerScreenX = vp.worldToScreenX(centerWorldX, camX)
            val feetBefore = Offset(centerScreenX, footBottomScreen)
            val minHeadTopPx = PlatformerPlayerSprites.minHeadTopPxForPlayer(vp.cell, p)
            val feetScreen = if (characterId == PlatformerCharacterId.CHICK_PRO_MAX) {
                val grounded = PlatformerPlayerSprites.alignGroundedFeetScreen(
                    feetScreen = feetBefore,
                    player = p,
                    cellPx = vp.cell,
                )
                val stabilized = PlatformerPlayerSprites.stabilizeAirborneFeetScreen(
                    frame = frame,
                    layout = layout,
                    feetScreen = grounded,
                    player = p,
                )
                PlatformerPlayerSprites.adjustFeetScreenKeepHeadVisible(
                    frame = frame,
                    layout = layout,
                    feetScreen = stabilized,
                    minHeadTopPx = minHeadTopPx,
                    player = p,
                )
            } else {
                feetBefore
            }
            val mirror = PlatformerCharacterRenderer.mirrorHorizontally(characterId, p.facingRight)
            PlatformerSpriteDraw.drawChickLayout(
                scope = scope,
                frame = frame,
                layout = layout,
                feetScreen = feetScreen,
                mirrorHorizontally = mirror,
                debugPlayer = if (characterId == PlatformerCharacterId.CHICK_PRO_MAX) p else null,
                debugFeetBefore = if (characterId == PlatformerCharacterId.CHICK_PRO_MAX) feetBefore else null,
                debugMinHeadTopPx = if (characterId == PlatformerCharacterId.CHICK_PRO_MAX) minHeadTopPx else null,
            )
            return
        }

        val dstW = PlatformerPhysics.PLAYER_W * vp.scale
        val dstH = PlatformerPhysics.PLAYER_H * vp.scale
        val left = vp.worldToScreenX(p.x, camX)
        val top = vp.worldToScreenY(p.y)
        scope.drawRoundRect(
            color = Color(0xFFFF8C42),
            topLeft = Offset(left, top),
            size = Size(dstW, dstH),
            cornerRadius = CornerRadius(6f),
        )
    }

    private fun drawAttackSlash(
        scope: DrawScope,
        player: PlatformerPlayer,
        feetScreen: Offset,
        vp: PlatformerViewport,
        mirrorHorizontally: Boolean,
    ) {
        if (player.attackAnimSecLeft <= 0f) return
        val progress = PlatformerCombat.attackProgress(player)
        if (progress !in 0.12f..0.72f) return
        val facingRight = if (mirrorHorizontally) !player.facingRight else player.facingRight
        val swing = (progress - 0.12f) / 0.6f
        val alpha = (1f - kotlin.math.abs(swing - 0.5f) * 2f).coerceIn(0.15f, 0.85f)
        val reach = vp.cell * 0.95f
        val arcSpan = 78f
        val startAngle = if (facingRight) -20f - swing * 55f else 200f + swing * 55f
        val center = Offset(
            feetScreen.x + if (facingRight) reach * 0.35f else -reach * 0.35f,
            feetScreen.y - vp.cell * 0.55f,
        )
        scope.drawArc(
            color = Color(0xFFFFF59D).copy(alpha = alpha * 0.55f),
            startAngle = startAngle,
            sweepAngle = if (facingRight) -arcSpan else arcSpan,
            useCenter = false,
            topLeft = Offset(center.x - reach, center.y - reach),
            size = Size(reach * 2f, reach * 2f),
            style = Stroke(width = vp.cell * 0.14f),
        )
        scope.drawArc(
            color = Color.White.copy(alpha = alpha * 0.75f),
            startAngle = startAngle + if (facingRight) 8f else -8f,
            sweepAngle = if (facingRight) -arcSpan * 0.72f else arcSpan * 0.72f,
            useCenter = false,
            topLeft = Offset(center.x - reach * 0.82f, center.y - reach * 0.82f),
            size = Size(reach * 1.64f, reach * 1.64f),
            style = Stroke(width = vp.cell * 0.06f),
        )
    }

    private fun drawRangedMuzzleFlash(
        scope: DrawScope,
        player: PlatformerPlayer,
        feetScreen: Offset,
        vp: PlatformerViewport,
        mirrorHorizontally: Boolean,
    ) {
        if (player.rangedAnimSecLeft <= 0f) return
        val progress = PlatformerRangedCombat.rangedProgress(player)
        if (progress !in 0.38f..0.52f) return
        val facingRight = if (mirrorHorizontally) !player.facingRight else player.facingRight
        val alpha = (1f - kotlin.math.abs(progress - 0.45f) / 0.08f).coerceIn(0f, 1f) * 0.75f
        val cx = feetScreen.x + if (facingRight) vp.cell * 0.35f else -vp.cell * 0.35f
        val cy = feetScreen.y - vp.cell * 0.52f
        scope.drawCircle(Color(0xFF81D4FA).copy(alpha = alpha), vp.cell * 0.14f, Offset(cx, cy))
        scope.drawCircle(Color.White.copy(alpha = alpha * 0.8f), vp.cell * 0.06f, Offset(cx, cy))
    }

    private fun drawTraps(
        scope: DrawScope,
        world: PlatformerWorld,
        assets: PlatformerAssets,
        camX: Float,
        vp: PlatformerViewport,
        animTime: Float,
    ) {
        val tile = world.tileF
        for (trap in world.traps) {
            val left = vp.worldToScreenX(trap.x, camX)
            val top = vp.worldToScreenY(trap.y)
            when (trap.type) {
                PlatformerTrapType.LASER -> {
                    val active = PlatformerTrapSystem.laserActive(trap, animTime)
                    val color = if (active) Color(0xFFFF1744) else Color(0xFF455A64)
                    if (trap.axis == PlatformerTrapAxis.HORIZONTAL) {
                        val w = trap.span * vp.scale
                        scope.drawRect(
                            color.copy(alpha = if (active) 0.85f else 0.35f),
                            topLeft = Offset(left, top + vp.cell * 0.4f),
                            size = Size(w, vp.cell * 0.12f),
                        )
                    } else {
                        scope.drawRect(
                            color.copy(alpha = if (active) 0.85f else 0.35f),
                            topLeft = Offset(left + vp.cell * 0.4f, top),
                            size = Size(vp.cell * 0.12f, trap.span * vp.scale),
                        )
                    }
                }
                PlatformerTrapType.TURRET -> {
                    scope.drawRoundRect(
                        color = Color(0xFF546E7A),
                        topLeft = Offset(left, top),
                        size = Size(vp.cell * 0.55f, vp.cell * 0.45f),
                        cornerRadius = CornerRadius(4f),
                    )
                    scope.drawCircle(
                        Color(0xFFFF5722),
                        vp.cell * 0.08f,
                        Offset(left + vp.cell * 0.45f, top + vp.cell * 0.22f),
                    )
                }
                PlatformerTrapType.MOVING_SPIKE -> drawSpike(
                    scope,
                    Rect(left, top, left + vp.cell * 0.62f, top + vp.cell * 0.55f),
                )
                PlatformerTrapType.CRUSHER -> {
                    scope.drawRoundRect(
                        color = Color(0xFF37474F),
                        topLeft = Offset(left, top),
                        size = Size(vp.cell * 0.75f, vp.cell * 0.35f),
                        cornerRadius = CornerRadius(3f),
                    )
                }
            }
        }
        for (proj in world.projectiles.filter { it.alive }) {
            val px = vp.worldToScreenX(proj.x, camX)
            val py = vp.worldToScreenY(proj.y)
            when {
                proj.source == PlatformerProjectileSource.PLAYER &&
                    proj.shotKind == PlatformerPlayerShotKind.BASKETBALL -> {
                    val bmp = assets.chickBasketball
                    val d = (proj.radius * 2f * vp.scale).coerceAtLeast(vp.cell * 0.36f)
                    if (bmp != null) {
                        scope.withTransform({
                            rotate(
                                degrees = proj.spinRad * 180f / PI.toFloat(),
                                pivot = Offset(px + d * 0.5f, py + d * 0.5f),
                            )
                        }) {
                            scope.drawImage(
                                image = bmp,
                                dstOffset = IntOffset(px.toInt(), py.toInt()),
                                dstSize = IntSize(d.toInt().coerceAtLeast(1), d.toInt().coerceAtLeast(1)),
                            )
                        }
                    } else {
                        scope.drawCircle(Color(0xFFFF7043), d * 0.5f, Offset(px + d * 0.5f, py + d * 0.5f))
                    }
                }
                proj.source == PlatformerProjectileSource.PLAYER &&
                    proj.shotKind == PlatformerPlayerShotKind.KUNAI -> {
                    val w = vp.cell * 0.22f
                    val h = vp.cell * 0.06f
                    scope.drawRoundRect(
                        color = Color(0xFFB0BEC5),
                        topLeft = Offset(px - w * 0.5f, py - h * 0.5f),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(2f),
                    )
                }
                proj.source == PlatformerProjectileSource.PLAYER -> {
                    scope.drawCircle(Color(0xFF29B6F6), vp.cell * 0.1f, Offset(px, py))
                    scope.drawCircle(Color.White.copy(alpha = 0.7f), vp.cell * 0.04f, Offset(px, py))
                }
                else -> {
                    scope.drawCircle(Color(0xFFFF9100), vp.cell * 0.12f, Offset(px, py))
                }
            }
        }
    }

    private fun drawHazards(
        scope: DrawScope,
        world: PlatformerWorld,
        camX: Float,
        vp: PlatformerViewport,
        viewportW: Float,
        animTime: Float,
    ) {
        val tile = PLATFORMER_TILE_PX.toFloat()
        val startTx = max(0, (camX / tile).toInt() - 2)
        val endTx = min(
            world.width - 1,
            ((camX + viewportW / vp.scale) / tile).toInt() + 2,
        )
        for (ty in 0 until world.height) {
            for (tx in startTx..endTx) {
                val dst = tileRect(tx, ty, camX, vp)
                when (world.cellAt(tx, ty)) {
                    PlatformerCell.SPIKE -> drawSpike(scope, dst)
                    PlatformerCell.SPRING -> drawSpring(scope, dst, animTime)
                    else -> Unit
                }
            }
        }
    }

    private fun drawSpike(scope: DrawScope, dst: Rect) {
        val spikeColor = Color(0xFFE53935)
        val outline = Color(0xFF8B0000)
        val w = dst.width
        val h = dst.height
        val cx = dst.left + w * 0.5f
        val baseY = dst.top + h * 0.88f
        val tipY = dst.top + h * 0.38f
        for (i in -1..1) {
            val offset = i * w * 0.28f
            val halfW = w * 0.14f
            val path = Path().apply {
                moveTo(cx + offset - halfW, baseY)
                lineTo(cx + offset, tipY)
                lineTo(cx + offset + halfW, baseY)
                close()
            }
            scope.drawPath(path, spikeColor)
            scope.drawPath(path, outline, style = Stroke(width = 1.5f))
        }
    }

    private fun drawSpring(scope: DrawScope, dst: Rect, animTime: Float) {
        val padColor = Color(0xFF43A047)
        val coilColor = Color(0xFFFFD54F)
        val bounce = sin(animTime * 9f) * dst.height * 0.04f
        val padTop = dst.top + dst.height * 0.58f + bounce
        val padH = dst.height * 0.32f
        scope.drawRoundRect(
            color = padColor,
            topLeft = Offset(dst.left + dst.width * 0.08f, padTop),
            size = Size(dst.width * 0.84f, padH),
            cornerRadius = CornerRadius(dst.width * 0.08f),
        )
        val coilLeft = dst.left + dst.width * 0.18f
        val coilRight = dst.left + dst.width * 0.82f
        val coilTop = dst.top + dst.height * 0.42f + bounce
        val coilBot = padTop + padH * 0.15f
        val coils = 3
        val path = Path()
        for (i in 0 until coils) {
            val t0 = i / coils.toFloat()
            val t1 = (i + 1) / coils.toFloat()
            val y0 = coilTop + (coilBot - coilTop) * t0
            val y1 = coilTop + (coilBot - coilTop) * t1
            val x0 = if (i % 2 == 0) coilLeft else coilRight
            val x1 = if (i % 2 == 0) coilRight else coilLeft
            if (i == 0) path.moveTo(x0, y0) else path.lineTo(x0, y0)
            path.lineTo(x1, y1)
        }
        scope.drawPath(path, coilColor, style = Stroke(width = dst.width * 0.08f))
    }
}
