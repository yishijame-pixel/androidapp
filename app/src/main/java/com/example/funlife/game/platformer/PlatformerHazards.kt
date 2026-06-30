package com.example.funlife.game.platformer



import kotlin.math.max

import kotlin.math.min



object PlatformerHazards {



    const val SPRING_VELOCITY = -720f



    /** 与渲染器 drawSpike 中 tipY 比例一致。 */

    private const val SPIKE_TIP_FRAC = 0.38f

    private const val FOOT_HALF_WIDTH_FRAC = 0.22f



    fun isSpike(cell: PlatformerCell): Boolean = cell == PlatformerCell.SPIKE



    fun isSpring(cell: PlatformerCell): Boolean = cell == PlatformerCell.SPRING



    fun blocksLikePlatform(cell: PlatformerCell): Boolean =

        cell == PlatformerCell.PLATFORM || cell == PlatformerCell.SPRING



    /** 脚底踩进地刺尖端区域才判定死亡（与贴图对齐，避免悬空误伤）。 */

    fun hitsSpike(world: PlatformerWorld, x: Float, y: Float, w: Float, h: Float): Boolean {

        val tile = PLATFORMER_TILE_PX.toFloat()

        val feetY = y + h

        val footCx = x + w / 2f

        val footL = footCx - w * FOOT_HALF_WIDTH_FRAC

        val footR = footCx + w * FOOT_HALF_WIDTH_FRAC

        val minTx = max(0, (footL / tile).toInt() - 1)

        val maxTx = min(world.width - 1, (footR / tile).toInt() + 1)

        val ty = ((feetY - 1f) / tile).toInt()

        if (ty !in 0 until world.height) return false

        for (tx in minTx..maxTx) {

            if (!isSpike(world.cellAt(tx, ty))) continue

            val tileL = tx * tile

            val tileR = tileL + tile

            val spikeTipY = ty * tile + tile * SPIKE_TIP_FRAC

            if (feetY >= spikeTipY && footR > tileL && footL < tileR) return true

        }

        return false

    }



    /** 脚底踩在弹簧格上时触发。 */

    fun springBoostAt(world: PlatformerWorld, x: Float, y: Float, w: Float, h: Float): Boolean {

        val feetY = y + h

        val footCx = x + w / 2f

        val tx = (footCx / PLATFORMER_TILE_PX).toInt()

        val ty = ((feetY - 2f) / PLATFORMER_TILE_PX).toInt()

        if (tx !in 0 until world.width || ty !in 0 until world.height) return false

        return isSpring(world.cellAt(tx, ty))

    }

}


