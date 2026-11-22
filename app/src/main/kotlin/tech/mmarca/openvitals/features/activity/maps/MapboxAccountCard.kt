package tech.mmarca.openvitals.features.activity.maps

import tech.mmarca.openvitals.ui.theme.Spacing
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsCard

/** "Online maps - Mapbox" in the activities settings, with its status and a tap to disconnect. */
@Composable
internal fun MapboxAccountCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val revision by MapboxTokenStore.changes.collectAsState()
    var connected by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(revision) {
        connected = withContext(Dispatchers.IO) { MapboxTokenStore.read(context).isNotBlank() }
        ready = true
    }
    fun disconnect() {
        scope.launch {
            try {
                withContext(Dispatchers.IO) { MapboxTokenStore.clear(context) }
                failed = false
            } catch (_: Exception) {
                failed = true
            }
        }
    }
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Outlined.Public,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(Spacing.xl),
                )
                Column(
                    modifier = Modifier
                        .padding(start = Spacing.md)
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(
                        text = stringResource(R.string.settings_online_maps_mapbox_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (ready) {
                        Text(
                            text = stringResource(
                                R.string.settings_online_maps_status,
                                stringResource(
                                    if (connected) {
                                        R.string.openwatch_status_connected
                                    } else {
                                        R.string.settings_online_maps_disconnected
                                    },
                                ),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (connected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    if (ready && connected) {
                        Text(
                            text = stringResource(R.string.settings_online_maps_touch_to_disconnect),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable(onClick = ::disconnect)
                                .padding(vertical = Spacing.xs),
                        )
                    }
                    if (ready && !connected) {
                        Text(
                            text = stringResource(R.string.mapbox_login_body_short),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (failed) {
                        Text(
                            text = stringResource(R.string.screen_error_generic),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            if (ready && !connected) {
                OpenVitalsButton(
                    onClick = { context.startActivity(Intent(context, MapboxLoginActivity::class.java)) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.mapbox_sign_in)) }
            }
        }
    }
}
