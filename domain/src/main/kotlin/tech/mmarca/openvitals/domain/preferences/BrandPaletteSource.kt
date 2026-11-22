package tech.mmarca.openvitals.domain.preferences

/** 97: where the home screen's brand pieces take their colours from. */
enum class BrandPaletteSource {
    /** The four Google colours of the icon. */
    GOOGLE,

    /** The phone's own colours (Material You), as the theme has them. */
    PHONE,

    /** The colours of the three tiles that make the rings at the top. */
    HERO,

    /**
     * 150: the colours of the home screen's tiles: the first page when the tiles come in pages,
     * else those inside the first four-by-four block of spaces.
     */
    TILES,

    /** The rings' colours, then the home screen's. */
    HERO_AND_TILES,
    ;

    companion object {
        fun fromStorage(value: String?): BrandPaletteSource = entries.firstOrNull { it.name == value } ?: GOOGLE
    }
}
