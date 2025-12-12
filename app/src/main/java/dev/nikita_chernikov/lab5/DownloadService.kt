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
        val tempFile = File(journalDir, "$userInput.tmp")
        try {
            tempFile.createNewFile()
            DownloadStatusManager.postUpdate()

            val finalUrl = if (userInput.startsWith("http")) userInput else "https://www.rfc-editor.org/rfc/rfc$userInput.pdf"
            val url = URL(finalUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val finalFile = File(journalDir, "rfc$userInput.pdf")
                FileOutputStream(finalFile).use { output ->
                    connection.inputStream.use { input ->
                        input.copyTo(output)
                    }
                }
            } else if (connection.responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(applicationContext, "File rfc$userInput.pdf not found (404)", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(applicationContext, "Download failed for rfc$userInput.pdf: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } finally {
            tempFile.delete()
            DownloadStatusManager.postUpdate()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}