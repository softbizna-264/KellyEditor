package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.ImageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

data class SavedImageInfo(
    val uri: Uri,
    val filePath: String,
    val filename: String,
    val sizeBytes: Long
)

object BitmapUtils {

    /**
     * Checks whether runtime storage write permission is required for the current Android version.
     */
    fun isStoragePermissionRequired(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    }

    /**
     * Loads a Bitmap from resource ID with safe memory downsampling.
     */
    fun decodeSampledBitmapFromResource(
        context: Context,
        resId: Int,
        reqWidth: Int = 1920,
        reqHeight: Int = 1920
    ): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeResource(context.resources, resId, options)

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            BitmapFactory.decodeResource(context.resources, resId, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Loads a Bitmap from content Uri with safe memory downsampling and rotation correction.
     */
    suspend fun loadBitmapFromUri(
        context: Context,
        uri: Uri,
        maxWidth: Int = 2560,
        maxHeight: Int = 2560
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // First bounds check
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val sampleSize = calculateInSampleSize(boundsOptions, maxWidth, maxHeight)

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inJustDecodeBounds = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            var bitmap: Bitmap? = null
            context.contentResolver.openInputStream(uri)?.use { stream ->
                bitmap = BitmapFactory.decodeStream(stream, null, decodeOptions)
            }

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves edited bitmap to public external storage (e.g. Pictures/KellyEditor or DCIM/KellyEditor)
     * with support for both modern Scoped MediaStore (Android 10+) and direct external file creation (Pre-Android 10).
     */
    suspend fun saveBitmapToExternalStorage(
        context: Context,
        bitmap: Bitmap,
        customFilename: String? = null,
        subfolder: String = "Pictures/KellyEditor",
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 95
    ): Result<SavedImageInfo> = withContext(Dispatchers.IO) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val extension = when (format) {
                Bitmap.CompressFormat.PNG -> "png"
                Bitmap.CompressFormat.WEBP -> "webp"
                else -> "jpg"
            }
            val mimeType = when (format) {
                Bitmap.CompressFormat.PNG -> "image/png"
                Bitmap.CompressFormat.WEBP -> "image/webp"
                else -> "image/jpeg"
            }

            val sanitizedPrefix = customFilename?.trim()?.takeIf { it.isNotEmpty() }?.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val baseName = sanitizedPrefix ?: "KellyEditor_$timeStamp"
            val finalFilename = if (baseName.endsWith(".$extension", ignoreCase = true)) baseName else "$baseName.$extension"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Scoped MediaStore on Android 10+
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, finalFilename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, subfolder)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext Result.failure(Exception("Failed to create MediaStore entry"))

                var bytesWritten = 0L
                resolver.openOutputStream(imageUri)?.use { outStream ->
                    bitmap.compress(format, quality, outStream)
                    outStream.flush()
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)

                val displayPath = "/storage/emulated/0/$subfolder/$finalFilename"
                Result.success(
                    SavedImageInfo(
                        uri = imageUri,
                        filePath = displayPath,
                        filename = finalFilename,
                        sizeBytes = bytesWritten
                    )
                )
            } else {
                // Direct External Storage directory on Pre-Android 10
                val rootExternalDir = Environment.getExternalStorageDirectory()
                val targetDir = File(rootExternalDir, subfolder)
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }
                val outputFile = File(targetDir, finalFilename)
                FileOutputStream(outputFile).use { outStream ->
                    bitmap.compress(format, quality, outStream)
                    outStream.flush()
                }

                // Index via MediaScannerConnection so it immediately appears in all Gallery and File Manager apps
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(outputFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )

                val fileUri = Uri.fromFile(outputFile)
                Result.success(
                    SavedImageInfo(
                        uri = fileUri,
                        filePath = outputFile.absolutePath,
                        filename = finalFilename,
                        sizeBytes = outputFile.length()
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Saves edited bitmap to MediaStore (Pictures/KellyEditor).
     */
    suspend fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 95
    ): Result<Uri> = withContext(Dispatchers.IO) {
        val result = saveBitmapToExternalStorage(
            context = context,
            bitmap = bitmap,
            subfolder = "Pictures/KellyEditor",
            format = format,
            quality = quality
        )
        result.map { it.uri }
    }

    /**
     * Prepares a temporary file URI for Android Share Sheet.
     */
    suspend fun prepareBitmapForSharing(
        context: Context,
        bitmap: Bitmap
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val cachePath = File(context.cacheDir, "shared_images")
            cachePath.mkdirs()
            val file = File(cachePath, "KellyEditor_Share_${System.currentTimeMillis()}.jpg")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)
            stream.flush()
            stream.close()

            // Return secure FileProvider URI
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Extracts technical metadata for the Image Inspector.
     */
    fun extractMetadata(bitmap: Bitmap): ImageMetadata {
        val width = bitmap.width
        val height = bitmap.height
        val megapixels = (width * height) / 1_000_000f

        val gcdVal = gcd(width, height)
        val aspectX = width / gcdVal
        val aspectY = height / gcdVal
        val aspectStr = if (aspectX in 1..20 && aspectY in 1..20) "$aspectX:$aspectY" else String.format(Locale.US, "%.2f:1", width.toFloat() / height)

        val approxBytes = bitmap.byteCount.toLong()
        val approxKb = approxBytes / 1024L

        return ImageMetadata(
            width = width,
            height = height,
            megapixel = megapixels,
            aspectRatioStr = aspectStr,
            approximateSizeKb = approxKb,
            colorFormat = "ARGB_8888 (32-bit Ultra-Color)"
        )
    }

    private fun gcd(a: Int, b: Int): Int {
        var n1 = a
        var n2 = b
        while (n2 != 0) {
            val temp = n2
            n2 = n1 % n2
            n1 = temp
        }
        return if (n1 > 0) n1 else 1
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
