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
        else -> lobby
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
