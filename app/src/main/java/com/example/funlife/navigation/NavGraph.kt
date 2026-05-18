// NavGraph.kt - 导航图
package com.example.funlife.navigation

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
    object Profile : Screen("profile", "我的")
    object Inventory : Screen("inventory", "背包")
    object Vip : Screen("vip", "VIP会员")
    object VipProfile : Screen("vip_profile", "VIP个人主页")
    object AvatarFrameShop : Screen("avatar_frame_shop", "头像框商城")
    object ChatBill : Screen("chat_bill", "聊天记账")
    object BillDetail : Screen("bill_detail", "记账详情")
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
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("shop") {
            ShopScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) }
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
                        coinRepository = CoinRepository(application.database.coinDao()),
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
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
        
        composable(Screen.Profile.route) {
            ProfileScreen(
                authViewModel = authViewModel,
                onLogout = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToInventory = {
                    navController.navigate(Screen.Inventory.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        // 背包页面
        composable(Screen.Inventory.route) {
            val context = LocalContext.current
            val database = (context.applicationContext as FunLifeApplication).database
            val inventoryRepository = remember { 
                com.example.funlife.repository.InventoryRepository(database.inventoryDao()) 
            }
            val userPreferencesDao = remember { database.userPreferencesDao() }
            val userVipDao = remember { database.userVipDao() } // 🔥 新增VIP DAO
            val userAvatarDao = remember { database.userAvatarDao() } // 🔥 新增UserAvatar DAO
            val inventoryViewModel: com.example.funlife.viewmodel.InventoryViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return com.example.funlife.viewmodel.InventoryViewModel(
                            inventoryRepository,
                            userPreferencesDao,
                            userVipDao, // 🔥 传递VIP DAO
                            userAvatarDao // 🔥 传递UserAvatar DAO
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
            VipProfileScreen(
                authViewModel = authViewModel,
                scoreViewModel = scoreViewModel,
                onLogout = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
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
                val chatViewModel = remember {
                    ChatViewModel(application, userSession.userId)
                }
                ChatBillScreen(
                    viewModel = chatViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToBillDetail = { navController.navigate(Screen.BillDetail.route) },
                    avatarUri = userAvatar?.avatarUri
                )
            } else {
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
                val chatViewModel = remember {
                    ChatViewModel(application, userSession.userId)
                }
                BillDetailScreen(
                    viewModel = chatViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
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
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }
}
