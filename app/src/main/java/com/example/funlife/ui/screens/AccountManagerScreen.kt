// AccountManagerScreen.kt — 账户管理中心（Phase 3 · 暗黑霓虹风）
//
// 🔒 数据隔离：所有操作经 ChatViewModel（构造时绑定 userId）。
//    无 userId 字面量；新建账户由 Repository 强制要求 userId > 0。
package com.example.funlife.ui.screens

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.funlife.data.model.Account
import com.example.funlife.data.model.AccountType
import com.example.funlife.ui.utils.ResponsiveDialogBox
import com.example.funlife.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.util.Locale

// 复用 BudgetManagerScreen 的颜色（保持视觉一致性）
private val DarkBg = Color(0xFF0D1117)
private val DarkCard = Color(0xFF161B22)
private val DarkCardLight = Color(0xFF1A1F2E)
private val NeonCyan = Color(0xFF00F5D4)
private val NeonPurple = Color(0xFFBB86FC)
private val NeonPink = Color(0xFFFF6B9D)
private val NeonOrange = Color(0xFFFF9E43)
private val NeonGreen = Color(0xFF22C55E)
private val NeonRed = Color(0xFFFF4D6D)
private val NeonBlue = Color(0xFF64DFDF)
private val TextPrimary = Color(0xFFE6EDF3)
private val TextSecondary = Color(0xFF8B949E)
private val GlowLine = Color(0xFF30363D)

