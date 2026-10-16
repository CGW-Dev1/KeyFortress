package com.keyfortress.app.ui.components

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.keyfortress.app.data.model.CategoryConstants
import com.keyfortress.app.data.repository.AccountItem
import com.keyfortress.app.ui.theme.AvatarGradients
import com.keyfortress.app.util.SmartTextParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

private const val DEFAULT_SAMPLE_TEXT = """微信：13800138000——WxPass#2026——常用社交账号
公司堡垒机：dev_admin——Fortress@Secure99
招商银行：6225880123456789——Bank#Pwd888——常用储蓄卡"""

private const val GOOGLE_CSV_SAMPLE_TEXT = """name,url,username,password,note
Google,https://accounts.google.com,testuser@gmail.com,GooglePass#2026,个人主账号
GitHub,https://github.com/login,octocat,ghp_secureToken123,开源工作开发账号
招商银行,https://www.cmbchina.com,6225880123456789,Cmb#Pass888,常用储蓄卡"""

data class ParsedItemUiState(
    val id: String = java.util.UUID.randomUUID().toString(),
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
    categories: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirmImport: (List<AccountItem>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val availableCategories = remember(categories) {
        if (categories.isNotEmpty()) categories else CategoryConstants.DEFAULT_CATEGORIES
    }
    val defaultCategory = remember(availableCategories) {
        availableCategories.firstOrNull { it == CategoryConstants.OTHER } ?: availableCategories.first()
    }

    var currentStep by remember { mutableStateOf(ImportStep.INPUT) }
    var rawInputText by remember { mutableStateOf("") }
    var isParsing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var editingItem by remember { mutableStateOf<ParsedItemUiState?>(null) }

    val parsedList = remember { mutableStateListOf<ParsedItemUiState>() }

    fun doParse(text: String) {
        if (text.isBlank()) {
            errorMessage = "请输入或粘贴待导入的文本内容"
            return
        }
        isParsing = true
        errorMessage = null

        coroutineScope.launch {
            // Run heavy regex & CSV parsing in background thread to prevent UI freezing on large pastes
            val results = withContext(Dispatchers.Default) {
                SmartTextParser.parse(text, validCategories = availableCategories)
            }
            if (results.isEmpty()) {
                errorMessage = "未能识别出有效的账号密码信息，请检查文本格式"
                isParsing = false
                return@launch
            }
            val mapped = results.map { parsed ->
                val cat = if (availableCategories.contains(parsed.category)) {
                    parsed.category
                } else {
                    defaultCategory
                }
                ParsedItemUiState(
                    title = parsed.title,
                    accountName = parsed.accountName,
                    password = parsed.password,
                    notes = parsed.notes,
                    category = cat,
                    isSelected = true
                )
            }
            parsedList.clear()
            parsedList.addAll(mapped)
            isParsing = false
            currentStep = ImportStep.PREVIEW
        }
    }

    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                }
                if (!content.isNullOrBlank()) {
                    rawInputText = content
                    errorMessage = null
                    doParse(content)
                } else {
                    errorMessage = "所选文件内容为空"
                }
            } catch (e: Exception) {
                errorMessage = "读取文件失败：${e.localizedMessage}"
            }
        }
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
                            text = "💡 支持各种常见格式与谷歌 CSV 导入：",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "• 谷歌/浏览器导出的 CSV 文件 (name,url,username,password,note)\n• 平台：账号——密码——备注\n• 平台\\t账号\\t密码 (从表格直接复制)\n• 平台:xxx 账号:xxx 密码:xxx",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick buttons (scrollable horizontally)
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
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
                            }

                            item {
                                AssistChip(
                                    onClick = { csvPickerLauncher.launch("*/*") },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    label = { Text("选择 CSV 文件") }
                                )
                            }

                            item {
                                AssistChip(
                                    onClick = {
                                        rawInputText = DEFAULT_SAMPLE_TEXT
                                        errorMessage = null
                                    },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    label = { Text("常规示例") }
                                )
                            }

                            item {
                                AssistChip(
                                    onClick = {
                                        rawInputText = GOOGLE_CSV_SAMPLE_TEXT
                                        errorMessage = null
                                    },
                                    label = { Text("谷歌CSV示例") }
                                )
                            }

                            if (rawInputText.isNotEmpty()) {
                                item {
                                    AssistChip(
                                        onClick = { rawInputText = "" },
                                        label = { Text("清空") }
                                    )
                                }
                            }
                        }

                        if (rawInputText.isNotBlank()) {
                            val lineCount = rawInputText.lines().count { it.isNotBlank() }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "已载入 $lineCount 行文本 (共 ${rawInputText.length} 字符)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = rawInputText,
                            onValueChange = {
                                rawInputText = it
                                errorMessage = null
                            },
                            placeholder = {
                                Text("请在此粘贴或输入待导入的密码文本...\n\n示例：\n$DEFAULT_SAMPLE_TEXT")
                            },
                            maxLines = 10,
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

                    if (isParsing) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("正在智能语义识别，请稍候...", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Button(
                            onClick = { doParse(rawInputText) },
                            enabled = rawInputText.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("开始智能识别 (解析)")
                        }
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

                        // Ultra high-performance preview LazyColumn with compact read cards
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(
                                items = parsedList,
                                key = { it.id },
                                contentType = { "parsed_card" }
                            ) { item ->
                                CompactParsedCard(
                                    item = item,
                                    onToggleSelect = {
                                        val idx = parsedList.indexOfFirst { it.id == item.id }
                                        if (idx != -1) {
                                            parsedList[idx] = item.copy(isSelected = !item.isSelected)
                                        }
                                    },
                                    onTogglePasswordVisibility = {
                                        val idx = parsedList.indexOfFirst { it.id == item.id }
                                        if (idx != -1) {
                                            parsedList[idx] = item.copy(isPasswordVisible = !item.isPasswordVisible)
                                        }
                                    },
                                    onEdit = { editingItem = item },
                                    onDelete = {
                                        parsedList.removeAll { it.id == item.id }
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
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
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
                            modifier = Modifier
                                .weight(2f)
                                .height(48.dp),
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

    // Modal Edit Dialog for single item tweaking
    if (editingItem != null) {
        val target = editingItem!!
        EditParsedItemDialog(
            item = target,
            availableCategories = availableCategories,
            onDismiss = { editingItem = null },
            onSave = { updated ->
                val idx = parsedList.indexOfFirst { it.id == target.id }
                if (idx != -1) {
                    parsedList[idx] = updated
                }
                editingItem = null
            }
        )
    }
}

/**
 * Lightweight, high-performance card for smart preview items.
 * Renders instantaneously without heavy multi-TextField layout overhead.
 */
@Composable
private fun CompactParsedCard(
    item: ParsedItemUiState,
    onToggleSelect: () -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val gradientColors = remember(item.title) {
        val gradientIndex = abs(item.title.hashCode()) % AvatarGradients.size
        AvatarGradients[gradientIndex]
    }
    val backgroundBrush = remember(gradientColors) {
        Brush.linearGradient(listOf(gradientColors.first, gradientColors.second))
    }
    val initial = remember(item.title) {
        item.title.firstOrNull()?.toString()?.uppercase() ?: "K"
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleSelect)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isSelected,
                onCheckedChange = { onToggleSelect() },
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Avatar circle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(backgroundBrush),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Text column
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title.ifBlank { "未知平台" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.accountName.ifBlank { "(未识别到账号)" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (item.isPasswordVisible) item.password else "••••••••",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = if (item.isPasswordVisible) FontFamily.Monospace else FontFamily.Default
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (item.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable(onClick = onTogglePasswordVisibility)
                    )
                }

                if (item.notes.isNotBlank()) {
                    Text(
                        text = "备注: ${item.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Edit & Delete Buttons
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "修改",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Modal dialog to edit single parsed account item.
 */
@Composable
private fun EditParsedItemDialog(
    item: ParsedItemUiState,
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (ParsedItemUiState) -> Unit
) {
    var title by remember { mutableStateOf(item.title) }
    var accountName by remember { mutableStateOf(item.accountName) }
    var password by remember { mutableStateOf(item.password) }
    var notes by remember { mutableStateOf(item.notes) }
    var category by remember {
        mutableStateOf(
            if (availableCategories.contains(item.category)) item.category
            else availableCategories.firstOrNull { it == CategoryConstants.OTHER } ?: availableCategories.firstOrNull() ?: CategoryConstants.OTHER
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改识别条目") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("平台名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("分类：", style = MaterialTheme.typography.bodySmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(availableCategories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("账号") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("备注 (选填)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        item.copy(
                            title = title.trim(),
                            accountName = accountName.trim(),
                            password = password.trim(),
                            notes = notes.trim(),
                            category = category
                        )
                    )
                },
                enabled = title.isNotBlank()
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

/**
 * Single line smart recognition dialog for use on the Add/Edit screen.
 */
@Composable
fun SingleSmartParseDialog(
    categories: List<String> = emptyList(),
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
                        val parsed = SmartTextParser.parseLine(textInput, validCategories = categories.ifEmpty { null })
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
