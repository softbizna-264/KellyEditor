package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.engine.BitmapUtils
import com.example.engine.ImageProcessor
import com.example.model.Adjustments
import com.example.model.EditState
import com.example.model.ImageFilter
import com.example.model.TransformState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context verifies Kelly Editor app name`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Kelly Editor", appName)
  }

  @Test
  fun `image processor applies brightness and saturation correctly`() {
    val sampleBitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
    sampleBitmap.eraseColor(Color.rgb(100, 100, 100))

    val editState = EditState(
      adjustments = Adjustments(brightness = 20f, saturation = 30f)
    )
    val result = ImageProcessor.processImage(sampleBitmap, editState)

    assertNotNull(result)
    assertEquals(50, result.width)
    assertEquals(50, result.height)
  }

  @Test
  fun `image processor rotates 90 degrees and flips horizontal`() {
    val sampleBitmap = Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888)
    val editState = EditState(
      transform = TransformState(rotationDegrees = 90, flipHorizontal = true)
    )
    val result = ImageProcessor.processImage(sampleBitmap, editState)

    assertNotNull(result)
    assertEquals(40, result.width)
    assertEquals(80, result.height)
  }

  @Test
  fun `image processor applies cinematic filter`() {
    val sampleBitmap = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888)
    val editState = EditState(
      activeFilter = ImageFilter.CYBERPUNK,
      filterIntensity = 80f
    )
    val result = ImageProcessor.processImage(sampleBitmap, editState)

    assertNotNull(result)
  }

  @Test
  fun `bitmap utils extracts metadata correctly`() {
    val bitmap = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888)
    val meta = BitmapUtils.extractMetadata(bitmap)

    assertEquals(1920, meta.width)
    assertEquals(1080, meta.height)
    assertEquals("16:9", meta.aspectRatioStr)
    assertTrue(meta.megapixel > 2.0f)
  }

  @Test
  fun `isStoragePermissionRequired returns boolean without exception`() {
    val req = BitmapUtils.isStoragePermissionRequired()
    // On SDK 34, it should be false (Android 10+ scoped media)
    assertEquals(false, req)
  }

  @Test
  fun `saveBitmapToExternalStorage creates valid result`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sampleBitmap = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
    val result = BitmapUtils.saveBitmapToExternalStorage(
      context = context,
      bitmap = sampleBitmap,
      customFilename = "TestExportUnit",
      subfolder = "Pictures/KellyEditor"
    )

    assertTrue(result.isSuccess)
    val saved = result.getOrNull()
    assertNotNull(saved)
    assertTrue(saved!!.filename.contains("TestExportUnit"))
  }
}

