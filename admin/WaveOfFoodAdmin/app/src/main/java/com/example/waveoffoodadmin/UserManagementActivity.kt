package com.example.waveoffoodadmin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.waveoffoodadmin.adapter.UserAdapter
import com.example.waveoffoodadmin.databinding.ActivityUserManagementBinding
import com.google.firebase.database.*

class UserManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserManagementBinding
    private lateinit var database: DatabaseReference
    private val usersList = mutableListOf<Map<String, String>>()
    private val usersToDelete = mutableSetOf<String>()
    private lateinit var adapter: UserAdapter
    private var restaurantKey: String? = null
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)


        restaurantKey = intent.getStringExtra("RESTAURANT_KEY")
        userId = intent.getStringExtra("USER_ID")

        if (restaurantKey == null || userId == null) {
            Toast.makeText(this, "Missing restaurant key or user ID.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }


        database = FirebaseDatabase.getInstance("https://palatos-6612b-default-rtdb.firebaseio.com/").reference

        setupRecyclerView()
        fetchUsers()


        binding.backButton.setOnClickListener {
            navigateBackToMainActivity()
        }


        binding.addUserButton.setOnClickListener {
            val intent = Intent(this, CreateUserActivity::class.java).apply {
                putExtra("RESTAURANT_KEY", restaurantKey)
                putExtra("USER_ID", userId)
            }
            startActivity(intent)
        }


        binding.saveInfoButton.setOnClickListener {
            if (usersToDelete.isNotEmpty()) {
                processDeletions()
            } else {
                Toast.makeText(this, "No users marked for deletion", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Setup RecyclerView and its adapter.
     */
    private fun setupRecyclerView() {
        adapter = UserAdapter(this, usersList, { userId ->
            markUserForDeletion(userId)
        }, { userId ->
            unmarkUserForDeletion(userId.toString())
        })
        binding.userRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.userRecyclerView.adapter = adapter
    }

    /**
     * Fetch all users from the Firebase database.
     */
    private fun fetchUsers() {
        database.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usersList.clear()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.value as? Map<String, String> ?: continue
                    val userKey = userSnapshot.key ?: ""

                    val userWithKey = user.toMutableMap()
                    userWithKey["key"] = userKey
                    usersList.add(userWithKey)
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@UserManagementActivity,
                    "Error fetching users: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    /**
     * Mark a user for deletion.
     */
    private fun markUserForDeletion(userId: String) {
        usersToDelete.add(userId)
        Toast.makeText(this, "User marked for deletion", Toast.LENGTH_SHORT).show()
    }

    /**
     * Unmark a user for deletion.
     */
    private fun unmarkUserForDeletion(userId: String) {
        usersToDelete.remove(userId)
        Toast.makeText(this, "User unmarked for deletion", Toast.LENGTH_SHORT).show()
    }

    /**
     * Process deletions for all users marked for deletion.
     */
    private fun processDeletions() {
        for (userId in usersToDelete) {
            database.child("users").child(userId).removeValue().addOnSuccessListener {
                Toast.makeText(this, "User deleted successfully", Toast.LENGTH_SHORT).show()
                fetchUsers()
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to delete user: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
        usersToDelete.clear()
    }

    /**
     * Navigate back to MainActivity and pass the restaurantKey and userId.
     */
    private fun navigateBackToMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("RESTAURANT_KEY", restaurantKey)
            putExtra("USER_ID", userId)
        }
        startActivity(intent)
        finish()
    }
}
