package com.example.ui.tabs

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ImageFilter
import com.example.ui.theme.KellyCyan
import com.example.ui.theme.SliderTrackDark
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import kotlin.math.roundToInt

@Composable
fun FilterTabContent(
    activeFilter: ImageFilter,
    filterIntensity: Float,
    filterPreviews: Map<ImageFilter, Bitmap>,
    onFilterSelected: (ImageFilter) -> Unit,
    onIntensityChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Intensity Slider (if filter is not ORIGINAL)
        if (activeFilter != ImageFilter.ORIGINAL) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${activeFilter.displayName} Intensity",
                    color = StudioTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${filterIntensity.roundToInt()}%",
                    color = KellyCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = filterIntensity,
                onValueChange = onIntensityChange,
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = KellyCyan,
                    activeTrackColor = KellyCyan,
                    inactiveTrackColor = SliderTrackDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("filter_intensity_slider")
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Horizontal Filter Thumbnails
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ImageFilter.values().forEach { filter ->
                val isSelected = activeFilter == filter
                val previewBitmap = filterPreviews[filter]

                Surface(
                    onClick = { onFilterSelected(filter) },
                    shape = RoundedCornerShape(12.dp),
                    color = StudioSurfaceCard,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, KellyCyan) else null,
                    modifier = Modifier
                        .width(72.dp)
                        .testTag("filter_${filter.name.lowercase()}")
                ) {
                    Column(
                        modifier = Modifier.padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            if (previewBitmap != null) {
                                Image(
                                    bitmap = previewBitmap.asImageBitmap(),
                                    contentDescription = filter.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )
                            } else {
                                Text(
                                    text = filter.displayName.take(2).uppercase(),
                                    color = StudioTextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = filter.displayName,
                            color = if (isSelected) KellyCyan else StudioTextPrimary,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
