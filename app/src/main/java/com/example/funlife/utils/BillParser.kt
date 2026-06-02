package com.example.funlife.utils

data class ParsedBill(val amount: Double, val category: String, val note: String)

// 分类关键词表
private val categoryMap = mapOf(
    "餐饮" to listOf("吃", "饭", "餐", "外卖", "奶茶", "咖啡", "麻辣烫", "火锅", "早餐", "午餐", "晚餐", "零食",
        "水果", "饮料", "烧烤", "面", "粥", "小吃", "饮品", "美食", "下馆子", "点外卖", "喝", "吃饭", "菜",
        "蛋糕", "面包", "饺子", "包子", "汉堡", "披萨", "炸鸡", "便当", "甜品", "宵夜", "夜宵", "干果",
        "零嘴", "可乐", "啤酒", "白酒", "红酒", "酒水"),
    "交通" to listOf("打车", "地铁", "公交", "加油", "停车", "滴滴", "高铁", "火车", "机票", "出租车",
        "骑行", "路费", "过路费", "车费", "加气", "充电", "共享单车",
        "车票", "高铁票", "动车", "动车票", "船票", "飞机票", "汽车票", "巴士", "大巴", "网约车",
        "美团打车", "曹操出行", "T3", "首汽", "ETC", "油费", "汽油", "柴油", "电动车", "摩托",
        "电瓶车", "停车费", "洗车"),
    "购物" to listOf("买", "淘宝", "京东", "超市", "衣服", "鞋", "拼多多", "网购", "商场", "包", "化妆品",
        "护肤", "日用品", "电器", "数码", "手机", "电脑",
        "便利店", "罗森", "全家", "711", "7-11", "美妆", "口红", "面膜", "纸巾", "牙膏", "洗发水",
        "沐浴露", "充电器", "耳机", "平板", "键盘", "鼠标", "家具", "床单", "被子"),
    "娱乐" to listOf("电影", "游戏", "KTV", "旅游", "门票", "演出", "酒吧", "唱歌", "充值", "会员", "视频",
        "演唱会", "话剧", "脱口秀", "密室", "剧本杀", "桌游", "网吧", "台球", "保龄球", "蹦床",
        "游乐园", "迪士尼", "环球影城", "皮肤", "氪金", "手游", "Steam", "PSN", "switch"),
    "居住" to listOf("房租", "水电", "物业", "燃气", "宽带", "维修", "房贷", "电费", "水费",
        "网费", "煤气", "天然气", "管理费", "暖气费", "中介费", "押金", "搬家"),
    "医疗" to listOf("看病", "药", "医院", "体检", "牙", "挂号", "门诊", "诊所", "拔牙", "补牙",
        "口罩", "感冒药", "退烧药", "眼镜", "隐形眼镜"),
    "学习" to listOf("书", "课程", "培训", "考试", "学费", "补习", "补课",
        "教材", "练习册", "网课", "辅导班", "驾照", "驾考", "雅思", "托福"),
    "社交" to listOf("红包", "请客", "礼物", "份子钱", "聚餐", "随份子",
        "随礼", "婚礼", "满月", "升学宴", "乔迁", "份子"),
    "服饰" to listOf("衣服", "鞋子", "裤子", "裙子", "外套", "内衣",
        "T恤", "卫衣", "羽绒服", "棉袄", "袜子", "帽子", "围巾", "手套", "皮带", "腰带"),
    "美容" to listOf("理发", "剪发", "烫发", "染发", "美甲", "美睫", "纹眉", "纹身", "spa", "按摩", "spa", "洗浴"),
    "宠物" to listOf("猫粮", "狗粮", "宠物", "猫砂", "罐头", "鱼食", "鸟食", "宠物医院"),
    "通讯" to listOf("话费", "流量", "宽带", "电话费", "充话费", "套餐"),
)

// 收入关键词
private val incomeKeywords = listOf("工资", "收入", "转入", "红包收", "退款", "奖金", "补贴", "报销", "到账")

// 金额指示词：有这些词明确表示是金钱
private val moneyIndicators = listOf("元", "块", "钱", "¥", "￥", "花了", "花费", "消费", "费用", "支出", "账", "付款", "付了", "买了", "给了")

// 非账单模式：匹配到这些表示不是记账
private val nonBillPatterns = listOf(
    Regex("""\d{4}年"""),                         // 2026年
    Regex("""\d{1,2}月"""),                        // 3月
    Regex("""\d{1,2}号"""),                        // 5号
    Regex("""\d{1,2}日"""),                        // 15日
    Regex("""\d{1,2}点"""),                        // 3点
    Regex("""\d{1,2}时"""),                        // 3时
    Regex("""\d{1,2}分钟?"""),                    // 30分钟
    Regex("""\d{1,2}秒"""),                        // 10秒
    Regex("""\d{1,2}岁"""),                        // 25岁
    Regex("""\d+个"""),                           // 3个
    Regex("""\d+次"""),                           // 5次
    Regex("""\d+天"""),                           // 7天
    Regex("""\d+周"""),                           // 2周
    Regex("""\d+层"""),                           // 3层
    Regex("""\d+楼"""),                           // 5楼
    Regex("""\d+人"""),                           // 3人
    Regex("""\d+只"""),                           // 2只
    Regex("""\d+张"""),                           // 2张
    Regex("""\d+米"""),                           // 100米
    Regex("""\d+公里"""),                        // 5公里
    Regex("""\d+小时"""),                        // 2小时
    Regex("""\d{3,}尾号"""),                      // 电话尾号
    Regex("""1[3-9]\d{9}"""),                      // 手机号
    Regex("""\d{5,}"""),                           // 5位以上纯数字（可能是ID/编号）
    Regex("""第\d+"""),                           // 第3
    Regex("""\d+度"""),                           // 36度
    Regex("""\d+%"""),                             // 50%
)

