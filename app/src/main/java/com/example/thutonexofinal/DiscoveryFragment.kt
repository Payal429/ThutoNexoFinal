package com.example.thutonexofinal

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.thutonexofinal.R
import com.example.thutonexofinal.User
import com.example.thutonexofinal.UserAdapter
import com.google.android.material.appbar.MaterialToolbar

class DiscoveryFragment : Fragment() {

    // UI references
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchBar: EditText
    private lateinit var roleSpinner: Spinner
    private lateinit var btnDiscovery: Button

    // Data and adapter
    private lateinit var adapter: UserAdapter
    private val userList = mutableListOf<User>()
    private val db = FirebaseFirestore.getInstance()
    private val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Fragment life-cycle
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_discovery_activity, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Make the toolbar the action bar → title shows
        val toolbar = view.findViewById<MaterialToolbar>(R.id.topAppBar)
        (requireActivity() as androidx.appcompat.app.AppCompatActivity).apply {
            setSupportActionBar(toolbar)
            supportActionBar?.title = "Discover Chats"
            supportActionBar?.setDisplayShowTitleEnabled(true)
        }
        // Make toolbar title bold with custom font
        for (i in 0 until toolbar.childCount) {
            val child = toolbar.getChildAt(i)
            if (child is TextView && child.text == toolbar.title) {
                // Set bold style
                child.setTypeface(child.typeface, android.graphics.Typeface.BOLD)
                // Set custom font (backward-compatible)
                val typeface = ResourcesCompat.getFont(requireContext(), R.font.anek_gujarati_bold)
                child.typeface = typeface
                break
            }
        }

        // View binding
        recyclerView = view.findViewById(R.id.userRecyclerView)
        searchBar = view.findViewById(R.id.searchBar)
        roleSpinner = view.findViewById(R.id.roleSpinner)
        btnDiscovery = view.findViewById(R.id.btnDiscovery)

        // RecyclerView and adapter
        adapter = UserAdapter(requireContext(), userList) { user ->
            openChatWithUser(user.uid, user.name)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Request contacts permission when fragment starts
        PermissionHelper.requestContactsPermission(requireActivity())

        // Setup listeners
        setupRoleSpinner()
        fetchUsers()

        // Real-time search while typing
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterUsers()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Manual filter button (duplicates spinner logic for accessibility)
        btnDiscovery.setOnClickListener { filterUsers() }
    }

    // Handle permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PermissionHelper.REQUEST_CONTACTS -> {
                PermissionHelper.handlePermissionResult(
                    requireActivity(),
                    requestCode,
                    grantResults,
                    onGranted = {
                        // Permission granted, you can optionally do something
                        Toast.makeText(
                            requireContext(),
                            "Contacts permission granted",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onDenied = {
                        Toast.makeText(
                            requireContext(),
                            "Contacts permission denied",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }

    // Role spinner (All / Tutor / Student)
    private fun setupRoleSpinner() {
        val roles = listOf("All", "Tutor", "Student")
        val roleAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roles)
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        roleSpinner.adapter = roleAdapter

        roleSpinner.setOnItemSelectedListener(object :
            android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                filterUsers()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
    }

    //  Load all users once (except self)
    private fun fetchUsers() {
        db.collection("users").get()
            .addOnSuccessListener { result ->
                userList.clear()
                for (doc in result) {
                    val user = doc.toObject(User::class.java)
                    if (user.uid != currentUserUid) userList.add(user)
                }
                adapter.updateList(userList)
            }
            .addOnFailureListener { e -> e.printStackTrace() }
    }

    // Filter list by name + role (case-insensitive)
    private fun filterUsers() {
        val query = searchBar.text.toString().lowercase()
        val selectedRole = roleSpinner.selectedItem.toString()

        val filteredList = userList.filter { user ->
            val matchesName = user.name.lowercase().contains(query)
            val matchesRole =
                selectedRole == "All" || user.role.equals(selectedRole, ignoreCase = true)
            matchesName && matchesRole
        }
        adapter.updateList(filteredList)
    }

    // Open chat and ensure single chatId
    private fun openChatWithUser(otherUserId: String, otherUserName: String) {
        val chatsRef = db.collection("chats")

        // Query for existing chat
        chatsRef.whereArrayContains("participants", currentUserUid)
            .get()
            .addOnSuccessListener { snapshot ->
                val existingChat = snapshot.documents.firstOrNull {
                    val participants = it.get("participants") as? List<*>
                    participants?.contains(otherUserId) == true
                }

                if (existingChat != null) {
                    // Open existing chat
                    startChatActivity(existingChat.id, otherUserId, otherUserName)
                } else {
                    // Create new chat
                    val newChat = mapOf(
                        "participants" to listOf(currentUserUid, otherUserId),
                        "lastMessage" to "",
                        "timestamp" to System.currentTimeMillis()
                    )
                    chatsRef.add(newChat).addOnSuccessListener { doc ->
                        startChatActivity(doc.id, otherUserId, otherUserName)
                    }
                }
            }
    }

    // Helper: start ChatActivity with required extras
    private fun startChatActivity(chatId: String, receiverUid: String, receiverName: String) {
        val intent = android.content.Intent(requireContext(), ChatActivity::class.java)
        intent.putExtra("chatId", chatId)
        intent.putExtra("receiverId", receiverUid)
        intent.putExtra("receiverName", receiverName)
        startActivity(intent)
    }
}
