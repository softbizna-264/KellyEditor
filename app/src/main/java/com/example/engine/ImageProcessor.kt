package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.LinearGradient
import android.graphics.Shader
import com.example.model.Adjustments
import com.example.model.AutoEnhanceType
import com.example.model.BlurMode
import com.example.model.BlurSettings
import com.example.model.CropRatio
import com.example.model.DoodlePath
import com.example.model.EditState
import com.example.model.ImageFilter
import com.example.model.NormalizedRect
import com.example.model.SplashChannel
import com.example.model.TextOverlay
import com.example.model.TextStyleFont
import com.example.model.TransformState
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

object ImageProcessor {

    /**
     * Main offline rendering pipeline.
     * Takes source bitmap and applies full EditState parameters to produce the output bitmap.
     */
    fun processImage(
        source: Bitmap,
        state: EditState,
        isThumbnailPreview: Boolean = false
    ): Bitmap {
        // 1. First apply Transforms (Crop, Rotate, Flip, Straighten)
        var transformed = applyTransform(source, state.transform)

        // 2. If thumbnail preview, downscale if needed for silky 60fps slider drag
        if (isThumbnailPreview && (transformed.width > 1200 || transformed.height > 1200)) {
            val scale = 1000f / max(transformed.width, transformed.height)
            val newW = (transformed.width * scale).toInt().coerceAtLeast(100)
            val newH = (transformed.height * scale).toInt().coerceAtLeast(100)
            transformed = Bitmap.createScaledBitmap(transformed, newW, newH, true)
        }

        // 3. Apply Algorithmic Auto Enhancements if enabled
        var enhanced = if (state.autoEnhance != AutoEnhanceType.NONE) {
            applyAutoEnhance(transformed, state.autoEnhance)
        } else {
            transformed
        }

        // 4. Apply Color Adjustments & Base Filter ColorMatrix
        val colorAdjusted = applyColorAdjustmentsAndFilter(
            enhanced,
            state.adjustments,
            state.activeFilter,
            state.filterIntensity
        )

        // 5. Apply Convolution / Pixel-level filters if selected (Sketch, Emboss, Pixelate)
        var pixelFiltered = applyPixelFilterIfNeeded(colorAdjusted, state.activeFilter, state.filterIntensity)

        // 6. Apply Sharpness if adjustments.sharpness > 0
        if (state.adjustments.sharpness > 0f) {
            pixelFiltered = applySharpness(pixelFiltered, state.adjustments.sharpness / 100f)
        }

        // 7. Apply Color Splash if active
        if (state.splash != SplashChannel.OFF) {
            pixelFiltered = applyColorSplash(pixelFiltered, state.splash)
        }

        // 8. Apply Blur & Tilt-Shift / Bokeh if active
        if (state.blur.mode != BlurMode.NONE && state.blur.intensity > 0f) {
            pixelFiltered = applyBlurAndFocus(pixelFiltered, state.blur)
        }

        // 9. Apply Vignette if adjustments.vignette > 0
        if (state.adjustments.vignette > 0f) {
            pixelFiltered = applyVignette(pixelFiltered, state.adjustments.vignette / 100f)
        }

        // 10. Apply Grain noise if adjustments.grain > 0
        if (state.adjustments.grain > 0f) {
            pixelFiltered = applyGrain(pixelFiltered, state.adjustments.grain / 100f)
        }

        // 11. Render Doodles & Text Overlays onto final canvas
        val finalResult = renderOverlays(pixelFiltered, state.doodlePaths, state.textOverlays)

        return finalResult
    }

    /**
     * Applies rotation, flip, straighten angle, and cropping.
     */
    fun applyTransform(source: Bitmap, transform: TransformState): Bitmap {
        val totalRotation = (transform.rotationDegrees + transform.straightenAngle)
        val matrix = Matrix()

        if (transform.flipHorizontal) {
            matrix.postScale(-1f, 1f, source.width / 2f, source.height / 2f)
        }
        if (transform.flipVertical) {
            matrix.postScale(1f, -1f, source.width / 2f, source.height / 2f)
        }
        if (totalRotation != 0f) {
            matrix.postRotate(totalRotation, source.width / 2f, source.height / 2f)
        }

        val rotated = Bitmap.createBitmap(
            source, 0, 0, source.width, source.height, matrix, true
        )

        // Apply Normalized Crop Rect
        val cropRect = transform.cropRectNormalized
        if (cropRect.left > 0f || cropRect.top > 0f || cropRect.right < 1f || cropRect.bottom < 1f) {
            val cropX = (cropRect.left * rotated.width).toInt().coerceIn(0, rotated.width - 1)
            val cropY = (cropRect.top * rotated.height).toInt().coerceIn(0, rotated.height - 1)
            val cropW = ((cropRect.right - cropRect.left) * rotated.width).toInt().coerceIn(1, rotated.width - cropX)
            val cropH = ((cropRect.bottom - cropRect.top) * rotated.height).toInt().coerceIn(1, rotated.height - cropY)

            return Bitmap.createBitmap(rotated, cropX, cropY, cropW, cropH)
        }

        return rotated
    }

