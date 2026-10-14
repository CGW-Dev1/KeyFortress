package com.keyfortress.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.keyfortress.app.data.entity.PasswordHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: PasswordHistoryEntity): Long

    @Query("SELECT * FROM password_history WHERE account_id = :accountId ORDER BY changed_at DESC LIMIT 3")
    fun getByAccountIdFlow(accountId: Long): Flow<List<PasswordHistoryEntity>>

    @Query("SELECT * FROM password_history WHERE account_id = :accountId ORDER BY changed_at DESC")
    suspend fun getByAccountId(accountId: Long): List<PasswordHistoryEntity>

    @Query("""
        DELETE FROM password_history 
        WHERE account_id = :accountId 
          AND id NOT IN (
              SELECT id FROM password_history 
              WHERE account_id = :accountId 
              ORDER BY changed_at DESC 
              LIMIT :keepCount
          )
    """)
    suspend fun trimHistory(accountId: Long, keepCount: Int = 3)

    @Transaction
    suspend fun recordHistoryAndTrim(history: PasswordHistoryEntity, keepCount: Int = 3) {
        insert(history)
        trimHistory(history.accountId, keepCount)
    }

    @Query("DELETE FROM password_history WHERE account_id = :accountId")
    suspend fun deleteByAccountId(accountId: Long)
}
