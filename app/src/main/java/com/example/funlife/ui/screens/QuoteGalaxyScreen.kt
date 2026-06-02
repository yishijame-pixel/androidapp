// QuoteGalaxyScreen.kt — v53 阅光书房 · 匿名摘抄星河
//
// 视觉：
//   深空夜色 + 随机散布的星点（Canvas）
//   用户用手指拖移视野；点击星点 → 弹出对应摘抄气泡 + 点亮 / 举报操作
//   顶栏：刷新 + "寄一颗自己的星"（VIP1+ 才能写）
//
// 实现：
//   - feed 拉到的列表确定性映射到 (x, y, size, twinkle) 四元组（基于 id hash）
//   - 拖动只改 viewportOffset；星点位置不变 → 对所有人感觉是"同一片星空"
package com.example.funlife.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.funlife.ui.theme.ReadingRoomTheme as RT
import com.example.funlife.viewmodel.QuoteGalaxyViewModel
import com.example.funlife.vip.QuoteGalaxyCloudRepository
import kotlin.math.absoluteValue
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteGalaxyScreen(
    userId: Long,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as android.app.Application
    val vm: QuoteGalaxyViewModel = viewModel(
        key = "Galaxy_$userId",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                QuoteGalaxyViewModel(app, userId) as T
        }
    )
    val stars by vm.stars.collectAsState()
    val loading by vm.loading.collectAsState()
    val toast by vm.toast.collectAsState()
    val vipLevel by vm.vipLevel.collectAsState()

    val snack = remember { SnackbarHostState() }
    LaunchedEffect(toast) { toast?.let { snack.showSnackbar(it); vm.consumeToast() } }

    var selected by remember { mutableStateOf<QuoteGalaxyCloudRepository.StarItem?>(null) }
    var showCompose by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        containerColor = Color.Transparent,
        // 沉浸式：让深空背景延伸到状态栏后；HUD 自己处理 inset
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(RT.galaxyBackground())
        ) {
            // 星空层
            StarField(
                stars = stars,
                onTap = { selected = it },
            )

            // 顶部 HUD
            Column(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("✨ 摘抄星河", color = Color.White,
                            fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text("匿名 · 共 ${stars.size} 颗",
                            color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Default.Refresh, "刷新", tint = Color.White)
                    }
                }
            }

            if (loading && stars.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RT.GalaxyAccent)
                }
            }

            if (!loading && stars.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🌌", fontSize = 56.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("夜空还很安静",
                        color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("成为第一个发光的人？",
                        color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }

            // FAB：寄一颗
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(RT.GalaxyAccent)
                    .clickable { showCompose = true }
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Text("✦ 寄一颗", color = RT.GalaxyBgTop,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // 选中星点详情
    selected?.let { star ->
        StarDetailDialog(
            star = star,
            onDismiss = { selected = null },
            onLight = { vm.light(star.id) },
            onReport = { reason -> vm.report(star.id, reason); selected = null }
        )
    }

    if (showCompose) {
        ComposeStarSheet(
            vipLevel = vipLevel,
            onDismiss = { showCompose = false },
            onSubmit = { txt, book ->
                vm.publish(txt, book); showCompose = false
            }
        )
    }
}

/* ════════════════════════════════════════════════════════════
   星空 Canvas
   ════════════════════════════════════════════════════════════ */

@Composable
private fun StarField(
    stars: List<QuoteGalaxyCloudRepository.StarItem>,
    onTap: (QuoteGalaxyCloudRepository.StarItem) -> Unit,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    // 帧动画驱动闪烁
    val tick by androidx.compose.runtime.produceState(initialValue = 0L) {
        while (true) {
            value = System.currentTimeMillis()
            kotlinx.coroutines.delay(80)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    offset += dragAmount
                }
            }
            .pointerInput(stars) {
                detectTapGestures { tap ->
                    val hit = stars
                        .map { it to starPos(it, size, offset) }
                        .firstOrNull { (_, p) ->
                            (p - tap).getDistance() < 28.dp.toPx()
                        }?.first
                    if (hit != null) onTap(hit)
                }
            }
            .onSizeChanged { size = it }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // 远景星尘（确定性，依赖 stars 个数和当前布局尺寸）
            val w = this.size.width; val h = this.size.height
            val rng = java.util.Random(42)
            for (i in 0 until 80) {
                val x = (rng.nextFloat() * w + offset.x * 0.1f) % w
                val y = (rng.nextFloat() * h + offset.y * 0.1f) % h
                val r = 0.6f + rng.nextFloat() * 1.2f
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f + rng.nextFloat() * 0.15f),
                    radius = r,
                    center = Offset(if (x < 0) x + w else x, if (y < 0) y + h else y)
                )
            }
            // 真实摘抄星点
            stars.forEachIndexed { i, item ->
                val pos = starPos(item, size, offset)
                if (pos.x < -20 || pos.x > w + 20 || pos.y < -20 || pos.y > h + 20) return@forEachIndexed
                val baseR = 4f + (item.lightCount.coerceAtMost(20)) * 0.4f
                val twinkle = sin((tick / 400.0) + item.id.hashCode() % 10).toFloat()
                val r = baseR + twinkle * 0.8f
                // 光晕
                drawCircle(
                    color = RT.GalaxyAccent.copy(alpha = 0.18f),
                    radius = r * 4f, center = pos
                )
                drawCircle(
                    color = RT.GalaxyAccent.copy(alpha = 0.5f),
                    radius = r * 2f, center = pos
                )
                drawCircle(
                    color = RT.GalaxyStar,
                    radius = r, center = pos
                )
            }
        }
    }
}

