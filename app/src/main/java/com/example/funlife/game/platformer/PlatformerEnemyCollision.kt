package com.example.funlife.game.platformer

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** 敌人与瓦片世界的碰撞与贴地检测（与玩家物理规则对齐）。 */
object PlatformerEnemyCollision {

    private fun walkable(cell: PlatformerCell): Boolean =
        cell == PlatformerCell.SOLID ||
            cell == PlatformerCell.PLATFORM ||
            cell == PlatformerCell.CRATE ||
            cell == PlatformerCell.SPRING

    private fun blocksBody(cell: PlatformerCell): Boolean =
        cell == PlatformerCell.SOLID || cell == PlatformerCell.CRATE

    /** 脚底正下方可站立面顶边；容差约 1/3 格。 */
    fun groundTopUnder(world: PlatformerWorld, footCx: Float, feetY: Float): Float? {
        val tile = world.tileF
        val tx = (footCx / tile).toInt().coerceIn(0, world.width - 1)
        var best: Float? = null
        var bestDist = Float.MAX_VALUE
        for (ty in 0 until world.height) {
            val cell = world.cellAt(tx, ty)
            if (!walkable(cell)) continue
            val top = ty * tile
            val dist = abs(feetY - top)
            if (dist <= tile * 0.38f && dist < bestDist) {
                bestDist = dist
                best = top
            }
        }
        return best
    }

    fun canStandAt(world: PlatformerWorld, x: Float, y: Float, w: Float, h: Float): Boolean {
        val feetY = y + h
        val footCx = x + w / 2f
        return groundTopUnder(world, footCx, feetY) != null &&
            !overlapsSolid(world, x, y, w, h)
    }

    fun overlapsSolid(world: PlatformerWorld, x: Float, y: Float, w: Float, h: Float): Boolean {
        val tile = world.tileF
        val minTx = max(0, (x / tile).toInt())
        val maxTx = min(world.width - 1, ((x + w - 0.5f) / tile).toInt())
        val minTy = max(0, (y / tile).toInt())
        val maxTy = min(world.height - 1, ((y + h - 0.5f) / tile).toInt())
        for (ty in minTy..maxTy) {
            for (tx in minTx..maxTx) {
                if (blocksBody(world.cellAt(tx, ty))) return true
            }
        }
        return false
    }

    fun resolveHorizontal(
        world: PlatformerWorld,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
    ): Pair<Float, Boolean> {
        val tile = world.tileF
        var pos = x
        var hit = false
        val minTx = max(0, (pos / tile).toInt() - 1)
        val maxTx = min(world.width - 1, ((pos + w) / tile).toInt() + 1)
        val minTy = max(0, (y / tile).toInt())
        val maxTy = min(world.height - 1, ((y + h - 1f) / tile).toInt())

        for (ty in minTy..maxTy) {
            for (tx in minTx..maxTx) {
                if (!blocksBody(world.cellAt(tx, ty))) continue
                val tileL = tx * tile
                val tileR = tileL + tile
                val tileT = ty * tile
                val tileB = tileT + tile
                if (pos + w > tileL + 1f && pos < tileR - 1f && y + h > tileT + 2f && y < tileB - 2f) {
                    pos = if (pos + w / 2f < tileL + tile / 2f) tileL - w else tileR
                    hit = true
                }
            }
        }
        return pos to hit
    }

    /** 若身体嵌入实心块，推到最近外侧（优先向上站到顶面）。 */
    fun depenetrate(world: PlatformerWorld, x: Float, y: Float, w: Float, h: Float): Pair<Float, Float> {
        var px = x
        var py = y
        repeat(6) {
            if (!overlapsSolid(world, px, py, w, h)) return px to py
            val tile = world.tileF
            val footCx = px + w / 2f
            val feetY = py + h
            val top = groundTopUnder(world, footCx, feetY)
            if (top != null) {
                py = top - h
                if (!overlapsSolid(world, px, py, w, h)) return px to py
            }
            val (rx, hitX) = resolveHorizontal(world, px, py, w, h)
            px = rx
            if (!hitX) {
                py -= tile * 0.25f
            }
        }
        return px to py
    }

    /** 仅在当前高度附近微调贴地，禁止吸附到下方立柱顶面。 */
    fun snapToGround(
        world: PlatformerWorld,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
    ): Float {
        val feetY = y + h
        val footCx = x + w / 2f
        val top = groundTopUnder(world, footCx, feetY) ?: return y
        return top - h
    }

    fun isLedgeAhead(
        world: PlatformerWorld,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        facingRight: Boolean,
    ): Boolean {
        val tile = world.tileF
        val feetY = y + h
        val probeX = if (facingRight) x + w + tile * 0.12f else x - tile * 0.12f
        return groundTopUnder(world, probeX, feetY) == null
    }

    fun findStandY(
        world: PlatformerWorld,
        tileX: Int,
        tileY: Int,
        h: Float,
    ): Float {
        val tile = world.tileF
        val tx = tileX.coerceIn(0, world.width - 1)
        val preferredFeetY = (tileY.coerceIn(0, world.height - 1) + 1) * tile
        var bestStandY = tileY * tile - h
        var bestDist = Float.MAX_VALUE
        for (ty in 0 until world.height) {
            if (!walkable(world.cellAt(tx, ty))) continue
            val top = ty * tile
            val standY = top - h
            val dist = abs(top - preferredFeetY)
            if (dist < bestDist) {
                bestDist = dist
                bestStandY = standY
            }
        }
        return bestStandY
    }

    /** 飞行敌人悬浮高度：贴最近站立面再抬高半格。 */
    fun findHoverY(
        world: PlatformerWorld,
        tileX: Int,
        tileY: Int,
        h: Float,
    ): Float {
        val tile = world.tileF
        val standY = findStandY(world, tileX, tileY, h)
        return standY - tile * 0.55f
    }

    /** 沿当前平台扫描有效巡逻区间，避免走入立柱或悬崖。 */
    fun computePatrolBounds(
        world: PlatformerWorld,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        defaultPatrolW: Float,
    ): Pair<Float, Float> {
        val tile = world.tileF
        val half = defaultPatrolW / 2f
        var left = x
        var right = x

        while (left - tile >= 0f && canStandAt(world, left - tile * 0.35f, y, w, h)) {
            left -= tile
        }
        while (right + w + tile <= world.width * tile &&
            canStandAt(world, right + tile * 0.35f, y, w, h)
        ) {
            right += tile
        }

        val cx = x + w / 2f
        val minLeft = (cx - half).coerceAtLeast(0f)
        val maxRight = (cx + half - w).coerceAtMost(world.width * tile - w)
        left = max(left, minLeft)
        right = min(right, maxRight)
        if (right < left) right = left
        return left to right
    }
}
