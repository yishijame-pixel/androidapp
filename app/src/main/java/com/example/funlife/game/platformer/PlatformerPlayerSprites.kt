package com.example.funlife.game.platformer

import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeIkunGameplayScale
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.resource.ResourceStore
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeBitmapFeetAnchor
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinAnimCache
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinSheetPlayback
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinAnimClip
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinAnimCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinAnimManifest
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSheetCellFeetCache
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinRenderProfileCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinTransform
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterPose
import kotlin.math.abs
import com.example.funlife.social.game.engine.pacmaze.Direction
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 横版冒险主角：行走小鸡 Pro Max（复用 PacMaze 云端序列帧）。 */
object PlatformerPlayerSprites {

    val skinId: PacMazeSkinId = PacMazeSkinId.FOOD_CHICK_WALKER_PRO_MAX

    val drawHeightCells: Float = PacMazeIkunGameplayScale.PLATFORMER_HEIGHT_CELL_FRAC

    const val WALK_FRAME_RATE = 24f
    const val IDLE_FRAME_RATE = 24f
    const val JUMP_FRAME_RATE = 20f
    const val DIE_FRAME_RATE = 24f
    const val ATTACK_FRAME_RATE = 24f
    /** sheet 局内：walk 内容 span 已在 [canvasHeightForCharacterTarget] 补偿，勿再叠乘放大。 */
    private const val CHICK_SHEET_VISUAL_BOOST = 1.0f
    /** sheet 脚底屏幕微调（0=与碰撞盒底对齐；负=上移）。sole 锚点已对齐降采样鞋底。 */
    private const val CHICK_SHEET_FEET_LIFT_CELL_FRAC = 0f
    /** sheet 降采样后真实鞋底在归一化画布上的 fy（manifest 单帧 fy 偏低，勿用于 layout 锚点）。 */
    private const val CHICK_SHEET_SOLE_FRAC_ESTIMATE = 0.912f
    /** 死亡动画播完后，复活倒计时秒数 */
    const val RESPAWN_COUNTDOWN_SEC = 3f
    /** @deprecated 仅作兜底；实际以 [deathAnimDurationSec] + 倒计时为准 */
    const val DIE_DURATION_SEC = 2.6f

    /** 死亡 clip 只播一遍，末帧保持至复活（不用 mod 循环）。 */
    fun dieFrameIndex(deathAnimTime: Float, frameCount: Int, frameRate: Float = DIE_FRAME_RATE): Int {
        if (frameCount <= 1) return 0
        return (deathAnimTime * frameRate).toInt().coerceIn(0, frameCount - 1)
    }

    /** 从第 0 帧播到最后一帧所需时间（秒）。 */
    fun deathAnimDurationSec(frameCount: Int, frameRate: Float = DIE_FRAME_RATE): Float {
        if (frameCount <= 1) return 0f
        return frameCount / frameRate
    }

    fun totalDeathPhaseSec(frameCount: Int, frameRate: Float = DIE_FRAME_RATE): Float =
        deathAnimDurationSec(frameCount, frameRate) + RESPAWN_COUNTDOWN_SEC

    private const val MIN_PLAYABLE_WALK_FRAMES = 4
    private const val MIN_PLAYABLE_JUMP_FRAMES = 2
    private const val MIN_DIE_FRAMES = 4
    /** 离地时额外下移，给发顶留屏内安全区。 */
    private const val JUMP_HEAD_SCREEN_MARGIN_FRAC = 0.08f

    private var cachedWalkHeadTopFrac: Float? = null
    private var cachedWalkMinHeadTopFrac: Float? = null
    /** walk 在归一化画布上的垂直内容占比（fy_max - ty_min），用于抵消 jump 竖屏撑高的透明边。 */
    private var cachedWalkContentSpanFrac: Float? = null
    fun walkFrameTarget(): Int =
        PacMazeRemoteSkinAnimCache.clipFrameTarget(skinId, PacMazeSkinAnimClip.WALK)

    fun jumpFrameTarget(): Int =
        PacMazeRemoteSkinAnimCache.clipFrameTarget(skinId, PacMazeSkinAnimClip.JUMP)

    fun idleFrameTarget(): Int =
        PacMazeRemoteSkinAnimCache.clipFrameTarget(skinId, PacMazeSkinAnimClip.IDLE)

    fun attackFrameTarget(): Int =
        PacMazeRemoteSkinAnimCache.clipFrameTarget(skinId, PacMazeSkinAnimClip.ATTACK)

    fun attackSheetFrameCount(): Int =
        PacMazeRemoteSkinAnimCache.playbackSheet(skinId, PacMazeSkinAnimClip.ATTACK)?.frameCount
            ?.takeIf { it > 0 }
            ?: attackFrameTarget().coerceAtLeast(4)

    fun basketballThrowDurationSec(): Float =
        attackSheetFrameCount() / ATTACK_FRAME_RATE

    fun isBasketballThrowing(player: PlatformerPlayer): Boolean =
        player.rangedClip == PlatformerAnimClipRef.BASKETBALL && player.rangedAnimSecLeft > 0f

    fun prefetchAttackSheet() {
        PacMazeRemoteSkinAnimCache.requestSheetPlaybackAsync(skinId, PacMazeSkinAnimClip.ATTACK)
    }

    /**
     * walk 相位增速：与 [WALK_FRAME_RATE] 对齐，61 帧 @24fps ≈ 2.5s/圈。
     * 勿用 PacMaze 走廊 4.8/s（≈91 帧/秒），否则小鸡腿会像抽搐。
     */
    fun walkAnimPhasePerSec(): Float {
        val frameCount = walkFrameTarget().coerceAtLeast(1)
        val perFrame = com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinAnimTiming
            .walkPhasePerFrame(skinId, walkPreview = false, frameCount = frameCount)
        return perFrame * WALK_FRAME_RATE
    }

    fun isWalkFullyLoaded(): Boolean =
        PacMazeRemoteSkinAnimCache.playbackSheet(skinId, PacMazeSkinAnimClip.WALK)?.frameCount?.let { it >= walkFrameTarget() } == true ||
            PacMazeRemoteSkinAnimCache.isClipFullyLoaded(skinId, PacMazeSkinAnimClip.WALK)

    fun isJumpFullyLoaded(): Boolean =
        PacMazeRemoteSkinAnimCache.playbackSheet(skinId, PacMazeSkinAnimClip.JUMP)?.frameCount?.let { it >= jumpFrameTarget() } == true ||
            PacMazeRemoteSkinAnimCache.isClipFullyLoaded(skinId, PacMazeSkinAnimClip.JUMP)

    /** build 归一化画布（manifest 未读到时的兜底，v16 HEAD_CANVAS_PAD=160）。 */
    private const val NORMALIZED_CANVAS_W = 710
    private const val NORMALIZED_CANVAS_H = 750
    private const val NORMALIZED_FEET_Y = 0.979651f
    private const val NORMALIZED_FEET_X = 0.513103f

