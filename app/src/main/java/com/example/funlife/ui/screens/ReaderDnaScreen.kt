// ReaderDnaScreen.kt — v53 阅光书房 · 读者 DNA
package com.example.funlife.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.funlife.repository.ReaderDnaRepository
import com.example.funlife.ui.components.ReaderDnaRadar
import com.example.funlife.ui.theme.ReadingRoomTheme as RT
import com.example.funlife.viewmodel.ReaderDnaViewModel
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderDnaScreen(
    userId: Long,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as android.app.Application
    val vm: ReaderDnaViewModel = viewModel(
        key = "Dna_$userId",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                ReaderDnaViewModel(app, userId) as T
        }
    )
    val cards by vm.history.collectAsState()
    val generating by vm.generating.collectAsState()
    val toast by vm.toast.collectAsState()
    val cooldown by vm.cooldownMs.collectAsState()
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(toast) { toast?.let { snack.showSnackbar(it); vm.consumeToast() } }

    var selectedIndex by remember { mutableStateOf(0) }
    val current = cards.getOrNull(selectedIndex)
    val gson = remember { Gson() }
    val dna = remember(current) {
        runCatching {
            current?.vectorJson?.let {
                gson.fromJson(it, ReaderDnaRepository.ParsedDna::class.java)
            }
        }.getOrNull()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        containerColor = Color.Transparent,
    ) { padding ->
        Box(Modifier.fillMaxSize().background(RT.pageBackground())) {
            Column(
                Modifier.fillMaxSize().padding(padding)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = RT.PrimaryInk)
                    }
                    Text("🧬 读者 DNA", color = RT.PrimaryInk,
                        fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
                if (dna == null) {
                    EmptyDna(
                        cooldownMs = cooldown,
                        generating = generating,
                        onGenerate = { vm.generate() }
                    )
                } else {
                    DnaContent(
                        dna = dna,
                        card = current!!,
                        cards = cards,
                        selectedIndex = selectedIndex,
                        onSelect = { selectedIndex = it },
                        cooldownMs = cooldown,
                        generating = generating,
                        onGenerate = { vm.generate() }
                    )
                }
            }
        }
    }
}

@Composable
private fun DnaContent(
    dna: ReaderDnaRepository.ParsedDna,
    card: com.example.funlife.data.model.ReaderDnaCard,
    cards: List<com.example.funlife.data.model.ReaderDnaCard>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    cooldownMs: Long,
    generating: Boolean,
    onGenerate: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // 历史选择条
        if (cards.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(cards.size) { i ->
                    val sel = i == selectedIndex
                    val date = SimpleDateFormat("MM-dd", Locale.getDefault())
                        .format(Date(cards[i].generatedAt))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) RT.PrimaryInk else RT.CardSoft)
                            .clickable { onSelect(i) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(date, color = if (sel) Color.White else RT.SecondaryInk,
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        // 雷达图卡
        Box(
            Modifier
                .fillMaxWidth()
                .height(360.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(RT.CardCream)
                .padding(20.dp)
        ) {
            ReaderDnaRadar(dna = dna, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(14.dp))
        // Tagline
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(RT.heroGradient())
                .padding(20.dp)
        ) {
            Text("「${card.tagline}」",
                color = Color.White, fontSize = 16.sp, lineHeight = 26.sp,
                fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        if (dna.keywords.isNotEmpty()) {
            Text("关键词", color = RT.SecondaryInk, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            FlowChips(words = dna.keywords)
        }
        Spacer(Modifier.height(14.dp))
        Text("基于你最近 ${card.basedOnBookCount} 本读完的书与摘抄分析",
            color = RT.MutedInk, fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        // 重新生成按钮
        GenerateBar(cooldownMs, generating, onGenerate)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun EmptyDna(
    cooldownMs: Long,
    generating: Boolean,
    onGenerate: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🧬", fontSize = 56.sp)
        Spacer(Modifier.height(8.dp))
        Text("还没有 DNA 卡",
            color = RT.PrimaryInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "从你的书单和摘抄里，AI 会找出你的阅读人格——\n理性 vs 感性 / 向内 vs 向外 / 温柔 vs 锋利。",
            color = RT.SecondaryInk, fontSize = 13.sp, lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(28.dp))
        GenerateBar(cooldownMs, generating, onGenerate)
    }
}

@Composable
private fun GenerateBar(
    cooldownMs: Long,
    generating: Boolean,
    onGenerate: () -> Unit,
) {
    val available = cooldownMs <= 0L && !generating
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (available) RT.heroGradient()
                else androidx.compose.ui.graphics.SolidColor(RT.MutedInk)
            )
            .clickable(enabled = available, onClick = onGenerate)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            when {
                generating -> "✨ 正在解码你的阅读人格…"
                cooldownMs <= 0L -> "🧬 生成 / 重新生成"
                cooldownMs < 24L * 3600 * 1000 -> "❄️ 还需 ${cooldownMs / 3600000} 小时"
                else -> "❄️ 还需 ${cooldownMs / (24L * 3600 * 1000)} 天"
            },
            color = Color.White, fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowChips(words: List<String>) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        words.forEach { w ->
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(RT.AccentRose.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(w, color = RT.AccentRose,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

