// ═══════════════════════════════════════════════════════════════════════════
// MagicBook3DWidget · SceneView (Filament) · Stage 2
// ───────────────────────────────────────────────────────────────────────────
// 6 个 PlaneNode 组成立方体，每个面贴自己的 BookSkin 渲染位图。
// 父节点 cubeRoot 接收用户的拖拽旋转。
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.diarybook

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.sin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.funlife.domain.skin.BookSkin
import com.example.funlife.data.DiaryBookCustomizationStore
import com.example.funlife.ui.components.diarybook.skin.rememberBookCustomization
import com.example.funlife.R
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Texture
import com.google.android.filament.android.TextureHelper
import io.github.sceneview.Scene
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Size
import io.github.sceneview.node.Node
import io.github.sceneview.node.PlaneNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberNodes

/**
 * 真 PBR 3D 魔法书 · Stage 2
 *
 * 6 个 PlaneNode 各自贴当前 [BookSkin] 渲染的纹理：
 *   front  · 封面（标题 + 烫金 + 装饰）
 *   back   · 后封（朱砂 "啟" 印章）
 *   right  · 右页摞（高密度水平页线）
 *   left   · 书脊（烫金中线 + 顶/底装饰）
 *   top    · 顶切口（垂直页线）
 *   bottom · 底切口
 */
