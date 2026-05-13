// FrameSelector.kt - 相框选择器
package com.example.funlife.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.data.model.InventoryItem

// 相框数据类
data class FrameOption(
    val id: String,
    val name: String,
    val isDefault: Boolean = false,
    val isOwned: Boolean = false
)

@Composable
fun FrameSelector(
    selectedFrameId: String,
    ownedFrames: List<InventoryItem>,
    onFrameSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 相框名称映射
    val frameNames = mapOf(
        "jinian_card_1" to "经典相框",
        "jinian_card_2" to "温馨相框",
        "jinian_card_3" to "花卉相框",
        "jinian_card_4" to "星空相框",
        "jinian_card_5" to "爱心相框",
        "jinian_card_6" to "彩虹相框",
        "jinian_card_7" to "金色相框",
        "jinian_card_8" to "蝴蝶相框",
        "jinian_card_9" to "樱花相框",
        "jinian_card_10" to "皇冠相框"
    )
    
    // 默认相框列表
    val defaultFrames = listOf(
        FrameOption("jinian_card_1", frameNames["jinian_card_1"] ?: "默认相框1", isDefault = true, isOwned = true),
        FrameOption("jinian_card_2", frameNames["jinian_card_2"] ?: "默认相框2", isDefault = true, isOwned = true)
    )
    
    // 用户拥有的相框
    val userFrames = ownedFrames.map { item ->
        FrameOption(
            id = item.itemId,
            name = item.itemName,
            isDefault = false,
            isOwned = true
        )
    }
    
    // 所有可用相框（示例：显示所有相框，未拥有的显示锁定）
    val allFrameIds = listOf(
        "jinian_card_1", "jinian_card_2", "jinian_card_3", "jinian_card_4",
        "jinian_card_5", "jinian_card_6", "jinian_card_7", "jinian_card_8",
        "jinian_card_9", "jinian_card_10"
    )
    
    val ownedFrameIds = (defaultFrames + userFrames).map { it.id }.toSet()
    
    val allFrames = allFrameIds.map { id ->
        FrameOption(
            id = id,
            name = frameNames[id] ?: "相框 ${id.substringAfterLast("_")}",
            isDefault = id in listOf("jinian_card_1", "jinian_card_2"),
            isOwned = id in ownedFrameIds
        )
    }
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "选择相框",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "已拥有 ${ownedFrameIds.size}/${allFrameIds.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(300.dp)
        ) {
            items(allFrames) { frame ->
                FrameOptionItem(
                    frame = frame,
                    isSelected = frame.id == selectedFrameId,
                    onClick = {
                        if (frame.isOwned) {
                            onFrameSelected(frame.id)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FrameOptionItem(
    frame: FrameOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val frameBitmap = remember(frame.id) {
        com.example.funlife.utils.ImageCache.loadImage(context, "login/${frame.id}.png")
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) Color(0xFF4CAF50) else Color.Gray,
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(enabled = frame.isOwned) { onClick() }
        ) {
            // 相框预览
            frameBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = frame.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            
            // 选中标记
            if (isSelected) {
                Surface(
                    color = Color(0xFF4CAF50),
                    shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        Icons.Default.Check,
                        "已选择",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(20.dp)
                    )
                }
            }
            
            // 未拥有的显示锁定
            if (!frame.isOwned) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                "未拥有",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "未拥有",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // 默认标记
            if (frame.isDefault) {
                Surface(
                    color = Color(0xFFFFD700),
                    shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        "默认",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        
        // 相框名称
        Text(
            text = frame.name,
            fontSize = 11.sp,
            color = if (frame.isOwned) MaterialTheme.colorScheme.onSurface else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(top = 4.dp),
            maxLines = 1
        )
    }
}
