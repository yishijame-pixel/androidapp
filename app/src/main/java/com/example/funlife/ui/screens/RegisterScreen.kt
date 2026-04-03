// RegisterScreen.kt - 创意注册页
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
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var betaCode by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorShakeKey by remember { mutableStateOf(0) }  // 🔥 新增：用于触发震动动画
    
    val authState by viewModel.authState.collectAsState()
    val focusManager = LocalFocusManager.current
    
    var visible by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var successUsername by remember { mutableStateOf("") }
    
    // 🔥 新增：进入页面时重置状态
    LaunchedEffect(Unit) {
        viewModel.resetAuthState()
        delay(100)
        visible = true
    }
    
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.RegisterSuccess -> {
                isLoading = false
                // 🔥 修改：注册成功后显示提示，然后跳转到登录页面
                successUsername = (authState as AuthState.RegisterSuccess).username
                showSuccessMessage = true
                delay(2000)  // 显示2秒成功提示
                onNavigateToLogin()
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
            animation = tween(35000, easing = LinearEasing),
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
                .size(220.dp)
                .offset(x = (-70).dp, y = 80.dp)
                .rotate(rotate)
                .alpha(0.1f)
                .background(
                    Color(0xFF00D9FF),
                    RoundedCornerShape(45.dp)
                )
        )
        
        Box(
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = (-30).dp)
                .rotate(-rotate * 0.6f)
                .alpha(0.08f)
                .background(
                    Color(0xFFFFD700),
                    CircleShape
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))
            
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
                        "创建",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    
                    Text(
                        "账号",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00D9FF)
                    )
                    
                    Spacer(Modifier.height(10.dp))
                    
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
                                            Color(0xFF00D9FF),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Text(
                            "加入我们的社区",
                            fontSize = 13.sp,
                            color = Color(0xFFE94560)
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(28.dp))
            
            // 注册表单 - 创意卡片
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
                        topStart = 10.dp,
                        topEnd = 40.dp,
                        bottomStart = 40.dp,
                        bottomEnd = 10.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F3460).copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 0.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // 用户名
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(Color(0xFFE94560), CircleShape)
                                )
                                Text(
                                    "用户名",
                                    fontSize = 12.sp,
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
                                        "至少3个字符",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 14.sp
                                    ) 
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFFE94560),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
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
                        
                        // 昵称
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(Color(0xFFFFD700), CircleShape)
                                )
                                Text(
                                    "昵称（可选）",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                            
                            TextField(
                                value = nickname,
                                onValueChange = { nickname = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { 
                                    Text(
                                        "给自己起个好听的名字",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 14.sp
                                    ) 
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Face,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White.copy(alpha = 0.1f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color(0xFFFFD700)
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
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(Color(0xFF00D9FF), CircleShape)
                                )
                                Text(
                                    "密码",
                                    fontSize = 12.sp,
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
                                        "至少6个字符",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 14.sp
                                    ) 
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF00D9FF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.Visibility 
                                            else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) 
                                    VisualTransformation.None 
                                else 
                                    PasswordVisualTransformation(),
                                shape = RoundedCornerShape(14.dp),
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
                        
                        // 确认密码
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(Color(0xFF00D9FF), CircleShape)
                                )
                                Text(
                                    "确认密码",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                            
                            TextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { 
                                    Text(
                                        "再次输入密码",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 14.sp
                                    ) 
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF00D9FF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                        Icon(
                                            if (confirmPasswordVisible) Icons.Default.Visibility 
                                            else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                visualTransformation = if (confirmPasswordVisible) 
                                    VisualTransformation.None 
                                else 
                                    PasswordVisualTransformation(),
                                shape = RoundedCornerShape(14.dp),
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
                        
                        // 内测码
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(Color(0xFFFFD700), CircleShape)
                                )
                                Text(
                                    "内测码",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                            
                            TextField(
                                value = betaCode,
                                onValueChange = { betaCode = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { 
                                    Text(
                                        "请输入内测码",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 14.sp
                                    ) 
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Key,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White.copy(alpha = 0.1f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color(0xFFFFD700)
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done,
                                    autoCorrect = false
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { 
                                        focusManager.clearFocus() 
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
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        tint = Color(0xFFFF6B9D),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        errorMessage,
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        
                        // 🔥 新增：成功提示
                        AnimatedVisibility(
                            visible = showSuccessMessage,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF00D9FF).copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF00FFF0),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        "注册成功！正在跳转到登录页面...",
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        // 注册按钮
                        Button(
                            onClick = {
                                showError = false
                                when {
                                    username.isEmpty() -> {
                                        errorMessage = "请输入用户名"
                                        showError = true
                                        errorShakeKey++  // 🔥 触发震动
                                    }
                                    username.length < 3 -> {
                                        errorMessage = "用户名至少需要3个字符"
                                        showError = true
                                        errorShakeKey++  // 🔥 触发震动
                                    }
                                    password.isEmpty() -> {
                                        errorMessage = "请输入密码"
                                        showError = true
                                        errorShakeKey++  // 🔥 触发震动
                                    }
                                    password.length < 6 -> {
                                        errorMessage = "密码至少需要6个字符"
                                        showError = true
                                        errorShakeKey++  // 🔥 触发震动
                                    }
                                    confirmPassword.isEmpty() -> {
                                        errorMessage = "请确认密码"
                                        showError = true
                                        errorShakeKey++  // 🔥 触发震动
                                    }
                                    password != confirmPassword -> {
                                        errorMessage = "两次密码输入不一致"
                                        showError = true
                                        errorShakeKey++  // 🔥 触发震动
                                    }
                                    betaCode.isEmpty() -> {
                                        errorMessage = "请输入内测码"
                                        showError = true
                                        errorShakeKey++  // 🔥 触发震动
                                    }
                                    else -> {
                                        viewModel.register(username, password, nickname, betaCode)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(27.dp),
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
                                                Color(0xFF00D9FF),
                                                Color(0xFF00FFF0)
                                            )
                                        ),
                                        RoundedCornerShape(27.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Text(
                                        "注册",
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
            
            Spacer(Modifier.height(16.dp))
            
            // 登录提示
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, delayMillis = 400))
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "已有账号？",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    TextButton(onClick = onNavigateToLogin) {
                        Text(
                            "立即登录",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE94560)
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(20.dp))
        }
    }
}
