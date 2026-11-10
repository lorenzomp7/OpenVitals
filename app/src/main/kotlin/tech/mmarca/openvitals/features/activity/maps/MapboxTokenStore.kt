package tech.mmarca.openvitals.features.activity.maps

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.MutableStateFlow

/** Only the public map token persists. OAuth credentials never enter this store or backups. */
internal object MapboxTokenStore {
    private const val ALIAS = "openvitals.mapbox.public-token.v1"
    val changes = MutableStateFlow(0L)

    @Synchronized
    fun read(context: Context): String = try {
        val bytes = file(context).readFully()
        require(bytes.size > 28)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
            .takeIf { MapboxOAuthProtocol.isPublicToken(it) }.orEmpty()
    } catch (_: Exception) {
        // Missing, invalidated or corrupt credentials mean disconnected; never log their contents.
        ""
    }

    @Synchronized
    fun save(context: Context, token: String) {
        require(MapboxOAuthProtocol.isPublicToken(token)) { "Invalid public map credential" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.iv + cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        val target = file(context)
        val stream = target.startWrite()
        try {
            stream.write(encrypted)
            target.finishWrite(stream)
        } catch (error: Exception) {
            target.failWrite(stream)
            throw error
        }
        changes.value += 1
    }

    @Synchronized
    fun clear(context: Context) {
        file(context).delete()
        changes.value += 1
    }

    private fun file(context: Context) = AtomicFile(File(context.noBackupFilesDir, "mapbox-credential.enc"))

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
}
