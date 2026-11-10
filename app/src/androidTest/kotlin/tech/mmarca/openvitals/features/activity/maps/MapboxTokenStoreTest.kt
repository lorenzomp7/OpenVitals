package tech.mmarca.openvitals.features.activity.maps

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class MapboxTokenStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    @After fun cleanup() { MapboxTokenStore.clear(context) }

    @Test fun publicTokenRoundTripsEncryptedOutsideBackupsAndCanBeRemoved() {
        MapboxTokenStore.clear(context)
        assertEquals("", MapboxTokenStore.read(context))
        val token = "pk.fixture.signature"
        MapboxTokenStore.save(context, token)
        assertEquals(token, MapboxTokenStore.read(context))
        val file = File(context.noBackupFilesDir, "mapbox-credential.enc")
        assertTrue(file.isFile)
        assertFalse(file.readBytes().toString(Charsets.ISO_8859_1).contains(token))
        MapboxTokenStore.clear(context)
        assertFalse(file.exists())
        assertEquals("", MapboxTokenStore.read(context))
    }

    @Test fun corruptCredentialsFallBackToDisconnectedAndCanBeReplaced() {
        MapboxTokenStore.save(context, "pk.fixture.signature")
        File(context.noBackupFilesDir, "mapbox-credential.enc").writeBytes(ByteArray(40))
        assertEquals("", MapboxTokenStore.read(context))
        MapboxTokenStore.save(context, "pk.replacement.signature")
        assertEquals("pk.replacement.signature", MapboxTokenStore.read(context))
    }

    @Test fun secretTokensAreNeverPersisted() {
        MapboxTokenStore.clear(context)
        assertThrows(IllegalArgumentException::class.java) { MapboxTokenStore.save(context, "sk.fixture.signature") }
        assertEquals("", MapboxTokenStore.read(context))
    }
}
