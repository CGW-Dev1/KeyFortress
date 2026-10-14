package com.keyfortress.app.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keyfortress.app.data.repository.AccountRepository
import com.keyfortress.app.security.AppLockManager
import com.keyfortress.app.security.BiometricAuthHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

data class SettingsUiState(
    val biometricEnabled: Boolean = false,
    val isBiometricHardwareAvailable: Boolean = false,
    val autoLockTimeout: Int = AppLockManager.TIMEOUT_30_SECONDS,
    val isProcessing: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AccountRepository(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message.asSharedFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        val bioEnabled = AppLockManager.isBiometricEnabled()
        val bioAvailable = BiometricAuthHelper.isBiometricAvailable(getApplication())
        val timeout = AppLockManager.getAutoLockTimeout()

        _uiState.value = SettingsUiState(
            biometricEnabled = bioEnabled,
            isBiometricHardwareAvailable = bioAvailable,
            autoLockTimeout = timeout
        )
    }

    fun setBiometricEnabled(enabled: Boolean) {
        AppLockManager.setBiometricEnabled(enabled)
        _uiState.value = _uiState.value.copy(biometricEnabled = enabled)
        viewModelScope.launch {
            _message.emit(if (enabled) "生物识别解锁已开启" else "生物识别解锁已关闭")
        }
    }

    fun setAutoLockTimeout(seconds: Int) {
        AppLockManager.setAutoLockTimeout(seconds)
        _uiState.value = _uiState.value.copy(autoLockTimeout = seconds)
        viewModelScope.launch {
            val label = when (seconds) {
                AppLockManager.TIMEOUT_IMMEDIATE -> "立即锁定"
                AppLockManager.TIMEOUT_30_SECONDS -> "30 秒"
                AppLockManager.TIMEOUT_1_MINUTE -> "1 分钟"
                AppLockManager.TIMEOUT_5_MINUTES -> "5 分钟"
                else -> "$seconds 秒"
            }
            _message.emit("后台锁定时间已设为: $label")
        }
    }

    fun changeMasterPassword(oldPassword: String, newPassword: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            // First verify old password
            val verified = AppLockManager.unlockWithPassword(oldPassword)
            if (!verified) {
                onComplete(false, "原主密码错误")
                return@launch
            }

            try {
                // Re-setup with new password
                AppLockManager.setupMasterPassword(newPassword)
                if (_uiState.value.biometricEnabled) {
                    AppLockManager.setBiometricEnabled(true)
                }
                onComplete(true, "主密码修改成功")
            } catch (e: Exception) {
                onComplete(false, "修改失败: ${e.localizedMessage}")
            }
        }
    }

    fun exportBackup(uri: Uri, backupPassword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            try {
                val encryptedData = repository.exportEncryptedVault(backupPassword)
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { os ->
                        OutputStreamWriter(os, Charsets.UTF_8).use { writer ->
                            writer.write(encryptedData)
                        }
                    }
                }
                _uiState.value = _uiState.value.copy(isProcessing = false)
                _message.emit("加密备份成功导出！")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isProcessing = false)
                _message.emit("导出备份失败: ${e.localizedMessage}")
            }
        }
    }

    fun importBackup(uri: Uri, backupPassword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            try {
                val content = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { isStream ->
                        BufferedReader(InputStreamReader(isStream, Charsets.UTF_8)).use { reader ->
                            reader.readText()
                        }
                    } ?: throw IllegalStateException("无法读取选中的备份文件")
                }

                val count = repository.importEncryptedVault(content, backupPassword)
                _uiState.value = _uiState.value.copy(isProcessing = false)
                _message.emit("成功恢复 $count 条密码记录！")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isProcessing = false)
                _message.emit("导入恢复失败 (密码可能错误): ${e.localizedMessage}")
            }
        }
    }
}
