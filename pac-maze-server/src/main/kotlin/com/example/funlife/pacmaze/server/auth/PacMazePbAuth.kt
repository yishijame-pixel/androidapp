package com.example.funlife.pacmaze.server.auth

import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class PacMazeRoomMemberSummary(
    val host: String,
    val guest: String,
    val status: String,
    val arenaId: String = "arena_001",
    val matchSeed: Long = 0L,
    val hostPbId: String = "",
    val guestPbId: String = "",
)

data class PacMazeAuthResult(
    val userId: String,
    val room: PacMazeRoomMemberSummary,
    val isHost: Boolean,
    val entityId: String,
)

class PacMazePbAuth(
    private val pbBaseUrl: String,
    private val authTimeoutMs: Long = 4_000L,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(authTimeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(authTimeoutMs, TimeUnit.MILLISECONDS)
        .build()

    private data class TokenCache(val userId: String, val expMs: Long)
    private data class RoomCache(val room: PacMazeRoomMemberSummary, val expMs: Long)

    private val tokenCache = ConcurrentHashMap<String, TokenCache>()
    private val roomCache = ConcurrentHashMap<String, RoomCache>()

    fun authenticateJoin(token: String, roomId: String): PacMazeAuthResult {
        val userId = resolveUserId(token)
        val room = verifyRoomMember(token, roomId, userId)
        val isHost = userId == room.host
        val entityId = if (isHost) "pac_a" else "pac_b"
        return PacMazeAuthResult(
            userId = userId,
            room = room,
            isHost = isHost,
            entityId = entityId,
        )
    }

    private fun resolveUserId(token: String): String {
        val now = System.currentTimeMillis()
        tokenCache[token]?.let { if (now < it.expMs) return it.userId }
        val decoded = decodePbUserId(token)
        val userId = decoded.ifBlank { verifyPbToken(token) }
        tokenCache[token] = TokenCache(userId, now + TOKEN_CACHE_TTL_MS)
        return userId
    }

    private fun verifyRoomMember(token: String, roomId: String, userId: String): PacMazeRoomMemberSummary {
        val cacheKey = "$roomId:$userId"
        val now = System.currentTimeMillis()
        roomCache[cacheKey]?.let { if (now < it.expMs) return it.room }

        val base = pbBaseUrl.trimEnd('/')
        val req = Request.Builder()
            .url("$base/api/collections/game_rooms/records/${encodePath(roomId)}")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .get()
            .build()
        val body = client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("room_http_${resp.code}")
            resp.body?.string().orEmpty()
        }
        val root = JsonParser.parseString(body).asJsonObject
        val host = relationId(root.get("host"))
        val guest = relationId(root.get("guest"))
        if (userId != host && userId != guest) error("not_room_member")
        val gameType = root.get("game_type")?.asString?.trim().orEmpty()
        if (gameType != "pac_maze") error("not_pac_maze:$gameType")
        val gameState = root.getAsJsonObject("game_state")
        val pacMaze = gameState?.getAsJsonObject("pac_maze")
        val arenaId = pacMaze?.get("arena_id")?.asString?.trim()?.ifBlank { null } ?: "arena_001"
        val matchSeed = pacMaze?.get("match_seed")?.asLong ?: 0L
        val summary = PacMazeRoomMemberSummary(
            host = host,
            guest = guest,
            status = root.get("status")?.asString.orEmpty(),
            arenaId = arenaId,
            matchSeed = matchSeed,
            hostPbId = host,
            guestPbId = guest,
        )
        roomCache[cacheKey] = RoomCache(summary, now + ROOM_CACHE_TTL_MS)
        return summary
    }

    private fun verifyPbToken(token: String): String {
        val base = pbBaseUrl.trimEnd('/')
        val req = Request.Builder()
            .url("$base/api/collections/users/auth-refresh")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .post("{}".toRequestBody("application/json".toMediaType()))
            .build()
        val body = client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("auth_http_${resp.code}")
            resp.body?.string().orEmpty()
        }
        val root = JsonParser.parseString(body).asJsonObject
        return relationId(root.get("record")).ifBlank { relationId(root.get("model")) }
            .ifBlank { error("no_user_id") }
    }

    private fun decodePbUserId(token: String): String = runCatching {
        val parts = token.split(".")
        if (parts.size < 2) return ""
        val payload = String(Base64.getUrlDecoder().decode(padBase64(parts[1])))
        val root = JsonParser.parseString(payload).asJsonObject
        relationId(root.get("id")).ifBlank { relationId(root.get("record")) }
    }.getOrDefault("")

    private fun relationId(el: com.google.gson.JsonElement?): String {
        if (el == null || el.isJsonNull) return ""
        return when {
            el.isJsonPrimitive -> el.asString.trim()
            el.isJsonObject -> el.asJsonObject.get("id")?.asString?.trim().orEmpty()
            else -> ""
        }
    }

    private fun padBase64(part: String): String {
        val rem = part.length % 4
        return if (rem == 0) part else part + "=".repeat(4 - rem)
    }

    private fun encodePath(id: String): String =
        java.net.URLEncoder.encode(id, Charsets.UTF_8.name())

    companion object {
        private const val TOKEN_CACHE_TTL_MS = 5 * 60 * 1000L
        private const val ROOM_CACHE_TTL_MS = 60 * 1000L
    }
}
