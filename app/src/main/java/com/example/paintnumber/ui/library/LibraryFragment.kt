package com.example.paintnumber.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.paintnumber.R
import com.example.paintnumber.databinding.FragmentLibraryBinding
import com.google.android.material.tabs.TabLayout
import androidx.core.os.bundleOf
import org.json.JSONObject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import android.util.Log
import android.widget.Toast
import java.io.File
import com.example.paintnumber.data.models.PaintImage

class LibraryFragment : Fragment() {
    
    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: LibraryViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LibraryViewModel(requireActivity().application) as T
            }
        }
    }
    private lateinit var bannerAdapter: BannerAdapter
    private lateinit var imageAdapter: ImageAdapter

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupBannerViewPager()
        setupTabLayout()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupBannerViewPager() {
        bannerAdapter = BannerAdapter()
        binding.bannerViewPager.apply {
            adapter = bannerAdapter
            orientation = ViewPager2.ORIENTATION_HORIZONTAL

            // Add page transform animation
            setPageTransformer { page, position ->
                page.apply {
                    val r = 1 - kotlin.math.abs(position)
                    page.scaleY = 0.85f + r * 0.15f
                }
            }
        }
    }

    private fun setupTabLayout() {
        // Thêm các tab
        categories.forEach { category ->
            binding.categoryTabs.addTab(
                binding.categoryTabs.newTab().setText(category)
            )
        }
        
        // Xử lý sự kiện khi chọn tab
        binding.categoryTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val position = tab?.position ?: 0
                viewModel.onCategorySelected(position)
                // Scroll tab được chọn vào giữa nếu có thể
                tab?.let { scrollTabToCenter(it) }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun scrollTabToCenter(tab: TabLayout.Tab) {
        val tabView = tab.view
        binding.categoryTabs.parent.requestChildFocus(binding.categoryTabs, tabView)
    }

    private fun setupRecyclerView() {
        imageAdapter = ImageAdapter { paintImage ->
            try {
                val imageId = paintImage.id.split("_").last()
                val lineArtResId = resources.getIdentifier(
                    "image_${imageId}_line_art",
                    "raw",
                    requireContext().packageName
                )
                val svgResId = resources.getIdentifier(
                    "image_$imageId",
                    "raw",
                    requireContext().packageName
                )
                
                // Nếu ảnh đã hoàn thành, hiển thị dialog để chọn xem hoặc chơi lại
                if (paintImage.isCompleted && paintImage.completedImagePath != null) {
                    showCompletedImageOptions(paintImage, lineArtResId, svgResId)
                    return@ImageAdapter
                }

                // Nếu chưa hoàn thành, chuyển đến màn hình tô màu
                if (lineArtResId != 0 && svgResId != 0) {
                    navigateToPaintScreen(paintImage.id, lineArtResId, svgResId)
                } else {
                    Log.e("LibraryFragment", "Failed to find resources for ${paintImage.id}")
                    Toast.makeText(context, "Không thể tải hình ảnh", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("LibraryFragment", "Error navigating to paint screen: ${e.message}")
                e.printStackTrace()
                Toast.makeText(context, "Có lỗi xảy ra", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = imageAdapter
        }
    }

    private fun showCompletedImageOptions(paintImage: PaintImage, lineArtResId: Int, svgResId: Int) {
        val options = arrayOf("Xem ảnh đã hoàn thành", "Chơi lại")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Bạn muốn làm gì?")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { // Xem ảnh đã hoàn thành
                        findNavController().navigate(
                            LibraryFragmentDirections.actionLibraryToCompletedImage(
                                imagePath = paintImage.completedImagePath!!
                            )
                        )
                    }
                    1 -> { // Chơi lại
                        navigateToPaintScreen(paintImage.id, lineArtResId, svgResId)
                    }
                }
            }
            .show()
    }

    private fun navigateToPaintScreen(imageId: String, lineArtResId: Int, svgResId: Int) {
        findNavController().navigate(
            LibraryFragmentDirections.actionLibraryToSketchLoading(
                imageId = imageId,
                lineArtResId = lineArtResId,
                svgResId = svgResId
            )
        )
    }

    private fun observeViewModel() {
        viewModel.banners.observe(viewLifecycleOwner) { banners ->
            bannerAdapter.submitList(banners)
        }

        viewModel.images.observe(viewLifecycleOwner) { images ->
            imageAdapter.submitList(images)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.recyclerView.isVisible = !isLoading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 