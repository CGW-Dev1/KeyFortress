package com.keyfortress.app.security

import java.security.SecureRandom

data class PasswordGeneratorConfig(
    val length: Int = 16,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeNumbers: Boolean = true,
    val includeSymbols: Boolean = true,
    val excludeAmbiguous: Boolean = false
)

object PasswordGenerator {

    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val NUMBERS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?"

    private const val AMBIGUOUS_CHARS = "0O1lI"

    private val secureRandom = SecureRandom()

    fun generate(config: PasswordGeneratorConfig): String {
        var upper = UPPERCASE
        var lower = LOWERCASE
        var numbers = NUMBERS
        var symbols = SYMBOLS

        if (config.excludeAmbiguous) {
            upper = upper.filter { it !in AMBIGUOUS_CHARS }
            lower = lower.filter { it !in AMBIGUOUS_CHARS }
            numbers = numbers.filter { it !in AMBIGUOUS_CHARS }
        }

        val pool = StringBuilder()
        val mandatoryChars = mutableListOf<Char>()

        if (config.includeUppercase && upper.isNotEmpty()) {
            pool.append(upper)
            mandatoryChars.add(upper[secureRandom.nextInt(upper.length)])
        }
        if (config.includeLowercase && lower.isNotEmpty()) {
            pool.append(lower)
            mandatoryChars.add(lower[secureRandom.nextInt(lower.length)])
        }
        if (config.includeNumbers && numbers.isNotEmpty()) {
            pool.append(numbers)
            mandatoryChars.add(numbers[secureRandom.nextInt(numbers.length)])
        }
        if (config.includeSymbols && symbols.isNotEmpty()) {
            pool.append(symbols)
            mandatoryChars.add(symbols[secureRandom.nextInt(symbols.length)])
        }

        // If no charsets were selected, fallback to letters + digits
        if (pool.isEmpty()) {
            pool.append(LOWERCASE).append(NUMBERS)
            mandatoryChars.add(LOWERCASE[secureRandom.nextInt(LOWERCASE.length)])
            mandatoryChars.add(NUMBERS[secureRandom.nextInt(NUMBERS.length)])
        }

        val passwordChars = mutableListOf<Char>()
        passwordChars.addAll(mandatoryChars)

        val totalLength = config.length.coerceIn(8, 32)
        while (passwordChars.size < totalLength) {
            val randomIndex = secureRandom.nextInt(pool.length)
            passwordChars.add(pool[randomIndex])
        }

        // Fisher-Yates shuffle with SecureRandom
        for (i in passwordChars.indices.reversed()) {
            val j = secureRandom.nextInt(i + 1)
            val temp = passwordChars[i]
            passwordChars[i] = passwordChars[j]
            passwordChars[j] = temp
        }

        return passwordChars.joinToString("")
    }
}
