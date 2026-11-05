package com.kuswa.clashtimer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.kuswa.clashtimer.R
import com.kuswa.clashtimer.core.AppSettings

/**
 * Asks for the name shown on the widget header. The village export has no player name, so the app
 * asks on first launch; it can be changed later in Settings. Empty = "Chief".
 *
 * [onDone] gets the cleaned name ("" for Chief). Dismissing without saving also counts as
 * answered on first launch, so the prompt doesn't come back on every start.
 */
@Composable
fun PlayerNameDialog(current: String?, firstLaunch: Boolean, onDone: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current.orEmpty()) }
    // "Chief" in English, 首领 in Chinese: the header name the widget falls back to.
    val defaultName = stringResource(R.string.chief)
    val save = { onDone(AppSettings.cleanName(text)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (firstLaunch) R.string.welcome_chief else R.string.player_name_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.name_question, defaultName))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(AppSettings.MAX_PLAYER_NAME) },
                    singleLine = true,
                    placeholder = { Text(defaultName) },
                    supportingText = {
                        Text("${text.length}/${AppSettings.MAX_PLAYER_NAME}", style = MaterialTheme.typography.bodySmall)
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { save() }),
                )
            }
        },
        confirmButton = { TextButton(onClick = save) { Text(stringResource(R.string.save)) } },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(if (firstLaunch) R.string.skip else R.string.cancel))
            }
        },
    )
}
