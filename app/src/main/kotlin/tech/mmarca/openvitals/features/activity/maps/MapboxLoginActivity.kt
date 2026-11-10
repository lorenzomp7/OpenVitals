package tech.mmarca.openvitals.features.activity.maps

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/** Password and MFA stay in the external browser. Only a public map token is persisted. */
class MapboxLoginActivity : ComponentActivity() {
    private var status by mutableStateOf(R.string.openwatch_status_connecting)
    private var busy by mutableStateOf(false)
    private var session: MapboxOAuthClient? = null
    private var loginJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            OpenVitalsTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.padding(24.dp)) {
                        Text(stringResource(R.string.mapbox_name), style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.mapbox_login_body))
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(status))
                        if (!busy) Button(onClick = ::login) { Text(stringResource(R.string.mapbox_sign_in)) }
                        TextButton(onClick = { finish() }) { Text(stringResource(R.string.action_cancel)) }
                    }
                }
            }
        }
        login()
    }

    private fun login() {
        if (busy) return
        busy = true
        status = R.string.openwatch_status_connecting
        loginJob = lifecycleScope.launch {
            var active: MapboxOAuthClient? = null
            try {
                val url = withContext(Dispatchers.IO) {
                    MapboxOAuthClient().also { active = it; session = it }.prepare()
                }
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE))
                val token = withContext(Dispatchers.IO) { active!!.complete() }
                withContext(Dispatchers.IO) { MapboxTokenStore.save(applicationContext, token) }
                finish()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Do not send credentials, callbacks or provider responses into logs/crash reports.
                status = R.string.screen_error_generic
            } finally {
                active?.close()
                session = null
                busy = false
            }
        }
    }

    override fun onDestroy() {
        session?.close()
        loginJob?.cancel()
        super.onDestroy()
    }
}
