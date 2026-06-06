package com.example.funlife.social.game.model

import com.example.funlife.social.model.PbUserProfile

data class GameRoomDto(
    val id: String,
    val gameType: String,
    val inviteMode: InviteMode,
    val roomCode: String,
    val hostPbId: String,
    val guestPbId: String?,
    val status: GameRoomStatus,
    val hostReady: Boolean,
    val guestReady: Boolean,
    val inviteMessage: String,
    val declinedByGuest: Boolean,
    val declinedByPbId: String?,
    val gameState: GameRoomStatePayload?,
    val hostProfile: PbUserProfile?,
    val guestProfile: PbUserProfile?,
    val updatedAtMs: Long,
    val createdAtMs: Long,
)
