package com.example.funlife.game.platformer.tmx

import android.content.Context
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

object PlatformerTmxParser {

    fun load(context: Context, tmxAssetPath: String): PlatformerTmxMap {
        val dir = tmxAssetPath.substringBeforeLast('/')
        context.assets.open(tmxAssetPath).use { stream ->
            return parseMap(context, stream, dir)
        }
    }

    private fun parseMap(context: Context, stream: java.io.InputStream, assetDir: String): PlatformerTmxMap {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(stream, null)

        var width = 0
        var height = 0
        var tilePx = 8
        var tilesetSource: String? = null
        var tilesetFirstGid = 1
        var tilesetColumns = 32
        var tilesetImage = ""
        var backgroundPath: String? = null
        val layers = mutableMapOf<String, IntArray>()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "map" -> {
                        width = parser.getAttributeValue(null, "width")?.toIntOrNull() ?: 0
                        height = parser.getAttributeValue(null, "height")?.toIntOrNull() ?: 0
                        tilePx = parser.getAttributeValue(null, "tilewidth")?.toIntOrNull() ?: 8
                    }
                    "tileset" -> {
                        tilesetFirstGid = parser.getAttributeValue(null, "firstgid")?.toIntOrNull() ?: 1
                        tilesetSource = parser.getAttributeValue(null, "source")
                    }
                    "imagelayer" -> Unit
                    "image" -> {
                        val src = parser.getAttributeValue(null, "source")
                        if (src != null && backgroundPath == null && parser.depth <= 4) {
                            backgroundPath = "$assetDir/$src"
                        }
                    }
                    "layer" -> {
                        val name = parser.getAttributeValue(null, "name") ?: "layer"
                        val lw = parser.getAttributeValue(null, "width")?.toIntOrNull() ?: width
                        val lh = parser.getAttributeValue(null, "height")?.toIntOrNull() ?: height
                        val data = readLayerData(parser, lw * lh)
                        if (data != null) layers[name] = data
                    }
                }
            }
            event = parser.next()
        }

        if (tilesetSource != null) {
            val tsxPath = "$assetDir/$tilesetSource"
            context.assets.open(tsxPath).use { tsx ->
                val meta = parseTileset(tsx)
                tilesetImage = "$assetDir/${meta.image}"
                tilesetColumns = meta.columns
            }
        }

        return PlatformerTmxMap(
            assetDir = assetDir,
            width = width,
            height = height,
            tilePx = tilePx,
            tilesetPath = tilesetImage,
            tilesetColumns = tilesetColumns,
            tilesetFirstGid = tilesetFirstGid,
            backgroundPath = backgroundPath,
            layers = layers,
        )
    }

    private data class TsxMeta(val image: String, val columns: Int)

    private fun parseTileset(stream: java.io.InputStream): TsxMeta {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(stream, null)
        var image = ""
        var columns = 32
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "tileset" -> columns = parser.getAttributeValue(null, "columns")?.toIntOrNull() ?: 32
                    "image" -> image = parser.getAttributeValue(null, "source") ?: ""
                }
            }
            event = parser.next()
        }
        return TsxMeta(image, columns)
    }

    private fun readLayerData(parser: XmlPullParser, size: Int): IntArray? {
        var event = parser.next()
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "data") {
                val encoding = parser.getAttributeValue(null, "encoding")
                if (encoding == "csv") {
                    val text = readText(parser)
                    val values = text.split(',')
                        .map { it.trim().toIntOrNull() ?: 0 }
                    return IntArray(size) { i -> values.getOrElse(i) { 0 } }
                }
            }
            if (event == XmlPullParser.END_TAG && parser.name == "layer") return null
            event = parser.next()
        }
        return null
    }

    private fun readText(parser: XmlPullParser): String {
        var text = ""
        if (parser.next() == XmlPullParser.TEXT) {
            text = parser.text
            parser.nextTag()
        }
        return text
    }
}
