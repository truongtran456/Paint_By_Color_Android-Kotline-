package com.example.paintnumber.utils

import android.content.Context
import android.graphics.Path
import android.util.LruCache
import com.caverock.androidsvg.SVG
import com.example.paintnumber.models.PaintTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.URL
import java.io.File

/**
 * Quản lý các template tô màu trong ứng dụng.
 * Chức năng chính:
 * - Tải và lưu cache các template
 * - Quản lý bộ nhớ cache để tối ưu hiệu suất
 * - Cung cấp các phương thức để truy cập template
 */
class TemplateManager private constructor(private val context: Context) {
    private val appContext = context.applicationContext
    private val memoryCache: LruCache<String, PaintTemplate>
    private val svgParser = SVGParser()

    init {
        // Tính toán kích thước cache dựa trên bộ nhớ khả dụng
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8 // Sử dụng 1/8 bộ nhớ khả dụng cho cache
        
        memoryCache = object : LruCache<String, PaintTemplate>(cacheSize) {
            override fun sizeOf(key: String, template: PaintTemplate): Int {
                // Ước tính kích thước template trong KB
                return (template.width * template.height * 4 / 1024) +
                       (template.regions.size * 2) // Thêm dung lượng cho các vùng
            }
        }
    }

    /**
     * Tải template từ resource hoặc stream
     * @param templateId ID của template cần tải
     * @param svgStream Stream chứa dữ liệu SVG (tùy chọn)
     * @return Template nếu tải thành công, null nếu có lỗi
     */
    suspend fun loadTemplate(templateId: String, svgStream: InputStream? = null): PaintTemplate? {
        // Thử lấy từ cache trước
        memoryCache.get(templateId)?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val template = if (svgStream != null) {
                    // Tải từ SVG stream được cung cấp
                    svgParser.parseTemplate(svgStream)
                } else {
                    // Tải từ resource
                    val resourceId = appContext.resources.getIdentifier(
                        templateId, "raw", appContext.packageName
                    )
                    if (resourceId == 0) {
                        android.util.Log.e("TemplateManager", "Không tìm thấy resource cho ID: $templateId")
                        return@withContext null
                    }
                    appContext.resources.openRawResource(resourceId).use { inputStream ->
                        svgParser.parseTemplate(inputStream)
                    }
                }
                
                if (template != null) {
                    memoryCache.put(templateId, template)
                }
                template
                
            } catch (e: Exception) {
                android.util.Log.e("TemplateManager", "Lỗi khi tải template: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Xóa toàn bộ cache để giải phóng bộ nhớ
     */
    fun clearCache() {
        memoryCache.evictAll()
    }

    companion object {
        @Volatile
        private var instance: TemplateManager? = null
        
        /**
         * Lấy instance của TemplateManager (Singleton pattern)
         */
        fun getInstance(context: Context): TemplateManager {
            return instance ?: synchronized(this) {
                instance ?: TemplateManager(context).also { instance = it }
            }
        }
    }
} 