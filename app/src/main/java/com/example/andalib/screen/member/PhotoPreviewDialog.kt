package com.example.andalib.screen.member

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Dialog untuk preview foto sebelum upload
 */
@Composable
fun PhotoPreviewDialog(
    imageUri: Uri,
    isUploading: Boolean = false,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isUploading) onCancel() },
        title = {
            Text("Preview Foto")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Image preview
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Preview foto",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Loading indicator
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Mengupload foto...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isUploading
            ) {
                Text("Upload")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                enabled = !isUploading
            ) {
                Text("Batal")
            }
        }
    )
}
