// DailyCheckIn.kt - 每日签到组件
package com.example.funlife.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

/**
 * 每日签到按钮
 */
@Composable
fun DailyCheckInButton(
    vipLevel: Int,
    hasCheckedInToday: Boolean,
    consecutiveDays: Int,
    onCheckIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rewardCoins = when (vipLevel) {
        1 -> 20
        2 -> 50
        3 -> 100
        else -> 10
    }
    
    var showAnimation by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    
    Button(
        onClick = {
            if (!hasCheckedInToday) {
                showAnimation = true
                onCheckIn()
            } else {
                showDialog = true
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(30.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp),
        enabled = !hasCheckedInToday
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (hasCheckedInToday) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFE0E0E0),
                                Color(0xFFF5F5F5)
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFFD700),
                                Color(0xFFFFA500)
                            )
                        )
                    },
                    RoundedCornerShape(30.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (hasCheckedInToday) Icons.Default.CheckCircle else Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = if (hasCheckedInToday) Color(0xFF95A5A6) else Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (hasCheckedInToday) "今日已签到" else "每日签到",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasCheckedInToday) Color(0xFF95A5A6) else Color.White
                    )
                    if (!hasCheckedInToday) {
                        Text(
                            "+$rewardCoins💰",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
    
    // 签到成功动画
    if (showAnimation) {
        CheckInSuccessAnimation(
            coins = rewardCoins,
            onDismiss = { showAnimation = false }
        )
    }
    
    // 签到信息对话框
    if (showDialog) {
        CheckInInfoDialog(
            consecutiveDays = consecutiveDays,
            vipLevel = vipLevel,
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * 签到成功动画
 */
@Composable
fun CheckInSuccessAnimation(
    coins: Int,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
        delay(2000)
        visible = false
        delay(300)
        onDismiss()
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(32.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 成功图标
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF4CAF50),
                                        Color(0xFF45A049)
                                    )
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    
                    Text(
                        "签到成功！",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    
                    Text(
                        "+$coins💰",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                }
            }
        }
    }
}

/**
 * 签到信息对话框
 */
@Composable
fun CheckInInfoDialog(
    consecutiveDays: Int,
    vipLevel: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "签到信息",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                
                Divider(color = Color(0xFFF0F0F0))
                
                // 连续签到天数
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "连续签到",
                        fontSize = 16.sp,
                        color = Color(0xFF7F8C8D)
                    )
                    Text(
                        "$consecutiveDays 天",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B35)
                    )
                }
                
                // VIP奖励
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFF6B35).copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "VIP每日奖励",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C3E50)
                        )
                        
                        when (vipLevel) {
                            0 -> CheckInRewardItem("普通用户", "10💰")
                            1 -> CheckInRewardItem("VIP1", "20💰")
                            2 -> CheckInRewardItem("VIP2", "50💰")
                            3 -> CheckInRewardItem("VIP3", "100💰")
                        }
                    }
                }
                
                // 签到日历（简化版）
                Text(
                    "本周签到",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEachIndexed { index, day ->
                        CheckInDayItem(
                            day = day,
                            isChecked = index < consecutiveDays % 7
                        )
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B35)
                    )
                ) {
                    Text("知道了")
                }
            }
        }
    }
}

@Composable
fun CheckInRewardItem(level: String, reward: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            level,
            fontSize = 13.sp,
            color = Color(0xFF7F8C8D)
        )
        Text(
            reward,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD700)
        )
    }
}

@Composable
fun CheckInDayItem(day: String, isChecked: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isChecked) Color(0xFF4CAF50) else Color(0xFFF5F5F5),
                    CircleShape
                )
                .border(
                    width = if (isChecked) 0.dp else 1.dp,
                    color = if (isChecked) Color.Transparent else Color(0xFFE0E0E0),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isChecked) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            day,
            fontSize = 12.sp,
            color = if (isChecked) Color(0xFF2C3E50) else Color(0xFF95A5A6)
        )
    }
}
