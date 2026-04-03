// LoginScreen.kt - 创意登录页
package com.example.funlife.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.viewmodel.AuthState
import com.example.funlife.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorShakeKey by remember { mutableStateOf(0) }  // 🔥 新增：用于触发震动动画
    
    val authState by viewModel.authState.collectAsState()
    val focusManager = LocalFocusManager.current
    
    var visible by remember { mutableStateOf(false) }
    
    // 🔥 新增：进入页面时重置状态
    LaunchedEffect(Unit) {
        viewModel.resetAuthState()
        delay(100)
        visible = true
    }
    
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                isLoading = false
                onLoginSuccess()
                viewModel.resetAuthState()
            }
            is AuthState.Error -> {
                isLoading = false
                errorMessage = (authState as AuthState.Error).message
                showError = true
                errorShakeKey++  // 🔥 触发震动动画
                // 🔥 立即重置状态，允许下次点击
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
    
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val rotate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E)
                    )
                )
            )
    ) {
        // 创意几何装饰
        Box(
            modifier = Modifier
                .size(250.dp)
                .offset(x = (-80).dp, y = 50.dp)
                .rotate(rotate)
                .alpha(0.1f)
                .background(
                    Color(0xFFE94560),
                    RoundedCornerShape(50.dp)
                )
        )
        
        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .rotate(-rotate * 0.7f)
                .alpha(0.08f)
                .background(
                    Color(0xFF00D9FF),
                    CircleShape
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(30.dp))
            
            // 创意标题区
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600)) + slideInVertically(
                    initialOffsetY = { -it / 2 },
                    animationSpec = tween(600)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "欢迎",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    
                    Text(
                        "回来",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFE94560)
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(3.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFE94560),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Text(
                            "登录继续你的旅程",
                            fontSize = 14.sp,
                            color = Color(0xFF00D9FF)
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(35.dp))
            
            // 登录表单 - 创意卡片
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, delayMillis = 200)) + slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = tween(600, delayMillis = 200)
                )
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(
                        topStart = 40.dp,
                        topEnd = 10.dp,
                        bottomStart = 10.dp,
                        bottomEnd = 40.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F3460).copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 0.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // 用户名
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(Color(0xFFE94560), CircleShape)
                                )
                                Text(
                                    "用户名",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                            
                            TextField(
                                value = username,
                                onValueChange = { username = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { 
                                    Text(
                                        "输入你的用户名",
                                        color = Color.White.copy(alpha = 0.4f)
                                    ) 
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFFE94560)
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White.copy(alpha = 0.1f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color(0xFFE94560)
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
                        }
                        
                        // 密码
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(Color(0xFF00D9FF), CircleShape)
                                )
                                Text(
                                    "密码",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                            
                            TextField(
                                value = password,
                                onValueChange = { password = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { 
                                    Text(
                                        "输入你的密码",
                                        color = Color.White.copy(alpha = 0.4f)
                                    ) 
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF00D9FF)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.Visibility 
                                            else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) 
                                    VisualTransformation.None 
                                else 
                                    PasswordVisualTransformation(),
                                shape = RoundedCornerShape(16.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White.copy(alpha = 0.1f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color(0xFF00D9FF)
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
                        }
                        
                        // 错误提示 - 带震动动画
                        AnimatedVisibility(
                            visible = showError,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            // 🔥 震动动画效果 - 使用 Animatable 确保每次都触发
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
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFE94560).copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        tint = Color(0xFFFF6B9D),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        errorMessage,
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        // 登录按钮
                        Button(
                            onClick = {
                                showError = false
                                when {
                                    username.isEmpty() -> {
                                        errorMessage = "请输入用户名"
                                        showError = true
                                        errorShakeKey++  // 🔥 触发震动
                                    }
                                    password.isEmpty() -> {
                                        errorMessage = "请输入密码"
                                        showError = true
                                        errorShakeKey++  // 🔥 触发震动
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
                                                Color(0xFFE94560),
                                                Color(0xFFFF6B9D)
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
                                    Text(
                                        "登录",
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
            
            Spacer(Modifier.height(20.dp))
            
            // 注册提示
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, delayMillis = 400))
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "还没有账号？",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    TextButton(onClick = onNavigateToRegister) {
                        Text(
                            "立即注册",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00D9FF)
                        )
                    }
                }
            }
        }
    }
}
