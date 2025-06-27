package com.example.paintnumber.ui.splash

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import com.example.paintnumber.MainActivity
import com.example.paintnumber.R
import com.example.paintnumber.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val circles = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize circles list
        circles.add(binding.circle1)
        circles.add(binding.circle2)
        circles.add(binding.circle3)
        circles.add(binding.circle4)
        circles.add(binding.circle5)

        // Start animations with a slight delay
        binding.root.postDelayed({ startAnimations() }, 500)
    }

    private fun startAnimations() {
        // Palette icon animation
        binding.ivPalette.apply {
            scaleX = 0f
            scaleY = 0f
            alpha = 0f
            animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(800)
                .setInterpolator(OvershootInterpolator())
                .withEndAction { startCircleAnimations() }
                .start()
        }
    }

    private fun startCircleAnimations() {
        val animatorSet = AnimatorSet()
        val animators = mutableListOf<Animator>()

        circles.forEachIndexed { index, circle ->
            // Rotation animation
            val rotationAnimator = ObjectAnimator.ofFloat(
                circle,
                "rotation",
                0f,
                360f
            ).apply {
                duration = 1000
                interpolator = AccelerateDecelerateInterpolator()
            }

            // Scale and alpha animation
            circle.scaleX = 0f
            circle.scaleY = 0f
            circle.alpha = 0f

            val scaleXAnimator = ObjectAnimator.ofFloat(circle, "scaleX", 0f, 1f)
            val scaleYAnimator = ObjectAnimator.ofFloat(circle, "scaleY", 0f, 1f)
            val alphaAnimator = ObjectAnimator.ofFloat(circle, "alpha", 0f, 1f)

            val circleAnimatorSet = AnimatorSet().apply {
                playTogether(rotationAnimator, scaleXAnimator, scaleYAnimator, alphaAnimator)
                startDelay = (index * 100).toLong()
                duration = 500
            }

            animators.add(circleAnimatorSet)
        }

        animatorSet.apply {
            playTogether(animators)
            doOnEnd { startTextAnimations() }
            start()
        }
    }

    private fun startTextAnimations() {
        // App name animation
        binding.tvAppName.apply {
            translationY = 50f
            animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(500)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }

        // Tagline animation with delay
        binding.tvTagline.apply {
            translationY = 50f
            animate()
                .translationY(0f)
                .alpha(1f)
                .setStartDelay(300)
                .setDuration(500)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction { navigateToMain() }
                .start()
        }
    }

    private fun navigateToMain() {
        // Wait for 1 second before navigating
        binding.root.postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            // Add fade animation for activity transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 1000)
    }
} 