package com.example.paintnumber.ui.library

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.paintnumber.R
import com.example.paintnumber.data.models.Banner
import com.example.paintnumber.data.models.PaintImage
import com.example.paintnumber.data.CompletedPaintingsManager
import com.example.paintnumber.utils.TemplateManager
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val _banners = MutableLiveData<List<Banner>>()
    val banners: LiveData<List<Banner>> = _banners

    private val _images = MutableLiveData<List<PaintImage>>()
    val images: LiveData<List<PaintImage>> = _images

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val completedPaintingsManager = CompletedPaintingsManager(application)

    private val categories = listOf(
        "Mới",
        "Đặc biệt",
        "Nhân vật",
        "Màu sắc",
        "Động vật",
        "Tranh vẽ",
        "Truyện",
        "Phong cảnh",
        "Hoa",
        "Mandala",
        "Anime",
        "Chibi",
        "Hoạt hình"
    )

    init {
        loadBanners()
        loadNewImages()
    }

    fun onCategorySelected(position: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            when (position) {
                0 -> loadNewImages()
                1 -> loadSpecialImages()
                else -> loadCategoryImages(position)
            }
            _isLoading.value = false
        }
    }

    private fun loadBanners() {
        val dummyBanners = listOf(
            Banner(
                id = "1",
                imageResId = R.drawable.banner_1,
                title = "Bộ sưu tập mới",
                description = "Khám phá những bức tranh mới nhất"
            )
        )
        _banners.value = dummyBanners
    }
    //Hiển thị trạng thái hoàn thành
    private fun createPaintImage(id: Int, previewResId: Int, category: String): PaintImage {
        val imageId = "image_$id"
        val isCompleted = completedPaintingsManager.isCompleted(imageId)
        val completedImagePath = if (isCompleted) {
            completedPaintingsManager.getCompletedImagePath(imageId)
        } else {
            null
        }

        return PaintImage(
            id = imageId,
            previewResId = previewResId,
            outlineResId = 0,
            category = category,
            difficulty = 1,
            progress = 0,
            colorData = mapOf(),
            isCompleted = isCompleted,
            completedImagePath = completedImagePath
        )
    }

    private fun loadNewImages() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                val images = mutableListOf<PaintImage>()
                
                // Load all available preview images (2-5)
                for (id in 1..5) {
                    val previewResId = context.resources.getIdentifier(
                        "image_${id}_preview",
                        "drawable",
                        context.packageName
                    )
                    
                    if (previewResId != 0) {
                        images.add(createPaintImage(id, previewResId, "Mới"))
                        Log.d("LibraryViewModel", "Added image_${id}_preview to library")
                    } else {
                        Log.e("LibraryViewModel", "Failed to find preview for image_${id}")
                    }
                }
                
                _images.postValue(images)
                Log.d("LibraryViewModel", "Posted ${images.size} images to view")
                
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error loading images: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun loadSpecialImages() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                val images = mutableListOf<PaintImage>()
                
                // Add images 2 and 3 to special category
                for (id in 1..3) {
                    val previewResId = context.resources.getIdentifier(
                        "image_${id}_preview",
                        "drawable",
                        context.packageName
                    )
                    
                    if (previewResId != 0) {
                        images.add(createPaintImage(id, previewResId, "Đặc biệt"))
                        Log.d("LibraryViewModel", "Added image_${id}_preview to special category")
                    }
                }
                
                _images.postValue(images)
                
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error loading special images: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun loadCategoryImages(categoryPosition: Int) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                val images = mutableListOf<PaintImage>()
                
                // Add images 4 and 5 to other categories
                for (id in 4..5) {
                    val previewResId = context.resources.getIdentifier(
                        "image_${id}_preview",
                        "drawable",
                        context.packageName
                    )
                    
                    if (previewResId != 0) {
                        images.add(createPaintImage(id, previewResId, categories[categoryPosition]))
                        Log.d("LibraryViewModel", "Added image_${id}_preview to ${categories[categoryPosition]} category")
                    }
                }
                
                _images.postValue(images)
                
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error loading category images: ${e.message}")
                e.printStackTrace()
            }
        }
    }
} 