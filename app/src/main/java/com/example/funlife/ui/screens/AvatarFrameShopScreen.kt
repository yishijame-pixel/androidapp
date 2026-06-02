package com.example.funlife.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.funlife.data.model.ShopItem
import com.example.funlife.viewmodel.ShopViewModel
import com.example.funlife.ui.components.AvatarFrameDetailDialog
import com.example.funlife.ui.components.PurchaseConfirmDialog
import com.example.funlife.ui.components.PurchaseSuccessDialog

// Palette
object Palette {
    val bg = Color(0xFFFEF3E8)
    val white = Color(0xFFFFFFFF)
    val coral = Color(0xFFFF5222)
    val amber = Color(0xFFF5A623)
    val ink = Color(0xFF1E0D06)
    val brown = Color(0xFF7A3D24)
    val muted = Color(0xFFB07D66)
    val border = Color(0x1FC86E46)
    val shadow = Color(0x19B4501E)
}

data class RarityConfig(
    val label: String,
    val color: Color,
    val bg: Color,
    val border: Color,
    val glow: Color
)

val rarityConfigs = mapOf(
    "普通" to RarityConfig("普通", Color(0xFF8B7355), Color(0xFFF5F0E8), Color(0x408B7355), Color.Transparent),
    "稀有" to RarityConfig("稀有", Color(0xFF1A7FC1), Color(0xFFE8F4FF), Color(0x4D1A7FC1), Color(0x261A7FC1)),
    "史诗" to RarityConfig("史诗", Color(0xFF8B3FC1), Color(0xFFF5E8FF), Color(0x4D8B3FC1), Color(0x268B3FC1)),
    "传说" to RarityConfig("传说", Color(0xFFC17A1A), Color(0xFFFFF4E0), Color(0x59C17A1A), Color(0x33FFAA00))
)

