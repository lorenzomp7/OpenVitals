package tech.mmarca.openvitals.core.presentation

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import io.mockk.anyConstructed
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HealthConnectSourceDeclarationsTest {

    private val packageManager = mockk<PackageManager>()
    private val context = mockk<Context>().also {
        every { it.packageManager } returns packageManager
        every { it.packageName } returns "tech.mmarca.openvitals.debug"
    }

    @Test
    fun `requested health permissions keep only the health ones`() {
        every { packageManager.getPackageInfo("com.huami.watch.hmwatchmanager", PackageManager.GET_PERMISSIONS) } returns
            PackageInfo().apply {
                requestedPermissions = arrayOf(
                    "android.permission.health.WRITE_HEART_RATE",
                    "android.permission.health.READ_STEPS",
                    "android.permission.BLUETOOTH_CONNECT",
                )
            }

        assertEquals(
            setOf("android.permission.health.WRITE_HEART_RATE", "android.permission.health.READ_STEPS"),
            HealthConnectSourceDeclarations(context).requestedHealthPermissions("com.huami.watch.hmwatchmanager"),
        )
    }

    @Test
    fun `an app that requests no permission declares none`() {
        every { packageManager.getPackageInfo("com.example.none", PackageManager.GET_PERMISSIONS) } returns
            PackageInfo().apply { requestedPermissions = null }

        assertEquals(emptySet<String>(), HealthConnectSourceDeclarations(context).requestedHealthPermissions("com.example.none"))
    }

    @Test
    fun `a package that is not visible reads as unknown`() {
        every { packageManager.getPackageInfo("com.other.phone.app", PackageManager.GET_PERMISSIONS) } throws
            PackageManager.NameNotFoundException()

        assertNull(HealthConnectSourceDeclarations(context).requestedHealthPermissions("com.other.phone.app"))
    }

    @Test
    fun `snapshot lists the Health Connect apps found by intent, without this app, plus the seen sources`() = runTest {
        mockkConstructor(Intent::class)
        every { anyConstructed<Intent>().addCategory(any()) } answers { self as Intent }
        every { packageManager.queryIntentActivities(any<Intent>(), 0) } returns listOf(
            resolveInfo("com.huami.watch.hmwatchmanager"),
            resolveInfo("tech.mmarca.openvitals.debug"),
        )
        every { packageManager.getPackageInfo("com.huami.watch.hmwatchmanager", PackageManager.GET_PERMISSIONS) } returns
            PackageInfo().apply { requestedPermissions = arrayOf("android.permission.health.WRITE_SLEEP") }
        every { packageManager.getPackageInfo("com.other.phone.app", PackageManager.GET_PERMISSIONS) } throws
            PackageManager.NameNotFoundException()

        val snapshot = HealthConnectSourceDeclarations(context).snapshot(listOf("com.other.phone.app"))

        assertEquals(
            mapOf(
                "com.huami.watch.hmwatchmanager" to setOf("android.permission.health.WRITE_SLEEP"),
                "com.other.phone.app" to null,
            ),
            snapshot,
        )
        unmockkConstructor(Intent::class)
    }

    private fun resolveInfo(packageName: String) = ResolveInfo().apply {
        activityInfo = ActivityInfo().apply { this.packageName = packageName }
    }
}
