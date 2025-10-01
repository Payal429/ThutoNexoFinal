package com.example.thutonexofinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.thutonexofinal.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initially hide views
        binding.title.visibility = View.INVISIBLE
        binding.tagline.visibility = View.INVISIBLE
        binding.tagline2.visibility = View.INVISIBLE
        binding.btnGetStarted.visibility = View.INVISIBLE

        // Load the fade+slide animation
        val fadeSlideAnim: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_slide_up)

        // Change status bar color
        window.statusBarColor = getColor(R.color.light_purple) // your desired color

        // Optional: make status bar icons dark or light
        window.decorView.systemUiVisibility = 0 // 0 = light icons, or use SYSTEM_UI_FLAG_LIGHT_STATUS_BAR for dark icons

        // Animate sequentially
        binding.logo.startAnimation(fadeSlideAnim)

        fadeSlideAnim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                binding.title.visibility = View.VISIBLE
                val titleAnim = AnimationUtils.loadAnimation(this@SplashActivity, R.anim.fade_slide_up)
                binding.title.startAnimation(titleAnim)

                titleAnim.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}
                    override fun onAnimationEnd(animation: Animation?) {
                        binding.tagline.visibility = View.VISIBLE
                        val taglineAnim = AnimationUtils.loadAnimation(this@SplashActivity, R.anim.fade_slide_up)
                        binding.tagline.startAnimation(taglineAnim)

                        taglineAnim.setAnimationListener(object : Animation.AnimationListener {
                            override fun onAnimationStart(animation: Animation?) {}
                            override fun onAnimationEnd(animation: Animation?) {
                                binding.tagline2.visibility = View.VISIBLE
                                val tagline2Anim = AnimationUtils.loadAnimation(this@SplashActivity, R.anim.fade_slide_up)
                                binding.tagline2.startAnimation(tagline2Anim)

                                tagline2Anim.setAnimationListener(object : Animation.AnimationListener {
                                    override fun onAnimationStart(animation: Animation?) {}
                                    override fun onAnimationEnd(animation: Animation?) {
                                        binding.btnGetStarted.visibility = View.VISIBLE
                                        val buttonAnim = AnimationUtils.loadAnimation(this@SplashActivity, R.anim.fade_slide_up)
                                        binding.btnGetStarted.startAnimation(buttonAnim)
                                    }
                                    override fun onAnimationRepeat(animation: Animation?) {}
                                })
                            }
                            override fun onAnimationRepeat(animation: Animation?) {}
                        })
                    }
                    override fun onAnimationRepeat(animation: Animation?) {}
                })
            }
            override fun onAnimationRepeat(animation: Animation?) {}
        })

        // Button click to go to LoginActivity
        binding.btnGetStarted.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}