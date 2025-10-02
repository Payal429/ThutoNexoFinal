package com.example.thutonexofinal

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.io.ByteArrayOutputStream
import java.util.*

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
    private val PICK_FILE_REQUEST = 1004
    private var cameraImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Change status bar color
        window.statusBarColor = getColor(R.color.green)

        // Make status bar icons dark or light
        // 0 = light icons, or use SYSTEM_UI_FLAG_LIGHT_STATUS_BAR for dark icons
        window.decorView.systemUiVisibility = 0

        chatId = intent.getStringExtra("chatId") ?: run { finish(); return }
        receiverUid = intent.getStringExtra("receiverId") ?: run { finish(); return }
        receiverName = intent.getStringExtra("receiverName") ?: run { finish(); return }

        PermissionHelper.requestStoragePermission(this)

        val toolbar = findViewById<Toolbar>(R.id.chatToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val customView = layoutInflater.inflate(R.layout.chat_toolbar, null)
        profileImage = customView.findViewById(R.id.toolbarProfileImage)
        usernameText = customView.findViewById(R.id.toolbarUsername)
        usernameText.text = receiverName
        toolbar.addView(customView)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        recyclerView = findViewById(R.id.messagesRecyclerView)
        adapter = MessageAdapter(listOf(), currentUserId)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<ImageButton>(R.id.attachImageButton).setOnClickListener { pickImage() }
        findViewById<ImageButton>(R.id.attachFileButton).setOnClickListener { pickFile() }
        findViewById<ImageButton>(R.id.sendButton).setOnClickListener { sendMessage() }

        db.collection("users").document(receiverUid)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    val base64 = snapshot.getString("profileImage") ?: ""
                    if (base64.isNotEmpty()) {
                        val bytes = Base64.decode(base64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        profileImage.setImageBitmap(bitmap)
                    }
                }
            }

        loadMessages()
    }

    private fun pickImage() {
        // Camera intent
        val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.resolveActivity(packageManager)?.let {
            val fileName = "camera_${System.currentTimeMillis()}.jpg"
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.TITLE, fileName)
            }
            cameraImageUri = contentResolver.insert(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )
            cameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, cameraImageUri)
        }

        // Gallery intent
        val galleryIntent = Intent(Intent.ACTION_PICK)
        galleryIntent.type = "image/*"

        // Chooser
        val chooser = Intent.createChooser(galleryIntent, "Select Image")
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
        startActivityForResult(chooser, PICK_IMAGE_REQUEST)
    }

    private fun pickFile() {
        PermissionHelper.requestStoragePermission(this)
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(Intent.createChooser(intent, "Select File"), PICK_FILE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return

        if (requestCode == PICK_IMAGE_REQUEST) {
            val uri = data?.data ?: cameraImageUri
            uri?.let { sendImageMessage(it) }
        }

        if (requestCode == PICK_FILE_REQUEST && data?.data != null) {
            sendFileMessage(data.data!!)
        }
    }

    private fun sendImageMessage(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 40, outputStream)
        val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)

        val message = Message(
            senderId = currentUserId,
            timestamp = System.currentTimeMillis(),
            imageBase64 = base64String,
            type = "image"
        )

        sendMessageToFirestore(message, "[Image]")
    }

    private fun sendFileMessage(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes() ?: return
        val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
        val fileName = uri.lastPathSegment ?: "file_${System.currentTimeMillis()}"

        val message = Message(
            senderId = currentUserId,
            timestamp = System.currentTimeMillis(),
            type = "file",
            fileName = fileName,
            fileBase64 = base64String
        )

        sendMessageToFirestore(message, "[File] $fileName")
    }

    private fun sendMessage() {
        val messageInput = findViewById<EditText>(R.id.messageInput)
        val text = messageInput.text.toString().trim()
        if (text.isEmpty()) return

        val message = Message(
            senderId = currentUserId,
            text = text,
            timestamp = System.currentTimeMillis(),
            type = "text"
        )

        sendMessageToFirestore(message, text)
        messageInput.setText("")
    }

    private fun sendMessageToFirestore(message: Message, lastMessage: String) {
        val chatRef = db.collection("chats").document(chatId)
        val messagesRef = chatRef.collection("messages")
        messagesRef.add(message).addOnSuccessListener {
            val chatMap = hashMapOf(
                "participants" to listOf(currentUserId, receiverUid),
                "lastMessage" to lastMessage,
                "timestamp" to System.currentTimeMillis()
            )
            chatRef.set(chatMap, com.google.firebase.firestore.SetOptions.merge())
        }.addOnFailureListener { e ->
            Log.e("ChatActivity", "Failed to update chat: ${e.message}")
        }
    }

    private fun loadMessages() {
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
                    adapter.updateItems(messageList.map { ChatItem.MessageItem(it) })
                    recyclerView.scrollToPosition(messageList.size - 1)
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        messageListener?.remove()
    }
}
