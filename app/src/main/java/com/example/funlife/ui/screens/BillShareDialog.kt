// BillShareDialog.kt — 每日账单分享卡 Dialog
//
// ⚠️ 数据隔离：
//   - 调用方必须传入 currentUserId（无默认值）
//   - 内部传给 BillShareHelper.summarize / renderAndShare 时强制校验 userId > 0
//   - 文件名带 userId 防止跨账号缓存碰撞
//
// 屏幕适配：使用 Spacing / Radius / TextSize / IconSize / ResponsiveDialogBox
package com.example.funlife.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Image as FImage
import com.example.funlife.data.model.Bill
import com.example.funlife.ui.utils.IconSize
import com.example.funlife.ui.utils.Radius
import com.example.funlife.ui.utils.ResponsiveDialogBox
import com.example.funlife.ui.utils.Spacing
import com.example.funlife.ui.utils.TextSize
import com.example.funlife.ui.utils.rdp
import com.example.funlife.ui.utils.rsp
import com.example.funlife.utils.BillShareHelper
import com.example.funlife.utils.BillShareTheme
import com.example.funlife.utils.DailyBillSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun BillShareDialog(
    currentUserId: Long,
    nickname: String,
    allBills: List<Bill>,
    onDismiss: () -> Unit,
    avatarBitmap: Bitmap? = null,
    initialDate: Calendar = Calendar.getInstance()
) {
    require(currentUserId > 0L) { "BillShareDialog 必须传入有效 userId（数据隔离）" }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // ──── 状态：日期 / 主题 / 备注 / 预览 ────
    var selectedDate by remember { mutableStateOf(initialDate) }
    var selectedTheme by remember { mutableStateOf(BillShareTheme.SAKURA) }
    var userNote by remember { mutableStateOf("") }
    var previewBmp by remember { mutableStateOf<Bitmap?>(null) }
    var rendering by remember { mutableStateOf(false) }
    var sharing by remember { mutableStateOf(false) }

    // 当前日期的聚合（按 currentUserId 强过滤）
    val summary: DailyBillSummary = remember(currentUserId, allBills, selectedDate) {
        BillShareHelper.summarize(currentUserId, allBills, selectedDate)
    }

    // 主题/日期/备注变化 → 后台重渲染预览
    LaunchedEffect(selectedTheme, summary, userNote) {
        rendering = true
        previewBmp = withContext(Dispatchers.Default) {
            runCatching {
                BillShareHelper.renderBitmap(
                    ctx = ctx,
                    userId = currentUserId,
                    summary = summary,
                    theme = selectedTheme,
                    nickname = nickname.ifBlank { "我" },
                    avatarBitmap = avatarBitmap,
                    userNote = userNote.takeIf { it.isNotBlank() }
                )
            }.getOrNull()
        }
        rendering = false
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        ResponsiveDialogBox {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.xxl))
                    .background(Color(0xFFFFFBF7))
                    .verticalScroll(rememberScrollState())
            ) {
                // ───── Hero 头 ─────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.rdp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(selectedTheme.accent.toLong() or 0xFF000000),
                                    Color(selectedTheme.accentDark.toLong() or 0xFF000000)
                                )
                            )
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.rdp)
                            .align(Alignment.TopEnd)
                            .padding(Spacing.sm)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.28f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(IconSize.sm))
                    }
                    Column(
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = Spacing.lg)
                    ) {
                        androidx.compose.material3.Text(
                            "📤 分享今日账单",
                            fontSize = TextSize.headline,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        androidx.compose.material3.Text(
                            "选择主题、加一句话、一键发送给好友",
                            fontSize = TextSize.xs,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    // ── 日期切换 ──
                    DateChips(selectedDate) { selectedDate = it }

                    // ── 当日数据小结 ──
                    DaySummaryStrip(summary)

                    // ── 主题选择 ──
                    androidx.compose.material3.Text(
                        "🎨 选择主题",
                        fontSize = TextSize.sm, fontWeight = FontWeight.Black,
                        color = Color(0xFF3D1F2C)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BillShareTheme.values().forEach { t ->
                            ThemeChip(
                                theme = t,
                                selected = t == selectedTheme,
                                modifier = Modifier.weight(1f),
                                onClick = { selectedTheme = t }
                            )
                        }
                    }

                    // ── 一句话备注 ──
                    NoteInput(userNote) { userNote = it.take(72) }

                    // ── 预览 ──
                    PreviewBox(previewBmp, rendering, ratio = 1080f / 1920f)

                    // ── 行动按钮 ──
                    androidx.compose.material3.Button(
                        onClick = {
                            if (sharing) return@Button
                            sharing = true
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    BillShareHelper.renderAndShare(
                                        ctx = ctx,
                                        userId = currentUserId,
                                        summary = summary,
                                        theme = selectedTheme,
                                        nickname = nickname.ifBlank { "我" },
                                        avatarBitmap = avatarBitmap,
                                        userNote = userNote.takeIf { it.isNotBlank() }
                                    )
                                }
                                sharing = false
                                if (ok) onDismiss()
                                else android.widget.Toast.makeText(ctx, "分享失败，请重试", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.rdp),
                        shape = RoundedCornerShape(Radius.pill),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(selectedTheme.accent.toLong() or 0xFF000000)
                        )
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(IconSize.sm))
                        Spacer(Modifier.width(8.dp))
                        androidx.compose.material3.Text(
                            if (sharing) "正在生成…" else "立即分享",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = TextSize.sm
                        )
                    }

                    Spacer(Modifier.height(Spacing.xs))
                }
            }
        }
    }
}

