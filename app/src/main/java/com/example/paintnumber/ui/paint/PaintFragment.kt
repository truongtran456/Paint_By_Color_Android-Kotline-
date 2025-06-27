package com.example.paintnumber.ui.paint

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Path
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.paintnumber.R
import com.example.paintnumber.databinding.FragmentPaintBinding
import com.example.paintnumber.utils.SVGParser
import com.example.paintnumber.models.PaintColor
import com.example.paintnumber.data.CompletedPaintingsManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

class PaintFragment : Fragment(), PaintCanvasView.OnPaintingCompletedListener, PaintCanvasView.OnColorCompletedListener {
    companion object {
        private const val TAG = "PaintFragment"
        private const val INITIAL_HINT_COUNT = 0
        private const val REGIONS_FOR_HINT = 10
    }
    
    private var _binding: FragmentPaintBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var colorAdapter: ColorPaletteAdapter
    private val args: PaintFragmentArgs by navArgs()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private val colors = mutableListOf<PaintColor>()
    private lateinit var completedPaintingsManager: CompletedPaintingsManager
    
    private var hintCount = INITIAL_HINT_COUNT
    private var manuallyFilledRegionsCount = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaintBinding.inflate(inflater, container, false)
        completedPaintingsManager = CompletedPaintingsManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Hide bottom navigation
        activity?.findViewById<BottomNavigationView>(R.id.nav_view)?.visibility = View.GONE
        
        setupColorPalette()
        setupButtons()

        binding.paintCanvas.onPaintingCompletedListener = this
        binding.paintCanvas.onColorCompletedListener = this

        // Xử lý nút back
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        // Setup back button
        binding.backButton.setOnClickListener {
            showExitConfirmationDialog()
        }

