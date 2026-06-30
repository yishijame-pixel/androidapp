package com.example.funlife.viewmodel

import com.example.funlife.social.game.engine.pacmaze.PacMazeWorldState

/** 逻辑帧之间的渲染插值快照（60Hz 逻辑 + 屏幕刷新率绘制）。 */
data class PacMazeRenderFrame(
    val current: PacMazeWorldState,
    val previous: PacMazeWorldState?,
    val blend: Float,
    /** [spanDurationNs] > 0 时，Canvas 用显示时钟在 previous→current 间连续插值。 */
    val spanStartNs: Long = 0L,
    val spanDurationNs: Long = 0L,
)
