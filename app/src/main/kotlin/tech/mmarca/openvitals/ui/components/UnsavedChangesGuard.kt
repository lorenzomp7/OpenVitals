package tech.mmarca.openvitals.ui.components

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R

/**
 * The one rule for every screen that edits something. Back with nothing
 * changed simply goes back. Back with changes asks Save or Discard: Save runs
 * [onSave], which saves and calls `leave` once the save went through; Discard
 * leaves without saving; tapping outside stays on the screen.
 *
 * The app bar arrow goes through the back dispatcher, so it is guarded too.
 */
@Composable
fun UnsavedChangesBackHandler(
    hasChanges: Boolean,
    onSave: (leave: () -> Unit) -> Unit,
    canSave: Boolean = true,
    /** Leaves without saving; by default, the back press the guard held. */
    onDiscard: (() -> Unit)? = null,
) {
    var asking by rememberSaveable { mutableStateOf(false) }
    var leaving by rememberSaveable { mutableStateOf(false) }
    var discarded by rememberSaveable { mutableStateOf(false) }
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    BackHandler(enabled = hasChanges && !leaving) { asking = true }

    // Discard lets the held back press through, once this guard has stepped aside.
    LaunchedEffect(leaving) {
        if (leaving) {
            if (onDiscard != null && discarded) onDiscard() else dispatcher?.onBackPressed()
            discarded = false
            leaving = false
        }
    }

    if (asking && hasChanges) {
        AlertDialog(
            onDismissRequest = { asking = false },
            title = { Text(stringResource(R.string.unsaved_changes_title)) },
            confirmButton = {
                TextButton(
                    enabled = canSave,
                    onClick = {
                        asking = false
                        onSave { leaving = true }
                    },
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        asking = false
                        discarded = true
                        leaving = true
                    },
                ) { Text(stringResource(R.string.action_discard)) }
            },
        )
    }
}

/**
 * Whether a form differs from how it first stood once [ready]: the first ready
 * [signature] is the baseline. Pass only the fields the user edits.
 */
@Composable
fun <T> formHasChanges(signature: T, ready: Boolean): Boolean {
    val baseline = androidx.compose.runtime.remember { arrayOfNulls<Any?>(1) }
    val captured = androidx.compose.runtime.remember { booleanArrayOf(false) }
    if (ready && !captured[0]) {
        baseline[0] = signature
        captured[0] = true
    }
    return captured[0] && baseline[0] != signature
}
