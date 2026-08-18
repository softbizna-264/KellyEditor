package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.engine.SavedImageInfo
import com.example.ui.theme.KellyCyan
import com.example.ui.theme.KellyGreen
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioDarkBackground
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.StudioSurfaceDark
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary

@Composable
fun SaveSuccessDialog(
    savedInfo: SavedImageInfo,
    onDismiss: () -> Unit,
    onOpenInGallery: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceDark),
            border = BorderStroke(1.dp, KellyGreen.copy(alpha = 0.5f)),
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("save_success_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success Badge Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(KellyGreen.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Saved",
                        tint = KellyGreen,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Saved to External Storage!",
                    color = StudioTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Your image is ready in your device gallery & file manager.",
                    color = StudioTextSecondary,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // File Details Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StudioSurfaceCard,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Filename", color = StudioTextMuted, fontSize = 11.sp)
                            Text(
                                text = savedInfo.filename,
                                color = KellyCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Storage Path", color = StudioTextMuted, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(StudioDarkBackground, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = savedInfo.filePath,
                                color = StudioTextPrimary,
                                fontSize = 10.sp,
                                maxLines = 2,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Saved File Path", savedInfo.filePath)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Path copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Path",
                                    tint = StudioTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons: Open in Gallery, Share, Done
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onShare,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StudioBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("save_success_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = KellyCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", color = KellyCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onOpenInGallery,
                        colors = ButtonDefaults.buttonColors(containerColor = KellyGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                            .testTag("save_success_view_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open in Gallery",
                            tint = Color.Black,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Image", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceCard, contentColor = StudioTextPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("save_success_done_button")
                ) {
                    Text("Done", color = StudioTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
