// InventoryScreen.kt - 背包界面
package com.example.funlife.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.funlife.data.model.InventoryItem
import com.example.funlife.data.model.ItemRarity
import com.example.funlife.data.model.InventoryItemType
import com.example.funlife.viewmodel.InventoryViewModel
import kotlinx.coroutines.launch

@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val items by viewModel.items.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val itemCount by viewModel.itemCount.collectAsState()
    val totalQuantity by viewModel.totalQuantity.collectAsState()
    val itemsByType by viewModel.itemsByType.collectAsState()
    val equippedPanelSkin by viewModel.equippedPanelSkin.collectAsState()
    val inventoryCapacity by viewModel.inventoryCapacity.collectAsState() // 🔥 背包容量
    val userVip by viewModel.userVip.collectAsState() // 🔥 VIP状态
    
    var showItemDetail by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<InventoryItem?>(null) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 🔥 计算是否是VIP
    val isVip = userVip?.isVip() == true && userVip?.isExpired() == false
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF8E1),
                        Color(0xFFFFE0B2),
                        Color(0xFFFFCC80)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 顶部导航栏
            InventoryTopBar(
                itemCount = itemCount,
                totalQuantity = totalQuantity,
                inventoryCapacity = inventoryCapacity, // 🔥 传递容量
                isVip = isVip, // 🔥 传递VIP状态
                onNavigateBack = onNavigateBack
            )
            
            // 类型过滤标签
            ItemTypeFilter(
                selectedType = selectedType,
                itemsByType = itemsByType,
                onTypeSelected = { viewModel.setTypeFilter(it) }
            )
            
            // 物品网格
            if (items.isEmpty()) {
                EmptyInventoryState()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items, key = { it.id }) { item ->
                        // 判断是否是当前装备的面板或按钮皮肤或头像框
                        val equippedButtonSkin by viewModel.equippedButtonSkin.collectAsState()
                        val equippedAvatarFrame by viewModel.equippedAvatarFrame.collectAsState()
                        
                        // 🔥 精确匹配头像框路径，避免误判
                        val isEquipped = when {
                            // 面板皮肤匹配
                            item.itemId == "panel_$equippedPanelSkin" -> true
                            // 按钮皮肤匹配
                            item.itemId == "button_$equippedButtonSkin" -> true
                            // 头像框匹配：从description中提取实际路径进行精确匹配
                            item.itemId.startsWith("avatar_frame_") && equippedAvatarFrame != null -> {
                                val actualAssetPath = if (item.description.contains("/")) {
                                    item.description.substringBefore("\n").trim()
                                } else {
                                    val frameNum = item.itemId.removePrefix("avatar_frame_")
                                    "xiangkuang/$frameNum.png"
                                }
                                actualAssetPath == equippedAvatarFrame
                            }
                            else -> false
                        }
                        
                        InventoryItemCard(
                            item = item,
                            onClick = {
                                selectedItem = item
                                showItemDetail = true
                            },
                            isEquipped = isEquipped
                        )
                    }
                }
            }
        }
    }
    
    // 物品详情对话框
    if (showItemDetail && selectedItem != null) {
        ItemDetailDialog(
            item = selectedItem!!,
            onDismiss = { showItemDetail = false },
            onUse = { item ->
                viewModel.useItem(item)
                showItemDetail = false
            },
            onDelete = { item ->
                viewModel.deleteItem(item)
                showItemDetail = false
            },
            onEquipPanel = { skinName ->
                viewModel.equipPanelSkin(skinName)
                scope.launch {
                    android.widget.Toast.makeText(context, "已设置为当前结算面板", android.widget.Toast.LENGTH_SHORT).show()
                }
                showItemDetail = false
            },
            onEquipButton = { skinName ->
                viewModel.equipButtonSkin(skinName)
                scope.launch {
                    android.widget.Toast.makeText(context, "已设置为当前转盘按钮", android.widget.Toast.LENGTH_SHORT).show()
                }
                showItemDetail = false
            },
            onEquipAvatarFrame = { assetPath ->
                viewModel.equipAvatarFrame(assetPath)
                scope.launch {
                    android.widget.Toast.makeText(context, "已装备头像框", android.widget.Toast.LENGTH_SHORT).show()
                }
                showItemDetail = false
            }
        )
    }
}

