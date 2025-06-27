package com.example.paintnumber.models

import android.graphics.Color
import android.graphics.Path

/**
 * Đại diện cho một template tô màu hoàn chỉnh
 * @param id ID duy nhất của template
 * @param name Tên hiển thị của template
 * @param difficulty Độ khó (1-5)
 * @param width Chiều rộng của template
 * @param height Chiều cao của template
 * @param regions Danh sách các vùng cần tô màu
 */
data class PaintTemplate(
    val id: String,
    val name: String,
    val difficulty: Int,
    val width: Int,
    val height: Int,
    val regions: List<Region>
) {
    /**
     * Đại diện cho một vùng cần tô màu trong template
     * @param id ID duy nhất của vùng
     * @param svgPath Đường dẫn SVG định nghĩa hình dạng vùng
     * @param number Số hiển thị trong vùng
     * @param targetColor Màu đích của vùng
     * @param isBackground Có phải là vùng nền không
     * @param cachedPath Đường dẫn đã được cache để tối ưu hiệu suất
     */
    data class Region(
        val id: Int,
        val svgPath: String,
        val number: Int,
        val targetColor: Int,
        val isBackground: Boolean = false,
        var cachedPath: Path? = null
    )
}

/**
 * Thông tin mô tả về một template
 * @param id ID duy nhất của template
 * @param name Tên hiển thị
 * @param difficulty Độ khó (1-5)
 * @param previewImageUrl URL của ảnh xem trước
 * @param category Danh mục của template
 */
data class PaintTemplateMetadata(
    val id: String,
    val name: String,
    val difficulty: Int,
    val previewImageUrl: String,
    val category: String
) 