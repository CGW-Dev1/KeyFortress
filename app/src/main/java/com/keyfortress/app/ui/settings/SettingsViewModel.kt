package com.keyfortress.app.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keyfortress.app.data.repository.AccountItem
import com.keyfortress.app.data.repository.AccountRepository
import com.keyfortress.app.security.AppLockManager
import com.keyfortress.app.security.BiometricAuthHelper
import com.keyfortress.app.data.entity.CategoryEntity
import com.keyfortress.app.util.BackupFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStreamWriter

data class SettingsUiState(
    val biometricEnabled: Boolean = false,
    val isBiometricHardwareAvailable: Boolean = false,
    val autoLockTimeout: Int = AppLockManager.TIMEOUT_30_SECONDS,
    val hasSecurityQuestion: Boolean = false,
    val securityQuestion: String = "",
    val isProcessing: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AccountRepository(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message.asSharedFlow()

    val categories: StateFlow<List<CategoryEntity>> = repository.getCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadSettings()
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            val success = repository.addCategory(name)
            if (success) {
                _message.emit("分类「$name」创建成功")
            } else {
                _message.emit("创建分类失败（可能已存在同名分类）")
            }
        }
    }

    fun updateCategory(id: Long, oldName: String, newName: String) {
        viewModelScope.launch {
            val success = repository.updateCategory(id, oldName, newName)
            if (success) {
                _message.emit("分类修改成功")
            } else {
                _message.emit("修改分类失败（可能已存在同名分类）")
            }
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            val success = repository.deleteCategory(id)
            if (success) {
                _message.emit("分类已删除，原数据已移至「其它」")
            } else {
                _message.emit("删除分类失败")
            }
        }
    }

    fun loadSettings() {
        val bioEnabled = AppLockManager.isBiometricEnabled()
        val bioAvailable = BiometricAuthHelper.isBiometricAvailable(getApplication())
        val timeout = AppLockManager.getAutoLockTimeout()
        val hasQuestion = AppLockManager.hasSecurityQuestion()
        val questionText = AppLockManager.getSecurityQuestion()

        _uiState.value = SettingsUiState(
            biometricEnabled = bioEnabled,
            isBiometricHardwareAvailable = bioAvailable,
            autoLockTimeout = timeout,
            hasSecurityQuestion = hasQuestion,
            securityQuestion = questionText
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

    fun setSecurityQuestion(question: String, answer: String) {
        val success = AppLockManager.setSecurityQuestion(question, answer)
        if (success) {
            _uiState.value = _uiState.value.copy(
                hasSecurityQuestion = true,
                securityQuestion = question
            )
            viewModelScope.launch {
                _message.emit("密保问题设置成功！")
            }
        } else {
            viewModelScope.launch {
                _message.emit("密保问题设置失败，请先解锁保险库")
            }
        }
    }

    fun changeMasterPassword(oldPassword: String, newPassword: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val verified = AppLockManager.unlockWithPassword(oldPassword)
            if (!verified) {
                onComplete(false, "原主密码错误")
                return@launch
            }

            try {
                val success = AppLockManager.changeMasterPasswordDirectly(newPassword)
                if (success) {
                    onComplete(true, "主密码修改成功")
                } else {
                    onComplete(false, "修改失败，密码长度至少 6 位")
                }
            } catch (e: Exception) {
                onComplete(false, "修改失败: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Exports backup directly to Downloads/KeyFortress and optionally launches system share sheet.
     * 100% crash-free on all devices (does not depend on DocumentsUI).
     */
    fun exportBackupDirectly(backupPassword: String, shareAfterExport: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            try {
                val encryptedData = repository.exportEncryptedVault(backupPassword)
                val fileName = "KeyFortress_Backup_${System.currentTimeMillis()}.vault"

                val (saved, msg) = withContext(Dispatchers.IO) {
                    BackupFileManager.saveToDownloads(getApplication(), fileName, encryptedData)
                }

                if (shareAfterExport) {
                    AppLockManager.isBypassingAutoLock = true
                    BackupFileManager.shareBackup(getApplication(), fileName, encryptedData)
                }

                _uiState.value = _uiState.value.copy(isProcessing = false)
                _message.emit(if (saved) msg else "导出备份失败")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isProcessing = false)
                _message.emit("导出失败: ${e.localizedMessage}")
            }
        }
    }

    fun exportBackupToUri(uri: Uri, backupPassword: String) {
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

    fun importBackupFromUri(uri: Uri, backupPassword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            try {
                val content = withContext(Dispatchers.IO) {
                    BackupFileManager.readTextFromUri(getApplication(), uri)
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

    fun importBackupFromText(content: String, backupPassword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            try {
                val count = repository.importEncryptedVault(content.trim(), backupPassword)
                _uiState.value = _uiState.value.copy(isProcessing = false)
                _message.emit("成功恢复 $count 条密码记录！")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isProcessing = false)
                _message.emit("导入恢复失败 (密码或数据格式错误): ${e.localizedMessage}")
            }
        }
    }

    fun getLocalBackups(): List<File> {
        return BackupFileManager.listLocalBackups(getApplication())
    }

    fun importSmartAccounts(items: List<AccountItem>, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            try {
                val count = repository.saveAccountsBatch(items)
                _uiState.value = _uiState.value.copy(isProcessing = false)
                _message.emit("成功智能导入 $count 条账号密码！")
                onComplete(count)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isProcessing = false)
                _message.emit("智能导入失败: ${e.localizedMessage}")
            }
        }
    }
}
