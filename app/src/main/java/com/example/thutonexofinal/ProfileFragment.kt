package com.example.thutonexofinal

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.example.thutonexofinal.databinding.FragmentProfileBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var selectedImageUri: Uri? = null
    private var selectedImageBase64: String? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1001
        private const val TAG = "ProfileFragment"
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Firebase init
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Load user data
        loadUserData()

        // Buttons
        binding.btnSelectImage.setOnClickListener { pickImage() }
        binding.btnUpdate.setOnClickListener { updateProfile() }

        // Setup toolbar
        setupToolbar()

    }

    private fun setupToolbar() {
        val toolbar: MaterialToolbar = binding.topAppBar
        (requireActivity() as androidx.appcompat.app.AppCompatActivity).apply {
            setSupportActionBar(toolbar)
            supportActionBar?.title = "Update Your Profile"
            supportActionBar?.setDisplayShowTitleEnabled(true)
        }

        // Show back arrow only if opened from Settings
        val fromSettings = arguments?.getBoolean("fromSettings", false) ?: false
        if (fromSettings) {
            toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
            toolbar.setNavigationOnClickListener {
                requireActivity().supportFragmentManager.popBackStack()
            }
        } else {
            toolbar.navigationIcon = null
            toolbar.setNavigationOnClickListener(null)
        }

        // Make toolbar title bold with custom font
        for (i in 0 until toolbar.childCount) {
            val child = toolbar.getChildAt(i)
            if (child is TextView && child.text == toolbar.title) {
                val typeface = ResourcesCompat.getFont(requireContext(), R.font.anek_gujarati_bold)
                child.typeface = typeface
                child.setTypeface(child.typeface, android.graphics.Typeface.BOLD)
                break
            }
        }
    }

    // Image picker
    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedImageUri = uri
                val bitmap = uriToBitmap(uri)
                binding.ivProfile.setImageBitmap(bitmap)
                selectedImageBase64 = bitmapToBase64(bitmap)
            }
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap {
        val stream = requireContext().contentResolver.openInputStream(uri)
        return BitmapFactory.decodeStream(stream!!)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }

    private fun base64ToBitmap(base64: String): Bitmap {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                if (doc.exists()) {
                    binding.etName.setText(doc.getString("name") ?: "")
                    binding.etPhone.setText(doc.getString("phone") ?: "")
                    binding.etSubject.setText(doc.getString("subjects") ?: "")
                    binding.etBio.setText(doc.getString("bio") ?: "")

                    when (doc.getString("role")) {
                        "Tutor" -> binding.rbTutor.isChecked = true
                        "Student" -> binding.rbStudent.isChecked = true
                    }

                    val imageBase64 = doc.getString("profileImage")
                    if (!imageBase64.isNullOrEmpty()) {
                        try {
                            val bitmap = base64ToBitmap(imageBase64)
                            binding.ivProfile.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to load image", e)
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateProfile() {
        val uid = auth.currentUser?.uid ?: return

        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val role = when {
            binding.rbTutor.isChecked -> "Tutor"
            binding.rbStudent.isChecked -> "Student"
            else -> ""
        }
        val subjects = binding.etSubject.text.toString().trim()
        val bio = binding.etBio.text.toString().trim()

        val updates = mutableMapOf<String, Any>(
            "name" to name,
            "phone" to phone,
            "role" to role,
            "subjects" to subjects,
            "bio" to bio
        )

        selectedImageBase64?.let { updates["profileImage"] = it }

        firestore.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update profile", e)
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
