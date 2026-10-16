package com.keyfortress.app

import com.keyfortress.app.data.model.CategoryConstants
import com.keyfortress.app.util.SmartTextParser
import org.junit.Assert.assertEquals
import org.junit.Test

class SmartTextParserTest {

    @Test
    fun testDashDelimitedExamples() {
        val input = """
            微信：13800138000——WxPass#2026——常用社交账号
            公司堡垒机：dev_admin——Fortress@Secure99
            招商银行：6225880123456789——Bank#Pwd888——常用储蓄卡
        """.trimIndent()

        val parsed = SmartTextParser.parse(input)
        assertEquals(3, parsed.size)

        // Item 1: 微信
        val item1 = parsed[0]
        assertEquals("微信", item1.title)
        assertEquals("13800138000", item1.accountName)
        assertEquals("WxPass#2026", item1.password)
        assertEquals("常用社交账号", item1.notes)
        assertEquals(CategoryConstants.SOCIAL, item1.category)

        // Item 2: 公司堡垒机
        val item2 = parsed[1]
        assertEquals("公司堡垒机", item2.title)
        assertEquals("dev_admin", item2.accountName)
        assertEquals("Fortress@Secure99", item2.password)
        assertEquals("", item2.notes)
        assertEquals(CategoryConstants.WORK, item2.category)

        // Item 3: 招商银行
        val item3 = parsed[2]
        assertEquals("招商银行", item3.title)
        assertEquals("6225880123456789", item3.accountName)
        assertEquals("Bank#Pwd888", item3.password)
        assertEquals("常用储蓄卡", item3.notes)
        assertEquals(CategoryConstants.FINANCE, item3.category)
    }

    @Test
    fun testTabSeparatedAndSpaceSeparated() {
        val input = """
            淘宝	13812345678	TbPass123	常用买东西
            微博 13987654321 WbPassword 个人微博
        """.trimIndent()

        val parsed = SmartTextParser.parse(input)
        assertEquals(2, parsed.size)

        assertEquals("淘宝", parsed[0].title)
        assertEquals("13812345678", parsed[0].accountName)
        assertEquals("TbPass123", parsed[0].password)
        assertEquals("常用买东西", parsed[0].notes)

        assertEquals("微博", parsed[1].title)
        assertEquals("13987654321", parsed[1].accountName)
        assertEquals("WbPassword", parsed[1].password)
        assertEquals("个人微博", parsed[1].notes)
        assertEquals(CategoryConstants.SOCIAL, parsed[1].category)
    }

    @Test
    fun testBlockFormat() {
        val input = """
            平台：哔哩哔哩
            账号：bili_user
            密码：mypassword
            备注：追番账号
            ---
            平台：Steam
            账号：steam_gamer
            密码：steam_secret
        """.trimIndent()

        val parsed = SmartTextParser.parse(input)
        assertEquals(2, parsed.size)

        assertEquals("哔哩哔哩", parsed[0].title)
        assertEquals("bili_user", parsed[0].accountName)
        assertEquals("mypassword", parsed[0].password)
        assertEquals("追番账号", parsed[0].notes)
        assertEquals(CategoryConstants.ENTERTAINMENT, parsed[0].category)

        assertEquals("Steam", parsed[1].title)
        assertEquals("steam_gamer", parsed[1].accountName)
        assertEquals("steam_secret", parsed[1].password)
        assertEquals(CategoryConstants.ENTERTAINMENT, parsed[1].category)
    }

    @Test
    fun testInlineLabelsFormat() {
        val line = "平台:GitHub 账号:octocat 密码:github_token 备注:个人开源"
        val parsed = SmartTextParser.parseLine(line)
        assertEquals("GitHub", parsed.title)
        assertEquals("octocat", parsed.accountName)
        assertEquals("github_token", parsed.password)
        assertEquals("个人开源", parsed.notes)
        assertEquals(CategoryConstants.WORK, parsed.category)
    }

    @Test
    fun testGoogleCsvImportStandard() {
        val csv = """
            name,url,username,password,note
            Google,https://accounts.google.com,testuser@gmail.com,MySecretPass123,
            GitHub,https://github.com/login,octocat,ghp_mytoken456,开源工作开发账号
            "Taobao, Inc.",https://www.taobao.com,tb_shopper,TbPassword888,
        """.trimIndent()

        val parsed = SmartTextParser.parse(csv)
        assertEquals(3, parsed.size)

        // Item 1: Google
        assertEquals("Google", parsed[0].title)
        assertEquals("testuser@gmail.com", parsed[0].accountName)
        assertEquals("MySecretPass123", parsed[0].password)
        assertEquals("https://accounts.google.com", parsed[0].notes)

        // Item 2: GitHub (note merged with url)
        assertEquals("GitHub", parsed[1].title)
        assertEquals("octocat", parsed[1].accountName)
        assertEquals("ghp_mytoken456", parsed[1].password)
        assertEquals("https://github.com/login\n开源工作开发账号", parsed[1].notes)
        assertEquals(CategoryConstants.WORK, parsed[1].category)

        // Item 3: Taobao with quotes and comma
        assertEquals("Taobao, Inc.", parsed[2].title)
        assertEquals("tb_shopper", parsed[2].accountName)
        assertEquals("TbPassword888", parsed[2].password)
        assertEquals("https://www.taobao.com", parsed[2].notes)
    }

    @Test
    fun testGoogleCsvMissingNameFallbackToUrl() {
        val csv = """
            name,url,username,password
            ,https://bilibili.com/login,bili_user,bili_pass
        """.trimIndent()

        val parsed = SmartTextParser.parse(csv)
        assertEquals(1, parsed.size)
        assertEquals("bilibili.com", parsed[0].title)
        assertEquals("bili_user", parsed[0].accountName)
        assertEquals("bili_pass", parsed[0].password)
        assertEquals("https://bilibili.com/login", parsed[0].notes)
    }

    @Test
    fun testCategoryMatchingWithoutEntertainment() {
        val categoriesWithoutEntertainment = listOf("社交", "金融", "工作", "其它")

        // When entertainment is NOT in validCategories, bilibili should fall back to 其它
        val cat1 = SmartTextParser.inferCategory("哔哩哔哩", validCategories = categoriesWithoutEntertainment)
        assertEquals("其它", cat1)

        // When entertainment IS present, it should infer 娱乐
        val categoriesWithEntertainment = listOf("社交", "金融", "工作", "娱乐", "其它")
        val cat2 = SmartTextParser.inferCategory("哔哩哔哩", validCategories = categoriesWithEntertainment)
        assertEquals("娱乐", cat2)

        // In Google CSV parsing with category constraint
        val csv = """
            name,url,username,password
            哔哩哔哩,https://bilibili.com,user1,pass1
            微信,https://weixin.qq.com,user2,pass2
        """.trimIndent()

        val parsed = SmartTextParser.parse(csv, validCategories = categoriesWithoutEntertainment)
        assertEquals(2, parsed.size)
        assertEquals("其它", parsed[0].category)
        assertEquals("社交", parsed[1].category)
    }
}
