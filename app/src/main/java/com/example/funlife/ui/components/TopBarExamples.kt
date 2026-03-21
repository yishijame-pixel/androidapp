package com.example.funlife.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 这个文件展示了如何在不同页面中使用 EnhancedTopBar
 * 复制相应的代码到你的页面中即可
 */

// ============ 示例 1: 习惯打卡页面 ============
@Composable
fun HabitScreenWithEnhancedTopBar() {
    Column(modifier = Modifier.fillMaxSize()) {
        EnhancedTopBar(
            title = "习惯打卡",
            subtitle = "坚持每一天",
            icon = Icons.Default.CheckCircle,
            gradientColors = TopBarGradients.Green,
            actions = listOf(
                TopBarAction(
                    icon = Icons.Default.Add,
                    contentDescription = "添加习惯",
                    onClick = { /* 打开添加对话框 */ }
                ),
                TopBarAction(
                    icon = Icons.Default.BarChart,
                    contentDescription = "统计",
                    onClick = { /* 查看统计 */ }
                )
            )
        )
        
        // 页面内容
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            // 你的内容
        }
    }
}

// ============ 示例 2: 目标管理页面 ============
@Composable
fun GoalScreenWithEnhancedTopBar() {
    Column(modifier = Modifier.fillMaxSize()) {
        EnhancedTopBar(
            title = "目标管理",
            subtitle = "追逐梦想",
            icon = Icons.Default.Flag,
            gradientColors = TopBarGradients.Orange,
            actions = listOf(
                TopBarAction(
                    icon = Icons.Default.Add,
                    contentDescription = "添加目标",
                    onClick = { /* 添加目标 */ },
                    highlighted = true
                ),
                TopBarAction(
                    icon = Icons.Default.FilterList,
                    contentDescription = "筛选",
                    onClick = { /* 筛选 */ }
                )
            )
        )
        
        // 页面内容
    }
}

// ============ 示例 3: 心情日记页面 ============
@Composable
fun MoodScreenWithEnhancedTopBar() {
    Column(modifier = Modifier.fillMaxSize()) {
        EnhancedTopBar(
            title = "心情日记",
            subtitle = "记录美好时光",
            icon = Icons.Default.Favorite,
            gradientColors = TopBarGradients.Pink,
            actions = listOf(
                TopBarAction(
                    icon = Icons.Default.Add,
                    contentDescription = "写日记",
                    onClick = { /* 写日记 */ }
                ),
                TopBarAction(
                    icon = Icons.Default.CalendarToday,
                    contentDescription = "日历",
                    onClick = { /* 日历视图 */ }
                )
            )
        )
        
        // 页面内容
    }
}

// ============ 示例 4: 纪念日页面 ============
@Composable
fun AnniversaryScreenWithEnhancedTopBar() {
    Column(modifier = Modifier.fillMaxSize()) {
        EnhancedTopBar(
            title = "纪念日",
            subtitle = "珍藏重要时刻",
            icon = Icons.Default.Cake,
            gradientColors = TopBarGradients.Purple,
            actions = listOf(
                TopBarAction(
                    icon = Icons.Default.Add,
                    contentDescription = "添加纪念日",
                    onClick = { /* 添加 */ }
                ),
                TopBarAction(
                    icon = Icons.Default.Notifications,
                    contentDescription = "提醒",
                    onClick = { /* 提醒设置 */ },
                    badge = "3"
                )
            )
        )
        
        // 页面内容
    }
}

// ============ 示例 5: 转盘页面 ============
@Composable
fun SpinWheelScreenWithEnhancedTopBar() {
    Column(modifier = Modifier.fillMaxSize()) {
        EnhancedTopBar(
            title = "幸运转盘",
            subtitle = "试试你的运气",
            icon = Icons.Default.Casino,
            gradientColors = TopBarGradients.DeepPurple,
            actions = listOf(
                TopBarAction(
                    icon = Icons.Default.Settings,
                    contentDescription = "设置",
                    onClick = { /* 设置 */ }
                ),
                TopBarAction(
                    icon = Icons.Default.History,
                    contentDescription = "历史",
                    onClick = { /* 历史记录 */ }
                )
            )
        )
        
        // 页面内容
    }
}

