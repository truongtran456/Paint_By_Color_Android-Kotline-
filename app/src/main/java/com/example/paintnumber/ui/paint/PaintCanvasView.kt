package com.example.paintnumber.ui.paint

import android.content.Context
import android.graphics.*
import android.graphics.Bitmap.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import androidx.core.view.ScaleGestureDetectorCompat
import androidx.core.view.ViewCompat
import com.example.paintnumber.models.PaintTemplate
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import android.view.Gravity
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ImageView
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import com.example.paintnumber.utils.ConfettiManager
import com.google.android.material.button.MaterialButton

/**
 * View chính để xử lý việc vẽ và tô màu trong ứng dụng Paint By Number.
 * Class này quản lý:
 * - Hiển thị hình ảnh gốc và các vùng cần tô
 * - Xử lý thao tác chạm và phóng to/thu nhỏ
 * - Tô màu các vùng khi người dùng chọn
 * - Hiệu ứng hoàn thành khi tô xong
 */
class PaintCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /**
     * Interface để thông báo khi người dùng hoàn thành việc tô màu toàn bộ hình
     */
    interface OnPaintingCompletedListener {
        fun onPaintingCompleted(completedBitmap: Bitmap)
    }

    /**
     * Interface để thông báo khi người dùng hoàn thành việc tô một màu cụ thể
     */
    interface OnColorCompletedListener {
        fun onColorCompleted(number: Int)
        fun onRegionFilled()
    }

    var onPaintingCompletedListener: OnPaintingCompletedListener? = null
    var onColorCompletedListener: OnColorCompletedListener? = null
    private var completionMessageView: TextView? = null
    private var isShowingCompletionAnimation = false

    companion object {
        private const val TAG = "PaintCanvasView"
        private const val MIN_SCALE = 0.5f  // Tỷ lệ thu nhỏ tối thiểu
        private const val MAX_SCALE = 3.0f  // Tỷ lệ phóng to tối đa
    }

    /**
     * Class đại diện cho một vùng cần tô màu
     * @param path Đường dẫn định nghĩa hình dạng vùng
     * @param number Số hiển thị trong vùng
     * @param currentColor Màu hiện tại của vùng (TRANSPARENT nếu chưa tô)
     * @param isHighlighted Trạng thái highlight khi vùng được chọn
     * @param bounds Khung giới hạn của vùng
     */
    data class Region(
        val path: Path,
        val number: Int,
        var currentColor: Int = Color.TRANSPARENT,
        var isHighlighted: Boolean = false,
        var bounds: RectF = RectF()
    )

    // Danh sách các vùng cần tô màu
    private var regions = mutableListOf<Region>()
    // Map lưu trữ các vùng theo số
    private var regionsMap = mutableMapOf<Int, MutableList<Region>>()
    
    // Bitmap để vẽ màu
    private var workingBitmap: Bitmap? = null
    // Bitmap chứa hình nền (đường viền)
    private var backgroundBitmap: Bitmap? = null
    
    // Màu và số được chọn hiện tại
    private var selectedColor: Int = Color.TRANSPARENT
    private var selectedNumber: Int = -1
    
    // Các thông số về tỷ lệ và vị trí vẽ
    private var scaleFactor: Float = 1f
    private var drawRect = RectF()
    
    // Paint để vẽ viền highlight cho vùng được chọn
    private val highlightPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        setShadowLayer(2f, 0f, 0f, Color.argb(120, 0, 0, 0))
        isAntiAlias = true
    }
    private val regionPaths = mutableMapOf<String, MutableList<String>>()

    // Các biến để xử lý thao tác chạm và phóng to/thu nhỏ
    private var lastFocusX = 0f
    private var lastFocusY = 0f
    private var viewScaleFactor = 1f
    private var viewTranslateX = 0f
    private var viewTranslateY = 0f
    private var isZooming = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    init {
        // Bật hardware acceleration để tăng hiệu suất
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    private val scaleGestureDetector = ScaleGestureDetector(context, object : SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            isZooming = true
            lastFocusX = detector.focusX
            lastFocusY = detector.focusY
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            val focusX = detector.focusX
            val focusY = detector.focusY

            // Tính toán tỷ lệ scale mới
            var newScale = viewScaleFactor * scaleFactor
            newScale = newScale.coerceIn(MIN_SCALE, MAX_SCALE)

            // Tính toán điểm focus mới
            val focusShiftX = (focusX - lastFocusX) / viewScaleFactor
            val focusShiftY = (focusY - lastFocusY) / viewScaleFactor

            // Cập nhật vị trí và tỷ lệ
            viewTranslateX += focusShiftX * (1f - scaleFactor)
            viewTranslateY += focusShiftY * (1f - scaleFactor)

            // Update scale factor and focus point
            viewScaleFactor = newScale
            lastFocusX = focusX
            lastFocusY = focusY

            ViewCompat.postInvalidateOnAnimation(this@PaintCanvasView)
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            isZooming = false
        }
    })

    fun setTemplate(template: PaintTemplate) {
        Log.d(TAG, "Setting template with dimensions: ${template.width}x${template.height}")
        Log.d(TAG, "Number of regions: ${template.regions.size}")

        // Tạo bitmap mới cho việc vẽ
        val newBitmap = createBitmap(
            template.width,
            template.height,
            Config.ARGB_8888
        )
        // Xóa dữ liệu cũ
        workingBitmap?.recycle()
        regions.clear()
        regionsMap.clear()

        // Xử lý từng region trong template
        template.regions.mapIndexed { index, region ->
            region.cachedPath?.let { originalPath ->
                val path = Path()
                path.set(originalPath)    // pathData là chuỗi "M 415,318 L 43.....

                // Tính bounds cho region
                val bounds = RectF()
                path.computeBounds(bounds, true)
                
                Log.d(TAG, "Region ${region.number} (index $index):")
                Log.d(TAG, "  Path bounds: $bounds")
                // Tạo region mới
                val newRegion = Region(
                    path = path,
                    number = region.number,
                    currentColor = Color.TRANSPARENT,
                    isHighlighted = false,
                    bounds = bounds
                )

                // Thêm vào danh sách và map
                regions.add(newRegion)
                
                // Add to regionsMap
                if (!regionsMap.containsKey(region.number)) {
                    regionsMap[region.number] = mutableListOf()
                }
                regionsMap[region.number]?.add(newRegion)
                
                newRegion
            } ?: run {
                Log.w(TAG, "Region ${region.number} (index $index) has null cachedPath")
                null
            }
        }

        // Debug: Print region counts by number
        val countByNumber = regionsMap.mapValues { it.value.size }
        Log.d(TAG, "Region counts by number: $countByNumber")

        workingBitmap = newBitmap
        updateDrawingRect()
        invalidate()
    }
    //Thiết lập Background:
    fun setBackgroundBitmap(bitmap: Bitmap) {
        // Scale bitmap nếu kích thước không khớp
        val scaledBitmap = if (bitmap.width != workingBitmap?.width || bitmap.height != workingBitmap?.height) {
            Log.d(TAG, "Scaling background bitmap from ${bitmap.width}x${bitmap.height} to ${workingBitmap?.width}x${workingBitmap?.height}")
            createScaledBitmap(
                bitmap,
                workingBitmap?.width ?: bitmap.width,
                workingBitmap?.height ?: bitmap.height,
                true
            )
        } else {
            bitmap
        }
        
        backgroundBitmap = scaledBitmap
        updateDrawingRect()
        invalidate()
    }

    private fun updateDrawingRect() {
        if (workingBitmap == null) return
        
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val bitmapWidth = workingBitmap!!.width.toFloat()
        val bitmapHeight = workingBitmap!!.height.toFloat()
        
        // Calculate scale to fit bitmap in view while maintaining aspect ratio
        val scaleX = viewWidth / bitmapWidth
        val scaleY = viewHeight / bitmapHeight
        scaleFactor = Math.min(scaleX, scaleY)
        
        // Calculate centered position
        val scaledWidth = bitmapWidth * scaleFactor
        val scaledHeight = bitmapHeight * scaleFactor
        val left = (viewWidth - scaledWidth) / 2
        val top = (viewHeight - scaledHeight) / 2
            
        drawRect.set(left, top, left + scaledWidth, top + scaledHeight)
        Log.d(TAG, "Updated drawing rect: $drawRect, scaleFactor: $scaleFactor")
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateDrawingRect()
    }

