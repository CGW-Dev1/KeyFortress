package com.keyfortress.app.ui.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keyfortress.app.data.model.CategoryConstants
import com.keyfortress.app.ui.components.PasswordGeneratorSheet
import com.keyfortress.app.ui.components.PasswordStrengthBar
import com.keyfortress.app.ui.components.SingleSmartParseDialog

private val PLATFORM_SUGGESTIONS = listOf(
    Pair("微信", CategoryConstants.SOCIAL),
    Pair("支付宝", CategoryConstants.FINANCE),
    Pair("抖音", CategoryConstants.ENTERTAINMENT),
    Pair("小红书", CategoryConstants.SOCIAL),
    Pair("GitHub", CategoryConstants.WORK),
    Pair("Google", CategoryConstants.WORK),
    Pair("淘宝", CategoryConstants.OTHER),
    Pair("京东", CategoryConstants.OTHER),
    Pair("微博", CategoryConstants.SOCIAL),
    Pair("哔哩哔哩", CategoryConstants.ENTERTAINMENT),
    Pair("网易邮箱", CategoryConstants.WORK),
    Pair("Apple ID", CategoryConstants.OTHER)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    accountId: Long = 0L,
    viewModel: EditViewModel,
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(accountId) {
        viewModel.loadAccount(accountId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    var showGeneratorSheet by remember { mutableStateOf(false) }
    var showSmartParseDialog by remember { mutableStateOf(false) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onNavigateBack()
        }
    }

    if (showGeneratorSheet) {
        PasswordGeneratorSheet(
            onDismiss = { showGeneratorSheet = false },
            onPasswordSelected = { pwd ->
                viewModel.onPasswordChange(pwd)
                passwordVisible = true
            }
        )
    }

    if (showSmartParseDialog) {
        SingleSmartParseDialog(
            onDismiss = { showSmartParseDialog = false },
            onParsed = { title, account, password, notes, category ->
                viewModel.applySmartParsed(title, account, password, notes, category)
                showSmartParseDialog = false
                passwordVisible = true
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "编辑账号" else "新增账号") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (!uiState.isEditMode) {
                        IconButton(onClick = { showSmartParseDialog = true }) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "智能识别填入")
                        }
                    }
                    IconButton(
                        onClick = { viewModel.saveAccount() },
                        enabled = !uiState.isSaving
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "保存")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Quick Platform Suggestions for new accounts
            if (!uiState.isEditMode && uiState.title.isBlank()) {
                Text(
                    text = "常用平台快捷选择",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(PLATFORM_SUGGESTIONS) { (name, category) ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                viewModel.onTitleChange(name)
                                viewModel.onCategoryChange(category)
                            },
                            label = { Text(name, fontSize = 12.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Platform Title
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.onTitleChange(it) },
                label = { Text("平台名称 (如 微信、华宝证券)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Category Selection (Dynamic categories from DB + quick add)
            Text(
                text = "分类标签",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            val categoryList = if (categories.isNotEmpty()) categories.map { it.name } else CategoryConstants.DEFAULT_CATEGORIES
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categoryList) { cat ->
                    FilterChip(
                        selected = uiState.category == cat,
                        onClick = { viewModel.onCategoryChange(cat) },
                        label = { Text(cat) }
                    )
                }
                item {
                    AssistChip(
                        onClick = { showNewCategoryDialog = true },
                        label = { Text("+ 新增分类") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Account Name
            OutlinedTextField(
                value = uiState.accountName,
                onValueChange = { viewModel.onAccountNameChange(it) },
                label = { Text("登录账号 (手机号 / 邮箱 / 用户名)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Password with Masking toggle
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { viewModel.onPasswordChange(it) },
                label = { Text("登录密码") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "隐藏密码" else "显示明文"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            PasswordStrengthBar(password = uiState.password)

            Spacer(modifier = Modifier.height(8.dp))

            // Random Password Generator Button
            OutlinedButton(
                onClick = { showGeneratorSheet = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("一键随机生成高强度密码")
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Linked Phone / Email (Optional)
            OutlinedTextField(
                value = uiState.linkedContact,
                onValueChange = { viewModel.onLinkedContactChange(it) },
                label = { Text("绑定手机 / 邮箱 (选填)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Notes (Optional)
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.onNotesChange(it) },
                label = { Text("备注信息 (密保提示等，选填)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Starred Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "设为常用 (星标置顶)",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = uiState.isStarred,
                    onCheckedChange = { viewModel.onStarredChange(it) }
                )
            }

            AnimatedVisibility(visible = uiState.errorMessage.isNotEmpty()) {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = { viewModel.saveAccount() },
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (uiState.isEditMode) "保存修改" else "保存到保险库", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Add New Category Dialog
    if (showNewCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            title = { Text("新增分类") },
            text = {
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    label = { Text("分类名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newCatName.trim()
                        if (trimmed.isNotBlank()) {
                            viewModel.addCategory(trimmed) {
                                showNewCategoryDialog = false
                            }
                        }
                    },
                    enabled = newCatName.isNotBlank()
                ) {
                    Text("添加并选中")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCategoryDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
