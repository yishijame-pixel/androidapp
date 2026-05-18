package com.example.funlife.utils

import com.example.funlife.data.model.Bill

data class ReplyRule(
    val category: String,           // 匹配分类，"*" = 全部
    val amountRange: ClosedFloatingPointRange<Double>?, // 金额范围，null = 不限
    val replies: Map<String, List<String>> // personaId → 回复列表
)

class RuleEngine {

    private val rules = listOf(
        ReplyRule("餐饮", null, mapOf(
            "dad" to listOf("又下馆子，自己做不行吗", "少吃外卖，不健康", "花这么多就为吃一顿？", "你妈做的饭不好吃吗", "一天到晚就知道吃"),
            "girlfriend" to listOf("好吃吗～下次带我一起呀♡", "你吃饱了吗～别亏着自己哦", "我也想吃！好馋呀", "记得多喝水哦～", "吃得开心就好啦♡"),
            "roast" to listOf("又吃？你的胃是无底洞吗", "这钱够我吃三天了", "建议你直接住在餐厅", "吃这么贵的？炫富呢", "你的钱包在哭泣"),
            "gentle" to listOf("吃得开心就好呀～", "记得多吃蛇菜呢", "好好吃饭是最重要的事～", "吃好了才有力气呀", "希望你吃得满足呢"),
            "eunuch" to listOf("皇上用膳了！奴才这就记下！", "万岁爷龙体安康，吃好吃好！", "皇上英明，吃饭也不忘记账！", "奴才给您记上啦，皇上慢用！"),
            "buddha" to listOf("食为身之本，不可贪多。", "施主饱腹即可，莫贪口福。", "一粒米皆辛苦，惜福即修行。"),
            "cat" to listOf("喵～有本喵的猫粮好吃吗？", "铲屎官光自己吃，本喵呢？", "吃这么贵？本喵的猫粮都没这价。"),
            "grandma" to listOf("又在外面吃！奶奶做的饭不好吃吗！", "花这么多就为吃一顿？我们那时候...", "吃饱了就行，别浪费粮食啊！"),
        )),
        ReplyRule("交通", null, mapOf(
            "dad" to listOf("能走就走，省点钱", "公交不行吗", "我年轻时候都是骑车的"),
            "girlfriend" to listOf("路上注意安全哦～♡", "到了跟我说一声～", "别太赶啦，安全第一"),
            "roast" to listOf("出门就打车，腿是摆设？", "地铁不配是吗", "有这钱不如买双好鞋走路"),
            "gentle" to listOf("路上小心呀～", "出行方便最重要", "安全到达就好呢"),
            "eunuch" to listOf("皇上出行，奴才已记下银两！", "万岁爷一路顺风！奴才给您记着！", "摆驾费用已记，皇上请安！"),
            "buddha" to listOf("行路也是修行，施主慢行。", "千里之行始于足下。"),
            "cat" to listOf("喵～铲屎官又出门，本喵好无聊。", "打车？本喵走路都不花钱。"),
            "grandma" to listOf("能走就走，省点钱！奶奶那时候都是走路的！", "路上小心啊孙子！"),
        )),
        ReplyRule("购物", null, mapOf(
            "dad" to listOf("又买东西！缺啥跟我说", "能不能省着点花", "这东西有必要买吗"),
            "girlfriend" to listOf("买了什么呀～给我看看！", "有没有给我也买呀♡", "下次一起逛街嘛～"),
            "roast" to listOf("购物车清空了？钱包也清空了", "买买买，等吃土的时候别哭", "剁手党实锤了"),
            "gentle" to listOf("买到喜欢的东西真开心呢", "犬劳自己也是应该的～", "希望是自己喜欢的呀"),
            "eunuch" to listOf("皇上好眼光！这东西奴才记下了！", "万岁爷花钱买的必是好物！", "皇上购置的物件，奴才已登记在册！"),
            "buddha" to listOf("物质皆虚幻，施主莫执。", "买而不执，方为真修行。"),
            "cat" to listOf("喵～有给本喵买小鱼干吗？", "买买买，铲屎官真舘豪。"),
            "grandma" to listOf("又买东西！缺什么告诉奶奶，奶奶给你买！", "这东西有必要买吗？我们那时候..."),
        )),
        ReplyRule("娱乐", null, mapOf(
            "dad" to listOf("玩物丧志啊", "偶尔放松可以，别天天这样", "花这个钱不如存起来"),
            "girlfriend" to listOf("好羡慕！下次带我去呀♡", "玩得开心吗～", "我也想去！呜呜"),
            "roast" to listOf("有钱人的快乐我不懂", "这就是你存不到钱的原因", "快乐是真的，穷也是真的"),
            "gentle" to listOf("适当放松是很重要的呢", "开心就好呀～", "生活不只是工作呢"),
            "eunuch" to listOf("皇上龙心大悦！奴才细细记下！", "万岁爷享乐也是治国之道！", "皇上开心，奴才就安心了！"),
            "buddha" to listOf("娱乐亦是修行，但莫沉迷。", "施主开心就好，但莫忘初心。"),
            "cat" to listOf("喵～铲屎官去玩了？本喵好无聊。", "吃喝玩乐都有你，本喵只有猫粮。"),
            "grandma" to listOf("又去玩！偶尔放松可以，别天天这样！", "花这个钱不如存起来！"),
        )),
        ReplyRule("社交", null, mapOf(
            "dad" to listOf("少应酬，伤身体", "这种聚会能不去就不去", "花钱交朋友，值吗"),
            "girlfriend" to listOf("是跟谁呀～有没有女生？", "早点回来哦♡", "别喝太多啦～"),
            "roast" to listOf("社交达人，钱包瘦人", "又请客？你是冤大头吧", "朋友们一定很爱你的钱包"),
            "gentle" to listOf("和朋友在一起开心就好～", "社交也是生活的一部分呢", "感情是无价的呀"),
            "eunuch" to listOf("皇上宽待群臣，奴才佩服！", "万岁爷恩典百官，花费已记！", "皇上赏赐臣子，英明神武！"),
            "buddha" to listOf("人与人之缘，不可强求。", "社交也是修缘，施主随心。"),
            "cat" to listOf("喵～铲屎官又去社交？本喵不需要社交。", "请客？本喵只请自己。"),
            "grandma" to listOf("少应酬，伤身体！奶奶担心你！", "花钱交朋友，值吗？奶奶觉得不值！"),
        )),
        // 小额通用
        ReplyRule("*", 0.0..15.0, mapOf(
            "dad" to listOf("这点钱还记，行，好习惯", "虽然不多，积少成多啊", "还算节约"),
            "girlfriend" to listOf("小小一笔～记账的你好棒♡", "一点点也要记，好认真呢", "小金额也不放过，真仔细"),
            "roast" to listOf("这也值得记？大款行为", "几块钱也记，你是不是太闲了", "记账记到强迫症了吧"),
            "gentle" to listOf("积少成多，记账是好习惯呢～", "小金额也认真记录，很棒呀", "细心的你真了不起"),
            "eunuch" to listOf("皇上连小银子都记，勤俭治国啊！", "奴才记下了，皇上勤算英明！", "滴水之银，皇上也不放过，奴才佩服！"),
            "buddha" to listOf("滴水之财，亦是修行。", "施主记账严谨，善哉。"),
            "cat" to listOf("喵～这点钱也记？铲屎官你好闲。", "本喵的猫粮都比这贵。"),
            "grandma" to listOf("虽然不多，也要记好！积少成多啊！", "好孩子，省钱是好习惯！"),
        )),
        // 中额通用
        ReplyRule("*", 100.0..500.0, mapOf(
            "dad" to listOf("这笔不小啊，花的什么", "一百多块，想想值不值", "钱要花在刀刃上"),
            "girlfriend" to listOf("这笔有点大呢，是什么呀？", "花了不少呢～值得就好♡", "嗯嗯，记好账心里有数～"),
            "roast" to listOf("大手大脚，月底喝西北风吧", "钱包正在流泪", "这消费水平…有矿？"),
            "gentle" to listOf("这笔不小呢，是必要开支吗？", "花钱也要照顾自己的感受呀", "合理消费就好～"),
            "eunuch" to listOf("皇上这笔开支不小，奴才已记入内务府账册！", "万岁爷出手阔综，奴才佩服！", "皇上大手笔，国库要紧了啊！"),
            "buddha" to listOf("钱财如水，施主莫过于执着。", "中道而行，不偏不倡。"),
            "cat" to listOf("喵！这么多？给本喵买多少猫粮了。", "铲屎官花钱大手大脚，本喵担心。"),
            "grandma" to listOf("这么大一笔！奶奶心疼啊！", "花这么多！我们那时候一个月才这个数！"),
        )),
        // 大额通用
        ReplyRule("*", 500.0..Double.MAX_VALUE, mapOf(
            "dad" to listOf("这么大一笔！什么东西这么贵", "你的钱是大风刮来的吗", "这笔开支有必要吗"),
            "girlfriend" to listOf("这么多！是给我买礼物了吗♡", "哇好大一笔…没事吧？", "是重要的开支吧～辛苦了"),
            "roast" to listOf("好家伙，家里有矿？", "这一笔我一个月工资了", "豪横！但你月底怎么办"),
            "gentle" to listOf("这笔不小呢，是必要开支吗？", "花大钱也别有压力呀～", "重要的开支是值得的呢"),
            "eunuch" to listOf("皇上这笔巨款，奴才心惊肉跳！已记下！", "万岁爷花钱如流水，奴才担心国库啊！", "好大一笔银子！皇上隆恩浩荡！"),
            "buddha" to listOf("施主这笔巨财，是缘是劫？", "万金散尽还复来，施主莫愁。"),
            "cat" to listOf("喵！！！这么多？铲屎官你是土豪吗！", "本喵惊呆了！给本喵买个别墅呀！"),
            "grandma" to listOf("天哪！这么多钱！奶奶一辈子都没花这么多！", "你的钱是大风刮来的吗！奶奶心疼死了！"),
        )),
    )

