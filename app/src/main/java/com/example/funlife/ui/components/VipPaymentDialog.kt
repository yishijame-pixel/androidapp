package com.example.funlife.ui.components

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.funlife.R
import com.example.funlife.config.VipConfig
import com.example.funlife.utils.ImageSaveHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * VIP支付对话框
 * 显示微信支付二维码，用户扫码支付后进入联系页面
 */
@Composable
fun VipPaymentDialog(
    vipTitle: String,
    price: String,
    onDismiss: () -> Unit,
    onPaymentComplete: () -> Unit
) {
    var showContactPage by remember { mutableStateOf(false) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable(enabled = false) { },
            contentAlignment = Alignment.Center
        ) {
            if (!showContactPage) {
                // 支付页面
                PaymentQRCodePage(
                    vipTitle = vipTitle,
                    price = price,
                    onClose = onDismiss,
                    onPaymentComplete = {
                        showContactPage = true
                    },
                    enableCountdown = VipConfig.ENABLE_PAYMENT_COUNTDOWN
                )
            } else {
                // 联系作者页面
                ContactAuthorPage(
                    onClose = {
                        onPaymentComplete()
                        onDismiss()
                    }
                )
            }
        }
    }
}

/**
 * 支付二维码页面
 */
@Composable
private fun PaymentQRCodePage(
    vipTitle: String,
    price: String,
    onClose: () -> Unit,
    onPaymentComplete: () -> Unit,
    enableCountdown: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 图片查看器状态
    var showImageViewer by remember { mutableStateOf(false) }
    
    // 倒计时状态
    var countdown by remember { mutableStateOf(if (enableCountdown) VipConfig.COUNTDOWN_SECONDS else 0) }
    var canProceed by remember { mutableStateOf(!enableCountdown) }
    
    // 倒计时逻辑
    LaunchedEffect(enableCountdown) {
        if (enableCountdown) {
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
            canProceed = true
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "payment")
    
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    // 从assets加载图片
    val qrBitmap = remember {
        try {
            context.assets.open("login/zhifu.png").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .clip(RoundedCornerShape(32.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F3460)
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 关闭按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 标题
        Text(
            text = "微信扫码支付",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            style = LocalTextStyle.current.copy(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color(0xFF00FF88).copy(alpha = 0.6f),
                    offset = Offset(0f, 0f),
                    blurRadius = 20f
                )
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // VIP信息
        Text(
            text = "$vipTitle - ¥$price",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD700)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 二维码容器（可点击放大）
        Box(
            modifier = Modifier
                .size(300.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(
                    width = 3.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00FF88),
                            Color(0xFF00DDFF),
                            Color(0xFF00FF88)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .clickable { showImageViewer = true }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "微信支付二维码",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                // 如果加载失败，显示占位图
                Image(
                    painter = painterResource(id = R.drawable.wechat_pay_qr),
                    contentDescription = "微信支付二维码",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
        
        // 图片查看器
        if (showImageViewer && qrBitmap != null) {
            com.example.funlife.ui.components.ImageViewerDialog(
                bitmap = qrBitmap,
                onDismiss = { showImageViewer = false }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 保存到相册按钮
        Button(
            onClick = {
                scope.launch {
                    try {
                        if (qrBitmap != null) {
                            val success = ImageSaveHelper.saveBitmapToGallery(
                                context = context,
                                bitmap = qrBitmap,
                                fileName = "wechat_pay_qr_${System.currentTimeMillis()}.png"
                            )
                            if (success) {
                                Toast.makeText(context, "二维码已保存到相册", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "保存失败，请检查存储权限", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("VipPaymentDialog", "保存二维码失败", e)
                        Toast.makeText(context, "保存失败，请重试", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.15f)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "下载",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "保存到相册",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 提示文字
        Text(
            text = "请使用微信扫描二维码完成支付",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 支付完成按钮（带倒计时）
        Button(
            onClick = onPaymentComplete,
            enabled = canProceed,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .border(
                    width = 2.dp,
                    brush = if (canProceed) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF00FF88),
                                Color(0xFF00DDFF)
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Gray.copy(alpha = 0.5f),
                                Color.Gray.copy(alpha = 0.5f)
                            )
                        )
                    },
                    shape = RoundedCornerShape(28.dp)
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (canProceed) {
                    Text(
                        text = "我已完成支付",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        style = LocalTextStyle.current.copy(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF00FF88),
                                    Color(0xFF00DDFF)
                                )
                            )
                        )
                    )
                } else {
                    Text(
                        text = "我已完成支付 (${countdown}s)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

/**
 * 联系作者页面
 */
@Composable
private fun ContactAuthorPage(
    onClose: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "contact")
    
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .clip(RoundedCornerShape(32.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F3460)
                    )
                )
            )
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 成功图标
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00FF88).copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✓",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF00FF88),
                style = LocalTextStyle.current.copy(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color(0xFF00FF88).copy(alpha = glow),
                        offset = Offset(0f, 0f),
                        blurRadius = 30f
                    )
                )
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 标题
        Text(
            text = "支付成功！",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            style = LocalTextStyle.current.copy(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color(0xFF00FF88).copy(alpha = 0.6f),
                    offset = Offset(0f, 0f),
                    blurRadius = 20f
                )
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "感谢您的支持！",
            fontSize = 18.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // 联系信息卡片
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF00FF88).copy(alpha = 0.15f),
                            Color(0xFF00DDFF).copy(alpha = 0.15f)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00FF88).copy(alpha = 0.5f),
                            Color(0xFF00DDFF).copy(alpha = 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "请添加作者微信",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 微信号
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "18821943198",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD700),
                        letterSpacing = 2.sp,
                        style = LocalTextStyle.current.copy(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color(0xFFFFD700).copy(alpha = glow * 0.8f),
                                offset = Offset(0f, 0f),
                                blurRadius = 20f
                            )
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "添加时请备注：VIP开通",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 完成按钮
        Button(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF00FF88),
                                Color(0xFF00DDFF)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "我知道了",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
