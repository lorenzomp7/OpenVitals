package tech.mmarca.openvitals.features.imports.screenshot

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal suspend fun recognizeScreenshot(image: InputImage): Text {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    return try {
        suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
                .addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
        }
    } finally { recognizer.close() }
}

/** Keep words as well as lines: ML Kit can put two neighbouring measurements on one line. */
internal fun screenshotOcrValues(text: Text): List<ScreenshotValue> {
    val lines = text.textBlocks.flatMap { it.lines }.flatMap { line ->
        listOfNotNull(line.boundingBox?.let { ScreenshotOcrLine(line.text, it.left, it.top, it.right, it.bottom) }) +
            line.elements.mapNotNull { word -> word.boundingBox?.let { ScreenshotOcrLine(word.text, it.left, it.top, it.right, it.bottom) } }
    }.distinct()
    return ScreenshotParser.parseLines(lines)
}
