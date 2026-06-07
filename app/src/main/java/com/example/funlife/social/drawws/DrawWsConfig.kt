package com.example.funlife.social.drawws

import com.example.funlife.BuildConfig

object DrawWsConfig {

    fun url(): String = BuildConfig.DRAW_WS_URL.trim().trimEnd('/')

    fun isEnabled(): Boolean {
        val u = url()
        if (u.isBlank()) return false
        if (u.startsWith("http://") && !BuildConfig.DEBUG) return false
        return u.startsWith("ws://") || u.startsWith("wss://")
    }

    /** 与 PocketBase 公网同源时适当拉长 ping */
    fun pingIntervalMs(): Long = if (url().startsWith("wss://")) 15_000L else 10_000L
}
