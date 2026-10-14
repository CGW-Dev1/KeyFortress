package com.keyfortress.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.keyfortress.app.data.dao.AccountDao
import com.keyfortress.app.data.dao.PasswordHistoryDao
import com.keyfortress.app.data.entity.AccountEntity
import com.keyfortress.app.data.entity.PasswordHistoryEntity
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [AccountEntity::class, PasswordHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun passwordHistoryDao(): PasswordHistoryDao

    companion object {
        private const val DB_NAME = "keyfortress_vault.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context, passphraseBytes: ByteArray): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext, passphraseBytes).also {
                    INSTANCE = it
                }
            }
        }

        private fun buildDatabase(context: Context, passphraseBytes: ByteArray): AppDatabase {
            val factory = SupportFactory(passphraseBytes)
            return Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        }

        fun closeInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
