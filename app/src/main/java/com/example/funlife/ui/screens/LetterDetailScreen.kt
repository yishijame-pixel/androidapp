// LetterDetailScreen.kt — 时光信箱 · 信件详情（含翻信封动画）
package com.example.funlife.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.data.model.LetterDirection
import com.example.funlife.data.model.LetterRecipient
import com.example.funlife.repository.LetterView
import com.example.funlife.viewmodel.LetterViewModel
import java.text.SimpleDateFormat
import java.util.*

private val PaperBg = Color(0xFFFBF7EE)
private val PaperLine = Color(0xFFE8DFC9)
private val Ink = Color(0xFF3F2E1F)
private val InkSoft = Color(0xFF6B5238)
private val Ribbon = Color(0xFFB39DDB)
private val RibbonDark = Color(0xFF7E57C2)
private val Stamp = Color(0xFFE65A50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LetterDetailScreen(
    viewModel: LetterViewModel,
    letterId: Long,
    onNavigateBack: () -> Unit,
    onReply: (recipientId: Long) -> Unit
) {
    val recipients by viewModel.recipients.collectAsState()
    val letters by viewModel.letters.collectAsState()
    val letter = remember(letters, letterId) { letters.firstOrNull { it.id == letterId } }
    val recipient: LetterRecipient? = remember(recipients, letter) {
        letter?.let { l -> recipients.firstOrNull { it.id == l.recipientId } }
    }

    LaunchedEffect(letterId) {
        // 标记已读
        viewModel.markLetterRead(letterId)
    }

    if (letter == null) {
        Scaffold(containerColor = PaperBg) { p ->
            Box(modifier = Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                Text("信件不存在或已被删除", color = InkSoft)
            }
        }
        return
    }

    val isReply = letter.direction == LetterDirection.FROM_RECIPIENT
    val isOnTheWay = letter.deliveredAt == null

    Scaffold(
        containerColor = PaperBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            isReply -> "${recipient?.name ?: "TA"} 的回信"
                            else    -> "致 ${recipient?.name ?: "TA"}"
                        },
                        fontWeight = FontWeight.Bold, color = Ink
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, "返回", tint = Ink)
                    }
                },
                actions = {
                    if (recipient != null) {
                        IconButton(onClick = { onReply(recipient.id) }) {
                            Icon(Icons.Rounded.Edit, "再写一封", tint = Ink)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaperBg)
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
            if (isOnTheWay && isReply) {
                OnTheWayCard(deliveryAt = letter.deliveryAt)
            } else {
                EnvelopeOpenAnimation(isReply = isReply) {
                    LetterPaper(
                        letter = letter,
                        recipient = recipient,
                        isReply = isReply
                    )
                }
            }
        }
    }
}

@Composable
private fun OnTheWayCard(deliveryAt: Long) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 信封图标 + 呼吸
        val infinite = rememberInfiniteTransition(label = "breathe")
        val alpha by infinite.animateFloat(
            initialValue = 0.5f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse),
            label = "alpha"
        )
        Text("✉️", fontSize = 64.sp, modifier = Modifier.graphicsLayer { this.alpha = alpha })
        Spacer(Modifier.height(16.dp))
        Text("信在路上", fontWeight = FontWeight.Bold, color = Ink, fontSize = 18.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "预计 ${SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(deliveryAt))} 送达",
            fontSize = 12.sp, color = InkSoft
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "想让 TA 提前回信吗？\n开通年卡 VIP，信件可立即送达。",
            fontSize = 12.sp, color = Ink.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EnvelopeOpenAnimation(isReply: Boolean, content: @Composable () -> Unit) {
    // 进入动画：信件从信封口"展开" —— scale + alpha + 轻微旋转
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.86f,
        animationSpec = tween(600, easing = EaseOutCubic), label = "scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(700, easing = EaseOutCubic), label = "alpha"
    )
    val rot by animateFloatAsState(
        targetValue = if (visible) 0f else if (isReply) -2f else 2f,
        animationSpec = tween(700, easing = EaseOutCubic), label = "rot"
    )
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale; scaleY = scale
                this.alpha = alpha
                rotationZ = rot
            }
    ) { content() }
}

@Composable
private fun LetterPaper(
    letter: LetterView,
    recipient: LetterRecipient?,
    isReply: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(PaperBg, Color(0xFFF6EFDA))
                )
            )
            .border(1.dp, PaperLine, RoundedCornerShape(14.dp))
            .padding(20.dp)
    ) {
        // 邮戳/称谓行
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                (if (isReply) recipient?.avatar else "✍️") ?: "✉️",
                fontSize = 28.sp
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isReply) "from「${recipient?.name ?: "TA"}」" else "to「${recipient?.name ?: "TA"}」",
                    fontSize = 13.sp, color = Ink, fontWeight = FontWeight.Bold
                )
                Text(
                    SimpleDateFormat("yyyy 年 M 月 d 日 HH:mm", Locale.CHINA)
                        .format(Date(letter.deliveredAt ?: letter.sentAt)),
                    fontSize = 11.sp, color = InkSoft
                )
            }
            // 邮戳：圆形带文字
            PostStamp(isReply = isReply)
        }

        Spacer(Modifier.height(14.dp))
        Divider(color = PaperLine)
        Spacer(Modifier.height(14.dp))

        // 心情 emoji（如果有）
        if (!letter.mood.isNullOrBlank() && !isReply) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("写信时的心情：", fontSize = 11.sp, color = InkSoft)
                Text(letter.mood, fontSize = 18.sp)
            }
            Spacer(Modifier.height(10.dp))
        }

        // 信件正文
        Text(
            letter.content,
            fontSize = 15.sp,
            lineHeight = 26.sp,
            color = Ink,
            fontFamily = FontFamily.Serif
        )

        Spacer(Modifier.height(20.dp))
        // 落款
        if (isReply) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    "—— ${recipient?.name ?: "TA"}",
                    fontSize = 13.sp,
                    color = Ink,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Serif
                )
            }
        }

        // 解密失败提示
        if (!letter.decryptOk) {
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFEE7E7))
                    .padding(12.dp)
            ) {
                Text(
                    "⚠ 此信加密校验失败，可能数据迁移损坏或非本账号写出。",
                    fontSize = 11.sp, color = Color(0xFFB71C1C)
                )
            }
        }

        // AI 兜底回信提示
        if (isReply && letter.failureReason != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                "（这封信因网络问题暂未生成完整内容，可在「写信」中再写一封触发重写）",
                fontSize = 10.sp, color = Ink.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PostStamp(isReply: Boolean) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .rotate(if (isReply) -12f else 8f)
            .clip(RoundedCornerShape(50))
            .border(2.dp, Stamp.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(2.dp)
            .border(1.dp, Stamp.copy(alpha = 0.3f), RoundedCornerShape(50)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (isReply) "已送达" else "已寄出",
                fontSize = 8.sp, color = Stamp, fontWeight = FontWeight.Bold
            )
            Text(
                SimpleDateFormat("M.d", Locale.CHINA).format(Date()),
                fontSize = 8.sp, color = Stamp
            )
        }
    }
}
