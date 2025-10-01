package com.example.thutonexofinal

sealed class ChatItem {
    // Wraps a [Message] object so the adapter can treat it as a list item.
    data class MessageItem(val message: Message) : ChatItem()
    // Lightweight header item that only carries the date string to display.
    data class DateHeader(val date: String) : ChatItem()
}