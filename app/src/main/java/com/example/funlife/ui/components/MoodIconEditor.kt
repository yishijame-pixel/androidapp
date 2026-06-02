// ═══════════════════════════════════════════════════════════════════════════
// MoodIconEditor.kt
// 心情图标管理界面（对话框形式）：
//   - 网格展示所有图标（含 14 内置 + 用户自定义）
//   - 点 + 号上传图片新增
//   - 点已有项进入编辑（改 label/level/color，自定义项可改图、可删除）
//   - 点"重置"恢复默认（清理用户文件）
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.funlife.data.MoodIcon
import com.example.funlife.data.MoodIconStore
import com.example.funlife.ui.utils.Radius
import com.example.funlife.ui.utils.ResponsiveDialogBox
import com.example.funlife.ui.utils.Spacing
import com.example.funlife.ui.utils.TextSize
import com.example.funlife.ui.utils.rdp
import com.example.funlife.ui.utils.rsp
import java.io.File

private val ColorPalette: List<Long> = listOf(
    0xFFFF6F91, 0xFFFF8F4F, 0xFFFFA726, 0xFFFFB74D, 0xFFEF5350,
    0xFFEC407A, 0xFF7E57C2, 0xFFAB7B3F, 0xFF26A69A, 0xFF42A5F5,
    0xFF1E88E5, 0xFF66BB6A, 0xFF9575CD, 0xFF7986CB, 0xFF607D8B,
    0xFF9E9E9E
)

/**
 * 心情图标管理对话框入口。
 * @param userId 当前用户 id
 * @param onChanged 任何修改后回调（让外层重新拉取列表 / 触发 recompose）
 */
@Composable
fun MoodIconManagerDialog(
    userId: Long,
    onDismiss: () -> Unit,
    onChanged: () -> Unit
) {
    val ctx = LocalContext.current
    var tick by remember { mutableStateOf(0) }
    val icons = remember(tick, userId) { MoodIconStore.getAll(ctx, userId) }

    // 编辑中的图标（null = 没在编辑；新建时是个临时占位）
    var editing by remember { mutableStateOf<MoodIcon?>(null) }

    // 选图 launcher：选好后直接弹"新增"编辑器
    var newImagePath by remember { mutableStateOf<String?>(null) }
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val saved = saveCustomMoodIcon(ctx, uri, userId)
            if (saved != null) {
                newImagePath = saved
                // 触发新建编辑器
                editing = MoodIcon(
                    id = "__new__",
                    value = saved,
                    label = "我的心情",
                    level = 3,
                    color = ColorPalette.random(),
                    isCustom = true
                )
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ResponsiveDialogBox {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radius.xxl),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7FA))
            ) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    // 标题
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎨", fontSize = 20.rsp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "管理心情图标",
                            fontSize = TextSize.headline,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF3D1F2C)
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = {
                            // 重置：先清掉用户的图片文件，再清存储
                            icons.filter { it.isCustom }.forEach {
                                deleteCustomMoodIconFile(ctx, it.value, userId)
                            }
                            MoodIconStore.resetToDefaults(ctx, userId)
                            tick++
                            onChanged()
                        }) {
                            Text("重置", color = Color(0xFFE53935), fontSize = TextSize.sm, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        "点 + 上传图片创建专属心情；点已有项可改名称、等级、颜色",
                        fontSize = TextSize.tiny,
                        color = Color(0xFF8B5670)
                    )

                    Spacer(Modifier.height(Spacing.md))

                    // 网格
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(icons, key = { it.id }) { ic ->
                            IconTile(
                                icon = ic,
                                onClick = { editing = ic }
                            )
                        }
                        // 末尾的 + 上传
                        item(key = "__add__") {
                            AddTile(onClick = {
                                pickImageLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            })
                        }
                    }

                    Spacer(Modifier.height(Spacing.md))
                    TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                        Text("完成", color = Color(0xFFFF6F91), fontSize = TextSize.md, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }

    // 编辑/新建 对话框
    editing?.let { target ->
        MoodIconEditDialog(
            initial = target,
            isNew = target.id == "__new__",
            onDismiss = {
                // 新建时若用户取消，清理已复制的文件
                if (target.id == "__new__" && newImagePath != null) {
                    deleteCustomMoodIconFile(ctx, newImagePath!!, userId)
                }
                newImagePath = null
                editing = null
            },
            onSave = { label, level, color ->
                if (target.id == "__new__") {
                    MoodIconStore.addCustom(
                        ctx, userId,
                        imagePath = target.value,
                        label = label,
                        level = level,
                        color = color
                    )
                } else {
                    MoodIconStore.update(ctx, userId, target.id, label, level, color)
                }
                newImagePath = null
                editing = null
                tick++
                onChanged()
            },
            onDelete = if (target.isCustom && target.id != "__new__") {
                {
                    MoodIconStore.deleteCustom(ctx, userId, target.id)
                    deleteCustomMoodIconFile(ctx, target.value, userId)
                    editing = null
                    tick++
                    onChanged()
                }
            } else null
        )
    }
}

