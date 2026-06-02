// AvatarUploader.kt - 头像上传组件
package com.example.funlife.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.style.TextOverflow
import com.example.funlife.ui.utils.ResponsiveDialogBox
import com.example.funlife.ui.utils.Spacing
import com.example.funlife.ui.utils.Radius
import com.example.funlife.ui.utils.TextSize
import com.example.funlife.ui.utils.IconSize
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * 头像上传器组件
 * 上传图标显示在圆圈中间
 */
@Composable
fun AvatarUploader(
    currentAvatarUri: String?,
    onAvatarSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var isHovered by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .size(100.dp)
            .clip(CircleShape)
            .clickable { showDialog = true },
        contentAlignment = Alignment.Center
    ) {
        if (currentAvatarUri != null) {
            // 显示当前头像
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(currentAvatarUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "头像",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // 半透明遮罩层，显示编辑提示
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "更换头像",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(32.dp)
                )
            }
        } else {
            // 默认头像 - 显示上传图标在中间
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFE8EAF6),
                                Color(0xFFC5CAE9),
                                Color(0xFF9FA8DA)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.AddAPhoto,
                        contentDescription = "上传头像",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        "上传",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
    
    if (showDialog) {
        AvatarUploadDialog(
            onDismiss = { showDialog = false },
            onAvatarSelected = { uri ->
                onAvatarSelected(uri)
                showDialog = false
            }
        )
    }
}

/**
 * 头像上传对话框
 */
@Composable
fun AvatarUploadDialog(
    onDismiss: () -> Unit,
    onAvatarSelected: (Uri) -> Unit
) {
    val context = LocalContext.current
    
    // 相册选择器
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onAvatarSelected(it) }
    }
    
    // 相机拍照
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        // TODO: 将bitmap保存为文件并返回Uri
        // 这里需要实现bitmap到Uri的转换
    }
    
    // 🔥 关键：usePlatformDefaultWidth=false 取消系统强制窄宽，配合 ResponsiveDialogBox 自适应限宽
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
                Column(
                    modifier = Modifier.padding(
                        horizontal = Spacing.lg,
                        vertical = Spacing.lg
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Text(
                        "选择头像",
                        fontSize = TextSize.headline,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )

                    Divider(color = Color(0xFFF0F0F0))

                    AvatarUploadOption(
                        icon = Icons.Default.PhotoLibrary,
                        title = "从相册选择",
                        subtitle = "选择已有照片",
                        color = Color(0xFF4CAF50),
                        onClick = { galleryLauncher.launch("image/*") }
                    )

                    AvatarUploadOption(
                        icon = Icons.Default.CameraAlt,
                        title = "拍照",
                        subtitle = "使用相机拍摄",
                        color = Color(0xFF2196F3),
                        onClick = { cameraLauncher.launch(null) }
                    )

                    AvatarUploadOption(
                        icon = Icons.Default.Person,
                        title = "使用默认头像",
                        subtitle = "恢复默认样式",
                        color = Color(0xFF9E9E9E),
                        onClick = { onDismiss() }
                    )

                    Spacer(Modifier.height(Spacing.xs))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "取消",
                            color = Color(0xFF95A5A6),
                            fontSize = TextSize.md
                        )
                    }
                }
            }
        }
    }
}

/**
 * 头像上传选项
 */
@Composable
fun AvatarUploadOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .clickable(onClick = onClick)
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = Spacing.md, vertical = Spacing.sm + 4.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color.copy(alpha = 0.22f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(IconSize.lg)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                title,
                fontSize = TextSize.md,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2C3E50),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                fontSize = TextSize.sm,
                color = Color(0xFF95A5A6),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFBDC3C7),
            modifier = Modifier.size(IconSize.md)
        )
    }
}

/**
 * 头像裁剪对话框（简化版）
 * TODO: 实现完整的图片裁剪功能
 */
@Composable
fun AvatarCropDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onCropped: (Uri) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "裁剪头像",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // TODO: 添加图片裁剪UI
                AsyncImage(
                    model = imageUri,
                    contentDescription = "待裁剪图片",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    
                    Button(
                        onClick = { onCropped(imageUri) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}
