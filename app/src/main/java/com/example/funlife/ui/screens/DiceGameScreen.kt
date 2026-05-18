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

// 主题色
private val PinkLight = Color(0xFFFFE0EC)
private val PinkSoft = Color(0xFFFFCAD4)
private val PinkMid = Color(0xFFFF80AB)
private val PinkDeep = Color(0xFFEC407A)
private val PinkAccent = Color(0xFFD81B60)
private val GoldStar = Color(0xFFFFC107)
private val CreamBg = Color(0xFFFFF5F8)

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

    // 🎬 流程动画驱动 — 根据 gameStage 自动推进
    LaunchedEffect(gameStage) {
        when (gameStage) {
            com.example.funlife.data.model.DiceGameStage.DROPPING_DICE -> {
                // 1.4 秒骰子飞入动画 + 音效
                soundManager.play(com.example.funlife.utils.SoundEffect.DICE_DROP, volume = 0.7f)
                delay(1400)
                viewModel.setStage(com.example.funlife.data.model.DiceGameStage.COVERING)
            }
            com.example.funlife.data.model.DiceGameStage.COVERING -> {
                // 0.6 秒杯子翻转倒扣动画
                delay(600)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF0F5),
                        Color(0xFFFFE0EC),
                        Color(0xFFFFCAD4)
                    )
                )
            )
    ) {
        // 背景装饰
        BackgroundDecor()

        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部栏
            DiceTopBar(
                onBack = onNavigateBack,
                onSettings = { showSettings = true },
                sensorEnabled = sensorShakeEnabled,
                onToggleSensor = { sensorShakeEnabled = !sensorShakeEnabled }
            )

            // 模式选择
            ModeSelectorRow(
                selected = gameMode,
                onSelect = { viewModel.setGameMode(it) }
            )

            // 数字定罚目标数字（仅在该模式下）
            AnimatedVisibility(visible = gameMode == DiceGameMode.NUMBER_PENALTY) {
                PenaltyNumberPicker(
                    selected = penaltyNumber,
                    onSelect = { viewModel.setPenaltyNumber(it) }
                )
            }

            // 骰子数量调整
            DiceCountAdjuster(
                count = diceCount,
                onChange = { viewModel.setDiceCount(it) }
            )

            // 玩家滑动条
            PlayersRow(
                players = players,
                currentIndex = currentPlayerIndex,
                onAdd = { showAddPlayer = true },
                onRemove = { viewModel.removePlayer(it) }
            )

            // 主舞台 - 杯子 + 骰子
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
                    currentPlayer = players.getOrNull(currentPlayerIndex)
                )
            }

            // 操作按钮区（根据 stage 显示不同按钮）
            StageActionButtons(
                stage = gameStage,
                onDropDice = { viewModel.setStage(com.example.funlife.data.model.DiceGameStage.DROPPING_DICE) },
                onShake = { viewModel.setStage(com.example.funlife.data.model.DiceGameStage.SHAKING) },
                onReveal = { viewModel.setStage(com.example.funlife.data.model.DiceGameStage.REVEALING) },
                onNext = { viewModel.nextPlayer() }
            )

            // 当前玩家结果 + 模式提示
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
        // 散落的心形和星星
        val w = size.width; val h = size.height
        listOf(
            Triple(0.08f, 0.12f, 16f),
            Triple(0.85f, 0.08f, 12f),
            Triple(0.92f, 0.35f, 14f),
            Triple(0.05f, 0.55f, 10f),
            Triple(0.88f, 0.78f, 18f),
            Triple(0.12f, 0.88f, 14f),
            Triple(0.50f, 0.05f, 9f)
        ).forEach { (fx, fy, s) ->
            drawDecorHeart(Offset(w * fx, h * fy + float), s, Color.White.copy(alpha = 0.5f))
        }
        listOf(
            0.20f to 0.20f, 0.75f to 0.25f, 0.30f to 0.85f, 0.95f to 0.55f, 0.40f to 0.40f
        ).forEach { (fx, fy) ->
            drawSparkleStar(Offset(w * fx, h * fy - float * 0.3f), 8f, Color.White.copy(alpha = 0.55f))
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
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, "返回", tint = PinkAccent)
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .shadow(8.dp, RoundedCornerShape(50), ambientColor = PinkDeep, spotColor = PinkDeep)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(listOf(PinkMid, PinkDeep))
                )
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("🎲", fontSize = 20.sp)
            Text(
                "骰子游戏",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                letterSpacing = 1.5.sp,
                style = TextStyle(shadow = Shadow(Color(0xFFAD1457), Offset(0f, 2f), 4f))
            )
            Text("✨", fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.weight(1f))
        // 重力感应开关
        IconButton(onClick = onToggleSensor) {
            Icon(
                if (sensorEnabled) Icons.Default.Vibration else Icons.Default.PhoneAndroid,
                "重力感应",
                tint = if (sensorEnabled) PinkAccent else Color.Gray
            )
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Default.Settings, "设置", tint = PinkAccent)
        }
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
    currentPlayer: DicePlayer?
) {
    val cupCovered = stage == com.example.funlife.data.model.DiceGameStage.COVERING ||
            stage == com.example.funlife.data.model.DiceGameStage.COVERED ||
            stage == com.example.funlife.data.model.DiceGameStage.SHAKING ||
            stage == com.example.funlife.data.model.DiceGameStage.SHAKEN
    val isShaking = stage == com.example.funlife.data.model.DiceGameStage.SHAKING
    val isRevealed = stage == com.example.funlife.data.model.DiceGameStage.REVEALED
    val isDropping = stage == com.example.funlife.data.model.DiceGameStage.DROPPING_DICE

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
    // 杯子升降动画：盖住时 0，未盖时升到 -260
    val cupRise by animateFloatAsState(
        targetValue = if (cupCovered) 0f else -260f,
        animationSpec = tween(550, easing = EaseOutBack),
        label = "rise"
    )
    // 杯子翻转动画：IDLE/DROPPING 时 0°（直立），COVERING 起翻转 180°
    val cupFlip by animateFloatAsState(
        targetValue = if (stage == com.example.funlife.data.model.DiceGameStage.IDLE ||
                        stage == com.example.funlife.data.model.DiceGameStage.DROPPING_DICE) 0f else 180f,
        animationSpec = tween(550, easing = EaseInOutCubic),
        label = "flip"
    )
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
                com.example.funlife.data.model.DiceGameStage.IDLE -> "👇 点击「投掷骰子」把骰子放进杯子"
                com.example.funlife.data.model.DiceGameStage.DROPPING_DICE -> "🎲 骰子飞入杯中..."
                com.example.funlife.data.model.DiceGameStage.COVERING -> "🥤 杯子倒扣盖住骰子..."
                com.example.funlife.data.model.DiceGameStage.COVERED -> "👇 点击「摇一摇」摇晃杯子（也可摇手机）"
                com.example.funlife.data.model.DiceGameStage.SHAKING -> "💃 摇晃中... 骰子叮当响"
                com.example.funlife.data.model.DiceGameStage.SHAKEN -> "👇 点击「揭杯」查看点数"
                com.example.funlife.data.model.DiceGameStage.REVEALING -> "✨ 杯子升起，揭晓时刻..."
                com.example.funlife.data.model.DiceGameStage.REVEALED -> "🎉 看看你的运气！点击「下一位」继续"
            },
            fontSize = 11.sp,
            color = PinkAccent.copy(alpha = 0.85f),
            fontWeight = FontWeight.SemiBold
        )

        // 主舞台
        Box(
            modifier = Modifier
                .size(width = 280.dp, height = 320.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // 阴影台面
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(width = 240.dp, height = 24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.2f))
            )
            // 骰子区（在杯子下方位置）
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
                    .graphicsLayer {
                        scaleX = revealScale; scaleY = revealScale
                        alpha = diceAlpha
                        // DROPPING 时从空中掉落
                        if (isDropping) {
                            translationY = (1f - dropProgress) * -300f
                        }
                    }
            ) {
                DiceCluster(diceCount, diceValues, isShaking)
            }
            // 杯子
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationY = cupRise
                        // 翻转：从直立 (0°) 旋转到倒扣 (180°)
                        rotationZ = cupFlip
                        if (isShaking) {
                            translationX = shakeOffsetX
                            translationY = cupRise + shakeOffsetY
                            rotationZ = cupFlip + shakeRotZ
                        }
                    }
            ) {
                CuteCup()
            }
        }
    }
}

