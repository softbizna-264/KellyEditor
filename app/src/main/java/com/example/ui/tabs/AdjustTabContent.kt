package com.example.ui.tabs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Details
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Vignette
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Adjustments
import com.example.ui.theme.KellyCyan
import com.example.ui.theme.SliderTrackDark
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import kotlin.math.roundToInt

enum class AdjustCategory(val title: String, val icon: ImageVector) {
    BRIGHTNESS("Brightness", Icons.Default.Brightness6),
    CONTRAST("Contrast", Icons.Default.Contrast),
    SATURATION("Saturation", Icons.Default.InvertColors),
    EXPOSURE("Exposure", Icons.Default.Exposure),
    WARMTH("Warmth", Icons.Default.WbSunny),
    TINT("Tint", Icons.Default.Thermostat),
    SHARPNESS("Sharpness", Icons.Default.Details),
    VIGNETTE("Vignette", Icons.Default.Vignette),
    HUE("Hue", Icons.Default.InvertColors),
    GRAIN("Film Grain", Icons.Default.Grain)
}

@Composable
fun AdjustTabContent(
    adjustments: Adjustments,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onExposureChange: (Float) -> Unit,
    onWarmthChange: (Float) -> Unit,
    onTintChange: (Float) -> Unit,
    onSharpnessChange: (Float) -> Unit,
    onVignetteChange: (Float) -> Unit,
    onHueChange: (Float) -> Unit,
    onGrainChange: (Float) -> Unit,
    onAdjustmentCommitted: () -> Unit,
    onResetAdjustments: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(AdjustCategory.BRIGHTNESS) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Active slider section with numeric readout & quick reset
        val (currentValue, valueRange, onValueChange) = when (selectedCategory) {
            AdjustCategory.BRIGHTNESS -> Triple(adjustments.brightness, -100f..100f, onBrightnessChange)
            AdjustCategory.CONTRAST -> Triple(adjustments.contrast, -100f..100f, onContrastChange)
            AdjustCategory.SATURATION -> Triple(adjustments.saturation, -100f..100f, onSaturationChange)
            AdjustCategory.EXPOSURE -> Triple(adjustments.exposure, -100f..100f, onExposureChange)
            AdjustCategory.WARMTH -> Triple(adjustments.warmth, -100f..100f, onWarmthChange)
            AdjustCategory.TINT -> Triple(adjustments.tint, -100f..100f, onTintChange)
            AdjustCategory.SHARPNESS -> Triple(adjustments.sharpness, 0f..100f, onSharpnessChange)
            AdjustCategory.VIGNETTE -> Triple(adjustments.vignette, 0f..100f, onVignetteChange)
            AdjustCategory.HUE -> Triple(adjustments.hue, -180f..180f, onHueChange)
            AdjustCategory.GRAIN -> Triple(adjustments.grain, 0f..100f, onGrainChange)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = selectedCategory.icon,
                    contentDescription = selectedCategory.title,
                    tint = KellyCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedCategory.title,
                    color = StudioTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = StudioSurfaceCard
                ) {
                    Text(
                        text = if (currentValue > 0) "+${currentValue.roundToInt()}" else "${currentValue.roundToInt()}",
                        color = if (currentValue != 0f) KellyCyan else StudioTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            if (!adjustments.isDefault) {
                TextButton(
                    onClick = onResetAdjustments,
                    modifier = Modifier.testTag("reset_adjustments_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset",
                        tint = StudioTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reset All",
                        color = StudioTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Precision Slider
        Slider(
            value = currentValue,
            onValueChange = onValueChange,
            onValueChangeFinished = onAdjustmentCommitted,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = KellyCyan,
                activeTrackColor = KellyCyan,
                inactiveTrackColor = SliderTrackDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("adjust_slider_${selectedCategory.name.lowercase()}")
        )

        // Horizontal Category Chips
        val chipScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(chipScrollState)
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdjustCategory.values().forEach { category ->
                val isSelected = selectedCategory == category
                val hasModifiedValue = when (category) {
                    AdjustCategory.BRIGHTNESS -> adjustments.brightness != 0f
                    AdjustCategory.CONTRAST -> adjustments.contrast != 0f
                    AdjustCategory.SATURATION -> adjustments.saturation != 0f
                    AdjustCategory.EXPOSURE -> adjustments.exposure != 0f
                    AdjustCategory.WARMTH -> adjustments.warmth != 0f
                    AdjustCategory.TINT -> adjustments.tint != 0f
                    AdjustCategory.SHARPNESS -> adjustments.sharpness != 0f
                    AdjustCategory.VIGNETTE -> adjustments.vignette != 0f
                    AdjustCategory.HUE -> adjustments.hue != 0f
                    AdjustCategory.GRAIN -> adjustments.grain != 0f
                }

                Surface(
                    onClick = { selectedCategory = category },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) KellyCyan.copy(alpha = 0.2f) else StudioSurfaceCard,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, KellyCyan) else null,
                    modifier = Modifier.height(38.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = category.title,
                            tint = if (isSelected) KellyCyan else if (hasModifiedValue) KellyCyan else StudioTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = category.title,
                            color = if (isSelected) KellyCyan else StudioTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (hasModifiedValue && !isSelected) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = KellyCyan,
                                modifier = Modifier.size(5.dp)
                            ) {}
                        }
                    }
                }
            }
        }
    }
}
