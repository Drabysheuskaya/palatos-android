package com.example.waveoffoodadmin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.waveoffoodadmin.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var database: DatabaseReference


    private val binding: ActivityLoginBinding by lazy { ActivityLoginBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://palatos-6612b-default-rtdb.firebaseio.com/").reference


        configureGoogleSignIn()


        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            } else {
                validateEmailAndLogin(email, password)
            }
        }


        binding.signUpButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }


        binding.forgotPasswordButton.setOnClickListener {
            val intent = Intent(this, ResetPasswordActivity::class.java)
            startActivity(intent)
        }


        binding.googleButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, 1)
        }
    }

    private fun configureGoogleSignIn() {
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)
    }

    private fun validateEmailAndLogin(email: String, password: String) {
        database.child("location").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var isAdmin = false
                var restaurantKey: String? = null

                // Iterate through all locations to check if the email is approved
                for (locationSnapshot in snapshot.children) {
                    val approvedAdminEmail = locationSnapshot.child("APPROVED_ADMINS").child("email").getValue(String::class.java)
                    if (approvedAdminEmail == email) {
                        isAdmin = true
                        restaurantKey = locationSnapshot.child("restaurantKey").getValue(String::class.java)
                        break
                    }
                }

                if (isAdmin && restaurantKey != null) {
                    // Proceed with login if email is approved
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val userId = auth.currentUser?.uid
                                Log.d("LoginActivity", "Login successful! UID: $userId")
                                if (userId != null) {
                                    navigateToMainActivity(restaurantKey, userId)
                                } else {
                                    Toast.makeText(this@LoginActivity, "Failed to retrieve user ID.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Log.e("LoginActivity", "Login failed: ${task.exception?.message}")
                                Toast.makeText(this@LoginActivity, "Login Failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    // Deny access if email is not approved
                    Toast.makeText(this@LoginActivity, "Access denied: Not an approved admin", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@LoginActivity, "Something went wrong", Toast.LENGTH_SHORT).show()
                Log.e("LoginActivity", "Firebase error: ${error.message}")
            }
        })
    }


    private fun navigateToMainActivity(restaurantKey: String, userId: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("RESTAURANT_KEY", restaurantKey)
            putExtra("USER_ID", userId)
        }
        startActivity(intent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Fetch restaurantKey after successful Google Sign-In
                        val userId = auth.currentUser?.uid
                        fetchRestaurantKeyAndNavigate(account.email ?: "", userId)
                    } else {
                        Toast.makeText(this, "Google Sign-In Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchRestaurantKeyAndNavigate(email: String, userId: String?) {
        if (userId == null) {
            Toast.makeText(this, "Failed to retrieve user ID.", Toast.LENGTH_SHORT).show()
            return
        }

        database.child("location").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var restaurantKey: String? = null

                for (locationSnapshot in snapshot.children) {
                    val approvedAdminEmail = locationSnapshot.child("APPROVED_ADMINS").child("email").getValue(String::class.java)
                    if (approvedAdminEmail == email) {
                        restaurantKey = locationSnapshot.child("restaurantKey").getValue(String::class.java)
                        break
                    }
                }

                if (restaurantKey != null) {
                    navigateToMainActivity(restaurantKey, userId)
                } else {
                    Toast.makeText(this@LoginActivity, "Access denied: Not an approved admin", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@LoginActivity, "Something went wrong", Toast.LENGTH_SHORT).show()
                Log.e("LoginActivity", "Firebase error: ${error.message}")
            }
        })
    }
}
