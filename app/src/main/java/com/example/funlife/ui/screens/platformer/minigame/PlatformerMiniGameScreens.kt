package com.example.funlife.ui.screens.platformer.minigame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.game.platformer.PlatformerCharacterId
import com.example.funlife.game.platformer.catalog.PlatformerAnimClip
import com.example.funlife.game.platformer.catalog.PlatformerRemoteAnimCache

/** 天空射击：波次敌机 + 星野 parallax + 飞机序列帧。 */
@Composable
fun PlatformerPlaneShooterScreen(onBack: () -> Unit) {
    var state by remember { mutableStateOf(PlatformerPlaneShooterEngine.reset()) }
    val planeFrames = remember { PlatformerMiniGameAssets.loadPlaneFlyFrames() }
    val bulletFrames = remember { PlatformerMiniGameAssets.loadPlaneBulletFrames() }
    val enemyFrames = remember { PlatformerMiniGameAssets.loadPlaneEnemyFrames() }
    var animTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.gameOver) {
        if (state.gameOver) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(16)
            animTick++
            PlatformerPlaneShooterEngine.tick(state)
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF050818))) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        PlatformerPlaneShooterEngine.setPlaneX(state, change.position.x / size.width)
                    }
                },
        ) {
            drawPlaneBackground(state)
            val planeFrame = planeFrames.getOrNull((animTick / 4) % planeFrames.size.coerceAtLeast(1))
            val bulletFrame = bulletFrames.getOrNull((animTick / 3) % bulletFrames.size.coerceAtLeast(1))
            val enemyFrame = enemyFrames.getOrNull((animTick / 5) % enemyFrames.size.coerceAtLeast(1))

            state.bullets.forEach { b ->
                val s = size.width * 0.035f
                if (bulletFrame != null) {
                    drawImage(
                        bulletFrame,
                        dstOffset = IntOffset((b.x * size.width - s / 2).toInt(), (b.y * size.height).toInt()),
                        dstSize = IntSize(s.toInt(), s.toInt()),
                    )
                } else {
                    drawCircle(Color(0xFF80D8FF), radius = s * 0.4f, center = Offset(b.x * size.width, b.y * size.height))
                }
            }

            state.enemies.forEach { e ->
                val r = size.width * e.kind.radius
                if (enemyFrame != null) {
                    drawImage(
                        enemyFrame,
                        dstOffset = IntOffset((e.x * size.width - r).toInt(), (e.y * size.height - r).toInt()),
                        dstSize = IntSize((r * 2).toInt(), (r * 2).toInt()),
                    )
                } else {
                    drawCircle(
                        when (e.kind) {
                            PlatformerPlaneShooterEngine.EnemyKind.SCOUT -> Color(0xFFFF5252)
                            PlatformerPlaneShooterEngine.EnemyKind.HEAVY -> Color(0xFFE040FB)
                            PlatformerPlaneShooterEngine.EnemyKind.DART -> Color(0xFFFFAB40)
                        },
                        radius = r,
                        center = Offset(e.x * size.width, e.y * size.height),
                    )
                }
            }

            val px = state.planeX * size.width
            val py = PlatformerPlaneShooterEngine.playerY() * size.height
            if (state.invincibleTicks > 0 && animTick % 6 < 3) {
                // 无敌闪烁
            } else if (planeFrame != null) {
                val pw = size.width * 0.2f
                val ph = pw * planeFrame.height / planeFrame.width
                drawImage(
                    planeFrame,
                    dstOffset = IntOffset((px - pw / 2).toInt(), (py - ph / 2).toInt()),
                    dstSize = IntSize(pw.toInt(), ph.toInt()),
                )
            } else {
                drawCircle(Color(0xFF4FC3F7), radius = size.width * 0.06f, center = Offset(px, py))
            }
        }
        MiniGameTopBar("天空射击 · ${state.score}  ♥${state.lives}", onBack)
        if (state.gameOver) {
            MiniGameOverlay(
                "坠落!",
                "得分 ${state.score}",
                onBack,
                onRetry = { state = PlatformerPlaneShooterEngine.reset(); animTick = 0 },
            )
        }
    }
}

