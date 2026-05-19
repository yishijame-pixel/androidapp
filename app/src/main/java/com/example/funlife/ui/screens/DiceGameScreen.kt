package com.example.funlife.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import com.example.funlife.ui.components.gl.DiceCubeGLView
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.data.model.*
import com.example.funlife.viewmodel.DiceGameViewModel
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// 主题色（保留原粉色用于弹窗）
private val PinkLight = Color(0xFFFFE0EC)
private val PinkSoft = Color(0xFFFFCAD4)
private val PinkMid = Color(0xFFFF80AB)
private val PinkDeep = Color(0xFFEC407A)
private val PinkAccent = Color(0xFFD81B60)
private val GoldStar = Color(0xFFFFC107)
private val CreamBg = Color(0xFFFFF5F8)

// 🔥 新版"欢乐摇骰子"暗色主题
private val NightBg1 = Color(0xFF0E1726)
private val NightBg2 = Color(0xFF13202F)
private val NightBg3 = Color(0xFF1A2A3F)
private val DeepBlue1 = Color(0xFF2A4E78)
private val DeepBlue2 = Color(0xFF1F3A5C)
private val DeepBlue3 = Color(0xFF152A44)
private val SteelBlue = Color(0xFF3B6EA8)
private val PlateRim = Color(0xFF14253A)
private val ShakeRed1 = Color(0xFFFF5A3C)
private val ShakeRed2 = Color(0xFFE0341A)
private val ShakeRed3 = Color(0xFFB22315)
private val IconYellow1 = Color(0xFFFFD86A)
private val IconYellow2 = Color(0xFFFFB23A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiceGameScreen(
    viewModel: DiceGameViewModel = remember { DiceGameViewModel() },
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val diceCount by viewModel.diceCount.collectAsState()
    val diceValues by viewModel.diceValues.collectAsState()
    val isCupCovered by viewModel.isCupCovered.collectAsState()
    val isShaking by viewModel.isShaking.collectAsState()
    val isRevealed by viewModel.isRevealed.collectAsState()
    val players by viewModel.players.collectAsState()
    val currentPlayerIndex by viewModel.currentPlayerIndex.collectAsState()
    val gameMode by viewModel.gameMode.collectAsState()
    val penaltyNumber by viewModel.penaltyNumber.collectAsState()
    val truthDareCard by viewModel.truthDareCard.collectAsState()
    val showRoundResult by viewModel.showRoundResult.collectAsState()
    val gameStage by viewModel.gameStage.collectAsState()
    
    // 吹牛骰盅状态
    val liarPhase by viewModel.liarPhase.collectAsState()
    val liarBidCount by viewModel.liarBidCount.collectAsState()
    val liarBidFace by viewModel.liarBidFace.collectAsState()
    val liarBidder by viewModel.liarBidder.collectAsState()
    val liarChallengeResult by viewModel.liarChallengeResult.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var showAddPlayer by remember { mutableStateOf(false) }
    var sensorShakeEnabled by remember { mutableStateOf(true) }
    
    // 🎵 音效
    val soundManager = remember { com.example.funlife.utils.SoundEffectManager.getInstance(context) }

    // 🔥 进入页面或骰子数量改变时，自动让骰子落进骰盘
    LaunchedEffect(diceCount) {
        if (gameStage == com.example.funlife.data.model.DiceGameStage.IDLE) {
            viewModel.setStage(com.example.funlife.data.model.DiceGameStage.DROPPING_DICE)
        }
    }

    // 🎬 流程动画驱动 — 根据 gameStage 自动推进
    LaunchedEffect(gameStage) {
        when (gameStage) {
            com.example.funlife.data.model.DiceGameStage.DROPPING_DICE -> {
                // 1.2 秒骰子飞入骰盘动画 + 音效
                soundManager.play(com.example.funlife.utils.SoundEffect.DICE_DROP, volume = 0.7f)
                delay(1200)
                // 🔥 骰子落定后停在 REVEALED（骰子可见，等用户滑动杯子下来盖住）
                viewModel.setStage(com.example.funlife.data.model.DiceGameStage.REVEALED)
            }
            com.example.funlife.data.model.DiceGameStage.COVERING -> {
                // 兼容：若仍有外部触发 COVERING，0.4 秒后自动进入 COVERED
                delay(400)
                viewModel.setStage(com.example.funlife.data.model.DiceGameStage.COVERED)
            }
            com.example.funlife.data.model.DiceGameStage.SHAKING -> {
                // 摇骰循环音效 + 数字抖动
                soundManager.play(com.example.funlife.utils.SoundEffect.DICE_SHAKE, volume = 0.6f, loop = true)
                var elapsed = 0
                while (gameStage == com.example.funlife.data.model.DiceGameStage.SHAKING && elapsed < 1800) {
                    viewModel.jitterDiceValues()
                    delay(80)
                    elapsed += 80
                }
                soundManager.stop(com.example.funlife.utils.SoundEffect.DICE_SHAKE)
                soundManager.play(com.example.funlife.utils.SoundEffect.DICE_DROP, volume = 0.7f)
                viewModel.stopShakeAndRoll()
                viewModel.setStage(com.example.funlife.data.model.DiceGameStage.SHAKEN)
            }
            com.example.funlife.data.model.DiceGameStage.REVEALING -> {
                // 0.55 秒揭杯升起动画 + 音效
                soundManager.play(com.example.funlife.utils.SoundEffect.DICE_REVEAL, volume = 0.8f)
                delay(550)
                viewModel.revealCup()  // 记录积分等
                viewModel.setStage(com.example.funlife.data.model.DiceGameStage.REVEALED)
            }
            else -> {}
        }
    }
    
    // 回合结算音效
    LaunchedEffect(showRoundResult) {
        if (showRoundResult) {
            delay(300)
            soundManager.play(com.example.funlife.utils.SoundEffect.DICE_WIN, volume = 0.9f)
        }
    }

    // 重力感应
    DisposableEffect(sensorShakeEnabled) {
        if (!sensorShakeEnabled) return@DisposableEffect onDispose { }
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var lastShakeTime = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
                val g = sqrt((x*x + y*y + z*z).toDouble()) / SensorManager.GRAVITY_EARTH
                if (g > 1.8) {  // 摇晃阈值
                    val now = System.currentTimeMillis()
                    val stage = viewModel.gameStage.value
                    val canShake = stage == com.example.funlife.data.model.DiceGameStage.COVERED ||
                            stage == com.example.funlife.data.model.DiceGameStage.SHAKEN
                    if (now - lastShakeTime > 1500 && canShake) {
                        lastShakeTime = now
                        viewModel.setStage(com.example.funlife.data.model.DiceGameStage.SHAKING)
                    }
                }
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (accel != null) {
            sensorManager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    var showMorePlay by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(NightBg1, NightBg2, NightBg3, NightBg1)
                )
            )
    ) {
        // 暗色星点背景装饰
        BackgroundDecor()

        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部栏：返回 + 标题
            DiceTopBar(
                onBack = onNavigateBack,
                onSettings = { showSettings = true },
                sensorEnabled = sensorShakeEnabled,
                onToggleSensor = { sensorShakeEnabled = !sensorShakeEnabled }
            )

            // 4 个胶囊圆形入口
            TopActionIcons(
                onMorePlay = { showMorePlay = true },
                onSkin = {
                    viewModel.let { /* 占位 */ }
                    android.widget.Toast.makeText(context, "皮肤系统开发中～", android.widget.Toast.LENGTH_SHORT).show()
                },
                onDaily = {
                    android.widget.Toast.makeText(context, "每日福利稍后开放～", android.widget.Toast.LENGTH_SHORT).show()
                },
                onShare = {
                    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, "来一起玩欢乐摇骰子吧！🎲")
                    }
                    context.startActivity(android.content.Intent.createChooser(sendIntent, "分享好友"))
                }
            )

            // 主舞台 - 蓝色骰盅 + 蓝色骰盘 + 骰子
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                DiceStage(
                    diceCount = diceCount,
                    diceValues = diceValues,
                    stage = gameStage,
                    currentPlayer = players.getOrNull(currentPlayerIndex),
                    onCoverByDrag = {
                        // 🔥 用户把骰盅拖到底 → 进入 COVERED
                        if (gameStage == com.example.funlife.data.model.DiceGameStage.REVEALED ||
                            gameStage == com.example.funlife.data.model.DiceGameStage.IDLE) {
                            viewModel.setStage(com.example.funlife.data.model.DiceGameStage.COVERED)
                        }
                    }
                )
            }

            // 底部操作栏：设置(左) + 红色摇按钮(中) + 小骰盅(右)
            BottomActionBar(
                stage = gameStage,
                onSettings = { showSettings = true },
                onShakeAction = {
                    when (gameStage) {
                        com.example.funlife.data.model.DiceGameStage.COVERED -> {
                            // 已盖住 → 摇动
                            viewModel.setStage(com.example.funlife.data.model.DiceGameStage.SHAKING)
                        }
                        com.example.funlife.data.model.DiceGameStage.SHAKEN -> {
                            // 已摇完 → 揭盅
                            viewModel.setStage(com.example.funlife.data.model.DiceGameStage.REVEALING)
                        }
                        com.example.funlife.data.model.DiceGameStage.REVEALED -> {
                            // 揭盅后 → 重新投掷
                            viewModel.setStage(com.example.funlife.data.model.DiceGameStage.DROPPING_DICE)
                        }
                        com.example.funlife.data.model.DiceGameStage.IDLE -> {
                            // 还没投掷 → 投掷
                            viewModel.setStage(com.example.funlife.data.model.DiceGameStage.DROPPING_DICE)
                        }
                        else -> {}
                    }
                },
                onCupAction = { showMorePlay = true }
            )

            // 当前玩家结果 + 模式提示（紧凑显示在底部上方）
            AnimatedVisibility(
                visible = isRevealed,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut()
            ) {
                CurrentResultPanel(
                    player = players.getOrNull(currentPlayerIndex),
                    diceValues = diceValues,
                    mode = gameMode,
                    penaltyNumber = penaltyNumber,
                    truthDareCard = truthDareCard,
                    onDrawCard = { viewModel.drawTruthOrDare() },
                    onDrawTruth = { viewModel.drawTruthOrDare(CardType.TRUTH) },
                    onDrawDare = { viewModel.drawTruthOrDare(CardType.DARE) }
                )
            }
            // 吹牛骰盅 - 叫数面板
            if (gameMode == DiceGameMode.LIAR_DICE && liarPhase == LiarPhase.BIDDING) {
                LiarBiddingPanel(
                    currentPlayer = players.getOrNull(currentPlayerIndex),
                    bidCount = liarBidCount,
                    bidFace = liarBidFace,
                    onRaise = { c, f -> viewModel.liarRaiseBid(c, f) },
                    onChallenge = { viewModel.liarChallenge() }
                )
            }
            // 吹牛骰盅 - 开盅结果
            if (gameMode == DiceGameMode.LIAR_DICE && liarPhase == LiarPhase.SHOWDOWN && liarChallengeResult != null) {
                LiarShowdownPanel(
                    outcome = liarChallengeResult!!,
                    players = players,
                    onNextRound = { viewModel.liarStartNextRound() }
                )
            }
        }
    }

    // "更多玩法"底部面板：包含模式选择、惩罚数字、骰子数量、玩家
    if (showMorePlay) {
        MorePlaySheet(
            gameMode = gameMode,
            onSelectMode = { viewModel.setGameMode(it) },
            penaltyNumber = penaltyNumber,
            onSelectPenalty = { viewModel.setPenaltyNumber(it) },
            diceCount = diceCount,
            onChangeDiceCount = { viewModel.setDiceCount(it) },
            players = players,
            currentIndex = currentPlayerIndex,
            onAddPlayer = { showAddPlayer = true },
            onRemovePlayer = { viewModel.removePlayer(it) },
            onDismiss = { showMorePlay = false }
        )
    }

    // 设置弹窗
    if (showSettings) {
        SettingsDialog(
            sensorEnabled = sensorShakeEnabled,
            onToggleSensor = { sensorShakeEnabled = it },
            onReset = { viewModel.resetGame() },
            onDismiss = { showSettings = false }
        )
    }

    // 添加玩家弹窗
    if (showAddPlayer) {
        AddPlayerDialog(
            onConfirm = { name ->
                viewModel.addPlayer(name)
                showAddPlayer = false
            },
            onDismiss = { showAddPlayer = false }
        )
    }

    // 回合结算弹窗
    if (showRoundResult) {
        RoundResultDialog(
            players = players,
            mode = gameMode,
            winner = viewModel.computeRoundWinner(),
            loser = viewModel.computeRoundLoser(),
            truthDareCard = truthDareCard,
            onDismiss = { viewModel.dismissRoundResult() }
        )
    }
}

