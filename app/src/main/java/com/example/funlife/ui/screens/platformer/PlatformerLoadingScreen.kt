package com.example.funlife.ui.screens.platformer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.funlife.game.platformer.PlatformerPlayerSprites
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinAnimCache
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinAnimClip
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.RemoteSkinLoadPhase
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

private val Ink = Color(0xFF0E1524)
private val Glass = Color(0xCC141C2B)
private val GlassBorder = Color(0x33FFFFFF)
private val TextPrimary = Color(0xFFF2F5FA)
private val TextSecondary = Color(0x99A8B8CC)
private val Accent = Color(0xFF6CB4FF)
private val AccentDone = Color(0xFF5AD8A6)
private val Track = Color(0xFF2A3448)

private val LOADING_TIPS = listOf(
    "透明摇杆控制方向，右侧按钮跳跃",
    "空中再按一次可二段跳",
    "收集蓝色宝石，抵达金色终点过关",
    "小心地面空隙，会从脚下掉下去",
    "行走小鸡 Pro Max 自带跳跃动画",
)

@Composable
fun PlatformerLoadingScreen(
    message: String,
    progress: Int,
    modifier: Modifier = Modifier,
    title: String = "坤坤大冒险",
    subtitle: String = "行走小鸡 Pro Max",
    phaseLabel: String? = null,
    onBack: (() -> Unit)? = null,
) {
    val displayMessage = GameResourceLoadCopy.forDisplay(message)
    val displayPhase = phaseLabel?.let { GameResourceLoadCopy.phaseLabel(it) }
    var animTime by remember { mutableFloatStateOf(0f) }
    var tipIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        var lastNs = 0L
        while (true) {
            withFrameNanos { frameNs ->
                if (lastNs != 0L) {
                    animTime += ((frameNs - lastNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
                }
                lastNs = frameNs
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3600)
            tipIndex = (tipIndex + 1) % LOADING_TIPS.size
        }
    }

    val targetProgress = progress.coerceIn(0, 100)
    val displayProgress by animateIntAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = if (targetProgress >= 100) 0 else 220),
        label = "loadProgress",
    )

    BoxWithConstraints(modifier.fillMaxSize()) {
        val compact = maxHeight < 340.dp || maxWidth < 560.dp
        val heroSize = min(maxHeight.value * 0.52f, maxWidth.value * 0.18f).coerceIn(72f, 118f).dp
        val cardMaxW = min(maxWidth.value * 0.52f, 400f).coerceAtLeast(240f).dp
        val hPad = if (compact) 12.dp else 20.dp
        val useSideBySide = maxWidth >= 520.dp && maxHeight >= 260.dp

        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) {
                drawLoadingBackdrop(this, animTime, size.width, size.height)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = hPad, vertical = if (compact) 6.dp else 10.dp),
            ) {
                CompactTopBar(
                    title = title,
                    subtitle = subtitle,
                    compact = compact,
                    onBack = onBack,
                )

                Spacer(Modifier.height(if (compact) 4.dp else 8.dp))

                if (useSideBySide) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(0.36f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center,
                        ) {
                            LoadingHeroPreview(
                                animTime = animTime,
                                boxSize = heroSize,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(0.64f)
                                .fillMaxHeight()
                                .padding(start = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            LoadingStatusCard(
                                message = displayMessage,
                                progress = displayProgress,
                                targetProgress = targetProgress,
                                tip = LOADING_TIPS[tipIndex],
                                phaseLabel = displayPhase,
                                compact = compact,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = cardMaxW),
                            )
                        }
                    }
                } else {
                    val scroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scroll),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        LoadingHeroPreview(
                            animTime = animTime,
                            boxSize = heroSize.coerceAtMost(96.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        LoadingStatusCard(
                            message = displayMessage,
                            progress = displayProgress,
                            targetProgress = targetProgress,
                            tip = LOADING_TIPS[tipIndex],
                            phaseLabel = displayPhase,
                            compact = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactTopBar(
    title: String,
    subtitle: String,
    compact: Boolean,
    onBack: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 32.dp else 36.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBack)
                    .background(Color(0x66000000))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "返回",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        } else {
            Spacer(Modifier.width(if (compact) 52.dp else 58.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        ) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = if (compact) 15.sp else 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = if (compact) 10.sp else 11.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun rememberPlatformerLoadProgress(
    fallbackMessage: String,
    fallbackProgress: Int,
    canEnter: Boolean = false,
): Pair<String, Int> {
    val allStatus by PacMazeRemoteSkinAnimCache.status.collectAsStateWithLifecycle()
    val remote = allStatus[PlatformerPlayerSprites.skinId]
    val walkFrames = PacMazeRemoteSkinAnimCache.playbackFrameCount(
        PlatformerPlayerSprites.skinId,
        PacMazeSkinAnimClip.WALK,
    )
    val walkTarget = PacMazeRemoteSkinAnimCache.BOOT_WALK_FRAMES

    val remoteProgress = when (remote?.phase) {
        RemoteSkinLoadPhase.Ready -> 95
        RemoteSkinLoadPhase.Downloading -> 20 + remote.percent.coerceIn(0, 100) * 28 / 100
        RemoteSkinLoadPhase.Decoding -> 40 + remote.percent.coerceIn(0, 100) * 55 / 100
        else -> null
    }
    val frameProgress = if (walkFrames > 0) {
        40 + walkFrames.coerceAtMost(walkTarget) * 55 / walkTarget
    } else {
        null
    }

    val message = when {
        canEnter -> fallbackMessage
        fallbackProgress >= 92 && fallbackMessage.isNotBlank() -> fallbackMessage
        remote?.message?.isNotBlank() == true &&
            remote.phase != RemoteSkinLoadPhase.None &&
            remote.phase != RemoteSkinLoadPhase.Ready &&
            remote.phase != RemoteSkinLoadPhase.Failed -> remote.message
        walkFrames in 1 until walkTarget ->
            GameResourceLoadCopy.progress("加载角色动作资源", walkFrames, walkTarget)
        else -> fallbackMessage
    }

    val progress = when {
        canEnter -> 100
        fallbackProgress >= 100 -> 100
        fallbackProgress >= 92 -> fallbackProgress.coerceIn(92, 99)
        else -> maxOf(
            fallbackProgress,
            remoteProgress ?: 0,
            frameProgress ?: 0,
        ).coerceIn(0, 99)
    }

    return GameResourceLoadCopy.forDisplay(message) to progress
}

@Composable
private fun LoadingHeroPreview(
    animTime: Float,
    boxSize: Dp,
    modifier: Modifier = Modifier,
) {
    val skinId = PlatformerPlayerSprites.skinId

    LaunchedEffect(skinId) {
        PacMazeRemoteSkinAnimCache.requestPreloadCoverAsync(skinId)
    }

    val cover = PacMazeRemoteSkinAnimCache.cover(skinId)
    val single = PacMazeRemoteSkinAnimCache.peekSingleWalkFrame(skinId)
    val sprite: ImageBitmap? = cover ?: single
    val bob = sin(animTime * 4.5f) * 2.5f
    val density = LocalDensity.current

    Box(
        modifier = modifier.size(boxSize),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val shadowW = size.width * 0.42f
            val shadowH = size.height * 0.06f
            drawOval(
                color = Color(0x40000000),
                topLeft = Offset((size.width - shadowW) * 0.5f, size.height * 0.88f),
                size = Size(shadowW, shadowH),
            )
        }

        if (sprite != null) {
            val maxH = with(density) { (boxSize * 0.82f).toPx() }
            val layout = PlatformerPlayerSprites.layoutForPreview(maxH, sprite)
            val w = layout.dstW
            val h = layout.dstH
            val footScreenY = with(density) { boxSize.toPx() * 0.86f }
            val top = footScreenY - h * layout.feetYFrac + bob + layout.frameOy
            val left = (with(density) { boxSize.toPx() } - w) * 0.5f
            Canvas(Modifier.fillMaxSize()) {
                drawImage(
                    image = sprite,
                    dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                    dstSize = IntSize(
                        layout.frameW.roundToInt().coerceAtLeast(1),
                        layout.frameH.roundToInt().coerceAtLeast(1),
                    ),
                )
            }
        } else {
            PlaceholderChick(animTime, Modifier.size(boxSize * 0.62f))
        }
    }
}

@Composable
private fun PlaceholderChick(animTime: Float, modifier: Modifier = Modifier) {
    val bounce = sin(animTime * 5f) * 4f
    Canvas(modifier.padding(bottom = bounce.dp)) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.55f
        drawCircle(Color(0xFFFFD54F), radius = size.minDimension * 0.28f, center = Offset(cx, cy))
        drawCircle(Color.White, radius = size.minDimension * 0.09f, center = Offset(cx - 14f, cy - 10f))
        drawCircle(Color.White, radius = size.minDimension * 0.09f, center = Offset(cx + 14f, cy - 10f))
        drawCircle(Color.Black, radius = 4f, center = Offset(cx - 12f, cy - 8f))
        drawCircle(Color.Black, radius = 4f, center = Offset(cx + 16f, cy - 8f))
    }
}

@Composable
private fun LoadingStatusCard(
    message: String,
    progress: Int,
    targetProgress: Int,
    tip: String,
    phaseLabel: String? = null,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val complete = progress >= 100
    val stages = listOf("资源", "角色", "就绪")
    val stageIndex = when {
        complete -> 2
        progress >= 35 -> 1
        progress >= 12 -> 0
        else -> 0
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Glass)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StageStepper(
            stages = stages,
            stageIndex = stageIndex,
            complete = complete,
            compact = compact,
        )

        if (phaseLabel != null && !complete) {
            Text(
                text = phaseLabel,
                color = TextSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                color = TextPrimary,
                fontSize = if (compact) 12.sp else 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "$progress%",
                color = if (complete) AccentDone else Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        SlimProgressBar(
            progress = progress / 100f,
            indeterminate = !complete && progress == targetProgress && progress in 18..94,
        )

        Text(
            text = tip,
            color = TextSecondary,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StageStepper(
    stages: List<String>,
    stageIndex: Int,
    complete: Boolean,
    compact: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        stages.forEachIndexed { i, label ->
            val done = i < stageIndex || (complete && i == stageIndex)
            val current = i == stageIndex && !complete
            val dotColor = when {
                done -> AccentDone
                current -> Accent
                else -> Track
            }
            Box(
                modifier = Modifier
                    .size(if (compact) 6.dp else 7.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            if (i < stages.lastIndex) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(if (i < stageIndex) AccentDone.copy(alpha = 0.6f) else Track),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = stages[stageIndex.coerceIn(0, stages.lastIndex)],
            color = if (complete) AccentDone else Accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun SlimProgressBar(
    progress: Float,
    indeterminate: Boolean,
) {
    val filled = progress.coerceIn(0f, 1f)
    val pulse by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    val displayFill = if (indeterminate) max(filled, 0.12f) else filled

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Track),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(displayFill)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Accent.copy(alpha = if (indeterminate) pulse else 0.85f),
                            AccentDone.copy(alpha = if (indeterminate) pulse else 0.95f),
                        ),
                    ),
                ),
        )
    }
}

private fun drawLoadingBackdrop(
    scope: androidx.compose.ui.graphics.drawscope.DrawScope,
    animTime: Float,
    w: Float,
    h: Float,
) {
    scope.drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Ink, Color(0xFF152238), Color(0xFF1A3050)),
            startY = 0f,
            endY = h,
        ),
        size = Size(w, h),
    )

    scope.drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x226CB4FF), Color.Transparent),
            center = Offset(w * 0.72f, h * 0.38f),
            radius = min(w, h) * 0.55f,
        ),
        size = Size(w, h),
    )

    repeat(3) { i ->
        val cloudX = ((i * 260f + animTime * 18f) % (w + 180f)) - 90f
        val cloudY = h * (0.1f + i * 0.06f)
        drawCloud(scope, Offset(cloudX, cloudY), min(w * 0.08f, 36f), alpha = 0.35f - i * 0.08f)
    }

    val groundTop = h * 0.92f
    scope.drawRect(
        color = Color(0xFF1E3D2A),
        topLeft = Offset(0f, groundTop),
        size = Size(w, h - groundTop),
    )
    val tile = (w / 40f).coerceIn(10f, 24f)
    var tx = 0f
    while (tx < w + tile) {
        scope.drawRect(Color(0xFF2E5938), Offset(tx, groundTop), Size(tile - 1f, tile * 0.35f))
        tx += tile
    }
}

private fun drawCloud(
    scope: androidx.compose.ui.graphics.drawscope.DrawScope,
    center: Offset,
    size: Float,
    alpha: Float,
) {
    val c = Color.White.copy(alpha = alpha)
    scope.drawCircle(c, size * 0.35f, Offset(center.x - size * 0.3f, center.y))
    scope.drawCircle(c, size * 0.42f, Offset(center.x, center.y - size * 0.08f))
    scope.drawCircle(c, size * 0.38f, Offset(center.x + size * 0.32f, center.y))
}
