package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import com.example.funlife.resource.ResourceStore
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

/**
 * 企业级皮肤动画清单：build 阶段 normalize 后写入，运行时唯一数据源。
 * 兼容 legacy（clips: { walk: 10 }）与 schema v2（完整 clip + canvas + anchor）。
 */
internal object PacMazeSkinAnimManifest {

    private const val MANIFEST_FILE = "anim_manifest.json"
    private val gson = Gson()
    private val cache = ConcurrentHashMap<String, SkinAnimManifest>()

    data class PlatformerFrameMetrics(
        @SerializedName("fy") val feetY: Float,
        @SerializedName("fx") val feetX: Float,
        @SerializedName("ty") val headTopY: Float,
        /** normalize 阶段 bbox 不透明宽度占比（相对格宽），局内定标用。 */
        @SerializedName("ow") val opaqueWidthFrac: Float? = null,
        /** normalize 阶段 bbox 不透明高度占比（相对格高）。 */
        @SerializedName("oh") val opaqueHeightFrac: Float? = null,
    )

    data class SkinAnimManifest(
        @SerializedName("schemaVersion") val schemaVersion: Int = 1,
        @SerializedName("skinId") val skinId: String,
        @SerializedName("normalized") val normalized: Boolean = false,
        @SerializedName("canvas") val canvas: CanvasSpec? = null,
        @SerializedName("anchorFrac") val anchorFrac: AnchorFrac? = null,
        @SerializedName("clips") val clips: Map<String, ClipEntry> = emptyMap(),
        @SerializedName("render") val render: RenderSpec? = null,
        /** build 阶段写入：横版按帧脚点/头顶，读盘 hydration 时 O(1) 注册。 */
        @SerializedName("platformerMetrics") val platformerMetrics: Map<String, List<PlatformerFrameMetrics>>? = null,
    ) {
        fun clipSet(): Set<PacMazeSkinAnimClip> = clips.keys.mapNotNull { name ->
            PacMazeSkinAnimClip.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
        }.toSet()

        fun frameCount(clip: PacMazeSkinAnimClip): Int =
            clips[clip.name.lowercase()]?.count ?: 0

        fun clipFolder(clip: PacMazeSkinAnimClip): String =
            clips[clip.name.lowercase()]?.folder ?: clip.folder

        fun clipPrefix(clip: PacMazeSkinAnimClip): String =
            clips[clip.name.lowercase()]?.prefix ?: clip.prefix

        fun clipSheet(clip: PacMazeSkinAnimClip): SheetSpec? =
            clips[clip.name.lowercase()]?.sheet

        fun clipSheetByKey(clipKey: String): SheetSpec? =
            clips[clipKey.lowercase()]?.sheet

        fun clipFolderByKey(clipKey: String): String =
            clips[clipKey.lowercase()]?.folder ?: clipKey.lowercase()

        fun frameCountByKey(clipKey: String): Int =
            clips[clipKey.lowercase()]?.count ?: 0

        fun hasSheet(clip: PacMazeSkinAnimClip): Boolean = clipSheet(clip) != null

        fun hasSheetByKey(clipKey: String): Boolean = clipSheetByKey(clipKey) != null

        fun platformerClipMetrics(clip: PacMazeSkinAnimClip): List<PlatformerFrameMetrics>? =
            platformerMetrics?.get(clip.name.lowercase())

        fun platformerFrameMetrics(clip: PacMazeSkinAnimClip, frameIndex: Int): PlatformerFrameMetrics? =
            platformerClipMetrics(clip)?.getOrNull(frameIndex)

        /**
         * build 阶段 platformerMetrics 推导的最小不透明内容占比（相对格宽高）。
         * 归一化 sheet 局内定标 O(1) 兜底，无需等像素扫描。
         */
        fun minOpaqueContentSpanFrac(clip: PacMazeSkinAnimClip): Pair<Float, Float>? {
            val metrics = platformerClipMetrics(clip) ?: return null
            if (metrics.isEmpty()) return null
            val minHeightFrac = metrics.minOf { m ->
                m.opaqueHeightFrac ?: (m.feetY - m.headTopY).coerceAtLeast(0.08f)
            }
            val minWidthFrac = metrics.minOf { m ->
                m.opaqueWidthFrac ?: run {
                    val fxSpread = (metrics.maxOf { it.feetX } - metrics.minOf { it.feetX })
                        .coerceAtLeast(0.08f)
                    if (normalized) maxOf(fxSpread * 2.4f, 0.30f) else fxSpread.coerceAtLeast(0.35f)
                }
            }.coerceAtMost(1f)
            return minHeightFrac to minWidthFrac
        }
    }

    data class CanvasSpec(
        @SerializedName("w") val w: Int,
        @SerializedName("h") val h: Int,
    )

