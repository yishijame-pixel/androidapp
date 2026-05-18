// AvatarFrameDetailDialog.kt - 头像框商品详情对话框
package com.example.funlife.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.funlife.data.model.ShopItem

// 颜色配置
private object DialogPalette {
    val bg = Color(0xFFFEF3E8)
    val white = Color(0xFFFFFFFF)
    val coral = Color(0xFFFF5222)
    val amber = Color(0xFFF5A623)
    val ink = Color(0xFF1E0D06)
    val brown = Color(0xFF7A3D24)
    val muted = Color(0xFFB07D66)
    val border = Color(0x1FC86E46)
    val vipGold = Color(0xFFFFD700)
    val vipBrown = Color(0xFFC17A1A)
    val disabled = Color(0xFFB0B0B0)
    val strikethrough = Color(0xFF999999)
}

data class RarityStyle(
    val label: String,
    val color: Color,
    val bg: Color,
    val border: Color
)

private val rarityStyles = mapOf(
    "普通" to RarityStyle("普通", Color(0xFF8B7355), Color(0xFFF5F0E8), Color(0x408B7355)),
    "稀有" to RarityStyle("稀有", Color(0xFF1A7FC1), Color(0xFFE8F4FF), Color(0x4D1A7FC1)),
    "史诗" to RarityStyle("史诗", Color(0xFF8B3FC1), Color(0xFFF5E8FF), Color(0x4D8B3FC1)),
    "传说" to RarityStyle("传说", Color(0xFFC17A1A), Color(0xFFFFF4E0), Color(0x59C17A1A))
)

