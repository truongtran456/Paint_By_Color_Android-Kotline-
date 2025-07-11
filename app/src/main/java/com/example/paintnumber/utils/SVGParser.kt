package com.example.paintnumber.utils

import android.graphics.Color
import android.graphics.Path
import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.Picture
import android.graphics.Bitmap
import android.util.Log
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.RenderOptions
import com.example.paintnumber.models.PaintTemplate
import org.json.JSONObject
import java.io.InputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import android.graphics.Matrix

/**
 * Class chuyên xử lý việc đọc và phân tích file SVG.
 * Chức năng chính:
 * - Đọc file SVG và trích xuất metadata
 * - Phân tích các đường dẫn (path) trong SVG
 * - Tạo template cho việc tô màu
 */
class SVGParser {
    companion object {
        private const val TAG = "SVGParser"
    }

    // Vị trí hiện tại khi vẽ đường dẫn
    private var currentX: Float = 0f
    private var currentY: Float = 0f

    /**
     * Phân tích file SVG và tạo template tô màu
     * @param inputStream Stream chứa nội dung file SVG
     * @return PaintTemplate nếu phân tích thành công, null nếu có lỗi
     */
    fun parseTemplate(inputStream: InputStream): PaintTemplate? {
        return try {
            // Đọc nội dung SVG
            val svgContent = inputStream.bufferedReader().use { it.readText() }
            
            // Trích xuất metadata từ comment XML
            val metadataRegex = """<!--\s*(\{[\s\S]*?\})\s*-->""".toRegex()
            val metadataMatch = metadataRegex.find(svgContent)
            
            if (metadataMatch == null) {
                Log.e(TAG, "Không tìm thấy metadata trong SVG")
                return null
            }
            
            val metadata = JSONObject(metadataMatch.groupValues[1])
            val regionsArray = metadata.getJSONArray("regions")
            
            Log.d(TAG, "Tìm thấy ${regionsArray.length()} vùng trong metadata")
            
            // Phân tích các đường dẫn SVG
            val pathRegex = """<path\s+([^>]*)>""".toRegex(RegexOption.DOT_MATCHES_ALL)
            val idRegex = """id="([^"]*)"""".toRegex()
            val dRegex = """d="([^"]*)"""".toRegex()
            
            val regionPaths = mutableMapOf<String, MutableList<String>>()
            var pathCount = 0

            // Tìm và lưu tất cả các path trong SVG
            pathRegex.findAll(svgContent).forEach { pathMatch ->
                val attributes = pathMatch.groupValues[1]
                val idMatch = idRegex.find(attributes)
                val dMatch = dRegex.find(attributes)
                
                if (idMatch != null && dMatch != null) {
                    val id = idMatch.groupValues[1]
                    val pathData = dMatch.groupValues[1]
                    if (!regionPaths.containsKey(id)) {
                        regionPaths[id] = mutableListOf()
                    }
                    regionPaths[id]?.add(pathData)
                    pathCount++
                    Log.d(TAG, "Found path for region $id")
                }
            }
            
            Log.d(TAG, "Found $pathCount paths in SVG")
            
            // Debug: Print first path tag found in SVG
            val firstPathMatch = svgContent.indexOf("<path")
            if (firstPathMatch >= 0) {
                val endIndex = svgContent.indexOf(">", firstPathMatch) + 1
                Log.d(TAG, "First path tag: ${svgContent.substring(firstPathMatch, endIndex)}")
            }
            
            // Tạo list region
            val regions = mutableListOf<PaintTemplate.Region>()
            
            for (i in 0 until regionsArray.length()) {
                val region = regionsArray.getJSONObject(i)
                val id = region.getString("id")
                val number = region.getInt("number")
                val color = Color.parseColor(region.getString("color"))
                val isBackground = region.optBoolean("is_background", false)
                
                val pathDataList = regionPaths[id] ?: continue
                
                pathDataList.forEach { pathData ->
                    val path = Path()
                    try {
                        currentX = 0f
                        currentY = 0f
                        path.setPathData(pathData)
                        regions.add(PaintTemplate.Region(  // Tạo Region từ Path
                            id = regions.size + 1,
                            number = number,
                            targetColor = color,
                            isBackground = isBackground,
                            svgPath = pathData,
                            cachedPath = path
                        ))
                        Log.d(TAG, "Added region $id with number $number and color $color")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse path data for region $id: ${e.message}")
                    }
                }
            }
            
            if (regions.isEmpty()) {
                Log.e(TAG, "No valid regions found in SVG")
                return null
            }
            
            // Get SVG dimensions
            val svg = SVG.getFromString(svgContent)
            val width = svg.documentWidth.toInt()
            val height = svg.documentHeight.toInt()
            
            // Create template
            PaintTemplate(
                id = metadata.optString("name", "template_1"),
                name = metadata.optString("name", "Template"),
                difficulty = metadata.optInt("difficulty", 1),
                width = width,
                height = height,
                regions = regions
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing SVG template: ${e.message}")
            e.printStackTrace()
            null
        }
    }

 //Chuyển đổi Path Data thành Android Path
    private fun Path.setPathData(pathData: String) {
        val numbers = pathData.split("[^0-9.-]+".toRegex())
            .filter { it.isNotEmpty() }
            .map { it.toFloat() }
        
        var i = 0
        val commands = pathData.split("[0-9.-]+".toRegex())
            .filter { it.isNotEmpty() }
            .map { it.trim() }
            
        for (cmd in commands) {
            when (cmd) {
                "M", "m" -> {
                    if (i + 2 <= numbers.size) {
                        if (cmd == "M") {
                            moveTo(numbers[i], numbers[i + 1])
                            currentX = numbers[i]
                            currentY = numbers[i + 1]
                        } else {
                            rMoveTo(numbers[i], numbers[i + 1])
                            currentX += numbers[i]
                            currentY += numbers[i + 1]
                        }
                        i += 2
                    }
                }
                "L", "l" -> {
                    if (i + 2 <= numbers.size) {
                        if (cmd == "L") {
                            lineTo(numbers[i], numbers[i + 1])
                            currentX = numbers[i]
                            currentY = numbers[i + 1]
                        } else {
                            rLineTo(numbers[i], numbers[i + 1])
                            currentX += numbers[i]
                            currentY += numbers[i + 1]
                        }
                        i += 2
                    }
                }
                "H", "h" -> {
                    if (i + 1 <= numbers.size) {
                        if (cmd == "H") {
                            lineTo(numbers[i], currentY)
                            currentX = numbers[i]
                        } else {
                            rLineTo(numbers[i], 0f)
                            currentX += numbers[i]
                        }
                        i += 1
                    }
                }
                "V", "v" -> {
                    if (i + 1 <= numbers.size) {
                        if (cmd == "V") {
                            lineTo(currentX, numbers[i])
                            currentY = numbers[i]
                        } else {
                            rLineTo(0f, numbers[i])
                            currentY += numbers[i]
                        }
                        i += 1
                    }
                }
                "Z", "z" -> {
                    close()
                }
            }
        }
    }
} 