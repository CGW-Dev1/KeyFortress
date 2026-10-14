package com.keyfortress.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["title"]),
        Index(value = ["category"]),
        Index(value = ["is_starred"]),
        Index(value = ["pinyin_index"])
    ]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "account_name")
    val accountName: String,

    @ColumnInfo(name = "password_cipher")
    val passwordCipher: String,

    @ColumnInfo(name = "password_iv")
    val passwordIv: String,

    @ColumnInfo(name = "linked_contact")
    val linkedContact: String = "",

    @ColumnInfo(name = "category")
    val category: String = "其它",

    @ColumnInfo(name = "notes_cipher")
    val notesCipher: String = "",

    @ColumnInfo(name = "notes_iv")
    val notesIv: String = "",

    @ColumnInfo(name = "is_starred")
    val isStarred: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "pinyin_index")
    val pinyinIndex: String = ""
)