@Composable
fun AvatarFrameShopScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: ShopViewModel = viewModel()
) {
    var typeTab by remember { mutableStateOf("动态") }
    var rarityTab by remember { mutableStateOf("全部") }
    
    // 记录已显示过的卡片ID，避免重复播放入场动画
    val shownCardIds = remember { mutableSetOf<Int>() }
    
    // 🔥 对话框状态
    var selectedFrame by remember { mutableStateOf<ShopItem?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var showPurchaseConfirm by remember { mutableStateOf(false) }
    var showPurchaseSuccess by remember { mutableStateOf(false) }  // 🔥 购买成功动画
    var purchasedFrameName by remember { mutableStateOf("") }  // 🔥 购买的商品名称
    
    val avatarFrames by viewModel.avatarFrames.collectAsState()
    val userCoinsData by viewModel.userCoins.collectAsState()
    val ownedFrames by viewModel.userOwnedFrames.collectAsState()
    val isUserVip by viewModel.isUserVip.collectAsState()  // 🔥 获取VIP状态
    val message by viewModel.message.collectAsState()  // 🔥 获取消息提示
    
    // 🔥 调试：打印数据数量
    LaunchedEffect(avatarFrames) {
        android.util.Log.d("AvatarFrameShop", "Total avatar frames: ${avatarFrames.size}")
        avatarFrames.forEach { frame ->
            android.util.Log.d("AvatarFrameShop", "Frame: ${frame.name}, type=${frame.type}, animated=${frame.isAnimated}, rarity=${frame.rarity}")
        }
    }
    
    val coins = userCoinsData?.coins ?: 0
    
    // 🔥 消息提示 Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessage()
        }
    }
    
    val rarityMap = mapOf("COMMON" to "普通", "RARE" to "稀有", "EPIC" to "史诗", "LEGENDARY" to "传说")
    
    val filtered = remember(typeTab, rarityTab, avatarFrames) {
        avatarFrames.filter { frame ->
            val typeMatch = when (typeTab) {
                "动态" -> frame.isAnimated
                "静态" -> !frame.isAnimated
                else -> true
            }
            val rarityMatch = rarityTab == "全部" || rarityMap[frame.rarity] == rarityTab
            typeMatch && rarityMatch
        }
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Palette.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ══ HEADER ══
            Box(
                modifier = Modifier.fillMaxWidth().background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFF5222), Color(0xFFFF8C3A), Color(0xFFFFC55E)),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 1000f)
                    )
                )
            ) {
                // 装饰圆圈 - 精确复刻React版本
                // 右上角大圆：从右上角向右上偏移，部分超出边界
                Box(
                    Modifier
                        .size(176.dp)
                        .offset(x = 40.dp, y = (-40).dp)  // 向右向上偏移
                        .align(Alignment.TopEnd)
                        .graphicsLayer { alpha = 0.15f }
                        .background(Color.White.copy(alpha = 0.5f), CircleShape)
                )
                // 左上角小圆：从左上角向左下偏移，部分超出边界
                Box(
                    Modifier
                        .size(96.dp)
                        .offset(x = (-24).dp, y = 24.dp)  // 向左向下偏移
                        .align(Alignment.TopStart)
                        .graphicsLayer { alpha = 0.10f }
                        .background(Color.White.copy(alpha = 0.5f), CircleShape)
                )
                
                Column(modifier = Modifier.statusBarsPadding()) {
                    Spacer(Modifier.height(8.dp))
                    
                    // 顶部栏
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 返回按钮
                        var backPressed by remember { mutableStateOf(false) }
                        val backScale by animateFloatAsState(if (backPressed) 0.88f else 1f, label = "backScale")
                        
                        Surface(
                            modifier = Modifier.size(36.dp).scale(backScale)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            backPressed = true
                                            tryAwaitRelease()
                                            backPressed = false
                                        },
                                        onTap = { onNavigateBack() }
                                    )
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.22f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                        
                        // 标题
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("头像框商城", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 0.3.sp)
                            Text("让头像框代表你", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                        }
                        
                        // 金币
                        var coinPressed by remember { mutableStateOf(false) }
                        val coinScale by animateFloatAsState(if (coinPressed) 0.93f else 1f, label = "coinScale")
                        
                        Surface(
                            modifier = Modifier.scale(coinScale)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            coinPressed = true
                                            tryAwaitRelease()
                                            coinPressed = false
                                        }
                                    )
                                },
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Star, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Text(coins.toString(), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }
                        }
                    }
                    
                    // 类型标签 - 使用AnimatedContent实现滑动效果
                    Box(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                            .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                            .padding(4.dp)
                    ) {
                        // 滑动背景
                        val offsetX by animateFloatAsState(
                            targetValue = if (typeTab == "动态") 0f else 1f,
                            animationSpec = spring(
                                dampingRatio = 0.75f,
                                stiffness = 300f
                            ),
                            label = "tabOffset"
                        )
                        
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val density = LocalDensity.current
                            val tabWidth = with(density) { (maxWidth / 2).toPx() }
                            
                            Box(
                                Modifier.fillMaxWidth(0.5f)
                                    .offset(x = (offsetX * tabWidth / density.density).dp)
                                    .height(40.dp)
                                    .background(Palette.white, RoundedCornerShape(12.dp))
                            )
                        }
                        
                        Row(Modifier.fillMaxWidth()) {
                            listOf("动态", "静态").forEach { type ->
                                var pressed by remember { mutableStateOf(false) }
                                val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "typeScale")
                                val isSelected = typeTab == type
                                
                                Box(
                                    Modifier.weight(1f).scale(scale)
                                        .pointerInput(type) {
                                            detectTapGestures(
                                                onPress = {
                                                    pressed = true
                                                    tryAwaitRelease()
                                                    pressed = false
                                                },
                                                onTap = { typeTab = type }
                                            )
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            if (type == "动态") "✦" else "◎",
                                            fontSize = 13.sp,
                                            color = if (isSelected) Palette.coral else Color.White.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            type,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Palette.coral else Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(28.dp))
                }
                
                // 波浪底部
                Canvas(Modifier.fillMaxWidth().height(28.dp).align(Alignment.BottomCenter)) {
                    val path = Path().apply {
                        moveTo(0f, size.height * 0.36f)
                        cubicTo(
                            size.width * 0.18f, size.height,
                            size.width * 0.41f, 0f,
                            size.width * 0.62f, size.height * 0.57f
                        )
                        cubicTo(
                            size.width * 0.77f, size.height,
                            size.width * 0.90f, size.height * 0.29f,
                            size.width, size.height * 0.5f
                        )
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(path, color = Palette.bg)
                }
            }
            
            // ══ RARITY FILTER ══
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("全部", "普通", "稀有", "史诗", "传说").forEach { rarity ->
                    val active = rarityTab == rarity
                    val rc = if (rarity != "全部") rarityConfigs[rarity] else null
                    var pressed by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "rarityScale")
                    
                    Surface(
                        modifier = Modifier.scale(scale)
                            .pointerInput(rarity) {
                                detectTapGestures(
                                    onPress = {
                                        pressed = true
                                        tryAwaitRelease()
                                        pressed = false
                                    },
                                    onTap = { rarityTab = rarity }
                                )
                            },
                        shape = RoundedCornerShape(20.dp),
                        color = if (active) (rc?.bg ?: Color(0x1AFF5222)) else Palette.white,
                        border = BorderStroke(1.5.dp, if (active) (rc?.border ?: Color(0x4DFF5222)) else Palette.border),
                        shadowElevation = if (active) 4.dp else 0.dp
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (rarity == "传说" && active) {
                                Icon(Icons.Default.Star, null, tint = rc!!.color, modifier = Modifier.size(9.dp))
                            }
                            Text(
                                rarity,
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) (rc?.color ?: Palette.coral) else Palette.muted
                            )
                        }
                    }
                }
            }
            
            // ══ GRID ══
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filtered.isEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("🎭", fontSize = 40.sp)
                            Text("暂无该稀有度头像框", fontSize = 14.sp, color = Palette.muted)
                        }
                    }
                } else {
                    itemsIndexed(filtered, key = { _, frame -> frame.id }) { index, frame ->
                        val isOwned = ownedFrames.any { it.frameId == frame.id }
                        val rarityDisplay = rarityMap[frame.rarity] ?: "普通"
                        val isVip = isUserVip  // 🔥 获取VIP状态
                        
                        FrameCard(
                            frame = frame,
                            index = index,
                            isOwned = isOwned,
                            rarityDisplay = rarityDisplay,
                            shownCardIds = shownCardIds,
                            isVip = isVip,
                            onClick = {
                                selectedFrame = frame
                                showDetailDialog = true
                            }
                        )
                    }
                }
            }
        }
        
        // 🔥 Snackbar提示
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
        
        // 🔥 商品详情对话框
        if (showDetailDialog && selectedFrame != null) {
            val frame = selectedFrame!!
            val isOwned = ownedFrames.any { it.frameId == frame.id }
            val isEquipped = ownedFrames.any { it.frameId == frame.id && it.isEquipped }
            val rarityDisplay = rarityMap[frame.rarity] ?: "普通"
            
            AvatarFrameDetailDialog(
                frame = frame,
                isOwned = isOwned,
                isEquipped = isEquipped,
                isVip = isUserVip,
                userCoins = coins,
                rarityDisplay = rarityDisplay,
                onDismiss = {
                    showDetailDialog = false
                    selectedFrame = null
                },
                onPurchase = {
                    showPurchaseConfirm = true
                },
                onEquip = {
                    viewModel.equipAvatarFrame(frame.id)
                },
                onUnequip = {
                    viewModel.unequipAvatarFrame()
                }
            )
        }
        
        // 🔥 购买确认对话框
        if (showPurchaseConfirm && selectedFrame != null) {
            val frame = selectedFrame!!
            val actualPrice = if (isUserVip) frame.vipPrice else frame.price
            
            PurchaseConfirmDialog(
                frame = frame,
                price = actualPrice,
                currentCoins = coins,
                isVip = isUserVip,
                onDismiss = {
                    showPurchaseConfirm = false
                },
                onConfirm = {
                    purchasedFrameName = frame.name
                    viewModel.purchaseAvatarFrame(frame)
                    showPurchaseConfirm = false
                    showDetailDialog = false
                    selectedFrame = null
                    showPurchaseSuccess = true  // 🔥 显示购买成功动画
                }
            )
        }
        
        // 🔥 购买成功动画
        if (showPurchaseSuccess) {
            PurchaseSuccessDialog(
                frameName = purchasedFrameName,
                onDismiss = {
                    showPurchaseSuccess = false
                }
            )
        }
    }
}

