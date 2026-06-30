package com.example.funlife.ui.screens.pacmaze.debug

import android.util.Log
import com.example.funlife.BuildConfig
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeBitmapStableLayoutCache
import kotlin.math.abs
import kotlin.math.hypot

/**
 * PacMaze 移动/渲染顿挫诊断。Logcat 过滤：
 *
 * ```
 * adb logcat -s PacMazeMotionDiag
 * ```
 *
 * 正常移动：`anomaly=none`，screenY 连续变化。
 * 若一顿一顿，重点看带 `>>>` 的行：
 * - `RENDER_CLAMP`：插值/外推位置非法，渲染被钳制（竖向常见）
 * - `LOGIC_CLAMP`：逻辑 tick 碰撞修正（窄道/贴墙）
 * - `MULTI_TICK`：单显示帧内多次 sim tick，跳过了中间插值
 * - `SCREEN_Y_JUMP`：屏幕 Y 帧间跳变 > 阈值（绘制锚点问题）
 */
internal object PacMazeMotionDiag {

    const val TAG = "PacMazeMotionDiag"

    /** 自动化审计：true 时不锁步态帧，用于复现 VISUAL_HEAD_JUMP。 */
    var auditForceAnimatedWalk: Boolean = false

    var enabled: Boolean = true

    private const val THROTTLE_MS = 80L
    private const val BLEND_SAMPLE_THROTTLE_MS = 8L
    private const val SCREEN_Y_JUMP_PX = 4.5f
    private const val MAX_LOGS = 600

    private var lastLogMs = 0L
    private var logCount = 0
    private var lastScreenY = Float.NaN
    private var lastBlend = Float.NaN
    private var lastLogicY = Float.NaN

    private var pendingClampReason: String? = null
    private var pendingClampDelta = 0f

    private var lastBlendSampleMs = 0L
    private var lastBitmapFeetY = Float.NaN

    fun notePublishFrame(
        accumNs: Long,
        stepNs: Long,
        hasPrevious: Boolean,
        blend: Float,
        simDebtNs: Long = 0L,
        ticksThisFrame: Int = 0,
    ) {
        if (!BuildConfig.DEBUG || !enabled) return
        if (!hasPrevious) return
        val now = System.currentTimeMillis()
        if (now - lastBlendSampleMs < BLEND_SAMPLE_THROTTLE_MS) return
        lastBlendSampleMs = now
        Log.i(
            TAG,
            "publish accum=${fmt(accumNs / 1_000_000f)}ms step=${fmt(stepNs / 1_000_000f)}ms " +
                "debt=${fmt(simDebtNs / 1_000_000f)}ms ticks=$ticksThisFrame blend=${fmt(blend)}",
        )
    }

    fun resetSession() {
        lastLogMs = 0L
        logCount = 0
        lastScreenY = Float.NaN
        lastBlend = Float.NaN
        lastLogicY = Float.NaN
        lastBlendSampleMs = 0L
        pendingClampReason = null
        pendingClampDelta = 0f
        lastBitmapFeetY = Float.NaN
        PacMazeBitmapStableLayoutCache.clear()
        PacMazeBitmapDrawDiag.resetSession()
    }

    /** 关卡结束或自动化测试收尾时输出位图绘制审计摘要。 */
    fun finishBitmapAudit(skinId: String? = null) {
        PacMazeBitmapDrawDiag.printSessionSummary(skinId)
    }

    /** 位图布局脚点 Y：横走时 feetCenter 应稳定；dFeetY>1px → 换帧脚点跳变。 */
    fun noteBitmapLayoutFeet(
        entityId: String,
        skinId: String,
        feetY: Float,
        layoutTopY: Float,
        logicY: Float,
        frameIndex: Int = -1,
    ) {
        if (!BuildConfig.DEBUG || !enabled) return
        if (logCount >= MAX_LOGS) return
        val dFeet = if (!lastBitmapFeetY.isNaN()) feetY - lastBitmapFeetY else 0f
        if (kotlin.math.abs(dFeet) <= 1f) {
            lastBitmapFeetY = feetY
            return
        }
        logCount++
        Log.w(
            TAG,
            ">>> BITMAP_FEET_JUMP id=$entityId skin=$skinId frame=$frameIndex " +
                "feetY=${feetY.toInt()} dFeetY=${fmt(dFeet)} topY=${layoutTopY.toInt()} logicY=${fmt(logicY)}",
        )
        lastBitmapFeetY = feetY
    }

    fun noteRenderClamp(
        entityId: String,
        reason: String,
        fromX: Float,
        fromY: Float,
        toX: Float,
        toY: Float,
    ) {
        if (!BuildConfig.DEBUG || !enabled) return
        val delta = hypot(toX - fromX, toY - fromY)
        if (delta < 0.002f) return
        pendingClampReason = reason
        pendingClampDelta = delta
        Log.w(TAG, ">>> RENDER_CLAMP id=$entityId reason=$reason d=${fmt(delta)} " +
            "from=(${fmt(fromX)},${fmt(fromY)}) to=(${fmt(toX)},${fmt(toY)})")
    }

