// LetterMailboxScreen.kt — 时光信箱 · 信箱总览页
package com.example.funlife.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.funlife.data.model.LetterDirection
import com.example.funlife.data.model.LetterRecipient
import com.example.funlife.data.model.LetterStatus
import com.example.funlife.data.model.RecipientRelation
import com.example.funlife.repository.LetterView
import com.example.funlife.viewmodel.LetterViewModel
import java.text.SimpleDateFormat
import java.util.*

private val PaperBg = Color(0xFFF5EFE2)
private val PaperBgLight = Color(0xFFFBF7EE)
private val Ink = Color(0xFF3F2E1F)
private val InkSoft = Color(0xFF6B5238)
private val Ribbon = Color(0xFFB39DDB)
private val RibbonDark = Color(0xFF7E57C2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LetterMailboxScreen(
    viewModel: LetterViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCompose: (recipientId: Long?) -> Unit,
    onNavigateToDetail: (letterId: Long) -> Unit,
    onManageRecipients: () -> Unit,
    onNavigateToVip: () -> Unit = {}
) {
    val recipients by viewModel.recipients.collectAsState()
    val letters by viewModel.letters.collectAsState()
    val unread by viewModel.unreadCount.collectAsState()
    val vipLevel by viewModel.vipLevel.collectAsState()
    val sentThisMonth by viewModel.sentThisMonth.collectAsState()

    // 🆕 v51 配额弹窗：进入本页时会推一次（8 天间隔；额度耗尽则强弹）
    val quota = com.example.funlife.vip.VipQuota.letterMonthlyLimit(vipLevel)
    val unlimited = quota == com.example.funlife.vip.VipQuota.UNLIMITED
    val exhausted = !unlimited && sentThisMonth >= quota
    val subtitle = buildString {
        append("本月已寄 ")
        append(com.example.funlife.vip.VipQuota.formatLetterUsage(sentThisMonth, vipLevel))
        append(" · 投递 ")
        append(com.example.funlife.vip.VipQuota.formatMinDelay(vipLevel))
    }
    com.example.funlife.ui.components.QuotaBannerOneShot(
        featureKey = "letter_mailbox",
        userId = viewModel.userId,
        vipLevel = vipLevel,
        used = sentThisMonth,
        quota = if (unlimited) -1 else quota,
        exhausted = exhausted,
        title = "你的时光信箱额度",
        subtitle = subtitle,
        tease = com.example.funlife.vip.VipQuota.nextTierTeaser(vipLevel),
        onUpgrade = onNavigateToVip
    )

    Scaffold(
        containerColor = PaperBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("时光信箱", fontWeight = FontWeight.Bold, color = Ink, fontSize = 18.sp)
                        Text(
                            if (unread > 0) "$unread 封新回信" else "把心事，写给那个 TA",
                            fontSize = 11.sp, color = InkSoft
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, "返回", tint = Ink)
                    }
                },
                actions = {
                    IconButton(onClick = onManageRecipients) {
                        Icon(Icons.Rounded.People, "收信人管理", tint = Ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaperBg, scrolledContainerColor = PaperBg
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToCompose(null) },
                containerColor = RibbonDark,
                contentColor = Color.White,
                icon = { Icon(Icons.Rounded.Edit, null) },
                text = { Text("写信", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ===== 收信人滑条 =====
            if (recipients.isEmpty()) {
                EmptyRecipientHint(onCreate = onManageRecipients)
            } else {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) {
                    items(recipients, key = { it.id }) { r ->
                        RecipientChip(
                            recipient = r,
                            onClick = { onNavigateToCompose(r.id) }
                        )
                    }
                    item {
                        AddRecipientChip(onClick = onManageRecipients)
                    }
                }
            }

            Divider(color = Ink.copy(alpha = 0.08f))

            // ===== 信件列表 =====
            if (letters.isEmpty()) {
                EmptyLetterHint()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(letters, key = { _, l -> l.id }) { _, letter ->
                        LetterRow(
                            letter = letter,
                            recipient = recipients.firstOrNull { it.id == letter.recipientId },
                            onClick = { onNavigateToDetail(letter.id) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun RecipientChip(recipient: LetterRecipient, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Ribbon.copy(alpha = 0.4f), PaperBgLight)
                    )
                )
                .border(1.5.dp, Ribbon.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!recipient.customAvatarUri.isNullOrBlank()) {
                AsyncImage(
                    model = recipient.customAvatarUri,
                    contentDescription = recipient.name,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Text(recipient.avatar.ifBlank { "✉️" }, fontSize = 24.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            recipient.name,
            fontSize = 11.sp,
            color = Ink,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            relationLabel(recipient.relation),
            fontSize = 9.sp,
            color = InkSoft,
            maxLines = 1
        )
    }
}

@Composable
private fun AddRecipientChip(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(PaperBgLight)
                .border(1.5.dp, Ink.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Add, null, tint = InkSoft, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text("新建", fontSize = 11.sp, color = InkSoft, fontWeight = FontWeight.Medium)
        Text("收信人", fontSize = 9.sp, color = InkSoft)
    }
}

@Composable
private fun LetterRow(
    letter: LetterView,
    recipient: LetterRecipient?,
    onClick: () -> Unit
) {
    val isReply = letter.direction == LetterDirection.FROM_RECIPIENT
    val isOnTheWay = letter.deliveredAt == null && letter.status == LetterStatus.PENDING
    val cardColor = if (isReply) PaperBgLight else PaperBg
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isReply && !letter.isRead) Ribbon else Ink.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 左侧图标：邮件/在路上
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isReply) Ribbon.copy(alpha = 0.2f) else Ink.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                val icon = when {
                    isOnTheWay -> Icons.Rounded.HourglassEmpty
                    isReply -> Icons.Rounded.MarkEmailUnread
                    else -> Icons.Rounded.Outbox
                }
                Icon(icon, null, tint = if (isReply) RibbonDark else InkSoft, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when {
                            isReply -> "${recipient?.name ?: "TA"} 回信"
                            else    -> "致 ${recipient?.name ?: "TA"}"
                        },
                        fontWeight = FontWeight.Bold, color = Ink, fontSize = 14.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isReply && !letter.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(RibbonDark)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isOnTheWay) "信在路上 · 预计 ${formatRelativeTime(letter.deliveryAt)}"
                    else letter.content,
                    fontSize = 12.sp,
                    color = InkSoft,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    formatTimeShort(letter.deliveredAt ?: letter.deliveryAt),
                    fontSize = 10.sp,
                    color = Ink.copy(alpha = 0.45f)
                )
            }
        }
    }
}

