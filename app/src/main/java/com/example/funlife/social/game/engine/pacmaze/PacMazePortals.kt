package com.example.funlife.social.game.engine.pacmaze

/**
 * 001 / 002 成对传送门（左右位置不变，竖直跃迁，**保持进入方向**）：
 * - 玩家需 **分别** 踩过左右两扇传送门（各写入独立 [armedTag]）才能跃迁
 * - 两门都激活后：从 **上方** 向下进入 → 从对侧 **下方** 穿出；从 **下方** 向上进入 → 从对侧 **上方** 穿出
 */
object PacMazePortals {

    /** [PacMazeWorldState.visitedCheckpointTags] 前缀；每扇门 `LINK_ARMED:x,y`。 */
    const val LINK_ARMED_PREFIX = "LINK_ARMED:"

    /** @deprecated 旧版整对激活 tag；仅用于兼容读档，新逻辑不再写入。 */
    const val LINK_ARMED_TAG = "LINK_ARMED"

    data class PortalPair(
        val left: PacMazeMapMarker,
        val right: PacMazeMapMarker,
    )

    fun armedTag(marker: PacMazeMapMarker): String = armedTagAt(marker.x, marker.y)

    fun armedTagAt(x: Int, y: Int): String = "$LINK_ARMED_PREFIX$x,$y"

    fun isArmedTag(tag: String): Boolean =
        tag.startsWith(LINK_ARMED_PREFIX) || tag == LINK_ARMED_TAG

    fun isPortalArmed(state: PacMazeWorldState, marker: PacMazeMapMarker): Boolean =
        armedTag(marker) in state.visitedCheckpointTags

    fun isPortalArmedAt(state: PacMazeWorldState, x: Int, y: Int): Boolean =
        armedTagAt(x, y) in state.visitedCheckpointTags

    fun isPairReady(state: PacMazeWorldState, pair: PortalPair): Boolean =
        isPortalArmed(state, pair.left) && isPortalArmed(state, pair.right)

    /** 任意一对 LINK 传送门是否已全部激活（可跃迁）。 */
    fun isLinkArmed(state: PacMazeWorldState, level: PacMazeLevelConfig): Boolean {
        val pairs = pairs(level.markers, level.width)
        if (pairs.isEmpty()) return false
        if (LINK_ARMED_TAG in state.visitedCheckpointTags) return true
        return pairs.all { isPairReady(state, it) }
    }

    fun isLinkArmed(state: PacMazeWorldState): Boolean =
        LINK_ARMED_TAG in state.visitedCheckpointTags

    fun armedPortalCount(state: PacMazeWorldState, level: PacMazeLevelConfig): Int =
        portalMarkers(level).count { isPortalArmed(state, it) }

    fun portalCount(level: PacMazeLevelConfig): Int = portalMarkers(level).size

    fun portalMarkers(level: PacMazeLevelConfig): List<PacMazeMapMarker> =
        pairs(level.markers, level.width).flatMap { listOf(it.left, it.right) }

    fun portalMarkerAt(markers: List<PacMazeMapMarker>, width: Int, x: Int, y: Int): PacMazeMapMarker? =
        pairs(markers, width).flatMap { listOf(it.left, it.right) }.firstOrNull { it.x == x && it.y == y }

    fun portalMarkerAt(level: PacMazeLevelConfig, x: Int, y: Int): PacMazeMapMarker? =
        portalMarkerAt(level.markers, level.width, x, y)

    fun pairForMarker(markers: List<PacMazeMapMarker>, width: Int, marker: PacMazeMapMarker): PortalPair? =
        pairs(markers, width).firstOrNull { it.left.x == marker.x && it.left.y == marker.y || it.right.x == marker.x && it.right.y == marker.y }

