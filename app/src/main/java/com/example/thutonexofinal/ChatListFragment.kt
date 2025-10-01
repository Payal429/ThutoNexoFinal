package com.example.thutonexofinal

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thutonexofinal.databinding.FragmentChatListBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.*


class ChatListFragment : Fragment() {
    // UI references
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatListAdapter
    private lateinit var userAvatar: CircleImageView
    private lateinit var userName: TextView
    private lateinit var userStatus: TextView

    // Data and Firestore
    private val chatList = mutableListOf<ChatListModel>()
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().uid!!
    private val userListeners = mutableMapOf<String, ListenerRegistration>()

    // listener for own user doc
    private var ownUserListener: ListenerRegistration? = null

    // Fragment life-cycle
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout (contains header + RecyclerView)
        return inflater.inflate(R.layout.fragment_chat_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // View binding
        userAvatar = view.findViewById(R.id.userAvatar)
        userName = view.findViewById(R.id.userName)
        userStatus = view.findViewById(R.id.userStatus)
        recyclerView = view.findViewById(R.id.chatRecyclerView)

        // RecyclerView setup
        adapter = ChatListAdapter(chatList) { chat ->
            // Row clicked -> open the conversation
            val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("chatId", chat.chatId)
            intent.putExtra("receiverId", chat.userId)
            intent.putExtra("receiverName", chat.username)
            startActivity(intent)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Request Permissions
        requestNecessaryPermissions()

        loadChats()
        loadOwnUserInfo()
    }

    // Permissions handling
    private fun requestNecessaryPermissions() {

        // Request notifications for Android 13+
        PermissionHelper.requestNotificationPermission(requireActivity())
    }

    // Handle the permission result callback
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PermissionHelper.REQUEST_NOTIFICATIONS -> {
                PermissionHelper.handlePermissionResult(
                    requireActivity(),
                    requestCode,
                    grantResults,
                    onGranted = { /* Optionally do something if granted */ },
                    onDenied = { /* Optionally handle denial */ }
                )
            }
        }
    }

    // Clean-up: remove all active listeners
    override fun onDestroyView() {
        super.onDestroyView()
        userListeners.values.forEach { it.remove() }
        ownUserListener?.remove()
    }

    // load OWN user document
    private fun loadOwnUserInfo() {
        ownUserListener = db.collection("users")
            .document(currentUserId)
            .addSnapshotListener { doc, error ->
                if (error != null || doc == null || !doc.exists()) return@addSnapshotListener

                val name = doc.getString("name") ?: "Me"
                val status = doc.getString("bio") ?: "No bio"
                val b64 = doc.getString("profileImage") ?: ""

                userName.text = name
                userStatus.text = status

                //  Decode Base64 avatar (fallback to placeholder)
                if (b64.isNotEmpty()) {
                    try {
                        val bytes = Base64.decode(b64, Base64.DEFAULT)
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        userAvatar.setImageBitmap(bmp)
                    } catch (e: Exception) {
                        userAvatar.setImageResource(R.drawable.ic_profile) // fallback
                    }
                } else {
                    userAvatar.setImageResource(R.drawable.ic_profile)
                }
            }
    }

    // Conversations list
    private fun loadChats() {
        // Remove old listeners before building a new set
        userListeners.values.forEach { it.remove() }
        userListeners.clear()
        chatList.clear()
        adapter.notifyDataSetChanged()

        // Query: only chats where current user is a participant, newest first
        db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace(); return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                // For every chat document...
                for (doc in snapshot.documents) {
                    val chatId = doc.id
                    val participants = doc.get("participants") as? List<String> ?: continue
                    val otherUserId = participants.firstOrNull { it != currentUserId } ?: continue

                    // Firestore reference to the other participant
                    val userRef = db.collection("users").document(otherUserId)

                    // Remove previous listener for this user (if any)
                    userListeners[otherUserId]?.remove()

                    // Attach a real-time listener to the other user document
                    val listener = userRef.addSnapshotListener { userDoc, _ ->
                        if (userDoc != null && userDoc.exists()) {
                            val name = userDoc.getString("name") ?: "Unknown"
                            val profileImageBase64 = userDoc.getString("profileImage") ?: ""

                            // Fetch the last message in this chat
                            db.collection("chats").document(chatId)
                                .collection("messages")
                                .orderBy("timestamp", Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .addOnSuccessListener { messageSnapshot ->

                                    // Get last message type
                                    val lastMessageDoc = messageSnapshot.documents.firstOrNull()
                                    val lastMessageText = lastMessageDoc?.getString("text") ?: ""
                                    val lastMessageType =
                                        lastMessageDoc?.getString("type") ?: "text"
                                    val timestampText =
                                        (lastMessageDoc?.get("timestamp") as? Long)?.let {
                                            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(
                                                Date(it)
                                            )
                                        } ?: ""

                                    // Count unread messages sent by the other user
                                    db.collection("chats")
                                        .document(chatId)
                                        .collection("messages")
                                        .whereEqualTo("read", false)
                                        .get()
                                        .addOnSuccessListener { snap ->
                                            val totalUnread =
                                                snap.documents.count { it.getString("senderId") != currentUserId }

                                            val index =
                                                chatList.indexOfFirst { it.userId == otherUserId }
                                            if (index != -1) {
                                                chatList[index] = chatList[index].copy(
                                                    username = name,
                                                    profileImageBase64 = profileImageBase64,
                                                    lastMessage = lastMessageText,
                                                    lastMessageType = lastMessageType, // 🔹 update type
                                                    timestamp = timestampText,
                                                    unreadCount = totalUnread
                                                )
                                                adapter.notifyItemChanged(index)
                                            } else {
                                                chatList.add(
                                                    ChatListModel(
                                                        chatId = chatId,
                                                        userId = otherUserId,
                                                        username = name,
                                                        profileImageBase64 = profileImageBase64,
                                                        lastMessage = lastMessageText,
                                                        lastMessageType = lastMessageType, // 🔹 save type
                                                        timestamp = timestampText,
                                                        unreadCount = totalUnread
                                                    )
                                                )
                                                adapter.notifyDataSetChanged()
                                            }
                                        }
                                }
                        }
                    }

                    // Keep reference so we can remove it later
                    userListeners[otherUserId] = listener
                }
            }
    }
}