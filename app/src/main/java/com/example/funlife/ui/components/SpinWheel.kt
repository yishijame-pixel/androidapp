// SpinWheel.kt - 转盘组件（完全重写版）
package com.example.funlife.ui.components

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import com.example.funlife.data.model.SpinWheelMode
import com.example.funlife.data.model.WheelOption
import com.example.funlife.data.model.WheelTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val TAG = "SpinWheel"

/**
 * 简化版转盘
 */
@Composable
fun SpinWheel(
    options: List<String>,
    onResult: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SpinWheel(
        options = options,
        mode = "NORMAL",
        canSpin = true,
        onResult = onResult,
        modifier = modifier
    )
}

/**
 * 完整版转盘
 */
@Composable
fun SpinWheel(
    options: List<String>,
    mode: String,
    canSpin: Boolean,
    weights: List<Int> = emptyList(),
    showWeightVisualization: Boolean = false,
    theme: WheelTheme? = null,
    multiSpinMode: Boolean = false,
    autoSpinTrigger: Int = 0,
    forceResult: String? = null,
    showButton: Boolean = true,
    onSpinStart: () -> Unit = {},
    onAutoSpinStart: () -> Unit = {},
    onResult: (String) -> Unit = {},
    onMultiSpinComplete: () -> Unit = {},
    onShowResult: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Log.d(TAG, "=== SpinWheel Recomposed ===")
    Log.d(TAG, "Options count: ${options.size}")
    Log.d(TAG, "Options: ${options.joinToString(", ")}")
    Log.d(TAG, "Mode: $mode, CanSpin: $canSpin, MultiSpin: $multiSpinMode")
    Log.d(TAG, "Weights: ${weights.joinToString(", ")}")
    Log.d(TAG, "ForceResult: $forceResult")
    Log.d(TAG, "AutoSpinTrigger: $autoSpinTrigger")
    
    val scope = rememberCoroutineScope()
    var isSpinning by remember { mutableStateOf(false) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var selectedResult by remember { mutableStateOf("") }
    
    // 监听autoSpinTrigger变化，自动触发旋转
    LaunchedEffect(autoSpinTrigger) {
        if (autoSpinTrigger > 0 && !isSpinning && options.isNotEmpty()) {
            Log.d(TAG, "=== AUTO SPIN TRIGGERED ===")
            Log.d(TAG, "autoSpinTrigger: $autoSpinTrigger")
            Log.d(TAG, "forceResult: $forceResult")
            
            onSpinStart()
            
            try {
                isSpinning = true
                
                // 如果有强制结果，计算目标旋转角度
                val targetRotation = if (forceResult != null && options.contains(forceResult)) {
                    Log.d(TAG, "=== FORCE RESULT DETECTED ===")
                    val targetIndex = options.indexOf(forceResult)
                    val anglePerOption = 360f / options.size
                    
                    val targetCenterAngleInWheel = targetIndex * anglePerOption + anglePerOption / 2 - 90f
                    val randomOffset = (Random.nextFloat() - 0.5f) * anglePerOption * 0.8f
                    val targetAngleWithRandom = targetCenterAngleInWheel + randomOffset
                    val targetRotationAngle = (270f - targetAngleWithRandom + 360) % 360f
                    
                    val currentNormalized = rotation % 360
                    val rotationDiff = (targetRotationAngle - currentNormalized + 360) % 360
                    val finalRotation = rotation + 360 * 3 + rotationDiff
                    
                    Log.d(TAG, "forceResult: $forceResult, targetIndex: $targetIndex, finalRotation: $finalRotation")
                    
                    finalRotation
                } else {
                    Log.d(TAG, "=== NO FORCE RESULT - Random Spin ===")
                    rotation + 360 * 3 + Random.nextFloat() * 360
                }
                
                // 使用更流畅的缓动函数和更短的时长
                val animDuration = if (multiSpinMode) 800 else 1800
                
                animate(
                    initialValue = rotation,
                    targetValue = targetRotation,
                    animationSpec = tween(animDuration, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f))
                ) { value, _ -> 
                    rotation = value
                }
                
                rotation = targetRotation % 360
                
                // 计算最终结果
                val anglePerOption = 360f / options.size
                val normalizedRotation = rotation % 360
                
                var resultIndex = 0
                options.indices.forEach { i ->
                    val optionStartInWheel = i * anglePerOption - 90f
                    val optionEndInWheel = (i + 1) * anglePerOption - 90f
                    
                    var optionStartAfterRotation = (optionStartInWheel + normalizedRotation) % 360
                    var optionEndAfterRotation = (optionEndInWheel + normalizedRotation) % 360
                    
                    if (optionStartAfterRotation < 0) optionStartAfterRotation += 360
                    if (optionEndAfterRotation < 0) optionEndAfterRotation += 360
                    
                    val pointerAngle = 270f
                    val isInRange = if (optionStartAfterRotation < optionEndAfterRotation) {
                        pointerAngle >= optionStartAfterRotation && pointerAngle < optionEndAfterRotation
                    } else {
                        pointerAngle >= optionStartAfterRotation || pointerAngle < optionEndAfterRotation
                    }
                    
                    if (isInRange) {
                        resultIndex = i
                    }
                }
                
                selectedResult = options[resultIndex]
                
                Log.d(TAG, "=== FINAL RESULT ===")
                Log.d(TAG, "Result: $selectedResult, Expected: $forceResult, Match: ${selectedResult == forceResult}")
                
                isSpinning = false
                onResult(selectedResult)
                // 只在非连抽模式下显示单次结果动画
                if (!multiSpinMode) {
                    onShowResult(selectedResult)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Spin error", e)
                isSpinning = false
            }
        }
    }
    
    val wheelOptions = remember(options, weights) {
        val opts = if (weights.isNotEmpty() && weights.size == options.size) {
            options.mapIndexed { index, text ->
                WheelOption(text = text, weight = weights[index])
            }
        } else {
            options.map { WheelOption(text = it) }
        }
        Log.d(TAG, "WheelOptions created: ${opts.size} items")
        opts.forEachIndexed { index, opt ->
            Log.d(TAG, "  [$index] ${opt.text} (weight: ${opt.weight})")
        }
        opts
    }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp) // 从32dp减少到16dp
    ) {
        // 移除顶部Spacer，直接开始转盘
        
        // 转盘容器 - 简洁设计
        Box(
            modifier = Modifier.size(280.dp), // 从320dp减小到280dp，适配小屏幕
            contentAlignment = Alignment.Center
        ) {
            // 转盘画布（会旋转）
            SpinWheelCanvas(
                options = wheelOptions,
                rotation = rotation,
                mode = mode,
                showWeightVisualization = showWeightVisualization,
                modifier = Modifier.fillMaxSize()
            )
            
            // 指针（固定在中心圆盘位置，指向上方）
            PointerIndicator()
        }
        
        // 旋转按钮（减少间距）- 可选显示
        if (showButton) {
            Button(
            onClick = {
                Log.d(TAG, "=== Button Clicked ===")
                Log.d(TAG, "isSpinning: $isSpinning")
                Log.d(TAG, "canSpin: $canSpin")
                Log.d(TAG, "multiSpinMode: $multiSpinMode")
                Log.d(TAG, "options.size: ${options.size}")
                Log.d(TAG, "options.isEmpty: ${options.isEmpty()}")
                Log.d(TAG, "mode: $mode")
                Log.d(TAG, "Button enabled: ${!isSpinning && canSpin && options.isNotEmpty()}")
                
                if (!isSpinning && options.isNotEmpty()) {
                    Log.d(TAG, "=== BUTTON CLICK - Starting Spin ===")
                    Log.d(TAG, "forceResult parameter: $forceResult")
                    Log.d(TAG, "options: ${options.joinToString(", ")}")
                    
                    onSpinStart()
                    
                    // 🔥 修复：使用 Compose 协程作用域
                    scope.launch {
                        try {
                            Log.d(TAG, "=== Inside Coroutine ===")
                            Log.d(TAG, "forceResult at coroutine start: $forceResult")
                            isSpinning = true
                            
                            // 如果有强制结果，计算目标旋转角度
                            val targetRotation = if (forceResult != null && options.contains(forceResult)) {
                                Log.d(TAG, "=== FORCE RESULT DETECTED ===")
                                val targetIndex = options.indexOf(forceResult)
                                val anglePerOption = 360f / options.size
                                
                                // 关键：转盘绘制时，第i个选项的startAngle = i * anglePerOption - 90
                                // 所以第i个选项在转盘上的实际角度范围是：
                                // 起始：i * anglePerOption - 90
                                // 结束：(i+1) * anglePerOption - 90
                                // 中心：i * anglePerOption + anglePerOption/2 - 90
                                
                                val targetCenterAngleInWheel = targetIndex * anglePerOption + anglePerOption / 2 - 90f
                                
                                // 在目标选项范围内随机选择一个位置
                                val randomOffset = (Random.nextFloat() - 0.5f) * anglePerOption * 0.8f
                                val targetAngleWithRandom = targetCenterAngleInWheel + randomOffset
                                
                                // 指针在-90度（270度），我们需要旋转多少度让目标角度对准指针
                                // 转盘旋转后，目标角度变成：targetAngleWithRandom + rotation
                                // 我们要让它等于 -90（即270），所以：
                                // targetAngleWithRandom + rotation = 270
                                // rotation = 270 - targetAngleWithRandom
                                
                                val targetRotationAngle = (270f - targetAngleWithRandom + 360) % 360f
                                
                                // 从当前角度旋转到目标角度，加上5圈
                                val currentNormalized = rotation % 360
                                val rotationDiff = (targetRotationAngle - currentNormalized + 360) % 360
                                val finalRotation = rotation + 360 * 3 + rotationDiff
                                
                                Log.d(TAG, "=== Force Result Calculation ===")
                                Log.d(TAG, "forceResult: $forceResult")
                                Log.d(TAG, "targetIndex: $targetIndex")
                                Log.d(TAG, "anglePerOption: $anglePerOption")
                                Log.d(TAG, "targetCenterAngleInWheel: $targetCenterAngleInWheel")
                                Log.d(TAG, "randomOffset: $randomOffset")
                                Log.d(TAG, "targetAngleWithRandom: $targetAngleWithRandom")
                                Log.d(TAG, "targetRotationAngle: $targetRotationAngle")
                                Log.d(TAG, "currentNormalized: $currentNormalized")
                                Log.d(TAG, "rotationDiff: $rotationDiff")
                                Log.d(TAG, "finalRotation: $finalRotation")
                                
                                finalRotation
                            } else {
                                Log.d(TAG, "=== NO FORCE RESULT - Random Spin ===")
                                Log.d(TAG, "forceResult is null or not in options")
                                // 正常随机旋转
                                rotation + 360 * 3 + Random.nextFloat() * 360
                            }
                            
                            Log.d(TAG, "Current rotation: $rotation, Target rotation: $targetRotation")
                            
                            // 使用更流畅的缓动函数和更短的时长
                            val animDuration = if (multiSpinMode) 800 else 1800
                            
                            animate(
                                initialValue = rotation,
                                targetValue = targetRotation,
                                animationSpec = tween(animDuration, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f))
                            ) { value, _ -> 
                                rotation = value
                            }
                            
                            rotation = targetRotation % 360
                            
                            // 计算最终结果 - 找出指针指向的选项
                            val anglePerOption = 360f / options.size
                            val normalizedRotation = rotation % 360
                            
                            Log.d(TAG, "=== Calculating Result ===")
                            Log.d(TAG, "Final rotation: $rotation")
                            Log.d(TAG, "Normalized rotation: $normalizedRotation")
                            
                            // 指针在 270 度（-90度），找出哪个选项包含这个角度
                            var resultIndex = 0
                            options.indices.forEach { i ->
                                // 第i个选项在转盘上的角度范围（绘制时）
                                val optionStartInWheel = i * anglePerOption - 90f
                                val optionEndInWheel = (i + 1) * anglePerOption - 90f
                                
                                // 旋转后，选项的角度范围
                                var optionStartAfterRotation = (optionStartInWheel + normalizedRotation) % 360
                                var optionEndAfterRotation = (optionEndInWheel + normalizedRotation) % 360
                                
                                // 标准化到0-360范围
                                if (optionStartAfterRotation < 0) optionStartAfterRotation += 360
                                if (optionEndAfterRotation < 0) optionEndAfterRotation += 360
                                
                                // 检查 270 度是否在这个选项的范围内
                                val pointerAngle = 270f
                                val isInRange = if (optionStartAfterRotation < optionEndAfterRotation) {
                                    // 正常情况：起始 < 结束
                                    pointerAngle >= optionStartAfterRotation && pointerAngle < optionEndAfterRotation
                                } else {
                                    // 跨越 0 度的情况
                                    pointerAngle >= optionStartAfterRotation || pointerAngle < optionEndAfterRotation
                                }
                                
                                Log.d(TAG, "Option $i (${options[i]}): start=$optionStartAfterRotation, end=$optionEndAfterRotation, isInRange=$isInRange")
                                
                                if (isInRange) {
                                    resultIndex = i
                                }
                            }
                            
                            selectedResult = options[resultIndex]
                            
                            Log.d(TAG, "=== FINAL RESULT ===")
                            Log.d(TAG, "Result index: $resultIndex")
                            Log.d(TAG, "Result: $selectedResult")
                            Log.d(TAG, "Expected (forceResult): $forceResult")
                            Log.d(TAG, "Match: ${selectedResult == forceResult}")
                            
                            isSpinning = false
                            onResult(selectedResult)
                            // 只在非连抽模式下显示单次结果动画
                            if (!multiSpinMode) {
                                onShowResult(selectedResult)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Spin error", e)
                            isSpinning = false
                        }
                    }
                } else {
                    Log.d(TAG, "Cannot spin - isSpinning: $isSpinning, options.isEmpty: ${options.isEmpty()}, canSpin: $canSpin")
                }
            },
            enabled = !isSpinning && canSpin && options.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = when (mode) {
                    "LUCKY" -> Color(0xFFFFD700)
                    "ADVANCED" -> Color(0xFF9C27B0)
                    else -> Color(0xFF2196F3)
                },
                disabledContainerColor = Color(0xFFBDBDBD)
            ),
            shape = RoundedCornerShape(30.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 6.dp,
                pressedElevation = 10.dp
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (isSpinning) "⏳" else "🎯", fontSize = 24.sp)
                Text(
                    if (isSpinning) "旋转中..." else "开始旋转",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        }
        // 移除底部Spacer
    }
}

