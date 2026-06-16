package com.example.funlife.social.game.model

import com.example.funlife.social.game.engine.DrawGuessWordBank
import com.example.funlife.social.game.engine.GomokuRules

/** 开局时初始化各游戏 play 子状态（纯函数，可单测）。 */
object PlayStateFactory {

    fun mergePlayIntoLobby(
        lobby: GameRoomStatePayload,
        gameId: String,
        hostPbId: String,
        guestPbId: String,
        timerPreset: GomokuTimerPreset = GomokuTimerPreset.STANDARD,
        forbiddenEnabled: Boolean = true,
    ): GameRoomStatePayload = when (gameId) {
        "gomoku" -> lobby.copy(
            gomoku = GomokuPlayState(
                board = GomokuRules.emptyBoard(),
                moveCount = 0,
                blackPbId = hostPbId,
                whitePbId = guestPbId,
                timer = timerPreset.createState(),
                forbiddenEnabled = forbiddenEnabled,
            ),
        )
        "draw_guess" -> lobby.copy(
            drawGuess = DrawGuessPlayState(
                round = 1,
                phase = DrawGuessPhase.DRAWING.wire,
                drawerPbId = hostPbId,
                word = DrawGuessWordBank.randomWord(),
                scores = mapOf(hostPbId to 0, guestPbId to 0),
                drawSeconds = 60,
                phaseStartedAtMs = System.currentTimeMillis(),
            ),
        )
        "pac_maze" -> mergePacMazePlay(lobby, hostPbId, guestPbId)
        else -> lobby
    }

    fun initialPacMazeLobby(
        hostPbId: String,
        matchMode: String = "versus_duel",
        levelId: Int = 1,
        versusRule: String = "race_pellets",
        timeLimitSec: Int = 150,
    ): PacMazePlayState = PacMazePlayState(
        matchMode = matchMode,
        versusRule = versusRule,
        levelId = levelId,
        timeLimitSec = timeLimitSec,
        hostPbId = hostPbId,
        playerA = PacMazePlayerMeta(
            pbId = hostPbId,
            entityId = "pac_a",
            ready = true,
        ),
    )

    private fun mergePacMazePlay(
        lobby: GameRoomStatePayload,
        hostPbId: String,
        guestPbId: String,
    ): GameRoomStatePayload {
        val existing = lobby.pacMaze ?: initialPacMazeLobby(hostPbId)
        val seed = existing.matchSeed.takeIf { it != 0L }
            ?: (hostPbId.hashCode().toLong() xor guestPbId.hashCode().toLong() xor System.currentTimeMillis())
        val arenaIndex = (kotlin.math.abs(seed) % 3).toInt()
        val arenaId = "arena_00${arenaIndex + 1}"
        return lobby.copy(
            pacMaze = existing.copy(
                matchSeed = seed,
                arenaId = if (existing.matchMode == "versus_duel") arenaId else existing.arenaId,
                hostPbId = hostPbId,
                guestPbId = guestPbId,
                phase = "countdown",
                startedAtMs = System.currentTimeMillis(),
                playerA = existing.playerA.copy(
                    pbId = hostPbId,
                    entityId = "pac_a",
                ),
                playerB = PacMazePlayerMeta(
                    pbId = guestPbId,
                    entityId = "pac_b",
                    ready = existing.playerB.ready,
                ),
            ),
        )
    }

    fun firstTurnPbId(gameId: String, hostPbId: String): String = hostPbId

    /**
     * 创建五子棋初始状态（带配置）
     */
    fun createGomokuState(
        hostPbId: String,
        guestPbId: String,
        timerPreset: GomokuTimerPreset = GomokuTimerPreset.STANDARD,
        forbiddenEnabled: Boolean = true,
    ): GomokuPlayState = GomokuPlayState(
        board = GomokuRules.emptyBoard(),
        moveCount = 0,
        blackPbId = hostPbId,
        whitePbId = guestPbId,
        timer = timerPreset.createState(),
        forbiddenEnabled = forbiddenEnabled,
    )
}
