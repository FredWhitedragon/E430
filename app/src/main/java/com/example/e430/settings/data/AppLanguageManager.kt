package com.example.e430.settings.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.core.content.edit
import com.example.e430.settings.model.AppLanguage
import java.util.Locale

/** Applies the locale before Activity creation, where asynchronous DataStore reads are unavailable. */
object AppLanguageManager {
    private const val PREFERENCES_NAME = "app_language"
    private const val LANGUAGE_TAG = "language_tag"

    fun wrapContext(context: Context): Context {
        val language = selectedLanguage(context)
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(language.languageTag))
            setLocales(LocaleList(Locale.forLanguageTag(language.languageTag)))
        }
        return context.createConfigurationContext(configuration)
    }

    fun selectedLanguage(context: Context): AppLanguage {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val stored = preferences.getString(LANGUAGE_TAG, null)
        AppLanguage.entries.firstOrNull { it.languageTag == stored }?.let { return it }

        val selected = chooseInitialLanguage(
            context.resources.configuration.locales.toList(),
        )
        save(preferences, selected)
        return selected
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        save(context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE), language)
    }

    @SuppressLint("ApplySharedPref")
    private fun save(preferences: android.content.SharedPreferences, language: AppLanguage) {
        // The value must reach disk before Activity recreation so attachBaseContext reads it.
        preferences.edit(commit = true) { putString(LANGUAGE_TAG, language.languageTag) }
    }

    internal fun chooseInitialLanguage(systemLocales: List<Locale>): AppLanguage {
        systemLocales.forEach { locale ->
            when {
                locale.language.equals("en", ignoreCase = true) -> return AppLanguage.English
                locale.isSimplifiedChinese() -> return AppLanguage.SimplifiedChinese
            }
        }
        return AppLanguage.English
    }

    private fun Locale.isSimplifiedChinese(): Boolean {
        if (!language.equals("zh", ignoreCase = true)) return false
        if (script.equals("Hans", ignoreCase = true)) return true
        if (script.equals("Hant", ignoreCase = true)) return false
        return country.uppercase(Locale.ROOT) !in setOf("TW", "HK", "MO")
    }

    private fun LocaleList.toList(): List<Locale> = List(size()) { index -> get(index) }
}
