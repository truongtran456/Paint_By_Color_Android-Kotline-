package com.example.paintnumber.models

/**
 * Đại diện cho một màu trong bảng màu của template
 * @param color Giá trị màu dạng ARGB
 * @param number Số tương ứng với màu trong template
 */
data class PaintColor(
    val color: Int,
    val number: Int
) 