    private fun isNormalizedCanvasFrame(frame: ImageBitmap): Boolean =
        (frame.width == NORMALIZED_CANVAS_W && frame.height == NORMALIZED_CANVAS_H) ||
            (frame.width == 725 && frame.height == 688) ||
            (frame.width == 725 && frame.height == 676) ||
            (frame.width == 1001 && frame.height == 621) ||
            (frame.width == 993 && frame.height == 600) ||
            (frame.width == 993 && frame.height == 928)

    private var cachedStableFeet: Pair<Float, Float>? = null
    private var cachedReferenceAspect: Float? = null
    private var cachedWalkRefWidth: Int? = null
    private var cachedWalkRefHeight: Int? = null
    /** walk 已加载帧的最大裁边尺寸，避免首帧参考框过小把后续帧挤扁。 */
    private var cachedWalkBoxWidth: Int? = null
    private var cachedWalkBoxHeight: Int? = null

    /** walk_1 基准缩放（jump 等相对 walk 高度成比例放大）。 */
    private fun referenceDrawScale(targetH: Float): Float {
        val refH = (cachedWalkRefHeight ?: return 1f).coerceAtLeast(1)
        return targetH / refH.toFloat()
    }

    /**
     * manifest 归一化：整画布缩放到高度 [targetH]（与 PacMaze HEIGHT_CELL_FRAC 一致），发型完整保留。
     */
    private fun layoutManifestFullScaled(
        targetH: Float,
        refWidth: Int,
        refHeight: Int,
        stableFeet: Pair<Float, Float>,
        feetBias: Float = PacMazeIkunGameplayScale.FEET_FRAC_GAMEPLAY_BIAS,
    ): ChickSpriteLayout {
        val refH = refHeight.coerceAtLeast(1).toFloat()
        val refW = refWidth.coerceAtLeast(1).toFloat()
        val feetY = (stableFeet.first - feetBias)
            .coerceIn(PacMazeIkunGameplayScale.FEET_Y_FRAC_MIN, 0.999f)
        val dstH = targetH
        val dstW = dstH * (refW / refH)
        return ChickSpriteLayout(
            dstW = dstW,
            dstH = dstH,
            feetYFrac = feetY,
            feetXFrac = stableFeet.second.coerceIn(0.12f, 0.88f),
            frameW = dstW,
            frameH = dstH,
            frameOx = 0f,
            frameOy = 0f,
        )
    }

    private fun walkReferenceHeadTopFrac(): Float {
        cachedWalkHeadTopFrac?.let { return it }
        PacMazeSkinAnimManifest.platformerClipMetrics(skinId, PacMazeSkinAnimClip.WALK)
            ?.firstOrNull()?.headTopY?.let {
                cachedWalkHeadTopFrac = it
                return it
            }
        cachedWalkHeadTopFrac = 0.31f
        return cachedWalkHeadTopFrac!!
    }

    /**
     * walk 全帧在归一化画布上的最大垂直跨度。
     * 共享画布常被 jump 竖屏帧撑高，整画布缩放会把角色缩到 ~25% 目标高度。
     */
    private fun walkContentSpanFrac(): Float {
        cachedWalkContentSpanFrac?.let { return it }
        val span = PacMazeSkinAnimManifest.platformerClipMetrics(skinId, PacMazeSkinAnimClip.WALK)
            ?.takeIf { it.isNotEmpty() }
            ?.let { metrics ->
                val minHead = metrics.minOf { it.headTopY }
                val maxFeet = metrics.maxOf { it.feetY }
                (maxFeet - minHead).coerceIn(0.22f, 0.72f)
            }
            ?: 0.42f
        cachedWalkContentSpanFrac = span
        return span
    }

    /** jump 全帧垂直跨度（与 walk 同算法，用于对齐实体高度）。 */
    private fun jumpContentSpanFrac(): Float {
        val span = PacMazeSkinAnimManifest.platformerClipMetrics(skinId, PacMazeSkinAnimClip.JUMP)
            ?.takeIf { it.isNotEmpty() }
            ?.let { metrics ->
                val minHead = metrics.minOf { it.headTopY }
                val maxFeet = metrics.maxOf { it.feetY }
                (maxFeet - minHead).coerceIn(0.22f, 0.72f)
            }
            ?: walkContentSpanFrac()
        return span
    }

    /**
     * jump 格子里角色更「满」（~0.60 vs walk ~0.54），同尺寸画布会显得更大。
     * 按实体跨度比缩小 jump 绘制，使与 walk 同高；脚点锚不变。
     */
    private fun jumpBodyMatchScale(): Float {
        val walkSpan = walkContentSpanFrac()
        val jumpSpan = jumpContentSpanFrac()
        if (jumpSpan <= walkSpan * 1.001f) return 1f
        return (walkSpan / jumpSpan).coerceIn(0.72f, 1f)
    }

    /** 使 walk 角色实体高度 ≈ [characterTargetH]，而非整张贴图透明画布。 */
    private fun canvasHeightForCharacterTarget(characterTargetH: Float): Float =
        characterTargetH / walkContentSpanFrac()

    /** walk 全帧最小 ty：jump 对齐到此，避免帧间发顶在屏上跳 8–12px 像被切。 */
    private fun walkMinHeadTopFrac(): Float {
        cachedWalkMinHeadTopFrac?.let { return it }
        PacMazeSkinAnimManifest.platformerClipMetrics(skinId, PacMazeSkinAnimClip.WALK)
            ?.minOfOrNull { it.headTopY }
            ?.let {
                cachedWalkMinHeadTopFrac = it
                return it
            }
        cachedWalkMinHeadTopFrac = walkReferenceHeadTopFrac()
        return cachedWalkMinHeadTopFrac!!
    }

    private fun layoutInStableClipBox(
        frame: ImageBitmap,
        targetH: Float,
        refWidth: Int,
        refHeight: Int,
        stableFeet: Pair<Float, Float>,
        topPad: Float = 0f,
        alignFrameFeetToManifest: Boolean = false,
        maxFrameHeight: Float? = null,
    ): ChickSpriteLayout {
        val refH = refHeight.coerceAtLeast(1).toFloat()
        val refW = refWidth.coerceAtLeast(1).toFloat()
        val pad = topPad.coerceAtLeast(0f)
        val boxH = targetH + pad
        val boxW = refW * (targetH / refH)

        var fitScale = minOf(
            boxW / frame.width.coerceAtLeast(1),
            boxH / frame.height.coerceAtLeast(1),
        )
        maxFrameHeight?.let { cap ->
            val capScale = cap / frame.height.coerceAtLeast(1)
            if (capScale < fitScale) fitScale = capScale
        }
        val frameW = frame.width * fitScale
        val frameH = frame.height * fitScale
        val frameFeet = if (alignFrameFeetToManifest) {
            stableFeet
        } else {
            PacMazeBitmapFeetAnchor.platformerFeetAnchor(frame, skinId)
        }
        val anchorX = boxW * stableFeet.second
        val anchorY = pad + targetH * stableFeet.first
        val frameOx = anchorX - frameW * frameFeet.second
        val frameOy = anchorY - frameH * frameFeet.first

        return ChickSpriteLayout(
            dstW = boxW,
            dstH = boxH,
            feetYFrac = anchorY / boxH,
            feetXFrac = stableFeet.second,
            frameW = frameW,
            frameH = frameH,
            frameOx = frameOx,
            frameOy = frameOy,
        )
    }


