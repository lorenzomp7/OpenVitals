package tech.mmarca.openvitals.features.imports.screenshot

import android.content.Context
import android.content.res.Configuration
import java.util.Locale
import tech.mmarca.openvitals.domain.model.ExerciseTypeNames
import tech.mmarca.openvitals.features.activity.exerciseTypeLabelRes

/** Use the same translated workout names as the rest of the app. */
internal fun screenshotWorkoutLabels(context: Context): Map<String, Int> = buildMap {
    val contexts = listOf(context) + listOf("it", "en").map { language ->
        context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(language))
        })
    }
    ExerciseTypeNames.byType.forEach { (type, name) ->
        put(name.replace('_', ' '), type)
        contexts.forEach { put(it.getString(exerciseTypeLabelRes(type)), type) }
    }
}
