package com.keyfortress.app

import com.keyfortress.app.security.AppLockManager
import com.keyfortress.app.security.CryptoManager
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.security.GeneralSecurityException
import javax.crypto.spec.SecretKeySpec

class SecurityQuestionTest {

    @Test
    fun testNormalizeAnswer() {
        assertEquals("beijing", AppLockManager.normalizeAnswer("  Beijing  "))
        assertEquals("shanghai", AppLockManager.normalizeAnswer("Shang Hai"))
        assertEquals("杭州", AppLockManager.normalizeAnswer("  杭州  "))
    }

    @Test
    fun testSecurityQuestionKeyWrappingAndRecovery() {
        val originalVaultKey = CryptoManager.generateRandomKey()
        val question = "你的出生城市是哪里？"
        val answer = " 杭州 "
        val normalizedAnswer = AppLockManager.normalizeAnswer(answer)

        val qSalt = CryptoManager.generateSalt()
        val qKey = CryptoManager.deriveKey(normalizedAnswer.toCharArray(), qSalt)
        val (cipher, iv) = CryptoManager.encryptBytes(originalVaultKey.encoded, qKey)

        // Decrypt with correct answer
        val userEnteredAnswer = "hangzhou"
        val userNormalized = AppLockManager.normalizeAnswer(userEnteredAnswer)
        // For Chinese characters:
        val correctNormalized = AppLockManager.normalizeAnswer("杭州")
        val correctKey = CryptoManager.deriveKey(correctNormalized.toCharArray(), qSalt)
        val recoveredBytes = CryptoManager.decryptBytes(cipher, iv, correctKey)
        assertArrayEquals(originalVaultKey.encoded, recoveredBytes)

        // Decrypt with incorrect answer
        val wrongNormalized = AppLockManager.normalizeAnswer("上海")
        val wrongKey = CryptoManager.deriveKey(wrongNormalized.toCharArray(), qSalt)
        try {
            CryptoManager.decryptBytes(cipher, iv, wrongKey)
            fail("Decryption with wrong answer should fail GCM authentication")
        } catch (_: GeneralSecurityException) {
            // Expected
        }
    }
}