    fun referenceDrawAspect(): Float {
        cachedReferenceAspect?.let { return it }
        refreshStableFeetAnchor()
        return cachedReferenceAspect ?: (864f / 480f)
    }

    fun drawSizeForPlatformer(frame: ImageBitmap, targetH: Float): Pair<Float, Float> {
        val s = referenceDrawScale(targetH)
        return frame.width * s to frame.height * s
    }

    fun drawSizeMatchHeight(frame: ImageBitmap, targetH: Float): Pair<Float, Float> =
        drawSizeForPlatformer(frame, targetH)

    data class ChickSpriteLayout(
        /** 固定布局框（世界锚点按此框 + 脚点计算）。 */
        val dstW: Float,
        val dstH: Float,
        val feetYFrac: Float,
        val feetXFrac: Float,
        /** 当前帧在框内的等比绘制尺寸与偏移。 */
        val frameW: Float,
        val frameH: Float,
        val frameOx: Float = 0f,
        val frameOy: Float = 0f,
        /** jump 等 clip 实体跨度更大时，在脚点处缩放绘制（布局框不变）。 */
        val drawScale: Float = 1f,
    )

    data class ChickSheetDraw(
        val sheet: PacMazeSkinSheetPlayback,
        val clip: PacMazeSkinAnimClip,
        val frameIndex: Int,
    )

    fun isWalkPlayable(): Boolean =
        PacMazeRemoteSkinAnimCache.playbackSheet(skinId, PacMazeSkinAnimClip.WALK) != null ||
            PacMazeRemoteSkinAnimCache.playbackFrameCount(skinId, PacMazeSkinAnimClip.WALK) >= MIN_PLAYABLE_WALK_FRAMES

    fun isJumpPlayable(): Boolean =
        PacMazeRemoteSkinAnimCache.playbackSheet(skinId, PacMazeSkinAnimClip.JUMP) != null ||
            PacMazeRemoteSkinAnimCache.playbackFrameCount(skinId, PacMazeSkinAnimClip.JUMP) >= MIN_PLAYABLE_JUMP_FRAMES

    fun isReady(): Boolean = isWalkFullyLoaded() && isJumpFullyLoaded()

    /** 可玩就绪：bootstrap 帧 + 脚点（进局门槛，不等全量 61 帧）。 */
    fun isBootstrapPlayable(): Boolean =
        PacMazeRemoteSkinAnimCache.isBootstrapPlayable(skinId)

    /** 完整动画就绪：walk + jump 全量（局内画质/后台预热用）。 */
    fun isPlayableReady(): Boolean =
        isWalkFullyLoaded() &&
            isJumpFullyLoaded() &&
            PacMazeBitmapFeetAnchor.hasGameplayDefault(skinId)

    /**
     * 横版加载：bootstrap 即可进局，全量 clip 后台补齐。
     */
    suspend fun warmupPlayable(onStatus: (String, Int) -> Unit = { _, _ -> }) {
        val sheetReady = PacMazeRemoteSkinAnimCache.preparePlatformerSheetsForBoot(skinId) { status ->
            onStatus(status.message, status.percent)
        }
        if (!sheetReady) {
            PacMazeRemoteSkinAnimCache.preparePlatformerSkinForBoot(skinId) { status ->
                onStatus(status.message, status.percent)
            }
        }
        refreshStableFeetAnchor()
    }

    /**
     * 进局内：先保证 walk 至少有 bootstrap 可播帧；磁盘已有完整 walk 则同步拉满，避免内存被 LRU 清掉后只剩占位块。
     */
    suspend fun ensureGameplayAnimReady(onStatus: (String, Int) -> Unit = { _, _ -> }) {
        if (!isWalkPlayable()) {
            warmupPlayable(onStatus)
        }
        requestMissingClips()
        if (!isWalkPlayable()) {
            PacMazeRemoteSkinAnimCache.ensureClip(skinId, PacMazeSkinAnimClip.WALK)
        }
        if (PacMazeRemoteSkinAnimCache.isDiskClipComplete(skinId, PacMazeSkinAnimClip.WALK) &&
            !PacMazeRemoteSkinAnimCache.hasPlatformerSheetBundle(skinId) &&
            !isWalkFullyLoaded()
        ) {
            runCatching {
                PacMazeRemoteSkinAnimCache.ensureFullClip(skinId, PacMazeSkinAnimClip.WALK)
            }
        }
        refreshStableFeetAnchor()
    }

    /** 后台仅补 die sheet，避免切角时全量 decode 61 帧导致卡顿。 */
    suspend fun warmupDieSheetOnly() {
        PacMazeRemoteSkinAnimCache.requestSheetPlaybackAsync(skinId, PacMazeSkinAnimClip.DIE)
        runCatching {
            PacMazeRemoteSkinAnimCache.ensureSheetPlayback(skinId, PacMazeSkinAnimClip.DIE)
        }
    }

    /** 横版 sheet 快路径：walk+jump sheet 就绪即可，不拉全量逐帧。 */
    suspend fun prepareSheetsForPlay(): Boolean {
        if (isBootstrapPlayable()) return true
        return runCatching {
            PacMazeRemoteSkinAnimCache.preparePlatformerSheetsForBoot(skinId)
        }.getOrDefault(false) && isBootstrapPlayable()
    }

    /** 后台补齐全量序列帧（die / 完整 walk 等）。 */
    suspend fun warmupFull() {
        if (PacMazeRemoteSkinAnimCache.hasPlatformerSheetBundle(skinId)) {
            warmupDieSheetOnly()
            return
        }
        if (!ResourceStore.isPacMazeBundleReady("pac_maze_skins")) {
            PacMazeRemoteSkinAnimCache.requestEnsureBundleAsync()
        }
        PacMazeRemoteSkinAnimCache.reloadClipsIfIncomplete(skinId)
        requestMissingClips()
        PacMazeRemoteSkinAnimCache.ensureFullClip(skinId, PacMazeSkinAnimClip.WALK)
        PacMazeRemoteSkinAnimCache.ensureFullClip(skinId, PacMazeSkinAnimClip.JUMP)
        PacMazeRemoteSkinAnimCache.warmUpGameplayClips(
            skinId,
            PacMazeSkinAnimClip.DIE,
        )
        awaitClipFrames(PacMazeSkinAnimClip.DIE, MIN_DIE_FRAMES, maxAttempts = 20)
        refreshStableFeetAnchor()
    }