@Composable
fun FrameCard(
    frame: ShopItem, 
    index: Int, 
    isOwned: Boolean, 
    rarityDisplay: String,
    shownCardIds: MutableSet<Int>,
    isVip: Boolean = false,  // 🔥 新增：用户是否为VIP
    onClick: () -> Unit = {}  // 🔥 新增：点击事件
) {
    var tapped by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (tapped) 0.93f else 1f,
        animationSpec = spring(stiffness = 400f, dampingRatio = 0.6f),
        label = "cardScale"
    )
    
    val rc = rarityConfigs[rarityDisplay]!!
    val isVipOnly = frame.vipPrice < frame.price
    
    // 入场动画 - 只在第一次显示时播放，且无延迟
    val hasShown = remember(frame.id) { shownCardIds.contains(frame.id) }
    var visible by remember(frame.id) { mutableStateOf(hasShown) }
    
    LaunchedEffect(frame.id) {
        if (!hasShown) {
            // 移除延迟，立即显示
            visible = true
            shownCardIds.add(frame.id)
        }
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(stiffness = 260f, dampingRatio = 0.85f),
        label = "cardAlpha"
    )
    
    val colors = remember(frame.id) {
        // 根据商品ID生成独特的颜色组合
        val colorPalettes = listOf(
            // 粉色系
            listOf(0xFFFFD6E7, 0xFFFFACC7),
            // 蓝色系
            listOf(0xFFD6E8FF, 0xFFACD0FF),
            // 橙色系
            listOf(0xFFFFE8C6, 0xFFFFCA88),
            // 青色系
            listOf(0xFFC6F0E8, 0xFF88DDCC),
            // 紫色系
            listOf(0xFFF0D6FF, 0xFFD899FF),
            // 黄色系
            listOf(0xFFFFF0D6, 0xFFFFD699),
            // 薄荷绿系
            listOf(0xFFD6FFE8, 0xFFAAFFCC),
            // 桃色系
            listOf(0xFFFFD6D6, 0xFFFF9999),
            // 天蓝系
            listOf(0xFFD6F0FF, 0xFF99D6FF),
            // 淡紫系
            listOf(0xFFE8D6FF, 0xFFC299FF),
            // 金色系
            listOf(0xFFFFE8D6, 0xFFFFBB88),
            // 玫瑰系
            listOf(0xFFFFF0E8, 0xFFFFD6CC)
        )
        
        // 根据商品ID选择颜色（循环使用）
        colorPalettes[frame.id % colorPalettes.size]
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        tapped = true
                        tryAwaitRelease()
                        tapped = false
                    },
                    onTap = { onClick() }  // 🔥 添加点击事件
                )
            },
        shape = RoundedCornerShape(20.dp),
        color = Palette.white,
        border = BorderStroke(1.5.dp, if (isOwned) Color(0x40FF5222) else Palette.border),
        shadowElevation = if (isOwned) 6.dp else 2.dp,
        tonalElevation = 0.dp
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth()) {
                // 稀有度顶部条纹
                Box(
                    Modifier.fillMaxWidth().height(3.dp).background(
                        brush = when (rarityDisplay) {
                            "传说" -> Brush.horizontalGradient(listOf(Color(0xFFFF9500), Color(0xFFFFD000), Color(0xFFFF9500)))
                            "史诗" -> Brush.horizontalGradient(listOf(Color(0xFF8B3FC1), Color(0xFFC17AFF)))
                            "稀有" -> Brush.horizontalGradient(listOf(Color(0xFF1A7FC1), Color(0xFF5AB4FF)))
                            else -> Brush.horizontalGradient(listOf(Color(0xFFC8AA8C), Color(0xFFC8AA8C)))
                        }
                    )
                )
                
                // VIP徽章
                if (isVipOnly) {
                    Surface(
                        Modifier.padding(start = 10.dp, top = 12.dp).align(Alignment.TopStart),
                        shape = RoundedCornerShape(7.dp),
                        color = Color.Transparent
                    ) {
                        Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFFFF9500), Color(0xFFFFCD00))))) {
                            Row(
                                Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, null, tint = Color.White, modifier = Modifier.size(8.dp))
                                Text("VIP", fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 0.3.sp)
                            }
                        }
                    }
                }
                
                // 已拥有徽章
                if (isOwned) {
                    Surface(
                        Modifier.padding(end = 10.dp, top = 12.dp).size(20.dp).align(Alignment.TopEnd),
                        shape = CircleShape,
                        color = Palette.coral
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            // 头像框预览 - 带双层阴影
            Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) {
                // 外圈装饰环 + 双层阴影
                Canvas(Modifier.fillMaxSize()) {
                    val color1 = Color(colors[0])
                    val color2 = Color(colors[1])
                    
                    // 外层阴影
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color2.copy(alpha = 0.4f), Color.Transparent),
                            radius = size.width * 0.6f
                        ),
                        radius = size.width / 2f + 8f
                    )
                    
                    // 内层阴影环
                    drawCircle(
                        color = color2.copy(alpha = 0.27f),
                        radius = size.width / 2f + 2f
                    )
                    
                    // 主圆环
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(color1, color2),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height)
                        ),
                        radius = size.width / 2f
                    )
                }
                
                // 内圈头像 - 显示真实的头像框图片
                Box(
                    Modifier.size(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 如果有头像框资源路径，显示真实图片
                    if (!frame.assetPath.isNullOrEmpty()) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        
                        // 使用SubcomposeAsyncImage支持自定义加载和错误状态
                        coil.compose.SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data("file:///android_asset/${frame.assetPath}")
                                .crossfade(300) // 300ms淡入动画
                                .memoryCacheKey(frame.assetPath) // 缓存key
                                .build(),
                            contentDescription = frame.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            loading = {
                                // 骨架屏加载效果 - 更明显的脉动动画
                                val infiniteTransition = rememberInfiniteTransition(label = "loading")
                                val shimmerAlpha by infiniteTransition.animateFloat(
                                    initialValue = 0.2f,
                                    targetValue = 0.8f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "shimmerAlpha"
                                )
                                
                                val shimmerOffset by infiniteTransition.animateFloat(
                                    initialValue = -1f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1500, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "shimmerOffset"
                                )
                                
                                Box(
                                    Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // 背景渐变
                                    Canvas(Modifier.fillMaxSize()) {
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    Color(colors[0]).copy(alpha = 0.3f),
                                                    Color(colors[1]).copy(alpha = 0.15f)
                                                )
                                            )
                                        )
                                    }
                                    
                                    // 闪烁光效
                                    Canvas(Modifier.fillMaxSize()) {
                                        val gradientWidth = size.width * 0.5f
                                        val startX = size.width * shimmerOffset
                                        
                                        drawCircle(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0f),
                                                    Color.White.copy(alpha = shimmerAlpha * 0.4f),
                                                    Color.White.copy(alpha = 0f)
                                                ),
                                                start = Offset(startX - gradientWidth, 0f),
                                                end = Offset(startX + gradientWidth, 0f)
                                            )
                                        )
                                    }
                                    
                                    // 加载指示器
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White.copy(alpha = 0.7f),
                                        strokeWidth = 2.5.dp
                                    )
                                }
                            },
                            error = {
                                // 加载失败时显示emoji
                                Box(
                                    Modifier.fillMaxSize().background(
                                        brush = Brush.linearGradient(listOf(Color(colors[0]), Color.White)),
                                        shape = CircleShape
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(frame.icon, fontSize = 26.sp)
                                }
                            }
                        )
                    } else {
                        // 没有资源路径时显示emoji
                        Box(
                            Modifier.fillMaxSize().background(
                                brush = Brush.linearGradient(listOf(Color(colors[0]), Color.White)),
                                shape = CircleShape
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(frame.icon, fontSize = 26.sp)
                        }
                    }
                }
                
                // 传说/史诗光晕动画
                if (rarityDisplay == "传说" || rarityDisplay == "史诗") {
                    val infiniteTransition = rememberInfiniteTransition(label = "glow")
                    val glowScale by infiniteTransition.animateFloat(
                        initialValue = 1f, targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2500, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "glowScale"
                    )
                    val glowAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.6f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2500, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "glowAlpha"
                    )
                    
                    Box(
                        Modifier.fillMaxSize().graphicsLayer {
                            scaleX = glowScale
                            scaleY = glowScale
                            this.alpha = glowAlpha
                        }.background(
                            brush = Brush.radialGradient(
                                colors = listOf(rc.glow, Color.Transparent),
                                radius = 100f
                            ),
                            shape = CircleShape
                        )
                    )
                }
            }
            
            Spacer(Modifier.height(4.dp))
            
            // 名称
            Text(frame.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink, maxLines = 1)
            
            Spacer(Modifier.height(2.dp))
            
            // 稀有度标签
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = rc.bg,
                border = BorderStroke(1.dp, rc.border)
            ) {
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (rarityDisplay == "传说") {
                        Icon(Icons.Default.Star, null, tint = rc.color, modifier = Modifier.size(8.dp))
                    }
                    Text(rc.label, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = rc.color)
                }
            }
            
            Spacer(Modifier.height(10.dp))
            
            // 底部价格/状态栏
            Box(
                Modifier.fillMaxWidth()
                    .background(
                        color = when {
                            isOwned -> Color(0x0AFF5222)
                            isVipOnly -> Color(0x0FFFCD00)
                            else -> Color(0x0AF5A623)
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = when {
                            isOwned -> Color(0x1FFF5222)
                            isVipOnly -> Color(0x26FF9500)
                            else -> Palette.border
                        },
                        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                    )
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    // 已拥有
                    isOwned -> Text("已拥有", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Palette.coral)
                    
                    // VIP专属商品
                    isVipOnly -> {
                        if (isVip) {
                            // VIP用户：显示VIP价格（高亮）+ 普通价格（划线）
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // VIP价格（高亮）
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(10.dp))
                                    Text(
                                        frame.vipPrice.toString(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFC17A1A)
                                    )
                                }
                                
                                // 普通价格（划线）
                                Text(
                                    frame.price.toString(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF999999),
                                    style = androidx.compose.ui.text.TextStyle(
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                    )
                                )
                            }
                        } else {
                            // 普通用户：显示普通价格 + VIP价格（置灰+锁）
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 普通价格
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Star, null, tint = Palette.amber, modifier = Modifier.size(10.dp))
                                    Text(
                                        frame.price.toString(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Palette.brown
                                    )
                                }
                                
                                // VIP价格（置灰+锁）
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, null, tint = Color(0xFFB0B0B0), modifier = Modifier.size(8.dp))
                                    Text(
                                        frame.vipPrice.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFB0B0B0)
                                    )
                                }
                            }
                        }
                    }
                    
                    // 普通商品
                    else -> Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, null, tint = Palette.amber, modifier = Modifier.size(10.dp))
                        Text(frame.price.toString(), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Palette.brown)
                    }
                }
            }
        }
    }
}


