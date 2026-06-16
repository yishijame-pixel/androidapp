package com.example.funlife.social.game.engine.pacmaze

fun PacMazeEntity.isPlayerPac(): Boolean =
    role == "pac" || role == "pac_a" || role == "pac_b"

fun PacMazeWorldState.playerPacs(): List<PacMazeEntity> =
    entities.filter { it.isPlayerPac() }

fun PacMazeWorldState.primaryPac(): PacMazeEntity? =
    entities.firstOrNull { it.role == "pac" }
        ?: entities.firstOrNull { it.isPlayerPac() }

fun PacMazeWorldState.pacById(id: String): PacMazeEntity? =
    entities.firstOrNull { it.id == id && it.isPlayerPac() }

/** 开局幽灵解禁倒计时：应在哪位玩家头顶显示「你 / 安全期」标记。 */
fun PacMazeWorldState.ghostReleaseHintEntityId(onlineLocalEntityId: String = ""): String? {
    if (ghostReleaseTicksLeft <= 0) return null
    if (onlineLocalEntityId.isNotBlank()) {
        return playerPacs().firstOrNull { it.id == onlineLocalEntityId }?.id
    }
    return primaryPac()?.id ?: playerPacs().firstOrNull()?.id
}

fun PacMazeWorldState.ghostReleaseSecondsCeil(): Int =
    ((ghostReleaseTicksLeft + PacMazeConstants.TICKS_PER_SECOND - 1) / PacMazeConstants.TICKS_PER_SECOND)
        .coerceAtLeast(0)
