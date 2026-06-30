package com.example.funlife.game.platformer

import com.example.funlife.game.platformer.catalog.PlatformerUnlockProgress

/** 关卡解锁进度：委托 [PlatformerUnlockProgress] 持久化实现。 */
object PlatformerLevelProgress {

    val maxUnlockedLevelId: Int
        get() = PlatformerUnlockProgress.maxUnlockedLevelId

    var testUnlockAll: Boolean
        get() = PlatformerUnlockProgress.testUnlockAll
        set(value) {
            PlatformerUnlockProgress.testUnlockAll = value
        }

    fun isUnlocked(levelId: Int): Boolean = PlatformerUnlockProgress.isLevelUnlocked(levelId)

    fun unlockAllForTest() = PlatformerUnlockProgress.unlockAllForTest()

    fun resetTestUnlock() = PlatformerUnlockProgress.resetTestUnlock()

    fun onLevelCleared(levelId: Int) = PlatformerUnlockProgress.onLevelCleared(levelId)
}
