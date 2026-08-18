package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KellyCyan
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioDarkBackground
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary

@Composable
fun EditorTopBar(
    canUndo: Boolean,
    canRedo: Boolean,
    isComparing: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCompareChange: (Boolean) -> Unit,
    onOpenSamples: () -> Unit,
    onOpenInfo: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioDarkBackground),
        color = StudioDarkBackground,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand Logo & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("brand_header")
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(KellyCyan.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Kelly Editor Logo",
                        tint = KellyCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Kelly Editor",
                    color = StudioTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // Central Actions: Undo, Redo, Compare (Hold to view original)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Undo Button
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("undo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) StudioTextPrimary else StudioTextMuted
                    )
                }

                // Redo Button
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("redo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) StudioTextPrimary else StudioTextMuted
                    )
                }

                // Hold-to-Compare Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isComparing) KellyCyan.copy(alpha = 0.25f) else StudioSurfaceCard)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    onCompareChange(true)
                                    tryAwaitRelease()
                                    onCompareChange(false)
                                }
                            )
                        }
                        .testTag("compare_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Compare,
                        contentDescription = "Hold to Compare Original",
                        tint = if (isComparing) KellyCyan else StudioTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Right Actions: Library, Info, Save
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Samples / Gallery Picker
                IconButton(
                    onClick = onOpenSamples,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(StudioSurfaceCard)
                        .testTag("open_samples_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = "Choose Image",
                        tint = StudioTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Info Inspector
                IconButton(
                    onClick = onOpenInfo,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(StudioSurfaceCard)
                        .testTag("info_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Image Details",
                        tint = StudioTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Save Action
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KellyCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("save_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Export / Save",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Save",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
