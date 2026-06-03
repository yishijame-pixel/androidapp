package com.example.funlife.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.funlife.data.model.SocialFriendCache
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
}
