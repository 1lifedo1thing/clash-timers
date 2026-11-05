package com.kuswa.clashtimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.ui.res.stringResource
import com.kuswa.clashtimer.R
import com.kuswa.clashtimer.i18n.AppLocale
import com.kuswa.clashtimer.i18n.texts
import com.kuswa.clashtimer.core.AppSettings
import com.kuswa.clashtimer.core.AppState
import com.kuswa.clashtimer.core.Countdown
import com.kuswa.clashtimer.core.RowButton
import com.kuswa.clashtimer.core.RowKind
import com.kuswa.clashtimer.core.SyncLog
import com.kuswa.clashtimer.core.SyncLogEntry
import com.kuswa.clashtimer.core.WidgetModel
import com.kuswa.clashtimer.core.WidgetModelBuilder
import com.kuswa.clashtimer.core.WidgetRow
import com.kuswa.clashtimer.data.Names
import com.kuswa.clashtimer.data.StateRepository
import com.kuswa.clashtimer.data.Times
import com.kuswa.clashtimer.sync.SyncRunner
import com.kuswa.clashtimer.ui.AboutSection
import com.kuswa.clashtimer.ui.CrashSection
import com.kuswa.clashtimer.ui.PlayerNameDialog
import com.kuswa.clashtimer.ui.SettingsSection
import com.kuswa.clashtimer.ui.SwitchRow
import com.kuswa.clashtimer.ui.theme.ClashTimerTheme
import com.kuswa.clashtimer.widget.WidgetAlarm
import com.kuswa.clashtimer.widget.WidgetIcons
import com.kuswa.clashtimer.widget.WidgetRenderer
import com.kuswa.clashtimer.widget.WidgetUpdater
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Launcher screen: sync from the clipboard inside the app and show, in plain text, what happened.
 * Players usually have no adb/logcat, so this screen is the diagnostics channel.
 */
