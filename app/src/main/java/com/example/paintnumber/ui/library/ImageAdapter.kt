package com.example.paintnumber.ui.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.paintnumber.databinding.ItemImageBinding
import com.example.paintnumber.data.models.PaintImage
import java.io.File

class ImageAdapter(
    private val onItemClick: (PaintImage) -> Unit
) : ListAdapter<PaintImage, ImageAdapter.ImageViewHolder>(ImageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ImageViewHolder(
        private val binding: ItemImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(image: PaintImage) {
            if (image.isCompleted && image.completedImagePath != null) {
                // Hiển thị ảnh đã hoàn thành
                Glide.with(binding.root)
                    .load(File(image.completedImagePath))
                    .centerCrop()
                    .into(binding.imageView)
                
                // Hiển thị dấu tick
                binding.completionCheckmark.visibility = View.VISIBLE
            } else {
                // Hiển thị ảnh preview cho các ảnh chưa hoàn thành
                val previewResId = if (image.previewResId != 0) {
                    image.previewResId
                } else {
                    image.outlineResId
                }
                
                Glide.with(binding.root)
                    .load(previewResId)
                    .centerCrop()
                    .into(binding.imageView)
                
                // Ẩn dấu tick
                binding.completionCheckmark.visibility = View.GONE
            }

            // Hiển thị tag đặc biệt nếu category là "Đặc biệt"
            binding.specialTag.visibility = if (image.category == "Đặc biệt") {
                View.VISIBLE
            } else {
                View.GONE
            }

            binding.root.setOnClickListener { onItemClick(image) }
        }
    }
}

private class ImageDiffCallback : DiffUtil.ItemCallback<PaintImage>() {
    override fun areItemsTheSame(oldItem: PaintImage, newItem: PaintImage) = 
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: PaintImage, newItem: PaintImage) = 
        oldItem == newItem
} 