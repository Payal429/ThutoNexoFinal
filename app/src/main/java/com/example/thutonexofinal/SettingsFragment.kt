package com.example.thutonexofinal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Switch
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import kotlin.jvm.java

class SettingsFragment : Fragment() {

    private lateinit var switchNotifications: Switch
    private lateinit var btnChangePassword: MaterialButton
    private lateinit var btnProfileSettings: MaterialButton
    private lateinit var btnAboutApp: MaterialButton
    private lateinit var btnLogout: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        switchNotifications = view.findViewById(R.id.switchNotifications)
        btnChangePassword = view.findViewById(R.id.btnChangePassword)
        btnProfileSettings = view.findViewById(R.id.btnProfileSettings)
        btnAboutApp = view.findViewById(R.id.btnAboutApp)
        btnLogout = view.findViewById(R.id.btnLogout)

        // SharedPreferences
        val prefs = requireContext().getSharedPreferences("app_settings", 0)
        switchNotifications.isChecked = prefs.getBoolean("notifications", true)

        // Click listeners
        btnChangePassword.setOnClickListener { showChangePasswordDialog() }

        btnProfileSettings.setOnClickListener {
            val profileFragment = ProfileFragment()
            profileFragment.arguments = Bundle().apply {
                putBoolean("fromSettings", true) // pass flag
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, profileFragment)
                .addToBackStack(null)
                .commit()
        }

        btnAboutApp.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("About App")
                .setMessage("ThutonExo v1.0\n\nA learning platform for students and tutors.")
                .setPositiveButton("OK", null)
                .show()
        }

        btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showChangePasswordDialog() {
        val editText = EditText(requireContext())
        editText.hint = "Enter new password"

        AlertDialog.Builder(requireContext())
            .setTitle("Change Password")
            .setView(editText)
            .setPositiveButton("Update") { _, _ ->
                val newPassword = editText.text.toString()
                if (newPassword.length < 6) {
                    Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                FirebaseAuth.getInstance().currentUser?.updatePassword(newPassword)
                    ?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(requireContext(), "Password updated successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
