package com.example.funlife.resource

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.File

/**
 * 解码动画磁盘缓存索引（企业级 sidecar）。
 * 避免 listFiles() 扫描；O(1) 完整性校验；支持 png/webp 混合格式迁移期。
 */
object DecodedClipDiskIndex {

    const val META_FILENAME = "clip_meta.json"
    private val gson = Gson()

    data class Meta(
        @SerializedName("v") val schemaVersion: Int = SCHEMA_VERSION,
        @SerializedName("frameCount") val frameCount: Int,
        @SerializedName("decodeTag") val decodeTag: String,
        @SerializedName("format") val format: String = FORMAT_PNG,
        @SerializedName("frameWidth") val frameWidth: Int = 0,
        @SerializedName("frameHeight") val frameHeight: Int = 0,
        @SerializedName("writtenAtMs") val writtenAtMs: Long = System.currentTimeMillis(),
        @SerializedName("files") val files: List<String>? = null,
    ) {
        fun isComplete(expectedFrames: Int, expectedTag: String): Boolean =
            frameCount >= expectedFrames &&
                decodeTag == expectedTag &&
                schemaVersion == SCHEMA_VERSION
    }

    const val SCHEMA_VERSION = 2
    const val FORMAT_PNG = "png"
    const val FORMAT_WEBP = "webp"

    fun metaFile(dir: File): File = File(dir, META_FILENAME)

    fun read(dir: File): Meta? {
        if (!dir.isDirectory) return null
        val file = metaFile(dir)
        if (!file.isFile) return null
        return runCatching {
            gson.fromJson(file.readText(), Meta::class.java)
        }.getOrNull()?.takeIf { it.frameCount > 0 }
    }

    fun write(dir: File, meta: Meta) {
        dir.mkdirs()
        metaFile(dir).writeText(gson.toJson(meta))
    }

    /** O(1) 帧数；无 meta 时回退扫描（兼容旧缓存，并补写 meta）。 */
    fun frameCount(dir: File, decodeTag: String): Int {
        read(dir)?.takeIf { it.decodeTag == decodeTag }?.let { return it.frameCount }
        return scanAndMaybeWriteMeta(dir, decodeTag)
    }

    fun isComplete(dir: File, expectedFrames: Int, decodeTag: String): Boolean {
        if (expectedFrames <= 0) return false
        read(dir)?.let { meta ->
            return meta.isComplete(expectedFrames, decodeTag)
        }
        return scanAndMaybeWriteMeta(dir, decodeTag) >= expectedFrames
    }

    fun listFrameFiles(dir: File, decodeTag: String): List<File>? {
        if (!dir.isDirectory) return null
        read(dir)?.takeIf { it.decodeTag == decodeTag }?.files?.let { names ->
            val mapped = names.mapNotNull { name ->
                File(dir, name).takeIf { it.isFile }
            }
            if (mapped.size >= names.size) return mapped
        }
        val scanned = scanFrameFiles(dir)
        if (scanned.isNotEmpty()) {
            writeMetaFromFiles(dir, decodeTag, scanned)
        }
        return scanned.takeIf { it.isNotEmpty() }
    }

    fun writeMetaFromFiles(dir: File, decodeTag: String, files: List<File>, format: String = detectFormat(files)) {
        val sorted = files.sortedBy { frameIndex(it) ?: Int.MAX_VALUE }
        write(
            dir,
            Meta(
                frameCount = sorted.size,
                decodeTag = decodeTag,
                format = format,
                files = sorted.map { it.name },
            ),
        )
    }

    private fun scanAndMaybeWriteMeta(dir: File, decodeTag: String): Int {
        val files = scanFrameFiles(dir)
        if (files.isEmpty()) return 0
        writeMetaFromFiles(dir, decodeTag, files)
        return files.size
    }

    private fun scanFrameFiles(dir: File): List<File> =
        dir.listFiles()
            ?.filter { it.isFile && isFrameFile(it) }
            ?.sortedBy { frameIndex(it) ?: Int.MAX_VALUE }
            ?: emptyList()

    private fun isFrameFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext == "png" || ext == "webp"
    }

    private fun frameIndex(file: File): Int? =
        file.nameWithoutExtension.toIntOrNull()

    private fun detectFormat(files: List<File>): String {
        val webp = files.count { it.extension.equals("webp", ignoreCase = true) }
        return if (webp > files.size / 2) FORMAT_WEBP else FORMAT_PNG
    }
}
