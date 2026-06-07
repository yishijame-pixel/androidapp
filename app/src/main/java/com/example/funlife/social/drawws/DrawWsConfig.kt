package com.example.funlife.social.drawws

import com.example.funlife.BuildConfig
import com.example.funlife.social.PocketBaseConfig
import java.net.URI

object DrawWsConfig {

    /** 显式配置优先；否则从 PB 同域推导（同机房 / 反代 `/draw-ws`） */
    fun url(): String {
        val explicit = BuildConfig.DRAW_WS_URL.trim().trimEnd('/')
        if (explicit.isNotBlank()) return explicit
        return coLocatedUrlFromPb()
    }

    fun isEnabled(): Boolean {
        val u = url()
        if (u.isBlank()) return false
        if (u.startsWith("http://") && !BuildConfig.DEBUG) return false
        return u.startsWith("ws://") || u.startsWith("wss://")
    }

    /** 绘画热路径默认走二进制帧（JSON 仍用于 joined/replay） */
    fun useBinaryWire(): Boolean = isEnabled()

    /** 与 PocketBase 公网同源时适当拉长 ping */
    fun pingIntervalMs(): Long = if (url().startsWith("wss://")) 12_000L else 8_000L

    /** UI/发送节流：启用 WS 时走 4ms 热路径 */
    fun liveWireEnabled(): Boolean = isEnabled()

    /**
     * 同区域部署：PB `https://pb.example.com` → WS `wss://pb.example.com/draw-ws`
     * 开发态：`http://127.0.0.1:8090` → `ws://127.0.0.1:8790`
     */
    private fun coLocatedUrlFromPb(): String {
        val pb = PocketBaseConfig.baseUrl()
        if (pb.isBlank()) return ""
        return runCatching {
            when {
                pb.startsWith("https://") -> pb.replaceFirst("https://", "wss://") + "/draw-ws"
                pb.startsWith("http://") -> {
                    val uri = URI(pb)
                    val host = uri.host?.takeIf { it.isNotBlank() } ?: "127.0.0.1"
                    "ws://$host:8790"
                }
                else -> ""
            }
        }.getOrDefault("")
    }
}
