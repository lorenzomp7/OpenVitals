package tech.mmarca.openvitals.features.activity.maps

import android.content.Intent
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.OpenVitalsCard

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
    OpenVitalsCard(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.mapbox_name))
            Text(stringResource(R.string.mapbox_login_body))
            if (connected) Text(stringResource(R.string.openwatch_status_connected))
            if (failed) Text(stringResource(R.string.screen_error_generic))
            if (ready && !connected) Button(onClick = {
                context.startActivity(Intent(context, MapboxLoginActivity::class.java))
            }) { Text(stringResource(R.string.mapbox_sign_in)) }
            if (connected) TextButton(modifier = Modifier.widthIn(min = 1.dp), contentPadding = PaddingValues(0.dp), onClick = {
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { MapboxTokenStore.clear(context) }
                        failed = false
                    } catch (_: Exception) { failed = true }
                }
            }) { Text(stringResource(R.string.action_remove)) }
        }
    }
}