/**
 * 转盘画布 - 专业美化版
 */
@Composable
private fun SpinWheelCanvas(
    options: List<WheelOption>,
    rotation: Float,
    mode: String,
    showWeightVisualization: Boolean,
    modifier: Modifier = Modifier
) {
    Log.d(TAG, "SpinWheelCanvas drawing - options: ${options.size}, rotation: $rotation")
    
    Canvas(modifier = modifier.rotate(rotation)) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension / 2 * 0.88f
        
        Log.d(TAG, "Canvas size: ${size.width} x ${size.height}, radius: $radius")
        
        if (options.isEmpty()) {
            Log.e(TAG, "No options to draw!")
            return@Canvas
        }
        
        val anglePerOption = 360f / options.size
        val totalWeight = options.sumOf { it.weight }
        
        Log.d(TAG, "Drawing ${options.size} sectors, anglePerOption: $anglePerOption, totalWeight: $totalWeight")
        
        // 外圈柔和阴影 - 简洁设计
        for (i in 0..2) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.12f - i * 0.04f),
                        Color.Transparent
                    ),
                    center = Offset(centerX + i * 0.5f, centerY + i * 0.5f),
                    radius = radius + 12f - i * 4f
                ),
                radius = radius + 12f - i * 4f,
                center = Offset(centerX + i * 0.5f, centerY + i * 0.5f)
            )
        }
        
        var currentAngle = 0f
        
        // 绘制扇形 - 增强版
        options.forEachIndexed { index, option ->
            val sweepAngle = if (showWeightVisualization && totalWeight > 0) {
                360f * option.weight / totalWeight
            } else {
                anglePerOption
            }
            
            Log.d(TAG, "Sector $index: '${option.text}' at angle $currentAngle, sweep $sweepAngle")
            
            // 精选配色方案 - 更鲜艳、更有活力
            val colorPalette = when (mode) {
                "LUCKY" -> listOf(
                    Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFFFB347), Color(0xFFFFE4B5),
                    Color(0xFFFFAA00), Color(0xFFFFCC33), Color(0xFFFFDD55), Color(0xFFFFEE88)
                )
                "ADVANCED" -> listOf(
                    Color(0xFFAB47BC), Color(0xFFBA68C8), Color(0xFF9C27B0), Color(0xFFCE93D8),
                    Color(0xFF8E24AA), Color(0xFFE1BEE7), Color(0xFF7B1FA2), Color(0xFFEA80FC)
                )
                else -> listOf(
                    Color(0xFFFF6B9D), // 粉红
                    Color(0xFFFFD93D), // 金黄
                    Color(0xFFAB47BC), // 紫色
                    Color(0xFF4CAF50), // 绿色
                    Color(0xFF2196F3), // 蓝色
                    Color(0xFFFF9800), // 橙色
                    Color(0xFF00BCD4), // 青色
                    Color(0xFFFF5252)  // 红色
                )
            }
            
            val baseColor = colorPalette[index % colorPalette.size]
            
            Log.d(TAG, "  Color: ${baseColor.value.toString(16)}")
            
            // 扇形主体 - 纯色简洁设计
            drawArc(
                color = baseColor,
                startAngle = currentAngle - 90f,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
            
            // 轻微高光 - 简洁设计
            drawArc(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = Offset(centerX - radius * 0.3f, centerY - radius * 0.3f),
                    radius = radius * 0.6f
                ),
                startAngle = currentAngle - 90f,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
            
            // 分隔线 - 更精致
            val lineAngle = (currentAngle - 90f) * Math.PI / 180.0
            val lineStartX = centerX + (radius * 0.25f) * cos(lineAngle).toFloat()
            val lineStartY = centerY + (radius * 0.25f) * sin(lineAngle).toFloat()
            val lineEndX = centerX + radius * cos(lineAngle).toFloat()
            val lineEndY = centerY + radius * sin(lineAngle).toFloat()
            
            // 分隔线阴影
            drawLine(
                color = Color.Black.copy(alpha = 0.2f),
                start = Offset(lineStartX + 1f, lineStartY + 1f),
                end = Offset(lineEndX + 1f, lineEndY + 1f),
                strokeWidth = 4f
            )
            
            // 分隔线主体
            drawLine(
                color = Color.White.copy(alpha = 0.8f),
                start = Offset(lineStartX, lineStartY),
                end = Offset(lineEndX, lineEndY),
                strokeWidth = 3f
            )
            
            currentAngle += sweepAngle
        }
        
        // 简洁边框
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 5f)
        )
        
        // ========== 简洁优雅的中心圆设计 ==========
        
        // 1. 柔和阴影
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = Offset(centerX + 2f, centerY + 2f),
                radius = radius * 0.26f
            ),
            radius = radius * 0.26f,
            center = Offset(centerX + 2f, centerY + 2f)
        )
        
        // 2. 外圈彩色环
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFFF6B9D),
                    Color(0xFFFFD93D),
                    Color(0xFF4CAF50),
                    Color(0xFF2196F3),
                    Color(0xFFAB47BC),
                    Color(0xFFFF6B9D)
                ),
                center = Offset(centerX, centerY)
            ),
            radius = radius * 0.25f,
            center = Offset(centerX, centerY)
        )
        
        // 3. 白色过渡环
        drawCircle(
            color = Color.White,
            radius = radius * 0.23f,
            center = Offset(centerX, centerY)
        )
        
        // 4. 主圆盘 - 温暖渐变
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFFBF5),
                    Color(0xFFFFEFDD)
                ),
                center = Offset(centerX, centerY),
                radius = radius * 0.22f
            ),
            radius = radius * 0.22f,
            center = Offset(centerX, centerY)
        )
        
        // 5. 装饰小圆点 - 简约设计
        val decorRadius = radius * 0.15f
        val decorPositions = listOf(0f, 90f, 180f, 270f)
        decorPositions.forEach { angle ->
            val rad = angle * Math.PI.toFloat() / 180f
            val x = centerX + decorRadius * cos(rad)
            val y = centerY + decorRadius * sin(rad)
            
            // 小圆主体
            drawCircle(
                color = Color(0xFFFFD700),
                radius = radius * 0.02f,
                center = Offset(x, y)
            )
            
            // 小圆高光
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = radius * 0.008f,
                center = Offset(x - radius * 0.006f, y - radius * 0.006f)
            )
        }
        
        // 6. 简洁边框
        drawCircle(
            color = Color(0xFFBDBDBD),
            radius = radius * 0.22f,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2f)
        )
        
        // 7. 顶部高光 - 玻璃质感
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.5f),
                    Color.Transparent
                ),
                center = Offset(centerX - radius * 0.08f, centerY - radius * 0.08f),
                radius = radius * 0.12f
            ),
            radius = radius * 0.12f,
            center = Offset(centerX - radius * 0.05f, centerY - radius * 0.05f)
        )
        
        // 8. 中心星形装饰
        val starPoints = 8
        val starOuterRadius = radius * 0.055f
        val starInnerRadius = radius * 0.028f
        
        val starPath = Path()
        for (i in 0 until starPoints * 2) {
            val angle = (i * 180f / starPoints - 90f) * Math.PI.toFloat() / 180f
            val r = if (i % 2 == 0) starOuterRadius else starInnerRadius
            val x = centerX + r * cos(angle)
            val y = centerY + r * sin(angle)
            
            if (i == 0) {
                starPath.moveTo(x, y)
            } else {
                starPath.lineTo(x, y)
            }
        }
        starPath.close()
        
        // 星形主体
        drawPath(
            starPath,
            color = Color(0xFFFFD700)
        )
        
        // 9. 中心圆点
        drawCircle(
            color = Color.White,
            radius = radius * 0.016f,
            center = Offset(centerX, centerY)
        )
        
        drawCircle(
            color = Color(0xFFFFD700),
            radius = radius * 0.016f,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1f)
        )
        
        // 绘制文字
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            var textAngle = 0f
            
            Log.d(TAG, "Drawing text for ${options.size} options")
            
            options.forEachIndexed { index, option ->
                val sweepAngle = if (showWeightVisualization && totalWeight > 0) {
                    360f * option.weight / totalWeight
                } else {
                    anglePerOption
                }
                
                val midAngle = (textAngle + sweepAngle / 2 - 90f) * Math.PI.toFloat() / 180f
                val textRadius = radius * 0.65f
                val textX = centerX + textRadius * cos(midAngle)
                val textY = centerY + textRadius * sin(midAngle)
                
                Log.d(TAG, "Text $index: '${option.text}' at ($textX, $textY), angle: ${textAngle + sweepAngle / 2}")
                
                nativeCanvas.save()
                nativeCanvas.translate(textX, textY)
                nativeCanvas.rotate((textAngle + sweepAngle / 2))
                
                // 文字阴影
                val shadowPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 46f
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    style = android.graphics.Paint.Style.FILL
                    alpha = 80
                    isAntiAlias = true
                }
                
                // 文字描边
                val strokePaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 46f
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 6f
                    isAntiAlias = true
                }
                
                // 文字填充
                val fillPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 46f
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    style = android.graphics.Paint.Style.FILL
                    isAntiAlias = true
                }
                
                // 计算概率
                val probability = if (totalWeight > 0) {
                    (option.weight.toFloat() / totalWeight * 100).toInt()
                } else {
                    (100f / options.size).toInt()
                }
                
                // 绘制选项名称
                val drawY = fillPaint.textSize / 3
                nativeCanvas.drawText(option.text, 0f, drawY + 2f, shadowPaint)
                nativeCanvas.drawText(option.text, 0f, drawY, strokePaint)
                nativeCanvas.drawText(option.text, 0f, drawY, fillPaint)
                
                // 如果开启权重可视化，显示概率
                if (showWeightVisualization) {
                    val probabilityText = "$probability%"
                    
                    // 概率文字样式（较小）
                    val probShadowPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 32f
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        style = android.graphics.Paint.Style.FILL
                        alpha = 80
                        isAntiAlias = true
                    }
                    
                    val probStrokePaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 32f
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 5f
                        isAntiAlias = true
                    }
                    
                    val probFillPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.YELLOW
                        textSize = 32f
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        style = android.graphics.Paint.Style.FILL
                        isAntiAlias = true
                    }
                    
                    // 在选项名称下方绘制概率
                    val probDrawY = drawY + 50f
                    nativeCanvas.drawText(probabilityText, 0f, probDrawY + 2f, probShadowPaint)
                    nativeCanvas.drawText(probabilityText, 0f, probDrawY, probStrokePaint)
                    nativeCanvas.drawText(probabilityText, 0f, probDrawY, probFillPaint)
                }
                
                nativeCanvas.restore()
                textAngle += sweepAngle
            }
        }
    }
}

