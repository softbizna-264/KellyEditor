package com.example.ui.tabs

import androidx.compose.foundation.background
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SplashChannel
import com.example.ui.theme.KellyCyan
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary

@Composable
fun SplashTabContent(
    activeChannel: SplashChannel,
    onChannelSelected: (SplashChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Selective Color Splash (Isolates one color, converts rest to B&W)",
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SplashChannel.values().forEach { channel ->
                val isSelected = activeChannel == channel
                val swatchColor = when (channel) {
                    SplashChannel.OFF -> Color.Gray
                    SplashChannel.RED -> Color(0xFFE53935)
                    SplashChannel.GREEN -> Color(0xFF43A047)
                    SplashChannel.BLUE -> Color(0xFF1E88E5)
                    SplashChannel.YELLOW -> Color(0xFFFDD835)
                    SplashChannel.CYAN -> Color(0xFF00ACC1)
                    SplashChannel.MAGENTA -> Color(0xFFD81B60)
                }

                Surface(
                    onClick = { onChannelSelected(channel) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) KellyCyan.copy(alpha = 0.2f) else StudioSurfaceCard,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, KellyCyan) else null,
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("splash_${channel.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = channel.displayName,
                            color = if (isSelected) KellyCyan else StudioTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
