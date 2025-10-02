package com.example.thutonexofinal

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.thutonexofinal.databinding.ItemChatBinding
import de.hdodenhof.circleimageview.CircleImageView

class ChatListAdapter(
    private val chatList: List<ChatListModel>,
    private val onChatClick: (ChatListModel) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatListViewHolder>() {

    inner class ChatListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val username: TextView = view.findViewById(R.id.username)
        val lastMessage: TextView = view.findViewById(R.id.lastMessage)
        val timestamp: TextView = view.findViewById(R.id.timestamp)
        val userImage: CircleImageView = view.findViewById(R.id.profileImage)
        val unreadBadge: TextView = view.findViewById(R.id.unreadBadge)

        init {
            view.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onChatClick(chatList[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
        return ChatListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatListViewHolder, position: Int) {
        val chat = chatList[position]
        holder.username.text = chat.username
        holder.timestamp.text = chat.timestamp

        // Determine last message preview based on type
        holder.lastMessage.text = when (chat.lastMessageType) {
            "image" -> "📷 Image"
            "file" -> "📎 File"
            else -> chat.lastMessage
        }

        // Bold and badge if there are unread messages
        if (chat.unreadCount > 0) {
            holder.lastMessage.setTypeface(null, android.graphics.Typeface.BOLD)
            holder.unreadBadge.visibility = View.VISIBLE
            holder.unreadBadge.text = chat.unreadCount.toString()
        } else {
            holder.lastMessage.setTypeface(null, android.graphics.Typeface.NORMAL)
            holder.unreadBadge.visibility = View.GONE
        }

        // Load profile image
        if (chat.profileImageBase64.isNotEmpty()) {
            try {
                val decodedBytes = Base64.decode(chat.profileImageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                holder.userImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.userImage.setImageResource(R.drawable.ic_profile)
            }
        } else {
            holder.userImage.setImageResource(R.drawable.ic_profile)
        }
    }

    override fun getItemCount(): Int = chatList.size
}
