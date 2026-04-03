// AnniversaryDetailDialog.kt - 按照原型图设计的纪念日详情对话框
package com.example.funlife.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.funlife.data.model.Anniversary

@Composable
fun AnniversaryDetailDialog(
    anniversary: Anniversary,
    onDismiss: () -> Unit
) {
    val daysRemaining = anniversary.getDaysRemaining()
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 背景：模糊的照片或默认渐变
            if (!anniversary.imageUri.isNullOrEmpty()) {
                AsyncImage(
                    model = Uri.parse(anniversary.imageUri),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(50.dp),
                    contentScale = ContentScale.Crop
                )
                // 半透明遮罩
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.3f))
                )
            } else {
                // 如果没有照片，使用默认渐变背景
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFE5F0),
                                    Color(0xFFE3F2FD)
                                )
                            )
                        )
                )
            }
            
            // 前景内容
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 顶部粉色栏（改进版）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp)
                        .background(Color(0xFFFFB3D9))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 返回按钮
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(2.dp, Color(0xFFFF69B4), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "返回",
                                    tint = Color(0xFFFF69B4),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        
                        // 标题（带描边效果）
                        Text(
                            text = "纪念日详情",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color(0xFFFF1493),
                                    offset = Offset(2f, 2f),
                                    blurRadius = 1f
                                )
                            )
                        )
                        
                        // 编辑按钮
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(2.dp, Color(0xFFFF69B4), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { },
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "编辑",
                                    tint = Color(0xFFFF69B4),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
                
                
                // 内容区域
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 32.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 中间白色卡片（细化边框）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(2.dp) // 为外层边框留空间
                    ) {
                        // 外层深粉色边框
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(30.dp))
                                .background(Color(0xFFFF69B4))
                                .padding(3.dp) // 深粉色边框宽度（细化）
                        ) {
                            // 内层浅粉色边框
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(27.dp))
                                    .background(Color(0xFFFFD1DC))
                                    .padding(2.dp) // 浅粉色边框宽度（细化）
                            ) {
                                // 白色内容区
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(25.dp))
                                        .background(Color.White.copy(alpha = 0.95f))
                                        .padding(20.dp)
                                ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 照片
                            if (!anniversary.imageUri.isNullOrEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                ) {
                                    AsyncImage(
                                        model = Uri.parse(anniversary.imageUri),
                                        contentDescription = "照片",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            
                            // 日期
                            Text(
                                text = anniversary.getFormattedDate(),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE91E63),
                                textAlign = TextAlign.Center
                            )
                            
                            // 标题（带爱心装饰）
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💕 ${anniversary.name} 💕",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF333333),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                                }
                            }
                        }
                    }
                    
                    
                    // 底部按钮（细化阴影）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 添加回忆按钮（粉色，带底部阴影）
                        Box(
                            modifier = Modifier.weight(1f)
                        ) {
                            // 底部阴影层（深粉色）
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .offset(y = 3.dp)
                                    .clip(RoundedCornerShape(25.dp))
                                    .background(Color(0xFFFF1493))
                            )
                            // 按钮本体
                            Button(
                                onClick = { },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(25.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFB3D9)
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 0.dp
                                )
                            ) {
                                Text(
                                    text = "添加回忆",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        
                        // 分享纪念按钮（蓝色，带底部阴影）
                        Box(
                            modifier = Modifier.weight(1f)
                        ) {
                            // 底部阴影层（深蓝色）
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .offset(y = 3.dp)
                                    .clip(RoundedCornerShape(25.dp))
                                    .background(Color(0xFF1E90FF))
                            )
                            // 按钮本体
                            Button(
                                onClick = { },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(25.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF81D4FA)
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 0.dp
                                )
                            ) {
                                Text(
                                    text = "分享纪念",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
