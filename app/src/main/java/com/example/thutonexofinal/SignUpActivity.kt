package com.example.thutonexofinal

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.thutonexofinal.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Sign Up button click
        binding.btnSignUp.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etSignUpEmail.text.toString().trim()
            val password = binding.etSignUpPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            checkUserExistsAndSignUp(name, email, password)
        }

        // Redirect to LoginActivity
        binding.tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun checkUserExistsAndSignUp(name: String, email: String, password: String) {
        // Check if email is already registered
        auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val signInMethods = task.result?.signInMethods
                if (signInMethods.isNullOrEmpty()) {
                    // User doesn't exist, create account
                    createUser(name, email, password)
                } else {
                    Toast.makeText(this, "User already exists. Please login.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Failed to check user existence", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createUser(name: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid

                // Save a default user document to Firestore
                val userMap = hashMapOf(
                    "uid" to userId,
                    "name" to name,
                    "email" to email,
                    "profileImage" to "", // default empty
                    "role" to "",         // will be set in ProfileSetupActivity
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
                            Toast.makeText(this, "Failed to save user: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            } else {
                Toast.makeText(this, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

}