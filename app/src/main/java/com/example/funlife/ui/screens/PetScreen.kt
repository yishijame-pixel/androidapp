// PetScreen.kt - 宠物主页
package com.example.funlife.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.funlife.data.model.*
import com.example.funlife.ui.components.FrameAnimation
import com.example.funlife.ui.components.PetAnimationFrames
import com.example.funlife.ui.components.PetWalkingAnimation
import com.example.funlife.utils.PetImageLoader
import com.example.funlife.utils.rememberAssetImage
import com.example.funlife.viewmodel.PetViewModel
import com.example.funlife.viewmodel.AnimationState
import kotlinx.coroutines.delay

@Composable
fun PetScreen(
    navController: NavController,
    viewModel: PetViewModel
) {
    val pet by viewModel.pet.collectAsState()
    val animationState by viewModel.animationState.collectAsState()
    val coins by viewModel.userCoins.collectAsState()
    val missions by viewModel.missions.collectAsState()
    val items by viewModel.items.collectAsState()
    val toast by viewModel.toast.collectAsState()
    
    var showShopDialog by remember { mutableStateOf(false) }
    var showInventoryDialog by remember { mutableStateOf(false) }
    var showMissionsDialog by remember { mutableStateOf(false) }
    
    // 🎯 Toast 反馈
    val context = LocalContext.current
    LaunchedEffect(toast) {
        toast?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
    }
    
    when {
        pet == null -> {
            AdoptPetScreen(
                onAdopt = { name, type -> viewModel.createPet(name, type) },
                onBack = { navController.popBackStack() }
            )
        }
        else -> {
            PetHomeScreen(
                pet = pet!!,
                animationState = animationState,
                coins = coins?.coins ?: 0,
                missions = missions,
                onFeed = {
                    // 有高级食物先用高级，没有就用普通免费食物
                    val premium = items.firstOrNull {
                        it.itemId == PetItems.PREMIUM_FOOD.id || it.itemId == PetItems.SNACK.id
                    }
                    if (premium != null) viewModel.feedPet(premium.itemId)
                    else viewModel.feedPet(PetItems.BASIC_FOOD.id)
                },
                onClean = { viewModel.cleanPet() },
                onPlay = { viewModel.playWithPet() },
                onPet = { viewModel.petPet() },
                onShop = { showShopDialog = true },
                onInventory = { showInventoryDialog = true },
                onMissions = { showMissionsDialog = true },
                onBack = { navController.popBackStack() }
            )
        }
    }
    
    if (showShopDialog) {
        PetShopDialog(
            onDismiss = { showShopDialog = false },
            onPurchase = { item ->
                viewModel.purchaseShopItem(item.id, item.name, item.type, item.price)
            }
        )
    }
    if (showInventoryDialog) {
        PetInventoryDialog(
            items = items,
            onDismiss = { showInventoryDialog = false },
            onUseFood = { id -> viewModel.feedPet(id); showInventoryDialog = false }
        )
    }
    if (showMissionsDialog) {
        PetMissionsDialog(
            missions = missions,
            onDismiss = { showMissionsDialog = false },
            onClaim = { viewModel.claimMission(it) }
        )
    }
}

@Composable
fun PetHomeScreen(
    pet: Pet,
    animationState: AnimationState,
    coins: Int,
    missions: List<com.example.funlife.utils.PetMissionHelper.MissionState>,
    onFeed: () -> Unit,
    onClean: () -> Unit,
    onPlay: () -> Unit,
    onPet: () -> Unit,
    onShop: () -> Unit,
    onInventory: () -> Unit,
    onMissions: () -> Unit,
    onBack: () -> Unit
) {
    // 加载背景图片
    val backgroundImage = rememberAssetImage(PetImageLoader.getBackgroundPath("home"))
    
    // 获取背景音乐管理器并播放音乐
    val context = LocalContext.current
    val musicManager = remember { com.example.funlife.utils.BackgroundMusicManager.getInstance(context) }
    
    // 进入页面时播放背景音乐，离开时停止
    DisposableEffect(Unit) {
        musicManager.playPetMusic()
        onDispose {
            musicManager.stop()
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 背景图片
        if (backgroundImage != null) {
            Image(
                bitmap = backgroundImage,
                contentDescription = "背景",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // 备用渐变背景
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFE4E1),
                                Color(0xFFE0F7FA),
                                Color(0xFFFFF0F5)
                            )
                        )
                    )
            )
        }
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 顶部栏 - 含金币、任务、背包入口
            PetTopBarPro(
                petName = pet.name,
                coins = coins,
                missionUnclaimed = missions.count { it.completed && !it.claimed },
                onBack = onBack,
                onMissions = onMissions,
                onInventory = onInventory
            )
            
            // 状态栏
            PetStatusBar(pet = pet)
            
            // 每日任务条
            DailyMissionStrip(missions = missions, onClick = onMissions)
            
            // 宠物展示区
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // 宠物形象
                PetCharacter(
                    pet = pet,
                    animationState = animationState,
                    onPet = onPet
                )
            }
            
            // 等级信息
            PetLevelInfo(pet = pet)
            
            // 操作按钮
            PetActionButtons(
                onFeed = onFeed,
                onClean = onClean,
                onPlay = onPlay,
                onShop = onShop
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetTopBar(
    petName: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        // 左上角返回按钮
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(40.dp)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "返回",
                tint = Color(0xFF5D4037),
                modifier = Modifier.size(24.dp)
            )
        }
        
        // 中间宠物名字（可选）
        Text(
            text = petName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5D4037),
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun PetStatusBar(pet: Pet) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatusIndicator(
            icon = "🍖",
            value = pet.hungerValue,
            color = Color(0xFFFF6B9D),
            label = "饥饿"
        )
        StatusIndicator(
            icon = "💧",
            value = pet.cleanValue,
            color = Color(0xFF4ECDC4),
            label = "清洁"
        )
        StatusIndicator(
            icon = "😊",
            value = pet.moodValue,
            color = Color(0xFFFFD700),
            label = "心情"
        )
        StatusIndicator(
            icon = "🏥",
            value = pet.healthValue,
            color = Color(0xFF4CAF50),
            label = "健康"
        )
    }
}

