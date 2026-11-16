package tech.mmarca.openvitals.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.mmarca.openvitals.domain.preferences.BrandPaletteSource

/** 97: the home screen's colours and whether its brand pieces move. A file of its own. */
@Singleton
class BrandLookPreferences @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("openvitals_brand_look", Context.MODE_PRIVATE)

    private val _palette = MutableStateFlow(BrandPaletteSource.fromStorage(prefs.getString(KEY_PALETTE, null)))
    val palette: StateFlow<BrandPaletteSource> = _palette.asStateFlow()

    private val _animated = MutableStateFlow(prefs.getBoolean(KEY_ANIMATED, true))
    val animated: StateFlow<Boolean> = _animated.asStateFlow()

    fun setPalette(source: BrandPaletteSource) {
        prefs.edit().putString(KEY_PALETTE, source.name).apply()
        _palette.value = source
    }

    fun setAnimated(animated: Boolean) {
        prefs.edit().putBoolean(KEY_ANIMATED, animated).apply()
        _animated.value = animated
    }

    private companion object {
        const val KEY_PALETTE = "palette"
        const val KEY_ANIMATED = "animated"
    }
}
