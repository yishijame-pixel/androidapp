// LuckyValueProgressBarDemo.kt - 幸运值进度条使用示例
package com.example.funlife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/**
 * 幸运值进度条演示页面
 * 
 * 展示如何使用 LuckyValueProgressBar 组件
 */
@Composable
fun LuckyValueProgressBarDemo() {
    var luckyValue by remember { mutableIntStateOf(0) }
    val maxValue = 100
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        Text(
            text = "幸运值进度条组件演示",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // 主组件
        LuckyValueProgressBar(
            currentValue = luckyValue,
            maxValue = maxValue,
            onDiceClick = {
                // 点击骰子增加幸运值
                val increment = Random.nextInt(1, 11)
                luckyValue = (luckyValue + increment).coerceAtMost(maxValue)
            }
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // 控制按钮
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2D2D44)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "控制面板",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { luckyValue = (luckyValue + 10).coerceAtMost(maxValue) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("+10")
                    }
                    
                    Button(
                        onClick = { luckyValue = (luckyValue - 10).coerceAtLeast(0) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5722)
                        )
                    ) {
                        Text("-10")
                    }
                    
                    Button(
                        onClick = { luckyValue = 0 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF9E9E9E)
                        )
                    ) {
                        Text("重置")
                    }
                    
                    Button(
                        onClick = { luckyValue = maxValue },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD700)
                        )
                    ) {
                        Text("满值")
                    }
                }
                
                Text(
                    text = "当前值: $luckyValue / $maxValue",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // 组件特性说明
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2D2D44)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "✨ 组件特性",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFFFD700)
                )
                
                FeatureItem("🎨 5层结构设计")
                FeatureItem("🌈 彩虹渐变进度条")
                FeatureItem("✨ 星光粒子闪烁动画")
                FeatureItem("💫 流光动画效果")
                FeatureItem("🎲 骰子按钮呼吸光效")
                FeatureItem("🔆 多层发光效果")
                FeatureItem("🎯 高度还原设计稿")
            }
        }
    }
}

@Composable
private fun FeatureItem(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.8f)
    )
}

/**
 * 简单使用示例
 */
@Composable
fun SimpleLuckyValueExample() {
    var luckyValue by remember { mutableIntStateOf(50) }
    
    LuckyValueProgressBar(
        currentValue = luckyValue,
        maxValue = 100,
        onDiceClick = {
            luckyValue = (luckyValue + Random.nextInt(1, 11)).coerceAtMost(100)
        }
    )
}

/**
 * 自定义最大值示例
 */
@Composable
fun CustomMaxValueExample() {
    var luckyValue by remember { mutableIntStateOf(0) }
    val customMaxValue = 200
    
    LuckyValueProgressBar(
        currentValue = luckyValue,
        maxValue = customMaxValue,
        onDiceClick = {
            luckyValue = (luckyValue + Random.nextInt(5, 21)).coerceAtMost(customMaxValue)
        }
    )
}