    suspend fun warmup() = warmupFull()

    fun hasAnimatableClips(): Boolean = isWalkFullyLoaded() && isJumpFullyLoaded()

    /** 横版局内：死亡→die；丢篮球→attack；滞空→jump；移动→walk；站立→idle。 */
    private fun pickPlatformerClip(player: PlatformerPlayer): PacMazeSkinAnimClip = when {
        player.dying -> PacMazeSkinAnimClip.DIE
        isBasketballThrowing(player) -> PacMazeSkinAnimClip.ATTACK
        shouldUseJumpClip(player) -> PacMazeSkinAnimClip.JUMP
        player.locomoting || abs(player.vx) > WALK_CLIP_VX_GATE -> PacMazeSkinAnimClip.WALK
        standStillSec < IDLE_CLIP_HOLD_SEC -> PacMazeSkinAnimClip.WALK
        else -> PacMazeSkinAnimClip.IDLE
    }

    /** 仍有滑行速度时保持 walk，避免松手一帧闪 idle。与 [PlatformerPhysics] LOCOMOTE_OFF 对齐。 */
    private const val WALK_CLIP_VX_GATE = 6f

    /** 松手后仍播 walk 第 0 帧的时长，避免 walk↔idle 来回闪。 */
    private const val IDLE_CLIP_HOLD_SEC = 0.15f

    /** food_chick idle sheet 53–55 扫描/姿势异常，横版待机跳过该段。 */
    private const val IDLE_SKIP_FRAME_FROM = 53
    private const val IDLE_SKIP_FRAME_TO = 55
    private const val IDLE_SKIP_HOLD_FRAME = 52

    /** 进入 idle 时的 animTime，待机从第 0 帧顺播，避免全局 animTime 跳帧浮空。 */
    private var lastResolvedClip: PacMazeSkinAnimClip? = null
    private var idlePhaseOriginAnimTime = Float.NaN
    private var standStillSec = 0f
    private var lastResolveAnimTime = Float.NaN

    private fun noteClipForIdlePhase(clip: PacMazeSkinAnimClip, animTime: Float) {
        if (clip == PacMazeSkinAnimClip.IDLE && lastResolvedClip != PacMazeSkinAnimClip.IDLE) {
            idlePhaseOriginAnimTime = animTime
        } else if (clip != PacMazeSkinAnimClip.IDLE) {
            idlePhaseOriginAnimTime = Float.NaN
        }
        lastResolvedClip = clip
    }

    private fun idleLocalAnimTime(animTime: Float): Float {
        val origin = idlePhaseOriginAnimTime
        return if (origin.isNaN()) 0f else (animTime - origin).coerceAtLeast(0f)
    }

    private fun mapIdleFrameIndex(raw: Int, frameCount: Int): Int {
        val last = frameCount - 1
        if (raw in IDLE_SKIP_FRAME_FROM..IDLE_SKIP_FRAME_TO) {
            return IDLE_SKIP_HOLD_FRAME.coerceIn(0, last)
        }
        return raw.coerceIn(0, last)
    }

    fun resetIdlePhaseTracking() {
        lastResolvedClip = null
        idlePhaseOriginAnimTime = Float.NaN
        standStillSec = 0f
        lastResolveAnimTime = Float.NaN
    }

    private fun advanceStandStillTimer(player: PlatformerPlayer, animTime: Float) {
        val dt = if (lastResolveAnimTime.isNaN()) {
            0f
        } else {
            (animTime - lastResolveAnimTime).coerceIn(0f, 0.12f)
        }
        lastResolveAnimTime = animTime
        standStillSec = if (player.locomoting || abs(player.vx) > WALK_CLIP_VX_GATE) {
            0f
        } else {
            standStillSec + dt
        }
    }

    /**
     * 滞空才切 jump。土狼时间内仍 walk（防 grounded 漏判）；离台后必须 jump，不能因 loco 继续 walk。
     */
    private fun shouldUseJumpClip(player: PlatformerPlayer): Boolean {
        if (player.grounded) return false
        if (player.jumpActive) return true
        if (player.vy < -45f || player.vy > 45f) return true
        if (player.coyoteSec > 0.02f) return false
        return true
    }

