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
 * 基于图片的转盘组件
 * 
 * @param options 转盘选项列表
 * @param canSpin 是否可以旋转
 * @param onResult 旋转结束回调
 * @param onSpinStart 开始旋转回调
 * @param autoSpinTrigger 外部触发旋转的计数器，每次变化时触发旋转
 * @param forceResult 强制指定结果（用于幸运值系统）
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 加载转盘图片
    val wheelBitmap = remember {
        try {
            context.assets.open("dibu/zp1.png").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageBasedSpinWheel", "Failed to load wheel image: ${e.message}")
            null
        }
    }
    
    // 加载指针图片
    val pointerBitmap = remember {
        try {
            context.assets.open("dibu/zz.png").use { inputStream ->
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
    
    // 监听外部触发
    LaunchedEffect(autoSpinTrigger) {
        if (autoSpinTrigger > 0 && !isSpinning && options.isNotEmpty()) {
            isSpinning = true
            onSpinStart()
            
            // 确定结果
            val result = if (forceResult != null && options.contains(forceResult)) {
                forceResult
            } else {
                options[Random.nextInt(options.size)]
            }
            
            val resultIndex = options.indexOf(result)
            
            // 计算目标角度（添加随机偏移，让指针可以停在扇形区域的任意位置）
            val degreesPerOption = 360f / options.size
            val randomOffset = Random.nextFloat() * degreesPerOption - (degreesPerOption / 2f) // 在扇形范围内随机偏移
            val targetAngle = 360f - (resultIndex * degreesPerOption) + (degreesPerOption / 2f) + randomOffset
            val totalRotation = rotationAnimatable.value + 360f * 5 + targetAngle // 5圈 + 目标角度
            
            // 使用 Animatable 实现流畅动画
            rotationAnimatable.animateTo(
                targetValue = totalRotation,
                animationSpec = tween(
                    durationMillis = 3000,
                    easing = FastOutSlowInEasing
                )
            )
            
            isSpinning = false
            
            // 延迟后返回结果
            delay(300)
            onResult(result)
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(enabled = canSpin && !isSpinning) {
                if (options.isEmpty()) return@clickable
                
                scope.launch {
                    isSpinning = true
                    onSpinStart()
                    
                    // 随机选择结果
                    val resultIndex = Random.nextInt(options.size)
                    val result = options[resultIndex]
                    
                    // 计算目标角度（添加随机偏移，让指针可以停在扇形区域的任意位置）
                    val degreesPerOption = 360f / options.size
                    val randomOffset = Random.nextFloat() * degreesPerOption - (degreesPerOption / 2f) // 在扇形范围内随机偏移
                    val targetAngle = 360f - (resultIndex * degreesPerOption) + (degreesPerOption / 2f) + randomOffset
                    val totalRotation = rotationAnimatable.value + 360f * 5 + targetAngle // 5圈 + 目标角度
                    
                    // 使用 Animatable 实现流畅动画
                    rotationAnimatable.animateTo(
                        targetValue = totalRotation,
                        animationSpec = tween(
                            durationMillis = 3000,
                            easing = FastOutSlowInEasing
                        )
                    )
                    
                    isSpinning = false
                    
                    // 延迟后返回结果
                    delay(300)
                    onResult(result)
                }
            },
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
            
            // 在转盘上绘制选项文字（白色字体 + 紫色渐变描边 + 3D立体膨胀感）
            options.forEachIndexed { index, option ->
                val degreesPerOption = 360f / options.size
                // 计算每个扇形区域的中心角度
                val angle = -90f + (index * degreesPerOption) + (degreesPerOption / 2f)
                val angleRad = Math.toRadians(angle.toDouble())
                // 文字位于扇形区域中间偏上的位置
                val radius = boxSize * 0.23f
                
                val offsetX = (radius.value * Math.cos(angleRad)).toFloat().dp
                val offsetY = (radius.value * Math.sin(angleRad)).toFloat().dp
                
                // 第一层：深紫色阴影（最外层，营造3D立体感）
                androidx.compose.material3.Text(
                    text = option,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = offsetX + 1.5.dp, y = offsetY + 1.5.dp)
                        .graphicsLayer {
                            rotationZ = angle + 90f
                        },
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 22.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                        color = androidx.compose.ui.graphics.Color(0xFF4A148C),
                        drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 8f,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                // 第二层：深紫色描边（膨胀效果）
                androidx.compose.material3.Text(
                    text = option,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = offsetX, y = offsetY)
                        .graphicsLayer {
                            rotationZ = angle + 90f
                        },
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 22.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                        color = androidx.compose.ui.graphics.Color(0xFF7B1FA2),
                        drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 6f,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                // 第三层：中紫色描边（渐变过渡）
                androidx.compose.material3.Text(
                    text = option,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = offsetX, y = offsetY)
                        .graphicsLayer {
                            rotationZ = angle + 90f
                        },
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 22.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                        color = androidx.compose.ui.graphics.Color(0xFF9C27B0),
                        drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 4f,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                // 第四层：白色填充（最内层）
                androidx.compose.material3.Text(
                    text = option,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = offsetX, y = offsetY)
                        .graphicsLayer {
                            rotationZ = angle + 90f
                        },
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 22.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                        color = androidx.compose.ui.graphics.Color.White
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
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
