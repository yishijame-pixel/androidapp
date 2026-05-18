// PurchaseConfirmDialog.kt - 购买确认对话框
package com.example.funlife.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.funlife.data.model.ShopItem

// 颜色配置
private object ConfirmPalette {
    val white = Color(0xFFFFFFFF)
    val coral = Color(0xFFFF5222)
    val amber = Color(0xFFF5A623)
    val ink = Color(0xFF1E0D06)
    val brown = Color(0xFF7A3D24)
    val muted = Color(0xFFB07D66)
    val vipGold = Color(0xFFFFD700)
    val vipBrown = Color(0xFFC17A1A)
    val success = Color(0xFF4CAF50)
    val warning = Color(0xFFFF9800)
}

@Composable
fun PurchaseConfirmDialog(
    frame: ShopItem,
    price: Int,
    currentCoins: Int,
    isVip: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val afterCoins = currentCoins - price
    val isVipPrice = isVip && frame.vipPrice < frame.price
    
    // 入场动画
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(200)) + scaleIn(
                initialScale = 0.85f,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 350f)
            ),
            exit = fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.9f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onDismiss() })
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .pointerInput(Unit) {
                            detectTapGestures { /* 阻止点击穿透 */ }
                        },
                    shape = RoundedCornerShape(24.dp),
                    color = ConfirmPalette.white,
                    shadowElevation = 32.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 图标
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = if (isVipPrice) {
                                            listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                                        } else {
                                            listOf(ConfirmPalette.coral, Color(0xFFFF8C3A))
                                        }
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        // 标题
                        Text(
                            "确认购买",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ConfirmPalette.ink
                        )
                        
                        // 分隔线
                        Divider(
                            modifier = Modifier.fillMaxWidth(0.8f),
                            color = ConfirmPalette.muted.copy(alpha = 0.2f),
                            thickness = 1.dp
                        )
                        
                        // 商品信息
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 商品名称
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "商品",
                                    fontSize = 14.sp,
                                    color = ConfirmPalette.muted,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    frame.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ConfirmPalette.ink
                                )
                            }
                            
                            // 价格
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "价格",
                                    fontSize = 14.sp,
                                    color = ConfirmPalette.muted,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        null,
                                        tint = if (isVipPrice) ConfirmPalette.vipGold else ConfirmPalette.amber,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        price.toString(),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isVipPrice) ConfirmPalette.vipBrown else ConfirmPalette.brown
                                    )
                                    if (isVipPrice) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFFF4E0)
                                        ) {
                                            Text(
                                                "VIP折扣",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ConfirmPalette.vipBrown,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // 余额变化
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "余额",
                                    fontSize = 14.sp,
                                    color = ConfirmPalette.muted,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 当前余额
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Star,
                                            null,
                                            tint = ConfirmPalette.amber,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            currentCoins.toString(),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ConfirmPalette.brown
                                        )
                                    }
                                    
                                    Icon(
                                        Icons.Default.ArrowForward,
                                        null,
                                        tint = ConfirmPalette.muted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    
                                    // 购买后余额
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Star,
                                            null,
                                            tint = if (afterCoins >= 0) ConfirmPalette.success else Color(0xFFE53935),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            afterCoins.toString(),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (afterCoins >= 0) ConfirmPalette.success else Color(0xFFE53935)
                                        )
                                    }
                                }
                            }
                        }
                        
                        // 提示信息
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF5F5F5)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    null,
                                    tint = ConfirmPalette.muted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "购买后将放入背包，可在背包中装备",
                                    fontSize = 12.sp,
                                    color = ConfirmPalette.muted,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(4.dp))
                        
                        // 按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 取消按钮
                            var cancelPressed by remember { mutableStateOf(false) }
                            val cancelScale by animateFloatAsState(
                                if (cancelPressed) 0.94f else 1f,
                                label = "cancelScale"
                            )
                            
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .scale(cancelScale)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                cancelPressed = true
                                                tryAwaitRelease()
                                                cancelPressed = false
                                            }
                                        )
                                    },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.5.dp, ConfirmPalette.muted.copy(alpha = 0.3f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = ConfirmPalette.muted
                                )
                            ) {
                                Text(
                                    "取消",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            // 确认按钮
                            var confirmPressed by remember { mutableStateOf(false) }
                            val confirmScale by animateFloatAsState(
                                if (confirmPressed) 0.94f else 1f,
                                label = "confirmScale"
                            )
                            
                            Button(
                                onClick = onConfirm,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .scale(confirmScale)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                confirmPressed = true
                                                tryAwaitRelease()
                                                confirmPressed = false
                                            }
                                        )
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isVipPrice) {
                                        Color(0xFFFFD700)
                                    } else {
                                        ConfirmPalette.coral
                                    }
                                )
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        "确认购买",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
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
