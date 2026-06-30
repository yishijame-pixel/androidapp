package com.example.funlife.game.platformer

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.funlife.resource.ResourceStore

/** 外部 128px 横版地砖包（Desert / Winter），编号与 Craftpix 布局一致。 */
data class PackTileMapping(
    val fill: Int = 5,
    val top: Int = 2,
    val left: Int = 4,
    val right: Int = 8,
    val bottom: Int = 7,
    val topLeft: Int = 1,
    val topRight: Int = 3,
    val platform: Int = 14,
)

data class PackObject(
    val assetName: String,
    val image: ImageBitmap,
)

data class PlatformerPackTileset(
    val pack: PlatformerTilesetPack,
    val tiles: Map<Int, ImageBitmap>,
    val mapping: PackTileMapping,
    val background: ImageBitmap?,
    val objects: List<PackObject>,
) {
    fun tile(index: Int): ImageBitmap? = tiles[index]

    fun objectNamed(keyword: String): ImageBitmap? =
        objects.firstOrNull { it.assetName.contains(keyword, ignoreCase = true) }?.image

    val crateSprite: ImageBitmap? get() = objectNamed("crate") ?: objectNamed("stoneblock")
    val treeSprite: ImageBitmap? get() = objectNamed("tree")
    val groundDecoSprites: List<ImageBitmap>
        get() = objects.filter { obj ->
            val n = obj.assetName
            !n.contains("tree", true) &&
                !n.contains("crate", true) &&
                !n.contains("stoneblock", true) &&
                !n.contains("igloo", true)
        }.map { it.image }

    fun pickSolidTile(
        solidAbove: Boolean,
        solidBelow: Boolean,
        solidLeft: Boolean,
        solidRight: Boolean,
        isPlatform: Boolean,
    ): ImageBitmap? {
        val m = mapping
        if (isPlatform) return tile(m.platform) ?: tile(m.top)
        val index = when {
            !solidAbove -> when {
                solidLeft && solidRight -> m.top
                solidLeft -> m.topRight
                solidRight -> m.topLeft
                else -> m.top
            }
            !solidBelow -> m.bottom
            !solidLeft && !solidRight -> m.fill
            !solidLeft -> m.left
            !solidRight -> m.right
            else -> m.fill
        }
        return tile(index) ?: tile(m.fill) ?: tiles.values.firstOrNull()
    }
}

object PlatformerPackTilesetLoader {

    private val defaultMapping = PackTileMapping()

    fun loadAll(context: Context): Map<PlatformerTilesetPack, PlatformerPackTileset> {
        val specs = listOf(
            packSpec(PlatformerTilesetPack.DESERT_PACK, "desert"),
            packSpec(PlatformerTilesetPack.WINTER_PACK, "winter"),
            packSpec(PlatformerTilesetPack.FOREST_PACK, "forest"),
            packSpec(PlatformerTilesetPack.GRAVEYARD_PACK, "graveyard"),
            packSpec(PlatformerTilesetPack.JUNGLE_PACK, "jungle"),
            packSpec(PlatformerTilesetPack.SCIFI_PACK, "scifi"),
            packSpec(PlatformerTilesetPack.GROTTO_PACK, "grotto"),
            packSpec(PlatformerTilesetPack.MINIMAL_PACK, "minimal", tileCount = 1),
        )
        return specs.mapNotNull { spec ->
            loadPack(context, spec.pack, spec.assetRoot, spec.tileDir, spec.objectDir, spec.tileCount)
                ?.let { spec.pack to it }
        }.toMap() + loadSuperTuxPack(context).let { map ->
            if (map != null) mapOf(map) else emptyMap()
        }
    }

    /** SuperTux 南极 tileset（`platformer_supertux/tilesets/antarctic`）。 */
    fun loadSuperTuxPack(context: Context): Pair<PlatformerTilesetPack, PlatformerPackTileset>? {
        val root = "platformer_supertux/tilesets/antarctic"
        if (ResourceStore.resolveFile("$root/tileset_manifest.json") == null &&
            runCatching { context.assets.open("$root/tileset_manifest.json").close() }.isFailure
        ) {
            return null
        }
        val tileCount = 14
        val tiles = mutableMapOf<Int, ImageBitmap>()
        for (i in 1..tileCount) {
            val name = i.toString().padStart(2, '0')
            decodeResource(context, "$root/tiles/$name.png")?.let { tiles[i] = it }
        }
        if (tiles.isEmpty()) return null
        val fill = tiles.values.first()
        for (i in 1..tileCount) {
            if (tiles[i] == null) tiles[i] = fill
        }
        val bg = decodeResource(context, "$root/background.png")
        return PlatformerTilesetPack.SUPERTUX to PlatformerPackTileset(
            pack = PlatformerTilesetPack.SUPERTUX,
            tiles = tiles,
            mapping = defaultMapping,
            background = bg,
            objects = emptyList(),
        )
    }

    private fun decodeResource(context: Context, path: String): ImageBitmap? {
        ResourceStore.openInputStream(path)?.use { stream ->
            return runCatching { BitmapFactory.decodeStream(stream)?.asImageBitmap() }.getOrNull()
        }
        return decodeAsset(context, path)
    }

    private data class PackSpec(
        val pack: PlatformerTilesetPack,
        val assetRoot: String,
        val tileDir: String = "tiles",
        val objectDir: String = "objects",
        val tileCount: Int = 18,
    )

    private fun packSpec(
        pack: PlatformerTilesetPack,
        folder: String,
        tileCount: Int = 18,
    ) = PackSpec(
        pack = pack,
        assetRoot = "platformer/tilesets/$folder",
        tileCount = tileCount,
    )

    private fun loadPack(
        context: Context,
        pack: PlatformerTilesetPack,
        assetRoot: String,
        tileDir: String,
        objectDir: String,
        tileCount: Int = 18,
    ): PlatformerPackTileset? {
        val tiles = mutableMapOf<Int, ImageBitmap>()
        for (i in 1..tileCount) {
            decodeAsset(context, "$assetRoot/$tileDir/$i.png")?.let { tiles[i] = it }
        }
        if (tiles.isEmpty()) return null
        val fill = tiles.values.first()
        for (i in 1..18) {
            if (tiles[i] == null) tiles[i] = fill
        }
        val mapping = if (pack == PlatformerTilesetPack.MINIMAL_PACK) {
            PackTileMapping(
                fill = 1, top = 1, left = 1, right = 1,
                bottom = 1, topLeft = 1, topRight = 1, platform = 1,
            )
        } else {
            defaultMapping
        }
        val bg = decodeAsset(context, "$assetRoot/bg.png")
            ?: decodeAsset(context, "$assetRoot/tileset.png")
        val objects = runCatching {
            context.assets.list("$assetRoot/$objectDir")
        }.getOrNull()
            ?.sorted()
            ?.take(24)
            ?.mapNotNull { name ->
                decodeAsset(context, "$assetRoot/$objectDir/$name")?.let { PackObject(name, it) }
            }
            .orEmpty()
        return PlatformerPackTileset(
            pack = pack,
            tiles = tiles,
            mapping = mapping,
            background = bg,
            objects = objects,
        )
    }

    private fun decodeAsset(context: Context, path: String): ImageBitmap? =
        runCatching {
            context.assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
        }.getOrNull()
}