    /**
     * Computes combined ColorMatrix for all adjustments & cinematic tone filters.
     */
    private fun applyColorAdjustmentsAndFilter(
        source: Bitmap,
        adj: Adjustments,
        filter: ImageFilter,
        filterIntensity: Float
    ): Bitmap {
        val combinedMatrix = ColorMatrix()

        // Filter Matrix
        val filterMatrix = getFilterMatrix(filter)
        if (filterIntensity < 100f) {
            val identity = ColorMatrix()
            val t = filterIntensity / 100f
            val interpolated = FloatArray(20)
            val filterArr = filterMatrix.array
            val idArr = identity.array
            for (i in 0 until 20) {
                interpolated[i] = idArr[i] * (1f - t) + filterArr[i] * t
            }
            filterMatrix.set(interpolated)
        }
        combinedMatrix.postConcat(filterMatrix)

        // Saturation (-100 to 100 -> 0 to 2)
        if (adj.saturation != 0f) {
            val satMatrix = ColorMatrix()
            val satFactor = (adj.saturation + 100f) / 100f
            satMatrix.setSaturation(satFactor.coerceIn(0f, 3f))
            combinedMatrix.postConcat(satMatrix)
        }

        // Contrast & Brightness (-100 to 100)
        if (adj.contrast != 0f || adj.brightness != 0f) {
            val contrastFactor = (adj.contrast + 100f) / 100f
            val brightnessOffset = adj.brightness * 2.55f
            val t = 128f * (1f - contrastFactor) + brightnessOffset
            val cbMatrix = ColorMatrix(
                floatArrayOf(
                    contrastFactor, 0f, 0f, 0f, t,
                    0f, contrastFactor, 0f, 0f, t,
                    0f, 0f, contrastFactor, 0f, t,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            combinedMatrix.postConcat(cbMatrix)
        }

        // Exposure (-100 to 100)
        if (adj.exposure != 0f) {
            val expFactor = Math.pow(2.0, (adj.exposure / 50.0)).toFloat()
            val expMatrix = ColorMatrix(
                floatArrayOf(
                    expFactor, 0f, 0f, 0f, 0f,
                    0f, expFactor, 0f, 0f, 0f,
                    0f, 0f, expFactor, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            combinedMatrix.postConcat(expMatrix)
        }

        // Warmth (Temperature) & Tint
        if (adj.warmth != 0f || adj.tint != 0f) {
            val warmVal = adj.warmth * 0.4f
            val tintVal = adj.tint * 0.4f
            val tempMatrix = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, warmVal + tintVal,
                    0f, 1f, 0f, 0f, -tintVal * 0.5f,
                    0f, 0f, 1f, 0f, -warmVal,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            combinedMatrix.postConcat(tempMatrix)
        }

        // Hue Rotation (-180 to 180)
        if (adj.hue != 0f) {
            val hueMatrix = ColorMatrix()
            val rad = Math.toRadians(adj.hue.toDouble())
            val cosVal = cos(rad).toFloat()
            val sinVal = sin(rad).toFloat()
            val lumR = 0.213f
            val lumG = 0.715f
            val lumB = 0.072f

            hueMatrix.set(
                floatArrayOf(
                    lumR + cosVal * (1 - lumR) + sinVal * (-lumR),
                    lumG + cosVal * (-lumG) + sinVal * (-lumG),
                    lumB + cosVal * (-lumB) + sinVal * (1 - lumB), 0f, 0f,

                    lumR + cosVal * (-lumR) + sinVal * (0.143f),
                    lumG + cosVal * (1 - lumG) + sinVal * (0.140f),
                    lumB + cosVal * (-lumB) + sinVal * (-0.283f), 0f, 0f,

                    lumR + cosVal * (-lumR) + sinVal * (-(1 - lumR)),
                    lumG + cosVal * (-lumG) + sinVal * (lumG),
                    lumB + cosVal * (1 - lumB) + sinVal * (lumB), 0f, 0f,

                    0f, 0f, 0f, 1f, 0f
                )
            )
            combinedMatrix.postConcat(hueMatrix)
        }

        // Render with ColorFilter
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(combinedMatrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)

        return result
    }

    /**
     * Library of 18 cinematic, film, retro, and lighting filters.
     */
    fun getFilterMatrix(filter: ImageFilter): ColorMatrix {
        val cm = ColorMatrix()
        when (filter) {
            ImageFilter.ORIGINAL -> {
                // Identity
            }
            ImageFilter.KELLY_VIVID -> {
                // Boosted vibrance, deep blues and rich golds
                cm.set(
                    floatArrayOf(
                        1.2f, 0.05f, 0.0f, 0f, 10f,
                        0.0f, 1.15f, 0.05f, 0f, 5f,
                        0.0f, 0.05f, 1.3f, 0f, 12f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            ImageFilter.HIDREAM_AURA -> {
                // Ethereal luminous glow with warm peach highlights and lifted velvet blacks
                cm.set(
                    floatArrayOf(
                        1.15f, 0.1f, 0.05f, 0f, 25f,
                        0.05f, 1.05f, 0.1f, 0f, 15f,
                        0.0f, 0.05f, 1.1f, 0f, 30f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            ImageFilter.NOIR_CLASSIC -> {
                // Dramatic contrast Ansel Adams black & white
                val lumR = 0.299f
                val lumG = 0.587f
                val lumB = 0.114f
                val contrast = 1.35f
                val t = 128f * (1f - contrast) - 10f
                cm.set(
                    floatArrayOf(
                        lumR * contrast, lumG * contrast, lumB * contrast, 0f, t,
                        lumR * contrast, lumG * contrast, lumB * contrast, 0f, t,
                        lumR * contrast, lumG * contrast, lumB * contrast, 0f, t,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            ImageFilter.CYBERPUNK -> {
                // Electric Cyan shadows and Neon Magenta highlights
                cm.set(
                    floatArrayOf(
                        1.3f, -0.2f, 0.2f, 0f, 30f,
                        -0.1f, 1.1f, 0.3f, 0f, -10f,
                        0.2f, 0.2f, 1.5f, 0f, 40f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            ImageFilter.VINTAGE_1977 -> {
                // Faded 70s Polaroid film, lifted matte blacks, warm creamy whites
                cm.set(
                    floatArrayOf(
                        0.95f, 0.15f, 0.1f, 0f, 35f,
                        0.1f, 0.9f, 0.1f, 0f, 25f,
                        0.1f, 0.1f, 0.75f, 0f, 45f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            ImageFilter.GOLDEN_HOUR -> {
                // Amber golden sunlight cast
                cm.set(
                    floatArrayOf(
                        1.25f, 0.1f, 0f, 0f, 25f,
                        0.05f, 1.1f, 0f, 0f, 10f,
                        0f, 0f, 0.75f, 0f, -15f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            ImageFilter.EMERALD_FOREST -> {
                // Moody lush forest greens and cool shadows
                cm.set(
                    floatArrayOf(
                        0.8f, 0.1f, 0.05f, 0f, -10f,
                        0.1f, 1.35f, 0.15f, 0f, 15f,
                        0.05f, 0.1f, 1.1f, 0f, 10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            ImageFilter.DRAMATIC_MATTE -> {
                // High-end cinematic desaturated matte look
                cm.set(
                    floatArrayOf(
                        0.85f, 0.1f, 0.05f, 0f, 20f,
                        0.1f, 0.85f, 0.1f, 0f, 20f,
                        0.05f, 0.1f, 0.9f, 0f, 25f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            ImageFilter.SUNSET_GLOW -> {
                // Rich purple & fiery orange gradient tone
                cm.set(
                    floatArrayOf(
                        1.35f, 0.0f, 0.1f, 0f, 20f,
                        0.1f, 0.9f, 0.1f, 0f, -10f,
                        0.25f, 0.0f, 1.3f, 0f, 25f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            ImageFilter.PASTEL_DREAM -> {
                // Soft pastel tones, dreamy low contrast
                cm.set(
                    floatArrayOf(
                        0.85f, 0.1f, 0.1f, 0f, 45f,
                        0.1f, 0.85f, 0.1f, 0f, 45f,
                        0.1f, 0.1f, 0.9f, 0f, 50f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            ImageFilter.MONOCHROME_GRAIN -> {
                // Silver halide fine grain film
                val lumR = 0.2126f
                val lumG = 0.7152f
                val lumB = 0.0722f
                cm.set(
                    floatArrayOf(
                        lumR * 1.1f, lumG * 1.1f, lumB * 1.1f, 0f, 5f,
                        lumR * 1.1f, lumG * 1.1f, lumB * 1.1f, 0f, 5f,
                        lumR * 1.1f, lumG * 1.1f, lumB * 1.1f, 0f, 5f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            ImageFilter.SEPIA_HERITAGE -> {
                // Traditional warm antique sepia
                cm.set(
                    floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            ImageFilter.INVERT_NEGATIVE -> {
                // Photo negative inversion
                cm.set(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            else -> {
                // Special non-matrix pixel filters (handled separately)
            }
        }
        return cm
    }

    /**
     * Non-linear pixel algorithms: Sketch, Emboss, Posterize, Pixelate.
     */
    private fun applyPixelFilterIfNeeded(
        source: Bitmap,
        filter: ImageFilter,
        intensity: Float
    ): Bitmap {
        return when (filter) {
            ImageFilter.POSTERIZE -> applyPosterize(source, levels = 5)
            ImageFilter.SKETCH_EDGE -> applyPencilSketch(source, intensity / 100f)
            ImageFilter.EMBOSS_3D -> applyEmboss(source)
            ImageFilter.PIXELATE_8BIT -> applyPixelate(source, blockSize = 24)
            else -> source
        }
    }

    /**
     * Offline Algorithmic AI Enhancements:
     * - Auto Dynamic Range (Adaptive histogram expansion)
     * - Auto White Balance (Gray World algorithm)
     * - Portrait Skin Glow (Fast bilateral approximation)
     * - De-haze / Clarity
     */
    fun applyAutoEnhance(source: Bitmap, type: AutoEnhanceType): Bitmap {
        return when (type) {
            AutoEnhanceType.NONE -> source
            AutoEnhanceType.AUTO_DYNAMIC_RANGE -> {
                // Sample 10,000 pixels for fast histogram calculation
                val width = source.width
                val height = source.height
                val sampleStep = max(1, (width * height) / 10000)
                val pixels = IntArray(width * height)
                source.getPixels(pixels, 0, width, 0, 0, width, height)

                var minLum = 255
                var maxLum = 0
                for (i in pixels.indices step sampleStep) {
                    val p = pixels[i]
                    val r = (p shr 16) and 0xFF
                    val g = (p shr 8) and 0xFF
                    val b = p and 0xFF
                    val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                    if (lum < minLum) minLum = lum
                    if (lum > maxLum) maxLum = lum
                }

                if (maxLum <= minLum) return source

                val contrast = 255f / (maxLum - minLum).coerceAtLeast(40)
                val brightness = -minLum * contrast

                val cm = ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, brightness,
                        0f, contrast, 0f, 0f, brightness,
                        0f, 0f, contrast, 0f, brightness,
                        0f, 0f, 0f, 1f, 0f
                    )
                )

                val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(result)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    colorFilter = ColorMatrixColorFilter(cm)
                }
                canvas.drawBitmap(source, 0f, 0f, paint)
                result
            }
            AutoEnhanceType.AUTO_WHITE_BALANCE -> {
                // Gray World Assumption: normalize mean R, G, B
                val width = source.width
                val height = source.height
                val sampleStep = max(1, (width * height) / 10000)
                val pixels = IntArray(width * height)
                source.getPixels(pixels, 0, width, 0, 0, width, height)

                var sumR = 0L
                var sumG = 0L
                var sumB = 0L
                var count = 0L

                for (i in pixels.indices step sampleStep) {
                    val p = pixels[i]
                    sumR += (p shr 16) and 0xFF
                    sumG += (p shr 8) and 0xFF
                    sumB += p and 0xFF
                    count++
                }

                if (count == 0L) return source
                val avgR = sumR.toFloat() / count
                val avgG = sumG.toFloat() / count
                val avgB = sumB.toFloat() / count
                val avgGray = (avgR + avgG + avgB) / 3f

                val scaleR = (avgGray / avgR.coerceAtLeast(1f)).coerceIn(0.7f, 1.4f)
                val scaleG = (avgGray / avgG.coerceAtLeast(1f)).coerceIn(0.7f, 1.4f)
                val scaleB = (avgGray / avgB.coerceAtLeast(1f)).coerceIn(0.7f, 1.4f)

                val cm = ColorMatrix(
                    floatArrayOf(
                        scaleR, 0f, 0f, 0f, 0f,
                        0f, scaleG, 0f, 0f, 0f,
                        0f, 0f, scaleB, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )

                val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(result)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    colorFilter = ColorMatrixColorFilter(cm)
                }
                canvas.drawBitmap(source, 0f, 0f, paint)
                result
            }
            AutoEnhanceType.PORTRAIT_SKIN_GLOW -> {
                // Soft focus glow + clarity preserve
                val blurred = fastBoxBlur(source, radius = 12)
                val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(result)
                canvas.drawBitmap(source, 0f, 0f, null)

                // Blend blurred copy with screen/overlay alpha
                val blendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    alpha = 110 // ~43% soft glow
                }
                canvas.drawBitmap(blurred, 0f, 0f, blendPaint)
                result
            }
            AutoEnhanceType.DEHAZE_CLARITY -> {
                // Boost mid-tone micro-contrast & reduce haze
                val cm = ColorMatrix(
                    floatArrayOf(
                        1.2f, -0.05f, -0.05f, 0f, -15f,
                        -0.05f, 1.2f, -0.05f, 0f, -15f,
                        -0.05f, -0.05f, 1.25f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                val sharp = applySharpness(source, 0.6f)
                val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(result)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    colorFilter = ColorMatrixColorFilter(cm)
                }
                canvas.drawBitmap(sharp, 0f, 0f, paint)
                result
            }
        }
    }

    /**
     * Pencil sketch algorithm: Invert -> Gaussian Blur -> Color Dodge with original.
     */
    fun applyPencilSketch(source: Bitmap, blendStrength: Float): Bitmap {
        val width = source.width
        val height = source.height

        // 1. Grayscale
        val grayBm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val grayCanvas = Canvas(grayBm)
        val grayCm = ColorMatrix().apply { setSaturation(0f) }
        val grayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(grayCm)
        }
        grayCanvas.drawBitmap(source, 0f, 0f, grayPaint)

        // 2. Invert Grayscale
        val invBm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val invCanvas = Canvas(invBm)
        val invCm = ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val invPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(invCm)
        }
        invCanvas.drawBitmap(grayBm, 0f, 0f, invPaint)

        // 3. Blur inverted
        val blurredInv = fastBoxBlur(invBm, radius = 10)

        // 4. Color Dodge blend grayBm with blurredInv
        val grayPixels = IntArray(width * height)
        val blurPixels = IntArray(width * height)
        val outPixels = IntArray(width * height)

        grayBm.getPixels(grayPixels, 0, width, 0, 0, width, height)
        blurredInv.getPixels(blurPixels, 0, width, 0, 0, width, height)

        for (i in outPixels.indices) {
            val base = grayPixels[i] and 0xFF
            val blend = blurPixels[i] and 0xFF
            // Color dodge formula: (base << 8) / (255 - blend)
            val dodge = if (blend == 255) 255 else min(255, (base shl 8) / (255 - blend))
            val finalVal = (base * (1f - blendStrength) + dodge * blendStrength).toInt().coerceIn(0, 255)
            outPixels[i] = (0xFF shl 24) or (finalVal shl 16) or (finalVal shl 8) or finalVal
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(outPixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * Emboss 3D relief filter.
     */
    fun applyEmboss(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        val outPixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                val p1 = pixels[(y - 1) * width + (x - 1)]
                val p2 = pixels[(y + 1) * width + (x + 1)]

                val r1 = (p1 shr 16) and 0xFF
                val g1 = (p1 shr 8) and 0xFF
                val b1 = p1 and 0xFF

                val r2 = (p2 shr 16) and 0xFF
                val g2 = (p2 shr 8) and 0xFF
                val b2 = p2 and 0xFF

                val diffR = (r1 - r2 + 128).coerceIn(0, 255)
                val diffG = (g1 - g2 + 128).coerceIn(0, 255)
                val diffB = (b1 - b2 + 128).coerceIn(0, 255)

                val gray = (0.299 * diffR + 0.587 * diffG + 0.114 * diffB).toInt()
                outPixels[idx] = (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(outPixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * Posterize: reduces continuous color gradations to quantized steps.
     */
    fun applyPosterize(source: Bitmap, levels: Int = 5): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val step = 255 / (levels - 1)
        for (i in pixels.indices) {
            val p = pixels[i]
            val a = (p shr 24) and 0xFF
            var r = (p shr 16) and 0xFF
            var g = (p shr 8) and 0xFF
            var b = p and 0xFF

            r = ((r / step) * step).coerceIn(0, 255)
            g = ((g / step) * step).coerceIn(0, 255)
            b = ((b / step) * step).coerceIn(0, 255)

            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * Pixelate / 8-Bit Mosaic effect.
     */
    fun applyPixelate(source: Bitmap, blockSize: Int = 20): Bitmap {
        val width = source.width
        val height = source.height
        val scaledDownW = max(1, width / blockSize)
        val scaledDownH = max(1, height / blockSize)

        val small = Bitmap.createScaledBitmap(source, scaledDownW, scaledDownH, false)
        val pixelated = Bitmap.createScaledBitmap(small, width, height, false)
        return pixelated
    }

    /**
     * 3x3 High-pass unsharp mask sharpness.
     */
    fun applySharpness(source: Bitmap, strength: Float): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        val outPixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val s = strength.coerceIn(0f, 2f)

        for (y in 1 until height - 1) {
            val yPrev = (y - 1) * width
            val yCurr = y * width
            val yNext = (y + 1) * width

            for (x in 1 until width - 1) {
                val center = pixels[yCurr + x]
                val left = pixels[yCurr + (x - 1)]
                val right = pixels[yCurr + (x + 1)]
                val top = pixels[yPrev + x]
                val bottom = pixels[yNext + x]

                val cR = (center shr 16) and 0xFF
                val cG = (center shr 8) and 0xFF
                val cB = center and 0xFF

                val lR = (left shr 16) and 0xFF
                val lG = (left shr 8) and 0xFF
                val lB = left and 0xFF

                val rR = (right shr 16) and 0xFF
                val rG = (right shr 8) and 0xFF
                val rB = right and 0xFF

                val tR = (top shr 16) and 0xFF
                val tG = (top shr 8) and 0xFF
                val tB = top and 0xFF

                val bR = (bottom shr 16) and 0xFF
                val bG = (bottom shr 8) and 0xFF
                val bB = bottom and 0xFF

                val nR = (cR + s * (4 * cR - (lR + rR + tR + bR))).toInt().coerceIn(0, 255)
                val nG = (cG + s * (4 * cG - (lG + rG + tG + bG))).toInt().coerceIn(0, 255)
                val nB = (cB + s * (4 * cB - (lB + rB + tB + bB))).toInt().coerceIn(0, 255)

                outPixels[yCurr + x] = (0xFF shl 24) or (nR shl 16) or (nG shl 8) or nB
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(outPixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * Color Splash: Converts all colors except selected Hue channel to monochrome.
     */
    fun applyColorSplash(source: Bitmap, channel: SplashChannel): Bitmap {
        if (channel == SplashChannel.OFF) return source

        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val targetHue = channel.targetHue
        val hueRange = channel.hueRange
        val hsv = FloatArray(3)

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF

            android.graphics.Color.RGBToHSV(r, g, b, hsv)
            val hue = hsv[0]

            var diff = abs(hue - targetHue)
            if (diff > 180f) diff = 360f - diff

            if (diff > hueRange) {
                // Desaturate to black & white
                val gray = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
                pixels[i] = (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * Blur & Bokeh / Tilt-Shift effects.
     */
    fun applyBlurAndFocus(source: Bitmap, settings: BlurSettings): Bitmap {
        val width = source.width
        val height = source.height
        val blurRadius = (settings.intensity * 0.25f).toInt().coerceIn(2, 30)
        val blurred = fastBoxBlur(source, radius = blurRadius)

        if (settings.mode == BlurMode.FULL_GAUSSIAN) {
            return blurred
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Draw blurred background
        canvas.drawBitmap(blurred, 0f, 0f, null)

        // Create Mask for sharp image area
        val maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val maskCanvas = Canvas(maskBitmap)

        when (settings.mode) {
            BlurMode.RADIAL_BOKEH -> {
                val cx = settings.centerX * width
                val cy = settings.centerY * height
                val radius = (settings.radius * min(width, height)).coerceAtLeast(40f)

                val gradient = RadialGradient(
                    cx, cy, radius,
                    intArrayOf(
                        android.graphics.Color.WHITE,
                        android.graphics.Color.WHITE,
                        android.graphics.Color.TRANSPARENT
                    ),
                    floatArrayOf(0f, 0.6f, 1f),
                    Shader.TileMode.CLAMP
                )
                val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = gradient
                }
                maskCanvas.drawCircle(cx, cy, radius, maskPaint)
            }
            BlurMode.LINEAR_TILT_SHIFT -> {
                val cy = settings.centerY * height
                val bandHeight = (settings.radius * height).coerceAtLeast(60f)
                val topSharp = (cy - bandHeight / 2f).coerceAtLeast(0f)
                val bottomSharp = (cy + bandHeight / 2f).coerceAtMost(height.toFloat())

                val gradient = LinearGradient(
                    0f, (topSharp - 80f).coerceAtLeast(0f),
                    0f, (bottomSharp + 80f).coerceAtMost(height.toFloat()),
                    intArrayOf(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.WHITE,
                        android.graphics.Color.WHITE,
                        android.graphics.Color.TRANSPARENT
                    ),
                    floatArrayOf(0f, 0.25f, 0.75f, 1f),
                    Shader.TileMode.CLAMP
                )
                val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = gradient
                }
                maskCanvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), maskPaint)
            }
            else -> {}
        }

        // Apply sharp image through mask
        val sharpWithMask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val sharpCanvas = Canvas(sharpWithMask)
        sharpCanvas.drawBitmap(maskBitmap, 0f, 0f, null)
        val paintIn = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        sharpCanvas.drawBitmap(source, 0f, 0f, paintIn)

        // Draw masked sharp layer over blurred background
        canvas.drawBitmap(sharpWithMask, 0f, 0f, null)

        return result
    }

    /**
     * Radial Vignette effect.
     */
    fun applyVignette(source: Bitmap, strength: Float): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)

        val cx = width / 2f
        val cy = height / 2f
        val radius = sqrt((cx * cx + cy * cy).toDouble()).toFloat()
        val darkAlpha = (strength * 220).toInt().coerceIn(0, 255)

        val gradient = RadialGradient(
            cx, cy, radius,
            intArrayOf(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.argb(darkAlpha, 0, 0, 0)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        return result
    }

    /**
     * Grain / Analog Film noise generator.
     */
    fun applyGrain(source: Bitmap, intensity: Float): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val noiseRange = (intensity * 40).toInt()
        val random = java.util.Random(42) // Consistent pseudo-random seed

        for (i in pixels.indices) {
            val noise = random.nextInt(noiseRange * 2 + 1) - noiseRange
            val p = pixels[i]
            val r = (((p shr 16) and 0xFF) + noise).coerceIn(0, 255)
            val g = (((p shr 8) and 0xFF) + noise).coerceIn(0, 255)
            val b = ((p and 0xFF) + noise).coerceIn(0, 255)
            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * Renders vector doodle paths and typography text overlays onto the bitmap.
     */
    fun renderOverlays(
        source: Bitmap,
        doodles: List<DoodlePath>,
        texts: List<TextOverlay>
    ): Bitmap {
        if (doodles.isEmpty() && texts.isEmpty()) return source

        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)

        // Draw Doodles
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val scaleFactor = source.width / 1000f

        for (doodle in doodles) {
            if (doodle.points.size < 2) continue

            strokePaint.strokeWidth = doodle.strokeWidth * scaleFactor
            strokePaint.color = doodle.colorArgb

            when (doodle.brushMode) {
                com.example.model.BrushMode.NORMAL -> {
                    strokePaint.xfermode = null
                    strokePaint.alpha = (doodle.opacity * 255).toInt()
                }
                com.example.model.BrushMode.NEON_GLOW -> {
                    strokePaint.xfermode = null
                    strokePaint.alpha = 255
                    // Draw outer glow
                    strokePaint.strokeWidth = doodle.strokeWidth * scaleFactor * 2.2f
                    strokePaint.alpha = 90
                    val glowPath = android.graphics.Path().apply {
                        moveTo(doodle.points[0].x * source.width, doodle.points[0].y * source.height)
                        for (i in 1 until doodle.points.size) {
                            lineTo(doodle.points[i].x * source.width, doodle.points[i].y * source.height)
                        }
                    }
                    canvas.drawPath(glowPath, strokePaint)
                    strokePaint.strokeWidth = doodle.strokeWidth * scaleFactor
                    strokePaint.alpha = 255
                }
                com.example.model.BrushMode.HIGHLIGHTER -> {
                    strokePaint.xfermode = null
                    strokePaint.alpha = 110 // Semi-transparent
                }
                com.example.model.BrushMode.ERASER -> {
                    // Draw with base image pixels or neutral mask
                    strokePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                }
            }

            val path = android.graphics.Path().apply {
                moveTo(doodle.points[0].x * source.width, doodle.points[0].y * source.height)
                for (i in 1 until doodle.points.size) {
                    lineTo(doodle.points[i].x * source.width, doodle.points[i].y * source.height)
                }
            }
            canvas.drawPath(path, strokePaint)
        }

        // Draw Texts
        for (overlay in texts) {
            val textPaint = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = overlay.textColorArgb
                textSize = overlay.textSizeSp * scaleFactor * 1.8f
                textAlign = Paint.Align.CENTER
                typeface = when (overlay.fontStyle) {
                    TextStyleFont.MODERN_SANS -> android.graphics.Typeface.DEFAULT_BOLD
                    TextStyleFont.ELEGANT_SERIF -> android.graphics.Typeface.SERIF
                    TextStyleFont.BOLD_IMPACT -> android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    TextStyleFont.MONOSPACE -> android.graphics.Typeface.MONOSPACE
                    TextStyleFont.CURSIVE -> android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.ITALIC)
                }
                if (overlay.hasShadow) {
                    setShadowLayer(8f * scaleFactor, 3f * scaleFactor, 3f * scaleFactor, android.graphics.Color.BLACK)
                }
            }

            val posX = overlay.normalizedX * source.width
            val posY = overlay.normalizedY * source.height

            canvas.save()
            canvas.rotate(overlay.rotation, posX, posY)

            // Optional background chip
            if (overlay.backgroundColorArgb != android.graphics.Color.TRANSPARENT) {
                val textBounds = android.graphics.Rect()
                textPaint.getTextBounds(overlay.text, 0, overlay.text.length, textBounds)
                val padX = 24f * scaleFactor
                val padY = 16f * scaleFactor
                val chipRect = android.graphics.RectF(
                    posX - textBounds.width() / 2f - padX,
                    posY - textBounds.height() - padY,
                    posX + textBounds.width() / 2f + padX,
                    posY + padY
                )
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = overlay.backgroundColorArgb
                }
                canvas.drawRoundRect(chipRect, 16f * scaleFactor, 16f * scaleFactor, bgPaint)
            }

            canvas.drawText(overlay.text, posX, posY, textPaint)
            canvas.restore()
        }

        return result
    }

    /**
     * Fast, memory-safe Box blur implementation for local image processing.
     */
    fun fastBoxBlur(source: Bitmap, radius: Int): Bitmap {
        if (radius < 1) return source
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val wm = width - 1
        val hm = height - 1
        val wh = width * height
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(max(width, height))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (idx in 0 until 256 * divsum) {
            dv[idx] = idx / divsum
        }

        yw = 0
        yi = 0

        for (curY in 0 until height) {
            rsum = 0
            gsum = 0
            bsum = 0
            for (curI in -radius..radius) {
                p = pixels[yi + min(wm, max(curI, 0))]
                rsum += (p shr 16) and 0xFF
                gsum += (p shr 8) and 0xFF
                bsum += p and 0xFF
            }
            for (curX in 0 until width) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                if (curY == 0) {
                    vmin[curX] = min(curX + radius + 1, wm)
                }
                val p1 = pixels[yw + vmin[curX]]
                val p2 = pixels[yw + max(curX - radius, 0)]

                rsum += ((p1 shr 16) and 0xFF) - ((p2 shr 16) and 0xFF)
                gsum += ((p1 shr 8) and 0xFF) - ((p2 shr 8) and 0xFF)
                bsum += (p1 and 0xFF) - (p2 and 0xFF)
                yi++
            }
            yw += width
        }

        for (curX in 0 until width) {
            rsum = 0
            gsum = 0
            bsum = 0
            yp = -radius * width
            for (curI in -radius..radius) {
                yi = max(0, yp) + curX
                rsum += r[yi]
                gsum += g[yi]
                bsum += b[yi]
                yp += width
            }
            yi = curX
            for (curY in 0 until height) {
                pixels[yi] = (0xFF shl 24) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                if (curX == 0) {
                    vmin[curY] = min(curY + radius + 1, hm) * width
                }
                val p1 = curX + vmin[curY]
                val p2 = curX + max(curY - radius, 0) * width

                rsum += r[p1] - r[p2]
                gsum += g[p1] - g[p2]
                bsum += b[p1] - b[p2]

                yi += width
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }
}
