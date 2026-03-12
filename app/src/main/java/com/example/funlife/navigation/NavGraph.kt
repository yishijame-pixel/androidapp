// NavGraph.kt - 导航图
package com.example.funlife.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.funlife.ui.screens.*
import com.example.funlife.viewmodel.AnniversaryViewModel
import com.example.funlife.viewmodel.AuthViewModel
import com.example.funlife.viewmodel.ScoreViewModel

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
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    authViewModel: AuthViewModel,
    anniversaryViewModel: AnniversaryViewModel = viewModel(),
    scoreViewModel: ScoreViewModel = viewModel()
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
                authViewModel = authViewModel
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
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
