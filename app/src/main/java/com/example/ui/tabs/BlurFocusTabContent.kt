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
import androidx.compose.material.icons.filled.BlurCircular
import androidx.compose.material.icons.filled.BlurLinear
import androidx.compose.material.icons.filled.BlurOff
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BlurMode
import com.example.model.BlurSettings
import com.example.ui.theme.KellyCyan
import com.example.ui.theme.SliderTrackDark
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import kotlin.math.roundToInt

@Composable
fun BlurFocusTabContent(
    blurSettings: BlurSettings,
    onBlurModeChange: (BlurMode) -> Unit,
    onBlurIntensityChange: (Float) -> Unit,
    onBlurRadiusChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Blur Mode Selector Chips
        val modeScroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(modeScroll)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BlurMode.values().forEach { mode ->
                val isSelected = blurSettings.mode == mode
                val icon = when (mode) {
                    BlurMode.NONE -> Icons.Default.BlurOff
                    BlurMode.RADIAL_BOKEH -> Icons.Default.BlurCircular
                    BlurMode.LINEAR_TILT_SHIFT -> Icons.Default.BlurLinear
                    BlurMode.FULL_GAUSSIAN -> Icons.Default.BlurOn
                }

                Surface(
                    onClick = { onBlurModeChange(mode) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) KellyCyan.copy(alpha = 0.2f) else StudioSurfaceCard,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, KellyCyan) else null,
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("blur_mode_${mode.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = mode.displayName,
                            tint = if (isSelected) KellyCyan else StudioTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = mode.displayName,
                            color = if (isSelected) KellyCyan else StudioTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        if (blurSettings.mode != BlurMode.NONE) {
            Spacer(modifier = Modifier.height(6.dp))

            // Blur Intensity Slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Blur Intensity", color = StudioTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("${blurSettings.intensity.roundToInt()}%", color = KellyCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = blurSettings.intensity,
                onValueChange = onBlurIntensityChange,
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = KellyCyan,
                    activeTrackColor = KellyCyan,
                    inactiveTrackColor = SliderTrackDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("blur_intensity_slider")
            )

            // Focus Zone Size (for Bokeh & Tilt-Shift)
            if (blurSettings.mode == BlurMode.RADIAL_BOKEH || blurSettings.mode == BlurMode.LINEAR_TILT_SHIFT) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Focus Zone Size", color = StudioTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${(blurSettings.radius * 100).roundToInt()}%", color = KellyCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = blurSettings.radius,
                    onValueChange = onBlurRadiusChange,
                    valueRange = 0.1f..0.8f,
                    colors = SliderDefaults.colors(
                        thumbColor = KellyCyan,
                        activeTrackColor = KellyCyan,
                        inactiveTrackColor = SliderTrackDark
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("blur_radius_slider")
                )
            }
        }
    }
}
