package com.example.funlife.social.game.engine.pacmaze

import kotlin.math.abs

object PacMazeCombat {

    fun tryFireAttack(state: PacMazeWorldState): PacMazeWorldState {
        if (state.attackCharges <= 0 || state.attackCooldownTicksLeft > 0) return state
        val pac = state.entities.firstOrNull { it.role == "pac" } ?: return state
        val dir = pac.facing
        val projectile = PacMazeProjectile(
            id = "proj_${state.tick}",
            x = pac.x,
            y = pac.y,
            direction = dir,
        )
        return state.copy(
            attackCharges = state.attackCharges - 1,
            attackCooldownTicksLeft = PacMazeConstants.ATTACK_COOLDOWN_TICKS,
            projectiles = state.projectiles + projectile,
        )
    }

    fun tickProjectiles(state: PacMazeWorldState): PacMazeWorldState {
        if (state.projectiles.isEmpty()) return state

        val step = PacMazeConstants.PROJECTILE_SPEED_CELLS_PER_SEC / PacMazeConstants.TICKS_PER_SECOND
        var world = state
        val remaining = mutableListOf<PacMazeProjectile>()

        state.projectiles.forEach { projectile ->
            val (dx, dy) = projectile.direction.delta()
            val (nx, ny) = PacMazeMotion.integrateWithBodyCollision(
                state = world,
                x = projectile.x,
                y = projectile.y,
                nx = projectile.x + dx * step,
                ny = projectile.y + dy * step,
                forGhost = false,
            )
            val moved = abs(nx - projectile.x) > 1e-4f || abs(ny - projectile.y) > 1e-4f
            if (!moved) return@forEach

            val advanced = projectile.copy(x = nx, y = ny)
            val hitGhostId = world.entities.firstOrNull { entity ->
                entity.role == "ghost" &&
                    entity.ghostMode != GhostMode.EATEN &&
                    PacMazeMotion.tileX(entity.x) == PacMazeMotion.tileX(advanced.x) &&
                    PacMazeMotion.tileY(entity.y) == PacMazeMotion.tileY(advanced.y)
            }?.id

            if (hitGhostId != null) {
                world = world.copy(
                    entities = world.entities.map { entity ->
                        if (entity.id != hitGhostId) entity
                        else entity.copy(ghostMode = GhostMode.EATEN, facing = entity.facing)
                    },
                    score = world.score + PacMazeConstants.GHOST_SCORE,
                )
            } else {
                remaining.add(advanced)
            }
        }

        return world.copy(projectiles = remaining)
    }
}
