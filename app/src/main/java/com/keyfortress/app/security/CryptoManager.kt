package com.keyfortress.app.security

import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val AES_GCM_ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "AES"
    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12

    private val secureRandom = SecureRandom()

    fun generateSalt(size: Int = 32): ByteArray {
        val salt = ByteArray(size)
        secureRandom.nextBytes(salt)
        return salt
    }

    /**
     * Derives a 256-bit AES SecretKey from the user's master password and salt using PBKDF2.
     */
    fun deriveKey(password: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, KEY_ALGORITHM)
    }

    /**
     * Generates a verification hash string to verify the master password without decrypting data.
     */
    fun createVerificationHash(password: CharArray, salt: ByteArray): String {
        val spec = PBEKeySpec(password, salt, ITERATIONS + 1, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val hashBytes = factory.generateSecret(spec).encoded
        return toBase64(hashBytes)
    }

    /**
     * Encrypts plaintext using AES-256-GCM.
     * Returns Pair(ciphertextBase64, ivBase64).
     */
    fun encrypt(plaintext: String, secretKey: SecretKey): Pair<String, String> {
        if (plaintext.isEmpty()) return Pair("", "")
        return encryptBytes(plaintext.toByteArray(StandardCharsets.UTF_8), secretKey)
    }

    /**
     * Decrypts ciphertextBase64 with ivBase64 using AES-256-GCM.
     */
    fun decrypt(ciphertextBase64: String, ivBase64: String, secretKey: SecretKey): String {
        if (ciphertextBase64.isEmpty()) return ""
        val decryptedBytes = decryptBytes(ciphertextBase64, ivBase64, secretKey)
        return String(decryptedBytes, StandardCharsets.UTF_8)
    }

    fun generateRandomKey(sizeBytes: Int = 32): SecretKey {
        val keyBytes = ByteArray(sizeBytes)
        secureRandom.nextBytes(keyBytes)
        return SecretKeySpec(keyBytes, KEY_ALGORITHM)
    }

    /**
     * Encrypts raw byte array using AES-256-GCM.
     * Returns Pair(ciphertextBase64, ivBase64).
     */
    fun encryptBytes(data: ByteArray, secretKey: SecretKey): Pair<String, String> {
        val iv = ByteArray(IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val cipherBytes = cipher.doFinal(data)
        return Pair(toBase64(cipherBytes), toBase64(iv))
    }

    /**
     * Decrypts ciphertextBase64 with ivBase64 using AES-256-GCM to raw byte array.
     */
    fun decryptBytes(ciphertextBase64: String, ivBase64: String, secretKey: SecretKey): ByteArray {
        val cipherBytes = fromBase64(ciphertextBase64)
        val iv = fromBase64(ivBase64)

        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        return cipher.doFinal(cipherBytes)
    }

    fun toBase64(bytes: ByteArray): String {
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun fromBase64(str: String): ByteArray {
        return Base64.getDecoder().decode(str)
    }
}
