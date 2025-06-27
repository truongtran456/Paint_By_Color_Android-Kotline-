package com.example.paintnumber.utils

import android.graphics.Color

/**
 * Quản lý các màu sắc trong ứng dụng.
 * Class này cung cấp:
 * - Bảng màu mặc định cho các số từ 1-9
 * - Khả năng tùy chỉnh màu cho từng số
 * - Kiểm tra tính hợp lệ của số màu
 */
object ColorManager {
    // Bảng màu mặc định
    private val defaultColors = mapOf(
        1 to Color.parseColor("#FF0000"),  // Đỏ
        2 to Color.parseColor("#00FF00"),  // Xanh lá
        3 to Color.parseColor("#0000FF"),  // Xanh dương
        4 to Color.parseColor("#FFFF00"),  // Vàng
        5 to Color.parseColor("#FF00FF"),  // Hồng
        6 to Color.parseColor("#00FFFF"),  // Xanh ngọc
        7 to Color.parseColor("#FFA500"),  // Cam
        8 to Color.parseColor("#800080"),  // Tím
        9 to Color.parseColor("#008000")   // Xanh lá đậm
    )
    
    // Lưu trữ các màu tùy chỉnh
    private var customColors = mutableMapOf<Int, Int>()
    
    /**
     * Lấy màu tương ứng với số được chỉ định
     * @param number Số cần lấy màu
     * @return Mã màu tương ứng, nếu không tìm thấy sẽ trả về màu xám
     */
    fun getColorForNumber(number: Int): Int {
        return customColors[number] ?: defaultColors[number] ?: Color.GRAY
    }
    
    /**
     * Đặt màu tùy chỉnh cho một số
     * @param number Số cần đặt màu
     * @param color Mã màu mới
     */
    fun setColorForNumber(number: Int, color: Int) {
        customColors[number] = color
    }
    
    /**
     * Xóa tất cả các màu tùy chỉnh, trở về bảng màu mặc định
     */
    fun clearCustomColors() {
        customColors.clear()
    }
    
    /**
     * Kiểm tra xem một số có hợp lệ để sử dụng làm số màu không
     * @param number Số cần kiểm tra
     * @return true nếu số nằm trong khoảng 1-9 hoặc đã được đặt màu tùy chỉnh
     */
    fun isValidNumber(number: Int): Boolean {
        return number in 1..9 || customColors.containsKey(number)
    }
} 