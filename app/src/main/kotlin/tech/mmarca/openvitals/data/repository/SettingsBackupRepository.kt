package tech.mmarca.openvitals.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class SettingsBackupPreview(val stores: Map<String, Map<String, Any>>, val photo: ByteArray? = null, val includesPhoto: Boolean = false) {
    val count: Int get() = stores.values.sumOf { it.size }
}

/** Uses Android document streams; never copies health records, active recordings or credentials. */
@Singleton
internal class SettingsBackupRepository @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun export(uri: Uri) = withContext(Dispatchers.IO) {
        val stores = SettingsBackupSchema.stores.keys.associateWith { preferences(it).all }
        val photo = java.io.File(context.filesDir, "user-profile.jpg").takeIf { it.isFile }?.readBytes()
        val root = org.json.JSONObject(SettingsBackupCodec.encode(stores))
        root.put("profilePhoto", photo?.let { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) } ?: org.json.JSONObject.NULL)
        val text = root.toString(2)
        SettingsBackupCodec.decode(text)
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            ?: error("Unavailable destination")
    }

    suspend fun preview(uri: Uri): SettingsBackupPreview = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= SettingsBackupCodec.MAX_BYTES)
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: error("Unavailable source")
        val text = String(bytes, Charsets.UTF_8)
        val root = org.json.JSONObject(text)
        val photo = if (root.has("profilePhoto") && !root.isNull("profilePhoto")) {
            android.util.Base64.decode(root.getString("profilePhoto"), android.util.Base64.DEFAULT).also {
                require(it.size <= 512_000)
                val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size, bounds)
                require(bounds.outWidth in 1..1024 && bounds.outHeight in 1..1024)
            }
        } else null
        SettingsBackupPreview(SettingsBackupCodec.decode(text), photo, root.has("profilePhoto"))
    }

    suspend fun restore(preview: SettingsBackupPreview) = withContext(Dispatchers.IO) {
        // Hold the originals for rollback if a later file cannot be committed.
        val originals = preview.stores.keys.associateWith { preferences(it).all }
        val photoFile = if (preview.includesPhoto) java.io.File(context.filesDir, "user-profile.jpg") else null
        val originalPhoto = photoFile?.takeIf { it.isFile }?.readBytes()
        try {
            preview.stores.forEach { (store, values) -> write(store, values) }
            photoFile?.let { writePhoto(it, preview.photo) }
        } catch (failure: Exception) {
            originals.forEach { (store, values) -> runCatching { write(store, values) } }
            photoFile?.let { runCatching { writePhoto(it, originalPhoto) } }
            throw failure
        }
    }

    private fun writePhoto(file: java.io.File, bytes: ByteArray?) {
        if (bytes == null) { check(!file.exists() || file.delete()); return }
        val atomic = android.util.AtomicFile(file)
        val stream = atomic.startWrite()
        try {
            stream.write(bytes)
            atomic.finishWrite(stream)
        } catch (failure: Exception) {
            atomic.failWrite(stream)
            throw failure
        }
    }

    private fun preferences(store: String) = context.getSharedPreferences(store, Context.MODE_PRIVATE)

    private fun write(store: String, values: Map<String, *>) {
        val prefs = preferences(store)
        val editor = prefs.edit()
        // Replace settings in each supplied group, while keeping device-local status untouched.
        prefs.all.keys.filter { SettingsBackupSchema.type(store, it) != null }.forEach(editor::remove)
        values.forEach { (key, value) ->
            if (SettingsBackupSchema.type(store, key) != null) editor.putValue(key, value)
        }
        check(editor.commit())
    }

    private fun SharedPreferences.Editor.putValue(key: String, value: Any?) {
        when (value) {
            is Boolean -> putBoolean(key, value)
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is Float -> putFloat(key, value)
            is String -> putString(key, value)
            is Set<*> -> putStringSet(key, value.filterIsInstance<String>().toSet())
            null -> remove(key)
            else -> error("Unsupported preference type")
        }
    }
}
