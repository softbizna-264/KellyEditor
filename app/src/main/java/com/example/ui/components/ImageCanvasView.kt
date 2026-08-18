package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BlurMode
import com.example.model.BlurSettings
import com.example.model.DoodlePoint
import com.example.model.EditorTab
import com.example.model.TextOverlay
import com.example.ui.theme.KellyAmber
import com.example.ui.theme.KellyCyan
import com.example.ui.theme.StudioDarkBackground
import com.example.ui.theme.StudioSurfaceDark
import kotlin.math.roundToInt

@Composable
fun ImageCanvasView(
    bitmap: Bitmap?,
    isProcessing: Boolean,
    isComparing: Boolean,
    activeTab: EditorTab,
    blurSettings: BlurSettings,
    onBlurCenterChange: (Float, Float) -> Unit,
    onDoodlePoint: (DoodlePoint) -> Unit,
    onDoodleFinished: () -> Unit,
    textOverlays: List<TextOverlay>,
    selectedTextId: String?,
    onSelectText: (String) -> Unit,
    onMoveText: (String, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDarkBackground)
            .testTag("image_canvas_container"),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(activeTab) {
                        if (activeTab == EditorTab.DOODLE) {
                            detectDragGestures(
                                onDragStart = { startPos ->
                                    val nx = (startPos.x / size.width).coerceIn(0f, 1f)
                                    val ny = (startPos.y / size.height).coerceIn(0f, 1f)
                                    onDoodlePoint(DoodlePoint(nx, ny))
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val nx = (change.position.x / size.width).coerceIn(0f, 1f)
                                    val ny = (change.position.y / size.height).coerceIn(0f, 1f)
                                    onDoodlePoint(DoodlePoint(nx, ny))
                                },
                                onDragEnd = {
                                    onDoodleFinished()
                                },
                                onDragCancel = {
                                    onDoodleFinished()
                                }
                            )
                        } else if (activeTab == EditorTab.BLUR_FOCUS && blurSettings.mode != BlurMode.NONE) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val nx = (change.position.x / size.width).coerceIn(0f, 1f)
                                val ny = (change.position.y / size.height).coerceIn(0f, 1f)
                                onBlurCenterChange(nx, ny)
                            }
                        } else {
                            // Pinch to zoom and pan
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(0.8f, 5f)
                                scale = newScale
                                if (newScale > 1f) {
                                    offset = Offset(
                                        x = offset.x + pan.x,
                                        y = offset.y + pan.y
                                    )
                                } else {
                                    offset = Offset.Zero
                                }
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        // Double tap to reset zoom
                        detectTapGestures(
                            onDoubleTap = {
                                scale = 1f
                                offset = Offset.Zero
                            }
                        )
                    }
            ) {
                val boxWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                val boxHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

                // Render Canvas
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val imgWidth = imageBitmap.width.toFloat()
                    val imgHeight = imageBitmap.height.toFloat()

                    // Compute aspect fit scale
                    val scaleFit = kotlin.math.min(canvasWidth / imgWidth, canvasHeight / imgHeight)
                    val drawWidth = imgWidth * scaleFit * scale
                    val drawHeight = imgHeight * scaleFit * scale
                    val drawLeft = (canvasWidth - drawWidth) / 2f + offset.x
                    val drawTop = (canvasHeight - drawHeight) / 2f + offset.y

                    // Draw image
                    drawImage(
                        image = imageBitmap,
                        dstOffset = androidx.compose.ui.unit.IntOffset(drawLeft.roundToInt(), drawTop.roundToInt()),
                        dstSize = androidx.compose.ui.unit.IntSize(drawWidth.roundToInt(), drawHeight.roundToInt())
                    )

                    // Draw Rule of Thirds Grid if in Transform tab
                    if (activeTab == EditorTab.TRANSFORM) {
                        val stroke = Stroke(width = 1.5.dp.toPx())
                        val gridColor = Color.White.copy(alpha = 0.45f)

                        // Outer border
                        drawRect(
                            color = gridColor,
                            topLeft = Offset(drawLeft, drawTop),
                            size = Size(drawWidth, drawHeight),
                            style = stroke
                        )

                        // Vertical lines
                        drawLine(
                            color = gridColor,
                            start = Offset(drawLeft + drawWidth / 3f, drawTop),
                            end = Offset(drawLeft + drawWidth / 3f, drawTop + drawHeight),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = gridColor,
                            start = Offset(drawLeft + 2f * drawWidth / 3f, drawTop),
                            end = Offset(drawLeft + 2f * drawWidth / 3f, drawTop + drawHeight),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Horizontal lines
                        drawLine(
                            color = gridColor,
                            start = Offset(drawLeft, drawTop + drawHeight / 3f),
                            end = Offset(drawLeft + drawWidth, drawTop + drawHeight / 3f),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = gridColor,
                            start = Offset(drawLeft, drawTop + 2f * drawHeight / 3f),
                            end = Offset(drawLeft + drawWidth, drawTop + 2f * drawHeight / 3f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Draw Blur & Focus visual indicator circle/band
                    if (activeTab == EditorTab.BLUR_FOCUS && blurSettings.mode != BlurMode.NONE) {
                        val focusCx = drawLeft + blurSettings.centerX * drawWidth
                        val focusCy = drawTop + blurSettings.centerY * drawHeight

                        when (blurSettings.mode) {
                            BlurMode.RADIAL_BOKEH -> {
                                val focusRadius = blurSettings.radius * kotlin.math.min(drawWidth, drawHeight)
                                drawCircle(
                                    color = KellyCyan.copy(alpha = 0.8f),
                                    center = Offset(focusCx, focusCy),
                                    radius = focusRadius,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                drawCircle(
                                    color = KellyCyan,
                                    center = Offset(focusCx, focusCy),
                                    radius = 6.dp.toPx()
                                )
                            }
                            BlurMode.LINEAR_TILT_SHIFT -> {
                                val bandH = blurSettings.radius * drawHeight
                                val topY = focusCy - bandH / 2f
                                val botY = focusCy + bandH / 2f

                                drawLine(
                                    color = KellyCyan.copy(alpha = 0.8f),
                                    start = Offset(drawLeft, topY),
                                    end = Offset(drawLeft + drawWidth, topY),
                                    strokeWidth = 2.dp.toPx()
                                )
                                drawLine(
                                    color = KellyCyan.copy(alpha = 0.8f),
                                    start = Offset(drawLeft, botY),
                                    end = Offset(drawLeft + drawWidth, botY),
                                    strokeWidth = 2.dp.toPx()
                                )
                                drawCircle(
                                    color = KellyCyan,
                                    center = Offset(focusCx, focusCy),
                                    radius = 6.dp.toPx()
                                )
                            }
                            else -> {}
                        }
                    }
                }

                // Interactive Draggable Text overlays
                if (activeTab == EditorTab.TEXT) {
                    textOverlays.forEach { overlay ->
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        (overlay.normalizedX * boxWidthPx).roundToInt() - 60,
                                        (overlay.normalizedY * boxHeightPx).roundToInt() - 20
                                    )
                                }
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selectedTextId == overlay.id)
                                        KellyCyan.copy(alpha = 0.25f)
                                    else
                                        Color.Transparent
                                )
                                .padding(6.dp)
                                .pointerInput(overlay.id) {
                                    detectTapGestures {
                                        onSelectText(overlay.id)
                                    }
                                }
                                .pointerInput(overlay.id) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val newX = ((overlay.normalizedX * boxWidthPx + dragAmount.x) / boxWidthPx).coerceIn(0.1f, 0.9f)
                                        val newY = ((overlay.normalizedY * boxHeightPx + dragAmount.y) / boxHeightPx).coerceIn(0.1f, 0.9f)
                                        onMoveText(overlay.id, newX, newY)
                                    }
                                }
                        ) {
                            Text(
                                text = overlay.text,
                                color = Color(overlay.textColorArgb),
                                fontSize = overlay.textSizeSp.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Comparing Original Badge
        AnimatedVisibility(
            visible = isComparing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = KellyAmber.copy(alpha = 0.92f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = "ORIGINAL IMAGE (HOLDING)",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        // Processing Progress Spinner
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = KellyCyan,
                    modifier = Modifier.testTag("processing_indicator")
                )
            }
        }
    }
}
