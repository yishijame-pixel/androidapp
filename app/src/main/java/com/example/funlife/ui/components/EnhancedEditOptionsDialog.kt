package com.example.funlife.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.data.model.WheelOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedEditOptionsDialog(
    currentOptions: List<WheelOption>,
    onOptionsUpdated: (List<WheelOption>) -> Unit,
    onDismiss: () -> Unit
) {
    var editableOptions by remember { 
        mutableStateOf(currentOptions.toMutableList())
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFAFAFA),
        shape = RoundedCornerShape(24.dp),
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "编辑选项",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color(0xFF1F1F1F)
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF667EEA).copy(alpha = 0.1f)
                ) {
                    Text(
                        "${editableOptions.size}/12",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFF667EEA)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                editableOptions.forEachIndexed { index, option ->
                    ModernOptionCard(
                        index = index,
                        option = option,
                        onTextChange = { newText ->
                            editableOptions = editableOptions.toMutableList().apply {
                                this[index] = option.copy(text = newText)
                            }
                        },
                        onWeightChange = { newWeight ->
                            editableOptions = editableOptions.toMutableList().apply {
                                this[index] = option.copy(weight = newWeight)
                            }
                        },
                        onExcludedChange = { isExcluded ->
                            editableOptions = editableOptions.toMutableList().apply {
                                this[index] = option.copy(isExcluded = isExcluded)
                            }
                        },
                        onDelete = {
                            if (editableOptions.size > 2) {
                                editableOptions = editableOptions.toMutableList().apply {
                                    removeAt(index)
                                }
                            }
                        },
                        canDelete = editableOptions.size > 2
                    )
                }
                
                // 添加按钮
                if (editableOptions.size < 12) {
                    OutlinedButton(
                        onClick = {
                            editableOptions = editableOptions.toMutableList().apply {
                                add(WheelOption(text = "", weight = 1, isExcluded = false))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF667EEA)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF667EEA)
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "添加选项",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val validOptions = editableOptions
                        .filter { it.text.isNotBlank() }
                        .distinctBy { it.text }
                    
                    if (validOptions.size >= 2) {
                        onOptionsUpdated(validOptions)
                    }
                },
                enabled = editableOptions.filter { it.text.isNotBlank() }.distinctBy { it.text }.size >= 2,
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6B6B),
                    disabledContainerColor = Color(0xFFBDBDBD)
                )
            ) {
                Text(
                    "保存",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    "取消",
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = Color(0xFF757575)
                )
            }
        }
    )
}

@Composable
private fun ModernOptionCard(
    index: Int,
    option: WheelOption,
    onTextChange: (String) -> Unit,
    onWeightChange: (Int) -> Unit,
    onExcludedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (option.isExcluded) {
                Color(0xFFFFF4E6)
            } else {
                Color.White
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // 主行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 序号
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF667EEA)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${index + 1}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                
                // 输入框
                OutlinedTextField(
                    value = option.text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { 
                        Text(
                            "输入选项",
                            fontSize = 14.sp,
                            color = Color(0xFFBDBDBD)
                        ) 
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF667EEA),
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedContainerColor = Color(0xFFFAFAFA),
                        unfocusedContainerColor = Color(0xFFFAFAFA)
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                
                // 展开按钮
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF667EEA),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // 删除按钮
                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            // 展开内容
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Divider(color = Color(0xFFE0E0E0))
                    
                    // 权重
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFA726),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "权重",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF424242)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF667EEA).copy(alpha = 0.1f)
                            ) {
                                Text(
                                    "${option.weight}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF667EEA)
                                )
                            }
                        }
                        Slider(
                            value = option.weight.toFloat(),
                            onValueChange = { onWeightChange(it.toInt()) },
                            valueRange = 1f..10f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF667EEA),
                                activeTrackColor = Color(0xFF667EEA),
                                inactiveTrackColor = Color(0xFFE0E0E0)
                            )
                        )
                    }
                    
                    // 排除开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = if (option.isExcluded) Color(0xFFFF6B6B) else Color(0xFF9E9E9E),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "排除此选项",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF424242)
                            )
                        }
                        Switch(
                            checked = option.isExcluded,
                            onCheckedChange = onExcludedChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFFF6B6B),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFBDBDBD)
                            )
                        )
                    }
                }
            }
        }
    }
}
