package com.keyfortress.app.util

import com.keyfortress.app.data.model.CategoryConstants
import java.util.UUID

data class ParsedAccount(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var accountName: String = "",
    var password: String = "",
    var notes: String = "",
    var category: String = CategoryConstants.OTHER,
    var isSelected: Boolean = true,
    val rawText: String = ""
)

object SmartTextParser {

    private val FINANCE_KEYWORDS = listOf(
        "交易", "银行", "基金", "信托", "证券", "理财", "券商", "支付宝", "微信支付",
        "钱包", "金融", "保险", "招商", "工行", "建行", "农行", "中行", "交行",
        "浦发", "中信", "平安", "民生", "光大", "华夏", "兴业", "广发", "银联",
        "易方达", "汇添富", "南方", "嘉实", "富国", "博时"
    )

    private val WORK_KEYWORDS = listOf(
        "堡垒机", "git", "github", "gitlab", "gitee", "jira", "confluence",
        "jenkins", "邮箱", "mail", "outlook", "企业微信", "企微", "飞书", "钉钉",
        "aliyun", "阿里云", "腾讯云", "华为云", "aws", "azure", "服务器", "server",
        "linux", "数据库", "vpn", "oa", "内网", "工作", "公司", "开发者", "dev"
    )

    private val SOCIAL_KEYWORDS = listOf(
        "微信", "wechat", "qq", "微博", "weibo", "抖音", "tiktok", "快手",
        "小红书", "知乎", "贴吧", "telegram", "tg", "twitter", "facebook",
        "fb", "instagram", "ins", "whatsapp", "discord", "line", "reddit",
        "推特", "领英", "linkedin"
    )

    private val ENTERTAINMENT_KEYWORDS = listOf(
        "哔哩哔哩", "bilibili", "b站", "爱奇艺", "优酷", "youku", "腾讯视频",
        "芒果", "网易云", "qq音乐", "酷狗", "steam", "epic", "playstation",
        "psn", "switch", "nintendo", "xbox", "米哈游", "原神", "王者荣耀",
        "英雄联盟", "游戏", "电影", "音乐", "acfun"
    )

    fun inferCategory(title: String, notes: String = ""): String {
        val titleLower = title.lowercase()
        val fromTitle = matchCategory(titleLower)
        if (fromTitle != CategoryConstants.OTHER) {
            return fromTitle
        }
        val notesLower = notes.lowercase()
        return matchCategory(notesLower)
    }

    private fun matchCategory(text: String): String {
        return when {
            WORK_KEYWORDS.any { text.contains(it) } -> CategoryConstants.WORK
            FINANCE_KEYWORDS.any { text.contains(it) } -> CategoryConstants.FINANCE
            SOCIAL_KEYWORDS.any { text.contains(it) } -> CategoryConstants.SOCIAL
            ENTERTAINMENT_KEYWORDS.any { text.contains(it) } -> CategoryConstants.ENTERTAINMENT
            else -> CategoryConstants.OTHER
        }
    }

    /**
     * Parses batch text into a list of [ParsedAccount].
     * Supports line-by-line format (e.g. 平台：账号——密码——备注),
     * table/CSV/TSV format, and multi-line block format.
     */
    fun parse(input: String): List<ParsedAccount> {
        val normalized = input.replace("\r\n", "\n").replace('\r', '\n').trim()
        if (normalized.isBlank()) return emptyList()

        val lines = normalized.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()

        // Check if input is block-formatted (multiple lines per entry separated by empty line or dashes)
        if (isBlockFormat(lines)) {
            val blocks = normalized.split(Regex("\n\\s*\\n|^-{3,}$|^={3,}$", RegexOption.MULTILINE))
            val result = mutableListOf<ParsedAccount>()
            for (block in blocks) {
                val parsed = parseBlock(block.trim())
                if (parsed != null && (parsed.title.isNotBlank() || parsed.accountName.isNotBlank())) {
                    result.add(parsed)
                }
            }
            if (result.isNotEmpty()) return result
        }

        // Line by line parsing
        val results = mutableListOf<ParsedAccount>()
        for (line in lines) {
            // Skip pure comment lines
            if (line.startsWith("#") || line.startsWith("//")) continue
            val item = parseLine(line)
            if (item.title.isNotBlank() || item.accountName.isNotBlank() || item.password.isNotBlank()) {
                results.add(item)
            }
        }
        return results
    }

    private fun isBlockFormat(lines: List<String>): Boolean {
        var labelCount = 0
        for (line in lines) {
            if (Regex("^(平台|名称|应用|网站|账号|用户名|密码|口令|备注)[：:]").containsMatchIn(line)) {
                labelCount++
            }
        }
        // If more than 3 lines are individual field labels and lines don't contain common delimiters
        return labelCount >= 3 && lines.none { it.contains("——") || it.contains("\t") }
    }

    private fun parseBlock(block: String): ParsedAccount? {
        val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return null

        var title = ""
        var account = ""
        var password = ""
        var notes = ""

        for (line in lines) {
            val colonIdx = line.indexOfFirst { it == ':' || it == '：' }
            if (colonIdx > 0) {
                val key = line.substring(0, colonIdx).trim()
                val value = line.substring(colonIdx + 1).trim()
                when {
                    key.contains("平台") || key.contains("名称") || key.contains("应用") || key.contains("网站") -> title = value
                    key.contains("账号") || key.contains("用户名") || key.contains("手机") || key.contains("邮箱") || key.contains("登录") -> account = value
                    key.contains("密码") || key.contains("口令") -> password = value
                    key.contains("备注") || key.contains("说明") -> notes = if (notes.isEmpty()) value else "$notes; $value"
                    else -> if (title.isEmpty()) title = value
                }
            }
        }

        if (title.isBlank() && account.isBlank() && password.isBlank()) return null
        return ParsedAccount(
            title = title,
            accountName = account,
            password = password,
            notes = notes,
            category = inferCategory(title, notes),
            rawText = block
        )
    }

