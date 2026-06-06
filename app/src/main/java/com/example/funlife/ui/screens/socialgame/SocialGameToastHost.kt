package com.example.funlife.ui.screens.socialgame

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SocialGameToastHost(
    toast: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2600)
            onDismiss()
        }
    }
    toast?.let { msg ->
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                msg,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = SocialGamePalette.accentPurple.copy(0.15f))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, SocialGamePalette.glassBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                color = SocialGamePalette.inkPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        }
    }
}
