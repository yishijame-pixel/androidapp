// LetterComposeScreen.kt — 时光信箱 · 写信页 / 收信人管理
package com.example.funlife.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.data.model.LetterRecipient
import com.example.funlife.data.model.RecipientRelation
import com.example.funlife.repository.LetterRepository
import com.example.funlife.viewmodel.LetterSendUiState
import com.example.funlife.viewmodel.LetterViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

private val PaperBg = Color(0xFFFBF7EE)
private val Ink = Color(0xFF3F2E1F)
private val InkSoft = Color(0xFF6B5238)
private val Ribbon = Color(0xFFB39DDB)
private val RibbonDark = Color(0xFF7E57C2)
private val VipGold = Color(0xFFD4A437)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LetterComposeScreen(
    viewModel: LetterViewModel,
    initialRecipientId: Long?,
    onNavigateBack: () -> Unit,
    onSent: (letterId: Long) -> Unit,
    onUpgrade: () -> Unit
) {
    val context = LocalContext.current
    val recipients by viewModel.recipients.collectAsState()
    val vipLevel by viewModel.vipLevel.collectAsState()
    val sendState by viewModel.sendState.collectAsState()

    var selectedRecipientId by remember(initialRecipientId, recipients) {
        mutableStateOf(initialRecipientId ?: recipients.firstOrNull()?.id)
    }
    var content by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf<String?>(null) }
    /** 用户选择的投递延迟（小时） */
    var deliveryDelayHours by remember(vipLevel) {
        mutableStateOf(defaultDelayHours(vipLevel))
    }

    val minDelayMs = LetterRepository.minDeliveryDelayMs(vipLevel)
    val quota = LetterRepository.monthlyQuota(vipLevel)
    val remaining by produceState(initialValue = -1, key1 = recipients, key2 = sendState) {
        value = viewModel.remainingThisMonth()
    }

    // 处理 sendState 副作用
    LaunchedEffect(sendState) {
        when (val s = sendState) {
            is LetterSendUiState.Sent -> {
                android.widget.Toast.makeText(context, "信已封好，等待 TA 的回信", android.widget.Toast.LENGTH_LONG).show()
                viewModel.resetSendState()
                onSent(s.letterId)
            }
            is LetterSendUiState.QuotaExceeded -> {
                // UI 已显示提示，无副作用
            }
            is LetterSendUiState.Error -> {
                android.widget.Toast.makeText(context, s.msg, android.widget.Toast.LENGTH_SHORT).show()
                viewModel.resetSendState()
            }
            else -> Unit
        }
    }

    Scaffold(
        containerColor = PaperBg,
        topBar = {
            TopAppBar(
                title = { Text("写一封信", fontWeight = FontWeight.Bold, color = Ink) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, "返回", tint = Ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaperBg, scrolledContainerColor = PaperBg
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // ── 配额提示 ──
            QuotaBanner(quota = quota, remaining = remaining, vipLevel = vipLevel, onUpgrade = onUpgrade)
            Spacer(Modifier.height(12.dp))

            // ── 选择收信人 ──
            SectionTitle("寄给")
            if (recipients.isEmpty()) {
                Text(
                    "请先到「信箱→右上角→收信人管理」创建一个 TA。",
                    fontSize = 12.sp, color = InkSoft
                )
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(recipients, key = { it.id }) { r ->
                        RecipientSelectChip(
                            recipient = r,
                            selected = r.id == selectedRecipientId,
                            onClick = { selectedRecipientId = r.id }
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // ── 心情 emoji ──
            SectionTitle("写信时的心情")
            MoodPicker(selected = mood, onSelect = { mood = if (mood == it) null else it })
            Spacer(Modifier.height(14.dp))

            // ── 正文 ──
            SectionTitle("写下来吧（最长 5000 字）")
            OutlinedTextField(
                value = content,
                onValueChange = { if (it.length <= 5000) content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Ribbon,
                    unfocusedBorderColor = Ink.copy(alpha = 0.15f),
                    cursorColor = RibbonDark,
                    focusedContainerColor = Color.White.copy(alpha = 0.7f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("提笔，跟 TA 说说话...", color = Ink.copy(alpha = 0.35f)) },
                keyboardOptions = KeyboardOptions.Default
            )
            Text(
                "${content.length} / 5000",
                fontSize = 10.sp, color = Ink.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
            )
            Spacer(Modifier.height(14.dp))

            // ── 投递时间 ──
            SectionTitle("什么时候送达？")
            DeliverySlider(
                vipLevel = vipLevel,
                hours = deliveryDelayHours,
                onChange = { deliveryDelayHours = it },
                onUpgrade = onUpgrade
            )

            Spacer(Modifier.height(18.dp))

            // ── 发送按钮 ──
            Button(
                onClick = {
                    val rid = selectedRecipientId
                    if (rid == null) {
                        android.widget.Toast.makeText(context, "请先选择收信人", android.widget.Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val deliveryAt = System.currentTimeMillis() +
                        TimeUnit.HOURS.toMillis(deliveryDelayHours.toLong())
                    viewModel.sendLetter(rid, content, mood, deliveryAt)
                },
                enabled = sendState !is LetterSendUiState.Sending && content.isNotBlank() && selectedRecipientId != null,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RibbonDark),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (sendState is LetterSendUiState.Sending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White, strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("封信中...", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Rounded.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("封缄并寄出", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // 配额耗尽提示
            if (sendState is LetterSendUiState.QuotaExceeded) {
                Spacer(Modifier.height(10.dp))
                QuotaExceededHint(vipLevel = vipLevel, onUpgrade = onUpgrade)
            }
            // 投递太快提示
            if (sendState is LetterSendUiState.DeliveryTooSoon) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "免费/月卡用户的最快投递为 ${minDelayMs / (24 * 3600_000L)} 天。开通年卡或终身可立即送达。",
                    color = VipGold, fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// 收信人管理页（新建 / 编辑 / 删除）
// ════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LetterRecipientManagerScreen(
    viewModel: LetterViewModel,
    onNavigateBack: () -> Unit
) {
    val recipients by viewModel.recipients.collectAsState()
    var editing by remember { mutableStateOf<LetterRecipient?>(null) }
    var creating by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<LetterRecipient?>(null) }

    Scaffold(
        containerColor = PaperBg,
        topBar = {
            TopAppBar(
                title = { Text("收信人", fontWeight = FontWeight.Bold, color = Ink) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, "返回", tint = Ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaperBg)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                containerColor = RibbonDark,
                contentColor = Color.White,
                icon = { Icon(Icons.Rounded.PersonAdd, null) },
                text = { Text("新建", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (recipients.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("✉️", fontSize = 48.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("还没有收信人", fontWeight = FontWeight.Bold, color = Ink)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "可以是 5 年前的你、暧昧未表白的 TA、已经远去的家人...",
                                fontSize = 12.sp, color = InkSoft,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(recipients, key = { it.id }) { r ->
                    RecipientCard(
                        recipient = r,
                        onEdit = { editing = r },
                        onDelete = { pendingDelete = r }
                    )
                }
            }
        }
    }

    if (creating || editing != null) {
        RecipientEditDialog(
            existing = editing,
            onDismiss = { creating = false; editing = null },
            onSave = { name, avatar, uri, relation, persona, anchor ->
                viewModel.saveRecipient(editing, name, avatar, uri, relation, persona, anchor)
                creating = false; editing = null
            }
        )
    }

    if (pendingDelete != null) {
        val target = pendingDelete!!
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除「${target.name}」？", fontWeight = FontWeight.Bold) },
            text = { Text("和 TA 之间的所有信件也会一并删除，无法恢复。", color = InkSoft) },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteRecipient(target); pendingDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                ) { Text("删除", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun RecipientCard(
    recipient: LetterRecipient,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onEdit,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Ink.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Ribbon.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(recipient.avatar.ifBlank { "✉️" }, fontSize = 24.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(recipient.name, fontWeight = FontWeight.Bold, color = Ink, fontSize = 15.sp)
                Text(
                    relationLabel2(recipient.relation),
                    fontSize = 11.sp, color = InkSoft
                )
                if (recipient.persona.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        recipient.persona,
                        fontSize = 11.sp,
                        color = Ink.copy(alpha = 0.5f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, "删除", tint = Color(0xFFEF5350))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipientEditDialog(
    existing: LetterRecipient?,
    onDismiss: () -> Unit,
    onSave: (name: String, avatar: String, customUri: String?, relation: String, persona: String, timeAnchor: Long?) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var avatar by remember { mutableStateOf(existing?.avatar ?: "✉️") }
    var relation by remember { mutableStateOf(existing?.relation ?: RecipientRelation.SELF_PAST) }
    var persona by remember { mutableStateOf(existing?.persona ?: "") }
    var anchorYearText by remember {
        mutableStateOf(existing?.timeAnchor?.let {
            SimpleDateFormat("yyyy", Locale.CHINA).format(Date(it))
        } ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "创建收信人" else "编辑收信人", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("称呼", fontSize = 12.sp, color = InkSoft, fontWeight = FontWeight.Medium)
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 40) name = it },
                    placeholder = { Text("如：5 年前的我 / 爷爷 / 暧昧期的她") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(Modifier.height(10.dp))

                Text("头像 emoji", fontSize = 12.sp, color = InkSoft, fontWeight = FontWeight.Medium)
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(EMOJI_OPTIONS) { e ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (e == avatar) Ribbon.copy(alpha = 0.4f) else Color(0xFFF1ECE0))
                                .clickable { avatar = e },
                            contentAlignment = Alignment.Center
                        ) { Text(e, fontSize = 18.sp) }
                    }
                }
                Spacer(Modifier.height(10.dp))

                Text("关系", fontSize = 12.sp, color = InkSoft, fontWeight = FontWeight.Medium)
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(RELATION_OPTIONS) { (key, label) ->
                        FilterChip(
                            selected = relation == key,
                            onClick = { relation = key },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Ribbon,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                if (relation == RecipientRelation.SELF_PAST || relation == RecipientRelation.SELF_FUTURE) {
                    Text("时间锚（年份）", fontSize = 12.sp, color = InkSoft, fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value = anchorYearText,
                        onValueChange = { anchorYearText = it.filter { c -> c.isDigit() }.take(4) },
                        placeholder = { Text("如 2020") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                }

                Text("人设描述（AI 会用这个口吻回信）", fontSize = 12.sp, color = InkSoft, fontWeight = FontWeight.Medium)
                OutlinedTextField(
                    value = persona,
                    onValueChange = { if (it.length <= 800) persona = it },
                    placeholder = { Text("例：我的爷爷，退休教师，严厉但内心柔软，常说'吃得苦中苦方为人上人'") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    shape = RoundedCornerShape(10.dp)
                )
                Text("${persona.length}/800", fontSize = 10.sp, color = Ink.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.End))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val anchor = anchorYearText.toIntOrNull()?.let { y ->
                        Calendar.getInstance().apply {
                            set(Calendar.YEAR, y); set(Calendar.MONTH, 0)
                            set(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                        }.timeInMillis
                    }
                    onSave(name.trim(), avatar, existing?.customAvatarUri, relation, persona.trim(), anchor)
                },
                colors = ButtonDefaults.buttonColors(containerColor = RibbonDark),
                enabled = name.isNotBlank()
            ) { Text("保存", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// ════════════════════════════════════════════════════════════════════
// 小组件
// ════════════════════════════════════════════════════════════════════

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 12.sp, color = InkSoft, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun QuotaBanner(quota: Int, remaining: Int, vipLevel: Int, onUpgrade: () -> Unit) {
    val isUnlimited = quota == LetterRepository.UNLIMITED
    val bg = if (vipLevel >= 2) VipGold.copy(alpha = 0.15f) else Ribbon.copy(alpha = 0.12f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isUnlimited) Icons.Rounded.AllInclusive else Icons.Rounded.Mail,
            null,
            tint = if (vipLevel >= 2) VipGold else RibbonDark,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            when {
                isUnlimited -> "VIP · 本月可无限写信"
                remaining < 0 -> "本月还可写 ... 封"
                else -> "本月还可写 $remaining / $quota 封"
            },
            fontSize = 12.sp, color = Ink, fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (!isUnlimited) {
            TextButton(onClick = onUpgrade) {
                Text("升级", color = RibbonDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun QuotaExceededHint(vipLevel: Int, onUpgrade: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFEE7E7))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.WarningAmber, null, tint = Color(0xFFE65A50))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("本月配额已用完", fontWeight = FontWeight.Bold, color = Ink, fontSize = 13.sp)
            Text(
                if (vipLevel == 0) "普通用户每月仅 1 封。月卡 5 封 / 年卡终身无限。"
                else "月卡每月 5 封。升级年卡或终身可不限。",
                fontSize = 11.sp, color = InkSoft
            )
        }
        Button(
            onClick = onUpgrade,
            colors = ButtonDefaults.buttonColors(containerColor = VipGold),
            shape = RoundedCornerShape(10.dp)
        ) { Text("升级 VIP", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun MoodPicker(selected: String?, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(MOOD_EMOJIS) { e ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (e == selected) Ribbon else Color(0xFFF1ECE0))
                    .clickable { onSelect(e) },
                contentAlignment = Alignment.Center
            ) { Text(e, fontSize = 20.sp) }
        }
    }
}

@Composable
private fun RecipientSelectChip(
    recipient: LetterRecipient,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Ribbon else Color(0xFFF1ECE0))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(recipient.avatar.ifBlank { "✉️" }, fontSize = 14.sp)
        Spacer(Modifier.width(4.dp))
        Text(
            recipient.name,
            color = if (selected) Color.White else Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DeliverySlider(
    vipLevel: Int,
    hours: Int,
    onChange: (Int) -> Unit,
    onUpgrade: () -> Unit
) {
    val minDelayMs = LetterRepository.minDeliveryDelayMs(vipLevel)
    val minHours = (minDelayMs / 3600_000L).toInt().coerceAtLeast(0)
    val safeHours = hours.coerceAtLeast(minHours)
    val deliveryAt = System.currentTimeMillis() + safeHours * 3600_000L
    val dateText = SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(deliveryAt))

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Schedule, null, tint = InkSoft, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (safeHours == 0) "立即送达"
                else "${humanDelay(safeHours)} 后 · $dateText",
                fontSize = 13.sp, color = Ink, fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(6.dp))
        Slider(
            value = safeHours.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = minHours.toFloat()..(24f * 30f),    // 最多 30 天
            colors = SliderDefaults.colors(
                thumbColor = RibbonDark,
                activeTrackColor = Ribbon,
                inactiveTrackColor = Ribbon.copy(alpha = 0.25f)
            )
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                when (vipLevel) {
                    0 -> "普通用户：最快 3 天后"
                    1 -> "月卡 VIP：最快 1 天后"
                    else -> "VIP：可立即送达"
                },
                fontSize = 11.sp, color = Ink.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f)
            )
            if (vipLevel < 2) {
                TextButton(onClick = onUpgrade) {
                    Text("升级解锁立即送达", color = VipGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun humanDelay(hours: Int): String {
    if (hours == 0) return "立即"
    val days = hours / 24
    val h = hours % 24
    return when {
        days > 0 && h == 0 -> "${days}天"
        days > 0 -> "${days}天${h}小时"
        else -> "${h}小时"
    }
}

private fun defaultDelayHours(vipLevel: Int): Int = when {
    vipLevel >= 2 -> 24       // 默认 1 天
    vipLevel == 1 -> 24
    else          -> 72       // 默认 3 天（也是免费用户最小延迟）
}

private fun relationLabel2(r: String): String = when (r) {
    RecipientRelation.SELF_PAST -> "过去的我"
    RecipientRelation.SELF_FUTURE -> "未来的我"
    RecipientRelation.FAMILY -> "家人"
    RecipientRelation.LOVER -> "恋人 / 暧昧"
    RecipientRelation.FRIEND -> "朋友"
    else -> "自定义"
}

private val EMOJI_OPTIONS = listOf("✉️", "💌", "🌸", "🌙", "☀️", "🍃", "🐱", "🐰", "👴", "👵", "👨", "👩", "🧒", "🧑‍🎓", "💍")

private val MOOD_EMOJIS = listOf("😊", "🥲", "😴", "😢", "😠", "😍", "🤔", "😌", "😔", "💪")

private val RELATION_OPTIONS = listOf(
    RecipientRelation.SELF_PAST to "过去的我",
    RecipientRelation.SELF_FUTURE to "未来的我",
    RecipientRelation.FAMILY to "家人",
    RecipientRelation.LOVER to "恋人",
    RecipientRelation.FRIEND to "朋友",
    RecipientRelation.CUSTOM to "自定义"
)
