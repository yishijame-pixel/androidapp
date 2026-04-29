// ImageBasedSpinWheel.kt - 基于图片的转盘组件
package com.example.funlife.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * 辅助数据类：用于返回4个值
 */
private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

/**
 * 转盘模式枚举
 */
enum class WheelMode(val wheelImage: String, val pointerImage: String) {
    NORMAL("dibu/zp1.png", "dibu/zz.png"),        // 普通转盘
    ADVANCED("dibu/jj-zp.png", "dibu/jj-zz.png"), // 进阶转盘
    LUCKY("dibu/xy-zp.png", "dibu/xy-zj.png")     // 幸运转盘
}

/**
 * 基于图片的转盘组件
 * 
 * @param options 转盘选项列表
 * @param canSpin 是否可以旋转
 * @param onResult 旋转结束回调
 * @param onSpinStart 开始旋转回调
 * @param autoSpinTrigger 外部触发旋转的计数器，每次变化时触发旋转
 * @param forceResult 强制指定结果（用于幸运值系统）
 * @param wheelMode 转盘模式：NORMAL(普通), ADVANCED(进阶), LUCKY(幸运)
 * @param modifier 修饰符
 */
@Composable
fun ImageBasedSpinWheel(
    options: List<String>,
    canSpin: Boolean = true,
    onResult: (String) -> Unit = {},
    onSpinStart: () -> Unit = {},
    autoSpinTrigger: Int = 0,
    forceResult: String? = null,
    wheelMode: WheelMode = WheelMode.NORMAL,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 根据模式加载转盘图片
    val wheelBitmap = remember(wheelMode) {
        try {
            context.assets.open(wheelMode.wheelImage).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageBasedSpinWheel", "Failed to load wheel image: ${e.message}")
            null
        }
    }
    
    // 根据模式加载指针图片
    val pointerBitmap = remember(wheelMode) {
        try {
            context.assets.open(wheelMode.pointerImage).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageBasedSpinWheel", "Failed to load pointer image: ${e.message}")
            null
        }
    }
    
    // 旋转状态（使用 Animatable 实现流畅动画）
    val rotationAnimatable = remember { Animatable(0f) }
    var isSpinning by remember { mutableStateOf(false) }
    
    // 🔥 记住上一次的 autoSpinTrigger 值，只在真正变化时触发
    // 使用 remember(key) 确保在组件重新创建时正确初始化
    var lastAutoSpinTrigger by remember(wheelMode, options.size) { mutableIntStateOf(autoSpinTrigger) }
    
    // 监听外部触发
    LaunchedEffect(autoSpinTrigger) {
        // 🔥 只有当 autoSpinTrigger 真正变化且大于0时才触发旋转
        if (autoSpinTrigger > 0 && autoSpinTrigger != lastAutoSpinTrigger && !isSpinning && options.isNotEmpty()) {
            android.util.Log.d("ImageBasedSpinWheel", "=== Spin Triggered ===")
            android.util.Log.d("ImageBasedSpinWheel", "autoSpinTrigger: $autoSpinTrigger, lastAutoSpinTrigger: $lastAutoSpinTrigger")
            lastAutoSpinTrigger = autoSpinTrigger
            isSpinning = true
            onSpinStart()
            
            // 确定结果
            val result = if (forceResult != null && options.contains(forceResult)) {
                forceResult
            } else {
                options[Random.nextInt(options.size)]
            }
            
            val resultIndex = options.indexOf(result)
            
            android.util.Log.d("ImageBasedSpinWheel", "=== SPIN START ===")
            android.util.Log.d("ImageBasedSpinWheel", "Wheel Mode: $wheelMode")
            android.util.Log.d("ImageBasedSpinWheel", "Options count: ${options.size}")
            android.util.Log.d("ImageBasedSpinWheel", "Options: ${options.joinToString(", ")}")
            android.util.Log.d("ImageBasedSpinWheel", "Target Result: $result")
            android.util.Log.d("ImageBasedSpinWheel", "Target Result Index: $resultIndex")
            
            // ========== 核心算法：精确计算目标旋转角度 ==========
            val degreesPerOption = 360f / options.size
            
            // 获取当前转盘的旋转角度
            val currentRotation = rotationAnimatable.value % 360f
            val normalizedCurrentRotation = if (currentRotation < 0) currentRotation + 360f else currentRotation
            
            // 🎲 添加随机偏移，让指针可以停在扇形区域的任意位置
            // 偏移范围：扇形区域的 -40% 到 +40%（避免太靠近边缘）
            val randomOffset = (Random.nextFloat() - 0.5f) * degreesPerOption * 0.8f
            
            // 计算目标选项的中心角度（未旋转时）
            // 🎯 修正：index=0 在顶部（-90度），所以需要减去90度
            // 由于文字有 +90f 的旋转，两者抵消，所以这里直接计算即可
            val targetOptionInitialAngle = (resultIndex * degreesPerOption) + (degreesPerOption / 2f) + randomOffset
            
            // 计算目标选项当前的实际角度
            var targetOptionCurrentAngle = (targetOptionInitialAngle + normalizedCurrentRotation) % 360f
            if (targetOptionCurrentAngle < 0) targetOptionCurrentAngle += 360f
            
            // 计算需要旋转多少度才能让目标选项对准指针（0度顶部）
            var additionalRotation = 0f - targetOptionCurrentAngle
            
            // 确保是正向旋转
            if (additionalRotation < 0) additionalRotation += 360f
            
            // 总旋转 = 当前角度 + 5整圈 + 对准指针所需的额外旋转
            val totalRotation = rotationAnimatable.value + 360f * 5 + additionalRotation
            
            android.util.Log.d("ImageBasedSpinWheel", "--- Calculation Details ---")
            android.util.Log.d("ImageBasedSpinWheel", "Degrees per option: ${String.format("%.2f", degreesPerOption)}°")
            android.util.Log.d("ImageBasedSpinWheel", "Random offset: ${String.format("%.2f", randomOffset)}°")
            android.util.Log.d("ImageBasedSpinWheel", "Current rotation (normalized): ${String.format("%.2f", normalizedCurrentRotation)}°")
            android.util.Log.d("ImageBasedSpinWheel", "Target option initial angle: ${String.format("%.2f", targetOptionInitialAngle)}°")
            android.util.Log.d("ImageBasedSpinWheel", "Target option current angle: ${String.format("%.2f", targetOptionCurrentAngle)}°")
            android.util.Log.d("ImageBasedSpinWheel", "Additional rotation needed: ${String.format("%.2f", additionalRotation)}°")
            android.util.Log.d("ImageBasedSpinWheel", "Total rotation target: ${String.format("%.2f", totalRotation)}°")
            
            // 执行动画
            rotationAnimatable.animateTo(
                targetValue = totalRotation,
                animationSpec = tween(
                    durationMillis = 3000,
                    easing = FastOutSlowInEasing
                )
            )
            
            isSpinning = false
            delay(300)
            
            // ========== 验证算法：反向计算最终结果 ==========
            val finalRotation = rotationAnimatable.value % 360f
            val normalizedFinalRotation = if (finalRotation < 0) finalRotation + 360f else finalRotation
            
            android.util.Log.d("ImageBasedSpinWheel", "=== VERIFICATION ===")
            android.util.Log.d("ImageBasedSpinWheel", "Final rotation: ${String.format("%.2f", normalizedFinalRotation)}°")
            
            // 找到旋转后最接近指针（0度）的选项
            var verifiedResultIndex = 0
            var minAngleDiff = 360f
            
            for (i in options.indices) {
                // 选项 i 的初始中心角度
                // 🎯 修正：index=0 在顶部（-90度）
                val initialAngle = (i * degreesPerOption) + (degreesPerOption / 2f)
                
                // 旋转后的角度
                var rotatedAngle = (initialAngle + normalizedFinalRotation) % 360f
                if (rotatedAngle < 0) rotatedAngle += 360f
                
                // 与指针（0度）的角度差（考虑360度循环）
                var angleDiff = Math.abs(rotatedAngle - 0f)
                if (angleDiff > 180f) angleDiff = 360f - angleDiff
                
                android.util.Log.d("ImageBasedSpinWheel", "  Option[$i] '${options[i]}': initial=${String.format("%.2f", initialAngle)}°, rotated=${String.format("%.2f", rotatedAngle)}°, diff=${String.format("%.2f", angleDiff)}°")
                
                if (angleDiff < minAngleDiff) {
                    minAngleDiff = angleDiff
                    verifiedResultIndex = i
                }
            }
            
            val verifiedResult = options[verifiedResultIndex]
            
            android.util.Log.d("ImageBasedSpinWheel", "=== FINAL RESULT ===")
            android.util.Log.d("ImageBasedSpinWheel", "Verified result index: $verifiedResultIndex")
            android.util.Log.d("ImageBasedSpinWheel", "Verified result: $verifiedResult")
            android.util.Log.d("ImageBasedSpinWheel", "Min angle diff: ${String.format("%.2f", minAngleDiff)}°")
            android.util.Log.d("ImageBasedSpinWheel", "Expected result: $result")
            android.util.Log.d("ImageBasedSpinWheel", "Match: ${if (verifiedResult == result) "✓ SUCCESS" else "✗ FAILED"}")
            
            // 使用验证后的结果
            onResult(verifiedResult)
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        // 转盘图片和文字（一起旋转）
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = rotationAnimatable.value
                },
            contentAlignment = Alignment.Center
        ) {
            val boxSize = maxWidth
            
            // 转盘背景图片
            if (wheelBitmap != null) {
                Image(
                    bitmap = wheelBitmap,
                    contentDescription = "转盘",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            
            // 🔥 调试模式：显示索引号来确定正确的数组顺序
            val debugMode = false  // 设置为 true 显示索引号，false 显示文字
            
            // 在转盘上绘制选项文字或索引号
            options.forEachIndexed { index, option ->
                val degreesPerOption = 360f / options.size
                // 🎯 修正：让 index=0 对应到顶部（-90度位置）
                // 原始计算会让 index=0 在右侧（0度），需要减去90度让它到顶部
                val angle = (index * degreesPerOption) + (degreesPerOption / 2f) - 90f
                val angleRad = Math.toRadians(angle.toDouble())
                
                // 根据转盘模式选择字体大小和文字半径
                val (fontSize, radius) = when(wheelMode) {
                    WheelMode.NORMAL -> Pair(16.sp, boxSize * 0.23f)
                    WheelMode.ADVANCED -> Pair(14.sp, boxSize * 0.23f)
                    WheelMode.LUCKY -> Pair(11.sp, boxSize * 0.18f)
                }
                
                val offsetX = (radius.value * Math.cos(angleRad)).toFloat().dp
                val offsetY = (radius.value * Math.sin(angleRad)).toFloat().dp
                
                // 🎯 文字旋转角度：让文字始终朝向圆心外侧（垂直于半径方向）
                // angle 是从右侧（0度）开始顺时针的角度
                // 文字需要旋转 angle + 90度 才能垂直于半径
                val textRotation = angle + 90f
                
                // 🔥 调试模式：显示索引号（大号红色）
                if (debugMode) {
                    androidx.compose.material3.Text(
                        text = "[$index]",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = offsetX, y = offsetY)
                            .graphicsLayer {
                                rotationZ = textRotation
                            },
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 24.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                            color = androidx.compose.ui.graphics.Color.Red,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = androidx.compose.ui.graphics.Color.White,
                                offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                blurRadius = 4f
                            )
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                } else {
                    // 正常模式：显示文字（多层描边）
                    val (shadowColor, darkStrokeColor, midStrokeColor) = when(wheelMode) {
                        WheelMode.NORMAL -> Triple(
                            androidx.compose.ui.graphics.Color(0xFF4A148C),
                            androidx.compose.ui.graphics.Color(0xFF7B1FA2),
                            androidx.compose.ui.graphics.Color(0xFF9C27B0)
                        )
                        WheelMode.ADVANCED -> Triple(
                            androidx.compose.ui.graphics.Color(0xFFD81B60),
                            androidx.compose.ui.graphics.Color(0xFFEC407A),
                            androidx.compose.ui.graphics.Color(0xFFF06292)
                        )
                        WheelMode.LUCKY -> Triple(
                            androidx.compose.ui.graphics.Color(0xFFE65100),
                            androidx.compose.ui.graphics.Color(0xFFFF6F00),
                            androidx.compose.ui.graphics.Color(0xFFFF9800)
                        )
                    }
                    
                    // 阴影层
                    androidx.compose.material3.Text(
                        text = option,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = offsetX + 1.5.dp, y = offsetY + 1.5.dp)
                            .graphicsLayer { rotationZ = textRotation },
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = fontSize,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                            color = shadowColor,
                            drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 8f,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            )
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    // 深色描边
                    androidx.compose.material3.Text(
                        text = option,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = offsetX, y = offsetY)
                            .graphicsLayer { rotationZ = textRotation },
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = fontSize,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                            color = darkStrokeColor,
                            drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 6f,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            )
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    // 中色描边
                    androidx.compose.material3.Text(
                        text = option,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = offsetX, y = offsetY)
                            .graphicsLayer { rotationZ = textRotation },
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = fontSize,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                            color = midStrokeColor,
                            drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 4f,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            )
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    // 白色填充
                    androidx.compose.material3.Text(
                        text = option,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = offsetX, y = offsetY)
                            .graphicsLayer { rotationZ = textRotation },
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = fontSize,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                            color = androidx.compose.ui.graphics.Color.White
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        
        // 指针图片（固定在中心，不旋转）
        if (pointerBitmap != null) {
            Image(
                bitmap = pointerBitmap,
                contentDescription = "指针",
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit
            )
        }
    }
}