// ════════════════════════════════════════════════════════════
// 🔥 底部动作栏：设置(左) + 大红"摇"按钮(中) + 小骰盅(右)
// ════════════════════════════════════════════════════════════
@Composable
private fun BottomActionBar(
    stage: com.example.funlife.data.model.DiceGameStage,
    onSettings: () -> Unit,
    onShakeAction: () -> Unit,
    onCupAction: () -> Unit
) {
    val canTap = stage == com.example.funlife.data.model.DiceGameStage.IDLE ||
            stage == com.example.funlife.data.model.DiceGameStage.COVERED ||
            stage == com.example.funlife.data.model.DiceGameStage.SHAKEN ||
            stage == com.example.funlife.data.model.DiceGameStage.REVEALED
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 设置按钮
        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(6.dp, CircleShape, ambientColor = SteelBlue, spotColor = SteelBlue)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(NightBg3, DeepBlue3)))
                .border(1.5.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                .clickable { onSettings() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Settings, "设置",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(24.dp)
            )
        }

        // 红色"摇"按钮
        val infinite = rememberInfiniteTransition(label = "shakeBtn")
        val pulse by infinite.animateFloat(
            initialValue = 0.96f, targetValue = 1.04f,
            animationSpec = infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse),
            label = "p"
        )
        val scale = if (canTap) pulse else 1f
        Box(
            modifier = Modifier
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .size(78.dp)
                .shadow(12.dp, CircleShape, ambientColor = ShakeRed1, spotColor = ShakeRed2)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(ShakeRed1, ShakeRed2, ShakeRed3),
                        radius = 130f
                    )
                )
                .border(3.dp, Color.White.copy(alpha = 0.65f), CircleShape)
                .clickable(enabled = canTap) { onShakeAction() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "摇",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                style = TextStyle(shadow = Shadow(Color(0xFF7A1A0F), Offset(0f, 2f), 4f))
            )
        }

        // 小骰盅图标（打开"更多玩法"以快速调整骰子数量）
        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(6.dp, CircleShape, ambientColor = SteelBlue, spotColor = SteelBlue)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(NightBg3, DeepBlue3)))
                .border(1.5.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                .clickable { onCupAction() },
            contentAlignment = Alignment.Center
        ) {
            Text("\uD83C\uDFB2", fontSize = 22.sp)
        }
    }
}

