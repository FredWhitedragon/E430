package com.example.e430.settings.data

import com.example.e430.settings.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AppLanguageManagerTest {
    @Test
    fun usesFirstSupportedSystemLanguage() {
        assertEquals(
            AppLanguage.SimplifiedChinese,
            AppLanguageManager.chooseInitialLanguage(
                listOf(Locale.FRANCE, Locale.forLanguageTag("zh-Hans-CN"), Locale.ENGLISH),
            ),
        )
    }

    @Test
    fun fallsBackToEnglishWhenNoSystemLanguageIsSupported() {
        assertEquals(
            AppLanguage.English,
            AppLanguageManager.chooseInitialLanguage(listOf(Locale.FRANCE, Locale.JAPAN)),
        )
    }

    @Test
    fun traditionalChineseDoesNotSelectSimplifiedChinese() {
        assertEquals(
            AppLanguage.English,
            AppLanguageManager.chooseInitialLanguage(listOf(Locale.forLanguageTag("zh-Hant-TW"))),
        )
    }
}
