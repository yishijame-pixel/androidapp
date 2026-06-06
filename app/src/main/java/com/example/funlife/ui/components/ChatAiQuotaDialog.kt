package com.example.funlife.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.funlife.ui.utils.ResponsiveDialogBox
import com.example.funlife.ui.utils.rdp
import com.example.funlife.ui.utils.rsp
import com.example.funlife.vip.ChatAiBarState
import com.example.funlife.vip.ChatAiLimits
import com.example.funlife.vip.ChatAiBilling
import com.example.funlife.vip.ChatAiEntitlementUi
import com.example.funlife.vip.ChatAiSku
import com.example.funlife.vip.VipQuota
import com.example.funlife.viewmodel.ChatViewModel

private val AiPurple = Color(0xFF7C4DFF)
private val AiPink = Color(0xFFFF6090)
private val AiCyan = Color(0xFF00E5FF)
private val AiGold = Color(0xFFFFD54F)

@Composable
fun ChatAiQuotaDialog(
    visible: Boolean,
    viewModel: ChatViewModel,
    themeColor: Color,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    val entitlement by viewModel.chatAiEntitlement.collectAsState()
    val isRedeeming by viewModel.isRedeemingChatAi.collectAsState()
    var cardCode by remember { mutableStateOf("") }
    var inlineError by remember { mutableStateOf<String?>(null) }
    var successFlash by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            viewModel.refreshChatAiEntitlement()
            cardCode = ""
            inlineError = null
            successFlash = false
        }
    }

    val shimmer = rememberInfiniteTransition(label = "ai_shimmer")
    val shimmerShift by shimmer.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
        label = "shift"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ResponsiveDialogBox {
            Surface(
                shape = RoundedCornerShape(24.rdp),
                color = Color.White,
                shadowElevation = 16.rdp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // ── 渐变顶栏 ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(88.rdp)
                            .clip(RoundedCornerShape(topStart = 24.rdp, topEnd = 24.rdp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        AiPurple,
                                        themeColor.copy(alpha = 0.92f),
                                        AiPink.copy(alpha = 0.85f)
                                    ),
                                    start = Offset(shimmerShift * 400f, 0f),
                                    end = Offset(shimmerShift * 400f + 320f, 180f)
                                )
                            )
                    ) {
                        // 装饰光斑
                        Box(
                            modifier = Modifier
                                .size(120.rdp)
                                .offset(x = (-20).rdp, y = (-30).rdp)
                                .background(Color.White.copy(alpha = 0.12f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(80.rdp)
                                .align(Alignment.TopEnd)
                                .offset(x = 20.rdp, y = 10.rdp)
                                .background(AiCyan.copy(alpha = 0.18f), CircleShape)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.rdp, vertical = 16.rdp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.rdp)
                                    .clip(RoundedCornerShape(14.rdp))
                                    .background(Color.White.copy(alpha = 0.22f))
                                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(14.rdp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🤖", fontSize = 26.rsp)
                            }
                            Spacer(Modifier.width(12.rdp))
                            Column {
                                Text(
                                    "AI 额度",
                                    color = Color.White,
                                    fontSize = 20.rsp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "灵犀智能对话 · 卡密激活",
                                    color = Color.White.copy(alpha = 0.88f),
                                    fontSize = 12.rsp
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.rdp, vertical = 18.rdp)
                    ) {
                        ChatAiStatusCard(entitlement = entitlement, themeColor = themeColor)

                        Spacer(Modifier.height(16.rdp))

                        Text(
                            "输入购买的 AI 卡密，激活云端大模型对话。\n未激活时仅使用本地智能回复，不消耗云端额度。",
                            fontSize = 12.rsp,
                            color = Color(0xFF888888),
                            lineHeight = 18.rsp
                        )

                        Spacer(Modifier.height(14.rdp))

                        OutlinedTextField(
                            value = cardCode,
                            onValueChange = {
                                cardCode = it.uppercase().replace(" ", "")
                                inlineError = null
                            },
                            label = { Text("卡密") },
                            placeholder = { Text("FL-XXXX-XXXX-XXXX", color = Color(0xFFCCCCCC)) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Key, null, tint = themeColor.copy(alpha = 0.7f))
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.rdp),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                if (cardCode.isNotBlank() && !isRedeeming) {
                                    viewModel.redeemChatAiCard(cardCode) { ok, msg ->
                                        if (ok) {
                                            cardCode = ""
                                            inlineError = null
                                            successFlash = true
                                        } else {
                                            inlineError = msg
                                        }
                                    }
                                }
                            }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeColor,
                                focusedLabelColor = themeColor,
                                cursorColor = themeColor
                            ),
                            enabled = !isRedeeming
                        )

                        Spacer(Modifier.height(6.rdp))
                        Text(
                            "激活后立即生效 · 一卡一账号 · 不可转让",
                            fontSize = 11.rsp,
                            color = Color(0xFFBBBBBB)
                        )

                        if (inlineError != null) {
                            Spacer(Modifier.height(10.rdp))
                            Surface(
                                color = Color(0xFFFFEBEE),
                                shape = RoundedCornerShape(10.rdp)
                            ) {
                                Text(
                                    "⚠ $inlineError",
                                    modifier = Modifier.padding(horizontal = 12.rdp, vertical = 8.rdp),
                                    fontSize = 12.rsp,
                                    color = Color(0xFFC62828)
                                )
                            }
                        }

                        if (successFlash) {
                            Spacer(Modifier.height(8.rdp))
                            Text(
                                "✓ 激活成功，额度已刷新",
                                fontSize = 12.rsp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(Modifier.height(12.rdp))
                        ChatAiRetailTiersSection(themeColor = themeColor)

                        Spacer(Modifier.height(20.rdp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("关闭", color = Color(0xFF999999), fontSize = 14.rsp)
                            }
                            Spacer(Modifier.width(8.rdp))
                            Button(
                                onClick = {
                                    viewModel.redeemChatAiCard(cardCode) { ok, msg ->
                                        if (ok) {
                                            cardCode = ""
                                            inlineError = null
                                            successFlash = true
                                        } else {
                                            inlineError = msg
                                            successFlash = false
                                        }
                                    }
                                },
                                enabled = cardCode.isNotBlank() && !isRedeeming,
                                shape = RoundedCornerShape(14.rdp),
                                modifier = Modifier.height(44.rdp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    disabledContainerColor = Color(0xFFE0E0E0)
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (cardCode.isNotBlank() && !isRedeeming) {
                                                Brush.linearGradient(
                                                    listOf(AiPurple, themeColor, AiPink.copy(alpha = 0.9f))
                                                )
                                            } else {
                                                Brush.linearGradient(
                                                    listOf(Color(0xFFE0E0E0), Color(0xFFE0E0E0))
                                                )
                                            },
                                            RoundedCornerShape(14.rdp)
                                        )
                                        .padding(horizontal = 28.rdp, vertical = 10.rdp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isRedeeming) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.rdp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Rounded.Bolt,
                                                null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.rdp)
                                            )
                                            Spacer(Modifier.width(6.rdp))
                                            Text(
                                                "激活",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.rsp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatAiStatusCard(
    entitlement: ChatAiEntitlementUi,
    themeColor: Color,
) {
    val pres = statusPresentation(entitlement, themeColor)
    val pulse = rememberInfiniteTransition(label = "pulse")
    val glow by pulse.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.rdp))
            .background(pres.bg)
            .border(1.dp, pres.accent.copy(alpha = 0.25f), RoundedCornerShape(16.rdp))
            .padding(14.rdp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.rdp)
                        .clip(CircleShape)
                        .background(pres.accent.copy(alpha = glow))
                )
                Spacer(Modifier.width(8.rdp))
                Text(pres.title, fontSize = 14.rsp, fontWeight = FontWeight.SemiBold, color = pres.accent)
                if (entitlement.sourceLabel != null) {
                    Spacer(Modifier.weight(1f))
                    Surface(
                        color = pres.accent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.rdp)
                    ) {
                        Text(
                            entitlement.sourceLabel,
                            modifier = Modifier.padding(horizontal = 8.rdp, vertical = 3.rdp),
                            fontSize = 10.rsp,
                            color = pres.accent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.rdp))
            Text(pres.subtitle, fontSize = 13.rsp, color = Color(0xFF555555), lineHeight = 18.rsp)

            if (entitlement.dailyLimit > 0 && entitlement.progress != null) {
                Spacer(Modifier.height(10.rdp))
                LinearProgressIndicator(
                    progress = entitlement.progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.rdp)
                        .clip(RoundedCornerShape(4.rdp)),
                    color = pres.accent,
                    trackColor = pres.accent.copy(alpha = 0.15f),
                )
                Spacer(Modifier.height(4.rdp))
                Text(
                    "今日 ${entitlement.usedToday} / ${entitlement.dailyLimit} 条",
                    fontSize = 11.rsp,
                    color = Color(0xFF777777)
                )
            } else if (entitlement.state == ChatAiBarState.TRIAL && entitlement.trialRemaining != null) {
                Spacer(Modifier.height(10.rdp))
                Text(
                    "体验剩余 ${entitlement.trialRemaining} / ${ChatAiLimits.TRIAL_TOTAL} 条",
                    fontSize = 11.rsp,
                    color = Color(0xFF777777)
                )
            }
        }
    }
}

