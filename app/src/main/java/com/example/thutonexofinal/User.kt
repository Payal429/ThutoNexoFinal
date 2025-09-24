package com.example.thutonexofinal

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val subjects: Any? = null,  // Accept String or List<String>
    val profilePic: String = ""
)