/**
 * 指针 - 精美现代设计
 */
@Composable
private fun PointerIndicator() {
    Canvas(
        modifier = Modifier
            .size(70.dp)
            .offset(y = (-35).dp)
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        
        // 指针尺寸
        val arrowHeight = 55f
        val arrowWidth = 36f
        val tipWidth = 18f
        
        // === 1. 外层光晕效果 ===
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFF5722).copy(alpha = 0.3f),
                    Color(0xFFFF5722).copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = 45f
            ),
            radius = 45f,
            center = Offset(centerX, centerY)
        )
        
        // === 2. 多层阴影 ===
        for (i in 0..2) {
            val shadowPath = Path().apply {
                moveTo(centerX, centerY - arrowHeight / 2)
                lineTo(centerX + tipWidth / 2, centerY - arrowHeight / 2 + 14f)
                lineTo(centerX + arrowWidth / 2, centerY - arrowHeight / 2 + 14f)
                lineTo(centerX + arrowWidth / 2, centerY + arrowHeight / 2 - 2f)
                lineTo(centerX - arrowWidth / 2, centerY + arrowHeight / 2 - 2f)
                lineTo(centerX - arrowWidth / 2, centerY - arrowHeight / 2 + 14f)
                lineTo(centerX - tipWidth / 2, centerY - arrowHeight / 2 + 14f)
                close()
            }
            drawPath(
                shadowPath,
                color = Color.Black.copy(alpha = 0.15f - i * 0.05f)
            )
        }
        
        // === 3. 主体箭头路径 ===
        val arrowPath = Path().apply {
            moveTo(centerX, centerY - arrowHeight / 2)
            lineTo(centerX + tipWidth / 2, centerY - arrowHeight / 2 + 14f)
            lineTo(centerX + arrowWidth / 2, centerY - arrowHeight / 2 + 14f)
            lineTo(centerX + arrowWidth / 2, centerY + arrowHeight / 2 - 2f)
            lineTo(centerX - arrowWidth / 2, centerY + arrowHeight / 2 - 2f)
            lineTo(centerX - arrowWidth / 2, centerY - arrowHeight / 2 + 14f)
            lineTo(centerX - tipWidth / 2, centerY - arrowHeight / 2 + 14f)
            close()
        }
        
        // === 4. 主体渐变 - 火焰色 ===
        drawPath(
            arrowPath,
            brush = Brush.verticalGradient(
                0f to Color(0xFFFF6B35),
                0.3f to Color(0xFFFF5722),
                0.6f to Color(0xFFFF3D00),
                1f to Color(0xFFE64A19)
            )
        )
        
        // === 5. 左侧高光 ===
        val leftHighlight = Path().apply {
            moveTo(centerX - tipWidth / 2 + 2f, centerY - arrowHeight / 2 + 14f)
            lineTo(centerX - 2f, centerY - arrowHeight / 2 + 3f)
            lineTo(centerX - arrowWidth / 2 + 4f, centerY - arrowHeight / 2 + 14f)
            lineTo(centerX - arrowWidth / 2 + 4f, centerY + arrowHeight / 2 - 8f)
            close()
        }
        drawPath(leftHighlight, Color.White.copy(alpha = 0.4f))
        
        // === 6. 右侧阴影 ===
        val rightShadow = Path().apply {
            moveTo(centerX + tipWidth / 2 - 2f, centerY - arrowHeight / 2 + 14f)
            lineTo(centerX + 2f, centerY - arrowHeight / 2 + 3f)
            lineTo(centerX + arrowWidth / 2 - 4f, centerY - arrowHeight / 2 + 14f)
            lineTo(centerX + arrowWidth / 2 - 4f, centerY + arrowHeight / 2 - 8f)
            close()
        }
        drawPath(rightShadow, Color.Black.copy(alpha = 0.2f))
        
        // === 7. 尖端高光 ===
        val tipHighlight = Path().apply {
            moveTo(centerX, centerY - arrowHeight / 2 + 1f)
            lineTo(centerX - 5f, centerY - arrowHeight / 2 + 11f)
            lineTo(centerX + 5f, centerY - arrowHeight / 2 + 11f)
            close()
        }
        drawPath(tipHighlight, Color.White.copy(alpha = 0.7f))
        
        // === 8. 金色边框 ===
        drawPath(
            arrowPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFD700),
                    Color(0xFFFFA500),
                    Color(0xFFFFD700)
                )
            ),
            style = Stroke(width = 3f)
        )
        
        // === 9. 中心装饰线 ===
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.6f),
                    Color.White.copy(alpha = 0.3f),
                    Color.Transparent
                )
            ),
            start = Offset(centerX, centerY - arrowHeight / 2 + 16f),
            end = Offset(centerX, centerY + arrowHeight / 2 - 10f),
            strokeWidth = 2f
        )
        
        // === 10. 底部装饰圆 ===
        val circleY = centerY + arrowHeight / 2 + 3f
        
        // 圆的外层光晕
        for (i in 0..2) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF5722).copy(alpha = 0.25f - i * 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, circleY),
                    radius = 16f + i * 3f
                ),
                radius = 16f + i * 3f,
                center = Offset(centerX, circleY)
            )
        }
        
        // 圆的阴影
        drawCircle(
            color = Color.Black.copy(alpha = 0.25f),
            radius = 14f,
            center = Offset(centerX + 1.5f, circleY + 1.5f)
        )
        
        // 圆的主体 - 渐变
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFF7043),
                    Color(0xFFFF5722),
                    Color(0xFFFF3D00),
                    Color(0xFFE64A19)
                ),
                center = Offset(centerX - 2f, circleY - 2f),
                radius = 13f
            ),
            radius = 13f,
            center = Offset(centerX, circleY)
        )
        
        // 圆的金色边框
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFFFD700),
                    Color(0xFFFFA500),
                    Color(0xFFFFD700),
                    Color(0xFFFFA500),
                    Color(0xFFFFD700)
                ),
                center = Offset(centerX, circleY)
            ),
            radius = 13f,
            center = Offset(centerX, circleY),
            style = Stroke(width = 2.5f)
        )
        
        // 圆的内圈装饰
        drawCircle(
            color = Color(0xFFFFAB91).copy(alpha = 0.5f),
            radius = 10f,
            center = Offset(centerX, circleY),
            style = Stroke(width = 1f)
        )
        
        // 圆的顶部高光
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.8f),
                    Color.White.copy(alpha = 0.4f),
                    Color.Transparent
                ),
                center = Offset(centerX - 4f, circleY - 4f),
                radius = 6f
            ),
            radius = 6f,
            center = Offset(centerX - 3f, circleY - 3f)
        )
        
        // 圆的中心点
        drawCircle(
            color = Color.White,
            radius = 4f,
            center = Offset(centerX, circleY)
        )
        
        drawCircle(
            color = Color(0xFFFF5722),
            radius = 2f,
            center = Offset(centerX, circleY)
        )
        
        // === 11. 顶部装饰星星 ===
        val starX = centerX
        val starY = centerY - arrowHeight / 2 - 8f
        
        // 星星光芒（4条）
        for (angle in 0..3) {
            val rad = (angle * 90f) * Math.PI.toFloat() / 180f
            val startX = starX + 4f * cos(rad)
            val startY = starY + 4f * sin(rad)
            val endX = starX + 10f * cos(rad)
            val endY = starY + 10f * sin(rad)
            
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFD700).copy(alpha = 0.9f),
                        Color(0xFFFFD700).copy(alpha = 0.5f),
                        Color.Transparent
                    ),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY)
                ),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2.5f
            )
        }
        
        // 星星中心
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFFFD700)
                ),
                center = Offset(starX, starY),
                radius = 4f
            ),
            radius = 4f,
            center = Offset(starX, starY)
        )
        
        drawCircle(
            color = Color(0xFFFFD700),
            radius = 4f,
            center = Offset(starX, starY),
            style = Stroke(width = 1f)
        )
        
        drawCircle(
            color = Color(0xFFFFD700),
            radius = 2f,
            center = Offset(starX, starY)
        )
    }
}

