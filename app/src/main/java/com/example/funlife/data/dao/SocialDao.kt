package com.example.funlife.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.funlife.data.model.SocialConversationCache
import com.example.funlife.data.model.SocialFriendCache
import com.example.funlife.data.model.SocialGameRoomCache
import com.example.funlife.data.model.SocialMessageCache
import com.example.funlife.data.model.SocialPocketBaseLink
import kotlinx.coroutines.flow.Flow

@Dao
interface SocialDao {

    @Query("SELECT * FROM social_pb_links WHERE userId = :userId LIMIT 1")
    suspend fun getLink(userId: Long): SocialPocketBaseLink?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLink(link: SocialPocketBaseLink)

    @Query("DELETE FROM social_pb_links WHERE userId = :userId")
    suspend fun deleteLink(userId: Long)

    @Query("SELECT * FROM social_friend_cache WHERE userId = :userId ORDER BY displayName COLLATE NOCASE")
    fun observeFriends(userId: Long): Flow<List<SocialFriendCache>>

    @Query("SELECT * FROM social_friend_cache WHERE userId = :userId ORDER BY displayName COLLATE NOCASE")
    suspend fun getFriends(userId: Long): List<SocialFriendCache>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFriends(items: List<SocialFriendCache>)

    @Query("DELETE FROM social_friend_cache WHERE userId = :userId")
    suspend fun clearFriends(userId: Long)

    @Query(
        "UPDATE social_friend_cache SET remark = :remark, updatedAt = :updatedAt " +
            "WHERE userId = :userId AND friendPbId = :friendPbId",
    )
    suspend fun updateRemark(userId: Long, friendPbId: String, remark: String, updatedAt: Long)

    @Query(
        "UPDATE social_friend_cache SET online = :online, updatedAt = :updatedAt " +
            "WHERE userId = :userId AND friendPbId = :friendPbId",
    )
    suspend fun updateFriendOnline(
        userId: Long,
        friendPbId: String,
        online: Boolean,
        updatedAt: Long,
    )

    @Query(
        "UPDATE social_friend_cache SET status = :status, updatedAt = :updatedAt " +
            "WHERE userId = :userId AND friendshipId = :friendshipId",
    )
    suspend fun updateFriendStatus(
        userId: Long,
        friendshipId: String,
        status: String,
        updatedAt: Long,
    )

    @Query("DELETE FROM social_friend_cache WHERE userId = :userId AND friendshipId = :friendshipId")
    suspend fun deleteFriendByFriendshipId(userId: Long, friendshipId: String)

    // ── Phase 2 私聊缓存（全部带 userId 过滤）──

    @Query(
        "SELECT * FROM social_conversation_cache WHERE userId = :userId " +
            "ORDER BY lastMessageAt DESC",
    )
    fun observeConversations(userId: Long): Flow<List<SocialConversationCache>>

    @Query("SELECT * FROM social_conversation_cache WHERE userId = :userId AND conversationId = :conversationId LIMIT 1")
    suspend fun getConversation(userId: Long, conversationId: String): SocialConversationCache?

    @Query("SELECT * FROM social_conversation_cache WHERE userId = :userId AND peerPbId = :peerPbId LIMIT 1")
    suspend fun getConversationByPeer(userId: Long, peerPbId: String): SocialConversationCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversations(items: List<SocialConversationCache>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(item: SocialConversationCache)

    @Query("DELETE FROM social_conversation_cache WHERE userId = :userId")
    suspend fun clearConversations(userId: Long)

    @Query(
        "SELECT * FROM social_message_cache WHERE userId = :userId AND conversationId = :conversationId " +
            "ORDER BY createdAt ASC",
    )
    fun observeMessages(userId: Long, conversationId: String): Flow<List<SocialMessageCache>>

    @Query(
        "SELECT * FROM social_message_cache WHERE userId = :userId AND conversationId = :conversationId " +
            "ORDER BY createdAt DESC LIMIT :limit",
    )
    suspend fun getRecentMessages(userId: Long, conversationId: String, limit: Int): List<SocialMessageCache>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessages(items: List<SocialMessageCache>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(item: SocialMessageCache)

    @Query("DELETE FROM social_message_cache WHERE userId = :userId")
    suspend fun clearMessages(userId: Long)

    @Query("DELETE FROM social_message_cache WHERE userId = :userId AND conversationId = :conversationId")
    suspend fun clearMessagesForConversation(userId: Long, conversationId: String)

    // ── 趣玩中心房间缓存 ──

    @Query(
        "SELECT * FROM social_game_room_cache WHERE userId = :userId " +
            "AND (status NOT IN ('cancelled', 'expired') OR declinedByGuest = 1) " +
            "ORDER BY updatedAtMs DESC",
    )
    fun observeGameRooms(userId: Long): Flow<List<SocialGameRoomCache>>

    @Query(
        "SELECT * FROM social_game_room_cache WHERE userId = :userId " +
            "AND (status NOT IN ('cancelled', 'expired') OR declinedByGuest = 1) " +
            "ORDER BY updatedAtMs DESC",
    )
    suspend fun getGameRooms(userId: Long): List<SocialGameRoomCache>

    @Query("SELECT * FROM social_game_room_cache WHERE userId = :userId AND roomId = :roomId LIMIT 1")
    suspend fun getGameRoom(userId: Long, roomId: String): SocialGameRoomCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGameRooms(items: List<SocialGameRoomCache>)

    @Query("DELETE FROM social_game_room_cache WHERE userId = :userId")
    suspend fun clearGameRooms(userId: Long)

    @Query("DELETE FROM social_game_room_cache WHERE userId = :userId AND roomId = :roomId")
    suspend fun deleteGameRoom(userId: Long, roomId: String)
}