/** 登山挑战：程序化山地 + 分部件车辆。 */
@Composable
fun PlatformerHillClimbScreen(onBack: () -> Unit) {
    var state by remember { mutableStateOf(PlatformerHillClimbEngine.reset()) }
    var gasHeld by remember { mutableStateOf(false) }
    val parts = remember { PlatformerMiniGameAssets.loadHillClimbParts() }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.gameOver) {
        if (state.gameOver) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(16)
            tick++
            PlatformerHillClimbEngine.tick(state, gas = gasHeld, brake = false)
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF87CEEB))) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            gasHeld = true
                            tryAwaitRelease()
                            gasHeld = false
                        },
                    )
                },
        ) {
            @Suppress("UNUSED_EXPRESSION") tick
            val camX = PlatformerHillClimbEngine.cameraOffset(state)
            val viewLeft = camX
            val viewRight = camX + 0.55f

            drawRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF64B5F6), Color(0xFFBBDEFB))),
                size = size,
            )

            val terrainPath = Path().apply {
                var started = false
                state.terrain.forEach { pt ->
                    if (pt.x < viewLeft - 0.05f || pt.x > viewRight + 0.05f) return@forEach
                    val sx = (pt.x - viewLeft) / 0.55f * size.width
                    val sy = pt.y * size.height
                    if (!started) { moveTo(sx, sy); started = true } else lineTo(sx, sy)
                }
                if (started) {
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
            }
            drawPath(terrainPath, Color(0xFF6D4C41))
            drawPath(terrainPath, Color(0xFF8D6E63), alpha = 0.35f)

            state.terrain.forEach { pt ->
                if (pt.x < viewLeft || pt.x > viewRight) return@forEach
                if ((pt.x * 1000).toInt() % 17 == 0) {
                    val sx = (pt.x - viewLeft) / 0.55f * size.width
                    drawCircle(Color(0xFF388E3C), radius = 8f, center = Offset(sx, pt.y * size.height - 12f))
                }
            }

            val carScreenX = (state.carX - viewLeft) / 0.55f * size.width
            val carScreenY = state.carY * size.height
            val wheelR = size.width * 0.035f

            withTransform({
                rotate(state.angle * 57.2958f, pivot = Offset(carScreenX, carScreenY))
            }) {
                parts.body?.let { bmp ->
                    val bw = size.width * 0.22f
                    val bh = bw * bmp.height / bmp.width
                    drawImage(
                        bmp,
                        dstOffset = IntOffset((carScreenX - bw / 2).toInt(), (carScreenY - bh * 0.85f).toInt()),
                        dstSize = IntSize(bw.toInt(), bh.toInt()),
                    )
                } ?: drawRoundRect(
                    Color(0xFFE53935),
                    topLeft = Offset(carScreenX - size.width * 0.08f, carScreenY - size.width * 0.06f),
                    size = Size(size.width * 0.16f, size.width * 0.08f),
                )
                val wb = PlatformerHillClimbEngine.wheelBase() / 0.55f * size.width * 0.5f
                drawHillWheel(parts.backWheel, carScreenX - wb, carScreenY, wheelR, state.carX * 40f)
                drawHillWheel(parts.frontWheel, carScreenX + wb, carScreenY, wheelR, state.carX * 40f)
            }
        }
        MiniGameTopBar("登山挑战 · ${state.distance.toInt()}m  ⛽${state.fuel.toInt()}%", onBack)
        Text(
            "按住屏幕加油",
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
            color = Color.White,
            fontSize = 14.sp,
        )
        if (state.gameOver) {
            MiniGameOverlay(
                if (state.flipped) "翻车!" else "燃油耗尽",
                "距离 ${state.distance.toInt()}m",
                onBack,
                onRetry = { state = PlatformerHillClimbEngine.reset(); tick = 0 },
            )
        }
    }
}

