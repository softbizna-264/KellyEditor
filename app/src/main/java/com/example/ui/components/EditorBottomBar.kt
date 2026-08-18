package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EditorTab
import com.example.ui.theme.KellyCyan
import com.example.ui.theme.StudioDarkBackground
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary

@Composable
fun EditorBottomBar(
    activeTab: EditorTab,
    onTabSelected: (EditorTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = StudioDarkBackground,
        tonalElevation = 8.dp
    ) {
        val scrollState = rememberScrollState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorTab.values().forEach { tab ->
                val isSelected = activeTab == tab
                val icon = getTabIcon(tab)

                Surface(
                    onClick = { onTabSelected(tab) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) KellyCyan.copy(alpha = 0.15f) else Color.Transparent,
                    modifier = Modifier
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("tab_${tab.name.lowercase()}")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) KellyCyan else StudioTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = tab.label,
                            color = if (isSelected) KellyCyan else StudioTextMuted,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private fun getTabIcon(tab: EditorTab): ImageVector {
    return when (tab) {
        EditorTab.ADJUST -> Icons.Default.Tune
        EditorTab.FILTERS -> Icons.Default.Filter
        EditorTab.AI_TOOLS -> Icons.Default.AutoAwesome
        EditorTab.TRANSFORM -> Icons.Default.CropRotate
        EditorTab.BLUR_FOCUS -> Icons.Default.BlurOn
        EditorTab.SPLASH -> Icons.Default.ColorLens
        EditorTab.DOODLE -> Icons.Default.Brush
        EditorTab.TEXT -> Icons.Default.TextFields
        EditorTab.EXPORT -> Icons.Default.Download
    }
}
