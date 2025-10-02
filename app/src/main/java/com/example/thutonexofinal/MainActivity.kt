package com.example.thutonexofinal

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    // Fragment instances (kept in memory for simplicity)
    private val chatListFragment = ChatListFragment()
    private val discoveryFragment = DiscoveryFragment()
    private val profileFragment = ProfileFragment()
    private val settingsFragment = SettingsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Change status bar color
        window.statusBarColor = getColor(R.color.light_purple)

        // Make status bar icons dark or light
        // 0 = light icons, or use SYSTEM_UI_FLAG_LIGHT_STATUS_BAR for dark icons
        window.decorView.systemUiVisibility = 0

        // Bottom-navigation setup
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Determine which fragment to load
        val openChatList = intent.getBooleanExtra("openChatList", false)
        if (savedInstanceState == null) {
            if (openChatList) {
                loadFragment(chatListFragment)
                // highlight chat tab
                bottomNav.selectedItemId = R.id.nav_home
            } else {
                // Default
                loadFragment(chatListFragment)
            }
        }

        // Handle tab selections
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

    //  Helper: replace fragment in container
    fun loadFragment(fragment: Fragment, selectTabId: Int? = null): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

        selectTabId?.let { id ->
            val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNav.selectedItemId = id
        }

        return true
    }
    override fun onBackPressed() {
        val fragmentCount = supportFragmentManager.backStackEntryCount

        if (fragmentCount == 0) {
            // No fragments left in back stack → app will exit
            showLogoutDialog()
        } else {
            // Navigate back normally
            super.onBackPressed()
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                // Clear session, go to login screen
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


}
