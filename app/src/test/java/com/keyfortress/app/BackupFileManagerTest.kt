package com.keyfortress.app

import com.keyfortress.app.util.BackupFileManager
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.charset.Charset

class BackupFileManagerTest {

    @Test
    fun testDecodeBytes_utf8() {
        val original = "name,url,username,password\nGoogle,https://accounts.google.com,test@gmail.com,123456"
        val bytes = original.toByteArray(Charsets.UTF_8)
        val decoded = BackupFileManager.decodeBytesToText(bytes)
        assertEquals(original, decoded)
    }

    @Test
    fun testDecodeBytes_utf8WithBom() {
        val original = "name,url,username,password\nGitHub,https://github.com,testuser,gitpass"
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val bytes = bom + original.toByteArray(Charsets.UTF_8)
        val decoded = BackupFileManager.decodeBytesToText(bytes)
        assertEquals(original, decoded)
    }

    @Test
    fun testDecodeBytes_gbk() {
        val original = "名称,网址,账号,密码\n百度,https://baidu.com,13800000000,baidu123"
        val gbk = Charset.forName("GBK")
        val bytes = original.toByteArray(gbk)
        val decoded = BackupFileManager.decodeBytesToText(bytes)
        assertEquals(original, decoded)
    }

    @Test
    fun testDecodeBytes_empty() {
        val decoded = BackupFileManager.decodeBytesToText(byteArrayOf())
        assertEquals("", decoded)
    }
}
