package com.keyfortress.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.keyfortress.app.data.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM accounts WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE accounts SET category = :targetCategory WHERE category = :sourceCategory")
    suspend fun updateCategoryName(sourceCategory: String, targetCategory: String)

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    fun getByIdFlow(id: Long): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts ORDER BY is_starred DESC, updated_at DESC")
    fun getAllFlow(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE is_starred = 1 ORDER BY updated_at DESC")
    fun getStarredFlow(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE category = :category ORDER BY is_starred DESC, updated_at DESC")
    fun getByCategoryFlow(category: String): Flow<List<AccountEntity>>

    @Query("""
        SELECT * FROM accounts 
        WHERE title LIKE '%' || :query || '%' 
           OR account_name LIKE '%' || :query || '%' 
           OR linked_contact LIKE '%' || :query || '%'
           OR pinyin_index LIKE '%' || :query || '%'
        ORDER BY is_starred DESC, updated_at DESC
    """)
    fun searchFlow(query: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY id ASC")
    suspend fun getAllList(): List<AccountEntity>

    @Query("UPDATE accounts SET is_starred = :isStarred WHERE id = :id")
    suspend fun updateStarred(id: Long, isStarred: Boolean)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getCount(): Int

    @Query("DELETE FROM accounts")
    suspend fun clearAll()
}
