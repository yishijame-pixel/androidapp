// VipScreen.kt - 商业级高端VIP会员系统界面
// 完整可运行版本
package com.example.funlife.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.funlife.data.model.VipLevel
import com.example.funlife.viewmodel.VipViewModel
import com.example.funlife.viewmodel.AuthViewModel
import kotlin.math.*

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

data class VipCardData(
    val title: String,
    val icon: String,
    val benefits: List<String>,
    val price: String,
    val period: String,
    val gradient: List<Color>,
    val level: VipLevel,
    val cornerTag: String? = null,
    val cornerTagColor: List<Color>? = null,
    val topLeftTag: String? = null,
    val topLeftTagColor: List<Color>? = null,
    val originalPrice: String? = null,   // 原价（划线价）
    val discountLabel: String? = null,   // 折扣标签，如"5折"
    val saleBadge: String? = null        // 促销徽章，如"限时特惠"
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VipScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    vipViewModel: VipViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val userSession = authViewModel.getCurrentSession()
    val userVip by vipViewModel.userVip.collectAsState()
    val message by vipViewModel.message.collectAsState()
    val isLoading by vipViewModel.isLoading.collectAsState()
    
    // VIP激活动画状态
    var showVipAnimation by remember { mutableStateOf(false) }
    var activatedVipLevel by remember { mutableStateOf<VipLevel?>(null) }
    var bonusCoins by remember { mutableStateOf(0) }
    
    // 页面加载状态
    var isPageLoading by remember { mutableStateOf(true) }
    
    // 支付对话框状态
    var showPaymentDialog by remember { mutableStateOf(false) }
    var selectedVipCard by remember { mutableStateOf<VipCardData?>(null) }
    
    LaunchedEffect(userSession) {
        userSession?.userId?.let { vipViewModel.setUserId(it) }
    }
    
    LaunchedEffect(message) {
        if (message != null) {
            // 检查是否是VIP激活消息（格式：VIP名称|金币数量）
            if (message!!.contains("成功激活") || message!!.contains("成功购买")) {
                val parts = message!!.split("|")
                if (parts.size == 2) {
                    // 解析VIP等级和金币
                    val vipName = parts[0].substringAfter("成功激活 ").substringAfter("成功购买 ")
                    val coins = parts[1].toIntOrNull() ?: 0
                    
                    // 确定VIP等级
                    val vipLevel = when {
                        vipName.contains("终身") || vipName.contains("VIP3") -> VipLevel.VIP3
                        vipName.contains("年费") || vipName.contains("VIP2") -> VipLevel.VIP2
                        vipName.contains("普通") || vipName.contains("VIP1") -> VipLevel.VIP1
                        else -> null
                    }
                    
                    if (vipLevel != null && coins > 0) {
                        activatedVipLevel = vipLevel
                        bonusCoins = coins
                        showVipAnimation = true
                        
                        // 设置标记，让HomeScreen显示能量光束动画
                        val prefs = navController.context.getSharedPreferences("vip_animation", android.content.Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("show_beam_animation", true).apply()
                    }
                }
            }
            
            kotlinx.coroutines.delay(3000)
            vipViewModel.clearMessage()
        }
    }
    
    // 模拟页面加载完成
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(800) // 短暂延迟让动画显示
        isPageLoading = false
    }
    
    BackHandler {
        navController.popBackStack()
    }
    
    val vipCards = listOf(
        VipCardData(
            title = VipLevel.VIP1.displayName,
            icon = VipLevel.VIP1.icon,
            benefits = VipLevel.VIP1.benefits,
            price = "39.9",
            period = "永久",
            gradient = listOf(
                Color(0xFFFFD700), Color(0xFFFF8C00), Color(0xFFE6446E)
            ),
            level = VipLevel.VIP1,
            cornerTag = "超值",
            cornerTagColor = listOf(Color(0xFFFF6B00), Color(0xFFFF9500)),
            topLeftTag = "新人专享",
            topLeftTagColor = listOf(Color(0xFFFF1744), Color(0xFFFF5252)),
            originalPrice = "79.9",
            discountLabel = "5折",
            saleBadge = "🎁 首充特惠"
        ),
        VipCardData(
            title = VipLevel.VIP2.displayName,
            icon = VipLevel.VIP2.icon,
            benefits = VipLevel.VIP2.benefits,
            price = "99.9",
            period = "年费",
            gradient = listOf(
                Color(0xFF00E5FF), Color(0xFF2979FF), Color(0xFF7C4DFF)
            ),
            level = VipLevel.VIP2,
            cornerTag = "热销",
            cornerTagColor = listOf(Color(0xFFFF1744), Color(0xFFFF5252)),
            topLeftTag = "限时优惠",
            topLeftTagColor = listOf(Color(0xFFFF6B00), Color(0xFFFFAA00)),
            originalPrice = "199",
            discountLabel = "5折",
            saleBadge = "🔥 爆款推荐"
        ),
        VipCardData(
            title = VipLevel.VIP3.displayName,
            icon = VipLevel.VIP3.icon,
            benefits = VipLevel.VIP3.benefits,
            price = "399",
            period = "终身",
            gradient = listOf(
                Color(0xFFE040FB), Color(0xFFAA00FF), Color(0xFF6200EA)
            ),
            level = VipLevel.VIP3,
            cornerTag = "至尊",
            cornerTagColor = listOf(Color(0xFFFFD700), Color(0xFFFFAA00)),
            topLeftTag = "尊贵推荐",
            topLeftTagColor = listOf(Color(0xFFFFD700), Color(0xFFFF9500)),
            originalPrice = "999",
            discountLabel = "4折",
            saleBadge = "👑 买到即赚"
        )
    )
    
    val pagerState = rememberPagerState(
        initialPage = 1,
        pageCount = { vipCards.size }
    )
    
    // 自动轮播效果 - 从当前页面继续循环
    var isUserInteracting by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500) // 每500ms检查一次
            val currentTime = System.currentTimeMillis()
            
            // 如果用户5秒内没有交互，且不在滚动中，则自动轮播
            if (!isUserInteracting && 
                !pagerState.isScrollInProgress && 
                currentTime - lastInteractionTime > 5000) {
                
                val nextPage = (pagerState.currentPage + 1) % vipCards.size
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(
                        durationMillis = 800,
                        easing = FastOutSlowInEasing
                    )
                )
                // 更新最后交互时间，让下一次轮播在3秒后继续
                lastInteractionTime = System.currentTimeMillis()
                kotlinx.coroutines.delay(3000) // 停留3秒
            }
        }
    }
    
    // 监听用户交互
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (pagerState.isScrollInProgress) {
            isUserInteracting = true
            lastInteractionTime = System.currentTimeMillis()
        } else {
            isUserInteracting = false
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 可爱的加载动画
        if (isPageLoading) {
            CuteLoadingAnimation()
        }
        
        // 主内容 - 淡入效果
        AnimatedVisibility(
            visible = !isPageLoading,
            enter = fadeIn(animationSpec = tween(600))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
        PremiumSpaceBackground()
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f)
                        ),
                        radius = 1200f
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))  // 最小间距，紧贴状态栏
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 返回按钮（无背景，只有图标）
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // VIP标签（炫酷霓虹渐变 + 流光效果）
                VipBadge()
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            EpicTitle()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 可滑动的3D卡片轮播
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp),  // 恢复原来的高度
                pageSpacing = 16.dp,
                contentPadding = PaddingValues(horizontal = 60.dp),
                beyondBoundsPageCount = 1
            ) { page ->
                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                
                SwipeableVipCard(
                    card = vipCards[page],
                    pageOffset = pageOffset,
                    isCenter = page == pagerState.currentPage,
                    onClick = {
                        vipViewModel.purchaseVip(
                            vipCards[page].level.level,
                            when(vipCards[page].period) {
                                "月费" -> 30
                                "季费" -> 90
                                "年费" -> 365
                                else -> 30
                            },
                            (vipCards[page].price.toFloatOrNull() ?: 0f).toInt() * 100
                        )
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(30.dp))
            
            // 页面指示器
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(vipCards.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 32.dp else 8.dp, 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) {
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFFFFD700), Color(0xFFFF9500))
                                    )
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.3f))
                                    )
                                }
                            )
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 兑换码区域
            RedeemCodeSection(
                vipViewModel = vipViewModel,
                onRedeemSuccess = {
                    // 兑换成功后的处理
                }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            PremiumPurchaseButton(
                onClick = {
                    val selectedCard = vipCards[pagerState.currentPage]
                    selectedVipCard = selectedCard
                    showPaymentDialog = true
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                "开通VIP",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(50.dp))
        }
        
        AnimatedVisibility(
            visible = message != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF00FF88), Color(0xFF00DDFF))
                        )
                    )
                    .padding(horizontal = 28.dp, vertical = 14.dp)
            ) {
                Text(
                    text = message ?: "",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        if (isLoading) {
            val loadingMessage by vipViewModel.loadingMessage.collectAsState()
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFFFD700),
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(60.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text(
                        text = loadingMessage ?: "加载中...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // 支付对话框
        if (showPaymentDialog && selectedVipCard != null) {
            com.example.funlife.ui.components.VipPaymentDialog(
                vipTitle = selectedVipCard!!.title,
                price = selectedVipCard!!.price,
                onDismiss = {
                    showPaymentDialog = false
                    selectedVipCard = null
                },
                onPaymentComplete = {
                    // 支付完成后激活VIP
                    vipViewModel.purchaseVip(
                        selectedVipCard!!.level.level,
                        when(selectedVipCard!!.period) {
                            "月费" -> 30
                            "季费" -> 90
                            "年费" -> 365
                            else -> 30
                        },
                        (selectedVipCard!!.price.toFloatOrNull() ?: 0f).toInt() * 100
                    )
                }
            )
        }
            }
        }
        
        // VIP激活动画（最高层级）
        if (showVipAnimation && activatedVipLevel != null) {
            com.example.funlife.ui.components.VipActivationAnimation(
                vipLevel = activatedVipLevel!!,
                coins = bonusCoins,
                onDismiss = {
                    showVipAnimation = false
                    activatedVipLevel = null
                    bonusCoins = 0
                    
                    // 设置首页首次进入特效标记
                    val prefs = context.getSharedPreferences("vip_animation", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("show_first_entry_effect", true).apply()
                }
            )
        }
    }
}

// 兑换码区域组件
@Composable
fun RedeemCodeSection(
    vipViewModel: VipViewModel,
    onRedeemSuccess: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var redeemCode by remember { mutableStateOf("") }
    
    val infiniteTransition = rememberInfiniteTransition(label = "redeem")
    
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 展开/收起按钮
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF6B5FFF).copy(alpha = 0.3f),
                            Color(0xFF9D4FFF).copy(alpha = 0.3f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF6B5FFF).copy(alpha = 0.6f),
                            Color(0xFF9D4FFF).copy(alpha = 0.6f)
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .clickable { isExpanded = !isExpanded },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CardGiftcard,
                    contentDescription = "兑换码",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isExpanded) "收起兑换码" else "输入兑换码",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "收起" else "展开",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        
        // 展开的输入区域
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 输入框
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF1A1A2E).copy(alpha = 0.8f),
                                    Color(0xFF16213E).copy(alpha = 0.8f)
                                )
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFFD700).copy(alpha = 0.5f),
                                    Color(0xFFFF00FF).copy(alpha = 0.5f),
                                    Color(0xFF00D9FF).copy(alpha = 0.5f)
                                )
                            ),
                            shape = RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // 流光效果 - 使用圆角矩形，填充整个Box
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val shimmerOffset = shimmer * size.width * 2.5f
                        val cornerRadius = 28.dp.toPx()
                        
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.1f),
                                    Color.Transparent
                                ),
                                startX = shimmerOffset - size.width * 0.8f,
                                endX = shimmerOffset + size.width * 0.2f
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
                        )
                    }
                    
                    // 输入框内容（带padding）
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = redeemCode,
                            onValueChange = { redeemCode = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "兑换码",
                                        tint = Color(0xFFFFD700).copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (redeemCode.isEmpty()) {
                                            Text(
                                                text = "请输入兑换码",
                                                color = Color.White.copy(alpha = 0.4f),
                                                fontSize = 16.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 兑换按钮
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFFD700),
                                    Color(0xFFFF9500)
                                )
                            )
                        )
                        .clickable(enabled = redeemCode.isNotEmpty()) {
                            vipViewModel.redeemCode(redeemCode)
                            redeemCode = ""
                            isExpanded = false
                            onRedeemSuccess()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "立即兑换",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        style = LocalTextStyle.current.copy(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.3f),
                                offset = Offset(0f, 2f),
                                blurRadius = 4f
                            )
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 提示文字
                Text(
                    text = "💎 输入兑换码即可获得永久VIP特权",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun PremiumSpaceBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "space")

    // 星云旋转 - 加快
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.88f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    // 极光呼吸
    val aurora by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "aurora"
    )
    // 大范围漂移
    val drift by infiniteTransition.animateFloat(
        initialValue = -80f, targetValue = 80f,
        animationSpec = infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift"
    )
    // 水平漂移
    val driftX by infiniteTransition.animateFloat(
        initialValue = -60f, targetValue = 60f,
        animationSpec = infiniteRepeatable(tween(7000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "driftX"
    )
    // 光球浮动
    val orbFloat by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Restart),
        label = "orbFloat"
    )
    // 能量波
    val wave by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart),
        label = "wave"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // 深色渐变底色
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF05000D),
                            Color(0xFF0D0422),
                            Color(0xFF150835),
                            Color(0xFF0A0D2E),
                            Color(0xFF030108)
                        )
                    )
                )
        )

        // 漂移星云层 - 不断移动
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = rotation
                    scaleX = pulse
                    scaleY = pulse
                    translationX = driftX
                    translationY = drift * 0.3f
                }
        ) {
            val cx = size.width / 2
            val cy = size.height / 3f

            // 紫色星云（中心）- 更亮
            repeat(6) { i ->
                val r = (200 + i * 160).dp.toPx()
                val a = (0.22f - i * 0.025f).coerceAtLeast(0.03f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFBB55FF).copy(alpha = a),
                            Color(0xFF8833DD).copy(alpha = a * 0.6f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy), radius = r
                    ),
                    radius = r, center = Offset(cx, cy)
                )
            }

            // 青蓝星云（右上）
            repeat(4) { i ->
                val r = (150 + i * 130).dp.toPx()
                val a = (0.15f - i * 0.025f).coerceAtLeast(0.03f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00CCFF).copy(alpha = a),
                            Color(0xFF0066EE).copy(alpha = a * 0.5f),
                            Color.Transparent
                        ),
                        center = Offset(cx + 200f, cy - 100f), radius = r
                    ),
                    radius = r, center = Offset(cx + 200f, cy - 100f)
                )
            }

            // 粉紫星云（左下）
            repeat(4) { i ->
                val r = (120 + i * 120).dp.toPx()
                val a = (0.13f - i * 0.02f).coerceAtLeast(0.03f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF55BB).copy(alpha = a),
                            Color(0xFFDD3399).copy(alpha = a * 0.5f),
                            Color.Transparent
                        ),
                        center = Offset(cx - 150f, cy + 400f), radius = r
                    ),
                    radius = r, center = Offset(cx - 150f, cy + 400f)
                )
            }
        }

        // 浮动光球 - 6个大光球漂浮移动
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = orbFloat
            val orbData = listOf(
                Triple(Color(0xFFFF00FF), 0.15f, 0f),
                Triple(Color(0xFF00D9FF), 0.7f, 0.17f),
                Triple(Color(0xFFFFD700), 0.4f, 0.33f),
                Triple(Color(0xFF00FF88), 0.85f, 0.5f),
                Triple(Color(0xFFFF5577), 0.25f, 0.67f),
                Triple(Color(0xFF7755FF), 0.6f, 0.83f)
            )
            orbData.forEach { (color, xFrac, phase) ->
                val p = (t + phase) % 1f
                val ox = size.width * xFrac + sin(p * PI.toFloat() * 2f) * 60f + driftX
                val oy = size.height * (0.1f + p * 0.8f) + drift * 0.5f
                val oa = (0.12f + 0.08f * sin(p * PI.toFloat() * 4f)).coerceIn(0f, 1f)
                val oRadius = 40.dp.toPx() + 15.dp.toPx() * sin(p * PI.toFloat() * 2f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = oa),
                            color.copy(alpha = oa * 0.4f),
                            Color.Transparent
                        ),
                        center = Offset(ox, oy), radius = oRadius
                    ),
                    radius = oRadius, center = Offset(ox, oy)
                )
            }
        }

        // 极光光带 - 更宽更明显，大幅移动
        Canvas(modifier = Modifier.fillMaxSize()) {
            val auroraAlpha = (0.08f + aurora * 0.15f).coerceIn(0f, 1f)
            // 光带1 - 青绿
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF00FFaa).copy(alpha = auroraAlpha),
                        Color(0xFF00DDFF).copy(alpha = auroraAlpha * 0.8f),
                        Color.Transparent
                    ),
                    startY = size.height * 0.1f + drift * 2.5f,
                    endY = size.height * 0.35f + drift * 2.5f
                )
            )
            // 光带2 - 粉紫
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFFF00FF).copy(alpha = auroraAlpha * 0.7f),
                        Color(0xFF8800FF).copy(alpha = auroraAlpha * 0.5f),
                        Color.Transparent
                    ),
                    startY = size.height * 0.5f - drift * 2f,
                    endY = size.height * 0.75f - drift * 2f
                )
            )
            // 光带3 - 金色（新增）
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFFFD700).copy(alpha = auroraAlpha * 0.4f),
                        Color(0xFFFF8C00).copy(alpha = auroraAlpha * 0.3f),
                        Color.Transparent
                    ),
                    startY = size.height * 0.7f + drift * 1.8f,
                    endY = size.height * 0.9f + drift * 1.8f
                )
            )
        }

        // 能量波纹 - 从中心扩散
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2
            repeat(3) { i ->
                val p = (wave + i * 0.33f) % 1f
                val waveRadius = p * size.width * 0.8f
                val wa = ((1f - p) * 0.15f).coerceIn(0f, 1f)
                if (wa > 0.01f) {
                    drawCircle(
                        color = Color(0xFF7755FF).copy(alpha = wa),
                        radius = waveRadius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5f)
                    )
                }
            }
        }

        // 彩色星点 - 持续运动
        Canvas(modifier = Modifier.fillMaxSize()) {
            val time = rotation / 360f
            val starColors = listOf(
                Color.White, Color(0xFFFFD700), Color(0xFF00D9FF),
                Color(0xFFFF77CC), Color(0xFFAABBFF), Color(0xFF88FFBB)
            )
            repeat(100) { i ->
                // 星点持续漂移
                val baseX = ((i * 23f + 7f) % size.width)
                val baseY = ((i * 31f + 13f) % size.height)
                val x = baseX + sin(time * PI.toFloat() * 2f + i * 0.5f) * 20f + driftX * 0.3f
                val y = baseY + cos(time * PI.toFloat() * 2f + i * 0.7f) * 20f + drift * 0.2f
                val twinkle = (0.2f + 0.8f * ((sin(time * PI.toFloat() * 6f + i * 0.9f) + 1f) / 2f)).coerceIn(0f, 1f)
                val s = when {
                    i % 20 == 0 -> 3f
                    i % 10 == 0 -> 2.2f
                    i % 5 == 0 -> 1.6f
                    else -> 1f
                }
                val color = starColors[i % starColors.size]
                drawCircle(
                    color = color.copy(alpha = (twinkle * 0.85f).coerceIn(0f, 1f)),
                    radius = s,
                    center = Offset(x, y)
                )
                if (s >= 2f) {
                    drawCircle(
                        color = color.copy(alpha = (twinkle * 0.3f).coerceIn(0f, 1f)),
                        radius = s * 3.5f,
                        center = Offset(x, y)
                    )
                }
            }
        }

        // 流星效果 - 大量、多方向
        val meteorAnim by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart),
            label = "meteor"
        )
        val meteorAnim2 by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(3800, easing = LinearEasing), RepeatMode.Restart),
            label = "meteor2"
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val meteorColors = listOf(
                Color(0xFF00D9FF), Color(0xFFFFD700), Color(0xFFFF77CC),
                Color(0xFF88FFBB), Color(0xFFAABBFF)
            )
            // 从右上往左下的流星 - 8条
            repeat(8) { idx ->
                val phase = (meteorAnim + idx * 0.125f) % 1f
                val sx = size.width * (0.1f + idx * 0.115f)
                val sy = -30f
                val ex = sx - 220f
                val ey = size.height * 0.6f
                val mx = sx + (ex - sx) * phase
                val my = sy + (ey - sy) * phase
                val tailLen = 90f + (idx % 3) * 20f
                val ma = if (phase < 0.06f) phase / 0.06f else if (phase > 0.6f) (1f - phase) / 0.4f else 1f
                val ca = (ma * 0.55f).coerceIn(0f, 1f)
                if (ca > 0.01f) {
                    val ang = atan2(ey - sy, ex - sx)
                    val mc = meteorColors[idx % meteorColors.size]
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = ca),
                                mc.copy(alpha = ca * 0.5f),
                                Color.Transparent
                            ),
                            start = Offset(mx, my),
                            end = Offset(mx - cos(ang) * tailLen, my - sin(ang) * tailLen)
                        ),
                        start = Offset(mx, my),
                        end = Offset(mx - cos(ang) * tailLen, my - sin(ang) * tailLen),
                        strokeWidth = 1.8f
                    )
                    drawCircle(color = Color.White.copy(alpha = ca), radius = 2.5f, center = Offset(mx, my))
                }
            }
            // 从左上往右下的流星 - 4条（反向）
            repeat(4) { idx ->
                val phase = (meteorAnim2 + idx * 0.25f) % 1f
                val sx = size.width * (0.05f + idx * 0.25f)
                val sy = -20f
                val ex = sx + 150f
                val ey = size.height * 0.5f
                val mx = sx + (ex - sx) * phase
                val my = sy + (ey - sy) * phase
                val tailLen = 70f
                val ma = if (phase < 0.08f) phase / 0.08f else if (phase > 0.55f) (1f - phase) / 0.45f else 1f
                val ca = (ma * 0.35f).coerceIn(0f, 1f)
                if (ca > 0.01f) {
                    val ang = atan2(ey - sy, ex - sx)
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = ca),
                                Color(0xFFFF00FF).copy(alpha = ca * 0.4f),
                                Color.Transparent
                            ),
                            start = Offset(mx, my),
                            end = Offset(mx - cos(ang) * tailLen, my - sin(ang) * tailLen)
                        ),
                        start = Offset(mx, my),
                        end = Offset(mx - cos(ang) * tailLen, my - sin(ang) * tailLen),
                        strokeWidth = 1.5f
                    )
                    drawCircle(color = Color.White.copy(alpha = ca), radius = 2f, center = Offset(mx, my))
                }
            }
        }
    }
}

