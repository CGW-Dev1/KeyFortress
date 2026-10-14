package com.keyfortress.app

import com.keyfortress.app.security.PasswordGenerator
import com.keyfortress.app.security.PasswordGeneratorConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordGeneratorTest {

    @Test
    fun testGenerateLength() {
        val config = PasswordGeneratorConfig(length = 20)
        val password = PasswordGenerator.generate(config)
        assertEquals(20, password.length)
    }

    @Test
    fun testGenerateContainsRequiredCharTypes() {
        val config = PasswordGeneratorConfig(
            length = 16,
            includeUppercase = true,
            includeLowercase = true,
            includeNumbers = true,
            includeSymbols = true
        )
        val password = PasswordGenerator.generate(config)

        assertTrue(password.any { it.isUpperCase() })
        assertTrue(password.any { it.isLowerCase() })
        assertTrue(password.any { it.isDigit() })
        assertTrue(password.any { !it.isLetterOrDigit() })
    }

    @Test
    fun testExcludeAmbiguous() {
        val config = PasswordGeneratorConfig(
            length = 32,
            includeUppercase = true,
            includeLowercase = true,
            includeNumbers = true,
            includeSymbols = false,
            excludeAmbiguous = true
        )
        val password = PasswordGenerator.generate(config)
        val ambiguousChars = listOf('0', 'O', '1', 'l', 'I')

        for (ch in ambiguousChars) {
            assertFalse("Password should not contain $ch", password.contains(ch))
        }
    }
}
