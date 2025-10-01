package com.example.thutonexofinal

data class ChatListModel(
    val chatId: String = "",
    val userId: String = "",
    val username: String = "",
    val profileImageBase64: String = "",
    val lastMessage: String = "",
    val lastMessageType: String = "text",
    val timestamp: String = "",
    val unreadCount: Int = 0
)
