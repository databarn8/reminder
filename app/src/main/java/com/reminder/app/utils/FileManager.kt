package com.reminder.app.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class FileManager(private val context: Context) {
    
    // Current file state
    val currentFileName = mutableStateOf("")
    val currentFileContent = mutableStateOf("")
    val currentFileUri = mutableStateOf<Uri?>(null)
    val isFileModified = mutableStateOf(false)
    
    // Load file content from URI
    fun loadFileFromUri(uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val content = reader.readText()
                    currentFileContent.value = content
                    currentFileUri.value = uri
                    currentFileName.value = getFileNameFromUri(uri)
                    isFileModified.value = false
                    Log.d("FileManager", "Successfully loaded file: ${currentFileName.value}")
                    true
                }
            } ?: false
        } catch (e: Exception) {
            Log.e("FileManager", "Error loading file from URI: ${e.message}")
            false
        }
    }
    
    // Get file name from URI
    private fun getFileNameFromUri(uri: Uri): String {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex) ?: "unknown_file"
        } ?: "unknown_file"
    }
    
    // Create new file
    fun createNewFile() {
        currentFileName.value = "new_file_${System.currentTimeMillis()}.txt"
        currentFileContent.value = ""
        currentFileUri.value = null
        isFileModified.value = false
    }
    
    // Update file content
    fun updateContent(content: String) {
        currentFileContent.value = content
        isFileModified.value = true
    }
    
    // Save to original file
    suspend fun saveToOriginalFile(): Boolean {
        return currentFileUri.value?.let { uri ->
            saveFileToUri(uri, currentFileContent.value)
        } ?: false
    }
    
    // Save as new file
    suspend fun saveAsNewFile(fileName: String): Boolean {
        return try {
            val uri = createNewFileUri(fileName)
            saveFileToUri(uri, currentFileContent.value).also { success ->
                if (success) {
                    currentFileUri.value = uri
                    currentFileName.value = fileName
                    isFileModified.value = false
                }
            }
        } catch (e: Exception) {
            Log.e("FileManager", "Error saving new file: ${e.message}")
            false
        }
    }
    
    // Save content to specific URI
    private suspend fun saveFileToUri(uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            }
            Log.d("FileManager", "Successfully saved file to URI: $uri")
            true
        } catch (e: Exception) {
            Log.e("FileManager", "Error saving file to URI: ${e.message}")
            false
        }
    }
    
    // Create new file URI
    private fun createNewFileUri(fileName: String): Uri {
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            java.io.File(context.filesDir, fileName)
        )
    }
    
    // Discard changes
    suspend fun discardChanges(): Boolean {
        return currentFileUri.value?.let { uri ->
            loadFileFromUri(uri)
        } ?: false
    }
    
    // Get display file name
    fun getDisplayFileName(): String {
        return if (currentFileName.value.isBlank()) {
            "Untitled"
        } else {
            currentFileName.value
        }
    }
    
    // Get saved files list
    fun getSavedFiles(): List<FileInfo> {
        val filesDir = context.filesDir
        return filesDir.listFiles()?.mapNotNull { file ->
            if (file.name.endsWith(".txt") || file.name.endsWith(".md")) {
                FileInfo(file.name, file.length())
            } else null
        }?.sortedByDescending { it.name } ?: emptyList()
    }
    
    // Load saved file
    suspend fun loadSavedFile(fileName: String): Boolean {
        return try {
            val file = java.io.File(context.filesDir, fileName)
            if (file.exists()) {
                currentFileContent.value = file.readText()
                currentFileName.value = fileName
                currentFileUri.value = createNewFileUri(fileName)
                isFileModified.value = false
                Log.d("FileManager", "Successfully loaded saved file: $fileName")
                true
            } else {
                Log.e("FileManager", "File not found: $fileName")
                false
            }
        } catch (e: Exception) {
            Log.e("FileManager", "Error loading saved file: ${e.message}")
            false
        }
    }
    
    // Delete file
    fun deleteFile(fileName: String): Boolean {
        return try {
            val file = java.io.File(context.filesDir, fileName)
            val deleted = file.delete()
            if (deleted) {
                Log.d("FileManager", "Successfully deleted file: $fileName")
            } else {
                Log.e("FileManager", "Failed to delete file: $fileName")
            }
            deleted
        } catch (e: Exception) {
            Log.e("FileManager", "Error deleting file: ${e.message}")
            false
        }
    }
    
    // Load file from content (for attached files)
    fun loadFileFromContent(fileName: String, content: String): Boolean {
        return try {
            currentFileName.value = fileName
            currentFileContent.value = content
            currentFileUri.value = null
            isFileModified.value = false
            Log.d("FileManager", "Successfully loaded file from content: $fileName")
            true
        } catch (e: Exception) {
            Log.e("FileManager", "Error loading file from content: ${e.message}")
            false
        }
    }
    
    // Open current file with appropriate app
    fun openCurrentFile(context: Context) {
        try {
            currentFileUri.value?.let { uri ->
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, getMimeType(uri))
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Log.d("FileManager", "Opened file: $uri")
            } ?: Log.e("FileManager", "No file URI to open")
        } catch (e: Exception) {
            Log.e("FileManager", "Error opening file: ${e.message}")
        }
    }
    
    // Get MIME type for URI
    private fun getMimeType(uri: Uri): String {
        return context.contentResolver.getType(uri) ?: "text/plain"
    }
    
    // File info data class
    data class FileInfo(
        val name: String,
        val length: Long
    )
}