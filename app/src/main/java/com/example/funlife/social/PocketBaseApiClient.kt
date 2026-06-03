package com.example.funlife.social

import android.content.Context
import com.example.funlife.social.model.FriendshipDto
import com.example.funlife.social.model.FriendshipStatus
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

    fun updateOnline(token: String, online: Boolean) {
        runCatching {
            patchJson(
                "$apiBase/collections/users/records/${authRecordId(token)}",
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

    private fun authRecordId(token: String): String {
        // JWT payload 中间段 base64 — 仅取 id 字段；失败则抛出让上层 refresh
        val parts = token.split('.')
        if (parts.size < 2) throw PocketBaseApiException(0, "无效 token")
        val payload = String(
            android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP),
            StandardCharsets.UTF_8,
        )
        val obj = JsonParser.parseString(payload).asJsonObject
        return obj.get("id")?.asString ?: throw PocketBaseApiException(0, "token 无 id")
    }

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

class PocketBaseApiException(val code: Int, message: String) : Exception(message)