@Composable
fun AccountManagerScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val accounts by viewModel.accounts.collectAsState()
    val allAccounts by viewModel.allAccounts.collectAsState()

    var editorAccount by remember { mutableStateOf<Account?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deletePending by remember { mutableStateOf<Account?>(null) }

    val totalBalance = accounts.sumOf { it.balance }
    val totalInitial = accounts.sumOf { it.initialBalance }

    val infinite = rememberInfiniteTransition(label = "neon")
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "g"
    )
    val gridShift by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "gs"
    )

    Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        // 动态网格背景
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellSize = 40f
            val offset = gridShift * cellSize
            val w = size.width; val h = size.height
            var x = -cellSize + (offset % cellSize)
            while (x < w) {
                drawLine(GlowLine.copy(alpha = 0.3f), Offset(x, 0f), Offset(x, h), 0.6f); x += cellSize
            }
            var y = -cellSize + (offset % cellSize)
            while (y < h) {
                drawLine(GlowLine.copy(alpha = 0.3f), Offset(0f, y), Offset(w, y), 0.6f); y += cellSize
            }
        }
        // 顶部光晕
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(NeonCyan.copy(alpha = 0.10f), size.width * 0.55f, Offset(-size.width * 0.1f, -size.width * 0.2f))
            drawCircle(NeonPurple.copy(alpha = 0.08f), size.width * 0.45f, Offset(size.width * 1.05f, -size.width * 0.15f))
        }

        Column(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)
        ) {
            TopBar(
                glowAlpha = glowAlpha,
                onNavigateBack = onNavigateBack,
                onAdd = { editorAccount = null; showEditor = true }
            )

            if (allAccounts.isEmpty()) {
                EmptyState(
                    glowAlpha = glowAlpha,
                    onCreate = { editorAccount = null; showEditor = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        TotalBalanceCard(
                            balance = totalBalance,
                            initialBalance = totalInitial,
                            activeCount = accounts.size,
                            archivedCount = allAccounts.count { it.isArchived },
                            glowAlpha = glowAlpha
                        )
                    }
                    val active = allAccounts.filter { !it.isArchived }
                    val archived = allAccounts.filter { it.isArchived }
                    if (active.isNotEmpty()) {
                        item { SectionHeader("启用账户", active.size, glowAlpha) }
                        items(items = active, key = { it.id }) { a ->
                            AccountItemCard(
                                account = a,
                                onClickEdit = { editorAccount = a; showEditor = true },
                                onArchive = {
                                    scope.launch { viewModel.setAccountArchived(a.id, true) }
                                },
                                onUnarchive = null,
                                onRequestDelete = { deletePending = a }
                            )
                        }
                    }
                    if (archived.isNotEmpty()) {
                        item { SectionHeader("已归档", archived.size, glowAlpha, color = TextSecondary) }
                        items(items = archived, key = { it.id }) { a ->
                            AccountItemCard(
                                account = a,
                                onClickEdit = { editorAccount = a; showEditor = true },
                                onArchive = null,
                                onUnarchive = {
                                    scope.launch { viewModel.setAccountArchived(a.id, false) }
                                },
                                onRequestDelete = { deletePending = a }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        AccountEditorDialog(
            initial = editorAccount,
            onDismiss = { showEditor = false },
            onConfirm = { newAccount ->
                scope.launch { viewModel.saveAccount(newAccount) }
                showEditor = false
            }
        )
    }

    deletePending?.let { target ->
        AlertDialog(
            onDismissRequest = { deletePending = null },
            containerColor = DarkCard,
            shape = RoundedCornerShape(20.dp),
            title = { Text("删除「${target.name}」？", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    "删除后该账户从切换条消失。已记录的账单不会删除，但 accountId 仍指向此条已删除账户。" +
                        "建议优先选择「归档」。",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { viewModel.deleteAccount(target) }
                        deletePending = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed)
                ) { Text("仍然删除", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { deletePending = null }) { Text("取消", color = TextSecondary) }
            }
        )
    }
}

// ═══════════════════════════════════════════════
// 顶栏
// ═══════════════════════════════════════════════
@Composable
private fun TopBar(glowAlpha: Float, onNavigateBack: () -> Unit, onAdd: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(DarkCard, DarkBg.copy(alpha = 0.95f))))
            .drawBehind {
                drawLine(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, NeonCyan.copy(alpha = glowAlpha * 0.6f), Color.Transparent)
                    ),
                    Offset(0f, size.height), Offset(size.width, size.height),
                    strokeWidth = 1.5f
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart).size(40.dp)
                .clip(RoundedCornerShape(12.dp)).background(DarkCardLight)
                .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .clickable { onNavigateBack() },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.ArrowBack, "返回", tint = NeonCyan, modifier = Modifier.size(20.dp)) }
        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🏦", fontSize = 18.sp)
                Text("账户管理中心", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextPrimary, letterSpacing = 2.sp)
            }
            Text(
                "ACCOUNT · VAULT",
                fontSize = 9.sp, color = NeonBlue.copy(alpha = 0.7f),
                letterSpacing = 4.sp, fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd).size(40.dp)
                .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = NeonCyan)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(NeonCyan, NeonBlue)))
                .clickable { onAdd() },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Add, "新建", tint = DarkBg, modifier = Modifier.size(22.dp)) }
    }
}

// ═══════════════════════════════════════════════
// 总资产卡
// ═══════════════════════════════════════════════
@Composable
private fun TotalBalanceCard(
    balance: Double, initialBalance: Double,
    activeCount: Int, archivedCount: Int,
    glowAlpha: Float
) {
    val delta = balance - initialBalance
    val deltaColor = if (delta >= 0) NeonGreen else NeonRed
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = NeonCyan)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(DarkCard, DarkCardLight, DarkCard)))
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(NeonCyan.copy(alpha = 0.45f * glowAlpha), NeonPurple.copy(alpha = 0.35f))
                ),
                RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NeonCyan)
                    .drawBehind {
                        drawCircle(NeonCyan.copy(alpha = glowAlpha * 0.6f), size.minDimension * 1.6f)
                    })
                Spacer(Modifier.width(8.dp))
                Text("VAULT · 总资产", fontSize = 11.sp, color = TextSecondary, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                money(balance),
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${if (delta >= 0) "+" else ""}${money(delta)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = deltaColor
                )
                Text("vs 初始", fontSize = 11.sp, color = TextSecondary, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniBadge("启用", "$activeCount", NeonGreen)
                if (archivedCount > 0) MiniBadge("归档", "$archivedCount", TextSecondary)
            }
        }
    }
}

