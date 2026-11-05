package com.kuswa.clashtimer.i18n

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * The in-app language choice ("跟随系统 / 简体中文 / English" on the settings screen).
 *
 * Stored in SharedPreferences rather than in the DataStore [com.kuswa.clashtimer.core.AppSettings]
 * because it has to be readable synchronously in `attachBaseContext`, before any coroutine or
 * DataStore read could run.
 *
 * [wrap] returns a context whose resources use the chosen locale. Activities call it from
 * `attachBaseContext`; anything that renders the widget or posts a notification (which run from a
 * plain application/receiver context) calls [localized] or `context.texts()` instead.
 */
object AppLocale {
    const val SYSTEM = "system"
    const val CHINESE = "zh"
    const val ENGLISH = "en"

    private const val PREFS = "clash_timers_locale"
    private const val KEY = "language"

    /** One of [SYSTEM], [CHINESE], [ENGLISH]. */
    fun preference(context: Context): String =
        prefs(context).getString(KEY, SYSTEM) ?: SYSTEM

    fun setPreference(context: Context, value: String) {
        prefs(context).edit().putString(KEY, value).apply()
    }

    /** The forced locale, or null to follow the system. */
    fun localeFor(context: Context): Locale? = when (preference(context)) {
        CHINESE -> Locale.SIMPLIFIED_CHINESE
        ENGLISH -> Locale.ENGLISH
        else -> null
    }

    /** Tag used to pick the bundled names file: `zh` or `en`. */
    fun languageTag(context: Context): String =
        if (localeFor(context)?.language == "zh") CHINESE else ENGLISH

    /** [base] with the chosen locale applied; [base] itself when following the system. */
    fun wrap(base: Context): Context {
        val locale = localeFor(base) ?: return base
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

/** [this] with the chosen locale applied, for code that runs outside an activity (widget, notifications). */
fun Context.localized(): Context = AppLocale.wrap(this)
