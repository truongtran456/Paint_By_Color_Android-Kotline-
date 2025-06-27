package com.example.paintnumber.ui.sketch

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.paintnumber.databinding.FragmentSketchLoadingBinding

class SketchLoadingFragment : Fragment() {

    private var _binding: FragmentSketchLoadingBinding? = null
    private val binding get() = _binding!!
    
    private val args: SketchLoadingFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSketchLoadingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Load the sketch image
        binding.sketchImage.setImageResource(args.lineArtResId)
        
        // Simulate loading for 3 seconds then navigate to coloring screen
        Handler(Looper.getMainLooper()).postDelayed({
            findNavController().navigate(
                SketchLoadingFragmentDirections.actionSketchLoadingToPaint(
                    imageId = args.imageId,
                    lineArtResId = args.lineArtResId,
                    svgResId = args.svgResId,
                    progressPath = args.progressPath
                )
            )
        }, 3000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 