/** 把 starId 哈希到 [0,1]^2 平面，再映射到当前画布 + 拖动偏移。 */
private fun starPos(item: QuoteGalaxyCloudRepository.StarItem, sz: IntSize, offset: Offset): Offset {
    if (sz.width == 0 || sz.height == 0) return Offset.Zero
    // 双散列保证 x/y 不相关
    val h1 = (item.id.hashCode() and 0x7fffffff)
    val h2 = (item.id.hashCode().toLong() * 2654435761L and 0x7fffffffffL).toInt()
    val nx = (h1 % 1000) / 1000f
    val ny = (h2 % 1000) / 1000f
    val w = sz.width.toFloat(); val h = sz.height.toFloat()
    // 留 8% padding
    val x = (w * 0.08f) + nx * (w * 0.84f) + offset.x
    val y = (h * 0.12f) + ny * (h * 0.74f) + offset.y
    return Offset(x, y)
}

/* ════════════════════════════════════════════════════════════
   弹窗
   ════════════════════════════════════════════════════════════ */

@Composable
private fun StarDetailDialog(
    star: QuoteGalaxyCloudRepository.StarItem,
    onDismiss: () -> Unit,
    onLight: () -> Unit,
    onReport: (String) -> Unit,
) {
    var showReport by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RT.GalaxyBgBottom,
        title = {
            Text(if (star.bookTitle.isNotBlank()) "—— 来自《${star.bookTitle}》"
                else "—— 来自一颗匿名的星",
                color = RT.GalaxyAccent, fontSize = 12.sp)
        },
        text = {
            Column {
                Text(star.text,
                    color = Color.White, fontSize = 16.sp, lineHeight = 26.sp,
                    fontStyle = FontStyle.Italic)
                Spacer(Modifier.height(12.dp))
                Text("✨ 已被 ${star.lightCount} 个人接住",
                    color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { onLight(); onDismiss() }) {
                Text("✨ 接住这颗星", color = RT.GalaxyAccent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { showReport = true }) {
                Text("举报", color = Color.White.copy(alpha = 0.5f))
            }
        }
    )
    if (showReport) {
        AlertDialog(
            onDismissRequest = { showReport = false },
            title = { Text("举报这颗星") },
            text = { Text("我们会人工复核。多次被举报的内容会被自动隐藏。") },
            confirmButton = {
                TextButton(onClick = { onReport(""); showReport = false }) { Text("确认举报") }
            },
            dismissButton = {
                TextButton(onClick = { showReport = false }) { Text("取消") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposeStarSheet(
    vipLevel: Int,
    onDismiss: () -> Unit,
    onSubmit: (text: String, bookTitle: String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var book by remember { mutableStateOf("") }
    val canPublish = vipLevel >= 1
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = RT.GalaxyBgBottom) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("寄一颗自己的星", color = Color.White,
                fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            if (!canPublish) {
                Text("⛔ 月卡及以上才能在星河发声。下方输入仅作预览。",
                    color = RT.AccentRose, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
            } else {
                Text("匿名发出 · 不会带你的昵称、不会被检索。每天最多 1 条。",
                    color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                Spacer(Modifier.height(10.dp))
            }
            OutlinedTextField(
                value = text, onValueChange = { text = it.take(200) },
                placeholder = { Text("一段你想送进夜空的话…", color = Color.White.copy(alpha = 0.4f)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = RT.GalaxyAccent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                )
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = book, onValueChange = { book = it.take(80) },
                placeholder = { Text("出自哪本书？（选填）", color = Color.White.copy(alpha = 0.4f)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = RT.GalaxyAccent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                )
            )
            Spacer(Modifier.height(8.dp))
            Text("${text.length} / 200", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (canPublish && text.trim().isNotEmpty())
                            androidx.compose.ui.graphics.SolidColor(RT.GalaxyAccent)
                        else androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.15f))
                    )
                    .clickable(enabled = canPublish && text.trim().isNotEmpty()) {
                        onSubmit(text.trim(), book.trim())
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (canPublish) "✨ 寄出"
                    else "🔒 月卡解锁",
                    color = if (canPublish && text.trim().isNotEmpty()) RT.GalaxyBgTop else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