@Composable
private fun IconTile(icon: MoodIcon, onClick: () -> Unit) {
    val c = Color(icon.color)
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, c.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.rdp)
                .clip(CircleShape)
                .background(c.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            MoodIconView(icon = icon, iconSize = 44.rdp, emojiFontSize = 22.rsp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            icon.label,
            color = c,
            fontSize = TextSize.tiny,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "Lv ${icon.level}${if (icon.isCustom) " · 自定义" else ""}",
            color = Color(0xFF9CA3AF),
            fontSize = 9.rsp,
            maxLines = 1
        )
    }
}

@Composable
private fun AddTile(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFFFFE5EC), Color(0xFFFCE4EC)))
            )
            .border(1.5.dp, Color(0xFFFF6F91).copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.rdp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Text("＋", color = Color(0xFFFF6F91), fontSize = 26.rsp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(4.dp))
        Text("上传", color = Color(0xFFFF6F91), fontSize = TextSize.tiny, fontWeight = FontWeight.Black)
        Text("图片", color = Color(0xFFFF6F91).copy(alpha = 0.6f), fontSize = 9.rsp)
    }
}

/**
 * 单个图标编辑对话框：改名、改等级、改颜色；自定义项支持删除。
 */
@Composable
private fun MoodIconEditDialog(
    initial: MoodIcon,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (label: String, level: Int, color: Long) -> Unit,
    onDelete: (() -> Unit)?
) {
    var label by remember { mutableStateOf(initial.label) }
    var level by remember { mutableStateOf(initial.level) }
    var color by remember { mutableStateOf(initial.color) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ResponsiveDialogBox {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radius.xxl),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    // 预览
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(color).copy(alpha = 0.15f))
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.rdp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                MoodIconView(
                                    icon = initial.copy(color = color, label = label, level = level),
                                    iconSize = 64.rdp,
                                    emojiFontSize = 36.rsp
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                label.ifBlank { "未命名" },
                                fontSize = TextSize.md,
                                fontWeight = FontWeight.Black,
                                color = Color(color)
                            )
                            Text("Lv $level", fontSize = TextSize.tiny, color = Color(0xFF6B7280))
                        }
                    }

                    Spacer(Modifier.height(Spacing.md))

                    // 标签输入
                    Text("名称", fontSize = TextSize.sm, fontWeight = FontWeight.Black, color = Color(0xFF3D1F2C))
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.pill))
                            .background(Color(0xFFFAF7F8))
                            .border(1.dp, Color(0xFFEFEAEE), RoundedCornerShape(Radius.pill))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        BasicTextField(
                            value = label,
                            onValueChange = { if (it.length <= 10) label = it },
                            singleLine = true,
                            cursorBrush = SolidColor(Color(color)),
                            textStyle = TextStyle(color = Color(0xFF1F2937), fontSize = TextSize.sm),
                            decorationBox = { inner ->
                                if (label.isEmpty()) Text("起个名字…", color = Color(0xFF9CA3AF), fontSize = TextSize.sm)
                                inner()
                            }
                        )
                    }

                    Spacer(Modifier.height(Spacing.md))

                    // 等级
                    Text("心情等级", fontSize = TextSize.sm, fontWeight = FontWeight.Black, color = Color(0xFF3D1F2C))
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..5).forEach { lv ->
                            val active = level == lv
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.rdp)
                                    .clip(RoundedCornerShape(Radius.pill))
                                    .background(if (active) Color(color) else Color(0xFFFAF7F8))
                                    .border(
                                        1.dp,
                                        if (active) Color.Transparent else Color(0xFFEFEAEE),
                                        RoundedCornerShape(Radius.pill)
                                    )
                                    .clickable { level = lv },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$lv",
                                    color = if (active) Color.White else Color(0xFF6B7280),
                                    fontWeight = FontWeight.Black,
                                    fontSize = TextSize.sm
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(Spacing.md))

                    // 颜色
                    Text("主题色", fontSize = TextSize.sm, fontWeight = FontWeight.Black, color = Color(0xFF3D1F2C))
                    Spacer(Modifier.height(6.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(8),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.heightIn(max = 120.dp)
                    ) {
                        items(ColorPalette) { c ->
                            val active = c == color
                            Box(
                                modifier = Modifier
                                    .size(32.rdp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                                    .border(
                                        if (active) 3.dp else 1.dp,
                                        if (active) Color(0xFF1F2937) else Color.White,
                                        CircleShape
                                    )
                                    .clickable { color = c }
                            )
                        }
                    }

                    Spacer(Modifier.height(Spacing.lg))

                    // 操作按钮
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (onDelete != null) {
                            TextButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                                Text("删除", color = Color(0xFFE53935), fontWeight = FontWeight.Black)
                            }
                        }
                        TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text("取消", color = Color(0xFF9CA3AF), fontWeight = FontWeight.Black)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.rdp)
                                .clip(RoundedCornerShape(Radius.pill))
                                .background(Color(color))
                                .clickable { onSave(label.ifBlank { "心情" }, level, color) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (isNew) "添加" else "保存",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = TextSize.md
                            )
                        }
                    }
                }
            }
        }
    }
}
