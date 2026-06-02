// ═══════════════════════════════════════════════════════════════════════════
// BookCustomizationSheet.kt — 编辑魔法书书名 & 刻印署名
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.diarybook.skin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.R
import com.example.funlife.data.DiaryBookCustomizationStore
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.ui.components.diarybook.BookStageTheme
import com.example.funlife.ui.components.diarybook.bookStageThemeFor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookCustomizationSheet(
    userId: Long,
    skinRawId: String,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    val stage = bookStageThemeFor(skinRawId)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val customizationState = LocalBookCustomizationState.current
        ?: return

    val defaultTitle = stringResource(R.string.diary_book_default_title)
    val defaultSubtitle = stringResource(R.string.diary_book_default_subtitle)

    var titleInput by remember {
        mutableStateOf(
            customizationState.customization.bookTitle.ifBlank { defaultTitle },
        )
    }
    var ownerInput by remember {
        mutableStateOf(customizationState.customization.ownerName)
    }

    // 首次打开且未设署名时，用主玩家名作占位提示（不自动写入，用户确认才保存）
    var ownerPlaceholder by remember { mutableStateOf("") }
    LaunchedEffect(userId) {
        val player = runCatching {
            AppDatabase.getDatabase(ctx).playerDao()
                .getAllPlayers(userId).first().firstOrNull()?.name
        }.getOrNull()
        if (!player.isNullOrBlank()) {
            ownerPlaceholder = player.take(DiaryBookCustomizationStore.MAX_OWNER_LEN)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = stage.bgMid,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.diary_book_customize_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = stage.title,
                letterSpacing = 3.sp,
            )
            Text(
                text = stringResource(R.string.diary_book_customize_hint),
                fontSize = 12.sp,
                color = stage.subtitle,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
            )

            CustomField(
                label = stringResource(R.string.diary_book_customize_book_title),
                value = titleInput,
                onValueChange = {
                    titleInput = DiaryBookCustomizationStore.sanitizeTitle(it)
                },
                stage = stage,
                maxLen = DiaryBookCustomizationStore.MAX_TITLE_LEN,
            )
            Spacer(Modifier.height(12.dp))
            CustomField(
                label = stringResource(R.string.diary_book_customize_owner_name),
                value = ownerInput,
                onValueChange = {
                    ownerInput = DiaryBookCustomizationStore.sanitizeOwner(it)
                },
                stage = stage,
                maxLen = DiaryBookCustomizationStore.MAX_OWNER_LEN,
                placeholder = ownerPlaceholder.ifBlank {
                    stringResource(R.string.diary_book_customize_owner_placeholder)
                },
            )

            Text(
                text = stringResource(
                    R.string.diary_book_customize_preview,
                    DiaryBookCustomizationStore.resolveTitle(
                        customizationState.customization.copy(
                            bookTitle = titleInput,
                            ownerName = ownerInput,
                        ),
                        defaultTitle,
                    ),
                    DiaryBookCustomizationStore.resolveOwnerLine(
                        customizationState.customization.copy(
                            bookTitle = titleInput,
                            ownerName = ownerInput,
                        ),
                        defaultSubtitle,
                    ),
                ),
                fontSize = 11.sp,
                color = stage.statLabel,
                modifier = Modifier.padding(top = 12.dp),
            )

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val t = titleInput.ifBlank { defaultTitle }
                    customizationState.update(
                        if (t == defaultTitle) "" else t,
                        ownerInput,
                    )
                    scope.launch {
                        sheetState.hide()
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.diary_book_customize_save))
            }
            TextButton(
                onClick = {
                    customizationState.reset()
                    titleInput = defaultTitle
                    ownerInput = ""
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, tint = stage.icon)
                Text(
                    text = stringResource(R.string.diary_book_customize_reset),
                    color = stage.subtitle,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun CustomField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    stage: BookStageTheme,
    maxLen: Int,
    placeholder: String = "",
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = stage.subtitle) },
        placeholder = if (placeholder.isNotBlank()) {
            { Text(placeholder, color = stage.statLabel) }
        } else null,
        singleLine = true,
        supportingText = {
            Text("${value.length}/$maxLen", color = stage.statLabel, fontSize = 11.sp)
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
        ),
        modifier = Modifier.fillMaxWidth(),
        textStyle = androidx.compose.ui.text.TextStyle(color = stage.title),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = stage.halo,
            unfocusedBorderColor = stage.statLabel.copy(alpha = 0.5f),
            cursorColor = stage.halo,
            focusedLabelColor = stage.halo,
        ),
    )
}
