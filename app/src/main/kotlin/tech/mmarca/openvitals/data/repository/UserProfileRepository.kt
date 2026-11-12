package tech.mmarca.openvitals.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.AtomicFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class UserProfile(val name: String, val age: Int?)

@Singleton
class UserProfileRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val prefs = context.getSharedPreferences("openvitals_user_profile", Context.MODE_PRIVATE)
    private val photoFile get() = File(context.filesDir, "user-profile.jpg")

    fun read() = UserProfile(prefs.getString("name", "").orEmpty(), prefs.getInt("age", -1).takeIf { it >= 0 })

    suspend fun save(name: String, age: Int?) = withContext(Dispatchers.IO) {
        require(name.length <= 100 && (age == null || age in 0..130))
        check(prefs.edit().putString("name", name.trim()).putInt("age", age ?: -1).commit())
    }

    suspend fun photo(): Bitmap? = withContext(Dispatchers.IO) {
        photoFile.takeIf { it.isFile }?.let { BitmapFactory.decodeFile(it.path) }
    }

    suspend fun setPhoto(uri: Uri?) = withContext(Dispatchers.IO) {
        if (uri == null) {
            check(!photoFile.exists() || photoFile.delete())
            return@withContext
        }
        val bitmap = previewPhoto(uri) ?: error("Image unavailable")
        val atomic = AtomicFile(photoFile)
        val output = atomic.startWrite()
        try {
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output))
            atomic.finishWrite(output)
        } catch (error: Exception) {
            atomic.failWrite(output)
            throw error
        } finally {
            bitmap.recycle()
        }
    }
    suspend fun previewPhoto(uri: Uri?): Bitmap? = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0)
        val options = BitmapFactory.Options().apply {
            inSampleSize = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / inSampleSize > 1024) inSampleSize *= 2
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Image unavailable")
        val scale = minOf(1f, 512f / maxOf(decoded.width, decoded.height))
        val bitmap = Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1), true)
        if (bitmap !== decoded) decoded.recycle()
        bitmap
    }

}
