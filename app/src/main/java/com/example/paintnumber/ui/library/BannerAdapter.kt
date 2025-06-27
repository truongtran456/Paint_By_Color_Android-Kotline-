package com.example.paintnumber.ui.library

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.paintnumber.databinding.ItemBannerBinding
import com.example.paintnumber.data.models.Banner

class BannerAdapter : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    private var banners: List<Banner> = emptyList()

    fun submitList(newList: List<Banner>) {
        banners = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val binding = ItemBannerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BannerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        holder.bind(banners[position])
    }

    override fun getItemCount() = banners.size

    inner class BannerViewHolder(
        private val binding: ItemBannerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(banner: Banner) {
            Glide.with(binding.root)
                .load(banner.imageResId)
                .transform(RoundedCorners(16))
                .into(binding.bannerImage)

            binding.titleText.text = banner.title
            binding.descriptionText.text = banner.description
            
            binding.root.setOnClickListener {
                // TODO: Handle banner click
            }
        }
    }
} 