// 结果动画 - 根据模式显示不同效果
@Composable
fun ResultAnimation(
    result: String,
    mode: SpinWheelMode,
    onDismiss: () -> Unit,
    panelSkin: String = "js_1"
) {
    // 直接使用 SimpleResultAnimation
    SimpleResultAnimation(
        result = result,
        mode = mode,
        onDismiss = onDismiss,
        panelSkin = panelSkin
    )
}

// 增强版普通模式卡片 - 简化版（确保抖动可见）
@Composable
fun EnhancedNormalModeCard(
    result: String,
    scale: Float,
    rotation: Float,
    shakeIntensity: Float,
    particleBurst: Float
) {
    // 简单直接的抖动 - 使用 derivedStateOf 实时计算
    val shakeX by remember {
        derivedStateOf {
            if (shakeIntensity > 0.1f) {
                (kotlin.math.sin(System.currentTimeMillis() / 30.0) * 15f * shakeIntensity).toFloat()
            } else 0f
        }
    }
    
    val shakeY by remember {
        derivedStateOf {
            if (shakeIntensity > 0.1f) {
                (kotlin.math.cos(System.currentTimeMillis() / 25.0) * 10f * shakeIntensity).toFloat()
            } else 0f
        }
    }
    
    // 强制重组以更新抖动
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(shakeIntensity) {
        if (shakeIntensity > 0.1f) {
            while (shakeIntensity > 0.1f) {
                tick++
                delay(16)  // 60fps
            }
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 粒子爆发层 - 48个粒子（三层，每层16个）
        if (particleBurst > 0.05f) {
            Box(
                modifier = Modifier
                    .size(800.dp)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    },
                contentAlignment = Alignment.Center
            ) {
                // 第一层：快速外扩
                repeat(16) { i ->
                    val angle = (i * 22.5f) * Math.PI.toFloat() / 180f
                    val distance = 200f + particleBurst * 250f
                    val offsetX = distance * cos(angle)
                    val offsetY = distance * sin(angle)
                    val alpha = (1f - particleBurst * 0.8f).coerceIn(0f, 1f)
                    
                    if (alpha > 0.05f) {
                        Text(
                            text = "🎁",
                            fontSize = (24 - particleBurst * 16).sp,
                            modifier = Modifier
                                .offset(x = offsetX.dp, y = offsetY.dp)
                                .graphicsLayer {
                                    this.alpha = alpha
                                    rotationZ = particleBurst * 720f
                                }
                        )
                    }
                }
                
                // 第二层：中速
                repeat(16) { i ->
                    val angle = ((i * 22.5f) + 11.25f) * Math.PI.toFloat() / 180f
                    val distance = 200f + particleBurst * 200f
                    val offsetX = distance * cos(angle)
                    val offsetY = distance * sin(angle)
                    val alpha = (1f - particleBurst * 0.7f).coerceIn(0f, 1f)
                    
                    if (alpha > 0.05f) {
                        Text(
                            text = "🎁",
                            fontSize = (22 - particleBurst * 14).sp,
                            modifier = Modifier
                                .offset(x = offsetX.dp, y = offsetY.dp)
                                .graphicsLayer {
                                    this.alpha = alpha
                                    rotationZ = -particleBurst * 540f
                                }
                        )
                    }
                }
                
                // 第三层：慢速内圈
                repeat(16) { i ->
                    val angle = (i * 22.5f) * Math.PI.toFloat() / 180f
                    val distance = 200f + particleBurst * 150f
                    val offsetX = distance * cos(angle)
                    val offsetY = distance * sin(angle)
                    val alpha = (1f - particleBurst * 0.6f).coerceIn(0f, 1f)
                    
                    if (alpha > 0.05f) {
                        Text(
                            text = "✨",
                            fontSize = (20 - particleBurst * 12).sp,
                            modifier = Modifier
                                .offset(x = offsetX.dp, y = offsetY.dp)
                                .graphicsLayer {
                                    this.alpha = alpha
                                    rotationZ = particleBurst * 360f
                                }
                        )
                    }
                }
            }
        }
        
        // 光晕效果
        Box(
            modifier = Modifier
                .size((400 * scale).dp)
                .graphicsLayer {
                    this.alpha = 0.4f
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF2196F3).copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        
                // 主卡片
        Box(
            modifier = Modifier
                .width(280.dp)
                .wrapContentHeight()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                    translationX = shakeX  // 直接使用计算值
                    translationY = shakeY  // 直接使用计算值
                }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE3F2FD),
                            Color(0xFFBBDEFB),
                            Color(0xFF90CAF9)
                        )
                    ),
                    RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 顶部装饰
                Text(
                    "✨ 恭喜抽中 ✨",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                
                // 大图标
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFE3F2FD),
                                    Color(0xFFBBDEFB)
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "🎁",
                        fontSize = 48.sp
                    )
                }
                
                // 结果文字
                Text(
                    result,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1565C0),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

