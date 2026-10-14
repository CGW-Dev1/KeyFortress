package com.keyfortress.app.ui.lock

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keyfortress.app.security.AppLockManager
import com.keyfortress.app.security.BiometricAuthHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LockUiState(
    val isSetupMode: Boolean = false,
    val canUseBiometric: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val errorMessage: String = "",
    val unlockSuccess: Boolean = false
)

class LockViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        checkLockState()
    }

    fun checkLockState() {
        val isSetup = !AppLockManager.isVaultSetup()
        val biometricAvailable = BiometricAuthHelper.isBiometricAvailable(getApplication())
        val canBio = AppLockManager.canUseBiometricUnlock()

        _uiState.value = _uiState.value.copy(
            isSetupMode = isSetup,
            canUseBiometric = canBio,
            isBiometricAvailable = biometricAvailable,
            errorMessage = ""
        )
    }

    fun setupMasterPassword(password: String, enableBiometric: Boolean) {
        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "主密码长度至少为 6 位")
            return
        }

        viewModelScope.launch {
            try {
                AppLockManager.setupMasterPassword(password)
                if (enableBiometric && _uiState.value.isBiometricAvailable) {
                    AppLockManager.setBiometricEnabled(true)
                }
                _uiState.value = _uiState.value.copy(unlockSuccess = true, errorMessage = "")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "设置主密码失败: ${e.localizedMessage}")
            }
        }
    }

    fun unlockWithPassword(password: String) {
        if (password.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "请输入主密码")
            return
        }

        viewModelScope.launch {
            val success = AppLockManager.unlockWithPassword(password)
            if (success) {
                _uiState.value = _uiState.value.copy(unlockSuccess = true, errorMessage = "")
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "主密码错误，请重试")
            }
        }
    }

    fun triggerBiometricUnlock(activity: FragmentActivity) {
        if (!_uiState.value.canUseBiometric) return

        BiometricAuthHelper.authenticate(
            activity = activity,
            onSuccess = {
                val success = AppLockManager.unlockWithBiometric()
                if (success) {
                    _uiState.value = _uiState.value.copy(unlockSuccess = true, errorMessage = "")
                } else {
                    _uiState.value = _uiState.value.copy(errorMessage = "生物识别解密失败，请使用主密码")
                }
            },
            onError = { _, errString ->
                // User may have canceled or selected negative button
            },
            onFailed = {
                _uiState.value = _uiState.value.copy(errorMessage = "指纹/面部识别未通过")
            }
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = "")
    }
}
