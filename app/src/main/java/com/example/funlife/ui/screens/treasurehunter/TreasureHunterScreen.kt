package com.example.funlife.ui.screens.treasurehunter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.game.platformer.*
import com.example.funlife.game.treasurehunter.TreasureHunterLevels
import com.example.funlife.ui.screens.pacmaze.components.LockLandscape
import com.example.funlife.ui.screens.pacmaze.components.PacMazeHideSystemBars
import com.example.funlife.ui.screens.platformer.GamePlayArea
import com.example.funlife.ui.screens.platformer.PlatformerLoadingScreen
import com.example.funlife.ui.screens.platformer.rememberPlatformerLoadProgress
import com.example.funlife.ui.screens.platformer.themeAccent
import kotlinx.coroutines.delay

@Composable
fun TreasureHunterScreen(onNavigateBack: () -> Unit) {
    LockLandscape()
    PacMazeHideSystemBars()

    val context = LocalContext.current
    val assets = remember { runCatching { PlatformerAssets.load(context) }.getOrNull() }
    var ready by remember { mutableStateOf(true) }
    var loadMsg by remember { mutableStateOf("加载宝藏猎人…") }

    LaunchedEffect(Unit) {
        runCatching { PlatformerCharacterRenderer.warmup(PlatformerCharacterId.TREASURE_HUNTER) }
        ready = true
    }

    var screen by remember { mutableStateOf(ThUi.LevelSelect) }
    var levelIndex by remember { mutableIntStateOf(0) }
    var playSession by remember { mutableIntStateOf(0) }
    var maxUnlocked by remember { mutableIntStateOf(1) }

    Box(Modifier.fillMaxSize()) {
        when (screen) {
            ThUi.LevelSelect -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color(0xFF1A237E), Color(0xFF311B92), Color(0xFF4A148C))))
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                        }
                        Column(Modifier.padding(start = 8.dp)) {
                            Text("宝藏猎人", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("独立地牢模式 · 5 关卡", color = Color.White.copy(0.8f), fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(TreasureHunterLevels.all) { level ->
                            val unlocked = level.id <= maxUnlocked
                            Card(
                                onClick = { if (unlocked) { levelIndex = level.id - 1; playSession++; screen = ThUi.Playing } },
                                enabled = unlocked,
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${level.id}", fontSize = 26.sp, fontWeight = FontWeight.Black)
                                        if (!unlocked) Icon(Icons.Default.Lock, null, Modifier.size(16.dp))
                                    }
                                    Text(level.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(level.subtitle, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
            ThUi.Playing -> {
                val (msg, progress) = rememberPlatformerLoadProgress(loadMsg, 100)
                if (assets == null || !ready) {
                    PlatformerLoadingScreen(message = msg, progress = progress, onBack = { screen = ThUi.LevelSelect })
                } else {
                    key(playSession, levelIndex) {
                        val world = remember(playSession, levelIndex) {
                            PlatformerLevels.buildWorld(
                                context,
                                TreasureHunterLevels.all[levelIndex],
                                PlatformerCharacterId.TREASURE_HUNTER,
                            )
                        }
                        GamePlayArea(
                            world = world,
                            assets = assets,
                            context = context,
                            onExit = { screen = ThUi.LevelSelect },
                            onRetry = { playSession++ },
                            onNextLevel = {
                                if (levelIndex < TreasureHunterLevels.all.lastIndex) {
                                    maxUnlocked = maxOf(maxUnlocked, TreasureHunterLevels.all[levelIndex].id + 1)
                                    levelIndex++
                                    playSession++
                                } else {
                                    screen = ThUi.LevelSelect
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

private enum class ThUi { LevelSelect, Playing }
