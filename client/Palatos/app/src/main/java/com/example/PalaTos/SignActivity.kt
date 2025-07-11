package com.example.PalaTos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.PalaTos.databinding.ActivitySignBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class SignActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private val binding: ActivitySignBinding by lazy { ActivitySignBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = Firebase.database.reference


        binding.createAccountButton.setOnClickListener {
            val username = binding.userName.text.toString().trim()
            val email = binding.emailAddress.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (username.isBlank() || email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            } else {
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        saveUserData(username, email)
                        Toast.makeText(this, "Account Created Successfully!", Toast.LENGTH_SHORT).show()
                        navigateToLogin()
                    } else {
                        Toast.makeText(this, "Account Creation Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


        binding.alreadyhavebutton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun saveUserData(username: String, email: String) {
        val userId = auth.currentUser?.uid ?: return
        val user = mapOf("username" to username, "email" to email)
        database.child("users").child(userId).setValue(user)
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
