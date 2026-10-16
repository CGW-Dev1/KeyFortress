package com.keyfortress.app.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keyfortress.app.security.AppLockManager
import com.keyfortress.app.util.TimeFormatter
import java.io.File

/**
 * Built-in in-app file explorer dialog that works 100% offline and requires zero external system components.
 * Bypasses broken or missing DocumentsUI / OEM file pickers across all Android devices.
 */
@Composable
fun InAppFilePickerDialog(
    onDismiss: () -> Unit,
    onFileSelected: (File) -> Unit
) {
    val context = LocalContext.current

    val initialDir = remember {
        val download = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        when {
            download != null && download.exists() && download.canRead() -> download
            File("/storage/emulated/0/Download").exists() -> File("/storage/emulated/0/Download")
            File("/storage/emulated/0").exists() -> File("/storage/emulated/0")
            else -> context.getExternalFilesDir(null) ?: context.filesDir
        }
    }

    var currentDir by remember { mutableStateOf(initialDir) }
    var fileList by remember { mutableStateOf<List<File>>(emptyList()) }
    var manualPathInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableStateOf(0) }

    fun checkAllFilesPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    var hasStoragePermission by remember { mutableStateOf(checkAllFilesPermission()) }

    fun refreshFiles(dir: File) {
        val list = try {
            dir.listFiles()?.toList() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        val (folders, files) = list.partition { it.isDirectory }
        val sortedFolders = folders.sortedBy { it.name.lowercase() }
        val sortedFiles = files.sortedWith(
            compareByDescending<File> { it.extension.equals("csv", ignoreCase = true) || it.extension.equals("txt", ignoreCase = true) }
                .thenByDescending { it.lastModified() }
        )
        fileList = sortedFolders + sortedFiles
    }

    LaunchedEffect(currentDir, refreshKey) {
        hasStoragePermission = checkAllFilesPermission()
        refreshFiles(currentDir)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentDir.parentFile != null && currentDir.parentFile?.canRead() == true) {
                    IconButton(
                        onClick = {
                            currentDir.parentFile?.let { parent ->
                                currentDir = parent
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上一级")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = "浏览手机文件",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { refreshKey++ },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "刷新")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Current path indicator
                Text(
                    text = "当前路径: ${currentDir.absolutePath}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Quick jump shortcuts
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        AssistChip(
                            onClick = {
                                val dl = File("/storage/emulated/0/Download")
                                if (dl.exists()) currentDir = dl
                                else Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.let { currentDir = it }
                            },
                            label = { Text("下载目录", fontSize = 11.sp) }
                        )
                    }
                    item {
                        AssistChip(
                            onClick = {
                                val docs = File("/storage/emulated/0/Documents")
                                if (docs.exists()) currentDir = docs
                                else Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)?.let { currentDir = it }
                            },
                            label = { Text("文档目录", fontSize = 11.sp) }
                        )
                    }
                    item {
                        AssistChip(
                            onClick = {
                                val root = File("/storage/emulated/0")
                                if (root.exists()) currentDir = root
                            },
                            label = { Text("内部存储根目录", fontSize = 11.sp) }
                        )
                    }
                    item {
                        AssistChip(
                            onClick = {
                                val kf = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "KeyFortress")
                                if (kf.exists()) currentDir = kf
                                else context.getExternalFilesDir(null)?.let { currentDir = it }
                            },
                            label = { Text("应用存储目录", fontSize = 11.sp) }
                        )
                    }
                }

                // Storage permission banner for Android 11+
                if (!hasStoragePermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "需要存储访问权限",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Android 11+ 系统需要授予「所有文件访问权限」才能浏览公共下载目录：",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = {
                                    try {
                                        AppLockManager.isBypassingAutoLock = true
                                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        try {
                                            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("前往系统设置开启文件访问权限", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Directory file listing
                if (fileList.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "当前文件夹为空或无权读取",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(fileList) { file ->
                            val isCsvOrTxt = file.extension.equals("csv", ignoreCase = true) || file.extension.equals("txt", ignoreCase = true)
                            val isDir = file.isDirectory

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isDir) {
                                            currentDir = file
                                        } else if (isCsvOrTxt) {
                                            onFileSelected(file)
                                        }
                                    },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCsvOrTxt) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isDir) Icons.Default.Folder else Icons.Default.Description,
                                        contentDescription = null,
                                        tint = if (isCsvOrTxt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isCsvOrTxt) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCsvOrTxt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!isDir) {
                                            val sizeKb = (file.length() / 1024.0).let { "%.1f KB".format(it) }
                                            Text(
                                                text = "${TimeFormatter.formatFull(file.lastModified())} • $sizeKb",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Manual file path input section
                Text(
                    text = "或直接输入/粘贴文件完整路径：",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualPathInput,
                        onValueChange = {
                            manualPathInput = it
                            inputError = null
                        },
                        placeholder = { Text("例如 /sdcard/Download/passwords.csv", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            val trimmed = manualPathInput.trim()
                            if (trimmed.isBlank()) {
                                inputError = "请输入文件路径"
                                return@Button
                            }
                            val targetFile = File(trimmed)
                            if (!targetFile.exists() || !targetFile.isFile) {
                                inputError = "文件不存在或不是有效文件"
                                return@Button
                            }
                            onFileSelected(targetFile)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("读取", fontSize = 12.sp)
                    }
                }
                if (inputError != null) {
                    Text(
                        text = inputError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
