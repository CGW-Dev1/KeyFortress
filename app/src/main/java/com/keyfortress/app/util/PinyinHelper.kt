package com.keyfortress.app.util

/**
 * Lightweight Pinyin helper for Chinese character search indexing.
 * Provides pinyin matching and initial letter extraction without heavy third-party dependencies.
 * Supports matching queries like "wx" -> "微信", "dy" -> "抖音", "xhs" -> "小红书", "zfb" -> "支付宝".
 */
object PinyinHelper {

    // Common Chinese characters to Pinyin initials mapping
    private val PINYIN_TABLE: Map<Char, String> = mapOf(
        '微' to "wei", '信' to "xin", '抖' to "dou", '音' to "yin",
        '小' to "xiao", '红' to "hong", '书' to "shu", '支' to "zhi",
        '付' to "fu", '宝' to "bao", '淘' to "tao", '猫' to "mao",
        '京' to "jing", '东' to "dong", '美' to "mei", '团' to "tuan",
        '拼' to "pin", '多' to "duo", '百' to "bai", '度' to "du",
        '知' to "zhi", '乎' to "hu", '微' to "wei", '博' to "bo",
        '哔' to "bi", '哩' to "li", '网' to "wang", '易' to "yi",
        '云' to "yun", '音' to "yin", '乐' to "le", '酷' to "ku",
        '狗' to "gou", '腾' to "teng", '讯' to "xun", '会' to "hui",
        '议' to "yi", '钉' to "ding", '企' to "qi", '业' to "ye",
        '飞' to "fei", '书' to "shu", '高' to "gao", '德' to "de",
        '地' to "di", '图' to "tu", '滴' to "di", '饿' to "e",
        '了' to "le", '么' to "me", '携' to "xie", '程' to "cheng",
        '同' to "tong", '去' to "qu", '哪' to "na", '儿' to "er",
        '招' to "zhao", '商' to "shang", '银' to "yin", '行' to "hang",
        '建' to "jian", '设' to "she", '工' to "gong", '农' to "nong",
        '中' to "zhong", '国' to "guo", '交' to "jiao", '通' to "tong",
        '邮' to "you", '储' to "chu", '浦' to "pu", '发' to "fa",
        '民' to "min", '生' to "sheng", '平' to "ping", '安' to "an",
        '华' to "hua", '夏' to "xia", '兴' to "xing", '广' to "guang",
        '浙' to "zhe", '注' to "zhu", '册' to "ce", '账' to "zhang",
        '号' to "hao", '密' to "mi", '码' to "ma", '邮' to "you",
        '箱' to "xiang", '手' to "shou", '机' to "ji", '公' to "gong",
        '司' to "si", '客' to "ke", '户' to "hu", '端' to "duan",
        '谷' to "gu", '歌' to "ge", '微' to "wei", '软' to "ruan",
        '苹' to "ping", '果' to "guo", '推' to "tui", '特' to "te",
        '脸' to "lian", '脸' to "lian", '推' to "tui", '电' to "dian",
        '脑' to "nao", '服' to "fu", '务' to "wu", '器' to "qi",
        '路' to "lu", '由' to "you", '器' to "qi", '宽' to "kuan",
        '带' to "dai", '校' to "xiao", '园' to "yuan", '学' to "xue"
    )

    // Standard Chinese GB2312 boundaries for general pinyin first letters
    private val GB_PINYIN_LETTERS = charArrayOf(
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'w', 'x', 'y', 'z'
    )
    private val GB_PINYIN_BOUNDS = intArrayOf(
        45217, 45253, 45761, 46318, 46826, 47010, 47297, 47614,
        48119, 49062, 49324, 49896, 50371, 50614, 50622, 50906,
        51387, 51446, 52218, 52698, 52980, 53689, 54481
    )

    /**
     * Converts a single character to pinyin string if known.
     */
    fun getPinyin(ch: Char): String {
        if (ch in 'a'..'z') return ch.toString()
        if (ch in 'A'..'Z') return ch.lowercase()
        if (ch in '0'..'9') return ch.toString()

        PINYIN_TABLE[ch]?.let { return it }

        // Approximate with GB2312 pinyin initial letter
        val initial = getInitial(ch)
        return if (initial != ' ') initial.toString() else ch.toString()
    }

    /**
     * Returns the initial letter of a character (lowercase).
     */
    fun getInitial(ch: Char): Char {
        if (ch in 'a'..'z') return ch
        if (ch in 'A'..'Z') return ch.lowercaseChar()
        if (ch in '0'..'9') return ch

        PINYIN_TABLE[ch]?.firstOrNull()?.let { return it }

        // Fallback for general Chinese characters
        val code = getCharacterCode(ch)
        if (code < GB_PINYIN_BOUNDS[0] || code > 54480) {
            return ch.lowercaseChar()
        }
        for (i in GB_PINYIN_BOUNDS.indices.reversed()) {
            if (code >= GB_PINYIN_BOUNDS[i]) {
                return GB_PINYIN_LETTERS[i]
            }
        }
        return ch.lowercaseChar()
    }

    private fun getCharacterCode(ch: Char): Int {
        return try {
            val bytes = ch.toString().toByteArray(charset("GB2312"))
            if (bytes.size >= 2) {
                ((bytes[0].toInt() + 256) and 0xFF) * 256 + ((bytes[1].toInt() + 256) and 0xFF)
            } else {
                ch.code
            }
        } catch (_: Exception) {
            ch.code
        }
    }

    /**
     * Returns a composite search token containing:
     * - Original text in lowercase
     * - Pinyin initials (e.g. "wx" for "微信")
     * - Full pinyin without spaces (e.g. "weixin" for "微信")
     */
    fun getSearchTokens(text: String): String {
        if (text.isBlank()) return ""
        val initials = StringBuilder()
        val fullPinyin = StringBuilder()

        for (ch in text) {
            if (ch.isWhitespace()) continue
            initials.append(getInitial(ch))
            fullPinyin.append(getPinyin(ch))
        }

        return "${text.lowercase()} ${initials} ${fullPinyin}"
    }

    /**
     * Checks if a target string matches a query using direct substring, initials, or pinyin.
     */
    fun matches(target: String, query: String): Boolean {
        if (query.isBlank()) return true
        val cleanQuery = query.trim().lowercase()
        val cleanTarget = target.lowercase()

        if (cleanTarget.contains(cleanQuery)) return true

        val searchTokens = getSearchTokens(target)
        return searchTokens.contains(cleanQuery)
    }
}
