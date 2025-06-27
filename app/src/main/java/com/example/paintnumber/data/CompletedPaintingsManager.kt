package com.example.paintnumber.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONObject
import java.io.File

/**
 * Quản lý thông tin về các bức tranh đã hoàn thành
 */
class CompletedPaintingsManager(context: Context) {
    companion object {
        private const val TAG = "CompletedPaintingsManager"
        private const val PREFS_NAME = "completed_paintings"
        private const val KEY_COMPLETED_PAINTINGS = "completed_paintings_data"
        private const val KEY_IN_PROGRESS_PAINTINGS = "in_progress_paintings_data"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Đánh dấu một bức tranh là đã hoàn thành
     * @param imageId ID của bức tranh
     * @param imagePath Đường dẫn đến file ảnh đã hoàn thành
     */
    fun markAsCompleted(imageId: String, imagePath: String) {
        try {
            val completedData = getCompletedPaintingsData()
            completedData.put(imageId, imagePath)
            saveCompletedPaintingsData(completedData)
            Log.d(TAG, "Marked painting $imageId as completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking painting as completed: ${e.message}")
        }
    }

    /**
     * Kiểm tra xem một bức tranh đã hoàn thành chưa
     * @param imageId ID của bức tranh
     * @return true nếu đã hoàn thành, false nếu chưa
     */
    fun isCompleted(imageId: String): Boolean {
        return try {
            val completedData = getCompletedPaintingsData()
            completedData.has(imageId)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if painting is completed: ${e.message}")
            false
        }
    }

    /**
     * Lấy đường dẫn đến file ảnh đã hoàn thành
     * @param imageId ID của bức tranh
     * @return Đường dẫn đến file ảnh hoặc null nếu chưa hoàn thành
     */
    fun getCompletedImagePath(imageId: String): String? {
        return try {
            val completedData = getCompletedPaintingsData()
            if (completedData.has(imageId)) {
                completedData.getString(imageId)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting completed image path: ${e.message}")
            null
        }
    }

    /**
     * Lấy danh sách đường dẫn của tất cả các bức tranh đã hoàn thành
     * @return List<String> chứa đường dẫn đến các file ảnh đã hoàn thành
     */
    fun getCompletedPaintings(): List<String> {
        return try {
            val completedData = getCompletedPaintingsData()
            val paths = mutableListOf<String>()
            completedData.keys().forEach { key ->
                paths.add(completedData.getString(key))
            }
            paths
        } catch (e: Exception) {
            Log.e(TAG, "Error getting completed paintings: ${e.message}")
            emptyList()
        }
    }

    /**
     * Lưu tiến trình của một bức tranh
     * @param imageId ID của bức tranh
     * @param progressPath Đường dẫn đến file ảnh tiến trình
     */
    fun saveProgressPath(imageId: String, progressPath: String) {
        try {
            val progressData = getInProgressPaintingsData()
            progressData.put(imageId, progressPath)
            saveInProgressPaintingsData(progressData)
            Log.d(TAG, "Saved progress for painting $imageId")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving progress: ${e.message}")
        }
    }

    /**
     * Kiểm tra xem một bức tranh có đang trong tiến trình không
     * @param imageId ID của bức tranh
     * @return true nếu đang trong tiến trình, false nếu không
     */
    fun isInProgress(imageId: String): Boolean {
        return try {
            val progressData = getInProgressPaintingsData()
            progressData.has(imageId)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if painting is in progress: ${e.message}")
            false
        }
    }

    /**
     * Lấy đường dẫn đến file ảnh tiến trình
     * @param imageId ID của bức tranh
     * @return Đường dẫn đến file ảnh hoặc null nếu không có
     */
    fun getProgressPath(imageId: String): String? {
        return try {
            val progressData = getInProgressPaintingsData()
            if (progressData.has(imageId)) {
                progressData.getString(imageId)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting progress path: ${e.message}")
            null
        }
    }

    /**
     * Lấy danh sách đường dẫn của tất cả các bức tranh đang trong tiến trình
     * @return List<String> chứa đường dẫn đến các file ảnh đang trong tiến trình
     */
    fun getInProgressPaintings(): List<String> {
        return try {
            val progressData = getInProgressPaintingsData()
            val paths = mutableListOf<String>()
            progressData.keys().forEach { key ->
                paths.add(progressData.getString(key))
            }
            paths
        } catch (e: Exception) {
            Log.e(TAG, "Error getting in progress paintings: ${e.message}")
            emptyList()
        }
    }

    /**
     * Xóa tiến trình của một bức tranh
     * @param imageId ID của bức tranh
     */
    fun removeProgress(imageId: String) {
        try {
            val progressData = getInProgressPaintingsData()
            if (progressData.has(imageId)) {
                // Xóa file ảnh tiến trình
                val progressPath = progressData.getString(imageId)
                File(progressPath).delete()
                
                // Xóa dữ liệu từ SharedPreferences
                progressData.remove(imageId)
                saveInProgressPaintingsData(progressData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing progress: ${e.message}")
        }
    }

    // Hàm private để lấy dữ liệu từ SharedPreferences
    private fun getCompletedPaintingsData(): JSONObject {
        val jsonStr = prefs.getString(KEY_COMPLETED_PAINTINGS, "{}")
        return JSONObject(jsonStr)
    }

    private fun saveCompletedPaintingsData(data: JSONObject) {
        prefs.edit().putString(KEY_COMPLETED_PAINTINGS, data.toString()).apply()
    }

    private fun getInProgressPaintingsData(): JSONObject {
        val jsonStr = prefs.getString(KEY_IN_PROGRESS_PAINTINGS, "{}")
        return JSONObject(jsonStr)
    }

    private fun saveInProgressPaintingsData(data: JSONObject) {
        prefs.edit().putString(KEY_IN_PROGRESS_PAINTINGS, data.toString()).apply()
    }
} 