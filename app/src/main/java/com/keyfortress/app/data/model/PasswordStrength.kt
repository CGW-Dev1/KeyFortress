package com.keyfortress.app.data.model

import androidx.compose.ui.graphics.Color

enum class PasswordStrengthLevel(val label: String, val color: Color, val fraction: Float) {
    EMPTY("无", Color(0xFF9E9E9E), 0f),
    VERY_WEAK("极弱", Color(0xFFE53935), 0.2f),
    WEAK("较弱", Color(0xFFFB8C00), 0.4f),
    MEDIUM("中等", Color(0xFFFDD835), 0.6f),
    STRONG("强", Color(0xFF43A047), 0.8f),
    VERY_STRONG("极强", Color(0xFF1E88E5), 1.0f)
}

object PasswordStrengthCalculator {

    fun calculate(password: String): PasswordStrengthLevel {
        if (password.isEmpty()) return PasswordStrengthLevel.EMPTY

        var score = 0

        // Length checks
        when {
            password.length >= 16 -> score += 35
            password.length >= 12 -> score += 25
            password.length >= 8 -> score += 15
            else -> score += 5
        }

        // Diversity checks
        val hasLower = password.any { it.isLowerCase() }
        val hasUpper = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSymbol = password.any { !it.isLetterOrDigit() }

        var charTypes = 0
        if (hasLower) charTypes++
        if (hasUpper) charTypes++
        if (hasDigit) charTypes++
        if (hasSymbol) charTypes++

        score += charTypes * 12

        // Bonus for all types and good length
        if (charTypes >= 4 && password.length >= 12) {
            score += 15
        }

        // Deductions for simple repetitive or sequential characters
        if (password.all { it == password.first() }) {
            score = 10
        }

        return when {
            score >= 80 -> PasswordStrengthLevel.VERY_STRONG
            score >= 60 -> PasswordStrengthLevel.STRONG
            score >= 40 -> PasswordStrengthLevel.MEDIUM
            score >= 20 -> PasswordStrengthLevel.WEAK
            else -> PasswordStrengthLevel.VERY_WEAK
        }
    }
}