/** 神庙跑酷：三车道透视赛道 + 神庙跑者动画。 */
@Composable
fun PlatformerTempleRunScreen(onBack: () -> Unit) {
    var state by remember { mutableStateOf(PlatformerTempleRunEngine.reset()) }
    var animTick by remember { mutableIntStateOf(0) }
    val runFrames = remember { PlatformerMiniGameAssets.templeRunnerFrames(PlatformerAnimClip.RUN) }
    val jumpFrames = remember { PlatformerMiniGameAssets.templeRunnerFrames(PlatformerAnimClip.JUMP) }
    val slideFrames = remember { PlatformerMiniGameAssets.templeRunnerFrames(PlatformerAnimClip.SLIDE) }

    LaunchedEffect(Unit) {
        PlatformerRemoteAnimCache.requestWarmup(PlatformerCharacterId.TEMPLE_RUNNER)
    }

    LaunchedEffect(state.gameOver) {
        if (state.gameOver) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(16)
            animTick++
            PlatformerTempleRunEngine.tick(state)
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF1B5E20))) {
        Canvas(Modifier.fillMaxSize()) {
            drawTempleTrack(state)
            drawTempleObstacles(state)

            val playerDist = 0.82f
            val px = PlatformerTempleRunEngine.laneCenterX(state.lane, playerDist) * size.width
            val py = size.height * (playerDist - state.jumpOffset)
            val frameList = when {
                state.sliding && slideFrames.isNotEmpty() -> slideFrames
                state.jumpOffset > 0.02f && jumpFrames.isNotEmpty() -> jumpFrames
                runFrames.isNotEmpty() -> runFrames
                else -> emptyList()
            }
            val frame = frameList.getOrNull((animTick / 3) % frameList.size.coerceAtLeast(1))
            val pw = size.width * 0.14f
            if (frame != null) {
                val ph = pw * frame.height / frame.width
                drawImage(
                    frame,
                    dstOffset = IntOffset((px - pw / 2).toInt(), (py - ph * 0.9f).toInt()),
                    dstSize = IntSize(pw.toInt(), ph.toInt()),
                )
            } else {
                drawCircle(Color(0xFFFFB300), radius = pw * 0.35f, center = Offset(px, py))
            }
        }
        Row(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(onClick = { PlatformerTempleRunEngine.moveLeft(state) }) { Text("左") }
            Button(onClick = { PlatformerTempleRunEngine.jump(state) }) { Text("跳") }
            Button(onClick = { PlatformerTempleRunEngine.slide(state) }) { Text("铲") }
            Button(onClick = { PlatformerTempleRunEngine.moveRight(state) }) { Text("右") }
        }
        MiniGameTopBar("神庙跑酷 · ${state.score}  🪙${state.coins}", onBack)
        if (state.gameOver) {
            MiniGameOverlay(
                "撞到了!",
                "距离 ${state.score}  金币 ${state.coins}",
                onBack,
                onRetry = { state = PlatformerTempleRunEngine.reset(); animTick = 0 },
            )
        }
    }
}

private fun DrawScope.drawPlaneBackground(state: PlatformerPlaneShooterEngine.State) {
    state.starLayers.forEach { layer ->
        layer.stars.forEach { (x, y) ->
            val sy = PlatformerPlaneShooterEngine.starY(layer, y) * size.height
            drawCircle(
                Color.White.copy(alpha = 0.35f + layer.speed * 80f),
                radius = 1.5f + layer.speed * 200f,
                center = Offset(x * size.width, sy),
            )
        }
    }
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color(0x00050818),
            0.85f to Color(0x331A237E),
            1f to Color(0xFF263238),
        ),
        size = size,
    )
}

