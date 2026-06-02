// BookChatGateLogicTest.kt — v53 阅光书房 · AI 深聊 VIP3 门控的纯逻辑测试
//
// BookChatViewModel 的门控逻辑核心是：
//   priorUserTurns >= 3 && !deepChatUnlocked → 拒绝
//
// 直接测 ViewModel 需要 mock Application + Database，但门控规则可以
// 抽象为一个纯函数；这里就以"等价规则"形式测，等同于复刻它的判定语义。
//
// 注：如果未来 BookChatViewModel 暴露 testable 工厂，这套测试可改成直接
// 调 send() 然后 collect 验证 Msg.System，但目前规则简单到不必那么重。
package com.example.funlife.viewmodel

import com.example.funlife.vip.VipQuota
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BookChatGateLogicTest {

    /** 复制 BookChatViewModel.send 中的门控判定，让规则变成可测纯函数。 */
    private fun shouldBlock(priorUserTurns: Int, vipLevel: Int): Boolean {
        val unlocked = VipQuota.aiBookDeepChatUnlocked(vipLevel)
        return priorUserTurns >= 3 && !unlocked
    }

    /* ───── 普通用户 ───── */

    @Test fun normal_first_round_passes() {
        assertThat(shouldBlock(0, 0)).isFalse()
    }

    @Test fun normal_third_round_passes() {
        assertThat(shouldBlock(2, 0)).isFalse()
    }

    @Test fun normal_fourth_round_blocked() {
        assertThat(shouldBlock(3, 0)).isTrue()
    }

    @Test fun normal_tenth_round_still_blocked() {
        assertThat(shouldBlock(10, 0)).isTrue()
    }

    /* ───── VIP1 / VIP2 同样被门控 ───── */

    @Test fun vip1_fourth_round_blocked() {
        assertThat(shouldBlock(3, 1)).isTrue()
    }

    @Test fun vip2_fourth_round_blocked() {
        assertThat(shouldBlock(3, 2)).isTrue()
    }

    /* ───── VIP3 / 永久会员 不被门控 ───── */

    @Test fun vip3_fourth_round_passes() {
        assertThat(shouldBlock(3, 3)).isFalse()
    }

    @Test fun vip3_hundredth_round_passes() {
        assertThat(shouldBlock(99, 3)).isFalse()
    }

    @Test fun permanent_fourth_round_passes() {
        assertThat(shouldBlock(3, 99)).isFalse()
    }
}
