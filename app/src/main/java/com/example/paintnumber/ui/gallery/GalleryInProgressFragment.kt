package com.example.paintnumber.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.paintnumber.databinding.FragmentGalleryListBinding
import com.example.paintnumber.ui.library.ImageAdapter

class GalleryInProgressFragment : Fragment() {
    private var _binding: FragmentGalleryListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GalleryViewModel by viewModels({ requireParentFragment() })
    private lateinit var imageAdapter: ImageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        imageAdapter = ImageAdapter { paintImage ->
            // Lấy ID ảnh từ đường dẫn file
            val imageId = paintImage.id
            val context = requireContext()
            
            // Tìm resource ID cho ảnh outline và svg
            val outlineResId = context.resources.getIdentifier(
                "${imageId}_line_art",
                "raw",
                context.packageName
            )
            val svgResId = context.resources.getIdentifier(
                imageId,
                "raw",
                context.packageName
            )

            if (outlineResId != 0 && svgResId != 0) {
                findNavController().navigate(
                    GalleryFragmentDirections.actionGalleryToSketchLoading(
                        imageId = imageId,
                        lineArtResId = outlineResId,
                        svgResId = svgResId,
                        progressPath = paintImage.progressPath
                    )
                )
            } else {
                Toast.makeText(context, "Không thể tải hình ảnh", Toast.LENGTH_SHORT).show()
            }
        }

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = imageAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.inProgressImages.observe(viewLifecycleOwner) { images ->
            imageAdapter.submitList(images)
            binding.emptyText.visibility = if (images.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 