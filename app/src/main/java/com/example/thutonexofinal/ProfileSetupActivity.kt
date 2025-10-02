package com.example.thutonexofinal

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.thutonexofinal.databinding.ActivityProfileSetupBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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

        // Toolbar back button
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // Select image button
        binding.btnSelectImage.setOnClickListener {
            checkAndPickImage()
        }

        // Save profile button
        binding.btnSave.setOnClickListener {
            saveProfile()
        }
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
                                downloadUrl.toString()
                            )
                        }.addOnFailureListener {
                            saveUserData(
                                userId,
                                name,
                                phone,
                                role,
                                subjects,
                                bio,
                                selectedImageBase64
                            )
                        }
                    }
                    .addOnFailureListener {
                        saveUserData(userId, name, phone, role, subjects, bio, selectedImageBase64)
                    }
            } else {
                saveUserData(userId, name, phone, role, subjects, bio, selectedImageBase64)
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
        profileImage: String?
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
            "profileImage" to profileImage
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
}