@Composable
private fun MiniBadge(label: String, value: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
            Text(label, fontSize = 10.sp, color = TextSecondary, letterSpacing = 1.sp)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

// ═══════════════════════════════════════════════
// 区段标题
// ═══════════════════════════════════════════════
@Composable
private fun SectionHeader(title: String, count: Int, glowAlpha: Float, color: Color = NeonCyan) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(4.dp, 18.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(10.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Black, color = TextPrimary, letterSpacing = 2.sp)
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.12f))
                .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) { Text("$count", fontSize = 10.sp, fontWeight = FontWeight.Black, color = color) }
    }
}

// ═══════════════════════════════════════════════
// 账户卡
// ═══════════════════════════════════════════════
@Composable
private fun AccountItemCard(
    account: Account,
    onClickEdit: () -> Unit,
    onArchive: (() -> Unit)?,
    onUnarchive: (() -> Unit)?,
    onRequestDelete: () -> Unit
) {
    val accent = mapAccountColorToNeon(account.color)
    val balanceColor = when {
        account.balance < 0 -> NeonRed
        account.balance >= account.initialBalance * 1.5 -> NeonGreen
        else -> accent
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = accent)
            .clip(RoundedCornerShape(18.dp))
            .let { m ->
                if (account.isArchived) m.background(DarkCard.copy(alpha = 0.6f))
                else m.background(Brush.linearGradient(listOf(DarkCard, DarkCardLight)))
            }
            .border(1.dp, accent.copy(alpha = if (account.isArchived) 0.18f else 0.35f), RoundedCornerShape(18.dp))
    ) {
        Box(
            modifier = Modifier.fillMaxHeight().width(3.dp).background(accent).align(Alignment.CenterStart)
        )
        Column(modifier = Modifier.padding(start = 18.dp, end = 14.dp, top = 14.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.12f))
                        .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) { Text(account.icon, fontSize = 24.sp) }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            account.name,
                            fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextPrimary
                        )
                        if (account.isArchived) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(GlowLine)
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) { Text("已归档", fontSize = 9.sp, color = TextSecondary, letterSpacing = 1.sp) }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        accountTypeLabel(account.type),
                        fontSize = 11.sp,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        money(account.balance),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = balanceColor
                    )
                    Text(
                        "初始 ${money(account.initialBalance)}",
                        fontSize = 9.sp,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                if (onArchive != null) {
                    ActionPill(label = "归档", color = TextSecondary, onClick = onArchive)
                    Spacer(Modifier.width(6.dp))
                }
                if (onUnarchive != null) {
                    ActionPill(label = "启用", color = NeonGreen, onClick = onUnarchive)
                    Spacer(Modifier.width(6.dp))
                }
                ActionIcon(icon = Icons.Default.Edit, color = NeonPurple, onClick = onClickEdit)
                Spacer(Modifier.width(6.dp))
                ActionIcon(icon = Icons.Default.Delete, color = NeonRed, onClick = onRequestDelete)
            }
        }
    }
}

@Composable
private fun ActionPill(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Black, color = color, letterSpacing = 1.sp) }
}

@Composable
private fun ActionIcon(icon: ImageVector, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = color, modifier = Modifier.size(15.dp)) }
}

