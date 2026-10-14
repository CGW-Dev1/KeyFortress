package com.keyfortress.app

import com.keyfortress.app.util.PinyinHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinyinHelperTest {

    @Test
    fun testPinyinInitials() {
        assertEquals('w', PinyinHelper.getInitial('微'))
        assertEquals('x', PinyinHelper.getInitial('信'))
        assertEquals('d', PinyinHelper.getInitial('抖'))
        assertEquals('y', PinyinHelper.getInitial('音'))
        assertEquals('z', PinyinHelper.getInitial('支'))
        assertEquals('f', PinyinHelper.getInitial('付'))
        assertEquals('b', PinyinHelper.getInitial('宝'))
    }

    @Test
    fun testSearchMatching() {
        assertTrue("wx should match 微信", PinyinHelper.matches("微信", "wx"))
        assertTrue("weixin should match 微信", PinyinHelper.matches("微信", "weixin"))
        assertTrue("dy should match 抖音", PinyinHelper.matches("抖音", "dy"))
        assertTrue("zfb should match 支付宝", PinyinHelper.matches("支付宝", "zfb"))
        assertTrue("git should match GitHub", PinyinHelper.matches("GitHub", "git"))
    }
}
