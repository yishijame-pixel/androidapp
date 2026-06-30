package com.example.funlife.game.platformer

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.funlife.resource.ResourceStore
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/** SuperTux 原生 tile id → 32px 贴图（`platformer_supertux/tilesets/antarctic/atlas`）。 */
class PlatformerSuperTuxTileAtlas(
    val tiles: Map<Int, ImageBitmap>,
) {
    fun tile(id: Int): ImageBitmap? = if (id <= 0) null else tiles[id]
}

object PlatformerSuperTuxTileAtlasLoader {

    private val gson = Gson()

    fun load(context: Context): PlatformerSuperTuxTileAtlas? {
        val manifestText = readText("platformer_supertux/tilesets/antarctic/atlas_manifest.json")
            ?: return null
        val manifest = runCatching {
            gson.fromJson(manifestText, AtlasManifest::class.java)
        }.getOrNull() ?: return null
        val tiles = mutableMapOf<Int, ImageBitmap>()
        manifest.tiles.forEach { (idStr, rel) ->
            val id = idStr.toIntOrNull() ?: return@forEach
            decodeResource(context, "platformer_supertux/tilesets/antarctic/$rel")?.let {
                tiles[id] = it
            }
        }
        return if (tiles.isEmpty()) null else PlatformerSuperTuxTileAtlas(tiles)
    }

    private data class AtlasManifest(
        @SerializedName("tiles") val tiles: Map<String, String> = emptyMap(),
    )

    private fun readText(path: String): String? {
        ResourceStore.resolveFile(path)?.readText()?.let { return it }
        return ResourceStore.openInputStream(path)?.bufferedReader()?.use { it.readText() }
    }

    private fun decodeResource(context: Context, path: String): ImageBitmap? {
        ResourceStore.openInputStream(path)?.use { stream ->
            return runCatching { BitmapFactory.decodeStream(stream)?.asImageBitmap() }.getOrNull()
        }
        return runCatching {
            context.assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
        }.getOrNull()
    }
}
