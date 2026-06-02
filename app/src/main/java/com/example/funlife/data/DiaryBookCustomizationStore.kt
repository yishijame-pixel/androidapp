// ═══════════════════════════════════════════════════════════════════════════
// DiaryBookCustomizationStore.kt — 魔法书封面定制（书名 / 刻印署名）
//
// SharedPreferences + userId 前缀隔离，遵循 DEVELOPMENT_PRINCIPLES §1.5。
// 不侵入 Room：轻量偏好，与 GoalExtrasStore 同模式。
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.data

import android.content.Context

/** 用户定制的魔法书封面文案。 */
data class BookCustomization(
    val bookTitle: String,
    val ownerName: String,
) {
    companion object {
        val Empty = BookCustomization(bookTitle = "", ownerName = "")
    }
}

object DiaryBookCustomizationStore {
    private const val PREF = "diary_book_customization"
    const val MAX_TITLE_LEN = 8
    const val MAX_OWNER_LEN = 8

    private fun prefs(c: Context) =
        c.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun keyTitle(userId: Long) = "u${userId}_title"
    private fun keyOwner(userId: Long) = "u${userId}_owner"

    fun load(c: Context, userId: Long): BookCustomization = BookCustomization(
        bookTitle = prefs(c).getString(keyTitle(userId), null)?.trim().orEmpty(),
        ownerName = prefs(c).getString(keyOwner(userId), null)?.trim().orEmpty(),
    )

    fun save(c: Context, userId: Long, bookTitle: String, ownerName: String) {
        val t = sanitizeTitle(bookTitle)
        val o = sanitizeOwner(ownerName)
        prefs(c).edit()
            .putString(keyTitle(userId), t)
            .putString(keyOwner(userId), o)
            .apply()
    }

    fun reset(c: Context, userId: Long) {
        prefs(c).edit()
            .remove(keyTitle(userId))
            .remove(keyOwner(userId))
            .apply()
    }

    fun sanitizeTitle(raw: String): String =
        raw.trim().take(MAX_TITLE_LEN)

    fun sanitizeOwner(raw: String): String =
        raw.trim().take(MAX_OWNER_LEN)

    /** 封面烫金主书名：空则回退默认「岁时录」。 */
    fun resolveTitle(custom: BookCustomization, defaultTitle: String): String =
        custom.bookTitle.ifBlank { defaultTitle }

    /** 封面刻印副标：有署名则紧凑连写，否则默认副标（保留诗意字距）。 */
    fun resolveOwnerLine(custom: BookCustomization, defaultSubtitle: String): String {
        val name = custom.ownerName
        if (name.isBlank()) return defaultSubtitle
        return name.take(MAX_OWNER_LEN)
    }

    /** 书脊 / 页摞竖排用字（最多 [max] 字，逐字列表）。 */
    fun spineOwnerChars(custom: BookCustomization, max: Int = 4): List<String> =
        custom.ownerName.trim().take(max.coerceAtLeast(1)).map { it.toString() }
}