// ════════════════════════════════════════════════════════════
// 🔥 "更多玩法"底部面板（包含原来散布在主界面的：模式/惩罚数字/骰子数量/玩家）
// ════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MorePlaySheet(
    gameMode: DiceGameMode,
    onSelectMode: (DiceGameMode) -> Unit,
    penaltyNumber: Int,
    onSelectPenalty: (Int) -> Unit,
    diceCount: Int,
    onChangeDiceCount: (Int) -> Unit,
    players: List<DicePlayer>,
    currentIndex: Int,
    onAddPlayer: () -> Unit,
    onRemovePlayer: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NightBg2,
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("\uD83C\uDFAE", fontSize = 22.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    "更多玩法",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
            // 模式选择
            ModeSelectorRow(selected = gameMode, onSelect = onSelectMode)
            // 数字定罚目标数字（仅在该模式下）
            AnimatedVisibility(visible = gameMode == DiceGameMode.NUMBER_PENALTY) {
                PenaltyNumberPicker(selected = penaltyNumber, onSelect = onSelectPenalty)
            }
            // 骰子数量
            DiceCountAdjuster(count = diceCount, onChange = onChangeDiceCount)
            // 玩家
            PlayersRow(
                players = players,
                currentIndex = currentIndex,
                onAdd = onAddPlayer,
                onRemove = onRemovePlayer
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
// 背景装饰
// ════════════════════════════════════════════════════════════
@Composable
private fun BackgroundDecor() {
    val infinite = rememberInfiniteTransition(label = "bgAnim")
    val float by infinite.animateFloat(
        initialValue = 0f, targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(4000, easing = EaseInOut), RepeatMode.Reverse),
        label = "f"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        // 暗夜星点
        val w = size.width; val h = size.height
        val stars = listOf(
            0.08f to 0.12f, 0.85f to 0.08f, 0.92f to 0.35f, 0.05f to 0.55f,
            0.88f to 0.78f, 0.12f to 0.88f, 0.50f to 0.05f, 0.20f to 0.20f,
            0.75f to 0.25f, 0.30f to 0.85f, 0.95f to 0.55f, 0.40f to 0.40f,
            0.62f to 0.32f, 0.18f to 0.68f, 0.83f to 0.60f, 0.45f to 0.75f
        )
        stars.forEach { (fx, fy) ->
            drawCircle(
                Color.White.copy(alpha = 0.18f),
                radius = 1.6f,
                center = Offset(w * fx, h * fy + float * 0.2f)
            )
        }
        // 几颗稍亮的"闪烁星"
        listOf(0.22f to 0.22f, 0.78f to 0.30f, 0.40f to 0.10f).forEach { (fx, fy) ->
            drawSparkleStar(Offset(w * fx, h * fy - float * 0.3f), 5f, Color.White.copy(alpha = 0.45f))
        }
    }
}

private fun DrawScope.drawDecorHeart(center: Offset, s: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y + s * 0.4f)
        cubicTo(
            center.x - s * 1.2f, center.y - s * 0.3f,
            center.x - s * 0.4f, center.y - s * 1.1f,
            center.x, center.y - s * 0.3f
        )
        cubicTo(
            center.x + s * 0.4f, center.y - s * 1.1f,
            center.x + s * 1.2f, center.y - s * 0.3f,
            center.x, center.y + s * 0.4f
        )
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawSparkleStar(center: Offset, s: Float, color: Color) {
    drawLine(color, Offset(center.x - s, center.y), Offset(center.x + s, center.y), 2.5f)
    drawLine(color, Offset(center.x, center.y - s), Offset(center.x, center.y + s), 2.5f)
    drawLine(color, Offset(center.x - s * 0.5f, center.y - s * 0.5f), Offset(center.x + s * 0.5f, center.y + s * 0.5f), 1.5f)
    drawLine(color, Offset(center.x + s * 0.5f, center.y - s * 0.5f), Offset(center.x - s * 0.5f, center.y + s * 0.5f), 1.5f)
}

// ════════════════════════════════════════════════════════════
// 顶部栏
// ════════════════════════════════════════════════════════════
@Composable
private fun DiceTopBar(
    onBack: () -> Unit,
    onSettings: () -> Unit,
    sensorEnabled: Boolean,
    onToggleSensor: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, "返回", tint = Color.White.copy(alpha = 0.85f))
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            "欢乐摇骰子",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            letterSpacing = 2.sp,
            style = TextStyle(shadow = Shadow(Color(0xFF000000).copy(alpha = 0.7f), Offset(0f, 2f), 6f))
        )
        Spacer(modifier = Modifier.weight(1f))
        // 重力感应开关
        IconButton(onClick = onToggleSensor) {
            Icon(
                if (sensorEnabled) Icons.Default.Vibration else Icons.Default.PhoneAndroid,
                "重力感应",
                tint = if (sensorEnabled) IconYellow1 else Color.White.copy(alpha = 0.45f)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
// 🔥 顶部 4 个胶囊圆形入口（更多玩法/换皮肤/每日福利/分享好友）
// ════════════════════════════════════════════════════════════
@Composable
private fun TopActionIcons(
    onMorePlay: () -> Unit,
    onSkin: () -> Unit,
    onDaily: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top
    ) {
        TopActionIcon("\uD83D\uDE03", "更多玩法", onMorePlay)
        TopActionIcon("\uD83C\uDFF7", "更换皮肤", onSkin)
        TopActionIcon("\uD83C\uDF81", "每日福利", onDaily)
        TopActionIcon("\u21AA", "分享好友", onShare)
    }
}

@Composable
private fun TopActionIcon(emoji: String, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .shadow(6.dp, CircleShape, ambientColor = IconYellow2, spotColor = IconYellow2)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(IconYellow1, IconYellow2)))
                .border(1.5.dp, Color.White.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 22.sp)
        }
        Text(
            label,
            fontSize = 11.sp,
            color = IconYellow1,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ════════════════════════════════════════════════════════════
// 模式选择条
// ════════════════════════════════════════════════════════════
@Composable
private fun ModeSelectorRow(
    selected: DiceGameMode,
    onSelect: (DiceGameMode) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(DiceGameMode.values()) { mode ->
            val isSel = mode == selected
            val bgBrush = if (isSel) {
                Brush.horizontalGradient(listOf(PinkMid, PinkDeep))
            } else {
                Brush.horizontalGradient(listOf(Color.White, PinkLight))
            }
            Column(
                modifier = Modifier
                    .shadow(if (isSel) 8.dp else 3.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgBrush)
                    .clickable { onSelect(mode) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(mode.emoji, fontSize = 22.sp)
                Text(
                    mode.displayName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSel) Color.White else PinkAccent
                )
                if (mode.isDrinking) {
                    Text("🍺", fontSize = 8.sp)
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// 数字定罚 - 数字选择
// ════════════════════════════════════════════════════════════
@Composable
private fun PenaltyNumberPicker(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("罚:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PinkAccent)
        (1..6).forEach { n ->
            val sel = n == selected
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .shadow(if (sel) 6.dp else 2.dp, CircleShape, ambientColor = PinkDeep)
                    .clip(CircleShape)
                    .background(
                        if (sel) Brush.verticalGradient(listOf(PinkMid, PinkDeep))
                        else Brush.verticalGradient(listOf(Color.White, PinkLight))
                    )
                    .clickable { onSelect(n) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    n.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = if (sel) Color.White else PinkAccent
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// 骰子数量调整器
// ════════════════════════════════════════════════════════════
@Composable
private fun DiceCountAdjuster(count: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("骰子", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PinkAccent)
        // -
        Box(
            modifier = Modifier
                .size(32.dp)
                .shadow(4.dp, CircleShape, ambientColor = PinkDeep)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(PinkMid, PinkDeep)))
                .clickable(enabled = count > 1) { onChange(count - 1) },
            contentAlignment = Alignment.Center
        ) {
            Text("−", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Black)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(2.dp, PinkMid, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 5.dp)
        ) {
            Text(
                "$count 颗",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = PinkAccent
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .shadow(4.dp, CircleShape, ambientColor = PinkDeep)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(PinkMid, PinkDeep)))
                .clickable(enabled = count < 10) { onChange(count + 1) },
            contentAlignment = Alignment.Center
        ) {
            Text("+", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.weight(1f))
        Text("最多10颗", fontSize = 10.sp, color = PinkAccent.copy(alpha = 0.7f))
    }
}

// ════════════════════════════════════════════════════════════
// 玩家滑动条
// ════════════════════════════════════════════════════════════
@Composable
private fun PlayersRow(
    players: List<DicePlayer>,
    currentIndex: Int,
    onAdd: () -> Unit,
    onRemove: (Long) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(players) { idx, player ->
            PlayerChip(
                player = player,
                isCurrent = idx == currentIndex,
                onRemove = { onRemove(player.id) }
            )
        }
        item {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Color.White, PinkLight)))
                    .border(2.dp, PinkMid, CircleShape)
                    .clickable { onAdd() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, "添加", tint = PinkDeep)
            }
        }
    }
}

@Composable
private fun PlayerChip(
    player: DicePlayer,
    isCurrent: Boolean,
    onRemove: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "playerPulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.95f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(700, easing = EaseInOut), RepeatMode.Reverse),
        label = "p"
    )
    val scaleVal = if (isCurrent) pulse else 1f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer { scaleX = scaleVal; scaleY = scaleVal }
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(if (isCurrent) 10.dp else 3.dp, CircleShape, ambientColor = player.color, spotColor = player.color)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(player.color.copy(alpha = 0.85f), player.color)))
                    .border(if (isCurrent) 3.dp else 1.dp, if (isCurrent) GoldStar else Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(player.emoji, fontSize = 28.sp)
            }
            if (!isCurrent) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, PinkMid, CircleShape)
                        .clickable { onRemove() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, "删除", tint = PinkDeep, modifier = Modifier.size(10.dp))
                }
            }
        }
        Text(
            player.name,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = PinkAccent,
            maxLines = 1
        )
        Text(
            "💎${player.totalScore} 🏆${player.wins}",
            fontSize = 9.sp,
            color = PinkAccent.copy(alpha = 0.7f)
        )
    }
}


