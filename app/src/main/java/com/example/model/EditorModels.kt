package com.example.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.UUID

enum class EditorTab(val label: String) {
    ADJUST("Adjust"),
    FILTERS("Filters"),
    AI_TOOLS("AI Magic"),
    TRANSFORM("Crop & Rotate"),
    BLUR_FOCUS("Blur & Focus"),
    SPLASH("Color Splash"),
    DOODLE("Draw"),
    TEXT("Text"),
    EXPORT("Export")
}

data class Adjustments(
    val brightness: Float = 0f,    // -100 to 100
    val contrast: Float = 0f,      // -100 to 100
    val saturation: Float = 0f,    // -100 to 100
    val exposure: Float = 0f,      // -100 to 100
    val warmth: Float = 0f,        // -100 to 100 (temperature)
    val tint: Float = 0f,          // -100 to 100 (green/magenta)
    val vibrance: Float = 0f,      // -100 to 100
    val highlights: Float = 0f,    // -100 to 100
    val shadows: Float = 0f,       // -100 to 100
    val sharpness: Float = 0f,     // 0 to 100
    val vignette: Float = 0f,      // 0 to 100
    val hue: Float = 0f,           // -180 to 180
    val grain: Float = 0f          // 0 to 100
) {
    val isDefault: Boolean
        get() = brightness == 0f && contrast == 0f && saturation == 0f &&
                exposure == 0f && warmth == 0f && tint == 0f &&
                vibrance == 0f && highlights == 0f && shadows == 0f &&
                sharpness == 0f && vignette == 0f && hue == 0f && grain == 0f
}

enum class ImageFilter(val displayName: String, val category: String) {
    ORIGINAL("Original", "Basic"),
    KELLY_VIVID("Vivid Pop", "Modern"),
    HIDREAM_AURA("Aura Glow", "Artistic"),
    NOIR_CLASSIC("Noir Film", "B&W"),
    CYBERPUNK("Cyberpunk", "Cinematic"),
    VINTAGE_1977("Vintage '77", "Retro"),
    GOLDEN_HOUR("Golden Hour", "Lighting"),
    EMERALD_FOREST("Emerald", "Nature"),
    DRAMATIC_MATTE("Matte Cinema", "Cinematic"),
    SUNSET_GLOW("Sunset Glow", "Lighting"),
    PASTEL_DREAM("Pastel Dream", "Artistic"),
    MONOCHROME_GRAIN("Silver B&W", "B&W"),
    SEPIA_HERITAGE("Sepia", "Retro"),
    INVERT_NEGATIVE("Invert", "Special"),
    POSTERIZE("Posterize", "Special"),
    SKETCH_EDGE("Pencil Sketch", "Artistic"),
    EMBOSS_3D("Emboss 3D", "Special"),
    PIXELATE_8BIT("Pixelate 8-Bit", "Retro")
}

enum class CropRatio(val displayName: String, val ratioX: Float, val ratioY: Float) {
    ORIGINAL("Original", 0f, 0f),
    FREE("Freeform", 0f, 0f),
    SQUARE_1_1("1:1 Square", 1f, 1f),
    PORTRAIT_4_5("4:5 Insta", 4f, 5f),
    PORTRAIT_3_4("3:4 Portrait", 3f, 4f),
    PORTRAIT_9_16("9:16 Story", 9f, 16f),
    LANDSCAPE_4_3("4:3 Standard", 4f, 3f),
    LANDSCAPE_16_9("16:9 Cinema", 16f, 9f),
    LANDSCAPE_3_2("3:2 Photo", 3f, 2f)
}

data class TransformState(
    val rotationDegrees: Int = 0,     // 0, 90, 180, 270
    val straightenAngle: Float = 0f,  // -45f to +45f
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val selectedCropRatio: CropRatio = CropRatio.ORIGINAL,
    val cropRectNormalized: NormalizedRect = NormalizedRect(0f, 0f, 1f, 1f)
)

data class NormalizedRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
)