//chọn màu
    fun setSelectedColor(color: Int, number: Int) {
        Log.d(TAG, "Selected color: ${String.format("#%06X", 0xFFFFFF and color)}, number: $number")
        selectedColor = color
        selectedNumber = number

    // Reset tất cả highlight
        regions.forEach { it.isHighlighted = false }

    // Highlight các vùng chưa tô có cùng số
        regionsMap[number]?.forEach { region ->
            // Chỉ highlight các vùng chưa tô
            region.isHighlighted = region.currentColor == Color.TRANSPARENT
        }

        // Debug: Print highlighted regions
        val highlightedCount = regions.count { it.isHighlighted }
        Log.d(TAG, "Number of highlighted regions for number $number: $highlightedCount")
        
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Apply view transformation
        canvas.save()
        canvas.translate(viewTranslateX, viewTranslateY)
        canvas.scale(viewScaleFactor, viewScaleFactor)
        
        workingBitmap?.let { bitmap ->
            // Draw background bitmap if available
            backgroundBitmap?.let { bg ->
                canvas.drawBitmap(bg, null, drawRect, null)
        }

            // Vẽ các vùng đã tô
            canvas.drawBitmap(bitmap, null, drawRect, null)

            // Vẽ highlight
            canvas.save()
            canvas.translate(drawRect.left, drawRect.top)
            canvas.scale(scaleFactor, scaleFactor)
            
            regions.forEach { region ->
                if (region.isHighlighted) {
                    canvas.drawPath(region.path, highlightPaint)
                }
            }
            
            canvas.restore()
        }
        
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!isZooming) {
                    lastTouchX = event.x
                    lastTouchY = event.y
                    isDragging = true

                    // Chuyển đổi tọa độ màn hình sang tọa độ bitmap
                    val bitmapX = ((event.x - viewTranslateX) / viewScaleFactor - drawRect.left) / scaleFactor
                    val bitmapY = ((event.y - viewTranslateY) / viewScaleFactor - drawRect.top) / scaleFactor
                    
                    Log.d(TAG, "Touch event received at screen coordinates: (${event.x}, ${event.y})")
                    Log.d(TAG, "Mapped to bitmap coordinates: ($bitmapX, $bitmapY)")
                    
                fillRegion(bitmapX, bitmapY)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isZooming && isDragging && viewScaleFactor > 1f) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    
                    viewTranslateX += dx
                    viewTranslateY += dy
                    
                    lastTouchX = event.x
                    lastTouchY = event.y
                    
                    ViewCompat.postInvalidateOnAnimation(this)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
        }
        
            return true
    }

    private fun Path.isPointInPath(x: Float, y: Float, storedBounds: RectF): Boolean {
        // First check stored bounds
        if (!storedBounds.contains(x, y)) {
            return false
        }
        
        // Then do precise hit testing
        val region = Region()
        val boundsInt = Region(
            storedBounds.left.toInt(),
            storedBounds.top.toInt(),
            storedBounds.right.toInt(),
            storedBounds.bottom.toInt()
        )
        
        val pathRegion = Region()
        pathRegion.setPath(this, boundsInt)
        
        return pathRegion.contains(x.toInt(), y.toInt())
    }