// 为其他模式创建占位函数
@Composable
fun EnhancedAdvancedModeCard(result: String, scale: Float, rotation: Float, shakeIntensity: Float, particleBurst: Float) {
    // 使用相同逻辑，只是颜色不同
    EnhancedNormalModeCard(result, scale, rotation, shakeIntensity, particleBurst)
}

@Composable
fun EnhancedLuckyModeCard(result: String, scale: Float, rotation: Float, shakeIntensity: Float, particleBurst: Float) {
    // 使用相同逻辑，只是颜色不同
    EnhancedNormalModeCard(result, scale, rotation, shakeIntensity, particleBurst)
}

// 保留旧版本以防需要回退
// 普通模式卡片 - 清新蓝色设计（丝滑流畅版 - 明显抖动 + 更多粒子）
@Composable
fun NormalModeCard(result: String, scale: Float, rotation: Float, alpha: Float, explosionProgress: Float = 0f) {
    // 烟花粒子角度 - 减少到16个
    val fireworkParticles = remember {
        List(16) { i ->
            (i * 22.5f) * Math.PI.toFloat() / 180f
        }
    }
    
    // 抖动动画 - 增强版，更明显的抖动
    val shakeX = remember { Animatable(0f) }
    val shakeY = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        // 同时启动X和Y方向的抖动
        launch {
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 800) {
                shakeX.snapTo(12f)
                delay(30)
                shakeX.snapTo(-12f)
                delay(30)
            }
            shakeX.animateTo(0f, animationSpec = tween(300, easing = FastOutSlowInEasing))
        }
        launch {
            delay(15) // 错开Y轴抖动，产生更自然的效果
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 800) {
                shakeY.snapTo(8f)
                delay(30)
                shakeY.snapTo(-8f)
                delay(30)
            }
            shakeY.animateTo(0f, animationSpec = tween(300, easing = FastOutSlowInEasing))
        }
    }
    
    // 粒子动画 - 使用Animatable，流畅循环
    val particleProgress = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        while (true) {
            particleProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(900, easing = LinearEasing)
            )
            particleProgress.snapTo(0f)
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 烟花效果层 - 在最底层，从卡片位置向四周扩散（优化渲染）
        if (explosionProgress > 0.01f) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // 启用硬件加速
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
            ) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val maxRadius = 800f
                
                fireworkParticles.forEach { angle ->
                    val radius = maxRadius * explosionProgress
                    val particleAlpha = if (explosionProgress < 0.7f) {
                        0.9f
                    } else {
                        0.9f * (1f - (explosionProgress - 0.7f) / 0.3f)
                    }
                    
                    if (particleAlpha > 0.01f) {
                        val x = centerX + radius * cos(angle)
                        val y = centerY + radius * sin(angle)
                        
                        // 绘制发光粒子
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF2196F3).copy(alpha = particleAlpha),
                                    Color(0xFF64B5F6).copy(alpha = particleAlpha * 0.6f),
                                    Color.Transparent
                                )
                            ),
                            radius = 50f,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
        
        // 外层光晕效果
        Box(
            modifier = Modifier
                .size(380.dp)
                .graphicsLayer {
                    scaleX = scale * 1.1f
                    scaleY = scale * 1.1f
                    this.alpha = alpha * 0.3f
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF2196F3).copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        
        // 礼物盒粒子层 - 增强版（32个粒子，双层效果）
        Box(
            modifier = Modifier
                .width(400.dp)
                .height(600.dp)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                },
            contentAlignment = Alignment.Center
        ) {
            // 第一层：外圈礼物盒粒子（16个）
            repeat(16) { i ->
                val angle = (i * 22.5f) * Math.PI.toFloat() / 180f
                val baseDistance = 170f
                val distance = baseDistance + particleProgress.value * 200f
                
                val offsetX = distance * cos(angle)
                val offsetY = distance * sin(angle)
                
                val pAlpha = (1f - particleProgress.value) * alpha
                
                if (pAlpha > 0.05f) {
                    Text(
                        text = "🎁",
                        fontSize = (22 - particleProgress.value * 14).sp,
                        modifier = Modifier
                            .offset(x = offsetX.dp, y = offsetY.dp)
                            .graphicsLayer {
                                this.alpha = pAlpha
                                rotationZ = particleProgress.value * 360f
                            }
                    )
                }
            }
            
            // 第二层：内圈礼物盒粒子（16个，错开角度）
            repeat(16) { i ->
                val angle = ((i * 22.5f) + 11.25f) * Math.PI.toFloat() / 180f  // 错开11.25度
                val baseDistance = 170f
                val distance = baseDistance + particleProgress.value * 160f  // 稍微慢一点
                
                val offsetX = distance * cos(angle)
                val offsetY = distance * sin(angle)
                
                val pAlpha = (1f - particleProgress.value * 0.8f) * alpha * 0.8f
                
                if (pAlpha > 0.05f) {
                    Text(
                        text = "🎁",
                        fontSize = (18 - particleProgress.value * 10).sp,
                        modifier = Modifier
                            .offset(x = offsetX.dp, y = offsetY.dp)
                            .graphicsLayer {
                                this.alpha = pAlpha
                                rotationZ = -particleProgress.value * 360f  // 反向旋转
                            }
                    )
                }
            }
        }
        
        Box(
            modifier = Modifier
                .width(280.dp)
                .wrapContentHeight()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                    translationX = shakeX.value  // X轴抖动
                    translationY = shakeY.value  // Y轴抖动
                    this.alpha = alpha
                }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE3F2FD),
                            Color(0xFFBBDEFB),
                            Color(0xFF90CAF9)
                        )
                    ),
                    RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {
            // 移除卡片背景粒子，提升性能
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 顶部装饰
                Text("✨ 恭喜抽中 ✨", 
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                
                // 大图标 + 粒子散发效果
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 粒子散发层 - 从图标中心向外（减少到8个粒子，硬件加速）
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                    ) {
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        
                        // 生成8个粒子，从中心向外散发
                        repeat(8) { i ->
                            val angle = (i * 45f + particleProgress.value * 360f) * Math.PI.toFloat() / 180f
                            val distance = 55f * particleProgress.value  // 粒子向外移动的距离
                            
                            val x = centerX + distance * cos(angle)
                            val y = centerY + distance * sin(angle)
                            
                            // 粒子透明度（从中心到边缘逐渐淡出）
                            val pAlpha = (1f - particleProgress.value) * 0.9f
                            
                            // 粒子大小（从大到小）
                            val pSize = 12f * (1f - particleProgress.value * 0.5f)
                            
                            if (pAlpha > 0.05f) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFFD700).copy(alpha = pAlpha),
                                            Color(0xFFFFA500).copy(alpha = pAlpha * 0.6f),
                                            Color.Transparent
                                        )
                                    ),
                                    radius = pSize,
                                    center = Offset(x, y)
                                )
                            }
                        }
                    }
                    
                    // 图标背景
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .graphicsLayer {
                                // 持续抖动（X和Y方向）
                                translationX = shakeX.value
                                translationY = shakeY.value
                            }
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFE3F2FD),
                                        Color(0xFFBBDEFB)
                                    )
                                ),
                                CircleShape
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color(0xFF2196F3),
                                        Color(0xFF00BCD4),
                                        Color(0xFF2196F3)
                                    )
                                ),
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                        
                        Text(
                            "🎯",
                            fontSize = 42.sp,
                            modifier = Modifier.graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                        )
                    }
                }
                
                // 结果卡片
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 12.dp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFE3F2FD),
                                        Color.White,
                                        Color(0xFFE3F2FD)
                                    )
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            result,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp,
                            color = Color(0xFF1565C0)
                        )
                    }
                }
                
                // 星星评级
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    repeat(5) { index ->
                        val starScale by animateFloatAsState(
                            targetValue = if (scale > 0.5f) 1f else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "star$index"
                        )
                        Text(
                            "⭐",
                            fontSize = 24.sp,
                            modifier = Modifier.graphicsLayer {
                                scaleX = starScale
                                scaleY = starScale
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdvancedModeCard(result: String, scale: Float, rotation: Float, alpha: Float, explosionProgress: Float = 0f) {
    // 缓存粒子位置
    val particles = remember {
        List(8) { i ->
            Triple(
                Random.nextFloat(),
                Random.nextFloat(),
                Random.nextFloat() * 0.5f + 0.5f // 大小因子
            )
        }
    }
    
    // 烟花粒子角度
    val fireworkParticles = remember {
        List(24) { i ->
            (i * 15f) * Math.PI.toFloat() / 180f
        }
    }
    
    // 流畅的粒子动画
    val infiniteTransition = rememberInfiniteTransition(label = "tech")
    val techProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 烟花效果层 - 在最底层，从卡片位置向四周扩散
        if (explosionProgress > 0.01f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val maxRadius = 800f
                
                fireworkParticles.forEach { angle ->
                    val radius = maxRadius * explosionProgress
                    val particleAlpha = if (explosionProgress < 0.7f) {
                        0.9f
                    } else {
                        0.9f * (1f - (explosionProgress - 0.7f) / 0.3f)
                    }
                    
                    if (particleAlpha > 0.01f) {
                        val x = centerX + radius * cos(angle)
                        val y = centerY + radius * sin(angle)
                        
                        // 绘制发光粒子
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFE91E63).copy(alpha = particleAlpha),
                                    Color(0xFFFF4081).copy(alpha = particleAlpha * 0.6f),
                                    Color.Transparent
                                )
                            ),
                            radius = 50f,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
        
        // 外层脉冲光环
        Box(
            modifier = Modifier
                .size(380.dp)
                .graphicsLayer {
                    scaleX = scale * 1.1f
                    scaleY = scale * 1.1f
                    this.alpha = alpha * 0.4f
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF9C27B0).copy(alpha = 0.5f),
                            Color(0xFFE91E63).copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        
        Box(
            modifier = Modifier
                .width(340.dp)
                .wrapContentHeight()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                    this.alpha = alpha
                }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A2E),
                            Color(0xFF16213E),
                            Color(0xFF0F3460)
                        )
                    ),
                    RoundedCornerShape(28.dp)
                )
                .padding(24.dp)
        ) {
            // 优化的科技粒子 - 放在最底层
            Canvas(modifier = Modifier.matchParentSize()) {
                val width = size.width
                val height = size.height
                
                particles.forEach { (startX, startY, sizeFactor) ->
                    val x = width * startX
                    val y = (height * startY + techProgress * height * 0.4f) % height
                    val particleAlpha = 1f - (y / height) * 0.5f
                    
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFE91E63).copy(alpha = 0.8f * particleAlpha),
                                Color(0xFF9C27B0).copy(alpha = 0.4f * particleAlpha),
                                Color.Transparent
                            )
                        ),
                        radius = 18f * sizeFactor,
                        center = Offset(x, y)
                    )
                }
            }
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 顶部装饰
                Text("⚡ 系统选定 ⚡", 
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE91E63)
                )
                
                // 大图标 - 六边形容器
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 六边形背景
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val hexPath = Path().apply {
                            val centerX = size.width / 2
                            val centerY = size.height / 2
                            val radius = size.width / 2
                            
                            for (i in 0..5) {
                                val angle = (i * 60f - 30f) * Math.PI.toFloat() / 180f
                                val x = centerX + radius * cos(angle)
                                val y = centerY + radius * sin(angle)
                                if (i == 0) moveTo(x, y) else lineTo(x, y)
                            }
                            close()
                        }
                        
                        drawPath(
                            path = hexPath,
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF9C27B0).copy(alpha = 0.3f),
                                    Color(0xFF1A1A2E)
                                )
                            )
                        )
                        
                        drawPath(
                            path = hexPath,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFF9C27B0),
                                    Color(0xFFE91E63),
                                    Color(0xFF9C27B0)
                                )
                            ),
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }
                    
                    Text(
                        "⚙️",
                        fontSize = 56.sp,
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                    )
                }
                
                // 结果卡片
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1A1A2E),
                    shadowElevation = 16.dp,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF9C27B0),
                                Color(0xFFE91E63),
                                Color(0xFF9C27B0)
                            )
                        )
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF16213E),
                                        Color(0xFF1A1A2E),
                                        Color(0xFF16213E)
                                    )
                                )
                            )
                            .padding(horizontal = 28.dp, vertical = 16.dp)
                    ) {
                        Text(
                            result,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 36.sp,
                            color = Color(0xFFE91E63)
                        )
                    }
                }
                
                // 星星评级
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    repeat(5) { index ->
                        val starScale by animateFloatAsState(
                            targetValue = if (scale > 0.5f) 1f else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "star$index"
                        )
                        Text(
                            "⭐",
                            fontSize = 24.sp,
                            modifier = Modifier.graphicsLayer {
                                scaleX = starScale
                                scaleY = starScale
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LuckyModeCard(result: String, scale: Float, rotation: Float, alpha: Float, explosionProgress: Float = 0f) {
    // 缓存金币粒子位置
    val coins = remember {
        List(10) { i ->
            Triple(
                Random.nextFloat(),
                Random.nextFloat(),
                Random.nextFloat() * 0.6f + 0.4f // 大小因子
            )
        }
    }
    
    // 烟花粒子角度
    val fireworkParticles = remember {
        List(24) { i ->
            (i * 15f) * Math.PI.toFloat() / 180f
        }
    }
    
    // 流畅的金币雨动画
    val infiniteTransition = rememberInfiniteTransition(label = "coins")
    val coinProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 烟花效果层 - 在最底层，从卡片位置向四周扩散
        if (explosionProgress > 0.01f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val maxRadius = 800f
                
                fireworkParticles.forEach { angle ->
                    val radius = maxRadius * explosionProgress
                    val particleAlpha = if (explosionProgress < 0.7f) {
                        0.9f
                    } else {
                        0.9f * (1f - (explosionProgress - 0.7f) / 0.3f)
                    }
                    
                    if (particleAlpha > 0.01f) {
                        val x = centerX + radius * cos(angle)
                        val y = centerY + radius * sin(angle)
                        
                        // 绘制发光粒子
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFD700).copy(alpha = particleAlpha),
                                    Color(0xFFFFA500).copy(alpha = particleAlpha * 0.6f),
                                    Color.Transparent
                                )
                            ),
                            radius = 50f,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
        
        // 外层金色光环
        Box(
            modifier = Modifier
                .size(400.dp)
                .graphicsLayer {
                    scaleX = scale * 1.1f
                    scaleY = scale * 1.1f
                    this.alpha = alpha * 0.5f
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = 0.6f),
                            Color(0xFFFFA500).copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        
        Box(
            modifier = Modifier
                .width(340.dp)
                .wrapContentHeight()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                    this.alpha = alpha
                }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFF9C4),
                            Color(0xFFFFE082),
                            Color(0xFFFFD54F)
                        )
                    ),
                    RoundedCornerShape(28.dp)
                )
                .padding(24.dp)
        ) {
            // 优化的金币雨效果 - 放在最底层
            Canvas(modifier = Modifier.matchParentSize()) {
                val width = size.width
                val height = size.height
                
                coins.forEach { (startX, startY, sizeFactor) ->
                    val x = width * startX
                    val y = (height * startY + coinProgress * height * 0.5f) % height
                    val coinAlpha = 1f - (y / height) * 0.3f
                    
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD700).copy(alpha = 0.9f * coinAlpha),
                                Color(0xFFFFA500).copy(alpha = 0.5f * coinAlpha),
                                Color.Transparent
                            )
                        ),
                        radius = 22f * sizeFactor,
                        center = Offset(x, y)
                    )
                }
            }
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 顶部装饰
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💫", fontSize = 32.sp)
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(4.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFFFD700),
                                        Color(0xFFFFA500),
                                        Color(0xFFFFD700)
                                    )
                                ),
                                RoundedCornerShape(2.dp)
                            )
                    )
                    Text("💫", fontSize = 32.sp)
                }
                
                // 大图标 - 多层圆形容器
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 最外层光晕
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFD700).copy(alpha = 0.4f),
                                    Color(0xFFFFA500).copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                    }
                    
                    // 中层旋转圆环
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .background(
                                Brush.sweepGradient(
                                    colors = listOf(
                                        Color(0xFFFFD700),
                                        Color(0xFFFFA500),
                                        Color(0xFFFF8C00),
                                        Color(0xFFFFD700)
                                    )
                                ),
                                CircleShape
                            )
                            .padding(4.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFFDE7),
                                        Color(0xFFFFE082)
                                    )
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "🍀",
                            fontSize = 72.sp,
                            modifier = Modifier.graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                        )
                    }
                }
                
                // 标题
                Text(
                    "幸运降临",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 30.sp,
                    color = Color(0xFFFF8C00),
                    modifier = Modifier.graphicsLayer {
                        shadowElevation = 4f
                    }
                )
                
                // 结果卡片 - 超华丽
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFFFFF9C4),
                    shadowElevation = 16.dp,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        3.dp,
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFFD700),
                                Color(0xFFFFA500),
                                Color(0xFFFF8C00),
                                Color(0xFFFFA500),
                                Color(0xFFFFD700)
                            )
                        )
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFE082),
                                        Color(0xFFFFF9C4),
                                        Color(0xFFFFE082)
                                    )
                                )
                            )
                            .padding(horizontal = 32.dp, vertical = 24.dp)
                    ) {
                        Text(
                            result,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 44.sp,
                            color = Color(0xFFFF8C00)
                        )
                    }
                }
                
                // 星星评级 - 金色
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    repeat(5) { index ->
                        val starScale by animateFloatAsState(
                            targetValue = if (scale > 0.5f) 1f else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "star$index"
                        )
                        Text(
                            "⭐",
                            fontSize = 36.sp,
                            modifier = Modifier.graphicsLayer {
                                scaleX = starScale
                                scaleY = starScale
                            }
                        )
                    }
                }
            }
        }
    }
}

