package com.keyfortress.app.data.model

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Represents a custom field in an account (e.g. 交易密码, 验证密码, PIN码, 密保问题).
 */
@Immutable
data class CustomField(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val value: String,
    val isSecured: Boolean = true
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("label", label)
            put("value", value)
            put("isSecured", isSecured)
        }
    }

    companion object {
        val PRESET_LABELS = listOf(
            "交易密码",
            "支付PIN码",
            "查询密码",
            "安全邮箱",
            "密保问题",
            "动态口令",
            "手势密码",
            "第二手机号"
        )

        fun fromJson(json: JSONObject): CustomField {
            return CustomField(
                id = json.optString("id", UUID.randomUUID().toString()),
                label = json.optString("label", ""),
                value = json.optString("value", ""),
                isSecured = json.optBoolean("isSecured", true)
            )
        }

        fun listToJsonString(list: List<CustomField>): String {
            val array = JSONArray()
            list.forEach { array.put(it.toJson()) }
            return array.toString()
        }

        fun listFromJsonString(jsonString: String): List<CustomField> {
            if (jsonString.isBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonString)
                val result = ArrayList<CustomField>(array.length())
                for (i in 0 until array.length()) {
                    result.add(fromJson(array.getJSONObject(i)))
                }
                result
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
