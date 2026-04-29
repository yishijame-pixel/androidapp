package com.example.funlife.repository

import android.util.Log
import com.example.funlife.data.dao.RiddleDao
import com.example.funlife.data.dao.RiddleProgressDao
import com.example.funlife.data.dao.RiddleStatsDao
import com.example.funlife.data.model.Riddle
import com.example.funlife.data.model.RiddleProgress
import com.example.funlife.data.model.RiddleStats
import kotlinx.coroutines.flow.Flow

private const val TAG = "RiddleRepository"

class RiddleRepository(
    private val riddleDao: RiddleDao,
    private val progressDao: RiddleProgressDao,
    private val statsDao: RiddleStatsDao
) {
    fun getAllRiddles(): Flow<List<Riddle>> {
        Log.d(TAG, "getAllRiddles() called")
        return riddleDao.getAllRiddles()
    }
    
    suspend fun getRiddleById(riddleId: Long): Riddle? = riddleDao.getRiddleById(riddleId)
    
    suspend fun insertRiddles(riddles: List<Riddle>) {
        Log.d(TAG, "insertRiddles: inserting ${riddles.size} riddles")
        riddleDao.insertRiddles(riddles)
        Log.d(TAG, "insertRiddles: complete")
    }
    
    suspend fun getRiddleCount(): Int {
        val count = riddleDao.getRiddleCount()
        Log.d(TAG, "getRiddleCount: $count")
        return count
    }
    
    suspend fun getProgress(userId: Long, riddleId: Long): RiddleProgress? =
        progressDao.getProgress(userId, riddleId)
    
    fun getUserProgress(userId: Long): Flow<List<RiddleProgress>> =
        progressDao.getUserProgress(userId)
    
    suspend fun updateProgress(progress: RiddleProgress) =
        progressDao.insertProgress(progress)
    
    fun getStats(userId: Long): Flow<RiddleStats?> = statsDao.getStats(userId)
    
    suspend fun getStatsSync(userId: Long): RiddleStats? = statsDao.getStatsSync(userId)
    
    suspend fun updateStats(stats: RiddleStats) = statsDao.insertStats(stats)
    
    suspend fun initializeRiddles() {
        val count = getRiddleCount()
        Log.d(TAG, "initializeRiddles: current count = $count")
        
        if (count == 0) {
            Log.d(TAG, "initializeRiddles: parsing riddles from text")
            val riddles = parseRiddlesFromText()
            Log.d(TAG, "initializeRiddles: parsed ${riddles.size} riddles")
            insertRiddles(riddles)
            Log.d(TAG, "initializeRiddles: insertion complete")
        } else {
            Log.d(TAG, "initializeRiddles: riddles already exist, skipping")
        }
    }
    
    private fun parseRiddlesFromText(): List<Riddle> {
        return listOf(
            // 草地系列
            Riddle(question = "前面有一块只有草的草地，猜一花名", answer = "梅花", category = "谐音梗", difficulty = 2),
            Riddle(question = "前面又有一块只有草的草地，猜一花名", answer = "野梅花", category = "谐音梗", difficulty = 2),
            Riddle(question = "这个时候，来了一群羊，猜一水果", answer = "草莓", category = "谐音梗", difficulty = 2),
            Riddle(question = "羊在吃草的时候，来了一只狼，猜一水果", answer = "杨梅", category = "谐音梗", difficulty = 2),
            Riddle(question = "狼在吃羊的时候，又来了一只狼，但却没吃羊，猜一动物", answer = "虾", category = "谐音梗", difficulty = 3),
            Riddle(question = "这个时候，又来了两只狼，还是没吃羊，猜一动物", answer = "对虾", category = "谐音梗", difficulty = 3),
            Riddle(question = "又来了一只狼，还是没吃羊，猜一动物", answer = "海虾", category = "谐音梗", difficulty = 3),
            Riddle(question = "最后，又来了一只狼，不但没吃羊，还对周围的声音没反应，猜一动物", answer = "龙虾", category = "谐音梗", difficulty = 3),
            Riddle(question = "一只狐狸跑去草地，跟狼争抢羊吃，为什么？", answer = "狐狸糊涂", category = "谐音梗", difficulty = 3),
            
            // 冰箱系列
            Riddle(question = "把长颈鹿放进冰箱里，需要几步完成？", answer = "3步", category = "脑筋急转弯", difficulty = 2),
            Riddle(question = "把大象放进冰箱里，需要几步完成？", answer = "4步", category = "脑筋急转弯", difficulty = 2),
            Riddle(question = "森林召开全体动物联欢会，请问什么动物没参加？", answer = "大象", category = "脑筋急转弯", difficulty = 2),
            Riddle(question = "飞机上有10块大石头，经过森林时掉下来一块，还剩多少块？", answer = "9块", category = "脑筋急转弯", difficulty = 1),
            Riddle(question = "一个很坏的猎人要去森林打猎，在森林里趟过一条有鳄鱼的河，居然没事，为什么？", answer = "鳄鱼都去参加动物联欢会了", category = "脑筋急转弯", difficulty = 3),
            Riddle(question = "动物联欢会并没有因为猎人而停止，为什么？", answer = "猎人被天上掉下的石头砸了", category = "脑筋急转弯", difficulty = 3),
            
            // 飞行系列
            Riddle(question = "胡萝卜为什么会飞？", answer = "神奇的会飞的胡萝卜", category = "脑筋急转弯", difficulty = 2),
            Riddle(question = "兔子为什么会飞？", answer = "吃了会飞的胡萝卜", category = "脑筋急转弯", difficulty = 2),
            Riddle(question = "老鹰为什么会飞？", answer = "老鹰本来就会飞", category = "脑筋急转弯", difficulty = 1),
            Riddle(question = "牛为什么会飞？", answer = "有人在吹牛", category = "谐音梗", difficulty = 2),
            Riddle(question = "为什么飞在空中的牛掉了下来？", answer = "吹牛的人要停下来换口气", category = "脑筋急转弯", difficulty = 3),
            Riddle(question = "飞在空中的牛经过一片森林时，为何不飞了？", answer = "要下来参加动物联欢会", category = "脑筋急转弯", difficulty = 3),
            Riddle(question = "动物联欢会上点名，发现大象不在，于是就派了兔子去接大象，为什么兔子经过一条大河时身上没湿？", answer = "会飞的兔子", category = "脑筋急转弯", difficulty = 3),
            Riddle(question = "会飞的兔子将大象从冰箱里放出来后，在去往森林的路上被森林警察给拦了下来，为什么？", answer = "大象超重了", category = "脑筋急转弯", difficulty = 3),
            Riddle(question = "大象和兔子不服，同森林警察激烈争执了起来，大象因为情绪过重突发疾病去世了。森林法庭介入调查罪魁祸首，问最后谁被判了？", answer = "冰箱", category = "脑筋急转弯", difficulty = 4),
            Riddle(question = "森林联欢会结束，请问谁获得了本届联欢会的突出贡献？", answer = "石头", category = "脑筋急转弯", difficulty = 3),
            
            // 食物系列
            Riddle(question = "老鼠对马说：明天我要去和猫聚餐，猜一食物", answer = "薯片", category = "谐音梗", difficulty = 2),
            Riddle(question = "马不信，觉得老鼠是在忽悠，于是把老鼠拎了起来，打了一顿，猜一蔬菜", answer = "马铃薯", category = "谐音梗", difficulty = 2),
            Riddle(question = "马的耳朵受伤很严重，大夫让它去另一个地方，说那里肯定能治好，请问是哪里？", answer = "成功", category = "成语", difficulty = 2),
            Riddle(question = "马就出发去成功，路上经过一片大海，遇到了一只鲨鱼也要去成功，马很热情的驮着鲨鱼一道前往，猜一食物", answer = "沙琪玛", category = "谐音梗", difficulty = 3),
            
            // 土豆系列
            Riddle(question = "土豆找包子打架，却被一条河给拦住过不去，猜一蔬菜", answer = "荷兰豆", category = "谐音梗", difficulty = 2),
            Riddle(question = "土豆泄气了，这时一只猪经过，不停地鼓励土豆，猜一零食", answer = "朱古力", category = "谐音梗", difficulty = 2),
            Riddle(question = "在猪的不断鼓励下，土豆终于过了河，找到包子打架，土豆给了包子致命一击，打一食物", answer = "豆沙包", category = "谐音梗", difficulty = 3),
            Riddle(question = "包子的好兄弟米饭要给包子报仇，带着一众米饭找土豆，土豆的朋友粽子、青椒等过来帮忙，可米饭人多，很快土豆等都被打的很惨，唯独粽子没事，为什么？", answer = "粽子是卧底", category = "脑筋急转弯", difficulty = 4),
            Riddle(question = "土豆受伤，去医院住院时被严加看管，为什么？", answer = "怕他偷", category = "谐音梗", difficulty = 3),
            Riddle(question = "土豆伤好后，被好朋友请去高档澡堂，洗完澡蒸桑拿时土豆睡着了，猜一菜品", answer = "土豆泥", category = "谐音梗", difficulty = 2),
            
            // 小鸡系列
            Riddle(question = "小鸡自打出生后，一直就没见过爸爸，于是就问鸡妈妈：我的爸爸是谁？鸡妈妈告诉小鸡：等你考了第一就告诉你。于是小鸡非常努力学习，终于考了第一。这时，鸡妈妈告诉了小鸡，问：小鸡的爸爸叫什么？", answer = "瓦特", category = "谐音梗", difficulty = 4),
            Riddle(question = "小鸡不能接受自己的爸爸是人类，自此变得颓废学习不上心，成绩一落千丈，这时班主任跟小鸡说，你的爸爸不是瓦特，为什么？", answer = "莱特兄弟", category = "谐音梗", difficulty = 4),
            Riddle(question = "小鸡接受不了，疯癫了，这时她的爸爸又变了，请问是谁？", answer = "法拉第", category = "谐音梗", difficulty = 4),
            
            // 经典脑筋急转弯
            Riddle(question = "金木水火土，谁的腿最长？", answer = "火", category = "脑筋急转弯", difficulty = 1),
            Riddle(question = "玻璃杯从楼上掉下来，会对你说什么？", answer = "晚安", category = "谐音梗", difficulty = 2),
            Riddle(question = "你知道一颗星星有多重吗？", answer = "八克", category = "谐音梗", difficulty = 2),
            Riddle(question = "愚公移山，打一首歌", answer = "一闪一闪亮晶晶", category = "谐音梗", difficulty = 2),
            Riddle(question = "A和C谁比较高？", answer = "C", category = "脑筋急转弯", difficulty = 1),
            Riddle(question = "小白长得很像他哥哥，打一成语", answer = "真相大白", category = "成语", difficulty = 2),
            Riddle(question = "狗过了独木桥之后就不叫了，为什么？", answer = "过目不汪", category = "谐音梗", difficulty = 2),
            Riddle(question = "什么动物最没安全感？", answer = "麋鹿", category = "谐音梗", difficulty = 2),
            Riddle(question = "有两个人掉到陷阱里了，死的人叫死人，活人叫什么？", answer = "叫救命", category = "脑筋急转弯", difficulty = 1),
            Riddle(question = "十五的月亮多少钱？", answer = "十六元", category = "谐音梗", difficulty = 2),
            Riddle(question = "被人放鸽子，最高兴的是谁？", answer = "鸽子", category = "脑筋急转弯", difficulty = 1),
            Riddle(question = "为什么想考一本？", answer = "一本正经", category = "谐音梗", difficulty = 2),
            Riddle(question = "什么鱼不能吃？", answer = "木鱼", category = "脑筋急转弯", difficulty = 1),
            Riddle(question = "为什么飞机飞那么高不会撞到星星呢？", answer = "星星会闪", category = "脑筋急转弯", difficulty = 2),
            
            // 更多脑筋急转弯
            Riddle(question = "从前有个人姓铁，他总是不长头发，他得了什么病？", answer = "老铁没毛病", category = "谐音梗", difficulty = 2),
            Riddle(question = "地铁上有三只羊，中途来一只狼，那么还有几只羊呢？", answer = "三只羊", category = "脑筋急转弯", difficulty = 2),
            Riddle(question = "小王剪了中分会变成什么？", answer = "小全", category = "脑筋急转弯", difficulty = 2),
            Riddle(question = "理发师出门要带什么？", answer = "托尼带水", category = "谐音梗", difficulty = 3),
            Riddle(question = "26个字母去掉字母ET，剩下几个字母？", answer = "21个", category = "脑筋急转弯", difficulty = 2),
            Riddle(question = "花为什么搞笑？", answer = "花有梗", category = "脑筋急转弯", difficulty = 2),
            Riddle(question = "风的孩子叫什么？", answer = "水起", category = "成语", difficulty = 2),
            Riddle(question = "冰山能值多少钱？", answer = "一角钱", category = "谐音梗", difficulty = 2),
            Riddle(question = "汽车会飞，打一种饮料", answer = "咖啡", category = "谐音梗", difficulty = 2),
            Riddle(question = "七分熟的牛排和八分熟的牛排相遇为什么不打招呼？", answer = "不熟", category = "脑筋急转弯", difficulty = 2),
            Riddle(question = "气球的里面有空气，那么救生圈里面有什么呢？", answer = "不会游泳的人", category = "脑筋急转弯", difficulty = 2),
            Riddle(question = "金钟奖、金马奖、金像奖，谁对国家贡献最大？", answer = "金钟奖", category = "谐音梗", difficulty = 3),
            
            // 更多谜题
            Riddle(question = "一个小白加一个小白，等于什么？", answer = "小白兔", category = "谐音梗", difficulty = 2),
            Riddle(question = "一个胖子从12楼掉下来会变成什么？", answer = "死胖子", category = "脑筋急转弯", difficulty = 1),
            Riddle(question = "一个小孩和一个大人在漆黑的夜里走路，小孩是大人的儿子，大人却不是小孩的父亲，请问为什么？", answer = "母子", category = "脑筋急转弯", difficulty = 2),
            Riddle(question = "大象的左耳像什么？", answer = "右耳", category = "脑筋急转弯", difficulty = 1),
            Riddle(question = "有一只鲨鱼吃下了一颗绿豆，结果它变成了什么？", answer = "绿豆沙", category = "谐音梗", difficulty = 2),
            Riddle(question = "天的儿子叫什么？", answer = "我材", category = "成语", difficulty = 3),
            Riddle(question = "天的女儿叫什么？", answer = "丽质", category = "成语", difficulty = 3),
            Riddle(question = "狼、老虎和狮子谁玩游戏一定会被淘汰？", answer = "狼", category = "谐音梗", difficulty = 2),
            Riddle(question = "下雨天没多少钱不要出门？", answer = "30000000", category = "谐音梗", difficulty = 3),
            Riddle(question = "什么牌子化妆品容易感冒？", answer = "雅倩", category = "谐音梗", difficulty = 3),
            Riddle(question = "什么鱼最白痴？", answer = "鲨鱼", category = "谐音梗", difficulty = 1),
            Riddle(question = "什么鱼最聪明？", answer = "鲸鱼", category = "谐音梗", difficulty = 1),
            Riddle(question = "有一棵三角形的树被送到北极去种，请问长大后，那棵树叫什么？", answer = "三角函数", category = "谐音梗", difficulty = 3),
            
            // 更多经典题目
            Riddle(question = "狮子和熊比赛拉粑粑，谁赢了？", answer = "狮子", category = "谐音梗", difficulty = 2),
            Riddle(question = "超人为什么要穿紧身衣？", answer = "救人要紧", category = "谐音梗", difficulty = 2),
            Riddle(question = "公共厕所（猜一外国首都）", answer = "伦敦", category = "谐音梗", difficulty = 3),
            Riddle(question = "哪一种竹子不长在土里？", answer = "爆竹", category = "脑筋急转弯", difficulty = 1),
            Riddle(question = "什么书中毛病最多？", answer = "医书", category = "脑筋急转弯", difficulty = 2),
            Riddle(question = "大家都不想得到的是什么？", answer = "得病", category = "脑筋急转弯", difficulty = 1),
            Riddle(question = "世界上什么地方的海不产鱼？", answer = "辞海", category = "脑筋急转弯", difficulty = 2),
            Riddle(question = "你在学校学到的知识越多，什么就会少？", answer = "不知道的东西", category = "脑筋急转弯", difficulty = 2),
            Riddle(question = "什么酒不能喝？", answer = "碘酒", category = "脑筋急转弯", difficulty = 1),
            Riddle(question = "一个自讨苦吃的地方在哪里？", answer = "药店", category = "脑筋急转弯", difficulty = 2),
            Riddle(question = "全世界最大的公鸡是从哪里来的？", answer = "蛋里", category = "脑筋急转弯", difficulty = 1),
            Riddle(question = "两对父子去买帽子，为什么只买了三顶？", answer = "爷爷爸爸和儿子", category = "脑筋急转弯", difficulty = 2),
            Riddle(question = "男人在一起喝酒，为什么非划拳不可？", answer = "敬酒不吃吃罚酒", category = "脑筋急转弯", difficulty = 2),
            
            // 最后一批
            Riddle(question = "什么工作令人惊讶？", answer = "挖藕", category = "谐音梗", difficulty = 2),
            Riddle(question = "猴子最厌恶什么线？", answer = "平行线", category = "谐音梗", difficulty = 2),
            Riddle(question = "哪位历史人物最欠扁？", answer = "苏武", category = "谐音梗", difficulty = 3),
            Riddle(question = "今天下午到旺角看电影，到了旺角，半个人也看不见，为什么？", answer = "人是没有半个的", category = "脑筋急转弯", difficulty = 2),
            Riddle(question = "为什么蚕宝宝很有钱？", answer = "会结茧", category = "谐音梗", difficulty = 2),
            Riddle(question = "为什么小白兔不嫁给斑马呢？", answer = "兔妈妈说纹身不是好孩子", category = "脑筋急转弯", difficulty = 2),
            Riddle(question = "如果你想美梦成真首先要做什么？", answer = "醒来", category = "脑筋急转弯", difficulty = 1),
            Riddle(question = "四个人打麻将，被人举报了，警察来抓人，为什么带走了五个？", answer = "四个人在打一个叫麻将的人", category = "脑筋急转弯", difficulty = 3),
            Riddle(question = "松下为什么没索尼强？", answer = "怕了索尼哥", category = "谐音梗", difficulty = 3),
            Riddle(question = "世界上什么人一下子变老？", answer = "新娘", category = "脑筋急转弯", difficulty = 2),
            Riddle(question = "一颗心价值多少钱？", answer = "1亿", category = "谐音梗", difficulty = 2),
            Riddle(question = "要考试了，不能看什么书？", answer = "百科全书", category = "谐音梗", difficulty = 2),
            Riddle(question = "铅笔姓什么？", answer = "萧", category = "谐音梗", difficulty = 2)
        )
    }
}
