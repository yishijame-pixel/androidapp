package com.example.funlife.viewmodel

import androidx.lifecycle.ViewModel
import com.example.funlife.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class DiceGameViewModel : ViewModel() {

    // 骰子数量 1~10
    private val _diceCount = MutableStateFlow(2)
    val diceCount: StateFlow<Int> = _diceCount.asStateFlow()

    // 当前骰子点数
    private val _diceValues = MutableStateFlow(List(2) { 1 })
    val diceValues: StateFlow<List<Int>> = _diceValues.asStateFlow()

    // 杯子是否扣下（true 表示盖住，看不见点数）— 初始 IDLE 为 false
    private val _isCupCovered = MutableStateFlow(false)
    val isCupCovered: StateFlow<Boolean> = _isCupCovered.asStateFlow()

    // 是否正在摇晃
    private val _isShaking = MutableStateFlow(false)
    val isShaking: StateFlow<Boolean> = _isShaking.asStateFlow()

    // 是否已揭晓
    private val _isRevealed = MutableStateFlow(false)
    val isRevealed: StateFlow<Boolean> = _isRevealed.asStateFlow()
    
    // 完整流程状态机
    private val _gameStage = MutableStateFlow(DiceGameStage.IDLE)
    val gameStage: StateFlow<DiceGameStage> = _gameStage.asStateFlow()
    
    fun setStage(stage: DiceGameStage) {
        _gameStage.value = stage
        // 同步 legacy 状态
        when (stage) {
            DiceGameStage.IDLE -> {
                _isCupCovered.value = false
                _isShaking.value = false
                _isRevealed.value = false
            }
            DiceGameStage.DROPPING_DICE -> {
                _isCupCovered.value = false
                _isShaking.value = false
                _isRevealed.value = false
            }
            DiceGameStage.COVERING -> {
                _isCupCovered.value = true
                _isShaking.value = false
                _isRevealed.value = false
            }
            DiceGameStage.COVERED -> {
                _isCupCovered.value = true
                _isShaking.value = false
                _isRevealed.value = false
            }
            DiceGameStage.SHAKING -> {
                _isCupCovered.value = true
                _isShaking.value = true
                _isRevealed.value = false
            }
            DiceGameStage.SHAKEN -> {
                _isCupCovered.value = true
                _isShaking.value = false
                _isRevealed.value = false
            }
            DiceGameStage.REVEALING -> {
                _isCupCovered.value = false
                _isShaking.value = false
                _isRevealed.value = false
            }
            DiceGameStage.REVEALED -> {
                _isCupCovered.value = false
                _isShaking.value = false
                _isRevealed.value = true
            }
        }
    }

    // 玩家列表
    private val _players = MutableStateFlow(
        listOf(
            DicePlayer(1L, "玩家1", DicePlayerPresets.colors[0], DicePlayerPresets.emojis[0]),
            DicePlayer(2L, "玩家2", DicePlayerPresets.colors[2], DicePlayerPresets.emojis[1])
        )
    )
    val players: StateFlow<List<DicePlayer>> = _players.asStateFlow()

    // 当前玩家索引
    private val _currentPlayerIndex = MutableStateFlow(0)
    val currentPlayerIndex: StateFlow<Int> = _currentPlayerIndex.asStateFlow()

    // 游戏模式
    private val _gameMode = MutableStateFlow(DiceGameMode.default())
    val gameMode: StateFlow<DiceGameMode> = _gameMode.asStateFlow()

    // 数字定罚 - 目标数字
    private val _penaltyNumber = MutableStateFlow(6)
    val penaltyNumber: StateFlow<Int> = _penaltyNumber.asStateFlow()

    // 投掷历史
    private val _rollHistory = MutableStateFlow<List<DiceRollRecord>>(emptyList())
    val rollHistory: StateFlow<List<DiceRollRecord>> = _rollHistory.asStateFlow()

    // 真心话/大冒险卡片
    private val _truthDareCard = MutableStateFlow<TruthOrDareCard?>(null)
    val truthDareCard: StateFlow<TruthOrDareCard?> = _truthDareCard.asStateFlow()

    // 一轮结果（所有玩家都投完后展示）
    private val _showRoundResult = MutableStateFlow(false)
    val showRoundResult: StateFlow<Boolean> = _showRoundResult.asStateFlow()

    // ─── 操作 ───
    fun setDiceCount(count: Int) {
        val n = count.coerceIn(1, 10)
        _diceCount.value = n
        _diceValues.value = List(n) { 1 }
        setStage(DiceGameStage.IDLE)
    }

    fun setGameMode(mode: DiceGameMode) {
        _gameMode.value = mode
        resetGame()
    }

    fun setPenaltyNumber(n: Int) {
        _penaltyNumber.value = n.coerceIn(1, 6)
    }

    fun addPlayer(name: String) {
        val list = _players.value.toMutableList()
        val nextId = (list.maxOfOrNull { it.id } ?: 0L) + 1L
        val idx = list.size
        list.add(
            DicePlayer(
                id = nextId,
                name = name.ifBlank { "玩家${idx + 1}" },
                color = DicePlayerPresets.colors[idx % DicePlayerPresets.colors.size],
                emoji = DicePlayerPresets.emojis[idx % DicePlayerPresets.emojis.size]
            )
        )
        _players.value = list
    }

    fun removePlayer(playerId: Long) {
        val list = _players.value.filter { it.id != playerId }
        if (list.isEmpty()) return  // 至少保留 1 个
        _players.value = list
        if (_currentPlayerIndex.value >= list.size) _currentPlayerIndex.value = 0
    }

    fun startShake() {
        if (_isShaking.value) return
        _isShaking.value = true
        _isCupCovered.value = true
        _isRevealed.value = false
        // 摇晃期间不断刷新骰子点数（用于视觉效果）
    }

    fun stopShakeAndRoll() {
        if (!_isShaking.value) return
        _isShaking.value = false
        // 生成最终点数
        val n = _diceCount.value
        _diceValues.value = List(n) { Random.nextInt(1, 7) }
    }

    /** 摇动期间用于产生抖动数字的辅助 */
    fun jitterDiceValues() {
        val n = _diceCount.value
        _diceValues.value = List(n) { Random.nextInt(1, 7) }
    }

    fun revealCup() {
        _isCupCovered.value = false
        _isRevealed.value = true
        // 记录这次投掷
        val player = _players.value.getOrNull(_currentPlayerIndex.value) ?: return
        val sum = _diceValues.value.sum()
        val updated = _players.value.map {
            if (it.id == player.id) it.copy(
                lastRoll = _diceValues.value,
                lastSum = sum,
                totalScore = it.totalScore + sum
            ) else it
        }
        _players.value = updated
        _rollHistory.value = _rollHistory.value + DiceRollRecord(
            playerId = player.id,
            playerName = player.name,
            dice = _diceValues.value,
            sum = sum
        )
    }

    fun nextPlayer() {
        val total = _players.value.size
        if (total == 0) return
        val nextIdx = (_currentPlayerIndex.value + 1) % total
        _currentPlayerIndex.value = nextIdx
        setStage(DiceGameStage.IDLE)
        _truthDareCard.value = null
        // 如果回到第 0 个玩家，说明一轮结束 → 展示结算
        if (nextIdx == 0 && total > 1) {
            // 吹牛骰盅：转到 BIDDING 阶段，不弹结算
            if (_gameMode.value == DiceGameMode.LIAR_DICE) {
                liarStartBidding()
                return
            }
            // 真心话/大冒险模式：自动给最小者抽卡
            if (_gameMode.value == DiceGameMode.TRUTH_DARE) {
                _truthDareCard.value = TruthOrDareDatabase.random()
            }
            _showRoundResult.value = true
        }
    }
    
    /** 21点是否爆牌：当前玩家累计点数 > 21 */
    fun isCurrentPlayerBust(): Boolean {
        if (_gameMode.value != DiceGameMode.BLACKJACK_21) return false
        val p = _players.value.getOrNull(_currentPlayerIndex.value) ?: return false
        return p.totalScore > 21
    }

    fun dismissRoundResult() {
        _showRoundResult.value = false
        // 给胜者+1 win
        val winner = computeRoundWinner()
        if (winner != null) {
            _players.value = _players.value.map {
                if (it.id == winner.id) it.copy(wins = it.wins + 1) else it
            }
        }
        // 清空本轮 lastRoll
        _players.value = _players.value.map { it.copy(lastRoll = emptyList(), lastSum = 0) }
    }

    fun computeRoundWinner(): DicePlayer? {
        val playersWithRolls = _players.value.filter { it.lastRoll.isNotEmpty() }
        if (playersWithRolls.isEmpty()) return null
        return when (_gameMode.value) {
            DiceGameMode.COMPARE_SIZE,
            DiceGameMode.DRINKING_SMALL_DRINKS -> playersWithRolls.maxByOrNull { it.lastSum }
            DiceGameMode.TRUTH_DARE,
            DiceGameMode.DRINKING_BIG_DRINKS -> playersWithRolls.minByOrNull { it.lastSum }
            DiceGameMode.BLACKJACK_21 -> {
                // 最接近 21 不超
                playersWithRolls
                    .filter { it.totalScore <= 21 }
                    .maxByOrNull { it.totalScore }
                    ?: playersWithRolls.minByOrNull { it.totalScore }
            }
            DiceGameMode.NUMBER_PENALTY -> playersWithRolls.firstOrNull { !it.lastRoll.contains(_penaltyNumber.value) }
            DiceGameMode.LIAR_DICE -> playersWithRolls.maxByOrNull { it.lastSum }
        }
    }

    fun computeRoundLoser(): DicePlayer? {
        val playersWithRolls = _players.value.filter { it.lastRoll.isNotEmpty() }
        if (playersWithRolls.isEmpty()) return null
        return when (_gameMode.value) {
            DiceGameMode.COMPARE_SIZE,
            DiceGameMode.DRINKING_SMALL_DRINKS -> playersWithRolls.minByOrNull { it.lastSum }
            DiceGameMode.TRUTH_DARE,
            DiceGameMode.DRINKING_BIG_DRINKS -> playersWithRolls.maxByOrNull { it.lastSum }
            DiceGameMode.NUMBER_PENALTY -> playersWithRolls.firstOrNull { it.lastRoll.contains(_penaltyNumber.value) }
            else -> null
        }
    }

    fun drawTruthOrDare(forceType: CardType? = null) {
        _truthDareCard.value = when (forceType) {
            CardType.TRUTH -> TruthOrDareDatabase.randomTruth()
            CardType.DARE -> TruthOrDareDatabase.randomDare()
            null -> TruthOrDareDatabase.random()
        }
    }

    // ════════════════════════════════════════════════════
    // 🎰 吹牛骰盅模式
    // ════════════════════════════════════════════════════
    
    /** 当前叫数：count 个 face（如 3个6） */
    private val _liarBidCount = MutableStateFlow(0)
    val liarBidCount: StateFlow<Int> = _liarBidCount.asStateFlow()
    private val _liarBidFace = MutableStateFlow(1)
    val liarBidFace: StateFlow<Int> = _liarBidFace.asStateFlow()
    /** 当前轮叫的玩家 id (上一个叫的人) */
    private val _liarBidder = MutableStateFlow<Long?>(null)
    val liarBidder: StateFlow<Long?> = _liarBidder.asStateFlow()
    /** 吹牛阶段：BIDDING(叫数中) / SHOWDOWN(开盅了) */
    private val _liarPhase = MutableStateFlow(LiarPhase.SHAKING)
    val liarPhase: StateFlow<LiarPhase> = _liarPhase.asStateFlow()
    /** 开盅结果：true=叫数成立 false=吹牛 */
    private val _liarChallengeResult = MutableStateFlow<LiarChallengeOutcome?>(null)
    val liarChallengeResult: StateFlow<LiarChallengeOutcome?> = _liarChallengeResult.asStateFlow()

    /** 吹牛模式：所有玩家投完骰子（每人偷偷看自己），进入叫数阶段 */
    fun liarStartBidding() {
        _liarPhase.value = LiarPhase.BIDDING
        _liarBidCount.value = 0
        _liarBidFace.value = 1
        _liarBidder.value = null
        _liarChallengeResult.value = null
    }

    /** 当前玩家叫一个新数，必须比上次更大 */
    fun liarRaiseBid(count: Int, face: Int): Boolean {
        // 比较：先按 count 升序，count 相同再按 face 升序
        val curCount = _liarBidCount.value
        val curFace = _liarBidFace.value
        val isHigher = count > curCount || (count == curCount && face > curFace)
        if (curCount == 0 || isHigher) {
            _liarBidCount.value = count
            _liarBidFace.value = face
            val player = _players.value.getOrNull(_currentPlayerIndex.value) ?: return false
            _liarBidder.value = player.id
            // 切到下一玩家继续叫数 / 开盅
            val total = _players.value.size
            _currentPlayerIndex.value = (_currentPlayerIndex.value + 1) % total
            return true
        }
        return false
    }

    /** 当前玩家选择"开盅"，验证上一个叫数 */
    fun liarChallenge() {
        val challenger = _players.value.getOrNull(_currentPlayerIndex.value) ?: return
        val bidderId = _liarBidder.value ?: return
        val bidder = _players.value.firstOrNull { it.id == bidderId } ?: return
        // 统计所有玩家骰子中 bidFace 的数量（1 通常视为癞子，简化版不计癞子）
        val face = _liarBidFace.value
        val total = _players.value.sumOf { p -> p.lastRoll.count { it == face } }
        val claimed = _liarBidCount.value
        val bidderTruthful = total >= claimed  // 实际数量 >= 叫数 → 叫数成立
        val loser = if (bidderTruthful) challenger else bidder
        val winner = if (bidderTruthful) bidder else challenger
        _players.value = _players.value.map {
            when (it.id) {
                winner.id -> it.copy(wins = it.wins + 1, totalScore = it.totalScore + 5)
                else -> it
            }
        }
        _liarChallengeResult.value = LiarChallengeOutcome(
            challengerName = challenger.name,
            bidderName = bidder.name,
            claimedCount = claimed,
            claimedFace = face,
            actualCount = total,
            bidderTruthful = bidderTruthful,
            winnerName = winner.name,
            loserName = loser.name
        )
        _liarPhase.value = LiarPhase.SHOWDOWN
        // 揭开所有骰子
        _isCupCovered.value = false
        _isRevealed.value = true
    }

    fun liarStartNextRound() {
        _liarPhase.value = LiarPhase.SHAKING
        _liarBidCount.value = 0
        _liarBidFace.value = 1
        _liarBidder.value = null
        _liarChallengeResult.value = null
        _currentPlayerIndex.value = 0
        _isCupCovered.value = true
        _isRevealed.value = false
        _players.value = _players.value.map { it.copy(lastRoll = emptyList(), lastSum = 0) }
    }

    fun resetGame() {
        _players.value = _players.value.map {
            it.copy(totalScore = 0, wins = 0, lastRoll = emptyList(), lastSum = 0)
        }
        _currentPlayerIndex.value = 0
        _isCupCovered.value = true
        _isRevealed.value = false
        _isShaking.value = false
        _rollHistory.value = emptyList()
        _truthDareCard.value = null
        _showRoundResult.value = false
    }
}