    fun pairs(markers: List<PacMazeMapMarker>, width: Int = 0): List<PortalPair> {
        val linkPairs = markers
            .filter { it.kind == PacMazeMarkerKind.CHECKPOINT && it.tag == "LINK" }
            .sortedBy { it.y * 1000 + it.x }
            .chunked(2)
            .mapNotNull { chunk ->
                if (chunk.size == 2) PortalPair(left = chunk[0], right = chunk[1]) else null
            }
        if (linkPairs.isNotEmpty()) return linkPairs

        if (width <= 0) return emptyList()
        val edge = markers.filter {
            it.kind == PacMazeMarkerKind.CHECKPOINT &&
                (it.x <= 1 || it.x >= width - 2)
        }
        val left = edge.filter { it.x <= 1 }.minByOrNull { it.y } ?: return emptyList()
        val right = edge.filter { it.x >= width - 2 }.minByOrNull { it.y } ?: return emptyList()
        return listOf(PortalPair(left = left, right = right))
    }

    /** 玩家首次踏上某一扇 LINK 传送门时激活该门（不跃迁）。 */
    fun tryArmLinkPair(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
        level: PacMazeLevelConfig,
    ): PacMazeWorldState {
        if (entity.role != "pac") return state
        val portalPairs = pairs(level.markers, level.width)
        if (portalPairs.isEmpty()) return state

        val tx = PacMazeMotion.tileX(entity.x)
        val ty = PacMazeMotion.tileY(entity.y)
        val portal = portalPairs
            .flatMap { listOf(it.left, it.right) }
            .firstOrNull { it.x == tx && it.y == ty }
            ?: return state

        val tag = armedTag(portal)
        if (tag in state.visitedCheckpointTags) return state

        return state.copy(
            visitedCheckpointTags = state.visitedCheckpointTags + tag,
        )
    }

    fun applyTransit(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
        level: PacMazeLevelConfig,
    ): PacMazeEntity {
        val portalPairs = pairs(level.markers, level.width)
        if (portalPairs.isNotEmpty() && !isLinkArmed(state, level)) {
            return entity
        }

        val dir = entity.direction ?: return entity
        if (dir != Direction.UP && dir != Direction.DOWN) return entity

        val tx = PacMazeMotion.tileX(entity.x)
        val ty = PacMazeMotion.tileY(entity.y)
        val forGhost = entity.role == "ghost"

        for (pair in portalPairs) {
            val onLeft = tx == pair.left.x && ty == pair.left.y
            val onRight = tx == pair.right.x && ty == pair.right.y
            if (!onLeft && !onRight) continue
            if (!shouldWarp(entity, dir)) continue

            val exitPortal = if (onLeft) pair.right else pair.left
            val dest = resolveExitTile(state, exitPortal, dir, forGhost) ?: continue
            val warped = entity.copy(
                x = dest.first.toFloat(),
                y = dest.second.toFloat(),
                direction = dir,
                facing = dir,
            )
            if (PacMazeMotion.isPositionLegal(state, warped.x, warped.y, forGhost)) {
                return warped
            }
        }
        return entity
    }

    /** 刚进入传送格时触发（按实体中心在格内的竖直位置判断进出方向）。 */
    private fun shouldWarp(entity: PacMazeEntity, dir: Direction): Boolean {
        val ty = PacMazeMotion.tileY(entity.y)
        val frac = PacMazeMotion.centerY(entity.y) - ty
        return when (dir) {
            Direction.DOWN -> frac <= 0.35f
            Direction.UP -> frac >= 0.65f
            else -> false
        }
    }

    private fun resolveExitTile(
        state: PacMazeWorldState,
        exitPortal: PacMazeMapMarker,
        dir: Direction,
        forGhost: Boolean,
    ): Pair<Int, Int>? {
        val primaryY = when (dir) {
            Direction.DOWN -> exitPortal.y + 1
            Direction.UP -> exitPortal.y - 1
            else -> return null
        }
        val innerX = if (exitPortal.x < state.width / 2) {
            exitPortal.x + 1
        } else {
            exitPortal.x - 1
        }
        val candidates = buildList {
            add(exitPortal.x to primaryY)
            add(exitPortal.x to exitPortal.y)
            add(innerX to primaryY)
            for (dy in -2..2) {
                add(exitPortal.x to primaryY + dy)
            }
        }
        for ((x, y) in candidates) {
            if (PacMazeRules.isWalkable(state, x, y, forGhost)) return x to y
        }
        return null
    }
}
