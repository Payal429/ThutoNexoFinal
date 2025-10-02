package com.example.thutonexofinal

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.thutonexofinal.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    // View-binding
    private lateinit var binding: ActivitySignupBinding

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Sign Up button click
        binding.btnSignUp.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Reset all borders first
            binding.layoutName.parent?.let { (it as? com.google.android.material.textfield.TextInputLayout)?.setBoxStrokeColor(getColor(R.color.black)) }
            binding.layoutEmail.parent?.let { (it as? com.google.android.material.textfield.TextInputLayout)?.setBoxStrokeColor(getColor(R.color.black)) }
            binding.layoutPassword.parent?.let { (it as? com.google.android.material.textfield.TextInputLayout)?.setBoxStrokeColor(getColor(R.color.black)) }

            var isValid = true

            // Check empty fields and set red border
            if (name.isEmpty()) {
                (binding.layoutName.parent as? com.google.android.material.textfield.TextInputLayout)
                    ?.setBoxStrokeColor(getColor(R.color.red))
                isValid = false
            }

            if (email.isEmpty()) {
                (binding.layoutEmail.parent as? com.google.android.material.textfield.TextInputLayout)
                    ?.setBoxStrokeColor(getColor(R.color.red))
                isValid = false
            }

            if (password.isEmpty()) {
                (binding.layoutPassword.parent as? com.google.android.material.textfield.TextInputLayout)
                    ?.setBoxStrokeColor(getColor(R.color.red))
                isValid = false
            }

            // Stop if validation fails
            if (!isValid) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check for valid email format
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check password length
            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            // Check for existing account → create if none
            checkUserExistsAndSignUp(name, email, password)
        }

        // Redirect to LoginActivity
        binding.tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // Queries Firebase Auth to see if the email is already registered.
    private fun checkUserExistsAndSignUp(name: String, email: String, password: String) {
        auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val signInMethods = task.result?.signInMethods
                if (signInMethods.isNullOrEmpty()) {
                    createUser(name, email, password)
                } else {
                    Toast.makeText(this, "User already exists. Please login.", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                Toast.makeText(this, "Failed to check user existence", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Creates Firebase Auth account and writes a minimal user document
    private fun createUser(name: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid

                // Save a default user document to Firestore
                val userMap = hashMapOf(
                    "uid" to userId,
                    "name" to name,
                    "email" to email,
                    "profileImage" to "",
                    "role" to "",
                    "phone" to "",
                    "subjects" to "",
                    "bio" to "",
                    "profileComplete" to false
                )

                userId?.let {
                    firestore.collection("users").document(it)
                        .set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Sign up successful!", Toast.LENGTH_SHORT).show()
                            // Go to ProfileSetupActivity
                            startActivity(Intent(this, ProfileSetupActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Failed to save user: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            } else {
                Toast.makeText(
                    this,
                    "Sign up failed: ${task.exception?.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
