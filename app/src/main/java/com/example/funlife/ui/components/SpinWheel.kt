// SpinWheel.kt - 转盘组件（完全重写版）
package com.example.funlife.ui.components

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
    
    val scope = rememberCoroutineScope()
    var isSpinning by remember { mutableStateOf(false) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var selectedResult by remember { mutableStateOf("") }
    
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
    
    // 自动旋转触发已移除，改为直接在按钮点击时处理
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp) // 从32dp减少到16dp
    ) {
        // 移除顶部Spacer，直接开始转盘
        
        // 转盘容器 - 简洁设计
        Box(
            modifier = Modifier.size(320.dp),
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
        
        // 旋转按钮（减少间距）
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
                    Log.d(TAG, "Calling onSpinStart()")
                    onSpinStart()
                    
                    // 🔥 修复：使用 Compose 协程作用域
                    scope.launch {
                        try {
                            Log.d(TAG, "Starting spin animation (mode: ${if (multiSpinMode) "MULTI" else "SINGLE"})")
                            isSpinning = true
                            
                            val targetRotation = rotation + 360 * 5 + Random.nextFloat() * 360
                            Log.d(TAG, "Current rotation: $rotation, Target rotation: $targetRotation")
                            
                            animate(
                                initialValue = rotation,
                                targetValue = targetRotation,
                                animationSpec = tween(2000, easing = FastOutSlowInEasing)
                            ) { value, _ -> 
                                rotation = value
                            }
                            
                            rotation = targetRotation % 360
                            
                            val anglePerOption = 360f / options.size
                            val normalizedRotation = (360 - rotation % 360) % 360
                            val resultIndex = ((normalizedRotation + anglePerOption / 2) / anglePerOption).toInt() % options.size
                            selectedResult = options[resultIndex]
                            
                            Log.d(TAG, "Spin completed - result: $selectedResult (index: $resultIndex)")
                            isSpinning = false
                            onResult(selectedResult)
                            onShowResult(selectedResult)
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
                
                val drawY = fillPaint.textSize / 3
                nativeCanvas.drawText(option.text, 0f, drawY + 2f, shadowPaint)
                nativeCanvas.drawText(option.text, 0f, drawY, strokePaint)
                nativeCanvas.drawText(option.text, 0f, drawY, fillPaint)
                
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

// 结果动画 - 华丽礼物盒粒子效果
@Composable
fun ResultAnimation(result: String, onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(true) }
    var scale by remember { mutableFloatStateOf(0f) }
    var alpha by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableFloatStateOf(-10f) }
    var particles by remember { mutableStateOf<List<Particle>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        // 创建大量礼物盒粒子 - 增加到300个
        particles = List(300) {
            val angle = Random.nextFloat() * 360f
            val speed = Random.nextFloat() * 12f + 6f
            Particle(
                x = 0f,
                y = 0f,
                vx = cos(Math.toRadians(angle.toDouble())).toFloat() * speed,
                vy = sin(Math.toRadians(angle.toDouble())).toFloat() * speed - 5f,
                life = 1f,
                maxLife = 1f,
                color = listOf(
                    Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFFF6B9D),
                    Color(0xFF4FACFE), Color(0xFFFFFFFF), Color(0xFFFFE4B5),
                    Color(0xFFFF1744), Color(0xFF00E676), Color(0xFF00B0FF),
                    Color(0xFFFFEA00), Color(0xFFE040FB), Color(0xFFFF4081),
                    Color(0xFF69F0AE), Color(0xFF40C4FF), Color(0xFFFFAB91),
                    Color(0xFFCE93D8), Color(0xFF80DEEA), Color(0xFFFFF176)
                ).random(),
                size = Random.nextFloat() * 14f + 8f
            )
        }
        
        // 弹跳动画
        animate(0f, 1.5f, animationSpec = tween(250, easing = FastOutSlowInEasing)) { v, _ -> scale = v }
        animate(1.5f, 0.95f, animationSpec = tween(150)) { v, _ -> scale = v }
        animate(0.95f, 1.05f, animationSpec = tween(100)) { v, _ -> scale = v }
        animate(1.05f, 1f, animationSpec = tween(100)) { v, _ -> scale = v }
        
        // 旋转动画
        animate(-10f, 10f, animationSpec = tween(200)) { v, _ -> rotation = v }
        animate(10f, -5f, animationSpec = tween(150)) { v, _ -> rotation = v }
        animate(-5f, 0f, animationSpec = tween(100)) { v, _ -> rotation = v }
        
        // 粒子动画
        var time = 0f
        while (time < 3000f) {
            withFrameNanos { _ ->
                time += 16f
                particles = particles.map { particle ->
                    particle.copy(
                        x = particle.x + particle.vx,
                        y = particle.y + particle.vy,
                        vx = particle.vx * 0.98f,
                        vy = particle.vy + 0.18f, // 重力
                        life = particle.life - 0.006f
                    )
                }.filter { it.life > 0 }
            }
        }
        
        // 淡出
        animate(1f, 0f, animationSpec = tween(500)) { v, _ -> alpha = v }
        visible = false
        onDismiss()
    }
    
    if (visible) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f * alpha)),
            Alignment.Center
        ) {
            // 礼物盒粒子效果层
            Canvas(Modifier.fillMaxSize()) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                
                particles.forEach { particle ->
                    val particleAlpha = (particle.life / particle.maxLife).coerceIn(0f, 1f) * alpha
                    val particleX = centerX + particle.x
                    val particleY = centerY + particle.y
                    val boxSize = particle.size
                    
                    // 礼物盒光晕
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                particle.color.copy(alpha = particleAlpha * 0.6f),
                                particle.color.copy(alpha = particleAlpha * 0.3f),
                                Color.Transparent
                            ),
                            radius = boxSize * 3f
                        ),
                        radius = boxSize * 3f,
                        center = Offset(particleX, particleY)
                    )
                    
                    // 礼物盒主体
                    drawRoundRect(
                        color = particle.color.copy(alpha = particleAlpha),
                        topLeft = Offset(particleX - boxSize * 0.5f, particleY - boxSize * 0.4f),
                        size = androidx.compose.ui.geometry.Size(boxSize, boxSize * 0.8f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(boxSize * 0.15f)
                    )
                    
                    // 礼物盒渐变高光
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = particleAlpha * 0.5f),
                                Color.Transparent
                            )
                        ),
                        topLeft = Offset(particleX - boxSize * 0.5f, particleY - boxSize * 0.4f),
                        size = androidx.compose.ui.geometry.Size(boxSize, boxSize * 0.4f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(boxSize * 0.15f)
                    )
                    
                    // 丝带（垂直）
                    drawRect(
                        color = Color.White.copy(alpha = particleAlpha * 0.95f),
                        topLeft = Offset(particleX - boxSize * 0.1f, particleY - boxSize * 0.4f),
                        size = androidx.compose.ui.geometry.Size(boxSize * 0.2f, boxSize * 0.8f)
                    )
                    
                    // 丝带（水平）
                    drawRect(
                        color = Color.White.copy(alpha = particleAlpha * 0.95f),
                        topLeft = Offset(particleX - boxSize * 0.5f, particleY - boxSize * 0.1f),
                        size = androidx.compose.ui.geometry.Size(boxSize, boxSize * 0.2f)
                    )
                    
                    // 蝴蝶结装饰
                    drawCircle(
                        color = Color.White.copy(alpha = particleAlpha),
                        radius = boxSize * 0.25f,
                        center = Offset(particleX - boxSize * 0.25f, particleY - boxSize * 0.5f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = particleAlpha),
                        radius = boxSize * 0.25f,
                        center = Offset(particleX + boxSize * 0.25f, particleY - boxSize * 0.5f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = particleAlpha),
                        radius = boxSize * 0.15f,
                        center = Offset(particleX, particleY - boxSize * 0.5f)
                    )
                }
            }
            
            // 主卡片
            Card(
                modifier = Modifier
                    .padding(32.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = rotation
                        this.alpha = alpha
                    },
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.cardElevation(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text("🎉", fontSize = 64.sp)
                    Text("恭喜抽中", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(result, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(5) { Text("⭐", fontSize = 20.sp) }
                    }
                }
            }
        }
    }
}

