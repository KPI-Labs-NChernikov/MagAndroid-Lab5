package dev.nikita_chernikov.lab5

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val journalDir by lazy {
        File(getExternalFilesDir(null), "journals").also { if (!it.exists()) it.mkdirs() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userInput = intent?.getStringExtra("USER_INPUT")
        userInput?.let {
            serviceScope.launch {
                downloadFile(it)
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun downloadFile(userInput: String) {
        val finalFile = File(journalDir, "rfc$userInput.pdf")
        val tempFile = File(journalDir, "rfc$userInput.pdf.tmp")
        var successful = false

        try {
            tempFile.createNewFile()
            DownloadStatusManager.postUpdate() // Notify UI that download has started

            val finalUrl = if (userInput.startsWith("http")) userInput else "https://www.rfc-editor.org/rfc/rfc$userInput.pdf"
            val url = URL(finalUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                // Download TO the temp file
                FileOutputStream(tempFile).use { output ->
                    connection.inputStream.use { input ->
                        input.copyTo(output)
                    }
                }
                // If download is complete, rename the file
                tempFile.renameTo(finalFile)
                successful = true
            } else if (connection.responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(applicationContext, "File rfc$userInput.pdf not found (404)", Toast.LENGTH_LONG).show()
                }
            }
        } catch (_: Exception) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(applicationContext, "Download failed for rfc$userInput.pdf.", Toast.LENGTH_LONG).show()
            }
        } finally {
            if (!successful) {
                tempFile.delete()
            }
            DownloadStatusManager.postUpdate() // Notify UI that the process is finished (either success or fail)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}