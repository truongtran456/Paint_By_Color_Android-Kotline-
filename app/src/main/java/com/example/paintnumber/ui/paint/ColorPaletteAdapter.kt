package com.example.paintnumber.ui.paint

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.paintnumber.databinding.ItemColorBinding
import com.example.paintnumber.models.PaintColor
import com.google.android.material.card.MaterialCardView

class ColorPaletteAdapter(
    private val onColorSelected: (color: Int, number: Int) -> Unit
) : ListAdapter<PaintColor, ColorPaletteAdapter.ColorViewHolder>(ColorDiffCallback()) {

    private var selectedPosition = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val binding = ItemColorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ColorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, position == selectedPosition)

        holder.itemView.setOnClickListener {

            // Cập nhật vị trí được chọn
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition

            // Cập nhật UI
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)

            // Gọi callback với màu và số được chọn
            onColorSelected(item.color, item.number)
        }
    }

    inner class ColorViewHolder(private val binding: ItemColorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PaintColor, isSelected: Boolean) {
            // Set color
            binding.colorView.setCardBackgroundColor(item.color)
            
            // Set number
            binding.numberText.text = item.number.toString()
            
            // Set text color based on background color brightness
            val color = item.color
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            val brightness = (r * 299 + g * 587 + b * 114) / 1000
            binding.numberText.setTextColor(if (brightness > 128) Color.BLACK else Color.WHITE)
            
            // Handle selection state
            binding.colorView.cardElevation = if (isSelected) 8f else 2f
            binding.colorView.setStrokeWidth(if (isSelected) 2 else 0)
            binding.colorView.setCardBackgroundColor(item.color)
        }
    }

    private class ColorDiffCallback : DiffUtil.ItemCallback<PaintColor>() {
        override fun areItemsTheSame(oldItem: PaintColor, newItem: PaintColor): Boolean {
            return oldItem.number == newItem.number
        }

        override fun areContentsTheSame(oldItem: PaintColor, newItem: PaintColor): Boolean {
            return oldItem == newItem
        }
    }
} 
