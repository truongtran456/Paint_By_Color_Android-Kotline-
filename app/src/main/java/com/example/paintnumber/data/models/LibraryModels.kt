package com.example.paintnumber.data.models

data class Banner(
    val id: String,
    val imageResId: Int,
    val title: String,
    val description: String
)

data class PaintImage(
    val id: String,
    val previewResId: Int,
    val outlineResId: Int = 0,
    val category: String,
    val difficulty: Int = 1,
    val progress: Int = 0,
    val colorData: Map<String, Int> = mapOf(),
    var isCompleted: Boolean = false,
    var completedImagePath: String? = null,  // Path to the saved colored image
    var isInProgress: Boolean = false,  // Whether the image is in progress
    var progressPath: String? = null  // Path to the saved progress image
) 