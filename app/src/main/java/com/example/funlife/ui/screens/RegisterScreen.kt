// RegisterScreen.kt - 可爱小狗主题注册页
package com.example.funlife.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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

@OptIn(ExperimentalMaterial3Api::class)
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
    var errorShakeKey by remember { mutableStateOf(0) }
    
    val authState by viewModel.authState.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    
    var visible by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var successUsername by remember { mutableStateOf("") }
    
    // 加载背景图片 - 使用高质量设置
    val backgroundBitmap = remember {
        try {
            context.assets.open("login/zhuce.png").use { inputStream ->
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
    
    // 加载图标图片
    val icon1Bitmap = remember {
        try {
            context.assets.open("login/tubiao_1.png").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
            }
        } catch (e: Exception) {
            null
        }
    }
    
    val icon2Bitmap = remember {
        try {
            context.assets.open("login/tubiao_2.png").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
            }
        } catch (e: Exception) {
            null
        }
    }
    
    val icon3Bitmap = remember {
        try {
            context.assets.open("login/tubiao_3.png").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
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
            is AuthState.RegisterSuccess -> {
                isLoading = false
                successUsername = (authState as AuthState.RegisterSuccess).username
                showSuccessMessage = true
                delay(2000)
                onNavigateToLogin()
                viewModel.resetAuthState()
            }
            is AuthState.Error -> {
                isLoading = false
                errorMessage = (authState as AuthState.Error).message
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
        modifier = Modifier.fillMaxSize()
    ) {
        // 背景图片 - 高质量显示
        backgroundBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                filterQuality = androidx.compose.ui.graphics.FilterQuality.High
            )
        }
        
        // 内容区域 - 使用BoxWithConstraints获取屏幕高度
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val screenHeight = maxHeight
            
            // 注册表单 - 定位在白色区域中心，使用滚动
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 40.dp)
                    .padding(top = screenHeight * 0.30f), // 从屏幕30%开始
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 用户名标签
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        color = Color(0xFFE94560),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                            Text(
                                "用户名",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF333333)
                            )
                        }
                        
                        // 用户名输入框
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            placeholder = { 
                                Text(
                                    "用户名",
                                    color = Color.Gray.copy(alpha = 0.6f),
                                    fontSize = 15.sp
                                ) 
                            },
                            leadingIcon = {
                                // 小狗头图标 (tubiao_1)
                                icon1Bitmap?.let { bitmap ->
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .padding(start = 4.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            },
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
                        
                        // 昵称标签
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        color = Color(0xFFFFD700),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                            Text(
                                "昵称",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF333333)
                            )
                        }
                        
                        // 昵称输入框
                        OutlinedTextField(
                            value = nickname,
                            onValueChange = { nickname = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            placeholder = { 
                                Text(
                                    "昵称（可选）",
                                    color = Color.Gray.copy(alpha = 0.6f),
                                    fontSize = 15.sp
                                ) 
                            },
                            leadingIcon = {
                                // 小狗头图标 (tubiao_1)
                                icon1Bitmap?.let { bitmap ->
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .padding(start = 4.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            },
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
                        
                        // 密码标签
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        color = Color(0xFF00D9FF),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                            Text(
                                "密码",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF333333)
                            )
                        }
                        
                        // 密码输入框
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            placeholder = { 
                                Text(
                                    "密码",
                                    color = Color.Gray.copy(alpha = 0.6f),
                                    fontSize = 15.sp
                                ) 
                            },
                            leadingIcon = {
                                // 锁图标 (tubiao_2)
                                icon2Bitmap?.let { bitmap ->
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .padding(start = 4.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            },
                            trailingIcon = {
                                // 可爱的小狗头图标
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible }
                                ) {
                                    Canvas(
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        val dogColor = Color(0xFFFFB74D)
                                        val darkColor = Color(0xFFFF9E80)
                                        
                                        // 狗头
                                        drawCircle(
                                            brush = Brush.radialGradient(colors = listOf(dogColor, darkColor)),
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
                                            // 睁开眼睛
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
                                            // 用爪子捂眼睛
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
                                            
                                            // 左爪子
                                            drawCircle(
                                                color = darkColor.copy(alpha = 0.8f),
                                                radius = size.minDimension * 0.12f,
                                                center = Offset(size.width * 0.35f, size.height * 0.42f)
                                            )
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
                                        
                                        // 嘴巴
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
                                imeAction = ImeAction.Next,
                                autoCorrect = false
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            singleLine = true,
                            maxLines = 1
                        )
                        
                        // 确认密码标签
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        color = Color(0xFF00D9FF),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                            Text(
                                "确认密码",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF333333)
                            )
                        }
                        
                        // 确认密码输入框
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            placeholder = { 
                                Text(
                                    "确认密码",
                                    color = Color.Gray.copy(alpha = 0.6f),
                                    fontSize = 15.sp
                                ) 
                            },
                            leadingIcon = {
                                // 小狗头图标 (tubiao_1)
                                icon1Bitmap?.let { bitmap ->
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .padding(start = 4.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            },
                            trailingIcon = {
                                // 可爱的小狗头图标
                                IconButton(
                                    onClick = { confirmPasswordVisible = !confirmPasswordVisible }
                                ) {
                                    Canvas(
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        val dogColor = Color(0xFFFFB74D)
                                        val darkColor = Color(0xFFFF9E80)
                                        
                                        drawCircle(
                                            brush = Brush.radialGradient(colors = listOf(dogColor, darkColor)),
                                            radius = size.minDimension * 0.4f,
                                            center = Offset(size.width * 0.5f, size.height * 0.5f)
                                        )
                                        
                                        drawCircle(
                                            color = darkColor,
                                            radius = size.minDimension * 0.15f,
                                            center = Offset(size.width * 0.25f, size.height * 0.25f)
                                        )
                                        
                                        drawCircle(
                                            color = darkColor,
                                            radius = size.minDimension * 0.15f,
                                            center = Offset(size.width * 0.75f, size.height * 0.25f)
                                        )
                                        
                                        if (confirmPasswordVisible) {
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
                                            
                                            drawCircle(
                                                color = darkColor.copy(alpha = 0.8f),
                                                radius = size.minDimension * 0.12f,
                                                center = Offset(size.width * 0.35f, size.height * 0.42f)
                                            )
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
                                            
                                            drawCircle(
                                                color = darkColor.copy(alpha = 0.8f),
                                                radius = size.minDimension * 0.12f,
                                                center = Offset(size.width * 0.65f, size.height * 0.42f)
                                            )
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
                                        
                                        drawCircle(
                                            color = Color(0xFF333333),
                                            radius = size.minDimension * 0.06f,
                                            center = Offset(size.width * 0.5f, size.height * 0.58f)
                                        )
                                        
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
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                                imeAction = ImeAction.Next,
                                autoCorrect = false
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            singleLine = true,
                            maxLines = 1
                        )
                        
                        // 内测码标签
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        color = Color(0xFFFFD700),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                            Text(
                                "内测码",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF333333)
                            )
                        }
                        
                        // 内测码输入框
                        OutlinedTextField(
                            value = betaCode,
                            onValueChange = { betaCode = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            placeholder = { 
                                Text(
                                    "内测码",
                                    color = Color.Gray.copy(alpha = 0.6f),
                                    fontSize = 15.sp
                                ) 
                            },
                            leadingIcon = {
                                // 钥匙图标 (tubiao_3)
                                icon3Bitmap?.let { bitmap ->
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .padding(start = 4.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            },
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
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                                autoCorrect = false
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
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
                        
                        // 成功提示
                        AnimatedVisibility(
                            visible = showSuccessMessage,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFC8E6C9)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF388E3C),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        "注册成功！正在跳转...",
                                        fontSize = 13.sp,
                                        color = Color(0xFF388E3C)
                                    )
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(6.dp))
                        
                        // 注册按钮
                        Button(
                            onClick = {
                                showError = false
                                when {
                                    username.isEmpty() -> {
                                        errorMessage = "请输入用户名"
                                        showError = true
                                        errorShakeKey++
                                    }
                                    username.length < 3 -> {
                                        errorMessage = "用户名至少需要3个字符"
                                        showError = true
                                        errorShakeKey++
                                    }
                                    password.isEmpty() -> {
                                        errorMessage = "请输入密码"
                                        showError = true
                                        errorShakeKey++
                                    }
                                    password.length < 6 -> {
                                        errorMessage = "密码至少需要6个字符"
                                        showError = true
                                        errorShakeKey++
                                    }
                                    confirmPassword.isEmpty() -> {
                                        errorMessage = "请确认密码"
                                        showError = true
                                        errorShakeKey++
                                    }
                                    password != confirmPassword -> {
                                        errorMessage = "两次密码输入不一致"
                                        showError = true
                                        errorShakeKey++
                                    }
                                    betaCode.isEmpty() -> {
                                        errorMessage = "请输入内测码"
                                        showError = true
                                        errorShakeKey++
                                    }
                                    else -> {
                                        viewModel.register(username, password, nickname, betaCode)
                                    }
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
                                            "注册",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        // 登录提示
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
                                "已有账号？",
                                fontSize = 14.sp,
                                color = Color(0xFF666666)
                            )
                            TextButton(onClick = onNavigateToLogin) {
                                Text(
                                    "立即登录",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64B5F6)
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}