// 可爱杯子 / 骰盅
@Composable
private fun CuteCup() {
    Canvas(modifier = Modifier.size(width = 220.dp, height = 260.dp)) {
        val w = size.width; val h = size.height
        // 杯子主体（倒扣，下宽上窄）
        val bodyPath = Path().apply {
            moveTo(w * 0.10f, h * 0.18f)
            lineTo(w * 0.90f, h * 0.18f)
            lineTo(w * 0.78f, h * 0.92f)
            lineTo(w * 0.22f, h * 0.92f)
            close()
        }
        // 阴影
        drawPath(
            bodyPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFF8FA3),
                    Color(0xFFEC407A),
                    Color(0xFFAD1457)
                )
            )
        )
        // 杯口椭圆（顶部）
        drawOval(
            color = Color(0xFFAD1457),
            topLeft = Offset(w * 0.10f, h * 0.12f),
            size = Size(w * 0.80f, h * 0.14f)
        )
        drawOval(
            color = Color(0xFFEC407A),
            topLeft = Offset(w * 0.13f, h * 0.13f),
            size = Size(w * 0.74f, h * 0.10f)
        )
        // 高光
        drawPath(
            Path().apply {
                moveTo(w * 0.20f, h * 0.25f)
                cubicTo(w * 0.18f, h * 0.55f, w * 0.22f, h * 0.75f, w * 0.30f, h * 0.88f)
                lineTo(w * 0.36f, h * 0.88f)
                cubicTo(w * 0.28f, h * 0.70f, w * 0.26f, h * 0.45f, w * 0.30f, h * 0.25f)
                close()
            },
            color = Color.White.copy(alpha = 0.35f)
        )
        // 杯底圈
        drawOval(
            color = Color(0xFF880E4F).copy(alpha = 0.6f),
            topLeft = Offset(w * 0.22f, h * 0.88f),
            size = Size(w * 0.56f, h * 0.10f),
            style = Stroke(width = 4f)
        )
        // 装饰小心 + 装饰
        drawDecorHeart(Offset(w * 0.55f, h * 0.45f), 12f, Color.White.copy(alpha = 0.9f))
        drawDecorHeart(Offset(w * 0.40f, h * 0.65f), 8f, Color.White.copy(alpha = 0.7f))
        drawSparkleStar(Offset(w * 0.70f, h * 0.55f), 6f, Color.White.copy(alpha = 0.8f))
        drawSparkleStar(Offset(w * 0.30f, h * 0.40f), 5f, Color.White.copy(alpha = 0.7f))

        // 蝴蝶结（杯口装饰）
        val bowCx = w * 0.50f; val bowCy = h * 0.10f
        drawPath(
            Path().apply {
                moveTo(bowCx, bowCy)
                lineTo(bowCx - 18f, bowCy - 10f)
                lineTo(bowCx - 22f, bowCy + 10f)
                close()
            },
            color = Color(0xFFFFC0CB)
        )
        drawPath(
            Path().apply {
                moveTo(bowCx, bowCy)
                lineTo(bowCx + 18f, bowCy - 10f)
                lineTo(bowCx + 22f, bowCy + 10f)
                close()
            },
            color = Color(0xFFFFC0CB)
        )
        drawCircle(Color(0xFFFF80AB), radius = 6f, center = Offset(bowCx, bowCy))
    }
}

