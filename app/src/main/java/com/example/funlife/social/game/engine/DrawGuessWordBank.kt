package com.example.funlife.social.game.engine

/** 内置词库（首期不接 AI 生成）。 */
object DrawGuessWordBank {
    private val words = listOf(
        "苹果", "香蕉", "火锅", "蛋糕", "咖啡",
        "猫咪", "小狗", "熊猫", "老虎", "兔子",
        "太阳", "月亮", "彩虹", "雨伞", "雪花",
        "篮球", "足球", "游泳", "跑步", "自行车",
        "手机", "电脑", "电视", "相机", "耳机",
        "医生", "警察", "厨师", "老师", "画家",
        "长城", "金字塔", "埃菲尔铁塔", "自由女神", "故宫",
        "守株待兔", "画蛇添足", "对牛弹琴", "亡羊补牢", "掩耳盗铃",
        "泰坦尼克", "哈利波特", "蜘蛛侠", "皮卡丘", "米老鼠",
    ).map { it.trim() }.filter { it.isNotBlank() }

    fun randomWord(exclude: Set<String> = emptySet()): String {
        val pool = words.filter { it !in exclude }
        return (pool.ifEmpty { words }).random()
    }
}
