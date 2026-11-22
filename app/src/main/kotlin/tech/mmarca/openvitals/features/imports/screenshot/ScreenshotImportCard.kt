package tech.mmarca.openvitals.features.imports.screenshot

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.features.manualentry.rememberManualEntryWritePermissionRequester
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

/**
 * A "+ log" tile, styled like the others. A tap opens the image
 * picker at once; the review opens only with a picked image.
 */
@Composable
internal fun ScreenshotImportCard(modifier: Modifier = Modifier) {
    var pickedUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    // Every pick is a review of its own, with state of its own: one review's "saved" or
    // "busy" can never hold up or close the next, however soon after it is opened.
    var pickCount by rememberSaveable { mutableIntStateOf(0) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { pickCount++; pickedUri = uri }
    }
    Column(modifier = modifier.fillMaxWidth()) {
    tech.mmarca.openvitals.ui.components.OpenVitalsCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { picker.launch(arrayOf("image/*")) },
        containerColor = tech.mmarca.openvitals.features.manualentry.manualEntryTileContainer(ScreenshotImportAccent),
        style = tech.mmarca.openvitals.ui.components.OpenVitalsCardStyle.Accent,
        accentColor = ScreenshotImportAccent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = LayoutMetrics.minTouchTarget)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Outlined.AddPhotoAlternate,
                contentDescription = null,
                tint = ScreenshotImportAccent,
                modifier = Modifier.size(Spacing.lg + Spacing.sm),
            )
            Text(
                text = stringResource(R.string.screenshot_import_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = Spacing.md),
            )
        }
    }
    var savedCount by remember { mutableIntStateOf(0) }
    var savedBanner by rememberSaveable { mutableStateOf(false) }
    pickedUri?.let { uri ->
        ScreenshotImportDialog(
            onDismiss = { pickedUri = null },
            initialUri = uri,
            onSaved = { savedCount++ },
            viewModel = hiltViewModel(key = "screenshot-import-$pickCount"),
        )
    }
    // The confirmation is a card of the app's own, under the tile, gone after a few seconds.
    // Each save shows it afresh for its full time; the tile above stays usable throughout.
    LaunchedEffect(savedCount) {
        if (savedCount > 0) {
            savedBanner = true
            kotlinx.coroutines.delay(ScreenshotSavedBannerMillis)
            savedBanner = false
        }
    }
    androidx.compose.animation.AnimatedVisibility(visible = savedBanner) {
        tech.mmarca.openvitals.ui.components.OpenVitalsCard(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            style = tech.mmarca.openvitals.ui.components.OpenVitalsCardStyle.Accent,
            accentColor = ScreenshotImportAccent,
            containerColor = tech.mmarca.openvitals.features.manualentry.manualEntryTileContainer(ScreenshotImportAccent),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = ScreenshotImportAccent,
                )
                Text(
                    text = stringResource(R.string.screenshot_import_saved),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
    }
}

private const val ScreenshotSavedBannerMillis = 4_000L

private val ScreenshotImportAccent = androidx.compose.ui.graphics.Color(0xFF4285F4)

@Composable
internal fun ScreenshotImportDialog(
    onDismiss: () -> Unit,
    initialUri: Uri? = null,
    /** Called once the values are in Health Connect, before the review closes. */
    onSaved: (() -> Unit)? = null,
    viewModel: ScreenshotImportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var start by rememberSaveable { mutableStateOf("") }
    var end by rememberSaveable { mutableStateOf("") }
    var confirmed by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var requested by remember { mutableStateOf<Set<String>>(emptySet()) }
    // Android stops showing the Health Connect dialog after repeated denials and answers
    // at once with nothing. Then OpenVitals' own page in Health Connect is the way to grant.
    val requester = rememberManualEntryWritePermissionRequester { granted ->
        if (!granted.containsAll(requested)) {
            tech.mmarca.openvitals.healthconnect.openHealthConnectPermissionSettings(context)
        }
        scope.launch { viewModel.refreshWritePermissions() }
    }
    LaunchedEffect(state.values.map { it.kind }) { viewModel.refreshWritePermissions() }
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        scope.launch { viewModel.refreshWritePermissions() }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { confirmed = false; viewModel.scan(uri) }
    }
    LaunchedEffect(initialUri) { initialUri?.let(viewModel::startWith) }
    LaunchedEffect(state.suggestedStart, state.suggestedEnd) {
        start = state.suggestedStart; end = state.suggestedEnd; confirmed = false
    }
    // Closing never asks anything. A save says so and closes the review.
    fun requestClose() {
        if (!state.busy) onDismiss()
    }
    val savedMessage = stringResource(R.string.screenshot_import_saved)
    // The context above also serves the toast.
    LaunchedEffect(state.saved) {
        // Read the live value: the composed one can still be the last import's "saved".
        if (state.saved && viewModel.state.value.saved) {
            viewModel.onSavedHandled()
            if (onSaved != null) onSaved() else {
                android.widget.Toast.makeText(context, savedMessage, android.widget.Toast.LENGTH_LONG).show()
            }
            onDismiss()
        }
    }
    val actions = ScreenshotImportActions(
        onPick = { picker.launch(arrayOf("image/*")) },
        onEdit = { index, value -> confirmed = false; viewModel.edit(index, value) },
        onRemove = { confirmed = false; viewModel.remove(it) },
        onStart = { start = it; if (!screenshotNeedsInterval(state.values)) end = it; confirmed = false },
        onEnd = { end = it; confirmed = false },
        onConfirmed = { confirmed = it },
        onType = { viewModel.selectExerciseType(it); confirmed = false },
        onPermissions = {
            scope.launch {
                try {
                    val missing = viewModel.missingWritePermissions()
                    if (missing.isNotEmpty()) {
                        requested = missing
                        requester.launch(missing)
                    } else {
                        viewModel.refreshWritePermissions()
                    }
                } catch (error: Exception) {
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    if (!tech.mmarca.openvitals.healthconnect.openHealthConnectPermissionSettings(context)) {
                        viewModel.permissionScreenFailed()
                    }
                }
            }
        },
        onSave = { viewModel.save(start, end) }, onClose = { requestClose() },
    )
    // A full screen with the back arrow on top, like every other menu.
    Dialog(
        onDismissRequest = { requestClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) { ScreenshotImportContent(state, start, end, confirmed, actions) }
    }
}