// ════════════════════════════════════════════════════════════
// 主舞台 - 杯子盖住骰子或揭开
// ════════════════════════════════════════════════════════════
@Composable
private fun DiceStage(
    diceCount: Int,
    diceValues: List<Int>,
    stage: com.example.funlife.data.model.DiceGameStage,
    currentPlayer: DicePlayer?,
    onCoverByDrag: () -> Unit = {}
) {
    val cupCovered = stage == com.example.funlife.data.model.DiceGameStage.COVERING ||
            stage == com.example.funlife.data.model.DiceGameStage.COVERED ||
            stage == com.example.funlife.data.model.DiceGameStage.SHAKING ||
            stage == com.example.funlife.data.model.DiceGameStage.SHAKEN
    val isShaking = stage == com.example.funlife.data.model.DiceGameStage.SHAKING
    val isRevealed = stage == com.example.funlife.data.model.DiceGameStage.REVEALED
    val isDropping = stage == com.example.funlife.data.model.DiceGameStage.DROPPING_DICE
    val isDraggable = stage == com.example.funlife.data.model.DiceGameStage.REVEALED ||
            stage == com.example.funlife.data.model.DiceGameStage.IDLE

    val infinite = rememberInfiniteTransition(label = "stage")
    val shakeRotZ by infinite.animateFloat(
        initialValue = -22f, targetValue = 22f,
        animationSpec = infiniteRepeatable(tween(110, easing = EaseInOut), RepeatMode.Reverse),
        label = "shakeRz"
    )
    val shakeOffsetX by infinite.animateFloat(
        initialValue = -10f, targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(90, easing = EaseInOut), RepeatMode.Reverse),
        label = "shakeOx"
    )
    val shakeOffsetY by infinite.animateFloat(
        initialValue = -6f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(70, easing = EaseInOut), RepeatMode.Reverse),
        label = "shakeOy"
    )
    // 🔥 杯子下移动画：默认停在顶部（0），盖住时向下移动 ~110dp 罩住骰盘
    val density = androidx.compose.ui.platform.LocalDensity.current
    val coverDistancePx = with(density) { 110.dp.toPx() }
    val cupRise by animateFloatAsState(
        targetValue = if (cupCovered) coverDistancePx else 0f,
        animationSpec = tween(550, easing = EaseOutBack),
        label = "rise"
    )
    // 🔥 拖拽偏移：用户在 REVEALED/IDLE 时向下拖骰盅累计的位移
    var cupDragY by remember { mutableStateOf(0f) }
    // 当不可拖时归零
    LaunchedEffect(isDraggable) { if (!isDraggable) cupDragY = 0f }
    val coverThreshold = coverDistancePx * 0.6f // 拖过 60% 距离即视为已盖住
    // 骰子可见度：DROPPING/REVEALED 显示，IDLE/COVERED/SHAKING/SHAKEN 隐藏
    val diceVisible = isDropping ||
            stage == com.example.funlife.data.model.DiceGameStage.REVEALING || isRevealed
    val diceAlpha by animateFloatAsState(
        targetValue = if (diceVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "diceAlpha"
    )
    val revealScale by animateFloatAsState(
        targetValue = if (isRevealed) 1f else if (isDropping) 0.85f else 0.5f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
        label = "rs"
    )
    // 骰子下落动画（DROPPING 时使用）
    val dropProgress by animateFloatAsState(
        targetValue = if (isDropping) 1f else 0f,
        animationSpec = tween(900, easing = EaseOutBounce),
        label = "drop"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 当前玩家提示
        currentPlayer?.let {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(listOf(it.color.copy(alpha = 0.85f), it.color))
                    )
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(it.emoji, fontSize = 14.sp)
                    Text(
                        "${it.name} 的回合",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
        // 阶段提示文字
        Text(
            text = when (stage) {
                com.example.funlife.data.model.DiceGameStage.IDLE -> "🎲 准备投掷骰子..."
                com.example.funlife.data.model.DiceGameStage.DROPPING_DICE -> "🎲 骰子飞入骰盘..."
                com.example.funlife.data.model.DiceGameStage.COVERING -> "🥤 骰盅倒扣..."
                com.example.funlife.data.model.DiceGameStage.COVERED -> "👇 点「摇」开始摇骰（或晃手机）"
                com.example.funlife.data.model.DiceGameStage.SHAKING -> "💃 摇晃中..."
                com.example.funlife.data.model.DiceGameStage.SHAKEN -> "👇 点「摇」揭盅看点数"
                com.example.funlife.data.model.DiceGameStage.REVEALING -> "✨ 揭盅..."
                com.example.funlife.data.model.DiceGameStage.REVEALED -> "↓ 滑动骰盅向下盖住骰子"
            },
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.SemiBold
        )

        // 主舞台 - 顶部蓝色骰盅 + 中部留白 + 底部蓝色骰盘+骰子
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp),
            contentAlignment = Alignment.Center
        ) {
            // 蓝色骰盅 - 位于上半部，可竖直拖拽下来盖住骰子
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .graphicsLayer {
                        // 基础升降 + 用户拖拽偏移（仅 REVEALED/IDLE 可拖）
                        translationY = cupRise + cupDragY
                        if (isShaking) {
                            translationX = shakeOffsetX
                            translationY = cupRise + shakeOffsetY
                            rotationZ = shakeRotZ
                        }
                    }
                    .pointerInput(isDraggable) {
                        if (!isDraggable) return@pointerInput
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (cupDragY >= coverThreshold) {
                                    onCoverByDrag()
                                    cupDragY = 0f
                                } else {
                                    cupDragY = 0f
                                }
                            },
                            onDragCancel = { cupDragY = 0f }
                        ) { _: androidx.compose.ui.input.pointer.PointerInputChange, dy: Float ->
                            cupDragY = (cupDragY + dy).coerceIn(0f, coverDistancePx + 24f)
                        }
                    }
            ) {
                CuteCup()
                // 提示：当骰盅可拖时浮一行小字
                if (isDraggable && cupDragY < 4f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp)
                    ) {
                        Text(
                            "↓ 滑动骰盅向下盖住骰子",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 底部 蓝色骰盘 + 骰子
            Column(
                modifier = Modifier.align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier.size(width = 280.dp, height = 130.dp)
                ) {
                    // 骰盘
                    BluePlate(modifier = Modifier.align(Alignment.BottomCenter))
                    // 骰子（放在骰盘之上）
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 22.dp)
                            .graphicsLayer {
                                scaleX = revealScale; scaleY = revealScale
                                alpha = diceAlpha
                                if (isDropping) {
                                    translationY = (1f - dropProgress) * -300f
                                }
                            }
                    ) {
                        DiceCluster(diceCount, diceValues, isShaking)
                    }
                }
            }
        }
    }
}

