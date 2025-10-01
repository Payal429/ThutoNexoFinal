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

    // Messages list
    private lateinit var recyclerView: RecyclerView
    // Adapter for messages + headers
    private lateinit var adapter: MessageAdapter
    // Raw message objects from Firestore
    private val messageList = mutableListOf<Message>()
    // Singleton Firestore instance
    private val db = FirebaseFirestore.getInstance()
    // Logged-in user UID
    private val currentUserId = FirebaseAuth.getInstance().uid ?: ""

    /* Chat meta-data passed via Intent extras */
    // Firestore document id for this chat
    private lateinit var chatId: String
    // Other participant (used for 1-to-1)
    private lateinit var receiverUid: String
    // Display name in toolbar
    private lateinit var receiverName: String
    // Toolbar avatar
    private lateinit var profileImage: ImageView
    // Toolbar title
    private lateinit var usernameText: TextView

    // Firestore real-time listener (removed in onDestroy to avoid leaks)
    private var messageListener: ListenerRegistration? = null

    // Request code for image picker
    private val PICK_IMAGE_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Change status bar color
        window.statusBarColor = getColor(R.color.green) // your desired color

        // Optional: make status bar icons dark or light
        window.decorView.systemUiVisibility = 0 // 0 = light icons, or use SYSTEM_UI_FLAG_LIGHT_STATUS_BAR for dark icons

        // Extract Intent extras (finish if missing)
        chatId = intent.getStringExtra("chatId") ?: run { finish(); return }
        receiverUid = intent.getStringExtra("receiverId") ?: run { finish(); return }
        receiverName = intent.getStringExtra("receiverName") ?: run { finish(); return }

        // Request storage permission for selecting images
        PermissionHelper.requestStoragePermission(this)

        // Toolbar with custom layout (avatar + name)
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

        // Back arrow navigation
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Input handlers
        findViewById<ImageButton>(R.id.attachImageButton).setOnClickListener { pickImage() }
        // Send button
        findViewById<ImageButton>(R.id.sendButton).setOnClickListener { sendMessage() }

        // Load receiver profile picture (real-time)
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

    // Permission handling (Android 13+ compatibility)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PermissionHelper.REQUEST_STORAGE -> {
                PermissionHelper.handlePermissionResult(
                    this,
                    requestCode,
                    grantResults,
                    onGranted = { /* Optional: do something if granted */ },
                    onDenied = { /* Optional: handle denial */ }
                )
            }
        }
    }

    // Image picker and compression
    private fun pickImage() {
        // Check storage permission first
        PermissionHelper.requestStoragePermission(this)

        // After permission is granted, open gallery
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // On Android 13+, use READ_MEDIA_IMAGES
            PermissionHelper.requestImagePermission(this)
        }

        // Launch system gallery
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    // Called when user selects an image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                sendImageMessage(uri)
            }
        }
    }

    // Compresses the chosen image, converts to Base64 and sends as a message
    private fun sendImageMessage(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)

        // Compress to JPEG 40 % quality to stay within Firestore 1 MiB limit
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 40, outputStream)
        val compressedBytes = outputStream.toByteArray()
        val base64String = Base64.encodeToString(compressedBytes, Base64.DEFAULT)

        val message = Message(
            senderId = currentUserId,
            timestamp = System.currentTimeMillis(),
            imageBase64 = base64String,
            type = "image"
        )

        // Add to sub-collection and update parent chat meta-data
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

    // Text message sender
    private fun sendMessage() {
        val messageInput = findViewById<EditText>(R.id.messageInput)
        val text = messageInput.text.toString().trim()
        if (text.isEmpty()) return

        val message = Message(currentUserId, text, System.currentTimeMillis())

        val chatRef = db.collection("chats").document(chatId)
        val messagesRef = chatRef.collection("messages")

        // Add message to subcollection
        messagesRef.add(message).addOnSuccessListener {
            // Update parent document (lastMessage & timestamp)
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
        // clear input field
        messageInput.setText("")
    }

    // Helper: convert timestamp → “Today”, “Yesterday”, date
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

    // Firestore real-time listener with read-receipts
    private fun loadMessages() {
        messageListener = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {

                    // Find unread messages not sent by me
                    val unreadDocs = snapshot.documents.filter {
                        val msg = it.toObject(Message::class.java)
                        msg?.senderId != currentUserId && msg?.read == false
                    }
                    // Batch mark them as read
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

                    // Convert Firestore documents → Message objects
                    messageList.clear()
                    for (doc in snapshot.documents) {
                        doc.toObject(Message::class.java)?.let { messageList.add(it) }
                    }

                    // Build UI list with date headers (Today / Yesterday / full date)
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

                    // Update RecyclerView
                    adapter.updateItems(items)
                    recyclerView.scrollToPosition(items.size - 1)
                }
            }
    }

    // Clean-up: remove Firestore listener
    override fun onDestroy() {
        super.onDestroy()
        messageListener?.remove()
    }
}

