package com.example.funlife.social.game.engine

import com.example.funlife.social.game.model.GomokuTimerState
import com.example.funlife.social.game.model.TimeoutResult

/**
 * 五子棋计时器引擎（纯函数，可单测）
 *
 * 计时规则：
 * - 服务端时间为准，客户端仅展示
 * - 每次落子时切换计时方
 * - 支持总时间限制和单步限时
 * - 超时自动判负
 */
object GomokuTimer {

    /**
     * 创建初始计时器状态
     */
    fun create(
        totalTimeMs: Long = 10 * 60 * 1000L,
        stepLimitMs: Long = 0,
        enabled: Boolean = true,
    ): GomokuTimerState {
        val now = System.currentTimeMillis()
        return GomokuTimerState(
            blackRemainingMs = totalTimeMs,
            whiteRemainingMs = totalTimeMs,
            lastTickMs = now,
            turnStartMs = now,
            stepLimitMs = stepLimitMs,
            enabled = enabled,
        )
    }

    /**
     * 落子后更新计时器
     * @param state 当前计时器状态
     * @param currentColor 刚落子的颜色
     * @param nowMs 当前时间戳
     * @return 更新后的计时器状态
     */
    fun onMove(
        state: GomokuTimerState,
        currentColor: Char,
        nowMs: Long = System.currentTimeMillis(),
    ): GomokuTimerState {
        if (!state.enabled) return state.copy(lastTickMs = nowMs, turnStartMs = nowMs)

        val elapsed = (nowMs - state.turnStartMs).coerceAtLeast(0)

        val (newBlack, newWhite) = when (currentColor) {
            GomokuRules.CELL_BLACK -> {
                (state.blackRemainingMs - elapsed).coerceAtLeast(0) to state.whiteRemainingMs
            }
            GomokuRules.CELL_WHITE -> {
                state.blackRemainingMs to (state.whiteRemainingMs - elapsed).coerceAtLeast(0)
            }
            else -> state.blackRemainingMs to state.whiteRemainingMs
        }

        return state.copy(
            blackRemainingMs = newBlack,
            whiteRemainingMs = newWhite,
            lastTickMs = nowMs,
            turnStartMs = nowMs,
        )
    }

    /**
     * 获取当前方的剩余时间（实时计算）
     * @param state 计时器状态
     * @param currentTurnColor 当前轮到的颜色
     * @param nowMs 当前时间戳
     * @return 当前方剩余时间（毫秒）
     */
    fun getCurrentRemaining(
        state: GomokuTimerState,
        currentTurnColor: Char,
        nowMs: Long = System.currentTimeMillis(),
    ): Long {
        if (!state.enabled) return Long.MAX_VALUE

        val elapsed = (nowMs - state.turnStartMs).coerceAtLeast(0)
        val base = when (currentTurnColor) {
            GomokuRules.CELL_BLACK -> state.blackRemainingMs
            GomokuRules.CELL_WHITE -> state.whiteRemainingMs
            else -> Long.MAX_VALUE
        }
        return (base - elapsed).coerceAtLeast(0)
    }

    /**
     * 获取单步已用时间
     */
    fun getStepElapsed(
        state: GomokuTimerState,
        nowMs: Long = System.currentTimeMillis(),
    ): Long {
        return (nowMs - state.turnStartMs).coerceAtLeast(0)
    }

    /**
     * 检查是否超时
     * @param state 计时器状态
     * @param currentTurnColor 当前轮到的颜色
     * @param nowMs 当前时间戳
     * @return 超时结果
     */
    fun checkTimeout(
        state: GomokuTimerState,
        currentTurnColor: Char,
        nowMs: Long = System.currentTimeMillis(),
    ): TimeoutResult {
        if (!state.enabled) return TimeoutResult.None

        // 检查单步超时
        if (state.stepLimitMs > 0) {
            val stepElapsed = nowMs - state.turnStartMs
            if (stepElapsed > state.stepLimitMs) {
                return TimeoutResult.StepTimeout(currentTurnColor)
            }
        }

        // 检查总时间超时
        val remaining = getCurrentRemaining(state, currentTurnColor, nowMs)
        if (remaining <= 0) {
            val winner = GomokuRules.opponentColor(currentTurnColor)
            return TimeoutResult.PlayerTimeout(currentTurnColor, winner)
        }

        return TimeoutResult.None
    }

    /**
     * 格式化时间显示
     * @param remainingMs 剩余毫秒
     * @return 格式化字符串 "MM:SS" 或 "H:MM:SS"
     */
    fun formatTime(remainingMs: Long): String {
        if (remainingMs == Long.MAX_VALUE || remainingMs < 0) return "--:--"

        val totalSeconds = remainingMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    /**
     * 格式化时间显示（带毫秒，用于低于 10 秒时）
     */
    fun formatTimeWithMs(remainingMs: Long): String {
        if (remainingMs == Long.MAX_VALUE || remainingMs < 0) return "--:--"

        val totalSeconds = remainingMs / 1000
        val ms = (remainingMs % 1000) / 100

        return if (totalSeconds < 10) {
            "%d.%d".format(totalSeconds, ms)
        } else {
            formatTime(remainingMs)
        }
    }

    /**
     * 判断是否处于危险时间（用于 UI 警告）
     */
    fun isDanger(remainingMs: Long, threshold: Long = 30_000L): Boolean {
        return remainingMs in 0 until threshold
    }

    /**
     * 判断是否处于紧急时间（用于 UI 闪烁）
     */
    fun isCritical(remainingMs: Long, threshold: Long = 10_000L): Boolean {
        return remainingMs in 0 until threshold
    }

    /**
     * 暂停计时器（保存当前状态）
     */
    fun pause(
        state: GomokuTimerState,
        currentTurnColor: Char,
        nowMs: Long = System.currentTimeMillis(),
    ): GomokuTimerState {
        if (!state.enabled) return state

        val elapsed = (nowMs - state.turnStartMs).coerceAtLeast(0)
        val (newBlack, newWhite) = when (currentTurnColor) {
            GomokuRules.CELL_BLACK -> {
                (state.blackRemainingMs - elapsed).coerceAtLeast(0) to state.whiteRemainingMs
            }
            GomokuRules.CELL_WHITE -> {
                state.blackRemainingMs to (state.whiteRemainingMs - elapsed).coerceAtLeast(0)
            }
            else -> state.blackRemainingMs to state.whiteRemainingMs
        }

        return state.copy(
            blackRemainingMs = newBlack,
            whiteRemainingMs = newWhite,
            lastTickMs = nowMs,
            // 不重置 turnStartMs，resume 时用于判断暂停时长
        )
    }

    /**
     * 恢复计时器
     */
    fun resume(
        state: GomokuTimerState,
        nowMs: Long = System.currentTimeMillis(),
    ): GomokuTimerState {
        return state.copy(
            lastTickMs = nowMs,
            turnStartMs = nowMs,
        )
    }

    /**
     * 添加额外时间（用于加时赛等场景）
     */
    fun addTime(
        state: GomokuTimerState,
        color: Char,
        additionalMs: Long,
    ): GomokuTimerState {
        return when (color) {
            GomokuRules.CELL_BLACK -> state.copy(
                blackRemainingMs = state.blackRemainingMs + additionalMs,
            )
            GomokuRules.CELL_WHITE -> state.copy(
                whiteRemainingMs = state.whiteRemainingMs + additionalMs,
            )
            else -> state
        }
    }
}