    fun noteLogicClamp(
        entityId: String,
        dir: Direction?,
        beforeX: Float,
        beforeY: Float,
        afterX: Float,
        afterY: Float,
        velocityKept: Boolean,
    ) {
        if (!BuildConfig.DEBUG || !enabled) return
        val delta = hypot(afterX - beforeX, afterY - beforeY)
        if (delta < 0.008f) return
        Log.w(
            TAG,
            ">>> LOGIC_CLAMP id=$entityId dir=${dir?.name ?: "?"} d=${fmt(delta)} " +
                "velKept=$velocityKept before=(${fmt(beforeX)},${fmt(beforeY)}) " +
                "after=(${fmt(afterX)},${fmt(afterY)})",
        )
    }

    fun noteMultiTick(ticksThisFrame: Int, movementOccurred: Boolean = true, renderBlend: Float = Float.NaN) {
        if (!BuildConfig.DEBUG || !enabled || !movementOccurred || ticksThisFrame <= 1) return
        if (!renderBlend.isNaN()) lastBlend = renderBlend
        Log.w(TAG, ">>> MULTI_TICK count=$ticksThisFrame spanBlend=${fmt(lastBlend)} (0→1 over spanDuration)")
    }

    fun noteCollisionShortfall(requestedStep: Float, actualStep: Float, dir: Direction?) {
        if (!BuildConfig.DEBUG || !enabled) return
        if (requestedStep <= 0f) return
        val ratio = actualStep / requestedStep
        if (ratio >= 0.85f) return
        if (ratio <= 0.001f) return // 贴墙顶死时不刷屏
        Log.w(
            TAG,
            ">>> COLLISION_SHORT dir=${dir?.name ?: "?"} req=${fmt(requestedStep)} " +
                "actual=${fmt(actualStep)} ratio=${fmt(ratio)}",
        )
    }

    fun onRenderSample(
        entityId: String,
        direction: Direction?,
        logicX: Float,
        logicY: Float,
        renderX: Float,
        renderY: Float,
        screenCenterY: Float,
        blend: Float,
        cellYPx: Float = 0f,
    ) {
        if (!BuildConfig.DEBUG || !enabled) return
        if (logCount >= MAX_LOGS) return

        val now = System.currentTimeMillis()
        val anomalies = detectAnomalies(logicY, screenCenterY, cellYPx)
        val shouldLog = anomalies.isNotEmpty() ||
            pendingClampReason != null ||
            now - lastLogMs >= THROTTLE_MS

        if (!shouldLog) return
        lastLogMs = now
        logCount++

        val level = if (anomalies.isNotEmpty() || pendingClampReason != null) Log.WARN else Log.INFO
        Log.println(
            level,
            TAG,
            buildString {
                append("id=$entityId dir=${direction?.name ?: "?"} ")
                append("logic=(${fmt(logicX)},${fmt(logicY)}) render=(${fmt(renderX)},${fmt(renderY)}) ")
                append("screenY=${screenCenterY.toInt()} blend=${fmt(blend)} ")
                if (!lastScreenY.isNaN()) append("dScreenY=${fmt(screenCenterY - lastScreenY)} ")
                if (!lastLogicY.isNaN()) append("dLogicY=${fmt(logicY - lastLogicY)} ")
                pendingClampReason?.let { append("clamp=$it d=${fmt(pendingClampDelta)} ") }
                append("anomaly=")
                if (anomalies.isEmpty()) append("none") else anomalies.joinToString("|")
            },
        )
        anomalies.forEach { Log.w(TAG, ">>> $it") }

        lastScreenY = screenCenterY
        lastBlend = blend
        lastLogicY = logicY
        pendingClampReason = null
        pendingClampDelta = 0f
    }

    private fun detectAnomalies(logicY: Float, screenCenterY: Float, cellYPx: Float): List<String> {
        val out = mutableListOf<String>()
        if (!lastScreenY.isNaN()) {
            val dY = screenCenterY - lastScreenY
            val dLogicY = logicY - lastLogicY
            val logicMoving = !lastLogicY.isNaN() && abs(dLogicY) > 0.0005f
            if (logicMoving) {
                val expectedPx = if (cellYPx > 1f) abs(dLogicY) * cellYPx else abs(dY)
                val tolerance = if (cellYPx > 1f) expectedPx * 0.45f + 2f else SCREEN_Y_JUMP_PX
                val excess = abs(dY) - expectedPx - tolerance
                if (excess > 0f) {
                    out += "SCREEN_Y_JUMP dY=${fmt(dY)}px expected~${fmt(expectedPx)}px"
                }
                if (dY * dLogicY < 0f && abs(dY) > 1.5f) {
                    out += "SCREEN_Y_BACKWARD"
                }
            } else if (!lastScreenY.isNaN() && abs(dY) > SCREEN_Y_JUMP_PX && abs(dLogicY) <= 0.0005f) {
                out += "SCREEN_Y_DRIFT dY=${fmt(dY)}px logicYFlat"
            }
        }
        return out
    }

    private fun fmt(v: Float): String = "%.3f".format(v)
}
