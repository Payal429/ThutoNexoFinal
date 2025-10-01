package com.example.thutonexofinal

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.thutonexofinal.databinding.ItemChatBinding
import de.hdodenhof.circleimageview.CircleImageView


class ChatListAdapter(
    // Immutable data set
    private val chatList: List<ChatListModel>,
    // Click callback
    private val onChatClick: (ChatListModel) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatListViewHolder>() {

    // ViewHolder: caches view references & click listener
    inner class ChatListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val username: TextView = view.findViewById(R.id.username)
        val lastMessage: TextView = view.findViewById(R.id.lastMessage)
        val timestamp: TextView = view.findViewById(R.id.timestamp)
        val userImage: CircleImageView = view.findViewById(R.id.profileImage)
        val unreadBadge: TextView = view.findViewById(R.id.unreadBadge)

        init {
            // Entire row acts as a button; guard against NO_POSITION
            view.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onChatClick(chatList[position])
                }
            }
        }
    }

    // Create new row layout (item_chat.xml)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
        return ChatListViewHolder(view)
    }

    // Bind data to a row
    override fun onBindViewHolder(holder: ChatListViewHolder, position: Int) {
        val chat = chatList[position]
        // Basic info
        holder.username.text = chat.username
        holder.lastMessage.text = chat.lastMessage
        holder.timestamp.text = chat.timestamp

        // Show "📷 Image" if the last message was an image
        holder.lastMessage.text = if (chat.lastMessageType == "image") {
            "📷 Image"
        } else {
            chat.lastMessage
        }

        // Apply bold if there are unread messages
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
                // Fallback to default avatar on decode error
                holder.userImage.setImageResource(R.drawable.ic_profile)
            }
        } else {
            // No image supplied: show placeholder
            holder.userImage.setImageResource(R.drawable.ic_profile)
        }
    }

    // RecyclerView contract
    override fun getItemCount(): Int = chatList.size
}