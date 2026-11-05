package com.kuswa.clashtimer.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.kuswa.clashtimer.R
import com.kuswa.clashtimer.crash.CrashActivity
import com.kuswa.clashtimer.crash.CrashReport
import com.kuswa.clashtimer.crash.CrashReporter

/** Diagnostics: the last saved crash report (crash/CrashReporter.kt), with Copy / Show / Clear. */
@Composable
fun CrashSection() {
    val context = LocalContext.current
    var report by remember { mutableStateOf(CrashReporter.lastReport(context)) }
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.last_crash), fontWeight = FontWeight.Bold)
        val r = report
        if (r == null) {
            Text(stringResource(R.string.none_saved), style = MaterialTheme.typography.bodySmall)
        } else {
            Text(
                "${CrashReport.time(r) ?: "?"}\n${CrashReport.summary(r)}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    context.getSystemService(ClipboardManager::class.java)
                        ?.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.crash_label), r))
                }) { Text(stringResource(R.string.copy)) }
                OutlinedButton(onClick = { expanded = !expanded }) {
                    Text(stringResource(if (expanded) R.string.hide else R.string.show))
                }
                OutlinedButton(onClick = {
                    CrashReporter.clear(context)
                    report = null
                    expanded = false
                }) { Text(stringResource(R.string.clear)) }
            }
            if (expanded) {
                SelectionContainer(Modifier.horizontalScroll(rememberScrollState())) {
                    Text(r, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, softWrap = false)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (r != null) {
                TextButton(onClick = { context.startActivity(CrashActivity.intent(context)) }) {
                    Text(stringResource(R.string.open_crash_screen))
                }
            }
            // Lets the player check that the crash screen opens on their phone (no adb to check it otherwise).
            val testMessage = stringResource(R.string.test_crash_message)
            TextButton(onClick = { throw RuntimeException(testMessage) }) {
                Text(stringResource(R.string.test_crash_screen))
            }
        }
    }
}
