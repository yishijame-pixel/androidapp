// TransactionHelper.kt - 数据库事务辅助工具
package com.example.funlife.utils

import androidx.room.withTransaction
import com.example.funlife.data.database.AppDatabase

/**
 * 数据库事务辅助工具
 * 提供统一的事务管理接口
 */
object TransactionHelper {
    
    /**
     * 在事务中执行操作
     * @param database 数据库实例
     * @param block 要执行的操作
     * @return 操作结果
     */
    suspend fun <T> executeInTransaction(
        database: AppDatabase,
        block: suspend () -> T
    ): T {
        return database.withTransaction {
            block()
        }
    }
    
    /**
     * 在事务中执行操作，并返回 Result
     * @param database 数据库实例
     * @param block 要执行的操作
     * @return Result<T>
     */
    suspend fun <T> executeInTransactionWithResult(
        database: AppDatabase,
        block: suspend () -> T
    ): Result<T> {
        return try {
            val result = database.withTransaction {
                block()
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