// 纯聊天/问候模式
private val chatPatterns = listOf(
    Regex("""^你好.{0,5}$"""), Regex("""^嘿.{0,5}$"""), Regex("""^在吗.{0,3}$"""),
    Regex("""^干嘛.{0,3}$"""), Regex("""^吃了吗.{0,3}$"""), Regex("""^晚安.{0,3}$"""),
    Regex("""^早安.{0,3}$"""), Regex("""^谢谢.{0,3}$"""), Regex("""^好的.{0,3}$"""),
    Regex("""^嗯.{0,3}$"""), Regex("""^哈哈.{0,5}$"""), Regex("""^呵呵.{0,5}$"""),
    Regex("""^.{0,2}是谁.{0,3}$"""), Regex("""^.{0,3}干什么.{0,3}$"""),
    Regex("""^.{0,3}在干嘛.{0,3}$"""), Regex("""^.{0,3}我爱你.{0,3}$"""),
    Regex("""^.{0,3}想你.{0,3}$"""), Regex("""^.{0,3}无聊.{0,3}$"""),
    Regex("""^.{0,3}好无聊.{0,3}$"""), Regex("""^.{0,3}我好.{0,6}$"""),
    Regex("""^[\u4e00-\u9fff]{1,4}$"""),           // 1-4个纯汉字（如"你好"“早安"）
)

fun parseBillInput(input: String): ParsedBill? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null

    // 1. 先检查是否是纯聊天模式
    if (chatPatterns.any { it.matches(trimmed) }) return null

    // 2. 检查非账单模式（年份、日期、时间、数量词等）
    // 如果输入中的所有数字都被非账单模式匹配，则不是记账
    val allNumbers = Regex("""\d+\.?\d*""").findAll(trimmed).toList()
    if (allNumbers.isEmpty()) return null

    val nonBillMatched = nonBillPatterns.flatMap { it.findAll(trimmed).toList() }
    val allNumbersCoveredByNonBill = allNumbers.all { numMatch ->
        nonBillMatched.any { nbm ->
            numMatch.range.first >= nbm.range.first && numMatch.range.last <= nbm.range.last
        }
    }
    if (allNumbersCoveredByNonBill && nonBillMatched.isNotEmpty()) return null

    // 3. 判断记账意图
    val hasMoneyIndicator = moneyIndicators.any { trimmed.contains(it) }
    val hasCategoryKeyword = categoryMap.values.flatten().any { trimmed.contains(it) }
    val hasIncomeKeyword = incomeKeywords.any { trimmed.contains(it) }

    // E) 兜底规则：纯"短词+数字"格式（2-5汉字+数字，整串无其他字符），视为账单
    //    例如 "车票150"、"蜂蜜80"、"理发38" 等关键词未命中但意图明显的输入
    val shortWordAmountPattern = Regex("""^[\u4e00-\u9fffA-Za-z]{2,6}\s*\d+\.?\d*$""")
    val matchesShortWordAmount = shortWordAmountPattern.matches(trimmed)

    // 必须满足以下任一条件才认为是记账：
    // A) 有金钱指示词（元/块/¥）+ 数字
    // B) 有消费分类关键词 + 数字
    // C) 有收入关键词 + 数字
    // D) 格式如 "午饭35" "咖啡30" "打车15" 等（关键词+数字紧邻）
    // E) 短词+数字 紧凑格式（兜底）
    if (!hasMoneyIndicator && !hasCategoryKeyword && !hasIncomeKeyword && !matchesShortWordAmount) {
        return null // 纯数字或无关文字+数字，不记账
    }

    // 4. 提取金额
    // 优先匹配带金钱符号的：¥35 / 35元 / 35块
    val moneyRegex = Regex("""[\u00a5￥]\s*(\d+\.?\d*)|([\d]+\.?\d*)\s*[元块]""") 
    val moneyMatch = moneyRegex.find(trimmed)
    val amount: Double

    if (moneyMatch != null) {
        amount = (moneyMatch.groupValues[1].takeIf { it.isNotEmpty() }
            ?: moneyMatch.groupValues[2]).toDoubleOrNull() ?: return null
    } else {
        // 没有金钱符号，但有分类关键词，取第一个未被非账单模式覆盖的数字
        val freeNumber = allNumbers.firstOrNull { numMatch ->
            !nonBillMatched.any { nbm ->
                numMatch.range.first >= nbm.range.first && numMatch.range.last <= nbm.range.last
            }
        } ?: return null
        amount = freeNumber.value.toDoubleOrNull() ?: return null
    }

    if (amount <= 0 || amount > 999999) return null // 金额不合理

    // 5. 确定分类
    val category = categoryMap.entries
        .firstOrNull { (_, keywords) -> keywords.any { trimmed.contains(it) } }
        ?.key ?: "其他"

    // 6. 生成备注
    val note = trimmed
        .replace(Regex("""[\u00a5￥]"""), "")
        .replace(Regex("""\d+\.?\d*"""), "")
        .replace("元", "").replace("块", "").replace("钱", "")
        .replace("花了", "").replace("花费", "").replace("消费", "")
        .trim()

    // 7. 检查是否为收入
    val isIncome = hasIncomeKeyword

    return ParsedBill(
        amount = if (isIncome) amount else -amount,
        category = if (isIncome) "收入" else category,
        note = note.ifEmpty { trimmed }
    )
}
