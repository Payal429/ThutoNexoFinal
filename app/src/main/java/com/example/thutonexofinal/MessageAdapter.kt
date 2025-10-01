package com.example.thutonexofinal

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.util.Base64
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.RequestOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private var items: List<ChatItem>,
    private val currentUserId: String

) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        private const val VIEW_TYPE_DATE = 3
    }

    // Add this function to update items
    fun updateItems(newItems: List<ChatItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    // View-type decision
    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ChatItem.DateHeader -> VIEW_TYPE_DATE
            is ChatItem.MessageItem -> {
                val msg = (items[position] as ChatItem.MessageItem).message
                if (msg.senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
            }
        }
    }

    // ViewHolder creation
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> SentViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_sent, parent, false)
            )

            VIEW_TYPE_RECEIVED -> ReceivedViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_received, parent, false)
            )

            VIEW_TYPE_DATE -> DateViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_date_header, parent, false)
            )

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemCount(): Int = items.size

    // Binding
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ChatItem.DateHeader -> (holder as DateViewHolder).bind(item)
            is ChatItem.MessageItem -> {
                val msg = item.message
                val timeText =
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp))

                if (holder is SentViewHolder) {
                    holder.bind(msg.text, msg.imageBase64, timeText)
                }
                if (holder is ReceivedViewHolder) {
                    holder.bind(msg.text, msg.imageBase64, timeText)
                }
            }
        }
    }

    // Sent message holder
    inner class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val messageTime: TextView = itemView.findViewById(R.id.messageTime)
        private val messageImage: ImageView = itemView.findViewById(R.id.messageImage)

        fun bind(text: String?, imageBase64: String?, time: String) {
            messageTime.text = time

            if (!imageBase64.isNullOrEmpty()) {
                // Show image, hide text
                messageImage.visibility = View.VISIBLE
                messageText.visibility = View.GONE

                val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
                Glide.with(itemView.context)
                    .asBitmap()
                    .load(bytes)
                    .into(messageImage)

                // Click listener to open full screen
                messageImage.setOnClickListener {
                    val intent = Intent(itemView.context, ImagePreviewActivity::class.java)
                    intent.putExtra("imageBase64", imageBase64)
                    itemView.context.startActivity(intent)
                }

            } else {
                // Show text, hide image
                messageImage.visibility = View.GONE
                messageText.visibility = View.VISIBLE
                messageText.text = text
            }
        }
    }

    // Received message holder
    inner class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val messageTime: TextView = itemView.findViewById(R.id.messageTime)
        private val messageImage: ImageView = itemView.findViewById(R.id.messageImage)

        fun bind(text: String?, imageBase64: String?, time: String) {
            messageTime.text = time

            if (!imageBase64.isNullOrEmpty()) {
                messageImage.visibility = View.VISIBLE
                messageText.visibility = View.GONE

                val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
                Glide.with(itemView.context)
                    .asBitmap()
                    .load(bytes)
                    .into(messageImage)

                messageImage.setOnClickListener {
                    val intent = Intent(itemView.context, ImagePreviewActivity::class.java)
                    intent.putExtra("imageBase64", imageBase64)
                    itemView.context.startActivity(intent)
                }

            } else {
                messageImage.visibility = View.GONE
                messageText.visibility = View.VISIBLE
                messageText.text = text
            }
        }
    }

    // Date holder
    inner class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateText: TextView = itemView.findViewById(R.id.dateText)
        fun bind(item: ChatItem.DateHeader) {
            dateText.text = item.date
        }
    }
}