enum class BlurMode(val displayName: String) {
    NONE("None"),
    RADIAL_BOKEH("Radial Bokeh"),
    LINEAR_TILT_SHIFT("Tilt-Shift"),
    FULL_GAUSSIAN("Full Soft Focus")
}

data class BlurSettings(
    val mode: BlurMode = BlurMode.NONE,
    val intensity: Float = 40f, // 0 to 100
    val centerX: Float = 0.5f,  // 0 to 1
    val centerY: Float = 0.5f,  // 0 to 1
    val radius: Float = 0.35f   // 0.1 to 0.8
)

enum class SplashChannel(val displayName: String, val targetHue: Float, val hueRange: Float) {
    OFF("Normal (Off)", 0f, 0f),
    RED("Red Only", 0f, 35f),
    GREEN("Green Only", 120f, 45f),
    BLUE("Blue Only", 220f, 40f),
    YELLOW("Yellow Only", 55f, 30f),
    CYAN("Cyan Only", 180f, 35f),
    MAGENTA("Magenta Only", 300f, 40f)
}

enum class AutoEnhanceType(val displayName: String, val description: String) {
    NONE("Normal", "No auto enhancement"),
    AUTO_DYNAMIC_RANGE("Auto Dynamic Range", "Balances shadows & highlights via adaptive histogram stretching"),
    AUTO_WHITE_BALANCE("Auto White Balance", "Corrects color temperature and removes unwanted color casts"),
    PORTRAIT_SKIN_GLOW("Portrait Soft Glow", "Smooths skin textures while preserving crisp edge details"),
    DEHAZE_CLARITY("De-haze & Clarity", "Cuts through atmospheric haze and boosts midtone micro-contrast")
}

enum class BrushMode(val displayName: String) {
    NORMAL("Normal Brush"),
    NEON_GLOW("Neon Glow"),
    HIGHLIGHTER("Highlighter"),
    ERASER("Eraser")
}

data class DoodlePoint(val x: Float, val y: Float)

data class DoodlePath(
    val id: String = UUID.randomUUID().toString(),
    val points: List<DoodlePoint>,
    val colorArgb: Int = Color.Cyan.toArgb(),
    val strokeWidth: Float = 12f,
    val brushMode: BrushMode = BrushMode.NORMAL,
    val opacity: Float = 1.0f
)

enum class TextStyleFont(val displayName: String) {
    MODERN_SANS("Modern Sans"),
    ELEGANT_SERIF("Elegant Serif"),
    BOLD_IMPACT("Bold Display"),
    MONOSPACE("Code Mono"),
    CURSIVE("Script Calligraphy")
}

data class TextOverlay(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "Kelly Editor",
    val fontStyle: TextStyleFont = TextStyleFont.MODERN_SANS,
    val textColorArgb: Int = Color.White.toArgb(),
    val backgroundColorArgb: Int = Color.Transparent.toArgb(),
    val textSizeSp: Float = 28f,
    val normalizedX: Float = 0.5f,
    val normalizedY: Float = 0.5f,
    val rotation: Float = 0f,
    val hasShadow: Boolean = true
)

data class EditState(
    val adjustments: Adjustments = Adjustments(),
    val activeFilter: ImageFilter = ImageFilter.ORIGINAL,
    val filterIntensity: Float = 100f, // 0 to 100
    val autoEnhance: AutoEnhanceType = AutoEnhanceType.NONE,
    val transform: TransformState = TransformState(),
    val blur: BlurSettings = BlurSettings(),
    val splash: SplashChannel = SplashChannel.OFF,
    val doodlePaths: List<DoodlePath> = emptyList(),
    val textOverlays: List<TextOverlay> = emptyList()
)

data class ImageMetadata(
    val width: Int = 0,
    val height: Int = 0,
    val megapixel: Float = 0f,
    val aspectRatioStr: String = "1:1",
    val approximateSizeKb: Long = 0L,
    val colorFormat: String = "ARGB_8888 (32-bit)"
)

data class SamplePhoto(
    val title: String,
    val category: String,
    val drawableResId: Int,
    val description: String
)
