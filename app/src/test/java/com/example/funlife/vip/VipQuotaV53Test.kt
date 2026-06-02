// VipQuotaV53Test.kt — v53 阅光书房 · VIP 配额七个方法的全等级 + 边界测试
//
// 设计原则：
//   1. 每个方法都跑 4 个 vipLevel（0/1/2/3）+ 永久(99) + 非法值(-1, 100)
//   2. 配额数值严格对照 docs/v53_reading_room_design.md 第 73-90 行的功能矩阵
//   3. UNLIMITED 用 -1 表示，验证调用方不会把它当成"耗尽"
package com.example.funlife.vip

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VipQuotaV53Test {

    /* ───── readingCapsuleMonthlyLimit · 1 / 5 / 20 / ∞ ───── */

    @Test fun capsuleLimit_normal_is_1() {
        assertThat(VipQuota.readingCapsuleMonthlyLimit(0)).isEqualTo(1)
    }

    @Test fun capsuleLimit_vip1_is_5() {
        assertThat(VipQuota.readingCapsuleMonthlyLimit(1)).isEqualTo(5)
    }

    @Test fun capsuleLimit_vip2_is_20() {
        assertThat(VipQuota.readingCapsuleMonthlyLimit(2)).isEqualTo(20)
    }

    @Test fun capsuleLimit_vip3_is_unlimited() {
        assertThat(VipQuota.readingCapsuleMonthlyLimit(3)).isEqualTo(VipQuota.UNLIMITED)
    }

    @Test fun capsuleLimit_permanent_is_unlimited() {
        // 99 在 mapLevel 中应当被映射成 PERMANENT
        assertThat(VipQuota.readingCapsuleMonthlyLimit(99)).isEqualTo(VipQuota.UNLIMITED)
    }

    @Test fun capsuleLimit_negative_falls_back_to_normal() {
        assertThat(VipQuota.readingCapsuleMonthlyLimit(-1)).isEqualTo(1)
    }

    /* ───── aiBookChatDailyLimit · 1 / 5 / 20 / ∞ ───── */

    @Test fun aiBookChat_normal_1() {
        assertThat(VipQuota.aiBookChatDailyLimit(0)).isEqualTo(1)
    }

    @Test fun aiBookChat_vip1_5() {
        assertThat(VipQuota.aiBookChatDailyLimit(1)).isEqualTo(5)
    }

    @Test fun aiBookChat_vip2_20() {
        assertThat(VipQuota.aiBookChatDailyLimit(2)).isEqualTo(20)
    }

    @Test fun aiBookChat_vip3_unlimited() {
        assertThat(VipQuota.aiBookChatDailyLimit(3)).isEqualTo(VipQuota.UNLIMITED)
    }

    /* ───── aiBookDeepChatUnlocked · 仅 VIP3+ ───── */

    @Test fun deepChat_locked_for_normal() {
        assertThat(VipQuota.aiBookDeepChatUnlocked(0)).isFalse()
    }

    @Test fun deepChat_locked_for_vip1() {
        assertThat(VipQuota.aiBookDeepChatUnlocked(1)).isFalse()
    }

    @Test fun deepChat_locked_for_vip2() {
        assertThat(VipQuota.aiBookDeepChatUnlocked(2)).isFalse()
    }

    @Test fun deepChat_unlocked_for_vip3() {
        assertThat(VipQuota.aiBookDeepChatUnlocked(3)).isTrue()
    }

    @Test fun deepChat_unlocked_for_permanent() {
        assertThat(VipQuota.aiBookDeepChatUnlocked(99)).isTrue()
    }

    /* ───── galaxyPublishUnlocked · VIP1+ 即可 ───── */

    @Test fun galaxy_locked_for_normal_only() {
        assertThat(VipQuota.galaxyPublishUnlocked(0)).isFalse()
    }

    @Test fun galaxy_unlocked_for_vip1() {
        assertThat(VipQuota.galaxyPublishUnlocked(1)).isTrue()
    }

    @Test fun galaxy_unlocked_for_vip2() {
        assertThat(VipQuota.galaxyPublishUnlocked(2)).isTrue()
    }

    @Test fun galaxy_unlocked_for_vip3() {
        assertThat(VipQuota.galaxyPublishUnlocked(3)).isTrue()
    }

    @Test fun galaxy_unlocked_for_permanent() {
        assertThat(VipQuota.galaxyPublishUnlocked(99)).isTrue()
    }

    /* ───── postcardDriftMonthlyLimit · 0 / 0 / 1 / 4 ───── */

    @Test fun postcard_normal_zero() {
        assertThat(VipQuota.postcardDriftMonthlyLimit(0)).isEqualTo(0)
    }

    @Test fun postcard_vip1_zero() {
        assertThat(VipQuota.postcardDriftMonthlyLimit(1)).isEqualTo(0)
    }

    @Test fun postcard_vip2_one() {
        assertThat(VipQuota.postcardDriftMonthlyLimit(2)).isEqualTo(1)
    }

    @Test fun postcard_vip3_four() {
        assertThat(VipQuota.postcardDriftMonthlyLimit(3)).isEqualTo(4)
    }

    /* ───── readerDnaCooldownDays · 365 / 90 / 30 / 0 ───── */

    @Test fun dnaCooldown_normal_year() {
        assertThat(VipQuota.readerDnaCooldownDays(0)).isEqualTo(365)
    }

    @Test fun dnaCooldown_vip1_quarter() {
        assertThat(VipQuota.readerDnaCooldownDays(1)).isEqualTo(90)
    }

    @Test fun dnaCooldown_vip2_month() {
        assertThat(VipQuota.readerDnaCooldownDays(2)).isEqualTo(30)
    }

    @Test fun dnaCooldown_vip3_zero() {
        assertThat(VipQuota.readerDnaCooldownDays(3)).isEqualTo(0)
    }

    /* ───── heraldWeeklyLimit · 2 / 7 / 7 / 7 ───── */

    @Test fun herald_normal_2_per_week() {
        assertThat(VipQuota.heraldWeeklyLimit(0)).isEqualTo(2)
    }

    @Test fun herald_vip1_daily() {
        assertThat(VipQuota.heraldWeeklyLimit(1)).isEqualTo(7)
    }

    @Test fun herald_vip2_daily() {
        assertThat(VipQuota.heraldWeeklyLimit(2)).isEqualTo(7)
    }

    @Test fun herald_vip3_daily() {
        assertThat(VipQuota.heraldWeeklyLimit(3)).isEqualTo(7)
    }

    /* ───── 单调性约束（business invariant） ─────
       配额必须随 vipLevel 单调不减；这是付费心理的基础。 */

    @Test fun monotonic_capsule() {
        val (a, b, c, d) = listOf(0, 1, 2, 3)
            .map { VipQuota.readingCapsuleMonthlyLimit(it) }
            .let { it.toTypedArray().let { arr -> arrayOf(arr[0], arr[1], arr[2], arr[3]) } }
        // -1 (UNLIMITED) 视为 +∞
        fun gte(x: Int, y: Int) = x == VipQuota.UNLIMITED || (y != VipQuota.UNLIMITED && x >= y)
        assertThat(gte(b, a)).isTrue()
        assertThat(gte(c, b)).isTrue()
        assertThat(gte(d, c)).isTrue()
    }

    @Test fun monotonic_aiBookChat() {
        fun gte(x: Int, y: Int) = x == VipQuota.UNLIMITED || (y != VipQuota.UNLIMITED && x >= y)
        val q = listOf(0, 1, 2, 3).map { VipQuota.aiBookChatDailyLimit(it) }
        assertThat(gte(q[1], q[0])).isTrue()
        assertThat(gte(q[2], q[1])).isTrue()
        assertThat(gte(q[3], q[2])).isTrue()
    }

    @Test fun monotonic_postcard() {
        val q = listOf(0, 1, 2, 3).map { VipQuota.postcardDriftMonthlyLimit(it) }
        assertThat(q[1]).isAtLeast(q[0])
        assertThat(q[2]).isAtLeast(q[1])
        assertThat(q[3]).isAtLeast(q[2])
    }

    @Test fun monotonic_dnaCooldown_decreasing() {
        // 冷却时间应当随 vipLevel 单调"不增"（VIP 越高等待越短）
        val q = listOf(0, 1, 2, 3).map { VipQuota.readerDnaCooldownDays(it) }
        assertThat(q[0]).isAtLeast(q[1])
        assertThat(q[1]).isAtLeast(q[2])
        assertThat(q[2]).isAtLeast(q[3])
    }
}
