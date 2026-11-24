package tech.mmarca.openvitals.core.presentation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * What the Health Connect apps on this phone declare in their manifests: which apps integrate
 * with Health Connect and which health permissions each one requests.
 *
 * A request is a declaration, not a grant: it tells "this app never writes sleep" apart from
 * "this app writes sleep, but none arrived". Visibility comes from the `<queries>` intents in
 * the manifest (the permissions-rationale activity, and the Android 14 permission-usage alias
 * with the HEALTH_PERMISSIONS category); a package outside them reads as not installed.
 */
@Singleton
class HealthConnectSourceDeclarations @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * The packages installed here that declare a Health Connect integration, with the health
     * permissions they request, plus [alsoInclude] (sources seen in the data). A package that
     * is not visible here maps to null.
     */
    suspend fun snapshot(alsoInclude: Collection<String> = emptyList()): Map<String, Set<String>?> =
        withContext(Dispatchers.IO) {
            val packages = healthConnectApps() + alsoInclude
            packages.associateWith { requestedHealthPermissions(it) }
        }

    /** Packages declaring a Health Connect permissions screen, this app excluded. */
    fun healthConnectApps(): Set<String> {
        val packageManager = context.packageManager
        return HEALTH_CONNECT_INTENTS
            .flatMap { intent -> packageManager.queryActivities(intent) }
            .mapNotNull { it.activityInfo?.packageName }
            .filterNot { it == context.packageName }
            .toSet()
    }

    /** The `android.permission.health.*` permissions [packageName] requests; null if not visible. */
    fun requestedHealthPermissions(packageName: String): Set<String>? =
        try {
            context.packageManager.packageInfoWithPermissions(packageName)
                .requestedPermissions
                .orEmpty()
                .filter { it.startsWith(HEALTH_PERMISSION_PREFIX) }
                .toSet()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

    private fun PackageManager.queryActivities(intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            queryIntentActivities(intent, 0)
        }

    private fun PackageManager.packageInfoWithPermissions(packageName: String): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        }

    companion object {
        const val HEALTH_PERMISSION_PREFIX = "android.permission.health."

        private val HEALTH_CONNECT_INTENTS: List<Intent>
            get() = listOf(
                Intent("androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"),
                Intent("android.intent.action.VIEW_PERMISSION_USAGE")
                    .addCategory("android.intent.category.HEALTH_PERMISSIONS"),
            )
    }
}
