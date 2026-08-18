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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.WbAuto
import androidx.compose.material3.Icon
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
import com.example.model.AutoEnhanceType
import com.example.model.ImageFilter
import com.example.ui.theme.KellyCyan
import com.example.ui.theme.KellyViolet
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary

@Composable
fun AiToolsTabContent(
    activeEnhance: AutoEnhanceType,
    activeFilter: ImageFilter,
    onEnhanceSelected: (AutoEnhanceType) -> Unit,
    onFilterSelected: (ImageFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Offline Neural & Mathematical Enhancements",
            color = StudioTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Auto Dynamic Range
            AiToolCard(
                title = "Dynamic Range",
                subtitle = "Adaptive Histogram",
                icon = Icons.Default.Flare,
                isSelected = activeEnhance == AutoEnhanceType.AUTO_DYNAMIC_RANGE,
                onClick = {
                    if (activeEnhance == AutoEnhanceType.AUTO_DYNAMIC_RANGE)
                        onEnhanceSelected(AutoEnhanceType.NONE)
                    else
                        onEnhanceSelected(AutoEnhanceType.AUTO_DYNAMIC_RANGE)
                },
                tag = "ai_tool_dynamic_range"
            )

            // Auto White Balance
            AiToolCard(
                title = "White Balance",
                subtitle = "Gray World Cast Fix",
                icon = Icons.Default.WbAuto,
                isSelected = activeEnhance == AutoEnhanceType.AUTO_WHITE_BALANCE,
                onClick = {
                    if (activeEnhance == AutoEnhanceType.AUTO_WHITE_BALANCE)
                        onEnhanceSelected(AutoEnhanceType.NONE)
                    else
                        onEnhanceSelected(AutoEnhanceType.AUTO_WHITE_BALANCE)
                },
                tag = "ai_tool_white_balance"
            )

            // Portrait Skin Glow
            AiToolCard(
                title = "Portrait Glow",
                subtitle = "Bilateral Skin Glow",
                icon = Icons.Default.Face,
                isSelected = activeEnhance == AutoEnhanceType.PORTRAIT_SKIN_GLOW,
                onClick = {
                    if (activeEnhance == AutoEnhanceType.PORTRAIT_SKIN_GLOW)
                        onEnhanceSelected(AutoEnhanceType.NONE)
                    else
                        onEnhanceSelected(AutoEnhanceType.PORTRAIT_SKIN_GLOW)
                },
                tag = "ai_tool_portrait_glow"
            )

            // De-Haze & Clarity
            AiToolCard(
                title = "De-Haze",
                subtitle = "Atmospheric Clarity",
                icon = Icons.Default.Landscape,
                isSelected = activeEnhance == AutoEnhanceType.DEHAZE_CLARITY,
                onClick = {
                    if (activeEnhance == AutoEnhanceType.DEHAZE_CLARITY)
                        onEnhanceSelected(AutoEnhanceType.NONE)
                    else
                        onEnhanceSelected(AutoEnhanceType.DEHAZE_CLARITY)
                },
                tag = "ai_tool_dehaze"
            )

            // Pencil Sketch
            AiToolCard(
                title = "Pencil Sketch",
                subtitle = "Sobel Edge Kernel",
                icon = Icons.Default.AutoFixHigh,
                isSelected = activeFilter == ImageFilter.SKETCH_EDGE,
                onClick = {
                    if (activeFilter == ImageFilter.SKETCH_EDGE)
                        onFilterSelected(ImageFilter.ORIGINAL)
                    else
                        onFilterSelected(ImageFilter.SKETCH_EDGE)
                },
                tag = "ai_tool_sketch"
            )

            // 3D Emboss
            AiToolCard(
                title = "3D Emboss",
                subtitle = "Directional Relief",
                icon = Icons.Default.Layers,
                isSelected = activeFilter == ImageFilter.EMBOSS_3D,
                onClick = {
                    if (activeFilter == ImageFilter.EMBOSS_3D)
                        onFilterSelected(ImageFilter.ORIGINAL)
                    else
                        onFilterSelected(ImageFilter.EMBOSS_3D)
                },
                tag = "ai_tool_emboss"
            )

            // Pixelate 8-Bit
            AiToolCard(
                title = "Pixelate 8-Bit",
                subtitle = "Retro Mosaic Blocks",
                icon = Icons.Default.GridOn,
                isSelected = activeFilter == ImageFilter.PIXELATE_8BIT,
                onClick = {
                    if (activeFilter == ImageFilter.PIXELATE_8BIT)
                        onFilterSelected(ImageFilter.ORIGINAL)
                    else
                        onFilterSelected(ImageFilter.PIXELATE_8BIT)
                },
                tag = "ai_tool_pixelate"
            )
        }
    }
}

@Composable
private fun AiToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) KellyViolet.copy(alpha = 0.22f) else StudioSurfaceCard,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, KellyViolet) else null,
        modifier = Modifier
            .width(132.dp)
            .height(84.dp)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) KellyViolet else KellyCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = if (isSelected) KellyViolet else StudioTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = subtitle,
                color = StudioTextMuted,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}