// 🔥 蓝色骰盅 - 圆顶状（参考图深蓝色穹顶）
@Composable
private fun CuteCup() {
    Canvas(modifier = Modifier.size(width = 240.dp, height = 260.dp)) {
        val w = size.width; val h = size.height
        // 杯身（圆顶状：上半圆 + 下方直筒）
        val bodyPath = Path().apply {
            moveTo(w * 0.12f, h * 0.95f)
            lineTo(w * 0.12f, h * 0.32f)
            // 顶部圆弧
            cubicTo(
                w * 0.12f, h * 0.05f,
                w * 0.88f, h * 0.05f,
                w * 0.88f, h * 0.32f
            )
            lineTo(w * 0.88f, h * 0.95f)
            close()
        }
        // 阴影背景（让骰盅看起来立体）
        drawPath(
            bodyPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    SteelBlue,
                    DeepBlue1,
                    DeepBlue2,
                    DeepBlue3
                ),
                start = Offset(w * 0.15f, h * 0.1f),
                end = Offset(w * 0.95f, h * 0.95f)
            )
        )
        // 左侧高光
        val hlPath = Path().apply {
            moveTo(w * 0.18f, h * 0.85f)
            lineTo(w * 0.18f, h * 0.34f)
            cubicTo(
                w * 0.18f, h * 0.14f,
                w * 0.42f, h * 0.06f,
                w * 0.50f, h * 0.06f
            )
            // 回到内侧
            cubicTo(
                w * 0.40f, h * 0.10f,
                w * 0.26f, h * 0.20f,
                w * 0.26f, h * 0.36f
            )
            lineTo(w * 0.26f, h * 0.85f)
            close()
        }
        drawPath(hlPath, color = Color.White.copy(alpha = 0.10f))

        // 右下阴影
        val shPath = Path().apply {
            moveTo(w * 0.82f, h * 0.30f)
            lineTo(w * 0.82f, h * 0.95f)
            lineTo(w * 0.72f, h * 0.95f)
            lineTo(w * 0.72f, h * 0.36f)
            cubicTo(
                w * 0.72f, h * 0.20f,
                w * 0.62f, h * 0.10f,
                w * 0.55f, h * 0.06f
            )
            cubicTo(
                w * 0.70f, h * 0.07f,
                w * 0.82f, h * 0.18f,
                w * 0.82f, h * 0.30f
            )
            close()
        }
        drawPath(shPath, color = Color.Black.copy(alpha = 0.18f))

        // 顶部最高光斑
        drawCircle(
            Color.White.copy(alpha = 0.20f),
            radius = w * 0.05f,
            center = Offset(w * 0.36f, h * 0.16f)
        )
        // 杯口（底沿环 - 因为这是倒扣的骰盅，"底沿"在视觉上是杯子底部）
        drawOval(
            color = PlateRim,
            topLeft = Offset(w * 0.10f, h * 0.90f),
            size = Size(w * 0.80f, h * 0.10f)
        )
        drawOval(
            color = DeepBlue1,
            topLeft = Offset(w * 0.12f, h * 0.91f),
            size = Size(w * 0.76f, h * 0.07f)
        )
        // 底沿描边
        drawOval(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(w * 0.10f, h * 0.90f),
            size = Size(w * 0.80f, h * 0.10f),
            style = Stroke(width = 2f)
        )
    }
}

// 🔥 蓝色骰盘 - 圆形托盘（参考图骰子下方蓝色椭圆盘）
@Composable
private fun BluePlate(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 260.dp, height = 60.dp)) {
        val w = size.width; val h = size.height
        // 阴影
        drawOval(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(0f, h * 0.25f),
            size = Size(w, h * 0.75f)
        )
        // 盘外圈
        drawOval(
            brush = Brush.verticalGradient(
                colors = listOf(SteelBlue, DeepBlue1, DeepBlue2),
                startY = 0f, endY = h
            ),
            topLeft = Offset(w * 0.02f, h * 0.10f),
            size = Size(w * 0.96f, h * 0.85f)
        )
        // 盘内圈（更深）
        drawOval(
            brush = Brush.verticalGradient(
                colors = listOf(DeepBlue2, DeepBlue3, PlateRim),
                startY = h * 0.15f, endY = h * 0.95f
            ),
            topLeft = Offset(w * 0.07f, h * 0.20f),
            size = Size(w * 0.86f, h * 0.70f)
        )
        // 高光
        drawOval(
            color = Color.White.copy(alpha = 0.10f),
            topLeft = Offset(w * 0.12f, h * 0.22f),
            size = Size(w * 0.76f, h * 0.18f)
        )
    }
}

// 多颗骰子聚集 - 紧凑分布在骰盘上（等距投影占用空间略大）
@Composable
private fun DiceCluster(count: Int, values: List<Int>, isShaking: Boolean) {
    // 5 颗及以下：错位两排（前排 3 + 后排 2），更接近参考图
    if (count <= 5) {
        val front = values.take(3.coerceAtMost(count))
        val back = if (count > 3) values.subList(3, count) else emptyList()
        Column(
            verticalArrangement = Arrangement.spacedBy((-14).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 后排（小一点、上抬错位）
            if (back.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    back.forEachIndexed { i, v ->
                        Dice2D(value = v, isShaking = isShaking, idx = i + 100, sizeDp = 50)
                    }
                }
            }
            // 前排
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                front.forEachIndexed { i, v ->
                    Dice2D(value = v, isShaking = isShaking, idx = i, sizeDp = 56)
                }
            }
        }
    } else {
        // 6 颗以上：每行 5 颗
        val rowItems = values.chunked(5)
        Column(
            verticalArrangement = Arrangement.spacedBy((-8).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            rowItems.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    row.forEachIndexed { i, v ->
                        Dice2D(value = v, isShaking = isShaking, idx = i, sizeDp = 44)
                    }
                }
            }
        }
    }
}

/**
 * 🔥 等距投影 3D 骰子 - 同时展示前/顶/右三面，复刻参考图立体感
 * 主面 = value，顶面/右面是与 value 相邻的两个合理面（按真实骰子相邻关系）。
 */