@Composable
private fun ChatAiRetailTiersSection(themeColor: Color) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        color = themeColor.copy(alpha = 0.06f),
        shape = RoundedCornerShape(12.rdp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.rdp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (expanded) "▼ 套餐与购买说明" else "▶ 套餐与购买说明",
                    fontSize = 12.rsp,
                    fontWeight = FontWeight.SemiBold,
                    color = themeColor
                )
            }
            if (expanded) {
                Spacer(Modifier.height(8.rdp))
                ChatAiSku.retailTiers.forEach { tier ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.rdp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tier.title, fontSize = 12.rsp, fontWeight = FontWeight.Medium, color = Color(0xFF444444))
                            Text(tier.quotaLabel, fontSize = 10.rsp, color = Color(0xFF888888))
                        }
                        Text(tier.priceLabel, fontSize = 12.rsp, fontWeight = FontWeight.Bold, color = themeColor)
                    }
                }
                Spacer(Modifier.height(6.rdp))
                Text(
                    "购买后在此输入卡密激活。${ChatAiBilling.purchaseUnavailableHint()}",
                    fontSize = 10.rsp,
                    color = Color(0xFF999999),
                    lineHeight = 14.rsp
                )
            }
        }
    }
}

private data class StatusPresentation(
    val accent: Color,
    val bg: Brush,
    val title: String,
    val subtitle: String,
)

