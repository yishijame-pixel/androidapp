// AccountRepository.kt — 多账户仓库（聊天记账 Phase 2A）
//
// 🔒 所有公开方法都强制 userId 参数（无默认值）。
// 🌱 ensureSeeded：用户首次进入聊天记账时插入 6 个默认账户（按 systemKey 幂等）。
package com.example.funlife.repository

import com.example.funlife.data.dao.AccountDao
import com.example.funlife.data.model.Account
import com.example.funlife.data.model.DEFAULT_ACCOUNT_SPECS
import kotlinx.coroutines.flow.Flow

class AccountRepository(private val accountDao: AccountDao) {

    fun getActiveAccounts(userId: Long): Flow<List<Account>> {
        require(userId > 0L) { "userId must be > 0 (data isolation)" }
        return accountDao.getActiveAccounts(userId)
    }

    fun getAllAccounts(userId: Long): Flow<List<Account>> {
        require(userId > 0L) { "userId must be > 0 (data isolation)" }
        return accountDao.getAllAccounts(userId)
    }

    suspend fun getById(userId: Long, id: Long): Account? {
        require(userId > 0L)
        return accountDao.getById(userId, id)
    }

    suspend fun insert(account: Account): Long {
        require(account.userId > 0L) { "Account.userId must be > 0" }
        return accountDao.insert(account)
    }

    suspend fun update(account: Account) {
        require(account.userId > 0L)
        accountDao.update(account.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(account: Account) {
        require(account.userId > 0L)
        accountDao.delete(account)
    }

    suspend fun setArchived(userId: Long, id: Long, archived: Boolean) {
        require(userId > 0L)
        accountDao.setArchived(userId, id, archived)
    }

    suspend fun refreshAllBalances(userId: Long) {
        require(userId > 0L)
        accountDao.refreshAllBalances(userId)
    }

    /**
     * 幂等地为某用户初始化 6 个默认账户。已经存在（按 systemKey 匹配）的不重复插入。
     * 返回新插入的数量。
     */
    suspend fun ensureSeeded(userId: Long): Int {
        require(userId > 0L) { "userId must be > 0" }
        if (accountDao.count(userId) > 0) {
            // 仅补全缺失的（向旧用户补发新增默认账户也走这个逻辑）
            var added = 0
            DEFAULT_ACCOUNT_SPECS.forEach { spec ->
                if (accountDao.getBySystemKey(userId, spec.systemKey) == null) {
                    accountDao.insert(spec.toAccount(userId))
                    added++
                }
            }
            return added
        }
        // 新用户首次：批量 IGNORE 插入
        val list = DEFAULT_ACCOUNT_SPECS.map { it.toAccount(userId) }
        val ids = accountDao.insertAllIgnore(list)
        return ids.count { it >= 0 }
    }
}

private fun com.example.funlife.data.model.DefaultAccountSpec.toAccount(userId: Long): Account =
    Account(
        userId = userId,
        name = name,
        type = type,
        icon = icon,
        color = color,
        sortOrder = sortOrder,
        systemKey = systemKey
    )
