package com.example.paintnumber.utils

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import kotlin.random.Random

class ConfettiManager(private val context: Context) {
    private var velocityX: Pair<Float, Float> = Pair(0f, 0f)
    private var velocityY: Pair<Float, Float> = Pair(0f, 0f)
    private var rotationalVelocity: Pair<Float, Float> = Pair(0f, 0f)
    private var emissionDuration: Long = 1000
    private var emissionRate: Int = 50
    private val confettiList = mutableListOf<Confetti>()
    private val random = Random(System.currentTimeMillis())
    private val colors = arrayOf(
        Color.parseColor("#FFC107"), // Yellow
        Color.parseColor("#FF5722"), // Deep Orange
        Color.parseColor("#4CAF50"), // Green
        Color.parseColor("#2196F3"), // Blue
        Color.parseColor("#9C27B0")  // Purple
    )

    fun setVelocityX(min: Int, max: Int): ConfettiManager {
        velocityX = Pair(min.toFloat(), max.toFloat())
        return this
    }

    fun setVelocityY(min: Int, max: Int): ConfettiManager {
        velocityY = Pair(min.toFloat(), max.toFloat())
        return this
    }

    fun setRotationalVelocity(min: Int, max: Int): ConfettiManager {
        rotationalVelocity = Pair(min.toFloat(), max.toFloat())
        return this
    }

    fun setEmissionDuration(duration: Long): ConfettiManager {
        emissionDuration = duration
        return this
    }

    fun setEmissionRate(rate: Int): ConfettiManager {
        emissionRate = rate
        return this
    }

    fun animate(parent: ViewGroup) {
        val confettiView = ConfettiView(context)
        parent.addView(confettiView)

        val emissionAnimator = ValueAnimator.ofInt(0, (emissionDuration / (1000f / emissionRate)).toInt())
        emissionAnimator.duration = emissionDuration
        emissionAnimator.interpolator = LinearInterpolator()
        emissionAnimator.addUpdateListener { animator ->
            if (animator.animatedValue as Int > confettiList.size) {
                createConfetti(parent.width, parent.height)
            }
            confettiView.invalidate()
        }
        emissionAnimator.start()
    }

    private fun createConfetti(width: Int, height: Int) {
        val x = width / 2f
        val y = height.toFloat()
        val vx = random.nextFloat() * (velocityX.second - velocityX.first) + velocityX.first
        val vy = random.nextFloat() * (velocityY.second - velocityY.first) + velocityY.first
        val rotation = random.nextFloat() * (rotationalVelocity.second - rotationalVelocity.first) + rotationalVelocity.first
        val color = colors[random.nextInt(colors.size)]
        confettiList.add(Confetti(x, y, vx, vy, rotation, color))
    }

    inner class ConfettiView(context: Context) : View(context) {
        private val paint = Paint().apply {
            style = Paint.Style.FILL
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val iterator = confettiList.iterator()
            while (iterator.hasNext()) {
                val confetti = iterator.next()
                confetti.update()
                if (confetti.y > height) {
                    iterator.remove()
                    continue
                }
                paint.color = confetti.color
                canvas.save()
                canvas.translate(confetti.x, confetti.y)
                canvas.rotate(confetti.rotation)
                canvas.drawRect(-10f, -5f, 10f, 5f, paint)
                canvas.restore()
            }
            if (confettiList.isNotEmpty()) {
                invalidate()
            }
        }
    }

    inner class Confetti(
        var x: Float,
        var y: Float,
        private val vx: Float,
        private val vy: Float,
        private val rotationalVelocity: Float,
        val color: Int
    ) {
        var rotation = 0f
        private var time = 0f
        private val gravity = 980f // pixels per second squared

        fun update() {
            time += 0.016f // Assuming 60 FPS
            x += vx * 0.016f
            y += vy * 0.016f + 0.5f * gravity * time * time
            rotation += rotationalVelocity * 0.016f
        }
    }
} 