// 多颗骰子聚集
@Composable
private fun DiceCluster(count: Int, values: List<Int>, isShaking: Boolean) {
    // 简单 grid 布局：每行 3 颗
    val rows = (count + 2) / 3
    val rowItems = mutableListOf<List<Int>>()
    for (r in 0 until rows) {
        val start = r * 3
        val end = (start + 3).coerceAtMost(count)
        rowItems.add((start until end).toList())
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rowItems.forEach { rowIdxs ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowIdxs.forEach { i ->
                    Dice3DCube(value = values.getOrElse(i) { 1 }, isShaking = isShaking, idx = i)
                }
            }
        }
    }
}

// 单颗真 3D 立方体骰子 - 6个面拼成立方体
@Composable
private fun Dice3DCube(value: Int, isShaking: Boolean, idx: Int) {
    val density = LocalDensity.current
    val sizeDp = 56.dp
    val halfPx = with(density) { sizeDp.toPx() / 2f }
    val cam = 16f * density.density
    
    val infinite = rememberInfiniteTransition(label = "cube$idx")
    // 持续 3D 旋转动画（X+Y 轴），不同骰子速度不同
    val cubeRotX by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(700 + idx * 70, easing = LinearEasing)),
        label = "crx$idx"
    )
    val cubeRotY by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(850 + idx * 90, easing = LinearEasing)),
        label = "cry$idx"
    )
    // 静态时的微微浮动（让 IDLE/REVEALED 时也有活力）
    val idleRotY by infinite.animateFloat(
        initialValue = -18f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(2500, easing = EaseInOut), RepeatMode.Reverse),
        label = "idle$idx"
    )
    
    // 骰子相对面之和为 7（标准骰子规则）
    val faceFront = value
    val faceBack = 7 - value
    val faceTop = 2
    val faceBottom = 5
    val faceRight = 3
    val faceLeft = 4
    
    Box(
        modifier = Modifier
            .size(sizeDp)
            .graphicsLayer {
                cameraDistance = cam
                if (isShaking) {
                    rotationX = cubeRotX
                    rotationY = cubeRotY
                } else {
                    rotationX = -22f  // 稍微俯视角度让 3D 立体感更明显
                    rotationY = idleRotY
                }
            }
    ) {
        // 前面 (Front - 默认位置)
        CubeFace(faceFront, cam, Modifier
            .size(sizeDp)
            .graphicsLayer {
                cameraDistance = cam
            }
        )
        // 后面 (Back - rotationY 180°)
        CubeFace(faceBack, cam, Modifier
            .size(sizeDp)
            .graphicsLayer {
                cameraDistance = cam
                rotationY = 180f
            }
        )
        // 上面 (Top) - rotationX -90° 让面变水平朝上, translationY 推到上方
        CubeFace(faceTop, cam, Modifier
            .size(sizeDp)
            .graphicsLayer {
                cameraDistance = cam
                rotationX = -90f
                translationY = -halfPx
            }
        )
        // 下面 (Bottom)
        CubeFace(faceBottom, cam, Modifier
            .size(sizeDp)
            .graphicsLayer {
                cameraDistance = cam
                rotationX = 90f
                translationY = halfPx
            }
        )
        // 左面 (Left)
        CubeFace(faceLeft, cam, Modifier
            .size(sizeDp)
            .graphicsLayer {
                cameraDistance = cam
                rotationY = 90f
                translationX = -halfPx
            }
        )
        // 右面 (Right)
        CubeFace(faceRight, cam, Modifier
            .size(sizeDp)
            .graphicsLayer {
                cameraDistance = cam
                rotationY = -90f
                translationX = halfPx
            }
        )
    }
}

