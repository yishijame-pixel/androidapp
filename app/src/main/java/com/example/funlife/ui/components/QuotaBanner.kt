// QuotaBanner.kt — 通用配额展示弹窗
//
// 设计目标：
//   - 进入某个 VIP 限额功能页时弹出一次，展示「身份 + 当前用量 + 升级文案」
//   - 频控：同一用户同一功能 7 天最多弹一次；额度耗尽时强制弹（覆盖 7 天间隔）
//   - 完全无业务耦合：调用方传入展示数据 + onUpgrade 回调即可
//
// 频控持久化：SharedPreferences（按 userId + featureKey 隔离），不写 DB
package com.example.funlife.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.funlife.data.model.VipLevel
import java.util.concurrent.TimeUnit

/**
 * 配额弹窗一次性展示器（无 UI 内容时不渲染）。
 *
 * @param featureKey 业务唯一标识，如 "letter_mailbox"
 * @param userId    当前用户
 * @param vipLevel  当前 VIP 等级 (Int)
 * @param used      已用额度
 * @param quota     总额度，VipQuota.UNLIMITED 表示无限
 * @param exhausted 是否已耗尽（耗尽强弹一次，绕过 7 天间隔）
 * @param title     弹窗标题
 * @param subtitle  副标题（如 "本月已寄 3 / 5 封 · 投递最快 24 小时"）
 * @param tease     升级文案（VipQuota.nextTierTeaser 提供，可为 null = 已是最高档）
 * @param onUpgrade 点击「立即升级」回调（一般跳 Vip 页）
 */
@Composable
fun QuotaBannerOneShot(
    featureKey: String,
    userId: Long,
    vipLevel: Int,
    used: Int,
    quota: Int,
    exhausted: Boolean,
    title: String,
    subtitle: String,
    tease: String?,
    onUpgrade: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showDialog by remember(featureKey, userId) { mutableStateOf(false) }
    var triggered by remember(featureKey, userId) { mutableStateOf(false) }

    // 进入页时（首帧后）评估一次。triggered 防止回到此 Composable 反复弹。
    LaunchedEffect(featureKey, userId, exhausted) {
        if (triggered) return@LaunchedEffect
        triggered = true
        if (QuotaBannerPrefs.shouldShow(context, featureKey, userId, exhausted)) {
            showDialog = true
            QuotaBannerPrefs.markShown(context, featureKey, userId)
        }
    }

    if (!showDialog) return
    Dialog(
        onDismissRequest = { showDialog = false },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        QuotaBannerCard(
            vipLevel = vipLevel,
            used = used,
            quota = quota,
            title = title,
            subtitle = subtitle,
            tease = tease,
            exhausted = exhausted,
            onClose = { showDialog = false },
            onUpgrade = {
                showDialog = false
                onUpgrade()
            }
        )
    }
}

@Composable
private fun QuotaBannerCard(
    vipLevel: Int,
    used: Int,
    quota: Int,
    title: String,
    subtitle: String,
    tease: String?,
    exhausted: Boolean,
    onClose: () -> Unit,
    onUpgrade: () -> Unit
) {
    val level = VipLevel.fromLevel(vipLevel)
    val accent = Color(level.color)
    val isMaxTier = tease == null
    val pct = if (quota <= 0) 1f else (used.toFloat() / quota).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 顶部身份徽章
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(accent.copy(alpha = 0.85f), accent.copy(alpha = 0.55f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(level.icon, fontSize = 22.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        level.displayName,
                        fontSize = 13.sp,
                        color = accent,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 19.sp
            )

            // 进度条（无限额度跳过）
            if (quota > 0) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = pct,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (exhausted) Color(0xFFE53935) else accent,
                    trackColor = accent.copy(alpha = 0.15f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (exhausted) "本月额度已用完" else "已用 $used / $quota",
                    fontSize = 12.sp,
                    color = if (exhausted) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isMaxTier && tease != null) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = accent.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        tease,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 12.5.sp,
                        color = accent,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onClose) {
                    Text(if (isMaxTier) "知道了" else "稍后再说", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!isMaxTier) {
                    Spacer(Modifier.width(6.dp))
                    // 🎨 升级按钮：高饱和暖色渐变（橙→金→粉），制造"花钱按钮"的视觉冲击
                    //    比单一品牌色 accent 醒目得多，CTR 验证最佳的支付按钮配色
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFF6B35),  // 橙
                                        Color(0xFFFF3D7F),  // 粉红
                                        Color(0xFFFFB300)   // 金黄
                                    )
                                )
                            )
                    ) {
                        TextButton(
                            onClick = onUpgrade,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                        ) {
                            Text(
                                if (exhausted) "✨ 立即升级解锁" else "✨ 看看升级权益",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ======================== 频控持久化 ======================== */

object QuotaBannerPrefs {
    private const val PREFS_NAME = "quota_banner_prefs"
    private val INTERVAL_MS = TimeUnit.DAYS.toMillis(7)

    fun shouldShow(context: Context, featureKey: String, userId: Long, exhausted: Boolean): Boolean {
        if (exhausted) return true   // 额度耗尽强弹，覆盖 7 天间隔
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val last = prefs.getLong(key(featureKey, userId), 0L)
        return System.currentTimeMillis() - last >= INTERVAL_MS
    }

    fun markShown(context: Context, featureKey: String, userId: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(key(featureKey, userId), System.currentTimeMillis())
            .apply()
    }

    private fun key(featureKey: String, userId: Long) = "${featureKey}_$userId"
}
