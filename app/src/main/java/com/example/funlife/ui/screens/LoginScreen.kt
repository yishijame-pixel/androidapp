// LoginScreen.kt - 可爱小狗主题登录页
package com.example.funlife.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.resource.ResourceStore
import com.example.funlife.viewmodel.AuthState
import com.example.funlife.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) } // 密码可见性状态
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorShakeKey by remember { mutableStateOf(0) }
    
    val authState by viewModel.authState.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    
    var visible by remember { mutableStateOf(false) }
    
    // 加载背景图片 - 使用高质量设置
    val backgroundBitmap = remember {
        try {
            ResourceStore.openInputStream("login/login_1.png")?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                    inScaled = false
                    inDither = false
                }
                BitmapFactory.decodeStream(inputStream, null, options)?.asImageBitmap()
            }
        } catch (e: Exception) {
            null
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.resetAuthState()
        delay(100)
        visible = true
    }
    
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                isLoading = false
                val recovered = (authState as AuthState.Success).recoveredFromCloud
                android.widget.Toast.makeText(
                    context,
                    if (recovered) {
                        "已从云端恢复账号，金币/积分已同步（个人数据需备份恢复）"
                    } else {
                        "登录成功 🎉 欢迎回来！"
                    },
                    android.widget.Toast.LENGTH_LONG,
                ).show()
                onLoginSuccess()
                viewModel.resetAuthState()
            }
            is AuthState.Error -> {
                isLoading = false
                errorMessage = (authState as AuthState.Error).message
                showError = true
                errorShakeKey++
                viewModel.resetAuthState()
            }
            is AuthState.Banned -> {
                isLoading = false
                errorMessage = "🚫 账号已被封禁\n${(authState as AuthState.Banned).reason}"
                showError = true
                errorShakeKey++
                viewModel.resetAuthState()
            }
            is AuthState.Loading -> {
                isLoading = true
                showError = false
            }
            is AuthState.Idle -> {
                isLoading = false
            }
            else -> {}
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 背景图片（不参与 imePadding，键盘弹起时背景仍铺满）
        backgroundBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                filterQuality = androidx.compose.ui.graphics.FilterQuality.High
            )
        }

        // 内容区域 — imePadding 让容器随键盘自动收缩；
        // verticalScroll 让用户在小屏 / 键盘弹起时能滚动到输入框
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            val screenHeight = maxHeight

            // 登录表单 — 用可滚动 Column + padding-top 替代 offset，
            // 这样键盘弹起后整体上移、且能滚动
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 40.dp)
                    .padding(top = screenHeight * 0.54f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(600)) + slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = tween(600)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp) // 增加间距
                    ) {
                        // 用户名输入框
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            placeholder = { 
                                Text(
                                    "用户名",
                                    color = Color.Gray.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                ) 
                            },
                            leadingIcon = {
                                // 可爱的狗爪图标 - 美化版
                                Canvas(
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    val pawColor1 = Color(0xFFFFB74D) // 浅橙色
                                    val pawColor2 = Color(0xFFFF9E80) // 深橙色
                                    
                                    // 主爪垫 - 带渐变
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(pawColor1, pawColor2),
                                            center = Offset(size.width * 0.5f, size.height * 0.65f),
                                            radius = size.minDimension * 0.28f
                                        ),
                                        radius = size.minDimension * 0.28f,
                                        center = Offset(size.width * 0.5f, size.height * 0.68f)
                                    )
                                    
                                    // 四个小爪垫 - 更圆润
                                    // 左上
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(pawColor1, pawColor2)
                                        ),
                                        radius = size.minDimension * 0.14f,
                                        center = Offset(size.width * 0.22f, size.height * 0.28f)
                                    )
                                    // 中上
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(pawColor1, pawColor2)
                                        ),
                                        radius = size.minDimension * 0.15f,
                                        center = Offset(size.width * 0.5f, size.height * 0.18f)
                                    )
                                    // 右上
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(pawColor1, pawColor2)
                                        ),
                                        radius = size.minDimension * 0.14f,
                                        center = Offset(size.width * 0.78f, size.height * 0.28f)
                                    )
                                    // 右侧
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(pawColor1, pawColor2)
                                        ),
                                        radius = size.minDimension * 0.11f,
                                        center = Offset(size.width * 0.88f, size.height * 0.48f)
                                    )
                                }
                            },
                            textStyle = TextStyle(fontSize = 13.sp),
                            shape = RoundedCornerShape(25.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFFE0E0E0),
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                focusedTextColor = Color(0xFF333333),
                                unfocusedTextColor = Color(0xFF333333),
                                cursorColor = Color(0xFFFF9E80)
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next,
                                autoCorrect = false
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            singleLine = true,
                            maxLines = 1
                        )
                        
                        // 密码输入框
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            placeholder = { 
                                Text(
                                    "密码",
                                    color = Color.Gray.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                ) 
                            },
                            leadingIcon = {
                                // 可爱的狗爪图标 - 和用户名框一样
                                Canvas(
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    val pawColor1 = Color(0xFFFFB74D) // 浅橙色
                                    val pawColor2 = Color(0xFFFF9E80) // 深橙色
                                    
                                    // 主爪垫 - 带渐变
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(pawColor1, pawColor2),
                                            center = Offset(size.width * 0.5f, size.height * 0.65f),
                                            radius = size.minDimension * 0.28f
                                        ),
                                        radius = size.minDimension * 0.28f,
                                        center = Offset(size.width * 0.5f, size.height * 0.68f)
                                    )
                                    
                                    // 四个小爪垫 - 更圆润
                                    // 左上
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(pawColor1, pawColor2)
                                        ),
                                        radius = size.minDimension * 0.14f,
                                        center = Offset(size.width * 0.22f, size.height * 0.28f)
                                    )
                                    // 中上
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(pawColor1, pawColor2)
                                        ),
                                        radius = size.minDimension * 0.15f,
                                        center = Offset(size.width * 0.5f, size.height * 0.18f)
                                    )
                                    // 右上
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(pawColor1, pawColor2)
                                        ),
                                        radius = size.minDimension * 0.14f,
                                        center = Offset(size.width * 0.78f, size.height * 0.28f)
                                    )
                                    // 右侧
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(pawColor1, pawColor2)
                                        ),
                                        radius = size.minDimension * 0.11f,
                                        center = Offset(size.width * 0.88f, size.height * 0.48f)
                                    )
                                }
                            },
                            textStyle = TextStyle(fontSize = 13.sp),
                            trailingIcon = {
                                // 可爱的小狗头图标
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible }
                                ) {
                                    Canvas(
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        val dogColor = Color(0xFFFFB74D)
                                        val darkColor = Color(0xFFFF9E80)
                                        
                                        // 狗头（圆形）
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(dogColor, darkColor)
                                            ),
                                            radius = size.minDimension * 0.4f,
                                            center = Offset(size.width * 0.5f, size.height * 0.5f)
                                        )
                                        
                                        // 左耳
                                        drawCircle(
                                            color = darkColor,
                                            radius = size.minDimension * 0.15f,
                                            center = Offset(size.width * 0.25f, size.height * 0.25f)
                                        )
                                        
                                        // 右耳
                                        drawCircle(
                                            color = darkColor,
                                            radius = size.minDimension * 0.15f,
                                            center = Offset(size.width * 0.75f, size.height * 0.25f)
                                        )
                                        
                                        if (passwordVisible) {
                                            // 睁开眼睛 - 密码可见
                                            // 左眼
                                            drawCircle(
                                                color = Color(0xFF333333),
                                                radius = size.minDimension * 0.08f,
                                                center = Offset(size.width * 0.35f, size.height * 0.45f)
                                            )
                                            drawCircle(
                                                color = Color.White,
                                                radius = size.minDimension * 0.03f,
                                                center = Offset(size.width * 0.37f, size.height * 0.43f)
                                            )
                                            
                                            // 右眼
                                            drawCircle(
                                                color = Color(0xFF333333),
                                                radius = size.minDimension * 0.08f,
                                                center = Offset(size.width * 0.65f, size.height * 0.45f)
                                            )
                                            drawCircle(
                                                color = Color.White,
                                                radius = size.minDimension * 0.03f,
                                                center = Offset(size.width * 0.67f, size.height * 0.43f)
                                            )
                                        } else {
                                            // 用爪子捂眼睛 - 密码隐藏
                                            // 闭着的眼睛
                                            drawLine(
                                                color = Color(0xFF333333),
                                                start = Offset(size.width * 0.28f, size.height * 0.45f),
                                                end = Offset(size.width * 0.42f, size.height * 0.45f),
                                                strokeWidth = 3.dp.toPx(),
                                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                                            )
                                            drawLine(
                                                color = Color(0xFF333333),
                                                start = Offset(size.width * 0.58f, size.height * 0.45f),
                                                end = Offset(size.width * 0.72f, size.height * 0.45f),
                                                strokeWidth = 3.dp.toPx(),
                                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                                            )
                                            
                                            // 小爪子（捂在眼睛上）
                                            // 左爪子
                                            drawCircle(
                                                color = darkColor.copy(alpha = 0.8f),
                                                radius = size.minDimension * 0.12f,
                                                center = Offset(size.width * 0.35f, size.height * 0.42f)
                                            )
                                            // 爪子的小肉垫
                                            drawCircle(
                                                color = Color(0xFFFF8A65),
                                                radius = size.minDimension * 0.03f,
                                                center = Offset(size.width * 0.32f, size.height * 0.38f)
                                            )
                                            drawCircle(
                                                color = Color(0xFFFF8A65),
                                                radius = size.minDimension * 0.03f,
                                                center = Offset(size.width * 0.38f, size.height * 0.38f)
                                            )
                                            
                                            // 右爪子
                                            drawCircle(
                                                color = darkColor.copy(alpha = 0.8f),
                                                radius = size.minDimension * 0.12f,
                                                center = Offset(size.width * 0.65f, size.height * 0.42f)
                                            )
                                            // 爪子的小肉垫
                                            drawCircle(
                                                color = Color(0xFFFF8A65),
                                                radius = size.minDimension * 0.03f,
                                                center = Offset(size.width * 0.62f, size.height * 0.38f)
                                            )
                                            drawCircle(
                                                color = Color(0xFFFF8A65),
                                                radius = size.minDimension * 0.03f,
                                                center = Offset(size.width * 0.68f, size.height * 0.38f)
                                            )
                                        }
                                        
                                        // 鼻子
                                        drawCircle(
                                            color = Color(0xFF333333),
                                            radius = size.minDimension * 0.06f,
                                            center = Offset(size.width * 0.5f, size.height * 0.58f)
                                        )
                                        
                                        // 嘴巴（微笑）
                                        val mouthPath = Path().apply {
                                            moveTo(size.width * 0.5f, size.height * 0.58f)
                                            quadraticBezierTo(
                                                size.width * 0.45f, size.height * 0.68f,
                                                size.width * 0.38f, size.height * 0.65f
                                            )
                                            moveTo(size.width * 0.5f, size.height * 0.58f)
                                            quadraticBezierTo(
                                                size.width * 0.55f, size.height * 0.68f,
                                                size.width * 0.62f, size.height * 0.65f
                                            )
                                        }
                                        drawPath(
                                            path = mouthPath,
                                            color = Color(0xFF333333),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                width = 2.dp.toPx(),
                                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                                            )
                                        )
                                    }
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            shape = RoundedCornerShape(25.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFFE0E0E0),
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                focusedTextColor = Color(0xFF333333),
                                unfocusedTextColor = Color(0xFF333333),
                                cursorColor = Color(0xFFFF9E80)
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                                autoCorrect = false
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (username.isNotEmpty() && password.isNotEmpty()) {
                                        viewModel.login(username, password)
                                    }
                                }
                            ),
                            singleLine = true,
                            maxLines = 1
                        )
                        
                        // 错误提示
                        AnimatedVisibility(
                            visible = showError,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            val shakeOffset = remember { Animatable(0f) }
                            
                            LaunchedEffect(errorShakeKey) {
                                if (errorShakeKey > 0) {
                                    shakeOffset.snapTo(0f)
                                    shakeOffset.animateTo(
                                        targetValue = 0f,
                                        animationSpec = keyframes {
                                            durationMillis = 500
                                            -15f at 50
                                            15f at 100
                                            -15f at 150
                                            15f at 200
                                            -10f at 250
                                            10f at 300
                                            -5f at 350
                                            5f at 400
                                            0f at 500
                                        }
                                    )
                                }
                            }
                            
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(x = shakeOffset.value.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFFFCDD2)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        errorMessage,
                                        fontSize = 13.sp,
                                        color = Color(0xFFD32F2F)
                                    )
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(6.dp))
                        
                        // 登录按钮
                        Button(
                            onClick = {
                                showError = false
                                when {
                                    username.isEmpty() -> {
                                        errorMessage = "请输入用户名"
                                        showError = true
                                        errorShakeKey++
                                    }
                                    password.isEmpty() -> {
                                        errorMessage = "请输入密码"
                                        showError = true
                                        errorShakeKey++
                                    }
                                    else -> viewModel.login(username, password)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(0.dp),
                            enabled = !isLoading
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFFFFB74D),
                                                Color(0xFFFF9E80)
                                            )
                                        ),
                                        RoundedCornerShape(28.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "🐾",
                                            fontSize = 20.sp
                                        )
                                        Text(
                                            "登录",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp)) // 减小间距
                        
                        // 注册提示
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF64B5F6),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "还没有账号？",
                                fontSize = 14.sp,
                                color = Color(0xFF666666)
                            )
                            TextButton(
                                onClick = onNavigateToRegister,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    "立即注册",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64B5F6)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
