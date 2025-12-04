package com.reminder.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun SimpleFilePicker(
    attachedFiles: List<Pair<String, String>>, // List of (fileName, fileUri)
    onFilesSelected: (List<Pair<String, String>>) -> Unit,
    onFileClick: (String) -> Unit = {},
    onFileRemove: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    
    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.let { data ->
            val uris = mutableListOf<Uri>()
            
            // Handle multiple files
            data.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    clipData.getItemAt(i)?.uri?.let { uri ->
                        uris.add(uri)
                    }
                }
            } ?: run {
                // Handle single file
                data.data?.let { uri ->
                    uris.add(uri)
                }
            }
            
            // Convert URIs to file name and URI string pairs
            val newFiles = uris.mapNotNull { uri ->
                val fileName = getFileName(uri, context)
                Pair(fileName, uri.toString())
            }
            
            if (newFiles.isNotEmpty()) {
                onFilesSelected(attachedFiles + newFiles)
            }
        }
    }
    
    fun launchFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        filePickerLauncher.launch(Intent.createChooser(intent, "Select files"))
    }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // File picker button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Attachments",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            
            Button(
                onClick = { launchFilePicker() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.AttachFile,
                    contentDescription = "Attach Files",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Files")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Attached files list
        if (attachedFiles.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(attachedFiles) { (fileName, fileUri) ->
                        FileItem(
                            fileName = fileName,
                            fileUri = fileUri,
                            onClick = { onFileClick(fileUri) },
                            onRemove = { 
                                val index = attachedFiles.indexOf(Pair(fileName, fileUri))
                                if (index != -1) {
                                    onFileRemove(index)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileItem(
    fileName: String,
    fileUri: String,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File info
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📎",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                }
            }
            
            // Action buttons
            Row {
                // View button
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.OpenInBrowser,
                        contentDescription = "View File",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                // Remove button
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove File",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun getFileName(uri: Uri, context: android.content.Context): String {
    var fileName = "unknown_file"
    
    // Try to get display name
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        fileName = cursor.getString(nameIndex) ?: "unknown_file"
    }
    
    return fileName
}