        // Load template và progress
        loadTemplateAndProgress()
    }

    private fun setupColorPalette() {
        colorAdapter = ColorPaletteAdapter { color, number ->
            binding.paintCanvas.setSelectedColor(color, number)
        }
        binding.colorPalette.adapter = colorAdapter
    }

    private fun loadTemplateAndProgress() {
        showLoading(true)

        Log.d(TAG, "Starting to load template. LineArtResId: ${args.lineArtResId}, SvgResId: ${args.svgResId}")
        
        // Check if resource exists
        try {
            resources.getResourceName(args.lineArtResId)
            Log.d(TAG, "Line art resource exists")
        } catch (e: Exception) {
            Log.e(TAG, "Line art resource not found: ${e.message}")
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            try {
                // Load line art bitmap and SVG template in parallel
                val (lineArtBitmap, template) = withContext(Dispatchers.IO) {
                    val bitmap = BitmapFactory.decodeResource(resources, args.lineArtResId)
                    Log.d(TAG, "Line art bitmap loaded successfully: ${bitmap.width}x${bitmap.height}")
                    Log.d(TAG, "Bitmap config: ${bitmap.config}")

                    val template = resources.openRawResource(args.svgResId).use { inputStream ->
                        SVGParser().parseTemplate(inputStream)
                    }
                    
                    Pair(bitmap, template)
                }

                withContext(Dispatchers.Main) {
                    if (template == null) {
                        Log.e(TAG, "Template is null")
                        showError("Failed to load template")
                        return@withContext
                    }

                    try {
                        Log.d(TAG, "Template loaded successfully with ${template.regions.size} regions")
                        Log.d(TAG, "Template dimensions: ${template.width}x${template.height}")
                        
                        // Update template name
                        binding.templateName.text = template.name
                        
                        // Set the template for color regions first
                        binding.paintCanvas.setTemplate(template)
                        Log.d(TAG, "Template set to canvas")
                        
                        // Then set the numbered line art as background
                        binding.paintCanvas.setBackgroundBitmap(lineArtBitmap)
                        Log.d(TAG, "Background bitmap set to canvas")
                        
                        // Update color palette with unique colors
                        val templateColors = template.regions
                            .filter { region -> !region.isBackground }
                            .distinctBy { region -> region.number }
                            .sortedBy { region -> region.number }
                            .map { region -> 
                                val colorWithAlpha = region.targetColor or 0xFF000000.toInt()
                                PaintColor(colorWithAlpha, region.number) 
                            }
                        
                        colors.clear()
                        colors.addAll(templateColors)
                        colorAdapter.submitList(templateColors)
                        Log.d(TAG, "Color palette updated with ${templateColors.size} colors")
                        
                        // Load progress if available
                        args.progressPath?.let { path ->
                            try {
                                val progressFile = File(path.toString())
                                if (progressFile.exists()) {
                                    val progressBitmap = BitmapFactory.decodeFile(progressFile.absolutePath)
                                    binding.paintCanvas.loadProgress(progressBitmap)
                                    Log.d(TAG, "Loaded progress from: $path")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error loading progress: ${e.message}")
                                Toast.makeText(context, "Không thể tải tiến trình", Toast.LENGTH_SHORT).show()
                            }
                        }
                        
                        // Show the canvas and color palette
                        showLoading(false)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting up template: ${e.message}")
                        e.printStackTrace()
                        showError("Error setting up template")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading template: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showError("Error loading template")
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.loadingContainer.visibility = if (show) View.VISIBLE else View.GONE
        binding.paintCanvas.visibility = if (show) View.GONE else View.VISIBLE
        binding.colorPaletteContainer.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        binding.loadingContainer.visibility = View.GONE
        binding.paintCanvas.visibility = View.GONE
        binding.colorPaletteContainer.visibility = View.GONE
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun setupButtons() {
        binding.backButton.setOnClickListener {
            showExitConfirmationDialog()
        }
//tô màu gợi ý
        binding.hintButton.setOnClickListener {
            if (hintCount > 0) {
                val success = binding.paintCanvas.fillAllHighlightedRegions()
                if (success) {
                    hintCount--
                    updateHintCount()
                }
            } else {
                Toast.makeText(context, "Không còn gợi ý!", Toast.LENGTH_SHORT).show()
            }
        }

        updateHintCount()
    }

    private fun updateHintCount() {
        binding.hintCount.text = hintCount.toString()
        binding.hintButton.isEnabled = hintCount > 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancel any ongoing coroutines
        coroutineScope.cancel()
        // Show bottom navigation when leaving
        activity?.findViewById<BottomNavigationView>(R.id.nav_view)?.visibility = View.VISIBLE
        _binding = null
    }

    //lưu ảnh đã hoàn thành
    override fun onPaintingCompleted(completedBitmap: Bitmap) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Tạo thư mục nếu chưa tồn tại
                val completedDir = File(requireContext().filesDir, "completed_paintings")
                if (!completedDir.exists()) {
                    completedDir.mkdirs()
                }

                // Lưu bitmap vào file với chất lượng cao nhất và không nén
                val imageFile = File(completedDir, "${args.imageId}.png")
                FileOutputStream(imageFile).use { out ->
                    // Đảm bảo bitmap không bị mất alpha channel và chất lượng
                    completedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                }

                // Log để debug
                Log.d(TAG, "Saved completed painting to: ${imageFile.absolutePath}")
                Log.d(TAG, "File exists: ${imageFile.exists()}")
                Log.d(TAG, "File size: ${imageFile.length()} bytes")

                // Lưu trạng thái hoàn thành vào SharedPreferences
                completedPaintingsManager.markAsCompleted(args.imageId, imageFile.absolutePath)

                withContext(Dispatchers.Main) {
                    // Chuyển đến màn hình congratulation
                    findNavController().navigate(
                        PaintFragmentDirections.actionPaintToCongratulation(
                            imagePath = imageFile.absolutePath
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving completed painting: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Không thể lưu tranh đã hoàn thành", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    // Xóa màu khỏi danh sách
    override fun onColorCompleted(number: Int) {
        // Tìm vị trí của màu trong danh sách
        val currentList = colors.toMutableList()
        val position = currentList.indexOfFirst { it.number == number }
        
        if (position != -1) {
            // Xóa màu khỏi danh sách
            currentList.removeAt(position)
            colors.clear()
            colors.addAll(currentList)
            
            // Cập nhật RecyclerView với danh sách mới
            colorAdapter.submitList(currentList) {
                // Callback sau khi danh sách đã được cập nhật
                if (currentList.isNotEmpty()) {
                    // Chọn màu tiếp theo
                    val nextPosition = if (position < currentList.size) position else currentList.size - 1
                    val nextColor = currentList[nextPosition]
                    binding.paintCanvas.setSelectedColor(nextColor.color, nextColor.number)
                }
            }
        }
    }

    override fun onRegionFilled() {
        manuallyFilledRegionsCount++
        Log.d(TAG, "Manually filled regions: $manuallyFilledRegionsCount")
        
        if (manuallyFilledRegionsCount >= REGIONS_FOR_HINT) {
            manuallyFilledRegionsCount = 0
            hintCount++
            updateHintCount()
            Toast.makeText(context, "Bạn nhận được 1 gợi ý!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProgress() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Lấy bitmap hiện tại từ canvas
                val currentBitmap = binding.paintCanvas.getCurrentBitmap()
                
                // Tạo thư mục nếu chưa tồn tại
                val progressDir = File(requireContext().filesDir, "progress_paintings")
                if (!progressDir.exists()) {
                    progressDir.mkdirs()
                }

                // Lưu bitmap vào file
                val progressFile = File(progressDir, "${args.imageId}_progress.png")
                FileOutputStream(progressFile).use { out ->
                    currentBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                }

                // Lưu đường dẫn vào SharedPreferences
                completedPaintingsManager.saveProgressPath(args.imageId, progressFile.absolutePath)

                Log.d(TAG, "Saved progress to: ${progressFile.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving progress: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Không thể lưu tiến trình", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showExitConfirmationDialog() {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Thoát")
            .setMessage("Bạn có muốn lưu tiến trình trước khi thoát không?")
            .setPositiveButton("Lưu và Thoát") { _, _ ->
                saveProgress()
                findNavController().navigateUp()
            }
            .setNegativeButton("Thoát không lưu") { _, _ ->
                findNavController().navigateUp()
            }
            .setNeutralButton("Hủy", null)
            .create()
        dialog.show()
    }
} 