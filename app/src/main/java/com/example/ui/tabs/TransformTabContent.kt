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
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CropRatio
import com.example.model.TransformState
import com.example.ui.theme.KellyCyan
import com.example.ui.theme.SliderTrackDark
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import kotlin.math.roundToInt

@Composable
fun TransformTabContent(
    transformState: TransformState,
    onRotateClockwise: () -> Unit,
    onFlipHorizontal: () -> Unit,
    onFlipVertical: () -> Unit,
    onStraightenAngleChange: (Float) -> Unit,
    onCropRatioSelected: (CropRatio) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Transform action buttons: Rotate 90, Flip H, Flip V
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onRotateClockwise,
                shape = RoundedCornerShape(10.dp),
                color = StudioSurfaceCard,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("rotate_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Rotate90DegreesCw,
                        contentDescription = "Rotate 90°",
                        tint = KellyCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Rotate 90°", color = StudioTextPrimary, fontSize = 12.sp)
                }
            }

            Surface(
                onClick = onFlipHorizontal,
                shape = RoundedCornerShape(10.dp),
                color = if (transformState.flipHorizontal) KellyCyan.copy(alpha = 0.2f) else StudioSurfaceCard,
                border = if (transformState.flipHorizontal) androidx.compose.foundation.BorderStroke(1.dp, KellyCyan) else null,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("flip_h_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Flip,
                        contentDescription = "Flip Horizontal",
                        tint = if (transformState.flipHorizontal) KellyCyan else StudioTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Flip H", color = StudioTextPrimary, fontSize = 12.sp)
                }
            }

            Surface(
                onClick = onFlipVertical,
                shape = RoundedCornerShape(10.dp),
                color = if (transformState.flipVertical) KellyCyan.copy(alpha = 0.2f) else StudioSurfaceCard,
                border = if (transformState.flipVertical) androidx.compose.foundation.BorderStroke(1.dp, KellyCyan) else null,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("flip_v_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenRotation,
                        contentDescription = "Flip Vertical",
                        tint = if (transformState.flipVertical) KellyCyan else StudioTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Flip V", color = StudioTextPrimary, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Straighten Angle Slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Straighten Angle",
                color = StudioTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${transformState.straightenAngle.roundToInt()}°",
                color = KellyCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = transformState.straightenAngle,
            onValueChange = onStraightenAngleChange,
            valueRange = -45f..45f,
            colors = SliderDefaults.colors(
                thumbColor = KellyCyan,
                activeTrackColor = KellyCyan,
                inactiveTrackColor = SliderTrackDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("straighten_slider")
        )

        // Aspect Ratio presets chips
        val aspectScroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(aspectScroll)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CropRatio.values().forEach { ratio ->
                val isSelected = transformState.selectedCropRatio == ratio
                Surface(
                    onClick = { onCropRatioSelected(ratio) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) KellyCyan.copy(alpha = 0.2f) else StudioSurfaceCard,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, KellyCyan) else null,
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("crop_${ratio.name.lowercase()}")
                ) {
                    Text(
                        text = ratio.displayName,
                        color = if (isSelected) KellyCyan else StudioTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
