// FrameBackgroundSelector.kt - 头像框和背景选择器
package com.example.funlife.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.funlife.data.model.AvatarFrame
import com.example.funlife.data.model.ProfileBackground

/**
 * 头像框选择器对话框
 */
@Composable
fun FrameSelectorDialog(
    frames: List<AvatarFrame>,
    currentFrameId: String?,
    vipLevel: Int,
    userCoins: Int = 0,
    onDismiss: () -> Unit,
    onSelect: (AvatarFrame) -> Unit,
    onPurchase: (AvatarFrame) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("全部", "基础", "高级", "专属")
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "选择头像框",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "💰 $userCoins",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = Color(0xFF95A5A6)
                            )
                        }
                    }
                }
                
                Divider(color = Color(0xFFF0F0F0))
                
                // 分类标签
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.White,
                    contentColor = Color(0xFFFF6B35),
                    edgePadding = 16.dp
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontSize = 15.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
                
                Divider(color = Color(0xFFF0F0F0))
                
                // 头像框网格
                val filteredFrames = when (selectedTab) {
                    1 -> frames.filter { it.category == "basic" }
                    2 -> frames.filter { it.category == "premium" }
                    3 -> frames.filter { it.category == "exclusive" }
                    else -> frames
                }
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredFrames) { frame ->
                        FrameItem(
                            frame = frame,
                            isSelected = frame.id == currentFrameId,
                            isLocked = frame.requiredVipLevel > vipLevel,
                            canAfford = userCoins >= frame.price,
                            onClick = {
                                if (frame.requiredVipLevel <= vipLevel) {
                                    if (frame.price == 0) {
                                        onSelect(frame)
                                    } else {
                                        onPurchase(frame)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 头像框项目
 */
@Composable
fun FrameItem(
    frame: AvatarFrame,
    isSelected: Boolean,
    isLocked: Boolean,
    canAfford: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !isLocked, onClick = onClick)
            .background(
                if (isSelected) Color(0xFFFF6B35).copy(alpha = 0.1f)
                else Color(0xFFF5F5F5)
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) Color(0xFFFF6B35) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 图标
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLocked) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "已锁定",
                    tint = Color(0xFF95A5A6),
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Text(
                    frame.icon,
                    fontSize = 32.sp
                )
            }
            
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .background(Color(0xFF4CAF50), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "已选中",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        
        // 名称
        Text(
            frame.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isLocked) Color(0xFF95A5A6) else Color(0xFF2C3E50),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        
        // 价格或VIP要求
        if (isLocked) {
            Text(
                "VIP${frame.requiredVipLevel}",
                fontSize = 11.sp,
                color = Color(0xFFFF6B35),
                fontWeight = FontWeight.Bold
            )
        } else if (frame.price > 0) {
            Text(
                "${frame.price}💰",
                fontSize = 11.sp,
                color = if (canAfford) Color(0xFFFFD700) else Color(0xFFE74C3C),
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                "免费",
                fontSize = 11.sp,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 背景选择器对话框
 */
@Composable
fun BackgroundSelectorDialog(
    backgrounds: List<ProfileBackground>,
    currentBackgroundId: String?,
    vipLevel: Int,
    userCoins: Int = 0,
    onDismiss: () -> Unit,
    onSelect: (ProfileBackground) -> Unit,
    onPurchase: (ProfileBackground) -> Unit
) {
    var selectedBackground by remember { mutableStateOf<ProfileBackground?>(null) }
    var showPreview by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "选择背景",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "💰 $userCoins",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = Color(0xFF95A5A6)
                            )
                        }
                    }
                }
                
                Divider(color = Color(0xFFF0F0F0))
                
                // 背景网格
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(backgrounds) { background ->
                        BackgroundItem(
                            background = background,
                            isSelected = background.id == currentBackgroundId,
                            isLocked = background.requiredVipLevel > vipLevel,
                            canAfford = userCoins >= background.price,
                            onClick = {
                                if (background.requiredVipLevel <= vipLevel) {
                                    selectedBackground = background
                                    showPreview = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    
    // 预览对话框
    if (showPreview && selectedBackground != null) {
        BackgroundPreviewDialog(
            background = selectedBackground!!,
            canAfford = userCoins >= selectedBackground!!.price,
            onDismiss = { showPreview = false },
            onConfirm = {
                if (selectedBackground!!.price == 0) {
                    onSelect(selectedBackground!!)
                } else {
                    onPurchase(selectedBackground!!)
                }
                showPreview = false
            }
        )
    }
}

/**
 * 背景项目
 */
@Composable
fun BackgroundItem(
    background: ProfileBackground,
    isSelected: Boolean,
    isLocked: Boolean,
    canAfford: Boolean,
    onClick: () -> Unit
) {
    val colors = parseGradientColors(background.gradientColors)
    
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !isLocked, onClick = onClick)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) Color(0xFFFF6B35) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 背景预览
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(colors = colors),
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLocked) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "已锁定",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Text(
                    background.preview,
                    fontSize = 40.sp
                )
            }
            
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .background(Color(0xFF4CAF50), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "已选中",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        // 信息栏
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                background.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isLocked) Color(0xFF95A5A6) else Color(0xFF2C3E50),
                maxLines = 1
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLocked) {
                    Text(
                        "VIP${background.requiredVipLevel}",
                        fontSize = 12.sp,
                        color = Color(0xFFFF6B35),
                        fontWeight = FontWeight.Bold
                    )
                } else if (background.price > 0) {
                    Text(
                        "${background.price}💰",
                        fontSize = 12.sp,
                        color = if (canAfford) Color(0xFFFFD700) else Color(0xFFE74C3C),
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        "免费",
                        fontSize = 12.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Icon(
                    Icons.Default.Visibility,
                    contentDescription = "预览",
                    tint = Color(0xFF95A5A6),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * 背景预览对话框
 */
@Composable
fun BackgroundPreviewDialog(
    background: ProfileBackground,
    canAfford: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = parseGradientColors(background.gradientColors)
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // 背景预览
                VipProfileBackground(
                    vipLevel = background.requiredVipLevel,
                    background = background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 顶部信息
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                background.name,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            
                            Text(
                                background.description,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "粒子效果：",
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        background.particleType,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        
                        // 底部按钮
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (background.price > 0) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "价格",
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            "${background.price}💰",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (canAfford) Color(0xFFFFD700) else Color(0xFFE74C3C)
                                        )
                                    }
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        Color.White
                                    )
                                ) {
                                    Text("取消")
                                }
                                
                                Button(
                                    onClick = onConfirm,
                                    modifier = Modifier.weight(1f),
                                    enabled = background.price == 0 || canAfford,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFFFF6B35)
                                    )
                                ) {
                                    Text(
                                        if (background.price == 0) "使用" else "购买"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 解析渐变色字符串
 */
private fun parseGradientColors(colorString: String): List<Color> {
    return colorString.split(",").mapNotNull { colorHex ->
        try {
            Color(android.graphics.Color.parseColor(colorHex.trim()))
        } catch (e: Exception) {
            null
        }
    }.ifEmpty { listOf(Color(0xFFF5F5F5), Color.White) }
}
