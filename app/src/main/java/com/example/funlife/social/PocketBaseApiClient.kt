package com.example.funlife.social

import android.content.Context
import com.example.funlife.social.game.model.GameRoomDto
import com.example.funlife.social.game.model.GameRoomStateCodec
import com.example.funlife.social.game.model.GameRoomStatus
import com.example.funlife.social.game.model.InviteMode
import com.example.funlife.social.model.FriendshipDto
import com.example.funlife.social.model.FriendshipStatus
import com.example.funlife.social.model.ConversationDto
import com.example.funlife.social.model.MessageDto
import com.example.funlife.social.model.PbUserProfile
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * PocketBase REST 客户端（OkHttp + Gson，与 VIP 层风格一致）。
 */
class PocketBaseApiClient(private val context: Context) {

    private val gson = Gson()
    private val http = PocketBaseHttp.client()
    private val jsonType = "application/json".toMediaType()

    private val apiBase: String
        get() = PocketBaseConfig.apiBase()

    // ── Auth ─────────────────────────────────────────────────────────────

    data class AuthResult(val token: String, val recordId: String)

    fun authWithPassword(identity: String, password: String): AuthResult {
        val body = mapOf("identity" to identity, "password" to password)
        val json = postJson("$apiBase/collections/users/auth-with-password", body, authToken = null)
        return parseAuth(json)
    }

    fun authRefresh(token: String): AuthResult {
        val json = postJson("$apiBase/collections/users/auth-refresh", emptyMap<String, Any>(), authToken = token)
        return parseAuth(json)
    }

    fun registerUser(
        identity: String,
        password: String,
        displayName: String,
        localUserId: Long,
        funlifeUsername: String,
    ): AuthResult {
        val body = mapOf(
            "email" to identity,
            "password" to password,
            "passwordConfirm" to password,
            "name" to displayName,
            "funlife_local_id" to localUserId,
            "funlife_username" to funlifeUsername,
            "online" to false,
        )
        postJson("$apiBase/collections/users/records", body, authToken = null)
        return authWithPassword(identity, password)
    }

