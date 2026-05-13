// AnniversaryListView.kt - 纪念日列表视图
package com.example.funlife.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.funlife.data.model.Anniversary

@Composable
fun AnniversaryListView(
    anniversaries: List<Anniversary>,
    onDelete: (Anniversary) -> Unit,
    onPin: (Anniversary) -> Unit,
    onEdit: (Anniversary) -> Unit,
    onShare: (Anniversary) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(anniversaries, key = { it.id }) { anniversary ->
            AnniversaryCard(
                anniversary = anniversary,
                onDelete = { onDelete(anniversary) },
                onPin = { onPin(anniversary) },
                onEdit = { onEdit(anniversary) },
                onShare = { onShare(anniversary) },
                modifier = Modifier.fillMaxWidth(0.95f)
            )
        }
    }
}
