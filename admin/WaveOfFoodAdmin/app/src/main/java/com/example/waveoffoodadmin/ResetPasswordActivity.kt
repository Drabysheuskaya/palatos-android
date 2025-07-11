package com.example.waveoffoodadmin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var continueButton: Button
    private lateinit var messageTextView: TextView
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.resetpassword)


        emailEditText = findViewById(R.id.emailEditText)
        continueButton = findViewById(R.id.continueButton)
        messageTextView = findViewById(R.id.messageTextView)
        database = FirebaseDatabase.getInstance("https://palatos-6612b-default-rtdb.firebaseio.com/").reference


        findViewById<View>(R.id.backButton).setOnClickListener {
            navigateBackToLogin()
        }


        continueButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()

            when {
                email.isEmpty() -> {
                    messageTextView.text = "Please enter your email."
                }
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    messageTextView.text = "Please enter a valid email address."
                }
                else -> {
                    checkIfAdminAndResetPassword(email)
                }
            }
        }
    }

    private fun checkIfAdminAndResetPassword(email: String) {
        database.child("location").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var isAdmin = false


                for (locationSnapshot in snapshot.children) {
                    val approvedAdminEmail = locationSnapshot.child("APPROVED_ADMINS").child("email").getValue(String::class.java)
                    if (approvedAdminEmail == email) {
                        isAdmin = true
                        break
                    }
                }

                if (isAdmin) {
                    resetPassword(email)
                } else {
                    messageTextView.text = "Access denied: Not an approved admin."
                    Toast.makeText(this@ResetPasswordActivity, "Access denied: Not an approved admin.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                messageTextView.text = "Something went wrong."
                Toast.makeText(this@ResetPasswordActivity, "Something went wrong: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun resetPassword(email: String) {

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    messageTextView.text = "Password reset email sent to $email."
                    Toast.makeText(this, "Check your email for the reset link!", Toast.LENGTH_SHORT).show()
                } else {
                    messageTextView.text = "Error: ${task.exception?.message}"
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateBackToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }


    override fun onBackPressed() {
        super.onBackPressed()
        navigateBackToLogin()
    }
}
