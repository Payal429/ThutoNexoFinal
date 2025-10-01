package com.example.thutonexofinal

data class Chat(
    // Will be replaced by the server-generated ID
    var chatId: String = "",
    // Shown in the chat-list preview
    val participants: List<String> = listOf(),
    val lastMessage: String = "",
    // Millis since epoch; used for sorting
    val timestamp: Long = 0,
    // Display name (other user's name)
    var name: String = "",
    // Badge count on the chat item
    val unreadCount: Int = 0
)
