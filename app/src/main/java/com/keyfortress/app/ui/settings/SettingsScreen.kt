package com.keyfortress.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keyfortress.app.security.AppLockManager
import com.keyfortress.app.ui.components.ClipboardImportDialog
import com.keyfortress.app.ui.components.ExportVaultDialog
import com.keyfortress.app.ui.components.ImportOptionsDialog
import com.keyfortress.app.ui.components.PasswordInputDialog
import com.keyfortress.app.ui.components.SetSecurityQuestionDialog
import com.keyfortress.app.ui.components.SmartTextImportDialog
import kotlinx.coroutines.flow.collectLatest
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.message.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showTimeoutDialog by remember { mutableStateOf(false) }
    var showSecurityQuestionDialog by remember { mutableStateOf(false) }

    // Export & Import Dialogs
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportOptionsDialog by remember { mutableStateOf(false) }
    var showClipboardImportDialog by remember { mutableStateOf(false) }
    var showSmartImportDialog by remember { mutableStateOf(false) }

    // Local file import state
    var selectedLocalFile by remember { mutableStateOf<File?>(null) }
    var showLocalFilePasswordDialog by remember { mutableStateOf(false) }

    // SAF File Picker
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showUriPasswordDialog by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        AppLockManager.isBypassingAutoLock = false
        if (uri != null) {
            pendingImportUri = uri
            showUriPasswordDialog = true
        }
    }

    if (showSecurityQuestionDialog) {
        SetSecurityQuestionDialog(
            initialQuestion = uiState.securityQuestion,
            onConfirm = { q, a ->
                viewModel.setSecurityQuestion(q, a)
                showSecurityQuestionDialog = false
            },
            onDismiss = { showSecurityQuestionDialog = false }
        )
    }

    if (showExportDialog) {
        ExportVaultDialog(
            onConfirmExport = { pwd, share ->
                viewModel.exportBackupDirectly(pwd, share)
                showExportDialog = false
            },
            onDismiss = { showExportDialog = false }
        )
    }

    if (showImportOptionsDialog) {
        ImportOptionsDialog(
            localBackups = viewModel.getLocalBackups(),
            onSelectFilePicker = {
                showImportOptionsDialog = false
                try {
                    AppLockManager.isBypassingAutoLock = true
                    importLauncher.launch(arrayOf("*/*"))
                } catch (e: Exception) {
                    AppLockManager.isBypassingAutoLock = false
                    // Fallback to clipboard if system document picker is unavailable
                    showClipboardImportDialog = true
                }
            },
            onSelectLocalFile = { file ->
                showImportOptionsDialog = false
                selectedLocalFile = file
                showLocalFilePasswordDialog = true
            },
            onSelectClipboard = {
                showImportOptionsDialog = false
                showClipboardImportDialog = true
            },
            onDismiss = { showImportOptionsDialog = false }
        )
    }

    if (showLocalFilePasswordDialog && selectedLocalFile != null) {
        PasswordInputDialog(
            title = "恢复本地备份",
            message = "正在恢复文件: ${selectedLocalFile!!.name}\n请输入导出时设置的备份解密密码：",
            confirmText = "确认恢复",
            onConfirm = { pwd ->
                try {
                    val content = selectedLocalFile!!.readText(Charsets.UTF_8)
                    viewModel.importBackupFromText(content, pwd)
                } catch (e: Exception) {
                    // Handled in viewModel
                }
                selectedLocalFile = null
                showLocalFilePasswordDialog = false
            },
            onDismiss = {
                selectedLocalFile = null
                showLocalFilePasswordDialog = false
            }
        )
    }

    if (showUriPasswordDialog && pendingImportUri != null) {
        PasswordInputDialog(
            title = "输入备份解密密码",
            message = "请输入当初导出该备份时设置的加密密码：",
            confirmText = "确认恢复",
            onConfirm = { pwd ->
                viewModel.importBackupFromUri(pendingImportUri!!, pwd)
                pendingImportUri = null
                showUriPasswordDialog = false
            },
            onDismiss = {
                pendingImportUri = null
                showUriPasswordDialog = false
            }
        )
    }

    if (showClipboardImportDialog) {
        ClipboardImportDialog(
            onConfirm = { text, pwd ->
                viewModel.importBackupFromText(text, pwd)
                showClipboardImportDialog = false
            },
            onDismiss = { showClipboardImportDialog = false }
        )
    }

    if (showSmartImportDialog) {
        SmartTextImportDialog(
            onDismiss = { showSmartImportDialog = false },
            onConfirmImport = { items ->
                viewModel.importSmartAccounts(items) {
                    showSmartImportDialog = false
                }
            }
        )
    }

    if (showChangePasswordDialog) {
        ChangeMasterPasswordDialog(
            onConfirm = { old, new ->
                viewModel.changeMasterPassword(old, new) { success, _ ->
                    if (success) {
                        showChangePasswordDialog = false
                    }
                }
            },
            onDismiss = { showChangePasswordDialog = false }
        )
    }

    if (showTimeoutDialog) {
        AutoLockTimeoutDialog(
            currentTimeout = uiState.autoLockTimeout,
            onSelect = { timeout ->
                viewModel.setAutoLockTimeout(timeout)
                showTimeoutDialog = false
            },
            onDismiss = { showTimeoutDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置与安全") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Section 1: Security & Unlock
            SectionHeader(title = "安全认证")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingActionItem(
                        icon = Icons.Default.Key,
                        title = "修改主密码",
                        subtitle = "更换派生密钥的主密码",
                        onClick = { showChangePasswordDialog = true }
                    )

                    if (uiState.isBiometricHardwareAvailable) {
                        SettingSwitchItem(
                            icon = Icons.Default.Fingerprint,
                            title = "生物识别解锁",
                            subtitle = "支持指纹与面部识别快速解锁",
                            checked = uiState.biometricEnabled,
                            onCheckedChange = { viewModel.setBiometricEnabled(it) }
                        )
                    }

                    val timeoutLabel = when (uiState.autoLockTimeout) {
                        AppLockManager.TIMEOUT_IMMEDIATE -> "立即锁定"
                        AppLockManager.TIMEOUT_30_SECONDS -> "30 秒"
                        AppLockManager.TIMEOUT_1_MINUTE -> "1 分钟"
                        AppLockManager.TIMEOUT_5_MINUTES -> "5 分钟"
                        else -> "${uiState.autoLockTimeout} 秒"
                    }
                    SettingActionItem(
                        icon = Icons.Default.Timer,
                        title = "后台自动锁定",
                        subtitle = "退至后台后延迟锁定: $timeoutLabel",
                        onClick = { showTimeoutDialog = true }
                    )

                    SettingActionItem(
                        icon = Icons.Default.QuestionAnswer,
                        title = "安全密保问题",
                        subtitle = if (uiState.hasSecurityQuestion) {
                            "已设置: ${uiState.securityQuestion} (点击修改)"
                        } else {
                            "未设置 (点击设置密保防遗忘)"
                        },
                        onClick = { showSecurityQuestionDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: Data Management
            SectionHeader(title = "数据备份与恢复")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingActionItem(
                        icon = Icons.Default.FileUpload,
                        title = "导出加密备份文件",
                        subtitle = "安全保存至「下载」目录或调起系统分享",
                        onClick = { showExportDialog = true }
                    )

                    SettingActionItem(
                        icon = Icons.Default.FileDownload,
                        title = "从本地文件恢复数据",
                        subtitle = "从手机存储、下载目录或剪贴板导入",
                        onClick = { showImportOptionsDialog = true }
                    )

                    SettingActionItem(
                        icon = Icons.Default.AutoAwesome,
                        title = "智能文本导入",
                        subtitle = "批量粘贴任意文本，自动识别账号密码并分类",
                        onClick = { showSmartImportDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 3: Security & Privacy Principles
            SectionHeader(title = "隐私保护与安全声明")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SecurityBullet(
                        title = "完全离线运行",
                        description = "应用未申请网络权限 (INTERNET)，绝无任何云端上传可能。"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SecurityBullet(
                        title = "零明文落盘保护",
                        description = "基于 PBKDF2 + AES-256-GCM + SQLCipher 全库强加密，敏感数据全流程密文存储。"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SecurityBullet(
                        title = "防截屏与多任务防窥",
                        description = "全局启用 FLAG_SECURE 安全标记，阻止录屏截屏，多任务切换自动变黑防窥。"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SecurityBullet(
                        title = "剪贴板隐私自动清理",
                        description = "复制敏感密码 30 秒后自动清空剪贴板，并在 Android 13+ 隐藏系统复制弹窗预览。"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Version Info
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "KeyFortress v1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "安全·离线·纯粹的本地密码管家",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SecurityBullet(title: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = "•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChangeMasterPasswordDialog(
    onConfirm: (oldPassword: String, newPassword: String) -> Unit,
    onDismiss: () -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改主密码") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it; error = "" },
                    label = { Text("原主密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; error = "" },
                    label = { Text("新主密码 (至少 6 位)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; error = "" },
                    label = { Text("确认新主密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (error.isNotEmpty()) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        oldPassword.isEmpty() -> error = "请输入原主密码"
                        newPassword.length < 6 -> error = "新主密码长度至少 6 位"
                        newPassword != confirmPassword -> error = "两次输入的新密码不一致"
                        else -> onConfirm(oldPassword, newPassword)
                    }
                }
            ) {
                Text("修改")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun AutoLockTimeoutDialog(
    currentTimeout: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        Pair("立即锁定", AppLockManager.TIMEOUT_IMMEDIATE),
        Pair("30 秒", AppLockManager.TIMEOUT_30_SECONDS),
        Pair("1 分钟", AppLockManager.TIMEOUT_1_MINUTE),
        Pair("5 分钟", AppLockManager.TIMEOUT_5_MINUTES)
    )

    var selected by remember { mutableIntStateOf(currentTimeout) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("后台锁定延迟") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                options.forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = value }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == value,
                            onClick = { selected = value }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSelect(selected) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
