package com.example.waveoffoodadmin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.waveoffoodadmin.databinding.ActivityCreateUserBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class CreateUserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateUserBinding
    private lateinit var database: DatabaseReference
    private var restaurantKey: String? = null
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateUserBinding.inflate(layoutInflater)
        setContentView(binding.root)


        restaurantKey = intent.getStringExtra("RESTAURANT_KEY")
        userId = intent.getStringExtra("USER_ID")

        if (restaurantKey == null || userId == null) {
            Toast.makeText(this, "Missing restaurant key or user ID.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }


        database = FirebaseDatabase.getInstance("https://palatos-6612b-default-rtdb.firebaseio.com/").reference


        binding.createUserButton.setOnClickListener {
            val name = binding.nameEditText.text.toString()
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                val newUser = mapOf(
                    "name" to name,
                    "email" to email,
                    "password" to password
                )
                database.child("users").push().setValue(newUser).addOnSuccessListener {
                    Toast.makeText(this, "User created successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed to create user: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }


        binding.backButton.setOnClickListener {
            navigateBackToUserManagement()
        }
    }

    /**
     * Navigate back to UserManagementActivity while retaining restaurantKey and userId.
     */
    private fun navigateBackToUserManagement() {
        val intent = Intent(this, UserManagementActivity::class.java).apply {
            putExtra("RESTAURANT_KEY", restaurantKey)
            putExtra("USER_ID", userId)
        }
        startActivity(intent)
        finish()
    }
}