    data class AnchorFrac(
        @SerializedName("x") val x: Float,
        @SerializedName("y") val y: Float,
    )

    data class ClipEntry(
        @SerializedName("count") val count: Int,
        @SerializedName("folder") val folder: String? = null,
        @SerializedName("prefix") val prefix: String? = null,
        @SerializedName("fps") val fps: Float? = null,
        @SerializedName("sheet") val sheet: SheetSpec? = null,
    )

    /** build 阶段 pack_sprite_sheets.py 写入：1 clip = 1 张精灵图 */
    data class SheetSpec(
        @SerializedName("file") val file: String,
        @SerializedName("columns") val columns: Int,
        @SerializedName("rows") val rows: Int,
        @SerializedName("cellW") val cellW: Int,
        @SerializedName("cellH") val cellH: Int,
    )

    data class RenderSpec(
        @SerializedName("invertBitmapFacing") val invertBitmapFacing: Boolean? = null,
        @SerializedName("syncWalkCycleToSprite") val syncWalkCycleToSprite: Boolean? = null,
        @SerializedName("sampleSize") val sampleSize: Int? = null,
    )

    fun load(assetRoot: String): SkinAnimManifest? {
        cache[assetRoot]?.let { return it }
        val stream = ResourceStore.openInputStream("$assetRoot/$MANIFEST_FILE") ?: return null
        return stream.use {
            runCatching {
                parseManifest(InputStreamReader(it, Charsets.UTF_8).readText().trimStart('\uFEFF'))
            }.getOrNull()?.also { manifest -> cache[assetRoot] = manifest }
        }
    }

    fun loadForSkin(skinId: com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId): SkinAnimManifest? {
        val assetRoot = PacMazeRemoteSkinAnimCatalog.assetRoot(skinId) ?: return null
        return load(assetRoot)
    }

    fun isNormalized(assetRoot: String): Boolean = load(assetRoot)?.normalized == true

    fun invalidateCache() {
        cache.clear()
    }

    /** 测试 / 注入用 */
    internal fun parseManifest(json: String): SkinAnimManifest {
        val root = gson.fromJson(json, JsonObject::class.java)
        val skinId = root.get("skinId")?.asString ?: error("skinId required")
        val schemaVersion = root.get("schemaVersion")?.asInt ?: 1
        val normalized = root.get("normalized")?.asBoolean ?: false
        val canvas = root.get("canvas")?.let { gson.fromJson(it, CanvasSpec::class.java) }
        val anchorFrac = root.get("anchorFrac")?.let { gson.fromJson(it, AnchorFrac::class.java) }
        val render = root.get("render")?.let { gson.fromJson(it, RenderSpec::class.java) }
        val platformerMetrics = root.get("platformerMetrics")?.let { el ->
            if (!el.isJsonObject) return@let null
            el.asJsonObject.entrySet().associate { (clipName, arr) ->
                clipName.lowercase() to arr.asJsonArray.map { item ->
                    gson.fromJson(item, PlatformerFrameMetrics::class.java)
                }
            }
        }
        val clips = parseClips(root.get("clips"))
        return SkinAnimManifest(
            schemaVersion = schemaVersion,
            skinId = skinId,
            normalized = normalized,
            canvas = canvas,
            anchorFrac = anchorFrac,
            clips = clips,
            render = render,
            platformerMetrics = platformerMetrics,
        )
    }

    fun platformerClipMetrics(
        skinId: com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
    ): List<PlatformerFrameMetrics>? = loadForSkin(skinId)?.platformerClipMetrics(clip)

    fun platformerFrameMetrics(
        skinId: com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        frameIndex: Int,
    ): PlatformerFrameMetrics? = loadForSkin(skinId)?.platformerFrameMetrics(clip, frameIndex)

    private fun parseClips(element: JsonElement?): Map<String, ClipEntry> {
        if (element == null || !element.isJsonObject) return emptyMap()
        val out = linkedMapOf<String, ClipEntry>()
        element.asJsonObject.entrySet().forEach { (name, value) ->
            val key = name.lowercase()
            out[key] = when {
                value.isJsonPrimitive && value.asJsonPrimitive.isNumber ->
                    ClipEntry(count = value.asInt)
                value.isJsonObject -> {
                    val obj = value.asJsonObject
                    ClipEntry(
                        count = obj.get("count")?.asInt ?: 0,
                        folder = obj.get("folder")?.asString,
                        prefix = obj.get("prefix")?.asString,
                        fps = obj.get("fps")?.takeIf { it.isJsonPrimitive }?.asFloat,
                        sheet = obj.get("sheet")?.let { gson.fromJson(it, SheetSpec::class.java) },
                    )
                }
                else -> return@forEach
            }
        }
        return out
    }
}
