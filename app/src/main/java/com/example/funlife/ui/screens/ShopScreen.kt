// ShopScreen.kt - 商城页面
package com.example.funlife.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.funlife.data.model.ShopItem
import com.example.funlife.viewmodel.ShopViewModel
import com.example.funlife.viewmodel.HabitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    shopViewModel: ShopViewModel = viewModel(),
    habitViewModel: HabitViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val shopItems by shopViewModel.shopItems.collectAsState()
    val userCoins by shopViewModel.userCoins.collectAsState()
    val habits by habitViewModel.habits.collectAsState()
    
    var selectedItem by remember { mutableStateOf<ShopItem?>(null) }
    var selectedHabitId by remember { mutableStateOf<Int?>(null) }
    var showPurchaseSuccess by remember { mutableStateOf(false) }
    var purchasedItemName by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🛒", fontSize = 24.sp)
                        Text(
                            "商城",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // 金币显示
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFFD700).copy(alpha = 0.2f),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💰", fontSize = 20.sp)
                            Text(
                                "${userCoins?.coins ?: 0}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD700)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 提示卡片
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💡", fontSize = 40.sp)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "如何获得金币？",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "• 每日打卡奖励 10 金币\n• 连续7天打卡额外奖励 50 金币\n• 补卡奖励 5 金币",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
                
                // 分类标题
                item {
                    Text(
                        "热门商品",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                // 商品列表
                items(shopItems) { item ->
                    ShopItemCard(
                        item = item,
                        userCoins = userCoins?.coins ?: 0,
                        onClick = {
                            if (item.type == "makeup_card") {
                                // 补卡卡片需要选择习惯
                                selectedItem = item
                            } else {
                                // 其他商品直接购买
                                shopViewModel.purchaseItem(item)
                                purchasedItemName = item.name
                                showPurchaseSuccess = true
                            }
                        }
                    )
                }
            }
            
            // 购买成功动画
            if (showPurchaseSuccess) {
                PurchaseSuccessAnimation(
                    itemName = purchasedItemName,
                    onDismiss = { showPurchaseSuccess = false }
                )
            }
        }
    }
    
    // 选择习惯对话框
    if (selectedItem != null && selectedItem!!.type == "makeup_card") {
        AlertDialog(
            onDismissRequest = { selectedItem = null },
            title = { Text("选择习惯") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("为哪个习惯购买补卡卡片？")
                    habits.forEach { habit ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedHabitId = habit.id
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedHabitId == habit.id)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(habit.icon, fontSize = 24.sp)
                                Text(habit.name)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedHabitId?.let { habitId ->
                            shopViewModel.purchaseItem(selectedItem!!, habitId)
                        }
                        selectedItem = null
                        selectedHabitId = null
                    },
                    enabled = selectedHabitId != null
                ) {
                    Text("确认购买")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    selectedItem = null
                    selectedHabitId = null
                }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun ShopItemCard(
    item: ShopItem,
    userCoins: Int,
    onClick: () -> Unit
) {
    val canAfford = userCoins >= item.price
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canAfford, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (canAfford)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (canAfford) 6.dp else 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 背景装饰
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 30.dp, y = (-20).dp)
                    .size(120.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // 图标
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item.icon, fontSize = 36.sp)
                    }
                    
                    // 商品信息
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            item.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            item.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // 价格标签
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (canAfford)
                                Color(0xFFFFD700).copy(alpha = 0.2f)
                            else
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💰", fontSize = 18.sp)
                                Text(
                                    "${item.price}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (canAfford)
                                        Color(0xFFFFD700)
                                    else
                                        MaterialTheme.colorScheme.error
                                )
                                Text(
                                    "金币",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (canAfford)
                                        Color(0xFFFFD700).copy(alpha = 0.8f)
                                    else
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
                
                // 购买按钮
                Button(
                    onClick = onClick,
                    enabled = canAfford,
                    modifier = Modifier
                        .height(56.dp)
                        .widthIn(min = 100.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canAfford)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = if (canAfford) 4.dp else 0.dp
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            if (canAfford) Icons.Default.ShoppingCart else Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            if (canAfford) "购买" else "不足",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun PurchaseSuccessAnimation(
    itemName: String,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(true) }
    var scale by remember { mutableStateOf(0f) }
    var alpha by remember { mutableStateOf(1f) }
    
    LaunchedEffect(Unit) {
        // 放大动画
        animate(0f, 1.2f, animationSpec = tween(300)) { value, _ -> scale = value }
        animate(1.2f, 1f, animationSpec = tween(200)) { value, _ -> scale = value }
        
        // 等待2秒
        kotlinx.coroutines.delay(2000)
        
        // 淡出动画
        animate(1f, 0f, animationSpec = tween(500)) { value, _ -> alpha = value }
        visible = false
        onDismiss()
    }
    
    if (visible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f * alpha)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(32.dp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        alpha = alpha
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("🎉", fontSize = 64.sp)
                    
                    Text(
                        "购买成功！",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            itemName,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