// 简化的普通模式背景 - 蓝色烟花粒子（增强版）
@Composable
fun SimplifiedNormalBackground(alpha: Float) {
    // 缓存烟花粒子的角度和速度
    val fireworks = remember {
        List(12) { i ->
            val angle = (i * 30f) * Math.PI.toFloat() / 180f
            Triple(angle, Random.nextFloat() * 0.5f + 0.5f, Random.nextFloat() * 0.3f)
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "firework")
    val fireworkProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = size.width.coerceAtLeast(size.height) * 0.5f
        
        fireworks.forEach { (angle, speedFactor, delay) ->
            val adjustedProgress = ((fireworkProgress - delay).coerceAtLeast(0f) / (1f - delay)).coerceIn(0f, 1f)
            val radius = maxRadius * adjustedProgress * speedFactor
            val particleAlpha = (1f - adjustedProgress * 0.7f) * alpha
            
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)
            
            drawCircle(
                color = Color(0xFF2196F3).copy(alpha = particleAlpha),
                radius = 25f * (1f - adjustedProgress * 0.3f),
                center = Offset(x, y)
            )
        }
    }
}

// 简化的进阶模式背景 - 紫色烟花粒子（增强版）
@Composable
fun SimplifiedAdvancedBackground(alpha: Float) {
    val fireworks = remember {
        List(16) { i ->
            val angle = (i * 22.5f) * Math.PI.toFloat() / 180f
            Triple(angle, Random.nextFloat() * 0.5f + 0.5f, Random.nextFloat() * 0.3f)
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "firework")
    val fireworkProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = size.width.coerceAtLeast(size.height) * 0.5f
        
        fireworks.forEach { (angle, speedFactor, delay) ->
            val adjustedProgress = ((fireworkProgress - delay).coerceAtLeast(0f) / (1f - delay)).coerceIn(0f, 1f)
            val radius = maxRadius * adjustedProgress * speedFactor
            val particleAlpha = (1f - adjustedProgress * 0.7f) * alpha
            
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)
            
            drawCircle(
                color = Color(0xFFE91E63).copy(alpha = particleAlpha),
                radius = 28f * (1f - adjustedProgress * 0.3f),
                center = Offset(x, y)
            )
        }
    }
}