@Composable
private fun Dice2D(value: Int, isShaking: Boolean, idx: Int, sizeDp: Int = 56) {
    // 摇晃时整体抖动 + 旋转
    val infinite = rememberInfiniteTransition(label = "d2d$idx")
    val jitterRot by infinite.animateFloat(
        initialValue = -6f, targetValue = 6f,
        animationSpec = infiniteRepeatable(
            tween(120 + (idx * 30) % 80, easing = EaseInOut), RepeatMode.Reverse
        ),
        label = "jr$idx"
    )
    val jitterY by infinite.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(
            tween(90 + (idx * 23) % 60, easing = EaseInOut), RepeatMode.Reverse
        ),
        label = "jy$idx"
    )

    // 主面对应的相邻顶面 / 右面（真实骰子相邻关系任选）
    val (topVal, rightVal) = remember(value) {
        when (value) {
            1 -> 2 to 3
            2 -> 1 to 4
            3 -> 1 to 5
            4 -> 2 to 6
            5 -> 3 to 6
            6 -> 4 to 5
            else -> 2 to 3
        }
    }

    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .graphicsLayer {
                if (isShaking) {
                    rotationZ = jitterRot
                    translationY = jitterY
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // 立方体顶点（更平缓的 isometric 透视，深度约 35%）
            fun p(x: Float, y: Float) = Offset(x * w, y * h)
            val ftl = p(0.06f, 0.36f)
            val ftr = p(0.66f, 0.36f)
            val fbl = p(0.06f, 0.92f)
            val fbr = p(0.66f, 0.92f)
            // 深度向右上偏移 dx=0.20, dy=-0.18
            val ttl = p(0.26f, 0.18f)
            val ttr = p(0.86f, 0.18f)
            val rbb = p(0.86f, 0.74f)

            val red = Color(0xFFE53935)
            val black = Color(0xFF1A1A1A)
            fun pipColor(v: Int) = if (v == 1 || v == 4) red else black
            fun pipScale(v: Int) = if (v == 1) 1.7f else 1f

            val pipsForFace: (Int) -> List<Pair<Float, Float>> = { v ->
                when (v) {
                    1 -> listOf(0.5f to 0.5f)
                    2 -> listOf(0.28f to 0.28f, 0.72f to 0.72f)
                    3 -> listOf(0.28f to 0.28f, 0.5f to 0.5f, 0.72f to 0.72f)
                    4 -> listOf(0.28f to 0.28f, 0.72f to 0.28f, 0.28f to 0.72f, 0.72f to 0.72f)
                    5 -> listOf(0.28f to 0.28f, 0.72f to 0.28f, 0.5f to 0.5f, 0.28f to 0.72f, 0.72f to 0.72f)
                    6 -> listOf(0.28f to 0.28f, 0.72f to 0.28f, 0.28f to 0.5f, 0.72f to 0.5f, 0.28f to 0.72f, 0.72f to 0.72f)
                    else -> emptyList()
                }
            }

            // ─ 地面柔和阴影 ─
            drawOval(
                color = Color.Black.copy(alpha = 0.30f),
                topLeft = Offset(w * 0.02f, h * 0.91f),
                size = Size(w * 0.94f, h * 0.09f)
            )

            // ─ 右面（白色但暗一档，背光）─
            val rightPath = Path().apply {
                moveTo(ftr.x, ftr.y); lineTo(ttr.x, ttr.y); lineTo(rbb.x, rbb.y); lineTo(fbr.x, fbr.y); close()
            }
            drawPath(
                path = rightPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFCED3D9), Color(0xFF9097A0)),
                    start = ftr, end = rbb
                )
            )

            // ─ 顶面（最亮的白色）─
            val topPath = Path().apply {
                moveTo(ttl.x, ttl.y); lineTo(ttr.x, ttr.y); lineTo(ftr.x, ftr.y); lineTo(ftl.x, ftl.y); close()
            }
            drawPath(
                path = topPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFF1F4F7)),
                    start = ttl, end = ftr
                )
            )

            // ─ 前面（亮白，正面散射光）─
            val frontPath = Path().apply {
                moveTo(ftl.x, ftl.y); lineTo(ftr.x, ftr.y); lineTo(fbr.x, fbr.y); lineTo(fbl.x, fbl.y); close()
            }
            drawPath(
                path = frontPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFAFBFC), Color(0xFFD8DDE3)),
                    start = ftl, end = fbr
                )
            )

            // ─ 边棱亮白高光（模拟圆角反光，柔化棱角）─
            drawLine(color = Color.White.copy(alpha = 0.95f), start = ttl, end = ttr, strokeWidth = 2f)  // 顶面后缘
            drawLine(color = Color.White.copy(alpha = 0.85f), start = ttl, end = ftl, strokeWidth = 1.6f) // 顶左前缘
            drawLine(color = Color.White.copy(alpha = 0.80f), start = ftl, end = ftr, strokeWidth = 1.6f) // 顶前缘
            // 右面交界线（顶/右、前/右）— 偏暗细线区分
            drawLine(color = Color(0xFF7A818A), start = ttr, end = ftr, strokeWidth = 1.0f)
            drawLine(color = Color(0xFF7A818A).copy(alpha = 0.85f), start = ftr, end = fbr, strokeWidth = 1.0f)
            // 底前缘（轻暗）
            drawLine(color = Color(0xFFB8BFC6), start = fbl, end = fbr, strokeWidth = 1.0f)
            drawLine(color = Color(0xFFB8BFC6), start = ftl, end = fbl, strokeWidth = 1.0f)

            // 局部 (u,v) → 屏幕坐标
            fun frontPos(u: Float, v: Float) = Offset(
                ftl.x + u * (ftr.x - ftl.x) + v * (fbl.x - ftl.x),
                ftl.y + u * (ftr.y - ftl.y) + v * (fbl.y - ftl.y)
            )
            fun topPos(u: Float, v: Float) = Offset(
                ttl.x + u * (ttr.x - ttl.x) + v * (ftl.x - ttl.x),
                ttl.y + u * (ttr.y - ttl.y) + v * (ftl.y - ttl.y)
            )
            fun rightPos(u: Float, v: Float) = Offset(
                ftr.x + u * (ttr.x - ftr.x) + v * (fbr.x - ftr.x),
                ftr.y + u * (ttr.y - ftr.y) + v * (fbr.y - ftr.y)
            )

            // 前面 pip（最大）
            val frontPipR = w * 0.060f
            pipsForFace(value).forEach { (u, v) ->
                drawCircle(pipColor(value), radius = frontPipR * pipScale(value), center = frontPos(u, v))
            }
            // 顶面 pip
            val topPipR = w * 0.046f
            pipsForFace(topVal).forEach { (u, v) ->
                drawCircle(pipColor(topVal), radius = topPipR * pipScale(topVal), center = topPos(u, v))
            }
            // 右面 pip
            val rightPipR = w * 0.044f
            pipsForFace(rightVal).forEach { (u, v) ->
                drawCircle(pipColor(rightVal), radius = rightPipR * pipScale(rightVal), center = rightPos(u, v))
            }
        }
    }
}

// 单颗真 3D 立方体骰子 - 使用 OpenGL ES 2.0 渲染
@Composable
private fun Dice3DCube(value: Int, isShaking: Boolean, idx: Int) {
    val sizeDp = 64.dp
    // 持续旋转角度（摇晃时快速翻滚；静止时缓慢飘动）
    val infinite = rememberInfiniteTransition(label = "glcube$idx")
    val spinX by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(if (isShaking) 600 + idx * 60 else 6000, easing = LinearEasing)
        ),
        label = "spx$idx"
    )
    val spinY by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(if (isShaking) 750 + idx * 90 else 8000, easing = LinearEasing)
        ),
        label = "spy$idx"
    )
    val spinZ by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(if (isShaking) 900 + idx * 110 else 10000, easing = LinearEasing)
        ),
        label = "spz$idx"
    )

    // 静止状态大角度倾斜，同时露出 3 个面（前/上/右），让立方体感非常明显
    val idleSwayY by infinite.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOut), RepeatMode.Reverse),
        label = "sway$idx"
    )
    val targetRotX = if (isShaking) spinX else -28f
    val targetRotY = if (isShaking) spinY else (35f + idleSwayY + (idx % 3) * 6f)
    val targetRotZ = if (isShaking) spinZ else 0f

    AndroidView(
        modifier = Modifier.size(sizeDp),
        factory = { ctx ->
            DiceCubeGLView(ctx).apply {
                cubeRenderer.faceFront = value
            }
        },
        update = { v ->
            v.cubeRenderer.faceFront = value
            v.cubeRenderer.rotX = targetRotX
            v.cubeRenderer.rotY = targetRotY
            v.cubeRenderer.rotZ = targetRotZ
        }
    )
}

