package com.example.funlife.social

import com.example.funlife.BuildConfig

/** PocketBase 是否已配置（未配置时社交入口显示「服务未启用」）。 */
object PocketBaseConfig {

    fun baseUrl(): String = BuildConfig.POCKETBASE_URL.trim().trimEnd('/')

    /** 开发态允许 http；生产必须 https */
    fun isEnabled(): Boolean {
        val url = BuildConfig.POCKETBASE_URL.trim()
        if (url.isBlank()) return false
        if (url.startsWith("http://") && !BuildConfig.DEBUG) return false
        return true
    }

    /** 公网 HTTPS（经 Cloudflare 隧道）比局域网慢，需更长超时 */
    fun isRemote(): Boolean = baseUrl().startsWith("https://")

    fun apiBase(): String = baseUrl() + "/api"
}
