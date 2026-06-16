package com.example.funlife.ui.screens.pacmaze.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import com.example.funlife.social.game.engine.pacmaze.PacMazeMovementMode
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.ui.screens.pacmaze.maptheme.CyberVisualEffects

/** 赛博左侧窄栏 HUD：不遮挡地图。 */
@Composable
fun PacMazeCyberPlayHudSidebar(
    levelId: Int,
    score: Int,
    lives: Int,
    elapsedSeconds: Int,
    attackCharges: Int,
    powerTicksLeft: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    playerDrawScale: Float = 1f,
    onPlayerDrawScaleChange: ((Float) -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .width(68.dp)
            .fillMaxHeight()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f)),
        ) {
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = "返回",
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }

        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(CyberVisualEffects.NeonRed)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(lives.coerceIn(0, 5)) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                    )
                }
            }
            SidebarStat("LVL", levelId.toString().padStart(3, '0'))
            SidebarStat("SCR", score.toString().takeLast(4).padStart(4, '0'))
            SidebarStat(
                "T",
                "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60),
            )
            if (attackCharges > 0) {
                Text(
                    "ATK$attackCharges",
                    color = CyberVisualEffects.NeonYellow,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
            }
            if (powerTicksLeft > 0) {
                Text(
                    "PWR",
                    color = CyberVisualEffects.NeonBlue,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        Canvas(modifier = Modifier.size(width = 12.dp, height = 28.dp)) {
            val stripeW = 4f
            var x = 0f
            var i = 0
            while (x < size.width + stripeW) {
                drawRect(
                    color = if (i % 2 == 0) CyberVisualEffects.NeonRed else Color.White,
                    topLeft = Offset(x, 0f),
                    size = Size(stripeW, size.height),
                )
                x += stripeW
                i++
            }
        }

        if (onPlayerDrawScaleChange != null) {
            Spacer(modifier = Modifier.weight(1f))
            PacMazePlayerScaleControl(
                scale = playerDrawScale,
                onScaleChange = onPlayerDrawScaleChange,
                accent = CyberVisualEffects.NeonYellow,
            )
        }
    }
}

@Composable
private fun SidebarStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            value,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

/** 横条版（暂停菜单等场景仍可用）。 */
@Composable
fun PacMazeCyberPlayHud(
    levelId: Int,
    score: Int,
    lives: Int,
    elapsedSeconds: Int,
    attackCharges: Int,
    powerTicksLeft: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f)),
        ) {
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = "返回",
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }

        Row(
            modifier = Modifier
                .height(26.dp)
                .background(CyberVisualEffects.NeonRed),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "L${levelId.toString().padStart(2, '0')}",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 6.dp, end = 4.dp),
            )
            Text(
                text = score.toString().padStart(5, '0'),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(end = 4.dp),
            )
            Text(
                text = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(end = 6.dp),
            )
            HazardStripeEnd(modifier = Modifier.width(14.dp).height(26.dp))
        }
    }
}

@Composable
private fun HazardStripeEnd(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stripeW = 5f
        var x = 0f
        var i = 0
        while (x < size.width + stripeW) {
            drawRect(
                color = if (i % 2 == 0) Color.Black else Color.White,
                topLeft = Offset(x, 0f),
                size = Size(stripeW, size.height),
            )
            x += stripeW
            i++
        }
    }
}