private fun DrawScope.drawTempleTrack(state: PlatformerTempleRunEngine.State) {
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFF004D40), Color(0xFF1B5E20), Color(0xFF33691E)),
        ),
        size = size,
    )
    for (lane in 0 until PlatformerTempleRunEngine.LANE_COUNT) {
        val left = PlatformerTempleRunEngine.laneCenterX(lane, 0.72f) - PlatformerTempleRunEngine.laneWidth(0.72f) / 2
        val right = PlatformerTempleRunEngine.laneCenterX(lane, 1.35f) + PlatformerTempleRunEngine.laneWidth(1.35f) / 2
        val path = Path().apply {
            moveTo(left * size.width, size.height * 0.72f)
            lineTo((PlatformerTempleRunEngine.laneCenterX(lane, 1.35f) -
                PlatformerTempleRunEngine.laneWidth(1.35f) / 2) * size.width, size.height * 0.05f)
            lineTo((PlatformerTempleRunEngine.laneCenterX(lane, 1.35f) +
                PlatformerTempleRunEngine.laneWidth(1.35f) / 2) * size.width, size.height * 0.05f)
            lineTo(right * size.width, size.height * 0.72f)
            close()
        }
        drawPath(path, Color(0xFF558B2F).copy(alpha = 0.35f + lane * 0.08f))
    }
    state.decors.forEach { d ->
        val x = if (d.side == 0) 0.06f else 0.94f
        val y = d.dist * size.height
        drawCircle(Color(0xFF2E7D32), radius = size.width * 0.04f, center = Offset(x * size.width, y))
    }
}

private fun DrawScope.drawTempleObstacles(state: PlatformerTempleRunEngine.State) {
    state.obstacles.forEach { obs ->
        val cx = PlatformerTempleRunEngine.laneCenterX(obs.lane, obs.dist) * size.width
        val cy = obs.dist * size.height
        val lw = PlatformerTempleRunEngine.laneWidth(obs.dist) * size.width
        when (obs.kind) {
            PlatformerTempleRunEngine.ObstacleKind.BLOCK -> drawRect(
                Color(0xFF5D4037),
                topLeft = Offset(cx - lw * 0.35f, cy - size.height * 0.06f),
                size = Size(lw * 0.7f, size.height * 0.06f),
            )
            PlatformerTempleRunEngine.ObstacleKind.LOW_BAR -> drawRect(
                Color(0xFF795548),
                topLeft = Offset(cx - lw * 0.4f, cy - size.height * 0.14f),
                size = Size(lw * 0.8f, size.height * 0.025f),
            )
            PlatformerTempleRunEngine.ObstacleKind.PIT -> drawRect(
                Color(0xFF212121),
                topLeft = Offset(cx - lw * 0.4f, cy - size.height * 0.01f),
                size = Size(lw * 0.8f, size.height * 0.02f),
            )
            PlatformerTempleRunEngine.ObstacleKind.COIN -> drawCircle(
                Color(0xFFFFD54F),
                radius = lw * 0.12f,
                center = Offset(cx, cy - size.height * 0.03f),
            )
            PlatformerTempleRunEngine.ObstacleKind.GEM -> drawCircle(
                Color(0xFF69F0AE),
                radius = lw * 0.14f,
                center = Offset(cx, cy - size.height * 0.04f),
            )
        }
    }
}

private fun DrawScope.drawHillWheel(
    bitmap: ImageBitmap?,
    cx: Float,
    cy: Float,
    radius: Float,
    rotation: Float,
) {
    if (bitmap != null) {
        withTransform({
            rotate(rotation, pivot = Offset(cx, cy))
        }) {
            drawImage(
                bitmap,
                dstOffset = IntOffset((cx - radius).toInt(), (cy - radius).toInt()),
                dstSize = IntSize((radius * 2).toInt(), (radius * 2).toInt()),
            )
        }
    } else {
        drawCircle(Color(0xFF212121), radius = radius, center = Offset(cx, cy))
    }
}

@Composable
private fun BoxScope.MiniGameTopBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(8.dp).align(Alignment.TopStart),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
        }
        Text(title, color = Color.White, fontSize = 16.sp)
    }
}

@Composable
private fun MiniGameOverlay(title: String, subtitle: String, onBack: () -> Unit, onRetry: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, color = Color.White, fontSize = 24.sp)
            Text(subtitle, color = Color.White.copy(alpha = 0.85f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack) { Text("返回") }
                Button(onClick = onRetry) { Text("再来") }
            }
        }
    }
}
