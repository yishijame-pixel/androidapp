// AnniversaryGridView.kt - 纪念日网格视图
package com.example.funlife.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.funlife.data.model.Anniversary

@Composable
fun AnniversaryGridView(
    anniversaries: List<Anniversary>,
    onDelete: (Anniversary) -> Unit,
    onPin: (Anniversary) -> Unit,
    onEdit: (Anniversary) -> Unit,
    onShare: (Anniversary) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
