package com.keyfortress.app.ui.lock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.keyfortress.app.security.AppLockManager
import com.keyfortress.app.ui.components.ConfirmDialog
import com.keyfortress.app.ui.components.PasswordStrengthBar
import com.keyfortress.app.ui.components.SecurityQuestionRecoveryDialog

@Composable
fun LockScreen(
    viewModel: LockViewModel,
    onUnlocked: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.unlockSuccess) {
        if (uiState.unlockSuccess) {
            onUnlocked()
        }
    }

    // Auto-trigger biometric prompt when screen loads if available
    LaunchedEffect(uiState.canUseBiometric) {
        if (uiState.canUseBiometric && !uiState.isSetupMode) {
            (context as? FragmentActivity)?.let { activity ->
                viewModel.triggerBiometricUnlock(activity)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (uiState.isSetupMode) {
            SetupVaultView(
                uiState = uiState,
                onSetup = { pwd, bio, q, a ->
                    viewModel.setupMasterPassword(pwd, bio, q, a)
                },
                onClearError = { viewModel.clearError() }
            )
        } else {
            UnlockVaultView(
                uiState = uiState,
                onPasswordUnlock = { pwd ->
                    focusManager.clearFocus()
                    viewModel.unlockWithPassword(pwd)
                },
                onBiometricClick = {
                    (context as? FragmentActivity)?.let { activity ->
                        viewModel.triggerBiometricUnlock(activity)
                    }
                },
                onRecoverWithQuestion = { answer, newPassword, onResult ->
                    viewModel.recoverWithSecurityQuestion(answer, newPassword, onResult)
                },
                onResetVault = {
                    viewModel.resetVaultData()
                },
                onClearError = { viewModel.clearError() }
            )
        }
    }
}

@Composable
private fun SetupVaultView(
    uiState: LockUiState,
    onSetup: (password: String, enableBiometric: Boolean, question: String?, answer: String?) -> Unit,
    onClearError: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var enableBiometric by remember { mutableStateOf(uiState.isBiometricAvailable) }

    // Security Question state
    var enableQuestion by remember { mutableStateOf(true) }
    var selectedQuestion by remember { mutableStateOf(AppLockManager.PRESET_QUESTIONS[0]) }
    var isCustomQuestion by remember { mutableStateOf(false) }
    var customQuestion by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var answerVisible by remember { mutableStateOf(false) }
    var questionDropdownExpanded by remember { mutableStateOf(false) }

    var localError by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Shield Icon
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "欢迎使用 KeyFortress",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "首次使用请设置主密码。主密码用于派生 256 位加密密钥，零明文存储。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Master Password Input
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                localError = ""
                onClearError()
            },
            label = { Text("设置主密码 (至少 6 位)") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        PasswordStrengthBar(password = password)

        Spacer(modifier = Modifier.height(10.dp))

        // Confirm Password Input
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                localError = ""
                onClearError()
            },
            label = { Text("确认主密码") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        if (uiState.isBiometricAvailable) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { enableBiometric = !enableBiometric }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = enableBiometric,
                    onCheckedChange = { enableBiometric = it }
                )
                Text(
                    text = "开启指纹 / 面部识别快捷解锁",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Security Question Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { enableQuestion = !enableQuestion },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = enableQuestion,
                        onCheckedChange = { enableQuestion = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "设置安全密保问题 (强烈推荐)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "用于忘记主密码时安全找回并重置密码",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (enableQuestion) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Question Selection
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (isCustomQuestion) "自定义问题..." else selectedQuestion,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("选择密保问题") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.clickable { questionDropdownExpanded = true }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { questionDropdownExpanded = true },
                            shape = RoundedCornerShape(10.dp)
                        )

                        DropdownMenu(
                            expanded = questionDropdownExpanded,
                            onDismissRequest = { questionDropdownExpanded = false }
                        ) {
                            AppLockManager.PRESET_QUESTIONS.forEach { q ->
                                DropdownMenuItem(
                                    text = { Text(q) },
                                    onClick = {
                                        selectedQuestion = q
                                        isCustomQuestion = false
                                        questionDropdownExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("自定义问题...") },
                                onClick = {
                                    isCustomQuestion = true
                                    questionDropdownExpanded = false
                                }
                            )
                        }
                    }

                    if (isCustomQuestion) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customQuestion,
                            onValueChange = { customQuestion = it; localError = "" },
                            label = { Text("输入自定义密保问题") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Answer
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { answer = it; localError = "" },
                        label = { Text("输入密保答案") },
                        singleLine = true,
                        visualTransformation = if (answerVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { answerVisible = !answerVisible }) {
                                Icon(
                                    imageVector = if (answerVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        val displayError = localError.ifEmpty { uiState.errorMessage }
        AnimatedVisibility(visible = displayError.isNotEmpty()) {
            Text(
                text = displayError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val finalQ = if (enableQuestion) {
                    if (isCustomQuestion) customQuestion.trim() else selectedQuestion
                } else null
                val finalA = if (enableQuestion) answer.trim() else null

                when {
                    password.length < 6 -> localError = "主密码长度至少为 6 位"
                    password != confirmPassword -> localError = "两次输入的密码不一致"
                    enableQuestion && finalQ.isNullOrBlank() -> localError = "请选择或填写密保问题"
                    enableQuestion && finalA.isNullOrBlank() -> localError = "请填写密保答案"
                    else -> onSetup(password, enableBiometric, finalQ, finalA)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("创建加密保险库", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun UnlockVaultView(
    uiState: LockUiState,
    onPasswordUnlock: (String) -> Unit,
    onBiometricClick: () -> Unit,
    onRecoverWithQuestion: (answer: String, newPassword: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onResetVault: () -> Unit,
    onClearError: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showPasswordInput by remember { mutableStateOf(!uiState.canUseBiometric) }

    var showRecoveryDialog by remember { mutableStateOf(false) }
    var recoveryError by remember { mutableStateOf("") }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    if (showRecoveryDialog) {
        SecurityQuestionRecoveryDialog(
            questionText = uiState.securityQuestionText,
            errorMessage = recoveryError,
            onConfirm = { ans, newPwd ->
                onRecoverWithQuestion(ans, newPwd) { success, msg ->
                    if (success) {
                        showRecoveryDialog = false
                        recoveryError = ""
                    } else {
                        recoveryError = msg
                    }
                }
            },
            onDismiss = {
                showRecoveryDialog = false
                recoveryError = ""
            }
        )
    }

    if (showResetConfirmDialog) {
        ConfirmDialog(
            title = "清空并重置保险库",
            message = "⚠️ 警告：重置将彻底删除本地所有已存密码和密钥，此操作无法撤销！确定要清空重置吗？",
            confirmText = "清空重置",
            isDestructive = true,
            onConfirm = {
                onResetVault()
                showResetConfirmDialog = false
            },
            onDismiss = { showResetConfirmDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Shield Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(46.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "KeyFortress",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "安全·离线·纯粹",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(36.dp))

        if (uiState.canUseBiometric && !showPasswordInput) {
            // Biometric Unlock Mode
            Card(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBiometricClick),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "指纹解锁",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "点击进行指纹解锁",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(28.dp))

            TextButton(onClick = { showPasswordInput = true }) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("使用主密码解锁")
            }
        } else {
            // Password Unlock Mode
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    onClearError()
                },
                label = { Text("输入主密码") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onPasswordUnlock(password) }),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onPasswordUnlock(password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("解 锁", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            if (uiState.canUseBiometric) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = {
                    showPasswordInput = false
                    onBiometricClick()
                }) {
                    Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("切换为指纹解锁")
                }
            }
        }

        // Forgot password / Security Question recovery options
        if (uiState.hasSecurityQuestion) {
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = {
                recoveryError = ""
                showRecoveryDialog = true
            }) {
                Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("忘记主密码？使用密保找回")
            }
        } else {
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = { showResetConfirmDialog = true }) {
                Text(
                    text = "忘记密码并重置保险库",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }

        AnimatedVisibility(visible = uiState.errorMessage.isNotEmpty()) {
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}
