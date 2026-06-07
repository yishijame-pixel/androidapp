package com.example.funlife.social.game.model

import com.example.funlife.social.SocialChatUtils
import com.example.funlife.social.model.PbUserProfile
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/** PocketBase `game_rooms` record → DTO（SSE / REST 共用，无 expand 亦可）。 */
object GameRoomRecordParser {

    fun fromRecord(obj: JsonObject): GameRoomDto {
        val expand = obj.getAsJsonObject("expand")
        val hostId = relationId(obj.get("host")).orEmpty()
        val guestId = relationId(obj.get("guest"))
        val hostProfile = expand?.getAsJsonObject("host")?.let { parseUser(it) }
        val guestProfile = expand?.getAsJsonObject("guest")?.let { parseUser(it) }
        val gameState = GameRoomStateCodec.parse(obj.get("game_state"))
        val legacyDeclined = obj.get("game_state")?.takeIf { it.isJsonObject }?.asJsonObject
            ?.get("declined_by")?.asString == "guest"
        val declinedByPbId = gameState?.declinedByPbId
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
            currentTurnPbId = relationId(obj.get("current_turn")),
            winnerPbId = relationId(obj.get("winner")),
            hostProfile = hostProfile,
            guestProfile = guestProfile,
            updatedAtMs = SocialChatUtils.parseCreatedAt(obj.get("updated")?.asString),
            createdAtMs = SocialChatUtils.parseCreatedAt(obj.get("created")?.asString),
        )
    }

    fun isRelevantToUser(dto: GameRoomDto, myPbId: String): Boolean {
        val state = dto.gameState ?: return false
        if (dto.hostPbId == myPbId) return true
        if (dto.guestPbId == myPbId) return true
        if (state.pendingInvitePbId == myPbId) return true
        if (GameRoomStateCodec.isMember(state, myPbId)) return true
        return state.memberIds.contains(myPbId)
    }

    private fun relationId(el: JsonElement?): String? {
        if (el == null || el.isJsonNull) return null
        return when {
            el.isJsonPrimitive -> el.asString.takeIf { it.isNotBlank() }
            el.isJsonObject -> el.asJsonObject.get("id")?.asString
            else -> null
        }
    }

    private fun parseUser(obj: JsonObject): PbUserProfile {
        val id = obj.get("id")?.asString.orEmpty()
        val avatarFile = obj.get("avatar")?.asString?.takeIf { it.isNotBlank() }
        return PbUserProfile(
            id = id,
            funlifeUsername = obj.get("funlife_username")?.asString.orEmpty(),
            displayName = obj.get("name")?.asString
                ?: obj.get("funlife_username")?.asString.orEmpty(),
            avatarUrl = avatarFile,
            online = obj.get("online")?.asBoolean ?: false,
        )
    }
}
