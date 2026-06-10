package com.example.funlife.ui.screens.pacmaze.character

enum class PacMazeCharacterId(
    val storageKey: String,
    val displayName: String,
    val subtitle: String,
    val emoji: String,
) {
    CLASSIC_PAC("classic_pac", "经典豆人", "街机原味 · 张嘴吃豆", "🟡"),
    SCHOLAR("scholar", "小书童", "青衫束发 · 庭院游侠", "📚"),
    LANTERN_FOX("lantern_fox", "提灯小狐", "宫灯引路 · 园林灵物", "🦊"),
    CANDY_SPIRIT("candy_spirit", "糖纸精灵", "彩虹糖纸 · 弹跳闯关", "🍬"),
    DATA_CORE("data_core", "数据核心", "六边芯片 · 霓虹拖尾", "💠"),
    BUBBLE_SLIME("bubble_slime", "气泡史莱姆", "咕嘟冒泡 · 弹性过关", "🫧"),
    NOODLE_PHANTOM("noodle_phantom", "拉面精", "面条成精 · 晃晃悠悠", "🍜"),
    GEAR_MOLE("gear_mole", "发条鼹鼠", "黄铜齿轮 · 挖地突进", "⚙️"),
    ;

    companion object {
        val selectable: List<PacMazeCharacterId> = entries

        fun fromStorage(raw: String): PacMazeCharacterId =
            entries.firstOrNull { it.storageKey == raw } ?: CLASSIC_PAC
    }
}

fun PacMazeCharacterId.hasPowerAura(): Boolean = this == PacMazeCharacterId.DATA_CORE
