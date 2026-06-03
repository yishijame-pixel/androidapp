package com.example.funlife.social.model

/** PocketBase friendships 集合 status 字段 */
enum class FriendshipStatus(val wire: String) {
    PENDING("pending"),
    ACCEPTED("accepted"),
    BLOCKED("blocked"),
    ;

    companion object {
        fun fromWire(value: String?): FriendshipStatus =
            entries.firstOrNull { it.wire == value } ?: PENDING
    }
}

data class PbUserProfile(
    val id: String,
    val funlifeUsername: String,
    val displayName: String,
    val avatarUrl: String?,
    val online: Boolean,
)

data class FriendshipDto(
    val id: String,
    val requesterId: String,
    val addresseeId: String,
    val status: FriendshipStatus,
    val requester: PbUserProfile?,
    val addressee: PbUserProfile?,
)

/** UI 层好友项（含 pending 方向） */
data class FriendUiModel(
    val friendshipId: String,
    val friendPbId: String,
    val funlifeUsername: String,
    val displayName: String,
    val avatarUrl: String?,
    val status: FriendshipStatus,
    val isIncomingRequest: Boolean,
    val remark: String,
)

sealed class SocialLinkState {
    data object NotConfigured : SocialLinkState()
    data object Linking : SocialLinkState()
    data class Linked(val pbUserId: String) : SocialLinkState()
    data class Error(val message: String) : SocialLinkState()
}

sealed class FriendsUiState {
    data object Loading : FriendsUiState()
    data class Ready(
        val friends: List<FriendUiModel>,
        val pendingIn: List<FriendUiModel>,
        val pendingOut: List<FriendUiModel>,
    ) : FriendsUiState()
    data class Error(val message: String) : FriendsUiState()
}
