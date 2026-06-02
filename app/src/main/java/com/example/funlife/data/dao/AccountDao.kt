// AccountDao.kt — 多账户 DAO（聊天记账 Phase 2A）
//
// 🔒 数据隔离强约束：所有查询必须带 userId 参数（DEVELOPMENT_PRINCIPLES §1.1）
package com.example.funlife.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.funlife.data.model.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: Account): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(accounts: List<Account>): List<Long>

    @Update
    suspend fun update(account: Account)

    @Delete
    suspend fun delete(account: Account)

    @Query("SELECT * FROM accounts WHERE userId = :userId AND isArchived = 0 ORDER BY sortOrder ASC, id ASC")
    fun getActiveAccounts(userId: Long): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE userId = :userId ORDER BY sortOrder ASC, id ASC")
    fun getAllAccounts(userId: Long): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getById(userId: Long, id: Long): Account?

    @Query("SELECT * FROM accounts WHERE userId = :userId AND systemKey = :systemKey LIMIT 1")
    suspend fun getBySystemKey(userId: Long, systemKey: String): Account?

    @Query("SELECT COUNT(*) FROM accounts WHERE userId = :userId")
    suspend fun count(userId: Long): Int

    @Query("UPDATE accounts SET isArchived = :archived, updatedAt = :now WHERE userId = :userId AND id = :id")
    suspend fun setArchived(userId: Long, id: Long, archived: Boolean, now: Long = System.currentTimeMillis())

    /**
     * 由账单聚合刷新某账户余额：
     *   balance = initialBalance + Σ bills.amount  (该 userId + accountId)
     */
    @Query("""
        UPDATE accounts SET
            balance = initialBalance + COALESCE(
                (SELECT SUM(amount) FROM bills WHERE bills.userId = :userId AND bills.accountId = accounts.id),
                0
            ),
            updatedAt = :now
        WHERE userId = :userId AND id = :accountId
    """)
    suspend fun refreshBalance(userId: Long, accountId: Long, now: Long = System.currentTimeMillis())

    /**
     * 一次性刷新该 userId 下所有账户余额。
     */
    @Query("""
        UPDATE accounts SET
            balance = initialBalance + COALESCE(
                (SELECT SUM(amount) FROM bills WHERE bills.userId = :userId AND bills.accountId = accounts.id),
                0
            ),
            updatedAt = :now
        WHERE userId = :userId
    """)
    suspend fun refreshAllBalances(userId: Long, now: Long = System.currentTimeMillis())
}
