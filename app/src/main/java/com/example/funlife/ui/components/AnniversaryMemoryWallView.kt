// AnniversaryMemoryWallView.kt - 纪念日回忆墙视图（照片墙风格）
package com.example.funlife.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.funlife.data.model.Anniversary
import kotlin.random.Random

@Composable
fun AnniversaryMemoryWallView(
    anniversaries: List<Anniversary>,
    onDelete: (Anniversary) -> Unit,
    onPin: (Anniversary) -> Unit,
    onEdit: (Anniversary) -> Unit,
    onShare: (Anniversary) -> Unit,
    modifier: Modifier = Modifier
) {
    // 为每个卡片生成随机旋转角度和偏移（但保持一致性）
    val cardTransforms = remember(anniversaries.size) {
        anniversaries.mapIndexed { index, _ ->
            CardTransform(
                rotation = Random.nextFloat() * 6f - 3f, // -3° 到 +3°
                offsetX = Random.nextFloat() * 20f - 10f, // -10dp 到 +10dp
                scale = 0.95f + Random.nextFloat() * 0.1f // 0.95 到 1.05
            )
        }
    }
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(anniversaries, key = { _, item -> item.id }) { index, anniversary ->
            val transform = cardTransforms.getOrNull(index) ?: CardTransform()
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        rotationZ = transform.rotation
                        translationX = transform.offsetX
                        scaleX = transform.scale
                        scaleY = transform.scale
                    }
            ) {
                AnniversaryCard(
                    anniversary = anniversary,
                    onDelete = { onDelete(anniversary) },
                    onPin = { onPin(anniversary) },
                    onEdit = { onEdit(anniversary) },
                    onShare = { onShare(anniversary) },
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
            }
        }
    }
}

// 卡片变换数据类
private data class CardTransform(
    val rotation: Float = 0f,
    val offsetX: Float = 0f,
    val scale: Float = 1f
)
