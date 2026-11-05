package com.kuswa.clashtimer.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kuswa.clashtimer.R
import com.kuswa.clashtimer.core.AppSettings
import com.kuswa.clashtimer.core.BoostSettings
import com.kuswa.clashtimer.core.GameIds
import com.kuswa.clashtimer.core.VillageExport
import com.kuswa.clashtimer.i18n.AppLocale
import com.kuswa.clashtimer.notify.Notifier
import com.kuswa.clashtimer.sync.SyncRunner
import kotlinx.coroutines.launch

private const val H = 3600L

/**
 * Settings on the launcher screen: language, boost amounts, helper interval, total builders,
 * the Goblin potion toggle and notifications. Every change is saved at once and the widget is
 * redrawn by [onChange]'s caller.
 */
@Composable
fun SettingsSection(
    settings: AppSettings,
    export: VillageExport?,
    /** The screen's 1-second clock, so the notification permission state is re-read after a trip to settings. */
    now: Long,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
) {
    val context = LocalContext.current
    val b = settings.boosts
    fun boosts(f: (BoostSettings) -> BoostSettings) = onChange { it.copy(boosts = f(it.boosts)) }

    // Re-read notification permission after the system dialog or a trip to the settings app.
    var permissionTick by remember { mutableIntStateOf(0) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionTick++ }
    val notificationsAllowed = remember(now, permissionTick) { Notifier.allowed(context) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

        LanguageRow()

        var editName by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.player_name))
                Text(
                    settings.playerName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.chief),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = { editName = true }) { Text(stringResource(R.string.change)) }
        }
        if (editName) {
            PlayerNameDialog(
                current = settings.playerName,
                firstLaunch = false,
                onDone = { name -> onChange { it.copy(playerName = name) }; editName = false },
                onDismiss = { editName = false },
            )
        }

        Text(stringResource(R.string.potions_heading), style = MaterialTheme.typography.labelLarge)
        HoursStepper(stringResource(R.string.builder_potion), b.builderPotionSavingSec, 1..47) { v ->
            boosts { it.copy(builderPotionSavingSec = v) }
        }
        HoursStepper(stringResource(R.string.builder_bite), b.builderBiteSavingSec, 1..23) { v ->
            boosts { it.copy(builderBiteSavingSec = v) }
        }
        HoursStepper(stringResource(R.string.research_potion), b.researchPotionSavingSec, 1..47) { v ->
            boosts { it.copy(researchPotionSavingSec = v) }
        }
        HoursStepper(stringResource(R.string.pet_potion), b.petPotionSavingSec, 1..47) { v ->
            boosts { it.copy(petPotionSavingSec = v) }
        }

        Text(stringResource(R.string.helpers_heading), style = MaterialTheme.typography.labelLarge)
        AutoHoursStepper(
            stringResource(R.string.apprentice_saving),
            b.apprenticeSavingSecOverride,
            auto = export?.helper(GameIds.HELPER_BUILDERS_APPRENTICE)?.level?.times(H),
            range = 1..24,
        ) { v -> boosts { it.copy(apprenticeSavingSecOverride = v) } }
        AutoHoursStepper(
            stringResource(R.string.lab_assistant_saving),
            b.labAssistantSavingSecOverride,
            auto = export?.helper(GameIds.HELPER_LAB_ASSISTANT)?.level?.times(H),
            range = 1..24,
        ) { v -> boosts { it.copy(labAssistantSavingSecOverride = v) } }
        Stepper(
            label = stringResource(R.string.helper_period),
            value = stringResource(R.string.helper_period_value, (b.helperPeriodSec / H).toInt()),
            hint = stringResource(R.string.helper_period_hint),
            onMinus = (b.helperPeriodSec - H).takeIf { it >= 2 * H }?.let { v -> { boosts { it.copy(helperPeriodSec = v) } } },
            onPlus = (b.helperPeriodSec + H).takeIf { it <= 48 * H }?.let { v -> { boosts { it.copy(helperPeriodSec = v) } } },
        )

        Text(stringResource(R.string.builders_heading), style = MaterialTheme.typography.labelLarge)
        val derived = export?.derivedTotalBuilders
        val total = settings.totalBuildersOverride
        Stepper(
            label = stringResource(R.string.total_builders),
            value = total?.toString() ?: stringResource(R.string.auto_value, derived ?: AppSettings.DEFAULT_TOTAL_BUILDERS),
            hint = if (total == null) stringResource(R.string.builders_hint) else null,
            onMinus = ((total ?: derived ?: AppSettings.DEFAULT_TOTAL_BUILDERS) - 1).takeIf { it >= 1 }
                ?.let { v -> { onChange { it.copy(totalBuildersOverride = v) } } },
            onPlus = ((total ?: derived ?: AppSettings.DEFAULT_TOTAL_BUILDERS) + 1).takeIf { it <= 10 }
                ?.let { v -> { onChange { it.copy(totalBuildersOverride = v) } } },
            onAuto = if (total != null) ({ onChange { it.copy(totalBuildersOverride = null) } }) else null,
        )
        SwitchRow(
            stringResource(R.string.boosts_goblin),
            b.boostsApplyToGoblin,
        ) { on -> boosts { it.copy(boostsApplyToGoblin = on) } }

        Text(stringResource(R.string.notifications_heading), style = MaterialTheme.typography.labelLarge)
        SwitchRow(stringResource(R.string.notify_free), settings.notificationsEnabled) { on ->
            onChange { it.copy(notificationsEnabled = on) }
            if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifier.allowed(context)) {
                askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (settings.notificationsEnabled && !notificationsAllowed) {
            Text(
                stringResource(R.string.notifications_blocked),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    OutlinedButton(onClick = { askPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                        Text(stringResource(R.string.allow))
                    }
                }
                OutlinedButton(onClick = { openNotificationSettings(context); permissionTick++ }) {
                    Text(stringResource(R.string.open_settings))
                }
            }
        }
        Text(
            stringResource(R.string.battery_hint),
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedButton(onClick = { openAppSettings(context) }) { Text(stringResource(R.string.app_settings_button)) }

        TextButton(onClick = {
            onChange { AppSettings(liveCountdown = it.liveCountdown, playerName = it.playerName) }
        }) { Text(stringResource(R.string.reset_settings)) }
    }
}

