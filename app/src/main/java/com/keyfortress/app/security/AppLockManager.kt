package com.keyfortress.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.keyfortress.app.data.database.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

object AppLockManager {

    private const val PREFS_FILE = "keyfortress_secure_prefs"

    private const val KEY_IS_INITIALIZED = "is_initialized"
    private const val KEY_MASTER_SALT = "master_salt"
    private const val KEY_MASTER_VERIFIER = "master_verifier"
    private const val KEY_ENCRYPTED_VAULT_KEY = "encrypted_vault_key"
    private const val KEY_VAULT_KEY_IV = "vault_key_iv"

    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_BIOMETRIC_CIPHER = "biometric_cipher"
    private const val KEY_BIOMETRIC_IV = "biometric_iv"
    private const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout"

    // Security Question storage keys
    private const val KEY_QUESTION_TEXT = "security_question_text"
    private const val KEY_QUESTION_SALT = "security_question_salt"
    private const val KEY_QUESTION_CIPHER = "security_question_cipher"
    private const val KEY_QUESTION_IV = "security_question_iv"

    const val TIMEOUT_IMMEDIATE = 0
    const val TIMEOUT_30_SECONDS = 30
    const val TIMEOUT_1_MINUTE = 60
    const val TIMEOUT_5_MINUTES = 300

    val PRESET_QUESTIONS = listOf(
        "你的出生地（城市）是哪里？",
        "你母亲的姓名是？",
        "你的小学校名是？",
        "你第一只宠物的名字是？",
        "你的中学班主任姓名是？"
    )

    private var prefs: SharedPreferences? = null

    @Volatile
    private var activeMasterKey: SecretKey? = null

    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var backgroundTimestamp: Long = 0L

