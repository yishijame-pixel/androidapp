// NavGraph.kt - 导航图
package com.example.funlife.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.funlife.FunLifeApplication
import com.example.funlife.repository.CoinRepository
import com.example.funlife.repository.PetRepository
import com.example.funlife.repository.PetItemRepository
import com.example.funlife.ui.screens.*
import com.example.funlife.viewmodel.AnniversaryViewModel
import com.example.funlife.viewmodel.AuthViewModel
import com.example.funlife.viewmodel.ScoreViewModel
import com.example.funlife.viewmodel.GoalViewModel
import com.example.funlife.viewmodel.PetViewModel
import com.example.funlife.viewmodel.ChatViewModel

/**
 * 🛡️ 安全导航：当目标路由不存在 / 无法解析时，吃掉异常并提示用户，
 * 避免应用崩溃或停留在空白页。所有调用 navigate(route) 的地方都应使用此扩展。
 */
fun NavHostController.safeNavigate(
    route: String,
    context: android.content.Context? = null,
    builder: (androidx.navigation.NavOptionsBuilder.() -> Unit)? = null
) {
    try {
        if (builder != null) this.navigate(route, builder) else this.navigate(route)
    } catch (e: IllegalArgumentException) {
        android.util.Log.e("NavGraph", "🚧 未知路由: $route", e)
        context?.let {
            android.widget.Toast.makeText(it, "页面暂未开放 🚧", android.widget.Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        android.util.Log.e("NavGraph", "导航失败: $route", e)
        context?.let {
            android.widget.Toast.makeText(it, "跳转失败，请稍后再试", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * � 完全重启 MainActivity（清空 task），用于"退出登录 / 切换账号"。
 *
 * 为什么必须重启而不是 Activity.recreate()：
 *   1. recreate() 会保存 savedInstanceState，Compose Navigation 自动恢复回退栈，
 *      用户退出后还会停在 Profile/Home 页，回不到 Welcome
 *   2. 重启可清空所有 Activity-scoped ViewModel，避免新账号看到旧账号数据
 *   3. CLEAR_TASK 确保整个任务栈被清空，等同冷启动到 startDestination = Welcome
 *
 * 调用方应已先 authViewModel.logout() 清掉 session，再调本函数。
 */
fun restartAppForLogout(context: android.content.Context) {
    val act = context as? android.app.Activity ?: return
    val intent = android.content.Intent(act, com.example.funlife.MainActivity::class.java).apply {
        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    act.startActivity(intent)
    act.finish()
    act.overridePendingTransition(0, 0)
}

/**
 * � 加载占位：用于路由分支判定中（如 userSession == null 跳登录的等待期），
 * 显示一个友好的加载界面而不是空白。
 */
@Composable
private fun LoadingFallback(message: String = "加载中…") {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xFFFFF8E1)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                color = androidx.compose.ui.graphics.Color(0xFFFFB74D)
            )
            Text(message, fontSize = 14.sp, color = androidx.compose.ui.graphics.Color(0xFF8D6E63))
        }
    }
}

sealed class Screen(val route: String, val title: String) {
    object Welcome : Screen("welcome", "欢迎")
    object Login : Screen("login", "登录")
    object Register : Screen("register", "注册")
    object Home : Screen("home", "首页")
    object Habit : Screen("habit", "打卡")
    object Mood : Screen("mood", "心情")
    object Goal : Screen("goal", "目标")
    object Anniversary : Screen("anniversary", "纪念日")
    object SpinWheel : Screen("spin_wheel", "幸运转盘")
    object ScoreCounter : Screen("score_counter", "游戏计分")
    object Statistics : Screen("statistics", "统计")
    object History : Screen("history", "历史")
    object Settings : Screen("settings", "设置")
    object Help : Screen("help", "帮助中心")
    object Profile : Screen("profile", "我的")
    object Inventory : Screen("inventory", "背包")
    object Vip : Screen("vip", "VIP会员")
    object VipProfile : Screen("vip_profile", "VIP个人主页")
    object AvatarFrameShop : Screen("avatar_frame_shop", "头像框商城")
    object ChatBill : Screen("chat_bill", "聊天记账")
    object BillDetail : Screen("bill_detail", "记账详情")
    object BudgetManager : Screen("budget_manager", "预算管理")
    object AccountManager : Screen("account_manager", "账户管理")
    object Notifications : Screen("notifications", "通知中心")
    object Inbox : Screen("inbox", "通知收件箱")
    // 🆕 v51 时光信箱
    object LetterMailbox : Screen("letter_mailbox", "时光信箱")
    object LetterCompose : Screen("letter_compose?recipientId={recipientId}", "写信") {
        fun routeWith(recipientId: Long?) =
            if (recipientId == null) "letter_compose" else "letter_compose?recipientId=$recipientId"
    }
    object LetterDetail : Screen("letter_detail/{letterId}", "信件详情") {
        fun routeWith(letterId: Long) = "letter_detail/$letterId"
    }
    object LetterRecipients : Screen("letter_recipients", "收信人")
    // 🆕 v52 人生书架
    object Bookshelf : Screen("bookshelf", "人生书架")
    // 🆕 v53 阅光书房
    object ReadingRoom : Screen("reading_room", "阅光书房")
    object BookDetail : Screen("book_detail/{bookId}", "书籍详情") {
        fun routeWith(bookId: Long) = "book_detail/$bookId"
    }
    object BookChat : Screen("book_chat/{bookId}?sessionId={sessionId}", "AI 读书伴侣") {
        fun routeWith(bookId: Long, sessionId: Long = 0L) =
            if (sessionId > 0L) "book_chat/$bookId?sessionId=$sessionId"
            else "book_chat/$bookId?sessionId=0"
    }
    object QuoteGalaxy : Screen("quote_galaxy", "摘抄星河")
    object ReaderDna : Screen("reader_dna", "读者 DNA")
    object PostcardDrift : Screen("postcard_drift", "明信片漂流")
    object Friends : Screen("friends", "好友")
    object FriendChat : Screen("friend_chat/{peerPbId}", "私聊") {
        fun routeWith(peerPbId: String) = "friend_chat/$peerPbId"
    }
    // 🆕 v55 古籍日记本
    object DiaryBook : Screen("diary_book", "日记本")
    object DiaryBookFull : Screen("diary_book_full", "日记本")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    authViewModel: AuthViewModel,
    anniversaryViewModel: AnniversaryViewModel = viewModel(),
    scoreViewModel: ScoreViewModel = viewModel(),
    goalViewModel: GoalViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route,
        modifier = modifier
    ) {
        // 欢迎页
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }
        
        // 登录页
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        // 注册页
        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                anniversaryViewModel = anniversaryViewModel,
                scoreViewModel = scoreViewModel,
                authViewModel = authViewModel,
                goalViewModel = goalViewModel
            )
        }
        
        composable(Screen.Habit.route) {
            HabitScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Mood.route) {
            MoodScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Goal.route) {
            GoalScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Anniversary.route) {
            AnniversaryScreen(
                viewModel = anniversaryViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.SpinWheel.route) {
            EnhancedSpinWheelScreen(
                viewModel = viewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.ScoreCounter.route) {
            ScoreCounterScreen(
                viewModel = scoreViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Statistics.route) {
            StatisticsScreen(
                viewModel = viewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.History.route) {
            HistoryScreen(
                viewModel = viewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Help.route) {
            HelpScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Notifications.route) {
            com.example.funlife.ui.screens.NotificationSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Inbox.route) {
            com.example.funlife.ui.screens.InboxScreen(
                onNavigateBack = { navController.popBackStack() },
                onDeepLink = { route ->
                    val target = when (route) {
                        "home" -> Screen.Home.route
                        "goal" -> Screen.Goal.route
                        "habit" -> Screen.Habit.route
                        "mood" -> Screen.Mood.route
                        "anniversary" -> Screen.Anniversary.route
                        else -> route
                    }
                    navController.navigate(target)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel(),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHelp = { navController.navigate(Screen.Help.route) },
                onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) }
            )
        }
        
        composable("shop") {
            val ctx = LocalContext.current
            ShopScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigate = { route -> navController.safeNavigate(route, ctx) }
            )
        }
        
        composable("pet") {
            val context = LocalContext.current
            val application = context.applicationContext as FunLifeApplication
            val userSession = authViewModel.getCurrentSession()
            
            if (userSession != null) {
                val petViewModel = remember {
                    PetViewModel(
                        petRepository = PetRepository(application.database.petDao()),
                        petItemRepository = PetItemRepository(application.database.petItemDao()),
                        coinRepository = CoinRepository(application.database.coinDao(), context.applicationContext),
                        userId = userSession.userId,
                        appContext = context.applicationContext
                    )
                }
                PetScreen(
                    navController = navController,
                    viewModel = petViewModel
                )
            }
        }
        
        composable("dice_game") {
            com.example.funlife.ui.screens.DiceGameScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("riddle_game") {
            val context = LocalContext.current
            val application = context.applicationContext as FunLifeApplication
            val userSession = authViewModel.getCurrentSession()
            
            if (userSession != null) {
                RiddleGameScreen(
                    userId = userSession.userId,
                    database = application.database,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                // 如果没有登录，返回登录页
                LoadingFallback("正在跳转登录…")
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
        
        composable(Screen.Profile.route) {
            val ctx = LocalContext.current
            ProfileScreen(
                authViewModel = authViewModel,
                onLogout = { restartAppForLogout(ctx) },
                onNavigateToInventory = {
                    navController.navigate(Screen.Inventory.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToInbox = {
                    navController.navigate(Screen.Inbox.route)
                },
                onNavigateToFriends = {
                    navController.navigate(Screen.Friends.route)
                },
            )
        }

        composable(Screen.Friends.route) {
            val context = LocalContext.current
            val userSession = authViewModel.getCurrentSession()
            if (userSession == null) {
                LoadingFallback("正在跳转登录…")
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                return@composable
            }
            val friendsViewModel: com.example.funlife.viewmodel.FriendsViewModel = viewModel(
                key = "friends_${userSession.userId}",
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return com.example.funlife.viewmodel.FriendsViewModel(
                            application = context.applicationContext as android.app.Application,
                            currentUserId = userSession.userId,
                            myFunlifeUsername = userSession.username,
                            displayName = userSession.nickname.ifBlank { userSession.username },
                        ) as T
                    }
                },
            )
            FriendsScreen(
                viewModel = friendsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { peerPbId ->
                    navController.safeNavigate(Screen.FriendChat.routeWith(peerPbId), context)
                },
            )
        }

        composable(
            route = Screen.FriendChat.route,
            arguments = listOf(navArgument("peerPbId") { type = NavType.StringType }),
        ) {
            val context = LocalContext.current
            val userSession = authViewModel.getCurrentSession()
            val peerPbId = it.arguments?.getString("peerPbId").orEmpty()
            if (peerPbId.isBlank()) {
                LoadingFallback("无效的会话")
                LaunchedEffect(Unit) { navController.popBackStack() }
                return@composable
            }
            if (userSession == null) {
                LoadingFallback("正在跳转登录…")
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                return@composable
            }
            val chatViewModel: com.example.funlife.viewmodel.FriendChatViewModel = viewModel(
                key = "friend_chat_${userSession.userId}_$peerPbId",
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return com.example.funlife.viewmodel.FriendChatViewModel(
                            application = context.applicationContext as android.app.Application,
                            currentUserId = userSession.userId,
                            peerPbId = peerPbId,
                        ) as T
                    }
                },
            )
            FriendChatScreen(
                viewModel = chatViewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        
        // 背包页面
        composable(Screen.Inventory.route) {
            val context = LocalContext.current
            val database = (context.applicationContext as FunLifeApplication).database
            val userSession = authViewModel.getCurrentSession()

            if (userSession == null) {
                // 🔒 未登录直接跳到登录页，避免使用默认 userId 加载错乱数据
                LoadingFallback("正在跳转登录…")
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                return@composable
            }

            val inventoryRepository = remember {
                com.example.funlife.repository.InventoryRepository(database.inventoryDao())
            }
            val userPreferencesDao = remember { database.userPreferencesDao() }
            val userVipDao = remember { database.userVipDao() }
            val userAvatarDao = remember { database.userAvatarDao() }
            val shopDao = remember { database.shopDao() }
            // � ViewModel 实例按 userId 区分，登录不同账号会得到不同实例，
            // 防止登出再登入复用旧用户数据。
            val inventoryViewModel: com.example.funlife.viewmodel.InventoryViewModel = viewModel(
                key = "inventory_${userSession.userId}",
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return com.example.funlife.viewmodel.InventoryViewModel(
                            inventoryRepository,
                            userPreferencesDao,
                            userVipDao,
                            userAvatarDao,
                            shopDao,
                            currentUserId = userSession.userId
                        ) as T
                    }
                }
            )

            InventoryScreen(
                viewModel = inventoryViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // VIP会员页面
        composable(Screen.Vip.route) {
            VipScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
        
        // VIP个人主页
        composable(Screen.VipProfile.route) {
            val ctx = LocalContext.current
            VipProfileScreen(
                authViewModel = authViewModel,
                scoreViewModel = scoreViewModel,
                onLogout = { restartAppForLogout(ctx) }
            )
        }
        
        // 🔥 聊天记账
        composable(Screen.ChatBill.route) {
            val context = LocalContext.current
            val application = context.applicationContext as FunLifeApplication
            val userSession = authViewModel.getCurrentSession()
            
            if (userSession != null) {
                val database = application.database
                val userAvatarDao = remember { database.userAvatarDao() }
                val userAvatar by userAvatarDao.getUserAvatar(userSession.userId)
                    .collectAsState(initial = null)
                // 🔒 安全修复：remember key 加上 userId，登出再登入不同账号会得到全新 ViewModel 实例
                val chatViewModel = remember(userSession.userId) {
                    ChatViewModel(application, userSession.userId)
                }
                ChatBillScreen(
                    viewModel = chatViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToBillDetail = { navController.navigate(Screen.BillDetail.route) },
                    onNavigateToBudgetManager = { navController.navigate(Screen.BudgetManager.route) },
                    onNavigateToAccountManager = { navController.navigate(Screen.AccountManager.route) },
                    avatarUri = userAvatar?.avatarUri
                )
            } else {
                LoadingFallback("正在跳转登录…")
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }

        // 🔥 记账详情
        composable(Screen.BillDetail.route) {
            val context = LocalContext.current
            val application = context.applicationContext as FunLifeApplication
            val userSession = authViewModel.getCurrentSession()

            if (userSession != null) {
                // 🔒 同上：remember 按 userId 区分
                val chatViewModel = remember(userSession.userId) {
                    ChatViewModel(application, userSession.userId)
                }
                BillDetailScreen(
                    viewModel = chatViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                LoadingFallback("正在跳转登录…")
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }

        // 🆕 v48 账户管理
        composable(Screen.AccountManager.route) {
            val context = LocalContext.current
            val application = context.applicationContext as FunLifeApplication
            val userSession = authViewModel.getCurrentSession()
            if (userSession != null) {
                val chatViewModel = remember(userSession.userId) {
                    ChatViewModel(application, userSession.userId)
                }
                AccountManagerScreen(
                    viewModel = chatViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                LoadingFallback("正在跳转登录…")
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }

        // 🆕 v49 预算管理
        composable(Screen.BudgetManager.route) {
            val context = LocalContext.current
            val application = context.applicationContext as FunLifeApplication
            val userSession = authViewModel.getCurrentSession()
            if (userSession != null) {
                val chatViewModel = remember(userSession.userId) {
                    ChatViewModel(application, userSession.userId)
                }
                BudgetManagerScreen(
                    viewModel = chatViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                LoadingFallback("正在跳转登录…")
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }

        // 🔥 头像框商城
        composable(Screen.AvatarFrameShop.route) {
            val context = LocalContext.current
            val application = context.applicationContext as FunLifeApplication
            val userSession = authViewModel.getCurrentSession()
            
            if (userSession != null) {
                val shopViewModel: com.example.funlife.viewmodel.ShopViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return com.example.funlife.viewmodel.ShopViewModel(application) as T
                        }
                    }
                )
                
                com.example.funlife.ui.screens.AvatarFrameShopScreen(
                    viewModel = shopViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                // 如果没有登录，返回登录页
                LoadingFallback("正在跳转登录…")
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════
        // 🆕 v51 时光信箱
        // ════════════════════════════════════════════════════════

        // 🆕 v52 人生书架
        // 🆕 v53 阅光书房 · 总入口（4-Tab）
        composable(Screen.ReadingRoom.route) {
            val s = authViewModel.getCurrentSession()
            if (s != null) {
                com.example.funlife.ui.screens.ReadingRoomScreen(
                    userId = s.userId,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenBookshelf = { navController.navigate(Screen.Bookshelf.route) },
                    onOpenGalaxy = { navController.navigate(Screen.QuoteGalaxy.route) },
                    onOpenDna = { navController.navigate(Screen.ReaderDna.route) },
                    onOpenPostcard = { navController.navigate(Screen.PostcardDrift.route) },
                    onOpenBookDetail = { id -> navController.navigate(Screen.BookDetail.routeWith(id)) },
                )
            }
        }
        // 🆕 v53 单本书详情
        composable(
            Screen.BookDetail.route,
            arguments = listOf(
                androidx.navigation.navArgument("bookId") {
                    type = androidx.navigation.NavType.LongType
                }
            )
        ) { backStack ->
            val s = authViewModel.getCurrentSession()
            val bid = backStack.arguments?.getLong("bookId") ?: 0L
            if (s != null && bid > 0L) {
                com.example.funlife.ui.screens.BookDetailScreen(
                    userId = s.userId,
                    bookId = bid,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenChat = { id, sid -> navController.navigate(Screen.BookChat.routeWith(id, sid)) },
                    onOpenSnapshot = { /* 由 Screen 内部用 SnapshotShareDialog 弹出 */ },
                )
            }
        }
        // 🆕 v53 AI 读书伴侣（v54 加 sessionId）
        composable(
            Screen.BookChat.route,
            arguments = listOf(
                androidx.navigation.navArgument("bookId") {
                    type = androidx.navigation.NavType.LongType
                },
                androidx.navigation.navArgument("sessionId") {
                    type = androidx.navigation.NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStack ->
            val s = authViewModel.getCurrentSession()
            val bid = backStack.arguments?.getLong("bookId") ?: 0L
            val sid = backStack.arguments?.getLong("sessionId") ?: 0L
            if (s != null && bid > 0L) {
                com.example.funlife.ui.screens.BookChatScreen(
                    userId = s.userId, bookId = bid, sessionId = sid,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        // 🆕 v53 摘抄星河
        composable(Screen.QuoteGalaxy.route) {
            val s = authViewModel.getCurrentSession()
            if (s != null) {
                com.example.funlife.ui.screens.QuoteGalaxyScreen(
                    userId = s.userId, onBack = { navController.popBackStack() }
                )
            }
        }
        // 🆕 v53 读者 DNA
        composable(Screen.ReaderDna.route) {
            val s = authViewModel.getCurrentSession()
            if (s != null) {
                com.example.funlife.ui.screens.ReaderDnaScreen(
                    userId = s.userId, onBack = { navController.popBackStack() }
                )
            }
        }
        // 🆕 v55 古籍日记本·中心页（3D 魔法书入口）
        composable(Screen.DiaryBook.route) {
            val s = authViewModel.getCurrentSession()
            if (s != null) {
                com.example.funlife.ui.screens.DiaryHubScreen(
                    userId = s.userId,
                    onBack = { navController.popBackStack() },
                    onOpenFullReader = {
                        navController.navigate(Screen.DiaryBookFull.route)
                    },
                    onOpenWriteToday = {
                        navController.navigate(Screen.DiaryBookFull.route + "?openEditor=1")
                    }
                )
            } else {
                LoadingFallback("正在跳转登录…")
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
        // v55 日记本·全屏阅读
        composable(
            route = Screen.DiaryBookFull.route + "?openEditor={openEditor}",
            arguments = listOf(
                androidx.navigation.navArgument("openEditor") {
                    type = androidx.navigation.NavType.StringType
                    defaultValue = "0"
                    nullable = true
                }
            )
        ) { backStack ->
            val s = authViewModel.getCurrentSession()
            if (s != null) {
                val openEditor = backStack.arguments?.getString("openEditor") == "1"
                com.example.funlife.ui.screens.DiaryBookScreen(
                    userId = s.userId,
                    onBack = { navController.popBackStack() },
                    openEditorOnLaunch = openEditor
                )
            } else {
                LoadingFallback("正在跳转登录…")
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
        // 同路由，不带参数仍可访问
        composable(Screen.DiaryBookFull.route) {
            val s = authViewModel.getCurrentSession()
            if (s != null) {
                com.example.funlife.ui.screens.DiaryBookScreen(
                    userId = s.userId,
                    onBack = { navController.popBackStack() }
                )
            } else {
                LoadingFallback("正在跳转登录…")
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
        // 🆕 v53 明信片漂流
        composable(Screen.PostcardDrift.route) {
            val s = authViewModel.getCurrentSession()
            if (s != null) {
                com.example.funlife.ui.screens.PostcardDriftScreen(
                    userId = s.userId, onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.Bookshelf.route) {
            val userSession = authViewModel.getCurrentSession()
            if (userSession != null) {
                com.example.funlife.ui.screens.BookshelfScreen(
                    userId = userSession.userId,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                LoadingFallback("正在跳转登录…")
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                }
            }
        }

        composable(Screen.LetterMailbox.route) {
            val context = LocalContext.current
            val application = context.applicationContext as FunLifeApplication
            val userSession = authViewModel.getCurrentSession()
            if (userSession != null) {
                val vm = remember(userSession.userId) {
                    com.example.funlife.viewmodel.LetterViewModel(application, userSession.userId)
                }
                com.example.funlife.ui.screens.LetterMailboxScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCompose = { rid ->
                        navController.navigate(Screen.LetterCompose.routeWith(rid))
                    },
                    onNavigateToDetail = { lid ->
                        navController.navigate(Screen.LetterDetail.routeWith(lid))
                    },
                    onManageRecipients = { navController.navigate(Screen.LetterRecipients.route) },
                    onNavigateToVip = { navController.navigate(Screen.Vip.route) }
                )
            } else {
                LoadingFallback("正在跳转登录…")
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                }
            }
        }

        composable(
            Screen.LetterCompose.route,
            arguments = listOf(
                androidx.navigation.navArgument("recipientId") {
                    type = androidx.navigation.NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStack ->
            val context = LocalContext.current
            val application = context.applicationContext as FunLifeApplication
            val userSession = authViewModel.getCurrentSession()
            if (userSession != null) {
                val vm = remember(userSession.userId) {
                    com.example.funlife.viewmodel.LetterViewModel(application, userSession.userId)
                }
                val rid = backStack.arguments?.getLong("recipientId", -1L)?.takeIf { it > 0L }
                com.example.funlife.ui.screens.LetterComposeScreen(
                    viewModel = vm,
                    initialRecipientId = rid,
                    onNavigateBack = { navController.popBackStack() },
                    onSent = { navController.popBackStack() },
                    onUpgrade = { navController.navigate(Screen.Vip.route) }
                )
            } else {
                LoadingFallback("正在跳转登录…")
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                }
            }
        }

        composable(
            Screen.LetterDetail.route,
            arguments = listOf(
                androidx.navigation.navArgument("letterId") { type = androidx.navigation.NavType.LongType }
            )
        ) { backStack ->
            val context = LocalContext.current
            val application = context.applicationContext as FunLifeApplication
            val userSession = authViewModel.getCurrentSession()
            if (userSession != null) {
                val vm = remember(userSession.userId) {
                    com.example.funlife.viewmodel.LetterViewModel(application, userSession.userId)
                }
                val lid = backStack.arguments?.getLong("letterId") ?: 0L
                com.example.funlife.ui.screens.LetterDetailScreen(
                    viewModel = vm,
                    letterId = lid,
                    onNavigateBack = { navController.popBackStack() },
                    onReply = { rid ->
                        navController.navigate(Screen.LetterCompose.routeWith(rid))
                    }
                )
            } else {
                LoadingFallback("正在跳转登录…")
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                }
            }
        }

        composable(Screen.LetterRecipients.route) {
            val context = LocalContext.current
            val application = context.applicationContext as FunLifeApplication
            val userSession = authViewModel.getCurrentSession()
            if (userSession != null) {
                val vm = remember(userSession.userId) {
                    com.example.funlife.viewmodel.LetterViewModel(application, userSession.userId)
                }
                com.example.funlife.ui.screens.LetterRecipientManagerScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                LoadingFallback("正在跳转登录…")
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                }
            }
        }
    }
}
