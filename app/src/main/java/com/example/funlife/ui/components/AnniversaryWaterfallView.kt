// AnniversaryWaterfallView.kt - 纪念日瀑布流视图
package com.example.funlife.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.funlife.data.model.Anniversary

@Composable
fun AnniversaryWaterfallView(
    anniversaries: List<Anniversary>,
    onDelete: (Anniversary) -> Unit,
    onPin: (Anniversary) -> Unit,
    onEdit: (Anniversary) -> Unit,
    onShare: (Anniversary) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp
    ) {
        items(anniversaries, key = { it.id }) { anniversary ->
            AnniversaryCard(
                anniversary = anniversary,
                onDelete = { onDelete(anniversary) },
                onPin = { onPin(anniversary) },
                onEdit = { onEdit(anniversary) },
                onShare = { onShare(anniversary) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
