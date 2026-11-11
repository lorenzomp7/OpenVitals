package tech.mmarca.openvitals.features.imports.screenshot

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.core.presentation.isPermissionFailure
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository
import tech.mmarca.openvitals.healthconnect.screenshotRecords

internal data class ScreenshotImportState(
    val values: List<ScreenshotValue> = emptyList(),
    val exerciseType: Int? = null,
    val suggestedStart: String = "",
    val suggestedEnd: String = "",
    val busy: Boolean = false,
    val saved: Boolean = false,
    val error: ScreenError? = null,
)

@HiltViewModel
internal class ScreenshotImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AppleHealthImportRepository,
) : ViewModel() {
    private val mutable = MutableStateFlow(ScreenshotImportState())
    val state = mutable.asStateFlow()
    private var sharedUri: Uri? = null
    fun scanShared(uri: Uri) {
        if (sharedUri == uri) return
        sharedUri = uri
        scan(uri)
    }
    fun scan(uri: Uri) {
        if (mutable.value.busy) return
        mutable.value = ScreenshotImportState(busy = true)
        viewModelScope.launch {
            try {
                val (recognized, timestamp) = withContext(Dispatchers.IO) {
                    val name = runCatching {
                        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) cursor.getString(0) else null
                        }
                    }.getOrNull().orEmpty()
                    val image = InputImage.fromFilePath(context, uri)
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    try {
                        val recognized = suspendCancellableCoroutine<com.google.mlkit.vision.text.Text> { continuation ->
                            recognizer.process(image)
                                .addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
                                .addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
                        }
                        recognized to ScreenshotParser.filenameTime(name)
                    } finally { recognizer.close() }
                }
                val text = recognized.text
                val lines = recognized.textBlocks.flatMap { it.lines }.mapNotNull { line ->
                    line.boundingBox?.let { box -> ScreenshotOcrLine(line.text, box.left, box.top, box.right, box.bottom) }
                }
                val values = ScreenshotParser.parseLines(lines)
                val seconds = values.firstOrNull { it.kind == "duration_min" }?.value?.toDoubleOrNull()?.times(60)?.let { kotlin.math.round(it).toLong() } ?: 0L
                mutable.value = ScreenshotImportState(values = values,
                    exerciseType = ScreenshotParser.workoutType(text, screenshotWorkoutLabels(context)),
                    suggestedStart = timestamp?.minusSeconds(seconds)?.toString().orEmpty(),
                    suggestedEnd = timestamp?.toString().orEmpty())
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                mutable.value = ScreenshotImportState(error = (if (error.isPermissionFailure()) SecurityException("Health Connect permission required") else IllegalStateException("Check the image, values, date format and Health Connect availability")).toScreenError())
            }
        }
    }
    fun edit(index: Int, value: String) {
        mutable.value = mutable.value.copy(values = mutable.value.values.mapIndexed { i, item ->
            if (i == index) item.copy(value = value) else item
        }, saved = false)
    }
    fun remove(index: Int) { mutable.value = mutable.value.copy(values = mutable.value.values.filterIndexed { i, _ -> i != index }) }
    fun selectExerciseType(type: Int) {
        mutable.value = mutable.value.copy(exerciseType = type, saved = false)
    }
    fun save(start: String, end: String) {
        if (mutable.value.busy || mutable.value.values.none { it.kind != "bmi" } || mutable.value.saved) return
        val values = mutable.value.values
        mutable.value = mutable.value.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                repository.insertImportedRecords(screenshotRecords(values.map { it.kind to it.value }, start, end,
                    if (values.any { it.kind == "duration_min" }) requireNotNull(mutable.value.exerciseType) else null))
                mutable.value = mutable.value.copy(busy = false, saved = true)
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                mutable.value = mutable.value.copy(busy = false, error = (if (error.isPermissionFailure()) SecurityException("Health Connect permission required") else IllegalStateException("Check the image, values, date format and Health Connect availability")).toScreenError())
            }
        }
    }
}
