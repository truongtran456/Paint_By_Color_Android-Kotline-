package com.example.paintnumber.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.paintnumber.databinding.FragmentGalleryListBinding
import com.example.paintnumber.ui.library.ImageAdapter

class GalleryCompletedFragment : Fragment() {
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
            if (paintImage.completedImagePath != null) {
                findNavController().navigate(
                    GalleryFragmentDirections.actionGalleryToCompletedImage(
                        imagePath = paintImage.completedImagePath!!
                    )
                )
            }
        }

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = imageAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.completedImages.observe(viewLifecycleOwner) { images ->
            imageAdapter.submitList(images)
            binding.emptyText.visibility = if (images.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 