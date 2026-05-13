// AvatarFrameShopScreen.kt - 头像框商城界面
package com.example.funlife.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.funlife.data.model.InventoryItemType
import com.example.funlife.data.model.ItemRarity
import com.example.funlife.data.model.ShopItem
import com.example.funlife.ui.components.AvatarSizes
import com.example.funlife.viewmodel.ShopViewModel
import kotlinx.coroutines.launch

/**
 * 头像框商城主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarFrameShopScreen(
    viewModel: ShopViewModel,
    onNavigateBack: () -> Unit
) {
    val frames by viewModel.avatarFrames.collectAsState()
    val ownedFrames by viewModel.userOwnedFrames.collectAsState()
    val userCoins by viewModel.userCoins.collectAsState()
    val isVip by viewModel.isUserVip.collectAsState()
    val message by viewModel.message.collectAsState()
    
    var selectedCategory by remember { mutableStateOf("全部") }
    var selectedRarity by remember { mutableStateOf("全部") }
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var selectedFrame by remember { mutableStateOf<ShopItem?>(null) }
    
    // 滚动状态
    val scrollState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    
    // 计算滚动偏移量，用于头部动画
    val scrollOffset = remember { derivedStateOf { scrollState.firstVisibleItemScrollOffset } }
    val isScrolled = remember { derivedStateOf { scrollState.firstVisibleItemIndex > 0 || scrollOffset.value > 0 } }
    
    // 头部高度动画
    val headerHeight by animateDpAsState(
        targetValue = if (isScrolled.value) 56.dp else 120.dp,
        animationSpec = tween(300),
        label = "headerHeight"
    )
    
    // 头部透明度动画
    val headerAlpha by animateFloatAsState(
        targetValue = if (isScrolled.value) 0.95f else 1f,
        animationSpec = tween(300),
        label = "headerAlpha"
    )
    
    // 显示消息提示
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF3E0),
                        Color(0xFFFFE0B2),
                        Color(0xFFFFFFFF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 固定的顶部栏（带滚动效果）
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .zIndex(10f),
                shadowElevation = if (isScrolled.value) 8.dp else 0.dp,
                color = Color(0xFFFF9800).copy(alpha = headerAlpha)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 返回按钮
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                    
                    // 标题（滚动时居中显示）
                    if (!isScrolled.value) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Text(
                                text = "头像框商城",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "让你的头像更出众",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                    
                    // 滚动时的简化标题
                    if (isScrolled.value) {
                        Text(
                            text = "头像框商城",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(start = 48.dp)
                        )
                    }
                    
                    // 金币显示
                    Surface(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.3f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "💰",
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${userCoins?.coins ?: 0}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            
            // VIP提示卡片
            if (isVip && !isScrolled.value) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .animateContentSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFD700).copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "VIP",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "VIP专享特权",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD700)
                            )
                            Text(
                                "所有头像框享受8折优惠",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFD700).copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
            
            // 筛选栏（固定在顶部）
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(5f),
                shadowElevation = if (isScrolled.value) 4.dp else 0.dp,
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    // 类别筛选
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listOf("全部", "动态", "静态")) { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { 
                                    selectedCategory = category
                                    scope.launch {
                                        scrollState.animateScrollToItem(0)
                                    }
                                },
                                label = { 
                                    Text(
                                        when(category) {
                                            "动态" -> "✨ 动态"
                                            "静态" -> "🖼️ 静态"
                                            else -> category
                                        }
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFF9800),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 稀有度筛选
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listOf("全部", "普通", "稀有", "史诗", "传说")) { rarity ->
                            FilterChip(
                                selected = selectedRarity == rarity,
                                onClick = { 
                                    selectedRarity = rarity
                                    scope.launch {
                                        scrollState.animateScrollToItem(0)
                                    }
                                },
                                label = { Text(rarity) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when(rarity) {
                                        "普通" -> Color(0xFF9E9E9E)
                                        "稀有" -> Color(0xFF2196F3)
                                        "史诗" -> Color(0xFF9C27B0)
                                        "传说" -> Color(0xFFFF9800)
                                        else -> Color(0xFFFF9800)
                                    },
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 头像框网格
            val filteredFrames = frames.filter { frame ->
                (selectedCategory == "全部" || 
                 (selectedCategory == "动态" && frame.isAnimated) ||
                 (selectedCategory == "静态" && !frame.isAnimated)) &&
                (selectedRarity == "全部" || 
                 frame.rarity == selectedRarity.uppercase())
            }
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = scrollState,
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = filteredFrames,
                    key = { it.id }
                ) { frame ->
                    val isOwned = ownedFrames.any { it.frameId == frame.id }
                    AvatarFrameCard(
                        frame = frame,
                        isVip = isVip,
                        isOwned = isOwned,
                        onClick = {
                            selectedFrame = frame
                            showPurchaseDialog = true
                        }
                    )
                }
            }
        }
        
        // Snackbar提示
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
    
    // 购买确认对话框
    if (showPurchaseDialog && selectedFrame != null) {
        PurchaseFrameDialog(
            frame = selectedFrame!!,
            isVip = isVip,
            isOwned = ownedFrames.any { it.frameId == selectedFrame!!.id },
            onDismiss = { showPurchaseDialog = false },
            onPurchase = {
                viewModel.purchaseAvatarFrame(selectedFrame!!)
                showPurchaseDialog = false
            },
            onEquip = {
                viewModel.equipAvatarFrame(selectedFrame!!.id)
                showPurchaseDialog = false
            }
        )
    }
}

/**
 * 头像框卡片
 */