//Kiểm tra hoàn thành toàn bộ chưa
    private fun checkCompletion() {
        // Kiểm tra xem tất cả các vùng đã được tô màu chưa
        val isCompleted = regions.all { it.currentColor != Color.TRANSPARENT }
        
        if (isCompleted && !isShowingCompletionAnimation) {
            isShowingCompletionAnimation = true
            
            try {
                // Tạo bitmap hoàn chỉnh với nền trắng và kích thước gốc
            val completedBitmap = createBitmap(
                workingBitmap!!.width,
                workingBitmap!!.height,
                Config.ARGB_8888
                ).apply {
                    eraseColor(Color.WHITE) // Đặt nền trắng
                }
                
            val canvas = Canvas(completedBitmap)
            
                // Vẽ các vùng đã tô với màu đầy đủ
                workingBitmap?.let { bitmap ->
                    val paint = Paint().apply {
                        isAntiAlias = true
                        isDither = true
                        isFilterBitmap = true
                        alpha = 255 // Đảm bảo độ đục hoàn toàn
                        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
                    }
                    canvas.drawBitmap(bitmap, 0f, 0f, paint)
                }
            
                // Vẽ đường viền đè lên với màu đen
                backgroundBitmap?.let { outline ->
                    val paint = Paint().apply {
                        isAntiAlias = true
                        isDither = true
                        isFilterBitmap = true
                        alpha = 255 // Đảm bảo độ đục hoàn toàn
                        xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
                    }
                    canvas.drawBitmap(outline, 0f, 0f, paint)
                }

                // Log để debug
                Log.d(TAG, "Created completed bitmap: ${completedBitmap.width}x${completedBitmap.height}")
                Log.d(TAG, "Bitmap config: ${completedBitmap.config}")
            
            // Thông báo hoàn thành với bitmap đã tô
                onPaintingCompletedListener?.onPaintingCompleted(completedBitmap.copy(Config.ARGB_8888, true))
            
            // Hiển thị animation hoàn thành
            showCompletionAnimation()
            } catch (e: Exception) {
                Log.e(TAG, "Error creating completed bitmap: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    //show hoàn thành
    private fun showCompletionAnimation() {
        isShowingCompletionAnimation = true

        // Container chính với nền mờ
        val completionContainer = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#80000000"))
        }

        // Container cho nội dung chính, đặt ở giữa màn hình
        val contentContainer = LinearLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            alpha = 0f
            setPadding(32, 32, 32, 32)
            setBackgroundResource(android.R.drawable.dialog_holo_dark_frame)
        }

        // TextView "Tuyệt vời!"
        val messageView = TextView(context).apply {
            text = "Tuyệt vời!"
            setTextColor(Color.WHITE)
            textSize = 48f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setShadowLayer(10f, 0f, 2f, Color.BLACK)
            setPadding(48, 32, 48, 32)
        }

        // Icon trophy
        val trophyIcon = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_dialog_info)
            layoutParams = LinearLayout.LayoutParams(160, 160).apply {
                gravity = Gravity.CENTER
                topMargin = 32
                bottomMargin = 48
            }
            setColorFilter(Color.YELLOW)
        }

        // Container cho các nút
        val buttonContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                topMargin = 16
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(16, 0, 16, 16)
        }

        // Nút "Chơi lại"
        val replayButton = MaterialButton(context).apply {
            text = "Chơi lại"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 16
            }
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            cornerRadius = 20
            elevation = 8f
            setPadding(32, 16, 32, 16)
            minimumWidth = 200
        }

        // Nút "Hoàn thành"
        val completeButton = MaterialButton(context).apply {
            text = "Hoàn thành"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 16
            }
            setBackgroundColor(Color.parseColor("#2196F3"))
            setTextColor(Color.WHITE)
            cornerRadius = 20
            elevation = 8f
            setPadding(32, 16, 32, 16)
            minimumWidth = 200
        }

        // Click listeners
        replayButton.setOnClickListener {
            resetCanvas()
            (parent as? ViewGroup)?.removeView(completionContainer)
            isShowingCompletionAnimation = false
        }

        completeButton.setOnClickListener {
            workingBitmap?.let { bitmap ->
                onPaintingCompletedListener?.onPaintingCompleted(bitmap)
            }
        }

        // Thêm các view vào container
        buttonContainer.addView(replayButton)
        buttonContainer.addView(completeButton)

        contentContainer.addView(messageView)
        contentContainer.addView(trophyIcon)
        contentContainer.addView(buttonContainer)

        // Đặt contentContainer vào giữa completionContainer
        completionContainer.addView(contentContainer, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        })

        // Thêm vào view cha
        (parent as? ViewGroup)?.addView(completionContainer)

        // Animation
        val scaleX = ObjectAnimator.ofFloat(contentContainer, "scaleX", 0f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(contentContainer, "scaleY", 0f, 1.2f, 1f)
        val alpha = ObjectAnimator.ofFloat(contentContainer, "alpha", 0f, 1f)

        // Hiệu ứng confetti
        val confettiManager = ConfettiManager(context)
            .setVelocityX(0, 1000)
            .setVelocityY(-1000, -400)
            .setRotationalVelocity(180, 90)
            .setEmissionDuration(1000)
            .setEmissionRate(100)
            .animate(completionContainer)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 1000
            interpolator = OvershootInterpolator()
            start()
        }
    }

    private fun resetCanvas() {
        // Reset all regions to their original state
        regions.forEach { region ->
            region.currentColor = Color.TRANSPARENT
            region.isHighlighted = false
        }
        
        // Clear the working bitmap
        workingBitmap?.let { bitmap ->
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        }
        
        // Reset the selected color and number
        selectedColor = Color.TRANSPARENT
        selectedNumber = -1
        
        invalidate()
    }
