package com.example.thutonexofinal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    // Immutable data set
    private val chatList: List<Chat>,
    // Click listener supplied by caller
    private val onChatClick: (Chat) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    // ViewHolder caches findViewById calls
    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Display name
        val userName: TextView = itemView.findViewById(R.id.username)

        // Preview Text
        val lastMessage: TextView = itemView.findViewById(R.id.lastMessage)

        // Time HH:mm
        val timestamp: TextView = itemView.findViewById(R.id.timestamp)
    }

    // Create new row (item_chat.xml)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            // Row layout
            .inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    // Bind data to a row
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chatList[position]

        // Show the other participant’s name (already set in the Chat object)
        holder.userName.text = chat.name

        // Last message preview (can be "[Image]" or plain text)
        holder.lastMessage.text = chat.lastMessage

        // Format timestamp as HH:mm; fallback to empty string if null
        holder.timestamp.text = chat.timestamp?.let {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))
        } ?: ""

        // Whole row acts as a button
        holder.itemView.setOnClickListener { onChatClick(chat) }
    }

    // RecyclerView contract
    override fun getItemCount(): Int = chatList.size
}