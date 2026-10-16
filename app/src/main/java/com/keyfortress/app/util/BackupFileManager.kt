package com.keyfortress.app.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object BackupFileManager {

    /**
     * Saves backup content to the public Downloads/KeyFortress directory without requiring dangerous permissions.
     * Returns Pair(success, displayPathOrMessage).
     */
    fun saveToDownloads(context: Context, fileName: String, content: String): Pair<Boolean, String> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (API 29+): MediaStore.Downloads requires zero runtime permissions
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/KeyFortress")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("无法创建文件路径")

                context.contentResolver.openOutputStream(uri)?.use { os ->
                    OutputStreamWriter(os, Charsets.UTF_8).use { writer ->
                        writer.write(content)
                    }
                }
                Pair(true, "已成功保存到手机「下载/KeyFortress」目录:\n$fileName")
            } else {
                // Android 8 / 9: Save to app-specific external documents or downloads
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "KeyFortress")
                if (!dir.exists()) dir.mkdirs()
                val targetDir = if (dir.canWrite()) dir else File(context.getExternalFilesDir(null) ?: context.filesDir, "KeyFortress")
                if (!targetDir.exists()) targetDir.mkdirs()

                val file = File(targetDir, fileName)
                file.writeText(content, Charsets.UTF_8)
                Pair(true, "已成功保存到: ${file.absolutePath}")
            }
        } catch (e: Exception) {
            // Fallback: save to internal cache and notify user
            try {
                val fallbackFile = File(context.getExternalFilesDir(null) ?: context.filesDir, fileName)
                fallbackFile.writeText(content, Charsets.UTF_8)
                Pair(true, "已保存到应用存储目录: ${fallbackFile.name}")
            } catch (err: Exception) {
                Pair(false, "保存失败: ${err.localizedMessage}")
            }
        }
    }

    /**
     * Shares backup content using standard Android System Share Sheet (WeChat, QQ, Email, Files, Drive, etc.).
     */
    fun shareBackup(context: Context, fileName: String, content: String): Boolean {
        return try {
            val cacheFile = File(context.cacheDir, fileName)
            cacheFile.writeText(content, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "发送或保存备份文件").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Reads text safely from a content URI.
     */
    fun readTextFromUri(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                reader.readText()
            }
        } ?: throw IllegalStateException("无法读取选中的备份文件")
    }

    /**
     * Scans known directories for existing .vault backup files on this device.
     */
    fun listLocalBackups(context: Context): List<File> {
        val results = mutableListOf<File>()
        try {
            val dlDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "KeyFortress")
            if (dlDir.exists()) {
                dlDir.listFiles { f -> f.extension.equals("vault", ignoreCase = true) || f.extension.equals("json", ignoreCase = true) }
                    ?.let { results.addAll(it) }
            }
        } catch (_: Exception) {}

        try {
            val appDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "KeyFortress")
            if (appDir.exists()) {
                appDir.listFiles { f -> f.extension.equals("vault", ignoreCase = true) || f.extension.equals("json", ignoreCase = true) }
                    ?.let { results.addAll(it) }
            }
        } catch (_: Exception) {}

        return results.sortedByDescending { it.lastModified() }
    }

    /**
     * Scans known directories for existing .csv files on this device.
     */
    fun listLocalCsvFiles(context: Context): List<File> {
        val results = mutableListOf<File>()
        val searchDirs = mutableListOf<File>()

        try {
            val pubDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (pubDownloads != null && pubDownloads.exists()) searchDirs.add(pubDownloads)
        } catch (_: Exception) {}

        try {
            val pubDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (pubDocs != null && pubDocs.exists()) searchDirs.add(pubDocs)
        } catch (_: Exception) {}

        try {
            val extRoot = Environment.getExternalStorageDirectory()
            if (extRoot != null && extRoot.exists()) {
                searchDirs.add(File(extRoot, "Download"))
                searchDirs.add(File(extRoot, "Downloads"))
                searchDirs.add(File(extRoot, "Documents"))
                searchDirs.add(File(extRoot, "tencent/QQfile_recv"))
                searchDirs.add(File(extRoot, "tencent/MicroMsg/Download"))
            }
        } catch (_: Exception) {}

        try {
            context.getExternalFilesDir(null)?.let { searchDirs.add(it) }
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { searchDirs.add(it) }
            context.filesDir?.let { searchDirs.add(it) }
            context.cacheDir?.let { searchDirs.add(it) }
        } catch (_: Exception) {}

        val scannedDirs = mutableSetOf<String>()

        fun scanDir(dir: File, depth: Int) {
            if (depth > 2 || !dir.exists() || !dir.isDirectory || !dir.canRead()) return
            val canonical = try { dir.canonicalPath } catch (_: Exception) { dir.absolutePath }
            if (scannedDirs.contains(canonical)) return
            scannedDirs.add(canonical)

            val files = try { dir.listFiles() } catch (_: Exception) { null } ?: return
            for (f in files) {
                if (f.isFile && f.extension.equals("csv", ignoreCase = true)) {
                    results.add(f)
                } else if (f.isDirectory && depth < 2 && !f.name.startsWith(".")) {
                    scanDir(f, depth + 1)
                }
            }
        }

        for (d in searchDirs.distinctBy { it.absolutePath }) {
            scanDir(d, 0)
        }

        return results.distinctBy { it.absolutePath }.sortedByDescending { it.lastModified() }
    }

    /**
     * Reads text safely from a content URI with UTF-8 / BOM / GBK encoding auto-detection.
     */
    fun readCsvFromUri(context: Context, uri: Uri): String {
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes()
            }
        } catch (_: Exception) {
            null
        } ?: if (uri.scheme == "file" && uri.path != null) {
            File(uri.path!!).readBytes()
        } else {
            throw IllegalStateException("无法读取选中的文件内容")
        }
        return decodeBytesToText(bytes)
    }

    /**
     * Reads text safely from a File with UTF-8 / BOM / GBK encoding auto-detection.
     */
    fun readCsvFromFile(file: File): String {
        val bytes = file.readBytes()
        return decodeBytesToText(bytes)
    }

    /**
     * Decodes byte array to string with UTF-8, UTF-8 BOM, and GB18030 / GBK fallback.
     */
    fun decodeBytesToText(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        // Check for UTF-8 BOM: 0xEF, 0xBB, 0xBF
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }
        // Try strict UTF-8 decoding
        try {
            val decoder = Charsets.UTF_8.newDecoder()
            decoder.onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            decoder.onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
            val byteBuffer = java.nio.ByteBuffer.wrap(bytes)
            return decoder.decode(byteBuffer).toString()
        } catch (_: Exception) {
            // Fallback to GB18030 (which is backward compatible with GBK and GB2312)
            try {
                return String(bytes, java.nio.charset.Charset.forName("GB18030"))
            } catch (_: Exception) {
                return String(bytes, Charsets.UTF_8)
            }
        }
    }
}
