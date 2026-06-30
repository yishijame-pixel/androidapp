package com.example.funlife.game.platformer

/**
 * 主线埋点：关卡开始 / 通关 / 死亡，供调参与 CI 采样。
 */
object PlatformerCampaignTelemetry {

    data class LevelSession(
        val levelId: Int,
        val startedAtMs: Long,
        var deaths: Int = 0,
        var cleared: Boolean = false,
        var elapsedMs: Long = 0L,
    )

    private var active: LevelSession? = null
    val recentSessions = mutableListOf<LevelSession>()

    fun onLevelStart(levelId: Int) {
        active = LevelSession(levelId = levelId, startedAtMs = System.currentTimeMillis())
    }

    fun onDeath(levelId: Int) {
        active?.takeIf { it.levelId == levelId }?.deaths = (active?.deaths ?: 0) + 1
    }

    fun onLevelClear(levelId: Int) {
        val now = System.currentTimeMillis()
        val session = active?.takeIf { it.levelId == levelId } ?: return
        session.cleared = true
        session.elapsedMs = now - session.startedAtMs
        recentSessions += session
        if (recentSessions.size > 64) recentSessions.removeAt(0)
        active = null
    }

    fun reset() {
        active = null
        recentSessions.clear()
    }
}
