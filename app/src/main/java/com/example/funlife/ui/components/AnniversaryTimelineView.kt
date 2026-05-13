// AnniversaryTimelineView.kt - 纪念日时间轴视图
package com.example.funlife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.data.model.Anniversary
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AnniversaryTimelineView(
    anniversaries: List<Anniversary>,
    onDelete: (Anniversary) -> Unit,
    onPin: (Anniversary) -> Unit,
    onEdit: (Anniversary) -> Unit,
    onShare: (Anniversary) -> Unit,
    modifier: Modifier = Modifier
) {
    // 按日期排序（最近的在前）
    val sortedAnniversaries = remember(anniversaries) {
        anniversaries.sortedByDescending { 
            try {
                LocalDate.parse(it.date, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (e: Exception) {
                LocalDate.MIN
            }
        }
    }
    
    // 按年份分组
    val groupedByYear = remember(sortedAnniversaries) {
        sortedAnniversaries.groupBy { anniversary ->
            try {
                LocalDate.parse(anniversary.date, DateTimeFormatter.ISO_LOCAL_DATE).year
            } catch (e: Exception) {
                0
            }
        }.toSortedMap(reverseOrder())
    }
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groupedByYear.forEach { (year, yearAnniversaries) ->
            // 年份标题
            item(key = "year_$year") {
                YearHeader(year = year)
            }
            
            // 该年份的纪念日
            items(yearAnniversaries, key = { it.id }) { anniversary ->
                TimelineItem(
                    anniversary = anniversary,
                    onDelete = { onDelete(anniversary) },
                    onPin = { onPin(anniversary) },
                    onEdit = { onEdit(anniversary) },
                    onShare = { onShare(anniversary) }
                )
            }
        }
    }
}

@Composable
private fun YearHeader(year: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = year.toString().takeLast(2), // 显示年份后两位
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = "${year}年",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        )
    }
}

@Composable
private fun TimelineItem(
    anniversary: Anniversary,
    onDelete: () -> Unit,
    onPin: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 时间轴线条和节点
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            // 节点
            Surface(
                shape = CircleShape,
                color = if (anniversary.isPinned) {
                    Color(0xFFFFD700) // 金色
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                modifier = Modifier.size(12.dp)
            ) {}
            
            // 连接线
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(200.dp) // 固定高度，实际会被卡片撑开
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
        }
        
        // 纪念日卡片
        AnniversaryCard(
            anniversary = anniversary,
            onDelete = onDelete,
            onPin = onPin,
            onEdit = onEdit,
            onShare = onShare,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp)
        )
    }
}

// 记住函数，避免重复计算
@Composable
private fun <T> remember(vararg keys: Any?, calculation: () -> T): T {
    return androidx.compose.runtime.remember(*keys, calculation = calculation)
}
