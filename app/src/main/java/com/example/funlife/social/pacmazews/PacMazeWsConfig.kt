package com.example.funlife.social.pacmazews

import com.example.funlife.BuildConfig
import com.example.funlife.social.PocketBaseConfig
import java.net.URI

object PacMazeWsConfig {

    fun url(): String {
        val explicit = BuildConfig.PAC_MAZE_WS_URL.trim().trimEnd('/')
        if (explicit.isNotBlank()) return explicit
        return coLocatedUrlFromPb()
    }

    fun isEnabled(): Boolean {
        val u = url()
        if (u.isBlank()) return false
        if (u.startsWith("http://") && !BuildConfig.DEBUG) return false
        return u.startsWith("ws://") || u.startsWith("wss://")
    }

    fun pingIntervalMs(): Long = if (url().startsWith("wss://")) 12_000L else 8_000L

    /** 同区域：PB `http://127.0.0.1:8090` → `ws://127.0.0.1:8791` */
    private fun coLocatedUrlFromPb(): String {
        val pb = PocketBaseConfig.baseUrl()
        if (pb.isBlank()) return ""
        return runCatching {
            when {
                pb.startsWith("https://") -> pb.replaceFirst("https://", "wss://") + "/pac-maze-ws"
                pb.startsWith("http://") -> {
                    val uri = URI(pb)
                    val host = uri.host?.takeIf { it.isNotBlank() } ?: "127.0.0.1"
                    "ws://$host:8791"
                }
                else -> ""
            }
        }.getOrDefault("")
    }
}