class MainActivity : ComponentActivity() {
    /** Makes the chosen in-app language apply to every string on this screen. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClashTimerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    SyncScreen(Modifier.padding(padding))
                }
            }
        }
    }
}

private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM HH:mm:ss")
private fun fmt(epochSec: Long): String = TIME.format(Instant.ofEpochSecond(epochSec).atZone(ZoneId.systemDefault()))

@Composable
private fun SyncScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repo = remember { StateRepository(context) }
    val names = remember { Names.get(context) }
    val times = remember { Times.get(context) }
    val scope = rememberCoroutineScope()

    val state by repo.state.collectAsState(AppState())
    val settings by repo.settings.collectAsState(AppSettings())
    // null until DataStore has loaded, so the first-launch prompt doesn't flash for returning players.
    val storedSettings by repo.settings.collectAsState<AppSettings, AppSettings?>(null)
    val log by repo.syncLog.collectAsState(SyncLog())

    if (storedSettings?.needsPlayerName == true) {
        // First launch: ask for the widget's name. Skip / empty = "Chief"; stored as "" so it isn't asked again.
        val answer = { name: String ->
            scope.launch {
                repo.updateSettings { it.copy(playerName = name) }
                SyncRunner.refreshWidgets(context, "app")
            }
            Unit
        }
        PlayerNameDialog(current = null, firstLaunch = true, onDone = answer, onDismiss = { answer("") })
    }
    var busy by remember { mutableStateOf(false) }
    var widgetCount by remember { mutableIntStateOf(-1) }
    var widgetSizes by remember { mutableStateOf("") }
    var now by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis() / 1000
            widgetCount = runCatching { WidgetUpdater.widgetIds(context).size }.getOrDefault(-1)
            widgetSizes = runCatching { widgetSizeReport(context, state) }.getOrDefault("")
            delay(1_000)
        }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.sync_help))

        Button(
            onClick = {
                busy = true
                val text = SyncRunner.readClipboard(context) // read now, while this screen has focus
                scope.launch {
                    SyncRunner.sync(context, text, "app")
                    now = System.currentTimeMillis() / 1000
                    busy = false
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(if (busy) R.string.sync_busy else R.string.sync_button)) }

        // Last result, in plain text.
        val last = log.lastResult
        Text(stringResource(R.string.last_result), fontWeight = FontWeight.Bold)
        SelectionContainer {
            Text(
                if (last == null) {
                    stringResource(R.string.no_sync_yet)
                } else {
                    stringResource(
                        R.string.sync_result_line,
                        stringResource(if (last.ok == true) R.string.status_ok else R.string.status_failed),
                        fmt(last.at),
                        last.source,
                        SyncRunner.plainText(last.message),
                    )
                },
                color = if (last?.ok == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
        state.export?.let { Text(stringResource(R.string.export_copied_at, fmt(it.timestamp), it.tag)) }

        HorizontalDivider()
        SettingsSection(settings, state.export, now) { change ->
            scope.launch {
                repo.updateSettings(change)
                SyncRunner.refreshWidgets(context, "app")
            }
        }

        // ---- Diagnostics (no logcat on most phones): below the settings. ----
        HorizontalDivider()
        Text(stringResource(R.string.diagnostics), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        CrashSection()
        Text(stringResource(R.string.widget_section), fontWeight = FontWeight.Bold)
        Text(
            when (widgetCount) {
                -1 -> stringResource(R.string.widgets_unknown)
                0 -> stringResource(R.string.widgets_none)
                else -> stringResource(R.string.widgets_count, widgetCount)
            },
        )
        if (widgetSizes.isNotEmpty()) Text(widgetSizes, style = MaterialTheme.typography.bodySmall)
        Text(
            stringResource(
                R.string.diagnostics_icons,
                WidgetIcons.get(context).available.size,
                times.size,
                stringResource(
                    if (WidgetAlarm.canScheduleExact(context)) R.string.exact_allowed else R.string.exact_not_allowed,
                ),
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        if (!WidgetAlarm.canScheduleExact(context) && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            OutlinedButton(onClick = {
                runCatching {
                    context.startActivity(
                        android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            .setData(android.net.Uri.parse("package:${context.packageName}")),
                    )
                }
            }) { Text(stringResource(R.string.allow_exact_alarms)) }
        }
        SwitchRow(stringResource(R.string.live_countdown), settings.liveCountdown) { on ->
            scope.launch {
                repo.updateSettings { it.copy(liveCountdown = on) }
                SyncRunner.refreshWidgets(context, "app")
            }
        }
        OutlinedButton(
            onClick = { scope.launch { widgetCount = SyncRunner.refreshWidgets(context, "app") } },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.refresh_widget)) }

        Text(stringResource(R.string.should_show), fontWeight = FontWeight.Bold)
        SelectionContainer {
            Text(
                widgetPreview(WidgetModelBuilder.build(state, settings, names, times, now, context.texts())),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        HorizontalDivider()
        Text(stringResource(R.string.recent_activity), fontWeight = FontWeight.Bold)
        if (log.entries.isEmpty()) Text(stringResource(R.string.nothing_yet))
        SelectionContainer {
            Text(SyncRunner.plainText(log.entries.joinToString("\n") { it.line() }), style = MaterialTheme.typography.bodySmall)
        }

        HorizontalDivider()
        AboutSection()
        Spacer(Modifier.height(24.dp))
    }
}

private fun SyncLogEntry.line(): String {
    val mark = when (ok) { true -> "OK"; false -> "FAIL"; null -> "·" }
    return "${fmt(at)}  [$source] $mark  $message"
}

/** Each placed widget's size as the launcher reports it, and which row size it gets. */
private fun widgetSizeReport(context: android.content.Context, state: AppState): String {
    val manager = AppWidgetManager.getInstance(context)
    val rows = (state.export?.jobs?.size ?: 0) + 2
    return WidgetUpdater.widgetIds(context).joinToString("\n") { id ->
        val o = manager.getAppWidgetOptions(id)
        val w = WidgetRenderer.reportedWidthDp(o)
        val h = WidgetRenderer.reportedHeightDp(o)
        val size = if (w != null && h != null) context.getString(R.string.widget_size_reported, w, h)
        else context.getString(R.string.widget_size_unknown)
        val rowSize = context.getString(
            if (WidgetRenderer.useCompactRows(o, rows)) R.string.rows_compact else R.string.rows_normal,
        )
        context.getString(R.string.widget_size_line, id, size, rowSize, WidgetRenderer.normalHeightDp(rows))
    }
}

/** The widget model as text, so a rendering problem can be told apart from a data problem. */
private fun widgetPreview(m: WidgetModel): String = SyncRunner.plainText(buildString {
    appendLine("${m.title}   [Sync]")
    if (!m.hasData) {
        append(if (m.syncFailed) WidgetModelBuilder.SYNC_FAILED else WidgetModelBuilder.NO_DATA)
        return@buildString
    }
    fun line(r: WidgetRow) {
        val time = if (r.kind == RowKind.RUNNING) Countdown.text(r.remaining) else r.staticStatus
        val button = when (r.button) {
            RowButton.NONE -> ""
            RowButton.LINK -> " [+]"
            RowButton.APPRENTICE -> " [Apprentice]"
            RowButton.LAB_ASSISTANT -> " [Assistant]"
        }
        val goblin = if (r.goblin) "GOBLIN " else ""
        val label = if (r.kind == RowKind.RUNNING) r.label else r.name // finished rows: name only
        appendLine("  $goblin$label | $time | ${(r.progress * 100).toInt()}%$button")
    }
    appendLine("${m.buildersLabel}${if (m.goblinWorking) " + Goblin" else ""}   [${m.bite.text}] [${m.builderPotion.text}]")
    m.builderRows.forEach(::line)
    appendLine("LAB   [${m.researchPotion.text}]")
    line(m.labRow)
    appendLine("PETS   [${m.petPotion.text}]")
    line(m.petRow)
    append(m.footerLeft.joinToString("") { it.text }).append("   ").append(m.footerRight)
})