@Composable
fun EpicTitle() {
    val infiniteTransition = rememberInfiniteTransition(label = "title")
    
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        // 背景光晕
        Box(
            modifier = Modifier
                .width(280.dp)
                .height(80.dp)
                .blur(50.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = glow * 0.3f),
                            Color(0xFFFF00FF).copy(alpha = glow * 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        // 纯文字，无流光遮罩
        Text(
            text = "VIP会员专属特权",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            style = LocalTextStyle.current.copy(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFD700),
                        Color(0xFFFF00FF),
                        Color(0xFF00D9FF),
                        Color(0xFFFF00FF),
                        Color(0xFFFFD700)
                    )
                ),
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color(0xFFFF00FF).copy(alpha = glow * 0.8f),
                    offset = Offset(0f, 0f),
                    blurRadius = 30f
                )
            )
        )
    }
}

@Composable
fun SwipeableVipCard(
    card: VipCardData,
    pageOffset: Float,
    isCenter: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "card${card.title}")
    
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // 3D翻转效果参数
    val scale = lerpFloat(0.85f, 1f, 1f - pageOffset.absoluteValue.coerceIn(0f, 1f))
    val alpha = lerpFloat(0.6f, 1f, 1f - pageOffset.absoluteValue.coerceIn(0f, 1f))
    
    // Y轴旋转角度：左侧卡片向右翻转30度，右侧卡片向左翻转30度
    val rotationY = when {
        pageOffset < 0 -> lerpFloat(0f, 30f, (-pageOffset).coerceIn(0f, 1f))  // 左侧卡片
        pageOffset > 0 -> lerpFloat(0f, -30f, pageOffset.coerceIn(0f, 1f))    // 右侧卡片
        else -> 0f  // 中间卡片
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(460.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
                this.rotationY = rotationY
                cameraDistance = 12f * density
                clip = false
            }
    ) {
        // 左上角超出范围的炫酷标签 - 使用zIndex确保在最上层
        if (card.topLeftTag != null && card.topLeftTagColor != null) {
            val bounceAnimation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bounce"
            )
            
            val rotateAnimation by infiniteTransition.animateFloat(
                initialValue = -5f,
                targetValue = 5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "rotate"
            )
            
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .zIndex(100f)  // 确保在最上层
                    .offset(x = 20.dp, y = (-30).dp + bounceAnimation.dp)
                    .graphicsLayer { 
                        this.rotationZ = rotateAnimation
                        this.transformOrigin = TransformOrigin(0.5f, 1f)
                    }
            ) {
                // 标签主体 - 倾斜的旗帜形状
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = 4.dp,
                                bottomEnd = 4.dp
                            )
                        )
                        .background(
                            brush = Brush.verticalGradient(
                                colors = card.topLeftTagColor
                            )
                        )
                        .border(
                            width = 2.dp,
                            color = Color.White.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = 4.dp,
                                bottomEnd = 4.dp
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 装饰图标
                        Text(
                            text = "🔥",
                            fontSize = 16.sp,
                            modifier = Modifier.offset(y = (-2).dp)
                        )
                        
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        // 标签文字
                        Text(
                            text = card.topLeftTag,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            style = LocalTextStyle.current.copy(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black.copy(alpha = 0.7f),
                                    offset = Offset(0f, 2f),
                                    blurRadius = 4f
                                )
                            )
                        )
                    }
                }
                
                // 底部三角形装饰（像旗帜的尾巴）
                Canvas(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 8.dp)
                        .size(12.dp, 8.dp)
                ) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width / 2, size.height)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = card.topLeftTagColor.map { it.copy(alpha = 0.9f) }
                        )
                    )
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.3f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }
        
        // 动态阴影：根据翻转角度调整阴影
        val shadowAlpha = lerpFloat(0.3f, 0.8f, 1f - pageOffset.absoluteValue.coerceIn(0f, 1f))
        val shadowBlurDp = lerpFloat(20f, 50f, 1f - pageOffset.absoluteValue.coerceIn(0f, 1f))
        
        // 外发光 - 使用卡片自身颜色
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(40.dp))
                .blur(if (isCenter) shadowBlurDp.dp else 25.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            card.gradient[0].copy(alpha = if (isCenter) shadowAlpha else 0.2f),
                            card.gradient[1].copy(alpha = if (isCenter) shadowAlpha * 0.7f else 0.15f),
                            card.gradient.getOrElse(2) { card.gradient[1] }.copy(alpha = if (isCenter) shadowAlpha * 0.5f else 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(40.dp)
                )
        )
        
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(36.dp))
                .border(
                    width = if (isCenter) 2.5.dp else 1.5.dp,
                    brush = if (isCenter) {
                        Brush.sweepGradient(
                            colors = listOf(
                                card.gradient[0],
                                Color.White.copy(alpha = 0.8f),
                                card.gradient[1],
                                Color.White.copy(alpha = 0.4f),
                                card.gradient.getOrElse(2) { card.gradient[0] },
                                Color.White.copy(alpha = 0.8f),
                                card.gradient[0]
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.4f),
                                Color.White.copy(alpha = 0.15f),
                                Color.White.copy(alpha = 0.4f)
                            )
                        )
                    },
                    shape = RoundedCornerShape(36.dp)
                )
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(36.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isCenter) 24.dp else 10.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                card.gradient[0].copy(alpha = if (isCenter) 0.95f else 0.85f),
                                card.gradient[1].copy(alpha = if (isCenter) 0.98f else 0.88f),
                                card.gradient.getOrElse(2) { card.gradient[1] }.copy(alpha = if (isCenter) 0.92f else 0.82f)
                            )
                        ),
                        shape = RoundedCornerShape(36.dp)
                    )
            ) {
                // 右上角吸引人的角标
                if (card.cornerTag != null && card.cornerTagColor != null) {
                    val starShimmer by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "starShimmer"
                    )
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-8).dp, y = 8.dp)
                    ) {
                        // 角标背景 - 倾斜的丝带效果
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, topEnd = 4.dp, bottomEnd = 12.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = card.cornerTagColor
                                    )
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = Color.White.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, topEnd = 4.dp, bottomEnd = 12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = card.cornerTag,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                style = LocalTextStyle.current.copy(
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        offset = Offset(0f, 1f),
                                        blurRadius = 3f
                                    )
                                )
                            )
                        }
                        
                        // 闪烁星星效果
                        Text(
                            text = "✨",
                            fontSize = 14.sp,
                            modifier = Modifier
                                .offset(x = (-6).dp, y = (-6).dp)
                                .graphicsLayer { this.alpha = starShimmer }
                        )
                    }
                }
                
                // 能量漩涡背景
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = rotation }
                ) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2.5f
                    
                    repeat(8) { index ->
                        val radius = (50 + index * 40).dp.toPx()
                        val a = (0.15f - index * 0.015f).coerceAtLeast(0.01f)
                        
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = a),
                                    Color.Transparent
                                ),
                                center = Offset(centerX, centerY),
                                radius = radius
                            ),
                            radius = radius,
                            center = Offset(centerX, centerY)
                        )
                    }
                }
                
                // 菱形网格装饰纹理
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val spacing = 40.dp.toPx()
                    val lineColor = Color.White.copy(alpha = 0.04f)
                    var lx = 0f
                    while (lx < size.width + size.height) {
                        drawLine(lineColor, Offset(lx, 0f), Offset(lx - size.height, size.height), strokeWidth = 0.8f)
                        drawLine(lineColor, Offset(lx - size.width, 0f), Offset(lx, size.height), strokeWidth = 0.8f)
                        lx += spacing
                    }
                }

                // 四角钻石装饰
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val ds = 6.dp.toPx()
                    val mg = 24.dp.toPx()
                    val corners = listOf(
                        Offset(mg, mg), Offset(size.width - mg, mg),
                        Offset(mg, size.height - mg), Offset(size.width - mg, size.height - mg)
                    )
                    corners.forEachIndexed { idx, pos ->
                        val da = (0.3f + 0.5f * sin(shimmer * PI.toFloat() * 2f + idx * 1.5f)).coerceIn(0f, 1f)
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(pos.x, pos.y - ds)
                            lineTo(pos.x + ds, pos.y)
                            lineTo(pos.x, pos.y + ds)
                            lineTo(pos.x - ds, pos.y)
                            close()
                        }
                        drawPath(path, color = Color.White.copy(alpha = da))
                    }
                }

                // 流光效果（所有卡片都有，中间卡片更强）
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val shimmerOffset = shimmer * size.width * 2.5f
                    val shimmerAlpha = if (isCenter) 0.4f else 0.25f
                    
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = shimmerAlpha),
                                Color.Transparent
                            ),
                            startX = shimmerOffset - size.width * 1.2f,
                            endX = shimmerOffset
                        ),
                        blendMode = BlendMode.Plus
                    )
                }

                // 顶部高光条 - 玻璃质感
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.1f),
                                    Color.White.copy(alpha = 0.03f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ===== 促销徽章 =====
                    if (card.saleBadge != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFFF1744).copy(alpha = 0.9f),
                                            Color(0xFFFF6D00).copy(alpha = 0.9f)
                                        )
                                    )
                                )
                                .padding(horizontal = 14.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = card.saleBadge,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // ===== 标题 =====
                    Text(
                        text = card.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        style = LocalTextStyle.current.copy(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.6f),
                                offset = Offset(0f, 2f),
                                blurRadius = 10f
                            )
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))

                    // ===== 耀眼价格区 =====
                    val priceGlow by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "priceGlow"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.4f),
                                        Color.White.copy(alpha = 0.08f)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(vertical = 10.dp, horizontal = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 原价 + 折扣标签行
                            if (card.originalPrice != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "原价 ¥${card.originalPrice}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.5f),
                                        textDecoration = TextDecoration.LineThrough
                                    )
                                    if (card.discountLabel != null) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    brush = Brush.horizontalGradient(
                                                        colors = listOf(
                                                            Color(0xFFFF1744),
                                                            Color(0xFFFF5252)
                                                        )
                                                    )
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = card.discountLabel,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            // 主价格 - 超大耀眼数字
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // 周期小标签
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                        .alignByBaseline()
                                ) {
                                    Text(
                                        text = card.period,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                // ¥符号
                                Text(
                                    text = "¥",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.alignByBaseline(),
                                    style = LocalTextStyle.current.copy(
                                        brush = Brush.linearGradient(
                                            colors = listOf(Color(0xFFFFD700), Color.White)
                                        ),
                                        shadow = androidx.compose.ui.graphics.Shadow(
                                            color = Color(0xFFFFD700).copy(alpha = priceGlow * 0.8f),
                                            blurRadius = 16f
                                        )
                                    )
                                )
                                // 大数字 - 渐变 + 发光
                                Text(
                                    text = card.price,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.alignByBaseline(),
                                    style = LocalTextStyle.current.copy(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color.White,
                                                Color(0xFFFFD700),
                                                Color.White
                                            )
                                        ),
                                        shadow = androidx.compose.ui.graphics.Shadow(
                                            color = Color(0xFFFFD700).copy(alpha = priceGlow),
                                            blurRadius = 24f
                                        )
                                    )
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // ===== 图标区：带发光光环 =====
                    Box(contentAlignment = Alignment.Center) {
                        // 外层发光圈
                        val iconGlow by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 0.8f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1800, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "iconGlow"
                        )
                        Canvas(modifier = Modifier.size(90.dp)) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = iconGlow * 0.25f),
                                        Color.White.copy(alpha = iconGlow * 0.08f),
                                        Color.Transparent
                                    )
                                ),
                                radius = size.minDimension / 2
                            )
                            // 细光环
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.6f),
                                        Color.White.copy(alpha = 0.1f),
                                        Color.White.copy(alpha = 0.6f)
                                    )
                                ),
                                radius = size.minDimension / 2 - 4f,
                                style = Stroke(width = 1.5f)
                            )
                        }
                        Text(
                            text = card.icon,
                            fontSize = 52.sp,
                            style = LocalTextStyle.current.copy(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black.copy(alpha = 0.3f),
                                    offset = Offset(0f, 4f),
                                    blurRadius = 8f
                                )
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // ===== 渐变分割线 =====
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(1.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.5f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // ===== 权益列表：毛玻璃卡片 =====
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        card.benefits.forEachIndexed { idx, benefit ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.08f + idx * 0.015f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                // 渐变勾选圆
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.5f),
                                                    Color.White.copy(alpha = 0.2f)
                                                )
                                            )
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = Color.White.copy(alpha = 0.6f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "✓",
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = benefit,
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontWeight = FontWeight.SemiBold,
                                    style = LocalTextStyle.current.copy(
                                        shadow = androidx.compose.ui.graphics.Shadow(
                                            color = Color.Black.copy(alpha = 0.5f),
                                            offset = Offset(0f, 1f),
                                            blurRadius = 4f
                                        )
                                    )
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // ===== 底部CTA提示 =====
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.15f),
                                        Color.White.copy(alpha = 0.08f)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.4f),
                                        Color.White.copy(alpha = 0.15f)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✨ 滑动选择 · 点击下方购买",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Perspective3DVipCard(
    card: VipCardData,
    isCenter: Boolean,
    scale: Float,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "card${card.title}")
    
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = Modifier
            .width(if (isCenter) 260.dp else 240.dp)
            .height(if (isCenter) 420.dp else 380.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        // 外发光（中间卡片更强）
        if (isCenter) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .blur(50.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFD700).copy(alpha = 0.8f),
                                Color(0xFFFF9500).copy(alpha = 0.6f)
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
            )
        }
        
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clip(RoundedCornerShape(32.dp))
                .border(
                    width = if (isCenter) 2.5.dp else 1.5.dp,
                    brush = if (isCenter) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFD700),
                                Color(0xFFFFB800),
                                Color(0xFFFFD700)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.5f),
                                Color.White.copy(alpha = 0.2f),
                                Color.White.copy(alpha = 0.5f)
                            )
                        )
                    },
                    shape = RoundedCornerShape(32.dp)
                )
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isCenter) 24.dp else 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = card.gradient.map { it.copy(alpha = if (isCenter) 0.98f else 0.85f) }
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
            ) {
                // 能量漩涡背景
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = rotation }
                ) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2.5f
                    
                    repeat(8) { index ->
                        val radius = (50 + index * 40).dp.toPx()
                        val alpha = 0.15f - index * 0.015f
                        
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = alpha),
                                    Color.Transparent
                                ),
                                center = Offset(centerX, centerY),
                                radius = radius
                            ),
                            radius = radius,
                            center = Offset(centerX, centerY)
                        )
                    }
                }
                
                // 流光效果（所有卡片都有，中间卡片更强）
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val shimmerOffset = shimmer * size.width * 2.5f
                    val shimmerAlpha = if (isCenter) 0.4f else 0.25f
                    
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = shimmerAlpha),
                                Color.Transparent
                            ),
                            startX = shimmerOffset - size.width * 1.2f,
                            endX = shimmerOffset
                        ),
                        blendMode = BlendMode.Plus
                    )
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isCenter) 20.dp else 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = card.title,
                        fontSize = if (isCenter) 24.sp else 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        style = LocalTextStyle.current.copy(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.5f),
                                offset = Offset(0f, 2f),
                                blurRadius = 8f
                            )
                        )
                    )
                    
                    Text(
                        text = card.icon,
                        fontSize = if (isCenter) 80.sp else 60.sp,
                        modifier = Modifier.padding(vertical = if (isCenter) 12.dp else 8.dp)
                    )
                    
                    if (isCenter) {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            card.benefits.take(4).forEach { benefit ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "✓",
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = benefit,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.95f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(if (isCenter) 16.dp else 12.dp))
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = card.period,
                            fontSize = if (isCenter) 16.sp else 14.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "¥",
                                fontSize = if (isCenter) 28.sp else 22.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = card.price,
                                fontSize = if (isCenter) 52.sp else 40.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                style = LocalTextStyle.current.copy(
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.4f),
                                        offset = Offset(0f, 3f),
                                        blurRadius = 10f
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactVipCard(
    card: VipCardData,
    isCenter: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "card${card.title}")
    
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isCenter) 1.05f else 0.9f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .width(115.dp)
            .height(300.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        if (isCenter) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
                    .blur(35.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = card.gradient.map { it.copy(alpha = 0.7f) }
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            )
        }
        
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .border(
                    width = if (isCenter) 2.dp else 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isCenter) 0.8f else 0.4f),
                            Color.White.copy(alpha = if (isCenter) 0.4f else 0.2f),
                            Color.White.copy(alpha = if (isCenter) 0.8f else 0.4f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isCenter) 16.dp else 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = card.gradient.map { it.copy(alpha = if (isCenter) 0.95f else 0.8f) }
                        )
                    )
            ) {
                if (isCenter) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val shimmerOffset = shimmer * size.width * 2.5f
                        
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.3f),
                                    Color.Transparent
                                ),
                                startX = shimmerOffset - size.width * 1.2f,
                                endX = shimmerOffset
                            ),
                            blendMode = BlendMode.Plus
                        )
                    }
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = card.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        style = LocalTextStyle.current.copy(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.4f),
                                offset = Offset(0f, 1f),
                                blurRadius = 4f
                            )
                        )
                    )
                    
                    Text(
                        text = card.icon,
                        fontSize = 42.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        card.benefits.take(3).forEach { benefit ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "✓",
                                        fontSize = 8.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = benefit,
                                    fontSize = 8.sp,
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = card.period,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "¥",
                                fontSize = 14.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = card.price,
                                fontSize = 28.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                style = LocalTextStyle.current.copy(
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.3f),
                                        offset = Offset(0f, 1f),
                                        blurRadius = 4f
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumVipCard(
    card: VipCardData,
    pageOffset: Float,
    isCenter: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "card${card.title}")
    
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    val scale = lerpFloat(0.7f, 1f, 1f - pageOffset.absoluteValue.coerceIn(0f, 1f))
    val alpha = lerpFloat(0.5f, 1f, 1f - pageOffset.absoluteValue.coerceIn(0f, 1f))
    val rotationY = lerpFloat(0f, 25f, pageOffset.coerceIn(-1f, 1f))
    
    Box(
        modifier = Modifier
            .width(240.dp)
            .height(400.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
                this.rotationY = rotationY
                cameraDistance = 12f * density
            }
    ) {
        if (isCenter) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .blur(40.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = card.gradient.map { it.copy(alpha = 0.7f) }
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
            )
        }
        
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isCenter) 0.8f else 0.5f),
                            Color.White.copy(alpha = if (isCenter) 0.4f else 0.2f),
                            Color.White.copy(alpha = if (isCenter) 0.8f else 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                ),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isCenter) 20.dp else 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = card.gradient.map { it.copy(alpha = if (isCenter) 0.95f else 0.85f) }
                        )
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val shimmerOffset = shimmer * size.width * 2.5f
                    
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.4f),
                                Color.Transparent
                            ),
                            startX = shimmerOffset - size.width * 1.2f,
                            endX = shimmerOffset
                        ),
                        blendMode = BlendMode.Plus
                    )
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = card.title,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        style = LocalTextStyle.current.copy(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.5f),
                                offset = Offset(0f, 2f),
                                blurRadius = 8f
                            )
                        )
                    )
                    
                    Text(
                        text = card.icon,
                        fontSize = 70.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        card.benefits.take(4).forEach { benefit ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "✓",
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = benefit,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = card.period,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "¥",
                                fontSize = 20.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = card.price,
                                fontSize = 40.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                style = LocalTextStyle.current.copy(
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.4f),
                                        offset = Offset(0f, 2f),
                                        blurRadius = 8f
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumPurchaseButton(
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "button")
    
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )
    
    Box(
        modifier = Modifier
            .width(320.dp)
            .height(60.dp),
        contentAlignment = Alignment.Center
    ) {
        // 外层呼吸光晕 - 用Canvas绘制避免矩形阴影
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.99f }
        ) {
            val glowA = (glowPulse * 0.45f).coerceIn(0f, 1f)
            // 左侧光晕
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD700).copy(alpha = glowA),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.2f, size.height / 2),
                    radius = size.height * 1.2f
                ),
                radius = size.height * 1.2f,
                center = Offset(size.width * 0.2f, size.height / 2)
            )
            // 中间光晕
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF00FF).copy(alpha = glowA * 0.8f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height / 2),
                    radius = size.height * 1.1f
                ),
                radius = size.height * 1.1f,
                center = Offset(size.width * 0.5f, size.height / 2)
            )
            // 右侧光晕
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00D9FF).copy(alpha = glowA),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.8f, size.height / 2),
                    radius = size.height * 1.2f
                ),
                radius = size.height * 1.2f,
                center = Offset(size.width * 0.8f, size.height / 2)
            )
        }
        
        // 按钮主体 - 渐变填充 + 边框
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(30.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = 0.15f),
                            Color(0xFFFF00FF).copy(alpha = 0.12f),
                            Color(0xFF00D9FF).copy(alpha = 0.15f)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = 0.9f),
                            Color(0xFFFF00FF).copy(alpha = 0.8f),
                            Color(0xFF00D9FF).copy(alpha = 0.9f)
                        )
                    ),
                    shape = RoundedCornerShape(30.dp)
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            // 流光扫过按钮背景
            Canvas(modifier = Modifier.fillMaxSize()) {
                val off = shimmer * size.width * 2.5f
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        startX = off - size.width * 0.8f,
                        endX = off
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(30.dp.toPx())
                )
            }
            
            Box {
                // 底层：渐变色文字
                Text(
                    text = "立即购买",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    style = LocalTextStyle.current.copy(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFD700),
                                Color(0xFFFF00FF),
                                Color(0xFF00D9FF),
                                Color(0xFFFF00FF),
                                Color(0xFFFFD700)
                            )
                        ),
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color(0xFFFF00FF).copy(alpha = 0.5f),
                            blurRadius = 16f
                        )
                    )
                )
                
                // 顶层：白色流光
                Text(
                    text = "立即购买",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier
                        .graphicsLayer { alpha = 0.99f }
                        .drawWithContent {
                            drawContent()
                            val shimmerOffset = shimmer * size.width * 2.5f
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.7f),
                                        Color.Transparent
                                    ),
                                    startX = shimmerOffset - size.width * 0.8f,
                                    endX = shimmerOffset + size.width * 0.2f
                                ),
                                blendMode = BlendMode.SrcIn
                            )
                        }
                )
            }
        }
    }
}


