package com.example.funlife.ui.screens.pacmaze.character

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.social.game.engine.pacmaze.PacMazeEntity
import com.example.funlife.social.game.engine.pacmaze.PacMazeEntityVisuals
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId
import kotlin.math.abs
import kotlin.math.sin

data class PacMazeCharacterPose(
    val facing: Direction,
    val animPhase: Float,
    val isMoving: Boolean,
    val powerActive: Boolean,
    /** 选角/工坊预览为 true，行走序列帧会使用更慢步态 */
    val walkPreview: Boolean = false,
    /** 序列帧皮肤：外部帧计时器强制指定当前帧（0..n-1），优先于 animPhase 推算 */
    val spriteFrameOverride: Int? = null,
    val attackCooldownTicksLeft: Int = 0,
    val attackCooldownTotal: Int = 0,
    val speedBoostActive: Boolean = false,
    val isDead: Boolean = false,
    /** 云端皮肤：仅绘制 preview 封面（网格未选中时） */
    val preferCoverOnly: Boolean = false,
    /** 局内速度 X（格/秒），供 ikun 横版位图在上下移动时保持水平朝向 */
    val velX: Float = 0f,
)

internal object PacMazeCharacterDraw {

    fun draw(
        scope: DrawScope,
        characterId: PacMazeCharacterId,
        center: Offset,
        radius: Float,
        pose: PacMazeCharacterPose,
        themeId: PacMazeMapThemeId = PacMazeMapThemeId.CLASSIC,
    ) {
        when (characterId) {
            PacMazeCharacterId.CLASSIC_PAC -> drawClassicPac(scope, center, radius, pose, themeId)
            PacMazeCharacterId.SCHOLAR -> drawScholar(scope, center, radius, pose)
            PacMazeCharacterId.LANTERN_FOX -> drawLanternFox(scope, center, radius, pose)
            PacMazeCharacterId.CANDY_SPIRIT -> drawCandySpirit(scope, center, radius, pose)
            PacMazeCharacterId.DATA_CORE -> drawDataCore(scope, center, radius, pose)
            PacMazeCharacterId.BUBBLE_SLIME -> drawBubbleSlime(scope, center, radius, pose)
            PacMazeCharacterId.NOODLE_PHANTOM -> drawNoodlePhantom(scope, center, radius, pose)
            PacMazeCharacterId.GEAR_MOLE -> drawGearMole(scope, center, radius, pose)
        }
    }

    fun poseFrom(
        entity: PacMazeEntity,
        animPhase: Float,
        powerTicksLeft: Int,
        attackCooldownTicksLeft: Int = 0,
        attackCooldownTotal: Int = 0,
        speedBoostTicksLeft: Int = 0,
    ): PacMazeCharacterPose {
        val visualFacing = PacMazeEntityVisuals.spriteFacing(entity)
        val moving = PacMazeEntityVisuals.isLocomoting(entity)
        return PacMazeCharacterPose(
            facing = visualFacing,
            animPhase = animPhase,
            isMoving = moving,
            powerActive = powerTicksLeft > 0,
            attackCooldownTicksLeft = attackCooldownTicksLeft,
            attackCooldownTotal = attackCooldownTotal,
            speedBoostActive = speedBoostTicksLeft > 0,
            velX = entity.velX,
        )
    }

    private fun walkCycle(pose: PacMazeCharacterPose): Float {
        if (!pose.isMoving) return 0f
        return sin(pose.animPhase * 3.2f)
    }

    /**
     * 角色默认朝右绘制。向左用水平镜像（避免 180° 旋转把眼睛翻到脚下），上下用旋转。
     */
    private inline fun DrawScope.withFacing(center: Offset, facing: Direction, block: DrawScope.() -> Unit) {
        when (facing) {
            Direction.RIGHT -> block()
            Direction.LEFT -> scale(scaleX = -1f, scaleY = 1f, pivot = center, block = block)
            Direction.UP -> rotate(degrees = -90f, pivot = center, block = block)
            Direction.DOWN -> rotate(degrees = 90f, pivot = center, block = block)
        }
    }

    private fun drawShadow(scope: DrawScope, center: Offset, radius: Float) {
        scope.drawOval(
            color = Color.Black.copy(alpha = 0.22f),
            topLeft = Offset(center.x - radius * 0.85f, center.y + radius * 0.52f),
            size = Size(radius * 1.7f, radius * 0.38f),
        )
    }

