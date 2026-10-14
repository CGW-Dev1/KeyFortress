package com.keyfortress.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keyfortress.app.security.AppLockManager

@Composable
fun SecurityQuestionRecoveryDialog(
    questionText: String,
    onConfirm: (answer: String, newPassword: String) -> Unit,
    onDismiss: () -> Unit,
    errorMessage: String = ""
) {
    var answer by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "密保问题找回主密码",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "回答你预先设置的密保问题，验证通过后可直接设置新主密码：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Question Display
                Text(
                    text = "密保问题：$questionText",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Answer Input
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it; localError = "" },
                    label = { Text("密保答案") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // New Password Input
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; localError = "" },
                    label = { Text("新主密码 (至少 6 位)") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                PasswordStrengthBar(password = newPassword)

                Spacer(modifier = Modifier.height(8.dp))

                // Confirm Password Input
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; localError = "" },
                    label = { Text("确认新主密码") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                val displayErr = localError.ifEmpty { errorMessage }
                if (displayErr.isNotEmpty()) {
                    Text(
                        text = displayErr,
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
                        answer.isBlank() -> localError = "请输入密保答案"
                        newPassword.length < 6 -> localError = "新主密码长度至少 6 位"
                        newPassword != confirmPassword -> localError = "两次输入的新密码不一致"
                        else -> onConfirm(answer, newPassword)
                    }
                }
            ) {
                Text("重置并进入")
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
fun SetSecurityQuestionDialog(
    initialQuestion: String = "",
    onConfirm: (question: String, answer: String) -> Unit,
    onDismiss: () -> Unit
) {
    val presets = AppLockManager.PRESET_QUESTIONS
    var selectedPreset by remember {
        mutableStateOf(if (initialQuestion.isNotEmpty() && initialQuestion in presets) initialQuestion else presets[0])
    }
    var isCustom by remember {
        mutableStateOf(initialQuestion.isNotEmpty() && initialQuestion !in presets)
    }
    var customQuestion by remember {
        mutableStateOf(if (isCustom) initialQuestion else "")
    }
    var answer by remember { mutableStateOf("") }
    var answerVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialQuestion.isEmpty()) "设置密保问题" else "修改密保问题") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "密保答案将用于在遗忘主密码时恢复保险库，答案采用 PBKDF2 加密派生，不落盘明文存储。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Question Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (isCustom) "自定义问题..." else selectedPreset,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("选择密保问题") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.clickable { dropdownExpanded = true }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dropdownExpanded = true },
                        shape = RoundedCornerShape(10.dp)
                    )

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        presets.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset) },
                                onClick = {
                                    selectedPreset = preset
                                    isCustom = false
                                    dropdownExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("自定义问题...") },
                            onClick = {
                                isCustom = true
                                dropdownExpanded = false
                            }
                        )
                    }
                }

                if (isCustom) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customQuestion,
                        onValueChange = { customQuestion = it; error = "" },
                        label = { Text("输入自定义问题") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Answer Input
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it; error = "" },
                    label = { Text("密保答案") },
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
                    val finalQuestion = if (isCustom) customQuestion.trim() else selectedPreset
                    when {
                        finalQuestion.isBlank() -> error = "请输入或选择密保问题"
                        answer.isBlank() -> error = "请输入密保答案"
                        else -> onConfirm(finalQuestion, answer)
                    }
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