@Composable
fun VipBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "vipBadge")
    
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    Box(
        modifier = Modifier
            .height(40.dp)
            .width(100.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFFF00FF),  // 霓虹粉
                        Color(0xFF00FFFF),  // 霓虹青
                        Color(0xFFFF00FF)   // 霓虹粉
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF).copy(alpha = 0.6f),
                        Color(0xFFFFFFFF).copy(alpha = 0.3f),
                        Color(0xFFFFFFFF).copy(alpha = 0.6f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // 使用单个Text + drawWithContent实现渐变文字 + 流光效果
        Text(
            text = "VIP",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFFFD700),  // 纯金色，与背景形成强烈对比
            style = LocalTextStyle.current.copy(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color(0xFF000000).copy(alpha = 0.8f),
                    offset = Offset(0f, 2f),
                    blurRadius = 8f
                )
            ),
            modifier = Modifier
                .graphicsLayer(alpha = 0.99f)
                .drawWithContent {
                    // 先绘制渐变文字
                    drawContent()
                    
                    // 创建一个图层来绘制流光效果
                    drawContext.canvas.saveLayer(
                        androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height),
                        androidx.compose.ui.graphics.Paint()
                    )
                    
                    // 再次绘制文字作为遮罩
                    drawContent()
                    
                    // 绘制流光渐变，使用SrcIn只保留与文字重叠的部分
                    val shimmerOffset = shimmer * size.width * 2.5f
                    
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.8f),
                                Color.Transparent
                            ),
                            startX = shimmerOffset - size.width * 0.8f,
                            endX = shimmerOffset + size.width * 0.2f
                        ),
                        blendMode = BlendMode.SrcIn
                    )
                    
                    drawContext.canvas.restore()
                }
        )
    }
}


