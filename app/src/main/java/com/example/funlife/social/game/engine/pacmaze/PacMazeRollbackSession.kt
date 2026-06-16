package com.example.funlife.social.game.engine.pacmaze

/**
 * GGPO 风格回滚会话：双端本地 [PacMazeOnlineSimulation]，只同步输入。
 *
 * - 显示位置 **永不** 从服务端快照覆盖
 * - 远端输入迟到 → 回滚到该 tick 重放
 * - 服务端快照仅用于阶段切换 / 严重 desync 硬同步
 */
class PacMazeRollbackSession(
    private var localEntityId: String,
    private var peerEntityId: String,
) {
    private var levelConfig: PacMazeLevelConfig? = null
    private var matchConfig: PacMazeOnlineMatchConfig? = null
    private var world: PacMazeWorldState? = null
    private var lastAdvanceMs: Long = 0L

    private val localInputs = PacMazeRollbackInputBuffer()
    private val remoteInputs = PacMazeRollbackInputBuffer()
    private val stateRing = PacMazeRollbackStateRing()

    private var lastLocalDir: Direction? = null
    private var lastRemoteDir: Direction? = null

    var rollbackCount: Int = 0
        private set
    var lastRollbackDepth: Int = 0
        private set

    fun updatePeers(localId: String, peerId: String) {
        localEntityId = localId
        peerEntityId = peerId
    }

    fun configure(level: PacMazeLevelConfig, match: PacMazeOnlineMatchConfig) {
        levelConfig = level
        matchConfig = match
    }

    fun reset(initial: PacMazeWorldState) {
        world = initial
        lastAdvanceMs = System.currentTimeMillis()
        localInputs.clear()
        remoteInputs.clear()
        stateRing.clear()
        stateRing.save(0L, initial)
        lastLocalDir = null
        lastRemoteDir = null
        rollbackCount = 0
        lastRollbackDepth = 0
    }

    fun currentWorld(): PacMazeWorldState? = world

    /** 本机即将发送的 tick（下一逻辑帧）。 */
    fun nextInputTick(): Long = (world?.tick ?: 0L) + 1L

    fun recordLocalInput(tick: Long, input: PacMazeTickInput) {
        localInputs.put(tick, input)
        if (input.committed != null) lastLocalDir = input.committed
    }

    fun onRemoteInput(tick: Long, input: PacMazeTickInput, attack: Boolean) {
        if (tick <= 0L) return
        val existing = remoteInputs.get(tick)
        if (existing != null && existing == input && !attack) return
        remoteInputs.put(tick, input)
        if (input.committed != null) lastRemoteDir = input.committed
        if (attack) remoteInputs.markAttack(tick)
        maybeRollbackAndResimulate(fromTick = tick)
    }

    fun onLocalAttack(tick: Long) {
        localInputs.markAttack(tick)
    }

    /**
     * 服务端校验包：仅同步非坐标权威数据；阶段结束或严重漂移才硬同步。
     */
    fun onAuthoritativeSync(server: PacMazeWorldState): Boolean {
        val sim = world ?: run {
            reset(server)
            return true
        }
        if (server.phase != PacMazePhase.PLAYING || sim.phase != PacMazePhase.PLAYING) {
            hardResync(server)
            return true
        }
        if (kotlin.math.abs(sim.tick - server.tick) > HARD_DESYNC_TICKS) {
            hardResync(server)
            return true
        }
        world = mergeMeta(sim, server)
        return false
    }

    fun advanceFrame(nowMs: Long, localInput: PacMazeTickInput): PacMazeWorldState? {
        val level = levelConfig ?: return world
        val match = matchConfig ?: return world
        var w = world ?: return null
        if (w.phase != PacMazePhase.PLAYING) return w

        val inputTick = w.tick + 1L
        localInputs.put(inputTick, localInput)
        if (localInput.committed != null) lastLocalDir = localInput.committed

        val stepMs = (nowMs - lastAdvanceMs).coerceIn(0L, 50L)
        lastAdvanceMs = nowMs
        if (stepMs <= 0L) return w

        val steps = (stepMs * PacMazeConstants.TICKS_PER_SECOND / 1_000L)
            .toInt()
            .coerceIn(1, PacMazeConstants.MAX_SIM_TICKS_PER_FRAME)
        repeat(steps) {
            w = stepOnce(w, level, match) ?: return world
        }
        world = w
        return w
    }

    private fun maybeRollbackAndResimulate(fromTick: Long) {
        val w = world ?: return
        if (fromTick > w.tick) return
        val checkpoint = stateRing.getAfterTick(fromTick - 1L) ?: return
        val targetTick = w.tick
        world = checkpoint
        rollbackCount++
        lastRollbackDepth = (targetTick - fromTick + 1).toInt()
        var t = fromTick
        val level = levelConfig ?: return
        val match = matchConfig ?: return
        while ((world?.tick ?: 0L) < targetTick) {
            world = stepOnce(world!!, level, match, t) ?: break
            t++
        }
    }

    private fun stepOnce(
        state: PacMazeWorldState,
        level: PacMazeLevelConfig,
        match: PacMazeOnlineMatchConfig,
        forcedTick: Long? = null,
    ): PacMazeWorldState? {
        if (state.phase != PacMazePhase.PLAYING) return state
        val tick = forcedTick ?: (state.tick + 1L)
        val localIn = localInputs.get(tick) ?: predict(lastLocalDir, tick)
        val remoteIn = remoteInputs.get(tick) ?: predict(lastRemoteDir, tick)
        val hostId = match.hostEntityId
        val guestId = match.guestEntityId
        val inputs = mapOf(
            hostId to if (localEntityId == hostId) localIn else remoteIn,
            guestId to if (localEntityId == guestId) localIn else remoteIn,
        )
        val attacks = buildSet {
            if (localInputs.hasAttack(tick) && localEntityId.isNotBlank()) add(localEntityId)
            if (remoteInputs.hasAttack(tick) && peerEntityId.isNotBlank()) add(peerEntityId)
        }
        val next = PacMazeOnlineSimulation.tick(state, inputs, level, match, attacks)
        stateRing.save(next.tick, next)
        return next
    }

    private fun predict(lastDir: Direction?, tick: Long): PacMazeTickInput =
        if (lastDir != null) PacMazeTickInput.committed(tick, lastDir)
        else PacMazeTickInput.Inactive.copy(tick = tick)

    private fun hardResync(server: PacMazeWorldState) {
        world = server
        stateRing.clear()
        stateRing.save(server.tick, server)
        localInputs.clear()
        remoteInputs.clear()
    }

    private fun mergeMeta(sim: PacMazeWorldState, server: PacMazeWorldState): PacMazeWorldState =
        sim.copy(
            playerScoreA = server.playerScoreA,
            playerScoreB = server.playerScoreB,
            playerLivesA = server.playerLivesA,
            playerLivesB = server.playerLivesB,
            onlineElapsedSeconds = server.onlineElapsedSeconds,
            onlineWinnerEntityId = server.onlineWinnerEntityId,
            onlineEndReason = server.onlineEndReason,
            phase = server.phase,
        )

    companion object {
        /** 约 2s @60Hz 才硬同步，日常不回拉坐标。 */
        const val HARD_DESYNC_TICKS = 120L
    }
}
