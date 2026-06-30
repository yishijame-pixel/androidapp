package com.example.funlife.game.platformer

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object PlatformerPhysics {
    val PLAYER_W: Float get() = playerW(PLATFORMER_TILE_PX)
    val PLAYER_H: Float get() = playerH(PLATFORMER_TILE_PX)

    fun playerW(tilePx: Int): Float = tilePx * 0.62f
    fun playerH(tilePx: Int): Float = tilePx * 0.88f

    private const val MAX_SPEED = 260f
    private const val ACCEL = 2300f
    private const val DECEL = 2900f
    private const val AIR_ACCEL = 1750f
    private const val JUMP_VELOCITY = -565f
    /** 二段跳低于首段，避免飞太高。 */
    private const val DOUBLE_JUMP_MUL = 0.72f
    private const val GRAVITY = 1520f
    private const val MAX_FALL = 820f
    private const val COYOTE_SEC = 0.12f
    private const val JUMP_BUFFER_SEC = 0.14f
    private const val JUMP_CUT = 0.42f

    private const val LOCOMOTE_ON_SPEED = 28f
    private const val LOCOMOTE_OFF_SPEED = 6f
    /**
     * 非小鸡 catalog 角色兜底；小鸡用 [PlatformerPlayerSprites.walkAnimPhasePerSec]（24fps）。
     */
    private const val PLAYER_WALK_ANIM_PHASE_PER_SEC_DEFAULT = 4.8f
    /** 垂直碰撞/贴地判定容差（像素），避免 y+h 恰好等于 tileTop 时漏判 grounded。 */
    private const val SURFACE_CONTACT_EPS = 1.5f
    /** 脚底横向有效宽度占碰撞箱比例（勿只用中心点，否则平台边缘会穿地）。 */
    private const val FOOT_SUPPORT_WIDTH_FRAC = 0.55f

    fun tick(
        world: PlatformerWorld,
        input: PlatformerInput,
        dt: Float,
        viewWorldW: Float = PlatformerViewport.VISIBLE_TILES_W * PLATFORMER_TILE_PX,
        time: Float = 0f,
    ): PlatformerWorld {
        if (world.phase != PlatformerPhase.PLAYING) return world

        val tile = world.tileF
        val scale = tile / PLATFORMER_TILE_PX
        val pw = playerW(world.tilePx)
        val ph = playerH(world.tilePx)

        var p = world.player
        val wasGrounded = p.grounded

        var coyote = if (wasGrounded) COYOTE_SEC else max(0f, p.coyoteSec - dt)
        var airJumps = if (wasGrounded) 1 else p.airJumpsLeft
        var jumpBuffer = if (input.jumpPressed) JUMP_BUFFER_SEC else max(0f, p.jumpBufferSec - dt)
        var jumpActive = p.jumpActive
        var jumpCanCut = p.jumpCanCut

        val targetDir = when {
            world.endlessMode -> if (input.right) 1f else 0f
            input.left && !input.right -> -1f
            input.right && !input.left -> 1f
            else -> 0f
        }

        if (input.attackPressed) {
            p = PlatformerCombat.tryBeginAttack(p, world.characterId, airborne = !wasGrounded)
        }
        if (input.rangedPressed) {
            p = PlatformerRangedCombat.tryBeginRanged(
                p, world.characterId,
                airborne = !wasGrounded,
                locomoting = p.locomoting || targetDir != 0f || abs(p.vx) > LOCOMOTE_ON_SPEED,
            )
        }
        p = PlatformerCombat.tickAttackTimers(p, dt)
        p = PlatformerRangedCombat.tickRangedTimers(p, dt)

        // 水平加减速（无尽模式：自动向前跑，仅保留跳跃与小幅修正）
        val accel = if (wasGrounded) ACCEL else AIR_ACCEL
        var vx = p.vx
        if (world.endlessMode) {
            val runSpeed = world.endlessScrollSpeed.coerceIn(
                PlatformerEndlessRunner.BASE_SCROLL_SPEED,
                PlatformerEndlessRunner.MAX_SCROLL_SPEED,
            )
            vx = runSpeed
            if (targetDir > 0f) vx += 35f
        } else if (targetDir != 0f) {
            vx += targetDir * accel * dt
            vx = vx.coerceIn(-MAX_SPEED, MAX_SPEED)
        } else {
            val cut = (if (wasGrounded) DECEL else DECEL * 0.6f) * dt
            vx = when {
                abs(vx) <= cut -> 0f
                vx > 0f -> vx - cut
                else -> vx + cut
            }
        }

        if (p.attackAnimSecLeft > 0f && wasGrounded) {
            vx *= 0.32f
        }
        if (p.rangedAnimSecLeft > 0f && wasGrounded) {
            vx *= if (p.rangedClip == PlatformerAnimClipRef.BASKETBALL) 0.48f else 0.45f
        }

        p = p.copy(
            vx = vx,
            facingRight = when {
                world.endlessMode -> true
                targetDir > 0f -> true
                targetDir < 0f -> false
                abs(vx) > 12f -> vx > 0f
                else -> p.facingRight
            },
        )

        var vy = p.vy

        // 跳跃判定在位移积分之前（土狼 / 缓冲 / 二段）
        val wantJump = input.jumpPressed || jumpBuffer > 0f
        if (wantJump) {
            when {
                wasGrounded || coyote > 0f -> {
                    vy = JUMP_VELOCITY * scale
                    coyote = 0f
                    jumpBuffer = 0f
                    jumpActive = true
                    jumpCanCut = true
                }
                airJumps > 0 -> {
                    vy = JUMP_VELOCITY * DOUBLE_JUMP_MUL * scale
                    airJumps--
                    jumpBuffer = 0f
                    jumpActive = true
                    jumpCanCut = false
                }
            }
        }

        vy += GRAVITY * scale * dt
        vy = min(vy, MAX_FALL * scale)

        if (!input.jumpHeld && jumpActive && jumpCanCut && vy < 0f) {
            vy *= JUMP_CUT
            jumpActive = false
        }

        var nx = p.x + vx * dt
        var ny = p.y + vy * dt

        val resolvedX = resolveAxis(world, nx, p.y, pw, ph, horizontal = true, prevVy = vy)
        nx = resolvedX.first
        val resolvedY = resolveAxis(world, nx, ny, pw, ph, horizontal = false, prevVy = vy)
        ny = resolvedY.first
        var grounded = resolvedY.second
        if (resolvedY.third) vy = 0f
        if (resolvedX.third) vx = 0f

        if (grounded) {
            coyote = COYOTE_SEC
            airJumps = 1
            jumpActive = false
            jumpCanCut = false
            if (PlatformerHazards.springBoostAt(world, nx, ny, pw, ph)) {
                vy = PlatformerHazards.SPRING_VELOCITY * scale
                grounded = false
                coyote = 0f
            }
        }

        if (grounded && !feetOnGround(world, nx, ny, pw, ph)) {
            grounded = false
        } else if (!grounded && vy >= -2f && feetOnGround(world, nx, ny, pw, ph)) {
            // 贴地静止时 y+h 可能恰好等于 tileTop，resolveAxis 未命中；此处补判并吸附
            grounded = true
            vy = 0f
            ny = snapFeetToSupport(world, nx, ny, pw, ph)
        }

        var enemies = PlatformerEnemySystem.tick(world, world.enemies, dt, time)
        var hitSparks = world.hitSparks.mapNotNull { spark ->
            val age = spark.ageSec + dt
            if (age >= PlatformerHitSpark.LIFETIME_SEC) null else spark.copy(ageSec = age)
        }
        enemies = enemies.map { enemy ->
            if (!enemy.alive) return@map enemy
            if (PlatformerEnemySystem.stompDefeats(enemy, nx, ny, pw, ph, vy, world.tilePx)) {
                vy = JUMP_VELOCITY * 0.55f * scale
                grounded = false
                return@map enemy.copy(alive = false)
            }
            enemy
        }
        if (p.attackAnimSecLeft > 0f) {
            val (meleeEnemies, sparks) = PlatformerCombat.applyMeleeHits(
                enemies, p, nx, ny, pw, ph, world.tilePx,
            )
            enemies = meleeEnemies
            hitSparks = hitSparks + sparks
        }

        var projectiles = world.projectiles
        val nextProjId = (projectiles.maxOfOrNull { it.id } ?: -1) + 1
        PlatformerRangedCombat.trySpawnProjectile(
            p, world.characterId, nx, ny, pw, ph, world.tilePx, nextProjId,
        )?.let { spawn ->
            p = spawn.player
            projectiles = projectiles + spawn.projectile
        }

        val (traps, projectilesRaw) = PlatformerTrapSystem.tick(
            world.traps, projectiles, dt, time, world.tilePx,
        )
        val projectilesMario = PlatformerChickBasketball.tickProjectiles(world, projectilesRaw, dt)
        val projectilesWall = PlatformerTrapSystem.filterProjectiles(world, projectilesMario)
        val (enemiesAfterShots, playerProjectiles, shotSparks) =
            PlatformerRangedCombat.applyPlayerProjectileHits(
                enemies, projectilesWall, world.tilePx,
            )
        enemies = enemiesAfterShots
        hitSparks = hitSparks + shotSparks
        val lethalProjectileHit = playerProjectiles.any { proj ->
            PlatformerTrapSystem.projectileHitsPlayer(proj, nx, ny, pw, ph, world.tilePx)
        }
        val projectilesAfterPlayer = PlatformerTrapSystem.removeProjectilesHittingPlayer(
            playerProjectiles, nx, ny, pw, ph, world.tilePx,
        )

        // 按住方向即算行走（贴墙 vx=0 也播 walk）；松手且速度很低才停
        val locomoting = when {
            p.locomoting -> targetDir != 0f || abs(vx) > LOCOMOTE_OFF_SPEED
            else -> targetDir != 0f || abs(vx) > LOCOMOTE_ON_SPEED
        }
        val walkPhaseRate = when (world.characterId) {
            PlatformerCharacterId.CHICK_PRO_MAX -> PlatformerPlayerSprites.walkAnimPhasePerSec()
            else -> PLAYER_WALK_ANIM_PHASE_PER_SEC_DEFAULT
        }
        val anim = if (locomoting) {
            p.animPhase + dt * walkPhaseRate
        } else if (p.locomoting) {
            0f
        } else {
            p.animPhase
        }

        val gems = world.gems.map { g ->
            if (g.collected) g
            else {
                val dx = (nx + pw / 2f) - g.x
                val dy = (ny + ph / 2f) - g.y
                if (dx * dx + dy * dy < (tile * 0.65f) * (tile * 0.65f)) g.copy(collected = true) else g
            }
        }
        val collected = gems.count { it.collected }

        val reachedGoal = if (world.endlessMode) {
            false
        } else when {
            world.goalX != null && world.goalY != null -> {
                val px = nx + pw / 2f
                val py = ny + ph / 2f
                abs(px - world.goalX!!) < tile * 1.2f && abs(py - world.goalY!!) < tile * 1.4f
            }
            else -> findGoal(world)?.let { (gx, gy) ->
                val px = nx + pw / 2f
                val py = ny + ph / 2f
                abs(px - gx) < tile * 0.7f && abs(py - gy) < tile * 0.85f
            } ?: false
        }

        val phase = if (reachedGoal) PlatformerPhase.LEVEL_CLEAR else world.phase

        val camTarget = (nx + pw / 2f) - viewWorldW * 0.38f
        val cam = world.cameraX + (camTarget - world.cameraX) * min(1f, dt * 7f)
        val maxCam = max(0f, world.width * tile - viewWorldW)
        val camClamped = cam.coerceIn(0f, maxCam)

        val stepped = world.copy(
            player = p.copy(
                x = nx, y = ny, vx = vx, vy = vy,
                grounded = grounded, animPhase = anim, locomoting = locomoting,
                airJumpsLeft = airJumps, coyoteSec = coyote,
                jumpActive = jumpActive, jumpCanCut = jumpCanCut,
                jumpBufferSec = jumpBuffer,
            ),
            gems = gems,
            gemsCollected = collected,
            enemies = enemies,
            traps = traps,
            projectiles = projectilesAfterPlayer,
            lethalProjectileHit = lethalProjectileHit,
            cameraX = camClamped,
            phase = phase,
            hitSparks = hitSparks,
        )
        val skyTick = PlatformerSkyChickSystem.tick(stepped, nx, ny, pw, ph, dt, time)
        val withSky = stepped.copy(
            skyChick = skyTick.skyChick,
            skyEggs = skyTick.skyEggs,
            lethalSkyEggHit = skyTick.lethalSkyEggHit,
        )
        return if (withSky.endlessMode) {
            PlatformerEndlessRunner.afterPhysics(withSky, dt, viewWorldW)
        } else if (withSky.campaignScrollMode) {
            PlatformerCampaignScrollRunner.afterPhysics(withSky, dt, viewWorldW)
        } else {
            withSky
        }
    }

    private fun findGoal(world: PlatformerWorld): Pair<Float, Float>? {
        val tile = world.tileF
        for (y in 0 until world.height) {
            for (x in 0 until world.width) {
                if (world.cellAt(x, y) == PlatformerCell.GOAL) {
                    return (x * tile + tile / 2f) to (y * tile + tile / 2f)
                }
            }
        }
        return null
    }

    private fun resolveAxis(
        world: PlatformerWorld,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        horizontal: Boolean,
        prevVy: Float,
    ): Triple<Float, Boolean, Boolean> {
        val tile = world.tileF
        var pos = if (horizontal) x else y
        var grounded = false
        var hit = false
        val maxT = if (horizontal) world.width else world.height
        val minTile = max(0, if (horizontal) (pos / tile).toInt() - 1 else (pos / tile).toInt() - 1)
        val maxTile = min(maxT - 1, if (horizontal) ((pos + w) / tile).toInt() + 1 else ((pos + h) / tile).toInt() + 1)

        if (horizontal) {
            for (ty in (y / tile).toInt()..((y + h - 1) / tile).toInt()) {
                for (tx in minTile..maxTile) {
                    val cell = world.cellAt(tx, ty)
                    if (!cellBlocksHorizontal(cell, playerY = y, playerH = h, tileY = ty, tileSize = tile)) {
                        continue
                    }
                    val tileL = tx * tile
                    val tileR = tileL + tile
                    if (x + w > tileL && x < tileR && y + h > ty * tile && y < (ty + 1) * tile) {
                        pos = if (x + w / 2f < tileL + tile / 2f) tileL - w else tileR
                        hit = true
                    }
                }
            }
        } else {
            for (tx in (x / tile).toInt()..((x + w - 1) / tile).toInt()) {
                for (ty in minTile..maxTile) {
                    val cell = world.cellAt(tx, ty)
                    val fromAbove = prevVy >= 0f
                    if (!cellBlocks(cell, fromAbove = fromAbove)) continue
                    val tileT = ty * tile
                    val tileB = tileT + tile
                    if (x + w > tx * tile && x < (tx + 1) * tile &&
                        y + h >= tileT - SURFACE_CONTACT_EPS && y < tileB
                    ) {
                        if ((fromAbove && cell != PlatformerCell.PLATFORM && cell != PlatformerCell.SPRING) ||
                            cell == PlatformerCell.SOLID ||
                            cell == PlatformerCell.CRATE
                        ) {
                            if (y + h / 2f < tileT + tile / 2f) {
                                // 落脚以脚底中心为准，避免宽碰撞箱桥接沟壑
                                if (!footCenterSupported(world, x, w, y + h, tileT, cell)) continue
                                pos = tileT - h
                                grounded = true
                            } else {
                                pos = tileB
                            }
                            hit = true
                        } else if (
                            (cell == PlatformerCell.PLATFORM || cell == PlatformerCell.SPRING) && fromAbove
                        ) {
                            val feet = y + h
                            if (feet >= tileT && feet <= tileB + 12f && prevVy >= 0f) {
                                if (!footCenterSupported(world, x, w, feet, tileT, cell)) continue
                                pos = tileT - h
                                grounded = true
                                hit = true
                            }
                        }
                    }
                }
            }
        }
        return Triple(pos, grounded, hit)
    }

    private fun feetOnGround(world: PlatformerWorld, x: Float, y: Float, w: Float, h: Float): Boolean {
        val tile = world.tileF
        val feetY = y + h
        val tolerance = 14f * (tile / PLATFORMER_TILE_PX)
        val baseTy = (feetY / tile).toInt()
        for (ty in (baseTy - 1)..(baseTy + 1)) {
            if (ty !in 0 until world.height) continue
            val tileTop = ty * tile
            if (feetY < tileTop - SURFACE_CONTACT_EPS || feetY > tileTop + tolerance) continue
            if (footSpanSupported(world, x, w, feetY, tileTop, ty)) return true
        }
        return false
    }

    private fun snapFeetToSupport(
        world: PlatformerWorld,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
    ): Float {
        val tile = world.tileF
        val feetY = y + h
        val tolerance = 14f * (tile / PLATFORMER_TILE_PX)
        val baseTy = (feetY / tile).toInt()
        for (ty in (baseTy - 1)..(baseTy + 1)) {
            if (ty !in 0 until world.height) continue
            val tileTop = ty * tile
            if (feetY < tileTop - SURFACE_CONTACT_EPS || feetY > tileTop + tolerance) continue
            if (footSpanSupported(world, x, w, feetY, tileTop, ty)) {
                return tileTop - h
            }
        }
        return y
    }

    private fun footSpanSupported(
        world: PlatformerWorld,
        x: Float,
        w: Float,
        feetY: Float,
        tileTop: Float,
        ty: Int,
    ): Boolean {
        val tile = world.tileF
        if (feetY < tileTop - SURFACE_CONTACT_EPS ||
            feetY > tileTop + 14f * (tile / PLATFORMER_TILE_PX)
        ) {
            return false
        }
        val halfSpan = w * FOOT_SUPPORT_WIDTH_FRAC * 0.5f
        val footCx = x + w / 2f
        val txMin = ((footCx - halfSpan) / tile).toInt()
        val txMax = ((footCx + halfSpan) / tile).toInt()
        for (tx in txMin..txMax) {
            if (tx !in 0 until world.width) continue
            val cell = world.cellAt(tx, ty)
            if (cell == PlatformerCell.SOLID || cell == PlatformerCell.PLATFORM ||
                cell == PlatformerCell.CRATE || cell == PlatformerCell.SPRING
            ) {
                return true
            }
        }
        return false
    }

    private fun footCenterSupported(
        world: PlatformerWorld,
        x: Float,
        w: Float,
        feetY: Float,
        tileTop: Float,
        landingCell: PlatformerCell,
    ): Boolean {
        if (!isSupportCell(landingCell)) return false
        val tile = world.tileF
        if (feetY < tileTop - SURFACE_CONTACT_EPS ||
            feetY > tileTop + 14f * (tile / PLATFORMER_TILE_PX)
        ) {
            return false
        }
        val ty = (tileTop / tile).toInt()
        val footCx = x + w / 2f
        val txCenter = (footCx / tile).toInt()
        if (txCenter in 0 until world.width && world.cellAt(txCenter, ty) == landingCell) {
            return true
        }
        // 平台边缘：中心略出界但脚内侧仍落在同一格时可落脚
        return footSpanHasCell(world, x, w, ty, landingCell, FOOT_SUPPORT_WIDTH_FRAC * 0.65f)
    }

    private fun footSpanHasCell(
        world: PlatformerWorld,
        x: Float,
        w: Float,
        ty: Int,
        landingCell: PlatformerCell,
        widthFrac: Float = FOOT_SUPPORT_WIDTH_FRAC,
    ): Boolean {
        val tile = world.tileF
        val halfSpan = w * widthFrac * 0.5f
        val footCx = x + w / 2f
        val txMin = ((footCx - halfSpan) / tile).toInt()
        val txMax = ((footCx + halfSpan) / tile).toInt()
        for (tx in txMin..txMax) {
            if (tx !in 0 until world.width) continue
            if (world.cellAt(tx, ty) == landingCell) return true
        }
        return false
    }

    private fun isSupportCell(cell: PlatformerCell): Boolean =
        cell == PlatformerCell.SOLID ||
            cell == PlatformerCell.PLATFORM ||
            cell == PlatformerCell.CRATE ||
            cell == PlatformerCell.SPRING

    private fun cellBlocks(cell: PlatformerCell, fromAbove: Boolean): Boolean =
        when (cell) {
            PlatformerCell.SOLID, PlatformerCell.CRATE -> true
            PlatformerCell.PLATFORM, PlatformerCell.SPRING -> fromAbove
            else -> false
        }

    /** 平台/弹簧在水平方向：脚底低于台面时视为侧墙，防止穿墙。 */
    private fun cellBlocksHorizontal(
        cell: PlatformerCell,
        playerY: Float,
        playerH: Float,
        tileY: Int,
        tileSize: Float,
    ): Boolean = when (cell) {
        PlatformerCell.SOLID, PlatformerCell.CRATE -> true
        PlatformerCell.PLATFORM, PlatformerCell.SPRING -> {
            val tileTop = tileY * tileSize
            val playerBottom = playerY + playerH
            playerBottom > tileTop + tileSize * 0.12f
        }
        else -> false
    }

    fun isDead(world: PlatformerWorld, time: Float = 0f): Boolean {
        val tile = world.tileF
        val fallY = (world.height + 2) * tile
        val p = world.player
        val pw = playerW(world.tilePx)
        val ph = playerH(world.tilePx)
        return p.y > fallY ||
            world.lethalProjectileHit ||
            world.lethalSkyEggHit ||
            PlatformerHazards.hitsSpike(world, p.x, p.y, pw, ph) ||
            world.enemies.any { enemy ->
                enemy.alive && PlatformerEnemySystem.hitsPlayer(enemy, p.x, p.y, pw, ph, world.tilePx)
            } ||
            world.traps.any { trap ->
                PlatformerTrapSystem.hitsPlayer(trap, p.x, p.y, pw, ph, world.tilePx, time)
            }
    }
}