// ─────────────────────────────── 子组件 ───────────────────────────────

@Composable
private fun DateChips(selected: Calendar, onPick: (Calendar) -> Unit) {
    val today = remember { Calendar.getInstance() }
    val choices = remember {
        (0..6).map { offset ->
            (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
        }
    }
    val fmt = remember { SimpleDateFormat("M/d", Locale.CHINA) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        choices.forEach { c ->
            val isSel = c.get(Calendar.YEAR) == selected.get(Calendar.YEAR) &&
                    c.get(Calendar.DAY_OF_YEAR) == selected.get(Calendar.DAY_OF_YEAR)
            val label = when {
                c.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "今日"
                c.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) - 1 -> "昨日"
                else -> fmt.format(c.time)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(if (isSel) Color(0xFFFF6B9D) else Color.White)
                    .border(
                        1.dp,
                        if (isSel) Color(0xFFFF6B9D) else Color(0xFFE5C8D5),
                        RoundedCornerShape(Radius.pill)
                    )
                    .clickable { onPick(c) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                androidx.compose.material3.Text(
                    label,
                    fontSize = TextSize.xs,
                    color = if (isSel) Color.White else Color(0xFF3D1F2C),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DaySummaryStrip(summary: DailyBillSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(Color(0xFFFFF1F2))
            .border(1.dp, Color(0xFFFFD9E2), RoundedCornerShape(Radius.lg))
            .padding(Spacing.sm),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatCol("收入", "+${money(summary.income)}", Color(0xFF10B981))
        StatCol("支出", "-${money(summary.expense)}", Color(0xFFEF4444))
        StatCol("结余", money(summary.balance), Color(0xFFFF6B9D))
        StatCol("笔数", "${summary.itemCount}", Color(0xFF7E57C2))
    }
}

@Composable
private fun StatCol(label: String, value: String, c: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.material3.Text(
            value, fontSize = TextSize.title, fontWeight = FontWeight.Black, color = c
        )
        androidx.compose.material3.Text(
            label, fontSize = TextSize.tiny, color = Color(0xFF8B5670), fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ThemeChip(theme: BillShareTheme, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(56.rdp)
            .clip(RoundedCornerShape(Radius.lg))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(theme.bgTop.toLong() or 0xFF000000),
                        Color(theme.bgBottom.toLong() or 0xFF000000)
                    )
                )
            )
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) Color(theme.accent.toLong() or 0xFF000000) else Color(0xFFE5C8D5),
                RoundedCornerShape(Radius.lg)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            theme.nameZh,
            fontSize = TextSize.xs,
            fontWeight = FontWeight.Black,
            color = Color(theme.titleColor.toLong() or 0xFF000000)
        )
    }
}

@Composable
private fun NoteInput(value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Text(
                "✍️ 一句话日记（可选）",
                fontSize = TextSize.sm, fontWeight = FontWeight.Black,
                color = Color(0xFF3D1F2C)
            )
            androidx.compose.material3.Text(
                "${value.length}/72",
                fontSize = TextSize.tiny, color = Color(0xFF8B5670)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.lg))
                .background(Color.White)
                .border(1.dp, Color(0xFFFFD9E2), RoundedCornerShape(Radius.lg))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = false,
                textStyle = TextStyle(fontSize = TextSize.sm, color = Color(0xFF3D1F2C)),
                modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp)
            )
            if (value.isEmpty()) {
                androidx.compose.material3.Text(
                    "今日总结、感受或一句话…",
                    fontSize = TextSize.sm, color = Color(0xFFC9A9B5)
                )
            }
        }
    }
}

@Composable
private fun PreviewBox(bmp: Bitmap?, loading: Boolean, ratio: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .shadow(8.dp, RoundedCornerShape(Radius.lg))
            .clip(RoundedCornerShape(Radius.lg))
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) {
            FImage(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "预览",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Image, null, modifier = Modifier.size(IconSize.xl), tint = Color(0xFFC9A9B5))
                androidx.compose.material3.Text(
                    if (loading) "正在生成预览…" else "无预览",
                    fontSize = TextSize.xs, color = Color(0xFF8B5670)
                )
            }
        }
    }
}

private fun money(v: Double): String {
    val abs = kotlin.math.abs(v)
    return if (abs >= 10000) String.format(Locale.CHINA, "%.1f万", abs / 10000)
    else String.format(Locale.CHINA, "¥%.0f", abs)
}
