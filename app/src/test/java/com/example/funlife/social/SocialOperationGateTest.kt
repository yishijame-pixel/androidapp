package com.example.funlife.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** SocialOperationGate / SocialFailure 纯逻辑单元测试（无需 PocketBase）。 */
class SocialOperationGateTest {

    @Test
    fun normalizeUsername_stripsAtAndSpaces() {
        assertEquals("yishi", SocialOperationGate.normalizeUsername("  @yishi  "))
        assertEquals("abc", SocialOperationGate.normalizeUsername("@abc"))
    }

    @Test
    fun validateSearchQuery_rejectsTooShort() {
        val r = SocialOperationGate.validateSearchQuery("a")
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull() is SocialFailureException)
        assertEquals("用户名至少 2 个字符", (r.exceptionOrNull() as SocialFailureException).failure.userMessage)
    }

    @Test
    fun validateSearchQuery_acceptsValid() {
        val r = SocialOperationGate.validateSearchQuery("@yishi")
        assertTrue(r.isSuccess)
        assertEquals("yishi", r.getOrNull())
    }

    @Test
    fun socialFailure_mapsApiException() {
        val f = SocialFailure.fromThrowable(PocketBaseApiException(403, "Forbidden"))
        assertTrue(f is SocialFailure.Api)
        assertEquals("Forbidden", f.userMessage)
    }

    @Test
    fun socialFailure_mapsIOException() {
        val f = SocialFailure.fromThrowable(IOException("connection reset"))
        assertTrue(f is SocialFailure.Network)
        assertTrue(f.userMessage.contains("无法连接"))
    }

    @Test
    fun socialFailure_mapsValidation() {
        val f = SocialFailure.fromThrowable(IllegalArgumentException("不能添加自己"))
        assertTrue(f is SocialFailure.Validation)
    }

    @Test
    fun socialFailureException_preservesFailure() {
        val ex = SocialFailureException(SocialFailure.NotConfigured)
        assertEquals(SocialFailure.NotConfigured.userMessage, ex.message)
        assertEquals(SocialFailure.NotConfigured, ex.failure)
    }
}
