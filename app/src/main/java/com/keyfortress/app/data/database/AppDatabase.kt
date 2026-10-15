package com.keyfortress.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.keyfortress.app.data.dao.AccountDao
import com.keyfortress.app.data.dao.CategoryDao
import com.keyfortress.app.data.dao.PasswordHistoryDao
import com.keyfortress.app.data.entity.AccountEntity
import com.keyfortress.app.data.entity.CategoryEntity
import com.keyfortress.app.data.entity.PasswordHistoryEntity
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [AccountEntity::class, PasswordHistoryEntity::class, CategoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun passwordHistoryDao(): PasswordHistoryDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        private const val DB_NAME = "keyfortress_vault.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add custom fields columns to accounts
                db.execSQL("ALTER TABLE accounts ADD COLUMN custom_fields_cipher TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE accounts ADD COLUMN custom_fields_iv TEXT NOT NULL DEFAULT ''")

                // Create categories table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        is_default INTEGER NOT NULL DEFAULT 0,
                        display_order INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_categories_name ON categories(name)")

                // Prepopulate categories
                val now = System.currentTimeMillis()
                db.execSQL("INSERT OR IGNORE INTO categories (name, is_default, display_order, created_at) VALUES ('社交', 0, 1, $now)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, is_default, display_order, created_at) VALUES ('工作', 0, 2, $now)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, is_default, display_order, created_at) VALUES ('金融', 0, 3, $now)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, is_default, display_order, created_at) VALUES ('娱乐', 0, 4, $now)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, is_default, display_order, created_at) VALUES ('其它', 1, 5, $now)")
            }
        }

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
                .addMigrations(MIGRATION_1_2)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        val now = System.currentTimeMillis()
                        db.execSQL("INSERT OR IGNORE INTO categories (name, is_default, display_order, created_at) VALUES ('社交', 0, 1, $now)")
                        db.execSQL("INSERT OR IGNORE INTO categories (name, is_default, display_order, created_at) VALUES ('工作', 0, 2, $now)")
                        db.execSQL("INSERT OR IGNORE INTO categories (name, is_default, display_order, created_at) VALUES ('金融', 0, 3, $now)")
                        db.execSQL("INSERT OR IGNORE INTO categories (name, is_default, display_order, created_at) VALUES ('娱乐', 0, 4, $now)")
                        db.execSQL("INSERT OR IGNORE INTO categories (name, is_default, display_order, created_at) VALUES ('其它', 1, 5, $now)")
                    }
                })
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
