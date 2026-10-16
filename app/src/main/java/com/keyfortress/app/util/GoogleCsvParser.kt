package com.keyfortress.app.util

import com.keyfortress.app.data.model.CategoryConstants

/**
 * Parser for Google Chrome / Google Password Manager exported CSV files.
 * Supports standard RFC 4180 CSV syntax including quoted strings, escaped quotes,
 * commas inside values, newlines, and UTF-8 BOM headers.
 */
object GoogleCsvParser {

    /**
     * Checks if the given text matches a Google / browser passwords CSV format.
     */
    fun isGoogleCsv(input: String): Boolean {
        val clean = input.removePrefix("\uFEFF").trim()
        if (clean.isEmpty()) return false

        val firstLine = clean.lines().firstOrNull { it.isNotBlank() } ?: return false
        val tokens = parseCsvLine(firstLine).map { it.lowercase().trim().trim('"') }
        if (tokens.size < 3) return false

        val hasName = tokens.any { it in listOf("name", "title", "platform", "名称", "平台", "应用") }
        val hasPass = tokens.any { it in listOf("password", "pass", "pwd", "密码", "口令") }
        val hasUserOrUrl = tokens.any { it in listOf("url", "username", "user", "account", "login", "网址", "账号", "用户名") }

        return (hasName || hasUserOrUrl) && hasPass
    }

    /**
     * Parses Google CSV content into a list of [ParsedAccount].
     *
     * Mapping rules:
     * - name -> 平台名称 (title)
     * - username -> 登录账号 (accountName)
     * - password -> 密码 (password)
     * - url -> 放在备注信息中 (notes)
     * - category -> 基于平台名与 URL 进行智能分类，并严格约束在 [validCategories] 范围内
     */
    fun parse(csvText: String, validCategories: List<String>? = null): List<ParsedAccount> {
        val records = parseCsvRecords(csvText)
        if (records.size < 2) return emptyList()

        val header = records[0].map { it.lowercase().trim().trim('"') }
        val nameIdx = header.indexOfFirst { it in listOf("name", "title", "platform", "名称", "平台", "应用") }
        val urlIdx = header.indexOfFirst { it in listOf("url", "link", "网址", "链接", "站点") }
        val userIdx = header.indexOfFirst { it in listOf("username", "user", "account", "login", "账号", "用户名", "邮箱", "手机") }
        val passIdx = header.indexOfFirst { it in listOf("password", "pass", "pwd", "密码", "口令") }
        val noteIdx = header.indexOfFirst { it in listOf("note", "notes", "remark", "remarks", "备注", "说明") }

        val results = mutableListOf<ParsedAccount>()

        for (i in 1 until records.size) {
            val record = records[i]
            if (record.all { it.isBlank() }) continue

            val rawName = if (nameIdx in record.indices) record[nameIdx].trim() else ""
            val rawUrl = if (urlIdx in record.indices) record[urlIdx].trim() else ""
            val rawUser = if (userIdx in record.indices) record[userIdx].trim() else ""
            val rawPass = if (passIdx in record.indices) record[passIdx].trim() else ""
            val rawNote = if (noteIdx in record.indices) record[noteIdx].trim() else ""

            var title = rawName
            if (title.isBlank() && rawUrl.isNotBlank()) {
                title = extractTitleFromUrl(rawUrl)
            }

            // User requirement: "url放在备注信息里面"
            val finalNotes = when {
                rawUrl.isNotBlank() && rawNote.isNotBlank() && rawUrl != rawNote -> "$rawUrl\n$rawNote"
                rawUrl.isNotBlank() -> rawUrl
                else -> rawNote
            }

            if (title.isNotBlank() || rawUser.isNotBlank() || rawPass.isNotBlank()) {
                val category = SmartTextParser.inferCategory(title, finalNotes, validCategories)
                results.add(
                    ParsedAccount(
                        title = title,
                        accountName = rawUser,
                        password = rawPass,
                        notes = finalNotes,
                        category = category,
                        rawText = record.joinToString(",")
                    )
                )
            }
        }

        return results
    }

    /**
     * Extracts a readable title from a URL when name is omitted.
     */
    private fun extractTitleFromUrl(url: String): String {
        return try {
            val clean = url.trim()
            if (clean.startsWith("android://")) {
                val pkg = clean.substringAfter("@").substringBefore("/").trim()
                if (pkg.isNotBlank()) return pkg
            }
            val withoutProtocol = if (clean.contains("://")) clean.substringAfter("://") else clean
            val host = withoutProtocol.substringBefore("/").substringBefore("?").substringBefore(":")
            val domain = host.removePrefix("www.")
            domain.ifBlank { url }
        } catch (e: Exception) {
            url
        }
    }

    /**
     * Parses RFC 4180 CSV text into rows of columns.
     */
    fun parseCsvRecords(csvText: String): List<List<String>> {
        val cleanText = csvText.removePrefix("\uFEFF")
        val records = mutableListOf<List<String>>()
        val currentRecord = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < cleanText.length) {
            val c = cleanText[i]
            when {
                inQuotes -> {
                    if (c == '"') {
                        if (i + 1 < cleanText.length && cleanText[i + 1] == '"') {
                            // Escaped quote: "" -> "
                            currentField.append('"')
                            i++
                        } else {
                            // Closing quote
                            inQuotes = false
                        }
                    } else {
                        currentField.append(c)
                    }
                }
                c == '"' -> {
                    inQuotes = true
                }
                c == ',' -> {
                    currentRecord.add(currentField.toString().trim())
                    currentField.clear()
                }
                c == '\r' -> {
                    if (i + 1 < cleanText.length && cleanText[i + 1] == '\n') {
                        i++
                    }
                    currentRecord.add(currentField.toString().trim())
                    currentField.clear()
                    if (currentRecord.any { it.isNotBlank() }) {
                        records.add(ArrayList(currentRecord))
                    }
                    currentRecord.clear()
                }
                c == '\n' -> {
                    currentRecord.add(currentField.toString().trim())
                    currentField.clear()
                    if (currentRecord.any { it.isNotBlank() }) {
                        records.add(ArrayList(currentRecord))
                    }
                    currentRecord.clear()
                }
                else -> {
                    currentField.append(c)
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRecord.isNotEmpty()) {
            currentRecord.add(currentField.toString().trim())
            if (currentRecord.any { it.isNotBlank() }) {
                records.add(currentRecord)
            }
        }

        return records
    }

    /**
     * Helper to parse a single line as CSV tokens.
     */
    fun parseCsvLine(line: String): List<String> {
        val records = parseCsvRecords(line)
        return records.firstOrNull() ?: emptyList()
    }
}
