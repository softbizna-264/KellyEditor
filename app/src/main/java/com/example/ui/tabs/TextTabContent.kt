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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TextOverlay
import com.example.model.TextStyleFont
import com.example.ui.theme.KellyCyan
import com.example.ui.theme.KellyViolet
import com.example.ui.theme.SliderTrackDark
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import kotlin.math.roundToInt

@Composable
fun TextTabContent(
    textOverlays: List<TextOverlay>,
    selectedTextId: String?,
    onAddText: (String) -> Unit,
    onSelectText: (String?) -> Unit,
    onUpdateText: ((TextOverlay) -> TextOverlay) -> Unit,
    onDeleteText: () -> Unit,
    modifier: Modifier = Modifier
) {
    var newTextPrompt by remember { mutableStateOf("") }
    var isAddingNew by remember { mutableStateOf(false) }

    val activeOverlay = textOverlays.firstOrNull { it.id == selectedTextId }

    val textColors = listOf(
        Color.White,
        Color(0xFF00E5FF),
        Color(0xFFFFD600),
        Color(0xFFFF5252),
        Color(0xFF00E676),
        Color(0xFFE040FB),
        Color(0xFF1E1E1E)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        if (isAddingNew) {
            // New Text Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTextPrompt,
                    onValueChange = { newTextPrompt = it },
                    placeholder = { Text("Enter text sticker...", color = StudioTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = StudioTextPrimary,
                        unfocusedTextColor = StudioTextPrimary,
                        focusedBorderColor = KellyCyan,
                        unfocusedBorderColor = StudioBorder,
                        cursorColor = KellyCyan
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("text_input_field")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (newTextPrompt.isNotBlank()) {
                            onAddText(newTextPrompt)
                            newTextPrompt = ""
                            isAddingNew = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KellyCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            }
        } else if (activeOverlay != null) {
            // Selected Text Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "\"${activeOverlay.text}\"",
                    color = KellyCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { isAddingNew = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add More", tint = KellyCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Add New", color = KellyCyan, fontSize = 11.sp)
                    }

                    TextButton(onClick = onDeleteText) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StudioTextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Delete", color = StudioTextSecondary, fontSize = 11.sp)
                    }
                }
            }

            // Font Selector Chips
            val fontScroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(fontScroll)
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TextStyleFont.values().forEach { font ->
                    val isSelected = activeOverlay.fontStyle == font
                    Surface(
                        onClick = { onUpdateText { it.copy(fontStyle = font) } },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) KellyCyan.copy(alpha = 0.2f) else StudioSurfaceCard,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, KellyCyan) else null,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = font.displayName,
                            color = if (isSelected) KellyCyan else StudioTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Text Size Slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Font Size", color = StudioTextPrimary, fontSize = 11.sp)
                Text("${activeOverlay.textSizeSp.roundToInt()} sp", color = KellyCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = activeOverlay.textSizeSp,
                onValueChange = { size -> onUpdateText { it.copy(textSizeSp = size) } },
                valueRange = 16f..72f,
                colors = SliderDefaults.colors(
                    thumbColor = KellyCyan,
                    activeTrackColor = KellyCyan,
                    inactiveTrackColor = SliderTrackDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("text_size_slider")
            )

            // Text Color Swatches & Background Highlight Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Color swatches
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    textColors.forEach { color ->
                        val isSelected = activeOverlay.textColorArgb == color.toArgb()
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.dp else 0.5.dp,
                                    color = if (isSelected) KellyCyan else StudioBorder,
                                    shape = CircleShape
                                )
                                .clickable { onUpdateText { it.copy(textColorArgb = color.toArgb()) } }
                        )
                    }
                }

                // Background highlight chip toggle
                val hasBg = activeOverlay.backgroundColorArgb != Color.Transparent.toArgb()
                Surface(
                    onClick = {
                        val newBg = if (hasBg) Color.Transparent.toArgb() else Color(0xCC000000).toArgb()
                        onUpdateText { it.copy(backgroundColorArgb = newBg) }
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = if (hasBg) KellyCyan.copy(alpha = 0.2f) else StudioSurfaceCard,
                    border = if (hasBg) androidx.compose.foundation.BorderStroke(1.dp, KellyCyan) else null,
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = if (hasBg) "Highlight: ON" else "Highlight: OFF",
                        color = if (hasBg) KellyCyan else StudioTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        } else {
            // Empty state -> Add Text CTA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { isAddingNew = true },
                    colors = ButtonDefaults.buttonColors(containerColor = KellyCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_text_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Text", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Text Sticker", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
