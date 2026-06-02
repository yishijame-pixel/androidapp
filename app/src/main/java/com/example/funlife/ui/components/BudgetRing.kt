// BudgetRing.kt — 预算进度环（聊天记账 Phase 2B）
//
// 设计：
//   · 横滑列表，每条预算一颗 64dp 圆环 + 旁边数字
//   · 进度环颜色随状态变化：< 80% 主题色 / >= 80% 黄 / >= 100% 红
//   · 长按整条 → 弹出菜单（暂留接口）
//
// 数据隔离：仅渲染传入的 BudgetProgress 列表，不直接读 DB。
package com.example.funlife.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.funlife.data.model.BudgetPeriod
import com.example.funlife.data.model.BudgetScope
import com.example.funlife.repository.BudgetProgress
import com.example.funlife.ui.utils.Radius
import com.example.funlife.ui.utils.Spacing
import com.example.funlife.ui.utils.TextSize
import java.util.Locale

@Composable
fun BudgetRingStrip(
    progresses: List<BudgetProgress>,
    onClickItem: (BudgetProgress) -> Unit = {},
    onAddBudget: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (progresses.isEmpty()) {
        // 空态：一条彩色提示条引导用户设置预算
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = 6.dp)
                .clip(RoundedCornerShape(Radius.lg))
                .background(Color(0xFFFFF7E6))
                .border(1.dp, Color(0xFFFFD18A), RoundedCornerShape(Radius.lg))
                .clickable { onAddBudget() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(Color(0xFFFFA726)),
                contentAlignment = Alignment.Center
            ) {
                Text("+", fontSize = TextSize.headline, fontWeight = FontWeight.Black, color = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "设置月度预算",
                    fontSize = TextSize.sm,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFE65100)
                )
                Text(
                    "实时进度环 · 超 80% 警示 · 超额飘红",
                    fontSize = TextSize.tiny,
                    color = Color(0xFF8D6E63),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                "去设置 ›",
                fontSize = TextSize.xs,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFA726)
            )
        }
        return
    }
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(items = progresses, key = { it.budget.id }) { p ->
            BudgetRingCard(progress = p, onClick = { onClickItem(p) })
        }
    }
}

@Composable
private fun BudgetRingCard(progress: BudgetProgress, onClick: () -> Unit) {
    val budget = progress.budget
    val accent = Color(budget.color or 0xFF000000L)
    val ringColor = when {
        progress.exceeded -> Color(0xFFEF4444)
        progress.warned -> Color(0xFFFFA726)
        else -> accent
    }
    val bg = when {
        progress.exceeded -> Color(0xFFFFEBEE)
        progress.warned -> Color(0xFFFFF7E6)
        else -> Color.White
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.lg))
            .background(bg)
            .border(1.dp, ringColor.copy(alpha = 0.35f), RoundedCornerShape(Radius.lg))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 环形 progress
        Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sw = 5f
                // 底圈
                drawArc(
                    color = ringColor.copy(alpha = 0.18f),
                    startAngle = -90f, sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = sw, cap = StrokeCap.Round),
                    topLeft = Offset(sw, sw),
                    size = Size(size.width - sw * 2, size.height - sw * 2)
                )
                val sweep = (progress.percent.coerceIn(0f, 1f)) * 360f
                if (sweep > 0f) {
                    drawArc(
                        color = ringColor,
                        startAngle = -90f, sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = sw, cap = StrokeCap.Round),
                        topLeft = Offset(sw, sw),
                        size = Size(size.width - sw * 2, size.height - sw * 2)
                    )
                }
                // 超额第二圈淡红
                if (progress.exceeded) {
                    val over = ((progress.percent - 1f).coerceAtMost(1f)) * 360f
                    if (over > 0f) {
                        drawArc(
                            color = Color(0xFFEF4444).copy(alpha = 0.55f),
                            startAngle = -90f, sweepAngle = over,
                            useCenter = false,
                            style = Stroke(width = sw - 1f, cap = StrokeCap.Round),
                            topLeft = Offset(sw + 4f, sw + 4f),
                            size = Size(size.width - (sw + 4f) * 2, size.height - (sw + 4f) * 2)
                        )
                    }
                }
            }
            Text(
                "${(progress.percent * 100).toInt()}%",
                fontSize = TextSize.tiny,
                fontWeight = FontWeight.Black,
                color = ringColor
            )
        }
        Column {
            Text(
                budget.name,
                fontSize = TextSize.xs,
                fontWeight = FontWeight.Black,
                color = Color(0xFF424242)
            )
            Text(
                "${money(progress.used)} / ${money(budget.amount)} · ${labelOf(budget.period)}",
                fontSize = TextSize.tiny,
                color = Color(0xFF757575),
                fontWeight = FontWeight.SemiBold
            )
            // 副标：scope 提示
            val scopeHint = when (budget.scope) {
                BudgetScope.CATEGORY -> "分类 · ${budget.targetKey ?: "-"}"
                BudgetScope.ACCOUNT -> "账户预算"
                else -> "总预算"
            }
            Text(
                scopeHint,
                fontSize = TextSize.tiny,
                color = ringColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun money(v: Double): String {
    val abs = kotlin.math.abs(v)
    return if (abs >= 10_000) String.format(Locale.CHINA, "¥%.1f万", abs / 10_000)
    else String.format(Locale.CHINA, "¥%.0f", abs)
}

private fun labelOf(period: String): String = when (period) {
    BudgetPeriod.WEEKLY -> "本周"
    BudgetPeriod.YEARLY -> "本年"
    else -> "本月"
}
