package com.example.funlife.account

/**
 * 云端钱包快照（account_recover 返回，用于恢复金币/积分）。
 */
data class CloudWalletSnapshot(
    val balance: Int = 0,
    val totalEarned: Int = 0,
    val totalSpent: Int = 0,
    val pointsBalance: Int = 0,
    val hasSnapshot: Boolean = false,
)
