// PandaPetCanvas.kt - 🐼 Canvas 手绘熊猫宠物（无需图片资源）
// 完整动画系统：6 种待机变体 + 喂食(拿竹子) + 洗澡(浴桶+泡沫) + 玩耍(追球) + 抚摸 + 升级
package com.example.funlife.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.viewmodel.AnimationState
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ════════════════════════════════════════════════════════════
// 🎭 待机变体 - 6 种不同的待机动作
// ════════════════════════════════════════════════════════════
enum class PandaIdleVariant {
    SITTING,     // 静坐(默认) - 呼吸 + 眨眼
    LYING,       // 趴卧 - 身体压扁,头放低
    STRETCHING,  // 伸懒腰 - 双臂上举,身体拉长
    YAWNING,     // 打哈欠 - 大嘴张开 + 闭眼
    SCRATCHING,  // 挠痒痒 - 一只手举到耳朵
    WALKING      // 爬行/走路 - 横向移动 + 四肢交替
}

// ════════════════════════════════════════════════════════════
// 🐼 主入口 - 根据动画状态切换不同动画
// ════════════════════════════════════════════════════════════
@Composable
fun PandaPet(
    animationState: AnimationState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 待机变体随机切换 (3~6 秒切换一次)
    var idleVariant by remember { mutableStateOf(PandaIdleVariant.SITTING) }
    LaunchedEffect(animationState) {
        if (animationState == AnimationState.Idle) {
            while (true) {
                val dur = (3000L..6000L).random()
                delay(dur)
                // 加权随机：坐 30%、走 25%、其他各 ~11%
                idleVariant = when ((1..100).random()) {
                    in 1..30 -> PandaIdleVariant.SITTING
                    in 31..55 -> PandaIdleVariant.WALKING
                    in 56..67 -> PandaIdleVariant.LYING
                    in 68..78 -> PandaIdleVariant.STRETCHING
                    in 79..89 -> PandaIdleVariant.YAWNING
                    else -> PandaIdleVariant.SCRATCHING
                }
            }
        } else {
            idleVariant = PandaIdleVariant.SITTING
        }
    }

    // 走路时的水平位移 (持续左右来回)
    val walkOffsetX by rememberInfiniteTransition(label = "walk").animateFloat(
        initialValue = -50f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "walkOffsetX"
    )
    val isWalking = animationState == AnimationState.Idle && idleVariant == PandaIdleVariant.WALKING

    // 走路时身体的小幅上下颠簸
    val walkBobY by rememberInfiniteTransition(label = "walkBob").animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(280, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "walkBobY"
    )

    // 走路腿部相位 (用于交替)
    val legPhase by rememberInfiniteTransition(label = "legs").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(560, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "legPhase"
    )

    // 朝向: 走路时朝运动方向 (向右为正)
    val faceFlip = if (isWalking && walkOffsetX < 0) -1f else 1f

    // 呼吸缩放
    val breath by rememberInfiniteTransition(label = "breath").animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    // 眨眼控制
    var blink by remember { mutableStateOf(false) }
    LaunchedEffect(animationState, idleVariant) {
        // 这些状态持续闭眼/眯眼,不需要额外 blink
        if (animationState == AnimationState.Petting ||
            idleVariant == PandaIdleVariant.YAWNING ||
            idleVariant == PandaIdleVariant.STRETCHING ||
            idleVariant == PandaIdleVariant.LYING
        ) {
            blink = true
        } else {
            blink = false
            while (true) {
                delay(2500 + (0..1500).random().toLong())
                blink = true
                delay(150)
                blink = false
            }
        }
    }

    // 耳朵抖动
    val earWiggle by rememberInfiniteTransition(label = "ear").animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "earWiggle"
    )

    // 玩耍跳跃 (追球)
    val playBounce by rememberInfiniteTransition(label = "playB").animateFloat(
        initialValue = 0f,
        targetValue = -25f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "playBounceY"
    )
    // 玩耍倾斜
    val playTilt by rememberInfiniteTransition(label = "tilt").animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "playTiltAngle"
    )
    val activeBounce = if (animationState == AnimationState.Playing) playBounce else 0f
    val activeTilt = if (animationState == AnimationState.Playing) playTilt else 0f

    // 抚摸时身体微微缩放
    val actionScale by animateFloatAsState(
        targetValue = when (animationState) {
            AnimationState.Feeding -> 1.05f
            AnimationState.Playing -> 1.10f
            AnimationState.LevelUp -> 1.25f
            AnimationState.Petting -> 1.05f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "actionScale"
    )

    // 吃东西嘴巴开合相位
    val chewPhase by rememberInfiniteTransition(label = "chew").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chewPhase"
    )

    // 升级光环旋转
    val haloRotation by rememberInfiniteTransition(label = "halo").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloRotation"
    )

    // 打哈欠相位 (嘴大小)
    val yawnPhase by rememberInfiniteTransition(label = "yawn").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "yawnPhase"
    )

    // 挠痒痒手抖动
    val scratchPhase by rememberInfiniteTransition(label = "scratch").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(180, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scratchPhase"
    )

    // 伸懒腰展开度
    val stretchPhase by rememberInfiniteTransition(label = "stretch").animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stretchPhase"
    )

    Box(
        modifier = modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        // 升级光环
        if (animationState == AnimationState.LevelUp) {
            Canvas(modifier = Modifier.size(260.dp).rotate(haloRotation)) {
                val center = Offset(size.width / 2, size.height / 2)
                val ringR = size.minDimension / 2 - 6f
                for (i in 0 until 12) {
                    val angle = (i * 30f) * (PI / 180f).toFloat()
                    val px = center.x + cos(angle) * ringR
                    val py = center.y + sin(angle) * ringR
                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = 0.85f),
                        radius = 8f,
                        center = Offset(px, py)
                    )
                }
            }
        }

        // 阴影 (走路时随身体移动)
        Box(
            modifier = Modifier
                .size(150.dp, 22.dp)
                .offset(
                    x = if (isWalking) walkOffsetX.dp else 0.dp,
                    y = 110.dp
                )
                .scale(breath * 0.85f)
                .background(
                    Brush.radialGradient(
                        listOf(Color.Black.copy(alpha = 0.22f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        // 洗澡：木浴桶 (压在熊猫下半身)
        if (animationState == AnimationState.Cleaning) {
            WoodenTub(
                modifier = Modifier
                    .size(240.dp, 120.dp)
                    .offset(y = 70.dp)
            )
            // 蒸汽 (从桶上方升起，背景层)
            SteamWaves(
                modifier = Modifier
                    .size(200.dp, 90.dp)
                    .offset(y = 40.dp)
            )
            // 上方淋浴喷头水滴
            ShowerSpray(
                modifier = Modifier
                    .size(280.dp, 200.dp)
                    .offset(y = (-40).dp)
            )
        }

        // 玩耍：弹跳的彩色球
        if (animationState == AnimationState.Playing) {
            BouncingPlayBall(
                modifier = Modifier
                    .size(60.dp)
                    .offset(x = 90.dp, y = 30.dp)
            )
        }

        // 喂食：加分粒子 (胃口大开)
        if (animationState == AnimationState.Feeding) {
            FoodParticles()
        }

        // 洗澡：泡泡 (在熊猫身上方升起)
        if (animationState == AnimationState.Cleaning) {
            BubbleField()
        }

        // 抚摸：飘心
        if (animationState == AnimationState.Petting) {
            FloatingPandaHearts()
        }

        // ───── 主体熊猫 ─────
        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(
                    x = if (isWalking) walkOffsetX.dp else 0.dp,
                    y = (activeBounce + if (isWalking) walkBobY else 0f).dp
                )
                .scale(breath * actionScale)
                .rotate(activeTilt),
            contentAlignment = Alignment.Center
        ) {
            // 朝向翻转
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scaleX = faceFlip, scaleY = 1f)
                    .clickable(
                        onClick = onClick,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                PandaCanvas(
                    blink = blink,
                    isHappy = animationState == AnimationState.Petting ||
                            animationState == AnimationState.Playing ||
                            animationState == AnimationState.LevelUp,
                    isEating = animationState == AnimationState.Feeding,
                    chewPhase = chewPhase,
                    earOffset = earWiggle,
                    idleVariant = if (animationState == AnimationState.Idle) idleVariant else PandaIdleVariant.SITTING,
                    yawnPhase = yawnPhase,
                    scratchPhase = scratchPhase,
                    stretchPhase = stretchPhase,
                    legPhase = legPhase,
                    isInCleaning = animationState == AnimationState.Cleaning
                )
            }
        }

        // 洗澡前景层：小黄鸭、香皂、水花溅射 (画在熊猫上方)
        if (animationState == AnimationState.Cleaning) {
            // 小黄鸭浮在水面 (桶左侧)
            RubberDuck(
                modifier = Modifier
                    .size(56.dp, 48.dp)
                    .offset(x = (-95).dp, y = 88.dp)
            )
            // 香皂 (桶右侧水面)
            SoapBar(
                modifier = Modifier
                    .size(50.dp, 30.dp)
                    .offset(x = 92.dp, y = 95.dp)
            )
            // 水花溅射
            WaterSplashes()
        }

        // 升级飘星
        if (animationState == AnimationState.LevelUp) {
            LevelUpStars()
        }
    }
}

// ════════════════════════════════════════════════════════════
// 🐼 熊猫主体绘制 (根据各种状态参数变化)
// ════════════════════════════════════════════════════════════
@Composable
private fun PandaCanvas(
    blink: Boolean,
    isHappy: Boolean,
    isEating: Boolean,
    chewPhase: Float,
    earOffset: Float,
    idleVariant: PandaIdleVariant,
    yawnPhase: Float,
    scratchPhase: Float,
    stretchPhase: Float,
    legPhase: Float,
    isInCleaning: Boolean
) {
    val bodyWhite = Color(0xFFFFFAF5)
    val patchBlack = Color(0xFF2B2B2B)
    val noseColor = Color(0xFF1A1A1A)
    val cheekPink = Color(0xFFFFB1C8)
    val mouthColor = Color(0xFF222222)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2

        // 根据待机变体调整身体形状
        val bodySquish = when (idleVariant) {
            PandaIdleVariant.LYING -> 0.65f
            PandaIdleVariant.STRETCHING -> 1f + (stretchPhase - 0.6f) * 0.15f
            else -> 1f
        }
        val bodyTopShift = when (idleVariant) {
            PandaIdleVariant.LYING -> h * 0.08f
            else -> 0f
        }

        // 身体
        val bodyTop = h * 0.45f + bodyTopShift
        val bodyW = w * 0.78f
        val bodyH = h * 0.50f * bodySquish
        drawOval(
            color = bodyWhite,
            topLeft = Offset(cx - bodyW / 2, bodyTop),
            size = Size(bodyW, bodyH)
        )
        drawOval(
            color = patchBlack.copy(alpha = 0.15f),
            topLeft = Offset(cx - bodyW / 2, bodyTop),
            size = Size(bodyW, bodyH),
            style = Stroke(width = 3f)
        )

        // 走路时的脚 (交替前后)
        if (idleVariant == PandaIdleVariant.WALKING) {
            val legW = w * 0.20f
            val legH = h * 0.16f
            val legBaseY = bodyTop + bodyH - legH * 0.20f
            val swing = sin(legPhase * 2 * PI).toFloat() * 8f
            // 左脚
            drawOval(
                color = patchBlack,
                topLeft = Offset(cx - bodyW * 0.30f - legW / 2, legBaseY + swing),
                size = Size(legW, legH)
            )
            // 右脚 (反相)
            drawOval(
                color = patchBlack,
                topLeft = Offset(cx + bodyW * 0.30f - legW / 2, legBaseY - swing),
                size = Size(legW, legH)
            )
        } else {
            // 静态后腿
            val legW = w * 0.22f
            val legH = h * 0.18f * bodySquish
            drawOval(
                color = patchBlack,
                topLeft = Offset(cx - bodyW / 2 + legW * 0.10f, bodyTop + bodyH - legH * 0.35f),
                size = Size(legW, legH)
            )
            drawOval(
                color = patchBlack,
                topLeft = Offset(cx + bodyW / 2 - legW * 1.10f, bodyTop + bodyH - legH * 0.35f),
                size = Size(legW, legH)
            )
        }

        // 黑色手臂 - 多种状态下不同位置
        val armW = w * 0.20f
        val armH = h * 0.30f
        val baseArmLeftX = cx - bodyW / 2 - armW * 0.25f
        val baseArmRightX = cx + bodyW / 2 - armW * 0.75f
        val baseArmY = bodyTop + bodyH * 0.10f

        when {
            // 喂食：左手默认 + 右手稍微抬起 (具体竹子/手在头部后绘制以便覆盖)
            isEating -> {
                drawOval(
                    color = patchBlack,
                    topLeft = Offset(baseArmLeftX, baseArmY),
                    size = Size(armW, armH)
                )
                // 右手抬起朝嘴部，从身体伸出（这只是身体侧的手臂段）
                val armX = cx + bodyW * 0.15f
                val armY = bodyTop + bodyH * 0.05f
                rotate(degrees = -45f, pivot = Offset(armX + armW * 0.4f, armY + armH * 0.7f)) {
                    drawOval(
                        color = patchBlack,
                        topLeft = Offset(armX, armY),
                        size = Size(armW * 0.85f, armH * 0.95f)
                    )
                }
            }
            // 挠痒痒：右手举到耳朵
            idleVariant == PandaIdleVariant.SCRATCHING -> {
                drawOval(
                    color = patchBlack,
                    topLeft = Offset(baseArmLeftX, baseArmY),
                    size = Size(armW, armH)
                )
                val swing = sin(scratchPhase * 2 * PI).toFloat() * 6f
                val raisedX = cx + bodyW * 0.15f + swing
                val raisedY = h * 0.10f
                rotate(degrees = -50f, pivot = Offset(raisedX + armW / 2, raisedY + armH)) {
                    drawOval(
                        color = patchBlack,
                        topLeft = Offset(raisedX, raisedY),
                        size = Size(armW * 0.8f, armH * 0.85f)
                    )
                }
            }
            // 伸懒腰：双手向上
            idleVariant == PandaIdleVariant.STRETCHING -> {
                val raise = stretchPhase * 30f
                rotate(degrees = -25f, pivot = Offset(baseArmLeftX + armW / 2, baseArmY + armH)) {
                    drawOval(
                        color = patchBlack,
                        topLeft = Offset(baseArmLeftX, baseArmY - raise),
                        size = Size(armW, armH)
                    )
                }
                rotate(degrees = 25f, pivot = Offset(baseArmRightX + armW / 2, baseArmY + armH)) {
                    drawOval(
                        color = patchBlack,
                        topLeft = Offset(baseArmRightX, baseArmY - raise),
                        size = Size(armW, armH)
                    )
                }
            }
            // 趴卧：双手收前
            idleVariant == PandaIdleVariant.LYING -> {
                drawOval(
                    color = patchBlack,
                    topLeft = Offset(cx - bodyW * 0.35f, bodyTop + bodyH * 0.40f),
                    size = Size(armW * 1.1f, armH * 0.5f)
                )
                drawOval(
                    color = patchBlack,
                    topLeft = Offset(cx + bodyW * 0.10f, bodyTop + bodyH * 0.40f),
                    size = Size(armW * 1.1f, armH * 0.5f)
                )
            }
            // 走路：手臂前后摆动
            idleVariant == PandaIdleVariant.WALKING -> {
                val swing = sin(legPhase * 2 * PI).toFloat() * 12f
                rotate(degrees = -swing, pivot = Offset(baseArmLeftX + armW / 2, baseArmY + armH * 0.2f)) {
                    drawOval(
                        color = patchBlack,
                        topLeft = Offset(baseArmLeftX, baseArmY),
                        size = Size(armW, armH * 0.85f)
                    )
                }
                rotate(degrees = swing, pivot = Offset(baseArmRightX + armW / 2, baseArmY + armH * 0.2f)) {
                    drawOval(
                        color = patchBlack,
                        topLeft = Offset(baseArmRightX, baseArmY),
                        size = Size(armW, armH * 0.85f)
                    )
                }
            }
            // 默认手臂
            else -> {
                drawOval(
                    color = patchBlack,
                    topLeft = Offset(baseArmLeftX, baseArmY),
                    size = Size(armW, armH)
                )
                drawOval(
                    color = patchBlack,
                    topLeft = Offset(baseArmRightX, baseArmY),
                    size = Size(armW, armH)
                )
            }
        }

        // 头部位置 (趴卧时下沉)
        val headR = w * 0.30f
        val headOffsetY = when (idleVariant) {
            PandaIdleVariant.LYING -> h * 0.10f
            PandaIdleVariant.WALKING -> -h * 0.01f
            else -> 0f
        }
        val headCenter = Offset(cx, h * 0.36f + headOffsetY)
        val headTilt = when {
            idleVariant == PandaIdleVariant.SCRATCHING -> 8f
            idleVariant == PandaIdleVariant.YAWNING -> -4f
            else -> 0f
        }

        rotate(degrees = headTilt, pivot = headCenter) {
            drawPandaHead(
                headCenter = headCenter,
                headR = headR,
                bodyWhite = bodyWhite,
                patchBlack = patchBlack,
                noseColor = noseColor,
                cheekPink = cheekPink,
                mouthColor = mouthColor,
                blink = blink,
                isHappy = isHappy,
                isEating = isEating,
                chewPhase = chewPhase,
                earOffset = earOffset,
                idleVariant = idleVariant,
                yawnPhase = yawnPhase,
                isInCleaning = isInCleaning
            )
        }

        // 喂食：在头部之上绘制竹子 + 抓握的爪子，确保对齐嘴部
        if (isEating) {
            drawBambooInHand(
                cx = cx,
                mouthY = headCenter.y + headR * 0.45f,
                headR = headR,
                chewPhase = chewPhase,
                patchBlack = patchBlack
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
// 🎋 喂食时手中的竹子 + 黑色爪子 (画在头部之上)
// ════════════════════════════════════════════════════════════
private fun DrawScope.drawBambooInHand(
    cx: Float,
    mouthY: Float,
    headR: Float,
    chewPhase: Float,
    patchBlack: Color
) {
    // 嘴巴右侧少许，竹子上下贯穿嘴部
    val bambooX = cx + headR * 0.15f
    val bambooW = headR * 0.30f
    // 竹子向上伸出 (顶部)，越啃越短
    val bambooTopY = mouthY - headR * 0.85f * (1f - chewPhase * 0.20f)
    val bambooBottomY = mouthY + headR * 0.95f
    val bambooH = bambooBottomY - bambooTopY
    
    // 竹子主干 - 浅绿
    drawRoundRect(
        color = Color(0xFF6FB349),
        topLeft = Offset(bambooX - bambooW / 2, bambooTopY),
        size = Size(bambooW, bambooH),
        cornerRadius = CornerRadius(bambooW / 3)
    )
    // 竹子高光
    drawRoundRect(
        color = Color(0xFFA0E27D).copy(alpha = 0.6f),
        topLeft = Offset(bambooX - bambooW / 4, bambooTopY + 4f),
        size = Size(bambooW / 5, bambooH - 8f),
        cornerRadius = CornerRadius(bambooW / 8)
    )
    // 竹节横线
    val nodeCount = 3
    for (i in 1 until nodeCount) {
        val ny = bambooTopY + bambooH * (i.toFloat() / nodeCount)
        drawLine(
            color = Color(0xFF4F8A35),
            start = Offset(bambooX - bambooW / 2, ny),
            end = Offset(bambooX + bambooW / 2, ny),
            strokeWidth = 3f
        )
    }
    // 顶部竹叶 (两片)
    drawOval(
        color = Color(0xFF8BCB6F),
        topLeft = Offset(bambooX - bambooW * 1.4f, bambooTopY - bambooW * 0.2f),
        size = Size(bambooW * 1.4f, bambooW * 0.7f)
    )
    drawOval(
        color = Color(0xFF8BCB6F),
        topLeft = Offset(bambooX + bambooW * 0.0f, bambooTopY - bambooW * 0.2f),
        size = Size(bambooW * 1.4f, bambooW * 0.7f)
    )
    // 叶子高光
    drawOval(
        color = Color(0xFFB5E394).copy(alpha = 0.7f),
        topLeft = Offset(bambooX - bambooW * 1.2f, bambooTopY - bambooW * 0.1f),
        size = Size(bambooW * 0.5f, bambooW * 0.25f)
    )
    
    // 黑色爪子抓握竹子 (放在嘴下方位置)
    val pawCenter = Offset(bambooX + bambooW * 0.05f, mouthY + headR * 0.55f)
    val pawR = headR * 0.18f
    // 爪掌
    drawCircle(color = patchBlack, radius = pawR, center = pawCenter)
    // 爪掌内侧粉色
    drawCircle(
        color = Color(0xFFFFB1C8),
        radius = pawR * 0.55f,
        center = Offset(pawCenter.x - pawR * 0.05f, pawCenter.y - pawR * 0.05f)
    )
    // 三个小肉垫
    for (i in 0..2) {
        val ang = (-110f + i * 35f) * (PI / 180f).toFloat()
        drawCircle(
            color = Color(0xFFFFB1C8),
            radius = pawR * 0.18f,
            center = Offset(
                pawCenter.x + cos(ang) * pawR * 0.55f,
                pawCenter.y + sin(ang) * pawR * 0.55f
            )
        )
    }
}

// ════════════════════════════════════════════════════════════
// 🎯 头部绘制 (单独抽出便于旋转)
// ════════════════════════════════════════════════════════════
private fun DrawScope.drawPandaHead(
    headCenter: Offset,
    headR: Float,
    bodyWhite: Color,
    patchBlack: Color,
    noseColor: Color,
    cheekPink: Color,
    mouthColor: Color,
    blink: Boolean,
    isHappy: Boolean,
    isEating: Boolean,
    chewPhase: Float,
    earOffset: Float,
    idleVariant: PandaIdleVariant,
    yawnPhase: Float,
    isInCleaning: Boolean
) {
    // 耳朵 (黑色圆 + 粉色内侧)
    val earR = headR * 0.32f
    drawCircle(
        color = patchBlack,
        radius = earR,
        center = Offset(headCenter.x - headR * 0.75f + earOffset, headCenter.y - headR * 0.75f)
    )
    drawCircle(
        color = patchBlack,
        radius = earR,
        center = Offset(headCenter.x + headR * 0.75f - earOffset, headCenter.y - headR * 0.75f)
    )
    drawCircle(
        color = cheekPink.copy(alpha = 0.85f),
        radius = earR * 0.5f,
        center = Offset(headCenter.x - headR * 0.75f + earOffset, headCenter.y - headR * 0.70f)
    )
    drawCircle(
        color = cheekPink.copy(alpha = 0.85f),
        radius = earR * 0.5f,
        center = Offset(headCenter.x + headR * 0.75f - earOffset, headCenter.y - headR * 0.70f)
    )

    // 头部白色
    drawCircle(color = bodyWhite, radius = headR, center = headCenter)
    drawCircle(
        color = patchBlack.copy(alpha = 0.15f),
        radius = headR,
        center = headCenter,
        style = Stroke(width = 3f)
    )

    // 洗澡时头顶大泡沫
    if (isInCleaning) {
        for (i in 0 until 5) {
            val ang = (i * 60f - 90f) * (PI / 180f).toFloat()
            val r = headR * 0.85f
            val cxFoam = headCenter.x + cos(ang) * r * 0.6f
            val cyFoam = headCenter.y - headR * 0.85f + sin(ang) * r * 0.2f
            drawCircle(
                color = Color.White,
                radius = headR * (0.18f + (i % 3) * 0.04f),
                center = Offset(cxFoam, cyFoam)
            )
        }
    }

    // 黑色眼斑
    val patchW = headR * 0.45f
    val patchH = headR * 0.55f
    val leftPatchCenter = Offset(headCenter.x - headR * 0.35f, headCenter.y + headR * 0.05f)
    val rightPatchCenter = Offset(headCenter.x + headR * 0.35f, headCenter.y + headR * 0.05f)
    rotate(degrees = -18f, pivot = leftPatchCenter) {
        drawOval(
            color = patchBlack,
            topLeft = Offset(leftPatchCenter.x - patchW / 2, leftPatchCenter.y - patchH / 2),
            size = Size(patchW, patchH)
        )
    }
    rotate(degrees = 18f, pivot = rightPatchCenter) {
        drawOval(
            color = patchBlack,
            topLeft = Offset(rightPatchCenter.x - patchW / 2, rightPatchCenter.y - patchH / 2),
            size = Size(patchW, patchH)
        )
    }

    // 眼睛
    val eyeR = headR * 0.10f
    val leftEyeCenter = Offset(headCenter.x - headR * 0.30f, headCenter.y)
    val rightEyeCenter = Offset(headCenter.x + headR * 0.30f, headCenter.y)
    if (blink) {
        // 眯眼笑弧
        drawArc(
            color = Color.White,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(leftEyeCenter.x - eyeR, leftEyeCenter.y - eyeR),
            size = Size(eyeR * 2, eyeR * 2),
            style = Stroke(width = 4f)
        )
        drawArc(
            color = Color.White,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(rightEyeCenter.x - eyeR, rightEyeCenter.y - eyeR),
            size = Size(eyeR * 2, eyeR * 2),
            style = Stroke(width = 4f)
        )
    } else {
        drawCircle(color = Color.White, radius = eyeR, center = leftEyeCenter)
        drawCircle(color = Color.White, radius = eyeR, center = rightEyeCenter)
        drawCircle(color = patchBlack, radius = eyeR * 0.55f, center = leftEyeCenter)
        drawCircle(color = patchBlack, radius = eyeR * 0.55f, center = rightEyeCenter)
        drawCircle(
            color = Color.White,
            radius = eyeR * 0.22f,
            center = Offset(leftEyeCenter.x + eyeR * 0.20f, leftEyeCenter.y - eyeR * 0.20f)
        )
        drawCircle(
            color = Color.White,
            radius = eyeR * 0.22f,
            center = Offset(rightEyeCenter.x + eyeR * 0.20f, rightEyeCenter.y - eyeR * 0.20f)
        )
    }

    // 鼻子
    val noseW = headR * 0.18f
    val noseH = headR * 0.13f
    val noseCenter = Offset(headCenter.x, headCenter.y + headR * 0.30f)
    drawOval(
        color = noseColor,
        topLeft = Offset(noseCenter.x - noseW / 2, noseCenter.y - noseH / 2),
        size = Size(noseW, noseH)
    )

    // 嘴巴 (根据状态)
    val mouthY = noseCenter.y + headR * 0.15f
    val mouthCenter = Offset(headCenter.x, mouthY)
    when {
        idleVariant == PandaIdleVariant.YAWNING -> {
            // 打哈欠 - 大圆嘴
            val mw = headR * (0.30f + 0.30f * yawnPhase)
            val mh = headR * (0.20f + 0.30f * yawnPhase)
            drawOval(
                color = mouthColor,
                topLeft = Offset(mouthCenter.x - mw / 2, mouthCenter.y - mh / 2),
                size = Size(mw, mh)
            )
            // 粉舌头
            drawOval(
                color = Color(0xFFFF8FA8),
                topLeft = Offset(mouthCenter.x - mw / 4, mouthCenter.y),
                size = Size(mw / 2, mh / 2)
            )
        }
        isEating -> {
            val mw = headR * (0.20f + 0.15f * chewPhase)
            val mh = headR * (0.10f + 0.15f * chewPhase)
            drawOval(
                color = mouthColor,
                topLeft = Offset(mouthCenter.x - mw / 2, mouthCenter.y - mh / 2),
                size = Size(mw, mh)
            )
            drawOval(
                color = Color(0xFFFF8FA8),
                topLeft = Offset(mouthCenter.x - mw / 4, mouthCenter.y),
                size = Size(mw / 2, mh / 2)
            )
        }
        isHappy -> {
            drawArc(
                color = mouthColor,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(headCenter.x - headR * 0.18f, mouthY - headR * 0.05f),
                size = Size(headR * 0.36f, headR * 0.20f),
                style = Stroke(width = 5f)
            )
        }
        else -> {
            // 默认三瓣嘴
            drawLine(
                color = mouthColor,
                start = Offset(noseCenter.x, noseCenter.y + noseH / 2),
                end = Offset(noseCenter.x, mouthY - headR * 0.04f),
                strokeWidth = 4f
            )
            drawArc(
                color = mouthColor,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(mouthCenter.x - headR * 0.16f, mouthY - headR * 0.04f),
                size = Size(headR * 0.16f, headR * 0.10f),
                style = Stroke(width = 4f)
            )
            drawArc(
                color = mouthColor,
                startAngle = 90f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(mouthCenter.x, mouthY - headR * 0.04f),
                size = Size(headR * 0.16f, headR * 0.10f),
                style = Stroke(width = 4f)
            )
        }
    }

    // 腮红 (开心或抚摸)
    if (isHappy) {
        drawCircle(
            color = cheekPink.copy(alpha = 0.75f),
            radius = headR * 0.10f,
            center = Offset(headCenter.x - headR * 0.55f, headCenter.y + headR * 0.20f)
        )
        drawCircle(
            color = cheekPink.copy(alpha = 0.75f),
            radius = headR * 0.10f,
            center = Offset(headCenter.x + headR * 0.55f, headCenter.y + headR * 0.20f)
        )
    }

    // 趴卧/伸懒腰：嘴边小 ZZZ 飘字 (慵懒感)
    if (idleVariant == PandaIdleVariant.LYING) {
        // 不画 ZZZ (Canvas 画文字麻烦),用三个递增小圆代表呼吸感
        for (i in 0..2) {
            drawCircle(
                color = Color.Gray.copy(alpha = 0.4f - i * 0.1f),
                radius = 4f + i * 2f,
                center = Offset(headCenter.x + headR * 1.0f + i * 14f, headCenter.y - headR * 0.5f - i * 12f)
            )
        }
    }
}

// 短粗竹子段（喂食时手里拿的）
private fun DrawScope.drawBambooStub(topLeft: Offset, w: Float, h: Float, chewPhase: Float) {
    // 竹子被咬时变短
    val actualH = h * (1f - chewPhase * 0.15f)
    drawRoundRect(
        color = Color(0xFF6FB349),
        topLeft = topLeft,
        size = Size(w, actualH),
        cornerRadius = CornerRadius(w / 3)
    )
    // 节
    drawLine(
        color = Color(0xFF4F8A35),
        start = Offset(topLeft.x, topLeft.y + actualH * 0.5f),
        end = Offset(topLeft.x + w, topLeft.y + actualH * 0.5f),
        strokeWidth = 3f
    )
    // 叶子
    drawOval(
        color = Color(0xFF8BCB6F),
        topLeft = Offset(topLeft.x - w * 0.6f, topLeft.y - 4f),
        size = Size(w * 1.2f, h * 0.30f)
    )
}

// ════════════════════════════════════════════════════════════
// 🛁 木浴桶
// ════════════════════════════════════════════════════════════
@Composable
private fun WoodenTub(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val tubColor = Color(0xFF8B5A3C)
        val tubLight = Color(0xFFA67950)
        // 桶身（梯形上宽下窄）
        val path = Path().apply {
            moveTo(w * 0.05f, h * 0.20f)
            lineTo(w * 0.95f, h * 0.20f)
            lineTo(w * 0.85f, h)
            lineTo(w * 0.15f, h)
            close()
        }
        drawPath(path, tubColor)
        // 木板分割条纹
        for (i in 1..4) {
            val t = i / 5f
            val xTop = w * (0.05f + (0.95f - 0.05f) * t)
            val xBot = w * (0.15f + (0.85f - 0.15f) * t)
            drawLine(
                color = tubLight.copy(alpha = 0.6f),
                start = Offset(xTop, h * 0.20f),
                end = Offset(xBot, h),
                strokeWidth = 2f
            )
        }
        // 桶边
        drawRoundRect(
            color = tubLight,
            topLeft = Offset(w * 0.02f, h * 0.15f),
            size = Size(w * 0.96f, h * 0.10f),
            cornerRadius = CornerRadius(8f)
        )
        // 水面 (蓝色椭圆 + 高光)
        drawOval(
            color = Color(0xFF81D4FA).copy(alpha = 0.85f),
            topLeft = Offset(w * 0.06f, h * 0.18f),
            size = Size(w * 0.88f, h * 0.18f)
        )
        // 水面波纹
        drawOval(
            color = Color.White.copy(alpha = 0.6f),
            topLeft = Offset(w * 0.20f, h * 0.21f),
            size = Size(w * 0.30f, h * 0.05f)
        )
    }
}

// ════════════════════════════════════════════════════════════
// 🚿 喷头水流
// ════════════════════════════════════════════════════════════
@Composable
private fun ShowerSpray(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "spray")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sprayPhase"
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // 喷头
        drawRoundRect(
            color = Color(0xFFB0BEC5),
            topLeft = Offset(w / 2 - 30f, 0f),
            size = Size(60f, 18f),
            cornerRadius = CornerRadius(6f)
        )
        drawRoundRect(
            color = Color(0xFF90A4AE),
            topLeft = Offset(w / 2 - 8f, -22f),
            size = Size(16f, 22f),
            cornerRadius = CornerRadius(2f)
        )
        // 水流线
        for (i in 0 until 8) {
            val seed = (i * 17 % 100) / 100f
            val t = (phase + seed) % 1f
            val x = w / 2 + (i - 4) * 15f
            val y = 22f + t * h * 0.55f
            drawLine(
                color = Color(0xFF4FC3F7).copy(alpha = (1f - t) * 0.85f),
                start = Offset(x, y),
                end = Offset(x, y + 12f),
                strokeWidth = 3f
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
// 🏀 弹跳玩耍球 (彩色条纹)
// ════════════════════════════════════════════════════════════
@Composable
private fun BouncingPlayBall(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "ball")
    val bounce by infinite.animateFloat(
        initialValue = -25f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ballBounce"
    )
    val rot by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ballRot"
    )
    Box(
        modifier = modifier.offset(y = bounce.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().rotate(rot)) {
            val r = size.minDimension / 2
            val center = Offset(size.width / 2, size.height / 2)
            // 球底
            drawCircle(color = Color(0xFFFFEB3B), radius = r, center = center)
            // 红色条纹
            val stripeW = r * 0.30f
            drawRoundRect(
                color = Color(0xFFE53935),
                topLeft = Offset(center.x - stripeW / 2, 4f),
                size = Size(stripeW, size.height - 8f),
                cornerRadius = CornerRadius(stripeW / 2)
            )
            // 蓝色横条
            drawRoundRect(
                color = Color(0xFF1E88E5),
                topLeft = Offset(4f, center.y - stripeW / 4),
                size = Size(size.width - 8f, stripeW / 2),
                cornerRadius = CornerRadius(stripeW / 4)
            )
            // 高光
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = r * 0.18f,
                center = Offset(center.x - r * 0.35f, center.y - r * 0.35f)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
// 🫧 洗澡气泡场
// ════════════════════════════════════════════════════════════
@Composable
private fun BubbleField() {
    val infinite = rememberInfiniteTransition(label = "bubbles")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubblePhase"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        for (i in 0 until 12) {
            val baseX = (i * 37 % 100) / 100f * w
            val seed = (i * 13) % 100 / 100f
            val y = h - (phase + seed) % 1f * h * 0.9f
            val r = 8f + (i % 4) * 4f
            drawCircle(
                color = Color(0xFF4FC3F7).copy(alpha = 0.55f),
                radius = r,
                center = Offset(baseX, y)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = r * 0.3f,
                center = Offset(baseX - r * 0.3f, y - r * 0.3f)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
// 🍴 喂食粒子 (分号 + 星星)
// ════════════════════════════════════════════════════════════
@Composable
private fun FoodParticles() {
    val infinite = rememberInfiniteTransition(label = "fp")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fpPhase"
    )
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        for (i in 0 until 4) {
            val seed = i * 0.25f
            val t = (phase + seed) % 1f
            val x = (sin((t * 3 + i) * PI).toFloat() * 30f) + (i - 1.5f) * 30f
            val y = -t * 100f - 20f
            Text(
                text = if (i % 2 == 0) "✨" else "💛",
                fontSize = 16.sp,
                modifier = Modifier.offset(x = x.dp, y = y.dp)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
// 💕 抚摸飘心
// ════════════════════════════════════════════════════════════
@Composable
private fun FloatingPandaHearts() {
    val infinite = rememberInfiniteTransition(label = "hearts")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "heartPhase"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        for (i in 0 until 4) {
            val seed = i * 0.25f
            val t = (phase + seed) % 1f
            val cx = w / 2 + (sin((t * 4 + i) * PI).toFloat() * 30f) + (i - 1.5f) * 40f
            val cy = h * 0.55f - t * h * 0.5f
            val alpha = 1f - t
            drawHeart(Offset(cx, cy), 14f + i * 2f, Color(0xFFFF6B9D).copy(alpha = alpha))
        }
    }
}

private fun DrawScope.drawHeart(center: Offset, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y + size * 0.6f)
        cubicTo(
            center.x + size, center.y + size * 0.2f,
            center.x + size, center.y - size * 0.6f,
            center.x, center.y - size * 0.1f
        )
        cubicTo(
            center.x - size, center.y - size * 0.6f,
            center.x - size, center.y + size * 0.2f,
            center.x, center.y + size * 0.6f
        )
        close()
    }
    drawPath(path, color)
}

// ════════════════════════════════════════════════════════════
// ⭐ 升级飘星
// ════════════════════════════════════════════════════════════
@Composable
private fun LevelUpStars() {
    val infinite = rememberInfiniteTransition(label = "stars")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starPhase"
    )
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        for (i in 0 until 6) {
            val angle = (i * 60f + phase * 360f) * (PI / 180f).toFloat()
            val r = 110f
            Text(
                text = "⭐",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(
                    x = (cos(angle) * r).dp,
                    y = (sin(angle) * r).dp
                )
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
// 🐼 迷你熊猫图标 (用于领养界面，避免 emoji 不支持)
// ════════════════════════════════════════════════════════════
@Composable
fun MiniPandaIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h * 0.55f
        val headR = w * 0.42f
        val bodyWhite = Color(0xFFFFFAF5)
        val patchBlack = Color(0xFF2B2B2B)
        // 耳朵
        drawCircle(patchBlack, headR * 0.32f, Offset(cx - headR * 0.75f, cy - headR * 0.75f))
        drawCircle(patchBlack, headR * 0.32f, Offset(cx + headR * 0.75f, cy - headR * 0.75f))
        // 头
        drawCircle(bodyWhite, headR, Offset(cx, cy))
        // 眼斑
        rotate(-18f, Offset(cx - headR * 0.35f, cy + headR * 0.05f)) {
            drawOval(
                patchBlack,
                Offset(cx - headR * 0.35f - headR * 0.22f, cy + headR * 0.05f - headR * 0.27f),
                Size(headR * 0.44f, headR * 0.55f)
            )
        }
        rotate(18f, Offset(cx + headR * 0.35f, cy + headR * 0.05f)) {
            drawOval(
                patchBlack,
                Offset(cx + headR * 0.35f - headR * 0.22f, cy + headR * 0.05f - headR * 0.27f),
                Size(headR * 0.44f, headR * 0.55f)
            )
        }
        // 眼睛
        drawCircle(Color.White, headR * 0.10f, Offset(cx - headR * 0.30f, cy))
        drawCircle(Color.White, headR * 0.10f, Offset(cx + headR * 0.30f, cy))
        drawCircle(patchBlack, headR * 0.06f, Offset(cx - headR * 0.30f, cy))
        drawCircle(patchBlack, headR * 0.06f, Offset(cx + headR * 0.30f, cy))
        // 鼻子
        drawOval(
            patchBlack,
            Offset(cx - headR * 0.09f, cy + headR * 0.25f),
            Size(headR * 0.18f, headR * 0.13f)
        )
        // 嘴
        drawArc(
            patchBlack,
            startAngle = 0f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(cx - headR * 0.12f, cy + headR * 0.40f),
            size = Size(headR * 0.24f, headR * 0.12f),
            style = Stroke(width = 3f)
        )
    }
}

// ════════════════════════════════════════════════════════════
// 🦆 小黄鸭 (浮在水面，上下轻晃)
// ════════════════════════════════════════════════════════════
@Composable
private fun RubberDuck(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "duck")
    val bob by infinite.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "duckBob"
    )
    val rot by infinite.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "duckRot"
    )
    Box(modifier = modifier.offset(y = bob.dp).rotate(rot)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val yellow = Color(0xFFFFD93D)
            val yellowDark = Color(0xFFE8B713)
            val orange = Color(0xFFFF9F1C)
            
            // 身体 (椭圆)
            drawOval(
                color = yellow,
                topLeft = Offset(w * 0.05f, h * 0.40f),
                size = Size(w * 0.85f, h * 0.55f)
            )
            // 身体底部阴影
            drawOval(
                color = yellowDark.copy(alpha = 0.4f),
                topLeft = Offset(w * 0.10f, h * 0.65f),
                size = Size(w * 0.75f, h * 0.30f)
            )
            // 头部 (圆)
            val headR = h * 0.30f
            val headCenter = Offset(w * 0.30f, h * 0.30f)
            drawCircle(yellow, headR, headCenter)
            // 嘴 (橙色三角形)
            val beakPath = Path().apply {
                moveTo(headCenter.x - headR * 1.05f, headCenter.y)
                lineTo(headCenter.x - headR * 0.30f, headCenter.y - headR * 0.20f)
                lineTo(headCenter.x - headR * 0.30f, headCenter.y + headR * 0.30f)
                close()
            }
            drawPath(beakPath, orange)
            // 眼睛
            drawCircle(Color.Black, headR * 0.15f, Offset(headCenter.x - headR * 0.10f, headCenter.y - headR * 0.20f))
            drawCircle(Color.White, headR * 0.05f, Offset(headCenter.x - headR * 0.05f, headCenter.y - headR * 0.25f))
            // 翅膀
            drawOval(
                color = yellowDark,
                topLeft = Offset(w * 0.40f, h * 0.50f),
                size = Size(w * 0.35f, h * 0.30f)
            )
            // 水面波纹
            drawArc(
                color = Color.White.copy(alpha = 0.7f),
                startAngle = 0f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(-w * 0.05f, h * 0.85f),
                size = Size(w * 1.1f, h * 0.20f),
                style = Stroke(width = 2f)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
// 🧼 香皂 (带泡沫高光)
// ════════════════════════════════════════════════════════════
@Composable
private fun SoapBar(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "soap")
    val bob by infinite.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "soapBob"
    )
    Box(modifier = modifier.offset(y = bob.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // 香皂主体 (粉色圆角矩形)
            drawRoundRect(
                color = Color(0xFFFFAEC9),
                topLeft = Offset(0f, 0f),
                size = Size(w, h),
                cornerRadius = CornerRadius(h * 0.4f)
            )
            // 香皂高光
            drawRoundRect(
                color = Color(0xFFFFD3E2),
                topLeft = Offset(w * 0.10f, h * 0.10f),
                size = Size(w * 0.80f, h * 0.30f),
                cornerRadius = CornerRadius(h * 0.2f)
            )
            // 顶部小泡沫团
            drawCircle(Color.White, h * 0.30f, Offset(w * 0.25f, -h * 0.10f))
            drawCircle(Color.White, h * 0.22f, Offset(w * 0.55f, -h * 0.20f))
            drawCircle(Color.White, h * 0.18f, Offset(w * 0.80f, -h * 0.05f))
        }
    }
}

// ════════════════════════════════════════════════════════════
// 💦 水花溅射 (从桶边缘向上喷)
// ════════════════════════════════════════════════════════════
@Composable
private fun WaterSplashes() {
    val infinite = rememberInfiniteTransition(label = "splash")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "splashPhase"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val centerX = w / 2
        val baseY = h * 0.70f  // 大概在桶水面位置
        // 6 个水滴从桶里向外溅
        for (i in 0 until 6) {
            val seed = i * 0.18f
            val t = (phase + seed) % 1f
            val angleDeg = -110f + i * 35f  // -110~+65
            val rad = (angleDeg) * (PI / 180f).toFloat()
            val dist = t * 70f
            val px = centerX + cos(rad) * dist
            // 抛物线下落
            val py = baseY + sin(rad) * dist + t * t * 35f
            val alpha = (1f - t).coerceAtLeast(0f)
            drawCircle(
                color = Color(0xFF4FC3F7).copy(alpha = alpha * 0.85f),
                radius = 6f - t * 3f,
                center = Offset(px, py)
            )
            // 水滴拖尾
            drawCircle(
                color = Color(0xFF81D4FA).copy(alpha = alpha * 0.5f),
                radius = 3f,
                center = Offset(px - cos(rad) * 8f, py - sin(rad) * 8f)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
// ♨️ 蒸汽波纹 (热水蒸汽从浴桶上方升起)
// ════════════════════════════════════════════════════════════
@Composable
private fun SteamWaves(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "steam")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "steamPhase"
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // 3 团蒸汽，错开位置
        for (i in 0 until 6) {
            val seed = i * 0.16f
            val t = (phase + seed) % 1f
            val baseX = (i % 3) * w / 3f + w / 6f + (i / 3) * 30f
            val y = h - t * h * 1.1f
            val alpha = (1f - t) * 0.55f
            val r = 16f + t * 14f
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = r,
                center = Offset(baseX, y)
            )
            // 内层更白的小团
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.7f),
                radius = r * 0.5f,
                center = Offset(baseX + 4f, y - 3f)
            )
        }
    }
}
