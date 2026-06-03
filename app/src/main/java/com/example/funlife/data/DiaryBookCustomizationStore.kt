// ═══════════════════════════════════════════════════════════════════════════
// DiaryBookCustomizationStore.kt — 魔法书封面定制（书名 / 刻印署名）
//
// 按 userId + skinId 隔离：每本皮肤的书独立书名与署名。
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
    private const val LEGACY_DEFAULT_SKIN = "builtin::hengwu"

    private fun prefs(c: Context) =
        c.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun keyTitle(userId: Long, skinId: String) = "u${userId}_${skinId}_title"
    private fun keyOwner(userId: Long, skinId: String) = "u${userId}_${skinId}_owner"

    /** 旧版全局 key（v55 前仅一本），迁移到默认皮肤册。 */
    private fun legacyKeyTitle(userId: Long) = "u${userId}_title"
    private fun legacyKeyOwner(userId: Long) = "u${userId}_owner"

    fun load(c: Context, userId: Long, skinId: String): BookCustomization {
        val p = prefs(c)
        var title = p.getString(keyTitle(userId, skinId), null)?.trim().orEmpty()
        var owner = p.getString(keyOwner(userId, skinId), null)?.trim().orEmpty()
        if (title.isBlank() && owner.isBlank() && skinId == LEGACY_DEFAULT_SKIN) {
            val legacyTitle = p.getString(legacyKeyTitle(userId), null)?.trim().orEmpty()
            val legacyOwner = p.getString(legacyKeyOwner(userId), null)?.trim().orEmpty()
            if (legacyTitle.isNotBlank() || legacyOwner.isNotBlank()) {
                title = legacyTitle
                owner = legacyOwner
                save(c, userId, skinId, title, owner)
            }
        }
        return BookCustomization(bookTitle = title, ownerName = owner)
    }

    fun save(c: Context, userId: Long, skinId: String, bookTitle: String, ownerName: String) {
        val t = sanitizeTitle(bookTitle)
        val o = sanitizeOwner(ownerName)
        prefs(c).edit()
            .putString(keyTitle(userId, skinId), t)
            .putString(keyOwner(userId, skinId), o)
            .apply()
    }

    fun reset(c: Context, userId: Long, skinId: String) {
        prefs(c).edit()
            .remove(keyTitle(userId, skinId))
            .remove(keyOwner(userId, skinId))
            .apply()
    }

    fun sanitizeTitle(raw: String): String =
        raw.trim().take(MAX_TITLE_LEN)

    fun sanitizeOwner(raw: String): String =
        raw.trim().take(MAX_OWNER_LEN)

    fun resolveTitle(custom: BookCustomization, defaultTitle: String): String =
        custom.bookTitle.ifBlank { defaultTitle }

    fun resolveOwnerLine(custom: BookCustomization, defaultSubtitle: String): String {
        val name = custom.ownerName
        if (name.isBlank()) return defaultSubtitle
        return name.take(MAX_OWNER_LEN)
    }

    fun spineOwnerChars(custom: BookCustomization, max: Int = 4): List<String> =
        custom.ownerName.trim().take(max.coerceAtLeast(1)).map { it.toString() }
}
