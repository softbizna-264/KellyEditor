package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.EditorTab
import com.example.ui.components.EditorBottomBar
import com.example.ui.components.EditorTopBar
import com.example.ui.components.ImageCanvasView
import com.example.ui.dialogs.ExportDialog
import com.example.ui.dialogs.ImageInfoDialog
import com.example.ui.dialogs.SamplePickerSheet
import com.example.ui.dialogs.SaveSuccessDialog
import com.example.ui.tabs.AdjustTabContent
import com.example.ui.tabs.AiToolsTabContent
import com.example.ui.tabs.BlurFocusTabContent
import com.example.ui.tabs.DoodleTabContent
import com.example.ui.tabs.FilterTabContent
import com.example.ui.tabs.SplashTabContent
import com.example.ui.tabs.TextTabContent
import com.example.ui.tabs.TransformTabContent
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioDarkBackground
import com.example.ui.theme.StudioSurfaceDark
import com.example.viewmodel.EditorViewModel

@Composable
fun KellyEditorScreen(
    viewModel: EditorViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val displayBitmap by viewModel.displayBitmap.collectAsState()
    val editState by viewModel.editState.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val filterPreviews by viewModel.filterPreviews.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val isComparing by viewModel.isComparing.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val imageMetadata by viewModel.imageMetadata.collectAsState()
    val selectedTextId by viewModel.selectedTextId.collectAsState()
    val activeBrushColor by viewModel.activeBrushColor.collectAsState()
    val activeBrushWidth by viewModel.activeBrushWidth.collectAsState()
    val activeBrushMode by viewModel.activeBrushMode.collectAsState()

    val showSampleSheet by viewModel.showSampleSheet.collectAsState()
    val showInfoDialog by viewModel.showInfoDialog.collectAsState()
    val showExportDialog by viewModel.showExportDialog.collectAsState()
    val savedImageResult by viewModel.savedImageResult.collectAsState()
    val saveToastMessage by viewModel.saveToastMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(saveToastMessage) {
        saveToastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearSaveMessage()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDarkBackground)
            .statusBarsPadding()
            .testTag("kelly_editor_scaffold"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            EditorTopBar(
                canUndo = canUndo,
                canRedo = canRedo,
                isComparing = isComparing,
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                onCompareChange = viewModel::setComparing,
                onOpenSamples = { viewModel.setShowSampleSheet(true) },
                onOpenInfo = { viewModel.setShowInfoDialog(true) },
                onShare = viewModel::shareImage,
                onSave = { viewModel.setShowExportDialog(true) }
            )
        },
        bottomBar = {
            EditorBottomBar(
                activeTab = activeTab,
                onTabSelected = { tab ->
                    if (tab == EditorTab.EXPORT) {
                        viewModel.setShowExportDialog(true)
                    } else {
                        viewModel.selectTab(tab)
                    }
                }
            )
        },
        containerColor = StudioDarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Center Canvas Viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                ImageCanvasView(
                    bitmap = displayBitmap,
                    isProcessing = isProcessing,
                    isComparing = isComparing,
                    activeTab = activeTab,
                    blurSettings = editState.blur,
                    onBlurCenterChange = viewModel::setBlurCenter,
                    onDoodlePoint = viewModel::addDoodlePoint,
                    onDoodleFinished = viewModel::finishDoodlePath,
                    textOverlays = editState.textOverlays,
                    selectedTextId = selectedTextId,
                    onSelectText = viewModel::selectTextOverlay,
                    onMoveText = { id, nx, ny ->
                        viewModel.updateSelectedText { it.copy(normalizedX = nx, normalizedY = ny) }
                    }
                )
            }

            // Lower Creative Controls Tool Panel
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = StudioSurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                tonalElevation = 6.dp
            ) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_content_anim"
                ) { tab ->
                    when (tab) {
                        EditorTab.ADJUST -> {
                            AdjustTabContent(
                                adjustments = editState.adjustments,
                                onBrightnessChange = viewModel::setBrightness,
                                onContrastChange = viewModel::setContrast,
                                onSaturationChange = viewModel::setSaturation,
                                onExposureChange = viewModel::setExposure,
                                onWarmthChange = viewModel::setWarmth,
                                onTintChange = viewModel::setTint,
                                onSharpnessChange = viewModel::setSharpness,
                                onVignetteChange = viewModel::setVignette,
                                onHueChange = viewModel::setHue,
                                onGrainChange = viewModel::setGrain,
                                onAdjustmentCommitted = viewModel::commitAdjustmentChange,
                                onResetAdjustments = viewModel::resetAdjustments
                            )
                        }
                        EditorTab.FILTERS -> {
                            FilterTabContent(
                                activeFilter = editState.activeFilter,
                                filterIntensity = editState.filterIntensity,
                                filterPreviews = filterPreviews,
                                onFilterSelected = viewModel::selectFilter,
                                onIntensityChange = viewModel::setFilterIntensity
                            )
                        }
                        EditorTab.AI_TOOLS -> {
                            AiToolsTabContent(
                                activeEnhance = editState.autoEnhance,
                                activeFilter = editState.activeFilter,
                                onEnhanceSelected = viewModel::selectAutoEnhance,
                                onFilterSelected = viewModel::selectFilter
                            )
                        }
                        EditorTab.TRANSFORM -> {
                            TransformTabContent(
                                transformState = editState.transform,
                                onRotateClockwise = viewModel::rotateClockwise,
                                onFlipHorizontal = viewModel::flipHorizontal,
                                onFlipVertical = viewModel::flipVertical,
                                onStraightenAngleChange = viewModel::setStraightenAngle,
                                onCropRatioSelected = viewModel::setCropRatio
                            )
                        }
                        EditorTab.BLUR_FOCUS -> {
                            BlurFocusTabContent(
                                blurSettings = editState.blur,
                                onBlurModeChange = viewModel::setBlurMode,
                                onBlurIntensityChange = viewModel::setBlurIntensity,
                                onBlurRadiusChange = viewModel::setBlurRadius
                            )
                        }
                        EditorTab.SPLASH -> {
                            SplashTabContent(
                                activeChannel = editState.splash,
                                onChannelSelected = viewModel::selectSplashChannel
                            )
                        }
                        EditorTab.DOODLE -> {
                            DoodleTabContent(
                                activeBrushMode = activeBrushMode,
                                activeBrushColorArgb = activeBrushColor,
                                activeBrushWidth = activeBrushWidth,
                                onBrushModeChange = viewModel::setBrushMode,
                                onBrushColorChange = viewModel::setBrushColor,
                                onBrushWidthChange = viewModel::setBrushWidth,
                                onClearDoodles = viewModel::clearAllDoodles
                            )
                        }
                        EditorTab.TEXT -> {
                            TextTabContent(
                                textOverlays = editState.textOverlays,
                                selectedTextId = selectedTextId,
                                onAddText = viewModel::addTextOverlay,
                                onSelectText = viewModel::selectTextOverlay,
                                onUpdateText = viewModel::updateSelectedText,
                                onDeleteText = viewModel::deleteSelectedText
                            )
                        }
                        EditorTab.EXPORT -> {
                            // Dialog takes over
                            Spacer(modifier = Modifier.height(1.dp))
                        }
                    }
                }
            }
        }
    }

    // Modal Dialogs & Sheets
    if (showSampleSheet) {
        SamplePickerSheet(
            samplePhotos = viewModel.samplePhotos,
            onSelectSample = viewModel::loadSamplePhoto,
            onUriSelected = viewModel::loadFromUri,
            onDismiss = { viewModel.setShowSampleSheet(false) }
        )
    }

    if (showInfoDialog) {
        ImageInfoDialog(
            metadata = imageMetadata,
            onDismiss = { viewModel.setShowInfoDialog(false) }
        )
    }

    if (showExportDialog) {
        ExportDialog(
            metadata = imageMetadata,
            onDismiss = { viewModel.setShowExportDialog(false) },
            onSave = { format, quality, customFilename, subfolder ->
                viewModel.saveToExternalStorage(
                    format = format,
                    quality = quality,
                    customFilename = customFilename,
                    subfolder = subfolder
                )
            },
            onShare = viewModel::shareImage
        )
    }

    savedImageResult?.let { savedInfo ->
        SaveSuccessDialog(
            savedInfo = savedInfo,
            onDismiss = { viewModel.clearSavedImageResult() },
            onOpenInGallery = { viewModel.openSavedImage(savedInfo.uri) },
            onShare = { viewModel.shareSavedImageUri(savedInfo.uri) }
        )
    }
}
