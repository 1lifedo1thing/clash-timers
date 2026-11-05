package com.kuswa.clashtimer.ui

import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kuswa.clashtimer.R

/**
 * About: version, offline note, Supercell's fan content disclaimer (supercell.com/fan-content-policy),
 * credits and the font licence. The disclaimer is translated, but keeps the policy URL as-is.
 */
@Composable
fun AboutSection() {
    val context = LocalContext.current
    val version = remember { appVersion(context) }
    var showLicence by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.about), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.about_version, version))
        Text(stringResource(R.string.offline_title), fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.offline_text), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.fan_content_disclaimer), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.credits), style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { showLicence = !showLicence }) {
            Text(stringResource(if (showLicence) R.string.hide_font_licence else R.string.show_font_licence))
        }
        if (showLicence) {
            val licence = remember { readAsset(context, "licenses/LilitaOne-OFL.txt") }
            Text(licence, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** `1.0 (1)` from the installed package. */
private fun appVersion(context: Context): String = runCatching {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
    "${info.versionName} ($code)"
}.getOrDefault("")

private fun readAsset(context: Context, path: String): String =
    runCatching { context.assets.open(path).bufferedReader().use { it.readText() } }
        .getOrDefault(context.getString(R.string.licence_missing))