    private fun drawPowerAura(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, color: Color) {
        if (!pose.powerActive) return
        val pulse = 0.85f + 0.15f * sin(pose.animPhase * 3f)
        scope.drawCircle(
            color = color.copy(alpha = 0.28f * pulse),
            radius = radius * 1.55f * pulse,
            center = center,
        )
        scope.drawCircle(
            color = color.copy(alpha = 0.5f),
            radius = radius * 1.12f,
            center = center,
            style = Stroke(radius * 0.08f),
        )
    }

    private fun drawClassicPac(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        pose: PacMazeCharacterPose,
        themeId: PacMazeMapThemeId,
    ) {
        drawShadow(scope, center, radius)
        drawPowerAura(scope, center, radius, pose, Color(0xFFFFCA28))
        val bodyColors = when (themeId) {
            PacMazeMapThemeId.CHINESE -> listOf(Color(0xFFFFF8E1), Color(0xFFFFD54F), Color(0xFFFF8F00))
            PacMazeMapThemeId.CYBERPUNK -> listOf(Color(0xFFE0F7FA), Color(0xFF00E5FF), Color(0xFF00B0FF))
            else -> listOf(Color(0xFFFFFDE7), Color(0xFFFFEB3B), Color(0xFFFFB300), Color(0xFFFF8F00))
        }
        val mouthOpen = if (pose.isMoving) {
            28f + 18f * ((sin(pose.animPhase * 2.4f) + 1f) * 0.5f)
        } else {
            14f
        }
        scope.withFacing(center, pose.facing) {
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = bodyColors,
                    center = center - Offset(radius * 0.28f, radius * 0.32f),
                    radius = radius * 1.35f,
                ),
                radius = radius,
                center = center,
            )
            val mouthPath = Path().apply {
                moveTo(center.x, center.y)
                arcTo(
                    rect = Rect(
                        center.x - radius * 0.98f,
                        center.y - radius * 0.98f,
                        center.x + radius * 0.98f,
                        center.y + radius * 0.98f,
                    ),
                    startAngleDegrees = mouthOpen / 2f,
                    sweepAngleDegrees = 360f - mouthOpen,
                    forceMoveTo = false,
                )
                close()
            }
            scope.drawPath(mouthPath, color = Color(0xFF060B18), style = Fill)
            val eyeCenter = Offset(center.x + radius * 0.12f, center.y - radius * 0.42f)
            scope.drawCircle(color = Color.Black, radius = radius * 0.16f, center = eyeCenter)
            scope.drawCircle(
                color = Color.White,
                radius = radius * 0.05f,
                center = eyeCenter - Offset(radius * 0.05f, radius * 0.05f),
            )
        }
    }

    /** 书童：束发 + 青衫 + 摆臂走路（无骨骼，相位摆动）。 */
    private fun drawScholar(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose) {
        drawShadow(scope, center, radius)
        drawPowerAura(scope, center, radius, pose, Color(0xFFFFD54F))
        val step = walkCycle(pose)
        val bob = if (pose.isMoving) abs(step) * radius * 0.04f else 0f
        val c = center + Offset(0f, -bob)

        scope.withFacing(c, pose.facing) {
            val robe = Color(0xFF4A7C59)
            val robeDark = Color(0xFF2E5A3A)
            val skin = Color(0xFFFFE0B2)
            val hair = Color(0xFF1A1A1A)

            // 书箱（背后）
            scope.drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF8D6E63), Color(0xFF5D4037))),
                topLeft = Offset(c.x - radius * 0.42f, c.y - radius * 0.05f),
                size = Size(radius * 0.34f, radius * 0.55f),
                cornerRadius = CornerRadius(radius * 0.06f),
            )

            // 腿
            val legSwing = step * radius * 0.14f
            listOf(-radius * 0.14f to legSwing, radius * 0.14f to -legSwing).forEach { (ox, swing) ->
                scope.drawRoundRect(
                    color = robeDark,
                    topLeft = Offset(c.x + ox - radius * 0.07f, c.y + radius * 0.22f + swing),
                    size = Size(radius * 0.14f, radius * 0.28f),
                    cornerRadius = CornerRadius(radius * 0.04f),
                )
            }

            // 身体
            scope.drawRoundRect(
                brush = Brush.verticalGradient(listOf(robe.copy(alpha = 0.95f), robeDark)),
                topLeft = Offset(c.x - radius * 0.38f, c.y - radius * 0.12f),
                size = Size(radius * 0.76f, radius * 0.52f),
                cornerRadius = CornerRadius(radius * 0.12f),
            )
            scope.drawLine(
                color = Color(0xFFD4AF37).copy(alpha = 0.7f),
                start = Offset(c.x, c.y - radius * 0.1f),
                end = Offset(c.x, c.y + radius * 0.32f),
                strokeWidth = radius * 0.04f,
            )

            // 手臂
            val armSwing = step * radius * 0.18f
            listOf(-radius * 0.42f to armSwing, radius * 0.28f to -armSwing).forEach { (ox, swing) ->
                scope.drawRoundRect(
                    color = robe,
                    topLeft = Offset(c.x + ox, c.y - radius * 0.02f + swing),
                    size = Size(radius * 0.14f, radius * 0.32f),
                    cornerRadius = CornerRadius(radius * 0.05f),
                )
                scope.drawCircle(color = skin, radius = radius * 0.07f, center = Offset(c.x + ox + radius * 0.07f, c.y + radius * 0.28f + swing))
            }

            // 头
            scope.drawCircle(color = skin, radius = radius * 0.28f, center = Offset(c.x, c.y - radius * 0.32f))
            scope.drawCircle(color = hair, radius = radius * 0.3f, center = Offset(c.x, c.y - radius * 0.38f))
            scope.drawCircle(color = skin, radius = radius * 0.22f, center = Offset(c.x, c.y - radius * 0.3f))
            // 发髻
            scope.drawCircle(color = hair, radius = radius * 0.1f, center = Offset(c.x, c.y - radius * 0.52f))

            // 眼
            scope.drawCircle(color = Color(0xFF3E2723), radius = radius * 0.04f, center = Offset(c.x + radius * 0.08f, c.y - radius * 0.3f))
        }
    }

    /** 提灯小狐：三角耳 + 蓬松尾 + 摆动宫灯。 */
    private fun drawLanternFox(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose) {
        drawShadow(scope, center, radius)
        drawPowerAura(scope, center, radius, pose, Color(0xFF81C784))
        val step = walkCycle(pose)
        val bob = if (pose.isMoving) abs(step) * radius * 0.05f else sin(pose.animPhase) * radius * 0.015f
        val c = center + Offset(0f, -bob)

        scope.withFacing(c, pose.facing) {
            val fur = Color(0xFFFF8A65)
            val furLight = Color(0xFFFFCCBC)
            val furDark = Color(0xFFE64A19)

            // 尾巴
            val tailWag = sin(pose.animPhase * 4f) * radius * 0.12f
            val tail = Path().apply {
                moveTo(c.x - radius * 0.35f, c.y + radius * 0.15f)
                quadraticBezierTo(c.x - radius * 0.75f + tailWag, c.y - radius * 0.1f, c.x - radius * 0.55f, c.y - radius * 0.35f)
            }
            scope.drawPath(tail, color = fur, style = Stroke(radius * 0.16f))

            // 腿
            val legSwing = step * radius * 0.12f
            repeat(2) { i ->
                val ox = if (i == 0) -radius * 0.18f else radius * 0.08f
                val swing = if (i == 0) legSwing else -legSwing
                scope.drawRoundRect(
                    color = furDark,
                    topLeft = Offset(c.x + ox, c.y + radius * 0.18f + swing),
                    size = Size(radius * 0.14f, radius * 0.22f),
                    cornerRadius = CornerRadius(radius * 0.05f),
                )
            }

            // 身体
            scope.drawOval(
                brush = Brush.radialGradient(listOf(furLight, fur, furDark), center = c, radius = radius * 0.55f),
                topLeft = Offset(c.x - radius * 0.42f, c.y - radius * 0.18f),
                size = Size(radius * 0.84f, radius * 0.62f),
            )

            // 头
            scope.drawCircle(color = fur, radius = radius * 0.3f, center = Offset(c.x + radius * 0.12f, c.y - radius * 0.22f))
            scope.drawCircle(color = furLight, radius = radius * 0.18f, center = Offset(c.x + radius * 0.18f, c.y - radius * 0.12f))
            // 耳
            val earPath = Path().apply {
                moveTo(c.x + radius * 0.02f, c.y - radius * 0.42f)
                lineTo(c.x - radius * 0.08f, c.y - radius * 0.72f)
                lineTo(c.x + radius * 0.18f, c.y - radius * 0.38f)
                close()
            }
            scope.drawPath(earPath, color = furDark)
            val earPath2 = Path().apply {
                moveTo(c.x + radius * 0.28f, c.y - radius * 0.42f)
                lineTo(c.x + radius * 0.42f, c.y - radius * 0.72f)
                lineTo(c.x + radius * 0.38f, c.y - radius * 0.38f)
                close()
            }
            scope.drawPath(earPath2, color = furDark)
            scope.drawCircle(color = Color(0xFF3E2723), radius = radius * 0.045f, center = Offset(c.x + radius * 0.22f, c.y - radius * 0.24f))
            scope.drawCircle(color = Color(0xFF3E2723), radius = radius * 0.03f, center = Offset(c.x + radius * 0.34f, c.y - radius * 0.2f))

            // 宫灯（左右摆）
            val lanternSwing = sin(pose.animPhase * 3.5f) * radius * 0.08f
            val lantern = Rect(
                c.x + radius * 0.38f,
                c.y - radius * 0.05f + lanternSwing,
                c.x + radius * 0.58f,
                c.y + radius * 0.22f + lanternSwing,
            )
            scope.drawLine(
                color = Color(0xFF8D6E63),
                start = Offset(lantern.center.x, lantern.top - radius * 0.08f),
                end = Offset(lantern.center.x, lantern.top),
                strokeWidth = radius * 0.03f,
            )
            scope.drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFFD32F2F), Color(0xFF8B0000))),
                topLeft = lantern.topLeft,
                size = lantern.size,
                cornerRadius = CornerRadius(radius * 0.05f),
            )
            val glowPulse = 0.7f + 0.3f * sin(pose.animPhase * 2.5f)
            scope.drawCircle(
                color = Color(0xFFFFD54F).copy(alpha = 0.35f * glowPulse),
                radius = radius * 0.14f,
                center = lantern.center,
            )
        }
    }

    /** 糖纸精灵：弹跳 squash + 彩虹糖纸身体。 */
    private fun drawCandySpirit(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose) {
        drawShadow(scope, center, radius)
        drawPowerAura(scope, center, radius, pose, Color(0xFFFF80AB))
        val bounce = if (pose.isMoving) {
            val t = (sin(pose.animPhase * 6f) + 1f) * 0.5f
            -radius * 0.08f * t
        } else {
            sin(pose.animPhase * 1.5f) * radius * 0.02f
        }
        val squash = if (pose.isMoving && bounce < -radius * 0.03f) 1.08f else 1f
        val stretch = if (pose.isMoving && bounce > -radius * 0.02f) 0.92f else 1f
        val c = center + Offset(0f, bounce)

        scope.withFacing(c, pose.facing) {
            val bodyW = radius * 1.1f * squash
            val bodyH = radius * 0.95f * stretch
            scope.drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFFFF80AB),
                        Color(0xFFFFD54F),
                        Color(0xFF81D4FA),
                        Color(0xFFB39DDB),
                    ),
                ),
                topLeft = Offset(c.x - bodyW / 2f, c.y - bodyH / 2f),
                size = Size(bodyW, bodyH),
                cornerRadius = CornerRadius(radius * 0.35f),
            )
            scope.drawRoundRect(
                color = Color.White.copy(alpha = 0.35f),
                topLeft = Offset(c.x - bodyW * 0.35f, c.y - bodyH * 0.35f),
                size = Size(bodyW * 0.35f, bodyH * 0.25f),
                cornerRadius = CornerRadius(radius * 0.1f),
            )
            // 糖棍指向
            val stickLen = radius * 0.55f
            scope.drawRoundRect(
                color = Color(0xFFFFF8E1),
                topLeft = Offset(c.x + radius * 0.15f, c.y - radius * 0.04f),
                size = Size(stickLen, radius * 0.08f),
                cornerRadius = CornerRadius(radius * 0.04f),
            )
            scope.drawCircle(color = Color(0xFFFF4081), radius = radius * 0.14f, center = Offset(c.x, c.y - radius * 0.28f))
            scope.drawCircle(color = Color.White, radius = radius * 0.05f, center = Offset(c.x + radius * 0.05f, c.y - radius * 0.3f))
            scope.drawCircle(color = Color(0xFF3E2723), radius = radius * 0.035f, center = Offset(c.x + radius * 0.08f, c.y - radius * 0.27f))
        }
    }

    /** 数据核心：六边形芯片 + 旋转护盾。 */
    private fun drawDataCore(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose) {
        drawShadow(scope, center, radius)
        val pulse = 0.9f + 0.1f * sin(pose.animPhase * 3f)
        val r = radius * pulse
        val spin = if (pose.isMoving) pose.animPhase * 28f else pose.animPhase * 8f

        if (pose.powerActive) {
            scope.rotate(spin * 2f, center) {
                drawHex(scope, center, r * 1.35f, Color(0xFF00E5FF).copy(alpha = 0.35f), stroke = r * 0.06f)
            }
        }

        scope.drawContext.canvas.nativeCanvas.apply {
            val glow = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.FILL
                color = Color(0xFFFF1744).copy(alpha = 0.45f).toArgb()
                maskFilter = android.graphics.BlurMaskFilter(14f, android.graphics.BlurMaskFilter.Blur.NORMAL)
            }
            this.drawCircle(center.x, center.y, r * 1.1f, glow)
        }

        scope.rotate(spin, center) {
            drawHex(
                scope,
                center,
                r,
                Color(0xFFFF1744),
                fill = true,
            )
            drawHex(
                scope,
                center,
                r * 0.72f,
                Color(0xFF00E5FF).copy(alpha = 0.85f),
                fill = true,
            )
        }

        val eyeOffset = when (pose.facing) {
            Direction.LEFT -> Offset(-r * 0.12f, 0f)
            Direction.RIGHT -> Offset(r * 0.12f, 0f)
            Direction.UP -> Offset(0f, -r * 0.12f)
            Direction.DOWN -> Offset(0f, r * 0.12f)
        }
        scope.drawRect(
            color = Color.White.copy(alpha = 0.9f),
            topLeft = Offset(center.x + eyeOffset.x - r * 0.08f, center.y + eyeOffset.y - r * 0.08f),
            size = Size(r * 0.16f, r * 0.16f),
        )
    }

    private fun drawHex(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        color: Color,
        fill: Boolean = false,
        stroke: Float = 2f,
    ) {
        val path = Path()
        for (i in 0..5) {
            val angle = Math.toRadians((60.0 * i - 90.0))
            val x = center.x + radius * kotlin.math.cos(angle).toFloat()
            val y = center.y + radius * kotlin.math.sin(angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        if (fill) {
            scope.drawPath(path, color = color, style = Fill)
        } else {
            scope.drawPath(path, color = color, style = Stroke(stroke))
        }
    }

    /** 气泡史莱姆：半透明果冻体 + 头顶冒泡 + 大眼 */
    private fun drawBubbleSlime(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose) {
        drawShadow(scope, center, radius)
        drawPowerAura(scope, center, radius, pose, Color(0xFF4ADE80))
        val wobble = sin(pose.animPhase * 4f) * radius * 0.06f
        val squash = 1f + if (pose.isMoving) sin(pose.animPhase * 6f) * 0.06f else sin(pose.animPhase) * 0.03f
        val c = center + Offset(wobble, -abs(sin(pose.animPhase * 3f)) * radius * 0.04f)

        scope.withFacing(c, pose.facing) {
            val bodyW = radius * 1.05f * squash
            val bodyH = radius * 0.88f / squash
            scope.drawOval(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF86EFAC), Color(0xFF22C55E), Color(0xFF15803D)),
                    center = c - Offset(radius * 0.1f, radius * 0.15f),
                    radius = radius * 1.1f,
                ),
                topLeft = Offset(c.x - bodyW / 2f, c.y - bodyH / 2f),
                size = Size(bodyW, bodyH),
            )
            scope.drawOval(
                color = Color.White.copy(alpha = 0.28f),
                topLeft = Offset(c.x - bodyW * 0.28f, c.y - bodyH * 0.38f),
                size = Size(bodyW * 0.32f, bodyH * 0.22f),
            )
            // 气泡
            val bubbleY = c.y - bodyH * 0.55f - sin(pose.animPhase * 5f) * radius * 0.06f
            scope.drawCircle(
                color = Color(0xFFBBF7D0).copy(alpha = 0.55f),
                radius = radius * 0.12f,
                center = Offset(c.x + radius * 0.08f, bubbleY),
                style = Stroke(radius * 0.04f),
            )
            scope.drawCircle(
                color = Color(0xFFBBF7D0).copy(alpha = 0.35f),
                radius = radius * 0.07f,
                center = Offset(c.x - radius * 0.12f, bubbleY - radius * 0.14f),
            )
            // 大眼
            scope.drawCircle(color = Color.White, radius = radius * 0.16f, center = Offset(c.x + radius * 0.12f, c.y - radius * 0.08f))
            scope.drawCircle(color = Color(0xFF14532D), radius = radius * 0.09f, center = Offset(c.x + radius * 0.15f, c.y - radius * 0.06f))
            scope.drawCircle(color = Color.White, radius = radius * 0.03f, center = Offset(c.x + radius * 0.18f, c.y - radius * 0.09f))
            scope.drawCircle(color = Color.White.copy(alpha = 0.7f), radius = radius * 0.08f, center = Offset(c.x - radius * 0.14f, c.y - radius * 0.04f))
        }
    }

    /** 拉面精：幽灵面巾 + 面条四肢 + 筷子天线 */
    private fun drawNoodlePhantom(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose) {
        drawShadow(scope, center, radius)
        drawPowerAura(scope, center, radius, pose, Color(0xFFFBBF24))
        val floatY = sin(pose.animPhase * 2.8f) * radius * 0.07f
        val c = center + Offset(0f, floatY)
        val noodleSwing = if (pose.isMoving) walkCycle(pose) else sin(pose.animPhase * 3f) * 0.4f

        scope.withFacing(c, pose.facing) {
            val sheet = Color(0xFFF8FAFC)
            val noodle = Color(0xFFFDE68A)

            // 面条腿
            repeat(2) { i ->
                val ox = if (i == 0) -radius * 0.12f else radius * 0.08f
                val swing = if (i == 0) noodleSwing * radius * 0.2f else -noodleSwing * radius * 0.2f
                val noodlePath = Path().apply {
                    moveTo(c.x + ox, c.y + radius * 0.08f)
                    quadraticBezierTo(
                        c.x + ox + swing,
                        c.y + radius * 0.28f,
                        c.x + ox + swing * 0.5f,
                        c.y + radius * 0.42f,
                    )
                }
                scope.drawPath(noodlePath, color = noodle, style = Stroke(radius * 0.07f))
            }

            // 幽灵身体
            val bodyPath = Path().apply {
                moveTo(c.x - radius * 0.42f, c.y - radius * 0.18f)
                lineTo(c.x + radius * 0.42f, c.y - radius * 0.18f)
                lineTo(c.x + radius * 0.38f, c.y + radius * 0.22f)
                lineTo(c.x + radius * 0.18f, c.y + radius * 0.12f)
                lineTo(c.x, c.y + radius * 0.28f)
                lineTo(c.x - radius * 0.18f, c.y + radius * 0.12f)
                lineTo(c.x - radius * 0.38f, c.y + radius * 0.22f)
                close()
            }
            scope.drawPath(bodyPath, color = sheet.copy(alpha = 0.92f), style = Fill)
            scope.drawPath(bodyPath, color = Color(0xFFCBD5E1).copy(alpha = 0.5f), style = Stroke(radius * 0.03f))

            // 面条手臂
            val armSwing = noodleSwing * radius * 0.22f
            listOf(-radius * 0.38f to armSwing, radius * 0.32f to -armSwing).forEach { (ox, swing) ->
                val arm = Path().apply {
                    moveTo(c.x + ox, c.y - radius * 0.02f)
                    quadraticBezierTo(c.x + ox + swing, c.y + radius * 0.08f, c.x + ox + swing * 1.2f, c.y + radius * 0.22f)
                }
                scope.drawPath(arm, color = noodle, style = Stroke(radius * 0.06f))
            }

            // 筷子天线
            scope.drawLine(
                color = Color(0xFF92400E),
                start = Offset(c.x - radius * 0.06f, c.y - radius * 0.22f),
                end = Offset(c.x - radius * 0.22f, c.y - radius * 0.52f),
                strokeWidth = radius * 0.035f,
            )
            scope.drawLine(
                color = Color(0xFF92400E),
                start = Offset(c.x + radius * 0.06f, c.y - radius * 0.22f),
                end = Offset(c.x + radius * 0.18f, c.y - radius * 0.5f),
                strokeWidth = radius * 0.035f,
            )

            // 脸
            scope.drawCircle(color = Color(0xFF1E293B), radius = radius * 0.05f, center = Offset(c.x - radius * 0.1f, c.y - radius * 0.06f))
            scope.drawCircle(color = Color(0xFF1E293B), radius = radius * 0.05f, center = Offset(c.x + radius * 0.12f, c.y - radius * 0.06f))
            scope.drawArc(
                color = Color(0xFF64748B),
                startAngle = 10f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(c.x - radius * 0.12f, c.y + radius * 0.02f),
                size = Size(radius * 0.24f, radius * 0.12f),
                style = Stroke(radius * 0.025f),
            )
        }
    }

    /** 发条鼹鼠：护目镜 + 头顶齿轮 + 大爪子 */
    private fun drawGearMole(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose) {
        drawShadow(scope, center, radius)
        drawPowerAura(scope, center, radius, pose, Color(0xFFFB923C))
        val step = walkCycle(pose)
        val c = center + Offset(0f, -abs(step) * radius * 0.03f)

        scope.withFacing(c, pose.facing) {
            val fur = Color(0xFF78716C)
            val furDark = Color(0xFF44403C)
            val brass = Color(0xFFFBBF24)

            // 爪
            val clawSwing = step * radius * 0.15f
            repeat(2) { i ->
                val ox = if (i == 0) -radius * 0.28f else radius * 0.14f
                val swing = if (i == 0) clawSwing else -clawSwing
                scope.drawRoundRect(
                    color = furDark,
                    topLeft = Offset(c.x + ox, c.y + radius * 0.12f + swing),
                    size = Size(radius * 0.18f, radius * 0.14f),
                    cornerRadius = CornerRadius(radius * 0.06f),
                )
                repeat(3) { j ->
                    scope.drawLine(
                        color = Color(0xFF292524),
                        start = Offset(c.x + ox + radius * 0.03f + j * radius * 0.05f, c.y + radius * 0.24f + swing),
                        end = Offset(c.x + ox + radius * 0.02f + j * radius * 0.05f, c.y + radius * 0.32f + swing),
                        strokeWidth = radius * 0.025f,
                    )
                }
            }

            // 身体
            scope.drawOval(
                brush = Brush.verticalGradient(listOf(fur, furDark)),
                topLeft = Offset(c.x - radius * 0.38f, c.y - radius * 0.08f),
                size = Size(radius * 0.76f, radius * 0.48f),
            )

            // 头
            scope.drawCircle(color = fur, radius = radius * 0.32f, center = Offset(c.x, c.y - radius * 0.28f))
            scope.drawCircle(color = furDark, radius = radius * 0.14f, center = Offset(c.x, c.y - radius * 0.12f))

            // 护目镜
            scope.drawRoundRect(
                color = brass.copy(alpha = 0.85f),
                topLeft = Offset(c.x - radius * 0.28f, c.y - radius * 0.38f),
                size = Size(radius * 0.56f, radius * 0.18f),
                cornerRadius = CornerRadius(radius * 0.08f),
            )
            scope.drawCircle(
                brush = Brush.radialGradient(listOf(Color(0xFF67E8F9), Color(0xFF0891B2))),
                radius = radius * 0.1f,
                center = Offset(c.x - radius * 0.1f, c.y - radius * 0.29f),
            )
            scope.drawCircle(
                brush = Brush.radialGradient(listOf(Color(0xFF67E8F9), Color(0xFF0891B2))),
                radius = radius * 0.1f,
                center = Offset(c.x + radius * 0.1f, c.y - radius * 0.29f),
            )

            // 齿轮
            val gearSpin = if (pose.isMoving) pose.animPhase * 40f else pose.animPhase * 12f
            scope.rotate(gearSpin, Offset(c.x, c.y - radius * 0.52f)) {
                drawHex(scope, Offset(c.x, c.y - radius * 0.52f), radius * 0.12f, brass, fill = true)
                scope.drawCircle(color = furDark, radius = radius * 0.04f, center = Offset(c.x, c.y - radius * 0.52f))
            }

            // 鼻子
            scope.drawCircle(color = Color(0xFFFCA5A5), radius = radius * 0.05f, center = Offset(c.x, c.y - radius * 0.18f))
        }
    }
}