@Composable
fun StatusIndicator(
    icon: String,
    value: Int,
    color: Color,
    label: String
) {
    val isLow = value < 30
    // 低值脉冲警告
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = if (isLow) 0.9f else 1f,
        targetValue = if (isLow) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val actualColor = if (isLow) Color(0xFFFF3B30) else color
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp)
    ) {
        Text(text = icon, fontSize = 22.sp, modifier = Modifier.scale(pulse))
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(52.dp).scale(pulse)
        ) {
            // 背景圆
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(actualColor.copy(alpha = 0.12f), CircleShape)
            )
            CircularProgressIndicator(
                progress = value / 100f,
                modifier = Modifier.fillMaxSize(),
                color = actualColor,
                strokeWidth = 5.dp,
                trackColor = actualColor.copy(alpha = 0.15f)
            )
            Text(
                text = "$value",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = actualColor
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (isLow) "$label ⚠️" else label,
            fontSize = 10.sp,
            color = if (isLow) Color(0xFFFF3B30) else Color.Gray,
            fontWeight = if (isLow) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// 待机动作类型
enum class IdleAction {
    WALKING,      // 行走
    BREATHING,    // 呼吸+摇摆
    LICKING,      // 舔爪子
    HAPPY         // 开心动作
}

// 加权随机选择待机动作（行走权重更高）
fun getRandomIdleAction(): IdleAction {
    val random = (1..10).random()
    return when (random) {
        in 1..5 -> IdleAction.WALKING      // 50% 概率行走
        in 6..7 -> IdleAction.BREATHING    // 20% 概率呼吸
        in 8..9 -> IdleAction.LICKING      // 20% 概率舔爪子
        else -> IdleAction.HAPPY           // 10% 概率开心
    }
}

@Composable
fun PetCharacter(
    pet: Pet,
    animationState: AnimationState,
    onPet: () -> Unit
) {
    val context = LocalContext.current
    
    // 当前待机动作 - 初始加权随机选择（行走概率更高）
    var currentIdleAction by remember { mutableStateOf(getRandomIdleAction()) }
    
    // 待机状态下随机切换动作
    LaunchedEffect(animationState) {
        if (animationState == AnimationState.Idle) {
            while (true) {
                // 每个动作持续2-5秒
                val duration = (2000L..5000L).random()
                delay(duration)
                
                // 加权随机选择下一个动作（行走概率50%）
                currentIdleAction = getRandomIdleAction()
            }
        } else {
            // 非待机状态时，重置为随机动作，以便下次进入待机时有变化
            currentIdleAction = getRandomIdleAction()
        }
    }
    
    // 判断是否应该显示行走动画（待机状态且当前动作是行走）
    val shouldWalk = animationState == AnimationState.Idle && currentIdleAction == IdleAction.WALKING
    
    // 判断是否应该显示帧动画（待机状态且当前动作是舔爪子或开心）
    val shouldPlayIdleFrameAnimation = animationState == AnimationState.Idle && 
        (currentIdleAction == IdleAction.LICKING || currentIdleAction == IdleAction.HAPPY)
    
    when {
        // 🐼 熊猫使用纯 Canvas 绘制，独立的动画路径
        pet.type == PetType.PANDA -> {
            com.example.funlife.ui.components.PandaPet(
                animationState = animationState,
                onClick = onPet,
                modifier = Modifier
            )
        }
        shouldWalk -> {
            // 显示行走动画
            PetWalkingAnimation(
                petType = pet.type,
                size = 200.dp,
                autoWalk = true,
                walkSpeed = 0.3f,
                idleTime = 3000L
            )
        }
        shouldPlayIdleFrameAnimation -> {
            // 显示待机帧动画（舔爪子或开心）
            PetIdleFrameAnimation(
                pet = pet,
                idleAction = currentIdleAction,
                onPet = onPet
            )
        }
        else -> {
            // 显示其他动画状态（包括呼吸+摇摆的待机状态）
            PetStaticCharacter(
                pet = pet,
                animationState = animationState,
                onPet = onPet
            )
        }
    }
}

// 待机帧动画（舔爪子、开心等）
@Composable
fun PetIdleFrameAnimation(
    pet: Pet,
    idleAction: IdleAction,
    onPet: () -> Unit
) {
    // 呼吸动画
    val breathScale by rememberInfiniteTransition(label = "breath").animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )
    
    // 获取帧动画序列
    val animationFrames = remember(idleAction) {
        when (idleAction) {
            IdleAction.LICKING -> PetAnimationFrames.getLickingFrames()
            IdleAction.HAPPY -> PetAnimationFrames.getHappyFrames()
            else -> emptyList()
        }
    }
    
    Box(
        modifier = Modifier
            .size(250.dp),
        contentAlignment = Alignment.Center
    ) {
        // 宠物阴影
        Box(
            modifier = Modifier
                .size(120.dp, 20.dp)
                .offset(y = 100.dp)
                .scale(breathScale * 0.8f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // 宠物形象
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(breathScale)
                .clickable(
                    onClick = onPet,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (animationFrames.isNotEmpty()) {
                FrameAnimation(
                    frames = animationFrames,
                    frameDuration = when (idleAction) {
                        IdleAction.LICKING -> 150
                        IdleAction.HAPPY -> 300
                        else -> 100
                    },
                    loop = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun PetStaticCharacter(
    pet: Pet,
    animationState: AnimationState,
    onPet: () -> Unit
) {
    val context = LocalContext.current
    
    // 呼吸动画
    val breathScale by rememberInfiniteTransition(label = "breath").animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )
    
    // 轻微摇摆动画
    val swingRotation by rememberInfiniteTransition(label = "swing").animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swingRotation"
    )
    
    // 根据动画状态调整缩放
    val actionScale by animateFloatAsState(
        targetValue = when (animationState) {
            AnimationState.Feeding -> 1.15f
            AnimationState.Cleaning -> 1.08f
            AnimationState.Playing -> 1.2f
            AnimationState.Petting -> 1.1f
            AnimationState.LevelUp -> 1.3f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "actionScale"
    )
    
    // 弹跳动画（开心时）
    val bounceOffset by animateFloatAsState(
        targetValue = when (animationState) {
            AnimationState.Playing -> -30f
            AnimationState.LevelUp -> -40f
            else -> 0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounceOffset"
    )
    
    // 判断是否需要播放帧动画
    val shouldPlayFrameAnimation = remember(animationState) {
        PetImageLoader.shouldPlayFrameAnimation(animationState)
    }
    
    // 获取帧动画序列
    val animationFrames = remember(animationState) {
        when (animationState) {
            AnimationState.Feeding -> PetAnimationFrames.getEatingFrames()
            AnimationState.Petting -> PetAnimationFrames.getLickingFrames()
            AnimationState.Playing -> PetAnimationFrames.getHappyFrames()
            else -> emptyList()
        }
    }
    
    // 获取静态宠物图片路径
    val petImagePath = remember(pet.type, pet.getGrowthStage(), animationState) {
        PetImageLoader.getPetImagePath(pet.type, pet.getGrowthStage(), animationState)
    }
    
    // 加载静态图片
    val petImage = rememberAssetImage(petImagePath)
    
    Box(
        modifier = Modifier
            .size(250.dp)
            .offset(y = bounceOffset.dp),
        contentAlignment = Alignment.Center
    ) {
        // 宠物阴影
        Box(
            modifier = Modifier
                .size(120.dp, 20.dp)
                .offset(y = 100.dp)
                .scale(breathScale * 0.8f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // 宠物形象
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(breathScale * actionScale)  // 始终保持呼吸动画
                .rotate(if (shouldPlayFrameAnimation && animationState != AnimationState.Idle) 0f else swingRotation * 0.5f)  // 待机时保持摇摆
                .clickable(
                    onClick = onPet,
                    indication = null,  // 移除点击波纹效果
                    interactionSource = remember { MutableInteractionSource() }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (shouldPlayFrameAnimation && animationFrames.isNotEmpty()) {
                // 播放帧动画
                FrameAnimation(
                    frames = animationFrames,
                    frameDuration = when (animationState) {
                        AnimationState.Idle -> 5000     // 睡觉动画5秒一帧
                        AnimationState.Feeding -> 80    // 吃东西快一点
                        AnimationState.Petting -> 150   // 舔爪子慢一点
                        AnimationState.Playing -> 300   // 开心动画慢一点
                        else -> 100
                    },
                    loop = true,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // 显示静态图片
                if (petImage != null) {
                    Image(
                        bitmap = petImage,
                        contentDescription = "宠物",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // 加载失败时显示占位符
                    Text(
                        text = when (pet.type) {
                            PetType.CAT -> "🐱"
                            PetType.DOG -> "🐶"
                            PetType.RABBIT -> "🐰"
                            PetType.HAMSTER -> "🐹"
                            PetType.TIGER -> "🐯"
                            PetType.PANDA -> "🐼"
                        },
                        fontSize = 120.sp
                    )
                }
            }
        }
        
        // 显示动画特效
        AnimatedVisibility(
            visible = animationState != AnimationState.Idle,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                // 粒子特效和玩具
                when (animationState) {
                    AnimationState.Feeding -> FloatingHearts()
                    AnimationState.Cleaning -> WaterDropEffect()
                    AnimationState.Playing -> {
                        // 玩耍时显示球玩具和星星特效
                        Box(modifier = Modifier.fillMaxSize()) {
                            PlayingWithBall()
                            FloatingStarsWithAnimation()
                        }
                    }
                    AnimationState.Petting -> FloatingLoveHearts()
                    AnimationState.LevelUp -> LevelUpEffect()
                    else -> {}
                }
            }
        }
    }
}

// 漂浮星星特效（使用帧动画）
@Composable
fun FloatingStarsWithAnimation() {
    val stars = remember { List(3) { it } }
    stars.forEach { index ->
        FloatingParticleWithFrames(
            frames = PetAnimationFrames.getStarFrames(),
            delay = index * 200,
            offsetX = (index - 1) * 40f
        )
    }
}

// 玩耍动画 - 显示球玩具
@Composable
fun PlayingWithBall() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(y = 80.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 弹跳的球
        BouncingBall()
    }
}

// 弹跳的球动画
@Composable
fun BouncingBall() {
    val infiniteTransition = rememberInfiniteTransition(label = "ball")
    
    // 上下弹跳
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -60f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ballBounce"
    )
    
    // 旋转
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ballRotation"
    )
    
    Box(
        modifier = Modifier
            .size(50.dp)
            .offset(y = offsetY.dp)
            .rotate(rotation)
    ) {
        FrameAnimation(
            frames = PetAnimationFrames.getBallFrames(),
            frameDuration = 150,
            loop = true,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// 通用漂浮粒子（帧动画版本）
@Composable
fun FloatingParticleWithFrames(
    frames: List<String>,
    delay: Int = 0,
    offsetX: Float = 0f
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        visible = true
    }
    
    val offsetY by animateFloatAsState(
        targetValue = if (visible) -150f else 0f,
        animationSpec = tween(1500, easing = EaseOut),
        label = "offsetY"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 0f else 1f,
        animationSpec = tween(1500),
        label = "alpha"
    )
    
    if (visible) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .offset(x = offsetX.dp, y = offsetY.dp)
                .alpha(alpha)
        ) {
            FrameAnimation(
                frames = frames,
                frameDuration = 150,
                loop = true,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// 漂浮爱心特效
@Composable
fun FloatingHearts() {
    val hearts = remember { List(5) { it } }
    hearts.forEach { index ->
        FloatingParticle(
            emoji = "❤️",
            delay = index * 200,
            offsetX = (index - 2) * 20f
        )
    }
}

// 漂浮星星特效
@Composable
fun FloatingStars() {
    val stars = remember { List(6) { it } }
    stars.forEach { index ->
        FloatingParticle(
            emoji = "⭐",
            delay = index * 150,
            offsetX = (index - 3) * 25f
        )
    }
}

// 漂浮闪光特效
@Composable
fun FloatingSparkles() {
    val sparkles = remember { List(8) { it } }
    sparkles.forEach { index ->
        FloatingParticle(
            emoji = "✨",
            delay = index * 100,
            offsetX = (index - 4) * 20f
        )
    }
}

// 水滴下落特效（洗澡时）
@Composable
fun WaterDropEffect() {
    val drops = remember { List(12) { it } }
    drops.forEach { index ->
        WaterDrop(
            delay = index * 80,
            offsetX = (index - 6) * 15f
        )
    }
}

// 单个水滴
@Composable
fun WaterDrop(
    delay: Int = 0,
    offsetX: Float = 0f
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        visible = true
    }
    
    // 下落动画
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 200f else -50f,
        animationSpec = tween(800, easing = EaseIn),
        label = "dropOffsetY"
    )
    
    // 透明度动画（落到底部时消失）
    val alpha by animateFloatAsState(
        targetValue = if (visible && offsetY < 150f) 1f else 0f,
        animationSpec = tween(400),
        label = "dropAlpha"
    )
    
    // 大小变化（落下时变大）
    val scale by animateFloatAsState(
        targetValue = if (visible) 1.2f else 0.8f,
        animationSpec = tween(800),
        label = "dropScale"
    )
    
    if (visible) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .offset(x = offsetX.dp, y = offsetY.dp)
                .scale(scale)
                .alpha(alpha),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "💧",
                fontSize = 24.sp
            )
        }
    }
    
    // 循环播放
    LaunchedEffect(offsetY) {
        if (offsetY >= 200f) {
            kotlinx.coroutines.delay(100)
            visible = false
            kotlinx.coroutines.delay(delay.toLong())
            visible = true
        }
    }
}

// 漂浮爱心（抚摸）
@Composable
fun FloatingLoveHearts() {
    val hearts = remember { List(3) { it } }
    hearts.forEach { index ->
        FloatingParticle(
            emoji = "💕",
            delay = index * 250,
            offsetX = (index - 1) * 30f
        )
    }
}

// 升级特效
@Composable
fun LevelUpEffect() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 光芒效果
        val infiniteTransition = rememberInfiniteTransition(label = "levelup")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )
        
        Text(
            text = "✨",
            fontSize = 60.sp,
            modifier = Modifier.rotate(rotation)
        )
        
        // 漂浮星星
        FloatingStars()
    }
}

// 通用漂浮粒子
@Composable
fun FloatingParticle(
    emoji: String,
    delay: Int = 0,
    offsetX: Float = 0f
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        visible = true
    }
    
    val offsetY by animateFloatAsState(
        targetValue = if (visible) -150f else 0f,
        animationSpec = tween(1500, easing = EaseOut),
        label = "offsetY"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 0f else 1f,
        animationSpec = tween(1500),
        label = "alpha"
    )
    
    if (visible) {
        Text(
            text = emoji,
            fontSize = 30.sp,
            modifier = Modifier
                .offset(x = offsetX.dp, y = offsetY.dp)
                .alpha(alpha)
        )
    }
}

@Composable
fun PetLevelInfo(pet: Pet) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.8f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 等级徽章
            Surface(
                shape = CircleShape,
                color = Color(0xFFFFD700),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Lv.${pet.level}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            
            // 经验进度条
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "${pet.experience} / ${pet.getExpForNextLevel()}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = pet.experience.toFloat() / pet.getExpForNextLevel(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFFFFD700),
                    trackColor = Color(0xFFFFD700).copy(alpha = 0.2f)
                )
            }
            
            // 成长阶段
            Text(
                text = when (pet.getGrowthStage()) {
                    GrowthStage.BABY -> "幼年期"
                    GrowthStage.CHILD -> "少年期"
                    GrowthStage.ADULT -> "成年期"
                    GrowthStage.PERFECT -> "完全体"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6B9D)
            )
        }
    }
}