// 连抽结算动画
@Composable
fun MultiSpinResultAnimation(results: String, onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(true) }
    var scale by remember { mutableFloatStateOf(0f) }
    var alpha by remember { mutableFloatStateOf(1f) }
    
    LaunchedEffect(Unit) {
        animate(0f, 1.3f, animationSpec = tween(300, easing = FastOutSlowInEasing)) { v, _ -> scale = v }
        animate(1.3f, 1f, animationSpec = tween(200)) { v, _ -> scale = v }
        delay(3500)
        animate(1f, 0f, animationSpec = tween(600)) { v, _ -> alpha = v }
        visible = false
        onDismiss()
    }
    
    if (visible) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Card(
                modifier = Modifier.padding(24.dp).fillMaxWidth(0.92f).graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha },
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.cardElevation(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(36.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    Text("🎊🎉🎊", fontSize = 56.sp)
                    Text("连抽完成", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp)
                    Text("✨ 恭喜获得以下奖励 ✨", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    Divider(Modifier.fillMaxWidth(0.7f), thickness = 3.dp)
                    
                    Card(elevation = CardDefaults.cardElevation(8.dp), shape = RoundedCornerShape(24.dp)) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("🎁 抽取结果", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            Divider(thickness = 1.dp)
                            results.split(", ").forEach { item ->
                                Card(shape = RoundedCornerShape(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(item, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("✨🌟", style = MaterialTheme.typography.titleLarge)
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
