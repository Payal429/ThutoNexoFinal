package com.example.thutonexofinal

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thutonexofinal.databinding.ActivityChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.io.ByteArrayOutputStream

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MessageAdapter
    private val messageList = mutableListOf<Message>()
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().uid ?: ""

    private lateinit var chatId: String
    private lateinit var receiverUid: String
    private lateinit var receiverName: String
    private lateinit var profileImage: ImageView
    private lateinit var usernameText: TextView

    private var messageListener: ListenerRegistration? = null

    private val PICK_IMAGE_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Change status bar color
        window.statusBarColor = getColor(R.color.green) // your desired color

        // Optional: make status bar icons dark or light
        window.decorView.systemUiVisibility = 0 // 0 = light icons, or use SYSTEM_UI_FLAG_LIGHT_STATUS_BAR for dark icons



        chatId = intent.getStringExtra("chatId") ?: run { finish(); return }
        receiverUid = intent.getStringExtra("receiverId") ?: run { finish(); return }
        receiverName = intent.getStringExtra("receiverName") ?: run { finish(); return }

        findViewById<ImageButton>(R.id.attachImageButton).setOnClickListener { pickImage() }

        // Toolbar setup
        val toolbar = findViewById<Toolbar>(R.id.chatToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val customView = layoutInflater.inflate(R.layout.chat_toolbar, null)
        profileImage = customView.findViewById(R.id.toolbarProfileImage)
        usernameText = customView.findViewById(R.id.toolbarUsername)
        usernameText.text = receiverName
        toolbar.addView(customView)

        // RecyclerView setup
        recyclerView = findViewById(R.id.messagesRecyclerView)
        adapter = MessageAdapter(listOf(), currentUserId)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // back arrow
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Send button
        findViewById<ImageButton>(R.id.sendButton).setOnClickListener { sendMessage() }

        // Load receiver profile
        db.collection("users").document(receiverUid)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    val base64 = snapshot.getString("profileImage") ?: ""
                    if (base64.isNotEmpty()) {
                        val bytes = Base64.decode(base64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        profileImage.setImageBitmap(bitmap)
                    } else {
                        profileImage.setImageResource(R.drawable.ic_profile)
                    }
                }
            }

        // Listen for messages in real time
        loadMessages()
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                sendImageMessage(uri)
            }
        }
    }
    private fun sendImageMessage(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)

        // Compress and convert to Base64
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 40, outputStream)
        val compressedBytes = outputStream.toByteArray()
        val base64String = Base64.encodeToString(compressedBytes, Base64.DEFAULT)

        val message = Message(
            senderId = currentUserId,
            timestamp = System.currentTimeMillis(),
            imageBase64 = base64String,
            type = "image"  // 🔹 Add this field
        )

        val chatRef = db.collection("chats").document(chatId)
        val messagesRef = chatRef.collection("messages")

        messagesRef.add(message).addOnSuccessListener {
            val chatMap = hashMapOf(
                "participants" to listOf(currentUserId, receiverUid),
                "lastMessage" to "[Image]",
                "timestamp" to System.currentTimeMillis()
            )
            chatRef.set(chatMap, com.google.firebase.firestore.SetOptions.merge())
        }
    }


    private fun sendMessage() {
        val messageInput = findViewById<EditText>(R.id.messageInput)
        val text = messageInput.text.toString().trim()
        if (text.isEmpty()) return

        val message = Message(currentUserId, text, System.currentTimeMillis())

        val chatRef = db.collection("chats").document(chatId)
        val messagesRef = chatRef.collection("messages")

        // Add message to subcollection
        messagesRef.add(message).addOnSuccessListener {
            val chatMap = hashMapOf(
                "participants" to listOf(currentUserId, receiverUid),
                "lastMessage" to text,
                "timestamp" to System.currentTimeMillis()
            )

            // Create chat if not exists, or update if it does
            chatRef.set(chatMap, com.google.firebase.firestore.SetOptions.merge())
                .addOnFailureListener { e ->
                    Log.e("ChatActivity", "Failed to update chat: ${e.message}")
                }
        }

        messageInput.setText("")
    }

    /*    private fun loadMessages() {
            messageListener = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        messageList.clear()
                        for (doc in snapshot.documents) {
                            doc.toObject(Message::class.java)?.let { messageList.add(it) }
                        }

                        // Convert messages into ChatItem list with date headers
                        val items = mutableListOf<ChatItem>()
                        var lastDate = ""
                        for (msg in messageList) {
                            val msgDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                .format(Date(msg.timestamp))
                            if (msgDate != lastDate) {
                                items.add(ChatItem.DateHeader(msgDate))
                                lastDate = msgDate
                            }
                            items.add(ChatItem.MessageItem(msg))
                        }

                        // Pass ChatItem list to adapter
                        adapter = MessageAdapter(items, currentUserId)
                        recyclerView.adapter = adapter
                        recyclerView.scrollToPosition(items.size - 1)
                    }
                }
        }*/
    private fun getFriendlyDate(timestamp: Long): String {
        val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        return when {
            messageDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    messageDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> {
                "Today"
            }

            messageDate.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                    messageDate.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> {
                "Yesterday"
            }

            else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    private fun loadMessages() {
        messageListener = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {

                    // 1. Find unread messages not sent by me
                    val unreadDocs = snapshot.documents.filter {
                        val msg = it.toObject(Message::class.java)
                        msg?.senderId != currentUserId && msg?.read == false
                    }
                    // 2. Batch mark them as read
                    if (unreadDocs.isNotEmpty()) {
                        val batch = db.batch()
                        unreadDocs.forEach { doc ->
                            batch.update(
                                doc.reference, mapOf(
                                    "read" to true,
                                    "readAt" to System.currentTimeMillis()
                                )
                            )
                        }
                        batch.commit()
                    }


                    messageList.clear()
                    for (doc in snapshot.documents) {
                        doc.toObject(Message::class.java)?.let { messageList.add(it) }
                    }

                    val items = mutableListOf<ChatItem>()
                    var lastDate = ""
                    for (msg in messageList) {
                        val friendlyDate = getFriendlyDate(msg.timestamp)
                        if (friendlyDate != lastDate) {
                            items.add(ChatItem.DateHeader(friendlyDate))
                            lastDate = friendlyDate
                        }
                        items.add(ChatItem.MessageItem(msg))
                    }

                    adapter.updateItems(items)
                    recyclerView.scrollToPosition(items.size - 1)
                }
            }
    }



    override fun onDestroy() {
        super.onDestroy()
        messageListener?.remove()
    }
}

