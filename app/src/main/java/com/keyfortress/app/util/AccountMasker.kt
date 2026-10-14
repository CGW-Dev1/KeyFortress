package com.keyfortress.app.util

/**
 * Utility to mask account names (phones, emails, usernames) for privacy on lists.
 */
object AccountMasker {

    fun mask(account: String): String {
        if (account.isBlank()) return ""
        val trimmed = account.trim()

        // 1. Phone number (11 digits e.g. 13812345678)
        if (trimmed.matches(Regex("^1[3-9]\\d{9}$"))) {
            return "${trimmed.substring(0, 3)}****${trimmed.substring(7)}"
        }

        // 2. Email (e.g. user@domain.com)
        if (trimmed.contains('@')) {
            val parts = trimmed.split('@', limit = 2)
            val name = parts[0]
            val domain = parts[1]
            val maskedName = when {
                name.length <= 2 -> "${name.first()}*"
                name.length <= 4 -> "${name.first()}**${name.last()}"
                else -> "${name.substring(0, 2)}***${name.last()}"
            }
            return "$maskedName@$domain"
        }

        // 3. General usernames / strings
        return when {
            trimmed.length <= 2 -> "**"
            trimmed.length <= 4 -> "${trimmed.first()}**${trimmed.last()}"
            trimmed.length <= 8 -> "${trimmed.substring(0, 2)}***${trimmed.substring(trimmed.length - 2)}"
            else -> "${trimmed.substring(0, 3)}****${trimmed.substring(trimmed.length - 3)}"
        }
    }
}
