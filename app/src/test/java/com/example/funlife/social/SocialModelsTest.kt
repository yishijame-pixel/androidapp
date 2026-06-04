package com.example.funlife.social

import com.example.funlife.social.model.FriendshipStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SocialModelsTest {

    @Test
    fun friendshipStatus_fromWire() {
        assertEquals(FriendshipStatus.PENDING, FriendshipStatus.fromWire("pending"))
        assertEquals(FriendshipStatus.ACCEPTED, FriendshipStatus.fromWire("accepted"))
        assertEquals(FriendshipStatus.BLOCKED, FriendshipStatus.fromWire("blocked"))
        assertEquals(FriendshipStatus.PENDING, FriendshipStatus.fromWire(null))
        assertEquals(FriendshipStatus.PENDING, FriendshipStatus.fromWire("unknown"))
    }

    @Test
    fun friendshipStatus_wireRoundtrip() {
        FriendshipStatus.entries.forEach { s ->
            assertEquals(s, FriendshipStatus.fromWire(s.wire))
        }
    }
}
