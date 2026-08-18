package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.engine.BitmapUtils
import com.example.engine.ImageProcessor
import com.example.engine.SavedImageInfo
import com.example.model.Adjustments
import com.example.model.AutoEnhanceType
import com.example.model.BlurMode
import com.example.model.BlurSettings
import com.example.model.BrushMode
import com.example.model.CropRatio
import com.example.model.DoodlePath
import com.example.model.DoodlePoint
import com.example.model.EditState
import com.example.model.EditorTab
import com.example.model.ImageFilter
import com.example.model.ImageMetadata
import com.example.model.NormalizedRect
import com.example.model.SamplePhoto
import com.example.model.SplashChannel
import com.example.model.TextOverlay
import com.example.model.TextStyleFont
import com.example.model.TransformState
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext

    // Built-in Sample Photos for instant offline testing
    val samplePhotos = listOf(
        SamplePhoto(
            title = "Golden Hour Portrait",
            category = "Portrait",
            drawableResId = R.drawable.img_sample_portrait,
            description = "Natural sunlight portrait ideal for retouching, skin glow & cinematic filters"
        ),
        SamplePhoto(
            title = "Alpine Lake Vista",
            category = "Landscape",
            drawableResId = R.drawable.img_sample_landscape,
            description = "High detail alpine mountain reflection ideal for vibrant colors, HDR & tilt-shift"
        )
    )

    // Raw unmodified base bitmap
    private var baseBitmap: Bitmap? = null

    // Downscaled thumbnail of base for filter strip preview generation
    private var baseThumbnail: Bitmap? = null

    // Rendered output bitmap for display in canvas
    private val _displayBitmap = MutableStateFlow<Bitmap?>(null)
    val displayBitmap: StateFlow<Bitmap?> = _displayBitmap.asStateFlow()

    // Filter preview thumbnails (Filter -> Bitmap)
    private val _filterPreviews = MutableStateFlow<Map<ImageFilter, Bitmap>>(emptyMap())
    val filterPreviews: StateFlow<Map<ImageFilter, Bitmap>> = _filterPreviews.asStateFlow()

    // Current editing parameters
    private val _editState = MutableStateFlow(EditState())
    val editState: StateFlow<EditState> = _editState.asStateFlow()

    // Active Bottom Navigation Tab
    private val _activeTab = MutableStateFlow(EditorTab.ADJUST)
    val activeTab: StateFlow<EditorTab> = _activeTab.asStateFlow()

    // Undo / Redo History Stack
    private val history = mutableListOf<EditState>()
    private var historyIndex = -1

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Comparison Mode (Holding button shows original unmodified)
    private val _isComparing = MutableStateFlow(false)
    val isComparing: StateFlow<Boolean> = _isComparing.asStateFlow()

    // Active Text Overlay for styling
    private val _selectedTextId = MutableStateFlow<String?>(null)
    val selectedTextId: StateFlow<String?> = _selectedTextId.asStateFlow()

    // Active Drawing In Progress
    private val _currentDoodlePoints = MutableStateFlow<List<DoodlePoint>>(emptyList())
    val currentDoodlePoints: StateFlow<List<DoodlePoint>> = _currentDoodlePoints.asStateFlow()

    // Current Doodle Brush Settings
    private val _activeBrushColor = MutableStateFlow(android.graphics.Color.CYAN)
    val activeBrushColor: StateFlow<Int> = _activeBrushColor.asStateFlow()

    private val _activeBrushWidth = MutableStateFlow(14f)
    val activeBrushWidth: StateFlow<Float> = _activeBrushWidth.asStateFlow()

    private val _activeBrushMode = MutableStateFlow(BrushMode.NORMAL)
    val activeBrushMode: StateFlow<BrushMode> = _activeBrushMode.asStateFlow()

    // Processing indicator
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // Image Metadata
    private val _imageMetadata = MutableStateFlow<ImageMetadata?>(null)
    val imageMetadata: StateFlow<ImageMetadata?> = _imageMetadata.asStateFlow()

    // Dialog & Sheet States
    private val _showSampleSheet = MutableStateFlow(false)
    val showSampleSheet: StateFlow<Boolean> = _showSampleSheet.asStateFlow()

    private val _showInfoDialog = MutableStateFlow(false)
    val showInfoDialog: StateFlow<Boolean> = _showInfoDialog.asStateFlow()

    private val _showExportDialog = MutableStateFlow(false)
    val showExportDialog: StateFlow<Boolean> = _showExportDialog.asStateFlow()

    private val _savedImageResult = MutableStateFlow<SavedImageInfo?>(null)
    val savedImageResult: StateFlow<SavedImageInfo?> = _savedImageResult.asStateFlow()

    private val _saveToastMessage = MutableStateFlow<String?>(null)
    val saveToastMessage: StateFlow<String?> = _saveToastMessage.asStateFlow()

    private var renderJob: Job? = null

    init {
        // Load default portrait sample on first launch
        loadSamplePhoto(samplePhotos[0])
    }

    fun selectTab(tab: EditorTab) {
        _activeTab.value = tab
    }

    fun setShowSampleSheet(show: Boolean) {
        _showSampleSheet.value = show
    }

    fun setShowInfoDialog(show: Boolean) {
        _showInfoDialog.value = show
    }

    fun setShowExportDialog(show: Boolean) {
        _showExportDialog.value = show
    }

    fun clearSaveMessage() {
        _saveToastMessage.value = null
    }

    fun setComparing(comparing: Boolean) {
        _isComparing.value = comparing
        if (comparing) {
            _displayBitmap.value = baseBitmap
        } else {
            renderCurrentState()
        }
    }

    fun loadSamplePhoto(sample: SamplePhoto) {
        viewModelScope.launch {
            _isProcessing.value = true
            val bitmap = BitmapUtils.decodeSampledBitmapFromResource(
                context,
                sample.drawableResId,
                1920,
                1920
            )
            bitmap?.let { setLoadedBitmap(it) }
            _isProcessing.value = false
            _showSampleSheet.value = false
        }
    }

    fun loadFromUri(uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            val bitmap = BitmapUtils.loadBitmapFromUri(context, uri, 2560, 2560)
            if (bitmap != null) {
                setLoadedBitmap(bitmap)
            } else {
                _saveToastMessage.value = "Unable to load selected image"
            }
            _isProcessing.value = false
        }
    }

    private fun setLoadedBitmap(bitmap: Bitmap) {
        baseBitmap = bitmap
        _imageMetadata.value = BitmapUtils.extractMetadata(bitmap)

        // Generate downscaled thumbnail for filter preview bar
        val thumbScale = 140f / max(bitmap.width, bitmap.height)
        val tw = (bitmap.width * thumbScale).toInt().coerceAtLeast(60)
        val th = (bitmap.height * thumbScale).toInt().coerceAtLeast(60)
        baseThumbnail = Bitmap.createScaledBitmap(bitmap, tw, th, true)

        // Reset state
        val initialState = EditState()
        _editState.value = initialState
        history.clear()
        history.add(initialState)
        historyIndex = 0
        updateUndoRedo()

        renderCurrentState()
        generateFilterPreviews()
    }

    private fun generateFilterPreviews() {
        val thumb = baseThumbnail ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val previews = mutableMapOf<ImageFilter, Bitmap>()
            for (filter in ImageFilter.values()) {
                val dummyState = EditState(activeFilter = filter, filterIntensity = 100f)
                val renderedThumb = ImageProcessor.processImage(thumb, dummyState, isThumbnailPreview = true)
                previews[filter] = renderedThumb
            }
            _filterPreviews.value = previews
        }
    }

    /**
     * Updates edit state with debounce to guarantee continuous fluid 60fps UI.
     */
    fun updateEditState(recordInHistory: Boolean = false, transform: (EditState) -> EditState) {
        val newState = transform(_editState.value)
        _editState.value = newState

        if (recordInHistory) {
            pushToHistory(newState)
        }

        renderDebounced()
    }

    private fun renderDebounced() {
        renderJob?.cancel()
        renderJob = viewModelScope.launch(Dispatchers.Default) {
            delay(16) // ~60fps debounce
            val base = baseBitmap ?: return@launch
            val state = _editState.value
            val rendered = ImageProcessor.processImage(base, state, isThumbnailPreview = true)
            withContext(Dispatchers.Main) {
                _displayBitmap.value = rendered
            }
        }
    }

    private fun renderCurrentState() {
        val base = baseBitmap ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val state = _editState.value
            val rendered = ImageProcessor.processImage(base, state, isThumbnailPreview = true)
            withContext(Dispatchers.Main) {
                _displayBitmap.value = rendered
            }
        }
    }

    private fun pushToHistory(state: EditState) {
        // Cut forward history if branched
        while (history.size > historyIndex + 1) {
            history.removeAt(history.size - 1)
        }
        history.add(state)
        // Keep max 30 history states to conserve memory
        if (history.size > 30) {
            history.removeAt(0)
        } else {
            historyIndex++
        }
        updateUndoRedo()
    }

    fun undo() {
        if (historyIndex > 0) {
            historyIndex--
            _editState.value = history[historyIndex]
            updateUndoRedo()
            renderCurrentState()
        }
    }

    fun redo() {
        if (historyIndex < history.size - 1) {
            historyIndex++
            _editState.value = history[historyIndex]
            updateUndoRedo()
            renderCurrentState()
        }
    }

    fun resetAllEdits() {
        val resetState = EditState()
        updateEditState(recordInHistory = true) { resetState }
    }

    fun resetAdjustments() {
        updateEditState(recordInHistory = true) { it.copy(adjustments = Adjustments()) }
    }

    private fun updateUndoRedo() {
        _canUndo.value = historyIndex > 0
        _canRedo.value = historyIndex < history.size - 1
    }

    // --- ADJUSTMENTS ---
    fun setBrightness(value: Float) {
        updateEditState { it.copy(adjustments = it.adjustments.copy(brightness = value)) }
    }

    fun setContrast(value: Float) {
        updateEditState { it.copy(adjustments = it.adjustments.copy(contrast = value)) }
    }

    fun setSaturation(value: Float) {
        updateEditState { it.copy(adjustments = it.adjustments.copy(saturation = value)) }
    }

    fun setExposure(value: Float) {
        updateEditState { it.copy(adjustments = it.adjustments.copy(exposure = value)) }
    }

    fun setWarmth(value: Float) {
        updateEditState { it.copy(adjustments = it.adjustments.copy(warmth = value)) }
    }

    fun setTint(value: Float) {
        updateEditState { it.copy(adjustments = it.adjustments.copy(tint = value)) }
    }

    fun setSharpness(value: Float) {
        updateEditState { it.copy(adjustments = it.adjustments.copy(sharpness = value)) }
    }

    fun setVignette(value: Float) {
        updateEditState { it.copy(adjustments = it.adjustments.copy(vignette = value)) }
    }

    fun setHue(value: Float) {
        updateEditState { it.copy(adjustments = it.adjustments.copy(hue = value)) }
    }

    fun setGrain(value: Float) {
        updateEditState { it.copy(adjustments = it.adjustments.copy(grain = value)) }
    }

    fun commitAdjustmentChange() {
        pushToHistory(_editState.value)
    }

    // --- FILTERS ---
    fun selectFilter(filter: ImageFilter) {
        updateEditState(recordInHistory = true) {
            it.copy(activeFilter = filter)
        }
    }

    fun setFilterIntensity(intensity: Float) {
        updateEditState { it.copy(filterIntensity = intensity) }
    }

    // --- AI TOOLS ---
    fun selectAutoEnhance(type: AutoEnhanceType) {
        updateEditState(recordInHistory = true) {
            it.copy(autoEnhance = type)
        }
    }

    // --- TRANSFORM & CROP ---
    fun rotateClockwise() {
        val current = _editState.value.transform.rotationDegrees
        val next = (current + 90) % 360
        updateEditState(recordInHistory = true) {
            it.copy(transform = it.transform.copy(rotationDegrees = next))
        }
    }

    fun flipHorizontal() {
        val current = _editState.value.transform.flipHorizontal
        updateEditState(recordInHistory = true) {
            it.copy(transform = it.transform.copy(flipHorizontal = !current))
        }
    }

    fun flipVertical() {
        val current = _editState.value.transform.flipVertical
        updateEditState(recordInHistory = true) {
            it.copy(transform = it.transform.copy(flipVertical = !current))
        }
    }

    fun setStraightenAngle(angle: Float) {
        updateEditState {
            it.copy(transform = it.transform.copy(straightenAngle = angle))
        }
    }

    fun setCropRatio(ratio: CropRatio) {
        val currentTransform = _editState.value.transform
        val base = baseBitmap ?: return

        val cropRect = if (ratio == CropRatio.ORIGINAL || ratio == CropRatio.FREE) {
            NormalizedRect(0f, 0f, 1f, 1f)
        } else {
            val targetAspect = ratio.ratioX / ratio.ratioY
            val currentAspect = base.width.toFloat() / base.height.toFloat()

            if (targetAspect > currentAspect) {
                // Wider than image -> crop top & bottom
                val targetHeight = (base.width / targetAspect) / base.height
                val padY = ((1f - targetHeight) / 2f).coerceIn(0f, 0.45f)
                NormalizedRect(0f, padY, 1f, 1f - padY)
            } else {
                // Taller than image -> crop left & right
                val targetWidth = (base.height * targetAspect) / base.width
                val padX = ((1f - targetWidth) / 2f).coerceIn(0f, 0.45f)
                NormalizedRect(padX, 0f, 1f - padX, 1f)
            }
        }

        updateEditState(recordInHistory = true) {
            it.copy(transform = currentTransform.copy(selectedCropRatio = ratio, cropRectNormalized = cropRect))
        }
    }

    // --- BLUR & FOCUS ---
    fun setBlurMode(mode: BlurMode) {
        updateEditState(recordInHistory = true) {
            it.copy(blur = it.blur.copy(mode = mode))
        }
    }

    fun setBlurIntensity(intensity: Float) {
        updateEditState {
            it.copy(blur = it.blur.copy(intensity = intensity))
        }
    }

    fun setBlurRadius(radius: Float) {
        updateEditState {
            it.copy(blur = it.blur.copy(radius = radius))
        }
    }

    fun setBlurCenter(cx: Float, cy: Float) {
        updateEditState {
            it.copy(blur = it.blur.copy(centerX = cx, centerY = cy))
        }
    }

    // --- COLOR SPLASH ---
    fun selectSplashChannel(channel: SplashChannel) {
        updateEditState(recordInHistory = true) {
            it.copy(splash = channel)
        }
    }

    // --- DOODLE / BRUSH ---
    fun setBrushColor(colorArgb: Int) {
        _activeBrushColor.value = colorArgb
    }

    fun setBrushWidth(width: Float) {
        _activeBrushWidth.value = width
    }

    fun setBrushMode(mode: BrushMode) {
        _activeBrushMode.value = mode
    }

    fun addDoodlePoint(point: DoodlePoint) {
        _currentDoodlePoints.value = _currentDoodlePoints.value + point
    }

    fun finishDoodlePath() {
        val points = _currentDoodlePoints.value
        if (points.size >= 2) {
            val newPath = DoodlePath(
                points = points,
                colorArgb = _activeBrushColor.value,
                strokeWidth = _activeBrushWidth.value,
                brushMode = _activeBrushMode.value
            )
            updateEditState(recordInHistory = true) {
                it.copy(doodlePaths = it.doodlePaths + newPath)
            }
        }
        _currentDoodlePoints.value = emptyList()
    }

    fun clearAllDoodles() {
        updateEditState(recordInHistory = true) {
            it.copy(doodlePaths = emptyList())
        }
    }

    // --- TEXT OVERLAYS ---
    fun addTextOverlay(text: String) {
        val newOverlay = TextOverlay(
            text = text,
            normalizedX = 0.5f,
            normalizedY = 0.5f
        )
        _selectedTextId.value = newOverlay.id
        updateEditState(recordInHistory = true) {
            it.copy(textOverlays = it.textOverlays + newOverlay)
        }
    }

    fun selectTextOverlay(id: String?) {
        _selectedTextId.value = id
    }

    fun updateSelectedText(transform: (TextOverlay) -> TextOverlay) {
        val currentId = _selectedTextId.value ?: return
        updateEditState { state ->
            val updated = state.textOverlays.map {
                if (it.id == currentId) transform(it) else it
            }
            state.copy(textOverlays = updated)
        }
    }

    fun deleteSelectedText() {
        val currentId = _selectedTextId.value ?: return
        updateEditState(recordInHistory = true) { state ->
            state.copy(textOverlays = state.textOverlays.filterNot { it.id == currentId })
        }
        _selectedTextId.value = null
    }

    // --- EXPORT & SAVE TO EXTERNAL STORAGE ---
    fun saveToExternalStorage(
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 95,
        customFilename: String? = null,
        subfolder: String = "Pictures/KellyEditor"
    ) {
        val base = baseBitmap ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            // Full resolution rendering with high quality
            val fullOutput = withContext(Dispatchers.Default) {
                ImageProcessor.processImage(base, _editState.value, isThumbnailPreview = false)
            }
            val result = BitmapUtils.saveBitmapToExternalStorage(
                context = context,
                bitmap = fullOutput,
                customFilename = customFilename,
                subfolder = subfolder,
                format = format,
                quality = quality
            )
            _isProcessing.value = false
            _showExportDialog.value = false

            if (result.isSuccess) {
                val info = result.getOrNull()
                _savedImageResult.value = info
                _saveToastMessage.value = "Saved to external storage: ${info?.filename ?: ""}"
            } else {
                _saveToastMessage.value = "Save failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun saveToDevice(format: Bitmap.CompressFormat, quality: Int) {
        saveToExternalStorage(
            format = format,
            quality = quality,
            customFilename = null,
            subfolder = "Pictures/KellyEditor"
        )
    }

    fun clearSavedImageResult() {
        _savedImageResult.value = null
    }

    fun openSavedImage(uri: Uri) {
        try {
            val viewIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/*")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viewIntent)
        } catch (e: Exception) {
            _saveToastMessage.value = "Unable to open image viewer: ${e.message}"
        }
    }

    fun shareSavedImageUri(uri: Uri) {
        try {
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Saved Image via").apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            _saveToastMessage.value = "Unable to share image: ${e.message}"
        }
    }

    fun shareImage() {
        val base = baseBitmap ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            val fullOutput = withContext(Dispatchers.Default) {
                ImageProcessor.processImage(base, _editState.value, isThumbnailPreview = false)
            }
            val shareUri = BitmapUtils.prepareBitmapForSharing(context, fullOutput)
            _isProcessing.value = false

            if (shareUri != null) {
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(android.content.Intent.EXTRA_STREAM, shareUri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Edited Image via").apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
    }
}
