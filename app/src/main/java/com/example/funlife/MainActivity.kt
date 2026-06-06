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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // 通知在 App 已存活时被点击，仍要投递 deepLink
        intent.getStringExtra(
            com.example.funlife.notifications.NotificationCenter.EXTRA_DEEP_LINK
        )?.let { com.example.funlife.notifications.DeepLinkBus.publish(it) }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            com.example.funlife.notifications.FcmPushBootstrap.initAsync(this)
            com.example.funlife.social.SocialSessionManager.warmStartAsync(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 通知冷启动 App：把 deepLink 暂存到 Bus，待 NavHost ready 后消费
        intent?.getStringExtra(
            com.example.funlife.notifications.NotificationCenter.EXTRA_DEEP_LINK
        )?.let { com.example.funlife.notifications.DeepLinkBus.publish(it) }

        // 🔥 启用 edge-to-edge：让背景延伸到状态栏与导航栏后面（沉浸式）
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        // 状态栏 / 导航栏图标色按内容亮暗自动反色
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true   // 浅色背景下用深色图标
            isAppearanceLightNavigationBars = true
        }
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        // 🎨 系统导航栏透明，App nav 直接延伸到屏幕底部覆盖手势区，
        //    系统 3 键/手势条浮于波浪之上
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        // 🔥 关掉系统对导航栏的"对比度蒙层"（默认 Q+ 会在透明 nav 下加半透明黑），
        //    这样手势区/3 键背后才能真正透出波浪图，不再有灰色阴影
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        // 初始化音效管理器
        soundManager = SoundEffectManager.getInstance(this)
        
        // 🔔 Android 13+ 请求通知权限（不然 heads-up 不弹）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
        
        // 🎀 引导用户授予悬浮窗权限（用于纪念日提醒跨应用显示）
        // 仅在首次未授予时引导，避免每次启动都跳设置
        if (!com.example.funlife.utils.OverlayBannerService.hasOverlayPermission(this)) {
            val prefs = getSharedPreferences("app_perm_prefs", MODE_PRIVATE)
            if (!prefs.getBoolean("overlay_asked", false)) {
                prefs.edit().putBoolean("overlay_asked", true).apply()
                // 延迟到 onResume 后弹出，避免影响启动
                window.decorView.post {
                    com.example.funlife.utils.OverlayBannerService.requestOverlayPermission(this)
                }
            }
        }

        // 🔋 引导用户允许"忽略电池优化"（系统级弹框，用户点"允许"即可）
        if (!com.example.funlife.utils.BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)) {
            val prefs = getSharedPreferences("app_perm_prefs", MODE_PRIVATE)
            if (!prefs.getBoolean("battery_opt_asked", false)) {
                prefs.edit().putBoolean("battery_opt_asked", true).apply()
                // 延迟 1 秒弹出，避免与悬浮窗对话框冲突
                window.decorView.postDelayed({
                    try {
                        com.example.funlife.utils.BatteryOptimizationHelper
                            .requestIgnoreBatteryOptimizations(this)
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "请求忽略电池优化失败", e)
                    }
                }, 1500)
            }
        }

        // 📢 引导用户允许"全屏通知"权限（Android 14+ 必需，不然通知不会强制弹出）
        if (android.os.Build.VERSION.SDK_INT >= 34) {  // Android 14+
            try {
                val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                if (!nm.canUseFullScreenIntent()) {
                    val prefs = getSharedPreferences("app_perm_prefs", MODE_PRIVATE)
                    if (!prefs.getBoolean("fullscreen_intent_asked", false)) {
                        prefs.edit().putBoolean("fullscreen_intent_asked", true).apply()
                        window.decorView.postDelayed({
                            try {
                                val intent = android.content.Intent(
                                    android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT
                                ).apply {
                                    data = android.net.Uri.parse("package:$packageName")
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                startActivity(intent)
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "请求全屏通知权限失败", e)
                            }
                        }, 4500)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "检查全屏通知权限失败", e)
            }
        }

        // ⏰ 引导用户允许"精确闹钟"权限（Android 12+ 必需）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val am = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            if (!am.canScheduleExactAlarms()) {
                val prefs = getSharedPreferences("app_perm_prefs", MODE_PRIVATE)
                if (!prefs.getBoolean("exact_alarm_asked", false)) {
                    prefs.edit().putBoolean("exact_alarm_asked", true).apply()
                    window.decorView.postDelayed({
                        try {
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                            ).apply {
                                data = android.net.Uri.parse("package:$packageName")
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "请求精确闹钟权限失败", e)
                        }
                    }, 3000)
                }
            }
        }
        
        // 🔥 初始化应用数据（头像框等）
        initializeAppData()
        
        // 🔔 启动时调度所有纪念日提醒（精确闹钟，到点自动触发）
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                kotlinx.coroutines.delay(2000)  // 等待数据库初始化
                val sessionManager = com.example.funlife.utils.UserSessionManager(this@MainActivity)
                val userId = sessionManager.getCurrentUserId()
                if (userId > 0L) {
                    com.example.funlife.utils.AnniversaryReminderScheduler.scheduleAllForUser(
                        this@MainActivity, userId
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "调度纪念日提醒失败", e)
            }
        }

        // ⚡ 启动微优化：以下都是非阻塞用户首帧的初始化，统一延后到首帧后执行
        // 避免给 onCreate 加同步开销，让 setContent 尽快完成。
        window.decorView.post {
            // 🔔 WorkManager 周期任务（每 6 小时后台检查纪念日）
            try {
                com.example.funlife.utils.AnniversaryReminderWorker.schedulePeriodic(this)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "注册 Worker 失败", e)
            }
            // 🔔 通知中心：创建渠道 + 调度每日推送
            try {
                com.example.funlife.notifications.NotificationBootstrap.init(this)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "通知中心初始化失败", e)
            }
            // 🌅 每日首次打开 App 推送今日摘要
            try {
                com.example.funlife.notifications.OpenAppNotifier.trigger(this)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "OpenAppNotifier 失败", e)
            }
            // 🌱 拉活通知 + 后台 Worker
            try {
                com.example.funlife.notifications.EngagementNotifier.triggerAsync(this)
                com.example.funlife.notifications.EngagementWorker.schedulePeriodic(this)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "EngagementNotifier 失败", e)
            }

            // 🛡️ VIP 凭证每 7 天联网复验（破解防御核心）
            try {
                com.example.funlife.vip.VipReverifyWorker.schedulePeriodic(this)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "VipReverifyWorker 注册失败", e)
            }

            // � 定期账单后台扫描（每 6 小时）→ 房租 / 订阅自动入账
            try {
                com.example.funlife.utils.RecurringBillWorker.schedulePeriodic(this)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "RecurringBillWorker 注册失败", e)
            }

            // ✉️ 时光信箱投递 Worker（每 1 小时扫描 due letters → LLM 生成回信 / 推送已到的回信）
            try {
                com.example.funlife.utils.LetterDeliveryWorker.schedulePeriodic(this)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "LetterDeliveryWorker 注册失败", e)
            }

            // 🌅 v53 阅光书房 · 晨光信使（每天 7:30 ± 5 分钟推一张晨光卡）
            try {
                com.example.funlife.utils.MorningHeraldWorker.schedulePeriodic(this)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "MorningHeraldWorker 注册失败", e)
            }

            // �🛡️ VIP 凭证启动验签：本地凭证被改 / 设备指纹变 / 凭证过期 → 自动降级
            // 同时每 7 天联网复验一次（凭证续期）
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val sm = com.example.funlife.utils.UserSessionManager(this@MainActivity)
                    val uid = sm.getCurrentUserId()
                    if (uid > 0L) {
                        val mgr = com.example.funlife.vip.VipManager(this@MainActivity)
                        val r = mgr.validateOnStartup(uid)
                        android.util.Log.d("MainActivity", "VIP 凭证验签: $r")
                        // 如果凭证 EXPIRED（自身 1 年到期），尝试联网续期
                        if (r == com.example.funlife.vip.VipCertificateValidator.Result.EXPIRED) {
                            mgr.reverify(uid)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "VIP 启动验签失败", e)
                }
            }
        }
        
        // 切换到正常主题
        setTheme(R.style.Theme_FunLife)
        
        setContent {
            FunLifeTheme {
                // 🔥 全局响应式适配器：所有页面/动画自动按屏幕尺寸缩放
                val screenAdapter = com.example.funlife.ui.utils.rememberScreenAdapter()
                androidx.compose.runtime.CompositionLocalProvider(
                    com.example.funlife.ui.utils.LocalScreenAdapter provides screenAdapter
                ) {
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
    val userSession = authViewModel.getCurrentSession()
    val suppressGameInviteRoomId = navBackStackEntry?.arguments?.getString("roomId")
        ?.takeIf { currentDestination?.route?.startsWith("social_game_lobby") == true }

    // ─── Deep Link：通知点击 → 自动跳到对应页面 ───
    // 仅在已登录（非欢迎/登录/注册）状态消费，避免打断认证流程
    val pendingDeepLink by com.example.funlife.notifications.DeepLinkBus.pending.collectAsState()
    LaunchedEffect(pendingDeepLink, currentDestination?.route) {
        val target = pendingDeepLink ?: return@LaunchedEffect
        val current = currentDestination?.route ?: return@LaunchedEffect
    val authPages = setOf(
            Screen.Welcome.route, Screen.Login.route, Screen.Register.route
        )
        if (current in authPages) return@LaunchedEffect
        if (current == target) {
            com.example.funlife.notifications.DeepLinkBus.consume()
            return@LaunchedEffect
        }
        try {
            navController.navigate(target) { launchSingleTop = true }
            com.example.funlife.notifications.DeepLinkBus.consume()
        } catch (_: Throwable) {
            // 路由不合法直接清空，避免反复尝试
            com.example.funlife.notifications.DeepLinkBus.consume()
        }
    }
    
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
    
    // 🪟 侧边面板状态（左缘右滑唤出）
    val sidePanelState = com.example.funlife.ui.components.rememberSidePanelState()
    // 仅首页启用左侧滑抽屉（其他页面避免与内容滑动手势冲突）
    val sidePanelEnabled = currentDestination?.route == Screen.Home.route
    androidx.compose.runtime.LaunchedEffect(sidePanelEnabled) {
        sidePanelState.enabled = sidePanelEnabled
        if (!sidePanelEnabled && sidePanelState.isVisible) sidePanelState.close()
    }

    // ⬇ 顶部下拉抽屉状态（屏顶下滑唤出 · 多模式可切换）
    // 仅在「首页 + 侧滑抽屉未打开」时启用，避免与右滑/其他页面冲突
    val topDrawerState = com.example.funlife.ui.components.topdrawer.rememberTopDrawerState()
    val isHomeRoute = currentDestination?.route == Screen.Home.route
    val topDrawerEnabled = sidePanelEnabled && isHomeRoute && !sidePanelState.isVisible
    androidx.compose.runtime.LaunchedEffect(topDrawerEnabled) {
        topDrawerState.enabled = topDrawerEnabled
        if (!topDrawerEnabled && topDrawerState.isVisible) topDrawerState.close()
    }
    val topDrawerCtx = androidx.compose.ui.platform.LocalContext.current
    val currentUserIdForTopDrawer = remember {
        com.example.funlife.utils.UserSessionManager(topDrawerCtx).getCurrentUserId()
    }

    // ─── 返回键拦截：抽屉打开时优先关闭，而非退出 App ───
    val backScope = androidx.compose.runtime.rememberCoroutineScope()
    androidx.activity.compose.BackHandler(enabled = topDrawerState.isVisible) {
        backScope.launch { topDrawerState.close() }
    }
    androidx.activity.compose.BackHandler(enabled = sidePanelState.isVisible && !topDrawerState.isVisible) {
        backScope.launch { sidePanelState.close() }
    }

    // 判断是否显示底部导航栏（登录/注册/欢迎页/宠物页/游戏计分页/商城页/背包页/转盘页/纪念日页/目标页/VIP页/头像框商城不显示）
    val hideBottomBarRoutes = listOf(
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
        Screen.AvatarFrameShop.route,
        Screen.ChatBill.route,
        Screen.BillDetail.route,
        Screen.BudgetManager.route,
        Screen.AccountManager.route,
        Screen.Settings.route,
        Screen.Help.route,
        Screen.Notifications.route,
        Screen.Inbox.route,
        // 🆕 v51 时光信箱 4 个路由全部隐藏底栏（沉浸式信纸阅读体验）
        Screen.LetterMailbox.route,
        Screen.LetterCompose.route,
        Screen.LetterDetail.route,
        Screen.LetterRecipients.route,
        // 🆕 v52 人生书架沉浸式书页体验
        Screen.Bookshelf.route,
        // 🆕 v53 阅光书房 · 子页全部走沉浸式
        Screen.ReadingRoom.route,
        Screen.BookDetail.route,
        Screen.BookChat.route,
        Screen.QuoteGalaxy.route,
        Screen.ReaderDna.route,
        Screen.PostcardDrift.route,
        // 🆕 v55 古籍日记本（中心页 + 全屏翻页都沉浸）
        Screen.DiaryBook.route,
        Screen.DiaryBookFull.route,
        Screen.DiaryBookFull.route + "?openEditor={openEditor}",
        "riddle_game",
        "dice_game",
        // 好友页沉浸式（Phase 1 Beta）
        Screen.Friends.route,
        // Phase 2 私聊详情：全屏沉浸，隐藏主 Tab 底栏
        Screen.FriendChat.route,
        // 趣玩中心 · 全链路沉浸（无底部导航栏）
        Screen.SocialGameCenter.route,
        "social_game_center?tab={tab}&peerPbId={peerPbId}",
        Screen.SocialGameLobby.route,
    )
    val immersiveRoute = currentDestination?.route
    val showBottomBar = immersiveRoute !in hideBottomBarRoutes &&
        immersiveRoute?.startsWith("friend_chat/") != true &&
        immersiveRoute?.startsWith("social_game_detail/") != true &&
        immersiveRoute?.startsWith("social_game_lobby/") != true &&
        immersiveRoute?.startsWith("social_game_center") != true

    com.example.funlife.ui.components.topdrawer.TopDrawerHost(
        state = topDrawerState,
        userId = currentUserIdForTopDrawer
    ) {
    com.example.funlife.ui.components.SidePanelDrawer(
        state = sidePanelState,
        onNavigate = { route ->
            // 容错：未知路由忽略
            try { navController.navigate(route) } catch (_: Throwable) {}
        }
    ) {
    Scaffold(
        containerColor = Color.Transparent,
        // 🔥 不让 Scaffold 自动消费 systemBars，让背景能延伸到状态栏 / 导航栏
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                // 🪟 包裹式手势检测器：父级在 Final pass 监听右滑，
                //    子元素（LazyColumn、滚动卡片）在 Main pass 优先消费事件
                if (sidePanelEnabled) {
                    com.example.funlife.ui.components.SidePanelEdgeDetector(state = sidePanelState) {
                        NavGraph(
                            navController = navController,
                            modifier = Modifier.fillMaxSize(),
                            authViewModel = authViewModel
                        )
                    }
                } else {
                    NavGraph(
                        navController = navController,
                        modifier = Modifier.fillMaxSize(),
                        authViewModel = authViewModel
                    )
                }
            }
            
            // 🎀 全局纪念日提醒悬浮条 - 覆盖在所有页面顶部
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            ) {
                com.example.funlife.ui.components.InAppReminderBanner(
                    onClick = {
                        com.example.funlife.utils.AnniversaryReminderManager.dismissInAppBanner()
                        com.example.funlife.utils.AnniversaryReminderManager.stopAlarm()
                        navController.navigate("anniversary")
                    }
                )
            }

            // 👥 好友申请前台横幅（OEM 拦截系统 heads-up 时的兜底）
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            ) {
                com.example.funlife.ui.components.SocialHeadsUpBanner()
            }

            // 🎮 趣玩邀请：全 App 弹层（首页/任意 Tab 均可接受或拒绝）
            if (userSession != null && com.example.funlife.social.PocketBaseConfig.isEnabled()) {
                val gameCenterVm = com.example.funlife.ui.screens.socialgame.rememberGameCenterViewModel(userSession)
                com.example.funlife.ui.screens.socialgame.GlobalGameInviteLayer(
                    viewModel = gameCenterVm,
                    navController = navController,
                    suppressRoomId = suppressGameInviteRoomId,
                )
            }

            // 底部导航栏 - 波浪延伸到屏幕真正底部，覆盖系统手势/3键区
            if (showBottomBar) {
                val sysNavBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(90.dp + sysNavBottom)
                ) {
                    // 波浪背景图——填满整个 Box（含手势区），系统 3 键会浮在其上
                    Image(
                        painter = painterResource(id = R.drawable.nav_bg),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )

                    // 导航项 - 仅占顶部 90dp 安全区，不伸入手势区
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .align(Alignment.TopCenter)
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
                                            saveState = true
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
    } // SidePanelDrawer 闭合
    } // TopDrawerHost 闭合
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
