package dev.nikita_chernikov.lab5

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.nikita_chernikov.lab5.databinding.ItemFileBinding
import java.io.File

class FileAdapter(
    private val onView: (File) -> Unit,
    private val onDelete: (File) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    private var files: List<File> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(files[position])
    }

    override fun getItemCount(): Int = files.size

    fun updateFiles(newFiles: List<File>) {
        val diffCallback = FileDiffCallback(this.files, newFiles)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.files = newFiles
        diffResult.dispatchUpdatesTo(this)
    }

    inner class FileViewHolder(private val binding: ItemFileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(file: File) {
            binding.fileNameTextView.text = file.name
            val isDownloading = file.extension == "tmp"

            binding.progressBar.isVisible = isDownloading
            binding.buttonLayout.isVisible = !isDownloading

            if (!isDownloading) {
                binding.viewButton.setOnClickListener { onView(file) }
                binding.deleteButton.setOnClickListener { onDelete(file) }
            }
        }
    }

    class FileDiffCallback(
        private val oldList: List<File>,
        private val newList: List<File>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            // Compare unique identifiers, like file paths
            return oldList[oldItemPosition].path == newList[newItemPosition].path
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            // Compare the content itself. For files, name and extension are good indicators.
            val oldFile = oldList[oldItemPosition]
            val newFile = newList[newItemPosition]
            return oldFile.name == newFile.name && oldFile.extension == newFile.extension
        }
    }
}
