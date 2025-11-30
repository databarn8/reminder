package com.reminder.app.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore

/**
 * File manager utility for handling file operations in the reminder app
 */
class FileManager(private val context: Context) {
    
    companion object {
        private const val TAG = "FileManager"
        private const val REMINDER_FILES_DIR = "reminder_files"
    }
    
    // Current file state
    var currentFileName = mutableStateOf("")
    var currentFileContent = mutableStateOf("")
    var currentFileUri = mutableStateOf<Uri?>(null)
    var isFileModified = mutableStateOf(false)
    
    // Get the app's private external storage directory for reminder files
    private fun getReminderFilesDir(): File {
        val dir = File(context.getExternalFilesDir(null), REMINDER_FILES_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    /**
     * Check if the URI points to an image file
     */
    private fun isImageFile(uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType?.startsWith("image/") == true
    }
    
    /**
     * Get basic image information
     */
    private fun getImageInfo(uri: Uri): String {
        return try {
            if (isImageFile(uri)) {
                val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                if (bitmap != null) {
                    "📷 Image: ${bitmap.width}x${bitmap.height} pixels"
                } else {
                    "📷 Image: Unable to load"
                }
            } else {
                "📄 File: Text document"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting image info: ${e.message}")
            "📄 File: Unknown type"
        }
    }
    
    /**
     * Load content from a URI (file picker result)
     */
    suspend fun loadFileFromUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Loading file from URI: $uri")
            
            // Get file name
            val fileName = getFileName(uri) ?: "unknown_file"
            val content = if (isImageFile(uri)) {
                // For images, store image info instead of binary data
                val imageInfo = getImageInfo(uri)
                Log.d(TAG, "Loaded image file: $fileName, info: $imageInfo")
                imageInfo
            } else {
                // For text files, read the content
                readContentFromUri(uri)
            }
            
            currentFileName.value = fileName
            currentFileContent.value = content
            currentFileUri.value = uri
            isFileModified.value = false
            
            Log.d(TAG, "Successfully loaded file: $fileName, content length: ${content.length}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error loading file from URI: ${e.message}")
            false
        }
    }
    
    /**
     * Save the current content to the original file (if loaded from a file)
     */
    suspend fun saveToOriginalFile(): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = currentFileUri.value
            if (uri == null) {
                Log.w(TAG, "No original file URI to save to")
                false
            } else {
                Log.d(TAG, "Saving to original file: $uri")
                writeContentToUri(uri, currentFileContent.value)
                isFileModified.value = false
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to original file: ${e.message}")
            false
        }
    }
    
    /**
     * Save the current content to a new file in the app's private storage
     */
    suspend fun saveAsNewFile(fileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Saving as new file: $fileName")
            
            val filesDir = getReminderFilesDir()
            val file = File(filesDir, fileName)
            
            FileOutputStream(file).use { output ->
                output.write(currentFileContent.value.toByteArray())
            }
            
            currentFileName.value = fileName
            currentFileUri.value = Uri.fromFile(file)
            isFileModified.value = false
            
            Log.d(TAG, "Successfully saved new file: $file")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving new file: ${e.message}")
            false
        }
    }
    
    /**
     * Discard changes and reload the original file content
     */
    suspend fun discardChanges(): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = currentFileUri.value
            if (uri == null) {
                // If no original file, just clear everything
                currentFileName.value = ""
                currentFileContent.value = ""
                currentFileUri.value = null
                isFileModified.value = false
                true
            } else {
                // Reload from original URI
                val content = readContentFromUri(uri)
                currentFileContent.value = content
                isFileModified.value = false
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error discarding changes: ${e.message}")
            false
        }
    }
    
    /**
     * Create a new empty file
     */
    fun createNewFile() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "reminder_$timestamp.txt"
        
        currentFileName.value = fileName
        currentFileContent.value = ""
        currentFileUri.value = null
        isFileModified.value = false
        
        Log.d(TAG, "Created new file: $fileName")
    }
    
    /**
     * Update the current file content and mark as modified
     */
    fun updateContent(newContent: String) {
        if (currentFileContent.value != newContent) {
            currentFileContent.value = newContent
            isFileModified.value = true
        }
    }
    
    /**
     * Get the display name for the current file
     */
    fun getDisplayFileName(): String {
        val name = currentFileName.value
        return if (isFileModified.value) {
            "$name*"
        } else {
            name
        }
    }
    
    /**
     * Get file name from URI
     */
    private fun getFileName(uri: Uri): String? {
        return when (uri.scheme) {
            "content" -> {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    cursor.getString(nameIndex)
                }
            }
            "file" -> File(uri.path ?: "").name
            else -> null
        }
    }
    
    /**
     * Read content from URI
     */
    private fun readContentFromUri(uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader().use { reader ->
                reader.readText()
            }
        } ?: throw IOException("Could not open input stream for URI: $uri")
    }
    
    /**
     * Write content to URI
     */
    private fun writeContentToUri(uri: Uri, content: String) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(content.toByteArray())
        } ?: throw IOException("Could not open output stream for URI: $uri")
    }
    
    /**
     * Get list of all saved reminder files
     */
    fun getSavedFiles(): List<File> {
        val filesDir = getReminderFilesDir()
        return filesDir.listFiles()?.toList() ?: emptyList()
    }
    
    /**
     * Delete a saved file
     */
    fun deleteFile(fileName: String): Boolean {
        val file = File(getReminderFilesDir(), fileName)
        return if (file.exists()) {
            val deleted = file.delete()
            Log.d(TAG, "Deleted file $fileName: $deleted")
            deleted
        } else {
            Log.w(TAG, "File not found for deletion: $fileName")
            false
        }
    }
    
    /**
     * Load a previously saved file
     */
    suspend fun loadSavedFile(fileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(getReminderFilesDir(), fileName)
            if (!file.exists()) {
                Log.w(TAG, "Saved file not found: $fileName")
                return@withContext false
            }
            
            val content = file.readText()
            
            currentFileName.value = fileName
            currentFileContent.value = content
            currentFileUri.value = Uri.fromFile(file)
            isFileModified.value = false
            
            Log.d(TAG, "Successfully loaded saved file: $fileName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error loading saved file: ${e.message}")
            false
        }
    }
}