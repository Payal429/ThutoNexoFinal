package com.example.thutonexofinal

import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.os.Bundle
import android.util.Base64
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.thutonexofinal.databinding.ActivityImagePreviewBinding
import com.google.android.material.appbar.MaterialToolbar
import androidx.core.content.res.ResourcesCompat

class ImagePreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImagePreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Change status bar color
        window.statusBarColor = getColor(R.color.light_purple) // your desired color

        // Optional: make status bar icons dark or light
        window.decorView.systemUiVisibility = 0 // 0 = light icons, or use SYSTEM_UI_FLAG_LIGHT_STATUS_BAR for dark icons

        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "" // optional: remove title

        // Set up toolbar as action bar
        val toolbar: MaterialToolbar = binding.toolbar
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Full Image" // ← set your title

        // Make toolbar title bold with custom font
        for (i in 0 until toolbar.childCount) {
            val view = toolbar.getChildAt(i)
            if (view is TextView && view.text == toolbar.title) {
                // Set bold style
                view.setTypeface(view.typeface, Typeface.BOLD)
                // Set custom font from resources (backward-compatible)
                val typeface = ResourcesCompat.getFont(this, R.font.anek_gujarati_bold)
                view.typeface = typeface
                break
            }
        }

        // Back arrow click
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }


        // Get imageBase64 from intent
        val imageBase64 = intent.getStringExtra("imageBase64")
        if (!imageBase64.isNullOrEmpty()) {
            val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            binding.fullscreenImage.setImageBitmap(bitmap)
        }
    }
}
