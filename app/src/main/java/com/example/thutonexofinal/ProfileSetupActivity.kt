package com.example.thutonexofinal

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.example.thutonexofinal.databinding.ActivityProfileSetupBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import me.pushy.sdk.Pushy
import java.io.ByteArrayOutputStream
import java.util.*

class ProfileSetupActivity : AppCompatActivity() {

    // View binding
    private lateinit var binding: ActivityProfileSetupBinding

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // Image state
    private var selectedImageUri: Uri? = null
    private var selectedImageBase64: String? = null

    private var deviceToken: String? = null

    companion object {
        private const val TAG = "ProfileSetupActivity"
        private const val PICK_IMAGE_REQUEST = 1001
        private const val REQUEST_IMAGE_PERMISSION = 2001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase init
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // --- Toolbar setup ---
        val toolbar = binding.topAppBar
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Profile Setup"
        supportActionBar?.setDisplayShowTitleEnabled(true)

        // Make toolbar title bold with custom font
        for (i in 0 until toolbar.childCount) {
            val child = toolbar.getChildAt(i)
            if (child is TextView && child.text == toolbar.title) {
                child.setTypeface(child.typeface, android.graphics.Typeface.BOLD)
                val typeface = ResourcesCompat.getFont(this, R.font.anek_gujarati_bold)
                child.typeface = typeface
                break
            }
        }

        // Toolbar back button
        toolbar.setNavigationOnClickListener { onBackPressed() }


        // Select image button
        binding.btnSelectImage.setOnClickListener {
            checkAndPickImage()
        }

        // Save profile button
        binding.btnSave.setOnClickListener {
            saveProfile()
        }

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1002
                )
            }
        }

        // Start Pushy background service
        Pushy.listen(this)

        //Register device token with Pushy in the background
        Thread {
            try {
                deviceToken = Pushy.register(this)
                // Log it or send it to your server
                runOnUiThread {
                    Log.d(TAG, "Pushy device token: $deviceToken")
//                    Toast.makeText(this, "Pushy Token: $deviceToken", Toast.LENGTH_LONG).show()
                }

            }catch (exc: Exception) {
                exc.printStackTrace()
                runOnUiThread {
                    Log.e(TAG, "Pushy Registration Failed", exc)
//                    Toast.makeText(this, "Pushy Registration Failed", Toast.LENGTH_LONG).show()
                }
            }
        }.start()

        // Start Pushy background service
        Pushy.listen(this)
    }

    // Check permissions & open gallery
    private fun checkAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                openImagePicker()
            } else {
                requestPermissions(
                    arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES),
                    REQUEST_IMAGE_PERMISSION
                )
            }
        } else {
            // Older devices
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                openImagePicker()
            } else {
                requestPermissions(
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_IMAGE_PERMISSION
                )
            }
        }
    }

    // Handle permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_IMAGE_PERMISSION) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                openImagePicker()
            } else {
                Toast.makeText(this, "Permission required to select image", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    // Open gallery
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    // Handle image selection
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedImageUri = uri
                val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
                binding.ivProfile.setImageBitmap(bitmap)
                selectedImageBase64 = bitmapToBase64(bitmap) // fallback if storage fails
            }
        }
    }

    // Convert bitmap to Base64
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }

    // Save profile
    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val role = when {
            binding.rbTutor.isChecked -> "Tutor"
            binding.rbStudent.isChecked -> "Student"
            else -> ""
        }
        val subjects = binding.etSubject.text.toString().trim()
        val bio = binding.etBio.text.toString().trim()
        val uid = auth.currentUser?.uid

        if (name.isEmpty()) {
            binding.etName.error = "Name is required"
            binding.etName.requestFocus()
            return
        }
        if (phone.isEmpty()) {
            binding.etPhone.error = "Phone number is required"
            binding.etPhone.requestFocus()
            return
        }
        if (role.isEmpty()) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show()
            return
        }

        uid?.let { userId ->
            if (selectedImageUri != null) {
                // Upload image to Firebase Storage
                val imageRef =
                    storage.reference.child("profile_images/$userId/${UUID.randomUUID()}")
                imageRef.putFile(selectedImageUri!!)
                    .addOnSuccessListener {
                        imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                            saveUserData(
                                userId,
                                name,
                                phone,
                                role,
                                subjects,
                                bio,
                                downloadUrl.toString(),
                                deviceToken
                            )
                        }.addOnFailureListener {
                            saveUserData(
                                userId,
                                name,
                                phone,
                                role,
                                subjects,
                                bio,
                                selectedImageBase64,
                                deviceToken
                            )
                        }
                    }
                    .addOnFailureListener {
                        saveUserData(userId, name, phone, role, subjects, bio, selectedImageBase64, deviceToken)
                    }
            } else {
                saveUserData(userId, name, phone, role, subjects, bio, selectedImageBase64, deviceToken)
            }
        } ?: run {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    // Write user data to Firestore
    private fun saveUserData(
        userId: String,
        name: String,
        phone: String,
        role: String,
        subjects: String,
        bio: String,
        profileImage: String?,
        deviceToken: String? = null
    ) {
        val userData = hashMapOf(
            "uid" to userId,
            "name" to name,
            "phone" to phone,
            "role" to role,
            "subjects" to subjects,
            "bio" to bio,
            "email" to auth.currentUser?.email,
            "profileComplete" to true,
            "profileImage" to profileImage,
            "deviceToken" to deviceToken
        )

        firestore.collection("users")
            .document(userId)
            .set(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile saved successfully", Toast.LENGTH_SHORT).show()
                navigateToMain()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving profile", e)
                Toast.makeText(this, "Failed to save profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToMain() {
        val intent = Intent(this@ProfileSetupActivity, MainActivity::class.java)
        intent.putExtra("openChatList", true)
        startActivity(intent)
        finish()
    }

//    // Handle permission result
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//
//        if (requestCode == 1002) {
//            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Log.d("MainActivity", "POST_NOTIFICATIONS permission granted ✅")
//            } else {
//                Log.w("MainActivity", "POST_NOTIFICATIONS permission denied ❌")
//            }
//        }
//    }
}