@Composable
fun PetActionButtons(
    onFeed: () -> Unit,
    onClean: () -> Unit,
    onPlay: () -> Unit,
    onShop: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .padding(bottom = 80.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActionButton(
            icon = Icons.Default.Restaurant,
            label = "喂食",
            color = Color(0xFFFF6B9D),
            onClick = onFeed
        )
        ActionButton(
            icon = Icons.Default.Shower,
            label = "洗澡",
            color = Color(0xFF4ECDC4),
            onClick = onClean
        )
        ActionButton(
            icon = Icons.Default.SportsEsports,
            label = "玩耍",
            color = Color(0xFFFFD700),
            onClick = onPlay
        )
        ActionButton(
            icon = Icons.Default.ShoppingCart,
            label = "商店",
            color = Color(0xFF9C27B0),
            onClick = onShop
        )
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "buttonScale"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clickable(
                onClick = {
                    pressed = true
                    onClick()
                },
                indication = null,  // 移除点击波纹效果
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(60.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
    
    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(100)
            pressed = false
        }
    }
}

@Composable
fun AdoptPetScreen(
    onAdopt: (String, PetType) -> Unit,
    onBack: () -> Unit
) {
    var petName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<PetType?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    
    // 加载背景图片
    val backgroundImage = rememberAssetImage(PetImageLoader.getBackgroundPath("garden"))
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 背景图片
        if (backgroundImage != null) {
            Image(
                bitmap = backgroundImage,
                contentDescription = "背景",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.7f
            )
        } else {
            // 备用渐变背景
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFE4E1),
                                Color(0xFFE0F7FA),
                                Color(0xFFFFF0F5)
                            )
                        )
                    )
            )
        }
        
        // 左上角返回按钮
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
                .size(40.dp)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "返回",
                tint = Color(0xFF5D4037),
                modifier = Modifier.size(24.dp)
            )
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.95f)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "领养你的宠物",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "选择一只宠物开始你的养成之旅！",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // 宠物选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PetTypeButton(
                        emoji = "🐱",
                        type = PetType.CAT,
                        selected = selectedType == PetType.CAT,
                        onClick = { selectedType = PetType.CAT }
                    )
                    PetTypeButton(
                        emoji = "🐶",
                        type = PetType.DOG,
                        selected = selectedType == PetType.DOG,
                        onClick = { selectedType = PetType.DOG }
                    )
                    PetTypeButton(
                        emoji = "🐰",
                        type = PetType.RABBIT,
                        selected = selectedType == PetType.RABBIT,
                        onClick = { selectedType = PetType.RABBIT }
                    )
                    PetTypeButton(
                        emoji = "�",
                        type = PetType.PANDA,
                        selected = selectedType == PetType.PANDA,
                        onClick = { selectedType = PetType.PANDA }
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { showDialog = true },
                    enabled = selectedType != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("领养", modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
    
    if (showDialog && selectedType != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("给宠物起个名字") },
            text = {
                OutlinedTextField(
                    value = petName,
                    onValueChange = { petName = it },
                    label = { Text("宠物名字") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (petName.isNotBlank()) {
                            onAdopt(petName, selectedType!!)
                            showDialog = false
                        }
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun PetTypeButton(
    emoji: String,
    type: PetType,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Color(0xFFFF6B9D) else Color.LightGray.copy(alpha = 0.3f),
        modifier = Modifier
            .size(70.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // 🐼 熊猫用 Canvas 绘制（避免设备字体不支持 emoji）
            if (type == PetType.PANDA) {
                com.example.funlife.ui.components.MiniPandaIcon(
                    modifier = Modifier.size(50.dp)
                )
            } else {
                Text(
                    text = emoji,
                    fontSize = 40.sp
                )
            }
        }
    }
}


// 宠物道具数据类
data class PetShopItem(
    val id: String,
    val name: String,
    val description: String,
    val imagePath: String,
    val price: Int,
    val type: String  // "food" 或 "toy"
)

// 宠物商城对话框
@Composable
fun PetShopDialog(
    onDismiss: () -> Unit,
    onPurchase: (PetShopItem) -> Unit
) {
    val context = LocalContext.current
    
    // 定义商城道具列表
    val shopItems = remember {
        listOf(
            // 食物类 - 基础猫粮
            PetShopItem(
                id = "pet_foot",
                name = "普通猫粮",
                description = "基础食物，恢复少量饱食度",
                imagePath = "pet/daoju/pet_foot.png",
                price = 10,
                type = "food"
            ),
            // 食物类 - 狗粮系列
            PetShopItem(
                id = "dog_foot",
                name = "狗粮",
                description = "适合狗狗的美味食物",
                imagePath = "pet/daoju/dog_foot.png",
                price = 15,
                type = "food"
            ),
            PetShopItem(
                id = "dog_foot_01",
                name = "优质狗粮",
                description = "更美味的狗粮，营养更丰富",
                imagePath = "pet/daoju/dog_foot_01.png",
                price = 18,
                type = "food"
            ),
            PetShopItem(
                id = "dog_foot_02",
                name = "特级狗粮",
                description = "特制配方，狗狗最爱",
                imagePath = "pet/daoju/dog_foot_02.png",
                price = 22,
                type = "food"
            ),
            PetShopItem(
                id = "dog_foot_03",
                name = "顶级狗粮",
                description = "顶级食材，恢复大量饱食度",
                imagePath = "pet/daoju/dog_foot_03.png",
                price = 28,
                type = "food"
            ),
            // 食物类 - 高级猫粮系列
            PetShopItem(
                id = "high_foot",
                name = "高级猫粮",
                description = "营养丰富，恢复大量饱食度",
                imagePath = "pet/daoju/high_foot.png",
                price = 30,
                type = "food"
            ),
            PetShopItem(
                id = "high_foot_01",
                name = "豪华猫粮",
                description = "顶级配方，猫咪的最爱",
                imagePath = "pet/daoju/high_foot_01.png",
                price = 35,
                type = "food"
            ),
            // 食物类 - 鱼罐头系列
            PetShopItem(
                id = "yu_foot",
                name = "鱼罐头",
                description = "猫咪最爱的鱼罐头",
                imagePath = "pet/daoju/yu_foot.png",
                price = 25,
                type = "food"
            ),
            PetShopItem(
                id = "yu_foot_01",
                name = "金枪鱼罐头",
                description = "新鲜金枪鱼制作，美味可口",
                imagePath = "pet/daoju/yu_foot_01.png",
                price = 28,
                type = "food"
            ),
            PetShopItem(
                id = "yu_foot_02",
                name = "三文鱼罐头",
                description = "营养丰富的三文鱼",
                imagePath = "pet/daoju/yu_foot_02.png",
                price = 32,
                type = "food"
            ),
            PetShopItem(
                id = "yu_foot_03",
                name = "豪华海鲜罐头",
                description = "多种海鲜混合，极致美味",
                imagePath = "pet/daoju/yu_foot_03.png",
                price = 38,
                type = "food"
            ),
            // 玩具类 - 小球系列
            PetShopItem(
                id = "qiu",
                name = "小球",
                description = "可爱的玩具球，增加快乐值",
                imagePath = "pet/daoju/qiu.png",
                price = 20,
                type = "toy"
            ),
            PetShopItem(
                id = "qiu_01",
                name = "彩色小球",
                description = "五彩缤纷，吸引宠物注意",
                imagePath = "pet/daoju/qiu_01.png",
                price = 22,
                type = "toy"
            ),
            PetShopItem(
                id = "qiu_02",
                name = "弹力球",
                description = "弹性十足，玩耍更有趣",
                imagePath = "pet/daoju/qiu_02.png",
                price = 25,
                type = "toy"
            ),
            PetShopItem(
                id = "qiu_03",
                name = "发光球",
                description = "夜晚也能玩，会发光的球",
                imagePath = "pet/daoju/qiu_03.png",
                price = 28,
                type = "toy"
            ),
            PetShopItem(
                id = "qiu_04",
                name = "智能互动球",
                description = "自动滚动，智能互动玩具",
                imagePath = "pet/daoju/qiu_04.png",
                price = 35,
                type = "toy"
            )
        )
    }
    
    var selectedCategory by remember { mutableStateOf("all") }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF8E1)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 标题栏
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFF6B9D),
                                    Color(0xFFFF8FB3)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🛒 宠物商城",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = Color.White
                            )
                        }
                    }
                }
                
                // 分类标签
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryChip(
                        label = "全部",
                        selected = selectedCategory == "all",
                        onClick = { selectedCategory = "all" }
                    )
                    CategoryChip(
                        label = "食物",
                        selected = selectedCategory == "food",
                        onClick = { selectedCategory = "food" }
                    )
                    CategoryChip(
                        label = "玩具",
                        selected = selectedCategory == "toy",
                        onClick = { selectedCategory = "toy" }
                    )
                }
                
                // 商品列表
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    val filteredItems = if (selectedCategory == "all") {
                        shopItems
                    } else {
                        shopItems.filter { it.type == selectedCategory }
                    }
                    
                    items(filteredItems.size) { index ->
                        val item = filteredItems[index]
                        PetShopItemCard(
                            item = item,
                            onPurchase = { onPurchase(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Color(0xFFFF6B9D) else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) Color.Transparent else Color(0xFFFF6B9D)
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (selected) Color.White else Color(0xFFFF6B9D),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun PetShopItemCard(
    item: PetShopItem,
    onPurchase: () -> Unit
) {
    val context = LocalContext.current
    val itemImage = remember(item.imagePath) {
        com.example.funlife.utils.ImageCache.loadImage(context, item.imagePath, sampleSize = 2)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 商品图片
            if (itemImage != null) {
                Image(
                    bitmap = itemImage,
                    contentDescription = item.name,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF3E0)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF3E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎁", fontSize = 32.sp)
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 商品信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5D4037)
                )
                Text(
                    text = item.description,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("💰", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${item.price}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B9D)
                    )
                }
            }
            
            // 购买按钮
            Button(
                onClick = onPurchase,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6B9D)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text(
                    "购买",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// 🎮 增强版顶部栏 - 含金币、任务红点、背包入口
// ═══════════════════════════════════════════════════════════════════
@Composable
fun PetTopBarPro(
    petName: String,
    coins: Int,
    missionUnclaimed: Int,
    onBack: () -> Unit,
    onMissions: () -> Unit,
    onInventory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 返回 + 宠物名字
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(40.dp).clickable(onClick = onBack)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.ArrowBack, "返回",
                        tint = Color(0xFF5D4037),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.85f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🐾 ", fontSize = 16.sp)
                    Text(petName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D4037))
                }
            }
        }
        // 金币 + 任务 + 背包
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 金币显示
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFFFF3C7),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD700))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💰", fontSize = 14.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("$coins", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB8860B))
                }
            }
            // 任务按钮（含红点）
            Box {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(40.dp).clickable(onClick = onMissions)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("📋", fontSize = 18.sp)
                    }
                }
                if (missionUnclaimed > 0) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.TopEnd)
                            .background(Color(0xFFFF3B30), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$missionUnclaimed", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            // 背包按钮
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(40.dp).clickable(onClick = onInventory)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🎒", fontSize = 18.sp)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// 📋 每日任务条 - 横向显示任务进度
// ═══════════════════════════════════════════════════════════════════
@Composable
fun DailyMissionStrip(
    missions: List<com.example.funlife.utils.PetMissionHelper.MissionState>,
    onClick: () -> Unit
) {
    if (missions.isEmpty()) return
    val claimable = missions.count { it.completed && !it.claimed }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("📋", fontSize = 18.sp)
            Text(
                if (claimable > 0) "每日任务 · $claimable 项可领取" else "每日任务",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (claimable > 0) Color(0xFFE53935) else Color(0xFF5D4037)
            )
            Spacer(Modifier.weight(1f))
            missions.forEach { m ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            if (m.claimed) Color(0xFFE0E0E0)
                            else if (m.completed) Color(0xFFFFD700)
                            else Color(0xFFFFF0F0),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (m.claimed) "✓" else m.type.icon,
                        fontSize = if (m.claimed) 14.sp else 14.sp,
                        color = if (m.claimed) Color.Gray else Color(0xFF5D4037)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// 📋 每日任务详细对话框 - 显示进度并可领取奖励
// ═══════════════════════════════════════════════════════════════════
@Composable
fun PetMissionsDialog(
    missions: List<com.example.funlife.utils.PetMissionHelper.MissionState>,
    onDismiss: () -> Unit,
    onClaim: (com.example.funlife.utils.PetMissionHelper.MissionType) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📋 每日任务", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D4037))
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "关闭", tint = Color.Gray) }
                }
                Text("陪伴宠物，完成任务领取金币奖励", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                missions.forEach { m ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFFFF3C7), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(m.type.icon, fontSize = 22.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(m.type.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D4037))
                                Text(
                                    "${m.progress}/${m.type.target} · 奖励 ${m.type.reward} 💰",
                                    fontSize = 11.sp, color = Color.Gray
                                )
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = m.percent,
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = if (m.completed) Color(0xFFFFD700) else Color(0xFFFF6B9D),
                                    trackColor = Color(0xFFFFE0E0)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            when {
                                m.claimed -> Text(
                                    "已领取", fontSize = 12.sp, color = Color.Gray,
                                    modifier = Modifier
                                        .background(Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                                m.completed -> Button(
                                    onClick = { onClaim(m.type) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("领取", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                else -> Text(
                                    "进行中", fontSize = 12.sp, color = Color(0xFFFF6B9D),
                                    modifier = Modifier
                                        .background(Color(0xFFFFF0F5), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// 🎒 宠物背包 - 显示拥有的食物/玩具/药品，可使用食物喂食
// ═══════════════════════════════════════════════════════════════════
@Composable
fun PetInventoryDialog(
    items: List<PetItem>,
    onDismiss: () -> Unit,
    onUseFood: (Int) -> Unit
) {
    var category by remember { mutableStateOf("all") }
    val filtered = when (category) {
        "food" -> items.filter { it.itemType == ItemType.FOOD }
        "toy" -> items.filter { it.itemType == ItemType.TOY }
        "medicine" -> items.filter { it.itemType == ItemType.MEDICINE }
        else -> items
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.75f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFFFFB347), Color(0xFFFFC85F)))
                        )
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎒 我的背包", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "关闭", tint = Color.White) }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CategoryChip("全部", category == "all") { category = "all" }
                    CategoryChip("食物", category == "food") { category = "food" }
                    CategoryChip("玩具", category == "toy") { category = "toy" }
                    CategoryChip("药品", category == "medicine") { category = "medicine" }
                }
                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📦", fontSize = 60.sp)
                            Text("背包空空如也", fontSize = 16.sp, color = Color.Gray)
                            Spacer(Modifier.height(4.dp))
                            Text("去商城购买物品吧！", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filtered.size) { idx ->
                            val it = filtered[idx]
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(Color(0xFFFFF3E0), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            when (it.itemType) {
                                                ItemType.FOOD -> "🍖"
                                                ItemType.TOY -> "🎾"
                                                ItemType.MEDICINE -> "💊"
                                                ItemType.DECORATION -> "🎀"
                                            }, fontSize = 24.sp
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(it.itemName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D4037))
                                        Text("数量：${it.quantity}", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    if (it.itemType == ItemType.FOOD) {
                                        Button(
                                            onClick = { onUseFood(it.itemId) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B9D)),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text("喂食", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

