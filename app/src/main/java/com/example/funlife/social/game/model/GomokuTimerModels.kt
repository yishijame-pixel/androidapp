package com.example.funlife.social.game.model

import com.google.gson.annotations.SerializedName

/**
 * 五子棋计时器状态（服务端主导，客户端展示）
 */
data class GomokuTimerState(
    /** 黑方剩余时间（毫秒） */
    @SerializedName("black_remaining_ms") val blackRemainingMs: Long,
    /** 白方剩余时间（毫秒） */
    @SerializedName("white_remaining_ms") val whiteRemainingMs: Long,
    /** 上次同步时间戳（毫秒） */
    @SerializedName("last_tick_ms") val lastTickMs: Long,
    /** 当前回合开始时间戳（毫秒） */
    @SerializedName("turn_start_ms") val turnStartMs: Long,
    /** 单步限时（毫秒），0 表示无限制 */
    @SerializedName("step_limit_ms") val stepLimitMs: Long = 0,
    /** 是否启用计时 */
    @SerializedName("enabled") val enabled: Boolean = true,
) {
    fun toMap(): Map<String, Any?> = buildMap {
        put("black_remaining_ms", blackRemainingMs)
        put("white_remaining_ms", whiteRemainingMs)
        put("last_tick_ms", lastTickMs)
        put("turn_start_ms", turnStartMs)
        put("step_limit_ms", stepLimitMs)
        put("enabled", enabled)
    }

    companion object {
        /** 默认：每方 10 分钟，无单步限时 */
        fun default(totalTimeMs: Long = 10 * 60 * 1000L): GomokuTimerState {
            val now = System.currentTimeMillis()
            return GomokuTimerState(
                blackRemainingMs = totalTimeMs,
                whiteRemainingMs = totalTimeMs,
                lastTickMs = now,
                turnStartMs = now,
                stepLimitMs = 0,
                enabled = true,
            )
        }

        /** 快棋模式：每方 3 分钟，单步 30 秒 */
        fun blitz(): GomokuTimerState {
            val now = System.currentTimeMillis()
            return GomokuTimerState(
                blackRemainingMs = 3 * 60 * 1000L,
                whiteRemainingMs = 3 * 60 * 1000L,
                lastTickMs = now,
                turnStartMs = now,
                stepLimitMs = 30 * 1000L,
                enabled = true,
            )
        }

        /** 休闲模式：不计时 */
        fun casual(): GomokuTimerState {
            val now = System.currentTimeMillis()
            return GomokuTimerState(
                blackRemainingMs = Long.MAX_VALUE,
                whiteRemainingMs = Long.MAX_VALUE,
                lastTickMs = now,
                turnStartMs = now,
                stepLimitMs = 0,
                enabled = false,
            )
        }
    }
}

/**
 * 计时器配置预设
 */
enum class GomokuTimerPreset(
    val displayName: String,
    val totalTimeMs: Long,
    val stepLimitMs: Long,
    val enabled: Boolean,
) {
    STANDARD("标准", 10 * 60 * 1000L, 0, true),
    BLITZ("快棋", 3 * 60 * 1000L, 30 * 1000L, true),
    RAPID("速战", 5 * 60 * 1000L, 60 * 1000L, true),
    CASUAL("休闲", Long.MAX_VALUE, 0, false),
    ;

    fun createState(): GomokuTimerState {
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
}

/**
 * 超时判定结果
 */
sealed class TimeoutResult {
    object None : TimeoutResult()
    data class PlayerTimeout(val color: Char, val winnerColor: Char) : TimeoutResult()
    data class StepTimeout(val color: Char) : TimeoutResult()
}
