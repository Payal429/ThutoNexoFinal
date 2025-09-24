package com.example.thutonexofinal

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val chatListFragment = ChatListFragment()
    private val discoveryFragment = DiscoveryFragment()
    private val profileFragment = ProfileFragment()
    private val settingsFragment = SettingsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Change status bar color
        window.statusBarColor = getColor(R.color.light_purple) // your desired color

        // Optional: make status bar icons dark or light
        window.decorView.systemUiVisibility = 0 // 0 = light icons, or use SYSTEM_UI_FLAG_LIGHT_STATUS_BAR for dark icons


        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Determine which fragment to load
        val openChatList = intent.getBooleanExtra("openChatList", false)
        if (savedInstanceState == null) {
            if (openChatList) {
                loadFragment(chatListFragment)
                bottomNav.selectedItemId = R.id.nav_home // highlight chat tab
            } else {
                loadFragment(chatListFragment) // default
            }
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(chatListFragment)
                R.id.nav_discover -> loadFragment(discoveryFragment)
                R.id.nav_profile -> loadFragment(profileFragment)
                R.id.nav_settings -> loadFragment(settingsFragment)
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
        return true
    }

}
/*
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Load ChatListFragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ChatListFragment())
                .commit()
        }
    }
}*/
