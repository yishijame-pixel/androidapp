package com.example.funlife.ui.screens.pacmaze

enum class PacMazeMechanismKind(val glyph: String, val label: String) {
    LASER("⌁", "激光"),
    PORTAL("◎", "传送"),
    ENERGY_GATE("⚡", "能量门"),
    MOVING_WALL("⟳", "移动墙"),
    TURRET("⊕", "炮塔"),
}

object PacMazeLevelMechanisms {
    fun forLevel(levelId: Int): List<PacMazeMechanismKind> = buildList {
        if (levelId >= 5) add(PacMazeMechanismKind.LASER)
        if (levelId >= 6) add(PacMazeMechanismKind.PORTAL)
        if (levelId >= 10) add(PacMazeMechanismKind.ENERGY_GATE)
        if (levelId >= 14) add(PacMazeMechanismKind.MOVING_WALL)
        if (levelId >= 17) add(PacMazeMechanismKind.TURRET)
    }
}

fun tutorialHintFor(levelId: Int): String = when (levelId) {
    14 -> "教学：等移动墙开放 → 进西/东闸道 → 抵达核心"
    15 -> "十字激光网下走闸道边带，利用相位间隙"
    16 -> "能量门阵：门开冲线，门关绕侧道"
    in 17..19 -> "炮塔 + 移动墙组合，先清翼舱再进核心"
    in 20..22 -> "能量门与移动墙同步，把握条纹开放窗"
    23 -> "地狱终局：六厂 + 全机关，双翼必访"
    else -> ""
}
