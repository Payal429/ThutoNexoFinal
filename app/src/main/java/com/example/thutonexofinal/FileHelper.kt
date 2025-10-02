package com.example.thutonexofinal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import java.io.File

object FileHelper {

    // Save Base64 file to cache and open it
    fun openFile(context: Context, base64: String, fileName: String) {
        try {
            // Decode Base64 to bytes
            val bytes = Base64.decode(base64, Base64.DEFAULT)

            // Create temp file
            val tempFile = File.createTempFile(
                "file_${System.currentTimeMillis()}",
                "_$fileName",
                context.cacheDir
            )
            tempFile.writeBytes(bytes)
            tempFile.deleteOnExit()

            // Get content URI via FileProvider
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )

            // Determine MIME type
            val mimeType = getMimeType(fileName)

            // Create intent to view the file
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Open file"))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".pdf", true) -> "application/pdf"
            fileName.endsWith(".doc", true) -> "application/msword"
            fileName.endsWith(".docx", true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            fileName.endsWith(".ppt", true) -> "application/vnd.ms-powerpoint"
            fileName.endsWith(".pptx", true) -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            fileName.endsWith(".xls", true) -> "application/vnd.ms-excel"
            fileName.endsWith(".xlsx", true) -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            fileName.endsWith(".txt", true) -> "text/plain"
            else -> "*/*"
        }
    }
}
