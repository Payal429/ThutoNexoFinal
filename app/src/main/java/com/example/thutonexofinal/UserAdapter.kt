package com.example.thutonexofinal

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserAdapter(
    private val context: Context,
    private var userList: List<User>,
    private val onUserClick: (User) -> Unit // Lambda to handle clicks
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.userName)
        val roleText: TextView = itemView.findViewById(R.id.userRole)
        val subjectText: TextView = itemView.findViewById(R.id.userSubjects)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int = userList.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]

        holder.nameText.text = user.name
        holder.roleText.text = user.role

        // Convert subjects safely
        val subjectsList: List<String> = when (val s = user.subjects) {
            is String -> listOf(s)
            is List<*> -> s.filterIsInstance<String>()
            else -> emptyList()
        }

        holder.subjectText.text = subjectsList.joinToString(", ")

        // Call the fragment's click lambda instead of handling Firestore here
        holder.itemView.setOnClickListener {
            onUserClick(user)
        }
    }

    fun updateList(newList: List<User>) {
        userList = newList
        notifyDataSetChanged()
    }
}