// 科技感价格标签组件 - 悬浮在卡片底部
@Composable
fun PriceBadge(
    price: String,
    period: String,
    isCenter: Boolean,
    gradient: List<Color>
) {
    val infiniteTransition = rememberInfiniteTransition(label = "priceBadge")
    
    // 呼吸光效
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )
    
    // 流光动画
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    // 边框扫描动画
    val scanAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanAngle"
    )
    
    Box(
        modifier = Modifier
            .width(if (isCenter) 200.dp else 170.dp)  // 增大宽度
            .height(if (isCenter) 80.dp else 70.dp)   // 增大高度
    ) {
        // 外层光晕（呼吸效果）
        if (isCenter) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(25.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                gradient[0].copy(alpha = glowPulse * 0.8f),
                                gradient[1].copy(alpha = glowPulse * 0.5f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(40.dp)
                    )
            )
        }
        
        // 主体容器
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(40.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0A0A1E).copy(alpha = 0.95f),
                            Color(0xFF1A1A3E).copy(alpha = 0.95f)
                        )
                    )
                )
                .border(
                    width = if (isCenter) 3.dp else 2.5.dp,  // 增加边框宽度
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            gradient[0].copy(alpha = 0.9f),
                            gradient[1].copy(alpha = 0.9f),
                            gradient[2].copy(alpha = 0.9f),
                            gradient[0].copy(alpha = 0.9f)
                        ),
                        center = Offset(0.5f, 0.5f)
                    ),
                    shape = RoundedCornerShape(40.dp)
                )
        ) {
            // 扫描线动画
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                
                // 旋转扫描光束
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            gradient[0].copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        center = Offset(centerX, centerY)
                    ),
                    startAngle = scanAngle,
                    sweepAngle = 60f,
                    useCenter = true
                )
            }
            
            // 流光效果
            Canvas(modifier = Modifier.fillMaxSize()) {
                val shimmerOffset = shimmer * size.width * 2f
                
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        startX = shimmerOffset - size.width,
                        endX = shimmerOffset
                    ),
                    blendMode = BlendMode.Plus
                )
            }
            
            // 价格内容
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = if (isCenter) 10.dp else 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 周期标签
                Text(
                    text = period,
                    fontSize = if (isCenter) 13.sp else 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = gradient[0].copy(alpha = 0.9f),
                    style = LocalTextStyle.current.copy(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = gradient[0].copy(alpha = 0.5f),
                            offset = Offset(0f, 0f),
                            blurRadius = 8f
                        )
                    )
                )
                
                Spacer(modifier = Modifier.height(3.dp))
                
                // 价格显示
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "¥",
                        fontSize = if (isCenter) 24.sp else 20.sp,
                        fontWeight = FontWeight.Black,
                        style = LocalTextStyle.current.copy(
                            brush = Brush.linearGradient(
                                colors = gradient
                            ),
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = gradient[1].copy(alpha = glowPulse * 0.8f),
                                offset = Offset(0f, 0f),
                                blurRadius = 12f
                            )
                        ),
                        modifier = Modifier.padding(bottom = if (isCenter) 4.dp else 3.dp)
                    )
                    
                    Text(
                        text = price,
                        fontSize = if (isCenter) 36.sp else 32.sp,  // 增大字体
                        fontWeight = FontWeight.Black,
                        style = LocalTextStyle.current.copy(
                            brush = Brush.linearGradient(
                                colors = gradient
                            ),
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = gradient[1].copy(alpha = glowPulse * 0.8f),
                                offset = Offset(0f, 0f),
                                blurRadius = 15f
                            )
                        ),
                        maxLines = 1
                    )
                }
            }
            
            // 四角装饰线（科技感）
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cornerLength = 18f  // 增大装饰线长度
                val strokeWidth = 3.5f  // 增加线条宽度
                val cornerColor = gradient[0]
                
                // 左上角
                drawLine(
                    color = cornerColor,
                    start = Offset(0f, cornerLength),
                    end = Offset(0f, 0f),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = cornerColor,
                    start = Offset(0f, 0f),
                    end = Offset(cornerLength, 0f),
                    strokeWidth = strokeWidth
                )
                
                // 右上角
                drawLine(
                    color = cornerColor,
                    start = Offset(size.width - cornerLength, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = cornerColor,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, cornerLength),
                    strokeWidth = strokeWidth
                )
                
                // 左下角
                drawLine(
                    color = cornerColor,
                    start = Offset(0f, size.height - cornerLength),
                    end = Offset(0f, size.height),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = cornerColor,
                    start = Offset(0f, size.height),
                    end = Offset(cornerLength, size.height),
                    strokeWidth = strokeWidth
                )
                
                // 右下角
                drawLine(
                    color = cornerColor,
                    start = Offset(size.width - cornerLength, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = cornerColor,
                    start = Offset(size.width, size.height - cornerLength),
                    end = Offset(size.width, size.height),
                    strokeWidth = strokeWidth
                )
            }
        }
    }
}