// 简化的幸运模式背景 - 金色烟花粒子（增强版）
@Composable
fun SimplifiedLuckyBackground(alpha: Float) {
    val fireworks = remember {
        List(20) { i ->
            val angle = (i * 18f) * Math.PI.toFloat() / 180f
            Triple(angle, Random.nextFloat() * 0.5f + 0.5f, Random.nextFloat() * 0.3f)
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "firework")
    val fireworkProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = size.width.coerceAtLeast(size.height) * 0.5f
        
        fireworks.forEach { (angle, speedFactor, delay) ->
            val adjustedProgress = ((fireworkProgress - delay).coerceAtLeast(0f) / (1f - delay)).coerceIn(0f, 1f)
            val radius = maxRadius * adjustedProgress * speedFactor
            val particleAlpha = (1f - adjustedProgress * 0.7f) * alpha
            
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)
            
            drawCircle(
                color = Color(0xFFFFD700).copy(alpha = particleAlpha),
                radius = 30f * (1f - adjustedProgress * 0.3f),
                center = Offset(x, y)
            )
        }
    }
}

// 普通模式背景 - 增强版蓝色粒子波浪
@Composable
fun NormalModeBackground(alpha: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "normal")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )
    
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // 背景波浪
        for (i in 0..8) {
            val y = height * i / 8f
            val waveAmplitude = 30f
            val path = Path().apply {
                moveTo(0f, y)
                for (x in 0..width.toInt() step 20) {
                    val waveY = y + waveAmplitude * sin((x / 50f + waveOffset / 60f + i * 0.5f) * Math.PI).toFloat()
                    lineTo(x.toFloat(), waveY)
                }
            }
            
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF2196F3).copy(alpha = 0.15f * alpha),
                        Color(0xFF00BCD4).copy(alpha = 0.15f * alpha),
                        Color.Transparent
                    )
                ),
                style = Stroke(width = 2f)
            )
        }
        
        // 动态粒子
        for (i in 0..30) {
            val x = (width * (i % 6) / 6f + offset * 150f) % width
            val y = (height * (i / 6) / 6f + offset * 120f) % height
            val size = 15f + (i % 3) * 8f
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2196F3).copy(alpha = 0.6f * alpha),
                        Color(0xFF00BCD4).copy(alpha = 0.3f * alpha),
                        Color.Transparent
                    )
                ),
                radius = size,
                center = Offset(x, y)
            )
            
            // 内部高光
            drawCircle(
                color = Color.White.copy(alpha = 0.4f * alpha),
                radius = size * 0.3f,
                center = Offset(x - size * 0.2f, y - size * 0.2f)
            )
        }
    }
}

// 进阶模式背景 - 增强版科技紫色矩阵
@Composable
fun AdvancedModeBackground(alpha: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "advanced")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )
    
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // 背景六边形网格
        for (i in 0..6) {
            for (j in 0..10) {
                val x = width * i / 6f + (if (j % 2 == 0) 0f else width / 12f)
                val y = height * j / 10f
                val hexSize = 40f
                
                val hexPath = Path().apply {
                    for (k in 0..5) {
                        val angle = (k * 60f - 30f) * Math.PI.toFloat() / 180f
                        val px = x + hexSize * cos(angle)
                        val py = y + hexSize * sin(angle)
                        if (k == 0) moveTo(px, py) else lineTo(px, py)
                    }
                    close()
                }
                
                drawPath(
                    path = hexPath,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF9C27B0).copy(alpha = 0.2f * alpha * pulseProgress),
                            Color.Transparent
                        ),
                        center = Offset(x, y),
                        radius = hexSize
                    )
                )
                
                drawPath(
                    path = hexPath,
                    color = Color(0xFF9C27B0).copy(alpha = 0.4f * alpha),
                    style = Stroke(width = 1.5f)
                )
            }
        }
        
        // 横向扫描线
        for (i in 0..12) {
            val y = height * i / 12f
            val startX = -width + (progress * width * 2)
            
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFE91E63).copy(alpha = 0.7f * alpha),
                        Color(0xFF9C27B0).copy(alpha = 0.5f * alpha),
                        Color.Transparent
                    ),
                    startX = startX,
                    endX = startX + width * 0.5f
                ),
                start = Offset(startX, y),
                end = Offset(startX + width * 0.5f, y),
                strokeWidth = 3f
            )
        }
        
        // 纵向数据流
        for (i in 0..10) {
            val x = width * i / 10f
            val startY = -height + (progress * height * 1.8f)
            
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFBA68C8).copy(alpha = 0.6f * alpha),
                        Color(0xFF9C27B0).copy(alpha = 0.4f * alpha),
                        Color.Transparent
                    ),
                    startY = startY,
                    endY = startY + height * 0.4f
                ),
                start = Offset(x, startY),
                end = Offset(x, startY + height * 0.4f),
                strokeWidth = 2f
            )
        }
        
        // 脉冲光点
        for (i in 0..15) {
            val x = width * (i % 4) / 4f + width / 8f
            val y = height * (i / 4) / 4f + height / 8f
            val pulseSize = 8f + pulseProgress * 12f
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE91E63).copy(alpha = 0.8f * alpha * (1f - pulseProgress)),
                        Color.Transparent
                    )
                ),
                radius = pulseSize,
                center = Offset(x, y)
            )
        }
    }
}

