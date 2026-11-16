package tech.mmarca.openvitals.domain.preferences

/** 97: where the home screen's brand pieces take their colours from. */
enum class BrandPaletteSource {
    /** The four Google colours of the icon. */
    GOOGLE,

    /** The phone's own colours (Material You), as the theme has them. */
    PHONE,

    /** The colours of the three tiles that make the rings at the top. */
    HERO,

    /** The colours of the first six tiles of the first page. */
    TILES,

    /** The rings' colours, then the first six tiles'. */
    HERO_AND_TILES,
    ;

    companion object {
        fun fromStorage(value: String?): BrandPaletteSource = entries.firstOrNull { it.name == value } ?: GOOGLE
    }
}