// 角标价格标签组件 - 悬浮在卡片右上角
@Composable
fun CornerPriceTag(
    price: String,
    period: String,
    isCenter: Boolean,
    gradient: List<Color>
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cornerTag")
    
    // 呼吸光效
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )
    
    // 旋转动画
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = Modifier
            .width(if (isCenter) 100.dp else 90.dp)
            .height(if (isCenter) 100.dp else 90.dp)
    ) {
        // 外层光晕（呼吸效果）
        if (isCenter) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(20.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                gradient[0].copy(alpha = glowPulse * 0.6f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }
        
        // 旋转的背景圆环
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotation }
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.minDimension / 2
            
            // 绘制渐变圆环
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        gradient[0].copy(alpha = 0.3f),
                        gradient[1].copy(alpha = 0.5f),
                        gradient[2].copy(alpha = 0.3f),
                        gradient[0].copy(alpha = 0.3f)
                    ),
                    center = Offset(centerX, centerY)
                ),
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 3f)
            )
        }
        
        // 主体圆形标签
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            gradient[0].copy(alpha = 0.95f),
                            gradient[1].copy(alpha = 0.95f),
                            gradient[2].copy(alpha = 0.95f)
                        )
                    )
                )
                .border(
                    width = if (isCenter) 3.dp else 2.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.8f),
                            Color.White.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.8f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 周期标签
                Text(
                    text = period,
                    fontSize = if (isCenter) 11.sp else 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = LocalTextStyle.current.copy(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = Offset(0f, 1f),
                            blurRadius = 4f
                        )
                    )
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // 价格显示
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "¥",
                        fontSize = if (isCenter) 16.sp else 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        style = LocalTextStyle.current.copy(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = glowPulse * 0.6f),
                                offset = Offset(0f, 0f),
                                blurRadius = 8f
                            )
                        )
                    )
                    
                    Text(
                        text = price,
                        fontSize = if (isCenter) 22.sp else 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        style = LocalTextStyle.current.copy(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = glowPulse * 0.6f),
                                offset = Offset(0f, 0f),
                                blurRadius = 10f
                            )
                        ),
                        maxLines = 1
                    )
                }
            }
            
            // 闪光效果
            Canvas(modifier = Modifier.fillMaxSize()) {
                val shimmerAngle = (rotation / 360f) * 2 * PI.toFloat()
                val shimmerX = center.x + cos(shimmerAngle) * size.minDimension * 0.3f
                val shimmerY = center.y + sin(shimmerAngle) * size.minDimension * 0.3f
                
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = size.minDimension * 0.15f,
                    center = Offset(shimmerX, shimmerY),
                    blendMode = BlendMode.Plus
                )
            }
        }
    }
}


