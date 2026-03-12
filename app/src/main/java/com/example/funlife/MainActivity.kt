// MainActivity.kt - 主活动
package com.example.funlife

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FunLifeTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // 创建 AuthViewModel
    val authViewModel: com.example.funlife.viewmodel.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    
    // 底部导航项
    val bottomNavItems = listOf(
        BottomNavItem(
            screen = Screen.Home,
            icon = Icons.Default.Home,
            label = "首页"
        ),
        BottomNavItem(
            screen = Screen.Habit,
            icon = Icons.Default.CheckCircle,
            label = "打卡"
        ),
        BottomNavItem(
            screen = Screen.Mood,
            icon = Icons.Default.FavoriteBorder,
            label = "心情"
        ),
        BottomNavItem(
            screen = Screen.Goal,
            icon = Icons.Default.Flag,
            label = "目标"
        )
    )
    
    // 判断是否显示底部导航栏（登录/注册/欢迎页不显示）
    val showBottomBar = currentDestination?.route !in listOf(
        Screen.Welcome.route,
        Screen.Login.route,
        Screen.Register.route
    )
    
    Scaffold(
        topBar = {
            // 只在首页和底部导航的页面显示全局TopAppBar
            val showTopBar = currentDestination?.route in listOf(
                Screen.Home.route,
                Screen.Habit.route,
                Screen.Mood.route,
                Screen.Goal.route
            )
            
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
        bottomBar = {
            // 只在主要页面显示底部导航栏
            if (showBottomBar) {
                // 美化的底部导航栏 - 无背景，图标发光效果
                Surface(
                    tonalElevation = 0.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.screen.route
                        } == true
                        
                        BottomNavItem(
                            item = item,
                            selected = selected,
                            onClick = {
                                // 只有当前已经在目标页面时才不导航
                                if (currentDestination?.route == item.screen.route) {
                                    return@BottomNavItem
                                }
                                
                                navController.navigate(item.screen.route) {
                                    // 清除导航栈，但保留首页
                                    popUpTo(Screen.Home.route) {
                                        inclusive = false
                                    }
                                    // 避免重复导航到同一目的地
                                    launchSingleTop = true
                                    // 恢复状态
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            NavGraph(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
                authViewModel = authViewModel
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    // 缩放动画
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    // 透明度动画
    val alpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.7f,
        animationSpec = tween(durationMillis = 200),
        label = "alpha"
    )
    
    Column(
        modifier = Modifier
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(horizontal = 2.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .wrapContentWidth()
            .height(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(36.dp)
        ) {
            // 发光效果（仅选中时显示）
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer {
                            this.alpha = 0.3f
                        }
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }
            
            // 图标
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                modifier = Modifier.size(if (selected) 28.dp else 24.dp),
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        
        // 文字 - 确保完整显示
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                letterSpacing = 0.sp,
                lineHeight = 14.sp
            ),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        
        // 选中指示器（小圆点）
        if (selected) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

// 底部导航项数据类
data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)
