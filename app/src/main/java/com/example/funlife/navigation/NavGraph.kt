// NavGraph.kt - 导航图
package com.example.funlife.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.funlife.ui.screens.*
import com.example.funlife.viewmodel.AnniversaryViewModel
import com.example.funlife.viewmodel.ScoreViewModel

sealed class Screen(val route: String, val title: String) {
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
    anniversaryViewModel: AnniversaryViewModel = viewModel(),
    scoreViewModel: ScoreViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                anniversaryViewModel = anniversaryViewModel,
                scoreViewModel = scoreViewModel
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
