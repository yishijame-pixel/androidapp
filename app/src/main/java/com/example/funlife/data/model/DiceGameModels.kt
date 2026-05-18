package com.example.funlife.data.model

import androidx.compose.ui.graphics.Color

/** 玩家 */
data class DicePlayer(
    val id: Long,
    val name: String,
    val color: Color,
    val emoji: String,
    var totalScore: Int = 0,
    var wins: Int = 0,
    var lastRoll: List<Int> = emptyList(),
    var lastSum: Int = 0
)

/** 游戏模式 */
enum class DiceGameMode(
    val displayName: String,
    val emoji: String,
    val description: String,
    val isDrinking: Boolean = false
) {
    COMPARE_SIZE(
        "比大小",
        "🎯",
        "所有人投掷，谁的点数最大谁赢，最小者输",
        false
    ),
    TRUTH_DARE(
        "真心话大冒险",
        "💋",
        "点数最小者抽取真心话或大冒险卡片",
        false
    ),
    DRINKING_BIG_DRINKS(
        "大者罚酒",
        "🍻",
        "点数最大的玩家喝一杯",
        true
    ),
    DRINKING_SMALL_DRINKS(
        "小者罚酒",
        "🍷",
        "点数最小的玩家喝一杯",
        true
    ),
    NUMBER_PENALTY(
        "数字定罚",
        "🥂",
        "投出指定数字（如6）的玩家喝一杯/接受惩罚",
        true
    ),
    BLACKJACK_21(
        "21点骰子",
        "🎲",
        "累计点数最接近21不超过者赢",
        false
    ),
    LIAR_DICE(
        "吹牛骰盅",
        "🎰",
        "依次报出全场骰子点数，怀疑则开盅验证",
        true
    );

    companion object {
        fun default() = COMPARE_SIZE
    }
}

/** 真心话/大冒险卡片 */
data class TruthOrDareCard(
    val type: CardType,
    val content: String
)

enum class CardType(val displayName: String, val emoji: String) {
    TRUTH("真心话", "💕"),
    DARE("大冒险", "🔥")
}

/** 题库 */
object TruthOrDareDatabase {
    val truthQuestions = listOf(
        "说出你最近一次脸红的事情",
        "你最喜欢在座的哪一位？",
        "你做过最尴尬的事是什么？",
        "你暗恋过的人现在还在联系吗？",
        "你手机里最近一张截图是什么？",
        "你最不愿意被人知道的小秘密是什么？",
        "如果只能保留一个 App，你会留哪个？",
        "你曾经做过最疯狂的事情？",
        "你哭得最厉害的一次是因为什么？",
        "你最近一次失眠是为了谁？",
        "讲一个你做过的傻事",
        "你最讨厌别人对你做什么？",
        "你最害怕的事情是什么？",
        "你最近偷偷在想谁？",
        "如果可以重来，你最想改变人生中哪一刻？",
        "你最喜欢什么类型的人？",
        "你的初恋是什么时候？",
        "你有没有偷偷喜欢过同性？",
        "你单身的真正原因是什么？",
        "你做过最浪漫的事是什么？",
        "你最近一次撒谎是什么？",
        "讲一件你后悔到现在的事",
        "你曾经在朋友面前装过什么？",
        "你最骄傲的一件事是什么？",
        "你最讨厌自己什么缺点？",
        "如果今天是世界末日，你最想见谁？",
        "你最舍不得删的聊天记录是和谁的？",
        "你最近一次哭是为了什么？",
        "你心目中的理想型是什么样子？",
        "你有没有暗恋过现场的某个人？"
    )

    val dareTasks = listOf(
        "学三种动物叫并模仿它们走路",
        "对在场任一异性说『我爱你』",
        "唱一首你最喜欢的歌的副歌",
        "向左边的人说一句肉麻情话",
        "做 10 个深蹲",
        "用方言读一段话",
        "把袜子戴在手上当手套表演 1 分钟",
        "对镜子说『你真好看』5 次",
        "模仿一位偶像 30 秒",
        "学小孩哭 10 秒",
        "原地转 10 圈再走直线",
        "立刻拨打通讯录第 5 个人的电话",
        "翻看手机相册第一张照片给大家看",
        "公屏发一条朋友圈：『我是猪』",
        "给右边的人做 30 秒按摩",
        "学海豚音 5 秒",
        "保持一个微笑 1 分钟不能笑",
        "用筷子夹一颗豆放到杯子里",
        "把椅子当马骑一圈",
        "对全场鞠躬说『大佬好』",
        "现场表演一段广场舞",
        "学猫叫直到下一轮",
        "和左边的人击掌 20 次",
        "蒙眼喝一杯凉水",
        "现场来一段说唱",
        "用脚指头夹起一张纸",
        "学一位老师/同事讲话",
        "倒立 10 秒（量力而行）",
        "讲一个冷笑话直到有人笑",
        "现场拍视频发给暗恋对象"
    )

    fun randomTruth() = TruthOrDareCard(CardType.TRUTH, truthQuestions.random())
    fun randomDare() = TruthOrDareCard(CardType.DARE, dareTasks.random())
    fun random(): TruthOrDareCard =
        if ((0..1).random() == 0) randomTruth() else randomDare()
}

/** 一次投掷记录 */
data class DiceRollRecord(
    val playerId: Long,
    val playerName: String,
    val dice: List<Int>,
    val sum: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/** 骰子游戏完整流程阶段（动画状态机） */
enum class DiceGameStage {
    IDLE,           // 杯子直立空，骰子还没进入
    DROPPING_DICE,  // 骰子从空中飞入杯子
    COVERING,       // 杯子翻转倒扣下来
    COVERED,        // 杯子盖住静止，等待摇一摇
    SHAKING,        // 杯子摇晃中（骰子碰撞声）
    SHAKEN,         // 摇晃停止，等待揭杯
    REVEALING,      // 杯子升起揭杯
    REVEALED        // 已显示点数，等待下一位
}

/** 吹牛骰盅阶段 */
enum class LiarPhase {
    SHAKING,    // 各玩家依次摇骰子（私密查看自己骰子）
    BIDDING,    // 叫数阶段
    SHOWDOWN    // 开盅 / 揭晓
}

/** 吹牛开盅结果 */
data class LiarChallengeOutcome(
    val challengerName: String,
    val bidderName: String,
    val claimedCount: Int,
    val claimedFace: Int,
    val actualCount: Int,
    val bidderTruthful: Boolean,
    val winnerName: String,
    val loserName: String
)

/** 玩家预设头像emoji与配色 */
object DicePlayerPresets {
    val emojis = listOf("🐰", "🐱", "🐻", "🦊", "🐼", "🐨", "🦁", "🐯", "🐸", "🐵", "🦄", "🐶")
    val colors = listOf(
        Color(0xFFFF80AB),
        Color(0xFFEC407A),
        Color(0xFFBA68C8),
        Color(0xFF9575CD),
        Color(0xFF64B5F6),
        Color(0xFF4DB6AC),
        Color(0xFF81C784),
        Color(0xFFFFB74D),
        Color(0xFFFF8A65),
        Color(0xFFE57373)
    )
}
