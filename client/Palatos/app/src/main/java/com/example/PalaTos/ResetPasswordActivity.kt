package com.example.PalaTos

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var continueButton: Button
    private lateinit var messageTextView: TextView
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.resetpassword)


        emailEditText = findViewById(R.id.emailEditText)
        continueButton = findViewById(R.id.continueButton)
        messageTextView = findViewById(R.id.messageTextView)


        findViewById<View>(R.id.backButton).setOnClickListener {
            onBackPressed()
        }

        continueButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isEmpty()) {
                messageTextView.text = "Please enter a valid email."
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                messageTextView.text = "Please enter a valid email address."
            } else {
                resetPassword(email)
            }
        }
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

    override fun onBackPressed() {
        super.onBackPressed()
        onBackPressedDispatcher.onBackPressed()
    }
}