    companion object {
        /** JWT 内的 PocketBase 用户 id，与 createRule 的 @request.auth.id 一致。 */
        fun recordIdFromToken(token: String): String {
            val parts = token.split('.')
            if (parts.size < 2) throw PocketBaseApiException(0, "无效 token")
            val payload = String(
                android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP),
                StandardCharsets.UTF_8,
            )
            val obj = JsonParser.parseString(payload).asJsonObject
            return obj.get("id")?.asString ?: throw PocketBaseApiException(0, "token 无 id")
        }
    }

    fun updateOnline(token: String, online: Boolean) {
        runCatching {
            patchJson(
                "$apiBase/collections/users/records/${recordIdFromToken(token)}",
                mapOf("online" to online),
                authToken = token,
            )
        }
    }

    /** 上传 FCM Token，供 PocketBase Hook 杀进程推送 */
    fun updatePushToken(token: String, recordId: String, fcmToken: String) {
        patchJson(
            "$apiBase/collections/users/records/$recordId",
            mapOf("fcm_token" to fcmToken),
            authToken = token,
        )
    }

    /** 上传本地头像到 PocketBase users.avatar，供好友搜索/列表展示 */
    fun uploadUserAvatar(token: String, recordId: String, avatarFile: File): String? {
        if (!avatarFile.exists() || !avatarFile.canRead()) return null
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "avatar",
                avatarFile.name,
                avatarFile.asRequestBody("image/jpeg".toMediaType()),
            )
            .build()
        val req = Request.Builder()
            .url("$apiBase/collections/users/records/$recordId")
            .patch(body)
            .header("Authorization", "Bearer $token")
            .build()
        val json = executeJson(req)
        val avatarFileName = json.get("avatar")?.asString?.takeIf { it.isNotBlank() }
        return avatarFileName?.let { fileUrlForRecord("users", recordId, it) }
    }

    // ── Users search ─────────────────────────────────────────────────────

    fun findUserByFunlifeUsername(token: String, username: String): PbUserProfile? {
        val filter = URLEncoder.encode(
            "funlife_username = '${escapeFilter(username)}'",
            StandardCharsets.UTF_8.name(),
        )
        val url = "$apiBase/collections/users/records?filter=$filter&perPage=1"
        val json = getJson(url, token)
        val items = json.getAsJsonArray("items")
        if (items == null || items.size() == 0) return null
        return parseUser(items[0].asJsonObject)
    }

    fun getUserById(token: String, userId: String): PbUserProfile? {
        if (userId.isBlank()) return null
        return runCatching {
            parseUser(getJson("$apiBase/collections/users/records/$userId", token))
        }.getOrNull()
    }

    /** 服务端用户是否仍存在；仅 404 视为已失效（换服/删库后本地缓存过期）。 */
    fun userRecordExists(token: String, recordId: String): Boolean {
        if (recordId.isBlank()) return false
        return runCatching {
            getJson("$apiBase/collections/users/records/$recordId", token)
            true
        }.getOrElse { e ->
            if (e is PocketBaseApiException && e.code == 404) false else true
        }
    }

    // ── Friendships ──────────────────────────────────────────────────────

    fun listFriendships(token: String, myPbId: String): List<FriendshipDto> {
        val filter = URLEncoder.encode(
            "(requester = '$myPbId' || addressee = '$myPbId') && status != 'blocked'",
            StandardCharsets.UTF_8.name(),
        )
        val expand = URLEncoder.encode("requester,addressee", StandardCharsets.UTF_8.name())
        val url = "$apiBase/collections/friendships/records?filter=$filter&expand=$expand&perPage=100&sort=-updated"
        val json = getJson(url, token)
        val items = json.getAsJsonArray("items") ?: return emptyList()
        return items.map { parseFriendship(it.asJsonObject) }
    }

    /** 仅拉「发给我的」待处理好友申请，供首页铃铛补拉（轻量） */
    fun listPendingIncoming(token: String, myPbId: String): List<FriendshipDto> {
        val filter = URLEncoder.encode(
            "addressee = '$myPbId' && status = 'pending'",
            StandardCharsets.UTF_8.name(),
        )
        val expand = URLEncoder.encode("requester", StandardCharsets.UTF_8.name())
        val url = "$apiBase/collections/friendships/records?filter=$filter&expand=$expand&perPage=50&sort=-created"
        val json = getJson(url, token)
        val items = json.getAsJsonArray("items") ?: return emptyList()
        return items.map { parseFriendship(it.asJsonObject) }
    }

    fun createFriendRequest(token: String, requesterId: String, addresseeId: String): FriendshipDto {
        val body = mapOf(
            "requester" to requesterId,
            "addressee" to addresseeId,
            "status" to FriendshipStatus.PENDING.wire,
        )
        val json = postJson("$apiBase/collections/friendships/records", body, authToken = token)
        val id = json.get("id").asString
        return getFriendship(token, id)
    }

    private fun getFriendship(token: String, friendshipId: String): FriendshipDto {
        val expand = URLEncoder.encode("requester,addressee", StandardCharsets.UTF_8.name())
        val url = "$apiBase/collections/friendships/records/$friendshipId?expand=$expand"
        return parseFriendship(getJson(url, token))
    }

    fun acceptFriendship(token: String, friendshipId: String): FriendshipDto {
        val json = patchJson(
            "$apiBase/collections/friendships/records/$friendshipId",
            mapOf("status" to FriendshipStatus.ACCEPTED.wire),
            authToken = token,
        )
        return parseFriendship(json)
    }

    fun deleteFriendship(token: String, friendshipId: String) {
        delete("$apiBase/collections/friendships/records/$friendshipId", token)
    }

    // ── Game rooms ───────────────────────────────────────────────────────

    fun listMyGameRooms(token: String, myPbId: String): List<GameRoomDto> {
        val filter = URLEncoder.encode(
            "((host = '$myPbId' || guest = '$myPbId' || game_state ~ '$myPbId') && " +
                "(status != 'cancelled' && status != 'expired' || host = '$myPbId'))",
            StandardCharsets.UTF_8.name(),
        )
        val expand = URLEncoder.encode("host,guest", StandardCharsets.UTF_8.name())
        val url = "$apiBase/collections/game_rooms/records?filter=$filter&expand=$expand&perPage=50&sort=-updated"
        val json = getJson(url, token)
        val items = json.getAsJsonArray("items") ?: return emptyList()
        return items.map { parseGameRoom(it.asJsonObject) }
    }

    /** 受邀方专用：排除自己为 host 的房间，避免 outbound 邀请误命中。 */
    fun listIncomingGameInvites(token: String, myPbId: String): List<GameRoomDto> {
        val filter = URLEncoder.encode(
            "status = 'waiting' && host != '$myPbId' && (guest = '$myPbId' || game_state ~ '$myPbId')",
            StandardCharsets.UTF_8.name(),
        )
        val expand = URLEncoder.encode("host,guest", StandardCharsets.UTF_8.name())
        val url = "$apiBase/collections/game_rooms/records?filter=$filter&expand=$expand&perPage=20&sort=-updated"
        val json = getJson(url, token)
        val items = json.getAsJsonArray("items") ?: return emptyList()
        return items.map { parseGameRoom(it.asJsonObject) }
    }

    fun findGameRoomByCode(token: String, roomCode: String): GameRoomDto? {
        val filter = URLEncoder.encode(
            "room_code = '${escapeFilter(roomCode.uppercase())}' && status = 'waiting' && invite_mode = 'open'",
            StandardCharsets.UTF_8.name(),
        )
        val expand = URLEncoder.encode("host,guest", StandardCharsets.UTF_8.name())
        val url = "$apiBase/collections/game_rooms/records?filter=$filter&expand=$expand&perPage=1"
        val json = getJson(url, token)
        val items = json.getAsJsonArray("items") ?: return null
        if (items.size() == 0) return null
        return parseGameRoom(items[0].asJsonObject)
    }

    fun getGameRoom(token: String, roomId: String): GameRoomDto {
        val expand = URLEncoder.encode("host,guest", StandardCharsets.UTF_8.name())
        val url = "$apiBase/collections/game_rooms/records/$roomId?expand=$expand"
        return parseGameRoom(getJson(url, token))
    }

    fun createGameRoom(token: String, body: Map<String, Any?>): GameRoomDto {
        val json = postJson("$apiBase/collections/game_rooms/records", body, authToken = token)
        val id = json.get("id").asString
        return runCatching { getGameRoom(token, id) }
            .getOrElse { parseGameRoom(json) }
    }

    fun updateGameRoom(token: String, roomId: String, patch: Map<String, Any?>): GameRoomDto {
        val json = patchJson("$apiBase/collections/game_rooms/records/$roomId", patch, authToken = token)
        return parseGameRoom(json)
    }

    fun listActiveRoomsForUser(token: String, myPbId: String): List<GameRoomDto> {
        val statusClause = GameRoomStatus.ACTIVE.joinToString(" || ") { "status = '${it.wire}'" }
        val filter = URLEncoder.encode(
            "(host = '$myPbId' || guest = '$myPbId' || game_state ~ '$myPbId') && ($statusClause)",
            StandardCharsets.UTF_8.name(),
        )
        val url = "$apiBase/collections/game_rooms/records?filter=$filter&perPage=20&sort=-updated"
        val json = getJson(url, token)
        val items = json.getAsJsonArray("items") ?: return emptyList()
        return items.map { parseGameRoom(it.asJsonObject) }
    }

    // ── Phase 2: Conversations / Messages ───────────────────────────────

    fun findConversationByPairKey(token: String, pairKey: String): ConversationDto? {
        val filter = URLEncoder.encode(
            "pair_key = '${escapeFilter(pairKey)}'",
            StandardCharsets.UTF_8.name(),
        )
        val url = "$apiBase/collections/conversations/records?filter=$filter&perPage=1"
        val json = getJson(url, token)
        val items = json.getAsJsonArray("items") ?: return null
        if (items.size() == 0) return null
        return parseConversation(items[0].asJsonObject)
    }

    fun createConversation(
        token: String,
        memberAId: String,
        memberBId: String,
        pairKey: String,
    ): ConversationDto {
        val body = mapOf(
            "member_a" to memberAId,
            "member_b" to memberBId,
            "pair_key" to pairKey,
            "last_preview" to "",
            "last_message_at" to 0,
        )
        val json = postJson("$apiBase/collections/conversations/records", body, authToken = token)
        return parseConversation(json)
    }

    fun findOrCreateConversation(
        token: String,
        myPbId: String,
        peerPbId: String,
    ): ConversationDto {
        val pairKey = SocialChatUtils.computePairKey(myPbId, peerPbId)
        findConversationByPairKey(token, pairKey)?.let { return it }
        val (memberA, memberB) = SocialChatUtils.orderedMembers(myPbId, peerPbId)
        return createConversation(token, memberA, memberB, pairKey)
    }

    fun listConversations(token: String, myPbId: String): List<ConversationDto> {
        val filter = URLEncoder.encode(
            "member_a = '$myPbId' || member_b = '$myPbId'",
            StandardCharsets.UTF_8.name(),
        )
        val url = "$apiBase/collections/conversations/records?filter=$filter&perPage=100&sort=-last_message_at,-updated"
        val json = getJson(url, token)
        val items = json.getAsJsonArray("items") ?: return emptyList()
        return items.map { parseConversation(it.asJsonObject) }
    }

    fun listMessages(
        token: String,
        conversationId: String,
        page: Int = 1,
        perPage: Int = 50,
    ): List<MessageDto> = listMessagesPage(token, conversationId, page, perPage).messages

    /** 分页拉取；newestFirst=true 时 page=1 为最新一页（用于聊天同步）。 */
    fun listMessagesPage(
        token: String,
        conversationId: String,
        page: Int = 1,
        perPage: Int = 50,
        newestFirst: Boolean = true,
    ): MessagePageResult {
        val filter = URLEncoder.encode(
            "conversation = '${escapeFilter(conversationId)}'",
            StandardCharsets.UTF_8.name(),
        )
        val sort = if (newestFirst) "-created" else "created"
        val url = "$apiBase/collections/messages/records?filter=$filter&perPage=$perPage&page=$page&sort=$sort"
        val json = getJson(url, token)
        val items = json.getAsJsonArray("items") ?: return MessagePageResult(emptyList(), hasMore = false)
        val currentPage = json.get("page")?.asInt ?: page
        val totalPages = json.get("totalPages")?.asInt ?: 1
        val messages = items.map { parseMessage(it.asJsonObject) }
            .sortedBy { it.createdAt }
        return MessagePageResult(
            messages = messages,
            hasMore = currentPage < totalPages,
        )
    }

    fun sendMessage(
        token: String,
        conversationId: String,
        senderPbId: String,
        memberAId: String,
        memberBId: String,
        body: String,
    ): MessageDto {
        val payload = mapOf(
            "conversation" to conversationId,
            "member_a" to memberAId,
            "member_b" to memberBId,
            "sender" to senderPbId,
            "body" to body,
        )
        val json = postJson("$apiBase/collections/messages/records", payload, authToken = token)
        val message = parseMessage(json)
        runCatching {
            patchJson(
                "$apiBase/collections/conversations/records/$conversationId",
                mapOf(
                    "last_preview" to SocialChatUtils.previewText(body),
                    "last_message_at" to message.createdAt,
                ),
                authToken = token,
            )
        }
        return message
    }

    // ── HTTP helpers ───────────────────────────────────────────────────

    private fun getJson(url: String, token: String): JsonObject {
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        return executeJson(req)
    }

    private fun postJson(url: String, body: Map<String, Any?>, authToken: String?): JsonObject {
        val reqBuilder = Request.Builder()
            .url(url)
            .post(gson.toJson(body).toRequestBody(jsonType))
        if (authToken != null) reqBuilder.header("Authorization", "Bearer $authToken")
        return executeJson(reqBuilder.build())
    }

    private fun patchJson(url: String, body: Map<String, Any?>, authToken: String): JsonObject {
        val req = Request.Builder()
            .url(url)
            .patch(gson.toJson(body).toRequestBody(jsonType))
            .header("Authorization", "Bearer $authToken")
            .build()
        return executeJson(req)
    }

    private fun delete(url: String, token: String) {
        val req = Request.Builder()
            .url(url)
            .delete()
            .header("Authorization", "Bearer $token")
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful && resp.code != 404) {
                throw PocketBaseApiException(resp.code, resp.body?.string().orEmpty())
            }
        }
    }

    private fun executeJson(req: Request): JsonObject {
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw PocketBaseApiException(resp.code, sanitizeError(text))
            }
            if (text.isBlank()) return JsonObject()
            return JsonParser.parseString(text).asJsonObject
        }
    }

    private fun parseAuth(json: JsonObject): AuthResult {
        val token = json.get("token")?.asString ?: throw PocketBaseApiException(0, "无 token")
        val record = json.getAsJsonObject("record")
            ?: throw PocketBaseApiException(0, "无 record")
        val id = record.get("id")?.asString ?: throw PocketBaseApiException(0, "无 record id")
        return AuthResult(token, id)
    }

    private fun authRecordId(token: String): String = recordIdFromToken(token)

    private fun parseUser(obj: JsonObject): PbUserProfile {
        val id = obj.get("id").asString
        val avatarFile = obj.get("avatar")?.asString?.takeIf { it.isNotBlank() }
        val avatarUrl = avatarFile?.let { fileUrlForRecord("users", id, it) }
        return PbUserProfile(
            id = id,
            funlifeUsername = obj.get("funlife_username")?.asString.orEmpty(),
            displayName = obj.get("name")?.asString ?: obj.get("funlife_username")?.asString.orEmpty(),
            avatarUrl = avatarUrl,
            online = obj.get("online")?.asBoolean ?: false,
            updatedAtMs = SocialChatUtils.parseCreatedAt(obj.get("updated")?.asString),
        )
    }

    private fun parseFriendship(obj: JsonObject): FriendshipDto {
        val expand = obj.getAsJsonObject("expand")
        val requesterId = relationId(obj.get("requester")).orEmpty()
        val addresseeId = relationId(obj.get("addressee")).orEmpty()
        val requester = expand?.getAsJsonObject("requester")?.let { parseUser(it) }
            ?: requesterId.takeIf { it.isNotBlank() }?.let {
                PbUserProfile(it, "", "", null, false)
            }
        val addressee = expand?.getAsJsonObject("addressee")?.let { parseUser(it) }
            ?: addresseeId.takeIf { it.isNotBlank() }?.let {
                PbUserProfile(it, "", "", null, false)
            }
        return FriendshipDto(
            id = obj.get("id").asString,
            requesterId = requesterId,
            addresseeId = addresseeId,
            status = FriendshipStatus.fromWire(obj.get("status")?.asString),
            requester = requester,
            addressee = addressee,
        )
    }

    private fun relationId(el: com.google.gson.JsonElement?): String? {
        if (el == null || el.isJsonNull) return null
        return when {
            el.isJsonPrimitive -> el.asString.takeIf { it.isNotBlank() }
            el.isJsonObject -> el.asJsonObject.get("id")?.asString
            else -> null
        }
    }

    private fun parseGameRoom(obj: JsonObject): GameRoomDto {
        val expand = obj.getAsJsonObject("expand")
        val hostId = relationId(obj.get("host")).orEmpty()
        val guestId = relationId(obj.get("guest"))
        val hostProfile = expand?.getAsJsonObject("host")?.let { parseUser(it) }
        val guestProfile = expand?.getAsJsonObject("guest")?.let { parseUser(it) }
        val gameState = GameRoomStateCodec.parse(obj.get("game_state"))
        val legacyDeclined = obj.get("game_state")?.takeIf { it.isJsonObject }?.asJsonObject
            ?.get("declined_by")?.asString == "guest"
        val declinedByPbId = gameState?.declinedByPbId
        val declinedByGuest = !declinedByPbId.isNullOrBlank() || legacyDeclined
        return GameRoomDto(
            id = obj.get("id").asString,
            gameType = obj.get("game_type")?.asString.orEmpty(),
            inviteMode = InviteMode.fromWire(obj.get("invite_mode")?.asString),
            roomCode = obj.get("room_code")?.asString.orEmpty(),
            hostPbId = hostId,
            guestPbId = guestId,
            status = GameRoomStatus.fromWire(obj.get("status")?.asString),
            hostReady = obj.get("host_ready")?.asBoolean ?: false,
            guestReady = obj.get("guest_ready")?.asBoolean ?: false,
            inviteMessage = obj.get("invite_message")?.asString.orEmpty(),
            declinedByGuest = !declinedByPbId.isNullOrBlank() || legacyDeclined,
            declinedByPbId = declinedByPbId,
            gameState = gameState,
            hostProfile = hostProfile,
            guestProfile = guestProfile,
            updatedAtMs = SocialChatUtils.parseCreatedAt(obj.get("updated")?.asString),
            createdAtMs = SocialChatUtils.parseCreatedAt(obj.get("created")?.asString),
        )
    }

    private fun parseConversation(obj: JsonObject): ConversationDto {
        return ConversationDto(
            id = obj.get("id").asString,
            memberAId = relationId(obj.get("member_a")).orEmpty(),
            memberBId = relationId(obj.get("member_b")).orEmpty(),
            pairKey = obj.get("pair_key")?.asString.orEmpty(),
            lastPreview = obj.get("last_preview")?.asString.orEmpty(),
            lastMessageAt = obj.get("last_message_at")?.asLong ?: 0L,
        )
    }

    private fun parseMessage(obj: JsonObject): MessageDto {
        val createdRaw = obj.get("created")?.asString
        val createdAt = SocialChatUtils.parseCreatedAt(createdRaw)
            .takeIf { it > 0L } ?: System.currentTimeMillis()
        return MessageDto(
            id = obj.get("id").asString,
            conversationId = relationId(obj.get("conversation")).orEmpty(),
            senderPbId = relationId(obj.get("sender")).orEmpty(),
            body = obj.get("body")?.asString.orEmpty(),
            createdAt = createdAt,
        )
    }

    private fun fileUrlForRecord(collection: String, recordId: String, fileName: String): String {
        val base = PocketBaseConfig.baseUrl()
        return if (fileName.startsWith("http")) fileName else "$base/api/files/$collection/$recordId/$fileName"
    }

    private fun escapeFilter(value: String): String =
        value.replace("\\", "\\\\").replace("'", "\\'")

    private fun sanitizeError(raw: String): String {
        return runCatching {
            val o = JsonParser.parseString(raw).asJsonObject
            o.getAsJsonObject("data")?.entrySet()?.firstOrNull()?.value?.asJsonObject?.get("message")?.asString
                ?: o.get("message")?.asString
                ?: "请求失败"
        }.getOrDefault("请求失败")
    }
}

class PocketBaseApiException(val code: Int, message: String) : Exception(message) {
    fun toUserMessage(fallback: String = "请求失败"): String = when (code) {
        404 -> "房间不存在或无权访问，可能已结束"
        403 -> "无权执行此操作"
        401 -> "登录已过期，请重新进入好友页同步"
        0 -> (message ?: fallback).ifBlank { fallback }
        else -> (message ?: fallback).ifBlank { fallback }
    }
}

data class MessagePageResult(
    val messages: List<MessageDto>,
    val hasMore: Boolean,
)