// ============ 示例 6: 商店页面 ============
@Composable
fun ShopScreenWithEnhancedTopBar() {
    Column(modifier = Modifier.fillMaxSize()) {
        EnhancedTopBar(
            title = "积分商店",
            subtitle = "兑换精美奖励",
            icon = Icons.Default.Store,
            gradientColors = TopBarGradients.Teal,
            actions = listOf(
                TopBarAction(
                    icon = Icons.Default.ShoppingCart,
                    contentDescription = "购物车",
                    onClick = { /* 购物车 */ },
                    badge = "2"
                ),
                TopBarAction(
                    icon = Icons.Default.AccountBalanceWallet,
                    contentDescription = "我的积分",
                    onClick = { /* 积分详情 */ }
                )
            )
        )
        
        // 页面内容
    }
}

// ============ 示例 7: 统计页面 ============
@Composable
fun StatisticsScreenWithEnhancedTopBar() {
    Column(modifier = Modifier.fillMaxSize()) {
        EnhancedTopBar(
            title = "数据统计",
            subtitle = "了解你的进步",
            icon = Icons.Default.Analytics,
            gradientColors = TopBarGradients.Blue,
            actions = listOf(
                TopBarAction(
                    icon = Icons.Default.DateRange,
                    contentDescription = "时间范围",
                    onClick = { /* 选择时间 */ }
                ),
                TopBarAction(
                    icon = Icons.Default.Share,
                    contentDescription = "分享",
                    onClick = { /* 分享 */ }
                )
            )
        )
        
        // 页面内容
    }
}

// ============ 示例 8: 设置页面 ============
@Composable
fun SettingsScreenWithEnhancedTopBar() {
    Column(modifier = Modifier.fillMaxSize()) {
        EnhancedTopBar(
            title = "设置",
            subtitle = "个性化你的体验",
            icon = Icons.Default.Settings,
            gradientColors = TopBarGradients.Indigo,
            actions = listOf(
                TopBarAction(
                    icon = Icons.Default.Help,
                    contentDescription = "帮助",
                    onClick = { /* 帮助 */ }
                ),
                TopBarAction(
                    icon = Icons.Default.Info,
                    contentDescription = "关于",
                    onClick = { /* 关于 */ }
                )
            )
        )
        
        // 页面内容
    }
}

// ============ 示例 9: 个人资料页面 ============
@Composable
fun ProfileScreenWithEnhancedTopBar() {
    Column(modifier = Modifier.fillMaxSize()) {
        EnhancedTopBar(
            title = "个人资料",
            subtitle = "管理你的账户",
            icon = Icons.Default.Person,
            gradientColors = TopBarGradients.Pink,
            actions = listOf(
                TopBarAction(
                    icon = Icons.Default.Edit,
                    contentDescription = "编辑",
                    onClick = { /* 编辑资料 */ }
                ),
                TopBarAction(
                    icon = Icons.Default.Logout,
                    contentDescription = "退出登录",
                    onClick = { /* 退出 */ }
                )
            )
        )
        
        // 页面内容
    }
}

/**
 * 使用说明：
 * 
 * 1. 在你的 Screen 文件顶部添加导入：
 *    import com.example.funlife.ui.components.EnhancedTopBar
 *    import com.example.funlife.ui.components.TopBarAction
 *    import com.example.funlife.ui.components.TopBarGradients
 * 
 * 2. 将你的页面内容包裹在 Column 中，并在顶部添加 EnhancedTopBar
 * 
 * 3. 根据页面功能选择合适的渐变色：
 *    - TopBarGradients.Purple (紫色 - 默认)
 *    - TopBarGradients.Blue (蓝色 - 统计、数据)
 *    - TopBarGradients.Green (绿色 - 习惯、健康)
 *    - TopBarGradients.Orange (橙色 - 目标、任务)
 *    - TopBarGradients.Pink (粉色 - 心情、社交)
 *    - TopBarGradients.Teal (青色 - 商店、购物)
 *    - TopBarGradients.DeepPurple (深紫 - 游戏、娱乐)
 *    - TopBarGradients.Indigo (靛蓝 - 设置、系统)
 * 
 * 4. 添加操作按钮时，可以设置：
 *    - badge: 显示徽章数字（如通知数量）
 *    - highlighted: 高亮显示（更明显的背景）
 */