    private fun framesForClip(clip: PacMazeSkinAnimClip): List<ImageBitmap>? {
        PacMazeRemoteSkinAnimCache.playbackFrames(skinId, clip)
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }
        if (clip == PacMazeSkinAnimClip.WALK) {
            PacMazeRemoteSkinAnimCache.peekSingleWalkFrame(skinId)?.let { return listOf(it) }
            PacMazeRemoteSkinAnimCache.cover(skinId)?.let { return listOf(it) }
            return null
        }
        if (clip == PacMazeSkinAnimClip.IDLE) {
            PacMazeRemoteSkinAnimCache.peekSingleClipFrame(skinId, PacMazeSkinAnimClip.IDLE)?.let { return listOf(it) }
            PacMazeRemoteSkinAnimCache.peekSingleWalkFrame(skinId)?.let { return listOf(it) }
            return null
        }
        return PacMazeRemoteSkinAnimCache.peekSingleClipFrame(skinId, clip)?.let { listOf(it) }
    }

    private suspend fun awaitClipFrames(
        clip: PacMazeSkinAnimClip,
        minFrames: Int,
        maxAttempts: Int,
    ) {
        repeat(maxAttempts) {
            if (PacMazeRemoteSkinAnimCache.playbackFrameCount(skinId, clip) >= minFrames) return
            PacMazeRemoteSkinAnimCache.ensureBootstrapFrames(skinId, clip, minFrames)
            PacMazeRemoteSkinAnimCache.ensureClip(skinId, clip)
            PacMazeRemoteSkinAnimCache.requestClipAsync(skinId, clip)
            PacMazeRemoteSkinAnimCache.requestContinueClipLoadAsync(skinId, clip)
            if (!ResourceStore.isPacMazeBundleReady("pac_maze_skins")) {
                withContext(Dispatchers.IO) {
                    runCatching { ResourceStore.ensureBundle("pac_maze_skins") }
                }
            }
            delay(50)
        }
    }

    fun requestMissingClips() {
        PacMazeRemoteSkinAnimCache.requestClipAsync(skinId, PacMazeSkinAnimClip.WALK)
        PacMazeRemoteSkinAnimCache.requestClipAsync(skinId, PacMazeSkinAnimClip.IDLE)
        PacMazeRemoteSkinAnimCache.requestClipAsync(skinId, PacMazeSkinAnimClip.JUMP)
        PacMazeRemoteSkinAnimCache.requestClipAsync(skinId, PacMazeSkinAnimClip.DIE)
        PacMazeRemoteSkinAnimCache.requestSheetPlaybackAsync(skinId, PacMazeSkinAnimClip.IDLE)
        PacMazeRemoteSkinAnimCache.requestContinueClipLoadAsync(skinId, PacMazeSkinAnimClip.WALK)
        PacMazeRemoteSkinAnimCache.requestContinueClipLoadAsync(skinId, PacMazeSkinAnimClip.IDLE)
        PacMazeRemoteSkinAnimCache.requestContinueClipLoadAsync(skinId, PacMazeSkinAnimClip.DIE)
        PacMazeRemoteSkinAnimCache.requestContinueClipLoadAsync(skinId, PacMazeSkinAnimClip.JUMP)
        if (!isReady()) {
            PacMazeRemoteSkinAnimCache.requestGameplayWarmupAsync(skinId)
        }
    }

    fun resolveFrame(player: PlatformerPlayer, animTime: Float): ImageBitmap? =
        resolveChickFrame(player, animTime)

    private const val TAG = "PlatformerPlayerSprites"

    fun resolveChickFrame(player: PlatformerPlayer, animTime: Float): ImageBitmap? {
        val clip = pickPlatformerClip(player)
        PacMazeRemoteSkinAnimCache.requestClipAsync(skinId, clip)
        PacMazeRemoteSkinAnimCache.requestContinueClipLoadAsync(skinId, clip)
        val frames = framesForClip(clip)
        if (frames.isNullOrEmpty()) {
            Log.w(
                TAG,
                "no frames clip=${clip.name} walk=${PacMazeRemoteSkinAnimCache.playbackFrameCount(skinId, PacMazeSkinAnimClip.WALK)} " +
                    "jump=${PacMazeRemoteSkinAnimCache.playbackFrameCount(skinId, PacMazeSkinAnimClip.JUMP)} " +
                    "sheets walk=${PacMazeRemoteSkinAnimCache.playbackSheet(skinId, PacMazeSkinAnimClip.WALK) != null}",
            )
            PlatformerChickLoadLog.logFallbackFrame(clip, 0, 0, "no_frames")
            return null
        }
        val index = pickPlatformerFrameIndex(player, animTime, clip, frames.size)
        val frame = frames[index.coerceIn(0, frames.lastIndex)]
        PlatformerChickLoadLog.logFallbackFrame(clip, frame.width, frame.height, "per_frame")
        return frame
    }

    /** Sprite Sheet 局内主路径：一张 walk/jump sheet + srcRect，61 帧无需逐格 decode。 */
    fun resolveChickSheetDraw(player: PlatformerPlayer, animTime: Float): ChickSheetDraw? {
        advanceStandStillTimer(player, animTime)
        var clip = pickPlatformerClip(player)
        noteClipForIdlePhase(clip, animTime)
        if (clip == PacMazeSkinAnimClip.ATTACK) {
            PacMazeRemoteSkinAnimCache.requestSheetPlaybackAsync(skinId, PacMazeSkinAnimClip.ATTACK)
        }
        PacMazeRemoteSkinAnimCache.requestSheetPlaybackAsync(skinId, clip)
        if (clip == PacMazeSkinAnimClip.DIE) {
            PacMazeRemoteSkinAnimCache.requestSheetPlaybackAsync(skinId, PacMazeSkinAnimClip.DIE)
        }
        var sheet = PacMazeRemoteSkinAnimCache.playbackSheet(skinId, clip)
        if (sheet == null && clip == PacMazeSkinAnimClip.ATTACK) {
            sheet = PacMazeRemoteSkinAnimCache.playbackSheet(skinId, PacMazeSkinAnimClip.WALK)
                ?: return null
            clip = PacMazeSkinAnimClip.WALK
        }
        if (sheet == null && clip == PacMazeSkinAnimClip.IDLE) {
            sheet = PacMazeRemoteSkinAnimCache.playbackSheet(skinId, PacMazeSkinAnimClip.WALK)
                ?: return null
            return ChickSheetDraw(sheet = sheet, clip = PacMazeSkinAnimClip.WALK, frameIndex = 0)
        }
        if (sheet == null && clip != PacMazeSkinAnimClip.DIE) {
            sheet = PacMazeRemoteSkinAnimCache.playbackSheet(skinId, PacMazeSkinAnimClip.WALK)
        }
        sheet ?: return null
        val frameCount = sheet.frameCount.coerceAtLeast(1)
        val index = pickPlatformerFrameIndex(player, animTime, clip, frameCount)
        return ChickSheetDraw(sheet = sheet, clip = clip, frameIndex = index.coerceIn(0, frameCount - 1))
    }

    /** sheet 布局脚点：降采样 sheet 格内 bbox 鞋底优先，manifest fy 跳变大不可靠。 */
    private fun stableSheetGameplayFeet(): Pair<Float, Float> {
        var soleY = CHICK_SHEET_SOLE_FRAC_ESTIMATE
        PacMazeRemoteSkinAnimCache.playbackSheet(skinId, PacMazeSkinAnimClip.WALK)?.let { walkSheet ->
            PacMazeSheetCellFeetCache.precompute(skinId, PacMazeSkinAnimClip.WALK, walkSheet)
            PacMazeSheetCellFeetCache.walkCycleMaxFeetY(skinId, walkSheet)?.let { scanned ->
                soleY = maxOf(soleY, scanned)
            }
        }
        PacMazeRemoteSkinAnimCache.playbackSheet(skinId, PacMazeSkinAnimClip.IDLE)?.let { idleSheet ->
            PacMazeSheetCellFeetCache.precompute(skinId, PacMazeSkinAnimClip.IDLE, idleSheet)
            PacMazeSheetCellFeetCache.cycleMaxFeetY(skinId, PacMazeSkinAnimClip.IDLE, idleSheet)?.let { scanned ->
                soleY = maxOf(soleY, scanned)
            }
        }
        PacMazeSkinAnimManifest.platformerClipMetrics(skinId, PacMazeSkinAnimClip.WALK)
            ?.maxOfOrNull { it.feetY }
            ?.let { soleY = maxOf(soleY, it) }
        val y = (soleY - PacMazeIkunGameplayScale.PLATFORMER_FEET_FRAC_BIAS)
            .coerceIn(PacMazeIkunGameplayScale.FEET_Y_FRAC_MIN, 0.96f)
        val fx = PacMazeSkinAnimManifest.loadForSkin(skinId)?.anchorFrac?.x
            ?.coerceIn(0.12f, 0.88f) ?: 0.44f
        return y to fx
    }

    private var cachedSheetLayoutKey: Long? = null
    private var cachedSheetLayout: ChickSpriteLayout? = null

    private fun sheetLayoutCacheKey(cellPx: Float, tilePx: Int): Long =
        ((cellPx * 1000f).toLong() shl 32) or (tilePx.toLong() and 0xffffffffL)

    private fun computeStableSheetLayout(cellPx: Float, tilePx: Int): ChickSpriteLayout {
        val scale = (tilePx / PLATFORMER_TILE_PX.toFloat()).coerceIn(0.5f, 1f)
        val characterH = cellPx * drawHeightCells * scale * CHICK_SHEET_VISUAL_BOOST
        val manifest = PacMazeSkinAnimManifest.loadForSkin(skinId)
        val canvasBodyH = canvasHeightForCharacterTarget(characterH)
        val feet = stableSheetGameplayFeet()

        if (manifest?.normalized == true && manifest.canvas != null) {
            return layoutManifestFullScaled(
                targetH = canvasBodyH,
                refWidth = manifest.canvas.w,
                refHeight = manifest.canvas.h,
                stableFeet = feet,
                feetBias = 0f,
            )
        }

        val refW = manifest?.canvas?.w ?: 993
        val refH = manifest?.canvas?.h ?: 928
        return layoutManifestFullScaled(
            targetH = canvasBodyH,
            refWidth = refW,
            refHeight = refH,
            stableFeet = feet,
            feetBias = 0f,
        )
    }

    /**
     * sheet 步态对齐：用 decode 时扫出的格内 bbox 脚底，不用 manifest fy（相邻帧会跳 0.70↔0.82）。
     */
    private fun sheetWalkCycleFrameOy(
        sheet: PacMazeSkinSheetPlayback,
        clip: PacMazeSkinAnimClip,
        frameIndex: Int,
        frameH: Float,
        layoutFeetYFrac: Float,
    ): Float {
        val manifest = PacMazeSkinAnimManifest.loadForSkin(skinId) ?: return 0f
        if (manifest.render?.syncWalkCycleToSprite != true) return 0f
        val metricsClip = when (clip) {
            PacMazeSkinAnimClip.WALK, PacMazeSkinAnimClip.RUN -> PacMazeSkinAnimClip.WALK
            PacMazeSkinAnimClip.IDLE -> PacMazeSkinAnimClip.IDLE
            else -> return 0f
        }
        PacMazeSheetCellFeetCache.precompute(skinId, metricsClip, sheet)
        if (metricsClip == PacMazeSkinAnimClip.IDLE) {
            val cycleMax = PacMazeSheetCellFeetCache.cycleMaxFeetY(skinId, metricsClip, sheet)
                ?: PacMazeRemoteSkinAnimCache.playbackSheet(skinId, PacMazeSkinAnimClip.WALK)?.let { walkSheet ->
                    PacMazeSheetCellFeetCache.precompute(skinId, PacMazeSkinAnimClip.WALK, walkSheet)
                    PacMazeSheetCellFeetCache.cycleMaxFeetY(skinId, PacMazeSkinAnimClip.WALK, walkSheet)
                }
                ?: return 0f
            // 待机固定最深脚点，drawTop 全程不变；呼吸靠换帧，不靠 oy 跳。
            return frameH * (layoutFeetYFrac - cycleMax)
        }
        val cellFeetY = PacMazeSheetCellFeetCache.cellFeetY(skinId, metricsClip, sheet, frameIndex)
            ?: return 0f
        return frameH * (layoutFeetYFrac - cellFeetY)
    }

    /**
     * Sheet 布局框 walk/jump 共用；jump 仅 [drawScale] 在绘制时以脚点缩小，实体与 walk 同高。
     */
    fun layoutForDrawSheet(
        sheet: PacMazeSkinSheetPlayback,
        player: PlatformerPlayer,
        cellPx: Float,
        tilePx: Int,
        clip: PacMazeSkinAnimClip = PacMazeSkinAnimClip.WALK,
        frameIndex: Int = 0,
    ): ChickSpriteLayout {
        val cacheKey = sheetLayoutCacheKey(cellPx, tilePx)
        val base = if (cachedSheetLayoutKey == cacheKey && cachedSheetLayout != null) {
            cachedSheetLayout!!
        } else {
            computeStableSheetLayout(cellPx, tilePx).also {
                cachedSheetLayoutKey = cacheKey
                cachedSheetLayout = it
            }
        }
        var layout = when (clip) {
            PacMazeSkinAnimClip.JUMP -> {
                val scale = jumpBodyMatchScale()
                if (scale < 0.999f) base.copy(drawScale = scale) else base
            }
            else -> base
        }
        val cycleOy = sheetWalkCycleFrameOy(sheet, clip, frameIndex, layout.frameH, layout.feetYFrac)
        if (cycleOy != 0f) layout = layout.copy(frameOy = layout.frameOy + cycleOy)
        return layout
    }

    fun layoutForDrawManifest(
        sheet: PacMazeSkinSheetPlayback,
        player: PlatformerPlayer,
        cellPx: Float,
        tilePx: Int,
        clip: PacMazeSkinAnimClip = PacMazeSkinAnimClip.WALK,
        frameIndex: Int = 0,
    ): ChickSpriteLayout = layoutForDrawSheet(sheet, player, cellPx, tilePx, clip, frameIndex)

    /** 横版专用帧号：与 PacMaze 走廊 [PacMazeRemoteSkinAnimCatalog.frameIndex] 一致。 */
    private fun pickPlatformerFrameIndex(
        player: PlatformerPlayer,
        animTime: Float,
        clip: PacMazeSkinAnimClip,
        frameCount: Int,
    ): Int {
        if (frameCount <= 1) return 0
        val moving = player.locomoting
        val airborne = shouldUseJumpClip(player)
        val pose = PacMazeCharacterPose(
            facing = if (player.facingRight) Direction.RIGHT else Direction.LEFT,
            animPhase = player.animPhase,
            isMoving = moving,
            powerActive = false,
            airborne = airborne,
            isDead = player.dying,
        )
        return when (clip) {
            PacMazeSkinAnimClip.DIE ->
                dieFrameIndex(player.deathAnimTime, frameCount)
            PacMazeSkinAnimClip.JUMP -> {
                val jumpCount = frameCount
                val phase = when {
                    player.vy < -80f -> animTime * JUMP_FRAME_RATE * 0.55f
                    player.vy > 80f -> jumpCount * 0.55f + animTime * JUMP_FRAME_RATE * 0.35f
                    else -> jumpCount * 0.35f + animTime * JUMP_FRAME_RATE * 0.25f
                }
                phase.toInt().mod(frameCount)
            }
            PacMazeSkinAnimClip.IDLE -> {
                val raw = (idleLocalAnimTime(animTime) * IDLE_FRAME_RATE).toInt().mod(frameCount)
                mapIdleFrameIndex(raw, frameCount)
            }
            PacMazeSkinAnimClip.WALK, PacMazeSkinAnimClip.RUN -> {
                val perFrame = com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinAnimTiming
                    .walkPhasePerFrame(
                        skinId = skinId,
                        walkPreview = false,
                        frameCount = frameCount,
                    )
                // 走路按 animPhase；loco=false（松手停住）定在第 0 帧
                if (!moving) return 0
                (player.animPhase / perFrame).toInt().mod(frameCount)
            }
            PacMazeSkinAnimClip.ATTACK -> {
                val total = player.rangedAnimTotalSec.coerceAtLeast(0.001f)
                val elapsed = (total - player.rangedAnimSecLeft).coerceIn(0f, total)
                ((elapsed / total) * frameCount).toInt().coerceIn(0, frameCount - 1)
            }
            else -> PacMazeRemoteSkinAnimCatalog.frameIndex(skinId, pose, clip, frameCount)
        }
    }

    fun layoutNormalizedFrame(
        targetH: Float,
        canvasW: Int,
        canvasH: Int,
        feetY: Float,
        feetX: Float,
    ): ChickSpriteLayout = layoutManifestFullScaled(
        targetH = targetH,
        refWidth = canvasW,
        refHeight = canvasH,
        stableFeet = feetY to feetX,
        feetBias = PacMazeIkunGameplayScale.PLATFORMER_FEET_FRAC_BIAS,
    )

    /** @deprecated 保留参数签名；局内与预览一致，整画布缩放到 [targetH]。 */
    fun layoutNormalizedFrame(
        frame: ImageBitmap,
        targetH: Float,
        canvasW: Int,
        canvasH: Int,
        feetY: Float,
        feetX: Float,
        player: PlatformerPlayer,
        cellPx: Float,
    ): ChickSpriteLayout = layoutNormalizedFrame(targetH, canvasW, canvasH, feetY, feetX)

    /** 局内布局：固定 manifest 画布 + 稳定脚点，各 walk/jump 帧在框内对齐，消除帧间位移/缩放闪。 */
    fun layoutForDraw(
        frame: ImageBitmap,
        player: PlatformerPlayer,
        cellPx: Float,
        tilePx: Int,
    ): ChickSpriteLayout {
        val scale = (tilePx / PLATFORMER_TILE_PX.toFloat()).coerceIn(0.5f, 1f)
        val characterH = cellPx * drawHeightCells * scale
        refreshStableFeetAnchor()
        val manifest = PacMazeSkinAnimManifest.loadForSkin(skinId)
        val stableFeet = platformerStableFeetAnchor()
        val refW = manifest?.canvas?.w ?: cachedWalkRefWidth ?: frame.width
        val refH = manifest?.canvas?.h ?: cachedWalkRefHeight ?: frame.height
        val airborne = shouldUseJumpClip(player) && !player.dying
        val topPad = if (airborne) characterH * JUMP_HEAD_SCREEN_MARGIN_FRAC else 0f
        if (manifest?.normalized == true && manifest.canvas != null) {
            return layoutInStableClipBox(
                frame = frame,
                targetH = characterH,
                refWidth = refW,
                refHeight = refH,
                stableFeet = stableFeet,
                topPad = topPad,
                alignFrameFeetToManifest = true,
            )
        }
        return layoutInStableClipBox(
            frame = frame,
            targetH = characterH,
            refWidth = cachedWalkBoxWidth ?: refW,
            refHeight = cachedWalkBoxHeight ?: refH,
            stableFeet = stableFeet,
            topPad = topPad,
            alignFrameFeetToManifest = true,
        )
    }

    private fun layoutLegacy(
        frame: ImageBitmap,
        targetH: Float,
    ): ChickSpriteLayout {
        val aspect = frame.width.toFloat() / frame.height.coerceAtLeast(1)
        val dstH = targetH
        val dstW = dstH * aspect
        val feet = PacMazeBitmapFeetAnchor.platformerFeetAnchor(frame, skinId)
        return ChickSpriteLayout(
            dstW = dstW,
            dstH = dstH,
            feetYFrac = feet.first,
            feetXFrac = feet.second,
            frameW = dstW,
            frameH = dstH,
        )
    }

    fun layoutForPreview(maxHeightPx: Float, frame: ImageBitmap): ChickSpriteLayout {
        refreshStableFeetAnchor()
        val manifest = PacMazeSkinAnimManifest.loadForSkin(skinId)
        val feet = PacMazeBitmapFeetAnchor.platformerFeetAnchor(frame, skinId)
        if (manifest?.normalized == true && manifest.canvas != null) {
            // 加载页预览框已限定高度，勿再用 canvasHeightForCharacterTarget（会把角色放大 ~2.4x 溢出）。
            return layoutManifestFullScaled(
                targetH = maxHeightPx,
                refWidth = manifest.canvas.w,
                refHeight = manifest.canvas.h,
                stableFeet = feet.first to feet.second,
            )
        }
        return layoutInStableClipBox(
            frame = frame,
            targetH = maxHeightPx,
            refWidth = cachedWalkRefWidth ?: frame.width,
            refHeight = cachedWalkRefHeight ?: frame.height,
            stableFeet = stableGameplayFeetAnchor(),
        )
    }

    fun stableGameplayFeetAnchor(): Pair<Float, Float> {
        cachedStableFeet?.let { return it }
        refreshStableFeetAnchor()
        return cachedStableFeet ?: PacMazeBitmapFeetAnchor.gameplayFeetAnchorForSkin(skinId)
    }

    /** 横版绘制用稳定脚点（manifest 优先 + 横版贴地 bias）。 */
    private fun platformerStableFeetAnchor(): Pair<Float, Float> {
        val raw = PacMazeBitmapFeetAnchor.normalizedFeetAnchor(skinId) ?: stableGameplayFeetAnchor()
        val y = (raw.first - PacMazeIkunGameplayScale.PLATFORMER_FEET_FRAC_BIAS)
            .coerceIn(PacMazeIkunGameplayScale.FEET_Y_FRAC_MIN, 0.999f)
        return y to raw.second.coerceIn(0.12f, 0.88f)
    }

    private fun refreshStableFeetAnchor() {
        PacMazeSkinAnimManifest.loadForSkin(skinId)?.let { manifest ->
            if (manifest.normalized && manifest.anchorFrac != null) {
                PacMazeBitmapFeetAnchor.registerManifestAnchor(skinId, manifest.anchorFrac)
                manifest.canvas?.let { canvas ->
                    cachedWalkRefWidth = canvas.w
                    cachedWalkRefHeight = canvas.h
                    cachedWalkBoxWidth = canvas.w
                    cachedWalkBoxHeight = canvas.h
                    cachedReferenceAspect = canvas.w.toFloat() / canvas.h.coerceAtLeast(1)
                }
                cachedStableFeet = manifest.anchorFrac.let { it.y to it.x }
                cachedWalkContentSpanFrac = null
                return
            }
        }
        cachedWalkContentSpanFrac = null
        val walkRef = PacMazeRemoteSkinAnimCache.peekSingleWalkFrame(skinId)
            ?: PacMazeRemoteSkinAnimCache.playbackFrames(skinId, PacMazeSkinAnimClip.WALK)?.firstOrNull()
        if (walkRef != null) {
            cachedStableFeet = PacMazeBitmapFeetAnchor.platformerFeetAnchor(walkRef, skinId)
            cachedReferenceAspect = walkRef.width.toFloat() / walkRef.height.coerceAtLeast(1)
            cachedWalkRefWidth = walkRef.width
            cachedWalkRefHeight = walkRef.height
            PacMazeBitmapFeetAnchor.registerGameplayAnchor(skinId, walkRef, asDefault = true)
        }
        PacMazeRemoteSkinAnimCache.playbackFrames(skinId, PacMazeSkinAnimClip.JUMP)?.firstOrNull()
            ?.let { PacMazeBitmapFeetAnchor.registerGameplayAnchor(skinId, it, asDefault = false) }
        refreshClipBoxBounds()
    }

    private fun refreshClipBoxBounds() {
        fun maxSize(clip: PacMazeSkinAnimClip): Pair<Int, Int>? {
            val frames = PacMazeRemoteSkinAnimCache.playbackFrames(skinId, clip) ?: return null
            if (frames.isEmpty()) return null
            return frames.maxOf { it.width } to frames.maxOf { it.height }
        }
        maxSize(PacMazeSkinAnimClip.WALK)?.let { (w, h) ->
            cachedWalkBoxWidth = w
            cachedWalkBoxHeight = h
        }
        maxSize(PacMazeSkinAnimClip.JUMP)?.let { (jw, jh) ->
            cachedWalkBoxWidth = maxOf(cachedWalkBoxWidth ?: jw, jw)
            cachedWalkBoxHeight = maxOf(cachedWalkBoxHeight ?: jh, jh)
        }
    }

    fun mirrorHorizontally(facingRight: Boolean): Boolean {
        val facesRight = PacMazeSkinRenderProfileCatalog.defaultFacesRight(skinId)
        val facing = if (facingRight) Direction.RIGHT else Direction.LEFT
        return PacMazeSkinTransform.horizontalMirror(facing, facesRight)
    }

    /** sheet 离地：按 manifest 发顶对齐 walk 参考线，不放大布局框。 */
    fun stabilizeAirborneFeetScreenForSheet(
        layout: ChickSpriteLayout,
        feetScreen: Offset,
        player: PlatformerPlayer,
        clip: PacMazeSkinAnimClip,
        frameIndex: Int,
    ): Offset {
        if (!shouldUseJumpClip(player) || player.dying) return feetScreen
        if (clip == PacMazeSkinAnimClip.WALK) return feetScreen
        val refHead = walkMinHeadTopFrac()
        val frameHead = PacMazeSkinAnimManifest.platformerClipMetrics(skinId, clip)
            ?.getOrNull(frameIndex)?.headTopY ?: return feetScreen
        val shift = (refHead - frameHead) * layout.frameH
        if (kotlin.math.abs(shift) < 0.5f) return feetScreen
        return feetScreen.copy(y = feetScreen.y + shift)
    }

    fun adjustFeetScreenKeepHeadVisibleForSheet(
        layout: ChickSpriteLayout,
        feetScreen: Offset,
        minHeadTopPx: Float,
        player: PlatformerPlayer,
        clip: PacMazeSkinAnimClip,
        frameIndex: Int,
    ): Offset {
        if (player.grounded && !player.dying) return feetScreen
        if (!shouldUseJumpClip(player)) return feetScreen
        val headTopFrac = PacMazeSkinAnimManifest.platformerClipMetrics(skinId, clip)
            ?.getOrNull(frameIndex)?.headTopY ?: return feetScreen
        val drawTop = feetScreen.y - layout.frameH * layout.feetYFrac + layout.frameOy
        val headTopY = drawTop + layout.frameH * headTopFrac
        val shift = minHeadTopPx - headTopY
        if (shift <= 0f) return feetScreen
        return feetScreen.copy(y = feetScreen.y + shift)
    }

    /** sheet 路径：轻提脚底，不用 [alignGroundedFeetScreen] 的下沉。 */
    fun alignFeetScreenForSheet(
        feetScreen: Offset,
        player: PlatformerPlayer,
        cellPx: Float,
    ): Offset {
        if (!player.grounded || player.dying) return feetScreen
        return feetScreen.copy(y = feetScreen.y + cellPx * CHICK_SHEET_FEET_LIFT_CELL_FRAC)
    }

    /** 站立：仅做贴地微沉，布局已在稳定框内对齐脚点。 */
    fun alignGroundedFeetScreen(
        feetScreen: Offset,
        player: PlatformerPlayer,
        cellPx: Float,
    ): Offset {
        if (!player.grounded || player.dying) return feetScreen
        val sink = cellPx * PacMazeIkunGameplayScale.PLATFORMER_FEET_GROUND_NUDGE_CELL_FRAC
        return feetScreen.copy(y = feetScreen.y + sink)
    }

    /** 离地：发顶对齐 walk 参考线，消除 jump 帧间 8–20px 跳动。 */
    fun stabilizeAirborneFeetScreen(
        frame: ImageBitmap,
        layout: ChickSpriteLayout,
        feetScreen: Offset,
        player: PlatformerPlayer,
    ): Offset {
        if (!shouldUseJumpClip(player) || player.dying) return feetScreen
        val refHeadFrac = walkMinHeadTopFrac()
        val frameHeadFrac = PacMazeBitmapFeetAnchor.platformerHeadTopFraction(frame, skinId)
        val shift = (refHeadFrac - frameHeadFrac) * layout.frameH
        if (kotlin.math.abs(shift) < 0.5f) return feetScreen
        return feetScreen.copy(y = feetScreen.y + shift)
    }

    /** 保证贴图顶 / 发顶不低于 [minHeadTopPx]，防止跳高后 y<0 被裁切。 */
    fun adjustFeetScreenKeepHeadVisible(
        frame: ImageBitmap,
        layout: ChickSpriteLayout,
        feetScreen: Offset,
        minHeadTopPx: Float,
        player: PlatformerPlayer,
    ): Offset {
        if (player.grounded && !player.dying) return feetScreen
        if (!shouldUseJumpClip(player)) return feetScreen
        val headTopFrac = PacMazeBitmapFeetAnchor.platformerHeadTopFraction(frame, skinId)
        val drawTop = feetScreen.y - layout.frameH * layout.feetYFrac + layout.frameOy
        val headTopY = drawTop + layout.frameH * headTopFrac
        val shiftForHead = minHeadTopPx - headTopY
        val shiftForBitmapTop = minHeadTopPx - drawTop
        val shift = maxOf(shiftForHead, shiftForBitmapTop)
        if (shift <= 0f) return feetScreen
        return feetScreen.copy(y = feetScreen.y + shift)
    }

    fun minHeadTopPxForPlayer(cellPx: Float, player: PlatformerPlayer): Float {
        val base = if (shouldUseJumpClip(player) && !player.dying) 0.38f else 0.14f
        return cellPx * base
    }
}