// 🔥 骨架屏卡片组件
@Composable
fun SkeletonCard(colors: List<Long>) {
    // 脉动动画
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Palette.white,
        border = BorderStroke(1.5.dp, Palette.border),
        shadowElevation = 2.dp
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 顶部条纹占位
            Box(
                Modifier.fillMaxWidth().height(3.dp)
                    .background(Color(0xFFC8AA8C).copy(alpha = shimmerAlpha))
            )
            
            Spacer(Modifier.height(8.dp))
            
            // 圆形头像占位
            Box(
                Modifier.size(78.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(colors[0]).copy(alpha = shimmerAlpha * 0.5f),
                                Color(colors[1]).copy(alpha = shimmerAlpha * 0.3f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 加载指示器
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White.copy(alpha = 0.6f),
                    strokeWidth = 2.dp
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            // 文字占位
            Box(
                Modifier.width(60.dp).height(12.dp)
                    .background(
                        Color.Gray.copy(alpha = shimmerAlpha * 0.3f),
                        RoundedCornerShape(6.dp)
                    )
            )
            
            // 价格占位
            Box(
                Modifier.width(40.dp).height(10.dp)
                    .background(
                        Color.Gray.copy(alpha = shimmerAlpha * 0.2f),
                        RoundedCornerShape(5.dp)
                    )
            )
        }
    }
}