// ════════════════════════════════════════════════════════════
// 操作按钮区
// ════════════════════════════════════════════════════════════
@Composable
private fun StageActionButtons(
    stage: com.example.funlife.data.model.DiceGameStage,
    onDropDice: () -> Unit,
    onShake: () -> Unit,
    onReveal: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (stage) {
            com.example.funlife.data.model.DiceGameStage.IDLE -> {
                // 单按钮：投掷骰子
                BigPinkButton(
                    text = "🎲 投掷骰子",
                    enabled = true,
                    onClick = onDropDice,
                    modifier = Modifier.fillMaxWidth(),
                    highlight = true
                )
            }
            com.example.funlife.data.model.DiceGameStage.DROPPING_DICE -> {
                BigPinkButton(
                    text = "🎲 骰子飞入中...",
                    enabled = false,
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
            com.example.funlife.data.model.DiceGameStage.COVERING -> {
                BigPinkButton(
                    text = "🥤 杯子倒扣中...",
                    enabled = false,
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
            com.example.funlife.data.model.DiceGameStage.COVERED -> {
                BigPinkButton(
                    text = "🎲 摇一摇",
                    enabled = true,
                    onClick = onShake,
                    modifier = Modifier.fillMaxWidth(),
                    highlight = true
                )
            }
            com.example.funlife.data.model.DiceGameStage.SHAKING -> {
                BigPinkButton(
                    text = "💃 摇晃中...",
                    enabled = false,
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
            com.example.funlife.data.model.DiceGameStage.SHAKEN -> {
                BigPinkButton(
                    text = "👀 揭杯",
                    enabled = true,
                    onClick = onReveal,
                    modifier = Modifier.fillMaxWidth(),
                    highlight = true
                )
            }
            com.example.funlife.data.model.DiceGameStage.REVEALING -> {
                BigPinkButton(
                    text = "✨ 揭晓中...",
                    enabled = false,
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
            com.example.funlife.data.model.DiceGameStage.REVEALED -> {
                BigPinkButton(
                    text = "➡️ 下一位",
                    enabled = true,
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth(),
                    highlight = true
                )
            }
        }
    }
}

@Composable
private fun BigPinkButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    val bgBrush = when {
        !enabled -> Brush.horizontalGradient(listOf(Color(0xFFE0E0E0), Color(0xFFBDBDBD)))
        highlight -> Brush.horizontalGradient(listOf(GoldStar, Color(0xFFFFA000)))
        else -> Brush.horizontalGradient(listOf(PinkMid, PinkDeep))
    }
    Box(
        modifier = modifier
            .height(48.dp)
            .shadow(if (enabled) 8.dp else 0.dp, RoundedCornerShape(50), ambientColor = PinkDeep, spotColor = PinkDeep)
            .clip(RoundedCornerShape(50))
            .background(bgBrush)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            letterSpacing = 1.sp,
            style = TextStyle(shadow = Shadow(Color(0xFFAD1457), Offset(0f, 2f), 4f))
        )
    }
}

// ════════════════════════════════════════════════════════════
// 当前结果面板
// ════════════════════════════════════════════════════════════
@Composable
private fun CurrentResultPanel(
    player: DicePlayer?,
    diceValues: List<Int>,
    mode: DiceGameMode,
    penaltyNumber: Int,
    truthDareCard: TruthOrDareCard?,
    onDrawCard: () -> Unit,
    onDrawTruth: () -> Unit,
    onDrawDare: () -> Unit
) {
    if (player == null) return
    val sum = diceValues.sum()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(2.dp, PinkMid, RoundedCornerShape(20.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(player.emoji, fontSize = 22.sp)
            Text(player.name, fontSize = 14.sp, fontWeight = FontWeight.Black, color = PinkAccent)
            Spacer(modifier = Modifier.weight(1f))
            Text("点数和", fontSize = 11.sp, color = PinkAccent.copy(alpha = 0.7f))
            Text(
                sum.toString(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = PinkDeep,
                style = TextStyle(shadow = Shadow(GoldStar, Offset(0f, 2f), 4f))
            )
        }
        // 模式特殊内容
        when (mode) {
            DiceGameMode.NUMBER_PENALTY -> {
                val hit = diceValues.contains(penaltyNumber)
                Text(
                    if (hit) "🥃 投出了 $penaltyNumber！罚酒一杯！" else "✅ 安全过关，未投出 $penaltyNumber",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hit) Color(0xFFD32F2F) else Color(0xFF388E3C)
                )
            }
            DiceGameMode.BLACKJACK_21 -> {
                val total = player.totalScore
                val msg = when {
                    total == 21 -> "🎉 21点！恭喜！"
                    total > 21 -> "💥 爆掉了！($total)"
                    else -> "💎 累计 $total 点（目标21）"
                }
                Text(msg, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PinkAccent)
            }
            DiceGameMode.TRUTH_DARE -> {
                if (truthDareCard != null) {
                    TruthDareCardView(card = truthDareCard)
                } else {
                    Text("回合结束后最低点者抽取卡片 💋", fontSize = 12.sp, color = PinkAccent.copy(alpha = 0.7f))
                }
            }
            DiceGameMode.DRINKING_BIG_DRINKS -> Text("回合结束最大者罚酒 🍻", fontSize = 12.sp, color = PinkAccent.copy(alpha = 0.7f))
            DiceGameMode.DRINKING_SMALL_DRINKS -> Text("回合结束最小者罚酒 🍷", fontSize = 12.sp, color = PinkAccent.copy(alpha = 0.7f))
            DiceGameMode.LIAR_DICE -> Text("吹牛模式：等所有人投完后报点 🎰", fontSize = 12.sp, color = PinkAccent.copy(alpha = 0.7f))
            DiceGameMode.COMPARE_SIZE -> Text("回合结束比拼最大点数 🎯", fontSize = 12.sp, color = PinkAccent.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun TruthDareCardView(card: TruthOrDareCard) {
    val bgBrush = if (card.type == CardType.TRUTH) {
        Brush.horizontalGradient(listOf(Color(0xFFFFCAD4), Color(0xFFFF80AB)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFFFFAB91), Color(0xFFFF7043)))
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgBrush)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(card.type.emoji, fontSize = 18.sp)
            Text(
                card.type.displayName,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
        }
        Text(
            card.content,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            style = TextStyle(shadow = Shadow(Color(0xFF880E4F), Offset(0f, 1f), 3f))
        )
    }
}

// ════════════════════════════════════════════════════════════
// 添加玩家弹窗
// ════════════════════════════════════════════════════════════
@Composable
private fun AddPlayerDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("👥", fontSize = 22.sp)
                Text("添加玩家", color = PinkAccent, fontWeight = FontWeight.Black)
            }
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("玩家名字", color = PinkAccent) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PinkDeep,
                    unfocusedBorderColor = PinkSoft,
                    cursorColor = PinkDeep
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) {
                Text("确定", color = PinkDeep, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color.Gray)
            }
        }
    )
}

// ════════════════════════════════════════════════════════════
// 设置弹窗
// ════════════════════════════════════════════════════════════
@Composable
private fun SettingsDialog(
    sensorEnabled: Boolean,
    onToggleSensor: (Boolean) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("⚙️", fontSize = 22.sp)
                Text("游戏设置", color = PinkAccent, fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📳 重力感应摇晃", modifier = Modifier.weight(1f))
                    Switch(
                        checked = sensorEnabled,
                        onCheckedChange = onToggleSensor,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PinkDeep
                        )
                    )
                }
                Text(
                    "开启后，晃动手机即可摇骰子",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Divider()
                TextButton(
                    onClick = { onReset(); onDismiss() },
                    colors = ButtonDefaults.textButtonColors(contentColor = PinkDeep)
                ) {
                    Icon(Icons.Default.RestartAlt, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("重置积分", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = PinkDeep, fontWeight = FontWeight.Bold)
            }
        }
    )
}

// ════════════════════════════════════════════════════════════
// 回合结算弹窗
// ════════════════════════════════════════════════════════════
@Composable
private fun RoundResultDialog(
    players: List<DicePlayer>,
    mode: DiceGameMode,
    winner: DicePlayer?,
    loser: DicePlayer?,
    truthDareCard: TruthOrDareCard? = null,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🎉", fontSize = 48.sp)
                Text(
                    "本轮结算",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = PinkAccent
                )
                Text("[${mode.displayName}] ${mode.emoji}", fontSize = 13.sp, color = PinkAccent.copy(alpha = 0.7f))
                Divider()
                winner?.let {
                    Text("🏆 赢家：${it.emoji} ${it.name} (${it.lastSum}点)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF388E3C))
                }
                loser?.let {
                    val penalty = if (mode.isDrinking) "罚一杯 🥃" else "接受惩罚"
                    Text("💔 输家：${it.emoji} ${it.name} (${it.lastSum}点) - $penalty", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                }
                // 真心话/大冒险卡片
                if (truthDareCard != null && loser != null) {
                    Text("👇 ${loser.name} 抽到了：", fontSize = 12.sp, color = PinkAccent.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                    TruthDareCardView(card = truthDareCard)
                }
                Divider()
                Text("当前排名", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PinkAccent)
                players.sortedByDescending { it.totalScore }.forEachIndexed { idx, p ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${idx + 1}.", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PinkAccent)
                        Spacer(Modifier.width(6.dp))
                        Text(p.emoji, fontSize = 16.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(p.name, modifier = Modifier.weight(1f), fontSize = 13.sp)
                        Text("💎${p.totalScore}", fontSize = 12.sp, color = PinkDeep, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text("🏆${p.wins}", fontSize = 12.sp, color = GoldStar, fontWeight = FontWeight.Bold)
                    }
                }
                BigPinkButton(
                    text = "继续游戏",
                    enabled = true,
                    onClick = onDismiss,
                    highlight = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}



// ════════════════════════════════════════════════════════════
// 🎰 吹牛骰盅 - 叫数面板
// ════════════════════════════════════════════════════════════
@Composable
private fun LiarBiddingPanel(
    currentPlayer: DicePlayer?,
    bidCount: Int,
    bidFace: Int,
    onRaise: (Int, Int) -> Unit,
    onChallenge: () -> Unit
) {
    var newCount by remember(bidCount) { mutableStateOf(if (bidCount == 0) 1 else bidCount) }
    var newFace by remember(bidFace) { mutableStateOf(bidFace) }
    var errMsg by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.95f))
            .border(2.dp, PinkMid, RoundedCornerShape(20.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 标题
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("🎰", fontSize = 20.sp)
            Text("吹牛叫数", fontSize = 14.sp, fontWeight = FontWeight.Black, color = PinkAccent)
            Spacer(modifier = Modifier.weight(1f))
            currentPlayer?.let {
                Text("${it.emoji} ${it.name}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = it.color)
            }
        }
        // 当前叫数
        if (bidCount > 0) {
            Text(
                "上家叫: $bidCount 个 $bidFace",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD32F2F)
            )
        } else {
            Text("第一位叫数（请大胆估计 🎲）", fontSize = 11.sp, color = PinkAccent.copy(alpha = 0.7f))
        }
        // 数量调整
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("数量:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PinkAccent)
            CountStepper(value = newCount, onChange = { newCount = it.coerceAtLeast(1) })
            Text("个", fontSize = 12.sp, color = PinkAccent)
            Spacer(modifier = Modifier.weight(1f))
            Text("点数:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PinkAccent)
            (1..6).forEach { f ->
                val sel = f == newFace
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (sel) PinkDeep else Color(0xFFFFF0F5))
                        .border(1.dp, if (sel) PinkAccent else PinkSoft, CircleShape)
                        .clickable { newFace = f },
                    contentAlignment = Alignment.Center
                ) {
                    Text(f.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (sel) Color.White else PinkAccent)
                }
            }
        }
        if (errMsg.isNotEmpty()) {
            Text(errMsg, fontSize = 11.sp, color = Color.Red)
        }
        // 操作按钮
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigPinkButton(
                text = "📢 叫 $newCount 个 $newFace",
                enabled = true,
                onClick = {
                    val ok = onRaiseAttempt(newCount, newFace, bidCount, bidFace, onRaise)
                    if (!ok) {
                        errMsg = "必须比上家叫数大！(数量↑ 或 同数量点数↑)"
                    } else {
                        errMsg = ""
                    }
                },
                modifier = Modifier.weight(1f)
            )
            if (bidCount > 0) {
                BigPinkButton(
                    text = "🚨 开盅",
                    enabled = true,
                    onClick = onChallenge,
                    highlight = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun onRaiseAttempt(
    newCount: Int,
    newFace: Int,
    curCount: Int,
    curFace: Int,
    onRaise: (Int, Int) -> Unit
): Boolean {
    val higher = curCount == 0 || newCount > curCount || (newCount == curCount && newFace > curFace)
    if (!higher) return false
    onRaise(newCount, newFace)
    return true
}

@Composable
private fun CountStepper(value: Int, onChange: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(PinkSoft)
                .clickable(enabled = value > 1) { onChange(value - 1) },
            contentAlignment = Alignment.Center
        ) {
            Text("−", fontSize = 14.sp, fontWeight = FontWeight.Black, color = PinkAccent)
        }
        Text(
            value.toString(),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(1.dp, PinkMid, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 2.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = PinkAccent
        )
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(PinkSoft)
                .clickable { onChange(value + 1) },
            contentAlignment = Alignment.Center
        ) {
            Text("+", fontSize = 14.sp, fontWeight = FontWeight.Black, color = PinkAccent)
        }
    }
}

// ════════════════════════════════════════════════════════════
// 🎰 吹牛骰盅 - 开盅结果面板
// ════════════════════════════════════════════════════════════
@Composable
private fun LiarShowdownPanel(
    outcome: LiarChallengeOutcome,
    players: List<DicePlayer>,
    onNextRound: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(3.dp, GoldStar, RoundedCornerShape(20.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🚨 开盅！", fontSize = 22.sp, fontWeight = FontWeight.Black, color = PinkAccent)
        Text(
            "${outcome.challengerName} 不信 ${outcome.bidderName} 叫的 ${outcome.claimedCount} 个 ${outcome.claimedFace}",
            fontSize = 12.sp,
            color = Color.DarkGray,
            textAlign = TextAlign.Center
        )
        Text(
            "实际场上有 ${outcome.actualCount} 个 ${outcome.claimedFace}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (outcome.bidderTruthful) Color(0xFF388E3C) else Color(0xFFD32F2F)
        )
        Text(
            if (outcome.bidderTruthful) "✅ ${outcome.bidderName} 叫数成立！" else "❌ ${outcome.bidderName} 吹牛了！",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = if (outcome.bidderTruthful) Color(0xFF388E3C) else Color(0xFFD32F2F)
        )
        Text(
            "🏆 ${outcome.winnerName} 胜出（+5分） / 🥃 ${outcome.loserName} 罚酒",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = PinkAccent,
            textAlign = TextAlign.Center
        )
        Divider()
        // 显示所有玩家骰子
        Text("全场骰子", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PinkAccent)
        players.forEach { p ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(p.emoji, fontSize = 14.sp)
                Text(p.name, fontSize = 11.sp, modifier = Modifier.width(60.dp), color = PinkAccent)
                p.lastRoll.forEach { v ->
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (v == outcome.claimedFace) GoldStar.copy(alpha = 0.4f)
                                else Color.White
                            )
                            .border(
                                1.dp,
                                if (v == outcome.claimedFace) GoldStar else PinkSoft,
                                RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(v.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PinkAccent)
                    }
                }
            }
        }
        BigPinkButton(
            text = "🎲 下一轮",
            enabled = true,
            onClick = onNextRound,
            highlight = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