// ═══════════════════════════════════════════════
// 空态
// ═══════════════════════════════════════════════
@Composable
private fun EmptyState(glowAlpha: Float, onCreate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .shadow(16.dp, CircleShape, spotColor = NeonCyan)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(DarkCardLight, DarkCard)))
                .border(2.dp, NeonCyan.copy(alpha = 0.5f * glowAlpha), CircleShape),
            contentAlignment = Alignment.Center
        ) { Text("🏦", fontSize = 64.sp) }
        Spacer(Modifier.height(20.dp))
        Text("ACCOUNT · STANDBY", fontSize = 11.sp, color = NeonBlue.copy(alpha = 0.8f), letterSpacing = 4.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("尚未配置任何账户", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextPrimary, letterSpacing = 2.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "为现金 / 支付宝 / 微信 / 储蓄卡 / 信用卡\n创建独立账户，精细追踪每一笔",
            fontSize = 12.sp, color = TextSecondary,
            textAlign = TextAlign.Center, lineHeight = 18.sp, letterSpacing = 1.sp
        )
        Spacer(Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f).height(50.dp)
                .shadow(14.dp, RoundedCornerShape(25.dp), spotColor = NeonCyan)
                .clip(RoundedCornerShape(25.dp))
                .background(Brush.horizontalGradient(listOf(NeonCyan, NeonBlue, NeonPurple)))
                .clickable { onCreate() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("✨", fontSize = 16.sp)
                Text("立即创建账户", color = DarkBg, fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 2.sp)
            }
        }
    }
}

// ═══════════════════════════════════════════════
// 编辑/新增 弹层
// ═══════════════════════════════════════════════
@Composable
private fun AccountEditorDialog(
    initial: Account?,
    onDismiss: () -> Unit,
    onConfirm: (Account) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var iconText by remember { mutableStateOf(initial?.icon ?: "💰") }
    var typeSel by remember { mutableStateOf(initial?.type ?: AccountType.OTHER) }
    var initialBalanceText by remember {
        mutableStateOf(initial?.initialBalance?.let { String.format(Locale.CHINA, "%.2f", it) } ?: "0")
    }
    val typeOptions = listOf(
        AccountType.CASH to ("💵" to "现金"),
        AccountType.ALIPAY to ("🅰️" to "支付宝"),
        AccountType.WECHAT to ("💬" to "微信"),
        AccountType.DEBIT to ("💳" to "储蓄卡"),
        AccountType.CREDIT to ("🏦" to "信用卡"),
        AccountType.OTHER to ("💰" to "其它")
    )

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        ResponsiveDialogBox {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkBg)
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(DarkCardLight, DarkCard)))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (initial == null) "✨" else "✏️", fontSize = 18.sp)
                            Text(
                                if (initial == null) "新建账户" else "编辑账户",
                                fontSize = 18.sp, fontWeight = FontWeight.Black,
                                color = TextPrimary, letterSpacing = 2.sp
                            )
                        }
                        Text(
                            "ACCOUNT · CONFIG",
                            fontSize = 9.sp, color = NeonBlue.copy(alpha = 0.7f),
                            letterSpacing = 4.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    FieldLabel("类型")
                    // 类型选择：3 列 emoji 卡
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        typeOptions.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { (t, pair) ->
                                    val (e, lbl) = pair
                                    val sel = typeSel == t
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (sel) Brush.linearGradient(
                                                    listOf(NeonCyan.copy(alpha = 0.18f), NeonPurple.copy(alpha = 0.15f))
                                                ) else Brush.linearGradient(listOf(DarkCard, DarkCard))
                                            )
                                            .border(
                                                if (sel) 1.5.dp else 1.dp,
                                                if (sel) NeonCyan else GlowLine,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                typeSel = t
                                                if (iconText.isBlank() || initial == null) iconText = e
                                                if (name.isBlank() && initial == null) name = lbl
                                            }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(e, fontSize = 16.sp)
                                            Text(
                                                lbl, fontSize = 12.sp,
                                                color = if (sel) NeonCyan else TextSecondary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                repeat(3 - row.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    FieldLabel("名称")
                    NeonInput(value = name, onChange = { name = it.take(15) }, placeholder = "如：招行储蓄卡")

                    FieldLabel("图标（emoji）")
                    NeonInput(value = iconText, onChange = { iconText = it.take(2) }, placeholder = "💰")

                    FieldLabel("初始余额（元）")
                    NeonInput(
                        value = initialBalanceText,
                        onChange = { v -> initialBalanceText = v.filter { it.isDigit() || it == '.' || it == '-' }.take(12) },
                        placeholder = "如：500（信用卡可输负数代表已用额度）",
                        keyboardType = KeyboardType.Number
                    )

                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .weight(1f).height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(DarkCard)
                                .border(1.dp, GlowLine, RoundedCornerShape(23.dp))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) { Text("取消", color = TextSecondary, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f).height(46.dp)
                                .shadow(12.dp, RoundedCornerShape(23.dp), spotColor = NeonCyan)
                                .clip(RoundedCornerShape(23.dp))
                                .background(Brush.horizontalGradient(listOf(NeonCyan, NeonBlue, NeonPurple)))
                                .clickable {
                                    val ib = initialBalanceText.toDoubleOrNull() ?: 0.0
                                    val finalName = name.ifBlank { typeOptions.firstOrNull { it.first == typeSel }?.second?.second ?: "账户" }
                                    val finalIcon = iconText.ifBlank { typeOptions.firstOrNull { it.first == typeSel }?.second?.first ?: "💰" }
                                    val color: Long = when (typeSel) {
                                        AccountType.CASH -> 0xFF66BB6AL
                                        AccountType.ALIPAY -> 0xFF2196F3L
                                        AccountType.WECHAT -> 0xFF4CAF50L
                                        AccountType.DEBIT -> 0xFF7E57C2L
                                        AccountType.CREDIT -> 0xFFFF7043L
                                        else -> 0xFF9E9E9EL
                                    }
                                    val newAccount = (initial ?: Account(
                                        userId = 0L, name = finalName, type = typeSel,
                                        icon = finalIcon, color = color, initialBalance = ib,
                                        balance = ib
                                    )).copy(
                                        name = finalName,
                                        type = typeSel,
                                        icon = finalIcon,
                                        initialBalance = ib,
                                        // 新建：balance = initial；编辑：保持原 balance（用户后续可重算）
                                        balance = initial?.balance ?: ib,
                                        color = color
                                    )
                                    onConfirm(newAccount)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (initial == null) "创建账户" else "保存修改",
                                color = DarkBg, fontWeight = FontWeight.Black, letterSpacing = 2.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
// 工具
// ═══════════════════════════════════════════════
@Composable
private fun FieldLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(3.dp, 12.dp).clip(RoundedCornerShape(1.5.dp)).background(NeonCyan))
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Black, color = TextSecondary, letterSpacing = 2.sp)
    }
}