@Composable
fun MagicBook3DWidget(
    skin: BookSkin,
    pageCount: Int = 1000,
    widthDp: Dp = 200.dp,
    heightDp: Dp = 280.dp,
    modifier: Modifier = Modifier,
    openProgress: Float = 0f,           // 0=静态，1=完全翻开（书旋转+camera 推近+特效淡出）
    onClick: () -> Unit = {}
) {
    val engine: Engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // ── 立方体世界尺寸 ─────────────────────────────────
    val aspect = heightDp.value / widthDp.value
    val depthNorm = remember(pageCount) {
        when {
            pageCount <= 1000  -> 0.25f
            pageCount <= 5000  -> 0.35f
            pageCount <= 10000 -> 0.45f
            else               -> 0.60f
        }
    }

    val customization = rememberBookCustomization()
    val defaultTitle = stringResource(R.string.diary_book_default_title)
    val defaultSubtitle = stringResource(R.string.diary_book_default_subtitle)
    val coverTitle = DiaryBookCustomizationStore.resolveTitle(customization, defaultTitle)
    val coverOwnerLine = DiaryBookCustomizationStore.resolveOwnerLine(customization, defaultSubtitle)
    val ownerNameRaw = customization.ownerName

    // ── 6 张纹理位图（按 skin + 定制文案缓存；封面超采样 1.4×）─────
    val texScale = 1.4f
    val texWPx = with(density) { (widthDp * texScale).toPx().toInt() }
    val texHPx = with(density) { (heightDp * texScale).toPx().toInt() }
    val texTPx = with(density) { (widthDp * depthNorm * texScale).toPx().toInt() }
    val faceBitmaps = remember(skin, texWPx, texHPx, texTPx, coverTitle, coverOwnerLine, ownerNameRaw) {
        buildFaceBitmaps(skin, texWPx, texHPx, texTPx, coverTitle, coverOwnerLine, ownerNameRaw)
    }

    // ── 6 张 Texture（跟随 bitmap 重建，旧的需手动 destroy）─────
    val textures = remember(engine, faceBitmaps) {
        listOf(
            bitmapToTexture(engine, faceBitmaps.front),
            bitmapToTexture(engine, faceBitmaps.back),
            bitmapToTexture(engine, faceBitmaps.right),
            bitmapToTexture(engine, faceBitmaps.left),
            bitmapToTexture(engine, faceBitmaps.top),
            bitmapToTexture(engine, faceBitmaps.bottom),
        )
    }
    DisposableEffect(textures) {
        onDispose {
            textures.forEach { runCatching { engine.destroyTexture(it) } }
        }
    }

    // ── 6 个 MaterialInstance（依赖 textures，旧的需手动 destroy）
    val materials = remember(engine, textures) {
        FaceMaterials(
            front  = materialLoader.createImageInstance(textures[0]),
            back   = materialLoader.createImageInstance(textures[1]),
            right  = materialLoader.createImageInstance(textures[2]),
            left   = materialLoader.createImageInstance(textures[3]),
            top    = materialLoader.createImageInstance(textures[4]),
            bottom = materialLoader.createImageInstance(textures[5]),
        )
    }
    DisposableEffect(materials) {
        onDispose {
            listOf(materials.front, materials.back, materials.right,
                   materials.left, materials.top, materials.bottom).forEach {
                runCatching { engine.destroyMaterialInstance(it) }
            }
        }
    }

    // ── 父节点：所有面的旋转中心 ───────────────────────
    val cubeRoot = remember(engine) { Node(engine) }

    // ── 6 个 PlaneNode：组成立方体 ─────────────────────
    // 立方体半尺寸（世界单位）
    val halfW = 0.5f
    val halfH = aspect / 2f
    val halfD = depthNorm / 2f

    val planes = remember(engine, materials, depthNorm) {
        val list = mutableListOf<PlaneNode>()

        // 关键：所有 plane 几何都留在自身 local 原点（默认 center=0、朝向 +Z）。
        //       通过 Node.position 推到对应半立方体面，通过 Node.rotation 转朝向。
        //       这样 rotation 是"绕自身中心转 90° 朝外"，position 把它推到面位。

        // 前封 +Z
        list += PlaneNode(
            engine = engine,
            size = Size(1.0f, aspect, 0f),
            materialInstance = materials.front
        ).apply {
            position = Position(x = 0f, y = 0f, z = halfD)
        }

        // 后封 -Z（绕 Y 转 180°）
        list += PlaneNode(
            engine = engine,
            size = Size(1.0f, aspect, 0f),
            materialInstance = materials.back
        ).apply {
            position = Position(x = 0f, y = 0f, z = -halfD)
            rotation = Rotation(x = 0f, y = 180f, z = 0f)
        }

        // 右页摞 +X（绕 Y 转 +90°）
        list += PlaneNode(
            engine = engine,
            size = Size(depthNorm, aspect, 0f),
            materialInstance = materials.right
        ).apply {
            position = Position(x = halfW, y = 0f, z = 0f)
            rotation = Rotation(x = 0f, y = 90f, z = 0f)
        }

        // 左书脊 -X（绕 Y 转 -90°）
        list += PlaneNode(
            engine = engine,
            size = Size(depthNorm, aspect, 0f),
            materialInstance = materials.left
        ).apply {
            position = Position(x = -halfW, y = 0f, z = 0f)
            rotation = Rotation(x = 0f, y = -90f, z = 0f)
        }

        // 顶切口 +Y（绕 X 转 -90°）
        list += PlaneNode(
            engine = engine,
            size = Size(1.0f, depthNorm, 0f),
            materialInstance = materials.top
        ).apply {
            position = Position(x = 0f, y = halfH, z = 0f)
            rotation = Rotation(x = -90f, y = 0f, z = 0f)
        }

        // 底切口 -Y（绕 X 转 +90°）
        list += PlaneNode(
            engine = engine,
            size = Size(1.0f, depthNorm, 0f),
            materialInstance = materials.bottom
        ).apply {
            position = Position(x = 0f, y = -halfH, z = 0f)
            rotation = Rotation(x = 90f, y = 0f, z = 0f)
        }

        // 关键：先把上一轮的 planes 全摘掉，避免新旧叠加
        cubeRoot.childNodes.toList().forEach { cubeRoot.removeChildNode(it) }
        // 全部加到根节点
        list.forEach { cubeRoot.addChildNode(it) }
        list
    }
    // planes 被替换时，旧 PlaneNode 也要释放
    DisposableEffect(planes) {
        val current = planes
        onDispose {
            current.forEach { runCatching { it.destroy() } }
        }
    }

    val cameraNode = rememberCameraNode(engine) {
        position = Position(z = 3.0f)
    }

    // ── 拖拽旋转（用户输入）────────────────────────────
    // 默认视角：偏右 22° + 俯 15°，让正面占主视野（不再大量露出页摞）
    var dragY by remember { mutableStateOf(-22f) }
    var dragX by remember { mutableStateOf(15f) }
    var isDragging by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // ── 自动回正：松手 1.5s 后无操作 → 700ms spring 回默认 ──
    LaunchedEffect(isDragging) {
        if (!isDragging) {
            kotlinx.coroutines.delay(1500)
            // 期间若被再次触摸，isDragging 会重置 -> 这个协程被取消
            val startY = dragY
            val startX = dragX
            val targetY = -22f
            val targetX = 15f
            val durMs = 700
            val frameInterval = 16L
            val steps = (durMs / frameInterval).toInt()
            for (i in 1..steps) {
                val pp = (i.toFloat() / steps).let {
                    // ease-out cubic
                    1f - (1f - it) * (1f - it) * (1f - it)
                }
                dragY = startY + (targetY - startY) * pp
                dragX = startX + (targetX - startX) * pp
                kotlinx.coroutines.delay(frameInterval)
            }
        }
    }

    // ── 空闲 12s 彩蛋：长时间无交互 → 封面"打开 5°"再合上（暗示可点） ──
    val coverPeek = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            val idle = System.currentTimeMillis() - lastInteractionTime
            if (idle > 12_000 && !isDragging && openProgress < 0.01f) {
                // 1.2s 一个 peek 动作（开 → 合）
                coverPeek.animateTo(1f, tween(600, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                coverPeek.animateTo(0f, tween(600, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                lastInteractionTime = System.currentTimeMillis()        // 重置避免持续重复
                kotlinx.coroutines.delay(8000)                          // 至少间隔 8s 再次彩蛋
            }
        }
    }

    // ── 跟手高光：拖动时指针位置 + 0..1 强度（拖动开始 ramp up，结束 ramp down）──
    var pointerPos by remember { mutableStateOf<Offset?>(null) }
    val highlightStrength = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(isDragging) {
        if (isDragging) highlightStrength.animateTo(1f, tween(150))
        else highlightStrength.animateTo(0f, tween(450))
    }

    // ── 点击粒子爆发动画：每次点击 burstTrigger 自增，触发 0→1 过程 ──
    var burstTrigger by remember { mutableStateOf(0) }
    val burstAnim = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(burstTrigger) {
        if (burstTrigger > 0) {
            burstAnim.snapTo(0f)
            burstAnim.animateTo(1f, animationSpec = tween(700, easing = LinearEasing))
        }
    }

    // ── 切皮交叉过渡：skin.id 变化时 alpha 0.25→1 + 轻微缩放弹入 ──
    val skinFade = remember { androidx.compose.animation.core.Animatable(1f) }
    val skinScale = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(skin.id.raw) {
        skinFade.snapTo(0.22f)
        skinScale.snapTo(0.93f)
        kotlinx.coroutines.coroutineScope {
            launch {
                skinFade.animateTo(
                    1f,
                    animationSpec = tween(520, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                )
            }
            launch {
                skinScale.animateTo(
                    1f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = 0.72f,
                        stiffness = 380f,
                    ),
                )
            }
        }
    }

    // ── 呼吸 / 浮动 / 微旋转动画 ───────────────────────
    val infinite = rememberInfiniteTransition(label = "magic_book_3d")
    val breath by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    // 未拖拽时：在用户当前角度基础上叠加 ±3° 的轻摆，给"活着"的感觉
    val idleRotY = if (isDragging) 0f else (-3f + breath * 6f)
    val idleRotX = if (isDragging) 0f else (-1.5f + breath * 3f)
    val floatYpx = -6f + breath * 12f                              // 上下浮动 ±6px

    LaunchedEffect(dragY, dragX, idleRotY, idleRotX, openProgress, coverPeek.value) {
        // 翻开过程：在用户视角基础上额外增加 Y 旋转 -90°
        val openY = -90f * openProgress
        // 空闲彩蛋：封面微开 5°
        val peekY = -5f * coverPeek.value
        cubeRoot.rotation = Rotation(
            x = dragX + idleRotX,
            y = dragY + idleRotY + openY + peekY,
            z = 0f
        )
    }
    LaunchedEffect(openProgress) {
        // 翻开时摄像机由 z=3.0 推近到 z=1.4，营造放大冲入感
        cameraNode.position = Position(z = 3.0f - openProgress * 1.6f)
    }

    val nodes = rememberNodes { add(cubeRoot) }

    val stage = bookStageThemeFor(skin.id.raw)

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = widthDp + 200.dp, minHeight = heightDp + 240.dp)
            .graphicsLayer {
                alpha = skinFade.value * (1f - openProgress * 0.85f)
                scaleX = skinScale.value
                scaleY = skinScale.value
            },
        contentAlignment = Alignment.Center
    ) {
        // ── 书后不再画任何圆形气场/光晕（用户明确不要书背后的光盘）。
        //    氛围光交给背景渐变 + 顶部聚光 + 皮肤粒子特效(SkinFx)，不在书正后方堆叠径向光。──

        // ── 地面投影：用剧场气场色反照（火光下不该是黑阴影）────
        Canvas(
            modifier = Modifier
                .size(width = widthDp * 0.9f, height = 22.dp)
                .graphicsLayer {
                    translationY = (heightDp / 2 + 36.dp).toPx() + floatYpx * 0.3f
                    val t = (floatYpx / 12f).coerceIn(-1f, 1f)
                    scaleX = 1f - t * 0.12f
                    scaleY = (1f - t * 0.12f) * 0.7f
                    alpha = stage.groundAlpha - t * 0.18f
                }
        ) {
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        stage.ground.copy(alpha = stage.groundAlpha),
                        Color.Transparent
                    )
                ),
                topLeft = Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(size.width, size.height)
            )
        }

        // ── 皮肤专属魔法特效（必须在 Scene 之前 / SurfaceView 之下渲染，
        //    通过 Scene 的 isOpaque=false 让 3D 书外的透明像素透出特效）─
        SkinFx(
            skin = skin,
            widthDp = widthDp,
            heightDp = heightDp
        )

        // ── 3D Scene（给 3D 立方体旋转留余量；外圈仍给 SkinFx 显形）─
        Box(
            modifier = Modifier
                .size(widthDp + 80.dp, heightDp + 80.dp)
                .graphicsLayer { translationY = floatYpx }
        ) {
            Scene(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                cameraNode = cameraNode,
                cameraManipulator = null,
                childNodes = nodes,
                isOpaque = false
            )
        }

        // ── 角度敏感 sheen 已移除（用户不需要） ──

        // ── 书签丝带已移除（用户不需要） ──

        // ── 跟手高光：拖动时指针位置出现径向白光，松手柔和淡出 ──
        if (highlightStrength.value > 0.01f) {
            val pos = pointerPos
            if (pos != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strength = highlightStrength.value
                    val r = minOf(size.width, size.height) * 0.30f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.45f * strength),
                                Color.White.copy(alpha = 0.15f * strength),
                                Color.Transparent,
                            ),
                            center = pos,
                            radius = r,
                        ),
                        radius = r,
                        center = pos,
                    )
                }
            }
        }

        // ── 点击粒子爆发层（在 3D 书前面，30 颗 foil 色粒子向外飞）─
        if (burstAnim.value > 0f && burstAnim.value < 1f) {
            val p = burstAnim.value
            val particleColor = skin.palette.foil.base
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val maxR = minOf(size.width, size.height) * 0.45f
                for (i in 0 until 30) {
                    val ang = (i / 30f) * 2f * Math.PI.toFloat() + i * 0.13f
                    val r = maxR * p * (0.6f + (i % 5) * 0.1f)
                    val px = cx + kotlin.math.cos(ang) * r
                    val py = cy + kotlin.math.sin(ang) * r
                    val alpha = ((1f - p) * 0.95f).coerceIn(0f, 1f)
                    val radius = (4f + (i % 3) * 1.5f) * (1f - p * 0.5f)
                    drawCircle(particleColor.copy(alpha = alpha),
                        radius = radius, center = Offset(px, py))
                    drawCircle(particleColor.copy(alpha = (alpha * 0.35f).coerceIn(0f, 1f)),
                        radius = radius * 3f, center = Offset(px, py))
                }
                // 中心闪光环
                val ringAlpha = ((1f - p) * 0.6f).coerceIn(0f, 1f)
                drawCircle(particleColor.copy(alpha = ringAlpha),
                    radius = maxR * p, center = Offset(cx, cy),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f * (1f - p)))
            }
        }

        // ── 手势覆盖层（最上层）─────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { start ->
                            isDragging = true
                            pointerPos = start
                            lastInteractionTime = System.currentTimeMillis()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragEnd = {
                            isDragging = false
                            lastInteractionTime = System.currentTimeMillis()
                        },
                        onDragCancel = {
                            isDragging = false
                            lastInteractionTime = System.currentTimeMillis()
                        }
                    ) { change, drag ->
                        change.consume()
                        dragY += drag.x * 0.5f
                        dragX -= drag.y * 0.3f
                        pointerPos = change.position
                        lastInteractionTime = System.currentTimeMillis()
                    }
                }
                .pointerInput(onClick) {
                    detectTapGestures(onTap = {
                        lastInteractionTime = System.currentTimeMillis()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        burstTrigger++
                        onClick()
                    })
                }
        )
    }
}

private data class FaceMaterials(
    val front: MaterialInstance,
    val back: MaterialInstance,
    val right: MaterialInstance,
    val left: MaterialInstance,
    val top: MaterialInstance,
    val bottom: MaterialInstance,
)

/** 把 Android Bitmap 转成 Filament Texture（RGBA8，无 mipmap）。 */
private fun bitmapToTexture(engine: Engine, bitmap: Bitmap): Texture {
    val tex = Texture.Builder()
        .width(bitmap.width)
        .height(bitmap.height)
        .sampler(Texture.Sampler.SAMPLER_2D)
        .format(Texture.InternalFormat.RGBA8)
        .levels(1)
        .build(engine)
    TextureHelper.setBitmap(engine, tex, 0, bitmap)
    return tex
}
