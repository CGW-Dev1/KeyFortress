package com.keyfortress.app.data.model

import org.json.JSONArray
import org.json.JSONObject

data class VaultBackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val app: String = "KeyFortress",
    val accounts: List<AccountBackupItem>
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("version", version)
        root.put("exportedAt", exportedAt)
        root.put("app", app)

        val accountsArray = JSONArray()
        for (acc in accounts) {
            val item = JSONObject()
            item.put("title", acc.title)
            item.put("accountName", acc.accountName)
            item.put("password", acc.password)
            item.put("linkedContact", acc.linkedContact)
            item.put("category", acc.category)
            item.put("notes", acc.notes)
            item.put("isStarred", acc.isStarred)
            item.put("createdAt", acc.createdAt)
            item.put("updatedAt", acc.updatedAt)

            val historyArray = JSONArray()
            for (h in acc.history) {
                val hItem = JSONObject()
                hItem.put("oldPassword", h.oldPassword)
                hItem.put("changedAt", h.changedAt)
                historyArray.put(hItem)
            }
            item.put("history", historyArray)

            val customFieldsArray = JSONArray()
            for (cf in acc.customFields) {
                customFieldsArray.put(cf.toJson())
            }
            item.put("customFields", customFieldsArray)

            accountsArray.put(item)
        }
        root.put("accounts", accountsArray)
        return root.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): VaultBackupPayload {
            val root = JSONObject(jsonStr)
            val version = root.optInt("version", 1)
            val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
            val app = root.optString("app", "KeyFortress")

            val accountsList = mutableListOf<AccountBackupItem>()
            val accountsArray = root.optJSONArray("accounts") ?: JSONArray()
            for (i in 0 until accountsArray.length()) {
                val item = accountsArray.getJSONObject(i)
                val historyList = mutableListOf<HistoryBackupItem>()
                val historyArray = item.optJSONArray("history") ?: JSONArray()
                for (j in 0 until historyArray.length()) {
                    val h = historyArray.getJSONObject(j)
                    historyList.add(
                        HistoryBackupItem(
                            oldPassword = h.optString("oldPassword", ""),
                            changedAt = h.optLong("changedAt", System.currentTimeMillis())
                        )
                    )
                }

                val customFieldsList = mutableListOf<CustomField>()
                val cfArray = item.optJSONArray("customFields") ?: JSONArray()
                for (j in 0 until cfArray.length()) {
                    customFieldsList.add(CustomField.fromJson(cfArray.getJSONObject(j)))
                }

                accountsList.add(
                    AccountBackupItem(
                        title = item.optString("title", ""),
                        accountName = item.optString("accountName", ""),
                        password = item.optString("password", ""),
                        linkedContact = item.optString("linkedContact", ""),
                        category = item.optString("category", CategoryConstants.OTHER),
                        notes = item.optString("notes", ""),
                        isStarred = item.optBoolean("isStarred", false),
                        createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = item.optLong("updatedAt", System.currentTimeMillis()),
                        history = historyList,
                        customFields = customFieldsList
                    )
                )
            }

            return VaultBackupPayload(
                version = version,
                exportedAt = exportedAt,
                app = app,
                accounts = accountsList
            )
        }
    }
}

data class AccountBackupItem(
    val title: String,
    val accountName: String,
    val password: String,
    val linkedContact: String,
    val category: String,
    val notes: String,
    val isStarred: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val history: List<HistoryBackupItem> = emptyList(),
    val customFields: List<CustomField> = emptyList()
)

data class HistoryBackupItem(
    val oldPassword: String,
    val changedAt: Long
)

/**
 * Encrypted container format written to the backup file.
 */
data class EncryptedVaultContainer(
    val format: String = "KEYFORTRESS_VAULT",
    val version: Int = 1,
    val saltBase64: String,
    val ivBase64: String,
    val ciphertextBase64: String
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("format", format)
        obj.put("version", version)
        obj.put("salt", saltBase64)
        obj.put("iv", ivBase64)
        obj.put("ciphertext", ciphertextBase64)
        return obj.toString(2)
    }

    companion object {
        fun fromJson(jsonStr: String): EncryptedVaultContainer {
            val obj = JSONObject(jsonStr)
            val format = obj.getString("format")
            if (format != "KEYFORTRESS_VAULT") {
                throw IllegalArgumentException("非法的备份文件格式")
            }
            return EncryptedVaultContainer(
                format = format,
                version = obj.optInt("version", 1),
                saltBase64 = obj.getString("salt"),
                ivBase64 = obj.getString("iv"),
                ciphertextBase64 = obj.getString("ciphertext")
            )
        }
    }
}