    fun getReply(bill: Bill, personaId: String): String {
        val absAmount = kotlin.math.abs(bill.amount)
        val matchedRules = rules.filter { rule ->
            (rule.category == "*" || rule.category == bill.category) &&
            (rule.amountRange == null || absAmount in rule.amountRange)
        }

        val replies = matchedRules
            .flatMap { it.replies[personaId] ?: emptyList() }

        return replies.randomOrNull() ?: getDefaultReply(personaId)
    }

    private fun getDefaultReply(personaId: String): String {
        return when (personaId) {
            "dad" -> listOf("嗯，记好了", "知道了", "花钱要有计划啊").random()
            "girlfriend" -> listOf("收到啦～♡", "记好账啦～", "嗯嗯知道了♡").random()
            "roast" -> listOf("行吧，记了", "又花钱了", "你开心就好").random()
            "gentle" -> listOf("好的呢～已记录", "记好啦～辛苦了", "嘣嘣～继续加油").random()
            "eunuch" -> listOf("回皇上，奴才已记下！", "遵旨！已入账！", "奴才给您办妥当了！").random()
            "buddha" -> listOf("阿弥陀佛，已记。", "施主已记，随缘。", "贫僧已记下。").random()
            "cat" -> listOf("喵～本喵勉强记下了。", "记了记了，别烦本喵。", "喵。").random()
            "grandma" -> listOf("奶奶记下了！省着点花！", "知道了知道了，别乱花！", "奶奶都给你记着呢！").random()
            else -> "收到，已记录～"
        }
    }
}