@Composable
fun AvatarFrameCard(
    frame: ShopItem,
    isVip: Boolean,
    isOwned: Boolean,
    onClick: () -> Unit
) {
    val price = if (isVip) frame.vipPrice else frame.price
    
    // 卡片缩放动画
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardScale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFFFF8F0)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 头像框预览
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                ) {
                    // 示例头像
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(55.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFFE0B2),
                                            Color(0xFFFFCC80)
                                        )
                                    )
                                )
                        )
                    }
                    
                    // 头像框
                    frame.assetPath?.let { path ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data("file:///android_asset/$path")
                                .build(),
                            contentDescription = frame.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    
                    // 动态标识
                    if (frame.isAnimated) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp),
                            shape = CircleShape,
                            color = Color(0xFFFF9800),
                            shadowElevation = 2.dp
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "动态",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(14.dp)
                            )
                        }
                    }
                    
                    // 稀有度标识
                    RarityBadge(
                        rarity = ItemRarity.valueOf(frame.rarity),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 价格和状态
                if (isOwned) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "已拥有",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "已拥有",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "💰",
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "$price",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800)
                            )
                        }
                        
                        if (isVip && frame.price != frame.vipPrice) {
                            Text(
                                text = "原价${frame.price}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textDecoration = TextDecoration.LineThrough,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 稀有度徽章
 */
@Composable
fun RarityBadge(rarity: ItemRarity, modifier: Modifier = Modifier) {
    val (color, text, emoji) = when (rarity) {
        ItemRarity.COMMON -> Triple(Color(0xFF9E9E9E), "普通", "⚪")
        ItemRarity.RARE -> Triple(Color(0xFF2196F3), "稀有", "💎")
        ItemRarity.EPIC -> Triple(Color(0xFF9C27B0), "史诗", "💜")
        ItemRarity.LEGENDARY -> Triple(Color(0xFFFF9800), "传说", "⭐")
    }
    
    Surface(
        modifier = modifier,
        color = color,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = emoji,
                fontSize = 8.sp
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 购买确认对话框
 */
@Composable
fun PurchaseFrameDialog(
    frame: ShopItem,
    isVip: Boolean,
    isOwned: Boolean,
    onDismiss: () -> Unit,
    onPurchase: () -> Unit,
    onEquip: () -> Unit
) {
    val price = if (isVip) frame.vipPrice else frame.price
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    frame.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                RarityBadge(rarity = ItemRarity.valueOf(frame.rarity))
            }
        },
        text = {
            Column {
                // 头像框预览
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFF8F0),
                                    Color(0xFFFFE0B2)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // 示例头像
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFE0B2),
                                        Color(0xFFFFCC80)
                                    )
                                )
                            )
                    )
                    
                    // 头像框
                    frame.assetPath?.let { path ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data("file:///android_asset/$path")
                                .build(),
                            contentDescription = frame.name,
                            modifier = Modifier.size(140.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    
                    // 动态标识
                    if (frame.isAnimated) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFFF9800),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "动态",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "动态",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 描述
                Text(
                    frame.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 价格信息
                if (!isOwned) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFF9800).copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "价格:",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💰",
                                    fontSize = 24.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$price",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9800)
                                )
                            }
                        }
                    }
                    
                    if (isVip && frame.price != frame.vipPrice) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "VIP",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "VIP优惠已享受",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFD700)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "原价${frame.price}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = if (isOwned) onEquip else onPurchase,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOwned) Color(0xFF4CAF50) else Color(0xFFFF9800)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    if (isOwned) Icons.Default.CheckCircle else Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isOwned) "装备" else "购买",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("取消")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