// 幸运模式背景 - 超华丽金色粒子雨与光芒
@Composable
fun LuckyModeBackground(alpha: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "lucky")
    val rainProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain"
    )
    
    val glowProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // 背景放射光芒
        for (i in 0..12) {
            val angle = (i * 30f + glowProgress * 360f) * Math.PI.toFloat() / 180f
            val startRadius = 100f
            val endRadius = width.coerceAtLeast(height)
            
            val startX = width / 2f + startRadius * cos(angle)
            val startY = height / 2f + startRadius * sin(angle)
            val endX = width / 2f + endRadius * cos(angle)
            val endY = height / 2f + endRadius * sin(angle)
            
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFD700).copy(alpha = 0.3f * alpha * glowProgress),
                        Color(0xFFFFA500).copy(alpha = 0.15f * alpha * glowProgress),
                        Color.Transparent
                    ),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY)
                ),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 4f
            )
        }
        
        // 金色粒子雨 - 增强版
        for (i in 0..50) {
            val x = width * (i % 10) / 10f + (i * 17f) % 60f
            val y = (rainProgress * height + (i * 50f)) % height
            val size = 10f + (i % 5) * 5f
            
            // 粒子光晕
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD700).copy(alpha = 0.6f * alpha),
                        Color.Transparent
                    )
                ),
                radius = size * 2f,
                center = Offset(x, y)
            )
            
            // 粒子主体
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF).copy(alpha = 0.9f * alpha),
                        Color(0xFFFFD700).copy(alpha = 0.8f * alpha),
                        Color(0xFFFFB300).copy(alpha = 0.5f * alpha),
                        Color.Transparent
                    )
                ),
                radius = size,
                center = Offset(x, y)
            )
            
            // 拖尾效果 - 更长更华丽
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFD700).copy(alpha = 0.8f * alpha),
                        Color(0xFFFFA500).copy(alpha = 0.4f * alpha),
                        Color.Transparent
                    ),
                    startY = y,
                    endY = y + size * 5f
                ),
                start = Offset(x, y),
                end = Offset(x, y + size * 5f),
                strokeWidth = size * 0.6f
            )
        }
        
        // 闪烁的星星 - 增强版
        for (i in 0..25) {
            val starX = width * (i % 6) / 6f + (i * 23f) % 80f
            val starY = height * (i / 6) / 5f + (i * 31f) % 60f
            val starAlpha = ((rainProgress + i * 0.08f) % 1f)
            val starSize = 4f + (i % 3) * 2f
            
            // 星星光晕
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF).copy(alpha = starAlpha * 0.6f * alpha),
                        Color.Transparent
                    )
                ),
                radius = starSize * 3f,
                center = Offset(starX, starY)
            )
            
            // 星星主体
            drawCircle(
                color = Color(0xFFFFFFFF).copy(alpha = starAlpha * alpha),
                radius = starSize,
                center = Offset(starX, starY)
            )
            
            // 十字光芒
            val crossSize = starSize * 2f
            drawLine(
                color = Color(0xFFFFFFFF).copy(alpha = starAlpha * 0.7f * alpha),
                start = Offset(starX - crossSize, starY),
                end = Offset(starX + crossSize, starY),
                strokeWidth = 1.5f
            )
            drawLine(
                color = Color(0xFFFFFFFF).copy(alpha = starAlpha * 0.7f * alpha),
                start = Offset(starX, starY - crossSize),
                end = Offset(starX, starY + crossSize),
                strokeWidth = 1.5f
            )
        }
        
        // 漂浮的金币
        for (i in 0..8) {
            val coinX = width * (i % 3) / 3f + width / 6f
            val coinY = (height * (i / 3) / 3f + glowProgress * 100f) % height
            val coinSize = 20f + (i % 2) * 10f
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD700).copy(alpha = 0.9f * alpha),
                        Color(0xFFFFA500).copy(alpha = 0.6f * alpha)
                    )
                ),
                radius = coinSize,
                center = Offset(coinX, coinY)
            )
            
            drawCircle(
                color = Color(0xFFFFE082).copy(alpha = 0.8f * alpha),
                radius = coinSize * 0.6f,
                center = Offset(coinX, coinY)
            )
        }
    }
}

// 连抽结算动画 - 全新美化版（清新风格）
@Composable
fun MultiSpinResultAnimation(results: String, onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(true) }
    var scale by remember { mutableFloatStateOf(0f) }
    var alpha by remember { mutableFloatStateOf(1f) }
    
    // 解析结果
    val resultItems = results.split(", ").map { item ->
        val parts = item.split("×")
        if (parts.size == 2) {
            Pair(parts[0], parts[1].toIntOrNull() ?: 1)
        } else {
            Pair(item, 1)
        }
    }
    
    LaunchedEffect(Unit) {
        animate(0f, 1.2f, animationSpec = tween(400, easing = FastOutSlowInEasing)) { v, _ -> scale = v }
        animate(1.2f, 1f, animationSpec = tween(250, easing = FastOutSlowInEasing)) { v, _ -> scale = v }
        delay(4000)
        animate(1f, 0f, animationSpec = tween(500)) { v, _ -> alpha = v }
        visible = false
        onDismiss()
    }
    
    if (visible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f * alpha))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(0.92f)
                    .graphicsLayer { 
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    },
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(
                    3.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFD700),
                            Color(0xFFFFA500),
                            Color(0xFFFFD700)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFFBF5),
                                    Color.White,
                                    Color.White
                                )
                            )
                        )
                        .padding(28.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 顶部庆祝图标 - 更大更醒目
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFD700).copy(alpha = 0.2f),
                                        Color(0xFFFFA500).copy(alpha = 0.1f),
                                        Color.Transparent
                                    )
                                ),
                                shape = RoundedCornerShape(50)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎊", fontSize = 64.sp)
                    }
                    
                    // 主标题 - 更醒目
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "连抽完成",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 36.sp,
                            color = Color(0xFF2C3E50)
                        )
                        Text(
                            text = "Multi-Draw Complete",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF95A5A6),
                            fontSize = 13.sp
                        )
                    }
                    
                    // 副标题
                    Text(
                        text = "✨ 恭喜获得以下奖励 ✨",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF7F8C8D),
                        fontSize = 16.sp
                    )
                    
                    // 渐变分隔线
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(3.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFFFFD700).copy(alpha = 0.5f),
                                        Color(0xFFFFA500).copy(alpha = 0.5f),
                                        Color.Transparent
                                    )
                                ),
                                shape = RoundedCornerShape(50)
                            )
                    )
                    
                    Spacer(Modifier.height(4.dp))
                    
                    // 结果容器 - 清新卡片
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFFFFF8F0),
                        shadowElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // 标题栏
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🎁", fontSize = 24.sp)
                                    Text(
                                        text = "抽取结果",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE67E22),
                                        fontSize = 20.sp
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF00BCD4).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "共${resultItems.sumOf { it.second }}个",
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00BCD4),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            
                            // 结果列表 - 清新设计
                            resultItems.forEach { item ->
                                val name = item.first
                                val count = item.second
                                
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.White,
                                    shadowElevation = 0.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // 图标
                                            Box(
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .background(
                                                        brush = Brush.linearGradient(
                                                            colors = listOf(
                                                                Color(0xFFFFD700),
                                                                Color(0xFFFFA500)
                                                            )
                                                        ),
                                                        shape = RoundedCornerShape(16.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("✨", fontSize = 28.sp)
                                            }
                                            
                                            // 文字
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = name,
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2C3E50),
                                                    fontSize = 19.sp
                                                )
                                                Text(
                                                    text = "Lucky Item",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF95A5A6),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                        
                                        // 数量标签
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(
                                                            Color(0xFF00BCD4),
                                                            Color(0xFF00ACC1)
                                                        )
                                                    ),
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .padding(horizontal = 18.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = "×$count",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                fontSize = 22.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(4.dp))
                    
                    // 底部提示 - 更柔和
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👆", fontSize = 14.sp)
                        Text(
                            text = "点击任意处关闭",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF95A5A6),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}


