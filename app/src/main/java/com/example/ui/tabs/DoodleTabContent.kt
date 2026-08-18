package com.example.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrushMode
import com.example.ui.theme.KellyCyan
import com.example.ui.theme.SliderTrackDark
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import kotlin.math.roundToInt

@Composable
fun DoodleTabContent(
    activeBrushMode: BrushMode,
    activeBrushColorArgb: Int,
    activeBrushWidth: Float,
    onBrushModeChange: (BrushMode) -> Unit,
    onBrushColorChange: (Int) -> Unit,
    onBrushWidthChange: (Float) -> Unit,
    onClearDoodles: () -> Unit,
    modifier: Modifier = Modifier
) {
    val paletteColors = listOf(
        Color(0xFF00E5FF), // Cyan
        Color(0xFFFF5252), // Coral Red
        Color(0xFFFFD600), // Amber Yellow
        Color(0xFF00E676), // Neon Green
        Color(0xFF7C4DFF), // Electric Purple
        Color(0xFFFF4081), // Hot Pink
        Color(0xFFFF9100), // Orange
        Color(0xFFFFFFFF), // White
        Color(0xFF1E1E1E)  // Dark
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Brush Mode & Clear Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val modeScroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(modeScroll),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BrushMode.values().forEach { mode ->
                    val isSelected = activeBrushMode == mode
                    val icon = when (mode) {
                        BrushMode.NORMAL -> Icons.Default.Brush
                        BrushMode.NEON_GLOW -> Icons.Default.AutoAwesome
                        BrushMode.HIGHLIGHTER -> Icons.Default.Create
                        BrushMode.ERASER -> Icons.Default.CleaningServices
                    }

                    Surface(
                        onClick = { onBrushModeChange(mode) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) KellyCyan.copy(alpha = 0.2f) else StudioSurfaceCard,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, KellyCyan) else null,
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = mode.displayName,
                                tint = if (isSelected) KellyCyan else StudioTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = mode.displayName,
                                color = if (isSelected) KellyCyan else StudioTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            TextButton(
                onClick = onClearDoodles,
                modifier = Modifier.testTag("clear_doodles_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear",
                    tint = StudioTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text("Clear", color = StudioTextSecondary, fontSize = 11.sp)
            }
        }

        // Brush Width Slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Brush Stroke Size", color = StudioTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("${activeBrushWidth.roundToInt()} px", color = KellyCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = activeBrushWidth,
            onValueChange = onBrushWidthChange,
            valueRange = 4f..48f,
            colors = SliderDefaults.colors(
                thumbColor = KellyCyan,
                activeTrackColor = KellyCyan,
                inactiveTrackColor = SliderTrackDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("brush_width_slider")
        )

        // Color Swatches (if not eraser)
        if (activeBrushMode != BrushMode.ERASER) {
            val colorScroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(colorScroll)
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                paletteColors.forEach { color ->
                    val isSelected = activeBrushColorArgb == color.toArgb()
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onBrushColorChange(color.toArgb()) }
                    )
                }
            }
        }
    }
}
