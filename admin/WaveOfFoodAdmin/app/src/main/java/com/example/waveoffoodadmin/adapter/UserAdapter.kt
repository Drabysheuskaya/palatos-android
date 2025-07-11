package com.example.waveoffoodadmin.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.waveoffoodadmin.databinding.ItemUserBinding

class UserAdapter(
    private val context: Context,
    private val users: MutableList<Map<String, String>>,
    private val onDeleteUser: (String) -> Unit,
    param: (Any) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user)
        holder.binding.deleteButton.setOnClickListener {
            onDeleteUser(user["key"] ?: "")
        }
    }

    override fun getItemCount(): Int = users.size

    inner class UserViewHolder(val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: Map<String, String>) {
            binding.userName.text = user["name"] ?: "Unknown"
            binding.userEmail.text = user["email"] ?: "No Email"
            binding.userPhone.text = user["phone"] ?: "No Phone"
        }
    }
}