/**
 * Language picker: follow the system, or force 简体中文 / English. The choice is stored outside the
 * DataStore (see [AppLocale]) and applied by recreating the screen, then the widget and the
 * notifications are redrawn in the new language.
 */
@Composable
private fun LanguageRow() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf(AppLocale.preference(context)) }
    var open by remember { mutableStateOf(false) }

    fun apply(value: String) {
        current = value
        open = false
        AppLocale.setPreference(context, value)
        // The activity re-reads the locale in attachBaseContext; the widget reads resources itself.
        scope.launch { SyncRunner.refreshWidgets(context.applicationContext, "app") }
        (context as? Activity)?.recreate()
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.language))
            Text(languageLabel(current), style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = { open = true }) { Text(stringResource(R.string.change)) }
    }

    if (open) {
        val options = listOf(
            AppLocale.SYSTEM to stringResource(R.string.language_system),
            AppLocale.CHINESE to stringResource(R.string.language_zh),
            AppLocale.ENGLISH to stringResource(R.string.language_en),
        )
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(stringResource(R.string.language)) },
            text = {
                Column {
                    options.forEach { (value, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            RadioButton(selected = value == current, onClick = { apply(value) })
                            TextButton(onClick = { apply(value) }) { Text(label) }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun languageLabel(value: String): String = stringResource(
    when (value) {
        AppLocale.CHINESE -> R.string.language_zh
        AppLocale.ENGLISH -> R.string.language_en
        else -> R.string.language_system
    },
)

@Composable
private fun HoursStepper(label: String, seconds: Long, range: IntRange, onSet: (Long) -> Unit) {
    val h = (seconds / H).toInt()
    Stepper(
        label = label,
        value = stringResource(R.string.hours_value, h, h + 1),
        onMinus = (h - 1).takeIf { it in range }?.let { v -> { onSet(v * H) } },
        onPlus = (h + 1).takeIf { it in range }?.let { v -> { onSet(v * H) } },
    )
}

/** null = automatic (helper level in hours from the export). */
@Composable
private fun AutoHoursStepper(label: String, override: Long?, auto: Long?, range: IntRange, onSet: (Long?) -> Unit) {
    val current = ((override ?: auto ?: 0L) / H).toInt()
    Stepper(
        label = label,
        value = if (override == null) {
            stringResource(
                R.string.auto_hours,
                auto?.let { stringResource(R.string.helper_period_value, (it / H).toInt()) }
                    ?: stringResource(R.string.level),
            )
        } else {
            stringResource(R.string.helper_period_value, current)
        },
        hint = if (override == null) stringResource(R.string.helper_level_hint) else null,
        onMinus = (current - 1).takeIf { it in range }?.let { v -> { onSet(v * H) } },
        onPlus = (current + 1).takeIf { it in range }?.let { v -> { onSet(v * H) } },
        onAuto = if (override != null) ({ onSet(null) }) else null,
    )
}

@Composable
private fun Stepper(
    label: String,
    value: String,
    onMinus: (() -> Unit)?,
    onPlus: (() -> Unit)?,
    hint: String? = null,
    onAuto: (() -> Unit)? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(label)
            hint?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        if (onAuto != null) TextButton(onClick = onAuto) { Text(stringResource(R.string.auto)) }
        // ASCII "-" / "+": the phone's font mangles some symbols.
        TextButton(onClick = { onMinus?.invoke() }, enabled = onMinus != null) { Text("-", fontWeight = FontWeight.Bold) }
        Text(value, textAlign = TextAlign.Center, modifier = Modifier.width(96.dp))
        TextButton(onClick = { onPlus?.invoke() }, enabled = onPlus != null) { Text("+", fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }.onFailure { openAppSettings(context) }
}

fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
