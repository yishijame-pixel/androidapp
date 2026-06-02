// BookChatSessionJsonRobustnessTest.kt — v54 防御性测试
//
// 验证 messagesJson 在以下"脏数据"情况下都不会让 App 崩溃：
//   - 空字符串
//   - "null"
//   - 非数组 JSON
//   - 缺字段 / 多字段
//   - 不认识的 role
//   - 不合法的 JSON 语法
//
// BookChatViewModel.decodeMessages 用 runCatching 兜底，期望：始终返回 List（可能为空）。
package com.example.funlife.data

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.junit.Test

class BookChatSessionJsonRobustnessTest {

    // 与 BookChatViewModel.StoredMsg 同结构（私有，这里 mirror）
    private data class StoredMsg(
        @SerializedName("role") val role: String,
        @SerializedName("text") val text: String,
        @SerializedName("ts") val ts: Long,
    )
    private val gson = Gson()

    /** 模拟 BookChatViewModel.decodeMessages 的解析路径。 */
    private fun decodeRobust(json: String): List<Pair<String, String>> {
        return runCatching {
            val arr = gson.fromJson(json, Array<StoredMsg>::class.java)
                ?: return@runCatching emptyList()
            arr.mapNotNull { m ->
                when (m.role) {
                    "user" -> "user" to m.text
                    "ai" -> "ai" to m.text
                    else -> null
                }
            }
        }.getOrDefault(emptyList())
    }

    @Test fun blank_string_returns_empty() {
        assertThat(decodeRobust("")).isEmpty()
    }

    @Test fun literal_null_returns_empty() {
        assertThat(decodeRobust("null")).isEmpty()
    }

    @Test fun non_array_returns_empty() {
        // 一个 object 而非 array
        assertThat(decodeRobust("""{"role":"user","text":"x","ts":1}""")).isEmpty()
    }

    @Test fun malformed_json_returns_empty() {
        assertThat(decodeRobust("[{not-valid")).isEmpty()
    }

    @Test fun valid_array_parses_correctly() {
        val json = """[
            {"role":"user","text":"hi","ts":1},
            {"role":"ai","text":"hello","ts":2}
        ]"""
        val r = decodeRobust(json)
        assertThat(r).containsExactly("user" to "hi", "ai" to "hello").inOrder()
    }

    @Test fun unknown_role_filteredOut() {
        val json = """[
            {"role":"user","text":"hi","ts":1},
            {"role":"unknown_role","text":"keep","ts":2},
            {"role":"ai","text":"ok","ts":3}
        ]"""
        val r = decodeRobust(json)
        assertThat(r).containsExactly("user" to "hi", "ai" to "ok").inOrder()
    }

    @Test fun missing_optional_fields_uses_default() {
        // 缺 ts 字段
        val json = """[{"role":"user","text":"x"}]"""
        val r = decodeRobust(json)
        assertThat(r).containsExactly("user" to "x")
    }

    @Test fun extra_fields_ignored() {
        val json = """[{"role":"user","text":"x","ts":1,"extra":"abc","more":42}]"""
        val r = decodeRobust(json)
        assertThat(r).containsExactly("user" to "x")
    }

    @Test fun empty_array_returns_empty() {
        assertThat(decodeRobust("[]")).isEmpty()
    }

    @Test fun very_long_history_does_not_crash() {
        // 模拟 200 轮长档案
        val items = (1..200).joinToString(",") { i ->
            val role = if (i % 2 == 0) "ai" else "user"
            """{"role":"$role","text":"msg-$i","ts":$i}"""
        }
        val json = "[$items]"
        val r = decodeRobust(json)
        assertThat(r).hasSize(200)
    }
}
