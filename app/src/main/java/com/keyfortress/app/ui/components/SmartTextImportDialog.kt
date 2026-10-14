package com.keyfortress.app.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.keyfortress.app.data.model.CategoryConstants
import com.keyfortress.app.data.repository.AccountItem
import com.keyfortress.app.util.SmartTextParser

private const val DEFAULT_SAMPLE_TEXT = """微信：13800138000——WxPass#2026——常用社交账号
公司堡垒机：dev_admin——Fortress@Secure99
招商银行：6225880123456789——Bank#Pwd888——常用储蓄卡"""

data class ParsedItemUiState(
    var id: String = java.util.UUID.randomUUID().toString(),
    var title: String = "",
    var accountName: String = "",
    var password: String = "",
    var notes: String = "",
    var category: String = CategoryConstants.OTHER,
    var isSelected: Boolean = true,
    var isPasswordVisible: Boolean = false
)

private enum class ImportStep {
    INPUT,
    PREVIEW
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartTextImportDialog(
    onDismiss: () -> Unit,
    onConfirmImport: (List<AccountItem>) -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(ImportStep.INPUT) }
    var rawInputText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val parsedList = remember { mutableStateListOf<ParsedItemUiState>() }

    fun doParse(text: String) {
        if (text.isBlank()) {
            errorMessage = "请输入或粘贴待导入的文本内容"
            return
        }
        val results = SmartTextParser.parse(text)
        if (results.isEmpty()) {
            errorMessage = "未能识别出有效的账号密码信息，请检查文本格式"
            return
        }
        parsedList.clear()
        results.forEach { parsed ->
            parsedList.add(
                ParsedItemUiState(
                    title = parsed.title,
                    accountName = parsed.accountName,
                    password = parsed.password,
                    notes = parsed.notes,
                    category = parsed.category,
                    isSelected = true
                )
            )
        }
        errorMessage = null
        currentStep = ImportStep.PREVIEW
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "智能文本导入",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (currentStep == ImportStep.INPUT) "粘贴任意格式，秒级自动识别" else "识别结果预览与修改",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Step Content
                if (currentStep == ImportStep.INPUT) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 支持各种常见格式，例如：",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "• 平台：账号——密码——备注\n• 平台\\t账号\\t密码 (从表格直接复制)\n• 平台:xxx 账号:xxx 密码:xxx\n• 空格、逗号、斜杠等分隔，自动智能判断",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = clipboard.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        val text = clip.getItemAt(0).coerceToText(context).toString()
                                        rawInputText = text
                                        errorMessage = null
                                    }
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("粘贴剪贴板") }
                            )

                            AssistChip(
                                onClick = {
                                    rawInputText = DEFAULT_SAMPLE_TEXT
                                    errorMessage = null
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("填入示例") }
                            )

                            if (rawInputText.isNotEmpty()) {
                                AssistChip(
                                    onClick = { rawInputText = "" },
                                    label = { Text("清空") }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = rawInputText,
                            onValueChange = {
                                rawInputText = it
                                errorMessage = null
                            },
                            placeholder = {
                                Text("请在此粘贴或输入待导入的密码文本...\n\n示例：\n$DEFAULT_SAMPLE_TEXT")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { doParse(rawInputText) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("开始智能识别 (解析)")
                    }
                } else {
                    // PREVIEW Step
                    val selectedCount = parsedList.count { it.isSelected }
                    val allSelected = parsedList.isNotEmpty() && parsedList.all { it.isSelected }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    val target = !allSelected
                                    parsedList.indices.forEach { idx ->
                                        parsedList[idx] = parsedList[idx].copy(isSelected = target)
                                    }
                                }
                            ) {
                                Checkbox(
                                    checked = allSelected,
                                    onCheckedChange = { checked ->
                                        parsedList.indices.forEach { idx ->
                                            parsedList[idx] = parsedList[idx].copy(isSelected = checked)
                                        }
                                    }
                                )
                                Text(
                                    text = "全选 (${selectedCount}/${parsedList.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            TextButton(onClick = { currentStep = ImportStep.INPUT }) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("修改原文本")
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            itemsIndexed(parsedList, key = { _, item -> item.id }) { index, item ->
                                ParsedAccountCard(
                                    item = item,
                                    onItemChange = { updated ->
                                        parsedList[index] = updated
                                    },
                                    onDelete = {
                                        parsedList.removeAt(index)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { currentStep = ImportStep.INPUT },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("返回")
                        }

                        Button(
                            onClick = {
                                val toImport = parsedList
                                    .filter { it.isSelected && it.title.isNotBlank() }
                                    .map {
                                        AccountItem(
                                            title = it.title.trim(),
                                            accountName = it.accountName.trim(),
                                            password = it.password.trim(),
                                            notes = it.notes.trim(),
                                            category = it.category
                                        )
                                    }
                                onConfirmImport(toImport)
                            },
                            enabled = selectedCount > 0,
                            modifier = Modifier.weight(2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("确认导入 (已选 $selectedCount 条)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParsedAccountCard(
    item: ParsedItemUiState,
    onItemChange: (ParsedItemUiState) -> Unit,
    onDelete: () -> Unit
) {
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Checkbox, Title Field, Category Chip, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isSelected,
                    onCheckedChange = { checked ->
                        onItemChange(item.copy(isSelected = checked))
                    }
                )

                OutlinedTextField(
                    value = item.title,
                    onValueChange = { onItemChange(item.copy(title = it)) },
                    label = { Text("平台名称") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Category selector button
                Box {
                    AssistChip(
                        onClick = { categoryMenuExpanded = true },
                        label = { Text(item.category, fontSize = 12.sp) }
                    )
                    DropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false }
                    ) {
                        CategoryConstants.DEFAULT_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    onItemChange(item.copy(category = cat))
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除该条",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Account and Password in a Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = item.accountName,
                    onValueChange = { onItemChange(item.copy(accountName = it)) },
                    label = { Text("账号") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = item.password,
                    onValueChange = { onItemChange(item.copy(password = it)) },
                    label = { Text("密码") },
                    singleLine = true,
                    visualTransformation = if (item.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { onItemChange(item.copy(isPasswordVisible = !item.isPasswordVisible)) }) {
                            Icon(
                                imageVector = if (item.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (item.isPasswordVisible) "隐藏密码" else "显示明文",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Notes
            OutlinedTextField(
                value = item.notes,
                onValueChange = { onItemChange(item.copy(notes = it)) },
                label = { Text("备注信息 (可选)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

/**
 * Single line smart recognition dialog for use on the Add/Edit screen.
 */
@Composable
fun SingleSmartParseDialog(
    onDismiss: () -> Unit,
    onParsed: (title: String, account: String, password: String, notes: String, category: String) -> Unit
) {
    val context = LocalContext.current
    var textInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("智能识别单条文本")
            }
        },
        text = {
            Column {
                Text(
                    text = "粘贴包含平台、账号、密码的单行文本，自动解析并填入表单：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    AssistChip(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                textInput = clip.getItemAt(0).coerceToText(context).toString()
                            }
                        },
                        label = { Text("粘贴剪贴板") },
                        leadingIcon = { Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AssistChip(
                        onClick = {
                            textInput = "微信：13800138000——WxPass#2026——常用社交账号"
                        },
                        label = { Text("填入示例") }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("例如：微信：13800000000——mypass123") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (textInput.isNotBlank()) {
                        val parsed = SmartTextParser.parseLine(textInput)
                        onParsed(parsed.title, parsed.accountName, parsed.password, parsed.notes, parsed.category)
                    }
                },
                enabled = textInput.isNotBlank()
            ) {
                Text("识别并填入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
