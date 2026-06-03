package com.example.funlife.social

/**
 * 内存 Token 缓存：避免搜索/添加时每次 authRefresh 阻塞 UI。
 */
internal object SocialTokenCache {

    private const val TTL_MS = 12 * 60 * 1000L

    private data class Entry(val token: String, val cachedAtMs: Long)

    private val store = mutableMapOf<Long, Entry>()

    fun get(userId: Long): String? {
        val entry = store[userId] ?: return null
        if (System.currentTimeMillis() - entry.cachedAtMs > TTL_MS) {
            store.remove(userId)
            return null
        }
        return entry.token
    }

    fun put(userId: Long, token: String) {
        if (token.isBlank()) return
        store[userId] = Entry(token, System.currentTimeMillis())
    }

    fun clear(userId: Long) {
        store.remove(userId)
    }
}
