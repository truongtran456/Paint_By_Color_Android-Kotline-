package com.example.paintnumber.ui.congratulation

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.paintnumber.data.CompletedPaintingsManager
import com.example.paintnumber.databinding.FragmentCongratulationBinding
import com.example.paintnumber.utils.ConfettiManager
import java.io.File
import java.io.FileOutputStream

class CongratulationFragment : Fragment() {
    private var _binding: FragmentCongratulationBinding? = null
    private val binding get() = _binding!!
    private val args: CongratulationFragmentArgs by navArgs()
    private lateinit var completedPaintingsManager: CompletedPaintingsManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCongratulationBinding.inflate(inflater, container, false)
        completedPaintingsManager = CompletedPaintingsManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load completed image with high quality settings
        Glide.with(this)
            .load(File(args.imagePath))
            .skipMemoryCache(true) // Không cache để luôn load ảnh mới nhất
            .diskCacheStrategy(DiskCacheStrategy.NONE) // Không cache ở disk
            .into(binding.completedImageView)

        // Log để debug
        Log.d("CongratulationFragment", "Loading image from: ${args.imagePath}")
        val imageFile = File(args.imagePath)
        Log.d("CongratulationFragment", "File exists: ${imageFile.exists()}")
        Log.d("CongratulationFragment", "File size: ${imageFile.length()} bytes")

        // Setup back button
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        // Setup download button
        binding.downloadButton.setOnClickListener {
            saveImageToGallery()
        }

        // Setup share button
        binding.shareButton.setOnClickListener {
            showShareOptions()
        }

        // Load completed images
        loadCompletedImages()

        // Start animations
        startCongratulationAnimations()
    }

    private fun startCongratulationAnimations() {
        // Animate title
        val titleScaleX = ObjectAnimator.ofFloat(binding.congratulationTitle, "scaleX", 0f, 1.2f, 1f)
        val titleScaleY = ObjectAnimator.ofFloat(binding.congratulationTitle, "scaleY", 0f, 1.2f, 1f)
        val titleAlpha = ObjectAnimator.ofFloat(binding.congratulationTitle, "alpha", 0f, 1f)

        // Animate subtitle
        val subtitleTranslateY = ObjectAnimator.ofFloat(binding.congratulationSubtitle, "translationY", 100f, 0f)
        val subtitleAlpha = ObjectAnimator.ofFloat(binding.congratulationSubtitle, "alpha", 0f, 1f)

        // Animate image container
        val imageContainerScaleX = ObjectAnimator.ofFloat(binding.imageContainer, "scaleX", 0.5f, 1f)
        val imageContainerScaleY = ObjectAnimator.ofFloat(binding.imageContainer, "scaleY", 0.5f, 1f)
        val imageContainerAlpha = ObjectAnimator.ofFloat(binding.imageContainer, "alpha", 0f, 1f)

        // Animate buttons
        val buttonsTranslateY = ObjectAnimator.ofFloat(binding.buttonContainer, "translationY", 100f, 0f)
        val buttonsAlpha = ObjectAnimator.ofFloat(binding.buttonContainer, "alpha", 0f, 1f)

        // Create animation set
        AnimatorSet().apply {
            playTogether(
                titleScaleX, titleScaleY, titleAlpha,
                subtitleTranslateY, subtitleAlpha,
                imageContainerScaleX, imageContainerScaleY, imageContainerAlpha,
                buttonsTranslateY, buttonsAlpha
            )
            duration = 1000
            interpolator = OvershootInterpolator()
            start()
        }

        // Add confetti animation
        ConfettiManager(requireContext())
            .setVelocityX(-1000, 1000)
            .setVelocityY(-1000, -400)
            .setRotationalVelocity(-180, 180)
            .setEmissionDuration(2000)
            .setEmissionRate(100)
            .animate(binding.root as ViewGroup)
    }
//save image
    private fun saveImageToGallery() {
        try {
            val sourceFile = File(args.imagePath)
            val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath)
            
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val destFile = File(imagesDir, "PaintByNumber_${System.currentTimeMillis()}.png")
            
            FileOutputStream(destFile).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
            
            // Notify gallery
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val contentUri = Uri.fromFile(destFile)
            mediaScanIntent.data = contentUri
            requireContext().sendBroadcast(mediaScanIntent)
            
            Toast.makeText(context, "Đã lưu ảnh vào thư viện", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Không thể lưu ảnh", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun showShareOptions() {
        try {
            val imageFile = File(args.imagePath)
            
            // Log file information
            Log.d("CongratulationFragment", "Sharing image from path: ${args.imagePath}")
            Log.d("CongratulationFragment", "File exists: ${imageFile.exists()}")
            Log.d("CongratulationFragment", "File size: ${imageFile.length()} bytes")
            Log.d("CongratulationFragment", "File can read: ${imageFile.canRead()}")
            
            val contentUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                imageFile
            )
            
            // Log content URI
            Log.d("CongratulationFragment", "Content URI: $contentUri")

            // Create share intent for specific apps
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Create chooser with custom title
            val chooserIntent = Intent.createChooser(shareIntent, "Chia sẻ tác phẩm qua")

            // Start activity with chooser
            startActivity(chooserIntent)
        } catch (e: Exception) {
            Log.e("CongratulationFragment", "Error sharing image", e)
            Toast.makeText(context, "Không thể chia sẻ ảnh", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun loadCompletedImages() {
        val completedImages = completedPaintingsManager.getCompletedPaintings()
        binding.completedImagesContainer.removeAllViews()
        
        completedImages.forEach { imagePath ->
            val imageView = ImageView(context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    marginEnd = resources.getDimensionPixelSize(com.example.paintnumber.R.dimen.spacing_small)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setOnClickListener {
                    findNavController().navigate(
                        CongratulationFragmentDirections.actionCongratulationToCompletedImage(imagePath)
                    )
                }
            }
            
            Glide.with(this)
                .load(File(imagePath))
                .into(imageView)
                
            binding.completedImagesContainer.addView(imageView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 