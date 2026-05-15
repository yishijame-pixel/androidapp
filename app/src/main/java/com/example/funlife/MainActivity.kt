// MainActivity.kt - 主活动
package com.example.funlife

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material3.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.funlife.navigation.NavGraph
import com.example.funlife.navigation.Screen
import com.example.funlife.ui.theme.FunLifeTheme
import com.example.funlife.utils.SoundEffectManager
import com.example.funlife.utils.SoundEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var soundManager: SoundEffectManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化音效管理器
        soundManager = SoundEffectManager.getInstance(this)
        
        // 🔥 初始化应用数据（头像框等）
        initializeAppData()
        
        // 切换到正常主题
        setTheme(R.style.Theme_FunLife)
        
        setContent {
            FunLifeTheme {
                var showSplash by remember { mutableStateOf(true) }
                
                if (showSplash) {
                    com.example.funlife.ui.screens.SplashScreen(
                        onTimeout = { showSplash = false }
                    )
                } else {
                    MainScreen(soundManager = soundManager)
                }
            }
        }
    }
    
    /**
     * 初始化应用数据
     * 在后台线程执行，不阻塞UI
     */
    private fun initializeAppData() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val database = (application as FunLifeApplication).database
                com.example.funlife.utils.AppInitializer.initialize(
                    context = applicationContext,
                    shopDao = database.shopDao()
                )
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "应用数据初始化失败", e)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(soundManager: SoundEffectManager) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // 创建 AuthViewModel
    val authViewModel: com.example.funlife.viewmodel.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    
    // 底部导航项 - 使用自定义图标
    val bottomNavItems = listOf(
        BottomNavItem(
            screen = Screen.Home,
            icon = Icons.Default.Home,
            iconRes = R.drawable.nav_icon_1,
            label = "首页"
        ),
        BottomNavItem(
            screen = Screen.Habit,
            icon = Icons.Default.CheckCircle,
            iconRes = R.drawable.nav_icon_2,
            label = "习惯"
        ),
        BottomNavItem(
            screen = Screen.Mood,
            icon = Icons.Default.FavoriteBorder,
            iconRes = R.drawable.nav_icon_3,
            label = "心情"
        ),
        BottomNavItem(
            screen = Screen.Profile,
            icon = Icons.Default.Person,
            iconRes = R.drawable.nav_icon_4,
            label = "我的"
        )
    )
    
    // 判断是否显示底部导航栏（登录/注册/欢迎页/宠物页/游戏计分页/商城页/背包页/转盘页/纪念日页/目标页/VIP页/头像框商城不显示）
    val showBottomBar = currentDestination?.route !in listOf(
        Screen.Welcome.route,
        Screen.Login.route,
        Screen.Register.route,
        "pet",
        Screen.ScoreCounter.route,
        "shop",
        Screen.Inventory.route,
        "spin_wheel",
        "anniversary",
        Screen.Goal.route,
        Screen.Vip.route,
        Screen.AvatarFrameShop.route
    )
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            // VipProfile页面不显示TopBar
            val showTopBar = false  // 不显示TopBar
            
            if (showTopBar) {
                // 美化的顶部导航栏 - 带装饰元素
                Surface(
                    shadowElevation = 6.dp,
                    tonalElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    ) {
                        // 渐变背景
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surface,
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        )
                                    )
                                )
                        )
                        
                        // 装饰性圆形背景
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 40.dp, y = (-20).dp)
                                .size(120.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        )
                        
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .offset(x = (-30).dp, y = 30.dp)
                                .size(100.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        )
                        
                        // 标题内容
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // 根据页面显示不同的装饰图标
                            val (emoji, title) = when (currentDestination?.route) {
                                Screen.Home.route -> "🏠" to "首页"
                                Screen.Habit.route -> "📅" to "打卡"
                                Screen.Mood.route -> "😊" to "心情"
                                Screen.Goal.route -> "🎯" to "目标"
                                Screen.VipProfile.route -> "👤" to "我的"
                                else -> "🎉" to "FunLife"
                            }
                            
                            // Emoji 图标
                            Text(
                                text = emoji,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 32.sp
                                ),
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            
                            // 标题文字
                            Column(
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                // 装饰性下划线
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .height(3.dp)
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {}
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 主内容区域
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
            ) {
                NavGraph(
                    navController = navController,
                    modifier = Modifier.fillMaxSize(),
                    authViewModel = authViewModel
                )
            }
            
            // 底部导航栏 - 覆盖在内容上方
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(90.dp)
                ) {
                    // 背景图片
                    Image(
                        painter = painterResource(id = R.drawable.nav_bg),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                    
                    // 导航项
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bottomNavItems.forEachIndexed { index, item ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == item.screen.route
                            } == true
                            
                            BottomNavItem(
                                item = item,
                                selected = selected,
                                soundManager = soundManager,
                                navIndex = index,
                                onClick = {
                                    if (currentDestination?.route == item.screen.route) {
                                        return@BottomNavItem
                                    }
                                    
                                    navController.navigate(item.screen.route) {
                                        popUpTo(Screen.Home.route) {
                                            inclusive = false
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    item: BottomNavItem,
    selected: Boolean,
    soundManager: SoundEffectManager,
    navIndex: Int,
    onClick: () -> Unit
) {
    // 缩放动画 - 增强选中效果
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    // 所有导航按钮都使用相同的音效
    val soundEffect = SoundEffect.NAV_HOME
    
    Column(
        modifier = Modifier
            .clickable(
                onClick = {
                    // 播放音效
                    soundManager.play(soundEffect, volume = 0.6f)
                    onClick()
                },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .wrapContentWidth()
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(56.dp)
        ) {
            // 选中时的背景圆圈
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.3f),
                                    Color.White.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }
            
            // 使用自定义图标 - 大幅增加尺寸，移除透明度
            Image(
                painter = painterResource(id = item.iconRes),
                contentDescription = item.label,
                modifier = Modifier
                    .size(if (selected) 52.dp else 48.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                contentScale = ContentScale.Fit
            )
        }
        
        Spacer(modifier = Modifier.height(1.dp))
        
        // 文字 - 优化字号和间距，确保完整显示
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = if (selected) 11.sp else 10.sp,
                letterSpacing = 0.sp,
                lineHeight = 12.sp
            ),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.Black else Color.Black.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Visible
        )
    }
}

// 底部导航项数据类
data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val iconRes: Int,
    val label: String
)