// 立方体的单一面
@Composable
private fun CubeFace(value: Int, cam: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val cr = androidx.compose.ui.geometry.CornerRadius(w * 0.16f)
        // 阴影底
        drawRoundRect(
            color = Color(0xFFD0D0D0),
            topLeft = Offset(2f, 4f),
            size = Size(w - 4f, h - 4f),
            cornerRadius = cr
        )
        // 主体白底 + 立体渐变
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.White, Color(0xFFF5F5F5), Color(0xFFE8E8E8))
            ),
            size = Size(w, h),
            cornerRadius = cr
        )
        // 边框 - 粉色装饰
        drawRoundRect(
            color = Color(0xFFFF80AB),
            size = Size(w, h),
            cornerRadius = cr,
            style = Stroke(width = 2.5f)
        )
        // 内描边深粉
        drawRoundRect(
            color = Color(0xFFEC407A).copy(alpha = 0.3f),
            topLeft = Offset(3f, 3f),
            size = Size(w - 6f, h - 6f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.13f),
            style = Stroke(width = 1f)
        )
        // 点位
        drawDicePips(value, w, h)
        // 顶部高光（模拟 3D 立体光照）
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.5f), Color.Transparent),
                startY = 0f, endY = h * 0.4f
            ),
            topLeft = Offset(w * 0.12f, w * 0.10f),
            size = Size(w * 0.40f, h * 0.25f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.10f)
        )
    }
}

private fun DrawScope.drawDicePips(value: Int, w: Float, h: Float) {
    val r = w * 0.09f
    val pipColor = Color(0xFFD81B60)
    val cx = w / 2; val cy = h / 2
    val left = w * 0.28f; val right = w * 0.72f
    val top = h * 0.28f; val bottom = h * 0.72f
    fun pip(x: Float, y: Float) {
        drawCircle(pipColor, r, Offset(x, y))
        drawCircle(Color(0xFF880E4F).copy(alpha = 0.6f), r * 0.4f, Offset(x + r * 0.3f, y + r * 0.3f))
    }
    when (value) {
        1 -> pip(cx, cy)
        2 -> { pip(left, top); pip(right, bottom) }
        3 -> { pip(left, top); pip(cx, cy); pip(right, bottom) }
        4 -> { pip(left, top); pip(right, top); pip(left, bottom); pip(right, bottom) }
        5 -> { pip(left, top); pip(right, top); pip(cx, cy); pip(left, bottom); pip(right, bottom) }
        6 -> { pip(left, top); pip(right, top); pip(left, cy); pip(right, cy); pip(left, bottom); pip(right, bottom) }
    }
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

