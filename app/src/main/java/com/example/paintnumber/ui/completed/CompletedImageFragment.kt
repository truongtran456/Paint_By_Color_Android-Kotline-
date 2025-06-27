package com.example.paintnumber.ui.completed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.paintnumber.databinding.FragmentCompletedImageBinding
import java.io.File

class CompletedImageFragment : Fragment() {
    private var _binding: FragmentCompletedImageBinding? = null
    private val binding get() = _binding!!
    private val args: CompletedImageFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompletedImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load and display the completed image
        Glide.with(this)
            .load(File(args.imagePath))
            .into(binding.completedImageView)

        // Handle back button
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 