//Kiểm tra hoàn thành một màu
    private fun checkColorCompletion(number: Int) {
        val regionsForNumber = regionsMap[number] ?: return
        var allRegionsFilled = true
        var filledCount = 0
        
        regionsForNumber.forEach { region ->
            if (region.currentColor == Color.TRANSPARENT) {
                allRegionsFilled = false
            } else {
                filledCount++
            }
        }

        Log.d(TAG, "Color $number completion check: $filledCount/${regionsForNumber.size} regions filled")

        if (allRegionsFilled) {
            Log.d(TAG, "All regions for color number $number are filled!")
            onColorCompletedListener?.onColorCompleted(number)
        }
    }

    fun fillAllHighlightedRegions(): Boolean {
        if (selectedNumber == -1 || selectedColor == Color.TRANSPARENT) {
            return false
        }

        var filledAny = false
        val regionsForNumber = regionsMap[selectedNumber] ?: return false

        workingBitmap?.let { bitmap ->
            val canvas = Canvas(bitmap)

            regionsForNumber.forEach { region ->
                if (region.isHighlighted) {
                    // Tô màu cho vùng
            val paint = Paint().apply {
                color = selectedColor
                style = Paint.Style.FILL
                isAntiAlias = true
            }
                    canvas.drawPath(region.path, paint)

                    // Cập nhật trạng thái vùng
                    region.currentColor = selectedColor
                    region.isHighlighted = false
                    filledAny = true
                }
            }

            if (filledAny) {
            invalidate()
                // Kiểm tra xem màu hiện tại đã tô hết chưa
                checkColorCompletion(selectedNumber)
                // Kiểm tra xem đã tô hết hình chưa
                checkCompletion()
            }
        }

        return filledAny
    }

    private fun fillRegion(x: Float, y: Float) {
        if (selectedNumber == -1 || selectedColor == Color.TRANSPARENT) {
            return
        }

        val regionsForNumber = regionsMap[selectedNumber] ?: return
        
        // Create a Region to test point containment
        val region = regionsForNumber.find { highlightedRegion ->
            if (!highlightedRegion.isHighlighted) return@find false
            
            val bounds = RectF()
            highlightedRegion.path.computeBounds(bounds, true)
            val region = Region()
            region.setPath(
                highlightedRegion.path,
                Region(
                    bounds.left.toInt(),
                    bounds.top.toInt(),
                    bounds.right.toInt(),
                    bounds.bottom.toInt()
                )
            )
            region.contains(x.toInt(), y.toInt())
        }
        
        region?.let {
            workingBitmap?.let { bitmap ->
                // Tạo canvas để vẽ
                val canvas = Canvas(bitmap)
                
                // Cấu hình paint cho việc tô màu
                val paint = Paint().apply {
                    color = selectedColor
                    style = Paint.Style.FILL
                    isAntiAlias = true
                    isDither = true
                    alpha = 255 // Đảm bảo màu không trong suốt
                }
                
                // Tô màu cho vùng được chọn
                canvas.drawPath(it.path, paint)
                it.currentColor = selectedColor
                it.isHighlighted = false
                
                // Thông báo vùng đã được tô thủ công
                onColorCompletedListener?.onRegionFilled()
                
                // Tô màu cho các vùng có cùng đường viền
                regionsForNumber.forEach { r ->
                    val bounds1 = RectF()
                    val bounds2 = RectF()
                    it.path.computeBounds(bounds1, true)
                    r.path.computeBounds(bounds2, true)
                    
                    if (bounds1 == bounds2 && r != it) {
                        canvas.drawPath(r.path, paint)
                        r.currentColor = selectedColor
                        r.isHighlighted = false
                    }
                }
                
                Log.d(TAG, "Filled region ${it.number} with color ${String.format("#%08X", selectedColor)}")
                
                invalidate()
                
                // Kiểm tra xem màu hiện tại đã tô hết chưa
                checkColorCompletion(selectedNumber)
                
                // Kiểm tra hoàn thành
                checkCompletion()
            }
        } ?: Log.d(TAG, "No matching highlighted region found at click point ($x, $y)")
    }

    fun getCurrentBitmap(): Bitmap {
        // Tạo bitmap mới với nền trắng
        val currentBitmap = createBitmap(
            workingBitmap!!.width,
            workingBitmap!!.height,
            Config.ARGB_8888
        ).apply {
            eraseColor(Color.WHITE)
        }

        // Vẽ trạng thái hiện tại
        val canvas = Canvas(currentBitmap)
        
        // Vẽ các vùng đã tô
        workingBitmap?.let { bitmap ->
            val paint = Paint().apply {
                isAntiAlias = true
                isDither = true
                isFilterBitmap = true
                alpha = 255
            }
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
        }

        // Vẽ đường viền
        backgroundBitmap?.let { outline ->
            val paint = Paint().apply {
                isAntiAlias = true
                isDither = true
                isFilterBitmap = true
                alpha = 255
                xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
            }
            canvas.drawBitmap(outline, 0f, 0f, paint)
        }

        return currentBitmap
    }

    /**
     * Load trạng thái tô màu từ bitmap đã lưu
     */
    fun loadProgress(progressBitmap: Bitmap) {
        // Đảm bảo kích thước bitmap phù hợp
        if (workingBitmap == null || progressBitmap.width != workingBitmap!!.width || progressBitmap.height != workingBitmap!!.height) {
            Log.e(TAG, "Progress bitmap size mismatch or working bitmap not initialized")
            return
        }

        // Tạo canvas để vẽ lên working bitmap
        val canvas = Canvas(workingBitmap!!)
        val paint = Paint().apply {
            isAntiAlias = true
            isDither = true
            isFilterBitmap = true
            alpha = 255
        }
        
        // Vẽ bitmap tiến trình lên working bitmap
        canvas.drawBitmap(progressBitmap, 0f, 0f, paint)
        
        // Cập nhật trạng thái các vùng đã tô
        for (region in regions) {
            val bounds = region.bounds
            
            // Lấy điểm trung tâm của vùng
            val centerX = bounds.centerX().toInt()
            val centerY = bounds.centerY().toInt()
            
            // Kiểm tra màu tại điểm trung tâm
            if (centerX >= 0 && centerX < workingBitmap!!.width && 
                centerY >= 0 && centerY < workingBitmap!!.height) {
                val color = progressBitmap.getPixel(centerX, centerY)
                if (color != Color.TRANSPARENT && color != Color.WHITE) {
                    region.currentColor = color
                    
                    // Cập nhật regionsMap
                    regionsMap[region.number]?.forEach { matchingRegion ->
                        if (matchingRegion.bounds == region.bounds) {
                            matchingRegion.currentColor = color
                        }
                    }
                }
            }
        }
        
        // Yêu cầu vẽ lại view
        invalidate()
        
        Log.d(TAG, "Progress loaded successfully")
        Log.d(TAG, "Regions updated: ${regions.count { it.currentColor != Color.TRANSPARENT }}")
    }
} 