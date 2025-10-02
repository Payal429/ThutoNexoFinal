package com.example.thutonexofinal

data class Message(
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Long = 0,
    val read: Boolean = false,
    val readAt: Long? = null,
    val type: String = "text",  // text, image, file
    val imageBase64: String? = null,
    val fileBase64: String? = null,
    val fileName: String? = null
)
