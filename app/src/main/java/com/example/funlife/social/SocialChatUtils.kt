package com.example.funlife.social

/** 私聊域纯函数（可单元测试，无 Android 依赖）。 */
object SocialChatUtils {

    fun computePairKey(pbIdA: String, pbIdB: String): String {
        val a = pbIdA.trim()
        val b = pbIdB.trim()
        require(a.isNotBlank() && b.isNotBlank()) { "pair key requires two pb ids" }
        require(a != b) { "cannot chat with self" }
        return if (a < b) "$a|$b" else "$b|$a"
    }

    fun orderedMembers(myPbId: String, peerPbId: String): Pair<String, String> {
        val pairKey = computePairKey(myPbId, peerPbId)
        val parts = pairKey.split("|", limit = 2)
        return parts[0] to parts[1]
    }

    fun validateMessageBody(raw: String): Result<String> {
        val body = raw.trim()
        return when {
            body.isEmpty() -> Result.failure(IllegalArgumentException("消息不能为空"))
            body.length > MAX_BODY_LEN -> Result.failure(IllegalArgumentException("消息最多 $MAX_BODY_LEN 字"))
            else -> Result.success(body)
        }
    }

    fun previewText(body: String): String =
        body.trim().replace('\n', ' ').take(PREVIEW_LEN)

    /**
     * PocketBase 返回 `2024-06-05 12:34:56.789Z`（空格分隔），
     * [java.time.Instant.parse] 只认 `T` 分隔，解析失败会导致消息排序颠倒。
     */
    fun parseCreatedAt(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        val normalized = raw.trim().replace(' ', 'T')
        return runCatching { java.time.Instant.parse(normalized).toEpochMilli() }
            .getOrElse {
                runCatching {
                    java.time.OffsetDateTime.parse(normalized).toInstant().toEpochMilli()
                }.getOrDefault(0L)
            }
    }

    const val MAX_BODY_LEN = 2000
    private const val PREVIEW_LEN = 120
}
