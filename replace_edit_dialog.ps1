$file = "app/src/main/java/com/example/funlife/ui/screens/EnhancedSpinWheelScreen.kt"
$lines = Get-Content $file -Encoding UTF8

# Find the start line of EditOptionsDialog function (around line 1750)
$startLine = -1
$endLine = -1
$braceCount = 0
$inFunction = $false

for ($i = 0; $i -lt $lines.Count; $i++) {
    if ($i -gt 1745 -and $lines[$i] -match '@Composable' -and $i -lt 1760) {
        if ($i + 1 -lt $lines.Count -and $lines[$i + 1] -match 'fun EditOptionsDialog') {
            $startLine = $i
            $inFunction = $true
            Write-Host "Found start at line $($i + 1)"
            break
        }
    }
}

if ($startLine -ge 0) {
    # Count braces to find the end
    for ($i = $startLine; $i -lt $lines.Count; $i++) {
        $braceCount += ($lines[$i] -split '\{').Count - 1
        $braceCount -= ($lines[$i] -split '\}').Count - 1
        
        if ($braceCount -eq 0 -and $i -gt $startLine + 10) {
            $endLine = $i
            Write-Host "Found end at line $($i + 1)"
            break
        }
    }
}

if ($startLine -ge 0 -and $endLine -ge 0) {
    Write-Host "Replacing lines $($startLine + 1) to $($endLine + 1)"
    
    # New enhanced dialog code
    $newCode = @'
@Composable
fun EditOptionsDialog(
    currentOptions: List<WheelOption>,
    onOptionsUpdated: (List<WheelOption>) -> Unit,
    onDismiss: () -> Unit
) {
    var editableOptions by remember { 
        mutableStateOf(currentOptions.toMutableList())
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🎯 编辑转盘选项",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "当前: ${editableOptions.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (editableOptions.size in 2..12) {
                        Color(0xFF4CAF50)
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 使用说明卡片
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE3F2FD)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "💡 使用说明",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                        Text(
                            "• 点击箭头展开/折叠选项详情",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF424242)
                        )
                        Text(
                            "• 调整权重滑块改变中奖概率",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF424242)
                        )
                        Text(
                            "• 开启排除开关临时禁用选项",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF424242)
                        )
                    }
                }
                
                // 选项列表
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    editableOptions.forEachIndexed { index, option ->
                        EnhancedOptionItemInline(
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
                }
                
                // 添加新选项按钮
                if (editableOptions.size < 12) {
                    Button(
                        onClick = {
                            editableOptions = editableOptions.toMutableList().apply {
                                add(WheelOption(text = "", weight = 1, isExcluded = false))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("添加新选项", fontWeight = FontWeight.Bold)
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6F00)
                )
            ) {
                Text("保存", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
'@
    
    $newLines = $lines[0..($startLine - 1)] + $newCode.Split("`n") + $lines[($endLine + 1)..($lines.Count - 1)]
    $newLines | Set-Content $file -Encoding UTF8
    Write-Host "Successfully replaced EditOptionsDialog function"
} else {
    Write-Host "Could not find function boundaries"
    Write-Host "startLine: $startLine, endLine: $endLine"
}
