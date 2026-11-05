package com.kuswa.clashtimer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kuswa.clashtimer.core.AppSettings
import com.kuswa.clashtimer.core.AppState
import com.kuswa.clashtimer.core.NameMap
import com.kuswa.clashtimer.core.SyncLog
import com.kuswa.clashtimer.core.SyncLogEntry
import com.kuswa.clashtimer.core.UpgradeTimes
import com.kuswa.clashtimer.i18n.AppLocale
import com.kuswa.clashtimer.i18n.texts
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "clash_timers")

/**
 * All persisted state lives in one Preferences DataStore: [AppState] and [AppSettings],
 * each stored as a JSON string under its own key.
 */
class StateRepository(context: Context) {
    private val store = context.applicationContext.dataStore

    val state: Flow<AppState> = store.data.map { AppState.decode(it[STATE]) }.distinctUntilChanged()
    val settings: Flow<AppSettings> = store.data.map { AppSettings.decode(it[SETTINGS]) }.distinctUntilChanged()
    val syncLog: Flow<SyncLog> = store.data.map { SyncLog.decode(it[SYNC_LOG]) }.distinctUntilChanged()

    suspend fun currentState(): AppState = state.first()
    suspend fun currentSettings(): AppSettings = settings.first()

    /** Atomically read-modify-write the app state; returns the value [transform] produced. */
    suspend fun <R> updateState(transform: (AppState) -> Pair<AppState, R>): R {
        var result: R? = null
        store.edit { prefs ->
            val (next, r) = transform(AppState.decode(prefs[STATE]))
            prefs[STATE] = next.encode()
            result = r
        }
        @Suppress("UNCHECKED_CAST")
        return result as R
    }

    /** Appends to the on-screen sync log; never throws (it is diagnostics only). */
    suspend fun log(source: String, ok: Boolean?, message: String) {
        runCatching {
            store.edit { prefs ->
                val entry = SyncLogEntry(System.currentTimeMillis() / 1000, source, ok, message)
                prefs[SYNC_LOG] = SyncLog.decode(prefs[SYNC_LOG]).add(entry).encode()
            }
        }
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs -> prefs[SETTINGS] = transform(AppSettings.decode(prefs[SETTINGS])).encode() }
    }

    private companion object {
        val STATE = stringPreferencesKey("state_json")
        val SETTINGS = stringPreferencesKey("settings_json")
        val SYNC_LOG = stringPreferencesKey("sync_log_json")
    }
}

/**
 * Bundled `assets/names.json` plus the translation overlay `assets/names.zh.json`, loaded once per
 * process and language (the language can change at runtime).
 */
object Names {
    @Volatile private var cached: Pair<String, NameMap>? = null

    fun get(context: Context): NameMap {
        val tag = AppLocale.languageTag(context)
        cached?.takeIf { it.first == tag }?.let { return it.second }
        return synchronized(this) {
            cached?.takeIf { it.first == tag }?.second ?: build(context, tag).also { cached = tag to it }
        }
    }

    private fun build(context: Context, tag: String): NameMap {
        val assets = context.applicationContext.assets
        val base = assets.open("names.json").bufferedReader().use { it.readText() }
        val overlay = if (tag == AppLocale.CHINESE) {
            runCatching { assets.open("names.zh.json").bufferedReader().use { it.readText() } }.getOrNull()
        } else {
            null
        }
        return NameMap.fromJson(base, overlay, context.texts())
    }
}

/** Bundled `assets/upgrade_times.json` (full upgrade durations for the progress bars), loaded once. */
object Times {
    @Volatile private var cached: UpgradeTimes? = null

    fun get(context: Context): UpgradeTimes = cached ?: synchronized(this) {
        cached ?: runCatching {
            context.applicationContext.assets.open("upgrade_times.json").bufferedReader().use {
                UpgradeTimes.fromJson(it.readText())
            }
        }.getOrDefault(UpgradeTimes.EMPTY).also { cached = it }
    }
}