private fun statusPresentation(
    e: ChatAiEntitlementUi,
    themeColor: Color,
): StatusPresentation {
    val expireSuffix = e.expireDate?.let { " · 至 $it" }.orEmpty()
    val monthSuffix = if (e.monthlyLimit > 0) " · 本月 ${e.usedMonth}/${e.monthlyLimit}" else ""
    return when (e.state) {
        ChatAiBarState.TRIAL -> StatusPresentation(
            accent = Color(0xFF1565C0),
            bg = Brush.linearGradient(
                listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB).copy(alpha = 0.5f))
            ),
            title = "◐ ${e.packageName ?: "体验包"} · 体验中",
            subtitle = "剩余 ${e.trialRemaining ?: 0} / ${ChatAiLimits.TRIAL_TOTAL} 条$expireSuffix"
        )
        ChatAiBarState.ACTIVE_CARD -> StatusPresentation(
            accent = Color(0xFF2E7D32),
            bg = Brush.linearGradient(
                listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9).copy(alpha = 0.5f))
            ),
            title = "● ${e.packageName ?: "AI 套餐"} · 生效中",
            subtitle = "今日 ${e.usedToday} / ${e.dailyLimit} 条$monthSuffix$expireSuffix"
        )
        ChatAiBarState.ACTIVE_VIP -> StatusPresentation(
            accent = themeColor,
            bg = Brush.linearGradient(
                listOf(themeColor.copy(alpha = 0.12f), AiPurple.copy(alpha = 0.08f))
            ),
            title = "● ${e.packageName ?: "VIP"} · AI 额度已开通",
            subtitle = "今日 ${e.usedToday} / ${e.dailyLimit} 条$monthSuffix$expireSuffix"
        )
        ChatAiBarState.ACTIVE_BOTH -> StatusPresentation(
            accent = themeColor,
            bg = Brush.linearGradient(
                listOf(themeColor.copy(alpha = 0.12f), Color(0xFFE8F5E9).copy(alpha = 0.6f))
            ),
            title = "● 当前按 ${e.packageName ?: "VIP"} 计费",
            subtitle = "今日 ${e.usedToday} / ${e.dailyLimit} 条$monthSuffix$expireSuffix"
        )
        ChatAiBarState.EXHAUSTED_DAY -> StatusPresentation(
            accent = Color(0xFFE65100),
            bg = Brush.linearGradient(
                listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2).copy(alpha = 0.5f))
            ),
            title = "⚠ 今日额度已用完",
            subtitle = "${e.usedToday} / ${e.dailyLimit} · 明日恢复 · 或升级套餐"
        )
        ChatAiBarState.EXHAUSTED_MONTH -> StatusPresentation(
            accent = Color(0xFFE65100),
            bg = Brush.linearGradient(
                listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2).copy(alpha = 0.5f))
            ),
            title = "⚠ 本月额度已用完",
            subtitle = "本月 ${e.usedMonth} / ${e.monthlyLimit} · 下月恢复"
        )
        ChatAiBarState.EXPIRED -> StatusPresentation(
            accent = Color(0xFFE65100),
            bg = Brush.linearGradient(
                listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2).copy(alpha = 0.4f))
            ),
            title = "○ 套餐已过期",
            subtitle = "仅本地回复 · 请激活新卡密"
        )
        else -> StatusPresentation(
            accent = Color(0xFFFF9800),
            bg = Brush.linearGradient(
                listOf(Color(0xFFFFF8E1), Color(0xFFFFECB3).copy(alpha = 0.45f))
            ),
            title = "○ 未激活",
            subtitle = "本地智能回复可用 · 激活卡密解锁云端 AI"
        )
    }
}
