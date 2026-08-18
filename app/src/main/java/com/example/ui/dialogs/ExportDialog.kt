package com.example.ui.dialogs

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.engine.BitmapUtils
import com.example.model.ImageMetadata
import com.example.ui.theme.KellyCyan
import com.example.ui.theme.KellyGreen
import com.example.ui.theme.KellyOrange
import com.example.ui.theme.SliderTrackDark
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioDarkBackground
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.StudioSurfaceDark
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ExportDialog(
    metadata: ImageMetadata?,
    onDismiss: () -> Unit,
    onSave: (format: Bitmap.CompressFormat, quality: Int, customFilename: String?, subfolder: String) -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val initialTimestamp = remember { SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) }
    var filename by remember { mutableStateOf("KellyEditor_$initialTimestamp") }
    var selectedFormat by remember { mutableStateOf(Bitmap.CompressFormat.JPEG) }
    var quality by remember { mutableFloatStateOf(95f) }
    var selectedSubfolder by remember { mutableStateOf("Pictures/KellyEditor") }

    var showPermissionDeniedWarning by remember { mutableStateOf(false) }

    // Helper to perform save after permission verification
    fun executeSave() {
        val cleanName = filename.trim().ifEmpty { "KellyEditor_$initialTimestamp" }
        onSave(selectedFormat, quality.roundToInt(), cleanName, selectedSubfolder)
    }

    // Permission launcher for Android 9 and lower (API <= 28)
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showPermissionDeniedWarning = false
            executeSave()
        } else {
            showPermissionDeniedWarning = true
        }
    }

    // Handles Save button click with proper runtime permission check
    fun onSaveClicked() {
        focusManager.clearFocus()
        if (BitmapUtils.isStoragePermissionRequired()) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
                executeSave()
            } else {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else {
            // Android 10+ (Q+): Scoped MediaStore writes to external storage directly
            executeSave()
        }
    }

    val extension = when (selectedFormat) {
        Bitmap.CompressFormat.PNG -> "png"
        Bitmap.CompressFormat.WEBP -> "webp"
        else -> "jpg"
    }
    val fullPreviewPath = "/storage/emulated/0/$selectedSubfolder/${filename.trim().ifEmpty { "KellyEditor_$initialTimestamp" }}.$extension"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceDark),
            border = BorderStroke(1.dp, StudioBorder),
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("export_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(KellyCyan.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = KellyCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Save to Device",
                                color = StudioTextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "External Storage & Gallery Export",
                                color = StudioTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = StudioTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // File Name Input Field
                Text(
                    text = "File Name",
                    color = StudioTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = filename,
                    onValueChange = { filename = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    trailingIcon = {
                        Text(
                            text = ".$extension",
                            color = KellyCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = StudioTextPrimary,
                        unfocusedTextColor = StudioTextPrimary,
                        focusedBorderColor = KellyCyan,
                        unfocusedBorderColor = StudioBorder,
                        focusedContainerColor = StudioSurfaceCard,
                        unfocusedContainerColor = StudioSurfaceCard
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_filename_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Destination Folder Selector
                Text(
                    text = "External Storage Location",
                    color = StudioTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val folders = listOf(
                        "Pictures/KellyEditor" to "Pictures",
                        "DCIM/KellyEditor" to "DCIM / Camera",
                        "Download/KellyEditor" to "Downloads"
                    )
                    folders.forEach { (folderPath, label) ->
                        val isSelected = selectedSubfolder == folderPath
                        Surface(
                            onClick = { selectedSubfolder = folderPath },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) KellyCyan.copy(alpha = 0.2f) else StudioSurfaceCard,
                            border = if (isSelected) BorderStroke(1.dp, KellyCyan) else BorderStroke(1.dp, StudioBorder.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = if (isSelected) KellyCyan else StudioTextMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = label,
                                    color = if (isSelected) KellyCyan else StudioTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Format Selector (JPEG, PNG, WEBP)
                Text(
                    text = "Compression Format",
                    color = StudioTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val formats = listOf(
                        Bitmap.CompressFormat.JPEG to "JPEG",
                        Bitmap.CompressFormat.PNG to "PNG",
                        Bitmap.CompressFormat.WEBP to "WEBP"
                    )

                    formats.forEach { (fmt, label) ->
                        val isSelected = selectedFormat == fmt
                        Surface(
                            onClick = { selectedFormat = fmt },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) KellyCyan.copy(alpha = 0.2f) else StudioSurfaceCard,
                            border = if (isSelected) BorderStroke(1.dp, KellyCyan) else null,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    color = if (isSelected) KellyCyan else StudioTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Quality Slider (for JPEG & WEBP)
                if (selectedFormat != Bitmap.CompressFormat.PNG) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Compression Quality",
                            color = StudioTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${quality.roundToInt()}%",
                            color = KellyCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = quality,
                        onValueChange = { quality = it },
                        valueRange = 50f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = KellyCyan,
                            activeTrackColor = KellyCyan,
                            inactiveTrackColor = SliderTrackDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Destination Path Preview Box
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StudioDarkBackground,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = KellyCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Target Destination Path", color = StudioTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = fullPreviewPath,
                            color = StudioTextPrimary,
                            fontSize = 11.sp,
                            maxLines = 2
                        )
                    }
                }

                // Permission Denied Warning Banner (if user declined on pre-Q device)
                if (showPermissionDeniedWarning) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = KellyOrange.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, KellyOrange),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Storage Permission Required",
                                color = KellyOrange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Kelly Editor needs storage permission to write photos to external storage on this Android version. Please grant permission in App Settings.",
                                color = StudioTextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = KellyOrange, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Open App Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions: Save to External Storage & Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onShare,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, KellyCyan),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = KellyCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", color = KellyCyan, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onSaveClicked() },
                        colors = ButtonDefaults.buttonColors(containerColor = KellyCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Save to External Storage",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save File", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