    fun init(context: Context) {
        if (prefs == null) {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            prefs = EncryptedSharedPreferences.create(
                PREFS_FILE,
                masterKeyAlias,
                context.applicationContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    private fun getPrefs(): SharedPreferences {
        return prefs ?: throw IllegalStateException("AppLockManager is not initialized. Call init(context) first.")
    }

    fun isVaultSetup(): Boolean {
        return getPrefs().getBoolean(KEY_IS_INITIALIZED, false)
    }

    fun isBiometricEnabled(): Boolean {
        return getPrefs().getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        getPrefs().edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        if (enabled && activeMasterKey != null) {
            saveBiometricKey(activeMasterKey!!.encoded)
        } else if (!enabled) {
            getPrefs().edit()
                .remove(KEY_BIOMETRIC_CIPHER)
                .remove(KEY_BIOMETRIC_IV)
                .apply()
            KeyStoreManager.clearBiometricKey()
        }
    }

    fun getAutoLockTimeout(): Int {
        return getPrefs().getInt(KEY_AUTO_LOCK_TIMEOUT, TIMEOUT_30_SECONDS)
    }

    fun setAutoLockTimeout(seconds: Int) {
        getPrefs().edit().putInt(KEY_AUTO_LOCK_TIMEOUT, seconds).apply()
    }

    fun normalizeAnswer(answer: String): String {
        return answer.trim().lowercase().replace("\\s+".toRegex(), "")
    }

    /**
     * Initializes the vault:
     * Generates a 256-bit random Vault Key and wraps it with the master password.
     * Optionally sets up a security question wrapper.
     */
    fun setupMasterPassword(
        password: String,
        question: String? = null,
        answer: String? = null
    ): SecretKey {
        val vaultKey = CryptoManager.generateRandomKey()

        val salt = CryptoManager.generateSalt()
        val derivedKey = CryptoManager.deriveKey(password.toCharArray(), salt)
        val verifier = CryptoManager.createVerificationHash(password.toCharArray(), salt)

        val (encVaultKey, iv) = CryptoManager.encryptBytes(vaultKey.encoded, derivedKey)

        val editor = getPrefs().edit()
            .putBoolean(KEY_IS_INITIALIZED, true)
            .putString(KEY_MASTER_SALT, CryptoManager.toBase64(salt))
            .putString(KEY_MASTER_VERIFIER, verifier)
            .putString(KEY_ENCRYPTED_VAULT_KEY, encVaultKey)
            .putString(KEY_VAULT_KEY_IV, iv)

        if (!question.isNullOrBlank() && !answer.isNullOrBlank()) {
            val qSalt = CryptoManager.generateSalt()
            val normalized = normalizeAnswer(answer)
            val qKey = CryptoManager.deriveKey(normalized.toCharArray(), qSalt)
            val (qCipher, qIv) = CryptoManager.encryptBytes(vaultKey.encoded, qKey)

            editor.putString(KEY_QUESTION_TEXT, question.trim())
                .putString(KEY_QUESTION_SALT, CryptoManager.toBase64(qSalt))
                .putString(KEY_QUESTION_CIPHER, qCipher)
                .putString(KEY_QUESTION_IV, qIv)
        }

        editor.apply()

        unlock(vaultKey)
        return vaultKey
    }

    /**
     * Unlocks using the Master Password by unwrapping the Vault Key.
     */
    fun unlockWithPassword(password: String): Boolean {
        val saltBase64 = getPrefs().getString(KEY_MASTER_SALT, null) ?: return false
        val savedVerifier = getPrefs().getString(KEY_MASTER_VERIFIER, null) ?: return false

        val salt = CryptoManager.fromBase64(saltBase64)
        val computedVerifier = CryptoManager.createVerificationHash(password.toCharArray(), salt)

        if (computedVerifier != savedVerifier) {
            return false
        }

        val derivedKey = CryptoManager.deriveKey(password.toCharArray(), salt)
        val encVaultKey = getPrefs().getString(KEY_ENCRYPTED_VAULT_KEY, null)
        val iv = getPrefs().getString(KEY_VAULT_KEY_IV, null)

        val vaultKey: SecretKey = if (encVaultKey != null && iv != null) {
            try {
                val keyBytes = CryptoManager.decryptBytes(encVaultKey, iv, derivedKey)
                SecretKeySpec(keyBytes, "AES")
            } catch (_: Exception) {
                return false
            }
        } else {
            // Backward compatibility: use derived key directly as vault key and wrap it
            val (newEnc, newIv) = CryptoManager.encryptBytes(derivedKey.encoded, derivedKey)
            getPrefs().edit()
                .putString(KEY_ENCRYPTED_VAULT_KEY, newEnc)
                .putString(KEY_VAULT_KEY_IV, newIv)
                .apply()
            derivedKey
        }

        unlock(vaultKey)
        if (isBiometricEnabled()) {
            saveBiometricKey(vaultKey.encoded)
        }
        return true
    }

    /**
     * Unlocks using Biometrics by decrypting the Vault Key with Android Keystore.
     */
    fun unlockWithBiometric(): Boolean {
        if (!isBiometricEnabled()) return false
        val cipherBase64 = getPrefs().getString(KEY_BIOMETRIC_CIPHER, null) ?: return false
        val ivBase64 = getPrefs().getString(KEY_BIOMETRIC_IV, null) ?: return false

        return try {
            val keyBytes = KeyStoreManager.decryptMasterKey(cipherBase64, ivBase64)
            val key = SecretKeySpec(keyBytes, "AES")
            unlock(key)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun canUseBiometricUnlock(): Boolean {
        return isVaultSetup() && isBiometricEnabled() &&
                getPrefs().contains(KEY_BIOMETRIC_CIPHER) &&
                getPrefs().contains(KEY_BIOMETRIC_IV)
    }

    private fun saveBiometricKey(keyBytes: ByteArray) {
        try {
            val (cipherBase64, ivBase64) = KeyStoreManager.encryptMasterKey(keyBytes)
            getPrefs().edit()
                .putString(KEY_BIOMETRIC_CIPHER, cipherBase64)
                .putString(KEY_BIOMETRIC_IV, ivBase64)
                .apply()
        } catch (_: Exception) {}
    }

    // --- Security Question Methods ---

    fun hasSecurityQuestion(): Boolean {
        return getPrefs().contains(KEY_QUESTION_TEXT) &&
                getPrefs().contains(KEY_QUESTION_CIPHER) &&
                getPrefs().contains(KEY_QUESTION_SALT)
    }

    fun getSecurityQuestion(): String {
        return getPrefs().getString(KEY_QUESTION_TEXT, "") ?: ""
    }

    fun setSecurityQuestion(question: String, answer: String): Boolean {
        val vaultKey = activeMasterKey ?: return false
        if (question.isBlank() || answer.isBlank()) return false

        val qSalt = CryptoManager.generateSalt()
        val normalized = normalizeAnswer(answer)
        val qKey = CryptoManager.deriveKey(normalized.toCharArray(), qSalt)
        val (qCipher, qIv) = CryptoManager.encryptBytes(vaultKey.encoded, qKey)

        getPrefs().edit()
            .putString(KEY_QUESTION_TEXT, question.trim())
            .putString(KEY_QUESTION_SALT, CryptoManager.toBase64(qSalt))
            .putString(KEY_QUESTION_CIPHER, qCipher)
            .putString(KEY_QUESTION_IV, qIv)
            .apply()

        return true
    }

    /**
     * Recovers the Vault Key using the security question answer,
     * resets the Master Password to [newPassword], and unlocks the vault.
     */
    fun recoverWithSecurityQuestion(answer: String, newPassword: String): Boolean {
        if (!hasSecurityQuestion()) return false
        if (newPassword.length < 6) return false

        val qSaltBase64 = getPrefs().getString(KEY_QUESTION_SALT, null) ?: return false
        val qCipher = getPrefs().getString(KEY_QUESTION_CIPHER, null) ?: return false
        val qIv = getPrefs().getString(KEY_QUESTION_IV, null) ?: return false

        val qSalt = CryptoManager.fromBase64(qSaltBase64)
        val normalized = normalizeAnswer(answer)
        val qKey = CryptoManager.deriveKey(normalized.toCharArray(), qSalt)

        val vaultKeyBytes = try {
            CryptoManager.decryptBytes(qCipher, qIv, qKey)
        } catch (_: Exception) {
            // Decryption failed: wrong answer or altered data
            return false
        }

        val vaultKey = SecretKeySpec(vaultKeyBytes, "AES")

        // Re-wrap Vault Key with the new master password
        wrapAndSaveWithNewPassword(vaultKey, newPassword)

        unlock(vaultKey)
        return true
    }

    /**
     * Changes Master Password when already unlocked (e.g. from Settings or after Biometric unlock).
     */
    fun changeMasterPasswordDirectly(newPassword: String): Boolean {
        val vaultKey = activeMasterKey ?: return false
        if (newPassword.length < 6) return false

        wrapAndSaveWithNewPassword(vaultKey, newPassword)
        return true
    }

    private fun wrapAndSaveWithNewPassword(vaultKey: SecretKey, newPassword: String) {
        val newSalt = CryptoManager.generateSalt()
        val newDerivedKey = CryptoManager.deriveKey(newPassword.toCharArray(), newSalt)
        val newVerifier = CryptoManager.createVerificationHash(newPassword.toCharArray(), newSalt)

        val (encVaultKey, iv) = CryptoManager.encryptBytes(vaultKey.encoded, newDerivedKey)

        getPrefs().edit()
            .putString(KEY_MASTER_SALT, CryptoManager.toBase64(newSalt))
            .putString(KEY_MASTER_VERIFIER, newVerifier)
            .putString(KEY_ENCRYPTED_VAULT_KEY, encVaultKey)
            .putString(KEY_VAULT_KEY_IV, iv)
            .apply()

        if (isBiometricEnabled()) {
            saveBiometricKey(vaultKey.encoded)
        }
    }

    fun unlock(key: SecretKey) {
        activeMasterKey = key
        _isLocked.value = false
        backgroundTimestamp = 0L
    }

    fun lock() {
        activeMasterKey = null
        _isLocked.value = true
        backgroundTimestamp = 0L
        AppDatabase.closeInstance()
    }

    fun getActiveKey(): SecretKey? {
        return activeMasterKey
    }

    fun onAppBackgrounded() {
        if (!_isLocked.value) {
            backgroundTimestamp = System.currentTimeMillis()
            val timeout = getAutoLockTimeout()
            if (timeout == TIMEOUT_IMMEDIATE) {
                lock()
            }
        }
    }

    fun onAppForegrounded() {
        if (!_isLocked.value && backgroundTimestamp > 0L) {
            val elapsed = (System.currentTimeMillis() - backgroundTimestamp) / 1000L
            val timeout = getAutoLockTimeout()
            if (timeout != TIMEOUT_IMMEDIATE && elapsed >= timeout) {
                lock()
            }
            backgroundTimestamp = 0L
        }
    }

    /**
     * Emergency data wipe: cleans all encrypted database files and preferences.
     */
    fun resetAllVaultData(context: Context) {
        lock()
        getPrefs().edit().clear().apply()
        KeyStoreManager.clearBiometricKey()
        context.deleteDatabase("keyfortress_vault.db")
    }
}