@Composable
fun AvatarFrameDetailDialog(
    frame: ShopItem,
    isOwned: Boolean,
    isEquipped: Boolean,
    isVip: Boolean,
    userCoins: Int,
    rarityDisplay: String,
    onDismiss: () -> Unit,
    onPurchase: () -> Unit,
    onEquip: () -> Unit,
    onUnequip: () -> Unit
) {
    val isVipOnly = frame.vipPrice < frame.price
    val actualPrice = if (isVip) frame.vipPrice else frame.price
    val canAfford = userCoins >= actualPrice
    val rarityStyle = rarityStyles[rarityDisplay] ?: rarityStyles["普通"]!!
    
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
                initialScale = 0.9f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
            ),
            exit = fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.95f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onDismiss() })
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .pointerInput(Unit) {
                            detectTapGestures { /* 阻止点击穿透 */ }
                        },
                    shape = RoundedCornerShape(24.dp),
                    color = DialogPalette.white,
                    shadowElevation = 24.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 关闭按钮
                        Box(modifier = Modifier.fillMaxWidth()) {
                            var closePressed by remember { mutableStateOf(false) }
                            val closeScale by animateFloatAsState(
                                if (closePressed) 0.85f else 1f,
                                label = "closeScale"
                            )
                            
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .scale(closeScale)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                closePressed = true
                                                tryAwaitRelease()
                                                closePressed = false
                                            }
                                        )
                                    }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "关闭",
                                    tint = DialogPalette.muted,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        // 大图预览
                        Box(
                            modifier = Modifier.size(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // 外圈装饰
                            Canvas(Modifier.fillMaxSize()) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            rarityStyle.color.copy(alpha = 0.15f),
                                            Color.Transparent
                                        ),
                                        radius = size.width * 0.7f
                                    )
                                )
                                
                                drawCircle(
                                    color = rarityStyle.color.copy(alpha = 0.1f),
                                    radius = size.width / 2f + 4f
                                )
                                
                                drawCircle(
                                    color = rarityStyle.color.copy(alpha = 0.2f),
                                    radius = size.width / 2f
                                )
                            }
                            
                            // 头像框图片
                            Box(
                                modifier = Modifier.size(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!frame.assetPath.isNullOrEmpty()) {
                                    val context = LocalContext.current
                                    SubcomposeAsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data("file:///android_asset/${frame.assetPath}")
                                            .crossfade(300)
                                            .build(),
                                        contentDescription = frame.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit,
                                        loading = {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(32.dp),
                                                color = rarityStyle.color,
                                                strokeWidth = 3.dp
                                            )
                                        },
                                        error = {
                                            Box(
                                                Modifier
                                                    .fillMaxSize()
                                                    .background(rarityStyle.bg, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(frame.icon, fontSize = 48.sp)
                                            }
                                        }
                                    )
                                } else {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(rarityStyle.bg, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(frame.icon, fontSize = 48.sp)
                                    }
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(20.dp))
                        
                        // 商品名称
                        Text(
                            frame.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DialogPalette.ink,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        
                        // 稀有度标签
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = rarityStyle.bg,
                            border = BorderStroke(1.5.dp, rarityStyle.border)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (rarityDisplay == "传说") {
                                    Icon(
                                        Icons.Default.Star,
                                        null,
                                        tint = rarityStyle.color,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    rarityStyle.label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = rarityStyle.color
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        // 商品描述
                        Text(
                            frame.description,
                            fontSize = 13.sp,
                            color = DialogPalette.muted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            lineHeight = 18.sp
                        )
                        
                        Spacer(Modifier.height(24.dp))
                        
                        // 价格对比（仅未拥有时显示）
                        if (!isOwned) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // 普通价格
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        "普通价格",
                                        fontSize = 11.sp,
                                        color = DialogPalette.muted,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Star,
                                            null,
                                            tint = if (isVip && isVipOnly) DialogPalette.strikethrough else DialogPalette.amber,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            frame.price.toString(),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isVip && isVipOnly) DialogPalette.strikethrough else DialogPalette.brown,
                                            style = if (isVip && isVipOnly) {
                                                androidx.compose.ui.text.TextStyle(textDecoration = TextDecoration.LineThrough)
                                            } else {
                                                androidx.compose.ui.text.TextStyle()
                                            }
                                        )
                                    }
                                }
                                
                                // VIP价格
                                if (isVipOnly) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "VIP价格",
                                                fontSize = 11.sp,
                                                color = if (isVip) DialogPalette.vipBrown else DialogPalette.disabled,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (!isVip) {
                                                Icon(
                                                    Icons.Default.Lock,
                                                    null,
                                                    tint = DialogPalette.disabled,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Star,
                                                null,
                                                tint = if (isVip) DialogPalette.vipGold else DialogPalette.disabled,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                frame.vipPrice.toString(),
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isVip) DialogPalette.vipBrown else DialogPalette.disabled
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            
                            // 当前余额
                            Row(
                                modifier = Modifier.padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "当前余额：",
                                    fontSize = 12.sp,
                                    color = DialogPalette.muted
                                )
                                Icon(
                                    Icons.Default.Star,
                                    null,
                                    tint = DialogPalette.amber,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    userCoins.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (canAfford) DialogPalette.brown else Color(0xFFE53935)
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        
                        // 操作按钮
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 20.dp)
                        ) {
                            when {
                                // 已装备
                                isEquipped -> {
                                    var unequipPressed by remember { mutableStateOf(false) }
                                    val unequipScale by animateFloatAsState(
                                        if (unequipPressed) 0.96f else 1f,
                                        label = "unequipScale"
                                    )
                                    
                                    Button(
                                        onClick = {
                                            onUnequip()
                                            onDismiss()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .scale(unequipScale)
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onPress = {
                                                        unequipPressed = true
                                                        tryAwaitRelease()
                                                        unequipPressed = false
                                                    }
                                                )
                                            },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = DialogPalette.muted
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                                            Text(
                                                "已装备 · 点击卸下",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                
                                // 已拥有但未装备
                                isOwned -> {
                                    var equipPressed by remember { mutableStateOf(false) }
                                    val equipScale by animateFloatAsState(
                                        if (equipPressed) 0.96f else 1f,
                                        label = "equipScale"
                                    )
                                    
                                    Button(
                                        onClick = {
                                            onEquip()
                                            onDismiss()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .scale(equipScale)
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onPress = {
                                                        equipPressed = true
                                                        tryAwaitRelease()
                                                        equipPressed = false
                                                    }
                                                )
                                            },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = DialogPalette.coral
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Star, null, modifier = Modifier.size(20.dp))
                                            Text(
                                                "装备头像框",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                
                                // 未拥有
                                else -> {
                                    var purchasePressed by remember { mutableStateOf(false) }
                                    val purchaseScale by animateFloatAsState(
                                        if (purchasePressed) 0.96f else 1f,
                                        label = "purchaseScale"
                                    )
                                    
                                    Button(
                                        onClick = {
                                            if (canAfford) {
                                                onPurchase()
                                            }
                                        },
                                        enabled = canAfford,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .scale(purchaseScale)
                                            .pointerInput(canAfford) {
                                                if (canAfford) {
                                                    detectTapGestures(
                                                        onPress = {
                                                            purchasePressed = true
                                                            tryAwaitRelease()
                                                            purchasePressed = false
                                                        }
                                                    )
                                                }
                                            },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isVip && isVipOnly) {
                                                Color(0xFFFFD700)
                                            } else {
                                                DialogPalette.coral
                                            },
                                            disabledContainerColor = DialogPalette.disabled
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.ShoppingCart,
                                                null,
                                                modifier = Modifier.size(20.dp),
                                                tint = if (canAfford) Color.White else Color(0xFFE0E0E0)
                                            )
                                            Text(
                                                if (canAfford) "购买 · $actualPrice" else "金币不足",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (canAfford) Color.White else Color(0xFFE0E0E0)
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
    }
}
