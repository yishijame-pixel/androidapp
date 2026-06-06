package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "social_game_room_cache",
    primaryKeys = ["userId", "roomId"],
    indices = [
        Index("userId"),
        Index(value = ["userId", "updatedAtMs"]),
    ],
)
data class SocialGameRoomCache(
    val userId: Long,
    val roomId: String,
    val gameType: String,
    val inviteMode: String,
    val roomCode: String,
    val hostPbId: String,
    val guestPbId: String?,
    val hostDisplayName: String,
    val hostAvatarUrl: String,
    val guestProfileName: String,
    val guestProfileAvatar: String,
    val guestDisplayName: String,
    val peerAvatarUrl: String,
    val status: String,
    val inviteMessage: String,
    val declinedByGuest: Boolean,
    val declinedByPbId: String = "",
    val membersJson: String = "[]",
    val maxPlayers: Int = 2,
    val minPlayers: Int = 2,
    val pendingInvitePbId: String = "",
    val createdAtMs: Long,
    val updatedAtMs: Long,
)