@Composable
fun InventoryTopBar(
    itemCount: Int,
    totalQuantity: Int,
    inventoryCapacity: Int, // 🔥 新增容量参数
    isVip: Boolean, // 🔥 新增VIP状态
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 返回按钮 - 无背景色
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(40.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color(0xFF5D4037),
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // 标题
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🎒", fontSize = 24.sp)
                    Text(
                        "我的背包",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5D4037)
                    )
                }
                
                Text(
                    "$itemCount 种物品 · $totalQuantity 个",
                    fontSize = 12.sp,
                    color = Color(0xFF8D6E63)
                )
            }
        }
        
        // 🔥 容量显示条
        Spacer(modifier = Modifier.height(12.dp))
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "容量:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF5D4037)
                    )
                    // 🔥 终身VIP显示"无限"
                    if (inventoryCapacity == Int.MAX_VALUE) {
                        Text(
                            "$totalQuantity / ∞",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    } else {
                        Text(
                            "$totalQuantity / $inventoryCapacity",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalQuantity >= inventoryCapacity * 0.9) {
                                Color(0xFFE53935) // 接近满时显示红色
                            } else {
                                Color(0xFF4CAF50)
                            }
                        )
                    }
                }
                
                // VIP标识
                if (isVip) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFD700).copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("👑", fontSize = 14.sp)
                            Text(
                                "VIP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Gray.copy(alpha = 0.2f)
                    ) {
                        Text(
                            "普通",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 容量进度条 - 终身VIP不显示进度条
            if (inventoryCapacity != Int.MAX_VALUE) {
                val progress = totalQuantity.toFloat() / inventoryCapacity.toFloat()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE0E0E0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = if (progress >= 0.9f) {
                                        listOf(Color(0xFFE53935), Color(0xFFFF5252))
                                    } else {
                                        listOf(Color(0xFF4CAF50), Color(0xFF66BB6A))
                                    }
                                ),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
                
                // 容量提示
                if (progress >= 0.9f) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        if (isVip) "背包快满了！" else "背包快满了！开通VIP可扩容",
                        fontSize = 11.sp,
                        color = Color(0xFFE53935),
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // 终身VIP显示特殊提示
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "✨ 终身VIP · 无限容量",
                    fontSize = 11.sp,
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ItemTypeFilter(
    selectedType: InventoryItemType?,
    itemsByType: Map<InventoryItemType, Int>,
    onTypeSelected: (InventoryItemType?) -> Unit
) {
    data class TypeInfo(val type: InventoryItemType?, val label: String, val emoji: String)
    
    val types = listOf(
        TypeInfo(null, "全部", "📦"),
        TypeInfo(InventoryItemType.FOOD, "食物", "🍖"),
        TypeInfo(InventoryItemType.TOY, "玩具", "🎾"),
        TypeInfo(InventoryItemType.DECORATION, "装饰", "🎨"),
        TypeInfo(InventoryItemType.BUTTON_SKIN, "按钮", "🎯"),
        TypeInfo(InventoryItemType.PANEL_SKIN, "面板", "🎴"),
        TypeInfo(InventoryItemType.AVATAR_FRAME, "头像框", "🖼️"),
        TypeInfo(InventoryItemType.CONSUMABLE, "消耗", "💊"),
        TypeInfo(InventoryItemType.SPECIAL, "特殊", "✨")
    )
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.height(130.dp)
    ) {
        items(types.size) { index ->
            val typeInfo = types[index]
            val count = if (typeInfo.type == null) {
                itemsByType.values.sum()
            } else {
                itemsByType[typeInfo.type] ?: 0
            }
            val isSelected = selectedType == typeInfo.type
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable { onTypeSelected(typeInfo.type) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Color(0xFFFF9800) else Color.White.copy(alpha = 0.9f),
                tonalElevation = if (isSelected) 4.dp else 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        typeInfo.emoji, 
                        fontSize = 22.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        typeInfo.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Color(0xFF5D4037),
                        maxLines = 1
                    )
                    Text(
                        "($count)",
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White.copy(alpha = 0.9f) else Color(0xFF8D6E63),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun InventoryItemCard(
    item: InventoryItem,
    onClick: () -> Unit,
    isEquipped: Boolean = false
) {
    val rarityColor = when (item.itemRarity) {
        ItemRarity.COMMON -> Color(0xFF9E9E9E)
        ItemRarity.RARE -> Color(0xFF2196F3)
        ItemRarity.EPIC -> Color(0xFF9C27B0)
        ItemRarity.LEGENDARY -> Color(0xFFFF9800)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 稀有度边框
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                rarityColor.copy(alpha = 0.3f),
                                rarityColor.copy(alpha = 0.1f)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 物品图标 - 如果是面板皮肤或按钮皮肤，显示真实图片
                if (item.itemId.startsWith("panel_js_")) {
                    val context = LocalContext.current
                    val panelName = item.itemId.removePrefix("panel_")
                    val panelBitmap = remember(item.itemId) {
                        try {
                            context.assets.open("login/$panelName.png").use { inputStream ->
                                android.graphics.BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    if (panelBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = panelBitmap,
                            contentDescription = item.itemName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(
                            item.iconEmoji,
                            fontSize = 36.sp
                        )
                    }
                } else if (item.itemId.startsWith("button_pf_")) {
                    val context = LocalContext.current
                    val buttonName = item.itemId.removePrefix("button_")
                    val buttonBitmap = remember(item.itemId) {
                        try {
                            context.assets.open("login/$buttonName.png").use { inputStream ->
                                android.graphics.BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    if (buttonBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = buttonBitmap,
                            contentDescription = item.itemName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    } else {
                        Text(
                            item.iconEmoji,
                            fontSize = 36.sp
                        )
                    }
                } else if (item.itemId.startsWith("avatar_frame_")) {
                    // 🔥 显示头像框图片
                    val context = LocalContext.current
                    // 从itemId中提取资源路径（存储在description中）
                    val frameBitmap = remember(item.itemId) {
                        try {
                            // 尝试从description中获取路径，或使用默认路径
                            val assetPath = if (item.description.contains("/")) {
                                item.description.substringBefore("\n").trim()
                            } else {
                                // 从itemId提取数字，构建路径
                                val frameNum = item.itemId.removePrefix("avatar_frame_")
                                "xiangkuang/$frameNum.png"
                            }
                            context.assets.open(assetPath).use { inputStream ->
                                android.graphics.BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("InventoryItemCard", "Failed to load avatar frame: ${e.message}")
                            null
                        }
                    }
                    
                    if (frameBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = frameBitmap,
                            contentDescription = item.itemName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    } else {
                        Text(
                            item.iconEmoji,
                            fontSize = 36.sp
                        )
                    }
                } else {
                    Text(
                        item.iconEmoji,
                        fontSize = 36.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 物品名称
                Text(
                    item.itemName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF5D4037),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                
                // 数量
                if (item.quantity > 1) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = rarityColor.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            "x${item.quantity}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            // 已装备标识
            if (isEquipped) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF4CAF50),
                    shadowElevation = 2.dp
                ) {
                    Text(
                        "已装备",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyInventoryState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("🎒", fontSize = 80.sp)
            Text(
                "背包空空如也",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5D4037)
            )
            Text(
                "去商城购买一些物品吧！",
                fontSize = 14.sp,
                color = Color(0xFF8D6E63)
            )
        }
    }
}

@Composable
fun ItemDetailDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onUse: (InventoryItem) -> Unit,
    onDelete: (InventoryItem) -> Unit,
    onEquipPanel: (String) -> Unit = {},
    onEquipButton: (String) -> Unit = {},
    onEquipAvatarFrame: (String) -> Unit = {}  // 🔥 新增：装备头像框回调
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val rarityColor = when (item.itemRarity) {
        ItemRarity.COMMON -> Color(0xFF9E9E9E)
        ItemRarity.RARE -> Color(0xFF2196F3)
        ItemRarity.EPIC -> Color(0xFF9C27B0)
        ItemRarity.LEGENDARY -> Color(0xFFFF9800)
    }
    
    val rarityText = when (item.itemRarity) {
        ItemRarity.COMMON -> "普通"
        ItemRarity.RARE -> "稀有"
        ItemRarity.EPIC -> "史诗"
        ItemRarity.LEGENDARY -> "传说"
    }
    
    // 判断是否是结算面板皮肤或按钮皮肤
    val isPanelSkin = item.itemId.startsWith("panel_")
    val isButtonSkin = item.itemId.startsWith("button_")
    val isAvatarFrame = item.itemId.startsWith("avatar_frame_")  // 🔥 新增：判断是否是头像框
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFFBF5)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 物品图标 - 如果是面板皮肤或按钮皮肤，显示真实图片
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    rarityColor.copy(alpha = 0.3f),
                                    rarityColor.copy(alpha = 0.1f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        item.itemId.startsWith("panel_js_") -> {
                            val panelName = item.itemId.removePrefix("panel_")
                            val panelBitmap = remember(item.itemId) {
                                try {
                                    context.assets.open("login/$panelName.png").use { inputStream ->
                                        android.graphics.BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            
                            if (panelBitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = panelBitmap,
                                    contentDescription = item.itemName,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                )
                            } else {
                                Text(item.iconEmoji, fontSize = 60.sp)
                            }
                        }
                        item.itemId.startsWith("button_pf_") -> {
                            val buttonName = item.itemId.removePrefix("button_")
                            val buttonBitmap = remember(item.itemId) {
                                try {
                                    context.assets.open("login/$buttonName.png").use { inputStream ->
                                        android.graphics.BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            
                            if (buttonBitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = buttonBitmap,
                                    contentDescription = item.itemName,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                )
                            } else {
                                Text(item.iconEmoji, fontSize = 60.sp)
                            }
                        }
                        item.itemId.startsWith("avatar_frame_") -> {
                            // 🔥 显示头像框图片
                            val frameBitmap = remember(item.itemId) {
                                try {
                                    // 尝试从description中获取路径，或使用默认路径
                                    val assetPath = if (item.description.contains("/")) {
                                        item.description.substringBefore("\n").trim()
                                    } else {
                                        // 从itemId提取数字，构建路径
                                        val frameNum = item.itemId.removePrefix("avatar_frame_")
                                        "xiangkuang/$frameNum.png"
                                    }
                                    context.assets.open(assetPath).use { inputStream ->
                                        android.graphics.BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("ItemDetailDialog", "Failed to load avatar frame: ${e.message}")
                                    null
                                }
                            }
                            
                            if (frameBitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = frameBitmap,
                                    contentDescription = item.itemName,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                )
                            } else {
                                Text(item.iconEmoji, fontSize = 60.sp)
                            }
                        }
                        else -> {
                            Text(item.iconEmoji, fontSize = 60.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 物品名称
                Text(
                    item.itemName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5D4037)
                )
                
                // 稀有度标签
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = rarityColor.copy(alpha = 0.2f),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        rarityText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = rarityColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 物品描述
                Text(
                    item.description,
                    fontSize = 14.sp,
                    color = Color(0xFF5D4037).copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 物品信息
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    InfoItem("数量", "${item.quantity}")
                    if (item.effectValue > 0) {
                        InfoItem("效果", "+${item.effectValue}")
                    }
                    if (item.purchasePrice > 0) {
                        InfoItem("价值", "${item.purchasePrice}💰")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (item.isUsable) {
                        Button(
                            onClick = {
                                when {
                                    isPanelSkin -> {
                                        // 处理面板皮肤装备
                                        val skinName = item.itemId.removePrefix("panel_")
                                        onEquipPanel(skinName)
                                    }
                                    isButtonSkin -> {
                                        // 🔥 处理按钮皮肤装备
                                        val skinName = item.itemId.removePrefix("button_")
                                        onEquipButton(skinName)
                                    }
                                    isAvatarFrame -> {
                                        // 🔥 处理头像框装备
                                        val assetPath = if (item.description.contains("/")) {
                                            item.description.substringBefore("\n").trim()
                                        } else {
                                            val frameNum = item.itemId.removePrefix("avatar_frame_")
                                            "xiangkuang/$frameNum.png"
                                        }
                                        onEquipAvatarFrame(assetPath)
                                    }
                                    else -> {
                                        onUse(item)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isPanelSkin || isButtonSkin || isAvatarFrame) "装备" else "使用")
                        }
                    }
                    
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFE53935)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("丢弃")
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("关闭")
                }
            }
        }
    }
    
    // 删除确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Text("🗑️", fontSize = 48.sp) },
            title = { Text("确认丢弃") },
            text = { Text("确定要丢弃 ${item.itemName} 吗？此操作无法撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(item)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    )
                ) {
                    Text("确定丢弃")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = Color(0xFF8D6E63)
        )
        Text(
            value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5D4037)
        )
    }
}
