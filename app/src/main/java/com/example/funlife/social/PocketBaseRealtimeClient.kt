package com.example.funlife.social

import android.util.Log
import com.example.funlife.social.model.FriendUiModel
import com.example.funlife.social.model.FriendshipStatus
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.isActive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.source
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * PocketBase Realtime（SSE）：监听 friendships 新建，实现好友申请即时通知。
 */
class PocketBaseRealtimeClient {

    private val gson = Gson()
    private val jsonType = "application/json".toMediaType()
    private val sseClient = PocketBaseHttp.newBuilder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun listenFriendships(
        authToken: String,
        myPbId: String,
        onIncomingRequest: (FriendUiModel) -> Unit,
    ) {
        val url = "${PocketBaseConfig.apiBase()}/realtime"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .header("Authorization", "Bearer $authToken")
            .get()
            .build()

        val call = sseClient.newCall(request)
        val response = call.execute()
        if (!response.isSuccessful) {
            response.close()
            throw PocketBaseApiException(response.code, "Realtime 连接失败 ${response.code}")
        }

        val body = response.body ?: throw PocketBaseApiException(0, "Realtime 无响应体")
        try {
            val source = body.source()
            var eventName = ""
            val dataLines = StringBuilder()

            while (coroutineContext.isActive) {
                val line = source.readUtf8Line() ?: break
                when {
                    line.startsWith("event:") -> eventName = line.removePrefix("event:").trim()
                    line.startsWith("data:") -> {
                        if (dataLines.isNotEmpty()) dataLines.append('\n')
                        dataLines.append(line.removePrefix("data:").trim())
                    }
                    line.isEmpty() -> {
                        if (dataLines.isNotEmpty()) {
                            val data = dataLines.toString()
                            val ev = eventName.ifBlank { inferEventName(data) }
                            handleSseEvent(
                                eventName = ev,
                                data = data,
                                authToken = authToken,
                                myPbId = myPbId,
                                onIncomingRequest = onIncomingRequest,
                            )
                            dataLines.clear()
                            eventName = ""
                        }
                    }
                }
            }
        } finally {
            response.close()
            call.cancel()
        }
    }

    private fun inferEventName(data: String): String =
        if (data.contains("clientId")) "PB_CONNECT" else "PB_EVENT"

    private fun handleSseEvent(
        eventName: String,
        data: String,
        authToken: String,
        myPbId: String,
        onIncomingRequest: (FriendUiModel) -> Unit,
    ) {
        when {
            eventName == "PB_CONNECT" || data.contains("clientId") -> {
                val obj = JsonParser.parseString(data).asJsonObject
                val id = obj.get("clientId")?.asString ?: return
                // 必须同步订阅，避免 PB_EVENT 在 subscribe 完成前丢失
                val ok = subscribeFriendships(id, authToken)
                Log.d(TAG, "Realtime subscribe ok=$ok clientId=$id")
            }
            eventName == "PB_EVENT" || data.contains("\"action\"") -> {
                parseIncomingFriendRequest(data, myPbId)?.let {
                    Log.d(TAG, "incoming friend request ${it.friendshipId}")
                    onIncomingRequest(it)
                }
            }
        }
    }

    private fun subscribeFriendships(clientId: String, authToken: String): Boolean {
        val body = mapOf(
            "clientId" to clientId,
            // PocketBase 官方格式：集合名，非 friendships/*
            "subscriptions" to listOf("friendships"),
        )
        val req = Request.Builder()
            .url("${PocketBaseConfig.apiBase()}/realtime")
            .post(gson.toJson(body).toRequestBody(jsonType))
            .header("Authorization", "Bearer $authToken")
            .header("Content-Type", "application/json")
            .build()
        return sseClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                Log.w(TAG, "Realtime subscribe failed: ${resp.code} ${resp.body?.string()}")
                false
            } else {
                true
            }
        }
    }

    private fun parseIncomingFriendRequest(data: String, myPbId: String): FriendUiModel? {
        return runCatching {
            val root = JsonParser.parseString(data).asJsonObject
            val action = root.get("action")?.asString ?: return null
            if (action != "create") return null
            val record = root.getAsJsonObject("record") ?: return null
            val status = record.get("status")?.asString
            val addresseeId = relationId(record.get("addressee"))
            val requesterId = relationId(record.get("requester"))
            val friendshipId = record.get("id")?.asString
            if (status != FriendshipStatus.PENDING.wire ||
                addresseeId != myPbId ||
                friendshipId.isNullOrBlank() ||
                requesterId.isNullOrBlank()
            ) {
                return null
            }
            val expand = record.getAsJsonObject("expand")
            val requesterJson = expand?.getAsJsonObject("requester")
            val profile = requesterJson?.let { parseUserJson(it) }
            val username = profile?.funlifeUsername?.ifBlank { null } ?: requesterId.take(8)
            val displayName = profile?.displayName?.ifBlank { null }
                ?: profile?.funlifeUsername?.ifBlank { null }
                ?: "新好友"
            FriendUiModel(
                friendshipId = friendshipId,
                friendPbId = requesterId,
                funlifeUsername = username,
                displayName = displayName,
                avatarUrl = profile?.avatarUrl,
                status = FriendshipStatus.PENDING,
                isIncomingRequest = true,
                remark = "",
            )
        }.onFailure { Log.w(TAG, "parse PB_EVENT failed: ${it.message} data=${data.take(200)}") }
            .getOrNull()
    }

    private fun relationId(el: JsonElement?): String? {
        if (el == null || el.isJsonNull) return null
        return when {
            el.isJsonPrimitive -> el.asString.takeIf { it.isNotBlank() }
            el.isJsonObject -> el.asJsonObject.get("id")?.asString
            else -> null
        }
    }

    private fun parseUserJson(obj: JsonObject): com.example.funlife.social.model.PbUserProfile {
        val id = obj.get("id")?.asString.orEmpty()
        val avatarFile = obj.get("avatar")?.asString?.takeIf { it.isNotBlank() }
        val avatarUrl = avatarFile?.let {
            val base = PocketBaseConfig.baseUrl()
            "$base/api/files/users/$id/$it"
        }
        return com.example.funlife.social.model.PbUserProfile(
            id = id,
            funlifeUsername = obj.get("funlife_username")?.asString.orEmpty(),
            displayName = obj.get("name")?.asString
                ?: obj.get("funlife_username")?.asString.orEmpty(),
            avatarUrl = avatarUrl,
            online = obj.get("online")?.asBoolean ?: false,
        )
    }

    companion object {
        private const val TAG = "PbRealtime"
    }
}