@Composable
private fun EmptyRecipientHint(onCreate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("✉️", fontSize = 40.sp)
        Spacer(Modifier.height(8.dp))
        Text("还没有收信人", fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(4.dp))
        Text(
            "先创建一个 TA —— 可以是 5 年前的自己、未来的自己、想念的人...",
            fontSize = 12.sp, color = InkSoft,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onCreate,
            colors = ButtonDefaults.buttonColors(containerColor = RibbonDark),
            shape = RoundedCornerShape(12.dp)
        ) { Text("创建收信人", color = Color.White) }
    }
}

@Composable
private fun EmptyLetterHint() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📬", fontSize = 48.sp)
            Spacer(Modifier.height(8.dp))
            Text("信箱空空", fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(4.dp))
            Text("点击右下角「写信」开始你的第一封", fontSize = 12.sp, color = InkSoft)
        }
    }
}

private fun relationLabel(r: String): String = when (r) {
    RecipientRelation.SELF_PAST -> "过去的我"
    RecipientRelation.SELF_FUTURE -> "未来的我"
    RecipientRelation.FAMILY -> "家人"
    RecipientRelation.LOVER -> "恋人"
    RecipientRelation.FRIEND -> "朋友"
    else -> "自定义"
}

private fun formatTimeShort(ts: Long): String =
    SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.CHINA).format(Date(ts))

private fun formatRelativeTime(ts: Long): String {
    val diff = ts - System.currentTimeMillis()
    if (diff <= 0) return "即将到达"
    val days = diff / (24 * 3600 * 1000L)
    val hours = (diff % (24 * 3600 * 1000L)) / 3600_000L
    val minutes = (diff % 3600_000L) / 60_000L
    return buildString {
        if (days > 0) append("${days}天")
        if (hours > 0) append("${hours}小时")
        if (days == 0L && hours == 0L) append("${minutes}分钟")
        append("后")
    }
}
