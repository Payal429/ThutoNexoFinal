package com.example.thutonexofinal

data class Chat(
    var chatId: String = "",
    val participants: List<String> = listOf(),
    val lastMessage: String = "",
    val timestamp: Long = 0,
    var name: String = "", // Display name (other user's name)
    val unreadCount: Int = 0
)
