package com.example.funlife.game.platformer.catalog

import android.content.Context
import com.example.funlife.game.platformer.PlatformerEndlessRunner
import com.example.funlife.game.platformer.PlatformerSuperTuxLengthSpec
import com.example.funlife.game.platformer.PLATFORMER_TOTAL_LEVEL_COUNT

/** 角色 / 关卡解锁进度（SharedPreferences 持久化）。 */
object PlatformerUnlockProgress {

    private const val PREFS = "platformer_unlock"
    private const val KEY_MAX_LEVEL = "max_unlocked_level"
    private const val KEY_MAX_SUPERTUX_LEVEL = "max_unlocked_supertux_level"
    private const val KEY_ENDLESS_BEST = "endless_best_tiles"
    private const val KEY_UNLOCKED_CHARS = "unlocked_characters"
    private const val KEY_TEST_UNLOCK_ALL = "test_unlock_all"

    private var appContext: Context? = null

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    private fun requireContext(): Context =
        appContext ?: error("PlatformerUnlockProgress.init(context) must be called before use")

    private fun prefs() = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var testUnlockAll: Boolean
        get() = prefs().getBoolean(KEY_TEST_UNLOCK_ALL, false)
        set(value) {
            prefs().edit().putBoolean(KEY_TEST_UNLOCK_ALL, value).apply()
        }

    var maxUnlockedLevelId: Int
        get() = prefs().getInt(KEY_MAX_LEVEL, 1).coerceIn(1, PLATFORMER_TOTAL_LEVEL_COUNT)
        private set(value) {
            prefs().edit().putInt(KEY_MAX_LEVEL, value.coerceIn(1, PLATFORMER_TOTAL_LEVEL_COUNT)).apply()
        }

    /** 南极章 901 起默认解锁第一关试玩。 */
    var maxUnlockedSuperTuxLevelId: Int
        get() = prefs().getInt(
            KEY_MAX_SUPERTUX_LEVEL,
            PlatformerSuperTuxLengthSpec.SUPERTUX_LEVEL_START,
        ).coerceIn(
            PlatformerSuperTuxLengthSpec.SUPERTUX_LEVEL_START,
            PlatformerSuperTuxLengthSpec.SUPERTUX_LEVEL_END,
        )
        private set(value) {
            prefs().edit().putInt(
                KEY_MAX_SUPERTUX_LEVEL,
                value.coerceIn(
                    PlatformerSuperTuxLengthSpec.SUPERTUX_LEVEL_START,
                    PlatformerSuperTuxLengthSpec.SUPERTUX_LEVEL_END,
                ),
            ).apply()
        }

    var endlessBestTiles: Int
        get() = prefs().getInt(KEY_ENDLESS_BEST, 0)
        set(value) {
            prefs().edit().putInt(KEY_ENDLESS_BEST, value.coerceAtLeast(0)).apply()
        }

    fun isLevelUnlocked(levelId: Int): Boolean = when {
        testUnlockAll -> true
        PlatformerSuperTuxLengthSpec.isSuperTuxLevel(levelId) -> levelId <= maxUnlockedSuperTuxLevelId
        else -> levelId <= maxUnlockedLevelId
    }

    fun onLevelCleared(levelId: Int) {
        if (PlatformerSuperTuxLengthSpec.isSuperTuxLevel(levelId)) {
            val next = (levelId + 1).coerceAtMost(PlatformerSuperTuxLengthSpec.SUPERTUX_LEVEL_END)
            if (next > maxUnlockedSuperTuxLevelId) maxUnlockedSuperTuxLevelId = next
            return
        }
        val next = (levelId + 1).coerceAtMost(PLATFORMER_TOTAL_LEVEL_COUNT)
        if (next > maxUnlockedLevelId) maxUnlockedLevelId = next
    }

    fun unlockAllForTest() {
        testUnlockAll = true
        maxUnlockedLevelId = PLATFORMER_TOTAL_LEVEL_COUNT
        maxUnlockedSuperTuxLevelId = PlatformerSuperTuxLengthSpec.SUPERTUX_LEVEL_END
        unlockAllCharactersForTest()
    }

    fun resetTestUnlock() {
        testUnlockAll = false
    }

    fun isCharacterUnlocked(catalogId: String): Boolean {
        if (testUnlockAll) return true
        val entry = PlatformerContentCatalog.character(catalogId) ?: return false
        return when (entry.unlock.type) {
            "default" -> true
            "level_clear" -> maxUnlockedLevelId > entry.unlock.value
            "endless_tiles" -> endlessBestTiles >= entry.unlock.value
            "event" -> unlockedCharacterIds().contains(catalogId)
            else -> unlockedCharacterIds().contains(catalogId)
        }
    }

    fun onEndlessRunEnded(tilesRun: Int) {
        if (tilesRun > endlessBestTiles) endlessBestTiles = tilesRun
        if (tilesRun > PlatformerEndlessRunner.bestTilesRun) {
            PlatformerEndlessRunner.bestTilesRun = tilesRun
        }
    }

    private fun unlockedCharacterIds(): Set<String> =
        prefs().getStringSet(KEY_UNLOCKED_CHARS, emptySet()) ?: emptySet()

    private fun unlockAllCharactersForTest() {
        val all = PlatformerContentCatalog.requireLoaded().characters.map { it.id }.toSet()
        prefs().edit().putStringSet(KEY_UNLOCKED_CHARS, all).apply()
    }

    fun unlockCharacter(catalogId: String) {
        val set = unlockedCharacterIds().toMutableSet()
        set.add(catalogId)
        prefs().edit().putStringSet(KEY_UNLOCKED_CHARS, set).apply()
    }

    fun syncFromLegacy(progress: com.example.funlife.game.platformer.PlatformerLevelProgress) {
        if (progress.maxUnlockedLevelId > maxUnlockedLevelId) {
            maxUnlockedLevelId = progress.maxUnlockedLevelId
        }
        if (progress.testUnlockAll) unlockAllForTest()
    }
}
