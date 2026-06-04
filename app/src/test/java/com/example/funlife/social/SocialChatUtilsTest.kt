package com.example.funlife.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialChatUtilsTest {

    @Test
    fun computePairKey_isOrderIndependent() {
        val a = "abc123456789012"
        val b = "xyz987654321098"
        assertEquals(SocialChatUtils.computePairKey(a, b), SocialChatUtils.computePairKey(b, a))
    }

    @Test
    fun orderedMembers_matchesPairKey() {
        val (mA, mB) = SocialChatUtils.orderedMembers("bbb", "aaa")
        assertEquals("aaa|bbb", SocialChatUtils.computePairKey(mA, mB))
        assertTrue(mA < mB)
    }

    @Test
    fun validateMessageBody_trimsAndRejectsEmpty() {
        assertTrue(SocialChatUtils.validateMessageBody("  hi  ").isSuccess)
        assertTrue(SocialChatUtils.validateMessageBody("   ").isFailure)
    }

    @Test
    fun validateMessageBody_rejectsTooLong() {
        val long = "a".repeat(SocialChatUtils.MAX_BODY_LEN + 1)
        assertTrue(SocialChatUtils.validateMessageBody(long).isFailure)
    }

    @Test
    fun previewText_collapsesNewlines() {
        assertEquals("hello world", SocialChatUtils.previewText("hello\nworld"))
    }

    @Test
    fun parseCreatedAt_pocketBaseSpaceFormat() {
        val ms = SocialChatUtils.parseCreatedAt("2024-06-05 12:34:56.789Z")
        assertTrue(ms > 0L)
        assertEquals(
            ms,
            SocialChatUtils.parseCreatedAt("2024-06-05T12:34:56.789Z"),
        )
    }
}
