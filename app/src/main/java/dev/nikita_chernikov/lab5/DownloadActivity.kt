package dev.nikita_chernikov.lab5

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dev.nikita_chernikov.lab5.databinding.ActivityDownloadBinding
import dev.nikita_chernikov.lab5.databinding.ActivityMainBinding
import java.io.File

class DownloadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDownloadBinding
    private val journalDir by lazy {
        File(getExternalFilesDir(null), "journals").also { if (!it.exists()) it.mkdirs() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDownloadBinding.inflate(layoutInflater)
        val root = binding.root
        setContentView(root)

        val initialLeft = root.paddingLeft
        val initialTop = root.paddingTop
        val initialRight = root.paddingRight
        val initialBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                initialLeft + systemBars.left,
                initialTop + systemBars.top,
                initialRight + systemBars.right,
                initialBottom + systemBars.bottom
            )
            insets
        }

        binding.downloadButton.setOnClickListener {
            val userInput = binding.urlEditText.text.toString().trim()
            if (userInput.isNotEmpty()) {
                if (isFileAlreadyPresent(userInput)) {
                    Toast.makeText(this, "File already exists or is downloading.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                if (isNetworkAvailable()) {
                    startDownload(userInput)
                } else {
                    Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter an RFC number", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isFileAlreadyPresent(userInput: String): Boolean {
        val finalFile = File(journalDir, "rfc$userInput.pdf")
        val tempFile = File(journalDir, "$userInput.tmp")
        return finalFile.exists() || tempFile.exists()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }

    private fun startDownload(userInput: String) {
        val intent = Intent(this, DownloadService::class.java).apply {
            putExtra("USER_INPUT", userInput)
        }
        startService(intent)
        Toast.makeText(this, "Download started...", Toast.LENGTH_SHORT).show()
        finish()
    }
}