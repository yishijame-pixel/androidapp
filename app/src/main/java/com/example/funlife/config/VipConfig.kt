package com.example.funlife.config

/**
 * VIP支付配置
 */
object VipConfig {
    /**
     * 是否启用支付倒计时
     * true: 用户需要等待60秒后才能点击"我已完成支付"按钮
     * false: 立即可以点击（用于测试）
     * 
     * 测试完成后请将此值改为 true
     */
    const val ENABLE_PAYMENT_COUNTDOWN = true  // 已启用正式模式
    
    /**
     * 倒计时秒数
     */
    const val COUNTDOWN_SECONDS = 60
    
    /**
     * 是否显示金币不足提示
     * true: 显示金币不足提示
     * false: 不显示金币不足提示（VIP用户体验更好）
     */
    const val SHOW_INSUFFICIENT_COINS_WARNING = false  // 已禁用金币不足提示
}
