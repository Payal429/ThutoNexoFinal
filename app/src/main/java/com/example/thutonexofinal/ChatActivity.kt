package com.example.thutonexofinal

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thutonexofinal.databinding.ActivityChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

// Chat screen for messaging
class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MessageAdapter
    // Holds chat messages
    private val messageList = mutableListOf<Message>()
    // Firestore reference
    private val db = FirebaseFirestore.getInstance()
    // Current logged-in user
    private val currentUserId = FirebaseAuth.getInstance().uid ?: ""

    private lateinit var chatId: String
    private lateinit var receiverUid: String
    private lateinit var receiverName: String

    private lateinit var profileImage: ImageView
    private lateinit var usernameText: TextView

    // Firestore listener for messages
    private var messageListener: ListenerRegistration? = null
    private val PICK_IMAGE_REQUEST = 1001
    private val PICK_FILE_REQUEST = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If no logged-in user, exit
        if (currentUserId.isEmpty()) {
            finish()
            return
        }

        setContentView(R.layout.activity_chat)

        // Status bar color
        window.statusBarColor = getColor(R.color.green)
        window.decorView.systemUiVisibility = 0

        // Get intent extras (chat info)
        chatId = intent.getStringExtra("chatId") ?: run { finish(); return }
        receiverUid = intent.getStringExtra("receiverId") ?: run { finish(); return }
        receiverName = intent.getStringExtra("receiverName") ?: run { finish(); return }

        // Setup UI
        setupToolbar()
        setupRecyclerView()
        setupInputs()

        // load Profile and messages
        loadReceiverProfile()
        loadMessages()
    }

    // Mark all messages sent to current user as "read"
    private fun markMessagesAsRead(chatId: String) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("read", false)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (doc in querySnapshot.documents) {
                    doc.reference.update("read", true)
                }
            }
    }

    override fun onResume() {
        super.onResume()
        markMessagesAsRead(chatId)
        // Save current chat to preferences (used to suppress duplicate notifications)
        val prefs = getSharedPreferences("chat_prefs", MODE_PRIVATE)
        prefs.edit().putString("current_chat_id", chatId).apply()
    }
    override fun onPause() {
        super.onPause()
        // Clear current chat
        val prefs = getSharedPreferences("chat_prefs", MODE_PRIVATE)
        prefs.edit().remove("current_chat_id").apply()
    }

    // Toolbar setup with custom profile and name
    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.chatToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val customView = layoutInflater.inflate(R.layout.chat_toolbar, null)
        profileImage = customView.findViewById(R.id.toolbarProfileImage)
        usernameText = customView.findViewById(R.id.toolbarUsername)
        usernameText.text = receiverName
        toolbar.addView(customView)

        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    // RecyclerView for chat messages
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.messagesRecyclerView)
        adapter = MessageAdapter(listOf(), currentUserId)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    // Input buttons setup (send, attach image, attach file)
    private fun setupInputs() {
        findViewById<ImageButton>(R.id.attachImageButton).setOnClickListener { pickImage() }
        findViewById<ImageButton>(R.id.sendButton).setOnClickListener { sendMessage() }
        findViewById<ImageButton>(R.id.attachFileButton)?.setOnClickListener { pickFile() }
    }

    // Load receiver’s profile image from Firestore
    private fun loadReceiverProfile() {
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
    }

    // Image picking
    private fun pickImage() {
        if (PermissionHelper.hasStoragePermission(this)) {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        } else {
            PermissionHelper.requestStoragePermission(this)
        }
    }

    private fun pickFile() {
        if (PermissionHelper.hasStoragePermission(this)) {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            startActivityForResult(intent, PICK_FILE_REQUEST)
        } else {
            PermissionHelper.requestStoragePermission(this)
        }
    }

    // Handle image/file picker result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return

        when (requestCode) {
            PICK_IMAGE_REQUEST -> data.data?.let { uri -> sendImageMessage(uri) }
            PICK_FILE_REQUEST -> data.data?.let { uri -> sendFileMessage(uri) }
        }
    }

    // Send image message
    private fun sendImageMessage(uri: Uri) {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 40, outputStream)
            val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)

            val message = Message(
                senderId = currentUserId,
                receiverId = receiverUid,
                timestamp = System.currentTimeMillis(),
                imageBase64 = base64String,
                type = "image"
            )

            sendAndNotify(message, "[Image]")
        }
    }

    // Send file message
    private fun sendFileMessage(uri: Uri) {
        val fileName = uri.lastPathSegment ?: "file"
        contentResolver.openInputStream(uri)?.use { inputStream ->
            val bytes = inputStream.readBytes()
            val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)

            val message = Message(
                senderId = currentUserId,
                receiverId = receiverUid,
                timestamp = System.currentTimeMillis(),
                fileBase64 = base64String,
                fileName = fileName,
                type = "file"
            )

            sendAndNotify(message, "[File]")
        }
    }

    // Send text message
    private fun sendMessage() {
        val input = findViewById<EditText>(R.id.messageInput)
        val text = input.text.toString().trim()
        if (text.isEmpty()) return

//        val message = Message(currentUserId, text, System.currentTimeMillis().toString(), receiverId = receiverUid)
        val message = Message(
            senderId = currentUserId,
            receiverId = receiverUid,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        sendAndNotify(message, text)

        input.setText("")
    }

    // Save message in Firestore + notify receiver via Pushy
    private fun sendAndNotify(message: Message, lastMessageText: String) {
        val chatRef = db.collection("chats").document(chatId)
        chatRef.collection("messages").add(message).addOnSuccessListener {

            val chatMap = hashMapOf(
                "participants" to listOf(currentUserId, receiverUid),
                "lastMessage" to lastMessageText,
                "timestamp" to System.currentTimeMillis()
            )
            chatRef.set(chatMap, com.google.firebase.firestore.SetOptions.merge())

            // 🔥 Fetch sender's name from Firebase
            db.collection("users").document(currentUserId).get()
                .addOnSuccessListener { snapshot ->
                    val senderName = snapshot.getString("fullName") ?: "New Message" // or "username" field
                    val deviceToken = snapshot.getString("deviceToken") // optional if needed for sender
                    // Now send push to receiver
                    db.collection("users").document(receiverUid).get()
                        .addOnSuccessListener { receiverSnapshot ->
                            val receiverToken = receiverSnapshot.getString("deviceToken")
                            if (!receiverToken.isNullOrEmpty()) {
                                sendPushyNotification(receiverToken, lastMessageText, senderName)
                            }
                        }
                }
        }
    }

/*    private fun sendAndNotify(message: Message, lastMessageText: String) {
        val chatRef = db.collection("chats").document(chatId)
        chatRef.collection("messages").add(message).addOnSuccessListener {
            val chatMap = hashMapOf(
                "participants" to listOf(currentUserId, receiverUid),
                "lastMessage" to lastMessageText,
                "timestamp" to System.currentTimeMillis()
            )
            chatRef.set(chatMap, com.google.firebase.firestore.SetOptions.merge())

            // 🔥 After saving, send Pushy notification
            db.collection("users").document(receiverUid).get()
                .addOnSuccessListener { snapshot ->
                    val deviceToken = snapshot.getString("deviceToken")
                    if (!deviceToken.isNullOrEmpty()) {
                        sendPushyNotification(deviceToken, lastMessageText)
                    }
                }
        }
    }*/


    // Send Pushy notification via API
    private fun sendPushyNotification(deviceToken: String, message: String, senderName: String) {
        Thread {
            try {
                val payload = """
                {
                  "to":"$deviceToken",
                  "data":{
                    "title":"$senderName",
                    "message":"$message",
                    "chatId":"$chatId",
                    "senderId":"$currentUserId",
                    "senderName":"$senderName"
                  }
                }
            """.trimIndent()

                val apiKey = "550af5fe8c2aaf5c3bf72fa60091b844c1582e8d4f49e7ef96db89f828f95757" // ⚠️ Replace with your key
                val url = URL("https://api.pushy.me/push?api_key=$apiKey")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                conn.outputStream.use { os ->
                    val input = payload.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                val responseCode = conn.responseCode
                Log.d("Pushy", "Pushy notification sent, response code: $responseCode")
            } catch (e: Exception) {
                Log.e("Pushy", "Error sending push notification", e)
            }
        }.start()
    }

    // Load messages in real-time (with date headers)
    private fun loadMessages() {
        messageListener = db.collection("chats").document(chatId)
            .collection("messages").orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    val unreadDocs = it.documents.filter { doc ->
                        val msg = doc.toObject(Message::class.java)
                        msg?.senderId != currentUserId && msg?.read == false
                    }

                    if (unreadDocs.isNotEmpty()) {
                        val batch = db.batch()
                        val now = System.currentTimeMillis()
                        unreadDocs.forEach { doc ->
                            batch.update(doc.reference, mapOf("read" to true, "readAt" to now))
                        }
                        batch.commit()
                    }

                    messageList.clear()
                    it.documents.forEach { doc ->
                        doc.toObject(Message::class.java)?.let { msg -> messageList.add(msg) }
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

    // Format message timestamps into Today/Yesterday/dd MMM yyyy
    private fun getFriendlyDate(timestamp: Long): String {
        val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        return when {
            messageDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    messageDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today"
            messageDate.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                    messageDate.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Yesterday"
            else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        messageListener?.remove()
    }
}