// 豪华加载动画
@Composable
fun CuteLoadingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    // 三层环 - 不同速度/方向
    val ring1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "ring1"
    )
    val ring2 by infiniteTransition.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
        label = "ring2"
    )
    val ring3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart),
        label = "ring3"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    val textShimmer by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "textShimmer"
    )

    // 进度动画 0→1
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "progress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A0B2E),
                        Color(0xFF0A0015),
                        Color(0xFF000000)
                    ),
                    radius = 900f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // 背景粒子
        Canvas(modifier = Modifier.fillMaxSize()) {
            val time = ring1 / 360f
            repeat(40) { i ->
                val angle = (i * 9f + time * 360f) * PI.toFloat() / 180f
                val dist = 120f + (i % 7) * 35f
                val px = center.x + cos(angle) * dist
                val py = center.y + sin(angle) * dist
                val a = (0.15f + 0.2f * sin(time * PI.toFloat() * 2f + i * 0.5f)).coerceIn(0f, 1f)
                val r = 1f + (i % 3)
                drawCircle(
                    color = when (i % 3) {
                        0 -> Color(0xFFFFD700).copy(alpha = a)
                        1 -> Color(0xFFFF00FF).copy(alpha = a)
                        else -> Color(0xFF00D9FF).copy(alpha = a)
                    },
                    radius = r,
                    center = Offset(px, py)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 多层旋转环 + 中心图标
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                // 最外层环 - 虚线感
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = ring3 }
                ) {
                    val r = size.minDimension / 2 - 4f
                    repeat(24) { i ->
                        val a = (i * 15f) * PI.toFloat() / 180f
                        val len = 8f
                        drawLine(
                            color = Color(0xFF00D9FF).copy(alpha = 0.4f),
                            start = Offset(center.x + cos(a) * (r - len), center.y + sin(a) * (r - len)),
                            end = Offset(center.x + cos(a) * r, center.y + sin(a) * r),
                            strokeWidth = 2f
                        )
                    }
                }

                // 中层环 - 渐变扫描
                Canvas(
                    modifier = Modifier
                        .size(130.dp)
                        .graphicsLayer { rotationZ = ring2 }
                ) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFFFF00FF).copy(alpha = 0.7f),
                                Color.Transparent,
                                Color(0xFF00D9FF).copy(alpha = 0.5f),
                                Color.Transparent,
                                Color(0xFFFF00FF).copy(alpha = 0.7f)
                            )
                        ),
                        radius = size.minDimension / 2,
                        style = Stroke(width = 3.5f)
                    )
                }

                // 内层环 - 金色主环
                Canvas(
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer { rotationZ = ring1 }
                ) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFFFFD700).copy(alpha = 0.9f),
                                Color(0xFFFF9500).copy(alpha = 0.3f),
                                Color(0xFFFFD700).copy(alpha = 0.9f),
                                Color(0xFFFF9500).copy(alpha = 0.3f),
                                Color(0xFFFFD700).copy(alpha = 0.9f)
                            )
                        ),
                        radius = size.minDimension / 2,
                        style = Stroke(width = 5f)
                    )
                    // 光点
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = Offset(center.x, center.y - size.minDimension / 2)
                    )
                }

                // 中心：脉冲背景 + 皇冠 + VIP
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer {
                            scaleX = pulse
                            scaleY = pulse
                        }
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFD700).copy(alpha = 0.35f),
                                    Color(0xFF1A0B2E).copy(alpha = 0.95f)
                                )
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFD700).copy(alpha = 0.8f),
                                    Color(0xFFFF9500).copy(alpha = 0.4f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "👑", fontSize = 22.sp)
                        Text(
                            text = "VIP",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            style = LocalTextStyle.current.copy(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFFFFD700), Color(0xFFFFAA00))
                                )
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 进度条
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFFD700), Color(0xFFFF00FF), Color(0xFF00D9FF))
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 文字带流光
            Box {
                Text(
                    text = "正在加载VIP专属内容",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    style = LocalTextStyle.current.copy(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFD700).copy(alpha = 0.9f),
                                Color.White.copy(alpha = 0.7f),
                                Color(0xFF00D9FF).copy(alpha = 0.9f)
                            )
                        ),
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color(0xFFFFD700).copy(alpha = 0.4f),
                            blurRadius = 12f
                        )
                    )
                )
                // 流光
                Text(
                    text = "正在加载VIP专属内容",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .graphicsLayer { alpha = 0.99f }
                        .drawWithContent {
                            drawContent()
                            val off = textShimmer * size.width * 3f
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.6f),
                                        Color.Transparent
                                    ),
                                    startX = off - size.width,
                                    endX = off
                                ),
                                blendMode = BlendMode.SrcIn
                            )
                        }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 副标题
            Text(
                text = "尊享特权即将呈现 ✨",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.4f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
