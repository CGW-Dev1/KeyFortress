package com.keyfortress.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "password_history",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["account_id"]),
        Index(value = ["changed_at"])
    ]
)
data class PasswordHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "account_id")
    val accountId: Long,

    @ColumnInfo(name = "old_password_cipher")
    val oldPasswordCipher: String,

    @ColumnInfo(name = "old_password_iv")
    val oldPasswordIv: String,

    @ColumnInfo(name = "changed_at")
    val changedAt: Long = System.currentTimeMillis()
)