@Composable
private fun NeonInput(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .border(1.dp, GlowLine, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.Bold),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = Brush.linearGradient(listOf(NeonCyan, NeonPurple)),
            modifier = Modifier.fillMaxWidth()
        )
        if (value.isEmpty()) {
            Text(placeholder, fontSize = 15.sp, color = TextSecondary.copy(alpha = 0.6f))
        }
    }
}

private fun mapAccountColorToNeon(rawColor: Long): Color = when (rawColor) {
    0xFF66BB6AL -> NeonGreen
    0xFF2196F3L -> NeonBlue
    0xFF4CAF50L -> NeonGreen
    0xFF7E57C2L -> NeonPurple
    0xFFFF7043L -> NeonOrange
    0xFF9E9E9EL -> TextSecondary
    else -> NeonCyan
}

private fun accountTypeLabel(type: String): String = when (type) {
    AccountType.CASH -> "CASH · 现金"
    AccountType.ALIPAY -> "ALIPAY · 支付宝"
    AccountType.WECHAT -> "WECHAT · 微信"
    AccountType.DEBIT -> "DEBIT · 储蓄卡"
    AccountType.CREDIT -> "CREDIT · 信用卡"
    else -> "OTHER · 其它"
}

private fun money(v: Double): String {
    val abs = kotlin.math.abs(v)
    val sign = if (v < 0) "-" else ""
    return when {
        abs >= 10_000 -> String.format(Locale.CHINA, "%s¥%.1f万", sign, abs / 10_000)
        else -> String.format(Locale.CHINA, "%s¥%.2f", sign, abs)
    }
}