    /**
     * Parses a single line into a [ParsedAccount].
     */
    fun parseLine(line: String): ParsedAccount {
        val trimmed = line.trim()

        // 1. Check if line has inline explicit labels: e.g. 平台:xxx 账号:yyy 密码:zzz
        val titleMatch = Regex("(?:平台|名称|应用|网站)[：:]([^\\s]+)").find(trimmed)
        val accountMatch = Regex("(?:账号|用户名|手机号?|邮箱)[：:]([^\\s]+)").find(trimmed)
        val passwordMatch = Regex("(?:密码|口令)[：:]([^\\s]+)").find(trimmed)
        val notesMatch = Regex("(?:备注|说明)[：:](.+)").find(trimmed)

        if (titleMatch != null && (accountMatch != null || passwordMatch != null)) {
            val t = titleMatch.groupValues[1].trim()
            val a = accountMatch?.groupValues?.get(1)?.trim() ?: ""
            val p = passwordMatch?.groupValues?.get(1)?.trim() ?: ""
            val n = notesMatch?.groupValues?.get(1)?.trim() ?: ""
            return ParsedAccount(
                title = t,
                accountName = a,
                password = p,
                notes = n,
                category = inferCategory(t, n),
                rawText = trimmed
            )
        }

        // 2. Check if line has Title separated by a colon: e.g. 平台：账号——密码——备注
        val colonIndex = trimmed.indexOfFirst { it == '：' || it == ':' }
        if (colonIndex > 0 && !isUrlProtocol(trimmed, colonIndex)) {
            val titleCandidate = trimmed.substring(0, colonIndex).trim()
            val rest = trimmed.substring(colonIndex + 1).trim()

            // If prefix is literally just the word "平台" or "名称", treat rest as the whole payload
            if (titleCandidate == "平台" || titleCandidate == "名称" || titleCandidate == "应用") {
                return parseLineWithoutColon(rest, trimmed)
            }

            val tokens = splitTokens(rest)
            val title = titleCandidate
            val accountName: String
            val password: String
            val notes: String

            when (tokens.size) {
                0 -> {
                    accountName = ""
                    password = ""
                    notes = ""
                }
                1 -> {
                    accountName = tokens[0]
                    password = ""
                    notes = ""
                }
                2 -> {
                    accountName = tokens[0]
                    password = tokens[1]
                    notes = ""
                }
                3 -> {
                    // Canonical pattern: 账号——密码——备注
                    accountName = tokens[0]
                    password = tokens[1]
                    notes = tokens[2]
                }
                else -> {
                    accountName = tokens[0]
                    password = tokens[1]
                    notes = tokens.subList(2, tokens.size).joinToString(" —— ")
                }
            }

            return ParsedAccount(
                title = title,
                accountName = accountName,
                password = password,
                notes = notes,
                category = inferCategory(title, notes),
                rawText = trimmed
            )
        }

        // 3. Line without colon: e.g. 平台——账号——密码——备注 or 平台\t账号\t密码\t备注
        return parseLineWithoutColon(trimmed, trimmed)
    }

    private fun parseLineWithoutColon(text: String, originalLine: String): ParsedAccount {
        val tokens = splitTokens(text)
        val title: String
        val accountName: String
        val password: String
        val notes: String

        when (tokens.size) {
            0 -> {
                title = ""
                accountName = ""
                password = ""
                notes = ""
            }
            1 -> {
                title = tokens[0]
                accountName = ""
                password = ""
                notes = ""
            }
            2 -> {
                title = tokens[0]
                accountName = tokens[1]
                password = ""
                notes = ""
            }
            3 -> {
                title = tokens[0]
                accountName = tokens[1]
                password = tokens[2]
                notes = ""
            }
            4 -> {
                title = tokens[0]
                accountName = tokens[1]
                password = tokens[2]
                notes = tokens[3]
            }
            else -> {
                title = tokens[0]
                accountName = tokens[1]
                password = tokens[2]
                notes = tokens.subList(3, tokens.size).joinToString(" —— ")
            }
        }

        return ParsedAccount(
            title = title,
            accountName = accountName,
            password = password,
            notes = notes,
            category = inferCategory(title, notes),
            rawText = originalLine
        )
    }

    /**
     * Splits string using the most prominent delimiter.
     */
    private fun splitTokens(str: String): List<String> {
        val s = str.trim()
        if (s.isEmpty()) return emptyList()

        return when {
            s.contains("——") -> s.split("——")
            s.contains("—") -> s.split("—")
            s.contains("----") -> s.split("----")
            s.contains("---") -> s.split("---")
            s.contains("--") -> s.split("--")
            s.contains("\t") -> s.split("\t")
            s.contains("|") -> s.split("|")
            s.contains("，") || s.contains(",") -> s.split(Regex("[,，]"))
            s.contains(" / ") -> s.split(" / ")
            s.contains("/") && !s.contains("://") -> s.split("/")
            s.contains(Regex("\\s{2,}")) -> s.split(Regex("\\s{2,}"))
            s.contains(" ") -> s.split(Regex("\\s+"))
            else -> listOf(s)
        }.map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun isUrlProtocol(line: String, colonIndex: Int): Boolean {
        // e.g. http:// or https://
        val sub = line.substring(colonIndex)
        return sub.startsWith("://") || sub.startsWith(": //")
    }
}
