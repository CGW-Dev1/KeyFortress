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
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_BIOMETRIC_CIPHER = "biometric_cipher"
    private const val KEY_BIOMETRIC_IV = "biometric_iv"
    private const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout"

    const val TIMEOUT_IMMEDIATE = 0
    const val TIMEOUT_30_SECONDS = 30
    const val TIMEOUT_1_MINUTE = 60
    const val TIMEOUT_5_MINUTES = 300

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

    /**
     * Initializes a brand new master password for the vault.
     */
    fun setupMasterPassword(password: String): SecretKey {
        val salt = CryptoManager.generateSalt()
        val key = CryptoManager.deriveKey(password.toCharArray(), salt)
        val verifier = CryptoManager.createVerificationHash(password.toCharArray(), salt)

        getPrefs().edit()
            .putBoolean(KEY_IS_INITIALIZED, true)
            .putString(KEY_MASTER_SALT, CryptoManager.toBase64(salt))
            .putString(KEY_MASTER_VERIFIER, verifier)
            .apply()

        unlock(key)
        return key
    }

    /**
     * Verifies and unlocks using the master password.
     */
    fun unlockWithPassword(password: String): Boolean {
        val saltBase64 = getPrefs().getString(KEY_MASTER_SALT, null) ?: return false
        val savedVerifier = getPrefs().getString(KEY_MASTER_VERIFIER, null) ?: return false

        val salt = CryptoManager.fromBase64(saltBase64)
        val computedVerifier = CryptoManager.createVerificationHash(password.toCharArray(), salt)

        if (computedVerifier == savedVerifier) {
            val key = CryptoManager.deriveKey(password.toCharArray(), salt)
            unlock(key)
            if (isBiometricEnabled()) {
                saveBiometricKey(key.encoded)
            }
            return true
        }
        return false
    }

    /**
     * Unlocks using Biometric authentication by decrypting the Keystore-protected master key.
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
}
