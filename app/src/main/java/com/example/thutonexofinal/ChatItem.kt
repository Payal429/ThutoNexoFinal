package com.example.thutonexofinal

sealed class ChatItem {
    data class MessageItem(val message: Message) : ChatItem()
    data class DateHeader(val date: String) : ChatItem()
}