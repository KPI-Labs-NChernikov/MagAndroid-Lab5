package dev.nikita_chernikov.lab5

import android.annotation.SuppressLint
import android.content.*
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.nikita_chernikov.lab5.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fileAdapter: FileAdapter
    private val journalDir by lazy {
        File(getExternalFilesDir(null), "journals").also { if (!it.exists()) it.mkdirs() }
    }

    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "app_prefs"
    private val DONT_SHOW_AGAIN = "dont_show_again"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()

        binding.fab.setOnClickListener {
            startActivity(Intent(this, DownloadActivity::class.java))
        }

        lifecycleScope.launch {
            DownloadStatusManager.updateFlow.collectLatest {
                updateFileList()
            }
        }

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (!sharedPreferences.getBoolean(DONT_SHOW_AGAIN, false)) {
            binding.root.post { showPopupWindow() }
        }
    }

    override fun onResume() {
        super.onResume()
        updateFileList()
    }

    @SuppressLint("InflateParams")
    private fun showPopupWindow() {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_manual, null)

        val popupWindow = PopupWindow(
            popupView,
            (resources.displayMetrics.widthPixels * 0.8).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        val okButton = popupView.findViewById<Button>(R.id.okButton)
        val dontShowAgainCheckbox = popupView.findViewById<CheckBox>(R.id.dontShowAgainCheckbox)

        okButton.setOnClickListener {
            if (dontShowAgainCheckbox.isChecked) {
                sharedPreferences.edit { putBoolean(DONT_SHOW_AGAIN, true) }
            }
            popupWindow.dismiss()
        }

        popupWindow.showAtLocation(binding.root, Gravity.CENTER, 0, 0)
    }

    private fun setupRecyclerView() {
        fileAdapter = FileAdapter(
            onView = { file -> viewFile(file) },
            onDelete = { file -> deleteFile(file) }
        )
        binding.recyclerView.apply {
            adapter = fileAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun updateFileList() {
        val files = journalDir.listFiles() ?: emptyArray()
        val sortedFiles = files.sortedByDescending { it.lastModified() }

        if (sortedFiles.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
        }

        fileAdapter.updateFiles(sortedFiles)
    }

    private fun viewFile(file: File) {
        val uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No PDF viewer found", Toast.LENGTH_LONG).show()
        }
    }

    private fun deleteFile(file: File) {
        if (file.exists()) {
            if (file.delete()) {
                Toast.makeText(this, "File deleted: ${file.name}", Toast.LENGTH_SHORT).show()
                updateFileList()
            } else {
                Toast.makeText(this, "Failed to delete file", Toast.LENGTH_SHORT).show()
            }
        }
    }
}