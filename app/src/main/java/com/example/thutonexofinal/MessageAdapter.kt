package com.example.thutonexofinal

import android.content.Intent
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private var items: List<ChatItem>,
    private val currentUserId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        private const val VIEW_TYPE_DATE = 3
    }

    fun updateItems(newItems: List<ChatItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ChatItem.DateHeader -> VIEW_TYPE_DATE
            is ChatItem.MessageItem -> {
                val msg = (items[position] as ChatItem.MessageItem).message
                if (msg.senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
            }
        }
    }

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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (item) {
            is ChatItem.DateHeader -> (holder as DateViewHolder).bind(item)
            is ChatItem.MessageItem -> {
                val msg = item.message
                val timeText = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    .format(Date(msg.timestamp))

                when (holder) {
                    is SentViewHolder -> holder.bind(msg, timeText)
                    is ReceivedViewHolder -> holder.bind(msg, timeText)
                }
            }
        }
    }

    // ===== Sent Message Holder =====
    inner class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val messageTime: TextView = itemView.findViewById(R.id.messageTime)
        private val messageImage: ImageView = itemView.findViewById(R.id.messageImage)

        fun bind(msg: Message, time: String) {
            messageTime.text = time

            when {
                !msg.imageBase64.isNullOrEmpty() -> {
                    // Image message
                    messageText.visibility = View.GONE
                    messageImage.visibility = View.VISIBLE
                    val bytes = Base64.decode(msg.imageBase64, Base64.DEFAULT)
                    Glide.with(itemView.context)
                        .asBitmap()
                        .load(bytes)
                        .into(messageImage)

                    messageImage.setOnClickListener {
                        val intent = Intent(itemView.context, ImagePreviewActivity::class.java)
                        intent.putExtra("imageBase64", msg.imageBase64)
                        itemView.context.startActivity(intent)
                    }
                }

                !msg.fileBase64.isNullOrEmpty() && !msg.fileName.isNullOrEmpty() -> {
                    // File message
                    messageImage.visibility = View.VISIBLE
                    messageText.visibility = View.VISIBLE

                    // Set a generic file icon for files
                    messageImage.setImageResource(R.drawable.ic_file) // <-- Add a file icon in drawable
                    messageText.text = msg.fileName

                    messageImage.setOnClickListener {
                        FileHelper.openFile(itemView.context, msg.fileBase64, msg.fileName)
                    }

                    messageText.setOnClickListener {
                        FileHelper.openFile(itemView.context, msg.fileBase64, msg.fileName)
                    }
                }

                else -> {
                    // Text message
                    messageImage.visibility = View.GONE
                    messageText.visibility = View.VISIBLE
                    messageText.text = msg.text
                }
            }
        }
    }

    // ===== Received Message Holder =====
    inner class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val messageTime: TextView = itemView.findViewById(R.id.messageTime)
        private val messageImage: ImageView = itemView.findViewById(R.id.messageImage)

        fun bind(msg: Message, time: String) {
            messageTime.text = time

            when {
                !msg.imageBase64.isNullOrEmpty() -> {
                    messageText.visibility = View.GONE
                    messageImage.visibility = View.VISIBLE
                    val bytes = Base64.decode(msg.imageBase64, Base64.DEFAULT)
                    Glide.with(itemView.context)
                        .asBitmap()
                        .load(bytes)
                        .into(messageImage)

                    messageImage.setOnClickListener {
                        val intent = Intent(itemView.context, ImagePreviewActivity::class.java)
                        intent.putExtra("imageBase64", msg.imageBase64)
                        itemView.context.startActivity(intent)
                    }
                }

                !msg.fileBase64.isNullOrEmpty() && !msg.fileName.isNullOrEmpty() -> {
                    messageImage.visibility = View.VISIBLE
                    messageText.visibility = View.VISIBLE

                    messageImage.setImageResource(R.drawable.ic_file)
                    messageText.text = msg.fileName

                    messageImage.setOnClickListener {
                        FileHelper.openFile(itemView.context, msg.fileBase64, msg.fileName)
                    }

                    messageText.setOnClickListener {
                        FileHelper.openFile(itemView.context, msg.fileBase64, msg.fileName)
                    }
                }

                else -> {
                    messageImage.visibility = View.GONE
                    messageText.visibility = View.VISIBLE
                    messageText.text = msg.text
                }
            }
        }
    }

    // ===== Date Header Holder =====
    inner class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateText: TextView = itemView.findViewById(R.id.dateText)
        fun bind(item: ChatItem.DateHeader) {
            dateText.text = item.date
        }
    }
}
